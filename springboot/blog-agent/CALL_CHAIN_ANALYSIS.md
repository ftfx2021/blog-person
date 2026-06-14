# blog-agent 调用链缺陷分析报告

> 分析范围：`blog-agent/src/main/java/org/example/springboot/` 下除 `agent/` 目录外的所有包  
> 分析日期：2026-06-14

---

## 一、架构概览

```
调用方 (Controller / Service)
       │
       ▼
┌─────────────────────────────────────────────┐
│  RoutingLLMService / RoutingEmbeddingService / RoutingRerankService  │  ← 路由层
│  (负责故障转移、流式首包探测)                                            │
└──────────────┬──────────────────────────────┘
               │
       ┌───────┴───────┐
       ▼               ▼
  ModelSelector    ModelRoutingExecutor    ← 模型选择 & 执行器
  (候选排序)        (同步 fallback)
       │
       ▼
  ModelHealthStore                         ← 三态熔断器
       │
       ▼
  ChatClient / EmbeddingClient / RerankClient  ← 供应商客户端
  (AbstractOpenAIStyle* 模板方法)
       │
       ▼
  OkHttp → 外部 LLM API
```

---

## 二、致命缺陷（系统不可用）

### 【致命-1】ModelSelector：filter 方向写反，模型选择全部失效

**文件**：`model/ModelSelector.java:109`

```java
// 当前代码（BUG）
return modelCandidateList.stream().map(
        candidate -> buildModelTarget(candidate, providers)
).filter(Objects::isNull).toList();   // ← 只保留 null！
```

**问题**：`buildModelTarget()` 返回 `ModelTarget` 或 `null`（熔断/配置缺失时返回 null）。`.filter(Objects::isNull)` 把所有有效目标过滤掉，只保留 null 值。

**影响链**：
- `selectChatModel()` → 返回空列表或 null 列表 → `ModelRoutingExecutor` 抛出 `"targets is empty"` 或 NPE
- `selectEmbeddingCandidates()` → 同上 → Embedding 全部不可用
- `selectRerankCandidates()` → 同上 → Rerank 全部不可用
- **整个 AI 调用链完全瘫痪，同步/流式均不可用**

**修复**：
```java
.filter(Objects::nonNull).toList();
```

---

### 【致命-2】RoutingLLMService.streamChat：探针回调未传递，流式永远超时

**文件**：`chat/RoutingLLMService.java:89`

```java
// 当前代码（BUG）
FirstPacketAwaiter awaiter = new FirstPacketAwaiter();
ProbeBufferingCallback wrapper = new ProbeBufferingCallback(callback, awaiter);
StreamCancellationHandle handle;
handle = chatClient.streamChat(request, callback, modelTarget);  // ← 传了原始 callback！
```

**问题**：`wrapper`（ProbeBufferingCallback）被创建但从未使用。`chatClient.streamChat()` 收到的是原始 `callback`，而非 `wrapper`。这意味着：
1. `FirstPacketAwaiter` 的 `markContent()/markComplete()/markError()` 永远不会被调用
2. `awaiter.await()` 每次都等到超时（60秒）
3. 每个模型都被判定为 TIMEOUT，标记为失败并切换下一个
4. 所有模型尝试完毕后抛出 `"大模型调用失败"`

**影响**：**流式 Chat 完全不可用**，每次请求都白等60秒后失败。

**修复**：
```java
handle = chatClient.streamChat(request, wrapper, modelTarget);  // ← 应传 wrapper
```

---

## 三、严重缺陷（逻辑错误，可能导致异常行为）

### 【严重-1】ModelHealthStore.markFailure：HALF_OPEN 状态缺少 return，错误穿透

**文件**：`model/ModelHealthStore.java:133-148`

