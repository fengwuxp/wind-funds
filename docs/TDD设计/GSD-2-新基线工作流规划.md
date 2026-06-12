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
| 当前状态 | `PLAN_GRANT_ACTIVE_LOW_RISK_DOCS_AND_BASELINE_ONLY` |
| Git / code baseline | `b3b9712 feat: 对齐资金底座GSD基线与交易回放能力`。当前仅允许低风险文档、状态和任务卡推进；工作树中的 `openspec` staged 删除与未跟踪同名文件状态不纳入本轮默认写入范围。 |
| 设计 baseline | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec` 的当前可读状态；`openspec` 异常 Git 状态只作为停止条件和只读事实记录。 |
| 活跃未完成计划 | 已清零。旧候选不再作为当前计划，只能在新 Workflow 中被重新选择、重新编号、重新确认。 |
| Owner | AI Native 流程编排负责状态、Loop、GSD 和门禁；产品架构专家负责业务目标、对象、能力、验收和金融待确认；资深架构师负责系统边界、接口、TDD、验证和编码准入。 |
| 写入范围 | 本文、Agent Loop / Plan Grant 默认授权策略、AI 代码交付闭环基线、GSD-2 单一 Grant 任务模板、`docs/TDD设计/README.md`、`docs/README.md` 和旧 GSD 计划的迁移指针。 |
| 写入文件 | `docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md`、`docs/TDD设计/AI代码交付闭环与Spec模板基线.md`、`docs/TDD设计/GSD-2-Spec-AC-Harness-CAD任务模板.md`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md`、`docs/TDD设计/README.md`、`docs/README.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、最近 Git 提交和旧 GSD/Grant 历史材料。 |
| 只读参考 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、源码、测试和 Git 提交记录。 |
| Git 策略 | `summary_only`。本轮不自动 `git add` / `git commit`；若用户后续明确要求提交，再按项目 Git 规则执行。 |

## 2. 旧计划移除裁决

移除不是删除历史证据，而是从当前活跃执行队列中移除未完成计划。历史准入卡、Round 0、Execution Grant 消费记录、验证结果和 Not Done 边界继续保留，用于审计和下一轮重新选择。

| 处理对象 | 新状态 | 说明 |
| --- | --- | --- |
| 旧 GSD Goal 计划 | `SUPERSEDED_BY_GSD2_BASELINE_RESET` | `GSD-Goal-生产可用MVP推进计划.md` 保留为上一轮状态账本和历史证据，不再承载新的活跃计划。 |
| 旧 Agent Loop | `CLOSED_AS_HISTORY` | `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-LOOP-2026-06-11` 已关闭为历史 Loop。 |
| 已消费 Grant | `CONSUMED_HISTORY_ONLY` | 不得复用，例如账本 002A、003、004A 和 B3 直接退款引用回放。 |
| 未确认候选 | `BACKLOG_REFERENCE_NOT_ACTIVE_PLAN` | B2、B4、B5、B6/B8、B7、P2 VCC、P2 全球账户等候选均需在 GSD-2 中重新选择后才可进入计划。 |
| 当前活跃执行队列 | `EMPTY` | 当前没有可继续编码的 Execution Grant，也没有默认可自动执行的 Java / DDL / 公共契约任务。 |

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

背景：上一轮已经把交易回放、直接退款原交易引用、预算组兼容 guard 和 GSD 任务账本提交到 `b3b9712`。当前需要停止在旧计划上继续堆叠候选，改为先建立新的可审工作流。

目标：用 GSD-2 固定当前设计、代码和任务基线，清理活跃队列，重新定义下一轮只读侦察、Gap Audit、单一 Grant 选择和 CAD 执行的顺序。

非目标：本轮不改生产代码、测试代码、公共契约、DDL/H2 schema、运行时配置、状态机或模块依赖方向。

成功标准：文档能说明当前现状、核心决策、接口契约、数据方案、事务边界、一致性、补偿、对账、可靠性、安全、权限、审计、告警、验证方案、测试、静态检查、回归、发布、灰度、回滚、风险和待确认。

| 架构项 | 当前基线 |
| --- | --- |
| 现状和影响范围 | 代码基线为 `b3b9712`；影响范围仍覆盖 core、wallet、transaction、ledger、reconciliation、governance、tests、docs 和 openspec。 |
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
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试和 `b3b9712` 之后的 diff。 |
| 验收场景 | 重新列出设计与代码差异、生产可用缺口、下一可选 Grant 和 Not Done 边界。 |
| AI 交付闭环 | 每个缺口需标注建议 Spec 强度、AC 覆盖方式、独立验证证据和是否需要人工主导。 |
| 验证命令 | `rg` 追踪、`git status --short`、必要时只读源码锚点扫描；若不改代码，不要求编译。 |
| 停止条件 | 发现需要公共契约、DDL/H2、生产代码或跨能力域写入时，停止并转 Wave 2 Grant 选择。 |

### Wave 2：重新选择单一 Grant

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W2-SINGLE-GRANT-SELECTION` |
| Owner | 用户确认优先级；产品架构专家确认业务价值和验收；资深架构师确认写入范围、Red、验证命令和风险。 |
| 写入范围 | 仅所选 Grant 的任务卡、Red 卡、验证矩阵和状态账本。 |
| 只读范围 | 所有候选准入卡和源码证据。 |
| Spec 准入 | 必须消费 `AI代码交付闭环与Spec模板基线.md` 并复制 `GSD-2-Spec-AC-Harness-CAD任务模板.md` 或等价任务卡，补齐 Spec ID、Spec 强度、AC 表、Goal / AC 映射、验证命令、CR 交接和知识回流位置。 |
| 并行边界 | 同一时间只允许一个 Grant 进入 active；共享公共契约、状态机、fixture 或 H2 schema 的候选必须串行。 |
| 候选队列 | `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001`、`GSD2-B2-FR-TARGET-001`、`GSD2-B7-RECON-DIFFERENCE-MVP-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-001`。 |
| 默认建议 | 若业务仍以 VCC 和全球账户为目标，优先 `B2-ACCOUNT-HIERARCHY` 的 `contract-only/no-ddl`，因为卡绑定子账户、父账户快照和账目 profile 是 VCC/全球账户后续资金流前置。 |
| 停止条件 | 用户未确认优先级、Grant 字段缺写入范围、验证命令或停止条件；或候选需要外部规则、专业确认、生产配置、联网、依赖安装、Git push、部署或不可逆操作。 |

