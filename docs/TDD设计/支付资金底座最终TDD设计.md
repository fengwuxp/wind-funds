# 支付资金底座最终 TDD 设计

## 一、文档定位

本文是支付资金底座的最终版 TDD 设计入口，承接：

| 输入 | 本文如何使用 |
| --- | --- |
| `docs/产品设计/05-产品验收与TDD用例矩阵.md` | 提炼产品必须被证明的事实、红线和验收样例。 |
| `docs/DSL设计/支付资金底座最终DSL设计.md` | 把产品用例转成 `FundsInstruction`、`RouteSnapshot`、`PostingPlan`、`LedgerEntry` 和投影断言。 |
| `docs/系分设计/02-交易路由钱包账目与投影系分设计.md` | 映射交易、路由、钱包、账目、余额投影和交易投影的服务入口与执行流程。 |
| `docs/系分设计/05-测试观测安全与金融红线.md` | 继承测试分层、观测、安全、审计和金融红线。 |
| 代码模块与现有测试资产 | 校准真实服务、真实包名、已有覆盖和待补缺口。 |

本文不替代 PRD、DSL 或系分设计。本文只回答一个问题：

> 每个资金能力要如何通过测试证明：产品语义可靠、DSL 事实正确、系统执行路径真实、账务结果可信、异常和红线可阻断。

## 二、测试驱动设计原则

资金系统的 TDD 不是先写大量 Mock，也不是追求代码行覆盖率。它要先构造可验收的业务用例，再用测试证明真实执行路径没有偏离产品和 DSL。

| 原则 | 要求 |
| --- | --- |
| 场景先于实现 | 先写清角色、前置条件、输入事实、动作、期望状态、余额变化和红线，再决定测试类和断言方式。 |
| 事实先于接口 | 测试证明的是资金事实成立，不是某个方法被调用。 |
| 真实路径优先 | 非外部依赖尽量使用真实 converter、resolver、orchestrator、assembler、posting/projection 逻辑；只替换外部通道、时间、ID 和不可控基础设施。 |
| 每步余额断言 | 组合场景每一步都断言余额变化，不只断言最终余额。 |
| 账务证据闭环 | 有资金变化时必须同时证明 route snapshot、posting plan、ledger transaction、ledger entry 和 balance projection 可互相解释。 |
| 失败无副作用 | 余额不足、错币种、缺快照、超额、幂等冲突、权限不足等失败路径必须断言无 route、无 posting、无 entry 或余额无变化。 |
| 红线测试常驻 | 外部账户入账、直接改余额、授权拒绝写账、冻结表达消费、投影反写事实等必须有失败测试或明确不适用原因。 |

## 三、测试证据链

每个用例都应尽量形成以下证据链。小的纯单元测试可以只覆盖其中一段，但业务组合测试必须覆盖完整链路。

```mermaid
flowchart LR
    A["产品场景\n角色 / 目标 / 输入事实"] --> B["服务请求\nRequest / Operator / 幂等键"]
    B --> C["FundsInstruction\n指令类型 / 事件 / 金额 / 引用"]
    C --> D["RouteResolver\n参与方 / 账目 / route leg"]
    D --> E["RouteSnapshot\n历史路径固定"]
    E --> F["LedgerPostingAssembler\nposting plan 独立平衡"]
    F --> G["LedgerEntry\n不可变分录"]
    G --> H["BalanceProjection\n余额桶变化"]
    G --> I["TransactionView\n只读交易视图"]
    H --> J["审计与幂等\n重复 / 冲突 / 失败无副作用"]
```

| 证据 | 最低断言 |
| --- | --- |
| 产品输入 | 业务场景、业务流水、主体、金额、币种、操作者、引用完整。 |
| 指令 | `instructionType`、`eventType`、`transactionType`、`amount/originalAmount/exchangeRate` 正确。 |
| 路由 | 参与方、平台账户、账目、余额桶、route leg 与产品资金链路一致。 |
| 快照 | 后续退款、撤销、结算、拒付、退费、解冻优先使用原路径。 |
| 账务计划 | 每个 posting plan 独立平衡，币种一致，entry 金额为正。 |
| 分录 | `LedgerEntry` 可追溯到资金指令、route leg、账本交易和业务引用。 |
| 余额投影 | 相关主体、账目、币种、账本周期的 delta 正确。 |
| 交易投影 | 只从交易事实、route snapshot 和分录摘要派生，不反写事实。 |
| 幂等审计 | 同键同摘要复用结果，同键不同摘要失败；高危动作有原因、凭证、审批或操作者。 |

## 四、测试分层与工程落点

