# GSD-1 账本账目状态账本

## 1. 文档定位

本文档是 `GSD1-LEDGER-FACTS-CAD-001` 的 GSD 上下文账本、阶段状态和 handoff 入口，用于在会话中断、上下文压缩或下一轮继续推进时快速恢复。

本文档不授权修改生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。它只记录当前 GSD 状态、恢复入口、验证矩阵、交接要求和阻塞项。

## 2. currentState

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD1-LEDGER-FACTS-CAD-001` |
| 当前原子任务 | `GSD1-LD-RED-004A` |
| 任务目标 | 已消费 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`，完成 `GSD1-LD-RED-004A` 预算组兼容 guard；保留预算控制账本兼容路径，阻断 `BUDGET_GROUP` 扩大为直接交易、授权交易、出入金、退款、清结算或对账补事实的资金价值主体。 |
| 所属阶段 / Wave | GSD-1 / 账本账目 / Wave 2。 |
| Owner | `资深架构师` + Codex；用户负责确认后续新的 Execution Grant 或人工决策点。 |
| 阶段状态 | `WAVE_2_RED_004A_CONSUMED_SUMMARY_ONLY`。 |
| CAD 状态 | `004A_CONSUMED_SUMMARY_ONLY`。 |
| Git 策略 | `summary_only`，除非用户另行确认 `auto_commit`。 |
| 恢复入口 | 本文第 21 节、`docs/TDD设计/GSD-1-账本账目Wave1执行计划.md#8-handoff`、`docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md#10-handoff` 和 `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#35-agent-loop-engineering-契约`。 |
| 下一步 | 不能继续沿用 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 或 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`；下一轮若继续账本账目，必须重新确认新的单一 Grant，默认候选迁移为预算组 control ledger 退出条件、钱包账户/账户层级、交易内核或清结算对账中的一个切片。 |

## 3. sourceOfTruth

| 层级 | 入口 | 用途 |
| --- | --- | --- |
| Round 0 | `docs/TDD设计/GSD-1-账本账目Round0准入卡.md` | 记录账本账目产品语义、代码扫描、既有覆盖、首批 Red 决策和准入边界。 |
| Wave 1 | `docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | 记录原子任务包、写入范围、验证矩阵、Execution Grant 草案和 handoff。 |
| 阶段状态 | 本文档 | 记录当前状态、阻塞项、恢复入口和下一步。 |
| 代码库理解 | `docs/TDD设计/GSD-1-账本账目代码库理解结论包.md` | 记录真实代码入口、调用关系、现有覆盖、Red 输入、验证策略和残余风险。 |
| Grant 确认 | `docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md` | 记录可评审授权文本、首轮执行顺序、验收种子、人工确认点和 handoff。 |
| OpenSpec | `openspec/project.md` | 记录当前 Source of Truth 和编码授权边界。 |
| Harness | `openspec/changes/tdd-baseline-reset/tasks.md` | 记录任务账本、任务优先级和批次授权口径。 |
| TDD 索引 | `docs/TDD设计/README.md` | 记录 GSD-1 文档阅读顺序。 |

## 4. decisionRecord

