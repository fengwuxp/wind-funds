# GSD-2 AUTH Chargeback 目标语义对齐任务卡

## 1. 文档定位

本文是 `GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001` 的 contract/design-only 任务卡，用于把授权交易中的 dispute / chargeback 目标语义、现有兼容入口、后续编码候选和停止条件一次收清。

本文不是新的 PRD，不替代产品设计、DSL 设计、系分设计或 TDD 设计；也不是编码授权、测试写入授权、DDL/H2 schema 授权、公共契约变更授权或 Git 授权。本文只允许低风险文档、索引、OpenSpec tasks 和 LWT Goal 状态同步。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001` |
| 原子任务 | 明确 dispute / chargeback 是争议案件过程，不是默认独立资金结果；有资金影响时进入争议退款，无资金影响时不生成资金事实。 |
| 所属阶段 | GSD-2 / AUTH semantic alignment / contract-design-only。 |
| Goal ID | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| Loop ID | `GSD2-LWT-PRODUCTION-CAPABILITY-LOOP-2026-06-18` |
| 当前状态 | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` |
| Git / code baseline | `a38776c5 feat: 接入出款准入对账门禁`。 |
| Owner | AI Native 流程编排负责状态、Loop 和停止条件；产品架构专家负责争议、退款、拒付、运营、财务和验收口径；资深架构师负责现有代码锚点、契约边界、Red 候选和验证命令；用户确认是否进入后续单一 Execution Grant。 |
| Wave 边界 | 本任务只做 contract/design-only 语义对齐、状态同步和下一 Grant 草案；不得写 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、枚举、状态机、运行时配置或 Git。 |
| 执行顺序 / 依赖关系 | 本任务在 LWT Goal 建立后执行，并与 wallet facade、B4 投影解释、ledger guard、B7 清算/结算 gate 消费和 B5 审计扩展互不重叠；后续任一编码切片必须重新确认单一 Execution Grant。 |
| 写入范围 | 本文、LWT Goal、GSD-2 工作流、P0/P1 LWT 推进计划、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md`、`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、transaction-face、transaction-impl、route replay、ledger posting、projection explain、tests、AGENTS.md 和历史 B4 授权后继准入卡。 |
| 只读参考 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计/支付资金底座测试驱动设计.md`、`docs/TDD设计/B4-授权后继能力Round0准入卡.md`、`transaction/transaction-face`、`transaction/transaction-impl`。 |
| 上下文账本 | 本文是 chargeback 目标语义任务账本；LWT Goal、GSD-2 工作流、P0/P1 LWT 推进计划、TDD README、docs README 和 OpenSpec tasks 是恢复入口。 |
| Git 策略 | `summary_only`。未获用户明确授权前，不执行 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |

## 2. 背景、目标和非目标

背景：当前 TDD 已明确 `dispute / chargeback` 是案件过程。用户胜诉、部分胜诉或业务决策需要退款时，资金路径应通过完成后退款承接；用户败诉或无需资金处理时，不生成 route、posting、LedgerEntry、余额变化或新的交易事实。与此同时，代码中仍保留 `FundsAuthorizationTransactionService#chargeback`、`FundsAuthorizationTransactionChargebackRequest`、`FundsTransactionEventType.CHARGEBACK`、route replay 的 `CHARGEBACK` 分支和 `declinedAmount` 累计字段，这些现有兼容入口容易被误读为新的目标态。

业务目标：让产品、运营、财务、风控、研发和测试统一理解争议过程、资金结果和账务影响之间的边界，避免后续编码把 chargeback 做成独立资金内核入口，或把授权拒绝、普通退款、争议裁决退款和无资金影响争议混在一起。

技术目标：在进入任何 Java、测试、DDL/H2 schema 或公共契约变更前，先固定契约裁决、兼容策略、Red 候选、验证命令和停止条件。

用户价值：使用者能判断一笔争议到底是普通退款、争议裁决退款、拒付扣回、追偿、败诉无资金影响，还是授权阶段拒绝；运营和财务能在查询、投影和审计里解释资金为什么变化或为什么不变化。

成功指标：

1. 文档能明确目标态主入口是 `settleRefund` 的争议退款字段，而不是强制新增或扩展独立 `chargeback` 目标 API。
2. 文档能说明现有 `chargeback` 方法只属于历史兼容或显式事件适配入口，后续是否 deprecate、guard 或 adapter 必须另行确认。
3. 文档能列出最小 Red 候选，证明授权拒绝不是 chargeback、争议无资金影响不写资金事实、争议退款能与普通退款和无授权退款区分。
4. 文档能列出代码锚点和禁止范围，避免未授权改 face 接口、事件枚举、schema 或 ledger 分录。

