# 人生系统《功能规格》

> 版本：v1.0
> 
> 依据：`docs/设计文档.md`（范围与业务规则）、`PRD&功能优先级.md`（优先级与发布门）、`docs/architecture.md`（schema 与技术约束）。原型仅用于确认入口，不作为范围边界。

## 0. 约定

- 优先级：P0=MVP-1，P1=MVP-2，P2=MVP-3。发布门是工程顺序，不删除已确认范围。
- 时间字段统一保存 UTC `DATETIME(3)`，展示层转换为本地时间；日期字段按用户本地日计算。
- 所有 ID 为 UUID；业务事实写入 MySQL，Milvus 仅保存可重建的知识向量。
- “删除”除文档外为确认后硬删除并清理关联；文档使用软删除并保留恢复窗口。
- 时间线、情绪、提醒与洞察均不改变目标进度；任务/习惯投入不得换算为目标结果。

## 第一部分：功能清单总表

### G 目标行动线

| 功能编号 | 功能名 | 优先级 | 一句话描述 |
|---|---|---:|---|
| G01-F01 | 创建目标 | P0 | 创建年度/季度/月度目标并选择数值、里程碑或状态度量 |
| G01-F02 | 编辑目标 | P0 | 修改目标描述、周期、度量参数和标签 |
| G01-F03 | 删除目标 | P0 | 删除目标并解除可空关联 |
| G01-F04 | 完成目标 | P0 | 将进行中目标标记为已完成 |
| G01-F05 | 放弃目标 | P0 | 将进行中目标标记为已放弃 |
| G01-F06 | 记录数据点 | P0 | 为数值型目标记录真实结果值 |
| G01-F07 | 查看目标进度与趋势 | P0 | 按目标类型计算进度并查看数据点趋势 |
| G01-F08 | 创建里程碑 | P0 | 为里程碑型目标新增可排序子项 |
| G01-F09 | 完成/撤销里程碑 | P0 | 勾选或撤销里程碑并重算进度 |
| G01-F10 | 编辑/删除里程碑 | P0 | 修改里程碑内容或移除未完成子项 |
| G02-F01 | 创建生活项目 | P0 | 创建可独立存在或挂目标的行动容器 |
| G02-F02 | 编辑生活项目 | P0 | 修改项目资料、起止时间、标签和目标归属 |
| G02-F03 | 删除生活项目 | P0 | 删除项目并将其待办的 project_id 置空 |
| G02-F04 | 更新生活项目状态 | P0 | 在 active、paused、done 之间切换项目状态 |
| G03-F01 | 创建待办 | P0 | 以标题为最小输入创建可选挂目标/项目的待办 |
| G03-F02 | 编辑待办 | P0 | 修改标题、备注、截止日期及可空归属 |
| G03-F03 | 删除待办 | P0 | 删除待办并清理其关联 |
| G03-F04 | 推进/撤销待办状态 | P0 | 按 todo→doing→done 推进，完成后可撤销回 todo |
| G03-F05 | 筛选与排序待办 | P0 | 按截止时间、状态、目标和项目查看待办 |
| G03-F06 | 查看目标支持行动 | P0 | 在目标详情中列出挂载该目标的项目和待办 |
| G04-F01 | 创建习惯 | P0 | 创建 daily 或每周次数型习惯 |
| G04-F02 | 编辑习惯 | P0 | 修改习惯名称、说明和频率参数 |
| G04-F03 | 删除习惯 | P0 | 删除习惯及其打卡历史 |
| G04-F04 | 习惯打卡 | P0 | 记录指定本地日期的一次打卡并更新连续天数 |
| G04-F05 | 撤销习惯打卡 | P0 | 撤销当天打卡并回算连续天数 |
| G04-F06 | 查看习惯历史 | P0 | 查看打卡日历、当前 streak 和最近打卡日 |

### K 知识沉淀线

| 功能编号 | 功能名 | 优先级 | 一句话描述 |
|---|---|---:|---|
| K01-F01 | 存入收藏箱 | P1 | 暂存链接、片段或稍后读条目 |
| K01-F02 | 编辑收藏 | P1 | 修改收藏标题、链接和备注 |
| K01-F03 | 剪藏入库 | P1 | 抓取网页正文并生成知识库文档 |
| K01-F04 | 仅保留链接 | P1 | 将收藏标记为 bookmarked 而不抓取正文 |
| K01-F05 | 丢弃/恢复收藏 | P1 | 丢弃收藏或从 discarded 恢复为 pending |
| K02-F01 | 导入文档 | P1 | 导入 PDF、DOCX、Markdown、TXT、HTML 或平台导出文件 |
| K02-F02 | 创建手写文档 | P1 | 创建 note、skill 或 prompt 类型知识文档 |
| K02-F03 | 编辑文档 | P1 | 修改文档标题、正文、来源和类型相关元数据 |
| K02-F04 | 管理文档标签 | P1 | 通过 tag/entity_tag 增删标签并按标签过滤 |
| K02-F05 | 删除/恢复文档 | P1 | 软删除文档、排除检索并在保留期内恢复 |
| K02-F06 | 查看文档索引状态 | P1 | 查看解析、分块、向量化状态及错误摘要 |
| K02-F07 | 基础全文搜索 | P0 | 对标题和正文做可解释的关键词检索 |
| K02-F08 | 导出文档 | P1 | 将文档转换为 PDF、Markdown 或 TXT |
| K02-F09 | 复制 Prompt 模板 | P1 | 一键复制 prompt 正文并保留 usage_tags |
| K02-F10 | 执行统一文档入库 | P1 | 将各采集来源统一编排为解析、规范化、分块和持久化流程 |
| K02-F11 | 解析文档正文 | P1 | 按文档类型提取正文和可追溯的来源定位信息 |
| K02-F12 | 规范化文档内容 | P1 | 将解析结果统一为规范正文和来源元数据 |
| K02-F13 | 文档分块 | P1 | 按标题、段落和 token 边界生成可引用 chunk |
| K03-F01 | 创建向量索引 | P1 | 对文档全部 chunk 生成 embedding 并写入 Milvus |
| K03-F02 | 重试/重建向量索引 | P1 | 对 failed、stale 或模型变更文档重新建立索引 |
| K03-F03 | 校验索引一致性 | P1 | 校验 MySQL chunk 与 Milvus 向量数量和标识一致 |
| K03-F04 | 清理旧版本向量 | P1 | 新索引完整就绪后清理旧向量和软删除文档向量 |
| K03-F05 | 语义检索 | P1 | 对已就绪知识文档执行向量相似度检索 |
| K03-F06 | RAG 问答 | P1 | 基于命中 chunk 生成带真实引用的回答 |
| K03-F07 | 追问与查看引用 | P1 | 在当前问答上下文追问并打开引用原文定位 |
| K04-F01 | 创建沉淀条目 | P1 | 创建 diary、idea、decision、reading 或 review 时间线记录 |
| K04-F02 | 编辑沉淀条目 | P1 | 修改条目标题、内容、类型和时间 |
| K04-F03 | 删除沉淀条目 | P1 | 删除时间线条目并清理双向关联 |
| K04-F04 | 关联实体 | P1 | 通过 entity_link 关联目标、项目、待办、文档等实体 |
| K04-F05 | 查看/筛选时间线 | P1 | 按时间倒序和类型筛选沉淀记录 |
| K04-F06 | 升格为知识文档 | P1 | 将时间线条目原样或编辑后复制为 promoted 文档 |
| K04-F07 | 周复盘 | P1 | 用固定三问生成 review 条目并关联本周实体 |

### M 心理健康

| 功能编号 | 功能名 | 优先级 | 一句话描述 |
|---|---|---:|---|
| M01-F01 | 管理自定义情绪词 | P1 | 新增可复用的自定义情绪词 |
| M01-F02 | 记录情绪事件 | P1 | 记录情绪命名、强度、事件、情境和需求 |
| M01-F03 | 编辑情绪记录 | P1 | 修正情绪记录内容和记录时间 |
| M01-F04 | 删除情绪记录 | P1 | 删除错误或不再保留的情绪记录 |
| M01-F05 | 查看情绪分布与趋势 | P1 | 按时间范围统计情绪词分布和强度趋势 |
| M01-F06 | 生成情绪周报 | P1 | 将统计快照提交 LLM 生成非诊断性总结与建议 |
| M01-F07 | 查看历史周报 | P1 | 查看已生成周报及其周期和模型信息 |

### D 项目交付

| 功能编号 | 功能名 | 优先级 | 一句话描述 |
|---|---|---:|---|
| D01-F01 | 创建项目想法 | P2 | 低门槛记录标题和描述，状态为 inbox |
| D01-F02 | 编辑项目想法 | P2 | 修改想法标题和描述 |
| D01-F03 | 删除项目想法 | P2 | 删除未立项想法并清理评估信息 |
| D01-F04 | 进入评估 | P2 | 将 inbox 想法转为 evaluating |
| D01-F05 | 提交想法评估 | P2 | 记录价值、可行性 1~5 分和评估备注 |
| D01-F06 | 批准/放弃想法 | P2 | 将评估结果转为 approved 或 discarded |
| D01-F07 | 复活已放弃想法 | P2 | 将 discarded 想法恢复为 inbox 或 evaluating |
| D01-F08 | 一键立项 | P2 | 从 approved 想法创建开发项目并回填关联 |
| D02-F01 | 创建开发项目 | P2 | 创建独立于生活项目的开发项目 |
| D02-F02 | 编辑开发项目 | P2 | 修改名称、描述、仓库链接和状态 |
| D02-F03 | 删除/归档开发项目 | P2 | 删除项目或转为 archived 保留历史 |
| D02-F04 | 配置项目阶段 | P2 | 新增、改名、排序阶段并标记终止阶段 |
| D02-F05 | 查看开发看板 | P2 | 按阶段分组展示开发项目 |
| D02-F06 | 拖动切换阶段 | P2 | 将项目移动到指定阶段并同步 current_stage_id |
| D02-F07 | 手动推进阶段 | P2 | 推进到下一阶段或退回上一阶段 |
| D02-F08 | 查看阶段进度 | P2 | 根据当前阶段和终止阶段计算阶段进度 |