### Wave 3：CAD Loop 执行

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W3-CAD-LOOP-ACTIVE`，仅在 Wave 2 选定具体 Grant 后派生子任务。 |
| Owner | 资深架构师。 |
| 写入范围 | 只限被确认 Grant 的文件、模块、测试资产和最小实现。 |
| 只读范围 | 关联 PRD、DSL、系分、TDD、OpenSpec、旧准入卡和源码锚点。 |
| 验收场景 | Red 符合预期，Green 最小实现，资金不变量、失败无副作用、幂等、审计和回归证据齐备。 |
| AI 交付闭环 | 每轮遵循读取状态、选择最小动作、执行变更、独立验证、读取反馈、回写 Spec/AC/TDD/Harness、判断继续或停止。 |
| 验证命令 | 按 Grant 运行 `just test-one`、相关分组测试、`just compile`、`just pmd` 或 `just verify-cad`。 |
| Review | 每轮代码后做问题优先 CR，检查业务语义、边界方向、契约完整性、失败路径和工程一致性。 |
| 停止条件 | 验证失败无法在授权范围内修复、工作树冲突、公共契约越界、生产风险升级或用户中断。 |

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
| 状态载体 | 本文、`GSD-2-AgentLoop-PlanGrant默认授权策略.md`、`AI代码交付闭环与Spec模板基线.md`、`GSD-2-Spec-AC-Harness-CAD任务模板.md`、TDD README、docs README 和 Git HEAD。 |
| 决策输入 | 用户目标、当前 Git 状态、旧计划迁移裁决、PRD/DSL/系分/TDD/OpenSpec、源码和验证结果。 |
| 允许动作 | 低风险文档同步、状态回写、只读 Gap Audit、Spec/AC/Harness/CAD 任务卡、验证矩阵和下一 Grant 草案。 |
| 禁止动作 | 未确认新 Grant 前，不写 Java、测试代码、公共契约、DDL/H2 schema、运行时配置，不处置 `openspec` staged/untracked 异常状态，不做 Git add/commit/push、联网、依赖安装、生产配置或不可逆操作。 |
| 反馈源 | checker、`rg` 扫描、`git status --short`、`git diff --check`、用户确认和后续专项验证。 |
| 验证者 | 文档结构由脚本验证；产品语义由产品架构专家确认；工程边界由资深架构师确认；优先级由用户确认。 |
| 预算 / 最大轮次 | 每轮最多 1 个低风险本地任务；连续 2 轮没有新增证据、状态变化或缺口收敛时暂停。 |
| 无进展检测 | 如果连续两轮只是重复旧候选状态，没有新增证据、差异、验证或用户确认，则停止扩写并等待用户选择下一 Grant。 |
| 停止条件 | 触发公共契约、DDL/H2、生产代码、外部规则专业确认、工作树冲突、验证失败、工具审批、Git 操作或用户中断。 |
| 失败回写 | 本文、Agent Loop / Plan Grant 默认授权策略、AI 代码交付闭环基线、GSD-2 单一 Grant 模板和对应候选准入卡。 |
| 交接物 | 新基线裁决、AI 代码交付闭环、Backlog reference、候选优先级、验证矩阵和 Execution Handoff Card。 |

## 7. 下一候选优先级

下列候选是 backlog reference，不是活跃计划。任何一项进入执行前都必须重新确认单一 Execution Grant。

| 优先级 | 候选 | 建议级别 | 理由 | 不做范围 |
| --- | --- | --- | --- | --- |
| 1 | `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` | `contract-only/no-ddl` | VCC 卡绑定资金/信用子账户、父账户快照、账目 profile、卡绑定摘要和全球账户钱包都依赖账户层级口径。 | 不声明开户落账、账本初始化、余额可用、父子账户汇总生产 Done 或 VCC funding。 |
| 2 | `GSD2-B2-FR-TARGET-001` | `contract-only` 或 `service-flow-backed` | VCC、信用账户和平台责任需要 `targetSubjectType + targetSubjectId` 或等价主体引用。 | 不把 `fundingAccountId` 继续写成所有责任来源的唯一事实；不混入支付工具能力准入。 |
| 3 | `GSD2-B7-RECON-DIFFERENCE-MVP-001` | `service-flow-backed` | 清结算与对账需要最小差错闭环，才能支持 VCC clearing、全球账户出入金和运营补事实。 | 不一次性打开完整清分、清算、结算、出款、追偿和运营后台。 |
| 4 | `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` | `service-flow-backed` 或 `projection-store-backed` | B3/B4 已有回放局部证据，下一步可补交易投影解释或查询证据。 | 不改 canonical 入参，不新增统一支付工具交易服务。 |
| 5 | `GSD2-B5-BALANCE-ADJUST-AUDIT-001` | `service-flow-backed` | 余额调账、外部余额异常和运营补事实需要审批、原因、审计、幂等和对账回链。 | 不把普通调账绕过对账差错和审批白名单。 |
| 6 | 支付工具 / Spend Rule 支持 | `contract-only` 起步 | 工具动作能力、授权 application facade、Spend Rule 控制活动和只读解释依赖账户、资金责任、交易内核和对账。 | 不把支付工具、预算组或 Spend Rule 写成 ledger subject。 |
| 7 | P2 VCC / 全球账户 | `contract-only` 起步 | 业务目标重要，但必须消费账户、资金责任、交易内核和对账差错证据。 | 不直接写 P2 facade、资金流、外部轨道或通道规则生产结论。 |
| 8 | 收单 | `design-only` | 当前不是 MVP 实现优先级。 | 不写 capture/dispute 生产代码、测试或 DDL。 |

## 8. Execution Handoff Card

| 字段 | 内容 |
| --- | --- |
| Goal ID | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| Wave / Task ID | 当前为 `GSD2-W0B-AGENT-LOOP-PLAN-GRANT-2026-06-12`；下一步可默认进入低风险 `GSD2-W1-BASELINE-GAP-AUDIT`，或等待用户选择 `GSD2-W2-SINGLE-GRANT-SELECTION`。 |
| 状态载体 | 本文、Agent Loop / Plan Grant 默认授权策略、AI 代码交付闭环基线、GSD-2 单一 Grant 任务模板、TDD README、docs README。 |
| 写入范围 | 本轮仅文档和任务基线；后续按单一 Grant 限定。 |
| 只读范围 | 全部设计文档、OpenSpec、源码、测试、旧状态账本和 Git 提交。 |
| 反馈源 | checker、`rg`、`git status --short`、`git diff --check`、用户确认和专项测试。 |
| 验证命令 | 本轮文档检查；后续 Grant 按任务指定。 |
| AI 交付准出 | 后续 Grant 必须列出 Spec/AC 映射、Red/Green 证据、独立验证命令、CR 交接、Not Done、知识回流和建议 commit message。 |
| 停止条件 | 越过文档范围、需要代码/DDL/公共契约/Git 授权、需要处置 `openspec` 异常状态、验证失败或用户调整优先级。 |
| Git 策略 | `summary_only`。 |
| 下一 owner | 用户确认优先级后，产品架构专家补业务验收种子，资深架构师补单一 Grant 和 TDD/CAD 执行包。 |

## 9. 验证矩阵

| 验证层 | 命令或方式 | 当前通过口径 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-新基线工作流规划.md` | GSD Wave 必备字段齐全。 |
| AI 代码交付闭环结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/AI代码交付闭环与Spec模板基线.md` | Spec 准入、AC 映射、Harness、验证和交接字段齐全。 |
| 单一 Grant 任务模板结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-Spec-AC-Harness-CAD任务模板.md` | Spec、AC、Red、Harness、CAD、CR 和交付证据字段齐全。 |
| Agent Loop / Plan Grant 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md` | 默认授权范围、Loop 契约、验证者、停止条件、handoff 和显式确认边界齐全。 |
| 产品架构结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-新基线工作流规划.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构方案结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-新基线工作流规划.md` | 背景目标、边界取舍、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg` 扫描旧 Loop、旧 active status 和新 GSD2 指针 | 旧计划不再作为活跃入口，新 Workflow 可追踪。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译和测试 | 本轮不运行 | 仅文档和任务基线变更，不改 Java、测试、DDL/H2 或运行时配置。 |

## 10. 残余风险和停止条件

残余风险：

1. GSD-2 只移除活跃未完成计划，不删除旧准入卡中的候选材料；后续 Review 时仍需避免把历史候选误读为活跃计划。
2. `b3b9712` 之后若出现新代码或文档变更，下一轮必须先复核 `git status --short` 和 diff。
3. B2、B4、B7、P2 等候选仍有生产可用缺口，不能因为进入 GSD-2 而自动获得编码授权。
4. 涉及真实资金、客户资金、跨境、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规的结论，仍需专业确认。

停止条件：

1. 需要改公共契约、DDL/H2、生产代码、测试代码或运行时配置。
2. 需要 Git add、commit、push、PR、部署、联网、依赖安装或不可逆操作。
3. 发现旧计划和新基线冲突，且无法用迁移指针解释。
4. 连续两轮没有新增证据、状态变化或缺口收敛。
5. 用户调整优先级、暂停、撤销授权或指定新的 Execution Grant。
