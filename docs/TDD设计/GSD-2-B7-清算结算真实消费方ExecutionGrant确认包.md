# GSD-2 B7 清算结算真实消费方 Execution Grant 确认包

## 1. 文档定位

本文最初是 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 的 Execution Grant 确认包，用于承接已完成的 B7 对象级 Gate 基座，把对账差错阻断能力接入清算候选和结算单的真实准入消费方。本轮已按用户确认完成 Red / Green / Verify，本文继续作为确认、消费结果、Not Done 和交接证据。

本文不是完整清分、清算、结算、出款、追偿、补事实执行、运营审批、生产迁移或 Git 授权。本 Grant 已完成最小只读 consumer 服务切片，但不得继续沿用本 Grant 扩完整清结算生命周期、补事实、运营审批、生产迁移或报表能力。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` |
| 原子任务 | 新增清算 / 结算 gate 准入消费方服务，证明清算候选或结算单会真实消费对象级对账 gate。 |
| 所属阶段 | GSD-2 / B7 reconciliation gate consumer / consumed-green。 |
| 前置证据 | `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001 / scopeDecision=object-scope-schema-backed` 已完成本地 Green，差错事实已支持 `blockingObjectType / blockingObjectSn`，gate 查询已支持对象级精确命中和历史类型级保守命中。 |
| 当前状态 | `CONSUMED_GREEN_VERIFIED_SUMMARY_ONLY` |
| Owner | AI Native 负责 Loop、Goal 和停止条件；产品架构专家负责清算 / 结算业务语义、阻断对象和验收边界；资深架构师负责接口契约、TDD、实现、验证和代码 Review；用户确认单一 Grant。 |
| 写入范围 | `reconciliation-face` 契约、Request、DTO、`reconciliation-impl` 只读 consumer、目标测试和状态文档。 |
| 写入文件 | `reconciliation/reconciliation-face`、`reconciliation/reconciliation-impl`、`tests/src/test/java/com/wind/funds/reconciliation`、本文、LWT Goal、W5、README 和 OpenSpec tasks。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、`reconciliation-*`、`ledger-*`、`transaction-*`、`wallet-*`、tests、Justfile 和 AGENTS.md。 |
| 只读参考 | `ReconciliationGateApplicationService`、`PayoutOrderService`、`PayoutOrderServiceImpl`、`PayoutPreflightServiceTests`、B7 对象级 Gate 已消费确认包和当前 LWT Goal。 |
| Git 策略 | `summary_only`。本轮代码、测试和文档状态尚未提交；本文不授权 push、PR、merge、rebase、reset 或分支切换。 |

## 2. 产品裁决

产品裁决：清算候选生成和结算锁定前，必须有真实业务消费方调用对象级对账 gate，未闭环或重跑未对平差错应阻断后续资金释放流程。

业务语境：

1. 清算消费方服务的目标是判断某个清算候选或清算确认对象能否继续进入后续清算流程。
2. 结算消费方服务的目标是判断某个结算单或结算锁定对象能否继续进入后续结算、出款或释放可结算资金流程。
3. 本 Grant 只做准入判断和解释，不创建清算候选、清算批次、结算单、出款单或任何账务事实。
4. 真实资金变化仍必须由后续清分、清算、结算、出款或补事实专项 Grant 独立承接。

产品验收问题：

| 验收问题 | 必须回答 |
| --- | --- |
| 清算候选存在未闭环差错时能否阻断。 | 返回 `BLOCKED`，携带差错流水、阻断对象、证据引用和解释，不生成资金或账本事实。 |
| 结算单存在未闭环差错时能否阻断。 | 返回 `BLOCKED`，并说明是哪个结算对象被哪个差错阻断。 |
| 同类型不同对象是否会误阻断。 | 对象 A 的差错不得阻断对象 B；历史类型级差错除外，仍按保守阻断处理。 |
| 已处理且重跑对平后能否条件放行。 | 返回 `CONDITIONALLY_PASSED`，但不自动创建清算、结算或出款事实。 |
| 准入查询是否只读。 | 查询前后交易、route、ledger transaction、posting、LedgerEntry、余额投影、清算和结算事实数量不变。 |

### 2.1 规则矩阵

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验收口径 |
| --- | --- | --- | --- | --- | --- |
| 清算差错阻断 | 清算候选、清算批次或清算确认对象执行前置检查。 | 调用对象级 gate；命中未闭环或重跑未对平差错时返回 `BLOCKED`。 | P0 | B7-CONSUMER-MVP | 目标测试证明阻断且无交易、账本、余额、投影或清算事实副作用。 |
| 结算差错阻断 | 结算单、结算锁定或进入出款链路前置检查。 | 调用对象级 gate；命中未闭环差错时返回 `BLOCKED` 并携带阻断对象。 | P0 | B7-CONSUMER-MVP | 目标测试证明结算对象被精确阻断，且不生成结算或出款事实。 |
| 对象级不误阻断 | 同租户同类型存在多个清算或结算对象。 | 只阻断 `blockingObjectSn` 命中的对象；历史类型级差错按保守阻断。 | P0 | B7-CONSUMER-MVP | 对象 A 差错不得误阻断对象 B。 |
| 条件放行 | 差错已处理且重新对账对平。 | 返回 `CONDITIONALLY_PASSED`，并保留处理动作和重新对账证据。 | P1 | B7-CONSUMER-MVP | 只返回准入解释，不自动创建清算、结算或出款事实。 |
| 缺对象拒绝 | 请求缺 `gateObjectSn`、币种、金额或幂等键等关键准入字段。 | 拒绝请求或阻断，不降级为静默通过。 | P0 | B7-CONSUMER-MVP | 失败无资金、账本和投影副作用。 |

## 3. 能力地图和对象边界

| 能力域 | 本 Grant 目标 | 不做范围 |
| --- | --- | --- |
| 清算准入消费 | 为清算候选或清算确认对象提供只读 gate preflight，返回通过、阻断或条件放行。 | 不生成清算候选、清算批次或清算明细。 |
| 结算准入消费 | 为结算单或结算锁定对象提供只读 gate preflight，返回可解释准入结果。 | 不锁定结算单、不生成出款单、不触发代付。 |
| Gate 消费解释 | 复用 `ReconciliationGateApplicationService`，输出差错、证据、阻断对象和下一步原因。 | 不绕过 gate 直接读 Mapper，不写差错处理动作。 |
| 无资金副作用 | 证明准入消费方只读，不写交易、账本、余额或投影事实。 | 不做补事实、调账、冲正、核销、追偿。 |

业务对象：

- ClearingSettlementGateConsumer：清算 / 结算准入消费方服务，不是资金事实服务。
- ClearingPreflightResult：清算对象准入结果，不代表清算已发生。
- SettlementPreflightResult：结算对象准入结果，不代表结算已锁定或已出款。
- ReconciliationGateDecision：对账差错准入判断，由既有 gate 服务返回。
- Gate Object：清算候选流水、清算批次流水、结算单流水或等价业务对象流水。

## 4. 推荐服务边界

推荐新增一组 reconciliation face application/service 契约，而不是把清算和结算逻辑塞进现有 `PayoutOrderService`：

| 建议契约 | 建议落点 | 职责 |
| --- | --- | --- |
| `ClearingSettlementGateConsumerService` 或等价命名 | `reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/service` | 面向清算 / 结算编排、运营后台和自动任务，提供清算 / 结算准入检查。 |
| `CheckClearingSettlementGateRequest` 或拆分为清算、结算两个请求 | `reconciliation-face/model/request` | 承载 tenant、gateObjectType、gateObjectSn、业务金额、币种、幂等键和证据引用。 |
| `ClearingSettlementGateResultDTO` | `reconciliation-face/model/dto` | 返回 passed、decisionStatus、blockingReasons、evidenceRefs、checkedAt、checkedBy 和 display/operation status。 |
| `ClearingSettlementGateConsumerServiceImpl` | `reconciliation-impl/services/impl` | 复用 `ReconciliationGateApplicationService#checkGate`，映射准入结果和阻断原因。 |

