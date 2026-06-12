# AI 代码交付闭环与 Spec 模板基线

## 1. 文档定位

本文是 wind-funds 在 GSD-2 之后进入 AI 代码交付时的闭环基线。它用于把 `GSD + Goal`、Spec 模板、Harness、TDD、代码 Review、验证命令、发布观测和知识回流串成一条可审、可执行、可停止的交付链。

本文不是 PRD、系分设计、OpenSpec 正文或编码授权，也不替代 `资深架构师` 的源码级实现和 CR。当前默认 Plan Grant 只覆盖低风险文档、状态、任务卡和本地门禁；任何生产代码、测试代码、公共契约、DDL/H2 schema、运行时配置或 Git 提交，仍必须由具体 Execution Grant、CAD Grant 或用户显式授权承接。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W0-AI-CODE-DELIVERY-LOOP-2026-06-12` |
| 原子任务 | 建立 AI 代码交付闭环和 Spec 模板准入基线。 |
| 所属阶段 | GSD-2 Wave 0 / Delivery Loop Baseline / Planning-only。 |
| Goal ID | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| Loop ID | `GSD2-LOOP-AI-CODE-DELIVERY-2026-06-12` |
| 当前状态 | `PLAN_GRANT_ACTIVE_LOW_RISK_DOCS_AND_BASELINE_ONLY` |
| Owner | AI Native 流程编排负责闭环、Goal、Loop 和门禁；产品架构专家负责业务目标、对象、验收种子和产品风险；资深架构师负责系统边界、Spec 强度、TDD、代码 Review、验证和交付准出。 |
| 写入范围 | 本文、Agent Loop / Plan Grant 默认授权策略、GSD-2 单一 Grant 任务模板、GSD-2 工作流规划、TDD README、docs README 的流程入口和准入说明。 |
| 写入文件 | `docs/TDD设计/AI代码交付闭环与Spec模板基线.md`、`docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md`、`docs/TDD设计/GSD-2-Spec-AC-Harness-CAD任务模板.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/README.md`、`docs/README.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、AGENTS.md、上一轮 GSD/Grant 历史材料。 |
| 只读参考 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、源码、测试和 Git 状态。 |
| Git 策略 | `summary_only`。本文不授权自动 `git add`、`git commit`、push、PR 或发布；默认 Plan Grant 的 Git 口径以 `GSD-2-AgentLoop-PlanGrant默认授权策略.md` 为准。 |

## 2. 当前瓶颈判断

当前主要瓶颈不是缺少 AI 编码速度，而是需要把 AI 生成能力绑定到资金域的事实源、Spec 强度、独立验证和 Review 减负上。否则会出现文档、任务、测试、实现和准出证据各说各话，或者把候选计划误读为生产 Done。

| 判断项 | 当前结论 |
| --- | --- |
| 业务目标 | 已有 MVP 方向：资金底座支撑 VCC 发卡、VCC 交易处理、全球收付款、清结算和对账；但具体编码仍需单一 Grant。 |
| 上下文来源 | PRD、DSL、系分、TDD、OpenSpec、AGENTS.md 和源码事实必须共同作为规范事实源。 |
| Spec 层数 | 必须区分 PRD、SDD/系分/OpenSpec、实现 Spec；小任务可以轻量，但不能缺层后让 AI 猜。 |
| Harness 越界风险 | 旧候选只能作为 backlog reference；未重新确认前不能进入 CAD 或生产代码写入。 |
| CR 认知负荷 | 后续代码变更必须提供入口路径、影响模块、源码锚点、AC/测试映射和验证证据。 |
| 测试验证 | 资金变化不能只证明接口不报错；必须证明账务平衡、余额桶、幂等、失败无副作用和审计。 |
| 知识回流 | 高频缺口应回流到 TDD 卡、OpenSpec、AGENTS.md、测试 fixture、脚本或本文模板，而不是只写在会话总结中。 |

## 3. 交付闭环

