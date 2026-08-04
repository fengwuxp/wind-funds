# wind-funds 仓库发布就绪 Goal 执行规格

## Change Metadata

| Field | Value |
| --- | --- |
| Change ID | `funds-repository-release-readiness` |
| Goal ID | `019fc59d-b771-75b0-b5bc-6f9e366e30d8` |
| Goal state | `VERIFIED` |
| Current stage | `REPOSITORY_VERIFIED` |
| Date | `2026-08-04` |
| Baseline HEAD | `869888340504f0156aebdb4948083478762f7cc0` |
| Product readiness owner | `OWNER_APPROVED_2026-08-03` |
| Ledger contract owner | `OWNER_APPROVED_NEXT_SNAPSHOT_MIGRATION_2026-08-03` |
| Transaction/governance architecture owner | `OWNER_APPROVED_EVENT_FACT_SCAN_AND_W1-02A_2026-08-03` |
| Repository database evidence owner | `OWNER_APPROVED_H2_DDL_2026-08-04` |
| Engineering Maker | `wind-funds` maintainer or assigned implementation Agent |
| Independent Checker | `Peirce / W1_GATE_PASS; W2-01A_SECURITY_CHECKER_PASS; W2_GATE_PASS; W3_GATE_PASS` |
| Git strategy | `summary_only`，未授权 stage、commit、push 或 PR |

## 1. Current Decision

当前 Goal 状态是 `VERIFIED`，整体成熟度为 `REPOSITORY_VERIFIED`：Wave 1、Wave 2、Wave 3 均已准出。`W2-01A` 已在 RouteSnapshot JSON 持久化根边界递归校验 route、participant、leg、routing decision、payment instrument 和 external account 的敏感字段，自定义公共接口实现也不能绕过，并经独立 Checker 给出 `W2-01A_SECURITY_CHECKER_PASS`。`W2-01B` 已提供 tenant 有界事实扫描、双水位 checkpoint、持久任务/backlog、三种重放模式、差异摘要、影子/正式只读投影以及专属 H2/生产 DDL；`W2-02` 已复用真实 Spring/H2 装配证明 face/impl、Mapper、事务和 44 张表，并更新接入指南。Checker 复核中发现 tenant-aware Source/Writer 默认方法可能回退到无 tenant 旧入口，已补 Red 并在公共契约根边界改为 fail-closed，复验后给出 `W2_GATE_PASS`，P0/P1 为零。Owner 于 2026-08-04 裁决：`wind-funds` 是公共基础设施能力库，不持有独立目标 MySQL 接入；仓库数据库证据由 Spring/H2、生产 DDL 静态契约、44 表装配和全量门禁构成，实际数据库迁移、锁、隔离级别和性能由接入宿主验证。W3 Maker 门禁与独立 Checker 均已完成，最终结论为 `W3_GATE_PASS`、P0/P1 为零；该结论不能外推为 `HOST_INTEGRATION_VERIFIED` 或 `PRODUCTION_VERIFIED`。

仓库级发布就绪只证明：公共契约不允许绕过资金事实、关键投影可恢复、治理写操作受控、宿主装配契约可执行、H2/生产 DDL 静态契约和全量门禁通过，并由独立 Checker 复核。实际数据库、宿主部署、真实流量、生产指标和线上 Runbook 仍由接入方负责。

## 2. Goal Card

### Objective

把 `wind-funds` 从当前 L2、仓库发布待决状态推进到可独立复核的仓库级发布就绪资金能力库：统一就绪口径，关闭公共账本直接变更、交易投影持久恢复与治理重放控制面 P0/P1，补齐宿主集成契约和仓库数据库证据，通过全量验证与独立 Checker；不把仓库证据外推为宿主或生产已上线。

### Business Intent

- 接入方只能通过稳定资金契约创建和消费事实，不能绕过 posting、LedgerTransaction、LedgerEntry、幂等和审计直接改余额。
- 交易事实提交后即使进程崩溃或投影发布失败，也能从持久证据发现缺口并幂等恢复。
- `REBUILD_APPLY` 只修复只读投影，必须租户隔离、有界、有操作者、理由、审批或校验证据并可审计。
- 接入方能用可执行装配契约确认 impl、Mapper、数据源、事务和数据库表已正确接入，但仓库不替接入方实现业务宿主。

### Success Criteria

| ID | 完成条件 | 必须证据 |
| --- | --- | --- |
| `SC-RR-001` | 产品、系分、TDD、接入指南只使用分层就绪口径，不再把 H2 仓内验收写成无条件生产 `GO`。 | 文档一致性检索、`git diff --check`、Owner 复核。 |
| `SC-RR-002` | `ledger-face` 不再向外部调用方暴露可直接改变累计借贷额或物理删除账本的能力；余额只由正式 posting/投影路径驱动。 | 公共契约测试、真实 Spring/H2 账务流程、模块边界测试、兼容性裁决。 |
| `SC-RR-003` | 已提交交易的投影缺口可持久发现、限批恢复、幂等重放并暴露 backlog；失败不能回写交易、账本或余额事实。 | 崩溃窗口、发布失败、重复恢复、断点续跑和无副作用测试。 |
| `SC-RR-004` | 治理重放有生产 Source/Writer 装配；`VERIFY_ONLY` 无写副作用，`REBUILD_SHADOW` 只写影子，`REBUILD_APPLY` 缺授权证据时失败关闭。 | 服务级 H2 测试、公共契约测试、审计与租户隔离测试。 |
| `SC-RR-005` | 仓库提供一个最小宿主装配契约，能证明 face/impl、Mapper 扫描、数据源、事务和 44 张目标表的接入要求。 | `tests` 中真实 Spring Bean 装配测试和接入指南；不新增运行模块。 |
| `SC-RR-006` | 公共能力库用 H2 全量集成、生产 DDL 静态契约、测试 schema 对齐和 44 表装配证明仓库数据库边界；实际数据库专项不阻断仓库准出。 | `ReconciliationMysqlDdlContractTests`、`FundsHostCompositionContractTests`、H2 服务/并发测试和 Owner 决策；`test-mysql-reconciliation` 保留为采用 MySQL 的宿主可选证据。 |
| `SC-RR-007` | 聚焦测试、编译、PMD、边界测试和 `just verify-cad` 全绿，独立 Checker 未发现未关闭 P0/P1。 | 当前 HEAD 的命令输出摘要、文件清单、Checker 报告。 |

### Non-Goals