### H Dashboard 与回顾

| 功能编号 | 功能名 | 优先级 | 一句话描述 |
|---|---|---:|---|
| H01-F01 | 查看今日行动 | P0 | 首屏集中展示今日待办和习惯打卡 |
| H01-F02 | 查看目标摘要 | P0 | 展示近期目标、进度和到期信息 |
| H01-F03 | 查看站内提醒 | P0 | 聚合目标到期、习惯打卡和数据录入提示 |
| H01-F04 | 全局基础搜索 | P0 | 在支持的实体中按标题、正文、标签、类型检索 |
| H01-F05 | 查看洞察 | P2 | 展示目标趋势、逾期模式和复盘摘要，仅作自我观察 |
| H01-F06 | 日检视 | P2 | 按固定流程回顾当天行动并可生成 review 条目 |
| H01-F07 | 月度回顾 | P2 | 汇总月度目标、行动、情绪和沉淀数据 |
| H01-F08 | 季度规划 | P2 | 以季度 period 目标为入口制定下一阶段计划 |
| H01-F09 | 日历视图 | P2 | 以 task.due_date 展示待办和目标截止日期 |
| H01-F10 | 时间预算 | P2 | 规划任务时长预算（当前 schema 未提供时长字段） |
| H01-F11 | 查看主动推荐 | P2 | 基于数据给出候选行动建议，默认关闭，只建议不执行 |

### S 设置与系统

| 功能编号 | 功能名 | 优先级 | 一句话描述 |
|---|---|---:|---|
| S01-F01 | 保存数据源配置 | P0 | 保存 MySQL/Milvus 连接信息和健康检查参数 |
| S01-F02 | 测试数据源连接 | P0 | 分别测试 MySQL、Milvus 可用性并显示状态 |
| S01-F03 | 配置 LLM 密钥 | P1 | 保存 provider、endpoint、模型和密钥引用 |
| S01-F04 | 配置提醒与频率 | P0 | 开关一级/二级/三级主动性及频率 |
| S01-F05 | 配置降噪选项 | P0 | 控制站内通知聚合和推荐默认关闭策略 |
| S01-F06 | 一键备份 | P0 | 生成 MySQL dump、Milvus 快照、原件副本和 manifest |
| S01-F07 | 一键恢复 | P0 | 校验备份后整体恢复 MySQL 与 Milvus |
| S01-F08 | 导出 JSON | P0 | 导出版本化业务 DTO 并校验记录数与 hash |
| S01-F09 | 导出 Markdown/TXT | P0 | 导出时间线和文档的规范化文本 |
| S01-F10 | 查看系统任务状态 | P0 | 查看备份、恢复、解析、索引等后台任务进度和错误 |

## 第二部分：逐功能详述

### G01 目标

#### G01-F01 创建目标（P0）

- **入口**：目标列表页「新建目标」按钮。
- **主要字段**：`title`（必填，字符串，≤50字）；`description`（可选，文本）；`period`（必填，枚举，默认 `quarterly`）；`metric_type`（必填，枚举）；数值型的 `unit`、`start_value`、`target_value`；`due_date`（可选日期）；标签（通过 `tag`/`entity_tag`，可选）。
- **业务规则**：① `metric_type=numeric` 时三项数值字段必须存在且 `target_value≠start_value`；非 numeric 不得填起点/目标值；② 周期只能 annual/quarterly/monthly；③ 创建后 `status=active`；④ 目标不自动创建项目或待办；⑤ 空数据保存失败不产生半成品。
- **输出**：成功进入目标详情并显示初始进度；失败在表单内指出字段错误。

#### G01-F02 编辑目标（P0）

- **入口**：目标详情页「编辑」。
- **主要字段**：同创建字段；`updated_at` 由系统写入。
- **业务规则**：① 已有 `goal_record` 的 numeric 目标修改起点/目标值前必须二次确认并重算历史进度；② 已完成/已放弃目标允许改描述和标签，不允许恢复为 active（恢复另列待确认）；③ 修改 metric_type 若会丢失数据则拒绝；④ 标签采用大小写不敏感唯一名称。
- **输出**：成功刷新详情和 Dashboard 摘要；失败保留原值并提示原因。

#### G01-F03 删除目标（P0）

- **入口**：目标详情页「删除」。
- **主要字段**：目标 ID、二次确认文本。
- **业务规则**：① 确认后删除 goal，级联删除 `goal_record`/`milestone`；② task/project 的外键按 schema 置空，不删除待办或项目；③ 关联 `entity_link` 清理；④ 删除不可恢复，未确认不得执行。
- **输出**：成功返回目标列表；失败显示删除失败且数据不变。

#### G01-F04 完成目标（P0）

- **入口**：目标详情页「标记完成」。
- **主要字段**：目标 ID、完成操作。
- **业务规则**：① 仅 `active→done` 合法；② numeric/milestone 可完成但系统不强制进度 100%，由用户确认结果；③ `done` 后不可记录数据点或勾选里程碑；④ 写入 `updated_at`。
- **输出**：状态变为 done，目标置为只读并产生站内提示；非法状态返回“目标已结束”。

#### G01-F05 放弃目标（P0）

- **入口**：目标详情页「放弃目标」。
- **主要字段**：目标 ID、`status=abandoned`、`updated_at`（系统写入）。
- **业务规则**：① 仅 `active→abandoned` 合法；② 放弃后不可记录数据点、推进里程碑或再次放弃；③ 关联项目/待办保留，目标外键不自动解除；④ 不影响历史统计。
- **输出**：状态变为 abandoned 并从进行中列表移出；非法操作提示状态不允许。

#### G01-F06 记录数据点（P0）

- **入口**：numeric 目标详情页「记录数据」。
- **主要字段**：`goal_record.value`（必填，数值）；`note`（可选）；`recorded_at`（必填，默认当前时间）。
- **业务规则**：① 仅 numeric 且目标 `status=active` 可记录；② 记录时间不得晚于当前时间；③ 同一自然日多次记录，趋势按时间最新值；④ 进度=`(最新值-start)/(target-start)`，夹在 0~100%，不写回 goal；⑤ 数据库失败时不更新进度缓存。
- **输出**：成功更新进度和趋势；失败显示校验或连接错误。

#### G01-F07 查看目标进度与趋势（P0）

- **入口**：目标列表卡片或详情页「进度/趋势」。
- **主要字段**：目标 ID、时间范围（查询参数，可选）。
- **业务规则**：① numeric 取最新 `goal_record` 计算比例；② milestone 为 `is_done=1` 数量/总数；③ status 不显示百分比，仅显示状态；④ 无数据时显示“尚未记录”，不得用任务/习惯次数代替；⑤ 结果按 recorded_at/sort_order 稳定排序。
- **输出**：返回进度、趋势点和数据缺口说明；查询失败显示重试入口。

#### G01-F08 创建里程碑（P0）

- **入口**：milestone 型目标详情页「添加里程碑」。
- **主要字段**：`milestone.title`（必填，≤100字）；`sort_order`（整数，默认末尾）；`goal_id`（系统带入）；`is_done=0`、`done_at=null`、`created_at`、`updated_at`（系统生成）。
- **业务规则**：① 仅 milestone 型 active 目标可添加；② 标题去除首尾空格且不可为空；③ sort_order 同目标内唯一顺序由服务重排；④ 不允许直接创建已完成里程碑。
- **输出**：成功插入并刷新进度；失败提示目标类型或字段错误。

#### G01-F09 完成/撤销里程碑（P0）

- **入口**：目标详情页里程碑勾选框。
- **主要字段**：`is_done`（`0|1`）；`done_at`（完成时当前时间，撤销时 null）。
- **业务规则**：① 仅 milestone 型目标可操作；② active 目标可 `0→1` 或 `1→0`；③ done/abandoned 目标不可变更；④ 完成后进度即时重算；⑤ 同一操作幂等，重复勾选不产生重复记录。
- **输出**：成功更新勾选和进度；非法状态显示“目标已结束”。

#### G01-F10 编辑/删除里程碑（P0）

- **入口**：里程碑行「编辑/删除」。
- **主要字段**：`title`、`sort_order`；删除时里程碑 ID和确认标志。
- **业务规则**：① done/abandoned 目标只读；② 删除会重排 sort_order 并重算进度；③ 删除最后一个里程碑时进度显示无子项；④ 不允许跨目标移动。
- **输出**：成功刷新里程碑列表；失败保留原记录。

### G02 生活项目

#### G02-F01 创建生活项目（P0）

- **入口**：项目列表页「新建项目」。
- **主要字段**：`title`（必填，≤50字）；`description`（可选）；`goal_id`（可选）；`status=active`；`start_at`/`end_at`（可选）；标签（`tag`/`entity_tag`）。
- **业务规则**：① goal_id 为空合法；非空必须指向存在目标；② end_at 不得早于 start_at；③ 项目与开发项目是不同实体；④ 创建不自动生成待办。
- **输出**：成功进入项目详情；失败显示归属或日期错误。

#### G02-F02 编辑生活项目（P0）

- **入口**：生活项目详情页「编辑」。
- **主要字段**：创建字段及 `updated_at`。
- **业务规则**：① goal_id 可置空或改指向其他存在目标；② 已 done 项目可改描述/标签但不回到 active；③ 时间范围校验同创建；④ 不改变其待办状态。
- **输出**：成功刷新项目及目标支持行动区；失败不提交。

#### G02-F03 删除生活项目（P0）