AI 代码交付闭环按下列顺序推进。任何一步证据不足，都回到前一步补齐，而不是继续让 Agent 生成代码。

| 阶段 | Owner | 目标 | 交接物 | 停止条件 |
| --- | --- | --- | --- | --- |
| 1. 意图对齐 | 产品架构专家 + 用户 | 明确业务目标、使用者、非目标、验收种子和风险确认方。 | 产品上下文摘要、验收种子、待确认清单。 | 主体、资金来源、账务影响或外部规则不清。 |
| 2. Spec 定界 | 资深架构师 | 把产品目标映射到模块、接口、数据、状态、不变量和实现边界。 | Spec 卡、AC 表、写入范围、禁止范围。 | 需要公共契约、DDL、状态机或权限决策但未确认。 |
| 3. Harness 编排 | AI Native 流程编排 | 固定 Task ID、Goal、Wave、依赖顺序、验证命令、停止条件和交接。 | Harness 摘要、Plan Grant 或 Execution Grant 缺口。 | 写入范围、验证命令、Git 策略或停止条件缺失。 |
| 4. Red 优先 | 资深架构师 | 先写或确认失败用例，证明资金不变量和 forbidden facts。 | Red 卡、目标测试资产、最小断言。 | 测试只能断言状态、数量或不报错。 |
| 5. 最小实现 | 资深架构师 | 在授权范围内完成最小 Green，不做无关重构。 | 代码 diff、测试 diff、实现说明。 | 越过 Grant、引入无主依赖、触碰生产/外部高风险。 |
| 6. 独立验证 | 资深架构师 + 脚本门禁 | 运行编译、测试、静态检查、spec/AC/漂移检查。 | 验证命令和结果。 | 验证失败且无法在授权范围内修复。 |
| 7. CR 减负 | 资深架构师 | 问题优先审查业务语义、边界、契约、失败路径和工程一致性。 | CR 结论、残余风险、Not Done。 | Review 无法复述影响范围或缺少源码锚点。 |
| 8. 交付准出 | AI Native 流程编排 | 汇总覆盖清单、验证证据、发布/回滚边界和下一 owner。 | 交付说明、状态账本、建议 commit message。 | 缺验证、缺 Not Done、缺回滚或 Git 授权不明。 |
| 9. 知识回流 | 对应 owner | 把可复用经验回流到权威载体。 | TDD、OpenSpec、AGENTS.md、fixture、脚本或模板更新建议。 | 涉及长期学习、敏感数据或未验证猜测。 |

Wave 边界和依赖关系：

| Wave | 执行顺序 | 并行边界 | 准出条件 |
| --- | --- | --- | --- |
| Wave 0 | 先建立状态载体、交付闭环和 Spec 模板。 | 可与只读资料盘点并行，不与代码写入并行。 | 文档门禁、入口引用和停止条件清楚。 |
| Wave 0B | 再建立 Agent Loop / Plan Grant 默认授权策略。 | 只允许低风险文档、状态、任务卡和本地门禁，不与代码写入并行。 | 默认推进边界、显式确认边界、停止条件和 handoff 清楚。 |
| Wave 1 | 再做设计、代码、任务 Gap Audit。 | 只读扫描可并行，缺口裁决必须汇总到同一状态账本。 | 每个缺口标注 Spec 强度、AC 覆盖和验证证据。 |
| Wave 2 | 再选择单一 Grant。 | 互不重叠的候选也必须一次只激活一个 Grant。 | 写入范围、验证命令、停止条件和 owner 齐全。 |
| Wave 3 | 最后进入 CAD Loop。 | 同一轮只允许一个原子任务进入写入。 | Red/Green、独立验证、CR 和状态回写完成。 |

## 4. Spec 强度选择

后续任一 GSD-2 候选进入编码前，先按风险选择 Spec 强度。强度只服务风险和验证，不为形式感加重，也不为 AI 写得快而降级。

