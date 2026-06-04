# B2/B4 支付工具与 Spend Rule 生产可用性 Round 0 准入卡

## 1. 文档定位

本文档是支付工具与 Spend Rule 生产可用性的 Round 0 候选准入卡。它把 PRD、DSL、系分、TDD、OpenSpec、A0 准入裁决和当前代码观察收敛成一张可评审、可拆分、可转成 Execution Grant 的输入页。

本文档不授权修改生产代码、测试代码、DDL/H2 schema 或运行时配置。只有用户确认本页或确认经调整后的单一 MVP Execution Grant 后，才允许把本文档中的 Red 候选转成实际测试写入。

## 2. authorityBaseline

| 基线项 | 当前口径 |
| --- | --- |
| 设计和任务基线 | 最新已提交设计和任务对齐输入以确认时 Git HEAD 为准；当前未提交文档变更若要作为开工依据，必须先提交或在 Execution Grant 的 `authorityBaseline` 中显式列入。 |
| 产品入口 | `docs/产品设计/02-交易路由钱包账目与投影.md` 的支付工具能力控制、支付工具与 Spend Rule 生产可用性裁决、交易投影只读边界；`docs/产品设计/06-VCC发卡业务资金底座PRD.md` 的预付卡、共享卡交易服务能力包。 |
| DSL 入口 | `docs/DSL设计/README.md` 的支付工具和账户能力重定性后的 DSL CR 基线，以及 `docs/DSL设计/支付资金底座DSL承载层设计.md` 的 route、posting、projection 和 JSON 契约。 |
| 系分入口 | `docs/系分设计/02-交易路由钱包账目与投影系分设计.md` 的钱包账户设计、支付工具与 Spend Rule 生产可用性差距、application facade 分层和服务边界。 |
| TDD 入口 | `docs/TDD设计/支付资金底座测试驱动设计.md` 的 TDD-WALLET-018、TDD-WALLET-019、12.2 Round 0 Red 集合。 |
| OpenSpec 入口 | `openspec/specs/payment-funds-foundation/spec.md` 的支付工具交易入口、支付工具与 Spend Rule 生产可用性、支出主体资金责任解析关系术语；`openspec/changes/tdd-baseline-reset/tasks.md` 的 2026-06-01 生产可用性 CR。 |
| 现有代码证据 | `PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests`、`PaymentInstrumentRouteDslContractTests` 已作为局部基线；只证明资源服务、现有资金责任关系和 DSL 契约，不证明生产交易入口可用。 |
| B4 授权内核基线 | 截至 `967586c` 和 `47c5269`，账户主体型无授权退款路由回退与授权后继并发竞争已闭合；`FundsAuthorizationTransactionService#authorize` 和 `FundsAuthorizationTransactionAuthorizeRequest.accountId` 仍是 canonical 授权内核。当前未发现 `AuthorizationAdmissionApplicationService` 或 `authorizeByInstrument` 生产入口，B4-AUTH-PI 只能作为 Round 0 / Grant 候选。 |
| 外部参考确认 | Highnote 的 financial account / ledger / payment card / transaction feed 分层只作为设计参考：账户入账、工具归因、activity 或 projection 做卡维度流水。wind-funds 不照搬对象名，不新增卡账本或工具交易内核。 |

## 3. grantCandidate

| 字段 | 候选取值 |
| --- | --- |
| `mvpScenario` | 共享卡或 VCC 授权前准入：业务提交支付工具引用、使用主体、预算组或 Spend Rule 上下文和金额币种，系统完成工具能力准入、绑定快照、资金责任解析、规则决策、账户能力校验；批准后委派账户主体型授权内核，拒绝时无 route、posting、LedgerEntry。预付卡只在单独切片中验证“入金先确认、责任主体唯一、未确认不加余额”。 |
| `abilityBatch` | 优先拆成 B2 Round 0 和 B4 Round 0。B2 处理支付工具能力准入、绑定快照、资金责任目标主体；B4 处理 `authorizeByInstrument` 或等价授权准入组合；B5/B6/B8 只在预算预留释放、交易投影和治理重放被明确授权时进入。 |
| `businessQuestion` | 企业管理员、运营和财务能否解释一笔共享卡或 VCC 授权为什么通过或拒绝，能否解释预付卡入金为什么可用或被阻断，且能证明支付工具、预算组和 Spend Rule 只提供准入、规则和审计证据，不成为资金主体。 |
| `moneyFact` | 批准时资金影响仍落在资金账户、信用账户或平台角色解析后的平台资金账户；预算组和 Spend Rule 只写控制活动、规则决策、预留释放证据和只读投影。 |
| `productNotDone` | 不声明完整 VCC 发卡、完整支付工具交易入口、完整 Spend Rule 引擎、预算并发控制生产实现、清结算对账、卡组织/处理商/PCI/ACH/SWIFT/FX 规则确认或 P2 生产资金流完成。 |
| `firstRedSet` | `R0-PI-001`、`R0-FR-001` 优先；若选择 B4 再补 `R0-AUTH-001`；若选择预付卡资金切片再补 `R0-VCC-PREPAID-001`；若选择清算/逆向切片再补 `R0-VCC-LC-001`；若选择 B5/B6/B8 再补 `R0-SR-001`、`R0-SR-002`、`R0-PI-002`。 |
| `currentEvidence` | 三个既有测试命令通过证据只能作为回归资产和局部代码基线；Round 0 新 Red 必须证明当前缺口或确认已有实现已覆盖，不能直接把既有通过测试升级为生产可用。 |
| `highnoteMappingGuardrail` | 一张或多张共享卡绑定同一资金账户时，余额断言必须落到账户 ledger；卡维度断言必须落到交易投影、绑定版本和控制快照；不得要求 `LedgerEntry.subject`、余额投影主体或 route leg 使用卡、卡组、持卡人、预算组或 Spend Rule。 |

### 3.0 architectureReviewMap

| 架构审查项 | 本卡落点 |
| --- | --- |
| 背景、目标、非目标、成功标准 | 背景是支付工具、Spend Rule、VCC 和授权入口目标态已经完成设计收敛，但生产可用性不能由资源服务测试直接推导；目标是把 B2/B4/P2 可编码候选拆成单一 Execution Grant 输入；非目标是不混入清结算、对账、治理、完整发卡产品或外部轨道协议；成功标准是每个候选 Red 都能追溯到产品、DSL、系分、TDD、写入范围和停止条件。 |
| 现状、约束、问题和影响范围 | 现状是支付工具资源服务、资金责任关系和 DSL 契约已有局部基线；约束是交易 canonical 内核继续以已解析账户主体作为入参；问题是缺 application facade、资金责任唯一决策、动作能力准入和 Spend Rule 决策证据时不能宣称生产可用；影响范围覆盖 wallet-face、wallet-impl、transaction 内核委派、route snapshot、投影和边界测试。 |
| 核心决策、职责边界和取舍 | 核心决策是支付工具只做准入、快照和归因，资金影响仍落到账户主体；钱包 application facade 负责业务入口和资源编排，交易 canonical 内核负责账户主体型资金事实；取舍是优先 B2-PI-CAP、B2-FR 或 B4-AUTH-PI 单切片，不一次性打开 VCC、Spend Rule、B5/B6/B8 全部目标态。 |
| 接口契约、入参、错误码、幂等和兼容 | 后续 Execution Grant 必须显式列出 application service 名称、Request/DTO、依赖方向、动作能力字段、资金责任目标字段策略、错误码、幂等摘要和兼容策略；未确认前不修改 face/core 公共契约，不把支付工具引用塞进账户主体型交易请求。 |
| 数据方案、事务边界、一致性、补偿和对账 | B2-FR 必须在 `funding-account-only` 和 `targetSubjectType + targetSubjectId` 中二选一；涉及 DDL/H2、Entity、Mapper、摘要、fixture、route snapshot 或回放断言时必须扩权；失败路径必须证明无 route、posting、LedgerEntry、余额投影和不可核对投影副作用，对账、清结算和治理只作为后续独立域。 |
| 可靠性、安全、权限、审计和告警 | 工具准入、绑定版本、资金责任、Spend Rule 决策和拒绝原因必须可审计；敏感字段不得进入普通上下文、日志或投影；权限、告警、生产开关和外部规则核验只作为后续 Execution Grant 待确认项。 |
| 验证方案、测试、静态检查和回归 | 每个候选切片必须先写目标 Red，再按 Grant 跑对应 `just test-one ...`、`just test-transaction`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`；既有通过测试只能作为回归资产。 |
| 发布、灰度、回滚、风险和待确认 | 本卡不进入生产发布；若后续触碰公共契约、DDL/H2、外部规则、敏感数据、完整 VCC 生命周期、清结算对账或治理，必须停止并补发布、灰度、回滚、风险和待确认项。 |

### 3.0.1 productReviewMap

| 产品审查项 | 本卡落点 |
| --- | --- |
| 业务目标、用户价值、成功指标和非目标 | 业务目标是让共享卡、VCC 和内部钱包类入口在进入资金事实前具备可解释准入；用户价值是运营、财务和研发能看懂通过、拒绝和资金责任来源；成功指标是候选 Red 能证明准入、委派和失败无副作用；非目标是不声明完整发卡、清结算对账、外部轨道协议或 Spend Rule 引擎生产完成。 |
| 能力地图、能力域、前台能力、后台能力和数据能力 | 能力地图拆为支付工具能力准入、资金责任解析、授权 application facade、VCC 预付资金、VCC 生命周期、Spend Rule 决策和投影解释；前台能力接收工具引用和业务上下文，后台能力提供配置、审批和差错入口，数据能力提供快照、审计、指标和只读投影。 |
| 业务对象、对象模型、字段口径、生命周期和状态 | 业务对象包括支付工具、绑定版本、资金责任决策、Spend Rule 决策、授权准入请求、预付资金请求和生命周期事件；字段口径必须区分工具引用、账户主体、预算上下文、规则版本、幂等摘要和审计字段；状态只表达准入、拒绝、委派、确认和失败，不把工具或预算组变成账务主体。 |
| 业务流程、主流程、异常流程和人工兜底 | 主流程是业务入口提交工具引用和金额，application facade 完成工具、绑定、资金责任、规则和账户能力校验后委派账户主体型内核；异常流程包括能力不匹配、责任不唯一、规则拒绝、快照缺失和敏感字段阻断；人工兜底只形成审批、差错或审计处理，不直接写资金事实。 |
| 规则矩阵、触发条件、判断逻辑、优先级和版本 | 规则矩阵由工具状态/方向/动作能力、绑定版本、资金责任唯一性、Spend Rule 决策、账户能力、币种和敏感字段组成；触发条件和判断逻辑必须进入 Red 候选；优先级和版本由 Execution Grant 决定，未确认前不得宣称生产规则引擎完成。 |
| 运营后台、指标、报表、审计和数据口径 | 运营后台只能查看准入结果、拒绝原因、绑定版本、规则版本、责任主体、幂等摘要和差错引用；指标和报表只消费只读投影和审计数据口径；审计记录不得包含完整敏感凭证，且不能作为补造资金事实的入口。 |
| 风险、待确认、验收、确认方和发布 | 风险是把支付工具、预算组或 Spend Rule 误当资金主体，或把局部资源服务测试误判为生产可用；待确认项包括接口命名、字段策略、DDL/H2、外部规则和敏感数据边界；验收由产品、架构、研发和财务口径共同确认；发布、灰度和回滚只在单独 Execution Grant 中补齐。 |

### 3.1 fundingResponsibilityDecision

B2-FR 进入 Execution Grant 前必须先选择资金责任目标字段策略，且一次 Grant 只能选择一个策略。

| 策略 | 允许声明 | 必须验证 | 不允许声明 |
| --- | --- | --- | --- |
| `funding-account-only` | 支付工具、使用主体、预算组或 Spend Rule 上下文只能解析到资金账户。 | `fundingAccountId` 语义明确、关系唯一、币种一致、失败无 route/posting/entry；信用账户、平台角色、预算组和 Spend Rule 不会被塞进 `fundingAccountId`。 | 不声明信用账户、平台角色解析后的平台资金账户或多责任主体已经生产可用。 |
| `targetSubjectType + targetSubjectId` | 资金责任结果可以是资金账户、信用账户或平台角色解析后的平台资金账户。 | Request/DTO、DDL/H2 schema、Entity、Mapper、摘要、route snapshot、TDD fixture、回放和审计同步；`fundingAccountId` 只作为兼容字段或派生字段。 | 不允许只改文档或测试断言，代码仍长期只接受 `fundingAccountId`。 |

## 4. 场景裁剪

| 场景 | 本卡允许进入 Round 0 的内容 | 本卡不允许声明 |
| --- | --- | --- |
| VCC 或共享卡授权 | 支付工具准入、绑定快照、资金责任解析、Spend Rule 决策、账户能力校验、拒绝无副作用和委派账户主体型授权内核。 | 完整发卡产品、卡组织授权协议、PAN/CVC/PCI、卡账单全链路、processor 账户状态。 |
| 预付卡外部充值 | 可作为独立预付资金切片的 Red 输入：验证外部入金或系统内充值必须有确认引用、责任主体和幂等摘要，不能把 prepaid virtual card 当资金账户或账本主体。 | 在未获单独 Execution Grant 时实现充值、清算、外部回单、退卡提现或预付资金账户创建。 |
| 共享卡和预付卡清算/逆向 | 可作为清算/逆向切片的设计输入：验证 settle、release、refund、chargeback 必须引用原授权、原 route snapshot 和原资金责任决策。 | 在本卡中实现完整 clearing 文件处理、chargeback 全生命周期、费用和 FX 自动入账。 |
| VA 收款 | 仅验证 VA 是收款识别工具和外部引用，不是账本主体。 | 在本卡中实现银行流水匹配、到账确认、对账或入金直接交易生产链路。 |
| 内部余额钱包支付 | 验证内部入口先解析为 `SubjectRef` 或资金责任决策，不强制包装为支付工具。 | 新增内部钱包支付工具类型或绕过资金账户能力校验。 |
| 全球账户付款、ACH 或银行转账 | 只作为 P2 业务能力包和外部轨道边界输入。 | 实现 SWIFT、local rail、ACH/Nacha、银行协议、FX quote 或外部非终态处理。 |

## 4.1 interfacePlacementCandidate

以下落包只作为编码准入候选，不等于授权写入。若进入编码，Execution Grant 必须逐项确认接口名、Request/DTO、错误码、依赖方向和验证命令。

| 能力 | 候选接口 | face 包 | impl 包 | 准入切片 |
| --- | --- | --- | --- | --- |
| 支付工具能力准入 | `PaymentInstrumentCapabilityApplicationService` | `com.wind.funds.wallet.application.instrument` | `com.wind.funds.wallet.application.instrument.impl` | B2-PI-CAP |
| 资金责任解析 | `FundingResponsibilityResolutionApplicationService` | `com.wind.funds.wallet.application.funding` | `com.wind.funds.wallet.application.funding.impl` | B2-FR |
| 授权支付工具入口 | `AuthorizationAdmissionApplicationService` | `com.wind.funds.wallet.application.instrument` | `com.wind.funds.wallet.application.instrument.impl` | B4-AUTH-PI |
| Spend Rule 控制准入 | `SpendRuleControlApplicationService` | `com.wind.funds.wallet.application.spendrule` | `com.wind.funds.wallet.application.spendrule.impl` | B5-SR-CONTROL |
| 授权后清算/释放/逆向 | `InstrumentTransactionLifecycleApplicationService` | `com.wind.funds.wallet.application.instrument` | `com.wind.funds.wallet.application.instrument.impl` | P2-VCC-LIFECYCLE |
| VCC 预付资金处理 | `VccPrepaidFundingApplicationService` | `com.wind.funds.wallet.application.vcc` | `com.wind.funds.wallet.application.vcc.impl` | P2-VCC-PREPAID |
| VCC 共享卡场景编排 | `VccSharedCardTransactionApplicationService` | `com.wind.funds.wallet.application.vcc` | `com.wind.funds.wallet.application.vcc.impl` | P2-VCC-LIFECYCLE |

Request/DTO 默认落 `com.wind.funds.wallet.model.request` 和 `com.wind.funds.wallet.model.dto`；若模型数量超过单一切片需要，可在 Execution Grant 中允许增加 `instrument`、`funding` 或 `vcc` 子包。禁止新增顶层 `com.wind.funds.instrument`，禁止让 `transaction-impl` 反向依赖钱包资源服务。

## 4.2 transactionLayerCandidate

交易层需要继续完善账户主体型能力，但本卡不把这些能力包装成支付工具交易入口。交易层后续可独立进入 B3/B4/B5/B6 Execution Grant；支付工具 facade 只作为触发方、快照提供方或投影归因方。

| 交易层能力 | 是否可后续完善 | 典型写入范围 | 禁止混入 |
| --- | --- | --- | --- |
| 授权过期释放 | 已完成 B4-TRX-EXPIRE 基础能力，后续只作为回归基线或扩展切片。 | `FundsAuthorizationTransactionService#expire`、`FundsAuthorizationTransactionExpireRequest`、`EXPIRE` 事件、transaction-impl、route replay、授权流测试已由 `b0666ba` 闭合。 | 支付工具主体入参、卡账本、预算组入账；不得借过期释放扩展强制完成、无授权退款或 VCC 生命周期。 |
| 受控强制完成 | 已完成 B4-FORCE-SETTLE 首轮能力，后续只作为回归基线或扩展切片。 | settle 请求、策略字段、审计字段、金额边界测试已由 `616dac1` 和 `3825466` 闭合。 | 用强制完成伪造授权占用或绕过原路径；不得借回归扩展策略引擎、审批流或支付工具入口。 |
| 无授权直接退款 | 已完成 B4-NO-AUTH-REFUND 首轮能力，后续只作为回归基线或扩展切片。 | `authorizationTransactionSn` 空值语义、`externalReferenceSn`、退款原因、操作者/审计、`NO_AUTH` 内部上下文标签、外部引用路由回退和失败无副作用测试已由 `006bcaa`、`818da34` 和 `967586c` 闭合。 | 缺外部引用、缺原因或缺审计仍静默退款，携带内部授权流水，按当前工具绑定选路；不得借回归扩展运营审批、累计退款控制或完整 dispute case。 |
| 拒付和争议扣回 | 可以，B4 或 P2-VCC-LIFECYCLE 切片。 | chargeback 或等价逆向请求、原因/凭证/阶段、重复损失防护测试。 | 与授权拒绝或普通 refund 混同。 |
| 余额控制调账审计 | 可以，B5 独立切片。 | `FundsBalanceControlService`、adjust 请求、审批/差错/凭证字段和测试。 | 用支付工具或预算组作为余额主体。 |
| 原路径回放和投影解释 | 可以，B6/B8 独立切片。 | route replay、transaction projection、查询 DTO 和重放测试。 | 投影反写事实或替代 route/posting/ledger。 |

