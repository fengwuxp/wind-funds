# GSD-2 AUTH Chargeback 兼容入口 Execution Grant 确认包

## 1. 文档定位

本文是 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 的 Execution Grant 确认包，用于承接 [GSD-2-AUTH-Chargeback目标语义对齐任务卡.md](GSD-2-AUTH-Chargeback目标语义对齐任务卡.md) 的裁决，把现有 `chargeback` 入口的兼容策略、首批 Red、写入范围、禁止事项、验证命令和停止条件固化为下一轮可确认的单一 Grant。

本文最初是编码授权确认包。用户明确回复 `Execution Grant：GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 后，已允许进入对应 Red / Green / Review / Verify；本轮消费结果见第 14 节。第 1 至 13 节继续保留为该 Grant 的授权边界、禁止事项和回放审计依据。

2026-06-26 目标态已重新裁决：独立 `chargeback` 入口不再保留，并与 `expire` 一起进入移除队列。本文后续关于“保留兼容、不删除公共方法、不改 `CHARGEBACK` 分支”的文字只作为历史执行记录，不再作为当前设计、代码或任务阻断。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` |
| 原子任务 | 历史任务：曾为现有 `FundsAuthorizationTransactionService#chargeback` 建立兼容入口策略；已被 2026-06-26 `expire` / 独立 `chargeback` 移除裁决覆盖。 |
| 所属阶段 | GSD-2 / AUTH compatibility adapter / CAD candidate confirmation。 |
| Goal ID | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| Loop ID | `GSD2-LWT-PRODUCTION-CAPABILITY-LOOP-2026-06-18` |
| 当前状态 | `SUPERSEDED_BY_20260626_REMOVAL_DECISION`；历史状态为 `GREEN_VERIFIED_SUMMARY_ONLY`。 |
| Git / code baseline | `a38776c5 feat: 接入出款准入对账门禁`。 |
| Owner | AI Native 流程编排负责 Goal、Loop、状态和停止条件；产品架构专家负责 dispute / chargeback 产品语义、运营审计和验收；资深架构师负责接口兼容、TDD、Review、Refactor、编码红线、AI 产物复核、验证命令和后续实现。 |
| Wave 边界 | 本确认包只准备一个兼容入口原子任务；不得并行推进 B4 投影解释扩展、wallet facade、ledger guard、B7 清算/结算 gate、B5 审计扩展或 P2 VCC。 |
| 执行顺序 / 依赖关系 | 依赖 `GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001` 已完成；本 Grant 若确认，先写兼容入口 Red，再做最小 guard / deprecated / 文档化实现，最后回写 LWT Goal、W5 和 OpenSpec tasks。 |
| 授权范围 | 历史授权曾仅允许处理现有 `chargeback` 兼容入口的契约说明、最小 guard、兼容测试和必要的投影/查询解释衔接；当前不再授权保留独立入口，目标态以 `settleRefund / AUTH_REFUND` 承接争议资金结果。 |
| 写入范围 | 本文、LWT Goal、GSD-2 工作流、P0/P1 LWT 推进计划、TDD README、docs README 和 OpenSpec tasks 的状态同步；确认 Grant 后的候选代码写入范围见第 7 节。 |
| 写入文件 | `docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、transaction-face、transaction-impl、route replay、ledger posting、projection explain、tests、AGENTS.md、历史 B4 授权后继准入卡。 |
| 只读参考 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计/支付资金底座测试驱动设计.md`、`docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md`、`transaction/transaction-face`、`transaction/transaction-impl`、`tests`。 |
| 上下文账本 | 本文是 chargeback 兼容入口确认账本；LWT Goal、GSD-2 工作流、W5 推进计划、TDD README、docs README 和 OpenSpec tasks 是恢复入口。 |
| Git 策略 | `summary_only`。本文不授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |
| 撤销方式 | 未确认 Grant 时删除或还原本文和入口引用即可；确认并编码后，按本任务独立提交切片回滚，不得连带回滚无关历史提交。 |

## 2. 背景、目标和非目标

背景：目标语义任务卡已经明确 dispute / chargeback 是争议案件过程，不是默认独立资金结果。有资金影响时，目标态应由 `settleRefund` 携带争议字段承接；无资金影响时，不生成 route、posting、LedgerEntry、余额变化或新的资金交易事实。本任务创建时，代码仍保留 `FundsAuthorizationTransactionService#chargeback`、`FundsAuthorizationTransactionChargebackRequest`、`FundsTransactionEventType.CHARGEBACK` 和 route replay `CHARGEBACK` 分支，因此当时需要一个低风险兼容入口策略，避免新调用方继续把它当成目标态主入口。2026-06-26 后，上述独立入口和分支已进入移除目标态，本文仅保留历史授权和审计记录。

