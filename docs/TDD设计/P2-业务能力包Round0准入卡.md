# P2 业务能力包 Round 0 准入卡

## 1. 文档定位

本文档收敛 VCC、全球账户、收单等 P2 业务专项进入资金底座前的 Round 0 准入规则。P2 业务能力是专项能力包，不属于默认 P0/P1 编码开工范围；只有用户确认单一 Execution Grant 后，才允许进入 Red、Green、Review、Verify 和自动提交闭环。当前全局任务优先级为账本账目 > 钱包 > 交易层 > 支付工具支持 > VCC/全球账户支持；收单能力仅做设计和边界复核，不做实现。

本卡只把已经归一化、脱敏、确认过的业务资金动作映射回统一的钱包、交易、账本、清结算、对账和归档能力。它不建设银行核心、全球账户开户、外部清算网络、FX 执行、跨境合规管理、收单通道、卡组织或外部协议栈。

当前 VCC 预付资金和授权后生命周期候选已在 [B2B4-支付工具与SpendRule生产可用性Round0准入卡.md](B2B4-支付工具与SpendRule生产可用性Round0准入卡.md) 中形成；本文档承接非支付工具轴的全球账户候选包，以及收单设计-only 边界包。本文档只记录 P2 后置支持候选和设计差距，不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

## 2. P2 通用不写入范围

| 禁止项 | 处理口径 |
| --- | --- |
| 平行资金内核 | 不为全球账户、收单、VCC 或任一外部资金产品新建独立账本、余额、路由、交易事实或清结算内核。 |
| 外部协议侵入核心 | SWIFT、本地清算网络、银行协议、卡组织、收单通道和 FX 执行协议不得直接进入 core、wallet、transaction 或 ledger。 |
| 外部账户作为账务主体 | VA、银行账户、Nostro、Vostro、IBAN、routing number、SWIFT BIC、processor account 和 channel account 只能作为外部引用，不得作为 ledger subject、route leg、posting subject、LedgerEntry subject 或余额投影主体。 |
| 外部非终态转成功 | accepted、message sent、processing、pending、in transit 等外部非终态不得增加 AVAILABLE，不得生成成功 route/posting/LedgerEntry/projection。 |
| 敏感信息 | 不写入完整银行账号、卡号、CVV、token secret、外部报文原文、生产配置、跨境证明文件或客户敏感资料。 |
| 合规结论 | 规则版本、适用法域、资质、客户资金归属、数据跨境和外汇限制均保持待确认，不把产品或工程文档写成可上线合规结论。 |

## 3. 候选切片总览

| 候选切片 | P2 局部顺位 | 首批 Red | 本轮允许讨论的上限 | 不混入范围 |
| --- | --- | --- | --- | --- |
| P2-GA-INBOUND | 1 | R0-GA-IN-001 | 全球账户入金 facade、contract-only DTO、外部引用脱敏、外部非终态和未匹配不入账 Red。 | 不混入出款、退汇、FX 执行、完整清结算、外部协议和运营后台。 |
| P2-GA-OUTBOUND | 2 | R0-GA-OUT-001 | 全球账户出款预校验、外部已受理在途、成功回执和退汇候选。 | 不混入入金匹配、FX 执行和完整对账。 |
| P2-GA-FX-FEE | 3 | R0-GA-FX-001 | FX quote 引用、费用分离、错币种阻断和专业确认字段候选。 | 不做 FX 执行，不做汇率平台，不承诺跨境合规完成。 |
| P2-ACQ-CAPTURE | design-only | R0-ACQ-CAP-001 | 收单 capture 归一、商户 CLEARING、待清算和 PCI/外部规则边界设计。 | 不做实现、不写 Red 测试、不确认 Execution Grant、不做通道接入、卡组织清算文件、清分清算结算全链路、出款、退款或拒付。 |
| P2-ACQ-DISPUTE | design-only | R0-ACQ-DSP-001 | 收单 dispute/chargeback 事件语义和证据引用设计。 | 不混普通退款，不做证据文件存储、卡组织争议系统或实现候选。 |

## 4. P2-GA-INBOUND Round 0 扫描（2026-06-04）

状态：ROUND0_READY_NOT_CODE_AUTHORIZED。

产品基线：全球账户、VA、银行账户、Nostro、Vostro、外部银行流水和外部受理状态只作为业务对象或外部引用。accepted、message sent、processing、pending、in transit 只能表达外部已受理或在途，不能表达资金已到账、可用余额已增加或内部账本事实已完成。银行流水或 VA 匹配、合规与会计准入、内部责任主体决策完成后，才允许进入资金底座内部资金动作。

代码基线：当前仓库已存在 `ExternalAccountRefSpec`、外部账户敏感值校验、FX 端口和出款前准入候选等局部基础。这些只能说明已有外部引用、敏感值阻断和 FX 引用的局部承载能力，不能证明全球账户入金 facade、VA/银行流水匹配、外部在途状态、入金幂等和资金入账链路已经形成。

目标差距：当前未形成 `GlobalAccountInboundApplicationService`、全球账户入金 Request/DTO、VA/银行流水匹配 facade、外部受理在途契约、入金幂等摘要、错币种处理、外部账户不作为账务主体 Red 和对应最小实现。

语义决策：入金事件必须先由全球账户产品或银行适配层归一化和脱敏。资金服务只接收外部引用、匹配结果、金额币种、外部规则核验状态、内部责任主体决策、确认引用和幂等键。缺少匹配、缺少确认、内部责任主体不唯一、外部状态非终态、错币种且无有效 FX quote、外部账户被当作账务主体或携带敏感原文时，必须失败、挂起、差错或人工处理，不得写资金事实。

实现决策需要在授权时二选一：

| 决策 | 说明 |
| --- | --- |
| contract-only | 只允许新增 facade Request/DTO 目标 Red、入参校验、拒绝路径和脱敏审计候选；不委派真实资金交易，不改 DDL/H2 schema。 |
| canonical-funds-backed | 允许在明确列出的资金事实入口中委派账户主体型直接交易、清算确认事实或等价资金事实；必须同步余额、route、posting、entry、projection、幂等、审计和失败无副作用断言。 |

账户解析决策默认 `funding-account-only`。若要支持 `targetSubjectType + targetSubjectId`、信用账户或平台账户角色，必须另起 B2-FR-TARGET 或在本 Grant 中显式扩展并承担公共契约、DDL/H2 schema 和回归范围。

FX 决策默认 `same-currency-only`。跨币种只允许在 `fx-quote-backed` 决策下引用外部 quote/approval snapshot；资金服务只存引用和资金影响，不执行 FX。

首批 Red：

