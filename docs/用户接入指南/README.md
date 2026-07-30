# wind-funds 用户接入指南

## 1. 定位

本文面向要接入 `ledger`、`wallet`、`transaction` 三个主链模块，以及已声明对账、清分、内部清算、结算、出款和 W3 最小追偿责任/结果引用基础设施切片的业务系统和内部研发。它只说明当前代码中已有公共契约和本地测试证明过的接入方式，不承诺 VCC、全球账户、收单、具体通道执行、追偿策略与资金执行、归档或治理的生产接入。

接入三层主链时只依赖 `core`、`ledger-face`、`wallet-face`、`transaction-face`；使用已声明的清结算与对账切片时可额外依赖 `reconciliation-face`。不要依赖 `*-impl`、Entity、Mapper、交易层清算/结算/出款资金原语或测试工具类。

## 2. 模块边界

| 模块 | 定位 | 接入方能做 | 接入方不能做 |
| --- | --- | --- | --- |
| `ledger` | 账本事实和余额投影。 | 查询账本、账本交易、分录和余额投影；在底层服务级测试中验证过账事实。 | 业务方不直接拼分录、不直接改余额、不把投影当事实源。 |
| `wallet` | 账户、支付工具、资金责任、支出控制和准入快照。 | 建模资金账户/信用账户，解析账户能力、支付工具能力、资金责任和 Spend Rule 决策。 | 不创建交易事实，不写 route、posting、ledger entry 或余额投影。 |
| `transaction` | 标准资金交易、授权交易、余额控制、让利出资记账和外部确认入金消费。 | 提交已经成立的资金事实，由交易层编排路由、账务和账本影响。 | 不接收页面动作、审批中、通道处理中或外部非终态。 |
| `reconciliation` | 对账、清分、内部清算、结算和出款资金事实。 | 通过 `reconciliation-face` 管理批次、差错、Gate、SettlementOrder 和 PayoutOrder。 | 不直接调用交易层清算/结算/出款资金原语，不把 Gate 或预检结果当成提交授权，不在 wind-funds 内执行通道协议。 |

依赖方向是 `transaction -> wallet -> ledger`。横向治理或对账能力只能通过 face/core 只读消费事实，不反写主链事实。

## 3. 接入顺序

1. 先确认业务事实已经成立，并能回答账务三问：谁的钱、多少钱、怎么变的；同时准备幂等键、来源引用和操作者。
2. 先走 `wallet` 建模或准入：账户能力、支付工具能力、资金责任、Spend Rule 决策。
3. 再走 `transaction` 产生资金事实：直接交易、授权交易、余额控制、让利出资、外部确认入金。
4. 最后用 `ledger` 和投影查询验收：账本交易、分录、余额桶、幂等和失败无副作用。

如果第 1 步说不清，不进入 wind-funds。

## 4. 能力矩阵

| 能力 | 公共入口 | 已验证场景 | 主要测试 |
| --- | --- | --- | --- |
| 账本基础 | `LedgerService`、`LedgerTransactionPostingService`、`LedgerTransactionService` | 建账；通过 posting gateway 过账；通过 query service 按 SN 查询交易/分录；查询余额投影。 | `LedgerServiceImplTests`、`DefaultLedgerTransactionPostingServiceImplTests`、`LedgerTransactionServiceFactQueryTests`、`DefaultLedgerPostingAssemblerTests`、`LedgerBalanceProjectionServiceImplTests` |
| 账户和余额查询 | `FundsAccountCapabilityApplicationService`、`FundsSubjectBalanceQueryService` | 账户能力准入、余额查询、账本 profile 初始化。 | `FundsAccountCapabilityApplicationServiceTests`、`FundsSubjectBalanceQueryServiceImplTests`、`LedgerProfileContractTests`、`ControlAccountLedgerInitializationTests` |
| 支付工具 | `PaymentInstrumentCapabilityApplicationService`、`PaymentInstrumentPreTransactionSnapshotApplicationService` | 支付工具能力、绑定快照、交易前快照。 | `PaymentInstrumentCapabilityApplicationServiceTests`、`PaymentInstrumentPreTransactionSnapshotApplicationServiceTests`、`PaymentInstrumentServiceImplTests` |
| 资金责任 | `FundingResponsibilityResolutionApplicationService` | 按关系解析当前资金责任主体。 | `FundingResponsibilityResolutionApplicationServiceTests`、`SpendSubjectFundingRelationServiceImplTests` |
| 支出控制 | `SpendControlAdmissionApplicationService`、`BudgetControlLimitAdjustmentApplicationService`、`SpendControlTransactionConsumptionApplicationService` | Spend Rule 准入、预算控制调整、交易消费记录。 | `SpendControlAdmissionApplicationServiceTests`、`BudgetControlLimitAdjustmentApplicationServiceTests`、`SpendControlTransactionConsumptionApplicationServiceTests`、`SpendRuleDefinitionServiceFlowTests` |
| 直接交易 | `FundsDirectTransactionService` | 充值、转账、付款、退款、提现、手续费、退费。 | `FundsDirectTransactionFlowTests`、`FundsTransactionFeeFlowTests`、`FundsTransferPayWithdrawChainFlowTests` |
| 授权交易 | `FundsAuthorizationTransactionService` | 授权、撤销、完成、完成后退款。 | `FundsAuthorizationTransactionFlowTests` |
| 余额控制 | `FundsBalanceControlService` | 冻结、解冻、受控调整；冻结不表达扣款。 | `FundsBalanceControlFailureFlowTests`、`FundsFrozenOrderServiceImplTests`、`FundsWithdrawalSuccessFlowTests`、`FundsWithdrawalAfterPartialUnfreezeFlowTests` |
| 让利出资记账 | `FundsBenefitContributionTransactionService` | 平台/商户/合作方已决策让利出资入账和按原交易冲回。 | `FundsBenefitContributionTransactionServiceContractTests`、`FundsBenefitContributionTransactionServiceFlowTests` |
| 外部确认入金 | `ExternalFundsEventApplicationService` | 已确认外部入金消费为标准充值，目标必须是资金账户。 | `ExternalFundsEventApplicationServiceTests` |
| 清结算、出款与追偿引用事实 | `ClearingBatchApplicationService`、`SettlementOrderApplicationService`、`PayoutOrderApplicationService`、`RecoveryOrderApplicationService` | 内部清算 `CLEARING -> AVAILABLE`；外部端点结算锁定 `AVAILABLE -> SETTLEMENT`；出款提交意图与归一回单；宿主已确认追偿责任和已完成 `RECOVERY` 资金交易引用。 | `ClearingBatchApplicationServiceTests`、`SettlementOrderApplicationServiceTests`、`PayoutOrderApplicationServiceTests`、`RecoveryOrderApplicationServiceTests`、对应公共契约与 DDL 测试 |

