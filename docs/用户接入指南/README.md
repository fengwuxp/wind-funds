# wind-funds 用户接入指南

## 1. 定位

本文面向要接入 `ledger`、`wallet`、`transaction` 三个主链模块的业务系统和内部研发。它只说明当前代码中已有公共契约和测试证明过的接入方式，不承诺 VCC、全球账户、收单、清结算、对账、归档或治理的生产接入。

接入方只依赖 `core`、`ledger-face`、`wallet-face`、`transaction-face`。不要依赖 `*-impl`、Entity、Mapper、内部状态机或测试工具类。

## 2. 模块边界

| 模块 | 定位 | 接入方能做 | 接入方不能做 |
| --- | --- | --- | --- |
| `ledger` | 账本事实和余额投影。 | 查询账本、账本交易、分录和余额投影；在底层服务级测试中验证过账事实。 | 业务方不直接拼分录、不直接改余额、不把投影当事实源。 |
| `wallet` | 账户、支付工具、资金责任、支出控制和准入快照。 | 建模资金账户/信用账户，解析账户能力、支付工具能力、资金责任和 Spend Rule 决策。 | 不创建交易事实，不写 route、posting、ledger entry 或余额投影。 |
| `transaction` | 标准资金交易、授权交易、余额控制、让利出资记账和外部确认入金消费。 | 提交已经成立的资金事实，由交易层编排路由、账务和账本影响。 | 不接收页面动作、审批中、通道处理中或外部非终态。 |

依赖方向是 `transaction -> wallet -> ledger`。横向治理或对账能力只能通过 face/core 只读消费事实，不反写主链事实。

## 3. 接入顺序

1. 先确认业务事实已经成立：谁的钱、什么动作、金额、币种、幂等键、来源引用和操作者。
2. 先走 `wallet` 建模或准入：账户能力、支付工具能力、资金责任、Spend Rule 决策。
3. 再走 `transaction` 产生资金事实：直接交易、授权交易、余额控制、让利出资、外部确认入金。
4. 最后用 `ledger` 和投影查询验收：账本交易、分录、余额桶、幂等和失败无副作用。

如果第 1 步说不清，不进入 wind-funds。

## 4. 能力矩阵

| 能力 | 公共入口 | 已验证场景 | 主要测试 |
| --- | --- | --- | --- |
| 账本基础 | `LedgerService`、`LedgerTransactionService` | 建账、过账、按 SN 查询交易/分录、余额投影。 | `LedgerServiceImplTests`、`LedgerTransactionServiceImplTests`、`DefaultLedgerPostingAssemblerTests`、`LedgerBalanceProjectionServiceImplTests` |
| 账户和余额查询 | `FundsAccountCapabilityApplicationService`、`FundsSubjectBalanceQueryService` | 账户能力准入、余额查询、账本 profile 初始化。 | `FundsAccountCapabilityApplicationServiceTests`、`FundsSubjectBalanceQueryServiceImplTests`、`LedgerProfileContractTests`、`ControlAccountLedgerInitializationTests` |
| 支付工具 | `PaymentInstrumentCapabilityApplicationService`、`PaymentInstrumentPreTransactionSnapshotApplicationService` | 支付工具能力、绑定快照、交易前快照。 | `PaymentInstrumentCapabilityApplicationServiceTests`、`PaymentInstrumentPreTransactionSnapshotApplicationServiceTests`、`PaymentInstrumentServiceImplTests` |
| 资金责任 | `FundingResponsibilityResolutionApplicationService` | 按关系解析默认资金责任主体。 | `FundingResponsibilityResolutionApplicationServiceTests`、`SpendSubjectFundingRelationServiceImplTests` |
| 支出控制 | `SpendControlAdmissionApplicationService`、`BudgetControlLimitAdjustmentApplicationService`、`SpendControlTransactionConsumptionApplicationService` | Spend Rule 准入、预算控制调整、交易消费记录。 | `SpendControlAdmissionApplicationServiceTests`、`BudgetControlLimitAdjustmentApplicationServiceTests`、`SpendControlTransactionConsumptionApplicationServiceTests`、`SpendRuleDefinitionServiceFlowTests` |
| 直接交易 | `FundsDirectTransactionService` | 充值、转账、付款、退款、提现、手续费、退费。 | `FundsDirectTransactionFlowTests`、`FundsTransactionFeeFlowTests`、`FundsTransferPayWithdrawChainFlowTests` |
| 授权交易 | `FundsAuthorizationTransactionService` | 授权、撤销、完成、完成后退款。 | `FundsAuthorizationTransactionFlowTests` |
| 余额控制 | `FundsBalanceControlService` | 冻结、解冻、受控调整；冻结不表达扣款。 | `FundsBalanceControlFailureFlowTests`、`FundsFrozenOrderServiceImplTests`、`FundsWithdrawalSuccessFlowTests`、`FundsWithdrawalAfterPartialUnfreezeFlowTests` |
| 让利出资记账 | `FundsBenefitContributionTransactionService` | 平台/商户/合作方已决策让利出资入账和按原交易冲回。 | `FundsBenefitContributionTransactionServiceContractTests`、`FundsBenefitContributionTransactionServiceFlowTests` |
| 外部确认入金 | `ExternalFundsEventApplicationService` | 已确认外部入金消费为标准充值，目标必须是资金账户。 | `ExternalFundsEventApplicationServiceTests` |

