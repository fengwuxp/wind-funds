# 支付资金底座 API 契约测试与编码实施计划

# 一、定位

本文把前序 PRD、DSL、OpenSpec 和系分设计收敛为进入编码前的实施计划，重点回答：

1. face 层应该暴露哪些稳定能力。
2. DTO、Request、Query、错误码、幂等、审计和事件契约如何落地。
3. 每类能力需要哪些 JSON 契约样例和测试保护。
4. 本地验证命令如何映射到 Harness 门禁。
5. 后续代码落地应该按什么顺序推进。

本文不创建真实 Harness pipeline，不修改 Java 代码，不设计 Web API 入口。Web API 如后续需要，应在本文服务契约基础上做协议适配。

# 二、输入与边界

| 输入 | 用途 |
| --- | --- |
| `../v5 支付底座完整产品 PRD.md` | 产品目标、核心概念、用例、余额桶、清结算、归档和红线。 |
| `../v5 DSL 规范设计.md` | FundsInstruction、Route、Posting、LedgerEntry、Replay、归档治理 DSL。 |
| `../v5 DSL 契约复审矩阵.md` | 契约测试、JSON 样例和必须失败红线。 |
| `Ledger DSL Posting 系分设计.md` | 账本、路由到过账、摘要、余额投影和归档水位。 |
| `Wallets 账户与余额控制系分设计.md` | 账户主体、余额桶、平台账户角色、冻结、授权占用和受控负余额。 |
| `交易层服务能力系分设计.md` | 直接交易、逆向交易、授权交易、余额控制和查询重放。 |
| `清结算与对账系分设计.md` | 清算候选、结算单、出款单、对账差错和阻断规则。 |
| `归档投影与指标治理系分设计.md` | 检查点、水位、归档清单、交易视图重放和指标治理。 |
| `v4 代码 CR 遗留待办.md` | v4 代码评审中已确认未闭合的实现、命名、换汇、字面量和测试问题。 |
| `P0 编码任务拆分.md` | P0 代码工作包、模块范围、测试资产、验收标准和验证命令。 |

实施边界：

1. face 层只定义稳定契约，不暴露 Entity、Mapper、内部 route/assembler 实现。
2. transaction 层对业务侧提供统一交易能力，通过 ledger 和 wallets 能力实现。
3. wallets 层保留账户、余额查询、账本配置、平台账户角色和支付工具领域能力；冻结订单生命周期、余额控制命令和对外交易门面收敛到 transaction 层。
4. 清结算、对账、归档治理可以先完成模型和契约，代码落地顺序排在核心交易和账本稳定之后。
5. 不做历史兼容设计；但真实代码落地要小步提交、可编译、可测试。

# 三、目标模块归属

| 能力域 | 目标 face 边界 | 目标 impl 边界 | 不允许事项 |
| --- | --- | --- | --- |
| Ledger / Posting | `ledger-face` 暴露账本交易、分录、余额查询和重建契约；`wind-funds` 承载 DSL Spec。 | `ledger-impl` 实现 route 到 posting、分录持久化、余额投影、checkpoint 和 watermark。 | 业务侧直接传 `LedgerEntry` 或直接改余额。 |
| Wallets / Account | `wallet-face` 暴露资金账户、信用账户、预算组、账本配置、平台账户角色、支付工具和余额查询契约。 | `wallet-impl` 实现账户初始化、余额桶策略、账本 profile、平台角色解析和受控负余额策略。 | 把付款、退款、提现、转账等交易命令放回 wallet，或把信用/预算账户当真实资金账户。 |
| Transaction Layer | `transaction-face` 暴露直接交易、逆向交易、授权交易、余额控制、交易视图和重放契约。 | `transaction-impl` 实现幂等、生命周期、source fact、route snapshot、ledger 调用和事件。 | Web/API 直接调用 ledger 组装分录；授权拒绝写入争议拒付。 |
| Clearing / Reconciliation | `transaction-face` 或清结算子契约暴露清算批次、结算单、出款单、对账批次和差错处理能力。 | 实现候选生成、批次版本、结算锁定、出款结果、对账匹配、差错阻断和调账。 | 把清结算流程状态塞进 `FundsTransactionStatus` 或 DSL phase。 |
| Projection / Metrics | face 层暴露重建、重放、指标查询和治理任务契约。 | 实现水位推进、归档清单、视图重放、账户类型分表查询和指标快照。 | 用交易视图反推余额，或用归档保留周期当冷热计算边界。 |

# 四、face 层服务契约设计

## 4.1 交易层服务能力

交易层建议在 `transaction/transaction-face` 下按能力拆分服务。下表使用的是服务能力标签，不是最终 Java 接口名；真实接口命名应在编码前结合模块边界、兼容影响和现有调用方再确认。

| 能力 | 服务能力标签 | 请求 | 返回 | 核心约束 |
| --- | --- | --- | --- | --- |
| 直接交易 | 直接交易服务能力 | `DirectTransactionRequest` | `FundsTransactionDTO` | 已确认价值转移、责任变化或资金状态最终变化；必须生成 route snapshot、ledger transaction 和 entries。 |
| 逆向交易 | 逆向交易服务能力 | `ReverseTransactionRequest` | `FundsTransactionDTO` | 必须引用原交易或原快照；累计退款、退回、争议扣减不得超过产品上限。 |
| 授权交易 | 授权交易服务能力 | `AuthorizationTransactionRequest` | `FundsAuthorizationTransactionDTO` | 授权批准、拒绝、撤销、结算、授权退款、争议拒付口径分离。 |
| 余额控制 | 余额控制服务能力 | `BalanceControlRequest` | `BalanceControlDTO` | 冻结/解冻不创建 `FundsTransaction`；冻结单是来源事实，账本交易是余额变化事实。 |
| 交易视图查询 | 交易视图查询能力 | `TransactionViewQuery` | `TransactionViewDTO` / page | 只读投影；支持按账户主体类型分表后的聚合查询。 |
| 重放重建 | 重放重建能力 | `ReplayRequest` | `ReplayResultDTO` | 余额重建使用 checkpoint + watermark；交易视图重放必须限定范围。 |

## 4.2 请求公共字段

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `tenantId` | 是 | 租户边界，所有写操作必须参与幂等和权限判断。 |
| `businessScene` | 是 | 业务场景，例如平台内部付款、VCC 授权、出入金、清算确认。 |
| `businessSn` | 是 | 业务侧稳定流水，不使用数据库 ID。 |
| `eventType` | 是 | 资金事件类型，参考 PRD 事件主轴和 DSL 事件类型。 |
| `reference` | 条件必填 | 后续事件引用原资金事实，例如原交易、原授权、原冻结单、原账本交易或外部交易。当前执行链路优先使用该字段表达“本次动作依附哪个既有事实”。 |
| `sourceFactRef` | 延后设计 | 来源事实引用值对象，不作为当前 P0 必填公共字段。只有冻结单、清结算单、争议单、对账差错单等独立资金事实先于交易指令成立，并且这些事实已有稳定类型、流水、版本、明细和审计模型时，才引入独立 `sourceFactRef`；不恢复 `sourceObjectType/sourceObjectSn` 两个散字段。 |
| `idempotencyKey` | 是 | 调用方传入或系统按稳定字段生成。 |
| `requestHash` | 系统生成 | 摘要不包含数据库 ID、持久化流水、审计时间、展示文案等易变字段。 |
| `amount` / `currency` | 条件必填 | 金额类请求必填，金额精度和舍入规则由币种配置决定。 |
| `occurredAt` | 是 | 业务事实发生时间；入账时间由账本侧记录。 |
| `operator` | 条件必填 | 后台、高危、人工操作必填，普通业务系统可传系统操作者。 |
| `reasonCode` | 条件必填 | 拒绝、争议、冻结、调账、放行等场景必填稳定 code。 |
| `description` | 否 | 展示说明，不参与核心幂等摘要。 |

## 4.3 请求模型要点

| 请求 | 字段要点 | 必须失败场景 |
| --- | --- | --- |
| `DirectTransactionRequest` | 付款方/收款方引用、金额、费用、平台账户角色、route policy、来源事实、幂等键。 | 外部账户被当作 ledger subject；费用混入本金；route 不平衡。 |
| `ReverseTransactionRequest` | 原交易流水、原 route snapshot、逆向类型、逆向金额、原因、是否退费用。 | 缺原快照重新选路；累计金额超上限；原交易状态不允许逆向。 |
| `AuthorizationTransactionRequest` | 授权动作、授权链流水、资金账户/信用账户/预算组组合、拒绝原因、结算金额、争议原因。 | 授权拒绝生成 entry；结算超过剩余授权；争议拒付与授权拒绝混用。 |
| `BalanceControlRequest` | 控制动作、主体、账目、冻结单、解冻引用、额度/预算调整、审批引用。 | 冻结创建 `FundsTransaction`；解冻超过剩余冻结；负余额无策略。 |
| `SubjectBalanceQuery` | 主体、币种、账本 profile、查询时间、缺账本策略。 | 写流程用返回空余额绕过账本初始化；跨租户查询。 |
| `TransactionViewQuery` | 视图主体类型、主体流水、交易时间范围、账户类型分表路由、分页游标。 | 无时间范围全量扫表；用视图修正账本余额。 |
| `ReplayRequest` | replay 类型、范围、时间窗口、游标、dry run、原因、审批引用。 | 余额重建使用交易视图；交易视图重放生成 route 或 entry。 |

`SubjectBalanceQuery` 不建议继续使用 `failIfMissingLedger` 这种布尔字段。目标态应使用 `missingLedgerPolicy` 表达语义：`FAIL` 用于写流程和强一致查询，`RETURN_EMPTY` 用于展示型查询，`INIT_REQUIRED` 用于账户初始化流程。

v5 编码时还必须处理 v4 CR 遗留问题：避免为简单 builder 包装继续设计长参数 helper；涉及资金路径、route leg、snapshot schema、context key 的稳定字符串应使用常量或枚举；交易门面归属应从 wallet 历史命名收敛到 transaction 层能力。

## 4.4 业务身份、引用和 `sourceFactRef` 边界

本节用于防止 `businessScene/businessSn`、`reference` 和未来 `sourceFactRef` 混用。

| 概念 | 回答的问题 | 生命周期归属 | 示例 |
| --- | --- | --- | --- |
| `businessScene` | 本次资金请求属于哪类上游业务动作。 | 调用方业务动作或资金服务能力。 | `MERCHANT_ORDER_PAY`、`RISK_FREEZE`、`RECON_ADJUST`。 |
| `businessSn` | 本次业务动作的稳定流水是什么。 | 调用方业务动作或本次资金服务调用。 | `PAY_202605150001`、`FREEZE_202605150001`。 |
| `reference` | 本次动作引用哪个既有事实或外部凭证。 | 当前交易链路可立即使用。 | 原交易 `FT_xxx`、原授权 `FT_xxx`、冻结单 `FO_xxx`、外部交易号。 |
| `sourceFactRef` | 本次资金动作由哪个已经独立建模的资金域事实触发。 | 独立事实成熟后再启用。 | 争议单 `DSP_xxx`、清算单 `CLR_xxx`、对账差错单 `REX_xxx`。 |

设计原则：

