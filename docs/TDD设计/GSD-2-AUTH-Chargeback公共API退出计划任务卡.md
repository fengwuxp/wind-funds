# GSD-2 AUTH Chargeback 公共 API 退出计划任务卡

## 1. 文档定位

本文是 `GSD2-TRX-CHARGEBACK-PUBLIC-API-EXIT-PLAN-001` 的 design-only 历史任务卡，用于记录旧 `chargeback` 公共入口退役治理过程。2026-06-26 目标态重新裁决后，独立 `chargeback` 入口已与 `expire` 一起进入移除队列；本卡中“不删除”“删除不 ready”的旧结论只作为历史审计材料，不再作为当前目标态阻断。

当前目标态不再保留独立 `FundsAuthorizationTransactionService#chargeback`、`FundsAuthorizationTransactionChargebackRequest`、`CHARGEBACK` event 或 route replay 分支。争议、拒付或 chargeback 仍是业务或外部过程，资金层只消费裁决后的结果：需要退款时走 `settleRefund / AUTH_REFUND` 的争议字段，无资金影响时不调用交易写入链路。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-TRX-CHARGEBACK-PUBLIC-API-EXIT-PLAN-001` |
| 原子任务 | 明确 `chargeback` 历史公共 API 的退出策略、迁移门禁、调用方扫描、验证证据和停止条件。 |
| 所属阶段 | GSD-2 / transaction compatibility governance / design-only。 |
| 当前状态 | `SUPERSEDED_BY_20260626_REMOVAL_DECISION` |
| Owner | 产品架构专家负责 dispute / chargeback 产品语义、使用者解释和运营边界；资深架构师负责公共契约兼容、迁移路径、测试门禁和删除风险；AI Native 负责任务状态、Loop 边界和交接。 |
| 写入范围 | 本文、LWT Goal、TDD README、docs README、OpenSpec spec 和 OpenSpec tasks 的状态同步。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、transaction-face、transaction-impl、route replay、projection explain 和授权交易测试。 |
| Git 策略 | `summary_only`；除非用户另行要求提交，本卡不等同 Git 授权。 |

## 2. 背景、目标和非目标

背景：当前设计已经确认 dispute / chargeback 是争议案件过程，不是授权交易层默认资金结果。交易层只消费裁决后的资金结果：需要退款时走 `settleRefund / AUTH_REFUND` 并保留争议原因、凭证、外部案件引用和审计；无资金影响时不调用资金交易入口。2026-06-26 进一步裁决独立 `chargeback` 公共入口不再保留，旧兼容入口、请求模型、事件枚举和 replay 分支进入移除。

目标：把“旧入口退役治理”从代码实现中拆出来，形成一组可执行、可审计、可停止的退出条件。后续团队在决定删除、改签、隐藏或迁移旧公共 API 前，必须能证明迁移对象、替代入口、兼容窗口、调用方影响、测试回归和发布回滚均已准备好。

非目标：

1. 不把 `chargeback` 扩展成新的交易能力或完整 dispute case。
2. 不恢复旧公共方法、请求类、事件枚举或 route replay 分支。
3. 不把旧入口改成 `settleRefund` 委派实现后继续保留。
4. 不处理 representment、arbitration、准备金、追偿、商户负余额、完整清结算或 VCC processor 专项。
5. 不替代外部规则、法务、合规、财务、通道或卡组织最终确认。

## 3. 产品语义裁决

| 对象 | 目标态 | 兼容态 | 退出判断 |
| --- | --- | --- | --- |
| 争议案件 | 上层业务、运营或通道适配层管理案件、证据、裁决和责任。 | 资金底座只保留必要外部案件引用和审计上下文。 | 案件过程不得因旧 API 删除而丢失只读审计和使用者解释。 |
| 争议资金结果 | `settleRefund / AUTH_REFUND` 携带争议字段承接。 | 旧 `chargeback` 入口退出目标态。 | 新业务入口、文档和测试均不再要求调用 `chargeback`。 |
| 无资金影响争议 | 不调用交易写入链路。 | 可由上层案件系统、运营台或投影解释记录。 | 不能为了兼容旧 API 而生成资金交易、route、posting、LedgerEntry 或余额变化。 |
| 历史 `CHARGEBACK` 事实 | 本仓库目标态不再生成独立 `CHARGEBACK` 事实；如未来需要历史数据读取，只能由归档、迁移或外部只读解释专项承接。 | 不再作为交易内核事件分支保留。 | 删除公共 API 不等于建设新的争议案件系统。 |

## 4. 推荐退出策略

推荐策略已由 `STAGED_PUBLIC_API_EXIT_WITH_USAGE_GUARD` 更新为 `REMOVE_PUBLIC_CHARGEBACK_ENTRY_USE_SETTLE_REFUND_RESULT`。

含义：删除独立公共入口和交易内核分支，争议资金结果统一通过 `settleRefund / AUTH_REFUND` 表达；无资金影响的 chargeback 过程不进入资金写入链路。历史任务卡只保留旧治理证据，不再要求继续维护 `CompatChargeback` 代码路径。

| 阶段 | 目标 | 允许动作 | 禁止动作 |
| --- | --- | --- | --- |
| Phase 0 已完成 | 目标语义对齐，旧入口标注 deprecated。 | 文档、OpenSpec、JavaDoc、deprecated 和最小 guard。 | 删除 API、改事件语义、迁移到 `settleRefund` 委派。 |
| Phase 1 已完成 | 调用方扫描和替代入口证明。 | 扫描生产源码、测试、文档、OpenAPI 或 RPC 暴露面；列调用方和替代路径。 | 未列调用方就删除公共 API。 |
| Phase 2 首轮已执行 | 测试和投影迁移。 | 把新场景测试迁移到 `settleRefund` 争议字段，保留历史 `CHARGEBACK` 解释回归。 | 删除历史事件解释、让旧事实不可查。 |
| Phase 3 已由 2026-06-26 裁决覆盖 | 公共 API、请求模型、事件枚举和 replay 分支移除。 | 删除旧入口，迁移测试到争议退款结果或删除历史兼容测试。 | 恢复旧入口、把 chargeback 扩展成完整 dispute case，或和 VCC、清结算、Spend Rule、ledger、wallet 切片混做。 |

## 5. 删除前门禁

物理删除 `chargeback` 公共 API 前，必须同时满足：

1. 调用方扫描：生产源码、测试源码、文档、用户接入指南、OpenSpec、可能的外部 SDK 或 RPC 暴露面均已扫描，调用方清单有 owner 和处理结论。
2. 替代入口：所有新增资金影响型争议场景都能通过 `settleRefund` 争议字段表达，并保留原因、凭证、外部案件引用和操作者审计。
3. 历史解释：历史 `CHARGEBACK` event、route replay、投影解释、归档和对账回归仍可解释，不因删除公共 API 失去历史事实口径。
4. 测试迁移：普通退款、争议退款、无授权退款、兼容 chargeback、授权拒绝和无资金影响争议均有清晰测试边界；新目标测试不再以调用 `chargeback` 作为目标态完成条件。
5. 发布兼容：若 `transaction-face` 是外部模块依赖，必须给废弃周期、版本策略、迁移说明和回滚方式。
6. 风险确认：产品、架构、测试、清结算/对账 owner 确认删除不影响运营解释、历史对账、财务报表和用户账单。

## 6. 候选任务拆分

| Task ID | 目标 | 写入范围 | 验证候选 | 状态 |
| --- | --- | --- | --- | --- |
| `GSD2-TRX-CHARGEBACK-USAGE-SCAN-001` | 扫描旧 API 调用方和外部暴露面。 | 只读扫描报告、LWT Goal、OpenSpec tasks。 | `rg "chargeback\\(" transaction wallet ledger reconciliation governance tests docs openspec`，必要时补依赖树或接口暴露扫描。 | `SCAN_DONE_DOCS_ONLY` |
| `GSD2-TRX-CHARGEBACK-TEST-MIGRATION-001` | 把目标态争议资金结果测试迁移到 `settleRefund`，保留历史兼容回归。 | 授权交易测试、投影解释测试和任务状态。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-one FundsTransactionProjectionExplainApplicationServiceTests tests`、`just test-transaction`。 | `REMAINING_TESTS_TRIAGED_COMPAT_GUARD_RETAINED` |
| `GSD2-TRX-CHARGEBACK-PUBLIC-API-REMOVE-001` | 删除或隐藏 deprecated 公共 API。 | `transaction-face`、`transaction-impl`、测试和文档迁移。 | `just compile`、`just test-transaction`、`just test-boundary`、`just pmd`。 | `BLOCKED_BY_PUBLIC_CONTRACT_RELEASE_COMPAT` |

