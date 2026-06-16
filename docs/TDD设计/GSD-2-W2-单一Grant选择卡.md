# GSD-2 Wave 2 单一 Grant 选择卡

## 1. 文档定位

本文是 `GSD2-W2-SINGLE-GRANT-SELECTION` 的单一 Grant 选择卡，用于把 W1 推荐候选收敛成可评审、可授权、可验证的下一步任务包。

本文不是编码授权、不是测试写入授权、不是 DDL/H2 schema 授权、不是公共契约变更授权，也不是 Git 提交授权。只有用户明确确认本文中的具体 Execution Grant、写入范围、验证命令和停止条件后，才允许进入 W3 CAD Loop。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W2-SINGLE-GRANT-SELECTION` |
| 所属阶段 | GSD-2 Wave 2 / Single Grant Selection / draft only |
| 关联 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| 当前状态 | `DRAFT_SELECTED_THEN_W3_READONLY_POSITIONING_DONE_NOT_CODE_AUTHORIZED` |
| 推荐 Grant | `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` |
| Owner | 用户确认优先级；产品架构专家确认业务价值和验收；资深架构师确认写入范围、Red、验证命令和风险。 |
| 写入范围 | 本选择卡、GSD-2 入口、W1 审计、W3 只读定位草案、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-W2-单一Grant选择卡.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-W1-基线差距审计.md`、`docs/TDD设计/GSD-2-W3-B2账户层级CAD准入草案.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、AGENTS.md、最近 Git 提交和历史准入卡。 |
| Git 策略 | `summary_only`。本文不授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |

Wave 边界：W2 只做单一 Grant 选择和任务卡草案，不进入 Red/Green/CAD Loop。执行顺序必须是 W1 审计完成、W2 选择卡通过结构门禁、W3A 在 Plan Grant 下完成只读源码定位和 Red 重排、用户确认具体 Execution Grant，然后才允许 W3B 进入 Red/Green/CAD Loop。依赖关系上，账户层级优先于资金责任目标主体、支付工具 application facade、VCC 和全球账户 P2 能力。并行边界上，同一时间只允许一个 Grant 进入 active；涉及公共契约、状态机、fixture 或 H2 schema 的任务必须互不重叠并串行推进。

## 2. 选择结论

建议下一轮确认的单一 Grant：

```text
Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001
```

业务目标：把资金账户 / 信用账户父子结构、VCC 卡绑定子账户和 route snapshot 中账户层级快照的最小契约边界收口，为 VCC、全球账户、资金责任目标主体和支付工具绑定语义提供前置能力。

用户价值：产品、研发和测试能用稳定的账户层级快照解释“这张卡或这笔交易最终落到哪个资金/信用主体”，并避免后续 P2 能力把 VCC 卡、支付工具、预算组或投影误写成账务主体。

成功指标：下一轮 Grant 必须证明账户层级契约、route snapshot、回放、失败无副作用和 Not Done 边界可追踪；若仅做 `contract-only/no-ddl`，必须明确不声明开户落账、余额可用、父子汇总或 VCC funding 生产 Done。

非目标：不实现完整开户、账本初始化、父子账户余额汇总、VCC prepaid funding、shared card 调额、全球账户出入金、清结算对账、支付工具 application facade、Spend Rule 控制或任何外部通道规则。

## 3. 能力地图、对象模型和业务流程

能力地图：

| 能力域 | 本 Grant 关系 |
| --- | --- |
| P0 钱包账户 / 账本账目 | 提供账户层级和主体快照前置，不直接声明余额或账本生产完成。 |
| P1 交易路由 / route replay | 账户层级快照必须能被 route snapshot 或等价契约引用，并支持原快照回放口径。 |
| P2 VCC / 全球账户 | 只作为依赖方，不在本 Grant 中实现业务 facade 或外部轨道。 |

业务对象：资金账户、信用账户、父账户、子账户、根账户、VCC 卡支付工具、账户层级快照、route snapshot、资金责任目标主体和交易投影解释上下文。

对象模型：VCC 卡仍是支付工具，背后绑定资金或信用子账户；父账户默认只做约束、归属和汇总解释，不直接替代子账户入账；多张卡若共享同一资金池，业务上应绑定同一主账户或共享责任主体，不在本 Grant 中新增卡账本。

字段口径：候选字段包括 `accountHierarchySnapshot`、`parentAccountId`、`rootAccountId`、`accountLevel`、`postingRole` 或等价命名。最终字段名称必须由 W3 Red/Green 前的契约审查确认。

生命周期 / 状态：本 Grant 只关心账户层级快照在交易路由和回放中的稳定性，不定义账户开户、启用、冻结、关闭或销户完整生命周期。

业务流程：

1. 上游业务或钱包能力解析到资金账户 / 信用账户。
2. 若账户存在父子结构，生成账户层级快照。
3. route snapshot 记录当前交易实际使用的可入账主体和账户层级上下文。
4. 后续退款、撤销、回放或投影解释使用原 route snapshot，不按当前绑定重新推导。
5. 若缺少必要账户层级事实，应 fail-fast，且不得产生半截 route、posting、LedgerEntry 或余额投影副作用。

规则矩阵：

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验证方式 | 确认方 |
| --- | --- | --- | --- | --- | --- | --- |
| 可入账主体 | 交易实际入账。 | 只能落资金账户、信用账户或平台角色解析后的平台资金账户。 | P0 | 2026-06-12 | TDD Red / route snapshot 断言。 | 资深架构师 |
| 父账户快照 | 子账户参与交易。 | 记录父账户 / 根账户上下文，但不把父账户自动写成入账主体。 | P0 | 2026-06-12 | 契约测试 / 回放测试。 | 产品架构专家 + 资深架构师 |
| 原快照回放 | 退款、撤销、回放或投影解释。 | 使用原 route snapshot 的账户层级和支付工具快照，不按当前绑定重算。 | P0 | 2026-06-12 | route replay / 交易流测试。 | 资深架构师 |
| P2 不抢跑 | VCC 或全球账户要求业务实现。 | 本 Grant 只做前置契约，不实现 P2 facade。 | P0 | 2026-06-12 | Not Done 清单。 | 用户 + 产品架构专家 |

运营后台 / 数据口径：后续运营查询可使用账户层级快照解释交易归属，但本 Grant 不新增后台页面、报表、导出或指标。

风险 / 待确认 / 验收：待确认项是本 Grant 采用 `contract-only/no-ddl` 还是最小 `ledger-snapshot-backed`；验收以首批 Red、验证命令和 Not Done 边界为准。

发布：本选择卡不发布生产能力。若后续 Grant 只改测试或契约，发布口径仍需按实际变更另行判断。

## 4. 架构约束和实现边界

背景和目标：W1 已确认当前实际基线为 `da7d2ea`，账户层级和 route snapshot 已有局部前置证据，但任务状态尚未形成单一 Grant。W2 的目标是让下一轮能够明确“只做账户层级契约收口”，防止混入 VCC、支付工具、资金责任迁移或清结算对账。

现状和影响范围：影响范围预期集中在 core DSL/value object、transaction route snapshot/replay、wallet account 契约或 tests；实际写入文件必须在 W3 前由资深架构师复核。当前文档不授权任何源码写入。

核心决策：

1. 继续坚持 VCC 不新增 `VCC_ACCOUNT`。
2. 账户层级是资金账户 / 信用账户的结构能力，不是支付工具账本。
3. 父账户和根账户优先作为快照、约束、归属和解释上下文，是否参与 posting 必须由明确 `postingRole` 或等价决策控制。
4. 后续回放必须使用原 route snapshot，不按当前绑定重算。

接口契约：若需要新增入参、出参、错误码、幂等摘要、DTO、Request、Query、枚举或状态机，必须在 W3 Grant 中显式列入 `writeScope` 和兼容策略。

数据方案：默认建议首轮 `contract-only/no-ddl`；若选择 `ledger-snapshot-backed` 或需要 DDL/H2 schema、Entity、Mapper、索引、唯一键、迁移脚本，必须把 schemaGate 升级为显式确认点。

事务边界、一致性、补偿和对账：本 Grant 只证明 route snapshot / replay 中的账户层级一致性，不改变 ledger posting 事务边界、余额投影、补偿、清结算或对账。

可靠性、安全、权限、审计和告警：可靠性重点是回放不受当前绑定漂移影响；安全重点是不得把支付工具、预算组、Spend Rule 或投影误写为 ledger subject；审计重点是账户层级快照可追溯。

验证方案：后续 W3 至少运行目标测试、相关 route replay 或 transaction 分组、`just compile` 和 `git diff --check`；若触碰公共契约或跨模块行为，再追加 `just test-transaction`、`just test-boundary`、`just pmd` 或 `just verify-slice`。

发布、灰度、回滚、风险和待确认：本选择卡不涉及发布。风险是把 contract-only 误写成生产能力完成；待确认是 W3 是否允许源码/测试写入、是否允许公共契约变化、是否允许 DDL/H2。

## 5. Spec / AC / Red 草案

| 字段 | 草案 |
| --- | --- |
| Spec ID | `SPEC-GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` |
| Spec 强度 | 默认 `contract-only/no-ddl`；若用户确认可升级。 |
| authorityBaseline | 当前 Git HEAD，至少包含 `da7d2ea` 和 W1/W2 文档状态。 |
| firstRedSet | `B2-AH-RED-001` 账户层级快照进入 route snapshot；`B2-AH-RED-002` 回放使用原账户层级快照；`B2-AH-RED-003` 缺必要层级事实 fail-fast 无副作用。 |
| targetAssets | 待 W3 只读源码定位后确认，优先 route DSL / route replay / transaction flow 相关测试资产。 |
| minimumAssertions | 可入账主体、父账户 / 根账户快照、层级版本、postingRole 或等价决策、原快照回放、失败无 route/posting/entry/projection 副作用。 |
| schemaNeed | 默认 `NO_DDL`。若 Red 证明必须持久化字段或 H2 schema，停止回到用户确认。 |
| verificationCommand | 候选：`just test-one <TargetTest> tests`、`just test-transaction`、`just test-boundary`、`just compile`、`git diff --check`。 |

AC 草案：

| AC ID | 验收口径 |
| --- | --- |
| `AC-B2-AH-001` | 子账户交易的 route snapshot 能携带账户层级上下文，并区分实际入账主体与父账户 / 根账户解释上下文。 |
| `AC-B2-AH-002` | 原交易后续退款、撤销或回放使用原账户层级快照，不受当前卡绑定或账户层级变更影响。 |
| `AC-B2-AH-003` | 缺少必要账户层级事实时 fail-fast，且不生成半截 route、posting、LedgerEntry、余额投影或交易投影事实。 |
| `AC-B2-AH-004` | 明确 Not Done：不声明开户落账、余额可用、父子账户汇总、VCC prepaid funding、shared card 调额或全球账户生产完成。 |

## 6. 写入和禁止范围

候选写入范围需要在用户确认后由 W3 进一步收窄。当前建议：

| 范围 | 草案 |
| --- | --- |
| 允许写入 | 目标测试、最小 core DSL/value object、route snapshot/replay 适配、必要 converter/assembler 和文档状态回写。 |
| 条件写入 | 公共契约、DTO、枚举、H2 schema、Entity、Mapper、fixture 结构；必须在确认时逐项列出。 |
| 禁止写入 | VCC application facade、支付工具 application facade、资金责任目标主体迁移、Spend Rule、清结算对账、全球账户、收单、外部通道、生产配置、CI、Git 操作。 |
| 只读参考 | PRD、DSL、系分、TDD、OpenSpec、`B2B4-支付工具与SpendRule生产可用性Round0准入卡.md`、最近提交和目标源码。 |

## 7. 停止条件

1. 需要 DDL/H2 schema、公共契约、状态机或跨模块依赖变化，但 W3 Grant 未显式授权。
2. Red 证明实际缺口不在账户层级，而在资金责任目标主体、支付工具绑定、VCC facade 或清结算对账。
3. 需要联网、依赖安装、生产配置、真实资金、外部规则、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规确认。
4. 验证失败且无法在所选 Grant 范围内修复。
5. 工作树出现用户未归属变更，且影响目标文件。

## 8. Execution Handoff Card

| 字段 | 内容 |
| --- | --- |
| 当前 Wave / Task | `GSD2-W2-SINGLE-GRANT-SELECTION` |
| 当前状态 | `DRAFT_SELECTED_THEN_W3_READONLY_POSITIONING_DONE_NOT_CODE_AUTHORIZED` |
| 建议确认文本 | `Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` |
| 下一 Wave / Task | W3A 只读源码定位已完成；确认后进入 `GSD2-W3-CAD-LOOP-ACTIVE / B2-AH-SERVICE-FLOW`。 |
| 写入范围 | 待用户确认后由资深架构师按 W3 收窄。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、最近 Git 提交和历史准入卡。 |
| 反馈源 | `git status --short`、`rg`、目标测试、`just compile`、`git diff --check`、专项分组测试和用户确认。 |
| Git 策略 | `summary_only`，除非用户后续明确要求提交并且验证通过。 |
| 交接要求 | 确认后直接按 W3A 重排后的服务流 Red 选择一个最小 Red，再写测试或实现。 |

## 9. 验证矩阵

| 验证层 | 命令或方式 | 通过口径 |
| --- | --- | --- |
| Harness 结构 | `check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-W2-单一Grant选择卡.md` | Task、Owner、范围、Wave、上下文账本、禁止事项、验证和 handoff 字段齐全。 |
| CAD 候选结构 | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-W2-单一Grant选择卡.md` | 写入范围、验证、TDD/Review、Execution Grant、人工确认和交接字段齐全。 |
| 产品结构 | `check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-W2-单一Grant选择卡.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构结构 | `check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-W2-单一Grant选择卡.md` | 背景目标、现状约束、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-W2|GSD2-B2-ACCOUNT-HIERARCHY|W3_READONLY_SOURCE_POSITIONING" docs openspec` | GSD2 入口、README、W3 准入草案和 OpenSpec tasks 能追踪到 W2 选择卡及后续只读定位状态。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译、测试、PMD | 本轮不运行。 | 本轮仅任务卡草案，不改 Java、测试、DDL/H2 或运行时配置。 |
