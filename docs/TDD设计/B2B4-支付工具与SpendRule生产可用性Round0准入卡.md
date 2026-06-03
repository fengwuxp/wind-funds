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
| 支付工具能力准入 | `PaymentInstrumentCapabilityApplicationService` | `com.capte.funds.wallet.application.instrument` | `com.capte.funds.wallet.application.instrument.impl` | B2-PI-CAP |
| 资金责任解析 | `FundingResponsibilityResolutionApplicationService` | `com.capte.funds.wallet.application.funding` | `com.capte.funds.wallet.application.funding.impl` | B2-FR |
| 授权支付工具入口 | `AuthorizationAdmissionApplicationService` | `com.capte.funds.wallet.application.instrument` | `com.capte.funds.wallet.application.instrument.impl` | B4-AUTH-PI |
| 授权后清算/释放/逆向 | `InstrumentTransactionLifecycleApplicationService` | `com.capte.funds.wallet.application.instrument` | `com.capte.funds.wallet.application.instrument.impl` | P2-VCC-LIFECYCLE |
| VCC 预付资金处理 | `VccPrepaidFundingApplicationService` | `com.capte.funds.wallet.application.vcc` | `com.capte.funds.wallet.application.vcc.impl` | P2-VCC-PREPAID |
| VCC 共享卡场景编排 | `VccSharedCardTransactionApplicationService` | `com.capte.funds.wallet.application.vcc` | `com.capte.funds.wallet.application.vcc.impl` | P2-VCC-LIFECYCLE |

Request/DTO 默认落 `com.capte.funds.wallet.model.request` 和 `com.capte.funds.wallet.model.dto`；若模型数量超过单一切片需要，可在 Execution Grant 中允许增加 `instrument`、`funding` 或 `vcc` 子包。禁止新增顶层 `com.capte.funds.instrument`，禁止让 `transaction-impl` 反向依赖钱包资源服务。

## 4.2 transactionLayerCandidate

交易层需要继续完善账户主体型能力，但本卡不把这些能力包装成支付工具交易入口。交易层后续可独立进入 B3/B4/B5/B6 Execution Grant；支付工具 facade 只作为触发方、快照提供方或投影归因方。

| 交易层能力 | 是否可后续完善 | 典型写入范围 | 禁止混入 |
| --- | --- | --- | --- |
| 授权过期释放 | 已完成 B4-TRX-EXPIRE 基础能力，后续只作为回归基线或扩展切片。 | `FundsAuthorizationTransactionService#expire`、`FundsAuthorizationTransactionExpireRequest`、`EXPIRE` 事件、transaction-impl、route replay、授权流测试已由 `b0666ba` 闭合。 | 支付工具主体入参、卡账本、预算组入账；不得借过期释放扩展强制完成、无授权退款或 VCC 生命周期。 |
| 受控强制完成 | 可以，B4 独立切片。 | settle 请求、策略字段、审计字段、金额边界测试。 | 用强制完成伪造授权占用或绕过原路径。 |
| 无授权直接退款 | 可以，B3/B4 独立切片。 | `authorizationTransactionSn` 空值语义、`externalReferenceSn`、退款原因、操作者/审计、`NO_AUTH` 内部上下文标签和失败无副作用测试。 | 缺外部引用、缺原因或缺审计仍静默退款，携带内部授权流水，或按当前工具绑定选路。 |
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

| 切片 | 优先级 | 目标 | 首批 Red | 允许写入建议 | 不适合混入 |
| --- | --- | --- | --- | --- | --- |
| B2-PI-CAP | 1 | 支付工具能力准入 application facade。 | `R0-PI-001`。 | wallet-face/impl 的 facade 契约和测试；必要的 DTO。 | 授权状态机、Spend Rule 表、交易投影。 |
| B2-FR | 2 | 资金责任目标主体解析。 | `R0-FR-001`。 | 资金责任关系契约和测试；必须先选择 `funding-account-only` 或 `targetSubjectType + targetSubjectId`，迁移目标主体字段需单独授权 DTO、DDL/H2、摘要和 fixture。 | 直接交易、清结算、P2 轨道、混合字段策略。 |
| B4-AUTH-PI | 3 | 授权支付工具 application facade。 | `R0-AUTH-001`。 | 授权准入 facade、委派适配和边界测试。 | 替换授权内核请求、完整 VCC 发卡。 |
| P2-VCC-PREPAID | 4 | 预付卡入金确认、系统内充值和未确认阻断。 | `R0-VCC-PREPAID-001`。 | 预付资金 application facade、直接交易/内部转账适配、余额投影断言；需单独确认责任主体字段策略。 | 通用储值账户、退卡提现自动化、税务/会计自动处理。 |
| P2-VCC-LIFECYCLE | 5 | 共享卡和预付卡清算、释放、退款、拒付原路径回放。 | `R0-VCC-LC-001`。 | 授权生命周期 facade、route replay、差错入口和重复损失防护。 | 完整 clearing 文件处理、chargeback 全生命周期、FX 和费用自动入账。 |
| B5-SR-CONTROL | 6 | Spend Rule 决策日志和预算预留释放。 | `R0-SR-001`、`R0-SR-002`。 | 规则定义、控制活动、预算控制投影，需单独 DDL/H2 授权。 | A1、B2 基础能力、P2 轨道。 |
| B6/B8-PI-VIEW | 7 | 支付工具流水、预算控制视图、规则命中时间线和重放。 | `R0-PI-002`。 | 只读投影、重放范围和差异报告，需单独授权。 | 事实反写、正式治理 apply。 |

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

## 11. confirmationTemplate

```text
Execution Grant：B2/B4 支付工具与 Spend Rule Round 0
确认基线：确认时 Git HEAD；若本轮文档尚未提交，需显式纳入 authorityBaseline
选择切片：B2-PI-CAP / B2-FR / B4-AUTH-PI / B5-SR-CONTROL / B6-B8-PI-VIEW 之一
允许写入：仅限所选切片的测试资产和最小 application facade / DTO / 适配实现；DDL/H2 默认不允许
禁止写入：交易 canonical 请求替换、统一 InstrumentTransactionService、预算组账务主体、资金责任字段策略混用、清结算对账、治理 apply、P2 轨道协议、敏感原文
首批 Red：按所选切片选择 R0-PI-001、R0-FR-001、R0-AUTH-001、R0-SR-001、R0-SR-002 或 R0-PI-002
验证命令：just test-one PaymentInstrumentServiceImplTests tests；just test-one SpendSubjectFundingRelationServiceImplTests tests；just test-one PaymentInstrumentRouteDslContractTests tests；必要时 just test-transaction / just test-boundary；提交前 git diff --check 和 just pmd
停止条件：公共契约、表结构、外部规则、P2 轨道、清结算对账、治理、敏感数据或工作树冲突越界即停止
```

确认本模板前，本文档只作为 Round 0 准入卡和 TDD 分析产物，不进入编码。
