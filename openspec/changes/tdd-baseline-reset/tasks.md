# TDD 基线重置 Harness Plan

## 0. 当前状态

- [x] 作废旧 OpenSpec specs。
- [x] 作废旧 OpenSpec changes。
- [x] 删除旧测试源码。
- [x] 保留测试 resources。
- [x] 重建 OpenSpec 项目上下文。
- [x] 重建支付资金底座开发基线规格。
- [x] 校准 TDD 文档中“旧测试资产复用”表述。
- [x] 冻结当前设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果，作为进入编码前的独立检查点；上一已冻结设计交付提交点为 30b1a00 docs: 冻结权益快照设计基线，上一冻结点为 620b5a5 docs: 精简并加固资金底座设计交付文档。
- [x] 完成设计、代码、任务三方基线对齐：`eabd672` 纳入编码准备包，`6be9c99` 完成 B1-10 权益快照 DSL 契约承载，`75b46ef` 和 `9db3eba` 收敛请求摘要与稳定摘要支撑，`434f8a2` 校准账务计划装配器长 ID 测试规范，`5bad4b7` 对齐权益资金流编码基线，`42100dc` 固化权益生产准入门禁，`555a943` 至 `db766fa` 完成权益准入任务基线、架构师工程约规、CAD 自动推进约规、测试最佳实践、预算组默认周期 `LIFETIME`、账务事实断言、钱包/交易/治理边界、CAD 完整验证门禁和出款准入规则核验证据收敛，`f386ed0a` 至 `5901265` 完成编码准入设计冻结、出款准入设计冻结、提现/解冻红线、余额日志证据、路由事实边界、交易投影解释、权益回放摘要和治理重放范围/来源/差异项校验收敛，`5901265..77bc9f4` 完成资金事实红线、敏感上下文阻断、上下文不可变、防御性拷贝、外部账户原文阻断、MVP/归档边界和设计交付口径收敛；任务以本 Harness Plan 和 OpenSpec spec 为准。

## 1. MVP 任务写入范围

后续开发只能按 MVP 任务切片写入，不能按整章目标态或整张任务表一次性授权。任务开始前必须先写清 `mvpScenario`、覆盖 AC/DSL/系分/TDD/RED、写入范围、禁止范围、资金不变量、验证命令和停止条件。

当前执行优先级按资金底座能力划分：P0 先保障钱包、账本、账目、余额投影、对账、清分、清算、结算、大数据归档和账本余额快照；P1 再扩展直接交易、授权交易、余额控制、交易投影和交易投影重投影；P2 业务模式能力包按专项授权接入。B1 至 B8 只保留为 TDD 覆盖索引，不表示 03、04 能力低于 02，也不表示任何索引可以绕过独立 Execution Grant。

| 覆盖索引 | 能力优先级 | 可写入范围的上限 |
| --- | --- | --- |
| B1 | P0 共享承载 | `tests/src/test/java/com/capte/funds/dsl`、`tests/src/test/resources/dsl-contract-cases`、必要的 `core/src/main/java` DSL/枚举/Spec、Route DSL、PaymentInstrument DSL、RoutingDecision/FundingAllocation DSL、Posting/Ledger DSL、SettlementPolicy。 |
| B2 | P0 基础事实 | `wallet-*`、`ledger-*`、`tests/src/test/java` 中账户、支付工具、绑定关系、支出主体资金来源关系、账本、投影相关测试和最小实现。 |
| B3 | P1 直接交易 | `transaction-*`、`tests/src/test/java` 中直接交易测试和最小实现。 |
| B4 | P1 授权交易 | `transaction-*`、`core`、`tests/src/test/java` 中授权交易、无授权直接退款测试和最小实现。 |
| B5 | P1 余额控制 | `transaction-*`、`tests/src/test/java` 中余额控制测试和最小实现。 |
| B6 | P1 路由回放与交易投影 | `transaction-*`、`ledger-*`、`tests/src/test/java` 中 Route Replay、余额日志、交易投影测试和最小实现。 |
| B7 | P0 运营账务闭环 | 清结算、对账相关模块或包，需先确认；冻结基线已有 `reconciliation-*` 模块骨架和出款前准入候选实现，已完成专项验证，但未纳入清结算、对账或出款生命周期 Done 基线，目标态对象、表、状态机和测试未闭环。 |
| B8 | P0/P1 治理闭环 | 归档、余额重建、账本余额快照、交易投影重放相关模块或包，需先确认；当前已有 `governance-*` 交易投影重放骨架，但归档、余额快照和指标水位隔离未闭环；指标仅保留普通指标快照边界测试。进入编码前必须在 Execution Grant 中确认治理物理落点、依赖方向、是否新增公共契约、DDL/H2 schema、边界测试和指标水位隔离测试。 |

## 2. 全局只读范围

| 路径 | 用途 |
| --- | --- |
| `docs/产品设计` | 产品目标、流程、状态、验收和风险边界。 |
| `docs/DSL设计` | DSL 语义、JSON 契约和场景矩阵。 |
| `docs/系分设计` | 模块边界、服务契约、表设计、观测和金融红线。 |
| `docs/TDD设计` | 测试用例、测试分层和红线。 |
| `openspec/specs/payment-funds-foundation/spec.md` | 当前开发规格基线。 |
| `tests/src/test/resources`、`core/src/test/resources` | 测试配置、schema 和数据资源。 |

## 3. 全局禁止事项

1. 不恢复旧测试源码或历史过渡断言。
2. 不把测试通过建立在旧 OpenSpec 或旧 Harness 计划上。
3. 不绕过 TDD 直接大范围改生产代码。
4. 不把清结算、对账、归档、指标实现混入交易、钱包、账本主链路。
5. 不引入真实外部调用、生产配置、敏感数据或无主依赖。
6. 不在缺少人工确认时修改公共契约、枚举、表结构和状态机。
7. 不在设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果未冻结时进入生产代码实现。

## 4. 每个 MVP 任务基础验证命令

后续每个 MVP 编码任务默认先执行基础验证，再执行任务表中的专项验证命令：

```bash
just mvn-version
just compile
```

如因 Java runtime、私有 Maven 仓库、网络、凭据或本地缓存导致无法执行，交付记录必须区分环境问题与代码问题。

进入 CAD 自动模式或需要声明完整本地基线复核时，优先执行：

```bash
just verify-cad
```

`verify-cad` 聚合运行时确认、编译、核心契约、账本、交易、余额控制、业务流、架构边界、治理重放和 PMD。仅修改文档或说明性材料时可不执行完整门禁，但交付记录必须说明原因。

## 5. 基线对齐结论

- [x] OpenSpec 已重新对齐：以 `project.md` 和 `payment-funds-foundation/spec.md` 作为当前目标态规格，不再引用历史 specs 或历史 changes。
- [x] Superpowers 已重新对齐：以 TDD、增量、可回退、先写用例再改实现为执行纪律，不复用旧测试源码和旧断言。
- [x] Harness 已重新对齐：以本文件作为 MVP 任务计划、覆盖索引、写入范围、只读范围、禁止事项、人工确认点和交付记录入口。
- [x] 旧测试源码已移除，测试 resources 保留。
- [x] 当前基线支持在用户明确授权后进入 CAD 自动提交模式；进入生产编码前仍需用户按 MVP 任务切片授予 Execution Grant，且每轮交付必须声明专项验证或 `just verify-cad` 完整门禁结果。
- [x] 已冻结设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果已作为独立检查点冻结；上一设计冻结点为 30b1a00 docs: 冻结权益快照设计基线。当前设计和任务基线提交点为 `9456ab6 docs: 对齐 A0 准入与代码基线`，代码能力基线截至 `77bc9f4 fix: 阻断钱包上下文权益核心事实`；后续生产编码仍需用户按 MVP 任务切片单独授予 Execution Grant。
- [x] 代码基线已复核至当前 Git 基线 `77bc9f4`，并纳入 B1-10 权益快照 DSL 契约承载实现和测试：B1 覆盖索引的 DSL 契约测试已存在；B2 覆盖索引下支付工具、绑定历史、资金来源关系、显式建账、预算组默认周期 `LIFETIME`、账务计划装配器长 ID 追溯、余额日志证据、余额投影、上下文敏感字段阻断和防御性拷贝已有局部基线；B3 至 B6 覆盖索引已有部分直接交易、授权、余额控制、Route Replay、交易投影、稳定摘要、解释摘要、权益回放摘要、路由事实边界、主链路事实红线、敏感上下文阻断和投影重放差异校验测试；B7 已有 reconciliation-* 模块骨架和出款前准入候选实现，已通过专项服务测试验证，可作为后续候选输入但未纳入清结算、对账或出款生命周期 Done 基线；B8 已有 governance-* 交易投影重放骨架、范围/来源事实/差异项校验和局部边界测试。上述都只作为局部代码和验证门禁基线，不表示对应覆盖索引全量完成或可跳过 Execution Grant。
- [x] 完成 A0 只读核验证据登记：Java 21 运行时、编译门禁、A1 直接交易主链路、posting 装配和余额投影回归测试已通过；embedded Redis 由测试基础设施自动启动和使用，未触发额外人工确认。A0 通过只作为下一步单一 MVP Execution Grant 输入，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入。
- [x] 完成 CAD 完整门禁验证登记：`WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-cad` 已通过，覆盖 Java 运行时、编译、核心契约、账本、交易、余额控制、业务流、架构边界、治理重放和 PMD；该结果仍只作为下一步 Execution Grant 输入，不替代生产 Done。
- [x] 补齐 A1 直接交易事实红线候选准入卡：`docs/TDD设计/A1-直接交易事实红线准入卡.md` 已整理候选授权、覆盖验收、写入边界、Red 集合、验证命令和停止条件；该卡仍需用户确认后才成为实际 Execution Grant。
- [x] 完成 A1 现有覆盖扫描登记：`52f116f docs: 记录 A1 现有覆盖扫描` 已记录 `A1-RED-001`、`A1-RED-002` 既有回归覆盖和 `FundsDirectTransactionFlowTests` 22 个用例通过证据；该结果只作为 Execution Grant 输入，不替代生产 Done。
- [x] 完成 A1 证据链同步后完整门禁复核：`97bc25d docs: 同步 A1 覆盖扫描证据` 后执行 `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-cad` 并通过，覆盖编译、测试和 PMD；该结果只证明当前候选准入证据链与工程基线一致，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入。

### 5.1 设计、代码、任务对齐矩阵

| 设计域 | 代码现状 | 任务基线 |
| --- | --- | --- |
| `02-交易路由钱包账目与投影` | DSL 契约、支付工具、钱包账户、部分直接交易、授权、余额控制、Route Replay、余额投影和交易投影已有局部测试与实现。 | 按 P0/P1 拆解补齐覆盖索引：B1、B2 对应 DSL、钱包、账本、账目和余额投影；B3 至 B6 对应直接交易、授权交易、余额控制和交易投影。已存在能力只作为局部基线，进入编码时必须再裁剪成 A0 至 A4 或等价 MVP 任务，按 TDD 证明目标 AC/DSL/RED。 |
| `03-清结算与对账` | 冻结基线中 `reconciliation-face`、`reconciliation-impl` 已有模块骨架和出款前准入候选实现，已覆盖准入解释状态、完整外部规则核验证据和只读无账务事实断言，但仍缺完整出款生命周期、表结构和数据库级服务闭环。 | 清结算与对账继续编码前必须另起独立 OpenSpec change，并确认模块、表、状态机、接口、是否扩展出款前准入候选能力和验证命令。 |
| `04-归档重放与指标治理` | `governance-face`、`governance-impl` 已有交易投影重放骨架和局部边界测试。 | B8 是 P0/P1 治理闭环覆盖索引，不因编号排在 B7 后而降低优先级；归档 Manifest、账本余额快照、普通指标快照、交易投影重放和水位隔离仍需独立 Execution Grant，并以已确认事实边界为输入。 |
| 导出附件 | 若工作树存在 `docs/*.zip` 等导出包，只能作为评审附件。 | 导出包不作为规格、任务或验收 Source of Truth；是否纳入版本库需用户单独确认。 |

### 5.2 生产交付判定

本 Harness Plan 的任务结论只表示设计、覆盖索引和 MVP 任务规则已经对齐，不表示生产交付已经完成。后续每个 MVP 任务必须在交付记录中单独说明 Done 证据。

| 覆盖索引 | 当前判定 | Done 证据要求 |
| --- | --- | --- |
| B1/B2 覆盖索引 | P0 共享承载和基础事实可按单一 MVP 任务 Execution Grant 进入编码。 | 对应 DSL、钱包、账本、账目、余额投影 AC/DSL/TDD/RED 映射闭合，相关测试通过，资金变化断言覆盖主体、账目、posting、entry、projection、幂等和审计。 |
| B3 至 B6 覆盖索引 | P1 交易入口、余额控制、路由回放和交易投影可按单一 MVP 任务 Execution Grant 进入编码。 | 对应直接交易、授权交易、余额控制、Route Replay、交易投影和权益资金流 AC/DSL/TDD/RED 映射闭合，相关测试通过，资金变化断言覆盖状态、route、posting、entry、projection、幂等和审计；含权益任务还需覆盖使用者解释视图、证据最小化和外部规则核验状态。 |
| B7 | P0 清结算与对账只保留边界、计划和设计评审输入；整体编码准入未打开，出款前准入候选实现只作为差距复核输入。 | `CLS-GATE-*`、独立 OpenSpec change、模块/表/接口确认、DDL/H2、服务级 H2 测试、对账阻断和清结算幂等测试通过。 |
| B8 | P0/P1 归档重放与指标边界只保留边界、计划和设计评审输入；交易投影重放只是局部基线，编码准入未打开。 | `GOV-GATE-*`、Manifest、checkpoint、watermark、余额快照、指标水位隔离、范围锁、差异报告、异常人工处理、大数据归档承接边界、回滚/续跑测试通过。 |

