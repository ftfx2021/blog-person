package org.example.springboot.agent.learn.steps;

import lombok.RequiredArgsConstructor;
import org.example.springboot.agent.learn.LearningAgentReasoningService;
import org.example.springboot.agent.learn.WorkflowContext;
import org.example.springboot.agent.learn.WorkflowStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 步骤1：明确任务（Get the Mission）
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class GetMissionStep implements WorkflowStep {

    private final LearningAgentReasoningService reasoningService;

    @Override
    public String stepCode() {
        return "GET_MISSION";
    }

    @Override
    public void execute(WorkflowContext context) {
        String question = context.getQuestion() == null ? "" : context.getQuestion().trim();
        LearningAgentReasoningService.MissionDecision missionDecision =
                reasoningService.inferMission(question, context.getHistory());

        String intentType;
        List<String> keywords;
        List<String> missingInputs;
        String objective;
        String taskSummary;

        if (missionDecision != null) {
            intentType = missionDecision.intentType();
            keywords = missionDecision.keywords();
            missingInputs = missionDecision.missingInputs();
            objective = missionDecision.objective();
            taskSummary = missionDecision.taskSummary();
        } else {
            intentType = "KNOWLEDGE_QA";
            keywords = List.of();
            missingInputs = List.of();
            objective = question;
            taskSummary = "模型任务识别失败，使用默认任务类型。";
        }

        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("objective", objective);
        mission.put("intentType", intentType);
        mission.put("keywords", keywords);
        mission.put("missingInputs", missingInputs);
        mission.put("taskSummary", taskSummary);
        mission.put("missionSource", missionDecision != null ? "spring-ai" : "rule-fallback");
        context.setMission(mission);
    }
}
