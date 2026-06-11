# GSD-1 账本账目 Wave1 执行计划

## 1. 文档定位

本文档承接 `GSD-1-账本账目Round0准入卡.md`，用于把 `GSD1-LEDGER-FACTS-CAD-001` 从 Round 0 准入结论推进为 Wave 1 原子任务包、验证矩阵和恢复入口。当前阶段状态和跨会话恢复入口见 `GSD-1-账本账目状态账本.md`，代码库理解结论见 `GSD-1-账本账目代码库理解结论包.md`，可评审授权文本见 `GSD-1-账本账目ExecutionGrant确认卡.md`。

本文档仍不授权修改生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。`Execution Grant：GSD1-LEDGER-FACTS` 已被 `GSD1-LD-RED-001A` 消费，`Execution Grant：GSD1-LEDGER-IDEMPOTENCY` 已被 `GSD1-LD-RED-001B` 消费，`Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 已被 `GSD1-LD-RED-002A` 消费，`Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 已被 `GSD1-LD-RED-003` 消费，`Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 已被 `GSD1-LD-RED-004A` 消费；后续继续编码必须重新确认新的单一 Execution Grant。

## 2. gsdAdmissionConclusion

| 检查项 | 结论 | 说明 |
| --- | --- | --- |
| 输入成熟度 | `OpenSpec + Harness + Round0 card` | 已有产品、DSL、系分、TDD、OpenSpec、Harness 和源码扫描入口。 |
| GSD 状态 | `WAVE_2_RED_004A_CONSUMED_SUMMARY_ONLY` | 001A、001B 和 002A 均已在当前工作树完成目标测试补齐；003 已完成既有投影强化覆盖回归登记；004A 已完成 BudgetGroup 兼容 guard。目标测试文件当前未被 Git 跟踪。 |
| CAD 状态 | `004A_CONSUMED_SUMMARY_ONLY` | 003 已消费 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION`；004A 已消费 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`；后续继续编码需重新确认新的单一 Grant。 |
| 当前恢复入口 | 无可沿用 Grant | 必须重新确认新的单一 Execution Grant；候选可在预算组 control ledger 退出条件、钱包账户/账户层级、交易内核或清结算对账中选择一个切片。 |
| Git 策略 | `summary_only` | 未确认 `auto_commit` 前，不执行提交。 |
| 状态账本 | `GSD-1-账本账目状态账本.md` | 记录当前状态、决策日志、验证矩阵、handoff 和 Execution Grant 阻塞点。 |
| 代码库理解 | `GSD-1-账本账目代码库理解结论包.md` | 记录真实代码入口、调用关系、既有覆盖、Red 输入和残余风险。 |
| Grant 确认卡 | `GSD-1-账本账目ExecutionGrant确认卡.md` | 记录 001A/001B 已消费事实、验证结果和后续不得沿用旧 Grant 的交接口径。 |
| 最近运行时证据 | `GSD-1-账本账目状态账本.md#21-verificationevidence2026-06-11-004aconsumed` | 2026-06-11 已完成 004A BudgetGroup 兼容 guard；`FundsDirectTransactionFlowTests` 47 tests、`FundsAuthorizationTransactionFlowTests` 29 tests、`FundsBalanceControlFailureFlowTests` 19 tests 和 `just compile` 通过。 |

## 3. contextLedger

| 项 | 内容 |
| --- | --- |
| 业务目标 | 账本账目基础事实可解释：账本交易、posting plan、ledger entry、余额投影和审计证据完整、平衡、可追溯。 |
| 非目标 | 不声明钱包 application facade、交易层新能力、支付工具、VCC、全球账户、收单、清结算对账、治理 apply 或账本余额快照物理落地完成。 |
| 关键源码锚点 | `DefaultLedgerTransactionPostingServiceImpl`、`LedgerTransactionServiceImpl`、`DefaultLedgerPostingAssembler`、`LedgerBalanceProjectionServiceImpl`。 |
| 关键测试锚点 | `DefaultLedgerTransactionPostingServiceImplTests`、`LedgerTransactionServiceImplTests`、`DefaultLedgerPostingAssemblerTests`、`LedgerBalanceProjectionServiceImplTests`、`LedgerServiceImplTests`。 |
| 现有覆盖判断 | 入账编排服务已经由当前工作树中的 001A 覆盖非法交易、非法 posting plan 和非法 entry 的失败无半截事实；001B 已补齐 posting service 入口重复 post 不重复账本事实和余额投影的目标测试；002A 已补齐绑定账本账目、币种和负余额约束目标测试。003 已用既有 `LedgerBalanceProjectionServiceImplTests` 回归余额投影事件非事实源、来源证据、上下文快照、前置阻断和整批失败无半截投影。004A 已补 direct/auth 交易入口拒绝 `BUDGET_GROUP` 作为资金价值主体，并回归余额控制兼容路径。目标测试文件当前未被 Git 跟踪，后续 Grant 开工前必须保护该工作树内容。 |
| 高风险确认点 | 004A 只完成兼容 guard，不等于预算控制目标态退出完成；后续若要迁移或删除 BudgetGroup control ledger、引入 Spend Rule 控制投影、冻结历史兼容路径或做清结算对账补事实，必须另起新的单一 Grant。 |