### 4.1 当前接入成熟度基线

本表只表示仓库当前已实现并由本地测试证明的能力成熟度，不等同于生产准出。主链缺少统一版本化生产迁移、运行环境演练、监控告警和 Runbook 证据时，只能用于开发或受控联调。新增公共契约、扩展业务能力或进入 P2 场景前，必须重新确认写入范围、验证命令和不接入范围。

| 能力域 | 准出结论 | 当前可交付内容 | 生产接入前仍需确认 |
| --- | --- | --- | --- |
| `ledger` 账本主链 | 开发 / 受控联调可接入。 | 建账、过账、账本交易、分录查询和余额投影。 | 业务场景必须提供账务主体、账目、币种、幂等和 posting 平衡验收。 |
| `wallet` 账户和支付工具 | 开发 / 受控联调可接入。 | 资金账户、信用账户、账户能力、支付工具能力、绑定快照和交易前快照。 | 支付工具只做引用和准入快照，不作为账本主体；敏感数据不得进入 request、日志和投影。 |
| `wallet` 资金责任 | 开发 / 受控联调可接入。 | 按支出主体解析资金账户或信用账户责任主体。 | 多资金责任、错币种、停用账户和冲突优先级必须在准入前失败。 |
| `wallet` Spend Rule / 预算控制 | 受控试点可用。 | 单条规则只读评估、最终决策固化、周期额度流水、交易消费、退款补偿、可信控制释放和控制投影查询。 | 多规则组合裁决、强一致授权拦截、rolling amount、cooldown、外部协同授权和生产调度由上游或专项承接。 |
| `transaction` 直接交易 | 开发 / 受控联调可接入。 | 充值、转账、付款、退款、提现、手续费和退费。 | 外部 pending、审批中或通道处理中不得进入交易事实。 |
| `transaction` 授权交易 | 开发 / 受控联调可接入。 | 授权、撤销、完成和完成后本金退款，后继动作基于原路径。 | `refund` 只承接不超过已完成金额的本金；商家补偿、退货运费、FX credit 和 VCC 组件编排不属于该入口。授权拒绝不得生成 route、posting、ledger entry；清算、争议和强制完成需按专项边界确认。 |
| `transaction` 余额控制 | 开发 / 受控联调可接入。 | 冻结、解冻、受控余额调整和失败无副作用。 | 冻结只表达同主体 `AVAILABLE <-> FROZEN`，不表达扣款、消费或跨主体转移。 |
| 让利出资记账 | 开发 / 受控联调可接入。 | 上游已决策的平台、商户或合作方让利出资入账，以及按原交易冲回。 | 不计算券、不维护券生命周期、不保存营销归因；非入账权益不进本服务。 |
| 外部确认入金 | 开发 / 受控联调可接入。 | `confirmed credit -> funding account` 转为标准充值。 | accepted、submitted、processing、message sent、VA 未匹配、错币种和外部账户入账均不得进入本入口。 |
| FX 来源价格与金额换算 | 开发 / 受控联调可接入。 | `FxRateProvider` 是由接入方实现并注入的汇率来源端口，提供含 `snapshotId`、`observedAt` 和 `MID/BID/ASK` 的来源快照；跨币种换算可显式传入最终 `FxAppliedRate`，也可指定 `FxPriceType` 由换算服务查询来源快照并选价。 | wind-funds 不提供默认汇率来源实现；`FxAppliedRate` 与 `FxPriceType` 互斥且不默认选价。客户加点、quote、费用、有效期、锁汇、历史行情、换汇执行、合规和资金入账仍由上层业务或专项承接。 |
| 对账 / 清分 / 内部清算 / 结算 / 出款事实 | `GO`（wind-funds 已声明的基础设施切片）；不等同于宿主部署上线准出。 | 对账批次与结果、差错与 Gate、清分/清算批次、`CLEARING -> AVAILABLE`、`AVAILABLE -> SETTLEMENT`，以及基于锁定外部端点结算单的 PayoutOrder 创建、提交意图、归一回单、成功关闭和失败回退。W2 不启用 `IN_TRANSIT`，不支持失败后重试。 | 来源验签与归一、业务匹配执行、出款账户/端点/通道/合规准入、通道调用与状态归一、任务调度、宿主 IAM/审计、监控、Runbook、目标数据库与发布准出由接入方负责。 |
| 追偿责任 / 结果引用 | `CONDITIONAL GO`（W3 本地切片）；不作为生产追偿执行承诺。 | `RecoveryOrderApplicationService` 登记已确认责任和已完成 `RECOVERY` 资金交易引用，支持部分/全部追回累计、来源与结果幂等、超额和重复认领阻断；登记本身不动账。 | 目标 MySQL 迁移与 RR 并发仍需专用环境证据；责任判断、退款/拒付/催收、准备金、抵扣、负余额、人工收款、核销和资金执行由宿主或后续专项承接。 |
| 归档 / 治理 | 不作为当前生产接入承诺。 | 现有目标设计和局部治理能力只能作为后续专项输入。 | 需要按新切片补公共对象、状态机、DDL/H2、服务级测试和 owner 确认。 |
| VCC / 全球账户 / ACH / 收单 / FX quote 与执行 / 退汇 | P2 边界设计，不进入默认实现。 | 可复用主链事实、外部引用边界和显式 FX 金额计算。 | 业务生命周期、外部规则、通道协议、合规、敏感数据和专项回归未确认前，不得声明生产可用。 |

