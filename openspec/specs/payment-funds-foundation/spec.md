# 支付资金底座开发基线规格

## 一、目标

本规格把当前产品设计、DSL 设计、系分设计和 TDD 设计压缩成后续编码的最小稳定基线。它不是历史变更记录，也不是任务完成证明。

后续开发必须围绕一个核心目标推进：

> 每一笔资金事实都能被解释、被核对、被重建，并能通过 TDD 证明状态、账目、route、posting、ledger entry、投影、幂等和红线均成立。

## 二、范围

### 2.1 能力保障优先级

本规格的范围按资金底座能力优先级组织。优先级不等于一次性编码授权；任何生产代码、测试代码、DDL/H2 schema 或运行时配置变更仍必须由独立 Execution Grant 明确写入范围、验证命令和停止条件。

| 优先级 | 域 | 规格范围 |
| --- | --- | --- |
| P0 | DSL 契约与账务内核 | `FundsInstruction`、`eventType`、`transactionType`、金额临界值、引用、route、payment instrument、routing decision、funding allocation、posting、entry、`SettlementPolicy`、JSON 契约。 |
| P0 | 钱包账户 | 资金账户、信用账户、预算组（预算控制对象）、Spend Rule、平台账户角色、支付工具、支付工具绑定、支出主体资金责任解析关系、主体余额查询。 |
| P0 | 账务与余额投影 | posting plan、ledger transaction、ledger entry、余额投影、余额日志、账本余额快照覆盖模式和只读边界。 |
| P0 | 清结算与对账 | 可清分明细、清分批次、清算候选、清算批次、结算单、出款单、对账批次、差错单、追偿单和调账核销。 |
| P0 | 资金数据治理 | 事实留存门禁、Manifest、checkpoint、watermark、余额重建、账本余额快照、差异报告、异常人工处理、大数据消费边界、治理读取或导出快照、脱敏、digest 和审计；产品 04 仅保留拆分索引，具体口径由产品 02、03、05 承接。 |
| P1 | 直接交易 | 充值、付款、转账、提现、退款、手续费、退费、受控负余额。 |
| P1 | 授权交易 | 授权批准、授权拒绝、授权撤销、授权完成、授权过期、授权链退款、无授权直接退款、拒付承接口径、`settle` 强制完成模式。 |
| P1 | 余额控制 | 冻结、解冻、资金账户余额调整、信用账户额度调整和预算控制额度 / 窗口调整；预算控制调整只写控制活动和预算控制投影，不生成 route、posting、LedgerEntry 或账本余额；对账差错调账必须经过差错单、审批、凭证、审计和重新对账闭环，不得被简化成无来源的余额直接修改。 |
| P1 | 路由、回放和交易投影 | route resolver、route snapshot、Route Replay、缺快照失败、账本周期继承、交易投影和交易投影重放只读边界。 |
| P1 | 权益金额组件 | `FundsBenefitFundingApplicationService`、权益让利资金交易请求、权益来源引用、商户券、平台补贴、储值券、授权占券、不退券、部分退款和缺权益资金事实逆向处理；历史 `benefitSnapshotId` 和 `stableDigest` 仅作为只读摘要追溯字段。 |
| P2 | 业务能力包 | VCC 发卡、全球账户收付款和收单业务只作为资金底座上层业务模式支持；资金底座承接归一资金事实、账户、账本、清结算、对账和归档，不内置轨道协议、风控模型、商户经营或合规结论。VCC 卡、虚拟卡、卡 token、prepaid virtual card 和 shared card 先作为支付工具、资金模式或绑定模式输入，不新增资金账户、账本主体或余额投影主体。ACH 或银行转账只作为这些业务可能使用的外部轨道输入，不新增资金底座内建业务能力。 |
| 横切 | 使用者可解释性 | 用户账单、商户账单、运营时间线、财务对账视图、审计导出和 SRE Runbook 信号；只读解释资金事实、事实状态、展示状态、操作状态、状态含义、失败原因、处理入口和不可操作原因，不反写交易事实、账本事实、余额投影或清结算对象。 |

### 2.2 MVP 任务授权边界

P0、P1、P2 仍按能力优先级组织，但进入编码时不按大批次一次性授权。每次只能选择一个 MVP 任务切片，写清 `mvpScenario`、AC/DSL/TDD/RED、写入范围、禁止范围、资金不变量和验证命令。B1 至 B8 只作为 TDD 覆盖索引和历史任务编号，不表示 03、04 能力低于 02，也不表示任一编号可以越过独立 Execution Grant。

| 任务切片 | 能力优先级 | 授权口径 |
| --- | --- | --- |
| A0 | P0/P1/P2 准入核验 | 只读复核设计、OpenSpec、任务、代码、测试、H2 schema 和工作树；不写生产代码、测试代码或 DDL/H2。 |
| A1 至 A4 | P0/P1 最小闭环 | DSL、钱包、账本、账目、余额投影、直接交易、授权交易、余额控制、路由回放、交易投影和权益资金流必须按单一场景授权。 |
| B7 独立任务 | P0 运营账务闭环 | PRD、DSL、系分和 TDD 已可作为系分交付输入；清结算与对账编码必须另起授权，不混入交易入口实现。 |
| B8 独立任务 | P0/P1 治理闭环 | 事实留存、大数据消费边界和账本余额快照是 P0；交易投影重放是 P1 读模型治理；二者必须明确范围、水位、Manifest 和差异处理。 |
| 业务专项 | P2 能力包 | VCC、全球账户和收单业务只通过专项 PRD/系分/TDD 使用资金底座能力。 |

P2 业务专项进入编码前必须单独补 Execution Grant。授权中至少写明：对应业务分册、业务验收 ID、场景 pack 范围、归一资金动作、外部引用脱敏字段、允许新增或修改的公共契约、是否触碰 DDL/H2 schema、P0/P1 回归测试、外部规则核验状态、`TDD-P2-*` 专项用例和 Not Done 红线。缺任一项时，P2 业务只能停留在产品、DSL、系分、TDD 或 contract-only 验证，不得声明业务生产资金流 Done。

ACH 或银行转账不作为 P2 新增业务专项默认进入资金底座。若 VCC、全球账户、收单或平台出款后续使用 ACH 轨道，Execution Grant 必须证明 ACH 业务或通道适配层已经完成业务归属、指令、授权、文件、批次、return、NOC、reversal、外部受理状态、敏感数据最小化和外部规则解释；资金底座只允许承接归一资金事实、外部引用、对账差错、追偿、调账核销和审计，不得在 `core`、`transaction-*`、`wallet-*` 或 `ledger-*` 中实现 ACH 协议、Nacha 规则或银行账户敏感数据能力。

ACH 轨道进入任何 MVP 编码任务前必须回挂产品 `ACH-BOUNDARY-001` 至 `ACH-BOUNDARY-006`、`AC-RAIL-002A` 至 `AC-RAIL-007`、DSL P2 业务能力包准入卡和 TDD `TDD-RAIL-002` 至 `TDD-RAIL-007`。缺少上层解释、外部规则核验状态、脱敏字段白名单、回单或文件摘要边界、return/NOC/reversal 处理责任方、或 submitted/accepted/processing 与到账成功的展示隔离时，只能停留在产品、DSL、系分、TDD 或 contract-only 验证，不得声明生产资金流 Done。