## 5. writeScopeCandidate

| 范围 | 候选授权 |
| --- | --- |
| B2 测试资产 | `PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests`、`PaymentInstrumentRouteDslContractTests`、钱包 application facade 边界测试或等价新测试类。 |
| B4 测试资产 | 授权准入组合测试、`FundsAuthorizationTransactionFlowTests` 的边界回归、授权 application facade 边界测试。 |
| 生产实现 | 只有 Red 证明真实缺口后，才允许在 `wallet-face`、`wallet-impl` 增加 application facade；交易层只能在独立 B3/B4/B5/B6 Grant 中完善账户主体型内核能力。具体模块必须由 Execution Grant 指定。 |
| 公共契约 | 默认不允许破坏既有 face/core 请求字段；如必须新增 `PaymentInstrumentCapabilityApplicationService`、`AuthorizationAdmissionApplicationService`、`InstrumentTransactionLifecycleApplicationService`、`VccPrepaidFundingApplicationService`、Request/DTO 或动作能力字段，必须在 Execution Grant 明确命名、依赖方向和兼容策略。 |
| DDL/H2 schema | 默认不允许修改；如 Spend Rule 规则定义、决策日志、控制活动或预算控制投影需要表结构，必须单独扩权到 B5/B6/B8。 |
| 资金责任目标字段 | 默认不允许混合策略；B2-FR 必须选择 `funding-account-only` 或 `targetSubjectType + targetSubjectId`。若选择目标主体迁移，Execution Grant 必须显式允许 DTO、DDL/H2、Entity、Mapper、摘要、fixture 和回放断言同步修改。 |

## 6. noWriteScope

| 禁止范围 | 说明 |
| --- | --- |
| 交易 canonical 请求替换 | 不把 `FundsAuthorizationTransactionAuthorizeRequest`、直接交易请求或余额控制请求整体改成支付工具引用。 |
| 统一支付工具交易服务 | 不新增统一 `InstrumentTransactionService` 族覆盖直接交易、授权交易和余额控制。 |
| 预算组账务主体化 | 不把预算组或 Spend Rule 写成 route leg、posting、LedgerEntry、账本余额投影主体。 |
| P2 业务轨道实现 | 不实现卡组织、ACH/Nacha、SWIFT/local rail、PSP、银行协议、FX 执行或完整外部回单。 |
| 清结算、对账和治理 | 不新增清分、清算、结算、出款、对账差错、归档、Manifest、checkpoint、watermark 或正式重投影 apply。 |
| 敏感数据 | 不引入完整 PAN、CVV、token secret、银行账户敏感号、证件原文或生产通道配置。 |
| 资金责任策略混用 | 不允许一边保留 `fundingAccountId` 作为唯一写入字段，一边在 Done 结论中声明信用账户或平台角色责任主体已生产可用。 |

## 7. redCandidateSet

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `R0-PI-001` | 支付工具是否真正具备当前动作能力。 | 工具能力只能作为准入快照，不能替代账户能力、余额、额度、预算或账本周期。 | 非 ACTIVE、方向不匹配、缺动作能力、过期、错币种、敏感原文或绑定版本失效时返回可解释失败或授权拒绝。 | 不得生成 route、posting、LedgerEntry；不得返回完整敏感凭证。 | 状态、方向、能力、币种、有效期、绑定版本、脱敏字段、失败原因、无账务副作用。 | `PaymentInstrumentServiceImplTests`、新 application facade 测试。 | `just test-one PaymentInstrumentServiceImplTests tests`、`just test-boundary`。 | 需要新增动作能力枚举、DTO、表字段但未授权。 |
| `R0-FR-001` | 支付工具、使用主体、预算组或 Spend Rule 能否解析到唯一最终责任主体。 | 最终责任主体只能是资金账户、信用账户或平台角色解析后的平台资金账户；若选择 `funding-account-only`，本轮最终责任主体只能是资金账户。 | 缺失、不唯一、错币种、优先级冲突或预算组/Spend Rule 被当最终主体时失败。 | 不得随机选路，不得让预算组或 Spend Rule 入账；不得混用 `fundingAccountId` 和目标主体迁移策略。 | 决策主体类型、主体 ID、币种、优先级、规则版本、选择原因、字段策略、失败无 route/posting/entry。 | `SpendSubjectFundingRelationServiceImplTests`、`PaymentInstrumentRouteDslContractTests`。 | `just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-boundary`。 | 目标态要求信用账户或平台角色，但字段仍只允许 `fundingAccountId` 且未授权迁移。 |
| `R0-AUTH-001` | 授权支付工具入口是否只作为 application facade 委派账户主体型内核。 | 授权内核仍以已解析账户主体为 canonical 入参；拒绝无资金事实。 | `authorizeByInstrument` 或等价入口完成工具准入、绑定快照、Spend Rule、资金责任和账户能力后构造 canonical 授权请求。 | 不得直接替换 `FundsAuthorizationTransactionAuthorizeRequest.accountId`；不得让工具、预算组或 Spend Rule 成为 route leg。 | 准入步骤、委派请求、拒绝事实、route snapshot、幂等摘要、无副作用、敏感上下文阻断。 | 授权 application facade 测试、`FundsAuthorizationTransactionFlowTests` 回归。 | `just test-transaction`、`just test-boundary`。 | 需要改变授权内核公共契约或状态机但未授权。 |
| `R0-VCC-PREPAID-001` | 预付卡入金或系统内充值是否先确认资金责任再增加可用余额。 | prepaid virtual card 不是账本主体；只有确认后的资金账户、信用账户或平台责任账户可形成余额变化。 | `PostPrepaidFunding` 或等价命令必须包含 fundingRef、confirmationRef、责任主体、金额币种和 requestDigest；未确认、责任不唯一、重复同键不同摘要时无余额变化。 | 不得用卡对象、issuer 余额摘要或外部 financial account 直接增加平台账本余额；不得把储值券、礼品卡或非卡权益混入 VCC 预付卡。 | 确认引用、责任主体类型、幂等摘要、余额桶变化、重复提交、未确认阻断、敏感字段阻断。 | 预付资金 application facade 测试、直接交易/内部转账回归、余额投影断言。 | 由 Execution Grant 指定；建议 `just test-transaction`、`just test-ledger`、`just test-boundary`。 | 需要新增 Request/DTO、状态、表字段或 H2 schema 但未授权。 |
| `R0-VCC-LC-001` | 共享卡或预付卡清算、释放、退款和拒付是否按原路径回放。 | 后续事件必须引用原授权、原 route snapshot、原工具快照和原资金责任决策；当前绑定变化不能改变历史资金路径。 | settle/release/refund/chargeback 命令固化原授权引用、外部事件引用、金额币种、幂等摘要和处理阶段；快照缺失进入差错。 | 不得按当前卡绑定、当前预算组、当前 Spend Rule 或当前默认责任主体重新选路；不得把 chargeback 当普通 refund 合并。 | 原快照引用、绑定版本、责任主体、金额闭合、重复损失防护、差错入口、无快照失败。 | 授权生命周期 facade 测试、route replay 测试、交易投影回归。 | 由 Execution Grant 指定；建议 `just test-transaction`、`just test-boundary`。 | 需要扩展授权状态机、chargeback/settle/refund 契约或 route replay 公共契约但未授权。 |
| `R0-SR-001` | Spend Rule 拒绝是否能留下可审计决策而无账务副作用。 | 规则拒绝只能生成决策证据或拒绝事实，不生成资金事实。 | MCC、商户、时间窗、频控、限额或规则版本拒绝时记录规则决策和拒绝原因。 | 不得生成 route、posting、LedgerEntry；不得把规则通过等同于资金可用。 | 规则版本、命中条件、拒绝原因、使用主体、工具快照、无账务副作用。 | 新 Spend Rule 控制测试；未授权前可为 contract-only。 | 由 Execution Grant 指定；默认不写测试。 | 缺规则模型或表结构授权。 |
| `R0-SR-002` | 预算预留、释放和调整是否只更新控制活动和只读视图。 | 预算控制不等于账本余额，不得生成资金交易记录。 | 授权预留、撤销、过期、部分完成或退款释放写控制活动和预算控制投影。 | 不得写 LedgerEntry、账本余额桶或资金交易明细；不得无幂等并发更新预算可用。 | 控制活动类型、幂等键、前后预留量、规则版本、并发保护、投影只读。 | 新预算控制测试；未授权前可为 contract-only。 | 由 Execution Grant 指定；默认不写测试。 | 需要新增 Spend Control Activity 或预算投影表。 |
| `R0-PI-002` | 换绑后逆向交易是否仍按原快照解释。 | 退款、撤销、退费、拒付和重放优先沿原 route snapshot。 | 原工具快照、原绑定版本和原 route snapshot 可追溯；缺快照失败或人工处理。 | 不得按当前绑定、当前默认资金责任或当前能力重新选路。 | 原快照引用、绑定版本、原责任主体、金额闭合、失败处理。 | Route replay 或授权/交易逆向测试。 | `just test-transaction`、`just test-boundary`。 | 需要扩展 route replay 公共契约但未授权。 |

## 8. suggestedGrantSlices

本节只表示支付工具与 Spend Rule 支持能力内部的局部候选顺位，不覆盖全局任务优先级。全局恢复入口仍按账本账目 > 钱包 > 交易层 > 支付工具支持 > VCC/全球账户支持；收单仅 design-only。

