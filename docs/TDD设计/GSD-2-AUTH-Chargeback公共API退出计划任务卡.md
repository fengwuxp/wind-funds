# GSD-2 AUTH Chargeback 公共 API 退出计划任务卡

## 1. 文档定位

本文是 `GSD2-TRX-CHARGEBACK-PUBLIC-API-EXIT-PLAN-001` 的 design-only 任务卡，用于在旧 `chargeback` 公共入口已标注 deprecated 后，明确后续是否、何时、如何退出公共 API。

本文不删除 `FundsAuthorizationTransactionService#chargeback`，不删除 `FundsAuthorizationTransactionChargebackRequest`，不修改 `CHARGEBACK` event、route replay、投影解释、账务行为、DDL/H2 schema、Controller、HTTP/RPC 或运行时配置。公共 API 物理删除、改签或事件语义迁移必须另起单一 Execution Grant；测试迁移只能按本文候选任务单独推进，不得反推公共 API 已可删除。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-TRX-CHARGEBACK-PUBLIC-API-EXIT-PLAN-001` |
| 原子任务 | 明确 `chargeback` 历史公共 API 的退出策略、迁移门禁、调用方扫描、验证证据和停止条件。 |
| 所属阶段 | GSD-2 / transaction compatibility governance / design-only。 |
| 当前状态 | `EXIT_PLAN_LOCKED_DELETE_READINESS_NOT_READY` |
| Owner | 产品架构专家负责 dispute / chargeback 产品语义、使用者解释和运营边界；资深架构师负责公共契约兼容、迁移路径、测试门禁和删除风险；AI Native 负责任务状态、Loop 边界和交接。 |
| 写入范围 | 本文、LWT Goal、TDD README、docs README、OpenSpec spec 和 OpenSpec tasks 的状态同步。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、transaction-face、transaction-impl、route replay、projection explain 和授权交易测试。 |
| Git 策略 | `summary_only`；除非用户另行要求提交，本卡不等同 Git 授权。 |

## 2. 背景、目标和非目标

背景：当前设计已经确认 dispute / chargeback 是争议案件过程，不是授权交易层默认资金结果。交易层只消费裁决后的资金结果：需要退款时走 `settleRefund / AUTH_REFUND` 并保留争议原因、凭证、外部案件引用和审计；无资金影响时不调用资金交易入口。旧 `FundsAuthorizationTransactionService#chargeback` 和 `FundsAuthorizationTransactionChargebackRequest` 已标注 deprecated，只作为历史兼容入口保留。

目标：把“旧入口退役治理”从代码实现中拆出来，形成一组可执行、可审计、可停止的退出条件。后续团队在决定删除、改签、隐藏或迁移旧公共 API 前，必须能证明迁移对象、替代入口、兼容窗口、调用方影响、测试回归和发布回滚均已准备好。

非目标：

1. 不把 `chargeback` 扩展成新的交易能力或完整 dispute case。
2. 不在本轮物理删除公共方法、请求类、事件枚举或 route replay 分支。
3. 不把旧入口直接改成 `settleRefund` 委派实现。
4. 不处理 representment、arbitration、准备金、追偿、商户负余额、完整清结算或 VCC processor 专项。
5. 不替代外部规则、法务、合规、财务、通道或卡组织最终确认。

## 3. 产品语义裁决

| 对象 | 目标态 | 兼容态 | 退出判断 |
| --- | --- | --- | --- |
| 争议案件 | 上层业务、运营或通道适配层管理案件、证据、裁决和责任。 | 资金底座只保留必要外部案件引用和审计上下文。 | 案件过程不得因旧 API 删除而丢失只读审计和使用者解释。 |
| 争议资金结果 | `settleRefund / AUTH_REFUND` 携带争议字段承接。 | 旧 `chargeback` 仍可解释历史显式事件。 | 新业务入口、文档和测试均不再要求调用 `chargeback`。 |
| 无资金影响争议 | 不调用交易写入链路。 | 可由上层案件系统、运营台或投影解释记录。 | 不能为了兼容旧 API 而生成资金交易、route、posting、LedgerEntry 或余额变化。 |
| 历史 `CHARGEBACK` 事实 | 作为历史资金事实、显式事件或投影解释标签保留。 | 仍参与历史查询、对账、归档和测试回归。 | 删除公共 API 不等于删除历史事件或历史数据解释。 |

## 4. 推荐退出策略

推荐策略是 `STAGED_PUBLIC_API_EXIT_WITH_USAGE_GUARD`。

含义：先锁定目标语义和 deprecated 标记，再完成内部调用方替代、外部调用方扫描、测试迁移和发布兼容窗口。只有在调用方清单为空或已完成替代、历史事件解释仍可回归、发布回滚方案明确后，才允许另起删除或隐藏公共 API 的 Execution Grant。

