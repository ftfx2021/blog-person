package org.example.springboot.agent.learn.subagent;

/**
 * 子AGENT决策
 */
public record SubAgentDecision(
        String decision,
        String reason
) {

    public static SubAgentDecision run(String reason) {
        return new SubAgentDecision("RUN", reason);
    }

    public static SubAgentDecision skip(String reason) {
        return new SubAgentDecision("SKIP", reason);
    }
}