- 不新增独立 Spring Boot 应用、starter、Controller、RPC、运营台或完整 IAM 平台。
- 不在本 Goal 实现归档 Manifest、冷热切换、指标平台、余额重建全域或治理大平台。
- 不把 VCC、全球账户、ACH、收单、跨境、通道、合规或监管外部规则补成完整产品。
- 不把缺失的业务日志、`core` 依赖纯度和公共币种类型迁移混入 P0/P1；这些作为独立后续债务裁决。
- 不修改当前用户已有的 `pom.xml` 未提交差异；不执行 Git、联网、部署、生产、删除或不可逆动作。

## 3. Decision Register

| ID | 决策 | 状态 | Owner Gate |
| --- | --- | --- | --- |
| `D-RR-001` | 交付模型保持“资金能力库”；宿主装配用测试和指南证明，不创建独立服务。 | `CONFIRMED_BY_REPOSITORY_CONTRACT` | 架构 Owner 仅在真实宿主提出缺口时重新裁决。 |
| `D-RR-002` | 仓内生产调用者只剩 `LedgerBalanceProjectionServiceImpl`，物理删除没有生产调用；仓库没有稳定 tag/release，但 `rdc-snapshots` 已存在暴露三项危险方法的 `wind-funds-ledger-face:1.0.0-SNAPSHOT` 时间戳制品。推荐把下一 Snapshot 作为迁移边界，在首个仓库级稳定版本前移除公共直改/删除契约；余额变更直接收回既有 `ledger-impl` 投影路径，不新增透传 facade。 | `OWNER_APPROVED_IMPLEMENTED_CHECKER_PASS` | 下一 Snapshot 迁移决策已执行；Maker 与 Wave 1 Checker 门禁均通过。 |
| `D-RR-003` | 普通交易的仓库规范投影解释可从交易、明细、RouteSnapshot 和账本引用重建；`FREEZE`/`UNFREEZE` 走冻结事实。恢复面使用按事件类型读取的规范事实、双水位 checkpoint 和幂等投影 upsert，不新增通用 outbox。 | `OWNER_APPROVED_IMPLEMENTED_CHECKER_PASS` | W2-01A/B 已通过 Checker；历史缺快照记录和批次/冷数据范围明确不在本切片。 |
| `D-RR-004` | 治理重放采用持久任务控制面：`VERIFY_ONLY` 只比较，`REBUILD_SHADOW` 只写任务隔离影子，`REBUILD_APPLY` 必须有审批引用和已完成的同范围影子任务。 | `OWNER_APPROVED_IMPLEMENTED_CHECKER_PASS` | tenant/operator/reason/audit、限批 checkpoint、backlog、差异摘要和生产 Source/Writer 已实现并通过 Checker；宿主 IAM/触发/调度仍由接入方负责。 |
| `D-RR-005` | 分层状态统一为 `REPOSITORY_*`、`HOST_INTEGRATION_*`、`PRODUCTION_*`，禁止脱离层级使用裸 `GO`。 | `OWNER_APPROVED_CHECKER_PASS` | 继续以用户接入指南成熟度台账为单一口径；Git 打包前需处理该文件的前置暂存删除。 |
| `D-RR-006` | `wind-funds` 作为公共基础设施能力库不持有独立目标 MySQL 接入；H2、生产 DDL 静态契约、44 表装配和全量门禁构成仓库数据库证据，实际数据库兼容性由宿主负责。 | `OWNER_APPROVED_CHECKER_PASS` | 保留 MySQL 专项资产但不作为 `REPOSITORY_VERIFIED` 硬门禁；不得用 H2 声称 MySQL 锁、隔离、性能或迁移已验证。 |

## 3.1 Wave 0 Evidence

### W0-01 Readiness Vocabulary

- 用户接入指南 4.1 节是当前唯一成熟度台账，区分 `REPOSITORY_VERIFIED`、`HOST_INTEGRATION_VERIFIED` 和 `PRODUCTION_VERIFIED`。
- 产品分册只描述能力和产品边界，引用成熟度台账，不再自行声明工程 `GO`；全仓口径扫描同时覆盖产品验收矩阵。
- 系分与 TDD 中 H2/DDL 证据统一称为“仓内验收”；实际数据库、宿主和生产证据不能互相替代，也不反向阻断仓库级准出。
- Product readiness Owner 已确认公共能力库的仓库证据边界；`SC-RR-001` 已经 W3 独立 Checker 复核通过。

### W0-02 Public Ledger Mutation Surface

仓内调用清单已经闭合到源码级：

| 契约 | 生产调用 | 测试调用 | 结论 |
| --- | --- | --- | --- |
| `LedgerService#updateLedgerBalance` | 只有同模块 `LedgerBalanceProjectionServiceImpl` 一处，用于根据已生成的 `LedgerEntrySpec` 投影累计借贷额 | 多个测试直接调用以准备余额或构造 stub | 不需要继续作为 `ledger-face` 公共能力；实现应内收到既有投影路径，测试前置数据应走 posting 或同模块测试夹具。 |
| `LedgerService#deleteLedgerById(s)` | 无 | 当前检索未发现调用 | 资金事实不应物理删除；移除后不创建替代删除服务。 |
| 跨模块危险调用边界 | `transaction`、`wallet`、`reconciliation`、`governance` 均无调用 | `FundsModuleDependencyBoundaryTests` 已扫描上述生产源码 | 现有边界测试只能证明仓内调用者，不能证明仓外 Maven 消费者。 |

发布与兼容证据：

- Git 无 tag，提交历史未出现稳定 release；基线版本是 `1.0.0-jdk21-SNAPSHOT`，当前用户未提交的 `pom.xml` 把 revision 改为 `1.0.0-SNAPSHOT`，本 Goal 不触碰该差异。
- 本地 Maven 缓存的 `rdc-snapshots` 元数据证明 `wind-funds-ledger-face:1.0.0-SNAPSHOT` 存在 `20260727.052320-1` 时间戳制品；该制品字节码仍公开 `updateLedgerBalance`、`deleteLedgerById` 和 `deleteLedgerByIds`。
- `/Users/wuxp/Workspace/idea` 的本地源码检索未发现仓库外生产调用；这不是远端消费者清单，不能证明外部 Snapshot 消费者为零。
- 仓库已有 `@Deprecated(forRemoval = true)` + 契约测试的兼容惯例，可在 Owner 不能批准下一 Snapshot 破坏性迁移时复用；但仅标废弃不能满足 `SC-RR-002`。