| 层级 | 目标 | 工程落点 | 适用场景 |
| --- | --- | --- | --- |
| L1 DSL/契约/纯单元 | 锁定枚举、Spec、值对象、转换、摘要和不变量。 | `core/src/test`、`tests/src/test`。 | DSL JSON、金额模型、SettlementPolicy、route/replay 类型、entry digest。 |
| L2 模块服务测试 | 覆盖真实 converter、resolver、assembler、lifecycle 和 command service。 | `tests/src/test/java/com/capte/funds/**`。 | 三类交易服务、路由解析、账务组装、生命周期、冻结单。 |
| L3 业务组合测试 | 串联多步业务动作，每步断言余额、账务和幂等。 | `com.capte.funds.transaction.application.flow`。 | 充值-付款-退款、充值-冻结-提现、A 转 B 后付款提现、授权链组合。 |
| L4 数据库/事务测试 | 验证 Mapper、唯一键、事务、H2 schema、投影持久化。 | `tests` 模块，H2 MySQL Mode。 | 幂等唯一约束、ledger transaction 入账、余额投影持久化、查询服务。 |
| L5 架构和红线测试 | 防止模块边界和金融红线被破坏。 | `*LayerBoundaryTests`、契约失败测试。 | `wallet` 不写交易、`route` 不写账、`ledger` 不持有交易生命周期、外部账户不入账。 |

测试包名按被测能力归属：

| 能力 | 推荐包 |
| --- | --- |
| 交易门面服务 | `com.capte.funds.transaction.application` |
| 交易业务组合 | `com.capte.funds.transaction.application.flow` |
| 交易编排 | `com.capte.funds.transaction.application.orchestration` |
| 请求转换 | `com.capte.funds.transaction.converter` |
| 路由与回放 | `com.capte.funds.route` |
| 账务组装 | `com.capte.funds.transaction.ledger` |
| 交易生命周期 | `com.capte.funds.transaction.services.impl` |
| 钱包账户 | `com.capte.funds.wallet`、`com.capte.funds.wallet.services.impl` |
| Ledger | `com.capte.funds.ledger`、`com.capte.funds.ledger.impl` |

## 五、模块与真实执行路径矩阵

| 模块 | 主要服务或组件 | TDD 必须覆盖的真实路径 | 已有代表测试资产 |
| --- | --- | --- | --- |
| `core` | `FundsInstructionSpec`、route spec、ledger spec、`SettlementPolicySpec` | 指令结构、route snapshot、replay、posting、金额、FX 快照和 JSON 契约可解析。 | `FundsInstructionSpecContractTests`、`RouteDslContractTests`、`TransactionServiceAbilityDslJsonContractTests`、`SettlementPolicySpecTests`。 |
| `transaction-face` | `FundsDirectTransactionService`、`FundsAuthorizationTransactionService`、`FundsBalanceControlService` | 对外服务方法、请求模型、业务身份、幂等语义和返回流水。 | `FundsTransactionCommandServiceImplTests`、`FundsAuthorizationTransactionCommandServiceImplTests`、`FundsBalanceControlCommandServiceImplTests`。 |
| `transaction-impl` | `FundsTransactionCommandServiceImpl` | 请求 -> converter -> `FundsInstructionOrchestrator#execute`，失败前不得入编排。 | `FundsTransactionCommandValidationTests`、三类 command service 测试。 |
| `transaction-impl` | Direct/Authorization/BalanceControl converter | Request 到 `FundsInstructionSpec` 的指令、事件、金额、FeeSpec、reference、operator 和 FX 决策快照。 | `FundsDirectTransactionInstructionConverterTests`、`FundsDirectTransactionFeeInstructionConverterTests`、`FundsAuthorizationInstructionConverterTests`、`FundsBalanceControlInstructionConverterTests`。 |
| `transaction-impl` | `TransferFundsInstructionRouteResolver` | 充值、付款、转账、提现、退款、手续费和退费的 route leg。 | `TransferFundsInstructionRouteResolverTests`、`TransferFundsInstructionFeeRouteResolverTests`。 |
| `transaction-impl` | `AuthorizationFundsInstructionRouteResolver` | 授权批准、撤销、结算、退款、拒付、共享卡多主体 route。 | `AuthorizationFundsInstructionRouteResolverTests`、`AuthorizationSharedCardFundsInstructionRouteResolverTests`。 |
| `transaction-impl` | `BalanceControlFundsInstructionRouteResolver` | 冻结、解冻、资金调账、信用额度和预算调额 route。 | `BalanceControlFundsInstructionRouteResolverTests`、`BalanceControlFundingAdjustRouteResolverTests`、`BalanceControlLimitAdjustRouteResolverTests`。 |
| `transaction-impl` | `DefaultRoutedFundsInstructionOrchestrator` | resolver 选择、route snapshot、lifecycle、posting、成功/失败归纳。 | `DefaultRoutedFundsInstructionOrchestratorTests`、`DefaultRoutedFundsInstructionOrchestratorReplayTests`。 |
| `transaction-impl` | `DefaultLedgerPostingAssembler` | route leg 到 posting plan 和 entry spec；账本周期、normal balance、平衡和约束。 | `DefaultLedgerPostingAssemblerTests`、`DefaultLedgerPostingAssemblerPeriodTests`、`DefaultLedgerPostingAssemblerValidationTests`。 |
| `transaction-impl` | Lifecycle saver / frozen order service | 交易生命周期、引用事实、幂等摘要、手续费汇总、冻结剩余。 | `DefaultFundsInstructionLifecycleSaverTests`、`DefaultFundsFrozenOrderLifecycleSaverTests`、`FundsFrozenOrderServiceImplTests`。 |
| `ledger-face/impl` | `LedgerTransactionPostingService`、`LedgerTransactionService` | 账本交易创建、幂等入账、posting plan 校验、entry 落地和查询。 | `DefaultLedgerTransactionPostingServiceImplTests`、`LedgerTransactionServiceImplTests`、`LedgerTransactionServiceIdempotencyTests`。 |
| `ledger-impl` | `LedgerBalanceProjectionService` | normal balance、受控负数、投影事件、失败不污染事实。 | `LedgerBalanceProjectionServiceImplTests`、`LedgerBalanceProjectionEventTests`、`LedgerBalanceProjectionNegativeConstraintTests`。 |
| `wallet-face/impl` | 资金账户、信用账户、预算组、平台账户、支付工具、余额查询 | 显式账本初始化、平台角色解析、工具只做引用、余额查询不修复事实。 | `FundingAccountServiceImplTests`、`CreditAccountServiceImplTests`、`BudgetGroupServiceImplTests`、`PlatformFundingAccountServiceImplTests`、`DefaultFundsSubjectBalanceQueryTests`。 |
| `tests/support` | `FundsBalanceAssertionSupport` | 每一步余额快照、delta、posting 平衡和无副作用断言。 | `FundsTransactionLedgerBalanceAssertionsTests`、业务 flow 测试。 |

