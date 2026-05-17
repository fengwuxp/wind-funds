# Change Proposal: P1 Settlement and Payout

## Summary

为 P1 结算单与出款单进入实现建立独立 OpenSpec change，明确 `SettlementOrder`、`SettlementLine`、`PayoutOrder`、出款回单、失败回退、来源事实、测试矩阵和人工审批门禁。本变更只做设计和规格准备，不实现 Java 代码、不新增 DDL、不接真实 Harness pipeline、不提交外部出款。

## Why

`p1-clearing-batch` 已把商户资金从 `CLEARING` 进入 `AVAILABLE` 的清算批次边界钉住。下一步必须单独定义结算锁定与出款结果，否则容易出现：

1. 外部出款直接消耗 `AVAILABLE`，绕过 `SETTLEMENT` 排他锁定。
2. 外部受理被误认为出款成功，导致余额提前扣减。
3. 出款失败回退重复执行，造成商户可用余额被重复释放。
4. 回单金额、币种或账户不一致时被自动成功或自动回退，而不是进入差错和人工复核。
5. 出款、回单、外部账户和真实资金动作在 CAD 自动推进中越过人工审批。

## What Changes

1. 新增 `p1-settlement-payout` OpenSpec 变更入口。
2. 为 `clearing-reconciliation` 增加结算锁定、出款成功、失败回退和回单核验规格差异。
3. 为 `transaction-layer` 增加 `SETTLEMENT_ORDER`、`PAYOUT_ORDER` 来源事实约束。
4. 固化 `SettlementOrderServiceTests` 与 `PayoutResultServiceTests` 的测试矩阵和编码前置任务。
5. 明确 DDL、出款提交、出款成功入账、失败回退和外部回单接入必须进入 Harness manual approval。

## Scope

本变更包含：

1. `SettlementOrder`、`SettlementLine`、`PayoutOrder` 的对象语义。
2. 结算净额公式、扣减项、审批、锁定和可撤销边界。
3. 出款状态机、外部受理和成功回单的区分。
4. `MERCHANT_SETTLEMENT_LOCK`、`MERCHANT_PAYOUT_SUCCESS`、`MERCHANT_PAYOUT_FAIL_RESTORE` 三类入账动作的规格边界。
5. 编码前测试矩阵、审批材料和停止条件。

## Non Goals

1. 不实现结算单、出款单、Mapper、Entity、Service、Converter 或外部适配器。
2. 不新增或修改数据库 DDL、索引、迁移脚本或 H2 schema。
3. 不实现清算候选、清算批次、对账差错、报表、FX、跨境或归档。
4. 不实现银行、通道、卡组织、清算机构、代付接口、回单下载或凭据配置。
5. 不对真实资金、外部账户或生产数据发起任何操作。

## Impact

| 影响面 | 说明 |
| --- | --- |
| 产品设计 | 把结算锁定、出款成功和失败回退从 P1 readiness 总入口拆成可单独评审的工作包。 |
| 系分设计 | 对齐 `清结算与对账系分设计.md` 中 `4.5` 和 `4.6` 的结算单与出款单设计。 |
| 代码实现 | 当前无代码影响；后续实现必须先通过本 change 的任务清单和审批门禁。 |
| 测试验证 | 当前只固化测试矩阵；后续实现前先落 `SettlementOrderServiceTests` 和 `PayoutResultServiceTests`。 |
| 运维发布 | 不涉及发布；后续 DDL、出款提交、回单接入和入账动作需独立审批。 |

## Acceptance

1. 结算锁定、出款成功、失败回退和回单不匹配都有明确验收场景。
2. `SETTLEMENT` 作为出款中排他余额桶，不被展示状态或直接出款动作替代。
3. 外部受理不等于成功；成功必须有可信回单、金额、币种和账户核验。
4. 失败回退只能回退原锁定金额，且同一出款单只能成功回退一次。
5. 未来资金指令必须使用 `SETTLEMENT_ORDER` 或 `PAYOUT_ORDER` 作为来源事实，不以 `businessSn` 替代来源事实。
6. DDL、出款提交、出款成功入账、失败回退仍停在人工审批门禁后，不能在 CAD 自动提交中直接进入实现。
