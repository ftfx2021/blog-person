# P1-1 LLM 与 Embedding 实现记录

## 已落实的决策

- LLM 通过 Node 原生 `fetch` 调用 OpenAI 兼容的 `/v1/chat/completions`，不引入任何 AI SDK 或框架。
- LLM 端口同时支持 OpenAI `/v1/responses`；设置页通过协议字段选择 Chat Completions 或 Responses。
- 两种协议均支持非流式完整结果和 SSE 流式增量；推理模型的 `reasoning_content` 或 Responses reasoning 摘要保存在可选 `reasoning` 字段。
- Embedding 仅连接本机 Ollama 的 `/v1/embeddings`，配置与 LLM 分离，并返回向量维度。
- 请求超时 30 秒；网络错误和 5xx 按 1/2/4 秒退避重试，4xx 不重试，401/403 映射为 `AI_AUTH_ERROR`。
- API Key 使用 Electron `safeStorage` 加密后写入 `app_setting`，渲染层只接收掩码值。
- 未配置或连接失败统一返回健康状态，不阻断 MySQL、提醒、备份等核心设置功能。

## 本期边界

本期不实现 Milvus、分块、索引、RAG 和业务页面 AI 调用；provider 已提供流式/推理端口，后续业务模块负责把增量事件接入具体页面。
