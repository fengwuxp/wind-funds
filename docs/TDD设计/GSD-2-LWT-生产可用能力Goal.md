# GSD-2 LWT 生产可用能力 Goal

## 1. 文档定位

本文是 ledger、wallet、transaction 三条资金底座被依赖能力的 `Loop + Goal` 状态载体，用于把已完成的 GSD2 切片、仍未 Done 的生产能力缺口和下一轮单一 Execution Grant 候选聚合到一张可持续推进的 Goal 卡中。

本文不是新的 PRD，不替代产品设计、DSL 设计、系分设计或 TDD 设计；也不单独替代 DDL/H2 schema 授权、公共契约变更授权、生产发布授权或外部规则专业确认。`GSD2-B5-BALANCE-ADJUST-AUDIT-002` 已在 `da3b4f19` 提交固化为能力证据；`GSD2-B5-BALANCE-ADJUST-AUDIT-003` 已在 `4ef64275` 提交固化余额调账独立审计查询 Red / Green / Verify 结果；`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 与 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 已在 `0d3f68dc` 提交固化对象级 Gate 和清算 / 结算只读 consumer 服务。`GSD2-B7-RECON-DIFFERENCE-REPORT-001` 已在 `a1397ddf` 提交固化对账差异报告最小只读查询能力。当前不再复用 B5-003、B7 对象级阻断、B7 consumer 或 B7 差异报告授权扩范围，下一轮需重新确认单一 Grant。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` |
| 原子任务 | 建立 LWT 生产可用能力 Goal，聚合当前能力完备性、下一候选优先级、验证矩阵和 handoff。 |
| 所属阶段 | GSD-2 / LWT capability goal / B7 difference report verified。 |
| Goal ID | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| Loop ID | `GSD2-LWT-PRODUCTION-CAPABILITY-LOOP-2026-06-18` |
| 当前状态 | `B7_RECON_DIFFERENCE_REPORT_GREEN_VERIFIED_COMMITTED` |
| Git / code baseline | `a1397ddf feat: 补齐对账差异报告只读查询` 是当前已提交基线；B7 对象级 Gate、清算 / 结算 consumer 和 B7 对账差异报告均已作为能力证据固化。`4ef64275 feat: 补齐余额调账独立审计查询`、`da3b4f19 feat: 补齐余额调账路由审计回链`、`0b251593 feat: 补齐账本正常余额方向护栏`、`dd442888`、`ea8f8800`、`632bd2f6`、`ca603eab`、`873e5f8c`、`a38776c5`、`bc7ffc0f`、`10853e2d`、`ae8cb8a6` 和 `e81a8a25` 保留为已消费能力证据。 |
| 关联入口 | [GSD-2-新基线工作流规划.md](GSD-2-新基线工作流规划.md)、[GSD-2-P0P1-LedgerWalletTransaction推进计划.md](GSD-2-P0P1-LedgerWalletTransaction推进计划.md)、[GSD-2-B7-对账差异报告ExecutionGrant确认包.md](GSD-2-B7-对账差异报告ExecutionGrant确认包.md)、[GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md](GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md)、[GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md](GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md)、[GSD-2-AUTH-Chargeback目标语义对齐任务卡.md](GSD-2-AUTH-Chargeback目标语义对齐任务卡.md)、[GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md](GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md)、OpenSpec `tdd-baseline-reset` tasks。 |
| Owner | AI Native 流程编排负责 Goal、Loop、状态和停止条件；产品架构专家负责业务价值、验收和 Not Done；资深架构师负责模块边界、契约、测试、验证和编码准入；用户确认单一 Grant。 |
| 写入范围 | 本文、B7 对账差异报告确认包、GSD-2 入口、P0/P1 LWT 推进计划、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-B7-对账差异报告ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、ledger、wallet、transaction、core、reconciliation、tests、Justfile、AGENTS.md、最近 Git 提交和历史准入卡。 |
| 只读参考 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、源码、测试和 Git 提交记录。 |
| 上下文账本 | 本文是 LWT 当前状态账本；GSD-2 工作流、W5 推进计划、TDD README、docs README 和 OpenSpec tasks 是恢复入口。 |
| Git 策略 | B5-003 已提交到 `4ef64275`；B7 对象级阻断和清算 / 结算 consumer 已提交到 `0d3f68dc`。B7 差异报告已提交到 `a1397ddf`；后续若继续新 Grant，需重新确认写入范围和 Git 策略。 |

### 1.1 Goal 层级和当前恢复入口

本轮存在父 Goal 和 LWT 子 Goal 两层状态，恢复时按下表理解，避免把历史收口状态误当当前授权。

| 层级 | ID / 状态 | 含义 | 当前动作 |
| --- | --- | --- | --- |
| 父 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` | GSD-2 总基线，负责清理旧活跃计划、维护 Wave、OpenSpec tasks、Plan Grant 和恢复入口。 | 继续作为总入口和历史证据账本，不直接授权编码。 |
| LWT 子 Goal | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` / `B7_RECON_DIFFERENCE_REPORT_GREEN_VERIFIED_COMMITTED` | 聚合 ledger、wallet、transaction 和 reconciliation 的条件生产基线、完备性矩阵、三卡交接和下一 Grant 队列。 | 继续作为当前 LWT 状态载体，记录 wallet 授权准入、授权 route snapshot 回链、ledger normal balance guard、AUTH 兼容 guard、B4 投影解释矩阵、B5 route snapshot 审计回链、B5 独立审计查询、B7 对象级 gate 基座、B7 清算 / 结算 consumer 已提交，以及 B7 差异报告 Green / Verify 已提交。 |
| 当前可执行状态 | `B7_RECON_DIFFERENCE_REPORT_GREEN_VERIFIED_COMMITTED` | 对账差错已能登记对象级阻断字段，gate 查询可按对象精确命中并兼容历史类型级差错；清算 / 结算 consumer 已能只读消费对象级 gate；B7 差异报告已补单笔只读报告查询能力。 | 当前 Grant 已消费并提交；下一步需要重新选择 B7 报告扩展、wallet 完整预交易快照、账户能力来源组合、Spend Rule 控制闭环等新的单一 Grant。 |
| 历史证据状态 | `B7_RECON_DIFFERENCE_ACTION_GUARD_GREEN_VERIFIED`、`GSD2-B7-RECON-GATE-CONSUME-001/002`、`10853e2d` 等 | 只表示过去切片已完成或已消费，不能作为当前下一步或默认授权。 | 仅作为 Evidence Anchor 和 Not Done 边界来源。 |

