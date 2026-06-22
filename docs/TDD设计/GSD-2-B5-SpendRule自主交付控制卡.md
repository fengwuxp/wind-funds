# GSD-2 B5 Spend Rule 自主交付控制卡

## 1. 控制卡定位

本卡用于承接 `GSD2-B5-SPEND-RULE-SPEC-LOCK-001` 的 docs-only 推进：在进入下一轮编码前，先把 Spend Rule 的产品口径、系分边界和 DSL v1.1 规格锁定到可评审、可拆任务、可写测试的程度。

本卡最初不授权 Java、测试、DDL/H2 schema、Entity、Mapper、Controller、HTTP/RPC、交易 canonical 入参、ledger posting、事件消费、生产迁移或 Git 操作。

## 2. 自主交付控制卡

| 控制项 | 本轮边界 |
| --- | --- |
| 可自我挖掘 | 读取并对齐 Spend Rule 独立 PRD、系分分册、DSL README、TDD README、OpenSpec tasks 和上一轮 CR 结论。 |
| 可自我规划 | 将 CR 结论拆成规则版本 JSON、规则挂载 JSON、决策证据 JSON、场景验证、可解释性和任务基线同步。 |
| 可自动执行 | 修改产品设计、系分设计、DSL 设计、TDD 索引和 OpenSpec 任务记录；补充 docs-only 验证证据。 |
| 必须人工确认 | 进入编码、修改公共 Java 契约、修改数据库表结构、引入规则引擎、接入控制活动新模型、处理 `SpendControlActivity` 重构、修改交易 canonical 入参、让 Spend Rule 进入 ledger posting、提交 Git。 |
| 置信度 | 产品、系分、DSL docs-only 规格锁定为高；生产可用为中，需要后续服务层真实链路、H2 schema、目标测试和验证命令证明。 |

## 3. 本轮 CR 消费结论

| CR 结论 | 处理方式 |
| --- | --- |
| Spend Rule 应独立产品和系分分册。 | 已使用 `09-SpendRule支出规则产品设计.md` 和 `06-SpendRule支出规则系分设计.md` 作为权威分册。 |
| 编码前必须确认 Spend Rule spec。 | 本轮锁定 DSL v1.1，先定义规则版本、挂载和决策证据三个契约。 |
| JSON 需要能覆盖多规则、周期限额、次数限额、商户 / MCC / 国家 / 时间窗口等通用实践。 | DSL v1.1 引入 `matchSpec`、`counterSpec`、`limitSpec`、`decisionSpec`、`safetySpec` 和 `evaluatedRules`。 |
| 可读性和可解释性不足会影响客服、运营、风控和审计。 | 规则版本增加 `display`，决策证据增加 `decisionReasonCode`、`decisionReasonMessage`、`matchedFacts`、`missingEvidence` 和摘要字段。 |
| `SpendControlActivity` 设计待定。 | 本轮只把它作为既有控制事实消费方，不改模型、不扩展行为、不作为规则定义来源。 |

## 4. DSL v1.1 规格锁定

Spend Rule DSL v1.1 只锁三类契约：

1. `SpendRuleVersionSpec`：不可变规则版本正文，回答“这条规则怎么算”。
2. `SpendRuleAssignmentSpec`：规则挂载正文，回答“这条规则作用到谁、优先级和冲突策略是什么”。
3. `SpendRuleDecisionEvidenceSpec`：决策证据正文，回答“这次请求评估了哪些规则、最终为什么通过或拒绝”。

字段分组：

| 分组 | 作用 | 必填口径 |
| --- | --- | --- |
| `display` | 给运营、客服和审计阅读的名称、说明和展示原因。 | 规则版本必填；不能包含敏感原文。 |
| `matchSpec` | 描述请求事实匹配条件，例如金额、币种、MCC、商户、国家、时间、支付工具、预算 scope、业务场景。 | 规则版本必填；缺字段默认不匹配或进入 `decisionWhenEvidenceMissing`。 |
| `counterSpec` | 描述周期窗口、计数口径、累计依据和退款净额策略。 | 周期金额、周期次数类规则必填；单笔、商户、国家和时间窗口类可为空。 |
| `limitSpec` | 描述金额上限、次数上限或允许 / 拒绝集合。 | 限额和集合控制类规则必填。 |
| `decisionSpec` | 描述通过、违反、证据缺失时的裁决。 | 必填；不得缺省放行。 |
| `safetySpec` | 描述未知字段、敏感字段、摘要和历史回放策略。 | 必填；默认 fail closed。 |
| `evaluatedRules` | 记录一次请求评估过的规则列表。 | 决策证据必填；多规则命中时必须能解释最终裁决。 |