## 7. 停止条件

命中以下任一情况，立即停止并回到人工确认：

1. 发现外部模块、RPC、OpenAPI、SDK 或业务系统仍直接依赖 `chargeback`。
2. 删除会导致历史 `CHARGEBACK` 事实、投影解释、对账或归档不可解释。
3. 需要修改 DDL/H2 schema、交易 canonical 入参、事件枚举语义、route replay 语义或生产迁移。
4. 需要处理完整 dispute case、VCC processor、清结算追偿、准备金、商户负余额或外部规则。
5. 验证失败且无法在单一 Grant 范围内修复。

## 8. 当前结论

历史结论曾是 `chargeback` 公共 API 只能保持 deprecated 兼容、不进入物理删除；该结论已被 2026-06-26 目标态裁决覆盖。当前口径是：独立 `chargeback` 公共 API、请求模型、事件枚举和 route replay 分支进入移除，新增争议资金结果统一走 `settleRefund / AUTH_REFUND` 争议字段，无资金影响争议不调用交易写入链路。

## 9. 调用方扫描结果

`GSD2-TRX-CHARGEBACK-USAGE-SCAN-001` 已完成只读扫描。本节只记录扫描证据和下一步判断，不授权 Java、测试、DDL/H2 schema、公共契约、事件语义、route replay、投影解释、Controller、HTTP/RPC 或运行时配置变更。