恢复规则：下一轮必须先读取本文、GSD-2 工作流、W5 推进计划、B5-003 余额调账审计扩展确认包和 OpenSpec tasks；若运行时 Goal objective、旧对话摘要或历史任务卡仍指向 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 或 `GSD2-B5-BALANCE-ADJUST-AUDIT-003`，以当前工作树、本文和 OpenSpec tasks 为准，将这些切片视为已消费证据，不得回退重做或沿用为新授权。若用户没有确认新的单一 Grant，只能继续低风险状态维护或只读 Gap Audit。若用户改选 B7 清算/结算 gate、B7 差异报告、wallet 预交易快照或 P2 业务能力，必须先同步本映射和第 8.1 节 Grant 决策账本。

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
| `LWT-ROLE-LOOP-008` | 2026-06-18 | 重新加载 AI Native、产品架构专家和资深架构师角色协作规约，复核当前 Git、docs、OpenSpec 和 wallet / transaction 代码锚点。 | 当时 HEAD 已推进到 `ca603eab`，活跃基线需从旧 `a38776c5` 回写到 B4 remaining 已提交状态；代码扫描确认已有资金责任解析 facade 和支付工具能力准入 facade，但当时缺少 `AuthorizationAdmissionApplicationService` 或 `authorizeByInstrument` 生产入口。该缺口已由 `WALLET-AUTH-ADMISSION-009` 消费。 | 0 | 已转入下一轮 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 并完成 Green；后续 ledger guard 候选已由 `LD-LEDGER-GUARD-012` 消费。 |
| `WALLET-AUTH-ADMISSION-009` | 2026-06-18 | 消费 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`，补齐 wallet 支付工具授权准入最小服务流。 | 新增 `AuthorizationAdmissionApplicationService`、`AuthorizeByPaymentInstrumentRequest`、wallet-impl 最小实现和服务流测试；目标测试 3 tests、wallet application 组合回归 9 tests、授权交易回归 32 tests、`just compile`、`just pmd` 和 `git diff --check` 均通过。准入失败无资金事实；`approved=false` 授权拒绝只生成拒绝交易事实，route legs 为空且无 posting、LedgerEntry 或余额影响。 | 0 | 本 Grant 已消费；下一轮不得沿用它扩 VCC facade、Spend Rule 策略引擎或统一支付工具交易内核；后续 ledger guard 候选已由 `LD-LEDGER-GUARD-012` 消费。 |
| `WALLET-AUTH-ROUTE-SNAPSHOT-PRECHECK-010` | 2026-06-18 | 对 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 做只读源码准入审计。 | 源码确认 core/route 已有 `paymentInstrumentRef` 承载、序列化和回放能力，授权 route resolver 已透传 `instruction.instrumentRef`；当前缺口是 wallet 授权准入构造账户主体型授权请求时尚未把支付工具快照写入资金指令，route snapshot 只能证明存在，不能证明已回链支付工具引用和绑定版本。 | 0 | 已补下一候选准入包；未确认单一 Execution Grant 前不写 Java、测试、DDL/H2 schema 或运行时配置。 |
| `WALLET-AUTH-ROUTE-SNAPSHOT-011` | 2026-06-18 | 消费 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`，补齐支付工具授权准入后的 route snapshot 回链。 | 新增授权请求可选 `PaymentInstrumentRefSpec`、授权 converter 透传、wallet 授权准入快照构造和服务流断言；通过支付工具授权批准后，persisted route snapshot 顶层 `paymentInstrumentRef` 能审计工具号、工具类型、币种、状态、绑定号、绑定版本、绑定角色、内部主体、准入动作和准入决策；交易内核仍以账户主体 `FundsAccountId` 为 canonical 入参。已验证目标测试 3 tests、组合回归 40 tests 和 `just compile` 通过。 | 0 | 本 Grant 已消费；下一轮不得沿用它扩 VCC facade、Spend Rule 策略引擎、完整预交易快照或统一支付工具交易内核；后续 ledger guard 候选已由 `LD-LEDGER-GUARD-012` 消费。 |
| `LD-LEDGER-GUARD-012` | 2026-06-18 | 消费 `GSD2-LD-LEDGER-GUARD-REGRESSION-001`，补齐 ledger 固定账目类别正常余额方向 guard 首轮。 | 新增 `LedgerNormalBalanceGuard`，并接入 `LedgerServiceImpl`、`DefaultLedgerTransactionPostingServiceImpl` 和 `LedgerBalanceProjectionServiceImpl`；目标测试覆盖创建非法账本、历史异常账本入账和余额投影三条路径，均证明失败发生在持久化、posting、LedgerEntry、余额投影和事件发布前。已验证目标 ledger 测试 21 tests、ledger 分组 28 tests、`just compile` 和 `just pmd` 通过。 | 0 | 本 Grant 已消费；下一轮不得沿用它重启 GSD1 大包、扩治理重放或清结算补事实。默认下一候选切换为 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`；若用户优先 B7，则切清算/结算 gate 消费或差异报告。 |
| `B5-BALANCE-ADJUST-013` | 2026-06-19 | 消费 `GSD2-B5-BALANCE-ADJUST-AUDIT-002` 的 route snapshot 审计回链子切片。 | `BalanceControlFundsInstructionRouteResolver` 已把余额调账安全审计摘要写入 persisted route snapshot 顶层 contextVariables；`FundsBalanceAdjustAuditFlowTests` 已证明外部余额异常纠偏 route snapshot 回链来源流水、证据、审批、外部终局、外部余额快照、对账差错、重跑、责任和受控负可用策略，且不泄露 `EXTERNAL_ACCOUNT_REF`。 | 0 | 本子切片已消费；不得沿用它扩独立审计表、运营审批流、泛化运营补账或绕过 B7 差错。下一候选切换为 `GSD2-B5-BALANCE-ADJUST-AUDIT-003` 或 B7 清算/结算验收切片。 |
| `B5-AUDIT-GRANT-PRECHECK-014` | 2026-06-19 | 为 `GSD2-B5-BALANCE-ADJUST-AUDIT-003` 建立下一轮确认包。 | 新增 [GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md](GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md)，产品和架构裁决首个子切片选择“余额调账独立审计查询”，不选择“运营审批闭环”；该确认包明确 no-ddl、只读查询、无资金副作用、敏感字段不泄露和首个 Red，并补齐 Product Context Card、Engineering Handoff Card、Production Loop Card、源码锚点、候选落点、首个 Red 设计提示、可复制确认文本、Grant 消费预检清单和 Grant 消费运行卡。 | 0 | 当时处于用户确认前准备态；后续已由 `B5-AUDIT-QUERY-017` 消费为 Green。 |
| `LWT-RUNTIME-GOAL-015` | 2026-06-19 | 复核运行时 Goal objective 与当前工作树，明确旧 objective 中的 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 已过期。 | `git log`、本文、W5 和 OpenSpec tasks 均证明 B2 wallet 授权准入、授权 route snapshot、ledger guard 和 B5-002 已消费；当时活跃未完成编码计划为 `EMPTY`，B5-003 仍处于确认包准备态。 | 0 | 该判断已被 `B5-AUDIT-QUERY-017` 更新；后续自动续跑必须以当前工作树、本文和 OpenSpec tasks 为权威状态。 |
| `B5-AUDIT-SOURCE-ANCHOR-016` | 2026-06-19 | 对 `GSD2-B5-BALANCE-ADJUST-AUDIT-003` 做只读源码锚点复核和 Red 可行性收窄。 | B5-003 确认包已补明确候选接口、Query、DTO、实现类路径，校准 `ledger/ledger-face/.../service/LedgerTransactionService.java` 查询锚点，并记录 `FundsBalanceAdjustAuditFlowTests` 现有外部余额异常纠偏请求构造为私有方法，首个 Red 需在同类测试或抽取测试支撑之间收窄；实现只允许调用 ledger 查询方法，不得调用 posting、更新、删除、replay 或补事实入口。 | 0 | 该源码复核已被 `B5-AUDIT-QUERY-017` 消费；后续不得复用 B5-003 扩审批、补事实或清结算。 |
| `B5-AUDIT-QUERY-017` | 2026-06-19 | 消费 `Execution Grant：GSD2-B5-BALANCE-ADJUST-AUDIT-003`，补齐余额调账独立审计查询最小服务流。 | 新增 `FundsBalanceAdjustmentAuditApplicationService`、Query、DTO、完整性枚举和只读聚合实现；审计查询以资金交易主事实为定位轴，可按业务流水和交易流水解释外部余额异常纠偏调账的交易事实、route snapshot、ledger transaction 和 LedgerEntry；目标服务流测试证明查询无余额或账本副作用、不泄露外部账户敏感上下文，且交易事实和 route snapshot 存在但账本事实缺失时返回 `INCOMPLETE_LEDGER` 而不是误判未找到；同时补 `FundsBenefitSpecValidators` 精确 Money value object 允许规则。目标测试、接口影响面测试、余额控制、边界、交易分组、compile 和 PMD 已通过。 | 0 | 本 Grant 已消费；后续不得沿用它扩独立审计表、运营审批流、泛化运营补账、补事实执行、B7 差错创建或生产权限模型。下一轮需重新确认 B7 清算/结算 gate、B7 差异报告、wallet 完整预交易快照或其他单一 Grant。 |
| `B7-GATE-SCOPE-018` | 2026-06-19 | 对 `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 做只读准入审计。 | 当时确认 `gateObjectSn` 已存在于 gate request / decision，但差错事实侧尚未承载阻断对象字段。 | 0 | 该审计已被 `B7-OBJECT-GATE-GREEN-021` 消费；后续不得回退为待确认状态。 |
| `B7-CLSSET-GATE-GRANT-PRECHECK-019` | 2026-06-19 | 为 `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 建立下一轮确认包。 | 新增 [GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md](GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md)，把进入前决策收敛为类型级和对象级两个选项。 | 0 | 该确认包已按对象级方案消费；后续不得继续等待 scopeDecision。 |
| `B7-GATE-SCOPE-SOURCE-AUDIT-020` | 2026-06-19 | 对 B7 清算 / 结算 gate 的阻断粒度做源码级 Gap Audit。 | 当时确认对象级阻断不能靠既有类型级查询隐式实现，需显式补字段、schema、Mapper 和 Red。 | 0 | 该 Gap 已被 `B7-OBJECT-GATE-GREEN-021` 关闭为对象级 Gate 基座；真实清算 / 结算消费方仍需新 Grant。 |
| `B7-OBJECT-GATE-GREEN-021` | 2026-06-19 | 消费 `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 的 `scopeDecision=object-scope-schema-backed` 切片。 | 新增差错阻断对象字段、H2 schema 字段和对象级查询索引，gate 查询演进为“对象级精确命中 + 历史类型级保守命中”；目标测试覆盖同类型不同对象不误阻断、结算对象精确阻断、复合阻断范围下对象类型精确匹配、对象级差错幂等、对象字段成对校验和无账本副作用。已验证目标测试、`test-reconciliation`、`compile` 和 `pmd` 通过。 | 0 | 本 Grant 已消费并随 `0d3f68dc` 提交；下一轮不得复用它扩完整清分、清算、结算、出款、补事实执行、生产迁移或运营后台。 |
| `B7-GATE-OBJECT-SCOPE-HANDOFF-021` | 2026-06-19 | 收敛 B7 对象级阻断最小字段和兼容命中规则。 | 确认包第 7.3 / 7.4 已补 `blockingObjectType / blockingObjectSn` 字段口径：差错侧表达阻断对象，gate 侧继续使用 `gateObjectType / gateObjectSn` 表达消费对象；对象级查询需要同时支持精确对象命中和历史类型级差错保守阻断。 | 0 | 本轮仍不授权 Java、测试、DDL/H2、Entity 或 Mapper 写入；用户确认对象级 Grant 后，首个 Red 应优先证明同类型不同对象不误阻断，并证明历史类型级差错不会被静默放行。 |
| `B7-GATE-AUTHORIZATION-WAIT-022` | 2026-06-19 | 对未确认 `scopeDecision` 的连续续跑做停止点收敛。 | 当时 B7 对象级阻断交接包已通过结构检查，缺的是用户确认 `scopeDecision` 和单一 Execution Grant。 | 0 | 该等待点已被用户推荐并消费为 `B7-OBJECT-GATE-GREEN-021`；后续只等待新的单一 Grant。 |
| `B2-AUTH-ADMISSION-BASELINE-023` | 2026-06-19 | 按运行时请求优先复核 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`。 | 源码和任务账本确认该 Grant 已由 `WALLET-AUTH-ADMISSION-009` 消费；本轮只做恢复核验并重跑授权准入目标测试和组合回归。沙箱内 Spring 上下文失败后按项目约规在沙箱外重跑，`just test-one AuthorizationAdmissionApplicationServiceTests tests` 3 tests 通过，`just test-one AuthorizationAdmissionApplicationServiceTests,PaymentInstrumentCapabilityApplicationServiceTests,RouteSnapshotJsonSupportTests,FundsAuthorizationTransactionFlowTests tests` 40 tests 通过，证明支付工具授权准入服务流、工具能力、route snapshot 回链和账户主体型授权内核当前仍健康。 | 0 | 本轮不新增 Java、测试、DDL/H2、公共契约或 Git；B2 授权准入继续作为已消费被依赖基线，下一轮仍需在 B7 差异报告、wallet 完整预交易快照、账户能力来源组合或 Spend Rule 控制闭环中重新确认单一 Grant。 |
| `B7-CLSSET-CONSUMER-GRANT-PACK-024` | 2026-06-19 | 为 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 建立清算 / 结算真实消费方确认包。 | 新增 [GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md](GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md)，把下一步收敛为新增清算 / 结算 gate 准入 consumer，复用对象级 Gate 基座和 `PayoutOrderService` 的 preflight 样板；首个 Red 证明清算对象命中对象级未闭环差错时阻断且无账务副作用。 | 0 | 本轮只新增确认包和状态回写，不授权 Java、测试、DDL/H2 schema、公共契约、完整清结算生命周期、补事实、生产迁移或 Git。 |
| `B7-CLSSET-CONSUMER-SOURCE-AUDIT-025` | 2026-06-19 | 对清算 / 结算真实消费方确认包做只读源码 Gap Audit 和首个 Red 落点复核。 | 确认当前没有 `ClearingSettlementGateConsumerService` 或等价 consumer；`PayoutOrderServiceImpl#checkPayoutPreflight` 是真实消费 gate 样板；`PayoutPreflightServiceTests` 提供无账务副作用断言样板；对象级 Gate 基座已具备 `blockingObjectType / blockingObjectSn` 和 `CheckReconciliationGateRequest.gateObjectType / gateObjectSn`。确认包第 12 节已收敛推荐测试类、首个 Red、候选文件落点、准出和停止条件。 | 0 | 仍不授权 Java、测试、DDL/H2 schema、公共契约、运行时配置或 Git；用户复制确认 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 后，才从 `ClearingSettlementGateConsumerServiceTests` 首个 Red 进入编码闭环。 |
| `B7-OBJECT-GATE-VERIFY-026` | 2026-06-19 | 复跑 B7 对象级 Gate 基座目标验证和 reconciliation 分组回归。 | 沙箱内目标 Spring 测试因 embedded Redis 本地端口探测 `SocketException: Operation not permitted` 失败，已按项目约规在非沙箱环境复跑同一命令并通过：`just test-one ReconciliationGateApplicationServiceTests,ReconciliationDifferenceApplicationServiceTests tests` 20 tests 通过；`just test-reconciliation` 26 tests 通过。 | 0 | 对象级 Gate 基座验证证据刷新完成；本轮不新增 Java、测试、DDL/H2 schema、公共契约或 Git，也不授权清算 / 结算真实 consumer 编码。 |
| `B7-CLSSET-CONSUMER-GREEN-027` | 2026-06-19 | 消费 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001`，新增清算 / 结算只读 gate consumer 服务。 | 新增 `CheckClearingSettlementGateRequest`、`ClearingSettlementGateResultDTO`、`ClearingSettlementGateConsumerService`、`ClearingSettlementGateConsumerServiceImpl` 和 `ClearingSettlementGateConsumerServiceTests`；Red 首次证明 consumer 服务缺失，Green 复用 `ReconciliationGateApplicationService#checkGate`，覆盖清算对象阻断、结算对象阻断、同类型不同对象不误阻断、处理后重跑对平条件放行、非法请求拒绝和无账本副作用。目标测试 5 tests、`test-reconciliation` 26 tests、`compile` 和 `pmd` 通过；沙箱内 embedded Redis 端口限制已按约规在非沙箱重跑确认。 | 0 | 本 Grant 已消费并随 `0d3f68dc` 提交；不得沿用它扩完整清分、清算、结算、出款、补事实、运营审批、生产迁移或报表。其后一轮 B7 差异报告候选已由 `B7-REPORT-GREEN-032` 消费。 |
| `B7-REPORT-GRANT-PACK-028` | 2026-06-19 | 为 `GSD2-B7-RECON-DIFFERENCE-REPORT-001` 建立对账差异报告确认包。 | 新增 [GSD-2-B7-对账差异报告ExecutionGrant确认包.md](GSD-2-B7-对账差异报告ExecutionGrant确认包.md)，把下一步收敛为单笔只读差异报告查询：解释差错状态、阻断对象、处理动作、原始事实、重跑结果、gate 决策和证据引用，且不新增生产 DDL、不补事实、不写清结算或资金事实。 | 0 | 本轮只新增确认包和状态回写，不授权 Java、测试、DDL/H2 schema、公共契约、运营后台、导出、完整清结算生命周期、补事实、生产迁移或 Git。 |
| `B7-REPORT-SOURCE-AUDIT-029` | 2026-06-19 | 对 B7 对账差异报告确认包做只读源码落点审计。 | 当前源码确认：差错写服务位于 `application/difference`，gate 服务位于 `application/gate`，清算 / 结算 consumer 位于 `service` / `services/impl`；Request、DTO、枚举分别落在 `model/request`、`model/dto`、`reconciliation/enums`；`ReconciliationDifferenceMapper` 缺少报告所需的不加锁单笔查询。确认包已把候选接口校准为 `application/difference/report`，Query 改为 Request 风格，并补只读 mapper 方法和目标测试包路径。 | 0 | 本轮只做 docs-only 准入可消费性修正；不授权 Java、测试、DDL/H2 schema、公共契约、Mapper 实现、运行时配置或 Git。用户确认 `GSD2-B7-RECON-DIFFERENCE-REPORT-001` 后，才可从报告服务首个 Red 进入编码闭环。 |
| `B7-REPORT-GRANT-RUNBOOK-030` | 2026-06-19 | 为 B7 对账差异报告确认包补齐 Grant 消费预检清单和运行卡。 | 确认包第 10 / 11 节已明确授权文本、工作树、当前基线、首个 Red、写入范围、验证顺序、Git 策略、Pick / Red / Green / Review / Verify / Handoff 和最小断言清单。确认后默认从 `B7-REPORT-RED-001` 开始，只证明单笔报告解释对象级未闭环差错且无资金副作用。 | 0 | 本轮只增强确认包可执行性；未确认 Grant 前仍不授权 Java、测试、DDL/H2 schema、公共契约、Mapper 实现、运行时配置、Git 或生产发布。 |
| `B7-REPORT-ENTRY-SYNC-031` | 2026-06-19 | 同步 GSD-2 总入口和 LWT Goal 的当前确认包口径。 | `GSD-2-新基线工作流规划.md` 已从“无待消费确认包”修正为“B7 对账差异报告确认包已准备但未授权编码”；本文现状和影响范围已改为 B7 报告 docs-only 准入材料，不再误写为 B5 route snapshot 审计摘要或清算 / 结算 gate 消费缺口。 | 0 | 该准备态已被下一轮 `B7-REPORT-GREEN-032` 消费。 |
| `B7-REPORT-GREEN-032` | 2026-06-19 | 消费 `Execution Grant：GSD2-B7-RECON-DIFFERENCE-REPORT-001`，完成对账差异报告最小只读查询 Red / Green / Verify。 | 新增 `ReconciliationDifferenceReportApplicationService`、查询 Request、报告 DTO、完整性枚举、只读 mapper 查询和 report impl；新增目标服务流测试证明对象级未闭环差错报告可解释差错、阻断对象、gate 阻断、证据引用、报告视图开关、处理动作证据不完整、缺重跑结果和历史类型级差错缺 gate 决策，并保持无账本事实副作用；`test-reconciliation` 已纳入报告测试。目标测试 5 tests、对账分组 31 tests、compile 和 PMD 通过；沙箱内 embedded Redis 端口限制已在非沙箱复跑目标测试和对账分组确认。 | 0 | 当前 Grant 已消费并提交；不得沿用它扩批次报告、导出、运营后台、Controller、HTTP/RPC、补事实、完整清结算生命周期或生产发布。 |

