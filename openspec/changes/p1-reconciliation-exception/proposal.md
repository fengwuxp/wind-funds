# Change Proposal: P1 Reconciliation Exception

## Summary

为 P1 对账批次、对账差错、阻断规则和差错处理动作建立独立 OpenSpec change，明确 `ReconciliationBatch`、`ReconciliationException`、阻断作用域、释放条件、来源事实、测试矩阵和人工审批门禁。本变更只做设计和规格准备，不实现 Java 代码、不新增 DDL、不执行调账、不接真实外部文件。

## Why

清算批次和结算出款已经有独立准入入口。下一步必须把对账差错和阻断规则单独钉住，否则容易出现：

1. 对账差异只作为报表提示，无法阻断清算、结算或出款。
2. 差错处理直接修改历史 `LedgerEntry`、余额投影或交易事实。
3. 金额差、长短款、费用差异被普通余额调整吞掉，缺少责任方、凭证和审批。
4. 对账文件、外部回单和内部事实缺少标准化匹配维度，导致重复成功、错币种、状态差异不可解释。
5. 差错调账、挂账认领、核销等高风险动作在 CAD 自动推进中越过人工审批。

## What Changes

1. 新增 `p1-reconciliation-exception` OpenSpec 变更入口。
2. 为 `clearing-reconciliation` 增加对账批次、差错生成、阻断规则、释放条件和处理动作规格差异。
3. 为 `transaction-layer` 增加 `RECONCILIATION_EXCEPTION` 来源事实约束。
4. 固化 `ReconciliationMatchingServiceTests` 与 `ReconciliationExceptionAdjustmentTests` 的测试矩阵和编码前置任务。
5. 明确 DDL、阻断规则实现、差错调账、挂账认领、核销和外部文件接入必须进入 Harness manual approval。

## Scope

本变更包含：

1. `ReconciliationBatch`、`ReconciliationItem`、`ReconciliationException`、`BlockingRule` 的对象语义。
2. 对账匹配维度、差异类型、阻断级别、阻断作用域和释放条件。
3. 差错状态机、处理动作、审批和审计边界。
4. `RECONCILIATION_EXCEPTION` 作为未来差错处理入账来源事实的规格边界。
5. 编码前测试矩阵、审批材料和停止条件。

## Non Goals

1. 不实现对账批次、差错单、Mapper、Entity、Service、Converter 或外部文件解析。
2. 不新增或修改数据库 DDL、索引、迁移脚本或 H2 schema。
3. 不实现清算候选、清算批次、结算单、出款单、报表、FX 或归档。
4. 不执行补单、冲正、调账、挂账、认领、退回、核销或真实资金修复。
5. 不接真实通道文件、银行流水、回单接口、生产调度、凭据或生产数据。

## Impact

| 影响面 | 说明 |
| --- | --- |
| 产品设计 | 把对账差错、阻断规则和处理动作从 P1 readiness 总入口拆成可单独评审的工作包。 |
| 系分设计 | 对齐 `清结算与对账系分设计.md` 中 `4.8`、`4.9`、`4.10` 的对账批次、差错和阻断设计。 |
| 代码实现 | 当前无代码影响；后续实现必须先通过本 change 的任务清单和审批门禁。 |
| 测试验证 | 当前只固化测试矩阵；后续实现前先落 `ReconciliationMatchingServiceTests` 和 `ReconciliationExceptionAdjustmentTests`。 |
| 运维发布 | 不涉及发布；后续 DDL、外部文件接入、阻断规则和差错入账需独立审批。 |

## Acceptance

1. 对账批次导入、标准化、匹配和差错生成都有明确验收场景。
2. 对账差错能阻断清算、结算或出款，并能按释放条件解除。
3. 差错处理不得直接修改历史 `LedgerEntry`、余额投影、交易事实或出款事实。
4. 形成账务变化的处理动作必须使用独立来源事实 `RECONCILIATION_EXCEPTION`，并保存责任方、原因、凭证和审批引用。
5. DDL、阻断规则实现、差错调账、挂账认领、核销和外部文件接入仍停在人工审批门禁后，不能在 CAD 自动提交中直接进入实现。
