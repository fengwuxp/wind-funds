# GSD-2 新基线工作流规划

## 1. 文档定位

本文是 2026-06-12 之后 wind-funds 的 `Loop + GSD + Goal` 新基线工作流入口，用于把上一轮已经提交的设计、代码、测试和任务证据重新对齐，并把旧的未完成计划从当前活跃执行队列中移除。

本文不是新的 PRD，不替代产品设计、DSL 设计、系分设计或 TDD 设计；也不是编码授权、生产发布授权或 Git 授权。旧 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`、`QUEUED_AFTER_P0_P1`、`PARTIAL_*_NOT_DONE` 候选只作为 backlog reference 和历史准入材料保留，不能再被解释为当前活跃计划。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W0-BASELINE-RESET-2026-06-12` |
| 原子任务 | 清理旧活跃计划、建立 GSD-2 状态载体、规划新的 Loop + GSD + Goal Workflow，并接入 AI 代码交付闭环与 Spec 模板准入。 |
| 所属阶段 | GSD-2 Wave 0 / Baseline Reset / Planning-only。 |
| Goal ID | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| Loop ID | `GSD2-LOOP-DEFAULT-PLAN-GRANT-2026-06-12` |
| 当前状态 | `SR_TRANSACTION_REFUND_REFERENCE_FACT_GUARD_GREEN_VERIFIED` |
| Git / code baseline | 当前已提交 Git/code baseline 包含 `78f7f008 feat: 补齐支出控制活动与预算投影`、`021ee2ce feat: 补齐支出控制准入快照`、`a1397ddf feat: 补齐对账差异报告只读查询` 和 `96938fa8 docs: 回写对账差异报告提交基线`；`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001 / scopeDecision=object-scope-schema-backed`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001`、`GSD2-B7-RECON-DIFFERENCE-REPORT-001`、`GSD2-B2-SPEND-CONTROL-ADMISSION-001` 和 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 已完成 Red / Green / Verify 并提交固化。`GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001` 已成为已消费被依赖证据；`4ef64275 feat: 补齐余额调账独立审计查询`、`da3b4f19 feat: 补齐余额调账路由审计回链`、`0b251593 feat: 补齐账本正常余额方向护栏`、`dd442888`、`ea8f8800`、`632bd2f6`、`ca603eab`、`873e5f8c`、`a38776c5`、`bc7ffc0f`、`10853e2d`、`ae8cb8a6` 和 `e81a8a25` 保留为已消费能力证据，`da7d2ea test: 阻断契约夹具承载资金流断言` 和 `b3b9712 feat: 对齐资金底座GSD基线与交易回放能力` 只保留为 GSD-2 历史证据。 |
| 当前补充证据 | `GSD2-B5-SR-TRANSACTION-CONSUME-001`、`GSD2-B5-SR-TRANSACTION-CONSUME-CONCURRENCY-001`、`GSD2-B5-SR-TRANSACTION-CONSUME-REFUND-RELEASE-GUARD-001`、`GSD2-B5-SR-TRANSACTION-CONSUME-REFUND-FACT-GUARD-002`、`GSD2-B5-SR-TRANSACTION-CONSUME-REFUND-COMPENSATION-GUARD-003`、`GSD2-B5-SR-TRANSACTION-CONSUME-REFUND-REFERENCE-GUARD-004`、`GSD2-B5-SR-TRANSACTION-CONSUME-BUSINESS-SCENE-GUARD-005`、`GSD2-B5-SR-TRANSACTION-CONSUME-BUSINESS-SN-GUARD-006`、`GSD2-B5-SR-TRANSACTION-RELEASE-BUSINESS-SN-GUARD-007`、`GSD2-B5-SR-TRANSACTION-REFUND-REFERENCE-NET-GUARD-008`、`GSD2-B5-SR-TRANSACTION-REFUND-REFERENCED-CONSUMED-CONSISTENCY-GUARD-009`、`GSD2-B5-SR-TRANSACTION-CONTROL-TRANSACTION-AMOUNT-GUARD-010`、`GSD2-B5-SR-TRANSACTION-CONSUME-IDEMPOTENCY-SEMANTIC-GUARD-011` 和本轮 `GSD2-B5-SR-TRANSACTION-REFUND-REFERENCE-FACT-GUARD-012` 已形成服务层验证证据。 |
| 设计 baseline | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec` 的当前可读状态；`openspec` 异常 Git 状态只作为停止条件和只读事实记录。 |
| 活跃未完成编码计划 | 当前没有活跃未完成编码计划；`GSD2-B5-SR-CONTROL-ACTIVITY-001 / schemaDecision=ddl-backed` 已按用户确认完成首轮实现、本地 Green / Verify 并提交到 `78f7f008`，`GSD2-B2-BUDGET-GROUP-NON-LEDGER-SUBJECT-001` 已提交到 `a5b12a3f`，`GSD2-B5-SR-TRANSACTION-CONSUME-001` 已完成服务层 Green / Verify / Commit，并发幂等、退款释放守卫、退款事实守卫、退款补偿入口守卫、退款已消费引用守卫、业务场景一致性守卫、成功消费业务流水一致性守卫、失败释放业务流水一致性守卫、退款引用净消费上限守卫、退款引用已消费一致性守卫、交易事实累计解释金额守卫、控制活动幂等语义一致性守卫和退款引用原交易事实守卫已完成服务层 Red / Green / Verify。旧候选不再作为当前编码计划，只能在新 Workflow 中被重新选择、重新编号、重新确认。 |
| Owner | AI Native 流程编排负责状态、Loop、GSD 和门禁；产品架构专家负责业务目标、对象、能力、验收和金融待确认；资深架构师负责系统边界、接口、TDD、验证和编码准入。 |
| 写入范围 | 本文、W5 P0/P1 ledger-wallet-transaction 推进计划、LWT 生产可用能力 Goal、B5 交易消费支出控制活动确认包、Agent Loop / Plan Grant 默认授权策略、AI 代码交付闭环基线、GSD-2 单一 Grant 任务模板、`docs/TDD设计/README.md`、`docs/README.md` 和 OpenSpec tasks 状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-B5-交易消费支出控制活动ExecutionGrant确认包.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 本轮新增写入补充 | `GSD2-B5-SR-TRANSACTION-CONSUME-001` 已完成服务层消费：交易成功记录控制消耗，失败 / 拒绝 / 过期释放控制占用，已有退款资金事实后记录控制补偿，且不调整支付工具 `REFUND` 方向规则；后续补片已证明 `REFUND` 交易事实不得被 `consume` 或 `release` 降级为普通控制活动，非 `REFUND` 交易事实不得被 `refund` 降级为退款控制补偿，退款交易引用原资金交易但没有对应已消费控制活动时不得生成退款控制补偿，资金交易事实不得跨业务场景消费当前 Spend Rule 控制占用，`consume` 不得串用同场景其他业务流水的成功交易，`release` 不得串用同场景其他业务流水的失败、拒绝或过期交易，`refund` 补偿金额不得超过退款交易引用的已消费控制活动净额，被引用已消费控制活动必须与原控制活动同源一致，同一原控制活动下同一资金交易流水的累计解释金额不得超过资金交易金额，同一控制活动流水和摘要不得被不同业务语义复用，且退款交易引用的原消费资金交易必须真实存在并满足租户、类型、状态和币种基础一致性。该切片只覆盖交易成功消耗、失败释放、退款补偿、原控制活动 / 原交易回链、业务场景一致性守卫、业务流水一致性守卫、退款引用净消费上限守卫、同源一致性守卫、金额解释守卫、幂等语义守卫和退款引用原交易事实守卫，不授权继续扩 Spend Rule 规则定义实现、VCC facade、支付工具交易内核、Controller、HTTP/RPC、事件消费者、生产迁移或 Git 历史重写。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、最近 Git 提交和旧 GSD/Grant 历史材料。 |
| 只读参考 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、源码、测试和 Git 提交记录。 |
| Git 策略 | B7 对象级 Gate、清算 / 结算 consumer、B7 差异报告、支出控制准入快照和 B5 控制活动均已按授权提交固化；本轮退款引用原交易事实守卫随本提交固化；后续新 Grant 默认 `summary_only`，除非用户再次明确授权提交。 |

