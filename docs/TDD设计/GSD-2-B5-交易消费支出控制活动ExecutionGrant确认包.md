# GSD-2 B5 交易消费支出控制活动 Execution Grant 确认包

## 1. 文档定位

本文是 `GSD2-B5-SR-TRANSACTION-CONSUME-001` 的确认包，用于在当前 `ledger / wallet / transaction` 条件生产基线之上，评估是否打开“交易结果消费 Spend Rule 控制活动”的下一轮服务层切片。

本文不是编码授权、测试写入授权、DDL/H2 schema 授权、公共契约变更授权、Git 授权或生产发布授权。未确认本文第 12 节的 `Execution Grant` 前，只允许低风险文档、状态、任务和验证矩阵维护。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B5-SR-TRANSACTION-CONSUME-001` |
| 原子任务 | 在服务层补齐交易结果对 Spend Rule 控制活动的消费、释放、冲正和投影解释边界。 |
| 所属阶段 | GSD-2 / B5 Spend Rule / Transaction consumption admission / green verified committed。 |
| Goal ID | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| 当前状态 | `SR_TRANSACTION_CONSUME_GREEN_VERIFIED_COMMITTED` |
| 当前基线 | 本 Grant 已消费并随本提交固化；历史基线包含 `3b31d6e0 docs: 同步资金服务层提交状态`、`a5b12a3f feat: 收敛资金服务层交付基线`、`78f7f008 feat: 补齐支出控制活动与预算投影`、`021ee2ce feat: 补齐支出控制准入快照`。 |
| Owner | AI Native 流程编排负责确认包、状态和停止条件；产品架构专家负责业务目标、验收和 Not Done；资深架构师负责接口契约、事务边界、测试和验证命令；用户确认单一 Grant。 |
| 写入范围 | 本文、LWT Goal、W5 推进计划、GSD-2 新基线入口、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-B5-交易消费支出控制活动ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、core、wallet、transaction、ledger、tests、Justfile、AGENTS.md 和最近 Git 提交。 |
| Git 策略 | 当前确认包为 `summary_only`；除非用户再次明确授权提交，否则不执行 Git add / commit / push。 |
| 服务层边界 | 只处理 wallet / transaction 的服务层能力，不新增 Controller、HTTP/RPC、页面、导出、外部通道适配或控制层能力。 |

## 2. 业务目标

业务目标：把已经完成的支出控制准入、控制活动记录和预算控制投影，接到真实交易结果之后的控制活动消费链路中，让产品、运营、财务和风控能解释“规则为什么占用额度、交易是否真正消耗额度、失败或退款是否释放额度”。

用户价值：

1. 产品和运营能从同一条时间线看到准入、预留、交易成功、退款、撤销、过期和释放。
2. 财务和风控能区分资金交易事实和控制活动事实，避免把预算控制投影误读为资金余额。
3. 研发能在不改交易 canonical 入参的前提下，把支付工具和 Spend Rule 证据稳定固化到服务层消费链路。

成功指标：

1. 每个交易后控制动作都能回链原控制活动、原交易流水、业务场景和支付工具快照。
2. 交易成功和失败释放在语义上可区分，不把成功消耗伪装成普通释放。
3. 所有消费、释放、退款释放或冲正动作都具备幂等、失败无资金副作用、审计和查询证据。

非目标：

1. 不实现完整 Spend Rule 规则引擎、规则定义、规则计算、决策日志持久化或外部规则专业确认。
2. 不修改直接交易、授权交易、余额控制的 canonical 账户主体入参。
3. 不新增统一支付工具交易内核、VCC facade、全球账户、ACH、收单或清结算业务能力。
4. 不写 Controller、HTTP/RPC、外部消息消费者、页面、导出或生产迁移。
5. 不把预算组、支付工具、Spend Rule 或投影写成 ledger subject。

## 3. 能力地图

| 能力域 | 前台能力 | 后台能力 | 数据能力 |
| --- | --- | --- | --- |
| Spend Rule 准入 | 判断某次支付工具动作是否允许进入交易。 | 组合外部规则决策、支付工具预交易快照、账户能力和资金责任。 | `SpendControlAdmissionDecisionDTO`、预交易快照、规则决策摘要。 |
| 控制活动 | 记录准入、拒绝、预留、释放、过期和撤销。 | 幂等记录、查询和预算控制投影派生。 | `SpendControlActivity`、`BudgetControlProjectionDTO`。 |
| 交易消费控制活动 | 交易结果触发控制活动消耗、释放、退款释放或冲正。 | 服务层桥接交易结果和控制活动，不反写交易事实或账本事实。 | 原控制活动引用、交易引用、消费/释放活动、投影解释。 |
| 交易和账本 | 直接交易、授权交易、退款和余额控制继续生成资金事实。 | 交易内核仍以账户主体为 canonical 入参，ledger 只维护账本事实。 | Funds transaction、route snapshot、ledger transaction、LedgerEntry、余额投影。 |

## 4. 业务对象和生命周期

业务对象：

1. `SpendControlActivity`：支出控制活动事实，当前已承载准入、拒绝、预留、释放、过期和撤销。
2. `FundsTransaction`：资金交易事实，是交易生命周期和账务链路的主事实。
3. `RouteSnapshot`：交易路由快照，承载支付工具引用、绑定版本、资金责任和 route context。
4. `BudgetControlProjection`：由控制活动派生的只读控制投影，不替代资金账户或信用账户余额。
5. `PaymentInstrumentRef`：支付工具快照引用，只作为路由和审计维度，不作为账务主体。

字段口径：

- 控制活动目标主体只能是资金账户或信用账户。
- 交易结果引用建议包含原交易流水、原控制活动流水、业务流水、支付工具号、动作、规则标识、规则版本和决策摘要。
- 当前 `SpendControlActivityType` 没有交易成功后的“控制消耗”语义。建议下一 Grant 增补 `CONSUMED`，避免把成功消耗和失败释放都压到 `RELEASED`。

生命周期：

1. 准入阶段记录 `ADMISSION_RECORDED` 或 `REJECTED_RECORDED`。
2. 预留阶段记录 `RESERVED`，表达规则或预算控制占用。
3. 交易成功后记录 `CONSUMED`，表达控制占用被真实交易结果消耗。
4. 交易失败、撤销、过期或未成交通道终局后记录 `RELEASED`、`REVERSED` 或 `EXPIRED`，表达控制占用释放。
5. 退款或争议退款后记录补偿型控制活动，回链原交易和原控制活动。

## 5. 业务流程

主流程：

1. wallet application facade 先通过支付工具、账户能力、资金责任和外部 Spend Rule 决策生成准入结果。
2. 准入通过后，服务层记录 `RESERVED` 控制活动，形成控制活动流水和预算控制投影。
3. 交易仍通过账户主体型直接交易或授权交易服务完成，不替换交易内核入参。
4. 交易完成后，由交易消费控制活动服务接收交易结果、原控制活动引用和业务上下文。
5. 服务记录 `CONSUMED` 或释放类控制活动，并更新只读预算控制投影解释。

异常流程：

1. 缺少原控制活动、缺少原交易事实、租户不一致、目标主体不一致、币种不一致或金额超出已预留额度时 fail-fast。
2. fail-fast 不反写交易事实、route、posting、LedgerEntry、账本交易或余额投影。
3. 若交易已经成功但控制消费记录失败，本切片只记录服务层显式补偿入口和审计告警，不做自动消息补偿或 outbox。

人工兜底：

1. 控制消费失败、重复摘要冲突、原交易缺失或投影异常时进入运营审计和后续专项 Grant。
2. 生产迁移、历史补数、自动事件消费者、定时重试和运营后台不在本 Grant 首轮范围。

## 6. 规则矩阵

| 规则 | 触发条件 | 判断逻辑 | 优先级 | 版本 |
| --- | --- | --- | --- | --- |
| 控制消耗语义 | 交易成功或授权完成后关闭预留 | 必须记录独立的 `CONSUMED` 或等价成功消耗语义，不复用失败释放语义。 | P0 | GSD2-B5 |
| 控制释放语义 | 交易失败、撤销、过期或未成交终局 | 只能释放既有 `RESERVED` 控制占用，释放金额不得超过已预留未关闭金额。 | P0 | GSD2-B5 |
| 原事实引用 | 任一消费、释放、退款释放或冲正 | 必须引用原控制活动和原交易或业务流水，缺失则拒绝。 | P0 | GSD2-B5 |
| 入账主体红线 | 记录控制活动或计算投影 | 目标主体只能是资金账户或信用账户；预算组和支付工具不能成为 ledger subject。 | P0 | GSD2-LWT |
| 无资金副作用 | 控制活动消费失败 | 不得写资金交易、route、posting、LedgerEntry、账本交易或余额投影。 | P0 | GSD2-LWT |
| 单一 Grant | 任一代码或 schema 写入 | 必须由用户确认本 Grant 和 schemaDecision 后才能进入 Red / Green。 | P1 | GSD2 |

## 7. 架构设计

背景：当前 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 已完成控制活动记录、查询和预算控制投影最小服务层能力，但它只覆盖准入、拒绝、预留、释放、过期和撤销活动本身。交易成功后如何把预留变成真实消耗，仍没有服务层桥接能力。

现状和约束：

1. `SpendControlActivityApplicationService` 已能记录活动并派生预算控制投影。
2. `SpendControlActivityType` 只有 `ADMISSION_RECORDED`、`REJECTED_RECORDED`、`RESERVED`、`RELEASED`、`EXPIRED` 和 `REVERSED`。
3. `RecordSpendControlActivityRequest` 与 `t_spend_control_activity` 暂无原交易引用字段。
4. 交易内核继续以 `FundsAccountId` / 账户主体为 canonical 入参。

核心决策和取舍：

1. 推荐新增服务层能力 `SpendControlTransactionConsumptionApplicationService`，职责是把交易结果翻译为控制活动消费或释放。
2. 推荐新增 `CONSUMED` 控制活动类型，表达“交易成功消耗预留”，不复用 `RELEASED`。
3. 推荐采用 `activity-schema-backed`，在现有控制活动表上补最小交易引用和原活动引用，而不是新增独立投影表。
4. 不把该服务挂到 Controller、HTTP/RPC 或外部消息消费者；首轮以服务层显式调用闭环。

### 7.1 wallet application CR 准入结论

本轮对 `com.wind.funds.wallet.application` 做只读 CR 后，确认当前 application 包方向基本正确：资金责任解析、账户能力来源、支付工具能力准入、预交易快照、授权准入、支出控制准入和控制活动服务流均已形成可消费证据。但该包只能判定为“条件基础能力可用”，不能声明 wallet 全量生产 Done。

进入本 Grant 前需要吸收以下 CR 结论：

1. `SpendControlActivityApplicationService` 当前只覆盖控制活动记录和预算控制投影派生，尚未覆盖交易成功后的控制消耗；本 Grant 的 P0 目标就是补 `CONSUMED` 或等价消耗语义，以及 `consumedAmount` 或等价投影口径。
2. `AuthorizationAdmissionApplicationService` 当前应被理解为“支付工具授权内核准入 baseline”，不是完整的“支付工具 + Spend Rule + 控制活动 + 授权交易”业务编排入口；后续完整入口必须先消费支出控制准入 / 控制活动，再委派账户主体型交易服务。
3. 支付工具 `REFUND` 动作方向在当前实现中存在口径差异：支付工具能力检查按付款方向推断，预交易快照按收款能力判断。本 Grant 首轮不重构支付工具 `REFUND` 能力规则，只处理“已有资金退款交易事实之后的控制补偿 / 释放”。
4. 若本切片需要新增或复用账户能力判断，应优先消费 `FundsAccountCapabilityApplicationService` 的应用层决策，避免直接散落 `FundsAccount#canPay`、`canReceive` 或 `canWithdraw` 判断导致策略漂移。
5. 控制活动幂等不能只覆盖串行 `select then insert`；若沿用 `tenantId + activitySn` 唯一约束，服务实现需在并发重复写入冲突时读取既有活动并比较摘要，同摘要回放、异摘要拒绝。

