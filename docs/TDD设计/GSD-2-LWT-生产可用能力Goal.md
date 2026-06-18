# GSD-2 LWT 生产可用能力 Goal

## 1. 文档定位

本文是 ledger、wallet、transaction 三条资金底座被依赖能力的 `Loop + Goal` 状态载体，用于把已完成的 GSD2 切片、仍未 Done 的生产能力缺口和下一轮单一 Execution Grant 候选聚合到一张可持续推进的 Goal 卡中。

本文不是新的 PRD，不替代产品设计、DSL 设计、系分设计或 TDD 设计；也不是编码授权、测试写入授权、DDL/H2 schema 授权、公共契约变更授权或 Git 授权。本文只允许在低风险文档范围内同步状态、裁剪任务和提出下一轮候选。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` |
| 原子任务 | 建立 LWT 生产可用能力 Goal，聚合当前能力完备性、下一候选优先级、验证矩阵和 handoff。 |
| 所属阶段 | GSD-2 / LWT capability goal / docs-only Plan Grant。 |
| Goal ID | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| Loop ID | `GSD2-LWT-PRODUCTION-CAPABILITY-LOOP-2026-06-18` |
| 当前状态 | `WALLET_AUTHORIZATION_ADMISSION_GREEN_VERIFIED` |
| Git / code baseline | `ca603eab feat: 补齐交易投影解释剩余矩阵` 是进入本切片前的已提交基线；`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 已完成本地 Green，并随本轮提交固化为新的能力证据；`873e5f8c`、`a38776c5`、`bc7ffc0f`、`10853e2d`、`ae8cb8a6` 和 `e81a8a25` 保留为已消费能力证据。 |
| 关联入口 | [GSD-2-新基线工作流规划.md](GSD-2-新基线工作流规划.md)、[GSD-2-P0P1-LedgerWalletTransaction推进计划.md](GSD-2-P0P1-LedgerWalletTransaction推进计划.md)、[GSD-2-AUTH-Chargeback目标语义对齐任务卡.md](GSD-2-AUTH-Chargeback目标语义对齐任务卡.md)、[GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md](GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md)、OpenSpec `tdd-baseline-reset` tasks。 |
| Owner | AI Native 流程编排负责 Goal、Loop、状态和停止条件；产品架构专家负责业务价值、验收和 Not Done；资深架构师负责模块边界、契约、测试、验证和编码准入；用户确认单一 Grant。 |
| 写入范围 | 本文、GSD-2 入口、P0/P1 LWT 推进计划、AUTH Chargeback 目标语义任务卡、AUTH Chargeback 兼容入口确认包、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md`、`docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、ledger、wallet、transaction、core、reconciliation、tests、Justfile、AGENTS.md、最近 Git 提交和历史准入卡。 |
| 只读参考 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、源码、测试和 Git 提交记录。 |
| 上下文账本 | 本文是 LWT 当前状态账本；GSD-2 工作流、W5 推进计划、TDD README、docs README 和 OpenSpec tasks 是恢复入口。 |
| Git 策略 | `summary_only`。未获用户明确授权前，不执行 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |

### 1.1 Goal 层级和当前恢复入口

本轮存在父 Goal 和 LWT 子 Goal 两层状态，恢复时按下表理解，避免把历史收口状态误当当前授权。

| 层级 | ID / 状态 | 含义 | 当前动作 |
| --- | --- | --- | --- |
| 父 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` | GSD-2 总基线，负责清理旧活跃计划、维护 Wave、OpenSpec tasks、Plan Grant 和恢复入口。 | 继续作为总入口和历史证据账本，不直接授权编码。 |
| LWT 子 Goal | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` / `WALLET_AUTHORIZATION_ADMISSION_GREEN_VERIFIED` | 聚合 ledger、wallet、transaction 和 reconciliation 的条件生产基线、完备性矩阵、三卡交接和下一 Grant 队列。 | 继续作为当前 LWT 状态载体，记录 wallet 授权准入、AUTH 兼容 guard 和 B4 投影解释矩阵已消费。 |
| 当前可执行状态 | `WALLET_AUTHORIZATION_ADMISSION_GREEN_VERIFIED` | wallet application 层已新增 `AuthorizationAdmissionApplicationService`，完成支付工具准入、绑定校验、资金责任解析、账户能力校验和账户主体型授权内核委派的最小服务流。 | 下一轮默认先确认 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`，补授权准入的支付工具引用和绑定快照回链；若不继续 wallet，再从 ledger guard、B7 清算/结算 gate、B7 差异报告或 B5 审计扩展中确认单一 Grant。 |
| 历史证据状态 | `B7_RECON_DIFFERENCE_ACTION_GUARD_GREEN_VERIFIED`、`GSD2-B7-RECON-GATE-CONSUME-001/002`、`10853e2d` 等 | 只表示过去切片已完成或已消费，不能作为当前下一步或默认授权。 | 仅作为 Evidence Anchor 和 Not Done 边界来源。 |

恢复规则：下一轮必须先读取本文、GSD-2 工作流、W5 推进计划、AUTH Chargeback 兼容入口确认包和 OpenSpec tasks；若用户没有确认新的单一 Grant，只能继续 docs-only 状态维护。若用户改选 B4、wallet、ledger guard、B7 清算/结算 gate 或 B5 审计扩展，必须先同步本映射和第 8.1 节 Grant 决策账本。

### 1.2 Loop Progress Ledger 和无进展计数

本账本用于判断 `Loop + Goal` 是否仍在产生可交接证据，避免在没有新 Grant 时反复扩写同一组状态。每轮 docs-only 推进必须记录是否新增证据、状态变化或缺口收敛；若连续两轮没有新增证据、状态变化或缺口收敛，则停止扩写，等待用户确认单一 Grant 或重新选择候选。

| 轮次 | 日期 | 动作 | 新增证据或状态变化 | 无进展计数 | 下一判断 |
| --- | --- | --- | --- | --- | --- |
| `LWT-GOAL-001` | 2026-06-18 | 建立 LWT 生产可用能力 Goal、完备性矩阵、条件基线裁决和下一 Grant 队列。 | 新增本文、Completion Audit、Evidence Anchor Matrix、README 恢复导航和 OpenSpec tasks 记录。 | 0 | 可继续维护活跃状态载体。 |
| `AUTH-HANDOFF-002` | 2026-06-18 | 将 AUTH 兼容确认包、预检清单和运行卡同步到 Goal、W5、GSD-2、README 和 OpenSpec。 | `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 从“确认包存在”收敛为“确认包 + 第 11 节预检 + 第 12 节运行卡可消费”。 | 0 | 已由 `AUTH-COMPAT-005` 消费，保留为确认包准备证据。 |
| `LWT-PROGRESS-LEDGER-003` | 2026-06-18 | 补齐 Progress Ledger 和无进展计数口径。 | 将连续无进展停止条件落到本文、GSD-2 和 OpenSpec 的可恢复状态账本。 | 0 | 下一轮若没有新的 Grant、事实差异、验证证据或状态缺口收敛，应停止扩写并交还用户选择。 |
| `LWT-NO-GRANT-004` | 2026-06-18 | 复核当前工作树和运行时 Goal，确认未出现新的单一 Execution Grant，且本轮没有新的事实差异、验证证据或状态缺口收敛。 | 无新增能力证据；本轮只把“无新 Grant 时不继续扩写确认包”的判断回写为停止条件证据。 | 1 | 下一轮若仍未确认 Grant 且没有新事实，应停止 docs-only 扩写并交还用户选择，而不是继续维护同一确认包。 |
| `AUTH-COMPAT-005` | 2026-06-18 | 消费 `Execution Grant：GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`，完成兼容 chargeback 最小审计 guard、契约说明、Red/Green、交易分组、compile 和 PMD。 | 新增失败无副作用 Red，证明缺 `externalDisputeRef` 的兼容 chargeback 不得入账；Green 后目标测试 32 tests、transaction 分组 107 tests、compile 和 PMD 均通过。 | 0 | 本 Grant 已消费；下一轮不得沿用本 Grant 扩完整 dispute case，需重新确认 B4-002 或其他单一 Grant。 |
| `B4-PROJECTION-006` | 2026-06-18 | 消费 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002` 首轮争议退款解释切片。 | 新增争议退款投影解释 Red/Green：授权聚合下的 `settleRefund` 争议退款按最新非手续费明细解释，payload 透出 dispute 上下文，evidenceRefs 包含外部争议号和凭证号；目标测试 4 tests、主写投影回归 5 tests 通过。 | 0 | B4-002 仍有普通退款、无授权退款、释放/过期和兼容 chargeback 解释矩阵剩余；下一轮不得把本首轮外推为完整投影解释 Done。 |
| `B4-PROJECTION-007` | 2026-06-18 | 消费 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING`。 | 新增普通退款、无授权退款、授权释放/过期和兼容 chargeback 投影解释 Red/Green：解释层能区分 `REFUNDED`、`NO_AUTH_REFUNDED`、`RELEASED` 和 `COMPAT_CHARGEBACK_REFUNDED`，并透出外部引用、退款原因、chargeback evidence 和外部争议引用；目标投影解释测试 8 tests 通过。 | 0 | B4 remaining 已消费；下一轮不再重复 B4 remaining，除非新 Grant 明确扩失败态解释、projection store、治理重放、历史节点选择查询或运营差异报告。 |
| `LWT-ROLE-LOOP-008` | 2026-06-18 | 重新加载 AI Native、产品架构专家和资深架构师角色协作规约，复核当前 Git、docs、OpenSpec 和 wallet / transaction 代码锚点。 | 当时 HEAD 已推进到 `ca603eab`，活跃基线需从旧 `a38776c5` 回写到 B4 remaining 已提交状态；代码扫描确认已有资金责任解析 facade 和支付工具能力准入 facade，但当时缺少 `AuthorizationAdmissionApplicationService` 或 `authorizeByInstrument` 生产入口。该缺口已由 `WALLET-AUTH-ADMISSION-009` 消费。 | 0 | 已转入下一轮 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 并完成 Green；当前下一候选以 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 为准。 |
| `WALLET-AUTH-ADMISSION-009` | 2026-06-18 | 消费 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`，补齐 wallet 支付工具授权准入最小服务流。 | 新增 `AuthorizationAdmissionApplicationService`、`AuthorizeByPaymentInstrumentRequest`、wallet-impl 最小实现和服务流测试；目标测试 3 tests、wallet application 组合回归 9 tests、授权交易回归 32 tests、`just compile`、`just pmd` 和 `git diff --check` 均通过。准入失败无资金事实；`approved=false` 授权拒绝只生成拒绝交易事实，route legs 为空且无 posting、LedgerEntry 或余额影响。 | 0 | 本 Grant 已消费；下一轮不得沿用它扩 VCC facade、Spend Rule 策略引擎或统一支付工具交易内核。默认下一候选为 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`。 |