| 切片 | 局部支持顺位 | 目标 | 首批 Red | 允许写入建议 | 不适合混入 |
| --- | --- | --- | --- | --- | --- |
| B2-PI-CAP | 1 | 支付工具能力准入 application facade。 | `R0-PI-001`。 | wallet-face/impl 的 facade 契约和测试；必要的 DTO。 | 授权状态机、Spend Rule 表、交易投影。 |
| B2-FR | 2 | 资金责任目标主体解析。 | `R0-FR-001`。 | 资金责任关系契约和测试；必须先选择 `funding-account-only` 或 `targetSubjectType + targetSubjectId`，迁移目标主体字段需单独授权 DTO、DDL/H2、摘要和 fixture。 | 直接交易、清结算、P2 轨道、混合字段策略。 |
| B4-AUTH-PI | 3 | 授权支付工具 application facade。 | `R0-AUTH-001`。 | 授权准入 facade、委派适配和边界测试。 | 替换授权内核请求、完整 VCC 发卡。 |
| P2-VCC-PREPAID | 4 | 预付卡入金确认、系统内充值和未确认阻断。 | `R0-VCC-PREPAID-001`。 | 预付资金 application facade、直接交易/内部转账适配、余额投影断言；需单独确认责任主体字段策略。 | 通用储值账户、退卡提现自动化、税务/会计自动处理。 |
| P2-VCC-LIFECYCLE | 5 | 共享卡和预付卡清算、释放、退款、拒付原路径回放。 | `R0-VCC-LC-001`。 | 授权生命周期 facade、route replay、差错入口和重复损失防护。 | 完整 clearing 文件处理、chargeback 全生命周期、FX 和费用自动入账。 |
| B5-SR-CONTROL | 6 | Spend Rule 决策日志和预算预留释放。 | `R0-SR-001`、`R0-SR-002`。 | 规则定义、控制活动、预算控制投影，需单独 DDL/H2 授权。 | A1、B2 基础能力、P2 轨道。 |
| B6/B8-PI-VIEW | 7 | 支付工具流水、预算控制视图、规则命中时间线和重放。 | `R0-PI-002`。 | 只读投影、重放范围和差异报告，需单独授权。 | 事实反写、正式治理 apply。 |

### 8.1 B4-AUTH-PI Round 0 扫描（2026-06-03）

本节是 GSD-CAD 自动推进后的只读扫描结论。它把 `B4-AUTH-PI` 从候选列表推进到可确认的单一 Execution Grant 输入，但不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。

| 扫描项 | 结论 |
| --- | --- |
| 当前状态 | `ROUND0_READY_NOT_CODE_AUTHORIZED`。 |
| canonical 内核 | `FundsAuthorizationTransactionService#authorize` 仍接收 `FundsAuthorizationTransactionAuthorizeRequest`，请求以 `FundsAccountId accountId` 作为已解析账户主体入参；B4-AUTH-PI 不应替换该请求字段。 |
| 钱包资源服务 | `PaymentInstrumentService` 负责工具和绑定管理；`SpendSubjectFundingRelationService` 负责资金责任关系维护；两者不是授权准入 application facade。 |
| 缺口 | 未发现 `AuthorizationAdmissionApplicationService`、`PaymentInstrumentCapabilityApplicationService` 生产实现或 `authorizeByInstrument` 入口；既有测试未证明工具准入、绑定快照、Spend Rule、资金责任和账户能力组合后委派授权内核。 |
| 首批 Red | `R0-AUTH-001`：支付工具授权入口必须先做工具、绑定、Spend Rule、资金责任和账户能力准入；拒绝无 route、posting、LedgerEntry、projection 或敏感上下文副作用；批准后只委派账户主体型授权内核。 |
| Grant 必须列明 | application facade 名称、Request/DTO、错误码、幂等摘要、拒绝事实、route snapshot / audit 快照位置、敏感上下文白名单、目标测试资产和验证命令。 |
| 禁止混入 | 不新增统一 `InstrumentTransactionService`；不把支付工具、预算组或 Spend Rule 作为账务主体；不混入完整 VCC、Spend Rule 引擎、清结算对账、治理 apply、P2 轨道或敏感原文。 |

### 8.2 authInstrumentGrantCandidate（2026-06-03）

本节把 8.1 的只读扫描推进为可确认的单一 Execution Grant 候选。它仍不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置；只有用户确认本节的 `Execution Grant：B4-AUTH-PI` 后，才允许进入首批 Red。