## 2. 旧计划移除裁决

移除不是删除历史证据，而是从当前活跃执行队列中移除未完成计划。历史准入卡、Round 0、Execution Grant 消费记录、验证结果和 Not Done 边界继续保留，用于审计和下一轮重新选择。

| 处理对象 | 新状态 | 说明 |
| --- | --- | --- |
| 旧 GSD Goal 计划 | `SUPERSEDED_BY_GSD2_BASELINE_RESET` | `GSD-Goal-生产可用MVP推进计划.md` 保留为上一轮状态账本和历史证据，不再承载新的活跃计划。 |
| 旧 Agent Loop | `CLOSED_AS_HISTORY` | `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-LOOP-2026-06-11` 已关闭为历史 Loop。 |
| 已消费 Grant | `CONSUMED_HISTORY_ONLY` | 不得复用，例如账本 002A、003、004A 和 B3 直接退款引用回放。 |
| 未确认候选 | `BACKLOG_REFERENCE_NOT_ACTIVE_PLAN` | B2、B4、B5、B6/B8、B7、P2 VCC、P2 全球账户等候选均需在 GSD-2 中重新选择后才可进入计划。 |
| 当前活跃编码执行队列 | `SR_TRANSACTION_REFUND_REFERENCE_FACT_GUARD_GREEN_VERIFIED` | `GSD2-B5-SR-CONTROL-ACTIVITY-001` 已随 `78f7f008` 提交，预算组非建账已随 `a5b12a3f` 提交，`GSD2-B5-SR-TRANSACTION-CONSUME-001` 已完成服务层 Green / Verify / Commit；并发幂等、退款释放守卫、退款事实守卫、退款补偿入口守卫、退款已消费引用守卫、业务场景一致性守卫、成功消费业务流水一致性守卫、失败释放业务流水一致性守卫、退款引用净消费上限守卫、退款引用已消费一致性守卫、交易事实累计解释金额守卫、控制活动幂等语义一致性守卫和退款引用原交易事实守卫已完成服务层 Red / Green / Verify；当前没有 active 编码任务。下一轮必须重新选择新的单一 Grant。 |

清理规则：

