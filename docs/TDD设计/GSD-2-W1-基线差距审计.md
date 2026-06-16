# GSD-2 Wave 1 基线差距审计

## 1. 文档定位

本文是 `GSD2-W1-BASELINE-GAP-AUDIT` 的低风险审计报告，用于把最新 Git、设计、OpenSpec、任务队列和运行时 Goal 状态重新对齐，并给出下一轮单一 Execution Grant 的推荐入口。

本文不是 PRD、系分设计、OpenSpec 正文、编码授权、测试写入授权、DDL/H2 schema 授权或 Git 提交授权。本文只允许在默认 Plan Grant 下记录事实、差距、候选优先级、验证门禁和 handoff。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W1-BASELINE-GAP-AUDIT` |
| 所属阶段 | GSD-2 Wave 1 / Baseline Gap Audit / low-risk docs and status only |
| 关联文档 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| 运行时 Goal | 当前会话已创建运行时 Goal，用于持续推进 GSD2 基线审计、单一 Grant 选择和后续授权范围内任务。 |
| 当前状态 | `W1_GAP_AUDIT_DONE_READY_FOR_W2_SINGLE_GRANT_SELECTION` |
| Owner | AI Native 流程编排负责 Goal、Loop、状态和 handoff；产品架构专家负责业务目标和验收口径；资深架构师负责源码、测试、OpenSpec、架构边界和验证门禁。 |
| 写入范围 | 本报告、GSD-2 入口、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-W1-基线差距审计.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、AGENTS.md、最近 Git 提交和旧 GSD/Grant 历史材料。 |
| 只读参考 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、源码、测试、Justfile、AGENTS.md 和最近 Git 提交。 |
| Git 策略 | `summary_only`。本文不授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |

## 2. 当前事实

| 事实项 | 当前结论 | 影响 |
| --- | --- | --- |
| 工作树 | `git status --short` 为空。 | 当前没有未提交变更阻断 W1 文档审计。 |
| 当前 HEAD | `da7d2ea test: 阻断契约夹具承载资金流断言`。 | GSD-2 初始文档中的 `b3b9712` 只能作为 GSD2 建立时的历史基线，不能继续写作当前 Git/code baseline。 |
| GSD-2 初始基线 | `b3b9712 feat: 对齐资金底座GSD基线与交易回放能力`。 | 仍保留为 GSD2 重置点和历史证据。 |
| 最新已提交推进 | `a56bfbc`、`3c71e29`、`a3c2e21`、`4b63996..da7d2ea`。 | 账户层级快照、信用账户路由闭合、GSD2 路由快照契约、DSL 路由/账务计划/契约夹具门禁已有新增证据。 |
| 活跃编码 Grant | 无。 | 后续任何 Java、测试、公共契约、DDL/H2 或运行时配置写入，都必须进入 W2 并确认单一 Execution Grant。 |
| 默认授权 | 低风险文档、状态、只读审计和任务卡。 | 本轮可写 W1 审计和状态载体，不进入 CAD Loop。 |

## 3. 结构化门禁字段

### 3.1 业务目标、能力地图和对象模型

业务目标：把 GSD-2 当前状态从“历史重置点可读”推进到“最新设计、代码、任务和 OpenSpec 可恢复”，为下一单一 Grant 选择提供事实基线。

用户价值：产品、架构、研发和测试能知道当前哪些证据已经提交，哪些只是局部门禁强化，哪些能力仍必须重新授权后才能编码。

成功指标：W1 报告能让后续任务明确当前 Git/code baseline、候选优先级、Not Done 边界、验证方式和下一 owner。

非目标：不声明任何业务能力生产 Done，不发布、不改代码、不改公共契约、不改 DDL/H2、不提交 Git。

能力地图：

| 能力域 | W1 处理口径 |
| --- | --- |
| P0 账本账目、余额投影、数据治理 | 只作为前置事实和历史证据复核，不新增 Red 或实现。 |
| P1 交易、路由、余额控制、交易投影 | 登记 route snapshot、DSL 合约门禁和交易投影解释候选，不改 canonical 入参。 |
| 钱包账户和账户层级 | 作为下一单一 Grant 的推荐入口，聚焦账户层级 contract / replay / no-ddl 或最小 service-flow。 |
| 清结算与对账 | 保持 B7 候选，等待上游事实边界稳定后独立授权。 |
| P2 VCC、全球账户、收单 | 继续作为业务能力包，不绕过 P0/P1 前置能力。 |

业务对象：资金账户、信用账户、父子账户、支付工具、资金责任决策、资金交易、授权交易、余额控制、route snapshot、ledger transaction、LedgerEntry、余额投影、交易投影、对账差错和 GSD/Goal 状态账本。

对象模型和字段口径：W1 只记录对象之间的准入依赖、快照依赖和任务依赖；主体、金额、币种、账目、余额桶、外部引用、幂等键、审计引用和生命周期字段不在本轮变更。

生命周期 / 状态：当前状态从 `PLAN_GRANT_ACTIVE_LOW_RISK_DOCS_AND_BASELINE_ONLY` 收敛到 `W1_GAP_AUDIT_DONE_READY_FOR_W2_SINGLE_GRANT_SELECTION`；下一状态只能由 W2 选择单一 Grant 后进入。

Wave 边界：W1 只做只读审计和状态回写，W2 才做单一 Grant 选择，W3 才能进入 CAD Loop。执行顺序必须是 W1 审计完成后再进入 W2；依赖关系是账户层级优先于资金责任目标主体、支付工具应用入口、VCC 和全球账户 P2 能力。并行边界是同一时间只允许一个 Grant 进入 active，涉及公共契约、状态机、fixture 或 H2 schema 的候选不得并行推进；互不重叠的低风险文档同步可以在 Plan Grant 内完成。

### 3.2 业务流程、规则矩阵和运营数据口径

业务流程：

1. 读取最新用户目标、运行时 Goal、AGENTS、GSD-2 状态载体和 Git 状态。
2. 只读复核 PRD、DSL、系分、TDD、OpenSpec、最近提交和历史准入卡。
3. 识别当前事实、合理推断、待确认和范围外不做。
4. 回写 W1 差距审计、GSD-2 入口、README 和 OpenSpec tasks。
5. 运行本地文档门禁、引用扫描和空白检查。
6. 交接到 W2 单一 Grant 选择。

主流程终态：W1 审计完成，能推荐下一候选并说明不可直接编码的边界。

异常流程：若发现需要代码、测试、DDL/H2、公共契约、Git、联网或生产动作，则停止并转 W2 或显式确认。

人工兜底：优先级由用户确认；业务目标和验收由产品架构专家确认；写入范围、Red、验证命令和工程风险由资深架构师确认。

规则矩阵：

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验证方式 | 确认方 |
| --- | --- | --- | --- | --- | --- | --- |
| 当前基线校准 | 文档基线与 Git HEAD 不一致。 | 以当前 HEAD 为实际 Git/code baseline，历史提交只作重置点。 | P0 | 2026-06-12 | `git log --oneline -12`、`rg`。 | 资深架构师 |
| 单一 Grant | 需要进入代码、测试、契约或 DDL 写入。 | 必须只选一个 Task ID，补齐写入范围和验证命令。 | P0 | 2026-06-12 | W2 任务卡和 Harness checker。 | 用户 + 资深架构师 |
| P2 不抢跑 | VCC、全球账户或收单要求提前实现。 | 必须证明账户层级、资金责任、交易内核和对账前置已满足。 | P0 | 2026-06-12 | 准入卡、AC/RED、专项验证。 | 产品架构专家 + 资深架构师 |
| 局部证据不等于 Done | DSL 门禁或 route snapshot 测试增强。 | 只能作为前置保护，不能声明 service-flow 或生产能力完成。 | P0 | 2026-06-12 | 交付说明、Not Done 清单。 | 资深架构师 |

运营后台 / 数据口径：W1 不新增运营后台或报表能力；后续 Grant 必须继续区分资金事实、账本事实、余额投影、交易投影、对账差错、任务状态和验证证据。

风险 / 待确认 / 验收：风险是把历史候选、局部 DSL 合约门禁或 route snapshot 证据误读成生产能力完成；待确认项是 W2 具体选择哪个 Grant；验收口径是文档状态能被 GSD2 入口、README 和 OpenSpec tasks 同步引用。

发布：本文不发布生产能力；任何灰度、发布、回滚、告警或生产验收必须在具体 Grant 的实现和验证完成后另行评审。

### 3.3 架构背景、核心决策和验证发布风险

背景：GSD-2 初始重置点之后已经出现新的账户层级、路由快照和 DSL 契约门禁提交，旧 `b3b9712` 状态不能继续代表当前实际基线。

目标：完成设计、代码、任务和 OpenSpec 的 W1 差距审计，进入可审的 W2 单一 Grant 选择。

现状和影响范围：影响范围只覆盖 docs、OpenSpec tasks 和状态载体；生产模块、测试模块、DDL/H2 schema、公共契约和运行时行为不在本轮写入范围。

约束和问题：当前活跃编码 Grant 为空，默认 Plan Grant 只允许低风险文档和状态推进；问题是旧基线指针滞后于实际 HEAD。

核心决策和取舍：

1. 当前 Git/code baseline 校准为 `da7d2ea`。
2. `b3b9712` 降级为 GSD-2 初始重置点。
3. 下一步不直接进入 CAD Loop，而是进入 W2 单一 Grant 选择。
4. 推荐优先账户层级，是因为它是 VCC、全球账户、资金责任目标主体和支付工具绑定语义的前置。

职责边界：AI Native 负责编排和状态；产品架构专家负责业务目标、能力地图和验收；资深架构师负责接口契约、入参、出参、错误码、幂等、兼容、TDD、验证和工程风险。

接口契约：本轮不新增或修改任何接口契约、入参、出参、错误码、幂等摘要、DTO、Request、Query、枚举或状态机。

数据方案：本轮不新增或修改数据方案、表、索引、Entity、Mapper、H2 schema 或迁移脚本。

事务边界、一致性、补偿和对账：本轮只记录差距，不改变资金事务边界、一致性、补偿、对账或余额投影行为。

可靠性、安全、权限、审计和告警：本轮的可靠性风险是状态漂移；安全风险是误把文档授权扩大到代码、Git、生产或真实资金动作。控制方式是只读边界、停止条件、验证矩阵和 handoff。

验证方案：运行 Harness checker、产品结构 checker、架构结构 checker、`rg` 状态一致性扫描和 `git diff --check`；本轮不运行编译、测试、静态检查、压测或回归测试。

发布、灰度、回滚、风险和待确认：本文不发布生产能力；文档变更可通过 Git diff 回滚；待确认项是 W2 的单一 Grant 选择。

## 4. 差距审计

| 差距 ID | 差距 | 当前证据 | 裁决 |
| --- | --- | --- | --- |
| `W1-GAP-BASELINE-001` | GSD-2 入口和 OpenSpec tasks 仍把 `b3b9712` 写成当前 Git/code baseline。 | 当前 HEAD 已是 `da7d2ea`，且后续提交包含账户层级和 DSL 合约门禁强化。 | 必须把当前基线校准为 `da7d2ea`，同时保留 `b3b9712` 为历史重置点。 |
| `W1-GAP-ACCOUNT-001` | `B2-ACCOUNT-HIERARCHY` 已有局部代码/测试证据，但任务状态仍停在候选准入语义。 | `a56bfbc`、`3c71e29` 和 `a3c2e21` 已推进账户层级快照、信用账户路由闭合和 route snapshot 契约。 | 只能声明“局部证据已前进”，不能声明账户层级、开户落账、余额、父子汇总或 VCC funding 生产 Done。 |
| `W1-GAP-DSL-001` | DSL fixture 和合约门禁已经强化，但需要避免被误读为业务能力完整交付。 | `4b63996..da7d2ea` 连续强化 route 金额闭合、节点记账主体、参与方引用、上下文、账务分录周期、账务计划平衡、夹具等级和契约夹具资金流阻断。 | 可作为后续 Grant 的前置保护证据；不能替代 service-flow、公共契约或生产能力证明。 |
| `W1-GAP-GRANT-001` | 下一步候选仍有多个，尚未选择单一 Grant。 | GSD-2 候选队列包括账户层级、资金责任目标主体、对账差错、交易投影解释和余额调整审计。 | 必须进入 `GSD2-W2-SINGLE-GRANT-SELECTION`，同一时间只激活一个 Grant。 |
| `W1-GAP-P2-001` | VCC、全球账户等 P2 能力仍容易被业务目标牵引提前开工。 | P2 候选仍依赖账户层级、资金责任、交易内核、投影解释、清结算和对账差错证据。 | P2 只能作为后续业务能力包；未闭合 P0/P1 前不建议直接编码。 |

## 5. 下一候选优先级

| 优先级 | 候选 | W1 结论 | W2 建议 |
| --- | --- | --- | --- |
| 1 | `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` | 最近提交已经围绕账户层级、VCC 子账户快照和 route snapshot 形成连续证据，且它是 VCC、全球账户和资金责任目标主体的前置。 | 推荐作为下一单一 Grant，先做剩余 contract / replay / no-ddl 或最小 service-flow 证明，不声明完整钱包生产 Done。 |
| 2 | `GSD2-B2-FR-TARGET-001` | 资金责任目标主体迁移仍是 VCC、信用账户和平台责任主体生产可用的关键缺口。 | 建议排在账户层级之后；若提前选择，必须说明 schemaGate 和兼容策略。 |
| 3 | `GSD2-B7-RECON-DIFFERENCE-MVP-001` | 清结算与对账是生产可用验收的硬门禁，但依赖上游事实边界稳定。 | 如果目标切向清结算闭环，可作为下一 Grant；否则排在账户层级之后。 |
| 4 | `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` | DSL 与 route replay 证据增强后，交易投影解释更适合做只读解释或 projection-store-backed 切片。 | 不改 canonical 交易入参，不新增统一支付工具交易内核。 |
| 5 | `GSD2-B5-BALANCE-ADJUST-AUDIT-001` | 外部钱包/VCC 上游余额异常和运营调账需要审批、原因、审计、幂等和对账回链。 | 可作为余额控制专项 Grant，但不能绕过差错和审批白名单。 |

## 6. 推荐单一 Grant

推荐下一轮进入 `GSD2-W2-SINGLE-GRANT-SELECTION`，优先选择：

```text
Execution Grant Candidate:
GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001
```

推荐理由：

1. 它是 VCC 卡绑定资金/信用子账户、父账户快照、全球账户钱包和资金责任目标主体的共同前置。
2. 当前 Git 证据已经在账户层级和 route snapshot 方向上前进，继续收口比切换到 P2 或清结算深水区风险更低。
3. 该候选可以先保持 `contract-only/no-ddl` 或明确的最小 service-flow，不必一次打开开户落账、父子汇总、余额可用、VCC funding 或外部通道规则。

## 7. W2 准入字段

若用户确认下一 Grant，W2 必须至少补齐下列字段：

| 字段 | 必填口径 |
| --- | --- |
| `authorityBaseline` | 当前 Git HEAD，至少包含 `da7d2ea` 及本 W1 审计状态。 |
| `implementationDecision` | `contract-only/no-ddl`、`ledger-snapshot-backed` 或其他明确裁剪。 |
| `writeScope` | 具体文件、模块、测试资产和是否允许公共契约变化。 |
| `noWriteScope` | 不写 Java/测试/DDL/公共契约的范围，或若写则逐项明确。 |
| `firstRedSet` | 首批 Red、业务问题、资金不变量、forbidden facts 和验证命令。 |
| `verificationCommand` | `just test-one`、分组测试、`just compile`、`just pmd` 或 `just verify-slice` 的最小集合。 |
| `stopCondition` | 越过写入范围、需要 DDL/H2、公共契约、外部规则、Git 或生产动作时停止。 |

## 8. 不做范围

本轮 W1 不做：

1. 不写 Java、测试代码、公共契约、DDL/H2 schema、Entity、Mapper、运行时配置或 CI。
2. 不处理 Git add、commit、push、PR、merge、rebase、reset 或分支切换。
3. 不把 `B2-ACCOUNT-HIERARCHY` 的局部证据声明为钱包账户、VCC funding、父子账户汇总或全球账户生产 Done。
4. 不把支付工具、预算组、Spend Rule、交易投影或 DSL fixture 写成 ledger subject。
5. 不给真实资金、跨境、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规最终结论。

## 9. Execution Handoff Card

| 字段 | 内容 |
| --- | --- |
| 当前 Wave / Task | `GSD2-W1-BASELINE-GAP-AUDIT` |
| 当前状态 | `W1_GAP_AUDIT_DONE_READY_FOR_W2_SINGLE_GRANT_SELECTION` |
| 下一 Wave / Task | `GSD2-W2-SINGLE-GRANT-SELECTION` 已形成草案，详见 [GSD-2-W2-单一Grant选择卡.md](GSD-2-W2-单一Grant选择卡.md)。 |
| 推荐 Grant | `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` |
| 写入范围 | 下一轮只能写所选 Grant 的任务卡、AC 表、Red 卡、验证矩阵和状态账本；源码级写入需用户确认 Grant。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、最近 Git 提交和历史准入卡。 |
| 反馈源 | `git status --short`、`git log --oneline`、`rg`、Harness checker、产品/架构 checker、专项测试和用户确认。 |
| Git 策略 | `summary_only`，除非用户后续明确要求提交并且验证通过。 |
| 停止条件 | 需要代码/测试/DDL/公共契约/Git/联网/生产动作，或用户改变优先级。 |

## 10. 验证矩阵

| 验证层 | 命令或方式 | 通过口径 |
| --- | --- | --- |
| 工作树状态 | `git status --short` | 无未提交变更作为 W1 输入阻断；本轮新增文档变更在交付时列明。 |
| 基线证据 | `git log --oneline -12` | 能看到 `da7d2ea` 以及账户层级、GSD2 和 DSL 契约门禁提交链。 |
| 状态一致性 | `rg "GSD2-W1|W1_GAP_AUDIT|da7d2ea" docs openspec` | GSD2、README 和 OpenSpec tasks 能追踪到 W1 审计和当前 Git baseline。 |
| Harness 结构 | `check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-W1-基线差距审计.md` | Task、Owner、写入/只读范围、Wave、验证和 handoff 字段齐全。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译、测试、PMD | 本轮不运行。 | 本轮仅文档和状态审计，不改 Java、测试、DDL/H2 或运行时配置。 |
