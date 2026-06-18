# GSD-2 B7 PRD 系分合议预审与三卡交接

## 1. 文档定位

本文是 B7 清结算与对账在 `GSD2-B7-RECON-DIFFERENCE-MVP-001` 和 `GSD2-B7-RECON-DIFFERENCE-MVP-002` 首轮 Green 后的 PRD / 系分合议预审报告和三卡交接物。它只承载评审结论、决策日志、产品上下文、工程交接和生产 Loop 候选，不替代正式 PRD、系分正文、OpenSpec、Execution Grant、测试通过或上线批准。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B7-PRD-SDD-JOINT-REVIEW-HANDOFF-2026-06-18` |
| 原子任务 | 对 B7 清结算与对账 PRD、系分、TDD 和 OpenSpec 做合议预审，并输出 Product Context Card、Engineering Handoff Card、Production Loop Card。 |
| 当前阶段 | Hardened Candidate / GSD 候选交接。 |
| 当前 Git / code baseline | `10853e2d feat: 收紧对账差错处理动作守卫`。 |
| 评审对象 | `docs/产品设计/03-清结算与对账.md`、`docs/系分设计/03-清结算与对账系分设计.md`、`docs/TDD设计/B7-清结算与对账Round0准入卡.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 触发原因 | B7 已具备对账差错对象、处理动作上下文和重新对账幂等的最小闭环，需要判断 PRD / 系分是否足以交接到下一轮“清算、结算、出款准入消费差错状态”的单一 Grant 候选。 |
| Owner | AI Native 负责流程和三卡；产品架构专家负责业务目标、对象、规则、验收和待确认；资深架构师负责模块边界、接口契约、TDD、验证命令和停止条件；用户确认单一 Execution Grant。 |
| 写入范围 | 本文、TDD README、docs README、GSD-2 状态账本和 OpenSpec tasks 的状态同步。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、reconciliation、transaction、ledger、wallet、core、tests、Justfile、最近 Git 提交和历史准入卡。 |
| Git 策略 | `summary_only`。本文不授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |
| 停止条件 | 需要 Java、测试、公共契约、DDL/H2 schema、Entity、Mapper、运行时配置、外部协议、生产配置、真实资金、专业合规确认或 Git 操作时停止，等待新的单一 Execution Grant。 |

## 2. PRD / 系分合议预审结论

结论：当前 PRD 和系分可以作为下一轮 B7 工程任务的输入，但只能进入“清算 / 结算 / 出款准入消费差错状态”的薄切片候选，不能直接打开完整清分、内部清算、结算、出款、追偿、运营后台、补事实命令执行服务或生产迁移。

准出建议：可进入 `GSD2-B7-RECON-GATE-CONSUME-001` 候选包准备。该候选包的业务问题是：当存在重大未闭环差错、处理动作上下文缺失、重新对账未对平或阻断范围命中清算 / 结算 / 出款时，系统必须给出阻断决策和解释摘要，且不得生成清算候选、确认清算批次、锁定结算单或提交出款。

当前不能准出的范围：完整 B7 全量对象、补事实命令执行服务、交易层 / 账本层资金事实委派、运营审批流、职责分离落地、生产 DDL 迁移脚本、外部规则时效核验、卡组织 / 银行 / 通道协议、VCC / 全球账户 / 收单专项。

## 3. 三角色分工

| 工作位 | 本轮职责 | 本轮结论 |
| --- | --- | --- |
| 流程控制位 | 限定评审对象、来源材料、停止条件和准出路径。 | 本轮是合议预审和三卡交接，不是正式评审、编码授权或上线批准。 |
| 主笔 / 决策 owner | 把挑战者反馈落成 `ACCEPT`、`REJECT` 或 `PENDING`，并给出动作、owner 和验证方式。 | 下一步只建议薄切片 `GSD2-B7-RECON-GATE-CONSUME-001`。 |
| 挑战者位 | 从产品价值、运营财务、技术边界、资金安全、QA 验收、安全审计和生产发布挑刺。 | 主要风险不是 PRD / 系分缺总方向，而是把差错闭环误读成完整清结算或补事实资金修正已完成。 |

