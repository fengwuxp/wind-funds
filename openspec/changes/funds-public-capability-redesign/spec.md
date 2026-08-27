# 支付资金公共能力层重设计执行规格

## Change Metadata

| Field | Value |
| --- | --- |
| Change ID | `funds-public-capability-redesign` |
| Origin runtime Goal | `019ff383-2af1-7e82-ad2c-19913a6bb1d2 / PAUSED / provenance_only`；不是计划恢复或切片准入前置 |
| Plan state | `ACTIVE / REFACTORING_PLAN_V2 / W5_DOCUMENTATION_COMPLETION_REVIEW` |
| State revision | `plan-r2.323 / 2026-08-27` |
| Current task / next entry | `W5-MIG09-CREDIT-ACCOUNT-QUERY-SURFACE-NARROWING-RED-EXECUTION-001 / RED_INDEPENDENT_CHECKER_PASS / RED_TEST_IMMUTABLE / GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`；下一门只允许冻结五文件 Provider Green |
| Closed documentation gate history | `MIG-02C_REFUND_DOCUMENT_CARD_CHECKER_PASS / D-MIG-001-R_DECISION_PACKAGE_CHECKER_PASS / D-MIG-001-R-A_ACCEPTANCE_CHECKER_PASS / MIG-04_DOCUMENT_CARD_CHECKER_PASS / MIG-05_DOCUMENT_CARD_CHECKER_PASS / MIG05_TRANSACTION_LEDGER_REFERENCE_A_ACCEPTANCE_CHECKER_PASS / MIG05_TRANSACTION_LEDGER_REFERENCE_ENTRY_CARD_CHECKER_PASS / MIG05B_LEDGER_PROFILE_OWNERSHIP_ENTRY_CARD_CHECKER_PASS / MIG-07_DOCUMENT_CARD_REWORK_CHECKER_PASS / MIG-06_08_EVIDENCE_INVENTORY_CHECKER_PASS / MIG-06_08_CONSUMER_ROLE_CALIBRATION_DOCUMENT_CARD_CHECKER_PASS / MIG07_RECONCILIATION_CONTRACT_DECISION_PACKAGE_REWORK_CHECKER_PASS / MIG07_RECONCILIATION_CONTRACT_A_ACCEPTED / MIG07_RECONCILIATION_CONTRACT_A_ACCEPTANCE_CHECKER_PASS / SOURCE_RUN_ENTRY_CARD_CHECKER_PASS / SOURCE_RUN_CONTRACT_ACCEPTED / SOURCE_RUN_CONTRACT_ACCEPTANCE_CHECKER_PASS / GATE_REQUIREMENT_ENTRY_CARD_CHECKER_PASS / GATE_REQUIREMENT_CONTRACT_ACCEPTED / GATE_REQUIREMENT_CONTRACT_ACCEPTANCE_CHECKER_PASS / BREAKING_RELEASE_RED_ENTRY_CARD_FROZEN / RED_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / BASELINE_STATUS_MAPPING_REPAIR_CHECKER_PASS / BREAKING_RELEASE_RED_ENTRY_CARD_REFREEZE_CHECKER_PASS / BREAKING_RELEASE_RED_EXECUTION_GRANT_YES / BREAKING_RELEASE_RED_EXECUTION_COMPLETE / BREAKING_RELEASE_RED_INDEPENDENT_CHECKER_PASS / GREEN_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_NOT_PASS / BEHAVIORAL_RED_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_PASS / CONTRACT_SURFACE_GREEN_EXECUTION_GRANT_YES / CONTRACT_SURFACE_GREEN_EXECUTION_COMPLETE / CONTRACT_SURFACE_GREEN_INDEPENDENT_CHECKER_PASS / BEHAVIORAL_RED_EXECUTION_GRANT_YES / BEHAVIORAL_RED_EXECUTION_COMPLETE / BEHAVIORAL_RED_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_YES / GREEN_EXECUTION_COMPLETE / GREEN_INDEPENDENT_CHECKER_PASS`；均为已关闭历史事实，不构成当前授权 |
| Closed design gate history | `W4-02_CAPTE_BENEFIT_L3_CHECKER_PASS / CAPTE_BENEFIT_E4_PASS / CAPTE_DOMAIN_LIBRARY_INTEGRATION_CHECKER_PASS / P-SIM03_HOST_DEFERRED / W5-01_GREEN_CHECKER_PASS / W5-01_DIRECT_PRIMARY_RECOVERY_ENTRY_CARD_CHECKER_PASS / CONTRACT_INQUIRY_ACCEPTED / RED_EXECUTION_GRANT_YES / RED_REWORK_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_YES / W5-01_DIRECT_PRIMARY_RECOVERY_GREEN_CHECKER_PASS / W5-01_AUTHORIZATION_ROOT_ACTIONFACT_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / AUTHORIZATION_ROOT_CONTRACT_INQUIRY_ACCEPTED / AUTHORIZATION_ROOT_RED_EXECUTION_GRANT_YES / RED_EXECUTION_COMPLETE / RED_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_YES / GREEN_IMPLEMENTATION_COMPLETE / W5-01_AUTHORIZATION_ROOT_GREEN_CHECKER_PASS / W5-01_AUTHORIZATION_COMPLETE_ACTIONFACT_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / AUTHORIZATION_COMPLETE_CONTRACT_INQUIRY_ACCEPTED / AUTHORIZATION_COMPLETE_RED_EXECUTION_GRANT_YES / AUTHORIZATION_COMPLETE_RED_EXECUTION_COMPLETE / AUTHORIZATION_COMPLETE_RED_REWORK_INDEPENDENT_CHECKER_PASS / AUTHORIZATION_COMPLETE_GREEN_EXECUTION_GRANT_YES / AUTHORIZATION_COMPLETE_GREEN_IMPLEMENTATION_COMPLETE / W5-01_AUTHORIZATION_COMPLETE_GREEN_CHECKER_PASS / MIG07_BEHAVIORAL_GREEN_CHECKER_PASS / L4_EXECUTION_GRANT_NO`；均为成熟度或已关闭历史事实 |
| Closed current-slice execution history | `W5-MIG05_RED_EXECUTION_GRANT_YES / W5-MIG05_RED_EXECUTION_COMPLETE / W5-MIG05_RED_INDEPENDENT_CHECKER_PASS / W5-MIG05_GREEN_EXECUTION_GRANT_YES / W5-MIG05_TEST_REWORK_EXECUTION_GRANT_YES / W5-MIG05_TEST_REWORK_EXECUTION_COMPLETE / W5-MIG05_TEST_CONTRACT_REWORK_INDEPENDENT_CHECKER_PASS / W5-MIG05_GREEN_INDEPENDENT_CHECKER_PASS / W5-MIG05B_RED_EXECUTION_GRANT_YES / W5-MIG05B_RED_INDEPENDENT_CHECKER_NOT_PASS / MIG05B_CONTRACT_SURFACE_EXECUTION_GRANT_YES / MIG05B_CONTRACT_SURFACE_EXECUTION_COMPLETE / MIG05B_CONTRACT_SURFACE_INDEPENDENT_CHECKER_PASS / MIG05B_BEHAVIORAL_RED_EXECUTION_GRANT_YES / MIG05B_BEHAVIORAL_RED_EXECUTION_COMPLETE / MIG05B_BEHAVIORAL_RED_INDEPENDENT_CHECKER_PASS / MIG05B_BEHAVIORAL_GREEN_EXECUTION_GRANT_YES / MIG05B_BEHAVIORAL_GREEN_EXECUTION_PARTIAL / MIG05B_BEHAVIORAL_GREEN_INDEPENDENT_CHECKER_NOT_PASS / W5-MIG05B_EXTERNAL_FUNDS_LEG_RED_EXECUTION_GRANT_YES / W5-MIG05B_EXTERNAL_FUNDS_LEG_RED_EXECUTION_COMPLETE / W5-MIG05B_EXTERNAL_FUNDS_LEG_RED_INDEPENDENT_CHECKER_NOT_PASS / W5-MIG05B_EXTERNAL_FUNDS_LEG_GREEN_EXECUTION_GRANT_YES / W5-MIG05B_EXTERNAL_FUNDS_LEG_GREEN_EXECUTION_PARTIAL / W5-MIG05B_EXTERNAL_FUNDS_LEG_GREEN_INDEPENDENT_CHECKER_NOT_PASS / W5-MIG05B_EXTERNAL_FUNDS_LEG_TEST_REWORK_EXECUTION_GRANT_YES / W5-MIG05B_EXTERNAL_FUNDS_LEG_TEST_REWORK_EXECUTION_COMPLETE / W5-MIG05B_EXTERNAL_FUNDS_LEG_TEST_REWORK_INDEPENDENT_CHECKER_PASS / W5-MIG05B_FUNDING_BALANCE_ADJUST_RED_EXECUTION_GRANT_YES / W5-MIG05B_FUNDING_BALANCE_ADJUST_RED_EXECUTION_COMPLETE / W5-MIG05B_FUNDING_BALANCE_ADJUST_RED_INDEPENDENT_CHECKER_PASS / W5-MIG05B_FUNDING_BALANCE_ADJUST_GREEN_EXECUTION_GRANT_YES / W5-MIG05B_FUNDING_BALANCE_ADJUST_GREEN_EXECUTION_COMPLETE / W5-MIG05B_FUNDING_BALANCE_ADJUST_GREEN_INDEPENDENT_CHECKER_PASS / W5-MIG05B_EXTERNAL_FUNDS_LEG_GREEN_EXECUTION_COMPLETE / W5-MIG05B_EXTERNAL_FUNDS_LEG_GREEN_INDEPENDENT_CHECKER_PASS / W5-MIG03_RED_EXECUTION_GRANT_YES / W5-MIG03_RED_EXECUTION_COMPLETE / W5-MIG03_RED_INDEPENDENT_CHECKER_PASS / MIG04_RED_EXECUTION_GRANT_YES / MIG04_RED_EXECUTION_COMPLETE / MIG04_RED_INDEPENDENT_CHECKER_PASS / MIG04_GREEN_EXECUTION_GRANT_YES / MIG04_GREEN_IMPLEMENTATION_COMPLETE / MIG04_GREEN_INDEPENDENT_CHECKER_PASS / MIG05C_RED_EXECUTION_GRANT_YES / MIG05C_RED_EXECUTION_COMPLETE / MIG05C_RED_INDEPENDENT_CHECKER_PASS / MIG05C_GREEN_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_PASS`；均为已消耗历史事实，不构成当前授权 |
| Current execution authorization | `MIG09_CREDIT_ACCOUNT_QUERY_SURFACE_NARROWING_RED_EXECUTION_GRANT_CONSUMED / RED_INDEPENDENT_CHECKER_PASS / RED_TEST_IMMUTABLE / GREEN_NO / CAPTE_CONSUMER_NO / GIT_NO / NETWORK_NO / REMOTE_PUBLISH_NO / DEPLOY_PRODUCTION_NO / CODE_FREEZE` |
| Execution basis | 本 Change 的 `state_revision + accepted decisions + authority refs/fingerprints + slice eligibility`；runtime Goal 仅为历史 provenance，不参与运行绑定 |
| Design maturity | `DRAFT / ROUND_1 / G1_PASS / W2-01_CHECKER_PASS / W2-02_CHECKER_PASS / W3-01~04_CHECKER_PASS / W4-01_CHECKER_PASS / W4-02_VALIDATION_PLAN_CHECKER_PASS / W4-02_CAPTE_BENEFIT_L3_CHECKER_PASS / CAPTE_BENEFIT_E4_PASS / CAPTE_DOMAIN_LIBRARY_INTEGRATION_CHECKER_PASS / W5-01_GREEN_CHECKER_PASS / W5-01_DIRECT_PRIMARY_RECOVERY_ENTRY_CARD_CHECKER_PASS / CONTRACT_INQUIRY_ACCEPTED / RED_REWORK_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_YES / W5-01_DIRECT_PRIMARY_RECOVERY_GREEN_CHECKER_PASS / W5-01_AUTHORIZATION_ROOT_ACTIONFACT_ENTRY_CARD_CHECKER_PASS / AUTHORIZATION_ROOT_CONTRACT_INQUIRY_ACCEPTED / AUTHORIZATION_ROOT_RED_EXECUTION_GRANT_YES / RED_EXECUTION_COMPLETE / RED_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_YES / GREEN_IMPLEMENTATION_COMPLETE / W5-01_AUTHORIZATION_ROOT_GREEN_CHECKER_PASS / W5-01_AUTHORIZATION_COMPLETE_ACTIONFACT_ENTRY_CARD_CHECKER_PASS / AUTHORIZATION_COMPLETE_CONTRACT_INQUIRY_ACCEPTED / AUTHORIZATION_COMPLETE_RED_EXECUTION_GRANT_YES / AUTHORIZATION_COMPLETE_RED_EXECUTION_COMPLETE / AUTHORIZATION_COMPLETE_RED_REWORK_INDEPENDENT_CHECKER_PASS / AUTHORIZATION_COMPLETE_GREEN_EXECUTION_GRANT_YES / AUTHORIZATION_COMPLETE_GREEN_IMPLEMENTATION_COMPLETE / W5-01_AUTHORIZATION_COMPLETE_GREEN_CHECKER_PASS / P-SIM03_HOST_DEFERRED / VC-001_BLOCKED / VC-002_BLOCKED / L4_EXECUTION_GRANT_NO`；完整决策历史见正文与 Decision Register |
| Date | `2026-08-23` |
| Baseline HEAD | `eb12091819152fcec529f9453b48755f3aa2c999` |
| E4 assessment source baseline | `0ed7bbdb4664431ab630c46ef9f76e5899484cc7`；该值只记录本次谱系评估采用的 Provider source，不表示后续 live HEAD；任何 E4 执行前仍须重新冻结两仓 HEAD 与完整 dirty fingerprint |
| Evidence workspace | `Credit RED source=8c2fe2e6... / fresh XML=74ea8ddb... / 3 tests / 2 failures / 0 errors / 0 skipped / pre-post compile=21/21 / independent Checker PASS`；未修改 production、既有测试、Capte、POM 或 schema，未执行 Git/联网/发布 |
| Human Owner | 用户：目标、产品边界、公共契约和风险取舍的最终确认方 |
| Plan steward | `wise-agent`：维护本 Change 的单一持久状态、逐切片综合和交接；不替代 Human Owner，也不自行产生执行授权 |
| Current Maker | 产品、支付资金、DSL 与系统设计能力共同主笔；不形成多个事实源 |
| Independent Checker | `funds_core_evidence`：`G0 PASS`；`w1_product_checker`：`W1-01 PASS`；`w1_completion_checker`：`W1-02 COMPLETION EVIDENCE PASS`；`q002_decision_checker`：`Q-002 / Q-003 / Q-004 / P-SIM01-01 / P-SIM01-03 DECISION PACKAGE PASS`、`Q-004 ACCEPTANCE_AND_BREAKING_REDESIGN PASS`、`P-SIM01-01-D / P-SIM01-02-A / P-SIM01-03 / P-SIM02-01-A / P-SIM02-02-A / P-SIM03-01-D / P-SIM03-02-R-A / P-SIM04-01-A / P-SIM04-02-D / P-SIM05-01-A ACCEPTANCE PASS`、`P-SIM02-01 / P-SIM02-02 / P-SIM03-01 / P-SIM03-02 / P-SIM04-01 / P-SIM04-02 / P-SIM05-01 DECISION PACKAGE PASS`、`SIM-01~07 CONDITIONAL CONTRACTS PASS`、`RS-001 PASS`、`CI-RS001-CAPTE-WALLET-001 PASS / VC-001 BLOCKED`；`capte_domain_evidence`：`P-SIM06-01 DECISION PACKAGE PASS / 0 P0-P2`、`P-SIM06-01-A ACCEPTANCE PASS / 0 P0-P2`、`P-SIM06-02 DECISION PACKAGE PASS / 0 P0-P2`；`fincone_consumer_evidence`：`P-SIM06-02-B ACCEPTANCE PASS / 0 P0-P2`、`P-SIM06-03 DECISION PACKAGE PASS / 0 P0-P2`、`P-SIM06-03-B ACCEPTANCE PASS / 0 P0-P2`、`P-SIM07-01-A ACCEPTANCE PASS / 0 P0-P2`、`W1-02 / G1 PRODUCT READINESS PASS / 0 P0-P2`、`W3-01 SYSTEM DESIGN PASS / 0 P0-P2`、`W3-02 SYSTEM DESIGN PASS / 0 P0-P2`、`W3-03 SYSTEM DESIGN PASS / 0 P0-P2`、`W3-04 SYSTEM DESIGN PASS / 0 P0-P2`、`W4-01 TDD DESIGN PASS / 0 P0-P2`、`W4-02 VALIDATION PLAN PASS / 0 P0-P2` |
| Latest independent checker | `plan-r2.323 / CREDIT ACCOUNT QUERY SURFACE RED INDEPENDENT CHECKER PASS / P0=0 / P1=0 / P2=0`；失败映射、真实 Credit/Ledger/Profile/H2、四 bucket、零副作用、不泄露与 immutable RED 一致 |
| Git strategy | `summary_only`；未授权 stage、commit、push 或 PR |

## 1. 当前结论

本 Change 重新开始设计 `wind-funds` 的产品语义、DSL、`core` 抽象，以及 `ledger`、`wallet`、`transaction`、`reconciliation` 的公共接口、契约和能力边界。

当前已完成 Round 0 执行计划、证据基线、`Q-001 / W1-01` 产品定位、`Q-002` 对象边界、`Q-003` 正交完成证据语义、`Q-004` 业务意图身份与重放契约，以及 `P-SIM01` 至 `P-SIM07` 已列 Owner 决策与接受范围复核。`W1-02 / G1`、`W2-01`、`W2-02`、`W3-01` 至 `W3-04`、`W4-01`、`W4-02` 验证计划和 `CAPTE-BENEFIT` 当前测试宿主 L3/E4 均经独立 Checker 判定 `PASS / 0 P0-P2`。`capte-domain` 是无独立生产数据库和部署进程的通用领域模块；三个 Provider impl 仅在集中 `tests` 以 test scope 装配属于正确边界，不要求该仓提供生产 composition root、migration 或 L4。`P-SIM03-HOST` 只在首个真实可部署 Consumer 出现时恢复，届时由该 Consumer 负责数据库、运行装配、`SPECIFIED` 配置和部署证据；本轮不批准生产 API/DDL/配置、Git、enable/release/production。

原 runtime Goal 当前为 `paused`，本 Change 自 `plan-r2` 起独立持有重构计划的持久状态和恢复入口。runtime Goal 只记录来源，不再作为逐切片执行前置；后续由用户按本计划逐步发起单个切片，代码写入仍需该切片自己的授权、白名单、验证和停止条件。本轮只修执行规范，不确认目标态 Java 接口，也不修改生产代码。现有 `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、历史 OpenSpec 和当前实现都只作为输入证据：

- 当前源码和测试可以证明现状行为，不能自动定义正确产品语义。
- 旧设计可以提供问题、候选和历史取舍，不能直接继承为目标态。
- 只有在本 Change 中逐轮确认、经过对应 Checker 后，新的正式产品、DSL、系分和 TDD 文档才成为目标态设计来源。
- 未确认前不得以旧 API 已存在、旧文档自称权威或 API baseline 已冻结为理由保留公共抽象。

改变公共产品语义、DSL 或 Consumer 可观察契约的切片仍按以下顺序收敛；只读取证、API/Consumer 清册、保护网和行为保持型内部重构，只需通过本切片的 `eligibility`，不等待与其无关的全局产品 PENDING：

1. 产品设计：确认公共能力层为谁服务、承担什么、不承担什么，以及核心场景、主体、对象、流程和验收摘要。
2. DSL 设计：从已确认产品语义提炼最小稳定资金语言和 `core` 边界。
3. 系分设计：映射模块职责、公共接口、数据、状态、事务、一致性、破坏式切换范围和 Consumer 同步迁移方案。
4. TDD 设计：把已确认语义和契约转成 Red、行为断言、跨仓组合验证和准出门禁。
5. 实施计划：只对本切片实际改变的语义要求对应四层结论；行为保持型切片以现有契约、保护测试和独立 CR 为准。本 Change 当前不授予实现或 Git 权限。

## 2. 计划目标与成功标准

### 2.1 Objective

从当前源码、测试和真实业务接入项目出发，设计一套可用、易用、抽象好理解、层次清晰的资金公共能力层，使不同宿主可以复用同一套资金账户、交易、账本、余额、清结算和对账能力，而不复制场景专用资金内核。

### 2.2 Success Criteria

| ID | 完成条件 | 完成证据 |
| --- | --- | --- |
| `SC-001` | 产品设计能解释目标用户、业务主体、资金责任、能力边界、正逆异常场景和非目标。 | 人类 Owner 确认；产品 Checker；最新版 PRD validator。 |
| `SC-002` | DSL 只保留跨场景稳定语义，关键资金事实不依赖 `Map<String, Object>`、场景私有枚举或宿主内部对象。 | DSL 用例、JSON 契约、负例和跨场景走读。 |
| `SC-003` | `core` 每个 public 类型都有稳定不变量、真实消费者、边界理由和明确目标处置；无证据类型被质疑、下沉或删除。 | public API 清册、consumer 证据、依赖方向检查和 Owner 决策。 |
| `SC-004` | `ledger`、`wallet`、`transaction`、`reconciliation` 各自拥有明确事实、写入责任、查询责任和禁止边界。 | 系分正文、接口契约表、状态/事务/失败语义和架构 Checker。 |
| `SC-005` | 公共接口对调用方简单，对资金风险显式：输入前提、业务输出、副作用、幂等、冲突、失败、恢复和查询证据齐全。 | API 使用样例、反例、契约测试设计和宿主走读。 |
| `SC-006` | `capte-domain`、`fincone`/目标运行时宿主及 `wind-funds` 自身场景能用同一组核心语义模拟，场景差异不污染稳定内核。 | 跨项目证据卡、业务模拟、事实/推断/待确认分层。 |
| `SC-007` | TDD 能验证金额、余额桶、posting 平衡、ledger transaction/entry 可追溯、幂等/冲突、逆向原路径和失败零副作用。 | TDD 矩阵、最小 Red、验证命令与独立 Checker。 |
| `SC-008` | 每个目标态契约变更都在一个垂直切片内完成 Provider 与全部已知真实 Consumer 的同步切换，不保留兼容层、双轨或延迟退役。 | 破坏式变更清册、垂直切片、宿主谱系验证和停止条件。 |

### 2.3 Non-Goals

- 不在 Round 0 直接设计 Java 类、DTO 字段、数据库表或实现代码。
- 不把订单、优惠券、VCC、银行卡、ACH、支付通道、营销、佣金公式或外部协议生命周期搬进公共 Core。
- 不为当前没有真实消费者的场景预建接口、策略、插件、工厂或通用扩展框架。
- 不用模拟 Service、Recording/Fake、Provider 单仓测试或旧文档替代跨仓 real Bean 集成证据。
- 不在本 Change 中执行联网、安装、变更性 Git、部署、生产、数据迁移、删除或不可逆动作；允许 `status`、`diff` 等只读 Git 检查。
- `RS-000 / RS-001` 不修改当前工作区已有的未提交文件；后续代码切片必须冻结既有 diff fingerprint、明确重叠文件处置和隔离/回滚方式，未获切片授权不得触碰。

### 2.4 Stop Conditions

命中任一条件即停止当前轮，不把推断写成设计结论：

- 产品主体、资金归属、责任 Owner 或完成层级无法确认。
- 新结论会改变公共契约，但缺少人类 Owner 确认。
- Consumer 缺口可能只是依赖版本、Bean 装配、schema、事务管理器或映射错误，尚未用最小 RED 排除。
- 关键事实只能放入 `contextVariables` 或 `Map<String, Object>` 才能表达。
- 同一幂等标识下金额、币种、账户、原事实或业务结果冲突。
- posting plan 不平衡，或逆向只能重新计算当前规则而不能沿原事实。
- 旧文档、当前代码、跨项目材料之间发生冲突，且 Owner 尚未裁决。
- 需要 Git、联网、外部账号、生产数据或不可逆操作但未获授权。

## 3. 权威来源与证据协议

### 3.1 目标态事实优先级

1. 用户在本 Change 中明确确认的目标、非目标、红线和公共契约决策。
2. 本 Change 逐轮批准的新产品、DSL、系分和 TDD 正文，各自只对本层语义负责。
3. 当前源码、测试、构建、schema 和运行证据，证明现状与可行性。
4. `capte-domain`、`fincone`、`fincone-issuing` 等接入项目的当前源码、测试、依赖和宿主证据。
5. 旧产品/DSL/系分/TDD、历史 OpenSpec、归档预研和外部资料，只作为参考、反例或待验证候选。

来源冲突时不按文件名、修改时间或篇幅自动裁决；记录为 `conflict` 并交给对应 Owner。

### 3.2 证据等级

| Level | 含义 | 可支持的结论 |
| --- | --- | --- |
| `E0` | 想法、模型建议或无锚点转述 | 只能形成问题或候选。 |
| `E1` | 当前设计文档、合同/规则草案、历史决策 | 可支持讨论，不能证明实现或集成。 |
| `E2` | 当前源码、schema、静态调用和测试源码 | 可证明当前结构和预期行为，不能证明新鲜执行通过。 |
| `E3` | 当前 revision 的编译、契约/服务/H2 测试 | 可证明仓内实现切片。 |
| `E4` | Provider source -> built binary -> Consumer resolved/loaded artifact 谱系，加 real Bean/联合 schema 测试 | 可证明指定宿主的集成切片。 |
| `E5` | 目标环境部署、真实回执、监控、对账和运行证据 | 只证明对应环境和时间窗，不自动外推。 |

所有结论必须标记为 `事实`、`推断`、`待确认` 或 `范围外不做`。

### 3.3 最新模板基线

本次新文档采用 2026-08-12 当前安装版本，开始每个正式文档前重新读取并记录 hash；模板变化时先做差异评估，不静默套用旧结构。

| 文档 | 模板/规则 | 2026-08-12 SHA-256 |
| --- | --- | --- |
| 产品设计 | `product-prd-template.md` | `1df94436f6d284944223e7294343145d0255bfb0ee05b32a9f5f7d3c38baad59` |
| 产品写作 | `product-design-and-prd.md` | `64220889e2ccd0b869021187fe0d3618217e1a1551fddc9e60acb50833e7ef04` |
| 产品门禁 | `product-prd-quality-gates.md` | `b8c839625bbdad563e18e673322e8df75801397df870c36be33ff427a8850785` |
| DSL 规格治理 | `spec-template-practices.md`；当前安装 Skill 无独立 DSL 模板 | `71b8064cca513f834d64568aafb4e472d7684ac2edc6f5ab1a7bbda59f6b5c67` |
| 系分设计 | `system-analysis-template.md` | `b44c5983a16f280ad0c629789f1855d580e575b29694e8a612a1017c40f34e64` |
| TDD/测试 | `testing.md` | `946f86778d6dac84c9a9846ae22db7f59cdd85477f7cf9bb28260e3e4e7507b3` |
| 测试选型 | `testing-practices.md` | `a07c834c416bd3d4363761489349267e78a8c9fa5eb79580052a2c69c96f10ca` |
| 决策台账 | `question-ledger.md` | `beee5f408030a80173639a3aba5093265095a7ae57fadd426a03ec46e8a50d62` |

模块合议采用 `skills` 项目当前源码协议，不使用小说家能力或将模块拟人化。当前可复现基线为 `/Users/wuxp/Workspace/idea/github/skills` HEAD `c1c70006e64973956c1e7b0e1a9fe7a6888c80be` 加两份协议文件的 dirty diff；该 diff 的 SHA-256 为 `0e0108eaca3ed051c7f24ca89065d011be065a63b75827d29b7dcfb21b69d47a`，当前 `wise-agent/SKILL.md` SHA-256 为 `1bd2bcf06d552b11a48457f395eca84565826948a509b97cffc8164279fa9371`，当前 `wise-agent/references/context-handoff.md` SHA-256 为 `528187913d9fa29f117e5762a3b9ffb03ce0981ed891d0df5e96a95f013fbd32`。本 Change 消费的是该 `HEAD + dirty diff fingerprint + current file hashes` 组合，不把它表述为 clean HEAD；任一协议、基线 revision 或 fingerprint 变化时，未完成的会商材料标记为 `stale`，先做差异评估再继续。

## 4. 当前一手事实基线

### 4.1 wind-funds Provider

事实：

- 本节依据 2026-08-12 当前工作树，工作树在本 Change 前已有并行修改；本轮未把这些修改纳入交付或提交。
- 当前 public 顶层类型扫描为：`core=107`、`ledger-face=11`、`wallet-face=104`、`transaction-face=63`、`reconciliation-face=124`。
- `core` 同时包含账户/金额/账本不变量、Spend Rule/Payment Instrument/Benefit 等场景语义、跨模块端口，以及 transaction 内部编排 SPI。
- 当前 `99 stable / 4 experimental / 4 internal` API policy 只证明签名被历史门禁管理，不能证明语义合理或类型应继续位于 `core`。
- 当前最可信的稳定内核候选包括账户标识、资金主体、Money/币种、余额桶、账本分录、posting plan 平衡、原交易引用和 route/replay 证据。
- `FundsInstructionSpec` 是宽接口，包含大量 nullable default 和 `Map<String, Object>`；`SettlementPolicySpec` 含 parser、日历算法和全局可变状态；二者都必须重新证明边界。
- 当前源码和测试能提供充值/换汇、幂等冲突、共享卡授权、控制预留失败、收单清结算等场景证据，但本轮尚未执行新鲜测试。

关键源码入口：

- `core/src/main/java/com/wind/funds`
- `ledger/face/src/main/java/com/wind/funds/ledger`
- `wallet/face/src/main/java/com/wind/funds/wallet`
- `transaction/face/src/main/java/com/wind/funds/transaction`
- `reconciliation/face/src/main/java/com/wind/funds/reconciliation`
- `tests/src/test/java/com/wind/funds`

### 4.2 capte-domain Consumer

事实：

- 生产模块当前直接依赖 `wind-funds-core`、`wind-funds-transaction-face`、`wind-funds-wallet-face:1.0.1-SNAPSHOT`；集中 `tests` 模块另以 test scope 依赖 `ledger-impl`、`wallet-impl`、`transaction-impl`。生产模块没有 `ledger-face`、`reconciliation-face` 或 `*-impl` 依赖。
- 商品订单真实调用授权、完成、撤销、退款和交易查询；Benefit 真实调用 settle/refund。
- Benefit Consumer 与本地 Snapshot API 已出现漂移：新请求需要成本账户、接收账户和接收账目，Consumer 仍保留旧 `SubjectRef` 痕迹且未完整设置新字段。
- Consumer 没有持久化 Benefit settle 返回的资金交易号；退款会依赖业务键重查并读取当前活动配置，存在原事实漂移风险。
- 当前 `OrderCouponRedemptionIntegrationTests` 已手工 `@Import` 多个真实 transaction/ledger/wallet 实现，并与 Consumer mapper 共用 H2；`FundsProviderHostSchemaTests` 明确检查八张 Provider aggregate 表。但钱包腿仍注入 `RecordingInternalAccountPaymentParticipant`，手工测试装配也不等于生产 Bean/Proxy 装配，故只能作为 L3 候选证据。
- `ledger`、`reconciliation`、佣金、提现和 VCC 资金生命周期当前没有直接运行时消费证据。
- 历史基线曾记录 Capte revision `6de70922f2565ce1f7ff036d457c29f22cdab829` 及两个已加载 Snapshot JAR hash；该 Snapshot manifest/pom 未记录 Provider source revision，且与当时 Provider/Consumer API 存在漂移，只能作为 provenance，不能作为当前 L3 证据。
- 当前测试的 `assertCurrentProviderArtifactLoaded()` 仅在系统属性 `wind-funds.version` 非空时比较三个 impl class 的 `CodeSource` 与本地 Maven JAR 路径；属性缺失会直接跳过，且未比较 JAR SHA-256。W4-02 必须把 source -> binary -> resolved -> loaded hash 改成不可跳过的执行前门禁。
- 历史隔离 reactor 编译、54 个旧测试源码错误、Recording query 漂移和类加载探针结果均不自动沿用为当前结论；W4-02 只记录执行计划，fresh 结果由后续获准的 Consumer Host 执行轮次产生。

关键外部证据入口：

- `/Users/wuxp/Workspace/idea/capte/capte-domain/order/order-impl`
- `/Users/wuxp/Workspace/idea/capte/capte-domain/marketing/coupon-impl`
- `/Users/wuxp/Workspace/idea/capte/capte-domain/tests/src/test/resources/jdbc-schema.sql`

### 4.3 fincone Design Consumer

事实：

- `fincone` 是 docs-first 设计与准出仓，没有 Maven/Gradle/Java/Kotlin/SQL 运行时代码，不能证明已集成 `wind-funds`。
- 其逻辑能力名和 face 引用只可作为业务语义与接入候选，不是新增 public API 的授权。
- VCC 运行时目标属于 `fincone-issuing`，订单/Benefit 运行时目标属于 `capte-domain`；`fincone` 不应新增运行时 adapter。
- 全球账户当前仍是设计级证据；Submitted/Accepted/Processing 不等于资金可用，confirmed credit、return/reversal 和对账差异可作为目标场景候选。
- 宿主集成必须证明 artifact 谱系、real Bean、联合 schema、事务管理器和 JDK proxy 启动；Provider 单仓测试不能代替。

关键外部证据入口：

- `/Users/wuxp/Workspace/idea/capte/fincone/AGENTS.md`
- `/Users/wuxp/Workspace/idea/capte/fincone/docs/生产交付/资金内核`
- `/Users/wuxp/Workspace/idea/capte/fincone/docs/生产交付/全球账户与通道`
- `/Users/wuxp/Workspace/idea/capte/fincone/docs/生产交付/清结算`

## 5. 工作角色与责任

角色只提供专项证据和挑战，不成为平级 Owner，也不通过投票形成公共契约。

| 角色 | 责任 | 本轮输出 | 不能替代 |
| --- | --- | --- | --- |
| 人类 Owner | 确认产品定位、公共契约、风险接受和未决取舍 | 一次确认一个 `ask-owner` 决策 | 源码事实、测试和专业审批 |
| 资金业务专家 | 说明谁的钱、因何变化、完成层级、正逆异常和资金责任 | 主体/资金流/不变量/停止条件 | 法务、财务、会计和合规最终确认 |
| 产品主笔 | 用最新版增强 PRD 模板组织目标、场景、对象、规则、边界和验收摘要 | 正式产品设计候选 | 系分接口和实现字段 |
| Consumer 代表 | 证明真实业务入口、调用、数据、事务、DI、schema 和版本约束 | `capte-domain`、`fincone`/目标宿主证据卡 | Provider 的目标设计裁决 |
| DSL 设计者 | 从已确认产品事实提炼稳定词汇、结构、关系、不变量和反例 | DSL 候选、JSON/Java 中立契约 | 为单个场景发明 Core |
| 系统架构师 | 映射模块职责、接口、数据、一致性、破坏式切换和 Consumer 同步迁移 | 系分候选和接口清册 | 产品 Owner 的业务取舍 |
| Wind 约规 Reviewer | 检查 face/impl、Entity 不外露、币种、空值、服务分层和浅抽象 | 规则偏差与最小整改建议 | 源码级设计和业务语义 |
| TDD Owner | 把场景与契约转成 Red、断言和验证层级 | TDD 矩阵、命令和证据缺口 | Maker 自证准出 |
| Independent Checker | 读取原始产物与证据，挑战语义、边界和可验证性 | findings、准出/退回及残余风险 | 目标定义和发布批准 |

## 6. 目标文档集与事实边界

下列路径是逐轮确认后的目标位置，本 Round 不创建空壳正文：

| 层级 | 目标路径 | 唯一负责 | 不得包含 |
| --- | --- | --- | --- |
| 产品设计 | `docs/产品设计/支付资金公共能力层-产品设计.md` | 产品目标、主体、能力、对象、流程、规则、产品接口抽象、风险和验收摘要 | Java 类、数据库字段、事务实现和测试任务流水 |
| DSL 设计 | `docs/DSL设计/支付资金公共能力层-DSL设计.md` | 稳定资金语言、事实层级、引用、状态、不变量、序列化和负例 | 场景私有对象、宿主流程、Mapper/Entity 和实现算法 |
| 系分设计 | `docs/系分设计/支付资金公共能力层-系分设计.md` | 模块、public/internal 契约、数据、状态、事务、一致性、破坏式切换、Consumer 同步迁移和风险 | 未经产品确认的新业务语义和详细测试流水 |
| TDD 设计 | `docs/TDD设计/支付资金公共能力层-TDD设计.md` | 场景、Red、断言、AC 映射、验证命令、跨仓层级和 Checker | 重新定义产品目标、DSL 字段或系统接口 |
| 过程与执行 | 本文件 | 项目执行规范、角色证据、决策台账、Wave、任务、详细验收映射和状态 | 冒充正式设计结论 |

某层未通过对应门禁时，下一层只能做只读取证，不能形成正式目标态设计。

## 7. 逐轮讨论协议

每轮只关闭一个会改变方向、范围、公共契约或风险的主决策：

1. `察实`：回读本文件、上一轮已确认结论、源码/测试/跨仓证据和模板 hash。
2. `正名`：把问题写成“对象 + 待判断取舍 + 作用范围”的唯一命题。
3. `分角色取证`：资金、产品、Consumer、架构和 TDD 只提交各自证据、反例与待确认。
4. `业务模拟`：至少走一条正向、一条逆向或异常路径，明确四流、状态和完成证据。
5. `挑战`：检查是否有场景泄漏、万能接口、重复事实、当前规则重算、幂等冲突或失败副作用。
6. `裁决`：Facts 用证据自答；低风险可逆项可 `self-decided`；公共契约和高风险取舍只问 Owner 一个问题。
7. `留痕`：更新同一问题记录的裁决动作、最终结论、red_lines、影响和下一阶段输入。
8. `成文`：只把已确认结论写入对应正式文档；讨论过程和被拒方案留在本文件。
9. `验`：Maker 自检后交独立 Checker；未通过则退回当前轮，不跨层补写。

连续两轮没有新证据或新决策时停止扩问，列出剩余 `PENDING` 和下一 Owner。

每个 Wave 首轮最多发起 3 个 Owner 决策；超过后暂停当前 Wave，将未决项按“阻断 / 可延后 / 范围外”拆分并重新确认范围，不以压缩问题或扩大公共抽象绕过上限。

### 7.1 原子轮次执行动作规范

每个原子轮次都按下列动作执行；缺少任一承重输入时停在当前动作，不以会议纪要、Agent 摘要或工具输出补齐：

1. `A0 / 恢复对账`：先回读本 Change 的 `state_revision / Plan state / current task / execution_basis`，再回读已接受 Decision、权威文档 revision、dirty worktree、允许写入范围和验证入口。runtime Goal 的任何状态都只作历史 provenance，不影响切片准入；若旧材料仍把它写成当前执行状态，先以本文件为唯一持久状态并消除双权威。
2. `A1 / 冻结任务`：写明唯一 Task/Decision ID、目标、非目标、输入 revision、预期产物、Owner、Checker、预算和停止条件。一次只承载一个公共取舍或一个可独立验证的工程切片。
3. `A2 / 一手取证`：产品、模块、Consumer、Provider 和外部 Owner 只提交自己拥有的事实、证据、约束和未知项；旧设计和业内资料只能作为候选。
4. `A3 / 信息门禁`：逐项确认事实版本已收到并理解；冲突、缺失和过期项绑定 Owner、所需证据、是否阻断及失效条件。门禁未通过时不得交换方案或作结论。
5. `A4 / 合议或定契`：单模块直接设计；两个模块或两个项目优先双边定契；只有三个以上独立权威必须裁决同一不可拆问题时才召开主持式多方会商。
6. `A5 / 场景验证`：至少选择一条主路径和一条逆向、异常或 `UNKNOWN` 路径；资金影响场景断言余额、posting、ledger、引用和幂等，无资金/只读场景断言零副作用。
7. `A6 / 裁决与分流`：事实问题由证据关闭；低风险可逆工程项按既有约规选择最小方案；产品价值、公共契约、高风险责任和外部规则交对应 Human Owner。结论只允许 `accepted / rejected / pending`，其中 `pending` 必须有 Owner、阻断行为、fail-closed 和恢复条件。
8. `A7 / 权威回写`：产品结论写 PRD，DSL 结论写 DSL 正文，工程结论写系分/ADR，详细动作、证据版本和状态只写本执行规格；不复制第二套真相源。
9. `A8 / 独立验证`：运行本层 validator、diff 检查、场景/契约测试或集成证据；Checker 必须回读原始产物和证据，不只读取 Maker 摘要。
10. `A9 / 交接或停止`：门禁通过才生成下一层 Context/Handoff；未通过则回到最早失败动作。未获当前 `Refactoring Slice Card` 对应的写入授权时，即使计划完整也不得进入生产代码、Git、发布或生产动作。

每轮完成记录至少包含：输入 revision、实际动作、产物锚点、验证命令与结果、Owner 决策、残余风险、下一原子动作和失效条件。计划、讨论、Checker 预审和文档结构通过均不是实现完成。

### 7.2 模块合议执行规范

项目内模块合议的简短入口为 `$wise-agent 模块合议：<项目或边界议题>`。本 Change 在 `core`、`ledger`、`wallet`、`transaction`、`reconciliation` 出现共享责任、依赖或公共契约取舍时使用；模块只是事实权威，不是角色人格。`novelist` 只负责小说叙事，不参与本资金工程 Goal。

合议按契约风险触发，不按模块数量或 Gate 名称默认召开：`G1` 只处理跨 Owner 的产品责任/完成语义，`G2` 只处理 public DSL/Core 资格，`G3` 只处理 public/serialized/failure/破坏式切换契约，`G6` 只处理真实 Host 集成。`G4` 仅在 RED 跨仓、涉及联合 schema、DI/事务或 artifact 谱系时双边定契；`G5` 仅在 public/serialized contract、依赖方向或 Consumer 可观察行为变化时重开。单模块 private/internal 行为保持重构只需模块 Owner、最小测试和独立 Checker，不发起 Contract Inquiry。

模块合议按以下顺序落地：

1. 读取当前 PRD、DSL/系分、ADR、模块 POM、public API、生产消费者、源码、测试和必要运行证据，形成每个模块的 `Module Fact Card`。
2. 卡片至少记录模块 Owner 与 revision、业务价值、主定位、拥有的对象/数据/状态/规则、提供与消费能力、允许依赖、副作用、事务/失败/切换/发布责任、非目标、证据和未知项。
3. 每条真实依赖按“消费场景 -> 提供能力 -> 契约与版本 -> 依赖方向 -> 数据/状态归属 -> 副作用与失败 -> Consumer 同步切换 -> 验证与 Owner”对账。
4. 只有命中本节第 256 行所述 `G2/G3` 契约风险时，才按真实依赖把 `core <-> ledger`、`ledger <-> wallet`、`wallet <-> transaction`、`ledger <-> transaction`、`transaction <-> reconciliation`、`ledger <-> reconciliation` 分别双边定契；private/internal 行为保持变化不因跨模块就自动触发会商。只有一个共享概念同时改变三个以上模块且双边结论无法组合时，才以同一 information revision 进入主持式多方会商。
5. 信息充分性门禁必须同时满足：主题/术语/非目标一致；所有承重事实已理解或明确争议；阻断项已经关闭；参与模块和证据 revision 未过期。否则只补证，不进入 `Position Card` 或接口设计。
6. 确认结果写回既有产品、DSL、系分、ADR 或模块文档；会商过程只保留在本文件的 Decision/验证记录，不创建常驻会议文档、公共聊天室或第二个 Goal。

各 Wave 的模块合议点：

| Gate | 合议主题 | 默认参与权威 | 产物 / 停止线 |
| --- | --- | --- | --- |
| `G1` | 产品能力与事实 Owner，不讨论 Java 边界 | Product/Payment + 真实 Consumer/外部 Owner | 场景合同；责任或完成门槛不清则停止 DSL |
| `G2` | 稳定 DSL 是否应进入 `core`，以及 public/internal 资格 | `core` 分别与实际消费模块双边定契 | DSL 样例与类型处置候选；无两个真实场景不进 Core |
| `G3` | 对象、数据、状态、规则、依赖和公共接口归属 | 按上述真实依赖逐对定契 | Module Fact Card、接口清册、破坏式切换责任；冲突未裁决不写代码 |
| `G4` | 测试入口、证据层级、失败与恢复可验证性 | 仅命中跨仓 RED、联合 schema、DI/事务或 artifact 谱系时由提供模块与消费模块/宿主参与 | AC/Red 映射；Fake/Recording 不替代内部真实链路 |
| `G5` | 候选 diff 是否按同一切片完成目标契约与 Consumer 切换 | 仅命中 public/serialized、依赖方向或 Consumer 可观察变化时由实际变更模块、全部已知真实消费者和 Checker 参与 | scoped diff/CR；接口或依赖越界、任一 Consumer 未切换立即退回 G3 |
| `G6` | 实际加载制品、Bean/schema/事务和运行结果是否履约 | Provider + Consumer Host | Consumer Reconciliation；谱系或宿主证据不全不得宣称集成 |

### 7.3 跨项目会商执行规范

`wind-funds` 持有公共资金契约与 Provider 证据，`capte-domain` 持有订单/券/Benefit 的真实 Consumer 事实，`fincone` 持有 VCC、GlobalAccount、收单和清结算的 docs-first 产品事实；三者不得互改对方权威。跨项目会商使用短期 `Contract Inquiry -> Provider Evidence Response -> Consumer Reconciliation -> Checker`，不建立跨仓共享状态文件。

- `Contract Inquiry` 冻结 `inquiry_id`、topic/information revision、Consumer revision、Provider baseline revision、真实场景、待回答契约、双方写入边界、期望证据和失效条件。
- `Provider Evidence Response` 逐项返回 `supported / conditional / gap / out_of_scope`，并给出公共契约、副作用、破坏式变更边界和源码/测试/运行锚点；Provider 自述不能代替证据。
- `Consumer Reconciliation` 必须在原业务场景中独立验证接受版本，结论为 `confirmed / conflict / reopen / stale`；不凭接口能够编译就宣称业务闭合。
- 主题、信息、任一权威 revision、证据 fingerprint 或 loaded artifact 变化时，旧响应和旧结论失效；不得换 inquiry ID 规避重开。

跨项目会商按阶段触发：

| 时点 | 触发条件 | 会商方式 | 不能外推 |
| --- | --- | --- | --- |
| `G1` 产品场景 | 收款责任、Benefit 责任、VCC/ACH/收单 finality 或对账口径影响公共语义 | `capte-domain <-> wind-funds`、`fincone <-> wind-funds` 分别双边；外部/财务 Owner 只处理其责任项 | 设计材料不证明运行集成 |
| `G2` DSL 候选 | 某稳定概念需要被两个项目表达，或场景差异可能污染 Core | 向两个 Consumer 发同版本 Contract Inquiry，分别回收，不让 Consumer 互相定契 | 两个案例不自动等于通用平台 |
| `G3` 公共接口 | face 签名、查询证据、破坏式切换或失败语义会影响 Consumer | 每个真实 Consumer 独立验收；design-only Consumer 只能提供场景挑战 | docs-first 不能批准运行 API |
| `G4` TDD | 需要冻结 Consumer Red、联合 schema、DI/事务或 artifact 谱系 | Provider/Consumer 共同形成测试计划，Checker 复核 | Recording/Fake、裸 Snapshot 不等于 E4 |
| `G5` 每个目标态切片前后 | 变更 public contract、依赖方向、序列化或 Consumer 行为 | 切片前确认 Inquiry，切片内全部已知真实 Consumer 同步切换，切片后返回 Provider Evidence | 单仓 GREEN 不等于 Consumer 通过 |
| `G6` 宿主集成 | 真实 Bean、联合 schema/事务和 loaded binary 已可执行 | Consumer 执行 reconciliation；必要时加入真实 rail/adapter Owner | 指定宿主 E4 不外推生产 E5 |

三个以上项目仅在同一公共契约问题不可拆时进入主持式会商；例如同一 DSL 引用同时被 Capte、VCC 与 reconciliation 以互相冲突的语义消费。普通场景差异继续走两次双边会商。Issuer、ACH、PSP、银行、Finance/Legal/Accounting Owner 只在其权威规则阻断产品声明、自动资金动作或生产准出时加入，不常驻公共 DSL 讨论。

### 7.4 会商卡片与信息充分性门禁

知止者为“一主”，只维护本 Change 的计划状态、议题、版本、冲突路径和综合结论；模块与项目为“多权”，只维护自身事实；未参与主笔的 Checker 提供“独立证”。以下卡片只在具体议题中按需生成，保存必要事实指针和 revision，不预建全仓百科，也不是新的状态类型或真相源。

```text
Module / Project Fact Card:
module_or_project / authority_owner / authority_revision / worktree_fingerprint:
docs_first_revision: declared_document_version / design_or_admission_status / owner_decision / signed_scope / observed_at:
business_value / target_subject / primary_position / positioning_evidence:
owned_objects / data / state / rules / write_and_query_boundary:
provided_contracts / consumed_contracts / real_consumers / allowed_dependencies:
side_effects / transaction / failure / breaking_cutover / release_responsibility:
explicit_non_goals / red_lines:
Spec / ADR / build / API / source / test / schema / artifact / runtime_evidence:
facts / assumptions / unknowns / conflicts / decision_owner:
stale_or_reopen_conditions:
```

对 `fincone` 等 docs-first 权威，`authority_revision` 必须包含文档声明版本、`DesignStatus/AdmissionStatus`、`OwnerDecision`、已签收范围、内容 fingerprint 和观测时间；外部规则另带 source/version/scope/effective/verified date/evidenceRef。只有已由主签/会签 Owner 接受的条款可进入 `accepted_product_invariants`；`PENDING/BLOCKED` 只能作为候选、hard negative 或停止线，不能进入已接受 revision。

```text
Shared Information Matrix:
deliberation_id / topic_revision / information_revision:
topic / decision_questions / scope / non_goals / glossary / baseline_revisions:
information_item / type: fact | evidence | assumption | unknown | dependency
authority_ref / evidence_revision / evidence_fingerprint:
participant_status: received | understood | disputed | missing
gap / owner / required_evidence / blocks_current_decision / stop / stale_condition:
Information Readiness Gate: ready | blocked | stale
```

`Information Readiness Gate=ready` 必须同时满足：主题、术语、非目标和基线 revision 一致；每个决策问题有证据或显式 unknown；必要方至少标记 `understood` 或说明 `disputed`；不存在未关闭的 `blocks_current_decision=true`。仅已收到材料、已有 Owner、开过会或主持者认为足够都不能准出。门禁 `blocked/stale` 时只补信息，不交换方案偏好、不写 Position、不承诺契约。

```text
Contract Inquiry:
inquiry_id / change_id / optional_runtime_goal_ref / topic_revision / information_revision:
consumer_authority_ref / consumer_revision:
provider_authority_ref / provider_baseline_revision:
real_scenario / accepted_product_invariants / contract_questions:
expected_evidence / write_boundaries / stop / stale_conditions:

Provider Evidence Response:
inquiry_id / topic_revision / information_revision / consumer_revision:
provider_baseline_revision / provider_revision / response_revision:
evidence_fingerprint / supersedes:
item_result: supported | conditional | gap | out_of_scope
contract / side_effects / failure / breaking_cutover:
source / test / Spec / artifact / runtime_evidence:
owner_gate / remaining_gap / stale_conditions:

Consumer Reconciliation:
inquiry_id / accepted_topic_revision / accepted_information_revision:
accepted_consumer_revision / accepted_provider_revision:
accepted_response_revision / accepted_evidence_fingerprint:
scenario_acceptance / integration_evidence:
result: confirmed | conflict | reopen | stale
remaining_gap / owner / next_action:
```

同一接受版本的重试不得生成第二份裁决。主题、information、Consumer、Provider、artifact、schema 或运行证据 revision 变化时，旧卡标记 `stale`，只重开受影响议题。Checker 回读卡片指向的原始材料、接受版本和真实场景，检查事实越权、能力偷渡、版本错配和未绑定 Owner 的冲突。

## 8. Wave Plan

计划识别摘要：所属阶段依次为 Wave 0 至 Wave 6；原子任务以 `W<Wave>-<序号>` 唯一标识。当前写入文件为本规格和已获准创建的 `W1-01` 产品正文；DSL、系分、TDD 等后继正文只在上游 Gate 通过后创建。只读参考包括当前源码、测试、schema、旧设计和跨项目证据。

### Wave 0：事实、模板与问题地图

#### Task ID `W0-01`：现状证据清册

- Owner：Plan steward；Consumer 代表提供只读证据。
- 写入范围：仅本文件的事实基线、证据等级和待确认项。
- 只读范围：三个项目的规则、POM、源码、测试、schema、旧设计和当前 Git 状态。
- 依赖关系：无。
- 完成条件：Provider、真实 Consumer、design-only Consumer 分层；所有结论标明证据等级。
- 验证命令：链接/路径回读、`git diff --check`、独立 Checker 走读。
- 停止条件：来源冲突或读取权限不足时标 `conflict/PENDING`，不猜结论。
- 交接：形成 Wave 1 的 Product Context 输入。

#### Task ID `W0-02`：public API 清册规则

- Owner：系统架构师；Wind Reviewer 协同。
- 写入范围：后续在本文件追加 API 审查结果，不修改 API baseline 或生产源码。
- 只读范围：`core` 与四个 face、生产/测试调用、跨仓依赖和本地 Maven artifact。
- 依赖关系：`W0-01`。
- 完成条件：每个 public 类型可归入 `保留 / 移动 / 内收 / 替换 / 退役 / 待确认`，并有消费者和不变量证据。
- 验证命令：全仓/跨仓 `rg`、依赖树、字节码和契约测试仅在对应轮次执行。
- 停止条件：不能用“已有 API”或“baseline stable”替代语义证据。
- 交接：为 DSL 和系分提供现状面，不预先决定目标面。

### Wave 1：产品设计

#### Task ID `W1-01`：公共能力层产品定义

- Owner：人类 Owner；产品主笔负责候选，资金业务专家和 Consumer 代表提供证据。
- 写入范围：本文件的决策记录；确认后创建产品设计正文的背景、目标、定性与范围。
- 只读范围：Round 0 证据、旧 PRD、业务流程和支付资金专业 reference。
- 依赖关系：`W0-01`。
- 验收场景：订单钱包支付、Benefit 出资、VCC 授权、全球账户 confirmed credit、收单 capture 到 payout。
- 完成条件：明确产品是谁、服务谁、拥有何种事实、不拥有何种产品/外部协议生命周期。
- 验证命令：`check_product_deliverable.py --kind prd` 在产品正文完整后执行；本轮先人工场景走读。
- 停止条件：主体、资金归属或责任 Owner 不清时不进入 DSL。
- 交接：Product Context Card 只提取已确认事实。

#### Task ID `W1-02`：产品对象、流程、规则与验收摘要

- Owner：产品主笔；资金业务专家挑战；人类 Owner 确认公共取舍。
- 写入范围：产品设计正文。
- 只读范围：Consumer 场景、当前实现行为和外部规则来源索引。
- 依赖关系：`W1-01` 已确认。
- 完成条件：P0/P1 场景闭合主路径、逆向、异常、人工接管、可观察结果和红线；不写 Java/DDL。
- 验证命令：产品 validator、外部规则结构检查、独立产品 Checker、业务/研发/测试三方走读。
- 停止条件：任何外部规则缺来源/版本/适用范围时保持 `PENDING`。
- 交接：通过后产品设计成为 DSL 的唯一业务语义输入。

### Wave 2：DSL 与 Core

#### Task ID `W2-01`：最小稳定词汇与事实层级

- Owner：DSL 设计者；资金业务专家、架构师和 Consumer 代表共同挑战。
- 写入范围：确认后创建 DSL 正文；本文件更新决策台账。
- 只读范围：批准的产品设计、当前 core、跨仓请求/结果和测试场景。
- 依赖关系：Wave 1 通过。
- 候选起点：主体、账户标识、Money/币种、业务事实引用、原资金事实引用、账目目标、外部证据引用、幂等身份与摘要、时间。
- 完成条件：每个核心概念说明是什么、不是什么、Owner、不变量、序列化和至少两个真实场景；无证据概念不进入 Core。
- 验证命令：JSON 正反例、类型依赖检查、场景走读和 API 清册对照。
- 停止条件：需要宽 `Map` 或场景枚举才能表达关键事实时退回产品/DSL 重新定性。
- 交接：形成 Core Candidate，不等于 Java API 批准。

#### Task ID `W2-02`：指令、事实、路由、账务与逆向边界

- Owner：DSL 设计者；ledger/transaction 架构 Owner 协同。
- 写入范围：DSL 正文。
- 只读范围：当前 `FundsInstructionSpec`、route、posting、ledger、replay、projection 契约和测试。
- 依赖关系：`W2-01`。
- 完成条件：区分业务意图、执行指令、资金交易事实、route snapshot、posting plan、ledger fact、balance projection、reconciliation evidence；明确哪些 public、哪些 internal。
- 验证命令：支付/退款、授权/部分完成/释放、外部入金/return、清结算/对账四组契约样例。
- 停止条件：不得用一个万能 instruction 或一个总状态机承载所有完成层级。
- 交接：通过后进入系分，不直接修改 `core`。

### Wave 3：系分与公共接口

#### Task ID `W3-01`：`core` 与 `ledger` 边界

- Owner：系统架构师；Ledger Owner 与 Checker 独立复核。
- 写入范围：系分正文的 Core/Ledger、接口和破坏式切换章节。
- 只读范围：批准的产品/DSL、当前 ledger face/impl、消费者和账本测试。
- 依赖关系：Wave 2 通过。
- 完成条件：确认账本事实、posting 写边界、查询边界、余额投影、账目/周期、错误和幂等；公开写接口必须有调用方和职责证据。
- 验证命令：架构 validator、接口使用样例、posting 平衡/重复过账/冲正测试设计。
- 停止条件：Consumer 不得拼接或直写内部 posting primitive；DI 问题不得通过扩 public face 修复。
- 交接：更新 public API 处置清册。

#### Task ID `W3-02`：`wallet` 边界

- Owner：系统架构师；Wallet/Consumer 代表协同。
- 写入范围：系分正文的 Wallet、接口和破坏式切换章节。
- 只读范围：批准的产品/DSL、当前 wallet face/impl、capte 授权链和 VCC 场景证据。
- 依赖关系：`W3-01` 的账户/账本边界已确认。
- 完成条件：确认账户身份/状态、资金责任、支付工具、Spend Control/Rule 与余额查询的归属；钱包不创建交易或账本事实。
- 验证命令：账户能力、责任解析、共享卡、规则漂移和失败零副作用测试设计。
- 停止条件：支付工具、券、订单等业务对象不得直接成为资金/账本主体；payer、payee、merchant 等责任主体只有经租户、责任和账户映射确认后，才可成为 `FundsSubject`。
- 交接：更新 public API 处置清册。

#### Task ID `W3-03`：`transaction` 边界

- Owner：系统架构师；Transaction、Ledger、Wallet Owner 会审。
- 写入范围：系分正文的 Transaction、route、接口和破坏式切换章节。
- 只读范围：批准的产品/DSL、当前 transaction face/impl、capte 真实调用与资金流程测试。
- 依赖关系：`W3-01`、`W3-02`。
- 完成条件：确认 use-case facade、直接交易、授权、余额控制、外部资金事件、原路径逆向、route snapshot 和查询证据；场景专用服务须证明独立业务语义。
- 验证命令：同键重放/冲突、部分退款、授权累计、失败回滚、route replay 测试设计。
- 停止条件：宿主编排不进入 Core；关键收款/成本/账目事实不得藏在 context。
- 交接：更新 public API 处置清册和 Consumer 迁移影响。

#### Task ID `W3-04`：`reconciliation` 边界

- Owner：系统架构师；Reconciliation、财务/运营确认方和 Checker 参与。
- 写入范围：系分正文的清分、清算、结算、出款、对账、差错和 Gate 章节。
- 只读范围：批准的产品/DSL、当前 reconciliation face/impl、收单流程、fincone 设计和外部事实候选。
- 依赖关系：`W3-03` 资金事实边界已确认。
- 完成条件：区分 source fact、candidate、batch、result snapshot、Gate、settlement、payout、recovery 和 difference；只读证据与资金动作分离。
- 验证命令：架构 validator、状态机走读、重跑/差错/放行/失败无副作用测试设计。
- 停止条件：外部事实源、日期口径、规则 Owner 未确认时不扩出大而全 public API；对账不得反写历史资金事实。
- 交接：系分完整后交独立架构 Checker。

### Wave 4：TDD 与跨项目模拟

#### Task ID `W4-01`：TDD 场景与 Red 设计

- Owner：TDD Owner；各域 Maker 只提供可测入口，Checker 独立。
- 写入范围：TDD 正文和本文件的详细 AC/验证映射。
- 只读范围：批准的产品、DSL、系分、当前测试资产和跨仓源代码。
- 依赖关系：Wave 3 通过。
- 完成条件：每个 P0/P1 场景有正常、边界、逆向、异常、并发/重复和人工停止；资金变化断言满足项目红线。
- 验证命令：文档阶段运行链接/结构/一致性检查；实施阶段才运行 `just verify-slice`、模块测试、`just pmd` 和 `just verify-cad`。
- 停止条件：Recording/Fake 不得作为内部资金链最终证据；无真实入口的 demo 不进入计划。
- 交接：形成实现前 Red 清单，不授予代码执行。

#### Task ID `W4-02`：跨仓 L3/L4 验证计划

- Owner：Consumer Host Owner；Provider Owner 协同；独立 Checker 准出。
- 写入范围：TDD 正文的集成层级和本文件的 artifact 谱系卡。
- 只读范围：Provider/Consumer 构建、POM、schema、DI、事务和目标宿主。
- 依赖关系：`W4-01`。
- 完成条件：冻结 Provider commit/dirty fingerprint、JAR hash、Consumer resolved dependency 和运行时 loaded origin；real Bean + 联合 H2/目标数据库测试可执行。
- 验证命令：由对应宿主计划给出；裸 Snapshot 缓存命中不得准出。
- 停止条件：schema、事务管理器、JDK proxy、Mapper/Bean 装配或版本谱系不清即停止。
- 交接：只有通过后，才可把 Consumer 缺口归因为公共能力缺口。

### Wave 5：实现与迁移候选

#### Task ID `W5-01`：最小迁移切片

- Owner：系统架构师拆分；人类 Owner 单独授权实现。
- 写入范围：由本切片 `Refactoring Slice Card` 的白名单单独确定；本规格不授权源码修改。
- 只读范围：本切片依赖的 accepted decisions/AC、API 处置清册、Consumer 影响、dirty overlap 和验证矩阵。
- 依赖关系：本切片判定为 `eligible`。改变产品/public/serialized/Consumer 可观察语义的切片必须取得其所依赖的 G1-G4 结论；清册、保护网和 private/internal 行为保持切片不等待无关 Wave/PENDING。
- 完成条件：每个切片可独立理解、TDD、验证、回滚；优先修已消费契约，再内收/退役已证明无消费者的抽象，最后才考虑新增能力。
- 验证命令：按项目 `Justfile` 和宿主命令冻结在单任务 Execution Grant 中。
- 停止条件：公共契约、Git、数据库迁移、发布或生产动作缺少单独授权。
- 交接：Implementation Spec / Engineering Handoff；不在设计轮提前生成。

每个切片的固定顺序为：判定 `slice eligibility` -> 冻结 accepted decisions/AC、各方 revision 与既有 dirty fingerprint -> 搜索生产/测试/跨仓消费者 -> 对 public/serialized contract 或 Consumer 可观察变化确认当前有效的 `Contract Inquiry` 与接受 revisions -> 写最小 RED 并证明因目标缺口失败 -> 在同一垂直切片内同时修改 Provider 与全部已知真实 Consumer，直接切换到目标契约并删除被替换入口 -> focused Green -> 模块/架构/API/规约验证 -> Provider Evidence Response -> Consumer Reconciliation 与 E4 -> 独立源码 CR -> 只回写本切片 delta。纯 private/internal 且行为保持的切片可以把 Inquiry 标为 `NOT_APPLICABLE`，但仍需模块 Owner 与独立 Checker。Inquiry 缺失或过期、任何真实 Consumer 无法同批修改、RED 无法稳定复现、失败原因不是目标缺口、同一切片需要跨越未决产品语义、accepted revision 变化或无法隔离既有 dirty overlap 时立即停止。

```text
Refactoring Slice Card:
slice_id / plan_revision / slice_status: eligible | blocked | stale | verified
slice_kind: inventory | protection | behavior_preserving_internal | public_contract | host_integration
execution_basis / dependent_decisions / AC / accepted_revisions:
blocking_decisions / excluded_fail_closed / host_E4_blockers / external_E5_blockers:
consumer / provider / owner / checker:
inquiry_id / inquiry_status / inquiry_topic_revision / inquiry_information_revision:
inquiry_consumer_revision / inquiry_provider_baseline_revision / inquiry_stale_conditions:
write_whitelist / read_only_scope / forbidden_scope:
baseline_HEAD / preexisting_dirty_fingerprint / overlap_files / overlap_disposition:
RED_name / expected_failure / evidence:
minimal_production_change / breaking_change_scope / consumer_cutover_strategy:
focused / module / boundary / API / PMD / compile_verification:
Provider_Evidence / Consumer_Reconciliation_required:
rollback / stop / stale_conditions:
authorization / next_slice_dependency / next_entry:
```

切片准入不使用全局“一律关闭全部 PENDING”。`eligible` 必须证明当前切片依赖的产品语义、公共契约、AC、Consumer/Provider revision 和验证入口已经确认；未被本切片编码的外部 finality、`RETURNED`、rail matrix 等可以标记为 `excluded_fail_closed`，其 hard negative 必须是零自动资金效果、零终局声明或人工停止。直接改变责任、金额、原事实、重放、部分效果续作、public/serialized contract 或 Consumer 可观察行为的 PENDING 必须进入 `blocking_decisions`。`host_E4_blockers` 和 `external_E5_blockers` 分别只阻断宿主集成与外部/生产声明，不得倒逼无关内部切片假关闭。

当前 worktree 的既有修改覆盖 `core`、API baseline、`ledger`、`wallet`、`transaction`、`reconciliation` 与中央测试。任何代码切片开始前必须冻结目标文件的 `HEAD + preexisting dirty diff fingerprint`；命中 `overlap_files` 时只能选择避开、在获准隔离副本验证后人工合并，或停止交还 Owner，不能覆盖、格式化、暂存或回退用户改动。`RS-001` 只产清册和切片候选，不编辑这些重叠文件。

#### Task ID `W5-02`：随行 Consumer 同步切换

- Owner：系统架构师拆分；全部已知真实 Consumer Host Owner 验收；人类 Owner 单独授权破坏式切换。
- 写入范围：一个 Provider 契约及全部已知真实 Consumer 的明确白名单。
- 只读范围：accepted 产品/DSL/系分/TDD、public API 清册、artifact 谱系和直接/传递消费者。
- 依赖关系：与对应 `W5-01` 组成同一个垂直切片；Provider 候选通过本仓 focused 验证且 Consumer Contract Inquiry 未过期后执行，不等待其他 Provider 模块批次。
- 完成条件：Provider 与全部已知真实 Consumer 在同一切片只使用目标契约；旧接口、桥接、转发重载、别名、双读、双写、兼容窗口和延迟退役均不存在；Consumer production/test compile、accepted artifact 解析/类加载证据和整切片回滚点齐全。类加载只证明目标 artifact 被解析，不证明 real Bean/schema/事务或业务 E4。
- 验证命令：Provider focused/module/boundary/API 检查 + Consumer production/test compile + accepted artifact origin/hash；E4 仍留 Wave 6。
- 停止条件：不得先删除 public API 再寻找 Consumer；任一已知 Consumer 不能同批切换、未知运行 Consumer 无法排除、数据无法一次迁移或整切片不可回滚时停止，不增加兼容层绕过。
- 交接：Consumer 同步切换与回滚证据闭合后进入 Wave 6 的 E4；E4 未通过则整切片不准出。

### Wave 6：宿主集成与准出

#### Task ID `W6-01`：真实 Consumer / Provider 集成对账

- Owner：Consumer Host Owner；Provider Owner 协同；双方 Checker 独立准出。
- 写入范围：由单独 Integration/Execution Grant 确认；本规格只定义证据链，不授权宿主修改。
- 只读范围：Provider source/dirty fingerprint、built artifact、Consumer POM/锁定依赖、loaded classes、Bean/proxy、联合 schema/事务和业务记录。
- 依赖关系：对应 Provider/Consumer 迁移切片已通过仓内验证。
- 完成条件：形成 `source -> built binary/hash -> resolved dependency -> actually loaded artifact -> real Bean -> joint schema/transaction -> SIM business assertions` 的完整证据链与 Consumer Reconciliation。
- 验证命令：由目标宿主的 TDD/集成计划冻结；分别记录 production compile、test compile、class loading、Spring assembly 和真实业务流程结果。
- 停止条件：裸 `SNAPSHOT`、旧 Surefire、单次 `BUILD SUCCESS`、Recording/Fake、类加载探针或 Provider 单仓 GREEN 均不得单独支撑 E4。
- 交接：E4 只证明指定宿主集成切片；enable、release、production 和真实 rail/finality 仍需独立 Gate/E5。

#### Task ID `W6-02`：目标态清零复验

- Owner：Provider Owner 与全部已知真实 Consumer Host Owner；人类 Owner 授权本破坏式切片；独立 Checker 准出。
- 写入范围：只验证并收口同一垂直切片已经删除的旧 public contract、引用和基线，不创建独立延后退役批次。
- 只读范围：全仓/跨仓源码、测试、字节码/反射/序列化入口、accepted artifact、Consumer E4 证据和回滚材料。
- 依赖关系：目标契约、全部已知真实 Consumer 同步切换和旧入口删除已在同一切片完成；若已有生产发布，还需 E5 观察或明确风险接受。无运行宿主时必须有 `not_released / no_runtime_consumer` 的可复核证据。
- 完成条件：零剩余旧 Consumer/反射/序列化入口证据成立，Provider E3 与全部受影响 Consumer E4 通过。
- 停止条件：任何 Consumer、运行制品、数据迁移或整切片回滚路径不清，立即阻断整个切片；不得回补兼容分支掩盖缺口。
- 交接：清零复验不自动授权 enable、release 或 production；残余发布风险进入独立发布 Gate。

### 8.1 重构准入、迁移规则与切片顺序

#### 重构准入与当前问题证据

本计划需要独立重构设计，是因为它涉及跨模块 public contract、真实 Consumer、核心资金链和旧契约删除；局部行为保持型整理仍只使用切片卡与测试，不要求补齐无关产品设计。当前问题与证据包括：公共概念与场景名相混合、Provider/Consumer SNAPSHOT 漂移、真实宿主 E4 缺失、当前 worktree 多模块 dirty overlap，以及原 R2-R7 模块瀑布。目标结构不是一次重写后的目录图，而是每个垂直切片都能独立保护行为、直接切换一个明确目标契约、由真实 Consumer 验供、暂停和整切片回滚。

行为不变量与公共契约不变量以已接受 `Q-001 / Q-002 / Q-003 / Q-004` 和对应场景合同为准；具体场景 Owner PENDING 仍按其影响范围阻断切片。替换范围只由当前切片的 accepted decisions 与 AC 决定。用户已明确本次重构不考虑兼容：不保留旧接口与目标接口共存、bridge/default adapter、转发重载、别名、双写双读、兼容窗口、deprecation 周期或延后退役。所有已知真实 Consumer 必须在同一垂直切片切到目标契约，旧入口在该切片内删除；若任何 Consumer 不能同批切换，切片保持 `blocked`，不能用兼容层推进。

“不考虑兼容”不等于降低正确性门槛：Consumer 清册、accepted revisions、Provider E3、source -> binary -> resolved/loaded、real Bean、联合 schema/事务、资金不变量、数据迁移校验、独立 Checker 和 enable/release/production 授权仍必须分别满足。数据或 schema 变化只允许一次性显式迁移，必须有 dry-run、备份、守恒/对账校验和整切片回滚；不以双写、影子读或旧 schema 共存兜底。回滚是回退整个 Provider + Consumer + 数据版本切片，不是保留旧运行路径。

#### 迁移规则

| 规则 | 目标态切换 | 验证与数据动作 | 回滚 / 暂停 | 准出条件 |
| --- | --- | --- | --- | --- |
| public contract 迁移 | Provider 与全部已知真实 Consumer 在同一切片直接改为目标契约，并删除旧入口 | Provider E3、Consumer compile/load、全部真实 Host E4 和零剩余旧引用检查；不保留任何兼容形态 | 任一 Consumer 或 E4 失败则整切片不准出并整体回滚 | 目标契约单轨、旧入口清零、全部已知 Consumer E4 与 Checker 通过 |
| private/internal 行为保持重构 | public/serialized/Consumer 可观察行为不变 | 用特征测试、契约测试、模块/边界/回归测试验证；不触发跨项目会商 | focused 或边界测试失败即回退本切片，不扩大 public API | Checker 证明行为不变量保持 |
| 数据/schema 变化 | Provider、Consumer 与数据/schema 在同一切片切到唯一目标版本 | 一次性迁移 dry-run、备份、行数/金额/币种/引用守恒、对账和真实 Host 验证；禁止双写双读与双 schema 共存 | 任一差异、锁风险或不可逆写入即停止并整体回滚 | 目标 schema 单轨、数据校验、回滚/前滚和宿主证据全部闭合 |

#### MIG 切片与能力轨

`R2` 至 `R6` 仅是切片内按需调用的能力轨，不再是必须整模块完成的全局顺序。一个真实场景切片可以只使用其中必要的两个或三个能力轨；若改变 public/serialized contract 或 Consumer 可观察行为，`R7` 必须作为同一切片的随行步骤，不能留到所有 Provider 模块完成之后。

| 切片 / 能力轨 | 目标 | 前置条件与写入范围 | 验证证据 | 暂停 / 回退 |
| --- | --- | --- | --- | --- |
| `RS-001` 清册与切片准入 | 冻结当前已消费 public contract、Consumer、API disposition、dirty overlap 和首批候选 | 本 `plan-r2`；只写本执行规格，不改生产源码/API baseline/测试 | 全仓/跨仓源码与 artifact 指针、dirty fingerprint、Checker | 证据不足标 blocked/stale；不猜目标 API |
| `R1` 保护网 | 为某个 eligible slice 补最小特征/契约 RED 与边界守卫 | 对应 accepted AC；只写该切片白名单测试 | RED 原因、focused/module/boundary/API 验证 | 失败原因不是目标缺口即停止 |
| `R2` Core 轨 | Money、内部账户/责任与四类引用等稳定内核 | 只处理当前切片依赖的已确认概念 | core/DSL/依赖测试 | 不引入场景私有对象；具体 Intent/Attempt 物理类型仍须 G2/G3 裁决 |
| `R3` Ledger 轨 | posting、ledger transaction/entry、balance projection 与查询证据 | 当前切片需要账本变化且账务不变量已冻结 | 平衡、幂等、追溯、投影测试 | 账本事实与余额投影不得合并 |
| `R4` Wallet 轨 | 账户身份/能力、责任解析、工具绑定与余额查询 | 当前切片需要账户准入；责任/币种明确 | 账户能力、共享责任、零副作用测试 | wallet 不创建交易或账本事实 |
| `R5` Transaction 轨 | 单一用例的 route、原事实、恢复与查询证据 | 对应场景语义已确认；只做一个 action family | SIM、幂等/冲突/逆向/UNKNOWN 测试 | 不做万能 instruction；不整单重做部分成功 |
| `R6` Reconciliation 轨 | source/result/difference/Gate 及适用清结算用例 | 只在切片需要时进入；外部来源未签收则明确 fail-closed | 重跑、差错、Gate 零资金与只读测试 | 不反写主链，不由对账自动发起资金动作 |
| `R7` 随行 Consumer 轨 | 全部已知真实 Consumer 与 Provider 同批切到目标契约 | 与影响它们的 Provider public slice 同批；写入白名单逐宿主列出 | Contract Inquiry、compile、artifact origin/hash、Consumer Reconciliation | docs-only Consumer 只走读；任一真实 Consumer 未确认则切片不启动 |
| `R8A` 宿主 E4 | 证明目标契约在真实 Host 的 Bean/schema/事务和业务场景履约 | Provider/Consumer 仓内验证通过 | source -> binary -> resolved/loaded -> real Bean -> joint schema/transaction -> SIM | 任一谱系或宿主事实不清即阻断整切片 |
| `R8B` 目标态清零复验 | 证明旧契约、旧引用和旧序列化入口已在同一切片清零 | 全部已知 Consumer E4；零剩余消费；切片删除授权 | Provider E3 + Consumer E4 等价回归 | 有任何未知 Consumer、回滚或运行证据缺口则阻断整切片 |

固定迁移顺序为：`RS-001 清册 -> 选择一个 eligible 垂直切片 -> R1 保护 -> 按需调用 R2-R6 -> Provider 与全部已知真实 Consumer 同批目标态切换并删除旧入口 -> Provider E3 -> R7 Consumer 对账 -> R8A E4 -> R8B 零旧引用复验 -> 独立 Checker`。优先级是：真实已消费且有风险的契约 -> 支撑它的最小深模块变化 -> 当轮全部 Consumer 切换与旧入口清零 -> 最后才是新增能力。

#### Engineering Handoff / 第一实施切片入口

当前只准入 `RS-001`，不预选第一实施切片。`RS-001` 的执行 Owner 为系统架构师，验证 Owner 为未参与主笔的独立 Checker；其唯一产物是“已消费 public contract + Consumer + API disposition + dirty overlap + 候选垂直切片”清册。清册完成并通过 Checker 后，才从候选中选择一个 `eligible` 切片生成独立 `Refactoring Slice Card` 和写入授权；未被选择的切片不创建脚手架、分支或占位抽象。

```text
RS-001 Entry Card:
slice_id / plan_revision / status: RS-001 / plan-r2 / READY
slice_kind: inventory
execution_basis: Q-001~Q-003 accepted + conditional scenario contracts + this Change state_revision
blocking_decisions: none for inventory
excluded_fail_closed: 5.12 product/external PENDING unrelated to inventory; do not design or implement their semantics
host_E4_blockers / external_E5_blockers: recorded only, not closed by inventory
write_whitelist: openspec/changes/funds-public-capability-redesign/spec.md
read_only_scope: core, all *-face, direct implementations, tests, API baseline, capte-domain/fincone evidence, resolved artifacts
forbidden_scope: production source, test source, API baseline, PRD/DSL/system/TDD conclusions, Git writes
baseline_HEAD: eb12091819152fcec529f9453b48755f3aa2c999
tracked_dirty_scope_fingerprint: core/ledger/wallet/transaction/reconciliation/tests/api = a7a66f1908f42d815ae66fed9618be5334c1e9d19ef6f84fb74a4e8a013835cd (38 paths, 2026-08-12)
required_output: consumed-contract inventory, authority/Consumer evidence, current disposition, dirty overlap, stale conditions, 1-3 vertical slice candidates
verification: source and artifact anchors + current HEAD/dirty fingerprint + independent Checker
stop: missing Consumer authority, untraceable artifact lineage, ambiguous public behavior, or overlap that cannot be classified
next_entry: one selected eligible vertical Slice Card; otherwise blocked item + Owner + recovery evidence
```

### 8.2 `RS-001` 公共契约与 Consumer 清册

#### 执行边界与方法

`RS-001` 于 2026-08-12 只读取 `wind-funds` 当前 source/API baseline、`capte-domain` production/test/POM/schema/本机解析制品以及 `fincone` docs-first 权威；唯一写入为本节。未修改生产源码、测试、API baseline、正式产品/DSL/系分/TDD 文档或跨仓文件，未执行 Git 写操作。扫描只覆盖已知仓库、当前 Maven 解析、本机类加载探针和显式源码/文档入口；“未发现 Consumer”只表示本次范围未发现，不等于零剩余 Consumer、可内收或可删除。

冻结版本：

| Authority | Revision / fingerprint | 证据等级与失效条件 |
| --- | --- | --- |
| `wind-funds` Provider | `master@eb12091819152fcec529f9453b48755f3aa2c999`；`core/ledger/wallet/transaction/reconciliation/tests/api` 38 条 tracked diff aggregate=`a7a66f1908f42d815ae66fed9618be5334c1e9d19ef6f84fb74a4e8a013835cd`；同范围 untracked manifest=`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` | `E2` 当前 source/worktree；HEAD、38 条路径集合、任一内容或 untracked 变化使受影响清册 `stale`。 |
| `capte-domain` Consumer | `master@6de70922f2565ce1f7ff036d457c29f22cdab829`；tracked/untracked 均 clean，fingerprint=`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`；observed_at=`2026-08-12T14:58:58+08:00` | `E2` source/worktree；HEAD、POM、源码、schema、artifact path/hash/CodeSource 变化使对应 Inquiry `stale`。 |
| `fincone` docs-first Consumer | `d9ae6d69242a97735793b44fd6a69255e28d1a24` + 13 tracked dirty paths；binary diff=`490dd02e2e0537899b7841588bda6abf8e938bc5414026ed2fa01c3a9c6af431`；porcelain-z=`8ce6fa19ae7ac38e2a68a1e54bdc4759494ff98720e7c0dc755bde35a59074a5`；observed_at=`2026-08-12T15:25:57+08:00` | `E1` docs-only；没有 runtime source、manifest、schema 或 artifact。HEAD、dirty diff、文档版本/status/OwnerDecision/signed scope/hash 变化使对应条目 `stale`。 |

#### Public surface 与统一 API disposition

本清册不复制第二份 400+ 类型百科。`core` 成员级覆盖由现有机械清单定义；face 的集合定义为下列 content manifest 对应源码根中每一个 public 顶层类型及其源文件内 public member，统一 disposition 对集合中每一项逐项生效。Checker 可按冻结 revision 重新枚举路径并回读声明，真实 Consumer surface 再逐项展开；这是一种集合式清册，不把目录数量当成员证据。

| Surface | 完整性指针 | 当前 disposition | 不能外推 |
| --- | --- | --- | --- |
| `core` public members | `core/api-baseline/stable-api.txt` 当前 1062 行，SHA-256=`4dbe9d168cc41bc2ee6e04debb61854b042d2106212e1974cace639da2dab478`；HEAD blob=`84a6182a173074233ea631e3e90625d817612a33`；当前 diff=`26999996043d5839637ab9293d011aae615b4d2d2b1453a57c519d68d9125f2e` | 未在下表单列的成员一律 `保留现状 / 待确认`；历史 stable 只记录现状，不批准目标归属、语义或兼容承诺。 | baseline 本身 dirty；不得据此认定目标 API 已接受。 |
| `core` policy exceptions | `api-policy.tsv` SHA-256=`fcc2203125df966192d36da45661daac1ea54d1067496586fa11cb74b0af8dd5`：4 `EXPERIMENTAL`、4 `INTERNAL`、20 `EXCLUDED_MEMBER` | experimental=`待确认`；internal=`内收现状、禁止外部新增消费`；excluded=`独立 Owner 决策前待确认`。 | internal/experimental 不等于可删除；excluded 不等于已拒绝。 |
| `ledger-face` | 11 Java files，content manifest=`9c51f90689594bd500d412034af2ab82902fd12169c3b1f20466c3484a44a909` | 全部 `保留现状 / 待确认`；本次未发现 Capte direct dependency。 | 未扫描未知仓、反射和已发布制品，不能证明无 Consumer。 |
| `wallet-face` | 104 Java files，content manifest=`085a5cbb7fec48f6edc3f35360b882238f94aca208487e990ae8d6d65e82b0b1` | 真实 Capte production 使用项见下表；其余 `保留现状 / 待确认`。 | 没有独立 stable member baseline；source manifest 只冻结本次内容。 |
| `transaction-face` | 69 Java files，content manifest=`3e1e1da111feef6f062e675387181528019866a80388f0a72f2dac7119b5cce9` | 真实 Capte production 使用项见下表；其余 `保留现状 / 待确认`。 | Snapshot 与当前 checkout 已漂移，不能把当前 source 当 Consumer accepted artifact。 |
| `reconciliation-face` | 125 Java files，content manifest=`fed58864450eccff71ba8b7ec516e6a2a9c05c2002059692dfdce9e3cec61ca6` | 全部 `保留现状 / 待确认`；Fincone 仅有 docs-first 局部子契约。 | docs-first 名称不构成 runtime Consumer 或 API 签收。 |

真实 Consumer 使用的 public type/member：

| Contract surface | 真实 Consumer / 场景 | 当前语义与风险 | `RS-001` disposition |
| --- | --- | --- | --- |
| `FundsAuthorizationTransactionService#authorize/complete/reversal/refund`；四类 Request；`WindOperator` | Capte `WalletPaymentParticipant` 商品钱包授权、完成、撤销、退款 | 同步返回资金流水字符串；Consumer 以非空返回判本次调用成功，异常会压成 failure；返回值与动作明细/原授权链的稳定定位尚未形成 E4。 | `保留`；任何签名、返回语义、失败/UNKNOWN 行为变化必须走 `VC-001` Inquiry、随行 R7 和 E4。 |
| `FundsTransactionQueryService#queryFundsTransaction/queryFundsTransactionDetails`；`FundsTransactionDTO/DetailDTO` | Capte 钱包完成后退款定位、授权事实回读 | Consumer 通过完成返回流水的 `referenceTransactionSn` 反推原授权；当前 loaded Snapshot 与 Provider checkout/Recording 已有方法与 DTO 漂移。 | `保留`；原事实定位契约为 `VC-001` 核心问题，未定前禁止替换或退役。 |
| `FundsAccountId`、`DefaultFundsAccountType#USER_WALLET/isUserWalletType`、金额/币种对象 | Capte 钱包账户构造与授权事实校验 | Consumer 直接从业务 walletCode 构造 account；tenant/user owner binding 与真实 payee 责任仍是产品 PENDING。 | `保留`；账户构造目标语义 `待确认`，不得由清册批准新 API 或把业务 ID 自证为账户。 |
| `FundsBenefitContributionTransactionService#settle/refund`；两类 Request；`FundsTransactionQueryService#findFundsTransactionByBusiness`；`FundsTransactionDTO#getSn` | Capte Benefit 按活动责任项发起资金出资与退款 | `P-SIM03-01-D` 已要求每次核销在 A/B/C 中唯一选择，`P-SIM03-02-R-A` 已接受恢复优先政策；当前 Consumer 仍无路由/策略结果与责任快照，忽略 settle/refund 返回引用、按记录存在判断完成，并在退款时重读现行 funding；请求必填账户/承接账目也未闭合。 | `保留 / 替换候选待确认`；由 `P-SIM03-01-HOST / P-SIM03-02-HOST / P-SIM03-03` 与 `VC-002` 阻断，不能先改 public contract。 |
| `FundingAccountService#getFundingAccount`、`CreditAccountService#getCreditAccount`、`FundsSubjectBalanceQueryService#queryCurrentBalances` 及 DTO/Query/status | Capte Coupon 活动发布前账户可借贷/可贷记和余额初始化检查 | 只证明 production call placement；Capte 没有 funds impl、联合 schema/事务或 real Bean。 | `保留`；宿主装配属于 E4 blocker，不以扩 API 解决。 |
| `FundsTransactionQueryService#findFundsTransactionByExternalFundsFact` 等 loaded artifact 新成员 | Capte production 未调用；Recording 未实现导致 test compile 漂移 | 只证明共享 Snapshot 与当前 Consumer 测试面已漂移。 | `保留现状 / 目标处置待 Inquiry`；若替换，相关 production/test Consumer 同切片切换，不提供 shim；不得把 test Fake 漂移冒充生产行为缺口。 |

Provider production/test、Capte production、Capte test/Recording 和 Fincone docs-only 已分层：Capte production 调用是本表的真实 Consumer；Recording/Fake 只证明测试切换影响，不证明 real Bean、联合 schema、事务或 E4；Fincone 不计为 runtime Consumer。

Capte test/Fake/Recording 切换影响面：

| Test Consumer | 使用的 contract surface | 证据锚点与不能外推 |
| --- | --- | --- |
| `WalletPaymentParticipantTests` | 手写 `FundsAuthorizationTransactionService` 与 `FundsTransactionQueryService` Recording | `tests/.../WalletPaymentParticipantTests.java:287-409`；只验证 Consumer 单元编排与接口编译，不证明 Provider Bean、ledger/balance 或 E4。 |
| `CouponRedemptionApplicationServiceImplTests` | Benefit transaction/query Recording；in-memory map 返回 DTO，details 为空 | `tests/.../CouponRedemptionApplicationServiceImplTests.java:1920-2001`；不能证明真实资金状态、明细或账务。 |
| `CouponActivityServiceImplTests` | transaction/query unsupported proxies，以及 wallet account/balance anonymous Beans | `tests/.../CouponActivityServiceImplTests.java:1142-1239`；只保护 readiness 调用面，不能证明真实账户、余额或联合事务。 |

任一对应测试源码或 accepted interface revision 变化使 test contract 结论 stale，但不自动使 production behavior 失效；反之 production compile 也不覆盖 Recording 编译失败。

#### Capte 三制品谱系

`plan-r2.66` 历史基线中的 Capte 声明 `1.0.0-SNAPSHOT`，`capte-order-impl` 直接消费 core/transaction-face，`capte-coupon-impl` 直接消费 core/transaction-face/wallet-face；没有 direct ledger-face、reconciliation-face 或任何 funds impl。下表只保留当时制品谱系，三个制品不得合并外推：

| Artifact | Resolved origin / SHA-256 | Actually loaded | Provider source lineage | E4 |
| --- | --- | --- | --- | --- |
| `com.wind.funds:wind-funds-core:1.0.0-SNAPSHOT` | `/Users/wuxp/.m2/repository/com/wind/funds/wind-funds-core/1.0.0-SNAPSHOT/wind-funds-core-1.0.0-SNAPSHOT.jar`；`2b2afa58d3fef5828a66446bdf1b5b6c4339ef882834a1da3fb3ad25a31ba39f` | `FundsAccountId` 的隔离 JVM `CodeSource` 指向该 JAR | `UNTRACEABLE`；manifest/POM 无 Provider commit/revision | `NO` |
| `com.wind.funds:wind-funds-transaction-face:1.0.0-SNAPSHOT` | `/Users/wuxp/.m2/repository/com/wind/funds/wind-funds-transaction-face/1.0.0-SNAPSHOT/wind-funds-transaction-face-1.0.0-SNAPSHOT.jar`；`21fb3e0b5b50647385861f368f91aebeacef29c5e13ac52fd5d848e9b177de6e` | `FundsAuthorizationTransactionService` 的 `CodeSource` 指向该 JAR | `UNTRACEABLE`；且与 checkout/Recording 存在接口漂移 | `NO` |
| `com.wind.funds:wind-funds-wallet-face:1.0.0-SNAPSHOT` | `/Users/wuxp/.m2/repository/com/wind/funds/wind-funds-wallet-face/1.0.0-SNAPSHOT/wind-funds-wallet-face-1.0.0-SNAPSHOT.jar`；`61948a24e74bfd51eee701e443c2396b3e6377d7abfd86945e2c6131e7632116` | `FundingAccountService` 的 `CodeSource` 指向该 JAR | `UNTRACEABLE`；manifest/POM 无 Provider commit/revision | `NO` |

类加载结果只来自隔离 classpath 探针，不是 Capte Spring runtime。Capte 当前没有 funds impl dependency，测试 schema 没有资金/账本/钱包表，Order/Coupon 本地事务也不能证明与 Provider 共享 resource 或 transaction manager。生产源码编译、中央 tests 的 54 个陈旧源码错误和 Recording 接口漂移继续作为不同层证据，任何一层都不能覆盖另一层。

#### Fincone docs-first authority disposition

| Authority document | Declared status / SHA-256 | 可进入本清册的范围 |
| --- | --- | --- |
| `fincone/AGENTS.md` | project contract；`2157c78d3d1bfb2c1941f5bdb1052af913eae36b27dacc05611e72cef7f49ff3` | 已接受仓库职责边界：Fincone 持产品/系分，`fincone-issuing` 才是 VCC runtime，`wind-funds` 持资金事实。 |
| `业务架构/Fincone3.0业务架构.md` | `Ready for Review`、无 `OwnerDecision`；`74e5542e0425342dd2be00725674a5b767ab34871e69b3efe93d010eb44e3d88` | 业务对象权属与价值流 candidate baseline；不进入 accepted invariants。 |
| `资金内核-产品设计.md` v1.1 / `资金内核-系分设计.md` v1.0 / `资金内核-验收与准出清单.md` v1.0 | `REVIEW_READY/PENDING`、Gate `OPEN/E2_PARTIAL`；`dc5852b786814a70c150f78f5a34f9f9ba82d36da999cda8f6384fcf83bb6d3c` / `71d119d2f67229eb8ca49bc02bf0a28b2c7854d43a3603e33ecac47ccb8ef6c7` / `2954b617e7fffe0c34e80e5056379ef5d49d0a07dad2828df1b1d25cbf0df8d4` | 只能作为候选、hard negative 和停止线；未签收整体 public contract。 |
| `wind-funds集成指南.md` v1.2 | `REVIEW_READY/PENDING`；`ca878bd784cc949fc51c5d2f5c36d09ba7313208b66cb3e7be54475b3529e01e` | 仅 Benefit 与 `WF-FIN-CLR-FND-001` 局部 child scope 可进入 accepted inventory；整体不接受。 |
| 订单产品/系分/准出 | overall `PENDING`；BenefitDesignContract child=`ACCEPTED`；`ab7aebfd6bc8689880dfdad317242450f7636ab280b4f14be339378c5d942e11` / `e085790a711fb080b1de55062ef66eca3a551c5841e56adf6406a4b758fb2542` / `25372cc50e2608e005928e5d36b7a7c9268e5909e96cdd7ccde5d998a1df1597` | 仅 trusted payee、merchant `CLEARING`、逐 contribution 原引用等设计子契约；L3/enable/release/production 均 blocked。 |
| VCC 产品 v3.3 / 系分 v2.9 / 准出 | `REVIEW_READY/PENDING`、`NOT_STARTED`；`a722daccd26a0596a55985bc1042ea89991d2a2e9dd4b13a29b117449c37e2ad` / `1de3de4813b2462cf8d9e26c5a6d4913dd67a8ba6d302786f4de94b101689f72` / `2111cfd1398fe49e9c3cda45f95d36b6dec5657817f3026b3094857a861dea24` | 仅场景候选与 hard negatives，不能批准 public/runtime contract。 |
| 全球账户产品 v1.4 / 系分 v1.2 / 准出 | `REVIEW_READY/PENDING`、E1；`c6ab8f3705ca2d7f6b06b673cee56e808be5edfb11bc9e32f6850f8699a8ff4a` / `871f7acc1f2956beb27a8aa61312a159601d626a42309287fbe6291135a49787` / `a80a15eb0fba77ba6d9c85ddd805580f40be0cc409ed8a2f75f218d6793e4c97` | confirmed/return/NOC/finality 仅候选；不能批准自动资金动作。 |
| 产品权益与服务计划产品设计 v3.3 | `REVIEW_READY/PENDING`；`c14a31c1fa15915acbd2453d9ea21c2d052543c75b725f606b739aa7c213b34a` | payerRef、费用决策与 wind-funds 动作仅为 candidate；`ENT-GATE-007` 未关闭，不进入 accepted invariants。 |
| `收单业务准入-准入卡.md` v1.0 | `Admission=BLOCKED / Owner=PENDING`；`8b22014f7b65188acef7f221d0f69b989591b355df43d17418d10f2cf44ac773` | 仅 hard stop：不得据此启动收单 public/host slice。 |
| 清结算产品 v1.4 / 系分 v1.2 / 准出 v1.5 | overall `PENDING/PARTIAL`；`WF-FIN-CLR-FND-001` child=`ACCEPTED`；`37aeb9bd43da9eadde133cca9921d46ef04dde11fba7cbc1da74f88997b5c086` / `39582f894651e52f0846ff7a519f881c2f010a9102728ebcd67f85883fb83e34` / `48954dc3cb16098b30aa2c069948a40cae520ec689415de8cd3d0ce19c0aa0d8` | 仅 application-use-case handoff、1:1 replay/引用与 authority 分离子契约；真实 host、Gate、release/payout 仍 blocked。 |

短 hash 只作可读索引；对应完整 hash、dirty fingerprint、declared version、status、OwnerDecision 与 signed scope 已冻结在本次 Fincone Project Fact Card，任一变化即重开。当前没有整份 Fincone 文档达到 `OwnerDecision=ACCEPTED`，局部 child scope 不得提升为整份接受。

#### Provider 既有 dirty overlap manifest

总 fingerprint 与以下 38 条逐路径清单一致；`avoid` 表示本次和候选 pre-Inquiry 只读避开，`isolate+manual-merge` 表示未来获独立 Execution Grant 后也必须先在隔离候选中验证并人工合并，`stop-owner` 表示未取得重叠工作 Owner 处置前不得进入写切片。

| Path | Kind | HEAD blob / diff SHA-256 | Candidate overlap | Disposition |
| --- | --- | --- | --- | --- |
| `core/api-baseline/stable-api.txt` | API baseline | `84a6182a173074233ea631e3e90625d817612a33 / 26999996043d5839637ab9293d011aae615b4d2d2b1453a57c519d68d9125f2e` | `VC-001/002/003` | `stop-owner`；任何 public slice 不得覆盖当前 baseline diff |
| `core/src/main/java/com/wind/funds/route/support/ExternalAccountSensitiveValueValidator.java` | production | `c764378f11a439f291f00cb42bc63b45c7b11f4f / 7c8b585e68b595fdb34a24ec0b0330b92d060a9e537dd769857d677610034cf4` | none | `avoid` |
| `core/src/main/java/com/wind/funds/wallet/enums/SpendRuleBindingExplanationStatus.java` | production | `6583e2f46b4680b706e5b74576bb61bb474c62c0 / b07199bd99671b41976a2b8d815152edd590e2648d79f6a57b0582e01921295d` | none | `avoid` |
| `ledger/impl/src/main/java/com/wind/funds/ledger/dal/entities/Ledger.java` | production | `6e6bb769d3cc2da9857556bbc02550aea31fcdc6 / 4ad5531c52d129e97ea7f8352efcdd43478d8d5f680e2645d99b16c44be82c14` | `VC-001/002` 的潜在 E4 证据，不是首批写入 | `avoid`；若写入则 `isolate+manual-merge` |
| `reconciliation/face/src/main/java/com/wind/funds/reconciliation/enums/ExternalRuleVerificationStatus.java` | public | `477cad041c60680d1e5600b151e295353942b347 / 92a4bd6c7247d2940d0526a954b5611c9129e3021db89a197c355fa646e672ac` | `VC-003` | `stop-owner` |
| `reconciliation/face/src/main/java/com/wind/funds/reconciliation/enums/PayoutDisplayStatus.java` | public | `96b2d1f8fdaba2c5ca5ac97e4096f0f67d4af11d / 83b12dc370db2a94b91526cb899211c356d59102c8a6c31bdfb786b79386da9e` | `VC-003` | `stop-owner` |
| `reconciliation/face/src/main/java/com/wind/funds/reconciliation/enums/PayoutPreflightDisplayStatus.java` | public | `4d50cc0826eea0524e7f4803fbd82c9c43567f36 / ebfe6abeda80a04e19efce40d90d394f39c2615a18b0467ad89fb76507840fc8` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/ClearingBatch.java` | production | `58628ac46a71075a7af3a730deb4bc49765f1c2f / 972b8c9d20467ed4930b12990988cf666475093cc549a4c181530c1c93ca14bd` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/ClearingCandidate.java` | production | `8abbed431d6f0355c0fae62f10e69904eb479be9 / 5e8fc93bd95dd19787dea6040c4d9feac47fd482c45dfddfec7e4f348a02ffb7` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/ClearingSplitBatch.java` | production | `bd4c60b4de85e2544ca878ecf71d1dbd762be798 / d47073c07f6ff8ac50c9b85fe38c1909cb690554aac87ea34c0f19febac5a720` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/ClearingSplittableDetail.java` | production | `bee3b6d428ebe6b5c873e7921f7b4523179432f2 / 5dba3dfac975b698fb9d5b3aba8e896a9deceec2b4e055d08fdcf5b4e5ccbd9e` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/PayoutOrder.java` | production | `17442ba0d2f231c3806f3ca54685db8348c795bb / f3658f72da5698bbb148e4c7078e69f0f1a623fb8d1536619f4f3ab792aad7dc` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/PayoutReceipt.java` | production | `454dfb68538492f5bbfb72dac453d390f5b3c4a4 / 39d587c4b74580e5c76331644c3a797ec5a6a7a3ca99ae48e43ff41429dcfc9a` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/ReconciliationBatch.java` | production | `bedf30a8a0fa74c875daa6e1d020d350fe8c15cb / 4c6516d599599d3b68db27da35e4b3a1962859b987726462f259d4b3dee55236` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/ReconciliationDifference.java` | production | `7db27c7890f34638c7b7d61f7649b9f4dd698922 / d1a3af90f8d56c6c73b0013c3e97f4b8439d47f7382e1b6be205f4430b107a38` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/ReconciliationRunResult.java` | production | `4fd94b714737294f42cfe0e873e5fbb63f8bfcdd / 84140e1c8ffe397cab696d74d9a2507d872a6f336efb8ecc774bacdda004736e` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/RecoveryOrder.java` | production | `31b445b73a3064e816d518fe57159f07f4c20883 / 60b3efe8f4ab36f908b8dc2c3f2155aef115d277117b94094fbd6ad1a05dd2b7` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/SettlementOrder.java` | production | `e77418b6df711921f4ff0bd31d11d3f85906a7d8 / 1bc34ee935fa6ebdd7cefa9e8cb0324abeba7c1107683c59f319e4d10a492baa` | `VC-003` | `stop-owner` |
| `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/mapstruct/ClearingSplittableDetailConverter.java` | production | `9fa2be719b123532737177e31b85f71f791d0321 / 619335e71b22b1e317684a62138eb49d257e9852bb202f2f72d9de1b05cb3296` | `VC-003` | `stop-owner` |
| `tests/src/test/java/com/wind/funds/architecture/FundsModuleDependencyBoundaryTests.java` | test | `6d1430a131d6880e34a68e90701cd91177f09125 / 5de93542c82b774dc96873fa01e2c24e36bf15674a527f75f5178999c1639c62` | all future module-boundary slices | `avoid`；若切片需要则 `isolate+manual-merge` |
| `tests/src/test/java/com/wind/funds/dsl/SensitiveContextVariablesValidatorTests.java` | test | `6d1043c3db59c828478127de8f8b702cbe65aae8 / f0e153ac4a4b7ba546bb908d2f43fac2dec4b0dc28949511d77922b4c3bfb411` | none | `avoid` |
| `tests/src/test/java/com/wind/funds/reconciliation/application/batch/impl/ReconciliationBatchApplicationServiceTests.java` | test | `b4cefc70685df551c26894c0ce26cac6fd71342f / e6f978d0748074552ab39e799548808481e107b624420a0edb605ca3d720755d` | `VC-003` | `stop-owner` |
| `tests/src/test/java/com/wind/funds/reconciliation/application/clearing/impl/ClearingSplitBatchApplicationServiceTests.java` | test | `e44546b906fc4efa95308d2f6a3a03f29cfebf8e / d55456ea061cd117655ae0187e788c21035e1d2c1de60c5f10fe27bd44ee592c` | `VC-003` | `stop-owner` |
| `tests/src/test/java/com/wind/funds/transaction/application/flow/AgentCommissionSettlementBusinessFlowTests.java` | test | `0f32aabcc8d5dbaf46ac6f42378adfd89fbea7a1 / e046690a2f3d2278818217442ddf854a8ee427e758f0253cd9ef7a96eb534c81` | `VC-003` | `stop-owner` |
| `tests/src/test/java/com/wind/funds/transaction/application/flow/ClearingBatchApplicationServiceTests.java` | test | `df4ad5e3ce1c542375573f152466f9b44ed1bfe4 / a10194ac82311ed298131984928f2e6f80c79c128d927399fc552ac634a9511a` | `VC-003` | `stop-owner` |
| `tests/src/test/java/com/wind/funds/wallet/application/account/FundsAccountCapabilityApplicationServiceTests.java` | test | `456b25ba319d684213312573277fd33b09002c84 / ce83753254f8829428922f0d6982b0b746d9320aa937d7b0d38ce600832f440a` | `VC-002` | `stop-owner` |
| `tests/src/test/java/com/wind/funds/wallet/services/impl/PaymentInstrumentServiceImplTests.java` | test | `5fc23ff3c0712eadf327af81de1e23299f4f4f79 / e73d9282dcdd8d4fc145791c9c81681bcd0b46af869d4d3135566ab0932358bb` | none | `avoid` |
| `transaction/impl/src/main/java/com/wind/funds/transaction/dal/entities/FundsFrozenOrder.java` | production | `ee2b318f7edf226903ff43af9c2bf91100940d3e / 6a8976c862c555735212545a1d52ed34ca2b383a9bb4b82f28c50018854b578d` | `VC-001` 潜在原授权/释放实现 | `stop-owner` |
| `transaction/impl/src/main/java/com/wind/funds/transaction/dal/entities/FundsTransaction.java` | production | `179365e92e6c8df4b544aae8f83107b0775587f8 / 2fe6efa2eba97c56147203c1d70f013cbe011a0d887ba854861ac331385270ff` | `VC-001/002` 事实模型 | `stop-owner` |
| `transaction/impl/src/main/java/com/wind/funds/transaction/dal/entities/FundsTransactionDetail.java` | production | `dee889b34334e73d7083bd24ce489b8f59878a23 / 8fea512c338c8f7aa0e337d1c07b59db0fce0ed380da09097ef05b4fb6614ab9` | `VC-001/002` 动作证据 | `stop-owner` |
| `wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/CreditAccount.java` | production | `2e6d42732f012de466714cf7606e1767c79a42ca / 2bfacf9bd92694e7bfb1b4675d0bfc740084b912b9a50111f91ad0878bb7786c` | `VC-002` 责任账户 | `stop-owner` |
| `wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/FundingAccount.java` | production | `fd34ea38299951eedaa3b63a69729d7f53c7a85d / 674dee918b052e2a20486984ec86e5f9df83749dbe1981e9fd2ecfbb23bdc37b` | `VC-002` 责任账户 | `stop-owner` |
| `wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/PaymentInstrument.java` | production | `e4d438b522fdac44da24437e63175a060951bb47 / 09f81f25bd076a86500ebdef66720e5677ff4cc7ccecbec7c502b0815b35a62e` | none | `avoid` |
| `wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/PaymentInstrumentBinding.java` | production | `8f28bdabbc477f3bbe06e44d53ac7083e7ae975b / 18810b80147b79fbd7d9c8904f7dd4eddd61d9d1fa9a7e993213bccb196474af` | none | `avoid` |
| `wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/SpendControlScope.java` | production | `f2e3cb25ace37adb1e74725ea64dc901c4891330 / 17cf7fc1097edb1ed29779c347b0f3917b24b75c3e11d02b4013ff3c9a88deeb` | none | `avoid` |
| `wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/SpendRuleBinding.java` | production | `943558a59fa0d61ea83ded0f70293b7bb72bf26c / 88c6f48357e6abcbb25f68df8b505e7219105a1959419b44be9f98657d661a87` | none | `avoid` |
| `wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/SpendRuleDefinition.java` | production | `dd3f5db19e05d17abb8a5fbe4dd1d3ddcbbc0439 / e7a95c4ec35b3d74b80e83a168e9e7beddb403fa05a2a41535cb8a0be7325c0b` | none | `avoid` |
| `wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/SpendRuleVersion.java` | production | `638f0ed12d2462ddb7cd4dd501b829ab9bf9013a / 6298c3f4dfaf89b3d940f08c15c4f061b2af877d29b85cfbec727aff7b857730` | none | `avoid` |

执行切片必须回到上述完整相对路径并重新计算全 SHA-256；任何新增、消失或内容变化只使受影响候选 `stale`，不得通过格式化、覆盖、revert 或暂存消除 overlap。

#### 垂直切片候选与 pre-Inquiry 判定

| Candidate | 场景 / 真实问题 | 契约与能力轨 | 当前判定 | RED 候选、收益与停止线 |
| --- | --- | --- | --- | --- |
| `VC-001` Capte 钱包动作证据与制品谱系 | `SIM-01` production 真调用；complete 返回引用、查询事实、原授权定位与 loaded Snapshot 漂移尚未形成同一 accepted contract | transaction-face/core；按需 `R5`，严格内部资金闭合门槛要求账务/余额证据，因此还需 `R3/R4`；真实 Capte 随行 `R7/R8A` | `blocked`，不是 `eligible`。`Q-004`、`P-SIM01-01-D`、`P-SIM01-02-A` 与 `P-SIM01-03` 已接受；`P-SIM01-01-HOST`、`P-SIM01-02-HOST` 与 `P-SIM01-04` 继续阻断真实责任准入、展示取证和宿主 UNKNOWN 查询恢复，制品谱系与 host E4 仍阻断集成闭合。external rail=`NOT_APPLICABLE`。 | 最小可证伪 RED 尚未冻结：accepted artifact 下，完成动作能否以稳定事实唯一恢复原授权和动作账务引用，且 timeout 不被误判为可重做。命中 stable-api 与 transaction 三个 dirty entity 时停止交 Owner。 |
| `VC-002` Benefit 成本责任项与原引用退款 | `SIM-03` production 真调用；返回引用丢失、presence-only 完成判断、责任输入缺失和当前配置重算 | transaction-face/wallet-face；`R4/R5`，按需 `R3`；Capte `R7/R8A` | `blocked`。`Q-004`、`P-SIM03-01-D` 与 `P-SIM03-02-R-A` 已接受；`P-SIM03-01-HOST` 的路由/责任快照、`P-SIM03-02-HOST` 的父策略/恢复编排证据和 `P-SIM03-03` 的逐项权威证据仍阻断，不能用产品接受或 child contract 填补 Capte 宿主缺口。 | RED 候选仅作记录：按 D 冻结模式形成的每项资金/非资金事实可追溯，资金退款引用自身原成功事实且 FAILED/PROCESSING/UNKNOWN 不按存在视为成功。命中 API baseline、transaction/wallet entity 与账户测试 dirty，阻断闭合前不建 Inquiry。 |
| `VC-003` 清结算 application-only handoff / 1:1 replay 保护 | Fincone `WF-FIN-CLR-FND-001` 局部 child scope 已签收，但没有 runtime host，Provider reconciliation 当前存在大面积 dirty overlap | 按需 `R5/R6/R3`；真实 Host 未识别，Fincone 只能 docs walk-through | `blocked`。阻断：真实 Consumer authority/artifact、重叠工作 Owner；只有实际编码 fresh-Gate initial pay 时 `CF-001` 才阻断。`CF-002/003` 和 rail/finality 对本窄保护范围为 `excluded_fail_closed`。 | RED 候选仅作记录：application use-case handoff 复用原结果、Gate/authority 分离、对账零资金。不能以 docs hash 或 Provider 测试替代 R7/E4。 |

未选择 `VC-002/003`，也未为它们创建类型、脚手架或 API。VCC、GlobalAccount/ACH 与 acquiring 未列首批候选：前两者 Owner/rail/runtime 未签收，后者 Admission 明确 BLOCKED。

候选准入卡：

| Candidate | Provider / Consumer / Owner | Dependent decisions / blockers | Excluded / E4 / E5 | Forbidden / overlap / rollback / stale |
| --- | --- | --- | --- | --- |
| `VC-001` | `wind-funds transaction/core` / `capte-domain order`；Transaction + Capte Host Owner；Checker=`q002_decision_checker` | 依赖已接受的 `Q-001~Q-004`、`P-SIM01-01-D`、`P-SIM01-02-A`、`P-SIM01-03` 与 `SIM-01` 现状证据；`P-SIM01-01-HOST`、`P-SIM01-02-HOST` 与 `P-SIM01-04` 继续阻断真实责任准入、展示取证和宿主 UNKNOWN 查询恢复 | external rail=`NOT_APPLICABLE`；core + transaction-face lineage、稳定动作事实的真实供给、real Bean、联合 schema/tx 为 host E4 blockers；wallet-face 不属于本候选；无 E5 声明 | 禁止目标 API/代码/测试/Git；stable-api 与 transaction entities=`stop-owner`；无写入可直接停止；Provider/Capte order/POM/core 或 transaction-face JAR/CodeSource/dirty 任一变化即 stale |
| `VC-002` | `wind-funds transaction/wallet` / `capte-domain coupon`；Benefit、Order/Commercial、Finance/Accounting/Risk、Transaction/Wallet Owner | 依赖已接受的 `Q-004 / P-SIM03-01-D / P-SIM03-02-R-A`；`P-SIM03-01-HOST / P-SIM03-02-HOST / P-SIM03-03` 分别阻断路由责任快照、父策略/恢复编排和逐项权威证据；所需证据为宿主 D/R-A 结果和真实状态/账务查询合同 | rail=`NOT_APPLICABLE`；core + transaction-face + wallet-face lineage、real Bean/schema/tx 和所选模式完整场景为 E4 blockers | 禁止发 Inquiry/RED/API；baseline、transaction/wallet entities/account test=`stop-owner`；无可逆写入，阻断解除或 Consumer/POM/三 JAR revision 变化后重开 |
| `VC-003` | `wind-funds transaction/reconciliation` / Fincone docs-only，真实 Host 未识别；Clearing/Reconciliation + Host Owner | 仅 child design scope 已接受；真实 Consumer authority/artifact、当前 overlap Owner blocking；`CF-001` 只在实际编码 fresh-Gate initial pay 时 blocking | `CF-002` release、`CF-003` payout rail/finality 对 application-only/1:1 保护为 `excluded_fail_closed`，零自动 release/payout/终局声明；真实 Host E4 与 rail E5 分别未证明 | 禁止把 Fincone 当 runtime、禁止代码/API；reconciliation face/impl/test overlap=`stop-owner`；无写入可回退，任一 docs status/Host/dirty revision 变化即重开 |

#### 当时唯一下一入口

`RS-001` 已通过独立 Checker，当时唯一下一入口为 `CI-RS001-CAPTE-WALLET-001 / VC-001 pre-Inquiry + Information Readiness`；它只建立 Contract Inquiry 和独立 `Refactoring Slice Card` 的准入部分，不写 RED、生产代码、测试、API baseline 或 Consumer 仓。入口必须重新冻结：

- topic=`Capte 商品钱包动作事实、原授权定位与 accepted artifact 谱系`；不决定目标 DTO/API、账户责任、PAID/REFUNDED 门槛或失败后再次执行。
- accepted inputs=`Q-001~Q-003`、`plan-r2`、本节 Consumer/artifact/overlap revisions；该 Inquiry 当时发生在 `Q-004` 接受前，历史输入不改写；当前有效状态见 `8.4` 与恢复入口。
- `inquiry_status=DRAFT / INFORMATION_READINESS_PENDING`；Consumer=`capte-domain@6de70922...`，Provider baseline=`wind-funds@eb120918... + a7a66f...`；任一 source/POM/JAR/hash/loaded origin/dirty overlap 变化即 `stale`。
- write whitelist 仍仅为本执行规格中的 Inquiry/Slice Card 状态；forbidden scope 仍包括 production/test/API baseline/正式四层设计/Git。该 Inquiry Checker 必须回读 Provider source、Capte order production/POM、core + transaction-face JAR CodeSource/hash 和 dirty manifest；wallet-face 只保留在 RS-001 总清册与 `VC-002`。

`CI-RS001-CAPTE-WALLET-001` 通过只允许判定后续候选 `eligible/blocked/stale`；未获得 Human Owner 的独立切片授权前仍不得进入 R1、代码或 Git。

### 8.3 `CI-RS001-CAPTE-WALLET-001` Contract Inquiry

#### Inquiry 身份与边界

```text
inquiry_id: CI-RS001-CAPTE-WALLET-001
change_id / plan_revision / candidate: funds-public-capability-redesign / plan-r2 / VC-001
topic_revision / information_revision: topic-r1 / info-r1
topic: Capte 商品钱包动作事实、原授权定位与 accepted artifact 谱系
consumer_revision: capte-domain@6de70922f2565ce1f7ff036d457c29f22cdab829
  + clean tracked/untracked
  + root-pom:bcbf453482f6846f1a1763395990a7a5512aa9c028fe91db1fc4ceac2cf23350
  + order-pom:69c4a20c4ac1bc644713162d3989b025fb76bd91029655d0572080a1e46c89c0
  + wallet-participant:03642377f11c4bb67ff29e9b48ab2733f67f0fa19aaed390333682172458fb59
provider_baseline_revision: wind-funds@eb12091819152fcec529f9453b48755f3aa2c999
  + accepted-38-path-scope:a7a66f1908f42d815ae66fed9618be5334c1e9d19ef6f84fb74a4e8a013835cd
  + core-transaction-scope:5c7748d3146bc7772fce4a5fdd0695b3764424027c993c0ea5433638f844a3c4
accepted_artifacts:
  core=2b2afa58d3fef5828a66446bdf1b5b6c4339ef882834a1da3fb3ad25a31ba39f
  transaction-face=21fb3e0b5b50647385861f368f91aebeacef29c5e13ac52fd5d848e9b177de6e
  lineage=UNTRACEABLE / E4=NO
write_whitelist: 本节、Metadata 与恢复入口
forbidden_scope: RED、生产/测试源码、API baseline、正式 PRD/DSL/系分/TDD、Consumer 仓、Git
```

`wallet-face` 不属于本 Inquiry 的接受版本元组；它只保留在 `RS-001` 总清册与 `VC-002`。本题不决定目标 DTO/API、收款责任、`PAID/REFUNDED` 门槛、退款跨主体责任、`UNKNOWN` 或确定失败后的再次执行。该 Inquiry 当时以 `Q-001~Q-003` 为已接受输入，`Q-004` 尚待 Owner；后续 `Q-004` 接受只关闭产品语义，不改变该版本化 Inquiry 的历史接受元组。

#### Shared Information Matrix 与 decision questions

这里的 `blocks_current_decision` 只回答“能否判定 `VC-001` 的资格”，不表示缺口已关闭。缺口若足以确定候选为 `blocked`，其信息状态可以 `ready-for-qualification`，但仍阻断契约承诺、RED 和实现。

| ID | 信息 / 决策问题 | 类型与 authority | 双方状态 | 缺口、Owner 与 `blocks_current_decision` |
| --- | --- | --- | --- | --- |
| `CI-I01` | Capte production 实际调用哪些命令/查询，如何解释和持久化返回引用？ | fact/evidence；Capte order source/POM，`consumer_revision` | Provider=`understood`；Consumer=`understood` | 无信息缺口；当前行为不等于 accepted contract；`false`。 |
| `CI-I02` | accepted core/transaction-face JAR 实际暴露什么，是否可追到本次 Provider source？ | evidence；JAR hash/CodeSource/manifest/javap | 双方=`understood` | Source lineage 与 E4 缺失；Provider Build + Capte Host Integration Owner；足以判定 `blocked`，`false`。 |
| `CI-I03` | `authorize/complete/reversal/refund` 返回的 String 是聚合、动作还是账本事实，单独能证明什么？ | contract question；Provider source/test E2 | Consumer=`disputed`；Provider=`answered` | Consumer 当前“非空即成功”与 Provider 分层事实冲突；Transaction + Capte Host Owner；`false`，结论为 conflict。 |
| `CI-I04` | 完成动作及原授权当前可由哪个事实稳定恢复？ | contract question；Provider lifecycle/detail + Capte refund source | Consumer=`disputed`；Provider=`answered` | main `referenceTransactionSn` 不是授权后继动作回链；命中 `P-SIM01-03`；`false`，结论为 conflict。 |
| `CI-I05` | `REJECTED/FAILED/UNKNOWN` 与同动作重放当前能证明什么？ | fact/unknown/dependency；Provider source/test + Q-003/Q-004 | 双方=`understood`；Inquiry 当时产品语义=`missing` | 该版 Inquiry 时 `Q-004` 尚未裁决；其后方案 A 已接受。当前仍缺 `P-SIM01-04` 所需宿主权威查询与恢复证据，候选继续 blocked。 |
| `CI-I06` | 收款责任与 `PAID/REFUNDED` 展示门槛是否可由本 Inquiry 推导？ | dependency；P-SIM01-01/02 | 双方=`understood` | 明确 out-of-scope 且 blocking；Order/Payment/Commercial 与事实 Owner；`false`，不得偷接。 |
| `CI-I07` | 哪个窄 AC 可在不依赖上述 PENDING 时进入 RED？ | decision question；双方 reconciliation | 双方=`disputed` | 当前无法冻结同时忠实于 Provider 与 Consumer 的动作完成/退款 AC；最早恢复项为 P-SIM01-03；`false`，资格结论为 blocked。 |

`Information Readiness=READY_FOR_QUALIFICATION_DECISION`：所有问题均已有当前证据，或已明确为带 Owner、停止条件和恢复证据的缺口；因此可以作出 `blocked` 资格判定。它不表示 `READY_FOR_CONTRACT/RED/IMPLEMENTATION`。

#### Provider Evidence Response

```text
inquiry_id: CI-RS001-CAPTE-WALLET-001
topic_revision: topic-r1
information_revision: info-r1
consumer_revision: capte-domain@6de70922f2565ce1f7ff036d457c29f22cdab829
  + root-pom:bcbf453482f6846f1a1763395990a7a5512aa9c028fe91db1fc4ceac2cf23350
  + order-pom:69c4a20c4ac1bc644713162d3989b025fb76bd91029655d0572080a1e46c89c0
  + wallet-participant:03642377f11c4bb67ff29e9b48ab2733f67f0fa19aaed390333682172458fb59
provider_revision: wind-funds@eb12091819152fcec529f9453b48755f3aa2c999
response_revision: response-r1
provider_baseline_revision: wind-funds@eb12091819152fcec529f9453b48755f3aa2c999
  + accepted-38-path-scope:a7a66f1908f42d815ae66fed9618be5334c1e9d19ef6f84fb74a4e8a013835cd
  + core-transaction-scope:5c7748d3146bc7772fce4a5fdd0695b3764424027c993c0ea5433638f844a3c4
accepted_artifact_revision:
  core=/Users/wuxp/.m2/repository/com/wind/funds/wind-funds-core/1.0.0-SNAPSHOT/wind-funds-core-1.0.0-SNAPSHOT.jar
    + sha256:2b2afa58d3fef5828a66446bdf1b5b6c4339ef882834a1da3fb3ad25a31ba39f
    + FundsAccountId CodeSource:same-file-url
  transaction-face=/Users/wuxp/.m2/repository/com/wind/funds/wind-funds-transaction-face/1.0.0-SNAPSHOT/wind-funds-transaction-face-1.0.0-SNAPSHOT.jar
    + sha256:21fb3e0b5b50647385861f368f91aebeacef29c5e13ac52fd5d848e9b177de6e
    + FundsAuthorizationTransactionService CodeSource:same-file-url
evidence_fingerprint: 90b12f7b83fbeb11d733bd54473fd7cf358d091c9e845eba1d634098b98068c1
supersedes: N/A
evidence_level: E2 source/current test source + resolved/loaded artifact partial；未 fresh 执行测试，不是 E3/E4
```

`evidence_fingerprint` 的复算规则：以下 canonical manifest 按当前顺序使用 UTF-8，每项写成 `key=value` 并以单个 LF 结尾，不排序、不加空行，然后执行 `shasum -a 256`。绝对 JAR 路径属于本机接受制品 origin；CodeSource 的 `same-file-url` 表示隔离探针回读与该绝对路径一致，不外推为 Capte Spring runtime。

```text
inquiry_id=CI-RS001-CAPTE-WALLET-001
topic_revision=topic-r1
information_revision=info-r1
consumer_head=6de70922f2565ce1f7ff036d457c29f22cdab829
consumer_root_pom=bcbf453482f6846f1a1763395990a7a5512aa9c028fe91db1fc4ceac2cf23350
consumer_order_pom=69c4a20c4ac1bc644713162d3989b025fb76bd91029655d0572080a1e46c89c0
consumer_wallet_participant=03642377f11c4bb67ff29e9b48ab2733f67f0fa19aaed390333682172458fb59
consumer_order_configuration=a70a305bfeafb5abaa329294f0ca3c1db9bdfcec97c5a5388bac2a6ca931e469
consumer_payment_service=4d472d4576cc66607ed4ca6acece08b11ee01ff13429017d6ea23cd9bc9fd8f9
provider_head=eb12091819152fcec529f9453b48755f3aa2c999
provider_38_path_scope=a7a66f1908f42d815ae66fed9618be5334c1e9d19ef6f84fb74a4e8a013835cd
provider_core_transaction_scope=5c7748d3146bc7772fce4a5fdd0695b3764424027c993c0ea5433638f844a3c4
response_revision=response-r1
core_artifact=/Users/wuxp/.m2/repository/com/wind/funds/wind-funds-core/1.0.0-SNAPSHOT/wind-funds-core-1.0.0-SNAPSHOT.jar|2b2afa58d3fef5828a66446bdf1b5b6c4339ef882834a1da3fb3ad25a31ba39f|CodeSource:same-file-url
transaction_face_artifact=/Users/wuxp/.m2/repository/com/wind/funds/wind-funds-transaction-face/1.0.0-SNAPSHOT/wind-funds-transaction-face-1.0.0-SNAPSHOT.jar|21fb3e0b5b50647385861f368f91aebeacef29c5e13ac52fd5d848e9b177de6e|CodeSource:same-file-url
source_service=f21a41baffdbfcb1ffecfd857fe5c72286a4de00d4435d792322cc4acd0ae143
source_query_service=97eaf2fc45b1608db08dfa8f455d872917a4e4a547a2e2338f222a4121124d3e
source_transaction_dto=8b6f4ee64b68e43aea71b78dfc2a220b6ac5762277841ede7b1937dffa2c7368
source_detail_dto=e665d6418c7520382cb0e4adebaf224876763153207c1966a7cb6dbeff2e6ec2
source_command=e08e62667cc34b458593ea8547a91d503db4e6bd577de3090336fa6b27215026
source_orchestrator=e127731d5e840cd21fcc417bc41f1e739dfb7348f328f3630381a1778c54ace7
source_lifecycle=442e14ee1b7830bba3d466c234f276c2ff7fb1991cbc064b7a26e10314fd7b5a
source_query_impl=c12bc7df0a97db2f25aa6b53d41de5c1ec81f097be38ec5b54239052105dfcbd
test_authorization_flow=af371ee1f5eadf7d420e44010422be98c9faa240ec273c498d1a67fbb507285a
test_flow_support=0c13420f7f20ae39abbd2f6809bfdb6adb351dc9e12c2d9e094b5af08de30bf1
test_schema=f80385fd70f1c3ece36dd5c63dc47a930b2dc3548ab04b2d5058ec4c7ffa3969
```

| Question | Response | 当前证据结论 | 不能外推 / remaining gap |
| --- | --- | --- | --- |
| 返回 String 的身份与完成保证 | `supported` for identity；`conditional` for result | 编排器返回生命周期 `transactionSn`，不是账本流水。授权后继动作复用原授权 aggregate，因此 complete/reversal/auth-refund 返回同一主流水，不是动作唯一流水。 | 非空 String 不证明本动作 detail 成功、账务已过账或余额已投影。 |
| 主事实与动作事实查询 | `supported / conditional` | main query 查询授权聚合与累计；details 承载 `businessScene/businessSn/eventType/state/ledgerTransactionSn/referenceDetailSn/referenceLedgerTransactionSn`。 | public query 没有按授权后继业务键直接查询动作 detail 的已接受契约；Host 不能只凭 main 状态判断某次动作。 |
| `referenceTransactionSn` 定位原授权 | `gap` for Consumer usage；`conditional` for explicit authorization reference | lifecycle saver 对授权后继直接找到原授权主交易并追加 detail；原引用存在 detail，命令侧要求显式传 `authorizationTransactionSn`。 | 原授权 main DTO 的 `referenceTransactionSn` 不是授权后继动作回链；Capte 当前反查假设不成立。 |
| 账务动作证据 | `conditional` | 有 RouteLeg 的成功动作将 ledger transaction sn 写入匹配的 participant details；主 DTO 不承载该字段。拒绝或零 leg 可为空。 | 单个 detail、null 或主状态均不能独立证明完整动作；目标查询/API 本轮不设计。 |
| `REJECTED` | `supported` | 拒绝授权可以返回同一类 transactionSn，但 detail/main 为 REJECTED，零 RouteLeg/posting/entry/balance；相同摘要重放原拒绝事实。 | transactionSn 存在不等于动作成功或入账。 |
| `FAILED` | `conditional` | posting rejection 可在 `noRollbackFor` 下耐久化 FAILED detail 且零 ledger；普通 RuntimeException 仍受事务回滚影响。稳定 main 可与后继 FAILED detail 并存。 | 单词 FAILED 不证明零资金、可重试或聚合失败；不得由本题批准合成状态。 |
| `UNKNOWN`、后继重放与恢复 | `gap` | 当前 main/detail 无 UNKNOWN 语义；main 业务查询命中授权主业务键，不能据此按后继业务键恢复动作。 | `Q-004` 后续已接受 UNKNOWN 沿原身份恢复的产品语义；当前仍命中 `P-SIM01-04` 宿主取证缺口。PROCESSING 不得冒充 UNKNOWN，换键或新执行无授权。 |
| accepted artifact / E4 | `gap` | core 与 transaction-face JAR 已 resolved/隔离加载，但无 Provider revision；Snapshot DTO 使用 `status` 类型，当前 checkout 使用 `state` 类型，存在确定漂移。 | 无 source -> binary -> Consumer resolved/loaded -> real Bean/joint schema/tx；不能把 javap/CodeSource 当 E4。 |
| 目标 API、责任、展示和失败后执行 | `out_of_scope` | 本版 Response 形成时 `Q-004` 与 `P-SIM01-01~04` 均待决；其后 `Q-004`、`P-SIM01-01-D` 与 `P-SIM01-03` 已接受，`P-SIM01-01-HOST/02/04` 及 `P-SIM01-03-E4` 仍待闭合。 | 不得从当前实现反推产品取舍。 |

关键 source anchors：`FundsAuthorizationTransactionService.java:34-68`、`FundsTransactionQueryService.java:25-149`、`DefaultRoutedFundsInstructionOrchestrator.java:71-128`、`FundsTransactionCommandServiceImpl.java:203-359`、`DefaultFundsInstructionLifecycleSaver.java:241-325,395-419`、`DefaultFundsTransactionQueryService.java:58-163,313-324`。当前测试源码锚点为 `FundsAuthorizationTransactionFlowTests.java:175-350,2350-2456,2550-2867` 与 `FundsTransactionFlowTestSupport.java:824-850`；本轮未执行测试。

Provider Agent 另报的全仓 48-path fingerprint 不属于本 Inquiry 接受的 38-path scope。主笔已复算 accepted scope 仍为 `a7a66f...`、core+transaction scope 仍为 `5c7748d...`，因此不以无关全仓变化误判本 Inquiry 为 stale。

#### Consumer Reconciliation

```text
result: conflict
accepted_topic_revision: topic-r1
accepted_information_revision: info-r1
accepted_consumer_revision: capte-domain@6de70922f2565ce1f7ff036d457c29f22cdab829 + frozen POM/source hashes
accepted_provider_revision: wind-funds@eb12091819152fcec529f9453b48755f3aa2c999 + a7a66f... + 5c7748d...
accepted_response_revision: response-r1
accepted_evidence_fingerprint: 90b12f7b83fbeb11d733bd54473fd7cf358d091c9e845eba1d634098b98068c1
```

- `confirmed`：Capte 生产调用面、POM、core + transaction-face origin/hash/CodeSource、当前无 funds impl/联合 schema/事务和 Provider aggregate/detail 分层事实。
- `conflict`：Capte 以非空返回判动作成功，把完成返回值持久化为 `outTransactionSn`，再读取该 main DTO 的 `referenceTransactionSn` 恢复原授权；Provider 当前事实却是后继动作复用原授权 aggregate，main reference 不是动作回链，动作结果位于 detail。
- `conflict`：Capte 未按动作身份检查 detail state、participant 完整性、ledger reference 或余额证据；reversal 返回值被丢弃，mutation exception 又被压为 failure。当前行为不能成为 Q-003 所要求的完成/恢复契约。
- `confirmed gap`：Snapshot 与 checkout 漂移且 lineage 不可追踪；Provider 无 UNKNOWN/按后继业务身份恢复的 accepted contract。已知缺口正是本 Inquiry 原问题，不构成 `reopen`；接受版本未变化，不构成 `stale`。

#### `VC-001` Slice Card 资格判定

```text
candidate_id / slice_status: VC-001 / blocked
information_readiness: ready_for_qualification_decision
dependent_accepted_decisions: Q-001~Q-004; P-SIM01-01-D; P-SIM01-02-A; P-SIM01-03
blocking_decisions: P-SIM01-04 host authoritative query/recovery evidence; P-SIM01-01-HOST; P-SIM01-02-HOST
excluded_fail_closed: external rail only (NOT_APPLICABLE)
host_E4_blockers: P-SIM01-03-E4; traceable core + transaction-face artifact; real Bean; joint schema/transaction; SIM-01 host assertions
external_E5_blockers: none for the internal-wallet scope
dirty_overlap: core/api-baseline/stable-api.txt + FundsTransaction/FundsTransactionDetail/FundsFrozenOrder = stop-owner
RED: NOT FROZEN
Execution Grant: NO
write_whitelist: none beyond this Change Spec
forbidden_scope: target API/DTO, RED, production/test/Consumer code, API baseline, Git
rollback: no runtime or code mutation occurred
stale: accepted source/POM/JAR/CodeSource/38-path or core+transaction fingerprints change
```

`P-SIM01-02-A` 已接受严格内部资金闭合门槛，`P-SIM01-03` 已接受动作证据与退款原事实不变量，但 `VC-001` 仍为 `blocked`：`P-SIM01-01-HOST`、`P-SIM01-02-HOST` 与 `P-SIM01-04` 尚未闭合，`P-SIM01-03-E4` 尚未证明稳定动作事实的真实供给与消费，且 accepted artifact 与 dirty overlap 仍未形成可执行门禁。不得据此直接设计新查询 API、修改 Consumer 或保护当前含糊 main 引用假设。

#### Checker 结论与唯一下一入口

独立 Checker 回读并复算原始 source、Capte POM/JAR、完整接受版本元组、canonical evidence manifest 和 dirty scope 后，结论为 `PASS / 0 P0-P2`。该 PASS 只准出 Inquiry 包和 `VC-001=blocked` 判定，不等于 `eligible`，不批准 RED、目标 API/DTO、生产/测试/Consumer 代码或 Git。

本 Inquiry 准出时的唯一下一入口为 `P-SIM01-03_OWNER_GATE / ACTION_FACT_AND_REFUND_PROVENANCE`；其 Owner 后续已接受方案 A，当前结果与后续入口见 `8.4` 和本文件恢复入口。该裁决仍不建立 RED，不选择主交易/明细的目标物理形态，也不修改任何实现。

### 8.4 `P-SIM01-03` 动作事实与退款原事实决策包

#### 决策身份与唯一命题

- **决策包 ID**：`DP-PSIM01-03-ACTION-REFUND-PROVENANCE`。
- **状态**：`accepted / ACCEPTANCE_CHECKER_PASS`；当前只接受产品/公共契约不变量，不表示 `VC-001 eligible`。
- **Owner**：Transaction Owner 主签；Capte Host Integration Owner 会签宿主可理解、可保存和可恢复性；Order/Payment Owner 只确认本地业务引用的归属，不在本题裁决 `PAID/REFUNDED` 门槛。
- **唯一命题**：授权后的 complete、reversal、refund 等资金动作，必须具有相对授权生命周期可唯一定位、重启后可恢复的耐久动作事实；Owner 需选择该稳定语义是否采用“生命周期根 + 独立动作事实”，而不是继续以含糊主流水证明动作完成。
- **交接原因**：`CI-RS001-CAPTE-WALLET-001` 已证明 Provider 当前返回授权 aggregate 流水，而 Capte 把它当动作流水并从 main reference 反查原授权；双方现状契约冲突，不能直接冻结 RED。

#### 三个候选的同构比较

| 方案 | 动作身份与原授权关系 | 完成证据 / 返回引用含义 | 退款 provenance 与累计上限 | 部分动作、异常与 fail-closed | 切换成本与 Owner 风险 |
| --- | --- | --- | --- | --- | --- |
| **A：生命周期根 + 耐久独立动作事实（推荐）** | authorize 建立可解释的生命周期根；每次 complete、reversal、refund 都有不可变、可单独识别和引用的动作事实，并显式关联授权根或原完成事实。动作事实可以物理落在明细、事件或独立主事实，本题不固定载体。 | 生命周期根和返回引用只负责定位，不单独证明动作成功。Host 必须保存根引用和业务已接受的动作事实引用；动作是否完成由该动作所需的局部结果、资金效果及证据引用共同证明。 | refund 精确引用一个或多个已证明成功且仍可退的 complete 动作事实，并可沿链追到授权根；对每个原完成事实分别满足 `0 <= 已确认退款累计 + 本次退款 <= 该原完成事实的已完成可退总上限`。 | 只有被权威证明的完成、释放或退款资金效果才计入对应累计。REJECTED/FAILED 标签、缺证或冲突本身既不证明应计入，也不证明零效果；任何已证明的局部效果必须保留并占用对应上限，未闭合时 fail-closed/manual。`Q-004` 已接受 UNKNOWN 沿原身份恢复、确定失败后的受控新 Attempt 和部分效果禁止整单重跑；`P-SIM01-04` 仍阻断宿主自动恢复，历史动作无法唯一定位时停止自动退款并人工处理。 | 中等。贴近当前 Provider 的 aggregate/action 分层，避免强制翻转全部事实模型；但必须把 action fact 升格为稳定公共证据，Consumer 不能再只保存同一个 aggregate String。若只能依赖“最新动作”，A 退化为 C，不能接受。 |
| **B：每个动作都强制为独立主事实** | authorize、每次 complete、reversal、refund 都拥有独立主事实；complete/reversal 显式引用授权事实，refund 显式引用成功完成事实；授权链另保留累计关系。 | 每个动作主引用只定位该动作；“存在主事实”仍不等于完成，仍须按该动作的局部结果与资金效果证据判断。 | 与 A 相同，refund 引用一个或多个原成功完成事实，按各原事实的币种、责任、原 route 和剩余可退额约束。 | 与 A 相同；拆成主事实不得绕过累计、把 attempt 当经济事实或用 UNKNOWN 生成第二次资金效果。 | 高。宿主直观，但强制 Provider 翻转当前授权聚合结构，增加历史映射和累计归集成本；VCC/钱包多次完成仍需额外聚合。本方案可作为后续物理实现选择，不推荐成为跨场景强制产品模型。 |
| **C：保留含糊 main SN / 当前状态（拒绝候选）** | complete/reversal/refund 共用或返回语义不明的 main 流水，没有可耐久引用的动作身份。 | 非空 String、main 当前状态或累计摘要被调用方当动作完成；无法区分某次动作及其账务证据。 | 无法可靠引用原成功完成事实，只能猜授权根、main reference 或当前配置；无法安全计算多次部分完成和退款累计。 | 只能对正向/逆向能力整体 fail-closed/manual；否则异常、部分动作和重复处理会产生不可证明或重复资金风险。 | 表面迁移最少，实际把成本转给人工查账、退款停单和对账争议；不满足 Q-003，也不能关闭 P-SIM01-03。 |

#### 推荐答案与稳定不变量

推荐接受 **A 的稳定语义**，拒绝 C；B 只保留为后续系分可选择的更强物理形态，不作为公共层强制模型。A 不是批准当前 Provider 的 main/detail 结构，满足以下全部条件才成立：

1. 生命周期根与动作事实是两种明确证据身份；聚合可承载累计摘要，但每次资金动作必须有独立、不可变、可唯一定位且可被 Host 耐久引用的事实。
2. 每个动作事实至少能证明或关联：稳定动作身份、动作类型、原授权或原完成关系、金额币种、局部领域结果、资金效果证据/引用，以及必要的顺序或版本；具体字段名和载体后置。
3. 动作完成只表示本场景为该动作选定的正交证据维度按局部偏序闭合。授权 aggregate 状态/累计、main `transactionSn`、非空返回 String、RouteSnapshot、单个 action fact 或单笔账务引用，任一都不能单独冒充完整动作完成。
4. complete 只消费已成功授权事实的剩余可完成范围；完成前的 reversal/release 只释放尚未完成的授权余量；refund 只能引用真实、已证明成功且仍可退的原 complete 事实，不能只引用授权根或含糊 `outTransactionSn`。
5. 多次部分完成或退款必须按原完成事实分别保留来源和剩余上限，不跨动作、币种、责任或原 route 汇总猜测，也不按当前账户绑定、责任规则或配置重算历史。
6. 原事实不可覆盖。refund/reversal 追加关联新事实并保留因果链；REJECTED、FAILED 标签、缺证或引用冲突本身既不证明应计入累计，也不证明零资金效果。任何权威证明已形成的局部资金效果都必须保留并占用对应累计上限；证据未闭合时停止自动动作并转人工。
7. 历史记录只有在动作 provenance 唯一可证时才允许受控映射；无法唯一证明时保持 fail-closed/manual，不用当前代码、余额或“最新 detail”补造历史。

#### 跨场景校验与不外推

- 商品钱包当前 aggregate + detail 只是 A 的一种现状证据，不是目标物理结构。
- VCC 可以由卡交易聚合加不可变 Event 体现 A；ACH 可由 PaymentInstruction、receipt 和独立 FundsTransaction 共同体现；payout 可由 PayoutOrder、receipt 与独立 completion/rollback 资金事实体现。各场景不要求同表、同状态或同一 API。
- 主聚合只证明本层汇总；动作事实只证明本动作及其本层结果；FundsTransaction 不自动证明 Ledger/Balance；Ledger 不证明业务完成或外部终局；Balance 不证明动作来源；Reconciliation/Gate 不制造任何动作事实。
- VCC issuer finality、ACH return/NOC/reversal、payout beneficiary finality/RETURNED、清结算 Gate 和对账容差均保留各自 Owner PENDING，不进入本题。

#### 明确不裁决与 red lines

本题不裁决 Java interface/DTO/query/table/entity/字段、索引、序列化、事务或迁移实现；不裁决 Intent/Attempt、幂等 key/hash、timeout/UNKNOWN 恢复或确定失败后再次执行；不裁决 payer/payee/账户/商业责任和 `PAID/REFUNDED` 展示门槛；不关闭 source-to-binary、real Bean、联合 schema/事务或 E4。

以下任一项均为硬失败：main/aggregate/返回 String/单个 action fact/单笔 ledger reference 独自证明动作完成；refund 只引用授权根；按最新 detail 或当前配置猜原事实；累计超过已完成可退上限；把 A 偷换成当前数据结构签收；Owner 未回答就标 accepted；Owner Gate PASS 自动升级为 `VC-001 eligible`、RED、API、代码、测试或 Git 授权。

#### Owner 回答与后续

- **Owner 回答**：接受 A。
- **当前结论**：`accepted`。采用“生命周期根 + 耐久独立动作事实”的稳定语义；拒绝 C，B 只作为后续系分可选择的物理形态，不成为跨场景强制模型。
- **接受后的下一输入**：独立 Checker 已复核接受范围和两份产品状态；其后 `Q-004`、`P-SIM01-01-D` 与 `P-SIM01-02-A` 也已获 Owner 接受。当前入口见 Metadata 与恢复入口。`P-SIM01-01-HOST`、`P-SIM01-02-HOST`、`P-SIM01-04`、source -> binary -> Consumer resolved/loaded 与 real Bean/joint schema/tx 仍分别保持阻断。只有对应 Slice Card 再判 `eligible` 且取得 Execution Grant 后，才允许冻结 RED。
- **重开条件**：Consumer/Provider 权威版本变化、Owner 修改动作/退款因果语义，或跨场景证据证明 A/B 无法满足累计和原事实约束。

### 8.5 `P-SIM01-01-D` 商品钱包收款责任路由裁决

- **状态**：`accepted / ACCEPTANCE_CHECKER_PASS`。
- **Owner 回答**：接受 `P-SIM01-01-D`。
- **最终结论**：公共能力不固定商品钱包的单一商业模式。宿主必须在明确的 `tenant + business scene + merchant（或无 merchant）+ 责任规则版本` 范围内，从 A 平台自营、B 商户经济直收、C 平台代收清算中选择且每笔冻结唯一模式。D 是选择和冻结规则，不是第四种账户、资金路径或完成状态。
- **共同准入**：授权前必须证明 payer 责任与准入账户、所选模式要求的 payee/中间责任、currency、责任规则版本和原 route；C 还须冻结商户经济受益权。无法唯一命中、多个模式冲突或任一责任证据缺失时，允许保留无 legs 的拒绝解释事实，但无可执行 RouteLeg/posting/LedgerEntry/余额变化或价值转移。
- **逆向与下游**：A/B/C 分别沿原平台、原商户或原代收/商户受益责任及原事实处理；清分、结算、出款和对账是否适用继续服从该模式已冻结的下游责任，不由订单 `PAID` 外推。
- **当前能力边界**：Capte 当前没有 seller/merchant/payee 责任快照、D 选择结果、责任规则版本和收款账户准入证据；`payeeId=capte` 与当前 Provider route 均不能替代。该缺口转为 `P-SIM01-01-HOST`，在关闭前阻断钱包授权和完整价值转移。
- **不裁决**：法律/会计定性、具体账户类型、Ledger Profile、posting matrix、Java/API、幂等键、事务和轨道状态。
- **下一输入**：独立 Checker 准出后进入 `P-SIM01-02 / PAID_REFUNDED_COMPLETION_GATE`；宿主自动执行仍受 `P-SIM01-01-HOST`、`P-SIM01-03-E4` 与 `P-SIM01-04` 阻断。

### 8.6 `P-SIM01-02-A` 商品钱包业务声明门槛裁决

- **状态**：`accepted / ACCEPTANCE_CHECKER_PASS`。
- **Owner 回答**：用户授权按推荐推进，接受 `P-SIM01-02-A`。
- **最终结论**：纯内部钱包采用严格内部资金闭合作为订单最终声明门槛。业务目标和金额币种、成功 complete/refund 动作事实、对应平衡账务以及本次 D 责任范围要求的指定责任账户余额效果必须分别闭合并可沿稳定引用重查，才可声明 `PAID/REFUNDED`。这里的余额效果只覆盖本次内部支付动作及 D 已冻结的责任，不等待外部 finality、后续清分/结算/出款或 reconciliation。
- **部分与异常**：部分退款只声明已权威闭合的累计金额；订单可退款范围全部闭合后才可声明 `REFUNDED`。任一必需维度为 `UNKNOWN`、冲突或不可查询时保持结果确认中；资金事实已形成而账务、余额或业务投影未闭合时只修复缺失维度，不重做资金动作，也不提示用户重复支付或退款。
- **拒绝候选**：不采用“账务闭合但余额可滞后”的 B，也不接受 transaction/main String/Bill 或参与方布尔结果单独推出订单最终状态的 C。
- **当前能力边界**：Capte 当前只以 participant 布尔结果和可选流水传播 Bill/Order 状态，尚不能消费方案 A 所需的动作、账务和余额证据；该缺口转为 `P-SIM01-02-HOST`，不因产品裁决而关闭。
- **不裁决**：具体余额桶、API/DTO、状态枚举、同步/异步投影、事务、外部轨道、清结算、出款和对账关账。
- **下一输入**：独立 Checker 准出后进入 `P-SIM02-01 / COMPOSITE_PAYMENT_PARTIAL_RESULT_POLICY`；`VC-001` 仍受 `P-SIM01-01-HOST`、`P-SIM01-02-HOST`、`P-SIM01-04`、`P-SIM01-03-E4`、artifact lineage 与 dirty overlap 阻断。

### 8.7 `P-SIM02-01-A` 组合支付部分成功父计划策略

- **状态**：`accepted / ACCEPTANCE_CHECKER_PASS`。
- **Owner 回答**：用户明确接受 `P-SIM02-01-A`。
- **最终结论**：父支付计划采用“恢复优先，超出冻结边界后放弃并逆向”。一腿已有权威成功事实时不覆盖、不清空该事实；在已冻结履约时限、业务授权与 Q-004 准入内，只恢复未完成腿；超出任一边界后，放弃父支付并由所有成功腿 Owner 追加引用原成功事实的逆向。
- **UNKNOWN 与退出门槛**：任一腿 `UNKNOWN` 时只沿原腿身份查询，不新执行、cancel、refund 或补偿。只有所有必需腿按各自正向门槛闭合，父计划才可声明完整支付；只有所有已成功腿按各自 Owner 门槛完成所需逆向，父计划才可声明已放弃/取消。
- **未选方案**：不选 B“确定失败后立即放弃并逆向”或 C“默认人工接管”。当 A 的冻结边界、责任或权威事实不可证时仍 fail closed/manual，不得把 C 作为绕过证据的兼容通道。
- **当前能力边界**：当前 Capte 不能耐久恢复策略版本、成功腿、未完成腿、逆向/补偿事实及其因果关系，且父级 failure 会覆盖局部结果。该宿主缺口转为 `P-SIM02-01-HOST`；Coupon 动作权威已由后续 `P-SIM02-02-A` 接受，真实宿主消费与恢复继续由 `P-SIM02-02-HOST` 阻断。
- **不裁决**：具体 API/DTO、存储、事务/Saga、技术顺序、重试次数、Coupon 局部终态、宿主 E4 或实现授权。
- **后续结果**：`P-SIM02-02-A` 已由 Owner 接受并通过接受 Checker；该结果不改变 `VC-001=blocked`、`RED=NOT FROZEN` 或 `Execution Grant=NO`。

### 8.8 `P-SIM02-02-A` Coupon 动作权威裁决

- **状态**：`accepted / ACCEPTANCE_CHECKER_PASS`。
- **问题**：Order 在 Coupon confirm、release/cancel、return 的响应丢失、异常、重启或迟到冲突下，应消费何种权威事实，才可执行 `P-SIM02-01-A`。
- **一手事实**：Coupon 域当前源码已有不可变动作流水以及按支付/退款原事实查询决策的能力，结果对象也带动作引用；Order 侧只消费同步 `success`，丢弃 confirm、release/cancel、return 引用，且 confirm 异常会立即调用 release。该事实只达到源码 `E2`，不证明宿主运行闭合。
- **Owner 回答**：用户明确接受 `P-SIM02-02-A`。
- **最终结论**：Coupon Owner 为 lock、confirm、release/cancel、return 提供耐久、可唯一定位并带原事实关系的动作事实；Order 只保存引用和本地消费结果，不复制权威。B/C 不采用。本结论与物理载体正交，不新增 FundsTransaction 或公共资金抽象。
- **四动作边界**：lock 建立券占用原事实；confirm 只消费可确认 lock；release/cancel 只释放尚未被 confirm 消费的 lock；return 只引用真实成功 confirm 并形成独立逆向事实。每个动作都必须有稳定身份、券价值/范围、局部领域结果、业务效果、原事实关系与证据引用；这是产品合同，不是字段表。
- **UNKNOWN 与逆向**：同步异常只说明本次响应未知，必须沿原动作身份查询。`FAILED/REJECTED` 标签、查询未命中或日志不存在本身不证明券效果为零，也不自动授权相反动作。任一必需动作未知、冲突或查询不可用时，零新 Coupon 动作、零钱包 refund/补偿，父计划保持未决。return 不能只引用 lock 或当前聚合状态。
- **当前门禁**：接受 A 只关闭 Coupon 权威选择；Order 的动作引用保存、父计划恢复以及真实 Bean/schema/事务/宿主验收转为 `P-SIM02-02-HOST`，并继续受 `P-SIM02-01-HOST` 和后续集成证据阻断。
- **不裁决**：Java/API/DTO、表/字段、状态枚举、事务/Saga、消息、重试次数、父计划存储、G1/DSL/系分/TDD/实现或 Git。
- **下一输入**：该阶段曾进入 `P-SIM03-01 / BENEFIT_FUNDING_RESPONSIBILITY`，现已完成接受与复核；当前唯一入口见 Metadata 与恢复入口，仍不得进入 RED/代码。

### 8.9 `P-SIM03-01-D` Benefit 成本与承接责任路由裁决

- **状态**：`accepted / P-SIM03-01-D / ACCEPTANCE_CHECKER_PASS`。
- **问题**：100 CNY 券核销模拟中平台承担 60、商户承担 40 时，两项成本责任如何形成经济价值效果，谁承接 Benefit，哪些项属于内部资金 contribution。
- **一手事实**：Capte 当前只证明活动配置可把出资角色、资金性质、比例和 receiver 分开保存；60/40 来自测试模拟，生产 settle 尚未提供完整成本/承接责任，退款又会重读当前活动配置。Fincone 的 `WF-FIN-BENEFIT-MP-011` 只签收商品订单 `SPECIFIED` 的窄化设计：真实订单 payee 是经济价值承接责任来源，每个适用资金出资项独立留痕和原路退款；该 child contract 不替 Finance/Accounting/Risk 决定商户 40 是否为资金转移，也不证明运行能力。
- **共同快照**：冻结 tenant、商品订单/核销原事实、业务场景、币种、责任规则版本、平台/商户成本金额、分摊/舍入、经济承接责任和每项是否产生内部资金影响。成本方、卖方、收款方和 Benefit 承接方保持正交。
- **Owner 回答 D**：用户明确接受 `P-SIM03-01-D`。公共能力不固定某一模式；宿主按 `tenant + 产品类别/商品订单 + business scene + merchant（或无 merchant）+ currency + Benefit 责任规则版本` 在 A/B/C 中显式选择，每次核销只能冻结一种模式。D 不是第四种经济或资金模式，不允许同一责任项在 A/B/C 间叠加、降级或事后切换。
- **候选 A（推荐）**：平台 60 形成独立资金 contribution，补足订单已冻结经济受益责任；商户 40 是经营折让，直接减少原应收并形成非资金业务事实，不创建第二笔内部价值转移。退款分别沿平台原 contribution 和商户原折让决策恢复。
- **候选 B（条件）**：平台 60 与商户 40 都是经 Finance/Accounting/Risk 签收的独立资金责任，分别补足订单已冻结经济受益责任并形成两笔 contribution。商户成本方与承接方属于同一商户时，必须证明是不同经济责任且没有无意义自转或重复确认应收。
- **候选 C（零资金）**：60/40 只表达营销成本分配，本场景零 Benefit 资金影响；选择 C 时 `SIM-03` 的实时资金 settle 标为 `NOT_APPLICABLE`，订单价格/应收由订单商业合同另行解释，不能一边选择 C 一边保留资金调用。
- **共同逆向/partial**：只按权威闭合事实计入完成成本；任一项 `UNKNOWN/FAILED/PROCESSING` 不覆盖其他已知事实。资金退款只引用自身真实成功 contribution，非资金折让不伪造资金退款；70/30 新配置不得改变原 60/40 责任快照。
- **不可外推**：Benefit settle、待清分应收和资金账务分别不能证明 clearing batch、settlement、payout、商户可用、银行到账或 reconciliation；具体账户类型、账目代码、posting、会计分录、Java/API/表/事务均不在本题。
- **未关闭项**：当前 Capte 通用模块仍不能替未来真实宿主产出/消费 D 路由结果和完整责任快照，`P-SIM03-01-HOST`、`P-SIM03-02-HOST`、`P-SIM03-03` 与 `VC-002=blocked` 保留并延后到首个真实 Consumer。`plan-r2.68/r2.69` 只关闭当前公共制品 + Capte 集中测试切片的 artifact lineage、real Bean/schema/tx、单项 `SPECIFIED` E4 与公开版本解析编译；不要求 `capte-domain` 提供生产数据库、部署装配或 L4，也未批准真实 Consumer enablement、RED、代码/API/Git。
- **当时下一输入**：接受 Checker 准出 D 后进入 `P-SIM03-02_DECISION_PACKAGE`；该决策包及 Owner 选择现已完成，当前唯一入口见 Metadata 与恢复入口。

### 8.10 `P-SIM03-02` Benefit 部分成功与恢复政策

- **状态**：`accepted / P-SIM03-02-R-A / ACCEPTANCE_CHECKER_PASS`。
- **单一命题**：D 已为本次核销冻结责任模式后，所选模式内必需资金/非资金责任项出现 partial、UNKNOWN、确定失败或逆向未闭合时，父 Benefit 如何保留已知事实、继续/退出/manual，以及何时可声明完整完成或完整恢复。本题不重开模式、责任、金额或承接方。
- **事实基线**：Capte 当前按活动 funding rows 顺序执行资金 settle/refund，不表达异构资金/非资金责任项，不保存调用返回引用，用 `find...isPresent()` 冒充单项完成，并在退款时重读当前活动配置；同步异常会中断循环，外层也不能证明跨 Provider 原子回滚。Fincone accepted child scope 只支持逐资金项 handoff、原引用退款、累计和 UNKNOWN 原单查询，不替 Benefit Owner 决定父级 partial 策略。
- **共同合同**：以 D 冻结的责任项集合为权威；每项保留 Owner、资金/非资金类型、金额币种、舍入、承接责任、原事实、局部结果与效果证据。UNKNOWN 整组零新动作，只查原项；确定失败进入策略前还需证明零效果/责任闭合和不再迟到；成功项不覆盖、不清零、不重做。
- **同构候选**：`R-A` 恢复优先，在冻结时限、授权和 Q-004 准入内只处理未完成项，越界后逐项逆向成功项（推荐）；`R-B` 确定失败即退出，不恢复失败项，查清 UNKNOWN 后逐项逆向成功项；`R-C` 任一 partial/UNKNOWN/FAILED 即人工接管，不自动续作或逆向。三者均适配 `M-A/M-B/M-C`，其中 M-C 全程零资金。
- **完成与逆向**：全部必需责任项分别闭合才完整完成；所有已形成效果分别沿原事实逆向闭合、UNKNOWN 权威终结且未执行/零效果项有证据后才完整恢复。资金项分别遵守原金额、币种、责任、route 与累计上限；非资金项沿原折让/成本事实恢复。
- **验收矩阵**：PRD `5.18` 已实际给出共同情形表与 `M-A/M-B/M-C × R-A/R-B/R-C` partial 判别表：全成才完整完成；UNKNOWN 只查原项；配置漂移/重复恢复复用冻结事实；逆向未闭合不得完整恢复；M-A 资金/非资金分别恢复，M-B 逐资金项继续与累计，M-C 的资金动作和资金累计显式 `N/A`。每格均列允许动作、禁止动作、父层声明和人工出口。
- **Owner 结论**：用户明确接受 `R-A`，`R-B/R-C` 未选择。尚未执行项只可在冻结履约时限、业务授权和原计划边界内继续；已确定零效果失败项需要新 Attempt 时，还必须满足 `Q-004` 的旧 Attempt 局部终结、目标效果为零或责任闭合、不再迟到和重新授权。任一 `UNKNOWN/PROCESSING`、冲突或查询不可用时整组零新正向、零新 Attempt、零逆向；越界后逐项逆向所有已证明形成的效果。`M-C` 始终零 FundsTransaction/Ledger/Balance，资金退款与累计为 `N/A`。
- **停止线与当时下一输入**：`P-SIM03-01-HOST`、`P-SIM03-02-HOST`、`P-SIM03-03`、E4、`VC-002`、G1、RED、Execution Grant、API、code、test、Git 全部保持阻断。该阶段曾进入 `P-SIM04-01_DECISION_PACKAGE`；决策包现已通过 Checker，当前唯一入口见 Metadata 与恢复入口。

### 8.11 `P-SIM04-01` VCC 责任与授权累计决策包

- **状态**：`accepted / P-SIM04-01-A / ACCEPTANCE_CHECKER_PASS`。
- **单一命题**：只裁 PREPAID/SHARED 的内部责任配置，以及 authorize/complete/release/refund 的动作金额语义与累计上限；issuer authority/finality、overcapture、late clearing、Network Settlement、账户分类与 posting 全部不在本题。
- **事实冲突**：Provider 当前源码/测试 `E2` 支持授权根、后继动作、授权根级累计和 SHARED Credit/Funding 双责任，但 refund 只引用 authorization root，不能证明目标合同要求的逐 complete 分配与逐笔上限。Fincone VCC 文档仍为 `E1 / OwnerDecision=PENDING / NOT_STARTED`，支持通用增量 Event 模型，但其具体 30/50 样例是累计快照、最终 completed=50。候选 A 的 `Δ30+Δ50=80` 来自 Provider/SIM 产品候选，不是 Fincone 已确认数字事实。没有真实 VCC runtime Consumer 或 E4 证据，测试不能替 Owner 定契。
- **共同责任合同**：冻结 tenant、VCC/program/card、scene、currency、授权金额、责任模式/规则版本、Credit/Funding 关系、原 authorization、原 route 与适用范围。PREPAID 由 Funding 承担真实资金责任；SHARED 由 Credit 承担额度/应还责任、父 Funding 承担真实资金或结算责任。卡、holder、PaymentInstrument、IssuerAccount 不是内部余额主体；后继动作不按当前 binding 重算。
- **共同累计合同**：只消费权威闭合动作；`completed + released <= authorized disposition`。refund 分配并引用一个或多个真实成功 complete，逐原事实保持金额、币种、责任、原 route 和可退上限；release 不碰已完成部分，refund 不恢复授权可完成额度。拒绝零 legs/posting/entry/balance/declinedAmount；UNKNOWN 零新同类或相反动作，只查原动作，已证明 partial 必须保留并占用上限。
- **接受方案 A**：VCC/adapter 先把权威外部事实归一成不可变增量动作，资金公共层只消费该增量。内部模拟明确为 `authorize Δ100 -> complete Δ30 -> complete Δ50 -> release Δ20 -> refund Δ20`；最终 `completed=80, released=20, refunded=20, authorizationRemaining=0, refundableRemaining=60`。PREPAID/SHARED 均适用。Fincone 的累计快照 `30 -> 50` 若由 `P-SIM04-02` 签收，只能先归一成 `Δ30 + Δ20`，不得直接把快照 50 当公共层 `Δ50`；B 未选择但保留为历史比较证据。
- **候选 B**：消费经 `P-SIM04-02` 签收 sequence 的权威累计快照，每份快照及其可证明新增量仍形成不可变动作事实；30/50 表示 completed 从 30 到 50。最终 `completed=50, released=20, refunded=20, authorizationRemaining=30, refundableRemaining=30`。PREPAID/SHARED 均适用，但选择 B 必须同步改写 SIM-04 的 80 完成预期。
- **验收矩阵**：PRD `5.19` 已分别给出 A/B 的 100/30/50/20/20 目标合同演算，并覆盖 PREPAID/SHARED 全链、授权拒绝、两次 partial complete、release 超限、refund 错原事实/超限、timeout-after-effect、FAILED 标签但效果未知、重复/乱序、binding 漂移和卡关闭/过期后迟到动作；每格列允许/禁止动作、累计/责任、父层声明和 manual 出口。逐 complete refund、B 的快照归一和真实 Consumer 恢复均明确为当前能力 gap。
- **完成分层**：父层只分别声明授权可用/已处置范围、部分完成、释放、退款和证据未知；authorization accepted、动作事实、账务、余额、issuer Event、Network clearing/settlement 与争议互不证明。本题不批准新全局状态枚举。
- **Owner 与停止线**：VCC/Product + Funds Owner 已接受 A；Credit/Funding Account Owner 只确认准入与关系；issuer/processor Owner 仍在 `P-SIM04-02` 裁外部证据；Finance/Risk/Accounting 终审信用风险口径。逐 complete refund、adapter 归一、真实 Consumer、动作引用持久化、timeout-after-effect 恢复、artifact lineage、real Bean/schema/tx、HOST/E4/VC/G1/RED/Execution Grant 均未关闭。当前接受不批准 Java/API/DTO、表/字段、事务/Saga、状态枚举、账户类型、subject/posting matrix、rail mapping、实现、测试或 Git。
- **当时下一输入**：A 的接受范围准出后进入 `P-SIM04-02_DECISION_PACKAGE`；该包现已通过独立 Checker，当前唯一入口见 Metadata 与恢复入口，仍不得冻结 RED 或实现。

### 8.12 `P-SIM04-02` issuer 外部权威与 finality 决策包

- **状态**：`accepted / P-SIM04-02-D / ACCEPTANCE_CHECKER_PASS`。
- **单一命题**：只裁 VCC 边界在何种外部证据条件下可把 issuer/processor observation 认定为权威动作事实，并把 sequence/amount semantics 归一后交给 `P-SIM04-01-A`；内部账户、posting、Java/API/表/事务、厂商 rail matrix 与 Network Settlement 处置不在本题。
- **事实基线**：Fincone VCC 产品/系分是 `E1 / REVIEW_READY / OwnerDecision=PENDING / NOT_STARTED`，目标 issuer source/identity/sequence/amount/finality 和测试执行证据缺失；Provider 仅能执行已由上游裁决的内部资金动作，不拥有 issuer 协议、authority 或 VCC runtime Consumer。卡交易 source、Event、FundsProcessRef、聚合与 Network Settlement 分层只能作为候选设计输入，不能冒充运行能力。
- **共同合同**：delivery/observation、权威外部 action fact、规范化不可变 delta、内部 Funds/Ledger/Balance effect 四层分开；authority、sequence/amount semantics、action applicability、external finality、领域结果和内部资金效果正交。累计快照必须先由可证明 predecessor/sequence 归一为非负 delta，每个 delta 最多贡献一次；任何层的成功、失败或 final 不外推其他层。
- **候选 A / 权威刷新优先**：event/webhook/pull 只触发刷新，Owner 签收的 authoritative query/report 决定动作事实。它抗到达乱序但依赖查询可用性与完整历史；query/report 不可用、无法分解动作或冲突时 manual。
- **候选 B / 已签收契约事件优先**：仅逐 issuer/action/source/version 明确签收的认证 event 可定事实，query/report 负责恢复和对账。它时效最高，但必须证明 scope、稳定 action identity、sequence/predecessor、金额语义、完整 replay 与 correction；query 失败不阻断已签收 event，差异也不得覆盖原事实。
- **候选 C / 多证据收敛**：要求两类独立且已签收的权威证据在 action identity、金额、币种与结果上收敛；缺一、冲突、查询不可用或 canonical relation 不明即零自动资金影响/manual。自动率最低，适合首个 issuer 或高风险 action。
- **接受的 D 路由边界**：D 不是第四种证据政策。VCC/issuer adapter Owner 按 `issuer/program/action/rule version` 为 Authorization、Clearing/Presentment、Reversal/Void、Refund 唯一选择 A/B/C，并产出已验真、已归一、可引用的外部动作事实；`wind-funds` 不理解 carrier 优先级、不查询 issuer，也不执行 A/B/C 选择，只校验归一动作的稳定身份、金额币种、原事实、冻结责任/route、累计和证据引用。无命中、多命中、规则过期或证据缺项时由 adapter 保持零自动资金影响/manual，不把 C 实现为资金底座的运行时降级路由。
- **动作边界**：authorization approve 不等于 complete，reject 保持零 legs/posting/entry/balance/declinedAmount；Clearing/Presentment 只有权威金额与顺序闭合后才形成 complete delta；reversal/void 只 release 未完成范围；expired/card closed 不自动 release；linked refund 引用真实成功 complete，未关联 credit 不猜原事实；overcapture/forced post/late clearing、chargeback/dispute 均须独立规则与 Owner，未签收时零自动资金影响；Network Settlement 永远是独立外部证据，不改变动作 delta。
- **验收矩阵**：PRD `5.20` 已逐 A/B/C 覆盖认证/scope 失败、delivery/action 重放、累计 `30 -> 50` 与乱序 `50 -> 30`、event 后 query 失败、timeout-after-effect/重启/replay、event/query/report 冲突、reversal 与 partial/late clearing、expired、linked/unlinked refund、overcapture/late clearing/chargeback 和 Network Settlement 早到/迟到/缺失/冲突；每格明确允许/禁止动作、delta/累计、父层声明和 manual 出口。父层并列展示已知外部动作、外部证据未知/冲突和内部资金闭合范围，不合成总 SUCCESS/FAILED。
- **Owner 与停止线**：Human Owner 已接受 D 的能力归位和版本化选择原则；A/B/C 仍由每个适用 issuer/program/action/version 的 VCC/issuer adapter Owner 逐项签收，不存在全局默认政策。issuer/processor Owner 签 source/scope/identity/sequence/金额语义/action finality；VCC Product Owner 签父生命周期与展示；Funds Owner 只签 normalized delta 准入和累计；Finance/Risk/Accounting、Network Settlement/Reconciliation Owner 各自终审对应外部责任和差异。具体规则与真实 adapter 尚未证明，拆为 `P-SIM04-02-HOST`；任何规则缺 `source + version/date + scope + verified date + confirmation owner` 即继续 fail-closed/manual。
- **禁止与当时下一输入**：不得把 webhook/HTTP 200/签名/event 名称直接当权威，不得混用 delivery/transaction/action identity、以时间当 sequence、累计快照当 delta、current query 覆盖历史、UNKNOWN 后补单/逆向、expired 自动释放、generic FORCE 当 overcapture 准入、chargeback 当普通 refund，或把 Network Settlement/内部 `SETTLEMENT`/reconciliation 倒推动作完成。D 接受范围准出后曾进入 `P-SIM05-01_DECISION_PACKAGE`；该包现已形成，当前唯一入口见 Metadata 与恢复入口。

### 8.13 `P-SIM05-01` ACH 外部事实、return/reversal 与 NOC 决策包

- **状态**：`accepted / P-SIM05-01-A / ACCEPTANCE_CHECKER_PASS`。
- **单一命题**：只裁上游 GlobalAccount/rail adapter 在何种已验真、已归一外部事实下可请求内部资金影响，以及后继 return/reversal 与 NOC 的公共边界；不裁 ACH/Nacha 协议、状态/原因码、retry/re-origination、Java/API/表或 posting。
- **项目边界**：`wind-funds` 只消费跨场景稳定的 normalized funds fact/instruction，不接收 raw payload、rail 状态码或 NOC，不查询 bank/ACH，也不拥有 authority/finality、return 映射或版本化 rail 路由。外部账户/GlobalAccount 不是内部余额主体。
- **事实基线**：Fincone GlobalAccount 是 `E1 / REVIEW_READY / OwnerDecision=PENDING / NOT_STARTED`；ACH source/status/finality/return/NOC 证据未签收。Provider E2 仅支持 confirmed ACH/BANK credit 到 FundingAccount 的一次 topup 与外部资金事实重放/冲突；accepted 在资金事实前拒绝，return/reversal 不支持，NOC 无专用资金入口。当前 target account 校验先于非终局事件拒绝，rail event allowlist 仍位于资金实现，二者都是待迁移现状，不是上游归一目标合同。测试未 fresh 运行，且无真实 adapter/Consumer/E4。
- **共同合同**：delivery、accepted/processing、authority confirmed fact、FundsTransaction、Ledger、指定余额、reconciliation 与 rail finality 正交。共同输入冻结 provider/rail/environment/scope、direction/action、稳定身份、amount/currency、原事实、authority source、规则版本/生效日/核验日/Owner/evidenceRef 与 return/reversal/NOC 适用窗口；缺失、冲突、多命中或陈旧时 adapter 零资金/manual。primary funds effect 由 adapter 显式归一：入站是 FundingAccount credit，出站是沿原冻结事实 withdraw；它不等于原始 ACH Credit/Debit entry class。
- **接受方案 A / confirmed-gated effect**：上游业务/rail adapter 按已签收规则证明外部事实可信并形成权威 confirmed fact 后，首次提交一次 primary effect；入站形成 normalized credit，出站沿原冻结形成 withdraw。内部闭合后只声明相应内部效果，不宣称 rail final；GlobalAccount 与 Finance/Risk Owner 承接后续 return 风险。B/C 未选择并只保留为比较证据。
- **候选 B / finality-gated effect**：confirmed 只留上游证据并展示已确认待终局；达到另行签收的 rail finality 后才提交对应入站 credit 或出站 withdraw。若 rail 无绝对 finality，只能定义可核验合同窗口，不得伪造 `FINAL`。
- **候选 C / manual-gated effect**：confirmed 只留上游证据/Difference；有权人工复核签发后才提交对应入站 credit 或出站 withdraw。它是上游安全降级，不是 `wind-funds` 运行时 adapter 策略。
- **return/reversal 与 NOC**：return/reversal 是引用适用的原 confirmed 外部事实和原 primary effect 的独立 recovery fact，不能覆盖原事实；入站已确认 recovery 累计不得超过原 credit 可逆上限，出站已确认 return/recovery 累计不得超过原成功 withdraw 的同币种可恢复上限，任何已证明 partial 均占用对应上限。原引用、方向、动作类型、责任/窗口、累计或结果不明即 Difference/manual。NOC 只形成未来主数据建议和审计，零资金、不进资金入口、不改历史责任/route/交易/账务/余额。
- **Funds 准入责任**：上游负责外部真实性与 confirmed 归一，不等于 `wind-funds` 接受任意 caller payload、`trusted=true` 或 `CONFIRMED` 字符串。资金域仍校验 tenant、正金额与币种、规范化动作类别、内部责任/账户、稳定 identity、幂等/异语义冲突、出站原冻结、recovery 原 primary effect、逐原事实累计上限，以及 FundsTransaction、平衡 posting/Ledger 与指定余额变化；失败或 UNKNOWN 均零新增资金/manual。
- **验收矩阵**：PRD `5.21` 已逐 A/B/C 覆盖 accepted/processing、confirmed、重复/乱序/scope 失败、timeout-after-effect/restart、入站 full/partial return、出站 debit/withdrawal return、return/reversal 早到/缺原事实、重复/乱序逆向、NOC 与 reconciliation；每格给允许/禁止动作、资金效果、父层声明和 manual 出口。
- **Owner 与停止线**：用户明确接受 A，并说明业务层/adapter 判断业务数据可信，`wind-funds` 专注资金处理；该说明不免除 Funds 自身准入。ACH/bank/channel adapter Owner 签 source/authority/finality 和 return/reversal/NOC；GlobalAccount Owner 签展示、主数据和 normalized effect；Funds Owner 签内部准入、原事实、累计与守恒；Finance/Risk/Accounting/Legal/Compliance 与 Reconciliation 各终审自身边界。`P-SIM05-01-HOST`、真实 adapter/Consumer、return/reversal/NOC 查询、timeout/restart、target account 校验前移、rail event allowlist 退出资金入口、artifact/Bean/schema/tx/E4、VC/G1/RED/Execution Grant 均未关闭。A 的接受范围已由独立 Checker 准出，当前下一入口见 Metadata 与恢复入口；不直接进入 Owner Gate、DSL、API、实现、测试或 Git。
- **red lines**：accepted/HTTP 200/event 名称直接入账；`CONFIRMED` 字符串冒充 authority/finality；raw rail/NOC code 进入 Public API；到达时间当顺序；外部账户入账；NOC 改余额或触发逆向；return/reversal 当 refund/负 topup；覆盖原事实；当前主数据重算历史；UNKNOWN 后换身份补单；reconciliation/内部 SETTLEMENT/余额倒推 rail finality；在资金底座建立 ACH 状态机、rail matrix 或 adapter policy engine。

### 8.14 `P-SIM06-01` 收单 capture 权威与资金准入决策包

- **状态与命题**：`accepted / P-SIM06-01-A / ACCEPTANCE_CHECKER_PASS`。只裁冻结 acquiring 责任范围内，哪种上游已验真、已归一的 capture 证据足以让 `wind-funds` 接纳一次待清算资金 effect；不裁 Merchant/平台持牌角色、PSP/卡组织协议、fee/reserve、split/clearing 规则、refund/dispute/chargeback 责任、payout finality、Java/API/表或 posting。
- **证据基线**：Fincone 收单只有 `AdmissionStatus=BLOCKED / OwnerDecision=PENDING / ACQ-GATE-001~007=PENDING` 的准入卡，真实 Merchant、法域/牌照、PSP、capture lifecycle/authority 和资金责任均未签收。Provider `AcquiringSettlementBusinessFlowTests` 是未 fresh 执行的 E2 内部组合模拟：把“上游已确认 capture 700”作为通用 pay，证明测试责任进入待清算、split 零资金及后续清算/结算/出款组合；不证明 PSP authority、真实 Merchant、外部 clearing/finality、Consumer 或 E4。
- **共同边界**：Acquiring/PSP adapter 负责 raw payload、签名、provider/environment/account scope、外部 identity、状态语义、source priority/finality 和规则版本；`wind-funds` 不解析 raw status/rail code、不查询或选择 PSP source。normalized capture 至少冻结 tenant、scene、payer/收款责任、amount/currency、stable identity、原 authorization 或已签收 no-prior-auth 原事实、partial/multiple 语义、上限、责任/规则版本与 evidenceRef；Funds 仍独立校验 identity 冲突、内部账户准入、原事实/累计、冻结 route、账务和余额守恒。
- **候选与 Owner 结论**：用户明确接受 A authoritative-capture-gated：上游权威 normalized capture 首次成立即可进入 Funds 准入；B corroborated-capture-gated 与 C manual-capture-gated 未选择，只保留比较证据，不作为 A 失败后的 Funds runtime fallback。三者原本使用同一责任、同一 capture 类型、同一待清算 effect 和完成维度，只改变证据强度；本次接受不改变共同累计、UNKNOWN、refund 与完成分层合同。
- **累计与分层**：有授权 capture 保持 tenant/currency/责任/原 route 并满足 `已确认 capture 累计 + 本次 <= 已签收可捕获上限`；无内部授权仅限已签收类别并引用上游原事实，不伪造 authorization。授权100、capture30+50 后累计80、余20；重复50零新增，冲突/超限拒绝。refund 是引用真实成功 capture 的新事实，逐原 capture 校验累计；第一笔30退款20后剩10，第二笔50仍可退50。capture authority、领域结果、Funds、Ledger、Balance、clearing、settlement、payout、beneficiary finality、reconciliation 正交。
- **验收矩阵与异常**：PRD `5.22` 已逐 A/B/C 覆盖首次合法 capture、raw accepted/processing/FAILED/REJECTED/UNKNOWN、重复/冲突、partial/multiple/overcapture、原授权缺失或 no-prior-auth 类别、Funds 准入失败、timeout-after-effect/restart、refund 原事实/partial/超限、dispute 到达、split/clearing 与 payout 非终局。UNKNOWN 只沿原 identity 查询，零新同类/相反动作；FAILED/REJECTED 标签不证明零效果，已证明 partial 保留并占用上限。
- **Owner 与停止线**：Adapter/Evidence、Merchant Settlement、Funds Account/Transaction/Ledger Owner 分别签外部归一、待清算责任、资金准入与守恒，Finance/Risk/Accounting/Legal/Compliance 终审专业责任。接受只确认“adapter 权威 normalized capture -> Funds 独立准入 -> 一次待清算 effect”的产品边界；具体 PSP/source matrix、真实 adapter/Consumer、capture 供给/查询、Merchant/责任快照、timeout/restart、artifact/Bean/schema/tx/E4 已拆为 `P-SIM06-01-HOST`。Fincone `Admission=BLOCKED`、`ACQ-GATE-001~007`、`P-SIM06-02-B` 已接受、`P-SIM06-03-HOST`、既有 HOST/E4/VC、G1、RED、Execution Grant、DSL/API/code/test/Git 全部保持阻断。
- **red lines 与下一输入**：禁止 raw webhook/status/`CAPTURED` 字符串入账、Funds 自选 PSP source/finality、外部账户/payment instrument 成为内部账本主体、capture 直入可用/settlement/payout、账本/余额反证外部 authority、UNKNOWN 换 identity、冲突覆盖、无原成功 capture 或超累计退款、dispute 当 refund、clearing/reconciliation 倒写 capture，以及把当前账户类型、subject code 或 posting 测试矩阵写成产品答案。接受范围 Checker 已判定 `PASS / 0 P0-P2`；当时进入 `P-SIM06-02_DECISION_PACKAGE`，该包现已形成，当前唯一入口见 Metadata 与恢复入口。

### 8.15 `P-SIM06-02` 出款受益人终局与最终展示证据决策包

- **状态与命题**：`accepted / P-SIM06-02-B / ACCEPTANCE_CHECKER_PASS`。只裁 executor-authoritative payout `SUCCEEDED` 与内部 Funds/Ledger/Balance 闭合后，何种证据组合允许上层声明 beneficiary/bank arrival 或 rail finality；不重开出款准入、内部 payout 资金动作、Merchant 责任、clearing/settlement、RETURNED/recovery、API/表/posting。
- **证据基线**：Fincone 清结算 E1 仍为 `REVIEW_READY/PENDING`，真实外部 payout、executor/channel、beneficiary finality 与 `CF-003/PAYOUT-RAIL` 未签收，收单总体仍 `Admission=BLOCKED / ACQ-GATE-001~007=PENDING`。Provider E2 source 表明 `PayoutOrderApplicationService` 不调用外部通道，`SUBMITTED` 只是持久化提交意图，`ACCEPTED/PROCESSING` 零资金；Caller 传入 `SUCCEEDED` 后形成内部 `PAYOUT_SUCCEEDED` 并映射展示 `SUCCEEDED`，但回执没有 beneficiary credit/finality 语义。测试仅证明同回执重放、冲突与内部资金/账务结果，不是 fresh/E4。
- **共同合同**：payout admission、外部 delivery、executor result、FundsTransaction、Ledger、Balance、beneficiary arrival、rail finality 和 post-action reconciliation 是正交证据。最终展示证据不得产生第二次 payout 资金动作；raw bank/PSP 状态和 rail profile 由上游验真归一，`wind-funds` 不持有 rail 策略或路由引擎。
- **候选与 Owner 结论**：A=`rail-finality profile`；B=`independent-arrival evidence`（已接受）；C=`authorized-manual-finality`。用户明确接受 B：executor `SUCCEEDED` 只关闭执行结果，另以 bank/beneficiary query/report/statement 对平后关闭到账；rail finality 仍独立声明。A/C 只保留比较记录，不是 B 失败后的运行时 fallback。三者使用相同 payout 资金事实，只改变最终展示证据强度。
- **版本化选择与矩阵**：B 作为统一到账证据政策后，上游 adapter 不再选择 A/B/C 之一，而是按 `provider + rail + program/account + environment + jurisdiction + currency + beneficiary-bank/capability + rule version + effective period` 唯一选择独立到账证据的权威来源，并分别声明关闭 beneficiary arrival、rail finality 或二者；不得默认互推。无命中、多命中、规则陈旧或 PENDING 时 finality fail closed/manual。PRD `5.23` 已覆盖 submitted/accepted/processing、executor succeeded、独立到账证据、重放/冲突、timeout/restart、迟到 FAILED/RETURNED/reversal、reconciliation/Gate 和规则缺失；每格明确父层声明、零第二资金动作和人工出口。
- **Owner 与停止线**：用户接受 B；A/C 只作比较记录。Acquiring/Merchant Product 签展示，rail/bank/executor 签 source/result/arrival/finality，Finance/Reconciliation 签 close evidence，Risk/Legal/Compliance 终审责任，Funds 只签 normalized payout 与内部守恒。禁止把 PayoutOrder `SUCCEEDED`、FundsTransaction、Ledger、Balance、Gate/BALANCED 或测试回执单独写成 beneficiary finality；禁止迟到相反事实覆盖历史成功、自动返还或换 identity 重发。`P-SIM06-01-HOST / P-SIM06-02-HOST / P-SIM06-03-HOST`、Fincone Admission/ACQ Gate、既有 HOST/E4/VC、G1、RED、Grant、DSL/API/code/test/Git 均保持阻断。独立 Checker 已判定 `P-SIM06-02-B ACCEPTANCE PASS / 0 P0-P2`；当时进入 `P-SIM06-03_DECISION_PACKAGE`，该包现已形成，当前唯一入口见 Metadata/恢复入口。

### 8.16 `P-SIM06-03` RETURNED、退款、费用、损失与追偿责任决策包

- **状态与命题**：`accepted / P-SIM06-03-B / ACCEPTANCE_CHECKER_PASS`。用户明确选择 B：上游权威确认 capture refund、payout RETURNED/迟到 reversal、chargeback/dispute、fee 或 loss 相关事实后，必须逐项形成耐久责任事实，才允许提交对应 normalized refund/adjustment/recovery；不重开 capture authority、payout arrival/finality、Merchant 法律身份、rail reason matrix、账户/账目、API 或实现。
- **事实基线**：Fincone acquiring 仍为 `Admission=BLOCKED / ACQ-GATE-001~007=PENDING`。Provider E2 source 只证明 payout 成功后迟到相反终态转 `MISMATCHED` 且不自动反做资金、非终态 RETURNED 可留证但零资金；RecoveryOrder 只能校验上游已给责任、已关闭资金事实和累计，不决定责任或创建追偿动作。均不是 fresh/E4 或真实 rail/Consumer 证据。
- **候选与接受范围**：A=`pre-signed-policy recovery` 未选择；B=`typed-liability-gated recovery` 已接受，principal/fee/FX/loss 逐项形成耐久责任事实后才准入；C=`authorized-case recovery` 未选择。三者的比较证据保留；B 缺少分项责任只能 fail-closed/manual，不能自动降级为 C。
- **不变量与矩阵**：capture refund、returned principal、chargeback/dispute principal、fee、FX shortfall、loss/write-off 与 merchant recovery 分层。refund/chargeback principal 逐 capture、return 逐 payout、fee refund 逐 fee、recovery 逐责任项校验同币种剩余上限；争议通知不等于权威 chargeback 裁决，representment/撤销是引用原争议事实的后继事实。已实际回流的本金不得重复追收，fee/FX/loss 不得与 principal 静默净额。PRD `5.24` 已覆盖 capture100/refund30、payout80 成功后 RETURNED80、无原成功 payout effect 的 RETURNED、回流未知、dispute/chargeback、fee5、recovery60 分次追回、剩余 loss20、重放/冲突和 reconciliation/Gate。
- **Funds 与完成边界**：Funds 仍独立校验 tenant、identity/冲突、Money、内部责任/账户、原事实、累计、冻结 route、平衡账务和指定余额；UNKNOWN 沿原 identity 恢复且零新动作。Difference、负余额、RecoveryOrder、超 SLA、Gate/BALANCED 或未追回均不等于 loss/recovered；loss/write-off 由 Finance/Accounting 的独立事实关闭，对账只核对证据和结果。
- **Owner 与停止线**：Acquiring/Merchant Product、rail/executor、Merchant Settlement、Finance/Accounting/Risk/Legal 分别拥有业务资格、外部事实、合同责任和专业定责；Funds 只拥有 normalized action 与资金守恒，Reconciliation 不创建资金动作。A/C 未选择且不是 fallback。禁止 RETURNED 覆盖 SUCCEEDED 或自动返还/重发、把 refund/return/chargeback/fee/loss 合并、从当前 Merchant/余额/reserve/negative 推责任、raw reason/status 进入 Funds Core，或新增 acquiring/rail 专用公共 DSL/API。真实 source/rule/version/scope、逐项责任 Consumer、timeout/restart 与 artifact/Bean/schema/tx/E4 已拆为 `P-SIM06-03-HOST`；Fincone Admission、全部 HOST/E4/VC、G1、RED、Grant、DSL/API/code/test/Git 均保持阻断。决策包与接受范围独立 Checker 均已判定 `PASS / 0 P0-P2`；B 仍不等于可运行，当时下一入口为 `P-SIM07-01_DECISION_PACKAGE`，该包现已形成，当前入口见 Metadata/恢复入口。

### 8.17 `P-SIM07-01` 对账来源、匹配与差异关闭决策包

- **状态与命题**：`accepted / P-SIM07-01-A / ACCEPTANCE_CHECKER_PASS`。用户明确选择 A，且接受范围已通过替代独立 Checker：只允许上游已验真、归一并冻结范围的 strict-exact 匹配证据进入 `Matched/Balanced`，并继续使用 Difference 的追加处置与 current-lineage 关闭合同；不决定原始来源采集/解析、场景规则路由、资金修复责任、API/表/事务或实现。
- **事实基线**：Provider `eb120918... + dirty worktree` 的当前 E2 source 已有批次/两侧来源快照、不可变运行结果、显式 `1:1` 匹配项、追加 Difference/Action、后继 current-lineage 重跑和同事务 Gate 骨架；当前 match item 明确不支持用重复引用隐式表达 `1:N/N:1`。但实现只验证调用方提交的 `VERIFIED 1:1 match assertion` 的结构与 coverage，并把 `EXACT_MATCH/RULE_MATCH` 都直接计入自动对平；它不比较两侧 Money/status，也不验证 RULE_MATCH 的规则/算法/版本。因此当前 E2 不证明 A strict-exact 或 B signed-policy enforcement。Fincone HEAD `09c70360... + 89 dirty paths` 的清结算产品/系分/准出仍为 `E1 / OwnerDecision=PENDING / Gate OPEN`，只支持目标边界，不证明真实 Consumer、L3/E4 或发布。
- **共同合同**：每次 run 冻结 tenant、scope/Gate object、半开窗口与 timezone/date semantics、currency、rule version、两侧成员/digest、coverage/watermark、evidence refs 和 lineage。Completed 不等于 Balanced。旧 batch/run/match/difference/action 只读；输入变化创建后继 run。来源 adapter 拥有 raw source 的 authority、解析、业务聚合与规则选择，Funds 只保存 normalized evidence，不持有业务/rail 匹配路由或容差引擎。
- **Owner 结论与候选保留**：接受 A=`exact-evidence-gated`，仅规范化 `1:1`、同币 Money/状态/稳定引用严格相等且 coverage 完整时自动对平，复杂关系由来源 Owner 先固化为带成员清单和 digest 的单一聚合事实。B=`signed-policy-evidence-gated` 与 C=`authorized-case-evidence-gated` 未选择，只保留比较证据，不是 A 的运行时 fallback。A 只与当前 `1:1 + coverage + lineage` 骨架同向，strict equality 的 adapter/Consumer 供给和 Provider enforcement 仍是 gap。
- **不变量与矩阵**：同一成员在一个结果最多贡献一次；禁止重复引用伪装 `1:N/N:1`、空侧/coverage 不全按零、总额或行数相等即 Balanced、跨币无 FX/舍入证据比较。Difference 的 `Resolved` 必须先有受控 action evidence，再有后继 current-lineage `Balanced`；证据纠正只可让旧差错 `Invalidated`。Gate 仅消费 exact object 的 current completed/balanced lineage，`inspect` 不是最终授权，最终资金命令须同事务 `check`。PRD `5.25` 已覆盖 exact、60/40 聚合、微差、跨币、缺侧、重放/冲突、action 后重跑、证据替代、旧 lineage 与 Gate/Balanced。
- **Owner 与停止线**：各来源 Owner 签 source/scope/coverage/聚合/correction；Finance/Risk/Operations 签容差、条件放行、write-off 和人工权限；Funds/Ledger/Balance 只提供自身不可变事实。禁止 reconciliation 创建 adjustment/recovery、人工备注直接关 Difference、旧 Gate 放行或 Gate/Balanced 倒推资金/外部 finality。当前 Provider 会将 `EXACT_MATCH/RULE_MATCH` 都计入自动对平且不比较 Money/status，不能冒充 A 已实现；真实来源 adapter/Consumer、authority、strict equality、coverage、current-lineage 恢复、SLA/条件放行与 artifact/Bean/schema/tx/E4 已拆为 `P-SIM07-01-HOST`。既有 HOST/E4/VC、RED、Grant、API/code/test/Git 全部继续阻断；`G1` 现已通过，但只准入 `W2-01` DSL 设计，不关闭上述运行门禁。

### 8.18 `W1-02 / G1` 产品信息就绪与准出复核

- **复核对象与结论**：Maker 的 `G1 PASS CANDIDATE` 经替代独立 Checker 复核为 `PASS / 0 P0-P2`。产品正文已明确公共能力定位、目标/非目标、主体与事实 Owner、核心对象和不变量，并用 `SIM-01` 至 `SIM-07` 覆盖主路径、逆向、异常、人工停止、可观察结果与验收种子。
- **Owner 与产品决策**：`Q-001~Q-004` 以及场景政策 `P-SIM01-01-D / P-SIM01-02-A / P-SIM01-03 / P-SIM02-01-A / P-SIM02-02-A / P-SIM03-01-D / P-SIM03-02-R-A / P-SIM04-01-A / P-SIM04-02-D / P-SIM05-01-A / P-SIM06-01-A / P-SIM06-02-B / P-SIM06-03-B / P-SIM07-01-A` 均已获 Human Owner 接受并通过对应接受范围 Checker；不存在仍须由 DSL 代替产品裁决的公共语义分叉。
- **HOST/E4/E5 归位**：`5.12` 的责任快照、source profile、真实 adapter/Consumer、查询恢复、artifact/Bean/schema/tx 和外部/生产证据只阻断对应场景自动运行、垂直切片 eligibility 与生产声明。它们不会进入 `core` 或 Public API，也不阻断从已接受产品合同提炼最小稳定 DSL；任何切片仍须独立证明其 HOST/E4 门禁。
- **Product Context Card**：产品正文 `9.2` 已冻结业务意图、Owner、目标/非目标、核心对象与 DNA、流程规则、七场景验收种子、风险与工程交接条件。交接只允许进入 `W2-01`，不批准目标 Java/API、RED、Execution Grant、宿主集成、Git 或发布。
- **验证状态**：实际执行 `check_product_deliverable.py --kind prd --file <PRD>` 返回 `OK`（仅有非阻断 `implementation_language` WARN），`check_harness_plan.py --kind gsd-wave --file <spec>` 返回 `OK`；两份目标文档当前未跟踪，分别执行 `git diff --no-index --check /dev/null <file>` 均无空白诊断（退出码 `1` 仅表示文件相对 `/dev/null` 有内容）。重复标题与旧当前状态扫描均无命中。外部规则 checker 因真实 issuer/ACH/PSP/rail `source/version/scope/verified_at/confirming_party` 未签收而返回预期 `FAIL`，该失败继续记入对应 HOST/E5 blocker，不冒充 G1 已具备运行规则。
- **业务/研发/测试角色走读**：业务角色按 PRD `5.2~5.4 / 9.1` 回读七场景、Owner 与已接受规则，结论为不存在待 DSL 代裁的公共产品分叉；研发角色按 PRD `9.2` 回读最小对象、不变量与非目标，结论为可进入 `W2-01` 提炼稳定词汇且不得设计 Java/API；测试角色按 PRD `5.4 / SIM-01~07 / 9.1` 回读正常、逆向、UNKNOWN、人工停止与零副作用种子，结论为足以生成 DSL 正反例与后续 TDD 输入。三项均为 `PASS`；这是基于同一权威文档的角色走读证据，不是组织签字、HOST/E4/E5、运行测试、发布或生产批准。
- **Gate 结论与交接**：`G1 PASS` 只准出 Product Context Card 到 `W2-01`，用于提炼最小稳定词汇与事实层级。HOST/E4/E5、VC eligibility、RED、Execution Grant、Java/API、生产/测试源码、Consumer、Git、发布与生产仍未获授权。

### 8.19 `W2-01` 最小稳定词汇与事实层级

- **状态与范围**：`CORE_CANDIDATE / INDEPENDENT_CHECKER_PASS / 0 P0-P2`。目标 DSL 正文为 `docs/DSL设计/支付资金公共能力层-DSL设计.md`；它只定义跨场景稳定词汇、事实层级、引用语义和 JSON 正反例，不批准同名 Java/API/DTO、表、枚举、route/posting 细节或实现。旧 `支付资金底座DSL承载层设计.md` 降为当前实现、账务矩阵、API 草图和历史方案的迁移证据，不能覆盖 G1 产品语义或本轮候选。
- **最小词汇**：候选覆盖 tenant、稳定身份、Money、责任主体、交易角色、内部账户、账务目标、余额目标、事实时间、业务/外部/账本/规则引用、规范化外部资金事实、领域结果、资金责任快照、Intent、Attempt、FundsEffect、动作事实、原资金事实引用和语义摘要。每个概念均给出“是/不是”、Owner、不变量、最小序列化形状和至少两个真实场景；主体、角色、账户、账务目标与余额目标不能互推，四类事实引用不能因底层都是字符串而混用。
- **事实与完成分层**：业务、外部、执行、责任/route、账本、余额、对账七层只表示 Owner 与证明范围，不是全局状态阶梯；authority、domain outcome、funds effect、ledger、balance、external finality、reconciliation 七个完成维度正交。`FAILED/REJECTED` 不证明零效果，`UNKNOWN/PROCESSING` 只沿原 identity 查证，已证明 partial 保留并占用上限。
- **公共不变量与边界**：Intent 稳定、Attempt 受控增加、动作事实耐久独立、同身份同摘要重放/异摘要冲突、逆向逐原成功事实追加、逐原事实累计不超上限、责任/route 不按当前配置重算。raw webhook/rail status/NOC、宿主状态机和专业政策不进入 Funds Core，承重 Money/责任/原事实/authority/幂等/累计不得藏入 `contextVariables`。
- **现状与验证**：当前 `Money`、`FundsAccountId/SubjectRef`、`FundsInstructionSpec`、`FundsInstructionReferenceSpec`、route/ledger specs 与 `contextVariables` 只作为迁移差距证据；未据此确认目标类型。三个 JSON 代码块均经 Ruby 标准 `JSON.parse` 通过；目标 DSL/README/PRD/spec 尾随空白扫描无命中，DSL 标题无重复；`git diff --no-index --check /dev/null <DSL>` 无空白诊断，退出码 `1` 仅表示新文件有内容。
- **Checker 与停止线**：替代独立 Checker 两轮初审先后指出主体/角色/账务/余额混叠、宿主非资金事实越界、引用合同不闭合及 FundsEffect 基数冲突；修正后最终判定 `PASS / 0 P0-P2`。该 PASS 只准进入 `W2-02` 指令、事实、route、账务与逆向边界；HOST/E4/E5、VC、RED、Grant、系分/TDD、Java/API、生产/测试源码、Consumer、Git、发布和生产继续阻断。

### 8.20 `W2-02` 指令、事实、route、账务与逆向边界

- **状态与范围**：`BOUNDARY_CANDIDATE / INDEPENDENT_CHECKER_PASS / 0 P0-P2`。本轮只在 DSL 正文中确认 public/internal 产品语义，不批准 Java/API/DTO、表、状态枚举、事务、posting matrix、代码、测试或迁移执行。
- **唯一公共动作链**：上游 `BusinessFactRef / NormalizedExternalFundsFact -> FundsIntent -> FundsAttempt -> FundsActionInstruction -> FundsActionFact`。新增概念只有请求一次可按同 identity 查询资金动作的 `FundsActionInstruction`，以及嵌入 ActionFact、逐原事实绑定分配金额与 route snapshot 引用的 `FundsRouteProvenance` value；前者按 action kind 冻结原事实与外部事实基数，责任只沿 Attempt 所属 Intent 获取，不证明动作、账本或余额完成，也不成为万能场景容器。RouteSnapshot 本身仍是 internal durable fact。
- **内部事实边界**：resolved route 与 posting plan spec 为 internal transient；route snapshot、LedgerPostingPlan、ledger transaction/entry 为 internal durable facts，public 只持稳定引用或通过各自 face 只读查询；balance projection 与 reconciliation evidence 只证明各自范围，不创建或改写资金、route、posting、ledger 或外部事实。
- **逆向与累计**：complete/release 引用原 authorization；refund 创建 reverse Intent 并逐真实 successful complete 分配；return/recovery/adjustment 创建独立 reverse/recovery Intent 并逐实际原 primary/payout/fee 事实累计。每个 `OriginalFundsFactRef` 都携带本次 `allocatedMoney`，多项合计等于动作 Money；reverse responsibility snapshot 必须逐项覆盖这些分配对应的原冻结责任，不得合并转嫁。Funds 逐项加载原 `ActionFact.routeProvenance` 并把原事实、分配金额和原 route 绑定写入新事实，Consumer 不选择 route。所有后继动作保持原 tenant、Money、责任与 route，不用负 Money、当前配置或覆盖历史表达逆向。
- **当前源码差距**：`FundsInstructionSpec` 将业务身份、执行、外部事实、账户、账目、操作者和宽 context 合并，`FundsInstructionReferenceSpec` 只有单一含糊引用，route/ledger specs 仍含重复业务字段和宽 context。它们只证明迁移起点；目标态拆分不保留 bridge、别名、转发重载、双写双读或兼容窗口，实际处置延后 W3 与获授权垂直切片。
- **契约样例、Checker 与停止线**：DSL 正文给出支付/退款、授权/两次部分完成/释放、外部入金/return、清结算/对账四组 JSON；样例补齐各自承重的 Intent/Attempt/Instruction/原与后继 ActionFact route 链，支付退款样例还以两笔原 complete 的 `1200/800` 分配证明逐原 route provenance，并显式覆盖 `UNKNOWN -> query same identity -> zero new/opposite action`。`LedgerFactRef` 通过显式 action identity 独立追加，不依赖邻接或原地修改不可变 ActionFact；具体关联实现留 W3-01。全文 7 个 JSON 块均经标准 JSON parser 通过，Harness、产品结构和空白检查通过。替代独立 Checker 经三轮增量复核，最终判定 `PASS / 0 P0-P2`；该 PASS 只准进入 `W3-01` Core/Ledger 系分。HOST/E4/E5、VC、RED、Grant、TDD、代码、测试、Consumer、Git、发布和生产继续阻断。

### 8.21 `W3-01` Core 与 Ledger 系分边界

- **状态与范围**：`SYSTEM_DESIGN_CANDIDATE / INDEPENDENT_CHECKER_PASS / 0 P0-P2`。目标系分正文为 `docs/系分设计/支付资金公共能力层-系分设计.md`；本轮只确认 Core/Ledger 模块职责、内部写端口、只读查询、账本事实、余额投影、事务/幂等和 API 处置，不批准 Java 签名、DDL、API baseline、TDD、实现、Consumer 或 Git。
- **核心裁决**：Consumer 不能提交 posting plan、LedgerTransaction、LedgerEntry、账目或余额指令。transaction 在动作、责任和 route 准入后，只通过 `core` 的一个最小 internal Ledger command port 请求记账；该 command 只接受已证明正资金效果，full 按 action Money、partial 按 proven Money，zero/unknown 零 command/posting，effect kind 与实际 posted Money 纳入幂等摘要。Ledger 内部生成瞬态 posting plan spec，并在同一本地事务原子写入 LedgerTransaction、耐久 LedgerPostingPlan、entries、action identity 关联并更新余额主投影。该 port 是模块协作边界，不是 Public API；现有 ledger-impl `LedgerTransactionCommandService` 仍只是内部持久化命令。
- **最小实现面**：当前只有一个真实 posting grammar 与一个生产余额投影实现，因此 `LedgerPostingAssembler` 内收 ledger-impl、`CompositeLedgerPostingAssembler` 删除、`LedgerBalanceProjectionService` 内收并折叠单实现；不新增 Facade、插件注册表、策略引擎或兼容层。
- **事实与查询**：每个可记账 action 最多一个 LedgerTransaction，目标以 `tenant + action identity` 显式关联 LedgerFactRefs；`fundsTransactionSn` 可能是生命周期根，不能冒充独立 action identity。`LedgerPostingPlan` 保持 Ledger 内部不可变耐久事实，以 `postingPlanSn + routeLegId` 关联 entries 并支持逆向、清分来源摘要和归属校验；不新造 posting group 替代物，也不把 plan 暴露成 Consumer 可构造/回放对象。余额闭合沿 `action identity -> LedgerFactRefs/entries -> BalanceTarget -> committed ledger projection` 重查，以 Ledger 本地原子提交为前提；不新增 projection lineage，`Ledger.version` 只作乐观锁。`ledger-face` 只保留有真实调用方的受控建账和稳定只读事实查询，宽 context、裸 ID、内部 plan 内容与 Entity/Mapper 不进入稳定 Public API。
- **当前证据与缺口**：现有 transaction/ledger 本地事务链、ledger transaction SN + canonical digest 幂等、耐久 plan/entry 唯一 SN、余额乐观锁和 reconciliation/wallet 只读调用均为 E2 source；`DefaultLedgerTransactionPostingServiceImpl` 已依赖 ledger-impl 内部 command，而 transaction orchestrator 仍直接消费宽 assembler/spec/posting service。测试 schema 不证明生产 DDL，当前也没有目标 ActionFact 持久模型、目标跨模块 internal port、真实统一 Bean/schema/tx 或 Consumer E4。若 ledger facts 与余额投影不一致，只能视为完整性事件并 fail-closed/manual，不自动重放 action/posting 或局部投影。
- **破坏式处置**：目标切换不保留旧/新重载、bridge、alias、双写或双读。实际删除/迁移必须等对应 RED、TDD、Execution Grant 与 Consumer E4；本轮仅形成处置清册。Checker PASS 后唯一进入 `W3-02 / WALLET_BOUNDARY / SYSTEM_DESIGN`。

### 8.22 `W3-02` Wallet 系分边界

- **状态与范围**：`SYSTEM_DESIGN / INDEPENDENT_CHECKER_PASS / 0 P0-P2`。本轮只确认 Wallet 的账户身份/状态、资金责任关系、PaymentInstrument/绑定、Spend Control/Rule、余额查询及 Public API 处置；不批准 Java 签名、DDL、API baseline、TDD、实现、Consumer 或 Git。
- **公共职责**：Wallet 只拥有 Funding/Credit 账户主数据与能力、版本化资金责任关系、支付工具稳定引用与绑定历史、支出控制事实，以及受控账户余额查询。Card/token/holder、Coupon、Order、payer/payee/merchant 标签和外部账户不得直接成为内部账户或账本主体；必须先由上游形成责任事实，再经 tenant、币种、关系和账户 capability 准入映射为 InternalAccountRef。
- **账户与生命周期**：当前 `FundsAccountId(id + type)` 只作迁移证据，不能替代已接受的 tenant-scoped `InternalAccountRef`；`FundsAccountOwner` 只表达主数据归属。Capte 生产证据只支持 Funding/Credit 查询，开户及账户状态变更没有生产 Consumer 准出。目标状态变化归 Wallet Owner，必须带预期版本、原因/有权证据和稳定幂等身份，且只改变后续准入、零 Funds/Ledger/Balance 副作用；具体迁移矩阵、命令可见性和物理类型保持 PENDING。
- **Owner 分离**：Wallet 不创建 FundsTransaction、route、LedgerTransaction/Entry 或 Balance。`PaymentInstrumentTransactionApplicationService` 与 `SpendControlTransactionConsumptionApplicationService` 的现有实现位于 transaction-impl，目标交易/消费编排移交 W3-03；Wallet 仅提供准入证据与控制事实。`LedgerQueryService` 退出 wallet-face，由 W3-01 Ledger 查询承接；`SubjectLedgerInitializer` 只内收账户创建，当前还被 Transaction 调用的 `LedgerProfileService` 迁移为 Ledger Owner 受控读取，不继续由 Wallet 定义账务配置。
- **责任与工具不变量**：Funding 表达真实资金/待付责任，Credit 表达额度/授权占用/应还责任；两者不由 PREPAID/SHARED、Card 或 ownerType 自动创建/选择。新动作校验当前工具、binding、责任关系和账户能力；历史动作只沿原 authorization 保存的 binding/responsibility/route。Wallet 预交易 DTO 或 `ready=true` 不是 FundsResponsibilitySnapshot 的自证，最终冻结仍由 Funds 独立校验后形成。
- **规则与控制边界**：Wallet 可保存版本化 normalized rule、binding、decision、movement 和预算投影；raw PAN/CVV/card processing/POS/AVS 等 rail 字段及无第二生产实现证据的组合引擎不进入 Core/Public API。Transaction 只消费已固化决策，并按原 action/ref 请求 Wallet 追加控制事实；UNKNOWN 零新 movement、零补交易、零相反动作。
- **当前证据与缺口**：Capte 生产只直接读取 Funding/Credit 账户和指定余额投影，没有联合生产 schema/E4；账户创建只有测试/内部证据，账户状态只有开户初态与读取，没有目标状态命令；Fincone VCC 仍是 E1，无 runtime Consumer。当前责任 decision 缺完整 version/scope/effective evidence，ownerType 可推 LedgerProfile，PlatformFundingAccountRole 可推账目，Spend Rule 仍含卡协议枚举，均只作现状 gap，不是目标合同。
- **处置与停止线**：系分 7.7 已逐项标记 keep/narrow/internalize/move/remove；目标切换不增加 Facade、compatibility bridge、旧新重载、双写或双读。独立 Checker 初审的三项 P1 经最小修订后复核为 `PASS / 0 P0-P2`；当前唯一进入 W3-03 Transaction 系分。HOST/E4/E5、VC、RED、Grant、TDD、Java/API/code/test/Consumer/Git、发布和生产继续阻断。

### 8.23 `W3-03` Transaction 系分边界

- **状态与范围**：`SYSTEM_DESIGN / INDEPENDENT_CHECKER_PASS / 0 P0-P2`。系分正文第八章只确认 Transaction 的动作事实、生命周期根、route、原事实/累计、Ledger/Balance 本地闭合、Wallet 控制协作、内部 stage command、查询恢复和 Public API 处置；不批准 Java 签名、DDL、API baseline、TDD、实现、Consumer 或 Git。
- **事实与结果**：当前 `FundsTransaction` 是生命周期根或主聚合，授权后继动作可复用 root，公开服务返回的非空 `String` 也是 root SN；`FundsTransactionDetail` 又按 participant 保存动作片段。目标要求 Intent、Attempt 与 action identity 分别耐久且可查询，每次动作形成独立 `FundsActionFact`；root 只由已闭合动作派生累计，participant/detail、root state、同步返回或记录存在均不能冒充完整动作事实。
- **准入与幂等**：公共链只保留 `Intent -> Attempt -> ActionInstruction -> ActionFact`。Attempt 只属一个 Intent，同一时刻一个 Intent 最多一个仍可能生效 Attempt；UNKNOWN、在途、partial 或可能迟到时零新 Attempt。只有旧 Attempt 权威终结、目标效果为零或责任闭合、不再迟到、经济语义未变且重新授权后，才准入新 Attempt；语义改变必须新 Intent。同 action identity 同 digest 复用，异 tenant/action kind/Money/责任/原事实/normalized fact/rule version 冲突。FAILED/REJECTED 标签不证明零效果，authorize reject 只有零 legs/posting/entry/balance/declinedAmount 才是窄 `proven-zero`。
- **Route 与累计**：首次动作按冻结责任内部唯一选路；后继动作不接受 caller 选路，而是逐 `OriginalFundsFactRef + allocated Money` 加载原 `FundsRouteProvenance`。多原事实分配合计等于动作 Money，逐项同币并守住剩余上限。授权 complete+release 不超授权可处置范围；refund/recovery 逐原成功事实累计，partial 占用上限，UNKNOWN 不改累计。
- **本地闭合与正交性**：对 proven-full/partial，成功 Action、生命周期根累计/逐原事实上限占用、action-ledger 关联、LedgerTransaction/Plan/Entry 和 required BalanceTarget 主投影目标上共享本地事务，多原事实分配整体成功或整体回滚；zero/unknown 零 Ledger command。外部 authority/finality、Wallet 控制事实和 Reconciliation 继续正交，after-commit 观察失败不覆盖事实。真实统一 Bean/schema/transaction manager 与 rollback/restart 仍为 E4/HOST blocker。
- **Action kind 与场景协作**：现有 event/method 只能编译到 W2-02 六类稳定 kind：topup/transfer/direct pay/withdraw/fee/Benefit contribution/clearing confirm=`primary`；authorization/freeze/settlement lock=`authorize`；complete/payout success=`complete`；reversal/unfreeze/settlement release/已证明零 payout effect 的失败释放=`release`；逆向 successful complete=`refund`；primary/fee/Benefit/payout return 与有原事实调整=`recovery/adjustment`。无原事实 generic adjust 保持阻断，不得以 event string/context 扩 DSL。raw ACH/VCC/PSP status、accepted/processing/NOC 退出 Funds，由 adapter 归一为一个 external fact。Benefit adapter 留在 Capte，Provider 场景 face 在 Consumer E4 后删除。Clearing/Settlement/Payout 只保留为受 current-lineage/Gate/normalized result 约束的 internal stage command。
- **处置与停止线**：系分 8.7 已将 direct/auth/balance services 收敛为窄 action command/query，将 root/action 查询分开，raw rail entry 移除，Provider Benefit 场景 face 迁出，stage/lifecycle/route/frozen-order/projection primitive 内收；不增加 Facade、registry、bridge、双写或双读。当前没有目标 Intent/Attempt/ActionFact/query 物理合同；Capte Wallet/Coupon 仍以 root String/记录存在判成功，均为明确 HOST blocker。独立 Checker 初审发现 Intent/Attempt 合同缺失、Benefit adapter 归属含糊、stage 动作未映射稳定 action kind 三项 P1，以及 PRD revision 漂移一项 P2；最小修正后单点复核为 `PASS / 0 P0-P2`。当前唯一进入 `W3-04 / RECONCILIATION_BOUNDARY / SYSTEM_DESIGN`，HOST/E4/E5、VC、RED、Grant、TDD、Java/API/code/test/Consumer/Git、发布和生产继续阻断。

### 8.24 `W3-04` Reconciliation、Clearing 与 Settlement 系分边界

- **状态与范围**：`SYSTEM_DESIGN / INDEPENDENT_CHECKER_PASS / 0 P0-P2`。系分正文第九章只确认 source snapshot、strict-exact run、Difference/action、current lineage、Gate，以及 clearing/settlement/payout stage 的事实与协作边界；不批准 Java 签名、DDL、API baseline、TDD、实现、Consumer 或 Git。
- **最小事实核**：复用现有 `Batch -> SourceSnapshot -> Run/MatchResult -> Difference/Action -> replacement/rerun -> current-lineage Gate`，不建立第二套 Reconciliation Core、规则 registry 或通用 workflow。Batch Completed 与 Balanced 分开；旧 run、旧 Gate、行数/总额或 caller label 不构成对平证据。
- **strict-exact 强制**：来源 Owner 先完成 raw source 验真、状态归一和复杂聚合，再提交结构化稳定引用、同币 Money、范围/事实时间、原 Owner-local DomainOutcome evidence，以及同一 scope/rule version 下的 comparison status。DomainOutcome 不跨 Owner 比较；comparison status 只是 Reconciliation 规则作用域内投影，不进入 Core或成为全局状态。Provider 独立校验 1:1、Money/currency/comparison status/scope、唯一映射和完整 coverage 后才 Balanced；当前 `EXACT_MATCH/RULE_MATCH` 只信 `VERIFIED + refs` 的行为明确为 gap，`RULE_MATCH` 不进入已接受 A 的自动路径。单个 run 只做两侧 strict exact；三方/多来源场景组合由 Owner 冻结的必需 pairwise run 集合，不新增 N 方引擎。
- **Difference 与 Gate**：pure reconciliation 与 Gate-bound run 的非匹配结果都能形成 Difference。Difference 只追加受控 action evidence；Resolved 还需后继 current Balanced，Invalidated 只因正式 replacement。Gate 按冻结 policy 检查 exact object 的全部必需 run 均为各自 current completed+balanced lineage 且无 blocking Difference；当前单 `runResultSn` 是 gap。inspect 是解释，权威 check 只由对应 stage 用例在本地事务消费，不是资金动作、close evidence、缓存授权或初始支付 Gate。
- **阶段事实**：可清分来源必须沿真实 successful Action、LedgerFactRefs/entries、耐久 posting plan/route、Money 和 Balance evidence 验证；split confirmation、candidate、batch/approval 均零资金。clearing confirm=`primary`，settlement lock=`authorize`，release 引用原 lock=`release`；它们在自身事务复核 Gate/current source 后调用 W3-03 internal stage command。当前没有真实生产 Consumer，先保留 internal funds stage 语义，不据测试冻结宽 Public face。
- **payout 与 recovery 收缩**：raw receipt/status/reason、source priority、beneficiary arrival、rail finality、display 和责任 case 留上游 adapter/宿主。Funds 只消费一个 normalized payout fact；accepted/processing 零资金，success 引用 settlement lock complete，confirmed-zero failure 才可 release，RETURNED/late reversal 按原 payout fact 进入 recovery/adjustment 候选。当前通用 `RecoveryOrderApplicationService` 无真实 Consumer且允许 caller 自报责任，目标删除 case API，复用 ActionFact + Difference 关联。
- **当前证据与停止线**：现有 batch/source/run/lineage/Gate 与 clearing/settlement/payout 组合链只属 E2 source；真实 strict-exact structured source、pure Difference、stage ActionFact、adapter/Consumer、production schema/transaction manager 和 timeout/restart 仍由 HOST/E4/E5 阻断。独立 Checker 初审发现跨 Owner 比较 `DomainOutcome` 的一项 P1；改为保留 Owner-local outcome provenance、仅比较 scope/rule-version scoped comparison status 后复核为 `PASS / 0 P0-P2`。该阶段当时下一入口为 `W4-01`；该项现已准出，当前唯一入口见 Metadata 与恢复入口。

### 8.25 `W4-01` TDD 场景与 RED 设计

- **状态与范围**：`TDD_DESIGN_CHECKER_PASS / 0 P0-P2`。TDD 正文第二十章只形成实现前场景矩阵、共享断言、RED 分类、当前测试资产处置和验证边界；未创建或执行测试，未冻结 Java/API/DDL，也未取得 Execution Grant。
- **权威与旧资产**：目标语义按产品设计、DSL、系分和 TDD 第二十章依次解释。TDD 前十九章、旧 PRD/DSL/编号系分与当前测试只作实现回归和迁移证据；旧 `FundsInstruction`、root String/detail、Provider Benefit facade、raw external-event allowlist、caller `RULE_MATCH`、raw payout receipt 和 generic Recovery case 不得覆盖目标合同。
- **最小 RED 结构**：RED 仅分 `TARGET_CONTRACT_RED / CURRENT_BEHAVIOR_RED / HOST_EVIDENCE_RED`。共享集合覆盖 public DSL 边界、full/partial/zero/unknown Ledger Money、action-scoped Balance、Wallet 责任/控制、Intent/Attempt/ActionFact、UNKNOWN/幂等、逐原事实/route/累计、raw 外部事实隔离、strict exact、Difference/current lineage、stage Gate、Public API 边界和 HOST/E4 证据。
- **七场景覆盖**：`SIM-01` 至 `SIM-07` 均按同一六列给出正常、边界、逆向、异常/UNKNOWN、并发/重复和人工停止；共同资金链只定义一次，不为 Coupon、Benefit、VCC、ACH、收单或对账复制资金测试内核。资金变化统一断言 Action、Effect、原事实/累计、route、LedgerTransaction/Plan/Entry、required BalanceTarget、幂等与失败零副作用。
- **真实证据与复用**：内部链最低使用真实 Spring Bean、H2 schema 和既有 converter/resolver/orchestrator/assembler/posting/projection；Mock/Fake 只替换明确的外部 adapter、时间或 ID。现有 Ledger、Wallet、Transaction、Reconciliation 流程测试按职责复用；场景 facade 与 raw rail 测试只作 characterization。Recording/Fake、共享 SNAPSHOT 或单仓 H2 不能关闭 HOST/E4/E5。
- **准出前源码校准**：主笔回读当前 source 后收紧三处承重口径：零资金效果可保留 REJECTED/FAILED Action 或无 legs 解释证据，但零 Ledger/Balance 副作用；`CURRENT_BEHAVIOR_RED` 只将 Funds 内 `ACH_CREDIT_CONFIRMED/BANK_CREDIT_CONFIRMED` allowlist 记为当前可执行冲突；当前对账冲突精确为 caller 的 `VERIFIED + EXACT/RULE` assertion 未强制 Money/status/scope/rule evidence 便可导出 `BALANCED`。TDD 同时补了 RED -> 真实测试层级 -> 当前红灯依据 -> 最小命令族的映射；该校准是 Maker 修订，不自证 Checker PASS。
- **文档验证**：TDD 使用 `check_harness_plan.py --kind lightweight` 返回 `OK`；执行规格使用 `--kind gsd-wave` 返回 `OK`；目标系分 validator 返回 `OK`；产品 validator 返回 `OK`，仅保留非阻断 `implementation_language` WARN。W4 专项只读断言返回 `RED=13 / SIM=7 / required=10`，三条新增相对链接均存在。TDD tracked diff 与三份未跟踪权威文档的 `--no-index --check` 均无空白诊断；`--no-index` 退出码 `1` 只表示相对 `/dev/null` 有内容。架构交付 validator 没有 TDD kind，以 `architecture-plan` 试跑会因缺 `architecture_type` 返回 FAIL，因此不作为本 Gate 证据，也不为通过检查给 TDD 伪加架构字段。独立 Checker 首轮发现 WALLET/STAGE 命令清册未覆盖声明的真实测试层级；拆出 Spend Control 精确 slice 与 `test-reconciliation` 后单点复核为 `PASS / 0 P0-P2`。
- **交接与停止线**：plan-r2.64 后当时已进入 `W4-02` 跨仓 L3/L4 验证计划；这不是 RED 执行、API baseline、代码/测试、Consumer、Git、发布或生产授权。任何只能靠 Recording/Fake、聚合状态或未签外部规则变绿的场景必须保持 blocker。

### 8.26 `W4-02` 跨仓 L3/L4 验证计划

- **计划准出与执行范围**：`VALIDATION_PLAN_CHECKER_PASS / 0 P0-P2`。`plan-r2.66` 只冻结跨仓验证层级、artifact lineage、真实宿主资格、最小执行矩阵和停止条件；当时没有执行 Provider/Consumer 测试，也没有修改 Java/API/DDL、生产/测试源码或 Consumer 仓。`plan-r2.67/r2.68` 的授权与执行结果见本节后续条目。跨仓 L3 映射本 Change 的 E4，跨仓 L4 映射 E5。
- **Artifact Lineage Card**：Consumer Host Owner 必须一次提交 Provider repo/revision/dirty fingerprint、构建环境/命令、实际使用的 core/face/impl 坐标与 JAR SHA-256，Consumer repo/revision/dirty fingerprint、effective POM/dependency tree/resolved path；并为每个实际承重 core/face/impl 制品各选代表 class，逐项记录 `CodeSource`、resolved JAR SHA-256、loaded JAR SHA-256、JDK/classloader、测试命令/报告。每个制品的 binary/resolved/loaded hash 必须逐项相同；共享 SNAPSHOT、时间戳、缓存或历史报告不准出，lineage 校验不得因 property 缺失而跳过。
- **宿主资格**：当前仅 `CAPTE-BENEFIT` 具备 L3 计划资格：生产 Consumer 已调用 settle/refund，集中测试已手工装配真实 transaction/ledger/wallet impl、联合 H2 与八张 Provider 表；但仍缺强制谱系、生产等价 Bean/proxy、明确 tx 与 timeout/restart。`CAPTE-WALLET` 因组合测试仍使用内部 Recording 而 blocked；`fincone-issuing` 没有 wind-funds 运行时 Consumer，VCC/ACH blocked；收单/payout 缺 Admission/真实 Host；Reconciliation/Stage 只是 Provider 仓内 E3，不伪造跨仓 demo。
- **最小执行矩阵**：`CAPTE-BENEFIT` 后续获准执行时必须在同一谱系与宿主对象图覆盖 `LINEAGE / ASSEMBLY / SCHEMA_TX / SETTLE / REPLAY_CONFLICT / REFUND / FAILURE / UNKNOWN_CONTEXT_REBUILD / NONE`。settle 必须形成唯一 FundsTransaction、平衡账务、指定余额与 Consumer 耐久引用；refund 必须按真实原 settle 做 40+60 累计、重复复用、超限/篡改阻断；UNKNOWN 在 L3 中必须关闭并重建 Consumer Spring 集成上下文、复用同一耐久测试数据库，只查原 action identity且零新 attempt/逆向。该证据不等于 L4 目标部署进程/容器基于目标数据库的真实重启，二者不得互代。Provider direct call、内部 Recording/Fake 或测试短路不能关闭这些格子。
- **执行前源码校准**：Capte 业务模块只依赖 core/transaction-face/wallet-face，集中 tests 另依赖三个 impl；`OrderCouponRedemptionIntegrationTests` 使用显式 `@Import` 和 `FlexTransactionManager`，用于证明通用模块的跨仓集成对象图，不代表独立部署应用。执行前 loaded-origin helper 在 `wind-funds.version` 缺失时跳过且只比路径、不比 hash；这些测试宿主缺口已由 `plan-r2.68` 当前 L3 补强。`plan-r2.69` 进一步确认 `capte-domain` 没有独立生产数据库或部署进程，真实装配只属于未来具体 Consumer。历史 Snapshot hash、旧编译/测试错误和类加载探针全部降为 provenance。
- **Checker 与停止线**：独立 Checker 初审发现两项 P1：runtime lineage 未逐项覆盖 core，L3 context rebuild 与 L4 deployment restart 混层；修正后又发现 source fingerprint 与 JAR SHA 比较的一项 P2。最终收敛为 source 经构建日志绑定 binary、每个承重 core/face/impl 的 binary/resolved/loaded hash 逐项相等，L3 复用耐久测试库重建 Consumer context、L4 在目标 DB 上重启部署进程，复核为 `PASS / 0 P0-P2`。正式 Maven 命令、隔离仓库/唯一版本、跨仓读写和测试授权由 Consumer Host Owner 的执行 Entry Card 冻结；计划准出时 `L3_EXECUTION_GRANT=NO`，随后由 `plan-r2.67` Entry Card 获得本轮 L3 授权。source/dirty、lineage、Bean/proxy、Mapper/schema/tx、耐久原事实/action identity 或 UNKNOWN recovery 任一不清即停止；不关闭未被本卡实际证明的 HOST/E4/E5、VC，不批准 RED、API、实现、Git、enable、release 或 production。
- **Execution Entry Card**：`plan-r2.67 / ENTRY_CARD_FROZEN / L3_EXECUTION_GRANT_YES`。Provider=`eb120918... + combined dirty c4c0bbd4...f478196`，Consumer=`87925eb... + clean combined d3bd0bc1...f1753165`，唯一版本=`1.0.0-l3-w402-20260814-eb120918-c4c0bbd4-r1`，JDK=`Corretto 21.0.11`，Maven=`3.6.3`。七制品闭包、离线全 reactor 构建、Capte compile/dependency/test、fresh Surefire、两进程 file-H2 context rebuild 和 ASSEMBLY 失败上限详见 TDD `20.7.4`。授权不包含联网、Git、生产 API/DDL/生产配置、L4、enable/release/production。fresh 结果产生前状态仅为 `IN_PROGRESS`，不宣称 L3 PASS。
- **执行结论**：`plan-r2.68 / W4-02_CAPTE_BENEFIT_L3_CHECKER_PASS / CAPTE_BENEFIT_E4_PASS / 0 P0-P2`。权威 build-start combined fingerprint=`e6b34737...e8d`；版本名中的 `c4c0bbd4` 仅为授权阶段标签。七制品 built/resolved/loaded 谱系、测试宿主 Bean/proxy/Mapper/schema/单一 DataSource 与 transaction manager、settle/refund/failure/NONE、两个独立 Maven/JVM/Spring context 共用 file-H2 的 UNKNOWN recovery 均已 fresh 关闭；完整日志、SHA、报告与修正记录见 TDD `20.7.4.1`。PASS 覆盖 `capte-domain` 通用模块的 Benefit 集成测试切片；该仓没有独立生产数据库或部署进程，因此不把 production composition root、正式 migration 或 L4 作为本通用模块的未完成交付。首个真实可部署 Consumer 的装配、数据库、`SPECIFIED` 配置和部署证据在其接入时独立准入。

### 8.27 `W5-01` ActionFact Foundation 实现切片

- **状态与目的**：`ENTRY_CARD_INDEPENDENT_CHECKER_PASS / CONTRACT_INQUIRY_ACCEPTED / RED_CHECKER_PASS / GREEN_EXECUTION_GRANT_YES / W5-01_GREEN_CHECKER_PASS`。Human Owner 明确接受修订后的两条查询签名并授权 Green；Green rework 3 已经独立 Checker 判定 `PASS / 0 P0-P2`。该 PASS 状态回写现已关闭；当前切片见 `8.28`。原重构主线没有被真实 Consumer 的 L4 等待阻断，`P-SIM03-HOST` 继续作为独立宿主子轨延后。
- **最小范围**：覆盖当前 `FundsDirectTransactionService.pay(...)` 的完整可观察动作语义，而不是按调用场景分叉。无费成功在一个 Intent/Attempt 下形成一个本金 `primary / proven-full` ActionFact；带费成功固定形成本金与手续费两条独立 `primary / proven-full` ActionFact，并与同一 Ledger/Balance 本地事务原子闭合；`companion` 只描述第二条 fee `primary`，不新增 action kind、role 或 Public API。失败只有在 root/detail 全部 FAILED、全部 sibling 错误为本地 `LEDGER_POSTING_REJECTED`、无 ledger ref 且全部资金累计为零时才共同形成 `FAILED + proven-zero`；泛化失败码、事实缺失/篡改或任一 sibling 未知都必须空查询并继续受 Q-004 约束。root/detail 只保留 Intent 聚合与参与方事实，不再单独充当动作完成证据。
- **真实调用与回归边界**：当前仓内唯一生产 `pay(...)` caller 是 `FundsBenefitContributionTransactionServiceImpl#settle`；PaymentInstrument 与 ExternalFundsEvent 调用的是同接口/实现的 `topup(...)`。Benefit 产品政策不进入 W5-01，但现有 settle -> PAY 必须随行回归，不能因场景 out-of-scope 而删除真实调用证据。fee-bearing PAY 与 post-admission FAILED 是同一 public method 的现存语义，必须与无费成功共同迁移，避免形成 root/detail 与 ActionFact 两套真相。
- **幂等与查询**：同 action identity、同 semantic digest 必须查询并复用同一 ActionFact；同 identity 但 tenant、Money、责任、参与方、fee primary 或承重语义不同必须冲突，且零新增 action、route、ledger、balance。带费确定失败的本金/fee 两条 identity 必须共同终结、同摘要共同复用，任何一条未知都继续受 Q-004 零新 Attempt 约束。确定失败只有在本层证明零效果后才记录 `proven-zero`；同步返回、root state、detail 存在或 `FAILED` 标签不能代替 ActionFact 查询。
- **明确不做**：本切片不引入新 facade、registry、兼容重载、通用 action-group 引擎、双写双读或第二交易内核；不覆盖 refund/多原事实、authorize/complete/release、partial/unknown 执行、外部 rail、Benefit 产品政策、stage/reconciliation、Consumer HOST/L4。空 fee/舍入零等准入前拒绝继续零 Attempt/ActionFact/资金账务；其他 partial/unknown 合同待自然承重切片进入 RED。
- **准入与停止线**：权威 Entry Card 见 TDD `20.8`。RED 阶段当时只允许 TDD 白名单测试与状态文档，并以复用现有 `FundsTransactionQueryService` 的最小 ActionFact 查询作为可失败目标；查询签名、DTO、Green Grant 与 Green Checker 现均已关闭。`core/api-baseline/stable-api.txt` 只覆盖 core，本切片 face 契约无需修改该文件，既有 dirty 内容保持不动；其余命中 core/entity 仍为 `avoid`。当时未批准新 DDL、新持久化形态或下一切片；下一切片现已按 `8.28` 独立形成 Entry Card，但仍不得直接进入 RED、Git、enable/release/production。
- **签名与最小物理边界**：Human Owner 接受 `queryFundsActionFacts(FundsActionFactQuery) -> List<FundsActionFactDTO>` 与 `findFundsActionFact(FundsActionFactRef) -> Optional<FundsActionFactDTO>`。`FundsActionFactDTO` 只暴露 action identity、Intent/Attempt 引用、`primary`、Money、Owner-local `DomainOutcome`、`FundsEffect`、semantic digest 和 route provenance；不证明 Ledger、Balance、外部 finality 或 reconciliation。当前不新增 ActionFact 表、Mapper 或第二写链，而由唯一查询实现把同一 PAY root 下已终结且一致的 detail + route snapshot 事实组投影为 canonical ActionFact；处理中、混合结果、无 route 或非 PAY 均返回空，且空结果不得推断零效果。
- **RED fresh evidence**：Java 21 下执行 `just verify-slice FundsActionFactContractTests,FundsDirectTransactionFlowTests,FundsTransactionRateFeeFlowTests,FundsBenefitContributionTransactionServiceFlowTests tests`，测试源码编译成功，Surefire 共 `89 tests / 9 failures / 0 errors / 0 skipped`；四类分别为 `1/1`、`70/3`、`5/4`、`13/1`，全部失败均指向 `FundsTransactionQueryService` 缺少 `findFundsActionFactsByBusiness`，其余 80 个用例通过。首轮 Checker 指出的带费首次/重放 identity、fee 语义冲突、舍入零 ActionFact 和候选签名口径均已修订并重新观察红灯；最终独立 Checker 判定 `PASS / 0 P0-P2`。该红灯不是 Maven、Java、Spring 或 H2 环境故障。
- **修订签名 RED 与 Green rework 3 fresh evidence**：签名接受后先把 RED 修订为 query/ref DTO 形状，同一聚焦命令仍为 `89 tests / 9 failures / 0 errors / 0 skipped`，9 项均精准缺 `queryFundsActionFacts`。首轮 Green 为 `89/0/0/0`；第一轮 rework 为 `92/0/0/0`；第二轮 rework 为 `93/0/0/0`，但 Checker 又发现持久 RouteSnapshot 缺失 `participant.subjectRef` 时查询会抛 NPE 的唯一 P1。Green rework 3 只在共享 participant/node/Money 匹配边界增加 null guard，并补齐两个 ActionFact 查询入口的 fail-closed 回归；同一 slice 保持 `93/0/0/0`（`1 + 74 + 5 + 13`），`just test-transaction=150/0/0/0`、Java 21 `just compile=21/21`、Java 21 `just pmd` 与 `git diff --check` 通过。原 PAY ActionFact 在部分/全额退款后保持 identity/digest/route 不变；PAY/FEE route 必须严格匹配各自责任端点与 tenant/币种；ActionFact digest 由 projection-owned `transaction.action.pay.projection.v1` 覆盖字段独立计算，不暴露或误标 detail request hash。`plan-r2.76` 的 Wallet snapshot `state/status` 边界失败仍独立保留；`core/api-baseline/stable-api.txt` 只覆盖 core，本次 face 契约无需写入，既有 dirty 内容保持不动。

### 8.28 `W5-01-DIRECT-PRIMARY-RECOVERY-ACTIONFACT` Entry Card

- **状态与目标**：`ENTRY_CARD_INDEPENDENT_CHECKER_PASS / CONTRACT_INQUIRY_ACCEPTED / RED_EXECUTION_GRANT_YES / RED_REWORK_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_YES / W5-01_DIRECT_PRIMARY_RECOVERY_GREEN_CHECKER_PASS`。Human Owner 回复“按你的建议推”，接受已冻结的最小 Java 形态并授权 Green；首轮 Green Checker 的一项 P1 已完成最小 rework，最终独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。
- **稳定语义与真实路径**：物理方法 `FundsDirectTransactionService.refund(...)` 不决定公共 action kind。逆向 successful primary 映射为 `recovery/adjustment`，逆向 successful complete 才映射为 `refund`。当前 `FundsBenefitContributionTransactionServiceImpl#refund` 会把真实原 Benefit 资金流水映射为 `referenceTransactionSn`，因此是随行回归 Consumer；Benefit 产品策略和 HOST/L4 仍独立阻断。
- **最小适用谓词**：`referenceTransactionSn` 物理引用的 PAY root 必须无费，并且恰好可投影一条同 tenant、同币种、`primary/proven-full` principal ActionFact；`feeChargeSpec`、`accountId`、`payerId`、`payerLedgerSubjectCode` 均为空。原 PAY 带 fee、投影缺失/多命中或其他条件不满足时 fail-closed；本切片不新增 root -> principal 选择规则。成功恢复事实保存原 fact + allocated Money + 原 responsibility/route provenance，并按原事实累计；同摘要重放复用，异摘要冲突，UNKNOWN 只查原 action identity。
- **明确排除与 Inquiry**：无原引用的业务确认型直接退款不能冒充 reverse，必须另行签收 normalized authority 或目标移除；原 PAY 带 fee 或本次新增手续费会同时承重 principal recovery 与 fee primary sibling，在其原子/失败/UNKNOWN 合同获 Owner 接受前硬停止。本卡不覆盖多原事实、历史手续费退款、authorization/complete refund、外部 raw fact、Consumer 改签、通用 recovery engine、DDL/Mapper/新写链。Entry Card 不预批 Java/API 物理形态；Contract 接受并取得后续独立授权后，eventual Green 才可最小调整 `transaction-face` DTO/query。
- **金额与验收种子**：`pay100 -> recovery30 -> recovery20` 后原 PAY 仍为 `primary/proven-full 100`，两条恢复事实分别可查询，累计 `50`、剩余 `50`；重复不新增，超限、错原事实、错 tenant/币种/route 或同 identity 异语义均拒绝且零新增 ActionFact/route/Ledger/Balance。两个不同 action identity 并发 `recovery60 + recovery60` 竞争同一原 PAY 剩余额度时只能一个成功；胜者形成唯一 recovery ActionFact，原 PAY 不变，败者零新增 ActionFact/route/Ledger/Balance。失败只有本地耐久合取证据证明终结且零效果时才可 `proven-zero`，其余为空/UNKNOWN。
- **Green 白名单与停止线**：权威卡片见 TDD `20.9`。Human Owner 只授权最小调整 `FundsActionFactDTO`、`DefaultFundsTransactionQueryService`、`FundsTransactionCommandServiceImpl` 及随行契约/流程测试；未新增 DDL、Mapper、写入链、场景 facade、Consumer 改签或 API baseline。Git、L4、enable/release/production 仍未授权。
- **Fresh RED evidence**：首轮 `91/6/0/0` 经 Checker 判定 `NOT PASS / 0 P0 / 3 P1 / 1 P2`。rework 后 Java 21 聚焦 slice=`95/8/0/0`，类级为 `2/1`、`80/6`、`13/1`，其余 `87` 个用例通过；八个精准失败只指向 recovery DTO/投影、posting 拒绝 proven-zero、Benefit recovery 与原 PAY/本次 fee 两条停止线。现有物理事实已实际执行并通过 Money/context/original fact 冲突、第二笔累计、wrong tenant/malformed ref、PROCESSING/混合错误的业务查询、错币种、non-PAY、非 proven-full 与 unreferenced 边界；依赖目标 recovery ActionFact identity/digest/provenance 的后置断言仍等待 Green fresh 执行，不计入本轮通过证据。完整 before/after 与 forbidden fingerprint 见 TDD `20.9` manifest；无生产 Java/API/DDL/Mapper、API baseline 或 Consumer 修改。独立 Checker 最终判定 `PASS / 0 P0-P2`；该 PASS 只准入 Green Grant 决策，不是 Green 授权。
- **Fresh Green evidence**：2026-08-17 使用 Java 21 执行同一聚焦 slice，`FundsActionFactContractTests=2/0/0/0`、`FundsDirectTransactionFlowTests=80/0/0/0`、`FundsBenefitContributionTransactionServiceFlowTests=13/0/0/0`，合计 `95/0/0/0`；`just test-transaction=156/0/0/0`，`just compile=21/21`，`just pmd` 与 `git diff --check` 通过。扩大门禁未被冒充通过：`just test-boundary=200/1/0/0` 仅失败于既有 `PaymentInstrument` history `state/status` 断言，`just verify-cad` 在同一既有 `FundsAccount#getStatus -> getState` core API baseline 差异处停止；二者作为独立 blocker 保留，本切片未修改 wallet/core baseline。该阶段证据只证明当前 Provider/H2 recovery 切片满足已接受合同；首轮 Green Checker 后已由下一条 rework 证据承接，真实 Consumer HOST/L4、发布和生产仍未关闭。
- **Green Checker 与 rework evidence**：首轮独立 Checker 判定 `NOT PASS / 0 P0 / 1 P1 / 0 P2`：当前 recovery 自身事实虽一致，但没有同时证明本次 RESTORE route 是原 PAY route 的精确反向、allocated Money 在原本金范围内、原 PAY `refundedAmount` 与同一原 root 下全部已证明成功 recovery 累计一致。Maker 先用一个耐久篡改用例同时观察 route、amount、cumulative 三类列表查询与 identity 查询泄漏，共 `6` 个精准红灯；随后只在现有只读投影增加三项合取，不新增表、Mapper 或写链。rework 后聚焦 slice=`96/0/0/0`（`2 + 81 + 13`），`just test-transaction=157/0/0/0`、`just compile=21/21`、`just pmd` 与 `git diff --check` 通过。篡改 route 端点、放大 recovery Money 或破坏原 root 累计时，两种查询均 fail-closed；最终独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。

### 8.29 `W5-01-AUTHORIZATION-ROOT-ACTIONFACT` Entry Card

- **状态与选择理由**：`ENTRY_CARD_INDEPENDENT_CHECKER_PASS / CONTRACT_INQUIRY_ACCEPTED / RED_EXECUTION_GRANT_YES / RED_EXECUTION_COMPLETE / RED_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_YES / GREEN_IMPLEMENTATION_COMPLETE / W5-01_AUTHORIZATION_ROOT_GREEN_CHECKER_PASS`。Human Owner 在 RED Checker PASS 后明确回复“可以，推进吧”，授权最小 Green；rework 最终经独立 Checker 判定 `PASS / 0 P0-P2`。Maker 只把一次授权根投影为稳定 ActionFact；它是后续 complete/release 引用的必要原事实，且比一次引入完整授权生命周期、Intent/Attempt 持久化或外部 authority 更小。
- **公共边界**：只接受已经通过 Funds 自身 tenant、Money、责任、账户、route 和幂等准入的内部授权事实。卡、VCC、订单、外部 issuer 状态、authority/finality 和宿主流程仍在上游；本卡不新增场景对象、rail 策略或公共引擎。
- **最小适用谓词**：仅处理 `FundsTransactionMode.AUTHORIZATION + DefaultFundsTransactionType.PAY + original event AUTHORIZE` 的根授权。只选原 `AUTHORIZE` details 与原 RouteSnapshot participants 做完整双射；后继 COMPLETE/REVERSAL/AUTH_REFUND details 不参与授权事实投影。root、原 details、participants 与 RouteSnapshot 必须同 tenant、同业务身份、同币 Money、责任关系一致，且 route 语义为 `AUTHORIZATION_TRANSACTION / AUTHORIZE / PAY`。成功时稳定 `authorizedAmount` 必须等于授权 ActionFact Money；force/no-auth、其他 mode/type、raw external fact 均排除。
- **成功事实**：首次成功授权形成恰好一条 `authorize / succeeded / proven-full` ActionFact，Money 与 proven Money 都等于本次授权处置金额，`originalFundsFactRefs` 为空，route provenance 表示首次执行。SHARED 模式下必须恰有一个 Credit 子责任 participant 与一个父 Funding participant；Credit hierarchy parent 必须精确指向该 Funding participant；二者各与一条同 tenant、同币、同 Money=`60` 的 HOLD self-leg 完整双射。它们由同一冻结 RouteSnapshot 的多责任 provenance 承载，不拆成两个 ActionFact，也不得把两侧 `60 + 60` 相加为授权 `120`；digest 必须承重该 participant/hierarchy/leg 关系。
- **拒绝与 UNKNOWN**：只有 root=`REJECTED`、全部授权 details=`REJECTED`、RouteSnapshot 无 legs、transaction-owned details 无 LedgerFactRef，且 authorized/completed/reversed/refunded/declined 等资金累计均为零时，才可投影 `authorize / rejected / proven-zero`；`provenMoney` 为空。`REJECTED/FAILED` 标签、同步异常或 root/detail 单独存在都不证明零效果。PROCESSING、transaction-owned root/detail/route/ledger-reference 事实缺失、混合或冲突以及查询不可用一律返回空/UNKNOWN，只沿原 action identity 查询。实际 Ledger/Balance 不由 ActionFact DTO 或本查询证明，零 posting/entry/balance 与账务平衡由流程 RED 独立断言。
- **耐久与后继边界**：成功授权的 outcome/effect/digest 只由稳定 `authorizedAmount`、原 `AUTHORIZE` detail 组、冻结 RouteSnapshot 与该次授权 LedgerFactRef 合取，不读取会被后继动作改变的 root lifecycle state 或 completed/reversed/refunded 累计。同业务/action identity、同 semantic digest 重放同一事实；异 tenant/Money/责任/route/摘要冲突且零新增。后续 complete/release/refund 对这些后继累计的正常变化或篡改均不得改变原 authorization ActionFact；后继动作与 aggregate integrity 由各自事实/门禁处理，不在本切片实现。
- **验收种子**：Funding 可用 `100` 授权 `60`；同摘要重放 identity 不变，异摘要冲突零新增；SHARED Credit+父 Funding 仍只有一条授权 ActionFact；拒绝路径必须由流程测试证明零 legs/posting/entry/balance/declined；授权后 partial complete/release/refund 仍能读到完全相同的原授权 ActionFact。`authorizedAmount`、原 participant/hierarchy/HOLD leg、Money、route 或 LedgerFactRef 篡改及 PROCESSING 必须使业务列表与 identity 查询均 fail-closed；后继 completed/reversed/refunded 变化不属于该查询的篡改条件。
- **最小物理候选**：优先复用现有 `FundsTransactionQueryService` 两个 ActionFact 查询和 `FundsActionFactDTO`，不新增查询方法、DTO、表、Mapper 或第二写链。eventual Green 若能成立，只允许在 `DefaultFundsTransactionQueryService` 增加 authorization-root 只读投影，并补 `FundsActionFactContractTests` 与 `FundsAuthorizationTransactionFlowTests`；`FundsTransactionFlowTestSupport` 默认复用不改。
- **Dirty/白名单冻结**：当前 SHA-256：query face=`babdda...a1a61`（avoid）、DTO=`fb7062...26ea`（avoid）、query impl=`b5e6ed...713d6`（eventual candidate）、`tests/.../services/FundsActionFactContractTests.java=51467a...d246`、authorization flow=`af371e...85a`、flow support=`68232b...9e61a`（avoid）。`FundsTransactionQueryServiceContractTests.java=4740b8...0a24` 是另一既有回归文件，不是本卡 contract-test 候选。RED 前必须补全值、重算全白名单与 forbidden-scope fingerprint，并逐项登记 Owner/disposition；本卡不把当前 dirty 文件冒充干净基线。
- **RED 授权与停止线**：只允许修改四份权威状态文档与 `FundsAuthorizationTransactionFlowTests.java`，执行聚焦 RED、compile、PMD 与 diff check；不修改 contract/support 测试、生产 Java/API/DDL/Mapper、Consumer 或 Git。RED 只能精准失败于 authorization ActionFact 投影缺失；fresh 证据形成后进入独立 Checker，不得直接进入 Green。
- **Fresh RED evidence**：Java 21 聚焦 slice 共 `45 tests / 5 failures / 0 errors / 0 skipped`：`FundsAuthorizationTransactionFlowTests=43/5/0/0`、`FundsActionFactContractTests=2/0/0/0`。五个失败分别覆盖成功、拒绝、SHARED、生命周期后可读和耐久篡改入口，均精准停在首次 authorization ActionFact 查询返回空；其后的重放、后继生命周期稳定性与篡改 fail-closed 断言已编码但在 RED 阶段尚未越过首个目标失败，不计入 `40` 个通过用例。`just compile=21/21`、`just pmd`、`git diff --check` 通过；测试文件 SHA-256=`888de96d927815720539f10c8d68823003f36bb2df0324fa6970b05328026b79`，两份 Surefire XML 分别为 `e0d51a675e2acbe9121f18b4d102ad98ac9a20c6483bcdaba6bdb1a9eac4bf64`、`dd1372edef314cb12be2a259025394b8573723c221e63713905e10b050d18cd1`；production/Public API/schema forbidden fingerprint 仍为 `7a99455fc08bd9f033170fd57e4524b436c868db0e8a77cb1fcccb897baf56a7`。
- **Fresh Green evidence**：Maker 只修改 `DefaultFundsTransactionQueryService` 的既有只读投影和 `FundsAuthorizationTransactionFlowTests`，复用现有查询/DTO，不新增 face 方法、DTO、DDL、Mapper、写链或场景 facade。实现按原 `AUTHORIZE` details、冻结 participant/hierarchy/HOLD legs、LedgerFactRef 与稳定 Money 合取投影成功或已证明零效果；缺 leg、route/责任/Money/ledger ref 篡改、混合状态均双查询 fail-closed。Java 21 聚焦 slice=`45/0/0/0`（authorization=`43/0/0/0`、contract=`2/0/0/0`），`just test-transaction=162/0/0/0`、`just compile=21/21`、`just pmd` 与 `git diff --check` 通过。最终 SHA-256：query impl=`2f92b06dee1169da0e72b997238f2e00a8055bdcfe2bd202ab36ed583406dffb`、authorization flow=`9247f9731c3fca3064a7a7f8809ddb1a5259b84e75a3c39dd59f640567e855ce`、authorization XML=`f62fb19248035bf33d5cad52d6e54d24d96480b8beee5a7532f642c03cb8115b`、contract XML=`5560ca3fba510afcc88715d67270667a48d79e14e9de361b1107400bcc6b40cb`；query face、DTO 与 contract test 保持既有 SHA 不变。当前只进入独立 Green Checker，不预写 PASS。
- **Green Checker 与 rework evidence**：首轮独立 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 0 P2`：成功 authorization ActionFact 仍读取可变 root lifecycle state 与 completed/reversed/refunded 累计；单 participant 责任未限定 Funding 模式，SHARED parent currency 又可被空值当作 wildcard。Maker 先补三项精准 RED，fresh 观察 `45/3/0/0`；随后只收紧既有只读投影：成功事实不再依赖后继状态/累计，Funding 必须是 `PAYER + FUNDING_ACCOUNT`，SHARED 必须是 `AUTH_HOLDER + CREDIT_ACCOUNT` 和 `REAL_FUNDING_SOURCE + FUNDING_ACCOUNT`，hierarchy parent 与 Funding participant 的 tenant/type/id/currency 必须精确一致。另补缺失 `participant.subjectRef` 与 SHARED Funding participant 顶层 currency 缺失的双查询 fail-closed 防回归；前者复用共享 participant guard，后者冻结当前 RouteParticipant 的权威币种，不要求既有可选 `SubjectRef.currency`。rework 后 Java 21 聚焦 slice=`45/0/0/0`、`just test-transaction=162/0/0/0`、`just compile=21/21`、`just pmd` 与 `git diff --check` 通过；query impl SHA-256=`17233df4bc02f66e55a4cc4cc3dc8aff5643434f82d885b77bd4b4835f4a5ea5`、authorization flow=`6255309efac1df81b8b1c4f251095ad483fcf130e1a0f1091312f3d87da0b2b1`、authorization XML=`8346320f1f2a3fa862b67701a6442422a7227faf25120b38254aa88b6545415a`、contract XML=`30d2143079f08280adda9af3bd959594c9c3765fdf6cd416826013ac5fc926a4`。最终独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`；该 PASS 只关闭本切片，下一 W5 Slice 仍须独立 Entry Card。

### 8.30 `W5-01-AUTHORIZATION-COMPLETE-ACTIONFACT` Entry Card

- **状态与选择理由**：`ENTRY_CARD_INDEPENDENT_CHECKER_PASS / CONTRACT_INQUIRY_ACCEPTED / RED_EXECUTION_GRANT_YES / RED_EXECUTION_COMPLETE / RED_REWORK_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_YES / GREEN_IMPLEMENTATION_COMPLETE / W5-01_AUTHORIZATION_COMPLETE_GREEN_CHECKER_PASS`。Human Owner 按建议推进，接受 `CI-W5-01-AUTHORIZATION-COMPLETE-001` 并授权最小 Green；Green rework Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。本卡只处理普通授权后完成 `complete`，不把 release、refund、force/standalone completion 或完整 Intent/Attempt 持久化一起带入。
- **公共边界**：上游只提交已经归一的 complete 资金动作；issuer/PSP authority、capture/presentment/finality、卡或订单状态仍归上游。Funds 独立校验 tenant、Money、原授权、冻结责任/route、逐授权累计、幂等、账务和余额。本卡不增加场景对象、raw rail 字段、策略路由或通用 lifecycle engine。
- **最小适用谓词**：仅接受 `AUTHORIZATION_TRANSACTION + COMPLETE + PAY`、非 force、引用一个同 tenant/同币且可投影为 `authorize/proven-full` 的原授权。每个 `businessScene + businessSn + COMPLETE` 的全部 sibling details 是一个动作组；组内业务身份、event/type、Money、责任 participant、原 root/原 authorization Ledger 引用和本次 Ledger 引用必须完整一致。内部 detail `requestHash` 不是 ActionFact 投影证据，也不进入 projection digest；同键重放与冲突继续由既有写链独立验证。一个动作组只形成一条 complete ActionFact，多 participant 的金额不得相加。
- **成功事实与原引用**：成功组形成 `complete / succeeded / proven-full`，ActionFact Money 与 proven Money 均等于本次 complete Money；恰有一个 `OriginalFundsFactRef` 指向原 authorization ActionFact，`allocatedMoney` 等于本次 Money。责任组必须由原授权 RouteSnapshot 的全部 replayable HOLD legs 推导：每个原责任 participant 恰好映射一个本次 source sibling，每个被消费原 leg 恰好有一份同额 replay provenance，派生 capture target participant/CONSUME leg 也必须无重无漏；Funding 为“原责任方 + settlement”，SHARED 为“Credit + 父 Funding + settlement”。`FundsRouteProvenance` 只引用原授权冻结 RouteSnapshot；caller 不选择 route，也不要求新增 successor RouteSnapshot、表或写链。
- **身份、查询与耐久性**：业务查询只返回该 complete 业务身份对应的动作，不把 authorization root 下其他 complete/release/refund 混入；原 authorization 业务查询仍只返回 authorization ActionFact。`intentRef` 继续指向 authorization root，`attemptRef` 对应本次 `businessScene + businessSn + COMPLETE` 动作组；complete identity 由二者唯一确定。semantic digest 覆盖 Money、原 fact、责任 siblings、原 route/replay provenance 和 Ledger refs，但不吸收内部 request hash 或会随其他后继动作变化的 root aggregate。同 identity+digest 重放同一事实，异 Money/原事实/责任/route/摘要冲突。后续 complete、release 或 refund 不得改写已形成 complete ActionFact。
- **累计与完整性**：每条 successful complete 的 Money 独立可复算；同一授权根的全部已证明成功 complete 累计必须等于 root `completedAmount`，且 `completed + reversed <= authorized`。新增合法 complete 只追加新 ActionFact；root 累计、任一成功 sibling、原引用或 replay provenance 缺失/篡改时，同一授权根下 complete 投影整体 fail-closed，不能用当前余额或聚合状态补推单笔动作。
- **失败与 UNKNOWN**：首切不把 `FAILED/REJECTED` 标签投影为 proven-zero。PROCESSING、失败、混合 sibling、缺失 Ledger 引用、账务结果未知、错 tenant/币种/责任/原事实/route、force/standalone completion 均返回空/UNKNOWN，只沿原动作 identity 查询；不得换业务键重做。失败零效果若未来需要，必须单独证明耐久失败合取并重新成卡。
- **最小物理候选与 Inquiry**：`CI-W5-01-AUTHORIZATION-COMPLETE-001 / ACCEPTED`。复用 `FundsTransactionQueryService` 两个现有查询和 `FundsActionFactDTO`；`identity=<rootSn>:complete:<businessScene>:<businessSn>`、`factType=funds-action`、`relationRole=consumes-authorized-effect`、`provenanceRole=replayed-original-route`、digest domain/version=`transaction.action.complete.projection(.v1)`。eventual Green 最多修改 `DefaultFundsTransactionQueryService` 的只读投影与 `FundsAuthorizationTransactionFlowTests`；不新增 query method、DTO、DDL、Mapper、ActionFact 写链、兼容 facade 或 Consumer 改签。内部 request hash 不属于公共投影合同。
- **验收种子**：`authorize100 -> complete30 -> complete50 -> release20 -> refund20`。两条 complete identity 分离且分别保持 Money `30/50`，累计 `80`，release/refund 后原 complete 事实不变；同摘要重放零新增、异摘要冲突；SHARED complete60 仍是一条 ActionFact 而非 `120`。超限、并发竞争、wrong tenant、malformed ref，以及 detail state/reference/root cumulative/replay leg identity/amount/Ledger ref 篡改，必须使业务列表和 identity 查询均 fail-closed；缺失/重复/交换原责任 sibling、capture target sibling 或 replay leg 也必须双查询 fail-closed。request hash 不做不可重建的 canonical tamper 判定。所有拒绝路径断言零新增 transaction/detail/route/ledger/balance。
- **Green 与停止线**：Maker 只修改既有 `DefaultFundsTransactionQueryService` 只读投影与 `FundsAuthorizationTransactionFlowTests`。首轮 Green Checker 的 `3 P1 + 1 P2` 已以最小 rework 关闭：`requestHash` 退出准入，replay Money 精确比较并拒绝小数截断，ordinary complete 显式拒绝 FORCE marker，release/refund 前后 complete DTO 精确不变。fresh focused=`54/0`、transaction=`171/0`、compile=`21/21`，PMD 与 `git diff --check` 通过；最终 Checker=`PASS / 0 P0-P2`。本卡不新增 Public API/DTO/DDL/Mapper/写链/Consumer；下一 W5 Slice 必须独立 Entry Card，Git、HOST/L4、enable/release/production 均未授权。

### 8.31 `W5-DOCS-COMPLETION-REVIEW` 重构迁移基线

- **触发原因**：W1 产品、W2 DSL、W3 系分和 W4 TDD 已先于 W5 通过 Checker，但 W5 连续四个 Green 切片仍依赖 Checker 补齐物理事实与边界，说明分模块目标设计尚未被收敛为完整迁移顺序。继续按动作追加查询分支会把实现期变成设计补课，并可能把 `DefaultFundsTransactionQueryService` 扩成通用历史事实解释器。
- **历史 Surface 状态**：MIG-07 无兼容 breaking release 与 MIG-05A 均已完成并通过独立 Checker；相关授权均已耗尽。MIG-02B、MIG-03、MIG-04 与 MIG-05C 当时范围均已 Green。Human Owner 已接受 MIG-05D A，Contract Surface RED/Green 已完成并通过独立 Checker；当时进入 `plan-r2.251 / W5-MIG05D-LEDGER-POSTING-COMMAND-A-BEHAVIORAL-RED-GATE / CI-W5-MIG05D-LEDGER-POSTING-COMMAND-001-A / CONTRACT_SURFACE_GREEN_INDEPENDENT_CHECKER_PASS / BEHAVIORAL_RED_EXECUTION_GRANT_NO / CODE_FREEZE`，随后已由顶部 Metadata 与恢复入口的 `plan-r2.256` 接替。
- **权威迁移总表**：以系分第十一章为准，覆盖 `MIG-00~09`。分模块 Public API 处置清册继续负责对象级 `KEEP/NARROW/MOVE/REPLACE/REMOVE`；本节只持有跨模块顺序、当前状态、Owner、准出和停止条件，不复制第二份对象清册。

#### 8.31.1 当前覆盖与剩余顺序

| 顺序 | 交付结果 | 状态 | 下一门禁 |
| --- | --- | --- | --- |
| `MIG-00/01` | 产品/DSL/系分/TDD 基线与 ActionFact 公共只读查询基础。 | `DONE_FOR_CURRENT_SCOPE` | 不重开已接受产品语义和查询合同。 |
| `MIG-02A` | direct `primary`、direct `recovery/adjustment`、authorization `authorize`、ordinary `complete`。 | `FOUR_KINDS_PROVIDER_GREEN_CHECKER_PASS` | release 已由 MIG-02B 独立 Green；canonical refund 按 Owner 决策延期。 |
| `D-MIG-001` | ActionFact 物理承接：既有耐久事实投影或独立耐久写入。 | `A_ACCEPTED / ACCEPTANCE_CHECKER_PASS / D-MIG-001-R-A_ACCEPTANCE_CHECKER_PASS` | refund 的逐 complete 分配无法由当前耐久事实证明；Human Owner 已选择延期 canonical refund 投影。 |
| `MIG-02B` | authorization `release` 的文档卡，限定未完成授权范围。 | `DOCUMENT_CARD_CHECKER_PASS / RELEASE_ACTIONFACT_CONTRACT_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_PASS / RED_REWORK_INDEPENDENT_CHECKER_PASS / GREEN_INDEPENDENT_CHECKER_PASS` | 当前 Provider 范围完成；Consumer 接入与外部 finality 不在本卡。 |
| `MIG-02C` | authorization `refund` 文档卡，覆盖 `1..n` successful complete 的逐笔分配、原 route 和累计。 | `DOCUMENT_CARD_CHECKER_PASS / CURRENT_E2_DURABLE_ALLOCATION_GAP` | 目标合同与阻断结论已准出；现有 root-level refund 不得投影 canonical fact。 |
| `MIG-03` | Action/Ledger/required Balance closure、三层 persisted v1、同 key replay、全部 exact read fail-closed 与 Clearing 两层测试。 | `DONE_FOR_CURRENT_SCOPE / GREEN_INDEPENDENT_CHECKER_PASS` | 三类证据保持正交；新增证据维度必须独立 Entry Card。 |
| `MIG-04` | Transaction/Wallet Owner 迁移与重复 facade/read adapter 处置。 | `SURFACE_INTERNALIZATION_GREEN_INDEPENDENT_CHECKER_PASS / CURRENT_SCOPE_COMPLETE` | PaymentInstrument/Spend 交易编排已无兼容内收到 transaction-impl，两个 wallet-face facade 与六个旧请求已删除；Wallet 稳定事实契约保留，Benefit/MIG-08 不在本切片。 |
| `MIG-05` | Ledger internalization。 | `PROVIDER_GREEN_CHECKER_PASS / CURRENT_SCOPE_COMPLETE` | projection/profile/extension surface 与高阶 posting command 已收口；不自动创建 MIG-05E，只有新的真实 Public low-level 写旁路或 Consumer 缺口才能重开。 |
| `MIG-06` | normalized external fact。 | `DESIGN_ACCEPTED / RUNTIME_ADAPTER_PENDING` | Fincone 可提供设计/模拟输入；真实 authority adapter 与运行恢复仍待 Host。 |
| `MIG-07` | reconciliation/stage。 | `DOCUMENT_CARD_REWORK_CHECKER_PASS / CONTRACT_DECISION_PACKAGE_REWORK_CHECKER_PASS / A_ACCEPTED / ACCEPTANCE_CHECKER_PASS / SOURCE_RUN_ENTRY_CARD_CHECKER_PASS / SOURCE_RUN_CONTRACT_ACCEPTED / SOURCE_RUN_CONTRACT_ACCEPTANCE_CHECKER_PASS / GATE_REQUIREMENT_ENTRY_CARD_CHECKER_PASS / GATE_REQUIREMENT_CONTRACT_ACCEPTED / GATE_REQUIREMENT_CONTRACT_ACCEPTANCE_CHECKER_PASS / BREAKING_RELEASE_RED_ENTRY_CARD_FROZEN / RED_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / BASELINE_STATUS_MAPPING_REPAIR_CHECKER_PASS / BREAKING_RELEASE_RED_ENTRY_CARD_REFREEZE_CHECKER_PASS / RED_EXECUTION_GRANT_YES / RED_EXECUTION_COMPLETE / RED_INDEPENDENT_CHECKER_PASS / GREEN_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_NOT_PASS / BEHAVIORAL_RED_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_PASS / CONTRACT_SURFACE_GREEN_INDEPENDENT_CHECKER_PASS / BEHAVIORAL_RED_INDEPENDENT_CHECKER_PASS / GREEN_INDEPENDENT_CHECKER_PASS / CODE_FREEZE` | 当前范围已完成；下一 W5 切片必须重新形成 Entry Card，Consumer E4/L4 仍独立阻断。 |
| `MIG-08` | 真实 Consumer 切换。 | `CAPTE_BENEFIT_ACTIONFACT_CONSUMER_E4_PASS_LIBRARY_TEST_HOST_ONLY / RECONCILIATION_E4_PENDING / L4_DEFERRED` | `capte-domain` Benefit ActionFact Consumer 已在 Provider r9 与不可变 library test host source card 上关闭 E4；当前仍无 Reconciliation 生产调用或独立部署数据库，其他 Consumer 与部署宿主 L4 继续等待。 |
| `MIG-09` | 删除旧 facade、重复 read adapter、raw rail entry、宽 CRUD 和无 Consumer 扩展壳。 | `IN_PROGRESS / FUNDING_ACCOUNT_QUERY_SURFACE_GREEN_CLOSEOUT_INDEPENDENT_CHECKER_PASS / CREDIT_RED_INDEPENDENT_CHECKER_PASS / RED_TEST_IMMUTABLE / GREEN_EXECUTION_GRANT_NO` | Funding Provider/Capte 已关闭；Credit RED 精准为 `3/2F/0E/0S`，下一步只允许冻结五文件 Provider Green，未授权 Consumer。 |

本表历史结论已由 plan-r2.323 Credit RED supersede：MIG-02C 仍只在权威逐 complete allocation 出现后重开；MIG-06 与其他 Adapter/Consumer 继续等待各自 E4。MIG-09 Funding Provider 与 Capte Consumer Green 已通过独立技术 Checker；Credit immutable RED 也已独立准出，下一 Gate 只能是冻结五文件 Provider Green，不能进入 Consumer、其他 raw-id 或推测性 MIG-05E。

#### 8.31.2 `D-MIG-001` Owner 决策包

选择轴只有一个：ActionFact 的目标物理事实继续由现有 durable action group 在查询时投影，还是新增独立耐久 ActionFact 写入。

| 候选 | 适用方式 | 主要收益 | 代价/红线 |
| --- | --- | --- | --- |
| `A / EXISTING-DURABLE-FACT-PROJECTION`（推荐） | 现有 root/detail/route/Ledger/Balance 仍是 durable action group，Public ActionFact 为稳定只读投影；内部按稳定 action kind 显式分治。 | 不新增表和第二写链，最符合当前公共库与 `1.0.1-SNAPSHOT` 阶段。 | 只能投影机械可证事实；partial/unknown/原事实/route 无法闭合时必须停止。不得新增 registry/factory/Public SPI。 |
| `B / DURABLE-ACTION-FACT-WRITE` | 在同一本地事务写独立 ActionFact，以其作为公共查询事实。 | 查询和动作身份直接，适合无法可靠重建的事实。 | 需要新物理载体、写链、历史迁移与回滚设计；必须证明不形成第二真相。 |
| `C / HYBRID-OLD-PROJECTION-NEW-WRITE` | 历史投影、新动作独立写入。 | 渐进切换。 | 长期双读/双真相，与已接受破坏式切换原则冲突；建议 `REJECT`。 |

三候选必须共同证明跨版本稳定性：应用升级、重启或 projector/schema 版本变化不得改变既有 ActionFact 的 identity、Money、outcome、FundsEffect、original fact refs、route provenance 或 `SemanticDigest(algorithm/value/coveredFieldsVersion)`。A 按源事实格式/动作语义版本选择 projector；`vN` 历史事实在 `vN+1` 下必须逐字段相同，或显式调用保留的 `vN` projector，禁止当前默认规则静默重解释。B 版本化独立事实 schema，并以全量回填、校验、一次切流和整版本回滚保持既有事实不变。

Human Owner 已选择 A。A 的接受范围是：现有 durable action group 继续作为唯一物理事实源，ActionFact 仅为版本化、机械可证的稳定公共只读投影；B/C 未选择且不是运行时 fallback。最初 Acceptance Checker PASS 后曾只允许设计 authorization release；release/refund 文档卡现均已完成。A Acceptance Checker PASS 后当时唯一机械进入 `MIG-04_TRANSACTION_WALLET_OWNERSHIP_DOCUMENT_CARD / DOCUMENTATION_ONLY / CODE_FREEZE`，随后形成 8.35 文档卡并已通过 MIG-04 Checker；当前入口见 Metadata 与 MIG-04 Register。若某个 release/refund 不能从现有耐久事实完整证明，该动作保持不支持/人工处理，并重开项目级 `D-MIG-001`。只有新的项目级决策证明 B 能迁移、回填并一次切换既有四类与后续全部动作时才可改选 B；不能让某个新动作写 B、既有动作继续投影 A，否则实质上就是已拒绝的 C。

#### 8.31.3 文档 Checker 门槛

独立 Checker 必须确认：

1. 产品、DSL、系分、TDD 与本 OpenSpec 的稳定 action kind、Owner、原事实、累计和 UNKNOWN 边界无冲突。
2. `MIG-02~09` 均有范围、前置、写入边界、验证、停止条件和最终删除条件，不用代码阶段补产品或架构决策。
3. `D-MIG-001` 的 A/B/C 同构，推荐未冒充 Owner 接受；C 的双轨风险未被包装成默认 fallback。
4. 当前只读投影的完成范围与剩余动作明确，未把 Green Checker PASS 外推成完整重构、Consumer E4、L4 或生产准出。
5. Checker 输入状态统一为 `plan-r2.106 / W5-DOCS-COMPLETION-REVIEW / MIGRATION_BASELINE_REWORK / DOCUMENTATION_ONLY`，所有 RED/Green、Java/API/DDL/Mapper、Consumer、Git、HOST/L4、enable/release/production 授权均为否；Checker PASS 后当前状态机械切为 `plan-r2.107 / D-MIG-001_OWNER_GATE / OWNER_DECISION_PENDING / CODE_FREEZE`。

Decision Package Checker 与 A 的 Acceptance Checker 均已判定 `PASS / 0 P0-P2`。authorization release 与 refund 文档卡均已 Checker PASS，不形成代码 Entry Card。refund 的耐久分配证据命中 `D-MIG-001-A` 重开条件后，当时进入 `D-MIG-001-R` 项目级决策包；当前入口以 Metadata 与恢复入口为准。

### 8.32 `MIG-02B` authorization release 纯文档卡

- **单一范围**：只覆盖 ordinary authorization 未完成范围的 `release`；不覆盖 balance unfreeze、settlement release、payout failure、refund、force/no-auth 或 expired/timeout 自动释放。
- **稳定合同**：release 是唯一引用成功 authorization 的独立 reverse Intent；release Attempt 属于该 reverse Intent，Money 为正，沿原责任/route，满足 `completed + released <= authorized disposition`，重复复用、冲突/UNKNOWN fail-closed。
- **物理承接**：遵循已接受 `D-MIG-001-A`，只允许从既有 root/detail/route/Ledger durable action group 做版本化机械投影；无独立表、第二写链或 action 级 B fallback。
- **当前证据**：E2 已证明 `REVERSAL` 执行、未完成额度校验、route replay、累计、账务/余额、重放/冲突、SHARED 与并发；当前 query service 尚无 release ActionFact projector，故不得宣称已实现。
- **机械停止线**：原授权、完整 sibling、HOLD/RELEASE 一一对应、Money/累计、Ledger refs、版本任一不可证明，列表与 identity 查询必须为空/UNKNOWN；该动作保持 unsupported/manual，并重开项目级 `D-MIG-001`。
- **Checker 门槛**：产品、DSL、系分、TDD 的范围、原事实、累计、route、UNKNOWN、跨版本和排除项必须一致；不得预批 Java/API/DTO/DDL/Mapper、RED/Green 或 Git。
- **PASS 后入口**：仅机械进入 `MIG-02C_AUTHORIZATION_REFUND_DOCUMENT_CARD`，继续文档闭合；不建立 release 代码 Entry Card。

### 8.33 `MIG-02C` authorization refund 纯文档卡

- **单一范围**：只覆盖 ordinary authorization 链对 `1..n` 条真实 successful complete 的 canonical refund；不覆盖 no-auth、dispute/chargeback、direct/fee refund、payout return/recovery 或 rail authority/finality。
- **稳定合同**：refund 是独立 reverse Intent 下的新 action；Intent/instruction/ActionFact 持有同一组 complete refs 与逐项正 allocated Money，合计等于 action Money，沿每个原 complete 的冻结责任和 route provenance 执行。
- **逐原累计**：每条 complete 下已证明 refund 累计不超过同币种可退上限，partial 占用上限；refund 不恢复 authorization 可 complete 额度，不改写原 complete。
- **当前 E2 证据**：请求与 instruction 只引用 `authorizationTransactionSn`，lifecycle 只校验 root 的 `completed-refunded-declined` 并累加 `refundedAmount`；可证明根级执行/账务/余额，不可证明逐 complete 分配。
- **机械停止**：不得用 authorization root、聚合金额、Ledger/Balance、到达顺序或 FIFO/LIFO/比例反推分配。现有 root-level `AUTH_REFUND` 不得投影 canonical refund ActionFact，必须保持 unsupported/manual。
- **重放/版本**：同 identity 同分配复用，异 Money/complete refs/allocated Money/责任/route 冲突；重启/升级后 identity、intentRef、Money、effect、逐原分配、provenance 和 digest 不变，历史缺分配时仍 fail-closed。
- **Checker 后入口**：若文档合同与阻断结论 PASS，只进入 `D-MIG-001 / REFUND_ALLOCATION_DURABILITY_REOPEN_DECISION_PACKAGE`，由 Human Owner 决定保持 unsupported/manual，或在唯一事实源中补足版本化耐久分配与历史处置。未决策前不建立 Entry Card/RED/Green。

### 8.34 `D-MIG-001-R` refund allocation durability 决策包

- **单一命题**：在 MIG-02C 已证明当前 root-level refund 缺少逐 complete allocation 后，何时、以何种单一事实源承接 canonical refund；不重新裁 refund DSL，不设计 Java/API/DTO/DDL/Mapper。
- **共同合同**：三个候选都保留独立 reverse Intent、`1..n` successful complete、逐项正 allocated Money、逐原责任/route/cumulative、UNKNOWN 与跨版本不变；根级累计、Ledger/Balance、时间顺序和默认算法均不能制造分配。

| 候选 | 单一事实源与当前行为 | 准入证据与代价 |
| --- | --- | --- |
| `A / DEFER_CANONICAL_REFUND_PROJECTION`（已接受） | 继续已接受的 `D-MIG-001-A`；保留现有 root-level refund 执行，但 canonical refund 查询保持空/UNKNOWN。 | 不新增持久化或迁移；首个真实 Consumer 出现并证明分配、恢复、历史需求后再重开。 |
| `B / VERSIONED_EXISTING_GROUP_ENRICHMENT` | 仍以现有 durable action group 为唯一源；只对切换后的 ordinary authorization refund 持久显式 complete refs、allocated Money 和逐原 route provenance。 | 需要真实 Consumer、上游分配契约、版本化事实格式和一次切换；历史 root-only refund 不猜回填，继续 unsupported/manual。 |
| `C / FULL_PROJECT_ACTIONFACT_SOURCE_SWITCH` | 重开原 `D-MIG-001-B`，将既有与后续全部已接受 action kind 一次迁移到独立耐久 ActionFact 事实源。 | 需要全量 schema、回填、等价校验、一次切流和整版回滚；不能仅为 refund 开启。 |

- **Owner 接受范围**：Human Owner 已接受 A，理由是发卡行或上层业务并不保证提供可权威定位到具体 successful complete 的退款来源；`wind-funds` 不能按金额、时间、到达顺序、根级累计、Ledger 或 Balance 强行关联。现有 root-level refund 继续执行并受根级上限约束；没有显式、稳定、可验证的逐 complete 分配时，canonical refund ActionFact 保持 unavailable/UNKNOWN。
- **未来重开与硬负例**：未来若上游能提供完整关联，也必须由真实 Consumer 提交 authority、complete identity、allocated Money、route、版本、恢复和历史处置证据，再重开项目级决策；不得自动启用 B。B/C 未选择且不是运行时 fallback。继续禁止根级聚合伪造分配、nullable/context 字段绕过稳定合同、按 action 采用 A/B fallback，以及“旧动作走投影、refund 单独独立写入”的混合双真相。决策包与 A Acceptance Checker 均已 `PASS / 0 P0-P2`；该结论已进入 MIG-04 文档卡，不授权代码或测试。

### 8.35 `MIG-04` Transaction / Wallet ownership 纯文档卡

- **单一命题**：把 Wallet、Transaction、Ledger 和 Host/Consumer 的现有 facade/read adapter 归还给事实 Owner，并冻结未来一次性切换组；不重新裁产品 DSL，不设计 Java 签名或兼容层。
- **稳定 Owner**：Wallet 只持账户、责任关系、PaymentInstrument/binding、能力/状态与 Spend control 事实；Transaction 持 Intent/Attempt、指令/ActionFact、root/detail、原事实/累计/route 和动作编排；Ledger 持 profile、posting、LedgerTransaction/Entry、Balance 与窄读事实；Host/Adapter 持 Benefit/rail/场景流程与 authority/finality。
- **当前调用证据**：PaymentInstrument transaction facade 与 Spend control consumption 接口位于 `wallet-face`，实现位于 `transaction-impl`；前者仓内仅测试 Consumer，后者唯一生产调用来自 Transaction。Wallet 的 `LedgerQueryService` 被三个 Transaction 生产调用点消费，`LedgerProfileService` 同时被 Wallet initializer 与 Transaction settlement 消费，属于错 Owner read/profile 证据。
- **对象处置**：PaymentInstrument transaction facade 目标归 Transaction，但 Public 可见性必须等真实 Consumer；Spend control 协调器内聚 Transaction，Wallet 保留 movement fact port；Ledger wrapper/profile 归 Ledger Owner，但原 LedgerTransaction 引用解析与 profile ownership 分卡决定；`FundsTransactionQueryService` 留在 Transaction；Benefit facade 等 MIG-08 首个可部署 Consumer 完成 E4 与实际调用切换后退出 Provider。
- **当时依赖停止线**：MIG-04 不批准 `transaction-impl -> ledger-face`。当时把 `core` 窄读作为候选方向，不等于物理 port 已被证明必要；当前由 8.45 重新比较。继续禁止把既有写端口冒充读端口，或引入 Wallet bridge、alias、双 Bean、双 DTO、双读/双写、registry/factory 或长期兼容窗口。
- **原子切换与回退**：接口/请求/实现/测试/依赖/API baseline 和全部调用方按组同版本切换；旧入口零生产引用后才删除，失败回退整个组。未知 Consumer、MIG-05 物理解析决策未闭合或 MIG-08 可部署 Consumer E4/实际调用切换未闭合时保持原路径并阻断，不用桥接层掩盖。
- **未来验证**：模块依赖守卫；PaymentInstrument `authorize/complete/reversal（投影为 canonical release）/receive` 全链等价；Spend control movement 幂等和零额外 Funds/Ledger effect；Ledger/profile 迁移前后事实等价；timeout/restart 同 identity；旧 Wallet facade/read adapter 零引用与 API baseline 一致。
- **当前证据缺口**：没有 PaymentInstrument 生产 Consumer；MIG-05 文档方向已 PASS，但原 LedgerTransaction 引用的物理解析尚待 8.45 决策；Benefit 删除尚待 MIG-08 可部署 Consumer E4 与实际调用切换。三项都不在本卡补实现。
- **状态与下一入口**：独立 Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。该 PASS 当时只准进入 MIG-05 文档卡，不授权 MIG-04 实现；MIG-04 后续已在 r2.232 完成，当前活动状态只见顶部 Metadata。

### 8.36 `MIG-05` Ledger internalization 与交易投影纯文档卡

- **单一命题**：Ledger 如何收回错放 Wallet 的 query/profile，向 Transaction 提供足够但不过宽的内部账本证据，同时确认现有 `FundsActionFact` 交易投影是否继续成立；不设计 Java 签名、表或实现。
- **方案选择**：该历史文档卡选择 A `SAME-STORE_SOURCE-OWNED_NARROW_READ` 作为架构方向。这里的 same-store 是宿主现有资金事实的同一持久化/事务边界，不要求 `wind-funds` 独立部署数据库；它不等于已经选择“新增 core read port”。B 异步/独立物化读模型因无性能与隔离证据 `DEFER`；C 保留 Wallet bridge 因错误 Owner 和重复 DTO `REJECT`。
- **交易投影结论**：`FundsActionFact` 是 Transaction 对唯一 durable action group 的版本化、机械可证、fail-closed 公共只读投影，有 timeout/restart 恢复、Consumer 解耦、原事实/route 追溯、跨版本查询和审计价值；它不是 Ledger/Balance/finality/reconciliation 证明，也不是第二事实源或报表平台。
- **当前物理证据**：Wallet `LedgerQueryService` 只是对 `LedgerTransactionService` 的浅包装；三个 Transaction 生产调用点分别服务 direct 原 ledger ref、authorization 原 ledger ref 与 balance adjustment audit。Wallet `LedgerProfileService` 同时被 Wallet initializer 和 Transaction settlement 消费，profile 内容均为 Ledger 语义。`FundsBalanceAdjustmentAuditApplicationService` 当前无生产 Consumer，不能据此扩大 Public 查询面。
- **目标内部语义**：原 LedgerTransaction 引用必须按 tenant + stable funds fact identity + 必要 event/state 约束唯一解析，并由 Ledger 在 posting 边界验证自身 transaction/plan/entry/route。解析可以复用 Transaction 自有 durable detail，不自动要求新的 Ledger evidence read。Ledger-owned controlled subject admission/profile 是另一物理命题，另卡处理。
- **调用处置**：direct/authorization 逆向用例需要取得唯一 Ledger ref，解析与校验由 8.45 决定；converter 只允许消费已解析 ref 并完成结构转换，不查库、不调用 Service、不做业务决策。audit 仅在真实 Consumer 存在时组合 Transaction 与 Ledger 证据，否则内部化/删除 Public 可见性。Reconciliation 当前已有合法 Ledger face 生产调用并保留；Governance 当前无 Java 生产调用，允许依赖方向不构成新增契约证据。
- **写入与逆向**：posting spec/plan、LedgerTransaction/Entry、Balance 继续只有一条 Ledger-owned 写链；正效果本地原子，逆向逐 OriginalFundsFactRef/route provenance，read 只能验证不能修复。缺失、冲突、UNKNOWN 只停止/查询，不重做 action/posting。
- **行业校准**：Stripe 的 Charge/Refund、BalanceTransaction 与 Payout reconciliation 分层；Modern Treasury 的 LedgerTransaction/Entry 原子不可变；Microsoft CQRS 允许同库读写模型并提示独立读库的同步成本。由此推断当前同库按需投影合适，但第三方对象不直接成为本项目 DSL。来源：[Stripe Balance Transactions](https://docs.stripe.com/api/balance_transactions)、[Stripe payout reconciliation](https://docs.stripe.com/payouts/reconciliation)、[Modern Treasury Ledger Transactions](https://docs.moderntreasury.com/ledgers/docs/ledger-transactions-overview)、[Microsoft CQRS](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs)、[Materialized View](https://learn.microsoft.com/en-us/azure/architecture/patterns/materialized-view)，核验日期 `2026-08-17`。
- **硬停止**：新表/读库/事件总线、Public 通用 Ledger search、`transaction-impl -> ledger-face`、Wallet bridge/alias/双 DTO/双读、新 profile policy engine、Consumer 拼 posting、ActionFact 猜业务/外部 finality，任一出现即重开文档。
- **验证与回退**：未来先完成 8.45 物理解析决策，再按被接受候选形成独立 Entry Card；profile、initializer/settlement 与 Governance 空依赖不得偷并入首个引用切片。Reconciliation 的真实 Ledger face 读取保持行为不变；任一调用遗漏或行为不等价，整组不切换并保留当前唯一写链。
- **状态**：`DOCUMENT_CARD_CHECKER_PASS / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.121`。Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`，只关闭文档方向，不授权 Contract Inquiry、Entry/RED/Green、Java/API/DTO/DDL/Mapper、测试、Consumer、Git、HOST/L4 或发布。MIG-06 仍被 Host evidence 阻断；该阶段当时机械进入 `MIG-07_RECONCILIATION_STAGE_DOCUMENT_CARD`，当前活动状态见 8.37。

### 8.37 `MIG-07` Reconciliation core 与 Stage handoff 纯文档卡

- **单一命题**：建立 carrier-independent source snapshot、Reconciliation 自主 `1:1 strict-exact`、append-only Difference、多侧 Gate 和 Stage mandatory check 的稳定公共合同；完整设计 Reconciliation core，只冻结 Stage handoff，不重做 clearing/settlement/payout/recovery 生命周期。
- **Human Owner 确认**：选择 `CORE_FIRST / STAGE_BOUNDARY_ONLY`；不考虑现有 API/DTO/table 兼容，按正确设计一次切换；数据源是稳定能力抽象，file/API/query/report/event/statement/manual 只是 carrier。
- **Source 边界**：Source Owner/Adapter 验真 raw source、scope 与 authority，并按 Pair Comparison Rule Owner 签收的共同 ComparisonRuleRef 产出 immutable `ReconciliationSourceSnapshot + NormalizedComparisonFact[]`。该 rule ref 冻结两侧 source roles/namespaces、DomainOutcome mapping、claim kind、economic component、direction、scope/effective period 与 version；只有唯一、明确映射才可设置结构性 `comparisonProven=true`。Reconciliation 只验 caller access、tenant/scope/source type、共同 rule ref/version、字段、coverage/watermark 和 evidence refs；不接收 `trusted=true`，不解释 raw protocol。
- **事实与身份**：delivery identity、source fact identity、comparison identity 分离；semantic digest 与 evidence bundle digest 分离。semantic digest 覆盖 Money/status、comparisonProven、claim kind、economic component、direction 和共同 ComparisonRuleRef/version。同事实多 carrier 只追加 evidence association，同 identity 异任一经济语义进入 conflict。
- **匹配**：首包只有 normalized `1:1 strict exact`；两侧 comparison identity 各恰好一条、`comparisonProven=true`、同币 Money、`claim kind + economic component + direction` 逐项相等、引用同一有效 ComparisonRuleRef 且 rule-scoped ComparisonStatus 相等、coverage 完整时才 Matched/Balanced。双侧 UNKNOWN 即使 code 相同也不可比较。`1:N/N:1` 必须由来源 Owner 预聚合为含 member count/digest 的单 fact。无 tolerance/netting/FX/通用 rule engine。
- **Difference**：所有 mismatch/missing/conflict 固化 MatchResult，Difference Case 只按运营需要创建且初始责任中立。`RESOLVED` 需要 controlled action evidence + 后继 current Balanced run；`INVALIDATED` 仅由正式 superseding evidence 触发。manual 不改旧 result/run。
- **Gate/Stage**：Stage Owner 冻结 versioned GateRequirement 和完整 required pair set；每个 exact stage identity 同一时刻只有一个 current/effective requirement head。Gate 按 stage binding 自动解析 head，并固定合取全部 required pair 的 current run heads 与 blockers；caller 不能选择旧窄 requirement、漏掉不利 pair或配置 optional/non-blocking pair。普通 query 只展示；Stage 在本地事务内 mandatory recheck requirement head/current lineages/blockers，再原子提交 stage state/normalized funds action并冻结 consumed gate evidence。Gate 自身零资金/账务/stage effect，stale PASS 不可复用。
- **正交声明**：Matched/Balanced/Gate PASS、Stage result、Funds/Ledger/Balance、beneficiary arrival 与 external finality 互不推导。
- **现状 E2**：当前 source input 只有 ref/digest，match item 由 caller 提交 quality/strength/difference，`EXACT_MATCH/RULE_MATCH` 在未比较 Money/status、未验证 rule evidence 时可直接计为自动匹配；Gate 只消费单 run。它只证明骨架，不证明目标能力。
- **迁移**：未来获准后目标契约与全部调用方同版本一次切换；无 bridge、alias、V2、双 DTO、双读/双写或 carrier-specific flow。无法重算的历史 assertion 保持 unsupported/manual。
- **状态**：首轮独立 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 0 P2`；补齐 comparison semantics 与 pair-level Comparison Rule Owner 后，返工独立复核为 `PASS / 0 P0-P2`。本卡关闭状态为 `MIG-07_DOCUMENT_CARD_REWORK_CHECKER_PASS / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.124`；全局当前入口见 Metadata 与 8.38，不授权 Contract Inquiry、Entry/RED/Green、Java/API/DTO/DDL/Mapper、测试、Consumer、Git、HOST/L4 或发布。

### 8.38 `MIG-06/08` Host / deployable Consumer 证据盘点（`plan-r2.126`）

- **单一命题**：当前是否已经存在一个真实可部署 Consumer，足以把 MIG-06 外部归一和 MIG-08 Consumer 切换从等待态推进到独立 Contract Inquiry。只盘点现状，不选择未来宿主，不把模块依赖、测试宿主或产品文档冒充运行证据。
- **准入定义**：合格 Consumer 必须同时具备可部署应用或明确 composition root、`wind-funds` 目标制品依赖、真实 Source Adapter/Stage 调用、数据库与 schema/transaction Owner、source/version/scope、timeout/restart 恢复入口、artifact lineage 和可执行 E4/L4 计划。缺任一项只能记录候选或 blocker。

| 候选 | 一手证据 | 结论 | 缺口 |
| --- | --- | --- | --- |
| `wind-funds` 仓内 Clearing/Settlement/Payout | `reconciliation-impl` 内部已有 Gate 调用和 Stage 本地事务路径；当前 checkout `HEAD=eb120918...` 且 dirty。 | `PROVIDER_INTERNAL_E2 / NOT_A_DEPLOYABLE_CONSUMER` | 同一公共库内部自用不能证明宿主装配、外部 source adapter、真实数据库迁移、进程重启或生产部署。 |
| `capte-domain` | `HEAD=8b363be4...` 且 dirty；根 POM 使用 `wind-funds.version=1.0.1-SNAPSHOT`，生产 dependency management 只有 `core/transaction-face/wallet-face`；三个 impl 仅在 `tests` 以 test scope 引入。 | `GENERIC_LIBRARY + TEST_HOST_E4 / NOT_A_DEPLOYABLE_CONSUMER` | 无 `reconciliation-face/impl` 生产依赖、无 Reconciliation Java 调用、无独立生产数据库或部署进程。 |
| `fincone` | `HEAD=fd26cabd...` 且 clean；仓库 AGENTS 明确 docs-first，不承载运行时代码，目录只有设计与交付材料。 | `E1_DESIGN_AUTHORITY / NOT_A_RUNTIME_CONSUMER` | 无 POM、Java、composition root、数据库、adapter、Stage 调用或 E4。 |
| `fincone-issuing` | `HEAD=696b41fa...` 且 dirty；根 POM 仅聚合 `core`，当前无 Java 源码，也无 `wind-funds` / Reconciliation 依赖。 | `RUNTIME_TARGET_SKELETON / NOT_READY` | 未形成可部署应用、Source Adapter、Stage、数据库/事务或测试证据。 |
| 当前本机其他 Capte Maven 仓库 | 对 `/Users/wuxp/Workspace/idea/capte` 的 POM 与 Java import 扫描未发现除 `capte-domain` 外的当前 `wind-funds-reconciliation-*` Consumer。 | `NO_CURRENT_LOCAL_EVIDENCE` | 本机扫描不是组织级调用清册；未来由被提名 Consumer Owner 提供仓库与版本。 |

- **判定**：当时没有满足该“可部署 Consumer”定义的候选。`capte-domain` 的 `1.0.1-SNAPSHOT` 集成可证明公共库真实 Consumer 与测试宿主能力，但不能证明 Reconciliation E4/L4；`fincone` 的业务设计只能证明设计与模拟输入。该判定继续约束 L4，不再被解释为所有后续文档、Contract 和 E4 的统一阻断。
- **实际价值**：本次盘点避免为了“继续重构”把 Provider 内部 Stage、通用领域库或 docs-first 仓库虚构成 Host，也避免提前建设无 Consumer 的 source profile、规则引擎、兼容层或第二套对账入口。
- **当时恢复输入**：若要关闭 L4，Human Owner 或可部署 Host Owner 仍须给出 composition root、目标 Stage、source pair、规则与数据/事务 Owner、部署形态和失败恢复责任。
- **历史状态**：`EVIDENCE_INVENTORY_CHECKER_PASS / WAITING_FOR_HOST_AND_DEPLOYABLE_CONSUMER_EVIDENCE / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.126`。当前校准见 8.39。

### 8.39 `MIG-06/08` Consumer 角色与证据 Gate 校准纯文档卡

- **触发事实**：Human Owner 明确 `capte-domain` 是当前实际 Consumer，`fincone` 只能承担设计和 `wind-funds` 模拟 Consumer。复核确认前者已有 `core/transaction-face/wallet-face` 生产调用与跨仓 test-host 证据，但尚无 Reconciliation 依赖/调用；后者为 docs-first，无 runtime。
- **校准结论**：此前盘点对“当前无可部署宿主”的结论仍成立，但不能把 deployability 当成 Contract/E4 前置。角色分为 Provider、真实库/目标 Contract Consumer、设计/模拟 Consumer、未来 deployable host；证据分为 design simulation、Contract、test-host E4、deployable-host L4，逐层关闭且互不外推。
- **首个契约种子**：复用 Capte Benefit funding 交接。Capte 侧归一业务 handoff fact，Funds 侧归一 FundsActionFact；共享 ComparisonRuleRef，按 comparison identity、Money/currency、claim kind、economic component、direction、ComparisonStatus 做 `1:1 strict-exact`。Ledger/Balance 是正交证据，不混入该 pair；若 Stage 需要，必须作为独立 required pair。任何 missing/mismatch/conflict/coverage 不完整都不得 Balanced 或放行 Stage。
- **公共边界**：场景对象、adapter authority、业务 status mapping 仍归 Capte/Fincone；`wind-funds` 只接收 normalized snapshots/facts。Consumer/Host 是证据角色，不新增 core DSL、通用 source engine、场景 facade、registry 或兼容层。
- **Gate**：Fincone 可关闭 design/simulation；Capte 与 Funds Owner 可进入最小 Contract Inquiry；capte-domain 未来通过发布制品、真实 Spring/H2/transaction/restart 关闭 E4；只有可部署应用能关闭 L4。当前不声称 Contract/E4/L4 已 PASS。
- **Checker 门槛**：五份权威文档角色、首个种子、Gate、停止线和下一入口一致；不得把现有 Benefit E4 外推为 Reconciliation E4，不得把模拟输入写成 runtime evidence，也不得让 L4 缺失继续阻断纯文档 Contract Inquiry。
- **Checker 结论**：`PASS / 0 P0 / 0 P1 / 0 P2`。角色事实、四层 Gate、首个 pair 与停止线均闭合。
- **当时入口**：`CI-MIG07-RECONCILIATION-001 / CONTRACT_INQUIRY / CONTRACT_DECISION_PENDING / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.128`。该阶段只允许冻结最小物理契约候选，现已由 8.40 决策包承接；当前状态见 Metadata。

### 8.40 `CI-MIG07-RECONCILIATION-001` 最小物理契约决策包

- **单一命题**：在两侧 Source Owner 已完成 authority、scope 与归一的前提下，谁负责产出可被公共层声明为 `strict-exact` 的 MatchResult。不得借本包重裁 source authority、业务状态映射、资金动作、Ledger/Balance、Stage policy 或外部 finality。
- **共同合同**：carrier-neutral snapshot/fact、共同 ComparisonRuleRef、双侧 `comparisonProven=true`、`1:1`、同币 Money/comparison status/claim kind/economic component/direction 严格相等、coverage 完整、immutable lineage、responsibility-neutral Difference、Gate 无资金副作用。
- **A `PROVIDER_COMPUTED_STRICT_EXACT`（已接受，待 Acceptance Checker）**：Consumer 只提交 scope 与两侧 normalized snapshots/facts；Provider 计算 matched/missing/mismatch/conflict 和 run outcome。保留 Batch/Run/Gate 服务 Owner，破坏式替换 source/run/Gate 写入请求；删除 caller MatchResult assertion。
- **B `TYPED_ASSERTION_WITH_PROVIDER_RECHECK`（未选择）**：Matcher adapter 提交 typed assertion，Provider 仍对两侧事实完整复算。正确性可达标，但重复计算、重复 DTO 和错误解释没有当前 Consumer 价值证据；不得作为 A 的 fallback。
- **C `CALLER_ASSERTION_PERSISTED`（未选择）**：保留 caller `EXACT_MATCH/RULE_MATCH`，只加强 digest/IAM/evidence。无法证明 Money/status/semantics 真相，拒绝；不得作为 A 的 fallback。
- **最小 surface 候选**：`createBatch` 冻结 scope/pair/rule/currency/window，不再携带 Gate object；`recordSourceSnapshot` 接收 logical source namespace、snapshot version、coverage/watermark 和含 comparisonProven 的 `NormalizedComparisonFactInput[]`，不接 carrier source type；`recordRunResult(matchResults)` 候选替换为仅含 tenant+batch 的 `executeStrictExact`；Gate 记录 immutable versioned GateRequirement current/effective binding，`check/inspect` 只接 exact stage identity 并自动解析唯一 head，不接 caller requirement/run list。Java 名称、包和 annotation 仍待 Owner 接受及 Entry Card。
- **计算与错误**：Provider 唯一映射 comparison identity；先要求双侧 comparisonProven=true，再依次校验 rule、Money/currency、ComparisonStatus、claim kind、economic component、direction；产出有限的 matched/not-comparable/missing/money-status-semantics-rule mismatch/identity conflict。comparisonProven 进入 semantic/result digest；coverage incomplete 为 run blocker。`RULE_MATCH`、tolerance、FX、netting、人工 match 不在首包。
- **实际种子**：Capte Benefit funding handoff `60 CNY` 对 FundsActionFact `60 CNY` 且双方 proven 为 matched；对 `40 CNY` 为 Money mismatch；一侧缺失、同侧重复、status/rule/semantics 不同均 Difference/BLOCKED；双侧 UNKNOWN 即使相同也 NOT_COMPARABLE。Ledger/Balance 若为 Stage 必需证据，必须另列 required pair。
- **Gate 固定语义**：首包不提供开放式 blocking policy 或 pair-level blocking；GateRequirement 内全部 pair 都是 required 且必须 current、Completed、Balanced、零 blocker。观察性 pair 不进入 GateRequirement。旧窄 head、并行 head、inspect 后 head 变更和 timeout/restart 都必须重新解析并 fail-closed。
- **价值结论**：A 直接消除 caller 自报对平，复用既有 batch/run/difference/lineage 骨架，并把 file/API/event/report 从事实类型降为 evidence carrier；不建立通用规则引擎。当前 `GO=决策包+Checker`，`NO-GO=代码/E4/L4`；运行 ROI、人工率和 SLA 没有生产数据，不伪造数值。
- **接受边界**：A 只把 strict-exact 计算权威收敛到 Provider；Source Adapter 继续拥有 authority、scope、状态映射和 comparisonProven 来源责任，Provider 仍独立校验完整事实合同。事实缺失、UNKNOWN、冲突或规则失效只能 NOT_COMPARABLE/Difference/BLOCKED，不能降级 B/C。
- **拆片要求**：Acceptance Checker PASS 后才可先形成 `SOURCE_RUN_STRICT_EXACT` Entry Card；GateRequirement 紧随其后单独成卡。不得把 Gate/Stage 顺带塞进首个 Green，不得保留 old/new 双入口或 V2。
- **状态**：`accepted_answer=A / owner_decision=ACCEPTED / B_C=NOT_SELECTED / ACCEPTANCE_CHECKER_PASS / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.132`。该阶段当时唯一入口是编制 Source/Run Entry Card；Entry Card 现已由 8.41 承接，当前状态见 Metadata。

### 8.41 `W5-MIG07-SOURCE-RUN-STRICT-EXACT-001` Entry Card

- **目标与状态**：`ENTRY_CARD_INDEPENDENT_CHECKER_PASS / SOURCE_RUN_CONTRACT_ACCEPTED / SOURCE_RUN_CONTRACT_ACCEPTANCE_CHECKER_PASS / RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.136`。已把接受 A 转成通过 Entry Card 与 Contract Acceptance Checker 的 Source/Run breaking contract、RED 矩阵和写入边界；Human Owner 接受的唯一精确 surface 已准出。
- **当前缺口**：source item 只有 ref/digest；caller 通过 `recordRunResult(matchResults)` 写 quality/strength/difference/severity，Provider 不比较 Money/status/semantics。现有 schema 也没有 normalized fact 和有限 result kind。
- **已接受的唯一 Contract surface**：结构化 scope/rule、逐事实 `ComparisonRuleRef`、carrier-neutral snapshot + normalized facts、无 payload 的 `executeStrictExact`、Provider-computed finite result kind；batch/reference/comparison 三个 rule ref 必须一致。异币种固定为 `CURRENCY_MISMATCH` 且差额/较大侧为空；仅同币种 `MONEY_MISMATCH` 携带正的绝对差额 Money 与 `REFERENCE/COMPARISON` 较大侧；删除 caller assertion、carrier-mixed source type、match severity。该接受冻结目标契约与 breaking replace 方向，不等于 Java/DDL 实现授权。
- **持久化方向**：只使用现有 batch/source snapshot/source item/run/match 一套表族做原位 breaking migration；不新增 V2/第二表族、bridge 或双写。exact columns、annotation、Mapper 和 transaction 必须等 Contract/DDL 授权；获准后必须同步生产 create/verify SQL 与 H2 schema，禁止只形成测试库 Green。
- **运营边界**：strict-exact 只确定事实分类与可复算差额，不决定 Difference severity、责任、manual action 或资金修复。GateRequirement/Stage 继续另卡。
- **共享迁移屏障**：Source/Run 与 Gate 分卡 Checker，但最终同一 breaking release 一次切换。Gate 卡未关闭前不允许用旧 Gate object、旧 MatchResult 字段或兼容 facade 获得 Source/Run Green。
- **RED 入口候选**：一个 public contract RED + 既有 Batch/Run Spring/H2 流程，覆盖 matched、Money/status/semantics/rule mismatch、missing、coverage incomplete、identity conflict、NOT_COMPARABLE、重放/冲突/timeout 和零资金/Stage 副作用；当前未获准写测试。
- **写入与停止线**：未来白名单仅 reconciliation face/impl 的 batch/source/run/match、必要 schema 与对应 tests。需要 raw parser、rule engine、tolerance/FX/netting、manual match、兼容层、Consumer 或跨模块扩张时停止。
- **当时唯一入口**：`W5-MIG07-GATE-REQUIREMENT-001 / ENTRY_CARD_REQUIRED / DOCUMENTATION_ONLY / CODE_FREEZE`。该入口现由 8.42 承接；Gate Contract 与 Checker 关闭后才能单独申请 Source/Run RED，不得继承本轮授权。

### 8.42 `W5-MIG07-GATE-REQUIREMENT-001` Entry Card

- **状态**：`CONTRACT_ACCEPTED / CONTRACT_ACCEPTANCE_CHECKER_PASS / RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.140`。
- **当前 E2**：caller 以 `gateObjectType + gateObjectSn + reconciliationRunResultSn` 选择单 run；Provider 只锁一个对象 current lineage、Completed batch、Balanced run 与对象级 Difference。该实现不能证明 multi-pair 不可裁剪，也不能区分同一对象的不同 Stage action。
- **唯一候选 Contract**：`GateStageRef=stageKind + StableIdentity` 精确标识一次 Stage action；Stage Owner 以 `recordGateRequirement` 追加 immutable version，冻结 `RequiredPairRef=scopeIdentity + pairIdentity + ComparisonRuleRef` 全集；check/inspect 只接 tenant + stageRef，自动解析唯一 current/effective requirement head 和全部 required scope+pair current run heads。
- **发布规则**：caller 只提交 requirement version，identity 由 Provider 生成；tenant+stageRef+version 唯一。首次发布并发由唯一键冲突后一致性回读；同版本 semantic digest 与 evidence-bundle digest 都相同才重放；任一 digest 不同冲突；后继版本带 exact expected-current 并 CAS 推进 head。空/重复 scope+pair、CAS 失败、缺失或并行 head 均冲突。首包不提供 optional Pair、blocking flag、threshold、future-effective schedule 或 policy expression。
- **判定规则**：每个 required pair 必须 rule 一致、current run 唯一、batch Completed、run Balanced、coverage complete 且 current lineage 零 blocker；任一失败整体 BLOCKED。有限 blocker code 仅作解释，不成为策略输入。
- **事务**：`inspectGate` read-only 且不是授权；`checkGate` 必须 MANDATORY 加入 Stage 本地事务，锁 requirement head，再按稳定 scope+pair key 锁 current run heads/blockers。Stage 状态/规范化资金动作与 `ConsumedGateEvidence` 同事务提交；Gate decision 不落成可复用 PASS token。Gate BLOCKED 或成功 evidence 写失败整体回滚；后续确定性零效果拒绝可保留 Stage FAILED/BLOCKED 与 proven-zero Funds fact，但不得写成功 consumed evidence；UNKNOWN 保留已证明局部事实并 fail-closed。
- **最小持久化候选**：带 tenant+stageRef+version 业务唯一键的 immutable requirement header、按 requirement+scope+pair 唯一的 immutable required-pair rows、每个 tenant+stageRef 唯一的 mutable head pointer，以及由 Stage 成功事务写入的一条 internal consumed-evidence snapshot。Source/Run facts 不复制，不建第二对账内核；Stage evidence 的 scope+pair 集合先用 canonical structured payload，不提前拆索引表。
- **Difference 边界**：目标 blocker 按 required scope+pair/current run lineage 查询；Difference append-only，只有受控 action evidence + 后继 current Balanced 才可闭环。Gate 不创建、修改、解决 Difference，也不执行资金修复。
- **测试矩阵**：`MIG07-GATE-CONTRACT-001 + MIG07-GATE-001~010`，覆盖 public old-reference removal、三 scope+pair PASS、同 pair 不同 scope、Requirement 双 digest replay/conflict/CAS、missing/stale/not-completed/not-balanced/rule/coverage/head conflict、禁止 optional policy、inspect stale、Stage 成功原子提交与确定性拒绝/UNKNOWN 分层、timeout/restart、Difference/current lineage 和 shared breaking migration。
- **写入与回退**：未来条件白名单仅 reconciliation gate/requirement/current-lineage/Difference read、已知清分/清算/结算/出款 Stage caller、对应 tests 与生产/H2 schema。Source/Run + Gate 同一 breaking release 一次切换；旧 Gate object/run 参数和 Stage 单 run evidence production reference 必须归零，失败整切回退，不建 V2/bridge/双读写。
- **价值**：防止 caller 只提交有利 run 或漏掉 Ledger/业务等 required pair；把“展示时 PASS”与“最终执行时权威检查”分开；为 Stage 审计提供可复算 consumed evidence。它不证明 Stage 成功、Ledger/Balance、外部 finality 或 beneficiary arrival。
- **现有 caller 映射**：系分 11.16.5 已逐项冻结可清分识别、清分逐项确认、清算 candidate inspect/逐项 confirm、settlement lock/release、payout preflight/submit、difference report 和旧 wrapper 的 stageKind、identity、inspect/check、evidence Owner 与删除/替换处置；新增未知 caller 时停止。
- **当时唯一入口**：`W5-MIG07-SOURCE-RUN-GATE-BREAKING-GREEN-EXECUTION-GRANT / RED_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_NO`。Human Owner 后续仅授权重做文件级 Green Entry Card；当前入口见 8.43 与 Metadata。

### 8.43 breaking Green 文件级 Entry Card rework（plan-r2.146 Checker NOT PASS，历史）

- **状态**：`W5-MIG07-SOURCE-RUN-GATE-BREAKING-GREEN-ENTRY-CARD-REWORK-001 / GREEN_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_NOT_PASS / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.146`。
- **目标**：把 8.41/8.42 已接受且 RED 已证明的七项 breaking contract，冻结为未来 Green 唯一可执行的 ADD/MODIFY/DELETE 文件集合；本卡不重裁 Contract、不执行 Green。
- **七项一对一映射**：TDD 20.22.5 将 `SR-1~4 / GT-1~3` 分别唯一映射到移除 caller `matchResults`、删除 `ReconciliationMatchResultItem`、`recordRunResult -> executeStrictExact`、窄化 Provider command、Gate request 只接 tenant+stageRef、Gate service 只保留 record/check/inspect，以及 versioned mandatory-pair Requirement 发布。
- **生产方向**：沿既有 face/impl 边界破坏式切换；既有 Batch/Source/Run/Match 表族原位迁移，新增 Requirement header/pair/head 与一条 Stage evidence 表；全部 production Gate caller 直连 Gate face；删除旧 wrapper/object/run shape。没有 V2、bridge、alias、双读双写、通用 matcher/policy/registry 或第二对账内核。
- **direct-caller closure**：系分 11.16.5 与 TDD 20.22.5 逐项冻结可清分识别、清分确认、清算 inspect/confirm、settlement lock/release、payout preflight/submit、Difference report 和旧 wrapper 的 old call/new accepted call/保持行为。文件卡 Checker 前发现的同契约直接 caller 可机械补入；Checker PASS 后 Green 遇到任何表外文件必须停止。
- **测试边界**：`ReconciliationStrictExactPublicContractTests` 与 `ReconciliationGatePublicContractTests` 永久不可修改；四个原授权 legacy test/fixture 及清点出的其他 direct-caller tests 只允许机械迁移 setup/call，必须保留测试数量与业务、资金、Ledger、Balance、幂等、失败和只读断言。禁止以兼容层让旧测试继续编译。
- **唯一文件源**：精确全路径、operation、责任、RED 映射、caller old/new、schema 和命令只以 TDD 20.22.5 为准；产品、DSL、系分与本节只保存边界和状态，不建立第二白名单。
- **验证门槛**：未来仓库级 Green 必须得到 compile `21/21`、PublicContract `8/0F/0E`、combined `93/0F/0E`（old host `85/0F/0E`）、reconciliation `236/0F/0E/0S`、额外 direct callers `12+5+5=22/0F/0E/0S` 与 `git diff --check` PASS。MySQL DDL 只需完成本仓库 schema/DDL contract 验证；`wind-funds` 作为公共库不以真实 MySQL host 为准出前置。未来 Consumer 若选择 MySQL，其连接、迁移、事务与兼容性归 Consumer E4/L4 独立验证，不反向阻断公共库。计数下降、skip、unexpected error、旧引用非零或表外文件均停止。
- **排除范围**：PaymentInstrument、其他 baseline setter、core/transaction/wallet/ledger 生产语义、外部 Consumer、HOST/E4/L4、MIG-09、build/dependency、Git、联网、安装、部署、release/production。Mockito/ByteBuddy sandbox 约束继续独立保留；PMD 是仓库静态检查命令，未形成 fresh 结果时只登记验证缺项，不建立独立环境或产品能力 blocker。
- **Checker 结论与当时入口**：独立 Checker 判定 `NOT PASS / 0 P0 / 4 P1 / 1 P2`。contract RED 只证明 surface，不能证明 strict-exact/GateRequirement 行为；增量编译不能证明删除旧 class，且缺 Public Contract 约规、可执行测试入口和 Justfile 表数注释处置。该卡不得进入 Green，当前替代入口见 8.44 与 Metadata。

### 8.44 无兼容 hard-break surface、行为 RED 与最终 Green Entry Card

- **历史状态**：MIG-07 于 `plan-r2.150` 完成无兼容 surface、behavioral RED 与 behavioral Green 并通过独立 Checker。`plan-r2.147~r2.150` 的文件卡、Grant 与执行结果均为已关闭历史事实，不构成当前授权；当前活动状态只见 Metadata 与 8.45。
- **取舍**：Human Owner 明确不考虑兼容问题。目标仍是一个原子 breaking release，旧 class/method/field/caller 同批删除；禁止 alias、V2、bridge、facade、双读、双写、旧新并行和按 caller fallback。
- **检查点一，contract surface hard break**：已在独立 Grant 下按 TDD 20.22.5 文件白名单完成；`just clean-compile` 清除 stale class，`just verify-public-contracts` 与两个 immutable PublicContract tests 通过，旧十类、旧方法/字段和 production refs 归零，尚未实现行为保持 fail-closed。独立 Checker 最终 `PASS / 0 P0-P2`；该检查点未发布、提交或部署。
- **检查点二，behavioral RED**：已在独立 Grant 下新增 `ReconciliationStrictExactBehaviorTests` 与 `ReconciliationGateRequirementBehaviorTests`，逐项承接 `MIG07-SR-001~009` 和 `MIG07-GATE-001~010` 共 19 个顶层场景；两个 immutable contract tests 与既有 legacy tests 未改。经三轮最小返工，RED 独立 Checker 最终 `PASS / 0 P0-P2`。
- **检查点三，behavioral Green**：已在独立 Grant 和冻结白名单内实现 Provider strict-exact、Requirement/current head/multi-pair 与 consumed evidence，并完成 contract、19 个行为场景、reconciliation 和 direct-caller 回归。Green 首轮 Checker 发现首次并发发布不同 Requirement 版本时 loser 可误作 replay 并留下孤儿事实的 `1 P1`；最小返工改为先竞争唯一 head、按实际 winner 完整复验，新增同 Stage `v1/v2` 并发回归后最终 `PASS / 0 P0-P2`。
- **构建与约规**：breaking DELETE 首个门必须是 clean compile；`verify-public-contracts` 必须覆盖新增/修改 face 类型。Justfile 只允许 comment-only 将表数注释从 `21 reconciliation / 44 total` 改为 `25 reconciliation / 48 total`，recipe 不变。测试命令统一使用 TDD 20.22.6 已由当前 checkout 证明能进入 Surefire 的 `MAVEN_OPTS=-DskipSurefireReport=true + Java 21 + just test-one/test-reconciliation`；该前缀只跳过 report 聚合，不跳过测试。若精确命令未进入 Surefire，必须停下重冻，不能现场拼参。
- **唯一文件与命令源**：TDD 20.22.5 保留最终生产/schema/caller 白名单，TDD 20.22.6 只增加两个行为测试文件、Justfile comment-only 路径和三检查点命令；本节不建立第二白名单。
- **历史关闭**：本卡授权已耗尽；不得继续修改 MIG-07 行为、执行 Git 或发布。当前入口已由 8.45 接替。

### 8.45 `CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001` 物理契约决策包

- **单一命题**：direct/authorization 逆向用例构造指令时，谁负责解析原动作已持久化的 `ledgerTransactionSn`。本题不决定 Ledger profile、建账、posting admission 或通用 audit query。
- **一手事实**：`FundsTransactionDetail` 已保存 tenant、event、state、Money、reference 与 `ledgerTransactionSn`；`FundsTransactionQueryService` 已能按原资金交易读取 details 与 route snapshot；Ledger posting 已按 reference ledger transaction、原 funds transaction、replay route leg、plan/entries 复验。`FundsBalanceAdjustmentAuditApplicationService` 只有测试 Consumer。
- **A，已接受且 Acceptance Checker PASS**：`TRANSACTION-FACT-RESOLVED-LEDGER-REF`。Transaction Application/Service 用例边界从自有 durable details 校验 tenant、目标 event、成功状态、siblings 一致和唯一非空 ledger ref；converter 只映射已解析引用，Ledger posting 再验证自身事实。零新增 port/DTO/schema。
- **B，未选择且非 fallback**：`CORE-ACTION-LEDGER-EVIDENCE-READER`。新增 core internal read port，由 Ledger 按 action 搜索 evidence。当前只有两个承重逆向用例，且引用已经存在 Transaction detail，不足以证明单实现端口和重复搜索必要。
- **C，拒绝且非 fallback**：Transaction 直依赖 `ledger-face` 或保留 Wallet bridge。它破坏依赖方向或保留错误 Owner、宽查询与重复 DTO。
- **共同不变量**：选择顺序固定为 `source root/event -> frozen replay leg -> route participant coverage -> SUCCEEDED details -> distinct nonblank ledgerTransactionSn=1`。缺失、多引用、wrong tenant、mixed event/state/ref 或 route/participant 不一致一律空/UNKNOWN，不取第一条、不换 identity、不创建第二动作/posting。Transaction detail/ActionFact 不证明 Ledger/Balance；Ledger 继续独立闭合。
- **direct 细化**：普通 referenced refund 只接受 `DIRECT_TRANSACTION/PAY` 并选择非 `FEE` replay leg；standalone fee refund 只接受 `FEE_CHARGE` 的唯一 `FEE` leg；embedded fee refund 只接受带费 `PAY` 的唯一 `FEE` leg且 target 必须对应 `FEE_RECEIVER`。完整动作组必须共用一个 Ledger 引用；合法 payer/payee/fee siblings 的 subject 与 Money 可以不同，只逐项与对应 route participant/leg 对齐，不能按主体相等或金额相等误判冲突。
- **authorization 细化**：只读取 `AUTHORIZE + SUCCEEDED` 且与原 HOLD route participants 对齐的 siblings；complete/release/refund 后继 details 不参与。
- **价值确认**：A 复用已存在的 Transaction durable detail/route 证据，把“定位引用”留给 Transaction，把“证明真实账务”留给 Ledger；它直接覆盖 ordinary refund、standalone/embedded fee refund 和 authorization 后继动作，同时删除新增 core reader、跨模块 Ledger 查询与第二 DTO/schema 的必要性。A 不提供通用 audit/search，不证明 Ledger/Balance 完成，也不补造上游缺失的业务原事实。
- **拆片边界**：A 的未来首切只覆盖两个承重逆向用例在 Transaction Application/Service 边界的解析、converter 被动映射、Wallet Ledger wrapper 零引用和无 Consumer audit 处置；具体生产文件由后续 Entry Card 按 caller closure 冻结。profile 另进 `MIG-05B-LEDGER-PROFILE-OWNERSHIP`。若实现需要新 port/DTO/DDL/Mapper、Ledger 搜索、converter 查询/业务判断或兼容层，立即停止并重开决策。
- **Owner/status**：Human Owner 已选择 A；Transaction/Ledger Owner 分别签自有事实解析与 Ledger 再校验；Decision Package Checker 与 Acceptance Checker 均为 `PASS / 0 P0 / 0 P1 / 0 P2`；`accepted_answer=A / owner_decision=ACCEPTED / B_C=NOT_SELECTED_NOT_FALLBACK / status=ACCEPTANCE_CHECKER_PASS / plan-r2.154`。该接受阶段已经关闭，当前执行入口由 8.46 承接。

### 8.46 `W5-MIG05-TRANSACTION-LEDGER-REFERENCE-ENTRY-CARD`

- **目标**：一次无兼容迁移，把原 Ledger 引用解析收回 `FundsTransactionCommandServiceImpl` 的 Transaction 用例边界，converter 只映射；零引用后同批删除 Wallet wrapper/DTO/impl 与零生产 Consumer 的 balance-adjustment audit Public 面。
- **最小物理方案**：`ADD=0`。生产只允许 `FundsTransactionCommandServiceImpl`、`FundsDirectTransactionInstructionConverter`、`FundsAuthorizationInstructionConverter` 三个 `MODIFY`，以及系分 11.11.7 精确列出的九个 `DELETE`。CommandService 同时负责把原 `PAY` 带手续费的过宽整体拒绝收窄为冻结 route 上 `REFUND -> non-FEE`、`FEE_REFUND -> FEE` 的既有分腿规则，并继续拒绝本次 refund 新增手续费；不改 POM、schema、Mapper、`FundsTransactionQueryService`、Ledger profile 或 posting。
- **RED 边界**：只允许 TDD 20.17.6 的三个既有测试文件。七个独立可达失败组逐项证明 direct principal（含带费原 `PAY` 只退 principal、精准暴露当前过宽 fee 拒绝）、standalone fee、embedded fee、合法/非法 siblings、authorization、trust/UNKNOWN/replay 与架构退出；任何 Spring/H2/编译错误或既有回归都不是目标 RED。
- **Green caller closure**：除三个 RED 文件，未来 Green 只允许机械修改四个明确 Spring 测试装配文件并删除 `FundsBalanceAdjustAuditFlowTests`；不得改变既有业务断言。发现任何未列 production/test caller、需要新增类型/字段/依赖或无法原子删除的旧面，必须停卡重审。
- **验证**：所有 test 命令统一使用 TDD 20.17.6 已由当前 checkout 证明可进入 Surefire 的 `MAVEN_OPTS=-DskipSurefireReport=true + Java 21` 前缀；该前缀只跳过 report 聚合。RED 前聚焦 baseline=`158/0F/0E/0S`；RED 后 direct=`85/5F/0E/0S`、authorization=`53/1F/0E/0S`、architecture=`26/1F/0E/0S`，合计 `164/7F/0E/0S`，其余 `157` 个用例通过。五个 fail-closed 负例均冻结 `BaseException`、可识别错误语义和资金零副作用；独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。Green 前须 fresh 运行三个独立 Spring 装配类与待删除 audit 测试并冻结各自 count；Green 若另获 Grant，因存在 DELETE 必须从 `clean-compile` 开始，再运行聚焦 slice、三个存续 Spring 装配类、`test-transaction`、`verify-public-contracts` 和 `git diff --check`。含既有 PaymentInstrument/Mockito 独立 blocker 的全量 `test-boundary` 不作为本卡硬门，模块边界由聚焦架构测试承重。
- **工作区证据**：HEAD=`eb12091819152fcec529f9453b48755f3aa2c999`；连续两次读取均为 default `160 / b8958afe976fce5c2b52985a83a01c9c0fff430199e068f378f435cb7e00a035`、`-uall 166 / 96e089d2941406a4118d4eaf4f96f46f29c5398dc9458f9e1a67e7266047c689`；staged empty，`git diff --check` PASS。
- **停止线**：禁止 core/face 新类型、DTO/DDL/Mapper/schema、Ledger search、converter 查询/业务判断、`transaction-impl -> ledger-face`、compat/V2/bridge/facade、profile/MIG-09/Consumer/HOST/L4/Git/release/production。真实 MySQL host 与独立 PMD 环境不是公共库当前门禁。
- **历史状态**：该 Green 在 `plan-r2.159` 按冻结白名单执行；Green 前四个装配/audit 测试 fresh 为 `51/0F/0E/0S`。聚焦验证与独立 Checker 发现测试合同边界错误后已经暂停，当前活动状态见 8.47。

### 8.47 `W5-MIG05-TRANSACTION-LEDGER-REFERENCE-GREEN-CONTRACT-REWORK-ENTRY-CARD`

- **触发事实**：Green 聚焦验证为 direct `85/3F`、authorization `53/1F`、architecture `26/0F`，合计 `164/4F/0E/0S`；独立 Checker=`NOT PASS / 0 P0 / 2 P1 / 0 P2`。四个失败均属于旧测试把 Transaction 引用选择与 Ledger 真实性证明混为一层，不能在生产白名单中合法修复。
- **不变设计**：A、`3 MODIFY + 9 DELETE + 0 ADD`、Transaction/Ledger Owner、route/participant/sibling 谓词与现有生产 Green 变更均不重开。Transaction 从 durable `root + route + detail` 选择唯一 distinct 非空 ref；Ledger 独立验证 transaction/plan/entry/route-leg。
- **四项返工**：missing Ledger 与 successor ref 污染改为断言 Ledger assembler 稳定 fail-closed 和零资金副作用；未被 detail 引用的额外 Ledger 行不得改变已选 ref；带费 `PAY` principal refund 必须保留原 `PAY + FEE` route，只选择 replayable non-FEE leg并校验 `replayRefLegId`。
- **未来测试白名单**：仅 `FundsDirectTransactionFlowTests.java` 与 `FundsAuthorizationTransactionFlowTests.java` 两个 `MODIFY`。不得改生产、fixture、架构测试、其他行为断言或新增文件；发现不同语义或新文件立即停止。
- **无兼容红线**：禁止旧 Wallet/Transaction 中文文案适配、Ledger 按 `fundsTransactionSn` 宽查询、`ledger-face` 依赖、bridge/facade/V2、双读、fallback、catch-and-relabel。额外 Ledger 完整性治理若有真实需求，另建 Ledger invariant/audit 卡。
- **执行与验收**：本卡 Checker PASS 后仍须 Human Owner 单独授予测试返工执行权；只运行 TDD 20.17.7 的聚焦命令，目标 `164/0F/0E/0S`。达到后再恢复原 Green 其余门禁与独立 Checker，不能直接宣告 Green PASS。
- **工作区证据**：成包前连续两次 live checkout 稳定为 HEAD=`eb12091819152fcec529f9453b48755f3aa2c999`、default=`174/77855bb0e397e9be58d4d8702233822eaf91ab2079b78d157fcf144bdd670bd7`、`-uall=180/2d1a5096fb7786774efb7043731f41c925d19d02103b2bbb28ce1c3b44dad98f`，staged empty、`git diff --check` PASS。未来执行前必须重读 live manifest。
- **历史状态**：返工卡与执行结果独立 Checker 均为 `PASS / 0 P0 / 0 P1 / 0 P2`。聚焦三类为 Direct=`85/0F/0E/0S`、Authorization=`53/0F/0E/0S`、Architecture=`26/0F/0E/0S`，合计 `164/0F/0E/0S`；实际返工文件仅两个 flow test。FeeFlow=`17/0F/0E/0S`，完整 transaction=`176/0F/0E/0S`。`plan-r2.162` 已关闭，当前入口见 8.48。

### 8.48 `CI-MIG05B-LEDGER-PROFILE-OWNERSHIP-001` 决策包

- **单一命题**：Ledger profile 定义和 required-ledger 初始化应由哪个物理 surface 承接，以及 Transaction 如何退出 profile 预检查；不重裁账户 identity、route、posting、Ledger/Balance 完成或 MIG-09。
- **E2 事实**：Wallet face 当前公开 profile service/两个 DTO/initializer/request，Wallet impl 持有静态 catalog 与创建/复验；Funding/Credit account create 是两个生产 initializer caller，Transaction settlement 是唯一 profile query caller。`ledger-face.LedgerService` 已有 create/query ledger，Wallet impl 已合法依赖 ledger face；Transaction impl 禁止依赖 ledger face。
- **Consumer 事实**：`capte-domain` 生产只消费 `LedgerProfileCode` 账户事实，没有 profile query/initializer 调用；两个测试宿主显式装配 Wallet 默认 profile/initializer，仅需未来测试装配迁移。
- **共同合同**：只保留 `LedgerProfileCode` 作为账户事实引用；现有 core `LedgerProfileSpec/ItemSpec` 与 stable API 记录同 Wallet profile/initializer surface 一次删除。profile catalog/item 与版本解释归 Ledger；Wallet 只提交 admitted subject/account facts；Transaction 不读 profile、不自动建账。初始化与 posting/admission 复用同一按 `profileCode + version` 选择的 catalog integrity guard，逐 bucket 复验，缺失、多命中或漂移 fail-closed。
- **A `LEDGER-SERVICE-CONTROLLED-INITIALIZATION`（已接受，Acceptance Checker PASS）**：复用现有 `LedgerService` 增加受控 required-ledger 初始化命令，请求归 ledger face，catalog/integrity guard/创建/复验归 ledger impl 内部；不新增单实现 service，不公开 profile DTO。
- **B `DEDICATED-LEDGER-SUBJECT-ADMISSION-SERVICE`（未选择，非 fallback）**：新增独立 admission service；边界正确，但当前仅两个 Wallet caller，缺少相对现有 `LedgerService` 的独立职责证据。
- **C `MOVE-PROFILE-READ-SURFACE-AS-IS`（拒绝，非 fallback）**：原样搬迁 profile read/DTO；泄露内部配置并诱导 Transaction 直依赖 Ledger face，保留错误预检查。
- **并发与事务**：命令使用本地 REQUIRED 事务并加入 Funding/Credit 外层事务；任一 bucket 失败时账户与本次全部 ledger 回滚。同一物理 bucket key 下，同 profile/version/catalog 语义的并发 loser 必须在 winner 可见后回读完整 bucket set并逐字段复验一致，随后 `void` 幂等完成；调用方仅可通过既有 Ledger query 观察 durable identities。同一 key 下 profile/version 或 catalog bucket 语义不一致的 loser 稳定冲突、命令不成功且零部分事实。不同 subject、currency 或 effective period 是独立 bucket，应各自成功；禁止泄露偶发 `DuplicateKeyException`。
- **无兼容迁移约束**：Owner 接受后必须一次删除 Wallet 五个 profile/initializer Public 类型、core 两个 profile spec/stable baseline、两个实现和旧 profile contract test，同批迁移 22 个 wind-funds 直接引用/装配测试源、`RecordingLedgerService` 与 capte-domain 两个测试宿主；不得保留 alias、bridge、双 Bean、双 DTO、fallback 或按 Consumer 选 Owner。
- **工作区证据**：HEAD=`eb12091819152fcec529f9453b48755f3aa2c999`；成包前连续两次 default manifest=`175 / 4e0128674d9cf43799ffbbe34403b785b3ee4a961301be845e2d38dcbbf3d441`、`-uall=181 / 15c2ae90b3d9eddb6c1707a2c0d943c4be322dcc53feb9b6a16f5969a86109f6`，staged empty、`git diff --check` PASS。
- **Owner/status**：Human Owner 已选择 A；B/C 未选择且不是 fallback。Decision Package、Acceptance Checker、原 Entry Card、三检查点返工卡、surface execution Checker、方向证据返工、signed adjustment Entry/RED/Green 与外部资金腿最终门禁均已 `PASS / 0 P0 / 0 P1 / 0 P2`；`plan-r2.169` 首轮 RED 与早期 Green NOT PASS 只作历史。上述授权均已关闭，当前活动状态见 8.56。

### 8.49 `W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-ENTRY-CARD`

- **目标**：把已接受的 A 冻结为无兼容、可执行的单一文件卡；不重裁 profile 产品语义、账户 identity、route、posting、MIG-09 或 Consumer 生产接入。
- **最小 Public surface**：`ledger-face` 新增一个 `InitializeSubjectLedgerRequest`，字段为 tenant/subject/type/currency/profileCode/profileVersion/optional period；既有 `LedgerService` 只新增 `void initializeRequiredLedgers(request)`。不返回 map/DTO，不新增 service、SPI、resolver、registry、factory 或 compatibility overload。
- **文档先行闭包**：高置信分册 `docs/系分设计/02-交易路由钱包账目与投影系分设计.md` 已同步删除旧 `SubjectLedgerInitializer + ledgerId map` 目标口径，统一为 `LedgerService void command + ledger-impl internal catalog + existing Ledger query`；该同步是当前文档修订，不进入未来 Green 生产文件白名单。
- **单一内部实现**：`ledger-impl` 只新增一个 concrete `LedgerProfileCatalog`，由 `LedgerServiceImpl` 与 `DefaultLedgerPostingAssembler` 共用；它唯一解释 versioned profile、required bucket set 和完整 catalog integrity。不得复制静态表或建立第二实现。
- **仓内 Green 白名单**：系分 11.11.9 冻结 `ADD=2 / MODIFY=8 / DELETE=9`。删除 core 两个 profile spec、Wallet 五个 profile/initializer surface、两个默认实现；修改 LedgerService/impl/posting、Funding/Credit、Settlement、core API baseline 与 Justfile。零 POM/schema/Mapper/新表。
- **测试闭包**：TDD 20.17.9 冻结 `ADD=1 / MODIFY=6` 的七组 RED；未来 Green 只可机械迁移列出的 26 个测试/fixture 文件并删除旧 `LedgerProfileContractTests`，行为断言不可弱化。22 个旧默认实现引用源由 `21 MODIFY + 1 DELETE` 关闭，另单独纳入自定义 `SubjectLedgerInitializer` Bean、`RecordingLedgerService` 和新的行为/架构测试。
- **事务/并发**：初始化加入 Funding/Credit 外层 REQUIRED 事务；任一 required bucket 失败，账户与本次全部 ledger 回滚。同 bucket 同语义并发 loser 只可回读完整 winner buckets并逐字段复验后 `void` 幂等完成；同 bucket 异语义稳定冲突且命令不成功；不同 subject/currency/effective period 独立成功。调用方仅可通过既有 Ledger query 观察 durable identities。
- **fail-closed**：初始化和 posting/admission 使用同一 catalog guard；missing/multi-match 或 profile/version/subject/category/normalSide/allowNegative/period/settlementPolicy/cutoff 任一漂移均拒绝。Transaction 删除 profile read，不新增 ledger-face 依赖，零成功 posting/LedgerEntry/Balance。
- **Consumer handoff**：capte-domain 生产只保留 `LedgerProfileCode` 使用；两个测试宿主路径登记为独立 Consumer migration，不进入本仓写白名单，也不构成兼容 API 理由。breaking artifact 发布前必须另获 Grant 并完成迁移。
- **验证**：先 clean compile，再核 core API/Public Contract；RED/Green 都执行 TDD 20.17.9 的精确类清单并生成 fresh Surefire XML，再跑 ledger/transaction/reconciliation 聚焦回归与 `git diff --check`。公共库不以真实 MySQL host 或独立 PMD 环境为当前门禁。
- **工作区证据**：成包前连续只读复核稳定为 HEAD=`eb12091819152fcec529f9453b48755f3aa2c999`、default=`175/4e0128674d9cf43799ffbbe34403b785b3ee4a961301be845e2d38dcbbf3d441`、`-uall=181/15c2ae90b3d9eddb6c1707a2c0d943c4be322dcc53feb9b6a16f5969a86109f6`，staged empty、`git diff --check` PASS。
- **历史关闭**：Human Owner 在 `plan-r2.169` 授权七组 RED；fresh 基线=`69/0F/0E/0S`、RED=`70/7F/0E/0S`、compile=`21/21`。独立 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 1 P2`：surface 缺失提前截断 RED-002/003，行为矩阵不完整，测试命名仍指向旧 Owner。该 Grant 已消耗且不准出 RED Complete；当前替代入口见 8.50。

### 8.50 `W5-MIG05B` surface/behavior 三检查点 Entry Card 返工

- **目标与状态**：三检查点返工卡与 surface execution 独立 Checker均已 `PASS / 0 P0 / 0 P1 / 0 P2`；Behavioral RED 独立 Checker 为 `PASS / 0 P0 / 0 P1 / 1 P2_ENVIRONMENT_RESIDUAL`。本节所述 `plan-r2.173` Green 决策门已经消耗且未准出，当前替代入口只见 8.52；A/B/C 不重开。
- **问题机制**：原卡把新 Public surface 出现与 catalog/并发/原子性/posting 行为 RED 放在同一轮。新 request 不存在时，后四类断言不可达；七个 JUnit failure 因而不等于七个独立行为证据。
- **检查点一 `CONTRACT_SURFACE_OWNERSHIP_MOVE`**：在 11.11.9 既有 breaking closure 内删除旧 core/Wallet surface，把现有顺序初始化与 catalog 原样迁到 `LedgerService + ledger-impl`，并把 Transaction 当前对三类 required item 的静态存在性预读等价移入 Ledger assembler，保持 profile/bucket、开户和既有 fail-closed 行为。用 clean compile、Public contract、architecture 与迁移 caller baseline 证明 Owner/依赖方向；不实现新增并发、整组回滚或 ledger-row/catalog drift guard，不发布。
- **检查点二 `BEHAVIORAL_RED`**：surface 后先 fresh 建基线，再按 TDD 20.17.10 拆分并发、Funding/Credit 回滚、assembler catalog matrix 与 Settlement 三 bucket/代表性 version drift。已满足场景保留为 characterization，不为凑 failure 改坏现有实现；每个实际 failure 必须唯一映射一个不变量且 `errors=0/skipped=0`。
- **检查点三 `BEHAVIORAL_GREEN`**：只实现已观察的精准 RED。`LedgerProfileCatalog` 保持唯一 concrete guard，初始化与 posting 共用；禁止第二 service/catalog、DTO/schema、兼容层、Transaction ledger-face 或 Consumer 生产改动。
- **价值验收**：Funding/Credit required bucket 完整；同 key 并发只有一套 durable identities；冲突时零孤儿账户/半套账本；profile/bucket 漂移时零成功 posting/LedgerEntry/Balance；旧 surface/生产引用为零。没有生产数据时不声明事故率、人工成本、性能或财务 ROI。
- **唯一文件与命令源**：生产/caller 总 closure 仍以系分 11.11.9 为准，检查点级 `ADD/MODIFY/DELETE` 分配以系分 11.11.10 为准；surface 对 assembler 只做现有检查的 Owner 迁移，Behavioral RED 只修改五个测试 Owner，Behavioral Green 候选仅四个生产文件。三检查点测试矩阵、动态 RED 计数和验证顺序只以 TDD 20.17.10 为准。任一检查点必须先获独立 Human Owner Grant 与 Checker PASS，授权不得继承；发现未列文件立即停止并重冻。
- **surface execution evidence**：按 `ADD=2 / MODIFY=8 / DELETE=9` 及 26 caller + 1 contract 删除完成无兼容 ownership move；clean core API compile=`21/21`、Public Contract=`313/186/42`、非 Mockito caller closure=`258/3F/0E/0S`，三项 failure 精准保留为并发 winner、Funding 整组回滚与 Settlement catalog drift。assembler `10E` 仅为既有 Mockito/ByteBuddy sandbox self-attach；transaction=`176/0F/0E/0S`，reconciliation=`236/1F/0E/0S` 且唯一 failure 为同一 Settlement RED，最终 compile=`21/21`。workspace stable：HEAD=`eb12091819152fcec529f9453b48755f3aa2c999`、default=`214/2f7cfbc3b60553d2fa86d464018e4d001fa7fb2cd401b128c4fff4a33abc95f9`、`-uall=220/2549d6be6db65812509a1960739a49591e24d242b7f3c738a2ee32e0507ef306`、staged empty、diff-check PASS；独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。

### 8.51 `W5-MIG05B` Behavioral RED execution closeout

- Human Owner 仅授权五个冻结测试 Owner 形成 Behavioral RED；本轮未修改生产 Java、Public API/DTO、POM/DDL/Mapper/schema、Consumer 或 Git。
- Fresh Surefire：`LedgerServiceImplTests=5/0F/0E/0S`（characterization）；`ControlAccountLedgerInitializationTests=14/2F/0E/0S`（同 bucket 并发 winner 与 Credit 后序 bucket 整组回滚）；`FundingAccountServiceImplTests=10/2F/0E/0S`（Funding 回滚与既有 profile drift）；`DefaultLedgerPostingAssemblerTests=11/0F/11E/0S`（Mockito/ByteBuddy sandbox 在 Spring context 前 self-attach 阻断，未形成可采信的 assembler 行为 RED）；`FundsSettlementTransactionFlowTests=10/1F/0E/0S`（profile drift fail-closed 缺口）。
- `clean-compile=21/21`、`git diff --check=PASS`；独立 Checker=`PASS / 0 P0 / 0 P1 / 1 P2`。P2 仅是公共库本地 Mockito/ByteBuddy 环境残余，不把真实 MySQL host 或独立 PMD 环境设为当前公共库门禁。
- 当时只等待 `W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-GREEN-EXECUTION-GRANT` 的 Human Owner 决策；该门已消耗且 Checker 未准出。当前只允许 8.52 的最小返工卡复核，不能新增 service/SPI/DTO/schema/第二 catalog/兼容路径或触碰 Consumer。

### 8.52 `W5-MIG05B` Behavioral Green Entry Card 返工

- **输入事实**：Human Owner 授权按冻结 Green 推进后，候选实现使 `compile=21/21`，并在命令行显式启用既有 `FlexTransactionManager` 时使 Ledger/Control/Funding 三类合计 `29/0F/0E`；但 Assembler=`11/1F/3E`、Settlement=`10/1F/9E`。独立 Checker 判定 `NOT PASS / 0 P0 / 3 P1 / 0 P2`，该 Green Grant 已消耗且未准出。
- **三个根因**：初始化与 posting 各自复制 catalog 字段比较，尚未共享唯一 guard；两个测试 fixture 生成与账户 profile 不一致的账本行；Funding/Credit 原子回滚只被命令行临时事务属性证明。不能通过放宽生产 guard、保留错误 fixture 或依赖特殊命令准出。
- **唯一返工白名单**：生产只允许 `LedgerProfileCatalog.java`、`LedgerServiceImpl.java`、`DefaultLedgerPostingAssembler.java`；测试只允许 `DefaultLedgerPostingAssemblerTests.java`、`FundsTransactionFlowTestSupport.java`、`FundsBenefitContributionTransactionServiceFlowTests.java`、`AcquiringSettlementBusinessFlowTests.java`、`AgentCommissionSettlementBusinessFlowTests.java`、`ClearingBatchApplicationServiceTests.java`、`FundsClearingTransactionFlowTests.java`、`PayoutOrderApplicationServiceTests.java`、`SettlementOrderApplicationServiceTests.java`、`ControlAccountLedgerInitializationTests.java`、`FundingAccountServiceImplTests.java`。逐文件责任以系分 11.11.11 为唯一来源；六个已知 caller 只做 `FUNDING_BASIC -> FUNDING_MERCHANT` fixture 归位，不改变业务断言。
- **不可修改证据**：`LedgerServiceImplTests.java`、`FundsSettlementTransactionFlowTests.java`、`LedgerControlledInitializationPublicContractTests.java`、`FundsModuleDependencyBoundaryTests.java`、`AbstractFundsServiceTest.java`，以及其余生产/API/schema/build/Consumer 文件。
- **准出门槛**：六类 focused Ledger=`5`、Control=`14`、Funding=`10`、Assembler=`11`、Benefit=`13`、Settlement=`10`，合计 `63/0F/0E/0S` 且 fresh XML；共享 fixture 的 24 个直接子类全部 fresh 通过，再执行 ledger/transaction/reconciliation 回归与 diff check。真实 MySQL host 和独立 PMD 环境不作为公共库门禁。
- **状态与停止线**：本卡独立 Checker已 `PASS / 0 P0 / 0 P1 / 0 P2`。当时进入的 Green rework Grant 已消耗且未准出；测试证据返工、signed adjustment Entry/RED/Green 与外部资金腿最终门禁均已 PASS。该段为历史，当前活动状态见 8.56。

### 8.53 `W5-MIG05B` 外部资金腿记账方向文档 Entry Card 返工

- **触发事实**：`plan-r2.175` Green rework 候选 compile=`21/21`，但六类 focused 为 `63 tests / 1F / 16E / 0S`；共同根因是 `DefaultLedgerPostingAssembler` 对 `FUND_IN/EXTERNAL_IN` 仍统一应用 source `DECREASE`、target `INCREASE`，使平台 `CASH` 资产和客户负债应同时增加的计划不平衡。该 Green Grant 已消耗且未准出。
- **已接受方向合同**：route source/target 是经济路径端点，不是 debit/credit。ordinary `EXTERNAL_IN=INCREASE/INCREASE`；ordinary `EXTERNAL_OUT=DECREASE/DECREASE`；ordinary internal leg 及无 `replayRefLegId` 的 `RESTORE/RELEASE`=`DECREASE/INCREASE`；只有具有非空且唯一原腿引用的 reverse-class refund/reversal/release replay 才按 original posting entries 逐项反向，authorization completion 等非反向 successor 继续按当前稳定 leg 入账；explicit adjust 保留指令 effect。entry side 仍由 Ledger normal side + effect 推导，plan 必须平衡。
- **公共价值**：同一不变量覆盖 topup、ACH/card/VCC 入金、payout 和退款/冲正；上游 adapter 继续拥有外部 authority/finality，Ledger 只负责规范化 leg 到平衡 posting 的确定性翻译。没有场景策略、rail 状态或宿主流程进入公共层。
- **最小 Green 候选**：生产只有 `MODIFY ledger/impl/src/main/java/com/wind/funds/ledger/posting/DefaultLedgerPostingAssembler.java`；测试只有系分 11.11.12 的 16 个现有文件。`ADD=0 / DELETE=0`，不新增 API/DTO/schema/service/enum/direction engine/compat/V2/双路径/fallback。
- **RED 一手证据**：Human Owner 已授予并消耗 RED Grant。Assembler 四个承重方法 fresh=`4/2F/0E/0S`：`EXTERNAL_IN` 与 `EXTERNAL_OUT` 分别精准失败于 `LEG-POSTING-TOPUP`、`LEG-POSTING-WITHDRAW` plan 不平衡，internal derive 与跨交易 replay reject 保持通过；源码 SHA-256=`fb1b47f24b88a196cc589750a4fe0dd5713f71ffcb21495a0624177f4a63356b`，XML SHA-256=`71fdad0f5a033c26fa35a0a86eebefc7fefd3e41cc0b33e3e4c3acb2235b2180`。15 个 signed-CASH caller 已完成静态迁移闭包和 test compilation。
- **证据分阶段返工**：RED 动态门只要求上述四个 assembler 方法的 `2F/2P/0E/0S`；15 个 signed-CASH caller 在共享 assembler 根因修复前不重复执行同一前置失败。Green 修复后，它们必须逐类 fresh `0F/0E/0S` 并实际到达 ordinary/reverse、余额、posting、LedgerEntry、route provenance 与幂等断言。Benefit、Settlement、ExternalFundsEvent、PaymentInstrument、profile 初始化/回滚、Public Contract、schema/build 和共享 fixture 仍不可修改，只作 Green 验证证据。
- **Checker 裁决**：方向 RED 独立 Checker=`NOT PASS / 0 P0 / 1 P1 / 0 P2`；唯一 P1 是原卡同时要求共享根因保持 RED、又要求 15 个依赖该根因的 caller 在 RED 阶段逐类 fresh 到达后置断言。该 finding 不否定方向合同、测试或单文件 Green，只要求重排证据阶段。
- **工作区 tuple**：HEAD=`eb12091819152fcec529f9453b48755f3aa2c999`；文档返工写入前 default=`224/a0aeaad437fa9ac956b3f1a2f03b583a77db3841243184293fefc315b75b6c2d`；`-uall=230/6064f27636759e54af915b14c9e4fc86cfd8801e4253efe35a7f1e653c88ebb4`；staged empty；pre-write `git diff --check=PASS`。这些值只绑定当前 dirty checkout，不冒充提交基线。
- **状态与停止线（已关闭历史）**：返工卡独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。当时进入 `plan-r2.179` Green Grant；该授权已消耗且未准出，当前只见 8.54。

### 8.54 `W5-MIG05B` 外部资金腿 Green 证据 Entry Card 返工

- **触发事实**：Human Owner 曾授权单文件 Green。Assembler 外部腿候选使四个承重方法由 `4/2F/0E/0S` 转为 `4/0F/0E/0S`，但 15 类 caller fresh=`235/79F/2E/0S`；独立 Checker 判定 `NOT PASS / 0 P0 / 3 P1 / 0 P2`，该 Green 未准出。
- **根因分类**：profile fixture=`59`，旧 normal-side/外出 CASH 断言=`20`，独立 `FUNDING_BALANCE_ADJUST`=`2`。三者不得合并成新 direction 语义，也不得以放宽 catalog 或兼容分支解决。
- **最小文件卡**：生产候选、assembler 方向测试、`LedgerProfileCatalog` 和 `FundsTransactionFlowTestSupport` 全部冻结。未来测试证据返工只允许系分 11.11.13 的 `8 MODIFY / 0 ADD / 0 DELETE`，使用已有 `FUNDING_MERCHANT/FUNDING_BASIC`，并经既有 `findLedger(...)` 读取 `LedgerDTO.normalBalanceSide` 后按 production 公式计算余额，保留原业务流程与断言。
- **分阶段门禁**：证据返工必须精确收敛为 `235/1F/1E/0S`，只保留两个已命名 `FUNDING_BALANCE_ADJUST` 业务缺口；其中余额不足场景为 `1F`，commission adjust 直接暴露 posting plan 不平衡为 `1E`，该 error 不是环境错误。然后必须另建 explicit-adjust Entry Card 闭合平衡、余额限制、幂等和失败零副作用；其 Checker PASS 后才回到本方向切片验证 `235/0F/0E/0S` 与既有扩大门禁。
- **工作区 tuple**：HEAD=`eb12091819152fcec529f9453b48755f3aa2c999`；写入前两次连续读取均为 default=`224/a0aeaad437fa9ac956b3f1a2f03b583a77db3841243184293fefc315b75b6c2d`、`-uall=230/6064f27636759e54af915b14c9e4fc86cfd8801e4253efe35a7f1e653c88ebb4`、staged empty、`git diff --check=PASS`。
- **状态与停止线（已关闭历史）**：测试证据返工已完成，15 类 fresh=`235/1F/1E/0S`、其余 `233 PASS`，独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。该 `plan-r2.182 / ENTRY_CARD_REQUIRED` 已由 8.55 的 `plan-r2.183` Entry Card 成包接替，并在 `plan-r2.184` 通过独立 Checker；不授权生产、测试、Git、HOST/L4 或发布。

### 8.55 `W5-MIG05B` FUNDING_BALANCE_ADJUST signed accounting Entry Card

- **触发事实**：8.54 已将外部资金腿证据收敛到 `235/1F/1E/0S`。两个剩余缺口都属于 `FUNDING_BALANCE_ADJUST`：余额不足 decrease 未稳定 fail-closed，commission decrease 在 posting plan 平衡处失败；它们不是 external-leg direction、profile fixture 或宿主环境问题。
- **Owner 选择**：Human Owner 接受 `CI-MIG05B-FUNDING-BALANCE-ADJUST-ACCOUNTING-SEMANTICS-001-A`。平台 `ADJUSTMENT` 是 `DEBIT` normal、允许负数的内部暂挂余额，目标 `AVAILABLE` 是 `CREDIT` normal 客户负债；increase 对两端都应用显式 `INCREASE`，decrease 对两端都应用显式 `DECREASE`。route source/target 只表达经济路径，entry side 继续由 `normal side + balance effect` 唯一推导。
- **业务读法**：increase 时平台 `ADJUSTMENT` 记 DEBIT、目标 `AVAILABLE` 记 CREDIT，两端余额各 `+X`；decrease 时目标 `AVAILABLE` 记 DEBIT、平台 `ADJUSTMENT` 记 CREDIT，两端余额各 `-X`。目标 `AVAILABLE` 不足时必须在 posting/entry/balance 成功事实形成前 fail-closed；失败不得覆盖既有 commission/clearing 或 adjustment 事实。
- **公共能力边界**：本卡只固定跨场景稳定的 signed balance-control accounting invariant。调整原因、审批、额度、外部 finality 和是否允许某业务发起调整仍由上游负责；当前只有 wind-funds E2 流程证据，没有生产 Consumer E4，不把单一场景对象或策略放入 Public API。
- **最小 RED 白名单**：只允许 `MODIFY` 三个既有测试文件：`tests/src/test/java/com/wind/funds/ledger/posting/DefaultLedgerPostingAssemblerTests.java`、`tests/src/test/java/com/wind/funds/transaction/application/flow/FundsBalanceControlFailureFlowTests.java`、`tests/src/test/java/com/wind/funds/transaction/application/flow/AgentCommissionSettlementBusinessFlowTests.java`。Assembler 测试须删除/替换通用 matrix 中伪造的普通 `AVAILABLE -> AVAILABLE` `BALANCE_ADJUST` case，由两个专用方法承接 phase/effect/intent/scope，并仅在该文件内部调整 `RecordingLedgerService` 以提供真实平台 `ADJUSTMENT` 与目标 `AVAILABLE`；不得修改生产 catalog/shared fixture。四个目标方法必须 fresh=`4/4F/0E/0S`，分别证明 increase、decrease、余额不足零副作用和 commission 既有事实不可变；任何 fixture/Spring/H2 error 都不是有效 RED。
- **最小 Green 白名单**：只允许 `MODIFY ledger/impl/src/main/java/com/wind/funds/ledger/posting/DefaultLedgerPostingAssembler.java`。既有 request、route resolver、converter、service、profile catalog、balance projection、共享 test support 和 Public surface 全部不可修改；`FundsAccountCapabilityAdmissionFlowTests` 只作 Green 回归。`ADD=0 / DELETE=0`，不新增 direction engine、SPI、registry、compat/V2、双路径或 fallback。
- **验证与恢复**：Green 后先要求上述四目标 `4/0F/0E/0S`，再 fresh 执行 capability regression、external-leg assembler 四方法、15 类 signed-CASH=`235/0F/0E/0S` 及卡内既有扩大门禁。当前 pre-write tuple：HEAD=`eb12091819152fcec529f9453b48755f3aa2c999`；default=`224/a0aeaad437fa9ac956b3f1a2f03b583a77db3841243184293fefc315b75b6c2d`；`-uall=230/6064f27636759e54af915b14c9e4fc86cfd8801e4253efe35a7f1e653c88ebb4`；staged empty；`git diff --check=PASS`。
- **Green 证据与停止线**：唯一生产改动使 `BALANCE_ADJUST` 两端复用既有显式 effect；余额不足测试只把异常类型对齐为既有确定性 `LedgerPostingRejectedException`，零成功资金/账务效果断言未弱化。四格=`4/0F/0E/0S`；capability=`1/0`、balance-control=`44/0`、business-flow=`204/0`、外部资金腿 15 类=`235/0`、外部腿 assembler=`4/0`、compile=`21/21`、ledger=`61/0`、transaction=`176/0`、reconciliation=`238/0`、Public Contract=`313/186/42`，独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。该切片在 `plan-r2.186` 关闭，授权已耗尽；当前状态见 8.56。

### 8.56 `W5-MIG05B` 余额调账非负 Public surface Entry Card

- **触发事实**：现有 Public Request 与 context 暴露六个负余额输入，route 只校验字段完整，posting 直接读取 raw flag；没有生产 Consumer、authority、额度占用、并发累计、撤销/到期或恢复证据。该路径不满足公共资金准入的可验证性。
- **Owner 选择**：Human Owner 接受 `CI-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-001-A / REMOVE_UNPROVEN_NEGATIVE_BALANCE_PUBLIC_SURFACE`。无兼容删除六个 Request 字段、六个 transaction-face key 和 core `ALLOW_NEGATIVE_BALANCE`；不保留 deprecated、V2、alias、bridge、双入口或 raw context fallback。
- **保持不变**：`FundsBalanceControlService.adjust`、审计/来源/`approvalRef`、signed `ADJUSTMENT`、Money、route、posting、LedgerEntry、Balance 与幂等不变；Ledger internal profile/catalog 的 `allowNegative` 和 `LedgerBalanceConstraintType.ALLOW_NEGATIVE` 保留。`BALANCE_ADJUST/LIMIT_ADJUST` decrease 的资金账户 source entry 固定 `MUST_NOT_BE_NEGATIVE`，平台 `ADJUSTMENT` target entry 继续 `PROFILE_DEFAULT`。
- **生产/契约白名单**：`7 MODIFY / 0 ADD / 0 DELETE`，精确为 `FundsBalanceAdjustRequest.java`、`FundsInstructionContextKeys.java`、`FundsContextVariables.java`、`core/api-baseline/stable-api.txt`、`FundsBalanceControlInstructionConverter.java`、`BalanceControlFundsInstructionRouteResolver.java`、`DefaultLedgerPostingAssembler.java`；逐文件责任以系分 11.11.15 为准。
- **测试白名单**：RED=`3 MODIFY / 0 ADD / 0 DELETE`。`FundsTransactionRequestContextVariablesContractTests.java` 以一次聚合 reflection failure 冻结 surface absence；`LedgerDtoContextVariablesContractTests.java` 只做中性审计 key 的机械改名并保持 PASS；`FundsBalanceControlFailureFlowTests.java` 强化既有余额不足用例，通过通用 context 注入完整六项 raw tuple，仍要求标准 `LedgerPostingRejectedException`、允许的 FAILED funds fact/解释 route 与零成功 posting、LedgerEntry、Balance 效果。RED 目标为两项精准 failure、零 error，具体总数由 fresh RED 冻结。Green 中三份 RED 文件 immutable，再执行 TDD 20.17.15 的 focused 与扩大门禁。
- **停止线**：真实 Consumer、新 authority/limit/recovery 持久事实、内部 Ledger allow-negative 删除、新 DTO/service/schema、兼容或白名单外承重文件任一出现即停止重冻。首轮 Checker=`NOT_PASS_P0_0_P1_1_P2_0`，返工 Checker=`PASS_P0_0_P1_0_P2_0`，RED Checker=`PASS_P0_0_P1_0_P2_0`；七文件 Green 已执行且授权耗尽，当前状态由 8.57 接替，不授权修改 RED 测试、Consumer、Git、HOST/L4 或发布。

### 8.57 `W5-MIG05B` 非负 surface Green 证据与仓库基线拆分卡

- **触发与状态**：Human Owner 已授权并完成 8.56 的七文件 Green，三份 RED 测试保持 immutable。主三类=`30/0F/0E/0S`、佣金流=`4/0`、capability 精确方法=`1/0`、assembler=`15/0`、balance-control=`44/0`、transaction=`176/0`、business-flow=`204/0`、ledger=`61/0`、compile=`21/21`、Public Contract=`313/186/42`；源码复核确认无兼容删除、raw bypass 关闭、decrease 非负、signed adjustment 与 Ledger internal allow-negative 保持。该证据只登记 `GREEN_IMPLEMENTATION_VERIFIED`。
- **Checker 裁决**：独立 Green Checker=`NOT_PASS_P0_0_P1_2_P2_0`。P1-A 为完整 `FundsAccountCapabilityAdmissionFlowTests=12/1F/0E/0S`：PAY 用例在 capability 准入前把默认 `FUNDING_BASIC` payee 用于其 profile 不包含的 `CLEARING`。P1-B 为 core API guard 的历史 `107/99` cardinality 与当前已接受删除两个 profile spec 后 `105/97` 不一致；展开后只剩 baseline `FundsAccount#getStatus()` 与源码 `getState()` 一项 remove/add 漂移。两项均不指向七文件资金行为，但原卡要求完整门禁通过，因此不得登记 Green Checker PASS。
- **证据分层**：切片级 `GREEN_IMPLEMENTATION_VERIFIED` 与仓库级 `REPOSITORY_BASELINE_BLOCKED` 必须同时保留。前者证明已接受的资金不变量，不能替代仓库基线、发布或生产；后者不能反向要求恢复已删除 Public surface、兼容方法或改写本切片测试。
- **后续卡 A**：`BASELINE-CAPABILITY-PAYEE-PROFILE-FIXTURE-REPAIR-001`。未来写白名单仅 `MODIFY tests/src/test/java/com/wind/funds/transaction/application/flow/FundsAccountCapabilityAdmissionFlowTests.java`：在 `testPayShouldRequireBothAccountCapabilities` 内复用既有 `ensureFundingAccount(payee, LedgerProfileCode.FUNDING_MERCHANT)` 后再创建 `CLEARING`；保留 payer/payee capability、失败资金事实与零成功账务副作用断言。目标完整类=`12/0F/0E/0S`；任何共享 fixture、生产 Catalog、第二文件或不同业务语义需求均停止重冻。
- **后续卡 B**：`BASELINE-CORE-API-GOVERNANCE-REBASE-001`。未来写白名单仅 `MODIFY openspec/changes/funds-core-long-term-stability/spec.md`：新增 superseding decision，并机械更新其中仍作为当前权威的 `D-CS-006`、Public API Baseline Evidence、正式执行证据与 Core-1 当前表为 `105 public / 97 stable / 4 experimental / 4 internal / 1043 baseline lines / FundsAccount#getState()`；`107/99/1062`、两个旧 profile spec 与 `ALLOW_NEGATIVE_BALANCE` 保留为带日期历史，不删除历史。`MODIFY scripts/verify-core-api-baseline.sh` 将 cardinality/message 从 `107/99` 校准为 `105/97`；`MODIFY core/api-baseline/stable-api.txt` 以源码 `FundsAccount#getState()` 替换旧 `getStatus()`。`api-policy.tsv`、core Java 和其他 signature immutable；不恢复两个旧 profile spec，不增加 deprecated、alias、bridge 或兼容方法。目标=`just verify-core-api PASS`；任何额外 signature diff 或其他语义调整均停止重冻。
- **依赖与逐卡状态回写**：本证据卡、卡 A、卡 B 与最终 Green 复验均=`PASS_P0_0_P1_0_P2_0`；卡 A 完整 capability 类=`12/0F/0E/0S`，卡 B core API 当前权威=`105/97/4/4 / 1043 lines / getState()`。最终复验 fresh 门禁与 17/17 哈希均已通过并登记 `GREEN_INDEPENDENT_CHECKER_PASS`；相关 Execution Grant 已耗尽。

### 8.58 `W5-REFACTORING-PROGRESS-BASELINE-REFREEZE-001`

- **目的**：在非负 surface 最终 Green Checker PASS 后，用当前源码、测试与已关闭 Checker 重新校准 MIG-00~09，纠正文档完成、Provider 子切片 Green、Consumer E4/L4 和未开始删除之间的混写；不以单一百分比描述不同成熟度。
- **r2.201 当时的重基线结论**：`MIG-00/01` 当前范围完成；`MIG-02` 四类 ActionFact Provider Green，release 文档化、canonical refund 延期；`MIG-03` 物理事实链存在但稳定闭合能力缺失；`MIG-04` 文档 PASS、实现未开始；`MIG-05` 多个 Provider 子切片已 Green，但 core posting/projection 单实现接口与 composite 尚未收口；`MIG-06` 等待真实 Adapter；`MIG-07` Provider 当前范围 Green、Consumer E4/L4 独立等待；`MIG-08` 已识别 `capte-domain`，但无 Reconciliation 生产调用或独立部署数据库；`MIG-09` 未开始。当前结论只见顶部 Metadata 与 r2.232 Green 收口记录。
- **MIG-03 一手证据**：`FundsActionFactDTO` 明确不证明 Ledger/Balance；`DefaultFundsTransactionQueryService` 已投影四类 ActionFact；`LedgerTransactionService` 可按 funds transaction/event/business 查询 LedgerTransaction/Entry，`LedgerService` 可读账目投影；流程测试联合断言 Action/Ledger/Balance，但依赖内部 test support/JDBC。仓内没有按 `FundsActionFactRef` 输出闭合结果的稳定 service/DTO。
- **实际价值**：为 timeout/restart 去重、逆向前验证、Reconciliation source admission、审计与人工处置提供同一 action identity 下的三维证据重查，避免 root 状态、任意账本记录或当前余额互相冒充；`proven-zero`、缺失、多命中和语义冲突必须 fail-closed 且零修复副作用。
- **设计边界**：ActionFact、LedgerFact、Balance evidence 继续正交；不修改 `FundsActionFactDTO` 宣称账本或余额，不暴露 Entity/Mapper，不新建物化读模型、缓存、调度器、事件总线、第二写链或兼容层。Owner、真实 Consumer/Provider 恢复用例、候选模块和 Java shape 留给下一独立文档 Entry Card。
- **写入与验证**：本轮只修改既有五份权威文档；不运行 Java 测试、MySQL、PMD 或 Git。文档要求 current/recovery 单一、MIG 表互相一致、历史状态不被改写，并由独立 Checker 复核。
- **当时状态**：进度重基线已 `REFACTORING_PROGRESS_BASELINE_INDEPENDENT_CHECKER_PASS / plan-r2.202`；MIG-03 A、Contract RED 与 Green Entry Card 后续均通过独立 Checker。`plan-r2.214` 由 8.62 的 persisted digest 文档卡接替，当前状态只见 Metadata。

### 8.59 `W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-DOC-ENTRY-CARD-001`

- **授权与目标**：Human Owner 回复“按你的建议推进”，仅授权五文档形成 MIG-03 候选合同、首个真实 vertical slice、caller closure、TDD 种子、停止线和独立 Checker；不授权 Java、测试、DDL、API baseline、Consumer 或 Git。
- **真实入口**：`ClearingSplittableDetailApplicationServiceImpl#identifySplittableDetail` 当前接收 caller-selected `fundsTransactionSn + fundsTransactionDetailSn + ledgerEntrySn`，再读取 root/detail、LedgerTransaction/Entry/PostingPlan 做 source closure；直接测试 caller 为 clearing service、收单、代理分佣与 Gate behavior。仓外无生产 Consumer，证据等级为 Provider `E2`。
- **根因**：caller 拥有内部事实选择权，root `CLOSED` 仍参与完成判断，Reconciliation 重复理解 Transaction durable action group；继续增加校验不能形成稳定 ActionFact handoff。
- **候选 A（推荐）**：`TRANSACTION_RECORDED_REFERENCES_RECONCILIATION_VERIFIED_CLOSURE`。caller 只交 tenant + owner-qualified source action identity；Transaction 按 exact ActionFactRef 返回 principal、唯一 PAYEE、可选唯一 FEE_RECEIVER 的完整 matched sibling set，包括角色、Money、detail refs、semantic binding 和全组唯一 distinct recorded LedgerTransaction ref，不读取 Ledger；Reconciliation 再用 ledger-face 验证 transaction/plan/entries/Money/主体/digest，唯一选择 PAYEE/CLEARING credit，保留 fee 完整性但不误选，并以既有本地原子过账不变量判断 balance projection commit。首切仅 direct PAY primary proven-full -> merchant CLEARING credit。
- **候选 B**：新增 Ledger-owned durable per-action balance effect evidence；因需新 schema/写链/迁移且无当前证据，延后，不是 A fallback。
- **候选 C**：保持 caller-selected tuple；因 root/detail/entry 泄露与事实选择权错位而拒绝，只作当前基线。
- **关键边界**：ActionFact、Transaction-recorded refs、Ledger facts、Balance commit invariant 继续正交；不修改 ActionFact 使其宣称 Ledger/Balance，不解析 `attemptRef`，不返回历史伪 current balance，不新增 Governance 旁路、缓存、事件总线、物化读库、修复命令、兼容/V2/双读写。
- **当时状态**：首轮 Entry Card Checker=`NOT_PASS_P0_0_P1_1_P2_0`，返工后 Entry Card 与 A Acceptance Checker均=`PASS_P0_0_P1_0_P2_0`。B/C 均未选择且不是 fallback；后续 Green Entry Card 已被 8.62 persisted digest 文档卡接替，当前状态只见 Metadata。

### 8.60 `W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-RED-ENTRY-CARD-001`

- **授权边界**：Human Owner 在 RED Entry Card Checker PASS 与价值回归后回复“按你的建议推进”，授权唯一 Contract RED 执行。该 Grant 只允许新增冻结测试文件与执行验证，现已耗尽；不授权 Green、生产代码、既有测试、DDL、API baseline、Consumer 或 Git。
- **唯一 RED 写入**：`ADD tests/src/test/java/com/wind/funds/reconciliation/contract/FundsActionLedgerClosurePublicContractTests.java`。现有测试与全部生产文件 immutable。
- **两个精准失败**：fresh=`2/2F/0E/0S`；`recorded evidence contract missing` 命中 service/DTO 缺失，`clearing source request contract mismatch` 命中新 sourceActionFactRef 缺失和旧 root/detail/entry tuple。两项均为聚合 `AssertionError`，没有 missing class error、testCompile、Spring/H2 或环境失败。
- **候选物理 shape**：`FundsActionRecordedEvidenceQueryService#findRecordedEvidence(FundsActionFactRef)`；`FundsActionRecordedEvidenceDTO` 只含 ActionFact、nested sibling refs、唯一 recorded LedgerTransaction 与 recorded-reference digest；clearing request hard break 为 tenant + `StableIdentity sourceActionFactRef` + 原 policy fields。该 shape 是 RED 目标，不是当前 Java 授权。
- **候选 Green closure**：`ADD=2 / MODIFY=3` 生产文件，精确见系分 11.19.2；无 schema/POM/Mapper/API baseline。四个 clearing caller tests 与 `FundsDirectTransactionFlowTests` 只在后续 surface/behavior 卡机械迁移和验证，本 RED 不改。
- **验证**：Java 21 mvn-version PASS、compile=`21/21`、Public Contract=`313/186/42`。`just test-one` 在父级 Surefire HTML report 的私有 site descriptor / `~/.m2` 写锁处先行阻断；等价 Maven 命令仅增加 `-DskipSurefireReport=true`，保留 test compilation、Surefire 与 fresh XML。测试源码 SHA-256=`91ccb56ae80446f637b9e3500dc571507e3371001ce5ff33c1de90c10ab3e254`，XML SHA-256=`1dbcb0de75da6c22687a5d23c561d0ffb306a47e5db0fe9e29b19d80ab72f46d`。MySQL/PMD 不属于本卡，Git 仍未授权。
- **当时状态**：RED Entry Card、RED Execution 与 Green Entry Card 独立 Checker均=`PASS_P0_0_P1_0_P2_0`；`plan-r2.214` 后续已由 8.62 接替，当前状态只见 Metadata。

### 8.61 `W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-GREEN-ENTRY-CARD-001`

- **授权与目标**：Human Owner 回复“继续推进”，按 `plan-r2.212` 恢复入口只授权 Green Entry Card 文档设计与独立 Checker；不授权 Green、Java、测试、schema、POM、Consumer、Git 或发布。
- **生产 closure**：精确 `ADD=2 / MODIFY=3`。Transaction face 新增窄 recorded-evidence service/DTO，既有 `DefaultFundsTransactionQueryService` 实现 direct PAY principal proven-full 的完整 sibling 投影；Reconciliation request hard break 为 `tenantId + StableIdentity sourceActionFactRef + policy fields`，impl 使用 transaction-face + ledger-face 完成闭合与 PAYEE/CLEARING 唯一选择。没有第六个生产文件。
- **test/caller closure**：Contract RED 源码 immutable；`MODIFY=7` 为 `FundsDirectTransactionFlowTests`、`ClearingSplittableDetailApplicationServiceTests`、Acquiring/Agent/Gate 三个 caller、`ReconciliationTestFixture` 和 `ClearingSplitBatchApplicationServiceTests`。新增仅 Direct `+2` 与 Clearing `+4`，其余机械迁移 ActionFact request/Gate identity。
- **行为边界**：`ownerNamespace=funds`；Provider 不解析 attemptRef、不读 Ledger；recorded digest 覆盖 ActionFact digest与排序后完整 siblings；Reconciliation 用 Ledger count + exact-size entries 防止分页截断，验证每个 sibling/plan/digest并唯一选择 PAYEE。fee、proven-zero、UNKNOWN、unsupported、缺失/重复/不同 Ledger ref 均 fail-closed；退款和 Gate/idempotency 保持现有 Owner。
- **精确 Green 门**：Contract=`2/0`、聚焦=`148/0`、transaction=`178/0`、reconciliation=`242/0`、business-flow=`206/0`、compile=`21/21`、Public Contract=`315/187/42`；fresh XML 必须逐类复算。测试使用已证明的 Java 21 + `MAVEN_OPTS=-DskipSurefireReport=true` 前缀，仅跳过 HTML report。
- **排除与停止**：FundsActionFact DTO/Ref、broad query、Ledger face/impl、Mapper/Entity/schema/POM/core/Governance/Justfile/API baseline immutable；不兼容、不建 balance evidence、不用 current balance；Reconciliation 不解析 identity/attemptRef，Transaction 只复用既有 ActionFact identity resolver；不加缓存/事件/修复命令。任何未列文件或计数漂移停止重冻。
- **当时状态**：独立 Checker=`PASS_P0_0_P1_0_P2_0`；`plan-r2.214` 只进入 Human Owner Green Execution Grant 决策。该 Grant 后续已消耗，Green Execution Checker NOT PASS，当前由 8.62 接替。

### 8.62 `W5-MIG03-LEDGER-PERSISTED-DIGEST-CONTRACT-ENTRY-CARD-001`

- **授权来源**：Human Owner 回复“授权推进，推进时需要确认价值”，只授权既有五文档形成 persisted digest 价值确认、目标合同、候选文件卡、TDD 种子与独立 Checker；不授权 Java、测试、DDL、API、Consumer、Git 或发布。
- **前序 Green 事实**：MIG03 Green 已完成冻结的 ActionFact request/recorded-reference 主链部分实现，final compile=`21/21`、Contract=`2/0`、focused=`148/0`、Public Contract=`315/187/42`；独立 Checker 最终=`NOT_PASS_P0_0_P1_1_P2_0`，因此没有 Green 状态回写。先前扩大门 `transaction=178 / reconciliation=242 / business-flow=206` 是 blocker 发现前证据，不作为最终准出。
- **根因证据**：Ledger writer 在 insert 前把带小数秒的 `transactionTime` 纳入 transaction/entry digest；`jdbc-schema.sql` 的 `DATETIME` 回读不保留同一精度。真实 Acquiring flow 观察到写前 `13:19:46.713705`、回读 `13:19:47`，证明 Consumer 无法从 persisted facts 重建 stored digest。Reconciliation 内复制算法已被最小实验否证并撤回。
- **价值结论**：`VALUE_CONFIRMED / PUBLIC_LEDGER_INVARIANT`。可重建 digest 同时服务 same-key replay、stable-SN read integrity、clearing/reconciliation source admission、归档和审计；它不证明 current balance、外部 finality、业务完成或 reconciliation Balanced。
- **接受方案**：`A / LEDGER_INTERNAL_NORMALIZE_THEN_VERIFY`。Ledger 把承重时间归一到秒，BigDecimal 归一为去尾零 plain decimal 数值，transaction/plan/entry 使用唯一 versioned canonical builders，写后从 persisted entities 自校验，exact read 由 Ledger 内部 fail-closed。拒绝 `B` 的 DDL 精度绑定和 `C` 的移除时间字段。
- **无兼容**：目标只接受 `ledger.persisted-transaction/plan/entry.v1`；删除 legacy digest fallback，不双验、不回填、不创建兼容 facade。旧不匹配事实拒绝；真实宿主历史数据处置另立任务。
- **候选 closure**：生产 `MODIFY=1`，仅 `LedgerTransactionServiceImpl.java`；测试 `MODIFY=4`，仅 Ledger impl、Ledger fact query、ClearingSplittable 和 Gate behavior 四个现有类。`ledger-face`、DTO/Entity/Mapper/converter、schema/POM/core、Transaction/Reconciliation 生产源码、Justfile 和 API baseline immutable。
- **Checker 与返工**：首轮 Checker=`NOT_PASS_P0_0_P1_1_P2_0`，指出 request `exchangeRate=1` 与 DECIMAL 回读 `1.00000000` 会重现时间精度同构问题。原范围最小补齐 `BigDecimal.stripTrailingZeros().toPlainString()` canonical 与 scale-drift TDD 种子，白名单不变。
- **最终 Checker**：`PASS_P0_0_P1_0_P2_0`；确认 canonical decimal 关闭 scale drift，首轮其余价值、白名单、无兼容和测试闭包均未漂移。
- **历史最终状态**：`LEDGER_DIGEST_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / RED_ENTRY_CARD_DOCUMENTATION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.216`；当前由顶部 Metadata 接替。

### 8.63 `W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-ENTRY-CARD-001`

- **授权**：Human Owner 回复“推进吧”，按 r2.216 唯一 next gate 只授权五文档 RED Entry Card 与独立 Checker；不授权修改或运行测试、生产代码、DDL、Git 或发布。
- **RED whitelist**：`MODIFY=4 / ADD=0 / DELETE=0`，仅 Ledger impl tests、Ledger fact query tests、ClearingSplittable tests、Gate behavior tests；全部生产、schema、POM、Justfile、API baseline 与其他测试 immutable。
- **failure mapping**：`LD-RED-01` legacy acceptance；`LD-RED-02` canonical time/decimal round-trip；`LD-RED-03~05` transaction/plan/entry stable-SN read tamper；`LD-RED-06~08` clearing transaction/plan/entry tamper。
- **精确 RED**：Ledger impl=`10/2F`、Ledger query=`8/3F`、Clearing=`28/3F`、Gate=`10/0F`；focused=`56/8F/0E/0S`。八项均为带 stable label 的 assertion failure，不能出现 testCompile/H2/Spring error。
- **扩大证据**：test-ledger=`65/5F`、test-reconciliation=`245/3F`、transaction=`178/0`、business-flow=`206/0`、compile=`21/21`、Public Contract=`315/187/42`。只按目标 failure allowlist 解释，任何额外失败都阻断。
- **Green boundary**：RED Checker PASS 后四个测试文件 immutable；候选 Green 仍只有 `MODIFY LedgerTransactionServiceImpl.java`，无第二生产文件。
- **历史最终状态**：`LEDGER_DIGEST_RED_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.218`。Human Owner 随后授权 RED 执行；实际执行与门禁返工由 8.64 接替。

### 8.64 `W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-EXPANDED-GATE-REWORK-001`

- **执行事实**：Human Owner 授权 RED 后，仅四个冻结测试文件发生写入。focused=`56/8F/0E/0S`，八个 stable labels 一对一；compile=`21/21`、Public Contract=`315/187/42`、reconciliation=`245/3F/0E/0S`、transaction=`178/0F/0E/0S`、business-flow=`206/0F/0E/0S`。
- **原卡 Checker**：`NOT_PASS_P0_0_P1_1_P2_0`。唯一 P1 是完整 `test-ledger` 实际为 `65/5F/15E/0S`，与原卡错误冻结的 `65/5F/0E/0S` 冲突；15E 全部来自白名单外 `DefaultLedgerPostingAssemblerTests` 的 `MockitoInitializationException -> ByteBuddyAgent -> Could not self-attach`。
- **价值裁决**：`VALUE_CONFIRMED / EVIDENCE_LAYERING_ONLY`。persisted digest 的价值由 writer/read/clearing 可执行行为证明；Mockito Agent attach 不是资金语义。不能把 15E 记为 PASS，也不能让它覆盖 focused/non-assembler 已观察 RED。
- **返工门禁**：non-assembler Ledger 五类固定为 `DefaultLedgerTransactionPostingServiceImplTests,LedgerBalanceProjectionServiceImplTests,LedgerServiceImplTests,LedgerTransactionServiceFactQueryTests,LedgerTransactionServiceImplTests`；同一源码状态下五份 fresh XML 合计=`50/5F/0E/0S`，Green 前必须 fresh 执行该精确组合。assembler 单列当前=`15/0F/15E/0S` 环境 observation；环境可用后必须 `15/0F/0E/0S`，否则不得宣称完整 `test-ledger` PASS。
- **不变边界**：四个 RED 文件现已 immutable；8 个 failure、参数、labels、canonical helper、无兼容、唯一 Green 候选 `LedgerTransactionServiceImpl.java` 均不变。不授权 assembler 测试、Mockito/JVM/POM、生产、测试、DDL、API、Consumer 或 Git 写入。
- **历史最终状态**：`LEDGER_DIGEST_RED_EXPANDED_GATE_REWORK_INDEPENDENT_CHECKER_PASS / GREEN_ENTRY_CARD_DOCUMENTATION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.220`。Human Owner 随后授权 Green Entry Card 文档；当前由 8.65 接替。

### 8.65 `W5-MIG03-LEDGER-PERSISTED-DIGEST-GREEN-ENTRY-CARD-001`

- **价值**：`VALUE_CONFIRMED / LEDGER_INTERNAL_PERSISTED_INTEGRITY`。把 8 个 RED 收口为一个 Ledger-owned 不变量：写后可从 persisted facts 重建、同 key replay 只复用已验证同义事实、exact read/Clearing 在副作用前 fail-closed。
- **production whitelist**：`MODIFY=1 / ADD=0 / DELETE=0`，仅 `ledger/impl/src/main/java/com/wind/funds/ledger/impl/LedgerTransactionServiceImpl.java`。执行前 SHA=`5acbbdb67273c419095b0eb79e845bba97a8286eda638b2d0b19dd4055f7a866`。
- **write/replay**：写前秒级时间归一；在内存中物化 transaction/plan/最终生成 SN 的 entries；三层 v1 builder 后原子 insert + 回读自校验。同 key replay 先验 persisted aggregate，再按 plan SN、entry 稳定顺序绑定既有生成型身份，继续使用同一 builder，不新增第二 digest。
- **read boundary**：transaction id/sn 验完整 aggregate；entry id/sn/query 验自身与父引用；exists plan 验 plan 与父 transaction。错误包含 `transaction / posting plan / ledger entry` 与 SN；不改 Clearing。
- **8-to-1 mapping**：删除 legacy/dual fallback关闭 01；time/decimal/materialize/self-verify 关闭02；transaction/plan/entry guards关闭03~05；Ledger fail-closed 自然关闭 Clearing 06~08。
- **immutable**：四个 RED 测试、Contract、Ledger face/DTO/Entity/Mapper/converter、schema/POM/Justfile/core、Transaction/Reconciliation production、Consumer 与 API baseline。禁止新 helper 文件、API/DDL、`WindObjectDigestUtils` persisted path、兼容/回填。
- **Green gates**：focused=`56/0`、non-assembler Ledger=`50/0`、reconciliation=`245/0`、transaction=`178/0`、business=`206/0`、compile=`21/21`、Public Contract=`315/187/42`；assembler 当前环境只作 `15E` observation，不得冒充完整 ledger PASS。
- **Checker**：`NOT_PASS_P0_0_P1_1_P2_0`。单文件实现与 8-to-1 mapping 成立，但卡片要求 by-id、entry query、`existsPostingPlan` guards，而 immutable RED 只观察 transaction/entry by-SN；当前实现可漏做四个入口仍让全部 8 RED 转绿。
- **历史最终状态**：`LEDGER_DIGEST_GREEN_ENTRY_CARD_INDEPENDENT_CHECKER_NOT_PASS_P1_1 / RED_COVERAGE_REWORK_GRANT_NO / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.221`；当前由 8.66 接替。

### 8.66 `W5-MIG03-LEDGER-PERSISTED-DIGEST-EXACT-READ-RED-COVERAGE-REWORK-001`

- **价值**：同一 persisted Ledger 篡改必须在所有 exact read surface 一致 fail-closed，不能只保护 by-SN 后让 by-id/query/exists 返回未验证事实。
- **候选白名单**：仅 `MODIFY tests/src/test/java/com/wind/funds/ledger/service/LedgerTransactionServiceFactQueryTests.java`；生产、其他测试、schema/POM/API/Mapper/Consumer/Git immutable。
- **计数不变**：保留 `TRANSACTION/POSTING_PLAN/LEDGER_ENTRY` 三个 invocation 与原 stable labels，类仍 `8/3F/0E/0S`，focused 仍 `56/8F/0E/0S`。
- **聚合入口**：transaction=`byId+bySn`；plan=`transaction aggregate read+existsPostingPlan`；entry=`byId+bySn+queryLedgerEntries`。每个 invocation 收集未拒绝入口，最终只形成一个 failure，并断言 Ledger/Balance snapshot 不变。
- **不变 Green**：生产仍只候选 `LedgerTransactionServiceImpl.java`；normalized materialization、三层 v1、same-key replay 与无兼容设计不变。
- **当前状态**：`LEDGER_DIGEST_EXACT_READ_RED_COVERAGE_REWORK_INDEPENDENT_CHECKER_PASS / LEDGER_DIGEST_GREEN_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.223`。

### 8.67 `CI-W5-MIG05D-LEDGER-POSTING-COMMAND-001`

- **问题与当前事实**：`LedgerPostingAssembler` 与 `LedgerTransactionPostingService#post(LedgerTransactionSpec)` 仍是 `core` 稳定写入口；仓内唯一生产 caller 是 `DefaultRoutedFundsInstructionOrchestrator`，依次完成 route、lifecycle、assemble、post 和 success。Ledger 实现仍会校验上下文、金额、平衡、期间、摘要与幂等，因此当前没有证据证明可绕过资金不变量；待裁决的是低层 posting 组装权是否应继续作为公共承诺。
- **候选 A，推荐 `SINGLE_HIGH_LEVEL_CORE_POST_COMMAND`**：把组装与提交收敛为一个高阶 core command，输入只允许归一资金指令、非空资金交易身份与已解析 route；Ledger 边界独立复验两侧 `tenantId/businessScene/businessSn/instructionType/eventType/transactionType` 完全一致，内部唯一完成 posting spec/plan/entry 的物化与持久写入。候选阶段曾把 root `tenantId + fundsTransactionSn` 当作稳定命令身份；8.68 的真实 authorization caller closure 已将其纠正为 root + event/business action identity。`LedgerTransactionSpec`、`LedgerPostingPlanSpec`、`LedgerPostingPhaseSpec`、`LedgerEntrySpec` 继续留在 core 表达稳定记账 DSL 和不变量，但不再作为 Consumer 可提交的公共写输入。
- **候选 B，`KEEP_DUAL_LOW_LEVEL_CORE_PORTS`**：保留 assembler 与 `post(spec)` 两个稳定入口，换取 Consumer 自定义 posting grammar 的最大自由；当前没有第二生产 grammar、第二生产 assembler 或外部生产 Consumer 证明该自由具有现实价值，因此不推荐。
- **候选 C，`MOVE_LOW_LEVEL_PORTS_TO_LEDGER_FACE`**：不作为可选 fallback。它只移动包而不收回 Consumer 拼装 posting 的权力，并会引入当前明令禁止的 `transaction-impl -> ledger-face` 依赖。
- **Consumer/caller closure**：`capte-domain` 生产模块依赖 `wind-funds-core`、`wind-funds-transaction-face` 与 `wind-funds-wallet-face`，但没有生产调用这两个 posting 入口；其集中 tests 只直接装配两个 concrete impl，属于测试宿主证据。`fincone` 当前只有设计材料，不是运行时 Consumer。`FundsTransactionProjectionPublishContext` 虽携带完整 `LedgerTransactionSpec`，当前生产只读取 transaction SN，不消费 plan/entry。
- **价值与成本**：A 不新增用户可见能力；它把“归一资金指令与冻结 route 如何变成借贷分录”的唯一解释责任锁回 Ledger Owner，避免调用方选择 ledger identity、借贷方向、normal side、`allowNegative`、plan/entry/digest。PAY、authorization、refund/reversal、external in/out、balance adjustment 与 clearing 可复用同一不变量；现有本地事务、ActionFact 恢复和 ledger transaction 引用不需第二写链。成本是一处生产 caller、相关测试和投影上下文的破坏式迁移。
- **幂等缺口与返工**：当前 `CompositeRouteResolver` 已校验六字段一致性，但 assembler 与 Ledger 写链未复验；当前 assembler 先生成时序 Ledger SN，写链只按该 SN 做 replay。首轮 Checker 因此判定 `NOT PASS / 0 P0 / 1 P1 / 1 P2`。返工把六字段独立校验、非空命令身份、同 identity/digest 返回同一 persisted SN、异 digest 冲突且零副作用写入 A 与未来 RED；具体稳定 SN 派生或持久化查询/约束留待 Entry Card 证明，若需要新 schema/文件必须停止重冻。
- **证据指纹**：十三个直接证据文件按系分 11.31.3 的 1~13 仓库相对路径顺序冻结，SHA-256=`f01da0518faddc44cced8befb6c476c11d06f615a63d1bf54d29de22a828a404`，覆盖 instruction、resolved route、四个 posting specs、两个稳定端口、orchestrator、projection context、posting impl、default assembler 与 Core API baseline；执行前 HEAD=`fc6b6e004a32eb1813534de05df7f844cc6edf95`，default/`-uall` manifest=`68/7a68c0ebaba16d49ea2f684062605ed09da5bd7f273f47074a882f81e5503a5d`，staged empty，`git diff --check` PASS。
- **当前门禁**：本轮只完成五份权威文档的决策包返工，未修改或运行 Java/测试；独立 Checker 已 `PASS / 0 P0-P2`，当前只等待 Human Owner 选择 A 或 B。未选择前不得生成文件级 Entry Card、精确 RED 计数或 Java 签名。无论选择何项，PaymentInstrument、MIG-02C/03/06/08/09、schema/POM、兼容层、Consumer、Git、HOST/L4 与发布生产均不在本卡。

### 8.68 `CI-W5-MIG05D-LEDGER-POSTING-COMMAND-001-A`

- **Owner 接受**：Human Owner 回复“授权推进 A，做价值分析”，正式选择 `A / SINGLE_HIGH_LEVEL_CORE_POST_COMMAND` 并授权文档 Acceptance/Entry Card；B 未选择，C 保持 rejected。该授权不包含 RED、Green、源码、测试、Git 或发布。
- **精确合同**：`LedgerTransactionPostingService.post(FundsInstructionSpec, String fundsTransactionSn, ResolvedRouteSpec) -> String ledgerTransactionSn`。Ledger 独立复验 instruction/route 六字段；identity=`tenantId + nonblank fundsTransactionSn + eventType + businessScene + businessSn`；SN=`LE + first48(sha256CanonicalJson("ledger.posting.command.identity", identity))`；现有 persisted aggregate digest 判同 action 同义/冲突，同一 root 的不同 action 得到不同 SN。
- **最小物理边界**：删除 core `LedgerPostingAssembler`；concrete assembler 只留 ledger-impl；低层 `postAssembled(LedgerTransactionSpec)` 只允许 concrete impl package-private。Projection context 删除 raw spec component，直接复用现有 lifecycle result Ledger SN。不新增字段、表、UK、锁、缓存、幂等记录、digest、service、registry、factory 文件或第二写链。
- **执行 checkpoint**：`CONTRACT_SURFACE_RED -> CONTRACT_SURFACE_GREEN -> BEHAVIORAL_RED -> BEHAVIORAL_GREEN`。Surface RED=`Boundary 29/1F`；Surface Green focused=`61/0`；Behavioral RED 在 posting service test 新增 `7 methods / 12 invocations`，class=`30/12F`、focused=`73/12F`；Behavioral Green focused=`73/0`。
- **文件卡**：唯一闭包=`1 DELETE + 18 MODIFY / ADD=0`，精确 19 路径、阶段、职责与单文件 SHA 以系分 11.32.3 为唯一来源；ordered fingerprint=`25c12e2dae73722cc7155f458350e2ec4fa362b234d36b54a99970a88a4762a4`。包含 core API script/baseline 与 5 个测试文件；不包含 schema/POM/Justfile/api-policy/Entity/Mapper/Converter/Consumer。
- **API 结果**：当前物理 Core baseline 已为 `103/95/4/4 / 1036 lines`，Public Contract=`307/181/42`；`D-CS-006-T` 的 Surface 与 Behavioral 均已实现并通过独立 Checker。
- **价值**：PAY、authorization、refund/reversal、external in/out、adjustment 与 clearing 共享 Ledger-owned 会计翻译；调用方不能选 entry/ledger/constraint；超时、重启、并发同义返回同一 persisted SN，异义零副作用冲突；投影不再泄露完整 posting spec。该变化降低公共契约和重复入账风险，不新增用户功能。
- **动作级 identity 返工**：首轮 Entry Card Checker=`NOT PASS / 0 P0 / 1 P1 / 0 P2`。现有 lifecycle saver 会让 AUTHORIZE、多个 COMPLETE 与 REVERSAL 共用 authorization root，现有 flow 已证明同 root 四条独立 LedgerTransaction；root-only SN 会把合法后继动作误判冲突。返工不改变 A/signature/19 文件/API 目标，只把 identity 改为 root + event/business action，并新增同 root 不同 action 分离/各自重放的第 12 个 RED invocation。
- **停止线**：第 20 文件、Capte/Fincone 生产改签、同 root 不同 action 无法生成不同稳定 SN 并分别重放、schema/UK/lock/cache/幂等表、稳定 SN 长度不闭合、raw seam 被 production 调用、完整 plan/entry projection Consumer、兼容/V2/bridge/双端口/第二 digest/第二写链，任一出现即停止重冻。
- **Surface RED 执行**：唯一测试文件新增一个聚合方法；首轮 Checker=`NOT PASS / 0 P0 / 1 P1 / 1 P2` 后，以 reflection 补 concrete raw-spec visibility 门并移除过脆源码正例。最终 compile=`21/21`、Boundary=`29/1F/0E/0S`、focused=`61/1F/0E/0S`，唯一 failure 为目标方法；test SHA=`b05c78e7a0542b1302062f81481136948b12b229fadf530a1d00e5dd31f4e31d`、XML SHA=`e063473d228b6ee16a3f594bb6901c4e1976ae684b863db96266a603a1bbaf1e`、post fingerprint=`f57b9a0a5f22ad4e2e175d4a98c9dca0f7ae23bd91594de5cb34a854e7674f0b`；最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。
- **历史最终状态**：`BEHAVIORAL_GREEN_EXECUTION_COMPLETE / BEHAVIORAL_GREEN_INDEPENDENT_CHECKER_PASS / CURRENT_SCOPE_COMPLETE / ENTRY_CARD_REQUIRED / EXECUTION_GRANT_NO / CODE_FREEZE / plan-r2.254`。

## 9. 业务模拟验证集

每个场景先做产品事实模拟，再做 DSL 映射、系分责任和 TDD 断言；不得从现有 API 反推业务。

| ID | 场景 | 当前证据 | 必须验证的稳定语义 | 当前状态 |
| --- | --- | --- | --- | --- |
| `SIM-01` | 商品钱包支付、完成、撤销与原路退款 | `capte-domain` 当前源码，`E2` | payer/payee、Money、业务事实、授权累计、原事实引用、幂等、账本/余额 | `PRODUCT_POLICY_ACCEPTED / HOST_E4_BLOCKED` |
| `SIM-02` | 券 100 + 钱包 900 组合支付 | `capte-domain` 当前源码，`E2` | 宿主组合编排与资金事实分离；跨事务失败恢复 | `PRODUCT_POLICY_ACCEPTED / HOST_BLOCKED` |
| `SIM-03` | 平台 60% + 商户 40% 成本责任及退款 | `capte-domain` 当前源码和 API 漂移，`E2` | A/B/C 所选责任效果、每项独立事实、原引用、配置漂移、舍入 | `PRODUCT_POLICY_ACCEPTED / HOST_E4_BLOCKED` |
| `SIM-04` | VCC 授权 100、两个完成输入 30/50、释放、退款 20 | `wind-funds` 测试 `E2`；`fincone` 设计 `E1` | 信用/资金责任、规范化增量、乱序、拒绝零副作用、原 route | `P-SIM04-02-D_ACCEPTANCE_CHECKER_PASS` |
| `SIM-05` | 全球账户 confirmed credit、重复回执、NOC、return/reversal | `fincone` 设计 `E1`；`wind-funds` confirmed-credit 流程测试源码 `E2` | 外部完成层级、confirmed/finality 分开、NOC 零资金、逆向证据 | `P-SIM05-01-A_ACCEPTANCE_CHECKER_PASS` |
| `SIM-06` | 收单 capture -> clearing -> settlement -> payout -> refund | Fincone 收单准入卡 `E1 / BLOCKED`；`wind-funds` 内部组合测试源码 `E2` | capture authority/资金准入、累计、完成分层、Gate、payout 非终态、退款责任 | `P-SIM06-03-B_ACCEPTANCE_CHECKER_PASS` |
| `SIM-07` | Capte 账单/退款、资金交易/账本与外部事实三方对账 | Fincone 清结算设计 `E1/PENDING`；Provider current source `E2` | 冻结 source/scope/window/rule/coverage、1:1 strict exact、上游聚合、append-only difference 与 current lineage | `P-SIM07-01-A_ACCEPTANCE_CHECKER_PASS` |

验证按资金效果分型：`FUNDS_EFFECT` 场景至少断言相关主体余额桶变化、posting plan 借贷平衡、ledger transaction/entry 可追溯、原引用和幂等重放；`NO_FUNDS_EFFECT / READ_ONLY` 场景断言零 route/posting/entry/balance 变化。失败场景断言对应层级零副作用，且 Unknown 不得通过换键补单绕过。

## 10. Public API 处置规则

每个 `core` 和 face public 类型必须有一张处置卡：

```text
API / 类型：
当前模块与可见性：
业务职责与 Owner：
稳定不变量：
真实生产消费者：
测试消费者：
输入前提 / 输出 / 副作用：
幂等 / 冲突 / 失败 / 恢复：
为何必须 public：
场景变化轴：
破坏式变更与 Consumer 同步切换影响：
处置：保留 / 移动 / 内收 / 替换 / 退役 / 待确认
证据等级与路径：
```

处置默认遵循最短路径：

1. 没有业务职责或真实消费者：优先不保留。
2. 已在本仓其他位置有等价类型/能力：复用。
3. JDK 或既有依赖能表达：不新增自定义抽象。
4. 只有单模块实现/消费的 SPI：优先内收，不放 Core。
5. 场景差异用宿主映射或有证据的规则/适配边界承载，不创建万能字段。
6. 只有跨场景稳定、跨模块必要且可验证的语义才进入 Core/public face。

## 10A. W1-02 / Q-003 完成层级决策包

本节记录已接受的 `Q-003`：公共能力如何表达分层完成、失败与未知。它不批准目标态状态枚举、状态机、DTO、API、posting matrix 或外部轨道终态映射。

### 10A.1 当前证据支持的分层

每个事实 Owner 所回答的独立问题构成一个证据维度，这些维度不是全局单调阶梯。业务、外部、资金交易、账本、余额和对账/Gate 只是基础示例；清分计算、清算资金、结算锁定、出款提交/受理和出款资金结果等场景问题保持独立。VCC、ACH、收单和内部钱包等场景分别声明必要维度、局部偏序和最终展示门槛；不适用的维度无需为满足统一模型而伪造状态或证据。

| 待回答的问题 | 当前事实 Owner / 最小证据 | 能证明 | 不能证明 |
| --- | --- | --- | --- |
| 宿主业务是否接受 | 订单、券、VCC、全球账户或收单业务 Owner；业务单据、状态和稳定引用 | 宿主已接受或完成本层业务动作 | wind-funds 已记账、余额已变化或外部到账 |
| 外部网络是否受理/处理中 | issuer、银行、ACH、PSP 的适配 Owner；已验证 external reference、receipt、来源摘要和时点 | 外部 `ACCEPTED/PROCESSING` | 内部记账、余额可用、终局到账 |
| 外部资金结果是否终局 | 对应轨道的权威 Owner；已核验终态映射、金额币种和原始证据 | 指定轨道和版本下的 confirmed/succeeded/finality | wind-funds 已成功消费、内部余额已闭合或对账完成 |
| 资金命令结果是否明确 | transaction Owner；资金交易、明细、幂等摘要、原事实和 route snapshot | 本次内部资金意图已成功、拒绝、失败或仍未知；route snapshot 只证明本次路由决策证据已记录，是否存在可执行路径必须继续核验 legs 与可回放标记 | route legs 已执行、账务已形成、外部到账、清算/结算或对账完成；仅有 transactionSn 或解释快照也不自动证明后续各层 |
| 内部账务是否完成 | ledger Owner；平衡 posting plan、LedgerTransaction、LedgerEntry 及原交易关联 | 内部不可变账务事实已形成 | 指定余额已经可用、外部实体资金已转移 |
| 内部余额是否反映 | ledger projection Owner；指定账户、币种、账目/余额维度和时点的已提交投影 | 对应内部余额已按账务事实变化 | 外部到账、网络结算或业务履约完成 |
| 内部清分结果是否确认 | clearing calculation Owner；冻结来源、规则版本、守恒结果和确认快照 | 应收应付/分配结果已冻结 | 清算入账或资金已经可用 |
| 内部清算资金是否完成 | clearing funds Owner；Gate、清算资金交易、账务和余额证据 | 指定清算资金影响完成 | 结算锁定、出款或外部到账 |
| 结算资金是否锁定 | settlement Owner；审批、当前 BALANCED Gate、SettlementOrder 与锁定资金事实 | 结算责任和资金已被锁定 | 已提交通道或收款方到账 |
| 出款是否提交/受理 | payout/宿主准入/通道 Owner；Gate、审批、endpoint、channel、SUBMITTED/ACCEPTED 证据 | 已提交或外部受理 | 终局成功、beneficiary finality 或出款后对账完成 |
| 出款资金结果是否完成 | rail Owner + payout/transaction Owner；权威终态回单、receipt lineage、对应内部资金动作 | 外部终态已被内部一致消费 | 若轨道契约未说明 beneficiary finality，不能承诺收款方最终入账 |
| 对账运行是否完成且对平 | reconciliation Owner；冻结双侧来源、规则版本、不可变结果摘要和 `BALANCED` | 指定范围、时点和规则下对平，可供当前 Gate 复核 | 清分、结算或出款动作已经执行；Batch `COMPLETED` 也不自动等于 `BALANCED` |

### 10A.2 当前直接反例

1. `capte-domain` 的 `PAID/SUCCESS` 当前只证明 Consumer 保存了业务状态和部分资金流水引用；未查询 posting、ledger transaction 或交易后余额。
2. Capte 钱包调用把 transport/runtime exception 直接降为失败，不能表达 Provider 可能已提交而 Consumer 未收到结果的 `UNKNOWN`；Benefit 又以 `FundsTransactionDTO` 存在近似判定完成。
3. 组合支付的同步补偿没有持久化补偿意图，钱包 reversal 返回的新资金流水也被 Consumer 丢弃，不能证明失败链已经耐久闭合。
4. VCC 事件可以在授权拒绝且零账务时仍被宿主标记为已应用；VCC 内部 `SETTLEMENT` 余额维度不等于卡网络 Settlement。
5. ACH/银行入金 `ACCEPTED/PROCESSING` 被当前 Provider 明确拒绝入账，只有已确认 credit 候选才形成内部资金与余额事实。
6. 收单 capture 记账后资金仍可停在 `CLEARING`；清分确认只冻结计算结果，内部清算确认后才可能进入 `AVAILABLE`。
7. SettlementOrder `LOCKED` 只证明内部结算资金已锁定；Payout `SUBMITTED/ACCEPTED/PROCESSING` 均不证明收款方到账。
8. Payout 权威成功回单若无法形成对应内部账务，当前实现进入 `MISMATCHED`，不能保留表面成功；成功后的迟到冲突也不覆盖原资金事实。
9. ReconciliationBatch `COMPLETED` 只表示运行结果已固化，可以对应 `DIFFERENCE_FOUND`；只有当前血缘、来源完整且结果为 `BALANCED` 才能成为后续 Gate 的候选证据。
10. `inspectGate` 与 payout preflight 都是只读时点解释，不是可缓存、可转交的资金授权；最终资金动作必须在自己的写入边界权威复核。
11. transaction 的确定性 posting reject 可以形成持久 `FAILED` 且零 ledger facts；payout `FAILED` 当前却要求外部失败后返还资金成功。同名状态不能跨 Owner 推导相同资金效果。
12. payout `RETURNED` 当前只证明外部退回状态被登记，尚未证明已关联返还或冲正资金事实；在专项模拟前保持 `PENDING`。
13. 授权拒绝可以保存 `legs=0`、不可回放的 route explanation snapshot；快照存在不等于存在可执行路径，更不等于已经路由或入账。

### 10A.3 已接受产品规则与停止线

Owner 已接受以下 `Q-003` 产品规则：

- 不设计一个跨业务、外部、交易、账务、余额、清结算和对账的公共总 `SUCCESS`。
- 每个事实 Owner 所回答的独立问题构成正交证据维度，不形成一套全局单调完成度枚举；业务、外部、资金交易、账本、余额、对账/Gate 只是基础示例，场景特有的清分、清算、结算和出款问题不得被压回这些示例。场景声明所需维度、局部偏序和最终展示门槛。
- 每个维度分别表达是否适用、证据是否已知与是否终局、领域结果、资金效果、证据引用和不能外推的维度；这些是不同属性，不在本题合并成一个状态轴或批准具体枚举。
- `failed/rejected/completed` 必须附带本层资金效果；不得跨模块复用同名状态推导零账务、已返还或外部终局。
- 命令返回必须说明其原子保证覆盖到资金交易、账务和余额中的哪一层；调用方不得仅凭非空流水号自行外推。
- 提交后结果未知必须沿原幂等身份和权威查询恢复，不得直接改键重试或当作明确失败。
- 对账前置 Gate 和资金动作后的外部资金对账是两份不同证据，不得用前者替代后者。

决策停止线：Owner 只需裁决“是否拒绝跨 Owner 总 `SUCCESS` 和全局完成度枚举，改用每个事实 Owner 独立回答问题的证据维度与场景局部偏序，并分别表达适用性、证据确定性/终局性、领域结果、资金效果和不可外推范围”。真实 issuer/银行/ACH/PSP 状态权威、beneficiary finality、宿主事务拓扑和跨仓 artifact 谱系未证明前，不把当前单仓测试描述为外部完成或生产可用，也不随 `Q-003` 默认批准任何目标枚举或接口。

### 10A.4 后续 TDD 种子

- `CT-LVL-CAPTE`：业务 `PAID`、资金交易成功、账务完成和余额反映逐层断言；Provider 提交后 Consumer 超时进入 `UNKNOWN` 并沿原身份恢复。
- `CT-LVL-VCC`：issuer event 从接收、权威解释、资金处理到宿主 applied 逐层断言；拒绝 applied 零资金，内部 settlement 不冒充 network settlement。
- `CT-LVL-GAC`：accepted/processing 零资金，confirmed credit 唯一入账，return/reversal 引用原事实，NOC 零资金。
- `CT-LVL-ACQ`：capture、清分确认、清算入账、结算锁定、出款提交、终局回单和出款后对账逐层证明。
- `CT-LVL-RECON`：`COMPLETED + DIFFERENCE_FOUND` 不准出；当前 `BALANCED` 只形成 Gate 证据；差错需要处置引用和后继 balanced rerun 才能关闭。

## 10B. Q-002 对象边界决策包

本节只收敛 `Q-002` 的产品对象资格与关系，不批准 Java 类型、账户分类、posting matrix、状态机或 DSL。三项目证据共同支持分层，但要求把旧措辞中的“内部资金账户”收紧为“内部可记账账户”，以明确覆盖资金余额账户和信用/额度责任账户，且不把二者都解释为现金。

### 10B.1 最小关系候选

```text
资金责任方
  --[tenant + currency + 业务场景/账户模式 + 责任规则版本]-->
一个或多个内部可记账账户
  --[ledger profile/version + 账目/余额维度 + period]-->
只读余额投影

业务对象 / 交易角色 / 支付工具 / 外部账户或轨道
  --[识别、准入、选路或举证]--> 本次解析与资金事实

业务事实引用 / 原资金事实引用 / 账本事实引用 / 外部证据引用
  --[关联和追溯]--> 对应事实；引用自身不持有余额
```

这里的箭头表示本次交易上下文中的受控解析和事实关联，不表示永久一对一所有权树。当前 Provider 事实只证明 `FundingAccount` 与 `CreditAccount` 都可以成为内部 posting identity：前者承载资金余额，后者承载额度或责任控制余额；两者的最终上位名、继承关系和具体分类仍待后续设计。

### 10B.2 跨项目业务映射

| 场景 | 业务对象 / 角色 / 工具或外部端点 | 解析后的内部可记账账户 | 本题只确认的边界 | 后续专项 |
| --- | --- | --- | --- | --- |
| Capte 钱包支付 900 与退款 | order、bill、user、业务 `payerId/payeeId`、walletCode | 用户 FundingAccount；收款侧必须解析为真实内部账户，字符串 `capte` 不是账户 | walletCode 必须经 tenant/owner/account 准入；退款沿原资金事实 | 真实收款责任账户、钱包 owner binding、具体 posting |
| Benefit 平台 60 / 商户 40 与退款 | coupon、redemption、PLATFORM/MERCHANT 责任角色 | 每笔独立的 cost bearer account 与 receiver account | 角色、券和比例不是账户；receiver 账目与原 transactionSn 必须显式 | receiver 是谁、`CLEARING/SETTLEMENT`、原流水持久化 |
| 券 100 + 钱包 900 | couponId 与 walletCode 共用旧字段名 `payerId` | 只有钱包 900 解析到内部账户；普通券不持有余额 | 同名业务字段不得被统一转成 FundsAccountId | SPECIFIED Benefit 与主支付事实关联、跨事务恢复 |
| VCC PREPAID / SHARED | Card、PaymentInstrument、authorization holder | PREPAID 绑定 FundingAccount；SHARED 同时涉及 CreditAccount 与父 FundingAccount | 工具、工具 owner、绑定账户和真实资金责任账户分离；关系按原快照固化 | `OUTSTANDING/SETTLEMENT` posting matrix、共享责任规则 |
| GlobalAccount / ACH confirmed credit | GlobalAccount 产品视图、AchAccount、bank/rail evidence | 客户或商户 FundingAccount + 平台 CASH 映射账户 | 外部账户无内部 bucket；confirmed 外部事实只触发内部账户变化 | authoritative finality、return/NOC、账户与币种身份 |
| 收单 capture 到 payout | merchant role、PSP/acquirer、endpoint、receipt | payer FundingAccount、merchant FundingAccount、平台映射账户 | merchant role 先解析到账户；endpoint/channel/receipt 只作证据 | capture/clearing/settlement/payout posting 与完成层级 |

### 10B.3 硬负例

以下任一方案均与当前跨项目证据冲突，不应由后续 DSL 恢复：

1. 资金责任方、payer/payee/merchant 角色或业务对象直接成为 ledger posting subject。
2. Card、PaymentInstrument、券、GlobalAccount 产品对象、AchAccount、外部银行账户、PSP endpoint、rail/channel token 直接持有内部余额或成为 LedgerEntry subject。
3. 把 `FundingAccount` 视为唯一可入账账户，排除 `CreditAccount`；或把 Credit 的 `AVAILABLE` 解释为现金。
4. 把开放字符串 `FundsAccountId`、业务 `payerId/payeeId` 或当前 `DefaultFundsAccountType` 枚举值视为已通过 tenant/owner/currency/account 准入。
5. 把责任解析当作资金转移，或把本次交易角色固化为账户永久类型。
6. 把 LedgerProfile、账目/余额维度、BalanceView 或任何事实引用当作账户身份、责任方或可写余额。
7. 逆向按当前工具绑定、责任规则、活动配置或收款配置重新解析，而不是沿原账户、route 和资金事实。

### 10B.4 本题不裁决

- “内部可记账账户”的最终产品名，以及 Funding/Credit 是子型还是两类并列账户。
- tenant、currency、legal entity、book、region 是否属于账户稳定身份；单币种账户或账户下多币种账簇。
- 责任关系的基数、优先级、有效期、fallback、账户层级、共享资金责任和规则版本模型。
- 具体 account type、Ledger Profile、账目用途、余额状态和 period 如何拆轴，以及各场景 posting matrix。
- 工具多绑定、授权持有人、Spend Rule、FX、fee、准备金、负余额和差错调账规则。
- issuer、ACH、PSP 的 authoritative finality，外部账户/证据的隐私边界和法律、合同、会计责任。

决策停止线：Owner 只需裁决“对象是否分层、谁有内部可入账资格、解析结果是否冻结”这一项；任何具体账户分类、余额桶或业务状态答案都不随 `Q-002` 被默认接受。

## 10C. W1-02 三问后范围审计

`Q-001` 至 `Q-003` 已达到本 Wave 首轮三个 Owner 决策上限。本节只重新确认剩余范围，不把候选 `Q-004` 偷写成已确认产品规则。

### 10C.1 已可由前三项决议推出

- 历史资金事实不可覆盖；逆向和修复只能追加关联的新事实。
- 提交后证据未知必须沿原幂等身份和权威查询恢复，不得换身份补单。
- 同一身份下出现会改变金额、币种、责任账户、原事实或预期资金效果的语义差异时必须停止，且不产生新的资金影响。

### 10C.2 仍存在的独立产品取舍

当前跨项目审计识别出一个最小阻断题：稳定业务意图如何约束重放、确定失败后的再次尝试，以及哪些经济语义必须随意图保持不变。前三项决议没有回答以下两点：

1. 一个已明确失败的意图，后续执行是原意图下的关联 attempt，还是一个新的经济意图；两者的审计、授权和资金副作用边界不同。
2. 业务发生时间、原因/授权、责任账户、Benefit 分摊快照等哪些信息属于不可变经济语义；若不先定产品原则，DSL 无法区分合法重放与同键异义。

推荐重新确认 Wave 1 范围：只追加一个 `Q-004 / 业务意图身份与重放契约`，只裁决“确定失败后的再次执行属于原意图下的关联 attempt 还是新经济意图”和“不可变经济语义的产品类别边界”；不重新打开已确认的冲突停止、`UNKNOWN` 原身份恢复和原事实逆向规则，也不定义 key 拼接、摘要字段表、hash、状态枚举、API、存储、事务、Saga、重试次数或 rail 资金矩阵。`Q-004` 后不再发起新的公共抽象问题，直接闭合场景合同并进入 `G1`。

### 10C.3 场景范围与延后项

- `P0`：`SIM-01` 商品钱包支付/退款、`SIM-02` 券+钱包组合支付、`SIM-03` Benefit 多方成本分配、资金 contribution 与退款；它们有当前 Consumer 源码，且已暴露收款落点、部分成功恢复、契约漂移和原事实缺失。
- `P0` 场景 Owner/阻断：Capte Order Owner 必须提供可验证收款责任和账户前置；Coupon/Benefit Owner 必须提供成本承担、Benefit 接收责任、分摊/舍入快照和原 contribution 引用；Host Integration Owner 必须说明组合腿部分成功后的耐久证据。任一前置事实缺失时，目标产品合同以拒绝或人工停止闭合，不替宿主猜账户、比例或补偿成功。
- `P1`：`SIM-04` VCC、`SIM-05` ACH/GlobalAccount、`SIM-06` 收单 payout、`SIM-07` 三方对账；它们用于验证公共语义。Issuer/rail/adapter Owner 负责外部权威证据，Reconciliation Owner 负责 source/date/scope/rule、difference 关闭和人工处置权。
- `P1` 的 `G1` 闭合口径是确认产品合同、证据维度、停止线和人工路径，不宣称宿主已接入或外部已终局。缺外部 Owner、来源或版本时，自动资金动作和终局展示保持 `PENDING`，默认零资金影响或人工停止；真实 adapter、外部终局和生产谱系延后到 `E4/E5`。
- 可延后：具体 key/digest/字段序列/算法、DTO/API/枚举、索引/锁/事务/恢复机制、账户最终分类和 posting matrix。
- 外部 Owner：issuer、ACH、PSP、银行与财务/法务对 authoritative finality、beneficiary finality、`RETURNED`、争议和会计责任的裁决。

范围审计历史结论：Owner 当时重新确认只追加 `Q-004`；该 Owner Gate 后续已由方案 A 的接受关闭。产品 `G1` 仍须逐项关闭场景 Owner PENDING，不能由 Q-004 单独推出。

## 10D. W1-02 / Q-004 业务意图身份与重放决策包

本节只收敛稳定经济意图、执行尝试和不可变经济语义的产品关系。它不设计 key、digest/hash、字段表、状态枚举、DTO/API、存储、事务、Saga、重试次数或 rail matrix。

### 10D.1 必须分开的产品对象

| 对象 | 回答的问题 | 不是什么 |
| --- | --- | --- |
| 经济意图 `Intent` | 业务 Owner 在稳定责任、价值、因果和授权边界内，希望形成哪一个目标经济效果 | 一次 HTTP/MQ 调用、一次回执或一次执行进程 |
| 执行尝试 `Attempt` | 为实现同一经济意图发生的某次受控执行及其局部结果 | 新的经济授权、全局成功状态或原事实的覆盖版本 |
| 投递 `Delivery` | 请求、消息、Webhook、文件或回调被传输了一次 | 权威事件、执行尝试或资金效果 |
| 事件 `Event` | 宿主或外部 Owner 声明发生了什么领域事实 | 传输次数、内部资金结果或终局证明 |
| 证据 `Evidence` | 哪些材料支持某一层 claim，以及证据是否已知、终局或冲突 | 经济意图、执行尝试或自动资金动作 |

NOC、拒绝解释、只读查询和只读对账可以只有 Event/Evidence，而没有资金 Intent/Attempt；不得为统一模型伪造账户或资金事实。

### 10D.2 两个备选及推荐

| 方案 | 规则 | 收益 | 代价/风险 |
| --- | --- | --- | --- |
| A：稳定 Intent + 受控多 Attempt（推荐） | 经济语义不变时 Intent 保持稳定；只有权威证明旧 Attempt 已确定终止且不再可能产生目标效果后，才允许关联新 Attempt | 同时支持失败恢复、审计和防双付；不会把技术重试伪装成新授权 | 场景必须提供 attempt 准入证据和局部资金效果 |
| B：一个 Intent 只能有一个 Attempt | 任何再次执行都创建新 Intent | 模型表面简单 | 把同一经济目的拆成多个授权，难以限制重复经济效果和解释恢复 |
| C：所有重试复用同一 Attempt | Delivery 重投、确定失败后重驱都覆盖同一 Attempt | 对调用方字段最少 | 覆盖失败历史，无法表达重新授权、迟到冲突和每次执行责任 |

推荐方案 A，并增加一条并发红线：任一时刻，同一 Intent 最多只能有一个仍可能产生目标经济效果的 Attempt。

Owner 已接受方案 A。该接受只冻结本节产品语义与红线，不批准具体 key、字段、API、存储、事务、自动重试或 rail matrix。

### 10D.3 新 Attempt 的准入与停止线

只有同时满足以下产品证据，才允许在同一 Intent 下创建关联的新 Attempt：

1. 对应事实 Owner 已权威证明旧 Attempt 的局部领域结果为确定性 `FAILED` 或 `REJECTED`，且不会迟到形成目标经济效果；这不批准新的合成状态枚举。
2. 另有权威证据证明目标资金效果为零，或原 Attempt 的占用、差异与外部/内部责任已经完成经济闭合。人工工单关闭本身不等于经济效果已闭合。
3. 业务、风险和必要专业 Owner 仍授权同一经济意图继续执行。
4. 不可变经济语义没有变化，新 Attempt 保留 predecessor 和失败证据关系。

`UNKNOWN`、仍在途、证据冲突、查询不可用，以及“外部已成功但内部尚未闭合”均禁止新 Attempt；只能查询、同语义重放、补充证据、对账或人工接管。自动查询耗尽不会把 `UNKNOWN` 变成权威失败。

若原 Attempt 已形成部分目标经济效果，则禁止重跑完整 Intent。只有意图本身可分，且权威事实已冻结完成部分、未完成部分、剩余上限与原决策快照时，后续 Attempt 才能只处理未完成部分；否则只能人工停止，或发起引用既有事实的逆向/经济修复新 Intent。

### 10D.4 不可变经济语义类别

同一 Intent 内至少下列产品类别不得漂移：

- 作用域与责任：tenant、业务 Owner、资金责任方/受益方及已冻结的责任解析。
- 经济动作与业务目的：授权占用、完成扣划、出资、出款、退款、释放、调整或经济修复等目标效果。
- 价值：金额、币种，以及会改变实际价值的换算、分摊和舍入依据。
- 因果关系：原业务事实、原资金事实、原授权/完成/出资及累计可逆范围。
- 预期资金效果与完成声明：本意图要形成或明确不形成什么资金影响，什么证据才允许声明本层完成。
- 业务决策快照：会改变责任、金额、准入或结果的规则/版本、授权与经济生效时点。

trace、投递次数、查询时点、技术请求时间、日志或展示文案等非经济信息可以随 Attempt/Delivery 变化；若某时间或外部端点会改变账期、汇率、受益方或责任，它就属于经济语义而不是技术元数据。具体字段归 DSL/系分逐场景定义。

### 10D.5 业务模拟与 hard negatives

1. 钱包授权/完成/退款：超时保持 `UNKNOWN` 并沿原意图查询；只有确定性失败/拒绝、目标资金效果为零、不再可能迟到生效和重新授权等证据全部齐全才允许新 Attempt。退款和 reversal 分别是引用原完成/授权事实的新逆向 Intent，不是原支付 Attempt。
2. 券 100 + 钱包 900：父支付目的下的独立资金组件分别保留事实；钱包已成功、券失败时不得重跑整个组合，只处理失败组件或创建引用成功组件的逆向 Intent。
3. Benefit 60/40：平台 60、商户 40 是两项稳定成本责任，不预设两者都形成资金 Intent。只有已签收为资金 contribution 的责任项才适用 Attempt 准入和原资金事实逆向；非资金折让沿原业务决策恢复。活动变为 70/30 也不得按当前配置重算。
4. VCC/ACH/payout：重复 delivery 不创建新 Event/Attempt；外部成功而内部失败时禁止重发外部动作；迟到相反终态追加冲突证据，不覆盖旧事实。
5. 对账/NOC：新证据产生新 run 或差异血缘，不重发原资金 Intent；NOC 和只读 Gate 保持零资金影响。

以下任一行为均为硬失败：`UNKNOWN` 换身份补单；同一 Intent 并行两个可能成功的 Attempt；仅凭状态名 `FAILED` 重试；同 Intent 改金额、币种、责任、动作、原事实或预期效果；把 delivery/receipt 当 Intent；把当前配置当历史快照；把逆向或经济修复当原 Intent 重试；存在部分效果却重跑完整 Intent；丢弃已成功组件事实；把人工关闭或查询成功当经济完成。

术语停止线：查询、证据补全、同语义重放和继续已冻结的未完成组件属于原 Intent/Attempt 的技术或证据恢复；只有会改变、补正或逆转经济效果的经济修复，才是引用原事实的新 Intent。

### 10D.6 本题不裁决

- 具体 intent/attempt/delivery/event/evidence 的 Java 名称、字段、标识拼接和序列化。
- 摘要字段白名单、canonicalization、hash/version、唯一索引和冲突错误码。
- attempt 状态枚举、自动重试政策、次数、时限、退避、并发锁和恢复任务。
- 本地/分布式事务、Outbox/Inbox、Saga、消息和宿主进程拓扑。
- issuer、ACH、PSP 的 finality、return/chargeback/RETURNED 资金矩阵和自动执行权限。

## 11. Decision Register

### Q-001

- 问题 ID：`Q-001`
- 决策主题：`wind-funds` 产品定位与能力所有权范围。
- 待裁决命题 / 命题类型：公共能力层是否只拥有可嵌入宿主的内部资金事实与能力，而不拥有订单、优惠、VCC、支付通道等产品生命周期；类型为产品价值与公共契约。
- 父主题：Wave 1 / 公共能力层产品定义。
- 问题：是否接受下述推荐产品定义，作为新产品设计的第一条已确认边界。
- 为何现在问：它决定后续主体、对象、DSL Core 和四个模块的全部边界。
- 已查证据：用户本轮目标；`wind-funds` 当前模块与测试；`capte-domain` 当前真实调用；`fincone` docs-first 边界。
- 证据冲突 / 置信边界：当前旧文档和代码含大量场景对象，不能证明这些对象应留在公共层；产品所有权需要 Owner 明确。
- 裁决动作：`ask-owner`
- 问题保真度 / 求证媒介：低保真；源码、测试、设计和业务场景足以支持文字裁决。
- 推荐答案：接受。`wind-funds` 是可嵌入宿主的资金领域公共能力库：输入由宿主确认的业务意图和外部资金事实，输出账户、资金交易、路由证据、账本、余额、清结算与对账等可幂等、可追溯的内部资金事实和查询证据；资金影响只能通过关联的新事实冲正或补正，已记录的交易、账本分录和对账结果不可原地改写。它不拥有订单、优惠券、VCC、支付通道及外部网络的产品生命周期，也不替宿主做营销、履约、风控、合规或协议裁决。
- 用户回答：接受。
- 最终结论：`accepted`。采用上述推荐产品定义，作为产品设计、DSL、系分和 TDD 的第一条目标态边界。
- red_lines：旧设计仅参考；目标必须可用、易用、抽象好理解、层次清晰；先产品/DSL/系分/TDD，后接口实现。
- 影响范围：四份目标文档、Core 候选、四个模块 public API 和跨仓接入。
- 下一阶段输入：进入 `W1-01` 产品定义正文；后续主体、账户和事实边界必须服从本结论。
- 写回位置 / 更新时间：本文件，`2026-08-12 Asia/Shanghai`。

### Q-002

- 问题 ID：`Q-002`
- 决策主题：资金责任、交易角色、内部可记账账户与账目/余额维度的产品对象边界。
- 待裁决命题 / 命题类型：是否确认资金责任方、交易角色、内部可记账账户、账目/余额维度、支付工具和事实引用必须分层，且只有经过受控解析的内部可记账账户可以承载内部余额投影并成为内部账本记账主体；类型为产品名相与公共契约。
- 父主题：Wave 1 / 公共能力层产品定义。
- 问题：是否接受下述推荐对象边界，作为后续场景设计和 DSL 的共同前提。
- 为何现在问：当前实现中的 `Subject`、payer/payee、Account Owner、具体账户类型、Ledger Profile 和 `LedgerSubjectCode` 存在混名或压轴，若不先正名，DSL 会继承实现歧义。
- 已查证据：Provider 当前账户、路由、posting、账本投影和支付工具绑定源码；`capte-domain` 钱包支付、Benefit 60/40 成本分配、资金 contribution 与退款原事实；`fincone` VCC、全球账户和收单场景材料。
- 业务模拟结论：商品钱包、Benefit、VCC、confirmed credit 和收单结算均支持“业务对象/角色先解析，内部可记账账户才入账”；SHARED VCC 证明一次交易可同时涉及 Credit 额度责任账户和父 Funding 真实资金责任账户；NOC、拒绝和只读对账不产生内部资金影响。
- 证据冲突 / 置信边界：当前 `FundsSubjectType` 只允许 Funding/Credit Account 入账，但开放 `FundsAccountId.type`、混合 `DefaultFundsAccountType`、`LedgerSubjectCode`、单/多币种口径和责任层级仍有冲突；Fincone SHARED 的 `SETTLEMENT` 文案与当前 Provider `OUTSTANDING + parent Funding settlement` 测试契约也不一致。这些现状与 posting 取舍均不在本题中被批准。
- 裁决动作：`ask-owner`
- 问题保真度 / 求证媒介：中保真；三项目源码/测试/设计静态模拟足以确认产品对象分层，不足以确认目标 Java 类型和账务维度拆法。
- 决策包 ID / 交接原因：`DP-Q002-OBJECT-BOUNDARY`；三项目独立事实源需先汇合为同一产品命题，再交人类 Owner 裁决。
- 推荐答案：接受收紧后的命题。资金责任方回答“谁拥有资金或承担资金、信用、应收、应付、成本或负债责任”，不直接成为 posting subject；交易角色只表达一次交易中的 payer、payee、authorization holder 等上下文作用；内部可记账账户是唯一可承载内部余额投影并作为内部账本记账主体的对象，当前证据要求同时容纳 Funding 资金余额账户和 Credit 额度/责任控制账户，但不把 Credit 余额解释为现金。当业务对象、角色、支付工具或外部端点将产生内部资金/账务影响时，必须按 tenant、currency、业务场景/账户模式与责任规则解析到一个或多个内部可记账账户，并把规则版本、账户关系和原 route 结果冻结在资金事实中；这不是永久一对一所有权树。NOC、拒绝和纯只读证据不得为满足模型而解析或创建账户。订单、券、Card、Payment Instrument、控制范围、GlobalAccount 产品对象、外部账户/轨道和各类事实引用不得直接持有内部余额或成为账本主体；逆向沿原事实，不按当前配置重算。
- 本题明确不裁决：Funding/Credit 的最终上位名与分类；tenant/currency/legal entity/book 等账户身份；责任关系基数与规则；具体 account type；Ledger Profile、账目用途、余额状态、period 和 posting matrix；工具多绑定、外部 finality 以及法律、合同和会计责任。
- 用户回答：接受 `Q-002`。
- 最终结论：`accepted`。确认资金责任方、交易角色、内部可记账账户、账目/余额维度、支付工具和四类事实引用必须分层；只有产生内部资金/账务影响的输入才必须经受控解析落到内部可记账账户，并冻结规则版本、账户关系和原 route。Funding/Credit 的最终分类、账目/余额拆轴和 posting matrix 仍未裁决。
- red_lines：不得把业务对象、角色、支付工具、外部账户或事实引用直接作为内部余额与账本主体；不得把 Credit 解释为现金；NOC、拒绝和纯只读证据不得为满足模型而解析或创建账户；逆向不得按当前配置重算。
- 影响范围：产品核心名相、P0/P1 场景、DSL Core、route/posting/ledger/balance 契约和 public API 处置。
- 下一阶段输入：进入 `W1-02`，逐场景确认完成层级、正逆异常和验收；产品 Wave 1 未通过前不进入 DSL。
- 重开原因：无；在同一问题内依据跨项目证据收紧后由 Owner 接受，未重开。
- 写回位置 / 更新时间：本文件与产品正文，`2026-08-12 Asia/Shanghai`。

### Q-003

- 问题 ID：`Q-003`
- 决策主题：跨业务、外部、资金交易、账务、余额、清结算与对账的完成、失败和未知语义。
- 待裁决命题 / 命题类型：是否拒绝一个跨 Owner 的公共总 `SUCCESS` 和全局单调完成度枚举，把每个事实 Owner 所回答的独立问题作为正交证据维度，由场景声明所需维度、局部偏序与最终展示门槛，并要求每个维度分别表达适用性、证据确定性/终局性、领域结果、资金效果、证据引用和不可外推范围；类型为产品完成语义与公共契约。
- 父主题：Wave 1 / `W1-02` 产品对象、流程、规则与验收摘要。
- 问题：是否接受下述分层完成语义，作为场景状态、查询证据和后续 DSL 的共同前提。
- 为何现在问：当前 Consumer 的 `PAID/SUCCESS`、transaction `completed/FAILED/REJECTED`、route snapshot、ledger、余额、payout 和 reconciliation 状态跨 Owner 同名异义；若不先分层，公共接口会让调用方从单一流水号或状态误推账务、到账和对账完成。
- 已查证据：Provider transaction/route/ledger/balance/clearing/settlement/payout/reconciliation 当前源码与测试；`capte-domain` 钱包、组合支付和 Benefit Consumer；`fincone` VCC、全球账户/ACH 和收单设计。
- 业务模拟结论：Capte 业务 `PAID` 不证明 ledger/余额；VCC 拒绝事件可 applied 且零资金；ACH `ACCEPTED` 零资金而 confirmed credit 才入账；capture 后商户资金仍在 `CLEARING`；payout `SUBMITTED/ACCEPTED/PROCESSING` 非终局；ReconciliationBatch `COMPLETED` 可对应 `DIFFERENCE_FOUND`。
- 证据冲突 / 置信边界：现有状态枚举混合处理态、聚合生命周期、资金效果和外部结果；真实 issuer/银行/ACH/PSP authoritative finality、beneficiary finality、payout `RETURNED` 资金返还、宿主事务拓扑与跨仓运行谱系仍是 `PENDING`。
- 裁决动作：`ask-owner`
- 问题保真度 / 求证媒介：中保真；三项目源码、测试和设计足以确认不能合并完成层级，不足以批准目标状态枚举和外部终态映射。
- 决策包 ID / 交接原因：`DP-Q003-COMPLETION-LAYERS`；多个事实 Owner 的同名状态必须先汇合为一条产品不变量，再交人类 Owner 裁决。
- 推荐答案：接受收紧后的命题。不存在跨业务、外部、资金交易、账本、余额、清结算和对账的公共总 `SUCCESS`，也不建立全局单调完成度枚举。每个事实 Owner 所回答的独立问题构成一个正交证据维度；业务、外部、资金交易、账本、余额和对账/Gate 只是基础示例，清分计算、清算资金、结算锁定、出款提交/受理和出款资金结果等场景问题保持独立。每个场景声明所需维度、局部偏序和最终展示门槛。每个维度分别表达是否适用、证据是否已知与是否终局、领域结果、资金效果、证据引用和不能外推的维度；这些属性不得合成一个状态轴，具体枚举不在本题裁决。RouteSnapshot 只证明路由决策/解释证据被记录，必须继续区分有无 legs 和可回放性；transaction 完成不自动证明 ledger 或余额。确定性 `FAILED/REJECTED` 必须说明本层是否形成资金、posting、entry 和余额影响；不同 Owner 的同名状态不得互推。账务事实与余额投影分层；外部受理、外部终态、内部资金动作、beneficiary finality 和对账完成分层。提交后结果未知表示证据尚未闭合，必须沿原幂等身份和权威查询恢复，不得改键重试或直接降为领域失败。ReconciliationBatch `COMPLETED` 不等于 `BALANCED`；当前 `BALANCED` 只在冻结范围、时点和规则内形成 Gate 候选，不自动执行资金动作。
- 本题明确不裁决：目标状态枚举、DTO/API、状态机；具体 transaction atomicity；具体账户/账目和 posting matrix；issuer/ACH/PSP 状态映射与 beneficiary finality；重试时限和人工 SLA；payout `RETURNED` 的目标资金语义；部署、事务、artifact 谱系和生产可用性。
- 用户回答：接受 `Q-003`。
- 最终结论：`accepted`。确认每个事实 Owner 所回答的独立问题构成正交证据维度；场景声明所需维度、局部偏序和最终展示门槛；每个维度分别表达适用性、证据确定性/终局性、领域结果、资金效果、证据引用和不可外推范围，不建立跨 Owner 总 `SUCCESS` 或全局单调完成度枚举。
- red_lines：不得用 transactionSn、route snapshot、业务 `PAID`、ledger entry、余额、payout 状态或 reconciliation batch 中任意一个维度单独代替其他维度的完成证据；不得把证据未知伪装成领域失败或成功；不得从本结论推出统一状态枚举。
- 影响范围：产品规则与场景验收、DSL 结果/证据语义、transaction/ledger/reconciliation 查询契约、Consumer 恢复责任和 TDD 分层断言。
- 下一阶段输入：范围审计已完成并获 Owner 确认；进入唯一 `Q-004`，产品 Wave 1 未通过前不进入 DSL。
- 重开原因：不适用；首次发起。
- 写回位置 / 更新时间：本文件与产品正文，`2026-08-12 Asia/Shanghai`。

### Q-004

- 问题 ID：`Q-004`
- 决策主题：稳定经济意图、执行 Attempt 与不可变经济语义的产品边界。
- 待裁决命题 / 命题类型：是否采用“稳定 Intent + 受控多 Attempt”，并以权威终结、零未闭合资金效果和重新授权作为确定失败后新 Attempt 的准入；类型为产品幂等、恢复与公共契约。
- 父主题：Wave 1 / `W1-02` 产品规则与异常恢复。
- 问题：是否接受 `10D.2` 的方案 A 及 `10D.3`、`10D.4` 的准入和不可变语义边界。
- 为何现在问：若不区分 Intent、Attempt、Delivery/Event/Evidence，DSL 无法同时阻止双付、支持确定失败恢复并保留原事实。
- 已查证据：Provider 当前同业务身份同摘要复用/异摘要冲突、失败交易禁止重过账、授权/Benefit 原事实重放测试；Capte 钱包 RuntimeException 降级、组合支付部分成功和 Benefit 当前配置重算；Fincone VCC、ACH、payout、reconciliation 的重复、迟到、UNKNOWN 和 Recovery 设计。
- 业务模拟结论：三个项目均支持稳定 Intent 与可追踪 Attempt 分层；`UNKNOWN` 禁止新 Attempt；部分效果禁止重跑完整 Intent，只有已冻结完成部分与剩余上限后才能处理未完成组件；确定性失败/拒绝、目标资金效果为零或已经济闭合、不再可能迟到生效并重新授权后，才可允许新 Attempt；逆向和经济修复是引用原事实的新 Intent，证据恢复不是。
- 证据冲突 / 置信边界：当前实现有 request hash 和失败状态，但不存在已确认的目标 Intent/Attempt 公共契约；真实 rail finality、自动 retry 权限和宿主运行谱系仍为 `PENDING`。
- 裁决动作：`ask-owner`
- 问题保真度 / 求证媒介：中保真；当前源码、测试与跨项目业务模拟足以裁决产品关系，不足以批准具体类型和实现。
- 决策包 ID / 交接原因：`DP-Q004-INTENT-ATTEMPT`；这是 Wave 1 经 Owner 重新确认范围后唯一追加的公共抽象问题，确认后不再发起新问题。
- 推荐答案：接受方案 A。一次经济意图在作用域/责任、经济动作与目的、价值、因果关系、预期资金效果和承重业务决策不变时保持稳定；Attempt、Delivery、Event 和 Evidence 分层。相同 Intent 的重复处理只能复用当前 Attempt 或其权威结果。只有权威证明旧 Attempt 的局部结果为确定性失败/拒绝、目标资金效果为零或相关责任已经济闭合、旧 Attempt 不再可能迟到生效，并经重新授权后，才允许创建可追踪的新 Attempt；同一时刻最多一个 Attempt 仍可能产生目标效果。存在部分效果时禁止重跑完整 Intent，只有冻结完成部分、剩余上限和原快照后才可处理未完成组件。`UNKNOWN`、在途、冲突、查询不可用或外部成功但内部未闭合时禁止新 Attempt。退款、reversal、return、adjustment 和经济补偿是引用原事实的独立追加 Intent/事实；查询、证据补全、同语义重放和继续已冻结的未完成组件仍属于原 Intent/Attempt 的恢复。
- 本题明确不裁决：key/hash/字段表、类型/API、状态枚举、存储/事务/Saga、重试策略和 rail matrix。
- 用户回答：接受方案 A。
- 最终结论：`accepted`。采用稳定 Intent + 受控多 Attempt；执行仍须满足 `10D.3` 的权威证据合取和 `10D.4` 的不可变经济语义边界。
- red_lines：不得重复形成同一目标经济效果；不得从 `FAILED` 状态名直接推导可重试；不得把领域结果和资金效果合成新状态；不得把 `UNKNOWN`、Delivery、Event、Evidence、逆向或经济修复冒充原 Intent 的新 Attempt；部分效果不得重跑完整 Intent。
- 影响范围：产品场景合同、DSL Intent/Reference 候选、transaction 恢复契约、Consumer 重试责任和 TDD hard negatives。
- 下一阶段输入：回写正式产品规则和场景依赖；不再发起新的公共抽象问题。场景 Owner PENDING 与 E4/E5 证据仍按各自范围闭合。
- 重开原因：不适用；经三问后范围重审首次发起。
- 写回位置 / 更新时间：本文件与产品正文，`2026-08-12 Asia/Shanghai`。

### P-SIM01-01-D

- 问题 ID：`P-SIM01-01-D`
- 决策主题：商品钱包按订单责任范围选择收款责任模式。
- 命题类型：场景级产品责任路由，不是公共抽象问题。
- 问题：是否接受宿主按 `tenant + business scene + merchant（或无 merchant）+ 责任规则版本` 在 A 平台自营、B 商户经济直收、C 平台代收清算中显式选择并每笔冻结唯一模式。
- 已查证据：Capte 订单/商品快照没有 seller/merchant/payee 责任，`payeeId=capte` 是业务标签，Wallet 请求只含 payer；Fincone 设计只支持按真实 payee/责任解析的候选，不能反推 Capte 模式。
- 推荐答案：接受 D。公共层只固定选择与冻结规则，各实际订单由宿主责任事实决定 A/B/C；无法唯一选择时授权前 fail closed。
- 用户回答：接受 `P-SIM01-01-D`。
- 最终结论：`accepted`。D 不构成第四种资金模式，不批准当前 Capte 或 Provider 已能执行；真实 seller/payee、规则版本和责任账户准入转入 `P-SIM01-01-HOST`。
- red_lines：不得从 `payeeId=capte`、当前 route、测试账户名或默认配置猜模式；不得多模式并用；缺责任证据不得先扣冻 payer；退款不得按当前绑定重算；`PAID` 不得外推清结算、出款或对账完成。
- 影响范围：`SIM-01` 前置责任、账户解析、退款责任、下游清结算/对账适用性与 `P-SIM01-02` 的指定责任账户证据。
- 下一阶段输入：接受 Checker 后进入 `P-SIM01-02`；`P-SIM01-01-HOST` 独立阻断真实资金执行。
- 写回位置 / 更新时间：本文件与产品正文，`2026-08-12 Asia/Shanghai`。

### P-SIM01-02-A

- 问题 ID：`P-SIM01-02-A`
- 决策主题：纯内部商品钱包的 `PAID/PARTIAL_REFUND/REFUNDED` 业务声明门槛。
- 命题类型：场景级业务声明和履约门槛，不是公共总完成状态。
- 问题：在订单已按 `P-SIM01-01-D` 冻结唯一责任模式后，哪些正交证据闭合才允许 Order Owner 声明支付或退款最终完成。
- 已查证据：Capte 当前由 participant 布尔结果/可选流水推进 Bill/Order；Provider 测试源码分别证明 transaction、ledger、balance 可取证且单层结果不能替代其他层；Fincone 跨轨道设计证明 external finality、清结算、出款和对账必须保持独立。
- 推荐答案：A，严格内部资金闭合。要求业务目标、动作事实、平衡账务和指定责任账户余额效果全部闭合；不等待不适用的外部或后继证据。
- 用户回答：授权按推荐推进，接受 `P-SIM01-02-A`。
- 最终结论：`accepted`。部分退款只声明已闭合累计金额；全部可退范围及所有必需维度闭合后才声明 `REFUNDED`。`UNKNOWN` 或局部同步失败只修复缺失维度，不重做资金动作。
- red_lines：不得用 non-empty String、单一 FundsTransaction/main、Bill 状态、单笔 LedgerEntry 或单次余额观测独自推出业务最终状态；不得把外部 finality、merchant settlement、payout 或 reconciliation 混入纯内部钱包门槛；不得因产品规则接受而声称现有 Host 已满足。
- 影响范围：`SIM-01` 的业务声明、履约、部分/全额退款累计、`VC-001` 的 R3/R4 取证范围和宿主验收。
- 下一阶段输入：接受 Checker 后进入 `P-SIM02-01`；当前 Capte 取证缺口转为 `P-SIM01-02-HOST`。
- 写回位置 / 更新时间：本文件与产品正文，`2026-08-12 Asia/Shanghai`。

### P-SIM02-01-A

- 问题 ID：`P-SIM02-01-A`
- 决策主题：券 + 钱包组合支付在局部成功后的父计划策略。
- 命题类型：场景级宿主策略，不是公共总状态或跨系统原子契约。
- 问题：一腿已形成权威成功事实，另一腿未执行、`UNKNOWN` 或权威失败时，Order Owner 应如何恢复、退出并展示父计划。
- 已查证据：Capte 当前在后腿失败时会丢失成功腿引用、忽略 cancel 结果并把局部事实压成父级 failure；已接受 Q-003/Q-004 要求局部证据正交、`UNKNOWN` 先查询与新 Attempt 受控准入。
- 推荐答案：A，在已冻结履约时限、业务授权与 Q-004 准入内只恢复未完成腿；超出边界后放弃父支付并逆向所有成功腿。
- 用户回答：接受 `P-SIM02-01-A`。
- 最终结论：`accepted`。父层不覆盖局部成功；任一腿 `UNKNOWN` 时不执行新动作；所有必需正向闭合后才声明完整支付，所有成功腿的必需逆向闭合后才声明放弃/取消。
- red_lines：不丢弃成功腿引用；不在 `UNKNOWN` 时 cancel/refund/补偿；不整组重跑；不将确定失败简化为自动重试；不隐藏逆向/补偿失败；不将普通券资金化。
- 影响范围：`SIM-02` 的父计划、履约恢复、退出/逆向、局部展示和重启恢复验收。
- 后续结果：Coupon 动作权威已由 `P-SIM02-02-A` 接受；当前宿主供证分别由 `P-SIM02-01-HOST` 与 `P-SIM02-02-HOST` 阻断，不因两个产品策略接受而关闭。
- 下一阶段输入：`P-SIM02-02-A` 接受 Checker；通过后进入 `P-SIM03-01 / BENEFIT_FUNDING_RESPONSIBILITY`。
- 写回位置 / 更新时间：本文件与产品正文，`2026-08-12 Asia/Shanghai`。

### P-SIM02-02-A

- 问题 ID：`P-SIM02-02-A`
- 决策主题：Coupon confirm、release/cancel、return 的权威动作事实与宿主恢复边界。
- 命题类型：场景级跨 Owner 事实权威，不是 Coupon 实现或资金公共抽象。
- 问题：同步响应未知或重启后，Order 应以什么事实判断 Coupon 动作闭合，并安全推进组合支付恢复或退出。
- 已查证据：Coupon 域源码已有不可变动作流水、原事实关系和支付/退款决策查询；Order Coupon participant 仅判断同步 `success`，不保存 confirm/release/return 引用，confirm 异常还会立即 release。
- 候选：A 全部动作由 Coupon Owner 的耐久动作事实权威回答；B 只签收部分动作、未覆盖动作永久 fail closed/manual；C 由当前聚合状态、同步响应或 Order 影子状态推断动作。
- 推荐答案：A。Coupon Owner 的具体动作事实是唯一业务权威；Order 只保存引用与本地消费结果，异常沿原动作身份查询。
- Owner 回答：用户明确接受 `P-SIM02-02-A`。
- 当前结论：`accepted / ACCEPTANCE_CHECKER_PASS`。Coupon Owner 的耐久动作事实是唯一业务权威；Order 只保存引用与本地消费结果，B/C 不采用。
- red_lines：不把普通券资金化；不以布尔结果、异常、父状态、当前聚合状态或日志存在单独证明动作完成；confirm UNKNOWN 不 release；任何必需 Coupon 逆向 UNKNOWN 不逆向钱包或声明取消。
- 影响范围：`SIM-02` 的 Coupon 腿权威结果、P-SIM02-01-A 的恢复/退出门槛、父计划重启恢复与宿主验收。
- 未关闭项：`P-SIM02-01-HOST`、`P-SIM02-02-HOST`、真实 Bean/schema/事务和宿主恢复证据；本题不关闭 G1 或实现门禁。
- 下一阶段输入：接受 Checker PASS 后进入 `P-SIM03-01 / BENEFIT_FUNDING_RESPONSIBILITY`。
- 写回位置 / 更新时间：本文件与产品正文，`2026-08-12 Asia/Shanghai`。

### P-SIM03-01-D

- 问题 ID：`P-SIM03-01-D`
- 决策主题：商品订单 `SPECIFIED` Benefit 的平台/商户成本承担、经济价值承接与资金影响。
- 命题类型：场景级产品经济责任，不是账户、账目、posting 或会计分录设计。
- 问题：平台承担 60、商户承担 40 时，商户 40 是经营折让还是独立资金 contribution，平台 60 如何补足订单价值，哪些原事实约束退款和恢复。
- 证据等级：Capte production/test source=`E2`；Fincone `WF-FIN-BENEFIT-MP-011` 为 docs-first accepted child scope=`E1`，但整体 Owner/L3/enable/release/production 仍 PENDING/BLOCKED；Provider source/test 只证明通用候选能力，不证明本场景责任。
- 已查证据：Capte 将出资角色、资金性质、比例和 receiver 分开配置，但 60/40 只来自测试模拟，生产 settle 未传完整责任且退款重读活动配置；Fincone 窄化子契约只确认真实订单 payee 为 `SPECIFIED` 价值承接来源、每个适用资金项独立留痕和原路退款，不确认商户 40 必须资金化。
- 候选：A 平台 60 资金 contribution + 商户 40 经营折让；B 平台 60 / 商户 40 两笔独立资金 contribution；C 仅成本分配且零 Benefit 资金动作；D 为按适用范围从 A/B/C 中唯一选择并冻结的路由规则，不是第四模式。
- 接受答案 D：宿主按 `tenant + 产品类别/商品订单 + business scene + merchant（或无 merchant）+ currency + Benefit 责任规则版本` 为每次核销产出唯一 A/B/C 选择，并冻结成本/承接责任、资金影响、分摊舍入和退款原事实。无法唯一命中、多模式同时命中、规则版本或责任快照不明时 fail closed。
- Owner 回答：用户明确接受 `P-SIM03-01-D`。
- 当前结论：`accepted / ACCEPTANCE_CHECKER_PASS`。
- red_lines：不从角色枚举、活动字段、当前配置、测试账户、order payee、现有 receiver/账目或 Provider 测试倒推成本承担责任；order payee 仅在 Fincone 已接受的商品订单 `SPECIFIED` child scope 内作为冻结的经济价值承接来源，不外推其他产品类别、账户或账目；不把成本方自动等同收款方；不合并多个责任项；不按当前配置退款；不以核销/活动引用代替原成功 contribution；不把 A/B/C 写成账户类型、subject code、posting matrix、会计/法律结论或 API。
- 影响范围：`SIM-03` 正向资金事实基数、Benefit 完成/partial、退款原事实、`P-SIM03-02/03` 的恢复验收和 `VC-002` 后续资格。
- 未关闭项：`P-SIM03-01-HOST` 的 D 路由结果/责任快照，`P-SIM03-02` partial/恢复政策、`P-SIM03-03` 宿主权威证据、账户准入、artifact lineage、real Bean/schema/tx、E4、G1、DSL、RED、Execution Grant、代码/API/Git。
- 下一阶段输入：接受 Checker PASS 后只进入 `P-SIM03-02_DECISION_PACKAGE`；待该决策包独立准出后才能进入 Owner Gate，宿主自动执行仍由 `P-SIM03-01-HOST / P-SIM03-03 / E4` 阻断。
- 写回位置 / 更新时间：本文件与产品正文，`2026-08-12 Asia/Shanghai`。

### P-SIM03-02

- 问题 ID：`P-SIM03-02`
- 决策主题：D 冻结责任模式后的 Benefit 部分成功、恢复、退出和完整声明政策。
- 命题类型：场景级父 Benefit 产品策略；不修改已冻结的责任模式、金额、承接方或规则版本。
- 证据等级：Capte production/test source=`E2`；Fincone accepted child scope=`E1`，仅支持逐资金项事实与原引用，不证明父级策略或运行能力。
- 已查证据：该决策包冻结时，Consumer 顺序执行 funding rows、按记录存在跳过、异常中断且退款重读当前配置，不能表达 M-A 的资金/非资金组合，也不能用 NONE 证明 M-C 两项业务责任完成。`plan-r2.68` 后续只补证单项 `SPECIFIED` 的耐久资金引用与测试宿主恢复，不关闭多责任项父计划缺口。
- 候选：`R-A` 恢复优先、越界后逐项逆向（推荐）；`R-B` 确定失败即退出并逐项逆向；`R-C` 人工接管。三者与责任模式 `M-A/M-B/M-C` 正交。
- Owner 回答：用户明确接受 `P-SIM03-02-R-A`；`R-B/R-C` 未选择。
- 当前结论：`accepted / P-SIM03-02-R-A / ACCEPTANCE_CHECKER_PASS`。
- red_lines：UNKNOWN 后新执行/逆向；整组重跑；覆盖成功项；资金/非资金互相替代；失败金额转嫁或重分摊；按当前配置补造原事实；记录存在/返回 String/聚合状态冒充闭合；跨责任退款或超累计；M-C 产生资金动作；partial/部分逆向冒充完整完成/恢复；写入 API/表/事务/Saga/posting/会计分录。
- 影响范围：`SIM-03` 父 Benefit 展示、继续/退出、逐项逆向、人工接管、`P-SIM03-03` 宿主验收与 `VC-002` 后续资格。
- 未关闭项：`P-SIM03-01-HOST` 路由/责任快照；`P-SIM03-02-HOST` 父策略、必需项集合、冻结时限/授权、逐项恢复进度与重启恢复；`P-SIM03-03` 动作引用、局部结果/资金/账务查询；artifact lineage、real Bean/schema/tx、E4、G1、DSL、RED、Execution Grant、代码/API/Git。
- 当时下一阶段输入：接受 Checker 准出后进入 `P-SIM04-01_DECISION_PACKAGE`；该包现已通过 Checker，当前唯一入口见 Metadata 与恢复入口。
- 写回位置 / 更新时间：本文件与产品正文，`2026-08-12 Asia/Shanghai`。

### P-SIM04-01

- 问题 ID：`P-SIM04-01`
- 决策主题：VCC PREPAID/SHARED 责任配置与授权生命周期累计。
- 命题类型：场景级产品/公共合同；不裁 issuer authority/finality、清结算、账户分类或 posting。
- 证据等级：Provider 当前 source/test=`E2`（本轮未 fresh 执行）；Fincone VCC docs=`E1 / OwnerDecision=PENDING / NOT_STARTED`；真实 VCC runtime Consumer/E4 缺失。
- 已查证据：Provider 授权根、后继动作、累计校验和 SHARED Credit/Funding 双责任源码/测试；Fincone VCC/issuer-account docs-first 责任、Event/FundsProcessRef/聚合分层；Payment Expert 的 Card/PaymentInstrument/内部账户与外部网络边界。现有材料对完成输入 30/50 存在“增量 30+50”与“累计快照 30->50”冲突。
- 候选：A 规范化增量动作事实（推荐），全链累计 `80/20/20`；B 权威累计快照，全链累计 `50/20/20` 且仍有授权余量 30。两者都冻结 PREPAID Funding 责任和 SHARED Credit + 父 Funding 双责任，并使用同一原事实/上限/UNKNOWN 红线。
- Owner 回答：用户明确回复 `A`；接受 `P-SIM04-01-A`，B 未选择。
- 当前结论：`accepted / P-SIM04-01-A / ACCEPTANCE_CHECKER_PASS`。公共层只消费经 VCC/issuer 边界权威归一的不可变增量动作；累计快照的 authority/sequence/归一仍由 `P-SIM04-02` 裁决。
- red_lines：Card/holder/tool/IssuerAccount 当内部余额主体；Credit 当现金；Funding 当额度；SHARED 永久一对一；当前 binding/余额/卡状态重算原责任；authorize success 冒充完成；跨原事实 complete/release/refund 或超累计；release/refund 互换；UNKNOWN 后补单/逆向；聚合事实冒充多动作；用 issuer accepted、内部 SETTLEMENT、Network Settlement 或 reconciliation 倒推本题完成；写入 Java/API/表/事务/posting/rail matrix。
- 影响范围：`SIM-04` 的 PREPAID/SHARED 责任快照、两次完成输入、授权剩余、释放和退款累计、父层可观察范围与 `P-SIM04-02` 前置输入。
- 未关闭项：当前 Provider 逐 complete refund 分配/查询；VCC adapter 的累计快照归一；`P-SIM04-02` issuer/processor authority、sequence、finality、overcapture/late clearing/Network Settlement；真实 VCC Consumer、动作引用持久化、timeout-after-effect 恢复、artifact lineage、Bean/schema/tx、HOST/E4、G1、DSL、RED、Execution Grant、代码/API/测试/Git。
- 下一阶段输入：接受范围 Checker 已准出；当前只进入 `P-SIM04-02_DECISION_PACKAGE`，不得直接进入 Owner Gate 或实现。
- 写回位置 / 更新时间：本文件与产品正文 `5.19`，`2026-08-12 Asia/Shanghai`。

### P-SIM04-02

- 问题 ID：`P-SIM04-02`
- 决策主题：issuer/processor 外部动作 authority、sequence/amount semantics、action applicability 与 external finality。
- 命题类型：场景级产品证据政策；不裁内部账户/posting、Java/API/表/事务、厂商 rail matrix 或 Network Settlement 资金处置。
- 证据等级：Fincone VCC docs=`E1 / OwnerDecision=PENDING / NOT_STARTED`；Provider current source/test=`E2` 仅证明内部资金动作承接，不证明 issuer authority；真实 VCC runtime Consumer/E4 缺失。
- 已查证据：Fincone delivery/Event/aggregate/FundsProcessRef 分层、多来源与累计快照候选、expired/reversal/refund/overcapture/late clearing/chargeback/Network Settlement 边界；Provider payment-instrument 动作、拒绝零资金、generic FORCE 与 dispute refund 审计引用、external confirmed-credit 和 reconciliation 的明确非 VCC authority 边界。
- 候选：A authoritative query/report refresh first；B contracted event first；C convergent evidence/manual。三者共享同一四层证据模型、identity/sequence/amount/action/finality 问题与 hard negatives；版本化选择规则只按 issuer/program/action/rule version 唯一路由，不是第四候选。
- 推荐与 Owner 回答：用户回复“可以”，接受 `P-SIM04-02-D` 的版本化选择原则与能力归位；D 不是第四种证据政策，A/B/C 不在 `wind-funds` 内运行。首个 issuer/source matrix 未签收前，adapter 自动路径保持零资金影响/manual。
- red_lines：webhook/200/签名/event 名称直接当权威；carrier 固定优先级；identity 混用；时间当 sequence；快照当 delta；query 覆盖历史；UNKNOWN 后补单/逆向；expired 自动 release；void 撤销已完成金额；unlinked refund 猜原交易；generic FORCE 冒充 overcapture；chargeback 当普通 refund；Network Settlement/内部 SETTLEMENT/reconciliation 倒推动作完成；写入 API/表/事务/posting/rail matrix。
- 影响范围：`SIM-04` 外部 delivery -> authoritative action -> normalized delta 的准入、重复/乱序/迟到/冲突恢复、父层外部证据展示以及 `P-SIM04-01-A` 的 adapter 前置。
- 未关闭项：`P-SIM04-02-HOST` 继续持有具体 issuer/action 的 source/version/scope/identity/sequence/amount/finality、overcapture/late clearing、chargeback/损失、Network Settlement 和真实 adapter 证据；`P-SIM04-01-HOST`、逐 complete refund、artifact/Bean/schema/tx/E4、VC/G1/RED/Execution Grant、DSL/API/code/test/Git 均未关闭。
- 当前结论：`accepted / P-SIM04-02-D / ACCEPTANCE_CHECKER_PASS`；只接受 adapter 侧版本化路由和 `wind-funds` 归一事实准入边界，不代表任何具体 A/B/C、issuer 规则或运行能力已签收。
- 下一阶段输入：接受范围 Checker 准出后曾进入 `P-SIM05-01_DECISION_PACKAGE`；该包现已形成，当前唯一入口见 Metadata 与恢复入口。
- 写回位置 / 更新时间：本文件与产品正文 `5.20`，`2026-08-12 Asia/Shanghai`。

### P-SIM05-01

- 问题 ID：`P-SIM05-01`
- 决策主题：ACH accepted/confirmed/finality 的内部资金准入，以及 return/reversal 原事实与 NOC 零资金边界。
- 命题类型：场景级外部事实与资金可用性策略；rail 协议、状态码、retry/re-origination、Java/API/表/posting 不在本题。
- 证据等级：Fincone GlobalAccount docs=`E1 / OwnerDecision=PENDING / NOT_STARTED`；Provider current source/test=`E2` 只证明 confirmed credit 窄入口与重放/冲突，不证明 authority/finality、return/reversal/NOC 或真实 HOST/E4。
- 已查证据：GlobalAccount Instruction/Receipt/Difference 与 accepted/confirmed/finality 分层、return/reversal/NOC 候选边界；Provider `ExternalFundsEventApplicationService`、confirmed credit topup、外部资金事实去重/冲突、accepted fail-fast 和 reconciliation 只读边界。
- 候选：A confirmed-gated effect（推荐）；B finality-gated effect；C manual-gated effect。三者都要求 adapter 先验真归一，accepted 零资金，return/reversal 逐原事实追加，NOC 零资金。
- 推荐与 Owner 回答：用户明确接受 A，并澄清上游业务层/adapter 判断外部业务数据可信、`wind-funds` 专注资金处理；B/C 未选择。该澄清不等于 Funds 无条件信任调用方。
- red_lines：raw rail/NOC 状态进入 Public API；accepted/字符串 confirmed 直接入账；外部账户入账；arrival time 当 authority；NOC 改余额；return/reversal 覆盖原事实或当 refund/负 topup；UNKNOWN 补单；reconciliation/内部 SETTLEMENT/余额倒推 finality；在资金底座建 ACH 状态机或 rail policy engine。
- 影响范围：`SIM-05` 外部事实 -> normalized primary/recovery effect 的准入、内部效果声明、return/reversal 累计、NOC 主数据边界和父层人工出口。
- 未关闭项：具体 source/version/scope/authority/finality/status mapping/return window/NOC/retry 规则、真实 adapter/Consumer、HOST recovery、artifact/Bean/schema/tx/E4、VC/G1/RED/Execution Grant、DSL/API/code/test/Git。
- 当前结论：`accepted / P-SIM05-01-A / ACCEPTANCE_CHECKER_PASS`；产品边界已接受并通过独立 Checker，不代表 HOST/E4 或现有 Provider/Consumer 已具备能力。
- 当时下一阶段输入：`P-SIM06-01_DECISION_PACKAGE`；该包现已形成并进入独立 Checker，当前唯一入口见 Metadata 与恢复入口。
- 写回位置 / 更新时间：本文件与产品正文 `5.21`，`2026-08-13 Asia/Shanghai`。

### P-SIM06-01

- 问题 ID：`P-SIM06-01`
- 决策主题：在 acquiring 产品与责任范围已冻结的前提下，何种上游规范化 capture 证据强度足以让 `wind-funds` 接纳一次内部待清算资金效果。
- 命题类型：capture authority/admission 产品策略；真实 Merchant/持牌角色、PSP 协议、clearing/settlement/payout、refund/dispute/chargeback 责任、Java/API/表/posting 不在本题。
- 证据等级：Fincone acquiring admission=`E1 / AdmissionStatus=BLOCKED / OwnerDecision=PENDING / ACQ-GATE-001~007=PENDING`；Provider `AcquiringSettlementBusinessFlowTests`=`E2 source intent / 本轮未 fresh 执行`，只证明内部组合模拟，不证明 PSP authority、真实 Merchant 责任、HOST/E4 或生产能力。
- 已查证据：收单准入卡与任务规划；项目收单资金底座边界；Provider 测试中 normalized capture -> generic pay、split 零资金、clearing/settlement/payout 与原路退款的内部组合事实。
- 候选：A authoritative normalized capture 即准入（已接受）；B 在 A 之外还需已签收的独立规范化佐证（未选择）；C 仅有权人工签发的 normalized capture 才准入（未选择）。三者使用相同责任范围、capture 类别、资金效果和完成证据，只改变准入证据强度。
- 推荐与 Owner 回答：用户明确回复 `A`，接受 `P-SIM06-01-A`；B/C 未选择并保留比较记录，不作为 A 失败后的 Funds runtime fallback。
- red_lines：raw webhook/status/rail code 进入 Funds；仅凭 `CAPTURED` 字符串入账；Funds 自选 PSP source/finality；capture 外推 clearing/settlement/payout/finality；UNKNOWN 换 identity 重发；冲突覆盖；退款/争议无原成功 capture 或超累计；测试账户、subject code、posting 或聚合状态冒充公共产品合同。
- 影响范围：normalized capture 的资金准入门槛、授权/无内部授权类别、partial/multiple capture 累计、逐 capture refund 原事实、UNKNOWN 恢复、完成证据分层与人工出口。
- 未关闭项：`P-SIM06-01-HOST` 持有 `ACQ-GATE-001~007`、具体 PSP/source/version/scope/status/finality、真实 adapter/Consumer、payer/收款责任与待清算责任、逐 capture 引用/查询、timeout/restart、artifact/Bean/schema/tx/E4；`P-SIM06-02-B` 已接受、`P-SIM06-03-HOST`、VC/G1/RED/Execution Grant、DSL/API/code/test/Git 继续独立阻断。
- 当前结论：`accepted / P-SIM06-01-A / ACCEPTANCE_CHECKER_PASS`。只接受 adapter 权威 normalized capture 进入 Funds 独立准入的产品边界，不代表 acquiring admission、HOST/E4 或现有 Provider/Consumer 已具备能力。
- 当时下一阶段输入：`P-SIM06-02_DECISION_PACKAGE`；该包现已形成并进入独立 Checker，当前唯一入口见 Metadata 与恢复入口。
- 写回位置 / 更新时间：本文件 `8.14`、产品正文 `5.22`，`2026-08-13 Asia/Shanghai`。

### P-SIM06-02

- 问题 ID：`P-SIM06-02`
- 决策主题：executor `SUCCEEDED` 与内部资金闭合后，何种证据强度足以声明 beneficiary/bank arrival 或 rail finality。
- 命题类型：payout external finality / merchant display 产品策略；出款准入、内部 payout 资金动作、RETURNED/recovery、API/表/posting 不在本题。
- 证据等级：Fincone clearing/acquiring=`E1 / OwnerDecision=PENDING / Admission BLOCKED`；Provider payout source/test=`E2 source intent / 本轮未 fresh 执行`，只证明规范化回执与内部资金闭合，不证明真实 executor 或 beneficiary finality。
- 已查证据：Fincone 清结算产品/系分/准出与收单准入卡；Provider `PayoutOrderState`、`PayoutOrderApplicationServiceImpl`、`HandlePayoutReceiptRequest`、`PayoutOrderDTO`、`PayoutOrderApplicationServiceTests`；项目产品、接入指南和 TDD 出款边界。
- 候选：A rail-finality profile；B independent-arrival evidence（推荐）；C authorized-manual-finality。三者使用相同 payout/资金事实，只改变最终展示证据强度；版本化选择在上游 adapter，Funds 不持有 rail route。
- 推荐与 Owner 回答：用户按推荐明确接受 B；A/C 未选择并保留比较记录，不作为 B 失败后的 Funds runtime fallback。
- red_lines：submitted/accepted/processing、PayoutOrder SUCCEEDED、FundsTransaction、Ledger、Balance、Gate/BALANCED 或测试回执单独冒充 beneficiary finality；raw rail 状态进入 Funds；迟到相反事实覆盖历史成功、自动返还或换 identity 重发；RETURNED 资金处置偷接本题。
- 影响范围：payout 展示证据、beneficiary arrival/finality 的 authority profile、版本化选择、冲突/UNKNOWN/manual 与 close evidence；不改变 payout 资金动作。
- 未关闭项：真实 provider/rail/bank source/version/scope/arrival/finality、展示文案与 Consumer、timeout/restart、RETURNED/recovery、artifact/Bean/schema/tx/E4；Fincone Admission/ACQ Gate、`P-SIM06-01-HOST / P-SIM06-03-HOST`、VC/G1/RED/Grant、DSL/API/code/test/Git 继续阻断。
- 当前结论：`accepted / P-SIM06-02-B / ACCEPTANCE_CHECKER_PASS`；独立接受范围 Checker 判定 `PASS / 0 P0-P2`。
- 当时下一入口：`P-SIM06-03_DECISION_PACKAGE`；该包现已形成，当前唯一入口见 Metadata/恢复入口。
- 写回位置 / 更新时间：本文件 `8.15`、产品正文 `5.23`，`2026-08-13 Asia/Shanghai`。

### P-SIM06-03

- 问题 ID：`P-SIM06-03`
- 决策主题：capture refund、payout RETURNED/迟到 reversal、chargeback/dispute、fee、loss 与 merchant recovery 的责任证据强度及 normalized funds action 准入。
- 命题类型：reverse/recovery liability admission 产品策略；不裁 raw rail 语义、Merchant 法律身份、账户/账目、posting、API 或实现。
- 证据等级：Fincone acquiring=`E1 / Admission BLOCKED / ACQ-GATE-001~007 PENDING`；Provider payout/recovery=`E2 source intent / 本轮未 fresh 执行`，不证明真实 rail、责任、Consumer 或 E4。
- 已查证据：Fincone 收单准入、清结算产品/系分/准出与资金指南；Provider payout receipt/result、RecoveryOrder source/test；项目已接受的原事实、累计、正交完成和上游归一边界。
- 候选：A pre-signed-policy recovery；B typed-liability-gated recovery（推荐）；C authorized-case recovery。三者使用相同原事实、累计和 Funds 准入，只改变责任证据门槛。
- 推荐与 Owner 回答：用户明确回答 `B`，接受 typed-liability-gated recovery；A/C 未选择并仅保留比较记录，`accepted_answer=B`。缺少分项责任只能 fail-closed/manual，不能把 C 当运行时 fallback。
- red_lines：RETURNED=FAILED 或覆盖 SUCCEEDED；没有原成功 effect 却制造逆向；同一本金实际回流后重复 merchant recovery；refund/return/chargeback/fee/FX/loss 合并或静默净额；用当前 Merchant/余额/reserve/negative 反推责任；RecoveryOrder/Difference/Gate/BALANCED 推导 recovered/loss；raw reason/status 或责任策略进入 Funds Core。
- 影响范围：normalized reverse/recovery action 的责任证据、逐原事实累计、dispute/chargeback 后继链、UNKNOWN/manual、loss/write-off 与对账关闭边界；不改变已接受 capture/payout 政策。
- 未关闭项：`P-SIM06-03-HOST` 承接真实 source/rule/version/scope、Merchant/rail/fee/FX/loss 责任、逐项 Consumer、timeout/restart、artifact/Bean/schema/tx/E4；Fincone Admission/ACQ Gate、全部 HOST/VC/G1/RED/Grant、DSL/API/code/test/Git 继续阻断。
- 当前结论：`accepted / P-SIM06-03-B / ACCEPTANCE_CHECKER_PASS`；决策包与接受范围独立 Checker 均判定 `PASS / 0 P0-P2`。
- 当时下一入口：`P-SIM07-01_DECISION_PACKAGE`；该包现已形成，当前入口见 Metadata/恢复入口，不得直接进入 Owner Gate、RED 或实现。
- 写回位置 / 更新时间：本文件 `8.16`、产品正文 `5.24`，`2026-08-13 Asia/Shanghai`。

### P-SIM07-01

- 问题 ID：`P-SIM07-01`
- 决策主题：上游已归一来源进入对账自动对平的证据强度，以及 Difference 的追加处置与 current-lineage 关闭合同。
- 命题类型：reconciliation evidence admission 产品策略；不裁 raw source/rail 解析、业务匹配路由、资金修复责任、API 或实现。
- 证据等级：Fincone 清结算=`E1 / OwnerDecision=PENDING / Gate OPEN`；Provider reconciliation=`E2 current source intent / HEAD + dirty worktree / 本轮未 fresh 执行`，不证明真实来源 Consumer、L3/E4 或发布。
- 已查证据：Fincone 清结算产品、系分与准出；Provider batch/source snapshot、run/match、difference/action/rerun、Gate current-lineage face/impl 与 source tests；项目已接受的事实不可覆盖、Owner 正交与 Funds 不解释 raw protocol 边界。
- 候选：A exact-evidence-gated（推荐）；B signed-policy-evidence-gated；C authorized-case-evidence-gated。三者使用同一冻结 scope/source、不可变结果、追加 Difference 和 current-lineage 关闭合同，只改变自动对平证据强度。
- 推荐与 Owner 回答：用户明确回复 `A`，`accepted_answer=A / status=ACCEPTED / acceptance_checker=PASS`。A 只接受 normalized `1:1` strict exact；复杂关系由来源 Owner 先固化为单一聚合事实。B/C 未选择且不是 A 的运行时 fallback。
- red_lines：重复引用伪装 `1:N/N:1`；缺侧/coverage 不全按零；行数/总额相等即 Balanced；无 FX/舍入证据跨币比较；覆盖旧 run/result/difference/action；人工备注或 adjustment 已完成直接关 Difference；非 current lineage/旧 Gate 放行；reconciliation 自动调账/核销或倒推资金与外部 finality。
- 影响范围：normalized source snapshot、match relation/tolerance evidence、Difference closure、current lineage 与 Gate 消费；不改变交易、账本、余额、清分、结算或 payout 事实。
- 未关闭项：`P-SIM07-01-HOST` 承接真实 source authority、adapter/Consumer、strict equality、coverage/watermark、current-lineage 恢复、SLA/conditional pass/manual authority、timeout/restart、artifact/Bean/schema/tx/E4；全部既有 HOST/VC/G1/RED/Grant、DSL/API/code/test/Git 继续阻断。
- 当前结论：`accepted / P-SIM07-01-A / ACCEPTANCE_CHECKER_PASS`；独立 Checker 判定 `PASS / 0 P0-P2`，产品选择仍不代表 A enforcement、HOST/E4 或运行能力成立。
- 当前入口：`W1-02 / G1 PRODUCT_INFORMATION_READINESS_AND_ADMISSION`；只允许先判产品信息就绪与准出，不得直接进入 HOST、W2/DSL、RED 或实现。
- 写回位置 / 更新时间：本文件 `8.17`、产品正文 `5.25`，`2026-08-13 Asia/Shanghai`。

### D-MIG-001

- 问题 ID：`D-MIG-001`
- 决策主题：ActionFact 的单一目标物理事实源，是继续从既有 durable fact group 做版本化只读投影，还是全项目切换为独立耐久 ActionFact 写入。
- 命题类型：跨模块重构/事实迁移决策；不重开产品 action kind、公共资金不变量、宿主 authority/finality 或场景策略。
- 候选：A `EXISTING-DURABLE-FACT-PROJECTION`（推荐）；B `DURABLE-ACTION-FACT-WRITE`；C `HYBRID-OLD-PROJECTION-NEW-WRITE`（拒绝候选）。A/B 只能项目级单选，不能按 action kind fallback。
- 推荐依据：当前四类动作已有既有耐久事实与只读投影 E2，A 不新增表或第二写链；B 只有在 A 无法长期承接且可全量迁移/回填/一次切流时才成立；C 制造双读/双真相。
- 决策 Owner：Human Owner 最终选择；Transaction/Route、Ledger/Balance 和架构 Owner 提供事实与迁移意见；独立 Checker 复核候选同构性、跨版本稳定性与停止线。
- 证据 tuple：DSL 的不可变 `FundsActionFact` / 版本化 `SemanticDigest`；系分第十一章 `MIG-00~09`、执行/验证 Owner 与回退表；TDD `20.12`；当前 `DefaultFundsTransactionQueryService` 及四个已完成 Green 切片仅作 E2 现状证据。
- 跨版本合同：A/B 均须保证应用升级和重启后既有 identity、Money、outcome/effect、original refs、route provenance 与 digest 不变；A 版本化 projector，B 版本化 schema 并全量迁移/校验/回滚。
- red_lines：按动作混用 A/B；长期双写/双读；current projector 静默重解释历史；缺原事实/route/累计时继续补猜；用 Checker PASS 冒充 Owner 接受、DDL/实现或迁移授权。
- blocked behavior：`accepted_answer=A / status=ACCEPTANCE_CHECKER_PASS / owner_decision=ACCEPTED`。B/C 未选择且不是 fallback；MIG-02C refund 文档卡已形成，现有耐久事实无法证明逐 complete 分配，因而只允许文档 Checker 与后续重开决策包；不执行 RED/Green，不修改 Java/API/DTO/DDL/Mapper、Consumer 或 Git。
- reopen condition：Checker 判定决策包 `PASS / 0 P0-P2` 后进入 Owner Gate；若 A 在新动作上失败，只能保持该动作不支持并重开本决策，B 必须覆盖既有与后续全部动作。
- 影响文档：产品当前入口、系分第十一章、TDD `20.12`、本文件 `8.31`/Decision Register/history/recovery。
- 当时重开入口：`D-MIG-001-R_REFUND_ALLOCATION_DURABILITY_OWNER_GATE`；MIG-02C 已证明 A 对 canonical refund 不可机械投影，决策包 Checker 已 PASS 后曾进入 Human Owner 选择。当前 Owner 已接受 A，活动入口以 Metadata 与恢复入口为准，仍不得直接进入 RED/Green。

### MIG-02B

- 主题：ordinary authorization 未完成范围的 canonical `release` ActionFact 文档卡。
- 范围：唯一成功 authorization 原事实、正 Money、逐原 HOLD/RELEASE route provenance、`complete + release <= authorized`、重放/冲突/UNKNOWN 与跨版本稳定性。
- 证据：当前 `REVERSAL` converter、remaining guard、route replay、lifecycle cumulative、Ledger/Balance 与 authorization flow tests 仅作 E2 执行基线；query service 尚无 release ActionFact projector。
- red_lines：balance unfreeze、settlement release、payout failure、refund、force/no-auth、expired/timeout 自动释放；调用方选 route；用 FAILED 标签或余额反推 release；新增表、第二写链、registry/factory 或按 action fallback B。
- 当前状态：`DOCUMENT_CARD_CHECKER_PASS / CODE_FREEZE / plan-r2.112`；最终独立 Checker 判定 `PASS / 0 P0-P2`，不要求新的产品 Owner 选择。
- 下一入口：`MIG-02C_AUTHORIZATION_REFUND_DOCUMENT_CARD`；继续文档优先，不建立 release Entry Card 或执行 RED。

### MIG-02C

- 主题：ordinary authorization 链上对 `1..n` 条 successful complete 的 canonical `refund` ActionFact 文档卡。
- 目标合同：独立 reverse Intent；逐 complete original refs + allocated Money；分配合计等于 refund Money；逐原责任/route/cumulative；重放、UNKNOWN 与跨版本不变。
- 当前 E2：`FundsAuthorizationTransactionRefundRequest`/instruction 只引用 authorization root，lifecycle 只校验根级 `completed-refunded-declined`；没有逐 complete 分配。
- red_lines：根级引用或聚合金额冒充分配；FIFO/LIFO/比例/到达顺序猜原事实；跨 complete 挪上限；用 Ledger/Balance 反推分配；给 refund 单独落 B 形成混合双真相。
- blocked behavior：现有 root-level `AUTH_REFUND` 只作执行/迁移证据，不得投影 canonical refund ActionFact；当前保持 unsupported/manual，零 refund Entry/RED/Green。
- 当前状态：`DOCUMENT_CARD_CHECKER_PASS / D-MIG-001-R_DECISION_PACKAGE_CHECKER_PASS / D-MIG-001-R-A_ACCEPTANCE_CHECKER_PASS / CANONICAL_PROJECTION_DEFERRED / plan-r2.117`。
- 当时下一入口：`MIG-04_TRANSACTION_WALLET_OWNERSHIP_DOCUMENT_CARD / DOCUMENTATION_ONLY / CODE_FREEZE`；随后形成文档卡并已通过 MIG-04 Checker，当前入口见 Metadata 与 MIG-04 Register，不直接进入代码。

### D-MIG-001-R

- 问题 ID：`D-MIG-001-R`
- 决策主题：当前 root-level authorization refund 缺少逐 complete allocation 时，canonical refund 是继续延期、在现有唯一 durable group 内版本化补证，还是触发全项目 ActionFact 事实源切换。
- 命题类型：项目级耐久事实与迁移时点决策；不重新裁 refund 产品/DSL，不预批 Java/API/DTO/DDL/Mapper 或写链。
- 候选：A `DEFER_CANONICAL_REFUND_PROJECTION`（推荐当前）；B `VERSIONED_EXISTING_GROUP_ENRICHMENT`；C `FULL_PROJECT_ACTIONFACT_SOURCE_SWITCH`。禁止旧动作 A、refund 独立写入的混合第四方案。
- 推荐依据：当前没有真实 Consumer/E4 证明逐 complete 公共查询需求；A 保留既有 root-level 执行并让 canonical query fail-closed，成本最小且符合克制抽象。B 只有在真实 Consumer 能供给稳定 allocation 并接受历史 unsupported/manual 时成立；C 只有多 action kind 共同证明 A 长期不可承接时成立。
- 决策 Owner：Human Owner 最终选择或要求修改；Transaction/Route Owner 提供 durable allocation 可行性，Consumer Owner 提供真实需求和 E4 计划，独立 Checker 复核候选同构、单一事实源和停止线。
- 证据 tuple：MIG-02C 产品/DSL/系分/TDD 合同；当前 refund request/converter/command/lifecycle 与 flow tests 的 authorization-root E2；已接受 `D-MIG-001-A` 的唯一事实源和不可证即停止约束。
- accepted answer / status：`accepted_answer=A / owner_decision=ACCEPTED / status=ACCEPTANCE_CHECKER_PASS / plan-r2.117`。用户明确说明：发卡行或上层业务不保证提供退款来源，`wind-funds` 不能强行关联；独立 Checker 判定 `PASS / 0 P0-P2`。
- red_lines：根级累计、Ledger/Balance 或顺序反推分配；历史 root-only refund 猜回填；refund 单独新建第二事实源；A/B 按 action fallback；C 只迁移 refund；Checker PASS 外推为实现授权。
- blocked behavior：缺少权威逐 complete 来源时，canonical authorization refund 查询保持空/UNKNOWN，需要逐 complete 事实的 Consumer 继续 unsupported/manual；现有 root-level refund 执行不因选择 A 被删除或升级为 canonical 事实。
- reopen condition：只有真实 Consumer 能提供 authority、稳定 complete identity、显式 allocation、原 route、版本、恢复和历史处置证据时，才重开项目级决策；B/C 不自动生效。B 缺上述任一项即停止；C 缺全 action kind 等价迁移和整版回滚即停止。
- 影响文档：产品 `9.6`、DSL `10.10`、系分 `11.9`、TDD `20.15`、本文件 `8.34`/Decision Register/history/recovery。
- 当前入口：D-MIG 历史决策已进入 MIG-04；当前活动状态见 `MIG-04` Register 与 Metadata，不形成 Entry Card、RED/Green、代码、测试或 Git 授权。

### MIG-03

- 问题 ID：`MIG-03_ACTION_LEDGER_BALANCE_CLOSURE`。
- 决策主题：是否以及如何按稳定 ActionFactRef 机械重查独立 LedgerFact 与 required BalanceTarget 闭合证据，而不合并三个事实 Owner。
- 历史 Entry Card 证据：当时 ActionFact 四类 Provider 投影已 Green，LedgerTransaction/Entry 与余额投影物理链存在，流程测试有联合 E2 断言，但没有 action-scoped closure service/DTO；`FundsActionFactDTO` 明确不证明 Ledger/Balance。
- 历史 Entry Card 定性：`SOURCE_AUDIT_COMPLETE / PHYSICAL_CHAIN_PRESENT / CLOSURE_CAPABILITY_MISSING`。后续 MIG-03 Green、persisted v1 exact-read 和 Clearing 两层验证已经关闭当前范围，本条只保留为实现前理由。
- 实际价值：timeout/restart 去重、逆向前验证、Reconciliation source admission、审计与人工处置能沿原 action identity 判断证据完整性，避免 root/任意 Ledger/current balance 互相冒充。
- red_lines：给 ActionFact DTO 填 Ledger/Balance；用“无 Ledger”推断 proven-zero；first/latest 选事实；自动修复、重做 action/posting 或补余额；Entity/Mapper 外露；物化读库/缓存/事件总线/第二写链；无真实用例先造通用抽象。
- Owner/合同状态：Human Owner 已接受 A；Contract、RED、Green、persisted digest exact-read 与 legacy Clearing 测试迁移均已通过独立 Checker。
- 当前状态：`DONE_FOR_CURRENT_SCOPE / GREEN_INDEPENDENT_CHECKER_PASS`；Action/Ledger/Balance 继续正交，不新增 Public 合并 DTO。
- 下一门：只服从当前 Metadata；新的证据维度必须重新形成 Entry Card，不重复进入 MIG-03 RED。

### CI-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-001

- 问题 ID：`CI-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-001`。
- 单一决策：首个 direct PAY clearing source slice 如何把 caller-selected transaction/detail/ledger tuple 收敛为 stable ActionFact handoff，同时保留 Transaction、Ledger、Balance 和 Reconciliation 四类事实 Owner。
- 候选 A（推荐）：Transaction 投影 action-owned 完整 matched sibling set：principal、唯一 PAYEE、可选唯一 FEE_RECEIVER 的角色、Money、detail refs 与全组唯一 distinct recorded LedgerTransaction ref；Reconciliation 使用 transaction-face + ledger-face 验证 closure，唯一选择 PAYEE/CLEARING credit 且 fee 不误选；Balance 只沿既有本地原子过账不变量证明 commit，不新增 durable per-action balance record。
- 候选 B：Ledger 新增 durable action balance effect evidence；当前没有 Consumer/事故/查询指标支撑新表和写链，延后且不是 fallback。
- 候选 C：保留 caller-selected root/detail/entry tuple；不解决 root 冒充 ActionFact 和事实选择权错位，拒绝。
- 首切范围：仅 direct PAY `primary + proven-full`、merchant `CLEARING` credit 与 clearing source admission；authorization/recovery/adjustment、通用 audit、当前余额重算、数据修复、外部 Consumer E4/L4 均不在范围。
- A 的 Owner：Transaction Owner 只签 recorded refs；Ledger Owner 签 Ledger facts 与原子 projection invariant；Reconciliation Owner 签 source eligibility、closure verification、Gate 和幂等；Consumer 只提供 stable source action identity。
- A 的依赖边界：reconciliation-face 继续只依赖 core，用既有 `StableIdentity` 承载 owner-qualified source ref；reconciliation-impl 可依赖 transaction-face/ledger-face并做边界转换；transaction-impl 不依赖 ledger-face；不新增 governance dependency。
- A 的停止线：需要解析 `attemptRef`、修改 ActionFact 使其宣称 Ledger/Balance、增加新 schema/写链、返回历史伪 current balance、兼容/V2/双入口，或无法从 Transaction durable group 唯一投影完整 sibling refs及唯一 distinct LedgerTransaction 时停止并回到本决策。
- accepted answer / status：`accepted_answer=A / owner_decision=ACCEPTED / acceptance_checker=PASS_P0_0_P1_0_P2_0 / RED_ENTRY_CARD_MAKER_COMPLETE / RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.210`。B/C 未选择且不是 fallback。

### W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-RED-ENTRY-CARD-001

- 目标：用一个反射 Public Contract 测试类证明 accepted A 的 Transaction recorded-evidence surface 与 clearing request hard break 当前均不存在；不写生产或既有测试。
- RED whitelist：仅 `ADD tests/src/test/java/com/wind/funds/reconciliation/contract/FundsActionLedgerClosurePublicContractTests.java`。
- RED contract：两个聚合方法，分别覆盖 query service/DTO/complete sibling/digest 与 request sourceActionFactRef/旧 tuple removal；fresh=`2/2F/0E/0S`，missing target 只能形成 assertion failure。
- immutable：全部生产 Java、现有 tests/fixture、schema、POM、API baseline、Consumer 和五文档既有 A 语义。
- candidate Green closure：Transaction face `ADD=2`，Transaction impl `MODIFY=1`，Reconciliation face/impl `MODIFY=2`；无 schema/POM/Mapper。只作 RED 可闭合证明，尚未授权。
- caller closure：四个现有 clearing caller tests + `FundsDirectTransactionFlowTests` 在后续 surface/behavior card 迁移；当前 RED 不改。
- validation：Java 21 mvn-version、compile、Public Contract、单类测试、fresh XML与独立 Checker；MySQL/PMD 不适用，Git 仍需独立授权。
- current status：`MIG03_RED_INDEPENDENT_CHECKER_PASS / GREEN_ENTRY_CARD_DOCUMENTATION_GRANT_CONSUMED / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；当前由下方 Green Entry Card 接替。

### W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-GREEN-ENTRY-CARD-001

- authority/status：Human Owner “继续推进”仅授权本卡文档；独立 Checker=`PASS_P0_0_P1_0_P2_0`。`plan-r2.214` 为已关闭 Entry Card 历史；后续 Green Execution Grant 已消耗但未通过 Checker，当前入口见下方 Ledger Digest Card 与 Metadata。
- production whitelist：`ADD=2 / MODIFY=3`，精确见系分 11.20.1；没有 Ledger、schema、POM、Mapper、core、Governance 或第六个生产文件。
- immutable evidence：`FundsActionLedgerClosurePublicContractTests.java` SHA-256=`91ccb56ae80446f637b9e3500dc571507e3371001ce5ff33c1de90c10ab3e254`，未来 Green 不得修改。
- test whitelist：`MODIFY=7`，包括 Direct、Clearing、Acquiring、Agent、Gate、共享 Reconciliation fixture和 ClearingSplit 间接 Gate caller；新增测试只为 Direct `+2`、Clearing `+4`。
- exact Green gates：Contract=`2`、focused=`148`、transaction=`178`、reconciliation=`242`、business-flow=`206`，均 `0F/0E/0S`；compile=`21/21`；Public Contract=`315/187/42`。
- hard break：request 只留 tenant + `funds` ActionFact StableIdentity + policy；旧 tuple/旧 Gate detail owner归零；没有 alias/V2/bridge/fallback。
- stop：新文件、计数漂移、分页截断、弱化测试、修改 immutable、需要 Ledger API/schema/POM 或新业务裁决时立即停卡。

### W5-MIG03-LEDGER-PERSISTED-DIGEST-CONTRACT-ENTRY-CARD-001

- 问题 ID：`W5-MIG03-LEDGER-PERSISTED-DIGEST-CONTRACT-ENTRY-CARD-001`。
- 实际问题：MIG03 clearing 已能校验 ActionFact recorded refs 和 Ledger sibling/plan existence，但 Ledger stored digest 是 writer request digest，不是从 persisted facts 可重建的 read-integrity contract；时间精度漂移使当前算法物理不可验证。
- 价值：避免被改写或精度漂移的 transaction/plan/entry 被清分、对账、归档或人工处置当作 Ledger Owner 已证明事实；同时保留 same-key replay 冲突语义。该价值跨 direct PAY、收单、分佣和其他 posting 场景稳定复用。
- accepted design：`A / LEDGER_INTERNAL_NORMALIZE_THEN_VERIFY / VALUE_CONFIRMED`。canonical time=seconds；canonical BigDecimal=strip trailing zeros + plain decimal；唯一 persisted digest v1；writer/read 共用 Ledger internal builder；零 Public API。
- rejected：`B / DDL_TIME_PRECISION`、`C / REMOVE_TRANSACTION_TIME`、legacy/dual digest、回填、Reconciliation duplicate verifier、通用签名平台。
- future production candidate：`MODIFY ledger/impl/src/main/java/com/wind/funds/ledger/impl/LedgerTransactionServiceImpl.java`，`ADD=0 / DELETE=0`。
- future test candidates：`LedgerTransactionServiceImplTests.java`、`LedgerTransactionServiceFactQueryTests.java`、`ClearingSplittableDetailApplicationServiceTests.java`、`ReconciliationGateRequirementBehaviorTests.java`，均只在后续 RED Entry Card 精确冻结后可写。
- immutable：Ledger face/DTO/Entity/Mapper/converter、schema/POM/core、Transaction/Reconciliation production、Justfile、API baseline、Consumer、Git 和 release。
- closed status/history：`DOCUMENTATION_GRANT_CONSUMED / LEDGER_DIGEST_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / RED_ENTRY_CARD_DOCUMENTATION_GRANT_NO / CODE_FREEZE / plan-r2.216`。
- consumed next gate：Human Owner 已授权并形成下一条 `W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-ENTRY-CARD-001` Register；当前状态只见下一条 Register，不自动授权测试或生产代码。

### W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-ENTRY-CARD-001

- 问题 ID：`W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-ENTRY-CARD-001`。
- authority：Human Owner “推进吧”只授权五文档文件卡；`RED_EXECUTION_GRANT_NO`。
- test whitelist：`MODIFY=4`，精确路径见系分 11.22；无 ADD/DELETE。
- exact failures：8 个 method invocations，分别命中 legacy、canonical round-trip、read tx/plan/entry tamper、clearing tx/plan/entry tamper；稳定 labels 见系分 11.22.2。
- exact counts：focused `56/8F/0E/0S`；ledger `65/5F/0E/0S`；reconciliation `245/3F/0E/0S`；transaction `178/0`；business `206/0`。
- immutable：全部 production、schema/POM/Justfile/API baseline、Contract SHA 与白名单外 tests/fixtures；RED 后四个白名单测试也 immutable。
- candidate Green：仅 `MODIFY ledger/impl/src/main/java/com/wind/funds/ledger/impl/LedgerTransactionServiceImpl.java`；不授权当前写入。
- closed status/history：`LEDGER_DIGEST_RED_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / RED_EXECUTION_GRANT_CONSUMED / RED_EXECUTION_COMPLETE / RED_INDEPENDENT_CHECKER_NOT_PASS_P1_1 / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.218`。
- consumed next gate：Human Owner 已授权 RED，执行事实与唯一 Checker P1 已进入下一条 Register；不自动授权 Green 或生产代码。

### W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-EXPANDED-GATE-REWORK-001

- 问题 ID：`W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-EXPANDED-GATE-REWORK-001`。
- authority：Human Owner “推进吧，顺便做价值分析”只授权五文档返工与独立 Checker；Java、测试、DDL、API、Consumer、Git 和 Green 均未授权。
- accepted value：`VALUE_CONFIRMED / EVIDENCE_LAYERING_ONLY`；不改变 persisted digest 产品/DSL/系分合同。
- owned behavior gate：focused `56/8F/0E/0S`；non-assembler Ledger 同状态五份 fresh XML 合计 `50/5F/0E/0S`，Green 前须 fresh 执行精确组合；reconciliation `245/3F/0E/0S`；transaction `178/0`；business `206/0`。
- environment observation：assembler 当前 `15/0F/15E/0S`，仅同根 Mockito/ByteBuddy self-attach 可被分类为环境阻断；它不是 PASS，也不进入本切片 failure allowlist。
- immutable：四个 RED 测试、Contract SHA、全部生产、schema/POM/Justfile/API baseline/Consumer；不增加兼容或第二 helper。
- closed status/history：`LEDGER_DIGEST_RED_EXPANDED_GATE_REWORK_INDEPENDENT_CHECKER_PASS / GREEN_ENTRY_CARD_DOCUMENTATION_GRANT_CONSUMED / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.220`。
- consumed next gate：Human Owner 已授权 Green Entry Card 文档，当前状态只见下一条 Register；不自动授权生产代码或 Green 执行。

### W5-MIG03-LEDGER-PERSISTED-DIGEST-GREEN-ENTRY-CARD-001

- 问题 ID：`W5-MIG03-LEDGER-PERSISTED-DIGEST-GREEN-ENTRY-CARD-001`。
- authority：Human Owner “做一轮价值分析 然后推进”只授权五文档 Green Entry Card 与独立 Checker；`GREEN_EXECUTION_GRANT_NO`。
- production whitelist：仅 `MODIFY ledger/impl/src/main/java/com/wind/funds/ledger/impl/LedgerTransactionServiceImpl.java`；`ADD=0 / DELETE=0`。
- implementation closure：normalize -> materialize final identities -> three persisted v1 builders -> atomic persist -> reload verify；existing replay 绑定既有身份并使用同一 builders；exact read fail-closed。
- immutable tests：四个 RED SHA=`a897a565... / eed2f4f5... / 07555f3d... / 95d6126c...`，Contract SHA=`91ccb56a...`。
- exact Green：focused `56/0`、non-assembler Ledger `50/0`、reconciliation `245/0`、transaction `178/0`、business `206/0`、compile `21/21`、Public Contract `315/187/42`；assembler 环境边界不变。
- exclusions：test/schema/POM/Justfile/API/DTO/Entity/Mapper/converter/core/Transaction/Reconciliation/Consumer/Git/release；legacy/dual digest、backfill、second helper/service。
- current status：`LEDGER_DIGEST_GREEN_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.223`。
- next gate：Human Owner `W5-MIG03-LEDGER-PERSISTED-DIGEST-GREEN-EXECUTION` Grant 决策；不自动授权生产 Green。

### W5-MIG03-LEDGER-PERSISTED-DIGEST-EXACT-READ-RED-COVERAGE-REWORK-001

- 问题 ID：`W5-MIG03-LEDGER-PERSISTED-DIGEST-EXACT-READ-RED-COVERAGE-REWORK-001`。
- authority：Human Owner 已授权且 Grant 已消费；生产 Green 仍未授权。
- executed test whitelist：仅 `MODIFY LedgerTransactionServiceFactQueryTests.java`；其他四个 RED/fixture 文件与全部 production immutable。
- exact behavior：三个参数化 invocation 内分别聚合 transaction by-id/by-sn、plan aggregate/exists、entry by-id/by-sn/query；每个只保留一个 stable-label failure。
- exact counts：fact-query=`8/3F/0E/0S`，focused=`56/8F/0E/0S`，non-assembler Ledger=`50/5F/0E/0S`，Reconciliation=`245/3F/0E/0S`，Transaction=`178/0F/0E/0S`，Business=`206/0F/0E/0S`；failure 原因仍只为 persisted integrity guards 缺失。
- current status：`EXECUTION_COMPLETE / LEDGER_DIGEST_EXACT_READ_RED_COVERAGE_REWORK_INDEPENDENT_CHECKER_PASS / LEDGER_DIGEST_GREEN_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.223`。
- next gate：Human Owner `W5-MIG03-LEDGER-PERSISTED-DIGEST-GREEN-EXECUTION` Grant；Checker PASS 不自动授权生产 Green。

### MIG-04

- 问题 ID：`MIG-04_TRANSACTION_WALLET_OWNERSHIP`
- 决策主题：Transaction、Wallet、Ledger 与 Host/Consumer 的 facade、编排和只读事实分别归哪个 Owner，以及如何在不建立兼容桥的前提下一次切换。
- 命题类型：跨模块所有权和破坏式迁移文档卡；不新增产品动作、DSL、Public API、DTO、DDL、Mapper 或实现。
- 目标结构：Wallet 持账户/责任/支付工具/控制准入；Transaction 持资金动作与生命周期编排；Ledger 持账务/余额事实与窄读；Host/Adapter 持场景与外部协议。
- 处置：PaymentInstrument transaction facade 目标归 Transaction、Public 可见性待真实 Consumer；Spend control 协调器内聚 Transaction；Wallet Ledger wrapper/profile 等 MIG-05 后删除；FundsTransactionQuery 留 Transaction；Benefit facade 等 MIG-08 E4。
- 证据 tuple：`wallet-face` 接口与 `transaction-impl` 实现/调用关系；三个 Transaction `LedgerQueryService` 生产调用点；Wallet initializer 与 Transaction settlement 的 `LedgerProfileService` 调用；`FundsModuleDependencyBoundaryTests` 的现有依赖红线。
- red_lines：`transaction-impl -> ledger-face` 先行；Wallet alias/bridge/双 Bean；旧新 DTO/双读/双写；无 Consumer 仍保留 Public facade；把 Benefit/rail 策略泛化进 Funds；删除未切换调用方。
- 当时 blocked behavior：PaymentInstrument 无真实生产 Consumer、MIG-05 Ledger contract 未闭合、MIG-08 可部署 Consumer E4/实际调用切换未闭合时，对应原子组保持未迁移；不以中间层规避 blocker。MIG-05 现已 Checker PASS，其他 blocker 仍按当前 Metadata 与后续 Register 判断。
- 回退：同一迁移组按版本整体切换/整体回退；旧入口只有零生产引用且目标回归、边界与 API baseline 通过后才删除。
- Owner：Transaction/Wallet Owner 共同签模块归属；Ledger Owner 签 MIG-05 窄读；各 Consumer Owner 签真实调用与 E4；架构 + Consumer Checker 独立复核。
- accepted answer / status：本卡没有 A/B/C 产品取舍；`status=DOCUMENT_CARD_CHECKER_PASS / plan-r2.119`。Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，只确认归属、调用事实、原子切换和停止线完整。
- 当时下一入口：`MIG-05_LEDGER_INTERNALIZATION_DOCUMENT_CARD / DOCUMENTATION_ONLY / CODE_FREEZE`；该入口现已完成 Checker，当前入口见 Metadata 与 MIG-05 Register。
- 当时停止线：不直接形成 MIG-04 实现 Entry Card；MIG-05 必须先形成并复核纯文档卡。该历史条件后续已关闭，MIG-04 已在 r2.232 完成并通过独立 Checker。

### MIG-05

- 问题 ID：`MIG-05_LEDGER_INTERNALIZATION`
- 决策主题：Ledger read/profile 如何回归 Ledger Owner，以及 Transaction ActionFact 与 Ledger evidence 如何在不建立第二事实源的前提下协作。
- 命题类型：跨模块内部契约、事实 Owner 与迁移文档卡；不新增产品动作或预批 Java/API/DTO/DDL/Mapper。
- 候选：A `SAME-STORE_SOURCE-OWNED_NARROW_READ`（已按用户“按建议推进”形成推荐设计）；B `MATERIALIZED_LEDGER_EVIDENCE_READ_MODEL`（无性能/隔离证据，DEFER）；C `KEEP_WALLET_LEDGER_BRIDGE`（错误 Owner，REJECT）。
- 目标结构：ActionFact 继续由 Transaction 对唯一 durable action group 按需投影；Ledger 目标上通过 core/internal 方向提供 action-scoped evidence，并在内部拥有 profile、posting plan、LedgerTransaction/Entry 与 Balance projection；Wallet 不包装 Ledger，Transaction 不直连 `ledger-face`。当前不存在该 core read port，必须先过独立 Contract Inquiry，不能把既有写端口冒充读契约。
- 证据 tuple：Wallet `LedgerQueryService`/DTO 与 `DefaultLedgerQueryService` 浅包装；direct/authorization converter 与 balance adjustment audit 三个生产调用；Wallet initializer 与 Transaction settlement 的 `LedgerProfileService` 调用；Reconciliation 有真实 Ledger face 调用、Governance 只有空 POM 依赖；W3-01~03 internal 写端口、事务、provenance 和完成维度合同；官方行业资料核验日期 `2026-08-17`。
- 实际价值：同 identity 恢复、Consumer 脱离内部 root/detail、原事实与 route 追溯、跨版本稳定查询和审计；不提供业务状态、外部 finality、余额完成或通用报表结论。
- red_lines：新表/独立读库/事件总线无指标先行；Public 通用 Ledger search；Transaction 直依赖 ledger-face；Wallet bridge/alias/双 DTO/双读；profile policy 复制；Consumer 拼 posting；projector 猜业务、Money、route、refund allocation 或 finality。
- blocked behavior：当前不改代码。未来切换前任一调用方不明、唯一 evidence/事务/逆向不闭合或 API baseline 无法原子更新时，保持现路径和唯一写链，不加兼容桥。
- 验证：未来同批覆盖三个 Transaction 调用点、Wallet initializer、Transaction settlement、posting/entry/Balance 本地原子、逆向 provenance、timeout/restart、Wallet 旧引用与 Governance 空依赖归零、Reconciliation 读取不变、依赖守卫和 API baseline。
- Owner：Ledger Owner 主责；Transaction/Wallet Owner 签调用与依赖边界；架构 Checker 独立复核；性能读模型只有真实 Consumer 指标出现时重开。
- accepted answer / status：`selected_direction=A / status=DOCUMENT_CARD_CHECKER_PASS / plan-r2.121`。独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`；不等于物理 port、Contract Inquiry 或实现授权。
- 当时下一入口：MIG-06 继续 `BLOCKED_BY_HOST_EVIDENCE`；当时机械进入下一可行动纯文档卡 `MIG-07_RECONCILIATION_STAGE_DOCUMENT_CARD`，当前活动状态见 MIG-07 Register，不直接进入实现。
- 停止线：`DOCUMENTATION_ONLY / CODE_FREEZE`；Java、测试、Public API/DTO/DDL/Mapper、新写链、Consumer、Git、HOST/L4、enable/release/production 均未授权。
- 当前 superseding 状态：MIG-05A~D 已分别完成原 Ledger 引用解析、profile ownership、extension surface collapse 与高阶 posting command/稳定 identity，最终为 `PROVIDER_GREEN_CHECKER_PASS / CURRENT_SCOPE_COMPLETE / plan-r2.254`；目标通过现有窄边界实现，没有新增当时不存在的 core read port。新的 MIG-05 切片必须有真实 Public low-level 写旁路或 Consumer 缺口，不自动创建 MIG-05E。

### CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001

- 问题 ID：`CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001`。
- 单一命题：direct/authorization 逆向用例构造指令时，由谁解析原动作已经持久化的 `ledgerTransactionSn`。
- 独立性：本题只裁物理引用解析，不重裁 ActionFact/LedgerFact/Balance 正交关系，不同时裁 Ledger profile、建账/posting admission 或通用 audit query。
- 一手证据：`FundsTransactionDetail` 已持久化 tenant、event、state、Money、reference 和 ledger transaction SN；`FundsTransactionQueryService` 已能读取原交易 details 与 route snapshot；`DefaultLedgerPostingAssembler` 已复验 reference ledger transaction、原 funds transaction、replay route leg、plan 与 entries；balance adjustment audit 仅有测试 Consumer。
- 候选 A：`TRANSACTION-FACT-RESOLVED-LEDGER-REF`，Transaction Application/Service 用例边界从自有 durable details 解析唯一 ref，converter 只映射结果，Ledger posting 再验证；`ACCEPTED / ACCEPTANCE_CHECKER_PASS`。
- 候选 B：`CORE-ACTION-LEDGER-EVIDENCE-READER`，新增 core internal reader；当前缺多 Consumer 与不可替代读模型证据，`not selected / not fallback`。
- 候选 C：Transaction 直依赖 `ledger-face` 或保留 Wallet bridge；违反强制依赖方向或保留错误 Owner，`rejected / not selected / not fallback`。
- 共同不变量：按 source root/event、frozen replay leg、route participant coverage 与 `SUCCEEDED` details 完整校验后，distinct 非空 ledger ref 必须恰为 1；缺失、多引用、wrong tenant、mixed event/state/ref 或 route/participant 不一致为 UNKNOWN/fail-closed，不取第一条、不换 identity、不创建第二动作或 posting。Transaction 引用不证明 Ledger/Balance，Ledger 继续独立闭合。
- 拆片边界：A 的首个未来切片只覆盖两个承重逆向用例在 Transaction Application/Service 边界的解析、converter 被动映射、Wallet Ledger wrapper 零引用和无 Consumer audit 处置；profile 另进 `MIG-05B-LEDGER-PROFILE-OWNERSHIP`。需要新 port/DTO/DDL/Mapper、Ledger search、converter 查询/业务判断或兼容层即停止并重开决策。
- Owner/status：Human Owner 已选择 A；Transaction/Ledger Owner 分别签解析与再校验；Decision Package Checker 与 Acceptance Checker 均为 `PASS / 0 P0 / 0 P1 / 0 P2`；`accepted_answer=A / owner_decision=ACCEPTED / B_C=NOT_SELECTED_NOT_FALLBACK / status=ACCEPTANCE_CHECKER_PASS / plan-r2.154`。
- evidence tuple：产品 9.8.5、DSL 10.12、系分 11.11.6、TDD 20.17.5、OpenSpec 8.45；当前源码与调用清册只作 E2 事实，不冒充实现授权。
- red lines：Public 通用 Ledger search、单实现 speculative port、`transaction-impl -> ledger-face`、Wallet bridge、first/latest、LedgerFact 嵌入 ActionFact、profile/audit 偷并首切、兼容 facade/双读写。
- 当时下一入口：`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-ENTRY-CARD / ENTRY_CARD_REQUIRED / DOCUMENTATION_ONLY / CODE_FREEZE`；该入口已由下方独立 Entry Card 接替。

### W5-MIG05-TRANSACTION-LEDGER-REFERENCE-ENTRY-CARD

- 任务 ID：`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-ENTRY-CARD`。
- accepted input：`CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001-A_ACCEPTANCE_CHECKER_PASS`。
- execution owner：未来 RED/Green 均由 `wind-funds` 唯一写入 Owner 执行；本卡只冻结权限，不形成第二状态源。
- production whitelist：唯一来源为系分 11.11.7，`3 MODIFY + 9 DELETE + 0 ADD`。
- RED whitelist / Green test migration：唯一来源为 TDD 20.17.6，`3 RED MODIFY + 4 Green mechanical MODIFY + 1 DELETE`。
- evidence tuple：产品 9.8.6、DSL 10.12、系分 11.11.7、TDD 20.17.6、OpenSpec 8.46、当前源码/caller closure 与 live manifest。
- accepted answer / owner decision：本卡不新增产品决策；A 已接受。`entry_card_status=INDEPENDENT_CHECKER_PASS / red_execution=COMPLETE / red_checker=PASS / green_execution_grant=CONSUMED_AND_PAUSED / green_checker=NOT_PASS_P0_0_P1_2_P2_0`；四个旧测试合同问题由下方返工卡承接。
- red lines：新增任何未列文件、类型、字段、依赖、compat layer、converter query、Ledger search、profile、Consumer 或宿主门禁；命中即停止并返回 Human Owner。
- 历史入口：`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-GREEN-EXECUTION / RED_EXECUTION_COMPLETE / RED_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_YES` 已执行并因独立 Checker NOT PASS 暂停；当前入口由下方无兼容测试合同返工卡接替。

### W5-MIG05-TRANSACTION-LEDGER-REFERENCE-GREEN-CONTRACT-REWORK-ENTRY-CARD

- 任务 ID：`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-GREEN-CONTRACT-REWORK-ENTRY-CARD`。
- accepted input：`CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001-A_ACCEPTANCE_CHECKER_PASS / ENTRY_CARD_INDEPENDENT_CHECKER_PASS / RED_INDEPENDENT_CHECKER_PASS / GREEN_INDEPENDENT_CHECKER_NOT_PASS_P0_0_P1_2_P2_0`。
- 单一目标：只纠正四个旧 flow test 对 Transaction/Ledger Owner、未引用 Ledger 行和带费 `PAY` route 的错误断言，不改变 A、生产实现、Public 契约或资金行为。
- future test whitelist：仅 `FundsDirectTransactionFlowTests.java`、`FundsAuthorizationTransactionFlowTests.java` 两个 `MODIFY`；架构测试、fixture、生产文件和其他测试不可写。
- expected result：同一聚焦 slice 从 `164/4F/0E/0S` 收敛为 `164/0F/0E/0S`；missing Ledger/successor pollution 均由 Ledger fail-closed 且零副作用，额外未引用 Ledger 行不改变 selected ref，带费 `PAY` 原 route 保持 `PAY + FEE` 并只回放 principal。
- red lines：不考虑兼容；禁止旧中文错误文案适配、Ledger 宽查询、`ledger-face` 依赖、bridge/facade/V2、双读、fallback、catch-and-relabel、新文件或行为弱化。
- owner/status：Human Owner 已完成无兼容测试合同返工授权；两个 flow test 与独立 FeeFlow 修正均已执行并通过独立 Checker，`TEST_CONTRACT_REWORK_INDEPENDENT_CHECKER_PASS / GREEN_INDEPENDENT_CHECKER_PASS / plan-r2.162`。
- historical next entry：当时为 `W5-MIG05-NEXT-SLICE / ENTRY_CARD_REQUIRED / CODE_FREEZE`；旧 test rework 与 Green Grant 已耗尽，当前入口见下方 `CI-MIG05B-LEDGER-PROFILE-OWNERSHIP-001`。

### CI-MIG05B-LEDGER-PROFILE-OWNERSHIP-001

- 问题 ID：`CI-MIG05B-LEDGER-PROFILE-OWNERSHIP-001`。
- 单一命题：Ledger profile 定义、required-ledger 初始化和 Transaction profile 预检查应如何归位。
- 一手证据：Wallet face 五个 profile/initializer 类型与 Wallet impl 两个默认实现；Funding/Credit 两个生产 initializer caller；Transaction settlement 唯一 profile query caller；LedgerService 已有 create/query；capte-domain 生产只消费 profile code，两个测试宿主装配默认实现。
- 候选 A：`LEDGER-SERVICE-CONTROLLED-INITIALIZATION`，复用现有 LedgerService，profile catalog 内部化；`ACCEPTED / ACCEPTANCE_CHECKER_PASS`。
- 候选 B：`DEDICATED-LEDGER-SUBJECT-ADMISSION-SERVICE`，新增独立 service；`not selected / not fallback`。
- 候选 C：`MOVE-PROFILE-READ-SURFACE-AS-IS`；泄露内部配置并保留 Transaction 错误预检查，`rejected / not selected / not fallback`。
- 共同不变量：仅 profile code 是 Public 账户事实引用；core profile spec 与 Wallet profile/initializer surface 一次删除；profile catalog/items/版本解释归 Ledger；Wallet 只提交 admitted subject/account facts；Transaction 不读 profile、不依赖 ledger face、不自动建账；初始化与 posting/admission 复用同一 catalog integrity guard；同一物理 bucket key 内并发 winner 回读及异 profile/version/catalog 语义冲突，不同 subject/currency/effective period 独立成功，整组事务回滚；无兼容、双 Bean/DTO、schema 或第二事实源。
- evidence tuple：产品 9.8.8、DSL 10.12.2、系分 11.11.8、TDD 20.17.8、OpenSpec 8.48 与当前源码/caller closure。
- red lines：在 Decision/Acceptance 前形成 Entry Card；新增 registry/factory/policy engine；Transaction 依赖 ledger face；继续公开 profile items；按 Consumer fallback；偷并 PlatformFundingAccountRole/route 或 MIG-09。
- Owner/status：Human Owner 已选择 A；`accepted_answer=A / owner_decision=ACCEPTED / B_C=NOT_SELECTED_NOT_FALLBACK / status=ACCEPTANCE_CHECKER_PASS / plan-r2.166`。
- next entry：该入口已由下方 `W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-ENTRY-CARD` 成包接替；本条只保留 Acceptance 历史，不构成执行授权。

### W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-ENTRY-CARD

- Task ID：`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-ENTRY-CARD`。
- accepted input：`CI-MIG05B-LEDGER-PROFILE-OWNERSHIP-001-A / ACCEPTANCE_CHECKER_PASS`；B/C 未选择且不是 fallback。
- exact surface：`LedgerService.initializeRequiredLedgers(InitializeSubjectLedgerRequest)` 返回 `void`；一个 ledger-face request、一个 ledger-impl concrete catalog；仓内 `ADD=2 / MODIFY=8 / DELETE=9`，详见系分 11.11.9。
- exact RED/testing：七组 RED 与未来 Green 测试迁移白名单见 TDD 20.17.9；contract/behavior assertions 不得削弱，legacy test 只允许 import/Bean/call setup 机械迁移。
- Consumer boundary：capte-domain 两个测试宿主另行授权迁移；生产只用 `LedgerProfileCode`，不保留旧 profile/initializer API。
- workspace tuple：HEAD=`eb12091819152fcec529f9453b48755f3aa2c999`；default=`175/4e0128674d9cf43799ffbbe34403b785b3ee4a961301be845e2d38dcbbf3d441`；`-uall=181/15c2ae90b3d9eddb6c1707a2c0d943c4be322dcc53feb9b6a16f5969a86109f6`；staged empty；diff-check PASS。
- Owner/status：`maker=COMPLETE / independent_checker=PASS_P0_0_P1_0_P2_0` 仅指原 Entry Card；后续 `plan-r2.169` RED Checker 为 `NOT_PASS_P0_0_P1_2_P2_1`，该 RED Grant 已消耗且未准出。
- stop line：Human Owner 明确 Grant 前不得进入 RED；未来 RED/Green 任一发现未列文件、第二 service/catalog、兼容层、schema/POM 或 Consumer 生产改动，立即停止并重冻。
- next entry after PASS：当时进入 `plan-r2.169` RED；现已由下方 surface/behavior 三检查点返工卡接替，不构成当前授权。

### W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-SURFACE-BEHAVIOR-ENTRY-CARD-REWORK

- Task ID：`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-SURFACE-BEHAVIOR-ENTRY-CARD-REWORK`。
- accepted input：A 与 Acceptance Checker 不重开；原 Entry Card 的文件/caller closure 保留；输入新增 `RED_INDEPENDENT_CHECKER_NOT_PASS_P0_0_P1_2_P2_1`。
- correction：把原子 breaking release 拆为三个不可发布检查点：`CONTRACT_SURFACE_OWNERSHIP_MOVE -> BEHAVIORAL_RED -> BEHAVIORAL_GREEN`。检查点分开授权和复核，但最终仍是无兼容的单一发布结果，不存在旧新并行。
- evidence：fresh baseline=`69/0F/0E/0S`、RED=`70/7F/0E/0S`、compile=`21/21`；7 个 failure 均为 assertion 且 `errors=0`，但 RED-002/003 共享 request 缺失根因，RED-004~006 矩阵不完整。
- value：将结构归位、缺失行为证据和最终实现分别证明；防止“类不存在”冒充并发/原子性已经测试，同时避免在真实流程层重复穷举 catalog 字段。
- Owner/status：返工卡与 surface execution 均为 `maker=COMPLETE / independent_checker=PASS_P0_0_P1_0_P2_0`；Behavioral RED 为 `maker=COMPLETE / independent_checker=PASS_P0_0_P1_0_P2_1_ENVIRONMENT_RESIDUAL`。surface/RED Grant 均已消耗并关闭；`plan-r2.173` Green Grant 也已消耗且 Checker 未准出，当前状态见下方 Green Entry Card 返工记录。
- 当时 next entry after surface PASS：只允许 Human Owner 决定 `MIG05B-BEHAVIORAL-RED-EXECUTION-GRANT`；该门及后续首轮 Green 门均已完成关闭，不构成当前授权。

### W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-GREEN-ENTRY-CARD-REWORK

- Task ID：`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-GREEN-ENTRY-CARD-REWORK`。
- accepted input：A、surface 与 Behavioral RED 不重开；输入新增 `MIG05B_BEHAVIORAL_GREEN_INDEPENDENT_CHECKER_NOT_PASS_P0_0_P1_3_P2_0`。
- correction：只把共享 catalog guard、catalog-valid fixtures 与测试类级真实事务装配冻结为最小 rework；不新增能力、接口、表、抽象或兼容路径。
- whitelist/evidence：生产 `3 MODIFY`、测试 `11 MODIFY`，精确路径与责任见系分 11.11.11；不可修改证据、24 个直接子类 caller closure 与 `63/0F/0E/0S` 门槛见 TDD 20.17.11。
- Owner/status：`maker=COMPLETE / GREEN_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_PASS / GREEN_REWORK_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.175`。
- next entry：只允许 Human Owner 决定 `W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-GREEN-REWORK-EXECUTION-GRANT`；不得继承此前 Green Grant。

### W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-DOC-ENTRY-CARD-REWORK-001

- accepted input：Human Owner 授权仅做外部资金腿方向文档设计与 Checker，并明确不考虑兼容；`plan-r2.175` 候选的 `63/1F/16E` 共同暴露 `FUND_IN` posting 不平衡。
- stable contract：source/target 是经济端点；`EXTERNAL_IN=INCREASE/INCREASE`、`EXTERNAL_OUT=DECREASE/DECREASE`、internal leg 与无 replay ref 的 ordinary `RESTORE/RELEASE=DECREASE/INCREASE`；仅带非空唯一 `replayRefLegId` 的 reverse-class replay/reversal 精确反向 original posting entries，非反向 successor 按当前稳定 leg；adjust 使用显式 effect。
- ownership：Route 形成 leg/snapshot；Ledger assembler 唯一解释 balance effect 与 entry side；上游 adapter 负责 external authority/finality。禁止 Ledger 读取 rail/业务状态或按 event string 决策。
- exact future whitelist：生产 `1 MODIFY / 0 ADD / 0 DELETE`，测试 `16 MODIFY / 0 ADD / 0 DELETE`，详见系分 11.11.12；其他生产、测试、API、schema、build、Consumer 全部不可修改。
- no-compat：不允许 bridge、V2、alias、旧新双路径、fallback、方向策略 SPI/registry/factory 或第二 assembler。
- initial status（已关闭历史）：`maker=COMPLETE / independent_checker=PASS_P0_0_P1_0_P2_0 / RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.177`；首轮 ordinary `RESTORE/RELEASE` 缺口已最小关闭。
- execution result：Human Owner 后续授权 RED；assembler 四个方法 fresh=`4/2F/0E/0S`，15 个 signed-CASH caller 完成静态迁移和编译。独立 Checker=`NOT_PASS_P0_0_P1_1_P2_0`，唯一 finding 为 RED/Green 动态证据阶段矛盾。
- rework result：`W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-RED-ENTRY-CARD-REWORK-001 / maker=COMPLETE / RED_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_PASS / plan-r2.178`。Assembler 四方法 RED 动态门与 15 caller Green 硬门经独立 Checker判定 `PASS_P0_0_P1_0_P2_0`；方向合同与白名单不变。
- then entry（已关闭历史）：`W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-GREEN-EXECUTION-GRANT / plan-r2.179`。该 Green Grant 已消耗且未准出；当前只见下方证据返工 Register 与 Metadata。

### W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-GREEN-EVIDENCE-ENTRY-CARD-REWORK-001

- evidence：assembler 四方法=`4/0F/0E/0S`；15 caller=`235/79F/2E/0S`；分类为 profile fixture `59` + 旧 signed-CASH `20` + `FUNDING_BALANCE_ADJUST` `2`。
- accepted contract：外部腿 direction 与单文件生产候选不重开；不放宽 catalog，不增加兼容/V2/fallback，不把 explicit adjust 并入外部腿。
- exact future test whitelist：系分 11.11.13 的 `8 MODIFY / 0 ADD / 0 DELETE`；任何第 9 文件、shared support/catalog/生产修改都必须停止重冻。
- checkpoints：首先 `235/1F/1E/0S`，且 `1E` 明确定性为业务缺口而非环境错误；然后独立 `FUNDING_BALANCE_ADJUST` Entry Card，其闭合后才恢复方向 Green 并要求 `235/0F/0E/0S`。
- historical final status：`test_rework_execution=COMPLETE / independent_checker=PASS_P0_0_P1_0_P2_0 / fresh=235_1F_1E_0S / plan-r2.182`；当时下一入口为独立 `W5-MIG05B-FUNDING-BALANCE-ADJUST-ENTRY-CARD`，现已由 `plan-r2.186` 关闭。

### CI-MIG05B-FUNDING-BALANCE-ADJUST-ACCOUNTING-SEMANTICS-001

- 问题 ID：`CI-MIG05B-FUNDING-BALANCE-ADJUST-ACCOUNTING-SEMANTICS-001`。
- 单一命题：平台暂挂 `ADJUSTMENT` 与目标资金余额之间的 increase/decrease，如何在不泄露业务原因和宿主策略的前提下形成平衡、可追溯、可重放的公共 Ledger 事实。
- accepted answer：`A / SIGNED_DEBIT_NORMAL_PLATFORM_ADJUSTMENT`。平台 `ADJUSTMENT` 为 debit-normal signed suspense；increase=`INCREASE/INCREASE`，decrease=`DECREASE/DECREASE`，entry side 由各端 Ledger normal side 推导。Human Owner 已接受；B（平台余额永远取正）与 C（按 route source/target 固定一减一增）均未选择且不是 fallback。
- invariant：相同 action identity + digest 只形成一次 balanced posting；目标非负约束优先 fail-closed；平台允许 signed balance；LedgerTransaction/posting/entry/Balance 可追溯；失败零成功账务效果；既有业务分配和清分事实不可变。
- evidence：`LedgerProfileCatalog` 的 `ADJUSTMENT=DEBIT/allowNegative` 与 `AVAILABLE=CREDIT`，`LedgerBalanceProjectionServiceImpl` 的 normal-side 余额公式，`BalanceControlFundsInstructionRouteResolver` 的 increase/decrease route，以及 8.54 的两个业务缺口。仅为仓内 E2，不冒充生产 Consumer E4。
- exact future whitelist：RED=`3 MODIFY` 测试文件；其中 assembler 测试同步移除伪 `BALANCE_ADJUST` matrix case并调整文件内 Recording seam，两个专用方法承接完整断言。Green=`1 MODIFY DefaultLedgerPostingAssembler.java`；`ADD=0 / DELETE=0`。不新增 Public API/DTO/schema/service/catalog/direction abstraction，不修改 route、wallet service、converter、shared fixture 或 Consumer。
- current status：`owner_decision=ACCEPTED_A / entry_card=INDEPENDENT_CHECKER_PASS / red_execution=COMPLETE / green_execution=COMPLETE / green_checker=PASS_P0_0_P1_0_P2_0 / external_leg_green=COMPLETE / plan-r2.186`。
- next entry after PASS（已关闭历史）：当时进入 `W5-MIG05B-NEXT-SLICE / ENTRY_CARD_REQUIRED / DOCUMENTATION_ONLY / CODE_FREEZE`；现已由下方非负 surface Register 接替，不得继承本轮 Green。

### CI-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-001

- 问题 ID：`CI-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-001`。
- 单一命题：在没有真实 Consumer 和可查询负余额授权事实时，余额调账 Public surface 是否仍应允许调用方自报负余额策略与额度。
- accepted answer：`A / REMOVE_UNPROVEN_NEGATIVE_BALANCE_PUBLIC_SURFACE`。Human Owner 明确选择无兼容删除；不建设授权事实、策略引擎、SPI、DTO 或 schema。
- evidence：Public Request 六字段、transaction-face 六 key、core raw flag、converter/route/posting 调用链；route 只校验完整性；仓内与 `capte-domain` 无生产 Consumer或行为测试。Ledger internal allow-negative 是独立内部能力，不构成 Consumer 授权证据。
- exact future whitelist：生产/契约 `7 MODIFY / 0 ADD / 0 DELETE`，RED 测试 `3 MODIFY / 0 ADD / 0 DELETE`；精确文件与责任见 8.56、系分 11.11.15、TDD 20.17.15。
- red lines：无 deprecated/V2/alias/bridge/raw fallback；不修改 signed adjustment、Ledger internal allow-negative、schema、Consumer；发现真实 Consumer 或新权威证据即停止重冻。
- current status：`owner_decision=ACCEPTED_A / entry_card_rework=INDEPENDENT_CHECKER_PASS / RED_EXECUTION_COMPLETE / RED_INDEPENDENT_CHECKER_PASS / GREEN_IMPLEMENTATION_VERIFIED / GREEN_EVIDENCE_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_PASS_P0_0_P1_0_P2_0 / BASELINE_CAPABILITY_PAYEE_PROFILE_FIXTURE_REPAIR_CHECKER_PASS_P0_0_P1_0_P2_0 / BASELINE_CORE_API_GOVERNANCE_REBASE_CHECKER_PASS_P0_0_P1_0_P2_0 / FINAL_GREEN_REVERIFICATION_INDEPENDENT_CHECKER_PASS_P0_0_P1_0_P2_0 / CODE_FREEZE / plan-r2.200`。两项仓库基线与最终 Green 复验均已关闭；A、无兼容与七文件实现语义不重开。
- next entry：`W5-REFACTORING-PROGRESS-BASELINE-REFREEZE-001 / DOCUMENTATION_ONLY / EXECUTION_GRANT_YES / CODE_FREEZE`；只读重校 MIG-00~09 与 MIG-03 现状，不得修改源码、测试、脚本或 API baseline。

### MIG-07

- 问题 ID：`MIG-07_RECONCILIATION_STAGE`
- 决策主题：如何把多来源事实归一为可复用的对账输入，由 Reconciliation 独立形成 strict-exact/current-lineage 结果，并让 Stage 安全消费多侧 Gate。
- 命题类型：Reconciliation 公共能力与 Stage handoff 文档卡；不裁 raw source authority、业务/rail policy、责任/损失、Java/API/DTO/DDL/Mapper 或实现。
- accepted answer：`CORE_FIRST / STAGE_BOUNDARY_ONLY / CARRIER_INDEPENDENT_SOURCE / ONE_TO_ONE_STRICT_EXACT / MULTI_PAIR_GATE`。Human Owner 逐项选择 A 并明确“不考虑兼容，按正确设计推进；数据源是稳定抽象，文件只是一种形式”。
- 稳定合同：Source Owner/Adapter 按 Pair Comparison Rule Owner 签收的共同 ComparisonRuleRef 产出 immutable source snapshot/facts；Reconciliation 校验 scope/coverage，并按 comparison identity 对同币 Money、claim kind、economic component、direction、共同 rule ref 与 rule-scoped status 做严格比较；Difference 追加且责任中立；GateRequirement 冻结完整 required pair set；Stage 本地事务 mandatory recheck current lineage 后执行。
- identity/digest：delivery、source fact、comparison identity 分离；semantic digest 与 evidence bundle digest 分离；semantic digest 覆盖 Money/status、claim kind、economic component、direction 和共同 ComparisonRuleRef/version；同事实多 carrier 复用事实并追加 evidence，同 identity 异语义 conflict。
- E2 证据 tuple：`ReconciliationSourceItemInput`、`ReconciliationMatchResultItem`、`ReconciliationRunResultApplicationServiceImpl`、`ReconciliationGateApplicationServiceImpl`、`CreateReconciliationDifferenceRequest` 与 source tests；只证明 caller assertion/run/Difference/单 run Gate 骨架，未证明 Money/status strict-exact 或 multi-pair requirement。
- industry calibration：Stripe reconciliation/Balance Transaction、Adyen transaction/batch settlement reports、Modern Treasury reconciliation rules/manual matching/immutable ledgers；只用于分层校准，不复制厂商对象或 rule engine。
- 规则来源：Stripe、Adyen、Modern Treasury 官方在线文档，仅作行业架构校准。
- 版本或发布日期：在线文档访问快照 `2026-08-17`；未取得 provider contract/API pinned version。
- 适用范围：source abstraction、immutable facts、rule/evidence separation 与 reconciliation layering；不适用于 runtime status mapping、法域、资金责任或 finality。
- 核验日期：2026-08-17
- 确认方：Reconciliation Product Owner 与 System Architecture Owner 仅确认设计参考用途；真实 provider/rail 规则仍由 Host/Adapter Owner 签收。
- red_lines：caller `EXACT_MATCH/RULE_MATCH/trusted` 自证；两侧 Adapter 各自定义同名 ComparisonStatus；principal/fee/tax/FX 或 direction/claim kind 仅凭同金额同状态误配；carrier-specific flow；raw status 进入 Reconciliation；总额相等即 Balanced；first/latest/closest；默认 tolerance/netting/FX；manual 改 Balanced；Difference 自动定责/调账；Gate PASS 冒充全局 finality；stale Gate 重用；bridge/alias/V2/双读写。
- blocked behavior：authority/scope/ComparisonRuleRef/coverage/watermark/identity/Money/status/claim kind/economic component/direction 任一未知、规则过期/多命中或两侧 rule ref 不同，即 run 不 Balanced；required pair 缺失/非 current/有 blocker即 Gate 拒绝；无法重算的历史 assertion 保持 unsupported/manual。
- Owner：Reconciliation Owner 主责 source admission/run/result/Difference/Gate；Source Owner/Adapter 签 raw authority 并按共同 rule 归一；Pair Comparison Rule Owner 签两侧 roles/namespaces、DomainOutcome mapping、comparison semantics、scope/effective period 和 ComparisonRuleRef/version；Stage Owner 签 GateRequirement 和本地执行；Funds/Ledger、Finance/Operations、Consumer Owner 只签各自事实与真实用例。独立 Checker 复核本卡。
- affected docs：产品 `9.9`、DSL `10.13`、系分 `11.12`、TDD `20.18`、本文件 `8.31/8.37/Register/history/recovery`。
- accepted answer / status：`accepted_answer=上述组合 / owner_decision=ACCEPTED / first_checker=NOT_PASS_0P0_2P1_0P2 / rework_checker=PASS_0P0_0P1_0P2 / status=DOCUMENT_CARD_REWORK_CHECKER_PASS / plan-r2.124`。Owner 选择未变，两项 P1 已关闭。
- 当时 next entry：`WAITING_FOR_HOST_AND_DEPLOYABLE_CONSUMER_EVIDENCE`，随后完成 8.38 的可部署 Consumer 盘点。当前活动状态见 Metadata 与 8.39。
- stop line：`DOCUMENTATION_ONLY / CODE_FREEZE`；Java、测试、Public API/DTO/DDL/Mapper、新写链、Consumer、Git、HOST/L4、enable/release/production 均未授权。

### MIG-06/08 Host / Consumer Evidence Inventory（`plan-r2.126` 历史）

- 证据主题：当前是否存在足以解除 MIG-06/08 blocker 的真实可部署 Consumer。
- 盘点范围：当前 `wind-funds` checkout、`capte-domain`、`fincone`、`fincone-issuing`，以及本机 `/Users/wuxp/Workspace/idea/capte` 下 Maven/Java 对 `wind-funds-reconciliation-*` 的引用。
- 事实结论：`wind-funds` 只有 Provider 内部 Stage E2；`capte-domain` 是通用库且 impl 仅测试态，没有 Reconciliation 生产依赖；`fincone` 是 docs-first 权威仓；`fincone-issuing` 只有骨架；本机未发现其他当前 Reconciliation Consumer。
- eligibility：必须具备可部署 composition root、目标依赖、真实 adapter/Stage、数据库/schema/transaction Owner、source/version/scope、timeout/restart、artifact lineage 和 E4/L4 计划；当前无候选同时满足。
- 当时 decision：`NO_ELIGIBLE_DEPLOYABLE_CONSUMER_FOUND / KEEP_L4_WAITING`。这不是产品否决；它只禁止用测试宿主、依赖声明或设计文档冒充可部署证据。
- Owner：Human Owner 提名真实业务宿主；Consumer Owner 提供仓库、版本、部署和恢复证据；Host/Adapter Owner 提供 source/rule/scope；Funds Owner 只复核公共契约与模块边界。
- 历史 status：`EVIDENCE_INVENTORY_CHECKER_PASS / WAITING_FOR_HOST_AND_DEPLOYABLE_CONSUMER_EVIDENCE / plan-r2.126`；独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。当前角色与 Gate 解释由下一个 Register 取代。

### MIG-06/08 Consumer Role Calibration

- 问题 ID：`MIG-06_08_CONSUMER_ROLE_CALIBRATION`。
- 决策主题：如何在没有独立 deployable host 的当前阶段，正确使用实际库 Consumer、设计/模拟 Consumer 和测试宿主证据推进 Reconciliation 重构，同时保持 L4 fail-closed。
- accepted input：Human Owner 明确 `capte-domain` 是当前实际 Consumer，`fincone` 只承担设计和模拟 Consumer；本卡不重新选择 MIG-07 产品方案。
- 角色事实：`wind-funds=PROVIDER`；`capte-domain=ACTUAL_LIBRARY_CONTRACT_CONSUMER + TARGET_RECONCILIATION_E4_HOST`；`fincone=DESIGN_AUTHORITY + SIMULATION_CONSUMER`；`deployable application=FUTURE_L4_OWNER`。
- 证据 Gate：design/simulation -> Contract -> test-host E4 -> deployable-host L4；每层独立关闭，前层 PASS 不外推后层，L4 缺失不反向阻断纯文档 Contract Inquiry。
- first seed：Capte Benefit funding handoff fact vs FundsActionFact，使用共同 ComparisonRuleRef 做 normalized `1:1 strict-exact`；Ledger/Balance 保持独立 pair/证据；只作为 Contract/E4 种子，不进入公共 DSL 场景枚举。
- red lines：不得声称 capte-domain 当前已有 Reconciliation 调用/E4；不得把 fincone 模拟写成 runtime；不得让 Consumer/Host 角色进入 core；不得因 Contract 候选形成 Entry/RED/Green；不得降低 deployable L4 门槛。
- Owner：Human Owner 确认角色；Capte Consumer Owner 与 Funds Owner 关闭 Contract；Fincone Product/Simulation Owner 提供场景与规则输入；未来 Host Owner 关闭 L4。独立 Checker 复核本卡。
- affected docs：产品 `9.10`、DSL `10.14`、系分 `11.13`、TDD `20.19`、本文件 `8.31/8.38/8.39/Register/history/recovery`。
- status：`accepted_input=CONFIRMED / document_card=INDEPENDENT_CHECKER_PASS_0P0_0P1_0P2 / plan-r2.128 / DOCUMENTATION_ONLY / CODE_FREEZE`。
- 当时 current entry：`CI-MIG07-RECONCILIATION-001 / CONTRACT_INQUIRY / CONTRACT_DECISION_PENDING`；该阶段已由 8.40 决策包承接，当前状态见 Metadata。

### CI-MIG07-RECONCILIATION-001

- inquiry/topic：`CI-MIG07-RECONCILIATION-001 / Reconciliation strict-exact computation authority and minimum face contract`。
- question：两侧 source 已归一后，由 Provider、typed matcher + Provider recheck，还是 caller assertion 形成 MatchResult。
- candidates：A=`PROVIDER_COMPUTED_STRICT_EXACT`；B=`TYPED_ASSERTION_WITH_PROVIDER_RECHECK`；C=`CALLER_ASSERTION_PERSISTED`。
- recommendation/rationale：推荐 A。它是唯一单计算权威，能机械比较 Money/status/semantics/rule/coverage；复用现有 Batch/Run/Gate Owner，且不需要 V2、matcher SPI、rule engine 或第二对账内核。
- minimal surface candidate：breaking replace batch scope、含 comparisonProven 的 normalized snapshot facts、provider execute command，以及按 exact stage identity 自动解析唯一 current requirement head 的 Gate check；保留只读 run/match query；Java 名称、DTO、schema 和 transaction 未批准。
- first scenario：Capte Benefit funding handoff fact vs FundsActionFact；Fincone 提供 missing/mismatch/conflict/stale 模拟验收；Ledger/Balance 是独立 pair。
- value gate：设计正确性与跨场景复用已具备证据；E4/L4 尚无证据。当前 GO 仅到 Owner 决策，代码继续 NO-GO。
- evidence tuple：current reconciliation-face/impl source + `capte-domain=ACTUAL_LIBRARY_CONTRACT_CONSUMER/TARGET_E4_HOST` + `fincone=DESIGN_SIMULATION_CONSUMER` + MIG-07 accepted Profile。
- red lines：caller MatchResult/run outcome；carrier-specific source type；同名状态跨 rule 等同或双侧 UNKNOWN 自动相等；Gate caller 自选 requirement/run；开放 blocking policy/optional pair；RULE_MATCH/tolerance/FX/netting/manual engine；old/new API 并存；Difference 自动资金修复。
- blocked behavior：comparisonProven、必要事实、rule、coverage、current requirement/run lineage 或 required pair 任一缺失/冲突时 zero Balanced/zero Gate PASS/zero Stage side effect；保存证据并人工处理。
- Owner：Human Owner 已选择 A；Funds Reconciliation Owner 与 Capte Consumer Owner 后续在独立 Entry Card 签最小 face；Fincone/Pair Rule Owner 只签场景和 normalized semantics；Host Owner 后续关闭 E4/L4。
- accepted answer/status：`accepted_answer=A / owner_decision=ACCEPTED / B_C=NOT_SELECTED_NOT_FALLBACK / decision_package_checker=PASS_0P0_0P1_0P2 / acceptance_checker=PASS_0P0_0P1_0P2 / plan-r2.132`。A 的 Provider 计算权威、Source Adapter 责任、fail-closed、正交证据与拆片边界已通过 Acceptance Checker。
- affected docs：产品 `9.11`、DSL `10.15`、系分 `11.14`、TDD `20.20`、OpenSpec `8.40/Register/history/recovery`。
- 当时 next entry：`W5-MIG07-SOURCE-RUN-STRICT-EXACT-ENTRY-CARD / ENTRY_CARD_REQUIRED`；该卡现已由 8.41 成包，当前状态见 Metadata。

### W5-MIG07-SOURCE-RUN-STRICT-EXACT-001

- card/topic：Source/Run strict-exact breaking contract、Provider 计算、单一表族迁移和 RED 准入。
- accepted product input：`CI-MIG07-RECONCILIATION-001-A / ACCEPTANCE_CHECKER_PASS`；本卡不重裁 A/B/C。
- current E2：source item 仅 ref/digest；caller 提交 quality/strength/difference/severity；Provider 不比较 Money/status/semantics。
- contract candidate：结构化 scope/rule、逐事实 ComparisonRuleRef、carrier-neutral snapshot facts、无 match payload 的 execute command、有限 Provider result kind；batch/reference/comparison rule ref 必须一致；异币固定 `CURRENCY_MISMATCH` 且无差额/较大侧，仅同币 Money mismatch 携带正绝对差额与较大侧；移除 caller assertion、carrier-mixed source type 和 match severity。
- persistence candidate：原位替换既有 batch/source snapshot/source item/run/match 表族，不建 V2、第二写链或 bridge；exact Java/DDL/Mapper 未批准，获准后必须同步生产 create/verify SQL 与 H2 schema。
- test matrix：`MIG07-SR-CONTRACT-001` 与 `MIG07-SR-001~009`，覆盖 public contract、matched/mismatch/missing/coverage/conflict/not-comparable、重放/恢复、运营边界与共享迁移屏障。
- shared barrier：GateRequirement 独立成卡，但最终与 Source/Run 在同一 breaking release 一次切换；Gate 卡关闭前禁止 Source/Run 临时 Green。
- red lines：raw parser、通用 matcher/rule registry、tolerance/FX/netting/manual match、severity/责任推断、兼容 facade/V2/双读双写、Consumer/Stage/资金修复越界。
- Owner/status：Funds Reconciliation Owner + Capte Contract Consumer Owner；Human Owner 已接受唯一精确 surface。`ENTRY_CARD_INDEPENDENT_CHECKER_PASS / SOURCE_RUN_CONTRACT_ACCEPTED / SOURCE_RUN_CONTRACT_ACCEPTANCE_CHECKER_PASS / RED_EXECUTION_GRANT_NO / plan-r2.136`。
- accepted answer：保留现有 Batch/Run/read-only query Owner；用 carrier-neutral normalized facts 替换 source carrier 输入；用 `executeStrictExact(tenantId,batchSn)` 替换 caller-submitted MatchResult；Provider 产出有限 result kind 与 run outcome；只做同表族破坏式迁移，不做兼容层、V2、双写或第二事实源。
- 当时 current entry：`W5-MIG07-GATE-REQUIREMENT-001 / ENTRY_CARD_REQUIRED / DOCUMENTATION_ONLY / CODE_FREEZE`；该卡现已由 8.42 成包，当前状态见 Metadata。

### W5-MIG07-GATE-REQUIREMENT-001

- card/topic：versioned multi-pair GateRequirement、自动 current-head 解析、Stage 事务内 mandatory check 与 consumed evidence。
- accepted product input：`CI-MIG07-RECONCILIATION-001-A / ACCEPTANCE_CHECKER_PASS` 与 Source/Run Contract Acceptance Checker PASS；本卡不重裁 strict-exact 或 Source Adapter authority。
- current E2：caller 选择一个 gate object 与 run result；Provider 只校验单 run current lineage/Balanced 和对象级 Difference；Stage 只保存单一 run ref/digest。
- contract candidate：`GateStageRef` 精确标识一次 Stage action；immutable/versioned Requirement 冻结全部 mandatory `scopeIdentity + pairIdentity + ComparisonRuleRef`；唯一 current/effective head；check/inspect 只接 stageRef 并自动解析全部 current scope+pair runs。
- persistence candidate：Provider 生成 requirement identity；tenant+stageRef+version 唯一；semantic/evidence 双 digest 重放；immutable requirement header + 按 scope+pair 唯一的 rows + stage-scoped current head pointer + Stage 成功事务写入的一条 consumed-evidence snapshot；不持久化可复用 Gate PASS，不复制 Source/Run facts。
- transaction candidate：publish 以 expected-current CAS 推进 head；check MANDATORY 加入 Stage 本地事务并按稳定 scope+pair key 锁 requirement/current runs/blockers；Stage success/funds action/evidence 原子提交，确定性 proven-zero failure 与 UNKNOWN 按既有事实合同分层。inspect read-only。
- caller map：系分 11.16.5 是全部当前 production Gate references 的迁移基线；difference report 内嵌 Gate 与一行 wrapper 删除，其余 check/inspect 映射 exact stage action。
- test matrix：`MIG07-GATE-CONTRACT-001 + MIG07-GATE-001~010`，覆盖 public contract、multi-scope/pair、双 digest Requirement replay/conflict/CAS、current/coverage/rule/blocker、inspect stale、Stage success/failure/UNKNOWN atomicity、restart、Difference lineage 与 shared migration。
- red lines：caller requirement/run list；optional/non-blocking Pair；threshold/policy expression；future-effective scheduler；PASS token；Gate 自动 Difference action/资金修复；兼容 facade/V2/双写；Source/Run 或任一已知 Stage caller单独切换。
- blocked behavior：Requirement/head/Pair/run/rule/coverage/blocker 任一缺失、陈旧、多头或冲突时整体 BLOCKED，零 Stage、Funds、Ledger、Balance 和 consumed evidence 副作用；历史事实只读。
- baseline repair/refreeze evidence：reconciliation 11 个 status 映射与 `FundsTransaction.setStatus`、`ClearingSplittableDetail.setReconciliationDecisionStatus` 两个后续映射修复均已独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前 checkout 为 `HEAD=eb12091819152fcec529f9453b48755f3aa2c999`；默认 porcelain=`59 / 50fde3d7989e13cfa9446ec51c91f4bcb38de0303221c787d0213b669175dbf4`，`-uall`=`60 / bc904d7a6c16a7a35dc448c4d4dddf83445b57ceee3422ce46ba49facef3f1fb`；二者只差同一 untracked directory 的折叠/展开，连续复算稳定，staged 为空，`git diff --check` PASS。
- baseline validation：compile=`21/21`；旧宿主=`85/0F/0E`；纯契约 RED=`8/7F/0E/1P`；联合切片=`93/7F/0E`；reconciliation=`236/0F/0E/0S`。纯契约 RED 只记录当前物理证据，不追认或授予 RED；PaymentInstrument status 映射与 Mockito/ByteBuddy sandbox self-attach 继续独立保留。PMD 未形成 fresh 结果只属于本轮验证记录，不是公共能力 blocker。
- Owners：Stage Owner 签 stage identity/required pairs/requirement version；Pair Rule Owner 签 rule refs；Reconciliation Owner 签 requirement/current-lineage/Gate 判定；各 Stage Owner 签本地事务与 consumed evidence；Human Owner 已接受精确 Contract，独立 Contract Acceptance Checker 已 PASS；当时下一 Owner Gate 只审共同 RED Entry Card且不产生 RED Grant，该 RED 已由 `plan-r2.145` 关闭。
- accepted answer/status：`accepted_answer=GATE_REQUIREMENT_CONTRACT / owner_decision=ACCEPTED / contract_acceptance_checker=PASS_0P0_0P1_0P2 / red_execution_grant=NO / plan-r2.140 / DOCUMENTATION_ONLY / CODE_FREEZE`。
- affected docs：产品 `9.13`、DSL `10.17`、系分 `11.16`、TDD `20.22`、OpenSpec `8.42/Register/history/recovery`。
- 当时唯一入口：`W5-MIG07-SOURCE-RUN-GATE-BREAKING-GREEN-EXECUTION-GRANT / RED_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_NO`；Human Owner 后续仅授权文件卡返工，当前入口见下条 Register 与 Metadata。

### W5-MIG07-SOURCE-RUN-GATE-BREAKING-GREEN-ENTRY-CARD-REWORK-001（历史）

- question：在不修改已接受 Contract、不执行 Green 的前提下，未来一次 breaking Green 可以精确触碰哪些生产/schema/legacy-test 文件，如何逐项关闭七个 RED 并保持全部 direct caller 行为？
- evidence tuple：`HEAD=eb12091819152fcec529f9453b48755f3aa2c999`；稳定 checkout default=`59/50fde3d7989e13cfa9446ec51c91f4bcb38de0303221c787d0213b669175dbf4`、`-uall=60/bc904d7a6c16a7a35dc448c4d4dddf83445b57ceee3422ce46ba49facef3f1fb`、staged empty、`git diff --check` PASS；RED Checker=`PASS/0P0/0P1/0P2`；源码 direct-reference inventory；两个 immutable PublicContract tests；TDD 20.22.5。
- accepted scope：仅 documentation-only Entry Card rework。文件卡允许在 Checker 前补入同一 accepted breaking contract 的机械 direct caller；语义不同、需兼容层、弱化行为或出现新 Contract 决策时必须停止。
- exact file source：TDD 20.22.5 是唯一 ADD/MODIFY/DELETE 清单；每个文件具有 full path、责任、RED 映射和 caller 行为。其余文档不得另建并行白名单。
- immutable evidence：`ReconciliationStrictExactPublicContractTests`、`ReconciliationGatePublicContractTests` 永久不可修改。legacy tests 只迁移 setup/call，不删除、不减数、不弱化业务/资金/账本/只读断言。
- validation：未来仓库级 Green 固定 compile `21/21`、contract `8/0F/0E`、combined `93/0F/0E`、old host `85/0F/0E`、reconciliation `236/0F/0E/0S`、direct callers `12+5+5=22/0F/0E/0S`、diff-check PASS；本仓库只验证 MySQL DDL/schema 契约，不要求真实 MySQL host。Consumer 若采用 MySQL，另在其 E4/L4 验证连接、迁移与事务；任何表外文件或计数下降立即停止。
- red lines：compatibility facade/V2/bridge/dual read-write；修改 immutable tests；表外写入；PaymentInstrument/其他 setter；build/dependency；Consumer/HOST/L4/MIG-09；Git/联网/安装/发布；在本卡中执行 Green。
- owner/status：Human Owner 只授予 `GREEN_ENTRY_CARD_REWORK_GRANT`；`accepted_answer=FILE_LEVEL_CARD_CANDIDATE / status=GREEN_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_NOT_PASS / green_execution_grant=NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.146`。
- Checker/next：最终 `NOT PASS / 0 P0 / 4 P1 / 1 P2`，不得进入 Green；替代入口见下条 Register。

### W5-MIG07-BREAKING-BEHAVIORAL-RED-ENTRY-CARD-REWORK-001

- question：在完全不保留兼容层的前提下，如何让 breaking contract 先形成干净 target surface，再以 accepted 行为 RED 驱动实现，并保证中间态不被误发布？
- accepted scope：只重冻文档执行卡；产品/DSL/系分合同、20.22.5 已审计最终文件集合与所有业务 Owner 不变。
- decision：一个最终 breaking release，三个不可发布检查点：`CONTRACT_SURFACE_HARD_BREAK -> BEHAVIORAL_RED -> BEHAVIOR_GREEN`。旧 class/method/field/caller 在第一步同批归零，禁止 alias/V2/bridge/facade/双读写/fallback。
- behavioral evidence：两个新测试类、19 个顶层方法，逐项映射 `MIG07-SR-001~009` 与 `MIG07-GATE-001~010`；contract tests 永久 immutable，behavioral RED 阶段不迁移 legacy test 断言。
- build evidence：surface 首门为 `just clean-compile + just verify-public-contracts + immutable contract tests`；删除源码不得依赖增量 target。Justfile 只允许表数注释 `21/44 -> 25/48`，recipe 不变。
- blocked behavior：任一命令未到达 Surefire、RED 出现 unexpected error、发现表外文件/新业务决策或需要兼容层时停止；surface/RED 检查点均不可 commit/publish/deploy。
- owner/status：Human Owner 明确不考虑兼容；已关闭历史执行为 `contract_surface_green_execution=COMPLETE / behavioral_red_execution=COMPLETE / green_execution=COMPLETE / status=W5-MIG07-NEXT-SLICE / ENTRY_CARD_REQUIRED / GREEN_INDEPENDENT_CHECKER_PASS / plan-r2.150`。当前活动状态见 Metadata 与 `CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001` Register。
- evidence：fresh clean compile=`21/21`，Public Contract 约规=`types324/models192/enums43`，focused=`27/0F/0E/0S`，reconciliation=`236/0F/0E/0S`，旧宿主=`20/0F/0E/0S`，staged empty，`git diff --check` PASS；工作区 default=`160/b8958afe...`、`-uall=166/96e089d2...`。Green 首轮 `NOT PASS / 0 P0 / 1 P1 / 0 P2`，最小并发修正后最终 `PASS / 0 P0-P2`。
- historical next entry：MIG-07 当时进入 `W5-MIG07-NEXT-SLICE / ENTRY_CARD_REQUIRED / CODE_FREEZE`，现已由 `CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001` 接替；不自动授权任何新行为、实现、Git、HOST/L4 或发布。

### W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CUTOVER-ENTRY-CARD-001

- task/type：`Provider-to-Consumer handoff Entry Card / documentation-only`。
- trigger evidence：`capte-domain` 是实际 library Consumer；production 使用 `wind-funds.version=1.0.1-SNAPSHOT` 并调用 Benefit settle/refund，当前 `CouponBenefitFundingSettlement` 仍以 returned/root transaction existence 判断完成；本机已解析制品包含既有 ActionFact face/impl。
- accepted answer：A。保留 `benefitTransactionSn` 作为原 Funds intent/退款执行引用，以 `FundsActionFact` 作为 settle/refund 动作完成证据；不考虑旧完成判定兼容，不做 root + ActionFact 双读。
- stable contract：settlement 返回后和已有 reference 复用都必须唯一匹配 `primary + succeeded + proven-full`，且 Money/provenMoney、tenant 与 `intentRef` 对齐，已有 `benefitTransactionSn` 不得绕过验证；refund 必须唯一匹配 `recovery/adjustment + succeeded + proven-full`，要求 `intentRef == refund returned transactionSn`，且唯一原事实引用、allocation 与原 settlement ActionFact 对齐。空集合只表示 UNKNOWN，本身不授权重试、逆向或补单；只有 Coupon Owner 已成立且仍有效的业务意图、冻结请求和原 identity 才能独立授权同 identity 恢复。非空冲突 fail-closed。
- Provider boundary：复用 `FundsTransactionQueryService#queryFundsActionFacts`、`FundsActionFactQuery` 与 `FundsActionFactDTO`；不新增 Provider API/DTO/DDL/Mapper/写链。ActionFact 不证明 Ledger、Balance、外部 finality、Reconciliation 或 Coupon 生命周期。
- Consumer responsibility：Coupon Owner 持有核销、出资配置、退款资格、业务状态和 `benefitTransactionSn` reference；Consumer 只在完成 ActionFact 成立后保存/复用完成引用。
- future RED whitelist：`CouponRedemptionApplicationServiceImplTests.java`、`CouponImplContractBoundaryTests.java`、`OrderCouponRedemptionIntegrationTests.java` 三个 existing test files；production/POM/schema/Provider immutable。
- future Green whitelist：仅 `marketing/coupon-impl/src/main/java/com/capte/marketing/coupon/integration/funds/CouponBenefitFundingSettlement.java` `MODIFY`；RED tests immutable。
- exact exclusions：`WalletPaymentParticipant`、其他 Consumer、wind-funds Java/tests、POM、schema、Entity/Mapper、new service/facade/cache/lock、identity parsing、compatibility/fallback、MIG-02C、Reconciliation、HOST/L4、Git、network/install/release/production。
- live Consumer evidence：`capte-domain@ce3c69467745b181f128561a887519bcba2950c7`，manifest=`35 / 877689322b181484735c3b503eff0048e6d63bbcfce31deb448f7088047d2b2a`，staged empty、diff-check PASS；unit test SHA=`730269...ace` 且含既有 LedgerProfileCatalog/ActionFact stubs，integration test SHA=`0c863e...be2d` 且含既有 LedgerProfileCatalog migration，两份 dirty 未来都必须 preserve。
- current limitation：没有 fresh Surefire XML；artifact SHA 只证明本机制品，不证明 source revision lineage；Consumer 代码尚未切换，因此不是 E4 PASS、L4 或 production evidence。
- Owner boundary：wind-funds 记录 Provider contract/evidence response；Capte Human Owner 必须在 `capte-domain` 同仓重冻状态并单独授权 RED/Green，wind-funds 本卡不产生跨仓写入权。
- validation：五文档结构/交叉状态、未来 path existence、stale-active scan、`git diff --check`、独立 Checker；本轮不运行 Maven/测试。
- status：`ENTRY_CARD_INDEPENDENT_CHECKER_PASS / CAPTE_CONSUMER_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.257`；Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。
- next gate：`CAPTE_BENEFIT_ACTIONFACT_CONSUMER_RED_EXECUTION_GRANT` by Capte Human Owner；任何新增文件、不同业务语义、POM/schema 或兼容需求先返回双方 Owner。

### W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-RED-CLOSEOUT-AND-GREEN-REFREEZE-001

- task/type：`Consumer RED closeout + Green Entry Card re-freeze / documentation-only`。
- execution fact：Capte Human Owner 已授权并完成三个既有测试文件 RED；生产、POM、schema、Provider 和其他 Consumer immutable。RED Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。
- fresh RED：application=`57/8F/0E/0S`、boundary=`31/2F/0E/0S`，合计=`88/10F/0E/0S`，原 78 项通过。十个 failure 全部命中冻结缺口，无 unexpected error；Maven lifecycle 可能在 XML 失败时仍返回 `BUILD SUCCESS`，只以 fresh Surefire XML 计数和失败类裁决。
- integration evidence：主类最终=`25/0F/0E/3S`；lineage/seed/recover method-only 分别=`1/0F/0E/0S`，当前 retained recover XML SHA=`a15f4f39129d71f63df66eb1d860811986ee1e3622027c27dd87dcb7bde8c427`。只证明已发布 `1.0.1-SNAPSHOT` 下当前 library 恢复路径，不外推 Provider source revision lineage、L4 或生产。
- immutable RED evidence：`CouponRedemptionApplicationServiceImplTests.java=d78277172a2d76982d5b197d6bfaa9c7cd0f35f902e4833d18c7cc4e55629043`；`CouponImplContractBoundaryTests.java=f3558962efbd9ef73fd649e581c18b3eb8a5f7059b4de9d1775af837775fa614`；`OrderCouponRedemptionIntegrationTests.java=6b1cd39a4852729cd88498232e91795d7be92f3bd97168b665b7a71fea4ed4ca`。
- future Green whitelist：仅 `MODIFY marketing/coupon-impl/src/main/java/com/capte/marketing/coupon/integration/funds/CouponBenefitFundingSettlement.java`，写前 SHA=`c32e6f1df72551ae7a62039362b1d6a62209344e3517ed67ce65473f1f9264b5`；三个 RED tests immutable，`ADD=0 / DELETE=0`。
- Green responsibility：复用既有 `queryFundsActionFacts`，关闭十个 RED；existing reference、settle 返回和 refund root/返回都必须以对应唯一 ActionFact、Money、`intentRef` 与 original ref 证明，不再以主交易存在完成判定。
- Green exact validation：offline compile=`42/42`；focused=`88/0F/0E/0S` 且原 78 项保持；integration main=`25/0F/0E/3S`，lineage/seed/recover 各 `1/0F/0E/0S`；目标组件旧查询完成判断 zero-call；staged empty、stable manifest、`git diff --check`、独立 Checker。
- workspaces before refreeze：wind-funds=`fc6b6e004a32eb1813534de05df7f844cc6edf95 / 78 / 00d618fada22b7c67da9651af28bdde490594194df69c23ab7529f7280cda6f3`；Capte=`ce3c69467745b181f128561a887519bcba2950c7 / 36 / 9726d6a990e49b9a896dd8efeb0f6f465013408749a5326e4060576f440923d7`；两仓 staged empty、diff-check PASS。
- workspaces after documentation write：wind-funds manifest 仍为 `78 / 00d618fada22b7c67da9651af28bdde490594194df69c23ab7529f7280cda6f3`；Capte 仅新增本白名单内系分修改，manifest=`37 / f57383be9efe3b5f6c0ca2efeb669f02d402868bcf9e126ac142a61c0606aee6`；两仓 staged empty、diff-check PASS。
- exact exclusions：任何第二生产文件、测试改动、Provider API/DTO、POM/schema/Mapper、new service/facade/cache/lock、identity parsing、主交易 fallback、compatibility/dual-read、MIG-02C、Reconciliation、HOST/L4、Git、network/install/release/production。
- current status：`CAPTE_CONSUMER_RED_INDEPENDENT_CHECKER_PASS / GREEN_ENTRY_CARD_REFREEZE_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.258`；Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。
- next gate：仅在本重冻独立 Checker PASS 后，由 Human Owner 决定 `CAPTE_BENEFIT_ACTIONFACT_CONSUMER_GREEN_EXECUTION_GRANT`。

### W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-GREEN-CLOSEOUT-001

- task/type：`Consumer Green closeout + state alignment / documentation-only`。
- execution fact：Human Owner 已授权并完成唯一 Capte production Green；`CouponBenefitFundingSettlement.java` SHA=`8b28a049aa44b08fd5d2e99ce25d6ca0c2af43f49ee8927babe5081b76c0f1f3`，Green Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。
- behavior：new/existing settle reference 均验证唯一 primary/succeeded/proven-full、tenant、Money/provenMoney、intentRef 后才保存或复用；refund 前验证原 primary/stored intentRef/Money，existing/new recovery 验证唯一性、原 fact ref/relation/allocation，Provider 返回后绑定 returned transactionSn。
- zero-call：目标源码只调用 `queryFundsActionFacts`；`queryFundsTransaction`、`findFundsTransactionByBusiness`、compatibility/fallback/dual-read 均为零。Provider API/DTO/POM/schema/Mapper、第二生产文件和三个 RED tests 未改。
- immutable evidence：tests SHA=`d78277172a2d76982d5b197d6bfaa9c7cd0f35f902e4833d18c7cc4e55629043 / f3558962efbd9ef73fd649e581c18b3eb8a5f7059b4de9d1775af837775fa614 / 6b1cd39a4852729cd88498232e91795d7be92f3bd97168b665b7a71fea4ed4ca`；root/coupon/tests POM/schema SHA=`363e8678... / 5ea3322e... / b3ecc50f... / 91a4244e...`。
- final behavior evidence：application=`57/0F/0E/0S`、boundary=`31/0F/0E/0S`、focused=`88/0F/0E/0S`；main=`25/0F/0E/3S`；lineage/seed/recover 各=`1/0F/0E/0S`；offline compile=`42/42`。XML SHA=`bb0b8d8c... / 2fe600a5... / 4445377b... / b012f63f... / 68c15304... / b0cd04d5...`。
- transient evidence：首轮 focused 与 main 各出现一个旧异常文案片段 1F；Maker 只在生产失败分支依次补齐“原交易业务流水”和“原交易不存在”，未改测试、判断、资金/账务副作用或范围，随后完整 fresh 全绿。
- quality boundary：Wind module guard=`9E/74W`，全部命中其他既有文件，目标文件无命中；Maven duplicate dependency WARN 与 recover H2 alias 已存在 ERROR log 均未形成 test error。Capte manifest=`38 / 0b9a02c1e9a5284651824c3e68d0bcc52dd765803aab670112d98ea58ee159b1`，wind-funds=`78 / 00d618fada22b7c67da9651af28bdde490594194df69c23ab7529f7280cda6f3`；staged empty、diff-check PASS。
- value：现有 ActionFact 已被真实 Benefit Consumer 用于完成判定，消除 transaction/root existence 假完成；Coupon Owner 仍持有业务资格和恢复授权，Funds Owner 只证明动作结果，没有扩展公共抽象。
- evidence boundary：已发布 `1.0.1-SNAPSHOT`、real Bean/H2、main 与两 JVM seed/recover 已有证据，但仍缺 Provider source revision -> published artifact 的可审计 lineage；不直接宣称本切片 E4、L4、生产或 MIG-09。
- current status：`CAPTE_BENEFIT_ACTIONFACT_CONSUMER_GREEN_INDEPENDENT_CHECKER_PASS / GREEN_CLOSEOUT_INDEPENDENT_CHECKER_PASS / MIG08_ACTIONFACT_CONSUMER_E4_ASSESSMENT_ENTRY_CARD_REQUIRED / E4_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.259`；Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。
- next gate：仅在本 closeout 独立 Checker PASS 后，由 Human Owner 决定 `MIG08_ACTIONFACT_CONSUMER_E4_ASSESSMENT_ENTRY_CARD_GRANT`。

### W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-GREEN-SOURCE-RESTORE-ENTRY-CARD-001

- task/type：`Consumer Green source restore Entry Card / documentation-only`；只恢复已经在 `plan-r2.259` 通过 Green Checker 的交付物，不新增业务语义、Public Contract、兼容、fallback 或能力。
- actual value：把曾只存在于易失 worktree 的 ActionFact Consumer Green 固化为可重建 source card，避免 Provider lineage、Bean/H2 和业务测试全部通过却仍运行旧 transaction-presence Consumer 的假 E4；同时保留 Provider r9，无价值重建为零。
- recovery provenance：原始确定性证据为 `/Users/wuxp/.codex/sessions/2026/08/12/rollout-2026-08-12T09-08-07-019ff383-2af1-7e82-ad2c-19913a6bb1d2.jsonl`。production 依次重放 `2026-08-25T06:17:27.471Z / 06:18:11.223Z / 06:20:25.685Z / 06:23:23.157Z` 四个 patch；integration 依次重放 `05:21:28.181Z / 05:21:50.819Z / 05:22:12.505Z / 05:22:32.445Z / 05:26:14.313Z` 五个 patch。临时候选包=`/private/tmp/capte-actionfact-green-source-card.tar`，SHA=`e2676660f048494a2a0cc7d1db0429f1624d1d7e0829f69dde2d80fd56319300`，manifest SHA=`13d317663ea2e71e6539d2dc14dd40973822a985befa2c74a3b9ce471d79874e`；临时包缺失时必须按原始记录重建并复算同一 SHA，不能语义重写。
- live Capte preflight：HEAD=`8172deb18f3a60c80ea814226c4e256d337b5146`，default/`-uall` manifest=`3 / 498b7c2602ece454d9ecbaaab40d9dabc705c845f00c4cd96ba12a55163c8a98`，staged=`2`，`git diff --check` PASS。两份 staged acceptance tests 是其他既有用户改动，本卡只读保护；另有一个无关 untracked 原型文档，不得删除、暂存或改写。任一 live HEAD、三项 manifest、index 内容或目标 base SHA 漂移必须先重冻本卡。
- future write whitelist：
  1. `MODIFY marketing/coupon-impl/src/main/java/com/capte/marketing/coupon/integration/funds/CouponBenefitFundingSettlement.java`；base=`c32e6f1df72551ae7a62039362b1d6a62209344e3517ed67ce65473f1f9264b5`，target=`8b28a049aa44b08fd5d2e99ce25d6ca0c2af43f49ee8927babe5081b76c0f1f3`；只使用恢复包精确内容或上述四 patch。
  2. `MODIFY tests/src/test/java/com/capte/order/transaction/OrderCouponRedemptionIntegrationTests.java`；base=`0c863e2e54dec2f2dcdea2ea5b5f5b3466c8bb43afd548907f74db9d3e47be2d`，target=`6b1cd39a4852729cd88498232e91795d7be92f3bd97168b665b7a71fea4ed4ca`；只使用恢复包精确内容或上述五 patch。
- immutable staged acceptance：`tests/src/test/java/com/capte/marketing/coupon/CouponRedemptionApplicationServiceImplTests.java=d78277172a2d76982d5b197d6bfaa9c7cd0f35f902e4833d18c7cc4e55629043`；`tests/src/test/java/com/capte/marketing/coupon/CouponImplContractBoundaryTests.java=f3558962efbd9ef73fd649e581c18b3eb8a5f7059b4de9d1775af837775fa614`。两文件 index/worktree SHA 当前逐项相等；未来执行不得修改、stage、unstage 或重新生成。
- immutable config：root/coupon/tests POM/schema 当前 SHA=`6655fa6562eb568fff7b9d7f4d94ca9ea897afbf1d58f0a76ef50017cda65fb7 / 5ea3322ef77f0c783e5889a0f3882fc132cfd70d4b01286f518ee286d0d883f9 / b3ecc50fbb80f83b78fdb50a3c0d857405915697f7f73b28e25bf3677d610f48 / 8816919e210a185b16ba830049f8df77b977f20e1e55cda7e38e66b85c058bbf`；Provider、其他 Capte source/tests 和无关 untracked 文档同样 immutable。
- restore semantics：恢复后目标 production 必须只使用 `queryFundsActionFacts` 证明 new/existing settle reference 与 existing/new recovery；`queryFundsTransaction`、`findFundsTransactionByBusiness`、root-presence fallback、dual-read 和 compatibility zero-call。integration 必须重新断言 settle/recover ActionFact、`intentRef`、Money/provenMoney 与 original fact allocation；不改变业务资格、恢复授权或 Provider API。
- future validation：Java 21/Maven 3.6.3、offline，显式使用 Provider r9 `1.0.1-e4-mig08-20260825-615c639e-d8786110-r9`。先 `clean test-compile`，目标 reactor=`45/45`；fresh application=`57/0F/0E/0S`、boundary=`31/0F/0E/0S`、focused=`88/0F/0E/0S`，integration main=`25/0F/0E/3S`。随后复核四文件 SHA、production zero-call、两 staged index/worktree SHA、POM/schema、完整 manifest、staged 列表和 `git diff --check`。lineage/seed/recover 留给后续独立 E4 resume，不由 source restore PASS 冒充。
- stop/exclusions：恢复包或 raw patch 无法复算目标 SHA、base/staged/index 漂移、需要第三文件、测试放宽、兼容层、POM/schema/Provider 修改、联网、Git 或其他业务决定时立即停止。`ADD=0 / DELETE=0`；MySQL、PMD、MIG-09、L4、enable/release/production 不在本卡。
- current status：`SOURCE_RESTORE_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / SOURCE_RESTORE_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.263`；Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。
- next gate：仅在本 Entry Card 独立 Checker PASS 后，由 Human Owner 决定 `MIG08_ACTIONFACT_CONSUMER_GREEN_SOURCE_RESTORE_EXECUTION_GRANT`；恢复与 Checker PASS 后再独立决定 `MIG08_ACTIONFACT_CONSUMER_E4_RESUME_GRANT`，Provider r9 可复用。

## 12. 验证矩阵

| Gate | 对象 | Maker 自检 | Independent Checker | 未通过处理 |
| --- | --- | --- | --- | --- |
| `G0` | 本执行规格 | Harness validator、链接/路径、状态和 dirty-worktree 隔离 | 未参与主笔的架构/计划 Reviewer | 修计划，不进入产品设计 |
| `G1` | 产品设计 | 最新 PRD 模板、产品 validator、场景闭环 | 产品/资金 Checker + 人类 Owner | 退回对应产品决策 |
| `G2` | DSL 设计 | 词汇、JSON 正反例、依赖和不变量清点 | 架构/Consumer Checker | 退回 DSL 或产品层 |
| `G3` | 系分设计 | 系分 validator、API 清册、依赖/状态/事务/破坏式切换检查 | 独立架构 Checker | 退回模块决策，不写代码 |
| `G4` | TDD 设计 | AC/Red/命令/证据层级映射 | 独立 TDD Checker | 退回测试或上游设计 |
| `G5` | 实现候选 | TDD、编译、PMD、边界和 scoped diff | 独立源码 CR | 不提交、不发布 |
| `G6` | 宿主集成 | artifact 谱系、real Bean、联合 schema/事务 | Consumer/Provider 双方 Checker | 不宣称 L3/L4 |

当前文档验证命令：

```bash
python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py \
  --kind gsd-wave \
  --file openspec/changes/funds-public-capability-redesign/spec.md
git diff --check -- openspec/changes/funds-public-capability-redesign/spec.md
```

新文件尚未进入 Git index 时，补充执行 `git diff --no-index --check /dev/null <file>`；退出码 `1` 表示两侧有差异，只有出现空白诊断才表示检查失败。

正式产品和系分正文创建后，分别追加：

```bash
python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py \
  --kind prd \
  --file docs/产品设计/支付资金公共能力层-产品设计.md

python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py \
  --kind system-design \
  --file docs/系分设计/支付资金公共能力层-系分设计.md
```

validator 只证明结构完整，不证明业务语义、架构质量、测试通过或生产可用。

Round 0 验证记录（2026-08-12）：Harness validator 通过；所有证据路径存在；新文件空白检查无诊断；独立 Checker 初审发现事实可逆性、主体边界和零资金断言问题，修正后复核为 `G0 PASS`。本轮为文档计划，不运行 Maven 编译或测试。

`W1-01` 验证记录（2026-08-12）：最新版 PRD validator 通过；Harness validator 通过；三个项目完成主体/账户/业务对象静态模拟；独立产品 Checker 发现“候选规则提前确认”和“路由参与方等同记账主体”两项问题，修正后复核为 `W1-01 CHECKER PASS`。这不表示 `G1` 或 Wave 1 完成。

`W1-02` 完成层级取证记录（2026-08-12）：Provider、`capte-domain`、`fincone` 三个角色完成只读取证；识别出统一 `SUCCESS`、跨 Owner 同名 `FAILED`、空 route explanation snapshot、payout `RETURNED` 和 Consumer `UNKNOWN` 恢复等反例。独立 Checker 初审发现 route snapshot 过度概括和恢复状态陈旧，修正后复核为 `W1-02 COMPLETION EVIDENCE PASS`。这些证据现已进入 `Q-003` 决策包，不表示 `W1-02`、`G1` 或产品设计完成。

`Q-002` 决策包验证记录（2026-08-12）：Provider、`capte-domain`、`fincone` 完成对象关系和六个业务场景只读取证；决策包将上位名收紧为“内部可记账账户”，区分 Funding 资金余额与 Credit 额度/责任控制余额，并明确交易时受控解析、原事实冻结和七类硬负例。独立 Checker 初审发现零资金路径被无条件解析、PRD 与问题命题漂移及台账字段缺失，修正后复核为 `Q-002 DECISION PACKAGE PASS`。Owner 已于同日明确接受，接受范围以 `Q-002` Decision Register 为准。

`Q-002` Owner Gate 停止记录（2026-08-12）：连续三个 Goal turn 均未取得新的 Owner 裁决；可独立完成的跨项目取证、业务模拟、命题收紧和 Checker 复核已经完成。继续扩写不会消除产品取舍，只会越过“产品语义先确认、未过 Wave 1 不进入 DSL”的停止线，因此 Goal 状态转为 `BLOCKED / Q-002_OWNER_GATE`，恢复条件是 Owner 接受、修改或拒绝 `Q-002`。

`Q-002` 恢复记录（2026-08-12）：Owner 明确回复“接受 Q-002”；Decision Register 与正式产品正文已回写为 `accepted`，Goal 恢复 `ACTIVE` 并进入 `W1-02 / Q-003_PENDING`。未随本次接受批准账户具体分类、账目/余额拆轴、posting matrix、状态枚举或 Java 接口。

`Q-003` 决策包验证记录（2026-08-12）：`capte-domain` 与 `fincone` Consumer 角色复核钱包/Benefit/组合支付、VCC、ACH、收单和对账场景后，将候选从线性完成层级收紧为“每个事实 Owner 回答的独立问题构成正交证据维度，由场景声明局部偏序和展示门槛”。独立 Checker 初审发现候选混合适用性、证据终局性与领域结果，并遗漏清分/清算/结算/出款独立维度；拆轴并改为开放维度规则后复核为 `Q-003 DECISION PACKAGE PASS`。Owner 已于同日明确接受，接受范围以 `Q-003` Decision Register 为准。

`Q-003` 恢复与范围审计记录（2026-08-12）：Owner 明确回复“接受 Q-003”；Decision Register 与正式产品正文已回写为 `accepted`。由于 `Q-001` 至 `Q-003` 已达到本 Wave 首轮三个 Owner 决策上限，当前暂停扩问并按阻断、可延后和外部 Owner 分类剩余项。审计判定“确定失败后的 attempt 关系与不可变经济语义边界”仍是独立产品取舍，建议在 Owner 重新确认范围后只追加一个最小 `Q-004`；具体 key、摘要、API、事务和 rail 规则不进入该题。

`W1-02` 范围重新确认记录（2026-08-12）：Owner 明确回复“可以”，接受只追加一个 `Q-004 / 业务意图身份与重放契约`，只裁决确定失败后的 attempt 关系和不可变经济语义类别边界。`Q-004` 后不再新增公共抽象问题；key/hash/字段表、API、存储、事务、Saga、重试次数和 rail matrix 均不在本轮。

`Q-004` 决策包验证记录（2026-08-12）：Provider、`capte-domain`、`fincone` 分别复核当前幂等/失败行为、钱包与 Benefit/组合支付、VCC/ACH/收单/对账场景。候选收敛为“稳定 Intent + 受控多 Attempt”，并区分 Delivery、Event、Evidence。独立 Checker 初审发现合成 `FAILED_NO_EFFECT`、部分效果可能整单重跑及 recovery 混名三项问题；拆为局部领域结果与资金效果证据、禁止部分效果整单重跑、区分技术/证据恢复与经济修复后，复核为 `Q-004 DECISION PACKAGE PASS`。Owner 随后明确接受方案 A；当前进入接受范围与执行规范 Checker，不因此批准具体实现或 G1。

`Q-004` 接受与破坏式重构策略记录（2026-08-12）：Owner 明确接受方案 A，并要求后续重构不考虑任何兼容问题。执行规范据此采用目标态单轨：Provider、全部已知真实 Consumer 与必要数据/schema 在同一垂直切片同步切换并删除旧入口，不保留 bridge、转发重载、别名、双写双读、兼容窗口、deprecation 周期或延后退役。该取舍不降低 Consumer 清册、E3/E4、资金守恒、一次性迁移校验、独立 Checker 和整切片回滚门禁；任一已知 Consumer 不能同批切换时，整个切片保持 `blocked`。独立 Checker 复核为 `PASS / 0 P0-P2`；该 PASS 不构成 RED、实现、API baseline 或 Git 授权。

`SIM-01` 至 `SIM-07` 条件化场景合同验证记录（2026-08-12）：产品正文已覆盖钱包支付/退款、券+钱包组合、Benefit 60/40 验收模拟、VCC、ACH/GlobalAccount、收单 payout 和三方对账；每份合同包含 Owner、前置事实、局部证据偏序、资金效果、主逆异常、人工停止、AC、hard negatives 和 PENDING。Capte、Fincone/轨道与独立 G1 Checker 初审发现动作流水基数、Benefit 快照越界、UNKNOWN 恢复、NOC 资金矩阵、Gate 泛化、confirmed/finality 合并和对账匹配口径等问题，修正后均复核为 `SIM-01~07 CONDITIONAL CONTRACTS PASS`。该记录中的 `G1 NOT PASS` 是当时状态；后续 Owner 决策、场景接受复核与当前 `W1-02 / G1` 结论已将其取代，当前状态以 Metadata 与恢复入口为准。

`SIM-01` 制品谱系验证记录（2026-08-12）：在 `/tmp` 隔离副本以 Corretto 21 离线重建 Capte 当前生产源码，reactor 编译通过；显式覆盖本机 `maven.test.skip=true` 后，中央 tests 在执行前被 54 个跨域陈旧测试源码错误阻断。最小类加载探针 1/1 通过，确认运行时从本地 Maven 仓库加载 `transaction-face` 与 `core` 的 `1.0.0-SNAPSHOT` JAR，并冻结上述 SHA-256；钱包 Recording 测试单独编译则因实际加载接口新增 `findFundsTransactionByExternalFundsFact(...)` 而失败。Snapshot 不含 Provider revision，且 DTO/接口与当前 checkout 不同，因此仅达到 resolved/loaded 部分谱系，不是 `E4`；Capte 未依赖任何 funds impl、宿主 schema 无资金表，`P-SIM01-03-E4` 继续 fail-closed。

`P-SIM01-01` 决策包验证记录（2026-08-12）：Capte 当前 `Order`/商品快照没有 seller/merchant/payee 责任，`payeeId=capte` 只是对所有订单写入的业务标签，Wallet 资金请求也未传入收款责任；当前 Provider 固定平台 settlement route 同样不能反推商业模式。产品正文 `5.13` 将平台自营、商户经济直收、平台代收清算按授权效果、完成承接、分阶段退款、清结算/出款适用性和对账 Owner 统一展开；独立 Checker 初审发现后置收款解析旁路及三模式非同构问题，删除旁路并补齐同构事实后复核为 `P-SIM01-01 DECISION PACKAGE PASS`。该 PASS 只表示场景决策包可交对应 Owner，不表示已选定模式、`G1` 通过或可进入 DSL；`Q-004` 在其后已由 Owner 接受方案 A。

`P-SIM01-01-D` Owner 裁决记录（2026-08-12）：Owner 明确接受按 `tenant + business scene + merchant（或无 merchant）+ 责任规则版本` 在 A/B/C 中选择并冻结唯一模式。D 仅是责任路由规则，不是第四种资金模式。产品取舍已回写；当前 Capte 缺 seller/payee、模式选择结果、责任规则版本和账户准入证据，拆为 `P-SIM01-01-HOST` 并继续在授权前 fail closed。独立 Checker 初审发现 `VC-001` Slice Card 仍遗漏已接受的 `Q-004 / P-SIM01-01-D` 且误称动作事实规则未签收；修正 accepted basis、产品 blocker 与 `P-SIM01-03-E4` 后复核为 `PASS / 0 P0-P2`。该 PASS 不批准 `P-SIM01-02`、`VC-001 eligible`、RED、API、代码、测试或 Git。

`P-SIM01-02-A` Owner 裁决记录（2026-08-12）：用户授权按推荐推进，接受纯内部钱包严格内部资金闭合门槛。订单最终 `PAID/REFUNDED` 要求业务目标、动作事实、平衡账务和 D 责任范围指定余额效果分别闭合；不等待外部 finality、清结算、出款或 reconciliation。当前 Capte 仅以 participant 布尔结果/可选流水传播 Bill/Order，运行证据缺口转为 `P-SIM01-02-HOST`。Capte 与 Fincone 场景复核及独立 Checker 均为 `PASS / 0 P0-P2`；产品正文 `5.15` 同时达到 `P-SIM02-01 DECISION PACKAGE PASS`。该 PASS 不批准 `VC-001 eligible`、RED、API、代码、测试或 Git。

`P-SIM02-01-A` Owner 裁决记录（2026-08-12）：用户明确接受组合支付“恢复优先，超出冻结边界后放弃并逆向”策略。在履约时限、业务授权与 Q-004 准入内只恢复未完成腿；任一腿 `UNKNOWN` 时不执行新动作；超界后逆向所有成功腿，且只有所有必需逆向分别闭合才能声明父计划已取消。当前 Capte 的成功腿/逆向事实、策略版本和重启恢复缺口转为 `P-SIM02-01-HOST`；后续 `P-SIM02-02-A` 已接受 Coupon 动作事实权威，真实消费与恢复缺口转为 `P-SIM02-02-HOST`。接受回写经独立 Checker 复核为 `PASS / 0 P0-P2`；不因接受 A 而批准 G1、DSL、RED、API、代码、测试、Consumer 仓或 Git。

`P-SIM02-02` 决策包与 Owner 裁决记录（2026-08-12）：Coupon 域源码已存在不可变动作流水和支付/退款决策查询，但 Order Coupon participant 仍只消费同步布尔结果、丢弃动作引用并在 confirm 异常后立即 release。产品包同构比较全动作权威 A、仅签收部分动作且其余永久 fail closed/manual 的受限能力 B、当前聚合状态/同步响应/Order 影子状态推断 C，独立 Checker 判定 `DECISION_PACKAGE_PASS / 0 P0-P2`。用户随后明确接受 `P-SIM02-02-A`；接受范围经独立 Checker 复核为 `PASS / 0 P0-P2`，宿主缺口转为 `P-SIM02-02-HOST`。未运行测试，未修改生产/测试/API baseline/Consumer 仓或 Git。

`P-SIM03-01` 决策包与 D 路由裁决记录（2026-08-12）：Capte production/test source 证明 60/40 只是当前验收模拟与活动配置，出资角色、资金性质和 receiver 分层，但生产 settle 未提供完整成本/承接责任且退款重读当前配置；Fincone docs-first accepted child scope 只确认商品订单 `SPECIFIED` 由真实订单 payee 提供经济价值承接责任、适用资金项独立留痕和原路退款，不替 Finance/Accounting/Risk 决定商户 40 是否为资金转移。产品包据此同构比较 A“平台 60 资金补足 + 商户 40 经营折让”、B“两笔独立资金 contribution”、C“仅成本分配且零 Benefit 资金动作”，决策包经独立 Checker 复核为 `PASS / 0 P0-P2`。用户随后明确接受 `P-SIM03-01-D`：按产品/订单责任范围为每次核销在 A/B/C 中唯一选择并冻结完整责任快照；D 不是第四模式，宿主缺口转为 `P-SIM03-01-HOST`。接受回写经独立 Checker 复核为 `PASS / 0 P0-P2`；下一入口先形成 `P-SIM03-02` 决策包，不直接进入 Owner Gate。本轮未运行测试，未修改生产/测试/API baseline/Consumer 仓或 Git。

`P-SIM03-02` 决策包与 Owner 裁决记录（2026-08-12）：Capte E2 证明当前 Consumer 只按活动 funding rows 顺序调用资金 settle/refund、丢弃返回引用、按记录存在跳过并在退款时重读当前配置，不能表达 D 已冻结的异构责任项或 partial/UNKNOWN；Fincone E1 child scope 只支持逐资金项 handoff、原引用、累计与 UNKNOWN 原单查询，不裁父级策略。产品包据此用 `R-A/R-B/R-C` 同构比较恢复优先、退出优先和人工接管，并逐一适配 `M-A/M-B/M-C`；补齐共同情形与模式×策略判别矩阵后，独立 Checker 判定 `DECISION_PACKAGE PASS / 0 P0-P2`。用户随后明确接受 `P-SIM03-02-R-A`：在冻结边界内优先恢复未完成项，越界后逐项逆向已形成效果，UNKNOWN 整组零新动作；宿主缺口拆为 `P-SIM03-02-HOST`。接受回写经独立 Checker 复核为 `PASS / 0 P0-P2`；下一入口为 `P-SIM04-01_DECISION_PACKAGE`。本轮未运行测试，未修改生产/测试/API baseline/Consumer 仓或 Git。

`P-SIM04-01` 决策包与 Owner 裁决记录（2026-08-12）：Provider E2 支持授权根、后继动作累计和 SHARED Credit/Funding 双责任，但无真实 VCC Consumer；Fincone VCC E1 仍为 Owner PENDING/NOT_STARTED，支持通用增量 Event 模型，而具体 30/50 样例采用累计快照、最终 completed=50。产品包据此同构比较 Provider/SIM 候选 A“规范化增量动作事实”和 B“权威累计快照”，分别给出 PREPAID/SHARED 的 100/30/50/20/20 可复算链及拒绝、超限、错误引用、UNKNOWN、重复乱序和 binding 漂移矩阵。独立 Checker 初审发现证据归属与 B 的 refund allocation 两项问题；修正为跨项目证据冲突、明确 B 的 refund20 归首笔 `Δ30`，并把逐 complete refund 标为当前 Provider gap 后，复核为 `P-SIM04-01 DECISION_PACKAGE PASS / 0 P0-P2`。用户随后明确回复 `A`，接受公共层只消费 VCC/issuer 边界权威归一的不可变增量动作；累计快照仍由 `P-SIM04-02` 裁 authority/sequence 并先归一，不把 A 的 `Δ30 + Δ50 = 80` 倒写为 Fincone 事实。接受回写经独立 Checker 复核为 `P-SIM04-01-A ACCEPTANCE_SCOPE_PASS / 0 P0-P2`，宿主缺口转为 `P-SIM04-01-HOST`；当前唯一进入 `P-SIM04-02_DECISION_PACKAGE`。本轮不修改生产/测试/API baseline/Consumer 仓或 Git。

`P-SIM04-02` 决策包与 Owner 裁决记录（2026-08-12）：Fincone 卡交易 E1 只提供多来源留证、Event/聚合分层、累计快照归一、expired/reversal/refund/争议和 Network Settlement 候选边界，OwnerDecision 仍 PENDING 且全部 CT 是未来验收；Provider E2 只能执行已由上游裁决的内部资金动作，不判断 issuer source/sequence/finality，也无真实 VCC Consumer。产品包据此同构比较 A“权威 query/report 刷新优先”、B“已签收契约事件优先”和 C“多证据收敛/manual”，并用非第四候选的版本化选择规则按 issuer/program/action/rule version 唯一路由；独立 Checker 判定 `DECISION_PACKAGE_PASS / 0 P0-P2`。用户随后回复“可以”，接受 `P-SIM04-02-D`：版本化 A/B/C 选择归 VCC/issuer adapter，`wind-funds` 只消费并校验已验真、已归一的稳定动作事实，不持有 issuer 策略或路由引擎；具体 source matrix 与真实 adapter 证据转为 `P-SIM04-02-HOST`。接受范围 Checker 复核为 `PASS / 0 P0-P2`；下一入口为 `P-SIM05-01_DECISION_PACKAGE`，不修改生产/测试/API baseline/Consumer 仓或 Git。

`P-SIM05-01` 决策包与 Owner 裁决记录（2026-08-13）：Fincone GlobalAccount E1 证明 accepted/confirmed/finality、内部资金和 NOC/return 必须分层，但具体 ACH authority/finality/status/return 规则仍 Owner PENDING；Provider E2 只支持已归一 confirmed credit 的一次 topup、外部事实重放/冲突和 accepted fail-fast，不支持 return/reversal，也无 NOC 资金契约。产品包据此同构比较 A“confirmed 后形成对应 primary effect”、B“等待已签收 finality 后形成对应 effect”和 C“有权人工签发后形成对应 effect”，独立 Checker判定 `DECISION_PACKAGE_PASS / 0 P0-P2`。用户随后明确接受 A，并澄清上游业务/rail adapter 负责外部数据验真与 confirmed 归一，`wind-funds` 只专注规范化资金处理；资金域仍独立校验 tenant、金额币种、内部责任/账户、身份幂等、原事实、累计与账务守恒。接受范围经独立 Checker 判定 `PASS / 0 P0-P2`；下一入口为 `P-SIM06-01_DECISION_PACKAGE`，未修改生产/测试/API baseline/Consumer 仓或 Git。

`P-SIM06-01` 决策包与 Owner 裁决记录（2026-08-13）：Fincone acquiring 目前只有 `AdmissionStatus=BLOCKED / OwnerDecision=PENDING` 的准入卡，`ACQ-GATE-001~007` 全部未关闭，没有真实 PSP/acquirer authority、Merchant/责任或 runtime Consumer；Provider 的 `AcquiringSettlementBusinessFlowTests` 仅为 E2 source-level 内部组合模拟，本轮未 fresh 执行。产品包据此只比较同一 capture 资金效果的三种准入证据强度：A“上游权威规范化 capture 即准入”、B“另需独立规范化佐证”、C“有权人工签发后准入”；共同保留 Funds 对 tenant、身份冲突、金额币种、冻结责任、原授权/原事实、累计、账务和余额的独立校验。替代独立 Checker 在原 Checker 工具通道连续中断后完成只读回读，判定 `DECISION_PACKAGE_PASS / 0 P0-P2`。用户随后明确回复 `A`，接受 adapter 权威 normalized capture 进入 Funds 独立准入的产品边界；B/C 未选择，真实 source matrix、责任快照、adapter/Consumer 与恢复/E4 缺口转为 `P-SIM06-01-HOST`。接受范围 Checker 随后判定 `PASS / 0 P0-P2`；external-rule checker 因真实 PSP source/version/scope/verified_at/confirming_party 尚未签收而按预期失败并继续作为 ACQ/HOST blocker；下一入口为 `P-SIM06-02_DECISION_PACKAGE`，未修改生产/测试/API baseline/Consumer 仓或 Git。

`P-SIM06-02` 决策包与 Owner 裁决记录（2026-08-13）：Fincone E1 证明 payout submitted/accepted/processing、executor result、beneficiary arrival、rail finality 与 reconciliation 必须分层，真实外部 payout 与收单 Admission 仍 PENDING/BLOCKED；Provider E2 source 只能证明持久出款单、规范化回执、内部 Funds/Ledger/Balance 闭合及冲突保护，不能证明 beneficiary finality，且当前 `SUCCEEDED` 展示映射存在被上层误读的 gap。产品包据此同构比较 A“已签 rail profile 允许 executor success 同时证明 finality”、B“另需独立 bank/beneficiary 到账证据”和 C“有权人工裁决展示”。主 Checker 工具通道中断后，替代独立 Checker 完成只读回读并判定 `DECISION_PACKAGE_PASS / 0 P0-P2`。用户随后按推荐接受 B：executor `SUCCEEDED` 只关闭执行结果，独立到账证据才关闭 beneficiary arrival，rail finality 仍独立声明；A/C 只保留比较记录，版本化选择改为在上游 adapter 唯一选择独立到账证据的权威来源。接受范围 Checker 判定 `P-SIM06-02-B ACCEPTANCE PASS / 0 P0-P2`；随后唯一进入 `P-SIM06-03_DECISION_PACKAGE`。本轮未修改生产/测试/API baseline/Consumer 仓或 Git。

`P-SIM06-03` 决策包记录（2026-08-13）：Fincone acquiring/清结算 E1 继续把真实 RETURNED、chargeback/dispute、fee、loss、merchant recovery 与 ACQ Gate 保持 PENDING/BLOCKED；Provider E2 source 只证明 payout 成功后迟到相反终态不覆盖成功且不自动反做资金、无成功 payout effect 的 RETURNED 零资金，以及 RecoveryOrder 只校验上游给定责任和已完成 recovery 累计，不决定责任或创建追偿。产品包据此把 capture refund、returned principal、chargeback/dispute principal、fee、FX、loss/write-off 与 merchant recovery 拆成独立事实，同构比较 A“预签责任规则”、B“分项责任事实准入”和 C“有权逐案裁决”。独立 Checker 初审发现 5.12 范围窄化及 loss 与后继对账循环依赖两项问题，修正为完整分项范围、责任准入与后继对账正交后复核为 `P-SIM06-03 DECISION PACKAGE PASS / 0 P0-P2`。用户随后明确选择 B：上游逐项形成耐久责任事实，Funds 只执行明确归责且仍可恢复的 normalized action；A/C 未选择且不是 fallback，真实规则、Consumer、恢复与 E4 缺口转入 `P-SIM06-03-HOST`。B 的接受范围经独立 Checker 判定 `PASS / 0 P0-P2`，随后唯一进入 `P-SIM07-01_DECISION_PACKAGE`；本轮未修改生产/测试/API baseline/Consumer 仓或 Git。

`P-SIM07-01` 决策包与 Owner 裁决记录（2026-08-13）：Provider 当前 `HEAD + dirty worktree` 的 E2 source 已有不可变 batch/source snapshot、显式 `1:1` match、Difference/Action/rerun 与 current-lineage Gate 骨架，但明确不支持用重复引用表达 `1:N/N:1`，且本轮未 fresh 执行；现实现只固化调用方提交的 verified match assertion，并把 `EXACT_MATCH/RULE_MATCH` 都计入自动对平，不比较 Money/status 或验证 rule evidence，因此不证明 A/B 的目标 enforcement。Fincone 清结算 E1 仍为 Owner PENDING/Gate OPEN，只支持分层来源、冻结范围、不可变结果与后继 current-lineage 闭合的目标边界。产品包据此同构比较 A“exact evidence”、B“已签 normalized relation/tolerance evidence”和 C“有权逐案 decision/correction evidence”，共同禁止原始来源规则进入 Funds、覆盖旧结果、人工备注直关 Difference、旧 lineage 放行或 reconciliation 自动资金修复。独立 Checker 初审指出现有 RULE_MATCH 能力被误读为 A strict-exact 的 E2 保证，修正证据归属与 enforcement gap 后复核为 `DECISION_PACKAGE_PASS / 0 P0-P2`。用户随后明确选择 A：只允许 normalized `1:1`、同币 Money/状态/稳定引用严格相等且 coverage 完整的事实自动对平，复杂关系由来源 Owner 先固化为单一聚合事实；B/C 未选择且不是 fallback，当前 Provider enforcement 与真实来源供给缺口转为 `P-SIM07-01-HOST`。接受回写初审发现 SIM-07 两处旧 Owner/PENDING 状态，修正后替代独立 Checker 判定 `P-SIM07-01-A ACCEPTANCE PASS / 0 P0-P2`；随后唯一进入 `W1-02 / G1` 产品信息就绪与准出复核。本轮未修改生产/测试/API baseline/Consumer 仓或 Git。

`W1-02 / G1` 产品信息就绪与准出记录（2026-08-13）：产品 validator 实际返回 `OK`（仅非阻断 `implementation_language` WARN），Harness validator 返回 `OK`；两份未跟踪目标文档分别执行 `git diff --no-index --check /dev/null <file>` 均无空白诊断。业务、研发、测试角色走读均为 `PASS`。外部规则 checker 因真实 issuer/ACH/PSP/rail 的 `source/version/scope/verified_at/confirming_party` 未签收而按预期 `FAIL`，该缺口继续隔离在 HOST/E5。替代独立 Checker 初审要求补实际 validator 与三角色走读证据，补齐后复核为 `G1 PASS / 0 P0-P2`。该 PASS 只允许 Product Context Card 进入 `W2-01`，不关闭 HOST/E4/E5、VC eligibility、RED、Execution Grant、API、代码、测试、Consumer、Git、发布或生产。

执行动作规范与重构计划验证记录（2026-08-12）：采用 `skills` 项目最新 `wise-agent` 模块合议协议，将原子轮次 `A0-A9`、一主多权独立证、`Information Readiness Gate`、Module/Project Fact Card、双边 `Contract Inquiry -> Provider Evidence Response -> Consumer Reconciliation -> Checker`、G1-G6 会商点、Wave 5 测试先行切片、Wave 6 宿主集成和原 `R0-R8` 渐进重构顺序写入本规格。Capte 与 Fincone 分别按独立事实权威完成只读双边会商输入；独立 Checker 初审发现协议基线混用 clean HEAD/dirty 源码及承重 revision 字段折叠两项问题，改为可复现的 `HEAD + dirty diff fingerprint + current file hashes` 并展开 Provider/Consumer 接受版本后复核为 `EXECUTION_AND_REFACTORING_PLAN PASS`。Harness validator 通过，`git diff --check` 无诊断。该 PASS 是 `plan-r1` 历史结论，只确认当时计划结构，不表示 `Q-004` 接受、`G1` 通过或授权进入 DSL、接口、代码、Git、宿主集成、发布或生产。

`plan-r2` CR 修订记录（2026-08-12）：针对“暂停 runtime Goal 后按计划推进”的可执行性 CR，已将 Change Spec 明确为持久执行基座；把全局 `R0/G1-G4` 阻断改为逐切片 `eligibility`；把 `R2-R6` 改为垂直切片内的能力轨，`R7` 改为受影响 public contract 切片的随行 Consumer 迁移；要求所有已知 Consumer E4 在旧契约退役前完成；收紧 G4/G5 会商触发条件，并补 docs-first 权威签收状态、dirty overlap 与 `RS-001` Entry Card。该轮只形成待验证计划，未产生代码或 Git 授权；其后准出结果见下条 `RS-001` 验证记录。

`RS-001` 清册验证记录（2026-08-12）：已冻结 Provider public surface、Capte production/test Consumer、core/transaction-face/wallet-face 三制品 resolved/loaded/lineage、Fincone docs-first authority、38 条既有 dirty overlap 及三个垂直候选。Capte 与 Fincone Consumer Reconciliation 均为 `confirmed`；独立 Checker 逐项回读 source、POM/JAR、文档 metadata、dirty manifest 与候选准入卡后判定 `PASS / 0 P0-P2`。Harness 与 refactoring-design validator 通过，未修改生产/测试源码、API baseline、正式四层文档、跨仓文件或 Git。`VC-001` 仅进入 pre-Inquiry，不是实施切片准入。

`CI-RS001-CAPTE-WALLET-001` 验证记录（2026-08-12）：Provider Evidence 证明授权后继动作复用原授权 aggregate `transactionSn`，动作结果位于 detail，返回 String 与 main `referenceTransactionSn` 均不能单独证明本次动作完成或稳定回链；Capte production 当前却以非空 String 判成功，并从完成返回主流水的 `referenceTransactionSn` 反查原授权。Consumer Reconciliation=`conflict`，`VC-001=blocked`，不是 stale；最早 blocker=`P-SIM01-03`。独立 Checker 复算完整版本元组与 canonical evidence fingerprint=`90b12f7b...` 后判定 `PASS / 0 P0-P2`。本轮未执行测试，也未修改生产/测试/API baseline/Consumer 仓或 Git。

`P-SIM01-03` 决策包与 Owner 裁决记录（2026-08-12）：Capte、Provider 与 Fincone 跨场景证据支持“生命周期根 + 每次资金动作的耐久独立事实”的稳定语义，不支持含糊 main SN 证明动作完成。决策包同构比较 A/B/C，推荐 A、拒绝 C，并将 B 保留为后续可选物理形态；退款必须引用真实成功 complete 事实并按每个原事实约束累计。独立 Checker 初审发现从 FAILED/REJECTED 标签误推资金效果及累计公式重复扣减，修正为“按权威资金效果计数、标签本身不推断”与一致总上限公式后，判定 `P-SIM01-03 DECISION_PACKAGE_PASS / 0 P0-P2`。Owner 随后明确回复“A”，接受方案 A 的产品/公共契约语义；接受回写经独立 Checker 复核为 `PASS / 0 P0-P2`，`VC-001` 仍为 `blocked`。

`W3-01 / plan-r2.54` 系分候选与准出记录（2026-08-13）：按公共资金基础设施定位和当前源码重新对账后，撤销“删除持久 posting plan 并新造 entry posting group”的过度方案，保留现有不可变耐久 `LedgerPostingPlan` 及 `postingPlanSn + routeLegId` 分组、逆向和清分来源证据；区分瞬态 `LedgerPostingPlanSpec` 与耐久 plan。余额闭合复用 LedgerTransaction/plan/entry/主余额投影同一本地事务原子提交和现有可查询 ledger bucket，不把 `Ledger.version` 升格为 action lineage，也不新增 projection lineage 子系统；不一致视为完整性事件并 fail-closed/manual。当前源码已使用 ledger-impl 内部 `LedgerTransactionCommandService`，目标只新增一个 `core` 跨模块 internal port 以替换 transaction 对宽 assembler/spec/posting service 的直接依赖，不新增 Facade、注册表或兼容层。替代独立 Checker 初审只发现 G1/W2 对耐久 plan 的证据归属写高一项 P2；修正为耐久 plan 由当前 E2 source 与 W3 取舍承接后，单点复核判定 `W3-01 SYSTEM DESIGN PASS / 0 P0-P2`。随后机械进入 `plan-r2.55 / W3-02_WALLET_SYSTEM_DESIGN`；未批准 Java/API/DDL/TDD/代码/测试/Consumer/Git。

`W3-02 / plan-r2.56` Wallet 系分候选与准出记录（2026-08-13）：回读 Wallet face/impl、Transaction 调用链、架构边界测试、Capte 生产 Consumer 与 Fincone VCC E1 后，将 Wallet 收敛为账户主数据/能力、资金责任关系、支付工具/绑定、支出控制事实和账户余额查询五类职责。PaymentInstrument 交易执行与交易结果消费编排归 Transaction/W3-03；Ledger 查询适配退出 wallet-face；profile/账本初始化只作内部账户创建协作；ownerType、平台账户角色、Card/PREPAID/SHARED 与 raw rail rule 均不得直接决定账户、账目或 route。独立 Checker 初审指出当前 `FundsAccountId` 被误批为目标类型、Funding/Credit 查询证据被扩大到开户命令、账户状态变化合同缺失三项 P1；拆分标识/Owner、查询/开户和状态语义/命令可见性后，单点复核判定 `W3-02 SYSTEM DESIGN PASS / 0 P0-P2`。物理类型、开户 Public 准出、状态迁移矩阵、Fincone runtime 与所有 HOST/E4/E5 blocker 保持 PENDING；随后机械进入 `plan-r2.57 / W3-03_TRANSACTION_SYSTEM_DESIGN`，未批准 Java/API/DDL/TDD/代码/测试/Consumer/Git。

`W3-03 / plan-r2.58` Transaction 系分候选与准出记录（2026-08-14）：回读 transaction face/impl、route、Ledger/Wallet 协作、H2 source tests 与 Capte Wallet/Coupon 生产 Consumer 后，确认当前 root `FundsTransaction`、participant `FundsTransactionDetail` 与返回 root String 不能替代独立动作事实。目标复用 W2 唯一公共动作链，不新增 Facade/registry：Intent/Attempt/action identity 分别耐久查询，新 Attempt 受 Q-004 约束；每次动作形成独立 ActionFact；现有 event/method 只编译到 W2 六类 kind；首次动作内部唯一选路，后继动作逐原事实分配并沿原 route provenance；授权与退款累计只消费闭合动作事实；正资金效果的成功 Action、生命周期/逐原累计、ledger 关联、Ledger facts 和 required BalanceTarget 主投影共享本地事务。raw rail entry 退出 Funds，Benefit adapter 归 Capte 且 Provider 场景 face 待 E4 后删除，Clearing/Settlement/Payout 仅为 internal stage command。独立 Checker 初审发现 Intent/Attempt 系分合同、Benefit adapter 仓库归属、稳定 action kind 编译三项 P1 和 PRD revision 漂移一项 P2；最小修正后复核为 `W3-03 SYSTEM DESIGN PASS / 0 P0-P2`。当前目标 Intent/Attempt/ActionFact/query、统一 Bean/schema/tx、Capte UNKNOWN 恢复与 E4 仍未实现；随后机械进入 `plan-r2.59 / W3-04_RECONCILIATION_SYSTEM_DESIGN`，未批准 RED、Java/API/DDL/TDD/代码/测试/Consumer/Git。

`W3-04 / plan-r2.60` Reconciliation 系分候选与准出记录（2026-08-14）：回读 reconciliation face/impl、对账 batch/source/run/difference/Gate、clearing split/candidate/batch、settlement/payout/recovery 调用链以及真实 Consumer 后，确认当前 Reconciliation 只有调用方提交 `VERIFIED 1:1 assertion` 的 E2 固化能力，不能强制已接受的 Money/status/scope strict exact；pure reconciliation Difference 又被 Gate-only 限制。目标复用现有最小事实核，不新增规则引擎：来源 Owner 先归一，Provider 强制 1:1 strict exact 与 coverage，Difference action + 后继 current Balanced 才关闭，Gate 只作 exact-object 事务时点准入。clearing/settlement 保留规范化阶段事实并调用 W3-03 internal stage command；raw payout receipt/display/finality、责任 case 和 recovery policy 上移，通用 RecoveryOrder case API 目标删除并复用 ActionFact + Difference。独立 Checker 初审发现跨 Owner 比较 `DomainOutcome` 的一项 P1；改为 Owner-local outcome provenance 与规则作用域 comparison status 分层后，最终判定 `W3-04 SYSTEM DESIGN PASS / 0 P0-P2`。随后机械进入 `plan-r2.61 / W4-01_TDD_SCENARIO_AND_RED_DESIGN`；未批准 RED 执行、Java/API/DDL、代码/测试/Consumer/Git。

`W4-01 / plan-r2.62` TDD/RED 候选记录（2026-08-14）：在现有 TDD 主文档中保留前十九章作为当前实现与迁移资产，并新增目标权威基线和第二十章。候选用三类 RED、十三项共享 RED、七场景六维矩阵和现有资产处置表覆盖产品、DSL、W3 系分要求；内部资金链坚持真实 Bean/H2，HOST/E4/E5 不得用 Recording/Fake 变绿。TDD lightweight Harness、执行规格 gsd-wave Harness、目标系分 validator 和产品 validator 均通过；产品仅有非阻断 implementation language WARN，W4 专项结构断言为 `13 RED / 7 SIM / 10 required`，空白与新增链接检查无诊断。架构 validator 不提供 TDD kind，错误套用 `architecture-plan` 的 FAIL 不作为准出证据。本轮未执行测试、未修改 Java/API/DDL、生产/测试源码、Consumer 或 Git；当前等待独立 Checker，未进入 `W4-02`。

`W4-01 / plan-r2.63` 准出前源码校准记录（2026-08-14）：回读 `ExternalFundsEventApplicationServiceImpl/ExternalFundsRailResolver`、`ReconciliationRunResultApplicationServiceImpl` 与授权拒绝产品/测试红线后，修正三处承重口径：外部事实当前冲突只归属于 Funds 内 ACH/BANK raw status allowlist；对账当前冲突是 caller `VERIFIED + EXACT/RULE` assertion 未强制 Money/status/scope/rule evidence 即可导出 Balanced；零资金效果允许保留 Action/无 legs 解释证据，但必须零 Ledger/Balance 副作用。同时补齐十三项 RED 到真实测试层级、当前红灯依据和命令族的映射。该轮为 Maker 修订，状态仍是 `INDEPENDENT_CHECKER_PENDING`，未执行 RED、未修改 Java/API/DDL、生产/测试源码、Consumer 或 Git，未进入 `W4-02`。

`W4-01 / plan-r2.64` 独立 Checker 准出记录（2026-08-14）：首轮复核发现 1 项 P1，`WALLET-001` 未覆盖 Spend Control 准入/movement/消费真实流程，`STAGE-001` 未覆盖 clearing/settlement/payout 测试；TDD 命令清册拆分并补齐精确 slice、`test-balance-control`、`test-reconciliation` 与边界验证后，单点复核为 `PASS / 0 P0-P2`。本 PASS 只准入 `W4-02` 跨仓 L3/L4 验证计划，不表示 RED、目标 API、HOST/E4/E5、实现或 Git 获准。

`W4-02 / plan-r2.65` 跨仓 L3/L4 验证计划候选记录（2026-08-14）：回读当前 `capte-domain` POM、生产依赖、`OrderCouponRedemptionIntegrationTests`、`FundsProviderHostSchemaTests`、联合 H2、手工 Bean/Mapper/transaction manager 装配及 `fincone-issuing` 骨架后，确认只有 `CAPTE-BENEFIT` 具备 L3 计划资格；钱包仍有内部 Recording，VCC/ACH/收单/payout 没有真实 Consumer，Reconciliation/Stage 仍是 Provider 仓内 E3。TDD 第二十章据此冻结不可拆分 Artifact Lineage Card、五类宿主资格、九组 Benefit 最小执行矩阵以及 Bean/proxy/schema/tx/restart 停止线；旧 Snapshot hash、类加载探针和 historical tests 只保留 provenance。当前等待独立 Checker，未执行跨仓测试、RED，未修改 Java/API/DDL、生产/测试源码、Consumer 或 Git。

`W4-02 / plan-r2.66` 独立 Checker 准出记录（2026-08-14）：首轮发现 runtime lineage 未逐项覆盖 core、L3 context rebuild 与 L4 deployment restart 混层两项 P1；修正为每个实际承重 core/face/impl 的 binary/resolved/loaded SHA 逐项相等，并把 L3 耐久测试库上的 Consumer context rebuild 与 L4 目标数据库上的部署进程重启明确分离。单点复核又发现 source fingerprint 不能与 JAR SHA 比较的一项 P2；改为构建日志绑定 source 与 binary 后，最终判定 `W4-02 VALIDATION PLAN PASS / 0 P0-P2`。本 PASS 不等于 L3/L4 执行通过；当前 `L3_EXECUTION_GRANT=NO`，唯一下一入口是 Consumer Host Owner 对 `CAPTE-BENEFIT` Entry Card、跨仓构建/测试及必要 Consumer 测试修正给出授权。

`W4-02 / plan-r2.67` 执行授权与 Entry Card 记录（2026-08-14）：Consumer Host Owner 明确允许按建议推进，并允许独立会商。会商将最小证据闭包收敛为七个 Provider 承重制品、显式 `NONE` 零副作用、两个独立测试进程共用唯一 file H2 的上下文重建查证，以及 Bean/proxy/DataSource/transaction manager 观测；只允许必要的 Capte 测试/测试配置最小修正。当前按 `TDD 20.7.4` Entry Card 执行，未得到 fresh 结果前不宣称 L3 PASS；禁止联网、Git、生产 API/DDL/生产配置、L4、enable/release/production。

`W4-02 / plan-r2.68` L3 执行与独立 Checker 记录（2026-08-14）：Provider build-start combined fingerprint=`e6b34737...e8d`，唯一版本离线全 reactor `21/21 BUILD SUCCESS`；七个承重 JAR 的 built/resolved SHA 逐项一致，并由测试内 `CodeSource` 关闭 loaded path。Capte `45/45 BUILD SUCCESS`，fresh Surefire 共 `10 tests / 0 failures / 0 errors / 0 skipped`；两个独立 Maven/JVM/Spring context 使用同一 file-H2、schema init `always/never`，只按 Consumer 耐久引用恢复原事实且零新增 transaction/ledger/balance/refund。独立 Checker 判定 `CAPTE-BENEFIT L3 / E4 PASS / 0 P0-P2`，范围仅限当前 Provider 制品与 Capte 集中测试宿主。当时将 production composition root、正式 migration/schema、目标部署启动列为下一 blocker；`plan-r2.69` 根据 Owner 澄清将其校准为真实可部署 Consumer 的未来接入责任，不再作为 `capte-domain` 通用模块当前缺口。

`CAPTE-DOMAIN / plan-r2.69` 通用模块边界校准与版本解析记录（2026-08-14）：Human Owner 明确 `capte-domain` 是无独立生产数据库和部署进程的通用模块，`wind-funds:1.0.1-SNAPSHOT` 已发布，三个 Provider impl 应仅由集中测试装配。源码复核确认 `tests/pom.xml` 已以 test scope 引入 ledger/wallet/transaction impl，业务模块继续只依赖 core/face；Java 21 下执行 `mvn -U -pl tests -am -DskipTests compile`，Capte `42/42 BUILD SUCCESS`，七个 `1.0.1-SNAPSHOT` 承重 JAR 均从仓库解析并落入本地缓存。该结果关闭通用模块制品解析与编译门槛，不把测试 impl 依赖提升为业务模块 runtime 依赖；`P-SIM03-HOST`、数据库/装配/L4、enable/release/production 只在首个真实可部署 Consumer 出现时恢复。本轮仅修改正式设计与状态文档，未修改 Java、POM、测试或 Capte 工作区；独立 Checker 最终判定 `PASS / 0 P0-P2`。

`CAPTE-DOMAIN / plan-r2.70` 机械状态回写（2026-08-14）：`1.0.1-SNAPSHOT` 公共制品解析、Capte 通用模块编译与边界复核已关闭；当前没有需要在 `wind-funds` 或 `capte-domain` 补造的 starter、数据库、migration 或生产装配。计划停在 `WAITING_FOR_FIRST_DEPLOYABLE_CONSUMER`；只有首个真实可部署 Consumer 出现时，才按其实际宿主恢复 `P-SIM03-HOST`、VC/E4/E5 与 L4 准入，不据此进入 RED、API、实现、Git、enable/release/production。

`W5-01 / plan-r2.71` 主线纠偏与实现切片候选记录（2026-08-14）：Human Owner 确认原目标仍是 `wind-funds` 重构，并要求按“实现切片选择 -> RED -> 最小 Green”继续推进。`WAITING_FOR_FIRST_DEPLOYABLE_CONSUMER` 降为 `P-SIM03-HOST/L4` 子轨的延后状态，不再阻断仓内公共资金内核重构。首轮 Checker 指出调用清册、fee/FAILED 分支与 dirty overlap 证据不闭合；回读源码后，卡片改为覆盖同一 `pay` 方法的无费成功、带费 companion、准入后 `proven-zero` 失败和唯一生产 Benefit caller 回归，并引用逐文件 overlap authority。当前仍只等待独立 Checker，不执行 RED，不修改 Java/API/DDL、测试源码或 Git。

`W5-01 / plan-r2.72` Entry Card 准出记录（2026-08-14）：补齐带费 PAY 的本金/fee 两条既有 `primary` action identity 后，准入后任一 posting 拒绝必须使同 Attempt 全部 sibling action 共同 terminal `FAILED + proven-zero`，零 Ledger/Balance；同摘要共同复用，任一 UNKNOWN 继续受 Q-004 零新 Attempt。独立 Checker 最终判定 `PASS / 0 P0-P2`。该 PASS 只把唯一入口机械切到 `CI-W5-01-ACTIONFACT-001 + W5-01_RED_EXECUTION_GRANT`；Contract Owner 尚未接受两事实 fee 模型，API baseline overlap Owner 尚未处置，Human Owner 也尚未授予 RED 执行权，因此未执行 RED，未修改 Java/API/DDL、测试源码或 Git。

`W5-01 / plan-r2.73` Contract 与 RED 授权记录（2026-08-14）：Human Owner 回复“按你的建议推进”，接受 `CI-W5-01-ACTIONFACT-001`，允许保留并在后续获准 Green 时手工合并 `core/api-baseline/stable-api.txt` 的既有 dirty 内容，并授权本轮仅落目标测试 RED。RED 候选复用现有 `FundsTransactionQueryService`，不新增 query facade；只冻结按业务身份列出 ActionFact、再按返回的稳定 action identity 查询原事实的最小行为，不预先批准最终 Java 签名、DTO 或持久化。生产 Java/API/DDL、Green、Git、HOST/L4、enable/release/production均未授权。

`W5-01 / plan-r2.74` RED 执行记录（2026-08-14）：按 TDD `20.8` 白名单新增最小 ActionFact query 契约和 PAY/Benefit 流程断言，未修改生产 Java/API/DDL、API baseline、Consumer 或 Git。首轮 Java 21 聚焦 slice 为 `89 tests / 8 failures / 0 errors / 0 skipped`；独立 Checker 判定 `NOT PASS / 0 P0 / 1 P1 / 1 P2`，要求锁定带费首次/重放 identity、同业务 identity 变更 fee 语义冲突、舍入零 ActionFact，并澄清反射查询签名是待 Green Grant 审批的候选。修订后同一 slice 为 `89 tests / 9 failures / 0 errors / 0 skipped`，四个目标类仍全部只因 `FundsTransactionQueryService` 缺少 `findFundsActionFactsByBusiness` 失败，其余 80 个用例通过。该结果是目标 RED 证据，不是 Green 或实现完成；当前唯一入口为 `W5-01_RED_INDEPENDENT_CHECKER`，`GREEN_EXECUTION_GRANT=NO`。

`W5-01 / plan-r2.75` RED 独立准出记录（2026-08-14）：Checker 回读修订测试、Fresh Surefire 与停止线后判定 `W5-01 RED PASS / 0 P0-P2`。该 PASS 只证明候选测试在真实 Spring/H2 PAY/Benefit 路径上按缺 ActionFact query 的目标机制失败，且首次/重放 identity、fee 承重语义冲突、准入前零 ActionFact 和 sibling `proven-zero` 均已进入 Green 验收边界；不代表候选 Java 签名已被 Human Owner 接受，也不授权 Green。唯一下一入口为 `W5-01_GREEN_EXECUTION_GRANT`。

`W5-01 / plan-r2.76` Green 执行记录（2026-08-14）：Human Owner 接受 `queryFundsActionFacts(FundsActionFactQuery)` 与 `findFundsActionFact(FundsActionFactRef)` 并授权 Green。Maker 复用现有 terminal PAY root/detail/route snapshot 一致事实组提供 canonical ActionFact 只读投影，没有新增表、Mapper、写入链、facade、场景分支或 Consumer 改签；修订签名 RED 仍精准 `89/9/0/0`，Green 后为 `89/0/0/0`，交易基线、Public Contract 约规和 PMD 通过。当前只进入 `GREEN_IMPLEMENTATION_INDEPENDENT_CHECKER`；Wallet 边界基线的一项既有 `state/status` 失败独立保留，不冒充本切片通过或回归。

`W5-01 / plan-r2.77` Green 重做记录（2026-08-14）：首轮 Checker 判定 `NOT PASS / 0 P0 / 3 P1 / 1 P2`。Maker 保持无新表、Mapper、写链或 facade 的最小投影方案，只补全 PAY root/detail/route/Money/fee/ledger-ref/累计事实组校验，把 digest 与 route ref 对齐 DSL 结构化值，并把零效果收紧为 `LEDGER_POSTING_REJECTED + 全 sibling FAILED + 零 ledger ref + 零累计` 的耐久合取证据；泛化失败码和篡改/缺失事实均 fail-closed 空查询。新增边界与篡改测试后聚焦 `92/0/0/0`、交易回归 `149/0/0/0`、Java 21 compile `21/21`、Public Contract、PMD 和 diff check 均通过。当前仅进入 `GREEN_REWORK_INDEPENDENT_CHECKER`，不批准下一切片、Git、L4 或生产。

`W5-01 / plan-r2.78` Green rework 2 记录（2026-08-14）：上一轮 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 1 P2`，要求成功 PAY 不受后继 refund 累计覆盖、route/participant 完整校验端点与 tenant/币种，并停止把未知 detail hash 误标为 canonical digest。Maker 在同一只读投影内完成三项最小修正，无新表、Mapper、写链、facade 或 action kind；新增退款不变性、端点/tenant/fee target 篡改和 detail hash encoding 回归。fresh 聚焦 `93/0/0/0`、交易回归 `150/0/0/0`、Java 21 compile `21/21`、Public Contract 与 PMD 均通过。当前仅进入 `GREEN_REWORK_2_INDEPENDENT_CHECKER`，不批准下一切片、Git、L4 或生产。

`W5-01 / plan-r2.79` Green rework 3 记录（2026-08-14）：Green rework 2 Checker 判定 `NOT PASS / 0 P0 / 1 P1 / 0 P2`，唯一问题是 `RouteSnapshotJsonSupport` 可从缺失 `subjectRef` 的持久 JSON 构造空主体 participant，而查询匹配直接解引用并抛 NPE。Maker 仅在共享 participant/node/Money 匹配边界增加 null guard，并补充缺失 `participant.subjectRef` 时按业务身份与按 ActionFact identity 两个查询入口均空返回、恢复原快照后事实可重读的回归；无新 API、DTO、DDL、Mapper、写链、facade 或 action kind。fresh 聚焦 `93/0/0/0`、交易回归 `150/0/0/0`、Java 21 compile `21/21`、PMD 与 diff check 均通过。当前仅进入 `GREEN_REWORK_3_INDEPENDENT_CHECKER`，不批准下一切片、Git、L4 或生产。

`W5-01 / plan-r2.80` Green Checker PASS 状态回写记录（2026-08-14）：独立 Checker 对 rework 3 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。两个查询入口复用同一投影路径，缺失 `participant.subjectRef` 时均 fail-closed；refund immutability、PAY/FEE 端点与完整双射、tenant/currency 校验和 projection-owned digest 均保持。W5-01 ActionFact Foundation Green 至此关闭；下一 W5 Slice 必须单独形成 Entry Card，本 PASS 不授权 W5-02、下一轮 RED、Git、HOST/L4 或生产。

`W5-01-DIRECT-PRIMARY-RECOVERY-ACTIONFACT / plan-r2.81` Entry Card 形成记录（2026-08-15）：Human Owner 回复“可”，只授权形成下一最小切片卡片。卡片限定为 `referenceTransactionSn` 引用一个无费且真实成功的 PAY root，该 root 恰好投影一条 principal `primary/proven-full` ActionFact，且本次不新增手续费；direct primary return 按稳定 DSL 映射为 `recovery/adjustment`。无原引用业务确认型退款、原 PAY 带 fee 与本次新增手续费退款继续留在 Contract Inquiry，不以现有宽 `refund(...)` 方法签名扩大公共合同。Entry Card 不预批 Java/API；Contract 接受并取得后续独立授权后才可考虑最小 `transaction-face` DTO/query 调整。当前只等待 Entry Card 独立 Checker，不执行 RED，不修改 Java/API/DDL、测试源码或 Git。

`W5-01-DIRECT-PRIMARY-RECOVERY-ACTIONFACT / plan-r2.82` Entry Card Checker PASS 状态回写（2026-08-15）：独立 Checker 首轮发现原 PAY root 到 principal ActionFact 歧义、缺并发累计 RED、API 未预批与 eventual 合同实现边界混写三项 P1。卡片以最小范围限定原 PAY 无费且 root 恰好投影一条 principal ActionFact，补充并发 `60 + 60` 只一胜的逐事实验收，并明确 Contract 接受和后续独立授权后 eventual Green 才可最小调整 `transaction-face` DTO/query；DDL/Mapper/新写链继续排除。单点复核最终为 `PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Contract Inquiry 与 RED Grant 决策，不执行 RED，不修改 Java/API/DDL、测试源码或 Git。

`W5-01-DIRECT-PRIMARY-RECOVERY-ACTIONFACT / plan-r2.83` Contract 与 RED 授权记录（2026-08-15）：Human Owner 回复“可以，推进吧”，接受 `CI-W5-01-DIRECT-PRIMARY-RECOVERY-001`，并仅授权本轮目标测试 RED。RED 只覆盖原 PAY 无费且恰好一条 principal ActionFact、本次无新增 fee 的直接引用退款；用结构化原事实引用、allocated Money 与逐原 route provenance 候选冻结 eventual Green 合同。四个既有测试文件执行前 SHA 已登记于 TDD `20.9`；生产 Java/API/DDL/Mapper、API baseline、Consumer、Git、Green、HOST/L4、enable/release/production 仍未授权。

`W5-01-DIRECT-PRIMARY-RECOVERY-ACTIONFACT / plan-r2.84` RED 执行记录（2026-08-15）：按 TDD `20.9` 白名单增加 direct/Benefit recovery ActionFact 契约、累计、幂等、并发和原 PAY 带费停止线断言，未修改生产 Java/API/DDL/Mapper、API baseline、Consumer 或 Git。Java 21 fresh 聚焦 slice=`91 tests / 6 failures / 0 errors / 0 skipped`，其余 `85` 个用例通过；六项均精准指向缺失的 recovery ActionFact 合同/投影或原 PAY 带费未拒绝，不是环境或旧测试故障。改后 compile=`21/21`、diff check 通过。当前只进入 `RED_INDEPENDENT_CHECKER`，不授权 Green。

`W5-01-DIRECT-PRIMARY-RECOVERY-ACTIONFACT / plan-r2.85` RED rework 记录（2026-08-15）：首轮独立 Checker 判定 `NOT PASS / 0 P0 / 3 P1 / 1 P2`。Maker 只在既有 RED 白名单内补 recovery posting 拒绝的 `FAILED + proven-zero` 合同、事实混合/PROCESSING 的 UNKNOWN 空查询、原 fact/context/tenant/currency/route eligibility、non-PAY/非 proven-full/unreferenced 边界，并把原 PAY 带 fee 与本次新增 fee 两条已接受停止线都钉成 fail-closed。第二轮 Checker 又发现三组断言被更早目标红灯遮蔽；Maker 仅前移现有物理事实、冲突、查询边界断言，使其在目标红灯前实际执行，并明确 recovery identity/digest/provenance 后置断言留待 Green fresh 验证。fresh 聚焦 slice=`95 tests / 8 failures / 0 errors / 0 skipped`，其余 `87` 个用例通过；八项均精准指向未实现 recovery ActionFact 或手续费停止线。第三轮独立 Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。

`W5-01-DIRECT-PRIMARY-RECOVERY-ACTIONFACT / plan-r2.86` RED Checker PASS 状态回写（2026-08-15）：机械同步 metadata、PRD、系分、TDD、8.28、history 与 recovery；当前唯一入口切换为 `GREEN_EXECUTION_GRANT`，且 `GREEN_EXECUTION_GRANT=NO`。该状态只等待 Human Owner 接受或拒绝 Green，不授权生产 Java/API/DDL/Mapper、Consumer、Git、HOST/L4、enable/release/production。

`W5-01-DIRECT-PRIMARY-RECOVERY-ACTIONFACT / plan-r2.87` Green 执行记录（2026-08-17）：Human Owner 回复“按你的建议推”，接受最小 Java 形态并授权 Green。Maker 复用现有 terminal root/detail/route/Ledger 事实组提供只读 recovery ActionFact 投影，并在关联退款准入处硬停止原 PAY 带费和本次新增 fee；未新增 DDL、Mapper、写入链、场景 facade、Consumer 改签或 API baseline。fresh 聚焦 slice=`95/0/0/0`，交易基线=`156/0/0/0`，compile=`21/21`，PMD 与 diff check 通过；boundary=`200/1/0/0` 与 `verify-cad` 均只在既有 wallet/core `state/status` 差异处停止，该独立 blocker 未被本切片修改或冒充通过。当前唯一入口为 `GREEN_IMPLEMENTATION_INDEPENDENT_CHECKER`，不预写 Checker PASS，也不授权 Git、HOST/L4、enable/release/production。

`W5-01-DIRECT-PRIMARY-RECOVERY-ACTIONFACT / plan-r2.88` Green rework 记录（2026-08-17）：首轮 Green Checker 判定 `NOT PASS / 0 P0 / 1 P1 / 0 P2`。Maker 以一个耐久篡改 RED 同时证明 route、Money、累计三类漏洞会从列表和 identity 查询泄漏，随后在既有只读投影中校验精确反向 route、正数同币且不超原本金的 allocated Money、以及同一原 root 下全部已证明成功 recovery 累计与原 PAY `refundedAmount` 相等且不超本金。fresh 聚焦 slice=`96/0/0/0`，交易基线=`157/0/0/0`，compile=`21/21`，PMD 与 diff check 通过；没有新增 DDL、Mapper、写链或场景策略。当前只进入 `GREEN_REWORK_INDEPENDENT_CHECKER`，不预写 PASS。

`W5-01-DIRECT-PRIMARY-RECOVERY-ACTIONFACT / plan-r2.89` Green Checker PASS 状态回写（2026-08-17）：独立 Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 recovery route 精确反向、allocated Money 边界、逐原成功累计及 business/identity 两种查询 fail-closed 均闭合。该状态只关闭本切片 Green Checker；下一 W5 切片仍须独立 Entry Card，Git、HOST/L4、enable/release/production 均未授权。

`W5-01-AUTHORIZATION-ROOT-ACTIONFACT / plan-r2.90` Entry Card 形成记录（2026-08-17）：Human Owner 回复“可”，只授权按建议形成并独立复核下一最小切片。Maker 选择授权根而非完整授权生命周期：授权根是 complete/release 的必要原事实，现有终结 root/detail/RouteSnapshot/Ledger/Balance 证据可复用，且不需要新增 Intent/Attempt 表、ActionFact 写链或外部 authority 规则。当前不执行 RED，不修改 Java/API/DDL/Mapper、测试源码、Consumer 或 Git。

`W5-01-AUTHORIZATION-ROOT-ACTIONFACT / plan-r2.91` Entry Card Checker PASS 状态回写（2026-08-17）：首轮 Checker 的三项 P1 已以最小卡片修正关闭；原 P2 为两个 contract test 文件混淆，复算确认本卡 `FundsActionFactContractTests.java` SHA 正确。最终独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Human Owner 的 Contract Inquiry 与 RED Grant 决策，`RED_EXECUTION_GRANT=NO`；不执行 RED，不修改 Java/API/DDL/Mapper、测试源码、Consumer 或 Git。

`W5-01-AUTHORIZATION-ROOT-ACTIONFACT / plan-r2.92` Contract 与 RED Grant 接受记录（2026-08-17）：Human Owner 明确回复“可以，授权推进吧”，接受 `CI-W5-01-AUTHORIZATION-ROOT-001` 并只授权目标测试 RED。前置 Java 21 `just mvn-version` 与 `just compile=21/21` 通过；白名单和 forbidden-scope 指纹见 TDD `20.10`。当前只执行 RED，不授权 Green、生产 Java/API/DDL/Mapper、Consumer、Git、HOST/L4、enable/release/production。

`W5-01-AUTHORIZATION-ROOT-ACTIONFACT / plan-r2.93` RED 执行记录（2026-08-17）：只在授权白名单内补充 `FundsAuthorizationTransactionFlowTests` 与四份状态文档。首轮因测试夹具账户未建立和字段超长产生 `5 errors`，不计 RED 证据；修正夹具后 fresh slice=`45/5/0/0`，五个失败均为首次 authorization ActionFact 查询返回空，0 error/skip。`just compile=21/21`、`just pmd`、`git diff --check` 通过，production/Public API/schema forbidden fingerprint 与 before 完全一致。当前只进入 `RED_INDEPENDENT_CHECKER`，不授权 Green、生产代码、API/DDL、Consumer 或 Git。

`W5-01-AUTHORIZATION-ROOT-ACTIONFACT / plan-r2.94` RED Checker PASS 状态回写（2026-08-17）：独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认五个 RED 均先形成足够的真实授权物理事实，再精准失败于首次目标 ActionFact 查询；`40` 个可达通过用例与不可达后置断言披露、Surefire/hash、白名单、678-file forbidden 指纹和状态单一性均成立。当前唯一入口为 Human Owner `GREEN_EXECUTION_GRANT`，且 `GREEN_EXECUTION_GRANT=NO`；不授权生产实现、API/DDL、Consumer、Git、HOST/L4、enable/release/production。

`W5-01-AUTHORIZATION-ROOT-ACTIONFACT / plan-r2.95` Green 执行记录（2026-08-17）：Human Owner 在 RED Checker PASS 后明确授权推进最小 Green。Maker 只修改既有 `DefaultFundsTransactionQueryService` 只读投影与 `FundsAuthorizationTransactionFlowTests`，复用既有 face/DTO/写入事实，没有新增 API、DTO、DDL、Mapper、第二写链、场景 facade 或 Consumer 改签。fresh 聚焦 slice=`45/0/0/0`、交易回归=`162/0/0/0`、compile=`21/21`，PMD 与 diff check 通过；成功、拒绝、SHARED 双责任、生命周期稳定性和耐久篡改均越过 RED，其中缺失 legs 也 fail-closed。当前只进入 `GREEN_IMPLEMENTATION_INDEPENDENT_CHECKER`，不预写 Checker PASS，也不授权 Git、HOST/L4、enable/release/production。

`W5-01-AUTHORIZATION-ROOT-ACTIONFACT / plan-r2.96` Green rework 记录（2026-08-17）：首轮 Green Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 0 P2`，指出成功事实仍依赖可变 root lifecycle/后继累计，Funding/SHARED 责任校验过宽。Maker 先增加生命周期累计篡改仍保持原事实、单 Funding 责任篡改 fail-closed、SHARED parent currency 缺失 fail-closed 三项 RED，fresh 得到 `45/3/0/0`；随后只修改既有只读投影，并补缺失 participant subject 与权威 participant currency 的双查询防回归；既有可选 `SubjectRef.currency` 未被升级为必填。最终聚焦=`45/0/0/0`、transaction=`162/0/0/0`、compile=`21/21`、PMD 与 diff check 通过。当前只进入 `GREEN_REWORK_INDEPENDENT_CHECKER`，不预写 Checker PASS，也不授权 API/DTO/DDL/Mapper、Consumer、Git、HOST/L4、enable/release/production。

`W5-01-AUTHORIZATION-ROOT-ACTIONFACT / plan-r2.97` Green Checker PASS 状态回写（2026-08-17）：独立 Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认原 authorization ActionFact 不依赖后继 root state/累计，Funding/SHARED 责任、parent 关系与权威 participant currency 精确，缺失主体与币种时两种查询均 fail-closed。该状态只关闭本切片 Green Checker；下一 W5 Slice 必须独立形成 Entry Card，Git、HOST/L4、enable/release/production 均未授权。

`W5-01-AUTHORIZATION-COMPLETE-ACTIONFACT / plan-r2.98` Entry Card 形成记录（2026-08-17）：Human Owner 要求按本轮结论推进。Maker 选择 ordinary successful complete 的只读 ActionFact 投影作为下一最小切片：复用既有 authorization ActionFact、完成 sibling details、原 route replay provenance、Ledger 引用和 root 累计，不新增 query method、DTO、DDL、Mapper、写链、场景 facade 或 Consumer 改签。force/standalone completion、失败 proven-zero、release/refund 与外部 authority 均排除。当前只进入 Entry Card 独立 Checker，不执行 RED，不修改 Java、测试源码或 Git。

`W5-01-AUTHORIZATION-COMPLETE-ACTIONFACT / plan-r2.99` Entry Card Checker PASS 状态回写（2026-08-17）：首轮 Checker 的两项 P1 与两项 P2 已以最小卡片修正关闭：内部 detail `requestHash` 退出公共投影证据与 digest；complete group 对原 replayable HOLD legs、责任 siblings 与派生 target/CONSUME legs 做无重无漏映射；Contract Inquiry 补齐 identity、`factType`、`relationRole`、`provenanceRole`、digest domain/version；SHA manifest 使用完整路径。最终独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前唯一入口是 Human Owner 的 Contract Inquiry 与 RED Grant 决策，`RED_EXECUTION_GRANT=NO`；不执行 RED，不修改 Java/API/DTO/DDL/Mapper、测试源码、Consumer 或 Git。

`W5-01-AUTHORIZATION-COMPLETE-ACTIONFACT / plan-r2.100` Contract 与 RED Grant 接受记录（2026-08-17）：Human Owner 明确回复“按照这轮结论推进”，接受 `CI-W5-01-AUTHORIZATION-COMPLETE-001` 并只授权最小 RED。前置 Java 21 `just mvn-version` 与 `just compile=21/21` 通过；before manifest 与 `678` 文件 forbidden-scope 指纹见 TDD `20.11`。该阶段只执行 RED，不授权 Green、生产 Java/API/DTO/DDL/Mapper、其他测试、Consumer、Git、HOST/L4、enable/release/production。

`W5-01-AUTHORIZATION-COMPLETE-ACTIONFACT / plan-r2.101` RED 执行记录（2026-08-17）：只在授权白名单内补充四份权威文档与 `FundsAuthorizationTransactionFlowTests`。首轮 `4 failures / 2 fixture errors` 中，账户标识超长和 force amount 与既有策略不一致均不计 RED 证据；修正夹具后 fresh slice=`51/5/0/0`，五个失败分别覆盖 ordinary、partial lifecycle、SHARED、并发胜者和耐久篡改入口，均精准停在首次 complete ActionFact 查询返回空，`0 error/skip`。`just compile=21/21`、`just pmd`、`git diff --check` 通过，production/Public API/schema 的 `678` 文件 forbidden fingerprint 与 before 完全一致。当前只进入 `RED_INDEPENDENT_CHECKER`，不授权 Green、生产代码、API/DDL、其他测试、Consumer 或 Git。

`W5-01-AUTHORIZATION-COMPLETE-ACTIONFACT / plan-r2.102` RED rework 记录（2026-08-17）：首轮 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 2 P2`，指出目标红灯前物理事实不完整、负例被红灯遮蔽、after manifest 不完整和 identity 状态陈述过时。Maker 只在原白名单内补齐五个目标红灯前的 transaction/detail/route/posting/Ledger/Balance，并将 detail/累计/context、SHARED responsibility sibling、capture target 和 replay leg 的缺失/重复/交换/identity/Money 篡改拆成三个可达负例。fresh slice=`54/5/0/0`，`49` 个用例通过，五个失败仍精准停在首次 complete ActionFact 列表查询返回空；compile=`21/21`、PMD、diff check 与 `678` 文件 forbidden 指纹通过。当前只进入 `RED_REWORK_INDEPENDENT_CHECKER`，不预写 PASS，不授权 Green、生产代码、API/DDL、其他测试、Consumer 或 Git。

`W5-01-AUTHORIZATION-COMPLETE-ACTIONFACT / plan-r2.103` RED Checker PASS 状态回写（2026-08-17）：独立 Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认首轮 findings 全部关闭、三个负例独立可达、五个目标 RED 的物理前置完整、Surefire 与 after manifest 可复算且停止线未漂移。当前唯一入口为 Human Owner 的 `GREEN_EXECUTION_GRANT`，且 `GREEN_EXECUTION_GRANT=NO`；本 PASS 不授权 Green、生产 Java/API/DTO/DDL/Mapper、其他测试、Consumer、Git、HOST/L4、enable/release/production。

`W5-01-AUTHORIZATION-COMPLETE-ACTIONFACT / plan-r2.104` Green Checker PASS 状态回写（2026-08-17）：Human Owner 按建议推进并授权本卡最小 Green。Maker 只扩展既有 ActionFact 只读投影和授权流程测试；首轮 Green Checker 的 `3 P1 + 1 P2` 通过删除非权威 `requestHash` 准入、精确比较 replay Money、显式拒绝 FORCE marker、冻结 release/refund 前后 complete DTO 不变而关闭。fresh focused=`54/0`、transaction=`171/0`、compile=`21/21`，PMD 与 diff check 通过；最终独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。该 PASS 只关闭本切片；下一 W5 Slice 必须独立 Entry Card，Git、HOST/L4、enable/release/production 均未授权。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.105` 迁移基线成包记录（2026-08-17）：Human Owner 接受“先完成文档再恢复代码”的阶段校准。Maker 未建立下一代码 Entry Card，只在四份权威文档补齐 `MIG-00~09` 跨模块迁移总表、六类 action kind 当前覆盖、`D-MIG-001` 物理承接 A/B/C 决策包、Consumer 切换、旧路径下线条件和剩余 TDD 矩阵。当前决策包仍待独立 Checker，Checker PASS 后才进入 `D-MIG-001` Owner Gate；Java、测试、Public API/DTO/DDL/Mapper、Consumer、Git、HOST/L4、enable/release/production 均未授权。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.106` 迁移基线返工记录（2026-08-17）：首轮 `D-MIG-001` Checker 判定 `NOT PASS / 0 P0 / 4 P1 / 1 P2`。Maker 仅修订四份权威文档：为 `MIG-02~09` 补执行/验证 Owner、写入范围、验证和回退；把 A/B 收敛为项目级单选并禁止按动作形成 C；补跨版本 projector/schema、digest 与重启稳定性合同和 TDD；登记 Decision Register，并明确 root/detail 在 A/B 下的条件性定位。当前仍为 `D-MIG-001_DECISION_PACKAGE_REWORK_CHECKER_PENDING / OWNER_DECISION_PENDING / CODE_FREEZE`，不预写 Checker PASS 或 Owner 选择。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.107` 决策包 Checker PASS 状态回写（2026-08-17）：独立 Checker 判定 `D-MIG-001 DECISION_PACKAGE PASS / 0 P0 / 0 P1 / 0 P2`，确认 `MIG-02~09` 责任与回退、A/B 项目级单选、跨版本事实稳定合同、Decision Register 和 root/detail 条件性定位均已闭合。当前机械进入 `D-MIG-001_OWNER_GATE`；`accepted_answer=none`，A 仅为推荐，B 仍可选，代码冻结及全部实现/宿主/发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.108` Owner 接受 A 状态回写（2026-08-17）：Human Owner 明确选择 `D-MIG-001-A / EXISTING-DURABLE-FACT-PROJECTION`。现有 durable action group 继续作为唯一物理事实源，ActionFact 仅作版本化、机械可证的稳定公共只读投影；B/C 未选择且不是 fallback。当前只进入 Acceptance Checker，代码冻结及全部实现/宿主/发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.109` A Acceptance Checker PASS 状态回写（2026-08-17）：独立 Checker 判定 `D-MIG-001-A ACCEPTANCE PASS / 0 P0 / 0 P1 / 0 P2`，确认唯一物理事实源、版本化机械投影、B/C 非 fallback、不可证即停止、跨版本事实不变和全部停止线均闭合。当前唯一入口机械切为 authorization release 纯文档卡；代码冻结及全部实现/宿主/发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.110` MIG-02B release 文档卡成包记录（2026-08-17）：Maker 仅修订产品、DSL、系分、TDD 与本 OpenSpec，冻结 ordinary authorization release 的原授权、Money/累计、HOLD/RELEASE route provenance、重放/冲突/UNKNOWN、跨版本与排除边界。当前 `REVERSAL` 执行链仅作 E2 基线，query service 尚无 release ActionFact projector；状态为 `DOCUMENT_CARD_CHECKER_PENDING / CODE_FREEZE`。未建立代码 Entry Card，未修改 Java/测试/Public API/DTO/DDL/Mapper、新写链、Consumer 或 Git。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.111` MIG-02B 文档卡返工记录（2026-08-17）：首轮独立 Checker 判定 `NOT PASS / 0 P0 / 1 P1 / 0 P2`，指出 release 被误写为原 authorization Intent 的新 Attempt，与已接受 Q-004 冲突。Maker 仅修订五份权威文档和既有 DSL 样例：release 改为独立 reverse Intent，唯一引用成功 authorization；release Attempt 属于该 reverse Intent，digest 同时固定 release intentRef 与原 authorization ref。当前进入 rework Checker，代码与全部实施停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.112` MIG-02B Checker PASS 状态回写（2026-08-17）：最终独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认首轮 reverse Intent P1 关闭且无新 finding。release 卡只准出 ordinary authorization 未完成范围的产品/DSL/系分/TDD 合同，当前 query service 仍无 release ActionFact projector。唯一入口切为 MIG-02C refund 纯文档卡；代码与全部实施/宿主/发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.113` MIG-02C refund 文档卡成包记录（2026-08-17）：Maker 仅修订产品、DSL、系分、TDD 与本 OpenSpec，冻结 ordinary authorization refund 的独立 reverse Intent、`1..n` successful complete 逐笔分配、原责任/route、逐原累计、UNKNOWN 与跨版本合同。一手源码确认当前 request/instruction/lifecycle 只有 authorization-root 引用和根级上限，无耐久逐 complete allocation；因而现有 `AUTH_REFUND` 不得投影 canonical refund ActionFact，并命中 `D-MIG-001-A` 重开条件。当前只进入文档独立 Checker，不建立 Entry Card/RED/Green，不修改 Java、测试、API/DTO/DDL/Mapper、Consumer 或 Git。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.114` MIG-02C Checker PASS 与 `D-MIG-001-R` 决策包成包记录（2026-08-17）：独立 Checker 判定 MIG-02C `PASS / 0 P0 / 0 P1 / 0 P2`。Maker 随后只在五份权威文档形成 A“延期 canonical 投影”、B“现有唯一 durable group 版本化补证”、C“全项目事实源切换”三个同构候选，推荐 A 但保持 `accepted_answer=none`。当前只进入决策包独立 Checker；代码、测试、Public API/DTO/DDL/Mapper、新写链、Consumer、Git 与宿主/发布授权均未开放。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.115` `D-MIG-001-R` 决策包 Checker PASS 状态回写（2026-08-17）：独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认三候选只改变耐久承接与时点，A 不偷删 root-level 执行，B 保持现有唯一 durable group 且历史不猜回填，C 只能全项目切换，混合双真相与按 action fallback 均被拒绝。当前机械进入 Human Owner Gate；A 仍仅推荐，`accepted_answer=none`，代码与全部实施/宿主/发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.116` `D-MIG-001-R-A` Owner 接受记录（2026-08-17）：Human Owner 明确接受 A，并说明发卡行或上层业务并不保证提供可定位到具体 complete 的退款来源，`wind-funds` 不能强行关联。Maker 仅把接受范围同步到五份权威文档：保留 root-level refund 执行，缺少权威逐 complete 分配时 canonical query 空/UNKNOWN；B/C 未选择且非 fallback。MIG-03 文档前置已由 W3-01 与 `D-MIG-001-A` 关闭；Acceptance Checker PASS 后唯一机械下一入口冻结为 `MIG-04_TRANSACTION_WALLET_OWNERSHIP_DOCUMENT_CARD / DOCUMENTATION_ONLY / CODE_FREEZE`。当前代码和全部实施/宿主/发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.117` `D-MIG-001-R-A` Acceptance Checker PASS 状态回写（2026-08-17）：独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认缺少权威逐 complete 退款来源时不强行关联、root-level refund 保留、B/C 非 fallback、未来只按项目级证据重开及全部停止线均闭合。当前机械进入 `MIG-04_TRANSACTION_WALLET_OWNERSHIP_DOCUMENT_CARD / DOCUMENTATION_ONLY / CODE_FREEZE`，不授权任何实现。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.118` MIG-04 Transaction/Wallet ownership 文档卡成包记录（2026-08-17）：Maker 只修订产品、DSL、系分、TDD 与本 OpenSpec，按真实源码调用冻结 Wallet 准入/控制、Transaction 动作编排、Ledger 账务/余额窄读和 Host 场景适配的 Owner；将 PaymentInstrument facade、Spend control 协调器、Ledger wrapper/profile 与 Benefit facade 分为四个原子切换组。当前 PaymentInstrument 无生产 Consumer、Ledger 窄读待 MIG-05、Benefit 待 MIG-08 可部署 Consumer E4/实际调用切换，均保持阻断；现有集中测试宿主 E4 不等于可部署 Consumer 准出。未建立 bridge、alias、双读/双写或代码 Entry Card。当前只进入 MIG-04 文档独立 Checker，Java、测试、API/DTO/DDL/Mapper、Consumer、Git 与宿主/发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.119` MIG-04 文档卡 Checker PASS 状态回写（2026-08-17）：独立 Checker 首轮发现旧 TDD 仍把交易 facade 写成 Wallet 目标 Owner，以及物理 reversal/canonical release 名词混用；Maker 最小修正为迁移前 wallet-face 入口和 `reversal（投影为 canonical release）` 后，单点复核为 `PASS / 0 P0 / 0 P1 / 0 P2`。该 PASS 只关闭模块归属、四个原子组和停止线，PaymentInstrument 真实 Consumer、MIG-05 Ledger contract、MIG-08 可部署 Consumer E4 blocker 继续有效。当前机械进入 MIG-05 纯文档卡，不授权 MIG-04 实现或任何代码/宿主/发布动作。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.120` MIG-05 Ledger internalization 文档卡成包记录（2026-08-17）：Human Owner 要求按建议推进，并要求同步评估交易投影的合理性、可用性、业务价值与行业参考。Maker 只修订产品、DSL、系分、TDD 与本 OpenSpec，选择同库、按需、source-owned narrow read：`FundsActionFact` 继续作为 Transaction 唯一 durable action group 的版本化只读投影；Wallet Ledger wrapper/profile 目标退出，Ledger 目标上通过 core/internal 方向提供 action-scoped evidence 并拥有 profile/posting/Ledger/Balance。独立 Checker 指出当前 `core` 尚无该 read port，故它被明确冻结为未来 Contract Inquiry/Entry blocker；同时 Reconciliation 的真实调用与 Governance 的允许依赖方向已分开记录。Stripe、Modern Treasury 与 Microsoft CQRS/Materialized View 官方资料只用于校准分层和不提前拆读库的边界，不复制第三方对象。当前只进入文档独立 Checker；不建立 Entry/RED/Green，不修改 Java、测试、API/DTO/DDL/Mapper、新写链、Consumer 或 Git。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.121` MIG-05 文档卡 Checker PASS 状态回写（2026-08-17）：独立 Checker 首轮发现当前不存在的 `core` action-scoped read port 被写成已有端口，以及 Reconciliation 真实调用与 Governance 空 POM 依赖混写两项 P1。Maker 将前者改为未来独立 Contract Inquiry/Entry blocker，将后者拆分并把 Governance 空依赖纳入未来原子清理；残留三处旧措辞同步修正后，最终复核为 `PASS / 0 P0 / 0 P1 / 0 P2`。MIG-06 继续被 Host evidence 阻断，当前只进入 `MIG-07_RECONCILIATION_STAGE_DOCUMENT_CARD / DOCUMENTATION_ONLY / CODE_FREEZE`；不授权 MIG-05 Contract Inquiry、实现或任何代码/宿主/发布动作。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.122` MIG-07 Reconciliation/Stage 文档卡成包记录（2026-08-17）：Human Owner 经逐项 grill 全部选择 A，冻结 carrier-independent SourceSnapshot/NormalizedComparisonFact、`1:1 strict-exact`、rule-scoped ComparisonStatus、append-only responsibility-neutral Difference、versioned multi-pair GateRequirement 和 Stage 本地事务 mandatory Gate check；并明确文件只是一种 carrier、当前兼容不纳入目标设计。Maker 仅修订产品、DSL、系分、TDD 与本 OpenSpec，当前 Provider caller assertion/单 run Gate 只作 E2 差距，不冒充目标实现。当前只进入独立文档 Checker；MIG-06/08、Java/API/DTO/DDL/Mapper、测试、Consumer、Git、HOST/L4 与发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.123` MIG-07 首轮 Checker NOT PASS 与最小返工记录（2026-08-18）：独立 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 0 P2`。第一项 P1 是 strict-exact 未机械冻结 claim kind、economic component 与 direction，可能把 principal/fee/tax 或相反方向的同金额同状态事实误配；第二项 P1 是两侧 Source Adapter 只有单侧状态映射责任，没有共同 Comparison Rule Owner，可能把不同含义映射成同名 status。Maker 仅在五份权威文档补入 comparison semantics、ComparisonRuleRef/Owner、semantic digest 覆盖和未来负例；没有冻结 Java 字段、修改代码或运行测试。当前只进入返工独立 Checker；全部实现、Consumer、Git、HOST/L4 与发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.124` MIG-07 返工 Checker PASS 状态回写（2026-08-18）：独立 Checker 复核 comparison semantics 已进入 NormalizedComparisonFact、strict-exact、comparison identity 与 semanticDigest，Pair Comparison Rule Owner/Ref 已绑定双侧 source roles/namespaces、DomainOutcome mapping、ComparisonStatus、scope/effective period 与 version，未来负例覆盖缺失、陈旧、多命中、同名状态无共同规则及 principal/fee/tax/direction 错配，最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。本 PASS 只关闭 MIG-07 纯文档卡；MIG-06/08 真实 Host/Consumer evidence 仍缺失，当前进入等待，不授权 Contract Inquiry、Entry/RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.125` MIG-06/08 Host/Consumer 证据盘点成包记录（2026-08-18）：Maker 只读盘点 `wind-funds`、`capte-domain`、`fincone`、`fincone-issuing` 与本机 Capte Maven/Java 引用。结论是 Provider 内部 Stage、通用库 test host、docs-first 权威仓和 runtime skeleton 均不满足可部署 Consumer 定义，本机也未发现其他当前 Reconciliation Consumer；因此形成 `NO_ELIGIBLE_CONSUMER_FOUND / KEEP_WAITING`，当前只进入独立证据 Checker，不授权任何实现、测试、Git、宿主或发布动作。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.126` MIG-06/08 Host/Consumer 证据盘点 Checker PASS 状态回写（2026-08-18）：独立 Checker 复核 Provider 内部 E2、`capte-domain` 通用库测试宿主、`fincone` docs-first 权威仓、`fincone-issuing` runtime skeleton 与本机 Capte 引用范围，判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前没有满足准入定义的可部署 Consumer，结论保持 `NO_ELIGIBLE_CONSUMER_FOUND / KEEP_WAITING`；只有真实 Consumer Owner 提供仓库、目标 Stage、source pair、Comparison Rule Owner、数据库/schema/transaction、部署和恢复证据后，才恢复新的只读资格复核。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.127` MIG-06/08 Consumer 角色校准文档卡成包记录（2026-08-18）：Human Owner 明确 `capte-domain` 是当前实际 Consumer，`fincone` 只承担设计和 `wind-funds` 模拟 Consumer。Maker 复核 production call sites、测试宿主和 Reconciliation 零调用后，将前者定为真实库/目标 Contract Consumer 与未来 E4 宿主，将后者定为设计权威/模拟 Consumer，并把 design、Contract、E4、deployable L4 四层 Gate 拆开。首个 Contract 种子复用 Benefit funding handoff 与 FundsActionFact 的 strict-exact 核对，Ledger/Balance 保持正交；不新增 DSL、API、代码或测试。当前只进入独立文档 Checker。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.128` MIG-06/08 Consumer 角色校准 Checker PASS 状态回写（2026-08-18）：独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 `capte-domain` 的真实库 Consumer/目标 E4 宿主定位、`fincone` 的设计模拟定位、四层 Gate、Benefit handoff vs FundsActionFact 首个 pair 与 Ledger/Balance 正交边界均成立。当前只机械进入 `CI-MIG07-RECONCILIATION-001 / CONTRACT_INQUIRY / CONTRACT_DECISION_PENDING / DOCUMENTATION_ONLY / CODE_FREEZE`；不授权实现、测试、Consumer、Git、L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.129` `CI-MIG07-RECONCILIATION-001` 决策包成包记录（2026-08-18）：Maker 回读当前 Batch/Source/Run/Gate face、impl 与 tests，确认现实现由 caller 提交 `EXACT_MATCH/RULE_MATCH`，Provider 只校验引用覆盖，且单-run Gate 可由 caller 选取。五份权威文档据此同构比较 A“Provider 从 normalized facts 计算 strict-exact”、B“typed assertion + Provider 重算”和 C“保留 caller assertion”，推荐 A 但保持 `accepted_answer=none`；同时冻结 breaking replace、carrier-neutral source、Benefit `60 CNY` 模拟、不可裁剪 GateRequirement、价值 GO/NO-GO 与 Source/Run、Gate 分片边界。当前只进入独立决策包 Checker，未修改 Java、测试、API/DTO/DDL/Mapper、Consumer 或 Git。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.129` `CI-MIG07-RECONCILIATION-001` 决策包首轮 Checker 与最小返工记录（2026-08-18）：独立 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 1 P2`。双侧 UNKNOWN 可能被相等比较误判 Matched、caller 可能选择旧窄 GateRequirement，以及开放 blocking policy 会形成首包策略引擎。Maker 仅补入结构性 `comparisonProven` 及摘要覆盖、exact stage identity 唯一 current/effective requirement head、全部 required pair 固定合取和对应负例；删除开放 blocking policy/pair-level blocking，不改变 A/B/C 选择轴，不修改 Java、测试、API/DTO/DDL/Mapper、Consumer 或 Git。当前只进入返工独立 Checker。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.130` `CI-MIG07-RECONCILIATION-001` 决策包最终 Checker PASS 状态回写（2026-08-18）：返工 Checker 先发现有限 MatchResult 未显式包含 `NOT_COMPARABLE` 的单点 P1；Maker 将 `comparisonProven=false` 收敛为不可变 `NOT_COMPARABLE` 结果，并纳入 result digest、member coverage、查询和 run `DIFFERENCE_FOUND`。最终独立复核为 `PASS / 0 P0 / 0 P1 / 0 P2`。当前机械进入 Human Owner Gate；A 仍仅推荐，`accepted_answer=none`，不授权 Entry/RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.131` `CI-MIG07-RECONCILIATION-001-A` Owner 接受回写（2026-08-18）：Human Owner 明确回复 `A`，接受 Reconciliation Provider 从两侧已归一、已证明的 snapshots/facts 计算 strict-exact，删除 caller MatchResult assertion；B/C 未选择且不是运行时 fallback。接受不替代 Source Adapter authority/normalization，不批准具体 Java/API/DTO/DDL/Mapper、测试或 Consumer 修改。当前只进入 Acceptance Checker，代码与 HOST/L4/发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.132` `CI-MIG07-RECONCILIATION-001-A` Acceptance Checker PASS 状态回写（2026-08-18）：独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 Provider 是 strict-exact 唯一计算权威但不无条件信任上游、UNKNOWN/缺失/冲突/陈旧规则 fail-closed、B/C 非 fallback，且 Gate/Ledger/Balance/Stage/external finality 正交。当前只进入 Source/Run 独立 Entry Card 编制；GateRequirement 必须后续另卡，所有实现、Consumer、Git、HOST/L4 与发布停止线不变。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.133` `W5-MIG07-SOURCE-RUN-STRICT-EXACT-001` Entry Card 成包记录（2026-08-18）：Maker 回读当前 Batch/SourceSnapshot/RunResult/MatchResult face、impl、schema 和 Spring/H2 tests，确认 source item 仅有 ref/digest、caller 仍提交 quality/strength/difference/severity 且 Provider 不比较 Money/status/semantics。五份权威文档据此冻结单一最小 Contract 候选、`MIG07-SR-CONTRACT-001 + SR-001~009` 测试矩阵、原位单表族 breaking migration、写入白名单、整切回退和 Source/Run/Gate 共享迁移屏障；明确 severity/责任不是 strict-exact 事实，GateRequirement 继续另卡。当前只进入 Entry Card 独立 Checker，不授权 Contract 接受、RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.133` Entry Card 首轮 Checker 与最小返工记录（2026-08-18）：独立 Checker 判定 `NOT PASS / 0 P0 / 3 P1 / 0 P2`，指出逐事实 rule ref、差额方向/公式和生产 DDL 白名单尚不能机械执行。Maker 只补入逐事实 `ComparisonRuleRef` 及三方一致校验、`MONEY_MISMATCH` 的正绝对差额与 `REFERENCE/COMPARISON` 较大侧、对应 digest/RED，以及生产 create/verify SQL 条件白名单；未改变 A、未预批 Java/DDL、未修改代码或测试。当前仍停留 Entry Card 独立 Checker。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.133` Entry Card 单点复核与第二次最小返工记录（2026-08-18）：原三项 P1 已关闭；Checker 发现异币种不能计算绝对差额的单点 `P1`。Maker 将有限结果补为 `CURRENCY_MISMATCH`，明确不做 FX、差额与较大侧为空；`MONEY_MISMATCH` 只处理同币种金额不等，并同步 RED/result digest 断言。当前仍停留 Entry Card 独立 Checker。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.134` `W5-MIG07-SOURCE-RUN-STRICT-EXACT-001` Entry Card Checker PASS 状态回写（2026-08-18）：最终独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`；逐事实 rule ref、同币差额/异币分类、生产 DDL 条件白名单和 Source/Run/Gate 共享迁移屏障均已闭合。当前只进入 Human Owner 的 `CI-MIG07-SOURCE-RUN-STRICT-EXACT-001 / CONTRACT_INQUIRY`；精确 Java/DDL 未接受，RED/Green、代码、测试、Consumer、Git、HOST/L4 和发布仍未授权。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.135` Source/Run Contract Owner 接受回写（2026-08-18）：Human Owner 明确回复“可”，接受 8.41 与系分 11.15 冻结的唯一 Source/Run surface：carrier-neutral normalized facts、Provider-computed strict-exact、有限 result kind、无 caller assertion、同一既有表族破坏式切换。当前只进入独立 Contract Acceptance Checker；不授权 Gate contract、RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 或发布。Checker PASS 后唯一下一入口为 `W5-MIG07-GATE-REQUIREMENT-001 / ENTRY_CARD_REQUIRED / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.136` Source/Run Contract Acceptance Checker PASS 状态回写（2026-08-18）：独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 Owner 接受范围、Provider/Source Adapter 权威边界、carrier-neutral strict-exact、有限结果与单表族 breaking migration 均成立，且未扩张为 Gate、兼容层、RED/Green 或实现授权。当前唯一入口切换为 `W5-MIG07-GATE-REQUIREMENT-001 / ENTRY_CARD_REQUIRED / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.137` `W5-MIG07-GATE-REQUIREMENT-001` Entry Card 成包记录（2026-08-18）：Maker 回读当前 Gate face/impl、single-run lineage schema、Difference 查询、全部已知清分/清算/结算/出款调用与 Gate tests，确认 caller 当前可选择单一 run 且没有 versioned multi-pair Requirement。五份权威文档据此冻结唯一最小候选：exact Stage action identity、immutable requirement/pair、stage-scoped current head、caller 零 run selection、Provider 自动合取、Stage 本地 mandatory check 与 consumed evidence；持久化只增加 Requirement 三表与一条 Stage evidence snapshot，不建策略引擎、future scheduler、PASS token、V2 或第二对账内核。当前只进入 Entry Card 独立 Checker；Contract 接受、RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 与发布仍未授权。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.137` GateRequirement Entry Card 首轮 Checker 与最小返工记录（2026-08-18）：独立 Checker 判定 `NOT PASS / 0 P0 / 3 P1 / 1 P2`，指出 RequiredPair 缺 scopeIdentity、Requirement identity/版本唯一键/evidence 重放不闭合、失败全量回滚与确定性 proven-zero 拒绝事实冲突，以及现有 Gate caller 未逐项映射。Maker 只在五份权威文档恢复 `scope + pair + rule`、冻结 Provider identity 与 tenant+stageRef+version 唯一键及 semantic/evidence 双摘要、拆分 Gate BLOCKED/确定性拒绝/UNKNOWN，并补齐全部现有 caller 的 stageKind/identity/inspect-check/evidence Owner/退役处置表；未修改 Java、测试、API/DTO/DDL/Mapper、Consumer 或 Git。当前仍停 Entry Card 独立 Checker。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.138` GateRequirement Entry Card Checker PASS 状态回写（2026-08-18）：独立 Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 RequiredPair scope、Requirement 双摘要/并发/CAS、全量 Stage caller 映射及 `007A/007B/007C` 分层失败契约闭合。当前只机械进入 `CI-MIG07-GATE-REQUIREMENT-001 / CONTRACT_INQUIRY / CONTRACT_DECISION_PENDING`，不授权 RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.139` GateRequirement Contract Owner 接受回写（2026-08-18）：Human Owner 明确接受 8.42 的 GateRequirement 公共 Contract，接受范围仅包括 exact Stage identity、versioned mandatory scope+pair、Provider current-head 解析、Stage 事务内重新 check、consumed evidence 与 007A/007B/007C 失败分层。当前只进入独立 Contract Acceptance Checker，不授权 RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.140` GateRequirement Contract Acceptance Checker PASS（2026-08-18）：独立只读 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认接受范围、007A/007B/007C 失败分层、Gate 与 Source/Run/Ledger/Balance/Stage/external finality 正交边界及五份权威文档状态一致。当前只进入共同 `W5-MIG07-SOURCE-RUN-GATE-BREAKING-RED-ENTRY-CARD / ENTRY_CARD_REQUIRED`，不授权 RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.141` 共同 breaking-release RED Entry Card 冻结（2026-08-18）：固定 `HEAD`、69 路径 dirty manifest 与 SHA-256、未来写入白名单、禁止范围、RED 命令、目标红灯和整切回退；当前只进入独立 RED Entry Card Checker，不授权 RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.142` 共同 breaking-release RED Entry Card Checker PASS（2026-08-18）：独立只读 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，复算 `HEAD`、69 路径 dirty manifest 与 SHA-256 一致，确认 Source/Run 与 Gate 目标红灯、shared/RED 分层白名单、全量 Stage caller、生产/H2 schema、Justfile 命令和整 release 回退闭合。当前只进入 Human Owner 的 RED Execution Grant 决策；Checker PASS 不授权 RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.143` baseline repair Entry Card re-freeze（2026-08-19）：Human Owner 仅授权 `BASELINE-REPAIR-ENTRY-CARD-REFREEZE-001`。Maker 核实三个 status 映射修复切片均已通过独立 Checker，记录 compile `21/21`、旧宿主 `85/0F/0E`、纯契约 RED `8/7F/0E/1P`、联合切片 `93/7F/0E`、reconciliation `236/0F/0E/0S`，并以稳定 live checkout 的默认 `59/50fde3...` 与 `-uall 60/bc904d...` 双口径重冻 Entry Card。当前只进入 Entry Card re-freeze 独立 Checker；不追认 RED 写入，不授权 RED/Green、Java、测试、API/DTO/DDL/Mapper、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.144` baseline repair Entry Card re-freeze Checker PASS（2026-08-19）：独立只读 Checker 首轮指出系分残留一个旧“当前入口”，Maker 仅将其历史化；单点复核最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。五份权威文档、双 porcelain 口径、三组 status 映射修复、验证计数、RED 非追认和残余边界一致。当前只进入 Human Owner 的 RED Execution Grant 决策；`RED_EXECUTION_GRANT_NO`，不授权 Green、实现、测试写入、Git、HOST/L4 或发布。

`W5-MIG07-SOURCE-RUN-GATE-BREAKING-RED-EXECUTION-001 / plan-r2.145` RED 执行与独立 Checker PASS（2026-08-19）：Human Owner 仅授予当前冻结 Entry Card 的 RED Grant。Maker 未修改生产或测试源码，复用既有两份 PublicContract RED；Java 21 `mvn-version` 与 compile `21/21` 通过，fresh 纯契约=`8/7F/0E/1P`、联合切片=`93/7F/0E`、全量 reconciliation=`236/0F/0E/0S`。7 项失败仅命中旧 caller assertion、单 run Gate 及缺 Requirement/strict-exact 公共合同；独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Human Owner 的 Green Execution Grant 决策，`GREEN_EXECUTION_GRANT_NO`；不授权实现、测试继续写入、Git、HOST/L4 或发布。

`W5-MIG07-SOURCE-RUN-GATE-BREAKING-GREEN-ENTRY-CARD-REWORK-001 / plan-r2.146` 文件级 Green Entry Card 返工（2026-08-19）：Human Owner 只授权 documentation-only rework，并校准测试边界：两个 PublicContract tests 永久不可修改；既有 legacy tests 可在未来 Green 中按 accepted breaking contract 机械迁移 setup/call，但不得弱化业务行为。Maker 回读 Source/Run/Gate face/impl/schema、全部已知 production/test direct callers 与 Justfile，按 TDD 20.22.5 冻结七项 RED 一对一映射、精确 ADD/MODIFY/DELETE 路径、caller old/new/保持行为、schema 与固定验证计数；额外发现 `SettlementPublicContractTests` 直接冻结旧 release run 字段，按同契约补入。当前只进入独立文件卡 Checker；`GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`，未修改生产/测试/schema/Consumer/Git。

`W5-MIG07-SOURCE-RUN-GATE-BREAKING-GREEN-ENTRY-CARD-REWORK-001 / plan-r2.146` 文件卡 Checker NOT PASS 与返工转向（2026-08-19）：独立 Checker 判定 `NOT PASS / 0 P0 / 4 P1 / 1 P2`。主要缺口是七个 contract RED 只约束 API surface，不能证明 strict-exact/GateRequirement 行为；breaking DELETE 缺 clean build，Public Contract 约规与实际可执行测试入口未冻结，Justfile 表数注释也会陈旧。Human Owner 明确不考虑任何兼容问题，Maker 因此没有增加 V2/bridge，而是把后续收敛为同一最终 breaking release 内的 surface hard break、行为 RED、行为 Green 三个不可发布检查点。

`W5-MIG07-BREAKING-BEHAVIORAL-RED-ENTRY-CARD-REWORK-001 / plan-r2.147` 无兼容行为 TDD 卡成包与 Checker PASS 记录（2026-08-19）：五份权威文档保留 r2.146 已审计最终文件集合，新增 TDD 20.22.6 的两个行为测试文件与 19 个顶层场景，冻结 clean compile、Public Contract 约规、Justfile comment-only 表数同步和三检查点授权顺序。本地一手执行记录还确认 `MAVEN_OPTS=-DskipSurefireReport=true + Java 21 + just test-one/verify-slice` 已进入 Surefire 并产出 fresh XML，TDD 据此冻结精确命令，不再使用未验证参数。独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`；surface Green、行为 RED/Green、Java、测试、schema、Consumer、Git、HOST/L4 与发布均未授权。

`W5-MIG07-BREAKING-CONTRACT-SURFACE-GREEN-EXECUTION-GRANT / plan-r2.148` 机械状态回写（2026-08-19）：在 r2.147 无兼容三检查点文件卡独立 Checker PASS 后，五份权威文档只同步当前状态与恢复入口。当前唯一动作是 Human Owner 裁决 contract surface hard break Green Grant；`CONTRACT_SURFACE_GREEN_EXECUTION_GRANT_NO / BEHAVIORAL_RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`，未修改生产或测试代码，也未授权 Git、HOST/L4 或发布。

`W5-MIG07-BREAKING-CONTRACT-SURFACE-HARD-BREAK / plan-r2.149` 执行与独立 Checker PASS 记录（2026-08-19）：Human Owner 只授权已冻结 contract surface hard break。Maker 在白名单内关闭七项 surface 映射，删除旧十类与旧方法/字段/production refs，新增 19 个有唯一职责的稳定类型并同步四张 Requirement/evidence 表与已知 caller；不保留 alias、V2、bridge、facade、双读写或 fallback。strict-exact 与 GateRequirement 行为尚未实现时保持 fail-closed。fresh `clean-compile=21/21`、`verify-public-contracts=types324/models192/enums43`、immutable PublicContract=`8/0F/0E/0S`、MySQL DDL contract=`7/0F/0E/0S`，`git diff --check` PASS，staged empty。独立 Checker 首轮仅发现 19 个新增 Wind 类型缺 type-level `@author/@since` 的 `1 P2`；最小补齐后最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。该 PASS 只关闭 contract surface；当前 `BEHAVIORAL_RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`，未授权行为测试、实现、Git、HOST/L4 或发布。

`W5-MIG07-BREAKING-BEHAVIORAL-RED-AND-GREEN / plan-r2.150` 执行与独立 Checker PASS 记录（2026-08-20）：Human Owner 分别授予 behavioral RED 与 behavioral Green Grant。RED 只新增两份 Spring/H2 行为测试，覆盖 9 个 strict-exact 与 10 个 GateRequirement 场景；经真实 Stage 007B/007C、Ledger/Balance 正向证据等最小返工后，RED Checker 最终 `PASS / 0 P0-P2`。Green 在冻结白名单内完成 Provider strict-exact、versioned Requirement/current head、mandatory multi-pair Gate、Stage consumed evidence 与全部 caller/schema 一次无兼容切换。首轮 Green Checker 发现同 Stage 首次发布不同 Requirement 版本时，loser 可能误作 replay 并留下不在 current head 的孤儿 requirement/pair，判定 `NOT PASS / 0 P0 / 1 P1 / 0 P2`；Maker 只把首次发布顺序改为先竞争唯一 head，冲突后读取实际 winner 并核对 identity/version/semantic/evidence 双摘要，同时在既有 GT-B02 中增加 `v1/v2` 并发回归，证明一成功、一冲突且仅一条 requirement/pair。最终 Checker `PASS / 0 P0 / 0 P1 / 0 P2`。fresh 证据：clean compile `21/21`，Public Contract 约规 `types=324/models=192/enums=43`，focused `27/0F/0E/0S`，reconciliation `236/0F/0E/0S`，旧宿主 `20/0F/0E/0S`，`git diff --check` PASS、staged empty；双工作区清单稳定为 default `160/b8958afe976fce5c2b52985a83a01c9c0fff430199e068f378f435cb7e00a035`、`-uall 166/96e089d2941406a4118d4eaf4f96f46f29c5398dc9458f9e1a67e7266047c689`。真实 MySQL host 不适用于公共库准出；PMD 本轮未形成 fresh 结果，只作为验证缺项记录，不构成能力 blocker。PaymentInstrument、Mockito/ByteBuddy 与 Consumer E4/L4 继续独立保留。当前切片授权已耗尽，下一入口只允许重新形成 Entry Card。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.151` `CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001` 决策包成包记录（2026-08-20）：Maker 回读 Transaction durable root/detail、direct/authorization converter、Wallet Ledger wrapper、Ledger posting assembler 与调用清册，确认原 `ledgerTransactionSn` 已存在 Transaction 自有 detail，且 Ledger 会在 posting 边界再次验证。五份权威文档据此同构比较 A“Transaction 自有事实解析”、B“新增 core reader”和 C“ledger-face/Wallet bridge”，推荐 A 但保持 `accepted_answer=none`；profile 与无 Consumer audit 不偷并首切。当前只进入 Decision Package Checker，不授权 Entry/RED/Green、Java、测试、API/DTO/DDL/Mapper/schema、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.152` `CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001` Decision Package Checker PASS 状态回写（2026-08-20）：独立 Checker 首轮发现 direct 普通退款、standalone fee 与 embedded fee 的 event/route/participant 选择谓词不够机械；Maker 仅在五份权威文档补齐三类谓词、合法多主体/多 Money sibling 边界与 TDD 种子。最终 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 A 可复用现有 Transaction detail/route 查询且不新增 port/DTO/schema，Ledger/Balance 继续独立闭合。当前只机械进入 Human Owner Gate；`accepted_answer=none`，不授权 Entry/RED/Green、代码、测试、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.153` `CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001-A` Owner 接受回写（2026-08-20）：Human Owner 明确选择 A。接受范围仅为 Transaction 从自有 durable root/route/detail 解析唯一原 `ledgerTransactionSn`，Ledger 在 posting 边界独立复验；B/C 未选择且不是 fallback。价值确认是消除 Wallet bridge、跨模块宽查询和 speculative core reader，同时保留 ordinary refund、standalone/embedded fee refund 与 authorization 后继动作的原引用能力。当前只进入独立 Acceptance Checker，不授权 Entry/RED/Green、Java、测试、API/DTO/DDL/Mapper/schema、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.154` `CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001-A` Acceptance Checker PASS 状态回写（2026-08-20）：独立 Checker 首轮发现解析/校验被误放到 converter，以及系分/TDD 顶部 revision 未同步两项 P1。Maker 最小修正为 Transaction Application/Service 用例边界拥有查询与业务校验、converter 只映射已解析 ref，并统一 `plan-r2.153`；最终 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前只机械进入 `W5-MIG05-TRANSACTION-LEDGER-REFERENCE-ENTRY-CARD / ENTRY_CARD_REQUIRED / DOCUMENTATION_ONLY / CODE_FREEZE`，不授权 RED/Green、代码、测试、Git、HOST/L4 或发布。

`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-ENTRY-CARD / plan-r2.155` Entry Card 成包记录（2026-08-20）：Maker 在 A Acceptance Checker PASS 后回读两个 converter、`FundsTransactionCommandServiceImpl`、Transaction query、Wallet Ledger wrapper、balance-adjustment audit、全部生产引用和五个测试装配 caller，冻结 `ADD=0 / 3 MODIFY / 9 DELETE` 生产清单、三个 RED 文件、五个 Green 测试迁移/删除文件、七个独立 RED 行为组、clean breaking build 与无兼容停止线。当时只进入独立 Entry Card Checker；`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`，未修改 Java、测试、POM、schema、Consumer 或 Git。

`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-ENTRY-CARD / plan-r2.156` Entry Card Checker PASS 状态回写（2026-08-20）：独立 Checker 首轮指出裸测试命令无法证明进入 Surefire、三个独立 Spring 装配 caller 未进入 Green 验证两项 P1；Maker 仅在 TDD/OpenSpec 冻结已验证的 Java 21 + `skipSurefireReport` 前缀，并补 Green 前后独立装配类执行门。最终 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`；`ADD=0 / 3 MODIFY / 9 DELETE`、七个 RED 组、带费 `PAY` principal/FEE 分腿与无兼容停止线不变。当前只进入 Human Owner 的 RED Execution Grant 决策，不授权 RED、Java、测试、Git、HOST/L4 或发布。

`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-RED-EXECUTION / plan-r2.157` RED Execution Grant 回写（2026-08-20）：Human Owner 回复“可以，先对齐文档设计等，然后推进代码重构”。该授权在当前唯一 Gate 上精确解释为本卡 `RED_EXECUTION_GRANT_YES`：只允许 TDD 20.17.6 冻结的三个测试文件形成七组精准失败；生产 Java、Green、API/DTO/DDL/Mapper/schema、Consumer、Git、HOST/L4 与发布仍未授权。七组 RED 必须 fresh 执行并经独立 Checker PASS 后，才进入新的 Human Owner Green Execution Grant。

`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-RED-EXECUTION / plan-r2.158` RED 执行与独立 Checker PASS 状态回写（2026-08-20）：RED 前聚焦基线为 `158/0F/0E/0S`；只在三个测试文件白名单内形成精准失败后，fresh direct=`85/5F/0E/0S`、authorization=`53/1F/0E/0S`、architecture=`26/1F/0E/0S`，合计 `164/7F/0E/0S`，其余 `157` 个用例通过。独立 Checker 首轮指出五个 fail-closed 负例缺稳定异常与零副作用断言；最小 rework 后最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。RED Grant 已执行并关闭，当前只进入 Human Owner 的 Green Execution Grant 决策，`GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-GREEN-EXECUTION / plan-r2.159` Green Execution Grant 回写（2026-08-20）：Human Owner 回复“按本轮结论推进”，在 `plan-r2.158 / RED_INDEPENDENT_CHECKER_PASS` 上授权当前冻结 Green。授权只覆盖系分 11.11.7 的 `3 MODIFY + 9 DELETE + 0 ADD` 与 TDD 20.17.6 的四个测试装配机械迁移/一个 audit 测试删除；三个精准 RED 文件保持不变。Green 前版本与 compile 通过，四个装配/audit 测试 fresh 为 `12 + 26 + 9 + 4 = 51/0F/0E/0S`。Public API 新增、DTO/DDL/Mapper/schema、新写链、Consumer、Git、HOST/L4、MIG-09、enable/release/production 均未授权。

`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-GREEN-CONTRACT-REWORK-ENTRY-CARD / plan-r2.160` 成包记录（2026-08-20）：Green 聚焦验证为 `164/4F/0E/0S`，独立 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 0 P2`。四个失败分别来自旧测试绑定 Wallet/Transaction 错误文案、把未引用的额外 Ledger 行当成 Transaction ref 冲突、把带费 `PAY` 原 route 误断言为单腿，以及要求 Transaction 代替 Ledger 判断 successor ref ownership。Human Owner 明确不考虑兼容，Maker 仅在五份权威文档冻结两个 flow test 的无兼容返工卡；生产与测试代码均未修改，当前只进入返工卡独立 Checker。

`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-GREEN-CONTRACT-REWORK-ENTRY-CARD / plan-r2.161` Checker PASS 状态回写（2026-08-20）：独立只读 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 Transaction 只从自有 durable `root/route/detail` 选择引用、Ledger 独立验证 transaction/plan/entry/route-leg，且两个 flow test 足以关闭四个旧合同失败，无需 fixture、架构测试或生产修改。当前只机械进入 Human Owner 的测试返工执行授权门；`TEST_REWORK_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-TEST-CONTRACT-REWORK / plan-r2.162` 执行与 Green Checker PASS 状态回写（2026-08-20）：Human Owner 依次授权两个 flow test 与独立 FeeFlow 的无兼容测试合同返工。聚焦三类 Direct=`85/0F/0E/0S`、Authorization=`53/0F/0E/0S`、Architecture=`26/0F/0E/0S`，合计 `164/0F/0E/0S`；实际返工文件仅两个 flow test，Architecture 保持通过。FeeFlow 只把旧 RouteReplay 文案绑定改为 Transaction Owner 稳定 `BaseException`，完整保留余额、LedgerTransaction、posting、entry 与目标 businessSn 零副作用断言，fresh=`17/0F/0E/0S`。完整 `test-transaction=176/0F/0E/0S`、compile=`21/21`、Public Contract=`317/188/42`、`git diff --check` PASS；独立 Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前切片授权已耗尽，下一入口只允许重新形成 W5 Entry Card。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.163` `CI-MIG05B-LEDGER-PROFILE-OWNERSHIP-001` 决策包成包记录（2026-08-20）：Maker 回读 Wallet profile/initializer surface、Funding/Credit account create、Transaction settlement、LedgerService 与 wind-funds/capte-domain caller closure，确认 profile items 全属 Ledger 语义，capte-domain 生产只消费 profile code，两个测试宿主才直接装配默认实现。五份权威文档同构比较 A“复用 LedgerService 受控初始化”、B“独立 admission service”和 C“原样搬迁 profile read”，推荐 A 但保持 `accepted_answer=none`。当前只进入 Decision Package Checker，不授权 Entry/RED/Green、Java、测试、API/DTO/POM/schema、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.164` `CI-MIG05B-LEDGER-PROFILE-OWNERSHIP-001` Decision Package Checker PASS 状态回写（2026-08-20）：独立 Checker 首轮发现 core profile public surface、catalog integrity 与并发事务合同三项 P1；Maker 补齐一次性 caller closure、初始化与 posting/admission 共用的 internal versioned catalog guard、winner 回读与整组事务回滚。单点复核又发现物理 bucket identity 与配置语义混淆；Maker 将冲突限定为同一 bucket key 下的 profile/version/catalog 语义不一致，并明确不同 subject/currency/effective period 独立成功。最终 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前机械进入 Human Owner Gate；A 仍仅推荐，`accepted_answer=none`，不授权 Acceptance、Entry/RED/Green、代码、测试、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.165` `CI-MIG05B-LEDGER-PROFILE-OWNERSHIP-001-A` Owner 接受回写（2026-08-20）：Human Owner 明确选择 A。接受范围仅为复用既有 `LedgerService` 承接受控 required-ledger 初始化，profile catalog/integrity guard/创建与复验留在 `ledger-impl`，Wallet 只提交 admitted subject/account facts，Transaction 删除 profile 读取且不自动建账；B/C 未选择且不是 fallback。当前只进入独立 Acceptance Checker，不授权 Entry/RED/Green、Java、测试、API/DTO/POM/schema、Consumer、Git、HOST/L4 或发布。

`W5-DOCS-COMPLETION-REVIEW / plan-r2.166` `CI-MIG05B-LEDGER-PROFILE-OWNERSHIP-001-A` Acceptance Checker PASS 状态回写（2026-08-20）：独立 Checker 确认 A 与决策包完全同构，复用既有 `LedgerService`、保持 catalog/integrity guard 在 `ledger-impl`、仅保留 Public `LedgerProfileCode`，Wallet/Transaction、并发幂等、整组事务回滚与无兼容 caller closure 均未漂移；最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前机械进入独立 Entry Card 编制与复核，不授权 RED/Green、Java、测试、API/DTO/POM/schema、Consumer、Git、HOST/L4 或发布。

`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-ENTRY-CARD / plan-r2.167` 成包记录（2026-08-20）：Maker 在 A Acceptance Checker PASS 后回读 LedgerService、LedgerServiceImpl、posting assembler、Funding/Credit/Settlement callers、core stable API、Justfile、全部 wind-funds 旧 profile/initializer 测试引用和 capte-domain 两个测试宿主。卡片冻结一个 Ledger face request、既有 LedgerService 的 `void` 命令、一个 ledger-impl concrete catalog，以及仓内 `ADD=2 / MODIFY=8 / DELETE=9` 的无兼容 Green 白名单；TDD 冻结七组 RED、26 个测试/fixture Green 迁移文件和旧 contract test 删除。额外识别的自定义 `SubjectLedgerInitializer` Bean、`RecordingLedgerService` 与 Justfile 陈旧测试名均已显式纳入。当前只进入独立 Entry Card Checker，`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；未修改生产/测试代码，未授权 Consumer/Git/HOST/L4 或发布。

`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-ENTRY-CARD / plan-r2.168` Entry Card Checker PASS 状态回写（2026-08-20）：独立 Checker 首轮发现高置信系分分册仍保留旧 `SubjectLedgerInitializer + ledgerId map`，且并发 loser 文案与 `void` 命令冲突两项 P1。Maker 仅做文档先行返工：分册统一为 `LedgerService void command + ledger-impl internal catalog + existing Ledger query`，并把同语义 loser 固定为内部回读逐字段复验后 `void` 幂等完成。最终 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Human Owner 的 RED Execution Grant 决策，`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；不授权 Java、测试、Consumer、Git、HOST/L4 或发布。

`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-RED-EXECUTION / plan-r2.169` RED Execution Grant 回写（2026-08-20）：Human Owner 回复“授权推进”，在 `plan-r2.168 / ENTRY_CARD_INDEPENDENT_CHECKER_PASS` 的唯一活动 Gate 上精确授权本卡七组 RED。当前仅允许 TDD 20.17.9 的 `ADD=1 / MODIFY=6` 测试白名单形成精准失败；生产 Green、Public API/DTO/POM/DDL/Mapper/schema、其他生产/测试代码、Consumer、Git、HOST/L4、enable/release/production 继续禁止。RED 必须 fresh 执行并经独立 Checker PASS 后，才可进入新的 Human Owner Green Execution Grant。

`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-SURFACE-BEHAVIOR-ENTRY-CARD-REWORK / plan-r2.170` 成包记录（2026-08-20）：首轮 RED 得到 baseline=`69/0F/0E/0S`、fresh=`70/7F/0E/0S`、compile=`21/21`，独立 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 1 P2`。Maker 未进入生产 Green，只把原卡重构为 ownership surface move、behavioral RED、behavioral Green 三个独立授权检查点，并补齐 Credit/后序 bucket 回滚、assembler 全字段 matrix、Settlement 三 bucket 与代表性 version drift。当时只进入返工卡独立 Checker，所有执行 Grant 保持 NO。

`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-CONTRACT-SURFACE-EXECUTION-GRANT / plan-r2.171` Entry Card rework Checker PASS 状态回写（2026-08-20）：独立 Checker 确认三检查点授权隔离、surface `ADD=2 / MODIFY=8 / DELETE=9`、26 caller 修改与一个旧 contract 删除、五个 Behavioral RED Owner、四个 Green 候选生产文件及动态 failure 规则均闭合；assembler 在 surface 只等价承接旧 Transaction 的三类 required-item 检查，不形成 fail-open 中间态。最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当时只进入 Human Owner 的 surface execution Grant 决策，三个执行 Grant 当时仍为 NO，不授权 Java、测试、Consumer、Git、HOST/L4 或发布。

`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-CONTRACT-SURFACE-EXECUTION / plan-r2.172` ownership surface move 与独立 Checker PASS 状态回写（2026-08-20）：Human Owner 只授权 surface 检查点。Maker 按冻结 closure 完成 `ADD=2 / MODIFY=8 / DELETE=9`、26 caller 迁移与一个旧 contract 删除；旧 core/Wallet surface 与生产引用归零，`LedgerService` 只新增一个 `void` 命令，catalog 只有一个 concrete ledger-impl 实现，Transaction profile 预读退出，未实现并发 winner、整组回滚或 catalog drift Behavioral Green。clean core API compile=`21/21`、Public Contract=`313/186/42`、非 Mockito caller closure=`258/3F/0E/0S`、transaction=`176/0F/0E/0S`、reconciliation=`236/1F/0E/0S`；三项 failure 精准保留为 Behavioral RED，assembler `10E` 仅为既有 sandbox self-attach。独立 Checker最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Behavioral RED Human Owner Grant，`BEHAVIORAL_RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-BEHAVIORAL-RED-EXECUTION / plan-r2.173` Behavioral RED 与独立 Checker PASS 状态回写（2026-08-21）：Human Owner 只授权五个冻结测试 Owner。Fresh 结果为 Ledger=`5/0F/0E/0S`、Control=`14/2F/0E/0S`、Funding=`10/2F/0E/0S`、Assembler=`11/0F/11E/0S`、Settlement=`10/1F/0E/0S`；Control/Funding/Credit/Settlement 缺口均形成可达 RED，Assembler 因 Mockito/ByteBuddy sandbox 在 Spring context 前 self-attach 失败而只保留环境 P2。clean compile=`21/21`、`git diff --check` PASS；独立 Checker=`PASS / 0 P0 / 0 P1 / 1 P2`。当前只进入 Human Owner Green Execution Grant 决策，`GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`，未授权生产实现、Git、Consumer、HOST/L4 或发布。

`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-GREEN-ENTRY-CARD-REWORK / plan-r2.174` 成包记录（2026-08-21）：Human Owner 在 `plan-r2.173` 后授权按冻结 Behavioral Green 推进；候选实现 `compile=21/21`，并在命令行显式启用既有 `FlexTransactionManager` 时使 Ledger/Control/Funding 三类合计 `29/0F/0E`，但 Assembler=`11/1F/3E`、Settlement=`10/1F/9E`。独立 Checker 判定 `NOT PASS / 0 P0 / 3 P1 / 0 P2`：初始化与 posting 未共享同一个 concrete catalog guard，两个 fixture 固定制造 catalog-invalid ledger，Funding/Credit 原子回滚只被临时命令行属性证明。原 Green Grant 已消耗且未准出；Maker 当时仅在五份权威文档冻结生产 `3 MODIFY`、测试 `11 MODIFY` 的最小返工卡，并把共享 helper 的 24 个直接子类纳入 caller closure，进入独立 Entry Card Checker，`GREEN_REWORK_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05B-LEDGER-PROFILE-OWNERSHIP-GREEN-REWORK-EXECUTION-GRANT / plan-r2.175` Entry Card 返工 Checker PASS 状态回写（2026-08-21）：独立 Checker 首轮发现 6 个 `FUNDING_BASIC + CLEARING/SETTLEMENT` fixture caller 未纳入白名单，且 Benefit 实际为 13 个测试、聚焦总数应为 63；Maker 仅补齐测试 `11 MODIFY` caller closure 并校准 `63/0F/0E/0S`，不改产品语义、DSL 或代码。最终 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当时只进入 Human Owner 的 Green rework Grant 决策，`GREEN_REWORK_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；不授权生产/测试修改、Git、HOST/L4 或发布。

`W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-DOC-ENTRY-CARD-REWORK-001 / plan-r2.176` 成包记录（2026-08-21）：Human Owner 仅授权文档设计与 Checker。Maker 复核现有 route/assembler/original posting 证据，把外部入金、外部出金、内部腿、replay/reversal 与 explicit adjust 的 balance effect 冻结成单一无兼容方向矩阵；生产候选仅一个 assembler MODIFY，测试 closure 为 16 个现有文件，且 profile/Benefit/Settlement 等证据文件不可修改。当前进入独立 Entry Card Checker，`RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；未修改或运行代码/测试，不授权 Git、HOST/L4 或发布。

`W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-RED-EXECUTION-GRANT / plan-r2.177` Entry Card Checker PASS 状态回写（2026-08-21）：独立 Checker 首轮发现 ordinary `RESTORE/RELEASE` 无 `replayRefLegId` 时不能被误归为 reverse-class 的 1 个 P1；Maker 仅在五文档补齐其 `DECREASE/INCREASE` 方向，并把 reverse-class 限定为非空且唯一 original leg 引用。最终 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Human Owner RED Execution Grant 决策，`RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；不授权代码、测试、Git、HOST/L4 或发布。

`W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-RED-ENTRY-CARD-REWORK-001 / plan-r2.178` 成包记录（2026-08-21）：Human Owner 授权的方向 RED 已完成，assembler 四方法 fresh=`4/2F/0E/0S`，两个外部方向精确失败、internal derive 与跨交易 replay reject 通过；15 个 signed-CASH caller 完成机械迁移与编译。独立 Checker=`NOT PASS / 0 P0 / 1 P1 / 0 P2`，唯一问题是原卡要求这 15 类在共享 assembler 根因修复前逐类 fresh 到达后置断言。Maker 仅在五份权威文档把 RED 动态门收敛为四个 assembler 方法，并把 15 类 fresh 全绿移到 Green 硬门；资金方向、生产/测试白名单与无兼容边界不变。当前进入文档返工独立 Checker，`RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-GREEN-EXECUTION-GRANT / plan-r2.179` RED Entry Card 返工 Checker PASS 状态回写（2026-08-21）：独立 Checker 复核稳定方向、assembler `4/2F/0E/0S` RED、15 caller Green 硬门、生产 `1 MODIFY`/测试 `16 MODIFY` 白名单及五文档活动态，最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Human Owner Green Execution Grant 决策，`GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；不授权生产、测试、Git、HOST/L4 或发布。

`W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-GREEN-EVIDENCE-ENTRY-CARD-REWORK-001 / plan-r2.180` 成包记录（2026-08-21）：Human Owner 已授权的单文件 Green 使 assembler 四方法全绿，但 15 caller fresh=`235/79F/2E/0S`；独立 Checker 判定 `NOT PASS / 0 P0 / 3 P1 / 0 P2`，Green 未准出。Maker 仅在五份权威文档将 `59` 个 profile fixture、`20` 个旧 signed-CASH 断言与 `2` 个 `FUNDING_BALANCE_ADJUST` 分开，冻结 `8 MODIFY` 测试证据卡、`235/1F/1E/0S` 中间门和独立 explicit-adjust 后续切片。当时只进入证据返工卡独立 Checker；`TEST_REWORK_EXECUTION_GRANT_NO / GREEN_EXECUTION_PAUSED / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-TEST-REWORK-EXECUTION-GRANT / plan-r2.181` 证据返工卡 Checker PASS 状态回写（2026-08-21）：独立 Checker 首轮发现两个冻结 adjust 用例的机械结果应为 `1F+1E` 而不是 `2F`；Maker 仅把五文档中间门改为 `235/1F/1E/0S`，并明确 `1E` 是真实 posting 不平衡业务缺口而非环境错误。单点复核最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Human Owner 的测试返工 Execution Grant 决策，`TEST_REWORK_EXECUTION_GRANT_NO / GREEN_EXECUTION_PAUSED / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05B-EXTERNAL-FUNDS-LEG-ACCOUNTING-DIRECTION-TEST-REWORK-EXECUTION-001 / plan-r2.182` 执行与 Checker PASS 状态回写（2026-08-21）：Human Owner 授权后，Maker 只修改系分 11.11.13 冻结的 8 个测试文件，将 `SETTLEMENT/CLEARING` fixture 归位到既有 `FUNDING_MERCHANT`、月度授权 Ledger 对齐 `FUNDING_BASIC/v1` 目录字段、Direct 余额按持久 Ledger normal side 解释，并把 payout 平台 `CASH` 断言校正为 `-700`；未修改生产、共享 fixture、catalog、API 或 schema。Fresh 15 类精确为 `235/1F/1E/0S`、其余 `233 PASS`，`compile=21/21`，独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。当时只进入独立 `W5-MIG05B-FUNDING-BALANCE-ADJUST-ENTRY-CARD / ENTRY_CARD_REQUIRED / DOCUMENTATION_ONLY / CODE_FREEZE`；该入口已由下方 `plan-r2.183` 成包接替，外部资金腿 Green 继续暂停。

`W5-MIG05B-FUNDING-BALANCE-ADJUST-ENTRY-CARD / plan-r2.183` 成包记录（2026-08-21）：Human Owner 按建议接受 A，冻结平台 `ADJUSTMENT` 为 debit-normal signed suspense、目标 `AVAILABLE` 为 credit-normal liability，increase 两端均 `INCREASE`、decrease 两端均 `DECREASE`，entry side 由 normal side 推导。五份权威文档只建立 `3 MODIFY` 测试 RED 与 `1 MODIFY` assembler Green 白名单，`ADD=0 / DELETE=0`；不新增 API、schema、service、direction engine 或兼容路径。当时只进入 Entry Card 独立 Checker，`RED_EXECUTION_GRANT_NO / EXTERNAL_FUNDS_LEG_GREEN_EXECUTION_PAUSED / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05B-FUNDING-BALANCE-ADJUST-RED-EXECUTION-GRANT / plan-r2.184` Entry Card Checker PASS 状态回写（2026-08-21）：独立 Checker 首轮发现 assembler 通用 event matrix 仍含普通 `AVAILABLE -> AVAILABLE` 的伪 `BALANCE_ADJUST` case。Maker 只在既有三个 RED 文件范围内补齐迁移责任：删除/替换该 case，由两个专用方法承接 phase/effect/intent/scope，并允许同一测试文件内 `RecordingLedgerService` 返回真实平台 `ADJUSTMENT` 与目标 `AVAILABLE`；生产 catalog、shared fixture、RED `3 MODIFY`、Green `1 MODIFY` 和 `4/4F/0E/0S` 均不变。最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。当前只等待 Human Owner RED Grant 决策，`RED_EXECUTION_GRANT_NO / EXTERNAL_FUNDS_LEG_GREEN_EXECUTION_PAUSED / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05B-FUNDING-BALANCE-ADJUST-RED-EXECUTION / plan-r2.185` RED 与独立 Checker PASS 状态回写（2026-08-21）：Human Owner 仅授权三个冻结测试文件。Maker 删除通用 matrix 的伪 adjust case并用既有 external route 保持其余事件 `1/0F/0E/0S`；两个 assembler 方向、余额不足与佣金扣回形成 fresh=`4/4F/0E/0S`，四项唯一 failure 均为 `RouteLeg 生成的账务计划不平衡，legId = FUNDING_BALANCE_ADJUST`，compile=`21/21`。首轮 Checker 的 `2 P1 + 1 P2` 与后续单点 `1 P1` 仅要求补 Money/currency、失败 Ledger 快照、准确注释和 zero-root 事务边界断言，均在原白名单最小返工；最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Human Owner Green Execution Grant 决策，`GREEN_EXECUTION_GRANT_NO / EXTERNAL_FUNDS_LEG_GREEN_EXECUTION_PAUSED / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05B-FUNDING-BALANCE-ADJUST-GREEN-EXECUTION / plan-r2.186` Green、测试合同校正与独立 Checker PASS 状态回写（2026-08-22）：Human Owner 授权后，Maker 只在 `DefaultLedgerPostingAssembler` 让 `BALANCE_ADJUST` 两端复用既有显式 effect；首轮 Checker 发现余额不足 RED 错把既有确定性 `LedgerPostingRejectedException` 绑定为 `BaseException` 的 `1 P1`，后续只校正该异常类型，完整保留零成功资金/账务效果断言，未增加兼容包装。最终四格=`4/0F/0E/0S`；capability=`1/0`、balance-control=`44/0`、business-flow=`204/0`、外部资金腿 15 类=`235/0`、外部腿 assembler=`4/0`、compile=`21/21`、ledger=`61/0`、transaction=`176/0`、reconciliation=`238/0`、Public Contract=`313/186/42`、`git diff --check=PASS`，独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。生产文件 SHA=`b2007fdc3bd290f007faeabf94b42a798905580b61052b5a17241f37ab502f9b`，测试文件 SHA=`bee10c807434339a10d346e26d789e93494df44b25ded8de625138c4968f3fe3`；工作区稳定为 HEAD=`eb12091819152fcec529f9453b48755f3aa2c999`、default=`224/a0aeaad437fa9ac956b3f1a2f03b583a77db3841243184293fefc315b75b6c2d`、`-uall=230/6064f27636759e54af915b14c9e4fc86cfd8801e4253efe35a7f1e653c88ebb4`、staged empty。该轮及外部资金腿执行授权均已耗尽，当时进入 `W5-MIG05B-NEXT-SLICE / ENTRY_CARD_REQUIRED / DOCUMENTATION_ONLY / CODE_FREEZE`；不授权下一实现、Git、MIG-09、HOST/L4 或发布。

`W5-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-ENTRY-CARD / plan-r2.187` 成包与首轮 Checker 记录（2026-08-22）：Human Owner 选择 A，无兼容删除缺少真实 Consumer 与 authority 证明的六个负余额 Public 字段、六个 transaction-face key 和 core raw flag；signed `ADJUSTMENT` 与 Ledger internal allow-negative 保持。首轮独立 Checker=`NOT_PASS_P0_0_P1_1_P2_0`，唯一 finding 为卡片声明 raw literal context 不得绕过，但 `2 MODIFY` RED 白名单没有行为测试 Owner；生产 `7 MODIFY` 边界和 A 语义均未被否定。

`W5-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-ENTRY-CARD-REWORK-001 / plan-r2.188` 文档返工记录（2026-08-22）：Human Owner 仅授权五份权威文档返工与再次只读 Checker。RED 白名单最小扩为 `3 MODIFY / 0 ADD / 0 DELETE`，加入 `FundsBalanceControlFailureFlowTests.java` 的完整六项 raw tuple 余额不足行为；目标改为聚合 surface 与 raw bypass 两项精准 failure、零 error，Ledger DTO 中性 key 改名保持 PASS。生产/契约 `7 MODIFY`、A、无兼容、signed adjustment 与 Ledger internal allow-negative 均不变；当前仍为 `RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-RED-EXECUTION-001 / plan-r2.189` RED Grant 记录（2026-08-22）：返工独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。Human Owner 授权 TDD 20.17.15 冻结的三份测试执行 RED；生产/契约七文件保持只读，`GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`，不授权 Git、Consumer、HOST/L4 或发布。

`W5-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-RED-EXECUTION / plan-r2.190` RED 执行与独立 Checker PASS 状态回写（2026-08-22）：三份冻结测试源码形成 surface=`6/1F/0E/0S`、Ledger DTO=`5/0F/0E/0S`、flow=`19/1F/0E/0S`，合计 `30/2F/0E/0S`。Surface failure 聚合命中六字段、十二 getter/setter、六 transaction constants 与一 core flag；真实 Spring/H2 flow failure 命中 AVAILABLE delta expected `0` / actual `-80`，证明完整 raw tuple 可使余额由 50 降至 -30。compile=`21/21`；七份生产候选 SHA 未变；独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。RED Grant 已耗尽，当前只进入 Human Owner Green Grant 决策，`GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-GREEN-EXECUTION-001 / plan-r2.191` Green Grant 记录（2026-08-22）：Human Owner 回复“按你的建议执行推进”，在 `plan-r2.190 / RED_INDEPENDENT_CHECKER_PASS` 上授权当前冻结 Green。授权只覆盖生产/契约 `7 MODIFY / 0 ADD / 0 DELETE`，三份 RED 测试保持 immutable；无兼容、不新增 DTO/service/schema、Consumer 或 Git 权限。

`W5-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-GREEN-EVIDENCE-ENTRY-CARD-REWORK-001 / plan-r2.192` 文档返工记录（2026-08-22）：七文件 Green 已完成并形成切片级全绿证据，三份 RED 测试哈希保持冻结；独立 Green Checker 因完整 capability fixture=`12/1F/0E/0S` 与 core API 历史 `107/99` 对当前 `105/97`、`getStatus -> getState` 两项白名单外仓库基线，判定 `NOT PASS / 0 P0 / 2 P1 / 0 P2`。Human Owner 接受“先做价值与实际场景确认，再按建议推进”，仅授权五份权威文档建立 `GREEN_IMPLEMENTATION_VERIFIED / REPOSITORY_BASELINE_BLOCKED` 两层门禁，并冻结 `BASELINE-CAPABILITY-PAYEE-PROFILE-FIXTURE-REPAIR-001` 与 `BASELINE-CORE-API-GOVERNANCE-REBASE-001`；当前只进入独立文档 Checker，不授权任何源码、测试、脚本、API baseline、Git 或发布。

`W5-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-GREEN-EVIDENCE-ENTRY-CARD-REWORK-001 / plan-r2.193` 首次 Checker NOT PASS 与返工记录（2026-08-22）：独立 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 0 P2`；P1-1 为卡 B 只追加 core 长期稳定记录会保留 `107/99/ALLOW_NEGATIVE_BALANCE` 与当前 `105/97` 双权威，P1-2 为卡 A/B Checker PASS 后缺少逐卡五文档状态回写。Maker 未扩大文件范围，只把卡 B 责任收紧为 superseding decision 加当前态机械更新，并冻结卡 A PASS、卡 B PASS、最终 Green 复跑三段独立 Grant 与五文档回写边界；当前再次进入独立文档 Checker，所有源码、测试、脚本、API baseline、Git 和发布仍冻结。

`W5-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-GREEN-EVIDENCE-ENTRY-CARD-REWORK-001 / plan-r2.194` 最终 Checker PASS 状态回写（2026-08-22）：独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 core 长期稳定规格的 superseding/current/history 责任完整，卡 A PASS、卡 B PASS 与最终 Green 复跑三段均有独立 Human Owner Grant、Checker 和五文档机械回写边界，未弱化原 Green 门禁或引入兼容。下一唯一入口为 `BASELINE-CAPABILITY-PAYEE-PROFILE-FIXTURE-REPAIR-001 / EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；卡 B、最终复跑、Git 与发布均未授权。

`BASELINE-CAPABILITY-PAYEE-PROFILE-FIXTURE-REPAIR-001 / plan-r2.195` Execution Grant 记录（2026-08-22）：Human Owner 在完成价值与实际业务场景确认后回复“按你的建议推进”，授权仅修改 `FundsAccountCapabilityAdmissionFlowTests.java` 的 PAY payee profile 前置，复用既有 `FUNDING_MERCHANT` 后再创建 `CLEARING`；所有既有 capability、FAILED funds fact、零成功账务副作用和余额断言 immutable。Grant 包含完整类验证、独立 Checker 与 PASS 后五文档机械回写；生产、共享 fixture、Catalog、脚本、API baseline、卡 B、最终复跑、Git 与发布均未授权。

`BASELINE-CAPABILITY-PAYEE-PROFILE-FIXTURE-REPAIR-001 / plan-r2.196` 执行与 Checker PASS 状态回写（2026-08-22）：执行前完整类 fresh=`12/1F/0E/0S`，唯一 failure 为默认 `FUNDING_BASIC` payee 不包含 `CLEARING`；目标测试文件仅增加 `ensureFundingAccount(payee, LedgerProfileCode.FUNDING_MERCHANT)`，SHA 从 `9006229e37b3daf33176fbd5148a7703946564ccfb69e1b4f403ea713432b495` 变为 `5c46afafd212ab6199aaac814d2fbeda1306100fa4a956ce74c52a5ce6c5f11c`。Green fresh=`12/0F/0E/0S`，PAY 与 capability-drift 方法均通过，前后 compile=`21/21`；三 RED/七 Green 冻结文件哈希未变。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`，确认既有 PAY/RECEIVE、FAILED funds fact、零成功账务副作用和余额断言未改，生产、共享 fixture、Catalog/Orchestrator 未动。下一唯一入口为 `BASELINE-CORE-API-GOVERNANCE-REBASE-001 / EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；最终复跑、Git 与发布仍未授权。

`BASELINE-CORE-API-GOVERNANCE-REBASE-001 / plan-r2.197` Execution Grant 记录（2026-08-22）：Human Owner 回复“继续推进”，授权仅修改 `openspec/changes/funds-core-long-term-stability/spec.md`、`scripts/verify-core-api-baseline.sh` 与 `core/api-baseline/stable-api.txt`，把当前权威 supersede 为 `105 public / 97 stable / 4 experimental / 4 internal / 1043 baseline lines / FundsAccount#getState()`；`107/99/1062`、两个旧 profile spec 与 `ALLOW_NEGATIVE_BALANCE` 仅保留为带日期历史。`api-policy.tsv`、core Java、其他 signature、兼容入口、Consumer、Git、最终 Green 复跑与发布均未授权；Grant 包含 `just verify-core-api`、独立 Checker 和 PASS 后五文档机械回写。

`BASELINE-CORE-API-GOVERNANCE-REBASE-001 / plan-r2.198` 执行与 Checker PASS 状态回写（2026-08-22）：执行前 `just verify-core-api` clean compile 21/21 后精确 RED=`Expected 107 public top-level core types; found 105`。脚本只把 cardinality/message 从 `107/99` 改为 `105/97`，stable baseline 只把 `FundsAccount#getStatus()` 改为源码既有 `getState()`；core 长期规格以 `D-CS-006-R` 统一所有当前权威为 `105/97/4/4 / 1043 lines / getState()`，旧值均进入明确历史语境。Green `just verify-core-api` 再次 clean compile 21/21 并 baseline verified，direct script 与 `bash -n` PASS；`api-policy.tsv`、core Java、其他 signature 未变。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`，确认三文件反向机械重构精确回到执行前 SHA、无兼容恢复或双权威。下一唯一入口为最终 Green 复验 Human Owner Grant，当前 `EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；Git 与发布仍未授权。

`W5-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-FINAL-GREEN-REVERIFICATION-001 / plan-r2.199` Execution Grant 记录（2026-08-22）：Human Owner 回复“按你的建议执行”，在证据卡、卡 A 与卡 B 均独立 Checker PASS 后，只授权 fresh 重跑 8.56/TDD 20.17.15 冻结的 Java 21、core API、Public Contract、聚焦与扩大 Green 门禁、独立 Checker及 PASS 后五文档机械回写。源码、测试、脚本、API baseline、Consumer、Git、HOST/L4、enable/release/production 全部 immutable；`wind-funds` 公共库不设置真实 MySQL host 或独立 PMD 环境门禁。

`W5-MIG05B-BALANCE-ADJUST-NONNEGATIVE-SURFACE-FINAL-GREEN-REVERIFICATION-001 / plan-r2.200` 最终 Green 复验与独立 Checker PASS（2026-08-22）：Java 21、clean compile=`21/21`、core API=`105 public / 97 stable / 4 experimental / 4 internal`、Public Contract=`313/186/42` 均 PASS；fresh 聚焦 non-assembler=`46/0F/0E/0S`、assembler=`15/0F/0E/0S`、balance-control=`44/0F/0E/0S`、transaction=`176/0F/0E/0S`、business-flow=`204/0F/0E/0S`、ledger=`61/0F/0E/0S`。17/17 冻结文件 SHA 前后精确一致；独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。真实 MySQL host、PMD 与 Git 不属于本卡门禁；相关 Grant 已耗尽。Human Owner 同一“按你的建议执行”授权的第二阶段只覆盖 `W5-REFACTORING-PROGRESS-BASELINE-REFREEZE-001` 文档进度重基线与 Checker，不授权代码。

`W5-REFACTORING-PROGRESS-BASELINE-REFREEZE-001 / plan-r2.201` Maker 记录（2026-08-22）：只读复核 MIG-00~09、ActionFact/Ledger/Balance 源码和既有测试/Checker 证据后，在五份权威文档重冻分层进度。主要校准为 MIG-05 只标 Provider 部分实现、MIG-07 标 Provider Green 但 Consumer E4/L4 独立等待、MIG-03 标物理链存在而稳定闭合能力缺失；MIG-04/06/08/09 不提前升级。当前只进入独立 Checker，未修改或运行源码、测试、脚本、API baseline、MySQL、PMD 或 Git。

`W5-REFACTORING-PROGRESS-BASELINE-REFREEZE-001 / plan-r2.202` 独立 Checker PASS 状态回写（2026-08-22）：Checker 逐项回读 ActionFact projector、Ledger query/投影、MIG-04 旧归属、MIG-05 已删除 wrapper/profile 与仍存在的 core 单实现接口/composite、MIG-07 Provider 代码和 `capte-domain` 依赖，判定 `PASS / 0 P0 / 0 P1 / 0 P2`。确认五文档未预设 Java/API/DTO/表或模块 Owner，未合并 Action/Ledger/Balance，也未把 Provider Green 冒充 Consumer E4/L4。下一唯一入口为 MIG-03 文档 Entry Card Human Owner Grant 决策，当前 Grant=`NO`。

`W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-DOC-ENTRY-CARD-001 / plan-r2.203` Documentation Grant 记录（2026-08-22）：Human Owner 回复“按你的建议推进”，只授权回读当前权威、源码/caller/test 证据，并在既有五文档形成 MIG-03 候选合同、首个真实切片、调用闭包、TDD 种子、停止线与独立 Checker；Java、测试、DDL、API baseline、Consumer、Git、HOST/L4 和发布均未授权。

`W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-DOC-ENTRY-CARD-001 / plan-r2.204` Maker 记录（2026-08-22）：源码确认当前唯一同时消费 Transaction/Ledger 的生产入口是 clearing splittable source admission，request 由 caller 提交 root/detail/ledger entry 三元组；直接生产源码无 ActionFactRef Consumer，仓外也无生产 Consumer，证据为 Provider E2。五文档形成 A/B/C：推荐 A 由 Transaction 投影 recorded refs、Reconciliation 独立验证 Ledger/atomic balance commit；B 新增 durable balance evidence 延后；C 保留 caller tuple 拒绝。当前只进入独立 Checker与后续 Human Owner 合同选择，不授权代码。

`W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-DOC-ENTRY-CARD-001 / plan-r2.205` 首轮 Checker NOT PASS 与返工记录（2026-08-22）：Checker 判定 `NOT PASS / 0 P0 / 1 P1 / 0 P2`，指出 primary ActionFact 虽由 principal detail 生成，`proven-full` 却依赖 principal、PAYEE 与可选 FEE_RECEIVER 的完整 matched group；原卡可能被误实现为只返回 principal ref，迫使 Reconciliation 再猜 sibling。五文档仅补齐完整 sibling 角色/Money/detail refs、全组唯一 distinct LedgerTransaction、PAYEE/CLEARING 唯一选择、fee 不误选及 C02/C03/C05 验收种子；A/B/C、首切、文件范围和代码冻结不变，当前再次进入独立 Checker。

`W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-DOC-ENTRY-CARD-001 / plan-r2.206` 最终 Checker PASS 状态回写（2026-08-22）：Checker 独立回读 ActionFact projector、完整 matched sibling group、Ledger/Balance 事务边界、clearing request/impl、模块 POM 和四个测试 caller，判定 `PASS / 0 P0 / 0 P1 / 0 P2`。确认 A 必须返回 principal、唯一 PAYEE、可选唯一 FEE_RECEIVER 的角色/Money/detail refs与全组唯一 distinct LedgerTransaction，Reconciliation 只选 PAYEE/CLEARING credit且 fee 不误选；依赖方向、首切、A/B/C 和代码冻结均未漂移。当前只进入 Human Owner 合同裁决，Execution Grant=`NO`。

`CI-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-001 / plan-r2.207` Human Owner A 接受记录（2026-08-23）：Human Owner 明确回复“A”，接受 `TRANSACTION_RECORDED_REFERENCES_RECONCILIATION_VERIFIED_CLOSURE`。A 固定 Transaction 完整 matched sibling recorded refs、Reconciliation 独立 Ledger closure verification 与既有 atomic balance commit invariant；B durable balance evidence 与 C caller-selected tuple均未选择且不是 fallback。当前只进入独立 Contract Acceptance Checker，RED Entry Card、Java、测试、DDL、API baseline、Consumer、Git 和发布均未授权。

`CI-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-001 / plan-r2.208` A Acceptance Checker PASS 状态回写（2026-08-23）：Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 Human Owner 的 A 选择未外推为代码授权，完整 sibling、唯一 LedgerTransaction、PAYEE/CLEARING、fee 不误选、Transaction/Ledger/Reconciliation Owner 与 atomic balance commit 边界均保持；B/C 未选择且不是 fallback。下一只准入 RED Entry Card 文档授权。

`W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-RED-ENTRY-CARD-001 / plan-r2.209` Documentation Grant 记录（2026-08-23）：Human Owner 回复“继续”，只授权五文档形成 contract RED 测试白名单、精确失败、候选 Green closure、caller closure、验证和停止线；测试新增/修改、RED 执行、生产代码、DDL、API baseline、Consumer、Git 和发布均未授权。

`W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-RED-ENTRY-CARD-001 / plan-r2.210` Maker 记录（2026-08-23）：五文档冻结唯一 RED 文件 `FundsActionLedgerClosurePublicContractTests.java`，以两个反射聚合测试精准命中 recorded-evidence surface 与 clearing request hard break，目标 `2/2F/0E/0S`；候选生产 closure 为 `ADD=2 / MODIFY=3`，无 schema/POM/Mapper。当前只进入独立 RED Entry Card Checker，`RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-RED-ENTRY-CARD-001 / plan-r2.211` 独立 Checker PASS 状态回写（2026-08-23）：Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认唯一 RED 文件父目录存在且目标文件尚不存在，反射方案可在缺类/缺方法时形成两项聚合 assertion failure 而非 error，候选 `ADD=2 / MODIFY=3` 生产闭包与五个后续 behavior caller 完整，无 POM/schema/Mapper 遗漏。当前只进入 Human Owner `RED_EXECUTION_GRANT` 决策，默认 `NO / CODE_FREEZE`。

`W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-RED-EXECUTION-001 / plan-r2.212` RED 执行与独立 Checker PASS 状态回写（2026-08-23）：Human Owner 在价值回归后授权唯一 Contract RED。Maker 仅新增 `FundsActionLedgerClosurePublicContractTests.java`，Java 21 compile=`21/21`、Public Contract=`313/186/42`；父级 Surefire HTML report 的私有 site descriptor / `~/.m2` 写锁使 `just test-one` 未进入测试，等价 Maven 命令仅增加 `-DskipSurefireReport=true` 后 fresh=`2/2F/0E/0S`。测试源码 SHA-256=`91ccb56ae80446f637b9e3500dc571507e3371001ce5ff33c1de90c10ab3e254`，XML SHA-256=`1dbcb0de75da6c22687a5d23c561d0ffb306a47e5db0fe9e29b19d80ab72f46d`；五个生产文件与五个冻结 caller 哈希保持不变。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Green Entry Card 文档授权决策，`GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-GREEN-ENTRY-CARD-001 / plan-r2.213` Maker 记录（2026-08-23）：Human Owner 回复“继续推进”，只授权五文档形成 Green 文件卡。Maker 回读 production/query/Ledger/caller/Gate/fixture 全闭包，把生产冻结为 `ADD=2 / MODIFY=3`，补齐原候选遗漏的共享 `ReconciliationTestFixture` 与 `ClearingSplitBatchApplicationServiceTests`，测试冻结为 `MODIFY=7 + Contract RED immutable`。新增行为仅 Direct `+2`、Clearing `+4`；Green 精确门为 focused=`148`、transaction=`178`、reconciliation=`242`、business-flow=`206`、compile=`21/21`、Public Contract=`315/187/42`。当时只进入独立 Green Entry Card Checker，`GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG03-ACTION-LEDGER-BALANCE-CLOSURE-GREEN-ENTRY-CARD-001 / plan-r2.214` 独立 Checker PASS 状态回写（2026-08-23）：Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认生产 `ADD=2 / MODIFY=3` 与测试 `MODIFY=7 + Contract RED immutable` 是当前完整最小闭包；`ownerNamespace=funds`、principal proven-full、完整 sibling/digest、Ledger count + exact-size、PAYEE/CLEARING 与 fee 不误选可执行，不需 Ledger API/schema/POM/Mapper。当前一手 XML 复算纠正 reconciliation 基线为 `238`，Green `+4` 后门禁为 `242`；其余精确计数成立。当前只进入 Human Owner Green Execution Grant 决策，`GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-CONTRACT-ENTRY-CARD-001 / plan-r2.215` Maker 与首轮 Checker 返工记录（2026-08-23）：Human Owner 在 MIG03 Green 执行后授权“推进时需要确认价值”。Green 主链 final compile=`21/21`、Contract=`2/0`、focused=`148/0`、Public Contract=`315/187/42`，但独立 Green Checker最终因 persisted time 不可重建保持 P1。五文档确认 `VALUE_CONFIRMED / A=LEDGER_INTERNAL_NORMALIZE_THEN_VERIFY`，冻结秒级 canonical time、唯一 persisted v1、Ledger internal writer/read validation、无兼容和候选 `production MODIFY=1 / tests MODIFY=4`。文档首轮 Checker又发现 `exchangeRate 1 -> 1.00000000` scale drift 的 `1 P1`；Maker 只补 `stripTrailingZeros + plain decimal` canonical 与 TDD 证伪种子，范围和授权不变。当前再次进入独立 Checker，`RED_ENTRY_CARD_DOCUMENTATION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-CONTRACT-ENTRY-CARD-001 / plan-r2.216` 最终 Checker PASS 状态回写（2026-08-23）：独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 canonical time=seconds、canonical BigDecimal=strip trailing zeros/plain decimal，Money 仍为 integer amount+currency；`MIG03-LD-009` 可证伪 scale/toString/忽略汇率错误。生产 `MODIFY=1`、测试 `MODIFY=4`、无 DDL/API/helper、无 legacy/双验/回填/fallback 均未漂移。下一唯一入口为 Human Owner 的 Ledger Digest RED Entry Card 文档授权决策，`RED_ENTRY_CARD_DOCUMENTATION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-ENTRY-CARD-001 / plan-r2.217` Maker 记录（2026-08-23）：Human Owner “推进吧”只授权五文档 RED 文件卡。Maker 基于当前 XML/源码冻结四个 MODIFY 测试类、8 个一对一 expected failures 与 stable labels，目标 focused=`56/8F/0E/0S`、ledger=`65/5F`、reconciliation=`245/3F`，transaction/business 保持 `178/0 / 206/0`。Gate 仅迁移 canonical fixture并保持 `10/0`；全部生产与白名单外测试 immutable。当前只进入独立 Checker，`RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-ENTRY-CARD-001 / plan-r2.218` Checker PASS 状态回写（2026-08-23）：独立 Checker 首轮发现 focused 命令含多余 `+`，且四处 r2.216 历史状态仍标为当前；Maker 只修命令与历史/当前指针。复核最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认测试 `MODIFY=4 / ADD=0 / DELETE=0`、8 个一对一 expected failures、focused=`56/8F/0E/0S`、ledger=`65/5F`、reconciliation=`245/3F`、Green 候选唯一 `LedgerTransactionServiceImpl.java`、无兼容/API/DDL/helper 扩张均未漂移。当时只进入 Human Owner RED Execution Grant 决策，`RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-EXPANDED-GATE-REWORK-001 / plan-r2.219` Maker 记录（2026-08-23）：Human Owner 授权 RED 后，四个冻结测试形成 focused=`56/8F/0E/0S` 与 8 个 stable labels；reconciliation=`245/3F`、transaction=`178/0`、business=`206/0` 均成立。完整 `test-ledger=65/5F/15E` 的 15E 全部来自白名单外 assembler Mockito/ByteBuddy self-attach，原 Checker 因卡片未预先分层判定 `NOT PASS / 0 P0 / 1 P1 / 0 P2`。Human Owner 随后只授权五文档返工与价值分析；Maker 将同一源码状态下五份 fresh XML 合计的 non-assembler `50/5F/0E` 冻结为 owned behavior 门，并要求 Green 前 fresh 执行精确组合；assembler `15E` 保留为非 PASS 环境 observation。本轮未修改或运行 Java/测试，当时只进入独立返工 Checker，`GREEN_ENTRY_CARD_DOCUMENTATION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-RED-EXPANDED-GATE-REWORK-001 / plan-r2.220` Checker PASS 状态回写（2026-08-23）：独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 evidence layering 没有改变 persisted digest 合同、8 个 RED、4 文件白名单、无兼容或唯一 Green 候选；non-assembler 五类精确等于 Justfile Ledger 全集排除唯一 assembler，当前 `50/5F/0E` 只作为同状态 fresh XML 合计，Green 前仍须 fresh 执行；assembler `15E` 未计 PASS，完整 `test-ledger` 仍未准出。当前只进入 Human Owner Green Entry Card 文档授权决策，`GREEN_ENTRY_CARD_DOCUMENTATION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-GREEN-ENTRY-CARD-001 / plan-r2.221` Maker 记录（2026-08-23）：Human Owner 要求先做价值分析再推进，按 r2.220 唯一下一门只授权五文档 Green Entry Card。Maker 回读实际单文件、三个现有 Mapper、四个 immutable RED builder 与测试门禁，确认可在 `LedgerTransactionServiceImpl.java` 内完成 normalized materialization、最终生成型身份、三层 persisted v1、同 key replay 既有身份绑定、原子写后回读自校验和 exact read fail-closed；冻结 `MODIFY=1 / ADD=0 / DELETE=0`，不新增 API/DDL/helper/兼容。当时只进入独立 Green Entry Card Checker，`GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-EXACT-READ-RED-COVERAGE-REWORK-001 / plan-r2.222` Green Entry Card Checker NOT PASS 状态回写（2026-08-23）：独立 Checker 判定 `NOT PASS / 0 P0 / 1 P1 / 0 P2`。单文件 Green shape 可行，但 immutable RED 只观察 `getLedgerTransactionBySn` 和 `getLedgerEntryBySn`；漏做 transaction/entry by-id、entry query、`existsPostingPlan` 仍可让 8 RED 全绿。下一候选只重开 `LedgerTransactionServiceFactQueryTests.java`，保持三个 invocation/三个 stable-label failure，聚合覆盖全部 exact read surface；生产 `MODIFY=1` 与无兼容设计不变。当前只进入 Human Owner RED coverage rework Grant 决策，`RED_COVERAGE_REWORK_GRANT_NO / GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-EXACT-READ-RED-COVERAGE-REWORK-001 / plan-r2.223` RED coverage rework 与 Green Entry Card 复核状态回写（2026-08-23）：Human Owner 的“回到本项目的任务继续推进”和中断恢复只授权当时唯一 RED coverage rework。Maker 仅重开 `LedgerTransactionServiceFactQueryTests.java`，保持三个参数化 invocation 与三个 stable-label failure，分别聚合覆盖 transaction by-id/by-SN、plan aggregate/`existsPostingPlan`、entry by-id/by-SN/query。fresh focused=`56/8F/0E/0S`、non-assembler Ledger=`50/5F/0E/0S`、Reconciliation=`245/3F/0E/0S`、Transaction=`178/0F/0E/0S`、Business=`206/0F/0E/0S`、compile=`21/21`、Public Contract 清册=`315/187/42`；目标测试 SHA-256=`eed2f4f58d188f20ce1cc1e6c77b9599e2e73981785b3b42e0e1c8e61da886a9`，生产候选 SHA-256 仍为 `5acbbdb67273c419095b0eb79e845bba97a8286eda638b2d0b19dd4055f7a866`。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`，并确认原 Green Entry Card 唯一 P1 已关闭、可重新判定 `LEDGER_DIGEST_GREEN_ENTRY_CARD_INDEPENDENT_CHECKER_PASS`。该阶段当时只进入 Human Owner Green Execution Grant 决策，`GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；现由 `plan-r2.224` 接替。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-GREEN-EXECUTION-001 / plan-r2.224` Green 暂停与下一入口回写（2026-08-23）：Human Owner 授权唯一生产文件 Green。Maker 仅修改 `LedgerTransactionServiceImpl.java`，统一三层 persisted v1、写后回读、同 key replay 既有身份绑定与全部 exact read fail-closed；compile=`21/21`，原 8 个目标 RED 已全部关闭。fresh focused=`56/3F/1E/0S`：Ledger writer=`10/0F/0E`、Ledger fact query=`8/0F/0E`、Gate=`10/0F/0E`，仅 `ClearingSplittableDetailApplicationServiceTests=28/3F/1E/0S`。4 个旧用例在删除 plan 或修改 business identity、subject/direction/role 后未重算 persisted v1，却仍要求进入 Clearing 业务判断，与已接受的 Ledger 先验完整性 fail-closed 冲突。独立 Checker=`NOT PASS / 0 P0 / 1 P1 / 0 P2`，唯一 P1 是执行卡不可达，不是已审生产范围的实现缺陷；Green 停在 focused，不继续扩大门禁。生产文件 SHA-256=`d5b8fd6993913b66e0bfd24dc38aed04b2b90c11d101fd17167e936b219affa4`，五个 immutable acceptance 文件 SHA 均未漂移，staged empty、`git diff --check` PASS。下一最小候选只允许文档先行重作 `ClearingSplittableDetailApplicationServiceTests.java` 单文件测试迁移卡：缺 plan 用例改证 Ledger parent-integrity fail-closed；其余三个用例在有意改变持久化业务字段后重算合法 v1 摘要，继续保留原 Clearing 过滤断言。不得修改 Clearing 生产代码、建立兼容层或放行未验证事实；该阶段以 `LEGACY_CLEARING_TEST_ENTRY_CARD_REWORK_GRANT_NO / GREEN_EXECUTION_PAUSED / CODE_FREEZE` 收口，当前由 `plan-r2.230` 接替。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-GREEN-LEGACY-CLEARING-TEST-ENTRY-CARD-REWORK-001 / plan-r2.225` Maker 记录（2026-08-23）：Human Owner 回复“授权推进，并分析确认其价值”，仅授权五文档 Entry Card 返工与独立 Checker。Maker 回读 4 个冲突方法、现有 persisted v1 builders 和 fresh XML，确认无需新增生产能力：缺失 posting plan 是损坏 aggregate，必须停在 Ledger parent-integrity；transaction business identity、entry subject/code 与 debit/decrease 语义若要继续测试 Clearing，必须先重算合法 persisted v1。卡片冻结未来测试 `MODIFY=1 / ADD=0 / DELETE=0`，只允许 `ClearingSplittableDetailApplicationServiceTests.java` 迁移 4 个既有方法并复用同文件私有 builder；执行前必须 fresh 保持 `56/3F/1E/0S`，迁移后 focused=`56/0F/0E/0S`，扩大门为 non-assembler Ledger=`50/0`、Reconciliation=`245/0`、Transaction=`178/0`、Business=`206/0`、compile=`21/21`、Public Contract=`315/187/42`。生产 SHA=`d5b8fd6993913b66e0bfd24dc38aed04b2b90c11d101fd17167e936b219affa4` 与其他 immutable SHA 不得漂移；不允许兼容层、Clearing/Ledger production、共享 fixture、API/DTO/schema/Mapper/POM 或 Consumer 修改。该阶段当时只进入独立 Entry Card Checker，`TEST_REWORK_EXECUTION_GRANT_NO / GREEN_EXECUTION_PAUSED / CODE_FREEZE`；当前由 `plan-r2.230` 接替。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-GREEN-LEGACY-CLEARING-TEST-ENTRY-CARD-REWORK-001 / plan-r2.226` 独立 Checker PASS 状态回写（2026-08-23）：Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 missing plan 的 `ledger entry + SN` 父引用 fail-closed 与三个 digest-valid/Clearing-ineligible 场景分层成立；AVAILABLE 同步归一 `ASSET` category，transaction builder 显式接收 businessSn 即可承载 source mismatch；四个原 DTO/异常/零副作用断言未弱化。唯一未来写入文件、`ADD=0 / DELETE=0`、执行前后精确计数、immutable SHA、无兼容和禁止生产扩张均闭合。该阶段当时只进入 Human Owner Test Rework Execution Grant 决策，`TEST_REWORK_EXECUTION_GRANT_NO / GREEN_EXECUTION_PAUSED / CODE_FREEZE`；当前由 `plan-r2.230` 接替。

`W5-MIG03-LEDGER-PERSISTED-DIGEST-GREEN-LEGACY-CLEARING-TEST-REWORK-EXECUTION-001 / plan-r2.227` Test Rework 与独立 Checker PASS 状态回写（2026-08-23）：Human Owner 回复“授权推进，并分析确认其价值”，只授权 `ClearingSplittableDetailApplicationServiceTests.java`。执行前 Java 21、compile=`21/21`，fresh baseline=`56/3F/1E/0S`；Maker 只迁移四个冻结方法，让 transaction digest builder 显式接收 businessSn 并机械迁移同文件 call sites。business mismatch、AVAILABLE/ASSET 与 DEBIT/DECREASE/REFUND 均使用同一 persisted v1 构造可验证事实后保留原 Clearing 排除；missing plan 不修摘要，保留 Ledger `ledger entry + SN` fail-closed、`detail=0`、`gate evidence=0` 与 Ledger/Balance unchanged。后置 compile=`21/21`、focused=`56/0F/0E/0S`、non-assembler Ledger=`50/0F/0E/0S`、Reconciliation=`245/0F/0E/0S`、Transaction=`178/0F/0E/0S`、Business=`206/0F/0E/0S`、Public Contract=`315/187/42`。完整 Ledger 诊断=`65/0F/15E/0S`，精确为一个 `MockitoInitializationException -> ByteBuddyAgent -> Could not self-attach` 根错误加 14 个 context failure-threshold 派生错误，不计行为 PASS。目标测试新 SHA-256=`1c54a19dd7ebe553253cb7a01f1ed952f906feb15e5c91aa8c22d40fc0ae786a`，生产与另四个 acceptance SHA 未漂移；default/`-uall` manifest=`21/92aad3131d88308033908bff233b656b748b4686e70392dd9bd236d2fa2a6e3e`、staged empty、`git diff --check` PASS。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。价值结论为同时保住 Ledger 对损坏事实的统一拒绝和 Clearing 对完整但不适用事实的独立判断，不是凑绿。MIG-03 Ledger Digest Green 当前关闭，所有本轮授权耗尽；下一入口只允许重新形成 W5 Entry Card。

`W5-MIG04-TRANSACTION-ORCHESTRATION-SURFACE-INTERNALIZATION-ENTRY-CARD-001 / plan-r2.228` Maker 记录（2026-08-24）：Human Owner 回复“授权推进，并分析确认其价值”，在 `W5-MIG03-NEXT-SLICE / ENTRY_CARD_REQUIRED` 上只授权下一文件级 Entry Card 与独立 Checker。Maker 对账 MIG-04/05/09、仓内 caller、`capte-domain` 与 `fincone` 窄搜索后选择 MIG-04：`PaymentInstrumentTransactionApplicationService` 与 `SpendControlTransactionConsumptionApplicationService` 仍在 wallet-face，而实现已在 transaction-impl；前者没有生产 Consumer，后者唯一生产 caller 是同模块 PaymentInstrument 编排，capte-domain 无引用，fincone 只有待实施设计文案。目标不新增 transaction-face 替代 facade，而是删除两个错误 Wallet Public interface，并把六个 action/consumption request 原位迁入 transaction-impl；Wallet 继续拥有支付工具准入和 SpendControlMovement 事实。MIG-05 core assembler/projection 仍有真实 Transaction/Ledger 依赖，不能靠删接口硬做；MIG-09 也未满足全量零调用。卡片冻结 RED=`1 MODIFY`、Green=`6 ADD / 14 MODIFY / 8 DELETE`，无 POM/schema/Mapper/兼容层；23 个既有 RED/Green 输入文件的有序 SHA-256 scope fingerprint=`07b05881cd05f0068624ba099350227a735e6135839f33fd83f8528b82a88dc2`。当前只进入独立 Entry Card Checker，`RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG04-TRANSACTION-ORCHESTRATION-SURFACE-INTERNALIZATION-ENTRY-CARD-001 / plan-r2.229` live HEAD 最小返工记录（2026-08-24）：首轮独立 Checker 判定 `NOT PASS / 0 P0 / 1 P1 / 0 P2`，唯一 P1 是 Metadata 仅保留历史 Baseline HEAD=`eb120918...`，未冻结当前文件卡 checkout。Maker 不改变方案、路径、计数、fingerprint 或授权，只新增 `entry_card_live_head=fc6b6e004a32eb1813534de05df7f844cc6edf95`，并要求 RED 前连同双 manifest、staged、diff-check 和 scope fingerprint 复核。该阶段当时只进入同一卡独立复核，`RED_EXECUTION_GRANT_NO / CODE_FREEZE`；当前由 `plan-r2.230` 接替。

`W5-MIG04-TRANSACTION-ORCHESTRATION-SURFACE-INTERNALIZATION-ENTRY-CARD-001 / plan-r2.230` 独立 Checker PASS 状态回写（2026-08-24）：Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认历史 Baseline HEAD 与 live HEAD 分层、23 文件 scope fingerprint、零生产 Consumer、MIG-04 优先级、RED=`1 MODIFY`、Green=`6 ADD / 14 MODIFY / 8 DELETE`、focused=`88`、Public Contract=`307/181/42` 和无兼容边界全部成立。当前只进入 Human Owner RED Execution Grant 决策，`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG04-TRANSACTION-ORCHESTRATION-SURFACE-INTERNALIZATION-RED-EXECUTION-001 / plan-r2.231` RED 执行与独立 Checker PASS 状态回写（2026-08-24）：Human Owner 授权唯一 RED 测试文件。执行前 Java 21、compile=`21/21`、fresh baseline=`26/0F/0E/0S`；Maker 只在 `FundsModuleDependencyBoundaryTests.java` 新增一个聚合测试，形成 fresh=`27/1F/0E/0S`。唯一 failure 完整列出 8 个仍存在的 wallet-face 路径、6 个缺失的 transaction-impl internal command，以及两个 concrete service 的 5 条旧 interface import/implements 绑定；没有 compile/error、第二 failure、生产或兼容实现。目标测试 SHA-256 从 `7398207b25a6c6b3fd8b7938f6c3162fd791edfe9e87dbf31c575103731e4a84` 变为 `38e42fe318436f1816a6f164b977128ffe2cd80049c3cf3920e388b35394a015`；22 个非 RED 文件有序指纹执行前后均为 `227a539d7d3610d782e76ee7323e648fba96bfb75b986452c7c74b4c536aea63`。后置 compile=`21/21`，default/`-uall` manifest=`22/7ffb832f8724110e7c3ea3d935b87b51b1c6c00c392e66d551f49bcfc53fb269`、staged empty、`git diff --check` PASS。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。RED 已证明 Wallet Public surface 与 Transaction 编排 Owner 错位，下一只进入 Green Execution Human Owner Grant；`GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG04-TRANSACTION-ORCHESTRATION-SURFACE-INTERNALIZATION-GREEN-EXECUTION-001 / plan-r2.232` Green 执行与独立 Checker PASS 状态回写（2026-08-24）：Human Owner 在 r2.231 唯一 Green 入口上要求中断恢复继续，Maker 写入前明确按冻结文件卡执行。实际精确为 `6 ADD / 14 MODIFY / 8 DELETE`：六个 action/consumption request 只改 package 与 Owner 说明迁入 transaction-impl；删除两个 wallet-face facade 和六个旧请求；三个生产实现删除旧 interface import/implements/`@Override` 并改用内部命令；四个行为测试只迁移 import/注入类型；七份接入/专题文档统一为 canonical Funds Service + Provider internal orchestration。没有 transaction-face 替代 facade、兼容 alias/V2、POM/schema/Mapper、其他 Consumer 或 MIG-05/08/09 扩张。fresh pre-Green RED=`27/1F/0E/0S`；后置 clean compile=`21/21`、focused=`88/0F/0E/0S`、transaction=`178/0F/0E/0S`、Public Contract=`307/181/42`。immutable RED SHA=`38e42fe318436f1816a6f164b977128ffe2cd80049c3cf3920e388b35394a015`，20 个最终 Green 存量文件有序指纹=`bdd269e67a4270917dd7489c20b5682e618674716f9b5b7803c2e2e4ccd5c504`；default/`-uall` manifest=`50/834000bd756c1fc37c1fb87f20741d5bec7e2943d2e2dd41aa89e99faa2e383d`、staged empty、`git diff --check` PASS。独立 Checker 与格式增量 Checker 均=`PASS / 0 P0 / 0 P1 / 0 P2`。额外 Wind scanner 在 `check_redundant_jspecify_checks` 长跑后主动中止，不属于文件卡硬门，未声明 PASS；VCC 专题与旧交易系分的完整模板结构检查保留既有栏目缺口，不为本切片扩写。MIG-04 当前关闭，所有本轮授权耗尽；下一入口必须重新形成 W5 Entry Card。

`W5-MIG05C-LEDGER-EXTENSION-SURFACE-COLLAPSE-ENTRY-CARD-001 / plan-r2.233` Maker 与首轮 Checker 返工记录（2026-08-24）：Human Owner 选择 A，并授权下一文件级 Entry Card 与价值确认。源码证明 `LedgerBalanceProjectionService` 只有一个生产实现且只被 ledger-impl 写链消费，`DefaultLedgerTransactionPostingServiceImpl` 的 `List + supports` 没有第二实现；`CompositeLedgerPostingAssembler` 也只有一个真实 delegate，Default assembler 对非空 legs 一律支持。A 只删除 projection interface/composite，直接使用唯一 projector 并移除 assembler `Ordered`；仍被 Transaction 跨模块调用的 `LedgerPostingAssembler`、posting gateway/specs 全部延期到独立高阶 command Contract Inquiry。首轮 Checker=`NOT PASS / 0 P0 / 1 P1 / 0 P2`，唯一 P1 是漏列 `FundsAuthorizationTransactionFlowTests` 对 support 字段单参数 `project(entries)` 的直接调用。卡片不保留 concrete 兼容重载，只把该测试加入机械迁移，Green 修正为 `2 DELETE / 18 MODIFY / 0 ADD`。Core API 目标仍为 `104/96/4/4 / 1039 lines`，Public Contract=`306/181/42`。当前 fresh Boundary=`27/0`，七类无 Mockito集群=`84/0`；JDK proxy=`1/1F` 只因 ByteBuddy self-attach。live HEAD=`fc6b6e004a32eb1813534de05df7f844cc6edf95`，manifest=`50/834000bd756c1fc37c1fb87f20741d5bec7e2943d2e2dd41aa89e99faa2e383d`，返工后 21 文件 scope fingerprint=`0e8c8c1a5f30e3b969a5dd381893afe03bd461ee574c6a60fa747825a492167b`。当前再次进入独立 Entry Card Checker，`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05C-LEDGER-EXTENSION-SURFACE-COLLAPSE-ENTRY-CARD-001 / plan-r2.234` 独立 Checker PASS 状态回写（2026-08-24）：首轮唯一 P1 通过补入 `FundsAuthorizationTransactionFlowTests` 直接调用关闭；不保留 concrete 单参数兼容重载。最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`，确认 Green=`2 DELETE / 18 MODIFY / 0 ADD`、九个测试/support、scope 21、Green 20、Core API `104/96/4/4 / 1039 lines`、Public Contract `306/181/42`、无兼容和 posting command 延期全部成立。当前只进入 Human Owner RED Execution Grant 决策，`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05C-LEDGER-EXTENSION-SURFACE-COLLAPSE-RED-EXECUTION-001 / plan-r2.235` RED 执行与独立 Checker PASS 状态回写（2026-08-24）：Human Owner 明确授权当前唯一 RED Gate。执行前 Java 21、compile=`21/21`、Boundary=`27/0F/0E/0S`，HEAD、双 manifest、staged、diff-check、目标 SHA 与 21 文件 scope fingerprint 均稳定。Maker 只在 `FundsModuleDependencyBoundaryTests.java` 新增 `testLedgerProjectionAndAssemblerExtensionRoutersShouldCollapse`，不编译引用待删除类型，形成 Boundary=`28/1F/0E/0S`；唯一 failure 聚合暴露两个待删除路径、projection interface list/router、旧 implements 与 assembler `Ordered/getOrder` 七项残留。无 Mockito 行为保护组合=`85/1F/0E/0S`，其余 `84` 项全绿；目标 SHA 从 `38e42fe318436f1816a6f164b977128ffe2cd80049c3cf3920e388b35394a015` 变为 `93bb5146966a34338ba55ec95a846c57cd013167258c664c37a1de9d95df7caa`，Green 20 文件指纹保持 `68aff74f0df892e318dbc7b7d417e84568bed482119ee21269bcacc1465611ea`。后置 compile=`21/21`、manifest=`50/834000bd756c1fc37c1fb87f20741d5bec7e2943d2e2dd41aa89e99faa2e383d`、staged empty、`git diff --check` PASS；独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。RED 已把宿主通过 Bean 列表或排序分叉 Ledger 资金不变量的风险变成可执行删除契约；当前只进入 Human Owner Green Execution Grant 决策，`GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05C-LEDGER-EXTENSION-SURFACE-COLLAPSE-GREEN-EXECUTION-001 / plan-r2.236` Green 执行与独立 Checker NOT PASS 状态回写（2026-08-24）：Human Owner 授权原冻结 `2 DELETE / 18 MODIFY / 0 ADD` 无兼容 Green。Maker 删除 core projection interface 与单 delegate composite，posting service 直接依赖唯一 concrete projector，支持性校验仍在账本事实落库前，投影仍在同一本地事务；测试只迁移 concrete type 与显式 `NORMAL`，Core baseline 只删除四条目标 signature，三份旧设计删除 Public/multi-provider 承诺。clean compile=`21/21`、Boundary=`28/0F/0E/0S`、focused=`85/0F/0E/0S`、transaction=`178/0F/0E/0S`、Core API=`104/96/4/4 / 1039 lines` 均通过；Public Contract 实际=`307/181/42`，JDK proxy=`1/1F/0E/0S` 仍为 ByteBuddy self-attach。独立 Checker=`NOT PASS / 0 P0 / 2 P1 / 0 P2`：原卡误把不扫描 core 的 face-only Public Contract 目标写成 `306`，且白名单外稳定 `LedgerPostingAssembler` Javadoc 仍指向已删除 composite。实现无需回退，但不得宣称 Green PASS；当前只进入 Human Owner 最小 Entry Card rework 授权决策，候选为 `2 DELETE / 19 MODIFY / 0 ADD`，新增文件仅 comment-only Javadoc 校正，`GREEN_ENTRY_CARD_REWORK_GRANT_NO / CODE_FREEZE`。

`W5-MIG05C-LEDGER-EXTENSION-SURFACE-COLLAPSE-GREEN-ENTRY-CARD-REWORK-001 / plan-r2.237` Maker 记录（2026-08-24）：Human Owner 回复“授权，并分析确认其价值”，仅授权五份权威文档重作最小 Green 文件卡。返工确认 Public Contract 脚本只扫描五个 face roots，删除 core `LedgerBalanceProjectionService` 不改变计数，正确目标为 `307/181/42`；Green 白名单重冻为 `2 DELETE / 19 MODIFY / 0 ADD`，新增第 19 个 MODIFY 仅为 `core/src/main/java/com/wind/funds/ledger/LedgerPostingAssembler.java` comment-only Javadoc 校正，不改接口签名、实现行为、Core API baseline 或 posting command。当前 21 路径状态指纹=`7519cb3e75b11942d7b1680d6447b01d717fde7f7ac7b684a48e3bbbfcb3d807`，该 core 文件执行前 SHA=`c37149b77af6208939506db4e0d6f020b1535df2dae6e4f78b9c64b624039943`，immutable RED SHA=`93bb5146966a34338ba55ec95a846c57cd013167258c664c37a1de9d95df7caa`。本轮不修改源码、测试或业务设计；价值是让验证口径与真实脚本一致，并消除稳定接口对已删除 composite 的错误说明，避免下一轮临时扩权或引入兼容层。当前只进入独立文件卡 Checker，`GREEN_REWORK_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05C-LEDGER-EXTENSION-SURFACE-COLLAPSE-GREEN-ENTRY-CARD-REWORK-001 / plan-r2.238` 独立 Checker PASS 状态回写（2026-08-24）：Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 Public Contract face-only 目标 `307/181/42`、Green `2 DELETE / 19 MODIFY / 0 ADD`、第 19 个 MODIFY 仅 `LedgerPostingAssembler.java` comment-only、全部路径唯一必要职责、三项 SHA/指纹和无兼容停止线均成立；本轮未修改生产或测试源码。当前只进入 Human Owner Green Rework Execution Grant 决策，`GREEN_REWORK_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05C-LEDGER-EXTENSION-SURFACE-COLLAPSE-GREEN-REWORK-EXECUTION-001 / plan-r2.239` Green rework 与独立 Checker PASS 状态回写（2026-08-24）：Human Owner 回复“授权，并分析确认其价值”，授权当前唯一 comment-only rework。Maker 只修改 `core/src/main/java/com/wind/funds/ledger/LedgerPostingAssembler.java` 一行 Javadoc，把已删除 composite 委托说明校正为具体实现声明路径适用性；接口泛型、签名、注解、实现和行为均未改变。目标 SHA 从 `c37149b77af6208939506db4e0d6f020b1535df2dae6e4f78b9c64b624039943` 变为 `4d97fdf3effcf6921f76e2e02040f7231c6b9f9bcc112a4d0e66e2a97e4ee045`，RED SHA 保持 `93bb5146966a34338ba55ec95a846c57cd013167258c664c37a1de9d95df7caa`，21 路径 post fingerprint=`714fb0528b52d85fa5ea3d70bea446c5d1394f959ffe5f4e071f9dfa56d00366`。Java 21 clean compile=`21/21`、focused=`85/0F/0E/0S`、transaction=`178/0F/0E/0S`、Core API=`104/96/4/4 / 1039 lines`、Public Contract=`307/181/42`；JDK proxy=`1/1F/0E/0S` 仍唯一为 Mockito/ByteBuddy self-attach 环境 observation。HEAD=`fc6b6e004a32eb1813534de05df7f844cc6edf95`，default/`-uall` manifest=`68/7a68c0ebaba16d49ea2f684062605ed09da5bd7f273f47074a882f81e5503a5d`、staged empty、`git diff --check` PASS。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。MIG-05C 当前范围关闭，所有本轮授权耗尽；下一 W5 切片必须重新形成 Entry Card。

`W5-MIG02B-AUTHORIZATION-RELEASE-ACTIONFACT-CONTRACT-ENTRY-CARD-001 / plan-r2.240` Maker 记录（2026-08-24）：Human Owner 回复“按你的建议推进”，仅授权五份权威文档和独立 Checker。源码确认 REVERSAL 复用 authorization root，在同 root 下持久化唯一 business action group、RELEASE details、原 authorization/ledger refs、原 route replay 与 `reversedAmount`；当前 query 只缺 release projector。Contract 复用既有 `FundsTransactionQueryService`、`FundsActionFactDTO/Ref/Query`，冻结 action/reverse-intent/attempt identity、`releases-authorized-effect`、`replayed-original-route` 与 digest `transaction.action.release.projection(.v1)`，不新增 Public surface、表、Mapper 或写链。未来 RED=`1 MODIFY`、Green=`1 MODIFY`、`ADD=0 / DELETE=0`；RED 7 tests 将 focused 从 `53/0` 变为 `60/7F`、transaction 从 `178/0` 变为 `185/7F`，Green 分别回到 `60/0` 与 `185/0`。两文件 scope fingerprint=`6168a8669a1bd1a30cd4a70a41f50f8b6a70551ffe42a30c605ebc809afa6bd8`；HEAD=`fc6b6e004a32eb1813534de05df7f844cc6edf95`，default/`-uall` manifest=`68/7a68c0ebaba16d49ea2f684062605ed09da5bd7f273f47074a882f81e5503a5d`、staged empty、`git diff --check` PASS。本轮不修改源码/测试、不执行测试；当前只进入独立 Contract/Entry Card Checker，`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG02B-AUTHORIZATION-RELEASE-ACTIONFACT-CONTRACT-ENTRY-CARD-REWORK-001 / plan-r2.241` 首轮 Checker NOT PASS 与最小返工记录（2026-08-24）：独立 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 0 P2`。P1-1 指出原文冒号拼接对合法 Capte 分段业务键不是单射，且当前主表优先/detail fallback 会遮蔽同 key release；返工改为 UTF-8 Base64URL 无 padding 的 v1 三层身份，并冻结跨主表/detail、action family、authorization root 冲突时列表与 identity 双查询 fail-closed。P1-2 指出 release 不能只校验 `reversedAmount` 与 root 不等式；返工要求复用完整 complete group/cumulative，使 verified COMPLETE/REVERSAL 分别精确闭合 root completed/reversed。两个 P1 均并入原 7 个 RED 方法，不增加文件、方法数、Public API、DTO、schema 或写链；scope fingerprint、SHA、`60/7F`、`185/7F` 与全部排除边界不变。当前只进入独立复核，`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG02B-AUTHORIZATION-RELEASE-ACTIONFACT-CONTRACT-ENTRY-CARD-REWORK-001 / plan-r2.242` 独立 Checker PASS 状态回写（2026-08-24）：Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，确认 Base64URL v1 三层身份无歧义、跨 table/family/root 业务键冲突双查询 fail-closed、verified COMPLETE/REVERSAL 分别闭合 root completed/reversed、单次 digest 不吸收可变累计，以及 RED `1 MODIFY`、Green `1 MODIFY`、`ADD=0 / DELETE=0`、7 tests、`60/7F`、`185/7F`、scope/SHA/manifest 和全部排除项均成立。本轮未修改或运行源码/测试。当前只进入 Human Owner RED Execution Grant 决策，`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG02B-AUTHORIZATION-RELEASE-ACTIONFACT-RED-EXECUTION-001 / plan-r2.243` RED 执行与 Checker NOT PASS 记录（2026-08-24）：Human Owner 明确授权当前单测试文件 RED。Maker 只在 `FundsAuthorizationTransactionFlowTests.java` 增加冻结的 7 个 release ActionFact 测试和同文件私有 helper；fresh focused=`60/7F/0E/0S`、transaction=`185/7F/0E/0S`，7F 均停在首次 `release action fact missing ... Expected size: 1 but was: 0`，compile=`21/21`。独立 Checker 判定 `NOT PASS / 0 P0 / 2 P1 / 0 P2`：多个目标用例未逐 release 证明 Balance 与原 HOLD/replay 精确映射；首笔 release DTO 未在同 root 后继累计变化后回查，错误 digest 仍可能过测。test SHA=`b74f70461615d8bf02fe8cb59a10b3858dc298f5d0d0783d73ee828cc42daef6`，query impl SHA 保持 `32e4123b906a0355f328c666074f2d74448aafd88a1df4dc0f5d230e6f547a57`，scope fingerprint=`ef761fab6a8d820f76ceca77d0f0797a6f3806efce9d10a10017dd62ae38b759`。该状态不准入 Green，只进入同文件最小 RED rework Human Owner Grant。

`W5-MIG02B-AUTHORIZATION-RELEASE-ACTIONFACT-RED-REWORK-001 / plan-r2.244` RED rework 与独立 Checker PASS 状态回写（2026-08-24）：Human Owner 回复“授权推进，做价值分析”，只授权上一轮两项 P1 的同文件最小返工。Maker 保持 60 个测试方法和 7 个 release RED 名称不变：逐 release 补 Funding/SHARED Balance delta；`assertAuthorizationReleasePhysicalFacts` 按 authorization root、REVERSAL detail/Ledger 与同主体原 HOLD leg 精确核验 replay consumed ID/amount、`RELEASE_<originalLegId>` posting、Ledger ref、Money/currency、intent/scope/phase 和 entries；首笔 release DTO 后分别推进后继 complete 与第二笔 release，再回查原 DTO，明确历史 digest 不吸收 mutable `completedAmount/reversedAmount`。最终 fresh focused=`60/7F/0E/0S`、transaction=`185/7F/0E/0S`，其余 178 项通过；7F 仍全部精准缺 release projector。Java 21 compile/clean compile=`21/21`，Public Contract=`307/181/42`，Core API=`96 stable / 4 experimental / 4 internal`。test SHA=`3d292c4b200383c5511743348a10291013078b2f682f8ef18e9714f554055db9`，authorization XML SHA=`bc0179ec0477ca817f632b19fe23c88c03ec86af5f1d4a5954e2693c342bd358`，两文件有序 scope fingerprint=`94d2125cd2f2bb3ce59a495ca5a220cf0ea3e01bb329f96c309ad888ff5240da`；四个 face 与 query impl SHA 未漂移。HEAD=`fc6b6e004a32eb1813534de05df7f844cc6edf95`，default/`-uall` manifest=`68/7a68c0ebaba16d49ea2f684062605ed09da5bd7f273f47074a882f81e5503a5d`，staged empty，`git diff --check` PASS。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。该 PASS 只证明受控 RED 合同成立；当前只进入 Human Owner Green Execution Grant 决策，`GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG02B-AUTHORIZATION-RELEASE-ACTIONFACT-GREEN-EXECUTION-001 / plan-r2.245` Green 执行与独立 Checker PASS 状态回写（2026-08-24）：Human Owner 回复“授权推进，做价值分析”，授权 r2.244 唯一单文件 Green。Maker 只修改既有 `DefaultFundsTransactionQueryService.java`：业务列表、release identity 与普通 main identity 执行同业务键歧义门，跨 main/detail、COMPLETE/REVERSAL family 和 authorization root 冲突 fail-closed；Base64URL v1 使用 UTF-8/no-padding 并重新编码校验 canonical；release projector 校验成功 authorization、正 Money、完整责任 sibling、原/本次不同 Ledger ref、逐 HOLD replay，以及 verified COMPLETE/REVERSAL 分别闭合 root completed/reversed。projection digest 覆盖三层身份、outcome/effect、原 authorization identity + digest algorithm/value/version、allocation、排序 sibling、Ledger、原 route 与 replay，并排除 root mutable cumulative、余额、描述和时间。没有新增 Public API/DTO、DDL/schema、Mapper、写链、Consumer、service、registry、factory 或兼容层。最终 Java 21 compile=`21/21`、focused=`60/0F/0E/0S`、transaction=`185/0F/0E/0S`、Public Contract=`307/181/42`、Core API=`96 stable / 4 experimental / 4 internal`。query impl SHA=`acd8ad0203ae0334b1431b8fba8667edeffba733b49546b19a86da4c032938c7`，immutable test SHA=`3d292c4b200383c5511743348a10291013078b2f682f8ef18e9714f554055db9`，authorization XML SHA=`6d19702fdca8ed8b3a15bdaa175e43f311867b76f04cec381254515dffa823f7`，两文件有序 scope fingerprint=`7d8a4e387accb63a8e2a1382ef49a777cabeaaba6b0cd40265e2f0cf5795f701`；四个 face SHA 未漂移。HEAD=`fc6b6e004a32eb1813534de05df7f844cc6edf95`，default/`-uall` manifest=`68/7a68c0ebaba16d49ea2f684062605ed09da5bd7f273f47074a882f81e5503a5d`，staged empty，`git diff --check` PASS。独立 Green Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。MIG-02B 当前 Provider 范围关闭；Consumer 接入、外部 finality、真实 MySQL、PMD、Git、发布和生产均未验证或授权。下一 W5 切片必须重新形成 Entry Card，`ENTRY_CARD_REQUIRED / EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-DECISION-PACKAGE-001 / plan-r2.246` Maker 记录（2026-08-24）：Human Owner 回复“授权推进，做价值分析”，仅授权下一切片设计裁决与价值确认。源码闭包确认 `LedgerPostingAssembler` 与 `LedgerTransactionPostingService#post(LedgerTransactionSpec)` 只有一个仓内生产 caller，`capte-domain` 无生产调用、`fincone` 只有设计材料；当前 Ledger 写链会继续校验资金不变量，问题属于公共写入责任而非已证实资金漏洞。决策包列出 A 高阶单命令、B 保留双低层端口、C 移包三案，推荐 A，C 因不收回组装权且违反依赖方向被排除；posting spec 继续作为 core 记账 DSL，不预删稳定语义。七个证据文件有序指纹=`11639c5b52cc4635a4b922e548cdbedc9576c2f66fd23be659268e53afd1ee63`；HEAD=`fc6b6e004a32eb1813534de05df7f844cc6edf95`，default/`-uall` manifest=`68/7a68c0ebaba16d49ea2f684062605ed09da5bd7f273f47074a882f81e5503a5d`、staged empty、`git diff --check` PASS。本轮只修改既有五份权威文档，不修改或运行 Java/测试；当前进入独立 Checker，Owner 尚未选择 A/B，`ENTRY_CARD_REQUIRED / EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-DECISION-PACKAGE-REWORK-001 / plan-r2.246` 首轮 Checker NOT PASS 与返工记录（2026-08-24）：独立 Checker 判定 `NOT PASS / 0 P0 / 1 P1 / 1 P2`。P1 指出原 A 只收回低阶 posting 组装权，却没有要求 Ledger 独立复验 instruction/route 六个 identity 字段，也没有关闭 `fundsTransactionSn` 稳定命令身份的同摘要重放/异摘要冲突；当前校验只在 `CompositeRouteResolver`，assembler 会先生成时序 Ledger SN，现有写链只按该 SN replay。P2 指出 Capte 生产模块还依赖 wallet-face。返工在原五文档内补齐六字段独立校验、非空命令身份、同 `tenantId + fundsTransactionSn` 同 digest 返回同一 persisted SN、异 digest 冲突且零新增，并把 Capte 依赖校正为 core/transaction-face/wallet-face；不冻结物理 SN 映射、Java 签名、文件白名单或 RED 数量。首轮复核确认业务与架构缺口关闭，但因十三文件顺序未显式冻结再次判定 `NOT PASS / 0 P0 / 0 P1 / 1 P2`；随后以系分 11.31.3 的精确 1~13 路径顺序重算为 `f01da0518faddc44cced8befb6c476c11d06f615a63d1bf54d29de22a828a404`。当前进入独立复核，`OWNER_DECISION_PENDING / EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-DECISION-PACKAGE-REWORK-001 / plan-r2.247` 独立 Checker PASS 状态回写（2026-08-24）：最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。确认 instruction/route 六字段独立复验、`tenantId + 非空 fundsTransactionSn` 稳定命令身份、同 identity/digest 返回同一 persisted SN、异 digest 冲突且零 Ledger/Balance 新增、Capte core/transaction-face/wallet-face 依赖与无 production posting caller、A/B/C 公平性、C 排除理由、posting specs 留 core、`accepted_answer=none` 和未来 RED 责任均成立；精确 13 路径顺序复算 fingerprint=`f01da0518faddc44cced8befb6c476c11d06f615a63d1bf54d29de22a828a404`。四项文档结构检查均通过，产品仅有非阻断 `implementation_language` WARN；HEAD=`fc6b6e004a32eb1813534de05df7f844cc6edf95`，双 manifest=`68/7a68c0ebaba16d49ea2f684062605ed09da5bd7f273f47074a882f81e5503a5d`，staged empty，`git diff --check` PASS。本轮未修改或运行 Java/测试。当前只进入 Human Owner A/B 选择门，`OWNER_DECISION_PENDING / ENTRY_CARD_REQUIRED / EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-A-ACCEPTANCE-ENTRY-CARD-001 / plan-r2.248` Maker 记录（2026-08-24）：Human Owner 回复“授权推进 A，做价值分析”，正式接受 `A / SINGLE_HIGH_LEVEL_CORE_POST_COMMAND` 并授权文档 Acceptance/Entry Card。源码闭包将候选物理化为 core 三输入/String 返回高阶命令、Ledger 六字段独立校验、`tenantId + fundsTransactionSn` canonical identity、`LE + 48 hex` 确定性 SN、现有唯一键与 persisted aggregate digest；projection context 直接复用 lifecycle result SN，不增加重复字段。执行分为 surface RED/Green 与 behavioral RED/Green；唯一 19 文件闭包=`1 DELETE + 18 MODIFY / ADD=0`，fingerprint=`25c12e2dae73722cc7155f458350e2ec4fa362b234d36b54a99970a88a4762a4`。Surface RED=`29/1F`、Surface Green focused=`61/0`、Behavioral RED=`6 methods / 11 invocations / focused 72/11F`、Behavioral Green focused=`72/0`；目标 Core API=`103/95/4/4 / 1036 lines`，Public Contract=`307/181/42`。当前物理 API 仍为 `104/96/4/4 / 1039 lines`；`D-CS-006-T` 仅为 accepted target。文档写入限定产品/DSL/系分/TDD、主 OpenSpec 与必要 Core stability OpenSpec，未修改或运行 Java/测试；当前进入独立 Entry Card Checker，`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-A-ACTION-IDENTITY-ENTRY-CARD-REWORK-001 / plan-r2.248` Checker NOT PASS 与返工记录（2026-08-24）：首轮 Entry Card Checker=`NOT PASS / 0 P0 / 1 P1 / 0 P2`。P1 证明 `DefaultFundsInstructionLifecycleSaver` 对 COMPLETE/REVERSAL 返回 authorization root，orchestrator 将同一 root SN 交给 Ledger，既有 authorization flow 已形成同 root 的 AUTHORIZE、COMPLETE、REVERSAL、再次 COMPLETE 四条 LedgerTransaction；原 `tenantId + fundsTransactionSn` 会把合法后继动作聚合为同一 SN 并触发异 aggregate 冲突。返工把 identity 改为 `tenantId + fundsTransactionSn + eventType + businessScene + businessSn`，保留六字段一致性与 identity/aggregate digest 分工；同 root 不同 action 生成不同稳定 SN 并各自重放。Behavioral RED 增加一个方法/一个 invocation，总计 `7 methods / 12 invocations`，class=`30/12F`、focused=`73/12F`、non-assembler Ledger Green=`62/0`。A/signature/19 文件/fingerprint/Core API/Public Contract/无 schema 边界均不变；当前进入独立复核，`RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-A-ACTION-IDENTITY-ENTRY-CARD-REWORK-001 / plan-r2.249` 独立 Checker PASS 状态回写（2026-08-24）：最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。确认 action identity 与现有 Ledger 精确查询键、authorization action group 一致；同 root 的 AUTHORIZE/多个 COMPLETE/多个 REVERSAL 生成不同稳定 SN，同 action 重放返回同一 SN，Money/route/instructionType/transactionType 等继续由 aggregate digest 判冲突。`LE + 48 hex`、现有唯一键、无 schema、projection lifecycle SN、package-private seam、Capte test composition、四 checkpoint、19 文件 fingerprint=`25c12e2dae73722cc7155f458350e2ec4fa362b234d36b54a99970a88a4762a4`、Core API=`103/95/4/4 / 1036` 与 Public Contract=`307/181/42` 均准出；Behavioral RED=`7 methods / 12 invocations / class 30/12F / focused 73/12F`，Green focused=`73/0`、non-assembler Ledger=`62/0`。HEAD、68 项 manifest、staged 与 diff-check 稳定，未修改或运行 Java/测试。当前只进入 Contract Surface RED Human Owner Gate，`CONTRACT_SURFACE_RED_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-A-CONTRACT-SURFACE-RED-EXECUTION-001 / plan-r2.250` RED 执行、返工与独立 Checker PASS 状态回写（2026-08-24）：Human Owner 回复“授权推进，做价值分析”，只授权唯一 Boundary 测试 RED。Maker 新增 `testLedgerPostingCommandShouldOwnAssemblyAndStableIdentity`；首轮 fresh 已为 `29/1F/0E`，但 Checker=`NOT PASS / 0 P0 / 1 P1 / 1 P2`，指出 concrete impl 可保留 public raw-spec 旁路，且 interface/orchestrator 两个源码正例过度绑定写法。返工使用 reflection 锁定 core 单一三参数/String `post`、concrete 唯一 package-private `postAssembled(LedgerTransactionSpec)` 并禁止 public/protected raw-spec 方法；删除参数名和内联调用正例。一次括号错误产生 testCompile，修正后重新 fresh，不计入 RED。最终 pre/post compile=`21/21`、Boundary=`29/1F/0E/0S`、focused=`61/1F/0E/0S`，唯一 failure 为新方法，其余 60 项通过；test SHA=`b05c78e7a0542b1302062f81481136948b12b229fadf530a1d00e5dd31f4e31d`、XML SHA=`e063473d228b6ee16a3f594bb6901c4e1976ae684b863db96266a603a1bbaf1e`、19 文件 post fingerprint=`f57b9a0a5f22ad4e2e175d4a98c9dca0f7ae23bd91594de5cb34a854e7674f0b`；HEAD、68 manifest、staged empty、diff-check PASS。最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。当前测试 immutable，只进入 Surface Green Human Owner Gate，`CONTRACT_SURFACE_GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-A-CONTRACT-SURFACE-GREEN-EXECUTION-001 / plan-r2.251` Green 执行与独立 Checker PASS 状态回写（2026-08-24）：Human Owner 回复“授权推进，做价值分析”，只授权 Surface Green。Maker 精确执行 `1 DELETE + 16 MODIFY`：删除 core `LedgerPostingAssembler`，将 `LedgerTransactionPostingService` 收口为 `post(FundsInstructionSpec, String, ResolvedRouteSpec) -> String`，concrete raw seam 只留 package-private `postAssembled`；orchestrator 直接消费返回 SN，projection context 删除 raw spec 并复用 lifecycle SN。Boundary SHA=`b05c78e7a0542b1302062f81481136948b12b229fadf530a1d00e5dd31f4e31d`、factory SHA=`4335ab19447a26ffd27f29e6a07e23d55005bd1f1676c20e04aa6bc982b84ed4`保持不变，未提前实现 Behavioral identity/replay。fresh compile=`21/21`、focused=`61/0F/0E/0S`、Core API=`103/95/4/4 / 1036 lines`、Public Contract=`307/181/42`、core=`106/0`、transaction=`185/0`、reconciliation=`245/0`、business-flow=`213/0`。JDK proxy=`1/1F`、assembler=`15/15E` 仍只是 Mockito/ByteBuddy self-attach 环境观察，未冒充 PASS。19 路径 post-Surface fingerprint=`314d7a3d9e4f29bca80bd073511f737ea135bba73935f48dc08b43c06c470f0b`；HEAD=`fc6b6e004a32eb1813534de05df7f844cc6edf95`，状态回写前 manifest=`77/8c66a1e0dd12eb812095ef0737457966de894341401313aac063e30e0669e858`，staged empty，`git diff --check` PASS。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。当前只进入 Behavioral RED Human Owner Gate，`BEHAVIORAL_RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-A-BEHAVIORAL-RED-EXECUTION-001 / plan-r2.252` RED 执行、Checker P1 与 Owner 处置记录（2026-08-25）：Human Owner 只授权冻结 Behavioral RED。Maker 唯一修改 `DefaultLedgerTransactionPostingServiceImplTests`，新增 7 方法/12 invocations；真实 Spring/H2 class=`30/12F/0E/0S`、focused=`73/12F/0E/0S`，其他 61 项通过。12F 精确命中六字段复验、tenant/root identity、same-action replay/conflict/concurrency 和 same-root different-action replay 缺口。test SHA=`144fe653951b513dd74f5a1b8d25b3881d0d37d0ee634b240c67669f64ba4324`，XML SHA=`dcebac107a06dbfea74832f892830ffee3d4392b43c6f692174d655098b00381`，19 路径 fingerprint=`95c1135a0b09add4e8683b304d7a1bfbc8da28339e1f7f36088a12f97f10a144`，Boundary/factory immutable，HEAD/manifest=`fc6b6e... / 77 / 8c66a1e0...`、staged empty、diff-check PASS。

首轮独立 Checker 确认上述技术 RED PASS，但因普通 `just compile` 的 Maven Snapshot 策略自动访问私有仓库，对未单独授权联网判定 `NOT PASS / P1=1`。Maker 已以 exit 130 中断，之后只使用 Maven `-o`，offline compile=`21/21`、classfile guard PASS，未改动技术 RED。Human Owner 随后明确接受该授权偏差，并授权六文档记录与独立 Checker 复核；该处置不追认原联网动作为已授权，只形成可审计事实和后续 offline-only 停止线。当时进入 `BEHAVIORAL_RED_INDEPENDENT_CHECKER_RECHECK_PENDING / BEHAVIORAL_GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-A-BEHAVIORAL-RED-CHECKER-RECHECK / plan-r2.253` 独立复核收口记录（2026-08-25）：Checker 最终=`PASS / 0 P0 / 0 P1 / 0 P2`，确认技术 RED 证据与 SHA/fingerprint 未漂移，六文档对未授权 Maven Snapshot 联网、exit 130、Owner 接受但不追认、后续 offline-only 停止线的记录一致，且不存在第二活动权威。当时只进入 Behavioral Green Human Owner Gate，`BEHAVIORAL_RED_INDEPENDENT_CHECKER_PASS / BEHAVIORAL_GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG05D-LEDGER-POSTING-COMMAND-A-BEHAVIORAL-GREEN-EXECUTION / plan-r2.254` Green 执行与独立 Checker PASS 状态回写（2026-08-25）：Human Owner 回复“授权推进，做价值分析”，只授权冻结的 Behavioral Green。Maker 仅修改 posting service impl 与 spec factory：在任何组装/事实访问前复验 tenant、非空 root 与 instruction/route 六字段；以 `ledger.posting.command.identity` 的五字段 canonical v1 摘要生成 `LE + 48 hex`，删除时序 SN 和无 root 重载。现有 persisted aggregate digest、唯一键、DuplicateKey winner readback 与 Balance 本地事务未改；未新增 schema/UK/lock/cache/幂等表、第二摘要、兼容层或第二写链。

所有 Maven 均离线执行。fresh compile=`21/21`、posting=`30/0F/0E/0S`、focused=`73/0F/0E/0S`、去重扩大=`638/0F/0E/0S`；分组 core=`106/0`、non-assembler Ledger=`62/0`、transaction=`185/0`、reconciliation=`245/0`、business-flow=`213/0`。Core API=`103/95/4/4 / 1036 lines`、Public Contract=`307/181/42`，19 路径 fingerprint=`856227ce0b566c2bd51f9240ff81365ee4e759db3b13ea6f23cb172d10714fe2`；目标生产 SHA=`af65602c... / 583ad35c...`，immutable RED/Boundary SHA=`144fe653... / b05c78e7...`。HEAD/manifest=`fc6b6e... / 78 / 00d618fa...`，staged empty、diff-check PASS。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`；MIG-05D 当前范围关闭。

`W5-REFACTORING-PROGRESS-REBASE-002 / plan-r2.255` 文档重基线记录（2026-08-25）：Human Owner 接受推荐 A，只授权产品、DSL、系分、TDD 与主 OpenSpec 五文档校准和独立 Checker。重基线关闭三类陈旧表述：MIG-02B release 已在 `plan-r2.245` Green；MIG-03 已完成 Action/Ledger/Balance 与 persisted v1 exact-read 当前范围；MIG-05A~D 已完成 Ledger internalization 当前范围。MIG-02C refund 延期、MIG-06/08 Host/E4 和 MIG-09 Consumer cutover/zero-call blocker 均保持，不因重基线降低。

该卡的实际价值是阻止执行者重复实现 release、重复清理已删除 assembler 或越过 Consumer/finality 门禁；它不新增用户功能、DSL、Public API、代码、测试、schema 或 Consumer。当时只进入独立 Checker，`PROGRESS_REBASE_INDEPENDENT_CHECKER_PENDING / DOCUMENTATION_ONLY / EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-REFACTORING-PROGRESS-REBASE-002 / plan-r2.256` 独立 Checker PASS 状态回写（2026-08-25）：首轮 Checker=`NOT PASS / 0 P0 / 1 P1 / 0 P2`，指出产品与系分的 MIG-03 旧 Entry Card 仍以现在时形成第二下一入口。Maker 只把两处及同段落历史语气校正为“当时”，不改变设计或完成证据；最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。确认 MIG-02B/03/05 完成态、MIG-02C/06/08/09 blocker、无 MIG-05E、五文档白名单和“当前无可编码 Provider slice”结论成立。当时只进入 `NEXT_SLICE_EVIDENCE_REQUIRED / EXECUTION_GRANT_NO / CODE_FREEZE`，现已由 r2.257 承接。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CUTOVER-ENTRY-CARD-001 / plan-r2.257` Maker 记录（2026-08-25）：Human Owner 在 A 方案价值确认后回复“授权推进，做价值分析”，只授权 wind-funds 五文档形成真实 Consumer 交接卡。源码证据确认 Capte Benefit settle 保存返回 transactionSn、refund 以主交易查询和业务查询存在判断完成；既有 ActionFact 查询已足以表达 primary/recovery 完成事实。卡片保留 `benefitTransactionSn` 执行引用，要求 Consumer 以唯一 succeeded/proven-full ActionFact、Money、intentRef 和 original fact ref 完成判定；不新增 Provider API、POM、schema 或兼容层。未来 Capte RED=`3 existing tests`、Green=`1 production Consumer`，并明确 dirty integration test 只可 preserve。当前进入独立 Checker，`ENTRY_CARD_INDEPENDENT_CHECKER_PENDING / CAPTE_CONSUMER_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CUTOVER-ENTRY-CARD-001 / plan-r2.257` 独立 Checker PASS 状态回写（2026-08-25）：初审指出 ActionFact empty 被误写成重试授权、refund returned transactionSn 未绑定 recovery `intentRef`、unit test dirty 未保护、已有 `benefitTransactionSn` fast-path 可绕过验证，以及 r2.256 历史段仍像第二当前入口。Maker 只在五文档内返工：恢复授权归 Capte 已成立且有效的业务意图/冻结请求，empty 仅为 UNKNOWN；补 refund intentRef、existing-reference RED；保护两份 dirty 测试；历史化 r2.256。最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`，确认现有 Public Contract 足够、`3 RED tests + 1 Green production` 闭包最小、无需 Provider/POM/schema/兼容层。当前只进入 Capte Human Owner RED Grant 决策，`CAPTE_CONSUMER_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-RED-CLOSEOUT-AND-GREEN-REFREEZE-001 / plan-r2.258` Maker 记录（2026-08-25）：Human Owner 回复“按你的建议推进”，只授权上一轮建议的文档状态对齐与 Green Entry Card 重冻。Maker 回读 Capte fresh XML、三个 RED 文件、唯一 Green 生产文件和两仓 live manifest，将 RED `88/10F/0E/0S`、原 78 项通过、integration main/phase、RED Checker PASS、Maven lifecycle/XML 边界和文件 SHA 写回五份 wind-funds 权威文档；Capte 现有优惠券系分只同步稳定 ActionFact 完成证据语义，不写任务状态。当时进入独立重冻 Checker，`GREEN_ENTRY_CARD_REFREEZE_INDEPENDENT_CHECKER_PENDING / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；该状态现由下条 Checker PASS 接替，本轮未修改或运行代码/测试。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-RED-CLOSEOUT-AND-GREEN-REFREEZE-001 / plan-r2.258` 独立 Checker PASS 状态回写（2026-08-25）：Checker=`PASS / 0 P0 / 0 P1 / 0 P2`，确认 RED 计数/失败映射/XML SHA、唯一 Green production 路径与写前 SHA、三个 immutable test SHA、Capte 稳定完成证据语义、六文件范围、两仓 manifest/staged/diff-check 和五项结构检查均成立；`plan-r2.257` 仅为历史，未形成第二活动入口。integration retained XML 只保留 recover，且本卡未把它外推为 Provider source lineage、L4 或生产；MySQL、独立 PMD 环境、L4 和生产均不是本卡门禁。当前只进入 Human Owner `CAPTE_BENEFIT_ACTIONFACT_CONSUMER_GREEN_EXECUTION_GRANT` 决策，`GREEN_ENTRY_CARD_REFREEZE_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`CAPTE_BENEFIT_ACTIONFACT_CONSUMER_GREEN_EXECUTION / plan-r2.259` Green 执行与独立 Checker PASS 记录（2026-08-25）：Human Owner 回复“授权推进，并做价值分析”，授权 plan-r2.258 唯一 Capte production Green。Maker 只修改 `CouponBenefitFundingSettlement.java`，以既有 ActionFact 校验 settle/reference/original primary/existing recovery/refund returned intentRef，移除本组件旧主交易完成判定；中间两次 1F 只补既有异常诊断片段，测试 immutable。final compile=`42/42`、focused=`88/0F/0E/0S`、main=`25/0F/0E/3S`、lineage/seed/recover 各=`1/0F/0E/0S`，Checker 校正事实后最终=`PASS / 0 P0 / 0 P1 / 0 P2`。该 PASS 只证明当前 Consumer Green，不外推 E4、MIG-09、Git、发布或生产。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-GREEN-CLOSEOUT-001 / plan-r2.259` Maker 记录（2026-08-25）：Human Owner 再次回复“授权推进，并做价值分析”，只授权上一轮 Checker 要求的 documentation-only Green closeout/state alignment。Maker 只更新 wind-funds 五份权威文档，记录 Green 产物、final XML/compile、两次诊断文本修正、Checker PASS、两仓 manifest、质量残余和价值边界；Capte 系分、生产、测试、POM/schema 与 Provider 均不再修改。当时进入独立 closeout Checker，`GREEN_CLOSEOUT_INDEPENDENT_CHECKER_PENDING / E4_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；该状态现由下条 Checker PASS 接替。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-GREEN-CLOSEOUT-001 / plan-r2.259` 独立 Checker PASS 状态回写（2026-08-25）：Checker=`PASS / 0 P0 / 0 P1 / 0 P2`，确认 Green source/test/POM/schema SHA、final compile/XML、两次诊断片段修正、完成层级、五文档白名单、两仓 manifest、结构与 diff-check 均准确；`plan-r2.258` 仅为历史。现有 artifact path/real Bean/H2/restart 只是 E4 候选证据，Provider source revision -> published artifact lineage 仍缺失，因此未冒充 E4、L4、MIG-09、发布或生产。该 closeout 当时只进入 Human Owner `MIG08_ACTIONFACT_CONSUMER_E4_ASSESSMENT_ENTRY_CARD_GRANT` 决策，现由 `plan-r2.260` 承接。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-E4-ASSESSMENT-ENTRY-CARD-001 / plan-r2.260` Maker 记录（2026-08-25）：Human Owner 回复“授权推进，并做价值分析”，只授权 documentation-only E4 assessment。只读对账保留 Consumer resolved/loaded、real Bean、联合 H2、事务和 seed/recover 证据；已安装 transaction face/impl JAR SHA=`8df1ced2... / 813da0f1...`，metadata=`localCopy=true / 20260824040045`，manifest 无 source revision。四个 ActionFact face source/class 在评估时与已安装制品相等，但 impl source/class 不等，installed impl source 也不命中任何 Git revision。评估时 target face/impl JAR=`915c7651... / 614d7df0...`；并发 clean、提交与重建后 target 又变为 `6ec25ad9... / a1335052...`，两组均不等于 Consumer 制品，只作过程观察。并发流程将 Provider source 固化为 HEAD=`0ed7bbdb4664431ab630c46ef9f76e5899484cc7`，最终 live manifest=`5 / 3f6d169fa4d1ed4b0fb55be889f9ed9251249d45c6d22468954882cb9b50302b`；Capte HEAD=`ce3c69467745b181f128561a887519bcba2950c7`、manifest=`38 / 0b9a02c1e9a5284651824c3e68d0bcc52dd765803aab670112d98ea58ee159b1`，两仓 staged empty、diff-check PASS。故 `E4_BLOCKED_LINEAGE`，不是 DSL/API/业务 Green 缺口；当前已从“source 未固化”收敛为“稳定 source 尚未形成 Consumer 实际解析/加载的唯一制品”。下一卡只允许唯一非 Snapshot 版本、Java 21 offline full-reactor build、七制品 built/resolved/loaded SHA 对账和既有 lineage/main/seed/recover fresh 验证；不授权 build/install、代码/测试/POM/schema、MySQL/PMD、MIG-09、Git、L4、发布或生产。当前进入独立 Entry Card Checker，`E4_ASSESSMENT_ENTRY_CARD_INDEPENDENT_CHECKER_PENDING / E4_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-E4-ASSESSMENT-ENTRY-CARD-001 / plan-r2.260` 独立 Checker PASS 状态回写（2026-08-25）：首轮 Checker 因 r2.259 旧段落现在时和产品旧 W4-02 E4 状态并列判 `NOT PASS / P1=1`；历史化旧入口并区分旧唯一制品范围后，Checker 又随并发 clean 发现 target JAR 已从 live checkout 消失。Maker 将 target SHA 降为 assessment-time observation；随后并发提交 `0ed7bbd...` 与重建再次产生不同 target SHA，最终只以新 HEAD、五文档 manifest 和 installed lineage mismatch 为承重事实。独立复核最终=`PASS / 0 P0 / 0 P1 / 0 P2`；确认 installed Snapshot 无 revision、impl source/class 无可识别 Git revision、Host 保留证据、七制品离线下一卡、五文档单一活动入口与排除项准确。当前只进入 Human Owner `MIG08_ACTIONFACT_CONSUMER_E4_LINEAGE_EXECUTION_GRANT` 决策，`E4_ASSESSMENT_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / E4_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-E4-LINEAGE-EXECUTION-001 / plan-r2.261` Human Grant 与执行中记录（2026-08-25）：Human Owner 回复“授权推进，并做价值分析确认”，授权 r2.260 唯一 E4 lineage execution。四份产品/DSL/系分/TDD 已收敛为稳定设计基线，运行态只由本 OpenSpec 持有。执行中 r1 因并发币种强类型改造编译失败、r2 因未启用 flatten 导致 installed POM 保留 `${revision}`、r3 因完整 fingerprint 漂移、r4 因 fsmonitor 漏报而捕获并发日志改造中间态，均标记 INVALID 且不复用；r5 已完成 Provider `21/21`、Capte `45/45`、七制品 built/resolved 相等与 lineage/assembly=`1/0F/0E/0S`，但其后文档单一事实源改造改变完整 fingerprint，不得原地续跑 main/seed/recover。当前先把 Grant 与单一运行态写回，再以禁用 fsmonitor 的稳定 fingerprint 构建 r6；尚不宣称 E4 PASS。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-E4-LINEAGE-EXECUTION-001 / plan-r2.262` E4 NOT PASS 与 source-restore blocker（2026-08-25）：最终 Provider r9=`1.0.1-e4-mig08-20260825-615c639e-d8786110-r9`，source HEAD=`615c639eecf0a848db271b8b70f233822e63c7d5`、combined fingerprint=`d87861100ed9d2bef4816c6e19d2900434e8795d6f2397ad8366d5d31f06cf3e`，offline flattened reactor=`21/21`，七制品 built/resolved SHA 逐项相等。为隔离 Capte 活动仓 staged analytics 改动，Maker 使用 clean commit `cdc5818734bbf05fdf827c02ef3cbc191ee6987b` 的 git archive；compile=`45/45`、lineage=`1/0F/0E/0S`、main=`25/0F/0E/3S`、seed/recover 各=`1/0F/0E/0S`。独立 Checker 发现该 commit 的 `CouponBenefitFundingSettlement.java` SHA=`c32e6f1d...`，仍是 plan-r2.258 Green 写前版本且没有 ActionFact 查询；integration SHA=`0c863e2e...` 仍是 pre-RED。当前 active Capte 生产文件同为 `c32e6f1d...`，仅 unit/boundary acceptance tests `d7827717... / f3558962...` staged，已验 Green `8b28a049...` 与 integration `6b1cd39a...` 不存在于任何可识别 commit/source card。因此 r9 技术证据真实但验证了旧 transaction-presence Consumer，Checker=`NOT PASS / P0=0 / P1=1 / P2=0`，不得记为 MIG08 E4 PASS。Provider r9 immutable build 卡可在 Consumer source 恢复后复用；下一步必须先形成并授权 `MIG08_ACTIONFACT_CONSUMER_GREEN_SOURCE_RESTORE_ENTRY_CARD`，不得在本 Grant 下改代码或测试。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-GREEN-SOURCE-RESTORE-ENTRY-CARD-001 / plan-r2.263` Maker 记录（2026-08-25）：Human Owner 回复“授权推进，并做价值分析确认”，只授权 documentation-only source-restore Entry Card。Maker 从原执行 JSONL 的 4 个 production patch 和 5 个 integration patch，在临时目录对当前写前基线做确定性重放，结果精确命中 `8b28a049... / 6b1cd39a...`；再加入当前 staged `d7827717... / f3558962...` 形成四文件恢复包 `e2676660...`。本卡只冻结两文件未来写入、两 staged 文件 index/worktree 保护、POM/schema 与 untracked 文档排除、r9 复用和 source-restore/E4 两阶段验证；未修改 Capte 或运行测试。当前进入独立 Entry Card Checker，`SOURCE_RESTORE_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-GREEN-SOURCE-RESTORE-ENTRY-CARD-001 / plan-r2.263` 独立 Checker PASS 状态回写（2026-08-25）：Checker=`PASS / P0=0 / P1=0 / P2=0`，独立复算 raw JSONL 九个 patch、恢复包/manifest、四目标 SHA、live base、两 staged tests index/worktree、manifest/POM/schema、两文件白名单、zero-call、future 计数和 source-restore/E4 分层均准确；Provider r9 可复用。本 PASS 只准出 Entry Card，不授权源码恢复，也不改变 E4 NOT PASS。当前只进入 Human Owner `MIG08_ACTIONFACT_CONSUMER_GREEN_SOURCE_RESTORE_EXECUTION_GRANT` 决策。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-GREEN-SOURCE-RESTORE-EXECUTION-001 / plan-r2.264` Human Grant 与执行中记录（2026-08-25）：Human Owner 回复“授权推进，并做价值分析确认”，授权 plan-r2.263 唯一两文件 source restore。执行前 Capte=`8172deb18f3a60c80ea814226c4e256d337b5146 / 3 / 498b7c2602ece454d9ecbaaab40d9dabc705c845f00c4cd96ba12a55163c8a98 / staged 2`，production/integration base=`c32e6f1d... / 0c863e2e...`，两 staged index/worktree=`d7827717... / f3558962...`，恢复包=`e2676660...`，diff-check PASS。当前只允许按原九 patch 恢复到 `8b28a049... / 6b1cd39a...` 并执行卡内 offline 验证；尚不宣称恢复完成或 E4 PASS。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-GREEN-SOURCE-RESTORE-EXECUTION-001 / plan-r2.265` 独立 Checker PASS 状态回写（2026-08-25）：Maker 只按恢复包修改 production 与 integration 两文件，最终四文件 SHA=`8b28a049... / d7827717... / f3558962... / 6b1cd39a...`；两 unit/boundary tests 的 staged index/worktree、无关 untracked 原型、POM/schema 与其他文件均未改。Java 21 offline pre-compile 与 post `clean test-compile` 均=`45/45`；fresh application=`57/0F/0E/0S`、boundary=`31/0F/0E/0S`、focused=`88/0F/0E/0S`、main=`25/0F/0E/3S`，XML SHA=`f30acb7b... / 9cf1f1f8... / 5f951c7a...`，日志 SHA=`290fc65c... / b3e0bd82... / 545a6843... / 53bd6432...`。production 仅调用 `queryFundsActionFacts`，旧查询、fallback、dual-read、compatibility zero-call。独立 Checker=`PASS / P0=0 / P1=0 / P2=0`；本 PASS 只关闭 Consumer Green source restore，不冒充 lineage/seed/recover 或 E4。当前只进入 Human Owner `MIG08_ACTIONFACT_CONSUMER_E4_RESUME_GRANT` 决策，Provider r9 可复用。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-E4-RESUME-001 / plan-r2.266` Human Grant 与执行中记录（2026-08-25）：Human Owner 回复“授权推进，并做价值分析确认”，授权复用 immutable Provider r9。执行前 wind-funds 仅 OpenSpec dirty；Capte=`8172deb18f3a60c80ea814226c4e256d337b5146 / 5 / f9d35ace0e8b0d77bbcc2bc3adfec554986931b047a5d86f1b4f18840f6f289b / staged 2`，四文件=`8b28a049... / d7827717... / f3558962... / 6b1cd39a...`，diff-check PASS。当前只允许以 HEAD archive 加四文件 overlay 构造不可变 Consumer source card，显式解析 r9 并执行 dependency/lineage/main/seed/recover；尚不宣称 E4 PASS。

`W5-MIG08-CAPTE-BENEFIT-ACTIONFACT-CONSUMER-E4-RESUME-001 / plan-r2.267` 独立 Checker PASS 状态回写（2026-08-25）：Consumer source carrier=`HEAD 8172deb1 archive defc0cec... + Green overlay e2676660...`，四文件=`8b28a049... / d7827717... / f3558962... / 6b1cd39a...`，POM/schema 未漂移；Provider r9 七制品与 immutable source/build 卡继续复用。dependency list=`7213c4f6...`，lineage=`1/0F/0E/0S`、main=`25/0F/0E/3S`、seed/recover 各=`1/0F/0E/0S`，XML SHA=`7ac103c6... / 5902e76e... / 183d57b7... / 3a2971d4...`，日志 SHA=`e5dbc56d... / d7992504... / f92a4003... / 405ce56b...`。两个 Maven/JVM/Spring 进程复用同一 file-H2，schema init=`always/never`；recover 证明 ActionFact/intentRef/Money/provenMoney、原 fact identity/allocation 和 transaction/detail/ledger/posting/entry/refund 零新增。活动 Capte 后续只出现无关产品文档漂移，测试始终运行于不可变 source card。证据卡=`/private/tmp/capte-actionfact-e4-resume.HN1bho/E4-EVIDENCE.md`、SHA=`ebb35e43144dfea06fc78d363ec4f21083616e0b57bcfa1e1f9951271941c29b`。独立 Checker=`PASS / P0=0 / P1=0 / P2=0`，准出范围仅为指定 Capte library test host E4；L4、真实部署数据库、MIG-09、外部 finality、release/deploy/production 均未授权。下一步只进入 Human Owner `MIG08_ACTIONFACT_CONSUMER_R8B_ZERO_CALL_ENTRY_CARD_GRANT` 决策。

### 12.1 `W5-MIG08-CAPTE-BENEFIT-FACADE-R8B-ZERO-CALL-ENTRY-CARD-001`

**授权、Owner 决策与实际价值**：Human Owner 回复“授权推进，并做价值分析确认”，只授权 `MIG08_ACTIONFACT_CONSUMER_R8B_ZERO_CALL_ENTRY_CARD_GRANT`。同时继承 Human Owner 已明确确认的环境事实：`capte-domain` 是公共通用模块，没有独立生产数据库或部署进程，真实 MySQL host 与独立 PMD 环境不作为该公共模块准出门槛；`plan-r2.267` 已用唯一 Provider r9、真实 Bean、联合 schema/事务和两 JVM file-H2 restart 关闭指定 library test host E4。因此 `D-R8B-001=ACCEPTED`：对本 Benefit Consumer，library test host E4 足以进入 facade retirement 文档/RED/Green；未来真实可部署宿主的 L4 仍独立等待，不能反向阻塞通用模块，也不能被本结论冒充关闭。

本卡不新增资金能力；它把已经接受的“Benefit adapter 归 Capte、Provider 只保留 generic direct action + ActionFact”落实成可执行的无兼容破坏式边界。`FundsBenefitContributionTransactionServiceImpl` 不直接写资金事实，而是以 `FundsDirectTransactionService.pay/refund` 执行动作；但它还承担 tenant、资金性质、账户/账目、Money、稳定 context 与 context 旁路拒绝，不能定性为纯透传。删除价值仍成立：这些单场景准入与业务引用应迁回唯一 Consumer adapter 或变成结构性不可能，Funds 只保留跨场景稳定的 Money、账户/责任、route、幂等、Ledger/Balance 与 ActionFact。删除可移除一个单场景 Public facade、两个 request、一个 core 场景 enum 和一个浅实现，不需要兼容层、第二服务或新 API；前提是下述不变量逐项有 Owner 和测试。

**当前调用闭包与裁决**：`fincone` 与 `fincone-issuing` Java 源码对目标 Benefit facade/request 零引用；唯一 production Consumer 是 Capte。Capte 的 `CouponBenefitFundingSettlement` 已对 `queryFundsTransaction/findFundsTransactionByBusiness` 完成判断零调用并只用 `queryFundsActionFacts`，但它仍直接调用 Benefit facade；`CouponActivityServiceImpl` 仍以该 facade 做启用前 readiness。四个 Capte 测试 Consumer仍编译或反射目标 service/request；wind-funds 仍有 facade、impl、request、enum、正向 contract/flow tests、Spring test import、Justfile 与 core API baseline。非 Java XML/JSON/YAML/properties 未发现目标类型或旧查询序列化入口。由此当前只满足“Benefit 完成判断切换”，尚未满足“Benefit facade/serialized surface 清零”。

首轮 Checker 进一步证明权威口径也未清零：wind-funds 四份产品、两个 DSL、三份系分、TDD、接入指南和另一 active OpenSpec，Capte 一份系分，Fincone 两份生产交付文档仍以现在时正向定义旧 facade 或“可部署 Consumer E4”门；它们不是代码 caller，但会继续指导新接入，因此属于 authority closure。`CouponImplContractBoundaryTests` 还显式要求旧 service 名。只改 Capte 一份系分不足以进入 RED，必须先按下表一次性对齐全部当前权威；归档预研与历史记录只保留 provenance，不做机械改写。

`WalletPaymentParticipant` 对 `queryFundsTransaction/queryFundsTransactionDetails` 的 production 调用，以及 wind-funds Transaction/Reconciliation 对宽查询的内部调用，具有不同授权/退款/清结算语义，明确排除于本卡；它们证明不能在本切片删除或整体收窄 `FundsTransactionQueryService`、`FundsTransactionDTO` 或 `FundsTransactionDetailDTO`。全局 `MIG-09` 继续 `BLOCKED_BY_MIG08`，本卡只处理 Benefit 场景 facade，不把单一 Consumer 清零冒充全仓 legacy retirement。

**Authority 文档先行前置**：Entry Card Checker PASS 后的下一 Human Gate 只能是 `MIG08_BENEFIT_FACADE_AUTHORITY_DOC_ALIGNMENT_GRANT`。未来写入为主 OpenSpec 机械状态加下列 `15` 份语义文档；不得修改代码、测试、schema/POM 或新增设计文件。

| 仓库 | 操作 | 精确路径 | 对齐责任 |
| --- | --- | --- | --- |
| wind-funds | `MODIFY` | `docs/产品设计/01-PRD总览.md` | Benefit 从 Provider 产品能力改为上游已决策责任映射到 generic pay/refund，完成只认 ActionFact。 |
| wind-funds | `MODIFY` | `docs/产品设计/02-交易路由钱包账目与投影.md` | 删除旧场景 facade 当前态，保留 Money、原事实、route、Ledger/Balance 与 UNKNOWN 边界。 |
| wind-funds | `MODIFY` | `docs/产品设计/05-产品验收与TDD用例矩阵.md` | 把旧 service/flow 验收映射到 generic direct + Capte Consumer/E4，不删除资金不变量。 |
| wind-funds | `MODIFY` | `docs/产品设计/支付资金公共能力层-产品设计.md` | 固化 `D-R8B-001`，把“可部署 Consumer E4”校正为本通用模块 library test host E4 已满足、L4 独立等待。 |
| wind-funds | `MODIFY` | `docs/DSL设计/支付资金底座DSL承载层设计.md` | 删除旧 Java facade/request 的当前承载描述；不改变既有 action/Money/original-ref DSL。 |
| wind-funds | `MODIFY` | `docs/DSL设计/支付资金公共能力层-DSL设计.md` | 将 MIG-08 的“可部署 Consumer E4”门校准为 `D-R8B-001`；保留真实部署宿主 L4 独立阻断，不新增 DSL。 |
| wind-funds | `MODIFY` | `docs/系分设计/01-系分设计总览.md` | 更新 Public surface/Owner 总览，Benefit adapter 归 Consumer。 |
| wind-funds | `MODIFY` | `docs/系分设计/02-交易路由钱包账目与投影系分设计.md` | 将旧 facade 物理入口替换为 Consumer adapter -> direct command + ActionFact。 |
| wind-funds | `MODIFY` | `docs/系分设计/支付资金公共能力层-系分设计.md` | 更新处置清册与 MIG-08 门槛；保留 Wallet/Reconciliation 宽查询排除和全局 MIG-09 blocker。 |
| wind-funds | `MODIFY` | `docs/TDD设计/支付资金底座测试驱动设计.md` | 以本卡不变量/13-test 矩阵替换旧 facade 为当前承重资产的表述，冻结 doc -> RED -> Green 计数。 |
| wind-funds | `MODIFY` | `docs/用户接入指南/README.md` | 删除旧 facade 接入入口与命令，改为业务 adapter 组装 generic direct pay/refund 并查询 ActionFact。 |
| wind-funds | `MODIFY` | `openspec/changes/funds-settlement-lifecycle-closure/spec.md` | 保留“commission/rebate 不映射为 Benefit”结论，但不再依赖已退役类型；该 active Goal 不形成第二执行入口。 |
| capte-domain | `MODIFY` | `docs/系分设计/通用优惠券-系分设计.md` | 把旧 facade、宽查询完成判断和 fundingNature 上送改为 Capte adapter -> direct pay/refund、ActionFact 与 Capte 自持 fundingNature。 |
| fincone | `MODIFY` | `docs/生产交付/系分设计/订单交易-系分设计.md` | 删除旧 facade 正向接入；只引用 generic direct action 与稳定事实，保持 Fincone 业务责任。 |
| fincone | `MODIFY` | `docs/生产交付/专项交付/资金内核-wind-funds集成指南.md` | 同步公共接入指南，禁止把场景 facade 当新 Consumer contract。 |

Authority alignment 总写入=`16 MODIFY`：上述 `15` 份语义文档加本 OpenSpec 机械 state/history。wind-funds 12 份语义文档当时均 clean；Capte 系分 clean；Fincone 集成指南 clean，`订单交易-系分设计.md` 已有 unrelated dirty，未来只能在当前内容上做目标 hunk，不得覆盖。当前 SHA 顺序为 wind-funds=`334376f7... / c9c50706... / 8bfb5c65... / 65834767... / fd0641bf... / 4d45a370... / 7bf9ff0c... / 271c2557... / 13d8243a... / cf230237... / 95d67467... / dd92f5f0...`，Capte=`8576f7d6...`，Fincone=`3f4d2959... / 29d8fa7b...`。每仓必须各自双读 status、保持目标 hunk 和 `git diff --check`；对齐并独立 Checker PASS 前不得进入 RED。

**RED 白名单**：Consumer 文档对齐后，未来只有取得 `MIG08_ACTIONFACT_CONSUMER_R8B_ZERO_CALL_RED_EXECUTION_GRANT`，才允许先修改以下两个既有测试；生产、其余测试、POM/schema 与 Provider 制品 immutable。

| 仓库 | 操作 | 精确路径 | RED 责任 |
| --- | --- | --- | --- |
| wind-funds | `MODIFY` | `tests/src/test/java/com/wind/funds/transaction/application/FundsBenefitContributionTransactionServiceContractTests.java` | 把三个旧正向反射合同收成一个 absence contract：generic `FundsDirectTransactionService` 仍存在，而 Benefit service、两 request、场景 enum 与 impl 必须不存在；缺失应为 assertion failure，不得成为 class-loading error。 |
| capte-domain | `MODIFY` | `tests/src/test/java/com/capte/marketing/coupon/CouponImplContractBoundaryTests.java` | 将 delivery-doc required terms 从旧 facade 改为 direct command + ActionFact，并增加一个聚合 source boundary：两个 production Consumer 必须使用 `FundsDirectTransactionService` 与 ActionFact，不得引用 Benefit service/request，不得恢复 `queryFundsTransaction/findFundsTransactionByBusiness` 完成判断。 |

基于已接受 fresh 基线，RED 预期分别为 wind-funds contract=`1/1F/0E/0S`、Capte boundary=`32/1F/0E/0S`，合计两个目标 failure class；其余既有测试必须通过。两测试在 RED Checker PASS 后对 Green immutable；任何额外 failure/error、测试放宽、兼容探针或生产改动立即停止。

**Facade 不变量归属矩阵**：删除不是丢弃 guard，而是只迁移真实 Consumer 需要的规则；没有 Consumer 的场景扩展面直接退役。

| 当前 facade 责任 | 目标归属与实现边界 | 必须保留的验证 |
| --- | --- | --- |
| `request.tenantId == TenantContextHolder.requireTenantId()` | 迁到 `CouponBenefitFundingSettlement` 的 settle/refund 信任边界；generic request 不再重复 tenant 字段。 | 复用/改写 `OrderCouponRedemptionIntegrationTests#testRealProviderSeparatesWrapperAndCapabilityFailureFacts`，证明错 tenant 在任何 Funds/优惠券副作用前拒绝。 |
| Money 非空且正数 | Capte 只对正 contribution/refund 调用；`TransactionAmount.sameCurrency` 与 generic direct command 继续校验 Money/币种。 | Capte split/return amount tests + `FundsDirectTransactionFlowTests` 的 Money/币种/零副作用回归。 |
| funding nature 只允许 platform/merchant/partner | Capte `CouponFundingNature` 只有这三个值，结构性不可能传入 `NO_FUNDS_TRANSFER/PREPAID/USER_BALANCE/UNKNOWN`；funding nature 不再进入 Funds request。 | `CouponFaceContractShapeTests` 继续只读负向禁止 Funds benefit enum 泄露；`testConfirmNoBenefitFundingSkipsFundsService` 与 `testNoneBenefitCompletesWithoutProviderFundsEffects` 证明无真实资金时不调用 Funds。 |
| receiver ledger 只允许 CLEARING/SETTLEMENT | 当前真实 Capte Consumer 固定商户 `FUNDING_MERCHANT/CLEARING`；没有真实 Consumer 的 SETTLEMENT 场景不作为 Provider 扩展面保留，未来出现时另建 Consumer 证据。 | `testConfirmRealBenefitFundingRejectsReceiverWithoutClearingBucket`、real-provider posting/balance 集成断言；generic `testPayWithoutPayeeLedger...` 保留底层拒绝。 |
| cost bearer/receiver 必须是可入账 Funding/Credit | `CouponFundingSubjectType` 只映射 Funding/Credit，receiver adapter 只返回 Funding；generic direct 继续拒绝 external、same-account 与无效账户。 | Capte account readiness/receiver tests + Direct external/same/missing-account flow tests。 |
| stable context 参与旧幂等摘要 | `benefitFunding* / originalOrderSn / referenceTransactionSn / refundReason` 是场景追踪而非跨场景资金事实；按 Human Owner“不考虑兼容”明确退役，不复制到 Funds context。新摘要只由 generic direct request 的账户、Money、business identity、原事实与 route 承重；原订单、funding nature、reason 留在 Capte 事实/日志。 | generic same-business same/different request replay/conflict；Capte existing-reference、restart seed/recover、ActionFact identity/digest 与零新增事实。新 Provider/Consumer 重新 E4，不复用旧 r9 数据摘要。 |
| 递归拒绝 context 携带核心 Benefit 字段 | Capte adapter 不接受调用方 context，也不向 generic request 写 context，结构性关闭该旁路；generic direct 自身敏感/核心字段 validator 继续独立存在。 | Capte source boundary 断言无 context mapping；Direct pay/refund sensitive-context 测试保留。 |
| settle/refund 业务日志 | Funds generic command/lifecycle 继续记录资金动作；Capte adapter 在 ActionFact 验证后记录最小业务引用、fundingId、transactionSn、Money，不输出完整 request/context。 | 代码 CR 与日志字段审查；日志不替代 ActionFact/测试。 |

**13 个 Provider facade flow tests 逐项处置**：只有验证专属 facade surface 的测试随 surface 删除；资金不变量继续由 generic direct 与 Capte Consumer/E4 承重。

| 旧测试 | 处置 | 目标证据 |
| --- | --- | --- |
| `testSettleAndRefundShouldPostThroughStandardTransactionLedgerChain` | 迁移到 Consumer/E4 | Capte `testTrustedGoodsOrderSpecifiedCouponPostsThroughRealProviderBean`、`...RefundsPersistedBenefitTransaction` 改名/改签后继续断言 route/posting/entry/balance/ActionFact；Direct referenced refund 回归。 |
| `testCapabilityRejectionShouldKeepFailedBenefitTransactionFacts` | 迁移到 Consumer + generic | Capte `testTrustedGoodsOrderKeepsProviderFailedFactsWithoutCouponOrLedgerEffects` 改为 direct-provider 语义；Direct pay failure facts。 |
| `testSettleWithUserBenefitBalanceShouldFailWithoutFundsOrLedgerFacts` | 结构性不可能 | 删除 Funds enum 后 Capte 三值 enum 无该输入；face negative token 防泄露。 |
| `testSettleWithoutSupportedReceiverLedgerSubjectShouldFailWithoutFundsOrLedgerFacts` | Consumer 固定 + generic | Capte receiver/CLEARING bucket 测试；Direct missing/invalid payee ledger 拒绝。 |
| `testSettleWithInvalidAccountTypesShouldFailWithoutFundsOrLedgerFacts` | Consumer 类型封闭 + generic | Capte Funding/Credit 映射与 receiver adapter；Direct external/same/missing account 拒绝。 |
| `testSettleWithMerchantBorneBenefitShouldPostFundingResponsibility` | 迁移到 Consumer | Capte per-funding-party settle、真实 provider posting 与 `MERCHANT_BORNE` 配置。 |
| `testSettleSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects` | generic 承重并随 Consumer复验 | `testDirectPaySameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects` + Capte real provider conflict/零副作用。 |
| `testSettleScenarioMatrixShouldRecordConcreteContributionPairs` | 只迁真实 Consumer 范围 | Capte 当前商户 CLEARING、多出资方与余额断言；没有 Consumer 的 user/order SETTLEMENT 场景明确退役，不保留通用 Benefit matrix。 |
| `testMultipleContributorsShouldSettleAndRefundByOriginalBenefitTransaction` | 迁移到 Consumer | Capte per-funding-party、primary remainder、refund by original ActionFact/reference tests。 |
| `testRefundWithMissingOriginalBenefitTransactionShouldFailWithoutFundsOrLedgerFacts` | generic + Consumer | Direct missing reference/route 零副作用；Capte `testReturnRealBenefitFundingFailsWhenOriginalContributionTransactionMissing`。 |
| `testSettleWithNoFundsTransferNatureShouldFailWithoutFundsOrLedgerFacts` | 结构性不可能 | Capte `NONE` 分支不调用 Funds；无对应 Funds enum/request。 |
| `testSettleContextShouldRejectCoreBenefitFactsWithoutFundsOrLedgerFacts` | 旧 Public context surface 退役 | Capte adapter 无 context 输入/输出；generic Direct sensitive-context 测试保留。 |
| `testRefundContextShouldRejectCoreBenefitFactsWithoutFundsOrLedgerFacts` | 旧 Public context surface 退役 | 同上；退款只传原 transaction ref、Money 与业务 identity。 |

**Green 精确白名单与必要性审计**：`ADD=0 / MODIFY=9 / DELETE=6`。每个路径都由当前直接 production/test/build/baseline 引用证明；没有 future-only 扩展。首次 14 文件 Green 后，删除 stable core enum 必然触发 API cardinality 治理，故最终可执行闭包为 `15`；完整唯一写路径为 `33`：主 OpenSpec `1` + authority 语义文档 `15` + RED 测试 `2` + Green `15`。阶段责任不重叠，主 OpenSpec 后续只机械持有状态。

| 仓库 | 操作 | 精确路径 | 唯一责任 |
| --- | --- | --- | --- |
| capte-domain | `MODIFY` | `marketing/coupon-impl/src/main/java/com/capte/marketing/coupon/integration/funds/CouponBenefitFundingSettlement.java` | 把 settle/refund 映射直接下沉到 Capte adapter，复用既有 `FundsDirectTransactionService.pay/refund`；显式校验 TenantContext，固定真实 Consumer 的账户/账目映射，不写 context，保留 ActionFact、Money、intentRef、original fact/allocation、业务引用与最小日志，不增加 fallback/dual-read/compatibility。 |
| capte-domain | `MODIFY` | `marketing/coupon-impl/src/main/java/com/capte/marketing/coupon/service/impl/CouponActivityServiceImpl.java` | readiness 从场景 facade 改为既有 generic direct service；账户、余额与 ActionFact readiness 不变。 |
| capte-domain | `MODIFY` | `tests/src/test/java/com/capte/marketing/coupon/CouponRedemptionApplicationServiceImplTests.java` | Recording 改为 direct command + ActionFact query，保留 per-funding-party、Money、原引用、恢复、冲突和无新 attempt 行为断言；旧宽查询方法声明仅因未拆分 query interface 保留，不得被调用。 |
| capte-domain | `MODIFY` | `tests/src/test/java/com/capte/marketing/coupon/CouponActivityServiceImplTests.java` | readiness fixture 改为 direct service provider，原活动发布与缺服务拒绝断言不变。 |
| capte-domain | `MODIFY` | `tests/src/test/java/com/capte/order/transaction/OrderCouponRedemptionIntegrationTests.java` | 删除旧 facade/request/impl 的装配、反射和直接调用，改为新 Consumer source 的 direct service 装配；在既有 25 个方法内承接 tenant mismatch、capability、same-key conflict、settle/refund/failure，不增加测试数；保持 `25/0F/0E/3S`、lineage、seed/recover 与零新增事实断言。 |
| wind-funds | `DELETE` | `transaction/face/src/main/java/com/wind/funds/transaction/application/FundsBenefitContributionTransactionService.java` | 删除单场景 Public facade。 |
| wind-funds | `DELETE` | `transaction/face/src/main/java/com/wind/funds/transaction/model/request/FundsBenefitContributionSettleRequest.java` | 删除单场景 settle request；不新增 V2/alias。 |
| wind-funds | `DELETE` | `transaction/face/src/main/java/com/wind/funds/transaction/model/request/FundsBenefitContributionRefundRequest.java` | 删除单场景 refund request；逆向继续复用既有 referenced direct refund。 |
| wind-funds | `DELETE` | `core/src/main/java/com/wind/funds/transaction/enums/FundsBenefitFundingNature.java` | 删除只被 Benefit request/impl/tests 使用的场景 enum，防止营销出资分类留在 core。 |
| wind-funds | `DELETE` | `transaction/impl/src/main/java/com/wind/funds/transaction/application/impl/FundsBenefitContributionTransactionServiceImpl.java` | 删除只做 request translation 的浅实现；不改 generic direct command 实现。 |
| wind-funds | `DELETE` | `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsBenefitContributionTransactionServiceFlowTests.java` | 按上方 13-test 矩阵删除已退役 facade 测试；只有结构性不可能/明确退役项不迁移，其余由既有 Direct 或 Capte Consumer/E4 精确承接。 |
| wind-funds | `MODIFY` | `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsTransactionFlowTestSupport.java` | 仅移除已删除 impl 的 import/测试 Spring 组件，不改共享资金 fixture。 |
| wind-funds | `MODIFY` | `Justfile` | 仅从 `test-business-flow` 删除已退役测试类名；其他命令与测试清单不变。 |
| wind-funds | `MODIFY` | `core/api-baseline/stable-api.txt` | 只删除 `FundsBenefitFundingNature` 的 11 行 stable API；不得刷新或顺带修复其他 baseline 漂移。 |
| wind-funds | `MODIFY` | `scripts/verify-core-api-baseline.sh` | 只把删除一个 stable enum 后的 cardinality 与消息从 `103/95` 机械校准为 `102/94`；扫描算法、4/4 policy、成员例外与 baseline 内容不变。 |

Green 不得修改两个 RED 测试、`FundsDirectTransactionService`、`FundsTransactionQueryService`、ActionFact DTO/query、generic pay/refund request、Transaction command/converter、Wallet/Reconciliation、POM、schema、Mapper、产品/DSL/系分/TDD、其他 Consumer 或当前无关 dirty 文件。发现新 positive caller、需要兼容层、旧数据 digest 兼容、第二 API、POM/schema 或名单外文件时停止并重冻；用户已明确不考虑兼容问题，因此不建立 alias、bridge、双 Bean、双读、双写或旧 request 反序列化。

**冻结基线**：wind-funds HEAD=`ed4eaeddbfa3eb529847f3d14b23cf2c360c2337`；10 个未来目标 SHA 依次为 `Justfile=510031df...`、core baseline=`e28e68f2...`、enum=`510fc157...`、service=`7a3af7d7...`、settle request=`5d7ae9e3...`、refund request=`efb45957...`、impl=`f60bdfca...`、contract test=`d6f23a11...`、flow test=`7c6d1c28...`、flow support=`a5f4f1fe...`，当时均 clean。Capte HEAD=`8172deb18f3a60c80ea814226c4e256d337b5146`；六代码/测试目标 SHA=`8b28a049... / f46294bc... / d7827717... / 4d3e7239... / f3558962... / 6b1cd39a...`，Consumer 系分 SHA=`8576f7d6...`；其中 production settlement 与 integration 为既有 unstaged，application/boundary tests 为既有 staged，另两代码/测试与 Consumer 系分 clean。未来文档/RED/Green 前必须双读 live 状态并精确匹配对应目标内容或由 Human Owner 重冻；不得覆盖 index，也不得吸收 wind-funds 九个 Converter 或 Capte 指标文档/任务文档的并发修改。

**未来验证与零调用定义**：Green 必须 fresh 证明 wind-funds compile=`21/21`、legacy absence contract=`1/0F/0E/0S`、Core API=`102 public / 94 stable / 4 experimental / 4 internal / 1025 lines`；Capte Java 21 offline compile=`45/45`、application=`57/0F/0E/0S`、activity=`37/0F/0E/0S`、boundary=`32/0F/0E/0S`、focused=`89/0F/0E/0S`、只读 `CouponFaceContractShapeTests=61/0F/0E/0S`、main=`25/0F/0E/3S`、lineage/seed/recover 各=`1/0F/0E/0S`。`test-business-flow` 只允许相对 fresh 写前基线减少已删除 facade flow 的 `13` 项且其余全绿；`test-transaction`、core/boundary 与依赖边界必须全绿。新 Provider 必须使用唯一非 Snapshot 版本和隔离本地仓重新证明 source -> binary -> resolved/loaded；r9 只能作旧 E4 provenance，不能复用为删除后制品。

零调用扫描覆盖 wind-funds、capte-domain、fincone 与 fincone-issuing 的当前权威文档、Java production/test、反射类名、Spring 装配、Justfile、API baseline 及 XML/JSON/YAML/properties；目标 service、两 request、enum、impl 不得有 positive reference。合法负向 allowlist 恰有三处：两个 Green immutable 的 legacy absence/source boundary，以及只读 `CouponFaceContractShapeTests` 对 `FundsBenefitFundingNature` 泄露的既有禁止 token；归档/历史 provenance 可保留但不得形成当前接入指令。Benefit production/integration 不得调用 `queryFundsTransaction/findFundsTransactionByBusiness` 判断完成；测试为履行尚未拆分的 `FundsTransactionQueryService` 而保留的方法声明不算 invocation，明确排除的 Wallet/Reconciliation caller 不计入本切片清零。任何遗漏 caller、unexpected serialization、E4 谱系/重启失败或非目标 failure 都阻断整切片，不能靠恢复 facade 回退。

**Checker 返工与当前状态**：首轮独立 Checker=`NOT PASS / P0=0 / P1=2 / P2=0`：一是 17 路径漏掉当前 authority closure 和第三个合法负向 token；二是把 facade 错写成纯翻译并直接删除 13 个 flow tests，未证明 guard/test ownership。首次复核=`NOT PASS / P0=0 / P1=1 / P2=0`，只指出 `docs/DSL设计/支付资金公共能力层-DSL设计.md` 的当前“可部署 Consumer E4”门仍遗漏；第二次复核只发现“一个 DSL/两个 DSL”机械 P2。Maker 只在主 OpenSpec 返工为 `D-R8B-001 + 32 unique write paths + 3 negative allowlist + facade invariant matrix + 13-test matrix`，没有修改其他文档、Java 或测试。最终独立 Checker=`PASS / P0=0 / P1=0 / P2=0`，确认 authority 15、总写入 16、RED 2、Green 14、计数/SHA/dirty、L4/MIG-09 排除均一致。当前=`R8B_ZERO_CALL_ENTRY_CARD_REWORK_INDEPENDENT_CHECKER_PASS / AUTHORITY_DOC_ALIGNMENT_REQUIRED / R8B_RED_EXECUTION_GRANT_NO / MIG09_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`；下一步只能由 Human Owner 决定 Authority 文档对齐，不直接授权代码或 RED。

`W5-MIG08-BENEFIT-FACADE-AUTHORITY-DOC-ALIGNMENT-001 / plan-r2.270` Maker 记录（2026-08-25）：Human Owner 回复“授权推进，并做价值分析确认”，只授权主 OpenSpec 与冻结 15 份语义文档。Maker 在 wind-funds 四份产品、两个 DSL、三份系分、TDD、接入指南和另一 active OpenSpec，Capte 通用优惠券系分，Fincone 订单交易系分与资金内核集成指南中统一当前口径：Benefit 资格、funding nature、多出资方分摊、适用商户、原订单、退款资格和恢复授权归 Consumer adapter；Funds 只保留 generic direct pay/refund、Money、账户责任、route、幂等、Ledger/Balance 与 ActionFact。`D-R8B-001` 明确 Capte 通用模块 library test host E4 已满足本 Consumer facade-retirement 前置，未来部署宿主 L4 独立等待。

首轮独立 Checker=`NOT PASS / P0=0 / P1=2 / P2=0`：产品分册与 Capte 系分仍保留旧 settle/request/SubjectRef/fundingNature 上送；Fincone 仍把 r2.267 已关闭的 current-source lineage/real Bean/H2/L3 写为缺失。Maker 只在原白名单修正这些当前态段落：统一为 `FundsAccountId + Money + business identity + original transaction ref -> generic pay/refund -> FundsActionFact`，fundingNature/context 留 Consumer；Fincone 同步 `LIBRARY_HOST_E4=PASS`，`SPECIFIED` Enable、DDL、Release、Production 和未来部署宿主 L4 继续阻断。

首次复核=`NOT PASS / P0=0 / P1=1 / P2=0`：Fincone 订单系分 5.4/AC 仍写旧 Benefit settle/refund，旧 L3 计划未历史化；集成指南结果表仍把执行引用称为 settle 返回。Maker 将当前链统一为 Consumer adapter -> generic pay/referenced refund -> ActionFact，把 5.4.2 明确标记为 r2.267 已关闭的历史 L3/E4 计划，并只保留新 facade-retirement 制品必须重跑 lineage；Enable、正式 DDL、Release、Production 与未来部署宿主 L4 继续阻断。

15 份语义文档 final rework post SHA 依次为 wind-funds=`1f3955ae... / 1a6cffd7... / 0135a8fa... / 7ae37a0b... / 6029f489... / 665963db... / d6eb746d... / ec5b0505... / 27b41da1... / 0818cf9d... / ff2d9e55... / f6da60ce...`，Capte=`93a6e4a0...`，Fincone=`2ef0d7c9... / aa63f2d6...`。Fincone `订单交易-系分设计.md` 的既有 isTest dirty hunk保持，Maker 只修改冻结 Benefit 当前态；其余三仓无目标外写入。当前接入/产品/系分零正向旧 facade/service/request 引用；旧 DSL/TDD/系分/OpenSpec 中明确标记的历史迁移证据与未来 absence test 名保留 provenance，不构成当前合同。

最终独立 Checker=`PASS / P0=0 / P1=0 / P2=0`：15 份文档已统一为 Consumer adapter -> generic pay/referenced refund -> ActionFact；Fincone 旧 L3 计划已历史化，r2.267 library-host E4 已同步；新 facade-retirement 制品仍须重跑 lineage。`SPECIFIED` Enable、正式 DDL、Release、Production 和未来部署宿主 L4 保持阻断，Fincone isTest dirty 完整保留。该 PASS 只关闭 Authority 文档对齐，下一步仍需独立 RED Grant。

`W5-MIG08-BENEFIT-FACADE-R8B-ZERO-CALL-RED-EXECUTION-001 / plan-r2.272` Maker 记录（2026-08-25）：Human Owner 回复“授权推进，并做价值分析确认”，只授权两个冻结测试。执行前 Java/Maven=`Corretto 21.0.11 / Maven 3.6.3`；wind-funds offline compile=`21/21`，Capte 首次 sandbox compile 因 target 写权限阻断，获准后同一 offline compile=`45/45`。生产目标 SHA 全部保持冻结值：Provider enum/service/two requests/impl=`510fc157... / 7a3af7d7... / 5d7ae9e3... / efb45957... / f60bdfca...`；Capte settlement/activity=`8b28a049... / f46294bc...`。Capte 两 staged tests 的 cached diff 未变化。

Maker 把 wind-funds 三个旧正向反射合同收成一个 absence contract：generic direct service 必须存在，Benefit service/two requests/enum/impl 必须不存在。Capte 只更新 Authority required terms 并新增一个聚合 source boundary，要求 settlement/activity Consumer 改用 generic direct pay/refund + ActionFact，且旧 facade/request/主交易完成判断为零。首次 Capte fresh=`32/3F/0E/0S`，其中两项只因 required term 使用文档不存在的精确字符串 `FundsActionFact`；在同一测试白名单改为现有 Authority 词汇 `ActionFact` 后重新 fresh，额外失败关闭。

最终 wind contract=`1/1F/0E/0S`，唯一 failure 列出仍存在的 service、two requests、enum 与 impl；test/XML SHA=`b6fe87057e8155caac78c29762d26c2c18f7acce90beb7aa493a21f29704a447 / 1a5c18e5a9ec3d965621150eab93efafb7fca7b19ce1533cc0e165e020d49011`。Capte boundary=`32/1F/0E/0S`，唯一 failure 聚合 7 个 Consumer source mismatch；test/XML SHA=`be80bce63d3d7f75d7d3064d34d1d0553f4485fbdec1a85bb799c5565398e0ed / 79a8c86cb0f804c7a0a3ae675cb2d4b61e50f5c247b0bc37aa12f9890f9c7b1e`。Capte Maven report lifecycle 会在 XML 有失败时返回成功，因此只以 fresh XML 计数和 failure class 裁决。两仓 `git diff --check` PASS；未修改生产、其他测试、POM/schema 或 Git。当前进入 `R8B_RED_INDEPENDENT_CHECKER_PENDING`，不授权 Green。

独立 RED Checker 最终=`PASS / P0=0 / P1=0 / P2=0`：确认 wind 单一 absence contract 的 5 个旧类型与 Capte 单一 source-boundary test 的 7 个 mismatch 均为目标 assertion failure；test/XML freshness 与 SHA、Provider/Consumer production SHA、Capte staged index 和范围边界全部一致。Authority 阶段与本 RED 执行事实已正确分层；当前下一 Human Gate 仅为 `MIG08_ACTIONFACT_CONSUMER_R8B_ZERO_CALL_GREEN_EXECUTION_GRANT`。

Authority alignment 阶段的验证为：四份产品文档 `check_product_deliverable --kind product-architecture` PASS；主公共能力系分、Capte 系分和 Fincone 订单系分 `check_architecture_deliverable --kind system-design` PASS；主 OpenSpec refactoring validator 与 Harness PASS；三仓 `git diff --check` PASS。旧总览/分册与 `funds-settlement-lifecycle-closure` 不采用完整系分/重构模板，通用 architecture validator 的既有 missing-section 不据此重写，也不冒充 PASS。该阶段未运行编译/测试；`plan-r2.272` 已完成 offline compile 与两个聚焦 RED/XML，并由 `plan-r2.273` 独立 Checker PASS 关闭，下一只等待 Human Owner Green Grant，生产、POM/schema 和 Git 未修改。

`W5-MIG08-BENEFIT-FACADE-R8B-ZERO-CALL-GREEN-EXECUTION-001 / plan-r2.274` 执行记录（2026-08-25）：Human Owner 授权冻结 14 文件 Green，不考虑兼容。Capte adapter 将真实出资映射为既有 `FundsDirectTransactionService.pay/refund`，保持 cost bearer -> `accountId`、receiver -> `payeeId`、`CLEARING`、same-currency Money、原成功 ActionFact/reference refund；`NONE` 在 funding 为空后直接返回，不引入 Funds/tenant 约束。Provider 删除 Benefit service、两 request、funding enum、浅 impl 与 13-test flow，并机械迁移 support、Justfile 和 stable baseline。两个 RED 测试保持 immutable，四仓旧 surface 零正向调用。

Green fresh 证据为 wind-funds clean compile=`21/21`、absence=`1/0F/0E/0S`、business=`200/0F/0E/0S`、transaction=`185/0F/0E/0S`、core=`106/0F/0E/0S`、boundary=`208/0F/0E/0S`；Capte compile=`45/45`、application=`57/0F/0E/0S`、activity=`37/0F/0E/0S`、boundary=`32/0F/0E/0S`、Face=`61/0F/0E/0S`、main=`25/0F/0E/3S`，唯一 E4 版本=`1.0.1-e4-mig08-r8b-20260825-r1`，lineage/seed/recover 各=`1/0F/0E/0S`，目标 JAR 旧类为 0。独立 Checker 首轮发现 Activity 旧错误文案、Core API hardcode 和 Face 旧计数三个 P1；Activity 在原 Green 白名单内机械修正并 fresh=`37/0` 后，最终=`NOT PASS / P0=0 / P1=2 / P2=0`。剩余两项均为治理权威缺口，不否定资金行为或 E4；不得恢复 enum 补数。

`W5-MIG08-BENEFIT-FACADE-R8B-GREEN-GOVERNANCE-REBASE-001 / plan-r2.275` Maker 记录（2026-08-26）：Human Owner 回复“授权推进，并做价值分析确认”，授权且仅授权 `scripts/verify-core-api-baseline.sh`、Core 长期规格、主 TDD 与本 OpenSpec。执行前双读发现共享工作区由 `60 / 2aeab9...` 漂移并稳定为 `69 / 03acce...`，其中脚本 `103/95 -> 102/94` 和 Face `63 -> 61` 已由并发内容提供；Maker 保留这些既有修改，不覆盖新增 `tests/pom.xml`、boundary/reconciliation 等非目标差异，只补齐四文件权威闭包。写后 `just verify-core-api` fresh clean compile=`21/21` 并输出 `94 stable / 4 experimental / 4 internal`，public=`102`、baseline=`1025 lines`；`bash -n` 与 `git diff --check` PASS。Capte Activity/Face XML 继续为 `37/0` 与 `61/0`，SHA=`8e734103... / 3d26f288...`；当前进入独立只读 Checker，不预写 PASS，不授权生产/测试、兼容、POM/schema、Git、MIG-09、L4 或发布。

`W5-MIG08-BENEFIT-FACADE-R8B-GREEN-GOVERNANCE-REBASE-001 / plan-r2.276` 独立 Checker PASS 状态回写（2026-08-26）：Checker=`PASS / P0=0 / P1=0 / P2=0`。复核确认脚本只机械更新 `103/95 -> 102/94` 与四处消息，Core `D-CS-006-U` 是唯一当前权威，`D-CS-006-S/T` 保留历史；TDD/OpenSpec 的 Activity=`37/0`、Face=`61/0`、Core API=`102/94/4/4 / 1025` 与 Green 15 文件闭包一致。Harness、refactoring validator、`bash -n`、direct API script 和 `git diff --check` PASS；live HEAD=`ed4eaed...`、status=`70 / 106b29f4...`、staged empty，69 项既有 dirty 未被覆盖。R8B Green 至此关闭；MIG-09、其他 Consumer、Wallet/Reconciliation 宽查询、部署宿主 L4、Git、enable/release/production均未授权。

`W5-MIG09-FROZEN-ORDER-PUBLIC-CRUD-RETIREMENT-ENTRY-CARD-001 / plan-r2.277` Maker 记录（2026-08-26）：Human Owner 回复“授权推进，并做价值分析确认”，只授权 MIG-09 documentation-only 盘点、Entry Card 与 Checker。四仓 Java 清册确认 FrozenOrder CRUD 六类型在 Consumer 零引用，Provider 也无生产 caller；真实冻结事实由 balance-control -> orchestrator -> internal lifecycle -> Entity/Mapper 承重。首切据此冻结为 RED `1 MODIFY`、Green `6 DELETE + 3 MODIFY / ADD=0`；第三个 MODIFY 只在接入指南机械替换两处验证锚点，不触碰 Entity/Mapper/schema/lifecycle/query、资金行为或兼容层。raw external event 因 Fincone 当前 authority 正向引用且 resolver 被支付工具复用而 blocked；Ledger 只允许未来 method-level card；整个 FundsTransactionQueryService 明确排除。

首轮独立 Checker=`NOT PASS / P0=0 / P1=1 / P2=0`：TDD 与接入指南仍把未来转为 absence contract 的 `FundsFrozenOrderServiceImplTests` 当作敏感 context 和余额行为证据。Maker 只在当前 TDD 把安全锚点改为既有 `SensitiveContextVariablesValidatorTests + FundsBalanceControlFailureFlowTests`，并把接入指南作为未来 Green 第三个机械 MODIFY；Justfile 不改，两个 RED/Green contract 方法和 `test-balance-control=44` 保持。CRUD 专属 core-benefit context 拒绝随无 Consumer CRUD 退役；若要证明真实 freeze 对同类 reserved key fail-closed，必须另建 hardening 卡，不在本删除卡冒充已有证据。

`W5-MIG09-FROZEN-ORDER-PUBLIC-CRUD-RETIREMENT-ENTRY-CARD-001 / plan-r2.278` 独立 Checker PASS 状态回写（2026-08-26）：Checker=`PASS / P0=0 / P1=0 / P2=0`，确认四仓 Consumer 零调用、内部冻结事实承重、候选排除、RED 单文件两个 failure、Green `6 DELETE + 3 MODIFY`、Public Contract `304/179/42 -> 300/176/42`、focused 54、balance-control 44、converter/README dirty overlap 与 immutable survivor 均成立。三个 validator、路径检查、Public Contract 当前清册和 `git diff --check` PASS；live HEAD=`ed4eaed...`、status=`102 / 02cb4c18...`、staged empty。该 PASS 只准出 Entry Card；下一唯一 Human gate 为 `MIG09_FROZEN_ORDER_PUBLIC_CRUD_RED_EXECUTION_GRANT`。

`W5-MIG09-FROZEN-ORDER-PUBLIC-CRUD-RETIREMENT-RED-EXECUTION-001 / plan-r2.279` 重冻 Maker 记录（2026-08-26）：Human Owner 回复“授权推进，并做价值分析确认”，只授权 documentation-only RED Entry Card re-freeze。首次 RED 仅修改 `FundsFrozenOrderServiceImplTests`，test SHA=`ac4b60ca...`；写前/后 compile=`21/21`，两次技术 RED 均=`2/2F/0E/0S`，固化 XML SHA=`f0349e81...`。但原 manifest `102/02cb4c18...` 在执行中变为 `107/436200c2...`；独立 Checker 因明确 stop condition 判整体 `NOT PASS / P1=1`，不允许 Green。

重冻将五项增量准确拆为一个授权 RED test 与四个非目标 governance replay 差异，不把授权目标误记为漂移；四项 current/diff SHA 已冻结且全部 read-only。六生产候选、converter dirty、immutable survivor、RED/Green 白名单、Public Contract/54/44 计数和无兼容边界不变。当前 test artifact 已存在，未来新 RED Grant 默认只 fresh 重跑当前 SHA 并重新取得 Checker；本轮未修改 Java/测试/Justfile/接入指南，未运行 Maven、Git、联网或发布。

`plan-r2.277` 原 Entry Card 当时只修改主系分、TDD 与本 OpenSpec。当时盘点期间共享工作区从 `70 / 106b29f4...` 并发漂移后双读稳定为 `102 / 02cb4c18...`，三文档目标哈希未漂移；converter 为唯一未来删除目标 dirty overlap，冻结当时 current/diff SHA，Entity/Mapper/lifecycle 的并发修改全部 immutable。当时 Public Contract fresh=`304/179/42`，未来目标=`300/176/42`，Core API保持`102/94/4/4 / 1025`；该阶段未运行 Maven 测试，未修改代码/测试/Justfile/接入指南，未执行 Git、联网、安装或发布。该 `102` 只作原 Entry Card 历史证据，已由上文 `107 / 436200c2...` re-freeze supersede，不再表示当前状态。

`W5-MIG09-FROZEN-ORDER-PUBLIC-CRUD-RED-ENTRY-CARD-REFREEZE-001 / plan-r2.280` 独立 Checker PASS 状态回写（2026-08-26）：Checker=`PASS / P0=0 / P1=0 / P2=0`，确认 `107/436200c2...` 是唯一当前 manifest，`102` 只作原卡历史；五项增量准确分为一个授权 RED test 与四个非目标 governance replay 差异。四项 current/diff SHA、test/XML、六生产候选、converter dirty、immutable survivor 与 Green `6 DELETE + 3 MODIFY` 均保持；诊断 RED 未冒充 RED PASS。下一唯一 Human gate 为当前 test SHA=`ac4b60ca...` 的 fresh-rerun RED Grant。

`W5-MIG09-FROZEN-ORDER-PUBLIC-CRUD-RED-EXECUTION-001 / plan-r2.281` fresh-rerun 与独立 Checker PASS（2026-08-26）：Human Owner 授权后只在 immutable test SHA=`ac4b60ca...` 上 fresh 重跑，没有再次修改测试；结果=`2/2F/0E/0S`，failure class 仅列六个待退役类型，固化 XML SHA=`51647480...`。独立 RED Checker=`PASS / P0=0 / P1=0 / P2=0`，只准出冻结 Green 文件卡，不扩大其他 MIG-09。

`W5-MIG09-FROZEN-ORDER-PUBLIC-CRUD-GREEN-EXECUTION-001 / plan-r2.282` 技术执行记录（2026-08-26）：Human Owner 授权后只执行冻结的 `6 DELETE + 3 MODIFY / ADD=0`，删除无 Consumer Public CRUD 与平行 impl/converter，机械调整两个测试清册和接入指南两处锚点；RED test、Entity/Mapper/schema/lifecycle、Balance/Ledger/query 和其他 MIG-09 均 immutable，不提供兼容。Core API clean compile=`21/21`、`102/94/4/4 / 1025`，Public Contract=`300/176/42`，absence=`2/0`，focused=`54/0`，balance-control=`44/0`，transaction=`185/0`，business-flow=`200/0`，boundary=`212/0`；45 份 XML 全绿，四仓正向调用与 clean classpath 旧类型为 0。独立 Checker 对技术 Green=`PASS / P0=0 / P1=0 / P2=0`。

技术 Checker 后出现一个新 untracked `openspec/changes/funds-state-naming-normalization/spec.md`，使其复核的 `114/bfa74000...` 失效；增量 Checker 因 manifest stop condition 判整体 `NOT PASS / P0=0 / P1=1 / P2=0`，但确认技术 Green、XML、RED 和 survivor SHA 仍有效。该文件明确把 MIG-09 Green 列为 Out Of Scope。

`W5-MIG09-FROZEN-ORDER-PUBLIC-CRUD-GREEN-CLOSEOUT-MANIFEST-REFREEZE-001 / plan-r2.283` Maker 记录（2026-08-26）：Human Owner 只授权三份权威文档 closeout。双读冻结当前 HEAD=`ed4eaeddbfa3eb529847f3d14b23cf2c360c2337`、default manifest=`115/063a1de8fb1fd01f227e07dbabd3d7421726a03b973adc8488fb2b4e10601370`、`-uall=115/22dfd572039037c422d2b5029fef7c350e110752236c7884165261e98ea7b896`，staged empty、`git diff --check` PASS。state-naming 规格只读 SHA=`7eb5839e42f400bed876febb955fbbc29506bf5a0e8658564853aee2a7ba6dc6`；不读取为 FrozenOrder Green 依据，不进入 whitelist，不修改。既有技术 Green 与 45 份 XML 继续复用，本轮不重跑 Maven；当前进入独立 re-freeze Checker，不预写 Green 当前范围完成。

`W5-MIG09-FROZEN-ORDER-PUBLIC-CRUD-GREEN-CLOSEOUT-AFTER-STATE-NAMING-REBASE-001 / plan-r2.284` Maker 记录（2026-08-26）：state-naming 已在当前 HEAD=`ce360b039940aa951588bbe4c1bfdc43df181315` 完成，独立 Checker=`PASS / P0=0 / P1=0 / P2=0`。83 个非 Task6 提交路径与 HEAD 精确一致；118 份 source-backed Surefire XML 合计=`1205/0F/0E/1S`，唯一 skip 为 MySQL integration；13 份 PMD XML、13 个 JAR 和 48 个 NameRefs 均无违规或错误。该证据只关闭 survivor state 映射阻断，不替 MIG-09 准出。

旧 MIG-09 快照的 source 仍包含六个退役 CRUD 类型，而 absence XML 已为 Green，存在 source/classpath 不一致，本轮明确拒绝复用。当前 checkout 上 fresh 执行 `just verify-core-api`，clean compile reactor=`21/21`、Core API=`102/94/4/4 / 1025 lines`，六个旧类型在源码和 clean classpath 中均为 0；`just verify-public-contracts=300/176/42`，absence=`2/0F/0E/0S`，focused=`54/0F/0E/0S`，balance-control=`44/0F/0E/0S`，transaction=`185/0F/0E/0S`，business-flow=`200/0F/0E/0S`，boundary=`212/0F/0E/0S`。RED test SHA=`ac4b60ca...`，Entity/Mapper/lifecycle/Balance/query survivor SHA 分别为 `fbc50c16... / d3196f2e... / 64bcd018... / 4bcaddee... / 722d4ef9... / 70d78a2d...`；六个删除目标保持 absent。

三文档写前 SHA 为主系分=`8cafc5d2...`、TDD=`e87ef10a...`、本 OpenSpec=`b20660ee...`。写前双读稳定为 HEAD=`ce360b039940aa951588bbe4c1bfdc43df181315`、default/`-uall` manifest=`95/307f22161003c5d4a7df61230aff55927297e7b9bd123096c3b1a13b3686eb74`、staged empty、`git diff --check` PASS。本轮没有修改 Java、测试、构建、schema、Consumer 或 Git；当前=`FRESH_REVALIDATION_PASS / CLOSEOUT_INDEPENDENT_CHECKER_PENDING / MIG09_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`，不预写 Checker PASS。

`W5-MIG09-FROZEN-ORDER-PUBLIC-CRUD-GREEN-CLOSEOUT-AFTER-STATE-NAMING-REBASE-001 / plan-r2.285` 独立 Checker PASS 状态回写（2026-08-26）：Checker=`PASS / P0=0 / P1=0 / P2=0`。Checker 独立复算 45 份 12:35-12:38 fresh XML，确认 focused=`54`、balance-control=`44`、transaction=`185`、business-flow=`200`、boundary=`212` 全部零 failure/error；六旧类型源码/clean classpath 为 0，survivor/test SHA 与 OpenSpec 一致。三文档于 12:41 后才写入，12:38 后没有 Java、测试、构建或 schema 写入；旧 snapshot 确实同时存在六旧类型、旧正向 test SHA=`645ef419...` 与 absence=`2/0` XML，拒绝复用必要。当前 HEAD=`ce360b039940aa951588bbe4c1bfdc43df181315`、default/`-uall` manifest=`95/307f22161003c5d4a7df61230aff55927297e7b9bd123096c3b1a13b3686eb74`、staged empty、`git diff --check` PASS。

MIG-09 FrozenOrder Public CRUD retirement 当前范围完成，保持 `MIG09_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。下一唯一入口是 Human Owner 对其他 MIG-09 候选的 documentation-only Entry Card 授权；不得自动推进 raw external rail、Ledger 宽查询、全局 legacy 删除、兼容、Java/测试、Git、HOST/L4、enable/release/production。

`W5-MIG09-LEDGER-TRANSACTION-QUERY-SURFACE-NARROWING-ENTRY-CARD-001 / plan-r2.286` Maker 记录（2026-08-26）：Human Owner 回复“授权推进，并做价值分析确认”，按上一唯一入口只授权下一 MIG-09 documentation-only Entry Card。此前活跃的 `ENG-NAMING-CONTRACT-CLOSURE-001` 已正式完成并转 `VERIFIED`，全量=`1194/0F/0E/1S`、PMD/Public Contract/CAD/diff-check PASS，writer 已 idle；Maker 未与其并发写入，待其释放后才重冻本卡。

四仓 Java current scan 确认 `getLedgerTransactionById`、`queryAccountLedgerTransactions`、`getLedgerEntryById` 与 `LedgerTransactionQuery` 的生产调用均为 0；wind-funds 测试仅 `LedgerTransactionServiceFactQueryTests` 调用两个 by-id。Clearing 真实保留 caller 精确使用 `getLedgerTransactionBySn`、`queryLedgerEntries` 和 `existsPostingPlan`，`getLedgerEntryBySn` 作为稳定单分录事实查询继续保留。目标只移除无 tenant 的裸 DB id 和无 Consumer 交易宽分页，不新增替代 API/兼容层，不改 DTO、`LedgerEntryQuery`、Entity/Mapper/schema、posting/digest/Balance。

文件卡冻结为 RED `MODIFY=1`：仅 `LedgerTransactionServiceFactQueryTests.java`，保持 8 invocation 并形成 `8/1F/0E/0S`；Green `MODIFY=2 / DELETE=1 / ADD=0`：interface、`LedgerTransactionQuery`、impl。current SHA=`75259be8... / 48e766e1... / 5347d1a7... / 91c55f47...`；interface 既有 Javadoc diff SHA=`6daf9eb4...` 必须保留。Public Contract 目标=`300/176/42 -> 299/175/42`，Core API保持`102/94/4/4 / 1025`。

写前双读稳定为 HEAD=`ce360b039940aa951588bbe4c1bfdc43df181315`、default=`135/f5ef9504daf7b2e00dfa025c1dd45cc4407297b8d1958c3d097b5d92dcf51da9`、`-uall=136/28baf3837a119114c818087df43a993f16a38effcc78e969347bd3c53d08530c`、staged empty、`git diff --check` PASS。四文档写前 SHA 为 `02系分=36f3b955... / 主系分=80141707... / TDD=3c4efcda... / OpenSpec=93646c3e...`。本轮只回读 naming closure 的 fresh XML：Ledger=`77/0`、Transaction=`186/0`、Reconciliation=`247/0`、Business=`200/0`、Boundary=`211/0`，没有重复运行 Maven；没有修改 Java、测试、schema、build 或 Consumer。当前只进入独立 Entry Card Checker，不授权 RED/Green。

`W5-MIG09-LEDGER-TRANSACTION-QUERY-SURFACE-NARROWING-ENTRY-CARD-001 / plan-r2.287` 独立 Checker PASS 状态回写（2026-08-26）：Checker=`PASS / P0=0 / P1=0 / P2=0`。独立扫描确认四仓三个目标方法与 `LedgerTransactionQuery` 无生产 caller，测试仅有两个 by-id 调用；Clearing 真实使用 by-SN、entry query 和 plan membership，保留面准确。RED 保持 `5 @Test + 3 parameterized = 8` 且 `8/1F/0E/0S` 可执行；Green 精确为 interface/impl MODIFY、Query DELETE，连同两个只供 by-id 的 private helper，`ADD=0`。

Checker 复核 current SHA、interface 既有 Javadoc diff、Public Contract `300/176/42 -> 299/175/42`、Core API `102/94/4/4 / 1025`、Ledger=`77/0`、Transaction=`186/0`、Reconciliation=`247/0`、Business=`200/0`、Boundary=`211/0`、HEAD=`ce360b...`、双 manifest、staged empty 与 diff-check 均成立。`02` 分册的通用 system-design validator missing-section 属于既有模板适用性边界，主系分、OpenSpec refactoring、Harness 均 PASS；不得为工具越界重写分册。

当前只冻结 Entry Card，保持 `RED_EXECUTION_GRANT_NO / MIG09_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。下一唯一 Human gate 为 `MIG09_LEDGER_TRANSACTION_QUERY_SURFACE_NARROWING_RED_EXECUTION_GRANT`；不授权 Green、字段 `entryType/entrySide` 命名扩张、其他 MIG-09、Git、HOST/L4、enable/release/production。

`W5-MIG09-LEDGER-TRANSACTION-QUERY-SURFACE-NARROWING-RED-EXECUTION-001 / plan-r2.288` RED 与独立 Checker PASS（2026-08-26）：Human Owner 授权后只修改 `LedgerTransactionServiceFactQueryTests.java`。test SHA=`91c55f47... -> 956da091...`；测试仍为 `5 @Test + 3 parameterized = 8`，首个契约测试聚合 absence，tampered digest 仅移除两个 by-id 调用，保留 by-SN、entry query、plan membership、stable labels 和零事实副作用。

Java 21/Maven 3.6.3，写前/后 compile=`21/21`。fresh XML mtime=`2026-08-26 13:44:55`、SHA=`d2614a355cedc2ee89f34fba55c7489b15dabe3dcd92b5410d7b308f320ea334`、结果=`8/1F/0E/0S`；唯一 failure method=`testLedgerTransactionServiceShouldExposeStableSnGetQueriesWithoutOptional`，列表精确为 `getLedgerTransactionById / queryAccountLedgerTransactions / getLedgerEntryById / LedgerTransactionQuery`。生产 SHA未漂移，Public Contract=`300/176/42`。

独立 Checker=`PASS / P0=0 / P1=0 / P2=0`。post manifest default=`136/7d2b723a4f257b73870e0e3d541bce2732028987ea525dc3d6a8cb3de3bcab22`、`-uall=137/dfae9b3e2faf4c7654ba9e37e77736b4a6d39fa75dfb08d26ccde9810f14b845`、staged empty、`git diff --check` PASS；唯一目标新增 dirty 是该 test。`FundsTransactionQueryService` tenant scope、wallet/ledger 其余 raw-id、`entryType/entrySide`、projection explanation 与 context policy CR 候选均未混入。

当前保持 `GREEN_EXECUTION_GRANT_NO / MIG09_EXECUTION_GRANT_NO / CODE_FREEZE`。下一唯一 Human gate 为 `MIG09_LEDGER_TRANSACTION_QUERY_SURFACE_NARROWING_GREEN_EXECUTION_GRANT`，只允许 Entry Card 冻结的 `2 MODIFY + 1 DELETE / ADD=0`；不授权其他 CR 候选、Git、HOST/L4、enable/release/production。

`W5-MIG09-LEDGER-TRANSACTION-QUERY-SURFACE-NARROWING-GREEN-EXECUTION-001 / plan-r2.289` Green 与独立 Checker PASS（2026-08-26）：Human Owner 授权后精确修改 interface/impl 并删除 `LedgerTransactionQuery`，`ADD=0`；同一 impl 只额外删除随宽分页失去调用的 `enumName`。immutable RED test SHA=`956da091...`，interface/impl post SHA=`2a558337... / 93933bb4...`，旧 Query 在 source 和 Java 21 clean classpath 中均为 0。

验证结果为 clean compile=`21/21`、Public Contract=`299/175/42`、contract=`8/0`、Ledger=`77/0`、Reconciliation=`247/0`、Transaction=`186/0`、Business=`200/0`、Boundary=`211/0`；stable-SN、entry query、plan membership、digest、Balance 和 Clearing 均保持。显式 Java 21 `javap` 下 Core API baseline=`94 stable / 4 experimental / 4 internal` PASS；未带 Java 21 环境直接调用脚本时系统 Java 8 `javap` 会省略 interface `default` 标记并误报两条 `getExpiresAt` exclusion，此项只作非阻断环境观察。

独立 Green Checker=`PASS / P0=0 / P1=0 / P2=0`。post manifest default=`138/621c2c6309424fa6e45cda8036de7e1be0686b7929aee04f0df782f50d9f8fdc`、`-uall=139/2490da51900f50f3c35565d6778dcbef2729d16aa368a8a3aa1f81b6cfaccf60`、staged empty、`git diff --check` PASS。tenant scope、全 face raw-id、`entryType/entrySide`、projection explanation、无主 verification 字段与 context policy 均未混入本切片，仍需 Human Owner 选择新的 documentation-only Inquiry/Entry Card。

`W5-MIG09-TRANSACTION-QUERY-TENANT-SCOPE-BREAKING-CLOSEOUT-001 / plan-r2.290` 实现、验证与独立 Checker 收口（2026-08-26）：Human Owner 明确不考虑兼容。`FundsTransactionQueryService` 的主交易、明细、replay 消费判定/累计和两类 RouteSnapshot 持久化查询均已以 `Long tenantId` 为首参，旧 `queryFundsTransaction` 与无 tenant 重载为 0。实现中主交易、明细、replay 明细、冻结单与排除释放记录均带 tenant 谓词；45 个生产调用点传入 instruction/query/request/order/batch/transaction 已持有 tenant 或既有 `TenantContextHolder.requireTenantId()`。

跨租户在查询边界统一 fail-closed 为 empty/空列表/false/zero，Recovery 只看到“交易不存在”，不泄露外租户存在性。fresh tenant+contract=`5/0F/0E/0S`，Transaction=`186/0/0/0`，Reconciliation=`247/0/0/0`，Business=`200/0/0/0`，Boundary=`211/0/0/0`；clean compile=`21/21`，118 份 Surefire XML=`1198/0F/0E/1S`，唯一 skip 为无真实 MySQL host，Public Contract=`299/175/42`，13 份 PMD XML 无 violation/error。独立 Checker=`PASS / P0=0 / P1=0 / P2=0`。

本证据只准出 current checkout `RUNTIME_D4 / ENGINEERING_READY`，不声称仓外 Consumer 已迁移、目标环境已启用或生产 D5。`DefaultFundsInstructionLifecycleSaver` / `DefaultFundsFrozenOrderLifecycleSaver` 的私有 sn-only 读取只登记为后续 tenant-invariant Inquiry，不在本轮自动修复；raw-id、`entryType/entrySide`、projection explanation 和 context policy 同样需独立文档卡。本轮只回写主系分、主 TDD 与本 OpenSpec，未修改 Java、测试、Ledger、schema、build 或 Consumer。

`W5-MIG09-LIFECYCLE-TENANT-INVARIANT-ENTRY-CARD-001 / plan-r2.291` Maker 记录（2026-08-26）：Human Owner 在上一轮 CR 明确选择“推进修复”，按 plan-r2.290 唯一入口只授权 lifecycle tenant-invariant 的 documentation-only Entry Card。资产是 transaction/detail/frozen-order 生命周期事实、累计与后续 Ledger/Balance 解释；信任边界是 `FundsInstructionSpec.tenantId` 与 result/reference/context stable sn。上游 Route/Orchestrator 校验不替代 lifecycle 对象级 tenant 约束。

四仓当前生产调用确认两个 saver 只由 Transaction 内部 orchestrator/delegator 承重，但 `markSucceeded/markFailed`、reference transaction、refund summary、concurrent replay、unfreeze reference 和原冻结累计仍存在 sn-only 私有读取。当前只冻结一个最小行为：所有 sibling read 以 instruction tenant + stable sn 查询，外租户 result 在 lifecycle 边界 fail-closed 且零 transaction/detail/frozen/Ledger/Balance 变化；不同时 internalize face、不改 result DTO、route/query、Entity/Mapper/schema、Wallet/Ledger/Reconciliation、命名、摘要键、并发策略或兼容。

文件卡为 RED `ADD=1`：`FundsInstructionLifecycleTenantIsolationTests.java` 两个真实 Spring/H2 testcase，旧实现预期 `2/2F/0E/0S`；Green `MODIFY=2 / ADD=0 / DELETE=0`：只修改 `DefaultFundsInstructionLifecycleSaver.java` 与 `DefaultFundsFrozenOrderLifecycleSaver.java`。当前 target current/diff SHA=`dc0e037b.../b9919e1d...`、`4d13444b.../1136a9ef...`，既有 naming dirty 必须保留；测试基座与 Public Query tenant test SHA=`235681a2... / f27087ee...` immutable。

安全追踪冻结 `SR-LT-001~003 / CLAIM-LT-001`：transaction/detail result、frozen result/context reference 和全部 sibling query 都必须由 instruction tenant 约束；D1 验收后才能进入 RED，未来 D4 由精准 Green、Balance/Transaction/Business/Boundary、Public Contract、PMD/CAD 与独立 Checker 证明。当前 HEAD=`ce360b039940aa951588bbe4c1bfdc43df181315`、default/`-uall` manifest=`158/6a5270fee799415e654446a0840b21e9cfd61fc5d1831eb782a3c2505aa303ec`、`159/1d13c91b37328da0c1d4cadbf4dde299c63496cfe4e431264bad3ab6ad7cb160`、staged empty、`git diff --check` PASS；三文档写前 SHA=`4e251226... / 2ef650bd... / 1e745713...`。本轮未修改或运行 Java/测试，也未执行 Git。当前=`ENTRY_CARD_MAKER_COMPLETE / ENTRY_CARD_INDEPENDENT_CHECKER_PENDING / RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / DOCUMENTATION_ONLY / CODE_FREEZE`。

`W5-MIG09-LIFECYCLE-TENANT-INVARIANT-ENTRY-CARD-001 / plan-r2.292` 独立 Checker PASS 状态回写（2026-08-26）：Checker=`PASS / P0=0 / P1=0 / P2=0`，未修改文件、未执行 Git/Maven。问题定性为 `DEFENSE_IN_DEPTH_ELIGIBLE / LOCAL_WRITER_INVARIANT_MISSING`：正式 Command/Route 已 tenant-scoped，未证明当前正式入口可直接跨租户利用；但两个 saver 仍直接拥有资金事实写权，必须在本地边界把 result/reference/context stable sn 重新绑定到 instruction tenant。

Checker 独立确认旧源码只有四个无 tenant QueryWrapper，文档 helper/caller inventory 完整；两个真实 H2 RED 在旧实现会分别把 PROCESSING transaction/detail 改为 FAILED、把 CREATED frozen order 改为 FROZEN，预计精准=`2/2F/0E/0S`。Green `MODIFY=2` 足以删除无 tenant helper 签名并由编译强制全部 reference/context/concurrent-replay caller 迁移，无需 face/result/Orchestrator/Route/Mapper/schema。三文档 Checker 输入 SHA=`4a62fe1e... / 5f67502b... / 3a8a2f58...`，两个 saver、测试基座和上一 tenant test SHA 匹配；system-design、refactoring-design、lightweight 与 gsd-wave Harness 均 PASS。当前只进入 Human Owner RED Gate，不授权 Green、其他候选或 Git。

`W5-MIG09-LIFECYCLE-TENANT-INVARIANT-RED-EXECUTION-001 / plan-r2.293` RED 与返工 Checker 收口（2026-08-26）：Human Owner 只授权新增 `FundsInstructionLifecycleTenantIsolationTests.java`。首轮 SHA=`042b0515...`、XML=`53070f24...`、精准=`2/2F/0E/0S`，但因手工构造 route/snapshot 未命中冻结 Harness，独立 Checker=`NOT PASS / P0=0 / P1=1 / P2=0`。同一文件返工为真实 `RouteResolver -> RouteSnapshotFactory -> saver.beforePosting`，并对 TOPUP 三条 detail 及 FREEZE setup 后 Ledger/Balance 做 before/after 快照。最终 test SHA=`39ed7c98...`、fresh XML=`717fc287...`、仍为 `2/2F/0E/0S`，无 Harness error；独立 RED Checker=`PASS / P0=0 / P1=0 / P2=0`，测试随后 immutable。

`W5-MIG09-LIFECYCLE-TENANT-INVARIANT-GREEN-EXECUTION-001 / plan-r2.294` Green 与独立 Checker PASS（2026-08-26）：Human Owner 只授权修改两个 saver。Instruction saver 的 concurrent external replay、result transaction/detail、reference transaction 与 refund original summary，Frozen saver 的 result、unfreeze reference、persisted context recovery 与 `resolveFreezeType` 全部传入当前 `instruction.getTenantId()`。四个底层 QueryWrapper 均使用 NameRefs `tenantId + stable SN/transactionSn`，旧无 tenant helper 在源码/Java 21 classfile 中为 0；未新增 fallback、兼容重载、wrapper、双读/双写或抽象。

最终 saver SHA=`d252eb32... / 9ccefcb1...`，immutable RED=`39ed7c98...`。fresh lifecycle=`2/0`、联合 tenant+lifecycle=`8/0`、balance-control=`44/0`、transaction=`186/0`、business-flow=`200/0`、boundary=`211/0`，Public Contract=`299/175/42`，13 份 PMD XML 无 violation/error，`verify-cad` exit 0。119 份 Surefire XML=`1200/0F/0E/1S`，唯一 skip 仍为无真实 MySQL host；lifecycle XML=`ee25682f...` 晚于源码/classfile。独立 Green Checker=`PASS / P0=0 / P1=0 / P2=0`。本切片当前范围完成，只准出 current checkout `RUNTIME_D4 / ENGINEERING_READY`，不代表生产 D5。internalization、Wallet tenant/raw-id、same-tenant result 关联完整性、命名和 projection/context 继续独立分卡。

`W5-MIG09-WALLET-TENANT-RAW-ID-SURFACE-INQUIRY-001 / plan-r2.295` Maker 记录（2026-08-26）：Human Owner 在 plan-r2.294 当前范围完成后，只授权下一 documentation-only Wallet Inquiry 与价值分析。Maker 只读扫描 wind-funds、capte-domain、fincone、fincone-issuing：四仓 Java 文件数分别为 `780 / 1455 / 0 / 0`。Capte 生产只使用 Funding/Credit 的显式 `tenantId + accountSn/query` 和 tenant-scoped balance query；Fincone 两仓没有运行时 Java Consumer。

当前确认 12 个 wallet-face `getXxxById(Long id)` 均直接按 DB id 读取，12 个对应 Entity 均有 `tenantId`。它们没有仓外生产 caller：wind-funds 内只有 `PaymentInstrumentBindingService`、`SpendControlMovementService`、`SpendRuleDecisionRecordService`、`SpendRuleVersionService`、`SpendRuleBindingService` 五个内部/自调用，其余生产 caller 为 0；Capte 只有 Funding/Credit 两个 unsupported test stub。core `FundsAccountQueryService` 的 `getAccount/getLedgerProfileCode/getBalance/supports` 均只收 `FundsAccountId(id,type)`，生产调用为 `12/1/1/1`，会读取账户、profile、余额或存在性后再由部分上层事后校验 tenant。

`InternalAccountRef` 在四仓 Java 中为 0；它只是在 DSL/系分已接受的 `tenantId + accountId` 语义目标，物理 Java 类型仍 PENDING。实际 `FundsAccountId` 已被 Funds 与 Capte 承重，可继续作为 locator，但不是授权。Inquiry 冻结 `SR-WA-001~003 / CLAIM-WA-001`：tenant 决定授权，locator 只定位；外租户强查询统一不存在、`supports=false`，不得泄露状态/币种/能力/profile/余额/owner/层级；宽分页只在显式 tenant 或真实受控管理面下存在。

breaking 顺序冻结为 Core query tenant scope -> Funding query narrowing -> Credit query narrowing -> Platform/Hierarchy secondary cleanup -> PaymentInstrument、Spend/Responsibility、Spend Rule 三组 raw-id；create 返回 Long 与 DTO/Request id 另做 Inquiry，不偷带。推荐下一 Entry Card 只处理 Core query 四方法：候选 RED 新增真实 Spring/H2 `FundsAccountQueryTenantIsolationTests`，候选 Green 为系分 `11.41.5` 冻结的 `24 production MODIFY + 6 existing test MODIFY`；另 5 个装配/扩大测试只做 verification-only/non-write。不新增 `InternalAccountRef`、兼容重载或 schema。当前关键源码 SHA 为 core interface=`d9149c51...`、Default impl=`640fb1c9...`、Funding interface/impl/query=`76ae32ac... / 302b2f57... / 3d3a4706...`、Credit=`15f59ffe... / 424525fe... / 89dfd903...`；三文档写前 SHA=`2aaa77ec... / 15f72bda... / 69a549f8...`。

本轮只修改主系分、主 TDD 与本 OpenSpec，没有修改或运行 Java/测试/schema/build/Consumer，也未执行 Git。当前只达到 `DESIGN_D1 / ENGINEERING_READY_WITH_RISK`，进入独立只读 Inquiry Checker；Checker PASS 前不形成 Entry Card 准出，不授权 RED/Green、lifecycle internalization、entrySide、digest/context、projection、并发、其他 MIG-09、HOST/L4、发布或生产。

`W5-MIG09-WALLET-TENANT-RAW-ID-SURFACE-INQUIRY-001 / plan-r2.296` 独立 Checker PASS 状态回写（2026-08-26）：首轮 Checker=`NOT PASS / P0=0 / P1=1 / P2=0`，唯一 P1 是未来 Core Entry Card 的测试 MODIFY 白名单将 5 个只 import/装配 Default impl 或持有未调用字段的扩大测试误列为编译闭包。Maker 只把三文档 `11 -> 6`，保留 `AuthorizationFundsInstructionRouteResolverTests`、`CompositeRouteResolverTests`、`FundsAuthorizationTransactionFlowTests`、`SpendRuleEvaluationApplicationServiceTests`、`ControlAccountLedgerInitializationTests`、`FundingAccountServiceImplTests`，另 5 项明确 verification-only/non-write。

返工后三文档 SHA=`40e20e99... / f71d71bf... / 964e1e85...`，独立 Checker 最终=`PASS / P0=0 / P1=0 / P2=0`。Checker 确认 24 个 production 候选、RED、breaking 顺序、四仓 Consumer、12 raw-id、core `12/1/1/1`、Funding/Credit/Balance/Hierarchy/Platform、`InternalAccountRef` Java=0、`DESIGN_D1 / ENGINEERING_READY_WITH_RISK` 和排除项均成立；system-design、refactoring-design、lightweight Harness 与非 Git 格式检查通过。Inquiry 当前范围完成，不形成代码授权。

`W5-MIG09-CORE-FUNDS-ACCOUNT-QUERY-TENANT-SCOPE-ENTRY-CARD-001 / plan-r2.297` Maker 记录（2026-08-26）：Human Owner 明确授权 documentation-only Entry Card，并确认上一独立任务已把仓库提交到 `265dd18a6e1f4b8a30f7d2066433ddb769ee649d`、worktree clean、未 push。本 Maker 未执行 Git，从提交后源码重新双读并计算文件 SHA；不复用提交前 manifest。

Public breaking contract 冻结为 `getAccount/getLedgerProfileCode/getBalance/supports(Long tenantId, FundsAccountId)`，旧单参签名全部删除且不兼容。三个强查询 foreign tenant 统一当前 tenant 对象不存在，`supports=false`。tenant 只来自 instruction、request/order、`LedgerTransactionSpec` 或既有 Transaction TenantContext；禁止从返回 `FundsAccount`、RouteSnapshot、LedgerEntry context 或 locator 反推。

精确 Green 闭包为 `24 production MODIFY + 1 Core baseline MODIFY + 6 existing test MODIFY / ADD=0 / DELETE=0`。Ledger projection 保留 `project(entries, postingAccessType)`，删除其无 tenant account query 和 `supports`；Posting Owner 直接以 `transaction.getTenantId()` 调用新 core `supports`，因此五个扩大测试继续 verification-only/non-write。24 production manifest=`9e9a4e3ae1d9c7f0e6f69935d30d39743897524bb3061dd3076fbee37f879310`，6 test manifest=`71585768e8ace612120ac3a3630ba77c37914a587e7866dcdcdbb06606e7061b`，5 non-write manifest=`6179f18c3ed167a24707d47b915ac530e0f22bb4e95616403ab1f9a7e979b3d7`。core interface/impl SHA=`d9149c51... / 640fb1c9...`，baseline=`83b5fdbf... / 1025 lines`，RED target absent，三文档写前 SHA=`c5036a01... / ba292887... / a6d12fd8...`。

RED 只允许新增 `FundsAccountQueryTenantIsolationTests.java`，固定 `3 @Test / 3 invocations / old fresh 3F/0E/0S`。Public reflection/adaptive invocation 同时证明新签名缺失、旧签名存在和旧实现真实跨租户读取；Funding/Credit 均覆盖同租户 account/profile/balance/supports 正向、foreign tenant 强查询不存在/`supports=false`、敏感账户事实不泄露与零写副作用。RED Checker PASS 后 test immutable。

Core API verifier 脚本已有 Java 21 `javap` stable signature 比较能力，不修改；Green 只精确替换 `stable-api.txt` 第 800-803 行四个签名，保持 `94 stable / 4 experimental / 4 internal / 1025 lines`，Public Contract=`299/175/42`。当前 source-backed 基线为 `119 XML / 1200/0F/0E/1S`，未来完整 Green 预计 `1203/0F/0E/1S`；验证矩阵覆盖 compile、RED/Green、Wallet/Ledger、Balance/Transaction/Business/Reconciliation/Boundary、Public Contract、Java 21 Core API、PMD/CAD 与 diff-check。

当前=`ENTRY_CARD_MAKER_COMPLETE / ENTRY_CARD_INDEPENDENT_CHECKER_PENDING / RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。发现第 25 个 production、第 7 个 existing test、仓外 Core caller、缺 tenant 来源、新类型/DDL/Mapper/compatibility、Funding/Credit face/query/raw-id 或 baseline 四行外变化时立即停止并返回 Human Owner。

`W5-MIG09-CORE-FUNDS-ACCOUNT-QUERY-TENANT-SCOPE-ENTRY-CARD-001 / plan-r2.298` 独立 Checker PASS 状态回写（2026-08-26）：首轮 Checker=`NOT PASS / P0=0 / P1=2 / P2=0`。一项 P1 指出主系分 24 production 与 TDD 6 test 使用省略路径，manifest 不能替代文件白名单；另一项 P1 指出 baseline 目标仅写 TAB 右侧 signature，却称精确整行替换，缺少左侧 type prefix 与 literal TAB。

Maker 只在主系分/TDD 展开 24+6 为存在的完整 repository-relative path，并把 baseline 冻结为四条完整 `type prefix + literal TAB + new signature`；OpenSpec Maker 摘要、调用、tenant、SHA、manifest、RED、数量和范围均未改变。返工三文档 SHA=`a6e92287... / 0106a424... / 56483359...`，最终独立 Checker=`PASS / P0=0 / P1=0 / P2=0`。Checker 确认 24/6/5 manifest、Ledger 上移、Route/Amount sibling、RED `3/3F/0E/0S`、H2 矩阵、Core/Public counts、验证与停止线成立。

Entry Card 当前范围完成，不形成代码授权。下一 Human Gate=`MIG09_CORE_FUNDS_ACCOUNT_QUERY_TENANT_SCOPE_RED_EXECUTION_GRANT`，只允许 `ADD tests/src/test/java/com/wind/funds/wallet/services/impl/FundsAccountQueryTenantIsolationTests.java`，目标 fresh=`3/3F/0E/0S` 并独立 RED Checker；不授权 Green、24 production、baseline、6 existing test、Git 或其他 MIG-09。

`W5-MIG09-CORE-FUNDS-ACCOUNT-QUERY-TENANT-SCOPE-RED-AUTHORITY-ALIGNMENT-001 / plan-r2.299` Maker 记录（2026-08-26）：Human Owner 已消费单文件 RED 授权。最终 immutable test SHA=`300c4901e42da31e566e163ce1460a17df76809afec299571473c59edc20eda2`，fresh XML SHA=`d1edf42a89a934f71abf4a83dbd8236e51513eb831f50173c085f69b1de61903`，结果=`3/3F/0E/0S`。Public contract 失败证明四个新双参签名缺失且四个旧单参签名存在；Funding/Credit 失败分别证明外租户 account/profile/balance 被返回、无当前 tenant 不存在异常且 `supports=true`。同租户真实开户、profile、非零 balance 与 supports 正向通过，账户、Ledger、funds transaction/detail、ledger transaction/posting/entry 零副作用快照通过。

首轮独立 RED Checker 因异常不可泄露 token 未覆盖 account type、bucket、金额与 tenant 关系判定 `NOT PASS / P1=1`；同文件返工后最终 RED 代码/测试 Checker=`PASS / P0=0 / P1=0 / P2=0`，测试自此 immutable。Checker 同时指出 TDD `20.49.2` 将 `CREDIT_BASIC` 第四桶误写为 `SETTLEMENT`；`LedgerProfileCatalog`、`TDD-WALLET-002` 与 `TDD-CTRL-FLOW-003` 均证明真实控制桶为 `OUTSTANDING`。本轮只在三份权威文档机械纠正该 drift，不改其他 settlement 语义、Java、测试、baseline、24 production、6 existing test、5 non-write、schema/build 或验证矩阵。

三文档 `system-design / refactoring-design / lightweight` validators、格式、围栏、唯一 heading 与唯一恢复入口均 PASS。独立文档 Checker 以输入 SHA=`4486c86e... / cd13f8d0... / 2f7d8176...` 复核 `OUTSTANDING` authority、RED 证据、24+1+6+5 白名单、baseline 四行、验证矩阵与陈旧状态，结论=`PASS / P0=0 / P1=0 / P2=0`。当前=`RED_EXECUTION_COMPLETE / RED_INDEPENDENT_CHECKER_PASS / RED_TEST_IMMUTABLE / DOCUMENTATION_AUTHORITY_ALIGNMENT_INDEPENDENT_CHECKER_PASS / DOCUMENTATION_AUTHORITY_ALIGNMENT_COMPLETE / GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`。本记录不构成 Green、Git、发布或生产授权。

`W5-MIG09-CORE-FUNDS-ACCOUNT-QUERY-TENANT-SCOPE-GREEN-CLOSEOUT-001 / plan-r2.300` Maker 记录（2026-08-26）：Human Owner 已消费 Green、单一 Harness rework 与 documentation-only closeout 授权。Core 四方法只剩 `(Long tenantId, FundsAccountId)`，旧单参源码/Java 21 classfile 为 0；Default impl 在 Funding/Credit 首次读取使用 tenant+locator。24 production caller 的 tenant 均来自 Entry Card 冻结的 instruction、request/order、`LedgerTransactionSpec` 或 Transaction 既有 TenantContext，Ledger account authorization 已从 projection 上移至 posting Owner；Core baseline 只改四行，最终 SHA=`812e5328... / 1025 lines / 94 stable / 4 experimental / 4 internal`，六个 breaking-signature test 只做机械迁移，immutable RED SHA 保持 `300c4901...`。

首次完整 CAD=`1203/1F/0E/1S`，唯一失败是 `LedgerPostingJdkProxyContextTests` 缺少生产构造依赖 `FundsAccountQueryService` Bean。按独立 Human Grant 只为其现有 Config 增加默认 mock Bean，未改 test method、JDK proxy 断言、transaction manager、其他 Bean 或业务语义；最终 test SHA=`9ba73590...`，单类=`1/0F/0E/0S`。随后完整离线 CAD exit 0，120 份 fresh XML=`1203/0F/0E/1S`，唯一 skip 为无真实 MySQL host；tenant Green=`3/0`，两组 focused=`145/0`、`47/0`，balance/transaction/business/reconciliation/boundary=`44/0 / 186/0 / 200/0 / 247/0 / 211/0`，Public Contract=`299/175/42`，13 份 PMD XML 无 violation/error。独立技术 Green Checker=`PASS / P0=0 / P1=0 / P2=0`。

本证据只准出 current checkout `RUNTIME_D4 / ENGINEERING_READY`，不证明真实 MySQL、HOST/L4、发布或生产 D5。Funding/Credit face query narrowing、12 raw-id 分组、Platform/Hierarchy、`InternalAccountRef` 物理化、create/DTO id 和其他 MIG-09 仍需独立 Human Gate。本轮只修改主系分、主 TDD 与本 OpenSpec，不运行 Maven/Git，不修改 Java、测试、baseline、Consumer、schema 或 build。独立文档 closeout Checker 以 Maker 三文档 SHA=`948dc2f6... / 5d2703ee... / 1a336d0c...` 复核全部 Green 事实、验证计数、Harness 例外、D4 定级、残余边界、格式与唯一恢复入口，结论=`PASS / P0=0 / P1=0 / P2=0`。当前=`GREEN_CLOSEOUT_INDEPENDENT_CHECKER_PASS / CURRENT_SCOPE_COMPLETE / MIG09_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG09-FUNDING-ACCOUNT-QUERY-SURFACE-NARROWING-ENTRY-CARD-001 / plan-r2.301` Maker 记录（2026-08-26）：Human Owner 只授权 documentation-only Entry Card。FundingAccount 是真实资金余额/平台责任主体，tenant 是数据授权；DB id、accountSn、`FundsAccountId` 只定位对象。当前 Provider 两个旧 getter 的生产 caller 均为 0，wind-funds 只有 `FundingAccountServiceImplTests` 两处 by-id 调用；Capte 只有 `CouponActivityServiceImplTests` 匿名 interface 的两个未调用旧 override。真实生产路径全部已 tenant-scoped：wind-funds 的 AccountHierarchy、SpendSubjectFundingRelation 使用 tenant+sn，Core Default 与 Platform 使用显式 tenant query；Capte CouponActivity 使用 tenant+accountSn，receiver adapter 使用 tenant query；Fincone 两仓 Java=0。

根因不只是一组无主 API：`FundingAccountQuery.tenantId` 当前没有 Jakarta `@NotNull`，Service 也没有首次显式 guard，null tenant 可能被 MyBatis-Flex 省略为宽扫描。目标无兼容 hard cut 删除 `getFundingAccountById(Long)` 与 `getFundingAccount(FundsAccountId)`，保留 `getFundingAccount(Long,String)` 和 query；query field 增加 Jakarta `@NotNull`，Service 在 QueryWrapper 前显式拒绝 null，并继续使用 NameRefs tenant 谓词。foreign 强查询统一当前 tenant 不存在，query 只返回本 tenant，缺 tenant fail-closed，不泄露 state/currency/accountType/capability/profile/owner/context。

Provider RED 只新增 `FundingAccountServiceTenantIsolationTests.java`，固定 `3/2F/0E/0S`：contract/annotation 聚合 1F、缺 tenant 未拒绝 1F、既有 tenant-scoped get/query 隔离保持通过。Provider Green 精确=`3 production MODIFY + 1 wind existing test MODIFY / ADD=0 / DELETE=0`，production SHA=`76ae32ac... / 3d3a4706... / 302b2f57...`、manifest=`2abb19b7...`，test SHA=`460b42c5...`。不得修改 Core query、Credit、Platform/Hierarchy、其他 raw-id、create/DTO id、Entity/Mapper/schema/build 或 immutable Core tenant test。

Capte 是独立 Consumer Gate：Provider hard cut 后只修改 `CouponActivityServiceImplTests.java`（plan-r2.317 重冻 SHA=`d263b5fbad5344f43f82a3295d8d0911c0e268835c765bf3fdefef0fe9604283`）删除两个旧 override，再以 exact Provider artifact 执行 compile 和 CouponActivity/CouponRedemption/Order integration 三类测试。只读 production `CouponActivityServiceImpl.java` 同步重冻为 `8ad126aad5d2550953a0cf98e0a691588749743e66df4db12909fa7f3bbce68a`，`CouponBenefitReceiverAccountAdapter.java=d068ae90...` 未漂移；旧 `f46294bc... / 4d3e7239...` 只作 plan-r2.301 历史输入。Provider artifact 在 Consumer closure 前不得发布，也不得增加 compatibility 改变顺序。

当前 wind-funds source-backed baseline 为 120 XML=`1203/0F/0E/1S`；未来加 3 个 RED 后 old=`1206/2F/0E/1S`、Green=`1206/0F/0E/1S`。Public Contract=`299/175/42`、Core API=`94/4/4/1025` 保持，13 份 PMD XML 必须无 finding。三文档写前 SHA=`2bce7ca9... / c6201995... / 01503527...`；目标 source SHA 均从当前文件系统重算。system-design、refactoring-design 与 lightweight Harness 检查 PASS；全仓 Wind profile 守卫在 `check_redundant_jspecify_checks / METHOD_WITH_BODY.finditer` 长时间无输出后以 exit 130 中断，不计 PASS，目标 `Jakarta @NotNull + Service 显式 guard + NameRefs` 改由确定性源码检查、RED、Public Contract、PMD/CAD 与独立 CR 验证。因本轮禁止 Git/Maven，未复算 status/HEAD/dirty、未运行测试；last authorized HEAD=`265dd18a...` 只作历史基线。本轮只修改主系分、主 TDD 与本 OpenSpec。独立 Entry Card Checker 以 Maker 三文档 SHA=`4d4f26a1... / d706247e... / 929b796d...` 复核全部 caller、RED、文件卡、验证、工具边界和恢复状态，结论=`PASS / P0=0 / P1=0 / P2=0`；当前=`ENTRY_CARD_INDEPENDENT_CHECKER_PASS / RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG09-PUBLIC-READ-SURFACE-CONSUMER-ARTIFACT-CLOSURE-REBASE-001 / plan-r2.302` Maker 记录（2026-08-26）：Human Owner 在全项目价值评估后授权按建议推进，范围仅为主系分、主 TDD 与本 OpenSpec 的 documentation-only 权威重基线；没有授权 Java/测试/Capte/POM/schema/build、Git、Maven/install、联网、发布或生产。

新一手证据确认 current Provider source/class 已无兼容使用 tenant-scoped `FundsTransactionQueryService`，源码/class SHA=`899a202f... / f5525754...`；本机 Maven-local 同坐标 `wind-funds-transaction-face:1.0.1-SNAPSHOT` JAR SHA=`8df1ced2...`，仍是旧 `queryFundsTransaction(String)` 和无 tenant details/replay/RouteSnapshot 契约。Capte POM 同样声明 `1.0.1-SNAPSHOT`，两个 production Consumer SHA=`c32e6f1... / 03642377...` 仍有四处旧调用；两个 recording tests SHA=`a61baacf... / f385ecc4...` 仍实现旧 interface，integration test SHA=`0c863e2e...` 仍有两处旧调用。Capte target class 常量池也引用旧 interface，当前仓无 fresh Surefire XML。该 drift 不否定 Provider `RUNTIME_D4`，但证明仓外 Consumer/Artifact 尚未闭合，相同 Snapshot 坐标不能作为 source、built、resolved/loaded 同一性的证据。

本卡将下一顺序改为：先完成 Capte Transaction query Consumer/Artifact Closure，再恢复 plan-r2.301 Funding Provider RED；Funding 合同、`3 production + 1 wind test + 1 independent Capte test` 文件卡和 `3/2F` RED 均保持有效，不重新评审或扩大。未来 Capte write 精确 `MODIFY=5 / ADD=0 / DELETE=0`：`CouponBenefitFundingSettlement.java`、`WalletPaymentParticipant.java`、`CouponRedemptionApplicationServiceImplTests.java`、`OrderCouponRedemptionIntegrationTests.java`、`WalletPaymentParticipantTests.java`。production tenant 只来自已持有 Coupon request、`PaymentContext` 或 `RefundContext`；禁止从返回 transaction、SN 或 ThreadLocal fallback 反推。`CouponActivityServiceImpl`、`CapteOrderConfiguration`、`CouponActivityServiceImplTests` verification-only/non-write；两仓 POM/schema/build/source 非白名单保持 immutable。

未来 RED 为 exact current Provider artifact 下的 Capte compile/testCompile：只允许五文件因旧方法缺失或 recording stub 未实现当前 abstract method 失败；其他 compile/依赖/Harness/POM/schema failure 立即停止。Green 后要求 Capte full reactor compile/testCompile、三类承重测试与一类扩大测试 fresh 非零执行且 0F/0E/0S，旧查询/无 tenant signature 正向引用=0。Provider source carrier、隔离 Maven local built、Consumer resolved/loaded 的 core、transaction-face/impl、wallet-face/impl、ledger-face/impl 七制品 SHA 必须逐项一致；不得覆盖主 `~/.m2` 或远端 publish。当前无真实 MySQL host 是公共库证据边界，不阻断 Consumer closure，也不构成 wind-funds 自建数据库任务。

系分同时机械纠正两处陈旧当前态：authorization release 已 Green，不再写“尚无 canonical”；MIG-09 已进入多项子切片，不再写“全局未开始”。阶段知止线保持：不物理化 `InternalAccountRef`、不清理 create 返回 `Long`、不合并其他 raw-id、`entrySide`、projection/context 或 lifecycle internalization，不新增 compatibility。当前=`DOCUMENTATION_REBASE_MAKER_COMPLETE / INDEPENDENT_CHECKER_PENDING / CAPTE_CONSUMER_EXECUTION_GRANT_NO / FUNDING_RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG09-PUBLIC-READ-SURFACE-CONSUMER-ARTIFACT-CLOSURE-REBASE-001 / plan-r2.303` Checker NOT PASS 与最小返工记录（2026-08-26）：独立 Checker 对 plan-r2.302 判定=`NOT PASS / P0=0 / P1=3 / P2=0`。P1-A：`CouponActivityServiceImpl.java` 与 `CouponActivityServiceImplTests.java` 仍直接引用 Provider 已删除的 Benefit facade，不能是 verification-only，五文件白名单漏为七文件。P1-B：current Capte settlement、Coupon tests 与 integration 仍依赖已删除 facade/request/enum/impl，query-only compile RED 会先命中 R8B surface，无法按五文件迁绿；应复用已接受 R8B 五文件迁移并叠加 Wallet query 两文件。P1-C：本 OpenSpec 8.31 当前 MIG-09 表仍写 `NOT_STARTED/BLOCKED_BY_MIG08`，与已完成子切片和当前入口矛盾。

Maker 只在原三文档授权内返工：Consumer Green 重冻为 `MODIFY=7 / ADD=0 / DELETE=0`，即 R8B Capte 五文件 `CouponBenefitFundingSettlement`、`CouponActivityServiceImpl`、`CouponRedemptionApplicationServiceImplTests`、`CouponActivityServiceImplTests`、`OrderCouponRedemptionIntegrationTests`，加 Transaction query 两文件 `WalletPaymentParticipant`、`WalletPaymentParticipantTests`。settlement 恢复既有 generic direct pay/referenced refund + ActionFact，不保留 root query fallback；activity readiness 使用 direct service；Coupon recording/integration 同时恢复 R8B 行为并实现 tenant-scoped query；Wallet tenant 只来自 `PaymentContext/RefundContext`。`CapteOrderConfiguration` 继续 non-write，R8B boundary/Face tests immutable。

compile RED 现在允许且只允许两组已接受缺失：Benefit facade/request/enum/impl 与旧 Transaction query/recording signature；不冻结 javac 级联诊断数。Green 验证恢复已接受准确口径：application=`57/0F/0E/0S`、activity=`37/0F/0E/0S`、R8B boundary=`32/0F/0E/0S`、integration main=`25/0F/0E/3S`，lineage/seed/recover method-only 各=`1/0F/0E/0S`；七制品必须使用由 source fingerprint 派生的唯一 non-Snapshot validation version并在隔离 Maven local built/resolved/loaded SHA 相等。当前旧 Snapshot/r9 均不可复用或发布。

plan-r2.301 Funding Provider 合同、`3/2F`、3 production + 1 wind test 保持；但七文件 closure 会合法修改 `CouponActivityServiceImplTests`，其 `4d3e7239...` 只保留为当前写前 SHA，closure closeout 必须从新 source 重冻 Funding Capte 单文件后才恢复 Funding RED。OpenSpec 8.31 MIG-09 当前行同步为 `IN_PROGRESS / PROVIDER_D4 / CAPTE_R8B_AND_TRANSACTION_QUERY_CONSUMER_CLOSURE_REQUIRED / FUNDING_RED_DEFERRED`。当前=`DOCUMENTATION_REWORK_MAKER_COMPLETE / INDEPENDENT_RECHECK_PENDING / CAPTE_CONSUMER_EXECUTION_GRANT_NO / FUNDING_RED_EXECUTION_GRANT_NO / CODE_FREEZE`，不预写 recheck PASS。

`W5-MIG09-PUBLIC-READ-SURFACE-CONSUMER-ARTIFACT-CLOSURE-REBASE-001 / plan-r2.304` 机械 P2 返工记录（2026-08-26）：plan-r2.303 recheck 确认原三项 P1 已全部关闭，七文件 union、双 failure family、tenant 来源、测试矩阵、七制品谱系和 Funding 重冻顺序均可执行；但判定 `NOT PASS / P0=0 / P1=0 / P2=2`。Maker 只做两处机械修正：主系分 MIG-09 汇总从 query-only 状态同步为 `CAPTE_R8B_AND_TRANSACTION_QUERY_CONSUMER_CLOSURE_REQUIRED` 并补 current Capte 同时丢失 R8B/旧 query 的事实；TDD 把错误的“前五项”改为“除两个 Wallet 文件外的五个 Coupon production/test 路径”。七文件、行为、计数、artifact version/lineage、Funding 合同、授权和停止线均未改变。当前=`DOCUMENTATION_MECHANICAL_REWORK_COMPLETE / FINAL_RECHECK_PENDING / CAPTE_CONSUMER_EXECUTION_GRANT_NO / FUNDING_RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG09-PUBLIC-READ-SURFACE-CONSUMER-ARTIFACT-CLOSURE-REBASE-001 / plan-r2.305` 最终独立 Checker PASS 状态回写（2026-08-26）：Checker=`PASS / P0=0 / P1=0 / P2=0`。确认 current 最小 Consumer Green 为 R8B 五个 Coupon production/test 路径与两个 Wallet query 路径的七文件 union；compile RED 只含已接受 Benefit surface 删除与 Transaction tenant query hard cut两组 failure family；tenant 来源、application=`57`、activity=`37`、boundary=`32`、main=`25/3S`、lineage/seed/recover 各 `1`、unique non-Snapshot 七制品 built/resolved/loaded 和 Funding `CouponActivityServiceImplTests` 新 SHA 重冻顺序均一致。system-design、refactoring-design、lightweight Harness、格式、围栏、相对链接与唯一恢复入口通过。本 PASS 只关闭 documentation rebase，不授权 Consumer/Artifact Closure 执行、Funding RED、Maven/install、Git、远端发布或生产。

`W5-MIG09-CAPTE-R8B-TRANSACTION-QUERY-CONSUMER-ARTIFACT-CLOSURE-ENTRY-CARD-REWORK-001 / plan-r2.306` Maker 记录（2026-08-26）：Human Owner 对 plan-r2.305 下一 Gate 回复“授权推进，并作价值分析确认”。执行前 Maker 未写入七文件、未运行 Maven/Git；只读 preflight 发现 Capte `docs/系分设计/通用优惠券-系分设计.md` SHA=`8576f7d6...` 仍在 329/816 行要求已删除 `FundsBenefitContributionTransactionService.settle/refund`，`CouponImplContractBoundaryTests.java` SHA=`e7c49503...` 也将其列为 required terms。`CouponFaceContractShapeTests` SHA=`db36df30...` 仅含合法负向禁止 token。独立 drift Checker=`NOT PASS / P0=0 / P1=2 / P2=0`，裁决七文件 Execution Gate 准入失败且未消费，最小整体闭包为九文件，不能现场扩权。

Maker 按“文档先行”把九文件拆为两个串行 Gate。Gate A=`MIG09_CAPTE_R8B_AUTHORITY_DOC_RESTORE_GRANT`，只允许 Capte 单文件正式系分从旧 facade 恢复为 Consumer adapter -> `FundsDirectTransactionService.pay/referenced refund` -> `FundsActionFact`；不改业务责任、Money、原事实、route、Ledger/Balance、POM/schema或代码。若该 Gate 同时明确授权 offline focused test，则 current boundary 预期=`32/1F/0E/0S`，唯一 failure 是旧 required terms 与新 authority 不一致。Authority Doc Checker/RED Checker PASS 前不进入代码。

Gate B 未来精确 `MODIFY=8 / ADD=0 / DELETE=0`：原七 Consumer 文件加 `CouponImplContractBoundaryTests`。前七恢复 R8B/tenant query；boundary 将 required/source assertions 转为 generic direct + ActionFact + tenant query，禁止已删除 Benefit surface 与旧完成 fallback，其他 31 项不变。Green 目标为 application=`57/0F/0E/0S`、activity=`37/0F/0E/0S`、boundary=`32/0F/0E/0S`、main=`25/0F/0E/3S`、lineage/seed/recover 各=`1/0F/0E/0S`，并使用 source fingerprint 派生的 unique non-Snapshot version证明七制品 built/resolved/loaded SHA 一致。完成后重冻 Funding Capte test SHA，再恢复 Funding RED。

本轮只修改主系分、主 TDD 与本 OpenSpec，没有修改 Capte authority/code/test、wind-funds Java/test、POM/schema/build，也没有执行 Maven/Git/install/联网/发布。当前=`ENTRY_CARD_REWORK_MAKER_COMPLETE / INDEPENDENT_CHECKER_PENDING / CAPTE_AUTHORITY_DOC_RESTORE_GRANT_NO / CONSUMER_GREEN_EXECUTION_GRANT_NO / FUNDING_RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG09-CAPTE-R8B-TRANSACTION-QUERY-CONSUMER-ARTIFACT-CLOSURE-ENTRY-CARD-REWORK-001 / plan-r2.307` Checker P1 与返工记录（2026-08-26）：plan-r2.306 独立 Checker=`NOT PASS / P0=0 / P1=2 / P2=0`。P1-A 指出 Gate A 不能只改两处旧 service：Capte 系分 325-327/333 仍把 `costBearerSubjectRef/benefitReceiverSubjectRef/fundingNature` 当 Funds 参数，418/823 仍使用旧 settle/refund；已接受 R8B 必须统一为 `FundsAccountId + 显式账目 + Money + business identity/original ref -> generic pay/referenced refund -> FundsActionFact`，`fundingNature/context` 留 Consumer。P1-B 指出 current boundary 有 Gate terms 与 Decision Package terms 两个独立测试方法，完整 authority 恢复可能产生两个 failures，不能预写 `32/1F`。

Maker 只在原三文档内返工：Gate A 的单文件 target hunk 现在精确覆盖 Capte 系分 315-333 的准入/参数/业务键/资金性质、418/421 的对象与资金事实、814-817 的原事实/完成查询和 823-828 的生产编码要求；不改其他产品/数据/状态设计。focused RED 不再冻结 failure 数，只允许 `testBenefitFundingDeliveryDocsKeepSubjectMappingAndImpactModeGates` 与 `testBenefitFundingDeliveryDocsKeepSubjectMappingDecisionPackage` 两个方法失败，实际 methods/count 由 fresh XML 记录，必须 `0E/0S` 且其他测试通过。Gate A closeout 回写实际基线并通过独立 Checker 前，Gate B 仍不可执行。整体九文件、1 doc -> 8 code/test、unique artifact、Funding 重冻、权限和下一 Gate 不变。当前=`ENTRY_CARD_REWORK_MAKER_COMPLETE / INDEPENDENT_RECHECK_PENDING / CAPTE_AUTHORITY_DOC_RESTORE_GRANT_NO / CONSUMER_GREEN_EXECUTION_GRANT_NO / FUNDING_RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

`W5-MIG09-CAPTE-R8B-TRANSACTION-QUERY-CONSUMER-ARTIFACT-CLOSURE-ENTRY-CARD-REWORK-001 / plan-r2.308` 最终独立 Checker PASS 状态回写（2026-08-26）：Checker=`PASS / P0=0 / P1=0 / P2=0`。确认 Gate A 已冻结 Capte 系分完整 R8B authority hunk，不再只换 service 名；focused boundary RED 只允许两个既有文档方法失败，实际 methods/count 由 fresh XML 回写，足以阻断第三类 failure；整体九文件按 `1 authority doc + 8 code/test` 串行，unique non-Snapshot 七制品、Funding 新 SHA 重冻与全部 Grant NO 一致。system-design、refactoring-design、lightweight Harness、格式、围栏、相对链接与唯一恢复入口通过。本 PASS 不授权 Authority Doc Restore、focused Maven RED、代码 Green、Funding RED、Git 或发布。

`W5-MIG09-CAPTE-R8B-AUTHORITY-DOC-RESTORE-AND-BOUNDARY-RED-CLOSEOUT-001 / plan-r2.309` Maker 与 RED 记录（2026-08-27）：Human Owner 授权 Gate A 后，唯一 Capte 写入为 `docs/系分设计/通用优惠券-系分设计.md`，SHA=`8576f7d6... -> 9ba8749e...`。完整新 authority 保留 Capte funding nature、多出资方、适用商户、原订单与退款资格；Funds 输入收敛为 `FundsAccountId + CLEARING + Money + business identity/original intentRef`，执行为 `FundsDirectTransactionService.pay/referenced refund`，完成只认 primary/recovery `FundsActionFact`，不外推 Ledger/Balance/外部到账/生产 D5。文档 system-design、相对链接、围栏和尾空白 PASS。

immutable `CouponImplContractBoundaryTests.java`=`e7c49503...`、root/tests POM=`6655fa65... / b3ecc50f...` 未漂移。fresh XML mtime=`2026-08-27 09:07:48 +0800`、SHA=`0e35facd0ed9db0a8497aa0b8b1b53d6473bde33b747efdb4b9d15a894b1fa6d`，实际=`29/2F/0E/0S`；只失败 `testBenefitFundingDeliveryDocsKeepSubjectMappingAndImpactModeGates` 和 `testBenefitFundingDeliveryDocsKeepSubjectMappingDecisionPackage`，均命中旧 `costBearerSubjectRef` required-term，其余 27 项通过。42-module reactor 的 `BUILD SUCCESS` 不作为测试成功证据。独立 Checker=`AUTHORITY_BOUNDARY_RED PASS / P0=0 / P1=0 / P2=0`。本记录冻结 RED，但在三文档 closeout Checker PASS 前不准出八文件 Green Human Gate。

`W5-MIG09-CAPTE-R8B-AUTHORITY-DOC-RESTORE-AND-BOUNDARY-RED-CLOSEOUT-001 / plan-r2.310` 最终状态回写（2026-08-27）：plan-r2.309 closeout 首轮仅发现两处现行 MIG-09 汇总仍停在 Authority Restore 前，判定=`NOT PASS / P0=0 / P1=0 / P2=1`。Maker 只把主系分 MIG-09 row 与 OpenSpec 8.31 row/现行说明同步为 `AUTHORITY_BOUNDARY_RED_INDEPENDENT_CHECKER_PASS / CONSUMER_GREEN_8_FILE_PENDING / FUNDING_RED_DEFERRED`，未改业务设计、八文件清单、RED、代码或测试。recheck=`PASS / P0=0 / P1=0 / P2=0`。本 PASS 关闭 Gate A；下一 Human Gate 仅为 `MIG09_CAPTE_R8B_AND_TRANSACTION_QUERY_CONSUMER_ARTIFACT_CLOSURE_EXECUTION_GRANT`，Green 仍为 `NO`，Funding RED/Git/发布/生产仍未授权。

`W5-MIG09-CAPTE-R8B-AND-TRANSACTION-QUERY-CONSUMER-ARTIFACT-CLOSURE-EXECUTION-001 / plan-r2.311` Human Grant 与执行准入记录（2026-08-27）：Human Owner 回复“授权推进，并做价值分析确认”，授权 plan-r2.310 唯一下一 Gate。双读八文件/POM/authority SHA稳定；Provider source fingerprint=`298111e715f8c7bdef45a12a475590ab7abfba8461b3c7e9f27807d9c09093d1`，唯一版本=`1.0.1-mig09-capte-20260827-298111e715f8`。Java 21/Maven 3.6.3 offline Provider flatten+install=`21/21`；raw POM `${revision}` 与非 clean class reuse 两次 Harness 诊断已纠正，未改源码/POM。最终 Capte `clean compile` 只命中 Benefit absence 与 Transaction tenant hard cut 两组冻结 failure family。当前只允许精确八文件 Green 与 20.51/20.52 验证；尚未写入生产/测试，也不宣称 Green、lineage 或 Checker PASS。

`W5-MIG09-CAPTE-R8B-AND-TRANSACTION-QUERY-CONSUMER-ARTIFACT-CLOSURE-EXECUTION-001 / plan-r2.312` Blocker closeout（2026-08-27）：Maker 在精确八文件内完成旧 Benefit facade 到 direct pay/referenced refund + ActionFact 的破坏式迁移，并将 Wallet transaction/detail query 硬切为显式 tenant；八个 post SHA=`14ad0aec... / 8ad126aa... / bec2a668... / ba6ab0da... / d263b5fb... / 49197ffa... / 27495ae0... / da8ff5d8...`。Provider offline=`21/21`，Capte production focused compile=`10/10`，Wallet + Activity + Boundary fresh=`74/0F/0E/0S`（实际 `8+37+29`）。

Coupon application fresh XML SHA=`1bb2b04d...`、mtime=`2026-08-27 10:53:52 +0800`、结果=`49/0F/14E/0S`；一项 tenant fixture 已在授权文件内修复但未 fresh 重跑，其余 13 项先命中 `t_funding_account.STATE` 缺失。只读闭包确认 Capte schema SHA=`d0c7536a...` 的 `t_funding_account/t_credit_account/t_ledger/t_funds_transaction/t_funds_transaction_detail/t_funds_frozen_order` 仍用 `status`，host contract SHA=`a02cc673...` 仍要求 `STATUS`，已有 integration 还有 5 处 raw SQL `status`。独立 Checker=`NOT PASS / P0=0 / P1=2 / P2=1`，禁止双列、alias、test-local ALTER、mock 或跳过 H2。

当前最小候选必须重冻结为原八文件加 `tests/src/test/resources/jdbc-schema.sql` 与 `tests/src/test/java/com/capte/order/transaction/FundsProviderHostSchemaTests.java`，即 `MODIFY=10 / ADD=0 / DELETE=0`；只做六表/索引、host expected column 和 integration raw SQL 的 `status -> state` 硬切，不扩其他 DDL。该候选尚未授权。下一 Human Gate=`MIG09_CAPTE_R8B_AND_TRANSACTION_QUERY_CONSUMER_ARTIFACT_CLOSURE_ENTRY_CARD_REWORK_GRANT`，只允许三份 wind-funds 权威文档重做与独立 Checker；未来 10 文件 Green 需再次单独授权，Funding RED/Git/联网/发布/生产继续 `NO`。

plan-r2.312 三文档状态回写独立 Checker=`PASS / P0=0 / P1=0 / P2=0`：八个 post SHA、fresh `74/0` 与 `49/14E`、六表 schema drift、host `STATUS`、integration 5 处 raw SQL、10 文件最小闭包、无兼容和授权冻结均复核一致；没有第 11 个必改文件。该 PASS 只关闭 blocker 状态记录，不把 Consumer Green、10 文件 Entry Card 或后续执行改写为已通过/已授权。

`W5-MIG09-CAPTE-R8B-AND-TRANSACTION-QUERY-CONSUMER-ARTIFACT-CLOSURE-ENTRY-CARD-REWORK-002 / plan-r2.313` Maker 记录（2026-08-27）：Human Owner 授权 documentation-only Entry Card rework。双读三文档与 10 个 Capte 文件稳定；累计 manifest=`44b60a79...`。Maker 将前一版宽泛 `MODIFY=10` 收紧为累计 closure=`10`、未来实际 `MODIFY=3`、`IMMUTABLE=7`：只写 `jdbc-schema.sql`、`FundsProviderHostSchemaTests.java`、`OrderCouponRedemptionIntegrationTests.java`，其余 7 个已迁移 production/test 文件以当前 SHA 冻结。

六表 contract 精确为 lifecycle column `status -> state`，五个 status index 同步改名/改列，expire index 只改第二列；host 单测试同时断言六表有 `STATE`、无 `STATUS`；integration 只迁 5 处 raw SQL。当前 source counts 重新核实为 application=`49`、activity=`37`、wallet=`8`、boundary=`29`、host=`1`、integration=`25`，不是历史 `57/32`；此前 26 是把类级 `@TestPropertySource` 误计为测试。未来 default fresh 合计目标=`149/0F/0E/3S`，lineage/seed/recover 已冻结精确 method-only 命令且各须 `1/0F/0E/0S`；七制品已冻结 target/resolved `shasum` 命令，并由 lineage 继续验证 runtime loaded。本卡不要求真实 MySQL host，不构成部署或 D5；11.45.8/20.52.7 supersede 旧八/十文件当前卡。当前=`ENTRY_CARD_REWORK_MAKER_COMPLETE / INDEPENDENT_RECHECK_PENDING / GREEN_EXECUTION_GRANT_NO / FUNDING_RED_EXECUTION_GRANT_NO / CODE_FREEZE`。

plan-r2.313 首轮独立 Checker=`NOT PASS / P0=0 / P1=3 / P2=1`：integration source count 误把 `@TestPropertySource` 计入，phase 与 target/resolved 对账命令未展开，旧八文件 Gate 未明确 supersede，cumulative manifest 顺序说明与实际生成顺序不一致。Maker 只在原三文档修正为 integration=`25`、default total=`149`，补三条 method-only 和七组 target/resolved 命令，统一 11.45.8/20.52.7 为当前唯一卡，并分别声明 cumulative/immutable/write manifest 顺序；三文件 Green、7 immutable、六表合同和业务边界均未改变。当前等待独立 recheck。

plan-r2.313 最终独立 recheck=`PASS / P0=0 / P1=0 / P2=0`。确认累计 closure=`10`、未来 `MODIFY=3 / IMMUTABLE=7`、六表/五索引/expire index、host 单测试、integration 五处 SQL、default=`149/0F/0E/3S`、三阶段与七制品命令、manifest 顺序、无兼容和非 MySQL/D5 边界均可执行；下一 Human Gate 仅为三文件 `MIG09_CAPTE_R8B_AND_TRANSACTION_QUERY_CONSUMER_ARTIFACT_CLOSURE_GREEN_REWORK_EXECUTION_GRANT`，当前仍为 `NO`。

`W5-MIG09-CAPTE-R8B-AND-TRANSACTION-QUERY-CONSUMER-ARTIFACT-CLOSURE-GREEN-REWORK-EXECUTION-001 / plan-r2.314` Green 执行与阻断记录（2026-08-27）：Human Owner 授权 plan-r2.313 唯一三文件 Green。Maker 只修改 `jdbc-schema.sql`、`FundsProviderHostSchemaTests.java`、`OrderCouponRedemptionIntegrationTests.java`，post SHA=`1ba05e12... / 9cdaa54b... / 02a69b1a...`；七个 immutable SHA 全部保持，没有第四个 Capte 写入、兼容、POM 或 Provider 修改。六表和索引无兼容硬切到 `state`，host test 同时守住六表有 `STATE`、无 `STATUS`，integration 五处 raw SQL 已迁移；其 capability-rejection payer fixture 只与既有 request helper 对齐，完整保留 RECEIVE reject、failed ActionFact 和 Ledger/Balance 零副作用。

旧 schema 上 host contract fresh=`1/1F/0E/0S` 且只命中目标列缺失；Green 后 Java 21/Maven 3.6.3 reactor=`45/45`，default 六类=`149/0F/0E/3S`，lineage/seed/recover 各=`1/0F/0E/0S`，七制品 target/resolved/loaded SHA 逐项相等。实际命令纠正了 Entry Card 的两个 Harness 缺陷：使用 `test-compile` 而非不存在的 `testCompile`，并显式设置 `-Dmaven.test.skip=false` 使测试真实运行。

独立 Green Checker=`NOT PASS / P0=0 / P1=1 / P2=0`。唯一 P1 是冻结 PMD PASS 但没有可满足证据：全仓 PMD 在未修改的 `web-security` 四文件命中 6 个既有 violation，报告 SHA=`3690f4b9f315f1b505a79ab0256667cf066bbbfab465e617c7ef2f14edb57a33`；tests module PMD 实际 `No files to analyze`，不能冒充本切片 PASS。该裁决不否定业务 Green、宿主 schema、资金断言、重启恢复或 artifact lineage，也不授权清理 `web-security`。当前=`GREEN_REWORK_EXECUTION_COMPLETE / GREEN_INDEPENDENT_CHECKER_NOT_PASS / PMD_GATE_ENTRY_CARD_REWORK_REQUIRED / FUNDING_RED_EXECUTION_GRANT_NO / CODE_FREEZE`；下一 Human Gate 只能是三文档 `MIG09_CAPTE_R8B_AND_TRANSACTION_QUERY_CONSUMER_ARTIFACT_CLOSURE_PMD_GATE_REWORK_GRANT`，用于冻结 no-new-violation 的可执行 PMD 边界并重新 Checker。Capte、Funding RED、其他 MIG-09、Git、联网、远端发布、部署和生产均未授权。

`W5-MIG09-CAPTE-R8B-AND-TRANSACTION-QUERY-CONSUMER-ARTIFACT-CLOSURE-PMD-GATE-REWORK-001 / plan-r2.315` Maker 记录（2026-08-27）：Human Owner 授权上一唯一 documentation-only Gate。Maker 只修改主系分、主 TDD 与本 OpenSpec，没有修改 Capte、`web-security`、Funding、Java、测试、schema、POM 或构建配置，也未执行 Maven/Git/联网。PMD 准出被重做为三层：相关 `marketing/coupon-impl` 与 `order/order-impl` 报告晚于三份 changed production source 且 violation=`0/0`；全仓只允许未修改 `web-security` 的精确 `4 files/6 violations` baseline red，当前报告 SHA=`3690f4b9...` 只作 artifact identity，未来按 semantic tuple 而非 timestamp-sensitive SHA 比较；tests report=`0 file/0 violation`，明确为 `NOT_APPLICABLE_TO_CHANGED_TEST_FILES`，不能冒充 PASS。

test/schema 继续由 source SHA 未漂移的 Java 21 `45/45` compile/test-compile、default=`149/0F/0E/3S`、lineage/seed/recover 各=`1/0F/0E/0S`、六表 host contract、旧 surface/source guard、资金失败零副作用与独立 Checker 承重。该重做既不豁免相关 production finding，也不把无关 `web-security` 清债塞进 Consumer closure。当前=`PMD_GATE_REWORK_MAKER_COMPLETE / INDEPENDENT_CHECKER_PENDING / GREEN_CLOSEOUT_PENDING / FUNDING_RED_EXECUTION_GRANT_NO / CODE_FREEZE`，不预写 Checker PASS 或恢复 Funding RED。

`plan-r2.316` PMD Gate 独立 Checker 状态回写（2026-08-27）：Checker=`PASS / P0=0 / P1=0 / P2=0`。确认 `coupon-impl/order-impl` 分别有 41/55 个 production source、fresh PMD violation=`0/0`；全仓只保留 `web-security` 精确 4 文件/6 finding；tests PMD=`0 file / NOT_APPLICABLE`；编译、149 个 default tests、三阶段恢复、host/source guard、资金零副作用与七制品 lineage 足以闭合本轮静态质量责任。该 PASS 只准出后续机械 Green closeout，不自行写 `CURRENT_SCOPE_COMPLETE`，不授权 Capte、`web-security` 或 Funding RED。下一 Human Gate=`MIG09_CAPTE_R8B_AND_TRANSACTION_QUERY_CONSUMER_ARTIFACT_CLOSURE_GREEN_CLOSEOUT_GRANT`，只允许三文档收口并重冻 `CouponActivityServiceImplTests.java` SHA=`d263b5fb...`。

`W5-MIG09-CAPTE-R8B-AND-TRANSACTION-QUERY-CONSUMER-ARTIFACT-CLOSURE-GREEN-CLOSEOUT-001 / plan-r2.317` Maker 记录（2026-08-27）：Human Owner 授权上一唯一 documentation-only closeout。Maker 只修改主系分、主 TDD 与本 OpenSpec，不修改 Capte、Funding、`web-security`、Java、测试、schema、POM 或 build，也未运行 Maven/Git/联网。closeout 汇总累计 10 文件 Green、`45/45`、default=`149/0F/0E/3S`、三阶段各 `1/0F/0E/0S`、七制品 equality、相关 PMD=`0/0`、全仓无关 baseline=`4/6`、tests PMD=N/A 与资金零副作用。

Funding 恢复检查确认 plan-r2.301 四个 Provider 输入 SHA=`76ae32ac... / 3d3a4706... / 302b2f57... / 460b42c5...` 未漂移，RED file 仍 absent；Consumer closure 合法修改的 active read-only production `CouponActivityServiceImpl.java` 与 future-write test `CouponActivityServiceImplTests.java` 已分别重冻为 `8ad126aa... / d263b5fb...`，旧 `f46294bc... / 4d3e7239...` 不再是 active baseline。当前=`CONSUMER_ARTIFACT_CLOSURE_GREEN_CLOSEOUT_MAKER_COMPLETE / INDEPENDENT_CHECKER_PENDING / FUNDING_RED_EXECUTION_GRANT_NO / CODE_FREEZE`，不预写 `CURRENT_SCOPE_COMPLETE` 或 Funding RED Grant。

`plan-r2.318` Consumer/Artifact Closure 最终状态回写（2026-08-27）：独立 closeout Checker 首轮=`NOT PASS / P0=0 / P1=1 / P2=0`，指出 active Funding read-only production `CouponActivityServiceImpl` 仍为历史 SHA。Maker 只在三文档内把 production/test active inputs 重冻为 `8ad126aa... / d263b5fb...`，adapter=`d068ae90...` 未漂移；最终 recheck=`PASS / P0=0 / P1=0 / P2=0`。本 Consumer closure 以 `GREEN_CLOSEOUT_INDEPENDENT_CHECKER_PASS / CURRENT_SCOPE_COMPLETE` 关闭，不外推真实部署、D5、Git、发布或生产。

下一 Human Gate=`MIG09_FUNDING_ACCOUNT_QUERY_SURFACE_NARROWING_RED_EXECUTION_GRANT`，仅允许新增 `tests/src/test/java/com/wind/funds/wallet/services/impl/FundingAccountServiceTenantIsolationTests.java`，以 Java 21 offline fresh=`3/2F/0E/0S` 并进入独立 RED Checker。Provider Green 四文件、Capte Consumer、Credit/其他 MIG-09、Git、联网、发布和生产均未授权。

`W5-MIG09-FUNDING-ACCOUNT-QUERY-SURFACE-NARROWING-RED-EXECUTION-001 / plan-r2.319` RED 执行与独立 Checker 记录（2026-08-27）：Human Owner 授权唯一 RED 文件。Maker 新增 `FundingAccountServiceTenantIsolationTests.java`，SHA=`4a2373bd...`；四个 Green 输入 SHA 未漂移。写前/后 compile=`21/21`，最终 fresh XML=`3639b992... / 3/2F/0E/0S`，只失败 contract/annotation 与 missing tenant，两租户正向/foreign 隔离通过，全部资金/账务事实不变。父 site descriptor/`~/.m2` 写锁和一次 testCompile checked-exception 均按 Harness 诊断处理，未冒充 RED；最终离线跳过的只有 Surefire HTML report，不是 JUnit/XML。

独立 RED Checker=`PASS / P0=0 / P1=0 / P2=0`，确认真实 Spring/H2/Ledger 装配、失败映射、不泄露、零副作用和未来 Green 编译边界。RED 文件 immutable。下一 Human Gate=`MIG09_FUNDING_ACCOUNT_QUERY_SURFACE_NARROWING_GREEN_EXECUTION_GRANT`，仅允许 `FundingAccountService.java`、`FundingAccountQuery.java`、`FundingAccountServiceImpl.java`、`FundingAccountServiceImplTests.java` 四个 MODIFY；Capte Consumer、Core/Credit/Platform/Hierarchy、其他 raw-id、Git、联网、发布和生产继续 `NO`。

`W5-MIG09-FUNDING-ACCOUNT-QUERY-SURFACE-NARROWING-PROVIDER-GREEN-CLOSEOUT-001 / plan-r2.320` Maker 记录（2026-08-27）：Human Owner 授权 Funding Provider Green 后的三文档机械 closeout。Provider Green 严格保持 `3 production + 1 existing test MODIFY / ADD=0 / DELETE=0`：删除两个无 tenant getter，将 `FundingAccountQuery.tenantId` 声明为 Jakarta `@NotNull`，在 Service 首次 read 前显式拒绝 null tenant，并把旧测试两处 by-id 读机械迁移为 tenant+sn。最终 SHA=`f0711e17... / 87aaa1b8... / 320347d4... / 0b084d77...`，immutable RED=`4a2373bd...`、Green XML=`87216d28... / 3/0F/0E/0S`。

扩大证据为 compile=`21/21`，focused=`33/0F/0E/0S`，balance-control/transaction/business-flow/reconciliation/boundary=`44/0 / 186/0 / 200/0 / 247/0 / 211/0`，Public Contract=`299/175/42`，Core API=`94/4/4/1025`，13 份 PMD XML 无 finding，CAD PASS，121 份 Surefire XML=`1206/0F/0E/1S`；唯一 skip 仍是无真实 MySQL host 的 migration integration。独立技术 Checker=`PASS / P0=0 / P1=0 / P2=0`，独立代码 Review=`Ready`。价值是把 tenant 从 Consumer 自律上收为 Provider 对象授权不变量，阻止用 DB id、`FundsAccountId` 或 accountSn 替代授权并泄露 owner/state/currency/profile/capability/账务事实；不新增业务行为、兼容层或抽象。本证据只准出 current checkout `RUNTIME_D4 / ENGINEERING_READY`，不上推真实 MySQL、HOST/L4、发布或生产 D5。

本 Maker 只修改主系分、主 TDD 和本 OpenSpec，没有修改 Java、测试、Capte、schema、POM 或 build，也没有执行 Maven、Git、联网、发布或生产。独立 closeout Checker 以 Maker 三文档 SHA=`c1277616... / 6e4242bb... / 73e18476...` 复核权威 delta、Green 证据、tenant 授权价值、D4 边界、单一恢复入口与下一 Gate，结论=`PASS / P0=0 / P1=0 / P2=0`。当前=`PROVIDER_GREEN_CLOSEOUT_INDEPENDENT_CHECKER_PASS / PROVIDER_CURRENT_SCOPE_COMPLETE / CAPTE_CONSUMER_EXECUTION_GRANT_NO / CODE_FREEZE`。下一 Human Gate 只能是 `MIG09_FUNDING_ACCOUNT_QUERY_SURFACE_NARROWING_CAPTE_CONSUMER_EXECUTION_GRANT`，只允许 `CouponActivityServiceImplTests.java` 删除两个旧 override，并建立 exact Provider artifact -> Capte resolved artifact 谱系与 `37/49/25` 测试证据；Credit 与其他 MIG-09 继续冻结。

`W5-MIG09-FUNDING-ACCOUNT-QUERY-SURFACE-NARROWING-CAPTE-CONSUMER-GREEN-CLOSEOUT-001 / plan-r2.321` Maker 记录（2026-08-27）：Human Owner 授权对 Funding Capte Consumer Green 做三文档机械 closeout。Consumer 唯一修改为 `CouponActivityServiceImplTests.java`，SHA=`d263b5fb... -> abee9390...`，只删除两个旧 Funding getter override；37 个测试、tenant+sn/query、Credit Bean 和全部业务断言不变，Capte Java 旧 Funding getter 引用为 0。

Provider 674 个 POM/production 输入指纹=`07f11958917046275728de48c1968858a36db5c2556d9f4431dd44cce784f7a8`，唯一版本=`1.0.1-mig09-funding-20260827-07f119589170`，在隔离仓离线 flatten/install=`21/21`。受控 RED 真实编译 169 个测试源并仅命中两个旧 `@Override`；Green test compilation=`42/42`，Activity/Redemption/Integration=`37/0/0/0 + 49/0/0/0 + 25/0/0/3`，lineage method-only=`1/0/0/0`。七个 built/resolved SHA 成对相等，lineage 通过 `CodeSource` 证明 runtime loaded 来自同一版本。

价值是把 Provider 收窄、真实 Consumer source 与实际 loaded binary 闭合为一个可复核垂直切片：Capte 生产路径本已 tenant-scoped，删除的只是无调用测试 stub，不能再以测试编译为由恢复 Provider raw-id/无 tenant 旁路。唯一 artifact 谱系排除了 Snapshot、旧 target class 和 raw POM `${revision}` 造成的假兼容。`testCompile` 拼写错误、默认测试跳过和 raw POM 解析失败均只作 Harness 诊断，最终以 `test-compile`、显式 test flags、flatten install 和 fresh XML 闭合。独立代码/证据 Checker=`PASS / P0=0 / P1=0 / P2=0`；范围只到 library test host `D4/E4`，不上推 HOST/L4、真实 MySQL 或生产 D5。

本 Maker 只修改主系分、主 TDD 和本 OpenSpec，没有修改 wind-funds/Capte Java、测试、POM、schema 或 build，也没有执行 Maven、Git、联网、发布或生产。独立 closeout Checker 以 Maker 三文档 SHA=`c25ac83c... / 8e404ed0... / f7af9480...` 复核 Consumer/artifact/test 证据、Harness 分层、D4/E4 上限、唯一恢复入口与下一 Credit Gate，结论=`PASS / P0=0 / P1=0 / P2=0`。当前=`FUNDING_ACCOUNT_QUERY_SURFACE_GREEN_CLOSEOUT_INDEPENDENT_CHECKER_PASS / CURRENT_SCOPE_COMPLETE / CREDIT_ENTRY_CARD_GRANT_NO / CODE_FREEZE`；下一 Human Gate 仅为 `MIG09_CREDIT_ACCOUNT_QUERY_SURFACE_NARROWING_ENTRY_CARD_GRANT`，只允许三文档建立 Credit documentation-only Entry Card，不授权 RED、Java、测试、Consumer、Git、联网、发布或生产。

`W5-MIG09-CREDIT-ACCOUNT-QUERY-SURFACE-NARROWING-ENTRY-CARD-001 / plan-r2.322` Maker 记录（2026-08-27）：Human Owner 消费 Credit documentation-only Entry Card Grant。源码确认 `CreditAccountService` 的 `getCreditAccountById(Long)` 与 `getCreditAccount(FundsAccountId)` 均无生产 caller；wind-funds 只有 `ControlAccountLedgerInitializationTests` 两处 by-id 与 `FundsTransactionFlowTestSupport` 一处 FundsAccountId 直接调用，Capte 只有 `CouponActivityServiceImplTests` 两个匿名旧 override。正式 wind-funds 与 Capte production 均已使用显式 `tenantId + accountSn/query`，Fincone 两仓 Java=0。

目标无兼容删除两个旧 getter，保留 tenant+sn/query；`CreditAccountQuery.tenantId` 加 Jakarta `@NotNull`，Service 在首次 read 前显式拒绝 null tenant。Provider RED 只候选新增 `CreditAccountServiceTenantIsolationTests.java`，固定 `3/2F/0E/0S`；Provider Green 精确=`3 production + 2 existing wind test MODIFY`，production/test manifest=`2b4dc5e8... / c302a920...`；Capte 后续独立 Consumer 只允许当前 `CouponActivityServiceImplTests.java=abee9390...` 删除两个 Credit 旧 override。不得新增 replacement、compatibility、fallback、`InternalAccountRef`、Entity/Mapper/schema/POM/build 或其他 MIG-09 改动。

本卡的实际价值是把信用额度控制事实的 tenant 授权从调用方约定提升为 Provider 自身不变量，同时删除没有生产价值的公共旁路；不改变信用账户金额、四个 `CREDIT_BASIC` bucket、账期、Ledger 初始化或交易行为。当前只达到 `DESIGN_D1 / ENGINEERING_READY_WITH_RISK`，最后接受 wind 基线仍为 121 XML=`1206/0F/0E/1S`，未来新增 RED 后 Green 目标为 122 XML=`1209/0F/0E/1S`。本 Maker 只修改三份权威文档，未运行 Maven/Git/联网，也未修改 Java、测试、Capte、POM 或 schema。当前=`ENTRY_CARD_MAKER_COMPLETE / ENTRY_CARD_INDEPENDENT_CHECKER_PENDING / RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / CAPTE_CONSUMER_EXECUTION_GRANT_NO / CODE_FREEZE`。

独立 Entry Card Checker 初审=`NOT PASS / P0=0 / P1=0 / P2=1`，唯一 P2 是主系分 11.2 的现行 MIG-09 汇总仍保留上一轮 `CREDIT_ENTRY_CARD_GRANT_NO`；OpenSpec current state 和恢复入口本身正确，未形成第二执行授权。Maker 仅机械校准该汇总行，系分/TDD/OpenSpec Checker 输入 SHA=`e0717124... / 772dbe81... / e4ef2f43...`，最终 recheck=`PASS / P0=0 / P1=0 / P2=0`。当前=`ENTRY_CARD_INDEPENDENT_CHECKER_PASS / RED_EXECUTION_GRANT_NO / GREEN_EXECUTION_GRANT_NO / CAPTE_CONSUMER_EXECUTION_GRANT_NO / CODE_FREEZE`；下一 Human Gate=`MIG09_CREDIT_ACCOUNT_QUERY_SURFACE_NARROWING_RED_EXECUTION_GRANT`，只允许新增冻结的 Credit RED 文件。

`W5-MIG09-CREDIT-ACCOUNT-QUERY-SURFACE-NARROWING-RED-EXECUTION-001 / plan-r2.323` RED 记录（2026-08-27）：Human Owner 已消费单文件 RED Grant。唯一新增 `CreditAccountServiceTenantIsolationTests.java`，SHA=`8c2fe2e6ecad58678afbe09e8728898ad74e5425a3d13c6c2a725963d97c2585`；写前/后 Java 21 compile=`21/21`。Fresh XML mtime=`2026-08-27 15:12:55 +0800`、SHA=`74ea8ddbf2610cfff556f61fc325ff7f99c7c702e8ef3c0c8ac86dbb0d43a98d`，结果=`3/2F/0E/0S`。

Public contract 失败只证明两个旧 getter 尚在且 query tenant 缺 Jakarta `@NotNull`；missing tenant 失败只证明 Service 尚未在首次 read 前拒绝 null，事实快照断言通过；tenant-scoped 同租户/foreign 场景通过。测试真实装配 Spring/H2、Credit、Ledger/Profile，并为两租户各创建 `LIMIT/AVAILABLE/AUTHORIZATION/OUTSTANDING` 四个 required Ledger；快照覆盖 CreditAccount、FundsTransaction/Detail、Ledger、LedgerTransaction、PostingPlan、Entry，异常负断言覆盖 owner/description/context/state/currency/type/period/profile/foreign tenant。无 Mock/Fake、私有实现探针或非目标 error/skip。

独立 RED Checker=`PASS / P0=0 / P1=0 / P2=0`，该测试自此 immutable。价值是把风险精准收敛为“删除两个无 tenant Public 旁路 + query 首次 read 前 guard”，同时证明既有 tenant+sn/query 无需重做。当前=`RED_EXECUTION_COMPLETE / RED_INDEPENDENT_CHECKER_PASS / RED_TEST_IMMUTABLE / GREEN_EXECUTION_GRANT_NO / CAPTE_CONSUMER_EXECUTION_GRANT_NO / CODE_FREEZE`；下一 Human Gate=`MIG09_CREDIT_ACCOUNT_QUERY_SURFACE_NARROWING_GREEN_EXECUTION_GRANT`，只允许 plan-r2.322 冻结的 `3 production + 2 existing test MODIFY / ADD=0 / DELETE=0`，不授权 Capte Consumer、Git、联网、发布或生产。

## 13. 状态回写与恢复入口

每轮结束只更新本文件的以下 delta：

- 当前 `state_revision / Plan state / current task / next entry` 和所在 Wave/Task。
- 新增或更新的同一 Decision 记录。
- 已确认、被排除、待确认、conflict 和 red_lines。
- 新证据的等级、版本/hash、dirty fingerprint 和路径。
- 正式文档的当前版本、Checker 结论和下一 Owner。
- 验证命令、结果、失败原因和残余风险。

恢复时只以本文件的 `state_revision + accepted decisions + authority refs/fingerprints + current entry` 为持久执行依据，再读取当前 Task 需要的最新正式文档和一手证据。runtime Goal 的 `paused/blocked/archived/unavailable` 只作历史 provenance，不影响恢复，也不再绑定新的 runtime Goal。只有目标、权限、承重事实、Owner 决策、写入范围、模板版本和验证入口全部 `aligned` 才继续一个原子轮次。

当前恢复入口：`W5-MIG09-CREDIT-ACCOUNT-QUERY-SURFACE-NARROWING-RED-EXECUTION-001 / RED_INDEPENDENT_CHECKER_PASS / RED_TEST_IMMUTABLE / GREEN_EXECUTION_GRANT_NO / CODE_FREEZE / plan-r2.323`。下一 Human Gate=`MIG09_CREDIT_ACCOUNT_QUERY_SURFACE_NARROWING_GREEN_EXECUTION_GRANT`，只允许 `CreditAccountService.java`、`CreditAccountQuery.java`、`CreditAccountServiceImpl.java`、`ControlAccountLedgerInitializationTests.java`、`FundsTransactionFlowTestSupport.java` 五个 `MODIFY`；immutable RED、Capte Consumer、Core/Platform/Hierarchy、`web-security`、其他 MIG-09、兼容、Git、联网、远端发布、部署和生产均未授权。
