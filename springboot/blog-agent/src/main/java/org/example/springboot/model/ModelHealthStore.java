package org.example.springboot.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.springboot.config.AIModelProperties;
import org.example.springboot.emuns.State;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Data
@Component
@RequiredArgsConstructor
//三态熔断器，用于保护系统不会持续调用已经故障的模型
public class ModelHealthStore {

    private  final AIModelProperties properties;
    private final Map<String, ModelHealth> healthById = new ConcurrentHashMap<>();

    /**
     * 放行检查
     * 为什么把判断冷却时间放在这里面？
     * 把所有的逻辑包在 compute 里面，因为 compute 针对同一个模型是加锁排队的。这就保证了这 100 个并发请求必须单步进入：
     *
     * 第 1 个线程进入： 看表，时间到了（v.openUntil > now 为 false）！把状态改成 HALF_OPEN，把 halfOpenInFlight 设为 true。它成了合法的唯一探路车。
     * 第 2 个线程进入： 因为前一个线程已经把状态改了，它进来时发现状态已经是 HALF_OPEN，并且 halfOpenInFlight == true（探路车已经出发了）。于是它直接被拒绝，乖乖走开。
     * 第 3 到第 100 个线程： 同样发现探路车已经在路上，全部被直接拒绝。
     * 放在 compute 里面，就是为了保证在冷却倒计时结束的那个瞬间，无论多猛烈的流量涌过来，
     * 交警都能有条不紊地拦住后面的车，绝对只放第一辆车过去探路。
     * @param id 模型id
     * @return 是否放行
     */
    public boolean allowCall(String id){
        if(id==null){
            return false;
        }
        long now = System.currentTimeMillis();
        //使用 AtomicBoolean 而不用普通的 boolean，最根本的原因是 Java 语言对 Lambda 表达式（匿名函数）的语法限制
        //Java 有一个硬性规定：Lambda 表达式内部，如果想要使用外面定义的局部变量，这个变量必须是“最终的”（final 或 effectively final），
        // 也就是说，你不能在 Lambda 内部去重新赋值它。
        //在这里，AtomicBoolean 的核心作用并不是它的“原子并发控制”。
        //
        //因为 ConcurrentHashMap.compute() 方法本身就已经自带了强大的并发控制：它在执行内部那个 Lambda 表达式时，
        // 会自动对当前操作的 id（那座桥）加锁。 这就意味着，针对同一个模型 id，绝不可能有两个线程同时在执行这个 Lambda 里的代码。
        AtomicBoolean allowed = new AtomicBoolean(false);

        healthById.compute(id,(k,v)->{
            if(v==null){
                v = new ModelHealth();
            }
            if(v.getState()== State.OPEN){
                if(v.getOpenUntil()>now){ //冷却没到
                    return v;
                }
                v.setState(State.HALF_OPEN); //切为半开状态
                v.setHalfOpenInFlight(true); //放行一个请求，作为“探测车”
                allowed.set(true);

                return v;
            }
            if(v.getState()== State.HALF_OPEN){ //已经是半开态了
                if(v.isHalfOpenInFlight()){  //有探测车
                    return v;
                }
                v.setHalfOpenInFlight(true);
                allowed.set(true);
                return v;
            }
            allowed.set(true); //   CLOSE状态直接放行
            return v;
        });
        return   allowed.get();

    }

    /**
     * 不管当前是什么状态，全部重置为初始值。
     * 它最重要的使用场景是 HALF_OPEN 状态下的探测成功。
     * @param id 模型id
     */

    public void markSuccess(String id){
        if(id==null){
            return;
        }
        healthById.compute(id,(k,v)->{
            if(v==null){
                v = new ModelHealth();
            }
            v.setState(State.CLOSE);
            v.setHalfOpenInFlight(false);
            v.setOpenUntil(0L);
            v.setConsecutiveFailures(0);
            return v;
        });
    }

    public void markFailure(String id){
        if(id==null){
            return;
        }
        long now = System.currentTimeMillis();
        healthById.compute(id,(k,v)->{
            if(v==null){
                v = new ModelHealth();
            }
            //半开状态失败，重新进入熔断状态
            if(v.getState()== State.HALF_OPEN){
                v.setState(State.OPEN);//直接熔断
                v.setOpenUntil(now+properties.getSelection().getOpenDurationMs());
                v.setConsecutiveFailures(0);
                v.setHalfOpenInFlight(false);
            }
            //CLOSE状态下失败（正常）-》失败次数加一，若超过阈值，则状态变更为OPEN
            v.setConsecutiveFailures(v.getConsecutiveFailures()+1);
            if(v.getConsecutiveFailures()>=properties.getSelection().getFailureThreshold()){
                v.setState(State.OPEN);
                v.setHalfOpenInFlight(false);
                v.setOpenUntil(now+properties.getSelection().getOpenDurationMs());
                v.setConsecutiveFailures(0);
                return v;
            }
            return v;
        });
    }

}