| 日期 | 决策 | 影响 |
| --- | --- | --- |
| 2026-06-04 | 当前任务优先级固定为账本账目 > 钱包 > 交易层；支付工具、VCC、全球账户支持放到最后；收单只做设计。 | GSD/CAD 自动推进必须优先恢复账本账目，不越级进入支付工具或 P2 业务专项。 |
| 2026-06-04 | `DefaultLedgerTransactionPostingServiceImpl` 缺少独立目标测试类。 | 首批 Red 收敛为 `GSD1-LD-RED-001A`，目标测试为 `DefaultLedgerTransactionPostingServiceImplTests`。 |
| 2026-06-04 | `LedgerBalanceProjectionServiceImplTests` 已覆盖事件非事实源和整批失败无半截投影。 | `GSD1-LD-RED-002` 父项和 `GSD1-LD-RED-003` 暂作为后续强化；001A/001B 完成后，当前只拆出 `GSD1-LD-RED-002A` 入账编排入口三细场景。 |
| 2026-06-04 | `BUDGET_GROUP` 仍在可入账主体白名单内。 | `GSD1-LD-RED-004` 是人工确认点，未确认兼容策略前不得写 Green。 |
| 2026-06-04 | Wave 1 已形成原子任务包和验证矩阵。 | GSD 可继续；CAD 仍缺 Execution Grant。 |
| 2026-06-04 | 代码库理解结论包已把入账编排、持久层、投影层和既有测试资产接入同一恢复入口。 | 当时可直接从 `GSD1-LD-RED-001A` 的 Red 输入恢复，不需要重新扫描全仓；001A 完成后该入口已迁移到 001B。 |
| 2026-06-04 | `Execution Grant：GSD1-LEDGER-FACTS` 确认卡已形成。 | 该 Grant 后续已被 001A 消费；不再作为当前待确认授权。 |
| 2026-06-04 | `GSD1-LD-RED-001A` 测试驱动设计口径已补入 Execution Grant 确认卡。 | 该口径已被 001A 测试实现消费；001B 当前使用新的重复入账幂等测试口径。 |
| 2026-06-04 | AI Native 研发流程编排已重新进入 GSD 并完成门禁复核。 | 当前仍只满足 GSD 继续和 CAD 候选，不满足 Execution Grant；本轮保持 `summary_only`，只维护状态账本、Grant 确认卡、验证矩阵和索引。 |
| 2026-06-04 | 用户已确认并消费 `Execution Grant：GSD1-LEDGER-FACTS`，`GSD1-LD-RED-001A` 已落地当前工作树的 `DefaultLedgerTransactionPostingServiceImplTests`。 | 001A 只补目标测试，不改生产代码；专项测试 6 tests 通过，证明非法状态、空 entry、非法金额、流水不一致、缺 ledgerId 和账本绑定不一致均无半截账务事实；当前测试文件未被 Git 跟踪的事实见第 16 节。 |
| 2026-06-04 | Wave 1 下一候选切到 `GSD1-LD-RED-001B`。 | 新 Grant 候选只覆盖同一 ledger transaction 重复 post 的幂等/投影跳过证据；未确认 `Execution Grant：GSD1-LEDGER-IDEMPOTENCY` 前不写 Java 或测试。 |
| 2026-06-05 | 用户已确认并消费 `Execution Grant：GSD1-LEDGER-IDEMPOTENCY`，`GSD1-LD-RED-001B` 已落地当前工作树目标测试。 | 001B 只补目标测试和必要测试夹具，不改生产代码；专项测试 7 tests 通过，证明重复 post 后 ledger transaction、posting plan、ledger entry 和 ledger balance 均不重复变化；当前测试文件未被 Git 跟踪的事实见第 16 节。 |
| 2026-06-05 | Wave 1 的 001A/001B 已在当前工作树完成覆盖补齐。 | 后续继续 GSD 时不得沿用 001B 授权；必须在账本账目范围内重新确认下一单一 Execution Grant 或人工决策点；不得把当前工作树测试文件写成已冻结 Git 基线。 |
| 2026-06-05 | 只读扫描后下一候选收敛为 `GSD1-LD-RED-002A`。 | `GSD1-LD-RED-003` 在投影层已有较强覆盖，暂作为回归参考；`GSD1-LD-RED-004` 仍需 `BUDGET_GROUP` 兼容策略人工确认；当前只准备 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`，不写 Java。 |
| 2026-06-07 | 运行时 Goal 延续后完成 002A 授权前源码锚点复核。 | `DefaultLedgerTransactionPostingServiceImpl#assertEntryMatchesLedger` 已有账目、币种和 `ALLOW_NEGATIVE` guard；当前缺口收敛为目标测试未证明 002A 三个细场景。若授权后 Red 直接 Green，只登记覆盖补齐，不改生产代码。 |
| 2026-06-07 | 本轮对 002A 授权前材料做准入洁净化。 | 已把残留的 001B/重复入账/交易装配器口径收敛为 002A 的绑定账本细约束，不改变写入范围、不新增编码授权。 |
| 2026-06-07 | 完成 002A 授权前工作树审计。 | `DefaultLedgerTransactionPostingServiceImplTests.java` 当前存在于工作树但未被 Git 跟踪；它可作为当前工作树事实和后续编辑基底，不能被写成已冻结 Git 基线。002A 授权后应先保护既有 001A/001B 测试，再扩展测试夹具构造账目 code/category mismatch、ledger currency mismatch 和 `ALLOW_NEGATIVE` mismatch。 |
| 2026-06-07 | 用户确认并消费 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`。 | `GSD1-LD-RED-002A` 已在当前工作树目标测试中补齐三条绑定账本细约束用例；目标测试 10 tests 通过，未修改生产代码，登记为覆盖补齐。 |
| 2026-06-07 | 用户确认账本下一决策并消费 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION`。 | 本轮选择低风险 `GSD1-LD-RED-003` 投影强化回归，不触碰 `BUDGET_GROUP`；既有 `LedgerBalanceProjectionServiceImplTests` 已覆盖 003 五类红线，目标测试 5 tests / 0 failures / 0 errors，未修改生产代码或测试代码。 |
| 2026-06-07 | 完成 `GSD1-LD-RED-004` 预算组兼容策略准备。 | 推荐 `COMPAT_CONTROL_LEDGER_WITH_FREEZE`：迁移期保留 `BUDGET_GROUP` 预算控制账本兼容路径，但冻结为控制语义，不得扩大成支付、授权、出入金、清结算或对账补事实的资金价值主体；下一步建议确认 004A guard Grant。 |
| 2026-06-11 | GSD + Goal 续跑完成 004A 授权前源码锚点复核。 | 当时尚未确认新的单一 Execution Grant；只读复核确认 `DefaultLedgerTransactionPostingServiceImpl` 仍保留 `BUDGET_GROUP` 入账兼容白名单，`BalanceControlFundsInstructionRouteResolver` 仍保留预算额度控制路径，支付工具绑定和预算组控制账本测试已分别保护“预算组不是真实资金主体”和“预算组控制账本兼容”。该复核随后由 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 授权消费闭合。 |
| 2026-06-11 | 用户确认并消费 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`。 | `GSD1-LD-RED-004A` 已补直接交易 topup / transfer / pay 和授权交易的 `BUDGET_GROUP` 拒绝用例，最小 guard 落在 transaction converter 层；`FundsBalanceControlFailureFlowTests` 回归证明预算组控制账本兼容路径未被破坏。验证：`FundsDirectTransactionFlowTests` 47 tests 通过，`FundsAuthorizationTransactionFlowTests` 29 tests 通过，`FundsBalanceControlFailureFlowTests` 19 tests 通过，`just compile` 通过，`git diff --check` 通过。 |

## 5. scope

| 类型 | 范围 |
| --- | --- |
| 写入范围 | `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 已消费；本轮只允许 004A 目标测试、transaction converter 最小 guard、测试支撑断言和状态文档回写。后续只允许写 GSD 文档、OpenSpec/Harness 索引和交接记录，直到用户重新确认新的单一 Grant。 |
| 写入文件 | 004A 写入 `FundsDirectTransactionFlowTests`、`FundsAuthorizationTransactionFlowTests`、`FundsTransactionFlowTestSupport`、`FundsDirectTransactionInstructionConverter` 和 `FundsAuthorizationInstructionConverter`；未写 DDL/H2 schema、公共契约、Entity、Mapper、钱包、支付工具、VCC、清结算对账或运行时配置。002A 写入的 `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java` 当前仍未被 Git 跟踪；后续任何 Java、测试、DDL/H2 schema、公共契约或运行时配置写入都必须重新授权，并先保护该未跟踪测试文件中的既有 001A/001B/002A 覆盖。 |
| 只读范围 | 只读参考包括 `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、账本账目相关生产代码和测试资产。 |
| 只读参考 | `DefaultLedgerTransactionPostingServiceImpl`、`LedgerTransactionServiceImplTests`、`LedgerBalanceProjectionServiceImplTests`、`DefaultLedgerPostingAssemblerTests`、`AbstractFundsServiceTest`、`FundsTransactionFlowTestSupport`。 |
| 禁止事项 | 不改 `ledger-face`、`core` 公共契约、枚举、DTO、Request、DDL/H2 schema、`LedgerTransactionService` 摘要冲突语义、钱包、交易层新业务语义、支付工具、VCC、全球账户、收单、清结算对账、governance apply、生产配置或敏感数据处理。 |

## 6. verificationMatrix

| 阶段 | 验证命令 | 预期证据 |
| --- | --- | --- |
| GSD 文档结构 | `check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | Wave1 原子任务包结构完整。 |
| CAD 候选结构 | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | CAD 候选字段完整，但仍缺 Execution Grant。 |
| 文档空白 | `git diff --check`、`rg -n "[[:blank:]]+$|\r$" <GSD docs>` | 文档没有行尾空白或 CR。 |
| 编码前运行时 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version` | Java 21 runtime 正确。 |
| 编码前编译 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` | Maven reactor 编译通过。 |
| 已完成覆盖 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` | 当前工作树中的 `GSD1-LD-RED-001A`、`GSD1-LD-RED-001B` 和 `GSD1-LD-RED-002A` 专项通过，10 tests / 0 failures / 0 errors；目标测试文件当前未被 Git 跟踪。 |
| 投影强化回归 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one LedgerBalanceProjectionServiceImplTests tests` | `GSD1-LD-RED-003` 既有覆盖已回归通过，5 tests / 0 failures / 0 errors；未写生产代码或测试代码。 |
| 004A BudgetGroup 兼容 guard | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsDirectTransactionFlowTests tests`；`WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsAuthorizationTransactionFlowTests tests`；`WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsBalanceControlFailureFlowTests tests` | 直接交易 47 tests、授权交易 29 tests、余额控制失败流 19 tests 均通过；证明资金价值交易拒绝 `BUDGET_GROUP`，预算组控制账本兼容路径仍可用。 |
| 回归 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-ledger` | 账本服务、posting 装配、账本交易和余额投影回归通过。 |
| 提交前 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just pmd`、`git diff --check` | 规约和空白检查通过。 |

## 7. handoffs

| 场景 | 交接要求 |
| --- | --- |
| 继续 GSD 但未授权新编码 | 更新本文档、Round0 卡、Wave1 执行计划、OpenSpec project 或 Harness tasks；不得写 Java、测试或 schema。 |
| 004A 已完成后继续 | 不能沿用 `GSD1-LEDGER-BOUND-LEDGER`、`GSD1-LEDGER-PROJECTION-REGRESSION` 或 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`；下一轮需重新确认新的单一 Grant，候选可在预算组 control ledger 退出条件、钱包账户/账户层级、交易内核、清结算对账等范围内选择。 |
| Red 直接通过 | 001B 和 002A 均已按该口径登记为覆盖补齐，不硬改生产代码；后续 Red 直接 Green 时仍只登记覆盖补齐并停止生产改动。 |
| Red 失败且证明真实缺口 | 只做 `DefaultLedgerTransactionPostingServiceImpl` 最小 Green；不得扩展到公共契约、DDL、钱包、交易层新能力或 `LedgerTransactionService` 摘要冲突语义。 |
| 触发人工确认点 | 预算组兼容策略、公共契约、表结构、跨能力域、外部规则、敏感数据或无法表达资金不变量时停止。 |