交易投影当前在资金事务提交后通过 `afterCommit` 尽力发布；发布失败只记录告警，事实仍可由治理重放重建，但仓库尚无持久待办、自动补偿和积压监控。因此投影不能作为同步资金结果，也不能作为生产完整性证据；宿主进入生产前必须提供可持久发现、幂等重放和告警闭环。

## 5. 业务事实说明卡

每个接入场景先填这 10 项，填不满就不要拆研发任务。前三类信息必须先回答账务三问。

| 项 | 必填内容 |
| --- | --- |
| 业务动作 | topup、transfer、pay、refund、withdraw、fee、authorize、authorization reversal、complete、authorization refund、freeze、unfreeze、adjust、benefit settle/refund、external confirmed credit。 |
| 资金主体 | 回答“谁的钱”：成本承担方、资金流出方、资金流入方、授权主体或冻结主体。 |
| 账户类型 | FundingAccount、CreditAccount、平台账户角色解析结果；SpendControlScope、Spend Rule、PaymentInstrument 只能是控制或引用。 |
| 金额币种 | 回答“多少钱”：金额必须为正，币种和精度必须能和账户、账本 profile 对齐。 |
| 幂等 | 业务流水、请求摘要、重复提交和同键不同摘要处理。 |
| 来源引用 | 回答“怎么变的”：订单、提现单、授权单、外部事件、原交易 SN 或原冻结单 SN。 |
| 操作者 | 使用 `WindOperator`，不要塞进 request 字段。 |
| 准入快照 | 账户能力、支付工具能力、资金责任、Spend Rule 决策或原 route snapshot。 |
| 账务验收 | 交易状态、route、posting、ledger transaction、ledger entry、余额桶变化。 |
| 失败验收 | 准入失败、幂等冲突、余额不足、路由失败、外部非终态不得留下错误资金事实。 |

## 6. 常用接入路径

### 6.1 用户或商户入金

适用条件：外部资金已经确认到账，目标是内部资金账户。

推荐入口：

1. `FundsAccountCapabilityApplicationService.resolveFundsAccountCapability`
2. `FundsDirectTransactionService.topup`
3. 查询 `LedgerTransactionService` 或余额投影验收

如果来源是外部事件，可用 `ExternalFundsEventApplicationService.consume`，但当前只支持 confirmed credit -> funding account。

验证锚点：`FundsDirectTransactionFlowTests`、`ExternalFundsEventApplicationServiceTests`。

### 6.2 内部转账、付款、退款和提现

适用条件：资金事实已经成立，出入方都是可解析的内部账务主体，或者提现目标只是外部引用。

推荐入口：

| 场景 | 入口 |
| --- | --- |
| 内部转账 | `FundsDirectTransactionService.transfer` |
| 付款 | `FundsDirectTransactionService.pay` |
| 退款 | `FundsDirectTransactionService.refund` |
| 提现 | `FundsDirectTransactionService.withdraw` |
| 手续费 | `FundsDirectTransactionService.fee`、`refundFee` |

验证锚点：`FundsDirectTransactionFlowTests`、`FundsWithdrawalSuccessFlowTests`、`FundsWithdrawalRejectionFlowTests`、`FundsTransactionFeeFlowTests`。

### 6.3 授权交易

适用条件：先占用额度或余额，再按后续事件撤销、完成或退款。

推荐入口：

| 阶段 | 入口 | 说明 |
| --- | --- | --- |
| 授权 | `authorize` | 授权拒绝不得生成 route、posting 或 ledger entry。 |
| 撤销 | `reversal` | 释放原授权占用。 |
| 完成 | `complete` | 基于原授权路径完成，不按当前绑定重新选路。 |
| 完成后退款 | `refund` | 引用原完成事实。 |

验证锚点：`FundsAuthorizationTransactionFlowTests`。

#### 6.3.1 本金退款与额外 credit

当前公共契约没有“超额退款”入口。`refund` 只接收原清算币种本金，累计不得超过原交易 `completedAmount`；`totalCreditAmount`、商家补偿、退货运费和 FX credit 不得作为该方法的退款金额，也不得写入 `refundedAmount`。

VCC 或其他业务接入方负责外部事件验真、金额拆分、责任方、出资账户、到账账户、账目、币种、汇率快照、审批、上限、组件顺序、部分成功恢复和业务主状态。`wind-funds` 只执行已经成立的单个资金事实：本金使用 `refund`；额外 credit 只有在上述事实完整且对应 P2 专项完成生产确认后，才能选择适用的 canonical 直接交易入口独立提交。每个组件使用独立幂等键并携带同一 `refundRef`，但不共享跨调用事务。

禁止使用 `FundsBalanceControlService.adjust` 绕过该边界，也禁止把总 credit 拆成多次 `refund` 突破本金累计上限。清算本金 100、补偿 10 时，向 `refund` 提交 110 必须失败；业务侧应提交本金 100，并在额外 credit 专项准出后另行提交已确认的 10。

### 6.4 冻结、解冻和调整

适用条件：表达同一主体余额控制或有审批凭证的余额调整。

推荐入口：

| 场景 | 入口 | 禁止项 |
| --- | --- | --- |
| 冻结 | `FundsBalanceControlService.freeze` | 不能表达消费或跨主体转移。 |
| 解冻 | `FundsBalanceControlService.unfreeze` | 不能超过原冻结事实。 |
| 调整 | `FundsBalanceControlService.adjust` | 不能替代付款、退款或提现；同主体余额修正只能在差错单、审批、凭证、审计和重新对账闭环内承接，跨主体补偿不得通过本入口直接处理。 |

验证锚点：`FundsBalanceControlFailureFlowTests`、`FundsFrozenOrderServiceImplTests`。

### 6.5 支付工具和资金责任准入

适用条件：业务入口先拿到卡、外部账户、VA、PSP token 或支付工具引用，需要解析可用性和最终资金责任。

推荐入口：

1. `PaymentInstrumentCapabilityApplicationService.resolvePaymentInstrumentCapability`
2. `PaymentInstrumentPreTransactionSnapshotApplicationService`
3. `FundingResponsibilityResolutionApplicationService.resolveFundingResponsibility`
4. 交易层入口

支付工具不是账户。它只提供引用、绑定和路由输入。

验证锚点：`PaymentInstrumentCapabilityApplicationServiceTests`、`PaymentInstrumentPreTransactionSnapshotApplicationServiceTests`、`FundingResponsibilityResolutionApplicationServiceTests`。

