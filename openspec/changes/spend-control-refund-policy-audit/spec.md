# Spend Control Refund Policy Audit

## Change Metadata

| Field | Value |
| --- | --- |
| Change ID | `spend-control-refund-policy-audit` |
| Goal ID | `019fac6e-44e7-7bb2-b434-76125f3a3f3e` |
| Status | `IMPLEMENTED_AWAITING_REVIEW` |
| Date | `2026-07-29` |
| Product owner | `PENDING` |
| Architecture owner | `PENDING` |
| Engineering evidence owner | `wind-funds` maintainers |
| External rule verification | `NOT_REQUESTED_NOT_PERFORMED` |

## Context

A successful funds refund is not sufficient evidence that a period Spend Rule counter may be restored. The linked refund path previously accepted the funds fact and wrote `REFUND_COMPENSATED` without a product-policy reason, operator, or audit reference. That contradicted the existing product rule and left the control change unauditable.

This change uses the existing request, movement service, audit columns, and idempotency comparison. It does not add a refund facade, table, dependency, or external-provider compatibility claim.

## Scope

### In Scope

- Add conditional `reasonCode`, `operatorId`, and `auditReferenceSn` fields to `SpendControlTransactionConsumptionRequest`.
- Propagate those fields to the existing `RecordSpendControlMovementRequest`.
- Require audit evidence for every `REFUND_COMPENSATED` write at the shared movement boundary.
- Reject a linked refund compensation with missing evidence before any control movement is inserted.

### Out of Scope

- Funds-refund creation, payment-instrument refund facade, atomic funds-and-control refund orchestration, Controller/RPC, event consumers, MQ, Outbox, Saga, DDL, deployment, production migration, Git operations, and external rule verification.

## Normative Rules

1. A funds refund MUST NOT automatically restore a period control amount.
2. Every `REFUND_COMPENSATED` request MUST contain non-blank `reasonCode`, `operatorId`, and `auditReferenceSn`.
3. The guard MUST be enforced by `SpendControlMovementService`, including linked and business-confirmed compensation paths.
4. Missing policy audit evidence MUST fail closed and MUST NOT insert a control movement or mutate funds and ledger facts.
5. Idempotent replay MUST preserve the existing audit values; changed audit values for an existing movement MUST be rejected by the existing exact-match check.

## Acceptance And Evidence

| ID | Acceptance | Evidence |
| --- | --- | --- |
| `AC-SC-REF-001` | Linked refund compensation without policy audit evidence is rejected with no funds, ledger, or control side effect. | `SpendControlTransactionConsumptionApplicationServiceTests#testRefundWithoutPolicyAuditEvidenceShouldFailWithoutSideEffect` |
| `AC-SC-REF-002` | Authorized linked refund persists all three audit fields and updates only the control projection. | `SpendControlTransactionConsumptionApplicationServiceTests#testRefundConsumedControlActivityShouldRecordCompensationWithoutFundsSideEffect` |
| `AC-SC-REF-003` | Direct movement, budget projection, and wallet acceptance paths remain valid under the shared guard. | `SpendControlMovementServiceFlowTests`, `BudgetControlLimitAdjustmentApplicationServiceTests`, `WalletSpendControlsAcceptanceFlowTests` |

## Repository Caller Inventory

The current checkout has no non-test caller of `SpendControlTransactionConsumptionApplicationService#refund` and no non-test construction of `REFUND_COMPENSATED`. There is therefore no in-repository caller to migrate. The provider contract requires all adopters to supply the three conditional audit fields; actual host adoption is outside this Goal.

## Review Gates And Residual Risk

| Gate | State | Stop Condition |
| --- | --- | --- |
| Product policy vocabulary | `PENDING_OWNER_REVIEW` | Do not standardize example reason codes as production policy until the product owner publishes the allowed vocabulary and refund eligibility rules. |
| Caller migration | `REPO_INVENTORY_COMPLETE` | No non-test caller exists in this repository. External adapters are outside the Goal and do not block repository delivery. |
| Refund orchestration | `OUT_OF_SCOPE_RISK` | Do not claim atomic refund closure until the product and architecture owners decide whether funds refund and optional control compensation share a local transaction or use durable asynchronous recovery. |
| External practice verification | `PENDING_AUTHORIZATION` | No current provider, scheme, legal, compliance, or accounting rule is asserted by this change. |
