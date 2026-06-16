# GSD-2 Wave 4 B2 账户层级 Execution Grant 确认包

## 1. 文档定位

本文是 `GSD2-W4-B2-AH-EXECUTION-GRANT-PACK` 的确认包，用于把 W3A 只读源码定位后的账户层级服务流切片压缩成一份可复制确认、可进入 Red/Green/CAD Loop 的单一 Execution Grant。

本文不是默认编码授权。只有用户明确确认本文中的授权文本、写入范围、验证命令和停止条件后，才允许进入 `GSD2-W3-CAD-LOOP-ACTIVE / B2-AH-SERVICE-FLOW`。确认前仍不写 Java、测试、公共契约、DDL/H2 schema、运行时配置或 Git。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W4-B2-AH-EXECUTION-GRANT-PACK` |
| 原子任务 | 固化 B2 账户层级服务流 Execution Grant 确认包，给出可复制确认文本、首个 Red、写入范围、验证命令和停止条件。 |
| 所属阶段 | GSD-2 Wave 4 / Execution Grant confirmation pack / source-contract closed by follow-up Grant |
| 关联 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| 当前状态 | `W4_EXECUTION_GRANT_CONSUMED_RED_OBSERVED_SOURCE_CONTRACT_GREEN_VERIFIED` |
| 上游输入 | W1 基线差距审计、W2 单一 Grant 选择卡、W3 B2 账户层级 CAD 准入草案、当前 Git/code baseline `da7d2ea`。 |
| 推荐 Grant | `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` |
| Owner | 用户确认是否授权；产品架构专家确认验收和 Not Done；资深架构师执行 TDD/CAD、验证和 CR。 |
| 写入范围 | 本确认包、GSD-2 入口、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-W4-B2账户层级ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、W1/W2/W3 文档、core route spec/model、transaction route snapshot/replay、JSON support、既有 DSL/route/transaction 测试、Justfile、AGENTS.md 和最近 Git 提交。 |
| Git 策略 | `summary_only`。本文不授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |

Wave 边界：W4 已在用户确认 `Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` 后进入首个 Red。执行结果表明当前服务流没有合法的账户层级来源，不能在本 Grant 内进入最小 Green；该缺口随后由 `GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 在当前工作树完成本地 Green。依赖关系上，账户层级服务流证明仍优先于资金责任目标主体迁移、支付工具 application facade、VCC funding、全球账户出入金和清结算对账。并行边界上，同一时间只允许一个 Grant active；共享 route snapshot、FundingAllocation、账本主体或 H2 schema 的候选必须串行。

上下文账本：W3A 已确认 DSL / value object / JSON / replay 纯边界不是空白；本文只把下一步授权变成可执行文本，不重复扩写设计过程。

## 2. 可复制确认文本

建议用户若确认进入编码，使用下列原文：

```text
Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001

授权范围：
1. 只允许围绕 B2 账户层级服务流证明写目标测试、最小 route/service 实现、必要 converter/assembler 和文档状态回写。
2. 首个 Red 优先选择 B2-AH-RED-001-SERVICE-FLOW-SNAPSHOT：真实交易或授权服务流生成 route snapshot 时必须携带账户层级快照。
3. 默认 schemaNeed=NO_DDL，不允许修改 H2 schema、Entity、Mapper、索引、唯一键或迁移脚本。
4. 默认不允许修改公共 Request/DTO/枚举/状态机；若 Red 证明必须修改，立即停止并重新确认。
5. 不允许实现 VCC application facade、支付工具 application facade、资金责任目标主体迁移、Spend Rule、清结算对账、全球账户、收单、外部通道、CI、生产配置或 Git 操作。

验证要求：
1. 先写 Red，再最小 Green。
2. 至少运行目标测试、相关 route/json/replay 回归、just compile 和 git diff --check。
3. 若触碰公共契约或跨模块行为，追加 just test-transaction、just test-boundary、just pmd 或 just verify-slice。

停止条件：
1. 需要 DDL/H2、公共契约、状态机、账户关系表、Entity、Mapper 或跨模块依赖变化但未授权。
2. 缺口转向资金责任目标主体、支付工具 facade、VCC funding、全球账户、清结算对账或外部通道。
3. 需要联网、依赖安装、生产配置、真实资金、外部规则、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规确认。
4. 验证失败且无法在本 Grant 范围内修复。
```