业务目标：保留历史兼容能力，同时让产品、运营、财务、风控、研发和测试能清楚区分普通退款、争议裁决退款、兼容 chargeback 事件、授权拒绝和无资金影响争议。

用户价值：调用方不会因为看到 `chargeback` 方法就误以为应新增拒付主链路；运营和财务在查询、投影、审计和对账时能解释这是兼容入口、争议资金结果还是无资金影响案件。

成功指标：

1. 现有 `chargeback` 入口被明确标注为兼容、显式事件或内部适配入口，不再作为新的目标态主入口扩张。
2. 授权拒绝继续不生成 route、posting、LedgerEntry、余额变化或 `CHARGEBACK` 事件。
3. 兼容入口不得缺原授权 / 原完成事实、缺原 route snapshot 或缺审计上下文仍入账。
4. 首轮实现不删除公共 API、不做破坏性字段变更、不新增 DDL/H2 schema、不引入完整 dispute case。
5. 后续若要把 `chargeback` 委派到 `settleRefund`、调整 `CHARGEBACK` 事件语义或改 `declinedAmount`，必须另开 Grant。

非目标：

1. 不删除 `FundsAuthorizationTransactionService#chargeback`，不把现有方法直接从公共契约移除。
2. 不把 `chargeback` 整体迁移为 `settleRefund` 委派实现，除非后续单独 Grant 明确兼容影响。
3. 不新增 dispute case、证据包、representment、arbitration、准备金、追偿、运营台、商户负余额或完整卡组织规则能力。
4. 不修改 DDL/H2 schema、生产迁移脚本、Entity、Mapper、运行时配置或外部规则。
5. 不替代卡组织、银行、ACH、本地支付网络、收单行、发卡处理商、税务、会计、法务或合规最终确认。

## 3. 产品语义和能力地图

能力地图：

| 能力域 | 目标口径 | 使用者价值 | 本 Grant 处理 |
| --- | --- | --- | --- |
| 争议案件过程 | dispute / chargeback 是案件、举证、裁决和责任归属过程。 | 运营、风控、法务和客服理解案件流程。 | 不实现案件系统，只保护资金入口不误扩张。 |
| 争议退款资金结果 | 有资金影响时由 `settleRefund` 争议字段作为目标态承接。 | 用户、财务和账务能看到明确退款资金影响。 | 不重写目标态，只避免兼容入口被当主入口。 |
| 兼容 chargeback 入口 | 现有 `chargeback` 只作为历史兼容、显式事件或内部适配入口。 | 历史调用可继续解释，新调用不被误导。 | 本 Grant 推荐先做 `COMPAT_GUARD_NO_BEHAVIOR_BREAK`。 |
| 授权拒绝 | 授权拒绝不是拒付，不入账、不计拒付金额。 | 避免账务污染和指标误报。 | 必须保留或补充回归。 |
| 查询和投影解释 | 普通退款、争议退款、授权拒绝、兼容 chargeback 必须可区分。 | 运营、财务、账单和研发排障可解释。 | 本 Grant 只做必要兼容说明；完整解释矩阵进入 B4-002。 |

业务对象和字段口径：

- `FundsAuthorizationTransactionService#chargeback`：历史兼容公共方法，首轮不删除、不改名；确认 Grant 后可增加 JavaDoc、`@Deprecated` 或等价兼容说明，但不得直接破坏编译兼容。
- `FundsAuthorizationTransactionChargebackRequest`：兼容请求对象，不能表达授权拒绝；若需要新增审计或外部案件引用字段，必须确保向后兼容并列入写入范围。
- `FundsAuthorizationTransactionRefundRequest`：目标态争议资金结果承接对象，继续保留 `disputeMode`、`disputeReason`、`disputeVoucherRef` 和 `externalDisputeRef` 的目标口径。
- `FundsTransactionEventType.CHARGEBACK`：历史或显式事件分类，首轮不删除；后续是否降级为投影标签、历史事件或内部事件需另行 Grant。
- `FundsTransactionDTO#declinedAmount`：命名存在误读风险，本 Grant 不改字段名；只允许通过文档、测试或投影解释降低误读。

业务流程：