## 5. 场景验证矩阵

| 场景 | DSL v1.1 是否覆盖 | 关键证据 |
| --- | --- | --- |
| VCC 单卡单笔限额 | 覆盖。 | `matchSpec.paymentInstrumentTypes`、`assignment.scopeRef`、`limitSpec.amountLimit`、`decisionSpec.decisionWhenViolated`。 |
| 企业预算周期金额上限 | 覆盖。 | `matchSpec.budgetScopeRef`、`counterSpec.window`、`counterSpec.aggregationBasis`、`limitSpec.amountLimit`。 |
| 员工卡日交易次数 | 覆盖。 | `counterSpec.window`、`limitSpec.countLimit`、`evaluatedRules.remainingCount`。 |
| MCC 或商户拒绝 | 覆盖。 | `matchSpec.merchantCategory`、`matchSpec.merchantRefDigest`、`decisionReasonCode`。 |
| 国家或地区限制 | 覆盖。 | `matchSpec.countryRegion`、`decisionSpec`。 |
| 时间窗口限制 | 覆盖。 | `matchSpec.timeWindow`、`counterSpec.timezone`。 |
| 多规则同时命中 | 覆盖。 | `assignment.priority`、`conflictPolicy`、`decisionPolicy`、`evaluatedRules`。 |
| 授权拒绝无资金副作用 | 需要后续测试证明。 | DSL 只提供 `finalDecision=DECLINE` 和 forbidden facts，不能替代服务层断言。 |
| 交易后控制活动消费 | 不在本轮扩展。 | 后续由 `SpendControlActivity` 或交易消费服务任务承接。 |

## 6. 验收口径

| AC ID | 验收点 | 当前结论 |
| --- | --- | --- |
| AC-SR-SPEC-001 | PRD 明确 DSL v1.1 的产品语义、使用者和禁止误用。 | docs-only 可验收。 |
| AC-SR-SPEC-002 | 系分明确 DSL v1.1 的应用服务边界、持久化映射和失败无副作用。 | docs-only 可验收。 |
| AC-SR-SPEC-003 | DSL README 提供规则版本、挂载和决策证据 JSON 示例。 | docs-only 可验收。 |
| AC-SR-SPEC-004 | 多规则裁决有 `evaluatedRules`、`decisionPolicy` 和最终裁决证据。 | docs-only 可验收。 |
| AC-SR-SPEC-005 | 敏感数据、未知字段、证据缺失和历史重放有 fail-closed 口径。 | docs-only 可验收。 |
| AC-SR-SPEC-006 | 明确后续编码必须重新确认单一 Grant。 | docs-only 可验收。 |

## 7. 后续单一 Grant 候选

| 候选 Grant | 适用条件 | 禁止复用 |
| --- | --- | --- |
| `GSD2-B5-SPEND-RULE-DECISION-CONSUME-001` | 将规则决策证据接入钱包准入或支付工具生命周期入口。 | 不处理规则引擎、DDL 迁移、Controller、HTTP/RPC、ledger posting。 |
| `GSD2-B5-SPEND-RULE-PROJECTION-EXPLAIN-001` | 将决策证据和控制活动纳入交易投影解释。 | 不重算历史规则，不反写交易、route、posting、entry 或余额。 |
| `GSD2-B5-SPEND-RULE-SERVICE-HARDEN-001` | 硬化规则定义、版本、挂载和决策日志服务层。 | 不扩展 `SpendControlActivity` 待定设计，不做运营后台。 |

## 8. 本轮验证

本轮只做文档和任务基线变更。完成后应执行：

1. `git diff --check`
2. `rg -n "SPEND-RULE-SPEC-LOCK|SpendRuleVersionSpec|SpendRuleDecisionEvidenceSpec|evaluatedRules|decisionPolicy" docs openspec`

不运行 `just compile`、`just test-*` 或 `just pmd`，原因是本轮不修改 Java、测试、DDL/H2 schema、构建配置或运行时配置。

## 9. `GSD2-B5-SPEND-RULE-DECISION-CONSUME-001` 消费记录