### 5.3 MVP 任务交付证据包

后续每个 MVP 任务开始和完成时，分别补齐申请卡和验收记录。字段不允许留空；不适用时必须写 `N/A + 原因`。字段留空、用“待补”占位或无法说明不适用原因时，视为该任务仍未达到 Done。

任务开始前先填写申请卡：

```text
任务切片：
mvpScenario：
设计基线提交点：
申请目标：
设计域：
覆盖产品验收：
覆盖 DSL caseId：
覆盖 TDD 用例：
覆盖红线：
写入范围：
只读范围：
非目标：
允许修改公共契约：是/否
公共契约允许修改范围：
允许新增枚举或事件：是/否
允许新增服务入口：是/否
允许扩展 Request/Query/DTO：是/否
允许修改状态机：是/否
允许修改表结构：是/否
允许新增模块：是/否
是否影响架构 ADR：是/否
受影响 ADR：
是否触碰能力域边界：是/否
是否触碰事实端口层：是/否
架构边界测试范围：
治理物理落点：复用 governance-face/governance-impl / 扩展既有包 / 新增独立模块 / 不涉及；依赖方向 / 公共契约 / 表结构 / H2 schema / Mapper Entity 归属 / 边界测试 / 指标水位隔离测试：
运营补事实命令白名单：允许命令或等价事件 / 不允许 / 不涉及；来源单据、审批号、证据引用、幂等键、原事实引用、操作者、原因和可撤销边界：
退款分摊确定性规则：规则版本 / 分摊依据 / 稳定组件顺序 / 舍入模式 / 尾差归属 / 组件剩余额度版本 / 幂等摘要字段 / 并发保护 / 不涉及
先写或先恢复的测试：
验证命令：
人工确认点：
停止条件：
工作树状态：
允许纳入本任务的未提交变更：
必须排除的未提交变更：
NFR 假设：
观测告警：
回滚或补偿：
```

任务完成时再填写验收记录：

```text
任务切片：
mvpScenario：
Execution Grant：
覆盖产品验收：
覆盖 DSL caseId：
覆盖 TDD 用例：
覆盖红线：
修改范围：
公共契约变化：
DDL/H2 schema 变化：
服务契约证据：
状态机和幂等证据：
资金断言证据：
运营补事实白名单证据：
治理物理落点和 DDL/H2 证据：
观测和审计证据：
架构边界证据：
外部规则和待确认项：
验证命令：
验证结果：
未覆盖项：
残余风险：
完成结论：Done / Conditional Done / Not Done
```

设计准入评审可以使用“通过、带条件通过、阻断”；MVP 编码任务验收只能使用 `Done / Conditional Done / Not Done`。只有文档、设计或计划闭合时不得写 Done；本任务生产目标必需的 DDL/H2、服务级测试、资金断言、幂等或审计证据缺失时必须写 Not Done。

### 5.4 当前编码准入裁决

| 检查点 | 裁决 | 处理口径 |
| --- | --- | --- |
| CAD Round 0/A0 基线与验证证据 | 已提交为下一轮编码准备输入。 | 后续 Execution Grant 必须引用已提交设计基线和 A0 只读核验证据；若产生新的未提交设计变更，必须先提交或明确列为本任务基线附件。 |
| A0 至 A4 | P0/P1 条件准入。 | 仅在 Execution Grant 写清 `mvpScenario`、AC/DSL/系分/TDD/RED、写入范围、测试和验证命令后逐任务进入；不得一次性授权整个 02 目标态。B1 至 B6 只作为覆盖索引。 |
| B7 | P0 未准入。 | 出款前准入候选实现只能作为差距复核输入；清结算、对账和出款生命周期必须独立 OpenSpec change 后再授权，并确认 `CLS-GATE-*`、首批 Red、模块/表/接口、DDL/H2 schema、服务级测试、运营补事实白名单和职责分离边界。 |
| B8 | P0/P1 未准入。 | governance 交易投影重放局部基线不能替代归档、余额快照、差异报告、异常人工处理和指标水位隔离；进入编码前必须另补 Execution Grant，并确认 `GOV-GATE-*`、治理物理落点、依赖方向、DDL/H2 schema、边界测试、指标水位隔离测试和大数据归档承接边界。 |
| P2 业务能力包 | 不开放默认编码准入。 | VCC、全球账户和收单必须按业务专项 PRD、DSL 准入卡、系分承接卡和 `TDD-P2-*` 用例另起 Execution Grant；VCC 预付/共享卡还必须声明 `PaymentInstrument`、绑定快照、资金来源关系、禁止新增账户/账本主体和 P0/P1 回归范围；授权前只能做设计、契约草案或 contract-only 验证，不得把业务 pack 当作 P0/P1 默认实现范围。 |
| 本轮生产代码、测试代码、DDL/H2 schema 和运行时配置 | 不授权写入。 | 本轮只做设计和任务基线优化；编码开始前另行确认 Execution Grant、Git 策略、人工确认点和停止条件。 |

### 5.5 编码阶段 Execution Grant 对齐模板

进入编码阶段时，Execution Grant 必须作为 PRD、DSL、系分、TDD、OpenSpec、任务基线和当前 Git 状态之间的唯一写入授权单。缺少任一必填项时，本 change 只能作为 TDD 分析和设计基线，不得写生产代码、测试代码、DDL/H2 schema 或运行时配置。

| 必填项 | 填写要求 | 缺失时裁决 |
| --- | --- | --- |
| MVP 场景和任务切片 | 指明 `mvpScenario`、A0 至 A4、B7、B8 或 P2 业务专项；B1 至 B8 细项只作为覆盖索引，不能一次性授权整个目标态。 | Not Done。 |
| 业务驱动准入页 | 写清 `productGoal`、`businessQuestion`、`moneyFact`、使用者可见结果、成功/失败终态、产品验收 ID、DSL caseId、系分章节和产品不做范围。 | 回到 PRD 或 A0，不进入 Red 写入。 |
| 基线引用 | 引用 PRD、DSL、系分、TDD、OpenSpec、Harness Plan、任务文档和 Git 提交点；未提交变更必须列为本任务基线附件。 | 不得把草稿当冻结基线。 |
| 验收与红线 | 列出 AC-*、RED-*、DSL caseId、TDD-*、首批 Red 和 Not Done 条件；B7 必填 `CLS-GATE-*`，B8 必填 `GOV-GATE-*`。 | 只能继续 TDD 分析。 |
| 写入范围 | 列出允许修改的模块、包、公共契约、枚举、Request/Query/DTO、状态机、表结构、H2 schema、fixture、生产代码和测试目录。 | 不得写入未列范围。 |
| 禁止范围 | 列出不得触碰的模块、接口、表、运行配置、外部协议、历史事实和用户未提交变更。 | 越界即停止。 |
| 首批 Red | 指定先失败的测试名称、断言事实、夹具等级、目标验证命令和失败无副作用检查。 | 不得进入 Green 实现。 |
| 金融红线 | 明确金额、幂等、余额桶、posting 平衡、ledger transaction 可追溯、审计、权限、外部规则核验和专业确认状态。 | 只能做契约或设计验证。 |
| 验证命令 | 指定最小目标测试、边界测试、PMD/compile/verify-cad 取舍，以及无法执行时的环境限制说明。 | 不得声明 Done。 |
| 停止条件 | 明确公共契约冲突、表结构冲突、资金口径不一致、外部规则未确认、测试无法闭合、权限不足和工作树冲突时的暂停规则。 | 触发即停止并回到 CR。 |

字段化授权建议：

| 字段 | 承接内容 |
| --- | --- |
| `mvpScenario` | 本任务服务的使用者、资金事实、最小闭环和不得声明的扩展能力。 |
| `businessAdmission` | 产品目标、业务问题、资金事实、使用者可见结果、成功/失败终态、验收 ID、DSL caseId、系分章节和明确不做范围。 |
| `abilityBatch` | A0 至 A4、B7、B8 或 P2 业务专项、能力域、AC/DSL/系分/TDD/RED 和 Not Done 条件。 |
| `authorityBaseline` | PRD、DSL、系分、TDD、OpenSpec、Harness Plan、Git 提交点、工作树状态和允许读取文件。 |
| `writeScope` | 允许写入的生产代码、测试代码、公共契约、枚举、Request/Query/DTO、状态机、表结构、H2 schema、fixture 和运行时配置。 |
| `noWriteScope` | 禁止写入范围、不得触碰的模块、对象、资金语义、外部协议、历史事实和用户未提交变更。 |
| `physicalLanding` | 模块、包、face/impl、端口、依赖方向、DTO、Entity、Mapper、DDL/H2 和边界测试范围。 |
| `firstRedSet` | 必须先失败的 Red、目标测试资产、失败断言、测试层级和验证命令。 |
| `moneyInvariant` | 主体、账目、币种、周期、route、posting、entry、projection、余额、幂等、失败无副作用和审计证据。 |
| `operationGovernanceGate` | 清结算、对账、出款、差错、白名单、归档、Manifest、checkpoint、watermark、差异报告、人工处理和指标水位隔离。 |
| `externalRuleStatus` | 规则来源、版本或发布日期、生效日期、适用主体或范围、适用法域、核验日期、确认方和确认状态。 |
| `verificationAndStop` | 目标验证命令、边界测试、PMD/完整验证取舍、失败停止条件和 Not Done 判定。 |

## 6. 里程碑拆解

| 里程碑 | 目标 | 完成标志 | 阻塞关系 |
| --- | --- | --- | --- |
| M0 基线重置 | 作废历史基线、删除旧测试源码、保留 resources、重建 OpenSpec/Harness。 | 本文件第 0 节全部完成。 | 已完成。 |
| M1 P0 资金底座内核 | 先把钱包、账本、账目、余额投影、对账、清分、清算、结算、大数据归档和账本余额快照的不变量证明清楚。对应 B1、B2、B7、B8 的 P0 覆盖索引。 | P0 任务按各自 Execution Grant 通过，真实服务层测试、余额断言、账务平衡、清结算对账阻断、归档 Manifest、余额快照和失败隔离闭合。 | 优先级最高；可按独立授权分任务推进，不要求把 03/04 视为低优先级后置能力。 |
| M2 P1 交易与读模型扩展 | 在 P0 主体、账目、账本、余额和治理边界上，落直接交易、授权交易、余额控制、Route Replay、交易投影和权益资金流。对应 B3 至 B6 覆盖索引。 | P1 任务通过，交易入口具备状态、route、posting、entry、projection、幂等和审计断言；含权益场景补齐解释视图、证据最小化和规则核验断言。 | 依赖已确认的 P0 基础事实；不得反向改变 P0 资金口径。 |
| M3 P2 业务模式能力包 | 按专项 PRD/DSL/系分/TDD 支撑 VCC 发卡、全球账户收付款和收单业务。 | 业务专项能证明归一资金事实、外部引用脱敏、外部规则核验、轨道边界、`TDD-P2-*` 专项用例和 P0/P1 回归闭合，不把业务协议沉入资金内核。VCC 专项必须证明 VCC 卡/虚拟卡/卡 token 是 `PaymentInstrument`，prepaid virtual card 是资金模式，shared card 是绑定/使用模式，并回挂 `VCC-AC-007`、`VCC-AC-008`、`DSL-PAYMENT-INSTRUMENT-PREPAID-CARD-001`、`DSL-PAYMENT-INSTRUMENT-SHARED-CARD-001`、`TDD-RAIL-001A`、`TDD-P2-VCC-004` 至 `TDD-P2-VCC-011`。ACH 或银行转账仅作为上层业务或外部轨道输入，资金底座只承接归一资金事实、外部引用、对账差错、追偿、调账核销和审计；若专项使用 ACH 或银行转账，必须回挂 `ACH-BOUNDARY-001` 至 `ACH-BOUNDARY-006`、`AC-RAIL-002A` 至 `AC-RAIL-007`、`TDD-RAIL-002` 至 `TDD-RAIL-007`。 | 依赖 P0/P1 可复用能力和业务专项 Execution Grant；未授权前不得进入生产代码、测试代码、DDL/H2 schema 或运行配置写入。 |

## 7. 覆盖索引明细

本节保留 B1 至 B8 的 TDD 覆盖索引和历史任务编号，用于反查设计、测试和既有局部基线；它不是当前默认授权计划。进入编码前，仍必须从 A0 至 A4、B7、B8 或 P2 中选择一个 MVP 任务切片，并由 Execution Grant 明确 `mvpScenario`、写入范围和验证命令。