### 6.6 Spend Rule 和预算控制

适用条件：需要在交易前记录并验真支出控制决策，或记录交易消费对控制范围的影响。

当前接入口径：可信规则或业务决策方先完成规则判断，并通过 `SpendRuleDecisionRecordService.recordDecision` 固化 `PASSED` / `REJECTED` 决策；`wallet` 准入自行解析当前有效 Spend Rule 挂载，再按 `decisionSn` 回读和验真决策记录。普通交易接入方不得把 `recordDecision` 当作授权入口，也不能只传裸 `PASSED + sha256`。如只需要单条已发布规则的轻量评估，可先调用 `SpendRuleEvaluationApplicationService.evaluate` 覆盖单笔金额、周期金额可用额度、周期次数、滚动窗口次数、MCC 黑白名单、商户国家黑白名单、卡数据输入能力黑白名单、卡交易处理类型黑白名单、商户标识黑白名单、PAN 录入方式黑白名单、POS 类别黑白名单、CVV 必填、AVS 邮编校验结果、币种黑白名单和本地授权时间窗口判断，再由可信决策方把最终结果写为决策记录。每个被 `evaluate` 的 `ruleSpec` 只允许一个可执行控制项；该 evaluator 只提供只读候选评估，不提供并发强一致授权拦截。

准入只从可信上下文解析 `PAYMENT_INSTRUMENT`、已解析的 `FUNDING_ACCOUNT` / `CREDIT_ACCOUNT`、`BUSINESS_SCENE` 和请求中的 `controlScopeId` 对应挂载。没有适用且没有无法解析的有效挂载时返回显式 `NO_APPLICABLE_RULE`，不创建伪规则、binding、摘要或决策记录；只有一个适用挂载时必须提供可回读的 `decisionSn`，并核对 binding、支付工具、动作、金额、币种和业务流水；多个适用挂载时，当前单决策证据契约无法完整表达，默认拒绝。存在有效 `SPEND_CONTROL_SCOPE` 挂载但请求未携带匹配的 `controlScopeId`，或租户下存在当前无法解析的有效 `ACCOUNT_HIERARCHY` 挂载时，wallet 明确 fail-closed，不能降级为 `NO_APPLICABLE_RULE`。`controlScopeId` 目前仍由请求提供，生产 facade 必须从可信业务关系补齐并禁止调用方伪造或省略；`ACCOUNT_HIERARCHY` 参与生产准入前必须先补充可信层级标识来源并评审公共契约。

已成立授权按 `tenantId + businessScene + businessSn` 回读原交易，并以原交易上下文核对金额、币种、支付工具、授权结果和 Spend Rule 决策证据。完全相同的请求即使当前 binding 已暂停也返回原交易号，不重新生成资金或账务事实；关键参数或决策引用变化时拒绝。该规则只用于已经成立的授权事实，不允许失败中的请求绕过当前准入。

金额和事件口径：`EvaluateSpendRuleRequest.amount` 是调用方已归一后的本次评估金额。卡授权接入方如果区分 requested amount 和 authorized amount，必须先在上游确定进入支出控制的金额口径，再传入 evaluator；wallet 不从外部原始网络字段、退款、撤销或 Highnote 式延迟结果中推导累计授权金额。周期额度、周期次数和滚动窗口次数只读取本系统已有 `SpendControlMovement` 与预算控制投影。交易成功通过交易消费入口记录 `CONSUMED`，退款成功记录 `REFUND_COMPENSATED`；交易失败或拒绝依赖同一资金事务回滚预留，不写释放补偿；到期或超时不写控制流水；可信撤销、清算剩余释放或差错补事实由上层显式调用控制流水服务记录 `RELEASED`。

Velocity 控制口径：Highnote 的 `PER_TRANSACTION` 在本项目只对应本次评估；`DAILY` / `WEEKLY` / `MONTHLY` / `QUARTERLY` / `YEARLY` 由接入方生成稳定 `periodId` 后查询控制投影；滚动次数只按 `ROLLING + windowSizeMinutes` 做只读候选评估；`NINETY_DAYS`、`COOLDOWN_MINUTE`、`COOLDOWN_HOUR`、rolling amount 和 Highnote 的 velocity control 数量上限当前不是 wallet 硬约束。Highnote velocity balance 查询只对应 `BudgetControlProjectionDTO` 的控制口径，不是资金账户余额或账本余额。

不承接规则类型：Highnote 文档中的 maximum amount variance on credit limit、maximum percent variance on credit limit、conditional rule、authorization hold configuration、deposit amount、deposit count、deposit processing network 和 street address 当前不进入 wallet Spend Rule evaluator。信用额度浮动类规则由授信 / 风控专项承接；authorization hold configuration 不改变本项目授权占用和释放语义；外部确认入金走 `transaction` 的外部资金事件入口；地址类控制只允许传入 AVS / 邮编校验结果事实，不接收或保存街道地址原文。

外部风控或协同授权的 approve / decline 必须先由可信适配方写入可回读的决策记录，再以 `decisionSn` 进入 `SpendControlAdmissionApplicationService.resolveSpendControlAdmission`。`REJECTED` 会在交易内核前停止；`PASSED` 只有在 binding 和当前交易上下文验真通过后才能继续支付工具绑定、账户能力和资金责任校验，不能被理解为资金可用、授权成功或已经完成交易。

多规则裁决仍由上游负责合成并保留 `evaluatedRules`、`decisionPolicy`、`finalDecision` 等明细，但当前 wallet 的单决策记录契约不能证明多个同时适用 binding 已被完整评估，因此不会仅凭上游最终 `decisionSn` 选取其中一条 binding 放行。生产接入应确保当前准入上下文只有一个适用 binding；需要多规则同时生效时，先完成多规则证据公共契约和持久化设计。

