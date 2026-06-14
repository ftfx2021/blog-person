package org.example.springboot.chat;

import lombok.NoArgsConstructor;
import okhttp3.Call;

import java.util.concurrent.atomic.AtomicBoolean;

/*
    StreamCancellationHandles 工厂
    为什么要写这么复杂？为了健壮性。在处理 AI 流式输出这种长耗时、多线程的任务时，防止空指针、防止重复操作、及时释放网络资源，是评价一个系统是否“高级”的关键指标。
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class StreamCancellationHandles {
    //返回单例空操作句柄，cancel() 是一个空 lambda () -> {}。用于线程池拒绝等异常场景，流式任务根本没启动，没什么可取消的
    //返回单例空操作句柄，cancel() 是一个空 lambda () -> {}。用于线程池拒绝等异常场景，流式任务根本没启动，没什么可取消的
    private static final StreamCancellationHandle NOOP = () -> {};

    public static  StreamCancellationHandle noop() {
        return NOOP;
    }

    /**
     *构建 OkHttp 取消句柄，这是正常流程使用的
     * @param call
     * @param cancelled
     * @return
     */
    public static StreamCancellationHandle fromOkHttp(Call call, AtomicBoolean cancelled) {
      return   new OkHttpCancellationHandle(call, cancelled);
    }

    private static  final class OkHttpCancellationHandle implements StreamCancellationHandle {


        private final Call call;

        private final AtomicBoolean cancelled;    //记录“是否已经取消”的状态。
        private final AtomicBoolean once = new AtomicBoolean(false); //保证取消逻辑只执行一次。

        private OkHttpCancellationHandle(Call call, AtomicBoolean cancelled) {
            this.call = call;
            this.cancelled = cancelled;
        }
        @Override
        public void cancel() {
            //① CAS 保证只执行一次,即如果已经取消了则直接退出函数
            if(!once.compareAndSet(false, true)) {
                return;
            }
            //② 设置取消信号
            if(cancelled!=null) {
                cancelled.set(true);
            }
            //  ③ 取消 OkHttp 的 HTTP 连接。这一步直接关闭底层 socket，中断网络 I/O
            if(call!=null) {
                call.cancel();
            }
        }
    }
}