| 业务专项 | 可复用的资金底座能力 | 进入编码前必须证明 | Not Done 红线 |
| --- | --- | --- | --- |
| VCC 发卡 | 授权交易、支付工具、route snapshot、账本账目、清结算、对账、归档和审计。 | 卡、token、持卡人和 processor account 只作为脱敏工具或外部引用；授权拒绝无账务副作用；clearing、refund 和 chargeback 基于原授权或原 route。 | 完整 PAN/CVC 进入资金底座；VCC 卡被当作 ledger subject；授权拒绝生成 route、posting 或 ledger entry。 |
| 全球账户收付款 | 直接交易、外部账户引用、出款前准入、IN_TRANSIT、费用和 FX 引用、对账、归档和审计。 | VA 或银行流水匹配后才入账；外部 accepted 不等于到账；退汇关联原出金；无 FX quote 不静默换汇。 | 外部银行账户、VA、Nostro/Vostro 成为 ledger subject；外部受理被展示为到账成功；错币种无 quote 仍入账。 |
| 收单业务 | capture 归一资金动作、商户 CLEARING、清分、清算、结算、出款、退款、争议、对账和归档。 | capture 只进入待清算；清分、清算、结算和出款分层；refund 与 chargeback 防重复损失；敏感卡数据最小化。 | 支付成功直接展示可提现；清分确认释放可结算；chargeback 被普通退款吞掉；完整 PAN/CVC 进入日志、投影、导出或测试夹具。 |

## 三、非目标

1. 不恢复历史过程规格、旧 Harness 计划或旧测试断言。
2. 不在资金底座内实现报表指标模块内部计算、调度、存储和展示。
3. 不把通道协议、卡组织规则、ACH/Nacha 规则、银行协议、ACH 文件批次、Debit 授权、return code/NOC/reversal 解释或合规结论写成默认实现。
4. 不把退款待处理、业务取消、未来时间事件、乱序事件等上层业务编排场景作为资金底座默认 DSL 场景。
5. 不在没有独立系分和 TDD 的情况下把清结算、对账、归档、指标实现混入交易、钱包、账本主链路。
6. 不在资金底座内实现营销规则计算、最优券选择、用户券包生命周期、活动状态查询或权益库存管理。

## 四、统一术语