- **入口**：项目详情页「删除」。
- **主要字段**：项目 ID、二次确认。
- **业务规则**：① 确认后硬删除 project；② task.project_id 置空；③ entity_link 清理；④ 删除不级联目标和待办。
- **输出**：成功返回项目列表；失败提示并保留数据。

#### G02-F04 更新生活项目状态（P0）

- **入口**：项目详情状态菜单。
- **主要字段**：`status∈{active|done|paused}`；`start_at`/`end_at` 可在编辑中补齐。
- **业务规则**：① active 可转 paused/done，paused 可转 active/done；② done 不可直接回 active，需新建项目（待确认是否允许恢复）；③ done 时若 end_at 为空写当前时间；④ 状态不影响目标进度。
- **输出**：成功更新状态及列表分组；非法转换显示提示。

### G03 待办

#### G03-F01 创建待办（P0）

- **入口**：今日行动或待办列表「新建待办」。
- **主要字段**：`task.title`（必填，≤100字）；`note`（可选）；`due_date`（可选）；`goal_id`、`project_id`（可选，默认空）；`status=todo`；`created_at`/`updated_at`（系统生成）。
- **业务规则**：① 默认只显示标题和截止日期，目标/项目为折叠高级选项；② goal/project 非空必须存在；③ 最多同时关联一个目标和一个生活项目；④ 不设优先级、重要/紧急字段。
- **输出**：成功出现在列表首位或对应日期分组；失败在表单显示错误。

#### G03-F02 编辑待办（P0）

- **入口**：待办行「编辑」。
- **主要字段**：标题、备注、截止日期、goal_id、project_id、status。
- **业务规则**：① 已 done 待办不可编辑为未完成状态，只能通过撤销操作恢复；② 归属可置空；③ 修改截止日期后重新排序；④ completed_at 与 status 联动维护。
- **输出**：成功更新列表和目标支持行动；失败保留旧值。

#### G03-F03 删除待办（P0）

- **入口**：待办行「删除」。
- **主要字段**：task ID、二次确认。
- **业务规则**：① 确认后硬删除；② entity_link 清理；③ 不影响目标、项目和习惯；④ 删除不可恢复。
- **输出**：成功移除并更新计数；失败显示错误。

#### G03-F04 推进/撤销待办状态（P0）

- **入口**：今日行动/待办列表状态按钮或勾选框。
- **主要字段**：`status∈{todo|doing|done}`；`completed_at`（进入 done 时当前时间，否则 null）。
- **业务规则**：① 合法路径为 todo→doing→done；② done 后不可再推进，允许显式“撤销完成”回 todo；③ todo 不得直接被系统自动标 done；④ 操作幂等并记录 updated_at。
- **输出**：成功更新行样式和今日完成统计；非法操作 toast“已完成待办不可再推进”。

#### G03-F05 筛选与排序待办（P0）

- **入口**：待办列表筛选条。
- **主要字段**：状态、goal_id、project_id、日期范围、排序方式（查询参数，不新增表字段）。
- **业务规则**：① 默认按 due_date 升序，无日期置底；② 可按状态/目标/项目组合过滤；③ 空结果显示空状态；④ 查询仅读 MySQL，不把习惯混入待办表。
- **输出**：返回分组列表和数量；连接失败显示重试。

#### G03-F06 查看目标支持行动（P0）

- **入口**：目标详情页「支持行动」区。
- **主要字段**：目标 ID（必填查询参数）。
- **业务规则**：① 只返回 task.goal_id=目标的待办和 project.goal_id=目标的生活项目；② 无关联显示空状态；③ 不显示习惯，不依据行动数量计算目标进度；④ 点击可跳转实体详情。
- **输出**：展示关联项目/待办及状态；查询失败提示。

### G04 习惯

#### G04-F01 创建习惯（P0）

- **入口**：习惯页「新建习惯」。
- **主要字段**：`name`（必填，≤50字）；`note`（可选）；`frequency_type∈{daily|weekly_times}`（默认 daily）；`weekly_target`（weekly_times 必填 1~7，daily 必须 null）；`streak=0`、`last_done_on=null`、时间字段系统生成。
- **业务规则**：① 频率字段组合必须满足 schema CHECK；② 习惯独立于待办，不允许 goal_id/project_id；③ 创建不自动补历史打卡。
- **输出**：成功显示今日未打卡状态；失败提示频率校验。

#### G04-F02 编辑习惯（P0）

- **入口**：习惯行「编辑」。
- **主要字段**：name、note、frequency_type、weekly_target。
- **业务规则**：① 修改频率不删除历史 `habit_checkin`；② 新频率从修改日开始计算 streak；③ daily 与 weekly_target 的互斥校验同创建。
- **输出**：成功刷新频率说明和连续天数；失败不提交。

#### G04-F03 删除习惯（P0）

- **入口**：习惯行「删除」。
- **主要字段**：habit ID、二次确认。
- **业务规则**：确认后级联删除 habit_checkin；删除不可恢复；不产生目标进度变更。
- **输出**：成功移除习惯；失败保留数据。

#### G04-F04 习惯打卡（P0）

- **入口**：今日行动或习惯页「打卡」。
- **主要字段**：`habit_checkin.checked_on`（默认用户本地今日，不得晚于今日）。
- **业务规则**：① 同一 habit+checked_on 唯一，重复打卡幂等；② daily 连续日按相邻自然日计算；③ weekly_times 按滚动周内完成次数达标，streak 以连续达标周计算；④ 更新 habit.streak、last_done_on；⑤ 不写入 task 或 goal_record。
- **输出**：显示“已打卡”和最新 streak；失败提示日期或连接错误。

#### G04-F05 撤销习惯打卡（P0）

- **入口**：已打卡按钮「撤销」。
- **主要字段**：habit ID、checked_on。
- **业务规则**：删除唯一打卡记录并回算 streak/last_done_on；已不存在的记录视为幂等；不得删除其他日期记录。
- **输出**：恢复未打卡状态并更新连续天数；失败提示。

#### G04-F06 查看习惯历史（P0）

- **入口**：习惯详情「历史」。
- **主要字段**：habit ID、日期范围（查询参数）。
- **业务规则**：按 checked_on 倒序返回打卡；展示频率、streak、last_done_on；无历史显示空状态；查询不生成新记录。
- **输出**：返回日历/列表和统计；失败可重试。

### K01 收藏箱

#### K01-F01 存入收藏箱（P1）

- **入口**：收藏箱「新增收藏」。
- **主要字段**：`kind∈{link|snippet|read_later}`；`title`（必填，≤200字）；`url`（link/read_later 推荐，snippet 可空）；`note`（可选）；`status=pending`；时间字段系统生成。
- **业务规则**：① link/read_later 的 URL 必须是合法 http/https；② snippet 无 URL 时必须有 note；③ 新增不抓取网页、不进入 RAG。
- **输出**：成功进入 pending 列表；失败在表单提示。

#### K01-F02 编辑收藏（P1）

- **入口**：收藏条目「编辑」。
- **主要字段**：title、url、note、kind。
- **业务规则**：编辑后保持原 status；kind 改变需重新校验 URL/note；已 clipped 条目不覆盖已生成 document。
- **输出**：成功刷新收藏；失败不提交。

#### K01-F03 剪藏入库（P1）

- **入口**：pending 收藏「剪藏入库」。
- **主要字段**：收藏 ID、可选文档标题覆盖；生成 `document` 的 `doc_type=webpage`、`source_url`、`raw_text`、`parse_status`、`index_status`；回填 `inbox_item.document_id`。
- **业务规则**：① 仅 link/read_later 且 URL 可访问时抓取正文；② 抓取结果必须交给 K02-F10 统一入库，不得由收藏模块自行写 chunk/Milvus；③ 抓取失败保留收藏并记录 parse_status=failed（不得丢 URL）；④ 仅 MySQL 文档和 chunk 事务提交成功后，inbox status 才变为 clipped 并回填 document_id；⑤ 网页正文为空时不宣称完成剪藏。
- **输出**：成功显示文档和索引排队状态；失败提示抓取原因并提供重试。

#### K01-F04 仅保留链接（P1）

- **入口**：pending 收藏「仅保留链接」。
- **主要字段**：收藏 ID。
- **业务规则**：仅 `kind∈{link,read_later}` 合法；将 status 置为 bookmarked；不创建 document、不进入 RAG；可再次剪藏。
- **输出**：条目标记为 bookmarked；非法类型提示。

#### K01-F05 丢弃/恢复收藏（P1）

- **入口**：收藏条目「丢弃」或 discarded 列表「恢复」。
- **主要字段**：`status∈{pending|clipped|bookmarked|discarded}`。
- **业务规则**：pending/bookmarked 可转 discarded；discarded 可恢复为 pending；clipped 不直接丢弃文档，仅允许将收藏隐藏；恢复不删除已关联 document。
- **输出**：成功移动分组；非法路径提示。

### K02 知识库

#### K02-F01 导入文档（P1）

- **入口**：知识库「导入文件」。
- **主要字段**：文件（必选，扩展名 `.pdf|.docx|.md|.markdown|.txt|.html|.htm`）；`title`（默认文件名）；`doc_type`（按扩展名映射，平台导出文件按实际格式）；`source_path`；`stored_path`（复制到应用数据目录）；`raw_text`；`parse_status`、`index_status`（系统维护）。
- **业务规则**：① 单文件大小上限由设置读取，超限拒绝；② 先复制原件到受管目录，复制失败不得开始入库；③ 采集完成后调用 K02-F10 统一入库，由其执行解析、规范化、分块和事务持久化；④ 支持语雀等平台手动导出后按文件导入，不调用平台 API；⑤ 不导入情绪记录；⑥ 用户重复提交同一运行中任务时必须幂等，不创建两个入库任务。
- **输出**：成功显示文档和索引进度；失败保留原文件并给出格式/解析错误。

#### K02-F02 创建手写文档（P1）