1. 后续不得把旧文档中的 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` 当作“已经在做”的计划。
2. 后续不得把旧文档中的 `QUEUED_AFTER_P0_P1` 当作“排队自动执行”的计划。
3. 后续不得沿用旧 `Execution Grant` 名称继续写代码、测试、DDL/H2 schema、公共契约或运行时配置。
4. 下一轮只能从 GSD-2 重新建立 Task ID、Goal 映射、写入范围、验证命令和停止条件。

## 3. 产品基线

业务目标：交付金融创业公司 MVP 资金底座，支撑 VCC 发卡、VCC 交易处理、全球收付款，并能解释账户、交易、账本、清结算、对账、资金归属和余额变化。

用户价值：产品、运营、财务、风控、研发和测试可以用同一组资金事实判断能力是否可接入、可解释、可核对、可回归。

成功指标：每个被重新选择的任务都能回链 PRD 目标、DSL case、系分服务入口、TDD Red、真实 Spring Bean、H2/fixture、账务事实、余额投影、幂等、失败无副作用、审计和验证命令。

非目标：本轮不扩展完整发卡处理商、卡组织协议、PAN/CVV/HSM、完整 FX 执行、完整收单生产实现、监管或会计最终结论，也不新增统一支付工具交易内核。

| 产品项 | 当前基线 |
| --- | --- |
| 能力地图 | P0 账本账目、钱包账户、余额投影、清结算对账和资金数据治理；P1 直接交易、授权交易、余额控制、交易投影和 route replay；P2 VCC、全球账户、ACH/银行转账和收单只作为业务能力包接入。 |
| 业务对象 | 资金账户、信用账户、父子账户、支付工具、资金责任决策、资金交易、授权交易、余额控制、route snapshot、ledger transaction、LedgerEntry、余额投影、交易投影、对账批次、差错单和审计证据。 |
| 字段口径和生命周期 | 可入账主体仍以资金账户、信用账户和平台角色解析后的平台资金账户为核心；VCC 卡是支付工具，背后绑定资金或信用子账户；支付工具、外部账户、预算组、Spend Rule 和投影不作为账本主体。 |
| 业务流程 | 主流程是请求、准入、路由、账务、投影、对账和解释；异常流程包括缺原事实、缺 route snapshot、余额不足、规则不唯一、外部非终态、差错阻断和敏感字段越界；人工兜底只能进入差错、审批或补事实白名单。 |
| 规则矩阵 | 触发条件、判断逻辑、优先级和版本必须落到账户主体、资金责任、route snapshot、posting 平衡、幂等摘要、Spend Rule 控制、对账差错、外部规则和敏感字段阻断。 |
| 运营后台和数据口径 | 运营后台、指标、报表、审计和数据口径必须区分资金事实、账本事实、余额投影、交易投影、对账差错和外部非终态。 |
| 风险、待确认和验收 | 风险是把候选文档当生产 Done、把支付工具或预算组当账务主体、用 mock/内存实现冒充生产能力、绕过 P0/P1 依赖直接做 P2；确认方包括产品、架构、研发、测试、运营、财务、风控、安全、法务和合规。 |
| 发布 | 本文不发布生产能力；任何发布、灰度、回滚和上线验收都必须在具体 Grant 完成代码、测试、DDL/H2、审计和验证证据后另行评审。 |

## 4. 架构和代码基线

背景：GSD-2 初始重置点为 `b3b9712`，后续已继续提交账户层级快照、信用账户路由闭合、GSD2 路由快照契约、DSL 契约门禁、资金责任目标主体、wallet application facade、支付工具能力准入 facade、交易投影解释、余额调账审计、对账差错闭环、对账差错处理动作守卫、对账准入消费、出款 preflight 消费、AUTH chargeback 兼容 guard、B4 投影解释 remaining 矩阵、wallet 授权准入、route snapshot 回链、ledger guard、B5-002 route snapshot 审计回链、B5-003 独立审计查询、B7 对象级 Gate 基座、B7 清算 / 结算 consumer、B7 对账差异报告、账户能力来源准入、支付工具预交易快照、支出控制准入快照和 B5 Spend Rule 控制活动，当前已提交 Git/code baseline 至 `78f7f008`。当前需要避免继续沿用旧计划或旧基线状态，先以状态载体把最新设计、代码、任务和 OpenSpec 重新对齐。

目标：用 GSD-2 固定当前设计、代码和任务基线，清理活跃队列，重新定义下一轮只读侦察、Gap Audit、单一 Grant 选择和 CAD 执行的顺序。

非目标：本轮不继续扩大生产代码、测试代码、公共契约、生产 DDL、运行时配置、状态机或模块依赖方向；B7 对象级 Gate 和清算 / 结算 consumer 已按用户授权完成并提交，B7 差异报告已完成最小只读查询闭环但不外推到批次报告、导出、运营审批、补事实或完整清结算。

成功标准：文档能说明当前现状、核心决策、接口契约、数据方案、事务边界、一致性、补偿、对账、可靠性、安全、权限、审计、告警、验证方案、测试、静态检查、回归、发布、灰度、回滚、风险和待确认。

| 架构项 | 当前基线 |
| --- | --- |
| 现状和影响范围 | 当前最新已提交代码基线为 `021ee2ce feat: 补齐支出控制准入快照`；B7 对象级 Gate 基座、清算 / 结算 consumer、B7 差异报告和支出控制准入快照已提交固化。B5-002 route snapshot 审计回链已在 `da3b4f19` 提交固化，B5-003 独立审计查询已在 `4ef64275` 提交固化；`0b251593`、`dd442888`、`ea8f8800`、`632bd2f6`、`ca603eab`、`873e5f8c`、`a38776c5`、`bc7ffc0f`、`10853e2d`、`e81a8a25` 和 `ae8cb8a6` 保留为已消费能力证据，`da7d2ea` 和 `b3b9712` 只保留为 GSD-2 历史证据。当前 B5 Spend Rule 控制活动确认包只影响 docs 和 OpenSpec tasks，不影响 core、wallet-face、wallet-impl、tests 或运行时配置。 |
| 核心决策 | 交易内核继续使用已解析账户主体作为 canonical 入参；支付工具入口留在 wallet/application facade；ledger 只维护账本事实；route 只解析路径和快照；投影只读派生，不反写事实。 |
| 职责边界和取舍 | 先完整被依赖方能力，再做 VCC 和全球账户；先 contract-only 或 service-flow-backed 小切片，再考虑 P2 业务 facade；不为了统一入口引入 `InstrumentTransactionService`。 |
| 接口契约 | 任一入参、出参、错误码、幂等摘要、公共 DTO、Request、Query、枚举、状态机或兼容变更，必须由新的单一 Execution Grant 明确授权。 |
| 数据方案 | 涉及持久化时必须声明 DDL/H2 schema、Entity、Mapper、唯一键、索引、fixture、迁移边界和验证命令；本文不授权任何数据结构变更。 |
| 事务边界和一致性 | 资金变化必须证明 route、posting、entry、projection 和差错处理的事务边界；失败必须无半截事实；补偿和对账必须有来源事实、审批和幂等。 |
| 可靠性、安全和审计 | 后续 Grant 必须覆盖重复请求、并发、重放、外部非终态、敏感字段阻断、权限边界、审计引用和告警或人工处理入口。 |
| 验证方案 | 文档变更用 Harness / product / architecture checker、`rg` 扫描和 `git diff --check`；代码变更按具体 Grant 运行 `just test-one`、分组测试、`just compile`、`just pmd` 或 `just verify-cad`。 |
| 发布和回滚 | 本轮不进入发布；后续生产级 Grant 必须说明灰度、回滚、监控、告警、人工兜底和残余风险。 |

## 5. GSD-2 Workflow

GSD-2 的目标不是马上编码，而是先让新的状态、反馈、验证和停止条件可审。只有当某个候选被重新选中，并补齐单一 Execution Grant、Spec 强度、AC 映射、Harness 摘要和 AI 代码交付闭环后，才进入 CAD Loop。

### Wave 0：基线重置和旧计划出队

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W0-BASELINE-RESET-2026-06-12` |
| Owner | AI Native 流程编排 + 产品架构专家 + 资深架构师 |
| 写入范围 | 本文、Agent Loop / Plan Grant 默认授权策略、AI 代码交付闭环基线、旧 GSD 计划迁移指针、TDD README、docs README。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Git HEAD 和旧状态账本。 |
| Wave 边界 | 只做状态和任务基线，不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。 |
| 上下文账本 | 本文是新的状态载体；旧 GSD 计划、状态账本和 Grant 卡只作为历史证据。 |
| 验收场景 | 活跃未完成计划清零；旧候选可追溯但不再自动执行；新的 Workflow 有恢复入口、验证矩阵和 handoff。 |
| 验证命令 | Harness checker、产品交付物 checker、架构交付物 checker、`rg` 一致性扫描、`git diff --check`。 |
| 停止条件 | 发现旧计划仍被声明为活跃、发现 Git 基线和文档冲突、需要改代码或需要 Git 提交授权。 |
| handoff | Wave 0 完成后，进入 Wave 1 只读 Gap Audit 或等待用户确认下一单一 Grant。 |

