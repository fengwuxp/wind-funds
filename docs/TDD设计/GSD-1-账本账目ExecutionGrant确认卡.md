# GSD-1 账本账目 Execution Grant 确认卡

## 1. 文档定位

本文档是 `GSD1-LEDGER-FACTS-CAD-001` 的 Execution Grant 确认卡和消费记录。`GSD1-LD-RED-001A` 已消费 `Execution Grant：GSD1-LEDGER-FACTS` 并完成目标测试补齐；`GSD1-LD-RED-001B` 已消费 `Execution Grant：GSD1-LEDGER-IDEMPOTENCY` 并完成重复入账幂等覆盖补齐；`GSD1-LD-RED-002A` 已消费 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 并完成绑定账本细约束覆盖补齐；`GSD1-LD-RED-003` 已消费 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 并完成余额投影强化既有覆盖回归登记；`GSD1-LD-RED-004A` 已消费 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 并完成 BudgetGroup 兼容 guard。2026-06-07 工作树审计确认目标测试文件当前未被 Git 跟踪，因此该消费记录只作为当前工作树证据和后续编辑基底，不是已冻结 Git 基线。

本文档不再作为可继续消费的授权文本。后续若要继续编码，必须重新确认新的单一 Execution Grant；旧的 `GSD1-LEDGER-IDEMPOTENCY`、`GSD1-LEDGER-BOUND-LEDGER`、`GSD1-LEDGER-PROJECTION-REGRESSION` 和 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 只保留为执行证据。

## 2. grantReadiness

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD1-LEDGER-FACTS-CAD-001` |
| 原子任务 | `GSD1-LD-RED-004A` |
| 任务目标 | 已完成 BudgetGroup 兼容 guard：保留预算控制账本兼容路径，阻断预算组扩大为直接交易、授权交易、出入金、退款、清结算或对账补事实的资金价值主体。 |
| Owner | `资深架构师` + Codex 自动执行；用户负责后续新授权确认和高风险决策。 |
| 所属阶段 / Wave | GSD-1 / 账本账目 / Wave 2。 |
| 当前状态 | `WAVE_2_RED_004A_CONSUMED_SUMMARY_ONLY`。 |
| CAD 状态 | `004A_CONSUMED_SUMMARY_ONLY`。 |
| Git 策略 | `summary_only`；不自动 `git add`、不自动 `git commit`，除非用户另行确认 `auto_commit`。 |
| 只读恢复入口 | `GSD-1-账本账目状态账本.md#12-verificationevidence2026-06-05-001b`、`GSD-1-账本账目Wave1执行计划.md#51-executionresult`。 |
| 后续允许动作 | 继续更新 GSD 文档、OpenSpec/Harness 索引、验证矩阵、交接记录和下一候选授权卡；若要继续编码，必须重新确认新的单一 Execution Grant。 |
| 后续禁止动作 | 沿用 002A、003 或 004A 授权继续写 Java、测试、DDL/H2 schema、公共契约、运行时配置或 Git 提交。 |
| 最近运行时证据 | 2026-06-07 002A 提权复跑 `DefaultLedgerTransactionPostingServiceImplTests` 10 tests / 0 failures / 0 errors；003 运行 `LedgerBalanceProjectionServiceImplTests` 5 tests / 0 failures / 0 errors；2026-06-11 004A 运行 `FundsDirectTransactionFlowTests` 47 tests、`FundsAuthorizationTransactionFlowTests` 29 tests、`FundsBalanceControlFailureFlowTests` 19 tests 和 `just compile` 均通过。目标测试文件当前未被 Git 跟踪，因此不代表 Git 基线已冻结。 |

## 3. consumedExecutionGrantText

以下文本已由用户确认并被 001B 消费。它只作为执行证据保留，不再作为后续授权入口。

