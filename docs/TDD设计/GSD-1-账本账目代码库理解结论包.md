# GSD-1 账本账目代码库理解结论包

## 1. 文档定位

本文档是 `GSD1-LEDGER-FACTS-CAD-001` 的 AI Native 代码库理解结论包，用于把账本账目 Wave 1 的业务目标、真实代码入口、影响模块、调用关系、边界变化、源码锚点、验证证据和残余风险收敛到同一页。

本文档不授权修改生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。它只服务 GSD 交接、Execution Grant 确认和后续 Red 设计。

## 2. understandingConclusion

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD1-LEDGER-FACTS-CAD-001` |
| 当前原子任务 | `GSD1-LD-RED-004A` |
| 任务目标 | 已完成 BudgetGroup 兼容 guard：保留预算组控制账本兼容路径，阻断 `BUDGET_GROUP` 作为直接交易和授权交易的资金价值主体。 |
| Owner | `资深架构师` + Codex；用户负责确认后续新的 Execution Grant 或人工决策点。 |
| 阶段 / Wave | GSD-1 / 账本账目 / Wave 2。 |
| 当前状态 | `WAVE_2_RED_004A_CONSUMED_SUMMARY_ONLY`。 |
| 结论 | 代码入口足够明确，001A/001B/002A 已在当前工作树补齐入账编排目标测试，003 已完成余额投影既有回归，004A 已在 transaction converter 层完成 BudgetGroup 资金价值主体 guard。目标测试文件当前未被 Git 跟踪，后续继续编码必须确认新的单一 Grant，不得沿用 004A。 |
| 恢复入口 | `GSD-1-账本账目状态账本.md#21-verificationevidence2026-06-11-004aconsumed`、`GSD-1-账本账目ExecutionGrant确认卡.md#16-consumedbudgetgroupcompatguardgrant2026-06-11`。 |

## 3. businessIntent

账本账目的第一性目标不是新增钱包或交易能力，而是证明资金事实在进入账本后具备可解释、可核对和可回归的基础：

| 业务意图 | 代码侧需要证明 |
| --- | --- |
| 账本交易必须是可入账状态。 | 非 `POSTED` 交易不能进入持久化和余额投影。 |
| posting plan 必须完整且自洽。 | 缺 plan、缺 entry、交易流水不一致、借贷不平衡或币种不一致时失败。 |
| ledger entry 必须可落到真实可入账主体和账本。 | 主体类型、主体 ID、ledgerId、账目、币种、余额约束和账本绑定必须先校验。 |
| 失败不能产生半截账务事实。 | 失败后不得新增 ledger transaction、posting plan、ledger entry 或 balance projection。 |
| 余额投影只消费账本分录派生事实。 | 投影不能成为新事实源，也不能在非法 entry 下先写余额。 |
| 重复入账不能重复投影。 | 同一 ledger transaction 已存在且摘要一致时，posting service 必须跳过余额投影，避免 ledger balance 重复变化。 |

## 4. entryPath

| 顺序 | 源码锚点 | 职责 | 对 `GSD1-LD-RED-002A` 的意义 |
| --- | --- | --- | --- |
| 1 | `transaction/transaction-impl/src/main/java/com/wind/funds/transaction/DefaultRoutedFundsInstructionOrchestrator.java` | 交易路由编排完成后，调用 posting assembler 生成 `LedgerTransactionSpec`，再委派入账服务。 | 说明账本 Red 可以保持在 ledger 边界，不需要进入钱包、支付工具或 P2 业务。 |
| 2 | `transaction/transaction-impl/src/main/java/com/wind/funds/transaction/ledger/DefaultLedgerPostingAssembler.java` | 从资金指令和 route 组装账本交易、posting plan 和 entry。 | 只读上游锚点；002A 已证明入账编排入口不会把账目、币种或负余额约束不匹配的 entry 交给持久层或投影层。 |
| 3 | `ledger/ledger-impl/src/main/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImpl.java` | 执行入账前校验、主体分组、投影服务解析、账本事实持久化和余额投影编排。 | 001A/001B/002A 的目标测试边界；当前不是新的默认编码入口。 |
| 4 | `ledger/ledger-impl/src/main/java/com/wind/funds/ledger/impl/LedgerTransactionServiceImpl.java` | 持久化 ledger transaction、posting plan 和 ledger entry，并处理幂等摘要。 | 只读参考其事实写入边界；后续新的账本 Grant 才能继续扩展。 |
| 5 | `ledger/ledger-impl/src/main/java/com/wind/funds/ledger/impl/LedgerBalanceProjectionServiceImpl.java` | 根据 ledger entry 更新账本余额并发布余额变更观察事件。 | 003 已完成既有投影强化覆盖回归；当前不是新的默认编码入口。 |
| 6 | `transaction/transaction-impl/src/main/java/com/wind/funds/transaction/converter/FundsDirectTransactionInstructionConverter.java` | 把直接交易请求转换成资金指令。 | 004A 已在这里增加 BudgetGroup guard，阻断直接充值、转账、付款、退款、提现和手续费等资金价值入口使用预算组。 |
| 7 | `transaction/transaction-impl/src/main/java/com/wind/funds/transaction/converter/FundsAuthorizationInstructionConverter.java` | 把授权交易请求转换成授权资金指令。 | 004A 已在 authorize 入口增加 BudgetGroup guard，阻断预算组作为授权交易账户。 |

