# Payment Instrument Authorization Control Release

## Change Metadata

| Field | Value |
| --- | --- |
| Change ID | `payment-instrument-authorization-control-release` |
| Goal ID | `019fac6e-44e7-7bb2-b434-76125f3a3f3e` |
| Status | `IMPLEMENTED_AWAITING_REVIEW` |
| Date | `2026-07-29` |
| Product owner | `PENDING` |
| Architecture owner | `PENDING` |
| Engineering evidence owner | `wind-funds` maintainers |
| External rule verification | `NOT_REQUESTED_NOT_PERFORMED` |

## Context

A VCC or card authorization can reserve both ledger authorization funds and a Spend Rule control amount. A trusted partial reversal must release both facts. The previous callable sequence exposed two independent application calls: funds reversal first, control release second. A process failure between them could leave funds released while the budget remained occupied.

The applicable industry pattern is to reference the original authorization, replay its historical route and instrument binding, use a stable reversal idempotency key, and commit coupled local facts atomically when they share one process, data source, and transaction manager. This change does not claim compatibility with any current external provider rule.

## Scope

### In Scope

- Add `PaymentInstrumentTransactionApplicationService#reverseAuthorizationByInstrument` as the use-case boundary for trusted payment-instrument reversals.
- Derive the original ledger subject from the authorization `RouteSnapshot`; never route by the current instrument binding.
- Read `controlReservationSn` from the original authorization context.
- Commit canonical funds reversal and linked `RELEASED` movement in one local transaction.
- Derive a stable 64-character control movement key from tenant, original authorization, reversal scene, and reversal business number.
- Preserve the account-subject canonical `FundsAuthorizationTransactionService#reversal` contract.

### Out of Scope

- Controller, HTTP, RPC, issuer webhook, event consumer, scheduler, MQ, Outbox, Saga, deployment, DDL, production migration, secrets, Git operations, and external rule verification.
- Automatic timeout or expiration release.
- Completion/consumption and refund/compensation atomic orchestration.
- Removal of the low-level account-subject reversal API or generic control movement service.

## Normative Rules

1. A payment-instrument reversal MUST reference an existing `PAY` authorization owned by the current tenant.
2. Amount MUST be positive and currency MUST equal the original authorization currency.
3. The original authorization `RouteSnapshot` MUST contain a payment instrument binding snapshot with a ledger-postable subject.
4. Reversal MUST use the historical subject and original route; current binding state MUST NOT affect replay.
5. When the original authorization contains `controlReservationSn`, funds reversal and control release MUST join one local transaction.
6. A control release MUST reference the original `RESERVED` movement and MUST NOT exceed canonical cumulative `reversedAmount`.
7. The same reversal business identity MUST produce the same control movement key and digest; a conflicting digest MUST fail closed.
8. Any control release failure MUST roll back funds transaction amounts, route/posting facts, ledger transaction, ledger entries, and balance changes created by that reversal attempt.
9. Timeout, expiration, or local inference MUST NOT call this use case as a trusted reversal.

## Public Contract

`ReverseAuthorizationByPaymentInstrumentRequest` contains:

| Field | Meaning |
| --- | --- |
| `tenantId` | Current tenant boundary. |
| `authorizationTransactionSn` | Original authorization aggregate reference. |
| `amount` / `currency` | Trusted reversal amount in the original currency. |
| `businessScene` / `businessSn` | Stable identity of this reversal fact and idempotency source. |
| `reversalTime` | Optional trusted event time. |
| `description` | Non-authoritative description, excluded from control identity. |

The caller does not supply a ledger account, payment instrument binding, control reservation ID, rule version, scope, or period. Those values are historical facts recovered by the application service.

## Consistency And Recovery

The implemented guarantee is local ACID only: `transaction-impl` calls the canonical reversal service and wallet control release service through local Spring beans using the same data source and transaction manager. A failure before commit leaves neither new funds reversal facts nor a new control release.

If these writes move to different databases or remote services, this guarantee is invalid. That future change requires a separate OpenSpec selecting Outbox plus idempotent consumer, Saga/compensation, or an explicit reconciliation workflow with durable intent and an unknown-state runbook.

## Acceptance And Evidence

| ID | Acceptance | Evidence |
| --- | --- | --- |
| `AC-PI-REV-001` | Trusted partial reversal releases funds and linked control reservation; original aggregate remains `OPEN` when an amount remains. | `PaymentInstrumentTransactionAuthorizationTests#testTrustedAuthorizationReversalShouldReleaseLinkedControlReservation` |
| `AC-PI-REV-002` | Release never exceeds canonical cumulative trusted reversal amount. | Same test plus `SpendControlTransactionConsumptionApplicationServiceTests`. |
| `AC-PI-REV-003` | A conflicting control movement digest rolls back the entire funds reversal attempt. | `PaymentInstrumentTransactionAuthorizationTests#testReverseAuthorizationByInstrumentShouldRollbackFundsReversalWhenControlReleaseConflicts` |
| `AC-PI-REV-004` | Historical route and binding select the ledger subject; current binding is not consulted. | `PaymentInstrumentTransactionAuthorizationTests#testAuthorizationCompletionAndReversalShouldUseHistoricalSubjectAfterInstrumentRebind` |
| `AC-PI-REV-005` | Timeout, expiration, and local inference do not create `RELEASED`. | Existing authorization/TDD red-line coverage; no runtime timeout entry is added. |

## Execution Grant

The user request authorizes this SDLC slice inside the recovered `wind-funds` checkout, including the wallet-face request/interface, transaction-impl application orchestration, focused tests, and authoritative docs/OpenSpec updates. It does not authorize network access, Git mutation, DDL execution, deployment, production data, controller/RPC integration, or irreversible operations.

## Repository Integration Inventory

The current checkout contains the facade contract, its local implementation, and test callers only. No issuer, card, webhook, scheduler, or event adapter is owned by this repository, so there is no in-repository caller to migrate. External host adoption is a deployment responsibility outside this Goal; it does not block repository capability delivery and must not be inferred from repository test evidence.

## Review Gates And Residual Risk

| Gate | State | Stop Condition |
| --- | --- | --- |
| Product semantics | `PENDING_OWNER_REVIEW` | Do not expose timeout/expiry as trusted reversal; product owner must confirm upstream event authority. |
| Supported transaction topology | `REPOSITORY_PROVEN` | Spring integration tests must prove funds and control writes share one local transaction and roll back together. A remote or multi-database deployment is a different, unsupported topology requiring a new change. |
| Upstream adoption | `OUTSIDE_REPOSITORY_GOAL` | Repository evidence proves the provider contract only; it neither requires nor claims that an external issuer/card host has deployed it. |
| Completion atomicity | `IMPLEMENTED_AWAITING_REVIEW` | Follow-up is recorded in `payment-instrument-authorization-control-consumption`; do not mark the slice Checker-verified before Owner review and shared-transaction tests pass. |
| Internal bypass | `KNOWN_RISK` | Low-level reversal and generic `RELEASED` recording remain available for account workflows; architecture tests or access-boundary enforcement are a follow-up if misuse appears. |
| External practice verification | `PENDING_AUTHORIZATION` | No current provider, scheme, legal, compliance, or accounting rule is asserted by this change. |
