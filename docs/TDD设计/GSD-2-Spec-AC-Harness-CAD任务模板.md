# GSD-2 Spec / AC / Harness / CAD 任务模板

## 1. 文档定位

本文是 GSD-2 下选择单一 Grant 前必须填写的任务模板，用于把业务目标、Spec、AC、TDD Red、Harness、CAD Loop、验证命令、CR 交接和交付证据压到同一张可审任务卡里。

本文不是编码授权、不是 OpenSpec 正文、不是 PRD 或系分设计，也不是 Git 提交授权。默认 Plan Grant 只允许低风险填写、修订和验证本模板；只有当用户确认具体 Execution Grant 或 CAD Grant，且本模板的写入范围、验证命令、停止条件、风险和 Git 策略齐全后，才允许进入对应代码或测试写入。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-TEMPLATE-SPEC-AC-HARNESS-CAD-2026-06-12` |
| 原子任务 | 建立 GSD-2 单一 Grant 可填写模板。 |
| 所属阶段 | GSD-2 Wave 0A / Delivery Template / Planning-only。 |
| Goal ID | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| Loop ID | `GSD2-LOOP-AI-CODE-DELIVERY-2026-06-12` |
| 当前状态 | `PLAN_GRANT_ACTIVE_LOW_RISK_TEMPLATE_ONLY` |
| Owner | AI Native 流程编排负责模板和门禁；产品架构专家负责业务目标、验收种子和产品风险；资深架构师负责系统边界、TDD、实现路径、验证和 CR。 |
| 写入范围 | 本模板、Agent Loop / Plan Grant 默认授权策略及入口引用。 |
| 写入文件 | `docs/TDD设计/GSD-2-Spec-AC-Harness-CAD任务模板.md`、`docs/TDD设计/GSD-2-AgentLoop-PlanGrant默认授权策略.md`、`docs/TDD设计/AI代码交付闭环与Spec模板基线.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/README.md`、`docs/README.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、AGENTS.md、历史准入卡和旧 Grant 消费记录。 |
| Git 策略 | `summary_only`。填写模板不等于 `git add`、`git commit`、push、PR 或发布授权；默认授权口径以 `GSD-2-AgentLoop-PlanGrant默认授权策略.md` 为准。 |

## 2. 使用规则

任一 GSD-2 候选从 backlog reference 进入执行前，先复制本模板并替换占位内容。模板字段允许简短，但不得留空后让 Agent 猜。

| 使用阶段 | 目标 | 准出条件 |
| --- | --- | --- |
| Wave 0B Plan Grant | 在低风险范围内填写、修订和检查任务卡。 | 不触碰代码、测试、DDL/H2、公共契约、OpenSpec 异常状态或 Git 操作。 |
| Wave 1 Gap Audit | 只读复核设计、代码和任务差异。 | 列出缺口、建议 Spec 强度、风险、owner 和验证方式。 |
| Wave 2 Single Grant Selection | 把一个候选升级为可审 Grant。 | 完成 Spec Card、AC 表、Harness 摘要、停止条件和 Git 策略。 |
| Wave 3 CAD Loop | 在授权范围内做 Red/Green/验证/CR/回写。 | 每轮只处理一个原子任务，验证失败可定位并回写。 |
| Wave 4 Verify Handoff | 汇总交付证据和下一 owner。 | 覆盖清单、验证命令、Not Done、残余风险和建议提交切片齐全。 |

不得使用本模板做下列事情：

1. 绕过 PRD、DSL、系分、TDD 或 OpenSpec 的事实源。
2. 把旧候选或历史 Grant 自动恢复为 active。
3. 未确认写入范围就写 Java、测试、DDL/H2、公共契约或运行时配置。
4. 用 mock、内存版 Service、fixture 或示例代码冒充生产能力。
5. 把测试通过、Goal Active 或模板填写完成写成上线批准。
6. 把 `GSD-2-AgentLoop-PlanGrant默认授权策略.md` 中的低风险默认授权扩大为源码级 CAD 授权。

## 3. 产品语义预检

进入 Spec Card 前，先确认本任务的业务对象、生命周期、业务流程和规则矩阵。产品语义不清时，停止在 Wave 1 或 Wave 2，不进入 CAD Loop。