- **入口**：知识库「写作」「新建 Prompt」「新建技能」。
- **主要字段**：`title`（必填）；`raw_text`（必填）；`doc_type∈{note|skill|prompt}`；`usage_tags`（prompt/skill 可选，落 `tag/entity_tag`）；`source_url/source_path`（可选）；状态字段系统设置。
- **业务规则**：① prompt 必须支持复制；② raw_text 为空拒绝保存；③ 内容交给 K02-F10 统一入库；手写内容跳过文件解析但仍执行规范化、分块、MySQL 事务和异步索引；④ 不把时间线或情绪数据隐式复制进文档。
- **输出**：成功进入文档详情并显示索引排队；失败显示字段错误。

#### K02-F03 编辑文档（P1）

- **入口**：文档详情「编辑」。
- **主要字段**：title、raw_text、doc_type、source_url、source_path、stored_path、tags。
- **业务规则**：① 正文或类型变更调用 K02-F10，先生成全量新 chunk，再在同一 MySQL 事务更新文档并替换 chunk，index_status=pending；② 任一步失败保留上一版 raw_text、chunk 和可用旧向量；③ 已软删除文档不可直接编辑，需先恢复；④ 更新 updated_at；⑤ 新向量 ready 前不得删除旧向量。
- **输出**：成功保存并重新排队索引；失败回滚到原版本。

#### K02-F04 管理文档标签（P1）

- **入口**：文档详情标签编辑器。
- **主要字段**：`tag.name`（必填，去空格、大小写不敏感唯一）；`entity_tag.entity_type=document`、`entity_id`、`tag_id`。
- **业务规则**：① 重复标签幂等；② 删除标签仅解除当前实体关联，不删除被其他实体使用的 tag；③ 标签用于全文检索过滤，不拼接进 raw_text。
- **输出**：成功更新标签筛选结果；失败提示名称冲突。

#### K02-F05 删除/恢复文档（P1）

- **入口**：文档详情「删除」或回收站「恢复」。
- **主要字段**：`deleted_at`（删除时当前时间，恢复时 null）。
- **业务规则**：① 删除先软删并从 FULLTEXT、RAG 结果排除；② 保留期结束后由 K03-F04 清理 Milvus 向量，再删除 MySQL chunk 和原件；③ 恢复窗口内可恢复并标 index_status=stale，调用 K03-F02 重建；④ 不删除来源收藏/时间线原记录。
- **输出**：成功改变可见性并显示清理/重建状态；失败保持原状态。

#### K02-F06 查看文档索引状态（P1）

- **入口**：知识库文档行状态徽标或详情页「索引」。
- **主要字段**：`parse_status∈{pending|ready|failed}`；`index_status∈{pending|indexing|ready|failed|stale}`；错误摘要（运行时字段，不落 schema）。
- **业务规则**：① ready 文档的状态由 K03-F03 校验；② worker/Milvus 失败置 failed，保留原文、chunk 和可用旧索引；③ 启动校验异常转 stale；④ parse failed 不阻塞基础元数据编辑；⑤ “重试索引”必须调用 K03-F02，不得直接篡改状态。
- **输出**：返回状态、最近错误和“重试/重建索引”入口；无状态记录显示 pending。

#### K02-F07 基础全文搜索（P0）

- **入口**：全局搜索框或知识库搜索框。
- **主要字段**：关键词（必填，1~200字）；实体范围；标签、类型、日期过滤（查询参数）；文档使用 MySQL FULLTEXT(title,raw_text) ngram。
- **业务规则**：① 搜索结果排除 deleted_at 非空文档和所有 mood_record；② 类型过滤使用 `doc_type`，标签通过 entity_tag；③ 无匹配返回空状态；④ MySQL 不可用时明确提示，不伪造缓存结果。
- **输出**：返回实体、标题、命中片段和高亮；失败提示连接错误。

#### K02-F08 导出文档（P1）

- **入口**：文档详情「导出」。
- **主要字段**：文档 ID；格式 `pdf|markdown|txt`；目标路径（由文件选择器提供）。
- **业务规则**：① 导出只做格式转换，不重新进入 RAG；② PDF 生成非空且页数>0，Markdown/TXT 校验字符数和 hash；③ 无 raw_text 时失败；④ 不导出密钥和运行日志。
- **输出**：成功生成文件并返回路径/hash；失败提示写入或渲染错误。

#### K02-F09 复制 Prompt 模板（P1）

- **入口**：`doc_type=prompt` 文档行「复制」。
- **主要字段**：文档 ID；`raw_text`；usage_tags（只读展示）。
- **业务规则**：仅 prompt 类型可复制；复制内容为完整 raw_text，不附加系统提示；文档未索引不影响复制；空正文提示不可复制。
- **输出**：成功写入系统剪贴板并 toast；失败提示权限错误。

#### K02-F10 执行统一文档入库（P1）

- **入口**：由文件导入、网页剪藏、手写文档、文档编辑或时间线升格成功采集内容后自动触发；系统任务页可查看进度。
- **主要字段**：`document.id`（新建时系统生成，编辑时沿用）；`title`（必填）；`doc_type∈{webpage|pdf|docx|markdown|txt|html|note|skill|prompt|promoted}`；`source_url`、`source_path`、`stored_path`（按来源可选）；输入正文或原件；`promoted_from_timeline_id`（升格时必填）；`parse_status`、`index_status`（系统维护）；标签（可选）。
- **业务规则**：① 固定编排顺序为采集校验→创建/更新待解析元数据→K02-F11 解析→K02-F12 规范化→K02-F13 分块→MySQL 事务写 document+全量 chunk→提交后触发 K03-F01；手写/升格已有正文时可跳过文件提取但不得跳过规范化和分块；② 新文档先写 `raw_text=''`、parse_status=pending、index_status=pending 的元数据壳记录并保留来源/原件，解析失败更新为 parse_status=failed；编辑文档不得用壳记录覆盖旧可用版本；③ 入库成功定义为 MySQL 中 parse_status=ready、raw_text 非空且全量 chunk 事务提交成功，此时可查看和 FULLTEXT 搜索，但不代表 RAG ready；④ 新建、编辑和升格必须幂等：同一运行时任务 ID 重放不得重复建 document；正文 hash 为运行时计算值（不新增 document 列），已入库内容通过 raw_text/chunk.content_hash 比对；同来源同内容必须提示复用或仍新建，禁止静默覆盖；⑤ 最终 MySQL 事务失败时回滚新正文/新 chunk，新建场景保留 failed 壳记录，编辑场景保留旧正文、旧 chunk 和旧索引；⑥ 任何入口不得绕过本功能直接写 Milvus；⑦ 情绪、未升格时间线和运行日志不接受入库。
- **输出**：MySQL 入库成功返回 document_id、chunk 数、parse_status=ready、index_status=pending；失败返回 document_id、失败阶段并保留可重试的来源/原件或旧版本。

#### K02-F11 解析文档正文（P1）

- **入口**：K02-F10 对网页或文件来源执行解析步骤；失败文档详情「重新解析」。
- **主要字段**：原件 `stored_path` 或网页抓取内容；`doc_type`；输出 `raw_text` 和解析定位元数据；`parse_status∈{pending|ready|failed}`。
- **业务规则**：① PDF、DOCX、Markdown、TXT、HTML 分别使用对应解析器，网页剪藏先提取正文；平台导出文件按真实扩展名解析；② 解析开始显式置 pending，只有获得非空有效正文才可置 ready；③ 加密 PDF、损坏文件、不支持编码、空正文或解析超时置 failed，保留原件、来源和错误摘要；④ 解析器不得执行文档内脚本、宏或外部资源；⑤ 页码、标题层级和来源位置必须尽可能保留，缺失定位字段用 null，不得虚构；⑥ 重新解析不得覆盖仍可用的旧版本，须等新结果通过 K02-F13 并成功提交。
- **输出**：成功返回规范化前正文和定位元数据；失败返回明确错误类型并提供重新解析入口。

#### K02-F12 规范化文档内容（P1）

- **入口**：K02-F11 解析成功或手写/升格正文提交后自动执行。
- **主要字段**：输入正文；`document.title/doc_type/source_url/source_path`；输出规范 `raw_text` 与 chunk 所需来源元数据。
- **业务规则**：① 统一换行、字符编码和无意义空白，保留标题、列表、代码块、表格、引用和段落边界；② 不得摘要、改写或补写原文；③ HTML 去除脚本、样式和导航噪声，保留可读正文与链接文本；④ 生成正文级 content hash，用于幂等判断和变更检测；⑤ 规范化后为空则入库失败；⑥ metadata 至少准备 `documentTitle`、`docType`、`sourceUrl/sourcePath`，无法获得的字段为 null。
- **输出**：成功返回规范正文、content hash 和基础元数据；失败停止入库并保留输入。

#### K02-F13 文档分块（P1）

- **入口**：K02-F12 规范化成功后自动执行；文档详情重建索引时可重新执行。
- **主要字段**：`chunk.id`（UUID）；`document_id`；`content`；`seq_no`（从0连续）；`token_count`；`metadata_json`；`content_hash`；`created_at`。
- **业务规则**：① 先按标题和段落分组，再在约 400~700 tokens 切分，相邻块保留 80~120 tokens 重叠；② 代码、表格和引用不跨结构随意截断，超大不可分结构允许单块超过建议值并记录实际 token_count；③ metadata_json 至少包含 documentTitle、docType、sourceUrl/sourcePath、headingPath、pageStart/pageEnd、charStart/charEnd；④ 同一文档 seq_no 唯一且连续；chunk content_hash 由规范内容和稳定定位计算，重复执行相同输入必须产生相同逻辑分块；⑤ raw_text 非空但无法产生任何 chunk 时入库失败；⑥ 全量新 chunk 在单个 MySQL 事务内写入，编辑时不得逐块覆盖导致新旧混合。
- **输出**：成功返回全量 chunk、数量和 token 统计；失败阻止 MySQL 新版本提交并保留旧版本。