```java
// HALF_OPEN 失败处理
if(v.getState()== State.HALF_OPEN){
    v.setState(State.OPEN);
    v.setOpenUntil(now+properties.getSelection().getOpenDurationMs());
    v.setConsecutiveFailures(0);         // ← 重置为 0
    v.setHalfOpenInFlight(false);
    // ❌ 缺少 return v;
}
// 以下代码对 HALF_OPEN 也执行！
v.setConsecutiveFailures(v.getConsecutiveFailures()+1);  // 0 → 1
if(v.getConsecutiveFailures()>=properties.getSelection().getFailureThreshold()){
    v.setState(State.OPEN);              // 重复设置
    v.setHalfOpenInFlight(false);        // 重复设置
    v.setOpenUntil(now+...);             // 重复设置
    v.setConsecutiveFailures(0);
    return v;
}
return v;
```

**问题**：HALF_OPEN 块没有 `return v;`，执行完后穿透到通用失败计数逻辑：
- `consecutiveFailures` 先被重置为 0，又被加到 1
- OPEN 状态和 openUntil 被重复赋值（虽然值相同，但逻辑不清晰）

**影响**：HALF_OPEN → OPEN 转换后 `consecutiveFailures = 1` 而非预期的 0。下一次 CLOSE 状态失败时 `consecutiveFailures = 2`，如果 `failureThreshold = 2` 则会再次触发熔断。**正常情况下不应在 OPEN 已设定时再走阈值判断。**

**修复**：在 HALF_OPEN 块末尾添加 `return v;`。

---

### 【严重-2】ModelSelector.resolveId：provider 误写为 priority

**文件**：`model/ModelSelector.java:169`

```java
// 当前代码（BUG）
return String.format("%s::%s", candidate.getPriority(), candidate.getModel());
//                            ^^^^^^^^^^^^^^^^^^^^^^^^
//                            应为 candidate.getProvider()
```

**问题**：当候选模型没有显式配置 `id` 字段时，fallback ID 生成使用了 `priority`（如 "100"）而非 `provider`（如 "openai"），生成的 ID 形如 `"100::gpt-4"` 而非 `"openai::gpt-4"`。

**影响**：
1. `ModelHealthStore` 中的熔断状态 key 语义混乱，无法通过 provider 追踪
2. 如果两个不同供应商的模型恰好 priority 相同且 model 相同，会产生 ID 冲突
3. 日志中的 modelId 信息无意义

**修复**：
```java
return String.format("%s::%s", candidate.getProvider(), candidate.getModel());
```

---

## 四、中等缺陷（潜在风险）

### 【中等-1】ModelHealthStore.allowCall：OPEN→HALF_OPEN 切换后 halfOpenInFlight 的竞态窗口

**文件**：`model/ModelHealthStore.java:72-97`

**分析**：`ConcurrentHashMap.compute()` 对同一个 key 加锁，保证了单 key 串行。但存在一个微妙问题：

在 OPEN 状态且冷却时间到的情况下（line 80-84），代码将状态改为 HALF_OPEN 并设置 `halfOpenInFlight = true`。这是正确的。但如果探测请求在 `markSuccess()/markFailure()` 之前，另一个线程调用了 `allowCall()`，由于 `halfOpenInFlight = true`，后续请求会被正确拒绝。

此设计实际上是**正确的**，经仔细验证不存在竞态缺陷。标记为"无问题"。

---

### 【中等-2】RoutingLLMService.streamChat：InterruptedException 后重复通知

**文件**：`chat/RoutingLLMService.java:149-173`

```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    handle.cancel();
    RemoteException interruptedException = new RemoteException(...);
    callback.onError(interruptedException);    // ← 通知 callback
    throw interruptedException;                // ← 又抛异常
}
```

**问题**：`callback.onError()` 通知了下游，同时又抛出异常。上层调用者（streamChat 方法的 for 循环）catch 到异常后会调用 `notifyAllFailed(callback, lastError)`，再次调用 `callback.onError()`。客户端可能收到**两次错误通知**。

**影响**：下游收到重复的错误事件。对于 SSE 推送场景，可能导致已关闭的连接上重复写入。

**修复**：InterruptedException 场景下，直接抛出异常，不再调用 `callback.onError()`；或者在 `notifyAllFailed` 中检查是否已经通知过。

---