执行范围：

- `rg "chargeback\\s*\\(" core wallet transaction ledger reconciliation governance tests docs openspec -S`
- `rg "import .*FundsAuthorizationTransactionChargebackRequest|new FundsAuthorizationTransactionChargebackRequest|\\.chargeback\\s*\\(" core wallet transaction ledger reconciliation governance tests -S`
- `rg --files | rg "(Controller|controller|web|api|rpc|Rpc|dubbo|Dubbo|feign|Feign|openapi|OpenApi)"`

扫描结论：

1. 生产源码调用方只集中在 `transaction` 内部：`FundsAuthorizationTransactionService#chargeback` 公共契约、`FundsTransactionCommandServiceImpl#chargeback` 实现、`FundsAuthorizationInstructionConverter#convertToChargebackInstruction` 转换，以及 route replay、ledger posting、lifecycle saver 和 projection explanation 对历史 `CHARGEBACK` 事实的兼容解释。
2. 未发现 `wallet`、`ledger`、`reconciliation`、`governance` 生产代码直接调用 `authorizationTransactionService.chargeback(...)`。
3. 未发现本仓库存在 Controller、HTTP/RPC、OpenAPI、Feign、Dubbo 或等价外部暴露文件；但 `transaction-face` 仍是跨模块公共 Java 契约，因此物理删除仍是破坏性契约变更。
4. 测试调用方仍存在：`FundsAuthorizationTransactionFlowTests` 覆盖兼容 chargeback 正向、缺外部争议引用失败、超额失败、幂等回放和冲突；`FundsTransactionProjectionExplainApplicationServiceTests` 覆盖历史兼容 chargeback 投影解释；`FundsTransactionRequestContextVariablesContractTests` 把请求类纳入请求上下文契约扫描。
5. `FundsTransactionProjectionExplanationSource`、`DefaultRouteReplayService`、`DefaultLedgerPostingAssembler`、`DefaultFundsInstructionLifecycleSaver` 和 core 枚举仍需要保留历史 `CHARGEBACK` 事实解释能力；删除公共 API 不等于删除历史事件、route replay 分支或投影解释标签。

后续建议：

1. `GSD2-TRX-CHARGEBACK-TEST-MIGRATION-001` 已完成首轮：目标态争议资金结果测试应优先迁移到 `settleRefund` 争议字段，同时保留一组历史兼容 chargeback guard / projection explain 回归。
2. 剩余直接调用旧入口的测试已裁决为兼容回归，不再作为目标态交易能力入口；不得据此执行 `GSD2-TRX-CHARGEBACK-PUBLIC-API-REMOVE-001`。
3. 即使测试迁移完成，物理删除仍需单独确认发布兼容、跨模块依赖影响和回滚策略。

## 10. 首轮测试迁移结果

`GSD2-TRX-CHARGEBACK-TEST-MIGRATION-001` 首轮只迁移目标态资金红线，不迁移历史兼容解释链路，不删除旧公共 API。

已完成：