| Red ID | 目标 must-fail |
| --- | --- |
| R0-GA-IN-001A | 外部状态为 accepted、message sent、processing、pending、in transit，或 VA/银行流水未匹配，或缺少银行入账确认时，系统仍增加 AVAILABLE、生成成功 route/posting/LedgerEntry/projection。 |
| R0-GA-IN-001B | VA、银行账户、Nostro、Vostro、外部银行流水或完整敏感账号被作为账务主体、日志原文或测试原文；错币种且无有效 FX quote 时仍静默换汇或入账。 |

Execution Grant 必须列明全球账户业务章节、业务 AC、DSL 不变量、系分服务边界、TDD/RED、实现决策、外部引用字段、外部规则状态、匹配确认引用、内部责任主体、幂等摘要、审计字段、目标测试资产、P0/P1 回归范围和 DDL/H2 schema 是否允许修改。

不得混入全球账户开户、VA 分配、银行核心、SWIFT、本地清算网络、ACH/Nacha、FX 执行、跨境合规证明、出款回执、退汇、完整清结算、完整对账、运营后台、敏感原文或生产配置。

## 5. P2-GA-INBOUND Grant 候选（2026-06-04）

| 字段 | 内容 |
| --- | --- |
| Task ID | P2-GA-INBOUND-CAD-001 |
| 阶段切片 | P2 全球账户 / Wave 1 入金匹配与外部受理在途 |
| 状态 | READY_TO_CONFIRM_NOT_CODE_AUTHORIZED |
| Owner | 产品架构专家负责全球账户入金、外部状态、跨境、FX、外部规则和 Not Done 语义；资深架构师负责工程边界、TDD、Review、Refactor、验证命令和代码落地。 |
| authorityBaseline | 用户确认时 Git HEAD，且至少包含 `1defe01 docs: 补齐 VCC 生命周期候选包` 与本次 P2-GA-INBOUND 候选包提交。 |
| MVP 场景 | 银行适配层或全球账户系统提交入金事件，携带 VA/外部账户引用、银行流水引用、外部状态、匹配结果、金额币种、内部责任主体候选、外部规则核验状态、幂等键和操作者。系统证明外部非终态只进入在途/待处理/差错，不增加 AVAILABLE；只有匹配、确认、同币种或有效 FX quote、内部责任主体唯一且无敏感原文的事件，才按授权决策进入资金动作。 |
| 业务验收映射 | 产品 `GA-AC-001`、`GA-AC-002`、`GA-RED-001`、`GA-RED-002`；DSL 外部 accepted 不等于 success、外部账户不做 ledger subject、错币种无 quote 阻断；系分 P2 能力包、外部引用、内部责任主体、入金在途和差错；TDD `TDD-P2-GA-001`、`TDD-P2-GA-RED-001`、`TDD-RAIL-008`、`TDD-FX-001`、`TDD-FX-002`。 |
| implementationDecision | 必须二选一：`contract-only` 或 `canonical-funds-backed`。默认建议从 `contract-only` 开始。 |
| accountResolutionDecision | 默认 `funding-account-only`；扩展 `targetSubjectType + targetSubjectId`、信用账户或平台账户角色必须显式授权。 |
| fxDecision | 默认 `same-currency-only`；跨币种必须显式选择 `fx-quote-backed`，且只引用外部 quote/approval，不做 FX 执行。 |
| 首批 Red | `R0-GA-IN-001A`：外部非终态、未匹配、未确认不得增加 AVAILABLE 或生成 route/posting/entry/projection。 |
| 次批 Red | `R0-GA-IN-001B`：外部账户/VA/Nostro/Vostro/完整敏感账号不得作为账务主体或原文落库；错币种无有效 FX quote 不得入账。 |
| 写入范围 | 首轮只允许 `tests/src/test/java/com/wind/funds/wallet/application/globalaccount/GlobalAccountInboundApplicationServiceTests.java` 或等价 P2-GA 入金 Red。Red 成立后，`contract-only` 只允许 wallet facade Request/DTO、返回 DTO、wallet-impl 最小拒绝/校验实现和脱敏审计候选；`canonical-funds-backed` 才允许显式授权的账户主体型资金事实委派与 P0/P1 回归。 |
| 写入文件 | 未确认 Execution Grant 前，本候选包只允许写文档和索引；确认后写入文件必须按 Grant 中列出的测试、facade、Request/DTO、实现或 schema 范围执行。 |
| 只读范围 | `docs/产品设计/07-全球账户收付款资金底座PRD.md`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec` spec/tasks、`wallet`、`transaction`、`ledger`、`reconciliation`、`core`、`tests/src/test/resources/jdbc-schema.sql`。 |
| 只读参考 | 全球账户 PRD、DSL、系分、TDD、OpenSpec、现有 wallet/transaction/ledger/reconciliation/core 代码和 H2 schema 只作为参考，不等于编码授权。 |
| 公共契约门禁 | 只允许非破坏性的 P2-GA 入金 facade、Request/DTO、返回 DTO 和脱敏审计 payload。不得修改 transaction canonical request、ledger 公共契约、reconciliation payout preflight 契约、core FX port 或 route replay 公共契约，除非 Grant 显式列出。 |
| Schema 门禁 | 未显式授权前不得修改 `jdbc-schema.sql`、DDL、Entity、Mapper 或迁移脚本。若需要入金事件事实、匹配表、差错表、幂等表、在途投影或对账表，必须先确认 DDL/H2 范围。 |
| 依赖方向 | `wallet-face` 不依赖 `*-impl`；`wallet-impl` 可依赖 wallet-face、transaction-face、ledger-face、reconciliation-face 和 core；transaction、ledger、reconciliation 不反向依赖 wallet-impl。 |
| 禁止事项 | 不写外部协议栈、全球账户开户、VA 分配、银行核心、SWIFT/本地清算网络、ACH/Nacha、FX 执行、出款、退汇、完整清结算、完整对账、运营后台、敏感原文、生产配置或合规结论。 |
| 外部规则门禁 | 只记录外部规则核验完整性状态，必须保留规则来源、版本或发布日期、适用法域或范围、核验日期、确认方和确认状态；未完成法务、合规、财务、银行、通道、持牌机构和数据安全确认前，不得声明生产 Done。 |
| 验证命令 | 首轮 `just test-one GlobalAccountInboundApplicationServiceTests tests`；contract-only 触碰 wallet facade 时追加相关 wallet 服务测试、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`；canonical-funds-backed 追加直接交易、账本、交易模块和业务 flow 回归。 |
| Git 策略 | auto_commit，前提是验证通过、只包含本候选包授权范围且工具权限允许。 |
| 停止条件 | 缺 implementationDecision、账户解析决策、FX 决策或外部规则状态；需要 DDL/H2 但未授权；需要修改 transaction/ledger/reconciliation/core 公共契约；需要外部协议、FX 执行、完整清结算、出款或退汇；出现敏感原文、依赖反转、公有方法超过 5 个参数或工作树冲突。 |
| 交接 | 回写 Harness tasks、OpenSpec project/spec、TDD 索引、验证矩阵和残余风险；说明是否仍为 READY_TO_CONFIRM_NOT_CODE_AUTHORIZED。 |