## 8. executionGrantGate

```text
已消费：Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION；Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD
已执行：GSD1-LD-RED-003；GSD1-LD-RED-004A
当前候选：无；下一轮必须重新确认新的单一 Execution Grant
CAD 状态：004A_CONSUMED_SUMMARY_ONLY
当前阻塞：004A 已消费，不得沿用本 Grant 继续写 Java、测试、DDL/H2 schema、公共契约或运行时配置
最近复核：当前工作树中的 001A、001B、002A、003、004A 均已有专项验证证据；004A 额外完成 direct/auth/balance-control 目标回归与 compile
后续剩余允许动作：GSD 状态账本、交接、验证矩阵、索引同步和下一 Grant 决策准备
授权后禁止动作：沿用 002A、003 或 004A 授权继续写 Java、测试、DDL/H2 schema、公共契约、运行时配置或 Git 提交
```

## 9. gsdConclusion

当前 GSD 模式可以继续，且已完成五个授权消费：`GSD1-LD-RED-001A` 已在当前工作树补齐入账编排非法输入目标测试，`GSD1-LD-RED-001B` 已补齐重复入账幂等目标测试，`GSD1-LD-RED-002A` 已补齐绑定账本账目、币种和负余额约束目标测试，`GSD1-LD-RED-003` 已完成余额投影强化回归登记，`GSD1-LD-RED-004A` 已完成 BudgetGroup 兼容 guard。001A/001B/002A 专项测试、003 目标回归和 004A direct/auth/balance-control 回归均通过。002A 目标测试文件当前未被 Git 跟踪，因此这些证据只能作为当前工作树证据和后续编辑基底，不能写成已冻结 Git 基线。004A 已按 `COMPAT_CONTROL_LEDGER_WITH_FREEZE` 固化迁移期边界：预算组可保留控制账本兼容路径，但不得作为资金价值交易主体。下一轮如果仍只说“继续”或“推进”，必须先确认新的单一 Execution Grant；不得沿用 `GSD1-LEDGER-BOUND-LEDGER`、`GSD1-LEDGER-PROJECTION-REGRESSION` 或 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 继续编码。