```text
外部 dispute / chargeback 事件或内部兼容调用
    -> 校验原授权 / 原完成 / 原 route snapshot / 审计上下文
    -> 若是目标态争议退款：优先走 settleRefund 争议字段
    -> 若是历史兼容 chargeback：按兼容入口 guard 执行或阻断
    -> 若是授权拒绝或无资金影响争议：不得调用资金写入链路
    -> 查询和审计中标明普通退款 / 争议退款 / 兼容 chargeback / 授权拒绝差异
```

异常流程：

- 授权拒绝不得生成 `CHARGEBACK`。
- 缺原授权、缺原完成、缺 route snapshot、缺审计引用或重复冲突时，失败必须无资金副作用。
- 历史兼容入口不得按当前账户、支付工具或资金责任重新选路。
- 需要完整案件、证据包、追偿、准备金、清结算或商户负余额时，停止并进入独立专项。

运营后台、指标、报表、审计和数据口径：

- 运营后台应展示兼容 chargeback 与目标态争议退款的区别。
- 指标不得把授权拒绝计入 chargeback 金额或争议资金损失。
- 报表必须区分普通退款、争议裁决退款、兼容 chargeback 事件和无资金影响案件。
- 审计必须能追溯原交易、route snapshot、外部案件引用、凭证、操作人、处理结果和资金影响。

## 4. 规则矩阵

| 规则 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验证方式 | 确认方 |
| --- | --- | --- | --- | --- | --- | --- |
| 兼容入口不扩张 | `chargeback` 被调用 | 仅作为历史兼容、显式事件或内部适配入口；不作为新目标态主入口。 | P0 | GSD2-AUTH-COMPAT | JavaDoc / deprecated / guard / flow 回归。 | 用户 + 架构 |
| 授权拒绝不是拒付 | `authorize(approved=false)` | 不生成 route、posting、LedgerEntry、余额变化、`CHARGEBACK` 事件或拒付金额。 | P0 | GSD2-AUTH-COMPAT | 授权拒绝 flow / boundary 回归。 | 产品 + 架构 |
| 原路径回放 | 历史兼容 chargeback 产生资金影响 | 必须引用原授权、原完成或原 route snapshot，不按当前绑定重新选路。 | P0 | GSD2-AUTH-COMPAT | route replay 和 ledger 断言。 | 架构 |
| 失败无副作用 | 缺原事实、超额、幂等冲突、证据不足 | 失败前不生成半截 route、posting、LedgerEntry、余额投影或新交易事实。 | P0 | GSD2-AUTH-COMPAT | service-flow must-fail。 | 架构 |
| 查询可解释 | 用户账单、运营时间线、财务报表查询 | 兼容 chargeback 与普通退款、争议退款、授权拒绝可区分。 | P1 | GSD2-B4 | B4 投影解释扩展。 | 产品 + 架构 |
| 外部规则待确认 | 卡组织、银行、ACH 或本地网络规则影响资金处理 | 未确认规则版本、时限和适用范围前，不声明生产 dispute/chargeback 全链路 Done。 | P0 | GSD2-AUTH-COMPAT | 外部规则待确认清单。 | 法务 + 合规 + 通道 |

## 5. 推荐策略和备选方案

推荐策略：`COMPAT_GUARD_NO_BEHAVIOR_BREAK`。

含义：首轮保留现有公共方法和历史事件，先做兼容说明、最小 guard 和回归测试。该策略不删除接口、不把所有 chargeback 行为强迁到 `settleRefund`、不调整 schema，也不把完整 dispute case 塞进交易内核。它解决的是“不要误用、不要扩张、不要把授权拒绝当拒付、不要缺原事实入账”。

| 方案 | 结论 | 优点 | 风险 | 本轮建议 |
| --- | --- | --- | --- | --- |
| A. 兼容 guard，不破坏旧行为 | 保留 public API，补 JavaDoc / deprecated / guard / 回归。 | 风险低，能快速降低误用，兼容旧调用。 | 仍保留 `chargeback` 名称，后续需要投影解释继续降噪。 | 推荐。 |
| B. 直接委派 `settleRefund` | `chargeback` 内部改为争议退款模型。 | 目标态更统一。 | 可能改变事件、投影、幂等摘要和历史调用语义。 | 不作为首轮。 |
| C. 删除或硬阻断 `chargeback` | 公共方法删除或默认抛错。 | 概念最干净。 | 破坏公共契约和历史调用，发布风险高。 | 禁止本轮执行。 |

核心决策、职责边界和取舍：