```text
Execution Grant：GSD1-LEDGER-IDEMPOTENCY
允许执行：GSD1-LD-RED-001B
目标：为 DefaultLedgerTransactionPostingServiceImpl 补重复入账幂等 Red，证明同一 ledger transaction 第二次 post 时，LedgerTransactionService 返回 existing/newlyPosted=false 后，不重复 ledger transaction、posting plan、ledger entry，也不再次触发 balance projection 或余额变化
允许写入：tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java
条件写入：只有 Red 证明真实生产缺口后，允许 ledger/ledger-impl/src/main/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImpl.java 做最小修复
只读参考：docs/产品设计、docs/DSL设计、docs/系分设计、docs/TDD设计、openspec、DefaultLedgerTransactionPostingServiceImpl、LedgerTransactionServiceImpl、LedgerBalanceProjectionServiceImpl、DefaultLedgerPostingAssembler、LedgerTransactionServiceImplTests、LedgerBalanceProjectionServiceImplTests、DefaultLedgerPostingAssemblerTests、AbstractFundsServiceTest、FundsTransactionFlowTestSupport
禁止写入：ledger-face、core 公共契约、枚举、DTO、Request、DDL/H2 schema、Mapper、LedgerTransactionService 摘要冲突语义、wallet、transaction 新业务语义、支付工具、VCC、全球账户、收单、清结算对账、governance apply、生产配置、外部协议和敏感数据处理
验证命令：WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version；WIND_FUNDS_JAVA_HOME=<Java21 home> just compile；WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests；必要时 WIND_FUNDS_JAVA_HOME=<Java21 home> just test-ledger；提交前 WIND_FUNDS_JAVA_HOME=<Java21 home> just pmd 和 git diff --check
Git 策略：summary_only，不自动 git add，不自动 git commit
停止条件：公共契约、表结构、预算组兼容策略、跨能力域、外部规则、敏感数据、验证无法表达资金不变量、工具权限失败无法降级、Red 需要超出写入范围
撤销方式：用户任意时刻说暂停、停止、撤销授权或调整范围即停止自动推进
```

消费结果：001B 已在目标测试类中新增重复入账幂等测试，专项验证 7 tests / 0 failures / 0 errors，通过后未修改生产代码。该目标测试类当前未被 Git 跟踪，后续 Grant 开工前必须先保护当前工作树中的 001A/001B 测试内容。

## 4. scope

| 类型 | 范围 |
| --- | --- |
| 写入范围 | 授权后仅限 `GSD1-LD-RED-001B` 的目标测试和 Red 证明真实缺口后的最小入账编排修复。 |
| 写入文件 | 仅允许 `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`；该文件当前未被 Git 跟踪。 |
| 条件写入文件 | 只有 Red 证明真实生产缺口后，才允许 `ledger/ledger-impl/src/main/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImpl.java` 最小修复。 |
| 只读范围 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、账本相关生产代码和测试资产。 |
| 只读参考 | `DefaultLedgerTransactionPostingServiceImpl`、`LedgerTransactionServiceImpl`、`LedgerBalanceProjectionServiceImpl`、`DefaultLedgerPostingAssembler`、`LedgerTransactionServiceImplTests`、`LedgerBalanceProjectionServiceImplTests`、`DefaultLedgerPostingAssemblerTests`、`AbstractFundsServiceTest`、`FundsTransactionFlowTestSupport`。 |
| 禁止事项 | 不改 `ledger-face`、`core` 公共契约、枚举、DTO、Request、DDL/H2 schema、Mapper、`LedgerTransactionService` 摘要冲突语义、wallet、transaction 新业务语义、支付工具、VCC、全球账户、收单、清结算对账、governance apply、生产配置、外部协议和敏感数据处理。 |

## 5. executionRecord

`Execution Grant：GSD1-LEDGER-IDEMPOTENCY` 消费后的执行记录如下：

