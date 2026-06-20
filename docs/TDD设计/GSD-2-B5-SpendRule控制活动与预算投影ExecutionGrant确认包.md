# GSD-2 B5 Spend Rule 控制活动与预算投影 Execution Grant 确认包

## 1. 文档定位

本文用于把 `GSD2-B2-SPEND-CONTROL-ADMISSION-001` 之后的 wallet 缺口收敛为下一轮可确认的单一 Execution Grant：`GSD2-B5-SR-CONTROL-ACTIVITY-001`。该 Grant 只面向服务层能力，目标是从“支出控制准入快照”推进到“支出控制活动留痕和预算控制投影最小闭环”。

本文不是新的 PRD，不替代产品设计、DSL 设计、系分设计或 TDD 设计；也不授权 Java、测试、DDL/H2 schema、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 操作或生产发布。若用户确认该 Grant，仍需在编码前按本文第 11 节重新执行消费预检。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B5-SR-CONTROL-ACTIVITY-001` |
| 阶段切片 | B5 Spend Rule 控制活动 / 预算控制投影最小服务层切片。 |
| 关联 Goal | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| 当前基线 | `021ee2ce feat: 补齐支出控制准入快照`。 |
| 当前状态 | `SR_CONTROL_ACTIVITY_GREEN_VERIFIED` |
| Owner | 产品架构专家确认业务目标、对象、规则和验收；资深架构师确认模块职责、接口契约、数据方案、TDD、Review、Refactor、编码红线、验证命令和停止条件；用户确认 Execution Grant 与 schemaDecision。 |
| 写入范围 | 本确认包、GSD-2 入口、LWT Goal、W5 推进计划、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-B5-SpendRule控制活动与预算投影ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、wallet、transaction、ledger、core、reconciliation、tests、Justfile、AGENTS.md、最近 Git 提交和历史准入卡。 |
| 只读参考 | `GSD-2-LWT-生产可用能力Goal.md`、`B2B4-支付工具与SpendRule生产可用性Round0准入卡.md`、`SpendControlAdmissionApplicationService`、`PaymentInstrumentPreTransactionSnapshotApplicationService`、`FundsAccountCapabilityApplicationService`、`FundingResponsibilityResolutionApplicationService`。 |
| Git 策略 | 当前确认包为 `summary_only`；未确认 Git 授权前不提交。若后续用户确认编码并要求提交，必须在 Green / Verify 后另行执行。 |

## 2. 业务目标、用户价值和非目标

业务目标：把 Spend Rule 从“外部决策证据已可参与准入”推进到“控制活动可留痕、可查询、可幂等、可解释，并能形成预算控制投影”的最小生产可用链路。它服务于 VCC、全球账户付款、钱包付款、员工或代理预算控制等场景，但本切片不直接实现这些业务场景。

用户价值：

1. 产品和运营能看到某次支付工具或账户支出为什么被允许、拒绝、占用、释放或过期。
2. 财务和风控能区分“资金余额”与“预算或规则控制占用”，避免把 BudgetGroup 或 Spend Rule 误写成账务主体。
3. 研发和测试能基于可查询活动证明准入、占用、释放、幂等、回放和无账本副作用。

成功指标：

1. Spend Rule 控制活动能按业务流水、支付工具、资金责任目标主体、规则版本、预算组和活动流水查询。
2. 同一活动流水重复提交必须幂等，同流水不同摘要必须拒绝。
3. 预算控制投影只表达控制占用、释放和剩余控制口径，不表达真实资金余额。
4. 查询和记录控制活动不得创建资金交易、route、posting、LedgerEntry、ledger transaction、资金余额投影或交易投影事实。
5. 所有生产代码在显式 Grant 后遵循 TDD 先红后绿、Review、Refactor 和 AI 产物复核流程。

非目标：

1. 不实现 Spend Rule 规则定义、规则引擎、规则编排器或外部规则专业确认。
2. 不实现 VCC facade、全球账户、收单、清分、清算、结算、出款或运营后台。
3. 不把 BudgetGroup、Spend Rule、PaymentInstrument、父账户或交易投影写为 ledger subject。
4. 不修改直接交易、授权交易、余额控制的 canonical 入参，交易内核继续以已解析账户主体为准。
5. 不新增 Controller、HTTP/RPC、消息消费、生产迁移、灰度、告警或法务/合规最终确认。

## 3. 产品架构和能力地图

| 能力域 | 前台能力 | 后台能力 | 数据能力 |
| --- | --- | --- | --- |
| 支出控制准入 | 调用方提交支付工具、动作、金额、业务场景和外部 Spend Rule 决策证据。 | 校验预交易快照、资金责任、账户能力和外部决策结果。 | `SpendControlAdmissionDecisionDTO`、支付工具快照、账户能力快照、资金责任决策。 |
| 控制活动 | 记录允许、拒绝、占用、释放、过期、撤销、退款释放等控制动作。 | 幂等、摘要一致性、活动链路、原因和证据引用。 | activitySn、activityType、businessSn、spendRuleId、spendRuleVersion、spendDecisionSn、spendDecisionDigest、amount、currency、targetAccountId、budgetGroupSn。 |
| 预算控制投影 | 查询预算组或规则维度的控制占用和释放结果。 | 从控制活动派生只读控制余额，不反写资金余额。 | reservedAmount、releasedAmount、remainingControlAmount、lastActivitySn、lastActivityAt。 |
| 交易和账本 | 本切片不写交易或账本，只作为后续交易前后控制证据。 | 若后续交易消费控制活动，必须另起 Grant 连接交易事实。 | 无 route、posting、LedgerEntry、ledger balance 写入。 |

## 4. 业务对象、状态和字段口径

业务对象：

- `SpendControlAdmissionDecision`：既有准入结论，说明支付工具、资金责任目标、账户能力和外部决策证据是否允许本次支出。
- `SpendControlActivity`：新增目标对象，用于记录一次控制事实，例如准入记录、预算占用、预算释放、拒绝、过期、撤销或退款释放。
- `BudgetControlProjection`：新增目标视图，用于从控制活动派生预算控制占用状态。
- `BudgetGroup`：预算范围或控制视图，不是账务主体。
- `SpendRule`：规则和策略版本，不是账务主体。

核心状态：

| 状态或类型 | 含义 | 是否写资金事实 |
| --- | --- | --- |
| `ADMISSION_RECORDED` | 已记录准入结论和外部决策证据。 | 否 |
| `REJECTED_RECORDED` | 已记录拒绝原因和规则证据。 | 否 |
| `RESERVED` | 控制口径已占用预算或额度。 | 否 |
| `RELEASED` | 控制口径已释放占用。 | 否 |
| `EXPIRED` | 控制占用因超时失效。 | 否 |
| `REVERSED` | 控制活动被业务撤销或冲正。 | 否 |

字段口径：

1. `activitySn` 是控制活动幂等流水，不等同交易流水。
2. `businessScene + businessSn` 是业务追溯入口，不替代活动流水。
3. `targetAccountId` 或等价目标主体来自预交易快照，不允许调用方把预算组、Spend Rule 或支付工具直接作为入账主体。
4. `spendRuleId + spendRuleVersion + spendDecisionSn + spendDecisionDigest` 只表达规则决策证据。
5. `budgetGroupSn` 只表达预算控制范围，不触发账本初始化或余额入账。

## 5. 业务流程和规则矩阵

主流程：

1. 调用方先调用既有 `SpendControlAdmissionApplicationService` 获取支出控制准入结论。
2. 若准入失败，可记录 `REJECTED_RECORDED` 控制活动，供运营、风控、客服和审计查询。
3. 若准入通过，可记录 `ADMISSION_RECORDED` 或 `RESERVED` 控制活动，保留规则版本、决策流水、预算组和目标账户主体。
4. 后续交易成功、撤销、退款、过期或人工处理时，通过后续 Grant 记录 `RELEASED / EXPIRED / REVERSED` 等控制活动。
5. 预算控制投影从活动派生，用于查询控制占用和释放，不参与 ledger posting。

异常流程：

1. 缺少准入决策证据时，控制活动记录必须 fail-fast。
2. 同一 `activitySn` 重复提交且摘要一致时返回既有记录；摘要不一致时拒绝。
3. 释放金额不得超过同一控制链路的已占用金额。
4. 找不到原占用活动时不得创建释放投影。
5. 任何失败路径均不得生成资金交易、route、posting、LedgerEntry 或余额投影。

规则矩阵：

| 规则 | 触发条件 | 判断逻辑 | 优先级 | 版本 |
| --- | --- | --- | --- | --- |
| 控制活动主体 | 创建任一控制活动 | 必须解析到资金账户或信用账户目标主体，BudgetGroup 和 Spend Rule 只能作控制维度。 | P0 | GSD2-B5 |
| 幂等 | 同一 `activitySn` 重复提交 | 摘要一致返回既有结果，摘要不一致失败。 | P0 | GSD2-B5 |
| 无账务副作用 | 记录或查询控制活动 | 不创建资金交易、route、posting、LedgerEntry、ledger transaction 或余额投影。 | P0 | GSD2-B5 |
| 预算投影 | 记录占用或释放 | 只更新控制投影，不表达真实资金余额。 | P0 | GSD2-B5 |
| schemaDecision | 进入编码前 | `contract-only` 只落契约 Red；`ddl-backed` 才能声明控制活动和投影具备持久化最小闭环。 | P1 | GSD2 |

## 6. 系统设计和核心决策

核心决策：

1. 落点在 `wallet` application 层，不落在 `transaction` 内核。Spend Rule 控制活动是交易前后控制证据，不是资金交易事实。
2. 服务命名推荐 `SpendControlActivityApplicationService`，不推荐 `InstrumentTransactionService` 或交易层支付工具统一服务。
3. 首轮推荐 `ddl-backed`，因为控制活动和预算控制投影若不可持久化，就无法支撑生产审计、幂等、回放和对账追踪。
4. 若用户选择 `contract-only`，只允许落 face 契约、Request/DTO 和 Red，不声明生产可用闭环。
5. 预算控制投影可先从控制活动最小派生，后续是否拆独立 projection store 需要新的 Grant。

### 6.1 schemaDecision 决策矩阵

`schemaDecision` 是本 Grant 的人工确认点，用于决定下一轮是否可以从契约 Red 进入持久化 Green。未明确选择前，本确认包只允许作为设计交接材料消费，不授权 Java、测试、DDL/H2 schema、Entity、Mapper 或 Git。

| 决策值 | 适用判断 | 授权写入范围 | 可声明交付结果 | 禁止外推范围 |
| --- | --- | --- | --- | --- |
| `ddl-backed`（推荐） | 需要把 Spend Rule 准入证据沉淀为可审计、可幂等、可回放的控制活动，并由活动派生预算控制投影。 | `wallet-face` application/spend 契约、Request/Query/DTO、必要枚举，`wallet-impl` service-flow 实现、最小 Entity/Mapper、`tests` H2 schema 和目标服务流测试。 | 可声明“Spend Rule 控制活动与只读预算控制投影具备服务层最小闭环”，但仍需附带验证证据。 | 不声明完整规则引擎、交易事实消费、清结算联动、运营后台、Controller、HTTP/RPC、生产迁移或告警闭环。 |
| `contract-only` | 只需要先固化外部调用契约和首个 Red，暂不承诺持久化、幂等重放和投影查询。 | `wallet-face` 契约、Request/Query/DTO、必要枚举、目标 Red 和文档状态回写。 | 只能声明“契约和缺口已明确”，不能声明生产可用服务闭环。 | 不允许新增 Entity、Mapper、DDL/H2 schema、持久化实现、预算投影 Green 或 Git 自动提交。 |
| `defer` | 需要继续讨论产品语义、账务边界或表设计，暂不进入编码。 | 文档、只读 Gap Audit、任务计划和停止条件回写。 | 只能声明“准入材料继续完善”。 | 不授权任何生产代码、测试代码、公共契约、DDL/H2 schema 或运行时配置。 |

本轮推荐选择：

```text
Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001
schemaDecision：ddl-backed
```

`ddl-backed` 的首轮最小存储边界限定为 `t_spend_control_activity`。`BudgetControlProjectionDTO` 首轮由控制活动聚合派生，不单独新增 `t_budget_control_projection`；若后续需要独立 projection store、异步投影、批量重算或性能优化，需要新的 Execution Grant。

接口契约候选：

| 契约 | 入参 | 出参 | 错误码或失败路径 | 幂等 |
| --- | --- | --- | --- | --- |
| `recordActivity` | `RecordSpendControlActivityRequest` | `SpendControlActivityDTO` | 缺目标主体、缺准入证据、非法金额、重复摘要不一致。 | `tenantId + activitySn` |
| `queryActivities` | `SpendControlActivityQuery` | `List<SpendControlActivityDTO>` | 查询条件非法或越权。 | 只读 |
| `getBudgetControlProjection` | `BudgetControlProjectionQuery` | `BudgetControlProjectionDTO` | 缺预算组或币种条件时拒绝。 | 只读 |

候选包和文件落点：

| 类型 | 推荐路径 |
| --- | --- |
| face service | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/spend/SpendControlActivityApplicationService.java` |
| request/query/dto | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/model/request`、`model/query`、`model/dto` |
| enum | `core/src/main/java/com/wind/funds/wallet/enums` 或 wallet-face 当前枚举落点，需遵循现有依赖方向。 |
| impl | `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application/spend/impl` |
| entity/mapper | 仅在 `schemaDecision=ddl-backed` 时进入 `wallet/wallet-impl`。 |
| H2 schema | 仅在 `schemaDecision=ddl-backed` 时更新 `tests/src/test/resources/jdbc-schema.sql`。 |
| tests | `tests/src/test/java/com/wind/funds/wallet/application/spend/SpendControlActivityApplicationServiceTests.java` |

### 6.2 只读源码 Gap Audit（2026-06-19）

| 源码锚点 | 当前证据 | 对本 Grant 的结论 |
| --- | --- | --- |
| `SpendControlAdmissionApplicationService` | 仅暴露 `resolveSpendControlAdmission`，职责是组合支付工具预交易快照与外部 Spend Rule 决策证据。 | 已具备控制活动的上游准入快照来源，但不是控制活动记录服务。 |
| `ResolveSpendControlAdmissionRequest` / `SpendControlAdmissionDecisionDTO` | 已包含 `spendRuleId`、`spendRuleVersion`、`spendDecisionSn`、`spendDecisionDigest`、`budgetGroupSn`、`targetAccountId` 和预交易快照。 | 可作为 `RecordSpendControlActivityRequest` 的来源证据；不应重复设计一套规则计算入参。 |
| `SpendControlAdmissionApplicationServiceImpl` | `@Transactional(readOnly = true)`，只调用 `PaymentInstrumentPreTransactionSnapshotApplicationService` 并返回决策 DTO。 | 当前实现没有持久化 activity、预算占用、释放或投影；首个 Red 应证明这一缺口。 |
| `SpendControlAdmissionApplicationServiceTests` | 已覆盖规则通过、规则拒绝、缺少规则证据，并断言不创建资金交易、route、posting、LedgerEntry、账本交易或余额投影。 | 可作为 B5 Green 后的回归护栏；新增控制活动测试必须继续证明无资金事实副作用。 |
| 字段命名对齐 | 既有准入 Request / DTO 已采用 `spendRuleId`、`spendRuleVersion`、`spendDecisionSn` 和 `spendDecisionDigest`。 | 下一轮 `RecordSpendControlActivityRequest` 应沿用该命名；`ruleId`、`ruleVersion`、`decisionSn`、`decisionDigest` 仅可作为产品概念简写，不作为代码字段建议。 |
| `tests/src/test/resources/jdbc-schema.sql` | 当前未发现 `t_spend_control_activity` 或 `t_budget_control_projection`。 | `ddl-backed` 时首轮必须补 `t_spend_control_activity` 最小 H2 schema；预算控制投影建议先由活动聚合派生，除非 Grant 显式列名独立投影表。 |
| `wallet` / `transaction` / `ledger` / `core` 源码搜索 | 当前未发现 `SpendControlActivityApplicationService`、`RecordSpendControlActivityRequest`、`SpendControlActivityDTO`、`BudgetControlProjectionDTO` 或对应 Mapper / Entity。 | 首个 Red 应落在服务层契约缺失或 Spring 注入缺失，不从 transaction 或 ledger 反向补事实。 |
| `Justfile` | 已有 `test-one`、wallet application 相关目标测试和 `test-boundary` / `test-balance-control` 分组；没有单独 Spend Control 分组。 | 本 Grant 初期使用 `just test-one SpendControlActivityApplicationServiceTests tests` 和组合回归即可；是否新增分组不作为首轮必要条件。 |

### 6.3 ddl-backed 可实现性审计（2026-06-19）

本节只说明仓库已有结构是否支撑推荐的 `schemaDecision=ddl-backed`，不代表当前已授权新增 Java、测试、DDL/H2 schema、Entity 或 Mapper。

| 审计项 | 当前证据 | 对首轮实现的约束 |
| --- | --- | --- |
| face 包结构 | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/spend`、`model/request`、`model/query`、`model/dto` 已存在。 | `SpendControlActivityApplicationService` 和配套 Request / Query / DTO 可沿用现有 wallet application facade 结构，不需要新增模块或反向依赖。 |
| impl 包结构 | `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application/spend/impl` 已承载 `SpendControlAdmissionApplicationServiceImpl`。 | 新实现应落同包，优先消费既有准入 DTO 和预交易快照，不搬到 `services/impl` 资源服务层。 |
| DAL 包结构 | `wallet-impl` 已有 `dal/entities` 与 `dal/mapper`，现有 `PaymentInstrument`、`FundingAccount` 等 Entity 使用 `@Table`、`@Id`、`@Column(tenantId = true)` 和中文字段注释，Mapper 使用 MyBatis Flex `BaseMapper`。 | `ddl-backed` 首轮若新增 `SpendControlActivity` Entity / Mapper，应复用该样板；Entity 字段必须有中文注释，Mapper 只暴露必要查询，不使用 `LambdaQueryWrapper`。 |
| H2 schema | `tests/src/test/resources/jdbc-schema.sql` 已包含 funding / credit / budget / instrument / transaction / ledger / reconciliation 表，当前没有 `t_spend_control_activity`。 | 首轮只补 `t_spend_control_activity`；不补 `t_budget_control_projection`，预算控制投影从活动聚合派生。 |
| 现有准入实现 | `SpendControlAdmissionApplicationServiceImpl` 使用 `AssertUtils` 做入参强约束，`@Transactional(readOnly = true)` 调用预交易快照并返回准入 DTO。 | 控制活动写入实现应使用 `@Transactional(rollbackFor = Exception.class)`；查询和投影使用只读事务；校验沿用 `AssertUtils`。 |
| 测试样板 | wallet application 测试已继承 `AbstractFundsServiceTest`，并能断言 `t_funds_transaction`、route、posting、`t_ledger_transaction`、`t_ledger_entry` 和余额投影无新增事实。 | `SpendControlActivityApplicationServiceTests` 必须复用该无副作用断言方式，不用 mock 代替内部 Spring Bean。 |