用户后续确认 `GSD2-B5-SPEND-RULE-DECISION-CONSUME-001` 后，本卡进入编码消费态。本轮只把已存在的 Spend Rule 决策日志契约接入支出控制准入和支付工具授权准入，不扩展规则引擎、控制活动、交易 canonical 入参、ledger posting、Controller、HTTP/RPC、事件消费、生产迁移或 VCC facade。

| 控制项 | 本轮结果 |
| --- | --- |
| 可自我挖掘 | 已读取 Spend Rule 定义契约、支出控制准入、支付工具授权准入、目标服务流测试和 H2 schema。 |
| 可自我规划 | 将 `DECISION-CONSUME` 收敛为“准入服务记录 `SpendRuleDecisionLog`，授权门面透传 assignment / scope / decision evidence”。 |
| 自动执行范围 | `wallet-face` 请求 / DTO、`wallet-impl` 支出控制准入和授权准入组合、目标服务层测试、TDD / OpenSpec 状态回写。 |
| 明确禁止范围 | 不处理规则引擎、运营后台、控制活动重构、交易 canonical 入参、ledger posting、Controller、HTTP/RPC、事件消费、生产迁移、VCC facade 或 Git push。 |
| 验证证据 | 沙箱内目标测试因 embedded Redis 端口绑定被拒；沙箱外复跑 `just test-one SpendControlAdmissionApplicationServiceTests,AuthorizationAdmissionApplicationServiceTests tests`，9 tests 通过。 |
| 当前状态 | `SPEND_RULE_DECISION_CONSUME_GREEN_VERIFIED`。 |

## 10. `GSD2-B5-SPEND-RULE-PROJECTION-EXPLAIN-001` 消费记录

用户后续确认 `GSD2-B5-SPEND-RULE-PROJECTION-EXPLAIN-001` 后，本卡进入投影解释消费态。本轮只把授权准入通过时已固化的 Spend Rule 决策快照带入交易上下文，并由交易投影解释服务只读生成规则证据解释；不在解释阶段执行规则 DSL、脚本、风控模型或外部服务。

| 控制项 | 本轮结果 |
| --- | --- |
| 规则解释方案 | 采用自解释已固化事实，不引入 `wind-script`。`wind-script` 仅作为未来规则执行器或规则校验器候选，需另起独立 Grant。 |
| 可自我挖掘 | 已读取交易投影解释、支付工具授权准入、支出控制准入、Spend Rule 决策日志、PRD、系分、DSL 和 OpenSpec 任务基线。 |
| 可自我规划 | 将 `PROJECTION-EXPLAIN` 收敛为“准入通过后固化 `spendRuleDecision` 最小快照，投影解释 allow-list 读取并输出 evidence refs”。 |
| 自动执行范围 | `transaction-face` 上下文 key 与投影解释 source、`wallet-impl` 授权准入编排、目标服务流测试、PRD / 系分 / DSL / TDD / OpenSpec 状态回写。 |
| 明确禁止范围 | 不处理完整规则引擎、`wind-script` 接入、运营后台、控制活动重构、交易 canonical 入参、ledger posting、Controller、HTTP/RPC、事件消费、生产迁移、VCC facade 或 Git push。 |
| 验证证据 | 沙箱内目标测试因 embedded Redis 端口绑定被拒；沙箱外先复现 Red，再复跑 `just test-one AuthorizationAdmissionApplicationServiceTests tests`，5 tests 通过。 |
| 当前状态 | `SPEND_RULE_PROJECTION_EXPLAIN_GREEN_VERIFIED`。 |

## 11. `GSD2-B5-SPEND-RULE-MODULE-OWNERSHIP-GUARD-001` 消费记录

用户确认“Spend Rule 主能力放在 wallet，transaction 只消费已固化快照”后，本卡进入模块归属守卫态。本轮只固化产品、系分、DSL 和测试边界，不重构既有服务，不新增规则引擎，不调整交易 canonical 入参。

