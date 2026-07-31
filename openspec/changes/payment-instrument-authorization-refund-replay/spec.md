# Payment Instrument Authorization Refund Replay

## Change Metadata

| Field | Value |
| --- | --- |
| Change ID | `payment-instrument-authorization-refund-replay` |
| Goal ID | `019fac6e-44e7-7bb2-b434-76125f3a3f3e` |
| Status | `VERIFIED_AWAITING_REVIEW` |
| Date | `2026-07-30` |
| Product owner | `PENDING` |
| Architecture owner | `PENDING` |
| Engineering evidence owner | `wind-funds` maintainers |

## Context

Payment-instrument authorization, completion, and reversal have a facade because they combine current admission or linked control facts. Principal refund is already an account-subject canonical action: it must identify the original authorization subject and replay the original `RouteSnapshot`. A second payment-instrument refund facade would duplicate this contract and risk selecting the current binding after a rebind.

## Scope

### In Scope

- Verify authorization, partial completion, payment-instrument rebind, and principal refund as one application-service scenario.
- Verify the refund posts only to the original authorization subject and updates the canonical `refundedAmount`.
- Verify a funds refund does not automatically write `REFUND_COMPENSATED` or restore the period control amount.
- Align product, system, TDD, and integration documents with the existing canonical boundary.

### Out of Scope

- New payment-instrument refund API, Controller/RPC, external host adapter, issuer event authority, product refund eligibility rules, atomic funds-and-control compensation orchestration, DDL, deployment, and Git operations.

## Normative Rules

1. Principal refund MUST use `FundsAuthorizationTransactionService#refund` with the original authorization account subject.
2. Refund MUST replay the original `RouteSnapshot` and MUST NOT resolve the current payment-instrument binding.
3. A funds refund MUST NOT automatically create `REFUND_COMPENSATED` or restore a period control amount.
4. Optional control compensation MUST remain a separate product-policy-authorized action with `reasonCode`, `operatorId`, and `auditReferenceSn`.
5. This evidence proves repository-level provider behavior only; it MUST NOT be represented as issuer/external-host adoption or deployed-runtime stability.

## Acceptance And Evidence

| ID | Acceptance | Evidence |
| --- | --- | --- |
| `AC-PI-REF-001` | After authorization and payment-instrument rebind, principal refund posts only to the original account subject. | `PaymentInstrumentTransactionAuthorizationTests#testAuthorizationRefundShouldUseHistoricalSubjectWithoutAutomaticControlCompensationAfterRebind` |
| `AC-PI-REF-002` | The original funds aggregate records `refundedAmount=30` and the refund ledger event is `AUTH_REFUND`. | Same test and canonical `FundsAuthorizationTransactionService#refund` path. |
| `AC-PI-REF-003` | The current rebound account remains untouched and no `REFUND_COMPENSATED` movement is created. | Same test, balance projection, and Spend Control movement assertions. |

## Repository Caller Inventory

The current repository has no caller of `FundsAuthorizationTransactionService#refund` outside the canonical implementation and tests. No external adapter is owned by this repository. Host adoption remains outside this Goal and does not block repository capability delivery.

## Review Gates And Residual Risk

| Gate | State | Stop Condition |
| --- | --- | --- |
| Product compensation policy | `PENDING_OWNER_REVIEW` | Do not restore control amounts until eligible refund types, reason-code vocabulary, and audit ownership are published. |
| Refund orchestration | `PENDING_OWNER_REVIEW` | Do not claim atomic funds-and-control closure until owners select local atomic composition or durable asynchronous recovery. |
| External host adoption | `OUTSIDE_REPOSITORY_GOAL` | Do not infer deployment from repository tests; host adoption is not a repository completion gate. |
| Provider recovery contract | `REPOSITORY_EVIDENCE_REQUIRED` | Repository tests must prove original-route replay, idempotency, failure rollback, and queryable funds/control facts; real runtime monitoring is an adopter responsibility. |