| 授权包字段 | 候选内容 |
| --- | --- |
| `taskId` | `B4-AUTH-PI-CAD-001`。 |
| `stage` / `wave` | B4 授权交易 / Wave 2 支付工具授权 application facade 准入。 |
| `status` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。 |
| `cadCandidateStructureCheck` | 2026-06-04 已用资深架构师 Harness checker 以 `cad-candidate` 模式检查本卡和 Harness tasks，结果均为 `OK harness plan check: kind=cad-candidate`；该检查只证明候选包字段结构完整，不等于编码授权、测试通过或生产审批。 |
| `owner` | 资深架构师负责工程执行；产品架构专家只负责支付工具、资金主体、Spend Rule 和拒绝原因的业务语义复核。 |
| `authorityBaseline` | 确认时 Git HEAD；当前候选至少要求包含 `88d80c7 docs: 收敛授权后继索引基线`、`7b49684 docs: 记录授权支付工具候选门禁`、`be3df9f docs: 同步授权工具候选索引`、`c58431e docs: 同步 TDD 授权工具候选索引` 和 `226dfc2 docs: 回写授权工具索引流水`，确保 `967586c` 无授权退款路由回退、`47c5269` 授权并发闭环、`B4-AUTH-PI-CAD-001` 结构门禁、索引同步和恢复入口均已纳入确认语境。若确认前出现新的未提交文档变更，必须先提交或列入本 Grant 附件。 |
| `mvpScenario` | 业务方提交支付工具引用、使用主体、金额币种、业务流水、业务场景、预算或 Spend Rule 上下文；系统在 application facade 中完成支付工具能力、绑定快照、资金责任、Spend Rule 和账户能力准入。批准时构造账户主体型授权请求并委派 `FundsAuthorizationTransactionService#authorize`；拒绝时只留下拒绝事实、原因和审计，不生成资金事实。 |
| `businessAdmission` | 产品锚点为 `AC-PI-010` 和 `AC-AUTH-000`；DSL 锚点为支付工具入口到账户主体授权内核转译；系分锚点为 `AuthorizationAdmissionApplicationService`、支付工具能力应用服务、资金责任解析和账户能力查询；TDD 锚点为 `R0-AUTH-001`。 |
| `firstRedSet` | `R0-AUTH-001`：绕过工具准入、绑定快照、Spend Rule、资金责任或账户能力直接调用授权内核必须失败；拒绝路径无 route、posting、LedgerEntry、projection 和敏感上下文副作用；批准路径只委派账户主体型授权内核。 |
| `secondRedSet` | 最小失败矩阵：工具非 ACTIVE、方向或动作能力不匹配、绑定缺失或版本失效、资金责任缺失或不唯一、Spend Rule 拒绝、账户能力不支持、币种不一致、敏感上下文出现时失败且无资金副作用。 |
| `writeScope` | 先写 `tests/src/test/java/com/wind/funds/wallet/application/instrument/AuthorizationAdmissionApplicationServiceTests.java` 或等价授权 application facade 测试；Red 证明缺口后，仅允许在 `wallet-face` 新增 application facade 契约、Request/DTO，在 `wallet-impl` 新增最小 facade 实现和委派适配，并按需补 `FundsAuthorizationTransactionFlowTests` 回归。 |
| `readOnlyScope` | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/tasks.md`、既有 `wallet-*`、`transaction-*`、`core`、`tests/src/test/resources/jdbc-schema.sql`。 |
| `harnessScopeIndex` | 标准 Harness 字段索引：写入范围为授权 application facade 目标 Red、`wallet-face` application 契约和 Request/DTO、`wallet-impl` 最小实现、必要的授权 flow 回归；写入文件先限定新授权准入测试，Red 证明缺口后才进入上列生产触点；只读范围和只读参考为 PRD、DSL、系分、TDD、OpenSpec、既有 wallet/transaction/core 和 H2 schema。 |
| `publicContractGate` | 只允许非破坏性新增 wallet application facade、Request/DTO 和返回 DTO；不得修改或替换 `FundsAuthorizationTransactionAuthorizeRequest.accountId`、`FundsAuthorizationTransactionService#authorize`、交易状态机、core 枚举或 ledger 公共契约。 |
| `dependencyGate` | `wallet-face` 不依赖 `transaction-face` 或任何 impl；`wallet-impl` 可依赖 `wallet-face`、`transaction-face` 和 core；`transaction-impl` 不反向依赖 wallet 资源服务或 wallet impl。违反依赖方向时立即停止。 |
| `schemaGate` | 默认不修改 `tests/src/test/resources/jdbc-schema.sql`、生产 DDL、Entity 字段、Mapper 表字段、索引或唯一约束；若准入快照、拒绝事实或 Spend Rule 决策需要落表，必须重新确认 B5/B6/B8 或独立 Grant。 |
| `noWriteScope` | 不新增统一 `InstrumentTransactionService`；不把支付工具、预算组或 Spend Rule 写成 route leg、posting、LedgerEntry 或账本余额主体；不实现完整 VCC、Spend Rule 引擎、预算控制投影、交易投影、清结算对账、治理 apply、P2 轨道、外部协议、PCI/PAN/CVV 或敏感原文处理。 |
| `verificationCommand` | 首轮 `just test-one AuthorizationAdmissionApplicationServiceTests tests`；Green 后按触点补 `just test-one PaymentInstrumentServiceImplTests tests`、`just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-one PaymentInstrumentRouteDslContractTests tests`、`just test-transaction`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`。 |
| `gitStrategy` | 若用户确认本 Grant 并保持 GSD-CAD 自动模式，目标验证通过且未触发停止条件时按 `auto_commit` 提交；验证失败、环境不可判定或越界时转 `summary_only`。 |
| `stopCondition` | 需要修改 transaction canonical 请求、core 枚举或状态、ledger 公共契约、DDL/H2 schema、目标主体字段迁移、Spend Rule 表、预算控制投影、完整 VCC、清结算对账、治理、外部规则、敏感数据、跨模块依赖反转、公有方法超过 5 个参数或工作树冲突时停止。 |
| `handoff` | 本候选包的恢复入口为 `B4-AUTH-PI-CAD-001`。用户确认 `Execution Grant：B4-AUTH-PI` 后进入首批 Red；未确认时只保留为 Round 0 / summary_only。若用户改选 B2-PI-CAP、B2-FR、B5-SR-CONTROL 或其他任务包，本候选只作为只读参考和残余风险记录。 |

```text
Execution Grant：B4-AUTH-PI
确认基线：确认时 Git HEAD；至少包含 88d80c7 docs: 收敛授权后继索引基线、7b49684 docs: 记录授权支付工具候选门禁、be3df9f docs: 同步授权工具候选索引、c58431e docs: 同步 TDD 授权工具候选索引和 226dfc2 docs: 回写授权工具索引流水；若确认前有未提交文档变更，必须先提交或列入 authorityBaseline
任务包：B4-AUTH-PI-CAD-001
目标：新增 authorizeByInstrument 或等价 AuthorizationAdmissionApplicationService application facade，完成支付工具、绑定、Spend Rule、资金责任和账户能力准入；批准后委派账户主体型 FundsAuthorizationTransactionService#authorize；拒绝无 route、posting、LedgerEntry、projection 或敏感上下文副作用
允许写入：先写 tests 中授权 application facade 目标 Red；Red 证明缺口后允许 wallet-face application facade 契约、Request/DTO、wallet-impl 最小实现、委派适配和必要授权 flow 回归
允许公共契约：仅允许非破坏性新增 wallet application facade、Request/DTO 和返回 DTO；不得修改 FundsAuthorizationTransactionAuthorizeRequest.accountId 或 FundsAuthorizationTransactionService#authorize
首批 Red：R0-AUTH-001；必要时补工具状态、绑定、资金责任、Spend Rule、账户能力、币种和敏感上下文失败矩阵
验证命令：just test-one AuthorizationAdmissionApplicationServiceTests tests；just test-one PaymentInstrumentServiceImplTests tests；just test-one SpendSubjectFundingRelationServiceImplTests tests；just test-one PaymentInstrumentRouteDslContractTests tests；just test-transaction；just test-boundary；just compile；提交前 just pmd 和 git diff --check
禁止写入：交易 canonical 请求替换、统一 InstrumentTransactionService、支付工具或预算组或 Spend Rule 入账主体化、DDL/H2 schema、core 枚举状态、ledger 公共契约、完整 VCC、Spend Rule 引擎、预算控制投影、清结算对账、治理 apply、P2 轨道、外部协议、PCI/PAN/CVV 或敏感原文
Git 策略：auto_commit
停止条件：公共契约越界、表结构、依赖方向反转、外部规则、敏感数据、P2/清结算/治理越界、验证无法解释失败或工作树冲突即停止
交接：确认后从 B4-AUTH-PI-CAD-001 首批 Red 开始；未确认时不写 Java、测试、DDL/H2 schema 或运行时配置
```

### 8.3 B2-PI-CAP Round 0 扫描（2026-06-04）

本节把支付工具支持队列中的 `B2-PI-CAP` 支付工具能力准入切片推进到可确认输入。它只做只读扫描和候选授权包收敛，不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入；全局恢复顺位仍服从账本账目 > 钱包 > 交易层 > 支付工具支持。

| 扫描项 | 结论 |
| --- | --- |
| 当前状态 | `ROUND0_READY_NOT_CODE_AUTHORIZED`。 |
| 资源服务基线 | `PaymentInstrumentService` 和 `PaymentInstrumentServiceImpl` 已覆盖支付工具创建、绑定、绑定历史、状态、方向、币种、生效窗口、敏感字段阻断和无账务副作用；目标测试资产为 `PaymentInstrumentServiceImplTests` 和 `PaymentInstrumentRouteDslContractTests`。 |
| 目标缺口 | 当前未发现 `PaymentInstrumentCapabilityApplicationService` 或等价 application facade；`PaymentInstrumentDirection` 只有 `RECEIVE`、`PAYMENT`、`BOTH`，尚未固化 RECEIVE、PAY、AUTHORIZE、REFUND、WITHDRAW 五类工具动作能力，也没有统一输出不可变工具准入快照。 |
| 首批 Red | `R0-PI-001`：工具非 ACTIVE、方向不匹配、缺 RECEIVE/PAY/AUTHORIZE/REFUND/WITHDRAW 动作能力、过期、错币种、敏感原文或绑定版本失效时必须可解释失败，且不生成 route、posting、LedgerEntry 或余额投影。 |
| Grant 必须列明 | application facade 名称、动作能力承载枚举或等价字段、Request/DTO、准入快照字段、错误码、绑定版本读取、敏感字段白名单、目标测试资产和验证命令。 |
| 禁止混入 | 不进入授权准入组合、资金责任目标主体迁移、Spend Rule 表、预算控制投影、交易投影、清结算对账、治理 apply、完整 VCC 或 P2 轨道协议。 |

### 8.4 paymentInstrumentCapabilityGrantCandidate（2026-06-04）

本节把 8.3 的只读扫描推进为可确认的单一 Execution Grant 候选。它仍不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置；只有用户确认本节的 `Execution Grant：B2-PI-CAP` 后，才允许进入首批 Red。

| 授权包字段 | 候选内容 |
| --- | --- |
| `taskId` | `B2-PI-CAP-CAD-001`。 |
| `stage` / `wave` | B2 钱包基础能力 / Wave 1 支付工具能力 application facade 准入。 |
| `status` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。 |
| `owner` | 资深架构师负责工程执行；产品架构专家负责工具动作、准入快照、敏感字段和 Not Done 语义复核。 |
| `authorityBaseline` | 确认时 Git HEAD；当前候选至少要求包含 `73ea257 docs: 回写授权工具确认基线` 及本节提交点。若确认前出现新的未提交文档变更，必须先提交或列入本 Grant 附件。 |
| `mvpScenario` | 业务入口提交支付工具引用、动作类型、金额币种、使用主体和业务流水；系统在 application facade 中一次性校验工具状态、方向、动作能力、币种、生效窗口、绑定版本和敏感字段。通过时输出不可变工具准入快照；失败时返回可解释原因，不生成任何资金事实。 |
| `businessAdmission` | 产品锚点为 `AC-PI-003`、`AC-PI-006`；DSL 锚点为 `DSL-PAYMENT-INSTRUMENT-CAPABILITY-001`、`DSL-PAYMENT-INSTRUMENT-FAIL-001`；系分锚点为 `PaymentInstrumentCapabilityApplicationService`；TDD 锚点为 `TDD-WALLET-018`、`TDD-ROUTE-012` 和 `R0-PI-001`。 |
| `firstRedSet` | `R0-PI-001`：工具非 ACTIVE、方向不匹配、动作能力缺失、过期、错币种、敏感原文或绑定版本失效时仍被放行必须失败；失败不得生成 route、posting、LedgerEntry、余额投影或完整敏感凭证。 |
| `secondRedSet` | 工具能力通过但账户能力、资金责任或 Spend Rule 失败时不得被工具能力覆盖；该矩阵只作为下一切片输入，除非本 Grant 明确扩展，否则不进入实现。 |
| `writeScope` | 先写 `tests/src/test/java/com/wind/funds/wallet/application/instrument/PaymentInstrumentCapabilityApplicationServiceTests.java` 或等价 application facade 目标 Red；Red 证明缺口后，仅允许在 `wallet/wallet-face` 新增 application facade 契约、Request/DTO，在 `wallet/wallet-impl` 新增最小 facade 实现和委派适配，并按需补 `PaymentInstrumentServiceImplTests` 回归。 |
| `readOnlyScope` | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/tasks.md`、既有 `wallet`、`core`、`transaction`、`tests/src/test/resources/jdbc-schema.sql`。 |
| `publicContractGate` | 只允许非破坏性新增 wallet application facade、Request/DTO、返回 DTO 和必要动作能力枚举或等价字段；不得修改交易 canonical 请求、授权状态机、core 交易枚举、ledger 公共契约或现有资源服务语义。 |
| `dependencyGate` | `wallet/wallet-face` 不依赖任何 impl；`wallet/wallet-impl` 可依赖 `wallet/wallet-face`、core 和既有资源服务；`transaction` 不反向依赖 wallet impl。违反依赖方向时立即停止。 |
| `schemaGate` | 默认不修改 `tests/src/test/resources/jdbc-schema.sql`、生产 DDL、Entity 字段、Mapper 表字段、索引或唯一约束；若动作能力需要持久化字段或表结构，必须重新确认扩权。 |
| `noWriteScope` | 不实现 `authorizeByInstrument`、不新增统一 `InstrumentTransactionService`、不做资金责任目标主体迁移、不把工具能力通过等同于账户能力、余额、额度或账本周期通过、不混入 Spend Rule、预算控制投影、清结算对账、治理 apply、完整 VCC、P2 轨道、外部协议或敏感原文处理。 |
| `verificationCommand` | 首轮 `just test-one PaymentInstrumentCapabilityApplicationServiceTests tests`；Green 后按触点补 `just test-one PaymentInstrumentServiceImplTests tests`、`just test-one PaymentInstrumentRouteDslContractTests tests`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`。 |
| `gitStrategy` | 若用户确认本 Grant 并保持 GSD-CAD 自动模式，目标验证通过且未触发停止条件时按 `auto_commit` 提交；验证失败、环境不可判定或越界时转 `summary_only`。 |
| `stopCondition` | 需要修改交易 canonical 请求、授权 application facade、资金责任目标主体字段、DDL/H2 schema、Spend Rule、预算控制投影、清结算对账、治理、外部规则、敏感数据、依赖方向反转、公有方法超过 5 个参数或工作树冲突时停止。 |
| `handoff` | 本候选包的恢复入口为 `B2-PI-CAP-CAD-001`。用户确认 `Execution Grant：B2-PI-CAP` 后进入首批 Red；未确认时只保留为 Round 0 / summary_only。 |

```text
Execution Grant：B2-PI-CAP
确认基线：确认时 Git HEAD；至少包含 73ea257 docs: 回写授权工具确认基线及本节提交点；若确认前有未提交文档变更，必须先提交或列入 authorityBaseline
任务包：B2-PI-CAP-CAD-001
目标：新增 PaymentInstrumentCapabilityApplicationService 或等价 application facade，完成支付工具状态、方向、动作能力、币种、生效窗口、绑定版本和敏感字段准入；通过后输出不可变工具准入快照；失败无 route、posting、LedgerEntry、余额投影或完整敏感凭证
允许写入：先写 tests 中支付工具能力 application facade 目标 Red；Red 证明缺口后允许 wallet-face application facade 契约、Request/DTO、wallet-impl 最小实现、委派适配和必要资源服务回归
允许公共契约：仅允许非破坏性新增 wallet application facade、Request/DTO、返回 DTO 和必要动作能力枚举或等价字段；不得修改交易 canonical 请求、授权状态机、ledger 公共契约或现有资源服务语义
首批 Red：R0-PI-001；必要时补工具状态、方向、RECEIVE/PAY/AUTHORIZE/REFUND/WITHDRAW、币种、生效窗口、绑定版本和敏感上下文失败矩阵
验证命令：just test-one PaymentInstrumentCapabilityApplicationServiceTests tests；just test-one PaymentInstrumentServiceImplTests tests；just test-one PaymentInstrumentRouteDslContractTests tests；just test-boundary；just compile；提交前 just pmd 和 git diff --check
禁止写入：authorizeByInstrument、交易 canonical 请求替换、统一 InstrumentTransactionService、资金责任目标主体迁移、DDL/H2 schema、Spend Rule、预算控制投影、清结算对账、治理 apply、完整 VCC、P2 轨道、外部协议或敏感原文
Git 策略：auto_commit
停止条件：公共契约越界、表结构、依赖方向反转、外部规则、敏感数据、B4/B5/B6/B8/P2 越界、验证无法解释失败或工作树冲突即停止
交接：确认后从 B2-PI-CAP-CAD-001 首批 Red 开始；未确认时不写 Java、测试、DDL/H2 schema 或运行时配置
```

### 8.5 B2-FR Round 0 扫描（2026-06-04）

本节把 `B2-FR` 资金责任解析切片推进到可确认输入。它只做只读扫描和候选授权包收敛，不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。

| 扫描项 | 结论 |
| --- | --- |
| 当前状态 | `ROUND0_READY_NOT_CODE_AUTHORIZED`。 |
| 资源服务基线 | `SpendSubjectFundingRelationService` 和 `SpendSubjectFundingRelationServiceImpl` 仍是关系资源服务；创建关系时只接受 `fundingAccountId`，并校验真实资金账户存在、可借记、币种一致、生效窗口、默认关系唯一、优先级不冲突、敏感上下文阻断和无账务副作用。目标测试资产为 `SpendSubjectFundingRelationServiceImplTests` 和 `PaymentInstrumentRouteDslContractTests`。 |
| 目标缺口 | 当前未发现 `FundingResponsibilityResolutionApplicationService` 或等价 application facade；`CreateSpendSubjectFundingRelationRequest`、DTO、Query、Entity 和 H2 schema 仍以 `fundingAccountId` / `funding_account_id` 为主要目标字段，不能在不迁移字段的前提下声明信用账户或平台账户角色责任主体生产可用。 |
| 字段策略裁决 | 本轮只把低风险 `funding-account-only` 收敛为可确认 Grant 候选：解析结果只能是 `FUNDING_ACCOUNT`。如要进入 `targetSubjectType + targetSubjectId`，必须另起高风险迁移 Grant，并显式允许 Request/DTO、DDL/H2 schema、Entity、Mapper、摘要、fixture、route snapshot 和回放断言同步修改。 |
| 首批 Red | `R0-FR-001A`：资金责任缺失、不唯一、错币种、关系失效、目标资金账户不可借记、预算组或 Spend Rule 被当最终责任主体时必须可解释失败；成功时输出不可变 `FundingAllocationDecision` 或等价决策，且 targetSubjectType 只能是 `FUNDING_ACCOUNT`。 |
| Grant 必须列明 | application facade 名称、字段策略、Request/DTO、决策快照字段、错误码、关系优先级/默认关系排序、绑定或规则版本引用、敏感字段白名单、目标测试资产和验证命令。 |
| 禁止混入 | 不做 `targetSubjectType + targetSubjectId` 字段迁移，不把信用账户或平台角色写入 `fundingAccountId`，不进入支付工具动作能力、授权准入组合、Spend Rule 表、预算控制投影、交易投影、清结算对账、治理 apply、完整 VCC 或 P2 轨道协议。 |

### 8.6 fundingResponsibilityGrantCandidate（2026-06-04）

本节把 8.5 的只读扫描推进为可确认的单一 Execution Grant 候选。它仍不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置；只有用户确认本节的 `Execution Grant：B2-FR-FAO` 后，才允许进入首批 Red。

| 授权包字段 | 候选内容 |
| --- | --- |
| `taskId` | `B2-FR-FAO-CAD-001`。 |
| `stage` / `wave` | B2 钱包基础能力 / Wave 1 资金责任解析 application facade 准入。 |
| `status` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。 |
| `owner` | 资深架构师负责工程执行；产品架构专家负责资金责任语义、使用者解释和 Not Done 语义复核。 |
| `authorityBaseline` | 确认时 Git HEAD；当前候选至少要求包含 `97359f6 docs: 补齐支付工具能力候选包` 及本节提交点。若确认前出现新的未提交文档变更，必须先提交或列入本 Grant 附件。 |
| `mvpScenario` | 业务入口提交支付工具、使用主体、预算或 Spend Rule 上下文和金额币种；系统在 application facade 中读取已配置的资金责任关系，按默认关系、优先级、生效窗口、状态和币种裁决唯一资金账户责任主体。通过时输出不可变资金责任决策；失败时返回可解释原因，不生成任何资金事实。 |
| `businessAdmission` | 产品锚点为支付工具与资金责任生产可用性裁决；DSL 锚点为 `FundingAllocationDecision` 只表达最终可入账主体；系分锚点为 `FundingResponsibilityResolutionApplicationService`；TDD 锚点为 `TDD-WALLET-018`、`TDD-ROUTE-012` 和 `R0-FR-001`。 |
| `fieldStrategy` | `funding-account-only`。本 Grant 只允许声明解析到真实资金账户，不允许声明信用账户、平台账户角色、预算组或 Spend Rule 作为最终资金责任主体。 |
| `firstRedSet` | `R0-FR-001A`：缺资金责任、多个关系命中、优先级冲突、默认关系不唯一、关系失效、目标资金账户不可借记、错币种、敏感上下文或预算组/Spend Rule 被输出为最终主体时仍被放行必须失败；失败不得生成 route、posting、LedgerEntry 或余额投影。 |
| `secondRedSet` | `R0-FR-001B`：目标态要求信用账户或平台角色责任主体时必须阻断并提示需要 `targetSubjectType + targetSubjectId` 迁移 Grant；该 Red 只证明策略边界，不进入字段迁移实现。 |
| `writeScope` | 先写 `tests/src/test/java/com/wind/funds/wallet/application/funding/FundingResponsibilityResolutionApplicationServiceTests.java` 或等价 application facade 目标 Red；Red 证明缺口后，仅允许在 `wallet/wallet-face` 新增 application facade 契约、Request/DTO，在 `wallet/wallet-impl` 新增最小 facade 实现和资源服务适配，并按需补 `SpendSubjectFundingRelationServiceImplTests` 回归。 |
| `readOnlyScope` | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/tasks.md`、既有 `wallet`、`core`、`transaction`、`tests/src/test/resources/jdbc-schema.sql`。 |
| `publicContractGate` | 只允许非破坏性新增 wallet application facade、Request/DTO、返回 DTO 和资金责任决策快照；不得修改交易 canonical 请求、core 交易枚举、ledger 公共契约或现有资源服务语义。 |
| `schemaGate` | 不修改 `tests/src/test/resources/jdbc-schema.sql`、生产 DDL、Entity 字段、Mapper 表字段、索引或唯一约束；若需要 `target_subject_type` / `target_subject_id`，必须停止并改走 `B2-FR-TARGET` 独立 Grant。 |
| `dependencyGate` | `wallet/wallet-face` 不依赖任何 impl；`wallet/wallet-impl` 可依赖 `wallet/wallet-face`、core 和既有资源服务；`transaction` 不反向依赖 wallet impl。违反依赖方向时立即停止。 |
| `noWriteScope` | 不迁移 `targetSubjectType + targetSubjectId`，不新增信用账户或平台角色责任主体生产能力，不把预算组或 Spend Rule 写成 route leg、posting、LedgerEntry 或账本余额主体，不实现 `authorizeByInstrument`，不混入支付工具动作能力、Spend Rule、预算控制投影、清结算对账、治理 apply、完整 VCC、P2 轨道、外部协议或敏感原文处理。 |
| `verificationCommand` | 首轮 `just test-one FundingResponsibilityResolutionApplicationServiceTests tests`；Green 后按触点补 `just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-one PaymentInstrumentRouteDslContractTests tests`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`。 |
| `gitStrategy` | 若用户确认本 Grant 并保持 GSD-CAD 自动模式，目标验证通过且未触发停止条件时按 `auto_commit` 提交；验证失败、环境不可判定或越界时转 `summary_only`。 |
| `stopCondition` | 需要修改目标主体字段、DDL/H2 schema、Entity、Mapper、route snapshot、交易 canonical 请求、授权 application facade、支付工具动作能力、Spend Rule、预算控制投影、清结算对账、治理、外部规则、敏感数据、依赖方向反转、公有方法超过 5 个参数或工作树冲突时停止。 |
| `handoff` | 本候选包的恢复入口为 `B2-FR-FAO-CAD-001`。用户确认 `Execution Grant：B2-FR-FAO` 后进入首批 Red；未确认时只保留为 Round 0 / summary_only。若用户选择 `targetSubjectType + targetSubjectId`，本候选只作为只读参考，必须另起迁移 Grant。 |

```text
Execution Grant：B2-FR-FAO
确认基线：确认时 Git HEAD；至少包含 97359f6 docs: 补齐支付工具能力候选包及本节提交点；若确认前有未提交文档变更，必须先提交或列入 authorityBaseline
任务包：B2-FR-FAO-CAD-001
目标：新增 FundingResponsibilityResolutionApplicationService 或等价 application facade，在 funding-account-only 策略下完成资金责任关系读取、唯一资金账户责任主体裁决、币种和有效性校验；通过后输出不可变 FundingAllocationDecision 或等价快照；失败无 route、posting、LedgerEntry、余额投影或完整敏感凭证
字段策略：funding-account-only；本 Grant 不声明信用账户、平台账户角色、预算组或 Spend Rule 可作为最终资金责任主体
允许写入：先写 tests 中资金责任解析 application facade 目标 Red；Red 证明缺口后允许 wallet-face application facade 契约、Request/DTO、wallet-impl 最小实现、资源服务适配和必要资源服务回归
允许公共契约：仅允许非破坏性新增 wallet application facade、Request/DTO、返回 DTO 和资金责任决策快照；不得修改交易 canonical 请求、ledger 公共契约或现有资源服务语义
首批 Red：R0-FR-001A；必要时补缺关系、多关系、默认关系、优先级、状态、生效窗口、币种、敏感上下文和预算组/Spend Rule 误输出失败矩阵
验证命令：just test-one FundingResponsibilityResolutionApplicationServiceTests tests；just test-one SpendSubjectFundingRelationServiceImplTests tests；just test-one PaymentInstrumentRouteDslContractTests tests；just test-boundary；just compile；提交前 just pmd 和 git diff --check
禁止写入：targetSubjectType + targetSubjectId 迁移、DDL/H2 schema、Entity、Mapper、route snapshot 字段迁移、authorizeByInstrument、支付工具动作能力、统一 InstrumentTransactionService、Spend Rule、预算控制投影、清结算对账、治理 apply、完整 VCC、P2 轨道、外部协议或敏感原文
Git 策略：auto_commit
停止条件：字段策略越界、表结构、依赖方向反转、外部规则、敏感数据、B4/B5/B6/B8/P2 越界、验证无法解释失败或工作树冲突即停止
交接：确认后从 B2-FR-FAO-CAD-001 首批 Red 开始；未确认时不写 Java、测试、DDL/H2 schema 或运行时配置；若要支持信用账户或平台角色责任主体，改走独立 B2-FR-TARGET 迁移 Grant
```

### 8.7 B5-SR-CONTROL Round 0 扫描（2026-06-04）

本节把 `B5-SR-CONTROL` 从候选列表推进到可确认输入。它只做只读扫描和候选授权包收敛，不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。

| 扫描项 | 结论 |
| --- | --- |
| 当前状态 | `ROUND0_READY_NOT_CODE_AUTHORIZED`。 |
| 兼容代码基线 | 当前可定位的是 `BudgetGroupService` / `BudgetGroupServiceImpl`、`BUDGET_GROUP` ledger profile、余额控制调账和预算组余额查询等兼容路径；`FundsBalanceControlFailureFlowTests` 与 `ControlAccountLedgerInitializationTests` 可作为旧预算组账务兼容回归资产。 |
| 目标缺口 | 当前未发现 `SpendRuleDecisionLog`、`SpendControlActivity`、Spend Rule application facade、规则版本决策模型或预算控制投影生产模型；既有预算组创建初始化 ledger bucket、`BUDGET_GROUP` 余额桶和预算组额度调账，不能证明目标态 Spend Rule 控制事实已生产可用。 |
| 语义裁决 | B5 目标只表达规则决策、拒绝原因、控制活动、预算预留释放和只读控制投影；预算组、Spend Rule、规则命中和控制活动不得成为 route leg、posting、LedgerEntry 或账本余额主体。旧 BudgetGroup ledger 路径只能作为兼容差距和回归保护，不得写成目标态 Done。 |
| schemaDecision | 进入编码前必须在 `contract-only` 与 `ddl-backed` 中二选一。`contract-only` 只允许新增 application facade 契约、Request/DTO 和目标 Red；`ddl-backed` 必须显式授权 DDL/H2 schema、Entity、Mapper、唯一键、幂等摘要和投影表字段。未选择前不写 B5 Java、测试或表结构。 |
| 首批 Red | `R0-SR-001A`：MCC、商户、时间窗、频次、单笔金额、累计金额或规则版本拒绝时必须产生可审计规则决策和拒绝原因，且无 route、posting、LedgerEntry、余额投影或资金交易副作用。 |
| 次批 Red | `R0-SR-002A`：授权预留、撤销、过期、部分完成和退款释放只能写 Spend Control Activity 和预算控制投影，并证明幂等、并发和失败无副作用；不得更新预算组 ledger bucket 或资金交易事实。 |
| Grant 必须列明 | application facade 名称、schemaDecision、Request/DTO、规则版本字段、控制活动字段、拒绝原因、幂等摘要、并发保护、投影边界、目标测试资产、DDL/H2 是否允许和验证命令。 |
| 禁止混入 | 不改支付工具动作能力、资金责任目标主体字段、授权 application facade、直接交易、授权状态机、BudgetGroup ledger 兼容策略、清结算对账、治理 apply、完整 VCC、P2 轨道、外部协议或敏感原文。 |

### 8.8 spendRuleControlGrantCandidate（2026-06-04）

本节把 8.7 的只读扫描推进为可确认的单一 Execution Grant 候选。它仍不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置；只有用户确认本节的 `Execution Grant：B5-SR-CONTROL` 并选择 schemaDecision 后，才允许进入首批 Red。

| 授权包字段 | 候选内容 |
| --- | --- |
| `taskId` | `B5-SR-CONTROL-CAD-001`。 |
| `stage` / `wave` | B5 余额控制 / Wave 1 Spend Rule 决策日志与控制活动准入。 |
| `status` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。 |
| `owner` | 资深架构师负责工程执行；产品架构专家负责规则拒绝原因、控制活动、预算口径和 Not Done 语义复核。 |
| `authorityBaseline` | 确认时 Git HEAD；当前候选至少要求包含 `5a0ee17 docs: 补齐资金责任候选包` 及本节提交点。若确认前出现新的未提交文档变更，必须先提交或列入本 Grant 附件。 |
| `mvpScenario` | 业务入口提交支付工具、使用主体、预算上下文、Spend Rule 规则版本、金额币种和业务流水；系统在 application facade 中完成规则决策。拒绝时只留下规则决策、拒绝原因和审计；批准或授权后继时只记录控制活动和预算控制投影，不生成资金事实。 |
| `businessAdmission` | 产品锚点为支付工具与 Spend Rule 生产可用性裁决；DSL 锚点为 `SpendRuleDecisionLog`、`SpendControlActivity` 和只读预算控制投影；系分锚点为 `SpendRuleControlApplicationService`；TDD 锚点为 `R0-SR-001`、`R0-SR-002`。 |
| `schemaDecision` | 待确认，必须二选一：`contract-only` 或 `ddl-backed`。默认不允许 DDL/H2 schema；若选择 `ddl-backed`，Grant 必须列明表、字段、索引、唯一约束、Entity、Mapper 和 H2 fixture。 |
| `firstRedSet` | `R0-SR-001A`：MCC、商户、时间窗、频次、单笔金额、累计金额或规则版本拒绝仍继续授权或付款必须失败；拒绝必须可审计，且无 route、posting、LedgerEntry、余额投影或资金交易副作用。 |
| `secondRedSet` | `R0-SR-002A`：授权预留、撤销、过期、部分完成和退款释放缺幂等、缺并发保护、更新预算组 ledger bucket 或生成资金交易事实时必须失败。 |
| `writeScope` | 先写 `tests/src/test/java/com/wind/funds/wallet/application/spendrule/SpendRuleControlApplicationServiceTests.java` 或等价 application facade 目标 Red；Red 证明缺口后，`contract-only` 只允许在 `wallet/wallet-face` 新增 facade 契约和 Request/DTO，在 `wallet/wallet-impl` 新增最小空实现或拒绝型适配；`ddl-backed` 还必须由 Grant 显式授权模型、Mapper、DDL/H2 和投影实现。 |
| `readOnlyScope` | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/tasks.md`、既有 `wallet`、`transaction`、`core`、`ledger`、`tests/src/test/resources/jdbc-schema.sql`。 |
| `publicContractGate` | 只允许非破坏性新增 wallet application facade、Request/DTO、返回 DTO 和控制事实快照；不得修改交易 canonical 请求、授权状态机、core 交易枚举、ledger 公共契约或现有 BudgetGroup 资源服务语义。 |
| `schemaGate` | 未确认 `ddl-backed` 前，不修改 `tests/src/test/resources/jdbc-schema.sql`、生产 DDL、Entity 字段、Mapper 表字段、索引或唯一约束；确认 `ddl-backed` 后也只允许 B5 控制事实表和只读投影表，不得借机迁移 BudgetGroup ledger 兼容路径。 |
| `dependencyGate` | `wallet/wallet-face` 不依赖任何 impl；`wallet/wallet-impl` 可依赖 `wallet/wallet-face`、core 和既有资源服务；`transaction`、`ledger` 不反向依赖 wallet impl。违反依赖方向时立即停止。 |
| `noWriteScope` | 不把 BudgetGroup、Spend Rule 或控制活动写成 route leg、posting、LedgerEntry 或账本余额主体；不改 `BUDGET_GROUP` 兼容策略；不实现支付工具动作能力、资金责任迁移、授权 application facade、完整规则引擎、清结算对账、治理 apply、完整 VCC、P2 轨道、外部协议或敏感原文。 |
| `verificationCommand` | 首轮 `just test-one SpendRuleControlApplicationServiceTests tests`；若触碰兼容回归，补 `just test-one FundsBalanceControlFailureFlowTests tests`、`just test-one ControlAccountLedgerInitializationTests tests`、`just test-balance-control`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`。 |
| `gitStrategy` | 若用户确认本 Grant、选择 schemaDecision 并保持 GSD-CAD 自动模式，目标验证通过且未触发停止条件时按 `auto_commit` 提交；验证失败、环境不可判定或越界时转 `summary_only`。 |
| `stopCondition` | 未选择 schemaDecision、需要表结构但未获 `ddl-backed` 授权、需要迁移 BudgetGroup ledger 兼容路径、修改交易 canonical 请求、授权 application facade、资金责任字段、ledger 公共契约、清结算对账、治理、外部规则、敏感数据、依赖方向反转、公有方法超过 5 个参数或工作树冲突时停止。 |
| `handoff` | 本候选包的恢复入口为 `B5-SR-CONTROL-CAD-001`。用户确认 `Execution Grant：B5-SR-CONTROL` 并选择 `contract-only` 或 `ddl-backed` 后进入首批 Red；未确认时只保留为 Round 0 / summary_only。 |

```text
Execution Grant：B5-SR-CONTROL
确认基线：确认时 Git HEAD；至少包含 5a0ee17 docs: 补齐资金责任候选包及本节提交点；若确认前有未提交文档变更，必须先提交或列入 authorityBaseline
任务包：B5-SR-CONTROL-CAD-001
目标：新增 SpendRuleControlApplicationService 或等价 application facade，完成 Spend Rule 规则版本决策、拒绝原因、控制活动、预算预留释放和只读预算控制投影的首轮准入；拒绝或控制失败无 route、posting、LedgerEntry、资金交易或账本余额副作用
schemaDecision：必须选择 contract-only 或 ddl-backed；未选择前不写 Java、测试、DDL/H2 schema 或运行时配置
允许写入：先写 tests 中 Spend Rule 控制 application facade 目标 Red；Red 证明缺口后按 schemaDecision 允许 wallet-face application facade 契约、Request/DTO、wallet-impl 最小实现；只有 ddl-backed 才允许 DDL/H2 schema、Entity、Mapper 和投影实现
允许公共契约：仅允许非破坏性新增 wallet application facade、Request/DTO、返回 DTO 和控制事实快照；不得修改交易 canonical 请求、授权状态机、ledger 公共契约或现有 BudgetGroup 资源服务语义
首批 Red：R0-SR-001A；必要时补 R0-SR-002A，覆盖规则拒绝、授权预留、撤销、过期、部分完成、退款释放、幂等、并发和失败无副作用
验证命令：just test-one SpendRuleControlApplicationServiceTests tests；just test-one FundsBalanceControlFailureFlowTests tests；just test-one ControlAccountLedgerInitializationTests tests；just test-balance-control；just test-boundary；just compile；提交前 just pmd 和 git diff --check
禁止写入：BudgetGroup 或 Spend Rule 账务主体化、BUDGET_GROUP 兼容路径迁移、交易 canonical 请求、authorizeByInstrument、支付工具动作能力、资金责任迁移、统一 InstrumentTransactionService、清结算对账、治理 apply、完整 VCC、P2 轨道、外部协议或敏感原文
Git 策略：auto_commit
停止条件：schemaDecision 未确认、表结构未授权、依赖方向反转、外部规则、敏感数据、B2/B4/B6/B8/P2 越界、验证无法解释失败或工作树冲突即停止
交接：确认后从 B5-SR-CONTROL-CAD-001 首批 Red 开始；未确认时不写 Java、测试、DDL/H2 schema 或运行时配置
```

### 8.9 B6/B8-PI-VIEW Round 0 扫描（2026-06-04）

本节把 `B6/B8-PI-VIEW` 支付工具流水、预算控制视图、规则命中时间线和重放切片推进到可确认输入。它只做只读扫描和候选授权包收敛，不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。

| 扫描项 | 结论 |
| --- | --- |
| 当前状态 | `ROUND0_READY_NOT_CODE_AUTHORIZED`。 |
| 局部代码基线 | `FundsTransactionProjectionPublisher`、`FundsTransactionProjectionPublishContext` 和 `FundsTransactionProjectionExplanation` 已表达交易主写链路成功后的正常只读投影发布入口；`DefaultRoutedFundsInstructionOrchestratorProjectionTests` 覆盖付款、授权占用、授权拒绝和投影失败不回滚事实；`DefaultRouteReplayServiceTests` 覆盖原路径快照、支付工具快照、外部账户和资金责任快照回放；`FundsProjectionReplayServiceTests` 覆盖交易投影有界重放、影子/正式模式和差异报告边界。 |
| 目标缺口 | 当前未形成支付工具维度流水 query DTO、预算控制视图、规则命中时间线、支付工具绑定版本查询投影、Spend Rule 控制投影或面向运营/财务的统一解释查询；治理重放已有局部边界，但不等于完整 B8 Manifest、余额快照、指标水位或大数据消费边界 Done。 |
| 语义裁决 | B6/B8-PI-VIEW 只读解释必须消费交易事实、冻结单、route snapshot、`paymentInstrumentRef`、`FundingAllocationDecision`、`SpendRuleDecisionLog`、`SpendControlActivity`、账本摘要、授权拒绝事实、清结算和对账差错；不得把投影、重放结果、差异报告或查询 DTO 反写成 route、posting、LedgerEntry、余额投影或资金交易事实。 |
| schemaDecision | 进入编码前必须在 `query-contract-only` 与 `projection-store-backed` 中二选一。`query-contract-only` 只允许新增查询契约、DTO 和目标 Red；`projection-store-backed` 必须显式授权 DDL/H2 schema、Entity、Mapper、索引、checkpoint、影子表或正式投影表。未选择前不写 B6/B8 Java、测试或表结构。 |
| 首批 Red | `R0-PI-002A`：工具换绑、解绑、暂停或能力变化后，历史退款、撤销、退费或拒付按当前绑定、当前默认资金责任或当前工具能力重选路必须失败；解释视图必须使用原 route snapshot、原工具快照、原绑定版本和原资金责任决策。 |
| 次批 Red | `R0-PI-002B`：交易投影或治理重放把支付工具流水、预算控制视图、规则命中时间线写回资金事实、账本事实、余额投影、正式治理 apply 或无界全量重放时必须失败。 |
| Grant 必须列明 | 查询 facade 名称、schemaDecision、Query/DTO、视图域、投影来源字段、绑定版本字段、规则和控制活动字段、差异报告字段、checkpoint 策略、影子/正式模式、目标测试资产、DDL/H2 是否允许和验证命令。 |
| 禁止混入 | 不新增资金事实、不重写 route replay 内核、不修改交易 canonical 请求、不迁移 BudgetGroup ledger 兼容路径、不打开 B8 Manifest/余额快照/指标水位完整治理、不混入清结算对账、完整 VCC、P2 轨道、外部协议或敏感原文。 |

### 8.10 paymentInstrumentViewGrantCandidate（2026-06-04）

本节把 8.9 的只读扫描推进为可确认的单一 Execution Grant 候选。它仍不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置；只有用户确认本节的 `Execution Grant：B6-B8-PI-VIEW` 并选择 schemaDecision 后，才允许进入首批 Red。

| 授权包字段 | 候选内容 |
| --- | --- |
| `taskId` | `B6-B8-PI-VIEW-CAD-001`。 |
| `stage` / `wave` | B6 Route Replay 与交易投影 / B8 治理重放边界 / Wave 1 支付工具解释视图准入。 |
| `status` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。 |
| `owner` | 资深架构师负责工程执行；产品架构专家负责使用者解释视图、支付工具流水口径、预算控制视图和 Not Done 语义复核。 |
| `authorityBaseline` | 确认时 Git HEAD；当前候选至少要求包含 `053a6a0 docs: 补齐规则控制候选包` 及本节提交点。若确认前出现新的未提交文档变更，必须先提交或列入本 Grant 附件。 |
| `mvpScenario` | 运营、财务或业务方查询某个支付工具、共享卡、VCC、预算上下文或规则命中时间线时，系统只从交易事实、原 route snapshot、原工具快照、资金责任决策、Spend Rule 决策、控制活动和账本摘要构建解释视图。逆向交易或重放必须沿原快照解释，缺失快照时失败或进入人工处理，不按当前绑定关系重新选路。 |
| `businessAdmission` | 产品锚点为支付工具流水、预算控制视图和可解释输出；DSL 锚点为原 route snapshot、`paymentInstrumentRef`、`FundingAllocationDecision`、`SpendRuleDecisionLog`、`SpendControlActivity` 和 transaction projection；系分锚点为交易投影正常发布与治理重放边界；TDD 锚点为 `R0-PI-002`。 |
| `schemaDecision` | 待确认，必须二选一：`query-contract-only` 或 `projection-store-backed`。默认不允许 DDL/H2 schema；若选择 `projection-store-backed`，Grant 必须列明表、字段、索引、唯一约束、Entity、Mapper、H2 fixture、checkpoint 和重放模式。 |
| `firstRedSet` | `R0-PI-002A`：历史退款、撤销、退费或拒付按当前绑定、当前默认资金责任、当前工具能力或当前规则重新解释必须失败；必须使用原 route snapshot、原工具快照、原绑定版本、原资金责任决策和原控制证据。 |
| `secondRedSet` | `R0-PI-002B`：投影解释、支付工具流水或治理重放写回 route、posting、LedgerEntry、余额投影、资金交易事实，或无界全量重放、正式 apply 越权时必须失败。 |
| `writeScope` | 先写 `tests/src/test/java/com/wind/funds/transaction/projection/PaymentInstrumentProjectionViewTests.java` 或等价查询/投影目标 Red；Red 证明缺口后，`query-contract-only` 只允许新增非破坏性 Query/DTO/facade 契约和最小查询适配；`projection-store-backed` 还必须由 Grant 显式授权 DDL/H2、Entity、Mapper、投影表、checkpoint 和重放实现。 |
| `readOnlyScope` | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/tasks.md`、既有 `transaction`、`governance`、`wallet`、`ledger`、`core`、`tests/src/test/resources/jdbc-schema.sql`。 |
| `publicContractGate` | 只允许非破坏性新增查询 facade、Query/DTO、返回 DTO 和只读解释载荷；不得修改交易 canonical 请求、route replay 公共契约、ledger 公共契约、governance checkpoint 枚举或现有投影发布端口语义，除非 Grant 显式列名。 |
| `schemaGate` | 未确认 `projection-store-backed` 前，不修改 `tests/src/test/resources/jdbc-schema.sql`、生产 DDL、Entity 字段、Mapper 表字段、索引或唯一约束；确认后也只允许 B6/B8 支付工具解释视图和重放相关表，不得借机打开完整 B8 Manifest、余额快照或指标水位。 |
| `dependencyGate` | 查询 facade 和 DTO 只能依赖 face/core 契约；`transaction` 不反向依赖 governance impl，`wallet` 不反向依赖 transaction impl，`ledger` 不依赖 wallet/transaction impl。违反依赖方向时立即停止。 |
| `noWriteScope` | 不新增资金交易事实，不反写 route、posting、LedgerEntry 或余额投影，不实现完整治理 apply、B8 Manifest、账本余额快照、指标水位、大数据消费、清结算对账、完整 VCC、P2 轨道、外部协议或敏感原文。 |
| `verificationCommand` | 首轮 `just test-one PaymentInstrumentProjectionViewTests tests`；若触碰既有回归，补 `just test-one DefaultRouteReplayServiceTests tests`、`just test-one DefaultRoutedFundsInstructionOrchestratorProjectionTests tests`、`just test-one FundsProjectionReplayServiceTests tests`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`。 |
| `gitStrategy` | 若用户确认本 Grant、选择 schemaDecision 并保持 GSD-CAD 自动模式，目标验证通过且未触发停止条件时按 `auto_commit` 提交；验证失败、环境不可判定或越界时转 `summary_only`。 |
| `stopCondition` | 未选择 schemaDecision、需要表结构但未获 `projection-store-backed` 授权、需要修改 route replay 公共契约、governance checkpoint 枚举、交易 canonical 请求、ledger 公共契约、完整 B8 治理、清结算对账、外部规则、敏感数据、依赖方向反转、公有方法超过 5 个参数或工作树冲突时停止。 |
| `handoff` | 本候选包的恢复入口为 `B6-B8-PI-VIEW-CAD-001`。用户确认 `Execution Grant：B6-B8-PI-VIEW` 并选择 `query-contract-only` 或 `projection-store-backed` 后进入首批 Red；未确认时只保留为 Round 0 / summary_only。 |

```text
Execution Grant：B6-B8-PI-VIEW
确认基线：确认时 Git HEAD；至少包含 053a6a0 docs: 补齐规则控制候选包及本节提交点；若确认前有未提交文档变更，必须先提交或列入 authorityBaseline
任务包：B6-B8-PI-VIEW-CAD-001
目标：新增支付工具流水、预算控制视图、规则命中时间线或等价只读解释视图的首轮查询/投影能力；逆向交易和重放必须沿原 route snapshot、原工具快照、原绑定版本、原资金责任决策和原控制证据解释；缺失快照失败或进入人工处理
schemaDecision：必须选择 query-contract-only 或 projection-store-backed；未选择前不写 Java、测试、DDL/H2 schema 或运行时配置
允许写入：先写 tests 中支付工具解释视图目标 Red；Red 证明缺口后按 schemaDecision 允许非破坏性 Query/DTO/facade 契约和最小查询适配；只有 projection-store-backed 才允许 DDL/H2 schema、Entity、Mapper、投影表、checkpoint 和重放实现
允许公共契约：仅允许非破坏性新增查询 facade、Query/DTO、返回 DTO 和只读解释载荷；不得修改交易 canonical 请求、route replay 公共契约、ledger 公共契约、governance checkpoint 枚举或现有投影发布端口语义，除非 Grant 显式列名
首批 Red：R0-PI-002A；必要时补 R0-PI-002B，覆盖换绑后逆向、解绑、暂停、能力变化、原快照缺失、无界重放、投影反写事实和正式 apply 越权
验证命令：just test-one PaymentInstrumentProjectionViewTests tests；just test-one DefaultRouteReplayServiceTests tests；just test-one DefaultRoutedFundsInstructionOrchestratorProjectionTests tests；just test-one FundsProjectionReplayServiceTests tests；just test-boundary；just compile；提交前 just pmd 和 git diff --check
禁止写入：资金交易事实、route/posting/LedgerEntry/余额投影反写、完整治理 apply、B8 Manifest、账本余额快照、指标水位、大数据消费、清结算对账、完整 VCC、P2 轨道、外部协议或敏感原文
Git 策略：auto_commit
停止条件：schemaDecision 未确认、表结构未授权、route replay 公共契约或 governance checkpoint 枚举越界、依赖方向反转、外部规则、敏感数据、B2/B4/B5/B7/P2 越界、验证无法解释失败或工作树冲突即停止
交接：确认后从 B6-B8-PI-VIEW-CAD-001 首批 Red 开始；未确认时不写 Java、测试、DDL/H2 schema 或运行时配置
```

### 8.11 P2-VCC-PREPAID Round 0 扫描（2026-06-04）

本节把 `P2-VCC-PREPAID` 预付卡入金确认切片推进到可确认输入。它属于 P2 VCC 业务专项能力包，不属于 B2/B4/B5/B6-B8 默认编码队列；本节只做只读扫描和候选授权包收敛，不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。

| 扫描项 | 结论 |
| --- | --- |
| 当前状态 | `ROUND0_READY_NOT_CODE_AUTHORIZED`。 |
| 业务设计基线 | PRD、DSL、系分和 OpenSpec 已统一：VCC、虚拟卡、卡 token 和 prepaid virtual card 都先作为 `PaymentInstrumentRef` 或等价工具快照承接；prepaid 是资金模式，不是新的账本主体类型；共享卡是绑定和使用模式，不是新的工具或账户主体类型。 |
| 局部代码基线 | 当前只定位到支付工具资源服务、资金责任关系资源服务、账户主体型直接交易、账户主体型授权交易、route replay 和交易投影局部基线；未发现 `VccPrepaidFundingApplicationService`、预付资金 application facade、VCC prepaid funding 请求模型或 VCC 专项资金流实现。 |
| 目标缺口 | 现有资源服务和直接交易链路不能证明“外部入金或系统内充值已经确认、预付资金责任主体唯一、幂等摘要可追溯、失败无副作用、卡工具不入账”。因此 P2-VCC-PREPAID 只能先作为业务专项 Grant 候选，不得从支付工具资源服务测试直接升级为生产可用。 |
| 语义裁决 | prepaid virtual card 只表达工具和资金模式；可入账对象必须是经财务、合同、合规或业务专项确认的内部资金责任主体。缺确认引用、缺资金来源、缺责任主体、多个责任主体命中、币种不一致或卡工具自身被当主体时必须失败，且不得生成 route、posting、LedgerEntry、余额投影或资金交易事实。 |
| implementationDecision | 进入编码前必须在 `contract-only` 与 `funding-flow-backed` 中二选一。`contract-only` 只允许新增 facade 契约、Request/DTO、目标 Red 和失败型/校验型最小适配；`funding-flow-backed` 才允许委派账户主体型直接交易、内部转账或等价资金事实链路，并必须补余额、route、posting、entry、projection、幂等和审计断言。 |
| responsibilityStrategy | 默认只允许 `funding-account-only`。若 P2 专项要求信用账户、平台角色解析后的平台资金账户或 `targetSubjectType + targetSubjectId`，必须显式授权字段策略、Request/DTO、DDL/H2、Entity、Mapper、摘要、fixture、route snapshot 和回放断言，或先改走 B2-FR-TARGET 迁移 Grant。 |
| 首批 Red | `R0-VCC-PREPAID-001A`：外部入金或系统内充值缺确认引用、缺资金来源、缺唯一内部责任主体、缺幂等摘要、缺操作者/审计、错币种或敏感原文时仍增加可用余额必须失败；失败无 route、posting、LedgerEntry、余额投影或资金交易副作用。 |
| 次批 Red | `R0-VCC-PREPAID-001B`：prepaid virtual card、shared card、卡号、token、payment instrument id 或预算组被作为资金账户、ledger subject、route leg、posting subject、LedgerEntry subject 或余额投影主体时必须失败。 |
| Grant 必须列明 | VCC 业务分册、业务验收 ID、implementationDecision、responsibilityStrategy、外部引用脱敏字段、外部规则核验状态、预付资金责任主体、幂等摘要、审计字段、目标测试资产、P0/P1 回归范围、DDL/H2 是否允许和验证命令。 |
| 禁止混入 | 不实现完整 VCC 发卡产品、processor/PAN/CVV/PCI、卡余额账本、退卡提现自动化、完整 clearing 文件处理、chargeback 全生命周期、税务/会计自动处理、全球账户、收单、ACH/SWIFT/FX 外部协议、清结算对账或治理 apply。 |

### 8.12 vccPrepaidFundingGrantCandidate（2026-06-04）

本节把 8.11 的只读扫描推进为可确认的单一 Execution Grant 候选。它仍不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置；只有用户确认本节的 `Execution Grant：P2-VCC-PREPAID`，并选择 implementationDecision 与责任主体策略后，才允许进入首批 Red。

| 授权包字段 | 候选内容 |
| --- | --- |
| `taskId` | `P2-VCC-PREPAID-CAD-001`。 |
| `stage` / `wave` | P2 VCC 业务模式能力包 / Wave 1 预付资金确认准入。 |
| `status` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。 |
| `owner` | 产品架构专家负责 VCC 业务语义、资金模式、外部规则、合规和 Not Done 口径复核；资深架构师负责工程边界、接口、测试、验证和提交闭环。 |
| `authorityBaseline` | 确认时 Git HEAD；当前候选至少要求包含 `0e08472 docs: 补齐支付工具视图候选包` 及本节提交点。若确认前出现新的未提交文档变更，必须先提交或列入本 Grant 附件。 |
| `mvpScenario` | 业务方提交 VCC prepaid funding 事件、支付工具引用、外部确认引用、资金来源、目标预付责任主体、金额币种、业务流水、幂等键和操作者；系统确认工具只是 VCC/prepaid 工具快照，资金责任唯一且已确认。失败时只返回可解释原因并留下审计；成功时按 implementationDecision 进入 contract-only 结果或账户主体型资金事实委派。 |
| `businessAdmission` | 产品锚点为 `VCC-AC-007`、`VCC-AC-008`；DSL 锚点为 `DSL-PAYMENT-INSTRUMENT-PREPAID-CARD-001`、prepaid virtual card 不作为 ledger subject、缺确认不入账；系分锚点为 `PostPrepaidFunding` 和 `VccPrepaidFundingApplicationService`；TDD 锚点为 `R0-VCC-PREPAID-001`、`TDD-P2-VCC-004` 至 `TDD-P2-VCC-011` 中被本 Grant 明确选中的子集。 |
| `implementationDecision` | 待确认，必须二选一：`contract-only` 或 `funding-flow-backed`。默认不得使用资金流实现；若选择 `funding-flow-backed`，必须同步声明委派的账户主体型直接交易、内部转账或等价资金事实入口，以及 P0/P1 回归测试。 |
| `responsibilityStrategy` | 默认 `funding-account-only`；若选择 `targetSubjectType + targetSubjectId` 或需要信用账户、平台角色责任主体，必须另行扩权或改走 B2-FR-TARGET 迁移 Grant。 |
| `firstRedSet` | `R0-VCC-PREPAID-001A`：缺确认引用、缺资金来源、缺唯一内部责任主体、缺幂等摘要、缺操作者/审计、错币种或敏感原文时仍加余额必须失败；失败无 route、posting、LedgerEntry、余额投影或资金交易副作用。 |
| `secondRedSet` | `R0-VCC-PREPAID-001B`：prepaid virtual card、shared card、卡号、token、payment instrument id、预算组或 Spend Rule 被作为资金账户、账本主体、route leg、posting subject、LedgerEntry subject 或余额投影主体时必须失败。 |
| `writeScope` | 先写 `tests/src/test/java/com/wind/funds/wallet/application/vcc/VccPrepaidFundingApplicationServiceTests.java` 或等价 VCC prepaid funding 目标 Red；Red 证明缺口后，`contract-only` 只允许在 `wallet/wallet-face` 新增 facade 契约、Request/DTO、返回 DTO，并在 `wallet/wallet-impl` 新增校验型或拒绝型最小实现；`funding-flow-backed` 还必须由 Grant 显式授权账户主体型资金事实委派、余额断言和必要回归。 |
| `readOnlyScope` | `docs/产品设计/06-VCC发卡业务资金底座PRD.md`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/tasks.md`、既有 `wallet`、`transaction`、`ledger`、`core`、`tests/src/test/resources/jdbc-schema.sql`。 |
| `publicContractGate` | 只允许非破坏性新增 wallet VCC application facade、Request/DTO、返回 DTO 和脱敏审计载荷；不得修改交易 canonical 请求、授权状态机、core 交易枚举、ledger 公共契约、支付工具资源服务语义或现有资金责任关系资源服务语义。 |
| `schemaGate` | 未显式授权前，不修改 `tests/src/test/resources/jdbc-schema.sql`、生产 DDL、Entity 字段、Mapper 表字段、索引或唯一约束；若需要 VCC prepaid funding fact、外部确认表、幂等表或投影表，必须重新确认 DDL/H2 范围。 |
| `dependencyGate` | `wallet/wallet-face` 不依赖任何 impl；`wallet/wallet-impl` 可依赖 `wallet/wallet-face`、`transaction-face`、`ledger-face` 和 core；`transaction`、`ledger` 不反向依赖 wallet impl。违反依赖方向时立即停止。 |
| `noWriteScope` | 不把卡工具、卡 token、shared card、预算组或 Spend Rule 写成 route leg、posting、LedgerEntry 或账本余额主体；不新增卡余额账本、卡账户、完整发卡处理商协议、PAN/CVV/PCI、退卡提现自动化、完整 clearing、chargeback 全生命周期、全球账户、收单、ACH/SWIFT/FX、税务/会计自动处理、清结算对账或治理 apply。 |
| `verificationCommand` | 首轮 `just test-one VccPrepaidFundingApplicationServiceTests tests`；`contract-only` 触碰 wallet facade 时补 `just test-one PaymentInstrumentServiceImplTests tests`、`just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`；`funding-flow-backed` 还必须补 `just test-one FundsDirectTransactionFlowTests tests`、`just test-transaction` 和必要业务 flow 回归。 |
| `gitStrategy` | 若用户确认本 Grant、选择 implementationDecision 和责任主体策略并保持 GSD-CAD 自动模式，目标验证通过且未触发停止条件时按 `auto_commit` 提交；验证失败、环境不可判定或越界时转 `summary_only`。 |
| `stopCondition` | 未选择 implementationDecision、责任主体策略不清、外部规则核验状态缺失、需要表结构但未获 DDL/H2 授权、需要修改交易 canonical 请求、ledger 公共契约、支付工具资源服务语义、资金责任目标字段、完整 VCC 发卡、清结算对账、治理、外部协议、敏感数据、依赖方向反转、公有方法超过 5 个参数或工作树冲突时停止。 |
| `handoff` | 本候选包的恢复入口为 `P2-VCC-PREPAID-CAD-001`。用户确认 `Execution Grant：P2-VCC-PREPAID`、选择 `contract-only` 或 `funding-flow-backed`、并确认 `funding-account-only` 或目标主体迁移策略后进入首批 Red；未确认时只保留为 P2 业务专项 Round 0 / summary_only。 |

