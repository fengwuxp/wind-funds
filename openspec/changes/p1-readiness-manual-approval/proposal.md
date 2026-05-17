# Change Proposal: P1 Readiness Recalibration

## Summary

本 change 重新校准 P1 readiness：清算、清结算、结算出款、对账差错和报表相关任务已从当前有效 CAD 队列作废或移除；余额投影和交易投影只保留设计、流程、红线和测试矩阵再查验入口。该变更只更新 OpenSpec、系分追溯和审批边界，不实现 Java 代码、不新增 DDL、不接真实 Harness pipeline。

## Why

上一轮 P1 readiness 把清结算、对账、报表、归档、重建、交易视图重放和指标治理放在同一准入包里。随着当前版本目标收敛到交易、钱包、ledger、route 和投影边界，这种混合队列会带来三个问题：

1. 已作废的清结算、对账和报表任务可能从 readiness 文档中被重新当作有效 backlog。
2. 余额投影会被归档移动、指标治理或报表口径牵连，模糊 `LedgerEntry -> BalanceProjection` 的事实链。
3. 交易投影会被误解为路由回放或账务重建能力，进而污染 route、ledger transaction 或历史资金事实。

## What Changes

1. 将清算、清结算、结算出款、对账差错和报表相关 readiness 明确降级为历史草稿，不作为当前实现、测试、DDL 或外部集成依据。
2. 将余额投影再查验聚焦为：事实源、投影流程、业务余额变更观察口子、禁止视图/报表反推余额、checkpoint/watermark/rebuild 的重新设计门槛。
3. 将交易投影再查验聚焦为：只读读模型、来源事实、视图域、重放边界、与 route replay 的分界、禁止生成 route/entry/funds transaction。
4. 保留 FX operations 为后续独立能力提醒，但不把 FX、报表、指标治理混入当前投影再查验队列。
5. 明确任何 projection DDL、formal rebuild、formal replay、archive movement 或 report/metric governance 都必须新建 OpenSpec change 并重新获取用户确认。

## Scope

本变更包含：

1. `p1-readiness-manual-approval` 的 proposal、design、tasks 和 spec delta 再校准。
2. 与 `docs/v5/系分设计/API 契约测试与编码实施计划.md` 的投影再查验结论对齐。
3. 当前代码和测试证据的设计级引用：余额投影实现、余额变更事件测试、交易投影边界测试。
4. 后续进入实现前的 OpenSpec / Superpowers / Harness 门禁。

## Non Goals

1. 不实现清算候选、清算批次、结算单、出款单、对账批次、差错单或报表代码。
2. 不实现 BalanceCheckpoint、BalanceProjectionWatermark、ArchiveManifest、BalanceRebuildTask、TransactionViewReplayTask 或 MetricWatermark。
3. 不新增或修改数据库表结构、索引、迁移脚本。
4. 不新增交易投影生产写模型、正式重放任务、报表投影或指标治理任务。
5. 不创建真实 Harness pipeline、外部连接、凭据、调度任务或生产操作。

## Impact

| 影响面 | 说明 |
| --- | --- |
| 产品设计 | 不改变 P0 交易、钱包、账本和路由口径；清结算/对账/报表重新设计前不作为有效产品任务。 |
| 系分设计 | 余额投影和交易投影进入单独再查验；归档移动、指标治理和报表边界不随投影自动推进。 |
| 代码实现 | 无直接代码影响；当前只确认已有余额投影主链路和交易投影边界测试。 |
| 测试验证 | 不新增测试代码；记录下一轮投影测试矩阵入口。 |
| 运维发布 | 不涉及发布；任何 DDL、重建、正式重放、归档或报表重算必须单独审批。 |

## Acceptance

1. 已作废的清结算、结算出款、对账和报表任务不会在本 change 中作为有效 implementation backlog 出现。
2. 余额投影事实源明确为 ledger facts，不依赖交易视图、账单、报表或业务余额日志反推。
3. 交易投影明确为只读读模型，正式重放不得创建 route、posting、ledger entry、funds transaction 或 frozen order。
4. checkpoint/watermark/rebuild/replay/metric/reporting 如后续恢复，必须有新的 OpenSpec change、测试矩阵和用户确认的 Execution Grant。