1. 现有 `chargeback` 是兼容资产，不是目标态主入口。
2. 目标态 dispute 资金结果继续以 `settleRefund` 争议字段为 canonical 承接。
3. 首轮只做低风险兼容 guard，不做破坏性 API、事件、DTO、schema 或全链路迁移。
4. 完整投影解释矩阵仍归属 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002`。
5. 完整 dispute case、证据包、追偿、准备金和清结算影响仍归属 P2 / B7 专项。

## 6. 架构和代码锚点

现状和影响范围：

- `transaction/transaction-face` 已暴露 `FundsAuthorizationTransactionService#chargeback` 和 `FundsAuthorizationTransactionChargebackRequest`。
- `transaction/transaction-impl` 已有授权 chargeback 转换、命令服务、生命周期保存、route replay 和 ledger posting 衔接。
- `FundsAuthorizationTransactionFlowTests` 已包含部分授权 chargeback 相关测试，但目标语义需要按兼容入口重排。
- 交易投影解释已完成 B4-001 首轮，只覆盖 posted pay、declined authorization 和缺 RouteSnapshot fail-fast；完整退款 / 争议 / chargeback 矩阵仍未 Done。

接口契约、入参、出参、错误码、幂等和兼容：

- 确认 Grant 后，允许在不破坏旧调用的前提下补充 `chargeback` 方法和 request 的兼容说明、废弃提示或最小新增可选审计字段。
- 不允许删除方法、强制删除字段、修改既有字段语义或改变旧调用必填条件，除非用户另行确认破坏性兼容策略。
- 如果实现 guard，需要优先使用已有原授权、原完成、route snapshot 和幂等摘要事实，不新增独立事实表。
- 幂等必须继续区分普通退款、无授权退款、争议退款和兼容 chargeback，不能让同一业务流水以不同入口重复造成资金副作用。

数据方案、事务边界、一致性、补偿和对账：

- 本 Grant 不授权 DDL/H2 schema、生产迁移、Entity 或 Mapper 改动。
- 资金影响必须沿现有交易写入链路保持 funds transaction、route snapshot、posting plan、LedgerEntry、余额投影和交易投影一致。
- 失败路径必须证明没有半截 route、posting、LedgerEntry、余额投影或新交易事实。
- 涉及清算、结算、追偿、准备金、商户负余额、差错单或补事实时，停止并进入 B7 / P2 独立 Grant。

可靠性、安全、权限、审计和告警：

- 可靠性：覆盖重复请求、超额、缺原事实、缺 route snapshot、幂等冲突和历史兼容调用。
- 安全：外部案件引用、凭证和证据摘要只能保存最小必要信息，不保存 PAN、CVV、敏感认证数据、完整证件影像或无关附件。
- 权限：人工确认、证据提交和资金损失确认由运营、财务、风控或合规角色承担，不由交易内核隐式决定。
- 审计：必须能追溯原交易、route snapshot、资金影响、操作人和兼容入口原因。
- 告警：生产化时应对缺原事实退款、重复扣回、无证据资金变化和外部规则未确认资金处理设置 gate 或告警；本轮只登记为 Not Done。

## 7. 候选写入范围和禁止事项

确认 `Execution Grant：GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 后，候选写入范围上限如下。实际进入编码时，资深架构师仍需按工作树状态和首个 Red 收窄。

| 类型 | 候选范围 |
| --- | --- |
| face 契约 | `FundsAuthorizationTransactionService`、`FundsAuthorizationTransactionChargebackRequest` 的兼容说明、废弃提示或可选审计字段。 |
| impl 转换 | 授权 chargeback instruction converter 的最小 guard 或上下文标记。 |
| impl 命令 | `FundsTransactionCommandServiceImpl#chargeback` 相关兼容 guard、错误原因和失败无副作用处理。 |
| lifecycle / replay | 只允许在保持旧事件兼容的前提下校验原路径、原 route snapshot 和历史事件上下文。 |
| tests | `FundsAuthorizationTransactionFlowTests`、必要的投影解释或 route replay 回归测试。 |
| docs / OpenSpec | 本文、LWT Goal、W5、README、OpenSpec tasks 和 Not Done 回写。 |

禁止事项：