命名取舍：

1. 若首个 Red 同时覆盖清算和结算，使用 `ClearingSettlementGateConsumerService`，用 `gateObjectType` 区分 `CLEARING / SETTLEMENT`。
2. 若实现发现清算和结算的前置证据、状态或返回语义明显分裂，则拆成 `ClearingGateConsumerService` 和 `SettlementGateConsumerService`，但本 Grant 首轮不建议先拆两个服务。
3. 不新增 `ClearingService`、`SettlementService` 或 `SettlementOrderService` 这类容易被误解为完整生命周期的名字。

接口契约口径：

1. 入参必须明确租户、消费对象类型、消费对象流水、币种、金额、请求流水和证据引用；最小 MVP 可先保留金额和币种为准入解释字段，不作为清算或结算事实。
2. 出参必须明确准入状态、阻断原因、差错流水、阻断对象、证据引用、检查时间和操作建议。
3. 错误码或断言语义优先复用现有资金域断言；本 Grant 不新增完整公共错误码体系。
4. 幂等以请求流水和 gate 消费对象组合保证查询可重放；服务本身只读，不落幂等事实。
5. 兼容策略沿用对象级 Gate 基座：历史类型级差错保守阻断，新对象级差错精确命中。

## 5. 三卡交接

### 5.1 Product Context Card