1. `FundsAuthorizationTransactionFlowTests#testAuthorizationChargebackExceedingSettledAmountShouldLeaveNoSideEffects` 已改为 `testAuthorizationDisputeRefundExceedingSettledAmountShouldLeaveNoSideEffects`。
2. 该用例入口从旧 `authorizationTransactionService.chargeback(...)` 改为目标态 `authorizationTransactionService.settleRefund(...)`，并通过 `disputeMode=CHARGEBACK`、`disputeReason`、`disputeVoucherRef`、`externalDisputeRef` 表达争议裁决退款语义。
3. 资金红线保持不变：争议退款不得超过本交易已完成可回退金额，不得借用平台其他交易沉淀在 `SETTLEMENT` 的余额；失败后不得新增资金交易、route、posting、LedgerEntry 或余额变化。

保留并完成裁决：

1. 兼容 `chargeback` 正向回归继续保留，用于证明历史入口仍可解释；测试方法命名为 `testCompatChargebackShouldReplaySettlePathAndPreserveAuditContext`。
2. 缺少外部争议引用的兼容 guard 继续保留，用于防止历史入口缺最小审计证据；测试方法命名为 `testCompatChargebackMissingExternalDisputeRefShouldRejectAndLeaveNoSideEffects`。
3. 兼容 `chargeback` 幂等摘要冲突 guard 继续保留，用于证明历史入口不会因同业务流水不同请求重复入账；测试方法命名为 `testCompatChargebackSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects`。
4. 兼容 `chargeback` 投影解释回归继续保留，用于证明历史 `CHARGEBACK` 事实仍可查询、可投影、可审计；测试方法命名为 `testCompatChargebackShouldExplainAuditContextFromPersistedFacts`。

未完成：

1. 公共 API 删除、隐藏、改签、事件语义迁移和发布兼容仍未授权。
2. 如果后续要物理删除旧公共 API，仍需单独完成发布兼容影响、跨模块依赖和回滚策略确认。

## 11. 公共 API 删除 readiness 评估

`GSD2-TRX-CHARGEBACK-PUBLIC-API-REMOVE-READINESS-001` 已完成 design-only 评估。本节只给出是否可以进入物理删除的门禁判断，不授权 Java、测试、DDL/H2 schema、公共契约、事件语义、route replay、投影解释、Controller、HTTP/RPC、运行时配置或 Git 变更。

结论：历史 `DELETE_READINESS_NOT_READY` 已作废；当前状态为 `SUPERSEDED_BY_20260626_REMOVAL_DECISION`。

原因：

1. `transaction-face` 是跨模块 Java 公共契约。仓库内未发现 wallet、ledger、reconciliation 或 governance 直接调用旧入口，但这不能证明 fincone、fincone-issuing、nobe、SDK、RPC 适配层或其他外部依赖方已完成替代。
2. 历史 `CHARGEBACK` event、route replay、lifecycle saver、ledger posting assembler 和 projection explain 仍是历史事实解释链路的一部分。删除公共入口不应连带删除历史事件解释，也不能让历史资金事实不可查、不可投影或不可对账。
3. 旧 `chargeback` 方法当前同时承担兼容入口和历史回归锚点。若要移除 public face 方法，需要先明确内部兼容 adapter 或迁移方案，避免把历史兼容逻辑误删或误改成目标态 `settleRefund` 委派。
4. 当前没有版本策略、废弃窗口、迁移公告、下游影响清单和回滚策略。公共 API 删除属于破坏性契约变更，不能只凭本仓库测试绿色执行。

允许推进的下一步：

1. 删除本仓库内独立 public API、请求模型、事件枚举、route replay 分支和兼容测试。
2. 新增或保留争议资金结果测试时，统一使用 `settleRefund` 争议字段。
3. 若未来需要跨仓库兼容或历史数据只读解释，另起归档、迁移、对账或上层 dispute case 专项，不恢复交易内核 `chargeback` 主入口。

仍然禁止：

1. 把旧 public API 静默改成 `settleRefund` 委派后继续保留。
2. 借本任务顺手扩展完整 dispute case、representment、VCC processor、清结算追偿、准备金抵扣、商户负余额或外部规则。
3. 为了兼容旧入口恢复 `CHARGEBACK` 资金事件、route replay phase、投影解释标签或历史回归测试。

物理删除前最小验证清单：

1. 语义回归：目标态争议退款、普通退款、无授权退款和授权拒绝无副作用均通过。
2. 工程验证：`just compile`、`just test-transaction`、`just test-boundary`、`just pmd` 通过；必要时追加 projection explain 和业务流分组验证。
3. 发布材料、跨仓库依赖扫描和历史数据只读解释若成为真实发布要求，应由发布/迁移专项补齐。