## 2. 现状和影响范围

现状：`GSD2-B7-RECON-GATE-CONSUME-002` 已在 `a38776c5` 提交，`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 和 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING` 已推进到 `ca603eab`；`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 与 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 已在 `dd442888` 前完成本地 Green，wallet 授权准入能从支付工具解析到账户主体、委派授权内核，并把支付工具引用、绑定版本和准入决策固化到 route snapshot。`GSD2-LD-LEDGER-GUARD-REGRESSION-001` 已在 `0b251593` 固化固定账目类别正常余额方向 guard 首轮。`GSD2-B5-BALANCE-ADJUST-AUDIT-002` 已在 `da3b4f19` 完成 route snapshot 审计回链子切片，`GSD2-B5-BALANCE-ADJUST-AUDIT-003` 已在 `4ef64275` 提交余额调账独立审计查询最小服务流。B7 对象级 Gate 基座和清算 / 结算只读 consumer 已在 `0d3f68dc` 提交固化。`GSD2-B7-RECON-DIFFERENCE-REPORT-001` 已在当前工作树完成单笔只读差异报告查询本地 Green / Verify。ledger、wallet、transaction 和 reconciliation 已完成多个 service-flow-backed 首轮 Green，但仍存在完整预交易快照、账户能力来源组合、B7 批次报告 / 导出 / 运营后台、完整清分 / 清算 / 结算生命周期、运营审批闭环和生产权限模型等生产可用缺口。

影响范围：本轮影响 `reconciliation-face` 报告查询契约、`reconciliation-impl` 只读聚合实现、`tests` 目标服务流测试、`Justfile` 对账分组入口，以及 B7 对账差异报告确认包、LWT Goal、GSD-2 总入口、W5、README 和 OpenSpec tasks 的状态同步；不新增 DDL/H2 schema、资金写入链路、完整清结算、运行时配置、外部通道、生产迁移或 Git 历史重写。后续任一候选进入编码前，必须重新确认单一 Execution Grant。

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
| Conditional Ready | ledger guard 和治理类操作边界 | 固定账目类别正常余额方向 guard 已覆盖创建、入账和余额投影入口；任一后续触碰 posting、entry、余额投影或 schema 的切片仍需伴随回归。 | 后续资金变化切片继续复跑 ledger 目标测试和分组测试。 |
| Not Done | 生产迁移脚本、治理重放、差异报告、清结算补事实链路和更完整账务运营工作台 | 不影响当前已完成切片解释，但不能据此声明完整账务生产闭环。 | 放入后续单一 Grant，不与 wallet 或 transaction 小切片混写。 |
| Blocker | 把预算组、支付工具、父账户或投影写为 ledger subject | 一旦发现必须立即停止编码，回到 PRD/DSL/系分/TDD 修正。 | 资金主体红线前置评审。 |

### 5.2 Wallet

| 状态 | 能力 | 当前结论 | 下一动作 |
| --- | --- | --- | --- |
| Ready | funding account / credit account、账户层级来源、资金责任目标主体、资金责任解析 facade、支付工具能力准入 facade、授权准入 facade 最小服务流 | 已完成首轮服务流 Green，支付工具仍是能力入口和快照维度，不是账务主体。 | 后续作为 VCC、全球账户和更完整预交易准入的被依赖能力。 |
| Conditional Ready | wallet application facade | 已能完成授权准入最小服务流和 route snapshot 支付工具引用回链，但完整预交易快照、账户能力来源组合和 Spend Rule 控制闭环仍未完成。 | 新的预交易快照、账户能力来源组合或 Spend Rule 控制 Grant。 |
| Not Done | 钱包账户聚合、账户能力来源组合、完整预交易快照、Spend Rule 控制闭环、VCC facade | 仍不能声明 wallet 全量生产 Done。 | 按业务价值拆成 contract-only 或 service-flow-backed 小切片。 |
| Blocker | 让调用方绕过 application facade 自行拼资源服务，或把支付工具能力通过等同于账户资金可用 | 会破坏资金责任、快照和审计一致性。 | 进入 wallet facade 或 admission 专项 Grant。 |

### 5.3 Transaction

| 状态 | 能力 | 当前结论 | 下一动作 |
| --- | --- | --- | --- |
| Ready | 账户主体型直接交易、授权交易、余额控制、route snapshot、原路径回放和基础投影解释 | 已有 B4、B5、B7 多个服务流切片证明交易事实、失败无副作用和只读解释边界。 | 保持 canonical 内核以账户主体为稳定入参。 |
| Conditional Ready | 交易投影解释、余额调账审计和兼容 chargeback 最小 guard | B4-001、B4-002 首轮争议退款、B4-002 remaining、B5-001、B5-002 route snapshot 审计回链、B5-003 独立审计查询和 AUTH 兼容 adapter 已完成最小闭环；查询解释矩阵已覆盖付款、授权拒绝、缺快照、普通退款、无授权退款、争议退款、释放/过期和兼容 chargeback，余额调账审计查询可解释交易、route 和账本事实。 | 后续按 B7 差异报告、清算/结算 gate 消费、失败态解释或运营审批专项补强。 |
| Not Done | projection store、治理重放、失败态全量解释、差异报告和完整 dispute case | `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 只保护历史兼容入口的最小审计上下文，不声明完整 dispute / chargeback 生产链路 Done。 | 进入 B5、B7 或 P2 专项前重新确认单一 Grant。 |
| Blocker | 把交易内核整体改成支付工具入参，或让交易投影反写事实 | 会破坏账户主体 canonical 入账和重放稳定性。 | 停止并回到架构裁决。 |