## 六、直接交易测试矩阵

### 6.1 单场景验证

| 用例 ID | 场景 | 服务入口 | 真实执行路径 | 核心断言 |
| --- | --- | --- | --- | --- |
| TDD-DIR-001 | 充值成功 | `FundsDirectTransactionService#topup` | request -> direct converter -> transfer resolver -> posting assembler -> ledger/projection。 | 用户 `AVAILABLE` 增加；外部账户只做引用；posting 平衡；重复通知幂等。 |
| TDD-DIR-002 | 付款成功 | `pay` | request -> fee/main leg -> route -> posting。 | 付款方 `AVAILABLE` 减少；收款方目标桶增加；普通支付不默认走商户清算。 |
| TDD-DIR-003 | 商户订单付款 | `pay` | request -> merchant route -> posting。 | 用户 `AVAILABLE` 减少；商户 `CLEARING` 增加；不得直入 `AVAILABLE/SETTLEMENT`。 |
| TDD-DIR-004 | 转账成功 | `transfer` | request -> transfer route -> posting。 | A 减少，B 增加；同主体失败；币种不一致失败。 |
| TDD-DIR-005 | 原交易退款 | `refund` | refund request -> original route replay -> posting。 | 基于原 route snapshot；累计退款不超额；不按当前绑定重新选路。 |
| TDD-DIR-006 | 手续费收取 | `fee` 或含 `FeeSpec` 的主交易 | request -> fee leg -> platform fee account。 | 本金和费用拆 leg；平台 `FEE` 增加；费用不混入本金。 |
| TDD-DIR-007 | 手续费退回 | `refundFee` | fee refund request -> fee leg replay。 | 普通退款不默认退费；退费不超过原手续费；只回放费用路径。 |
| TDD-DIR-008 | 提现成功 + 手续费 | `freeze` 后 `withdraw` + `fee` 或提现 `FeeSpec` | balance control freeze -> direct fund out -> fee leg。 | 申请阶段只冻结；确认出款消耗冻结来源；手续费独立入平台 `FEE`；重复回调不重复扣。 |
| TDD-DIR-009 | 提现撤销或拒绝 | `freeze` 后 `unfreeze` | balance control freeze -> unfreeze。 | 不生成 `FUND_OUT`；本金和费用预留回到 `AVAILABLE`；重复撤销/拒绝不重复解冻。 |
| TDD-DIR-010 | 受控透支费用 | `fee` | fee request -> negative policy -> posting。 | 无策略失败且无余额变化；有策略生成负余额治理口径。 |
| TDD-DIR-011 | 错币种直接交易 | `pay/transfer/topup` | request amount fact -> converter -> route。 | 交易层只记录业务层已决策 FX 快照，不调用 `FxService`；账务主链路使用 `amount.currency`。 |

### 6.2 组合场景验证

| 用例 ID | 场景 | 步骤断言 |
| --- | --- | --- |
| TDD-DIR-FLOW-001 | 充值 -> 付款 -> 退款 | 充值后 A `AVAILABLE` 增加；付款后 A 减少、收款方增加；退款基于原路径回补；重复退款不重复入账。 |
| TDD-DIR-FLOW-002 | 充值 -> 付款含手续费 -> 本金退款 -> 手续费退回 | 手续费收取后平台 `FEE` 增加；普通退款不退费；`refundFee` 才回退费用；退费累计不超额。 |
| TDD-DIR-FLOW-003 | 充值 -> 冻结 -> 提现成功 | 冻结只做 `AVAILABLE -> FROZEN`；提现确认消耗 `FROZEN`；`AVAILABLE` 不被二次扣减。 |
| TDD-DIR-FLOW-004 | 充值 -> 冻结 -> 提现撤销/拒绝 | 冻结后可用减少；撤销或拒绝只解冻；不得生成提现交易和出款分录。 |
| TDD-DIR-FLOW-005 | A 充值 -> 转给 B -> B 付款 -> B 提现 | 每一步断言 A、B、商户、平台余额桶；B 提现先冻结再确认出款。 |
| TDD-DIR-FLOW-006 | 后置手续费触发受控负余额 | 已成功主交易后补收费用；无策略进入失败或人工处理；有策略记录负余额、上限、账龄和治理状态。 |

