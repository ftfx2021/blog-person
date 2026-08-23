# 人生系统 · 项目长期记忆

## 项目阶段（六阶段流程）
①产品定义 ✅ → ②PRD ✅ → ③架构 ✅ → ④原型设计 ✅（13 页高保真 + FSD）→ ⑤开发实现（当前，P0 脚手架任务书已交付）→ ⑥测试发布

## ⑤开发方式（已定）
- 用户不让本 agent 写代码：产出任务书 prompt 交给另一个模型搭建
- `docs/prompts/P0脚手架任务书.md`：P0 范围 = G01~G04 + H01 + S01 的 P0 项 + K02-F07 全文搜索；工程根 `life-system/` 子目录；ORM=Drizzle+mysql2、UI=Element Plus、打包=electron-builder（已锁定）；用法 A（模型可读文件）/B（附 FSD 详述）双模式
- 验收按任务书第八节 8 条核对；P1/P2 只留目录空壳+TODO；冲突记入 docs/decisions/

## 关键文档索引
- `docs/设计文档.md`（v0.2，范围与交互权威依据）｜`PRD&功能优先级.md`（P0/P1/P2 + MVP-1/2/3）
- `docs/architecture.md`（MySQL schema、Milvus、RAG 管线、备份）｜`docs/functional-spec.md`（FSD v1.0，⑤唯一需求依据，2026-08-19）
- `docs/UX原型设计-低保真-v0.1.md`｜`prototype/`（13 页高保真，深空暖光色系）
- 优先级：设计文档 > PRD > 架构文档 > 原型；原型不构成范围边界

## 已拍板的硬约束（不再重议）
- 存储已拍板 MySQL + Milvus 本地部署；连接信息用户配置，部署责任在用户
- Electron + Vue3 + TS 全栈；离线优先；本地 embedding + LLM API
- 目标进度只由数据点/里程碑驱动（投入≠结果）；无四象限/优先级
- 情绪数据不进 RAG/知识库/全局搜索（6 条可测规则见 FSD 3.3）
- 备份=一键备份/恢复（mysqldump + Milvus 快照），不做版本历史/冲突检测
- 沟通约定：少用商业术语（MVP/交付/北极星），"分阶段"只表述为开发顺序

## FSD 待确认项（T1~T18，见 functional-spec.md 附 B，开发前需拍板）
高影响项：T9 阶段历史需新增表（推荐 dev_project_stage_log）、T6 问答历史不持久化、T10 终点阶段=维护、T13 usage_tags 存储、T16 周报 LLM 输入颗粒度

## 用户偏好
- 简洁、结构化表达，表格优先；渐进式需求澄清（先看结果再细化）
- 视觉：深空暖光色系（深夜蓝 #1E2A4A + 暖橙 #FF9F45）；禁 emoji 插画；男生向