### 5.4 Reconciliation / Clearing / Settlement

| 状态 | 能力 | 当前结论 | 下一动作 |
| --- | --- | --- | --- |
| Ready | 对账差错对象、动作守卫、只读 gate、出款 preflight 消费、对象级阻断基座和清算 / 结算只读 consumer | B7-001、B7-002、Gate-001、Gate-002 已证明差错可阻断出款准入；`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 已按 `scopeDecision=object-scope-schema-backed` 本地 Green；`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 已证明清算 / 结算服务可真实消费对象级 gate 且无账务副作用。 | 可作为 B7 后续差异报告、完整清结算生命周期或运营审批专项的前置证据。 |
| Conditional Ready | 清算、结算、出款消费方接入 | 出款 preflight 和清算 / 结算只读 consumer 已接入 gate；但清算候选、结算单、出款完整生命周期、清分分润和生产迁移仍未接入。 | 下一轮若继续 B7，应重新确认“B7 差异报告”或完整清结算生命周期中的单一 Grant，不复用对象级 Gate 或 consumer Grant。 |
| Not Done | 差异报告、补事实命令执行服务、运营审批流、追偿、账龄升级、生产迁移脚本 | 不能声明完整 B7 生产 Done。 | 独立拆分 Grant，避免和交易/钱包基础能力混写。 |
| Blocker | 差错处理直接生成资金事实，或绕过白名单、审批和原始事实引用 | 会破坏账务可追溯和职责分离。 | 进入补事实专项设计和审批门禁。 |

### 5.5 生产可用基线判定矩阵

本轮判定口径是“可被上层 MVP 继续依赖的条件基线”，不是“ledger、wallet、transaction 三模块全量生产 Done”。满足条件基线意味着后续 VCC、全球账户、清结算和对账切片可以消费已完成的账户主体、route snapshot、ledger entry、余额投影、交易投影和对账 gate 证据；但任一新资金写入场景仍必须通过单一 Execution Grant、首个 Red、目标测试和回写 Not Done。