### Wave 0A：AI 代码交付闭环和 Spec 模板准入

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W0-AI-CODE-DELIVERY-LOOP-2026-06-12` |
| Owner | AI Native 流程编排 + 资深架构师 |
| 写入范围 | `docs/TDD设计/AI代码交付闭环与Spec模板基线.md` 及入口引用。 |
| 只读范围 | AI Native 流程 reference、Spec 模板最佳实践、GSD-2 基线、PRD/DSL/系分/TDD/OpenSpec 和 AGENTS.md。 |
| Wave 边界 | 只定义交付闭环、Spec 强度、AC 映射、Harness 三层、独立验证、CR 减负和知识回流，不写 Java、测试、DDL/H2 schema 或公共契约。 |
| 上下文账本 | `AI代码交付闭环与Spec模板基线.md` 作为后续 Wave 1/2/3 的 Spec 准入闸门，`GSD-2-Spec-AC-Harness-CAD任务模板.md` 作为单一 Grant 任务卡模板。 |
| 验收场景 | 任一 GSD-2 候选进入编码前，必须能说明 Spec ID、Goal 映射、AC/测试映射、验证命令、停止条件和知识回流位置。 |
| 验证命令 | Harness checker、产品交付物 checker、架构交付物 checker、`rg` 入口扫描、`git diff --check`。 |
| 停止条件 | 需要新增自动化脚本、改 OpenSpec tracked/untracked 状态、写代码、写测试、提交 Git 或变更项目长期规则。 |

### Wave 0B：Agent Loop 和默认 Plan Grant 授权策略

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W0B-AGENT-LOOP-PLAN-GRANT-2026-06-12` |
| Owner | AI Native 流程编排 + 资深架构师 |
| 写入范围 | `docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md` 及入口引用。 |
| 只读范围 | Agent Loop、GSD/CAD、Goal、CAD Mode reference、AGENTS.md、PRD/DSL/系分/TDD/OpenSpec、源码、测试和 Git 状态。 |
| Wave 边界 | 只固化低风险默认推进边界、Loop 契约、停止条件和 handoff，不写 Java、测试、DDL/H2 schema、公共契约、运行时配置或 `openspec` 异常状态。 |
| 上下文账本 | `GSD-2-AgentLoop-PlanGrant默认授权策略.md` 作为默认授权状态载体；本文继续作为 GSD-2 工作流总入口。 |
| 验收场景 | 后续用户要求“继续推进”时，低风险文档、状态、只读 Gap Audit、Spec/AC/Harness/CAD 任务卡可默认推进；代码、测试、DDL、公共契约、Git、联网和生产动作仍显式确认。 |
| 验证命令 | Harness checker、产品交付物 checker、架构交付物 checker、`rg` 入口扫描、`git diff --check`。 |
| 停止条件 | 需要 Git 操作、OpenSpec tracked/untracked 状态处置、代码/测试/DDL/公共契约写入、联网、依赖安装、部署、真实资金或专业合规确认。 |