| 概念 | 基线口径 |
| --- | --- |
| `transactionType` | 稳定资金业务类型：`TOPUP`、`TRANSFER`、`PAY`、`FEE`、`REFUND`、`WITHDRAW`、`ADJUSTMENT`。不得放生命周期事件。 |
| `eventType` | 生命周期事件：直接交易、授权、冻结、调账等具体事件。 |
| `SETTLEMENT_LOCK` | 结算锁定事件。当前基线不新增 `SETTLEMENT` 类 `transactionType`，目标态通过 `DIRECT_TRANSACTION / ADJUSTMENT` 承载，但必须以 `eventType=SETTLEMENT_LOCK`、清结算上下文和结算操作类型隔离，不得归入人工调账权限、报表或核销口径。 |
| `IN_TRANSIT` | 外部已受理但未最终成功或失败的账本可见在途桶，必须保留外部引用、责任方、账龄和到期重查口径；未启用账本在途桶时，只能保持出款单待确认。 |
| 冻结动作明细 | 冻结单主表只保存聚合金额和当前状态；冻结、解冻、过期释放和后续资金事实关闭冻结来源都必须有动作明细、动作幂等键、前后剩余冻结金额和账本/资金事实引用。 |
| 出款单唯一性 | 当前基线默认一张结算单只生成一张出款单；重复创建必须命中原出款单或失败。若要支持拆分出款，必须先补出款明细和拆分幂等模型。 |
| 出款前准入门禁 | 结算锁定后、提交外部出款前必须完成出款账户、收款端点、通道额度、cutoff、名单筛查、外部规则核验、负余额、准备金、对账差错、幂等和审批校验；任一门禁缺失、失败或未知时不得提交外部出款，不得生成 `FUND_OUT` 或 `IN_TRANSIT` 资金事实。 |
| 外部非终态出款 | 外部 `accepted/submitted/message sent/processing` 只表示受理或处理中，不等于到账成功；不得提前关闭 `SETTLEMENT/IN_TRANSIT`，必须等待成功回单、到账证明或对账确认。 |
| 运营补事实命令白名单 | 清结算或对账通过交易层追加资金事实时，Execution Grant 必须列出允许命令或等价事件、来源单据、审批号、证据引用、幂等键、原事实引用、操作者、原因和可撤销边界；未列入白名单的运营动作不得生成 `FundsInstruction`、route、posting 或 ledger entry。 |
| 权益退款分摊确定性规则 | 部分退款必须明确规则版本、分摊依据、稳定组件顺序、舍入模式、尾差归属、组件剩余额度版本、幂等摘要字段和并发保护；缺任一项时只能失败、转人工或停留在 contract-only。 |
| 外部规则核验状态 | 依赖税务、会计、合同、卡组织、银行、通道、KYC/KYB/AML、客户资金、跨境或外汇口径时，必须保存规则来源、版本或发布日期、生效日期、适用主体或适用范围、适用法域、核验日期、确认方和确认状态；缺失时只能阻断、降级或转人工。 |
| 授权撤销 | 外部明确撤销或冲正，事件使用 `REVERSAL`，终态可区分为撤销。 |
| 授权过期 | 系统按有效期释放剩余授权，事件使用 `EXPIRE`，终态可区分为过期。 |
| 授权完成 | 使用 `AUTHORIZATION_TRANSACTION / SETTLE`，表示授权占用转实际消费或清算结果。 |
| 强制完成 | 使用 `settle` 的强制完成模式，不新增独立事件，不伪造授权占用。 |
| 无授权直接退款 | 无前置授权但存在可追溯外部引用时，使用 `settleRefund` 的 no-auth 退款模式承接；以空原授权流水进入 no-auth 语义，必须携带 `externalReferenceSn`、退款原因、操作者和审计，普通授权链退款仍走原完成事实；不得补造内部授权占用、携带或查询内部授权流水、按当前绑定重选路或静默退款。 |
| 授权链拒付 | 表达已完成授权后的争议、扣回或追偿语义；资金底座目标态不要求落到 `FundsAuthorizationTransactionService#chargeback`，默认通过 `settleRefund` 携带拒付原因、凭证、上下文和审计承接。即使底层终态复用退款终态，也必须保留可查询、可投影、可审计的拒付语义，不能只留下普通退款结果。 |
| 授权支付工具应用入口 | 面向 VCC、卡、VA、共享卡、外部钱包端点或通道 token 的授权入口，可以接收支付工具引用和业务上下文；应用层必须先解析工具状态、绑定版本、使用主体、预算组、Spend Rule、资金责任和账户能力，再委派账户主体型授权内核。内部钱包、信用额度或权益入口先解析为 `SubjectRef`、权益资金事实引用或历史摘要。当前账户主体型 `authorize` 请求不得在未授权时被直接替换为工具引用字段。 |
| 支付工具交易入口 | 资金底座不建设统一的支付工具交易内核，也不把 `FundsAuthorizationTransactionService.authorize`、直接交易或余额控制 canonical 请求整体替换为支付工具引用。支付工具入口只位于 application facade，例如 `authorizeByInstrument` 或等价入口。VCC 预付卡充值、系统内余额钱包充值、共享卡调额、VA 收款、全球账户付款、ACH/银行转账事件必须先由 P2 业务能力包解释为归一资金事实，再映射到直接交易、授权交易、余额控制、清结算、对账调账或归档能力。共享卡、预付卡可形成 VCC 交易服务能力包，但该能力包只负责编排工具准入、资金责任、原路径回放和投影输入，不拥有新的账本主体或交易内核。 |
| 预付卡、共享卡交易服务能力包 | 预付卡能力必须遵循“资金先确认、后授权使用”：外部入金、系统内充值或退卡转出只有在确认引用、幂等摘要和内部责任主体唯一时才能委派到账户主体型交易；共享卡能力必须遵循“账户入账、工具归因”：授权、清算、释放、退款和拒付都固化工具快照、绑定版本、预算组上下文、Spend Rule 决策和资金责任决策。 | 首期只能作为 P2 独立 Execution Grant 切片进入，建议拆为 `P2-VCC-PREPAID` 与 `P2-VCC-LIFECYCLE`；不得与 B2/B4/B5/B6/B8 一次性混合授权。 |
| 支付工具 | 卡、VA、外部账户、虚拟卡、外部钱包端点或通道 token 的路由输入和审计快照；不表达内部余额，不作为账本主体。内部余额钱包、平台钱包、商户钱包、返利钱包或信用额度入口不是支付工具，必须解析为资金账户、信用账户、平台账户角色解析后的平台资金账户、权益让利资金事实或资金责任决策后才能进入 route leg、posting 或 ledger entry。预算组和 Spend Rule 只作为控制上下文或规则快照。 |
| Highnote 参考模式 | Highnote 的 financial account / ledger / payment card / financial account activity 只作为外部设计参考：账户承载资金和账本，卡承载访问工具，activity 或 transaction feed 承载卡维度归因。wind-funds 采纳“账户入账、工具归因、控制留痕、投影查询”的分层，不照搬对象名，不新增卡账本、支付工具账务主体或统一支付工具交易内核。共享卡或多卡共享账户时，通过同一内部责任主体、多工具绑定、控制快照和交易投影过滤区分卡账单；只有独立资金池、独立授信、独立账期、独立还款或独立资金责任成立时，才创建独立资金账户或信用账户。 |
| 支付工具绑定 | 工具和付款主体、收款主体、信用账户、预算组或真实资金账户之间的候选关系；只用于路由候选和快照，不直接入账。 |
| 支出主体资金责任解析关系 | 支出主体、支付工具、信用账户、预算组或 Spend Rule 上下文到最终内部资金或额度责任主体的解析关系；最终结果只能是资金账户、信用账户或平台账户角色解析后的平台资金账户。不计算 spend rules，不执行扣款，不写分录；预算组和 Spend Rule 只提供 scope、规则条件、规则版本和审计解释，不能作为资金池或 `LedgerEntry` 主体。`FundingAccount` 只表示真实资金账户，不得泛化承载信用账户、预算组、支付工具或钱包标识。历史代码、表名或服务名可继续使用 funding relation 兼容命名，但规格语义以资金责任解析为准。 |
| 支付工具与 Spend Rule 生产可用性 | 支付工具资源管理、绑定历史和 DSL 契约只能证明局部基线。生产可用必须另外证明 application facade 准入、资金责任唯一决策、账户能力来源、预交易快照、Spend Rule 规则版本和决策证据、Spend Control Activity、预算控制投影、失败无 route/posting/entry、只读投影重放和相关测试证据闭合。`GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001` 已证明支付工具预交易快照可组合工具能力、资金责任和账户能力；`GSD2-B2-SPEND-CONTROL-ADMISSION-001` 已证明 wallet application facade 能消费外部 Spend Rule 决策证据并输出支出控制准入快照，且无资金事实副作用。缺完整规则定义、决策日志持久化、控制活动和预算投影证据时，不得声明预算控制或 Spend Rule 引擎生产可用。 |
| 支付工具与 Spend Rule Round 0 | 支付工具应用准入、资金责任解析、授权支付工具入口、Spend Rule 控制和只读投影必须拆成独立切片评审。Round 0 可以使用 `B2B4-支付工具与SpendRule生产可用性Round0准入卡` 作为输入，但只能输出是否建议进入某一个单一 Execution Grant；不得一次性授权 B2、B4、B5、B6 和 B8 全部目标态。B2-FR 已在 `GSD2-B2-FR-TARGET-001` 首轮选择 `targetSubjectType + targetSubjectId` 并完成资源关系服务流 Green；支付工具能力准入、账户能力来源、预交易快照和支出控制准入快照已完成首轮 Green。后续完整 Spend Rule 控制活动、预算控制投影和 B6/B8 仍必须限定交易投影输入来源，不能把投影通过写成账务事实、余额事实或生产 Done。 |
| 权益让利资金事实 | `FundsBenefitFundingApplicationService` 及其 `settle/refund` 请求，只承接业务侧、订单侧或营销权益系统已决策的权益结果；请求模型仅保留业务流水、原订单或原交易引用、承担方、受益方、金额、资金性质、账务效果和来源引用等最小资金事实；`settle` 表达已确认入账，`refund` 统一承接退款、业务取消、人工纠错或反向冲销；不计算券规则，不判断券是否可用，不维护券生命周期。 |
| 权益金额组件 | 单个商户让利、平台补贴、代金券核销、储值券抵扣或合作方补贴金额项；必须标记闭合角色、账务效果、资金性质、承担方、受益方和退款处置。 |
| `NO_LEDGER` 权益 | 商户让利、展示优惠等无独立资金流组件，只能进入快照、清分展示、对账解释或投影辅助，不得生成 route leg、posting 或 ledger entry。 |
| 平台补贴 / 储值代金券 | 平台补贴可能形成平台自有资金支出；储值、预付或礼品卡代金券可能涉及负债、预收待付或用户权益余额，必须有专业口径确认后再入账。 |
| 不退券 | 用户侧处置和资金侧处置必须分开表达：用户可以不返券，资金侧可以冲回补贴、保留补贴、减少商户应收或恢复储值负债。 |
| 账本周期 | `periodType + periodId`，是余额 bucket 隔离键，不是清算账期、结算周期、报表周期、归档水位或 spend-rule window。 |
| 余额日志 | 从分录和余额投影派生的观察记录，不是余额事实源，不得用于修复余额。 |
| 广义指标快照 | 对某个范围、口径、时间边界和事实来源进行批量计算、校验和确认的任务形态。普通指标快照归报表指标模块；账本余额快照是特殊场景，确认的是账本 bucket 余额事实。 |
| 账本余额快照覆盖模式 | `HOT_ONLY` 表示只覆盖热区分录；`COLD_MANIFEST` 表示只覆盖已归档冷区；`MIXED` 表示同时覆盖冷区 Manifest 和热区游标。冷区和混合覆盖缺 Manifest 不得进入 `VERIFIED`。 |
| 普通指标快照 | 报表指标模块的发布和质量上下文，只影响指标查询、看板、导出或订阅；不得推进余额水位、修改归档 Manifest、替代交易投影重放 checkpoint 或证明余额正确。 |
| 大数据消费边界 | 报表数仓、离线指标或经营分析只能通过治理读取、导出快照、Manifest 摘要、脱敏、digest 和审计消费资金历史事实；资金冷归档是事实留存和重放证据，不是在线报表库，不得被反写或用来推进资金水位。 |
| 可解释输出 | 面向用户、商户、运营、财务、审计和 SRE 的只读解释结果，来自资金交易、route snapshot、ledger entry、余额投影、交易投影、批次、差错、审批和告警上下文；不能作为资金事实源，也不能用于补造或修正资金事实。 |

## 五、当前代码差距基线

以下差距用于指导后续 TDD 任务切片，不在本规格中直接修改代码。

### 5.1 代码能力对齐