## 3. 产品裁决

业务目标：证明 VCC 卡绑定子账户、钱包子账户或全球账户钱包场景下，真实交易服务流能把实际资金责任落到资金账户 / 信用账户，并把父账户、根账户、层级版本和支付工具快照固化到 route snapshot。

用户价值：产品、运营、财务、风控和研发可以用同一条交易事实解释“谁实际出资、归属哪个父账户、按哪个绑定版本发生、后续退款或回放为何仍沿原路径”。

成功指标：本 Grant 完成后，只能声明“账户层级服务流切片已被目标测试和回归证明”；不得声明开户落账、余额可用、父子汇总、VCC funding、全球账户、支付工具 facade 或清结算对账生产 Done。

非目标：不做 VCC 发卡业务、预付卡充值/提现、共享卡调额、全球账户出入金、ACH/Swift/local rail、清结算对账、运营后台、报表、指标或外部通道。

能力地图：

| 能力域 | 本 Grant 覆盖 | 本 Grant 不覆盖 |
| --- | --- | --- |
| 账户层级 | 服务流生成和保存账户层级快照。 | 账户开户、账户关系表、余额汇总。 |
| 交易路由 | route snapshot 中固化 funding allocation 和账户层级。 | 新增支付工具交易内核或 P2 业务 facade。 |
| 回放和投影解释 | 后续事件沿原 route snapshot，不按当前绑定重算。 | 完整交易投影查询产品化。 |
| 账本账目 | 只校验 posting 不把父账户或支付工具误写成主体。 | 新 ledger profile 或清结算账务。 |

业务对象：资金账户、信用账户、子账户、父账户、根账户、支付工具、账户层级快照、资金来源决策、route snapshot、交易事实、账本分录、余额投影和交易投影解释。

字段口径：核心字段是 `accountHierarchySnapshot.accountRef`、`parentAccountRef`、`rootAccountRef`、`contextVariables`、`paymentInstrumentRef`、`fundingAllocations.subjectRef`、`ledgerSubjectCode` 和 `amount`。context 只放非敏感解释字段。

生命周期 / 状态：本 Grant 只覆盖交易发生时的快照生成、交易事实保存、后续 replay 读取和失败路径；不定义账户生命周期或卡生命周期。

业务流程：

1. 测试准备资金账户 / 信用账户和支付工具快照。
2. 交易或授权服务流解析出实际子账户及其父/根账户。
3. route resolver 生成带账户层级快照的 funding allocation。
4. route snapshot 工厂和 lifecycle saver 保存该快照。
5. 后续 replay 使用原快照。
6. 缺层级事实时 fail-fast，无 route、posting、LedgerEntry、余额投影或交易投影副作用。

规则矩阵：

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验证方式 | 确认方 |
| --- | --- | --- | --- | --- | --- | --- |
| 可入账主体 | funding allocation 写入 route snapshot。 | 只能是资金账户、信用账户或平台角色解析后的平台资金账户。 | P0 | 2026-06-12 | 服务流 Red 和回归。 | 资深架构师 |
| 父/根账户 | 子账户或卡绑定账户参与交易。 | 父/根账户只做快照归因，不默认成为 posting 主体。 | P0 | 2026-06-12 | route snapshot、posting、ledger 断言。 | 产品架构专家 + 资深架构师 |
| 原快照回放 | 退款、撤销、过期、回放或投影解释。 | 使用原 route snapshot，不按当前绑定重算。 | P0 | 2026-06-12 | replay 服务流测试。 | 资深架构师 |
| 敏感上下文 | context 或 bindingSnapshot 写入。 | 不保存完整卡号、外部账户原文、密钥、凭据或隐私字段。 | P0 | 2026-06-12 | 敏感上下文回归。 | 资深架构师 |

运营后台 / 数据口径：本 Grant 只产生可解释事实，不新增后台页面、报表、导出或运营操作。后续数据查询可用账户层级快照作为解释来源。

风险 / 待确认 / 验收：风险是现有 contract-only 证据被误读为服务流完成；待确认是用户是否接受本确认包授权；验收以 Red/Green、回归、CR 和验证命令为准。