| 强度 | 使用条件 | 最小内容 | wind-funds 示例 |
| --- | --- | --- | --- |
| 轻量任务卡 | 单文件、低风险、无公共契约、可快速验证。 | 目标、非目标、写入范围、验证命令、停止条件。 | 文档索引、任务状态、只读 Gap Audit。 |
| 可评审 Spec | 涉及契约、状态、权限、数据、流程或多模块。 | 背景目标、设计取舍、AC、测试策略、风险自查、发布边界。 | 资金责任目标字段、交易投影解释查询、余额调账审计。 |
| Harness/GSD Spec | 长任务、多模块、上下文易衰减或需要跨轮交接。 | 可评审 Spec + Task ID、Owner、依赖顺序、写入范围、验证矩阵和 handoff。 | 账户层级、清结算对账差错 MVP、P2 VCC 支撑能力。 |
| CAD 候选 Spec | 原子任务边界清楚、验证可运行、授权明确。 | Harness/GSD Spec + Execution Grant、停止条件、恢复入口。 | 单个测试类 Red/Green、单个 Request/DTO 兼容改动。 |
| 人工主导 | 高不确定资金、合规、安全、生产数据、外部规则或不可逆操作。 | 人工确认、专业审批、dry-run、回滚、审计和人工验收。 | 卡组织规则、真实清算资金、生产修数、跨境/FX 结论。 |

## 5. Spec 模板

每个进入 GSD-2 Wave 2 或 Wave 3 的候选，至少补齐下列实现 Spec 字段。字段可以很短，但不能留给 AI 推断。实际创建单一 Grant 任务卡时，优先复制 [GSD-2-Spec-AC-Harness-CAD任务模板.md](GSD-2-Spec-AC-Harness-CAD任务模板.md)，再按本节裁剪。

```text
Spec ID:
关联 Goal:
状态: Draft / Review / Approved / Implementing / Verified / Superseded
Owner:
来源材料:
文档层级: PRD / SDD / 实现 Spec / 混合但已分层
规范事实源:
风险等级:
Spec 强度:
当前结论:
需要人拍板:

0. 摘要
目标:
非目标:
本期成功:
本次不做:
Goal 成功标准:
预算 / 时间盒:
停止条件:

1. 背景与上下文
业务意图:
用户 / 主体 / 验收方:
证据来源:
产品上下文 / PRD-Lite 链接:
项目知识引用:
隐性依赖 / 历史坑点:
输入输出示例:

2. 系统设计与实现空间
入口路径:
影响模块:
核心对象 / 状态 / 不变量:
接口 / 协议 / DDL / 配置:
结构化契约:
时序 / 流程 / 事务边界:
方案取舍:
写入范围:
只读范围:
正例 / 反例:
禁止事项:

3. 质量保障
AC 表:
Goal / AC 映射:
测试策略:
五支柱验证:
风险自查:
静态检查:
漂移检查:
人工确认:

4. 交付发布
Harness 摘要:
验证命令:
灰度 / 发布:
监控:
回滚:
残余风险:
知识回流:
```

结构化契约优先使用项目已有 DTO、Request、Query、DSL case、表设计、JSON 示例、fixture 或测试资产；确实不适用时写 `N/A` 和原因。不得让 Agent 自行发明字段、枚举、依赖、状态或返回结构。

## 6. AC 与测试映射

AC 是从 Spec 进入机器验证的主索引。后续任务的 P0/P1 AC 必须可回链 Goal 成功标准、资金事实和测试证据。

| 字段 | 规则 |
| --- | --- |
| AC ID | 使用全局唯一编号，例如 `AC-GSD2-B2-001`；进入测试或 CR 后不因文字润色重排。 |
| Given | 写清主体、账户、余额桶、route、历史事实、支付工具或外部引用。 |
| When | 写清触发服务、指令、事件、任务或查询动作。 |
| Then | 写清必须产生的事实、余额影响、账本分录、投影、差错、审计或错误。 |
| Forbidden Facts | 写清不得产生的半截 route、posting、entry、外部出款、敏感导出、治理反写或重复副作用。 |
| 验证方式 | 测试函数、fixture、静态检查、漂移检查、监控或人工确认。 |
| 证据状态 | `Draft / Covered / Verified / Waived`；`Waived` 必须说明原因、影响、替代证据和批准方。 |

