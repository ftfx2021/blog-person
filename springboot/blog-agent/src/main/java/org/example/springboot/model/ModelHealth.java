package org.example.springboot.model;

import lombok.Data;
import org.example.springboot.emuns.State;

@Data
public class ModelHealth {

    private int consecutiveFailures;//连续失败次数
    private long openUntil;//熔断截至时间戳
    private boolean halfOpenInFlight; //是否有探测请求在飞行中，指的是请求发出去，还没收到结果
    private State state; //当前状态


    public ModelHealth(){
        this.consecutiveFailures = 0;
        this.openUntil = 0;
        this.halfOpenInFlight = false;
        this.state = State.CLOSE;
    }
}