```text
Execution Grant：P2-VCC-PREPAID
确认基线：确认时 Git HEAD；至少包含 0e08472 docs: 补齐支付工具视图候选包及本节提交点；若确认前有未提交文档变更，必须先提交或列入 authorityBaseline
任务包：P2-VCC-PREPAID-CAD-001
目标：新增 VccPrepaidFundingApplicationService 或等价 application facade，完成 VCC prepaid funding 事件的工具快照、外部确认引用、资金来源、唯一内部预付责任主体、金额币种、幂等摘要和审计准入；prepaid virtual card 只作为 PaymentInstrumentRef，不作为资金账户、ledger subject 或余额主体
implementationDecision：必须选择 contract-only 或 funding-flow-backed；未选择前不写 Java、测试、DDL/H2 schema 或运行时配置
responsibilityStrategy：默认 funding-account-only；如需 targetSubjectType + targetSubjectId、信用账户或平台角色责任主体，必须另行扩权或改走 B2-FR-TARGET 迁移 Grant
允许写入：先写 tests 中 VCC prepaid funding 目标 Red；Red 证明缺口后按 implementationDecision 允许 wallet-face VCC application facade 契约、Request/DTO、wallet-impl 校验型或拒绝型最小实现；只有 funding-flow-backed 才允许账户主体型资金事实委派、余额断言和必要 P0/P1 回归
允许公共契约：仅允许非破坏性新增 wallet VCC application facade、Request/DTO、返回 DTO 和脱敏审计载荷；不得修改交易 canonical 请求、授权状态机、ledger 公共契约、支付工具资源服务语义或资金责任关系资源服务语义
首批 Red：R0-VCC-PREPAID-001A；必要时补 R0-VCC-PREPAID-001B，覆盖缺确认、缺资金来源、缺唯一责任主体、错币种、敏感原文、卡工具入账和失败无副作用
验证命令：just test-one VccPrepaidFundingApplicationServiceTests tests；just test-one PaymentInstrumentServiceImplTests tests；just test-one SpendSubjectFundingRelationServiceImplTests tests；若 funding-flow-backed 则补 just test-one FundsDirectTransactionFlowTests tests 和 just test-transaction；just test-boundary；just compile；提交前 just pmd 和 git diff --check
禁止写入：卡工具或共享卡账务主体化、卡余额账本、完整发卡 processor/PAN/CVV/PCI、退卡提现自动化、完整 clearing、chargeback 全生命周期、全球账户、收单、ACH/SWIFT/FX、税务/会计自动处理、清结算对账、治理 apply、外部协议或敏感原文
Git 策略：auto_commit
停止条件：implementationDecision 未确认、责任主体策略未确认、外部规则核验缺失、表结构未授权、依赖方向反转、外部协议或敏感数据越界、P0/P1 回归无法解释失败或工作树冲突即停止
交接：确认后从 P2-VCC-PREPAID-CAD-001 首批 Red 开始；未确认时不写 Java、测试、DDL/H2 schema 或运行时配置，也不得把 P2 业务 pack 当作 P0/P1 默认编码授权
```