## 2. 现状和影响范围

现状：`GSD2-B7-RECON-GATE-CONSUME-002` 已在 `a38776c5` 提交，`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 和 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING` 已推进到 `ca603eab`；本工作树已消费 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`，wallet 授权准入能从支付工具解析到账户主体并委派授权内核。ledger、wallet、transaction 和 reconciliation 已完成多个 service-flow-backed 首轮 Green，但仍存在授权准入 route snapshot 回链、完整预交易快照、账户能力来源组合、ledger guard、清算/结算 gate 消费和 B5 审计扩展等生产可用缺口。

影响范围：本 Goal 只影响文档、状态和任务入口，不影响生产代码、测试代码、DDL/H2 schema、公共契约、运行时配置和 Git 历史。后续任一候选进入编码前，必须重新确认单一 Execution Grant。

## 3. Goal Card

业务目标：把 ledger、wallet、transaction 从“多个切片已 Green”收口为“可作为 VCC、全球账户、清结算对账等上层业务依赖的生产可用资金底座基线”。

用户价值：产品、运营、财务、风控、研发和测试能用同一组账户主体、交易事实、route snapshot、ledger entry、余额投影、交易投影和对账差错证据判断一笔资金动作是否可解释、可核对、可重放、可审计。

成功标准：

1. ledger、wallet、transaction 均有 `Ready / Conditional Ready / Not Done / Blocker` 状态矩阵，且每个 Not Done 能落到下一轮 Grant 候选。
2. 资金主体口径稳定：支付工具、预算组、Spend Rule、交易投影、父账户和外部账户不得被误写为 ledger subject。
3. 任一后续编码切片必须有 Spec/AC/TDD/RED、写入范围、禁止范围、验证命令、停止条件和 Not Done。
4. 当前文档、OpenSpec tasks 和 Git/code baseline 不再停留在旧的 `10853e2d` 或 `bc7ffc0f` 状态。
5. 已完成切片能回链目标测试、compile、pmd、分组测试、`verify-cad` 或 `git diff --check` 证据；文档-only 切片说明未运行编译的原因。

非目标：

1. 本文不新增 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、状态机或运行时配置。
2. 本文不声明 wallet 全量生产 Done、支付工具交易入口全量 Done、VCC facade Done、清算/结算消费方 Done、补事实执行 Done 或 P2 业务 Done。
3. 本文不替代外部规则、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规最终确认。

## 4. 能力地图、对象和流程

能力地图：

| 能力域 | 前台能力 | 后台能力 | 数据能力 |
| --- | --- | --- | --- |
| ledger | 为上层交易和钱包提供账本事实、分录和余额投影护栏。 | 账本 guard、账务平衡、幂等和审计查询。 | ledger transaction、entry、posting plan、balance projection。 |
| wallet | 提供资金账户、信用账户、账户层级、资金责任和支付工具能力准入。 | 钱包 application facade、预交易快照、账户能力来源和 Spend Rule 控制。 | account hierarchy snapshot、funding responsibility decision、payment instrument capability snapshot。 |
| transaction | 提供直接交易、授权交易、余额控制、route snapshot、交易投影解释。 | 原路径回放、投影解释、余额调账审计和失败无副作用治理。 | funds transaction、route snapshot、transaction detail、projection explain。 |
| reconciliation | 提供对账差错、动作守卫、准入 gate 和出款 preflight 消费。 | 清算/结算 gate 消费、差异报告、补事实白名单和运营审批。 | reconciliation difference、gate decision、processing action、rerun evidence。 |

业务对象和对象模型：

- 业务对象：FundingAccount、CreditAccount、AccountHierarchy、PaymentInstrument、FundingResponsibilityDecision、FundsTransaction、RouteSnapshot、LedgerTransaction、LedgerEntry、BalanceProjection、TransactionProjection、ReconciliationDifference、ReconciliationGateDecision。
- 字段口径：资金账户和信用账户是核心入账主体；支付工具、预算组、Spend Rule、交易投影、父账户和外部账户只能作为控制、快照、展示、归集或审计维度。
- 生命周期和状态：钱包侧完成账户与能力解析，交易侧生成资金事实和 route snapshot，ledger 侧生成不可变账本事实和余额投影，reconciliation 侧登记差错、处理动作、重跑结果和准入决策。

业务流程：

1. 主流程：请求进入 wallet application facade，解析账户主体、支付工具能力、资金责任和控制规则，随后委派交易内核生成交易事实、route snapshot、posting plan、LedgerEntry 和投影解释。
2. 异常流程：缺原事实、缺 route snapshot、余额不足、外部余额异常、对账差错未闭环或动作上下文漂移时，必须 fail-fast 或进入差错/审计流程，不生成半截资金事实。
3. 人工兜底：补事实、核销、追偿、生产迁移和专业合规确认必须进入独立审批或白名单 Grant，不由普通交易、钱包或投影接口兜底。

规则矩阵：

| 规则 | 触发条件 | 判断逻辑 | 优先级 | 版本 |
| --- | --- | --- | --- | --- |
| 入账主体 | 交易、余额控制或账本分录生成 | 只能落到资金账户、信用账户或明确授权的控制账户。 | P0 | GSD2-LWT |
| 支付工具能力 | VCC、VA、钱包、电子钱包等入口触发 | 支付工具只做能力入口和快照，必须解析到账户主体后委派交易内核。 | P0 | GSD2-LWT |
| 投影边界 | 查询交易投影或余额投影 | 投影只读派生，不反写交易事实、route 或 ledger fact。 | P0 | GSD2-LWT |
| 对账 gate | 清算、结算或出款准入 | 未闭环或重跑未对平差错阻断，已处理且重跑对平只能条件放行。 | P0 | GSD2-B7 |
| Grant 串行 | 任一候选进入编码 | 同一时间只允许一个单一 Execution Grant active。 | P1 | GSD2 |

运营后台、指标、报表、审计和数据口径：

- 运营后台后续应按资金事实、账本事实、余额投影、交易投影、对账差错、gate 决策和处理动作分别展示，不能混成单一“资金流水”。
- 指标和报表只消费只读投影与对账结果，不反写资金事实。
- 审计必须保留请求流水、route snapshot、ledger transaction、entry、差错单、处理动作和重跑证据。
- 数据口径以当前 PRD、DSL、系分和 TDD 入口为准，本文只记录推进状态和候选顺序。

## 5. 完备性矩阵

### 5.1 Ledger

| 状态 | 能力 | 当前结论 | 下一动作 |
| --- | --- | --- | --- |
| Ready | 账本事实、ledger transaction、posting、entry、余额投影基本链路 | 已作为账户层级、资金责任、交易投影、余额调账、对账差错等切片的验证护栏。 | 后续资金变化切片继续强制断言 ledger entry、posting 平衡、余额桶和幂等。 |
| Conditional Ready | ledger guard 和治理类操作边界 | 现有 guard 能支撑已落地切片，但任一触碰 posting、entry、余额投影或 schema 的后续切片都需要伴随回归。 | `GSD2-LD-LEDGER-GUARD-REGRESSION-001`。 |
| Not Done | 独立 ledger guard 回归包、生产迁移脚本、治理重放、差异报告和清结算补事实链路 | 不影响当前已完成切片解释，但不能据此声明完整账务生产闭环。 | 放入后续单一 Grant，不与 wallet 或 transaction 小切片混写。 |
| Blocker | 把预算组、支付工具、父账户或投影写为 ledger subject | 一旦发现必须立即停止编码，回到 PRD/DSL/系分/TDD 修正。 | 资金主体红线前置评审。 |

### 5.2 Wallet

| 状态 | 能力 | 当前结论 | 下一动作 |
| --- | --- | --- | --- |
| Ready | funding account / credit account、账户层级来源、资金责任目标主体、资金责任解析 facade、支付工具能力准入 facade、授权准入 facade 最小服务流 | 已完成首轮服务流 Green，支付工具仍是能力入口和快照维度，不是账务主体。 | 后续作为 VCC、全球账户和更完整预交易准入的被依赖能力。 |
| Conditional Ready | wallet application facade | 已能完成授权准入最小服务流，但 route snapshot 支付工具引用回链、完整预交易快照、账户能力来源组合和 Spend Rule 控制闭环仍未完成。 | `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 或新的预交易快照 Grant。 |
| Not Done | 钱包账户聚合、账户能力来源组合、授权准入 route snapshot 回链、完整预交易快照、Spend Rule 控制闭环、VCC facade | 仍不能声明 wallet 全量生产 Done。 | 按业务价值拆成 contract-only 或 service-flow-backed 小切片。 |
| Blocker | 让调用方绕过 application facade 自行拼资源服务，或把支付工具能力通过等同于账户资金可用 | 会破坏资金责任、快照和审计一致性。 | 进入 wallet facade 或 admission 专项 Grant。 |