### 6.3 边界与异常

| 用例 ID | 场景 | 必须证明 |
| --- | --- | --- |
| TDD-DIR-ERR-001 | 同幂等键不同摘要 | 拒绝；不新增交易、route、entry 或投影。 |
| TDD-DIR-ERR-002 | 缺账本或平台账户 | 路由或账务失败；不自动建账；不展示交易成功。 |
| TDD-DIR-ERR-003 | 外部账户作为入账主体 | 失败；external/tool 只能出现在引用或快照。 |
| TDD-DIR-ERR-004 | 退款超额 | 失败；原交易已退金额不被污染。 |
| TDD-DIR-ERR-005 | 手续费退回超额 | 失败；平台 `FEE` 和原付费方余额不变化。 |
| TDD-DIR-ERR-006 | 提现成功后又撤销 | 失败或进入人工差错；不得把已确认出款再简单解冻。 |

## 七、授权交易测试矩阵

### 7.1 单场景验证

| 用例 ID | 场景 | 服务入口 | 核心断言 |
| --- | --- | --- | --- |
| TDD-AUTH-001 | 授权批准 | `FundsAuthorizationTransactionService#authorize` | 主体 `AVAILABLE -> AUTHORIZATION`；保存授权 route snapshot；授权不是消费。 |
| TDD-AUTH-002 | 授权拒绝 | `authorize(approved=false)` | 记录拒绝原因；无 route、posting、entry；不写 `chargebackAmount`。 |
| TDD-AUTH-003 | 部分撤销 | `reversal` | 基于原授权快照；`AUTHORIZATION -> AVAILABLE`；不超过剩余授权。 |
| TDD-AUTH-004 | 全额撤销 | `reversal` | 剩余授权归零；再次撤销失败或幂等返回。 |
| TDD-AUTH-005 | 部分结算 | `settle` | `AUTHORIZATION` 减少，收款方或清算桶增加；不触碰 `LIMIT`。 |
| TDD-AUTH-006 | 授权直接结算 | `authorize` 后 `settle` | 连续链路使用原授权 snapshot；结算金额不超过剩余授权。 |
| TDD-AUTH-007 | 授权结算后退款 | `settleRefund` | 基于已结算路径退款；累计退款不超过已结算金额。 |
| TDD-AUTH-008 | 授权结算后拒付 | `chargeback` | 与授权拒绝严格区分；不超过可追偿金额；拒付手续费可独立表达。 |
| TDD-AUTH-009 | 共享卡授权 | `authorize` | 卡是工具引用；信用账户、预算组、资金账户共同占用；任一失败整体失败。 |
| TDD-AUTH-010 | 预算组授权 | `authorize` | 预算组 `AVAILABLE -> AUTHORIZATION`；不触碰 `LIMIT`；不表达真实资金沉淀。 |
| TDD-AUTH-011 | 信用账户授权 | `authorize` | 信用 `AVAILABLE -> AUTHORIZATION`；普通结算不调整 `LIMIT`。 |

### 7.2 组合场景验证

| 用例 ID | 场景 | 步骤断言 |
| --- | --- | --- |
| TDD-AUTH-FLOW-001 | 授权问询 -> 部分撤销 -> 部分结算 -> 部分退款 | 授权占用、撤销释放、结算消费、退款回补逐步断言；累计金额不超过原授权和已结算金额。 |
| TDD-AUTH-FLOW-002 | 授权问询 -> 授权拒绝 | 拒绝只记录事实和原因；余额无变化；无 route/entry。 |
| TDD-AUTH-FLOW-003 | 授权批准 -> 直接全额结算 | 授权占用后全额结算；授权剩余归零；收款方或清算桶正确增加。 |
| TDD-AUTH-FLOW-004 | 共享卡授权 -> 部分结算 -> 剩余撤销 | 信用、预算、资金账户占用和释放同步；不回写 `LIMIT`。 |
| TDD-AUTH-FLOW-005 | 结算后拒付 + 拒付手续费 | 拒付本金和手续费拆 leg；普通退款与拒付区分；费用退回走 `refundFee` 或独立退费。 |

### 7.3 边界与异常

| 用例 ID | 场景 | 必须证明 |
| --- | --- | --- |
| TDD-AUTH-ERR-001 | 授权拒绝生成账务路径 | 必须失败；无 route、posting、entry。 |
| TDD-AUTH-ERR-002 | 撤销超过剩余授权 | 失败且余额无变化。 |
| TDD-AUTH-ERR-003 | 结算超过剩余授权 | 失败且不生成结算 entry。 |
| TDD-AUTH-ERR-004 | 授权退款超过已结算金额 | 失败；已退累计不变化。 |
| TDD-AUTH-ERR-005 | 授权拒绝被当作拒付 | 失败；拒绝不是 `CHARGEBACK`。 |
| TDD-AUTH-ERR-006 | 授权结算触碰 `LIMIT` | 必须失败或红线测试阻断；只有 `LIMIT_ADJUST` 可触碰 `LIMIT`。 |