非目标：

1. 本文不删除、不废弃、不重命名、不改造 `FundsAuthorizationTransactionService#chargeback`。
2. 本文不新增 dispute case 模型、证据包、运营台、卡组织 representment、仲裁、追偿、准备金、商户负余额或清结算专项能力。
3. 本文不改 Java、测试、DDL/H2 schema、公共契约、状态机、错误码、枚举、Entity、Mapper 或运行时配置。
4. 本文不替代卡组织、银行、ACH、本地支付网络、收单行、发卡处理商、税务、会计、法务或合规最终确认。

## 3. 产品语义裁决

能力地图：

| 能力 | 目标口径 | 使用者价值 | 当前状态 |
| --- | --- | --- | --- |
| 争议案件过程 | dispute / chargeback 是案件、举证、裁决和责任归属过程。 | 运营、风控、法务和客服能处理证据、时限、裁决和责任。 | 资金底座只记录和消费必要资金结果，不实现完整案件系统。 |
| 争议退款资金结果 | 裁决或业务决策需要退款时，通过 `settleRefund` 携带争议字段承接。 | 用户、财务和账务能看到明确退款资金影响。 | TDD 已有目标口径，代码已有 `FundsAuthorizationTransactionRefundRequest` 争议字段。 |
| 无资金影响结果 | 用户败诉、无需资金处理或只更新案件状态时，不生成 route、posting、LedgerEntry、余额变化或新资金交易事实。 | 避免无依据调账、重复扣回和账务污染。 | 需要后续 Red 或应用层输入证明。 |
| 兼容 chargeback 入口 | 现有 `chargeback` 方法只作为历史兼容、显式事件适配或内部过渡入口。 | 保持已有调用不被文档误删，同时不扩张目标 API。 | 代码存在，目标语义仍需后续单一 Grant 裁决是否加 guard、adapter 或 deprecated。 |
| 查询和投影解释 | 查询、交易投影和审计必须区分普通退款、无授权退款、争议裁决退款、授权拒绝和兼容 chargeback 事件。 | 运营、财务、用户账单和研发排障可解释。 | B4-001 只完成首轮投影解释，争议矩阵仍 Not Done。 |

业务对象和字段口径：

- `DisputeCase`：争议案件对象，属于业务或运营能力包；资金底座首期不实现完整对象。
- `FundsAuthorizationTransactionRefundRequest`：目标态争议退款承接请求，使用 `disputeMode`、`disputeReason`、`disputeVoucherRef` 和 `externalDisputeRef` 表达争议资金结果。
- `FundsAuthorizationTransactionChargebackRequest`：现有兼容请求，不能被继续解读为新的目标态公共入口；后续若保留，必须标注适配边界和不得表达授权拒绝。
- `FundsTransactionDTO#declinedAmount`：现有累计拒付/争议退回金额字段，命名存在误读风险；后续若调整，需要单独 Grant 评估兼容。
- `RouteSnapshot` / `RouteReplayType.CHARGEBACK`：现有 route replay 兼容分支，后续必须证明它只服务历史事件或显式事件适配，不允许按当前绑定重新选路。

生命周期和状态：

1. 事件进入：用户、商户、发卡行、银行、通道或运营创建争议案件或收到 chargeback / dispute 事件。
2. 案件处理：收集证据、判定责任、处理时限、决定接受、反驳、部分处理或无需资金处理。
3. 资金结果映射：需要退款或扣回时，映射为争议类 `settleRefund` 或后续清结算/追偿专项动作；无需资金处理时只更新案件、审计或只读解释。
4. 账务执行：资金结果必须沿原授权完成或原 route snapshot 回放，生成交易事实、posting plan、LedgerEntry、余额投影和审计。
5. 查询解释：普通退款、无授权退款、争议裁决退款、授权拒绝、兼容 chargeback 事件必须可区分。

业务流程：

```text
争议事件或案件
    -> 关联原授权 / 原完成 / 原 route snapshot / 外部案件引用
    -> 运营、风控或外部规则裁决
        -> 需要退款或扣回：通过 settleRefund 携带争议字段形成资金结果
        -> 无资金影响：不调用资金交易写入链路，只更新案件、审计或只读投影解释
        -> 需要追偿、准备金、费用或清结算动作：进入 B7 / P2 专项，不混入本任务
```

异常流程：