### 5.3 Transaction

| 状态 | 能力 | 当前结论 | 下一动作 |
| --- | --- | --- | --- |
| Ready | 账户主体型直接交易、授权交易、余额控制、route snapshot、原路径回放和基础投影解释 | 已有 B4、B5、B7 多个服务流切片证明交易事实、失败无副作用和只读解释边界。 | 保持 canonical 内核以账户主体为稳定入参。 |
| Conditional Ready | 交易投影解释、余额调账审计和兼容 chargeback 最小 guard | B4-001、B4-002 首轮争议退款、B4-002 remaining、B5-001 和 AUTH 兼容 adapter 已完成最小闭环；查询解释矩阵已覆盖付款、授权拒绝、缺快照、普通退款、无授权退款、争议退款、释放/过期和兼容 chargeback。 | 后续按 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`、失败态解释或运营差异报告专项补强。 |
| Not Done | 授权准入 route snapshot 回链、projection store、治理重放、失败态全量解释、差异报告和完整 dispute case | `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 只保护历史兼容入口的最小审计上下文，不声明完整 dispute / chargeback 生产链路 Done。 | 进入 wallet route snapshot 回链、B5、B7 或 P2 专项前重新确认单一 Grant。 |
| Blocker | 把交易内核整体改成支付工具入参，或让交易投影反写事实 | 会破坏账户主体 canonical 入账和重放稳定性。 | 停止并回到架构裁决。 |

### 5.4 Reconciliation / Clearing / Settlement

| 状态 | 能力 | 当前结论 | 下一动作 |
| --- | --- | --- | --- |
| Ready | 对账差错对象、动作守卫、只读 gate、出款 preflight 消费 | B7-001、B7-002、Gate-001、Gate-002 已证明差错可阻断出款准入。 | 可作为 B7 后续清算/结算准入消费的前置证据。 |
| Conditional Ready | 清算、结算、出款消费方接入 | 出款 preflight 已接入，清算和结算消费方仍未接入。 | `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001`。 |
| Not Done | 差异报告、补事实命令执行服务、运营审批流、追偿、账龄升级、生产迁移脚本 | 不能声明完整 B7 生产 Done。 | 独立拆分 Grant，避免和交易/钱包基础能力混写。 |
| Blocker | 差错处理直接生成资金事实，或绕过白名单、审批和原始事实引用 | 会破坏账务可追溯和职责分离。 | 进入补事实专项设计和审批门禁。 |