## 八、余额控制测试矩阵

### 8.1 单场景验证

| 用例 ID | 场景 | 服务入口 | 核心断言 |
| --- | --- | --- | --- |
| TDD-CTRL-001 | 资金账户冻结 | `FundsBalanceControlService#freeze` | 同主体 `AVAILABLE -> FROZEN`；冻结单记录原因、期限、操作者；不创建标准资金交易。 |
| TDD-CTRL-002 | 一次冻结多次解冻 | `unfreeze` | 每次 `FROZEN -> AVAILABLE`；累计不超过剩余冻结；重复请求幂等或明确失败。 |
| TDD-CTRL-003 | 冻结后提现 | `freeze` + `withdraw` | 冻结是来源事实；确认出款才消耗冻结；不是先解冻再无来源扣款。 |
| TDD-CTRL-004 | 冻结失败 | `freeze` | 可用余额不足、账目不支持或主体缺失时失败；余额无变化。 |
| TDD-CTRL-005 | 信用账户额度调增 | `adjust` | `LIMIT` 和 `AVAILABLE` 按规则变化；必须有审批、来源和审计。 |
| TDD-CTRL-006 | 信用账户额度调减 | `adjust` | 校验已授权、已使用和可调下限；失败不改余额。 |
| TDD-CTRL-007 | 预算组预算调增 | `adjust` | 预算 `LIMIT/AVAILABLE` 变化；预算不是资金，不入现金流。 |
| TDD-CTRL-008 | 预算组预算调减 | `adjust` | 不破坏已授权预算；超限失败。 |
| TDD-CTRL-009 | 资金账户余额调整 | `adjust` | 只允许明确差错/审批/凭证下的受控余额调整；跨主体价值转移应走直接交易。 |
| TDD-CTRL-010 | 错币种余额控制 | `freeze/unfreeze/adjust` | 余额控制不做 FX；带换汇意图必须失败。 |

### 8.2 组合场景验证

| 用例 ID | 场景 | 步骤断言 |
| --- | --- | --- |
| TDD-CTRL-FLOW-001 | 充值 -> 冻结 -> 多次解冻 | 每次解冻都断言 `AVAILABLE/FROZEN` delta；超额失败无副作用。 |
| TDD-CTRL-FLOW-002 | 冻结 -> 部分解冻 -> 提现剩余冻结 | 已解冻部分回可用；提现只能消耗剩余冻结来源。 |
| TDD-CTRL-FLOW-003 | 信用账户调额 -> 授权 -> 结算 | 调额触碰 `LIMIT`；授权和结算只触碰 `AVAILABLE/AUTHORIZATION`。 |
| TDD-CTRL-FLOW-004 | 预算组调额 -> 预算授权 -> 撤销 | 预算周期、`LIMIT`、`AVAILABLE`、`AUTHORIZATION` 逐步断言。 |
| TDD-CTRL-FLOW-005 | 资金账户调减触发受控负数 | 无策略失败；有策略保留来源、上限、账龄、审批和治理状态。 |

### 8.3 边界与异常

| 用例 ID | 场景 | 必须证明 |
| --- | --- | --- |
| TDD-CTRL-ERR-001 | 冻结表达消费 | 必须失败；冻结只控制可用性。 |
| TDD-CTRL-ERR-002 | 解冻超过剩余冻结 | 失败且余额无变化。 |
| TDD-CTRL-ERR-003 | 普通授权结算触碰 `LIMIT` | 失败；`LIMIT` 只能由 `LIMIT_ADJUST` 受控调整。 |
| TDD-CTRL-ERR-004 | 余额控制调用 FX | 失败；余额控制不承接 FX。 |
| TDD-CTRL-ERR-005 | 资金调账缺审批或凭证 | 失败；不生成调账 entry。 |

## 九、路由与回放测试矩阵

| 用例 ID | 场景 | 组件 | 核心断言 |
| --- | --- | --- | --- |
| TDD-ROUTE-001 | Composite resolver 选择 | `CompositeRouteResolver` | `supports` 正确委派；不在 supports 中做状态变更。 |
| TDD-ROUTE-002 | 直接交易 route | `TransferFundsInstructionRouteResolver` | topup/pay/transfer/refund/withdraw/fee/refundFee 的参与方、账目、平台账户角色和 route leg 正确。 |
| TDD-ROUTE-003 | 授权 route | `AuthorizationFundsInstructionRouteResolver` | authorize/reversal/settle/refund/chargeback 使用授权语义和原路径。 |
| TDD-ROUTE-004 | 余额控制 route | `BalanceControlFundsInstructionRouteResolver` | freeze/unfreeze/adjust 只表达同主体控制或受控调额。 |
| TDD-ROUTE-005 | route replay | `DefaultRouteReplayService` / resolver replay support | 退款、撤销、结算、拒付、退费、解冻缺快照失败；不得重新选当前路径。 |
| TDD-ROUTE-006 | 外部账户和工具引用 | route support | external account、payment instrument 只进 ref/snapshot，不进 ledger node。 |
| TDD-ROUTE-007 | route code 和上下文稳定性 | route support | 稳定 route code、context key、snapshot schema，不依赖字符串散落。 |