## 5. callRelation

```text
FundsInstructionSpec
  -> RouteResolver
  -> RouteSnapshotFactory
  -> FundsInstructionLifecycleRecorder.beforePosting
  -> DefaultLedgerPostingAssembler.assemble
  -> LedgerTransactionPostingService.post
       -> assertTransactionPostable
       -> assertAllPostingPlansHaveEntries
       -> assertAllEntriesUsePositiveAmounts
       -> assertAllPostingPlansUseSingleCurrency
       -> assertTransactionCurrencyMatchesPostingPlans
       -> assertAllPostingPlansBalanced
       -> assertAllEntriesUsePostableSubjects
       -> assertAllEntriesBoundToLedgers
       -> assertAllEntriesMatchBoundLedgers
       -> assertAllLedgerBalanceConstraintsSatisfied
       -> LedgerTransactionService.postLedgerTransaction
       -> LedgerBalanceProjectionService.project
  -> FundsInstructionLifecycleRecorder.markSucceeded / markFailed
```

关键判断：`DefaultLedgerTransactionPostingServiceImpl#post` 的前置校验在 `LedgerTransactionService#postLedgerTransaction` 和 `LedgerBalanceProjectionService#project` 之前，001A 已锁住“非法事实不得落库、不得投影”的基础边界；001B 已证明 `newlyPosted=false` 时不会二次投影。002A 继续在同一入口补细约束：entry 与绑定账本的账目 code/category、币种和负余额能力必须一致，否则不能把非法 entry 交给持久层或投影层。

### 5.1 sourceAnchorReview2026-06-07

本节记录运行时 Goal 延续后的 002A 授权前只读源码锚点复核。该复核不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

| 锚点 | 当前代码事实 | 对 002A 的影响 |
| --- | --- | --- |
| `DefaultLedgerTransactionPostingServiceImpl#post` | 在调用 `LedgerTransactionService#postLedgerTransaction` 前执行 `assertAllEntriesMatchBoundLedgers` 和 `assertAllLedgerBalanceConstraintsSatisfied`。 | 002A 仍应保持在入账编排服务入口，不需要进入交易、钱包、支付工具或 VCC。 |
| `DefaultLedgerTransactionPostingServiceImpl#assertEntryMatchesLedger` | 已校验 entry 与 bound ledger 的 subject、`ledgerSubjectCode` / `ledgerSubjectCategory`、currency，以及 `ALLOW_NEGATIVE` 与 ledger `allowNegative` 的兼容性。 | 生产实现可能已满足 002A 目标；授权后若 Red 直接 Green，应登记为覆盖补齐，不硬改生产代码。 |
| `DefaultLedgerTransactionPostingServiceImplTests` | 当前工作树已覆盖非 `POSTED`、空 entry、非法金额、流水不一致、缺 `ledgerId`、主体不匹配和重复 post 幂等，但该测试类当前未被 Git 跟踪。 | 仍缺 002A 三个独立目标测试：账目 code/category mismatch、currency mismatch、`ALLOW_NEGATIVE` 与 bound ledger `allowNegative=false` 不兼容；授权后必须先保护既有测试内容。 |
| `transaction/transaction-impl/.../DefaultLedgerPostingAssembler` | 上游装配器已根据 route node 和账本查询结果写入 ledgerId、账目、币种和约束字段。 | 只读参考；002A 不修改装配器，不把装配器问题混入账本入账边界。 |

## 6. existingCoverageMap