发布：确认包不发布生产能力。若后续只写测试，不涉及发布；若写生产代码，则按 Grant 的验证和回滚要求收口。

## 4. 架构裁决和写入范围

背景和目标：W3A 已确认账户层级快照模型、JSON 支持和 replay 纯边界已有局部证据。下一步目标是证明真实服务流能生成和保存这份快照。

现状和影响范围：候选影响 `transaction-impl` route resolver / orchestrator / lifecycle saver、`tests` 目标流程测试、必要 converter/assembler 和文档状态；是否触碰 `core` 必须由首个 Red 证明，默认不改公共契约。

核心决策：

1. VCC 卡继续是支付工具，不新增 `VCC_ACCOUNT`。
2. 资金账户 / 信用账户是可入账主体。
3. 父账户 / 根账户是约束和归因上下文，不默认参与 posting。
4. route snapshot 是历史事实边界，后续 replay 不按当前绑定重算。
5. 先做 service-flow-backed 小切片，不打开 P2 facade。

接口契约：默认不改公共 Request/DTO/Query/枚举/状态机；默认不新增入参、出参、错误码或幂等摘要字段，也不改变兼容策略。若测试无法表达业务场景，或需要调整入参、出参、错误码、幂等摘要、兼容行为、application facade、DTO 或枚举，必须先停下更新授权。

数据方案：默认 `NO_DDL`。不改 H2 schema、Entity、Mapper、索引、唯一键、迁移脚本或 fixture 结构。

事务边界、一致性、补偿和对账：必须证明失败无半截 route、posting、LedgerEntry、余额投影或交易投影；不改变清结算、对账、补偿或运营补事实边界。

可靠性、安全、权限、审计和告警：验证 replay 不漂移、敏感字段不落库、权限/生产动作不进入本 Grant、审计上下文可解释；不新增告警。

验证方案：确认后至少运行目标测试、相关 route/json/replay 回归、`just compile`、`git diff --check`。公共契约或跨模块行为变化时追加分组测试和 PMD。

发布、灰度、回滚、风险和待确认：未授权前无发布。授权后若写生产代码，回滚方式为撤销未提交 diff，并保留 Red 和验证结果；Git 提交另需用户明确要求。

## 5. 首个 Red 候选

| Red ID | 业务问题 | 最小断言 | 候选测试资产 | 预期结果 |
| --- | --- | --- | --- | --- |
| `B2-AH-RED-001-SERVICE-FLOW-SNAPSHOT` | 真实交易或授权服务流能否生成账户层级快照。 | route snapshot 的 funding allocation 使用资金/信用账户；`accountHierarchySnapshot` 含 accountRef、parentAccountRef、rootAccountRef；paymentInstrumentRef 脱敏；posting 不把父账户或支付工具写成主体。 | 优先 `FundsAuthorizationTransactionFlowTests` 或 `FundsDirectTransactionFlowTests`；若过重，可落到更小的 route/orchestrator 服务流测试。 | 先失败，证明当前服务流缺口或已有能力未被目标测试覆盖。 |
| `B2-AH-RED-002-LIFECYCLE-PERSISTENCE-REPLAY` | 已保存交易后 replay 是否沿原账户层级。 | 从交易事实查询 route snapshot，后续 replay 不被当前绑定覆盖，posting/ledger/projection 继续沿原快照。 | `DefaultRouteReplayServiceTests` 增强或交易 flow 回归。 | 作为第二 Red，除非第一个 Red 证明保存链已天然闭合。 |
| `B2-AH-RED-003-MISSING-HIERARCHY-FAILFAST` | 缺层级事实时是否失败无副作用。 | 无 route snapshot、posting、LedgerEntry、余额投影、交易投影；错误可解释。 | 交易 flow 失败路径或 orchestrator 投影测试。 | 作为失败路径 Red，避免只测 happy path。 |

建议第一刀：`B2-AH-RED-001-SERVICE-FLOW-SNAPSHOT`。

## 6. 验证命令候选

确认 Grant 后，按实际目标测试选择最小验证：

