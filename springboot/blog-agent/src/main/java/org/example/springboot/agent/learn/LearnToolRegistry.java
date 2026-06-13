package org.example.springboot.agent.learn;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 学习版工具注册器：统一封装并注册所有工具为 Spring AI ToolCallback
 */
@Slf4j
@Component
public class LearnToolRegistry {

    private static final String TOOLS_PACKAGE_PREFIX = "org.example.springboot.agent.learn.tools";

    private final ApplicationContext applicationContext;

    private volatile ToolCallback[] allCallbacks = new ToolCallback[0];
    private volatile ToolCallbackProvider allProvider = ToolCallbackProvider.from(new ToolCallback[0]);
    private volatile Map<String, ToolCallback> callbackByName = new LinkedHashMap<>();
    private volatile boolean initialized = false;

    public LearnToolRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 重新扫描并注册工具
     */
    public synchronized void refresh() {
        List<Object> toolBeans = listToolBeans();
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(toolBeans.toArray())
                .build();
        ToolCallback[] callbacks = provider.getToolCallbacks();

        Map<String, ToolCallback> byName = new LinkedHashMap<>();
        for (ToolCallback callback : callbacks) {
            byName.put(callback.getToolDefinition().name(), callback);
        }

        this.allCallbacks = callbacks;
        this.allProvider = ToolCallbackProvider.from(callbacks);
        this.callbackByName = byName;
        this.initialized = true;

        log.info("LearnToolRegistry 注册完成，工具数量: {}", callbacks.length);
    }

    /**
     * 扫描并返回所有可用工具 Bean（给 Tool Calling 用）
     */
    public List<Object> listToolBeans() {
        List<Object> toolBeans = new ArrayList<>();
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(org.springframework.stereotype.Component.class);
        for (String beanName : beanNames) {
            Class<?> beanType = applicationContext.getType(beanName);
            if (beanType == null || beanType.getPackage() == null) {
                continue;
            }
            if (!beanType.getPackage().getName().startsWith(TOOLS_PACKAGE_PREFIX)) {
                continue;
            }

            boolean hasToolMethod = false;
            for (Method method : beanType.getMethods()) {
                if (method.getAnnotation(Tool.class) != null) {
                    hasToolMethod = true;
                    break;
                }
            }
            if (!hasToolMethod) {
                continue;
            }

            Object bean = applicationContext.getBean(beanName);
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            if (targetClass == null || targetClass.getPackage() == null) {
                continue;
            }
            if (hasToolMethod) {
                toolBeans.add(bean);
            }
        }
        return toolBeans;
    }

    /**
     * 扫描并返回所有可用工具定义（给环境扫描展示）
     */
    public List<Map<String, Object>> listTools() {
        ensureInitialized();
        List<Map<String, Object>> tools = new ArrayList<>();
        for (Object bean : listToolBeans()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            if (targetClass == null || targetClass.getPackage() == null) {
                continue;
            }

            for (Method method : targetClass.getMethods()) {
                Tool tool = method.getAnnotation(Tool.class);
                if (tool == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", StringUtils.hasText(tool.name()) ? tool.name() : method.getName());
                item.put("description", tool.description());
                item.put("class", targetClass.getSimpleName());
                item.put("method", method.getName());
                item.put("parameters", extractParameters(method));
                tools.add(item);
            }
        }
        tools.sort(Comparator.comparing(t -> String.valueOf(t.getOrDefault("name", ""))));
        return tools;
    }

    /**
     * 获取全部工具注册器
     */
    public ToolCallbackProvider allProvider() {
        ensureInitialized();
        return allProvider;
    }

    /**
     * 按工具名获取子集注册器；为空时返回全部
     */
    public ToolCallbackProvider providerByNames(List<String> toolNames) {
        return providerByNames(toolNames, ToolExecutionListener.NO_OP);
    }

    /**
     * 按工具名获取子集注册器；为空时返回全部，并可监听工具执行事件
     */
    public ToolCallbackProvider providerByNames(List<String> toolNames, ToolExecutionListener listener) {
        ensureInitialized();
        if (toolNames == null || toolNames.isEmpty()) {
            return applyListener(allToolCallbacks(), listener);
        }
        List<ToolCallback> selected = new ArrayList<>();
        for (String name : toolNames) {
            if (name == null) {
                continue;
            }
            ToolCallback callback = callbackByName.get(name.trim());
            if (callback != null) {
                selected.add(callback);
            }
        }
        if (selected.isEmpty()) {
            return applyListener(allToolCallbacks(), listener);
        }
        return applyListener(selected, listener);
    }

    /**
     * 获取全部工具名
     */
    public List<String> allToolNames() {
        ensureInitialized();
        return new ArrayList<>(callbackByName.keySet());
    }

    /**
     * 根据名称过滤后返回实际可用工具名（不存在的会被忽略）
     */
    public List<String> normalizeToolNames(List<String> toolNames) {
        ensureInitialized();
        if (toolNames == null || toolNames.isEmpty()) {
            return allToolNames();
        }
        List<String> normalized = new ArrayList<>();
        for (String name : toolNames) {
            if (name == null) {
                continue;
            }
            String key = name.trim();
            if (callbackByName.containsKey(key) && !normalized.contains(key)) {
                normalized.add(key);
            }
        }
        return normalized.isEmpty() ? allToolNames() : normalized;
    }

    private List<Map<String, Object>> extractParameters(Method method) {
        List<Map<String, Object>> params = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", parameter.getName());
            ToolParam toolParam = parameter.getAnnotation(ToolParam.class);
            if (toolParam != null) {
                item.put("description", toolParam.description());
                item.put("required", toolParam.required());
            } else {
                item.put("description", "");
                item.put("required", true);
            }
            item.put("type", parameter.getType().getSimpleName());
            params.add(item);
        }
        return params;
    }