Red 准入调整：

1. `SpendControlActivityApplicationServiceTests` 首个 Red 必须复用或模拟既有 `SpendControlAdmissionDecisionDTO`，证明“准入结论可被记录为控制活动”，而不是重新实现 Spend Rule 规则计算。
2. `RecordSpendControlActivityRequest` 的 MVP 字段以可审计和幂等为准：`tenantId`、`activitySn`、`activityType`、`businessScene`、`businessSn`、`targetAccountId`、`amount`、`currency`、`spendRuleId`、`spendRuleVersion`、`spendDecisionSn`、`spendDecisionDigest`、`budgetGroupSn` 和活动摘要；不引入专业确认、运营审批、外部规则引擎或交易内核字段。
3. `BudgetControlProjectionDTO` 首轮只表达控制口径的 `reservedAmount`、`releasedAmount`、`remainingControlAmount`、`lastActivitySn` 和 `lastActivityAt`，不得包含 ledger balance、available balance 或 frozen balance 字段。
4. 幂等 Red 必须覆盖同一 `tenantId + activitySn` 重放：摘要一致返回既有活动，摘要不一致拒绝，且两类路径均无资金事实副作用。
5. 若实现过程中发现需要修改 `FundsDirectTransactionService`、`FundsAuthorizationTransactionService`、`FundsBalanceControlService`、ledger subject、BudgetGroup ledger 兼容语义或任何 Controller / HTTP / RPC，立即停止并重新申请 Grant。