| 字段 | 填写口径 |
| --- | --- |
| 业务目标 | 本任务服务哪个 MVP 资金底座目标，用户价值和成功指标是什么。 |
| 能力地图 | 归属 P0/P1/P2 哪个能力域，依赖哪些上游能力，是否是被依赖方优先。 |
| 业务对象 | 资金账户、信用账户、父子账户、支付工具、资金责任、交易、账本、余额投影、对账差错等对象是否涉及。 |
| 对象模型 | 对象之间的引用、归属、快照、生命周期和状态是否明确。 |
| 字段口径 | 主体、金额、币种、账目、余额桶、外部引用、幂等键、审计引用是否稳定。 |
| 生命周期 / 状态 | 创建、处理中、成功、失败、撤销、过期、差错、人工处理等状态是否有边界。 |
| 业务流程 | 主流程、异常流程、人工兜底和回放/重跑流程是否能画成步骤。 |
| 规则矩阵 | 触发条件、判断逻辑、优先级、版本、验证方式和确认方是否明确。 |
| 运营后台 / 数据口径 | 使用者如何查询、解释、审计、报表、告警和处理失败。 |
| 风险 / 待确认 / 验收 | 外部规则、合规、财务、风控、安全、发布和人工确认项是否列出。 |

产品侧规则矩阵：

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验证方式 | 确认方 |
| --- | --- | --- | --- | --- | --- | --- |
| 待填 | 待填 | 待填 | P0/P1/P2 | 待填 | 测试 / 静态检查 / 人工确认 | 待填 |

流程状态模板：

| 场景 | 主流程 | 异常流程 | 人工兜底 | 终态 | 不允许发生 |
| --- | --- | --- | --- | --- | --- |
| 待填 | 待填 | 待填 | 待填 | 待填 | 半截事实 / 重复副作用 / 越权 / 敏感泄露 / 投影反写 |

## 4. Spec Card

```text
Spec ID:
关联 Goal:
关联 Wave / Task:
状态: Draft / Review / Approved / Implementing / Verified / Superseded
Owner:
协作 Owner:
来源材料:
文档层级: PRD / SDD / 实现 Spec / 混合但已分层
规范事实源:
风险等级: P0 / P1 / P2 / P3
Spec 强度: 轻量任务卡 / 可评审 Spec / Harness-GSD Spec / CAD 候选 Spec / 人工主导
当前结论:
需要人拍板:

0. 摘要
目标:
非目标:
本期成功:
本次不做:
Goal 成功标准:
预算 / 时间盒:
停止条件:

1. 背景与上下文
业务意图:
用户 / 主体 / 验收方:
证据来源:
产品上下文 / PRD 链接:
DSL case / 契约链接:
系分入口:
TDD 入口:
项目知识引用:
隐性依赖 / 历史坑点:
输入输出示例:

2. 系统设计与实现空间
入口路径:
影响模块:
核心对象 / 状态 / 不变量:
接口 / 协议 / DDL / 配置:
结构化契约:
时序 / 流程 / 事务边界:
一致性 / 补偿 / 对账:
可靠性 / 安全 / 审计:
方案取舍:
写入范围:
只读范围:
正例:
反例:
禁止事项:

3. 质量保障
AC 表:
Goal / AC 映射:
测试策略:
Red 候选:
五支柱验证:
静态检查:
漂移检查:
人工确认:

4. 交付发布
Harness 摘要:
验证命令:
发布 / 灰度:
监控:
回滚:
残余风险:
知识回流:
建议 commit message:
```

## 5. AC 表模板

每个 P0/P1 AC 只验证一个业务事实、契约事实或质量事实。资金变化 AC 必须同时写 expected facts 和 forbidden facts。

| AC ID | 关联 Goal / 成功标准 | 分组 | Given | When | Then | Forbidden Facts | 验证方式 | 证据位置 | Owner | 风险等级 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `AC-GSD2-XXX-001` | 待填 | 正常路径 / 异常路径 / 幂等 / 权限 / 数据一致性 / 发布观测 | 待填 | 待填 | 待填 | 待填 | 测试 / 静态检查 / 人工确认 | 待填 | 待填 | P0/P1/P2 | Draft |

填写规则：

