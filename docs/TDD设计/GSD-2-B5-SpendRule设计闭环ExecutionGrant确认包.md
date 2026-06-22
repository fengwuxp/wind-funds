# GSD-2 B5 Spend Rule 设计闭环 Execution Grant 确认包

## 1. 文档定位

本文是 `GSD2-B5-SPEND-RULE-DESIGN-CLOSURE-001` 的角色 Loop 任务卡，用于把本轮 Spend Rule CR 结论落成可交接的产品、系分、DSL 和 TDD 设计闭环。

本文只授权低风险文档、任务和状态回写；不授权 Java、测试、DDL/H2 schema、Entity、Mapper、Controller、HTTP/RPC、公共契约、运行时配置或 Git 操作。`SpendControlActivity` 设计暂不继续推进，本轮不删除、不重写、不扩展既有控制活动实现。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B5-SPEND-RULE-DESIGN-CLOSURE-001` |
| 原子任务 | 完成 Spend Rule 规则定义、版本、挂载、决策日志和准入证据的设计闭环。 |
| 所属阶段 | GSD-2 / B5 Spend Rule / design closure / planning-only。 |
| Goal ID | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| Loop ID | `GSD2-LWT-PRODUCTION-CAPABILITY-LOOP-2026-06-18` |
| 当前状态 | `DESIGN_CLOSURE_READY_NOT_CODE_AUTHORIZED` |
| Owner | 产品架构专家负责规则语义、场景和验收；资深架构师负责模块边界、对象结构、测试准入和停止条件；AI Native 流程编排负责状态回写。 |
| 写入范围 | PRD、DSL、系分、TDD 任务卡、TDD README、LWT Goal、P0/P1 推进计划、docs README 和 OpenSpec tasks。 |
| 禁止范围 | `SpendControlActivity` 扩展、交易消费控制活动、完整规则引擎实现、Java、测试、DDL/H2、Entity、Mapper、Controller、HTTP/RPC、交易 canonical 入参、ledger posting、VCC facade、事件消费 / outbox、生产迁移。 |
| Git 策略 | `summary_only`；本任务不包含提交授权。 |

## 2. 角色 Loop 裁决

| 角色 | 裁决 | 落地动作 |
| --- | --- | --- |
| 产品架构专家 | Spend Rule 不是资金账户、信用账户、预算组或支付工具的替代品；它是针对工具、预算 scope、账户或业务场景的支出规则配置和准入决策能力。 | PRD 只保留定义、版本、挂载、决策日志、只读投影和生产可用阻断，不保留过程争论。 |
| 资深架构师 | 规则闭环应先补 `SpendRuleDefinition`、`SpendRuleVersion`、`SpendRuleAssignment`、`SpendRuleDecisionLog` 四层；`SpendControlActivity` 保留为既有控制事实能力，不作为本轮写入目标。 | 系分补四层对象、字段、边界、命名口径和后续编码准入；DSL 明确历史交易不得按当前规则重新解释。 |
| AI Native 流程编排 | 本轮属于产研交付视图的 docs-only Plan Grant，不进入 CAD 编码。 | TDD、Goal 和 OpenSpec 记录任务、禁止范围、恢复入口和验证命令。 |

## 2.1 现状、约束和影响范围

现状：wallet 已具备支付工具能力准入、账户能力来源、资金责任解析、预交易快照、支出控制准入快照、既有控制活动和交易结果控制活动的首轮服务层证据；但这些证据仍不等于完整 Spend Rule 规则生产能力。当前问题是规则定义、规则版本、规则挂载、决策日志、优先级冲突和规则变更审计尚未形成闭环，导致历史解释、运营配置、规则拒绝追溯和生产审计仍缺少权威对象。

约束：预算组、支付工具和 Spend Rule 都不能成为账务主体；规则拒绝、规则命中和控制活动都不能直接生成资金交易、route、posting、LedgerEntry、账本交易或余额投影。影响范围限于产品设计、系分设计、DSL 约定、TDD 任务、OpenSpec 状态和后续编码 Grant 准入，不影响当前 Java 运行时代码。

核心决策：

1. Spend Rule 先建“规则定义、不可变版本、规则挂载、决策日志”四层，不把既有 `SpendControlActivity` 继续扩成规则引擎。
2. 规则定义和规则版本解决“规则是什么、当时是什么版本”；规则挂载解决“规则应用到谁、优先级是什么”；决策日志解决“这次请求为什么通过、拒绝或复核”。
3. 交易投影只能读取当时固化的版本、挂载和决策流水，不得按当前规则定义、当前规则挂载或当前工具绑定重算历史。
4. 后续编码必须单一 Grant 推进，先从契约和 Red 测试证明规则版本不可原地覆盖、规则挂载不输出资金责任主体、规则拒绝无资金事实副作用。

## 3. 产品语义

业务目标：让 VCC 授权、支付工具付款、预算控制、员工卡/项目卡和商户/国家/MCC 限制等场景，有一套可配置、可版本化、可挂载、可审计、可回放解释的 Spend Rule 规则闭环。

用户价值：

1. 产品和运营能配置“哪些主体、卡、账户、预算 scope 或业务场景适用哪些规则”。
2. 风控和客服能解释一笔授权为什么通过、拒绝或进入人工复核。
3. 财务和审计能追踪规则版本、挂载版本、决策流水、拒绝原因和请求摘要。
4. 研发能把规则决策作为授权前准入输入，而不是让规则对象进入交易内核或账本事实。

非目标：

1. 不实现完整规则引擎、表达式解析器、运营后台或审批流。
2. 不继续扩展 `SpendControlActivity`、预算控制投影或交易消费控制活动。
3. 不新增资金交易、route、posting、LedgerEntry、账本交易或余额投影事实。
4. 不改变直接交易、授权交易、余额控制的账户主体 canonical 入参。

## 4. 对象模型

| 对象 | 产品含义 | 最小字段 | 边界 |
| --- | --- | --- | --- |
| `SpendRuleDefinition` | 规则定义，描述规则是什么、归谁管理、用于哪个规则域。 | `ruleId`、`ruleCode`、`ruleName`、`ruleType`、`ruleDomain`、`ownerType`、`ownerId`、`status`。 | 不保存实时决策结果，不直接绑定卡或账户。 |
| `SpendRuleVersion` | 不可变规则版本，描述条件、额度、动作和生效窗口。 | `ruleId`、`version`、`conditionSpec`、`limitSpec`、`actionSpec`、`effectiveFrom`、`effectiveTo`、`versionDigest`。 | 发布后不得原地覆盖；历史交易只按当时版本解释。 |
| `SpendRuleAssignment` | 规则挂载，描述某个版本应用到哪个 scope 以及如何排序。 | `assignmentId`、`ruleId`、`version`、`scopeType`、`scopeId`、`priority`、`conflictPolicy`、`status`。 | 不作为资金责任主体，不生成账本分录。 |
| `SpendRuleDecisionLog` | 规则决策日志，描述一次请求命中了什么规则和结果。 | `decisionSn`、`assignmentId`、`ruleId`、`version`、`decisionResult`、`rejectReason`、`requestDigest`、`decisionDigest`、`evaluatedAt`。 | 不创建交易事实，不替代控制活动，不反写 route、posting 或 ledger。 |

字段命名口径：

1. `ruleId` 或历史 `sn` 表示系统内稳定标识，承担幂等、引用和回放。
2. `ruleCode` 表示业务可读或运营配置编码，可修改时必须保留审计。
3. 不在本轮把资金账户、信用账户、预算组或支付工具表的既有 `sn` 统一改名为 `code`。

## 5. 能力地图

| 能力 | 输入 | 输出 | 验收口径 |
| --- | --- | --- | --- |
| 规则定义管理 | 规则名称、类型、归属和规则域。 | 可引用的规则定义。 | 同一租户内稳定标识唯一；停用规则不能新挂载。 |
| 规则版本发布 | 条件、额度、动作、生效窗口和摘要。 | 不可变规则版本。 | 已发布版本不能被覆盖；新版本不影响历史交易解释。 |
| 规则挂载 | scope、优先级、冲突策略和版本。 | 可评估的规则挂载关系。 | 同一 scope 多规则冲突必须按 priority 和 conflictPolicy 可解释。 |
| 规则评估决策 | 支付工具快照、账户主体、金额、币种、商户、MCC、国家、时间和业务场景。 | 决策结果、拒绝原因、决策摘要和决策流水。 | 拒绝或证据不完整必须停在交易内核前，且无 route、posting、LedgerEntry 或余额副作用。 |
| 决策日志查询 | 决策流水、规则版本、scope、业务流水和时间窗口。 | 只读决策时间线。 | 查询不修复状态、不推进交易、不重新计算历史规则。 |

## 5.1 业务流程、状态和规则矩阵

主流程：

1. 运营创建 `SpendRuleDefinition`，明确规则域、规则类型、规则归属和业务可读 `ruleCode`。
2. 运营发布 `SpendRuleVersion`，固化条件、额度、动作、生效窗口和 `versionDigest`。
3. 运营把某个版本通过 `SpendRuleAssignment` 挂载到支付工具、预算 scope、账户、账户层级或业务场景，并设置优先级和冲突策略。
4. 授权、付款或收款前，准入服务读取支付工具快照、账户主体、金额、币种、商户、MCC、国家、时间和业务场景，形成 `SpendRuleDecisionLog`。
5. 决策通过时继续委派账户主体型交易内核；决策拒绝时停在交易内核前，只留下拒绝决策日志和审计证据。
6. 交易投影或客服查询只读取当时版本、挂载和决策流水，解释历史规则命中，不重新评估当前规则。

异常流程和人工兜底：

1. 规则版本未发布、挂载已停用、版本过期或冲突策略无法裁决时，准入结果应为拒绝或待复核，不得默认放行。
2. 决策证据不完整、请求摘要缺失或规则评估超时，必须停在交易内核前，并生成可审计失败原因。
3. 需要人工复核时，输出复核状态和引用，不自动生成资金事实；后续复核放行必须形成新的决策流水。

规则矩阵：

| 触发条件 | 判断逻辑 | 优先级 | 结果 | 审计要求 |
| --- | --- | --- | --- | --- |
| 单次金额超过规则版本额度 | 按 scope 和币种匹配最高优先级有效挂载。 | `priority` 越高越先裁决；冲突按 `conflictPolicy`。 | 拒绝或待复核。 | 记录规则版本、挂载、拒绝原因和请求摘要。 |
| MCC、商户、国家或时间窗口不满足 | 按当时发布版本的 `conditionSpec` 判断。 | 明确 allow-list / deny-list 冲突策略。 | 拒绝。 | 不生成交易事实或账务事实。 |
| 多个规则同时命中 | 按优先级和冲突策略合成。 | 无策略时不得默认放行。 | 通过、拒绝或待复核。 | 决策日志记录参与裁决的版本和最终原因。 |
| 历史交易投影解释 | 只读取当时版本、挂载和决策流水。 | 不读取当前规则。 | 只读解释。 | 不反写 route、posting、entry 或 balance。 |

## 5.2 接口契约、数据方案和一致性

接口契约只作为后续编码候选，不在本轮写 Java：

| 候选接口 | 入参 | 出参 | 错误码和幂等 | 兼容要求 |
| --- | --- | --- | --- | --- |
| `SpendRuleDefinitionApplicationService` | 规则定义、版本发布请求、租户和操作者。 | 规则定义、版本、发布结果。 | `ruleId + version` 幂等；已发布版本禁止覆盖。 | 不复用 `SpendControlActivity` 作为规则定义表。 |
| `SpendRuleAssignmentApplicationService` | scope、ruleId、version、priority、conflictPolicy。 | 挂载结果和挂载版本。 | `assignmentId` 幂等；冲突策略缺失时报错。 | scope 不能输出资金责任主体。 |
| `SpendRuleDecisionApplicationService` | 支付工具快照、账户主体、金额、币种、商户、MCC、国家、时间和业务场景。 | 决策流水、结果、拒绝原因、摘要。 | `decisionSn` 或请求摘要幂等；摘要冲突拒绝。 | 拒绝路径必须停在交易内核前。 |

数据方案：

1. 规则定义、规则版本、规则挂载和决策日志应有独立数据边界；决策日志不可反写规则版本或挂载。
2. 已发布版本不可更新正文，只能发布新版本；历史交易和历史投影继续引用旧版本。
3. 规则决策和账户主体型交易之间的事务边界应以“先决策、后委派”为准；规则拒绝不进入交易事务。
4. 如后续决策通过后交易失败，补偿只发生在交易或控制事实层，不回滚规则定义和版本。
5. 对账只校验规则决策证据、交易事实和控制活动之间的引用一致性，不把 Spend Rule 当资金余额来源。

可靠性和安全：

1. 规则评估失败默认不放行；缺失版本、缺失挂载、冲突策略不明或摘要不一致均应 fail-fast。
2. 规则条件、请求摘要和决策摘要需脱敏保存，避免泄露卡号、商户敏感原文或外部账户敏感字段。
3. 规则定义、版本发布、挂载变更和决策查询应具备权限边界和审计记录。
4. 后续生产实现应预留指标、日志和告警：规则评估耗时、拒绝率、待复核率、配置冲突率和摘要冲突次数。

发布、灰度和风险：

1. 本轮不发布运行时代码；后续若新增表、公共契约或规则评估服务，必须补灰度、回滚和生产数据迁移策略。
2. 风险在于规则引擎过早做大、控制活动被误当规则定义、历史交易被当前规则重算、规则拒绝误入交易内核。
3. 待确认项包括 schemaDecision、规则表达式格式、冲突策略默认值、运营权限模型、历史数据迁移和生产回滚方案。

## 6. AC 和 Red 种子

| AC ID | 场景 | Then | Forbidden Facts |
| --- | --- | --- | --- |
| `AC-SR-DESIGN-001` | 创建规则定义并发布版本。 | 规则定义与不可变版本可追溯，版本摘要稳定。 | 发布版本被原地覆盖，或历史交易按当前版本重新解释。 |
| `AC-SR-DESIGN-002` | 将规则版本挂载到支付工具、预算 scope、账户或业务场景。 | 挂载 scope、优先级、冲突策略和生效窗口明确。 | 挂载关系输出资金责任主体，或生成 ledger subject。 |
| `AC-SR-DESIGN-003` | 授权前规则评估拒绝。 | 生成拒绝决策日志和拒绝原因，交易内核前失败。 | 生成 route、posting、LedgerEntry、资金交易扣款或余额投影。 |
| `AC-SR-DESIGN-004` | 历史投影解释规则命中。 | 交易投影读取当时规则版本、挂载和决策流水。 | 按当前规则定义、当前挂载或当前工具绑定重算历史解释。 |

| Red ID | 目标行为 | 目标测试资产候选 | 验证命令候选 |
| --- | --- | --- | --- |
| `RED-SR-DEF-001` | 已发布规则版本不得被原地覆盖。 | `SpendRuleDefinitionApplicationServiceTests`。 | `just test-one SpendRuleDefinitionApplicationServiceTests tests`。 |
| `RED-SR-ASSIGN-001` | 规则挂载不能把预算组、支付工具或 Spend Rule 输出为最终资金责任主体。 | `SpendRuleAssignmentApplicationServiceTests`、`WalletLayerBoundaryTests`。 | `just test-one SpendRuleAssignmentApplicationServiceTests tests`、`just test-boundary`。 |
| `RED-SR-DECISION-001` | 规则拒绝必须在交易内核前失败且无资金事实副作用。 | `SpendRuleDecisionApplicationServiceTests`、`AuthorizationAdmissionApplicationServiceTests`。 | `just test-one SpendRuleDecisionApplicationServiceTests tests`。 |

## 7. 后续编码准入

后续编码推进按新的单一 Grant 串行消费：

1. `GSD2-B5-SPEND-RULE-DEFINITION-CONTRACT-001`：已消费，只补规则定义、版本、挂载和决策日志契约、H2 schema 和目标服务流测试。
2. `GSD2-B5-SPEND-RULE-DECISION-CONSUME-001`：只把规则决策服务接入 `SpendControlAdmissionApplicationService` 或授权准入组合。
3. `GSD2-B5-SPEND-RULE-PROJECTION-EXPLAIN-001`：只补交易投影读取规则定义、版本、挂载和决策日志的解释能力。

继续消费下一编码 Grant 前必须重新确认：

| 门禁 | 要求 |
| --- | --- |
| schemaDecision | 是否允许新增规则定义、版本、挂载和决策日志表。 |
| writeScope | 是否允许写 `wallet-face`、`wallet-impl`、`tests` 和 H2 schema。 |
| noWriteScope | 不写 `SpendControlActivity`、交易 canonical 入参、ledger posting、Controller、HTTP/RPC、事件消费或生产迁移。 |
| verification | 至少包含目标服务流测试、`just compile`、`just pmd` 和 `git diff --check`；涉及 wallet 组合准入时追加相邻回归。 |

## 8. 验证

本轮为文档和任务闭环，验证命令：

```bash
git diff --check
```

验证方案还包括产品和架构结构检查、关键词残留检查和后续回归测试候选。静态检查只证明文档结构完整，不替代后续代码验证、服务流测试、compile、PMD 或发布验收。后续进入编码时必须追加目标测试、相邻回归、`just compile`、`just pmd` 和 `git diff --check`。

## 9. `GSD2-B5-SPEND-RULE-DEFINITION-CONTRACT-001` 消费记录

本节记录用户后续授权的独立编码 Grant，不改变前文 `GSD2-B5-SPEND-RULE-DESIGN-CLOSURE-001` 的 docs-only 性质。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B5-SPEND-RULE-DEFINITION-CONTRACT-001` |
| 当前状态 | `SPEND_RULE_DEFINITION_CONTRACT_GREEN_VERIFIED` |
| 已完成能力 | 新增 `SpendRuleDefinitionApplicationService`，用一个服务层入口承载规则定义、不可变版本发布、规则挂载和决策日志记录；新增 core 枚举、wallet face Request/DTO、wallet impl Entity/Mapper/Service、H2 schema 和目标服务流测试。 |
| 核心断言 | 已发布规则版本同摘要可幂等回放，摘要或规则正文不一致时拒绝覆盖；支付工具和预算组只作为控制 scope 记录，不输出资金责任主体；规则拒绝只留下决策日志，不生成资金交易、route、posting、LedgerEntry、账本交易或余额投影副作用。 |
| 写入范围 | `core` Spend Rule 枚举、`wallet-face` application / Request / DTO、`wallet-impl` application / DAL Entity / Mapper、`tests` 服务流测试和 H2 schema，以及本状态文档。 |
| 禁止范围 | 不实现完整规则引擎、规则表达式解析、运营后台、审批流、交易 canonical 入参调整、ledger posting、控制活动扩展、事件消费 / outbox、Controller、HTTP/RPC、生产 DDL 迁移、VCC facade 或 Git push。 |
| 验证证据 | 沙箱内目标测试因 embedded Redis 端口绑定受限失败；已按工具授权在沙箱外复跑 `SpendRuleDefinitionApplicationServiceTests`，3 tests 通过。收口仍需执行 `compile`、`pmd` 和 `git diff --check`。 |
| 下一候选 | `GSD2-B5-SPEND-RULE-DECISION-CONSUME-001` 已在后续记录中消费；后续若继续 Spend Rule，优先重新确认 `GSD2-B5-SPEND-RULE-PROJECTION-EXPLAIN-001`、完整规则引擎、事件消费 / outbox、生产迁移或其他单一 Grant。 |

