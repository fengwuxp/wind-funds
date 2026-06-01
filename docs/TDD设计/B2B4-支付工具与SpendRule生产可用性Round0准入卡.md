# B2/B4 支付工具与 Spend Rule 生产可用性 Round 0 准入卡

## 1. 文档定位

本文档是支付工具与 Spend Rule 生产可用性的 Round 0 候选准入卡。它把 PRD、DSL、系分、TDD、OpenSpec、A0 准入裁决和当前代码观察收敛成一张可评审、可拆分、可转成 Execution Grant 的输入页。

本文档不授权修改生产代码、测试代码、DDL/H2 schema 或运行时配置。只有用户确认本页或确认经调整后的单一 MVP Execution Grant 后，才允许把本文档中的 Red 候选转成实际测试写入。

## 2. authorityBaseline

| 基线项 | 当前口径 |
| --- | --- |
| 设计和任务基线 | 最新已提交设计和任务对齐输入以确认时 Git HEAD 为准；当前未提交文档变更若要作为开工依据，必须先提交或在 Execution Grant 的 `authorityBaseline` 中显式列入。 |
| 产品入口 | `docs/产品设计/02-交易路由钱包账目与投影.md` 的支付工具能力控制、支付工具与 Spend Rule 生产可用性裁决、交易投影只读边界。 |
| DSL 入口 | `docs/DSL设计/README.md` 的支付工具和账户能力重定性后的 DSL CR 基线，以及 `docs/DSL设计/支付资金底座DSL承载层设计.md` 的 route、posting、projection 和 JSON 契约。 |
| 系分入口 | `docs/系分设计/02-交易路由钱包账目与投影系分设计.md` 的钱包账户设计、支付工具与 Spend Rule 生产可用性差距、application facade 分层和服务边界。 |
| TDD 入口 | `docs/TDD设计/支付资金底座测试驱动设计.md` 的 TDD-WALLET-018、TDD-WALLET-019、12.2 Round 0 Red 集合。 |
| OpenSpec 入口 | `openspec/specs/payment-funds-foundation/spec.md` 的支付工具交易入口、支付工具与 Spend Rule 生产可用性、支出主体资金责任解析关系术语；`openspec/changes/tdd-baseline-reset/tasks.md` 的 2026-06-01 生产可用性 CR。 |
| 现有代码证据 | `PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests`、`PaymentInstrumentRouteDslContractTests` 已作为局部基线；只证明资源服务、现有资金责任关系和 DSL 契约，不证明生产交易入口可用。 |

## 3. grantCandidate

| 字段 | 候选取值 |
| --- | --- |
| `mvpScenario` | 共享卡或 VCC 授权前准入：业务提交支付工具引用、使用主体、预算组或 Spend Rule 上下文和金额币种，系统完成工具能力准入、绑定快照、资金责任解析、规则决策、账户能力校验；批准后委派账户主体型授权内核，拒绝时无 route、posting、LedgerEntry。 |
| `abilityBatch` | 优先拆成 B2 Round 0 和 B4 Round 0。B2 处理支付工具能力准入、绑定快照、资金责任目标主体；B4 处理 `authorizeByInstrument` 或等价授权准入组合；B5/B6/B8 只在预算预留释放、交易投影和治理重放被明确授权时进入。 |
| `businessQuestion` | 企业管理员、运营和财务能否解释一笔共享卡或 VCC 授权为什么通过或拒绝，且能证明支付工具、预算组和 Spend Rule 只提供准入、规则和审计证据，不成为资金主体。 |
| `moneyFact` | 批准时资金影响仍落在资金账户、信用账户或平台角色解析后的平台资金账户；预算组和 Spend Rule 只写控制活动、规则决策、预留释放证据和只读投影。 |
| `productNotDone` | 不声明完整 VCC 发卡、完整支付工具交易入口、完整 Spend Rule 引擎、预算并发控制生产实现、清结算对账、卡组织/处理商/PCI/ACH/SWIFT/FX 规则确认或 P2 生产资金流完成。 |
| `firstRedSet` | `R0-PI-001`、`R0-FR-001` 优先；若选择 B4 再补 `R0-AUTH-001`；若选择 B5/B6/B8 再补 `R0-SR-001`、`R0-SR-002`、`R0-PI-002`。 |
| `currentEvidence` | 三个既有测试命令通过证据只能作为回归资产和局部代码基线；Round 0 新 Red 必须证明当前缺口或确认已有实现已覆盖，不能直接把既有通过测试升级为生产可用。 |

## 4. 场景裁剪