### 【中等-3】AbstractOpenAIStyleChatClient.customizeRequestBody：NPE 风险

**文件**：`chat/AbstractOpenAIStyleChatClient.java:62`

```java
protected void customizeRequestBody(JsonObject body, ChatRequest request){
    if(request.getThinking().equals(Boolean.TRUE)){  // ← getThinking() 可能为 null
```

**问题**：`ChatRequest.thinking` 字段未设置时为 `null`，`request.getThinking()` 返回 `null`，调用 `.equals()` 会抛 NPE。

**影响**：当调用方构建 ChatRequest 未显式设置 `thinking` 字段时，同步 Chat 请求会失败。

**修复**：
```java
if(Boolean.TRUE.equals(request.getThinking())){
```

---

### 【中等-4】Embedding 批量请求未排序导致结果错位风险

**文件**：`embedding/AbstractOpenAIStyleEmbeddingClient.java:75-87`

```java
List<List<Float>> res = new ArrayList<>(Collections.nCopies(texts.size(), null));
for(int i=0, n=texts.size(); i<n; i+=batchSize){
    int end = Math.min(i+batchSize, n);
    List<String> slice = texts.subList(i, end);
    List<List<Float>> part = doEmbed(slice, target);
    for(int k=0; k<part.size(); k++){
        res.set(i+k, part.get(k));
    }
}
```

**分析**：这里使用 `set(i+k, ...)` 按索引填充，逻辑上是正确的。但假设 API 返回的 `data` 数组顺序与请求的 `input` 顺序一致（OpenAI 规范保证），则无问题。但如果某些非标准供应商打乱了返回顺序，结果会错位。目前代码对标准 OpenAI 兼容 API 是安全的。

---

## 五、低级缺陷（代码质量）

### 【低-1】RoutingRerankService 未使用的 import

**文件**：`rerank/RoutingRerankService.java:11`

```java
import org.jsoup.select.Collector;  // ← 未使用
```

### 【低-2】ModelCapability.displayName 未正确初始化

**文件**：`emuns/ModelCapability.java:14-18`

```java
private String displayName;
private ModelCapability() { }                    // ← 无参构造不赋 displayName
private ModelCapability(String displayName) {     // ← 有参构造赋 displayName
    this.displayName = displayName;
}
```

**问题**：`CHAT("chat")` 调用的是有参构造，displayName = "chat"。但如果枚举值使用无参构造（当前没有），displayName 为 null。这不是 bug 但存在隐患——如果有人添加新值时不传参数。

---

## 六、修复优先级汇总

| 级别 | 编号 | 文件:行 | 问题 | 影响 |
|------|------|---------|------|------|
| **致命** | F-1 | `ModelSelector.java:109` | `filter(Objects::isNull)` 方向反 | 全系统不可用 |
| **致命** | F-2 | `RoutingLLMService.java:89` | 传原始 callback 而非 wrapper | 流式 Chat 永远超时 |
| **严重** | S-1 | `ModelHealthStore.java:133` | HALF_OPEN 块缺 return | 熔断器计数异常 |
| **严重** | S-2 | `ModelSelector.java:169` | priority 误写为 provider | 模型 ID 语义错误 |
| **中等** | M-1 | `RoutingLLMService.java:169` | 中断后重复通知 | 客户端收到重复错误 |
| **中等** | M-2 | `AbstractOpenAIStyleChatClient.java:62` | getThinking() NPE | 未设 thinking 时崩溃 |
| **低** | L-1 | `RoutingRerankService.java:11` | 无用 import | 代码质量 |

---

## 七、调用链完整度评估

| 链路 | 同步 | 流式 | 状态 |
|------|------|------|------|
| Chat | ❌ (F-1) | ❌ (F-1 + F-2) | 完全不可用 |
| Embedding | ❌ (F-1) | N/A | 完全不可用 |
| Rerank | ❌ (F-1) | N/A | 完全不可用 |

**结论**：当前代码在未修复 F-1 和 F-2 的情况下，所有 AI 调用链路均不可用。
