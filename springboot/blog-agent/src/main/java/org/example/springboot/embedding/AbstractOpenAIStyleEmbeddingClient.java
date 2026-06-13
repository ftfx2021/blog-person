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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
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

    protected int maxBatchSize(){
        return 0;//0表示不限制
    }

    @Override
    public List<Float> embed(String text, ModelTarget target) {
        List<List<Float>> res = doEmbed(List.of(text), target);
        return res.get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts, ModelTarget target) {
        if(CollectionUtils.isEmpty(texts)){
            return Collections.emptyList();
        }
        int batchSize = maxBatchSize();
        if(batchSize <= 0||texts.size()<batchSize){
            return doEmbed(texts, target);
        }
        List<List<Float>> res = new ArrayList<>(Collections.nCopies(texts.size(),null));
        for(int i=0,n=texts.size();i<n;i+=batchSize){
            int end =Math.min(i+batchSize,n);
            List<String> slice = texts.subList(i,end);
            List<List<Float>> part = doEmbed(slice,target);
            for(int k=0;k<part.size();k++){
                res.set(i+k,part.get(k));
            }
        }
        return res;
    }

    protected List<List<Float>> doEmbed(List<String> texts, ModelTarget target) {
        AIModelProperties.ProviderConfig provider = HttpResponseHelper.requireProvider(target, provider());
        if(requiresApiKey()) {

            HttpResponseHelper.requireApiKey(provider,provider());
        }
        String url = ModelUrlResolver.resolveUrl(provider,target.candidate(), ModelCapability.EMBEDDED);
        JsonObject body = new JsonObject();
        body.addProperty("model",HttpResponseHelper.requireModel(target,provider()));
        JsonArray inputArray = new JsonArray();
        for (String text : texts) {
            inputArray.add(text);
        }
        body.add("input",inputArray);
        body.addProperty("dimensions",target.candidate().getDimension());
        customizeRequestBody(body,target);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), HttpMediaTypes.JSON));
        if(requiresApiKey()) {
            requestBuilder.addHeader("Authorization","Bearer "+provider.getApiKey());
        }

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