| 场景 | 本卡允许进入 Round 0 的内容 | 本卡不允许声明 |
| --- | --- | --- |
| VCC 或共享卡授权 | 支付工具准入、绑定快照、资金责任解析、Spend Rule 决策、账户能力校验、拒绝无副作用和委派账户主体型授权内核。 | 完整发卡产品、卡组织授权协议、PAN/CVC/PCI、卡账单全链路、processor 账户状态。 |
| 预付卡外部充值 | 仅作为业务事实映射评审输入，验证不能把 prepaid virtual card 当资金账户或账本主体。 | 在本卡中实现充值、清算、外部回单或预付资金账户创建。 |
| VA 收款 | 仅验证 VA 是收款识别工具和外部引用，不是账本主体。 | 在本卡中实现银行流水匹配、到账确认、对账或入金直接交易生产链路。 |
| 内部余额钱包支付 | 验证内部入口先解析为 `SubjectRef` 或资金责任决策，不强制包装为支付工具。 | 新增内部钱包支付工具类型或绕过资金账户能力校验。 |
| 全球账户付款、ACH 或银行转账 | 只作为 P2 业务能力包和外部轨道边界输入。 | 实现 SWIFT、local rail、ACH/Nacha、银行协议、FX quote 或外部非终态处理。 |

## 5. writeScopeCandidate

| 范围 | 候选授权 |
| --- | --- |
| B2 测试资产 | `PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests`、`PaymentInstrumentRouteDslContractTests`、钱包 application facade 边界测试或等价新测试类。 |
| B4 测试资产 | 授权准入组合测试、`FundsAuthorizationTransactionFlowTests` 的边界回归、授权 application facade 边界测试。 |
| 生产实现 | 只有 Red 证明真实缺口后，才允许在 `wallet-face`、`wallet-impl`、必要的 `transaction-face` application facade 契约或 `transaction-impl` 授权准入适配层做最小实现；具体模块必须由 Execution Grant 指定。 |
| 公共契约 | 默认不允许破坏既有 face/core 请求字段；如必须新增 `PaymentInstrumentCapabilityApplicationService`、`AuthorizationAdmissionApplicationService`、Request/DTO 或动作能力字段，必须在 Execution Grant 明确命名、依赖方向和兼容策略。 |
| DDL/H2 schema | 默认不允许修改；如 Spend Rule 规则定义、决策日志、控制活动或预算控制投影需要表结构，必须单独扩权到 B5/B6/B8。 |

## 6. noWriteScope

| 禁止范围 | 说明 |
| --- | --- |
| 交易 canonical 请求替换 | 不把 `FundsAuthorizationTransactionAuthorizeRequest`、直接交易请求或余额控制请求整体改成支付工具引用。 |
| 统一支付工具交易服务 | 不新增统一 `InstrumentTransactionService` 族覆盖直接交易、授权交易和余额控制。 |
| 预算组账务主体化 | 不把预算组或 Spend Rule 写成 route leg、posting、LedgerEntry、账本余额投影主体。 |
| P2 业务轨道实现 | 不实现卡组织、ACH/Nacha、SWIFT/local rail、PSP、银行协议、FX 执行或完整外部回单。 |
| 清结算、对账和治理 | 不新增清分、清算、结算、出款、对账差错、归档、Manifest、checkpoint、watermark 或正式重投影 apply。 |
| 敏感数据 | 不引入完整 PAN、CVV、token secret、银行账户敏感号、证件原文或生产通道配置。 |