### K03 RAG

#### K03-F01 创建向量索引（P1）

- **入口**：K02-F10 的 MySQL 入库事务提交后自动触发；系统任务页显示进度。
- **主要字段**：document_id；MySQL chunk 的 `id/content/content_hash`；Milvus 字段 `chunk_id`、`document_id`、`model_version`、512维 embedding；`document.index_status`。
- **业务规则**：① 仅 parse_status=ready、未软删除且至少一个 chunk 的文档可索引；② 开始时 `pending→indexing`，使用配置的本地 embedding 模型按批生成向量；初次索引写当前 collection，内容编辑使用全量新 chunk_id，模型切换的双版本写入策略见待确认项；③ 每个 Milvus chunk_id 必须能回查 MySQL，严禁写入情绪数据；④ 全量 chunk 写入并通过 K03-F03 校验后才置 ready；任何批次失败置 failed，禁止把部分成功当作 ready；⑤ 两库无分布式事务，失败时保留 MySQL 原文/chunk和仍可用的旧索引，新文档则降级为 FULLTEXT；⑥ 向量维度或 model_version 与 collection 不匹配时停止并要求重建/迁移。
- **输出**：成功返回 index_status=ready、模型版本和向量数量；失败返回 index_status=failed、失败批次和重试入口。

#### K03-F02 重试/重建向量索引（P1）

- **入口**：failed/stale 文档详情「重试/重建索引」、模型配置变更提示或启动一致性检查自动入队。
- **主要字段**：document_id；重建原因（失败重试/内容变更/模型变更/一致性异常，运行时类型）；当前 model_version；全部有效 chunk。
- **业务规则**：① failed 可重试，stale 必须全量重建；模型版本变化时所有受影响 ready 文档依次标 stale 并限流重建；② 重建以 MySQL 当前全量 chunk 为事实源，不复用不确定的部分向量；③ 内容变化产生新 chunk_id 时，新向量校验成功前保留旧向量；仅模型变化时是否能并存旧向量取决于待确认的 Milvus 版本化方案，未确认前不得覆盖仍在服务的 ready 索引；④ 同一文档同时只允许一个重建任务，重复请求合并；⑤ 失败保留旧索引和原文，状态为 failed 或 stale 并记录最近错误；⑥ 不因 LLM 密钥缺失阻止 embedding 重建。
- **输出**：成功切换到新模型版本并置 ready；失败保留可用旧索引或 FULLTEXT 降级并显示错误。

#### K03-F03 校验索引一致性（P1）

- **入口**：K03-F01/K03-F02 完成前强制执行；应用启动轻量检查；设置页「检查索引」。
- **主要字段**：document_id；MySQL chunk 数与 chunk_id/content_hash；Milvus chunk_id、document_id、model_version 和向量数量。
- **业务规则**：① ready 的必要条件是有效 MySQL chunk 数=当前 model_version 的 Milvus 向量数，且 chunk_id 集合一致；② 对大文档先全量比对 ID/数量，再抽样回查 content_hash 对应关系；③ 缺失、多余、跨文档、错误模型版本或不可回查向量均视为不一致；④ 启动检查发现不一致时将 ready→stale 并入队 K03-F02；⑤ Milvus 暂时不可用时报告“无法校验”，不得错误改为 ready；⑥ 校验只读，不直接删除向量。
- **输出**：返回一致/不一致/无法校验、差异数量和处理动作；一致时允许状态置 ready。

#### K03-F04 清理旧版本向量（P1）

- **入口**：新索引通过 K03-F03 后自动触发；文档软删除保留期结束后的清理任务。
- **主要字段**：document_id；保留的 model_version/chunk_id 集合；待删除向量 ID；文档 deleted_at。
- **业务规则**：① 内容编辑或模型重建时，仅在新版本全量 ready 后删除旧版本向量；② 软删除文档在保留期内只从检索过滤，不物理清理；到期后先删 Milvus 向量，确认成功后再允许删除 MySQL chunk 和原件；③ 删除失败可重试且不得把文档宣告已彻底清理；④ 清理范围必须由 document_id+版本/显式 chunk_id 确定，禁止无条件清空 collection；⑤ 正在被索引或恢复的文档不得清理。
- **输出**：成功返回删除数量并释放旧版本；失败保留待清理标记和重试信息。

#### K03-F05 语义检索（P1）

- **入口**：知识问答页「相似资料」或搜索模式切换。
- **主要字段**：查询文本（必填）；top_k（默认20，最大50）；可选类型/标签过滤。
- **业务规则**：① 仅查询 index_status=ready 且未删除的 document/chunk；② 本地 embedding 生成向量，Milvus COSINE 检索；③ 过滤后去重并返回最多 6 个上下文 chunk；④ 无模型/Milvus 时降级为 FULLTEXT，并明确“语义索引不可用”；⑤ 严禁 mood_record 进入向量查询。
- **输出**：返回 chunk_id、document_id、相似度和定位元数据；失败显示降级状态。

#### K03-F06 RAG 问答（P1）

- **入口**：知识问答页输入框「提问」。
- **主要字段**：问题（必填，≤2000字）；会话上下文（运行时）；检索过滤条件（可选）。
- **业务规则**：① 先检索再生成，发送给 LLM 的只有问题和命中知识 chunk；② prompt 要求无证据明确不知道、引用编号不可省略；③ 每个引用必须回查真实 chunk，不允许 LLM 自由生成链接；④ 无 LLM 密钥/断网时展示相似资料列表，不阻塞全文搜索；⑤ 情绪数据永不进入 RAG 请求。
- **输出**：成功返回回答、引用列表和原文定位；失败显示“AI 问答不可用”及可用降级能力。

#### K03-F07 追问与查看引用（P1）

- **入口**：回答下方「追问」或引用卡片。
- **主要字段**：追问文本（必填）；引用 `chunkId/documentId`（只读）。
- **业务规则**：追问只在当前会话上下文内重检索；点击引用必须打开原文标题/页码/标题路径；引用文档已删除时显示不可用并不重新生成虚假链接；会话不写入业务 schema。
- **输出**：成功追加回答或打开定位；失败提示引用已失效/网络不可用。

### K04 沉淀时间线

#### K04-F01 创建沉淀条目（P1）

- **入口**：时间线页「新建沉淀」。
- **主要字段**：`type∈{diary|idea|decision|reading|review}`；`title`（可选）；`content`（必填）；`created_at`（默认当前）；标签（entity_tag）；`document_id`（可选，仅升格后回填）。
- **业务规则**：① review 类型由周复盘流程生成，也允许手动创建；② 时间线默认不参与全文/RAG；③ content 为空拒绝；④ 情绪记录不可作为 timeline_entry 写入。
- **输出**：成功按时间倒序出现在时间线；失败显示校验错误。

#### K04-F02 编辑沉淀条目（P1）

- **入口**：时间线条目「编辑」。
- **主要字段**：title、content、type、created_at、tags。
- **业务规则**：① 已升格条目修改后不自动覆盖 document，需再次确认是否同步；② type 改变仅影响筛选，不改变历史关联；③ updated_at 更新。
- **输出**：成功刷新条目；失败保留原内容。

#### K04-F03 删除沉淀条目（P1）

- **入口**：时间线条目「删除」。
- **主要字段**：条目 ID、确认标志。
- **业务规则**：删除条目并清理 entity_link；已升格 document 默认保留，document.promoted_from_timeline_id 变为悬空引用但文档可继续检索；删除不可恢复。
- **输出**：成功移除条目；失败提示。

#### K04-F04 关联实体（P1）

- **入口**：时间线条目「关联」。
- **主要字段**：`entity_link.from_type/from_id`、`to_type/to_id`、`relation`（可选语义标签）、`created_at`。
- **业务规则**：① 两端实体必须存在，应用层校验多态关联；② 支持 0~n 关联和双向 backlink；③ 允许目标、生活项目、待办、文档、开发项目等，不默认关联情绪；④ 同一组合幂等，删除实体时清理关联。
- **输出**：成功显示双向关联；失败提示实体不存在或重复。

#### K04-F05 查看/筛选时间线（P1）

- **入口**：时间线页筛选条。
- **主要字段**：类型、日期范围、标签（查询参数）。
- **业务规则**：默认按 created_at DESC；类型枚举必须完整；无结果显示空状态；查询不触发 RAG。
- **输出**：返回分页条目和关联摘要；失败可重试。

#### K04-F06 升格为知识文档（P1）

- **入口**：时间线条目「升格为知识库」。
- **主要字段**：条目 ID；入库模式 `原样|编辑后`（界面选项）；生成 document 的 `doc_type=promoted`、title、raw_text、promoted_from_timeline_id、index_status。
- **业务规则**：① 原条目始终保留；② 编辑后内容仅作为入库输入，不改原条目；③ 调用 K02-F10，document+chunk 事务成功后才回填 timeline_entry.document_id；④ 升格文档 index_status=ready 后参与 RAG，索引失败保留 MySQL 文档并可重试；⑤ 重复升格需提示已有文档。
- **输出**：成功显示文档和索引状态；失败不改变原条目。

#### K04-F07 周复盘（P1）

- **入口**：Dashboard/时间线「周复盘」。
- **主要字段**：周起止日期（默认本地上一周）；固定三问内容（本周完成了什么/遇到什么阻碍/下周最重要的一步）；可选关联实体列表。
- **业务规则**：① 生成一条 `type=review` 的 timeline_entry；② 三问均可留空但必须保存结构化文本；③ 关联只写 entity_link；④ 不自动调用 LLM、不修改任务状态；⑤ 同一周允许编辑已有 review，不重复创建。
- **输出**：成功进入时间线并显示关联；失败提示日期或保存错误。