## 5. 业务事实说明卡

每个接入场景先填这 10 项，填不满就不要拆研发任务。

| 项 | 必填内容 |
| --- | --- |
| 业务动作 | topup、transfer、pay、refund、withdraw、fee、authorize、reversal、settle、settleRefund、freeze、unfreeze、adjust、benefit settle/refund、external confirmed credit。 |
| 资金主体 | 成本承担方、资金流出方、资金流入方、授权主体或冻结主体。 |
| 账户类型 | FundingAccount、CreditAccount、平台账户角色解析结果；BudgetGroup、Spend Rule、PaymentInstrument 只能是控制或引用。 |
| 金额币种 | 金额必须为正，币种和精度必须能和账户、账本 profile 对齐。 |
| 幂等 | 业务流水、请求摘要、重复提交和同键不同摘要处理。 |
| 来源引用 | 订单、提现单、授权单、外部事件、原交易 SN 或原冻结单 SN。 |
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
| 完成 | `settle` | 基于原授权路径完成，不按当前绑定重新选路。 |
| 完成后退款 | `settleRefund` | 引用原完成事实。 |

验证锚点：`FundsAuthorizationTransactionFlowTests`。

### 6.4 冻结、解冻和调整

适用条件：表达同一主体余额控制或有审批凭证的余额调整。

推荐入口：

| 场景 | 入口 | 禁止项 |
| --- | --- | --- |
| 冻结 | `FundsBalanceControlService.freeze` | 不能表达消费或跨主体转移。 |
| 解冻 | `FundsBalanceControlService.unfreeze` | 不能超过原冻结事实。 |
| 调整 | `FundsBalanceControlService.adjust` | 不能替代付款、退款、提现或对账差错闭环。 |

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

适用条件：需要在交易前固化支出控制决策，或记录交易消费对控制范围的影响。

推荐入口：

1. `SpendControlAdmissionApplicationService.resolve`
2. 交易层入口
3. `SpendControlTransactionConsumptionApplicationService` 记录消费事实

BudgetGroup 是控制范围，不是账本主体。

验证锚点：`SpendControlAdmissionApplicationServiceTests`、`SpendControlMovementServiceFlowTests`、`SpendControlTransactionConsumptionApplicationServiceTests`。

### 6.7 让利出资记账

适用条件：上游已经决策完成平台、商户或合作方承担多少让利成本，本服务只记录出资方账目交易。

推荐入口：

| 场景 | 入口 |
| --- | --- |
| 记录出资 | `FundsBenefitContributionTransactionService.settle` |
| 冲回出资 | `FundsBenefitContributionTransactionService.refund` |

多方出资就多次调用 `settle`，不要批量 API，也不要把多个出资方合并到一个营销账户。

验证锚点：`FundsBenefitContributionTransactionServiceContractTests`、`FundsBenefitContributionTransactionServiceFlowTests`。

## 7. 禁止路径

| 禁止项 | 原因 |
| --- | --- |
| 业务方直接依赖 `ledger-impl`、`wallet-impl`、`transaction-impl`。 | 破坏模块边界，绕过公共契约。 |
| 业务方直接写 ledger entry 或改余额投影。 | 破坏账本事实源。 |
| 把支付工具、外部账户、VA、卡号、BudgetGroup、Spend Rule 当作 ledger subject。 | 它们只是引用或控制范围。 |
| 把冻结当消费、扣款或提现成功。 | 冻结只表达同主体 `AVAILABLE <-> FROZEN`。 |
| 授权拒绝后继续生成 route、posting 或 ledger entry。 | 拒绝不是资金事实。 |
| 用 `contextVariables` 承载核心金额、分摊、账户或规则事实。 | 核心事实必须是强字段或稳定引用。 |
| 外部 pending、审批中、通道处理中直接入账。 | 还没有成立资金事实。 |

## 8. 验证命令

接入文档或代码变更至少跑相关切片。只改本文档时可跑边界和相关场景测试，不必每次全量。

| 变更范围 | 推荐命令 |
| --- | --- |
| ledger 接入说明或账本事实 | `just verify-slice DefaultLedgerPostingAssemblerTests,LedgerServiceImplTests,LedgerTransactionServiceImplTests,LedgerBalanceProjectionServiceImplTests tests` |
| wallet 接入说明 | `just verify-slice FundsAccountCapabilityApplicationServiceTests,PaymentInstrumentCapabilityApplicationServiceTests,FundingResponsibilityResolutionApplicationServiceTests,SpendControlAdmissionApplicationServiceTests tests` |
| transaction 接入说明 | `just verify-slice FundsDirectTransactionFlowTests,FundsAuthorizationTransactionFlowTests,FundsTransactionFeeFlowTests,FundsBalanceControlFailureFlowTests tests` |
| 让利或外部入金说明 | `just verify-slice FundsBenefitContributionTransactionServiceContractTests,FundsBenefitContributionTransactionServiceFlowTests,ExternalFundsEventApplicationServiceTests tests` |
| 模块边界 | `just test-boundary` |
| 收口 | `just verify-cad` |

## 9. 接入申请最小模板

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

## 10. 当前不在本指南承诺范围

清结算、对账、归档、重放、指标治理、VCC 产品化、全球账户、ACH、收单和通道协议适配不在本文接入承诺范围内。它们可以消费本文三层主链事实，但需要单独的产品设计、系统设计、TDD 和工程变更边界。