1. `businessScene/businessSn` 可以覆盖“冻结、清结算、争议、对账差错等场景发起了一次资金动作”，但不应覆盖“该动作来源于哪个已存在的资金域事实”。
2. `businessSn` 不得伪装成冻结单、清算单、争议单或对账差错单流水；如果这些事实尚未先建模，则本次请求只表达业务身份和必要 `reference`。
3. `sourceFactRef` 的价值是把“事实类型、事实流水、事实版本、明细流水、外部凭证、审计上下文”封成一个可演进值对象，避免长期依赖散落字符串字段。
4. P0 阶段不新增泛化 `sourceFactRef` 代码；先把冻结单通过 `reference=FREEZE_ORDER` 闭环，清结算、争议、对账差错进入 P1 独立事实设计后再引入。
5. 未来若引入 `sourceFactRef`，请求摘要必须纳入其稳定字段，但不得纳入数据库自增 ID、创建时间、展示文案和临时审批状态。

# 五、错误码、幂等与审计

## 5.1 错误码分类

错误码用于稳定判断和国际化。入库和事件中保留 code、可选默认 message 和必要上下文；最终展示文案由运行时国际化决定。

| 错误码族 | 示例 code | 场景 |
| --- | --- | --- |
| 参数错误 | `FUNDS_PARAMETER_INVALID` | 金额为空、金额非正、币种为空、主体为空。 |
| 幂等冲突 | `FUNDS_IDEMPOTENCY_CONFLICT` | 同幂等键重复请求但摘要不同。 |
| 状态冲突 | `FUNDS_STATE_CONFLICT` | 原交易已关闭、授权已结清、冻结单已释放。 |
| 余额约束 | `FUNDS_BALANCE_INSUFFICIENT` | 可用余额、授权余额、冻结余额不足。 |
| 负余额策略 | `FUNDS_NEGATIVE_AVAILABLE_POLICY_DENIED` | 无审批或无策略导致受控负余额失败。 |
| 路由失败 | `FUNDS_ROUTE_RESOLUTION_FAILED` | 缺主体、缺平台账户角色、外部账户误入账。 |
| 账本配置 | `FUNDS_LEDGER_PROFILE_MISSING` | 缺账本 profile、账目 normal balance 或 subject ledger。 |
| 账务失败 | `FUNDS_POSTING_UNBALANCED` | posting plan 不平衡、币种不一致、账目不允许。 |
| 授权拒绝 | `FUNDS_AUTHORIZATION_DECLINED` | 授权阶段业务拒绝；无 route、无 entry。 |
| 争议上限 | `FUNDS_CHARGEBACK_LIMIT_EXCEEDED` | 争议扣减和退款累计超过已结算金额。 |
| 冻结约束 | `FUNDS_FREEZE_AMOUNT_EXCEEDED` | 冻结或解冻超过可用/剩余冻结。 |
| 归档水位 | `FUNDS_ARCHIVE_WATERMARK_GAP` | 归档 cutoff 晚于水位或检查点缺失。 |
| 重放范围 | `FUNDS_REPLAY_RANGE_REQUIRED` | 交易视图重放缺时间窗口或主体范围。 |
| 权限审计 | `FUNDS_PERMISSION_DENIED` | 越权、缺审批、缺高危操作权限。 |

## 5.2 幂等键规则

| 事实类型 | 幂等键建议 |
| --- | --- |
| 直接交易 | `tenantId + businessScene + businessSn + eventType + idempotencyKey` |
| 授权批准/拒绝 | `tenantId + authorizationRequestSn + action + idempotencyKey` |
| 授权结算/撤销/退款/争议 | `tenantId + originalAuthorizationSn + action + businessSn + idempotencyKey` |
| 冻结/解冻 | `tenantId + frozenOrderSn + action + idempotencyKey` |
| 清算确认 | `tenantId + clearingBatchSn + version + idempotencyKey` |
| 结算锁定/出款结果 | `tenantId + settlementOrderSn/payoutOrderSn + action + idempotencyKey` |
| 对账差错调账 | `tenantId + reconciliationExceptionSn + adjustmentSn + idempotencyKey` |
| 余额重建/视图重放 | `tenantId + replayType + scopeHash + window + idempotencyKey` |

请求摘要必须包含决定资金和账务结果的稳定字段，排除 `id`、`sn`、`gmtCreate`、`gmtModified`、展示文案、操作者名称、运行时 message 和 traceId。

## 5.3 审计字段

高危写操作必须记录：

1. 操作者 ID、操作者类型、租户、来源系统、IP 或设备上下文。
2. 操作对象类型、对象流水、业务流水和幂等键。
3. 金额、币种、账目、主体、前置状态、后置状态。
4. 原因 code、原因说明、审批单引用、证据附件引用。
5. route snapshot、ledger transaction、entry 摘要或重放任务引用。

# 六、JSON 契约样例清单

JSON 样例用于验证 DSL 和服务契约可解析、可测试、可回归。建议落在后续测试资源目录中，命名先按业务语义，不按实现类命名。

| 样例 | 覆盖能力 | 验收重点 |
| --- | --- | --- |
| `direct-platform-transfer.json` | 直接交易 | 平台内部付款、route snapshot、posting plan 平衡。 |
| `direct-wallet-payment-with-fee.json` | 直接交易 | 本金和手续费拆分；费用归集账户入账。 |
| `reverse-refund-original-route.json` | 逆向交易 | 基于原 route snapshot 回放，不重新选路。 |
| `authorization-approve-multi-subject.json` | 授权交易 | 资金账户、信用账户、预算组组合占用整体成功或整体失败。 |
| `authorization-decline-no-posting.json` | 授权拒绝 | 只记录拒绝事实，无 route、无 entry、无 chargeback。 |
| `authorization-settlement-partial-release.json` | 授权结算 | 结算金额小于授权，差额释放或进入差错。 |
| `authorization-chargeback-dispute.json` | 争议拒付 | 争议拒付独立事实，累计上限正确。 |
| `balance-freeze-order.json` | 余额控制 | 冻结单为来源事实，`AVAILABLE -> FROZEN`。 |
| `balance-unfreeze-replay.json` | 余额控制 | 解冻引用冻结单，金额不超过剩余冻结。 |
| `clearing-settlement-payout.json` | 清结算 | 清算确认、结算锁定、出款成功的账目变化。 |
| `reconciliation-exception-adjustment.json` | 对账差错 | 差错阻断、调账来源和审计引用。 |
| `archive-balance-rebuild-watermark.json` | 余额重建 | checkpoint + watermark 无 gap、无 overlap。 |
| `transaction-view-replay-range.json` | 交易视图重放 | 时间范围、主体范围和只读投影边界。 |

# 七、全量测试清单

本节是进入编码前的统一测试 backlog，承接产品设计、DSL 契约矩阵、OpenSpec 和各系分设计。后续新增、修改或删除测试时，必须能追溯到本节或对应上游设计；如果上游设计新增能力，也必须同步补充本节测试项。

2026-05-17 CR 再校准：当前版本只继续推进交易、钱包和 ledger 层的测试与实现闭环；`FundsDirectTransactionService`、`FundsAuthorizationTransactionService`、`FundsBalanceControlService` 的服务门面单元测试、业务组合集成测试和测试包名对齐已完成。清分、清算、对账、账本账目归档、余额快照、完整外汇运营对象和相关运营工作台统一排入下一版本，不进入当前 CAD 自动提交批次。

测试状态口径：

| 状态 | 含义 |
| --- | --- |
| 已有 | 当前代码库已有同名或等价测试资产，后续持续回归。 |
| 待补 | P0/P1 目标态要求存在，当前尚未完整落地。 |
| 下版本 | 目标态保留，但不进入当前版本编码；等待独立 OpenSpec change、Harness 门禁或人工审批后再落地。 |
| 设计保留 | 依赖后续模型、表、外部适配或运营对象，先保留测试要求，代码实现时再落地。 |

测试层级口径：

| 层级 | 默认落地方式 | 使用原则 |
| --- | --- | --- |
| L1 契约/纯单元测试 | `core/src/test` 或 `tests/src/test`，不启动 Spring。 | DSL、枚举、route、posting、摘要、金额、状态机和 helper 规则优先使用。 |
| L2 应用服务测试 | `tests/src/test`，直接构造 service、fake/stub/mock 协作者。 | 幂等、生命周期、编排、失败路径和事务前置语义优先使用。 |
| L3 H2/集成测试 | `tests` 模块，H2 MySQL Mode 或最小 Spring 上下文。 | Mapper、DDL、唯一约束、本地事务、投影持久化确实需要数据库时使用。 |
| L4 架构/红线测试 | ArchUnit、Maven 依赖检查、边界测试或显式失败测试。 | 模块依赖、禁止入账主体、只读投影、不可绕过账本等红线使用。 |

## 7.1 当前测试资产索引

| 测试资产 | 层级 | 覆盖范围 | 状态 |
| --- | --- | --- | --- |
| `FundsInstructionSpecContractTests` | L1 | 指令业务标识、金额、引用、操作者和上下文字段。 | 已有 |
| `RouteDslContractTests` | L1 | route、snapshot、platform account、tool/external ref、replay policy。 | 已有 |
| `TransactionServiceAbilityDslJsonContractTests` | L1 | 交易层 JSON 样例可解析、能力覆盖、posting 平衡、无入账场景边界。 | 已有 |
| `SettlementPolicySpecTests` | L1 | 结算策略表达式、周期和边界日期。 | 已有 |
| `DefaultLedgerPostingAssemblerTests` | L1/L2 | route leg 到 posting plan、稳定引用、借贷方向、金额和币种。 | 已有 |
| `DefaultLedgerTransactionPostingServiceImplTests`、`DefaultLedgerTransactionPostingCurrencyValidationTests` | L2 | 成功入账、不平衡失败、外部主体失败、账本缺失失败、posting plan 与交易级币种红线。 | 已有 |
| `LedgerBalanceProjectionServiceImplTests` | L1/L2 | normal balance、禁止负数、受控负数、投影失败路径。 | 已有 |
| `FundsTransactionLedgerBalanceAssertionsTests` | L1/L2 | 核心资金变化的余额桶 delta 和 posting 平衡。 | 已有 |
| `FundsTransactionBusinessFlowIntegrationTests` | L2 | 充值 -> 付款 -> 退款，充值 -> 冻结 -> 提现，A -> B -> 商户 -> 提现。 | 已有 |
| `FundsBalanceControlBusinessFlowTests` | L2 | 资金账户调账、信用账户额度调额、预算组预算调整的余额桶、平台 `ADJUSTMENT` 和 `LIMIT` 边界。 | 已有 |
| `FundsTransactionFeeBusinessFlowTests` | L2 | 显式手续费付款、普通退款不退费、手续费退回和退费累计上限。 | 已有 |
| `FundsAuthorizationBusinessFlowTests` | L2 | 授权问询 -> 部分撤销 -> 部分结算 -> 部分退款、授权拒绝、授权直接结算和 `LIMIT` 红线。 | 已有 |
| `FundsSharedCardAuthorizationBusinessFlowTests` | L2 | 共享卡多主体授权同时占用信用账户、预算组和资金账户的余额桶、posting plan 和 `LIMIT` 红线。 | 已有 |
| `FundsTransactionOrchestrationFlowTests` | L2 | 普通 route resolver、orchestrator、recording posting service 编排链路。 | 已有 |
| `FundsTransactionOrchestrationReplayFlowTests` | L2 | 授权撤销、授权结算、拒付、解冻等 replay route resolver 编排链路。 | 已有 |
| `FundsTransactionCommandServiceImplTests` | L2 | 交易门面、幂等、请求摘要、授权拒绝和逆向链路。 | 已有 |
| `DefaultFundsInstructionLifecycleSaverTests` | L2 | 交易事实生命周期、引用事实回放、退款/拒付上限、冻结引用不回写消费。 | 已有 |
| `DefaultFundsFrozenOrderLifecycleSaverTests` | L2 | 冻结/解冻事实生命周期、剩余可释放金额、超额释放失败。 | 已有 |
| `FundsFrozenOrderServiceImplTests` | L2 | 冻结单创建、必填校验、金额和币种校验。 | 已有 |
| `DefaultLedgerProfileFundingAccountTests`、`DefaultLedgerProfileBudgetGroupTests`、`DefaultLedgerProfileRequiredItemTests` | L1/L2 | Funding/Credit/Budget/Platform profile 的 required 账目、normal balance、受控负数和缺账目失败。 | 已有 |
| `PlatformFundingAccountServiceImplTests` | L2 | 平台资金账户角色按租户、币种和角色解析到具体 FundingAccount，缺配置失败且不自动创建。 | 已有 |
| `PlatformFundingAccountRoleTests` | L1 | 平台资金账户角色名称、中文语义、FUNDING_PLATFORM profile 和目标账目映射。 | 已有 |
| `WalletLayerBoundaryTests` | L4 | wallet 只承载账户能力，不直接写交易事实或账本事实。 | 已有 |
| `LedgerLayerBoundaryTests`、`RouteLayerBoundaryTests` | L4 | ledger/route 分层与禁止依赖边界。 | 已有 |