| 字段 | 内容 |
| --- | --- |
| 业务目标 | 在清算候选生成或结算锁定前阻断未闭环对账差错，防止错误金额进入资金释放、结算或出款链路。 |
| 目标用户 / 验收方 | 财务、运营、风控、清结算 owner、测试和研发。 |
| 核心对象 | 清算候选、结算单、ReconciliationDifference、ReconciliationGateDecision、Gate Object。 |
| 关键不变量 | 准入服务只读；未闭环差错阻断；对象级差错不误阻断其他对象；条件放行必须依赖处理动作和重新对账对平证据。 |
| 主流程 | 清算 / 结算消费方构造 gate request，调用 gate，映射准入结果，返回可解释阻断或通过。 |
| 异常路径 | 缺 gateObjectSn、缺币种/金额/幂等键、未闭环差错、重跑未对平、历史类型级差错命中时拒绝或阻断。 |
| 非目标 | 不创建清算、结算、出款、追偿、补事实、运营审批或生产迁移能力。 |

### 5.2 Engineering Handoff Card

| 字段 | 内容 |
| --- | --- |
| 推荐首个 Red | `B7-CLSSET-CONSUMER-RED-001`，清算对象命中对象级未闭环差错时准入阻断且无账务副作用。 |
| 源码样板 | `PayoutOrderService#checkPayoutPreflight` 和 `PayoutOrderServiceImpl` 已证明“消费 gate + 映射阻断原因 + 不写账务事实”的模式。 |
| 可复用契约 | `ReconciliationGateApplicationService`、`CheckReconciliationGateRequest`、`ReconciliationGateDecisionDTO`、`ReconciliationGateObjectType.CLEARING / SETTLEMENT`。 |
| 写入范围 | 只允许新增清算 / 结算准入消费方契约、Request、DTO、实现、目标测试和状态文档回写。 |
| 禁止范围 | 不改 B7 对象级 Gate 基座字段，不新增生产 DDL，不写完整清结算生命周期，不写补事实执行服务。 |
| 验证命令 | 目标测试、`just test-reconciliation`、`just compile`、`just pmd`、`git diff --check`。 |

### 5.3 Production Loop Card