资金变化 AC 的最低断言：状态、账户主体、金额、币种、账目、余额桶、route snapshot、posting plan、ledger transaction、LedgerEntry、余额投影、幂等、审计和失败无副作用。

### 6.1 规则矩阵

后续 Grant 的规则矩阵用于把产品规则、资金红线、Spec AC 和测试证据连起来。规则只要影响资金事实、权限、安全、幂等、清结算、对账、审计或发布准出，就必须写入矩阵。

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验证方式 |
| --- | --- | --- | --- | --- | --- |
| Spec 准入 | 候选准备进入 Wave 2 或 Wave 3。 | 必须有 Spec ID、Goal 映射、写入范围、禁止范围、AC 表和验证命令。 | P0 | 随任务卡版本。 | Harness checker、人工 Review。 |
| 资金不变量 | 任务涉及金额、余额、账本、投影、清结算或对账。 | 必须证明 route、posting、entry、projection、幂等和失败无副作用。 | P0 | 随 PRD/DSL/TDD 版本。 | Red 卡、服务级测试、分组测试。 |
| 公共契约变更 | 任务触碰 DTO、Request、Query、枚举、状态机、错误码或 DDL/H2。 | 必须有单一 Execution Grant、兼容策略、回归范围和漂移检查。 | P0 | 随 OpenSpec change。 | `rg`、编译、边界测试、相关业务测试。 |
| AI 重试 | 同类生成错误重复或验证失败。 | 连续两轮无新增证据时停止，回写 Spec/AC/TDD/Harness 并升级 owner。 | P1 | 随 Loop 状态。 | 验证结果、状态账本、CR 结论。 |

## 7. Harness 三层闭环

后续 GSD-2 任务的 Harness 摘要按三层承载，避免把计划、知识和交付证据混在一起。

| 层 | 必填信息 | 交付口径 |
| --- | --- | --- |
| Orchestrator | Task ID、Goal ID、Owner、Wave、依赖顺序、写入范围、只读范围、禁止事项、停止条件、恢复入口。 | 说明 Agent 本轮能做什么、不能做什么、失败后回哪里。 |
| Knowledge | 领域术语、业务对象、不变量、历史坑点、架构决策、外部规则待确认、AGENTS.md 规则。 | 让 Agent 消费稳定上下文，而不是重复猜测。 |
| Delivery | Red、测试、编译、静态检查、CR、发布观测、回滚、知识回流和效果指标。 | 用独立证据证明能交付，而不是只看代码生成完成。 |

最小 Harness 摘要：

```text
Task ID:
Goal ID:
Goal 状态:
Goal 成功标准:
目标:
Spec 强度:
Owner:
来源上下文:
写入范围:
只读范围:
依赖顺序:
禁止事项:
验证命令:
独立验证证据:
CR 交接要求:
知识回流位置:
效果指标:
停止条件:
Goal Ledger 更新:
恢复入口:
```

## 8. 闸门管道

闸门不是某个单一脚本，而是一组独立于生成者的准出证据。后续任务按风险选择执行。

