package org.example.springboot.rerank;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class BaiLianRerankClient implements RerankClient {
    private final OkHttpClient okHttpClient;

    public BaiLianRerankClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

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
      Set<Integer> seen = new HashSet<>();

        for (RetrievedChunk retrievedChunk : candidate) {
            if(seen.add(retrievedChunk.getId())){
              dedup.add(retrievedChunk);
            }
        }
        if(topN<=0||dedup.size()<=topN){
          return dedup;
        }
        return doRerank(query, dedup, topN, target);
    }

    public List<RetrievedChunk> doRerank(String query, List<RetrievedChunk> candidate, Integer topN,ModelTarget target) {
        AIModelProperties.ProviderConfig provider = HttpResponseHelper.requireProvider(target, provider());
        if(candidate==null||candidate.isEmpty()){
            return List.of();
        }

        JsonObject requestBody = new JsonObject();



        requestBody.addProperty("model",HttpResponseHelper.requireModel(target,provider()));
        JsonObject input = new JsonObject();
        input.addProperty("query",query);
        JsonArray documentsArray  = new JsonArray();
        for (RetrievedChunk retrievedChunk : candidate) {
            documentsArray.add(retrievedChunk!=null?retrievedChunk.getText():"");
        }
        input.add("documents",documentsArray);
        JsonObject parameters  = new JsonObject();

        parameters.addProperty("top_n",topN);
        parameters.addProperty("return_documents",true);

        requestBody.add("input",input);
        requestBody.add("parameters",parameters);

        Request request = new Request.Builder()
                .url(ModelUrlResolver.resolveUrl(provider, target.candidate(), ModelCapability.EMBEDDED))
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
        JsonObject output = requireOutput(responseJson);

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

            if(idx<0||idx>=candidate.size()){
                continue;
            }
            RetrievedChunk src = candidate.get(idx);

            Float score = null;

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

        if(reranks.size()<=topN){
            for (RetrievedChunk retrievedChunk : candidate) {
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