## 4. review_task

| 锚点 | 问题 | 影响 | 建议 |
| --- | --- | --- | --- |
| PRD 第 1 章、第 1.2 节和第 6 章 | PRD 已明确清分、清算、结算、出款先判断是否允许成为资金事实，再交给交易层和路由层；但下一步工程容易越界到完整清算确认或出款结果入账。 | 产品范围膨胀，MVP 变成完整 B7 全量交付。 | 下一 Grant 只做 gate decision / preflight consumption，不生成 `CLEARING_CONFIRM`、`SETTLEMENT_LOCK` 或 `PAYOUT_SUCCESS` 资金事实。 |
| 系分 3.2、3.2.1 和 3.4 | 系分已规定 reconciliation 只维护运营对象、差错和审计，资金变化必须通过交易层或账本层追加标准事实；但现有 B7-002 只登记动作上下文，不执行补事实命令。 | 若误把 action guard 当作补账执行，会绕过交易 / 账本事实白名单。 | 在交接中明确 actionType、idempotencyKey 和 originalFactRef 只是处理意图和审计上下文，不是资金事实。 |
| B7 Round0 第 15.2、15.3 和第 16 节 | B7 已完成差错登记、处理回链、重新对账幂等和动作守卫；Not Done 包含清算/结算/出款消费、生产迁移、账龄升级和补事实命令执行服务。 | 后续选择错误会直接跳到清结算全量实现。 | 下一步优先验证消费差错状态和阻断范围，而不是补事实执行服务或完整批次对象。 |
| GSD-2 W5 和 OpenSpec tasks | 当前下一候选已经指向清算/结算/出款准入消费差错状态，或 B5 审计扩展。 | 状态账本如果不固化三卡，后续“继续推进”会再次进入计划确认循环。 | 用三卡把事实、执行和生产 Loop 分开，形成可继续工作的最小交接包。 |

## 5. evaluation_task 决策日志

| ID | 决策 | 理由 | 动作 | owner | 验证方式 |
| --- | --- | --- | --- | --- | --- |
| D1 | `ACCEPT` | PRD 和系分对 B7 的产品边界、模块边界和资金红线基本一致。 | 保留现有 PRD / 系分正文，不把本轮讨论过程混入正式设计。 | AI Native + 产品架构专家 + 资深架构师。 | 本文引用 PRD、系分、TDD 和 OpenSpec 当前有效章节。 |
| D2 | `ACCEPT` | B7-001 / B7-002 已证明差错对象和动作守卫，但还没有消费方准入。 | 下一候选限定为 `GSD2-B7-RECON-GATE-CONSUME-001`。 | 资深架构师。 | 后续 Red 必须证明被阻断时无清算、结算、出款或资金副作用。 |
| D3 | `ACCEPT` | actionType、idempotencyKey 和 originalFactRef 是处理意图守卫，不是补事实执行。 | 三卡中明确禁止把动作守卫升级为资金修正能力。 | 架构 owner。 | 代码测试继续断言不生成 route、posting、LedgerEntry、余额投影或交易投影。 |
| D4 | `PENDING` | 不同处理动作是否允许释放清算 / 结算 / 出款，需要产品、财务、风控和合规确认。 | 未确认前默认只有“处理动作已回链 + 重新对账对平 + 阻断范围解除”才可条件放行。 | 产品 owner + 财务 / 风控 / 合规确认方。 | Gate 测试先按保守默认规则设计，放行矩阵后续单独确认。 |
| D5 | `PENDING` | B7-002 已改 H2 测试 schema，但生产迁移脚本和上线回滚还不是本轮能力。 | 进入生产发布前必须补生产 DDL 迁移、回滚、兼容读取和数据核验。 | 架构 owner + DBA / SRE。 | 生产变更评审和 migration 验证，不由本文完成。 |
| D6 | `REJECT` | 直接打开完整清分、清算、结算、出款、追偿或补事实命令执行会扩大 MVP。 | 本轮拒绝把下一 Grant 命名为 B7 全量实现。 | AI Native。 | 写入范围检查不得出现完整批次对象、出款执行或交易 / 账本委派。 |
| D7 | `ACCEPT` | 后续测试必须证明 gate 消费差错状态的业务可用性和资金安全。 | Red 先覆盖 BLOCKED 差错阻断、处理未对平继续阻断、对平后条件放行、动作上下文漂移拒绝。 | QA / 资深架构师。 | `ReconciliationGateApplicationServiceTests` 或等价服务级 H2 流程测试。 |