```text
Execution Grant：P2-GA-INBOUND

Task ID：P2-GA-INBOUND-CAD-001
Git 策略：auto_commit
实现决策：contract-only 或 canonical-funds-backed（二选一）
账户解析：默认 funding-account-only；扩展目标主体需显式授权
FX 决策：默认 same-currency-only；跨币种需 fx-quote-backed 且不执行 FX
首批 Red：R0-GA-IN-001A
次批 Red：R0-GA-IN-001B
撤销方式：用户说“暂停/停止/撤销 P2-GA-INBOUND”即停止自动推进
```

## 6. 外部规则核验检查点

| 规则来源 | 版本或发布日期 | 适用法域或适用范围 | 核验日期 | 确认方 | 确认状态 |
| --- | --- | --- | --- | --- | --- |
| 银行协议、本地清算网络规则、SWIFT/代理行规则、外汇和跨境监管规则、数据跨境规则、财务和会计费用归因口径 | 待确认 | 全球账户入金、出款、退汇、FX quote 引用、费用归因、FX P&L 归属、目标国家/地区、币种、客户类型、外部账户和银行流水/回单匹配 | 2026-06-04，仅完成本地候选包字段完整性核验 | 待法务、合规、财务、银行、通道、持牌机构、税务、会计和数据安全负责人确认 | 未完成外部规则时效核验和专业口径确认，不作为上线依据。 |
| PSP 或收单行协议、卡组织规则、PCI DSS、本地支付方式规则、商户结算和拒付规则、银行出款协议 | 待确认 | 收单 capture、商户清分、清算、结算、退款、拒付、支付方式、卡网络、商户类型和结算模式 | 2026-06-04，仅完成本地候选包字段完整性核验 | 待法务、合规、安全、财务、PSP、收单行、卡组织、银行和支付方式负责人确认 | 未完成外部规则时效核验、PCI 安全边界和专业口径确认，不作为上线依据。 |

## 7. Round 0 验证计划

| 验证项 | 命令或方式 |
| --- | --- |
| 工作区状态 | `git status --short` |
| CAD 候选结构检查 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/P2-业务能力包Round0准入卡.md` |
| 外部规则字段完整性检查 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_external_rules.py --file docs/TDD设计/P2-业务能力包Round0准入卡.md` |
| Markdown diff 空白检查 | `git diff --check` 和 `git diff --cached --check` |
| 索引一致性 | 检索本卡已补齐的候选 Task ID、`Execution Grant`、首批 Red、实现决策、FX 决策、费用决策、清分/结算决策、敏感数据决策、外部规则门禁和 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。 |

## 8. 自动停止条件

出现以下任一情况，本候选包不得继续推进到代码：

1. 用户未确认对应的单一 `Execution Grant`。
2. 未选择该切片必需的 implementationDecision、账户解析、preflight、transit、return、FX、费用、外部规则或专业确认决策。
3. 写入范围需要 Java、测试、公共契约、DDL/H2 schema 或运行时配置，但 Grant 未授权。
4. 需要改 transaction canonical request、ledger 公共契约、reconciliation payout preflight 契约、core FX port 或 route replay 公共契约。
5. 需要全球账户开户、VA 分配、银行核心、SWIFT、本地清算网络、ACH/Nacha、FX 执行、锁汇、外部 FX provider、完整清结算、完整对账或运营后台。
6. 出现外部账户作为账务主体、敏感原文写入、依赖反转、公有方法超过 5 个参数、生产配置或合规上线结论。
7. 工作树出现无法区分的用户改动或验证失败且无法在授权范围内修复。

## 9. 确认模板

后续若要进入 P2 业务专项编码，请只确认一个切片：

```text
Execution Grant：P2-GA-INBOUND
Task ID：P2-GA-INBOUND-CAD-001
实现决策：contract-only / canonical-funds-backed
账户解析：funding-account-only / targetSubjectType + targetSubjectId（需额外授权）
FX 决策：same-currency-only / fx-quote-backed
外部规则状态：仅本地字段完整性 / 已完成法务合规财务银行通道确认
首批 Red：R0-GA-IN-001A
Git 策略：auto_commit
```

也可以选择 `P2-GA-OUTBOUND` 或 `P2-GA-FX-FEE`，但必须使用各自独立的 Execution Grant 模板，并说明为什么越过账本账目、钱包和交易层优先队列。`P2-ACQ-CAPTURE` 和 `P2-ACQ-DISPUTE` 当前仅 design-only，不得作为编码 Grant 选择。

## 10. P2-GA-OUTBOUND Round 0 扫描（2026-06-04）

状态：ROUND0_READY_NOT_CODE_AUTHORIZED。

产品基线：全球账户出款从 `REQUESTED` 进入前置检查，只有权限、余额、合规资料、收款端点、外部规则、风险和审批门禁完整后，才允许进入 `PRECHECKED`。外部提交后的 `SUBMITTED`、`PROCESSING`、accepted、message sent 或处理中只表达外部已受理或在途，不等于付款成功；只有成功回单、到账证明或对账来源确认后才能进入 `PAID`。退汇必须作为外部 return 事实处理，关联原出金、费用、责任方和幂等，不得静默走普通 refund。

代码基线：当前仓库已有 `PayoutOrderService#checkPayoutPreflight`、`CheckPayoutPreflightRequest`、`PayoutPreflightResultDTO` 和 `PayoutPreflightServiceTests` 候选基线，已能证明出款前置检查结构化阻断、外部规则字段完整性、解释状态和只读无账务事实。该基线只覆盖 preflight，不证明全球账户出款 facade、外部提交、`AVAILABLE -> IN_TRANSIT`、成功回单、退汇、金额不一致、数据库闭环或使用者解释状态已经生产可用。

目标差距：当前未形成 `GlobalAccountOutboundApplicationService`、全球账户出款 Request/DTO、出款幂等摘要、外部提交受理契约、成功回单终态契约、退汇处理契约、出款在途解释视图、退汇费用和责任处理 Red，也未形成出款生命周期表结构或 H2 服务级闭环。

语义决策：出款请求必须先由全球账户产品或出款适配层完成业务归一、收款端点脱敏、用途和合规资料校验。资金服务只接收内部责任主体、出款金额币种、脱敏外部收款引用、preflight 结果、外部规则核验状态、幂等键、操作者、外部提交引用和回单/退汇引用。preflight 缺失、失败或未知时，不得提交外部出款，不得生成 `FUND_OUT`、`IN_TRANSIT`、route、posting 或 LedgerEntry。外部非终态不得展示为到账成功，不得关闭 `SETTLEMENT/IN_TRANSIT`。退汇必须链接原出款事实，不能和普通退款混同。

实现决策需要在授权时二选一：