| 闸门 | 目的 | wind-funds 当前承接方式 |
| --- | --- | --- |
| `spec-lint` | 检查 Spec 必填段、owner、风险等级、AC、写入范围和授权缺口。 | 先用本文模板和 Harness checker 人机结合检查；必要时后续沉淀脚本。 |
| `ac-coverage` | 检查 P0/P1 AC 是否能回链测试、fixture、静态检查、监控或人工确认。 | TDD Red 卡、测试证据包和交付说明共同承接。 |
| `drift-check` | 检查 DTO、DDL/H2、状态枚举、错误码、路由、DSL case、测试 fixture 是否偏离 Spec。 | `rg`、diff、OpenSpec/TDD 映射和相关测试。 |
| 构建 / 静态检查 | 证明代码可编译、静态规则可过。 | `just compile`、`just pmd`。 |
| 资金测试 | 证明资金事实、账务、余额、幂等和失败无副作用。 | `just test-one`、`just test-ledger`、`just test-transaction`、`just test-balance-control`、`just test-business-flow`。 |
| 架构边界 | 证明模块方向、公共契约和禁止依赖未被破坏。 | `just test-boundary`、`just test-governance`。 |
| 完整收口 | 声明本地完整验证证据。 | `just verify-cad`。 |
| 发布观测 | 证明灰度、监控、告警、回滚和人工兜底。 | 本轮不授权发布；具体生产 Grant 单独补。 |

默认授权闸门：后续 Agent Loop 只要仍落在 `GSD-2-AgentLoop-PlanGrant默认授权策略.md` 的低风险范围内，可以直接推进文档、状态、任务卡和本地门禁；一旦需要代码、测试、公共契约、DDL/H2、Git、OpenSpec 状态处置、联网、部署或真实资金规则，必须停止并形成单一 Grant。

## 9. Agent Loop 反馈和重试

AI 生成失败时，优先回写 Spec、AC、测试或 Harness，而不是只换提示词。

| 步骤 | 动作 |
| --- | --- |
| 读取状态 | 读取 Goal、Spec、Harness、TDD、Git 状态和上一轮验证结果。 |
| 选择动作 | 只选择一个最小低风险任务或一个已授权 CAD 子任务。 |
| 执行变更 | 按写入范围修改文档、测试或代码。 |
| 独立验证 | 运行约定命令，保留成功或失败证据。 |
| 读取反馈 | 判断是 Spec 缺口、实现缺陷、测试资产缺口、环境问题还是授权越界。 |
| 回写 | 回写 Spec、AC、TDD、Harness、状态账本或 OpenSpec。 |
| 继续 / 停止 | 连续两轮无新增证据、同类错误重复、验证失败不可修或越界时停止。 |

重试上限：同一任务连续两轮出现同类 AI 错误，或连续两轮没有新增证据、状态变化、测试收敛或缺口收敛，必须暂停并升级 owner。

## 10. CR 减负交接

进入源码级 CR 前，交付说明必须让 Reviewer 能快速复述“为什么改、影响哪里、怎么证明、还剩什么风险”。

| 交接项 | 必须说明 |
| --- | --- |
| 业务意图 | 关联 Goal、业务目标、使用者和成功标准。 |
| 入口路径 | 触发服务、application facade、transaction/ledger/wallet/reconciliation 入口。 |
| 影响模块 | core、wallet、transaction、ledger、reconciliation、tests、docs、openspec 中的具体范围。 |
| 边界变化 | 公共契约、状态机、表结构、幂等、事务、审计或权限是否变化。 |
| 源码锚点 | 关键类、方法、DTO、测试类或 fixture。 |
| Spec / AC 映射 | 覆盖哪些 AC、DSL case、TDD Red 和 forbidden facts。 |
| 验证证据 | 实际命令、结果、未执行原因和替代证据。 |
| 残余风险 | Not Done、外部规则、合规、发布、数据迁移、并发或性能风险。 |

## 11. 知识回流和指标

知识回流只保存可复用、可验证、低敏感的团队知识。一次性探索、未验证猜测、外部文章原文、客户敏感信息、密钥、生产配置和用户长期偏好不写入仓库长期规则。