### M01 心理健康

#### M01-F01 管理自定义情绪词（P1）

- **入口**：情绪记录表单「自定义情绪词」。
- **主要字段**：`mood_word.name`（必填，≤30字，唯一）；`is_builtin=0`；`created_at`。
- **业务规则**：内置词 `is_builtin=1` 只读不可删除；自定义词重名（不区分大小写）拒绝；删除自定义词只允许在未被记录引用时，已有记录改用 custom_mood_word 或保留 mood_word_id。
- **输出**：成功加入词库下拉；失败提示重名或被引用。

#### M01-F02 记录情绪事件（P1）

- **入口**：情绪记录页「快速记录」。
- **主要字段**：`mood_word_id` 或 `custom_mood_word`（至少一个）；`intensity`（必填整数1~5）；`event`（必填，≤500字）；`context`（可选）；`need`（可选）；`recorded_at`（默认当前）。
- **业务规则**：① 情绪词必须来自词库或填写自定义词，不能两者都空；② 默认只要求词+强度，context/need 可折叠；③ 纯事件驱动，不生成每日打卡；④ 写入 mood_record 普通表，不写 document/chunk/vector/entity_link；⑤ AI 不自动修改记录。
- **输出**：成功进入时间线式情绪列表和统计；失败显示字段校验。

#### M01-F03 编辑情绪记录（P1）

- **入口**：情绪记录行「编辑」。
- **主要字段**：mood_word_id/custom_mood_word、intensity、event、context、need、recorded_at。
- **业务规则**：校验同创建；编辑不改变既有周报快照；更新通过替换记录实现（schema 无 updated_at）；不进入 RAG。
- **输出**：成功刷新统计；失败保留原记录。

#### M01-F04 删除情绪记录（P1）

- **入口**：情绪记录行「删除」。
- **主要字段**：记录 ID、确认标志。
- **业务规则**：确认后硬删除；历史 mood_report 的 input_snapshot_json 不回写；删除不清理其他实体（情绪禁止跨模块关联）。
- **输出**：成功移除并重算统计；失败提示。

#### M01-F05 查看情绪分布与趋势（P1）

- **入口**：情绪周报页统计区。
- **主要字段**：period_start、period_end（必填日期范围）。
- **业务规则**：按 mood_word/custom_mood_word 聚合次数，按 recorded_at 计算强度趋势；空数据显示“暂无足够记录”；结果仅用于自我观察，不输出因果或诊断结论。
- **输出**：返回分布、趋势和样本数；查询失败提示。

#### M01-F06 生成情绪周报（P1）

- **入口**：情绪周报页「生成/刷新周报」。
- **主要字段**：`mood_report.period_start/period_end`；`input_snapshot_json`（系统生成统计快照）；`model_name`；`content`。
- **业务规则**：① 只将该周期情绪统计与用户明确记录发送 LLM，不发送知识库 chunk；② 提示词要求使用“记录显示/你似乎”，禁止诊断和自动执行；③ 同一周期唯一，刷新覆盖需二次确认或更新已有报告；④ 无密钥/断网时不写成功报告，展示统计图表和不可用提示；⑤ LLM 失败保留旧报告。
- **输出**：成功保存并展示总结/建议；失败显示降级统计和重试入口。

#### M01-F07 查看历史周报（P1）

- **入口**：情绪周报页历史列表。
- **主要字段**：period_start、period_end、model_name、content、created_at（只读）。
- **业务规则**：按周期倒序；报告是生成时快照，不随新记录自动改变；缺少报告时提示手动生成；内容不进入全文或向量索引。
- **输出**：展示历史报告和生成模型；失败提示。

### D01 想法收集

#### D01-F01 创建项目想法（P2）

- **入口**：想法收集页「记想法」。
- **主要字段**：`title`（必填，≤100字）；`description`（可选）；`status=inbox`；时间字段系统生成。
- **业务规则**：低门槛保存，不要求评分；空标题拒绝；不创建开发项目。
- **输出**：成功进入 inbox 分组；失败显示校验。

#### D01-F02 编辑项目想法（P2）

- **入口**：想法详情「编辑」。
- **主要字段**：title、description。
- **业务规则**：已 launched 仅允许改描述；更新 updated_at；不改变关联项目。
- **输出**：成功刷新想法；失败不提交。

#### D01-F03 删除项目想法（P2）

- **入口**：想法行「删除」。
- **主要字段**：想法 ID、确认标志。
- **业务规则**：未 launched 可硬删除；launched 想法不可删除（project_idea 无 archived 状态），仅允许从想法列表隐藏展示，且不得删除已关联开发项目；清理评估字段。
- **输出**：成功移除或隐藏；非法操作提示。

#### D01-F04 进入评估（P2）

- **入口**：inbox 想法「开始评估」。
- **主要字段**：`status=evaluating`。
- **业务规则**：仅 inbox→evaluating 合法；已 discarded 可先复活为 inbox 再评估；重复操作幂等。
- **输出**：显示评分表单；非法路径提示。

#### D01-F05 提交想法评估（P2）

- **入口**：评估表单「保存评估」。
- **主要字段**：`value_score`（整数1~5）；`feasibility_score`（整数1~5）；`evaluation_note`（可选）；status 保持 evaluating 或由结果动作改变。
- **业务规则**：分数必须同时存在且在 1~5；备注不超过 1000 字；保存不自动批准、不自动立项；评估可重复修改。
- **输出**：成功显示评分和建议动作；失败提示范围错误。

#### D01-F06 批准/放弃想法（P2）

- **入口**：评估详情「批准」或「放弃」。
- **主要字段**：`status∈{approved|discarded}`。
- **业务规则**：evaluating→approved 或 evaluating→discarded；inbox 不得跳过评估直接 approved；discarded 保留评分和备注；approved 才能一键立项。
- **输出**：成功进入对应分组；非法状态提示。

#### D01-F07 复活已放弃想法（P2）

- **入口**：discarded 分组「复活」。
- **主要字段**：`status=inbox`（可选随后进入 evaluating）。
- **业务规则**：仅 discarded 可复活；不清空历史评分；不得直接恢复为 launched；复活后需要重新批准才能立项。
- **输出**：成功回到 inbox；失败提示。

#### D01-F08 一键立项（P2）

- **入口**：approved 想法「一键立项」。
- **主要字段**：想法 ID；生成 `dev_project.name/description`；`launched_to_project_id`；想法 `status=launched`。
- **业务规则**：① 仅 approved 可立项且不可重复立项；② 新项目默认 stages=需求/开发/测试/交付/维护，首阶段为当前阶段，status=active；③ 项目写入成功后在同事务回填关联；④ 失败时想法保持 approved。
- **输出**：成功进入开发项目详情；失败提示且不生成半项目。

### D02 开发项目与看板

#### D02-F01 创建开发项目（P2）

- **入口**：项目管理页「新建开发项目」。
- **主要字段**：`name`（必填，≤100字）；`description`（可选）；`repo_url`（可选合法 URL）；`status=active`；`current_stage_id`（系统指向首阶段）；`dev_project_stage` 默认五阶段。
- **业务规则**：阶段至少一个且 sort_order 从 0 连续；仅一个首阶段；创建与阶段写入同一事务。
- **输出**：成功出现在首阶段看板列；失败回滚项目和阶段。

#### D02-F02 编辑开发项目（P2）

- **入口**：开发项目详情「编辑」。
- **主要字段**：name、description、repo_url、status。
- **业务规则**：repo_url 为空合法；archived 项目只允许改描述；更新 updated_at；不改变阶段列表。
- **输出**：成功刷新详情；失败不提交。

#### D02-F03 删除/归档开发项目（P2）

- **入口**：项目详情「删除」或「归档」。
- **主要字段**：`status∈{active|done|paused|archived}`、确认标志。
- **业务规则**：active/paused 可归档；删除需二次确认并清理 project_idea.launched_to_project_id 与 entity_link；archived 默认不可拖动；删除不影响生活项目。
- **输出**：成功从活动看板移除或彻底删除；失败保持原状态。

#### D02-F04 配置项目阶段（P2）

- **入口**：开发项目详情「阶段设置」。
- **主要字段**：`dev_project_stage.name`（必填）；`sort_order`（整数）；`is_terminal∈{0|1}`；`dev_project_id`；阶段 ID。
- **业务规则**：同一项目 name 和 sort_order 唯一；至少保留一个阶段；终止阶段可多个但看板进度取最末 sort_order；删除当前阶段前必须先迁移项目；阶段改名不改变项目状态。
- **输出**：成功刷新列和阶段进度；失败提示唯一性或迁移要求。

#### D02-F05 查看开发看板（P2）

- **入口**：项目管理页「看板」。
- **主要字段**：项目状态过滤、阶段过滤（查询参数）。
- **业务规则**：按 dev_project_stage.sort_order 分列；archived 默认隐藏；无项目/空列显示空状态；不混入生活 project。
- **输出**：返回阶段列、项目卡片和当前状态；失败可重试。

#### D02-F06 拖动切换阶段（P2）

- **入口**：看板项目卡片拖动到目标阶段。
- **主要字段**：项目 ID、目标 `current_stage_id`。
- **业务规则**：目标阶段必须属于同一 dev_project；跨项目拖动拒绝；更新 current_stage_id 和 updated_at；移动到终止阶段不自动改 status=done，需用户确认。
- **输出**：成功移动卡片并更新进度；失败回滚原列。

#### D02-F07 手动推进阶段（P2）

- **入口**：项目详情「下一阶段/上一阶段」。
- **主要字段**：项目 ID、方向。
- **业务规则**：按 sort_order 推进或退回；首阶段不可再退、末阶段不可再进；终止阶段推进需先退出终止阶段或确认完成；不跳过中间阶段。
- **输出**：成功更新 current_stage_id；非法边界提示。

#### D02-F08 查看阶段进度（P2）