1. `Given` 必须能准备测试数据或说明人工确认方。
2. `When` 必须落到服务入口、指令、事件、任务或查询动作。
3. `Then` 必须能被断言或观测。
4. `Forbidden Facts` 必须写清不允许出现的半截 route、posting、entry、投影、外部出款、敏感导出、治理反写或重复资金副作用。
5. `Waived` 不是通过，必须说明豁免原因、影响、替代证据和批准方。

## 6. Red 卡模板

Red 卡用于进入实现前先定义失败反馈。只要任务涉及资金变化、状态流转、幂等、对账、清结算、余额、投影、权限或审计，就必须至少有一个 Red 或明确不适用原因。

| redId | targetBehavior | preconditions | action | expectedFacts | forbiddenFacts | assertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `RED-GSD2-XXX-001` | 待填 | 待填 | 待填 | 待填 | 待填 | 待填 | 测试类 / fixture / H2 数据 | `just test-one <TestClass> [module]` | 待填 |

最低断言清单：

| 资金事实 | 适用时必须断言 |
| --- | --- |
| 交易事实 | 状态、金额、币种、幂等键、原事实引用、审计引用。 |
| route | route snapshot、账户主体、支付工具快照、资金责任决策。 |
| posting | posting plan 平衡、借贷方向、账目、账本周期。 |
| ledger | ledger transaction、LedgerEntry、余额桶影响。 |
| projection | 余额投影或交易投影只读派生、可回放、失败不反写事实。 |
| 失败路径 | 无半截 route、posting、entry、外部出款、敏感导出或重复资金副作用。 |

## 7. Harness 摘要模板

```text
Task ID:
Goal ID:
Goal 状态:
Goal 成功标准:
目标:
Spec 强度:
Owner:
来源上下文:
写入范围:
写入文件:
只读范围:
只读参考:
依赖顺序:
并行边界:
禁止事项:
验证命令:
独立验证证据:
CR 交接要求:
知识回流位置:
效果指标:
停止条件:
Goal Ledger 更新:
恢复入口:
Git 策略:
建议提交切片:
```

Harness 三层：

| 层 | 本任务需填写 | 示例 |
| --- | --- | --- |
| Orchestrator | Task ID、owner、依赖顺序、写入范围、停止条件、恢复入口。 | `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001`。 |
| Knowledge | 领域术语、不变量、历史坑点、规则待确认和项目约束。 | 资金账户/信用账户是账务主体，支付工具不是账务主体。 |
| Delivery | Red、验证命令、CR、交付证据、知识回流和建议提交切片。 | `just test-one ...`、`just compile`、`just pmd`。 |

## 8. CAD Loop 模板

CAD Loop 只消费已确认的单一 Grant，不消费整个 Roadmap。每轮最多处理一个原子任务。

| 轮次 | 读取状态 | 本轮动作 | 写入范围 | 验证命令 | 反馈 | 回写位置 | 继续 / 停止 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Goal、Spec、AC、Red、Git 状态 | 写 Red / 最小实现 / 修验证 / 回写文档 | 待填 | 待填 | 待填 | Spec / TDD / Harness / OpenSpec | 待填 |

停止条件：

1. 验证失败且无法在授权范围内修复。
2. 需要公共契约、DDL/H2、状态机、权限、安全、生产行为或外部规则确认。
3. 出现工作树冲突或未归属变更。
4. 连续两轮没有新增证据、测试收敛或缺口收敛。
5. 用户暂停、撤销授权或调整优先级。

## 9. CR 交接模板

源码级 CR 前必须提供下列信息。没有源码变更时，也要说明不适用原因。

| 交接项 | 填写内容 |
| --- | --- |
| 业务意图 | 待填 |
| 关联 Goal | 待填 |
| 入口路径 | 待填 |
| 影响模块 | 待填 |
| 关键调用关系 | 待填 |
| 边界变化 | 公共契约 / 状态机 / DDL / 幂等 / 事务 / 权限 / 审计是否变化。 |
| 源码锚点 | 类、方法、DTO、测试类、fixture。 |
| Spec / AC 映射 | 覆盖 AC、DSL case、TDD Red、forbidden facts。 |
| 测试与静态检查证据 | 命令、结果、未执行原因。 |
| 事实 / 推断 | 分开列出。 |
| 残余不确定性 | 待填 |
| 需要人拍板 | 待填 |