| 基线项 | 必备能力 | 当前证据 | 判定 | 下一轮准入要求 |
| --- | --- | --- | --- | --- |
| 入账主体基线 | 资金账户、信用账户和明确授权控制账户是核心 ledger subject；支付工具、预算组、父账户、Spend Rule 和投影只作为控制、快照、归集或审计维度。 | PRD/DSL/系分/TDD 已统一主体口径，B2 账户层级和资金责任目标主体已完成服务流 Green。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 后续任一场景发现非账务主体入账，立即停止并回到设计基线。 |
| 钱包准入基线 | wallet application facade 至少能解析账户层级、资金责任和支付工具能力，并把能力快照交给交易内核。 | 资金责任解析 facade、支付工具能力准入 facade、授权准入 facade 和授权 route snapshot 支付工具回链已完成首轮 Green。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 进入 VCC、全球账户或完整预交易准入前，必须补完整预交易快照或说明为何本切片不需要。 |
| 交易内核基线 | 直接交易、授权交易、余额控制和原路径回放继续以账户主体作为 canonical 入参，支付工具只在外层解析。 | B4、B5、B7、wallet 授权准入和本轮 ledger guard 切片已证明 route snapshot、失败无副作用、只读解释、支付工具快照回链、账户主体委派边界、账目方向护栏和余额调账审计查询。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 下一步优先补 B7 清算/结算 gate 消费、B7 差异报告或 wallet 完整预交易快照，继续保持 canonical 请求不替换为支付工具引用。 |
| 账本护栏基线 | 每笔资金变化必须能断言 posting plan 平衡、LedgerEntry、余额桶、幂等和失败无副作用。 | 已完成切片均以 ledger 事实和余额投影作为验证护栏；本轮补齐固定账目类别与正常余额方向不一致时的创建、入账和投影 fail-fast。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 后续触碰 posting、entry、余额投影或 schema 时复跑目标 ledger 测试和 `test-ledger`，不得把本轮 guard 外推为治理重放或清结算补事实 Done。 |
| 对账准入基线 | 差错登记、动作守卫、只读 gate、对象级阻断基座、出款 preflight 消费和清算 / 结算只读 consumer 能阻断未闭环差错。 | B7 差错闭环、动作守卫、gate 决策、出款 preflight 消费、对象级差错阻断和清算 / 结算 consumer 已完成首轮 Green。 | `CONDITIONAL_DELIVERABLE_BASELINE`。 | 差异报告、完整清分、清算、结算、出款、补事实、运营审批和生产迁移仍需独立 Grant；不得把 consumer 外推为完整清结算 Done。 |
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
| 5 | `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` | `consumed-green` | 已补支付工具授权准入后的 route snapshot 顶层 `paymentInstrumentRef` 回链。 | 不继续沿用本 Grant 扩 VCC 生命周期、Spend Rule 策略引擎、完整预交易快照或统一支付工具交易内核。 |
| 6 | `GSD2-LD-LEDGER-GUARD-REGRESSION-001` | `consumed-green` | 已补固定账目类别正常余额方向 guard，并覆盖创建、入账和余额投影失败无副作用。 | 不继续沿用本 Grant 重启 GSD1 大包、扩治理重放或清结算补事实。 |
| 7 | `GSD2-B5-BALANCE-ADJUST-AUDIT-002` | `consumed-green` | 已补余额调账 route snapshot 审计回链，证明外部余额异常纠偏的来源、证据、审批、对账、责任和受控负可用策略可从 route snapshot 解释。 | 不继续沿用本 Grant 扩独立审计表、运营审批流或泛化运营补账。 |
| 8 | `GSD2-B5-BALANCE-ADJUST-AUDIT-003` | `consumed-green` | 已补余额调账独立审计查询最小服务流，能只读聚合交易事实、交易明细上下文、route snapshot、ledger transaction 和 LedgerEntry，并验证敏感字段不泄露、查询无资金副作用。 | 不继续沿用本 Grant 扩独立审计表、运营审批流、泛化运营补账、补事实执行、B7 差错创建或生产权限模型。 |
| 9 | `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` | `consumed-green-committed` | 已按 `scopeDecision=object-scope-schema-backed` 完成对象级阻断基座，并随 `0d3f68dc` 提交：差错事实、DTO、H2 schema、Mapper 和 gate 查询支持对象精确命中，兼容历史类型级差错。 | 本 Grant 不再复用；不一次性做完整清分、清算、结算、出款和补事实，不做生产迁移或运营后台。 |
| 10 | `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` | `consumed-green-verified-committed` | 已新增清算 / 结算只读 gate consumer 并随 `0d3f68dc` 提交，证明 CLEARING / SETTLEMENT 对象可真实消费对象级 gate，且目标测试、reconciliation 分组、compile 和 PMD 通过。 | 不继续沿用本 Grant 扩完整清分分佣、出款生命周期、补事实执行、运营审批、生产迁移或报表大包。 |
| 11 | `GSD2-B7-RECON-DIFFERENCE-REPORT-001` | `consumed-green-verified-committed` | 已完成单笔只读差异报告查询：解释差错状态、阻断对象、处理动作、重跑结果、gate 决策和证据引用，并证明查询无账务副作用；已提交到 `a1397ddf`。 | 不继续沿用本 Grant 扩批次报告、运营后台、导出、完整清结算、补事实或生产迁移。 |
| 12 | Spend Rule / P2 VCC / 全球账户 | `contract-only` 起步 | 业务价值高，但必须消费账户、钱包 facade、交易投影、ledger guard、余额调账审计回链、对象级 gate、清算 / 结算 consumer 和必要报告解释证据。 | 不直接写 P2 生产代码、外部轨道或通道规则。 |

### 8.1 单一 Grant 决策账本

本账本用于把 `Goal Active`、`Plan Grant docs-only` 和 `Execution Grant` 的边界拆开。它只决定下一轮如何从状态载体进入单一候选，不替代用户授权、架构师 CAD 门禁、目标测试或 Git 授权。

| 决策项 | 当前裁决 | 进入条件 | 切换条件 | 状态回写 |
| --- | --- | --- | --- | --- |
| 默认推荐 | B7 对账差异报告已提交，下一轮从 wallet 完整预交易快照、账户能力来源组合、Spend Rule 控制闭环或 B7 报告扩展中重新选择单一 Grant。 | 新的编码切片需重新确认单一 Execution Grant、首个 Red、写入范围、验证命令和停止条件。 | 若优先完整清结算生命周期、运营审批或 P2 业务，则另起专项 Grant，不复用已消费 consumer 或 report Grant。 | 本文第 8 节、第 10 节、第 11 节、W5 推进计划、B7 对账差异报告确认包和 OpenSpec tasks。 |
| Wallet 授权准入 | 目标是补齐支付工具授权入口的最小服务流。 | 已按 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 完成 application 契约、Request、实现和服务流测试。 | 后续需要 route snapshot 回链、Spend Rule、VCC facade、完整预交易快照或交易 canonical 入参调整时，必须重新开 Grant。 | 本文第 1.2 节、第 5.2 节、第 8 节和 Evidence Anchor Matrix。 |
| Wallet 授权准入恢复核验 | 防止旧运行时 Goal 或摘要把已消费 B2 授权准入误恢复为当前默认任务。 | 2026-06-19 已重跑授权准入目标测试 3 tests 和授权准入组合回归 40 tests，均在沙箱外通过；本轮无代码变更。 | 若后续用户仍指定该 ID，先按“已消费基线核验”处理；只有明确提出新的 wallet 子能力，才进入新的单一 Grant。 | 本文第 1.2 节、W5 推进计划和 OpenSpec tasks。 |
| AUTH 兼容 adapter | 目标是防止历史 `chargeback` 被误当新目标主入口。 | 已按 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 完成兼容说明、最小 guard、兼容测试和状态回写。 | 后续需要删除公共 API、改 DDL/H2 schema、完整 dispute case 或迁移到 `settleRefund` 委派时，必须重新开 Grant。 | `GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` 第 14 节已记录 Red/Green、验证证据和 Not Done。 |
| B4 投影解释扩展 | 目标是补齐普通退款、无授权退款、释放/过期和兼容 chargeback 的解释矩阵。 | `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING` 已消费，写入范围限 projection explain 只读查询和目标测试。 | 需要 projection store、治理重放、历史节点选择查询、反写事实、失败态全量解释、运营差异报告或 DDL 时停止并重新开 Grant。 | 已回写第 5.5 查询解释基线为 `CONDITIONAL_DELIVERABLE_BASELINE`。 |
| Wallet facade 补强 | 目标是补完整预交易快照、账户能力来源组合或 Spend Rule 控制闭环。 | 用户确认新的 wallet 子切片；`GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 已消费，只保留为证据。 | 需要 VCC facade、外部规则最终结论或替换交易 canonical 入参时停止。 | 回写第 5.2 Wallet 矩阵和 Evidence Anchor Matrix。 |
| Ledger guard 回归 | 目标是证明后续资金变化仍满足 posting、entry、余额投影和幂等红线。 | 已消费 `GSD2-LD-LEDGER-GUARD-REGRESSION-001` 首轮，覆盖固定账目类别正常余额方向 guard。 | 发现非账务主体入账、余额桶不平、失败有副作用、或后续触碰 posting/entry/projection/schema 时，另起新的 ledger guard 扩展 Grant。 | 回写第 5.1 Ledger 矩阵和第 11 Completion Audit。 |
| B7 清算/结算 gate 消费 | 目标是把对账 gate 的阻断事实从类型级升级为对象级，并作为后续清算/结算消费方前置基座。 | 已按 [GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md](GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md) 中的 `scopeDecision=object-scope-schema-backed` 本地 Green；当前只完成差错登记、DTO、H2 schema、Mapper 和 gate 查询基座。 | 后续需要清算候选、结算单、完整出款、补事实执行、运营审批或生产迁移时，必须重新开单一 Grant。 | 回写第 5.4 Reconciliation 矩阵、W5 Grant 队列、B7 确认包和 OpenSpec tasks。 |
| B5 route snapshot 审计回链 | 目标是补余额调账 route snapshot 回链。 | 已消费 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`；写入范围仅限 route snapshot 安全审计摘要和目标服务流测试。 | 需要独立审计表、运营审批流、泛化运营补账或绕过对账差错时停止并另开 Grant。 | 已回写第 5.3 Transaction 矩阵和 Evidence Anchor Matrix。 |
| B5 审计扩展后续 | B5-003 已补余额调账独立审计查询最小切片。 | 当前只作为已消费证据；后续不能复用本 Grant 继续写 Java、测试或公共契约。 | 需要泛化运营补账、绕过对账差错、真实资金纠偏审批、运营审批闭环、生产权限或独立审计表时停止并另起专项 Grant。 | 已回写第 5.3 Transaction 矩阵、B5 Not Done 和 B5-003 确认包；下一轮从新 Grant 重新开始。 |

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
| 状态一致性 | `rg "GSD2-LWT|LWT-PRODUCTION|CONDITIONAL_DELIVERABLE_BASELINE|PARTIAL_BASELINE|Completion Audit|Evidence Anchor Matrix|单一 Grant 决策账本|Loop Progress Ledger|AuthorizationAdmissionApplicationServiceTests|LedgerNormalBalanceGuard|B7_RECON_DIFFERENCE_REPORT_GREEN_VERIFIED_COMMITTED|GSD2-B7-RECON-DIFFERENCE-REPORT-001|GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001|GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001|blockingObjectType|blockingObjectSn" docs openspec` | GSD2 入口、W5、生产可用基线裁决、完成度审计、证据锚点、单一 Grant 决策账本、Loop Progress Ledger、README、OpenSpec tasks、wallet 授权准入、ledger guard、B5 route snapshot 审计回链、B5-003 独立审计查询、B7 对象级 Gate 基座、B7 清算/结算 consumer 和 B7 差异报告 Green 结果能追踪到本文和当前基线。 |
| Grant 消费可用性 | `rg -n 'Grant 消费预检清单|Grant 消费运行卡|Red 选择|最小断言清单|AUTH-CB-COMPAT-RED-001' docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md openspec/changes/tdd-baseline-reset/tasks.md` | AUTH 兼容确认包不仅有可复制 Grant，还能指导确认后的预检、首个 Red 选择、最小 Green、Review、Verify 和 Handoff。 |
| README 恢复导航 | `rg -n 'W1 基线差距审计把当前 Git/code baseline 校准到|推荐下一步确认 .*GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001|当前仍不授权代码|当前下一候选.*B2|下一候选.*GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001' docs/README.md docs/TDD设计/README.md docs/TDD设计/GSD-2-新基线工作流规划.md openspec/changes/tdd-baseline-reset/tasks.md` | 不再把 W1/W2/W3/W4 的历史基线、历史推荐或 B2 账户层级候选误写成当前下一候选；README 入口应指向本轮支付工具授权准入提交、LWT Goal、B4 remaining 已消费和 wallet 授权准入下一切片。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译和测试 | B7 差异报告涉及 reconciliation-face 查询契约、reconciliation-impl 只读聚合、对账报告目标服务流测试、`Justfile` 对账分组和文档状态回写，已运行目标报告测试、`test-reconciliation`、`just compile`、`just pmd` 和 `git diff --check`。 | Java、测试和公共契约变更均有本地验证证据。 |

