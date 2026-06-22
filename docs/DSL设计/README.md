# DSL 设计

本目录是支付资金底座 DSL 承载层稳定设计入口。完整设计包入口见 [../README.md](../README.md)。

权威文档：

- `支付资金底座DSL承载层设计.md`

业务接入方需要把业务事实、资金指令、route、posting、entry 和验收证据串成接入材料时，先读 [../用户接入指南/README.md](../用户接入指南/README.md)。用户接入指南只提供使用者视图和填报路径，DSL 语义仍以本目录权威文档为准。

## 背景和目标

背景：产品设计描述业务目标和资金规则，系分设计描述服务、状态、表和观测落点；两者之间需要一层稳定 DSL，把自然语言资金语义转成可编码、可测试、可回放、可审计的契约对象。

目标：用统一的资金事实、资金指令、route snapshot、posting plan、LedgerEntry、投影和审计引用承接 PRD，向下约束系分和 TDD，确保每笔金额都能被解释、被核对、被重建。

非目标：DSL 不定义业务产品功能，不替代清结算、对账、归档、指标、运营后台、风控合规或财务制度设计，不授权修改公共接口、生产代码、测试资源、DDL/H2 schema 或运行时配置。

成功标准：任一 `DSL-*` caseId 都能说明业务事实、主体、金额、账目、路径、账务、失败边界、审计引用和可测断言；无法落到 TDD 契约测试或资金断言的内容只能作为设计说明，不能声明机器契约通过。

## MVP DSL 约规

DSL 先承载 MVP 必须闭环的资金事实，不提前抽象完整协议层、通用规则平台或全量业务扩展模型。新增 DSL 对象、枚举或字段前，必须证明它用于目标可验收场景的请求、route snapshot、posting plan、LedgerEntry、投影解释、幂等或失败红线。

只为未来业务预留的字段、外部协议原文、完整通道状态、完整风控材料、完整运营动作和全量报表口径不得进入 MVP DSL；如后续需要，应以独立场景、独立 caseId 和独立测试证据补充。

## 定位

DSL 设计归属为产品到系分之间的领域承载层，不是产品 PRD，也不是实现方案。它是产品语义到资金底座契约之间的结构化转译层：

- 向上承接业务事实、主体、账户、账目、资金流、异常路径和验收红线。
- 向下统一资金交易结构、资金链路结构、资金指令、路由快照、账务计划、账本分录、余额投影、交易投影和契约验收的语义。
- 不替代清结算、对账、归档、指标、异常人工处理、运营后台、风控合规和财务口径等能力域的独立产品与系分设计。

## 使用方式

1. 产品评审时，重点看业务场景如何转成资金事实、资金交易结构和资金链路结构。
2. 系分设计时，重点看 DSL 对象结构、不变量、流程边界、异常路径和开发承接矩阵。
3. 测试设计时，重点看场景覆盖、JSON 契约样例、TDD 输入输出、余额断言和评审清单。
4. 业务沟通时，重点用本文统一“单、账、钱、余额、状态、链路”的共同语言。
5. 授权链路统一使用“授权完成”承接产品侧“授权结算”，事件使用 `AUTHORIZATION_TRANSACTION / SETTLE`；不要把它和商户清结算的 `SETTLEMENT` 账目混用。

## 核心决策

| 决策 | 取舍 | 边界 |
| --- | --- | --- |
| DSL 作为承载层 | 用资金事实和契约对象连接 PRD、系分和 TDD，避免产品语义直接散落到实现细节。 | DSL 不直接定义 Controller 报文、数据库结构或外部协议原文。 |
| 账本事实优先 | 余额、账单、交易投影和指标都从账本分录或不可变事实派生。 | 投影、日志、报表和治理结果不得反写资金事实。 |
| route snapshot 固化路径 | 正向交易记录路由决策，逆向、退款、拒付、退费和清结算重跑优先沿用原快照。 | 缺少原事实或快照时只能进入差异、审批、补证据或阻断，不得临时重选路。 |
| 机器契约分级 | 文档 caseId、契约夹具、资金流夹具和服务级测试分层声明证据等级。 | 文档样例不能直接替代测试资源、服务级测试或生产完成证据。 |
| 敏感证据最小化 | 外部账户、支付工具、凭证、规则来源和审计证据只保存摘要、脱敏值或引用。 | DSL 示例不得保存完整卡号、CVV、密钥、证件、token secret 或敏感原文。 |

## 场景落地口径

任何资金场景进入 DSL 前，必须先能回答下列问题；回答不完整时，应回到 PRD 或系分补设计。