规则挂载范围按本系统稳定对象表达，不直接暴露外部产品名：Highnote payment card 映射 `SpendRuleScopeType.PAYMENT_INSTRUMENT`，`scopeId` 使用支付工具流水；financial account 映射 `FUNDING_ACCOUNT` 或 `CREDIT_ACCOUNT`，`scopeId` 使用资金账户或信用账户流水；authorized user、cardholder、员工或账户层级映射 `ACCOUNT_HIERARCHY`，`scopeId` 使用企业员工、持卡人或账户层级的系统内稳定引用；card product 可按产品侧稳定场景映射 `BUSINESS_SCENE`。这些范围只用于规则挂载、查询和解释，不是资金主体、资金责任主体或账本主体。

商户标识场景示例：企业采购卡只允许合作商户 `MID-CONTRACT-001`，接入方发布 `ruleSpec.limitSpec.merchantIdControl.allowedMerchantIds=["MID-CONTRACT-001"]`，授权前调用 `evaluate` 时传入 `merchantId`。命中白名单只得到 `PASSED` 评估结论；仍需继续走支付工具、账户能力和资金责任准入。若发布 `deniedMerchantIds=["MID-RISK-001"]` 且请求命中该 MID，则返回 `REJECTED` 且不产生交易或账本事实。

PAN 录入方式场景示例：企业卡禁止手工录入卡信息，接入方发布 `ruleSpec.limitSpec.panEntryModeControl.deniedPanEntryModes=["MANUAL"]`，授权前调用 `evaluate` 时传入 `panEntryMode=manual`。命中黑名单返回 `REJECTED`，且不保存 PAN 原文、不产生交易或账本事实。若只允许非接触式交易，可发布 `allowedPanEntryModes=["CONTACTLESS"]`，命中后仅得到 `PASSED` 评估结论，后续仍需继续准入和交易链路。

POS 类别场景示例：企业差旅卡禁止 ATM 终端交易，接入方发布 `ruleSpec.limitSpec.pointOfServiceCategoryControl.deniedPointOfServiceCategories=["AUTOMATED_TELLER_MACHINE"]`，授权前调用 `evaluate` 时传入 `pointOfServiceCategory=automated_teller_machine`。命中黑名单返回 `REJECTED`，不产生交易或账本事实。车队卡只允许自助加油终端时，可发布 `allowedPointOfServiceCategories=["AUTOMATED_FUEL_DISPENSER"]`，命中后仅得到 `PASSED` 评估结论。

卡交易处理类型场景示例：企业卡禁止 PIN 变更类卡事件，接入方发布 `ruleSpec.limitSpec.cardTransactionProcessingTypeControl.deniedCardTransactionProcessingTypes=["PIN_CHANGE"]`，授权或卡事件处理前调用 `evaluate` 时传入 `cardTransactionProcessingType=pin_change`。命中黑名单返回 `REJECTED`，拒绝原因为卡交易处理类型不允许，且不产生交易、控制流水或账本事实。若只允许取现类处理类型，可发布 `allowedCardTransactionProcessingTypes=["CASH"]`，请求 `cardTransactionProcessingType=cash` 时仅得到 `PASSED` 评估结论。

CVV 必填场景示例：电商卡要求交易时提供 CVV，接入方发布 `ruleSpec.limitSpec.cvvControl.required=true`，授权前调用 `evaluate` 时只传入 `cvvProvided=false` 或 `true`。未提供时返回 `REJECTED`，拒绝原因为未提供 CVV；已提供时返回 `PASSED`。本接口不得传入 CVV 原文，也不会保存 CVV、PAN 或其他卡敏感值。

AVS 邮编校验结果场景示例：电商卡要求账单邮编校验匹配，接入方发布 `ruleSpec.limitSpec.postalCodeVerificationControl.allowedVerificationResults=["MATCH"]`，授权前调用 `evaluate` 时只传入 `postalCodeVerificationResult=match` 或 `no_match`。`NO_MATCH` 命中拒绝时返回 `REJECTED`，拒绝原因为邮编校验结果不允许；`MATCH` 命中白名单时仅得到 `PASSED` 评估结论。接口只接收 AVS 校验结果，不接收或保存邮编、街道地址、PAN 或 CVV 原文。

币种控制场景示例：企业卡只允许美元授权，接入方发布 `ruleSpec.limitSpec.currencyControl.allowedCurrencies=["USD"]`，授权前调用 `evaluate` 时传入 `currency=USD` 或其他 ISO 币种。命中允许币种时仅得到 `PASSED` 评估结论；若发布 `deniedCurrencies=["EUR"]` 且请求币种为 `EUR`，返回 `REJECTED`，拒绝原因为币种不允许，且不产生交易、控制流水或账本事实。本接口只判断请求币种，不做外汇换算、币种精度换算或账户余额校验。

本地授权时间窗口场景示例：企业卡只允许工作时间授权，接入方发布 `ruleSpec.limitSpec.timeWindowControl.allowedWindows=[{"startTime":"09:00","endTime":"18:00"}]`，授权前按业务或规则时区把授权时间归一化后传入 `authorizationTime`。窗口按起点包含、终点不包含解释，`09:00` 返回 `PASSED`，`20:30` 返回 `REJECTED`，拒绝原因为时间窗口不允许。接口不做时区换算、节假日判断或生产调度刷新。

滚动窗口次数场景示例：企业卡限制最近 15 分钟最多 3 次授权，接入方发布 `ruleSpec.counterSpec.windowMode=ROLLING`、`ruleSpec.counterSpec.windowSizeMinutes=15`、`ruleSpec.limitSpec.countLimit.maxCount=3`，授权前传入同一 `controlScopeId`、目标账户、币种和已归一化的 `authorizationTime`。evaluator 会只读既有 `SpendControlMovement` 中同规则、同控制范围、同账户、同币种且创建时间落在窗口内的 RESERVED / CONSUMED 流水，并按原始占用流水去重计数。窗口内已有 3 笔时返回 `REJECTED`，拒绝原因为滚动窗口次数超限；历史流水都已滑出窗口时返回 `PASSED`。该能力不生成控制流水或资金事实，也不提供并发强一致频控拦截；若授权链路要求强一致阻断，必须由交易 / 准入编排在同一事务或锁定边界内完成评估、准入和 RESERVED 写入。该能力不支持 rolling amount、cooldown 或生产调度刷新。

生产试点接入前，接入方还必须补齐下列证据；否则只能作为内部受控能力使用，不能按生产 Spend Controls 平台对外承诺。