    private void ensureInitialized() {
        if (!initialized || callbackByName.isEmpty()) {
            refresh();
        }
    }

    private List<ToolCallback> allToolCallbacks() {
        return new ArrayList<>(callbackByName.values());
    }

    private ToolCallbackProvider applyListener(List<ToolCallback> callbacks, ToolExecutionListener listener) {
        if (callbacks == null || callbacks.isEmpty()) {
            return ToolCallbackProvider.from(new ToolCallback[0]);
        }
        if (listener == null || listener == ToolExecutionListener.NO_OP) {
            return ToolCallbackProvider.from(callbacks);
        }
        List<ToolCallback> wrapped = callbacks.stream()
                .map(callback -> wrapWithListener(callback, listener))
                .toList();
        return ToolCallbackProvider.from(wrapped);
    }

    private ToolCallback wrapWithListener(ToolCallback delegate, ToolExecutionListener listener) {
        return new ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return delegate.getToolDefinition();
            }

            @Override
            public org.springframework.ai.tool.metadata.ToolMetadata getToolMetadata() {
                return delegate.getToolMetadata();
            }

            @Override
            public String call(String toolInput) {
                return callInternal(toolInput, null);
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                return callInternal(toolInput, toolContext);
            }

            private String callInternal(String toolInput, ToolContext toolContext) {
                String toolName = safeToolName(delegate);
                String callId = UUID.randomUUID().toString();
                String argsPreview = preview(toolInput, 600);
                long start = System.currentTimeMillis();
                listener.onToolStart(toolName, callId, argsPreview);

                try {
                    String output = toolContext == null ? delegate.call(toolInput) : delegate.call(toolInput, toolContext);
                    listener.onToolFinish(toolName, callId, "SUCCESS",
                            System.currentTimeMillis() - start, preview(output, 800), null);
                    return output;
                } catch (Exception e) {
                    listener.onToolFinish(toolName, callId, "FAILED",
                            System.currentTimeMillis() - start, "", safeErrorMessage(e));
                    if (e instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private String safeToolName(ToolCallback callback) {
        try {
            return callback.getToolDefinition() == null ? "unknownTool" : callback.getToolDefinition().name();
        } catch (Exception e) {
            return "unknownTool";
        }
    }

    private String safeErrorMessage(Exception e) {
        if (e == null || !StringUtils.hasText(e.getMessage())) {
            return e == null ? "未知异常" : e.getClass().getSimpleName();
        }
        return e.getMessage();
    }

    private String preview(String raw, int maxLength) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String text = raw.trim().replaceAll("\\s+", " ");
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 工具调用监听器
     */
    public interface ToolExecutionListener {
        ToolExecutionListener NO_OP = new ToolExecutionListener() {
        };

        default void onToolStart(String toolName, String callId, String argsPreview) {
        }

        default void onToolFinish(String toolName, String callId, String status, Long costMs, String outputPreview, String errorMessage) {
        }
    }
}