## 10. 当前执行交接和 handoff

| 字段 | 内容 |
| --- | --- |
| 当前可执行任务 | `GSD2-B7-RECON-DIFFERENCE-REPORT-001` 已消费为本地 Green / Verify，当前状态 `B7_RECON_DIFFERENCE_REPORT_GREEN_VERIFIED_COMMITTED`。B7 对象级 Gate 基座和清算 / 结算只读 consumer 已在 `0d3f68dc` 提交。 |
| 写入范围 | 本轮已写入 B7 对账差异报告 face 契约、request、DTO、完整性枚举、impl 只读聚合、mapper 只读查询、目标服务流测试、`Justfile` 对账分组和状态文档。未授权 DDL/H2 schema、资金写入链路、运营后台、导出、完整清结算消费方、补事实执行、运行时配置或 Git。 |
| 服务层边界 | 本轮只交付 reconciliation application service 能力；不新增 Controller、HTTP/RPC、页面、导出端点或外部适配入口。 |
| 验证命令 | `just test-one ReconciliationDifferenceReportApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just pmd` 和 `git diff --check` 已通过。 |
| 编译说明 | 本轮已改 Java、测试和文档，并完成目标测试、reconciliation 分组、编译和 PMD；Spring/Redis 端口类 sandbox 失败已按环境问题在非 sandbox 复跑通过。 |
| 下一 owner | 继续编码前需重新确认 B7 报告扩展、wallet 完整预交易快照、账户能力来源组合、Spend Rule 控制闭环等单一 Grant。若要扩完整清结算生命周期、补事实、运营审批或生产迁移，必须另起专项 Grant。 |
| 交接要求 | 后续每轮必须先读取本文、B7 对账差异报告确认包、GSD-2 工作流、W5 推进计划和 OpenSpec tasks，再确认新的单一 Execution Grant、写入范围、禁止范围、验证命令和回滚提示。 |
| 回滚提示 | B7 对象级 Gate 与清算 / 结算 consumer 已提交到 `0d3f68dc`，后续如需回退应按 Git revert 或补偿提交处理；不得连带回滚 `4ef64275`、`da3b4f19` 或更早已提交能力证据。 |

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
| 对齐当前 Git/code baseline | 本文、W5、GSD-2 工作流和 OpenSpec tasks 均校准到 `a1397ddf`；`GSD2-B5-BALANCE-ADJUST-AUDIT-002` 已在 `da3b4f19` 提交固化，`GSD2-B5-BALANCE-ADJUST-AUDIT-003` 已在 `4ef64275` 提交固化，`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001 / scopeDecision=object-scope-schema-backed` 和 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 已在 `0d3f68dc` 提交固化，`GSD2-B7-RECON-DIFFERENCE-REPORT-001` 已在 `a1397ddf` 提交固化，并保留 `0b251593`、`dd442888`、`ea8f8800`、`632bd2f6`、`ca603eab`、`873e5f8c`、`a38776c5`、`bc7ffc0f`、`10853e2d`、`ae8cb8a6`、`e81a8a25` 作为已消费证据。 | `DONE_CURRENT_BASELINE_COMMITTED`。 | 后续选择新的单一 Grant。 |
| 校准 README 恢复导航 | docs README 和 TDD README 已把 W1/W2/W3/W4 标为历史记录或已消费证据，当前入口指向 LWT Goal、B5 route snapshot 审计回链提交、B5-003 余额调账审计扩展确认包、ledger guard 结果、AUTH 兼容 adapter Green 结果、B4 投影解释 remaining 已消费结果、wallet 授权准入与 route snapshot 回链结果。 | `DONE_DOCS_ONLY`。 | 后续改 README 或入口导航时必须复跑 README 恢复导航扫描。 |
| 按依赖顺序推进低风险文档/计划 | AUTH 兼容、B4 投影解释 remaining、wallet 授权准入、route snapshot 回链、ledger guard、B5 route snapshot 审计回链和 B5 独立审计查询均已消费；B5-003 已补 service、Query、DTO、impl、目标测试、验证证据和 Not Done。 | `CONSUMED_GREEN`。 | 下一轮不得复用 B5-003；需重新确认 B7 清算/结算 gate、B7 差异报告或 wallet 后续单一 Grant。 |
| 建立无进展计数和停止账本 | 第 1.2 节已记录 docs-only 推进轮次、证据变化和无进展计数；最近一轮已把运行时 Goal 旧 B2 objective 与当前 B5-003/B7 决策账本的冲突收敛为恢复规则。 | `DONE_DOCS_ONLY`。 | 下一轮若没有 Grant、事实差异、验证证据或状态缺口收敛，应停止 docs-only 扩写并交还用户选择。 |
| 单一 Execution Grant 后进入代码、测试、验证、提交闭环 | `GSD2-B5-BALANCE-ADJUST-AUDIT-002` 已消费并提交到 `da3b4f19`；`GSD2-B5-BALANCE-ADJUST-AUDIT-003` 已消费并提交到 `4ef64275`，补余额调账独立审计查询，目标测试证明查询完整性、敏感字段过滤和无资金副作用；收口追加余额控制、边界、交易分组、compile、PMD 和 diff。 | `CONSUMED_GREEN_COMMITTED`。 | 下一轮必须重新确认新的单一 Grant。 |
| 补齐 AUTH 兼容 Grant 消费预检 | `GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` 第 11 节已补用户授权、工作树状态、当前基线、首个 Red 收窄、写入范围、验证顺序、状态回写和 Git 策略预检，并已被本轮消费。 | `CONSUMED_GREEN`。 | 预检结果只作为本 Grant 审计证据，后续不得复用为新授权。 |
| 补齐 AUTH 兼容 Grant 消费运行卡 | `GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` 第 12 节已补 Red 选择、Red 范围、Green 实现、Review、Verify、Handoff 和最小断言清单，并已被本轮消费。 | `CONSUMED_GREEN`。 | 运行卡结果只作为本 Grant 审计证据，后续不得复用为新授权。 |
| 消费 B7 清算 / 结算真实消费方服务 | `GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md` 已从确认包演进为消费记录；新增 `ClearingSettlementGateConsumerService`、请求、结果 DTO、只读实现和目标服务流测试。Red 首次证明服务缺失；Green 后清算和结算对象能真实消费对象级 gate，且无账本副作用。 | `CONSUMED_GREEN_VERIFIED_COMMITTED`。 | 本 Grant 已在 `0d3f68dc` 提交；下一轮不得沿用它扩完整清分、清算、结算、出款、补事实、运营审批或生产迁移。 |
| 消费 B7 对账差异报告 Grant | `GSD-2-B7-对账差异报告ExecutionGrant确认包.md` 第 13 节已记录 Red / Green / Verify；新增单笔只读差异报告查询能力和目标服务流测试。 | `CONSUMED_GREEN_VERIFIED_COMMITTED`。 | 本 Grant 已提交到 `a1397ddf`；继续能力扩展必须另起新的单一 Grant。 |
| 刷新 B7 对象级 Gate 验证证据 | 非沙箱环境复跑 `just test-one ReconciliationGateApplicationServiceTests,ReconciliationDifferenceApplicationServiceTests tests` 20 tests 通过，`just test-reconciliation` 26 tests 通过；沙箱内 embedded Redis 端口探测失败已归类为环境限制。 | `GREEN_EVIDENCE_REFRESHED`。 | 后续 consumer 编码仍需新的单一 Grant，且不得把对象级 Gate 基座验证外推为完整清算 / 结算 Done。 |
| 证明三模块全量生产可用 | 当前只具备条件可交付基线，仍缺完整预交易快照、失败态全量解释、projection store、治理重放、差异报告、完整清分/清算/结算/出款生命周期、补事实、运营审批、生产迁移、灰度和告警。 | `NOT_DONE`。 | 不得标记 Goal complete；继续按单一 Grant 收敛缺口。 |

