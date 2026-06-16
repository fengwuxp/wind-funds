# GSD-2 Wave 3 B2 账户层级 CAD 准入草案

## 1. 文档定位

本文是 `GSD2-W3-B2-AH-CAD-READINESS` 的只读源码定位和 CAD 准入草案，用于把 W2 推荐的 `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` 从“账户层级契约是否存在”收窄为“真实服务流是否能稳定生成、保存、回放和解释账户层级快照”。

本文不是编码授权、不是测试写入授权、不是公共契约变更授权、不是 DDL/H2 schema 授权、不是 OpenSpec 状态变更授权，也不是 Git 提交授权。当前仍需用户明确确认 Execution Grant 后，才允许进入 Red/Green/CAD Loop。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W3-B2-AH-CAD-READINESS` |
| 原子任务 | 只读定位 B2 账户层级源码现状，重排首批 Red，并形成进入 CAD Loop 前的准入草案。 |
| 所属阶段 | GSD-2 Wave 3 / readonly source positioning / CAD readiness draft |
| 关联 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| 当前状态 | `W3_READONLY_SOURCE_POSITIONING_DONE_WAITING_EXECUTION_GRANT` |
| 上游输入 | W1 基线差距审计、W2 单一 Grant 选择卡、当前 Git/code baseline `da7d2ea`、只读源码定位结果。 |
| 推荐 Grant | `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` |
| Owner | 用户确认是否进入代码；产品架构专家确认账户层级业务价值、验收场景和 Not Done；资深架构师确认源码落点、Red 顺序、写入范围、验证命令和停止条件。 |
| 写入范围 | 本准入草案、GSD-2 入口、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-W3-B2账户层级CAD准入草案.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、core route spec/model、transaction route snapshot/replay、JSON support、既有 DSL/route/transaction 测试、Justfile、AGENTS.md 和最近 Git 提交。 |
| Git 策略 | `summary_only`。本文不授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |

Wave 边界：W3 当前只做只读源码定位和 CAD 准入草案；未确认 Execution Grant 前，不进入 Red、不写 Java、不写测试、不改 DDL/H2、不改公共契约。执行顺序是 W1 审计、W2 选择、W3 只读定位、用户确认单一 Grant、W3 Red/Green/CAD Loop。依赖关系上，账户层级服务流证明优先于资金责任目标主体迁移、支付工具 application facade、VCC funding、全球账户出入金和清结算对账。并行边界上，同一时间只允许一个 Grant active；凡共享 route snapshot、FundingAllocation、账本主体或 H2 schema 的候选必须串行。

上下文账本：W1 负责当前基线，W2 负责单一 Grant 草案，本文负责只读源码锚点和首批 Red 重排，OpenSpec tasks 负责工作流恢复入口，后续 handoffs 必须引用本文状态。

## 2. 只读源码定位结论

只读 CR 结论：账户层级 DSL / value object / route snapshot JSON / route replay 边界已经具备局部实现和测试证据。下一轮不应再把“新增账户层级快照字段”作为首个 Red；更合适的 Red 是证明真实交易或授权服务流能从支付工具、账户绑定或资金责任解析中生成该快照，并在生命周期保存、回放和失败路径中不丢失、不漂移、不产生半截事实。

| 证据 | 当前事实 | W3 裁决 |
| --- | --- | --- |
| `core/src/main/java/com/wind/funds/route/spec/AccountHierarchySnapshotSpec.java:14` | 已定义 `accountRef`、`parentAccountRef`、`rootAccountRef` 和非敏感 context。 | 账户层级快照契约不是空白，不作为首个 Red。 |
| `core/src/main/java/com/wind/funds/model/route/ImmutableAccountHierarchySnapshotSpec.java:28` | 构造期要求账户主体和父/根账户关系兼容。 | 继续保留资金/信用账户主体红线，不扩大到 VCC 卡或预算组主体。 |
| `core/src/main/java/com/wind/funds/model/route/ImmutableAccountHierarchySnapshotSpec.java:77` | `accountRef`、`parentAccountRef`、`rootAccountRef` 只允许 `FUNDING_ACCOUNT` 或 `CREDIT_ACCOUNT`。 | 证明“VCC 是支付工具，账务主体落账户”的设计已有模型保护。 |
| `core/src/main/java/com/wind/funds/model/route/ImmutableAccountHierarchyFundingAllocationDecisionSpec.java:30` | 带层级快照的 funding allocation 要求 priority、正金额、原因、主体币种匹配。 | 后续 Red 应围绕真实 funding allocation 生成，而不是单纯 builder 构造。 |
| `core/src/main/java/com/wind/funds/model/route/ImmutableAccountHierarchyFundingAllocationDecisionSpec.java:82` | 账户层级快照的 accountRef 必须与 funding allocation 的 subjectRef 一致。 | 避免 route 中资金主体和层级快照主体分叉。 |
| `transaction/transaction-impl/src/main/java/com/wind/funds/route/DefaultRouteSnapshotFactory.java:21` | route snapshot 工厂原样固化 `routingDecision` 和 `paymentInstrumentRef`。 | 若 resolver 生成了层级快照，snapshot 层具备承载路径；缺口在服务流生成和保存链路证明。 |
| `transaction/transaction-impl/src/main/java/com/wind/funds/transaction/services/impl/RouteSnapshotJsonSupport.java:235` | route snapshot JSON 序列化会输出 `accountHierarchySnapshot`。 | JSON 输出边界已有，不应重复建设。 |
| `transaction/transaction-impl/src/main/java/com/wind/funds/transaction/services/impl/RouteSnapshotJsonSupport.java:503` | route snapshot JSON 反序列化会恢复带账户层级的 funding allocation。 | 回放读取历史快照已有基础，需要服务流级持久化和查询证明。 |
| `tests/src/test/java/com/wind/funds/dsl/PaymentInstrumentRouteDslContractTests.java:157` | 已覆盖 VCC shared card 解析为信用子账户并挂账户层级快照的契约测试。 | 这是 contract-only 证据，不等于真实交易服务流 Done。 |
| `tests/src/test/java/com/wind/funds/dsl/PaymentInstrumentRouteDslContractTests.java:192` | 已覆盖 route snapshot JSON 携带支付工具引用和账户层级快照。 | 可作为回归资产，不再作为首个 Red。 |
| `tests/src/test/java/com/wind/funds/transaction/services/impl/RouteSnapshotJsonSupportTests.java:77` | 已覆盖 JSON 往返后子账户、父账户、根账户、层级版本不丢失。 | 可作为 JSON support 回归资产。 |
| `tests/src/test/java/com/wind/funds/route/DefaultRouteReplayServiceTests.java:204` | 已覆盖 VCC shared card 回放复用原账户层级快照，不按当前绑定重算。 | replay 纯边界已有，下一步应证明从交易事实查询到 replay 的服务流保存完整。 |
| `tests/src/test/java/com/wind/funds/dsl/FundsDslJsonContractTests.java:846` | 已覆盖 DSL JSON 样例可机器校验账户层级快照。 | DSL 文档样例门禁已有，不替代生产链路验收。 |

## 3. 产品和架构裁决

业务目标：让 VCC、全球账户、钱包子账户等场景能解释“实际落账账户、父账户、根账户、绑定版本和支付工具快照”，并在退款、撤销、重放和交易投影解释中沿用原事实。

用户价值：产品、运营、财务、风控和研发可以按卡、按子账户、按主账户追踪资金责任，且不会把 VCC 卡、支付工具、预算组或 Spend Rule 误当成账务主体。

成功指标：下一轮若确认 Grant，必须至少证明真实服务流生成 route snapshot 时包含账户层级快照，生命周期保存和 JSON 往返不丢失，后续 replay 复用原快照，缺层级事实 fail-fast 且无 route/posting/LedgerEntry/projection 副作用。

非目标：不做开户落账、父子账户余额汇总、账单汇总、VCC prepaid funding、shared card 调额、全球账户出入金、支付工具 application facade、Spend Rule 生产控制、清结算对账、外部通道或生产配置。

能力地图：

| 能力域 | 当前成熟度 | 下一步裁剪 |
| --- | --- | --- |
| 账户层级 DSL / value object | 已有局部 contract 证据。 | 保持回归，不作为首个 Red。 |
| route snapshot JSON | 已有序列化和反序列化证据。 | 保持回归，补真实保存链路。 |
| route replay | 已有纯 replay 边界证据。 | 补从交易事实查询到 replay 的服务流证据。 |
| 交易 / 授权服务流 | 未证明真实服务流生成账户层级快照。 | 作为 W3 首个 Red 方向。 |
| 钱包账户 / 支付工具 facade | 仍是依赖方或后续入口。 | 不在本 Grant 内实现。 |

业务对象：资金账户、信用账户、子账户、父账户、根账户、VCC 卡支付工具、账户层级快照、支付工具绑定快照、资金来源决策、route snapshot、FundsTransaction、LedgerTransaction、LedgerEntry、余额投影和交易投影。

对象模型和字段口径：实际入账主体只允许资金账户或信用账户；父账户和根账户是约束、汇总和归因上下文；context 只能放非敏感解释字段，不放完整卡号、外部账户原文、通道密钥或凭据。

生命周期 / 状态：本 Grant 只证明交易当时的账户层级快照事实和后续回放稳定性，不定义账户开户、启用、冻结、关闭、销户、额度调整、账单出账或清结算状态机。

业务流程：

1. 支付工具或上游业务解析到资金账户 / 信用账户。
2. 资金责任解析识别账户层级关系和绑定版本。
3. route resolver 生成 funding allocation，并挂载账户层级快照。
4. route snapshot 工厂固化 routingDecision、paymentInstrumentRef 和外部引用。
5. 生命周期保存把 route snapshot 写入交易事实。
6. 退款、撤销、过期、重放、投影解释使用原 route snapshot，不按当前绑定重算。
7. 缺少必要层级事实时 fail-fast，且不得产生半截 route、posting、LedgerEntry、余额投影或交易投影。

规则矩阵：

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验证方式 | 确认方 |
| --- | --- | --- | --- | --- | --- | --- |
| 可入账主体 | route funding allocation 指向资金责任主体。 | 只能是资金账户、信用账户或平台角色解析后的平台资金账户。 | P0 | 2026-06-12 | 服务流 Red + DSL 回归。 | 资深架构师 |
| 父/根账户 | 子账户或卡绑定账户参与交易。 | 快照记录父账户 / 根账户，但不自动把父账户写成 posting 主体。 | P0 | 2026-06-12 | route snapshot 和 ledger posting 断言。 | 产品架构专家 + 资深架构师 |
| 原快照回放 | 退款、撤销、过期、重放或投影解释。 | 必须用原 route snapshot 的账户层级和支付工具快照。 | P0 | 2026-06-12 | replay 服务流测试。 | 资深架构师 |
| 敏感上下文 | contextVariables 或 bindingSnapshot 写入。 | 不得保存完整卡号、外部账户原文、通道密钥、凭据或隐私字段。 | P0 | 2026-06-12 | 敏感上下文回归。 | 资深架构师 |
| P2 不抢跑 | VCC 或全球账户要求业务实现。 | 本 Grant 只做账户层级服务流证明，不实现 P2 facade。 | P0 | 2026-06-12 | Not Done 清单。 | 用户 + 产品架构专家 |

运营后台 / 数据口径：后续运营查询可以按 `accountRef`、`parentAccountRef`、`rootAccountRef` 和 `paymentInstrumentRef` 解释交易归属；本 Grant 不新增后台页面、报表、导出、指标或审计表。

风险 / 待确认 / 验收：主要风险是把现有 contract-only 证据误读为生产服务流 Done；待确认是是否允许写目标测试和最小 route/service 实现；验收以服务流 Red、失败无副作用、验证命令和 Not Done 为准。

发布：本文不发布生产能力。后续若只改测试和 contract，不产生生产发布；若改生产代码或公共契约，必须在 Grant 中补灰度、回滚、兼容和风险。

## 4. W3 首批 Red 重排

W2 的 `firstRedSet` 需要按只读源码事实调整：已存在的 DSL / JSON / replay 纯边界不再作为首个 Red；首批 Red 应切向真实服务流和失败副作用。

| Red ID | 验收场景 | 最小断言 | 候选目标资产 | 当前授权 |
| --- | --- | --- | --- | --- |
| `B2-AH-RED-001-SERVICE-FLOW-SNAPSHOT` | 支付工具或账户绑定解析到子资金/信用账户后，真实交易或授权服务流生成 route snapshot。 | funding allocation 的 subjectRef 是资金/信用账户；accountHierarchySnapshot 含 accountRef、parentAccountRef、rootAccountRef；paymentInstrumentRef 只保存脱敏快照。 | `FundsAuthorizationTransactionFlowTests`、`FundsDirectTransactionFlowTests` 或更小的 route/orchestrator 服务流测试。 | 待确认，不可写。 |
| `B2-AH-RED-002-LIFECYCLE-PERSISTENCE-REPLAY` | 原交易保存后，退款、撤销或过期从交易事实查询 route snapshot 并 replay。 | 原账户层级快照不被当前绑定覆盖；replay route、posting plan、ledger entry 和投影解释沿用原快照。 | `DefaultRouteReplayServiceTests` 的服务流增强、交易 flow 回归或 `RouteSnapshotJsonSupportTests` 联动。 | 待确认，不可写。 |
| `B2-AH-RED-003-MISSING-HIERARCHY-FAILFAST` | 配置要求账户层级但缺父账户、根账户或主体币种不一致。 | fail-fast；不保存 FundsTransaction route snapshot；不生成 posting、LedgerEntry、余额投影或交易投影；错误信息可解释。 | `DefaultRoutedFundsInstructionOrchestratorProjectionTests`、交易 flow 失败路径或资金责任 resolver 测试。 | 待确认，不可写。 |

最小 AC：

| AC ID | 验收口径 |
| --- | --- |
| `AC-B2-AH-SF-001` | 真实服务流能把支付工具 / 账户绑定解析结果转成资金或信用账户层级快照，并进入 route snapshot。 |
| `AC-B2-AH-SF-002` | 交易生命周期保存后的 route snapshot JSON 往返不丢失账户层级。 |
| `AC-B2-AH-SF-003` | 后续 replay 使用原快照，不受当前支付工具绑定或账户层级变化影响。 |
| `AC-B2-AH-SF-004` | 缺账户层级事实或不一致时失败无副作用。 |
| `AC-B2-AH-SF-005` | 明确 Not Done：不声明开户落账、余额可用、父子汇总、VCC funding、全球账户或支付工具 facade 生产完成。 |

## 5. 架构约束和实现边界

背景和目标：当前已有账户层级快照模型、JSON 支持和 replay 测试，但真实交易服务入口是否生成并保存该快照仍未被证明。W3 的目标是把下一步编码范围压成一个服务流证明切片。

现状和影响范围：预期影响 `core` route model、`transaction-impl` route resolver / orchestrator / lifecycle saver、`tests` 目标流程测试和少量文档状态；实际写入文件必须在用户确认 Grant 后由资深架构师二次收窄。

核心决策：

1. 不新增 `VCC_ACCOUNT`，VCC 卡继续是支付工具。
2. 账户层级快照的实际入账主体只能是资金账户或信用账户。
3. 父账户 / 根账户默认是归因、约束和汇总解释上下文，不默认成为 posting 主体。
4. route snapshot 是历史事实边界，replay 不按当前绑定重算。
5. contract-only 证据不等于 service-flow-backed 生产 Done。

职责边界：wallet 产品门面只编排资金指令；route 解析资金路径并生成快照；transaction lifecycle 保存交易事实；ledger 维护账本事实和投影；测试必须断言 route、posting、ledger、余额和失败副作用。

接口契约：当前草案默认不改公共 Request/DTO/枚举/状态机。若 Red 证明必须新增入参、出参、错误码、幂等摘要、application facade、DTO 或枚举，必须停止并更新 Execution Grant。

数据方案：默认 `NO_DDL`。若需要资金账户父子字段、账户关系表、H2 schema、Entity、Mapper、索引、唯一键或迁移脚本，必须停止回到用户确认。

事务边界、一致性、补偿和对账：服务流必须在同一交易事实保存边界内固化 route snapshot；失败路径不得产生半截 route、posting、ledger entry、余额投影或交易投影。清结算和对账只读参考，不在本 Grant 写入。

可靠性、安全、权限、审计和告警：可靠性重点是 replay 不漂移；安全重点是敏感上下文不落库；权限重点是运营调账、补事实和生产动作不进入本 Grant；审计重点是 route snapshot 可追溯；告警不在本轮新增。

验证方案：确认 Grant 后先写 Red，再用最小实现 Green，随后运行目标测试、相关分组测试、`just compile` 和 `git diff --check`；公共契约或跨模块行为变化时追加 `just test-transaction`、`just test-boundary`、`just pmd` 或 `just verify-slice`。

发布、灰度、回滚、风险和待确认：本文无发布。后续若只写测试可无灰度；若写生产代码，回滚方式为撤销对应 diff 并保留 Red 证据；待确认是是否允许 Java/test 写入、是否允许公共契约变化、是否允许 DDL/H2、是否允许 Git 提交。

## 6. 候选写入范围和禁止事项

| 范围 | 草案 |
| --- | --- |
| 当前允许写入 | 仅本文和入口状态文档。 |
| 确认 Grant 后候选写入 | 目标测试、最小 route/service 实现、必要 converter/assembler、文档状态回写。 |
| 条件写入 | 公共契约、DTO、枚举、状态机、H2 schema、Entity、Mapper、fixture 结构；必须在 Execution Grant 中逐项列出。 |
| 禁止事项 | 未确认前不得写 Java、测试、公共契约、DDL/H2、运行时配置、Git、联网、依赖安装、生产配置或不可逆操作。 |
| 持续禁止 | 不做 VCC application facade、支付工具 application facade、资金责任目标主体迁移、Spend Rule、清结算对账、全球账户、收单、外部通道、CI 和生产配置。 |
| 撤销方式 | 用户未确认或撤销时，仅保留本文作为只读准入证据；如后续已写代码，按 Git diff 回退未提交切片并保留 Red/验证记录。 |

## 7. TDD / CAD Loop 候选

| 步骤 | 动作 | Review 关注 | 验证命令候选 |
| --- | --- | --- | --- |
| 0 | 用户确认 `Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001`。 | 授权范围、写入文件、schemaNeed、Git 策略、停止条件。 | 不适用。 |
| 1 | 选择一个服务流 Red，不并行打开多个能力域。 | TDD 是否表达真实资金不变量和失败无副作用。 | `just test-one <TargetTest> tests`。 |
| 2 | 最小 Green：只让 resolver/orchestrator/lifecycle 能生成并保存账户层级快照。 | 编码红线、模块边界、AI 产物复核、敏感上下文、幂等摘要。 | `just test-one <TargetTest> tests`、`just compile`。 |
| 3 | 回归现有 contract / JSON / replay 资产。 | 避免破坏已存在 DSL、JSON 往返和原快照 replay。 | `just test-one PaymentInstrumentRouteDslContractTests tests`、`just test-one RouteSnapshotJsonSupportTests tests`、`just test-one DefaultRouteReplayServiceTests tests`。 |
| 4 | CR 和状态回写。 | 业务语义、边界方向、契约完整性、失败路径、工程一致性。 | `git diff --check`，必要时 `just test-transaction`、`just test-boundary`、`just pmd`。 |

AI 产物复核要求：不得引入不存在的 API、不得把支付工具或预算组作为账务主体、不得通过 contextVariables 承载核心资金语义、不得绕过生命周期保存或 ledger posting 断言。

## 8. 停止条件

1. 用户未确认 Execution Grant，或确认文本不包含写入范围、验证命令和停止条件。
2. 需要公共契约、DDL/H2 schema、状态机、账户关系表、Entity、Mapper 或跨模块依赖变化，但 Grant 未显式授权。
3. Red 证明缺口实际属于资金责任目标主体、支付工具 application facade、VCC funding、全球账户、清结算对账或外部通道。
4. 需要联网、依赖安装、生产配置、真实资金、外部规则、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规确认。
5. 验证失败且无法在授权范围内修复。
6. 工作树出现用户未归属变更，且影响目标文件。

## 9. Execution Handoff Card

| 字段 | 内容 |
| --- | --- |
| 当前 Wave / Task | `GSD2-W3-B2-AH-CAD-READINESS` |
| 当前状态 | `W3_READONLY_SOURCE_POSITIONING_DONE_WAITING_EXECUTION_GRANT` |
| 建议确认文本 | `Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` |
| 下一 Wave / Task | 用户确认后进入 `GSD2-W3-CAD-LOOP-ACTIVE / B2-AH-SERVICE-FLOW`。 |
| 写入范围 | 当前只允许文档状态；确认后由资深架构师按首个 Red 二次收窄。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、最近 Git 提交和历史准入卡。 |
| 反馈源 | `rg`、目标测试、`just compile`、`git diff --check`、专项分组测试和用户确认。 |
| Git 策略 | `summary_only`，除非用户后续明确要求提交并且验证通过。 |
| 交接要求 | 确认后先选择一个 Red，写失败用例，再最小 Green；完成后回写 AC、验证命令、Not Done 和残余风险。 |

## 10. 验证矩阵

| 验证层 | 命令或方式 | 通过口径 |
| --- | --- | --- |
| Harness 结构 | `check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-W3-B2账户层级CAD准入草案.md` | Task、Owner、范围、Wave、上下文账本、禁止事项、验证和 handoff 字段齐全。 |
| CAD 候选结构 | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-W3-B2账户层级CAD准入草案.md` | 写入范围、验证、TDD/Review、Execution Grant、人工确认、撤销方式和交接字段齐全。 |
| 产品结构 | `check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-W3-B2账户层级CAD准入草案.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构结构 | `check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-W3-B2账户层级CAD准入草案.md` | 背景目标、现状约束、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-W3|W3_READONLY_SOURCE_POSITIONING|B2-AH-SERVICE-FLOW" docs openspec` | GSD2 入口、README 和 OpenSpec tasks 能追踪到 W3 准入草案。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译、测试、PMD | 本轮不运行。 | 本轮仅文档和只读源码定位，不改 Java、测试、DDL/H2 或运行时配置。 |