| 检查项 | 接入方需要准备 |
| --- | --- |
| 规则变更审计 | 规则创建、版本发布、挂载变更、停用 / 恢复、额度调整的操作者、原因、审批或审计引用、traceId、变更前后摘要和影响 scope。 |
| 最终决策证据 | 有适用 binding 时，可信决策方已持久化最终 `decisionSn`、`decisionResult`、`decisionDigest`、拒绝原因、上游外部决策引用和业务流水，且 wallet 可按引用回读验真；无适用规则时保留 `NO_APPLICABLE_RULE` 准入快照。 |
| 历史窗口查询 | 当前和历史周期都能用 `controlScopeId + periodId` 查询控制额度；需要按账户隔离时必须带目标账户和币种。 |
| 告警和 Runbook | 摘要冲突、挂载冲突、拒绝率异常、控制投影缺证据、滚动窗口慢查询和迁移失败都要有发现方式、处理 owner、止血动作和恢复验收。 |
| 灰度和回滚 | 规则回滚通过新增版本、停用挂载或恢复旧挂载完成；不得覆盖已发布版本、删除历史决策或修改历史控制流水。 |

受控试点角色交接：

- 产品 owner 确认试点只包含单条规则评估、最终决策证据、控制流水、控制投影、交易消费和退款补偿；可信控制释放由上层显式提交。
- 架构 owner 确认接入方只依赖 `wallet-face`、`transaction-face`、`ledger-face` 和 `core`，不依赖 `*-impl`、Entity、Mapper 或内部包。
- 测试 owner 回挂 evaluator、admission、movement、projection 和 transaction consumption 验证簇；进入发版前补 `just compile`，提交前补 `just pmd`。
- 发布 owner 确认灰度、回滚、告警、Runbook 和人工接管；确认不完整时停在联调、预发或受控试点。

SC-LOOP-06 准出交接卡：

| 交接项 | 结论 |
| --- | --- |
| 试点能力 | 单条规则只读评估、可信决策写入、wallet binding 解析与 decisionRef 回读验真、控制流水、控制投影、交易消费和退款补偿；可信控制释放由上层显式提交。 |
| 推荐入口 | `evaluate` -> `recordDecision` -> `resolveSpendControlAdmission` -> `adjustLimit` -> 交易层入口 -> `consume`。 |
| 生产前证据 | 规则变更审计、最终决策证据、历史窗口查询、告警 / Runbook、灰度 / 回滚。 |
| 不接入范围 | P2 VCC / 全球账户 / ACH / 收单 / FX quote 与执行、Highnote 托管规则引擎、rolling amount、cooldown、协同授权 webhook、完整生产启用或上线审批。 |

推荐入口：

1. `SpendRuleEvaluationApplicationService.evaluate` 可选只读评估单条已发布规则
2. 可信规则或业务决策方调用 `SpendRuleDecisionRecordService.recordDecision` 固化 `PASSED` / `REJECTED`
3. 普通交易调用方只把 `decisionSn` 作为决策引用交给 `SpendControlAdmissionApplicationService.resolveSpendControlAdmission`；rule、binding、结果和摘要字段即使提供也只用于一致性诊断
4. `BudgetControlLimitAdjustmentApplicationService.adjustLimit` 记录周期控制额度调增或调减
5. 交易层入口
6. `SpendControlTransactionConsumptionApplicationService` 记录交易消费或退款补偿事实；可信控制释放直接调用 `SpendControlMovementService`

SpendControlScope 是控制范围，不是账本主体。接入侧使用 `controlScopeId + periodId` 查询额度，并在准入 / 授权请求中传入 `controlScopeId`。

典型周期额度流程：

```mermaid
flowchart LR
    A["Spend Rule 设置周期额度"] --> B["周期刷新任务生成 LIMIT_INCREASED"]
    B --> C["授权前记录 RESERVED"]
    C --> D["交易成功记录 CONSUMED"]
    C --> E["交易失败记录 RELEASED"]
    D --> F["退款成功记录 REFUND_COMPENSATED"]
    B --> G["按 controlScopeId + periodId 查询预算控制投影"]
```

实际用例：2026-07 月度预算范围 `BG-SALES-2026` 刷新 10000 USD 控制额度，调度任务调用 `adjustLimit` 写入 `LIMIT_INCREASED`，请求携带 `controlScopeId=BG-SALES-2026`、`periodId=2026-07`。一笔企业卡授权通过后写入 `RESERVED 6000`；交易成功后 `consume` 写入 `CONSUMED 6000` 并继承原 `periodId`。查询 `BudgetControlProjectionQuery(controlScopeId=BG-SALES-2026, periodId=2026-07, currency=USD)` 返回该周期控制视图；历史周期查询只替换 `periodId`，例如查询 `periodId=2026-08` 不会混入 7 月流水，另一个 `controlScopeId` 的同周期流水也不会混入。

验证锚点：`SpendRuleEvaluationApplicationServiceTests`、`SpendControlAdmissionApplicationServiceTests`、`SpendControlMovementServiceFlowTests`、`SpendControlTransactionConsumptionApplicationServiceTests`。

### 6.7 让利出资记账

适用条件：上游已经决策完成平台、商户或合作方承担多少让利成本，本服务只记录出资方账目交易。

推荐入口：

| 场景 | 入口 |
| --- | --- |
| 记录出资 | `FundsBenefitContributionTransactionService.settle` |
| 冲回出资 | `FundsBenefitContributionTransactionService.refund` |

多方出资就多次调用 `complete`，不要批量 API，也不要把多个出资方合并到一个营销账户。

验证锚点：`FundsBenefitContributionTransactionServiceContractTests`、`FundsBenefitContributionTransactionServiceFlowTests`。

### 6.8 全球账户和跨境接入准入卡

全球账户、跨境收付款、ACH/银行转账和本地清算网络只作为上层业务能力包接入资金底座。接入方必须先完成产品生命周期、外部协议、合规材料、FX 决策、通道状态和对账来源解释，再把已经成立的资金事实交给 `wallet`、`transaction` 和 `ledger`。