| 测试资产 | 已证明内容 | 缺口 |
| --- | --- | --- |
| `tests/src/test/java/com/wind/funds/ledger/impl/LedgerTransactionServiceImplTests.java` | 持久层上下文敏感字段阻断、重复账本交易摘要冲突、失败后账务事实不落库等局部红线。 | 入口已经在 `LedgerTransactionService`，不能证明入账编排服务在调用持久层前拒绝非法 posting/entry。 |
| `tests/src/test/java/com/wind/funds/ledger/impl/LedgerBalanceProjectionServiceImplTests.java` | 余额投影事件不是事实源、投影失败无半截变化、账本主体/科目/币种/负余额约束等局部红线。 | 入口已经在投影层，不能证明编排服务不会把非法 entry 送入投影。 |
| `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java` | 001A 已在当前工作树证明非 `POSTED`、空 entry、非法金额、流水不一致、缺 `ledgerId` 和主体不匹配都会在账本事实和余额投影前失败；001B 已在当前工作树证明同一合法 transaction 重复 post 时不重复 ledger facts 和 balance projection。2026-06-07 只读源码锚点复核确认生产实现已存在账目、币种和 `ALLOW_NEGATIVE` guard。 | 尚缺 posting service 入口对账目 code/category mismatch、currency mismatch、entry `ALLOW_NEGATIVE` 与绑定 ledger `allowNegative=false` 不一致的独立目标测试；002A 授权后可能是目标测试覆盖补齐。该文件不是已冻结 Git 基线，下一轮开工前必须复核 `git status --short`。 |
| `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsDirectTransactionFlowTests.java` | 004A 已补直接充值、系统内转账和直接付款拒绝 `BUDGET_GROUP` 的服务流测试，并断言无新增交易事实、账本事实和余额副作用。 | 已覆盖 004A 代表入口；退款、提现、手续费等 guard 由同一 converter helper 覆盖，后续如需逐项证明必须另起新 Grant。 |
| `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsAuthorizationTransactionFlowTests.java` | 004A 已补授权交易拒绝 `BUDGET_GROUP` 的服务流测试，并断言无新增授权 route、posting、ledger entry 或余额副作用。 | 已覆盖 authorize 入口；授权后继生命周期的历史兼容清理不属于 004A。 |
| `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsBalanceControlFailureFlowTests.java` | 004A 回归通过，证明预算组控制账本兼容路径没有被 transaction converter guard 破坏。 | 只证明兼容路径仍可用，不等于预算控制目标态退出已完成。 |
| `tests/src/test/java/com/wind/funds/transaction/ledger/DefaultLedgerPostingAssemblerTests.java` | 装配器能把路由和资金指令转成 posting/entry，并保留 route leg、账目、主体、上下文等证据。 | 不覆盖入账编排服务的失败顺序和落库/投影副作用。 |
| `tests/src/test/java/com/wind/funds/ledger/impl/LedgerServiceImplTests.java` | 账本创建、更新和余额字段局部能力。 | 不覆盖账本交易编排。 |
| `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsTransactionFlowTestSupport.java` | 交易流测试中会装配真实 `DefaultLedgerTransactionPostingServiceImpl` Bean。 | 适合作为参考，不宜把 001B 做成大交易流集成测试。 |

当前工作树已存在独立的 `DefaultLedgerTransactionPostingServiceImplTests`，但文件未被 Git 跟踪。因此 `GSD1-LD-RED-001A` 和 `GSD1-LD-RED-001B` 的覆盖补齐只可作为当前工作树证据和后续编辑基底，不可被写成已冻结 Git 基线；后续不再沿用 001B 授权继续写代码。

## 7. impactAndBoundary

| 类型 | 范围 |
| --- | --- |
| 写入范围 | 001B 授权已消费；`Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 未确认前，只允许写 GSD 文档、OpenSpec/Harness 索引和交接记录。 |
| 已写入文件 | 当前工作树存在 `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`，但该文件未被 Git 跟踪；后续授权执行时必须先保护其中 001A/001B 覆盖。 |
| 后续条件写入文件 | 002A Red 证明真实缺口后，才允许在新 Grant 范围内最小修复 `DefaultLedgerTransactionPostingServiceImpl`；不得沿用 001B 授权修改任何生产文件。 |
| 只读范围 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、账本相关生产代码、账本相关测试资产。 |
| 禁止范围 | 不改 `ledger-face`、`core` 公共契约、枚举、DTO、Request、DDL/H2 schema、钱包、交易层新业务语义、支付工具、VCC、全球账户、收单、清结算对账、governance apply、生产配置或敏感数据处理。 |

## 8. redDesignInput

`GSD1-LD-RED-002A` 的 Red 设计输入如下；它只补 posting service 入口，不重复投影层已有测试：

| Red 输入 | 断言重点 |
| --- | --- |
| entry 的 `ledgerSubjectCode` 或 `ledgerSubjectCategory` 与绑定 ledger 不一致 | 必须在 `LedgerTransactionService#postLedgerTransaction` 前失败；ledger transaction、posting plan、ledger entry 和 ledger balance 均不新增、不变化。 |
| entry 币种与绑定 ledger 不一致 | 必须在持久化和投影前失败；不得把错币种 entry 写入账本事实或余额投影。 |
| entry `balanceConstraintType=ALLOW_NEGATIVE` 但绑定 ledger `allowNegative=false` | 必须在持久化和投影前失败；不得绕过账本负余额能力。 |