### 8.13 P2-VCC-LIFECYCLE Round 0 扫描（2026-06-04）

本节把 `P2-VCC-LIFECYCLE` 共享卡和预付卡授权后生命周期回放切片推进到可确认输入。它属于 P2 VCC 业务专项能力包，不属于 B2/B4/B5/B6-B8 默认编码队列；本节只做只读扫描和候选授权包收敛，不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。

| 扫描项 | 结论 |
| --- | --- |
| 当前状态 | `ROUND0_READY_NOT_CODE_AUTHORIZED`。 |
| 业务设计基线 | PRD、DSL、系分和 OpenSpec 已统一：共享卡或预付卡的清算、释放、退款和争议/拒付后继事件必须引用原授权、原 route snapshot、原工具快照、原绑定版本和原资金责任决策；当前卡绑定、当前预算组、当前 Spend Rule 或当前默认资金责任不得改变历史资金路径。 |
| 局部代码基线 | 交易层已有账户主体型授权生命周期局部能力，包括授权、完成、过期释放、撤销、无授权退款、争议退款可区分性、route replay 和交易投影回归；但未发现 `InstrumentTransactionLifecycleApplicationService`、`VccSharedCardTransactionApplicationService`、VCC lifecycle request model 或面向共享卡/预付卡的 application facade。 |
| 目标缺口 | 现有 canonical 生命周期能力不能证明 VCC 产品侧事件已完成原授权引用、原快照引用、绑定版本、资金责任决策、外部事件幂等、部分清算/释放金额闭合和争议/普通退款分流。因此 P2-VCC-LIFECYCLE 只能作为业务专项 Grant 候选，不得从交易层生命周期测试直接升级为 VCC 生产可用。 |
| 语义裁决 | VCC 后继事件只做产品侧编排和原路径回放准入；资金事实仍应委派账户主体型 canonical 生命周期能力。缺原授权、缺原 route snapshot、缺原工具快照、缺原资金责任决策、缺外部事件引用、金额超过原授权剩余额度或绑定版本不匹配时必须失败或进入差错/人工入口，不得按当前绑定重新选路。 |
| implementationDecision | 进入编码前必须在 `contract-only` 与 `canonical-lifecycle-backed` 中二选一。`contract-only` 只允许新增 facade 契约、Request/DTO、目标 Red 和失败型/校验型最小适配；`canonical-lifecycle-backed` 才允许委派 `FundsAuthorizationTransactionService` 的账户主体型 settle、reversal、expire、settleRefund 或 Grant 明确列名的等价生命周期入口，并必须补 route、posting、entry、projection、幂等、金额闭合和失败无副作用断言。 |
| snapshotDecision | 默认 `original-snapshot-required`；原授权引用、原 route snapshot、原工具快照、原绑定版本和原资金责任决策缺失时进入差错/人工入口或可解释失败，不允许使用当前绑定、当前预算组、当前 Spend Rule 或当前默认资金责任补算。 |
| disputeDecision | 默认 `settleRefund-dispute-semantic`，即争议/拒付首轮通过授权退款语义携带 dispute 字段、凭证、外部引用和审计上下文保持可区分；独立 chargeback 公共入口、完整 case 管理、卡组织 representment 和追偿生命周期不属于默认写入范围，除非 Grant 显式扩权。 |
| 首批 Red | `R0-VCC-LC-001A`：共享卡换绑、预算或资金责任关系变化后，历史清算、释放、退款或争议事件若按当前绑定、当前默认资金责任、当前预算组或当前 Spend Rule 重新选路必须失败；失败无 route、posting、LedgerEntry、余额投影或交易投影副作用。 |
| 次批 Red | `R0-VCC-LC-001B`：缺原授权、缺原 route snapshot、缺原工具快照、缺原资金责任决策、外部事件重复同键不同摘要、普通退款与争议/拒付合并不可区分、清算或释放金额超过剩余额度时必须失败或进入差错/人工入口。 |
| Grant 必须列明 | VCC 业务分册、业务验收 ID、implementationDecision、snapshotDecision、disputeDecision、原授权引用字段、原 route snapshot 引用字段、原工具快照引用字段、原资金责任决策引用字段、外部事件脱敏引用、幂等摘要、金额闭合规则、差错/人工入口、P0/P1 回归范围、DDL/H2 是否允许和验证命令。 |
| 禁止混入 | 不实现完整 clearing 文件处理、完整 dispute/chargeback case、representment、chargeback 追偿、FX 和费用自动入账、processor/PAN/CVV/PCI、卡余额账本、卡账户、全球账户、收单、ACH/SWIFT/FX、清结算对账或治理 apply。 |