1. 不删除 `FundsAuthorizationTransactionService#chargeback` 或 `FundsAuthorizationTransactionChargebackRequest`。
2. 不删除 `FundsTransactionEventType.CHARGEBACK`，不重写事件枚举语义。
3. 不改 DDL/H2 schema、生产迁移、Entity、Mapper、运行时配置或外部通道配置。
4. 不实现完整 dispute case、证据包、representment、arbitration、追偿、准备金、运营台、商户负余额或外部规则。
5. 不把 `chargeback` 适配为完整 `settleRefund` 委派，除非新的单一 Grant 明确事件、幂等、投影和兼容影响。
6. 不把授权拒绝、普通退款、无授权退款、争议退款和兼容 chargeback 混成同一投影标签。
7. 不新增统一支付工具交易服务，不替换账户主体型 canonical 请求。
8. 不做 Git add、commit、push、PR、merge、rebase、reset 或分支切换。

## 8. Red 候选、TDD 和验证矩阵

首批 Red 只用于确认后实施；本文不写测试。

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `AUTH-CB-COMPAT-RED-001` | 现有 `chargeback` 是否被明确限制为兼容入口，而不是目标态主入口。 | 兼容入口不得绕过原事实、原路径和审计最小集。 | 旧成功路径在兼容 guard 下仍可解释，且新文档 / deprecate / guard 明确使用边界。 | 不得删除公共方法，不得把授权拒绝转成 chargeback。 | 契约说明、兼容标识、成功路径、失败路径、无半截事实。 | `FundsAuthorizationTransactionService`、`FundsAuthorizationTransactionFlowTests`。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just compile`、`git diff --check`。 | 需要破坏 API 或 schema 时停止。 |
| `AUTH-CB-COMPAT-RED-002` | 授权拒绝是否仍不会被误记为 chargeback。 | 授权拒绝无账务影响。 | 只记录授权拒绝事实和拒绝原因。 | 不得生成 route、posting、LedgerEntry、余额投影、`CHARGEBACK` 事件或拒付金额。 | 拒绝状态、无 route、无 entry、无 balance change、投影解释可区分。 | `FundsAuthorizationTransactionFlowTests`、projection explain tests。 | `just test-transaction`、`just test-boundary`。 | 需要调整 DTO 字段名时停止。 |
| `AUTH-CB-COMPAT-RED-003` | 缺原授权、原完成或原 route snapshot 的兼容 chargeback 是否 fail-fast。 | 不允许缺原事实仍产生资金影响。 | 返回明确错误原因，无副作用。 | 不得生成半截交易、route、posting、LedgerEntry 或余额投影。 | 失败错误、余额不变、ledger 不变、幂等不污染。 | 授权 flow tests、route replay tests。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、必要时 `just test-one DefaultRouteReplayServiceTests tests`。 | 需要新增持久化事实表时停止。 |
| `AUTH-CB-COMPAT-RED-004` | 兼容 chargeback 与目标态争议退款是否能在查询或审计中区分。 | 查询解释不得把普通退款、争议退款、兼容事件和授权拒绝混同。 | 返回兼容入口解释或明确 Not Done。 | 不得反写交易事实或 ledger facts。 | projection explain / audit label / Not Done。 | 交易投影解释测试。 | 可转入 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002`。 | 若需要扩完整解释矩阵，转 B4-002。 |

验证矩阵：

| 验证层 | 命令或方式 | 完成条件 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` | Task、Owner、范围、验证、TDD、Review、Execution Grant、人工确认和交接字段齐全。 |
| GSD Wave 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` | Wave、上下文账本、禁止事项、验证矩阵和 handoff 字段齐全。 |
| 产品结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` | 背景目标、现状、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER|Chargeback兼容入口|COMPAT_GUARD_NO_BEHAVIOR_BREAK" docs openspec` | LWT Goal、GSD2、W5、README 和 OpenSpec tasks 能追踪到本文。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译和测试 | 本切片只改文档和任务状态，不运行 `just compile`。 | 无 Java、测试、DDL/H2 schema、公共契约或运行时配置变更。 |