### 7.2 源码预检校准

本轮只读源码预检确认：`GSD2-B5-SR-CONTROL-ACTIVITY-001` 已完成首轮实现，旧确认包中“控制活动服务、DTO、H2 表不存在”的历史证据已经过期。下一轮 Red 不能再写成“缺 `SpendControlActivityApplicationService` 或缺 `t_spend_control_activity`”，必须收敛到交易结果消费控制活动的真实缺口。

| 预检项 | 当前证据 | 对下一 Red 的影响 |
| --- | --- | --- |
| 控制活动服务 | 已存在 `wallet-face` 的 `SpendControlActivityApplicationService` 和 `wallet-impl` 的 `SpendControlActivityApplicationServiceImpl`。 | Red 应证明缺交易消费服务或缺消费语义，而不是缺控制活动记录服务。 |
| 控制活动模型 | 已存在 `RecordSpendControlActivityRequest`、`SpendControlActivityDTO`、`BudgetControlProjectionDTO`、`SpendControlActivity` Entity 和 `SpendControlActivityMapper`。 | Green 应复用控制活动模型并做最小扩展，不另起平行模型。 |
| H2 表结构 | `tests/src/test/resources/jdbc-schema.sql` 已存在 `t_spend_control_activity`，包含 `tenant_id + activity_sn` 唯一键、业务号、支付工具、目标主体、金额、币种、规则和摘要字段。 | `activity-schema-backed` 首轮只应在该表上补交易消费必要字段，不新增独立投影表。 |
| 活动类型 | `SpendControlActivityType` 当前只有 `ADMISSION_RECORDED`、`REJECTED_RECORDED`、`RESERVED`、`RELEASED`、`EXPIRED`、`REVERSED`。 | 必须新增 `CONSUMED` 或等价成功消耗语义，避免交易成功被解释为释放。 |
| 投影字段 | `BudgetControlProjectionDTO` 当前只有 `reservedAmount`、`releasedAmount` 和 `remainingControlAmount`。 | 若首轮交付声明交易成功消耗可解释，需要补 `consumedAmount` 或等价口径，并重新定义 remaining 计算。 |
| 原事实回链 | `RecordSpendControlActivityRequest`、DTO、Entity 和 H2 表当前没有原控制活动流水、原交易流水或退款交易流水字段。 | 本 Grant 的 `activity-schema-backed` 最小字段应围绕 `originalActivitySn`、`transactionSn` 和必要业务引用展开。 |
| mapper 包路径 | 当前 mapper 包为 `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/dal/mapper/SpendControlActivityMapper.java`。 | 后续写入范围应使用 `dal/mapper`，不要误写为 `dal/mappers`。 |