## 7.2 P0 产品验收测试清单

| 测试组 | 来源验收 ID | 建议测试资产 | 层级 | 必须断言 | 状态 |
| --- | --- | --- | --- | --- | --- |
| 入金成功 | `PTDD-IN-001` | `FundsTransactionLedgerBalanceAssertionsTests`、`FundsTransactionOrchestrationFlowTests` | L1/L2 | 用户 `AVAILABLE` 增加；平台现金/预收路径可解释；外部账户不入账；posting 平衡。 | 已有 |
| 入金幂等 | `PTDD-IN-002` | `FundsTransactionCommandServiceImplTests` | L2 | 同 business key + request hash 重复不生成重复交易、snapshot、entry 或投影。 | 已有 |
| 入金异常挂账 | `PTDD-IN-003` | `ReconciliationExceptionAdjustmentTests` 或后续对账差错服务测试 | L2/L3 | 外部已成功但内部不可入账时不直接增余额，生成挂账/差错/补入账任务。 | 下版本 |
| 用户提现申请 | `PTDD-OUT-001` | `BalanceControlFundsInstructionRouteResolverTests`、`DefaultFundsFrozenOrderLifecycleSaverTests` | L1/L2 | 余额充足才冻结；`AVAILABLE -> FROZEN`；冻结单含原因、引用、操作者和审计。 | 已有 |
| 用户提现确认成功 | `PTDD-OUT-002` | `FundsTransactionBusinessFlowIntegrationTests`、`DefaultFundsInstructionLifecycleSaverTests` | L2 | 提现作为独立直接交易事实引用冻结单；消耗 `FROZEN`；不回写冻结单消费状态；重复成功不重复扣。 | 已有 |
| 用户提现失败回退 | `PTDD-OUT-003` | `DefaultFundsFrozenOrderLifecycleSaverTests`、后续出款结果服务测试 | L2 | `FROZEN -> AVAILABLE` 只释放一次；成功/失败不能双终态；保留失败原因。 | 部分已有 |
| 系统内转账 | `PTDD-PAY-001` | `TransferFundsInstructionRouteResolverTests`、`FundsTransactionBusinessFlowIntegrationTests` | L1/L2 | 付款方 `AVAILABLE` 减少，收款方 `AVAILABLE` 增加；同主体和币种不一致失败。 | 已有 |
| 普通支付 | `PTDD-PAY-002` | `FundsTransactionLedgerBalanceAssertionsTests` | L1/L2 | 付款方减少，收款方按命令指定桶增加；普通支付不默认进入商户清结算桶。 | 已有 |
| 商户订单收款 | `PTDD-MER-001` | `TransactionServiceAbilityDslJsonContractTests`、`FundsTransactionLedgerBalanceAssertionsTests` | L1/L2 | 用户 `AVAILABLE -> 商户 CLEARING`；不得直入 `AVAILABLE/SETTLEMENT`。 | 部分已有 |
| 商户清算批次完成 | `PTDD-MER-002` | `MerchantClearingBatchServiceTests`、`ClearingSettlementFlowIntegrationTests` | L2/L3 | `CLEARING -> AVAILABLE`；批次可追溯明细；重跑不重复清算；退款并发不超额。 | 下版本 |
| 商户结算锁定 | `PTDD-MER-003` | `SettlementOrderServiceTests` | L2/L3 | `AVAILABLE -> SETTLEMENT`；出款中不可再次结算；保留审批、规则版本和金额组成。 | 下版本 |
| 商户出款失败回退 | `PTDD-MER-004` | `PayoutResultServiceTests` | L2/L3 | `SETTLEMENT -> AVAILABLE` 只回退一次；失败原因、通道引用和处理人可审计。 | 下版本 |
| 原交易退款 | `PTDD-REF-001` | `DefaultRouteReplayServiceTests`、`DefaultFundsInstructionLifecycleSaverTests` | L1/L2 | 基于原 `RouteSnapshot`；不重新选路；累计退款不超过剩余可退金额。 | 已有 |
| 清算前退款 | `PTDD-REF-002` | `FundsTransactionLedgerBalanceAssertionsTests`、后续清算候选测试 | L2 | 商户 `CLEARING` 减少，用户 `AVAILABLE` 增加；清算批次排除已退款金额。 | 部分已有，清算候选下版本 |
| 清算后退款 | `PTDD-REF-003` | `FundsTransactionLedgerBalanceAssertionsTests` | L2 | 商户 `AVAILABLE` 减少，用户 `AVAILABLE` 增加；商户不足进入人工/负余额/追偿策略。 | 部分已有 |
| 出款中或已出款后退款 | `PTDD-REF-004` | 后续退款/追偿服务测试 | L2/L3 | 不直接做 `SETTLEMENT -> 用户 AVAILABLE`；必须进入人工或追偿流程。 | 下版本 |
| 手工直接退款 | `PTDD-REF-005` | `ManualRefundServiceTests` | L2 | 权限、审批、原因、凭证、额度缺失均失败；不得冒充原交易退款。 | 待补 |
| 手续费退回 | `PTDD-REF-006` | `DefaultRouteReplayServiceTests`、`TransactionServiceAbilityDslJsonContractTests`、`FundsTransactionCommandServiceImplTests`、`FundsTransactionFeeBusinessFlowTests` | L1/L2 | 普通退款不默认退费；费用退款只回放 fee leg。 | 服务层入口、业务组合与 replay 累计上限已补；按原 fee leg 已消费金额校验，避免多主体明细重复累计 |
| 授权成功 | `PTDD-AUTH-001` | `AuthorizationFundsInstructionRouteResolverTests`、`FundsTransactionLedgerBalanceAssertionsTests`、`FundsSharedCardAuthorizationBusinessFlowTests` | L1/L2 | 一个或多个主体 `AVAILABLE -> AUTHORIZATION`；多主体任一失败整体失败。 | 已有 |
| 授权拒绝 | `PTDD-AUTH-002` | `TransactionServiceAbilityDslJsonContractTests`、`FundsTransactionCommandServiceImplTests` | L1/L2 | 保存拒绝事实；无 route、无 entry；不增加 `chargebackAmount`。 | 已有 |
| 授权撤销 | `PTDD-AUTH-003` | `DefaultRouteReplayServiceTests` | L1/L2 | 基于原快照 `AUTHORIZATION -> AVAILABLE`；超剩余撤销失败。 | 已有 |
| 授权结算 | `PTDD-AUTH-004` | `DefaultRouteReplayServiceTests`、`FundsTransactionLedgerBalanceAssertionsTests` | L1/L2 | 结算金额不超过剩余授权；控制主体减少授权占用；不得 `AUTHORIZATION -> LIMIT`。 | 部分已有 |
| 授权链退款和争议拒付上限 | `PTDD-AUTH-005` | `DefaultFundsInstructionLifecycleSaverTests` | L2 | `refundedAmount + chargebackAmount <= settledAmount`；争议拒付不是授权拒付。 | 已有 |
| 冻结与解冻 | `PTDD-CTRL-001` | `DefaultFundsFrozenOrderLifecycleSaverTests`、`FundsFrozenOrderBoundaryTests` | L2/L4 | 冻结只做 `AVAILABLE <-> FROZEN`；不创建 `FundsTransaction`；超额解冻失败。 | 已有 |
| 资金余额调账 | `PTDD-CTRL-002` | `FundsBalanceControlInstructionConverterTests`、`FundsTransactionCommandServiceImplTests`、`BalanceControlFundsInstructionRouteResolverTests`、后续 `ReconciliationExceptionAdjustmentTests` | L1/L2 | 请求显式携带原因、凭证、审批和可选差错引用；缺原因、凭证或审批时交易编排前失败；通过平台 `ADJUSTMENT` 账户形成平衡分录。 | 已落地基础调账红线，差错调账状态机 P1 |
| 信用/预算额度调整 | `PTDD-CTRL-003`、`AT-BASE-040`、`AT-BASE-041` | `DefaultLedgerProfileBudgetGroupTests`、`DefaultLedgerProfileRequiredItemTests`、`BalanceControlFundsInstructionRouteResolverTests`、`FundsBalanceControlCommandServiceImplTests`、后续 `ControlAccountLedgerRulesTests` | L1/L2 | `FundsBalanceControlService#adjust` 支持资金账户余额调账、信用账户额度调整和预算组预算调整；资金账户生成 `BALANCE_ADJUST`，信用/预算生成 `LIMIT_ADJUST`；`LIMIT` 不开放给普通交易迁移；不新增 `CONSUMED`；预算调减受控负数必须有预算周期、审批、上限、账龄和治理路径。 | 已补服务门面和 route 红线测试，后续只保留回归保护 |
| 受控负余额 | `PTDD-CTRL-004`、`PTDD-CTRL-005` | `LedgerBalanceProjectionServiceImplTests`、`ControlledNegativeAvailableTests` | L1/L2 | 有来源、策略、上限、账龄、审批或风控标记；负余额不能当作可继续消费余额。 | 部分已有 |
| 余额控制无 FX | `PTDD-CTRL-006`、`AT-BASE-039` | `FundsBalanceControlInstructionConverterTests` | L1/L2 | freeze/unfreeze/adjust 金额币种必须等于账户或账本币种；不调用 `FxService`、不接收 FX 快照；错币种直接失败，不挂账、不生成 route/entry。 | 已落地 |
| 交易层门面归属 | `PTDD-ARCH-001` | `WalletLayerBoundaryTests`、`FundsTransactionServiceApiContractTests` | L4/L2 | 交易命令归 transaction；wallet 不直接写交易事实或账本事实。 | 已有 |
| 交易/资金对账差异 | `PTDD-REC-001` | `ReconciliationMatchingServiceTests` | L2/L3 | 创建对账差错单，不直接改账；数据源、批次、差异类型和处理状态完整。 | 下版本 |
| 账务一致性核对 | `PTDD-REC-002` | `LedgerConsistencyInspectionTests` | L2/L3 | 检出缺分录、重复分录、投影不平；不靠汇总数掩盖明细差异。 | 下版本 |
| 余额变更日志观察 | `PTDD-LEDGER-001`、`AT-BASE-036` | `LedgerBalanceProjectionServiceImplTests` | L2/L3 | 已扩展 `LedgerBalanceChangedEvent`，余额变化观察包含主体、账本、账目、币种、变更前余额、变更后余额、变更额、`ledgerTransactionSn`、`ledgerEntrySn`、分录摘要和业务引用；事件发布失败不回滚余额投影。 | 已落地 |
| 争议拒付入账 | `PTDD-DSP-001` | `DisputeChargebackServiceTests`、`DefaultFundsInstructionLifecycleSaverTests` | L2 | 原因码、证据、时限、责任可追踪；拒付上限正确。 | 部分已有 |
| 持牌能力和备付金口径 | `PTDD-NBFX-001` | `ComplianceCapabilityBoundaryTests` | L4 | 未确认许可证范围不得启用；普通平台不得把待结算资金称为客户备付金。 | 设计保留 |
| KYC/KYB 缺失拦截 | `PTDD-NBFX-002` | `OnboardingRiskBoundaryTests` | L2/L4 | 开户、收款、清算或出款被拦截；风险等级和受益所有人信息可审计。 | 设计保留 |
| 备付金账户对账差异 | `PTDD-NBFX-003` | `ReserveAccountReconciliationTests` | L2/L3 | 生成备付金差错；不直接改账；不靠汇总数掩盖明细。 | 设计保留 |
| 跨境真实性和数据跨境前置 | `PTDD-NBFX-004` | `CrossBorderComplianceBoundaryTests` | L4 | 缺材料、报价过期、数据出境未确认时交易失败或待材料。 | 设计保留 |
| VCC 授权/清算 | `PTDD-RAIL-VCC-001`、`PTDD-RAIL-VCC-002` | `VccAuthorizationRailTests` | L2 | 授权批准不是最终入账；清算金额与授权差异可解释。 | 设计保留 |
| ACH return/NOC/reversal | `PTDD-RAIL-ACH-001`、`PTDD-RAIL-ACH-002` | `AchRailReturnBoundaryTests` | L2 | debit 缺授权不得提交；return 不是退款；NOC 不改原资金事实。 | 设计保留 |
| 全球付款和退汇 | `PTDD-RAIL-GPAY-001`、`PTDD-RAIL-GPAY-002` | `GlobalPaymentRailTests` | L2 | message sent 不等于到账；退汇不是退款；费用、原因和回补路径可解释。 | 设计保留 |
| 错币种和业务换汇 | `PTDD-RAIL-FX-001`、`PTDD-RAIL-FX-002`、`PTDD-RAIL-FX-003`、`AT-BASE-037` | `DefaultFxServiceImplTests`、`FundsDirectTransactionInstructionConverterTests`、`FundsAuthorizationInstructionConverterTests` | L1/L2 | 错币种不静默入账；交易层不自动调用 `FxService`；错币种缺显式 FX 决策失败、挂账或进入差错；有业务显式 FX 决策时保留原币、目标币、汇率、费用、审批和外部依据。 | 部分闭合，完整外汇运营对象 P1 |
| 交易请求金额契约 | `PTDD-RAIL-FX-003`、`AT-BASE-037` | `FundsDirectTransactionInstructionConverterTests`、`FundsDirectTransactionFeeInstructionConverterTests`、`FundsAuthorizationInstructionConverterTests`、`FundsTransactionCommandServiceImplTests` | L1/L2 | `TransactionAmountSnapshot` 重命名为 `TransactionAmount`；`FundsTransactionPayRequest`、`FundsTransactionTransferRequest`、`FundsAuthorizationTransactionAuthorizeRequest` 的字段命名为 `transactionAmount`；`FundsTransactionTopupRequest`、`FundsTransactionWithdrawRequest`、`FundsAuthorizationTransactionSettleRequest` 纳入 `TransactionAmount`；类内部主金额字段保留 `amount`，不得改成 `ledgerAmount`；fee/reversal/auth refund/chargeback 保留 `Money amount`；`FundsTransactionRefundRequest` 待原交易引用和退款语义收敛后再决定。 | 已落地 |
| 非 RT 结算策略契约 | `AT-BASE-038`、`RED-020` | `SettlementPolicySpecTests`、后续 `SettlementPolicyContractTests` | L1/L2 | 已覆盖 `T+N/H+N/W/M/Q/Y/C` 表达式解析、候选日期计算和 `C@DD-DD` 非预定义账期；不支持表达式显式失败，不得静默降级 `RT`。 | 已落地 |