### 5.5 生产可用基线判定矩阵

本轮判定口径是“可被上层 MVP 继续依赖的条件基线”，不是“ledger、wallet、transaction 三模块全量生产 Done”。满足条件基线意味着后续 VCC、全球账户、清结算和对账切片可以消费已完成的账户主体、route snapshot、ledger entry、余额投影、交易投影和对账 gate 证据；但任一新资金写入场景仍必须通过单一 Execution Grant、首个 Red、目标测试和回写 Not Done。

| 基线项 | 必备能力 | 当前证据 | 判定 | 下一轮准入要求 |
| --- | --- | --- | --- | --- |
| 入账主体基线 | 资金账户、信用账户和明确授权控制账户是核心 ledger subject；支付工具、预算组、父账户、Spend Rule 和投影只作为控制、快照、归集或审计维度。 | PRD/DSL/系分/TDD 已统一主体口径，B2 账户层级和资金责任目标主体已完成服务流 Green。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 后续任一场景发现非账务主体入账，立即停止并回到设计基线。 |
| 钱包准入基线 | wallet application facade 至少能解析账户层级、资金责任和支付工具能力，并把能力快照交给交易内核。 | 资金责任解析 facade、支付工具能力准入 facade 和授权准入 facade 最小服务流已完成首轮 Green。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 进入 VCC、全球账户或完整预交易准入前，必须补 route snapshot 支付工具引用回链、完整预交易快照或说明为何本切片不需要。 |
| 交易内核基线 | 直接交易、授权交易、余额控制和原路径回放继续以账户主体作为 canonical 入参，支付工具只在外层解析。 | B4、B5、B7 和 wallet 授权准入切片已证明 route snapshot、失败无副作用、只读解释和账户主体委派边界。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 下一步优先补授权准入 route snapshot 回链或 ledger guard，继续保持 canonical 请求不替换为支付工具引用。 |
| 账本护栏基线 | 每笔资金变化必须能断言 posting plan 平衡、LedgerEntry、余额桶、幂等和失败无副作用。 | 已完成切片均以 ledger 事实和余额投影作为验证护栏。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 后续触碰 posting、entry、余额投影或 schema 时，`GSD2-LD-LEDGER-GUARD-REGRESSION-001` 升级为前置或伴随 Red。 |
| 对账准入基线 | 差错登记、动作守卫、只读 gate 和出款 preflight 消费能阻断未闭环差错。 | B7 差错闭环、动作守卫、gate 决策和出款 preflight 消费已完成首轮 Green。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 清算和结算消费方仍需独立 Grant；不得把出款 preflight 证据外推为完整清结算 Done。 |
| 查询解释基线 | 交易投影和余额投影只能解释事实，不反写事实；退款、争议、兼容 chargeback 和授权拒绝需要可区分。 | B4-001 已覆盖 posted pay、declined authorization 和缺 RouteSnapshot fail-fast；B4-002 已覆盖争议退款、普通退款、无授权退款、授权释放/过期和兼容 chargeback，并保持查询只读。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 失败态全量解释、projection store、治理重放、历史节点选择查询或运营差异报告仍需独立 Grant。 |
| 生产发布基线 | 生产迁移、灰度、回滚、监控、告警、运营审批和外部规则确认必须可追溯。 | 当前只完成本地服务流、H2、编译、PMD、分组测试和文档状态回写证据。 | `NOT_DELIVERABLE_AS_FULL_PRODUCTION_DONE`。 | 上线或真实资金前另起发布/迁移/运营准入，不在 LWT Goal 文档-only 范围内声明完成。 |

当前裁决：ledger、wallet、transaction 和 reconciliation 已具备“条件可交付基线”，可继续作为上层 MVP 的被依赖能力进入下一单一 Grant；但不具备“全量生产 Done”裁决。后续任何代码切片都必须说明消费了哪一项基线、补强了哪一项 `PARTIAL_BASELINE` 或 `Not Done`，以及没有扩大到禁止范围。

## 6. 架构决策和契约边界

核心决策和取舍：

1. 交易内核继续以已解析账户主体作为 canonical 入参；支付工具入口放在 wallet/application facade。
2. ledger 只维护账本事实、分录和余额投影，不反持交易生命周期。
3. reconciliation gate 只读消费差错状态和证据，不直接生成资金事实或账本事实。
4. P2 VCC、全球账户和收单必须消费 LWT 被依赖能力，不平行实现资金内核。

接口契约、入参、出参、错误码、幂等和兼容：

- 本文不新增或修改 face 接口、Request、Query、DTO、Spec、错误码、枚举或状态机。
- 后续 Grant 若改入参或出参，必须说明兼容策略、幂等键、失败错误码、route snapshot 回链和旧行为影响。
- `GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001` 已完成 contract/design-only 对齐；`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 已按 `COMPAT_GUARD_NO_BEHAVIOR_BREAK` 消费并完成最小审计 guard，不直接删除或破坏 `FundsAuthorizationTransactionService#chargeback`。

数据方案、事务边界、一致性、补偿和对账：

- 本文不新增表、索引、Entity、Mapper、H2 schema 或生产迁移脚本。
- 后续写入型切片必须证明交易事实、route snapshot、posting plan、LedgerEntry 和余额投影在同一事务边界内一致，失败无半截事实。
- 补偿、对账、调账、核销和追偿必须携带原始事实引用、处理动作、审批或白名单依据。

可靠性、安全、权限、审计和告警：

- 可靠性：后续服务流必须覆盖幂等、重放、乱序、重复请求和外部非终态。
- 安全和权限：运营调账、补事实、核销、追偿、差异报告导出和敏感数据查看必须具备权限与审批边界。
- 审计和告警：资金异常、对账阻断、gate 拒绝、受控负余额和补偿动作必须能形成审计证据，并在后续生产化 Grant 中补告警口径。

发布、灰度、回滚、风险和待确认：

- 本文不进入发布，不声明灰度、回滚和上线 Done。
- 后续生产级 Grant 必须说明灰度对象、开关、回滚方式、监控、告警、残余风险和待确认方。
- 主要风险是把已 Green 小切片误读为模块全量生产 Done，或把历史候选误读为当前 active 编码授权。

## 7. Loop 推进顺序

| Wave | 目标 | 允许动作 | 停止条件 |
| --- | --- | --- | --- |
| Wave 0 | 建立 LWT 生产可用 Goal 状态载体 | 新增本文，同步 README、GSD2、W5 和 OpenSpec tasks。 | 需要写 Java、测试、DDL、公共契约或 Git。 |
| Wave 1 | 交易语义和接口目标态收口 | contract/design-only 对齐 `chargeback`、授权 admission、投影解释范围。 | 需要改 face 接口或生产代码。 |
| Wave 2 | wallet 预交易应用层补强 | 拆分钱包账户聚合、账户能力来源、资金责任、支付工具能力和 Spend Rule 控制。 | 需要引入 P2 VCC facade 或外部规则最终结论。 |
| Wave 3 | transaction 投影和审计补强 | 补 B4-002、B5-002 小切片 Red/Green。 | 触碰 schema、公共契约或调账审批闭环未授权。 |
| Wave 4 | ledger guard 和 B7 消费方补强 | 补 ledger guard regression、清算/结算 gate 消费或差异报告。 | 混入完整清结算出款、补事实执行或生产迁移。 |
| Wave 5 | 生产准入复核 | 汇总测试、compile、pmd、diff、Not Done 和交付证据。 | 验证失败且无法在授权范围内修复。 |