源码预检后的首个 Red 建议：

1. 准备一笔 `RESERVED` 控制活动和一笔已完成资金交易事实。
2. 调用候选 `SpendControlTransactionConsumptionApplicationService#consume`。
3. 当前应因缺服务、缺 `CONSUMED` 活动类型、缺原活动 / 原交易字段或缺 `consumedAmount` 投影口径而 Red。
4. Green 后必须记录 `CONSUMED` 活动，回链原 `RESERVED` 活动和交易流水，并证明没有新增资金交易、route、posting、LedgerEntry、账本交易或余额投影副作用。

接口契约：

| 候选接口 | 候选方法 | 入参 | 出参 | 幂等 |
| --- | --- | --- | --- | --- |
| `SpendControlTransactionConsumptionApplicationService` | `consume` | 交易成功或授权完成后的控制消费请求。 | 控制活动 DTO 或消费结果 DTO。 | `tenantId + activitySn` |
| `SpendControlTransactionConsumptionApplicationService` | `release` | 交易失败、撤销、过期或未成交终局后的控制释放请求。 | 控制活动 DTO 或释放结果 DTO。 | `tenantId + activitySn` |
| `SpendControlTransactionConsumptionApplicationService` | `refund` | 退款或争议退款后的控制补偿请求。 | 控制活动 DTO 或退款控制结果 DTO。 | `tenantId + activitySn` |

