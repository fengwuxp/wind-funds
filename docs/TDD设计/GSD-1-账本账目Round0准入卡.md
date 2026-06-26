# GSD-1 账本账目 Round0 准入卡

## 1. 文档定位

本文档是当前 GSD 产研协同流程下的第一优先级准入卡，用于把账本、账目、posting、ledger entry 和余额投影的产品语义、系统落点、代码资产和测试资产收敛成一个可确认的 Execution Grant 候选。

本文档不授权修改生产代码、测试代码、DDL/H2 schema 或运行时配置。只有用户明确确认本页或经调整后的单一 Execution Grant 后，才允许把本文 Red 候选转成实际测试写入和最小实现。

当前任务优先级按模块或能力排列为：账本账目 > 钱包 > 交易层。支付工具、VCC 和全球账户支持放到最后；收单能力仅做设计和边界复核，不进入实现候选。

## 2. gsdCadAdmission

| 检查项 | 当前结论 | 说明 |
| --- | --- | --- |
| GSD Round 0 | `PASS` | PRD、DSL、系分、TDD、OpenSpec、Harness 和源码入口已能支撑账本账目任务包整理。 |
| CAD Mode | `004A_CONSUMED_SUMMARY_ONLY` | `GSD1-LD-RED-003` 已消费 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 并完成既有覆盖回归登记；`GSD1-LD-RED-004A` 已消费 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 并完成 BudgetGroup 兼容 guard，后续继续编码需重新确认新的单一 Grant。 |
| 当前推荐候选 | 无可沿用 Grant | 可在预算组 control ledger 退出条件、钱包账户/账户层级、交易内核或清结算对账中重新选择一个低风险切片。 |
| Git 策略 | `summary_only` | 未获自动提交授权前，只记录文档变更、验证和下一步门禁。 |

## 3. authorityBaseline

| 基线项 | 当前口径 |
| --- | --- |
| 产品入口 | `docs/产品设计/02-交易路由钱包账目与投影.md` 中的账本账目、余额投影、账本周期和资金事实可解释性要求。 |
| DSL 入口 | `docs/DSL设计/支付资金底座DSL承载层设计.md` 中的 posting、ledger entry、账本周期、账务期望表和 JSON 契约。 |
| 系分入口 | `docs/系分设计/02-交易路由钱包账目与投影系分设计.md` 中 `LedgerService`、`LedgerTransactionService`、`LedgerTransactionPostingService`、`LedgerBalanceProjectionService` 的模块落点。 |
| TDD 入口 | `docs/TDD设计/支付资金底座测试驱动设计.md`、`A0-编码准入基线核验.md` 和本文档。 |
| OpenSpec 入口 | `openspec/project.md`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 代码入口 | `core`、`ledger-face`、`ledger-impl`、`transaction-impl` 中账务计划装配器，以及 `tests` 中账本服务、账本交易、posting 装配和余额投影测试。 |
| 当前 Git 基线 | 用户确认 Execution Grant 时的 Git HEAD。本文档提交前只作为未授权准入草案。 |
| Wave 1 入口 | `docs/TDD设计/GSD-1-账本账目Wave1执行计划.md`，用于承接本文首批 Red 推荐、原子任务包、验证矩阵和 Execution Grant 草案。 |

## 4. round0CodeScan

| 代码资产 | 当前观察 | 准入影响 |
| --- | --- | --- |
| `LedgerService` | 提供建账、更新账本余额和查询账本契约。 | 可作为账本账户与账本周期的公共契约锚点。 |
| `LedgerTransactionService` | 提供账本交易入账、交易查询和分录查询契约。 | 可作为 ledger transaction、posting plan 和 ledger entry 落库入口。 |
| `DefaultLedgerTransactionPostingServiceImpl` | 在入账前校验交易金额、状态、posting plan、entry 金额、币种、平衡、可入账主体、绑定账本和余额约束，再调用入账和投影。 | 是首批 Red 的主要生产实现锚点；现有可入账主体白名单仍包含 `BUDGET_GROUP`，是否收敛必须在 Grant 中单独确认。 |
| `DefaultLedgerPostingAssembler` | 从 resolved route 装配 posting plan 和 ledger entry。 | 可验证账本周期、planId 长度、route 上下文防御性拷贝和 posting 平衡。 |
| `LedgerBalanceProjectionServiceImpl` | 按 ledgerId 分组投影，校验 ledger/entry 主体、账目、币种和负余额约束，发布余额变更观察事件。 | 可验证余额投影只从 ledger entry 派生、失败前置校验和事件不成为事实源。 |