| 决策 | 说明 |
| --- | --- |
| preflight-contract-only | 只允许新增出款 facade Request/DTO 目标 Red、preflight 结果消费、拒绝路径、解释状态和脱敏审计候选；不提交外部出款，不写资金事实，不改 DDL/H2 schema。 |
| canonical-transit-backed | 允许在明确列出的资金事实入口中表达出款在途、成功回单和退汇回补或差错；必须同步余额桶、route、posting、entry、projection、幂等、审计、解释状态和失败无副作用断言。 |

出款前置检查决策默认 `reuse-reconciliation-preflight-candidate`。若要修改 `PayoutOrderService`、`CheckPayoutPreflightRequest`、`PayoutPreflightResultDTO` 或新增出款单公共契约，必须在 Execution Grant 中显式列入公共契约范围和回归测试。

在途账务决策默认 `no-ledger-in-transit`，即 contract-only 阶段只保留出款单待确认或解释状态。若选择 `ledger-in-transit-backed`，必须证明 `AVAILABLE -> IN_TRANSIT`、成功关闭、失败或退汇只回退一次、重复回单幂等和使用者解释状态不误导。

退汇决策默认 `return-as-difference-only`。若选择 `return-funds-backed`，必须证明退汇关联原出款、费用和责任方可解释、不是普通 refund、重复退汇不重复回补、失败不产生半截事实。

FX 决策默认 `same-currency-only`。跨币种出款只允许在 `fx-quote-backed` 决策下引用外部 quote/approval snapshot；资金服务只存引用和资金影响，不执行 FX。

首批 Red：

| Red ID | 目标 must-fail |
| --- | --- |
| R0-GA-OUT-001A | preflight 缺失、失败或未知时，系统仍提交外部出款、生成 `FUND_OUT`、`IN_TRANSIT`、route、posting、LedgerEntry 或成功状态。 |
| R0-GA-OUT-001B | 外部状态为 submitted、accepted、message sent、processing 或 pending 时，系统仍展示为 `PAID`、关闭 `SETTLEMENT/IN_TRANSIT`、释放为到账成功或允许用户/财务确认完成。 |
| R0-GA-OUT-001C | 银行退汇被当作普通 refund，缺原出款引用、费用、责任方、幂等摘要或重复退汇防护时仍回补或关闭差错。 |

Execution Grant 必须列明全球账户出款业务章节、`GA-AC-003` 至 `GA-AC-005`、DSL/TDD 不变量、实现决策、preflight 公共契约是否允许变更、在途账务决策、退汇决策、FX 决策、外部规则核验状态、目标测试资产、P0/P1 清结算与交易回归范围和 DDL/H2 schema 是否允许修改。

不得混入全球账户入金匹配、开户、VA 分配、银行核心、SWIFT、本地清算网络、ACH/Nacha、FX 执行、完整清结算、完整对账、运营后台、敏感原文、生产配置或合规结论。

## 11. P2-GA-OUTBOUND Grant 候选（2026-06-04）

| 字段 | 内容 |
| --- | --- |
| Task ID | P2-GA-OUTBOUND-CAD-001 |
| 阶段切片 | P2 全球账户 / Wave 2 出款在途、成功回单和退汇边界 |
| 状态 | READY_TO_CONFIRM_NOT_CODE_AUTHORIZED |
| Owner | 产品架构专家负责全球账户出款、退汇、外部状态、跨境、FX、外部规则和 Not Done 语义；资深架构师负责工程边界、TDD、Review、Refactor、验证命令和代码落地。 |
| authorityBaseline | 用户确认时 Git HEAD，且至少包含 `62aae0b docs: 补齐全球账户入金候选包` 与本次 P2-GA-OUTBOUND 候选包提交。 |
| MVP 场景 | 客户或业务系统提交全球账户出款，携带内部责任主体、收款端点引用、金额币种、用途、合规资料状态、外部规则状态、preflight 结果、幂等键和操作者。系统证明 preflight 缺失/失败/未知不得提交或写资金事实；外部非终态只进入在途或待确认；成功回单才 `PAID`；退汇必须关联原出金、费用、责任和幂等。 |
| 业务验收映射 | 产品 `GA-AC-003`、`GA-AC-004`、`GA-AC-005`；DSL 外部提交不等于 success、退汇不等于 refund、外部账户不做 ledger subject、错币种无 quote 阻断；系分 P2 能力包、出款前准入、外部引用、在途、退汇和差错；TDD `TDD-P2-GA-002`、`TDD-RAIL-008`、`TDD-RAIL-009`、`TDD-SETTLE-004`、`TDD-SETTLE-005`、`TDD-FX-001`、`TDD-FX-002`。 |
| implementationDecision | 必须二选一：`preflight-contract-only` 或 `canonical-transit-backed`。默认建议从 `preflight-contract-only` 开始。 |
| preflightDecision | 默认 `reuse-reconciliation-preflight-candidate`；改动 `PayoutOrderService`、preflight Request/DTO 或出款单公共契约必须显式授权。 |
| transitDecision | 默认 `no-ledger-in-transit`；选择 `ledger-in-transit-backed` 必须同步余额桶、账务、投影、幂等和解释状态断言。 |
| returnDecision | 默认 `return-as-difference-only`；选择 `return-funds-backed` 必须证明退汇不是普通 refund，且关联原出款、费用、责任和幂等。 |
| fxDecision | 默认 `same-currency-only`；跨币种必须显式选择 `fx-quote-backed`，且只引用外部 quote/approval，不做 FX 执行。 |
| 首批 Red | `R0-GA-OUT-001A`：preflight 缺失、失败或未知不得提交外部出款或生成资金事实。 |
| 次批 Red | `R0-GA-OUT-001B`：外部非终态不得展示为 `PAID` 或关闭 `SETTLEMENT/IN_TRANSIT`；`R0-GA-OUT-001C`：退汇不得当普通 refund。 |
| 写入范围 | 首轮只允许 `tests/src/test/java/com/wind/funds/wallet/application/globalaccount/GlobalAccountOutboundApplicationServiceTests.java` 或等价 P2-GA 出款 Red。Red 成立后，`preflight-contract-only` 只允许 wallet facade Request/DTO、返回 DTO、preflight 结果消费、拒绝路径和解释状态候选；`canonical-transit-backed` 才允许显式授权的出款在途、成功回单、退汇和 P0/P1 回归。 |
| 写入文件 | 未确认 Execution Grant 前，本候选包只允许写文档和索引；确认后写入文件必须按 Grant 中列出的测试、facade、Request/DTO、实现、preflight 契约或 schema 范围执行。 |
| 只读范围 | `docs/产品设计/07-全球账户收付款资金底座PRD.md`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec` spec/tasks、`wallet`、`transaction`、`ledger`、`reconciliation`、`core`、`tests/src/test/resources/jdbc-schema.sql`。 |
| 只读参考 | 全球账户 PRD、DSL、系分、TDD、OpenSpec、现有 `PayoutOrderService` / `PayoutPreflightServiceTests`、wallet/transaction/ledger/reconciliation/core 代码和 H2 schema 只作为参考，不等于编码授权。 |
| 公共契约门禁 | 只允许非破坏性的 P2-GA 出款 facade、Request/DTO、返回 DTO、preflight 消费和脱敏审计 payload。不得修改 transaction canonical request、ledger 公共契约、reconciliation payout preflight 契约、core FX port 或 route replay 公共契约，除非 Grant 显式列出。 |
| Schema 门禁 | 未显式授权前不得修改 `jdbc-schema.sql`、DDL、Entity、Mapper 或迁移脚本。若需要出款单、回单、退汇、差错、幂等、在途投影或解释视图表，必须先确认 DDL/H2 范围。 |
| 禁止事项 | 不写外部协议栈、银行核心、SWIFT/本地清算网络、ACH/Nacha、FX 执行、入金匹配、全球账户开户、完整清结算、完整对账、运营后台、敏感原文、生产配置或合规结论。 |
| 外部规则门禁 | 只记录外部规则核验完整性状态，必须保留规则来源、版本或发布日期、适用法域或范围、核验日期、确认方和确认状态；未完成法务、合规、财务、银行、通道、持牌机构和数据安全确认前，不得声明生产 Done。 |
| 验证命令 | 首轮 `just test-one GlobalAccountOutboundApplicationServiceTests tests`；触碰 preflight 候选时追加 `just test-one PayoutPreflightServiceTests tests`；contract-only 触碰 wallet facade 时追加 `just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`；canonical-transit-backed 追加清结算、账本、交易模块和业务 flow 回归。 |
| Git 策略 | auto_commit，前提是验证通过、只包含本候选包授权范围且工具权限允许。 |
| 停止条件 | 缺 implementationDecision、preflightDecision、transitDecision、returnDecision、FX 决策或外部规则状态；需要 DDL/H2 但未授权；需要修改 transaction/ledger/reconciliation/core 公共契约；需要外部协议、FX 执行、完整清结算、完整对账或出款生产通道；出现敏感原文、依赖反转、公有方法超过 5 个参数或工作树冲突。 |
| 交接 | 回写 Harness tasks、OpenSpec project/spec、TDD 索引、验证矩阵和残余风险；说明是否仍为 READY_TO_CONFIRM_NOT_CODE_AUTHORIZED。 |

```text
Execution Grant：P2-GA-OUTBOUND