### 6.4 编码样板和首轮切片边界（只读复核）

本节只用于后续编码前交接，不代表当前已获得 Java、测试、DDL/H2 schema、Entity、Mapper 或 Git 授权。样板来自当前仓库已完成的 `SpendControlAdmissionApplicationService`、`PaymentInstrumentCapabilityApplicationService`、`FundsAccountCapabilityApplicationService`、`FundingResponsibilityResolutionApplicationService` 和 `ReconciliationDifferenceReportApplicationService`。

应用服务样板：

1. face 契约放在 `wallet-face` 的 `application/spend` 包，方法以业务用例命名，MVP 保留 `recordActivity`、`queryActivities` 和 `getBudgetControlProjection`；不使用 default 方法，除非能证明默认实现不会隐藏业务失败路径。
2. Request、Query、DTO 放在既有 `wallet-face` model 包，字段使用 Jakarta Validation 表达外部输入边界；实现层仍使用 `AssertUtils` 做可信前最后一道强约束。
3. impl 放在 `wallet-impl/application/spend/impl`，写入方法使用 `@Transactional(rollbackFor = Exception.class)`，查询和投影方法使用 `@Transactional(readOnly = true)`。
4. 服务只消费既有准入结论、规则证据和目标账户主体，不重新计算 Spend Rule，不调用 transaction 或 ledger 写接口，不创建 route、posting、LedgerEntry、ledger transaction、余额投影或交易投影。

