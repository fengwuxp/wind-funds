# Change Proposal: P1 Clearing Batch

## Summary

为 P1 清算批次进入实现建立独立 OpenSpec change，明确 `ClearingCandidate`、`ClearingBatch`、`ClearingItem` 的业务对象、状态机、幂等、来源事实、测试矩阵和人工审批门禁。本变更仍为设计和规格准备，不实现 Java 代码、不新增 DDL、不接真实 Harness pipeline。

## Why

`p1-readiness-manual-approval` 已把清结算、FX、归档和报表拆成后续 change。清算批次是清结算链路的第一段，如果不先单独钉住候选生成、批次确认和重复清算红线，后续结算单、出款单和对账阻断都会缺少可信来源。

本 change 要先解决：

1. 清算候选只读生成，不产生资金入账。
2. 清算批次确认才触发商户 `CLEARING -> AVAILABLE`。
3. 同一候选明细不能被重复清算。
4. 清算批次必须保存策略版本、候选版本、数据源版本、审批引用和账本交易引用。
5. 当前语境是平台内部清分/内部清算，不表达持牌清算机构业务，也不替代合规、财务或合同确认。

## What Changes

1. 新增 `p1-clearing-batch` OpenSpec 变更入口。
2. 为 `clearing-reconciliation` 增加清算候选和清算批次的规格差异。
3. 为 `transaction-layer` 增加未来清算确认资金指令的来源事实约束。
4. 固化 `MerchantClearingBatchServiceTests` 的测试矩阵和编码前置任务。
5. 明确 DDL、确认入账、重跑补差必须进入 Harness manual approval。

## Scope

本变更包含：

1. `ClearingCandidate`、`ClearingBatch`、`ClearingItem` 的对象语义。
2. 候选幂等键、候选版本、排除原因和阻断原因。
3. 清算批次状态机和确认事务边界。
4. 清算确认来源事实 `CLEARING_BATCH` 与 route event `MERCHANT_CLEARING_COMPLETE`。
5. 编码前测试矩阵、审批材料和停止条件。

## Non Goals

1. 不实现清算候选、清算批次、Mapper、Entity、Service 或 Converter。
2. 不新增或修改数据库 DDL、索引、迁移脚本或 H2 schema。
3. 不实现 `SettlementOrder`、`PayoutOrder`、`ReconciliationBatch`、`ReconciliationException` 或报表对象。
4. 不实现外部银行、卡组织、清算机构、通道文件或资金到账对账。
5. 不接真实 Harness pipeline、外部账号、凭据、调度任务或生产数据。

## Impact

| 影响面 | 说明 |
| --- | --- |
| 产品设计 | 把清算候选、批次和确认从 P1 readiness 总入口拆成可单独评审的工作包。 |
| 系分设计 | 对齐 `清结算与对账系分设计.md` 中 `4.3` 和 `4.4` 的清算候选与批次确认设计。 |
| 代码实现 | 当前无代码影响；后续实现必须先通过本 change 的任务清单和审批门禁。 |
| 测试验证 | 当前只固化测试矩阵；后续实现前先落 `MerchantClearingBatchServiceTests`。 |
| 运维发布 | 不涉及发布；后续 DDL、确认入账和重跑补差需独立审批。 |

## Acceptance

1. 清算候选生成、清算批次确认和重复清算拒绝都有明确验收场景。
2. 清算批次状态机、幂等键、审计字段、策略快照和账本引用明确。
3. 未来清算确认资金指令必须以 `CLEARING_BATCH` 作为来源事实，不以 `businessSn` 替代来源事实。
4. `MerchantClearingBatchServiceTests` 覆盖候选幂等、排除原因、阻断差错、确认入账、重复确认和重跑版本。
5. DDL、确认入账、重跑补差仍停在人工审批门禁后，不能在 CAD 自动提交中直接进入实现。