确认后建议验证命令：

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one FundsAuthorizationTransactionFlowTests tests
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-transaction
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just compile
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just pmd
git diff --check
```

## 9. 发布、灰度、回滚、风险和待确认

发布和灰度：本文不发布生产能力。确认并编码后，如果只做 JavaDoc / deprecated / guard / 测试，不需要运行时开关；如果改变事件、投影、幂等摘要、错误码或兼容字段，则必须补发布、灰度、回滚和旧调用影响评估。

风险：

1. 保留 `chargeback` 方法会继续带来概念噪音，需要通过兼容说明、测试和投影解释持续降噪。
2. 直接迁移到 `settleRefund` 可能改变历史事件、投影和幂等摘要，首轮不做。
3. 强阻断或删除方法会破坏公共契约，首轮不做。
4. 完整 dispute case 未实现前，本 Grant 只能保护资金写入边界，不能声明争议运营全链路生产 Done。

历史待确认及消费裁决：

- 用户已确认按推荐策略 `COMPAT_GUARD_NO_BEHAVIOR_BREAK` 执行，本 Grant 已消费。
- 本 Grant 消费时公共方法和请求类只补充 JavaDoc 兼容说明；后续 `GSD2-TRX-CHARGEBACK-ENTRY-DEPRECATION-001` 已单独完成 `@Deprecated` 标注，公共 API 退出计划以 `GSD2-TRX-CHARGEBACK-PUBLIC-API-EXIT-PLAN-001` 为准。
- 不新增可选请求字段；最小审计上下文复用 `contextVariables` 中的 `chargebackReason`、`evidenceRef` 和 `externalDisputeRef`。
- 完整查询解释矩阵转入候选 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002`。
- P2 VCC、收单或 B7 清结算专项仍后续独立承接完整案件、证据、追偿和准备金。

## 10. 历史确认文本和 handoff

用户已复制确认并消费如下 Grant，文本保留为审计和回放依据：

```text
Execution Grant：GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001
策略：COMPAT_GUARD_NO_BEHAVIOR_BREAK
确认按 docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md 执行，只允许处理现有 chargeback 兼容入口的契约说明、最小 guard、兼容测试和状态回写；不删除公共 API、不做 DDL/H2 schema、不引入完整 dispute case、不提交 Git。
```

| 字段 | 内容 |
| --- | --- |
| 当前可执行任务 | `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` |
| 当前状态 | `GREEN_VERIFIED_SUMMARY_ONLY` |
| 推荐策略 | `COMPAT_GUARD_NO_BEHAVIOR_BREAK` |
| 本轮写入范围 | transaction-face 兼容说明、transaction-impl 最小 guard、授权 flow 测试、本文、LWT Goal、GSD-2 工作流、P0/P1 LWT 推进计划、TDD README、docs README 和 OpenSpec tasks。 |
| 本轮禁止范围 | 不删除公共 API、不改 DDL/H2 schema、不改 Entity / Mapper / 状态机 / 运行时配置、不引入完整 dispute case、不执行 Git、联网、生产数据或真实资金动作。 |
| 确认后第一动作 | 已执行：新增 `AUTH-CB-COMPAT-RED-001` 的更窄失败无副作用 Red，随后最小 Green。 |
| 验证命令 | 已执行目标测试、交易分组、compile、pmd、状态 `rg` 扫描和 `git diff --check`。 |
| 编译说明 | 本切片包含 Java、测试和公共契约注释变更，已运行 `just compile`。 |
| 交接要求 | 后续每轮必须先读取本文、目标语义任务卡和 LWT Goal，再确认新的单一 Execution Grant 是否有效、写入范围是否越界、验证是否通过、Not Done 是否需要回写。 |
| 回滚提示 | 本轮按独立任务切片回滚；不得连带回滚无关历史提交或用户未归属变更。 |

## 11. Grant 消费预检清单

本节用于用户复制确认后、资深架构师进入首个 Red 前执行。预检通过只表示可以开始本 Grant 的第一个失败用例，不代表后续 Green、Review、验证、Git 或生产准出已经通过。

