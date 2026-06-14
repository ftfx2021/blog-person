# Agent 调用链测试指南

## 创建的文件

1. **`blog-agent/src/main/java/org/example/springboot/config/AgentServiceConfig.java`**
   - 注册 `LLMService`、`EmbeddingService`、`RerankService` 三个 Bean
   - 解决 Routing*Service 类没有 `@Service` 注解的问题

2. **`blog-web/src/main/java/org/example/springboot/controller/AgentCallChainTestController.java`**
   - 测试控制器，提供所有调用链的测试接口
   - 放在 blog-web 模块（因为它依赖 blog-agent，能访问所有类）

## 前置条件

1. **启动应用**：确保 `application-ai.yml` 中的 API Key 有效
2. **测试工具**：浏览器、Postman、curl 均可

---

## 测试流程（按顺序执行）

### 第一步：一键诊断（必做）

```
GET http://localhost:8080/agent-test/diagnose
```

返回结果会告诉你：
- 配置是否完整
- 模型选择是否返回空（**检测 F-1 缺陷**）
- Bean 是否注入成功
- 诊断结论和下一步建议

### 第二步：模型选择测试

```
GET http://localhost:8080/agent-test/model-select/chat
GET http://localhost:8080/agent-test/model-select/embedding
GET http://localhost:8080/agent-test/model-select/rerank
```

**预期结果**：
- 如果 `isEmpty: true`，说明 **F-1 缺陷存在**（`filter(Objects::isNull)` 写反了）
- 如果返回有效候选列表，说明 F-1 已修复

### 第三步：同步 Chat 测试

```
POST http://localhost:8080/agent-test/chat/sync?prompt=你好
```

**预期结果**：
- 正常：返回模型回复文本
- F-1 存在：返回 `"targets is empty"` 错误
- 配置问题：返回 API Key 缺失等错误

### 第四步：流式 Chat 测试

```
POST http://localhost:8080/agent-test/chat/stream?prompt=介绍Java
```

**预期结果**：
- 正常：SSE 流式返回内容，最终收到 `done` 事件
- F-2 存在：等待 60 秒后超时，返回 `"大模型调用失败"`
- F-1 存在：立即返回 `"targets is empty"` 错误

### 第五步：Embedding 测试

```
POST http://localhost:8080/agent-test/embedding?text=Hello world
```

**预期结果**：
- 正常：返回向量数组和维度信息
- F-1 存在：返回 `"targets is empty"` 错误

### 第六步：Rerank 测试

```
POST http://localhost:8080/agent-test/rerank?query=Java是什么&topN=3
Content-Type: application/json

["Java是一种编程语言", "Python是蛇", "Java是咖啡"]
```

**预期结果**：
- 正常：返回按相关度排序的结果
- F-1 存在：返回 `"targets is empty"` 错误

---

## 缺陷验证对照表

| 测试接口 | 正常响应 | F-1 响应 | F-2 响应 |
|---------|---------|---------|---------|
| `/diagnose` | 未发现明显问题 | `F1_detected: true` | — |
| `/model-select/chat` | 返回候选列表 | `isEmpty: true` | — |
| `/chat/sync` | 返回模型回复 | `targets is empty` | — |
| `/chat/stream` | SSE 流式数据 | `targets is empty` | 60秒超时 |
| `/embedding` | 返回向量 | `targets is empty` | — |
| `/rerank` | 返回排序结果 | `targets is empty` | — |

---

## 配置信息查看

```
GET http://localhost:8080/agent-test/config
```

查看当前 AI 配置：Provider、模型组、熔断策略等。

---

## 注意事项

1. 流式测试使用 SSE，Postman 需选择 "Server-Sent Events" 类型
2. 如果 Bean 未注入，检查 `AgentServiceConfig` 是否被 Spring 扫描到
3. 如果编译报错，可能需要检查 `RetrievedChunk` 的构造函数参数