## 6. reporting_task

### 共识

1. B7 的产品主线是“交易成功后把钱算清、对清、结清”，但 MVP 必须先保护对账差错阻断和解释，不直接做完整清结算。
2. reconciliation 模块是运营资金闭环模块，只写对账、差错、阻断、处理动作、重跑和审计对象；资金变化仍由交易层或账本层追加标准事实。
3. 已完成能力可以声明为“对账差错运营对象和处理动作上下文最小闭环”，不能声明完整清结算、出款生命周期、补事实执行或生产上线完成。

### 必改 / 阻断

1. 若下一轮要编码，必须重新确认单一 `Execution Grant：GSD2-B7-RECON-GATE-CONSUME-001`。
2. 下一 Grant 必须写清 gate 决策的写入范围、只读范围、验证命令、停止条件和 Not Done，不得让任务名漂移成完整 B7。
3. 若触碰 DDL/H2、新公共契约、交易层、账本层、出款执行或生产迁移，必须停止并扩大授权。

### 分歧 / 待确认

| 待确认 | 影响 | 默认处理 |
| --- | --- | --- |
| 哪些 actionType + rerun result 可释放清算 / 结算 / 出款。 | 影响 gate 放行矩阵和运营解释。 | 默认保守阻断，只允许已处理且重新对账对平的差错进入条件放行。 |
| 是否需要生产 DDL 迁移脚本承接 B7-002 字段。 | 影响生产准入和回滚。 | 本轮不进入生产发布；后续生产变更单独评审。 |
| gate 决策是先只服务清算候选，还是同时服务结算锁定和出款 preflight。 | 影响首批 Red 范围。 | 建议首批覆盖通用 gate decision，再选择一个消费场景证明不产生资金副作用。 |

### 风险清单

| 风险 | 等级 | 控制方式 |
| --- | --- | --- |
| 把“处理动作守卫”误解为“补事实执行服务”。 | 高 | 三卡和 Red 均要求无 route、posting、LedgerEntry、余额投影或交易投影副作用。 |
| 把“差错已处理”误解为“可以放行”。 | 高 | 放行必须依赖重新对账结果、阻断范围解除和规则版本；仅处理回链不足以放行。 |
| 下一任务把清算、结算和出款全量混入。 | 高 | `GSD2-B7-RECON-GATE-CONSUME-001` 只做 gate consumption，不做批次确认或出款执行。 |
| 外部规则、财务、税务、合规确认缺失。 | 中 | 标为待确认；未确认前只做本地字段完整性和阻断默认策略。 |
| 文档状态与 Git baseline 漂移。 | 中 | 以 `10853e2d` 作为本轮交接基线，下一轮先复核 `git status --short`。 |

## 7. Product Context Card