| 控制项 | 本轮结果 |
| --- | --- |
| 产品结论 | Spend Rule 规则定义、版本、挂载、决策日志、准入、控制活动和预算控制视图归属于 `wallet` 支出控制域。 |
| 工程结论 | `transaction` 只消费 `spendRuleDecision` 等已固化证据做历史解释，不直接依赖 wallet Spend Rule 主服务、Entity 或 Mapper。 |
| 自动执行范围 | PRD / 系分 / DSL / TDD / OpenSpec 口径回写，新增边界测试守卫 `transaction-*` 生产源码依赖方向。 |
| 明确禁止范围 | 不处理完整规则引擎、`wind-script` 接入、运营后台、控制活动重构、交易 canonical 入参、ledger posting、Controller、HTTP/RPC、事件消费、生产迁移、VCC facade 或 Git push。 |
| 验证证据 | `just test-one FundsModuleDependencyBoundaryTests tests` 通过 6 tests；`just test-boundary` 通过 174 tests；`git diff --check` 通过。 |
| 当前状态 | `SPEND_RULE_MODULE_OWNERSHIP_GUARD_GREEN_VERIFIED`。 |

## 12. `GSD2-B5-SPEND-RULE-SERVICE-HARDEN-001` 消费记录

用户要求使用角色自主推进控制后，本卡进入服务层硬化回收态。本轮只补 Spend Rule 决策日志的支付工具范围一致性校验，不扩展完整规则引擎、控制活动设计、交易 canonical 入参、ledger posting、Controller、HTTP/RPC、事件消费、生产迁移或 VCC facade。

| 控制项 | 本轮结果 |
| --- | --- |
| 可自我挖掘 | 已读取 Spend Rule 定义服务、请求 / DTO、Entity、H2 schema、定义契约测试、支出控制准入测试和授权准入测试。 |
| 可自我规划 | 将 `SERVICE-HARDEN` 收敛为“`PAYMENT_INSTRUMENT` scope 的决策日志必须携带且匹配 `instrumentSn`”。 |
| 自动执行范围 | `SpendRuleDefinitionApplicationServiceImpl` 入参一致性校验、`SpendRuleDefinitionApplicationServiceTests` 目标 Red/Green、TDD / OpenSpec 状态回写。 |
| 明确禁止范围 | 不处理完整规则引擎、`wind-script` 接入、运营后台、控制活动重构、交易 canonical 入参、ledger posting、Controller、HTTP/RPC、事件消费、生产迁移、VCC facade 或 Git push。 |
| 验证证据 | 沙箱内目标测试因 embedded Redis 端口绑定被拒；沙箱外先复现 Red，再复跑 `just test-one SpendRuleDefinitionApplicationServiceTests tests` 通过 4 tests；`just test-one SpendRuleDefinitionApplicationServiceTests,SpendControlAdmissionApplicationServiceTests,AuthorizationAdmissionApplicationServiceTests tests` 通过 14 tests。 |
| 当前状态 | `SPEND_RULE_SERVICE_HARDEN_GREEN_VERIFIED`。 |

## 13. `GSD2-B5-SPEND-RULE-SERVICE-HARDEN-002` 消费记录

用户继续要求使用角色自主推进控制后，本卡进入服务层硬化第二轮。本轮只补 Spend Rule 决策日志的结果一致性校验：`PASSED` 决策不得携带拒绝原因，避免投影解释、客服审计和对账报告出现“通过但带拒绝原因”的矛盾证据。

| 控制项 | 本轮结果 |
| --- | --- |
| 可自我挖掘 | 已读取 Spend Rule 定义服务、决策日志请求 / Entity、独立 PRD、系分、DSL 和上一轮服务层硬化测试。 |
| 可自我规划 | 将 `SERVICE-HARDEN-002` 收敛为“非拒绝决策不得携带 `rejectReason`”，不改变请求字段兼容语义。 |
| 自动执行范围 | `SpendRuleDefinitionApplicationServiceImpl` 决策日志前置校验、`SpendRuleDefinitionApplicationServiceTests` 目标 Red/Green、TDD / OpenSpec 状态回写。 |
| 明确禁止范围 | 不处理 `assignmentSn` 强必填公共契约收紧、完整规则引擎、`wind-script` 接入、运营后台、控制活动重构、交易 canonical 入参、ledger posting、Controller、HTTP/RPC、事件消费、生产迁移、VCC facade 或 Git push。 |
| 验证证据 | 沙箱内目标测试因 embedded Redis 端口绑定被拒；沙箱外先复现 Red，再复跑 `just test-one SpendRuleDefinitionApplicationServiceTests tests` 通过 5 tests；`just test-one SpendRuleDefinitionApplicationServiceTests,SpendControlAdmissionApplicationServiceTests,AuthorizationAdmissionApplicationServiceTests tests` 通过 15 tests。 |
| 当前状态 | `SPEND_RULE_SERVICE_HARDEN_002_GREEN_VERIFIED`。 |