候选入参最小字段：

1. `tenantId`
2. `activitySn`
3. `originalActivitySn`
4. `transactionSn`
5. `businessScene`
6. `businessSn`
7. `instrumentSn`
8. `targetAccountId`
9. `amount`
10. `currency`
11. `activityDigest`
12. `description`

数据方案：

- `contract-only`：只补接口、Red 和文档，不改 Entity / Mapper / H2 schema。适合进一步验证模型，但不能形成生产可用消费事实。
- `activity-schema-backed`：在 `t_spend_control_activity` 上补原活动引用、交易引用和消费语义字段。推荐作为首轮可交付方案。
- `new-table-backed`：新增独立交易控制消费表。当前不推荐，除非后续证明控制消费生命周期明显独立于控制活动事实。

事务边界和一致性：

- 控制活动消费服务不创建资金交易，不写 route，不写 posting，不写 LedgerEntry。
- 若它由同一 application use case 在交易成功后同步调用，调用方必须决定是否与交易在同一事务内；本确认包不默认修改交易内核事务。
- 若需要自动消费或最终一致事件消费，应另起 outbox / event consumer Grant。

可靠性、安全、权限、审计和告警：

- 可靠性：重复请求同摘要返回既有活动，不同摘要拒绝。
- 安全：上下文变量继续走敏感字段阻断，不泄露外部账户、卡号、证件或规则内部详情。
- 权限：本切片只提供服务层能力，不定义运营权限模型。
- 审计：消费活动必须回链原控制活动、原交易流水和业务场景。
- 告警：交易成功但控制消费失败时，后续生产化 Grant 需要补告警和人工处理入口。

发布、灰度、回滚和风险：

- 本确认包不发布生产能力。
- 后续代码 Grant 若采用 schema 写入，需要单独说明生产 DDL、灰度、回滚和数据校验。
- 主要风险是把 `RELEASED` 误用为“交易成功消耗”，导致预算投影无法区分真实消耗和失败释放。

## 8. 首批 Red 和验收