- 授权阶段 `approved=false` 是授权拒绝，不是 chargeback，不写 `declinedAmount`，不生成 route、posting 或 LedgerEntry。
- 缺原授权、缺原完成、缺 route snapshot、缺外部争议引用、缺凭证或缺审计时，不得静默退款。
- 同一原交易同时出现普通退款和争议时，必须防重复退款、重复扣回和账务双重损失。
- 外部规则未确认、证据时限未确认、敏感数据未脱敏或专业确认缺失时，只能阻断、人工复核或 contract-only。

运营后台、指标、报表、审计和数据口径：

- 运营后台应展示案件状态、资金影响、责任方、证据引用、外部案件引用、原交易引用和处理结果。
- 指标应区分 dispute case 数、chargeback 金额、争议胜诉/败诉、资金退款金额、无资金影响案件和防重复损失命中。
- 报表不得把授权拒绝计入 chargeback，不得把普通退款和争议裁决退款混成同一资金原因。
- 审计必须记录操作者、裁决来源、凭证引用、外部案件引用、原交易引用、route snapshot 和资金结果。

## 4. 规则矩阵

| 规则 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验证方式 | 确认方 |
| --- | --- | --- | --- | --- | --- | --- |
| 授权拒绝不是拒付 | `authorize(approved=false)` | 授权拒绝只记录拒绝原因，不生成 route、posting、LedgerEntry，不写 `declinedAmount`，不生成 `CHARGEBACK` 事件。 | P0 | GSD2-AUTH | 授权拒绝 flow / boundary 回归。 | 产品 + 架构 |
| 争议案件不是资金事实 | dispute / chargeback 案件创建、举证或败诉 | 未形成需退款或扣回的资金结果前，不调用交易写入链路。 | P0 | GSD2-AUTH | 未来 no-funds-impact Red。 | 产品 + 财务 + 架构 |
| 争议退款走原路径 | 裁决需要退款或扣回 | 使用 `settleRefund` 携带争议原因、凭证和外部案件引用，沿原授权完成或原 route snapshot 回放。 | P0 | GSD2-AUTH | 争议退款 route replay 和 ledger assertion。 | 架构 |
| 兼容 chargeback 不扩张 | 现有 `chargeback` 方法被调用 | 只能作为历史兼容、显式事件适配或内部过渡入口，不作为新目标态主入口。 | P0 | GSD2-AUTH | 后续 adapter / guard / deprecated Grant。 | 用户 + 架构 |
| 投影可区分 | 查询交易、账单、审计或运营时间线 | 普通退款、无授权退款、争议裁决退款、兼容 chargeback 事件和授权拒绝必须有可解释差异。 | P1 | GSD2-B4 | 交易投影解释扩展测试。 | 产品 + 架构 |
| 外部规则待确认 | 卡组织、银行、ACH、收单或本地网络规则影响资金处理 | 未确认规则来源、版本、适用范围和时限前，不声明生产资金流完成。 | P0 | GSD2-AUTH | 外部规则确认清单和 must-fail。 | 法务 + 合规 + 通道 |

## 5. 架构和代码锚点

现状和影响范围：

- `FundsAuthorizationTransactionService#settleRefund` 已存在，是目标态争议资金结果的主承接口。
- `FundsAuthorizationTransactionRefundRequest` 已具备 `disputeMode`、`disputeReason`、`disputeVoucherRef` 和 `externalDisputeRef` 字段。
- `FundsAuthorizationTransactionService#chargeback`、`FundsAuthorizationTransactionChargebackRequest` 和 transaction-impl 的 `convertToChargebackInstruction` 仍存在，是当前兼容缺口和后续裁决对象。
- route replay、ledger posting、lifecycle saver、transaction projection 仍有 `CHARGEBACK` 事件或 replay 分支，需要后续明确兼容策略。

核心决策、职责边界和取舍：

1. 推荐目标态：`settleRefund` + 争议字段作为争议资金结果的 canonical 承接方式。
2. 保留兼容：现有 `chargeback` 方法先不删除、不替换、不扩张；后续以单一 Grant 决定 deprecate、guard、adapter 或保留显式事件入口。
3. 责任拆分：dispute case、证据包、representment、arbitration、追偿、准备金和清结算动作不放进授权交易内核。
4. 风险取舍：不在本轮直接改接口，可以避免破坏历史调用；但必须用文档和未来 Red 防止新代码继续依赖 chargeback 作为目标主入口。

接口契约、入参、出参、错误码、幂等和兼容：