## 十、账本、余额投影与交易投影测试矩阵

| 用例 ID | 场景 | 服务或组件 | 核心断言 |
| --- | --- | --- | --- |
| TDD-LEDGER-001 | posting plan 平衡 | `DefaultLedgerPostingAssembler` | 每个 plan 独立平衡；整笔 ledger transaction 平衡。 |
| TDD-LEDGER-002 | entry 字段完整 | assembler / posting service | ledger、subject、account、currency、period、route leg、phase、intent、digest 可追溯。 |
| TDD-LEDGER-003 | 金额和币种校验 | posting service | 金额为正；币种一致；`new BigDecimal(double)` 类风险不得进入金额模型。 |
| TDD-LEDGER-004 | 缺账本失败 | assembler | 入账路径不自动建账；缺 required ledger 失败。 |
| TDD-LEDGER-005 | ledger transaction 幂等 | `LedgerTransactionService` | 同稳定交易键不重复入账；冲突摘要失败。 |
| TDD-LEDGER-006 | 余额投影更新 | `LedgerBalanceProjectionService` | normal balance 正确；资金账户默认不允许负数；受控负数需策略。 |
| TDD-LEDGER-007 | 余额变更日志口子 | projection event | 日志或观察事件从分录和投影派生；失败不回滚事实；不得反写余额。 |
| TDD-LEDGER-008 | 账本周期隔离 | ledger/profile/assembler | `LIFETIME`、`MONTHLY`、`CUSTOM_CYCLE` 不串账；非生命周期缺 `periodId` 失败。 |
| TDD-VIEW-001 | 交易投影只读 | transaction view projector/boundary | 交易视图来自交易事实、route snapshot 和 entry 摘要；不写账、不修正余额。 |
| TDD-VIEW-002 | 交易投影重放边界 | replay design/boundary | 重放不重新入账；无范围全量重放失败。 |

## 十一、钱包账户测试矩阵

| 用例 ID | 场景 | 服务 | 核心断言 |
| --- | --- | --- | --- |
| TDD-WALLET-001 | 创建资金账户 | `FundingAccountService` | 账户类型、owner、币种、状态、ledger profile 正确；显式初始化账本。 |
| TDD-WALLET-002 | 创建信用账户 | `CreditAccountService` | 信用账户承载额度，不是真实资金账户；`LIMIT/AVAILABLE/AUTHORIZATION` 账目完整。 |
| TDD-WALLET-003 | 创建预算组 | `BudgetGroupService` | 预算组表达预算控制；周期、规则版本和治理字段可审计。 |
| TDD-WALLET-004 | 平台账户角色解析 | `PlatformFundingAccountService` | 按租户、币种、角色唯一解析；缺失或不唯一失败；不自动创建。 |
| TDD-WALLET-005 | 支付工具绑定 | `PaymentInstrumentService` | 工具只做引用和快照；不表达余额；敏感信息脱敏。 |
| TDD-WALLET-006 | 支出主体资金来源关系 | `SpendSubjectFundingRelationService` | 信用、预算、工具到真实资金账户关系可查询；不执行扣款。 |
| TDD-WALLET-007 | 主体余额查询 | `FundsSubjectBalanceQueryService` | 只读余额投影；不初始化账本、不修复余额、不承载历史回放。 |
| TDD-WALLET-008 | Wallet 边界 | boundary tests | wallet 不直接创建资金交易、route snapshot 或 ledger entry。 |

## 十二、清结算、对账、归档与指标 TDD 边界

这些能力是产品全景的一部分，但当前工程推进中曾明确跳过或移除清结算、对账、报表、归档移动和指标实现任务。TDD 设计保留验收方向，不把它们恢复为当前编码 backlog。

| 域 | 当前 TDD 状态 | 后续进入编码前必须补齐 |
| --- | --- | --- |
| 清分/清算 | 产品和系分设计保留；当前不作为自动推进任务。 | 清算候选、批次、排除、确认、重跑幂等、明细追溯和账务影响测试。 |
| 结算/出款 | 产品和系分设计保留；当前不作为自动推进任务。 | 结算单、锁定、外部回单、出款成功/失败、追偿、审批和审计测试。 |
| 对账/差错 | 产品和系分设计保留；当前不作为自动推进任务。 | 批次、数据源、口径、匹配规则、差异分类、差错单、调账核销和重跑测试。 |
| 归档/余额重建 | 产品和系分设计保留；当前仅保留边界测试。 | checkpoint、watermark、Manifest、冷热查询、余额重建和差异报告测试。 |
| 交易投影重放 | 保留只读边界和无范围失败红线。 | 单笔、有界批量、dry-run/apply、差异报告和幂等测试。 |
| 报表指标 | 仅列指标项和只读红线。 | 指标口径版本、指标水位、质量报告、失败不推进和发布审批测试。 |

## 十三、必须失败红线测试

