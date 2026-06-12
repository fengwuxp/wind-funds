# GSD-2 Agent Loop / Plan Grant 默认授权策略

## 1. 文档定位

本文是 wind-funds 在 GSD-2 阶段进入 `Agent Loop Engineering` 模式后的默认授权策略卡。它把 `GSD + Goal`、Agent Loop、Plan Grant、CAD 候选和 Git 策略拆开，说明哪些低风险本地动作可以默认推进，哪些动作必须停止并重新确认。

本文不是 PRD、系分设计、OpenSpec 正文、编码授权、生产发布授权或 Git 提交授权。它只授权当前工作区内的低风险文档、状态账本、只读差距复核、Spec / AC / Harness / CAD 任务卡和本地文档门禁；不授权生产代码、测试代码、公共契约、DDL/H2 schema、运行时配置、OpenSpec 异常状态修复、Git add/commit/push、联网、依赖安装、部署、生产数据、真实资金动作或不可逆操作。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W0B-AGENT-LOOP-PLAN-GRANT-2026-06-12` |
| 原子任务 | 固化 Agent Loop Engineering 模式和 GSD/CAD 默认低风险 Plan Grant 授权策略。 |
| 所属阶段 | GSD-2 Wave 0B / Plan Grant Baseline / Planning-only。 |
| Goal ID | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| Loop ID | `GSD2-LOOP-DEFAULT-PLAN-GRANT-2026-06-12` |
| 当前状态 | `PLAN_GRANT_ACTIVE_LOW_RISK_DOCS_AND_BASELINE_ONLY` |
| Plan Grant | `Active`，仅覆盖低风险文档、状态、只读复核、任务卡和本地门禁。 |
| Owner | AI Native 流程编排负责 Loop、Goal、Plan Grant 和停止条件；产品架构专家负责业务目标、能力地图、对象模型、验收和产品风险；资深架构师负责系统边界、接口契约、TDD、CAD 准入、验证和工程风险。 |
| 写入范围 | 本文、GSD-2 新基线工作流规划、AI 代码交付闭环基线、GSD-2 单一 Grant 任务模板、TDD README、docs README。 |
| 写入文件 | `docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/AI代码交付闭环与Spec模板基线.md`、`docs/TDD设计/GSD-2-Spec-AC-Harness-CAD任务模板.md`、`docs/TDD设计/README.md`、`docs/README.md`。 |
| 只读范围 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、源码、测试、Justfile、AGENTS.md、Git 状态和历史 Grant 材料。 |
| Git 策略 | `summary_only`。本文不授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |

## 2. 准入结论

当前可以进入 `Agent Loop Engineering`，但只进入低风险 Plan Grant Loop，不进入源码级 CAD Loop。

业务目标：让 wind-funds 的 GSD-2 工作流可以持续推进生产可用资金底座能力，优先服务 VCC 发卡、VCC 交易处理、全球收付款、清结算、对账、账目和钱包能力的可交付闭环。

用户价值：用户后续说“继续推进”“按计划推进”“进入 Agent Loop”时，Agent 能直接读取状态载体、选择范围内低风险动作、运行本地门禁、回写状态和输出交接，而不是每次都停在重复确认任务计划。

成功指标：

1. 每轮推进都能说明 Goal、Wave / Task、写入范围、只读范围、验证命令、停止条件和交接物。
2. 低风险本地任务可以默认推进；高风险资金、公共契约、代码、DDL、Git、联网和生产动作必须显式确认。
3. 任一候选进入编码前，必须补齐 Spec / AC / Red / Harness / CAD 任务卡，并由资深架构师确认单一 Grant。
4. 验证失败、范围漂移、连续无新增证据或工作树冲突时，Loop 必须暂停并回写状态。

非目标：

1. 不把 Plan Grant 写成长期无限授权。
2. 不把 Goal Active 写成代码、测试、上线或 Git 授权。
3. 不把 CAD Mode 扩大到整个 Roadmap。
4. 不修复当前 `openspec` 的 staged 删除与未跟踪同名文件并存状态；该状态只作为停止条件和只读事实记录。

## 3. 能力地图和对象模型

| 能力域 | 默认推进边界 | 必须显式确认 |
| --- | --- | --- |
| GSD 状态账本 | 更新 Goal、Wave、Task、状态、Not Done、验证矩阵和 handoff。 | 删除历史证据、重写已提交设计结论、改变 OpenSpec 权威口径。 |
| Agent Loop | 读取状态、选择一个低风险动作、执行、验证、回写和交接。 | 跨 Wave 扩大写入范围、连续无证据硬跑、把 Loop 当上线审批。 |
| Spec / AC / Harness | 新增或完善任务卡、AC 表、Red 卡、验证命令和停止条件。 | 把模板填写完成写成编码完成或生产 Done。 |
| Gap Audit | 只读复核 PRD、DSL、系分、TDD、OpenSpec、源码、测试和 Git diff。 | 在未确认 Grant 时修改 Java、测试、DDL/H2、公共契约或运行配置。 |
| CAD 候选 | 形成单一原子任务候选、写入范围、验证命令和风险缺口。 | 直接进入 Red/Green 写代码；跨多个任务并行写入。 |
| Git 交接 | 输出建议提交切片和建议 commit message。 | `git add`、`git commit`、push、PR、merge、rebase、reset。 |

业务对象和字段口径：

| 对象 | 生命周期 / 状态 | 字段口径 | 本策略中的作用 |
| --- | --- | --- | --- |
| Goal | Draft、Active、Verified、Closed、Blocked。 | `Goal ID`、目标、成功指标、非目标、预算、停止条件。 | 固定为什么推进和做到什么算完成。 |
| Wave | Candidate、Active、Verified、Paused、Closed。 | `Wave ID`、阶段目标、依赖顺序、写入范围、验证命令。 | 固定先做什么、后做什么。 |
| Plan Grant | Draft、Active、Suspended、Expired。 | 授权范围、Git 策略、验证范围、停止条件、审计交接。 | 允许范围内低风险任务默认推进。 |
| CAD Grant | Candidate、Active、Verified、Paused。 | 单一 Task ID、写入文件、验证命令、撤销方式。 | 只覆盖一个原子代码或测试任务。 |
| Handoff Card | Draft、Ready、Consumed。 | 状态载体、反馈源、验证者、下一 owner。 | 防止跨轮丢状态。 |

## 4. Agent Loop 契约

| 字段 | 内容 |
| --- | --- |
| Loop ID | `GSD2-LOOP-DEFAULT-PLAN-GRANT-2026-06-12` |
| 关联 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| Loop 类型 | `Plan Grant Loop / low-risk local baseline` |
| 触发条件 | 用户要求“继续推进”“按计划推进”“进入 Agent Loop Engineering”“开启 GSD/CAD 默认授权策略”或等价表达。 |
| 状态载体 | 本文、GSD-2 新基线工作流规划、AI 代码交付闭环基线、GSD-2 单一 Grant 任务模板、TDD README、docs README、Git status。 |
| 决策输入 | 用户最新指令、AGENTS.md、PRD/DSL/系分/TDD/OpenSpec、源码和测试只读事实、验证结果、工作树状态。 |
| 调用 Skill / 工具 | AI Native Engineering Workflow 编排；产品架构专家做产品语义和验收；资深架构师做架构边界、TDD、CAD 和 CR；本地 checker、`rg`、`git status`、`git diff --check` 做反馈。 |
| 允许动作 | 低风险文档同步、状态回写、只读 Gap Audit、Spec/AC/Harness/CAD 任务卡、验证矩阵、建议提交切片、文档门禁和 diff 检查。 |
| 禁止动作 | 未确认单一 Grant 前，不写 Java、测试、公共契约、DDL/H2 schema、运行时配置，不处理 `openspec` 异常 staged/untracked 状态，不做 Git add/commit/push，不联网，不安装依赖，不部署，不触碰生产或真实资金。 |
| 反馈源 | Harness checker、产品交付物 checker、架构交付物 checker、`rg` 扫描、`git status --short`、`git diff --check`、用户反馈和后续专项测试。 |
| 验证者 | 文档结构由脚本验证；产品语义由产品架构专家复核；工程边界由资深架构师复核；优先级和高风险口径由用户确认。 |
| 预算 / 最大轮次 | 每个用户回合最多推进 3 个低风险文档或状态切片；每轮只选择 1 个最小动作。 |
| 无进展检测 | 连续 2 轮没有新增验证证据、状态变化、缺口收敛或用户确认时，暂停并输出下一 owner。 |
| 停止条件 | 触发代码/测试/DDL/公共契约/Git/联网/部署/真实资金/生产数据/外部规则确认，验证失败无法解释，工作树冲突，`openspec` 状态需要处置，或用户中断。 |
| 恢复入口 | 本文和 `docs/TDD设计/GSD-2-新基线工作流规划.md`。 |
| 交接物 | Execution Handoff Card、验证矩阵、Not Done、残余风险、下一候选任务和建议 commit message。 |
| 知识回流位置 | GSD-2 文档、AI 代码交付闭环、单一 Grant 模板、TDD README、AGENTS.md 候选建议；长期学习仍需用户另行授权。 |

## 5. 授权策略

Plan Grant 分为三层，本轮只开启第一层。

| 授权层 | 状态 | 可默认推进 | 必须停止 |
| --- | --- | --- | --- |
| 低风险文档和基线 | `Active` | 修改本文、GSD-2、AI 交付闭环、任务模板、README；运行本地文档 checker、`rg`、`git diff --check`。 | 需要 Git 提交、OpenSpec 状态修复、代码、测试、DDL、公共契约或生产动作。 |
| 单一 Grant 候选 | `Draft` | 起草任务卡、AC 表、Red 卡、Harness 摘要、验证命令和停止条件。 | 写 Red 测试、改实现、改 DTO/枚举/DDL、运行高成本或需额外权限命令。 |
| CAD Loop | `Not Active` | 无。 | 必须先由用户确认单一 Task ID，资深架构师检查 CAD 门禁，补齐写入范围、验证命令、Git 策略和撤销方式。 |

默认可执行动作：

1. 读取仓库内设计文档、源码、测试、OpenSpec 和 Git 状态。
2. 更新 GSD/TDD/流程文档、任务模板、状态账本和索引入口。
3. 做只读 Gap Audit 并输出缺口、风险、下一 owner 和候选优先级。
4. 运行本地文档结构检查、`rg` 一致性扫描、`git diff --check`。
5. 输出建议提交切片和建议 commit message。

显式确认动作：

1. 修改生产代码、测试代码、公共 DTO/Request/Query、枚举、错误码、状态机、DDL/H2 schema、Entity、Mapper、配置、Justfile 或 CI。
2. 修复、stage 或删除当前 `openspec` 的 staged 删除与未跟踪同名文件状态。
3. 执行 `git add`、`git commit`、push、PR、merge、rebase、reset、分支切换或清理未跟踪文件。
4. 联网、安装依赖、启动外部服务、访问外部 API、部署、读写仓库外目录。
5. 真实资金、客户资金、商户待结算资金、跨境、FX、卡组织、银行、ACH、SWIFT、合规、法务、财务、税务或生产数据相关决策。

## 6. Wave / CAD 状态机

```text
Round 0
-> GSD Candidate
-> Wave Plan
-> Plan Grant Active
-> Loop Candidate
-> CAD Candidate
-> CAD Loop Active
-> Verified / Paused / Escalated / Closed
```

当前状态：`Plan Grant Active`，但仅限低风险文档、状态和任务卡。

| 状态 | wind-funds 当前口径 | 下一步 |
| --- | --- | --- |
| Wave 0 | 已建立 GSD-2 新基线。 | 继续保持为状态载体。 |
| Wave 0A | 已建立 AI 代码交付闭环和单一 Grant 模板。 | 进入后续任务准入闸门。 |
| Wave 0B | 本文建立默认授权策略。 | 允许低风险默认推进。 |
| Wave 1 | 可做只读 Gap Audit。 | 默认可推进，但只写报告和状态。 |
| Wave 2 | 可起草单一 Grant 任务卡。 | 需要用户确认优先级后才能激活。 |
| Wave 3 | CAD Loop 未激活。 | 需要单一 Task ID、Grant 和资深架构师门禁。 |
| Wave 4 | 验证和提交切片只做建议。 | Git 操作需显式授权。 |

## 7. 业务流程、规则矩阵和运营口径

主流程：

1. 读取用户最新目标、AGENTS.md、GSD-2 状态载体和 Git 状态。
2. 判断当前动作是否落在低风险 Plan Grant。
3. 若在范围内，选择一个最小动作并执行文档或状态回写。
4. 运行对应本地门禁，读取反馈。
5. 回写验证矩阵、Not Done、残余风险和下一 owner。
6. 若触发停止条件，暂停并输出需要用户确认的最小问题。

异常流程：

| 场景 | 处理 | 不允许发生 |
| --- | --- | --- |
| 需要代码或测试写入 | 暂停，要求单一 Grant 和资深架构师 CAD 门禁。 | 在默认授权下直接改 Java 或测试。 |
| 工作树出现未归属变更 | 只读记录，必要时暂停。 | 回滚或覆盖用户变更。 |
| `openspec` staged/untracked 异常 | 标记为停止条件，等待显式授权。 | 借文档整理顺手 stage、删除或修复。 |
| 验证失败 | 定位是文档缺字段、脚本不适用还是范围越界；可修文档则修，越界则暂停。 | 把失败包装成通过。 |
| 连续无进展 | 暂停并输出下一 owner。 | 通过扩写过程文字制造进展。 |

规则矩阵：

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验证方式 | 确认方 |
| --- | --- | --- | --- | --- | --- | --- |
| 低风险默认推进 | 动作只涉及 GSD/TDD/流程文档、状态、任务卡和本地文档门禁。 | 允许在 Plan Grant 下直接执行。 | P0 | 2026-06-12 | `git status`、checker、`git diff --check`。 | AI Native + 架构师 |
| 代码写入 | 动作涉及 Java、测试、DDL/H2、公共契约或运行配置。 | 必须停止，形成单一 Grant 和 CAD 门禁。 | P0 | 2026-06-12 | Harness / CAD candidate 检查。 | 用户 + 资深架构师 |
| Git 操作 | 动作涉及 add、commit、push、PR、merge、reset 或分支。 | 默认不授权；除非用户明确提交策略。 | P0 | 2026-06-12 | `git status`、diff、验证证据。 | 用户 |
| OpenSpec 异常状态 | `openspec` 存在 staged 删除和未跟踪同名文件。 | 默认只读记录，不处置。 | P0 | 2026-06-12 | `git status --short --untracked-files=all`。 | 用户 + 架构师 |
| 资金或合规口径 | 涉及真实资金、客户资金、商户待结算、卡组织、跨境、FX、税务、会计、法务或合规。 | 只做设计分析，不给最终专业结论。 | P0 | 2026-06-12 | 专业确认、审计记录、验收矩阵。 | 产品 / 法务 / 合规 / 财务 / 风控 |

运营后台和数据口径：本文不新增运营后台能力，但要求后续每个 Grant 的交付说明都能让产品、运营、财务、风控和研发看到发生了什么、为什么推进、验证了什么、哪些没有做、下一步谁负责。

## 8. 接口、数据和一致性边界

接口契约：本文不新增入参、出参、错误码、幂等键、公共 DTO、Request、Query、枚举或状态机。后续任何契约变化都必须进入单一 Grant，说明兼容策略、调用方影响、错误语义、幂等摘要、验证命令和回滚方式。

数据方案：本文不新增表、索引、唯一键、H2 schema、Entity、Mapper、MapStruct converter 或数据迁移。后续任何数据结构变化都必须说明事务边界、一致性、补偿、对账、审计、回填、灰度和回滚。

可靠性、安全和审计：默认 Plan Grant 只能改文档和任务状态，可靠性风险主要是授权越界和状态漂移；安全风险主要是误触敏感配置、生产数据或真实资金规则。控制方式是工作树检查、停止条件、只读边界和交接记录。

## 9. 验证矩阵

验证方案：本文作为低风险授权策略卡，验证重点不是 Java 行为，而是流程结构、授权边界、入口引用、停止条件和交接证据。测试口径为本地文档门禁、静态检查、`rg` 引用扫描和 diff 空白检查；回归口径为 GSD-2、AI 交付闭环、任务模板、TDD README 和 docs README 能共同指向同一份默认授权策略。完成条件是下表检查通过，且没有扩大到代码、测试代码、DDL/H2、公共契约、OpenSpec 状态处置或 Git 操作。

| 验证层 | 命令或方式 | 通过口径 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md` | Task、Owner、写入/只读范围、Wave、上下文账本、禁止事项、验证和 handoff 字段齐全。 |
| 产品结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收字段齐全。 |
| 架构结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md` | 背景目标、现状约束、核心决策、契约、数据一致性、可靠性安全、验证、发布风险字段齐全。 |
| 状态一致性 | `rg "PLAN_GRANT_ACTIVE|GSD2-W0B|GSD-2-AgentLoop" docs` | 新授权策略能被 README、GSD-2 和交付闭环引用。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译、测试、PMD | 本轮不运行。 | 本轮仅文档、状态和授权策略变更，不改 Java、测试、DDL/H2 或运行时配置。 |

## 10. 发布、回滚、风险和交接

发布：本文不发布生产能力，不改变运行时行为，不改变资金事实、账本事实、交易事实、投影、清结算或对账结果。

回滚：如需回滚本轮文档变更，可通过 Git diff 反向处理；不得通过 `git reset --hard` 或删除未跟踪文件处理，除非用户明确授权。

风险和待确认：

1. 当前 `openspec` 存在 staged 删除和未跟踪同名文件并存状态，本轮默认授权不处理。
2. Plan Grant 只覆盖低风险本地文档和状态任务；后续若进入编码，仍需单一 Grant。
3. 用户若要求“提交本轮变更”，需要先确认 Git 范围，避免混入历史 staged/untracked 状态。
4. 涉及真实资金、客户资金、跨境、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规的结论仍需专业确认。

Execution Handoff Card：

| 字段 | 内容 |
| --- | --- |
| Goal ID | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| Wave / Task ID | `GSD2-W0B-AGENT-LOOP-PLAN-GRANT-2026-06-12` |
| 状态载体 | 本文、GSD-2 新基线工作流规划、AI 代码交付闭环、GSD-2 单一 Grant 模板、TDD README、docs README。 |
| 写入范围 | 低风险文档、状态账本、任务卡和索引入口。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Git 状态和历史 Grant。 |
| 反馈源 | checker、`rg`、`git status`、`git diff --check`、用户反馈。 |
| 停止条件 | 代码、测试、DDL、公共契约、Git、OpenSpec 状态处置、联网、生产或高风险资金/合规动作。 |
| Git 策略 | `summary_only`。 |
| 下一 owner | 若继续低风险基线任务，AI Native 可直接推进；若选择编码任务，用户先确认单一 Grant，资深架构师进入 CAD 门禁。 |