| 预检项 | 通过口径 | 失败处理 |
| --- | --- | --- |
| 用户授权 | 最新用户消息明确包含 `Execution Grant：GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`，且策略仍为 `COMPAT_GUARD_NO_BEHAVIOR_BREAK` 或等价低风险兼容 guard。 | 未确认时只允许继续文档、状态和任务卡维护。 |
| 工作树状态 | `git status --short` 中未提交变更已分类；与本 Grant 无关的用户改动不纳入 Red / Green / Done 证据。 | 影响目标文件且无法安全区分时暂停并请用户确认。 |
| 当前基线 | Git/code baseline 仍可追踪到 `a38776c5` 及后续文档-only 变更；若已有新提交，先回写 baseline。 | baseline 漂移时先更新 LWT Goal、W5 和 OpenSpec tasks。 |
| 首个 Red 收窄 | 默认从 `AUTH-CB-COMPAT-RED-001` 或更窄 must-fail 开始；若现有代码已满足该 Red，则改用 `AUTH-CB-COMPAT-RED-002` 或 `AUTH-CB-COMPAT-RED-003`。 | 不得直接写 Green；先证明缺口或记录现有证据。 |
| 写入范围 | 只触碰第 7 节列出的 face 契约说明、impl 最小 guard、授权 flow 测试、必要投影/route replay 回归和文档状态回写。 | 需要 DDL/H2 schema、Entity、Mapper、公共字段破坏性变更或完整 dispute case 时停止。 |
| 验证顺序 | 先目标测试，再相关分组，再 `compile`、`pmd` 和 `git diff --check`；如测试受本地端口或依赖限制，按项目权限规则重跑或记录环境限制。 | 任何失败必须先判断是否本 Grant 范围内可修复，不能放宽断言。 |
| 状态回写 | Red / Green / Review / Verify 后必须回写本文、LWT Goal、W5 和 OpenSpec tasks 的被消费证据、Not Done、验证命令和下一 owner。 | 未回写不得声明 Grant 完成。 |
| Git 策略 | 本确认包默认 `summary_only`；只有用户另行要求提交，且验证通过、工作树无混入无关改动时，才执行本地提交。 | 未获 Git 授权时只给建议提交单元和 commit message。 |

## 12. Grant 消费运行卡

本节是确认后给资深架构师消费的第一轮 TDD 运行卡。它只在用户复制确认本 Grant 后生效；未确认前仍只作为交接说明。

| 阶段 | 动作 | 完成证据 | 停止条件 |
| --- | --- | --- | --- |
| Red 选择 | 优先选择 `AUTH-CB-COMPAT-RED-001`；若现有代码已经满足兼容说明和 guard 要求，则改选 `AUTH-CB-COMPAT-RED-002`；若仍无法形成失败样例，再改选 `AUTH-CB-COMPAT-RED-003`。 | 失败测试能明确暴露“兼容入口边界、授权拒绝不误计、缺原事实 fail-fast”之一。 | 需要删除 public API、改 schema、引入完整 dispute case 或调整 DTO 兼容字段时停止。 |
| Red 范围 | 首个 Red 只允许落在 `FundsAuthorizationTransactionFlowTests` 或等价授权服务流测试；必要时补 route replay 失败无副作用回归。 | 测试名、注释和断言能表达业务事实、资金不变量、禁止事实和无副作用。 | 需要新建完整案件入口、运营台或清结算追偿测试时停止。 |
| Green 实现 | 最小实现只允许兼容说明、最小 guard、错误原因、上下文标记或必要的失败无副作用保护。 | 首个 Red 通过，且旧 chargeback 成功路径、超额失败、幂等冲突和授权拒绝回归不倒退。 | 需要重写 `chargeback -> settleRefund` 委派或改变事件、幂等、投影语义时停止。 |
| Review | 按资金红线检查：账务主体、route replay、posting plan、LedgerEntry、余额投影、交易投影、幂等、失败无副作用和审计上下文。 | Review 结论能说明本轮只保护兼容入口，不声明完整 dispute / chargeback 生产链路 Done。 | 发现授权拒绝、普通退款、争议退款、兼容 chargeback 被混同，或出现半截事实时停止。 |
| Verify | 先运行目标测试，再运行相关分组和构建规约。 | 至少记录 `just test-one FundsAuthorizationTransactionFlowTests tests`、必要分组测试、`just compile`、`just pmd` 和 `git diff --check` 的结果或环境限制。 | 任一失败无法在本 Grant 范围内修复时停止并回写。 |
| Handoff | 回写本文、LWT Goal、W5 和 OpenSpec tasks。 | 回写被消费 Red、修改文件、验证命令、Not Done、下一候选和建议提交单元。 | 未回写状态不得声明本 Grant 完成。 |

最小断言清单：

1. 兼容 `chargeback` 入口不能表达授权拒绝。
2. 授权拒绝不得生成 route、posting、LedgerEntry、余额投影、`CHARGEBACK` 事件或拒付金额。
3. 兼容 `chargeback` 产生资金影响时必须沿原授权、原完成或原 route snapshot 回放。
4. 缺原事实、超额、幂等冲突或证据不足时失败无资金副作用。
5. 本轮不得把普通退款、无授权退款、争议退款、兼容 chargeback 和授权拒绝混成同一投影或审计语义。

## 13. 停止条件