| 问题 | 必须有的答案 |
| --- | --- |
| 这是什么事实 | `instructionType`、`eventType`、`transactionType` 和 `businessScene` 是否明确。 |
| 谁受影响 | 所有业务主体是否能解析为内部可记账主体或明确外部引用。 |
| 金额是否安全 | 金额、币种、原始金额、汇率、精度、上限和累计可处理金额是否可校验。 |
| 路径是否稳定 | route、route snapshot、payment instrument、external account、platform account 和 funding allocation 是否可解释。 |
| 账务是否平衡 | expected posting 是否能落到 entry 级别，且每个 posting plan 独立平衡。 |
| 失败是否无副作用 | 拒绝、余额不足、缺快照、错币种、规则不唯一、权限不足是否不生成 route、posting 或 entry。 |
| 逆向是否可回放 | 退款、撤销、退费、拒付、解冻或清结算重跑是否能沿用原快照。 |
| 人工处理是否安全 | 缺原事实、缺 Manifest、规则待确认、重放差异或权限不足时，是否只能进入差异报告、审批、补证据、缩小范围、重跑或关闭差异，而不是直接改事实。 |
| 审计是否足够 | 操作者、原因、凭证、规则版本、请求摘要和外部 reference 是否可追溯。 |
| 金融边界是否留痕 | 涉及资质、法域、客户资金、备付金、跨境、外汇、敏感数据或外部规则时，是否有规则来源、版本或发布日期、生效日期、适用主体或适用范围、适用法域、核验日期、确认方、确认状态和证据引用。 |

## DSL 准入评估口径

