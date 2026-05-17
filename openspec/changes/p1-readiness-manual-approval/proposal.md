# Change Proposal: P1 Readiness and Manual Approval Gates

## Summary

为清结算与对账、完整外汇运营对象、归档投影与指标治理进入下版本建立独立 OpenSpec 入口和 Harness manual approval gate。该变更只固化准入条件、任务拆分、规格差异和审批材料，不实现 Java 代码、不新增 DDL、不接真实 Harness pipeline。

## Why

当前 P0 代码闭环已完成交易、钱包、账本和路由主链路。剩余工作涉及出款、对账差错、余额归档、数据重建、完整外汇运营对象和报表治理，具备更高资金、数据和生产风险。如果继续沿用 P0 自动提交节奏，容易出现：

1. 清结算、对账、FX 和归档对象在代码中隐式增长，缺少独立评审入口。
2. DDL、出款、调账、归档和重建行为绕过人工审批材料。
3. 报表、交易视图、余额投影和账本事实边界再次混淆。
4. 外汇和跨境能力被误解为交易层自动换汇或合规已确认能力。

## What Changes

1. 新增 P1 独立 OpenSpec change，承接后续清结算与对账、FX operations、归档治理的准入门禁。
2. 为 `clearing-reconciliation` 补 P1 readiness 规格差异，定义清算、结算、出款、对账差错和报表进入实现前必须满足的条件。
3. 为 `payment-ledger` 补归档、checkpoint、watermark、manifest、余额重建和指标水位的实现前置条件。
4. 为 `transaction-layer` 补 P1 来源事实、FX 决策事实、交易视图重放和 route replay 分界的实现前置条件。
5. 明确所有涉及 DDL、出款、差错调账、余额重建、手动归档、真实外部资金或完整 FX 运营对象的任务必须进入 Harness manual approval gate。

## Scope

本变更包含：

1. P1 readiness proposal、design、tasks。
2. OpenSpec spec delta。
3. 与现有 `docs/v5/系分设计` 的追溯关系。
4. 手动审批材料清单和分阶段准入规则。

## Non Goals

1. 不实现清算候选、清算批次、结算单、出款单、对账批次、差错单或报表代码。
2. 不实现 FX quote、FX execution、汇损益、监管报送或跨境数据对象。
3. 不实现 BalanceCheckpoint、BalanceProjectionWatermark、ArchiveManifest、TransactionViewReplayTask 或 MetricWatermark。
4. 不新增或修改数据库表结构、索引、迁移脚本。
5. 不创建真实 Harness pipeline、外部连接、凭据、调度任务或生产操作。

## Impact

| 影响面 | 说明 |
| --- | --- |
| 产品设计 | 不改变已确认 P0 口径，只把 P1 高风险能力拆成独立评审入口。 |
| 系分设计 | 后续 P1 系分需要按本 change 补齐对象、状态机、账务矩阵、审批和测试矩阵。 |
| 代码实现 | 无直接代码影响；后续代码任务必须先满足本 change 的准入条件。 |
| 测试验证 | 只新增任务和规格要求，不新增测试代码。 |
| 运维发布 | 不涉及发布；后续 DDL、归档、重建、出款和调账必须单独审批。 |

## Acceptance

1. P1 清结算、FX operations、归档治理都有独立准入条件和非目标。
2. Harness manual approval gate 明确触发条件、审批材料、验证证据和退出条件。
3. OpenSpec spec delta 能追溯到现有 `清结算与对账系分设计.md`、`归档投影与指标治理系分设计.md` 和 API 契约测试计划。
4. `v5-system-design-kickoff` 中剩余 P1 提醒可以指向本 change，而不是继续停留在泛化待办。