## 5. existingCoverage

| 测试资产 | 已有覆盖 | 当前口径 |
| --- | --- | --- |
| `LedgerServiceImplTests` | 非生命周期账本缺 `periodId` 失败、显式周期建账、默认账本事实和不生成账务交易事实。 | 可作为账本账户和周期基础回归。 |
| `DefaultLedgerPostingAssemblerTests` | 非 LIFETIME 周期校验、显式周期、LIFETIME 默认、posting 平衡、长 planId 截断和上下文防御性拷贝。 | 可作为 posting 装配和账本周期回归。 |
| `LedgerTransactionServiceImplTests` | 账本交易、posting plan 和 ledger entry 上下文敏感字段、外部账户原文和权益核心事实阻断。 | 可作为账务事实写入边界回归。 |
| `LedgerBalanceProjectionServiceImplTests` | 余额事件失败不回滚余额事实、余额事件证据、上下文不可变、权益核心字段投影前阻断和整批约束失败无半截投影。 | 可作为余额投影和余额日志边界回归。 |
| `FundsModuleDependencyBoundaryTests` | 资金包名、模块依赖方向和生产模块不依赖 tests。 | 可作为账本账目变更后的架构边界回归。 |

### 5.1 firstRedDecision2026-06-04

| 检查项 | 本轮只读结论 | 首批 Red 影响 |
| --- | --- | --- |
| `DefaultLedgerTransactionPostingServiceImpl` 独立覆盖 | 当前未发现该入账编排服务的独立目标测试类；现有流程测试通过交易编排间接消费它，`LedgerTransactionServiceImplTests` 主要保护持久层上下文红线。 | 首轮优先补编排边界 Red，而不是继续重复持久层上下文阻断。 |
| `GSD1-LD-RED-001` | 生产实现已有金额、币种、状态、posting plan、entry、ledgerId、绑定账本和余额约束前置校验，但缺少一张独立测试说明这些校验发生在持久化和余额投影之前。 | 推荐拆成首个可执行切片 `GSD1-LD-RED-001A`。 |
| `GSD1-LD-RED-002` | 父项关注账本分录与绑定账本的主体、账目、币种和负余额约束一致性；投影层已有事件非事实源、核心字段投影前阻断和整批余额约束失败无半截投影覆盖。 | 暂作为父项回归；001A/001B 完成后，当前只拆出入账编排入口三细场景 `GSD1-LD-RED-002A`。 |
| `GSD1-LD-RED-003` | 已有整批失败无半截投影和事件发布失败不回滚余额事实测试。 | 暂作为既有覆盖证明，不作为首个入口。 |
| `GSD1-LD-RED-004` | 生产实现仍包含 `BUDGET_GROUP` 可入账兼容路径。 | 预算组兼容策略属于高风险确认点，未确认前不得作为首个 Green 目标。 |

首轮推荐入口：`GSD1-LD-RED-001A`，新增或等价落地 `DefaultLedgerTransactionPostingServiceImplTests`，证明非法账本交易、非法 posting plan 或非法 entry 在 `DefaultLedgerTransactionPostingServiceImpl#post` 中被拒绝，且拒绝发生在 `LedgerTransactionService#postLedgerTransaction`、posting plan/entry 落库和 `LedgerBalanceProjectionService#project` 之前。

如果 `GSD1-LD-RED-001A` 在现有实现下已经通过，不应硬改生产代码制造 Green；应把它登记为覆盖补齐，再转入 `GSD1-LD-RED-001B`。001A/001B 均完成后，下一真实缺口探测已收敛为 `GSD1-LD-RED-002A`。