### B1 覆盖索引：DSL 契约与枚举红线

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B1-01 | 重建核心测试骨架和命名规范。 | `tests/src/test/java/com/capte/funds/dsl` | TDD 设计、DSL 设计。 | 测试目录为空时先建立最小失败样例。 | 建立按 DSL/枚举/JSON 分层的测试包，不迁回旧测试源码。 | `just test-core` |
| B1-02 | 固化 `FundsInstructionSpec` 语义。 | `tests/src/test/java/com/capte/funds/dsl`、必要的 `core/src/main/java` | DSL 语义结构、JSON 契约。 | 构造最小合法指令、缺字段、非法金额、非法主体用例。 | 补齐或校准 Spec 校验行为。 | `just test-core` |
| B1-03 | 证明 `transactionType` 不承载生命周期事件。 | `tests/src/test/java/com/capte/funds/dsl`、必要枚举。 | DSL 第九章、第十章。 | 将授权生命周期事件误塞入交易类型应失败。 | 保持交易类型与事件类型分离。 | `just test-core` |
| B1-04 | 固化授权 `eventType` 生命周期边界。 | `tests/src/test/java/com/capte/funds/dsl`、必要枚举。 | DSL 授权交易用例。 | `PENDING`、`SETTLE`、`REVERSAL`、`REFUND`、`EXPIRE` 的合法/非法组合，以及拒付不强制落独立 `CHARGEBACK` 事件。 | 必要时补 `EXPIRE` 枚举或先形成失败基线；拒付通过退款承接口径表达。 | `just test-core` |
| B1-05 | 重建 JSON 契约样例解析测试。 | `tests/src/test/java/com/capte/funds/dsl`、`tests/src/test/resources/dsl-contract-cases` | DSL JSON 契约用例。 | JSON 样例无法解析、字段含义不一致、枚举不匹配。 | 让 DSL 样例能推导业务流程和 TDD 断言。 | `just test-core` |
| B1-06 | 固化 Route DSL 和 Route Replay 类型契约。 | `tests/src/test/java/com/capte/funds/dsl`、必要的 `core/src/main/java` | DSL Route、Route Replay、不变量。 | 外部账户或工具进入 ledger node、缺快照 Route Replay 仍成功、route code 漂移。 | 补 `RouteDslContractTests` 或等价契约测试，明确 Route Replay 不等同于交易投影重放、余额重建或归档续跑。 | `just test-core` |
| B1-06A | 固化 PaymentInstrument Route DSL 契约。 | `tests/src/test/java/com/capte/funds/dsl`、必要的 `core/src/main/java` | `DSL-PAYMENT-INSTRUMENT-ROUTE-001`、`DSL-PAYMENT-INSTRUMENT-FAIL-001`、`DSL-PAYMENT-INSTRUMENT-REPLAY-001`。 | 支付工具、外部账户或通道 token 被写成 ledger subject；routing decision 缺资金来源、优先级或选择原因；敏感字段进入快照；工具换绑后按当前绑定重路由。 | 补 `PaymentInstrumentRouteDslContractTests` 或等价契约测试，固化 `PaymentInstrumentRef`、`ExternalAccountRef`、`RoutingDecision`、`FundingAllocationDecision` 和 route snapshot 不变量。 | `just test-core` |
| B1-07 | 固化 Posting/Ledger DSL 契约。 | `tests/src/test/java/com/capte/funds/dsl`、必要的 `core/src/main/java` | DSL Posting、LedgerEntry、账务不变量。 | posting plan 不平衡仍可构造、entry 缺主体/账目/币种/周期仍通过。 | 补 posting、entry、digest 和账本周期契约测试。 | `just test-core` |
| B1-08 | 固化 SettlementPolicy DSL 契约。 | `tests/src/test/java/com/capte/funds/dsl`、必要的 `core/src/main/java` | DSL SettlementPolicy、结算策略边界。 | 策略解析失败被静默按实时处理。 | 补 `SettlementPolicySpecTests` 或等价策略解析红线。 | `just test-core` |
| B1-09 | 固化金额临界值契约。 | `tests/src/test/java/com/capte/funds/dsl`、必要的 `core/src/main/java` | TDD 5.1 金额临界值通用矩阵。 | 0 金额、负金额、超币种精度、超系统上限或多 leg 合计不闭合仍可入账。 | 补 `FundsAmountBoundaryContractTests` 或等价契约测试，统一最小单位、精度、上限和累计闭合校验。 | `just test-core` |
| B1-10 | 固化权益快照 DSL 契约。 | `tests/src/test/java/com/capte/funds/dsl`、`tests/src/test/resources/dsl-contract-cases`、必要的 `core/src/main/java` | `FundsBenefitSnapshotSpec`、`DSL-BENEFIT-SNAPSHOT-001`、产品 `AC-BEN-001`、`AC-BEN-012`、`AC-BEN-013`、红线 `RED-050` 至 `RED-059`、`TDD-BEN-001` 至 `TDD-BEN-007`、`TDD-BEN-ENTRY-005`；新增准入项 `AC-BEN-014` 至 `AC-BEN-019`、`RED-060` 至 `RED-066`、`TDD-BEN-RED-020` 至 `TDD-BEN-RED-030` 只作为后续 Phase 2/3 门禁，不倒灌为 B1-10 已完成范围。 | 权益核心字段藏入 `contextVariables`、无权益交易空值语义不成立、金额不闭合、闭合角色缺失或混用、组件重复、稳定摘要未覆盖权益快照差异、用户侧不返券和资金侧不冲补贴无法分开表达、请求态快照被误判为生产 Done；B1-10 越权修改交易服务入口、Request/DTO、route/posting/replay 或主链路实现。 | 补 `FundsBenefitSnapshotSpec`、组件、闭合角色、引用、退款策略、退款决策来源字段、稳定摘要边界和权益枚举的契约测试；Phase 1 只证明 DSL 承载和请求摘要可区分，不宣称 route/posting/replay 已闭合，不改直接交易或授权交易服务入口；`RED-058` 只作为生产 Done 门禁登记；交付结论只能写“契约承载 Done”。后续 Phase 2/3 必须另起 Execution Grant，并补权益准入证明。 | `just test-core` |

### B2 覆盖索引：钱包账户、账本和余额投影基础

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B2-01 | 重建账户角色与建账规则。 | `wallet-*`、`ledger-*`、`tests/src/test/java` | 产品账户/钱包/账本概念、系分表设计。 | 资金账户、信用账户、预算组、平台账户角色缺失或混用。 | 校准账户创建、角色标识、缺账本失败行为。 | `just test-ledger` |
| B2-02 | 固化显式建账红线。 | `ledger-*`、`tests/src/test/java` | 账本与账本周期设计。 | 未建账本直接交易应失败。 | 保持建账动作可追溯，不隐式创建关键账本。 | `just test-ledger` |
| B2-03 | 固化账本周期和 posting 平衡。 | `ledger-*`、`tests/src/test/java` | 账本周期语义矩阵、账务规则矩阵。 | 周期混用、借贷不平、entry 字段缺失。 | 补齐周期隔离、posting plan 平衡和分录完整性。 | `just test-ledger` |
| B2-04 | 固化余额投影边界。 | `ledger-*`、`tests/src/test/java` | 余额投影、余额日志。 | 投影失败误回滚事实、余额日志被当事实源。 | 保持事实追加、投影可重建、日志只读辅助。 | `just test-boundary` |
| B2-05 | 建立余额断言支撑。 | `tests/src/test/java` | TDD 资金断言红线。 | 只断言状态、不断言余额桶。 | 补公共断言支撑，后续 MVP 任务复用。 | `just test-boundary` |
| B2-06 | 固化支付工具、绑定和资金来源关系。 | `wallet-*`、`tests/src/test/java` | 产品 `AC-PI-*`、系分支付工具状态与路由准入、`GAP-PI-001`。 | 工具状态不可用、方向不匹配、资金来源缺失或不唯一、账户能力不足仍进入 route；敏感信息未脱敏；绑定历史被覆盖。 | 补 `PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests` 或等价测试，证明工具只做引用、绑定只做候选、资金来源关系不扣款不写账；绑定当前态变更必须追加历史审计。 | `just test-boundary` |

### B3 覆盖索引：直接交易

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B3-01 | 重建直接交易指令转换测试。 | `transaction-*`、`tests/src/test/java` | DSL 直接交易、系分交易编排。 | 充值、付款、商户订单付款、转账、提现转换错误。 | 校准 converter、amount、subject、route 快照。 | `just test-transaction` |
| B3-02 | 固化主路径资金流。 | `transaction-*`、`tests/src/test/java` | 产品流程、账务规则矩阵。 | 资金流方向、账本余额桶、平台账户处理错误。 | 逐步骤断言交易、route、entry、余额。 | `just test-business-flow` |
| B3-03 | 固化退款、手续费、退费和原路径 replay。 | `transaction-*`、`tests/src/test/java` | DSL 场景账务规则矩阵。 | 退款超额、退费方向错误、replay 丢快照。 | 保持原路径、原费项和幂等 replay。 | `just test-transaction` |
| B3-04 | 固化异常与红线。 | `transaction-*`、`tests/src/test/java` | TDD 异常路径。 | 错币种、受控负余额、重复请求、缺主体。 | 补失败原因、幂等键和余额不变断言。 | `just test-business-flow` |
| B3-05 | 固化含权益直接交易资金流。 | `transaction-*`、`tests/src/test/java` | `DSL-BENEFIT-MERCHANT-DISCOUNT-001`、`DSL-BENEFIT-PLATFORM-SUBSIDY-001`、`DSL-BENEFIT-PLATFORM-NO-SETTLEMENT-001`、`DSL-BENEFIT-PREPAID-VOUCHER-001`、`DSL-BENEFIT-PARTIAL-REFUND-001`、`TDD-BEN-ENTRY-001`、`TDD-BEN-ENTRY-002`。 | 商户让利生成 ledger entry、平台补贴与本金净额混记、储值券按普通优惠券入账、退款按当前营销规则重算、权益累计超额、零实付选型未确认仍入主链路、同一业务流水不同权益快照复用幂等结果；为优惠券、代金券或补贴新增直接交易权益专用服务入口。 | 不新增直接交易权益专用服务入口；`pay` 承接含权益支付，可在对应 Execution Grant 确认后扩展可选 `benefitSnapshot`；含权益生产链路必须固化 route snapshot、交易事实快照、独立权益快照表或等价不可变存储之一；幂等摘要纳入权益快照稳定摘要；`refund` 承接含权益退款，优先引用原交易、原 route snapshot 和原权益快照；route/posting 只消费原权益快照；`NO_LEDGER` 只入展示和对账解释；平台补贴或储值券必须形成可追溯资金影响或被专业口径阻断。 | `just test-business-flow` |

### B4 覆盖索引：授权交易

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B4-01 | 重建授权批准与授权拒绝。 | `transaction-*`、`core`、`tests/src/test/java` | DSL 9.2 授权交易、VCC 场景。 | 授权批准不冻结、授权拒绝写 route/entry。 | 保持批准冻结、拒绝仅记录原因且无资金扣划。 | `just test-transaction` |
| B4-02 | 重建撤销、完成、退款和拒付承接。 | `transaction-*`、`tests/src/test/java` | 授权组合场景、账务矩阵。 | 撤销后错误完成、完成后错误释放、多次清算/退款累计错误、拒付被当成授权拒绝或普通退款。 | 校准状态机、授权释放、实际扣款、退款/拒付承接和累计金额闭合；即使拒付底层复用退款终态，也必须保留拒付原因、凭证、外部引用、审计上下文和投影可区分性。 | `just test-business-flow` |
| B4-03 | 补授权过期独立语义。 | `transaction-*`、`core`、`tests/src/test/java` | `GAP-AUTH-001`、授权过期用例。 | `EXPIRE` 与 `REVERSAL` 混用，过期释放已完成金额。 | 人工确认后补 `EXPIRE` 事件和 `expire` 服务入口；只释放剩余 `AUTHORIZATION`。 | `just test-transaction` |
| B4-04 | 校准 `settle` 强制完成模式。 | `transaction-*`、`tests/src/test/java` | `GAP-AUTH-002`、force settle 设计。 | 无前置授权完成缺少策略、原因、上限或审计。 | 人工确认后扩展 settle 请求契约和策略校验。 | `just test-business-flow` |
| B4-05 | 校准 `settleRefund` 无授权退款模式。 | `transaction-*`、`tests/src/test/java`，必要时涉及请求契约 | `GAP-AUTH-004`、`AC-AUTH-012`、`TDD-AUTH-006`。 | 无前置授权但有外部原消费、原完成或差错凭证时无法退款；无原事实或缺审计仍静默退款；实现为了退款补造授权占用。 | 人工确认后补充或校准 `settleRefund` 引用、原因、凭证和审计字段；不得创建内部授权占用；查询、投影和审计能说明无授权退款来源。 | `just test-business-flow` |
| B4-06 | 固化 VCC / Spend Controls 扩展边界。 | `transaction-*`、`tests/src/test/java` | Spend Controls 为扩展能力。 | 将 spend controls 当成资金主链路 P0。 | 默认只保留边界测试；未明确启用发卡产品时不做实现。 | `just test-boundary` |
| B4-07 | 明确 chargeback 不落 `FundsAuthorizationTransactionService#chargeback`。 | `docs`、`openspec`、后续授权测试 | 用户补充口径、拒付业务事实。 | 测试或任务强制要求调用 `chargeback` 服务入口，或把拒付结果只保存成无法区分的普通退款。 | 拒付作为争议/扣回语义，通过 `settleRefund` 的原因、上下文、凭证和审计承接；如代码已有 `chargeback`，本轮不扩展为目标态主入口；查询、投影和审计必须能区分普通退款与拒付承接。 | `just test-transaction` |
| B4-08 | 固化授权并发竞争红线。 | `transaction-*`、`core`、`tests/src/test/java` | TDD 13.5、授权生命周期。 | 同一授权的完成、撤销、过期、退款并发导致重复入账、重复释放或剩余为负。 | 通过幂等键、状态版本、唯一约束或锁定策略确保同一授权同一时刻只有合法迁移生效，失败方无副作用。 | `just test-business-flow` |
| B4-09 | 固化授权占券和权益生命周期边界。 | `transaction-*`、`core`、`tests/src/test/java` | `DSL-BENEFIT-AUTH-HOLD-001`、`TDD-BEN-AUTH-*`、`TDD-BEN-ENTRY-003`、`TDD-BEN-ENTRY-004`、`RED-053`。 | 授权拒绝、工具准入失败或余额不足后仍核销权益；授权完成和过期并发导致同一权益重复核销或释放；完成时按当前券规则重算；新增授权权益专用服务入口或授权后续事件接收当前权益结果。 | 不新增授权权益专用服务入口；`authorize` 可在对应 Execution Grant 确认后扩展可选 `benefitSnapshot` 或等价字段固化占用引用；`reversal`、`expire`、`settle` 和 `settleRefund` 读取原授权事实和原权益快照；授权阶段只固化占用引用，完成按原快照核销，撤销或过期释放剩余占用；失败事件无 route/posting/entry 副作用。 | `just test-business-flow` |