审计裁决：当前 Goal 的状态载体、完备性矩阵、条件基线判定、AUTH 兼容 Grant 确认包、B4 投影解释矩阵、wallet 授权准入最小服务流、ledger guard、B5 route snapshot 审计回链和 B5 独立审计查询已完成收口；三模块仍未被证明为全量生产 Done，且下一轮缺口必须重新确认单一 Grant。因此本线程 Goal 继续保持 active。

### 11.2 三卡交接

| 交接卡 | 已具备内容 | 缺口 | 下一 owner |
| --- | --- | --- | --- |
| Product Context Card | 业务目标、用户价值、核心对象、入账主体规则、支付工具边界、投影边界、对账 gate、条件基线和 Not Done 已在第 3 至 5 节表达。 | VCC、全球账户、收单、完整清结算和运营后台验收仍需独立场景卡。 | 产品架构专家。 |
| Engineering Handoff Card | `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING`、`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`、`GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`、`GSD2-LD-LEDGER-GUARD-REGRESSION-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-002`、`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 和 `GSD2-B7-RECON-DIFFERENCE-REPORT-001` 已消费并通过目标测试；B7 报告已提交到 `a1397ddf`。 | 下一轮仍缺新的单一 Execution Grant；若改选后续能力，默认从 wallet 完整预交易快照、账户能力来源组合、Spend Rule 控制闭环或 B7 报告扩展中选择；对象级 Gate、consumer 和 report Grant 均不得复用。 | 资深架构师 + 用户。 |
| Production Loop Card | 已声明生产发布基线未完成，并列出迁移、灰度、回滚、监控、告警、运营审批、外部规则和真实资金前置条件。 | 生产发布、SLO、告警、Runbook、合规/法务/财务/通道确认均未授权。 | 发布 owner / 业务 owner / 合规和财务确认方。 |

三卡裁决：当前三卡可用于下一轮编码准入交接，但不能替代新的 Execution Grant、测试通过、Git 授权或上线审批。`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING`、`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`、`GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`、`GSD2-LD-LEDGER-GUARD-REGRESSION-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-002`、`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 和 `GSD2-B7-RECON-DIFFERENCE-REPORT-001` 已消费；B7 差异报告已提交到 `a1397ddf`。后续不得复用已消费 Grant 继续扩完整 dispute case、projection store、治理重放、VCC facade、Spend Rule 策略引擎、完整预交易快照、统一支付工具交易内核、独立审计表、运营审批流、补事实执行、完整清结算生命周期或清结算补事实；若继续推进，默认重新选择 wallet 后续能力、B7 报告扩展或其他单一 Grant。

### 11.3 Evidence Anchor Matrix

本矩阵只记录当前仓库中的事实锚点，方便后续单一 Grant 消费条件基线时快速定位代码、测试和验证命令。锚点存在不等于对应能力全量 Done；只有目标测试、分组回归、编译、PMD 和本节状态回写共同成立，才能作为该切片的 Green 证据。