| 字段 | 内容 |
| --- | --- |
| 生产可用锚点 | 清算 / 结算消费方能在释放资金流程前消费对账差错并返回可解释阻断。 |
| 安全边界 | 不返回外部账户原文，不写资金事实，不调用交易或账本写入。 |
| 可靠性边界 | gate check 幂等、只读、可重跑；缺关键对象或证据时不静默通过。 |
| 发布前门禁 | 目标测试、reconciliation 分组、compile、PMD、diff 检查通过；若后续接生产清算 / 结算表，需另行 DDL、迁移、灰度和回滚评审。 |
| 残余风险 | 完整清分分佣、结算锁定、出款执行、差异报告、补事实、运营审批、告警和 Runbook 仍为 Not Done。 |

## 6. Red 候选和验收矩阵

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `B7-CLSSET-CONSUMER-RED-001` | 清算对象命中对象级未闭环差错时是否阻断。 | 未闭环差错不得释放清算候选。 | 返回 `BLOCKED`、差错流水、阻断对象、证据引用和解释。 | 不生成清算候选、交易、route、posting、LedgerEntry、余额或投影副作用。 | 新清算 / 结算消费方测试。 | `just test-one ClearingSettlementGateConsumerServiceTests tests`。 | 需要完整清算对象生命周期时停止。 |
| `B7-CLSSET-CONSUMER-RED-002` | 结算对象命中对象级未闭环差错时是否阻断。 | 未闭环差错不得释放结算或出款链路。 | 返回 `BLOCKED`，阻断原因可解释到结算对象。 | 不生成结算单、出款单、交易、route、posting 或 LedgerEntry。 | 同上。 | 目标测试 + `just test-reconciliation`。 | 需要结算锁定或出款状态机时停止。 |
| `B7-CLSSET-CONSUMER-RED-003` | 同类型不同对象是否不误阻断。 | 对象级差错必须准确命中。 | 对象 A 阻断，对象 B 通过或仅受历史类型级差错影响。 | 不用类型级全阻断冒充对象级消费能力。 | 同上。 | 目标测试。 | 需要改 Mapper / schema 时停止，因为前置 Grant 已消费，除非另行授权。 |
| `B7-CLSSET-CONSUMER-RED-004` | 已处理且重跑对平后是否条件放行。 | 放行必须基于重新对账证据。 | 返回 `CONDITIONALLY_PASSED`，证据引用完整。 | 不创建后续清算 / 结算事实。 | 同上。 | 目标测试 + `test-reconciliation`。 | 需要改变差错状态机时停止。 |
| `B7-CLSSET-CONSUMER-RED-005` | 缺消费对象或关键准入字段时是否拒绝。 | 缺对象不得降级成全局放行。 | 抛出业务断言或返回阻断，且无副作用。 | 不静默通过，不写任何资金事实。 | 同上。 | 目标测试。 | 需要新增公共错误码体系时停止。 |

## 7. 写入范围和禁止事项

允许写入：

1. `reconciliation-face` 新增清算 / 结算 gate consumer 服务契约。
2. `reconciliation-face` 新增最小 Request / DTO / 枚举或复用现有 preflight 枚举。
3. `reconciliation-impl` 新增只读 consumer 实现，复用 `ReconciliationGateApplicationService`。
4. `tests` 新增目标服务流测试，使用真实 Spring Bean、H2 schema 和现有差错服务。
5. 文档、LWT Goal、W5、README 和 OpenSpec tasks 状态回写。

禁止写入：

1. 不修改交易、钱包、账本写入链路。
2. 不新增完整清分、清算批次、结算单、出款单、追偿对象或补事实命令。
3. 不新增生产 DDL、生产迁移、运行时配置或外部通道接入。
4. 不改 B7 对象级 Gate 字段语义，不把 `gateObjectSn` 和 `blockingObjectSn` 混成同一字段。
5. 不把准入通过解释为清算完成、结算锁定、出款成功或财务确认。

## 8. 可复制确认文本