### Wave 1：设计、代码、任务 Gap Audit

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W1-BASELINE-GAP-AUDIT` |
| Owner | 产品架构专家负责产品和验收口径；资深架构师负责源码、测试、OpenSpec 和架构边界。 |
| 写入范围 | 默认只写 Gap Audit 报告和任务状态，不写生产代码、测试、DDL/H2 或公共契约。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试和 `b3b9712..e81a8a25` 的已提交差异。 |
| 验收场景 | 重新列出设计与代码差异、生产可用缺口、下一可选 Grant 和 Not Done 边界。 |
| AI 交付闭环 | 每个缺口需标注建议 Spec 强度、AC 覆盖方式、独立验证证据和是否需要人工主导。 |
| 验证命令 | `rg` 追踪、`git status --short`、必要时只读源码锚点扫描；若不改代码，不要求编译。 |
| 停止条件 | 发现需要公共契约、DDL/H2、生产代码或跨能力域写入时，停止并转 Wave 2 Grant 选择。 |
| 当前结果 | 已形成 [GSD-2-W1-基线差距审计.md](GSD-2-W1-基线差距审计.md)，状态为 `W1_GAP_AUDIT_DONE_READY_FOR_W2_SINGLE_GRANT_SELECTION`。 |

### Wave 2：重新选择单一 Grant

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W2-SINGLE-GRANT-SELECTION` |
| Owner | 用户确认优先级；产品架构专家确认业务价值和验收；资深架构师确认写入范围、Red、验证命令和风险。 |
| 写入范围 | 仅所选 Grant 的任务卡、Red 卡、验证矩阵和状态账本。 |
| 只读范围 | 所有候选准入卡和源码证据。 |
| Spec 准入 | 必须消费 `AI代码交付闭环与Spec模板基线.md` 并复制 `GSD-2-Spec-AC-Harness-CAD任务模板.md` 或等价任务卡，补齐 Spec ID、Spec 强度、AC 表、Goal / AC 映射、验证命令、CR 交接和知识回流位置。 |
| 并行边界 | 同一时间只允许一个 Grant 进入 active；共享公共契约、状态机、fixture 或 H2 schema 的候选必须串行。 |
| 候选队列 | 已完成 `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001`、`GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002`、`GSD2-B2-FR-TARGET-001`、`GSD2-B2-WALLET-APPLICATION-FACADE-001`、`GSD2-B2-WALLET-APPLICATION-FACADE-002 / B2-PI-CAP-CAD-001`、`GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001`、`GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001`、`GSD2-B2-SPEND-CONTROL-ADMISSION-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-002`、`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、`GSD2-B7-RECON-DIFFERENCE-MVP-001`、`GSD2-B7-RECON-DIFFERENCE-MVP-002`、`GSD2-B7-RECON-GATE-CONSUME-001`、`GSD2-B7-RECON-GATE-CONSUME-002`、`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`、`GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 和 `GSD2-LD-LEDGER-GUARD-REGRESSION-001` 首轮 Green；当前以 [GSD-2-LWT-生产可用能力Goal.md](GSD-2-LWT-生产可用能力Goal.md) 作为下一轮 loop 状态载体。 |
| 默认建议 | 若继续优先收敛 LWT 生产可用能力，`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING`、wallet 授权准入、账户能力来源、预交易快照、支出控制准入、ledger guard、B5 余额调账、B7 gate / consumer / report、B5 控制活动、预算组非建账和 B5 交易消费支出控制活动均已消费；下一轮需重新选择新的单一 Grant。 |
| 当前默认确认包 | 当前没有可复用默认确认包；[GSD-2-B5-交易消费支出控制活动ExecutionGrant确认包.md](GSD-2-B5-交易消费支出控制活动ExecutionGrant确认包.md) 已消费并作为证据保留，状态 `SR_TRANSACTION_REFUND_REFERENCE_FACT_GUARD_GREEN_VERIFIED`。 |
| 当前结果 | 已形成 [GSD-2-W2-单一Grant选择卡.md](GSD-2-W2-单一Grant选择卡.md)，并已由 W3A 完成只读源码定位、由 W4 完成确认包；`Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` 已在 2026-06-15 消费到首个 Red，随后 `GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 已在当前工作树完成最小 Green 和目标回归。 |
| 停止条件 | 用户未确认优先级、Grant 字段缺写入范围、验证命令或停止条件；或候选需要外部规则、专业确认、生产配置、联网、依赖安装、Git push、部署或不可逆操作。 |

### Wave 3A：B2 账户层级只读源码定位和 CAD 准入草案

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W3-B2-AH-CAD-READINESS` |
| Owner | 产品架构专家确认账户层级业务验收和 Not Done；资深架构师确认源码锚点、Red 重排、写入范围和验证命令；用户确认是否进入代码。 |
| 写入范围 | 仅 W3 准入草案和入口状态回写，不写生产代码、测试、DDL/H2 或公共契约。 |
| 只读范围 | core route spec/model、transaction route snapshot/replay、JSON support、DSL/route/transaction 测试、PRD、DSL、系分、TDD、OpenSpec 和最近 Git 提交。 |
| 上下文账本 | [GSD-2-W3-B2账户层级CAD准入草案.md](GSD-2-W3-B2账户层级CAD准入草案.md) 已记录只读源码锚点、已具备证据、首批 Red 重排和 handoff。 |
| 当前结果 | 账户层级 DSL / value object / JSON / replay 纯边界已有局部证据；下一步 Red 应转向真实服务流生成、生命周期保存、回放和失败无副作用。 |
| 验证命令 | Harness checker、产品/架构结构检查、`rg` 一致性扫描和 `git diff --check`。 |
| 停止条件 | 用户未确认 Execution Grant，或需要 Java/测试/公共契约/DDL/H2/Git/联网/生产动作。 |

### Wave 3B：CAD Loop 执行

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W3-CAD-LOOP-ACTIVE`，仅在 Wave 2/3A 完成并由用户确认具体 Grant 后派生子任务。 |
| Owner | 资深架构师。 |
| 写入范围 | 只限被确认 Grant 的文件、模块、测试资产和最小实现。 |
| 只读范围 | 关联 PRD、DSL、系分、TDD、OpenSpec、旧准入卡和源码锚点。 |
| 验收场景 | Red 符合预期，Green 最小实现，资金不变量、失败无副作用、幂等、审计和回归证据齐备。 |
| AI 交付闭环 | 每轮遵循读取状态、选择最小动作、执行变更、独立验证、读取反馈、回写 Spec/AC/TDD/Harness、判断继续或停止。 |
| 验证命令 | 按 Grant 运行 `just test-one`、相关分组测试、`just compile`、`just pmd` 或 `just verify-cad`。 |
| Review | 每轮代码后做问题优先 CR，检查业务语义、边界方向、契约完整性、失败路径和工程一致性。 |
| 停止条件 | 验证失败无法在授权范围内修复、工作树冲突、公共契约越界、生产风险升级或用户中断。 |

### Wave 4A：B2 账户层级 Execution Grant 确认包

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W4-B2-AH-EXECUTION-GRANT-PACK` |
| Owner | AI Native 流程编排负责确认包；产品架构专家确认验收和 Not Done；资深架构师确认写入范围、首个 Red、验证命令和停止条件。 |
| 写入范围 | 仅 W4 确认包和入口状态回写，不写生产代码、测试、DDL/H2 或公共契约。 |
| 只读范围 | W1/W2/W3 文档、PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile 和最近 Git 提交。 |
| 上下文账本 | [GSD-2-W4-B2账户层级ExecutionGrant确认包.md](GSD-2-W4-B2账户层级ExecutionGrant确认包.md) 已给出可复制确认文本、首个 Red、验证命令和停止条件。 |
| 当前结果 | 用户已确认 `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` 并进入首个 Red；`B2-AH-RED-001-SERVICE-FLOW-SNAPSHOT` 已观察到预期失败。随后 `GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 补齐账户层级来源契约、H2 schema、wallet 服务和授权 route snapshot 接入，当前目标测试已 Green。 |
| 验证命令 | Harness checker、产品/架构结构检查、`rg` 一致性扫描和 `git diff --check`。 |
| 停止条件 | 用户未确认 Execution Grant，或需要 Java/测试/公共契约/DDL/H2/Git/联网/生产动作。 |

### Wave 5：P0/P1 Ledger-Wallet-Transaction 优先推进计划

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W5-P0P1-LWT-PRIORITY-PLAN` |
| Owner | AI Native 流程编排负责顺序和门禁；产品架构专家确认业务价值和 Not Done；资深架构师确认工程边界、Red 和验证命令。 |
| 写入范围 | 仅 W5 推进计划和入口状态回写，不写生产代码、测试、DDL/H2 或公共契约。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、ledger、wallet、transaction、core、tests、Justfile 和最近 Git 提交。 |
| 上下文账本 | [GSD-2-P0P1-LedgerWalletTransaction推进计划.md](GSD-2-P0P1-LedgerWalletTransaction推进计划.md) 已把 ledger guard、账户层级、资金责任、wallet application facade、交易投影解释和余额调账审计排成依赖队列。 |
| 当前结果 | 当前代码基线已推进到预算组非建账随 `a5b12a3f` 固化，docs 状态同步随 `3b31d6e0` 固化；B5 Spend Rule 控制活动已在 `78f7f008` 固化，支出控制准入快照已在 `021ee2ce` 固化，B7 对账差异报告已在 `a1397ddf` 固化。本轮 `GSD2-B5-SR-TRANSACTION-CONSUME-001` 已完成服务层 Green / Verify / Commit，并把 wallet application CR 结论部分消费为交易结果控制活动闭环证据。 |
| 验证命令 | Harness checker、产品/架构结构检查、`rg` 一致性扫描和 `git diff --check`。 |
| 停止条件 | 用户未确认 Execution Grant，或需要 Java/测试/公共契约/DDL/H2/Git/联网/生产动作。 |