| 条件基线 | face / contract 锚点 | impl 锚点 | 测试锚点 | 已记录验证命令 | 证据强度 |
| --- | --- | --- | --- | --- | --- |
| 账户层级和资金责任目标主体 | `wallet/wallet-face` 资金责任 Request/DTO/Query；`targetSubjectType + targetSubjectId` 口径。 | `wallet/wallet-impl` 资金责任关系服务和 H2 `t_spend_subject_funding_rel`。 | `tests/src/test/java/com/wind/funds/wallet/services/impl/SpendSubjectFundingRelationServiceImplTests.java`。 | `just test-one SpendSubjectFundingRelationServiceImplTests tests`。 | `SERVICE_FLOW_GREEN_RECORDED`。 |
| 资金责任解析 facade | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/funding/FundingResponsibilityResolutionApplicationService.java`。 | `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application/funding/impl/FundingResponsibilityResolutionApplicationServiceImpl.java`。 | `tests/src/test/java/com/wind/funds/wallet/application/funding/FundingResponsibilityResolutionApplicationServiceTests.java`。 | `just test-one FundingResponsibilityResolutionApplicationServiceTests tests`。 | `SERVICE_FLOW_GREEN_RECORDED`。 |
| 支付工具能力准入 facade | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/instrument/PaymentInstrumentCapabilityApplicationService.java`。 | `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application/instrument/impl/PaymentInstrumentCapabilityApplicationServiceImpl.java`。 | `tests/src/test/java/com/wind/funds/wallet/application/instrument/PaymentInstrumentCapabilityApplicationServiceTests.java`。 | `just test-one PaymentInstrumentCapabilityApplicationServiceTests tests`。 | `SERVICE_FLOW_GREEN_RECORDED`。 |
| 交易投影解释首轮 | `transaction/transaction-face/src/main/java/com/wind/funds/transaction/projection/FundsTransactionProjectionExplainApplicationService.java`。 | `transaction/transaction-impl/src/main/java/com/wind/funds/transaction/projection/impl/DefaultFundsTransactionProjectionExplainApplicationService.java`。 | `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsTransactionProjectionExplainApplicationServiceTests.java`、`tests/src/test/java/com/wind/funds/transaction/application/flow/DefaultRoutedFundsInstructionOrchestratorProjectionTests.java`。 | `just test-one FundsTransactionProjectionExplainApplicationServiceTests tests`、`just test-one DefaultRoutedFundsInstructionOrchestratorProjectionTests tests`、`just compile`、`just pmd`。 | `PARTIAL_BASELINE_GREEN_RECORDED`。 |
| 余额调账审计和 route snapshot 回链 | `transaction-face` balance adjust 请求审计字段、外部异常来源类型和 route snapshot 查询契约。 | `transaction-impl` instruction converter、command service、交易明细上下文透传和 `BalanceControlFundsInstructionRouteResolver` route context 安全摘要。 | `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsBalanceAdjustAuditFlowTests.java`、`tests/src/test/java/com/wind/funds/transaction/application/flow/FundsBalanceControlFailureFlowTests.java`。 | `just test-one FundsBalanceAdjustAuditFlowTests tests`、`just test-balance-control`、`just test-reconciliation`、`just compile`、`just pmd`。 | `SERVICE_FLOW_GREEN_RECORDED_WITH_ROUTE_AUDIT_SNAPSHOT_BACKLINK`。 |
| 余额调账独立审计查询 | `transaction/transaction-face/src/main/java/com/wind/funds/transaction/application/FundsBalanceAdjustmentAuditApplicationService.java`、`model/query/FundsBalanceAdjustmentAuditQuery.java`、`model/dto/FundsBalanceAdjustmentAuditDTO.java`、`enums/FundsBalanceAdjustmentAuditCompleteness.java`。 | `transaction/transaction-impl/src/main/java/com/wind/funds/transaction/application/impl/DefaultFundsBalanceAdjustmentAuditApplicationService.java` 只读聚合交易、明细、route snapshot、ledger transaction 和 LedgerEntry。 | `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsBalanceAdjustAuditFlowTests.java`、`tests/src/test/java/com/wind/funds/dsl/LedgerDtoContextVariablesContractTests.java`。 | `just test-one LedgerDtoContextVariablesContractTests tests`、`just test-one FundsBalanceAdjustAuditFlowTests tests`、`just test-balance-control`、`just test-boundary`、`just test-transaction`、`just compile`、`just pmd`。 | `SERVICE_FLOW_GREEN_RECORDED_WITH_AUDIT_QUERY_AND_NO_SIDE_EFFECTS`。 |
| 对账差错和动作守卫 | `reconciliation-face` 差错 application 契约、差错状态、动作类型和处理回链请求。 | `reconciliation-impl` 差错 Entity、Mapper、服务和 H2 `t_reconciliation_difference`。 | `tests/src/test/java/com/wind/funds/reconciliation/application/difference/impl/ReconciliationDifferenceApplicationServiceTests.java`。 | `just test-one ReconciliationDifferenceApplicationServiceTests tests`、`just test-reconciliation`、`just verify-fast`、`just pmd`。 | `SERVICE_FLOW_GREEN_RECORDED`。 |
| 对账 gate、出款 preflight 和清算 / 结算 consumer 消费 | `reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/application/gate/ReconciliationGateApplicationService.java`、`reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/service/ClearingSettlementGateConsumerService.java`、`reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/model/request/CheckClearingSettlementGateRequest.java`、`reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/model/dto/ClearingSettlementGateResultDTO.java`。 | `reconciliation/reconciliation-impl/src/main/java/com/wind/funds/reconciliation/application/gate/impl/ReconciliationGateApplicationServiceImpl.java`、`reconciliation/reconciliation-impl/src/main/java/com/wind/funds/reconciliation/services/impl/PayoutOrderServiceImpl.java`、`reconciliation/reconciliation-impl/src/main/java/com/wind/funds/reconciliation/services/impl/ClearingSettlementGateConsumerServiceImpl.java`。 | `tests/src/test/java/com/wind/funds/reconciliation/application/gate/impl/ReconciliationGateApplicationServiceTests.java`、`tests/src/test/java/com/wind/funds/reconciliation/application/difference/impl/ReconciliationDifferenceApplicationServiceTests.java`、`tests/src/test/java/com/wind/funds/reconciliation/services/impl/PayoutPreflightServiceTests.java`、`tests/src/test/java/com/wind/funds/reconciliation/services/impl/ClearingSettlementGateConsumerServiceTests.java`。 | `just test-one ReconciliationGateApplicationServiceTests,ReconciliationDifferenceApplicationServiceTests tests`、`just test-one PayoutPreflightServiceTests tests`、`just test-one ClearingSettlementGateConsumerServiceTests tests`、`just test-reconciliation`、`just compile`、`just pmd`。 | `SERVICE_FLOW_GREEN_RECORDED_WITH_CLEARING_SETTLEMENT_CONSUMER_AND_OBJECT_GATE_VERIFIED`。 |
| 支付工具授权准入 facade 和 route snapshot 回链 | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/instrument/AuthorizationAdmissionApplicationService.java`、`wallet/wallet-face/src/main/java/com/wind/funds/wallet/model/request/AuthorizeByPaymentInstrumentRequest.java`、`transaction/transaction-face/src/main/java/com/wind/funds/transaction/model/request/FundsAuthorizationTransactionAuthorizeRequest.java`。 | `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application/instrument/impl/AuthorizationAdmissionApplicationServiceImpl.java`、`transaction/transaction-impl/src/main/java/com/wind/funds/transaction/converter/FundsAuthorizationInstructionConverter.java`。 | `tests/src/test/java/com/wind/funds/wallet/application/instrument/AuthorizationAdmissionApplicationServiceTests.java`、`tests/src/test/java/com/wind/funds/wallet/application/instrument/PaymentInstrumentCapabilityApplicationServiceTests.java`。 | `just test-one AuthorizationAdmissionApplicationServiceTests tests`、`just test-one AuthorizationAdmissionApplicationServiceTests,PaymentInstrumentCapabilityApplicationServiceTests,RouteSnapshotJsonSupportTests,FundsAuthorizationTransactionFlowTests tests`、`just compile`。 | `SERVICE_FLOW_GREEN_RECORDED_WITH_ROUTE_SNAPSHOT_BACKLINK`。 |
| Ledger 护栏 | `ledger-face` 账本、账本交易、分录和余额投影契约。 | `ledger/ledger-impl/src/main/java/com/wind/funds/ledger/LedgerNormalBalanceGuard.java`、`LedgerServiceImpl`、`DefaultLedgerTransactionPostingServiceImpl`、`LedgerBalanceProjectionServiceImpl`。 | `tests/src/test/java/com/wind/funds/ledger/impl/LedgerServiceImplTests.java`、`tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`、`tests/src/test/java/com/wind/funds/ledger/impl/LedgerBalanceProjectionServiceImplTests.java`。 | `just test-one DefaultLedgerTransactionPostingServiceImplTests,LedgerBalanceProjectionServiceImplTests,LedgerServiceImplTests tests`、`just test-ledger`、`just compile`、`just pmd`。 | `SERVICE_FLOW_GREEN_RECORDED_WITH_NORMAL_BALANCE_GUARD`。 |

证据消费规则：后续任一编码切片不得只引用本矩阵作为 Done 证据；必须先复跑与本切片相关的目标测试和必要分组测试，再结合 `git diff --check`、`compile`、`pmd` 和 Not Done 回写完成交付闭环。

### 11.4 结构门禁验证证据账本

本账本只证明活跃设计入口具备可消费的结构字段和交接信息，不证明 Java、测试、DDL/H2 schema、公共契约、运行时配置、Git 或生产发布已经获得授权或通过准出。

| 文档 | Harness 结构 | 产品结构 | 架构结构 | 结论 |
| --- | --- | --- | --- | --- |
| `docs/TDD设计/GSD-2-新基线工作流规划.md` | `check_harness_plan.py --kind gsd-wave` 通过。 | `check_product_deliverable.py --kind product-architecture` 通过。 | `check_architecture_deliverable.py --kind architecture-plan` 通过。 | GSD-2 总恢复入口可消费。 |
| `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` | `check_harness_plan.py --kind gsd-wave` 通过。 | `check_product_deliverable.py --kind product-architecture` 通过。 | `check_architecture_deliverable.py --kind architecture-plan` 通过。 | Goal 状态载体可消费。 |
| `docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` | `check_harness_plan.py --kind gsd-wave` 通过。 | `check_product_deliverable.py --kind product-architecture` 通过。 | `check_architecture_deliverable.py --kind architecture-plan` 通过。 | W5 推进计划可消费。 |
| `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md` | `check_harness_plan.py --kind cad-candidate` 通过。 | 未运行，原因是本轮只补工程准入包结构。 | 未运行，原因是本轮只补工程准入包结构。 | `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 已消费为 Green 证据。 |
| `docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md` | `check_harness_plan.py --kind gsd-wave` 通过。 | `check_product_deliverable.py --kind product-architecture` 通过。 | `check_architecture_deliverable.py --kind architecture-plan` 通过。 | contract/design-only 任务卡可消费。 |
| `docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` | `check_harness_plan.py --kind cad-candidate` 和 `--kind gsd-wave` 通过。 | `check_product_deliverable.py --kind product-architecture` 通过。 | `check_architecture_deliverable.py --kind architecture-plan` 通过。 | 确认包已被用户授权并完成 Green 验证，保留为本 Grant 审计和回放依据。 |

验证边界：本轮 B5-003 为 Java、测试和文档状态回写切片，已运行目标 B5 测试、余额控制、边界、交易分组、LWT Goal / W5 / GSD-2 状态一致性检索、`compile` 和 `pmd`；收口执行 `git diff --check`。

## 12. 停止条件

1. 需要越出本轮 B5-003 写入范围，继续写 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、状态机或运行时配置。
2. 需要 push、PR、merge、rebase、reset、分支切换或 Git 历史重写。
3. 发现当前文档与 Git HEAD、OpenSpec tasks 或已完成验证证据冲突，且无法用状态回写解释。
4. 发现支付工具、预算组、Spend Rule、父账户或投影被设计为 ledger subject。
5. 需要联网、依赖安装、生产配置、真实资金、外部规则、税务、会计、法务或合规最终确认。
6. 连续两轮没有新增证据、状态变化或缺口收敛；判定以第 1.2 节 `Loop Progress Ledger` 的无进展计数为准。