Task ID：P2-GA-OUTBOUND-CAD-001
Git 策略：auto_commit
实现决策：preflight-contract-only 或 canonical-transit-backed（二选一）
preflight 决策：默认 reuse-reconciliation-preflight-candidate；改公共契约需显式授权
在途决策：默认 no-ledger-in-transit；账本在途需 ledger-in-transit-backed
退汇决策：默认 return-as-difference-only；资金回补需 return-funds-backed
FX 决策：默认 same-currency-only；跨币种需 fx-quote-backed 且不执行 FX
首批 Red：R0-GA-OUT-001A
次批 Red：R0-GA-OUT-001B / R0-GA-OUT-001C
撤销方式：用户说“暂停/停止/撤销 P2-GA-OUTBOUND”即停止自动推进
```

## 12. P2-GA-FX-FEE Round 0 扫描（2026-06-04）

状态：ROUND0_READY_NOT_CODE_AUTHORIZED。

产品基线：全球账户 FX 与费用能力只承接外部已决策的 quote、approval snapshot、费用组件、成本收入归因和使用者解释，不建设 FX 执行系统、汇率平台、跨境合规管理或财务总账。无有效 quote、quote 过期、币种不匹配、费用归因不完整或专业确认缺失时，必须阻断、挂起、差错或人工处理，不得静默换汇、不得把本金、平台费、银行费、中间行费、FX spread 或 FX P&L 净额混成单一金额。

代码基线：当前仓库已有 `FxService`、`FxRequest`、`FxResult`、`FxRate`、`DefaultFxServiceImpl`、`FundsInstruction.originalAmount`、`FundsInstruction.exchangeRate`、直接交易手续费和退费相关 Request/flow 局部能力。这些只能说明资金底座已有 FX 端口、原币金额/汇率字段和直接交易费用能力，不能证明全球账户 FX quote approval snapshot、费用归因矩阵、错币种阻断、使用者费用解释或专业确认门禁已经形成；`FxService` 也不能反推为全球账户 FX 执行能力。

目标差距：当前未形成 `GlobalAccountFxFeeApplicationService`、全球账户 FX quote / fee attribution Request/DTO、quote approval snapshot、费用组件归因模型、FX P&L 专业确认字段、错币种无 quote 红线、费用净额混淆红线、禁止调用 FX 执行红线和对应最小测试资产。

语义决策：资金服务只接收全球账户产品、FX/treasury、银行适配层或财务系统已经归一和脱敏的 quoteRef、fromCurrency、toCurrency、rate、validUntil、provider、executionRef、approvalRef、originalAmount、targetAmount、平台费、银行费、中间行费、费用承担方、成本收入归因、外部规则核验状态、专业确认状态、幂等键、操作者和审计引用。quote 缺失或过期、币种不匹配但无批准 quote、费用承担方不明确、费用与本金净额混同、FX P&L 归属缺专业确认、请求要求资金服务执行 FX 或调用外部汇率/交易系统时，必须失败、挂起或转人工，不得写成功资金事实。

实现决策需要在授权时二选一：

| 决策 | 说明 |
| --- | --- |
| contract-only | 只允许新增 FX quote / fee attribution facade Request/DTO 目标 Red、入参校验、拒绝路径、脱敏审计和解释字段候选；不写资金事实，不改 DDL/H2 schema。 |
| attribution-backed | 允许把 FX quote、费用组件、成本收入归因和专业确认状态作为不可变资金影响快照或显式资金事实引用记录；必须同步余额、posting、entry、projection、幂等、审计、解释状态和失败无副作用断言。 |

FX 执行决策默认 `no-fx-execution`。资金服务不得调用外部 FX 交易、quote provider 或 treasury 执行能力；若后续只做 quote 有效性查询或内部引用校验，必须显式选择 `quote-validation-only`，并证明不产生 FX 成交、锁汇或外部交易副作用。

费用决策默认 `fee-attribution-only`。若要把平台费、银行费或中间行费落成账务资金事实，必须显式选择 `fee-ledger-backed`，并列明直接交易费用/退费能力复用边界、金额闭合、费用承担方、收入成本科目、退款/退汇时费用处理和回归测试。

外部规则和专业确认默认 `external-rules-incomplete-blocking`。缺法务、合规、财务、税务、会计、银行、通道、持牌机构或数据安全确认时，只能阻断、挂起、降级为 contract-only 或标记 Not Done，不得声明生产资金流 Done。

首批 Red：

| Red ID | 目标 must-fail |
| --- | --- |
| R0-GA-FX-001A | 实际币种与预期币种不一致且无有效已批准 quote 时，系统仍增加 AVAILABLE、生成成功 route/posting/LedgerEntry/projection 或静默按预期币种入账。 |
| R0-GA-FX-001B | 本金、平台费、银行费、中间行费、FX spread 或 FX P&L 被净额混成单一金额，导致用户、财务、运营或对账无法解释金额差异。 |
| R0-GA-FX-001C | 资金服务调用 FX 执行、汇率提供方或外部交易系统，或在外部规则、财务/税务/会计/合规确认缺失时仍自动处理为生产 Done。 |

Execution Grant 必须列明全球账户 FX/费用业务章节、`GA-R003`、`GA-R004`、`GA-RED-001`、`TDD-FX-001`、`TDD-FX-002`、`TDD-DIR-006`、`TDD-DIR-007`、实现决策、FX 执行决策、费用决策、外部规则和专业确认状态、目标测试资产、P0/P1 回归范围和 DDL/H2 schema 是否允许修改。

不得混入 FX 执行、锁汇、报价撮合、汇率平台、treasury 核心、银行核心、SWIFT、本地清算网络、ACH/Nacha、完整清结算、完整对账、运营后台、生产配置、敏感原文或合规上线结论。

## 13. P2-GA-FX-FEE Grant 候选（2026-06-04）

| 字段 | 内容 |
| --- | --- |
| Task ID | P2-GA-FX-FEE-CAD-001 |
| 阶段切片 | P2 全球账户 / Wave 3 FX quote 引用、费用分离和错币种阻断 |
| 状态 | READY_TO_CONFIRM_NOT_CODE_AUTHORIZED |
| Owner | 产品架构专家负责全球账户 FX、费用、使用者解释、外部规则、财务/税务/会计/合规待确认和 Not Done 语义；资深架构师负责工程边界、TDD、Review、Refactor、验证命令和代码落地。 |
| authorityBaseline | 用户确认时 Git HEAD，且至少包含 `e43795b docs: 补齐全球账户出款候选包` 与本次 P2-GA-FX-FEE 候选包提交。 |
| MVP 场景 | 全球账户入金、出款或退汇出现币种差异或费用差异时，业务系统提交外部已决策 quote snapshot、费用组件、承担方、成本收入归因、外部规则核验状态、专业确认状态、幂等键和操作者。系统证明无有效 quote 不静默换汇，费用不净额混淆，资金服务不执行 FX，专业确认缺失不声明生产 Done。 |
| 业务验收映射 | 产品 `GA-R003`、`GA-R004`、`GA-RED-001`、`GA-AC-004`；DSL 错币种无 quote 阻断、外部 quote 只做引用、费用组件不混入本金；系分 P2 能力包、外部规则核验、费用归因和使用者解释；TDD `TDD-FX-001`、`TDD-FX-002`、`TDD-P2-GA-RED-001`、`TDD-P2-GA-001`、`TDD-P2-GA-002`、`TDD-DIR-006`、`TDD-DIR-007`、`TDD-RED-030`。 |
| implementationDecision | 必须二选一：`contract-only` 或 `attribution-backed`。默认建议从 `contract-only` 开始。 |
| fxExecutionDecision | 默认 `no-fx-execution`；仅在显式授权 `quote-validation-only` 时允许做无成交副作用的 quote 引用校验。 |
| feeDecision | 默认 `fee-attribution-only`；选择 `fee-ledger-backed` 必须同步费用资金事实、退费、退汇费用、收入成本归因、余额和账务断言。 |
| externalRuleDecision | 默认 `external-rules-incomplete-blocking`；缺外部规则和专业确认时不得声明生产 Done。 |
| 首批 Red | `R0-GA-FX-001A`：错币种无有效 quote 不得入账或静默换汇。 |
| 次批 Red | `R0-GA-FX-001B`：费用、本金和 FX P&L 不得净额混淆；`R0-GA-FX-001C`：资金服务不得执行 FX 或绕过外部规则/专业确认。 |
| 写入范围 | 首轮只允许 `tests/src/test/java/com/wind/funds/wallet/application/globalaccount/GlobalAccountFxFeeApplicationServiceTests.java` 或等价 P2-GA FX/fee Red。Red 成立后，`contract-only` 只允许 wallet facade Request/DTO、返回 DTO、拒绝路径、脱敏审计和解释字段候选；`attribution-backed` 才允许显式授权的 FX/费用不可变快照、费用事实引用和 P0/P1 回归。 |
| 写入文件 | 未确认 Execution Grant 前，本候选包只允许写文档和索引；确认后写入文件必须按 Grant 中列出的测试、facade、Request/DTO、实现、费用事实或 schema 范围执行。 |
| 只读范围 | `docs/产品设计/07-全球账户收付款资金底座PRD.md`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec` spec/tasks、`wallet`、`transaction`、`ledger`、`reconciliation`、`core`、`tests/src/test/resources/jdbc-schema.sql`。 |
| 只读参考 | 全球账户 PRD、DSL、系分、TDD、OpenSpec、现有 `FxService` / `DefaultFxServiceImpl`、直接交易手续费/退费 flow、wallet/transaction/ledger/reconciliation/core 代码和 H2 schema 只作为参考，不等于编码授权或 FX 执行授权。 |
| 公共契约门禁 | 只允许非破坏性的 P2-GA FX/fee facade、Request/DTO、返回 DTO、脱敏审计 payload 和解释字段。不得修改 core FX port、transaction canonical request、ledger 公共契约、reconciliation 公共契约或 route replay 公共契约，除非 Grant 显式列出。 |
| Schema 门禁 | 未显式授权前不得修改 `jdbc-schema.sql`、DDL、Entity、Mapper 或迁移脚本。若需要 FX quote 快照表、费用组件表、费用归因表、FX P&L 确认表或解释视图表，必须先确认 DDL/H2 范围。 |
| 禁止事项 | 不写 FX 执行、锁汇、报价撮合、汇率平台、treasury 核心、外部 FX provider 集成、银行核心、SWIFT/本地清算网络、ACH/Nacha、完整清结算、完整对账、运营后台、敏感原文、生产配置或合规结论。 |
| 外部规则门禁 | 只记录外部规则和专业确认完整性状态，必须保留规则来源、版本或发布日期、适用法域或范围、核验日期、确认方和确认状态；未完成法务、合规、财务、税务、会计、银行、通道、持牌机构和数据安全确认前，不得声明生产 Done。 |
| 验证命令 | 首轮 `just test-one GlobalAccountFxFeeApplicationServiceTests tests`；触碰直接交易费用能力时追加 `just test-one FundsTransactionFeeFlowTests tests`；contract-only 触碰 wallet facade 时追加 `just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`；attribution-backed 追加直接交易、账本、交易模块和业务 flow 回归。 |
| Git 策略 | auto_commit，前提是验证通过、只包含本候选包授权范围且工具权限允许。 |
| 停止条件 | 缺 implementationDecision、fxExecutionDecision、feeDecision、externalRuleDecision 或专业确认状态；需要 DDL/H2 但未授权；需要修改 core/transaction/ledger/reconciliation 公共契约；需要 FX 执行、外部 FX provider、treasury、完整清结算、完整对账或生产通道；出现敏感原文、依赖反转、公有方法超过 5 个参数或工作树冲突。 |
| 交接 | 回写 Harness tasks、OpenSpec project/spec、TDD 索引、验证矩阵和残余风险；说明是否仍为 READY_TO_CONFIRM_NOT_CODE_AUTHORIZED。 |