2026-06-07 状态更新：`GSD1-LD-RED-001A` 已消费 `Execution Grant：GSD1-LEDGER-FACTS` 并完成入账非法输入覆盖补齐；`GSD1-LD-RED-001B` 已消费 `Execution Grant：GSD1-LEDGER-IDEMPOTENCY` 并完成重复入账幂等覆盖补齐；`GSD1-LD-RED-002A` 已消费 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 并完成绑定账本账目、币种和负余额约束覆盖补齐；`GSD1-LD-RED-003` 已消费 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 并完成投影强化回归。专项测试和目标回归均通过且未修改生产代码。2026-06-07 工作树审计确认 002A 目标测试文件当前未被 Git 跟踪，因此这只作为当前工作树证据和后续编辑基底，不是已冻结 Git 基线。当前恢复入口迁移为 `GSD1-LD-RED-004A` / `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`，不得沿用 002A 或 003 授权继续写代码。

## 6. grantCandidate

| 字段 | 候选取值 |
| --- | --- |
| `taskId` | `GSD1-LEDGER-FACTS-CAD-001` |
| `mvpScenario` | 账本账目基础事实：一批经过 route/posting 装配后的账本交易入账时，系统必须证明账本交易、posting plan、ledger entry、余额投影和审计证据完整、平衡、可追溯，失败时不留下半截事实。 |
| `abilityBatch` | GSD-1 / P0 账本账目。只覆盖 ledger account、ledger transaction、posting、ledger entry、balance projection。 |
| `businessQuestion` | 运营、财务和研发能否仅凭账本事实解释某笔资金变化的主体、账目、币种、周期、借贷方向、余额影响、幂等结果和审计证据。 |
| `moneyFact` | ledger transaction 是不可变账务事实；posting plan 证明每个阶段独立平衡；ledger entry 证明具体主体、账目、方向和金额；balance projection 只从 entry 派生。 |
| `userVisibleResult` | 账本查询、分录查询、余额查询和后续交易投影能反查同一条资金事实，不出现状态成功但账务不可解释的结果。 |
| `productNotDone` | 不声明钱包 application facade、资金责任目标字段迁移、交易层新能力、清结算对账、资金数据治理、账本余额快照物理落地、支付工具、VCC、全球账户或收单实现完成。 |
| `currentStatus` | `WAVE_2_RED_004A_CONSUMED_SUMMARY_ONLY` |

## 7. acceptanceMap

| 设计锚点 | 本候选覆盖 | 本候选不覆盖 |
| --- | --- | --- |
| 产品验收 | 账本账目、余额投影、账本周期、分录可追溯、投影只读和失败无半截事实。 | 用户账单/商户账单完整投影、清结算对账对象、归档 Manifest、业务专项外部规则。 |
| DSL caseId | posting、ledger entry、账本周期、账务期望表中和账本事实相关的 caseId。 | 支付工具应用入口、VCC/全球账户业务 pack、收单 capture/dispute 实现。 |
| 系分入口 | `LedgerService`、`LedgerTransactionService`、`LedgerTransactionPostingService`、`LedgerBalanceProjectionService` 和 `DefaultLedgerPostingAssembler`。 | `FundsDirectTransactionService`、`FundsAuthorizationTransactionService`、钱包 application facade、清结算对账服务和 governance apply。 |
| TDD 用例 | `TDD-LEDGER-*`、posting/entry/balance projection、资金变化最小断言集和 must-fail 红线。 | `TDD-AUTH-*`、`TDD-CTRL-*`、`TDD-B7-*`、`TDD-B8-*`、`TDD-P2-*`。 |

## 8. writeScopeCandidate

| 范围 | 候选授权 |
| --- | --- |
| 目标测试资产 | `GSD1-LD-RED-002A` 已在当前未被 Git 跟踪的 `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java` 中完成覆盖补齐；后续 Grant 必须先保护既有 001A/001B/002A 覆盖。 |
| 生产实现 | 002A Red 直接 Green，未触发 `ledger/ledger-impl/src/main/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImpl.java` 最小修复；后续生产实现写入需重新确认 Grant。 |
| 公共契约 | 默认不允许修改 `ledger-face`、`core` 公共接口、枚举、DTO、Request 或 Spec；若 Red 证明必须扩展，立即停止并回到用户确认。 |
| H2 schema | 默认不允许修改 `tests/src/test/resources/jdbc-schema.sql`。 |
| 上游账务计划装配器 | 002A 不允许修改 `DefaultLedgerPostingAssembler`；若 Red 证明账务计划装配器写入账目、币种或约束字段存在真实缺口，必须另起独立 Grant。 |