Owner 只需裁决兼容策略，不需重新设计余额服务：

1. 推荐路径：批准下一 Snapshot 为迁移边界，本 Goal 直接删除三项公共方法和 `UpdateLedgerBalanceRequest`，把原子更新逻辑移动到 `LedgerBalanceProjectionServiceImpl`；迁移说明只指向正式 posting/application 入口。
2. 保守路径：先标记 `@Deprecated(forRemoval = true)` 并保留一个明确版本窗口；本 Goal 不能因此关闭，后续版本仍必须执行同一删除任务和完整验证。

两条路径都禁止新增一行透传 facade、双写接口或物理删除替代品。`W1-01` 的 Red 必须先证明 face 不再提供直改/删除能力，再证明正式 posting 的平衡、流水、分录、余额与幂等未退化。

`W1-01` 的最小 TDD/写入闭包已经固定：

1. Red：在 `FundsModuleDependencyBoundaryTests` 增加反射契约，证明 `LedgerService` 不声明三项危险方法；当前代码必须失败。
2. Green：删除 `LedgerService` 三项方法和 `UpdateLedgerBalanceRequest`；把现有原子余额更新逻辑移动到 `LedgerBalanceProjectionServiceImpl`，删除 `LedgerServiceImpl` 的余额直改和物理删除实现，不新增 helper/facade。
3. Fixture migration：所有测试余额准备改走正式 posting/资金流程；`LedgerServiceImplTests` 中余额状态用例迁移到投影/过账测试，不保留测试专用直改入口。
4. 验证：先运行 Red 所在切片，再执行 `just verify-slice LedgerServiceImplTests,LedgerBalanceProjectionServiceImplTests,FundsModuleDependencyBoundaryTests tests`、`just test-ledger`、`just test-boundary`、`just compile`。

### W0-03 Projection Reconstructability

当前仓库没有生产 `FundsTransactionProjectionPublisher` 实现，正常发布端口只把运行时上下文交给宿主扩展点。重启后的可重建性如下：

| 发布上下文 / 投影字段 | 持久来源 | 结论 |
| --- | --- | --- |
| 普通交易 `routeSnapshot` | `t_funds_transaction.route_snapshot`；`FundsTransactionQueryService#findRouteSnapshotByTransactionSn` 已提供回读 | 可重建当前规范声明的强类型稳定字段；不承诺任意运行时 Map 值保留原 Java 类型。 |
| 普通交易 `resolvedRoute` | `DefaultRouteSnapshotFactory` 把 ResolvedRoute 的规范稳定字段复制进 RouteSnapshot | 可在规范解释语义内由 RouteSnapshot 确定性恢复，不需要重新路由。 |
| 普通交易 `lifecycleResult` | `t_funds_transaction` 聚合状态、`t_funds_transaction_detail` 明细状态/流水/`ledger_transaction_sn` | 可重建完成态、交易/明细流水和账本引用。 |
| `FREEZE` route/lifecycle/ledger | `t_funds_frozen_order.context_variables` 保存本次 RouteSnapshot；冻结单号作为 lifecycle `transactionSn`，`freeze_ledger_transaction_sn` 保存账本引用 | 可从冻结事实重建规范稳定字段，但必须走 `findRouteSnapshotByFreezeOrderSn`/冻结事实读取，不能套用普通交易查询。 |
| `UNFREEZE` route/lifecycle/ledger | 新建解冻记录保存 request hash、原冻结单引用、状态、本次 `freeze_ledger_transaction_sn` 和本次 RouteSnapshot | 新记录可从解冻事实恢复本次 route/lifecycle/ledger；历史记录仍可能缺快照，且沿原冻结单只能得到原冻结路径，不能替代本次解冻快照。 |
| 账本事实 | 普通交易明细的 `ledger_transaction_sn`，冻结/解冻记录的 `freeze_ledger_transaction_sn`，以及 Ledger 查询契约 | 有账务影响时可回读账本事实并重建规范解释所需字段，不承诺逐对象复刻原 `LedgerTransactionSpec`；无账务影响或拒绝场景必须允许为空。 |
| 普通交易规范 `FundsTransactionProjectionExplanation` | 交易、明细、RouteSnapshot 和账本引用；`DefaultFundsTransactionProjectionExplainApplicationService` 按普通交易、`FREEZE`、`UNFREEZE` 分别读取持久事实 | 当前三类事件均可重建规范投影；历史缺少本次解冻快照的记录仍不能补造。 |
| 完整 `FundsInstructionSpec` | 交易/明细/RouteSnapshot 只保存部分字段 | 不可精确重建：运行时 `WindOperator` 按契约不持久化，原始 `eventTime` 与部分 direct instruction context 无稳定回读保证。 |
| `viewDomain`、`ownerType/ownerId`、`displayType` | `viewDomain` 来自持久重放任务；主体、事件和展示状态来自规范 explanation | 可重建当前仓库规范视图；宿主专有视图仍由接入适配器负责。 |
| backlog / checkpoint | `t_projection_replay_task` 持久任务保存普通交易与冻结事实双水位 checkpoint，并提供 tenant 有界 backlog | 已实现限批续跑；事实加载或写入失败时事务回滚，checkpoint 不前移。 |

证据结论：不能用现有事实精确复刻任意 `FundsTransactionProjectionPublishContext`，但当前仓库规范投影不再依赖该运行时对象。`W2-01` 已采用持久事实推荐路径：普通交易、`FREEZE`、`UNFREEZE` 通过事件专用读取进入 tenant 有界扫描和双水位 checkpoint；新建解冻记录保存本次 route。历史缺快照记录、batch/cold data 与宿主专有视图不在本切片。

未选择通用 outbox：现有持久事实已覆盖当前规范投影，不为尚无调用方的完整运行时 Publisher 上下文增加第二份事实。若未来出现该兼容要求，须独立评审最小、脱敏、可版本化的 pending/outbox，且不得序列化 `WindOperator` 或敏感 context。

仅把恢复责任继续留给宿主不满足本 Goal 的 `SC-RR-003`，不作为第三个准出方案。

### W0-04 Governance Replay Control Plane

W2-01B 在既有单批重建算法上补齐了持久控制契约、限批续跑和仓库内生产装配：