Wave 边界和依赖关系：Wave 0 只做状态和任务基线；Wave 1 只做交易语义和契约目标态收口；Wave 2/3/4 必须在用户确认单一 Grant 后进入 Red/Green；Wave 5 只做验证矩阵、Review 和 handoff，不新增能力。各 Wave 互不重叠，后序 Wave 不得回写前序已消费 Grant 的编码范围。

## 8. 下一候选优先级

下列候选是 backlog reference，不是活跃编码计划。任何一项进入执行前都必须重新确认单一 Execution Grant。

| 优先级 | 候选 | 建议级别 | 理由 | 不做范围 |
| --- | --- | --- | --- | --- |
| 1 | [GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001](GSD-2-AUTH-Chargeback目标语义对齐任务卡.md) | `contract/design-only` | 已补任务卡：目标态主入口为 `settleRefund` 争议字段；现有 `chargeback` 仅作为历史兼容、显式事件或内部适配资产。 | 已完成 docs-only 对齐，不进入代码。 |
| 2 | [GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001](GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md) | `consumed-green` | 已按 `COMPAT_GUARD_NO_BEHAVIOR_BREAK` 完成兼容说明、最小审计 guard、兼容测试和状态回写。 | 不继续沿用本 Grant 扩完整 dispute case、DDL/H2 schema、事件语义迁移或公共 API 删除。 |
| 3 | `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING` | `consumed-green` | 已覆盖普通退款、无授权退款、释放/过期和兼容 chargeback 的解释矩阵。 | 不继续沿用本 Grant 扩 projection store、治理重放、历史节点选择查询或运营差异报告。 |
| 4 | `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` / `AuthorizationAdmissionApplicationService` | `consumed-green` | 已完成工具准入、绑定校验、资金责任、账户能力和账户主体型授权内核委派的最小服务流。 | 不继续沿用本 Grant 扩 VCC facade、Spend Rule 策略引擎、完整预交易快照或统一支付工具交易内核。 |
| 5 | `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` | `service-flow-backed` | 授权准入已能委派内核，下一最小缺口是把支付工具引用、绑定版本和准入决策固化到 route snapshot / 交易明细可审计链路。 | 不替换交易 canonical 入参，不新增 VCC 生命周期，不新增统一支付工具交易内核。 |
| 6 | `GSD2-LD-LEDGER-GUARD-REGRESSION-001` | `guard-regression` | 后续任一资金变化切片都需要 ledger guard 伴随证明。 | 不重启 GSD1 大包。 |
| 7 | `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` | `service-flow-backed` | 出款 preflight 已消费 gate，清算/结算消费方仍未接入。 | 不一次性做完整清分、清算、结算、出款和补事实。 |
| 8 | `GSD2-B5-BALANCE-ADJUST-AUDIT-002` | `service-flow-backed` | B5-001 已有入参审计和交易上下文，后续可补独立审计查询、route snapshot 回链或审批闭环。 | 不开放泛化运营补账。 |
| 9 | B7 差异报告 / Spend Rule / P2 VCC / 全球账户 | `contract-only` 起步 | 业务价值高，但必须消费账户、钱包 facade、交易投影、ledger guard 和对账差错证据。 | 不直接写 P2 生产代码、外部轨道或通道规则。 |

### 8.1 单一 Grant 决策账本

本账本用于把 `Goal Active`、`Plan Grant docs-only` 和 `Execution Grant` 的边界拆开。它只决定下一轮如何从状态载体进入单一候选，不替代用户授权、架构师 CAD 门禁、目标测试或 Git 授权。

| 决策项 | 当前裁决 | 进入条件 | 切换条件 | 状态回写 |
| --- | --- | --- | --- | --- |
| 默认推荐 | `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`，首个子切片收敛为授权准入后的支付工具引用、绑定版本和准入决策回链。 | 用户重新确认该 Execution Grant；资深架构师先写目标测试，证明通过支付工具授权准入后，route snapshot 或交易明细能审计 payment instrument、binding version 和 admission decision，且仍委派账户主体型授权内核。 | 若用户明确不继续 wallet，改选 ledger guard、B7 清算/结算 gate、B7 差异报告或 B5-002。 | 本文第 8 节、第 10 节、第 11 节、W5 推进计划、B2B4 准入卡和 OpenSpec tasks。 |
| Wallet 授权准入 | 目标是补齐支付工具授权入口的最小服务流。 | 已按 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 完成 application 契约、Request、实现和服务流测试。 | 后续需要 route snapshot 回链、Spend Rule、VCC facade、完整预交易快照或交易 canonical 入参调整时，必须重新开 Grant。 | 本文第 1.2 节、第 5.2 节、第 8 节和 Evidence Anchor Matrix。 |
| AUTH 兼容 adapter | 目标是防止历史 `chargeback` 被误当新目标主入口。 | 已按 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 完成兼容说明、最小 guard、兼容测试和状态回写。 | 后续需要删除公共 API、改 DDL/H2 schema、完整 dispute case 或迁移到 `settleRefund` 委派时，必须重新开 Grant。 | `GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` 第 14 节已记录 Red/Green、验证证据和 Not Done。 |
| B4 投影解释扩展 | 目标是补齐普通退款、无授权退款、释放/过期和兼容 chargeback 的解释矩阵。 | `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING` 已消费，写入范围限 projection explain 只读查询和目标测试。 | 需要 projection store、治理重放、历史节点选择查询、反写事实、失败态全量解释、运营差异报告或 DDL 时停止并重新开 Grant。 | 已回写第 5.5 查询解释基线为 `CONDITIONAL_DELIVERABLE_BASELINE`。 |
| Wallet facade 补强 | 目标是补完整预交易快照、账户能力来源组合或授权 route snapshot 回链。 | 用户确认 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 或等价 wallet 子切片。 | 需要 VCC facade、外部规则最终结论或替换交易 canonical 入参时停止。 | 回写第 5.2 Wallet 矩阵和 Evidence Anchor Matrix。 |
| Ledger guard 回归 | 目标是证明后续资金变化仍满足 posting、entry、余额投影和幂等红线。 | 触碰 posting、LedgerEntry、余额投影、账目 schema 或资金变化高风险切片时前置或伴随。 | 发现非账务主体入账、余额桶不平或失败有副作用时停止。 | 回写第 5.1 Ledger 矩阵和第 11 Completion Audit。 |
| B7 清算/结算 gate 消费 | 目标是把对账 gate 从出款 preflight 扩展到清算/结算消费方。 | 用户确认 `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001`。 | 需要完整清分、清算、结算、补事实执行、运营审批或生产迁移时停止。 | 回写第 5.4 Reconciliation 矩阵和 W5 Grant 队列。 |
| B5 审计扩展 | 目标是补余额调账独立审计查询、route snapshot 回链或审批闭环的最小切片。 | 用户确认 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`，并明确是否触碰 schema。 | 需要泛化运营补账、绕过对账差错或真实资金纠偏审批时停止。 | 回写第 5.3 Transaction 矩阵和 B5 Not Done。 |

执行规则：

1. 同一时间只能有一个 `Execution Grant` active；Goal active 只允许维护状态、证据和候选顺序。
2. 默认推荐不是自动编码授权；用户未复制确认前，只能做低风险文档同步、只读 Gap Audit、Spec/AC/Harness/CAD 任务卡和验证矩阵。
3. 任一候选完成后必须回写本账本的当前裁决、被消费证据、Not Done、验证命令和下一 owner。
4. 连续两轮没有新增证据、状态变化或缺口收敛时，停止 Loop 并把原因写入 Completion Audit。

## 9. 验证矩阵

| 验证层 | 命令或方式 | 完成条件 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` | Task、Owner、范围、Wave、上下文账本、禁止事项、验证和 handoff 字段齐全。 |
| 产品结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` | 背景目标、现状、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-LWT|LWT-PRODUCTION|CONDITIONAL_DELIVERABLE_BASELINE|PARTIAL_BASELINE|Completion Audit|Evidence Anchor Matrix|单一 Grant 决策账本|Loop Progress Ledger|无进展计数|FundingResponsibilityResolutionApplicationServiceTests|AuthorizationAdmissionApplicationServiceTests|WALLET_AUTHORIZATION_ADMISSION_GREEN_VERIFIED|GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001|ca603eab" docs openspec` | GSD2 入口、W5、生产可用基线裁决、完成度审计、证据锚点、单一 Grant 决策账本、Loop Progress Ledger、README、OpenSpec tasks、wallet 授权准入已消费和下一 route snapshot 回链候选能追踪到本文和当前提交。 |
| Grant 消费可用性 | `rg -n 'Grant 消费预检清单|Grant 消费运行卡|Red 选择|最小断言清单|AUTH-CB-COMPAT-RED-001' docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md openspec/changes/tdd-baseline-reset/tasks.md` | AUTH 兼容确认包不仅有可复制 Grant，还能指导确认后的预检、首个 Red 选择、最小 Green、Review、Verify 和 Handoff。 |
| README 恢复导航 | `rg -n 'W1 基线差距审计把当前 Git/code baseline 校准到|推荐下一步确认 .*GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001|当前仍不授权代码|当前下一候选.*B2|下一候选.*GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001' docs/README.md docs/TDD设计/README.md docs/TDD设计/GSD-2-新基线工作流规划.md openspec/changes/tdd-baseline-reset/tasks.md` | 不再把 W1/W2/W3/W4 的历史基线、历史推荐或 B2 账户层级候选误写成当前下一候选；README 入口应指向本轮支付工具授权准入提交、LWT Goal、B4 remaining 已消费和 wallet 授权准入下一切片。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译和测试 | wallet 授权准入涉及 wallet-face application 契约、wallet-impl 服务实现、目标服务流测试和文档状态回写，已运行目标测试、组合回归、授权交易回归、`just compile`、`just pmd` 和 `git diff --check`。 | Java、测试和公共契约变更均有本地验证证据。 |