## 9. noWriteScope

| 禁止范围 | 说明 |
| --- | --- |
| 钱包 application facade | 不新增 `WalletAccountApplicationService`、`PaymentInstrumentCapabilityApplicationService`、`FundingResponsibilityResolutionApplicationService` 或等价 facade。 |
| 交易层新能力 | 不改直接交易、授权交易、余额控制、route replay、交易投影的新业务语义。 |
| 支付工具支持 | 不写 B2-PI-CAP、B4-AUTH-PI、B5-SR-CONTROL 或 B6/B8-PI-VIEW 的 Red 或实现。 |
| VCC 和全球账户 | 不写 P2 业务专项 application facade、Request/DTO、测试或实现。 |
| 收单 | 仅可作为设计-only 参考，不写 Red 测试或实现。 |
| 清结算对账和治理 apply | 不新增清结算/对账/Manifest/checkpoint/watermark/差异报告物理落点；账本余额快照只作为后续独立候选。 |
| 敏感数据和外部规则 | 不引入完整外部账户、PAN、CVV、密钥、生产配置或未经核验的外部规则自动放行。 |

## 10. redCandidateSet

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GSD1-LD-RED-001` | 账本交易入账前，posting plan、entry、币种、金额和状态是否被完整校验。 | 账本交易金额为正、币种一致、交易状态为 `POSTED`、每个 posting plan 独立平衡、entry 金额为正。 | 合法交易生成一笔 ledger transaction、对应 posting plan 和 ledger entry；非法交易不落任何账务事实。 | 不允许 posting 不平衡、entry 金额非正、币种混用、状态非 `POSTED` 或缺 entry 时仍落库。 | 首个切片建议新增 `DefaultLedgerTransactionPostingServiceImplTests`；必要时补 `LedgerTransactionServiceImplTests` 或 `DefaultLedgerPostingAssemblerTests` 回归。 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests`，必要时 `just test-ledger`。 | 需要修改公共契约、表结构或跨交易层语义时停止。 |
| `GSD1-LD-RED-002` | 账本分录与绑定账本是否严格匹配主体、账目、币种、周期和负余额规则。 | entry 必须绑定到真实 ledger；ledger/entry 主体、账目、币种和负余额规则一致。 | 合法 entry 投影到正确账本 bucket；不匹配 entry 在写余额前失败。 | 不允许错主体、错账目、错币种、缺 ledgerId 或违反负余额规则时写入余额。 | 父项回归参考为 `LedgerBalanceProjectionServiceImplTests`、`LedgerServiceImplTests`；当前可确认子切片 002A 落在 `DefaultLedgerTransactionPostingServiceImplTests`。 | 父项回归可运行 `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one LedgerBalanceProjectionServiceImplTests tests`；002A 确认后运行 `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests`，必要时 `just test-ledger`。 | 需要新增账本表字段、索引或余额快照物理表时停止；002A 不处理这些结构变化。 |
| `GSD1-LD-RED-003` | 余额投影失败是否不会留下半截余额变化，余额事件是否不成为事实源。 | 投影必须先完成整批约束校验；余额事件失败不回滚已提交余额事实，也不能反向修改账本事实。 | 合法批次按 ledgerId 更新余额；事件记录包含来源 entry 证据。 | 不允许前一个 bucket 已更新、后一个 bucket 失败；不允许事件失败阻断或篡改 ledger transaction、posting plan、entry。 | `LedgerBalanceProjectionServiceImplTests`。 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one LedgerBalanceProjectionServiceImplTests tests`。 | 需要引入外部事件存储、消息或异步基础设施时停止。 |
| `GSD1-LD-RED-004` | 可入账主体白名单是否与目标态一致。 | ledger entry 主体只能是目标态确认的可入账主体；支付工具、外部账户、预算组和 Spend Rule 不得直接成为 ledger entry 主体。 | 已确认可入账主体正常入账；非目标主体失败且不落账。 | 不允许支付工具、外部账户、预算组或 Spend Rule 被当作 ledger subject。 | `DefaultLedgerTransactionPostingServiceImpl` 相关测试，必要时补 `FundsModuleDependencyBoundaryTests`。 | 先运行目标 `test-one`，再补 `just test-boundary`。 | 当前实现仍包含 `BUDGET_GROUP` 可入账兼容路径；是否移除或兼容保留属于高风险决策，未在 Grant 中确认前不得写 Green。 |

首轮推荐 `GSD1-LD-RED-001A` 已执行完成：入账编排前置校验必须先于账本交易、posting plan、ledger entry 和余额投影写入。第二个候选 `GSD1-LD-RED-001B` 已执行完成：同一 ledger transaction 重复 post 后不重复账本事实和余额投影。第三个候选 `GSD1-LD-RED-002A` 已执行完成：entry 与绑定账本的账目、币种和负余额约束必须一致，失败在 `LedgerTransactionService#postLedgerTransaction` 和 `LedgerBalanceProjectionService#project` 前发生。第四个候选 `GSD1-LD-RED-003` 已执行完成：既有 `LedgerBalanceProjectionServiceImplTests` 已覆盖事件非事实源、来源证据、上下文快照、核心权益字段前置阻断和整批失败无半截投影，目标回归 5 tests 通过。001A/001B/002A 完成态目前只对应当前工作树中的未跟踪目标测试文件，不可外推为已冻结 Git 基线；003 未写生产代码或测试代码。`GSD1-LD-RED-004` 涉及预算组兼容语义，必须等用户在 Execution Grant 中明确 `BUDGET_GROUP` 处理策略后才能写实现。