## 9.1 aiNativeAdmissionPackage

本节按 `ai-native-engineering-workflow` 的 GSD/CAD 编排准入模板固化一页式判断，供下一轮直接恢复，不替代 `Execution Grant`。

| 项 | 当前判断 |
| --- | --- |
| GSD/CAD 编排准入结论 | 可继续 GSD；Wave 1 的 001A/001B 和 Wave 2 的 002A 已在当前工作树完成覆盖补齐，目标测试文件当前未被 Git 跟踪；后续不能在没有新 Grant 的情况下继续 CAD 执行。 |
| 输入成熟度 | `OpenSpec + Harness + Round0 card + Wave1 atomic task + code understanding package + Grant confirmation card`。 |
| 是否需要 GSD | 是；当前最高优先级仍是账本账目，需要上下文账本、恢复入口和验证矩阵维持跨轮一致。 |
| GSD Round 0 缺口 | Round 0 已补齐；当前缺口不在产品/工程上下文，而在用户确认的 Execution Grant。 |
| 建议 Wave | Wave 2 的 `GSD1-LD-RED-002A` 已完成覆盖补齐；下一轮先做账本下一决策，不推进钱包、交易层、支付工具、VCC、全球账户或收单实现。 |
| Atomic Task 候选 | 无当前候选；下一轮需在预算组 control ledger 退出条件、钱包账户/账户层级、交易内核、清结算对账等范围中重新确认一个新的单一 Grant。 |
| CAD 候选缺口 | `GSD1-LEDGER-BOUND-LEDGER`、`GSD1-LEDGER-PROJECTION-REGRESSION` 和 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 均已消费；当前没有可沿用的编码 Grant。 |
| Execution Grant 缺口 | 001A、001B、002A、003、004A 授权均已消费；下一轮需重新确认 Task ID、写入范围、验证范围、Git 策略、禁止事项、人工确认点和停止条件。 |
| 质量/测试门禁 | 当前工作树中的 001A/001B 专项测试已通过；后续新 Grant 仍需先运行 Java 21 和编译检查，再执行目标 `test-one`，必要时补 `just test-ledger`。 |
| 代码库理解结论包 | 已形成，恢复入口为 `GSD-1-账本账目代码库理解结论包.md#8-reddesigninput`。 |
| 下一步 owner | `资深架构师` + Codex 准备账本下一决策；用户确认新的单一 Execution Grant 或调整优先级。 |
| 停止条件 | 需要公共契约、表结构、预算组兼容策略、跨能力域、外部规则、敏感数据、验证无法表达资金不变量、工具权限失败无法降级或工作树冲突时停止。 |
| 路由 | AI Native 只保留准入和交接；工程执行继续交给 `资深架构师` 的 CAD Mode、TDD 和 Review 纪律。 |

### 9.1.1 agentLoopHandoff

本节记录进入 Agent Loop Engineering 后的最小循环契约。该 Loop 只允许维护状态载体和交接证据，不授权继续写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

| 字段 | 当前值 |
| --- | --- |
| Loop ID | `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-LOOP-2026-06-11` |
| Loop 状态 | `LOOP_ACTIVE_DOCS_ONLY_NEEDS_NEW_SINGLE_GRANT` |
| 关联 Goal | `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-2026-06-07` |
| 读取顺序 | 先读本文 currentState、executionGrantGate 和 verificationEvidence，再读 Goal 计划、Wave1 执行计划、OpenSpec project 与 Harness tasks。 |
| 允许动作 | 同步状态、索引、验证矩阵、handoff 和下一 Grant 候选；运行文档结构、空白和 diff 校验。 |
| 禁止动作 | 不写生产代码、测试代码、DDL/H2 schema、公共契约、运行时配置、Git add/commit/push、联网或依赖安装。 |
| 反馈源 | Harness checker、`rg` 一致性扫描、`git status --short`、`git diff --check`、用户确认和后续目标测试结果。 |
| 无进展检测 | 连续 2 轮只重复“需要新 Grant”且无新增事实、验证或候选收敛时暂停。 |
| 失败回写 | 回写本文第 8 节、第 9 节、本节、Goal 计划 3.5、OpenSpec tasks 9.2；若涉及候选任务，回写对应 Round0 准入卡。 |
| 恢复入口 | 重新确认新的单一 Execution Grant；默认候选仍限定在预算组 control ledger 退出条件、钱包账户/账户层级、交易内核或清结算对账之一。 |

## 9.2 preGrantSaturation

本节记录授权前材料饱和度，防止 GSD 自动推进在缺少 Execution Grant 时继续低价值扩写文档。