### 3.1 scopeBoundary

| 范围项 | 内容 |
| --- | --- |
| 写入范围 | `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 已消费；后续只允许写 GSD 文档、OpenSpec/Harness 索引和交接记录，直到用户重新确认新的单一 Grant。 |
| 写入文件 | 002A 已写入当前未被 Git 跟踪的 `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`；004A 已写入 transaction converter 和 transaction flow 测试；后续 Grant 必须先保护既有 001A/001B/002A/003/004A 覆盖。 |
| 只读范围 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、账本账目相关生产代码和测试资产。 |
| 只读参考 | `DefaultLedgerTransactionPostingServiceImpl`、`LedgerTransactionServiceImpl`、`LedgerBalanceProjectionServiceImpl`、`LedgerBalanceProjectionServiceImplTests`、`LedgerServiceImplTests`、`AbstractFundsServiceTest`。 |
| 禁止事项 | 不改公共契约、DDL/H2 schema、Mapper、预算组兼容策略、钱包、交易层新业务语义、支付工具、VCC、全球账户、收单、清结算对账或治理 apply。 |

## 4. wavePlan

| Wave | Task ID | 目标 | Owner | 依赖关系 | 状态 |
| --- | --- | --- | --- | --- | --- |
| Wave 1 | `GSD1-LD-RED-001A` | 为入账编排服务补独立 Red，证明非法交易或非法 posting/entry 在持久化和余额投影前失败。 | `资深架构师` + Codex | `Execution Grant：GSD1-LEDGER-FACTS` 已确认并消费。 | `DONE_COVERAGE_ADDED` |
| Wave 1 | `GSD1-LD-RED-001B` | 探测入账编排的幂等/重复入账或投影跳过行为是否有独立覆盖缺口。 | `资深架构师` + Codex | `Execution Grant：GSD1-LEDGER-IDEMPOTENCY` 已确认并消费。 | `DONE_COVERAGE_ADDED` |
| Wave 2 | `GSD1-LD-RED-002A` | 补入账编排层绑定账本账目、币种和负余额约束目标测试。 | `资深架构师` + Codex | `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 已确认并消费；不触碰 004 的预算组兼容策略。 | `DONE_COVERAGE_ADDED` |
| Wave 2 | `GSD1-LD-RED-003` | 强化余额投影失败无半截变化与事件非事实源。 | `资深架构师` + Codex | 已消费 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION`；既有投影层覆盖回归通过。 | `DONE_EXISTING_COVERAGE_VERIFIED` |
| Wave 2 | `GSD1-LD-RED-004A` | 预算组控制账本兼容 guard：保留预算控制兼容路径，阻断预算组扩大为资金价值主体。 | `资深架构师` + Codex | 已消费 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`。 | `DONE_COMPAT_GUARD_VERIFIED` |