`ddl-backed` 持久化样板：

1. 首轮只允许新增 `t_spend_control_activity`；`BudgetControlProjectionDTO` 从活动聚合派生，不新增 `t_budget_control_projection`，除非用户另行确认新的 projection store Grant。
2. Entity 放在 `wallet-impl` 的 DAL entity 包，Mapper 放在 DAL mapper 包；遵循 MyBatis Flex `@Table`、`BaseMapper`、`insertSelective` 和 `XxxRefs` 查询样板，不使用 `LambdaQueryWrapper`。
3. Entity 成员字段必须有中文注释，说明字段用途、幂等边界、审计意义或来源限制；不得用注释替代字段命名。
4. 唯一约束建议为 `tenant_id + activity_sn`，查询索引至少覆盖业务流水、目标账户主体、预算组、规则和活动时间；字段不存储外部账户敏感原文。

首轮服务流测试样板：

1. 测试类落在 `tests/src/test/java/com/wind/funds/wallet/application/spend/SpendControlActivityApplicationServiceTests.java`，继承 `AbstractFundsServiceTest`，优先使用真实 Spring Bean 和 H2 schema。
2. 每个资金安全相关断言都需要证明无副作用：控制活动记录、拒绝记录、幂等重放、摘要冲突和预算投影查询均不得新增资金交易、route、posting、LedgerEntry、ledger transaction 或余额投影事实。
3. 首轮至少覆盖允许准入活动、拒绝准入活动、同流水同摘要幂等、同流水不同摘要拒绝、预算控制投影从活动派生五类场景。
4. 若测试需要构造支付工具、资金责任或账户能力上下文，优先复用既有 wallet application 测试样板；不得为了测试方便新增内存版生产 Service。