| 检查项 | 结论 |
| --- | --- |
| Round 0 | 已形成 `GSD-1-账本账目Round0准入卡.md`，任务优先级、非目标、Red 候选和禁止范围清楚。 |
| Wave 1 | 已形成 `GSD-1-账本账目Wave1执行计划.md`，原子任务、验证矩阵、恢复入口和 Grant 草案清楚。 |
| 代码库理解 | 已形成 `GSD-1-账本账目代码库理解结论包.md`，源码锚点、调用关系、既有覆盖和 Red 输入清楚。 |
| 002A 源码锚点复核 | 已在 `GSD-1-账本账目代码库理解结论包.md#51-sourceanchorreview2026-06-07` 补充：生产实现 guard 可能已满足 002A，缺口主要是目标测试未证明。 |
| Execution Grant 确认 | 已形成并消费 001A Grant、001B Grant 和 002A Grant；后续不能沿用旧 Grant。 |
| 一页式确认摘要 | 已在 `GSD-1-账本账目ExecutionGrant确认卡.md#12-consumedgrantonepageconfirmation2026-06-07` 记录 002A 的确认摘要、执行范围、验证结果和消费状态。 |
| OpenSpec/Harness/TDD 索引 | 已同步到 `docs/TDD设计/README.md`、`openspec/project.md` 和 `openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 继续前可做事项 | 修正事实错误、同步索引漂移、补充验证证据、响应用户新增约束，或在用户确认新 Grant 后进入下一轮 TDD/CAD。 |
| 不再扩写事项 | 不再为 001B 新增计划、候选卡、测试设计表或代码库扫描包；不再重复刷新同一门禁结论。 |
| 下一动作 | 004A 已完成 BudgetGroup 兼容 guard；下一轮必须重新确认新的单一 Execution Grant，不能沿用本轮 Grant。 |
| 当前状态 | `WAVE_2_RED_004A_CONSUMED_SUMMARY_ONLY`。 |

## 10. verificationEvidence2026-06-04

本节记录 001A 授权前 GSD 文档和交接材料的轻量验证证据。该轮仅修改文档和 OpenSpec/Harness 索引，未运行 Maven 编译或测试；001A 授权消费和后续 001B 迁移证据见第 11 节。

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目代码库理解结论包.md` | PASS | 代码库理解结论包具备 Task、Owner、写入/只读范围、执行顺序、验证命令、停止条件和 handoff 字段。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md` | PASS | Execution Grant 确认卡具备 CAD 候选结构字段。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md` | PASS | Execution Grant 确认卡具备轻量 Harness 字段。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目状态账本.md` | PASS | 状态账本结构仍完整。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | Wave1 原子任务包结构仍完整。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | CAD 候选字段结构仍完整，但仍缺用户确认的 Execution Grant。 |
| `git diff --check` | PASS | 已跟踪文档 diff 无空白错误。 |
| `rg -n "[[:blank:]]+$|\r$" docs/TDD设计/GSD-1-账本账目Round0准入卡.md docs/TDD设计/GSD-1-账本账目Wave1执行计划.md docs/TDD设计/GSD-1-账本账目状态账本.md docs/TDD设计/GSD-1-账本账目代码库理解结论包.md docs/TDD设计/README.md openspec/project.md openspec/changes/tdd-baseline-reset/tasks.md` | PASS | GSD 文档、TDD 索引和 OpenSpec/Harness 索引无行尾空白或 CR。 |

结论：该轮 docs-only GSD 编排材料、Execution Grant 确认卡和测试驱动设计口径通过结构和空白检查。当时由于尚未确认 `Execution Grant：GSD1-LEDGER-FACTS`，未运行 `just compile`、`just test-one`、`just test-ledger` 或 `just pmd`。

## 11. verificationEvidence2026-06-04-001AAnd001BMigration

本节记录 `GSD1-LD-RED-001A` 授权消费后的验证事实，以及当前迁移到 `GSD1-LD-RED-001B` 的轻量验证计划。

| 项 | 结果 | 说明 |
| --- | --- | --- |
| `GSD1-LD-RED-001A` 实现范围 | PASS | 新增 `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`；未修改生产代码。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version` | PASS | Java 21 runtime 与 Maven 入口可用。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` | PASS | Maven reactor 编译通过。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` | PASS | 目标测试 6 tests / 0 failures / 0 errors；沙箱内曾因 embedded Redis 本地端口绑定受限失败，提升权限后通过。 |
| 001B 迁移范围 | PASS | 当前仅迁移 GSD 文档、OpenSpec/Harness/TDD 索引和 Grant 候选；未进入新 Java/测试写入。 |
| `check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目状态账本.md` | PASS | 状态账本结构完整。 |
| `check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | Wave1 原子任务包结构完整。 |
| `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | Wave1 CAD 候选结构完整。 |
| `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md` | PASS | 001B Execution Grant 确认卡结构完整。 |
| `check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目代码库理解结论包.md` | PASS | 代码库理解结论包结构完整。 |
| `git diff --check` | PASS | 当前 diff 无空白错误。 |
| `rg -n "[[:blank:]]+$|\r$" <GSD/OpenSpec/Harness docs>` | PASS | 当前 GSD 文档、TDD 索引、OpenSpec project 和 Harness tasks 无行尾空白或 CR。 |

结论：001A 已完成覆盖补齐并通过专项验证；当前阻塞点已迁移为 `Execution Grant：GSD1-LEDGER-IDEMPOTENCY`。本次 001B 迁移是 docs-only 状态修正，不运行 Maven 编译、专项测试或 PMD，只执行文档结构、空白和 diff 检查。

## 12. verificationEvidence2026-06-05-001B

本节记录 `GSD1-LD-RED-001B` 授权消费后的验证事实。该轮只修改目标测试资产和必要测试夹具，未修改生产代码、公共契约、DDL/H2 schema、Mapper 或运行时配置。