| 覆盖索引 | 当前代码和测试基线 | 对齐结论 |
| --- | --- | --- |
| B1 覆盖索引：DSL 契约 | 已存在 `FundsInstructionDslContractTests`、`RouteDslContractTests`、`PaymentInstrumentRouteDslContractTests`、`PostingLedgerDslContractTests`、`SettlementPolicySpecTests`、`FundsAmountBoundaryContractTests`、`FundsDslJsonContractTests`。 | DSL、Route、PaymentInstrument Route、Posting/Ledger、金额临界值、权益资金事实和 JSON 契约已有测试基线；仍需按后续 MVP 任务发现的公共契约缺口补 Red 用例。 |
| 权益资金事实 DSL | 设计权威已统一到 `docs/DSL设计/支付资金底座DSL承载层设计.md`；目标态为 `FundsBenefitFundingApplicationService` 的 `settle/refund`、权益资金来源引用和通用请求摘要。`GSD2-BENEFIT-LEGACY-SNAPSHOT-REMOVE-001` 已删除旧 `FundsBenefitSnapshotSpec`、组件、引用、退款策略、稳定摘要对象、JSON 支撑和旧夹具；JSON 契约拒绝 `instruction.benefitSnapshot`，历史 `benefitSnapshotId` 与 `stableDigest` 仅可作为只读摘要。 | 当前只能按验证结果声明权益资金事实契约和 `POSTING_REQUIRED` 首个资金流切片；不能声明非入账权益事实、授权占券、清结算、对账、投影、归档、冷热读取或治理重放生产链路已闭合。后续含权益 MVP 任务必须补 Phase/Batch、`fixtureLevel`、零实付表达、事实源、独立伴随指令原子性、储值/预付口径、退款分摊粒度、退款分摊确定性规则、历史无权益资金事实处理、补充权益事实模型、专业确认状态、审计证据包、使用者解释视图、证据最小化和外部规则核验状态准入。 |
| B2 覆盖索引：钱包账户与账本基础 | 已存在 `FundingAccountServiceImplTests`、`ControlAccountLedgerInitializationTests`、`PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests`、`LedgerBalanceProjectionServiceImplTests`、`DefaultLedgerPostingAssemblerTests`。 | 支付工具、绑定历史、资金责任解析关系（历史服务名仍为 funding relation）、资金账户/信用账户显式账本初始化、账务计划装配器长 ID 追溯和投影 afterCommit 已有局部基线；`BudgetGroupService#createBudgetGroup` 已收敛为只创建预算控制对象，不自动初始化 ledger bucket；账本周期、全量余额断言和组合交易仍需后续 MVP 任务继续闭合。 |
| B3 覆盖索引：直接交易 | 已存在 `FundsDirectTransactionFlowTests`、`FundsTransferPayWithdrawChainFlowTests`、`FundsTransactionFeeFlowTests`。 | 直接交易主链路、退款、手续费和链式流程已有局部流程基线；仍需按 TDD 覆盖全部 `AC-IN/OUT/PAY/MER/FEE` 和红线失败。 |
| B4 覆盖索引：授权交易 | 已存在 `FundsAuthorizationTransactionFlowTests`，覆盖授权拒绝、撤销、部分完成、全额完成、退款超额、授权过期释放、强制完成和无授权直接退款等局部场景；`b0666ba feat: 补齐授权过期释放 canonical 能力` 已补齐 `EXPIRE` 事件、`expire` 服务入口、请求模型、route replay、生命周期和授权过期释放测试；`616dac1 feat: 补齐授权强制完成能力` 和 `3825466 fix: 收紧授权强制完成策略红线` 已补齐 FORCE 模式、受信策略、上限、原因、外部原事实、凭证、审计、普通完成分支隔离和失败无副作用；B4-NO-AUTH-REFUND 已补齐 no-auth 退款模式、`externalReferenceSn`、退款原因和失败无副作用测试。 | 授权批准、拒绝、撤销、普通完成、完成后退款、授权过期释放、强制完成和无授权直接退款首轮已有局部基线；后续按 B4 覆盖索引闭合拒付承接口径、发卡控制扩展边界、授权占券和权益生命周期、并发竞争。 |
| B5 覆盖索引：余额控制 | 已存在 `FundsBalanceControlFailureFlowTests`、`FundsWithdrawalSuccessFlowTests`、`FundsWithdrawalAfterPartialUnfreezeFlowTests`、`FundsWithdrawalRejectionFlowTests`、`BudgetControlLimitAdjustmentApplicationServiceTests`。 | 冻结、解冻、提现、失败无副作用和部分组合路径已有局部基线；预算控制额度调整已具备控制活动 / 预算控制投影替代入口，不再把新增能力目标落到预算组 ledger control entry；资金账户余额调整、信用账户额度调整、adjust 红线、冻结关闭并发和全量金额临界值仍需补齐。 |
| B6 覆盖索引：Route Replay 与投影 | 已存在 `DefaultRouteReplayServiceTests`、`CompositeRouteResolverTests`、`DefaultRoutedFundsInstructionOrchestratorProjectionTests` 和交易投影解释服务流测试。 | Route Replay、resolver 无副作用、交易投影 afterCommit、投影失败不回滚事实、支付工具授权入口的 `paymentInstrumentRef` 解释和收款工具入口的 route snapshot 工具引用回链已有局部基线；支付工具换绑后全链路回放、完整敏感快照、绑定历史审计、余额日志边界和投影重放全量覆盖仍需按 B6 覆盖索引闭合。 |
| B7 覆盖索引：清结算与对账 | 当前代码基线包含 `reconciliation-face`、`reconciliation-impl` 模块骨架和出款前准入候选契约、服务与测试；PRD、DSL、系分和 TDD 已可作为 TDD 分析输入，但代码只能作为后续候选输入。 | 不能声明清结算、对账或出款生命周期已实现；进入编码前必须另起独立 OpenSpec change 和 Execution Grant，并显式列入允许写入范围、验证命令、DDL/H2 schema、服务级 H2 测试、NFR 假设、观测告警、运营补事实命令白名单、专业确认状态、使用者解释字段、职责分离、证据最小化、`CLS-GATE-*` 和 `TDD-B7-RED-001` 至 `TDD-B7-RED-007` 首批 Red。 |
| B8 覆盖索引：资金数据治理边界 | 已存在 `governance-face`、`governance-impl` 交易投影重放骨架和 `FundsProjectionReplayServiceTests`；PRD 02/03/05、DSL、系分和 TDD 已可作为 TDD 分析输入，PRD 04 仅保留拆分索引。 | 交易投影重放有局部边界基线；Manifest、账本余额快照覆盖模式、余额水位隔离、普通指标快照、指标水位隔离和大数据消费边界仍需独立落地。进入编码前必须在 Execution Grant 中确认 `GOV-GATE-*`、治理物理落点、依赖方向、是否新增公共契约、DDL/H2 schema、Mapper/Entity 归属、边界测试、指标水位隔离测试和 `TDD-B8-RED-001` 至 `TDD-B8-RED-005` 首批 Red。 |
| 使用者可解释性和 Runbook | 当前仅有产品和系分矩阵，尚未形成专项可执行测试资产。 | 后续触碰交易投影、查询 DTO、清结算、对账、归档、重放、导出或告警时，必须补可解释输出和 Runbook 信号断言，不能只证明内部状态正确。 |

### 5.2 差距清单