- 本文不修改 `FundsAuthorizationTransactionService`、Request、DTO、枚举、错误码或幂等摘要。
- 后续若进入代码 Grant，必须明确：
  - 是否把 `chargeback` 标注为 deprecated 或兼容 adapter。
  - 是否让 `chargeback` 内部委派到 `settleRefund` 的争议字段模型。
  - 是否保留 `FundsTransactionEventType.CHARGEBACK` 作为历史事件或显式事件分类。
  - 是否调整 `declinedAmount` 命名、查询解释或 DTO 兼容文案。
- 幂等必须区分普通退款、无授权退款、争议退款和兼容 chargeback，不能让同一业务流水以不同模式重复产生资金副作用。

数据方案、事务边界、一致性、补偿和对账：

- 本文不新增表、索引、Entity、Mapper、H2 schema 或生产迁移脚本。
- 后续争议资金结果必须在同一交易写入链路内保持 funds transaction、route snapshot、posting plan、LedgerEntry、余额投影和交易投影一致。
- 无资金影响争议不得生成半截 route、posting、LedgerEntry、余额投影或新的交易事实。
- 退款和争议碰撞需要对账、清结算或追偿专项时，必须进入 B7 或 P2 独立 Grant。

可靠性、安全、权限、审计和告警：

- 可靠性：争议退款必须覆盖重复请求、同原交易多次退款、退款与争议碰撞、缺 route snapshot 和外部乱序。
- 安全：证据包、外部案件引用和凭证引用只能保存最小必要摘要，不保存 PAN、CVV、敏感认证数据、银行账户敏感号或未脱敏附件。
- 权限：人工确认、证据提交、败诉损失确认、追偿和准备金处理必须由运营、财务、风控或合规角色确认，不由授权交易接口暗含。
- 审计：任何争议资金结果必须可追溯操作者、裁决来源、凭证、原交易、route snapshot 和资金影响。
- 告警：后续生产化时应对重复扣回、无原事实退款、缺证据资金变动和外部规则未确认资金处理设置告警或 gate。

## 6. Red 候选和 Execution Grant 草案

以下候选只是后续单一 Execution Grant 的输入，不是本轮编码授权。

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `AUTH-CB-RED-001` | 争议裁决需要退款时，是否能通过 `settleRefund` 明确生成争议退款资金结果。 | 争议退款必须引用原授权或原完成事实，沿原 route snapshot 回放，且与普通退款、无授权退款和授权拒绝可区分。 | 生成资金交易、route snapshot、posting plan、LedgerEntry、余额投影、交易投影和审计上下文。 | 不得按当前绑定重新选路，不得缺争议原因、凭证或外部案件引用仍入账。 | 状态、金额、route replay、ledger entry、投影解释、幂等摘要、审计。 | `FundsAuthorizationTransactionFlowTests`、交易投影解释测试。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just compile`、`git diff --check`。 | 需要改公共契约、枚举或 schema 但 Grant 未列名。 |
| `AUTH-CB-RED-002` | 用户败诉或争议无资金影响时，系统是否不会产生资金副作用。 | 无资金影响争议只能更新案件、审计或只读解释，不生成资金事实。 | 返回无资金影响结果或由上层案件系统记录状态。 | 不得生成 route、posting、LedgerEntry、余额变化或新的 funds transaction。 | 无副作用、余额不变、ledger 不变、投影不反写。 | 需先确定案件应用入口或 adapter 测试入口。 | 待后续 Grant 明确。 | 没有案件入口或 adapter 输入模型时只允许 contract-only。 |
| `AUTH-CB-RED-003` | 现有 `chargeback` 方法是否会误导新调用方把 chargeback 当目标主入口。 | 兼容入口不得绕过争议退款字段、原路径回放和审计最小集。 | 若保留，必须成为 adapter、deprecated 或强 guard；若阻断，新调用必须转 `settleRefund`。 | 不得扩张新业务使用，不得表达授权拒绝，不得缺证据仍入账。 | 兼容策略、错误原因、幂等、投影解释、旧行为影响。 | `FundsAuthorizationTransactionService` contract / flow tests。 | 待后续 Grant 明确。 | 需要破坏兼容或删除接口时必须单独确认。 |
| `AUTH-CB-RED-004` | 授权拒绝是否仍会被误计为拒付或争议。 | 授权拒绝无账务影响，不计入 `declinedAmount`，不产生 `CHARGEBACK`。 | 只记录授权拒绝事实和拒绝原因。 | 不得写 route、posting、LedgerEntry、余额投影或 chargeback 事件。 | 拒绝状态、无资金副作用、查询解释可区分。 | `FundsAuthorizationTransactionFlowTests`、projection explain tests。 | `just test-transaction`、`just test-boundary`。 | 需要调整 DTO 字段命名或投影契约时必须单独确认。 |

建议的后续 Grant 拆法：

1. `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`：只处理 `chargeback` 兼容入口的 adapter / deprecated / guard 策略。
2. `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002`：扩展交易投影解释矩阵，覆盖普通退款、无授权退款、争议退款、授权拒绝和兼容 chargeback。
3. `GSD2-AUTH-DISPUTE-NO-FUNDS-IMPACT-001`：仅在案件应用入口明确后，证明无资金影响争议不会写资金事实。

## 7. 验证矩阵

| 验证层 | 命令或方式 | 完成条件 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md` | Task、Owner、范围、Wave、上下文账本、禁止事项、验证和 handoff 字段齐全。 |
| 产品结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md` | 背景目标、现状约束、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-AUTH-CHARGEBACK-TARGET-ALIGN|Chargeback目标语义|settleRefund" docs openspec` | LWT Goal、GSD2 入口、W5、README 和 OpenSpec tasks 能追踪到本文。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译和测试 | 本切片只改文档和任务状态，不运行 `just compile`。 | 无 Java、测试、DDL/H2 schema、公共契约或运行时配置变更。 |

