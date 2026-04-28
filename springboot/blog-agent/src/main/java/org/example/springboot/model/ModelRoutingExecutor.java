package org.example.springboot.model;
//通用的故障转移执行器

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.emuns.ModelCapability;
import org.example.springboot.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRoutingExecutor {

    private final ModelHealthStore healthStore;


    /**
     * <C, T>（占位符声明),C (Client)：
     * 代表调用的客户端工具,T (Type/Target)：代表最终想要拿到的结果类型。
     * public <C,T> T :前面的T是一个声明，代表返回值是一个T（未知类型）
     * 若前面没声明T，后面使用T，则报错
     * 若前面声明T，后面没使用T，则没意义
     *C：就是一个占位符，代表方法参数可能使用到这个
     * @param modelCapability
     * @param targets
     * @param clientResolver
     * @param caller
     * @return
     * @param <C>
     * @param <T>
     */
    public <C,T> T executeWithFallback(
            ModelCapability modelCapability,
            List<ModelTarget> targets,
            Function<ModelTarget,C> clientResolver,
            ModelCaller<C,T> caller


    )  {
        String label = modelCapability.getDisplayName();
        if(targets==null||targets.isEmpty()){
            throw new ServiceException("targets is empty");
        }

        Throwable last = null;
        //挨个查找可用模型（客户端）
        for (ModelTarget target : targets) {
            C client  = clientResolver.apply(target);
            if(client ==null){
                log.warn("{} provider client missing: provider={}, modelId={}",
                        label, target.candidate().getProvider(), target.id());
                continue;
            }
            //这个用不了
            if(!healthStore.allowCall(target.id())){
                continue;
            }
            try {
                //这个可用，向该模型发送请求
                T response = caller.call(client, target);
                //请求成功，上报请求记录
                healthStore.markSuccess(target.id());
                return response;
            }catch (Exception e){
                last = e;
                //所有模型都用不了
                healthStore.markFailure(target.id());
                log.warn("{} model failed, fallback to next. modelId={}, provider={}",
                        label, target.id(), target.candidate().getProvider(), e);
            }

        }
        throw new ServiceException(
                "All " + label + " model candidates failed: "
                        + (last == null ? "unknown" : last.getMessage())

        );
    }


}