### 8.14 vccLifecycleGrantCandidate（2026-06-04）

本节把 8.13 的只读扫描推进为可确认的单一 Execution Grant 候选。它仍不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置；只有用户确认本节的 `Execution Grant：P2-VCC-LIFECYCLE`，并选择 implementationDecision、snapshotDecision 与 disputeDecision 后，才允许进入首批 Red。

| 授权包字段 | 候选内容 |
| --- | --- |
| `taskId` | `P2-VCC-LIFECYCLE-CAD-001`。 |
| `stage` / `wave` | P2 VCC 业务模式能力包 / Wave 2 共享卡和预付卡授权后生命周期回放准入。 |
| `status` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。 |
| `owner` | 产品架构专家负责 VCC 清算、释放、退款、争议/拒付、外部规则和 Not Done 口径复核；资深架构师负责工程边界、接口、测试、验证和提交闭环。 |
| `authorityBaseline` | 确认时 Git HEAD；当前候选至少要求包含 `4012233 docs: 补齐预付卡资金候选包` 及本节提交点。若确认前出现新的未提交文档变更，必须先提交或列入本 Grant 附件。 |
| `mvpScenario` | 业务方提交已授权 VCC shared/prepaid card 的清算、释放、退款或争议事件，携带原授权引用、原 route snapshot 引用、原工具快照引用、原绑定版本、原资金责任决策引用、外部事件引用、金额币种、幂等键和操作者；系统验证后按 implementationDecision 返回 contract-only 结果或委派账户主体型 canonical 生命周期能力。失败时只返回可解释原因并保留差错/审计，不生成资金事实。 |
| `businessAdmission` | 产品锚点为 `VCC-AC-003`、`VCC-AC-004`、`VCC-AC-005`、`VCC-AC-006`、`VCC-AC-008`；DSL 锚点为 VCC 后继事件沿原 route snapshot 回放、不按当前绑定重选路、争议退款保持可区分；系分锚点为 `SettleInstrumentAuthorization`、`ReleaseInstrumentAuthorization`、`RefundInstrumentTransaction`、`DisputeInstrumentRefund` 或等价 VCC lifecycle facade；TDD 锚点为 `R0-VCC-LC-001` 和被本 Grant 明确选中的 `TDD-P2-VCC-*` 子集。 |
| `implementationDecision` | 待确认，必须二选一：`contract-only` 或 `canonical-lifecycle-backed`。默认不得委派资金生命周期实现；若选择 `canonical-lifecycle-backed`，Grant 必须列明可委派的账户主体型 canonical 方法、请求字段映射、P0/P1 回归测试和金额闭合断言。 |
| `snapshotDecision` | 默认 `original-snapshot-required`；若任何原授权、原 route snapshot、原工具快照、原绑定版本或原资金责任决策缺失，不得当前补算，只能失败或进入差错/人工入口。 |
| `disputeDecision` | 默认 `settleRefund-dispute-semantic`；首轮只允许通过授权退款语义表达争议/拒付可区分字段。独立 chargeback 公共入口、完整 case、representment 或追偿链路必须另行扩权。 |
| `firstRedSet` | `R0-VCC-LC-001A`：换绑、解绑、预算或资金责任变化后，历史清算、释放、退款或争议事件按当前绑定、当前默认责任主体、当前预算组或当前 Spend Rule 重新选路必须失败；失败无 route、posting、LedgerEntry、余额投影或交易投影副作用。 |
| `secondRedSet` | `R0-VCC-LC-001B`：缺原授权、缺原 route snapshot、缺原工具快照、缺原资金责任决策、外部事件重复同键不同摘要、普通退款和争议/拒付不可区分、部分清算/释放金额不闭合或超剩余额度时必须失败或进入差错/人工入口。 |
| `writeScope` | 先写 `tests/src/test/java/com/wind/funds/wallet/application/vcc/VccInstrumentLifecycleApplicationServiceTests.java` 或等价 VCC lifecycle 目标 Red；Red 证明缺口后，`contract-only` 只允许在 `wallet/wallet-face` 新增 facade 契约、Request/DTO、返回 DTO，并在 `wallet/wallet-impl` 新增校验型或拒绝型最小实现；`canonical-lifecycle-backed` 还必须由 Grant 显式授权账户主体型 canonical 生命周期委派和必要 P0/P1 回归。 |
| `readOnlyScope` | `docs/产品设计/06-VCC发卡业务资金底座PRD.md`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/tasks.md`、既有 `wallet`、`transaction`、`ledger`、`core`、`tests/src/test/resources/jdbc-schema.sql`。 |
| `publicContractGate` | 只允许非破坏性新增 wallet VCC lifecycle application facade、Request/DTO、返回 DTO 和脱敏审计载荷；不得修改交易 canonical 请求、授权状态机、core 交易枚举、ledger 公共契约、route replay 公共契约、支付工具资源服务语义或资金责任关系资源服务语义，除非 Grant 显式列名。 |
| `schemaGate` | 未显式授权前，不修改 `tests/src/test/resources/jdbc-schema.sql`、生产 DDL、Entity 字段、Mapper 表字段、索引或唯一约束；若需要 VCC lifecycle event fact、差错表、case 表、幂等表或投影表，必须重新确认 DDL/H2 范围。 |
| `dependencyGate` | `wallet/wallet-face` 不依赖任何 impl；`wallet/wallet-impl` 可依赖 `wallet/wallet-face`、`transaction-face`、`ledger-face` 和 core；`transaction`、`ledger` 不反向依赖 wallet impl。违反依赖方向时立即停止。 |
| `noWriteScope` | 不按当前绑定、当前预算组、当前 Spend Rule 或当前默认资金责任重新选路；不新增独立 chargeback 公共入口、完整 case 管理、representment、追偿生命周期、卡余额账本、卡账户、完整 clearing 文件处理、processor/PAN/CVV/PCI、FX/费用自动入账、清结算对账、治理 apply、全球账户、收单、ACH/SWIFT/FX 或敏感原文。 |
| `verificationCommand` | 首轮 `just test-one VccInstrumentLifecycleApplicationServiceTests tests`；`contract-only` 触碰 wallet facade 时补 `just test-one PaymentInstrumentServiceImplTests tests`、`just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-one DefaultRouteReplayServiceTests tests`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`；`canonical-lifecycle-backed` 还必须补 `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction` 和必要业务 flow 回归。 |
| `gitStrategy` | 若用户确认本 Grant、选择 implementationDecision、snapshotDecision、disputeDecision 并保持 GSD-CAD 自动模式，目标验证通过且未触发停止条件时按 `auto_commit` 提交；验证失败、环境不可判定或越界时转 `summary_only`。 |
| `stopCondition` | 未选择 implementationDecision、snapshotDecision 或 disputeDecision，原快照引用策略不清，外部规则核验状态缺失，需要表结构但未获 DDL/H2 授权，需要修改交易 canonical 请求、授权状态机、route replay 公共契约、ledger 公共契约、完整 chargeback case、清结算对账、治理、外部协议、敏感数据、依赖方向反转、公有方法超过 5 个参数或工作树冲突时停止。 |
| `handoff` | 本候选包的恢复入口为 `P2-VCC-LIFECYCLE-CAD-001`。用户确认 `Execution Grant：P2-VCC-LIFECYCLE`、选择 `contract-only` 或 `canonical-lifecycle-backed`、确认 `original-snapshot-required` 或等价快照策略，并确认 disputeDecision 后进入首批 Red；未确认时只保留为 P2 业务专项 Round 0 / summary_only。 |

