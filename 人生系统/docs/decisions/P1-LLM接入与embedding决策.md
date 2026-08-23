# P1 决策记录：LLM 接入方式与 Embedding 方案

> 日期：2026-08-20　状态：**已拍板**　关联：`docs/architecture.md` §10 待确认项、`docs/开发计划.md` §3.1
> 记录目的：冻结 P1 任务书的输入，后续任务书直接引用本文件。

## 一、四项拍板决策

| # | 待确认项 | 拍板结果 | 说明 |
|---|---|---|---|
| D1 | embedding 模型分发 | **Ollama 本地服务接入**；模型名称与连接信息由用户在设置页配置 | 替代原推荐（Transformers.js 内嵌 bge-small-zh-v1.5 首启下载）。应用不再内置模型与 ONNX 运行时；Ollama 部署责任在用户（与 MySQL/Milvus 同一模式） |
| D2 | 导入原件保存 | **复制进应用数据目录**，原路径仅作来源记录 | 原文件移动/删除后仍可重建 |
| D3 | 文档删除保留期 | **软删除 30 天后清理** | 误删可恢复，30 天后释放存储 |
| D4 | LLM 供应商 | **兼容主流供应商，连接信息手动配置**（baseURL + apiKey + model） | 不做单一厂商绑定 |

## 二、LLM 客户端：手搓薄适配层（已定，推荐理由见 §三）

**方案**：自建 Provider Adapter，统一走 **OpenAI 兼容协议**（`/v1/chat/completions` + `/v1/embeddings`），用主进程 Node 原生 `fetch` 实现，**不引入 LangChain.js / LlamaIndex / Vercel AI SDK 等框架**。

接口面（单个 adapter 文件即可）：
```ts
interface LlmProvider {
  chat(req: { messages, model?, stream? }): Promise<ChatResult>;   // 非流式优先，流式二期
  embed(texts: string[]): Promise<number[][]>;                     // 批量 embedding
  healthCheck(): Promise<{ ok: boolean; detail: string }>;          // 设置页健康状态
}
interface EmbeddingProvider {   // Ollama 独立于 chat，可单独配置
  embed(texts: string[]): Promise<{ vectors: number[][]; model: string; dim: number }>;
}
```

配置模型（设置页 S01-F03 扩展）：
- Chat：`provider 预设或自定义` + `baseURL` + `apiKey` + `model`
- Embedding：`baseURL`（默认 Ollama `http://127.0.0.1:11434`）+ `embeddingModel`
- 内置预设下拉：OpenAI / DeepSeek / 通义千问 / Kimi / 智谱 / Ollama / 自定义——全部走 OpenAI 兼容端点

## 三、为什么手搓而不是用框架

| 维度 | 手搓薄适配层（选用） | LangChain.js / LlamaIndex | Vercel AI SDK |
|---|---|---|---|
| 覆盖需求 | chat + embed 两个端点，100% 覆盖本应用 | 大量无用抽象（chain/agent/tool/memory） | 面向 React/Next 流式渲染，主进程用不上 |
| 依赖与体积 | 零依赖（Node 原生 fetch） | 数十个传递依赖，打包体积与审计面增大 | 面向 Web 框架生态 |
| API 稳定性 | 自持，可控 | 版本激进、破坏性变更频繁 | 跟随前端生态迭代 |
| 调试成本 | 请求/重试/降级逻辑全在自己手里，单用户桌面好排错 | 多层抽象，问题难定位 | 需适配 Node 非浏览器环境 |
| 兼容性 | OpenAI 兼容端点覆盖 OpenAI/DeepSeek/通义/Kimi/智谱/豆包/Ollama/LM Studio/vLLM | 抽象层反而常出现 provider 差异 bug | 支持广但绑定其生态 |
| 扩展性 | 接口抽象保留；未来接入 Anthropic 原生协议只加一个实现类 | 不需要 | 不需要 |

**结论**：单用户桌面应用只需"发一个 HTTP 请求拿回答/向量"，框架解决的是"多步骤编排、agent、记忆、工具调用"等本应用明确不需要的问题。手搓 100~200 行适配层，比引入框架更小、更稳、更好维护。

## 四、D1 变更的影响（任务书必须处理）

1. **ADR-003 更新**：embedding 运行时从"内置 Transformers.js + ONNX Worker"改为 **Ollama 本地服务端点**；Worker Thread 原职责（ONNX 推理）消失，embedding 调用改为普通网络请求。架构文档 §5 的 Embedding 端口说明同步。
2. **Milvus 维度配置化**：架构原写 `FLOAT_VECTOR[512]`（bge-small-zh-v1.5 维度）。Ollama 模型维度各异（nomic-embed-text=768、bge-m3=1024、mxbai-embed-large=1024），**collection 维度必须由所选 embedding 模型的输出维度决定**：创建/重建 collection 时读取模型维度，Milvus 侧按 `model_version + dim` 区分 collection/partition。
3. **中文 embedding 质量验证点**：Ollama 官方库中文效果较好的选项需实测（nomic-embed-text 中文一般、bge-m3 更佳但体积大）。P1-5 RAG 验收前，用 100 条真实文档做召回抽样对比，模型名可随时换（配置化天然支持）。
4. **离线语义**：Ollama 为本机服务，无公网依赖，离线优先承诺保持；但 AI 功能依赖 Ollama 可用性，断连降级 `AI_UNAVAILABLE` 并在设置页展示健康状态。
5. **LLM 与 embedding 分离配置**：chat 走远端厂商（D4），embedding 走本地 Ollama（D1），两者独立配置、独立健康检查。

## 五、连带确认（一并冻结）

- embedding 请求在**主进程**执行（替代原 Worker 方案，因无 ONNX 推理），超时/重试策略：单次超时 30s，失败置 chunk 状态 `failed` 并保留"重试索引"入口（不变更架构索引一致性流程）。
- 设置页新增两个区块：**LLM 连接**（chat 供应商配置 + 测试）、**Embedding 连接**（Ollama 地址 + 模型 + 测试，显示模型维度）。