| 红线 ID | 场景 | 建议落点 |
| --- | --- | --- |
| TDD-RED-001 | 外部账户、VA、银行卡或支付工具被当作 ledger subject。 | route/ledger boundary tests。 |
| TDD-RED-002 | 业务系统、后台、投影或余额日志直接改余额。 | boundary tests、projection tests。 |
| TDD-RED-003 | 缺 route snapshot 的退款、撤销、结算、拒付、退费或解冻重新选路。 | replay tests。 |
| TDD-RED-004 | 任一 posting plan 不平衡仍写账。 | ledger posting tests。 |
| TDD-RED-005 | 授权拒绝生成 route、posting、entry 或 `chargebackAmount`。 | authorization command/flow tests。 |
| TDD-RED-006 | 冻结单状态被用来表达消费、扣划或付款完成。 | balance control tests。 |
| TDD-RED-007 | 商户订单款直入 `AVAILABLE` 或 `SETTLEMENT`。 | direct pay route/flow tests。 |
| TDD-RED-008 | 普通授权结算触碰 `LIMIT`。 | authorization flow and shared card tests。 |
| TDD-RED-009 | 余额控制承接 FX 或自动调用 `FxService`。 | converter/balance control tests。 |
| TDD-RED-010 | 投影、交易视图或报表反写资金事实。 | projection boundary tests。 |
| TDD-RED-011 | 非 `LIFETIME` 周期缺 `periodId` 仍入账或查询。 | ledger period tests。 |
| TDD-RED-012 | 受控负余额缺策略、上限、来源或审批仍继续消费。 | direct/balance/authorization tests。 |
| TDD-RED-013 | 清结算、对账、归档、指标任务在未重新设计前进入当前编码 backlog。 | 文档/任务审查。 |

## 十四、现有测试资产索引与缺口

| 领域 | 已有资产 | 当前判断 |
| --- | --- | --- |
| DSL 契约 | `FundsInstructionSpecContractTests`、`RouteDslContractTests`、`TransactionServiceAbilityDslJsonContractTests`。 | 已有基础，后续新增 JSON 契约时同步补充。 |
| 直接交易 | `FundsTransactionCommandServiceImplTests`、`FundsTransactionFeeCommandServiceImplTests`、`FundsTransactionBusinessFlowIntegrationTests`、`FundsTransactionFeeBusinessFlowTests`。 | 已覆盖主线；提现撤销/拒绝和提现手续费应作为后续回归重点。 |
| 授权交易 | `FundsAuthorizationTransactionCommandServiceImplTests`、`FundsAuthorizationBusinessFlowTests`、`FundsSharedCardAuthorizationBusinessFlowTests`。 | 已覆盖主线；继续强化拒付费用和共享卡边界。 |
| 余额控制 | `FundsBalanceControlCommandServiceImplTests`、`FundsBalanceControlBusinessFlowTests`、`DefaultFundsFrozenOrderLifecycleSaverTests`。 | 已覆盖主线；继续强化 LIMIT 红线和提现拒绝解冻链路。 |
| 路由回放 | `DefaultRouteReplayServiceTests`、`DefaultRouteReplayDirectRefundTests`、`DefaultRouteReplayAuthorizationTests`、`RouteReplaySupportTests`。 | 已有；后续保持 replay 属于 resolver/support 能力，不独立扩散服务概念。 |
| 账务与投影 | `DefaultLedgerPostingAssemblerTests`、`DefaultLedgerTransactionPostingServiceImplTests`、`LedgerBalanceProjectionServiceImplTests`、`FundsTransactionLedgerBalanceAssertionsTests`。 | 已有；新增资金场景必须复用余额断言 support。 |
| 钱包账户 | `FundingAccountServiceImplTests`、`CreditAccountServiceImplTests`、`BudgetGroupServiceImplTests`、`PlatformFundingAccountServiceImplTests`、`WalletLayerBoundaryTests`。 | 已有；继续保护 wallet 不承载交易命令。 |
| 清结算/对账/归档/指标 | 部分历史边界测试和设计草案存在。 | 不作为当前有效 backlog；后续重新设计后再生成正式测试任务。 |

## 十五、模块测试场景总表