```text
Product Context Card:
业务目标 / 非目标:
  目标：让清算、结算和出款准入能够消费对账差错状态、阻断范围、处理动作和重新对账结果，避免重大差错未闭环时释放资金或误展示成功。
  非目标：不做完整清分、清算批次确认、结算锁定、出款提交、追偿、运营后台、补事实命令执行、外部协议和生产迁移。

业务 owner / 验收 owner:
  产品 owner：确认清结算和对账使用者口径、放行矩阵和 Not Done。
  验收 owner：财务、运营、风控、测试、架构 owner。
  专业确认方：财务、税务、会计、合规、法务、银行 / 通道 / 卡组织或安全负责人按实际场景确认。

事实证据:
  PRD 已定义清分、清算、结算、出款、对账和差错闭环对象。
  系分已定义 reconciliation 模块边界、差错闭环、补事实白名单和资金事实红线。
  B7-001 已完成差错登记、处理回链、重新对账幂等和无资金副作用。
  B7-002 已完成处理动作类型、处理幂等键和原始事实引用守卫。

核心用户 / 被影响主体:
  财务、运营、风控、出纳、商户、业务接入方、审计和研发。
  被影响主体包括商户资金账户、平台资金账户、收益参与方账户、出款收款端点和对应对账批次。

核心对象 / 状态 / 不变量:
  对象：ReconciliationDifference、blockingScope、actionType、adjustmentIdempotencyKey、originalFactRef、rerun result、clearing / settlement / payout gate decision。
  状态：BLOCKED、IN_PROCESS、RESOLVED、REJECTED 或当前枚举等价状态，以代码和现有 DTO 为准。
  不变量：未闭环差错不得释放清算、结算或出款；差错处理不得直接改历史分录、余额投影或交易投影；重新对账不覆盖旧运行记录和审批凭证。

主流程 / 逆向 / 异常 / 人工兜底:
  主流程：消费对象发起 gate check -> 读取差错状态、阻断范围、处理动作和重跑结果 -> 生成阻断或条件放行解释。
  逆向：已放行后出现新差错，后续批次或出款必须重新执行 gate check。
  异常：差错状态未知、动作上下文缺失、阻断范围不明确、重跑未对平、规则版本缺失时默认阻断。
  人工兜底：只能补证据、审批、处理回链、发起重新对账或提交后续白名单命令，不直接改账。

关键规则 / 权限 / 数据口径:
  差错未处理或重跑未对平默认阻断。
  处理动作已回链但未重新对账对平，不等于放行。
  actionType 漂移、idempotencyKey 漂移或 originalFactRef 漂移必须拒绝。
  使用者解释必须区分事实状态、展示状态、操作状态、不可操作原因、责任方和下一步动作。

验收种子:
  存在 BLOCKED 差错时，清算候选 / 结算锁定 / 出款 preflight 返回阻断且无资金副作用。
  差错已处理但重新对账未对平时继续阻断。
  差错已处理且重新对账对平时返回条件放行和证据摘要。
  动作上下文漂移或缺失时不得放行。

风险 / 待确认 / 专业确认方:
  待确认 actionType 与放行矩阵。
  待确认生产迁移和回滚策略。
  待确认外部规则、合规、财务和税务口径。

交给 AI Native / 架构师的边界:
  仅交接 gate consumption 候选。
  不授权完整 B7、补事实执行、交易层 / 账本层委派或生产发布。
```

## 8. Engineering Handoff Card