人工确认点：`EXPIRE` 枚举、`expire` 服务入口、`settle` 强制完成请求契约、`settleRefund` 无授权退款引用和审计字段、授权占券字段和权益 hold/release/write-off 外部引用；VCC / Spend Controls 默认只做边界测试，只有明确启用发卡产品才进入实现范围。

### B5 覆盖索引：余额控制

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B5-01 | 固化冻结/解冻控制语义。 | `transaction-*`、`tests/src/test/java` | 余额控制 DSL、资金红线。 | 冻结表达消费、跨主体冻结、多次解冻超额。 | 保持同主体 `AVAILABLE <-> FROZEN` 控制。 | `just test-balance-control` |
| B5-02 | 固化冻结后提现与组合路径。 | `transaction-*`、`tests/src/test/java` | 产品验收用例。 | 提现忽略冻结、冻结释放后余额桶错误。 | 逐步骤断言可用、冻结、实际扣划。 | `just test-balance-control` |
| B5-03 | 固化资金账户余额调整、信用账户额度、预算组额度。 | `transaction-*`、`tests/src/test/java` | 账户/额度/预算概念边界。 | 余额调整缺来源、审批、凭证或审计，跨主体价值转移误用 adjust，额度与余额混用。 | 保持资金账户余额调整、信用账户额度和预算组额度分离；adjust 必须有准入来源和审计；完整对账差错闭环在B7 验证。 | `just test-balance-control` |
| B5-04 | 固化 FX 与余额控制边界。 | `transaction-*`、`tests/src/test/java` | TDD 红线。 | 余额控制承接 FX 或隐式换汇。 | 余额控制只处理同币种余额桶。 | `just test-boundary` |
| B5-05 | 固化冻结、解冻和提现并发竞争红线。 | `transaction-*`、`tests/src/test/java` | TDD 13.5、余额控制组合场景、冻结动作明细。 | 同一冻结来源被解冻和提现确认并发关闭，导致重复释放、重复关闭或 `FROZEN` 为负。 | 通过冻结动作明细、剩余金额校验、动作幂等键和唯一约束保证同一冻结来源只被关闭或释放一次。 | `just test-balance-control` |

### B6 覆盖索引：Route Replay、交易投影和余额日志

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B6-01 | 固化 route resolver 选择和无副作用。 | `transaction-*`、`tests/src/test/java` | 路由系分、路由能力边界。 | `supports` 写事实或改变状态。 | 保持 resolver 只判定路径，不写交易/账本事实。 | `just test-boundary` |
| B6-02 | 固化缺快照 Route Replay 失败。 | `transaction-*`、`tests/src/test/java` | Route Replay 设计。 | 缺 route/amount/subject 快照仍按当前绑定重放成功。 | Route Replay 必须依赖原始快照并输出明确失败原因；不得退化为重新路由。 | `just test-business-flow` |
| B6-03 | 固化交易投影只读和有界重放。 | `transaction-*`、`tests/src/test/java` | 交易投影重放。 | 无范围重放、投影水位复用余额水位或归档 checkpoint。 | 引入范围、模式、checkpoint 和差异报告红线；交易投影重放不重新入账、不改余额。 | `just test-boundary` |
| B6-04 | 固化余额日志边界。 | `ledger-*`、`tests/src/test/java` | 余额日志设计。 | 余额日志失败导致事实回滚，或被用于修复余额。 | 保持日志为观测与审计辅助，不作事实源。 | `just test-boundary` |
| B6-05 | 固化支付工具换绑后原路径回放。 | `transaction-*`、`tests/src/test/java` | `DSL-PAYMENT-INSTRUMENT-REPLAY-001`、TDD 支付工具回放红线。 | 原交易后工具换绑、解绑、暂停或默认资金来源变化，退款、撤销、退费或拒付按当前绑定重新选路。 | Route Replay 必须使用原 route snapshot、原工具快照和原 funding allocation；当前绑定只作为审计上下文，不参与资金路径。 | `just test-business-flow` |
| B6-06 | 固化权益快照回放和缺快照处理。 | `transaction-*`、`tests/src/test/java` | `DSL-BENEFIT-REFUND-NO-COUPON-001`、`DSL-BENEFIT-REFUND-RETAIN-SUBSIDY-001`、`DSL-BENEFIT-MISSING-SNAPSHOT-REPLAY-001`、`TDD-BEN-ENTRY-002`、`TDD-BEN-ENTRY-004`、`TDD-BEN-REFUND-004`、`TDD-BEN-REFUND-005`、`TDD-BEN-REPLAY-002`。 | 退款、撤销、过期或拒付按当前营销规则重算；缺原权益快照仍静默放行；不退券和不冲补贴混成一个布尔值；多次部分退款累计超额或尾差随机分摊；仅用外部渠道流水替代原资金交易或原权益快照引用；含权益退款请求承载新的权益计算结果；资金底座自行判断券是否可退。 | replay 使用原权益快照、组件摘要、退款策略和业务层本次退款决策引用；直接退款 Request 如需扩展，应补原资金交易、原 route snapshot、原权益快照、业务决策流水或 replay 策略引用，不承载新的权益计算结果；缺快照失败或进入人工处理；累计金额、补贴冲回、储值恢复和不可退权益都可追溯；多组件部分退款必须有确定性尾差规则且重试结果稳定。 | `just test-business-flow` |
| B6-07 | 固化交易投影和运营时间线可解释性。 | `transaction-*`、`tests/src/test/java` | 产品使用者可解释性矩阵、系分 05 可解释输出、`GAP-OPS-001`。 | 查询或投影只返回技术状态，无法说明金额来源、事实状态、展示状态、操作状态、失败阶段、不可操作原因、下一步动作、证据脱敏引用或外部规则核验状态；授权占用、冻结、待清算或出款受理被展示为完成资金结果；解释视图反写资金事实。 | 用户账单、商户账单、运营时间线和交易投影必须只读生成解释摘要，至少包含业务键、资金交易、route snapshot 摘要、账务引用、事实状态、展示状态、操作状态、失败原因、不可操作原因、下一步动作、脱敏证据引用和外部规则核验状态；不修改交易、账本或余额事实。 | `just test-boundary` |

### B7 独立任务索引：清结算与对账

B7 在本 change 中只保留边界、计划和设计评审输入，不表示整体编码准入已经打开。冻结基线中 `reconciliation-face` 和 `reconciliation-impl` 已有模块骨架和出款前准入候选实现，已完成专项验证，可作为后续候选输入，但不能作为完整清结算、对账或出款生命周期 Done 证据。继续编码前仍必须另起独立 OpenSpec change，确认模块命名、表设计、状态机、接口、是否扩展出款前准入候选能力、DDL/H2、NFR 假设、观测告警和验证命令。

当前 PRD、DSL、系分和 TDD 对清结算与对账已经达到 TDD 分析输入状态：对象边界、资金事实边界、验收矩阵和首批 Red 方向可评审。该状态只允许进入 TDD 分析、研发拆解和 Execution Grant 准备，不允许直接进入生产代码、测试代码、DDL/H2 schema 或运行时配置写入。

| CLS-GATE | 必须写入 TDD 分析输出或独立 Execution Grant |
| --- | --- |
| CLS-GATE-001 Execution Grant 准入 | 设计基线范围、写入范围、禁止范围、验证命令、人工确认点和停止条件。 |
| CLS-GATE-002 首批 Red 准入 | `TDD-B7-RED-001` 至 `TDD-B7-RED-007` 的失败断言、目标测试层级和目标验证命令。 |
| CLS-GATE-003 数据与服务闭环 | 对账批次、可清分明细、清分批次、清算候选、清算批次、结算单、出款单、差错单、追偿单和审计对象的表、状态机、幂等键、DTO、Entity、Mapper、服务和 H2 测试范围。 |
| CLS-GATE-004 运营补事实白名单 | 允许生成资金事实的命令或等价事件、来源单据、审批号、证据引用、幂等键、原事实引用、操作者、原因、可撤销边界和失败无副作用测试。 |
| CLS-GATE-005 NFR 与可观测性 | 批处理容量、重跑窗口、并发锁、对账文件延迟、回单乱序、告警指标、Runbook 信号、敏感导出和审计留存。 |
| CLS-GATE-006 专业确认状态 | 财务、税务、会计、合规、法务、银行、通道、卡组织、KYC/KYB/AML、客户资金、跨境或外汇规则来源和确认状态。 |
| CLS-GATE-007 使用者解释和安全操作 | 事实状态、展示状态、操作状态、不可操作原因、下一步动作、责任方、到期重查和脱敏证据引用。 |
| CLS-GATE-008 职责分离和证据最小化 | 高危动作发起、复核、审批分离，敏感证据查看、导出、脱敏、水印和审计。 |

| B7 准入门禁 | 必须写入独立 Execution Grant |
| --- | --- |
| 基线引用 | PRD、DSL、系分、TDD、OpenSpec、任务和代码基线提交点。 |
| 写入范围 | 是否复用 `reconciliation-face`、`reconciliation-impl`，以及允许新增或修改的 API、DTO、Entity、Mapper、DDL/H2 schema、fixture、服务和测试目录。 |
| 首批 Red | `TDD-B7-RED-001` 至 `TDD-B7-RED-007` 必须先落地，且先于 B7-01 至 B7-07 任一 Green。 |
| 运营补事实白名单 | 允许生成资金事实的命令或等价事件、来源单据、审批号、证据引用、幂等键、原事实引用、操作者、原因、可撤销边界和失败无副作用测试。 |
| NFR 与观测 | 批处理容量、重跑窗口、并发锁、对账文件延迟、回单乱序、告警指标、Runbook 信号、敏感导出和审计留存。 |
| 专业确认状态 | 财务、税务、会计、合规、法务、银行、通道、卡组织、KYC/KYB/AML、客户资金、跨境或外汇规则来源和确认状态。 |
| 使用者解释与职责分离 | 事实状态、展示状态、操作状态、不可操作原因、下一步动作、责任方、到期重查、脱敏证据引用、高危动作发起/复核/审批分离、敏感导出水印和审计。 |

| CLS-00 | 纳入出款前准入候选实现并固化候选基线。 | `reconciliation-face`、`reconciliation-impl`、`tests/src/test/java/com/capte/funds/reconciliation` | `AC-SET-006` 至 `AC-SET-009`、`TDD-SETTLE-004`、`TDD-SETTLE-005`、`GAP-CLS-002`。 | 只有 `ruleEvidenceRef` 仍被视为已核验、创建前检查强制要求 `payoutSn`、准入结果缺事实/展示/操作状态、准入检查写入账务事实。 | `checkPayoutPreflight` 候选实现必须同步纳入结构化规则核验证据、预创建检查、服务端解释状态、阻断原因和只读事实断言；随后再补出款表、回单、金额不一致和查询解释模型。 | `just test-one PayoutPreflightServiceTests tests` |

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B7-01 | 确认独立模块、表和状态机。 | 待确认的 `reconciliation-*` 扩展或独立包 | 系分 03、产品清分/清算/对账。 | 可清分明细、清分批次、清算候选、清算批次、结算单、出款单、对账批次、差错单或追偿单混成一个对象。 | 人工确认后建立独立对象边界。 | 待确认 |
| B7-02 | 固化可清分准入、清分批次和清算候选排除。 | 待确认 | 可清分规则、清分批次、清算候选。 | 未完成交易入可清分明细、清分批次确认即入账、冻结或重大差错交易入清算候选。 | 可清分只来自成功交易、已过账 `CLEARING` 分录和完整来源快照；清分批次只固化归类和规则版本；候选只来自已确认清分结果且满足可清算规则。 | 待确认 |
| B7-03 | 固化清算批次确认、结算锁定、出款成功/失败和追偿。 | 待确认 | 清算批次、结算单、出款单、追偿单边界。 | 候选入池即清算、清算前置对账缺失、出款失败直接改历史分录，或重复出款。 | 清算批次确认才触发 `CLEARING -> AVAILABLE`；结算锁定、出款单、失败原因和追偿链路各自独立。 | 待确认 |
| B7-04 | 固化对账批次、差错单和核销。 | 待确认 | 对账差错闭环。 | 对账差异直接改分录或余额，或绕过差错闭环直接调用 adjust。 | 强制差错单、审批、凭证、调账/冲正、核销、重新对账；差错类资金账户余额调整由闭环触发余额控制 adjust 或批次授权直接交易调账事实。 | 待确认 |
| B7-05 | 固化清结算与对账并发竞争红线。 | 待确认 | TDD 13.5、清结算退款时序、对账重跑。 | 清算候选锁定与退款并发、结算锁定与出款回单/退款并发、对账重跑与差错核销并发导致重复扣减、重复出款或证据覆盖。 | 建立对象级锁定、批次唯一键、候选状态版本、重跑运行记录和差错核销互斥；失败方必须可审计且无副作用。 | 待确认 |
| B7-06 | 固化含权益交易的清结算和对账拆分。 | 待确认 | `DSL-BENEFIT-CLEARING-RECONCILIATION-001`、`AC-BEN-011`、`AC-BEN-013`、`AC-BEN-015`、`TDD-BEN-CLS-*`、`TDD-BEN-RECON-*`、`RED-057`、`RED-059` 至 `RED-062`。 | 营销核销、订单金额、用户实付、商户应收、平台补贴、手续费和退款冲回不一致时被静默补平；商户券被误当平台成本；补贴冲回、不可退权益、负债恢复或展示项混入订单正向闭合；`CONTRACT_ONLY` 夹具或专业确认缺失被当作清结算 Done。 | 清分、清算、结算和对账明细拆出权益金额项；闭合角色决定金额项进入订单、退款、补贴冲回、负债恢复或展示解释；差异进入差错单和审计，不直接改历史资金事实；进入 B7 前必须确认夹具级别、权益事实源和专业确认状态。 | 待确认 |
| B7-07 | 固化清结算、对账和出款工作台可解释性。 | 待确认 | 产品使用者可解释性矩阵、系分 03 运营后台、系分 05 告警到 Runbook 动作矩阵、`GAP-OPS-001`。 | 重大差错或前置对账未完成仍展示为可清算、可结算或可出款；出款受理展示为成功；差错缺责任方、阈值、账龄、审批或到期重查；证据原文进入导出或告警；外部规则未核验仍展示可操作；缺事实状态、展示状态或操作状态仍进入商户账单、运营台或审计导出。 | 清分、清算、结算、出款、对账差错和追偿视图必须能解释金额来源、事实状态、展示状态、操作状态、阻断原因、放行依据、审批链、下一步动作、脱敏证据引用、外部规则核验状态和恢复验收。 | 待确认 |

