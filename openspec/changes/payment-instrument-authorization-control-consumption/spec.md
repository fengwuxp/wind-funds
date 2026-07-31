# Payment Instrument Authorization Control Consumption

## Change Metadata

| Field | Value |
| --- | --- |
| Change ID | `payment-instrument-authorization-control-consumption` |
| Goal ID | `019fac6e-44e7-7bb2-b434-76125f3a3f3e` |
| Status | `IMPLEMENTED_AWAITING_REVIEW` |
| Date | `2026-07-29` |
| Product owner | `PENDING` |
| Architecture owner | `PENDING` |
| Engineering evidence owner | `wind-funds` maintainers |
| External rule verification | `NOT_REQUESTED_NOT_PERFORMED` |

## Context

A VCC authorization can reserve both ledger authorization funds and a Spend Rule control amount. A trusted Clearing or Presentment result may complete only part of that authorization. Calling funds completion and control consumption separately exposed a crash window in which `completedAmount` and ledger facts were committed while the budget reservation remained unconsumed.

This change treats the original authorization as the authority for the ledger subject, route, instrument binding, and optional `controlReservationSn`. It does not assert compatibility with a current issuer, card-network, legal, compliance, or accounting rule.

## Scope

### In Scope

- Add `PaymentInstrumentTransactionApplicationService#completeAuthorizationByInstrument` for trusted payment-instrument authorization completion.
- Derive the ledger subject from the original authorization `RouteSnapshot`; never route by the current instrument binding.
- Commit canonical funds completion and linked `CONSUMED` movement in one local transaction.
- Support partial completion while the authorization aggregate remains `OPEN`.
- Limit cumulative control consumption to canonical cumulative `completedAmount`.
- Preserve `FundsAuthorizationTransactionService#complete` for account-subject workflows.

### Out of Scope

- Controller, HTTP, RPC, issuer adapter, event consumer, MQ, Outbox, Saga, deployment, DDL, production migration, secrets, Git operations, and external rule verification.
- Force completion without an original authorization, refund control compensation, timeout handling, and automatic expiration.
- Removal of the low-level account-subject completion API or generic control movement service.

## Normative Rules

1. A payment-instrument completion MUST reference an existing `PAY` authorization owned by the current tenant.
2. Amount MUST be positive and currency MUST equal the original authorization currency.
3. The original authorization `RouteSnapshot` MUST contain a payment instrument binding snapshot with a ledger-postable subject.
4. Completion MUST use the historical subject and original route; current binding state MUST NOT affect replay.
5. When the original authorization contains `controlReservationSn`, funds completion and control consumption MUST join one local transaction.
6. A control consumption MUST reference the original `RESERVED` movement and cumulative `CONSUMED` MUST NOT exceed canonical cumulative `completedAmount`.
7. The same completion business identity MUST produce the same control movement key and digest; a conflicting digest MUST fail closed.
8. Any control consumption failure MUST roll back funds transaction amounts, route/posting facts, ledger transaction, ledger entries, and balance changes created by that completion attempt.
9. Force completion, timeout, expiration, and local inference MUST NOT use this trusted completion use case.

## Public Contract

`CompleteAuthorizationByPaymentInstrumentRequest` contains:

| Field | Meaning |
| --- | --- |
| `tenantId` | Current tenant boundary. |
| `authorizationTransactionSn` | Original authorization aggregate reference. |
| `amount` / `currency` | Trusted completion amount in the original currency. |
| `businessScene` / `businessSn` | Stable identity and idempotency source for this completion fact. |
| `completedTime` | Optional trusted event time. |
| `description` | Non-authoritative description, excluded from control identity. |

The caller does not supply a ledger account, current binding, control reservation, rule version, scope, or period. The application service recovers those historical facts from the original authorization.

## Consistency And Recovery

The implemented guarantee is local ACID only: `transaction-impl` calls the canonical completion service and wallet control consumption service through local Spring beans using the same data source and transaction manager. A failure before commit leaves neither new funds completion facts nor a new control consumption.

If these writes move to different databases or remote services, this guarantee is invalid. That topology requires a separate design for durable intent, idempotent delivery, reconciliation, and unknown-state recovery.

## Acceptance And Evidence

| ID | Acceptance | Evidence |
| --- | --- | --- |
| `AC-PI-COMP-001` | Authorization 60 and trusted completion 40 keep the aggregate `OPEN`; funds `completedAmount` and control `CONSUMED` both equal 40. | `PaymentInstrumentTransactionAuthorizationTests#testPartialCompletionByInstrumentShouldConsumeLinkedControlReservationOnce` |
| `AC-PI-COMP-002` | Replaying the same completion business identity does not duplicate funds or control facts. | Same test. |
| `AC-PI-COMP-003` | A conflicting control movement digest rolls back the entire funds completion attempt. | `PaymentInstrumentTransactionAuthorizationTests#testCompleteAuthorizationByInstrumentShouldRollbackFundsCompletionWhenControlConsumptionConflicts` |
| `AC-PI-COMP-004` | Control consumption never exceeds canonical cumulative trusted completion. | `SpendControlTransactionConsumptionApplicationServiceTests` plus implementation guard. |
| `AC-PI-COMP-005` | Historical route and binding select the ledger subject; current binding is not consulted. | `PaymentInstrumentTransactionAuthorizationTests#testAuthorizationCompletionAndReversalShouldUseHistoricalSubjectAfterInstrumentRebind` |

## Execution Grant

The user request authorizes this SDLC slice inside the recovered `wind-funds` checkout, including the wallet-face request/interface, transaction-impl orchestration, focused tests, and authoritative docs/OpenSpec updates. It does not authorize network access, Git mutation, DDL execution, deployment, production data, controller/RPC integration, or irreversible operations.

## Repository Integration Inventory

The current checkout contains the facade contract, its local implementation, and test callers only. No issuer, card, clearing, presentment, webhook, or event adapter is owned by this repository, so there is no in-repository caller to migrate. External host adoption is a deployment responsibility outside this Goal; it does not block repository capability delivery and must not be inferred from repository test evidence.

## Review Gates And Residual Risk

| Gate | State | Stop Condition |
| --- | --- | --- |
| Product semantics | `PENDING_OWNER_REVIEW` | Do not expose an issuer status, timeout, or local inference as trusted completion until the product owner confirms event authority and amount semantics. |
| Supported transaction topology | `REPOSITORY_PROVEN` | Spring integration tests must prove funds and control writes share one local transaction and roll back together. A remote or multi-database deployment is a different, unsupported topology requiring a new change. |
| Upstream adoption | `OUTSIDE_REPOSITORY_GOAL` | Repository evidence proves the provider contract only; it neither requires nor claims that an external issuer/card host has deployed it. |
| Refund policy and coupling | `PARTIALLY_CLOSED` | `spend-control-refund-policy-audit` closes the local policy/audit guard; funds refund and optional control compensation remain separate facts without an approved atomic orchestration. |
| Internal bypass | `KNOWN_RISK` | Low-level completion and generic `CONSUMED` recording remain available for account workflows; enforce a stricter access boundary if misuse appears. |
| External practice verification | `PENDING_AUTHORIZATION` | No current provider, scheme, legal, compliance, or accounting rule is asserted by this change. |