恢复规则：本总入口的父 Goal 继续承载 GSD-2 全局历史和 Wave；当前 LWT 子 Goal 的实时状态、下一 Grant 和停止条件以 [GSD-2-LWT-生产可用能力Goal.md](GSD-2-LWT-生产可用能力Goal.md) 第 1.1 节为准。旧 `B7_RECON_DIFFERENCE_ACTION_GUARD_GREEN_VERIFIED`、`GSD2-B7-RECON-GATE-CONSUME-001/002`、B2 账户层级状态和 B5-003 只能作为已消费证据，不得作为当前可执行授权。

### Wave 4：验证、交付和提交切片

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W4-VERIFY-HANDOFF` |
| Owner | AI Native 流程编排 + 资深架构师。 |
| 写入范围 | 交付记录、验证矩阵、状态账本、OpenSpec/Harness 回写。 |
| Git 策略 | 默认 `summary_only`；只有用户明确要求提交并且验证通过，才执行本地 `git add` / `git commit`。 |
| 完成条件 | 修改文件、覆盖清单、验证命令、验证结果、Not Done、残余风险和下一 owner 清楚。 |
| 回滚提示 | 文档变更可通过 Git diff 回滚；代码变更必须按 Grant 的回滚或补偿边界说明。 |

## 6. Agent Loop 契约

| 字段 | 内容 |
| --- | --- |
| Loop ID | `GSD2-LOOP-DEFAULT-PLAN-GRANT-2026-06-12` |
| 关联 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| Loop 类型 | `Plan Grant Loop / low-risk local baseline` |
| 状态载体 | 本文、`GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`GSD-2-LWT-生产可用能力Goal.md` 第 1.2 节 `Loop Progress Ledger`、`GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md`、`GSD-2-AgentLoop-PlanGrant默认授权策略.md`、`AI代码交付闭环与Spec模板基线.md`、`GSD-2-Spec-AC-Harness-CAD任务模板.md`、TDD README、docs README 和 Git HEAD。 |
| 决策输入 | 用户目标、当前 Git 状态、旧计划迁移裁决、PRD/DSL/系分/TDD/OpenSpec、源码和验证结果。 |
| 允许动作 | 低风险文档同步、状态回写、只读 Gap Audit、Spec/AC/Harness/CAD 任务卡、验证矩阵和下一 Grant 草案。 |
| 禁止动作 | 未确认新 Grant 前，不写 Java、测试代码、公共契约、DDL/H2 schema、运行时配置，不处置 `openspec` staged/untracked 异常状态，不做 Git add/commit/push、联网、依赖安装、生产配置或不可逆操作。 |
| 反馈源 | checker、`rg` 扫描、`git status --short`、`git diff --check`、用户确认和后续专项验证。 |
| 验证者 | 文档结构由脚本验证；产品语义由产品架构专家确认；工程边界由资深架构师确认；优先级由用户确认。 |
| 预算 / 最大轮次 | 每轮最多 1 个低风险本地任务；连续 2 轮没有新增证据、状态变化或缺口收敛时暂停，并回写 LWT Goal 第 1.2 节无进展计数。 |
| 无进展检测 | 如果连续两轮只是重复旧候选状态，没有新增证据、差异、验证或用户确认，则停止扩写并等待用户选择下一 Grant；判定以 LWT Goal `Loop Progress Ledger` 为准。 |
| 停止条件 | 触发公共契约、DDL/H2、生产代码、外部规则专业确认、工作树冲突、验证失败、工具审批、Git 操作或用户中断。 |
| 失败回写 | 本文、Agent Loop / Plan Grant 默认授权策略、AI 代码交付闭环基线、GSD-2 单一 Grant 模板和对应候选准入卡。 |
| 交接物 | 新基线裁决、AI 代码交付闭环、Backlog reference、候选优先级、验证矩阵和 Execution Handoff Card。 |

## 7. 下一候选优先级

下列候选是 backlog reference，不是活跃计划。任何一项进入执行前都必须重新确认单一 Execution Grant。