| 编号 | 设计基线 | 当前代码观察 | 后续落地要求 |
| --- | --- | --- | --- |
| GAP-LEDGER-001 | Ledger 跨模块生产入口必须以账本交易过账、账本事实查询、余额投影查询 / 校验和受控运维 application facade 为主，资源型 CRUD 不得绕过不可变事实链。 | `GSD2-LD-APPLICATION-FACADE-001` 已完成首轮 application facade：`LedgerPostingApplicationService` 只委派标准 `LedgerTransactionPostingService`，`LedgerFactQueryApplicationService` 只读账本交易和账目分录事实，`LedgerBalanceProjectionApplicationService` 只读账本余额投影；`LedgerApplicationFacadeTests` 已证明 facade 不暴露 update/delete，也能走真实入账、事实查询和余额投影查询链路。`ledger-face` 仍保留 `LedgerService#updateLedgerBalance/deleteLedgerByIds`、`LedgerTransactionService#updateLedgerTransaction/deleteLedgerTransactionByIds` 等公共兼容 API。 | 首轮生产默认入口已补齐；后续若要删除、改签、隐藏或权限化现有 public service，必须单独给兼容策略、调用方影响清单、生产迁移和回归测试。受控运维审批、运营补账、历史数据清理和清结算差错补事实也不得复用本首轮 facade Grant。 |
| GAP-AUTH-001 | 授权过期必须有 `EXPIRE` 事件和 `expire` 服务入口。 | 已由 `b0666ba feat: 补齐授权过期释放 canonical 能力` 关闭：新增 `FundsTransactionEventType.EXPIRE`、`FundsTransactionStatus.EXPIRED`、`FundsAuthorizationTransactionExpireRequest` 和 `FundsAuthorizationTransactionService#expire`，并补齐 route replay、生命周期和授权流程测试。 | 后续只作为 B4 回归基线维护；除非新的 Execution Grant 扩展过期幂等、并发或投影解释，不再把 `GAP-AUTH-001` 当待实现缺口。 |
| GAP-AUTH-002 | `settle` 支持强制完成模式，必须有 FORCE 模式、受信策略或审批快照、原因、上限、外部原事实引用、凭证和审计；首轮 FORCE 模式不得依赖内部原授权流水。 | 已由 `616dac1 feat: 补齐授权强制完成能力` 和 `3825466 fix: 收紧授权强制完成策略红线` 关闭首轮缺口：普通完成继续要求 `authorizationTransactionSn`，FORCE 模式不构造 `AUTHORIZATION` reference、不查询原授权账本交易，路由从 `AVAILABLE` 进入 `SETTLEMENT`，并通过内部受信策略校验、上限校验、原因、外部原事实、凭证和审计失败无副作用测试。 | 后续只作为 B4 回归基线维护；若要扩展生产策略引擎、审批快照、额度窗口、带原授权 overcapture、外部清算文件或运营审批系统，必须另起独立 Execution Grant。 |
| GAP-AUTH-003 | 拒付只作为已完成授权后的争议、扣回或追偿语义，不等同于授权拒绝；目标态默认可通过 `settleRefund` 携带原因、凭证、外部引用和审计上下文承接，不强制独立 `chargeback` 服务入口。 | 代码已有 `FundsAuthorizationTransactionService#chargeback`、`FundsAuthorizationTransactionChargebackRequest`、`CHARGEBACK` eventType、route replay `CHARGEBACK` phase 和授权交易 flow 成功/失败/幂等测试；`B4-DISPUTE-SEMANTIC-ALIGNMENT` 已由 `949b24a` 闭合首轮 canonical 可区分性：`settleRefund / AUTH_REFUND` 通过 `disputeMode`、`disputeReason`、`disputeVoucherRef` 和 `externalDisputeRef` 保留争议审计上下文，请求侧不恢复 `refundMode`。现有 `chargeback` 仍是实现资产和兼容资产，不能直接反推为目标态主入口。 | 后续只把首轮争议退款可区分性作为 B4 回归基线维护；若确认完整 dispute/chargeback case 或把 `chargeback` 升级为一等目标态 API，必须另行说明公共契约、状态、投影、查询、清结算追偿、外部规则和兼容边界。 |
| GAP-AUTH-004 | `settleRefund` 无授权退款模式必须可表达可追溯外部引用。 | 已关闭首轮缺口并经 CR 收缩为资金层最小契约：`FundsAuthorizationTransactionRefundRequest` 以 `authorizationTransactionSn` 为空判定 no-auth refund，并保留 `externalReferenceSn`、退款原因和操作者/审计；converter 在 NO_AUTH 模式构造 `EXTERNAL_TRANSACTION` reference 并禁止携带内部授权流水；route resolver 显式支持 `AUTHORIZATION_NO_AUTH_REFUND_STANDARD`；生命周期保存独立退款事实。 | 后续只作为 B4 回归基线维护；若要扩展运营审批、人工差错单、累计退款跨请求聚合控制、查询投影解释、外部规则核验或敏感上下文专项矩阵，必须另起独立 Execution Grant。 |
| GAP-AUTH-005 | 授权支付工具应用入口必须支持从工具引用、绑定和规则上下文解析到账户主体型授权内核。 | `InstrumentTransactionLifecycleApplicationService#authorizeByInstrument` 已成为支付工具交易生命周期统一授权入口，当前只委派 `AuthorizationAdmissionApplicationService#authorizeByInstrument`；授权准入专项服务复用 `PaymentInstrumentPreTransactionSnapshotApplicationService` 组合支付工具能力、绑定、资金责任和账户能力，请求携带 Spend Rule 决策证据时显式消费 `SpendControlAdmissionApplicationService`，`REJECTED` 会在账户主体型授权内核前拒绝并保持无资金事实副作用；批准后仍委派账户主体型 `FundsAuthorizationTransactionService#authorize`，route snapshot 承载支付工具引用和绑定快照。 | `GSD2-WALLET-AUTH-ADMISSION-COMPOSE-002` 和 `GSD2-WALLET-INSTRUMENT-LIFECYCLE-AUTH-FACADE-001` 已消费为首轮 Green；仍不声明完整 Spend Rule 规则引擎、决策日志持久化、控制活动自动消费、VCC facade、统一支付工具交易内核或交易 canonical 入参改造完成。 |
| GAP-TRX-CAP-001 | 交易层仍需完善账户主体型授权生命周期、退款/拒付、余额控制调账和原路径回放能力；这些能力不是支付工具交易入口。 | 当前直接交易、授权交易、余额控制和投影已有局部基线，授权过期释放、强制完成、B4-NO-AUTH-REFUND 首轮 canonical 能力、支付工具授权入口 route snapshot 回链和 `paymentInstrumentRef` 投影解释已形成 Red/Green 证据；拒付语义、完整调账审计、缺快照差错、支付工具换绑后全链路 replay 和 projection store 仍未形成完整 Red/Green 证据。 | 后续 B3/B4/B5/B6 Execution Grant 可以在 `transaction-face`、`transaction-impl`、route replay 或 transaction projection 中扩展 canonical 请求、事件、状态机、DTO 和测试；主体仍必须是已解析资金账户、信用账户或平台账户角色解析后的平台资金账户。禁止把这些能力合并成统一 `InstrumentTransactionService`，也禁止让交易 canonical 请求直接接收支付工具作为账务主体。 |
| GAP-WALLET-001 | 钱包模块需要 application/use-case facade 承接跨模块用例，资源服务只作为内部协作或管理能力。 | 首轮已由 `GSD2-B2-WALLET-APPLICATION-FACADE-001` 补齐 `FundingResponsibilityResolutionApplicationService`，由 `GSD2-B2-WALLET-APPLICATION-FACADE-002` 补齐 `PaymentInstrumentCapabilityApplicationService`，由 `GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001` 补齐账户能力来源准入，由 `GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001` 补齐支付工具预交易快照，由 `GSD2-B2-SPEND-CONTROL-ADMISSION-001` 补齐支出控制准入快照，由 B5 切片补齐 Spend Control Activity 和交易消费控制活动；`GSD2-WALLET-AUTH-ADMISSION-COMPOSE-002` 已让授权准入复用预交易快照并可消费 Spend Rule 决策证据；`GSD2-WALLET-SPECIAL-BUSINESS-CAPABILITY-MAP-001` 已形成 VCC / VA / 全球账户 / ACH 入口能力地图，`InstrumentTransactionLifecycleApplicationService` 已具备授权和收款两个最小服务层入口。`GSD2-WALLET-INSTRUMENT-RECEIVE-SNAPSHOT-001` 已让 `receiveByInstrument` 在委派账户主体型 `topup` 时把脱敏支付工具引用固化到 route snapshot；`GSD2-WALLET-INSTRUMENT-RECEIVE-BINDING-VERSION-GUARD-001` 已让收款请求强制携带 expectedBindingVersion，缺版本在交易内核前失败且无资金事实副作用。调用方已可通过 wallet application 契约只读解析工具能力、资金责任、账户能力、外部 Spend Rule 决策证据和最终资金账户或信用账户目标主体快照；资源服务仍保留管理和内部协作职责。 | 后续 B2/B4/B5/P2 Execution Grant 必须继续决定钱包账户聚合、完整 Spend Rule 控制闭环、预算控制投影、VCC facade、`payOutByRail`、外部资金事件消费或业务场景准入的 facade 命名、Request/DTO、依赖方向和边界测试；跨模块调用不得长期由调用方拼装多个资源服务来完成工具准入、账户能力校验、Spend Rule 决策证据校验或授权准入；业务入口参数不得为了统一入口而强制把内部余额钱包、信用额度账户、返利钱包或商户钱包建模为 `PaymentInstrument`。 |
| GAP-WALLET-001A | 支付工具能力控制必须显式区分工具动作能力和内部账户能力。 | `PaymentInstrumentCapabilityApplicationService` 首轮已固化 `RECEIVE`、`PAY`、`AUTHORIZE`、`REFUND`、`WITHDRAW` 五类工具动作枚举，并基于支付工具状态、方向、币种、生效窗口、默认绑定、绑定角色和绑定版本输出只读准入快照；测试覆盖通过、方向不匹配和绑定版本失效三类无账务副作用场景。 | 后续若要把工具能力从方向映射升级为可配置能力字段或通道能力矩阵，必须同步 core/face 契约、DTO/Entity/H2 schema、敏感字段治理和边界测试；工具能力通过仍不得替代账户能力、余额、额度、周期、资金责任、Spend Rule 或授权准入校验。 |
| GAP-WALLET-002 | 支出主体资金责任解析关系的目标字段必须能表达最终资金或额度责任主体。 | 首轮已由 `GSD2-B2-FR-TARGET-001` 关闭资源关系目标字段缺口：`CreateSpendSubjectFundingRelationRequest`、DTO、Query、Entity 和 H2 schema 已新增 `targetSubjectType + targetSubjectId` / `target_subject_type + target_subject_id`，`fundingAccountId / funding_account_id` 保留为资金账户目标兼容字段；服务测试已覆盖资金账户兼容归一、信用账户目标、不可用信用账户拒绝和预算组拒绝。 | 后续仍需在 B2 wallet application facade 或 route snapshot Grant 中补平台角色解析、资金责任决策摘要、完整回放断言和对外用例封装；不得把本轮资源服务字段迁移等同为支付工具准入、VCC facade 或完整资金责任解析生产 Done。 |
| GAP-VCC-001 | 预付卡入金、系统内充值和退卡资金动作必须通过 application facade 解析到内部责任主体后委派账户主体型交易。 | 当前 PRD/系分已定义 `postPrepaidFunding`、`unloadPrepaidFunding` 或等价命令语义，但尚未形成稳定 Request/DTO、幂等摘要、确认引用、余额断言和失败无副作用测试。 | `P2-VCC-PREPAID` Execution Grant 必须先补 `R0-VCC-PREPAID-001`，证明未确认入金不增加余额、同键不同摘要冲突、责任主体不唯一阻断、完整敏感信息阻断；不得把 prepaid virtual card、issuer 余额摘要或外部 financial account 写成账本主体。 |
| GAP-VCC-002 | 共享卡和预付卡的清算、释放、退款、拒付必须按原授权、原 route snapshot、原工具快照和原资金责任决策回放。 | 当前 route snapshot 和授权生命周期已有基础能力，但尚未形成面向 VCC 工具 facade 的清算/逆向命令、重复损失防护、快照缺失差错入口和卡维度投影测试。 | `P2-VCC-LIFECYCLE` Execution Grant 必须先补 `R0-VCC-LC-001`，证明换绑后逆向不重算当前绑定、缺快照失败或人工处理、refund 与 chargeback 不合并、部分清算和剩余释放金额闭合。 |
| GAP-WALLET-003 | 预算组目标态不是核心资金账务主体，现有 `BUDGET_GROUP` 兼容路径必须收敛。 | `BudgetGroupService#createBudgetGroup` 已不再初始化 ledger bucket，目标测试覆盖 LIFETIME、MONTHLY 和 CUSTOM_CYCLE 预算组创建无账本事实副作用；Phase 1 已按 `staged-control-view-backed / control-view-replacement-first` 新增 `BudgetControlLimitAdjustmentApplicationService`，预算额度调增 / 调减写 Spend Control Activity 并派生 Budget Control Projection，不生成资金交易、route、posting、LedgerEntry、账本交易或余额投影；Phase 2 已消费 `GSD2-LD-BUDGET-GROUP-CONTROL-POSTING-FORBID-001`，任意 `BUDGET_GROUP` LedgerEntry 主体均在账本交易、posting plan、LedgerEntry 和余额投影落库前拒绝，预算组旧余额控制调额入口前置拒绝且无资金或账本事实副作用；Phase 3 已消费 `GSD2-B2-BUDGET-GROUP-LEDGER-COMPAT-CLEANUP-001`，`BUDGET_BASIC` 不再作为 active ledger profile，显式账本初始化、资金账户查询聚合、route leg / replay 和 DSL 合约均不再把预算组当作核心资金账务主体。 | `BUDGET_GROUP_LEDGER_COMPAT_CLEANUP_GREEN_VERIFIED`。不得把预算组 ledger 初始化、资金价值余额 bucket、控制账本兼容或任意 LedgerEntry 主体作为目标态 Done；`FundsSubjectType.BUDGET_GROUP` 仍作为历史枚举 / 控制上下文保留。生产历史数据清理、公共 API 删除、枚举删除、运营审批、权限模型或真实迁移脚本必须另起 Execution Grant。 |
| GAP-PI-001 | 支付工具、绑定关系、资金责任解析关系和绑定历史审计必须能支撑 route snapshot、原路径回放和敏感信息治理。 | 当前已有 `PaymentInstrumentService`、绑定当前态、绑定历史审计、资金责任解析关系（历史命名为资金来源关系）、工具方向/状态守卫、脱敏快照、授权 route snapshot 回链、收款工具 route snapshot 回链、收款 expectedBindingVersion 必填护栏和投影解释消费 `paymentInstrumentRef` 的服务层测试。 | B2 的支付工具基础能力可作为局部基线；后续仍需补支付工具换绑后资金全链路 replay、完整 route snapshot 引用组合断言和 projection store / query index。 |
| GAP-CTX-001 | 交易请求的 `contextVariables` 只作为扩展上下文只读承载，不作为核心资金事实源。 | 当前直接交易、授权交易和余额控制 Request 已统一为 `ReadonlyContextVariables`；`FundsRequestContextVariables` 已移除，`FundsTransactionRequestContextVariablesContractTests` 覆盖只读承载和 null 语义；转换器合并上下文前执行敏感字段校验。 | 后续触碰 `com.wind.funds.transaction.model.request` 时不得恢复可变字段或 request 专属快照工具；核心资金语义必须进入一等字段、route snapshot、交易事实快照或等价不可变存储，并补请求契约、摘要和敏感上下文回归测试。 |
| GAP-CTX-002 | 钱包、交易、路由、账务和投影等管理对象的 `contextVariables` 不得成为敏感信息或权益核心事实旁路。 | 当前已在 DSL、route、ledger、transaction、wallet 管理对象形成局部阻断基线；`77bc9f4` 补齐钱包资金账户和支付工具创建入口的 `fundingNature`、`currentMarketingRule` 等权益核心字段阻断。 | 后续新增任何上下文写入入口、DTO 映射、快照保存或查询导出时，必须回归敏感字段、外部账户原文和权益核心字段阻断，并证明失败不落账户、支付工具、route、posting、ledger entry、投影、日志或审计普通链路。 |
| GAP-BEN-001 | 旧 `FundsInstruction.benefitSnapshot` 和重型权益快照 DSL 必须退出目标契约，核心字段不能藏入 `contextVariables`。 | 已由 `GSD2-BENEFIT-LEGACY-SNAPSHOT-REMOVE-001` 关闭：旧 `FundsBenefitSnapshotSpec`、组件、引用、退款策略、稳定摘要对象、JSON 支撑、旧 JSON 夹具和旧契约测试已移除；`FundsDslJsonContractTests` 覆盖旧字段拒绝、核心字段 context 阻断和历史摘要允许。 | 后续只作为 B1 回归基线维护；新增权益字段必须进入权益资金事实一等契约、route snapshot、交易事实或等价不可变存储，不得恢复旧快照 DSL。 |
| GAP-BEN-002 | 直接交易需按原权益资金事实处理商户券、平台补贴、储值券、零实付权益和叠加权益。 | `POSTING_REQUIRED` 结算和退款型逆向处理已通过 `FundsBenefitFundingApplicationServiceImpl` 接到标准直接交易、route、posting 和账本链路；非入账权益事实、零实付主指令、伴随指令和储值负债口径仍未闭合。 | B3 覆盖索引继续补商户让利 no-ledger、平台补贴独立资金影响、储值券资金性质、零实付表达、独立伴随指令原子性、部分成功补偿、专业确认、审计证据包、使用者解释视图、证据最小化、外部规则核验和退款分摊确定性测试。 |
| GAP-BEN-003 | 授权交易需表达占券、完成核销、撤销或过期释放，以及拒绝不处理权益。 | 当前授权链路未见权益占用、核销或释放语义。 | B4 覆盖索引补授权占券、完成核销、撤销/过期释放、拒绝无副作用和并发竞争测试。 |
| GAP-BEN-004 | 退款、业务取消、过期、拒付和 Route Replay 必须使用原权益资金事实、历史摘要或经审批补充事实，不得按当前营销规则重算。 | `refund` 已要求引用原权益资金交易，并统一承接退款、业务取消、人工纠错或反向冲销；route replay 只读取历史 `benefitSnapshotId + stableDigest` 摘要且不依赖当前请求旧 DSL。仍缺补充权益事实模型、授权占券、部分退款累计和缺事实人工处理边界。 | B6 覆盖索引补原权益资金事实 digest、事实源一致性、缺事实失败、补充权益事实只追加、部分退款累计闭合、不退券和补贴冲回/保留分离测试。 |
| GAP-BEN-005 | 含权益交易进入清结算与对账时，营销核销、订单金额、资金入账、商户应收和补贴冲回必须可拆分核对。 | 冻结基线中清结算与对账仍只有模块骨架和出款前准入候选实现；当前候选实现不覆盖权益清分、清算、结算或对账拆分。 | 清结算与对账独立 OpenSpec change 中补权益清分、清算、结算、对账差错和人工处理矩阵。 |
| GAP-BEN-006 | `CONTRACT_ONLY` 样例、请求态摘要和 `contextVariables` 过渡不得替代生产事实源。 | 当前权益资金事实契约和 `POSTING_REQUIRED` 首个资金流已形成局部基线，后续非入账事实、清结算、对账、投影、归档、冷热读取和治理重放消费尚未闭合。 | 含权益 Phase 2/3 MVP 任务必须在 Execution Grant 中选择事实源、JSON 夹具级别、零实付表达、平台补贴表达、独立伴随指令原子性、储值预付口径、退款分摊粒度、退款分摊确定性规则、历史无权益资金事实处理、补充权益事实模型、专业确认状态、审计证据包、使用者解释视图、证据最小化和外部规则核验状态；缺决策时降级为 contract-only、设计验证或失败用例。 |
| GAP-BEN-007 | 含权益视图、证据包和外部规则核验必须满足可理解、安全和可审计要求。 | 当前只有设计矩阵和准入门禁，缺 `DSL-BENEFIT-EXPLAINABLE-VIEW-001` 对应的可执行契约和服务/投影测试。 | 后续触碰含权益用户账单、商户账单、运营时间线、财务对账视图、审计导出、证据包、外部规则或告警时，必须补 `TDD-BEN-RED-028` 至 `TDD-BEN-RED-030`、解释视图断言、脱敏导出断言和外部规则核验断言。 |
| GAP-BEN-008 | 权益让利资金入口应从重型权益快照契约重基线为交易级 application service。 | 已由 `GSD2-BENEFIT-FUNDING-TRANSACTION-REBASE-001`、`GSD2-BENEFIT-FUNDING-TRANSACTION-IMPL-001`、`GSD2-BENEFIT-LEGACY-SNAPSHOT-REMOVE-001` 和 `GSD2-BENEFIT-FUNDING-SETTLE-REFUND-REBASE-001` 关闭首轮缺口：`transaction-face` 暴露 `settle/refund`，`POSTING_REQUIRED` 已接到标准交易路由、交易事实和账本分录链路，旧重型权益 DSL 已物理删除，独立 `reverse` 入口已收敛为 `refund` 型逆向处理。 | 后续只作为权益资金交易回归基线维护；若要扩展非入账权益事实、独立权益事实表、清结算对账投影或授权占券，需要新的单一 Execution Grant。 |
| GAP-OPS-001 | 使用者可解释性必须能说明金额来源、事实状态、展示状态、操作状态、失败原因、处理入口、不可操作原因和恢复验收。 | 当前只有设计矩阵，缺专项测试和查询/投影输出断言。 | 后续按 MVP 任务补 `FundsOperationExplainabilityTests`、`FundsRunbookSignalTests` 或等价服务/投影测试；触碰高危查询、导出、告警或运营台时必须同步补权限、审计和只读边界。 |
| GAP-TDD-001 | 测试必须按最终 TDD 重建。 | 旧过渡测试不作为目标态依据；当前已有 B1、B2 和 B3 至 B6 的局部目标态测试，但 B1 至 B6 尚未按覆盖索引完整闭环。 | 继续按覆盖索引补齐测试；已重建测试只作为对应覆盖索引的局部基线。 |
| GAP-CLS-001 | 清结算与对账对象需独立状态机和表设计落地。 | 冻结基线已有 `reconciliation-face`、`reconciliation-impl` 模块骨架和出款前准入候选实现，但未覆盖对账任务、可清分明细、清分批次、清算候选、清算批次、结算单、出款单完整生命周期、差错单、追偿单、表结构和目标态服务层测试。 | 单独 OpenSpec change 和独立 MVP 任务处理，不混入交易主链路；后续确认是否复用现有 `reconciliation-*` 模块和是否扩展出款前准入候选能力。 |
| GAP-CLS-002 | 出款前准入门禁、外部非终态、金额不一致和出款解释状态必须有服务契约与测试资产。 | 当前代码基线已包含 `PayoutOrderService#checkPayoutPreflight`、`CheckPayoutPreflightRequest`、`PayoutPreflightResultDTO` 和 `PayoutPreflightServiceTests` 候选实现；已覆盖结构化外部规则核验证据、创建前无 `payoutSn` 检查、`factStatus`、`displayStatus`、`operationStatus` 和只读无账务事实断言，但仍缺 `PayoutOrderDTO` 解释状态字段、出款门禁表/字段、出款结果/回单处理、金额不一致处理、数据库级闭环和 `PayoutExplainabilityTests`。 | 清结算与对账 Execution Grant 必须把 `CLS-GATE-*`、`AC-SET-006` 至 `AC-SET-009`、`TDD-SETTLE-004`、`TDD-SETTLE-005`、`RED-032` 和 `RED-037` 纳入准入；若继续扩展候选能力，必须显式列入允许写入范围，并继续补出款表、状态守卫、回单、金额不一致、审计和外部规则字段完整性校验。 |
| GAP-ARCH-001 | 事实留存、重放、普通指标快照、账本余额快照和大数据消费边界需按治理能力独立落地。 | 当前代码已有 `governance-face` 和 `governance-impl` 交易投影重放骨架，已覆盖有界范围、交易投影 checkpoint 和 verify-only 不写投影等局部边界；尚未落 Manifest、余额快照覆盖模式、余额水位隔离、普通指标快照边界、指标水位隔离、异常人工处理闭环和大数据消费边界。 | B8 另起独立 OpenSpec change；必须把 `GOV-GATE-*` 纳入准入；事实留存和重放只做事实治理；账本余额快照按 `HOT_ONLY`、`COLD_MANIFEST`、`MIXED` 校验；普通指标快照只输出报表指标模块输入和边界测试；大数据消费只能通过治理读取、导出快照、Manifest 摘要、脱敏、digest 和审计承接；缺范围、缺 Manifest、冷热摘要不一致、检查点不连续、重放差异、权限不足或外部规则待确认时，只能生成差异报告、阻断、补证据、缩小范围、重跑或人工关闭，不得直接改事实。 |