### 10.1 consumedGrant2026-06-07

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD1-LD-RED-002A` |
| Execution Grant | `GSD1-LEDGER-BOUND-LEDGER`，已消费。 |
| 目标 | 补入账编排层目标测试，证明 ledger entry 与绑定 ledger 的账目 code/category、币种和负余额约束必须一致；失败必须发生在账本事实持久化和余额投影之前。 |
| 首批 Red | 1. entry 的 `ledgerSubjectCode` / `ledgerSubjectCategory` 与绑定 ledger 不一致；2. entry 币种与绑定 ledger 不一致；3. entry 使用 `ALLOW_NEGATIVE` 但绑定 ledger 不允许负余额。 |
| 已有覆盖不重复 | `GSD1-LD-RED-001A` 已覆盖缺 `ledgerId` 和主体不匹配；`GSD1-LD-RED-003` 在投影层已有事件非事实源和整批失败无半截投影覆盖。2026-06-07 源码锚点复核确认生产实现已有账目、币种和 `ALLOW_NEGATIVE` guard；002A 仍只补 posting service 入口的绑定账本细约束目标测试。 |
| 实际写入 | `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`；该文件当前未被 Git 跟踪，已保护既有 001A/001B 测试。 |
| 条件写入 | 未触发；Red 直接 Green，只登记覆盖补齐并停止生产改动。 |
| 禁止写入 | `ledger-face`、`core` 公共契约、枚举、DTO、Request、DDL/H2 schema、Mapper、`BUDGET_GROUP` 兼容策略、wallet、transaction 新业务语义、支付工具、VCC、全球账户、收单、清结算对账、governance apply、生产配置、外部协议和敏感数据处理。 |
| 验证命令 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version`、`WIND_FUNDS_JAVA_HOME=<Java21 home> just compile`、`WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests`；必要时补 `just test-ledger`；提交前 `just pmd` 和 `git diff --check`。 |
| 当前状态 | `DONE_COVERAGE_ADDED`。 |