## 7.3 P1 产品验收测试清单

| 测试组 | 来源验收 ID | 建议测试资产 | 层级 | 必须断言 | 状态 |
| --- | --- | --- | --- | --- | --- |
| VA 收款匹配 | `PTDD-VA-001` | 后续 VA 收款匹配服务测试 | L2 | VA 只作为工具引用；未知 VA 进入挂账，不进入余额。 | 设计保留 |
| 外部通知乱序与补拉 | `PTDD-EXT-001` | 后续外部通知/补拉服务测试 | L2/L3 | 可信外部事实推进状态；不出现双终态；补拉有幂等和审计。 | 设计保留 |
| 手续费与通道成本分离 | `PTDD-FEE-001` | `FeeAndChannelCostAccountingTests` | L2 | 商户实收、平台费用、通道成本分别可解释；费率版本可追溯。 | 待补 |
| 争议证据提交和结果处理 | `PTDD-DSP-002` | 后续争议证据服务测试 | L2 | 证据最小必要、脱敏、版本留痕；资金结果和责任一致。 | 设计保留 |
| 商户负余额追偿 | `PTDD-DSP-003` | 后续商户追偿服务测试 | L2/L3 | 形成负余额、准备金扣减、追偿单或后续抵扣，不制造不平账。 | 待补 |
| 用户和商户账单展示 | `PTDD-VIEW-001` | `TransactionViewProjectionBoundaryTests` | L2/L4 | 展示来自事实和投影规则；商户账单区分 `CLEARING/AVAILABLE/SETTLEMENT`。 | 部分已有 |
| 运营时间线和财务报表 | `PTDD-VIEW-002` | `TransactionTimelineReportProjectionTests`、后续报表边界测试 | L2/L4 | 报表不依赖不可解释汇总；时间线不反向改写事实。 | 待补 |
| 交易视图有界重放 | `PTDD-VIEW-003` | `TransactionViewReplayRangeTests` | L1/L2 | 必须限定租户、视图域、时间窗口、主体、批次或单笔来源。 | 部分已有 |
| 归档后余额重建 | `PTDD-ARCH-002` | `BalanceProjectionArchiveContractTests` | L2/L3 | checkpoint/cold summary + watermark 后增量分录；不从视图反推余额。 | 部分已有 |
| 水位推进顺序 | `PTDD-ARCH-003` | `BalanceWatermarkAdvanceTests` | L2/L3 | 先计算、写入、校验，再推进水位；失败时水位停留原值。 | 已有 |
| 外汇报价和换汇执行 | `PTDD-NBFX-005` | `FxQuoteExecutionTests` | L2 | 过期报价不得换汇；原币、目标币、汇率、费用和差额可解释。 | 设计保留 |
| 监管或机构报送失败重试 | `PTDD-NBFX-006` | `RegulatoryReportingRetryTests` | L2/L3 | 不修改原交易事实；保留源批次、回执和失败原因。 | 设计保留 |

## 7.4 DSL 契约测试清单

| 契约组 | 来源 | 建议测试资产 | 层级 | 必须断言 | 状态 |
| --- | --- | --- | --- | --- | --- |
| Instruction 业务身份 | DSL 三 | `FundsInstructionSpecContractTests` | L1 | `tenantId/businessScene/businessSn` 必填；金额、原币、汇率、operator、reference 语义完整。 | 已有 |
| Instruction 引用边界 | DSL 三、OpenSpec transaction-layer | `FundsInstructionSpecContractTests`、`RouteReplayContractTests` | L1 | 退款、撤销、结算、拒付、费用退回、解冻缺引用必须失败。 | 部分已有 |
| 幂等摘要稳定 | DSL 三、API 5.2 | `FundsIdempotencyDigestContractTests` | L1 | 摘要排除数据库 ID、持久化流水、审计时间、展示文案、traceId。 | 待补 |
| Route 只表达资金路径 | DSL 四 | `RouteDslContractTests`、`RouteLayerBoundaryTests` | L1/L4 | route leg 不承载通道处理、审批、证据提交或页面流程。 | 已有 |
| Snapshot 版本和平台账户 | DSL 四 | `RouteDslContractTests`、`PlatformFundingAccountServiceImplTests` | L1/L2 | `snapshotSchemaVersion/routeVersion` 分离；平台角色解析到具体 funding account。 | 已有 |
| Tool/external ref 只作快照 | DSL 四、OpenSpec ledger | `LedgerPostableSubjectContractTests` 或现有边界测试 | L1/L4 | 银行卡、VA、VCC、PSP、外部账户不得成为 `LedgerEntry.subjectType`。 | 部分已有 |
| Replay 使用原路径 | DSL 七 | `DefaultRouteReplayServiceTests` | L1/L2 | 原绑定关系变化后仍使用原 snapshot；未知 schema version 失败。 | 已有 |
| Posting plan 独立平衡 | DSL 五、OpenSpec ledger | `DefaultLedgerPostingAssemblerTests`、`DefaultLedgerTransactionPostingServiceImplTests` | L1/L2 | 每个 plan 同币种借贷相等；空 plan、混币种、不平衡失败。 | 已有 |
| Posting plan 摘要 | DSL 五、OpenSpec ledger | `LedgerPostingPlanDigestContractTests` | L1 | 相同账务计划语义重算一致；`routeLegId`、`postingScope` 和 `balanceEffectType` 变化会改变摘要。 | 已有 |
| Ledger entry 摘要 | DSL 五、OpenSpec ledger | `LedgerEntryDigestContractTests` | L1 | 相同业务事实重算一致；排除 entry sn、ID、ledger transaction sn、plan sn、审计时间。 | 已有 |
| Entry 展示语义隔离 | DSL 五 | `TransactionViewProjectionBoundaryTests` | L4 | Entry 不出现账单标题、展示原因、国际化文案。 | 部分已有 |
| Profile 和余额桶 | DSL 六、OpenSpec wallets | `DefaultLedgerProfileFundingAccountTests`、`DefaultLedgerProfileBudgetGroupTests`、`DefaultLedgerProfileRequiredItemTests`、`LedgerBalanceProjectionServiceImplTests` | L1/L2 | 账目规则由 profile 决定；`CONSUMED` 不入账；平台 profile 包含 `CASH/PREPAYMENT/CLEARING/SETTLEMENT/FEE/ADJUSTMENT`。 | 已有 |
| 冻结只迁移同主体余额 | DSL 六、OpenSpec wallets | `FundsFrozenOrderBoundaryTests`、`DefaultFundsFrozenOrderLifecycleSaverTests` | L2/L4 | 冻结不改变资金归属，不跨主体转移，不创建 `FundsTransaction`。 | 已有 |
| JSON 样例机械校验 | DSL 8.0 | `TransactionServiceAbilityDslJsonContractTests` | L1 | JSON 可解析；posting plan 平衡；平台角色不直接入账；query/replay 样例无 posting。 | 已有 |
| 归档和视图重放 | DSL 十 | `BalanceProjectionArchiveContractTests`、`TransactionViewReplayRangeTests` | L2/L3 | watermark 边界、checkpoint、manifest、有界重放、禁止视图反推余额。 | 部分已有 |

