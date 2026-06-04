# P2 业务能力包 Round 0 准入卡

## 1. 文档定位

本文档收敛 VCC、全球账户、收单等 P2 业务专项进入资金底座前的 Round 0 准入规则。P2 业务能力是专项能力包，不属于默认 P0/P1 编码开工范围；只有用户确认单一 Execution Grant 后，才允许进入 Red、Green、Review、Verify 和自动提交闭环。

本卡只把已经归一化、脱敏、确认过的业务资金动作映射回统一的钱包、交易、账本、清结算、对账和归档能力。它不建设银行核心、全球账户开户、外部清算网络、FX 执行、跨境合规管理、收单通道、卡组织或外部协议栈。

当前 VCC 预付资金和授权后生命周期候选已在 [B2B4-支付工具与SpendRule生产可用性Round0准入卡.md](B2B4-支付工具与SpendRule生产可用性Round0准入卡.md) 中形成；本文档承接非支付工具轴的全球账户和收单候选包。本文档只记录 Execution Grant 候选，不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

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

| 候选切片 | 优先级 | 首批 Red | 本轮允许讨论的上限 | 不混入范围 |
| --- | --- | --- | --- | --- |
| P2-GA-INBOUND | 1 | R0-GA-IN-001 | 全球账户入金 facade、contract-only DTO、外部引用脱敏、外部非终态和未匹配不入账 Red。 | 不混入出款、退汇、FX 执行、完整清结算、外部协议和运营后台。 |
| P2-GA-OUTBOUND | 2 | R0-GA-OUT-001 | 全球账户出款预校验、外部已受理在途、成功回执和退汇候选。 | 不混入入金匹配、FX 执行和完整对账。 |
| P2-GA-FX-FEE | 3 | R0-GA-FX-001 | FX quote 引用、费用分离、错币种阻断和专业确认字段候选。 | 不做 FX 执行，不做汇率平台，不承诺跨境合规完成。 |
| P2-ACQ-CAPTURE | 4 | R0-ACQ-CAP-001 | 收单 capture/settlement/refund 的外部状态归一候选。 | 不做通道接入、卡组织清算文件和商户结算全链路。 |
| P2-ACQ-DISPUTE | 5 | R0-ACQ-DSP-001 | 收单 dispute/chargeback 事件语义和证据引用候选。 | 不混普通退款，不做证据文件存储和卡组织争议系统。 |

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
| 银行协议、本地清算网络规则、SWIFT/代理行规则、外汇和跨境监管规则、数据跨境规则 | 待确认 | 全球账户入金、目标国家/地区、币种、客户类型、外部账户和银行流水匹配 | 2026-06-04，仅完成本地候选包字段完整性核验 | 待法务、合规、财务、银行、通道、持牌机构和数据安全负责人确认 | 未完成外部规则时效核验，不作为上线依据。 |

## 7. Round 0 验证计划

| 验证项 | 命令或方式 |
| --- | --- |
| 工作区状态 | `git status --short` |
| CAD 候选结构检查 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/P2-业务能力包Round0准入卡.md` |
| 外部规则字段完整性检查 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_external_rules.py --file docs/TDD设计/P2-业务能力包Round0准入卡.md` |
| Markdown diff 空白检查 | `git diff --check` 和 `git diff --cached --check` |
| 索引一致性 | 检索 `P2-GA-INBOUND-CAD-001`、`Execution Grant：P2-GA-INBOUND`、`R0-GA-IN-001A`、`R0-GA-IN-001B`、`contract-only`、`canonical-funds-backed`、`same-currency-only`、`fx-quote-backed`。 |

## 8. 自动停止条件

出现以下任一情况，本候选包不得继续推进到代码：

1. 用户未确认 `Execution Grant：P2-GA-INBOUND`。
2. 未选择 implementationDecision、accountResolutionDecision、fxDecision 或外部规则核验状态。
3. 写入范围需要 Java、测试、公共契约、DDL/H2 schema 或运行时配置，但 Grant 未授权。
4. 需要改 transaction canonical request、ledger 公共契约、reconciliation payout preflight 契约、core FX port 或 route replay 公共契约。
5. 需要全球账户开户、VA 分配、银行核心、SWIFT、本地清算网络、ACH/Nacha、FX 执行、出款、退汇、完整清结算、完整对账或运营后台。
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

也可以选择 `P2-GA-OUTBOUND`、`P2-GA-FX-FEE`、`P2-ACQ-CAPTURE` 或 `P2-ACQ-DISPUTE`，但这些切片需要先补各自 Round 0 扫描和 Grant 候选包。