| 控制项 | 权威设计 | 当前实现 | 结论 |
| --- | --- | --- | --- |
| 身份与审计 | create 要求 `requestSn/requestDigest/tenantId/reason/auditRef/operatorRef`，正式写另需 `approvalRef` | 持久任务保留请求流水/摘要、tenant、理由、审计/审批引用和稳定操作者身份；同租户请求幂等 | 仓库校验引用存在并留证；引用真实性与 IAM 授权由宿主负责。 |
| 范围与批次 | 有界范围、`maxBatchSize`、独立 checkpoint、续跑 | tenant 有界事实扫描使用普通交易与冻结事实双水位，单批上限 500，任务持久 checkpoint/backlog | 支持失败回滚和下一次续跑；当前明确拒绝 batch/cold data 范围。 |
| 模式门禁 | `VERIFY_ONLY` 不写；`SHADOW` 只写影子；`APPLY` 必须有审批和已验证影子任务 | 三种模式由持久任务执行；APPLY 校验审批引用及同 tenant、同范围、已完成影子任务 | 缺少控制证据时失败关闭，正式与影子投影隔离。 |
| 生产装配 | 只读事实 Source、影子/正式 Writer、任务与差异证据 | Spring Bean、Mapper、事务和 H2/MySQL 专属 DDL 已装配；差异只持久化 SHA-256 摘要 | 当前达到仓库 L2 与宿主装配契约证据；实际数据库和真实宿主由接入方验证。 |

已按最小风险分两步完成：

1. `W1-02` 检查点 A 已使未受控写模式失败关闭。
2. `W2-01B` 检查点 B 已补持久任务、租户/操作者/理由、限批 checkpoint、生产 Source/Writer 和审批/影子证据门禁。

不新建通用 IAM、审批或治理平台；仓库只校验稳定引用并持久保留执行证据，引用真实性、自动任务创建、调度、告警和人工差异复核由宿主负责。

`W1-02` 检查点 A 的最小 TDD/写入闭包已经固定：

1. Red：把现有 shadow/apply 成功写测试改为 `testShadowReplayWithoutControlPlaneShouldFailClosed` 和 `testApplyReplayWithoutControlPlaneShouldFailClosed`；断言失败发生在 Source/Writer 调用前，compare、shadow、official 均无记录。
2. Green：`FundsProjectionReplayService#replay` 在任何事实读取前只允许 `VERIFY_ONLY`；两种写模式返回稳定的“控制面尚未开放”错误。仅修改该服务和专属测试，不改 Request、DDL、Spring 装配或其他模块。
3. 验证：`just test-one FundsProjectionReplayServiceTests tests`、`just test-governance`、`just test-boundary`、`just compile`。
4. 停止条件：以上为检查点 A 的历史验收形态；检查点 B 已实现并通过独立 Checker，不据此外推实际数据库或生产准出。

### Wave 1 Owner Decision Request

推荐一次确认以下执行包：

1. 接受 4.1 唯一成熟度词汇，关闭 `D-RR-005`。
2. 接受下一 Snapshot 作为 Ledger 破坏性迁移边界，执行 `W1-01` 推荐路径；不把三项危险 API 带入首个仓库级稳定版本。
3. 投影选择按事件规范事实扫描并最小补齐 `UNFREEZE` 本次 route；治理先执行 `W1-02` 检查点 A 失败关闭，再单独审批公共契约/DDL 后执行检查点 B。

该执行包只授权 Wave 1 的 Red、Ledger 内收和治理检查点 A；不授权 Git、联网、发布、生产、删除数据库对象、目标 MySQL 或 Wave 2 DDL。

### Blocked Audit And Recovery

同一 Owner blocker 已连续出现 3 个 Goal 回合：首次 Wave 0 收口确认 Owner Gate；下一回合补齐远端 Snapshot、TDD 和写入闭包后请求唯一执行包；本回合仍未收到批准，且继续实现会越过公共契约、资金行为和治理写模式授权边界。只读事实和计划证据已经充分，继续扩展分析不能关闭任何 Success Criteria，因此 Goal 转为 `BLOCKED_OWNER_DECISION`。

Owner 已于 `2026-08-03` 回复“批准 Wave 1 推荐执行包”。Goal/Wave 已恢复为 `ACTIVE/IN_PROGRESS`，按 `W1-01` 与 `W1-02` 检查点 A 的 Red 顺序执行；其他权限仍保持原边界。

## 3.2 Wave 1 Execution Evidence

### W1-01 Ledger Contract Closure

- Red：`just verify-slice FundsModuleDependencyBoundaryTests tests` 执行 23 个测试，新增契约断言单点失败，并准确列出 `updateLedgerBalance`、`deleteLedgerById`、`deleteLedgerByIds`。
- Green：删除 `LedgerService` 三项危险方法和 `UpdateLedgerBalanceRequest`，将状态校验、Java + SQL 最小余额约束、版本与状态乐观锁和 `version + 1` 原子更新内收到 `LedgerBalanceProjectionServiceImpl`；未新增 facade、兼容双写或物理删除替代品。
- 调用闭合：生产与测试 Java 源码对三项方法和 Request 的检索为零；OpenSpec 历史证据和边界测试字符串不属于调用点。
- 聚焦证据：核心切片 `37/37`，直接调用迁移切片 `85/85`，`just test-ledger` 为 `52/52`。

### W1-02A Governance Fail-Closed

- Red：`just test-one FundsProjectionReplayServiceTests tests` 执行 19 个测试，新 shadow/apply 失败关闭场景 2 个单点失败。
- Green：`FundsProjectionReplayService#replay` 在 Source 读取和 Writer 比较/写入前只允许 `VERIFY_ONLY`；shadow/apply 测试同时断言 Source 调用为 0，compare、shadow、official 记录均为空。
- 聚焦证据：专属测试与 `just test-governance` 均为 `19/19`。

### W1-03 Boundary Test Isolation

- Red：独立 `just test-one RouteAccountHierarchySnapshotAppenderTests tests` 为 4 个测试中 1 failure、3 error，异常均来自 `WindOperatorFactory.system()` 对未初始化 Spring application context / static security access operations 的隐式依赖；其中预期业务异常也被该基础设施异常覆盖。
- Green：新增一个直接使用 `WindOperator.builder()` 的无 Spring、deny-all 测试操作者夹具，仅替换 4 个纯 route 测试的操作者构造；未修改生产源码、Spring 基类、资金行为、公共契约或依赖。
- 聚焦证据：单类 `4/4`，四类联合 `23/23`；route 测试目录对 `WindOperatorFactory.system()` 的检索为零。

### Cross-Cutting Verification And Checker