B7 首批 Red 集必须在 B7-01 至 B7-07 任一 Green 实现前完成：`TDD-B7-RED-001` 清分候选直接入账、`TDD-B7-RED-002` 出款前准入未知或失败仍提交、`TDD-B7-RED-003` 非白名单运营动作生成资金事实、`TDD-B7-RED-004` 外部非终态被当成功、`TDD-B7-RED-005` 差错未重新对账即关闭、`TDD-B7-RED-006` 缺使用者解释仍可放行、`TDD-B7-RED-007` 单人绕过职责分离或查看敏感原文。首批 Red 未落地时，B7 只能继续做设计或契约草案，不能声明清结算、对账或出款生命周期生产可用。

人工确认点：是否新建模块、模块命名、表命名、状态机、是否扩展出款前准入候选能力、权益清分字段、权益对账差错类型、运营补事实命令白名单、使用者解释字段、职责分离策略、证据最小化策略、NFR 假设、外部规则确认状态、是否进入本轮开发范围。

### B8 独立任务索引：归档、重放与指标边界

B8 在本 change 中只保留边界、计划和设计评审输入，不表示编码准入已经打开。当前代码已有 `governance-face` 和 `governance-impl` 交易投影重放骨架，可作为交易投影治理重放的候选落点；进入编码前仍必须另起独立 OpenSpec change，确认归档/重放任务模型、表设计、审批、回滚、NFR 假设、证据最小化、外部规则核验、观测告警和生产门禁；指标只保留边界，不实现报表指标模块。

当前 PRD、DSL、系分和 TDD 对归档重放与指标治理已经达到 TDD 分析输入状态：归档 Manifest、checkpoint、watermark、余额快照、交易投影重放、差异报告、人工处理、指标水位隔离和大数据归档承接边界可评审。该状态只允许进入 TDD 分析、研发拆解和 Execution Grant 准备，不允许直接进入生产代码、测试代码、DDL/H2 schema 或运行时配置写入。

| GOV-GATE | 必须写入 TDD 分析输出或独立 Execution Grant |
| --- | --- |
| GOV-GATE-001 Execution Grant 准入 | 设计基线范围、写入范围、禁止范围、验证命令、人工确认点和停止条件。 |
| GOV-GATE-002 首批 Red 准入 | `TDD-B8-RED-001` 至 `TDD-B8-RED-005` 的失败断言、目标测试层级和目标验证命令。 |
| GOV-GATE-003 治理物理落点 | 复用 `governance-face/governance-impl`、扩展既有包或新增独立模块的取舍、依赖方向、公共契约、DTO、Entity、Mapper 和边界测试范围。 |
| GOV-GATE-004 Manifest 与 H2 范围 | 归档任务、运行记录、Manifest、checkpoint、watermark、冷热对象清单、余额快照、差异报告、人工处理、导出快照、消费方登记和审计对象的表、状态机、幂等键和 H2 测试范围。 |
| GOV-GATE-005 dry-run/apply 边界 | dry-run 不推进 checkpoint、watermark、Manifest 或正式投影；apply 需要范围锁、幂等键、审批、差异报告、失败无副作用和回滚/续跑策略。 |
| GOV-GATE-006 只读事实边界 | 归档、余额重建、交易投影重放、差异报告、普通指标快照和大数据归档承接不得创建 route、posting、entry、交易明细、清结算对象或余额修复事实。 |
| GOV-GATE-007 水位隔离和指标边界 | 余额水位、归档水位、交易投影 checkpoint、指标水位和大数据导出消费水位独立；普通指标快照不能替代账本余额快照。 |
| GOV-GATE-008 使用者解释、导出和 Runbook | 差异报告、人工处理、治理导出、Runbook 告警和审计导出必须包含范围、模式、状态、阻断原因、影响范围、责任方、下一步动作、恢复验收和脱敏证据引用。 |
| GOV-GATE-009 大数据归档承接 | 报表数仓、离线指标或经营分析只能通过治理读取、导出快照、Manifest 摘要、脱敏、digest、消费方登记和审计承接。 |

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B8-01 | 确认归档和重放任务模型。 | 待确认的 `governance-*` 扩展或独立包 | 系分 04、归档重放设计。 | 无审批、无范围、无 Manifest 即执行归档/重放。 | 人工确认后建立任务、审批、范围和 Manifest 边界。 | 待确认 |
| B8-02 | 固化归档门禁和水位推进顺序。 | 待确认 | 归档流程。 | 先推水位再写结果、缺差异报告。 | checkpoint、watermark、Manifest、差异报告作为上线门禁。 | 待确认 |
| B8-03 | 固化余额重建和交易投影重放。 | 待确认 | Balance Rebuild、Transaction Projection Replay。 | 余额重建从交易投影、余额日志或报表反推余额；交易投影重放重新入账。 | 保持事实只读、投影可重建、差异可解释；余额重建只从账本分录、检查点、水位和 Manifest 出发。 | 待确认 |
| B8-04 | 固化账本余额快照覆盖模式。 | 待确认 | 系分 04 账本余额快照、`DSL-GOVERNANCE-BALANCE-SNAPSHOT-001`。 | `HOT_ONLY` 强制要求 Manifest、`COLD_MANIFEST` 缺 Manifest 仍通过、`MIXED` 跳过冷热合并摘要校验。 | 建立 `HOT_ONLY`、`COLD_MANIFEST`、`MIXED` 三种覆盖模式校验；冷区和混合覆盖缺 Manifest 不得进入 `VERIFIED`；失败不得推进余额水位。 | 待确认 |
| B8-05 | 固化指标只读和指标水位隔离。 | 待确认 | 指标治理仅列指标项、`DSL-GOVERNANCE-METRIC-SNAPSHOT-BOUNDARY-001`、`TDD-METRIC-003`、`TDD-METRIC-004`。 | 在本模块实现报表指标计算，或指标失败推进余额水位、修改归档 Manifest；普通指标快照成功替代账本余额快照。 | 仅提供业务关心的指标口径输入，具体实现交给报表指标模块；指标水位独立于余额水位、归档 Manifest 和交易投影 checkpoint；普通指标快照不能证明余额正确。 | 待确认 |
| B8-06 | 固化归档、重放和快照范围互斥。 | 待确认 | TDD 13.5、归档门禁、重放差异报告和普通指标快照并发边界。 | 同一范围重复正式 apply、dry-run 推进 checkpoint、水位或 Manifest 被并发任务重复推进；普通指标快照覆盖余额快照状态。 | 建立范围锁、任务幂等键、dry-run/apply 分离、成功后单次推进规则和指标/余额快照状态隔离；失败任务不得推进水位。 | 待确认 |
| B8-07 | 固化归档、重放和指标边界 Runbook 信号。 | 待确认 | 系分 05 告警到 Runbook 动作矩阵、生产门禁、`GAP-OPS-001`。 | 归档或重放告警只有异常堆栈，没有范围、模式、checkpoint、Manifest、差异报告、止血动作或恢复验收。 | 归档、余额重建、交易投影重放和指标边界告警必须输出任务范围、dry-run/apply 模式、checkpoint、watermark、Manifest、差异报告、负责人、止血动作和恢复验收。 | 待确认 |
| B8-08 | 固化归档重放异常人工处理闭环。 | 待确认 | 产品 `AC-ARCH-008`、系分 04 差异报告模型、`TDD-ARCH-009`。 | 缺范围、缺 Manifest、冷热摘要不一致、检查点不连续、重放差异、权限不足或外部规则待确认时，后台人工处理直接修改交易、账目、余额或投影事实。 | 异常必须进入差异报告、阻断原因、影响范围、责任归属、证据引用、人工处理入口和可重跑条件；人工处理只能审批、补证据、调整范围、重跑或关闭差异。 | 待确认 |
| B8-09 | 固化大数据归档承接边界。 | 待确认 | 产品 `AC-ARCH-009`、`DSL-GOVERNANCE-BIG-DATA-ARCHIVE-BOUNDARY-001`、系分 04 治理读取或导出快照、`TDD-ARCH-010`。 | 报表数仓、离线指标或经营分析绕过治理边界直接扫描在线资金冷归档，或反写交易、账目、余额、清结算、对账、投影事实，或推进资金水位、替代余额快照和交易重放 checkpoint。 | 大数据归档只能通过治理读取、导出快照、Manifest 摘要、脱敏、digest、消费方和审计边界承接；资金冷归档只作为事实留存和重放证据，不作为在线报表库。 | 待确认 |

B8 首批 Red 集必须在 B8-01 至 B8-09 任一 Green 实现前完成：`TDD-B8-RED-001` 缺 Manifest 或 checkpoint 仍归档成功、`TDD-B8-RED-002` 治理任务反写资金事实、`TDD-B8-RED-003` 普通指标快照替代余额确认、`TDD-B8-RED-004` 无范围或无差异报告的正式重放、`TDD-B8-RED-005` 大数据归档绕过治理读取。首批 Red 未落地时，B8 只能继续做设计、契约或 dry-run，不得进入 apply、正式水位推进或生产修复控制面。

人工确认点：生产重放范围、审批策略、回滚策略、账本余额快照覆盖模式字段、Manifest 覆盖策略、指标模块接口边界。

## 8. 依赖、门禁与设计反馈

1. 设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果必须先冻结为独立检查点；上一设计冻结点为 30b1a00 docs: 冻结权益快照设计基线，当前设计和任务基线提交点为 `9456ab6 docs: 对齐 A0 准入与代码基线`，代码能力基线截至 `77bc9f4 fix: 阻断钱包上下文权益核心事实`；后续具体 MVP 编码任务仍必须通过 Execution Grant 明确授权。
2. 当前执行优先级固定为：P0 资金底座内核优先，P1 交易与读模型扩展次之，P2 业务模式能力包按专项授权接入；文档编号和覆盖索引编号不代表能力优先级。
3. B1、B2 承接 P0 的 DSL、钱包、账本、账目和余额投影基础；B3 至 B6 承接 P1 的直接交易、授权交易、余额控制、Route Replay 与交易投影；B7、B8 分别承接 P0 清结算对账和 P0/P1 治理闭环。
4. B1 覆盖索引是后续所有交易、账本和授权测试的前置门禁。
5. B2 覆盖索引是后续业务流余额断言、账务平衡和组合测试的前置门禁。
6. B1 已有 DSL 契约测试基线，但仍未替代后续变更授权；如继续修改 `core` 公共枚举、Spec 或值对象，Execution Grant 必须显式确认是否允许修改公共契约和枚举，以及允许修改的范围。
7. B4 涉及公共契约、枚举、服务入口和 Request/Query/DTO，必须先经人工确认再改生产代码；`settle` 强制完成和 `settleRefund` 无授权退款必须有策略、原事实引用、凭证、原因和审计；chargeback 不作为 `FundsAuthorizationTransactionService#chargeback` 目标入口。
8. B7、B8 属于独立授权域，不得在 A0 至 A4 或 B1 至 B6 覆盖索引中顺手落入交易、钱包或账本入口；进入编码前必须另起独立 OpenSpec change。
9. B7 若允许清结算、对账、出款、差错、补事实、冲正、调账或追偿通过交易层追加资金事实，Execution Grant 必须列出运营补事实命令白名单、来源单据、审批号、证据引用、幂等键、原事实引用、操作者、原因、可撤销边界和失败无副作用测试；未列入白名单的运营动作只能生成处理单、差异报告或审计记录。
10. 归档重放与指标治理是否先于、并行或后于清结算与对账编码，取决于独立 Execution Grant 和事实边界是否已确认；04 只能消费 02 和 03 已确认的事实边界、批次摘要、差异报告和只读投影输入，不得用归档或重放补造上游事实。
11. B8 进入编码前必须确认治理物理落点：复用既有 `governance-face` / `governance-impl`、扩展已有包，还是新增独立模块；同时确认依赖方向、是否新增公共契约、表结构和 H2 schema、Mapper/Entity 归属、边界测试、指标水位隔离测试和普通指标只读边界。
12. 任一 MVP 任务发现产品、DSL、系分或 TDD 口径冲突时，先记录“设计错漏”，同步修正设计文档和 OpenSpec，再继续编码。
13. 每个 MVP 任务必须先提交 Red 用例，再做最小 Green 实现，最后补充重构、观测、边界和回归验证。
14. 金额临界值按 TDD 5.1 作为所有资金变化测试的公共前置矩阵；并发竞争按 TDD 13.5 分散到授权、余额控制、清结算、对账和归档相关 MVP 任务承接，不得只用顺序用例代替。
15. 权益快照按三段落地：B1 只做 `core` DSL 契约和 JSON 空值语义；B3、B4、B6 才进入直接交易、授权和 replay 的 route/posting 消费；B7 才进入清结算与对账拆分；B8 才进入归档、冷热读取和治理重放消费。未完成后段前，不得宣称含权益资金流已完整闭合；涉及独立伴随指令、补充权益事实、审计证据包、使用者解释视图、证据最小化或外部规则核验时，还必须在对应 MVP 任务的 Execution Grant 中登记原子性、补录载体、最终重校验、视图防误导、脱敏边界、规则核验和失败处理。
16. 直接交易和授权交易不因权益快照新增权益专用服务方法；后续如需扩展 `FundsTransactionPayRequest`、`FundsTransactionRefundRequest`、`FundsAuthorizationTransactionAuthorizeRequest` 或授权后续事件 Request，必须由对应 MVP 任务的 Execution Grant 明确允许。授权过期 `expire` 是 B4-03 既有授权生命周期缺口，不属于权益设计新增入口。
17. 券能不能退由业务层、订单层、营销权益系统或运营审批链路决策；资金底座只承接原权益快照和本次退款决策引用，不调用当前营销规则、不读取当前券包状态、不自行判断退券可行性。
18. 平台补贴、储值券、预付券、礼品卡、客户资金、商户待结算资金、负债、备付或收入成本口径进入 B3/B7 生产资金流前，必须在 Execution Grant 中登记财务、税务、会计、合规或法务确认状态；未确认时只能保留为契约或设计验证，不得进入生产 Done 结论。
19. 含权益 MVP 任务进入 Phase 2 或 Phase 3 前，Execution Grant 必须选择 Phase 能力边界、JSON 夹具级别、权益快照事实源、零实付表达、平台补贴表达、独立伴随指令原子性、储值/预付口径、退款分摊粒度、退款分摊确定性规则版本、分摊依据、稳定组件顺序、舍入模式、尾差归属、组件剩余额度版本、历史无权益快照处理策略、补充权益事实模型、专业确认状态、审计证据包、使用者解释视图、证据最小化和外部规则核验状态；缺任一项时只能执行 contract-only、设计验证或失败用例，不得实现生产资金流。
20. `contextVariables` 只允许作为短期迁移通道，且只能承载 `benefitSnapshotId`、`stableDigest`、`benefitGroupSn`、`componentSn` 摘要、`ruleVersion`、`refundDecisionId`、`externalDecisionId` 等追溯引用；组件金额、资金责任、退款处置完整内容和当前营销规则不得放入 `contextVariables` 作为生产事实。
21. `com.capte.funds.transaction.model.request` 下的交易 Request 只能以 `ReadonlyContextVariables` 承载扩展上下文；不得恢复 `WritableContextVariables` 字段或 `FundsRequestContextVariables` request 专属快照工具。后续若扩展核心资金语义，必须使用一等字段或不可变事实快照，并在 Execution Grant 中列明公共契约范围、请求摘要和敏感上下文回归测试。