## 5. atomicTaskPackage

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD1-LD-RED-002A` |
| 目标 | 在 `DefaultLedgerTransactionPostingServiceImplTests` 中新增绑定账本细约束目标测试，证明 entry 与绑定 ledger 的账目 code/category、币种和负余额约束不一致时，在账本事实持久化和余额投影前失败；若 Red 直接 Green，则只登记覆盖补齐，不硬改生产代码。 |
| 所属阶段 / Wave | GSD-1 / Wave 2 / 账本账目绑定账本约束。 |
| Owner | `资深架构师` + Codex 自动执行；002A Grant 已由用户确认并消费。 |
| 写入文件 | 已写当前未被 Git 跟踪的 `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`；已保护既有 001A/001B 测试并补 002A。 |
| 只读参考 | `DefaultLedgerTransactionPostingServiceImpl`、`LedgerTransactionServiceImpl`、`LedgerBalanceProjectionServiceImpl`、`LedgerBalanceProjectionServiceImplTests`、`LedgerServiceImplTests`、`AbstractFundsServiceTest`。 |
| 依赖顺序 | 已确认 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`；002A Red 直接 Green，登记覆盖补齐并停止生产改动。 |
| 允许动作 | 本轮已新增目标测试和必要测试夹具；未触发生产代码最小修复条件。 |
| 禁止动作 | 不改 `ledger-face`、`core` 公共契约、枚举、DTO、Request、DDL/H2 schema、Mapper、`BUDGET_GROUP` 兼容策略、钱包、交易层新能力、支付工具、VCC、全球账户、收单、清结算对账或治理 apply。 |
| Superpowers | 执行时遵循 `资深架构师` 的 TDD、Review、Refactor、编码红线和 AI 产物复核要求；先红后绿，CR 本轮差异，测试只断言外显业务事实和资金不变量。 |
| 验收场景 | entry 的账目 code/category、币种或负余额约束与绑定 ledger 不一致时，账本交易、posting plan、ledger entry 和 ledger balance 均不新增、不变化。 |
| 验证命令 | 已执行 `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests`，目标测试 10 tests / 0 failures / 0 errors。 |
| 停止条件 | 需要公共契约、表结构、预算组兼容策略、跨能力域、投影层重构、`LedgerTransactionService` 摘要冲突语义或无法断言资金不变量时停止。 |
| 完成条件 | Red/Green 结论可解释，目标测试和账本回归通过或明确环境失败；交付记录列明是否有生产改动。 |
| 交接要求 | 已回写状态账本、Harness tasks、OpenSpec project 和验证结果；003 与 004A 均已消费，下一轮需重新确认新的单一 Grant。 |
| 恢复入口 | 本文档第 8 节、`GSD-1-账本账目状态账本.md#21-verificationevidence2026-06-11-004aconsumed` 和 `GSD-1-账本账目ExecutionGrant确认卡.md#10-handoff`；下一轮需重新确认新的单一 Grant。 |
| CAD 候选 | 已消费，`summary_only` 收口。 |
| Execution Grant 关联 | 已消费 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`。 |

### 5.1 executionResult

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD1-LD-RED-001B` |
| 执行状态 | `DONE_COVERAGE_ADDED` |
| 授权 | 已消费 `Execution Grant：GSD1-LEDGER-IDEMPOTENCY`。 |
| 实际写入 | `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`；该文件当前未被 Git 跟踪。 |
| 生产代码 | 未修改。 |
| 测试结果 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` 通过，当前工作树 7 tests / 0 failures / 0 errors；不代表目标测试文件已进入冻结 Git 基线。 |
| Red/Green 结论 | 现有生产实现已通过 `LedgerTransactionService` 的 existing/newlyPosted=false 结果短路余额投影；001B 作为覆盖补齐闭合。 |
| 后续口径 | 不能继续沿用 001B 授权；该段为历史 001B 结果。2026-06-07 后 002A 与 003 均已消费，2026-06-11 后 004A 也已消费；下一步必须重新确认新的单一 Grant，或调整到账本以外的账户层级、交易内核、清结算对账等候选。 |

### 5.2 nextGrantCandidate

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD1-LD-RED-002A` |
| 当前状态 | `DONE_COVERAGE_ADDED` |
| 首批 Red | 账目 code/category mismatch、currency mismatch、entry `ALLOW_NEGATIVE` 与绑定 ledger `allowNegative=false` 不一致。 |
| 授权文本 | `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 已由用户确认并消费；后续不得沿用。 |
| 执行结果 | 已补三条目标测试；现有生产守卫直接满足，未修改生产代码；目标测试 10 tests 通过。 |

## 6. verificationMatrix

| 阶段 | 命令 | 预期证据 | 失败处理 |
| --- | --- | --- | --- |
| 准备 | `git status --short` | 工作树变更均属于本任务或已分类。 | 发现无关变更时停止并分类。 |
| 运行时 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version` | Java 21 runtime 与 Maven 可用。 | 区分 Java runtime、私服、网络、凭据或缓存问题。 |
| 编译 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` | 全量编译通过。 | 编译失败先判断是否与本任务相关。 |
| Red/Green | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` | 002A 已证明绑定账本细约束不匹配时无半截账务事实；目标测试 10 tests 通过。 | 后续新 Red 必须重新确认 Grant；触及公共契约、表结构或预算组兼容策略时停止。 |
| Green | 同上 | 目标测试通过，且无半截账务事实。 | 仅允许在 002A Grant 范围内做最小修复；否则停止。 |
| 投影回归 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one LedgerBalanceProjectionServiceImplTests tests` | 003 已证明余额投影事件非事实源、来源证据、上下文快照、核心权益字段前置阻断和整批失败无半截投影；目标回归 5 tests 通过。 | 若需引入外部事件存储、消息或异步基础设施，立即停止并另起 Grant。 |
| BudgetGroup 兼容 guard | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsDirectTransactionFlowTests tests`、`WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsAuthorizationTransactionFlowTests tests`、`WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsBalanceControlFailureFlowTests tests` | 004A 已证明 direct/auth 资金价值入口拒绝 `BUDGET_GROUP`，余额控制兼容路径仍通过；direct 47 tests、auth 29 tests、balance-control failure 19 tests 通过。 | 后续新 Red 必须重新确认 Grant；触及公共契约、表结构、预算组 control ledger 迁移或 Spend Rule 生产模型时停止。 |
| 回归 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-ledger` | 账本服务、posting 装配、账本交易和余额投影回归通过。 | 失败先本轮修复；越界则停止。 |
| 提交前 | `WIND_FUNDS_JAVA_HOME=<Java21 home> just pmd`、`git diff --check` | 规约和空白检查通过。 | 环境失败需说明，代码规约失败需修复。 |

## 7. consumedExecutionGrant

```text
Execution Grant：GSD1-LEDGER-IDEMPOTENCY
允许执行：GSD1-LD-RED-001B
允许写入：tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java；Red 证明真实缺口后，允许 ledger/ledger-impl/src/main/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImpl.java 最小修复
禁止写入：ledger-face、core 公共契约、枚举、DTO、Request、DDL/H2 schema、LedgerTransactionService 摘要冲突语义、wallet、transaction 新业务语义、支付工具、VCC、全球账户、收单、清结算对账、governance apply、生产配置、外部协议和敏感数据处理
验证命令：just mvn-version、just compile、just test-one DefaultLedgerTransactionPostingServiceImplTests tests、必要时 just test-ledger、提交前 just pmd 和 git diff --check
Git 策略：summary_only，除非另行确认 auto_commit
停止条件：公共契约、表结构、预算组兼容策略、跨能力域、外部规则、敏感数据、验证无法表达资金不变量或工具权限失败无法降级
```

该 Grant 已由 `GSD1-LD-RED-001B` 消费完毕，只保留为执行证据。后续不得沿用本授权继续写入 Java、测试、DDL/H2 schema、公共契约或运行时配置。

## 8. handoff

当前 GSD 结论：Wave 1 已完成 `GSD1-LD-RED-001A` 和 `GSD1-LD-RED-001B` 覆盖补齐，Wave 2 已完成 `GSD1-LD-RED-002A` 覆盖补齐、`GSD1-LD-RED-003` 投影强化既有覆盖回归登记和 `GSD1-LD-RED-004A` BudgetGroup 兼容 guard。001A/001B/002A 目标测试文件当前未被 Git 跟踪，完成态只能作为当前工作树证据和后续编辑基底。后续不能沿用 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`、`Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 或 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`，必须确认新的单一 Grant，或切换到钱包账户/账户层级、交易内核、清结算对账等候选。

2026-06-07 运行时补证据：002A 目标测试在沙箱内首次因 embedded Redis 端口绑定受限失败，提权复跑后 10 tests / 0 failures / 0 errors。003 目标投影回归直接运行通过，5 tests / 0 failures / 0 errors。上述证据用于说明当前工作树目标测试资产和既有投影回归可用，不代表 Git 基线已冻结或账本整体生产 Done。