首轮停止条件：

1. 需要交易内核消费控制活动、修改直接交易 / 授权交易 / 余额控制 canonical 入参、调整 ledger subject 或 BudgetGroup ledger 兼容策略。
2. 需要新增 Controller、HTTP/RPC、消息消费、运营后台、导出、生产迁移、灰度、告警或外部规则引擎。
3. 需要 `t_budget_control_projection` 独立投影表、异步投影、批量重算或投影补偿。
4. 目标测试无法证明控制活动和预算投影无资金事实副作用。

数据方案：

- `ddl-backed` 推荐最小表为 `t_spend_control_activity`，首轮可不单独建 projection 表，而是由活动聚合派生预算控制投影。
- 如果首轮性能或查询复杂度需要投影表，必须在 Grant 中显式列名 `t_budget_control_projection`，并补幂等和一致性测试。
- 事务边界应限定在控制活动和控制投影自身，不跨 transaction 或 ledger 事实。
- 一致性以活动流水幂等、摘要校验和投影派生为核心；失败时不做资金补偿，因为本切片不写资金事实。

可靠性、安全和审计：

- 审计字段至少包含租户、活动流水、业务场景、业务流水、规则版本、决策流水、目标账户、预算组、活动类型、金额、币种、原因和创建时间。
- 安全边界是不得暴露外部账户敏感原文，不得让调用方传入 ledger subject 外的伪主体。
- 告警和生产监控不在本切片实现，但需要在 Not Done 中保留。

## 7. Spec / AC / Red Card

| AC ID | 验收场景 | Given | When | Then |
| --- | --- | --- | --- | --- |
| `AC-SR-ACT-001` | 记录通过准入后的控制活动 | 已存在支付工具预交易快照和外部 Spend Rule 允许决策 | 记录 `ADMISSION_RECORDED` 或 `RESERVED` 活动 | 可查询活动，字段完整，无资金事实副作用。 |
| `AC-SR-ACT-002` | 记录拒绝准入活动 | 外部 Spend Rule 决策为拒绝 | 记录 `REJECTED_RECORDED` 活动 | 可查询拒绝原因和规则证据，无资金事实副作用。 |
| `AC-SR-ACT-003` | 幂等和摘要冲突 | 同一活动流水已存在 | 重放相同摘要或不同摘要 | 相同摘要返回既有记录，不同摘要拒绝且无副作用。 |
| `AC-SR-PROJ-001` | 预算控制投影 | 已存在占用和释放活动 | 查询预算组控制投影 | 返回占用、释放和剩余控制口径，不更新账本余额。 |

首批 Red：

| Red ID | 目标 | 最小失败证明 |
| --- | --- | --- |
| `RED-GSD2-B5-SR-CONTROL-ACTIVITY-001` | 证明当前缺少控制活动持久化和查询能力。 | 新增 `SpendControlActivityApplicationServiceTests`，调用目标服务记录通过准入活动，当前编译或 Spring 注入失败。 |
| `RED-GSD2-B5-SR-CONTROL-ACTIVITY-002` | 证明拒绝准入活动可留痕且无资金副作用。 | 当前没有服务或持久化记录可返回拒绝原因。 |
| `RED-GSD2-B5-BUDGET-PROJECTION-001` | 证明预算控制投影可从活动派生。 | 当前无法查询控制占用和释放后的只读投影。 |

## 8. 验证方案

显式 Grant 后的建议验证顺序：

1. `just test-one SpendControlActivityApplicationServiceTests tests`
2. `just test-one SpendControlAdmissionApplicationServiceTests tests`
3. `just test-one SpendControlActivityApplicationServiceTests,SpendControlAdmissionApplicationServiceTests,PaymentInstrumentPreTransactionSnapshotApplicationServiceTests tests`
4. 若触碰 H2 schema、Entity、Mapper 或投影持久化，追加 `just test-balance-control` 和 `just test-boundary`。
5. `just compile`
6. `just pmd`
7. `git diff --check`

文档-only 验证命令：

1. `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-B5-SpendRule控制活动与预算投影ExecutionGrant确认包.md`
2. `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-B5-SpendRule控制活动与预算投影ExecutionGrant确认包.md`
3. `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-B5-SpendRule控制活动与预算投影ExecutionGrant确认包.md`
4. `git diff --check`

### 8.1 首轮编码 Runbook（授权后执行）