| 场景 | 命令 |
| --- | --- |
| 首个目标测试 | `just test-one <TargetTest> tests` |
| route DSL 合约回归 | `just test-one PaymentInstrumentRouteDslContractTests tests` |
| route snapshot JSON 回归 | `just test-one RouteSnapshotJsonSupportTests tests` |
| route replay 回归 | `just test-one DefaultRouteReplayServiceTests tests` |
| 编译 | `just compile` |
| 空白和 patch 格式 | `git diff --check` |
| 跨交易模块行为 | `just test-transaction` |
| 边界/架构回归 | `just test-boundary` |
| 静态检查 | `just pmd` |

若只写测试且 Red 预期失败，必须在交付中明确“Red 已按预期失败、未进入 Green”或继续最小 Green 后再声明通过。

## 7. 禁止事项和停止条件

禁止事项：

1. 未确认前不写 Java、测试、公共契约、DDL/H2 schema、运行时配置或 Git。
2. 不实现 VCC application facade、支付工具 application facade、资金责任目标主体迁移、Spend Rule、清结算对账、全球账户、收单、外部通道、CI 或生产配置。
3. 不把支付工具、预算组、Spend Rule、交易投影或 DSL fixture 写成账本主体。
4. 不通过 contextVariables 承载核心资金语义或敏感信息。

停止条件：

1. 需要 DDL/H2、公共契约、状态机、账户关系表、Entity、Mapper 或跨模块依赖变化，但本 Grant 未授权。
2. Red 证明缺口实际属于资金责任目标主体、支付工具 facade、VCC funding、全球账户、清结算对账或外部通道。
3. 需要联网、依赖安装、生产配置、真实资金、外部规则、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规确认。
4. 验证失败且无法在本 Grant 范围内修复。
5. 工作树出现影响目标文件的用户未归属变更。

## 8. Execution Handoff Card

| 字段 | 内容 |
| --- | --- |
| 当前 Wave / Task | `GSD2-W4-B2-AH-EXECUTION-GRANT-PACK` |
| 当前状态 | `W4_EXECUTION_GRANT_CONSUMED_RED_OBSERVED_SOURCE_CONTRACT_GREEN_VERIFIED` |
| 建议确认文本 | 本 Grant 已确认并消费到首个 Red；后续 `GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 已完成本地 Green，不得重复确认同一 Grant 或来源契约 Grant。 |
| 下一 Wave / Task | 进入资金责任目标主体决策：`GSD2-B2-FR-TARGET-001` 或等价资金责任目标主体 Grant。 |
| 写入范围 | 当前已允许保留 Red 测试、来源契约 Green 和状态回写；下一步若修改资金责任 Request/DTO、DDL/H2、Entity、Mapper 或关系模型，必须由新 Grant 明确列名。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、最近 Git 提交和历史准入卡。 |
| 反馈源 | `rg`、目标测试、`just compile`、`git diff --check`、专项分组测试和用户确认。 |
| Git 策略 | `summary_only`，除非用户后续明确要求提交并且验证通过。 |
| 交接要求 | 当前已写 `B2-AH-RED-001-SERVICE-FLOW-SNAPSHOT` 并由来源契约 Grant 转 Green；下一步先准备资金责任目标主体 Red，不再重复账户层级来源契约。 |

## 9. 验证矩阵

| 验证层 | 命令或方式 | 通过口径 |
| --- | --- | --- |
| Harness 结构 | `check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-W4-B2账户层级ExecutionGrant确认包.md` | Task、Owner、范围、Wave、上下文账本、禁止事项、验证和 handoff 字段齐全。 |
| CAD 候选结构 | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-W4-B2账户层级ExecutionGrant确认包.md` | 写入范围、验证、TDD/Review、Execution Grant、人工确认、撤销方式和交接字段齐全。 |
| 产品结构 | `check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-W4-B2账户层级ExecutionGrant确认包.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构结构 | `check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-W4-B2账户层级ExecutionGrant确认包.md` | 背景目标、现状约束、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-W4|SOURCE_CONTRACT_GREEN|B2-AH-RED-001-SERVICE-FLOW-SNAPSHOT|FR-TARGET" docs openspec` | GSD2 入口、README 和 OpenSpec tasks 能追踪到 W4 Red 结果、来源契约 Green 和下一资金责任目标主体 Grant。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译、测试、PMD | 后续来源契约 Grant 已运行目标验证；W4 本身保留历史确认包，不单独重复验证。 | 目标验证见第 11 节来源契约 Green 回写。 |