## 10. 交付证据包模板

任务完成或暂停时，必须输出交付证据包。

| 证据项 | 内容 |
| --- | --- |
| 覆盖范围 | 涉及文件、模块、AC、DSL case、TDD Red、OpenSpec/Harness。 |
| 代码/测试变更 | 新增、修改、删除或不适用原因。 |
| 核心断言 | 状态、route、posting、LedgerEntry、余额投影、幂等、审计、失败无副作用。 |
| 验证命令 | 实际执行命令、结果和失败处理。 |
| 静态检查 | `just compile`、`just pmd`、边界测试或不适用原因。 |
| 未覆盖项 | Not Done、承接任务、人工确认点。 |
| 残余风险 | 外部规则、合规、财务、通道、性能、并发、数据迁移。 |
| 知识回流 | PRD/DSL/系分/TDD/OpenSpec/AGENTS/fixture/脚本/模板。 |
| 建议 commit message | 中文描述，必要时带 `feat:`、`fix:`、`test:`、`docs:` 等前缀。 |

## 11. 候选任务预填样例

下列样例只用于说明填写方式，不构成编码授权。

| 候选 | 建议 Spec 强度 | 首批 AC | 首批 Red | 验证命令 | 不做范围 |
| --- | --- | --- | --- | --- | --- |
| `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` | Harness/GSD Spec 或 CAD 候选 Spec。 | 父子账户引用可被公共契约表达；卡绑定子账户时能形成账户层级快照。 | 账户层级 DTO/Spec 缺失时的 contract Red。 | `just test-one <TestClass> [module]`、必要时 `just test-boundary`。 | 不做开户落账、账本初始化、父子余额汇总生产 Done。 |
| `GSD2-B7-RECON-DIFFERENCE-MVP-001` | Harness/GSD Spec。 | 对账差错可创建、重跑幂等、人工补事实有白名单和审计。 | 差错重复创建或无审批补事实 must-fail。 | `just test-reconciliation` 或指定服务级测试。 | 不打开完整清分、清算、结算、出款和追偿。 |
| `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` | 可评审 Spec 或 CAD 候选 Spec。 | 交易投影可解释来源 route、支付工具快照和账本摘要。 | 缺 route snapshot 时 fail-fast，不生成误导性解释。 | `just test-transaction` 或投影相关测试。 | 不改 canonical 入参，不新增统一支付工具交易服务。 |

## 12. 验证矩阵

| 验证层 | 命令或方式 | 当前通过口径 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-Spec-AC-Harness-CAD任务模板.md` | Task ID、Owner、写入范围、只读范围、依赖顺序、验证命令、停止条件和交接字段齐全。 |
| 产品架构结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-Spec-AC-Harness-CAD任务模板.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收字段齐全。 |
| 架构方案结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-Spec-AC-Harness-CAD任务模板.md` | 背景目标、边界取舍、契约、数据一致性、可靠性安全、验证、发布风险字段齐全。 |
| 文档一致性 | `rg` 扫描 GSD-2、AI 交付闭环、TDD README、docs README 对本文的入口引用。 | 后续单一 Grant 入口能追踪到本文。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译和测试 | 本轮不运行 | 仅模板和入口文档变更，不改 Java、测试、DDL/H2 或运行时配置。 |

## 13. 残余风险和停止条件

残余风险：

1. 模板齐全不代表任务已准备编码，仍需用户确认单一 Grant。
2. 模板中的候选预填样例仅是填写示例，不是最终设计、TDD 或实现承诺。
3. OpenSpec 当前工作树存在索引状态需要单独确认；本文不改动 OpenSpec tracked/untracked 状态。
4. 涉及真实资金、客户资金、卡组织、银行、跨境、FX、ACH、SWIFT、税务、会计、法务或合规的结论，仍需专业确认。

停止条件：

1. 需要改公共契约、DDL/H2、生产代码、测试代码或运行时配置。
2. 需要 Git add、commit、push、PR、部署、联网、依赖安装或不可逆操作。
3. 任务的 Spec、AC、Red、Harness、验证命令、停止条件或 owner 不完整。
4. 连续两轮没有新增证据、状态变化、测试收敛或缺口收敛。
5. 用户调整优先级、暂停、撤销授权或指定新的 Execution Grant。