## 六、TDD 落地顺序

TDD 落地顺序同时受能力优先级和 Execution Grant 约束：P0 先保证钱包、账本、账目、余额投影、对账、清分、清算、结算、账本余额快照和资金数据治理证据的资金不变量；P1 再扩展直接交易、授权交易、余额控制、交易投影和交易投影重放；P2 业务模式能力包只在专项授权中接入。下表的顺序 1 至 8 是覆盖索引，不表示 03 或治理能力低于 02，也不表示任一索引可以越过独立授权直接编码。

| 顺序 | 覆盖索引 | 目标 |
| --- | --- | --- |
| 0 | 基线重置 | 作废旧规格和旧测试；确认 docs、OpenSpec、resources 和代码差距。 |
| 1 | P0 共享承载：DSL 契约与枚举红线 | 重建 `FundsInstruction`、event、transactionType、金额临界值、route、payment instrument、routing decision、funding allocation、posting、`SettlementPolicy`、权益资金事实、JSON 契约测试。 |
| 2 | P0 基础事实：钱包账户与账本基础 | 重建账户、账本初始化、平台账户角色、支付工具、绑定关系、支出主体资金责任解析关系、账本周期、posting 平衡和余额投影测试。 |
| 3 | P1 交易入口：直接交易 | 重建充值、付款、转账、提现、退款、手续费、退费、受控负余额，以及商户券、平台补贴、储值券和不退券直接交易测试。 |
| 4 | P1 交易入口：授权交易 | 重建授权、拒绝、撤销、完成、过期、强制完成、完成后退款、无授权直接退款、拒付承接口径、授权占券和 VCC 扩展边界测试。 |
| 5 | P1 控制入口：余额控制 | 重建冻结、解冻、资金账户余额调整、信用账户额度、预算组额度、冻结提现组合测试，以及 adjust 绕过差错闭环的红线测试。 |
| 6 | P1 读模型：Route Replay 与投影 | 重建 Route Replay、支付工具换绑后原路径回放、缺快照失败、权益资金事实或历史摘要回放、交易投影只读、余额日志边界测试。 |
| 7 | P0 运营账务闭环：清结算与对账 | 按独立系分重建可清分准入、清分批次、清算候选、清算批次、结算单、出款单、出款前准入门禁、外部非终态、金额不一致、出款解释状态防误导、对账差错、权益拆分、退款时序和追偿测试。 |
| 8 | P0/P1 治理闭环：资金数据治理边界 | 重建事实留存门禁、Manifest、水位、余额重建、账本余额快照覆盖模式、交易投影重放、异常人工处理、大数据消费边界、指标只读、指标水位隔离和普通指标快照并发边界测试。 |