## 9. 覆盖索引到设计 ID 映射

| 覆盖索引 | 产品验收 ID | DSL 契约或场景 | TDD 用例入口 |
| --- | --- | --- | --- |
| B1 | `RED-001`、`RED-003`、`RED-009`、`RED-020`、`RED-022`、`RED-023`、`RED-046` 至 `RED-049`、`AC-BEN-001`、`AC-BEN-012`、`AC-BEN-013`、`AC-BEN-015`、`RED-050` 至 `RED-059`、`RED-061`、`RED-063` | Route DSL、PaymentInstrument Route DSL、Posting/Ledger DSL、SettlementPolicy、金额临界值、权益快照、JSON 契约、`DSL-BENEFIT-SNAPSHOT-001`、`CONTRACT_ONLY` 夹具边界 | `TDD-RED-001`、`TDD-RED-003`、`TDD-RED-004`、`TDD-RED-031`、`TDD-RED-032`、`TDD-RED-034` 至 `TDD-RED-037`、`TDD-LEDGER-001` 至 `TDD-LEDGER-011`、`TDD-ROUTE-011` 至 `TDD-ROUTE-013`、`TDD-BEN-001` 至 `TDD-BEN-009`、`TDD-BEN-RED-001`、`TDD-BEN-RED-002`、`TDD-BEN-RED-012`、`TDD-BEN-RED-017`、`TDD-BEN-RED-018`、`TDD-BEN-RED-020`、`TDD-BEN-RED-023` |
| B2 | `AC-PI-001` 至 `AC-PI-004`、`AC-PI-006`、`AC-PI-007`、`AC-CTRL-009` 至 `AC-CTRL-011`、`AC-BALLOG-001`、`RED-036`、`RED-046`、`RED-047`、`RED-049` | 支付工具、绑定关系、绑定历史审计、资金来源关系、账本周期、余额投影、余额日志 | `TDD-WALLET-*`、`TDD-ROUTE-011`、`TDD-ROUTE-012`、`TDD-LEDGER-*`、`TDD-VIEW-003` |
| B3 | `AC-IN-*`、`AC-OUT-*`、`AC-PAY-*`、`AC-MER-*`、`AC-FEE-*`、`AC-BEN-002` 至 `AC-BEN-005`、`AC-BEN-009`、`AC-BEN-013`、`AC-BEN-014`、`AC-BEN-016`、`AC-BEN-018`、`AC-BEN-019`、`RED-050` 至 `RED-052`、`RED-054` 至 `RED-060`、`RED-062`、`RED-064` 至 `RED-066` | `DSL-DIRECT-*`、`DSL-REVERSE-REFUND-FEE-001`、`DSL-BENEFIT-MERCHANT-DISCOUNT-001`、`DSL-BENEFIT-PLATFORM-SUBSIDY-001`、`DSL-BENEFIT-PLATFORM-NO-SETTLEMENT-001`、`DSL-BENEFIT-PREPAID-VOUCHER-001`、`DSL-BENEFIT-PARTIAL-REFUND-001`、`DSL-BENEFIT-COMPANION-INSTRUCTION-001`、`DSL-BENEFIT-AUDIT-EVIDENCE-001`、`DSL-BENEFIT-EXPLAINABLE-VIEW-001`、零实付权益表达、外部规则核验状态 | `TDD-DIR-*`、`TDD-DIR-FLOW-*`、`TDD-DIR-ERR-*`、`TDD-BEN-DIR-*`、`TDD-BEN-DIR-006`、`TDD-BEN-DIR-007`、`TDD-BEN-REFUND-003`、`TDD-BEN-RED-003`、`TDD-BEN-RED-004`、`TDD-BEN-RED-006`、`TDD-BEN-RED-008`、`TDD-BEN-RED-017` 至 `TDD-BEN-RED-019`、`TDD-BEN-RED-021`、`TDD-BEN-RED-022`、`TDD-BEN-RED-024`、`TDD-BEN-RED-025`、`TDD-BEN-RED-027` 至 `TDD-BEN-RED-030`、`TDD-RACE-012` |
| B4 | `AC-AUTH-001` 至 `AC-AUTH-012`、`AC-RAIL-001`、`AC-RAIL-002`、`AC-BEN-006`、`AC-BEN-013`、`RED-025` 至 `RED-027`、`RED-035`、`RED-053`、`RED-054`、`RED-059` | `DSL-AUTH-LIFECYCLE-001`、`DSL-AUTH-FORCE-CAPTURE-001`、`DSL-AUTH-REFUND-001`、`DSL-BENEFIT-AUTH-HOLD-001`、`AUTHORIZATION_TRANSACTION / SETTLE` 强制完成模式、`AUTHORIZATION_TRANSACTION / AUTH_REFUND` 无授权退款模式、VCC 授权归一边界和授权控制扩展 | `TDD-AUTH-*`、`TDD-AUTH-FLOW-*`、`TDD-AUTH-ERR-*`、`TDD-AUTH-EXT-*`、`TDD-BEN-AUTH-*`、`TDD-BEN-RACE-002`、`TDD-BEN-RED-005`、`TDD-BEN-RED-014`、`TDD-BEN-RED-017`、`TDD-ROUTE-003`、`TDD-ROUTE-005`、`TDD-ROUTE-008`、`TDD-ROUTE-009`、`TDD-RACE-001` 至 `TDD-RACE-003`、`TDD-RAIL-001`、`TDD-RED-003`、`TDD-RED-005`、`TDD-RED-008`、`TDD-RED-016`、`TDD-RED-017`、`TDD-RED-033`、`TDD-RED-036` |
| B5 | `AC-CTRL-001` 至 `AC-CTRL-008`、`AC-ADJ-001` 的 adjust 入口和红线；其中 `AC-CTRL-004` 为资金账户余额调整 | `DSL-BALANCE-CONTROL-FREEZE-001`、`DSL-BALANCE-CONTROL-ADJUST-001`、`DSL-BALANCE-CONTROL-LIMIT-BUDGET-001`、`DSL-SETTLEMENT-RECONCILIATION-ADJUST-001` 的 adjust 红线 | `TDD-CTRL-*`、`TDD-CTRL-FLOW-*`、`TDD-CTRL-ERR-*`、`TDD-CTRL-009`、`TDD-CTRL-ERR-005`、`TDD-RACE-004`、`TDD-RED-006`、`TDD-RED-011`、`TDD-RED-012`、`TDD-RED-015`、`TDD-RED-033` |
| B6 | `AC-ROUTE-*`、`AC-PI-005`、`AC-VIEW-*`、`AC-BALLOG-001`、`AC-REPLAY-*`、`AC-BEN-007` 至 `AC-BEN-010`、`AC-BEN-012`、`AC-BEN-016` 至 `AC-BEN-019`、使用者可解释性矩阵、`RED-003`、`RED-016`、`RED-017`、`RED-036`、`RED-043`、`RED-044`、`RED-048`、`RED-049`、`RED-054`、`RED-056`、`RED-058`、`RED-060`、`RED-062` 至 `RED-066` | Route Replay、支付工具换绑后原路径回放、权益快照回放、权益快照事实源一致性、补充权益事实、交易投影、运营时间线、余额日志、证据脱敏、规则核验、`DSL-BENEFIT-REFUND-NO-COUPON-001`、`DSL-BENEFIT-REFUND-RETAIN-SUBSIDY-001`、`DSL-BENEFIT-MISSING-SNAPSHOT-REPLAY-001`、`DSL-BENEFIT-SUPPLEMENTAL-FACT-001`、`DSL-BENEFIT-AUDIT-EVIDENCE-001`、`DSL-BENEFIT-EXPLAINABLE-VIEW-001`、外部规则核验状态 | `TDD-ROUTE-*`、`TDD-ROUTE-013`、`TDD-RACE-009`、`TDD-RACE-012`、`TDD-VIEW-*`、`TDD-REPLAY-*`、`TDD-BEN-REFUND-*`、`TDD-BEN-REPLAY-001` 至 `TDD-BEN-REPLAY-004`、`TDD-BEN-RACE-001`、`TDD-BEN-RED-002`、`TDD-BEN-RED-007` 至 `TDD-BEN-RED-009`、`TDD-BEN-RED-018`、`TDD-BEN-RED-021`、`TDD-BEN-RED-023`、`TDD-BEN-RED-026` 至 `TDD-BEN-RED-030`、`FundsOperationExplainabilityTests`、`FundsOperationPermissionBoundaryTests`、`TDD-RED-003`、`TDD-RED-010`、`TDD-RED-013`、`TDD-RED-014`、`TDD-RED-029`、`TDD-RED-034` 至 `TDD-RED-037` |
| B7 | `AC-CLR-*`、`AC-SET-*`、`AC-SET-006` 至 `AC-SET-009`、`AC-REC-*`、`AC-ADJ-001`、`AC-BEN-011`、`AC-BEN-013`、`AC-BEN-015` 至 `AC-BEN-019`、使用者可解释性矩阵、`RED-030` 至 `RED-033`、`RED-037` 至 `RED-039`、`RED-057`、`RED-059` 至 `RED-066` | 可清分明细、清分批次、清算候选、清算批次、结算单、出款单、出款前准入门禁、外部非终态、金额不一致、出款解释状态、对账批次、差错单、追偿单独立对象、权益清分和权益对账拆分、伴随指令清分合并、补充事实对账解释、清结算工作台解释输出、证据脱敏、规则核验、`DSL-BENEFIT-CLEARING-RECONCILIATION-001`、`DSL-BENEFIT-COMPANION-INSTRUCTION-001`、`DSL-BENEFIT-SUPPLEMENTAL-FACT-001`、`DSL-BENEFIT-AUDIT-EVIDENCE-001`、`DSL-BENEFIT-EXPLAINABLE-VIEW-001`、`DSL-SETTLEMENT-RECONCILIATION-001`、`DSL-SETTLEMENT-PAYOUT-RESULT-001`、`DSL-SETTLEMENT-RECONCILIATION-ADJUST-001` 完整差错闭环 | `TDD-CLS-*`、`TDD-CLS-FLOW-*`、`TDD-SETTLE-*`、`TDD-SETTLE-004`、`TDD-SETTLE-005`、`TDD-RECON-*`、`TDD-BEN-CLS-*`、`TDD-BEN-RECON-*`、`TDD-BEN-OPS-*`、`TDD-BEN-DIR-007`、`TDD-BEN-REPLAY-004`、`TDD-BEN-RED-010`、`TDD-BEN-RED-017`、`TDD-BEN-RED-020` 至 `TDD-BEN-RED-030`、`FundsOperationExplainabilityTests`、`FundsOperationPermissionBoundaryTests`、`FundsRunbookSignalTests`、`PayoutPreflightTests`、`PayoutReceiptMismatchTests`、`PayoutExplainabilityTests`、`TDD-RACE-005` 至 `TDD-RACE-007`、`TDD-RACE-012`、`TDD-RED-020` 至 `TDD-RED-025`、`TDD-RED-033` |
| B8 | `AC-ARCH-*`、`AC-ARCH-008`、`AC-ARCH-009`、`AC-REPLAY-*`、`AC-RPT-*`、`AC-BEN-016` 至 `AC-BEN-019`、使用者可解释性矩阵、`RED-016` 至 `RED-019`、`RED-029`、`RED-034`、`RED-040` 至 `RED-042`、`RED-060`、`RED-062`、`RED-064` 至 `RED-066` | `DSL-GOVERNANCE-ARCHIVE-MANIFEST-001`、`DSL-GOVERNANCE-BALANCE-SNAPSHOT-001`、`DSL-GOVERNANCE-PROJECTION-REPLAY-001`、`DSL-GOVERNANCE-METRIC-SNAPSHOT-BOUNDARY-001`、`DSL-GOVERNANCE-BIG-DATA-ARCHIVE-BOUNDARY-001`、`DSL-BENEFIT-COMPANION-INSTRUCTION-001`、`DSL-BENEFIT-SUPPLEMENTAL-FACT-001`、`DSL-BENEFIT-AUDIT-EVIDENCE-001`、`DSL-BENEFIT-EXPLAINABLE-VIEW-001`、归档、余额重建、交易投影重放、差异报告、异常人工处理、大数据归档承接边界、指标只读、指标水位隔离边界、证据脱敏、规则核验和 Runbook 信号 | `TDD-ARCH-*`、`TDD-ARCH-009`、`TDD-ARCH-010`、`TDD-REPLAY-*`、`TDD-METRIC-*`、`FundsOperationExplainabilityTests`、`FundsOperationPermissionBoundaryTests`、`FundsRunbookSignalTests`、`TDD-RACE-008`、`TDD-RACE-010`、`TDD-RACE-012`、`TDD-BEN-RED-025` 至 `TDD-BEN-RED-030`、`TDD-RED-018`、`TDD-RED-019`、`TDD-RED-026` 至 `TDD-RED-028`、`TDD-RED-033` |
| P2 ACH 边界 | `ACH-BOUNDARY-001` 至 `ACH-BOUNDARY-006`、`AC-RAIL-002A` 至 `AC-RAIL-007` | ACH 边界文档、P2 DSL 准入卡、ExternalAccountRefSpec、外部规则核验字段、文件摘要、回单引用、对账差错和调账核销引用；不新增 ACH DSL 内核对象。 | `TDD-RAIL-002` 至 `TDD-RAIL-007`、`TDD-RED-030`、`TDD-RED-034`、`TDD-P2-GA-*`、`TDD-P2-ACQ-*`、`TDD-RECON-*`、`TDD-OPS-*` |