## 10. `GSD2-B5-SPEND-RULE-SPEC-LOCK-001` docs-only 消费记录

本节记录用户要求“编码前先确认 Spend Rule spec JSON，并带入场景验证可读性和可解释性”的设计推进结果。本轮仅锁定产品、系分和 DSL 规格，不修改代码。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B5-SPEND-RULE-SPEC-LOCK-001` |
| 当前状态 | `SPEND_RULE_DSL_V1_1_DOCS_LOCKED` |
| 自主交付控制卡 | 新增 [GSD-2-B5-SpendRule自主交付控制卡.md](GSD-2-B5-SpendRule自主交付控制卡.md)，明确可自我挖掘、可自我规划、可自动执行、必须人工确认和置信度。 |
| DSL 规格 | `SpendRuleVersionSpec`、`SpendRuleAssignmentSpec`、`SpendRuleDecisionEvidenceSpec` 三类契约；字段组包括 `display`、`matchSpec`、`counterSpec`、`limitSpec`、`decisionSpec`、`safetySpec`、`evaluatedRules`、`decisionPolicy` 和 forbidden facts。 |
| 场景覆盖 | 单笔限额、周期金额限额、周期次数限额、MCC / 商户控制、国家地区控制、时间窗口、多规则裁决和拒绝无资金副作用目标断言。 |
| 写入范围 | 产品设计 09、系分设计 06、DSL README、TDD README、Spend Rule 设计闭环确认包、OpenSpec tasks。 |
| 禁止范围 | 不授权 Java、测试、DDL/H2 schema、Entity、Mapper、Controller、HTTP/RPC、交易 canonical 入参、ledger posting、事件消费、生产迁移、Git 提交或完整规则引擎。 |
| 下一候选 | `GSD2-B5-SPEND-RULE-DECISION-CONSUME-001` 已消费；后续可重新确认 `GSD2-B5-SPEND-RULE-PROJECTION-EXPLAIN-001`、完整规则引擎、运营后台、事件消费或生产迁移等独立 Grant。 |

## 11. `GSD2-B5-SPEND-RULE-DECISION-CONSUME-001` 消费记录

本节记录用户确认后的独立编码 Grant。本轮目标是让 Spend Rule 决策不再只停留在调用方口头证据中，而是在支出控制准入和支付工具授权准入链路里固化为 `SpendRuleDecisionLog`，同时保持交易 canonical 入参和账本事实边界不变。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B5-SPEND-RULE-DECISION-CONSUME-001` |
| 当前状态 | `SPEND_RULE_DECISION_CONSUME_GREEN_VERIFIED` |
| 已完成能力 | `SpendControlAdmissionApplicationService` 在完成支付工具预交易快照后调用 `SpendRuleDefinitionApplicationService#recordDecision`，固化规则 ID、版本、挂载、scope、支付工具、动作、金额、币种、业务流水、决策结果、拒绝原因和决策摘要；`AuthorizationAdmissionApplicationService` 将支付工具授权请求中的 Spend Rule assignment / scope / decision evidence 透传到支出控制准入。 |
| 核心断言 | 规则决策通过或拒绝都会形成可追溯决策日志；相同决策流水和摘要幂等复用，同一决策流水异摘要拒绝；规则拒绝停在授权内核前，不生成资金交易、route、posting、LedgerEntry、账本交易或余额投影副作用。 |
| 写入范围 | `wallet-face` 的支出控制准入和支付工具授权 Request / DTO、`wallet-impl` 的支出控制准入与授权准入组合、`tests` 中 `SpendControlAdmissionApplicationServiceTests` 和 `AuthorizationAdmissionApplicationServiceTests`，以及本文、控制卡和 OpenSpec tasks 状态回写。 |
| 禁止范围 | 不实现完整规则引擎、规则表达式执行器、运营后台、审批流、控制活动重构、交易 canonical 入参调整、ledger posting、Controller、HTTP/RPC、事件消费 / outbox、生产 DDL 迁移、VCC facade 或 Git push。 |
| 验证证据 | `just mvn-version` 通过；沙箱内目标测试因 embedded Redis 端口绑定被拒；沙箱外复跑 `just test-one SpendControlAdmissionApplicationServiceTests,AuthorizationAdmissionApplicationServiceTests tests`，9 tests 通过。后续提交前仍建议执行 `compile`、`pmd` 和 `git diff --check`。 |
| 残余风险 | 本轮只消费外部或上层已给出的决策证据，不计算 Spend Rule；不证明多规则引擎、运营配置后台、控制活动投影、事件消费、生产迁移或完整 VCC 业务 facade 已可用。 |