```text
Engineering Handoff Card:
当前状态:
  `B7_RECON_DIFFERENCE_ACTION_GUARD_GREEN_VERIFIED`，Git baseline 为 `10853e2d`。

关联 Goal / Spec / AC:
  Goal：`GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12`。
  B7 Round0：`R0-B7-CLS-001`、`R0-B7-PAYOUT-001`、`R0-B7-OPS-001` 的前置消费能力。
  CLS-GATE：优先承接 `CLS-GATE-001`、`CLS-GATE-002`、`CLS-GATE-004`、`CLS-GATE-007`。

Wave / Task ID:
  建议下一候选：`GSD2-B7-RECON-GATE-CONSUME-001`。

写入范围 / 只读范围 / 禁止事项:
  候选写入范围：reconciliation-face gate application 契约、Request/DTO、reconciliation-impl 只读 gate 实现、服务级 H2 测试和必要的 TDD/OpenSpec 状态回写。
  候选只读范围：PRD 03、系分 03、B7 Round0、GSD-2 W5、OpenSpec tasks、ReconciliationDifferenceApplicationService、差错 Entity/Mapper、tests H2 schema。
  禁止事项：不写交易层、账本层、钱包层资金事实；不新增补事实执行服务；不确认清算批次、结算锁定或出款成功；不实现完整运营后台、外部协议、生产迁移和 P2 业务。

依赖顺序:
  1. 读取现有差错 DTO / Entity / Mapper 和 action guard 测试。
  2. 先写 gate decision 目标 Red。
  3. 最小实现只读取差错状态、阻断范围、动作上下文和重跑结果。
  4. 回写 B7 Round0 / GSD-2 / OpenSpec 的验证证据和 Not Done。

验证命令 / 人工验收:
  首选：`just test-one ReconciliationGateApplicationServiceTests tests`。
  分组：`just test-reconciliation`。
  收口：`just compile`、`just pmd`、`git diff --check`。
  若触碰公共契约或边界测试，追加 `just test-boundary`。

授权策略 / Git 策略:
  当前为 `summary_only`。
  编码前必须由用户确认 `Execution Grant：GSD2-B7-RECON-GATE-CONSUME-001`。
  Git 仍需用户明确提交授权。

质量 / 测试门禁:
  先 Red 后 Green。
  测试使用真实 Spring Bean 和 H2 schema。
  必须断言无 route、posting、LedgerEntry、余额投影、交易投影和出款副作用。
  幂等、动作上下文漂移、阻断范围和使用者解释必须覆盖。

事实 / 推断 / 待确认 / 范围外不做:
  事实：B7-001 / B7-002 已完成最小差错闭环和动作守卫。
  推断：gate consumption 是下一条风险最低且价值最高的 B7 薄切片。
  待确认：放行矩阵、生产迁移、是否同时覆盖清算 / 结算 / 出款三个消费场景。
  范围外不做：完整清分清算结算出款、补事实执行、P2 VCC / 全球账户 / 收单。

失败回写位置:
  本文、B7 Round0 第 16 节、GSD-2 P0/P1 推进计划、OpenSpec tasks。

下一 owner / 停止条件:
  下一 owner：资深架构师承接工程任务，产品架构专家确认放行矩阵。
  停止条件：需要新公共契约、DDL/H2 扩权、交易/账本委派、外部规则、生产迁移、Git 或专业确认时停止。
```

### 8.1 推荐首批 Red

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | targetAssets | verificationCommand |
| --- | --- | --- | --- | --- | --- | --- |
| `R0-B7-GATE-001` | 存在命中阻断范围的 `BLOCKED` 差错时，清算 / 结算 / 出款准入是否会被阻断。 | 重大差错未闭环不得释放资金。 | gate decision = blocked、阻断原因、责任方、差错引用、下一步动作。 | 不生成清算候选、清算确认、结算锁定、出款提交、route、posting、entry、projection。 | `ReconciliationGateApplicationServiceTests`。 | `just test-one ReconciliationGateApplicationServiceTests tests`。 |
| `R0-B7-GATE-002` | 差错已回链处理动作但重新对账未对平时是否继续阻断。 | 处理动作不等于放行。 | decision = blocked、actionType、rerun result、不可操作原因。 | 不因有 actionType 就条件放行。 | 同上。 | 同上。 |
| `R0-B7-GATE-003` | 差错已处理且重新对账对平时，是否能返回条件放行和证据摘要。 | 放行只基于已验证事实，不基于人工备注。 | decision = conditionally allowed、rerunSn、evidenceRef、ruleVersion。 | 不修改历史差错、不覆盖旧运行记录。 | 同上。 | 同上。 |
| `R0-B7-GATE-004` | 动作类型、处理幂等键或原始事实引用漂移时，是否拒绝放行。 | 同一处理流水不能被替换成另一种资金修正语义。 | decision = blocked 或 request rejected、漂移原因。 | 不接受漂移后的动作上下文。 | 同上。 | 同上。 |

## 9. Production Loop Card