## 10. 当前执行交接和 handoff

| 字段 | 内容 |
| --- | --- |
| 当前可执行任务 | `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 已完成 Red / Green / Verify 和状态回写；下一轮默认等待重新确认 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`，或改选 ledger guard、B7 清算/结算 gate、B7 差异报告、B5 审计扩展。 |
| 写入范围 | `wallet-face` 授权准入 application 契约和 Request、`wallet-impl` 授权准入实现、目标服务流测试、本文、GSD-2 入口、P0/P1 LWT 推进计划、TDD README、docs README 和 OpenSpec tasks 状态同步。 |
| 验证命令 | 本轮已执行 `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one AuthorizationAdmissionApplicationServiceTests tests` 3 tests 通过、`just test-one AuthorizationAdmissionApplicationServiceTests,PaymentInstrumentCapabilityApplicationServiceTests,FundingResponsibilityResolutionApplicationServiceTests tests` 9 tests 通过、`just test-one FundsAuthorizationTransactionFlowTests tests` 32 tests 通过、`just compile` 通过、`just pmd` 通过和 `git diff --check` 通过。 |
| 编译说明 | 本轮包含 Java、测试和公共契约逻辑变更，已运行 `just compile`。 |
| 下一 owner | 用户确认是否进入 wallet 授权 route snapshot 回链、ledger guard、B7 清算/结算 gate 消费、B7 差异报告或 B5 审计扩展；若继续 B4，必须是新的失败态解释、projection store、治理重放、历史节点选择查询或运营差异报告专项 Grant。 |
| 交接要求 | 后续每轮必须先读取本文、GSD-2 工作流、W5 推进计划、AUTH 兼容确认包第 11 节预检清单、第 12 节运行卡和 OpenSpec tasks，再确认单一 Execution Grant、写入范围、禁止范围、验证命令和回滚提示。 |
| 回滚提示 | 本轮为独立 wallet 授权准入切片，回滚时还原 `AuthorizationAdmissionApplicationService`、`AuthorizeByPaymentInstrumentRequest`、wallet-impl 授权准入实现、目标服务流测试和相关文档状态 diff；不得连带回滚无关历史提交或用户未归属变更。 |

## 11. Completion Audit 和三卡交接

本节用于防止把“已有文档和局部 Green”误判成 Goal 已完成。审计结论以当前工作树、Git/code baseline、OpenSpec tasks、目标测试记录和本地结构门禁为证据；没有直接证据的项目不能标为 Done。

### 11.1 Completion Audit