```text
Execution Grant：P2-VCC-LIFECYCLE
确认基线：确认时 Git HEAD；至少包含 4012233 docs: 补齐预付卡资金候选包及本节提交点；若确认前有未提交文档变更，必须先提交或列入 authorityBaseline
任务包：P2-VCC-LIFECYCLE-CAD-001
目标：新增 VccInstrumentLifecycleApplicationService 或等价 application facade，完成 VCC shared/prepaid card 清算、释放、退款和争议事件的原授权引用、原 route snapshot、原工具快照、原绑定版本、原资金责任决策、外部事件引用、金额币种、幂等摘要和审计准入；后继事件必须沿原路径回放，不按当前绑定或当前规则重新选路
implementationDecision：必须选择 contract-only 或 canonical-lifecycle-backed；未选择前不写 Java、测试、DDL/H2 schema 或运行时配置
snapshotDecision：默认 original-snapshot-required；缺原授权、原 route snapshot、原工具快照、原绑定版本或原资金责任决策时不得当前补算，只能失败或进入差错/人工入口
disputeDecision：默认 settleRefund-dispute-semantic；独立 chargeback 公共入口、完整 case、representment 和追偿链路不在首轮范围内
允许写入：先写 tests 中 VCC lifecycle 目标 Red；Red 证明缺口后按 implementationDecision 允许 wallet-face VCC lifecycle application facade 契约、Request/DTO、wallet-impl 校验型或拒绝型最小实现；只有 canonical-lifecycle-backed 才允许账户主体型 canonical 生命周期委派和必要 P0/P1 回归
允许公共契约：仅允许非破坏性新增 wallet VCC lifecycle application facade、Request/DTO、返回 DTO 和脱敏审计载荷；不得修改交易 canonical 请求、授权状态机、route replay 公共契约、ledger 公共契约、支付工具资源服务语义或资金责任关系资源服务语义
首批 Red：R0-VCC-LC-001A；必要时补 R0-VCC-LC-001B，覆盖换绑后逆向、原快照缺失、外部事件幂等冲突、退款与争议可区分、金额闭合和失败无副作用
验证命令：just test-one VccInstrumentLifecycleApplicationServiceTests tests；just test-one DefaultRouteReplayServiceTests tests；若 canonical-lifecycle-backed 则补 just test-one FundsAuthorizationTransactionFlowTests tests 和 just test-transaction；just test-one PaymentInstrumentServiceImplTests tests；just test-one SpendSubjectFundingRelationServiceImplTests tests；just test-boundary；just compile；提交前 just pmd 和 git diff --check
禁止写入：当前绑定重选路、独立 chargeback 公共入口、完整 case 管理、representment、追偿生命周期、完整 clearing 文件处理、卡余额账本、processor/PAN/CVV/PCI、FX/费用自动入账、清结算对账、治理 apply、外部协议或敏感原文
Git 策略：auto_commit
停止条件：implementationDecision、snapshotDecision 或 disputeDecision 未确认，原快照引用策略不清，外部规则核验缺失，表结构未授权，依赖方向反转，外部协议或敏感数据越界，P0/P1 回归无法解释失败或工作树冲突即停止
交接：确认后从 P2-VCC-LIFECYCLE-CAD-001 首批 Red 开始；未确认时不写 Java、测试、DDL/H2 schema 或运行时配置，也不得把 P2 业务 pack 当作 P0/P1 默认编码授权
```

## 9. verificationPlan

| 阶段 | 命令 | 通过口径 |
| --- | --- | --- |
| Round 0 | `git status --short`、目标文档索引检查、目标测试资产定位。 | 工作树变更已分类；目标 Red 和 target assets 可定位。 |
| 资源基线回归 | `just test-one PaymentInstrumentServiceImplTests tests`、`just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-one PaymentInstrumentRouteDslContractTests tests`。 | 既有局部基线保持通过；失败时先区分环境、测试漂移和真实回归。 |
| 边界回归 | `just test-boundary`。 | 不破坏 face/impl、wallet/transaction/ledger 模块边界。 |
| 提交前 | `git diff --check`、必要时 `just pmd`。 | 文档或代码格式干净；若未改代码可只运行文档检查和 diff 检查。 |

## 10. stopConditions

1. 需要新增或修改公共契约、枚举、Request/DTO、状态机、表结构、H2 schema 或 Mapper，但 Execution Grant 未授权。
2. Red 目标开始触碰清结算、对账、归档、正式重投影、P2 轨道协议、外部规则或敏感数据。
3. 测试只能证明资源服务通过，不能证明准入、资金责任、拒绝无副作用、route/posting/entry 禁止事实。
4. 发现目标态要求信用账户或平台角色责任主体，但现有字段只能表达 `fundingAccountId`，且未确认迁移策略。
5. 工作树存在未分类变更，或本轮未提交文档基线未被纳入 `authorityBaseline`。
6. B5-SR-CONTROL 需要 DDL/H2 schema、Entity、Mapper、索引或投影表，但 Execution Grant 未明确选择 `ddl-backed`。
7. B6/B8-PI-VIEW 需要 DDL/H2 schema、Entity、Mapper、索引、checkpoint、影子表或正式投影表，但 Execution Grant 未明确选择 `projection-store-backed`。
8. P2-VCC-PREPAID 未确认 implementationDecision、责任主体策略、外部规则核验状态或 DDL/H2 范围，却开始写 VCC prepaid funding 生产代码、测试代码、公共契约、表结构或运行时配置。
9. P2-VCC-LIFECYCLE 未确认 implementationDecision、snapshotDecision、disputeDecision、原快照引用策略、外部规则核验状态或 DDL/H2 范围，却开始写 VCC lifecycle 生产代码、测试代码、公共契约、表结构或运行时配置。

## 11. confirmationTemplate

```text
Execution Grant：B2/B4/P2 支付工具与 Spend Rule Round 0
确认基线：确认时 Git HEAD；若本轮文档尚未提交，需显式纳入 authorityBaseline
选择切片：B2-PI-CAP / B2-FR / B4-AUTH-PI / P2-VCC-PREPAID / P2-VCC-LIFECYCLE / B5-SR-CONTROL / B6-B8-PI-VIEW 之一
允许写入：仅限所选切片的测试资产和最小 application facade / DTO / 适配实现；DDL/H2 默认不允许；若选择 P2-VCC-PREPAID，必须同步选择 contract-only 或 funding-flow-backed 以及责任主体策略；若选择 P2-VCC-LIFECYCLE，必须同步选择 contract-only 或 canonical-lifecycle-backed、原快照策略和 disputeDecision；若选择 B5-SR-CONTROL，必须同步选择 contract-only 或 ddl-backed；若选择 B6-B8-PI-VIEW，必须同步选择 query-contract-only 或 projection-store-backed
禁止写入：交易 canonical 请求替换、统一 InstrumentTransactionService、预算组账务主体、资金责任字段策略混用、清结算对账、治理 apply、P2 轨道协议、敏感原文
首批 Red：按所选切片选择 R0-PI-001、R0-FR-001、R0-AUTH-001、R0-VCC-PREPAID-001、R0-VCC-LC-001、R0-SR-001、R0-SR-002 或 R0-PI-002
验证命令：just test-one PaymentInstrumentServiceImplTests tests；just test-one SpendSubjectFundingRelationServiceImplTests tests；just test-one PaymentInstrumentRouteDslContractTests tests；必要时 just test-transaction / just test-boundary；提交前 git diff --check 和 just pmd
停止条件：公共契约、表结构、外部规则、P2 轨道、清结算对账、治理、敏感数据或工作树冲突越界即停止
```

确认本模板前，本文档只作为 Round 0 准入卡和 TDD 分析产物，不进入编码。