## 7.5 系分服务测试清单

| 能力域 | 来源系分 | 建议测试资产 | 层级 | 必测行为 | 状态 |
| --- | --- | --- | --- | --- | --- |
| Ledger Posting | `Ledger DSL Posting 系分设计.md` | `DefaultLedgerPostingAssemblerTests`、`DefaultLedgerTransactionPostingServiceImplTests` | L1/L2 | 成功入账、plan 不平衡失败、混币种失败、外部主体失败、ledgerId 缺失失败。 | 已有 |
| Ledger 投影 | `Ledger DSL Posting 系分设计.md` | `LedgerBalanceProjectionServiceImplTests` | L1/L2 | 借方/贷方 normal balance、禁止负数、受控负数、账本不匹配失败。 | 已有 |
| Ledger 事务回滚 | `Ledger DSL Posting 系分设计.md` | `LedgerPostingLocalTransactionTests` | L3 | 分录写入后投影失败时交易、计划、分录全部回滚。 | 待补 |
| Wallet 账户初始化 | `Wallets 账户与余额控制系分设计.md` | `DefaultSubjectLedgerInitializerTests`、`DefaultLedgerProfileFundingAccountTests`、`DefaultLedgerProfileBudgetGroupTests`、`DefaultLedgerProfileRequiredItemTests` | L2 | required ledger 初始化、幂等创建、缺 profile 失败。 | 已有 |
| Wallet 余额查询 | `Wallets 账户与余额控制系分设计.md` | `DefaultFundsAccountQueryServiceImplTests` | L2 | 未建账展示查询返回 initialized=false；写流程缺账本失败；空集合不返回 null。 | 已有 |
| Platform role 解析 | `Wallets 账户与余额控制系分设计.md` | `PlatformFundingAccountServiceImplTests`、`PlatformFundingAccountRoleTests` | L1/L2 | 平台角色解析到具体 funding account；缺角色失败；角色不作为 entry subject。 | 已有 |
| 冻结订单生命周期 | `Wallets`、`交易层服务能力系分设计.md` | `FundsFrozenOrderServiceImplTests`、`DefaultFundsFrozenOrderLifecycleSaverTests` | L2 | 创建、冻结成功、部分解冻、全部解冻、超额解冻、审计字段。 | 已有 |
| 授权占用 | `Wallets`、`交易层服务能力系分设计.md` | `AuthorizationFundsInstructionRouteResolverTests`、`DefaultRouteReplayServiceTests`、`FundsAuthorizationBusinessFlowTests`、`FundsSharedCardAuthorizationBusinessFlowTests` | L1/L2 | 资金、信用、预算授权占用、释放、结算、授权拒绝无分录。 | 部分已有 |
| 直接交易服务 | `交易层服务能力系分设计.md` | `FundsTransactionCommandServiceImplTests`、`DefaultRoutedFundsInstructionOrchestratorTests` | L2 | 成功入账、重复请求、摘要冲突、失败回滚、ledger posting 幂等命中。 | 已有 |
| 逆向交易服务 | `交易层服务能力系分设计.md` | `DefaultRouteReplayServiceTests`、`DefaultFundsInstructionLifecycleSaverTests` | L2 | 原快照回放、累计上限、状态冲突、费用退回规则。 | 部分已有 |
| 余额控制服务 | `交易层服务能力系分设计.md` | `DelegatingFundsInstructionLifecycleRecorderTests`、`BalanceControlFundsInstructionRouteResolverTests` | L1/L2 | lifecycle recorder 只能命中一个；冻结和解冻进入 FrozenOrder recorder；调账进入标准交易 recorder。 | 已有 |
| 清算批次 | `清结算与对账系分设计.md` | `MerchantClearingBatchServiceTests` | L2/L3 | 候选生成、批次版本、重跑幂等、退款/争议阻断、审批留痕。 | 下版本 |
| 结算与出款 | `清结算与对账系分设计.md` | `SettlementOrderServiceTests`、`PayoutResultServiceTests` | L2/L3 | 结算锁定、重复出款阻断、成功出款、失败回退、外部回单引用。 | 下版本 |
| 对账差错 | `清结算与对账系分设计.md` | `ReconciliationMatchingServiceTests`、`ReconciliationExceptionAdjustmentTests` | L2/L3 | 匹配、差错创建、阻断、放行、调账、核销、审计。 | 下版本 |
| 报表边界 | `清结算与对账系分设计.md` | 后续报表边界测试 | L4 | 报表从事实和明细派生，不反写账本或钱包事实。 | 下版本 |
| 余额归档 | `归档投影与指标治理系分设计.md` | `BalanceProjectionArchiveContractTests`、`BalanceWatermarkAdvanceTests` | L2/L3 | 预检查、checkpoint、manifest、水位推进、冷热拼接无 gap/overlap。 | 下版本 |
| 交易视图重放 | `归档投影与指标治理系分设计.md` | `TransactionViewReplayRangeTests` | L2/L4 | 必须限定范围；只写读模型或差异报告；不生成 route/entry。 | 下版本 |
| 指标治理 | `归档投影与指标治理系分设计.md` | `MetricWatermarkTests` | L2 | 指标水位独立，不复用归档时间边界；失败可重跑。 | 下版本 |
| 架构边界 | 全部系分 | `LedgerLayerBoundaryTests`、`RouteLayerBoundaryTests`、`WalletLayerBoundaryTests` | L4 | face 不依赖 impl/Entity/Mapper；wallet 不写交易事实；route 不写账本事实。 | 已有 |

## 7.5A 当前版本三类服务测试矩阵

本节是当前版本继续编码的直接输入，优先级高于 7.5 中已标记为下版本的清结算、对账、归档、余额快照和完整外汇运营任务。

| 服务 | 服务门面单元测试必须覆盖 | 业务组合集成测试必须覆盖 |
| --- | --- | --- |
| `FundsDirectTransactionService` | `topup/pay/transfer/withdraw/refund/fee/refundFee` 的指令类型、事件、参与方、route leg、显式 `TransactionAmount`、`FeeSpec`、本金/费用汇总、幂等和失败不入编排；`refundFee` 必须引用原手续费事实所在交易。 | `充值 -> 付款 -> 退款`、`充值 -> 冻结 -> 提现`、`A 充值 -> 转给 B -> B 付款 -> B 提现`、`付款含手续费 -> 本金退款 -> 费用退款`。 |
| `FundsAuthorizationTransactionService` | `authorize/reversal/settle/authRefund/chargeback` 的原交易引用、route replay、授权拒绝无 posting、部分撤销/部分结算/部分退款、共享卡多主体、拒付上限和 `LIMIT` 红线。 | `交易问询 -> 部分撤销 -> 部分结算 -> 部分退款`、`交易问询 -> 授权拒付`、授权交易直接结算、资金账户/共享卡/预算组组合授权。 |
| `FundsBalanceControlService` | `freeze/unfreeze/adjust` 的冻结引用、多次解冻、资金账户余额调账、信用额度调额、预算组调额、受控负数、治理上下文和无 FX 边界。 | `一次冻结多次解冻`、资金账户调额、信用账户额度调额、预算组预算调额，以及与提现、授权链路组合后的余额桶回归。 |

组合测试统一要求：每一步都断言相关主体余额桶 delta、posting plan 平衡、ledger transaction 可追溯和幂等行为；不得只断言最终余额或交易状态。

## 7.6 红线失败测试清单

源产品 TDD 文档存在一个重复编号 `RED-013`。本文用 `RED-013A` 表示“未确认真实性执行跨境外汇”，用 `RED-013B` 表示“轨道状态混用”，后续修订源文档时应同步去重。

| 红线 ID | 建议测试资产 | 层级 | 必须失败条件 | 状态 |
| --- | --- | --- | --- | --- |
| `RED-001` | `LedgerPostableSubjectContractTests`、`RouteLayerBoundaryTests` | L1/L4 | 外部银行账户、VA、VCC、PSP 账户被创建为内部 ledger subject。 | 部分已有 |
| `RED-002` | `DefaultRouteReplayServiceTests` | L1/L2 | 退款、撤销、结算、拒付、手续费退回缺原 `RouteSnapshot`。 | 已有 |
| `RED-003` | `DefaultFundsInstructionLifecycleSaverTests` | L2 | 累计退款超过剩余可退金额。 | 已有 |
| `RED-004` | `TransactionServiceAbilityDslJsonContractTests`、`FundsTransactionCommandServiceImplTests` | L1/L2 | 授权拒绝写入 `chargebackAmount` 或生成账务路径。 | 已有 |
| `RED-005` | `TransactionServiceAbilityDslJsonContractTests`、`MerchantCollectionRouteTests` | L1/L2 | 商户订单款直入 `AVAILABLE` 或 `SETTLEMENT`。 | 部分已有 |
| `RED-006` | 后续退款/追偿红线测试 | L2 | 出款中资金被系统自动反向给用户退款。 | 待补 |
| `RED-007` | `DefaultLedgerPostingAssemblerTests` | L1 | 任一 posting plan 借贷不平仍保存。 | 已有 |
| `RED-008` | `FundsBalanceControlInstructionConverterTests`、`FundsTransactionCommandServiceImplTests` | L1/L2 | 无原因、凭证或审批的资金调账成功；差错引用作为对账差错调账的可选来源引用。 | 已落地基础调账红线，完整对账差错调账 P1 |
| `RED-009` | 后续对账差错边界测试 | L2/L4 | 对账差异直接修改历史 entry 或余额。 | 待补 |
| `RED-010` | `TransactionViewProjectionBoundaryTests` | L4 | 展示投影写回 ledger 事实。 | 部分已有 |
| `RED-011` | `ControlledNegativeAvailableTests` | L2 | 负 `AVAILABLE` 被当作可继续消费、授权或出款余额。 | 部分已有 |
| `RED-012` | `ComplianceCapabilityBoundaryTests` | L4 | 未确认资质启用支付账户、备付金、跨境或外汇能力。 | 设计保留 |
| `RED-013A` | `CrossBorderComplianceBoundaryTests` | L4 | 缺真实性材料、报价过期或数据出境未确认仍执行跨境/换汇。 | 设计保留 |
| `RED-013B` | `RailStateBoundaryTests` | L2 | VCC 授权、ACH 提交或全球付款报文成功被当作最终到账。 | 设计保留 |
| `RED-014` | `FundsInstructionFxContractTests` | L1/L2 | 错币种交易静默按目标币种入账，无汇率、审批和差错记录。 | 部分已有 |
| `RED-014A` | `FundsDirectTransactionInstructionConverterTests`、`FundsAuthorizationInstructionConverterTests`、`FundsTransactionCommandServiceImplTests` | L1/L2 | converter、交易层或资金底座自动调用 `FxService` 并生成目标币种入账。 | 已落地 |
| `RED-014B` | `FundsBalanceControlInstructionConverterTests` | L1/L2 | `FundsBalanceControlService` 对冻结、解冻、余额调账或额度/预算调整自动调用 `FxService`，或接收 FX 快照后生成目标币种余额控制。 | 已落地 |
| `RED-015` | `TransactionViewReplayRangeTests` | L2/L4 | 交易视图无范围全量在线重放。 | 部分已有 |
| `RED-016` | `BalanceProjectionArchiveContractTests` | L2/L4 | 余额重建依赖用户账单、商户账单或报表投影。 | 已有 |
| `RED-017` | `BalanceProjectionArchiveContractTests` | L2/L3 | 用 180 天、自然日或归档日期作为冷热计算边界。 | 已有 |
| `RED-018` | `BalanceWatermarkAdvanceTests` | L2/L3 | 先推进水位再计算区间分录。 | 已有 |
| `RED-019` | `LedgerBalanceProjectionServiceImplTests` | L2/L3 | 业务余额变更日志被当作余额事实源，或反向修改分录、余额投影。 | 已落地 |
| `RED-020` | `SettlementPolicySpecTests` | L1/L2 | `SettlementPolicySpec` 表达式不支持或解析失败时仍按实时结算生成候选。 | 已落地 |