| Goal 要求 | 当前证据 | 判定 | 下一动作 |
| --- | --- | --- | --- |
| 建立 Loop + Goal 状态载体 | 本文已建立 Goal ID、Loop ID、状态、Owner、写入范围、只读范围、Git 策略、上下文账本、停止条件和验证矩阵。 | `DONE_DOCS_ONLY`。 | 后续每轮先读本文并回写状态。 |
| 建立 ledger / wallet / transaction 完备性矩阵 | 第 5.1 至 5.3 已按 Ready / Conditional Ready / Not Done / Blocker 拆分；第 5.4 将 reconciliation 作为 B7 gate 被依赖能力纳入。 | `DONE_DOCS_ONLY`。 | 若后续切片改变状态，必须同步矩阵。 |
| 给出可交付生产基线判定 | 第 5.5 已明确 `CONDITIONAL_DELIVERABLE_BASELINE`、`PARTIAL_BASELINE` 和 `NOT_DELIVERABLE_AS_FULL_PRODUCTION_DONE`。 | `DONE_DOCS_ONLY`。 | 后续切片必须声明消费或补强哪一项基线。 |
| 给出下一单一 Grant 决策账本 | 第 8.1 已拆分默认推荐、候选切换条件、停止条件和状态回写位置。 | `DONE_DOCS_ONLY`。 | 用户确认单一 Grant 后，按对应候选首个 Red 进入代码闭环。 |
| 结构门禁命令可复跑且通过 | 第 11.4 已记录 LWT Goal、P0/P1 LWT 推进计划、AUTH Chargeback 目标语义任务卡和 AUTH Chargeback 兼容入口确认包的 Harness、产品和架构结构 checker 结果。 | `DONE_DOCS_ONLY`。 | 后续改动任一活跃入口时必须复跑对应结构门禁。 |
| 对齐当前 Git/code baseline | 本文、W5、GSD-2 工作流和 OpenSpec tasks 均校准到本轮支付工具授权准入提交，并保留 `ca603eab`、`873e5f8c`、`a38776c5`、`bc7ffc0f`、`10853e2d`、`ae8cb8a6`、`e81a8a25` 作为已消费证据。 | `DONE_DOCS_ONLY`。 | 若发生新提交，先回写 baseline 再继续。 |
| 校准 README 恢复导航 | docs README 和 TDD README 已把 W1/W2/W3/W4 标为历史记录或已消费证据，当前入口指向 LWT Goal、本轮支付工具授权准入提交、AUTH 兼容 adapter Green 结果、B4 投影解释 remaining 已消费结果、`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 已消费结果和下一默认候选 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`；README 旧口径扫描无残留。 | `DONE_DOCS_ONLY`。 | 后续改 README 或入口导航时必须复跑 README 恢复导航扫描。 |
| 按依赖顺序推进低风险文档/计划 | AUTH 兼容和 B4 投影解释 remaining 已消费；下一候选已按 wallet 授权准入、ledger guard、B7 清算/结算 gate、B7 差异报告、B5 审计扩展排序。 | `DONE_DOCS_ONLY`。 | 未确认 Grant 前只允许状态和任务卡维护。 |
| 建立无进展计数和停止账本 | 第 1.2 节已记录 docs-only 推进轮次、证据变化和无进展计数；当前计数为 1。 | `DONE_DOCS_ONLY`。 | 下一轮若没有 Grant、事实差异、验证证据或状态缺口收敛，应停止 docs-only 扩写并交还用户选择。 |
| 单一 Execution Grant 后进入代码、测试、验证、提交闭环 | `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 已消费：补兼容入口最小审计 guard、契约说明和失败无副作用 Red；目标测试、交易分组、compile 和 PMD 通过。 | `GREEN_VERIFIED_SUMMARY_ONLY`。 | 本 Grant 已消费，下一轮必须重新确认新的单一 Grant。 |
| 补齐 AUTH 兼容 Grant 消费预检 | `GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` 第 11 节已补用户授权、工作树状态、当前基线、首个 Red 收窄、写入范围、验证顺序、状态回写和 Git 策略预检，并已被本轮消费。 | `CONSUMED_GREEN`。 | 预检结果只作为本 Grant 审计证据，后续不得复用为新授权。 |
| 补齐 AUTH 兼容 Grant 消费运行卡 | `GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` 第 12 节已补 Red 选择、Red 范围、Green 实现、Review、Verify、Handoff 和最小断言清单，并已被本轮消费。 | `CONSUMED_GREEN`。 | 运行卡结果只作为本 Grant 审计证据，后续不得复用为新授权。 |
| 证明三模块全量生产可用 | 当前只具备条件可交付基线，仍缺授权准入 route snapshot 回链、完整预交易快照、失败态全量解释、projection store、治理重放、ledger guard 回归、清算/结算消费、差异报告、补事实、运营审批、生产迁移、灰度和告警。 | `NOT_DONE`。 | 不得标记 Goal complete；继续按单一 Grant 收敛缺口。 |

审计裁决：当前 Goal 的状态载体、完备性矩阵、条件基线判定、AUTH 兼容 Grant 确认包、B4 投影解释矩阵和 wallet 授权准入最小服务流已完成收口；三模块仍未被证明为全量生产 Done，且下一轮缺口必须重新确认单一 Grant。因此本线程 Goal 继续保持 active。

### 11.2 三卡交接

| 交接卡 | 已具备内容 | 缺口 | 下一 owner |
| --- | --- | --- | --- |
| Product Context Card | 业务目标、用户价值、核心对象、入账主体规则、支付工具边界、投影边界、对账 gate、条件基线和 Not Done 已在第 3 至 5 节表达。 | VCC、全球账户、收单、完整清结算和运营后台验收仍需独立场景卡。 | 产品架构专家。 |
| Engineering Handoff Card | `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING` 和 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 已消费并通过目标测试；写入范围、禁止事项、验证命令、Not Done 和下一候选已回写。 | 下一轮仍缺新的单一 Execution Grant；默认建议确认 wallet 授权 route snapshot 回链子切片，本 Grant 不得复用。 | 资深架构师 + 用户。 |
| Production Loop Card | 已声明生产发布基线未完成，并列出迁移、灰度、回滚、监控、告警、运营审批、外部规则和真实资金前置条件。 | 生产发布、SLO、告警、Runbook、合规/法务/财务/通道确认均未授权。 | 发布 owner / 业务 owner / 合规和财务确认方。 |

三卡裁决：当前三卡可用于下一轮编码准入交接，但不能替代新的 Execution Grant、测试通过、Git 授权或上线审批。`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING` 和 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 已消费，后续不得复用这些 Grant 继续扩完整 dispute case、projection store、治理重放、VCC facade、Spend Rule 策略引擎或统一支付工具交易内核；若继续推进，默认重新确认 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`，或改选 ledger guard、B7 清算/结算 gate、B7 差异报告、B5 审计扩展等其他单一 Grant。

### 11.3 Evidence Anchor Matrix

本矩阵只记录当前仓库中的事实锚点，方便后续单一 Grant 消费条件基线时快速定位代码、测试和验证命令。锚点存在不等于对应能力全量 Done；只有目标测试、分组回归、编译、PMD 和本节状态回写共同成立，才能作为该切片的 Green 证据。