## 10. Execution Grant 候选模板

进入任一 MVP 编码任务前，由用户确认以下授权信息：

```text
授权任务：
允许写入范围：
禁止写入范围：
必须覆盖的 TDD 用例：
必须覆盖的 AC/DSL ID：
基线是否已冻结：是/否
工作树状态：clean / dirty，若 dirty 必须列出允许纳入和必须排除的变更
Phase 能力边界：Phase 1 contract-only / Phase 2 route-posting-replay / Phase 3 replay-clearing-reconciliation-projection-archive-governance
JSON 夹具级别：CONTRACT_ONLY / 资金流夹具 / 不涉及
权益快照事实源：RouteSnapshotSpec / 交易事实快照 / 独立权益快照表 / 等价不可变存储 / 不涉及
零实付表达：零金额主指令 / 伴随权益指令 / 阻断 / 不涉及
平台补贴表达：额外 route leg / 独立伴随指令 / 阻断 / 不涉及
独立伴随指令原子性：同事务 / 同业务组幂等加补偿 / Saga补偿 / 不涉及；失败补偿和投影合并策略：
储值预付口径：纳入并已确认 / 不纳入 / 阻断 / 不涉及
退款分摊粒度：商品行 / 比例 / 现金优先 / 权益优先 / 不可退权益优先 / 不涉及
退款分摊确定性规则：规则版本 / 分摊依据 / 稳定组件顺序 / 舍入模式 / 尾差归属 / 组件剩余额度版本 / 幂等摘要字段 / 并发保护 / 不涉及
历史无权益快照处理：失败 / 人工处理 / 迁移补录 / 只读差错 / 不涉及
历史权益补录事实模型：追加 supplemental benefit fact / 不允许补录 / 不涉及；补录来源、复核、digest 和撤销关系：
专业确认状态：财务 / 税务 / 会计 / 合规 / 法务 / 业务 / 不涉及；未确认时的降级处理：
审计证据包：确认方 / 确认时间 / 结论版本 / 适用范围 / 有效期 / 脱敏证据引用 / 撤销或变更处理 / 审计责任人 / 不涉及
使用者解释视图：用户账单 / 商户账单 / 运营时间线 / 财务对账视图 / 审计导出 / 不涉及；事实状态 / 展示状态 / 操作状态；防误导断言：
证据最小化：脱敏摘要 / 文件编号 / 外部 reference / 不涉及；禁止原文字段：
外部规则核验状态：规则来源 / 版本或发布日期 / 生效日期 / 适用主体或适用范围 / 适用法域 / 核验日期 / 确认方 / 确认状态 / 不涉及
出款前准入门禁：结算锁定 / 出款账户 / 收款端点 / 通道额度 / cutoff / 名单筛查 / 外部规则核验 / 负余额 / 准备金 / 对账差错 / 幂等 / 审批 / 不涉及；缺失、失败或未知处理：
外部非终态处理：accepted / submitted / message sent / processing / IN_TRANSIT / 不涉及；防止展示为到账成功的断言：
出款解释状态：事实状态 / 展示状态 / 操作状态 / 不涉及；缺失、待补证据、待人工处理、金额不一致或规则未确认的防误导断言：
运营补事实命令白名单：允许命令或等价事件 / 不允许 / 不涉及；来源单据 / 审批号 / 证据引用 / 幂等键 / 原事实引用 / 操作者 / 原因 / 可撤销边界：
治理物理落点：复用 governance-face/governance-impl / 扩展既有包 / 新增独立模块 / 不涉及；依赖方向 / 公共契约 / 表结构 / H2 schema / Mapper Entity 归属 / 边界测试 / 指标水位隔离测试：
允许修改公共契约：是/否
公共契约允许修改范围：
允许新增枚举或事件：是/否
允许新增服务入口：是/否
允许扩展 Request/Query/DTO：是/否
允许修改表结构：是/否
允许新增模块：是/否
是否影响架构 ADR：是/否
受影响 ADR：
是否触碰能力域边界：是/否
是否触碰事实端口层：是/否
架构边界测试范围：
人工确认点：
NFR 假设：
观测告警：
回滚或补偿：
基础验证命令：just mvn-version、just compile
专项验证命令：
交付方式：
```

## 11. B1-10 历史 Execution Grant 参考（已完成）

本节保留 B1-10 已执行授权的历史参考，不能直接作为新的开工授权。B1-10 已在 `6be9c99 feat: 固化权益快照稳定摘要契约` 完成交付，后续 `75b46ef` 和 `9db3eba` 已收敛请求摘要与稳定摘要支撑，`434f8a2` 已校准账务计划装配器长 ID 用例规范，`42100dc` 已固化权益生产准入门禁；仓库当前工程治理和测试门禁基线已推进至 `77bc9f4`。下一轮若继续修改权益契约、请求摘要、route/posting/replay、投影、归档、治理重放或生产主链路，必须另起目标 MVP 任务 Execution Grant。

```text
授权任务：B1 / B1-10 权益快照 DSL 契约
允许写入范围：tests/src/test/java/com/capte/funds/dsl；必要时 tests/src/test/resources/dsl-contract-cases；必要时 core/src/main/java/com/wind/integration/funds/spec/transaction、core/src/main/java/com/wind/integration/funds/model/transaction、core/src/main/java/com/wind/integration/funds/transaction/enums 中 FundsBenefitSnapshotSpec/FundsBenefitComponentSpec/FundsBenefitReferenceSpec/FundsBenefitRefundPolicySpec 和 FundsBenefit* 枚举的最小目标态契约修改
禁止写入范围：transaction-*、wallet-*、ledger-* 业务实现；Route Resolver、Posting Assembler、Route Replay、授权生命周期、清结算、对账、归档、指标实现；生产配置；外部通道适配
必须覆盖的 TDD 用例：TDD-BEN-001 至 TDD-BEN-007、TDD-BEN-RED-001、TDD-BEN-RED-002、TDD-BEN-RED-012、TDD-BEN-RED-017、TDD-BEN-RED-018
必须覆盖的 AC/DSL ID：AC-BEN-001、AC-BEN-012、AC-BEN-013、DSL-BENEFIT-SNAPSHOT-001；登记 RED-050 至 RED-059，其中 RED-058 只作为生产 Done 门禁，本历史授权只做契约层红线，不声明 route/posting/replay 已闭合
后续准入补充：AC-BEN-014 至 AC-BEN-019、RED-060 至 RED-066、TDD-BEN-008、TDD-BEN-009、TDD-BEN-DIR-006、TDD-BEN-DIR-007、TDD-BEN-REPLAY-003、TDD-BEN-REPLAY-004、TDD-BEN-RED-020 至 TDD-BEN-RED-030、TDD-RACE-012 是 B1-10 后新增的 Phase 2/3 准入门禁，不改变 B1-10 历史完成结论；继续实现含权益生产资金流时必须另起 Execution Grant。
基线是否已冻结：已冻结；设计冻结点为 30b1a00；B1-10 契约承载基线为 6be9c99；仓库当前代码基线截至 77bc9f4；本节只作历史参考，继续执行或调整 B1 仍需用户确认新的 Execution Grant
工作树状态：执行前必须复核；dirty 时未列入允许纳入范围的变更不得作为 Done 证据
允许修改公共契约：历史授权仅允许 B1-10 最小新增；后续再次修改公共契约必须重新确认，不允许删除或改写既有字段
公共契约允许修改范围：仅限新增 `FundsInstructionSpec#getBenefitSnapshot()` 默认空值方法、权益快照 Spec/Immutable model、权益枚举和必要 JSON 空值语义；不得改变 `amount`、`originalAmount`、`exchangeRate`、`reference`、`instrumentRef`、`externalAccountRef`、`contextVariables` 既有语义
允许新增枚举或事件：历史授权仅允许新增权益枚举，不新增交易事件；后续再次新增枚举或事件必须重新确认
允许新增服务入口：否
允许扩展 Request/Query/DTO：否
允许修改表结构：否
允许新增模块：否
是否影响架构 ADR：否；如新增公共枚举或调整 core 依赖方向，必须重新确认
受影响 ADR：ADR-002 core 作为资金语义内核
是否触碰能力域边界：否
是否触碰事实端口层：否
架构边界测试范围：如改动 core 依赖或公共 Spec 依赖方向，补充 `just test-boundary` 或等效静态检查
人工确认点：`benefitSnapshot` 字段命名和空值语义、权益枚举命名、退款处置枚举、`PLATFORM_DISPLAY_DISCOUNT` 与 `PLATFORM_SUBSIDY` 的语义边界、核心字段不得塞入 `contextVariables`
NFR 假设：本历史授权只做契约承载，不触碰生产并发、容量、外部回调、清结算批次或归档重放；若扩大范围必须重开授权
观测告警：本历史授权不新增生产告警；后续进入 route/posting/replay 或清结算消费时必须补权益快照缺失、摘要冲突和回放失败告警
回滚或补偿：本历史授权不写生产数据；公共契约变更若撤回，必须同步目标态文档、契约测试和版本迁移说明
基础验证命令：just mvn-version、just compile
专项验证命令：just test-one FundsBenefitSnapshotSpecTests tests；just test-one FundsDslJsonContractTests tests；必要时 just test-core
交付方式：每轮说明覆盖用例、修改文件、验证命令、验证结果和残余风险；未获 Git 授权时不自动提交
```

## 12. MVP 任务交付记录模板

## 12.1 准入模拟验证记录（2026-05-21）

本节是正式交付前的模拟记录，最新完成事实以 12.2 实际交付记录和当前 Git 基线为准。

本轮按 `B1-10 权益快照 DSL 契约` 做 TDD 模拟验证，结论如下：

| 检查项 | 结论 | 证据 |
| --- | --- | --- |
| 任务边界 | 只允许进入 Phase 1 契约承载；未进入 route、posting、replay、清结算或对账生产消费。 | 当前写入范围限制在 core DSL/model/enums、DSL JSON verifier、tests DSL 契约和 JSON 夹具；未修改 `transaction-*`、`wallet-*`、`ledger-*` 业务实现。 |
| 设计差异 | 已发现并修复代码缺少 `FundsBenefitAmountClosureRole` 的差异。 | DSL/TDD 要求只有 `ORDER_DISCOUNT_CLOSURE` 参与正向订单闭合，代码已补闭合角色枚举、组件字段、模型校验和 JSON 校验。 |
| TDD 红线 | 已覆盖无权益空值语义、核心字段不得塞入 `contextVariables`、金额闭合、闭合角色缺失或混用、组件唯一性、零实付边界、退款处置和当前营销规则输入拒绝；稳定摘要、请求态快照误判生产 Done 和闭合角色生产消费边界已纳入 B1-10 门禁。 | 模拟阶段 `FundsBenefitSnapshotSpecTests` 10 个用例通过；正式交付见 12.2，已扩展为 `FundsBenefitSnapshotSpecTests` 12 个用例和 `FundsDslJsonContractTests` 8 个用例通过；route/posting/replay 消费断言仍归后续 MVP 任务。 |
| 验证命令 | Java 21 下编译、目标测试和 PMD 均通过。 | `WIND_FUNDS_JAVA_HOME=... just compile`；`just test-one FundsBenefitSnapshotSpecTests tests`；`just test-one FundsDslJsonContractTests tests`；`just pmd`。 |
| 残余边界 | 只能声明 `B1-10 契约承载 Done`；B3/B4/B6/B7/B8 仍需各自 Execution Grant。 | 平台补贴、储值券、退款回放、授权占券、清结算、对账、投影、归档、冷热读取和治理重放尚未进入生产链路消费；解释视图、证据最小化和外部规则核验也只作为后续门禁。 |

准入结论：`B1-10` 已转为历史交付基线；若下一轮进入生产链路，必须重新声明目标 MVP 任务、写入范围、公共契约变更、表结构变更、测试矩阵和验证命令。

```text
任务切片：
覆盖 TDD 用例：
写入范围：
只读范围：
设计错漏：
已补设计：
测试新增：
实现变更：
验证命令：
验证结果：
残余风险：
下一批建议：
```

## 12.2 B1-10 实际交付记录（2026-05-21）

本轮按第 11 节 Execution Grant 完成 `B1-10 权益快照 DSL 契约` 的第一轮编码闭环，形成权益快照契约承载基线。

| 检查项 | 结论 | 证据 |
| --- | --- | --- |
| 任务 | 已完成 `B1-10 权益快照 DSL 契约`。 | 提交 `6be9c99 feat: 固化权益快照稳定摘要契约`。 |
| 写入范围 | 符合第 11 节授权范围。 | 写入 `core/src/main/java/com/wind/integration/funds/spec/transaction`、`core/src/main/java/com/wind/integration/funds/util`、`tests/src/test/java/com/capte/funds/dsl`、`tests/src/test/resources/dsl-contract-cases`；未写入 `transaction-*`、`wallet-*`、`ledger-*` 业务实现。 |
| 契约承载 | `FundsBenefitSnapshotSpec` 增加稳定摘要口径，避免 JSON 字段顺序、时间字段、运行态对象差异影响契约指纹。 | 新增 `FundsBenefitStableDigest`，`FundsBenefitSnapshotSpec#getStableDigest()` 输出稳定摘要。 |
| DSL 红线 | `fixtureLevel=CONTRACT_ONLY` 必须显式登记，避免把请求态样例误判为生产 Done。 | `FundsDslJsonContractVerifier` 增加 `fixtureLevel`、`scenarioCode`、`acceptanceIds`、`tddIds`、`systemDesignRefs` 和 `validation` 元数据校验。 |
| TDD 用例 | 已补稳定摘要、字段顺序、请求态样例、生产 Done 误判、闭合角色边界等契约测试。 | `FundsBenefitSnapshotSpecTests` 12 个用例通过；`FundsDslJsonContractTests` 8 个用例通过。 |
| 验证命令 | Java 21 下编译、目标测试、PMD 和空白检查通过。 | `just mvn-version`；`just compile`；`just test-one FundsBenefitSnapshotSpecTests tests`；`just test-one FundsDslJsonContractTests tests`；`just pmd`；`git diff --check`。 |
| 后续基线收敛 | 请求摘要、稳定摘要支撑、账务计划装配器长 ID、预算组默认周期 `LIFETIME`、账务事实断言、钱包/交易/治理边界、CAD 完整验证门禁、出款准入规则核验证据、编码准入设计冻结、提现/解冻红线、余额日志证据、路由事实边界、交易投影解释、权益回放摘要、治理重放差异校验、资金事实红线、敏感上下文阻断、上下文不可变、防御性拷贝、外部账户原文阻断、MVP/归档边界和设计交付口径已在后续提交中收敛，当前设计和任务基线提交点为 `9456ab6`，代码能力基线截至 `77bc9f4`。 | `75b46ef` 至 `5901265` 的历史基线证据，`5901265..77bc9f4` 的资金事实红线、敏感上下文和设计交付口径收敛证据，以及 `9456ab6` 的 A0 准入与代码基线对齐证据。 |
| 残余风险 | 只能声明 `B1-10 契约承载 Done`。 | route、posting、replay、授权占券、清结算、对账、投影、归档、冷热读取和治理重放消费仍在 B3/B4/B6/B7/B8 后续 MVP 任务；解释视图、证据最小化和外部规则核验仍在 B6/B7/B8 后续 MVP 任务。 |