## 8. 发布、灰度、回滚、风险和待确认

发布和灰度：本文不进入生产发布，不声明灰度、回滚和上线 Done。后续代码 Grant 若调整接口、事件、投影、DTO 或兼容入口，必须另写发布、灰度、回滚和旧调用影响。

风险：

1. 继续保留 `chargeback` 方法会让新调用方误以为它是目标主入口。
2. 直接删除或破坏 `chargeback` 方法可能影响历史调用、测试或外部集成。
3. `declinedAmount` 命名容易与授权拒绝混淆，后续投影和查询解释必须谨慎处理。
4. 争议案件系统未建模前，资金底座不能独立证明“无资金影响案件”的全链路，只能证明资金写入链路不应被调用。

待确认：

- 是否在后续 Grant 中把 `chargeback` 方法标注为 deprecated。
- 是否允许 `chargeback` 方法内部适配为 `settleRefund` 的争议字段模型。
- 是否保留 `CHARGEBACK` 事件作为历史兼容事件、显式资金事件，还是只保留为投影解释标签。
- 是否调整 `declinedAmount` 命名或 DTO 文案以降低误解。
- 是否由 P2 VCC、收单或 B7 清结算专项承接完整 dispute case、证据包、追偿和准备金。

## 9. 当前执行交接和 handoff

| 字段 | 内容 |
| --- | --- |
| 当前可执行任务 | `GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001` |
| 当前状态 | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` |
| 本轮写入范围 | 本文、LWT Goal、GSD-2 工作流、P0/P1 LWT 推进计划、TDD README、docs README 和 OpenSpec tasks。 |
| 本轮禁止范围 | Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、枚举、状态机、运行时配置、Git、联网、生产数据和真实资金动作。 |
| 下一建议 | 若要进入代码，优先确认 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 或转入 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002`。 |
| 验证命令 | Harness / product / architecture checker、`rg "GSD2-AUTH-CHARGEBACK-TARGET-ALIGN|Chargeback目标语义|settleRefund" docs openspec`、`git diff --check`。 |
| 编译说明 | 本切片只改文档和任务状态，不运行 `just compile`。 |
| 交接要求 | 后续每轮必须先读取本文和 LWT Goal，再确认单一 Execution Grant、兼容策略、写入范围、禁止范围、目标 Red、验证命令和回滚提示。 |
| 回滚提示 | 本轮为文档-only，回滚时只需还原本文、README、GSD2、W5、LWT Goal 和 OpenSpec tasks 的 diff。 |

## 10. 停止条件

1. 需要写 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、状态机、枚举、错误码或运行时配置。
2. 需要删除、废弃、重命名或调整 `FundsAuthorizationTransactionService#chargeback`。
3. 需要调整 `FundsTransactionDTO#declinedAmount` 或历史数据库字段语义。
4. 需要实现 dispute case、证据包、运营台、追偿、准备金、费用、清结算或对账补事实。
5. 需要真实卡组织、银行、ACH、本地支付网络、收单行、发卡处理商、税务、会计、法务或合规最终确认。
6. 需要 Git add、commit、push、PR、merge、rebase、reset 或分支切换。
7. 发现当前设计与资金主体、账务平衡、原路径回放、投影只读或失败无副作用红线冲突。
