# P1 决策记录：AI 助手体系扩展（助手/会话/历史持久化）

> 日期：2026-08-21　状态：**已拍板**　关联：`docs/functional-spec.md` 附 B T6、`docs/开发计划.md` §3.2、P1-1（LLM 端口）、基础问答（chat 流式）
> 背景：现有 AI 助手为单会话内存实现（`src/main/modules/chat/service.ts`），无持久化。用户要求做成类 Cherry Studio 的「助手 → 会话」体系，并持久化历史以支持对话恢复。

## 一、本次拍板（P1-8 任务书输入）

| # | 决策点 | 拍板结果 | 说明 |
|---|---|---|---|
| T6 覆写 | 问答历史持久化 | **持久化**。新增 `ai_assistant` / `ai_session` / `ai_message` 三表，会话历史落库，支持对话恢复 | 原 FSD 附 B T6 建议「问答历史不持久化」**作废**（用户明确要求对话恢复）；FSD 需同步更新 T6 状态 |
| A1 | 助手模型 | 助手 = 名称 + 描述 + **system prompt**（本期仅此三项配置）；模型/知识库/记忆字段预留在表结构但本期不启用 | 未来扩展点：`model_config_json`、`knowledge_base_id` 列预留 |
| A2 | 会话模型 | 助手 1—N 会话；会话 = 标题 + 消息序列；创建时标题「新会话」，首条消息后自动用消息前 20 字命名（若仍未改名） | 会话可重命名、可删除；删除助手级联删会话与消息 |
| A3 | 上下文策略（简单版） | 构造 LLM 请求：`system prompt` + 该会话**最近 20 条**历史消息；超出丢弃最旧 | 长上下文压缩/摘要属未来「上下文管理」，不在本期 |
| A4 | 消息落库时机 | 用户消息发送时即落库；助手回复**完成后**落库（含中止时已生成部分）；LLM 失败则该轮用户消息**回滚不落库** | 与现有失败回滚语义一致，避免半条请求污染历史 |
| A5 | 模型来源 | 会话对话仍用设置页**全局 LLM 配置**（P1-1 已实现）；每助手独立模型属未来「模型设置」 | 本期不改 settings 的 LLM 配置模型 |

## 二、未来扩展路线（列入工作计划，不实现）

| 项 | 内容 | 前置依赖 |
|---|---|---|
| 助手进阶配置 | 每助手独立模型设置（`model_config_json`）、知识库关联（`knowledge_base_id` → RAG） | P1-5 RAG、P1-8 |
| 上下文管理 | 长对话压缩/摘要、token 窗口管理、历史分页优化 | P1-8 后 |
| 记忆系统 | 全局记忆，可在会话中更新，注入 system prompt | 上下文管理后 |
| 工具系统 | 工具调用（目标查询/待办写入/知识库检索等） | LLM 支持 function calling + 记忆后 |

## 三、表设计（新增迁移 `002_ai_assistant.sql`，Drizzle schema 同步）

```sql
CREATE TABLE ai_assistant (
  id CHAR(36) NOT NULL COMMENT '助手 UUID',
  name VARCHAR(50) NOT NULL COMMENT '助手名称',
  description VARCHAR(200) NULL COMMENT '助手简介',
  system_prompt TEXT NOT NULL COMMENT '系统提示词（本期唯一模型配置）',
  model_config_json JSON NULL COMMENT '未来：每助手模型设置，本期 NULL',
  knowledge_base_id CHAR(36) NULL COMMENT '未来：关联知识库，本期 NULL',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 助手';

CREATE TABLE ai_session (
  id CHAR(36) NOT NULL COMMENT '会话 UUID',
  assistant_id CHAR(36) NOT NULL COMMENT '所属助手',
  title VARCHAR(100) NOT NULL DEFAULT '新会话' COMMENT '会话标题',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
  PRIMARY KEY (id),
  KEY ix_ai_session_assistant (assistant_id, updated_at DESC),
  CONSTRAINT fk_ai_session_assistant FOREIGN KEY (assistant_id) REFERENCES ai_assistant(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话';

CREATE TABLE ai_message (
  id CHAR(36) NOT NULL COMMENT '消息 UUID',
  session_id CHAR(36) NOT NULL COMMENT '所属会话',
  role VARCHAR(16) NOT NULL COMMENT '消息角色',
  content MEDIUMTEXT NOT NULL COMMENT '消息内容',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
  PRIMARY KEY (id),
  KEY ix_ai_message_session (session_id, created_at),
  CONSTRAINT fk_ai_message_session FOREIGN KEY (session_id) REFERENCES ai_session(id) ON DELETE CASCADE,
  CONSTRAINT ck_ai_message_role CHECK (role IN ('user', 'assistant'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话消息';
```

- 约定沿用架构文档：UUID `CHAR(36)`、UTC `DATETIME(3)`、InnoDB、utf8mb4；软删除不用于 AI 助手（删除即硬删 + 级联）
- `model_config_json` / `knowledge_base_id` 预留但不建外键（knowledge_base 表尚不存在），未来补
- **严禁**情绪数据进入任何 AI 消息/上下文（沿用 P0 硬约束）