- `just compile`：21 个 reactor 模块成功。
- `just pmd`：21 个 reactor 模块成功。
- `just verify-cad`：总退出码 0；1014 个测试中 0 failure、0 error、1 skipped，跳过项为缺少外部目标 MySQL 环境的 `ReconciliationMysqlMigrationIntegrationTests`；PMD、classfile 和 codegen 门禁随命令完成。
- `git diff --check` 与 `git diff --cached --check`：通过。
- 独立 `just test-boundary`：修复前 194 个测试中 8 failure、12 error；修复后 194/194，0 failure、0 error、0 skipped。
- 修复后 `just compile` 与 `just pmd`：21 个 reactor 模块成功；修复后 `just verify-cad`：1014 个测试中 0 failure、0 error、1 skipped，PMD、classfile 和 codegen 门禁成功。
- 独立 Checker：首轮因原始 boundary 证据被后续 CAD 报告覆盖而给出 `W1_GATE_FAIL`；随后亲自新鲜执行 `just test-boundary`、`just compile`、`just pmd`，确认分别为 194/194、21 模块成功、21 模块成功，最终给出 `W1_GATE_PASS`，P0/P1 为零。
- 前置工作树：`docs/用户接入指南/README.md` 的已暂存删除在本轮开始前已存在，本轮未修改索引或该文件；它不归因于 Wave 1，但会阻断未来按白名单打包。

停止结论：Boundary Test Owner 已批准并完成最小隔离修复，Maker 与独立 Checker 门禁全绿；Wave 1 已准出，当前停止在未授权的 Wave 2 边界，不执行 Git。

## 3.3 Wave 2 Execution Evidence

### W2-01A UNFREEZE Persistent Route Fact

- 授权边界：Owner 回复“继续”后进入 Wave 2；本切片复用冻结单既有 `context_variables` 和 `RouteSnapshotJsonSupport`，不新增公共 API、表、依赖、状态或抽象。
- Red：`DefaultFundsFrozenOrderLifecycleSaverTests` 新用例精确失败于解冻记录缺 `routeSnapshot`；真实 Spring/H2 发布失败用例精确失败于 `findRouteSnapshotByFreezeOrderSn` 返回空。沙箱内 Mockito self-attach 失败单独记录为环境问题，不作为代码 Red。
- Green：`DefaultFundsFrozenOrderLifecycleSaver#createUnfreezeRecord` 在原事务内把本次 `RouteSnapshot` 写入新建解冻记录；发布失败后清空内存上下文，仍可从 H2 解冻事实回读 `UNFREEZE/RELEASE` route，读取不改变账本余额和分录事实。
- Maker 验证：两类聚焦测试 `9/9`，`just test-transaction` 为 `127/127`；`just clean-compile` 与 `just pmd` 均为 21 个 reactor 模块成功；`git diff --check` 通过。首次联合验证暴露旧 `target` 缺编译产物，刷新可丢弃构建产物后通过，不归因于源码失败。
- Maker Review：首轮三文件审查未发现 P0-P3，但其“既有规范化路径足以阻断敏感字段”结论被独立 Checker 推翻。仓内 `ImmutableRouteSnapshotSpec`、leg、participant、routing decision、payment instrument 和 external account 构造器已有敏感字段校验；默认快照工厂却浅复制公共接口的嵌套实现，JSON 持久化边界未重新校验，因此宿主自定义 Route 实现可形成旁路。
- 安全修复：`RouteSnapshotJsonSupport` 在 JSON 持久化根边界递归校验 route、participant、leg、routing decision、payment instrument 和 external account 的稳定字段及 context；自定义公共接口实现注入敏感字段时写入前失败，脱敏引用仍保持兼容。
- Independent Checker：`W2-01A_SECURITY_CHECKER_PASS`，P0/P1 为零；Checker 确认根边界覆盖所有当前写入调用者，相关源码未在复核期间漂移。
- 聚焦证据：安全 helper `4/4`、冻结生命周期 saver `3/3`、发布失败 H2 持久回读 `6/6`，合计 `13/13`。
- 残余：历史 `UNFREEZE` 记录仍可能缺本次快照；该历史数据兼容不在 W2 最小切片内。

### W2-01B Persistent Replay Control Plane

- 公共契约：`transaction-face` 提供 tenant、事件/主体/时间范围和稳定 cursor 的规范事实限批扫描；`governance-face` 提供 create/run/get/backlog、checkpoint、审计引用和事实批次 DTO，不暴露 Entity、Mapper 或实现类。
- 持久控制面：governance 任务使用请求幂等摘要、双水位 checkpoint、限批续跑和 backlog；事实加载失败时事务回滚，任务保留在可重试状态且 checkpoint 不前移。
- 模式边界：`VERIFY_ONLY` 只比较并保存差异摘要；`REBUILD_SHADOW` 只写任务隔离影子；`REBUILD_APPLY` 必须提供审批引用和已完成的同 tenant、同视图、同范围影子任务。
- 写入边界：生产 Source/Writer 只读取 transaction 持久事实并写治理只读投影、任务和差异表，不回写交易、账本、余额、清结算或对账事实；差异值只保存 SHA-256 摘要。
- 范围边界：当前热交易投影支持事件/主体/时间范围；batch/cold data 明确失败关闭，不在 W2 承诺内。
- 数据库资产：H2 与 `database/mysql/governance/001_create_governance_tables.sql` 均新增任务、差异、交易投影 3 张表；静态 DDL 契约证明三份生产 DDL 与 H2 共 44 张表一致且无破坏性回滚。未执行 MySQL 专项，按 Owner 决策属于宿主可选兼容证据。
- 聚焦证据：`just test-governance` 为 `24/24`，其中持久控制面 `20/20`、宿主装配 `4/4`；`ReconciliationMysqlDdlContractTests` 为 `7/7`。
- Checker 修复：tenant-aware Source/Writer 五个入口的默认实现不再回退到无 tenant 旧入口，统一失败关闭；生产实现显式覆盖，新增契约测试后 Checker 给出 `W2_GATE_PASS`，P0/P1 为零。

### W2-02 Minimal Host Composition Contract