## 7. redCandidateSet

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `R0-PI-001` | 支付工具是否真正具备当前动作能力。 | 工具能力只能作为准入快照，不能替代账户能力、余额、额度、预算或账本周期。 | 非 ACTIVE、方向不匹配、缺动作能力、过期、错币种、敏感原文或绑定版本失效时返回可解释失败或授权拒绝。 | 不得生成 route、posting、LedgerEntry；不得返回完整敏感凭证。 | 状态、方向、能力、币种、有效期、绑定版本、脱敏字段、失败原因、无账务副作用。 | `PaymentInstrumentServiceImplTests`、新 application facade 测试。 | `just test-one PaymentInstrumentServiceImplTests tests`、`just test-boundary`。 | 需要新增动作能力枚举、DTO、表字段但未授权。 |
| `R0-FR-001` | 支付工具、使用主体、预算组或 Spend Rule 能否解析到唯一最终责任主体。 | 最终责任主体只能是资金账户、信用账户或平台角色解析后的平台资金账户。 | 缺失、不唯一、错币种、优先级冲突或预算组/Spend Rule 被当最终主体时失败。 | 不得随机选路，不得让预算组或 Spend Rule 入账。 | 决策主体类型、主体 ID、币种、优先级、规则版本、选择原因、失败无 route/posting/entry。 | `SpendSubjectFundingRelationServiceImplTests`、`PaymentInstrumentRouteDslContractTests`。 | `just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-boundary`。 | 目标态要求信用账户或平台角色，但字段仍只允许 `fundingAccountId` 且未授权迁移。 |
| `R0-AUTH-001` | 授权支付工具入口是否只作为 application facade 委派账户主体型内核。 | 授权内核仍以已解析账户主体为 canonical 入参；拒绝无资金事实。 | `authorizeByInstrument` 或等价入口完成工具准入、绑定快照、Spend Rule、资金责任和账户能力后构造 canonical 授权请求。 | 不得直接替换 `FundsAuthorizationTransactionAuthorizeRequest.accountId`；不得让工具、预算组或 Spend Rule 成为 route leg。 | 准入步骤、委派请求、拒绝事实、route snapshot、幂等摘要、无副作用、敏感上下文阻断。 | 授权 application facade 测试、`FundsAuthorizationTransactionFlowTests` 回归。 | `just test-transaction`、`just test-boundary`。 | 需要改变授权内核公共契约或状态机但未授权。 |
| `R0-SR-001` | Spend Rule 拒绝是否能留下可审计决策而无账务副作用。 | 规则拒绝只能生成决策证据或拒绝事实，不生成资金事实。 | MCC、商户、时间窗、频控、限额或规则版本拒绝时记录规则决策和拒绝原因。 | 不得生成 route、posting、LedgerEntry；不得把规则通过等同于资金可用。 | 规则版本、命中条件、拒绝原因、使用主体、工具快照、无账务副作用。 | 新 Spend Rule 控制测试；未授权前可为 contract-only。 | 由 Execution Grant 指定；默认不写测试。 | 缺规则模型或表结构授权。 |
| `R0-SR-002` | 预算预留、释放和调整是否只更新控制活动和只读视图。 | 预算控制不等于账本余额，不得生成资金交易记录。 | 授权预留、撤销、过期、部分完成或退款释放写控制活动和预算控制投影。 | 不得写 LedgerEntry、账本余额桶或资金交易明细；不得无幂等并发更新预算可用。 | 控制活动类型、幂等键、前后预留量、规则版本、并发保护、投影只读。 | 新预算控制测试；未授权前可为 contract-only。 | 由 Execution Grant 指定；默认不写测试。 | 需要新增 Spend Control Activity 或预算投影表。 |
| `R0-PI-002` | 换绑后逆向交易是否仍按原快照解释。 | 退款、撤销、退费、拒付和重放优先沿原 route snapshot。 | 原工具快照、原绑定版本和原 route snapshot 可追溯；缺快照失败或人工处理。 | 不得按当前绑定、当前默认资金责任或当前能力重新选路。 | 原快照引用、绑定版本、原责任主体、金额闭合、失败处理。 | Route replay 或授权/交易逆向测试。 | `just test-transaction`、`just test-boundary`。 | 需要扩展 route replay 公共契约但未授权。 |

## 8. suggestedGrantSlices

| 切片 | 优先级 | 目标 | 首批 Red | 允许写入建议 | 不适合混入 |
| --- | --- | --- | --- | --- | --- |
| B2-PI-CAP | 1 | 支付工具能力准入 application facade。 | `R0-PI-001`。 | wallet-face/impl 的 facade 契约和测试；必要的 DTO。 | 授权状态机、Spend Rule 表、交易投影。 |
| B2-FR | 2 | 资金责任目标主体解析。 | `R0-FR-001`。 | 资金责任关系契约和测试；是否迁移目标主体字段由授权决定。 | 直接交易、清结算、P2 轨道。 |
| B4-AUTH-PI | 3 | 授权支付工具 application facade。 | `R0-AUTH-001`。 | 授权准入 facade、委派适配和边界测试。 | 替换授权内核请求、完整 VCC 发卡。 |
| B5-SR-CONTROL | 4 | Spend Rule 决策日志和预算预留释放。 | `R0-SR-001`、`R0-SR-002`。 | 规则定义、控制活动、预算控制投影，需单独 DDL/H2 授权。 | A1、B2 基础能力、P2 轨道。 |
| B6/B8-PI-VIEW | 5 | 支付工具流水、预算控制视图、规则命中时间线和重放。 | `R0-PI-002`。 | 只读投影、重放范围和差异报告，需单独授权。 | 事实反写、正式治理 apply。 |

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
禁止写入：交易 canonical 请求替换、统一 InstrumentTransactionService、预算组账务主体、清结算对账、治理 apply、P2 轨道协议、敏感原文
首批 Red：按所选切片选择 R0-PI-001、R0-FR-001、R0-AUTH-001、R0-SR-001、R0-SR-002 或 R0-PI-002
验证命令：just test-one PaymentInstrumentServiceImplTests tests；just test-one SpendSubjectFundingRelationServiceImplTests tests；just test-one PaymentInstrumentRouteDslContractTests tests；必要时 just test-transaction / just test-boundary；提交前 git diff --check 和 just pmd
停止条件：公共契约、表结构、外部规则、P2 轨道、清结算对账、治理、敏感数据或工作树冲突越界即停止
```

确认本模板前，本文档只作为 Round 0 准入卡和 TDD 分析产物，不进入编码。