```text
Execution Grant：P2-GA-FX-FEE

Task ID：P2-GA-FX-FEE-CAD-001
Git 策略：auto_commit
实现决策：contract-only 或 attribution-backed（二选一）
FX 执行决策：默认 no-fx-execution；quote 校验需 quote-validation-only
费用决策：默认 fee-attribution-only；费用入账需 fee-ledger-backed
外部规则决策：默认 external-rules-incomplete-blocking
首批 Red：R0-GA-FX-001A
次批 Red：R0-GA-FX-001B / R0-GA-FX-001C
撤销方式：用户说“暂停/停止/撤销 P2-GA-FX-FEE”即停止自动推进
```

## 14. P2-ACQ-CAPTURE Round 0 扫描（2026-06-04）

状态：DESIGN_ONLY_NOT_CODE_CANDIDATE。

产品基线：收单产品、支付网关或通道适配层负责 Merchant、Payment Order、Payment Attempt、Capture、支付方式、PSP/收单行和卡组织事件的协议归一、风险判断和敏感数据隔离。资金底座只接收已经归一、脱敏、具备外部终态或待处理口径的 capture 资金动作；capture 或支付成功只能增加商户 `CLEARING` 或待清算事实，不得直接增加 `AVAILABLE`、`SETTLEMENT`、可提现余额或 payout-ready 状态。清分、清算、结算、出款、退款和拒付必须保持分层，不得把 capture 成功、清分确认、结算锁定、银行出款回执和拒付追偿混成一个状态。