| 模块或能力 | 单场景验证 | 组合场景验证 | 边界场景验证 | 异常或非预期场景验证 |
| --- | --- | --- | --- | --- |
| DSL 契约 | 指令、route、posting、entry、replay、SettlementPolicy、JSON 样例可解析。 | 产品用例映射到 DSL 场景，组合用例可生成稳定契约样例。 | 金额为正、币种明确、非 `LIFETIME` 周期有 `periodId`、快照版本稳定。 | 指令类型错用、缺引用、缺快照、策略解析失败、JSON 字段缺失必须失败。 |
| 直接交易服务 | topup、pay、transfer、withdraw、refund、fee、refundFee 单独验证。 | 充值-付款-退款、充值-付款含手续费-退费、充值-冻结-提现、A 转 B 后付款提现。 | 同主体转账、错币种、受控负余额、提现先冻结、费用独立 leg。 | 幂等冲突、余额不足、缺平台账户、退款/退费超额、提现成功后再撤销。 |
| 授权交易服务 | authorize、reversal、settle、settleRefund、chargeback 单独验证。 | 授权-撤销-结算-退款、共享卡授权-结算-剩余撤销、结算后拒付和费用。 | 授权拒绝无账务、普通结算不触碰 `LIMIT`、预算和信用账户不表达真实资金。 | 撤销超额、结算超额、授权退款超额、授权拒绝误写拒付、缺原快照。 |
| 余额控制服务 | freeze、unfreeze、adjust 单独验证。 | 冻结多次解冻、冻结后提现、信用调额后授权、预算调额后授权。 | 冻结只做同主体 `AVAILABLE <-> FROZEN`，`LIMIT` 只能调额触碰，余额控制无 FX。 | 超额解冻、冻结表达消费、调账缺审批、错币种余额控制、无策略负余额。 |
| 路由与回放 | direct、authorization、balance-control resolver 单独验证。 | 原路径退款、授权撤销/结算/退款、退费、解冻等 replay 组合。 | `supports` 只判断不改状态，外部账户和工具只进引用，平台角色必须解析为具体账户。 | 缺 snapshot 重新选路、route code 漂移、resolver 选错、外部账户入账。 |
| 账务与余额投影 | assembler、posting service、projection service 单独验证。 | 交易入账后同步断言 posting、entry、projection 和余额日志观察事件。 | posting plan 独立平衡、entry digest 稳定、账本周期隔离、受控负数策略。 | 不平衡仍写账、缺账本自动建账、projection 反写事实、日志失败回滚余额。 |
| 钱包账户 | funding、credit、budget、platform account、instrument、balance query 单独验证。 | 账户初始化后参与交易、授权、冻结、余额查询组合。 | 钱包只管理账户和工具，支付工具不表达余额，平台角色唯一解析。 | wallet 直接写交易或账本、交易路径自动建账、余额查询修复事实、敏感信息未脱敏。 |
| 交易投影 | 单笔交易视图生成和查询。 | 资金交易、授权、冻结、退款形成用户、商户、运营视图。 | 交易投影只读，来源为交易事实、route snapshot 和 entry 摘要。 | 投影写账、投影修正余额、无范围全量重放、用视图反推余额。 |
| 清结算、对账、归档、指标 | 仅保留产品和系分验收方向。 | 后续重新设计后再补完整组合链路。 | 当前不得混入交易/钱包/账本主线 backlog。 | 未重新设计就恢复实现、测试或公共契约时，审查必须阻断。 |

## 十六、测试用例书写规范

每个核心测试方法上方应说明以下内容。业务组合测试、红线测试和资金变化测试必须写完整；纯 helper 测试可按需裁剪。

```text
场景：
角色：
前置条件：
输入：
动作：
期望状态：
期望账目变化：
期望账本事实：
期望投影：
幂等断言：
异常断言：
权限和审计断言：
红线：
```

测试实现要求：

1. 金额使用最小货币单位或项目 Money 类型，不使用浮点。
2. 不从展示文本、日志、交易视图或报表反推余额。
3. 断言缺失账目时默认失败，除非该用例明确是展示查询并断言未初始化。
4. 业务组合测试每一步都保存 before/after balance snapshot 并断言 delta。
5. 对内部流程测试，优先使用真实 Spring bean 或真实对象组合；外部通道、时间、ID、不可控基础设施才使用 fake/stub。
6. Mock 只用于隔离外部依赖，不用于掩盖 route、posting、projection、lifecycle 的真实执行路径。

## 十七、验证命令

文档变更只需做 Markdown 差异检查。代码或测试变更应按范围执行：

```bash
just mvn-version
just compile
just test-core
just test-ledger
just test-transaction
just test-balance-control
just test-business-flow
just test-boundary
just test-one <TestClass> [module]
```

按测试域可使用：

| 测试域 | 建议命令 |
| --- | --- |
| DSL 契约 | `just test-core` |
| Ledger | `just test-ledger` |
| 交易门面和编排 | `just test-transaction` |
| 余额控制 | `just test-balance-control` |
| 业务组合 | `just test-business-flow` |
| 模块边界 | `just test-boundary` |

提交前优先执行 `just pmd`。如果 Maven 因私有仓库、网络、凭据或本地缓存失败，应在交付说明中区分环境问题和代码问题。

## 十八、进入编码前检查清单

| 检查项 | 通过标准 |
| --- | --- |
| 产品场景 | 能映射到产品验收 ID 或本文 TDD 用例 ID。 |
| DSL 事实 | 能说明 instruction、event、amount、reference、route、posting 和 entry。 |
| 服务入口 | 能落到具体 face service、converter、resolver、orchestrator、assembler 或 ledger/wallet service。 |
| 测试层级 | 已确定 L1/L2/L3/L4/L5 中至少一个落点。 |
| 正常路径 | 有成功状态、余额桶、posting、entry、投影和幂等断言。 |
| 异常路径 | 有失败原因和无副作用断言。 |
| 红线 | 涉及金融红线时有 must-fail 测试或明确不适用原因。 |
| 现有资产 | 复用已有 support 和测试包名，不重复造一套测试框架。 |
| 当前版本边界 | 清结算、对账、归档、指标类任务不混入当前交易/钱包/账本主线，除非重新完成产品和系分设计。 |