## 7.7 测试编写规范

1. 所有新增测试方法必须以 `test` 开头，并采用 `test<UseCase>Should<Expected>` 风格。
2. 关键资金测试的方法上方必须写明“场景、输入、输出、预期、红线”；不把这些说明写进方法体内部。
3. 有资金变化的测试不得只断言交易状态、route 或 entry 数量，必须同时断言所有相关主体的账本余额桶、posting 平衡和幂等行为。
4. 业务组合测试必须逐步断言每一步余额变化，不能只断言最终余额。
5. 测试数据必须有业务语义，避免 `foo`、`test1`、无含义金额和无含义流水；金额和币种必须服务断言意图。
6. 普通规则、DSL、route、posting、摘要、状态机优先无 Spring 单测；只有 mapper、DDL、唯一约束、本地事务或 H2 schema 需要时才启动 Spring/H2。
7. 外部银行、通道、卡组织、VA、PSP、监管报送、KYC/KYB 等依赖必须使用 fake、stub、mock、WireMock 或 Testcontainers，不得连接真实外部服务。

测试包名对齐规则：

| 测试类型 | 推荐包名 | 适用测试 |
| --- | --- | --- |
| 交易门面服务单元测试 | `com.capte.funds.transaction.application` | `FundsDirectTransactionService`、`FundsAuthorizationTransactionService`、`FundsBalanceControlService` 的服务门面测试和共享 support。 |
| 交易指令编排测试 | `com.capte.funds.transaction.application.orchestration` | `DefaultRoutedFundsInstructionOrchestrator` 主链路、幂等短路、route snapshot、失败生命周期和 replay 编排。 |
| 交易业务组合集成测试 | `com.capte.funds.transaction.application.flow` | 充值、付款、退款、冻结、提现、授权、调额等跨服务组合链路。 |
| 交易契约测试 | `com.capte.funds.transaction.contract` | 交易层 API 兼容性、参与方模型、交易明细 DTO 和稳定字段红线。 |
| 交易边界测试 | `com.capte.funds.transaction.boundary` | 交易事实边界、冻结单边界、展示投影延后和生产源码红线扫描。 |
| 交易账务口径测试 | `com.capte.funds.transaction.accounting` | 手续费、通道成本、账务口径和 DSL/生产源码一致性红线。 |
| 交易账本断言测试 | `com.capte.funds.transaction.ledger` | 交易层账本 posting、余额变化和断言支撑自测。 |
| 交易 converter 测试 | `com.capte.funds.transaction.converter` | 请求模型到 `FundsInstructionSpec` 的转换和 FX/错币种边界。 |
| 路由测试 | `com.capte.funds.route` | `RouteResolver`、route replay、route subject 和 platform account route support。 |
| 交易生命周期实现测试 | `com.capte.funds.transaction.services.impl` | `FundsInstructionLifecycleRecorder`、冻结单 lifecycle、查询服务实现。 |
| 钱包账户能力测试 | `com.capte.funds.wallet` / `com.capte.funds.wallet.services.impl` | wallet 边界、profile、账户、支付工具、平台账户角色和查询服务。 |
| Ledger 测试 | `com.capte.funds.ledger` / `com.capte.funds.ledger.impl` | posting、entry digest、transaction service、balance projection。 |
| 跨域共享测试支撑 | `com.capte.funds.support` | 被 wallet、transaction、ledger 多域复用的测试 helper 和领域断言支撑。 |

现有 `com.capte.funds.transaction` 根包作为迁移期包名保留；历史 `com.capte.funds.transaction.flow` 已迁移到 `com.capte.funds.transaction.application.flow`。新增测试优先使用上表包名，迁移旧类时必须同步文件路径、`package` 声明、聚焦验证命令和文档引用。

迁移进度：三类交易门面服务测试和共享 support 已迁移到 `com.capte.funds.transaction.application`；交易指令编排测试已迁移到 `com.capte.funds.transaction.application.orchestration`；交易契约测试已迁移到 `com.capte.funds.transaction.contract`；交易边界测试已迁移到 `com.capte.funds.transaction.boundary`；交易账务口径测试已迁移到 `com.capte.funds.transaction.accounting`；交易账本断言测试已迁移到 `com.capte.funds.transaction.ledger`；跨 ledger/transaction 复用的 `FundsTransactionTestSupport` 已迁移到 `com.capte.funds.support`；交易业务组合集成测试已迁移到 `com.capte.funds.transaction.application.flow`。`com.capte.funds.transaction` 根包测试迁移已清空，后续新增测试必须按能力归属选择上表包名。

Route 层 CR 后补测矩阵：

| 覆盖面 | 必补场景 | 目标测试 |
| --- | --- | --- |
| resolver 组合选择 | 单命中、多命中优先级、无匹配、`Ordered` 排序、自身排除、replay resolver 优先。 | `CompositeRouteResolverTests` |
| 直接交易路径 | topup、withdraw、pay、transfer、fee、fee refund、错币种平台账户、缺平台账户、无原交易引用退款边界。 | `TransferFundsInstructionRouteResolverTests`、`TransferFundsInstructionFeeRouteResolverTests`、route replay 测试 |
| 余额控制路径 | freeze、unfreeze、资金账户余额调账、受控负余额、信用额度调额、预算组预算调额、unsupported subject/event。 | `BalanceControlFundsInstructionRouteResolverTests`、`BalanceControlFundingAdjustRouteResolverTests`、`BalanceControlLimitAdjustRouteResolverTests` |
| 授权路径 | authorize approve/decline、共享卡授权、授权撤销、授权结算、授权退款、拒付、多主体原路回放。 | `AuthorizationFundsInstructionRouteResolverTests`、`AuthorizationSharedCardFundsInstructionRouteResolverTests`、route replay 测试 |
| replay policy | 缺 reference、缺原 route snapshot、空 route legs、部分 legId 缺失、重复消费、`EXTERNAL_TRANSACTION` 不回放。 | `DefaultRouteReplayServiceTests`、`DefaultRouteReplayAuthorizationTests`、`DefaultRouteReplayDirectRefundTests`、`DefaultRouteReplayPolicyTests` |
| 架构边界 | route 层不写交易事实、冻结事实、账本事实和余额投影，不依赖 DAL mapper、posting service 或 transaction application command service。 | `RouteLayerBoundaryTests` |

授权后续事件的默认测试口径：`REVERSAL`、`SETTLE`、`AUTH_REFUND` 必须引用原授权事实并走 route replay；缺 `reference` 时应失败，不允许 fallback 为普通授权 route。若后续需要兼容历史无引用请求，必须单独标记兼容场景，并把兼容范围写入测试名和方法说明。

## 7.8 推荐验证命令

| 变更范围 | 建议命令 |
| --- | --- |
| DSL / core 契约 | `mvn -pl core -am test -Dtest=FundsInstructionSpecContractTests,RouteDslContractTests,TransactionServiceAbilityDslJsonContractTests` |
| Ledger posting / 投影 | `mvn -pl tests -am test -Dtest=DefaultLedgerPostingAssemblerTests,DefaultLedgerTransactionPostingServiceImplTests,DefaultLedgerTransactionPostingCurrencyValidationTests,LedgerBalanceProjectionServiceImplTests` |
| Transaction layer | `mvn -pl tests -am test -Dtest=FundsTransactionCommandServiceImplTests,DefaultRoutedFundsInstructionOrchestratorTests,DefaultRoutedFundsInstructionOrchestratorReplayTests,DefaultRoutedFundsInstructionOrchestratorReplayPolicyTests,DefaultFundsInstructionLifecycleSaverTests` |
| Transaction service facade | `mvn -pl tests -am test -Dtest=FundsTransactionCommandServiceImplTests,FundsAuthorizationTransactionCommandServiceImplTests,FundsBalanceControlCommandServiceImplTests` |
| Route resolver / replay | `mvn -pl tests -am test -Dtest=CompositeRouteResolverTests,TransferFundsInstructionRouteResolverTests,TransferFundsInstructionFeeRouteResolverTests,BalanceControlFundsInstructionRouteResolverTests,BalanceControlFundingAdjustRouteResolverTests,BalanceControlLimitAdjustRouteResolverTests,AuthorizationFundsInstructionRouteResolverTests,AuthorizationSharedCardFundsInstructionRouteResolverTests,DefaultRouteReplayServiceTests,DefaultRouteReplayAuthorizationTests,DefaultRouteReplayDirectRefundTests,DefaultRouteReplayPolicyTests,RouteLayerBoundaryTests` |
| Frozen order / balance control | `mvn -pl tests -am test -Dtest=FundsFrozenOrderServiceImplTests,DefaultFundsFrozenOrderLifecycleSaverTests,BalanceControlFundsInstructionRouteResolverTests` |
| Wallet account capability | `mvn -pl tests -am test -Dtest=DefaultLedgerProfileFundingAccountTests,DefaultLedgerProfileBudgetGroupTests,DefaultLedgerProfileRequiredItemTests,DefaultFundsAccountQueryServiceImplTests,PlatformFundingAccountServiceImplTests,PlatformFundingAccountRoleTests,WalletLayerBoundaryTests` |
| 资金变化组合链路 | `mvn -pl tests -am test -Dtest=FundsTransactionLedgerBalanceAssertionsTests,FundsTransactionBusinessFlowIntegrationTests,FundsBalanceControlBusinessFlowTests,FundsTransactionFeeBusinessFlowTests,FundsTransactionOrchestrationFlowTests,FundsTransactionOrchestrationReplayFlowTests` |
| 架构边界 | `mvn -pl tests -am test -Dtest=LedgerLayerBoundaryTests,RouteLayerBoundaryTests,WalletLayerBoundaryTests` |
| 全量基础验证 | `mvn compile`，再按变更范围执行聚焦测试，最后执行团队认可的静态扫描。 |

# 八、Harness 门禁映射

