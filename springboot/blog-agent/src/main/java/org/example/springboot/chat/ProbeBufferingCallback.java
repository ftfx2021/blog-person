package org.example.springboot.chat;

import org.example.springboot.enums.BaseErrorCode;
import org.example.springboot.exception.RemoteException;

import java.util.ArrayList;
import java.util.List;

public  class ProbeBufferingCallback implements StreamCallback {
    private  final StreamCallback downstream; // 下游：数据最终要发给谁
    private final FirstPacketAwaiter awaiter;// 探针：用来"试探"第一个数据包
    //为什么要使用Object定义锁对象，而不是Boolean，String
    //1、new Object()保证 每次调用都创建一个全新的、唯一的实例，不会和任何其他代码共享。
    //2、private final Boolean lock = Boolean.True;或其他类似的写法：Boolean 只有两个实例：Boolean.TRUE 和 Boolean.FALSE，是全局共享的单例！
    //相当于整个 JVM 里只有两把锁（True和False），所有人都挤着用，性能极差甚至死锁。
    //3、private final String lock = new String();  // 确实是一个全新的独立对象
    //功能上可以，但不推荐。 new Object()让人一眼就知道：这就是一把锁，没别的用途。
    //new Object() 是 Java 并发编程中约定俗成的最优选择。
    private final Object lock = new Object();
    private final List<BufferedEvent> bufferedEvents = new ArrayList<>();// 缓冲区
    private  volatile boolean committed;// 是否已"提交"（开门营业）false = 缓存模式，true = 直通模式
    public ProbeBufferingCallback(StreamCallback downstream, FirstPacketAwaiter awaiter) {
        this.downstream = downstream;
        this.awaiter = awaiter;
        this.committed = false;
    }

    @Override
    public void onContent(String content) {
        awaiter.markContent();
        bufferOrDispatch(BufferedEvent.content(content));
    }

    @Override
    public void onThinking(String content) {
        awaiter.markContent();
        bufferOrDispatch(BufferedEvent.thinking(content));
    }

    @Override
    public void onComplete() {
        awaiter.markComplete();
        bufferOrDispatch(BufferedEvent.complete());
    }

    @Override
    public void onError(Throwable error) {
        awaiter.markError(error);
        bufferOrDispatch(BufferedEvent.error(error));
    }


    /**
     * 提交
     * 问题1：为什么要使用snapshot
     * 如果不使用这个，那么代码就要被写成如下：
     *     void commit(){
     *         synchronized (lock) {
     *             if(committed){
     *                 return;
     *             }
     *             committed = true;
     *             if(bufferedEvents.isEmpty()){
     *                 return;
     *             }
     *             // 直接在锁内转发
     *             for (BufferedEvent event : bufferedEvents) {
     *                 dispatch(event);   // 调用 downstream 的方法
     *             }
     *             bufferedEvents.clear();
     *
     *         }
     *     }
     *     这里的dispatch是个黑盒，不知道内部downstream.onContent()会干嘛
     *     若downstream.onContent（）抛出异常、做一件很耗时的事情、或调用本身，会导致卡住、逻辑被打断、死锁
     *
     *     问题2：为什么还没等到派发完，就标记提交、清空缓冲区了
     *     在某些特殊场景（极端并发）下，问题可能会有：
     *     如线程1调用 commit()，在committed设为true和派发时，线程2读到了committed为true,直接派发新数据
     *     但是：谁来调用 commit()？ 是 FirstPacketAwaiter（探针）在检测到第一个数据包到来时才调用
     *     通常 commit() 的时候，上游已经暂停发送了（探针在等第一个包确认）
     *
     *
     */
    void  commit(){
        List<BufferedEvent> snapshot;
        synchronized (lock){//加锁
            if(committed){//如果已经交了就不管了
                return;
            }
            committed = true;//标记为已经提交
            if(bufferedEvents.isEmpty()){  //缓冲区是空的，没什么要提交
                return;
            }
            snapshot = new   ArrayList<>(bufferedEvents);//取出快照
            bufferedEvents.clear();//清空缓冲区
        }
        //拿到快照慢慢转发
        for (BufferedEvent bufferedEvent : snapshot) {
            dispatch(bufferedEvent);
        }

    }

//    void commit(){
//        synchronized (lock) {
//            if(committed){
//                return;
//            }
//            committed = true;
//            if(bufferedEvents.isEmpty()){
//                return;
//            }
//            // ❌ 直接在锁内转发
//            for (BufferedEvent event : bufferedEvents) {
//                dispatch(event);   // 调用 downstream 的方法
//            }
//            bufferedEvents.clear();
//
//        }
//    }
    //数据到来
    private void bufferOrDispatch(BufferedEvent bufferedEvent) {
        boolean dispatchNow;
        synchronized (lock) {
            dispatchNow = committed;  //读取当前状态
            if(!dispatchNow) { //如果还不用提交
                bufferedEvents.add(bufferedEvent);//缓存起来
            }
        }
        if(dispatchNow) {
            dispatch(bufferedEvent);
        }
    }

    // 真正的转发，downstream是个回调
    private void dispatch(BufferedEvent bufferedEvent) {
        switch (bufferedEvent.type) {

            //下游写法可能是这样：
            //StreamCallback callback;
            // service.streamChat(callback)
            case CONTENT -> downstream.onContent(bufferedEvent.content);
            case THINKING ->  downstream.onThinking(bufferedEvent.content);
            case COMPLETE -> downstream.onComplete();
            case ERROR -> downstream.onError(bufferedEvent.error!=null?bufferedEvent.error:new RemoteException("流式请求失败", BaseErrorCode.REMOTE_ERROR));
        }
    }


    private record BufferedEvent(EventType type, String content, Throwable error) {

        private static BufferedEvent content(String content) {
            return new BufferedEvent(EventType.CONTENT, content, null);
        }

        private static BufferedEvent thinking(String content) {
            return new BufferedEvent(EventType.THINKING, content, null);
        }

        private static BufferedEvent complete() {
            return new BufferedEvent(EventType.COMPLETE, null, null);
        }

        private static BufferedEvent error(Throwable error) {
            return new BufferedEvent(EventType.ERROR, null, error);
        }
    }

    private enum EventType {
        CONTENT,
        THINKING,
        COMPLETE,
        ERROR
    }
}