代码基线：当前仓库已有账户主体型直接交易、授权 capture/settlement/refund/chargeback 概念、`MerchantInfoSpec`、`MerchantInfoRequest`、`FundsInstructionContextKeys.MERCHANT_INFO`、敏感上下文阻断、`LedgerSubjectCode.CLEARING` / `SETTLEMENT`、`SettlementPolicySpec` 和出款前准入候选等局部能力。这些只能说明资金底座已有商户信息、清算账目、授权生命周期和敏感上下文基础，不证明收单 capture application facade、收单 capture Request/DTO、capture 幂等摘要、外部状态匹配、商户 CLEARING 入账、清分明细生命周期或 PCI 敏感数据边界已经形成。

目标差距：当前未形成 `AcquiringCaptureApplicationService`、收单 capture Request/DTO、`AcquiringCaptureClearingTests`、`AcquiringSensitiveDataBoundaryTests`、capture event lifecycle、外部 capture 引用幂等、商户资金账户解析、商户 CLEARING 账务断言、支付成功不等于可提现 Red、完整 PAN/CVC/token secret/3DS 原文阻断 Red，也未形成 clearable item / clearing batch / settlement order 的 H2 服务级闭环。

语义决策：capture 事件必须先由收单产品、网关或通道适配层归一和脱敏。资金服务只接收 `captureId`、`paymentAttemptId`、商户引用、内部商户资金账户决策、金额币种、外部 capture 引用、脱敏支付方式或工具引用、PSP/收单行/渠道引用、外部状态、规则核验状态、幂等键、请求摘要、操作者和审计引用。外部状态非终态、缺 capture confirmation、缺商户资金账户决策、金额币种不一致、商户不唯一、幂等同键不同摘要、携带完整 PAN/CVC/token secret/3DS 原文或 PSP 原始报文时，必须失败、挂起、差错或人工处理，不得写 route、posting、LedgerEntry、projection、清分候选或可提现结果。

若未来重新打开收单实现优先级，新的授权实现决策需要重新二选一：

| 决策 | 说明 |
| --- | --- |
| contract-only | 只允许新增收单 capture facade Request/DTO 目标 Red、入参校验、拒绝路径、脱敏审计和解释状态候选；不写资金事实，不改 DDL/H2 schema。 |
| canonical-clearing-backed | 允许把已确认 capture 映射为账户主体型资金事实，使商户 `CLEARING` 或待清算余额增加，并记录费用或成本引用；必须同步余额桶、route、posting、entry、projection、幂等、审计和失败无副作用断言，且不得释放到 `AVAILABLE` 或 `SETTLEMENT`。 |

清分批次决策默认 `no-clearing-batch-write`。若选择 `clearing-batch-backed`，必须另行列明 clearable item、clearing batch、pre-reconciliation、批次幂等、差错状态、DDL/H2 schema 和 B7 清结算回归范围。

结算释放决策默认 `no-settlement-release`。本候选不处理清算确认、结算锁定、出款提交、银行成功回执或到账成功；任何 `SETTLEMENT`、`IN_TRANSIT`、`PAID` 或 payout-ready 写入都必须另起清结算、结算或出款 Grant。

敏感数据决策默认 `masked-reference-only`。完整 PAN、CVC、token secret、3DS 原始数据、完整 PSP payload、生产密钥或持卡人敏感资料不得进入资金底座日志、上下文、投影、导出、测试夹具或异常消息。

首批 Red：

| Red ID | 目标 must-fail |
| --- | --- |
| R0-ACQ-CAP-001A | capture 或支付成功事件直接增加商户 `AVAILABLE`、`SETTLEMENT`、可提现余额或 payout-ready 状态，而不是只进入 `CLEARING` 或待清算事实。 |
| R0-ACQ-CAP-001B | 完整 PAN、CVC、token secret、3DS 原始数据、完整 PSP payload 或生产敏感配置进入日志、上下文、投影、导出、异常消息或测试夹具。 |
| R0-ACQ-CAP-001C | 外部状态非终态、缺 capture confirmation、缺商户资金账户决策、缺幂等键、商户不唯一或同一外部 capture 引用不同摘要时，系统仍写 route、posting、LedgerEntry、projection、清分候选或可提现结果。 |

若未来重新打开实现，新的 Execution Grant 必须列明收单业务章节、`ACQ-AC-001`、`ACQ-RED-001`、`ACQ-RED-002`、`ACQ-R001`、`ACQ-R005`、DSL/TDD 不变量、实现决策、清分批次决策、结算释放决策、敏感数据决策、外部规则核验状态、商户资金账户解析、外部引用字段、目标测试资产、P0/P1 回归范围和 DDL/H2 schema 是否允许修改。当前仅登记为设计差距，不允许确认。