```text
Execution Grant：GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001
确认新增清算 / 结算真实 gate 准入消费方服务，复用已完成的对象级 ReconciliationGate 基座，写入范围限 reconciliation-face 契约、reconciliation-impl 只读 consumer、目标服务流测试和状态文档回写；不授权完整清分、清算、结算、出款、补事实、运营审批、生产迁移或 Git 提交。
```

## 9. Grant 消费运行卡

| 阶段 | 动作 | 通过口径 | 停止条件 |
| --- | --- | --- | --- |
| Pick | 选择 `B7-CLSSET-CONSUMER-RED-001` 作为首个 Red。 | Red 只证明清算对象准入阻断和无账务副作用。 | 需要完整清算对象模型时停止。 |
| Red | 写目标服务流测试。 | 当前无 consumer 服务或不消费对象级 gate 时失败。 | Red 需要改 schema 或生产迁移时停止。 |
| Green | 新增最小 consumer 契约和实现。 | 复用 gate，返回可解释阻断，查询只读。 | 需要补事实、结算锁定或出款执行时停止。 |
| Review | 做问题优先 CR。 | 模块边界、公共契约、DTO 字段、断言和无资金副作用成立。 | 发现准入服务写资金事实时停止。 |
| Verify | 跑目标测试、分组、compile、pmd、diff。 | 命令通过，或环境问题被区分并复跑。 | 验证失败且无法在授权范围内修复时停止。 |
| Handoff | 回写文档和 OpenSpec tasks。 | Done / Not Done / 验证命令 / 下一 owner 清楚。 | 需要 Git、发布、生产或专业确认时停止。 |

## 10. 验证矩阵

本 Grant 的验证方案以目标服务流测试为主，辅以 reconciliation 分组回归、编译、PMD 静态检查和 `git diff --check`。不做压测；若后续进入生产清算 / 结算表或高并发批处理，再独立补容量、压测和生产索引评审。

| 验证层 | 命令或方式 | 完成条件 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md` | Task、Owner、范围、验证、TDD、Review、Execution Grant、人工确认和交接字段齐全。 |
| 产品结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md` | 业务目标、能力地图、对象、流程、规则、数据审计、风险和验收齐全。 |
| 架构结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md` | 背景目标、现状、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001|清算结算真实消费方|B7-CLSSET-CONSUMER-RED" docs openspec` | LWT Goal、W5、README 和 OpenSpec tasks 能追踪到本文。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |

已执行和收口建议验证命令：

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one ClearingSettlementGateConsumerServiceTests tests
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-reconciliation
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just compile
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just pmd
git diff --check
```

## 11. Grant 消费结果

本节记录本 Grant 的 Red / Green / Verify 结果。它只证明清算 / 结算只读 consumer 能真实消费对象级 gate，不证明完整清分、清算、结算、出款或补事实生产链路 Done。

| 阶段 | 结果 |
| --- | --- |
| Red | `ClearingSettlementGateConsumerServiceTests` 首次编译失败，缺少 `CheckClearingSettlementGateRequest`、`ClearingSettlementGateResultDTO`、`ClearingSettlementGateConsumerService` 和 `ClearingSettlementGateConsumerServiceImpl`，证明真实消费方服务入口缺失。 |
| Green | 新增 reconciliation-face 请求、结果 DTO 和服务契约，新增 reconciliation-impl 只读实现；实现只调用 `ReconciliationGateApplicationService#checkGate`，不直接访问 Mapper，不写交易、route、posting、LedgerEntry、余额投影、清算或结算事实。 |
| 覆盖场景 | 清算对象命中对象级未闭环差错阻断；结算对象命中对象级未闭环差错阻断；同类型不同对象不误阻断；差错已处理且重跑对平后条件放行；出款对象类型或空对象流水被拒绝。 |
| 资金红线 | 每个目标用例均使用 `assertLedgerFactsUnchanged` 证明 consumer 查询无账本事实、LedgerEntry 或余额投影副作用。 |
| 验证证据 | 沙箱内目标 Spring 测试因 embedded Redis 本地端口探测限制失败，已在非沙箱环境重跑同一命令通过：`just test-one ClearingSettlementGateConsumerServiceTests tests` 5 tests 通过；`just test-reconciliation` 26 tests 通过；`just compile` 通过；`just pmd` 通过。 |
| Git 状态 | 本 Grant 已消费但尚未提交，仍处于 `summary_only`。 |