| 项 | 结果 | 说明 |
| --- | --- | --- |
| `Execution Grant：GSD1-LEDGER-IDEMPOTENCY` | CONSUMED | 用户已明确确认并授权执行 `GSD1-LD-RED-001B`。 |
| `GSD1-LD-RED-001B` 实现范围 | PASS | 在 `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java` 中新增重复入账幂等目标测试；未修改生产代码。 |
| 测试断言 | PASS | 第一次 post 后新增一组 ledger transaction、posting plan 和 ledger entry；第二次 post 后账本事实快照保持不变，ledger balance 不重复变化。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version` | PASS | Java 21 runtime 与 Maven 入口可用。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` | PASS | Maven reactor 编译通过。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` | PASS | 目标测试 7 tests / 0 failures / 0 errors；001B 在现有生产实现下直接 Green，登记为覆盖补齐。 |
| `git diff --check` | PASS | 当前已跟踪 diff 无空白错误。 |
| 沙箱说明 | N/A | 沙箱内目标测试曾因 embedded Redis 本地端口绑定受限失败，提升权限后通过；该失败判定为工具/沙箱限制而非代码失败。 |

结论：`GSD1-LD-RED-001B` 已闭合。后续继续 GSD 时必须选择新的账本账目单一候选；不得沿用 `GSD1-LEDGER-IDEMPOTENCY` 继续写代码。当前单一候选已从 `GSD1-LD-RED-002` 父项收敛为 `GSD1-LD-RED-002A`；`GSD1-LD-RED-003` 可作为投影强化候选，`GSD1-LD-RED-004` 涉及 `BUDGET_GROUP` 可入账主体兼容策略，必须先由用户确认。

## 13. verificationEvidence2026-06-05-002ASelection

本节记录 AI Native GSD 模式重新推进后的 docs-only 验证证据。本轮只收敛下一候选 `GSD1-LD-RED-002A` / `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`，未写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目状态账本.md` | PASS | 状态账本结构完整。 |
| `check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | Wave 计划具备 Task、Owner、范围、Wave 边界、上下文账本、约束、验证和 handoff。 |
| `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | 002A 具备 CAD 候选结构，但仍缺用户确认的 Execution Grant。 |
| `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md` | PASS | Grant 确认卡具备候选授权结构。 |
| `check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目代码库理解结论包.md` | PASS | 代码库理解结论包结构完整。 |
| `check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目Round0准入卡.md` | PASS | Round0 准入卡结构完整。 |
| `git diff --check -- <本轮 GSD/OpenSpec 文档>` | PASS | 本轮已跟踪文档 diff 无空白错误。 |
| `rg -n "[[:blank:]]+$|\r$" <本轮 GSD/OpenSpec 文档>` | PASS | 本轮 GSD 文档、TDD 索引和 OpenSpec/Harness 索引无行尾空白或 CR。 |

结论：GSD 可继续，下一候选已从开放式 `002/003/004` 收敛为 `GSD1-LD-RED-002A`。当前仍不是 CAD 编码状态；只有用户确认 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 后，才允许进入 Red/Green 编码闭环。

## 14. verificationEvidence2026-06-07-runtimeGoalPreGrantValidation

本节记录运行时 Goal 延续后的授权前真实验证证据。本轮未写 Java、测试、DDL/H2 schema、公共契约或运行时配置；验证只证明当前已存在的 001A/001B 目标测试套件和 reactor 编译基线，不证明 002A 已完成。

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version` | PASS | Maven 3.6.3 使用 Amazon Corretto 21.0.11，Java runtime 符合项目要求。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` | PASS | Maven reactor 14/14 modules `BUILD SUCCESS`，当前未提交基线没有破坏编译。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` | FAIL_ENV | 沙箱内首次执行因 embedded Redis 测试配置绑定本机端口失败，根因为 `java.net.SocketException: Operation not permitted`，判定为工具/沙箱限制，不作为业务断言失败。 |
| `/bin/zsh -lc 'WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests'` | PASS | 提权复跑后目标测试 7 tests / 0 failures / 0 errors，reactor `BUILD SUCCESS`。 |

结论：当前账本入账编排目标测试资产可真实运行，001A/001B 既有覆盖仍通过；`GSD1-LD-RED-002A` 仍处于 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`，需要用户确认 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 后才能写新 Red 或条件最小实现。

## 15. verificationEvidence2026-06-07-preGrantReadOnlyRefresh

本节记录 Goal 继续推进后的 002A 授权前只读复核。本轮只校正恢复入口、复核源码锚点和补充当前可复跑的 Harness 脚本路径；未写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `ls -l /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py` | PASS | 当前 Harness checker 入口来自 `资深架构师` Skill 目录，不是仓库根目录 `scripts/`。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目状态账本.md` | PASS | 状态账本结构完整。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | Wave1 执行计划具备 GSD Wave 结构。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | Wave1 仍具备 CAD 候选结构，但不等于用户已确认 Execution Grant。 |
| `git diff --check -- docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | 本轮锚点修复无空白错误。 |
| `rg -n "[[:blank:]]+$|\r$" docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | 本轮锚点修复无行尾空白或 CR。 |

结论：002A 当前材料可继续作为下一编码候选，但仍只处于 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。若用户未明确确认 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`，后续仍只能维护 GSD 文档、OpenSpec/Harness 索引、验证矩阵和交接记录；不能写新 Red、生产实现、DDL/H2 schema、公共契约或运行时配置。

## 16. verificationEvidence2026-06-07-preGrantWorktreeAudit

本节记录 002A 授权前对当前工作树的只读审计。本轮只读取 Git 状态、目标测试文件和入账编排源码；未写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git ls-files --error-unmatch tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java` | FAIL_NOT_TRACKED | 目标测试类当前不是 Git 已跟踪文件，不能当成已冻结 Git 基线。 |
| `git ls-files --others --exclude-standard -- tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java` | PASS | 目标测试类存在于当前工作树，后续授权执行时必须接续并保护该文件中的 001A/001B 测试。 |
| `DefaultLedgerTransactionPostingServiceImpl#assertEntryMatchesLedger` | PASS | 生产实现已校验 subject、ledger subject code/category、currency，以及 entry `ALLOW_NEGATIVE` 与 ledger `allowNegative` 的兼容性。 |
| `DefaultLedgerTransactionPostingServiceImplTests#TestLedgerEntrySpec` | GAP_FOR_002A | 当前测试夹具固定 `AVAILABLE/ASSET`，币种默认来自 `Money`，且未覆盖 `getBalanceConstraintType`；002A 授权后需先最小扩展夹具或 helper，构造三条绑定账本细约束 Red。 |
| 本轮 Maven / 目标测试 | NOT_RUN_DOCS_ONLY | 本轮未获 Execution Grant，不运行 Red/Green 编码闭环；不把既有 7 tests 运行证据扩展解释为 002A 完成。 |