- **入口**：项目详情进度区。
- **主要字段**：当前阶段 sort_order、终止阶段 sort_order（查询计算）。
- **业务规则**：进度为当前阶段在阶段序列中的位置比例；无阶段返回不可计算；不根据任务数或提交次数计算；阶段重排后即时重算。
- **输出**：显示阶段名称和百分比/不可计算状态；失败提示。

### H Dashboard 与回顾

#### H01-F01 查看今日行动（P0）

- **入口**：Dashboard 首屏主区。
- **主要字段**：本地今日日期；task 查询条件；habit 今日打卡状态。
- **业务规则**：展示未完成及今日到期待办、所有习惯今日状态；完成待办/打卡可直接操作；不把习惯转换为 task；空数据显示鼓励性空状态。
- **输出**：返回今日待办、习惯和完成计数；失败显示局部错误，不阻塞目标摘要。

#### H01-F02 查看目标摘要（P0）

- **入口**：Dashboard 次区目标卡片。
- **主要字段**：近期 active 目标、due_date、进度查询结果。
- **业务规则**：按 due_date/updated_at 排序；显示 numeric/milestone 进度或 status 状态；不显示任务换算进度；无目标显示引导创建。
- **输出**：返回摘要卡片和详情入口；失败显示占位错误。

#### H01-F03 查看站内提醒（P0）

- **入口**：Dashboard 提醒区、顶部通知入口。
- **主要字段**：提醒类型（目标到期/习惯打卡/数据录入/索引完成等运行时类型）；app_setting 开关和频率。
- **业务规则**：仅站内聚合，无桌面/邮件推送；目标到期、习惯未打卡、numeric 长时间未记录可生成提示；提醒是建议非强制；关闭对应开关后不生成新提醒。
- **输出**：返回未读/已读提醒；配置关闭时显示无提醒。

#### H01-F04 全局基础搜索（P0）

- **入口**：全局顶部搜索框。
- **主要字段**：关键词、实体范围、标签、类型、日期过滤。
- **业务规则**：调用各模块明确全文索引；document 使用 ngram FULLTEXT，其余实体按页面场景索引；mood_record 永不进入；无 MySQL 时显示离线不可用。
- **输出**：返回按实体分组的标题、片段和跳转；失败提示。

#### H01-F05 查看洞察（P2）

- **入口**：Dashboard「洞察」。
- **主要字段**：时间范围（查询参数）；无新增持久化字段。
- **业务规则**：从目标/任务/习惯/情绪/时间线聚合趋势；只表达自我观察，不作因果结论、排名或惩罚；数据不足显示样本不足；不将洞察写入 RAG。
- **输出**：展示趋势、完成率、逾期模式和复盘摘要；失败显示可用模块和重试。

#### H01-F06 日检视（P2）

- **入口**：Dashboard「日检视」。
- **主要字段**：本地日期；完成事项、阻碍、下一步（生成 review 条目内容）。
- **业务规则**：只读汇总 task/habit/goal 数据；提交后创建或更新当日 review timeline_entry；不自动改任务/目标。
- **输出**：成功保存日检视记录；失败提示。

#### H01-F07 月度回顾（P2）

- **入口**：Dashboard「月度回顾」。
- **主要字段**：月份范围；系统汇总数据；可选回顾文本。
- **业务规则**：使用既有实体和 timeline review 保存回顾；不新增月度实体；空月份允许保存但标记无数据；不调用 AI 自动修改。
- **输出**：展示月度汇总并可保存 review；失败提示。

#### H01-F08 季度规划（P2）

- **入口**：Dashboard「季度规划」。
- **主要字段**：季度；新建/筛选 goal.period=quarterly 的目标；可选规划文本写入 review。
- **业务规则**：不建立独立季度表；季度规划只操作 goal 与 timeline_entry；不得自动把任务挂到目标；周期值必须 quarterly。
- **输出**：进入季度目标列表并可保存规划记录；失败提示。

#### H01-F09 日历视图（P2）

- **入口**：Dashboard/待办页「日历」。
- **主要字段**：月份/日期范围；读取 task.due_date、goal.due_date、project.start_at/end_at。
- **业务规则**：无日期实体不显示；拖动日期等价于编辑 due_date，需确认；不创建日程实体；逾期仅视觉标识，不改变 task.status。
- **输出**：显示按日期分布的待办、目标和项目；失败提示。

#### H01-F11 查看主动推荐（P2）
- **入口**：Dashboard 推荐区（默认隐藏）。
- **主要字段**：无新增持久化字段；运行时候选列表（建议文本、关联实体、类型）。
- **业务规则**：① 三级主动性，默认关闭；② 只呈现候选建议，**用户确认后才执行**，绝不自动修改数据；③ 数据不足（如记录数少）不生成推荐；④ 关闭对应开关后不再展示；⑤ 推荐不写入业务表。
- **输出**：展示候选建议或"暂无推荐"；失败静默隐藏不干扰。

#### H01-F10 时间预算（P2，待确认）

- **入口**：日历/待办「时间预算」。
- **主要字段**：当前 schema 未提供 task duration/budget 字段；界面只能读取 due_date。
- **业务规则**：在字段未确认前不得写入自定义字段或 JSON 偷存；可先提供只读日期计划；预算计算和持久化需产品确认后实现。
- **输出**：当前版本显示“时间预算尚未配置字段”；不产生伪数据。

### S01 设置与系统

#### S01-F01 保存数据源配置（P0）

- **入口**：设置页「数据源连接」保存。
- **主要字段**：`app_setting.key`（受控键，如 `mysql.connection`、`milvus.connection`、`backup.retention_days`）；`value_json`（连接串、超时、加密引用等）；`updated_at`。
- **业务规则**：密钥不明文写日志/备份；保存前做 schema 校验和敏感字段加密存储；未测试通过的配置可保存但标记不可用；不得新增表列。
- **输出**：成功保存并显示配置摘要；失败提示格式或加密错误。

#### S01-F02 测试数据源连接（P0）

- **入口**：设置页 MySQL/Milvus「测试连接」。
- **主要字段**：使用 app_setting 中连接配置；测试超时（运行时）。
- **业务规则**：分别测试 MySQL 查询、Milvus collection/模型版本；任一失败只影响依赖该数据源的功能；MySQL 可用而 Milvus 不可用时保留 FULLTEXT；不修改业务数据。
- **输出**：返回连接延迟、版本和健康状态；失败给出可操作错误。

#### S01-F03 配置 LLM 密钥（P1）

- **入口**：设置页「AI 配置」保存。
- **主要字段**：provider、endpoint、model_name、密钥安全引用写入 `app_setting.value_json`。
- **业务规则**：密钥仅存本机安全存储，不进 manifest、JSON/Markdown 导出或日志；保存后通过最小请求测试；无密钥时 RAG/周报显示不可用但核心功能可用。
- **输出**：成功显示 provider/model 健康状态；失败提示鉴权或网络错误。

#### S01-F04 配置提醒与频率（P0）

- **入口**：设置页「提醒开关」。
- **主要字段**：`app_setting.key` 受控键 `notify.critical.enabled`、`notify.periodic.enabled`、`notify.recommendation.enabled`、各自 frequency；布尔值和频率写 value_json。
- **业务规则**：一级关键提醒默认开，二级周期报告默认生成但可关闭通知，三级推荐默认关；仅站内渠道；频率值由设置校验，关闭立即停止新提醒。
- **输出**：成功保存并即时影响 Dashboard；失败提示配置错误。

#### S01-F05 配置降噪选项（P0）

- **入口**：设置页「降噪配置」。
- **主要字段**：通知聚合窗口、已读保留天数、推荐确认策略等 JSON 设置。
- **业务规则**：推荐只能呈现候选，必须用户确认才执行；设置只改变提示频率，不改变业务状态；超出允许范围拒绝保存。
- **输出**：成功显示当前降噪策略；失败提示范围错误。

#### S01-F06 一键备份（P0）

- **入口**：设置页「一键备份」。
- **主要字段**：输出目录；manifest 的 schema/app 版本、MySQL dump、Milvus collection 快照、原始文件副本、SHA-256、向量数量。
- **业务规则**：① 先执行 MySQL `mysqldump --single-transaction`，再快照 Milvus；② 不包含 LLM 密钥、日志、缓存；③ 备份失败不覆盖旧备份；④ 生成后做表计数、抽样 hash 和 chunk_id 回查；⑤ 远端数据源无网络时明确失败，不承诺离线备份。
- **输出**：成功返回备份目录、manifest 和校验结果；失败保留旧备份并显示阶段错误。

#### S01-F07 一键恢复（P0）

- **入口**：设置页「恢复」选择 manifest。
- **主要字段**：备份目录/manifest；确认文本。
- **业务规则**：① 先备份当前 MySQL/Milvus，再校验选中 manifest；② 按 MySQL→Milvus→一致性校验顺序恢复；③ 失败回滚到恢复前备份，不接受只恢复一个数据源宣告成功；④ 整体替换，不做冲突合并/版本历史；⑤ 恢复期间禁止写入业务数据。
- **输出**：成功显示记录数、向量数和一致性结果；失败提示回滚完成或人工处理项。

#### S01-F08 导出 JSON（P0）

- **入口**：设置页「导出 JSON」。
- **主要字段**：导出范围；版本化 DTO；目标路径。
- **业务规则**：导出业务实体和关联，不导出 MySQL/Milvus 内部字段、密钥、日志；执行 JSON Schema 校验、记录数量与 hash；情绪可按用户选择导出，但永不进入 RAG；失败不生成半文件。
- **输出**：成功返回文件路径、版本和 hash；失败提示校验/写入错误。

#### S01-F09 导出 Markdown/TXT（P0）