## 13. A1 建议 Execution Grant

A0 只读核验通过后，当前建议优先确认 A1 直接交易事实红线。A1 的完整候选授权卡见 `docs/TDD设计/A1-直接交易事实红线准入卡.md`；该卡只作为下一步确认输入，不因写入本 Harness Plan 而自动授权编码。

| 项 | 候选口径 |
| --- | --- |
| 任务切片 | A1 直接交易事实红线。 |
| 业务问题 | 直接交易成功或失败后，是否能解释状态、金额、route、posting、ledger entry、余额投影、幂等和审计证据。 |
| 首批 Red | `A1-RED-001` 成功链路事实完整性；必要时补 `A1-RED-002` 失败无副作用和幂等冲突。 |
| 目标测试资产 | `FundsDirectTransactionFlowTests`、`DefaultLedgerPostingAssemblerTests`、`LedgerBalanceProjectionServiceImplTests`。 |
| 默认写入范围 | 先写 A1 目标测试资产；Red 证明缺口后，只允许在 `transaction-impl`、`ledger-impl` 做最小修复。 |
| 默认禁止范围 | face/core 公共契约、DDL/H2 schema、A2/A3/A4、B7/B8、P2、生产配置、外部协议和敏感数据处理。 |
| 验证命令 | `just mvn-version`、`just compile`、`just test-one FundsDirectTransactionFlowTests tests`，必要时 `just test-transaction`、`just test-business-flow`、`just test-boundary`，提交前 `just pmd` 和 `git diff --check`。 |
| 当前状态 | 候选准入卡已补齐，并已记录现有覆盖扫描与完整门禁复核；仍需用户确认后才成为实际 Execution Grant。 |

## 14. B2 建议 Execution Grant

B2 是后续直接交易、授权交易、余额控制、退款和权益资金流进入真实组合验证前的基础门禁。建议对应 MVP 任务只处理钱包账户、账本、余额投影和支付工具基础能力，不进入直接交易、授权交易或权益生产消费链路。

### 14.1 Round 0 开工核验

B2 开工前先做 Round 0，只核验不写入。Round 0 的输出是“是否建议进入 B2 Execution Grant”，不是代码完成结论。

| 核验项 | 必须确认 | 失败处理 |
| --- | --- | --- |
| 基线 | PRD、DSL、系分、TDD、OpenSpec、Harness Plan 和 Git 提交点已冻结或明确列为本任务附件。 | 未冻结时先提交或纳入 Execution Grant，不能开工。 |
| 工作树 | `git status --short` 中未提交变更已分类为允许读取、允许写入或忽略。 | 未分类变更不得作为 Done 证据；影响 B2 时先停下确认。 |
| 现有测试资产 | `ControlAccountLedgerInitializationTests`、`FundingAccountServiceImplTests`、`PlatformFundingAccountServiceImplTests`、`PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests`、`FundsSubjectBalanceQueryServiceImplTests`、`LedgerBalanceProjectionServiceImplTests`、`WalletLayerBoundaryTests` 可定位。 | 目标测试资产不存在或命名漂移时，先修正任务清单。 |
| 写入边界 | B2 只允许 wallet、ledger 和 tests 范围；`transaction-*`、权益消费、清结算、对账、归档和指标实现保持禁止写入。 | 需要越界时重新确认 Execution Grant。 |
| 验证环境 | `just mvn-version`、`just compile`、`just test-ledger`、`just test-boundary`、`just pmd` 可作为目标门禁。 | Maven/JDK/私服问题需记录环境限制，不能写成代码失败。 |

### 14.2 B2 最小 Red 包

| Red ID | 目标行为 | 目标测试资产 | 验证 |
| --- | --- | --- | --- |
| B2-RED-001 | 账户角色、显式建账和平台账户解析错误必须失败。 | `ControlAccountLedgerInitializationTests`、`FundingAccountServiceImplTests`、`PlatformFundingAccountServiceImplTests`。 | `just test-ledger`。 |
| B2-RED-002 | 支付工具状态、方向、账户能力和敏感数据治理错误必须失败。 | `PaymentInstrumentServiceImplTests`、`PaymentInstrumentRouteDslContractTests`。 | `just test-one PaymentInstrumentServiceImplTests tests`、`just test-boundary`。 |
| B2-RED-003 | 支出主体资金来源缺失、不唯一、币种不一致或优先级不确定必须失败。 | `SpendSubjectFundingRelationServiceImplTests`、`PaymentInstrumentRouteDslContractTests`。 | `just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-boundary`。 |
| B2-RED-004 | 余额查询初始化账本、修复余额、把余额日志当事实源或跨周期余额当可用余额必须失败。 | `FundsSubjectBalanceQueryServiceImplTests`、`FundsBalanceAssertionSupportTests`。 | `just test-one FundsSubjectBalanceQueryServiceImplTests tests`、`just test-boundary`。 |
| B2-RED-005 | posting 不平衡、分录缺周期、投影失败回滚账本事实或日志失败回滚投影必须失败。 | `PostingLedgerDslContractTests`、`DefaultLedgerPostingAssemblerTests`、`LedgerBalanceProjectionServiceImplTests`。 | `just test-one LedgerBalanceProjectionServiceImplTests tests`、`just test-ledger`。 |
| B2-RED-006 | wallet 直接创建资金交易、route snapshot 或 ledger entry 必须失败。 | `WalletLayerBoundaryTests`、`RouteResolverFactBoundaryTests`。 | `just test-boundary`。 |

### 14.3 B2 Done / Not Done 口径

| 项 | Done | Not Done |
| --- | --- | --- |
| 测试顺序 | B2 最小 Red 已先失败，再做最小 Green。 | 只补实现或只跑既有通过测试。 |
| 资金断言 | 账户、账本、账目、周期、posting、entry、projection、幂等和失败无副作用均被断言。 | 只断状态、数量或“不报错”。 |
| 写入范围 | 只改 B2 授权范围。 | 触碰交易编排、权益消费、清结算、对账、归档、指标或未授权公共契约。 |
| 安全边界 | 支付工具敏感信息脱敏、绑定历史审计、wallet 边界和余额查询只读均有证明。 | 敏感字段、审计或只读边界无测试证据。 |
| 验证 | 目标 `test-one`、`just test-ledger`、`just test-boundary`、`just pmd` 和 `git diff --check` 通过，或明确环境限制。 | 验证缺失、失败未解释或被包装成通过。 |

```text
授权任务：B2 / 钱包账户、账本和余额投影基础
允许写入范围：wallet/wallet-face、wallet/wallet-impl、ledger/ledger-face、ledger/ledger-impl、tests/src/test/java；必要时只读 transaction-*、core 和 tests/src/test/resources/jdbc-schema.sql 用于确认既有契约与表结构
禁止写入范围：transaction-* 业务实现；直接交易、授权交易、余额控制交易编排；Route Resolver、Posting Assembler、Route Replay；权益 route/posting/replay 消费；清结算、对账、归档、指标实现；生产配置；外部通道适配
必须覆盖的 TDD 用例：TDD-WALLET-*、TDD-ROUTE-011、TDD-ROUTE-012、TDD-LEDGER-*、TDD-VIEW-003
必须覆盖的 AC/DSL ID：AC-PI-001、AC-PI-002、AC-PI-003、AC-PI-004、AC-PI-006、AC-PI-007、AC-CTRL-009、AC-CTRL-010、AC-CTRL-011、AC-BALLOG-001、RED-036、RED-046、RED-047、RED-049
基线是否已冻结：已冻结；当前设计和任务基线提交点为 9456ab6，代码能力基线截至 77bc9f4；本任务启动前必须复核工作树状态
工作树状态：执行前必须复核；dirty 时未列入允许纳入范围的变更不得作为 Done 证据
允许修改公共契约：待用户确认；建议默认不删除、不改写既有 face/core 字段，只允许为账户、账本、支付工具基础能力做非破坏性新增或校验补齐
公共契约允许修改范围：如确需变更，只限 wallet/ledger face 中账户角色、账本创建、账期、支付工具绑定和资金账户关系的目标态字段；不得调整交易指令、权益快照、直接交易或授权交易请求语义
允许新增枚举或事件：否；如账户角色、账本周期或支付工具状态缺口必须新增，需重新确认
允许新增服务入口：否；优先复用既有 wallet/ledger 服务入口完成用例闭环
允许扩展 Request/Query/DTO：待用户确认；建议默认不扩展交易 Request/DTO，wallet/ledger 基础对象如确需扩展必须保持可选、语义明确并覆盖测试
允许修改表结构：否；如 H2 测试表与生产模型缺口阻断本任务验收，先停下补充表结构授权
允许新增模块：否
是否影响架构 ADR：否；如改变 core、wallet、ledger 依赖方向或事实归属，必须重新确认
受影响 ADR：ADR-002 core 作为资金语义内核；账本事实边界和 wallet 产品门面边界
是否触碰能力域边界：是，仅限 wallet 账户/支付工具门面与 ledger 账本事实/投影边界
是否触碰事实端口层：是，仅限 ledger 账本事实、账期和余额投影；不得反向持有业务交易生命周期状态
架构边界测试范围：`just test-ledger`、`just test-boundary`，以及与本任务修改对象对应的 `just test-one <TestClass> tests`
人工确认点：账户角色命名与创建规则、显式账本创建边界、账期与分录平衡断言、余额投影事务边界、支付工具绑定历史审计、funding relation 与账户主体关系、是否需要表结构授权
NFR 假设：本任务只做本地服务层与 H2 验证，不处理生产并发容量、外部通道回调、清结算批次或权益回放告警
观测告警：本任务不新增生产告警；后续交易主链路和权益消费任务再补缺失快照、余额投影滞后、摘要冲突和回放失败告警
回滚或补偿：本任务如修改公共契约必须同步目标态说明；如新增测试表字段，必须同步说明生产迁移前置条件
基础验证命令：just mvn-version、just compile
专项验证命令：just test-ledger；just test-boundary；必要时 just test-one <相关测试类> tests；提交前 just pmd
交付方式：先补失败用例再实现，逐项声明覆盖的 TDD/AC/RED；完成后按 CAD 自动提交；未确认本授权前不得进入 B2 编码
```