- 复用 `AbstractFundsServiceTest` 的真实 Spring/H2 上下文，验证 transaction/governance face/impl Bean、全部 Mapper、数据源、事务管理器和 44 张表；未新增 starter、运行模块、Controller 或 Fake 业务实现。
- 已从 `HEAD` 恢复并更新 `docs/用户接入指南/README.md` 工作树文件，明确依赖、Bean/Mapper/schema、任务调用顺序及宿主 IAM、触发、调度、告警和差异人工复核责任。
- Git 索引未获授权且未修改：该路径仍保留前置 staged delete，同时工作树恢复文件显示为 untracked；后续打包必须单独处理。
- Maker 收口：`just test-transaction` 为 `129/129`，`just test-boundary` 为 `194/194`，`just compile` 与修复后 `just pmd` 均为 21 个 reactor 模块成功，`just verify-cad` 为 `1024` 个测试、0 failure、0 error、1 skipped；唯一跳过项为宿主可选的 MySQL 迁移兼容测试。PMD、classfile 和 codegen 门禁随 CAD 成功。
- W2 收口点：W2-01B/W2-02 已通过独立 Checker；Owner 随后关闭目标 MySQL 仓库门禁分叉并进入 W3 H2/DDL 全量复验。Git、宿主实际数据库、真实部署和生产证据均未执行。

## 4. Plan-to-Goal Bridge

| Goal 缺口 | 对应任务 | 准出标准 |
| --- | --- | --- |
| 就绪口径互相冲突 | `W0-01` | `SC-RR-001` |
| 公共账本契约允许绕过账务事实 | `W0-02` -> `W1-01` | `SC-RR-002` |
| afterCommit 投影失败只记录日志 | `W0-03` -> `W2-01` | `SC-RR-003` |
| 治理重放只有骨架和测试 Fake，正式覆盖缺控制证据 | `W0-04` -> `W1-02` | `SC-RR-004` |
| 接入指南缺少可执行宿主装配证明 | `W2-02` | `SC-RR-005` |
| 公共能力库数据库证据边界已由 Owner 裁决 | `W3-01` | `SC-RR-006` |
| 最终全量证据与独立复核已完成 | `W3-02` -> `W3-03` | `SC-RR-007` |

## 5. Plan Grant

当前 Plan Grant 为 `WAVE_3_VERIFIED`：Owner 已批准并完成公共能力库以 H2、生产 DDL 静态契约和独立 Checker 形成仓库准出。以下动作仍未授权：

1. 运行会创建/删除对象的 MySQL 专项，或连接任何共享/生产数据库；该专项不再是当前 Goal 的完成前置。
2. 扩展批次/冷数据、历史缺快照补偿、通用 IAM/审批、自动任务创建、调度、告警或归档/指标平台。
3. 其他改变真实资金、余额、幂等、审批、审计、安全或合规行为。
4. 修改本任务白名单外文件、接触当前 `pom.xml` 用户差异，或引入新模块。
5. Git stage/commit/push/PR、联网、部署、生产操作和删除动作；尤其不得擅自处理用户指南的前置 staged delete。

授权可由用户随时撤销。任一任务触发上述边界，Goal 保持 `ACTIVE`，对应任务转 `BLOCKED_OWNER_DECISION`，不得借“自主推进”扩大权限。

## 6. Execution Loop

| Field | Contract |
| --- | --- |
| Loop ID | `WF-RR-LOOP-01` |
| 上下文账本 / 阶段状态 | 本 OpenSpec 的 Change Metadata、Decision Register、Wave 状态和 Goal Ledger |
| 原子任务 / 写入文件 | 每个 Task ID 只修改任务表声明的写入范围，不跨任务共享未声明文件 |
| 只读参考 | 当前源码、测试、DDL、权威设计、工作树差异和 Owner 决策 |
| 单任务循环 | 先补 Red/契约断言 -> 最小实现 -> 聚焦验证 -> Maker Review -> 独立 Checker |
| 验证矩阵 | Success Criteria -> Plan-to-Goal Bridge -> Wave 准出证据 |
| 最大空转 | 同一阻断连续 2 轮无新增证据即停止，并回写所缺 Owner/环境/权限 |
| 阶段推进 | 只有前置 Gate 和本 Wave 准出证据完成，下一 Wave 才可从 `PLANNED` 转 `READY` |
| 恢复入口 | 重新读取 `AGENTS.md`、本文件、`git status --short --branch`、`git diff -- pom.xml` 和最新验证摘要 |
| 回滚提示 | 每个任务保持独立最小 diff；公共契约和 DDL 不与其他任务合并，失败时仅撤销本任务白名单内变更 |

## 7. GSD Waves

### Wave 0: 口径与高风险决策

Wave 状态：`OWNER_APPROVED`。四项 Maker 证据和独立 Checker 已完成，Owner 已批准推荐执行包；转入 Wave 1 实现。

| Task ID | 状态 / Owner | 写入范围 | 只读范围 | 完成条件与验证命令 | 停止条件 / 交接要求 |
| --- | --- | --- | --- | --- | --- |
| `W0-01` 就绪口径统一 | `OWNER_APPROVED` / 产品 readiness Owner + 文档 Maker | `docs/产品设计/03-清结算与对账.md`、`docs/产品设计/05-产品验收与TDD用例矩阵.md`、`docs/系分设计/03-清结算与对账系分设计.md`、`docs/TDD设计/支付资金底座测试驱动设计.md`、`docs/用户接入指南/README.md`、本 OpenSpec | `docs` 全量 `GO/PENDING/生产验收` 命中 | 分层词汇一致且不降低现有红线；`rg` 一致性检查、`git diff --check` | 不得把仓库证据外推为宿主/生产；Git 打包前必须裁决用户指南前置暂存删除。 |
| `W0-02` 公共账本变更面裁决 | `OWNER_APPROVED_W1_IMPLEMENTED` / Ledger contract Owner | 本 OpenSpec；确认后另开最小代码 Task | `LedgerService`、`UpdateLedgerBalanceRequest`、`LedgerServiceImpl`、`LedgerBalanceProjectionServiceImpl`、所有调用点、远端 Snapshot 元数据和公共契约测试 | 已证明远端 Snapshot 暴露危险 API、仓内无外部生产调用，并固定推荐迁移边界与 Red/Green 闭包 | 下一 Snapshot 迁移已执行；Wave 1 边界门禁修复前不做阶段准出。 |
| `W0-03` 投影可重建性证明 | `OWNER_APPROVED_W2_IMPLEMENTED_CHECKER_PASS` / Transaction architecture Owner | 本 OpenSpec | 普通交易、冻结/解冻、route snapshot、ledger transaction/entry、projection context 和现有 H2 schema | 新建 `UNFREEZE` 保存本次 route，持久化根边界阻断自定义实现的敏感字段旁路；有界扫描/checkpoint 和治理 DDL 已实现 | W2-01A/B Checker 已通过；历史缺快照记录不在当前切片。 |
| `W0-04` 治理重放控制契约裁决 | `OWNER_APPROVED_W2_IMPLEMENTED_CHECKER_PASS` / Governance + Security Owner | 本 OpenSpec；确认后另开最小公共契约 Task | 系分 04、governance face/impl、`FundsProjectionReplayServiceTests` | 已完成契约差距、生产装配差距和两阶段失败关闭 AC | 检查点 A/B 均已完成并通过 Checker；不得用测试 Fake 冒充生产适配。 |