| Red / AC | 场景 | 预期 |
| --- | --- | --- |
| `RED-GSD2-B5-SR-TXN-CONSUME-001` | 已有 `RESERVED` 活动和成功交易，调用控制消费服务。 | 当前缺服务或缺 `CONSUMED` 语义，测试应失败；Green 后记录消费活动且不新增资金事实。 |
| `RED-GSD2-B5-SR-TXN-CONSUME-002` | 交易失败或撤销后释放控制占用。 | 只能释放既有预留，释放金额不得超过未关闭预留。 |
| `RED-GSD2-B5-SR-TXN-CONSUME-003` | 退款或争议退款回链原交易和原控制活动。 | 记录补偿型控制活动，可解释原消费和退款关系。 |
| `RED-GSD2-B5-SR-TXN-CONSUME-004` | 原控制活动缺失、币种不一致或目标账户不一致。 | fail-fast，且无交易、route、posting、LedgerEntry、账本交易或余额投影副作用。 |
| `RED-GSD2-B5-SR-TXN-CONSUME-005` | 同一 `tenantId + activitySn` 重放或并发重复写入。 | 同摘要幂等返回既有活动；不同摘要或事实漂移拒绝；并发唯一键冲突不得生成重复活动。 |
| `RED-GSD2-B5-SR-TXN-CONSUME-006` | 使用退款事实做控制补偿。 | 只解释已有退款交易事实和原消费控制活动的关系，不修改支付工具 `REFUND` 能力方向，不创建新的资金退款事实。 |

验收场景：

1. `RESERVED -> CONSUMED` 可解释真实交易成功消耗。
2. `RESERVED -> RELEASED / REVERSED / EXPIRED` 可解释失败、撤销、过期和未成交释放。
3. 退款或争议退款能回链原交易、原消费活动和业务场景。
4. 查询投影能区分 `reservedAmount`、`consumedAmount`、`releasedAmount` 和 `remainingControlAmount`。
5. 全路径证明控制活动消费不替代交易内核、账本事实或余额投影。
6. 授权准入入口和完整 Spend Rule 编排边界清晰：本切片不把 `AuthorizationAdmissionApplicationService` 扩成全量支付工具交易 facade。

### 8.1 TDD、Review 和 AI 产物复核

TDD：

1. 后续编码必须先补 `SpendControlTransactionConsumptionApplicationServiceTests` 的首个 Red，证明当前缺少服务层消费语义或缺少 `CONSUMED` 控制活动类型。
2. 每个 Green 都必须同时断言控制活动、预算控制投影、幂等、失败无资金副作用和原交易回链，不能只断言服务调用成功。

Review：

1. 资深架构师 Review 优先检查交易内核 canonical 入参是否未被替换、wallet application 是否只做服务层桥接、ledger 是否未被控制活动反向污染。
2. 产品架构专家 Review 优先检查业务解释是否能回答“谁占用了额度、哪笔交易消耗了额度、退款后如何释放或补偿”。

编码红线：

1. 本切片不得新增 Controller、HTTP/RPC、外部消息消费者、统一支付工具交易内核、VCC facade 或生产迁移脚本。
2. 控制活动消费失败不得写资金交易、route、posting、LedgerEntry、账本交易或余额投影。
3. 不在本切片内调整支付工具 `REFUND` 能力方向；若需要处理方向规则，先另行确认 receive/payment/bidirectional/split 裁决。

AI 产物复核：

1. 代码生成后必须按差异清单逐文件复核，不接受只因测试通过就自动进入提交。
2. 若出现接口字段膨胀、规则引擎化、跨层直接写账或默认方法兜底，应先 Refactor 收敛后再继续验证。

### 8.2 三卡交接和编码准入判定

本确认包面向下一轮服务层编码切片，只证明“可以进入 Grant 确认”，不证明“已经授权编码”。三卡消费状态如下：

| 交接卡 | 当前内容 | 可消费性 | 不足和处理 |
| --- | --- | --- | --- |
| Product Context Card | 业务目标是解释 Spend Rule 控制占用如何在交易成功、失败、撤销、过期、退款和争议退款后被消耗、释放或补偿；核心对象是控制活动、交易事实、route snapshot、预算控制投影和支付工具快照。 | 可消费。 | 外部规则决策、运营审批和完整规则引擎不在本切片内，后续另起 Grant。 |
| Engineering Handoff Card | 已明确 Task ID、Goal、写入范围、只读范围、候选接口、schemaDecision、首批 Red、验证命令、停止条件和禁止事项。 | 可消费但等待授权。 | 必须由用户确认 `Execution Grant` 和 `schemaDecision` 后，才可写 Java、测试、DDL/H2 schema、Entity、Mapper 或公共契约。 |
| Production Loop Card | 已明确状态载体、Maker / Checker、反馈源、验证命令、无资金副作用、敏感字段约束、Git 策略和停止条件。 | 只可作为本地服务层 Loop 候选。 | 不包含生产迁移、自动事件消费者、运营后台、监控告警、灰度回滚或真实通道接入，不能声明生产发布就绪。 |

