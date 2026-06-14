package org.example.springboot.embedding;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.springboot.config.AIModelProperties;
import org.example.springboot.emuns.ModelCapability;
import org.example.springboot.http.*;
import org.example.springboot.model.ModelTarget;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public abstract class AbstractOpenAIStyleEmbeddingClient implements EmbeddingClient {

    protected  final OkHttpClient httpClient;
    protected AbstractOpenAIStyleEmbeddingClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }



    protected boolean requiresApiKey() {
        return true;
    }

    protected void customizeRequestBody(JsonObject body, ModelTarget target) {
        body.addProperty("encoding_format","float");
    }

    /**
     * 最大批次数量限制，这个一般由供应商限制，如硅基流动限制为32
     * @return
     */
    protected int maxBatchSize(){
        return 0;//0表示不限制
    }

    @Override
    public List<Float> embed(String text, ModelTarget target) {
        List<List<Float>> res = doEmbed(List.of(text), target);
        return res.get(0);
    }

    /**
     * 批量向量化
     * @param texts 原文本
     * @param target 一次完整调用的所用的模型配置信息
     * @return
     */
    @Override
    public List<List<Float>> embedBatch(List<String> texts, ModelTarget target) {
        if(CollectionUtils.isEmpty(texts)){
            return Collections.emptyList();
        }
        int batchSize = maxBatchSize();
        //如果要向量化的文本条数没达到最大批次数量，直接向量化
        if(batchSize <= 0||texts.size()<batchSize){
            return doEmbed(texts, target);
        }
        /*
            new ArrayList<>(n)：初始化容量为n的数组，但是这n个“格子”啥都没有
            new ArrayList<>(Collections.nCopies(texts.size(),null));：初始化容量为n的数组，但是这n个格子，每个都有null占位，可以使用set进行分配
         */
        List<List<Float>> res = new ArrayList<>(Collections.nCopies(texts.size(),null));
        //以batchSize为批次大小，分为多少批
        for(int i=0,n=texts.size();i<n;i+=batchSize){
            int end =Math.min(i+batchSize,n);
            List<String> slice = texts.subList(i,end);
            List<List<Float>> part = doEmbed(slice,target);
            for(int k=0;k<part.size();k++){
                //按索引放置，保持原始顺序
                //如果用 add()，顺序会乱掉；
                res.set(i+k,part.get(k));
            }
        }
        return res;
    }

    /**
     * 向量化核心方法
     * @param texts 文本
     * @param target 一次完整调用的所用的模型配置信息
     * @return 向量化后的数据
     */
    protected List<List<Float>> doEmbed(List<String> texts, ModelTarget target) {
        //获取并对供应商进行校验
        AIModelProperties.ProviderConfig provider = HttpResponseHelper.requireProvider(target, provider());
        if(requiresApiKey()) {
            //校验apiKey的存在性
            HttpResponseHelper.requireApiKey(provider,provider());
        }
        //拼接完整url
        String url = ModelUrlResolver.resolveUrl(provider,target.candidate(), ModelCapability.EMBEDDED);
        //组装请求体
        JsonObject body = new JsonObject();
        body.addProperty("model",HttpResponseHelper.requireModel(target,provider()));
        JsonArray inputArray = new JsonArray();
        for (String text : texts) {
            inputArray.add(text);
        }
        //open ai支持传入数组，具体参考https://developers.openai.ac.cn/api/reference/resources/embeddings/methods/create
        body.add("input",inputArray);
        body.addProperty("dimensions",target.candidate().getDimension());
        customizeRequestBody(body,target);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), HttpMediaTypes.JSON));
        if(requiresApiKey()) {
            requestBuilder.addHeader("Authorization","Bearer "+provider.getApiKey());
        }
        /*
        为什么没有传 encoding_format？
        为 encoding_format 是可选参数，默认值就是 float！
         */

        Request request = requestBuilder.build();


        JsonObject json;

        try(Response response = httpClient.newCall(request).execute()){
            if(!response.isSuccessful()){
                String errBody = HttpResponseHelper.readBody(response.body());
                log.warn("{}embeeding请求失败,status={},body={}",provider(),response.code(),errBody);
                throw new ModelClientException(
                        provider() + " embedding 请求失败: HTTP " + response.code(),
                        ModelClientErrorType.fromHttpStatus(response.code()),
                        response.code()
                );
            }
            json = HttpResponseHelper.parseJson(response.body(), provider());
        }catch (IOException e){
            throw new ModelClientException(
                    provider() + " embedding 请求失败: " + e.getMessage(),
                    ModelClientErrorType.NETWORK_ERROR, null, e);
        }

        if(json.has("error")){
            JsonObject error = json.getAsJsonObject("error");
          String code=  error.has("code")?error.get("code").getAsString():"unknown";
          String message= error.has("message")?error.get("message").getAsString():"";
            throw new ModelClientException(
                    provider() + " embedding 错误: " + code + " - " + message,
                    ModelClientErrorType.PROVIDER_ERROR, null);
        }


        JsonArray data = json.getAsJsonArray("data");
        if(data==null||data.isEmpty()){
            throw new ModelClientException(
            provider() + " embedding 响应中缺少 data 数组",
                    ModelClientErrorType.INVALID_RESPONSE, null);
        }

        List<List<Float>> res = new ArrayList<>(data.size());
        for (JsonElement el : data) {
            JsonObject obj = el.getAsJsonObject();
            JsonArray emb = obj.getAsJsonArray("embedding");
            if(emb==null||emb.isEmpty()){
                throw new ModelClientException(
                        provider() + " embedding 响应中缺少 embedding 字段",
                        ModelClientErrorType.INVALID_RESPONSE, null);
            }

            List<Float> vector = new ArrayList<>(emb.size());
            for (JsonElement v : emb) {
                vector.add(v.getAsFloat());
            }
            res.add(vector);
        }
        return res;

    }
}