Wave 0 准出：`D-RR-002`、`D-RR-003`、`D-RR-004`、`D-RR-005` 均有明确结论；每个后续 Task 有独立写入白名单、Red 和验证命令。

### Wave 1: 关闭公共资金与治理控制面

Wave 状态：`VERIFIED`。`W1-01`、`W1-02A` 与 `W1-03` 已完成 TDD/根因修复，并通过 Maker 与独立 Checker 门禁；检查点 B 已转入 `W2-01B` Owner Gate。

| Task ID | 依赖 / Owner | 写入范围 | 只读范围 | 验收场景与验证命令 | 禁止事项 / 停止条件 |
| --- | --- | --- | --- | --- | --- |
| `W1-01` 账本变更内收 | `W0-02` / Ledger Maker | `ledger/face` 的 `LedgerService`/`UpdateLedgerBalanceRequest`，`ledger/impl` 的 `LedgerServiceImpl`/`LedgerBalanceProjectionServiceImpl`；当前所有直接调用测试、一个公共契约测试和相关稳定设计 | transaction/wallet 调用边界和用户指南 | 先 Red 证明 face 不暴露直接余额/删除；正式 posting 仍产生平衡 plan、LedgerTransaction、LedgerEntry、余额桶和幂等证据。`just verify-slice LedgerServiceImplTests,LedgerBalanceProjectionServiceImplTests,FundsModuleDependencyBoundaryTests tests`、`just test-ledger`、`just test-boundary`、`just compile` | 不新增一行透传服务；不保留测试专用直改入口；Snapshot 迁移授权改变即停。 |
| `W1-02` 治理重放控制面 | `W0-04` / Governance Maker | 检查点 A 仅 `FundsProjectionReplayService`、`FundsProjectionReplayServiceTests`；检查点 B 才扩到经授权的 governance face/impl、专属 H2 schema/测试和系分/TDD | transaction/ledger/reconciliation 只读 face 与事实模型 | 检查点 A 先 Red 证明无控制证据时两种写模式在读取前失败关闭；检查点 B 再覆盖跨租户、缺 operator/reason、缺 approval/audit、越界范围、重复 task、限批续跑和三种模式写入边界。A 执行 `just test-one FundsProjectionReplayServiceTests tests`、`just test-governance`、`just test-boundary`、`just compile` | 检查点 A 未绿不得触达 Source/Writer；不反写交易、账本、余额、清结算或对账事实；检查点 B 的公共契约/DDL 需单独授权。 |

Wave 1 准出：Maker Gate 与 Checker Gate 均已满足。`SC-RR-002` 和治理契约部分 `SC-RR-004` 已实现，Boundary 顺序依赖已关闭；Owner 已允许进入 Wave 2，但公共契约与 DDL 仍保持单独授权。

### Wave 2: 投影恢复与宿主装配

Wave 状态：`VERIFIED`。`W2-01A` 安全根因、`W2-01B` 持久恢复控制面和 `W2-02` 真实 Spring/H2 宿主装配契约均已完成 Maker 验证并通过独立 Checker；不创建新运行模块。

| Task ID | 依赖 / Owner | 写入范围 | 只读范围 | 验收场景与验证命令 | 禁止事项 / 停止条件 |
| --- | --- | --- | --- | --- | --- |
| `W2-01` 持久投影恢复 | `OWNER_APPROVED_IMPLEMENTED_CHECKER_PASS` / Transaction + Governance Maker | A：冻结生命周期 saver、RouteSnapshot 持久化安全根边界与专属测试；B：transaction/governance face/impl、专属表/schema/Mapper/测试、系分/TDD | transaction/governance face、route/ledger 只读证据 | A 的安全与持久回读 `13/13` 且 Checker 通过；B 覆盖 tenant 有界扫描、重复请求、限批续跑、backlog、三种模式、差异和幂等只读投影，`just test-governance` 为 `24/24` | Checker P0/P1 为零；不得新增通用 outbox，不得回写资金事实；批次/冷数据与历史补偿不在本切片。 |
| `W2-02` 最小宿主装配契约 | `OWNER_APPROVED_IMPLEMENTED_CHECKER_PASS` / Integration Maker | `tests` 中真实 Spring 装配测试、`docs/用户接入指南/README.md` | 各模块 POM、现有 `AbstractFundsServiceTest`、Mapper 扫描、datasource/transaction 配置、44 表 DDL | 真实 H2 上下文证明必要 face/impl Bean、Mapper、事务和 44 表可用，`FundsHostCompositionContractTests` 为 `4/4`；接入指南列出宿主责任 | Checker P0/P1 为零；不新增 starter、示例应用、Controller 或假实现；Git 索引的前置 staged delete 仍待独立授权处理。 |

Wave 2 准出：`SC-RR-003`、`SC-RR-004`、`SC-RR-005` 完成；任何恢复路径都能由测试稳定复现而非人工解释。

### Wave 3: 数据库证据边界、全量门禁与独立复核

Wave 状态：`VERIFIED`。Owner 数据库证据边界、Maker 全量门禁与独立 Checker 均已完成，结论为 `W3_GATE_PASS`、P0/P1 为零。