### 10.2 consumedGrant2026-06-07-projection

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD1-LD-RED-003` |
| Execution Grant | `GSD1-LEDGER-PROJECTION-REGRESSION`，已消费。 |
| 目标 | 复核余额投影失败无半截变化、余额事件不成为事实源、事件具备来源 entry 证据和投影前置校验。 |
| 覆盖结论 | 既有 `LedgerBalanceProjectionServiceImplTests` 已覆盖事件发布失败不回滚余额事实、事件携带来源分录证据、嵌套上下文不可被外部回写污染、核心权益字段投影前阻断、后一个余额桶失败时整批不写。 |
| 实际写入 | 无 Java、测试、DDL/H2 schema、公共契约或运行时配置写入；仅登记覆盖和状态。 |
| 验证命令 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one LedgerBalanceProjectionServiceImplTests tests`。 |
| 验证结果 | PASS，5 tests / 0 failures / 0 errors，reactor `BUILD SUCCESS`。 |
| Git 策略 | `summary_only`，不自动 `git add`，不自动 `git commit`。 |
| 后续人工确认点 | `GSD1-LD-RED-004` 的 `BUDGET_GROUP` 兼容策略仍未确认；不得在 003 Grant 下处理。 |

### 10.3 budgetGroupCompatibilityStrategy2026-06-07

本节是 `GSD1-LD-RED-004` 的产品 + 架构策略准备结论，不是编码授权。它用于把“预算组不是目标态核心资金记账主体”和“当前代码仍有预算组控制账本兼容路径”拆开处理，避免下一轮直接把 `BUDGET_GROUP` 从全局 posting 白名单删除而打断现有预算控制链路。

| 项 | 结论 |
| --- | --- |
| 目标态语义 | `BudgetGroup` 是预算控制 scope、规则归属、查询过滤和审计维度，不是核心资金账务主体；支付、授权、出入金、退款、撤销、清结算和对账补事实最终必须落到资金账户、信用账户或平台角色解析后的平台资金账户。 |
| 当前代码事实 | `FundsSubjectType.BUDGET_GROUP`、`POSTABLE_SUBJECT_TYPES`、`RouteSubjectSupport`、`BudgetGroupServiceImpl`、`BUDGET_BASIC` profile、预算组余额控制和部分 DSL / flow 测试仍保留 `BUDGET_GROUP` 兼容路径。现有预算组额度调整测试已经表达“只调整预算 LIMIT / AVAILABLE 控制账本，不污染真实资金账户或平台账本”。 |
| 推荐策略 | 采用 `COMPAT_CONTROL_LEDGER_WITH_FREEZE`：迁移期保留 `BUDGET_GROUP` 作为预算控制账本兼容主体，但冻结其目标态解释，禁止把它扩大成支付、授权、出入金或清结算的资金价值转移主体。 |
| 不推荐策略 | 不建议 004A 直接全局删除 `BUDGET_GROUP` 或从 `DefaultLedgerTransactionPostingServiceImpl` 全局拒绝 `BUDGET_GROUP`，因为这会连带破坏现有预算组 control ledger 初始化和预算额度调账兼容流。 |
| 明确拒绝 | 不接受把 `BUDGET_GROUP` 继续声明为目标态可记账资金主体；它只能作为兼容控制账本、控制活动和读模型维度存在，且必须有退出条件。 |
| 下一 Grant 建议 | `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` / `GSD1-LD-RED-004A`，级别为 `contract-only + characterization-regression`，先证明预算组只允许出现在预算控制兼容场景，不允许进入直接交易、授权交易、出入金、退款、清结算或对账补事实的资金价值路由。 |
| 004A 允许写入建议 | 若用户确认，只允许写目标测试或最小 route / transaction guard：优先保护预算控制现有行为，再补“直接交易或授权资金路由不得接受 BudgetGroup 作为 payer / payee / funding subject”的 Red；只有 Red 证明真实缺口时，才允许在交易路由入口做最小阻断。 |
| 004A 禁止写入 | 不删 `FundsSubjectType.BUDGET_GROUP`，不改 DDL/H2 schema，不迁移 `BudgetGroupServiceImpl`，不改预算组 ledger 初始化，不改 Spend Rule 生产模型，不改支付工具、VCC、全球账户、清结算对账或 governance apply。 |
| 后续退出条件 | 待 `B5-SR-CONTROL` 或等价预算控制视图、`SpendControlMovement`、`SpendRuleDecisionRecord` 和预算控制投影生产模型形成后，再另起 `GSD1-LD-RED-004B` 评估是否删除预算组 control ledger 兼容路径或把它降级为只读迁移别名。 |