`FxRateProvider` 是由接入方实现并注入的汇率来源端口，wind-funds 不提供默认实现，也不关心具体汇率来源及其接入方式。`FxRateProvider#getRateSnapshot` 只提供当前可用的来源价格快照。`FxRateSnapshot` 使用必填 `snapshotId` 标识 Wind 归一化快照，使用 `observedAt` 表达来源价格观测时间，并保留汇率提供方视角的 `MID/BID/ASK`；它不表达 provider、报价有效期、业务加点或换汇执行。接入方负责选择来源价格、完成客户定价或业务加点，并保存来源快照与最终决策的关联。

`FxAmountConversionService` 支持两种互斥模式：上层已完成客户加点或报价时显式传入最终 `FxAppliedRate`，服务不再查询 provider；只需按来源价格换算时指定 `FxPriceType.MID/BID/ASK`，服务调用 `FxRateProvider` 获取当前快照，选择对应价格并以 `snapshotId` 形成 `FxAppliedRate.rateId`。请求必须包含源金额和目标币种；跨币种必须提供 `FxAppliedRate` 或 `FxPriceType`，同币种两者都不需要并按 `rate=1` 处理。舍入模式默认 `HALF_UP` 且允许上层覆盖，以 `CurrencyIsoCode.getPrecision()` 为唯一币种精度来源，服务只在目标币种精度舍入一次。`TransactionAmount.converted(FxAmountConversionResult)` 可直接承接目标金额、原币金额和最终汇率；`rateId` 只保留在换算结果的 `FxAppliedRate` 中。应用汇率最多支持 10 位整数和 8 位小数，超出账本 `DECIMAL(18,8)` 无损保存范围时在进入交易前失败。`UNKNOWN` 不能作为源币种、目标币种或汇率事实币种；源金额、目标金额或目标币种舍入结果非正，以及金额超出系统上限时均明确失败，不会静默截断、溢出或变成负数。有关联原支付路径的退款、授权退款和费用退款必须通过 `TransactionAmount` 显式传入本次退款目标金额、对应原币金额和原支付快照汇率；部分退款不能复制原交易整笔原币金额，汇率与原支付 route leg 快照不一致时失败，也不能重新查询当前 provider。找不到原支付路径但经上层业务确认允许承接的退款，由上层提供完整、已确认的本次资金事实，不伪造原路由。授权撤销、清算、历史重算和对账同样只消费已确认的金额与汇率事实；`FxAppliedRate` 不能替代 quote、有效期、费用、审批、换汇执行结果或资金入账事实。

| 外部事实或状态 | 能否进入资金底座 | 推荐入口 | 必须带上的证据 | 不能做 |
| --- | --- | --- | --- | --- |
| 外部已确认入金，且目标是内部资金账户。 | 可以。 | `ExternalFundsEventApplicationService.consume` 或 `PaymentInstrumentTransactionApplicationService.receiveByInstrument`。 | 外部事件流水、confirmed credit 类型、目标资金账户、金额、币种、业务流水、外部 rail / provider 引用。 | 不把 VA、银行账户或外部账户建成 ledger subject。 |
| 外部入金只是 accepted、submitted、processing、message sent 或待匹配。 | 不可以。 | 停在上游入金单、在途、挂账、对账或人工复核。 | 外部引用、状态、文件摘要、匹配原因、待处理 owner。 | 不增加 `AVAILABLE`，不生成 `topup`。 |
| 外部出款已终态成功，且可以关闭内部冻结资金。 | 可以。 | 上层出款业务确认后调用 `FundsDirectTransactionService.withdraw`。 | 原冻结流水、经业务确认的外部出款事实、外部收款账户、金额、币种和业务流水。 | 不把 provider 状态解释下沉到资金底座；不按当前支付工具绑定重新选资金主体。 |
| `LOCKED + EXTERNAL_ENDPOINT` 结算单进入通用出款。 | 可以进入出款事实。 | 通过 `PayoutOrderApplicationService#createOrder/submitOrder/handleReceipt`；宿主实现唯一 `PayoutSubmissionAuthority` 并负责外部投递和状态归一。 | 锁定结算单、PAYOUT Gate 结果、宿主准入决策摘要与证据引用、回单唯一引用、外部 reference、金额币种、来源摘要。 | 不直接调用 `FundsPayoutTransactionService`，不把 submit 当外部受理，不在失败后覆盖原单重试。 |
| 结算出款已提交、受理、处理中或 message sent。 | 只可进入 PayoutOrder 非终态。 | 宿主归一后调用 `handleReceipt`；W2 保持资金在 `SETTLEMENT`。 | 出款单、可信外部状态、回单唯一引用、证据摘要、回单拉取计划、超时告警和处理 owner。 | 不展示到账成功，不关闭 `SETTLEMENT`，不生成 `withdraw` 或 `IN_TRANSIT`。 |
| 退汇、return、NOC、外部 rail reversal、金额不一致或费用不一致。 | 不可以直接进入默认交易入口。 | 进入对账差错、退汇专项或人工处理，明确责任后再生成资金事实。 | 原交易或原出款引用、外部原因、费用、责任方、审批或差错单。 | 不降级成普通 `refund`，不净额静默抵消。 |
| 涉及跨币种、FX、汇兑损益或错币种到账。 | 默认不可以。 | 先由 FX / treasury 或差错链路确认。 | quoteRef、原币、目标币、汇率、有效期、执行结果、审批引用。 | 不自动换汇，不按期望币种静默入账。 |

接入申请必须能回答四个问题：外部状态是否终态、内部账务主体是谁、金额币种是否可直接入账、失败或退回时由谁负责处理。回答不完整时，只能登记在途、差错或人工复核，不进入资金事实链路。

验证锚点：`ExternalFundsEventApplicationServiceTests`、`PaymentInstrumentTransactionApplicationServiceTests`、`FundsWithdrawalSuccessFlowTests`、`PayoutPreflightServiceTests`、`PayoutOrderApplicationServiceTests`、`PayoutPublicContractTests`；完整退汇、FX、跨境合规和多币种对账仍需独立业务专项。

## 7. 禁止路径