### 11.1 已落地文件

| 类型 | 文件 | 作用 |
| --- | --- | --- |
| 请求模型 | `reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/model/request/CheckClearingSettlementGateRequest.java` | 承载租户、消费对象类型、消费对象流水、币种、金额和幂等键。 |
| 结果模型 | `reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/model/dto/ClearingSettlementGateResultDTO.java` | 返回准入状态、阻断差错、证据引用、解释、检查时间和检查人。 |
| 服务契约 | `reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/service/ClearingSettlementGateConsumerService.java` | 面向清算 / 结算编排、运营后台或自动任务的只读准入消费入口。 |
| 实现类 | `reconciliation/reconciliation-impl/src/main/java/com/wind/funds/reconciliation/services/impl/ClearingSettlementGateConsumerServiceImpl.java` | 复用 gate application，限制对象类型为 `CLEARING / SETTLEMENT`。 |
| 目标测试 | `tests/src/test/java/com/wind/funds/reconciliation/services/impl/ClearingSettlementGateConsumerServiceTests.java` | Spring 服务流测试，真实 Bean + H2 schema，覆盖阻断、放行、拒绝和无账务副作用。 |

## 12. Not Done 和残余风险

本确认包完成后仍不能声明以下能力 Done：

1. 完整清分、清算、结算、出款、追偿和商户/合作方分润生命周期。
2. B7 差异报告、补事实命令执行、运营审批、职责分离和权限模型。
3. 生产 DDL 迁移、回滚、灰度、SLO、告警和 Runbook。
4. 外部卡组织、银行、通道、ACH、SWIFT、FX、税务、会计、法务或合规最终确认。
5. wallet 全量生产 Done、VCC facade、全球账户业务能力和 Spend Rule 控制闭环。

## 13. 源码 Gap Audit 和 Red 落点复核

本节保留确认前的只读源码 Gap Audit，用于说明本轮为何从“继续改 gate 基座”收敛为“新增真实清算 / 结算 consumer”。该 Gap 已由第 11 节消费结果关闭；后续不得再把本节解释为新的编码授权。

### 13.1 当前源码事实

| 源码事实 | 证据 | 结论 |
| --- | --- | --- |
| 当时没有 `ClearingSettlementGateConsumerService` 或等价清算 / 结算 consumer。 | 消费前 `rg "ClearingSettlementGateConsumerService" reconciliation tests` 仅命中本文档。 | 已由本轮 Red / Green 关闭，现有 consumer 作为已消费证据保留。 |
| 出款已有真实 consumer 样板。 | `PayoutOrderServiceImpl#checkPayoutPreflight` 注入 `ReconciliationGateApplicationService`，构造 `CheckReconciliationGateRequest` 并设置 `ReconciliationGateObjectType.PAYOUT`。 | 清算 / 结算 consumer 应复用该模式：只读调用 gate、映射阻断原因、合并证据引用、不写账务事实。 |
| 目标测试已有无账务副作用断言样板。 | `PayoutPreflightServiceTests` 已覆盖缺准入证据阻断、证据齐备通过、PAYOUT 差错阻断、条件放行和 `assertLedgerFactsUnchanged`。 | 新测试应复用“准入只读 + ledger facts unchanged”的断言风格，避免只断言状态码。 |
| 对象级 gate 基座已经具备前置能力。 | 当前工作树中 `ReconciliationDifference`、`CreateReconciliationDifferenceRequest`、`ReconciliationDifferenceDTO`、`ReconciliationGateBlockingDifferenceDTO` 和 H2 schema 已补 `blockingObjectType / blockingObjectSn`；`ReconciliationGateApplicationServiceTests` 覆盖 CLEARING / SETTLEMENT 对象级命中。 | 下一轮不应再扩大 gate 基座字段、Mapper 或 schema，除非发现阻断对象语义缺陷并重新授权。 |
| gate 请求已有消费对象字段。 | `CheckReconciliationGateRequest.gateObjectType / gateObjectSn` 已表达准入消费对象。 | consumer Request 应直接映射到这两个字段，不重新发明 `blockingObjectSn` 入参。 |