| Harness Stage | 本地命令 | 阻断条件 |
| --- | --- | --- |
| Environment | `mvn -version` | 不是 JDK 21 或 Maven runtime 不符合要求。 |
| Compile | `mvn compile` | 任一模块编译失败。 |
| Focused Unit Tests | `mvn -pl <module> -am test -Dtest=<TestClass>` | 相关模块单测失败。 |
| Contract Tests | `mvn -pl core -am test -Dtest=*ContractTests` | DSL JSON、枚举、route、posting 契约失败。 |
| Integration Tests | `mvn -pl tests -am test -Dtest=<IntegrationTest>` | 本地事务、投影、清结算、对账集成失败。 |
| Architecture Tests | 待代码落地后补 ArchUnit 或等价测试命令 | face/impl 依赖方向、禁止依赖规则失败。 |
| Static Scan | `mvn pmd:check` 或团队等价规约检查 | 当前阶段非阻塞；恢复后 PMD 或团队等价规约失败才阻断。 |
| Manual Approval | Harness 人工审批 | 资损、出款、归档、对账、数据修复、高危后台操作缺审批材料。 |

当前阶段只设计门禁，不配置真实 Harness org/project/pipeline，不写入凭据，不触发远程构建。

静态扫描执行说明：

1. 当前 CAD/Harness 轮次暂不把 `mvn pmd:check` 作为阻塞门禁。
2. `mvn pmd:check` 失败必须先区分 PMD 规则失败和 Maven 依赖解析失败。
3. 若多模块本地执行时因为 `wind-funds-core` 等 reactor artifact 被解析到远端 snapshot 仓库而失败，先记录为环境问题，不阻塞当前轮次。
4. 依赖解析失败不能视为规约通过；交付说明必须记录失败模块、失败依赖和与本次变更的关系。
5. 长期应调整父 POM 或 PMD 插件配置，确保多模块 reactor 下静态扫描优先使用当前 reactor 产物。

## 8.1 OpenSpec / Superpowers / Harness 再审查结论

本轮把当前规划重新按 `OpenSpec 定规格`、`Superpowers 定执行纪律`、`Harness 定验证门禁` 三层审查，结论如下：

1. OpenSpec 层：已完成的 P0-R、Route replay 与手续费 CR、控制账户调额设计均能追溯到 `transaction-layer`、`wallets`、`payment-ledger` 和 `clearing-reconciliation` 规格域；后续任何新增公共契约、状态机、余额桶语义、清结算对象或 FX 运营对象，必须先补 OpenSpec change 的 `proposal / design / tasks / spec delta`，不得只在代码里隐式新增规则。
2. Superpowers 层：后续编码任务继续使用“用例和红线先行”的节奏，每个工作包先补失败用例、契约测试或验收矩阵，再做最小实现；每完成一个工作包就做代码 Review 和任务状态回写，不把命名治理、业务逻辑、DDL 和测试大迁移混成一轮。本轮 CR 后，当前版本继续推进交易、钱包、ledger 和 route 层；清分、清算、对账、账本账目归档、余额快照和完整外汇运营对象全部排入下版本。
3. Harness 层：当前仍只使用本地 Harness 等价门禁，不创建真实 pipeline；每个工作包必须明确写入范围、聚焦测试、是否需要 `just compile`、是否需要人工审批。涉及资损、出款、清结算、对账差错、归档、数据修复或 DDL 的任务进入 Manual Approval 阶段，不进入当前 CAD 自动提交批次。

当前规划的状态重新整理为：

| 顺序 | 工作包 | OpenSpec 检查 | Superpowers 执行纪律 | Harness 门禁 | 当前状态 |
| --- | --- | --- | --- | --- | --- |
| 0 | P0-C Ledger Posting 主链路 | `payment-ledger` 与 `transaction-layer` 已覆盖 posting plan、entry、balance projection 和幂等入账结果主契约。 | 已按账本入账正向、写前失败、投影缺省语义和命名兼容测试驱动完成。 | `DefaultLedgerTransactionPostingServiceImplTests`、`DefaultLedgerTransactionPostingCurrencyValidationTests`、`DefaultLedgerTransactionPostingValidationTests`、`LedgerBalanceProjectionServiceImplTests`、`DefaultFundsAccountQueryServiceImplTests`。 | 已闭合，后续只保留回归保护。 |
| 1 | P0-R 产品口径回归 | `transaction-layer`、`payment-ledger`、`clearing-reconciliation` 已覆盖 FX 外置、余额日志和结算策略红线。 | 已按红线用例驱动完成。 | `SettlementPolicySpecTests`、FX converter、余额控制和余额投影事件测试。 | 已闭合，后续只保留回归保护。 |
| 2 | P0-CTRL 控制账户调额 | `wallets` 规格承接信用额度和预算组控制语义；`transaction-layer` 规格承接 `FundsBalanceControlService#adjust` 入口。 | 已按信用额度调增/调减、预算调增/调减、受控负数和 `LIMIT` 红线测试驱动完成。 | `FundsTransactionCommandServiceImplTests`、`BalanceControlFundsInstructionRouteResolverTests`、`just compile`。 | 已闭合，后续只保留回归保护。 |
| 3 | P0-E Wallets 账户与余额控制 | `wallets` 规格继续保护账户 profile、平台角色、冻结事实、受控负余额和预算治理。 | 保持 wallet 是账户能力层，不承载交易命令；新增资金变化必须先补余额断言。 | `DefaultLedgerProfileFundingAccountTests`、`DefaultLedgerProfileBudgetGroupTests`、`DefaultLedgerProfileRequiredItemTests`、`PlatformFundingAccountServiceImplTests`、`WalletLayerBoundaryTests`、`ControlAccountLedgerRulesTests`。 | 已闭合，后续只保留回归保护。 |
| 4 | P0-G 命名治理残余 | OpenSpec 不直接驱动纯命名，但公共契约名称变化必须同步 spec delta。 | 已按账本入账结果、生命周期记录器、余额控制服务和账户类型语义轴分批治理；包名统一保留为独立重构轮次。 | `WalletLayerBoundaryTests`、`FundsTransactionServiceApiContractTests`、`DefaultFundsAccountTypeTests`、`PlatformFundingAccountRoleTests`。 | 已闭合，后续只保留回归保护。 |
| 5 | P0-H-API 三类服务测试矩阵 | 测试治理不新增规格，但必须保持 PRD、DSL 和 OpenSpec 验收口径可追溯。 | 已补 `FundsDirectTransactionService`、`FundsAuthorizationTransactionService`、`FundsBalanceControlService` 的服务门面单测和业务组合集成测试矩阵。 | `FundsTransactionCommandServiceImplTests`、`FundsAuthorizationTransactionCommandServiceImplTests`、`FundsBalanceControlCommandServiceImplTests`、`FundsTransactionBusinessFlowIntegrationTests`、`FundsBalanceControlBusinessFlowTests`、`FundsTransactionFeeBusinessFlowTests`、`FundsAuthorizationBusinessFlowTests`、`FundsSharedCardAuthorizationBusinessFlowTests`。 | 已完成：三类服务门面与业务组合矩阵均已有回归测试。 |
| 6 | P0-H-PKG 测试包名对齐 | OpenSpec 不直接约束测试包名，但包名必须反映能力归属，避免 wallet/transaction 历史语义漂移。 | 小批次迁移 `com.capte.funds.transaction`、`com.capte.funds.transaction.flow` 到 `application`、`application.flow`、`contract`、`boundary`、`accounting`、`ledger`、`support` 等目标包名，不夹带业务逻辑。 | 聚焦执行被迁移测试类，必要时补 `rg "package com.capte.funds.transaction"` 人工复核。 | 已完成：交易根包测试已清空，门面服务、指令编排、契约、边界、账务口径、账本断言、共享 support 和业务组合 flow 测试均按能力归属迁移。 |
| 7 | P0-ROUTE 路由层 CR 与场景覆盖 | `transaction-layer` 规格已承接 route replay、`RouteResolver#supports` 和资金路径边界；本轮未新增 face 契约。 | 已补 Composite、direct、balance、authorization、replay policy 和 route boundary 测试，并完成 replay 收敛、重复逻辑抽取和过时入口清理。 | `CompositeRouteResolverTests`、各类 `*RouteResolverTests`、`DefaultRouteReplay*Tests`、`RouteLayerBoundaryTests`、`mvn compile`。 | 已闭合，后续只保留回归保护。 |
| 8 | P1-CLR 清结算与对账 | `clearing-reconciliation` 需要补产品层 `SettlementPolicy`、候选、批次、结算单、出款单、差错单 spec delta。 | 下版本再做模型、契约测试和批处理实现，不直接进入当前 CAD 批次。 | 进入清结算集成测试、对账差错测试和 Manual Approval。 | 下版本。 |
| 9 | P1-FX / P1-ARC 外汇运营与归档治理 | FX 报价、锁价、审批、费用、汇损益以及 archive/checkpoint/watermark 需要独立 change。 | 下版本隔离领域对象、余额快照、归档账目和只读投影边界，避免交易层重新自动换汇或报表反写账本。 | 需要 DDL 或数据修复时进入 Manual Approval。 | 下版本。 |

本轮之后的 CAD 自动推进顺序固定为：P0-C、P0-CTRL、P0-E、P0-G、P0-H-API、P0-H-PKG 和 P0-ROUTE 已闭合。清分、清算、对账、账本账目归档、余额快照、完整 FX 运营对象和相关运营工作台排入下版本。若后续编码中发现 OpenSpec 规格缺口，先补最小 spec delta 和测试计划，再改代码。

# 九、编码落地顺序

## 9.1 P0：契约和测试骨架

1. 在目标 face 模块固化 Request、Query、DTO、枚举和错误码命名。
2. 补齐 JSON 契约样例和 ContractTests。
3. 建立幂等键、请求摘要、来源事实引用和审计字段的公共约束；来源事实引用先以 `businessScene/businessSn/reference` 为基线，独立事实成熟后再引入 `sourceFactRef`。
4. 已完成有资金变化用例的账本余额断言和业务组合集成测试；后续作为回归保护。
5. P0-C、P0-R、P0-CTRL、P0-E、P0-G、P0-H-API、P0-H-PKG 和 P0-ROUTE 已闭合；后续新增资金变化或路由行为必须继续先补场景测试，再做实现和清理。

退出条件：契约测试能覆盖 DSL 矩阵的 P0 场景，且不需要启动 Spring 即可验证 DSL 和 posting 基础规则。

## 9.1A P0：产品口径回归闸口（已闭合）

1. 已撤回交易 converter 对 `FxService` 的隐式调用，交易请求改为携带业务或外汇域显式 FX 决策事实。
2. 错币种且缺 FX 决策事实时，交易层当前按请求失败处理，不生成 route/entry；挂账或错币种差错对象进入后续 P1/P2 运营模型。
3. `FundsBalanceControlService` 不承接 FX；冻结、解冻、余额调账和额度/预算调整金额必须是账户或账本币种，错币种直接失败，不挂账、不接收 FX 决策快照。
4. 交易请求中使用 `TransactionAmount` 的字段已统一命名为 `transactionAmount`；pay/transfer/authorize/topup/withdraw/settle 纳入交易金额对象，类内部主金额字段保留 `amount`，不使用 `ledgerAmount`；fee/reversal/auth refund/chargeback 暂保留 `Money amount`，direct refund 待退款引用语义收敛后再决定。
5. 余额投影提交后提供 `ledger.balance.changed` 或等价观察口子，业务余额变更日志只能从 `LedgerEntry` 和 `BalanceProjection` 派生。
6. `SettlementPolicySpec` 补非 RT 表达式契约和不支持表达式失败测试，不得把解析失败静默降级为 `RT`。

## 9.1B P0：Route replay 与手续费 CR 闸口（已闭合主链路）