| 顺序 | 动作 | 预期结果 |
| --- | --- | --- |
| 1 | 已运行 `WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version`。 | PASS，Java 21 runtime 与 Maven 入口可用。 |
| 2 | 已运行 `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile`。 | PASS，Maven reactor 编译通过。 |
| 3 | 已在 `DefaultLedgerTransactionPostingServiceImplTests` 中新增 001B 测试。 | 只写目标测试方法和必要测试夹具，未改生产代码；目标测试文件当前未被 Git 跟踪。 |
| 4 | 已运行 `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests`。 | PASS，当前工作树 7 tests / 0 failures / 0 errors；001B 直接 Green，登记为覆盖补齐。 |
| 5 | 已完成本轮 Review。 | 未发现越界写入；生产实现现有 `newlyPosted=false` 短路已满足幂等投影跳过。 |
| 6 | 已回写状态账本。 | 下一步进入新的账本账目候选选择或人工决策点。 |

## 6. acceptanceSeed

001B Red 的验收种子已按最小可证明集合落地，不扩展到摘要冲突或跨能力域。

| 场景 | 输入 | 必须断言 |
| --- | --- | --- |
| 重复同一账本交易 | 同一 `LedgerTransactionSpec` 第一次 post 成功后，使用相同 `sn`、相同 posting plan 和相同 entry 再次 post。 | 第二次 post 后 ledger transaction、posting plan、ledger entry 快照不变，相关 ledger balance 不变。 |
| 投影跳过可见性 | 第一次 post 已产生余额变化；第二次 post 进入 existing/newlyPosted=false 分支。 | 不通过 mock 断言私有调用，只用余额和账务事实证明没有重复投影。 |
| 幂等边界收缩 | 同一 `sn` 但不同摘要、不同金额或不同 entry。 | 当前 001B 不默认实现该分支；若 Red 必须进入摘要冲突语义，立即停止并另起 Grant。 |

验收底线：测试不得只断言“不报错”；必须断言重复 post 后无重复账务事实和无重复余额投影副作用。

## 7. consumedTestDesign

本节记录 001B 已消费的测试驱动设计口径。该口径只作为执行证据，不授权后续继续写测试代码。

| 维度 | 设计口径 |
| --- | --- |
| 被保护业务事实 | 入账编排服务必须在重复 post 时复用持久层幂等结果，不重复账本事实或余额投影。 |
| 测试层级 | 服务层流程测试，优先继承 `AbstractFundsServiceTest`，使用真实 Spring Bean、真实 H2 schema、真实 Mapper 和真实事务边界。 |
| 被测入口 | `DefaultLedgerTransactionPostingServiceImpl#post(LedgerTransactionSpec)`。 |
| 真实链路 | `DefaultLedgerTransactionPostingServiceImpl`、`LedgerTransactionService`、ledger 查询、账本事实快照、余额投影和 H2 表结构真实执行。 |
| 替身边界 | 不 Mock `LedgerTransactionService`、`LedgerService`、`LedgerBalanceProjectionService`、Mapper 或内部校验；如需替身，只能用于外部系统、不可控端口、时间或 ID。 |
| 前置数据 | 复用现有 ledger setup 或在测试中显式创建合法 funding account、AVAILABLE / SETTLEMENT 等必要账本；第一次 post 必须是合法入账，第二次只重复相同 spec。 |
| 核心断言 | 第一次 post 后记录 `ledgerFactSnapshot` 和相关 ledger balance；第二次 post 后断言 ledger transaction、posting plan、ledger entry 和 ledger balance 均不变。 |
| 断言禁区 | 不断言私有方法调用顺序，不用内部 Mock 交互替代业务事实，不通过硬凑 fixture、放宽断言或改测试迎合现有实现。 |
| 命名建议 | `testPostShouldSkipProjectionWhenLedgerTransactionAlreadyPosted`、`testPostShouldNotDuplicateLedgerFactsForSameTransaction`。 |
| 最小首例 | 推荐先写同一 spec 连续 post 两次，断言第二次后账务事实和余额均保持第一次后的快照。 |