002A 不处理 `BUDGET_GROUP` 是否可入账，不处理新增 ledger 字段、索引或余额快照物理落点，也不处理 `LedgerPostingPlanSpec#getEntries()` 与 `getPostingPhases()` 双源一致性。若 Red 直接通过，应登记为覆盖补齐，不强行修改生产代码。

## 9. verificationAndReview

| 阶段 | 验证命令 | 证据用途 |
| --- | --- | --- |
| GSD 文档结构 | `check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-1-账本账目代码库理解结论包.md` | 证明结论包具备 Task、Owner、范围、顺序、验证、停止条件和 handoff 字段。 |
| Wave 结构 | `check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | 证明 Wave1 原子任务包结构完整。 |
| CAD 候选结构 | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` | 证明 CAD 候选字段完整，但不等于授权。 |
| 文档空白 | `git diff --check`、`rg -n "[[:blank:]]+$|\r$" <GSD docs>` | 证明文档没有行尾空白或 CR。 |
| 已执行编译 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` | PASS，2026-06-07 复跑 reactor 14/14 modules `BUILD SUCCESS`，证明生产和测试源码可编译。 |
| 已执行目标测试 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` | PASS，2026-06-07 沙箱内因 embedded Redis 端口绑定受限失败，提权复跑后 7 tests / 0 failures / 0 errors；证明当前 001A/001B 既有覆盖仍通过，不证明 002A 已完成。 |
| 002A 授权后目标测试 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` | 证明绑定账本细约束不匹配时无半截账务事实；未授权前不执行。 |
| 授权后回归 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-ledger` | 证明账本主线未被破坏。 |

Review 顺序：先看业务语义、失败无副作用、持久化/投影边界、资金不变量和模块依赖方向；再看复用、命名、格式和测试组织。

## 10. stopConditions

1. 后续编码未确认 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`。
2. Red 需要修改公共契约、DTO、Request、枚举、表结构、H2 schema、Mapper 或运行时配置。
3. 修复需要进入钱包、交易层新业务语义、支付工具、VCC、全球账户、收单、清结算对账或治理 apply。
4. 需要决定 `BUDGET_GROUP` 是否继续作为可入账主体。
5. 目标测试无法证明账目、币种或负余额约束不匹配时无半截账务事实和余额投影副作用，或无法断言资金不变量。
6. 工具权限、Java runtime、私有 Maven 仓库、网络、凭据或本地缓存问题无法降级为文档交接。

## 11. residualRisksAndHandoff

| 风险 | 处理方式 |
| --- | --- |
| `BUDGET_GROUP` 当前仍在 `POSTABLE_SUBJECT_TYPES`。 | 作为人工确认点，不进入 002A 或 003 Green；下一轮若继续账本账目，必须先确认 004 兼容策略。 |
| `LedgerPostingPlanSpec#getEntries()` 是 default flatten，但接口允许自定义实现；持久层实际遍历 `getPostingPhases()`。 | 作为 001B 之后的残余风险，不混入重复入账幂等测试。 |
| 001B Red 已直接绿。 | 已登记为覆盖补齐，停止生产改动并回写 Wave1 结果。 |
| 002A Red 已直接绿。 | 已登记为覆盖补齐，不改生产代码。 |
| 003 投影强化回归已直接通过。 | 已登记为既有覆盖回归，不改生产代码或测试代码。 |
| 服务级目标测试需要真实 Spring Bean 和 H2 数据准备。 | 复用 `AbstractFundsServiceTest`、ledger setup 和现有 assertion support；Mock/Fake 只用于明确端口边界。 |

交接要求：`Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 和 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 均已消费。下一轮如果仍未确认 `GSD1-LD-RED-004` 的 `BUDGET_GROUP` 兼容策略或新的单一 Grant，只允许更新本文档、Round0 卡、Wave1 执行计划、状态账本、OpenSpec project 或 Harness tasks；如果要继续编码，必须先确认新的单一 Grant，再运行 Java 21 与编译检查。