1. 路由回放保留为资金路径能力，但不再暴露独立 `RouteReplayService` 契约；回放实现归入 `RouteResolver`，由 `supports(FundsInstructionSpec)` 命中退款、撤销、授权结算、授权退款、拒付、手续费退回和解冻等带原事实引用的场景。
2. 交易编排器只依赖统一 `RouteResolver`；原快照查询、`ReplayRequest` 构造、`REPLAY_ONCE` 消费校验和回放路径派生由 replay resolver 内聚处理。缺原 `RouteSnapshot` 时必须失败，不允许 fallback 到普通 route resolver 重新选路。
3. 手续费是否收取、按什么规则收取，由业务层或交易应用层决策后显式传入交易请求；route resolver 不再通过 `FundsAccountTransactionFeeProvider` 按账户自动查费率，避免把计费策略侵入资金路径解析。
4. `FeeSpec#feeType` 使用字符串 code 表达费用类型；`DefaultFeeType` 只作为默认 code 集暴露给上层使用，业务可扩展 `SMALL_AMOUNT_FEE`、`AUTH_DECLINE_FEE`、`CROSS_BORDER_FEE` 等费用类型。
5. 直接交易附带手续费和独立手续费交易都必须支持资金账户、信用账户和预算组。信用账户或预算组手续费优先消耗其 `AVAILABLE` 控制桶，并继续受受控负余额、限额、审批或风控策略约束；手续费不得混入本金 route leg、商户本金或通道成本。

当前验证：`mvn -pl tests -am test -Dtest=FundsDirectTransactionInstructionConverterTests,FundsDirectTransactionFeeInstructionConverterTests,FundsAuthorizationInstructionConverterTests,FundsBalanceControlInstructionConverterTests,FundsTransactionCommandServiceImplTests,TransferFundsInstructionRouteResolverTests,AuthorizationFundsInstructionRouteResolverTests,DefaultRouteReplayServiceTests,FundsTransactionBusinessFlowIntegrationTests,FundsBalanceControlBusinessFlowTests,FundsTransactionFeeBusinessFlowTests,FundsTransactionOrchestrationFlowTests,FundsTransactionOrchestrationReplayFlowTests`。

退出条件：`PTDD-RAIL-FX-003`、`PTDD-CTRL-006`、`PTDD-LEDGER-001`、`AT-BASE-038`、`AT-BASE-039`、`RED-014A`、`RED-014B`、`RED-019`、`RED-020` 对应测试或明确测试计划已经落入 7.2/7.6 清单。

## 9.1C P0：控制账户调额 CR 闸口

1. `FundsBalanceControlService#adjust` 是交易层余额控制入口，可承接三类语义：资金账户余额调账、信用账户额度调整、预算组预算调整。
2. 资金账户仍走 `BALANCE_ADJUST`，通过平台 `ADJUSTMENT` 账户形成平衡调账分录；信用账户和预算组走 `LIMIT_ADJUST`，不表达真实现金流，也不引入平台资金账户平衡。
3. 信用账户额度调增同步增加 `AVAILABLE`；额度调减同步减少 `AVAILABLE`。调减导致受控负 `AVAILABLE` 时必须有授信策略、上限、审批、原因和审计。
4. 预算组预算调增同步增加 `AVAILABLE`；预算调减同步减少 `AVAILABLE`。调减导致受控负 `AVAILABLE` 时必须有预算周期、治理策略、审批、原因、上限、账龄和报表标记；新授权必须重新经过预算策略。
5. `LIMIT` 只允许在 `BALANCE_CONTROL / LIMIT_ADJUST` 受控调额路径中表达额度或预算总量调整；普通支付、授权结算、退款、争议拒付、手续费或直接交易不得把 `LIMIT` 当 source/target。
6. 本轮不新增公共 `ControlAdjustmentSpec`，也不新增账务 `CONSUMED`；已消费继续由授权结算、退款、争议拒付、调额和预算周期规则进入产品报表或交易视图投影。

任务计划按 OpenSpec / Superpowers / Harness 重新整理如下：

| 顺序 | 任务 | OpenSpec 输入 | Superpowers 执行 | Harness 验证 |
| --- | --- | --- | --- | --- |
| 1 | 补齐控制账户调额文档一致性 | `wallets`、`transaction-layer` 规格与 PRD/ADR/DSL 的 `LIMIT_ADJUST` 口径一致。 | 只改文档，不改代码；先确认没有 `LIMIT` 普通迁移和 `LIMIT_ADJUST` 调额互相冲突。 | `rg "LIMIT_ADJUST\|预算调减\|FundsBalanceControlService#adjust"` 人工复审。 |
| 2 | 补信用账户调额服务门面测试 | `FundsBalanceControlService#adjust` 承接信用额度调增、调减和受控负数。 | 先写调增、调减、缺审批失败、超过策略失败测试，再做最小实现。 | `just test-one FundsTransactionCommandServiceImplTests tests`。 |
| 3 | 补预算组调额服务门面测试 | 预算组调额必须保留预算周期、治理策略、审批、原因、上限、账龄和报表标记。 | 先写预算调增、调减、受控负数、缺预算治理上下文失败测试。 | `just test-one FundsTransactionCommandServiceImplTests tests`。 |
| 4 | 补 `LIMIT` 红线测试 | `LIMIT` 仅允许在 `BALANCE_CONTROL / LIMIT_ADJUST` 受控路径出现。 | 先写普通交易、授权结算、退款、手续费不得把 `LIMIT` 当 source/target 的失败用例。 | `just test-one BalanceControlFundsInstructionRouteResolverTests tests` 和相关 route replay 测试。 |
| 5 | 最小实现收口 | 若测试暴露规格缺口，先补 OpenSpec spec delta，再改代码。 | 只在 request 校验、converter、resolver 或 context 内补齐，不新增公共 `ControlAdjustmentSpec`，不新增账务 `CONSUMED`。 | `just compile`、相关测试；`just pmd` 若依赖解析可用则执行，不可用则按环境问题记录。 |

退出条件：`PTDD-CTRL-003`、`AT-BASE-040`、`AT-BASE-041` 对应测试落地；PRD、ADR、DSL 和 API 计划不再出现“`LIMIT` 完全不可作为任何 route 节点”与“`LIMIT_ADJUST` 调额 route”互相冲突的表述。

## 9.2 P0：wind-funds 与 Ledger Posting

1. 已补 `SourceFactBoundaryContractTests`：生产契约和 transaction-layer DSL 样例不恢复 `sourceObjectType/sourceObjectSn`，也不提前暴露 `sourceFactRef`；当前继续以 `businessScene/businessSn/reference` 为基线，冻结、清结算、争议、对账差错等独立事实成熟后再设计 `sourceFactRef`。
2. 修正 entry 摘要字段，排除持久化流水和易变字段。
3. 已补 route leg 到 posting plan 的稳定引用，并通过账本入账、投影、查询和摘要契约测试保护持久化投影与摘要字段。
4. 已补平衡校验、账目允许、normal balance 推导、幂等不重复投影、缺账本失败、缺余额桶失败和展示查询未初始化语义测试。

退出条件：直接交易、授权占用、冻结、清结算类 DSL 都能生成稳定、平衡、可追溯的账本交易。

## 9.3 P0：Wallets 账户和余额控制

1. 补齐资金账户、商户资金账户、信用账户、预算组的 profile 和账目规则。
2. 收敛平台账户角色命名，明确现金映射、预收待付、清算过渡、结算应付、费用归集、调整挂账。
3. 冻结和解冻以 `FrozenOrder` 为来源事实，不创建 `FundsTransaction`。
4. 受控负 `AVAILABLE` 增加策略、上限、审批、账龄和审计字段。

退出条件：各主体的余额桶变化和负余额策略均有测试保护，冻结不再污染交易主表。

## 9.4 P0：Transaction Layer 服务能力

1. 交易门面主体已收敛到 transaction application 层，后续重点保持 wallet 边界测试不退化。
2. 调整来源事实生命周期时使用明确的 `sourceFactRef` 或等价值对象，不恢复 `sourceObjectType/sourceObjectSn` 两个散字段。
3. 授权拒绝和争议拒付继续保持字段、原因、事件和测试分离。
4. 明确主事务失败后失败事实的保留策略，避免同事务 `markFailed` 被回滚。

退出条件：直接交易、逆向交易、授权交易、余额控制都有应用服务测试，核心失败路径可诊断、可回滚、可审计。

## 9.5 P1：清结算、对账和报表

1. 建立清算候选、清算批次、结算单、出款单、对账批次和差错单模型。
2. 清算确认、结算锁定、出款成功、出款失败回退都通过来源事实生成账本交易。
3. 差错阻断、审批放行、调账和核销有完整状态机和审计。
4. 报表只读明细和快照，不反写账本事实。

退出条件：清结算与对账能解释资金流、账户流、账务流和差错处理路径。

## 9.6 P1：归档、重放和指标治理

1. 建立 `BalanceCheckpoint`、`BalanceProjectionWatermark`、`ArchiveManifest`。
2. 手动归档必须校验 cutoff 不晚于水位、不晚于热保留边界，且已有已验证检查点。
3. 余额重建使用 cold checkpoint + hot entries；交易视图重放限定时间窗口和主体范围。
4. 运营指标使用独立 `MetricWatermark`，不能复用归档时间边界。

退出条件：余额投影在冷热分层、归档、重建和指标统计下无 gap、无 overlap。

# 十、进入编码前确认项

| 事项 | 建议默认值 | 原因 |
| --- | --- | --- |
| 交易门面归属 | 放入 `transaction/transaction-face`，wallet 对外交易服务逐步收敛或迁移。 | 符合“交易层对业务侧提供统一能力”的目标。 |
| 第一批代码范围 | P0-C、P0-R、P0-CTRL、P0-E、P0-G、P0-H-API、P0-H-PKG 和 P0-ROUTE 已闭合。 | 避免把清结算、对账、归档、余额快照和完整 FX 运营对象混入当前交易/钱包/ledger/route 闭环。 |
| 清结算代码范围 | 排入下版本，当前只保留 PRD、DSL、OpenSpec 和系分追溯，不做批处理实现。 | 清结算依赖账本、交易、对账和运营策略，过早实现容易反复返工。 |
| DDL 和迁移 | 单独出数据库变更方案和回滚方案后再执行。 | 涉及资金事实、余额投影、归档和分表，必须有评审和验证。 |
| Harness 接入 | 先用本地命令和测试矩阵跑通，再创建真实 pipeline。 | 避免在规范未固化前引入 CI 凭据和远程环境变量。 |

# 十一、当前结论

从设计完整性看，产品目标、DSL、OpenSpec、核心系分、API 契约、测试矩阵和 Harness 本地门禁已经形成编码输入闭环；但执行顺序需要以 8.1 的再审查结果为准。

进入代码前建议先确认 `P0 编码任务拆分.md` 中的当前工作包状态。P0-A 测试保护、P0-D 交易门面、P0-C 账本主链路首轮、P0-F helper/route 主链路、P0-R 产品口径回归、P0-CTRL 控制账户调额、P0-E 钱包账户与余额控制、P0-G 命名治理、P0-H-API 三类服务测试矩阵、P0-H-PKG 测试包名对齐和 P0-ROUTE 路由层 CR 已经完成主体落地；后续进入下版本前，需要重新评估清结算、对账、归档和余额快照任务的 OpenSpec change、DDL 风险和 Harness manual approval gate。