测试完成定义：每个 Red 必须说明场景、输入、触发行为、输出和红线；至少断言一个能失败的业务事实或契约事实；资金事实相关断言必须覆盖账务事实不变和余额投影无副作用。

## 8. humanDecisionPoints

| 决策点 | 默认处理 |
| --- | --- |
| `BUDGET_GROUP` 是否继续作为可入账主体。 | 不在 `GSD1-LD-RED-001A`、`GSD1-LD-RED-001B`、`GSD1-LD-RED-002A` 或 `GSD1-LD-RED-003` 中处理，保持人工确认点。 |
| Red 需要新增公共契约、DTO、Request、枚举、表结构、H2 schema 或 Mapper。 | 立即停止，回到用户确认新的 Execution Grant。 |
| Red 直接通过。 | 登记为覆盖补齐，不硬改生产代码。 |
| Red 需要进入 `LedgerTransactionService` 摘要冲突、唯一索引或公共幂等契约。 | 立即停止，另起更窄或更高权限 Grant。 |
| 测试需要大交易流集成而非目标服务测试。 | 优先收缩回入账编排服务边界；无法收缩则暂停确认。 |
| Java runtime、私服、网络、凭据或缓存导致验证不可运行。 | 记录环境问题和已完成的替代检查，不把环境失败写成代码失败。 |

## 9. verificationForThisCard

| 阶段 | 验证命令 | 目的 |
| --- | --- | --- |
| 确认卡结构 | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md` | 检查 Task、Owner、范围、验证、Superpowers/Review、Execution Grant、人工确认和 handoff 字段。 |
| 文档空白 | `git diff --check`、`rg -n "[[:blank:]]+$|\r$" <GSD docs>` | 检查文档空白和 CR。 |
| 已执行编译 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` | PASS，确认 Java 代码基线。 |
| 已执行目标测试 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` | PASS，验证 001B Red/Green，当前工作树 7 tests / 0 failures / 0 errors；2026-06-07 沙箱内因 embedded Redis 端口绑定受限失败，提权复跑后通过；目标测试文件未被 Git 跟踪。 |

## 10. handoff

当前 handoff 结论：`GSD1-LEDGER-FACTS-CAD-001` 已完成 `GSD1-LD-RED-001A`、`GSD1-LD-RED-001B`、`GSD1-LD-RED-002A` 覆盖补齐、`GSD1-LD-RED-003` 既有投影强化覆盖回归登记和 `GSD1-LD-RED-004A` BudgetGroup 兼容 guard。003 专项验证通过且未修改生产代码或测试代码；004A 已用最小 transaction converter guard 阻断预算组作为资金价值交易主体，并保留余额控制兼容路径。目标测试文件当前未被 Git 跟踪，因此该结论只作为当前工作树证据，不作为已冻结 Git 基线。阻塞项已迁移为“需要确认新的单一 Execution Grant”。

下一轮如果用户仍只说“继续”或“推进”，不得默认沿用 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`。若要继续编码，必须确认新的单一 Execution Grant；候选可在预算组 control ledger 退出条件、钱包账户/账户层级、交易内核、清结算对账等范围中选择一个切片。

## 11. nextGrantCandidate2026-06-05

本节记录下一候选授权文本，状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。它不是已确认授权；用户确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