- **入口**：设置页「导出 Markdown/TXT」。
- **主要字段**：范围（文档/时间线/全部）；格式；目标目录。
- **业务规则**：文档按规范化 Markdown/纯文本导出，时间线按日期分文件；重新读取并比对字符数/hash；不导出向量、密钥或情绪到知识库目录；失败保留已存在文件。
- **输出**：成功返回文件清单和校验结果；失败提示。

#### S01-F10 查看系统任务状态（P0）

- **入口**：设置页「后台任务」或全局任务抽屉。
- **主要字段**：任务类型（备份/恢复/解析/索引/导出，运行时枚举）；进度、阶段、错误摘要、开始/结束时间。
- **业务规则**：任务状态来自主进程 IPC 推送；失败任务可重试且保留原文；关闭窗口不丢失最终状态；不将任务日志写入业务表。
- **输出**：展示 loading/success/failed 状态和重试入口；无任务显示空状态。

## 第三部分：枚举汇总表

| 枚举名（表.字段） | 值集 | 默认值 | 说明 |
|---|---|---|---|
| goal.period | `annual \| quarterly \| monthly` | `quarterly` | 目标周期 |
| goal.metric_type | `numeric \| milestone \| status` | 无 | 目标度量类型 |
| goal.status | `active \| done \| abandoned` | `active` | 目标状态 |
| project.status | `active \| done \| paused` | `active` | 生活项目状态 |
| task.status | `todo \| doing \| done` | `todo` | 待办状态 |
| habit.frequency_type | `daily \| weekly_times` | `daily` | 习惯频率类型 |
| milestone.is_done | `0 \| 1` | `0` | 里程碑完成标志 |
| inbox_item.kind | `link \| snippet \| read_later` | 无 | 收藏类型 |
| inbox_item.status | `pending \| clipped \| bookmarked \| discarded` | `pending` | 收藏处理状态 |
| document.doc_type | `webpage \| pdf \| docx \| markdown \| txt \| html \| note \| skill \| prompt \| promoted` | 无 | 文档类型 |
| document.parse_status | `pending \| ready \| failed` | `ready` | 文档解析状态；导入任务创建时显式写 pending |
| document.index_status | `pending \| indexing \| ready \| failed \| stale` | `pending` | 文档向量索引状态 |
| timeline_entry.type | `diary \| idea \| decision \| reading \| review` | 无 | 沉淀条目类型 |
| dev_project.status | `active \| done \| paused \| archived` | `active` | 开发项目状态 |
| dev_project_stage.is_terminal | `0 \| 1` | `0` | 是否终止阶段 |
| project_idea.status | `inbox \| evaluating \| approved \| discarded \| launched` | `inbox` | 想法漏斗状态 |
| mood_record.intensity | `1 \| 2 \| 3 \| 4 \| 5` | 无 | 情绪强度，CHECK 范围 |
| mood_word.is_builtin | `0 \| 1` | `0` | 内置/自定义情绪词标志 |

> `app_setting.key` 的受控键（提醒、AI、数据源、备份保留期）不属于数据库 CHECK 枚举；应用层必须维护键白名单和 value_json schema。`entity_link.from_type/to_type` 为多态实体类型，应用层需维护实体白名单并验证 ID 存在。

## 第四部分：状态机汇总

### 4.1 目标、项目、待办与习惯

| 实体 | 合法流转 | 非法路径/处理 |
|---|---|---|
| goal | `active → done`；`active → abandoned` | done/abandoned 不可再记录数据点、推进里程碑或回 active；删除需独立操作 |
| project | `active ↔ paused`；`active/paused → done` | done 不直接回 active；结束时间自动补齐或由用户填写 |
| task | `todo → doing → done`；`done → todo`（撤销） | done 不可再推进；系统不允许 todo 直接被后台自动完成 |
| milestone | `0 ↔ 1`（仅 active milestone 目标） | done/abandoned 目标不可变更；删除需单独确认 |
| habit_checkin | 未打卡 → 已打卡；已打卡 → 未打卡（撤销） | 同一 habit+日期唯一；不得补未来日期；streak 由历史回算 |

### 4.2 收藏箱与文档索引

| 实体 | 合法流转 | 非法路径/处理 |
|---|---|---|
| inbox_item | `pending → clipped`（剪藏）；`pending/bookmarked → discarded`；`discarded → pending`；`pending → bookmarked`（仅保留链接） | clipped 不等于删除文档；snippet 不可执行网页剪藏；已丢弃条目恢复后不丢 document 关联 |
| document.parse_status | `pending → ready`；`pending → failed`；`failed → pending`（重新解析）；`ready → pending`（内容重解析） | 解析失败保留原件和上一可用版本；raw_text 为空或 chunk 未事务提交时不得标 ready |
| document.index_status | `pending → indexing → ready`；`indexing → failed`；`failed → indexing`（重试）；`ready → stale → indexing`（重建） | 部分向量成功不得标 ready；failed/stale 保留原文和 chunk；软删除立即排除检索但延后物理清理 |

文档处理完成态必须严格区分：

| 完成态 | 必要条件 | 可用能力 |
|---|---|---|
| 已采集 | 原件/网页/手写内容已取得并通过基本格式校验 | 仅能继续入库，不能搜索 |
| 已入库 | `parse_status=ready`、raw_text 非空、document 与全量 chunk 已在同一 MySQL 事务提交 | 文档查看、编辑、标签与 MySQL FULLTEXT；不保证语义检索 |
| 正在索引 | `index_status=pending/indexing` | 保留已入库能力；新文档暂不可 RAG，编辑文档可继续使用未失效旧索引 |
| RAG 就绪 | `index_status=ready` 且 K03-F03 校验通过 | 语义检索、RAG 问答和真实引用定位 |
| 降级可用 | `index_status=failed/stale`，但 MySQL 入库数据完整 | 文档查看与 FULLTEXT；显示重试/重建入口，不得宣称 RAG 就绪 |

### 4.3 想法与开发项目阶段

| 实体 | 合法流转 | 非法路径/处理 |
|---|---|---|
| project_idea | `inbox → evaluating → approved → launched`；`evaluating → discarded`；`discarded → inbox`（复活） | inbox 不得跳过评估到 approved；discarded 不得直接 launched；launched 不重复立项 |
| dev_project.status | `active ↔ paused`；`active/paused → done`；`active/paused/done → archived` | archived 默认不可拖动；删除与归档不同，删除需二次确认并清理关联 |
| dev_project_stage | 项目创建生成有序阶段；current_stage 只能在同项目阶段间前进/后退 | 不得跳过阶段、跨项目拖动；删除当前阶段前必须迁移；终止阶段不自动把项目标 done |

### 4.4 沉淀升格与关联

- `timeline_entry` 始终保留；升格是单向复制：`timeline_entry → document(doc_type=promoted)`，回填 `timeline_entry.document_id`，原记录不可被删除替代文档。
- `entity_link` 为 0~n 双向关系；创建必须验证两端实体存在，删除任一实体时清理关联；情绪记录默认不与其他实体建立关联。
- 文档升格后进入 FULLTEXT、分块、embedding 和 RAG；普通 diary/idea/decision/reading/review 在未升格前不进入 RAG。

## 第五部分：待确认项清单

| 待确认项 | 推荐方案 | 影响 |
|---|---|---|
| 时间预算字段 | 在 `task` 增加 `estimated_minutes`（或独立预算表），并补充迁移与枚举/校验；确认前仅提供日历只读视图 | 当前 schema 无时长/预算字段，无法实现可持久化时间预算 |
| 目标/项目完成后的恢复 | MVP 保持终态不可回 active；若需要恢复，新增显式恢复操作和审计字段 | 影响状态机与历史统计 |
| 文档软删除保留期 | 默认 30 天，可由 `backup.retention_days` 配置 | 影响磁盘占用、恢复窗口和清理任务 |
| 本地 embedding 模型 | 默认 bge-small-zh-v1.5，首次运行下载并显示大小/进度，支持离线模型包 | 影响安装包体积、中文检索效果和首次可用时间 |
| LLM provider 与数据告知 | 首选 DeepSeek 兼容 API；设置页明确展示发送的情绪快照或知识 chunk | 影响隐私告知、费用和网络降级 |
| 导入原件保存 | 默认复制到应用数据目录，`source_path` 仅作来源记录 | 占用磁盘但保证原件可重建 |
| 日/月/季度回顾的固定模板 | 复用 timeline_entry.type=review，模板文本存 content，不新增表 | 影响回顾入口和去重规则 |
| entity_link 实体白名单 | v1 允许 goal/project/task/document/dev_project/timeline_entry，情绪默认关闭 | 影响关联 UI、清理逻辑与隐私边界 |
| 设置键白名单 | 在 shared contracts 固化 key 和 value_json schema，拒绝未知键 | 防止 JSON 设置漂移和不可迁移配置 |
| Milvus 模型版本原子切换 | 推荐将 Milvus 主键改为独立 `vector_id`（如 chunk_id+model_version），保留 chunk_id 普通字段，并以活动 model_version 过滤；或采用新 collection+alias 原子切换 | 当前 `chunk_id` 单主键只能覆盖同一 chunk 的旧模型向量，无法实现"新模型全量就绪后再删除旧向量" |
| AI 助手问答历史持久化 | **已拍板（2026-08-21）**：持久化，新增 `ai_assistant`/`ai_session`/`ai_message` 三表支持对话恢复；原"不持久化"建议作废，详见 `docs/decisions/P1-AI助手扩展决策.md` | 影响对话恢复、存储与隐私 |
| 收藏箱 snippet 转文档 | **已拍板（2026-08-21）**：snippet 允许剪藏入库，内容=note（无则 title），生成 `doc_type=note` 文档走统一入库；剪藏入库依赖 K02-F10 管线（P1-4），P1-3 以端口降级交付，详见 `docs/decisions/P1-收藏箱决策.md` | 补 K01-F03 缺口，影响收藏箱处理路径 |