## 10. Grant 执行回写（2026-06-15）

用户已确认 `Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001`，本轮按 TDD 先写 Red，并触发 CAD 停止条件。

| 字段 | 内容 |
| --- | --- |
| 执行状态 | `RED_OBSERVED_STOPPED_FOR_CONTRACT_SOURCE` |
| Red 用例 | `FundsAuthorizationTransactionFlowTests#testSharedCardAuthorizationShouldPersistAccountHierarchySnapshotInRoute` |
| 测试目标 | 授权服务流保存的 route snapshot 必须携带 `routingDecision.fundingAllocations[].accountHierarchySnapshot`，且父账户只做归因快照，不成为本次授权 LedgerEntry 主体。 |
| 执行命令 | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one FundsAuthorizationTransactionFlowTests tests` |
| 执行结果 | Red 按预期失败：`Tests run: 31, Failures: 1, Errors: 0`，失败点为 `routeSnapshot.getRoutingDecision()` 为 `null`。 |
| 源码锚点 | `AuthorizationFundsInstructionRouteResolver#route` 当前只复制 `paymentInstrumentRef`、`platformAccounts` 和 context，不生成 `routingDecision`；`DefaultRouteSnapshotFactory#createSnapshot` 已能复制 `routingDecision`；`PaymentInstrumentBinding` / `CreatePaymentInstrumentBindingRequest` 只有绑定主体字段，没有父账户、根账户或层级版本字段。 |
| 停止原因 | 本 Grant 默认 `NO_DDL` 且不允许修改公共 Request/DTO/枚举/状态机；当前服务流没有可追溯的父/根账户来源。通过 `contextVariables` 硬塞核心资金语义会违反第 7 节禁止事项。 |
| 本轮裁决 | 保留 Red 证据，不做半步 Green；生产代码、公共契约、DDL/H2 schema、Entity、Mapper 和运行时配置保持不变。 |
| 下一 Grant 候选 | `GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002`：明确账户层级来源契约，允许在单一授权内选择并实现 account hierarchy application facade / DTO / 账户关系表 / H2 schema 或等价可审计快照来源。 |

下一张 Grant 的最小决策项：

1. 账户层级来源：使用显式账户关系表，还是先通过 wallet application facade 生成一次性 route snapshot 来源。
2. 公共契约范围：是否允许新增非破坏性 Request/DTO/Application Service；若允许，列明 face/impl 包和兼容策略。
3. 数据范围：是否允许 H2 schema、Entity、Mapper 和迁移脚本；若仍坚持 no-ddl，则必须给出不依赖 contextVariables 的可审计来源。
4. 验证范围：继续使用本 Red 做 Green，并追加 route JSON / replay 回归、ledger 主体不漂移和敏感上下文阻断。

## 11. 来源契约 Green 回写（2026-06-15）

`GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 已消费第 10 节 Red 失败证据，并在当前工作树完成最小 Green。

| 字段 | 内容 |
| --- | --- |
| 执行状态 | `SOURCE_CONTRACT_GREEN_VERIFIED_NEXT_FR_TARGET` |
| 生产改动 | 新增账户层级来源 port、wallet-face 账户层级服务契约、wallet-impl 账户关系持久化和快照解析、H2 schema、授权 route snapshot 接入。 |
| 测试改动 | `FundsAuthorizationTransactionFlowTests#testSharedCardAuthorizationShouldPersistAccountHierarchySnapshotInRoute` 通过真实 Spring Bean 绑定账户层级关系，并断言 route snapshot 携带子/父/根账户、层级版本、金额和授权账目主体。 |
| 核心不变量 | 父账户只进入 route snapshot 的 `accountHierarchySnapshot` 和资金分配解释，不成为本次授权 LedgerEntry 主体；敏感上下文仍由 wallet 校验器阻断。 |
| 已验证 | `just compile`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-one AuthorizationFundsInstructionRouteResolverTests tests`、`just test-one PaymentInstrumentRouteDslContractTests tests`、`just test-one RouteSnapshotJsonSupportTests tests`。 |
| 收口验证 | 追加 `test-boundary`、`pmd` 和 `git diff --check`。 |
| 下一 Grant 候选 | `GSD2-B2-FR-TARGET-001`：资金责任目标主体迁移。 |