本节只定义后续获得 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001` 与 `schemaDecision` 后的首轮动作顺序，不代表当前已授权编码。

| 步骤 | Maker 动作 | Checker 口径 | 停止点 |
| --- | --- | --- | --- |
| 1. Pick | 读取本确认包、LWT Goal、W5 推进计划、OpenSpec tasks、`SpendControlAdmissionApplicationServiceTests` 和 `tests/src/test/resources/jdbc-schema.sql`。 | 确认当前工作树可解释，且用户已明确 `schemaDecision=ddl-backed` 或 `contract-only`。 | 未确认 `schemaDecision`、目标文件有冲突或需要 Git 历史操作时停止。 |
| 2. Red | 新增 `SpendControlActivityApplicationServiceTests`，先写“通过准入活动可记录并查询，且无资金事实副作用”的失败用例。 | Red 必须因服务契约、DTO、实现或 schema 缺失失败，不得通过放宽断言制造假红。 | Red 不能证明控制活动缺口，或必须修改交易/账本内核才能成立时停止。 |
| 3. Contract | 按 `schemaDecision` 新增 `SpendControlActivityApplicationService`、`RecordSpendControlActivityRequest`、`SpendControlActivityQuery`、`BudgetControlProjectionQuery`、`SpendControlActivityDTO` 和 `BudgetControlProjectionDTO`。 | 字段沿用 `spendRuleId`、`spendRuleVersion`、`spendDecisionSn`、`spendDecisionDigest`；不新增 default 方法；Request/Query 使用 Jakarta Validation。 | 需要 Controller、HTTP/RPC、消息、规则引擎或交易 canonical 入参时停止。 |
| 4. Green | `ddl-backed` 时补 `t_spend_control_activity`、Entity、Mapper 和 wallet-impl 服务；`contract-only` 时只补契约 Red，不写持久化 Green。 | `ddl-backed` 首轮只允许一张活动表；预算投影从活动聚合派生；实现使用 `AssertUtils`、`@Transactional` 和 MyBatis Flex 既有样板。 | 需要 `t_budget_control_projection`、异步投影、批量重算或生产迁移时停止。 |
| 5. Extend | 补拒绝活动、同流水同摘要幂等、同流水不同摘要拒绝、预算控制投影派生测试。 | 每个测试都必须断言无资金交易、route、posting、LedgerEntry、ledger transaction 或余额投影副作用。 | 任何失败路径写入资金事实、账本事实或交易事实时停止。 |
| 6. Review / Refactor | 只在本 Grant 范围内整理命名、字段注释、DTO 构造和 mapper 查询；不做旁路重构。 | 架构师 CR 先看资金主体、无账务副作用、幂等、失败路径、字段注释和模块依赖方向。 | 发现 BudgetGroup、Spend Rule、PaymentInstrument 或父账户被写为 ledger subject 时停止。 |
| 7. Verify | 按第 8 节执行目标测试、准入回归、组合回归、compile、pmd 和 `git diff --check`。 | 验证结果必须写回 LWT Goal 和 OpenSpec tasks；失败需说明是否可在 Grant 范围内修复。 | 验证失败且无法在本 Grant 范围修复时停止。 |
| 8. Handoff | 回写 Not Done、验证证据、提交建议和下一候选。 | 只声明 Spend Rule 控制活动与预算控制投影最小服务层能力，不声明完整规则引擎或生产发布。 | 用户要求提交但验证未过，或要求扩 VCC / B7 / Controller / 生产迁移时停止。 |

最小 Green 判定：

1. `recordActivity` 能记录通过和拒绝两类控制活动。
2. `queryActivities` 能按活动流水、业务流水、目标账户、预算组或规则证据查询。
3. `getBudgetControlProjection` 能从 `RESERVED` 与 `RELEASED` 活动派生控制口径投影。
4. `tenantId + activitySn` 幂等稳定，摘要冲突拒绝。
5. 目标测试能证明全链路无资金事实、交易事实、route、posting、LedgerEntry、ledger transaction 或余额投影副作用。

## 9. 风险、待确认和发布边界

| 风险 | 影响 | 待确认方 | 建议 |
| --- | --- | --- | --- |
| `schemaDecision` 未确认 | 无法判断是否可声明生产可用闭环。 | 用户、资深架构师。 | 推荐 `ddl-backed`，否则只做 contract-only。 |
| 预算控制投影被误当资金余额 | 会造成资金可用性判断错误。 | 产品架构专家、资深架构师。 | 字段命名必须带 control/projection 语义，并禁止 ledger posting。 |
| Spend Rule 规则引擎范围膨胀 | 单一 Grant 失控。 | 产品架构专家。 | 本切片只消费外部规则证据，不做规则计算。 |
| 控制活动和交易事实强耦合 | 破坏 transaction canonical 入参和 ledger 边界。 | 资深架构师。 | 活动留痕先独立完成，交易消费需后续 Grant。 |
| 生产发布误判 | 未具备迁移、灰度、告警和运营后台。 | 发布 owner。 | 本切片只到服务层验证，不发布生产。 |

发布、灰度、回滚和告警：

- 本确认包不发布、不灰度、不上线。
- 后续若进入编码，回滚方式是撤销该 Grant 的新增服务、表和测试变更，不能修改既有交易或账本事实。
- 生产告警、Runbook、运营后台和数据迁移需要新的 production-change 任务卡。

## 10. Harness / CAD 约束

编码红线：

1. TDD 先红后绿；未出现目标 Red 不写 Green。
2. Review 必须先看资金主体、无账务副作用、幂等、失败路径和测试断言。
3. Refactor 只允许在本 Grant 范围内进行，不做旁路整理。
4. AI 产物复核必须确认类名、包名、依赖方向、字段注释、AssertUtils 约规和现有编码风格。
5. 若发现需要修改交易 canonical 请求、ledger subject、BudgetGroup ledger 语义、Controller、HTTP/RPC 或生产迁移，立即停止。

停止条件：

1. 用户未确认 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001`。
2. 用户未确认 `schemaDecision=ddl-backed` 或 `contract-only`。
3. 需要 Java、测试、公共契约、DDL/H2 schema、Entity、Mapper、运行时配置或 Git，但 Grant 未列名授权。
4. 发现 BudgetGroup、Spend Rule、PaymentInstrument 或父账户被写为 ledger subject。
5. 验证失败且无法在授权范围内修复。
6. 需要联网、生产配置、真实资金、外部规则、法务、合规、会计或税务确认。

## 11. Execution Grant 消费预检清单

用户如要进入编码，应确认以下授权文本之一。

推荐授权文本：

```text
Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001
schemaDecision：ddl-backed
范围：新增 wallet 支出控制活动与预算控制投影最小服务层能力，允许写 wallet-face application/spend 契约、Request/Query/DTO、必要 enum、wallet-impl application/spend 实现、必要 Entity/Mapper、tests H2 schema、目标服务流测试和状态文档回写。
禁止：不写 Controller、HTTP/RPC、交易内核支付工具入参、Spend Rule 规则引擎、VCC facade、清结算、补事实、生产迁移或 Git push。
```