结论：002A 仍是下一默认候选，但当前工作树存在一个未跟踪测试文件这一事实必须进入下一轮开工前检查。若用户确认 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`，第一步不是新增生产实现，而是在当前测试文件中保护 001A/001B 覆盖并补 002A 的三条目标 Red；Red 直接 Green 时登记覆盖补齐，Red 证明真实缺口时才最小修复入账编排实现。

## 17. verificationEvidence2026-06-07-preGrantGitBaselineWording

本节记录 GSD + Goal 继续推进后的 Git 基线口径修正。本轮只修正文档、OpenSpec/Harness 索引和交接记录中关于 001A/001B 的表述；未写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| 文档口径修正 | PASS | 已在 `GSD-Goal-生产可用MVP推进计划.md`、`GSD-1-账本账目Round0准入卡.md`、`GSD-1-账本账目Wave1执行计划.md`、`GSD-1-账本账目ExecutionGrant确认卡.md` 和 `openspec/changes/tdd-baseline-reset/tasks.md` 中补充“当前工作树证据、目标测试文件未被 Git 跟踪、不是已冻结 Git 基线”的边界。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目状态账本.md` | PASS | 状态账本结构完整。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | PASS | Wave1 执行计划结构完整。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md` | PASS | Grant 确认卡结构完整。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` | PASS | Goal 推进计划结构完整。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目代码库理解结论包.md` | PASS | 代码库理解结论包结构完整。 |
| `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目Round0准入卡.md` | PASS | Round0 准入卡结构完整。 |
| `git diff --check -- <本轮 GSD/OpenSpec 文档>` | PASS | 本轮文档 diff 无空白错误。 |
| `rg -n "[[:blank:]]+$|\r$" <本轮 GSD/OpenSpec 文档>` | PASS | 本轮 GSD 文档、TDD 索引和 OpenSpec/Harness 索引无行尾空白或 CR。 |
| 本轮 Maven / 目标测试 | NOT_RUN_DOCS_ONLY | 本轮未获新的 Execution Grant，不运行 Red/Green 编码闭环；不把既有 7 tests 运行证据扩展解释为 002A 完成。 |

结论：该轮之后用户已确认并消费 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`，002A 已完成覆盖补齐。该结论随后由第 19 节和第 21 节更新：003 已完成投影强化既有覆盖回归登记，004A 已完成 BudgetGroup 兼容 guard。下一轮若继续编码，必须重新确认新的单一 Grant，并先复核 `git status --short`，保护当前未跟踪的 `DefaultLedgerTransactionPostingServiceImplTests.java` 中 001A/001B/002A 覆盖；若只说“继续推进”，默认只做 Agent Loop 状态同步、索引修正和下一 Grant 决策准备。

## 18. verificationEvidence2026-06-07-002A

本节记录 `GSD1-LD-RED-002A` 授权消费后的验证事实。该轮只修改目标测试资产和必要测试夹具，未修改生产代码、公共契约、DDL/H2 schema、Mapper 或运行时配置。

| 项 | 结果 | 说明 |
| --- | --- | --- |
| `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` | CONSUMED | 用户已明确确认并授权执行 `GSD1-LD-RED-002A`。 |
| `GSD1-LD-RED-002A` 实现范围 | PASS | 在 `DefaultLedgerTransactionPostingServiceImplTests` 中新增账目 code/category mismatch、currency mismatch、`ALLOW_NEGATIVE` 与 bound ledger `allowNegative=false` 三条目标测试；仅扩展测试 helper 和 `TestLedgerEntrySpec`。 |
| 生产代码 | UNCHANGED | `DefaultLedgerTransactionPostingServiceImpl#assertEntryMatchesLedger` 现有守卫已满足 002A；Red 直接 Green，登记为覆盖补齐。 |
| 沙箱目标测试 | FAIL_ENV | 沙箱内首次执行因 embedded Redis 测试配置绑定本机端口失败，根因为 `java.net.SocketException: Operation not permitted`，判定为工具/沙箱限制。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` | PASS | 提权复跑后目标测试 10 tests / 0 failures / 0 errors，reactor `BUILD SUCCESS`。 |
| Git 策略 | SUMMARY_ONLY | 未执行 `git add` 或 `git commit`；目标测试文件当前仍未被 Git 跟踪。 |

结论：`GSD1-LD-RED-002A` 已闭合。后续不得沿用 `GSD1-LEDGER-BOUND-LEDGER` 继续写代码；该结论随后由第 19 节和第 21 节更新为：`GSD1-LD-RED-003` 已闭合为既有覆盖回归登记，`GSD1-LD-RED-004A` 已闭合为 BudgetGroup 兼容 guard。下一步必须重新确认新的单一 Grant，或切换到钱包账户/账户层级、交易内核、清结算对账等候选。

## 19. verificationEvidence2026-06-07-003

本节记录 `GSD1-LD-RED-003` 授权消费后的验证事实。该轮未修改生产代码、测试代码、公共契约、DDL/H2 schema、Mapper 或运行时配置；执行结果为既有覆盖回归登记。

| 项 | 结果 | 说明 |
| --- | --- | --- |
| `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` | CONSUMED | 用户确认账本下一决策后，本轮低风险选择 `GSD1-LD-RED-003`；不触碰 `BUDGET_GROUP`。 |
| `GSD1-LD-RED-003` 实现范围 | PASS | 既有 `LedgerBalanceProjectionServiceImplTests` 已覆盖事件发布失败不回滚余额事实、事件携带来源分录证据、嵌套上下文不可被外部回写污染、核心权益字段投影前阻断、后一个余额桶失败时整批不写。 |
| 生产代码 | UNCHANGED | `LedgerBalanceProjectionServiceImpl` 现有实现满足本轮回归；未触发最小修复。 |
| 测试代码 | UNCHANGED | 目标回归使用既有测试资产；未新增或修改测试。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one LedgerBalanceProjectionServiceImplTests tests` | PASS | 目标测试 5 tests / 0 failures / 0 errors，reactor `BUILD SUCCESS`。 |
| Git 策略 | SUMMARY_ONLY | 未执行 `git add` 或 `git commit`。 |