编码准入判定：

| 判定项 | 结论 |
| --- | --- |
| 是否进入角色协作 Loop | 是，场景视图为产研交付，当前阶段为 TDD / 编码准入前确认。 |
| 是否形成 CAD 候选 | 是，候选 Task 为 `GSD2-B5-SR-TRANSACTION-CONSUME-001`。 |
| 是否已经形成 CAD Grant | 否，仍需用户确认推荐 Grant 和 `schemaDecision=activity-schema-backed`。 |
| 是否允许本轮继续改代码 | 否，本轮仅允许低风险文档、状态和验证矩阵维护。 |
| 下一步 owner | 用户确认 Grant；确认后由资深架构师按 TDD 先写首个 Red，再进入服务层 Green。若用户希望同时修正支付工具 `REFUND` 能力方向，需要先另起方向裁决或把本 Grant 升级为新的单一 Grant。 |

## 9. 候选写入范围

若用户确认 `Execution Grant：GSD2-B5-SR-TRANSACTION-CONSUME-001` 和 `schemaDecision=activity-schema-backed`，建议允许以下最小写入：

| 层 | 候选文件 |
| --- | --- |
| core | `core/src/main/java/com/wind/funds/wallet/enums/SpendControlActivityType.java` |
| face service | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/spend/SpendControlTransactionConsumptionApplicationService.java` |
| face model | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/model/request/*SpendControl*Transaction*Request.java`、`wallet/wallet-face/src/main/java/com/wind/funds/wallet/model/dto/*SpendControl*Transaction*DTO.java` |
| impl | `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application/spend/impl/*SpendControl*Transaction*ApplicationServiceImpl.java` |
| dal | `wallet/wallet-impl/src/main/java/com/wind/funds/wallet/dal/entities/SpendControlActivity.java`、`wallet/wallet-impl/src/main/java/com/wind/funds/wallet/dal/mapper/SpendControlActivityMapper.java` |
| test schema | `tests/src/test/resources/jdbc-schema.sql` |
| tests | `tests/src/test/java/com/wind/funds/wallet/application/spend/SpendControlTransactionConsumptionApplicationServiceTests.java`、必要的相邻支出控制活动回归。 |
| docs | 本文、LWT Goal、W5 推进计划、TDD 设计、OpenSpec tasks。 |

禁止范围：

1. 不写 Controller、HTTP/RPC、页面、导出或外部消息消费者。
2. 不改直接交易、授权交易、余额控制的 canonical 请求入参。
3. 不新增统一支付工具交易内核或 VCC facade。
4. 不扩完整 Spend Rule 规则引擎、外部规则计算、运营审批或生产迁移。
5. 不新增 ledger subject、posting 规则、LedgerEntry 或余额投影写入逻辑。
6. 不修改支付工具 `REFUND` 能力方向；本轮退款只作为控制活动补偿语义处理。

## 10. 验证方案

文档确认包验证：

1. `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-B5-交易消费支出控制活动ExecutionGrant确认包.md`
2. `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-B5-交易消费支出控制活动ExecutionGrant确认包.md`
3. `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-B5-交易消费支出控制活动ExecutionGrant确认包.md`
4. `rg "GSD2-B5-SR-TRANSACTION-CONSUME-001|交易消费支出控制活动|CONSUMED|wallet application CR|REFUND" docs openspec`
5. `git diff --check`

后续编码验证建议：

1. `just test-one SpendControlTransactionConsumptionApplicationServiceTests tests`
2. `just test-one SpendControlActivityApplicationServiceTests tests`
3. `just test-one SpendControlAdmissionApplicationServiceTests tests`
4. `just compile`
5. `just pmd`
6. `git diff --check`

若接入直接交易或授权交易真实 flow，再追加对应交易分组或目标 flow 测试。

## 11. 停止条件

出现以下任一情况立即停止：

1. 用户未确认 `Execution Grant` 或未确认 `schemaDecision`。
2. 需要改 Java、测试、公共契约、DDL/H2 schema、Entity、Mapper 或枚举，但 Grant 未列名授权。
3. 需要 Controller、HTTP/RPC、消息消费者、生产迁移、Git push、联网、依赖安装或外部规则专业确认。
4. 发现设计需要把预算组、支付工具、Spend Rule 或投影写成 ledger subject。
5. 发现必须修改交易内核 canonical 入参或 ledger posting 才能完成首个 Red。
6. 发现必须修改支付工具 `REFUND` 能力方向才能完成首轮控制补偿。
7. 验证失败且无法在本 Grant 写入范围内修复。

## 12. 可复制确认文本

推荐可交付版本：