1. 需要删除、重命名或破坏 `FundsAuthorizationTransactionService#chargeback`。
2. 需要把 `chargeback` 完整委派到 `settleRefund` 并改变事件、投影、幂等或历史行为。
3. 需要修改 DDL/H2 schema、生产迁移、Entity、Mapper、运行时配置或外部通道配置。
4. 需要调整 `FundsTransactionDTO#declinedAmount` 字段名或公共 DTO 兼容语义。
5. 需要实现完整 dispute case、证据包、representment、arbitration、追偿、准备金、商户负余额、运营台或完整清结算对账。
6. 需要联网、依赖安装、生产配置、真实资金、卡组织、银行、ACH、本地网络、税务、会计、法务或合规最终确认。
7. 需要 Git add、commit、push、PR、merge、rebase、reset 或分支切换。
8. 验证失败且无法在本 Grant 范围内修复。
9. 发现授权拒绝、普通退款、争议退款、兼容 chargeback 或无资金影响争议被混同。

## 14. Grant 消费结果和交接

本节记录 `Execution Grant：GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 的本轮消费结果。该 Grant 已消费，后续不得沿用本 Grant 继续扩展完整 dispute case、改事件语义、改 DDL/H2 schema、删除公共 API 或把 `chargeback` 委派为新的目标态主入口。

| 字段 | 内容 |
| --- | --- |
| 消费状态 | `GREEN_VERIFIED_SUMMARY_ONLY`。 |
| 策略 | `COMPAT_GUARD_NO_BEHAVIOR_BREAK`。 |
| 首个 Red | `testAuthorizationChargebackMissingExternalDisputeRefShouldRejectAndLeaveNoSideEffects`：已完成授权结算后的兼容 `chargeback` 缺少外部争议引用时必须在资金事实写入前失败，余额、交易累计、route、LedgerTransaction、LedgerEntry 和业务流水事实均不得产生副作用。 |
| Red 证据 | 首次运行 `just test-one FundsAuthorizationTransactionFlowTests tests` 时，新增用例按预期失败，当前实现会接受缺少 `externalDisputeRef` 的兼容 chargeback。 |
| Green 实现 | `FundsAuthorizationInstructionConverter#convertToChargebackInstruction` 在构造 instruction 前合并并校验最小审计上下文：`chargebackReason`、`evidenceRef` 和 `FundsInstructionContextKeys.EXTERNAL_DISPUTE_REF`；缺任一字段时通过 `AssertUtils.hasText` fail-fast，不进入交易编排和账务写入。 |
| 契约说明 | `FundsAuthorizationTransactionService#chargeback` 和 `FundsAuthorizationTransactionChargebackRequest` 已补充兼容入口说明：新增资金结果场景应优先使用 `settleRefund` 争议字段；继续使用本入口时必须携带拒付原因、证据引用和外部争议引用等最小审计上下文。 |
| 回归补齐 | 已补充既有 chargeback 成功路径和超额失败路径的 `externalDisputeRef` 上下文，保持历史兼容成功路径仍可解释。 |
| 验证命令 | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one FundsAuthorizationTransactionFlowTests tests` 通过，32 tests / 0 failures / 0 errors；`just test-transaction` 通过，107 tests / 0 failures / 0 errors；`just compile` 通过；`just pmd` 通过；`git diff --check` 通过。 |
| 写入文件 | `transaction/transaction-face/src/main/java/com/wind/funds/transaction/application/FundsAuthorizationTransactionService.java`、`transaction/transaction-face/src/main/java/com/wind/funds/transaction/model/request/FundsAuthorizationTransactionChargebackRequest.java`、`transaction/transaction-impl/src/main/java/com/wind/funds/transaction/converter/FundsAuthorizationInstructionConverter.java`、`tests/src/test/java/com/wind/funds/transaction/application/flow/FundsAuthorizationTransactionFlowTests.java`、本文、LWT Goal、W5、GSD-2 工作流、README 和 OpenSpec tasks 状态回写。 |
| Not Done | 不实现完整 dispute case、证据包、representment、arbitration、追偿、准备金、商户负余额、运营台、清结算影响、完整投影解释矩阵、`chargeback -> settleRefund` 委派或 `FundsTransactionDTO#declinedAmount` 字段调整。 |
| 下一候选 | 若继续交易解释，建议进入 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002`，补普通退款、无授权退款、争议退款、释放、授权拒绝和兼容 chargeback 的查询解释矩阵；也可按 LWT Goal 第 8.1 节改选 wallet facade、ledger guard、B7 清算/结算 gate 或 B5 审计扩展。 |
| Git 策略 | `summary_only`。本 Grant 未授权 Git add、commit、push、PR、merge、rebase、reset 或分支切换。 |