## 11. verificationPlan

| 阶段 | 命令 | 通过口径 |
| --- | --- | --- |
| 开工前 | `git status --short`、`WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version`、`WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` | 工作树变更已分类；Java 21 runtime 正确；编译通过。 |
| Red | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one <目标测试类> tests` | 新增或恢复的 Red 必须按预期失败；如果没有失败，先判断是否已有覆盖或 Red 写错。 |
| Green | 目标 `test-one` 或 `just verify-slice <TestClass>[,<TestClass>] tests` | 只做最小修复，让目标 Red 和既有账本回归通过。 |
| 回归 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-ledger` | 账本服务、posting 装配、账本交易和余额投影通过。 |
| 边界 | 触碰模块依赖、包名、主体白名单或上下文边界时补 `just test-boundary` | 资金模块边界和上下文红线未破坏。 |
| 提交前 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile`、`WIND_FUNDS_JAVA_HOME=<Java21 home> just pmd`、`git diff --check` | 编译、规约和空白检查通过；若因私服、网络或环境失败，必须区分环境问题和代码问题。 |

## 12. stopConditions

1. 需要修改 `ledger-face`、`core` 公共契约、枚举、Request/DTO/Spec 或错误码，但 Execution Grant 未授权。
2. 需要新增或修改生产表结构、H2 schema、Entity、Mapper、唯一键或索引。
3. Red 触碰钱包 application facade、交易层业务语义、清结算对账、governance apply、支付工具支持、VCC、全球账户或收单实现。
4. 预算组、Spend Rule 或支付工具是否可入账的语义需要取舍，且 Grant 未明确。
5. 测试只能断言状态、数量或“不报错”，无法断言 posting、entry、balance projection、幂等或失败无副作用。
6. 工作树出现未分类变更，或用户未提交变更与本任务写入范围冲突。

## 13. cadCandidateStructure

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD1-LEDGER-FACTS-CAD-001` |
| Owner | `资深架构师` + Codex 自动执行；产品、财务或合规只在资金语义、预算组兼容、外部规则和监管口径需要确认时介入。 |
| Wave | GSD-1 / 账本账目 / Round 0 -> Wave 1 候选。 |
| 当前状态 | `WAVE_2_RED_004A_CONSUMED_SUMMARY_ONLY`。 |
| 写入范围 | 002A、003 和 004A Grant 均已消费；当前只允许写 GSD 文档、OpenSpec/Harness 索引和交接记录，后续编码需重新确认新的单一 Grant。 |
| 只读范围 | 只读参考包括 `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、账本账目相关生产代码和测试资产。 |
| 依赖顺序 | 先确认单一 Execution Grant，再选择一个 Red；Red 失败符合预期后才允许 Green；Green 后运行专项回归；最后做本轮 CR 和交付记录。 |
| 验收场景 | 账本交易、posting plan、ledger entry、余额投影和审计证据完整、平衡、可追溯；失败时不留下半截事实。 |
| Superpowers | 执行时遵循 `资深架构师` 的 TDD、Review、Refactor、编码红线和 AI 产物复核要求；先红后绿，CR 本轮差异，再收口残余风险。 |
| 禁止事项 | 不写钱包、交易层新能力、支付工具、VCC、全球账户、收单实现、清结算对账、governance apply、DDL/H2 schema、公共契约、生产配置或敏感数据处理。 |
| 验证命令 | `just mvn-version`、`just compile`、目标 `just test-one <TestClass> tests`、`just test-ledger`；触碰边界时补 `just test-boundary`；提交前补 `just pmd` 和 `git diff --check`。 |
| 停止条件 | 公共契约、表结构、预算组兼容策略、跨能力域、外部规则、敏感数据或验证无法表达资金不变量时立即停止。 |
| 交接要求 | 交付必须列出文件、模块、覆盖的 TDD 清单项、验证命令、通过/未通过原因和残余风险。 |
| Execution Grant | 001A Grant、001B Grant 和 002A Grant 均已消费；当前等待用户重新确认新的账本账目候选或调整优先级。 |

## 14. consumedConfirmationTemplate

```text
Execution Grant：GSD1-LEDGER-IDEMPOTENCY
确认基线：确认时 Git HEAD、OpenSpec 和 Harness 最新任务账本；本文档作为 GSD-1 账本账目 Round0 准入卡，GSD-1-账本账目Wave1执行计划.md 作为 Wave 1 执行入口
允许写入：tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java；Red 证明真实缺口后允许 ledger/ledger-impl/src/main/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImpl.java 最小修复
禁止写入：wallet application facade、transaction 新业务语义、支付工具支持、VCC、全球账户、收单、清结算对账、governance apply、DDL/H2 schema、公共契约、LedgerTransactionService 摘要冲突语义、生产配置、外部协议、敏感数据处理
首批 Red：GSD1-LD-RED-001B；证明同一 ledger transaction 重复 post 后不重复 ledger transaction、posting plan、ledger entry 或 balance projection；GSD1-LD-RED-004 需先确认 BUDGET_GROUP 兼容策略
验证命令：just mvn-version、just compile、目标 just test-one、just test-ledger；触碰边界时补 just test-boundary；提交前 just pmd 和 git diff --check
Git 策略：summary_only，除非另行确认 auto_commit
停止条件：公共契约、表结构、预算组兼容策略、跨能力域、外部规则、敏感数据或验证无法表达资金不变量时立即停止
```

本模板已被 `GSD1-LD-RED-001B` 消费，只保留为授权证据。后续不得沿用本模板继续编码；新的账本账目切片必须重新确认 Execution Grant。

## 15. nextConfirmationTemplate2026-06-05

```text
Execution Grant：GSD1-LEDGER-BOUND-LEDGER
允许执行：GSD1-LD-RED-002A
目标：为 DefaultLedgerTransactionPostingServiceImpl 补 entry 与绑定账本的账目、币种和负余额约束 Red，证明不匹配 entry 在 LedgerTransactionService.postLedgerTransaction 和 LedgerBalanceProjectionService.project 前失败，不落账本交易、posting plan、ledger entry 或余额投影；2026-06-07 只读源码锚点显示 assertEntryMatchesLedger 已包含账目、币种和 ALLOW_NEGATIVE guard，002A 默认先补目标测试覆盖，不预设生产代码必改
允许写入：tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java；该文件当前未被 Git 跟踪，授权后必须先保护既有 001A/001B 测试，再补 002A
条件写入：只有 Red 证明真实生产缺口后，允许 ledger/ledger-impl/src/main/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImpl.java 做最小修复；若 Red 直接 Green，仅登记覆盖补齐并停止生产改动
只读参考：docs/产品设计、docs/DSL设计、docs/系分设计、docs/TDD设计、openspec、DefaultLedgerTransactionPostingServiceImpl、LedgerTransactionServiceImpl、LedgerBalanceProjectionServiceImpl、LedgerBalanceProjectionServiceImplTests、LedgerServiceImplTests、AbstractFundsServiceTest
禁止写入：ledger-face、core 公共契约、枚举、DTO、Request、DDL/H2 schema、Mapper、BUDGET_GROUP 兼容策略、wallet、transaction 新业务语义、支付工具、VCC、全球账户、收单、清结算对账、governance apply、生产配置、外部协议和敏感数据处理
首批 Red：账目 code/category mismatch、currency mismatch、entry balanceConstraintType=ALLOW_NEGATIVE 但 bound ledger allowNegative=false
Red 直接 Green：登记为覆盖补齐，回写状态账本和 OpenSpec，不修改生产代码
验证命令：WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version；WIND_FUNDS_JAVA_HOME=<Java21 home> just compile；WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests；必要时 WIND_FUNDS_JAVA_HOME=<Java21 home> just test-ledger；提交前 WIND_FUNDS_JAVA_HOME=<Java21 home> just pmd 和 git diff --check
Git 策略：summary_only，不自动 git add，不自动 git commit
停止条件：公共契约、表结构、预算组兼容策略、跨能力域、外部规则、敏感数据、验证无法表达资金不变量、工具权限失败无法降级、Red 需要超出写入范围
撤销方式：用户任意时刻说暂停、停止、撤销授权或调整范围即停止自动推进
```

本模板仅为下一候选授权文本，不代表已确认。用户确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