结论：`GSD1-LD-RED-003` 已闭合。后续不得沿用 `GSD1-LEDGER-PROJECTION-REGRESSION` 继续写代码；下一步若继续账本账目，必须先确认 `GSD1-LD-RED-004` 的 `BUDGET_GROUP` 兼容策略，或切换到其他新的单一 Grant。

## 20. verificationEvidence2026-06-11-004APreGrantResume

本节记录运行时 Goal 续跑后的 004A 授权前复核。本轮只读取当前工作树、GSD/OpenSpec 基线和源码锚点，并同步准入记录；未写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short` | PASS_DIRTY_CLASSIFIED | 工作树存在既有文档修改、GSD 未跟踪文档和未跟踪 `DefaultLedgerTransactionPostingServiceImplTests.java`；本轮未把这些变更解释成冻结 Git 基线或 Done 证据。 |
| `rg -n "GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD\|GSD1-LD-RED-004A\|READY_TO_CONFIRM_NOT_CODE_AUTHORIZED" docs/TDD设计 openspec docs/产品设计 docs/系分设计 docs/DSL设计` | PASS | 授权前 Source of Truth 指向 004A 候选，状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；该候选随后已被用户确认并消费。 |
| `DefaultLedgerTransactionPostingServiceImpl#POSTABLE_SUBJECT_TYPES` | COMPAT_RISK_VISIBLE | `BUDGET_GROUP` 仍在入账兼容白名单内，这是 004A 必须保护或阻断的核心差距锚点。 |
| `BalanceControlFundsInstructionRouteResolver#resolveAdjust` | COMPAT_PATH_VISIBLE | 余额控制仍允许 `BUDGET_GROUP` 进入预算额度 `LIMIT <-> AVAILABLE` 控制路径；004A 不应直接全局删除该兼容能力。 |
| `PaymentInstrumentServiceImplTests` | CONTROL_SCOPE_PROTECTED | 既有测试已证明支付工具真实资金主体不能绑定预算组，预算控制绑定才指向预算组。 |
| `ControlAccountLedgerInitializationTests` | CONTROL_LEDGER_PROTECTED | 既有测试已证明预算组初始化的是 `BUDGET_BASIC` 控制账本，且不生成账本交易事实。 |
| 本轮 Maven / 目标测试 | NOT_RUN_PREGRANT | 本轮没有新的单一 Execution Grant，不进入 Red/Green 编码闭环；不运行目标 Maven 测试，不声明代码生产 Done。 |

结论：本节是授权前证据，随后已由 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 消费闭合。当前不得再把 004A 作为下一步候选；若继续账本账目，必须确认新的单一 Grant。

## 21. verificationEvidence2026-06-11-004AConsumed

本节记录 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 确认并消费后的执行证据。本轮只处理 `GSD1-LD-RED-004A`：保留预算组控制账本兼容路径，阻断预算组作为资金价值交易主体。

| 项 | 结果 | 说明 |
| --- | --- | --- |
| `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` | CONSUMED | 用户确认后进入 Red/Green；Git 策略保持 `summary_only`，未执行 `git add` 或 `git commit`。 |
| 直接交易 Red/Green | PASS | 新增 topup / transfer / pay 的 `BUDGET_GROUP` 拒绝用例；converter 层最小 guard 阻断预算组作为充值入账账户、转账付款账户和直接付款账户。 |
| 授权交易 Red/Green | PASS | 新增 authorize 的 `BUDGET_GROUP` 拒绝用例；converter 层最小 guard 阻断预算组作为授权交易账户。 |
| 余额控制兼容回归 | PASS | `FundsBalanceControlFailureFlowTests` 通过，证明预算组控制账本兼容路径未因交易 guard 被破坏。 |
| 写入范围 | PASS | 仅写 transaction converter、transaction flow 测试和测试支撑断言；未写 DDL/H2 schema、公共契约、Entity、Mapper、钱包、支付工具、VCC、清结算对账或运行时配置。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsDirectTransactionFlowTests tests` | PASS | 47 tests / 0 failures / 0 errors。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsAuthorizationTransactionFlowTests tests` | PASS | 29 tests / 0 failures / 0 errors。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsBalanceControlFailureFlowTests tests` | PASS | 19 tests / 0 failures / 0 errors。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` | PASS | Maven reactor 编译通过。 |
| `git diff --check` | PASS | 当前 diff 无空白错误。 |

结论：`GSD1-LD-RED-004A` 已闭合。后续不得沿用 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 继续写代码；若要继续处理预算组 control ledger 退出、Spend Rule 控制投影、钱包账户/账户层级、交易内核或清结算对账，必须确认新的单一 Execution Grant。