保守授权文本：

```text
Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001
schemaDecision：contract-only
范围：只新增 Spend Rule 控制活动和预算投影 face 契约、Request/Query/DTO、目标 Red 和状态文档回写，不声明生产可用闭环。
禁止：不写 DDL/H2 schema、Entity、Mapper、实现、Controller、HTTP/RPC、交易内核支付工具入参、规则引擎、VCC facade、清结算、补事实、生产迁移或 Git push。
```

预检通过口径：

1. `git status --short` 可解释，无目标文件冲突。
2. 当前基线仍为 `021ee2ce feat: 补齐支出控制准入快照` 或用户明确接受后续提交差异。
3. 首个 Red 落在 `SpendControlActivityApplicationServiceTests`。
4. 写入范围和 `schemaDecision` 与用户授权一致。
5. 验证命令和停止条件已写入本确认包。

## 12. 三卡交接和 Coding Loop Contract

### 12.1 Product Context Card

| 字段 | 内容 |
| --- | --- |
| 产品目标 | 将既有支出控制准入结论沉淀为可查询、可幂等、可解释的 Spend Control Activity，并从活动派生预算控制投影。 |
| 核心对象 | `SpendControlAdmissionDecision`、`SpendControlActivity`、`BudgetControlProjection`、BudgetGroup 上下文、Spend Rule 规则证据。 |
| 业务不变量 | BudgetGroup、Spend Rule、PaymentInstrument、父账户和外部账户不得成为 ledger subject；控制活动只表达规则、预算和审计控制事实，不表达资金交易事实。 |
| 验收种子 | 通过准入可记录活动；拒绝准入可记录拒绝原因；同流水同摘要幂等；同流水不同摘要拒绝；预算投影只展示控制占用、释放和剩余；全路径无资金事实副作用。 |
| 产品 Not Done | Spend Rule 规则引擎、规则配置后台、运营审批、交易消费控制活动、VCC facade、完整业务策略准入、生产告警和发布 Runbook。 |
| 待确认 | `schemaDecision=ddl-backed` 或 `contract-only`。若要声明生产可用的留痕、幂等和审计查询，推荐 `ddl-backed`。 |

### 12.2 Engineering Handoff Card

| 字段 | 内容 |
| --- | --- |
| 当前状态 | `CAD Candidate consumed / SR_CONTROL_ACTIVITY_GREEN_VERIFIED`。 |
| Goal ID | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18`。 |
| Task ID | `GSD2-B5-SR-CONTROL-ACTIVITY-001`。 |
| 状态载体 | 本确认包、LWT Goal、W5 推进计划、GSD-2 工作流入口、TDD README、docs README、OpenSpec tasks。 |
| 写入范围 | 用户确认后，按 `schemaDecision` 写 wallet-face application/spend 契约、Request/Query/DTO、必要 enum、wallet-impl application/spend 实现、必要 Entity/Mapper、tests H2 schema、目标服务流测试和状态文档回写。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、wallet、transaction、ledger、core、reconciliation、tests、Justfile、AGENTS.md 和最近 Git 提交。 |
| 首个 Red | `SpendControlActivityApplicationServiceTests` 证明当前缺少控制活动记录、查询、幂等和预算控制投影能力。 |
| 验证命令 | `just test-one SpendControlActivityApplicationServiceTests tests`、`just test-one SpendControlAdmissionApplicationServiceTests tests`、必要组合回归、`just compile`、`just pmd`、`git diff --check`。 |
| 授权缺口 | 未确认 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001` 与 `schemaDecision` 前，不进入 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper 或 Git。 |
| Git 策略 | 当前 `summary_only`；若后续用户要求提交，只能在 Red / Green / Verify 通过后提交本 Task 独立切片。 |
| 停止条件 | 需要修改交易 canonical 入参、ledger subject、BudgetGroup ledger 兼容语义、Controller、HTTP/RPC、生产迁移、外部规则最终结论或真实资金操作时停止。 |
| 路由 | AI Native 负责 Loop 准入和状态回写；产品架构专家复核业务对象、验收和 Not Done；资深架构师消费本卡进入 TDD、CAD Mode、源码级 CR 和验证。 |

### 12.3 Production Loop Card

| 字段 | 当前口径 |
| --- | --- |
| 生产目标 | 仅形成服务层最小可验证能力：Spend Rule 控制活动可持久化、可查询、可幂等，预算控制投影可只读解释。 |
| 非目标 | 不发布生产、不开放 Controller / HTTP / RPC、不接入真实外部规则引擎、不做生产迁移、不做运营后台。 |
| Maker | 后续编码由资深架构师视角按 TDD 执行最小实现。 |
| Checker | 目标服务流测试、无资金事实副作用断言、结构检查、PMD、架构师 CR 和产品验收种子复核。 |
| 观测 / 审计 | 首轮只在控制活动表或契约中保留审计字段；生产监控、告警、Runbook 和运营审计后台另起 production-change 任务。 |
| 人工接管 | `schemaDecision`、规则引擎范围、生产迁移、运营审批、合规/财务/会计解释和外部规则最终确认必须人工接管。 |
| 理解债检查 | 交接时必须能解释控制活动、预算控制投影、资金交易事实和账本事实四者区别。 |
| 恢复入口 | 本节、本文第 11 节授权文本、LWT Goal 第 8.1 节单一 Grant 决策账本和 OpenSpec tasks。 |

### 12.4 Coding Loop Contract