| 条件基线 | face / contract 锚点 | impl 锚点 | 测试锚点 | 已记录验证命令 | 证据强度 |
| --- | --- | --- | --- | --- | --- |
| 账户层级和资金责任目标主体 | `wallet/wallet-face` 资金责任 Request/DTO/Query；`targetSubjectType + targetSubjectId` 口径。 | `wallet/wallet-impl` 资金责任关系服务和 H2 `t_spend_subject_funding_rel`。 | `tests/src/test/java/com/wind/funds/wallet/services/impl/SpendSubjectFundingRelationServiceImplTests.java`。 | `just test-one SpendSubjectFundingRelationServiceImplTests tests`。 | `SERVICE_FLOW_GREEN_RECORDED`。 |
| 资金责任解析 facade | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/funding/FundingResponsibilityResolutionApplicationService.java`。 | `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application/funding/impl/FundingResponsibilityResolutionApplicationServiceImpl.java`。 | `tests/src/test/java/com/wind/funds/wallet/application/funding/FundingResponsibilityResolutionApplicationServiceTests.java`。 | `just test-one FundingResponsibilityResolutionApplicationServiceTests tests`。 | `SERVICE_FLOW_GREEN_RECORDED`。 |
| 支付工具能力准入 facade | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/instrument/PaymentInstrumentCapabilityApplicationService.java`。 | `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application/instrument/impl/PaymentInstrumentCapabilityApplicationServiceImpl.java`。 | `tests/src/test/java/com/wind/funds/wallet/application/instrument/PaymentInstrumentCapabilityApplicationServiceTests.java`。 | `just test-one PaymentInstrumentCapabilityApplicationServiceTests tests`。 | `SERVICE_FLOW_GREEN_RECORDED`。 |
| 交易投影解释首轮 | `transaction/transaction-face/src/main/java/com/wind/funds/transaction/projection/FundsTransactionProjectionExplainApplicationService.java`。 | `transaction/transaction-impl/src/main/java/com/wind/funds/transaction/projection/impl/DefaultFundsTransactionProjectionExplainApplicationService.java`。 | `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsTransactionProjectionExplainApplicationServiceTests.java`、`tests/src/test/java/com/wind/funds/transaction/application/flow/DefaultRoutedFundsInstructionOrchestratorProjectionTests.java`。 | `just test-one FundsTransactionProjectionExplainApplicationServiceTests tests`、`just test-one DefaultRoutedFundsInstructionOrchestratorProjectionTests tests`、`just compile`、`just pmd`。 | `PARTIAL_BASELINE_GREEN_RECORDED`。 |
| 余额调账审计首轮 | `transaction-face` balance adjust 请求审计字段和外部异常来源类型。 | `transaction-impl` instruction converter、command service 和交易明细上下文透传。 | `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsBalanceAdjustAuditFlowTests.java`、`tests/src/test/java/com/wind/funds/transaction/application/flow/FundsBalanceControlFailureFlowTests.java`。 | `just test-one FundsBalanceAdjustAuditFlowTests tests`、`just test-balance-control`、`just test-reconciliation`、`just compile`、`just pmd`。 | `SERVICE_FLOW_GREEN_RECORDED`。 |
| 对账差错和动作守卫 | `reconciliation-face` 差错 application 契约、差错状态、动作类型和处理回链请求。 | `reconciliation-impl` 差错 Entity、Mapper、服务和 H2 `t_reconciliation_difference`。 | `tests/src/test/java/com/wind/funds/reconciliation/application/difference/impl/ReconciliationDifferenceApplicationServiceTests.java`。 | `just test-one ReconciliationDifferenceApplicationServiceTests tests`、`just test-reconciliation`、`just verify-fast`、`just pmd`。 | `SERVICE_FLOW_GREEN_RECORDED`。 |
| 对账 gate 和出款 preflight 消费 | `reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/application/gate/ReconciliationGateApplicationService.java`。 | `reconciliation/reconciliation-impl/src/main/java/com/wind/funds/reconciliation/application/gate/impl/ReconciliationGateApplicationServiceImpl.java`、`reconciliation/reconciliation-impl/src/main/java/com/wind/funds/reconciliation/services/impl/PayoutOrderServiceImpl.java`。 | `tests/src/test/java/com/wind/funds/reconciliation/application/gate/impl/ReconciliationGateApplicationServiceTests.java`、`tests/src/test/java/com/wind/funds/reconciliation/services/impl/PayoutPreflightServiceTests.java`。 | `just test-one ReconciliationGateApplicationServiceTests tests`、`just test-one PayoutPreflightServiceTests tests`、`just test-reconciliation`、`just compile`、`just pmd`。 | `SERVICE_FLOW_GREEN_RECORDED`。 |
| 支付工具授权准入 facade | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/instrument/AuthorizationAdmissionApplicationService.java`、`wallet/wallet-face/src/main/java/com/wind/funds/wallet/model/request/AuthorizeByPaymentInstrumentRequest.java`。 | `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application/instrument/impl/AuthorizationAdmissionApplicationServiceImpl.java`。 | `tests/src/test/java/com/wind/funds/wallet/application/instrument/AuthorizationAdmissionApplicationServiceTests.java`。 | `just test-one AuthorizationAdmissionApplicationServiceTests tests`、`just test-one AuthorizationAdmissionApplicationServiceTests,PaymentInstrumentCapabilityApplicationServiceTests,FundingResponsibilityResolutionApplicationServiceTests tests`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just compile`、`just pmd`。 | `SERVICE_FLOW_GREEN_RECORDED_NEEDS_ROUTE_SNAPSHOT_BACKLINK`。 |
| Ledger 护栏 | `ledger-face` 账本、账本交易、分录和余额投影契约。 | `ledger-impl` 账本服务、交易服务、余额投影服务；transaction posting assembler。 | `tests/src/test/java/com/wind/funds/ledger/impl/LedgerServiceImplTests.java`、`LedgerTransactionServiceImplTests.java`、`LedgerBalanceProjectionServiceImplTests.java`、`tests/src/test/java/com/wind/funds/transaction/ledger/DefaultLedgerPostingAssemblerTests.java`。 | `just test-ledger`、`just test-one DefaultLedgerPostingAssemblerTests tests`、`just compile`、`just pmd`。 | `GUARD_BASELINE_RECORDED_NEEDS_NEXT_GRANT_IF_TOUCHED`。 |

证据消费规则：后续任一编码切片不得只引用本矩阵作为 Done 证据；必须先复跑与本切片相关的目标测试和必要分组测试，再结合 `git diff --check`、`compile`、`pmd` 和 Not Done 回写完成交付闭环。

### 11.4 结构门禁验证证据账本

本账本只证明活跃设计入口具备可消费的结构字段和交接信息，不证明 Java、测试、DDL/H2 schema、公共契约、运行时配置、Git 或生产发布已经获得授权或通过准出。

| 文档 | Harness 结构 | 产品结构 | 架构结构 | 结论 |
| --- | --- | --- | --- | --- |
| `docs/TDD设计/GSD-2-新基线工作流规划.md` | `check_harness_plan.py --kind gsd-wave` 通过。 | `check_product_deliverable.py --kind product-architecture` 通过。 | `check_architecture_deliverable.py --kind architecture-plan` 通过。 | GSD-2 总恢复入口可消费。 |
| `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` | `check_harness_plan.py --kind gsd-wave` 通过。 | `check_product_deliverable.py --kind product-architecture` 通过。 | `check_architecture_deliverable.py --kind architecture-plan` 通过。 | Goal 状态载体可消费。 |
| `docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` | `check_harness_plan.py --kind gsd-wave` 通过。 | `check_product_deliverable.py --kind product-architecture` 通过。 | `check_architecture_deliverable.py --kind architecture-plan` 通过。 | W5 推进计划可消费。 |
| `docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md` | `check_harness_plan.py --kind gsd-wave` 通过。 | `check_product_deliverable.py --kind product-architecture` 通过。 | `check_architecture_deliverable.py --kind architecture-plan` 通过。 | contract/design-only 任务卡可消费。 |
| `docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` | `check_harness_plan.py --kind cad-candidate` 和 `--kind gsd-wave` 通过。 | `check_product_deliverable.py --kind product-architecture` 通过。 | `check_architecture_deliverable.py --kind architecture-plan` 通过。 | 确认包已被用户授权并完成 Green 验证，保留为本 Grant 审计和回放依据。 |

验证边界：本轮 AUTH 兼容切片已运行目标测试、交易分组、`just compile`、`just pmd`、`rg` 一致性检索和 `git diff --check`；未运行 `verify-cad`，原因是本 Grant 范围只触碰授权兼容入口的最小 guard、契约说明、目标测试和状态回写。

## 12. 停止条件

1. 需要写 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、状态机或运行时配置。
2. 需要 Git add、commit、push、PR、merge、rebase、reset 或分支切换。
3. 发现当前文档与 Git HEAD、OpenSpec tasks 或已完成验证证据冲突，且无法用状态回写解释。
4. 发现支付工具、预算组、Spend Rule、父账户或投影被设计为 ledger subject。
5. 需要联网、依赖安装、生产配置、真实资金、外部规则、税务、会计、法务或合规最终确认。
6. 连续两轮没有新增证据、状态变化或缺口收敛；判定以第 1.2 节 `Loop Progress Ledger` 的无进展计数为准。