```text
Execution Grant：GSD1-LEDGER-BOUND-LEDGER
允许执行：GSD1-LD-RED-002A
目标：为 DefaultLedgerTransactionPostingServiceImpl 补 entry 与绑定账本的账目、币种和负余额约束 Red，证明不匹配 entry 在 LedgerTransactionService.postLedgerTransaction 和 LedgerBalanceProjectionService.project 前失败，不落账本交易、posting plan、ledger entry 或余额投影；2026-06-07 只读源码锚点显示 assertEntryMatchesLedger 已包含账目、币种和 ALLOW_NEGATIVE guard，002A 默认先补目标测试覆盖，不预设生产代码必改
允许写入：tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java
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

候选判断：002A 是当前账本账目队列中最小、最安全的下一步。它不触碰 `BUDGET_GROUP` 可入账主体策略，不引入新表或公共契约，不推进钱包、交易层、支付工具、VCC、全球账户或收单能力。

授权前最新证据：2026-06-07 已复核 Java 21 runtime、Maven reactor compile、当前 `DefaultLedgerTransactionPostingServiceImplTests` 既有 7 tests 和 `assertEntryMatchesLedger` 源码锚点。该证据说明目标测试资产可运行，且生产 guard 可能已存在；但 002A 的三个新 Red 尚未写入，用户确认本节 Execution Grant 前不得把该证据解释为 002A 完成或生产代码可直接修改。最新工作树审计还确认 `DefaultLedgerTransactionPostingServiceImplTests.java` 当前存在但未被 Git 跟踪，授权后必须先接续并保护该文件中的 001A/001B 覆盖，再最小扩展测试夹具以构造 002A 的账目、币种和 `ALLOW_NEGATIVE` mismatch 场景。

## 12. consumedGrantOnePageConfirmation2026-06-07

本节记录 `GSD1-LEDGER-BOUND-LEDGER` 的确认摘要和消费结果。该 Grant 已由用户确认并被 `GSD1-LD-RED-002A` 消费；后续不得沿用本节继续写测试代码、生产实现或 Git 提交。

| 项 | 口径 |
| --- | --- |
| 推荐确认文本 | `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`。 |
| 允许执行 | `GSD1-LD-RED-002A`。 |
| 业务问题 | 账本入账编排入口必须阻断 entry 与绑定 ledger 的账目、币种或负余额约束不一致，避免账本交易、posting plan、ledger entry 或余额投影产生半截事实。 |
| 当前判断 | `assertEntryMatchesLedger` 现有账目、币种和 `ALLOW_NEGATIVE` guard 已满足 002A；本轮只补目标测试覆盖，未修改生产代码。 |
| 首批 Red | 账目 code/category mismatch、currency mismatch、entry `ALLOW_NEGATIVE` 但 bound ledger `allowNegative=false`。 |
| 实际写入 | `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`；已保护既有 001A/001B 测试，再补 002A 三条目标测试。 |
| 条件写入 | 未触发；Red 直接 Green，因此未修改 `ledger/ledger-impl/src/main/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImpl.java`。 |
| 只读参考 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、账本相关生产代码和既有测试资产。 |
| 禁止范围 | 不改公共契约、枚举、DTO、Request、DDL/H2 schema、Mapper、`BUDGET_GROUP` 兼容策略、wallet、transaction 新业务语义、支付工具、VCC、全球账户、收单、清结算对账、governance apply、生产配置、外部协议或敏感数据处理。 |
| 验证命令 | `just mvn-version`、`just compile`、`just test-one DefaultLedgerTransactionPostingServiceImplTests tests`；必要时 `just test-ledger`；提交前 `just pmd` 和 `git diff --check`。 |
| Git 策略 | `summary_only`，不自动 `git add`、不自动 `git commit`。 |
| 停止条件 | Red 需要公共契约、表结构、预算组兼容策略、跨能力域、外部规则、敏感数据、无法表达资金不变量、工具权限失败无法降级或超出写入范围时停止。 |
| 准出记录 | 已回写本文档、`GSD-1-账本账目状态账本.md`、`GSD-1-账本账目Wave1执行计划.md`、TDD README、OpenSpec project 和 Harness tasks。 |
| 验证结果 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` 提权复跑 PASS，10 tests / 0 failures / 0 errors。 |
| Git 策略结果 | `summary_only`；未执行 `git add` 或 `git commit`。 |

确认口径：本 Grant 已消费。下一轮如果用户只说“继续推进”，默认先做账本下一决策；若要继续编码，必须确认新的单一 Execution Grant。