| 阶段 | 目标 | 允许动作 | 禁止动作 |
| --- | --- | --- | --- |
| Phase 0 已完成 | 目标语义对齐，旧入口标注 deprecated。 | 文档、OpenSpec、JavaDoc、deprecated 和最小 guard。 | 删除 API、改事件语义、迁移到 `settleRefund` 委派。 |
| Phase 1 已完成 | 调用方扫描和替代入口证明。 | 扫描生产源码、测试、文档、OpenAPI 或 RPC 暴露面；列调用方和替代路径。 | 未列调用方就删除公共 API。 |
| Phase 2 首轮已执行 | 测试和投影迁移。 | 把新场景测试迁移到 `settleRefund` 争议字段，保留历史 `CHARGEBACK` 解释回归。 | 删除历史事件解释、让旧事实不可查。 |
| Phase 3 待确认 | 公共 API 删除或隐藏。 | 在单独 Grant 中删除、隐藏或移动旧 API，并给迁移说明和回滚策略。 | 和 VCC、清结算、Spend Rule、ledger 或 wallet 切片混做。 |

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

`chargeback` 公共 API 当前只能保持 deprecated 兼容，不进入物理删除。调用方扫描已完成，首轮测试迁移已把一个目标态争议资金结果红线用例迁移到 `settleRefund` 争议字段；剩余直接调用旧入口的测试已裁决为历史兼容回归，并通过 `CompatChargeback` 命名显式区分目标态争议退款。本轮已完成公共 API 删除 readiness 评估，结论为 `DELETE_READINESS_NOT_READY`：在发布兼容、跨模块 / 外部依赖影响、版本废弃窗口和回滚策略完成前，不得删除、隐藏或移动公共 API。

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

结论：`DELETE_READINESS_NOT_READY`。

原因：

1. `transaction-face` 是跨模块 Java 公共契约。仓库内未发现 wallet、ledger、reconciliation 或 governance 直接调用旧入口，但这不能证明 fincone、fincone-issuing、nobe、SDK、RPC 适配层或其他外部依赖方已完成替代。
2. 历史 `CHARGEBACK` event、route replay、lifecycle saver、ledger posting assembler 和 projection explain 仍是历史事实解释链路的一部分。删除公共入口不应连带删除历史事件解释，也不能让历史资金事实不可查、不可投影或不可对账。
3. 旧 `chargeback` 方法当前同时承担兼容入口和历史回归锚点。若要移除 public face 方法，需要先明确内部兼容 adapter 或迁移方案，避免把历史兼容逻辑误删或误改成目标态 `settleRefund` 委派。
4. 当前没有版本策略、废弃窗口、迁移公告、下游影响清单和回滚策略。公共 API 删除属于破坏性契约变更，不能只凭本仓库测试绿色执行。

允许推进的下一步：

1. 维持 public API deprecated 兼容态，继续把新增争议资金结果场景写到 `settleRefund` 争议字段。
2. 若用户明确要继续处理删除准备，优先执行 `GSD2-TRX-CHARGEBACK-PUBLIC-API-DEPENDENCY-SCAN-001`：扫描 fincone、fincone-issuing、nobe、可能的 SDK、RPC/HTTP 适配层和发布文档，输出调用方 owner、替代状态和迁移风险。
3. 若依赖扫描清零，再进入 `GSD2-TRX-CHARGEBACK-PUBLIC-API-REMOVE-GRANT-001`，在单一 Grant 中定义废弃版本、迁移说明、回滚方案、内部兼容 adapter 保留方式和完整回归命令。

仍然禁止：

1. 直接删除 `FundsAuthorizationTransactionService#chargeback` 或 `FundsAuthorizationTransactionChargebackRequest`。
2. 删除 `CHARGEBACK` 历史 event、route replay phase、投影解释标签或历史回归测试。
3. 把旧 public API 静默改成 `settleRefund` 委派，导致幂等摘要、事件类型、投影解释或历史审计语义变化。
4. 借本任务顺手扩展完整 dispute case、representment、VCC processor、清结算追偿、准备金抵扣、商户负余额或外部规则。

物理删除前最小验证清单：

1. 跨仓库依赖扫描：`wind-funds`、`fincone`、`fincone-issuing`、`nobe` 和已知接入包均无未迁移调用。
2. 语义回归：目标态争议退款、普通退款、无授权退款、兼容 `CHARGEBACK` 历史解释和授权拒绝无副作用均通过。
3. 工程验证：`just compile`、`just test-transaction`、`just test-boundary`、`just pmd` 通过；必要时追加 projection explain 和业务流分组验证。
4. 发布材料：迁移说明、废弃窗口、版本策略、回滚步骤和风险 owner 已确认。
