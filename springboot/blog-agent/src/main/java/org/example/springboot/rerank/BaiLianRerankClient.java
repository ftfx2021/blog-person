package org.example.springboot.rerank;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.springboot.config.AIModelProperties;
import org.example.springboot.emuns.ModelCapability;
import org.example.springboot.emuns.ModelProvider;
import org.example.springboot.exception.RemoteException;
import org.example.springboot.framework.RetrievedChunk;
import org.example.springboot.http.*;
import org.example.springboot.model.ModelTarget;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaiLianRerankClient implements RerankClient {
    private final OkHttpClient okHttpClient;


    @Override
    public String provider() {
        return ModelProvider.BAI_LIAN.getId();
    }



    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidate, Integer topN, ModelTarget target) {
      if(candidate==null||candidate.isEmpty()){
          return List.of();
      }
      List<RetrievedChunk> dedup=new ArrayList<>(candidate.size());
      //去重，seen Set按照id进行去重
      Set<Integer> seen = new HashSet<>();
        for (RetrievedChunk retrievedChunk : candidate) {
            if(seen.add(retrievedChunk.getId())){
              dedup.add(retrievedChunk);
            }
        }
        //看看去重后的待重排序的数量是否大于topN,如果不是，则没必要重排序了
        if(topN<=0||dedup.size()<=topN){
          return dedup;
        }
        //正式重排序
        return doRerank(query, dedup, topN, target);
    }

    /**
     * 重排的核心方法
     * @param query  待查询的文本
     * @param candidate  待重拍的候选数量
     * @param topN  取前多少个
     * @param target 一次完整请求所需要的模型信息
     * @return
     */
    public List<RetrievedChunk> doRerank(String query, List<RetrievedChunk> candidate, Integer topN,ModelTarget target) {
        //供应商信息校验和提取
        AIModelProperties.ProviderConfig provider = HttpResponseHelper.requireProvider(target, provider());
        if(candidate==null||candidate.isEmpty()){
            return List.of();
        }
        //封装请求体
        //参考：https://help.aliyun.com/zh/model-studio/text-rerank-api?spm=a2c4g.11186623.0.0.663a28bdKsMMhU
        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("model",HttpResponseHelper.requireModel(target,provider()));
        JsonObject input = new JsonObject();
        //查询内容。最大长度不能超过4,000个Token。
        input.addProperty("query",query);
        JsonArray documentsArray  = new JsonArray();
        for (RetrievedChunk retrievedChunk : candidate) {
            documentsArray.add(retrievedChunk!=null?retrievedChunk.getText():"");
        }
        //待排序的候选文档列表。每个元素是一个字符串。
        input.add("documents",documentsArray);
        JsonObject parameters  = new JsonObject();
        //返回排序后的top_n个文档。默认返回全部文档。如果指定的值大于文档总数，将返回全部文档。
        parameters.addProperty("top_n",topN);
        //是否在排序结果中返回文档原文。默认值false
        parameters.addProperty("return_documents",true);

        requestBody.add("input",input);
        //当使用 qwen3-rerank 模型时，无需使用 parameters 对象参数。此时，top_n 和 instruct 参数需与 model 等参数位于同一层级。
        requestBody.add("parameters",parameters);

        Request request = new Request.Builder()
                .url(ModelUrlResolver.resolveUrl(provider, target.candidate(), ModelCapability.RERANK))
                .post(RequestBody.create(requestBody.toString(), HttpMediaTypes.JSON))
                .build();
        JsonObject responseJson;
        try(Response response = okHttpClient.newCall(request).execute()){
            if(!response.isSuccessful()){
                String body = HttpResponseHelper.readBody(response.body());

                log.warn("{} rerank 请求失败: status={}, body={}",
                        provider(), response.code(), body);
                throw new ModelClientException(
                        provider() + " rerank 请求失败: HTTP " + response.code(),
                        ModelClientErrorType.fromHttpStatus(response.code()),
                        response.code()
                );
            }

            responseJson=HttpResponseHelper.parseJson(response.body(),provider());

        }catch (IOException e){
            throw new ModelClientException(
                    provider() + " rerank 请求失败: " + e.getMessage(),
                    ModelClientErrorType.NETWORK_ERROR, null, e);
        }
        //output任务输出信息
        JsonObject output = requireOutput(responseJson);
        //results，包含documents,index,relevance_score三个参数
        //index:表示该结果对应于输入 documents 列表中的原始索引位置。
        //relevance_score:该文档与查询的语义相关性得分，取值范围为 0.0 到 1.0。分数越高，相关性越强。
        //排序结果列表。按 relevance_score 从高到低排列。
        JsonArray results = output.getAsJsonArray("results");
        if(CollectionUtils.isEmpty(results.asList())){
            throw new ModelClientException(
                    provider() + " rerank results 为空",
                    ModelClientErrorType.INVALID_RESPONSE, null);
        }

        ArrayList<RetrievedChunk> reranks = new ArrayList<>();

        Set<Integer> addedIds = new HashSet<>();

        for (JsonElement result : results) {
            if(!result.isJsonObject()){
                continue;
            }
            JsonObject resJson = result.getAsJsonObject();
            if(!resJson.has("index")){
                continue;
            }
            int idx = resJson.get("index").getAsInt();
            //返回的index不在原始索引范围里
            if(idx<0||idx>=candidate.size()){
                continue;
            }
            RetrievedChunk src = candidate.get(idx);

            Float score = null;
            // 提取相关性得分
            if(resJson.has("relevance_score")&&!resJson.get("relevance_score").isJsonNull()){
                score = resJson.get("relevance_score").getAsFloat();
            }

            RetrievedChunk hit = score!=null?new RetrievedChunk(idx, src.getText(), score):src;
            reranks.add(hit);
            addedIds.add(src.getId());

            if(reranks.size()>=topN){
                break;
            }

        }
        //响应解析之后，如果 reranked 的数量不够 topN，还有一段回填逻辑
        //什么时候会触发回填？Rerank API 返回的结果数可能少于请求的 topN——比如模型认为大部分候选和 query 完全无关，
        // 主动过滤掉了低分结果；或者 API 实现有其他限制。
        if(reranks.size()<topN){
            for (RetrievedChunk retrievedChunk : candidate) {
                //addedIds.add(id) 返回 false 表示已存在
                if(addedIds.add(retrievedChunk.getId())){
                    reranks.add(retrievedChunk);
                }
                if(reranks.size()>=topN){
                    break;
                }
            }
        }

        return reranks;
    }

    private JsonObject requireOutput(JsonObject respJson){

        if(respJson==null||respJson.isEmpty()||!respJson.has("output")){

                throw new ModelClientException(
                        provider() + " rerank 响应缺少 output",
                        ModelClientErrorType.INVALID_RESPONSE, null);

        }

        JsonObject output = respJson.getAsJsonObject("output");
        if(output==null||!output.has("result")){
            throw new ModelClientException(
                    provider() + " rerank 响应缺少 output",
                    ModelClientErrorType.INVALID_RESPONSE, null);
        }
        return output;
    }
}