| 字段 | 内容 |
| --- | --- |
| 当前阶段 | 等待用户确认 Grant 与 `schemaDecision`；只能做只读 Gap Audit 或 docs-only 状态维护。 |
| TDD 顺序 | 先写 `SpendControlActivityApplicationServiceTests` 首个 Red，再按 `schemaDecision` 补契约、实现、持久化和投影。 |
| 实现限制 | 不复算 Spend Rule；只消费既有准入结论和外部规则证据；不写 transaction / ledger 事实；不把预算控制投影命名成余额。 |
| 验证者 | 目标测试、既有支出控制准入回归、资金事实副作用断言、结构检查和人工 CR。 |
| 失败回写 | Red 不成立、写入范围漂移或 `schemaDecision` 不匹配时，回写本确认包第 9 至 11 节和 LWT Goal 第 8.1 节。 |
| 最大轮次 | 单个 Grant 内按 Red / Green / Review / Verify / Handoff 一轮收口；连续两轮没有新增验证证据时停止。 |
| 提交切片 | `GSD2-B5-SR-CONTROL-ACTIVITY-001` 单独提交，不能混入 VCC、B7 报告扩展、交易投影、清结算或 Controller。 |

## 13. 交接和恢复入口

交接结论：本确认包把 wallet 下一步默认候选从“完整 Spend Rule 控制活动 / 预算控制投影”的泛化描述收敛为 `GSD2-B5-SR-CONTROL-ACTIVITY-001`。默认推荐 `schemaDecision=ddl-backed`，因为生产可用的控制活动和预算控制投影需要持久化、幂等和可审计查询；若用户只希望低风险先行，可选择 `contract-only`。

恢复入口：

1. 本文第 1 至 3 节确认目标、范围、Owner 和能力地图。
2. 第 6 至 8 节确认工程落点、接口契约、Red 和验证命令。
3. 第 10 至 12 节确认 CAD 约束、停止条件、可复制授权文本、三卡交接和 Coding Loop Contract。
4. LWT Goal、W5 推进计划、GSD-2 工作流和 OpenSpec tasks 只记录当前准备态，不替代用户确认。

残余风险：即使 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 后续 Green，也只能声明 Spend Rule 控制活动和预算控制投影最小服务层能力达成；完整规则引擎、业务策略准入、VCC facade、交易消费控制活动、运营后台、生产迁移、灰度、监控和告警仍为 Not Done。

### 13.1 2026-06-20 授权前置只读复核结论

本节记录进入实现前的确认包可消费性复核，用于说明当时的首个 Red 和写入范围来源；当前实现状态以 13.2 执行记录为准。

复核结论：

1. 授权前工作树可解释：当时未发现 Java、SQL、POM 或 Justfile 未提交变更；未提交差异仍是 B5 设计、任务和恢复入口文档对齐。
2. 授权前源码缺口成立：当时 `wallet`、`core`、`transaction`、`ledger`、`reconciliation` 和 `tests` 中未发现 `SpendControlActivityApplicationService`、`RecordSpendControlActivityRequest`、`SpendControlActivityDTO`、`BudgetControlProjectionDTO`、`t_spend_control_activity` 或 `t_budget_control_projection`，因此首个 Red 可以落在服务层契约、Spring 注入或 schema 缺失。
3. 上游字段口径仍一致：既有 `ResolveSpendControlAdmissionRequest` 和 `SpendControlAdmissionDecisionDTO` 继续使用 `spendRuleId`、`spendRuleVersion`、`spendDecisionSn`、`spendDecisionDigest`、`budgetGroupSn` 和 `targetAccountId`。
4. 结构门禁已通过：本文的 Harness、产品架构和系统架构结构检查通过，`git diff --check` 通过。
5. 随后用户已确认 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001` + `schemaDecision：ddl-backed`，本确认包已被 13.2 的实现和验证记录消费。

### 13.2 2026-06-20 执行记录

本轮已按用户确认进入 `GSD2-B5-SR-CONTROL-ACTIVITY-001 / schemaDecision=ddl-backed` 的首轮实现。

已落地内容：

1. 新增 `SpendControlActivityApplicationService`、`RecordSpendControlActivityRequest`、`SpendControlActivityQuery`、`BudgetControlProjectionQuery`、`SpendControlActivityDTO`、`BudgetControlProjectionDTO` 和 `SpendControlActivityType`。
2. 新增 `SpendControlActivity` Entity、`SpendControlActivityMapper`、`SpendControlActivityApplicationServiceImpl` 和 H2 `t_spend_control_activity`。
3. 新增 `SpendControlActivityApplicationServiceTests`，覆盖准入活动记录、拒绝活动记录、同流水同摘要幂等、同流水不同摘要拒绝、预算控制投影从活动派生，并断言无资金交易、route、posting、LedgerEntry、账本交易或余额投影副作用。

当前验证状态：

1. 本地 Maven 依赖缓存已恢复可解析状态，随后目标测试和仓库级验证均能进入新增 wallet 代码编译与执行阶段。
2. `just test-one SpendControlActivityApplicationServiceTests tests` 通过，6 tests 覆盖准入活动记录、拒绝活动记录、同流水同摘要幂等、同流水不同摘要拒绝、预算控制投影从活动派生，以及查询侧拒绝非资金账户 / 信用账户目标主体。
3. `just test-one SpendControlAdmissionApplicationServiceTests tests` 通过，3 tests 证明既有支出控制准入链路未被本切片破坏。
4. `just compile` 和 `just pmd` 通过；本状态迁移为 `SR_CONTROL_ACTIVITY_GREEN_VERIFIED`。

交接边界：

1. 本轮只声明 Spend Rule 控制活动与预算控制投影最小服务层能力 Green，可作为后续 VCC、钱包授权或预算控制业务入口的被依赖证据。
2. 完整 Spend Rule 规则定义、规则引擎、决策日志持久化、交易消费控制活动、VCC facade、Controller / HTTP / RPC、生产迁移、运营后台、灰度、监控和告警仍为 Not Done。
3. 若后续要求提交，只能提交本 Task 独立切片，不得混入 VCC、B7 报告扩展、交易投影、清结算或 Controller。