执行结果：已先复核 `git status --short` 和未跟踪测试文件内容，随后最小扩展 helper 与 `TestLedgerEntrySpec`，分别构造 ledger subject code/category、currency 和 `balanceConstraintType=ALLOW_NEGATIVE` 三类 mismatch。三条用例均证明失败发生在 `LedgerTransactionService#postLedgerTransaction` 和 `LedgerBalanceProjectionService#project` 前，最终作为覆盖补齐闭合。

## 13. consumedProjectionRegressionGrant2026-06-07

本节记录 `GSD1-LEDGER-PROJECTION-REGRESSION` 的确认摘要和消费结果。该 Grant 已由用户确认账本下一决策后被 `GSD1-LD-RED-003` 消费；后续不得沿用本节继续写测试代码、生产实现或 Git 提交。

| 项 | 口径 |
| --- | --- |
| 推荐确认文本 | `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION`。 |
| 允许执行 | `GSD1-LD-RED-003`。 |
| 业务问题 | 余额投影失败不能留下半截余额变化；余额事件只能作为观察事件和证据派生，不能成为余额事实源，也不能反写账本交易、posting plan 或 ledger entry。 |
| 当前判断 | 既有 `LedgerBalanceProjectionServiceImplTests` 已覆盖 003 红线；本轮只做源码锚点复核、目标回归和状态登记，不新增生产代码或测试代码。 |
| 覆盖场景 | 事件发布失败不回滚余额事实；事件包含主体、账本、账目、币种、余额前后值、变更额和来源 entry 证据；嵌套上下文为发布时快照；核心权益字段在余额写入前被拒绝；后一个余额桶失败时整批不写。 |
| 实际写入 | 无 Java、测试、DDL/H2 schema、公共契约或运行时配置写入；仅同步文档、OpenSpec/Harness 索引和交接记录。 |
| 条件写入 | 未触发；目标回归直接通过。 |
| 禁止范围 | 不改 `BUDGET_GROUP` 兼容策略、公共契约、枚举、DTO、Request、DDL/H2 schema、Mapper、wallet、transaction 新业务语义、支付工具、VCC、全球账户、收单、清结算对账、governance apply、生产配置、外部协议或敏感数据处理。 |
| 验证命令 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one LedgerBalanceProjectionServiceImplTests tests`。 |
| 验证结果 | PASS，5 tests / 0 failures / 0 errors，reactor `BUILD SUCCESS`。 |
| Git 策略 | `summary_only`，不自动 `git add`、不自动 `git commit`。 |
| 停止条件 | 需要外部事件存储、消息、异步基础设施、公共契约、表结构、预算组兼容策略或无法表达资金不变量时停止。 |
| 下一人工确认点 | `GSD1-LD-RED-004` 的 `BUDGET_GROUP` 可入账主体兼容策略。 |

确认口径：本 Grant 已消费。下一轮如果继续账本账目，必须先确认 `BUDGET_GROUP` 策略；若不处理 004，则应切换到钱包账户/账户层级、交易内核、清结算对账或其他新的单一 Grant。

## 14. nextBudgetGroupCompatGuardGrant2026-06-07

本节记录 004 策略准备后的候选授权原文。该候选已在 2026-06-11 被用户确认并由 `GSD1-LD-RED-004A` 消费；当前只作为历史授权文本保留，不再作为可继续确认或复用的编码入口。

```text
Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD
允许执行：GSD1-LD-RED-004A
目标：确认并保护预算组的迁移期兼容边界。BudgetGroup 目标态不是核心资金记账主体；迁移期只允许作为预算控制账本兼容主体、控制活动和读模型维度，不允许扩大为直接交易、授权交易、出入金、退款、清结算或对账补事实的资金价值主体。
推荐策略：COMPAT_CONTROL_LEDGER_WITH_FREEZE。保留当前预算组 control ledger 初始化、预算额度调账和预算余额查询兼容路径，但冻结为控制语义；后续通过 B5-SR-CONTROL 或等价预算控制投影形成退出条件。
允许写入：优先 tests/src/test/java/com/wind/funds/transaction/application/flow 或 tests/src/test/java/com/wind/funds/dsl 下的目标测试；Red 证明真实缺口后，允许 transaction/transaction-impl 中 route/transaction 入口做最小 guard。
条件写入：只有 Red 证明 BudgetGroup 可进入资金价值路由、直接交易或授权资金主体时，才允许最小生产修复；若现有测试已经证明预算组只在控制账本路径内活动，则登记为覆盖补齐。
只读参考：docs/产品设计、docs/DSL设计、docs/系分设计、docs/TDD设计、openspec、DefaultLedgerTransactionPostingServiceImpl、RouteSubjectSupport、BalanceControlFundsInstructionRouteResolver、FundsBalanceControlInstructionConverter、BudgetGroupServiceImpl、ControlAccountLedgerInitializationTests、FundsBalanceControlFailureFlowTests、PaymentInstrumentRouteDslContractTests。
禁止写入：不删 FundsSubjectType.BUDGET_GROUP；不改 DDL/H2 schema、Entity、Mapper、BudgetGroupServiceImpl 账本初始化、BUDGET_BASIC profile、Spend Rule 生产模型、支付工具、VCC、全球账户、清结算对账、governance apply、公共契约、运行时配置或敏感数据处理。
首批 Red：1. 直接交易或授权交易不得接受 BudgetGroup 作为 payer/payee/funding subject；2. 预算组额度调整仍只能影响 LIMIT/AVAILABLE 控制账本，不能污染资金账户或平台账本；3. DSL/route fixture 中 BudgetGroup 若作为 funding allocation 或控制维度，必须显式标记为 control/scope，不得被写成最终 money subject。
Red 直接 Green：登记为覆盖补齐，回写状态账本和 OpenSpec，不修改生产代码。
验证命令：WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version；WIND_FUNDS_JAVA_HOME=<Java21 home> just compile；目标 just test-one <TestClass> tests；必要时 WIND_FUNDS_JAVA_HOME=<Java21 home> just test-transaction 和 just test-boundary；提交前 WIND_FUNDS_JAVA_HOME=<Java21 home> just pmd 和 git diff --check。
Git 策略：summary_only，不自动 git add，不自动 git commit，除非用户另行确认 auto_commit。
停止条件：需要全局删除 BUDGET_GROUP、改公共契约、改表结构、迁移 BudgetGroupServiceImpl、引入 Spend Rule 生产模型、跨到支付工具/VCC/全球账户/清结算对账，或测试无法表达资金不变量时立即停止。
撤销方式：用户任意时刻说暂停、停止、撤销授权或调整范围即停止自动推进。
```

候选判断：004A 是当前账本账目线的最小风险下一步。它不把预算组重新定义为资金主体，也不一次性删除兼容路径；先把“预算控制账本兼容”和“资金价值主体禁止”用目标测试和最小 guard 固定下来。004A 完成后，若要删除或迁移预算组 control ledger，必须另起 004B 或 B5-SR-CONTROL Grant。

## 15. preGrantRefresh2026-06-11

本节记录 GSD + Goal 续跑后的 004A 授权前刷新。该刷新随后已由第 16 节的消费记录闭合；当前只作为授权前证据保留，不再代表当前状态。

| 检查项 | 当前证据 | 准入影响 |
| --- | --- | --- |
| Source of Truth | 授权前 `openspec/project.md`、`GSD-Goal-生产可用MVP推进计划.md` 和 `GSD-1-账本账目状态账本.md` 均指向 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` / `GSD1-LD-RED-004A`。 | 该候选随后已被用户确认并消费；当前不得沿用 004A，必须确认新的单一 Grant。 |
| 兼容风险锚点 | `DefaultLedgerTransactionPostingServiceImpl#POSTABLE_SUBJECT_TYPES` 仍包含 `BUDGET_GROUP`。 | 004A 的 Red 不能只看文档，需要证明预算组不会被扩大为资金价值主体。 |
| 兼容保留锚点 | `BalanceControlFundsInstructionRouteResolver#resolveAdjust` 仍把预算组路由到预算额度控制路径。 | 004A 不应直接删除预算组控制账本兼容路径，应先保护 `LIMIT/AVAILABLE` 控制语义。 |
| 既有保护资产 | `PaymentInstrumentServiceImplTests` 已保护预算组不能作为真实资金主体绑定；`ControlAccountLedgerInitializationTests` 已保护预算组控制账本初始化不生成账本交易事实。 | 004A 首批 Red 应复用这些边界，再补交易/路由价值主体阻断，而不是重做资源服务测试。 |
| 工作树风险 | 目标测试文件 `DefaultLedgerTransactionPostingServiceImplTests.java` 当前未被 Git 跟踪，且工作树存在多份既有设计文档修改。 | 授权后必须先复核 `git status --short`，并在 Grant 中列明允许写入的未提交文件；未列入文件不能作为冻结基线或 Done 证据。 |

