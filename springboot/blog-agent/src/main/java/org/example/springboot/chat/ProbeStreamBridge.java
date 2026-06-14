package org.example.springboot.chat;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 可用于替换 FirstPacketAwaiter
 */
public class ProbeStreamBridge implements StreamCallback {

    private final StreamCallback downstream;
    //CompletableFuture 就是一个"单次通信管道"：一头 complete() 发值，一头 get() 等值。
    // 发了就唤醒等的人，而且只能发一次，天然保证结果不会被覆盖。 比手动写 wait/notify 简单安全得多。
// // 创建一个"快递单"（此时里面没有值）
//CompletableFuture<String> future = new CompletableFuture<>();
//// 一小时后，异步线程"发货"
//future.complete("你的包裹");
//// 主线程一直"等快递"
//String result = future.get();   // 阻塞，直到有值才返回
    /// / result = "你的包裹"
    private final CompletableFuture<ProbeResult> probe = new CompletableFuture<>();

    private final Object lock = new Object();
    //Runnable 就是把一段代码打包装进一个对象里，可以存变量、存集合、传来传去，需要的时候 .run() 一下就执行了
    // 只有一个run()方法，没有参数，没有返回值
    //Runnable r = () -> System.out.println("你好");
    //// 存起来
    //List<Runnable> tasks = new ArrayList<>();
    //tasks.add(r);
    /// / 一小时后再执行
    //tasks.forEach(Runnable::run);
    private final List<Runnable> buffer = new ArrayList<>();

    private volatile boolean committed;
    public ProbeStreamBridge(StreamCallback downstream) {
        this.downstream = downstream;
    }
    //异步线程（发货方）
    // 不管下面的几个方法哪个先到，只有第一个 complete() 有效。
    @Override
    public void onContent(String content) {
        // 收到内容 → 发货（探测成功）
        probe.complete(ProbeResult.success());
        bufferOrDispatch(()->downstream.onContent(content));
    }

    @Override
    public void onComplete() {
        // 流结束没内容 → 发货（无内容）
        probe.complete(ProbeResult.noContent());
        //每个 lambda 就是一个待执行的 Runnable，缓冲在 List<Runnable> 中。commit 时遍历列表执行每个 lambda：
        bufferOrDispatch(()->downstream.onComplete());
    }


    @Override
    public void onError(Throwable t) {
    // 出错 → 发货（错误）
        probe.complete(ProbeResult.error(t));
        bufferOrDispatch(() -> downstream.onError(t));
    }
    @Override
    public void onThinking(String content) {
        probe.complete(ProbeResult.success());
        bufferOrDispatch(() -> downstream.onThinking(content));
    }

    ProbeResult awaitFirstPacket(long timeout, TimeUnit unit) throws InterruptedException {
        ProbeResult result;
        try {
            //主线程（收货方）
            // 阻塞等待，最多等 timeout unit
            result = probe.get(timeout, unit);
        }catch (TimeoutException e){
            /// 时间到了，没人 complete → 超时
            result = ProbeResult.timeout();
        }catch (ExecutionException e){
            return ProbeResult.error(e.getCause());
        }
        if (result.isSuccess()) {
            commit();
        }
        return result;

    }

    private void commit(){
        synchronized (lock) {
            if(committed){
                return;
            }
            committed = true;
            buffer.forEach(Runnable::run);
        }
    }

    private void bufferOrDispatch(Runnable action){
        boolean dispatchNow;
        synchronized (lock){
                dispatchNow =committed;
                if(!dispatchNow){
                    buffer.add(action);
                }

        }
        if(dispatchNow){
            action.run();
        }

    }

    @Getter
    static class ProbeResult {

        enum Type { SUCCESS, ERROR, TIMEOUT, NO_CONTENT }

        private final Type type;
        private final Throwable error;

        private ProbeResult(Type type, Throwable error) {
            this.type = type;
            this.error = error;
        }

        static ProbeResult success() { return new ProbeResult(Type.SUCCESS, null); }
        static ProbeResult error(Throwable t) { return new ProbeResult(Type.ERROR, t); }
        static ProbeResult timeout() { return new ProbeResult(Type.TIMEOUT, null); }
        static ProbeResult noContent() { return new ProbeResult(Type.NO_CONTENT, null); }

        boolean isSuccess() { return type == Type.SUCCESS; }
    }

}