## 七、Superpowers 纪律

1. 每个 MVP 任务先写失败测试或契约测试，再做最小实现。
2. 每个资金变化场景必须断言状态、route snapshot、posting plan、ledger entry、余额投影和幂等。
3. 红线测试必须证明失败无副作用。
4. 不用旧测试通过证明新设计通过。
5. 发现产品、DSL、系分、TDD 不一致时，先补设计，再继续编码。
6. 重构只能在测试保护下发生，不改变外部可观察行为。
7. 设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果必须先形成独立交付检查点；未冻结前不得进入具体生产代码实现。
8. 所有资金变化测试必须覆盖相关金额临界值；同一资金对象存在多事件推进时，必须评估并发竞争用例，不能只用顺序执行证明安全。

## 八、Harness 门禁

每个开发任务必须声明：

| 项 | 要求 |
| --- | --- |
| 写入范围 | 明确模块、包、文件或测试目录。 |
| 只读范围 | 明确可参考的 docs、OpenSpec、源码和 resources。 |
| 禁止事项 | 不越界修改生产代码、不引入无主依赖、不恢复旧测试。 |
| 验证命令 | 至少包含 `just mvn-version`、`just compile` 和本任务相关测试命令；CAD 自动模式或完整基线复核优先使用 `just verify-cad`；无法运行需说明。 |
| 人工确认点 | 公共契约、表结构、枚举、资金红线、清结算对象、运营补事实命令白名单、权益退款分摊确定性规则、治理物理落点、归档重放、外部规则。 |