```text
Execution Grant：GSD2-B5-SR-TRANSACTION-CONSUME-001
schemaDecision：activity-schema-backed
refundActionDecision：control-compensation-only
范围：在服务层补齐交易结果消费 Spend Rule 控制活动的最小能力，允许写 wallet-face application/spend 契约、Request/DTO、必要 SpendControlActivityType 扩展、wallet-impl application/spend 实现、必要 H2 schema/Entity/Mapper 字段扩展、目标服务流测试和状态文档回写。
禁止：不写 Controller、HTTP/RPC，不改交易 canonical 入参，不新增统一支付工具交易内核，不做完整规则引擎、VCC facade、清结算、补事实、生产迁移或 Git push；不修改支付工具 REFUND 能力方向，退款只作为已有退款资金事实后的控制补偿。
```

保守版本：

```text
Execution Grant：GSD2-B5-SR-TRANSACTION-CONSUME-001
schemaDecision：contract-only
范围：只补服务层契约、首个 Red 和文档回写，用于验证交易消费控制活动模型是否成立。
禁止：不写 DDL/H2 schema、Entity、Mapper、实现、Controller、HTTP/RPC、生产迁移或 Git push。
```

## 13. 交接和残余风险

交接要求：

1. 用户确认 Grant 后，先写 `SpendControlTransactionConsumptionApplicationServiceTests` 首个 Red。
2. Red 必须证明当前缺交易消费控制活动服务、缺成功消耗语义或缺原交易/原活动引用。
3. Green 必须先做到最小服务层闭环，再补相邻回归和文档状态回写。
4. 完成后回写 LWT Goal、W5 推进计划、TDD README、docs README 和 OpenSpec tasks。
5. 若实现过程中发现 `AuthorizationAdmissionApplicationService` 必须承担完整 Spend Rule 编排，或发现支付工具 `REFUND` 方向必须同步修正，立即停止并重新拆 Grant。

残余风险：

1. `CONSUMED` 命名、投影字段和兼容影响需要在代码 Grant 中最终确认。
2. 交易成功与控制消费是否同事务，需要按具体 application use case 裁决。
3. 自动事件消费、outbox、运营重试、生产迁移和历史补数均不在首轮范围。

### 13.1 Grant 消费运行卡

本节只定义用户确认推荐 Grant 后的首轮执行顺序，不代表当前已经授权编码。

| 步骤 | 动作 | 通过口径 | 停止条件 |
| --- | --- | --- | --- |
| 1. Pick | 读取本文、LWT Goal、W5 推进计划、OpenSpec tasks 和当前源码，确认工作树没有 Java、SQL、POM 或 `Justfile` 未解释差异。 | 只选择 `GSD2-B5-SR-TRANSACTION-CONSUME-001` 一个切片；`schemaDecision=activity-schema-backed`、`refundActionDecision=control-compensation-only` 已由用户确认。 | 发现用户改动冲突、Grant 缺字段或工作树混入无关代码变更时停止。 |
| 2. Red | 新增 `SpendControlTransactionConsumptionApplicationServiceTests`，先写成功交易消费 `RESERVED` 控制活动的失败用例。 | Red 应因缺服务、缺 `CONSUMED` 类型、缺原活动 / 原交易字段或缺 `consumedAmount` 投影口径失败。 | Red 需要修改交易 canonical 入参、ledger posting 或支付工具 `REFUND` 方向才能成立时停止。 |
| 3. Contract | 补 `SpendControlTransactionConsumptionApplicationService`、消费 / 释放 / 退款请求和结果 DTO、必要 `SpendControlActivityType` 扩展。 | 契约只表达交易结果消费控制活动；入参必须含 `tenantId`、`activitySn`、`originalActivitySn`、`transactionSn`、目标主体、金额币种和摘要。 | 字段膨胀到规则引擎、运营审批、外部事件消费或统一支付工具交易 facade 时停止。 |
| 4. Green | 在现有 `t_spend_control_activity`、Entity、Mapper 和活动服务基础上完成最小实现。 | 能记录 `CONSUMED`，回链原 `RESERVED` 活动和交易流水，并派生 `consumedAmount` 或等价投影；失败路径不写资金交易、route、posting、LedgerEntry、账本交易或余额投影。 | 需要新增独立 projection store、交易事实写入或账本事实写入时停止。 |
| 5. Extend | 补交易失败 / 撤销 / 过期释放、退款或争议退款控制补偿、幂等重放和摘要冲突用例。 | 释放金额不得超过未关闭预留；退款补偿只解释已有退款资金事实和原消费活动关系；并发唯一键冲突需读取既有活动并比较摘要。 | 需要自动消息补偿、outbox、运营后台、生产迁移或历史补数时停止。 |
| 6. Review | 用资深架构师视角检查模块边界、事务边界、失败无副作用、幂等和字段注释。 | wallet 只做服务层桥接，transaction canonical 入参不变，ledger 不被控制活动反向污染，实体字段符合注释约规。 | 出现 default 方法兜底、内存版业务实现、绕过 application facade 或预算组 / 支付工具入账时停止。 |
| 7. Verify | 运行目标测试、相邻回归、compile、pmd 和 diff 检查。 | 至少通过 `just test-one SpendControlTransactionConsumptionApplicationServiceTests tests`、`just test-one SpendControlActivityApplicationServiceTests tests`、`just test-one SpendControlAdmissionApplicationServiceTests tests`、`just compile`、`just pmd`、`git diff --check`。 | 验证失败且无法在本 Grant 写入范围内修复时停止并回写失败证据。 |
| 8. Handoff | 回写本文、LWT Goal、W5 推进计划、TDD README、docs README 和 OpenSpec tasks。 | 记录 Red / Green / Verify 证据、Not Done、残余风险和建议提交切片；不声明完整 Spend Rule、VCC facade、事件消费或生产发布 Done。 | 发现需要扩新能力时另起单一 Grant，不在本轮追加。 |