| 优先级 | 候选 | 建议级别 | 理由 | 不做范围 |
| --- | --- | --- | --- | --- |
| 1 | [GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001](GSD-2-AUTH-Chargeback目标语义对齐任务卡.md) | `contract/design-only` | 已补任务卡：目标态主入口为 `settleRefund` 争议字段；现有 `chargeback` 仅作为历史兼容、显式事件或内部适配资产。 | 已完成 docs-only 对齐，不进入代码。 |
| 2 | [GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001](GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md) | `consumed-green` | 已按 `COMPAT_GUARD_NO_BEHAVIOR_BREAK` 完成现有 `chargeback` 入口的兼容说明、最小审计 guard、兼容测试和状态回写。 | 不继续沿用本 Grant 扩完整 dispute case、DDL/H2 schema、事件语义迁移或公共 API 删除。 |
| 3 | `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING` | `consumed-green` | 已覆盖普通退款、无授权退款、争议退款、释放/过期和兼容 chargeback 的只读解释矩阵。 | 不继续沿用本 Grant 扩 projection store、治理重放、历史节点选择查询、失败态全量解释或运营差异报告。 |
| 4 | `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` | `consumed-green` | 已新增 `AuthorizationAdmissionApplicationService` 或等价入口，把工具准入、绑定快照、资金责任、账户能力和账户主体型授权内核委派组合成可测试用例。 | 不沿用本 Grant 扩 VCC facade、Spend Rule 策略引擎、完整预交易快照或统一支付工具交易内核。 |
| 5 | `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` | `consumed-green` | 已补授权准入后支付工具引用、绑定版本和准入决策到 route snapshot 的回链。 | 不继续沿用本 Grant 扩 VCC 生命周期、Spend Rule 策略引擎、完整预交易快照或统一支付工具交易内核。 |
| 6 | `GSD2-LD-LEDGER-GUARD-REGRESSION-001` | `consumed-green` | 已补固定账目类别正常余额方向 guard；任一资金变化切片后续仍需复跑 ledger 目标测试。 | 不继续沿用本 Grant 重启 GSD1 大包、治理重放或清结算补事实。 |
| 7 | `GSD2-B5-BALANCE-ADJUST-AUDIT-002` | `consumed-green` | 已补余额调账 route snapshot 审计回链，证明外部余额异常纠偏的来源、证据、审批、对账、责任和受控负可用策略可从 route snapshot 解释。 | 不继续沿用本 Grant 扩独立审计表、运营审批流或泛化运营补账。 |
| 8 | `GSD2-B5-BALANCE-ADJUST-AUDIT-003` | `consumed-green` | 已补余额调账独立审计查询最小服务流，能只读聚合交易事实、交易明细上下文、route snapshot、ledger transaction 和 LedgerEntry，并验证敏感字段不泄露、查询无资金副作用。 | 不继续沿用本 Grant 扩独立审计表、运营审批流、泛化运营补账、补事实执行、B7 差错创建或生产权限模型。 |
| 8 | `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` | `consumed-green-summary-only` | B7 gate consume、出款 preflight 消费和对象级差错阻断基座已完成；清算 / 结算真实消费方缺口已由后一条 consumer Grant 关闭。 | 本 Grant 不再复用；不一次性打开完整清分、清算、结算、出款、追偿和运营后台；不直接生成补事实资金事实。 |
| 8 | `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` | `consumed-green-verified-summary-only` | 已补清算 / 结算只读 gate consumer 服务流，证明 CLEARING / SETTLEMENT 能真实消费对象级 gate 且无账务副作用。 | 本 Grant 不再复用；不扩完整清结算生命周期、补事实命令、运营审批、生产迁移或外部通道。 |
| 9 | 支付工具 / Spend Rule 支持 | `contract-only` 起步 | 工具动作能力、授权 application facade、Spend Rule 控制活动和只读解释依赖账户、资金责任、交易内核和对账。 | 不把支付工具、预算组或 Spend Rule 写成 ledger subject。 |
| 9 | P2 VCC / 全球账户 | `contract-only` 起步 | 业务目标重要，但必须消费账户、资金责任、交易内核和对账差错证据。 | 不直接写 P2 facade、资金流、外部轨道或通道规则生产结论。 |
| 10 | 收单 | `design-only` | 当前不是 MVP 实现优先级。 | 不写 capture/dispute 生产代码、测试或 DDL。 |

## 8. Execution Handoff Card

| 字段 | 内容 |
| --- | --- |
| Goal ID | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| Wave / Task ID | `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 已消费并完成本地 Green、目标回归和门禁收口；`GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001` contract/design-only 已完成；`GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002`、`GSD2-B2-FR-TARGET-001`、`GSD2-B2-WALLET-APPLICATION-FACADE-001`、`GSD2-B2-WALLET-APPLICATION-FACADE-002 / B2-PI-CAP-CAD-001`、`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`、`GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-002`、`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、`GSD2-B7-RECON-DIFFERENCE-MVP-001`、`GSD2-B7-RECON-DIFFERENCE-MVP-002`、`GSD2-B7-RECON-GATE-CONSUME-001`、`GSD2-B7-RECON-GATE-CONSUME-002` 和 `GSD2-LD-LEDGER-GUARD-REGRESSION-001` 已完成本地 Green、目标回归和门禁收口。 |
| 状态载体 | 本文、W1 基线差距审计、W2 单一 Grant 选择卡、W3 B2 账户层级 CAD 准入草案、W4 B2 账户层级 Execution Grant 确认包、W5 P0/P1 ledger-wallet-transaction 推进计划、LWT 生产可用能力 Goal、AUTH Chargeback 目标语义任务卡、AUTH Chargeback 兼容入口确认包、Agent Loop / Plan Grant 默认授权策略、AI 代码交付闭环基线、GSD-2 单一 Grant 任务模板、TDD README、docs README。 |
| 写入范围 | 已消费来源契约 Grant、资金责任目标 Grant、wallet application facade、B4 投影解释、B5 余额调账审计、B5 独立审计查询、B7 差错闭环、B7 动作守卫、B7 准入消费、B7 出款准入消费和 B7 对象级 Gate 基座等切片；这些写入范围只作为历史能力证据和回归锚点。后续任一 Java、测试、生产 DDL、公共契约或运行时配置写入，均需新的单一 Grant 限定。 |
| 只读范围 | 全部设计文档、OpenSpec、源码、测试、旧状态账本和 Git 提交。 |
| 反馈源 | checker、`rg`、`git status --short`、`git diff --check`、用户确认和专项测试。 |
| 验证命令 | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-cad` 已通过；B5 首轮追加 `just test-one FundsBalanceAdjustAuditFlowTests tests`、`just test-one FundsBalanceControlFailureFlowTests tests`、`just test-balance-control`、`just test-reconciliation`、`just compile`、`just pmd` 和 `git diff --check`；B5-003 已追加 `just test-one LedgerDtoContextVariablesContractTests tests`、`just test-one FundsBalanceAdjustAuditFlowTests tests`、`just test-balance-control`、`just test-boundary`、`just test-transaction`、`just compile` 和 `just pmd`；B7 首轮和动作守卫追加 `just test-one ReconciliationDifferenceApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just verify-fast`、`just pmd` 和 `git diff --check`；B7 准入消费追加 `just test-one ReconciliationGateApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just pmd` 和 `git diff --check`；B7 出款准入消费追加 `just test-one PayoutPreflightServiceTests tests` 和 `just test-reconciliation`；本轮 B7 对象级 Gate 追加目标测试、`test-reconciliation`、`compile` 和 `pmd`。 |
| AI 交付准出 | 后续 Grant 必须列出 Spec/AC 映射、Red/Green 证据、独立验证命令、CR 交接、Not Done、知识回流和建议 commit message。 |
| 停止条件 | 未确认新的单一 Execution Grant 前，不继续修改 Java、测试、生产 DDL、公共契约、wallet application facade、交易投影、余额调账、支付工具准入、Spend Rule、VCC、清结算真实消费方或 P2 业务；B5-003 和 B7 对象级 Gate 已消费，不得复用为审批、补事实或清结算真实消费方授权；需要 Git 授权、处置 `openspec` 异常状态、验证失败或用户调整优先级时停止。 |
| Git 策略 | `summary_only`。 |
| 下一 owner | 用户重新选择新的单一 Grant；不再复用 `GSD2-B5-SR-TRANSACTION-CONSUME-001`、`GSD2-B5-SR-TRANSACTION-CONSUME-CONCURRENCY-001`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001`、`GSD2-B7-RECON-DIFFERENCE-REPORT-001`、`GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001`、`GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001`、`GSD2-B2-SPEND-CONTROL-ADMISSION-001`、`GSD2-B5-SR-CONTROL-ACTIVITY-001` 或 `GSD2-B2-BUDGET-GROUP-NON-LEDGER-SUBJECT-001`；若继续 wallet，建议优先从完整 Spend Rule 规则引擎、事件消费 / outbox、VCC facade 或生产迁移准入中择一建立新 Grant。 |