### Round 0 和 Execution Grant 字段

Round 0 是编码前核验，不是编码授权。Execution Grant 必须至少绑定以下字段；字段缺失时，本规格只能作为设计和 TDD 输入，不能作为生产代码、测试代码、DDL/H2 schema 或运行时配置写入依据。

| 字段 | 要求 |
| --- | --- |
| mvpScenario | 本任务服务的使用者、资金事实、最小闭环和不得声明的扩展能力。 |
| abilityBatch | A0 至 A4、B7、B8 或 P2 业务专项；B1 至 B8 细项只能作为覆盖索引，不得混合授权。 |
| authorityBaseline | PRD、DSL、系分、TDD、OpenSpec、Harness Plan、Git 提交点和允许读取的未提交文件。 |
| writeScope | 允许修改的模块、包、公共契约、枚举、Request/Query/DTO、状态机、表结构、H2 schema、测试资源和运行时配置。 |
| noWriteScope | 明确禁止修改的模块、对象、资金语义和 Done 结论。 |
| physicalLanding | 复用既有模块、新增 face/impl、暂不落物理模块或 contract-only；同时声明依赖方向、端口边界、DTO、Entity、Mapper 和边界测试。 |
| firstRedSet | 首批 Red ID、失败行为、目标测试资产、失败断言和验证命令。 |
| moneyInvariant | 金额闭合、主体、账目、币种、周期、route、posting、entry、projection、幂等、失败无副作用和审计断言。 |
| operationGovernanceGate | `CLS-GATE-*`、`GOV-GATE-*`、运营补事实命令白名单、Manifest、checkpoint、watermark、差异报告、人工处理和指标水位隔离。 |
| externalRuleStatus | 规则来源、版本或发布日期、生效日期、适用主体或范围、适用法域、核验日期、确认方和确认状态。 |
| verificationAndStop | 验证命令、失败停止条件、Not Done 条件和回到设计或人工确认的触发器。 |

## 九、必须失败红线

红线以 `docs/TDD设计/支付资金底座测试驱动设计.md` 第十四章为准，后续 OpenSpec change 不得删减红线，只能增加更具体的场景。