```text
Production Loop Card:
Loop 目标 / 状态载体:
  目标：让 B7 从差错对象最小闭环推进到清算 / 结算 / 出款准入可消费。
  状态载体：本文、B7 Round0、GSD-2 P0/P1 推进计划、GSD-2 新基线、OpenSpec tasks。

自动化心跳 / 触发条件:
  触发条件：用户确认新的单一 Execution Grant，或要求继续推进 B7 gate consumption。
  自动化心跳：当前不创建后台自动化；仅在本地会话中按 Plan Grant / Execution Grant 推进。

隔离执行方式:
  每轮只执行一个低风险切片。
  文档和状态同步可默认推进；Java、测试、DDL、公共契约、Git、联网和生产动作需要显式授权。

Maker / Checker 分工:
  Maker：资深架构师执行 Red / Green / Refactor。
  Checker：产品架构专家检查业务语义和 Not Done，架构师做资金红线 CR，验证命令作为机器证据。

反馈源 / 独立验证者:
  反馈源：目标测试、`just test-reconciliation`、`just compile`、`just pmd`、`git diff --check`、产品 owner 和财务 / 风控 / 合规确认。
  独立验证者：QA / 架构 owner / 产品 owner。

预算 / 最大轮次 / 无进展检测:
  每轮最多一个单一 Grant。
  连续两轮只是重复计划、没有新增 Red、验证证据或状态收敛时暂停。

观测审计:
  本轮只定义 gate decision 应输出阻断原因、差错引用、责任方、下一步动作和证据摘要。
  生产观测、告警、Runbook、导出审计和职责分离仍为后续 Grant。

人工接管:
  发现放行矩阵不清、外部规则未确认、生产迁移缺失、职责分离不满足或验证失败无法修复时，由用户 / 产品 owner / 架构 owner 接管决策。

发布 / 回滚:
  当前不发布。
  后续生产发布必须补 DDL 迁移、灰度、开关、回滚、数据核验、告警和 Runbook。

理解债和残余风险:
  残余风险包括放行矩阵未确认、生产迁移未设计、补事实执行服务未实现、运营审批和职责分离未落地、完整清结算出款未完成。
```

## 10. 准出路径

| 结论 | 下一步 |
| --- | --- |
| 可进入 PRD 修订 | 暂不需要。PRD 当前目标、边界和风险口径可继续作为工程输入；若后续产品确认放行矩阵，再回写 PRD 最终规则。 |
| 可进入系分修订 | 暂不需要重写系分正文。若下一 Grant 确认 gate application service，再在系分中吸收最终接口和数据设计。 |
| 可进入 Harness / GSD 候选 | 可以。建议候选为 `GSD2-B7-RECON-GATE-CONSUME-001`。 |
| CAD 候选缺口 | 需要用户确认单一 Execution Grant、首批 Red、写入范围、是否触碰 schema、Git 策略和放行矩阵默认口径。 |
| 必须回退 Round 0 | 若产品要求完整清算、结算、出款或补事实命令执行，则必须回退 B7 Round0 重新拆包。 |

## 11. 本轮验证计划

本轮验证方案只覆盖文档结构、任务交接和状态一致性；测试、静态检查和回归在下一轮 Execution Grant 中按 Engineering Handoff Card 执行。仅修改文档和状态入口，不运行 Java 编译、Maven 测试或 PMD。

| 验证项 | 命令或方式 | 通过口径 |
| --- | --- | --- |
| 产品评审结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-review --file docs/TDD设计/GSD-2-B7-PRD系分合议预审与三卡交接.md` | 合议评审上下文、共识、分歧、必改、待确认和验证方式齐全。 |
| 架构计划结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-B7-PRD系分合议预审与三卡交接.md` | 背景目标、现状约束、决策、契约、数据一致性、可靠性安全、验证和风险齐全。 |
| Harness 轻量结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind lightweight --file docs/TDD设计/GSD-2-B7-PRD系分合议预审与三卡交接.md` | Task、Owner、范围、顺序、验证和交接字段齐全。 |
| 状态一致性 | `rg "GSD2-B7-RECON-GATE-CONSUME-001|B7_RECON_DIFFERENCE_ACTION_GUARD|10853e2d" docs openspec` | 新交接、GSD-2 状态和 OpenSpec 能追踪到同一基线与下一候选。 |
| 空白检查 | `git diff --check` | 无行尾空白或 patch 格式问题。 |

后续进入编码时再按 Engineering Handoff Card 的验证命令执行。
