package org.example.springboot.chat;

import lombok.Getter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 首包等待器 - 用于等待第一个数据包到达的同步工具
 * 支持超时等待，并可区分成功、错误、超时、无内容等不同状态
 */
public class FirstPacketAwaiter {
    //为什么 hasContent 被定义为 final，后面还能通过 .set(true) 改变？
    //核心原因：final 锁死的是“引用”，而不是“对象内部的状态”
    //在 Java 中，如果你把一个引用变量声明为 final，意味着你不能把另一个新的对象重新赋值给这个变量。也就是说，这个指针指向哪里，是一出生就定死、不可更改的。
    //但是，该引用所指向的对象内部的成员变量（状态），只要对象本身允许，依然是可以被修改的。
    //为什么这里一定要加 final？
    //在并发编程中，将这些多线程共享的工具（如 CountDownLatch、AtomicBoolean）声明为 final 是一种极其优秀的实践。
    // 它能保证多线程可见性中的“安全发布”。
    // 确保任何线程在访问 FirstPacketAwaiter 时，看到的都是同一个、已经初始化完美的 hasContent 对象，
    // 绝对不会看到因指令重排导致的 null。

    //countDownLatch：多功能倒计时计数器。
    // 让一个或多个线程静静地等待，直到其他线程把各自手里的任务都干完了（计数器归零），大门才会打开，
    // 等待的线程才能继续往下走。
    private final CountDownLatch countDownLatch = new CountDownLatch(1);//new CountDownLatch(N)：初始化。设定一个初始的计数器数值 N（比如 N=3，表示需要等待 3 件事完成）。
    private final AtomicBoolean hasContent = new AtomicBoolean(false);
    private final AtomicBoolean eventFired = new AtomicBoolean(false);
    private final AtomicReference<Throwable> error = new AtomicReference<>();


    public void markContent() {
        hasContent.set(true);
       fireEventOnce();
    }

    public void markComplete() {
        fireEventOnce();
    }


    public void markError(Throwable throwable) {
        error.set(throwable);
        fireEventOnce();
    }

    //确保只执行一次
    private void fireEventOnce() {

        if (eventFired.compareAndSet(false, true)) {
            countDownLatch.countDown();//计数器减 1！ 其他干活的线程每做完一件事，就调用一次这个方法，计数器就会减 1。当计数器一路减到0的瞬间，所有卡在 await() 的线程都会被瞬间唤醒，大门敞开。
        }
    }
    public Result await(long timeout, TimeUnit unit) throws InterruptedException {
        // countDownLatch.awai（）等！ 调用这个方法的线程会在这里进入阻塞（挂起）状态，就像被门闩挡住了一样，动弹不得。
        //这里的 timeout 和 unit 组合起来，代表的是 “最大容忍的等待时间”（即超时时间）。
        //若超时了，会自动切断等待，强行让线程醒过来，并设置completed为false意味着超时
        boolean completed = countDownLatch.await(timeout, unit);
        if(error.get() != null) {
            return Result.error(error.get());
        }
        if(!completed) {
            return Result.timeout();
        }
        if(!hasContent.get()) {
            return Result.noContent();
        }
        return Result.success();

    }


    /**
     * 结果封装
     */
    @Getter
    public static class Result {

        public enum Type {SUCCESS, ERROR, TIMEOUT, NO_CONTENT}

        private final Type type;
        private final Throwable error;

        private Result(Type type, Throwable error) {
            this.type = type;
            this.error = error;
        }

        public static Result success() {
            return new Result(Type.SUCCESS, null);
        }

        public static Result error(Throwable t) {
            return new Result(Type.ERROR, t);
        }

        public static Result timeout() {
            return new Result(Type.TIMEOUT, null);
        }

        public static Result noContent() {
            return new Result(Type.NO_CONTENT, null);
        }

        public boolean isSuccess() {
            return type == Type.SUCCESS;
        }
    }


}