| 禁止项 | 原因 |
| --- | --- |
| 业务方直接依赖 `ledger-impl`、`wallet-impl`、`transaction-impl`。 | 破坏模块边界，绕过公共契约。 |
| 业务方直接写 ledger entry 或改余额投影。 | 破坏账本事实源。 |
| 把支付工具、外部账户、VA、卡号、SpendControlScope、Spend Rule 当作 ledger subject。 | 它们只是引用或控制范围。 |
| 把冻结当消费、扣款或提现成功。 | 冻结只表达同主体 `AVAILABLE <-> FROZEN`。 |
| 授权拒绝后继续生成 route、posting 或 ledger entry。 | 拒绝不是资金事实。 |
| 用 `contextVariables` 承载核心金额、分摊、账户或规则事实。 | 核心事实必须是强字段或稳定引用。 |
| 外部 pending、审批中、通道处理中直接入账。 | 还没有成立资金事实。 |

## 8. 生产接入检查和发布 Runbook

接入生产前，业务 owner、研发 owner、QA owner 和发布 owner 必须共同确认下面清单。确认不完整时，只能停在联调、预发或受控试点，不得按生产能力对外承诺。

### 8.1 生产接入检查清单

| 检查项 | 准出要求 |
| --- | --- |
| 账务三问 | 每个入口都能回答“谁的钱、多少钱、怎么变的”，并能映射到资金主体、金额币种、交易类型和账本影响。 |
| 事实终态 | 外部事件必须是已确认入金或终态成功出款；pending、processing、审批中、通道处理中不得入账。 |
| 公共契约 | 接入方只依赖 `*-face` 和 `core`，不得依赖 `*-impl`、Entity、Mapper、测试工具或内部包。 |
| 幂等和重放 | 业务流水、请求摘要、原交易引用和重复提交行为已验证；同键不同摘要必须拒绝。 |
| 失败无副作用 | 准入失败、余额不足、规则拒绝、外部非终态和路由失败不得留下错误资金事实。 |
| 账务验收 | 已验证 transaction、route、posting、ledger transaction、ledger entry、余额桶和投影查询。 |
| 敏感数据 | 不在 request、contextVariables、日志或审计摘要中保存 PAN、CVV、密钥、证件号、手机号或外部账号原文。 |
| P2 边界 | VCC、全球账户、ACH、收单、FX quote 与执行、退汇、NOC、外部 rail reversal、多币种对账和通道协议未进入本轮交付承诺；已提供的 FX 来源价格与金额换算不改变该边界。 |

### 8.2 发布 Runbook

| 阶段 | 动作 | Owner |
| --- | --- | --- |
| 发布前 | 执行相关 `verify-slice`；高风险或发版收口执行 `just verify-cad`。 | QA owner |
| 灰度 | 先接入单一租户、单一业务场景或单一资金入口；观察幂等冲突、拒绝率、余额投影差异和异常日志。 | 发布 owner |
| 监控 | 关注交易失败率、账本不平衡、posting 失败、余额不足异常、Spend Rule 拒绝率、外部事件重复和对账差异。 | 发布 owner |
| 回滚 | 停止新流量；保留已生成资金事实；通过业务补偿、反向交易、原交易冲回或人工差错单处理，不直接改账。 | 研发 owner |
| 人工接管 | 无法判断资金责任、外部终态、汇兑差异、退汇责任或余额差异时，停止自动处理并转人工复核。 | 业务 owner |

## 9. 验证命令

接入文档或代码变更至少跑相关切片。只改本文档时可跑边界和相关场景测试，不必每次全量。

| 变更范围 | 推荐命令 |
| --- | --- |
| ledger 接入说明或账本事实 | `just verify-slice DefaultLedgerPostingAssemblerTests,LedgerServiceImplTests,LedgerTransactionServiceImplTests,LedgerBalanceProjectionServiceImplTests tests` |
| wallet 接入说明 | `just verify-slice FundsAccountCapabilityApplicationServiceTests,PaymentInstrumentCapabilityApplicationServiceTests,FundingResponsibilityResolutionApplicationServiceTests,SpendControlAdmissionApplicationServiceTests tests` |
| transaction 接入说明 | `just verify-slice FundsDirectTransactionFlowTests,FundsAuthorizationTransactionFlowTests,FundsTransactionFeeFlowTests,FundsBalanceControlFailureFlowTests tests` |
| FX 来源价格和金额换算 | `just test-fx` |
| 让利或外部入金说明 | `just verify-slice FundsBenefitContributionTransactionServiceContractTests,FundsBenefitContributionTransactionServiceFlowTests,ExternalFundsEventApplicationServiceTests tests` |
| 模块边界 | `just test-boundary` |
| 收口 | `just verify-cad` |

## 10. 接入申请最小模板

```text
业务动作：
使用入口：
业务流水/幂等键：
操作者：
资金主体：
账户类型：
金额/币种：
支付工具或外部引用：
准入快照：
原交易或原冻结引用：
预期余额桶变化：
预期账本交易/分录：
失败无副作用要求：
对应测试类：
验证命令：
不接入范围：
```

## 11. 当前不在本指南承诺范围

已声明的对账、清分、内部清算、结算和出款事实切片按专项设计与验收接入，不并入 ledger/wallet/transaction 三层主链的默认通用接入承诺。宿主只使用 `ClearingBatchApplicationService`、`SettlementOrderApplicationService` 和 `PayoutOrderApplicationService`；Gate 检查、出款预检和只读发现结果都不构成提交授权。

具体银行/PSP/ACH/钱包通道协议、出款调度与重试、失败后换渠道、追偿策略与资金执行、归档、重放、指标治理、VCC 产品化、全球账户和收单不在当前接入承诺范围内。它们可以消费已有主链和清结算对账事实，但需要单独的产品设计、系统设计、TDD 和工程变更边界。

全球账户或跨境出款接入时，外部状态归一、终态判定、通道回单和收款人信息仍属于上游 P2 出款业务。基于 `LOCKED + EXTERNAL_ENDPOINT` 结算单的通用出款必须走 `PayoutOrderApplicationService`，由宿主准入、投递并回传归一回单；宿主不得直接调用 `FundsPayoutTransactionService`。只有独立的“原冻结提现”场景在上游确认成功后，才携带原 `referenceFreezeSn` 调用 `FundsDirectTransactionService.withdraw`。两条路径不能互换；外部 `submitted`、`accepted`、`processing`、`message sent` 都不能解释为到账成功。