历史确认口径：用户随后已确认 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`，本轮进入 004A 目标 Red 和必要最小 guard，并已由第 16 节消费记录闭合。当前若继续推进，应切换到 `B2-ACCOUNT-HIERARCHY`、`B4-CANONICAL-REPLAY-FAILFAST`、`B7-RECON-DIFFERENCE-MVP`、预算组 control ledger 退出条件或其他新的单一 Grant。

## 16. consumedBudgetGroupCompatGuardGrant2026-06-11

本节记录 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 的确认与消费结果。第 14 节候选授权文本已被用户确认并由本轮 004A 执行消费；后续不得继续沿用该 Grant。

| 项 | 结论 |
| --- | --- |
| Execution Grant | `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`。 |
| 原子任务 | `GSD1-LD-RED-004A`。 |
| 消费状态 | CONSUMED。 |
| 执行结论 | BudgetGroup 目标态仍不是核心资金记账主体；迁移期保留预算控制账本兼容路径，但直接交易和授权交易的资金价值入口拒绝 `BUDGET_GROUP`。 |
| 写入范围 | `FundsDirectTransactionFlowTests`、`FundsAuthorizationTransactionFlowTests`、`FundsTransactionFlowTestSupport`、`FundsDirectTransactionInstructionConverter`、`FundsAuthorizationInstructionConverter`。 |
| 禁止范围执行结果 | 未写 `FundsSubjectType` 枚举删除、DDL/H2 schema、Entity、Mapper、BudgetGroupServiceImpl、BUDGET_BASIC profile、Spend Rule 生产模型、支付工具、VCC、全球账户、清结算对账、governance apply、公共契约、运行时配置或敏感数据处理。 |
| Direct Red/Green | topup / transfer / pay 补 `BUDGET_GROUP` 拒绝用例，目标测试通过。 |
| Authorization Red/Green | authorize 补 `BUDGET_GROUP` 拒绝用例，目标测试通过。 |
| Control ledger 兼容回归 | `FundsBalanceControlFailureFlowTests` 通过，证明预算组控制账本兼容路径仍可用。 |
| 验证结果 | `FundsDirectTransactionFlowTests` 47 tests 通过；`FundsAuthorizationTransactionFlowTests` 29 tests 通过；`FundsBalanceControlFailureFlowTests` 19 tests 通过；`just compile` 通过；`git diff --check` 通过。 |
| Git 策略 | `summary_only`；未执行 `git add` 或 `git commit`。 |
| 下一步 | 重新确认新的单一 Execution Grant；不得沿用 004A Grant 继续写 Java、测试、DDL/H2 schema、公共契约或运行时配置。 |
