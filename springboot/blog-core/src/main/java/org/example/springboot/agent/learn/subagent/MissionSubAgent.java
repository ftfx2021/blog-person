package org.example.springboot.agent.learn.subagent;

import lombok.RequiredArgsConstructor;
import org.example.springboot.agent.learn.WorkflowContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 子AGENT：任务识别
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class MissionSubAgent implements SubAgent {

    public static final String CODE = "MISSION_SUB_AGENT";
    public static final String STEP_CODE = "GET_MISSION";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public SubAgentDecision shouldRun(WorkflowContext context) {
        return SubAgentDecision.run("任务识别始终执行");
    }

    @Override
    public void execute(WorkflowContext context, StepExecutionGateway gateway) throws Exception {
        gateway.executeStep(STEP_CODE, context);
    }

    @Override
    public void onFailure(WorkflowContext context, Exception exception, StepExecutionGateway gateway) {
        String question = context.getQuestion() == null ? "" : context.getQuestion().trim();
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("objective", question);
        mission.put("intentType", "KNOWLEDGE_QA");
        mission.put("keywords", List.of());
        mission.put("missingInputs", List.of());
        mission.put("taskSummary", "任务识别失败，主AGENT回退默认任务。");
        mission.put("missionSource", "master-fallback");
        context.setMission(mission);
    }
}