DSL 设计的准入目标是证明产品语义可以被稳定承载，并能继续下钻到系分和 TDD。完整跨文档门禁见 [../README.md#设计准入评估总控](../README.md#设计准入评估总控)。

| 评估维度 | DSL 侧必须证明 | 阻断信号 |
| --- | --- | --- |
| 可用性 | 产品场景能转为明确的 `instructionType`、`eventType`、`transactionType`、`businessScene` 和业务引用。 | 只能写自然语言说明，无法形成稳定 DSL 事件或指令。 |
| 资金安全 | 主体、账户、账目、金额、币种、账本周期、route snapshot、posting plan 和 LedgerEntry 语义完整。 | 外部账户、支付工具、用户 ID、商户 ID 或订单号被设计成可记账主体。 |
| 金融红线 | 规则来源、版本或发布日期、生效日期、适用主体或适用范围、适用法域、核验日期、确认方、确认状态、敏感信息脱敏和证据引用能在 DSL 中留痕或显式标为待确认。 | DSL 示例保存完整 PAN、CVV、密钥、token secret 或把外部规则写成无版本结论。 |
| 易用性 | DSL 字段能支持用户账单、商户账单、运营时间线、拒绝原因和审计解释。 | DSL 只能入账，不能解释为什么拒绝、为什么选路、逆向处理为什么回放原路径。 |
| 可理解性 | 枚举、事件、状态和 JSON 示例与产品术语映射一致。 | 授权完成与商户结算、`SETTLE` 事件与 `SETTLEMENT` 账目混用。 |
| 可开发性 | DSL 对象、不变量、失败边界和公共字段足够稳定，不要求实现时临时补核心字段。 | 公共契约字段未定、枚举含义未定或必填语义未定。 |
| 可测试性 | 每个 DSL 样例能映射到 TDD 契约测试、余额断言、红线失败、异常人工处理闭环或明确不适用原因。 | JSON 示例只是展示材料，不能驱动测试；治理差异只写日志，没有可断言的阻断原因、影响范围、责任归属和处理动作。 |

### 资金场景借贷平衡权威表

[支付资金底座DSL承载层设计.md#51-资金场景借贷平衡与账务期望表](支付资金底座DSL承载层设计.md#51-资金场景借贷平衡与账务期望表) 是各场景 `PostingPlan`、`LedgerEntry` 和余额投影断言的唯一权威表。PRD 只保留产品资金影响摘要，系分和 TDD 只承接引用与验证要求，不复制维护完整借贷表。该表必须同时说明参与方与账户示例、账户类型、`normalBalanceSide`、借贷如何平衡、余额桶如何变化和失败红线，避免多份表各自演进导致借贷口径漂移。

| 使用场景 | 必须使用该表回答 |
| --- | --- |
| 产品评审 | 该场景是同主体余额桶转换，还是跨主体价值转移；参与方账户示例和账户类型是否便于产品、财务、运营共同理解。 |
| DSL 设计 | 借贷方向、账目、账户类型、币种、账本周期、原事实引用和失败无副作用是否清楚。 |
| 系分设计 | `LedgerPostingAssembler`、账本写入、`normalBalanceSide` 推导、余额投影和清结算/治理边界如何落地。 |
| TDD 设计 | 每个资金变化测试应断言哪些 route、posting、entry、account category、projection delta、幂等和 forbidden facts。 |

## DSL 与产品、系分、TDD 对齐口径

DSL 评审口径：DSL 入口必须承接 PRD 的业务目标和资金语义，并能下钻到系分对象、服务边界和 TDD 证据。DSL 可作为 TDD 分析输入；只有 DSL caseId 落到测试资源、被测试读取，并覆盖对应资金断言后，才能声明机器契约通过。

| 对齐项 | DSL 承载 | 系分落点 | TDD 证据 | 阻断信号 |
| --- | --- | --- | --- | --- |
| 稳定口径 | DSL 只描述资金事实、指令、route、posting、entry、projection 和审计引用，不描述任务过程。 | 系分只按 DSL 不变量设计服务、状态、表和观测。 | TDD 只从稳定 caseId、字段语义和失败边界生成用例。 | DSL caseId 只是说明文字，无法落到测试输入或断言。 |
| 可解释、可核对、可重建 | 每笔资金事实必须有主体、金额、币种、账目、业务引用、规则来源和审计引用。 | 交易、路由、账本、投影、对账和治理分层承接。 | 测试同时断言状态、route snapshot、posting plan、ledger entry、projection、幂等和审计。 | 只有交易状态或错误码，无法证明金额来源和资金路径。 |
| P0 统一内核 | 钱包、账本、账目、余额投影、对账、清结算和归档使用统一资金事实与账务对象。 | 02、03、04 分册分别落账户账本、对账清算对象和治理对象。 | P0 Red 优先证明账户、账目、余额桶、对账差错、归档水位和只读边界。 | P2 业务在 DSL 中定义平行钱包、账本、清结算、对账或归档对象。 |
| P1 交易入口 | 直接交易、授权交易、余额控制、交易投影通过 instruction/event/route/posting 组合表达。 | 02 分册落服务入口、状态机、表设计、投影和 route replay。 | TDD 覆盖直接交易、授权完成/撤销/过期、冻结解冻、退款拒付、重复请求和余额不足。 | 授权拒绝生成 route/entry，冻结表达消费，逆向不按原 route snapshot 回放。 |
| P2 业务补充 | VCC、全球账户、收单和 ACH/银行转账只传入归一业务事实、外部引用、状态映射和待确认规则；VCC 发卡不新增 `VCC_ACCOUNT`，VCC 卡、prepaid virtual card、shared card 通过 `PaymentInstrumentRef`、绑定快照、`AccountHierarchySnapshot`、Spend Rule 快照和 `FundingAllocationDecision` 表达，并最终解析到资金子账户或信用子账户。 | 业务能力包通过准入卡接入统一资金内核，不改变 P0/P1 边界；VCC 关联子账户、资金账户和信用账户按真实资金、预付余额与授信额度职责入账，父账户默认用于约束和汇总，预算组和 Spend Rule 只按预算 scope、控制窗口、规则决策和审计职责使用；支付工具、资金责任解析和授权准入应由 wallet application facade 或等价 use-case 入口组合。 | TDD 覆盖业务状态映射、乱序重复、外部引用脱敏、规则待确认、P0/P1 回归、`TDD-RAIL-001A`、`TDD-LEDGER-013` 至 `TDD-LEDGER-016`、`TDD-P2-VCC-004` 至 `TDD-P2-VCC-011`、`TDD-WALLET-015` 至 `TDD-WALLET-019`。 | 业务轨道协议、外部账户、卡组织/银行原始规则、敏感原文沉入资金内核，绕过 application facade 拼装资源服务，或把卡工具、预付资金模式、共享卡绑定模式、父账户汇总、预算组、Spend Rule 当成可入账主体。 |
| 清结算与对账 | 对账差异、清分、清算、结算、出款和追偿必须保留批次、来源事实、规则和处理证据。 | 03 分册落对象状态机、服务 API、表设计、审批审计和补偿策略。 | `CLS-GATE-*`、`TDD-B7-RED-*`、服务级 H2 流程、并发重跑和失败无副作用测试。 | 对账差异直接改账，出款绕过结算锁定或外部规则核验。 |
| 归档、重放和指标边界 | Manifest、checkpoint、watermark、差异报告和指标输入都必须表达只读、范围和处理动作。 | 04 分册落 governance 逻辑边界、归档申请、重放任务、差异报告和人工处理。 | `GOV-GATE-*`、`TDD-B8-RED-*`、dry-run/apply、指标水位隔离和治理边界测试。 | 归档改变事实身份，重放重新入账，指标快照替代账本余额确认。 |

DSL 层不新增 `InstrumentTransaction`、`PaymentInstrumentTransaction` 或支付工具账务主体。若需要描述 VCC 预付卡充值、共享卡调额、VA 收款、全球账户付款、ACH return 等场景，应使用业务能力包准入卡或场景上下文描述：

```text
ScenarioFundsOperationContext
  -> externalEventRef / businessPackRef / railRef
  -> PaymentInstrumentRef / BindingSnapshot / FundingAllocationDecision
  -> canonical FundsInstruction / route / posting / ledger
```

特殊业务入口的 DSL 承载边界如下：

| 场景 | DSL 输入 | 转换后的资金动作 | 必须保留的外部引用 | 禁止表达 |
| --- | --- | --- | --- | --- |
| VCC 预付卡充值 | `PaymentInstrumentRef`、绑定资金子账户、外部入金引用、业务流水。 | 资金账户入账、账本分录和余额投影。 | 充值订单、processor event、外部入金流水。 | 卡号余额、VCC 账本主体、调额即入账。 |
| VCC 预付卡提现 | 卡工具引用、资金子账户、提现目标、外部出款引用。 | 提现、在途、费用、清结算和对账。 | payout、rail、fee、quote 或退汇引用。 | 外部 accepted 即成功。 |
| VCC 共享卡授权 | 卡工具引用、信用子账户、父账户快照、Spend Rule 决策证据。 | 账户主体型授权、冻结、退款和撤销。 | merchant、MCC、规则版本、控制活动引用。 | 替换 canonical 授权入参、共享卡号账本。 |
| VA 收款 | VA 引用、statement line、绑定资金账户。 | 资金账户入账或对账差异。 | 银行流水、PSP 通知、付款方摘要。 | VA 内部余额。 |
| 全球账户付款 | payout 指令、账户主体、rail、费用、quote。 | 出款、费用、在途、退汇和对账。 | SWIFT/local rail、quote、外部状态。 | 在 funds DSL 中实现 rail 协议。 |
| ACH 或银行转账事件 | 外部事件、原交易引用、内部账户主体。 | 入账、扣账、退款、撤销、调账或差异单。 | ACH return、NOC、reversal、银行事件流水。 | Nacha/银行文件协议和外部账户敏感明文。 |

DSL 的稳定不变量是：支付工具和外部 rail 只作为 `ScenarioFundsOperationContext`、`PaymentInstrumentRef`、`ExternalEventRef`、绑定快照和审计字段进入资金链路；资金交易、账本交易和账目分录仍必须落到资金账户、信用账户或平台角色解析后的平台资金账户。预算组、Spend Rule、支付工具、VA、VCC 卡号和外部账户不得成为 `SubjectRef` 的可入账主体。

`authorizeByInstrument` 是 application facade 的入口命名，不改变 `FundsAuthorizationTransactionService.authorize` 的 DSL 内核语义。授权 DSL 内核继续表达已解析账户主体的资金占用、route、posting、ledger 和投影事实。

支付工具与 Spend Rule 进入工程任务前，应先完成支付工具准入、资金责任解析、授权 application facade、Spend Rule 控制事实和只读投影边界核验。未形成独立工程任务前，DSL 不新增 `InstrumentTransaction`、支付工具账务主体、预算组账本主体或 Spend Rule 资金交易事实。

若业务强制要求 VCC 优先，DSL 侧应先证明 `AccountHierarchySnapshot`、绑定快照、父账户快照、账目 profile 和 VCC 卡到资金/信用子账户的映射可以被稳定承载。该路径不得直接新增 `P2-VCC-*` 资金流 DSL、H2/DDL schema、支付工具账务主体或卡号账本主体。

资金责任目标字段已在 `GSD2-B2-FR-TARGET-001` 首轮选择 `targetSubjectType + targetSubjectId`，资源关系可表达资金账户和信用账户目标主体；平台角色解析后的平台资金账户、完整 DSL fixture、资金责任摘要和回放断言仍需后续 Grant 同步。

### 清结算与对账 DSL 边界

B7 目标态可以描述可清分明细、清分批次、内部清算候选、结算锁定、出款结果、对账任务、匹配结果和差错单。最小可交付 DSL 证据应先覆盖对账任务范围、内部事实摘要、外部证据摘要、规则版本、匹配结果、差异指纹、差错阻断、重跑记录、处理动作和补事实白名单引用。

`contract-only` 只能证明契约字段、枚举和目标 Red 可承载，不得声明清结算、对账或出款生产可用；若要声明 B7 切片具备生产交付证据，必须同步最小 DDL/H2、真实 Spring Bean 服务流、差错阻断、重跑幂等、白名单补事实准入和失败无副作用测试。VCC clearing、全球账户入出金、VA 收款和外部 payout 只能消费该差错闭环作为阻断、解释或补事实输入，不得在 P2 业务包内平行定义清结算、对账、出款或追偿事实。

### 支付工具和账户能力 DSL 基线

本节用于约束支付工具、资金账户和信用账户定性后的 DSL 约定、路由规则、账目平衡、余额投影和交易投影。触碰这些对象时，必须先按本表检查；不满足时只能进入设计修正或 TDD 分析，不得顺手改公共契约、测试资源、DDL/H2 schema 或生产代码。

Highnote 公开发卡文档中的 financial account、ledger、ledger entry、payment card 和 financial account activity 分层，可作为本 DSL 的外部参考确认：资金和账本落在账户，卡只是访问工具，账户活动和交易事件承担卡维度归因。wind-funds DSL 因此坚持“账户入账、工具归因、控制留痕、投影查询”：`SubjectRef` 决定可入账主体，`PaymentInstrumentRef` 和 binding snapshot 决定工具归因，Spend Rule / 预算控制只产出控制证据和只读投影输入。

模块归属约束：Spend Rule 的规则定义、版本、挂载、决策日志、准入、控制活动和预算控制视图归属于 `wallet` 支出控制域；`transaction` 只消费已固化 `spendRuleDecision`、控制活动引用和 route snapshot 做历史投影解释；`ledger` 只接受可入账账户主体。交易模块不得直接依赖 wallet Spend Rule application service、DAL Entity 或 Mapper 来计算、更新或解释规则。

| 设计面 | 对齐口径 | 必须保持 | 工程影响 |
| --- | --- | --- | --- |
| DSL 主体约定 | `SubjectRef` 只承载资金账户、信用账户和平台角色解析后的平台资金账户；`PaymentInstrumentRef`、`ExternalAccountRef` 只承载工具、外部账户和脱敏引用；预算组和 Spend Rule 只承载 scope、规则快照、控制窗口和审计上下文。 | 不新增 `InstrumentTransaction`、`PaymentInstrumentTransaction` 或支付工具账务主体；内部余额钱包、平台钱包、商户钱包、返利钱包和信用额度入口先解析为 `SubjectRef`、`BenefitSnapshot`、`FundingAllocationDecision` 或等价不可变快照。 | 触碰 `core` 枚举、Spec、fixture 或公共 DSL 字段时，必须显式声明公共契约边界和 `fixtureLevel`。 |
| 路由规则 | route resolver 可以消费支付工具快照、绑定快照、`FundingAllocationDecision`、预算组上下文和 Spend Rule 决策，但 route leg participant 必须是最终可入账主体。 | 工具不可用、资金责任不唯一、错币种、预算或规则拒绝时不生成 route；退款、撤销、拒付、退费和重放优先沿原 route snapshot。 | 支付工具入口、资金责任解析和交易投影应分别形成独立任务，不借直接交易红线附带修改。 |
| 账目平衡 | `PostingPlan` 只从已解析 route 生成；`LedgerEntry.subject` 只能是资金账户、信用账户或平台角色解析后的平台资金账户；每个 posting plan 按同币种独立平衡。 | 预算组、Spend Rule、支付工具、外部账户和交易投影不得生成 ledger bucket；预算控制只生成控制证据、规则证据或只读投影视图。 | 触碰 posting assembler、账本 DSL 或账务表行时，必须补借贷平衡、`normalBalanceSide`、余额桶和 forbidden facts 断言。 |
| 余额投影 | 账本余额投影只从 ledger entry 派生，面向资金账户、信用账户和平台角色解析后的平台资金账户；余额日志只作为观察证据。 | 不从支付工具、预算组、Spend Rule、交易投影或业务轨道事件直接投影账本余额；预算控制可有独立控制视图，但不等于账本余额。 | BudgetGroup 兼容策略、预算控制视图和余额查询迁移必须拆成独立任务。 |
| 交易投影 | 交易投影是只读查询模型，从交易事实、冻结单、route snapshot、`paymentInstrumentRef`、`FundingAllocationDecision`、已固化 `spendRuleDecision` 快照、既有控制活动、账本摘要、授权拒绝事实、清结算和对账差错生成；可以按支付工具、账户、预算组、Spend Rule 查询或过滤。支付工具型交易解释只能读取已固化的 `paymentInstrumentRef` 和 binding snapshot；Spend Rule 解释只能读取已固化的规则、版本、挂载和决策引用。 | 交易投影不能作为资金来源、入账主体、路由事实或余额事实；重投影只能重建读模型，不得反写 route、posting、entry 或 balance；授权拒绝只能形成拒绝解释，不生成资金事实；不得按当前工具绑定、当前规则定义或当前规则挂载重新解释历史交易，也不得在解释阶段执行规则 DSL 或脚本。 | 不得用交易投影通过来声明账务事实、余额投影或生产 Done；补支付工具解释时必须验证脱敏展示号、绑定版本、准入决策和敏感原文不外泄；补 Spend Rule 解释时必须验证规则版本、挂载版本、决策流水和拒绝原因可追溯，并证明 ruleSpec/script 不外泄。 |

### Spend Rule DSL v1.1 规则版本、挂载和决策证据

Spend Rule DSL v1.1 只作为规则事实和控制证据契约，不作为规则引擎实现、Controller 报文或数据库结构。它锁定三个对象：不可变规则版本、规则挂载和决策证据。

#### 规则版本 DSL

```json
{
  "dslCaseId": "DSL-SPEND-RULE-VERSION-001",
  "fixtureLevel": "DOC_ONLY",
  "specType": "SpendRuleVersionSpec",
  "ruleId": "SR-VCC-DAILY-USD-001",
  "version": "v1",
  "ruleType": "PERIOD_AMOUNT_LIMIT",
  "ruleDomain": "AUTHORIZATION",
  "display": {
    "ruleName": "VCC USD daily spend limit",
    "operatorReasonTemplate": "Daily spend limit exceeded",
    "customerReasonTemplate": "The payment is above the daily card limit"
  },
  "matchSpec": {
    "businessScenes": ["VCC_AUTHORIZATION"],
    "paymentInstrumentTypes": ["VCC"],
    "currencies": ["USD"],
    "merchantCategory": {
      "mode": "ALLOW_LIST",
      "values": ["5812", "5814"]
    },
    "countryRegion": {
      "mode": "DENY_LIST",
      "values": ["XX"]
    }
  },
  "counterSpec": {
    "counterScope": "PAYMENT_INSTRUMENT",
    "windowMode": "CALENDAR_DAY",
    "timezone": "UTC",
    "aggregationBasis": "AUTHORIZED_AMOUNT",
    "nettingPolicy": "NET_REFUNDS_TO_ORIGINAL_WINDOW"
  },
  "limitSpec": {
    "amountLimit": {
      "amount": "1000.00",
      "currency": "USD"
    },
    "countLimit": {
      "maxCount": 20
    }
  },
  "decisionSpec": {
    "decisionWhenPassed": "ALLOW",
    "decisionWhenViolated": "DECLINE",
    "decisionWhenEvidenceMissing": "REVIEW",
    "violationReasonCode": "DAILY_LIMIT_EXCEEDED"
  },
  "safetySpec": {
    "unknownFieldPolicy": "FAIL_CLOSED",
    "sensitiveFieldPolicy": "DIGEST_OR_MASK_ONLY",
    "historyReplayPolicy": "USE_VERSION_AND_ASSIGNMENT_SNAPSHOT",
    "digestAlgorithm": "SHA-256"
  },
  "versionDigest": "sha256:version-body"
}
```

字段口径：

| 字段 | 含义 | 验收口径 |
| --- | --- | --- |
| display | 可读名称和展示原因。 | 不保存完整 PAN、token、外部账户号或商户敏感原文。 |
| matchSpec | 请求事实匹配条件。 | 不能空对象默认全量放行；缺必要证据时进入 `decisionWhenEvidenceMissing`。 |
| counterSpec | 周期窗口和累计口径。 | 周期类规则必须明确窗口、时区、累计依据和退款净额策略。 |
| limitSpec | 金额、次数或集合限制。 | 金额必须带币种，集合必须说明 allow list 或 deny list。 |
| decisionSpec | 通过、违反和缺证据的裁决。 | 不允许缺省放行。 |
| safetySpec | 未知字段、敏感字段和历史回放策略。 | 默认 fail closed，历史解释使用快照和摘要。 |

#### 规则挂载 DSL

```json
{
  "dslCaseId": "DSL-SPEND-RULE-ASSIGNMENT-001",
  "fixtureLevel": "DOC_ONLY",
  "specType": "SpendRuleAssignmentSpec",
  "assignmentId": "ASG-VCC-001",
  "ruleId": "SR-VCC-DAILY-USD-001",
  "version": "v1",
  "scopeRef": {
    "scopeType": "PAYMENT_INSTRUMENT",
    "scopeId": "PI-VCC-10001"
  },
  "priority": 10,
  "conflictPolicy": "DENY_OVERRIDES",
  "effectiveWindow": {
    "effectiveFrom": "2026-06-22T00:00:00Z",
    "effectiveTo": "2026-07-22T00:00:00Z"
  },
  "assignmentDigest": "sha256:assignment-body"
}
```

挂载 DSL 只表达控制 scope，不输出资金责任主体。支付工具、预算组、账户层级、使用主体和业务场景都可以成为控制 scope；最终 route leg 和 LedgerEntry subject 仍必须由资金账户、信用账户或平台角色解析后的平台资金账户承担。

#### 决策证据 DSL

```json
{
  "dslCaseId": "DSL-SPEND-RULE-DECISION-001",
  "fixtureLevel": "DOC_ONLY",
  "specType": "SpendRuleDecisionEvidenceSpec",
  "decisionSn": "SRD-20260622-0001",
  "businessScene": "VCC_AUTHORIZATION",
  "businessSn": "AUTH-20260622-0001",
  "requestDigest": "sha256:request-facts",
  "decisionPolicy": "DENY_OVERRIDES",
  "finalDecision": "DECLINE",
  "decisionReasonCode": "MERCHANT_CATEGORY_BLOCKED",
  "decisionReasonMessage": "Merchant category is not allowed for this card",
  "evaluatedRules": [
    {
      "assignmentId": "ASG-VCC-001",
      "ruleId": "SR-VCC-DAILY-USD-001",
      "version": "v1",
      "decision": "ALLOW",
      "reasonCode": "WITHIN_DAILY_LIMIT",
      "matchedFacts": {
        "amount": "35.00",
        "currency": "USD",
        "remainingAmount": "965.00"
      },
      "ruleDigest": "sha256:version-body"
    },
    {
      "assignmentId": "ASG-MCC-001",
      "ruleId": "SR-MCC-DENY-001",
      "version": "v3",
      "decision": "DECLINE",
      "reasonCode": "MERCHANT_CATEGORY_BLOCKED",
      "matchedFacts": {
        "merchantCategoryCode": "7995"
      },
      "ruleDigest": "sha256:mcc-rule-body"
    }
  ],
  "missingEvidence": [],
  "forbiddenFacts": [
    "NO_FUNDS_TRANSACTION_CREATED",
    "NO_ROUTE_CREATED",
    "NO_LEDGER_ENTRY_CREATED",
    "NO_BALANCE_PROJECTION_CHANGED"
  ],
  "decisionDigest": "sha256:decision-body"
}
```

决策证据验收口径：

1. `finalDecision` 是准入结论，`evaluatedRules` 是解释材料；两者都必须保留。
2. 多规则冲突必须记录 `decisionPolicy`，不能只保存最后一条命中规则。
3. 拒绝或待复核时必须能证明无资金事实副作用；文档 DSL 只表达目标断言，真实证明需要后续服务层测试。
4. 交易投影解释只能读取历史 `decisionSn`、规则版本、挂载摘要和决策摘要，不按当前规则重算。

场景覆盖：

| 场景 | 规则版本字段 | 挂载字段 | 决策证据字段 |
| --- | --- | --- | --- |
| 单笔限额 | `limitSpec.amountLimit` | `scopeRef=PAYMENT_INSTRUMENT` | `evaluatedRules.matchedFacts.amount` |
| 周期金额限额 | `counterSpec` + `limitSpec.amountLimit` | `scopeRef=BUDGET_GROUP / ACCOUNT_HIERARCHY / PAYMENT_INSTRUMENT` | `remainingAmount`、`decisionReasonCode` |
| 周期次数限额 | `counterSpec` + `limitSpec.countLimit` | `priority`、`conflictPolicy` | `remainingCount` 或等价摘要 |
| MCC / 商户限制 | `matchSpec.merchantCategory` 或商户摘要 | `scopeRef=PAYMENT_INSTRUMENT / BUSINESS_SCENE` | `MERCHANT_CATEGORY_BLOCKED` |
| 国家地区限制 | `matchSpec.countryRegion` | `scopeRef` | 地区命中摘要 |
| 多规则裁决 | `decisionSpec` | `priority`、`conflictPolicy` | `decisionPolicy`、`evaluatedRules`、`finalDecision` |

### DSL 易用性和误用防护

DSL 既要让研发能实现，也要让产品、测试、运营和审计能用同一份契约解释资金事实。新增或修改 DSL 对象时，除字段完整外，还要检查是否容易被误用。

| 检查项 | 必须满足 | 误用信号 |
| --- | --- | --- |
| 字段语义 | 每个核心字段能说明是输入事实、路由决策、账务结果、只读投影还是审计引用。 | 字段名看起来像状态，但实际承载账目、周期或外部流程。 |
| 失败解释 | 失败样例必须能说明失败阶段、失败原因、是否可重试、是否人工处理和无副作用证据。 | 只有 `success=false` 或错误码，无法解释 route/posting/entry 是否产生。 |
| 展示支撑 | route、posting、benefit、replay、clearing、reconciliation 和 governance 对象要能支撑用户账单、商户账单或运营时间线摘要。 | DSL 能入账但不能解释为什么扣款、为什么待清算、为什么阻断。 |
| 敏感边界 | 支付工具、外部账户、凭证、规则来源和证据引用必须保存摘要、脱敏值或引用 ID。 | JSON 示例出现完整卡号、CVV、token、密钥、证件原文或无边界凭证。 |
| 回放稳定 | 逆向、退款、撤销、退费、拒付、清结算重跑和对账差错必须能依赖原快照或等价不可变摘要。 | 请求态字段能表达，但历史回放没有稳定事实源。 |

## JSON 使用边界

本文档写给人读，优先用中文叙述、表格和流程图解释设计目标、语义边界和流程。只有真正的 DSL 契约对象和场景夹具使用 `json` 代码块，便于表达可验证的业务事实。

文档中的 JSON 是契约表达，不等同于 Controller 报文或数据库结构。设计目标、总体流程、评审清单和禁止清单不应强行 JSON 化。

## 结论

本 DSL 设计是支付资金底座的稳定承载层设计基线，可用于产品评审、系分设计、开发任务拆解、契约测试和 TDD 验收设计；不直接替代合规确认或财务制度确认。

## 机器契约准入口径

DSL 文档中的结构、表格和 JSON 示例只有在进入测试资源、被测试读取并通过验证后，才能作为机器契约完成证据。新增或修改 `DSL-*` caseId 时，必须按下列口径判定。

| 场景 | 可声明的结论 | 不可声明的结论 |
| --- | --- | --- |
| 只在文档中新增或调整 caseId | 设计语义已定义，可进入系分和 TDD 拆解。 | 机器契约已通过。 |
| 在 `tests/src/test/resources/dsl-contract-cases/` 落契约夹具并被测试读取 | DSL 契约具备可执行验收入口。 | 已覆盖资金流、route/posting/replay、清结算、对账、投影、归档、冷热读取或治理重放，除非夹具显式包含资金流断言并同步覆盖对应 AC/TDD/RED。 |
| 在 `tests/src/test/resources/dsl-contract-cases/` 落资金流夹具并被测试读取 | 夹具覆盖范围内的 route、posting、余额、投影或治理断言具备可执行验收入口。 | 已覆盖所有生产路径，除非同步覆盖该路径对应的 AC/TDD/RED、失败无副作用和残余风险。 |
| 复用已有 DSL 样例 | 复用范围内的语义可作为局部证据。 | 未覆盖差异自动视为已验证。 |
| 工程任务范围不触碰 DSL 测试资源 | 可以作为文档评审结论。 | 新增 caseId 已具备生产契约通过证据。 |

### DSL caseId 执行化盘点

工程落地前，所有新增、修改或用于验收声明的 `DSL-*` caseId 都必须做执行化盘点。盘点表不要求一次覆盖全部历史 caseId，但必须覆盖工程任务声明的目标范围和所有被交付说明引用的 caseId。

| 盘点字段 | 填写要求 |
| --- | --- |
| `dslCaseId` | 对应 DSL 文档中的稳定 caseId；同一语义不要新起临时编号。 |
| `acceptanceId` | 对应 PRD 的 `AC-*`、`RED-*` 或业务红线编号；无 PRD 验收时只能声明设计补充。 |
| `fixturePath` | 对应 `tests/src/test/resources/dsl-contract-cases/` 下的 JSON 或 YAML 夹具路径；没有夹具时填写 `NONE`。 |
| `fixtureLevel` | 使用 `DOC_ONLY`、`CONTRACT_ONLY`、`FUNDS_FLOW`、`SERVICE_FLOW` 或 `GOVERNANCE_FLOW` 分级。 |
| `targetTestClass` | 读取该 fixture 或承接该 caseId 的目标测试类；尚未落测试时填写目标类名和未覆盖说明。 |
| `coreAssertions` | 必须列出状态、route snapshot、posting plan、ledger entry、余额投影、交易投影、幂等、审计、失败无副作用或治理只读边界中的适用项。 |
| `notDone` | 未覆盖资金流、服务流、DDL/H2、外部规则、敏感数据、并发、人工处理或生产 NFR 时必须显式写明。 |

`fixtureLevel` 的结论口径如下：

| 级别 | 含义 | 可声明 | 不可声明 |
| --- | --- | --- | --- |
| `DOC_ONLY` | 只在 DSL 或 PRD 文档中定义。 | 语义已进入设计基线，可进入系分或 TDD 拆解。 | 机器契约通过、资金流已验证或生产完成。 |
| `CONTRACT_ONLY` | 有测试资源，且测试只验证字段结构、枚举、必填或兼容。 | 契约结构可执行。 | route、posting、余额、投影、清结算、对账、归档或治理已验证。 |
| `FUNDS_FLOW` | fixture 被资金主链路测试读取，并断言 route、posting、entry、余额和幂等中的适用项。 | 夹具覆盖范围内的资金流证据成立。 | 清结算、对账、治理或所有生产路径自动成立。 |
| `SERVICE_FLOW` | fixture 或等价数据被服务级 H2 流程读取，并覆盖状态机、持久化、查询和失败无副作用。 | 夹具覆盖范围内的服务级行为可作为生产交付证据之一。 | 未覆盖的外部规则、容量、并发、审计和 NFR 自动成立。 |
| `GOVERNANCE_FLOW` | fixture 被归档、重放、差异报告、人工处理或指标水位隔离测试读取。 | 治理边界内的只读、dry-run/apply、checkpoint 或差异处理证据成立。 | 治理任务可以反写资金事实，或替代账本余额确认。 |

交付说明引用 DSL 证据时，必须同步列出 `dslCaseId -> fixturePath -> targetTestClass -> coreAssertions -> notDone`。未完成盘点时，DSL 只能作为设计依据，不能作为机器契约或生产交付证据。

执行化盘点使用下列表头作为工程任务附件：

| dslCaseId | acceptanceId | fixturePath | fixtureLevel | targetTestClass | coreAssertions | notDone |
| --- | --- | --- | --- | --- | --- | --- |

盘点完成后再做三项复核：

1. `fixtureLevel` 是否与可声明结论一致，不能用低等级 fixture 支撑高等级交付结论。
2. `targetTestClass` 是否真的读取该 fixture 或等价数据，不能只在测试名中引用 caseId。
3. `coreAssertions` 是否覆盖目标资金变化的最小证据；缺 route、posting、entry、projection、幂等或审计时，必须在 `notDone` 中说明。

### DSL 基线核验清单

DSL 基线核验只确认 caseId、字段语义、fixture 等级和可测断言是否足以进入 TDD 分析，不新增测试资源、不修改公共契约、不补生产字段。发现 DSL 不能承接资金事实时，应先回到 PRD 或 DSL 设计补语义，再进入工程落地评审。

| 核验项 | 必须确认 | 未闭合时处理 |
| --- | --- | --- |
| caseId 归属 | 交付引用的 `DSL-*` 都能反查 PRD 验收、系分入口和 TDD 用例。 | 标为 `DOC_ONLY`，不得声明机器契约通过。 |
| 事实字段 | `instructionType`、`eventType`、`transactionType`、`businessScene`、主体、金额、币种和业务引用稳定。 | 回到 DSL 设计补字段语义或拆分场景。 |
| 路由和账务 | route snapshot、posting plan、LedgerEntry、余额投影和交易投影的适用断言清楚。 | 不得进入资金流实现；只能作为设计输入。 |
| fixture 等级 | `DOC_ONLY`、`CONTRACT_ONLY`、`FUNDS_FLOW`、`SERVICE_FLOW`、`GOVERNANCE_FLOW` 与可声明结论一致。 | 降级交付结论，或在工程任务中补测试资源写入范围。 |
| 失败边界 | 拒绝、余额不足、错币种、缺快照、权限不足、规则待确认和重复请求的无副作用语义明确。 | 补 must-fail case 或写明未覆盖范围。 |
| 敏感和外部证据 | 外部账户、支付工具、规则来源、凭证和审计证据只保留摘要、脱敏值或引用。 | 清理敏感示例；缺规则状态时不得作为生产完成证据。 |
| 测试承接 | `targetTestClass`、核心断言和验证命令能定位；未落测试时 `notDone` 明确。 | 只能进入 TDD 分析或契约草案，不能进入生产交付结论。 |