不得混入商户开户/KYB、checkout 或支付页、PSP/收单行/卡组织协议接入、PAN/CVC/token vault/3DS 原始认证数据、风控评分、完整清分清算结算、出款、退款、拒付/chargeback、对账归档、运营后台、生产配置或合规上线结论。

## 15. P2-ACQ-CAPTURE 设计-only 保留项（2026-06-04）

| 字段 | 内容 |
| --- | --- |
| Design ID | P2-ACQ-CAPTURE-DESIGN-001 |
| 阶段切片 | P2 收单 / Wave 1 capture 归一、商户 CLEARING 和敏感数据边界 |
| 状态 | DESIGN_ONLY_NOT_CODE_CANDIDATE |
| Owner | 产品架构专家负责收单 capture、商户资金语义、外部状态、PSP/收单行/卡组织/PCI 外部规则和 Not Done 语义；资深架构师负责工程边界、TDD、Review、Refactor、验证命令和代码落地。 |
| authorityBaseline | 最新已提交文档和任务账本；本节只作为设计差距，不作为实现授权基线。 |
| MVP 场景 | 收单产品、支付网关或通道适配层提交已确认 capture 事件，携带 capture/payment attempt 引用、商户资金账户决策、金额币种、外部 capture 引用、脱敏支付方式引用、PSP/收单行/渠道引用、外部状态、外部规则核验状态、幂等键和操作者。系统证明 capture 成功只进入商户 `CLEARING` 或待清算，不释放 `AVAILABLE/SETTLEMENT`；重复通知幂等；缺确认、缺商户账户、非终态、同键不同摘要或敏感原文都无资金副作用。 |
| 业务验收映射 | 产品 `ACQ-AC-001`、`ACQ-RED-001`、`ACQ-RED-002`、`ACQ-R001`、`ACQ-R005`；DSL 收单 capture 只进入待清算、支付成功不等于可提现、敏感卡数据不进入资金底座；系分 P2 能力包、商户 CLEARING、外部引用、敏感边界和外部规则核验；TDD `TDD-P2-ACQ`、`TDD-RED-007`、`TDD-RED-018`、`TDD-RED-020`、`TDD-RED-023`、`TDD-RED-025`。 |
| implementationDecision | 当前不选择；收单只做设计。若未来重新打开，必须另起 Round 0 和新的单一 Execution Grant。 |
| clearingDecision | 设计默认 `no-clearing-batch-write`；不得写 clearable item、clearing batch、pre-reconciliation、DDL/H2 schema 或 B7 回归实现。 |
| settlementDecision | 设计默认 `no-settlement-release`；不得释放 `SETTLEMENT`、`IN_TRANSIT`、`PAID` 或 payout-ready。 |
| sensitiveDecision | 设计默认 `masked-reference-only`；完整 PAN/CVC/token secret/3DS 原文/PSP 原始报文不得落入资金底座。 |
| 首批 Red | `R0-ACQ-CAP-001A`：capture/payment success 不得直接增加 `AVAILABLE`、`SETTLEMENT`、可提现余额或 payout-ready。 |
| 次批 Red | `R0-ACQ-CAP-001B`：敏感卡和 PSP 原文不得入资金底座；`R0-ACQ-CAP-001C`：非终态、缺确认、缺商户账户、缺幂等或同引用不同摘要不得写资金事实。 |
| 写入范围 | 仅允许文档、OpenSpec、Harness tasks、设计索引和边界差距登记。不得写 `AcquiringCaptureClearingTests`、facade、Request/DTO、实现、资金事实或 schema。 |
| 写入文件 | 只允许文档和索引。 |
| 只读范围 | `docs/产品设计/08-收单业务资金底座PRD.md`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec` spec/tasks、`wallet`、`transaction`、`ledger`、`reconciliation`、`core`、`tests/src/test/resources/jdbc-schema.sql`。 |
| 只读参考 | 收单 PRD、DSL、系分、TDD、OpenSpec、现有 `FundsAuthorizationTransactionService`、`MerchantInfoSpec`、`LedgerSubjectCode.CLEARING/SETTLEMENT`、`SettlementPolicySpec`、敏感上下文测试、wallet/transaction/ledger/reconciliation/core 代码和 H2 schema 只作为参考，不等于编码授权。 |
| 公共契约门禁 | 当前不允许新增 P2-ACQ capture facade、Request/DTO、返回 DTO、脱敏审计 payload 或解释字段；不得修改 transaction canonical request、ledger 公共契约、reconciliation clearing/payout 契约、core ledger subject 或 route replay 公共契约。 |
| Schema 门禁 | 当前不得修改 `jdbc-schema.sql`、DDL、Entity、Mapper 或迁移脚本；capture event、merchant clearing item、clearable item、clearing batch、幂等、差错或投影表只保留为设计差距。 |
| 禁止事项 | 不写商户开户/KYB、checkout、支付网关、PSP/收单行/卡组织协议栈、PAN/CVC/token vault/3DS 原始认证、风控评分、完整清分清算结算、出款、退款、拒付/chargeback、完整对账、运营后台、敏感原文、生产配置或合规结论。 |
| 外部规则门禁 | 只记录外部规则和 PCI 安全边界完整性状态，必须保留规则来源、版本或发布日期、适用法域或范围、核验日期、确认方和确认状态；未完成法务、合规、安全、财务、PSP、收单行、卡组织、银行和支付方式确认前，不得声明生产 Done。 |
| 验证命令 | 文档级验证：`git diff --check`、Harness plan 结构检查、外部规则字段完整性检查和索引一致性检索；不运行收单 Java 测试。 |
| Git 策略 | docs-only auto_commit，前提是只包含文档和索引。 |
| 停止条件 | 用户要求实现收单、需要写 Java/test/DDL/H2/schema/公共契约、需要 PSP/卡组织协议、PCI 原文、完整清分清算结算、出款、退款、拒付或生产通道时停止并重新确认优先级。 |
| 交接 | 回写 Harness tasks、OpenSpec project/spec、TDD 索引、验证矩阵和残余风险；说明当前为 `DESIGN_ONLY_NOT_CODE_CANDIDATE`。 |

```text
Design-only：P2-ACQ-CAPTURE

Design ID：P2-ACQ-CAPTURE-DESIGN-001
状态：DESIGN_ONLY_NOT_CODE_CANDIDATE
范围：收单 capture、商户 CLEARING、待清算、敏感数据和外部规则设计差距
禁止：不得写 Java、测试、DDL/H2 schema、公共契约、facade、Request/DTO 或实现
重新打开条件：用户明确要求重新打开收单实现优先级，并重新补 Round 0 与单一 Execution Grant
```