| Task ID | 依赖 / Owner | 写入范围 | 只读范围 | 完成条件与验证命令 | 停止条件 / 交接要求 |
| --- | --- | --- | --- | --- | --- |
| `W3-01` 数据库证据边界收口 | Wave 2 / Product readiness + Architecture Owner | OpenSpec、权威设计/TDD/接入指南和数据库 README | H2/DDL/宿主职责、MySQL 专项资产 | Owner 确认公共能力库不持有独立目标数据库接入；全仓统一 H2/DDL 为仓库门禁、实际数据库为宿主责任 | 不删除 MySQL 专项资产，不用 H2 声称实际数据库行为已验证，不连接任何数据库。 |
| `W3-02` 仓库全量验证 | `QA_MAKER_PASS_2026-08-04` | 只回写本 OpenSpec 的验证摘要 | 全部源码、测试、构建配置和工作树 | Java 21 / Maven 3.6.3；DDL/H2/装配/投影聚焦 `31/31`、boundary `194/194`、compile/PMD 21 模块、`verify-cad` `1024/1024`、`git diff --check` 全部通过 | 唯一 skipped 为宿主可选的 `ReconciliationMysqlMigrationIntegrationTests`；保留当前用户 `pom.xml` 差异。 |
| `W3-03` 独立 Checker 与状态封版 | `W3_GATE_PASS_2026-08-04` / Independent Checker + Owners | 本 OpenSpec，必要时仅修改就绪口径文档 | Goal diff、全部证据、残余风险、Owner 决策 | Checker 已直接复核源码、文档与 XML：P0=0、P1=0；Goal 转 `VERIFIED`，最高成熟度为 `REPOSITORY_VERIFIED` | 宿主和生产证据缺失，不声明 `HOST_INTEGRATION_VERIFIED` 或 `PRODUCTION_VERIFIED`；Git 仍需另行授权。 |

## 8. Deferred Debt Register

以下结论保留，但不与当前 P0/P1 混做；每项在本 Goal `VERIFIED` 前必须有下一 Owner，不要求本 Goal 内实现：

| Debt ID | 内容 | 下一 Owner / 推荐处理 |
| --- | --- | --- |
| `DEBT-RR-001` | payout/settlement 关键用例日志覆盖不足。 | Reconciliation/Observability Owner：独立日志切片，按敏感信息红线补聚焦测试/Review。 |
| `DEBT-RR-002` | `core` 依赖 Swagger/wind-jackson，削弱核心语义纯度。 | Core Architecture Owner：先做依赖使用清单；只有能无行为漂移移除时再执行。 |
| `DEBT-RR-003` | 部分公共币种字段仍使用 `String`，未统一 `CurrencyIsoCode`。 | Public Contract Owner：独立公共契约迁移 Goal，必须有兼容窗口和调用方清单。 |
| `DEBT-RR-004` | 产品/系分校验器与当前模板或命名存在漂移。 | Docs Tooling Owner：先确认权威模板，再单独修校验器，不能为通过而改设计语义。 |
| `DEBT-RR-005` | 归档、Manifest、指标、水位等治理广域多数仍为 L1。 | Governance Product/Architecture Owner：保持在后续 Goal，不用本轮投影恢复切片扩成治理平台。 |
| `DEBT-RR-006` | 公共 scan cursor 尚未校验 `0 <= last <= upper`，也未绑定初始化范围。 | Transaction Owner：作为独立 P2 公共契约切片补范围校验和回归；当前内部 checkpoint、tenant Context 与 SQL 边界不受影响。 |
| `DEBT-RR-007` | 历史 `UNFREEZE` 可能缺本次 route，batch/cold data 尚未进入投影恢复事实源。 | Transaction/Governance Owner：仅在需要历史补偿或冷数据重建时立项，不伪造缺失事实。 |

## 9. Goal Ledger

### Wave 0 Checker Verdict

Wave 0 当时的独立 Checker 结论为 `CHECKER_PASS_FOR_WAVE_0_EVIDENCE`：四项证据与源码一致，未发现新增或漏报 P0/P1，也未发现错误准出；当时公共账本直改/删除、持久投影恢复和治理写模式控制面仍未实现。后续 Owner 已批准 Wave 1 与 Wave 2 推荐执行包，当前状态以 3.2、3.3 节和下方 Goal Ledger 为准；Wave 2 已通过 `W2-01A_SECURITY_CHECKER_PASS` 与最终 `W2_GATE_PASS`，P0/P1 为零。

| Field | Current Value |
| --- | --- |
| Goal status | `VERIFIED` |
| Latest completed action | Independent Checker 直接复核源码、文档和 XML 后给出 `W3_GATE_PASS`，P0=0、P1=0 |
| Current evidence | HEAD `869888340504f0156aebdb4948083478762f7cc0`；Java 21 / Maven 3.6.3；`ReconciliationMysqlDdlContractTests`、`FundsHostCompositionContractTests`、`FundsProjectionReplayServiceTests` 合计 `31/31`；boundary `194/194`；compile/PMD 各 21 模块成功；`verify-cad` 为 107 个测试集、1024 个测试、0 failure、0 error、1 skipped，唯一 skipped 为宿主可选的 `ReconciliationMysqlMigrationIntegrationTests`；`git diff --check` 通过；Checker 直接确认 13 个生产叶子/测试模块 PMD XML 均为 0 violation。 |
| Current worktree | `master` 比 `origin/master` ahead 5；保留用户已有 `M pom.xml`；用户指南工作树已按授权恢复更新，但 Git 索引仍保留前置 staged delete，本轮未执行 Git |
| Current risks | 历史 `UNFREEZE` 记录可能缺本次 route；当前扫描不支持 batch/cold data，公共 scan cursor 尚未校验 `0 <= last <= upper` 或绑定初始化范围；宿主 IAM、自动任务创建、调度、告警和差异人工复核仍由接入方负责；H2/DDL 不证明宿主实际数据库的锁、隔离、性能和迁移；用户指南 staged delete 待 Git 授权处理 |
| Next action | 当前 Goal 无必做项；Git 打包需独立授权，采用 MySQL 的宿主可按需执行兼容专项 |
| Next owners | Repository Maintainer（可选 Git 打包）；Host Database/Release Owner（真实接入时） |
| Next review point | 下一次公共契约、生产 DDL、宿主装配或投影恢复范围变更时重新开启门禁 |
| Recovery | 读取本文件并运行 `git status --short --branch`、`git rev-parse HEAD`；保持 `pom.xml` 和用户指南 Git 索引不变，未经授权不执行 MySQL 或 Git |

## 10. Verification And Closure Rules

1. 单任务通过不等于 Goal 通过；必须完成跨任务契约、DDL/H2、恢复、全量门禁和独立 Checker 汇合。
2. 证据等级必须显式：设计为 L1、源码/本地测试为 L2、宿主装配为接入契约证据、实际数据库专项和真实生产为接入方证据。
3. Goal 转 `VERIFIED` 的最高表述只能是 `REPOSITORY_VERIFIED`；没有真实宿主和生产证据时不得写 `HOST_INTEGRATION_VERIFIED` 或 `PRODUCTION_VERIFIED`。
4. Goal 转 `CLOSED` 前必须更新 Goal Ledger、Deferred Debt Owner、Checker 结论和交接入口；Git 提交仍使用独立用户授权。