## 14. Grant 消费执行记录（2026-06-20）

本轮用户已确认按服务层边界推进 `GSD2-B5-SR-TRANSACTION-CONSUME-001`，并明确“不碰 Controller、HTTP/RPC、交易 canonical 入参、支付工具 REFUND 方向和 ledger posting”。本节记录本 Grant 的实际消费结果，保留第 13.1 节作为历史运行卡，不回写为新的授权来源。

| 项 | 结果 |
| --- | --- |
| Execution Grant | `GSD2-B5-SR-TRANSACTION-CONSUME-001`。 |
| schemaDecision | `activity-schema-backed`。 |
| refundActionDecision | `control-compensation-only`。 |
| 新增服务层能力 | 新增 `SpendControlTransactionConsumptionApplicationService`，提供 `consume`、`release`、`refund` 三个服务层方法，用于把既有资金交易结果转换为支出控制活动消费、释放或退款补偿事实。 |
| 活动语义 | 新增 `CONSUMED` 表达成功交易消耗预留控制；新增 `REFUND_COMPENSATED` 表达已有退款资金事实后的控制补偿，不改变支付工具退款方向。 |
| 回链字段 | 在控制活动请求、DTO、Entity 和 H2 测试表中补 `originalActivitySn` 与 `transactionSn`，用于回链原预留活动和资金交易事实。 |
| 投影解释 | `BudgetControlProjectionDTO` 补 `consumedAmount`；预算控制投影按 `reserved - netConsumed - released` 解释剩余控制额度。 |
| 无副作用边界 | 目标服务只记录控制活动，不创建或修改资金交易、route snapshot、posting、LedgerEntry、账本交易、余额投影或交易 canonical 请求。 |
| Red 证据 | 首次运行 `SpendControlTransactionConsumptionApplicationServiceTests` 因缺服务、请求和实现类型失败，符合 Red 预期。 |
| Green 证据 | `SpendControlTransactionConsumptionApplicationServiceTests` 覆盖消费、失败释放、退款补偿、超额拒绝和摘要冲突幂等边界，5 tests 通过。 |
| 相邻回归 | `SpendControlActivityApplicationServiceTests` 6 tests 通过；`SpendControlAdmissionApplicationServiceTests` 3 tests 通过。 |
| 编译与规约 | `just compile`、`just pmd` 和 `git diff --check` 均通过；沙箱内 embedded Redis 端口限制已按项目经验在非沙箱权限下复跑确认。 |
| 当前状态 | `SR_TRANSACTION_CONSUME_GREEN_VERIFIED_COMMITTED`。 |

验证命令：

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one SpendControlTransactionConsumptionApplicationServiceTests tests
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one SpendControlActivityApplicationServiceTests tests
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one SpendControlAdmissionApplicationServiceTests tests
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just compile
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just pmd
git diff --check
```

Not Done：

1. 不包含 Controller、HTTP/RPC、外部消息消费者、outbox、运营后台、生产迁移或历史补数。
2. 不包含完整 Spend Rule 规则引擎、规则定义、规则计算、决策日志持久化或外部规则专业确认。
3. 不包含统一支付工具交易内核、VCC facade、清结算、补事实、生产发布或支付工具 `REFUND` 方向重裁决。
4. 不包含交易内核 canonical 入参改造、route resolver、route replay、posting assembler 或 ledger posting 改造。
5. 并发唯一键冲突后的数据库异常捕获回读、自动告警和补偿重试仍需后续专项 Grant 评估。