## 9. 验证矩阵

| 验证层 | 命令或方式 | 当前通过口径 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-新基线工作流规划.md` | GSD Wave 必备字段齐全。 |
| AI 代码交付闭环结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/AI代码交付闭环与Spec模板基线.md` | Spec 准入、AC 映射、Harness、验证和交接字段齐全。 |
| 单一 Grant 任务模板结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-Spec-AC-Harness-CAD任务模板.md` | Spec、AC、Red、Harness、CAD、CR 和交付证据字段齐全。 |
| Agent Loop / Plan Grant 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md` | 默认授权范围、Loop 契约、验证者、停止条件、handoff 和显式确认边界齐全。 |
| P0/P1 推进计划结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` | ledger、wallet、transaction 任务顺序、Grant 队列、写入范围、验证命令和 handoff 字段齐全。 |
| LWT Goal 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` | LWT Goal 的状态载体、完备性矩阵、三卡交接、结构门禁证据和单一 Grant 决策账本可消费。 |
| AUTH Chargeback 入口结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md`、`python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md`、产品和架构结构 checker。 | 目标语义任务卡可作为 contract/design-only 证据，兼容入口确认包可供用户复制确认，并包含预检清单和运行卡；仍不授权 Java、测试、DDL/H2 schema、公共契约或 Git。 |
| 产品架构结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-新基线工作流规划.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构方案结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-新基线工作流规划.md` | 背景目标、边界取舍、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-LWT|LWT-PRODUCTION|单一 Grant 决策账本|结构门禁验证证据账本|FundsAccountCapabilityApplicationServiceTests|PaymentInstrumentPreTransactionSnapshotApplicationServiceTests|SpendControlAdmissionApplicationServiceTests|GSD2-B2-SPEND-CONTROL-ADMISSION-001|GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001|GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001|LedgerNormalBalanceGuard|B7_RECON_DIFFERENCE_REPORT_GREEN_VERIFIED_COMMITTED|GSD2-B7-RECON-DIFFERENCE-REPORT-001|GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001|GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001" docs openspec` | 旧计划不再作为活跃入口，新 Workflow、LWT Goal、W5、README、OpenSpec tasks、wallet 授权准入、route snapshot 回链、账户能力来源准入、支付工具预交易快照、支出控制准入快照、ledger guard、B5-002、B5-003、B7 对象级 Gate 基座、清算 / 结算 consumer 和 B7 差异报告 Green 结果均可追踪，下一 Grant 仍需用户重新确认。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译和测试 | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-cad`、B4/B5/B7 目标测试、`just test-reconciliation`、`just compile`、`just verify-fast`、`just pmd`、`git diff --check`。 | 账户层级来源契约、资金责任目标主体、资金责任解析 facade、支付工具能力准入 facade、边界测试、治理测试、账本、交易、余额控制、业务流和 PMD 完整门禁已在 `e81a8a25` 收口通过；B4/B5/B7-001 已在 `ae8cb8a6` 收口，B7 动作守卫已在 `10853e2d` 完成目标测试、对账分组、compile、verify-fast、PMD 和 diff 收口；B5-003 已完成目标 B5、余额控制、边界、交易、compile 和 PMD 验证；本轮 B7 对象级 Gate 和清算 / 结算 consumer 已完成目标测试、reconciliation 分组、compile 和 PMD 验证。 |

## 10. 残余风险和停止条件

残余风险：

1. GSD-2 只移除活跃未完成计划，不删除旧准入卡中的候选材料；后续 Review 时仍需避免把历史候选误读为活跃计划。
2. `4ef64275` 之后若出现新代码或文档变更，下一轮必须先复核 `git status --short`、diff 和 LWT Goal / W5 / OpenSpec 状态回写。
3. AUTH chargeback 兼容入口、B4-002、钱包后续 facade、ledger guard 首轮、清算 / 结算真实消费方、B5 运营审批和 P2 等候选仍有生产可用缺口，不能因为 B7 gate、对象级阻断基座、出款 preflight、ledger guard 首轮、B5-002 或 B5-003 Green 而自动获得后续编码授权。
4. 涉及真实资金、客户资金、跨境、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规的结论，仍需专业确认。

停止条件：

1. 需要改公共契约、DDL/H2、生产代码、测试代码或运行时配置。
2. 需要 Git add、commit、push、PR、部署、联网、依赖安装或不可逆操作。
3. 发现旧计划和新基线冲突，且无法用迁移指针解释。
4. 连续两轮没有新增证据、状态变化或缺口收敛。
5. 用户调整优先级、暂停、撤销授权或指定新的 Execution Grant。