### 13.2 推荐首个 Red 落点

| 项 | 建议 |
| --- | --- |
| 测试类 | `tests/src/test/java/com/wind/funds/reconciliation/services/impl/ClearingSettlementGateConsumerServiceTests.java`。 |
| 首个用例 | `testCheckClearingGateShouldBlockWhenObjectLevelDifferenceUnresolvedWithoutLedgerFactsMutation`。 |
| Red 构造 | 使用 `ReconciliationDifferenceApplicationService#createDifference` 创建 `blockingObjectType=CLEARING`、`blockingObjectSn=<clearingObjectSn>` 的未闭环差错，再调用清算 consumer。 |
| 期望结果 | 返回 `BLOCKED`、阻断对象、差错流水、证据引用和解释；查询前后 ledger transaction、posting、LedgerEntry、余额投影数量不变。 |
| 允许 Green | 新增最小 face 契约、Request、DTO 和 impl；impl 只调用 `ReconciliationGateApplicationService#checkGate`，不直接查 Mapper，不调用交易、账本、清算或结算写入服务。 |
| 第二批 Red | 结算对象阻断、同类型不同对象不误阻断、条件放行、缺对象或关键准入字段拒绝。 |

### 13.3 候选文件落点

| 类型 | 建议路径 | 口径 |
| --- | --- | --- |
| 服务契约 | `reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/service/ClearingSettlementGateConsumerService.java` | 用例级 application/service 入口，不是完整清算或结算生命周期服务。 |
| 请求模型 | `reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/model/request/CheckClearingSettlementGateRequest.java` | 承载 tenantId、gateObjectType、gateObjectSn、currency、amount、requestSn 或 idempotencyKey、evidenceRefs。 |
| 返回模型 | `reconciliation/reconciliation-face/src/main/java/com/wind/funds/reconciliation/model/dto/ClearingSettlementGateResultDTO.java` | 返回 passed、decisionStatus、blockingReasons、evidenceRefs、checkedAt、checkedBy 和 operationStatus。 |
| 实现类 | `reconciliation/reconciliation-impl/src/main/java/com/wind/funds/reconciliation/services/impl/ClearingSettlementGateConsumerServiceImpl.java` | 只读实现；复用 gate application；不绕过 application service 直接访问 Mapper。 |
| 目标测试 | `tests/src/test/java/com/wind/funds/reconciliation/services/impl/ClearingSettlementGateConsumerServiceTests.java` | Spring 服务流测试，真实 Bean + H2 schema；Mock 只用于外部不可控端口。 |

### 13.4 准出和停止条件补强

准出条件：

1. 首个 Red 能在没有 consumer 或未消费对象级 gate 时失败。
2. Green 后清算对象阻断返回可解释结果，且无交易、route、posting、LedgerEntry、余额投影或清算 / 结算事实副作用。
3. `CLEARING` 和 `SETTLEMENT` 只作为 gate 消费对象类型，不变成 ledger subject。
4. 复用对象级 Gate 基座的历史类型级保守阻断和对象级精确命中语义。

停止条件：

1. 需要创建完整清算候选、清算批次、结算单、出款单、追偿对象或补事实命令。
2. 需要新增生产 DDL、生产迁移、索引或运行时配置。
3. 需要改变 `blockingObjectType / blockingObjectSn` 的字段语义。
4. 需要把准入通过解释为清算完成、结算锁定、出款成功或财务确认。