| 回流位置 | 内容 |
| --- | --- |
| `AGENTS.md` | 长期有效的项目规则、命令、安全边界、禁止事项。 |
| PRD / DSL / 系分 / TDD | 已确认的产品语义、系统边界、测试证据和交付口径。 |
| OpenSpec / Harness | 单个变更的目标、范围、AC、验证、状态和交接。 |
| 测试 / fixture | 失败样例、边界样例、资金事实和回归路径。 |
| 脚本 / CI | 可机械检查的高频问题，例如 Spec 结构、AC 覆盖、漂移检查。 |
| 技能 reference | 跨项目可复用的方法，不包含当前项目敏感事实。 |

建议指标：

| 指标 | 用途 |
| --- | --- |
| 一次通过率 | 衡量 Spec、测试和实现是否足够清楚。 |
| 返工率 | 衡量需求/设计/Spec 漂移。 |
| CR 轮次 | 衡量交接可理解性和机器门禁覆盖度。 |
| 缺陷密度 | 衡量 AI 生成是否引入资金、安全或边界问题。 |
| 验证失败恢复成本 | 衡量失败能否快速定位到 Spec、测试、实现或环境。 |
| 上下文重建成本 | 衡量状态账本和 handoff 是否足够稳定。 |

## 12. GSD-2 接入规则

GSD-2 后续任务必须消费本文作为前置闸门：

1. Wave 1 Gap Audit 只读复核时，需标注每个缺口需要的 Spec 强度、AC 覆盖和验证门禁。
2. Wave 2 选择单一 Grant 时，必须复制 `GSD-2-Spec-AC-Harness-CAD任务模板.md` 或等价任务卡，补齐 Spec ID、Goal 映射、写入范围、禁止范围、验证命令、停止条件和知识回流位置。
3. Wave 3 CAD Loop 执行时，每轮只允许一个原子任务，先 Red 或目标测试资产，再最小实现，再独立验证，再 CR，再状态回写。
4. Wave 4 交付收口时，必须输出覆盖清单、验证命令、验证结果、Not Done、残余风险、建议 commit message 和下一 owner。
5. 未通过本文闸门的候选，只能保持 backlog reference 或 design-only，不得写生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。

## 13. 验证矩阵

| 验证层 | 命令或方式 | 当前通过口径 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/AI代码交付闭环与Spec模板基线.md` | GSD Wave 必备字段齐全。 |
| 单一 Grant 模板结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-Spec-AC-Harness-CAD任务模板.md` | Spec、AC、Red、Harness、CAD、CR 和交付证据字段齐全。 |
| Agent Loop / Plan Grant 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md` | 默认授权范围、Loop 契约、验证者、停止条件、handoff 和显式确认边界齐全。 |
| 产品架构结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/AI代码交付闭环与Spec模板基线.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构方案结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/AI代码交付闭环与Spec模板基线.md` | 背景目标、边界取舍、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 文档一致性 | `rg` 扫描 GSD-2、TDD README、docs README 对本文的入口引用。 | 后续 GSD + Goal 入口能追踪到本文。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译和测试 | 本轮不运行 | 仅流程和文档基线变更，不改 Java、测试、DDL/H2 或运行时配置。 |

## 14. 残余风险和停止条件

残余风险：

1. 本文建立的是交付闭环基线，不代表任一候选已获得编码授权。
2. `spec-lint`、`ac-coverage` 和 `drift-check` 当前先由文档模板、脚本门禁、`rg` 和人工 CR 组合承接，尚未全部沉淀为独立自动化脚本。
3. OpenSpec 当前工作树存在索引状态需要单独确认；本文不改动 OpenSpec tracked/untracked 状态。
4. 涉及真实资金、客户资金、跨境、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规的结论，仍需专业确认。

停止条件：

1. 需要改公共契约、DDL/H2、生产代码、测试代码或运行时配置。
2. 需要 Git add、commit、push、PR、部署、联网、依赖安装或不可逆操作。
3. Spec、AC、写入范围、验证命令、停止条件或 owner 不完整。
4. 连续两轮没有新增证据、状态变化、测试收敛或缺口收敛。
5. 用户调整优先级、暂停、撤销授权或指定新的 Execution Grant。
