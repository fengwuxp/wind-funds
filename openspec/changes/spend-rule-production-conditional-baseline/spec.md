# Spend Rule Production Conditional Baseline Evidence

## Change Metadata

| Field | Value |
| --- | --- |
| Change ID | `spend-rule-production-conditional-baseline` |
| Goal ID | `019fac6e-44e7-7bb2-b434-76125f3a3f3e` |
| Status | `ENGINEERING_VERIFIED_CONDITIONAL` |
| Date | `2026-07-30` |
| Product owner | `PENDING` |
| Architecture owner | `PENDING` |
| Engineering evidence owner | `wind-funds` maintainers |
| A2 engineering boundary | `APPROVED_BY_USER_2026-07-30` |

## Context

The current checkout already contains `SpendRuleDefinition`, immutable `SpendRuleVersion`, `SpendRuleBinding`, `SpendRuleDecisionRecord`, a bounded single-rule evaluator, and fail-closed wallet admission. The production-readiness overview still described parts of that service baseline as missing. This change records the actual design-code-test boundary without adding another rule engine, service facade, table, or adapter.

## Business Scenario

For a corporate card or VCC authorization, an owner defines a rule, publishes an immutable version, and binds it to a payment instrument, account, account hierarchy, business scene, or Spend Control scope. A trusted decision producer records the final decision. Wallet admission resolves the applicable binding and verifies the persisted decision before transaction authorization continues with payment-instrument, account-capability, funding-responsibility, and balance checks.

## Scope

### In Scope

- Verify definition, immutable version, binding lifecycle, decision record, bounded evaluation, and admission behavior.
- Verify idempotency, digest conflicts, tenant and binding identity, effective windows, and read-only explanation.
- Verify rejection, ambiguity, and unverifiable evidence fail before funds, route, posting, ledger entry, or balance facts.
- Bind a persisted decision to the payment-instrument binding version, control window, and optional evaluated target account used by admission.
- Align product, system, and TDD status with current source and fresh test evidence.

### Out of Scope

- A general expression engine, scripts, multi-rule conflict composition, rolling-amount or strongly consistent velocity enforcement.
- Controller, HTTP/RPC, trusted external decision adapter, issuer integration, IAM, operations UI, DDL, migration, deployment, Git, or production data.
- Compatibility or compliance claims for Highnote, a card network, an issuer, or another external provider.

## Normative Rules

1. Spend Rule MUST remain a can-spend decision layer and MUST NOT become an accounting subject, funding source, payment instrument, transaction fact, or ledger balance.
2. A published rule version MUST be immutable; an identical replay MAY return the existing version, while digest drift MUST fail closed.
3. A binding MUST identify one rule version, scope, priority, conflict policy, and effective window. Missing or ambiguous binding evidence MUST NOT default to allow.
4. A decision record MUST be tenant-bound, append-only in meaning, idempotent by stable identity and digest, and queryable only through bounded filters.
5. The evaluator MUST remain a read-only candidate decision for one supported published rule. It MUST NOT write a decision record, control movement, funds transaction, route, posting, ledger entry, or balance projection.
6. Wallet admission MUST accept persisted and context-matching decision evidence. Bare results, digest mismatch, unresolved scope, multiple applicable bindings, or tenant mismatch MUST fail closed.
7. A passed Spend Rule decision MUST NOT bypass payment-instrument capability, account capability, funding responsibility, balance, route, or ledger checks.
8. H2 service evidence MUST NOT be represented as production schema, external adapter adoption, operational readiness, or production approval.
9. A decision that identifies a payment instrument MUST bind its actual instrument binding version. `controlScopeId` and `periodId` MUST be both absent or both present. An optional `targetAccountId` MUST contain only a Funding Account or Credit Account subject.
10. Wallet admission MUST compare the persisted instrument binding version and control window with the current payment-instrument snapshot and request. When the decision contains a target account, it MUST also equal the account resolved by the current funding-responsibility snapshot. Missing legacy evidence or any mismatch MUST fail closed without funds side effects.

## Acceptance And Evidence

| ID | Acceptance | Evidence |
| --- | --- | --- |
| `AC-SR-BASE-001` | Definition creation and version publication are idempotent; a published version rejects in-place digest drift. | `SpendRuleDefinitionServiceTests`, `SpendRuleDefinitionServiceFlowTests` |
| `AC-SR-BASE-002` | Binding lifecycle, scope, priority, conflict policy, effective window, identity, query, and explanation are enforced without funds side effects. | `SpendRuleDefinitionServiceTests`, `SpendRuleDefinitionServiceFlowTests`, `SpendRuleBindingServiceTests` |
| `AC-SR-BASE-003` | Decision records are tenant-bound, replay-safe, query-bounded, explainable, and cannot represent caller-supplied `NO_APPLICABLE_RULE`. | `SpendRuleDecisionRecordServiceTests`, `SpendRuleDefinitionServiceFlowTests` |
| `AC-SR-BASE-004` | The bounded evaluator covers supported single-rule controls with stable digest and no funds side effects; unsupported multi-control composition fails fast. | `SpendRuleEvaluationApplicationServiceTests` |
| `AC-SR-BASE-005` | Admission returns explicit no-rule, verifies one persisted decision, and rejects bare, mismatched, unresolved, cross-tenant, or multi-binding evidence without funds side effects. | `SpendControlAdmissionApplicationServiceTests` |
| `AC-SR-BASE-006` | Decision persistence stores and returns instrument binding version, an all-or-nothing control window, and an optional Funding/Credit Account target; idempotent replay includes those fields and rejects context drift. | `SpendRuleDecisionRecordServiceTests` |
| `AC-SR-BASE-007` | Admission rejects cross-scope, cross-period, stale-binding, and mismatched-target-account decision reuse before transaction, route, posting, ledger entry, or balance facts. | `SpendControlAdmissionApplicationServiceTests`, `PaymentInstrumentTransactionAuthorizationTests` |

Fresh A2 verification on 2026-07-30 used Java 21. `SC-LOOP-04` passed 56 tests, and the
module-boundary suite passed 188 tests, all with 0 failures, 0 errors, and 0 skipped.
`just verify-cad` then passed compile, the full test module, PMD, classfile checks, and
code-generation checks across 21 Maven modules: 983 tests, 0 failures, 0 errors, and 1 skipped.

## Repository Integration Inventory

- `PaymentInstrumentAuthorizationProcessor` is the production source caller of `SpendControlAdmissionApplicationService`.
- The current repository has no production caller of `SpendRuleEvaluationApplicationService` or `SpendRuleDecisionRecordService#recordDecision`; those calls remain test evidence.
- The four rule tables exist in `tests/src/test/resources/jdbc-schema.sql`. No corresponding production MySQL migration is present under `database/`.
- Before A2, `decisionDigest` included control scope, period, and optional target account, but `SpendRuleDecisionRecord` did not persist admission-verifiable copies and did not bind the payment-instrument binding version. A caller could therefore reuse a persisted PASSED decision with a different control window or after a binding change.

## A2 Implementation Boundary

- Write: decision record Request/DTO/entity/service mapping, H2 test schema, wallet admission validation, focused wallet/transaction tests, PRD/system/TDD/OpenSpec evidence.
- Read only: PaymentInstrument snapshot and funding-responsibility contracts, transaction authorization orchestration, ledger and posting implementation.
- Compatibility: fields are nullable for non-instrument/non-budget decisions, but a decision used with a payment instrument must carry its binding version; control scope and period are an all-or-nothing pair. Records missing evidence required by the current request fail closed rather than receiving inferred values.
- Target account: optional means the evaluator made a scope-wide decision. When present, it is flattened as subject type and subject id, limited to Funding Account or Credit Account, and compared with the account resolved at admission.
- Non-goals: production MySQL DDL or execution, backfill, controller/RPC, external trusted-host adapter, multi-rule composition, deployment, or Git operations.

## Review Gates And Stop Conditions

| Gate | State | Stop Condition |
| --- | --- | --- |
| Product rule scope | `PENDING_OWNER_REVIEW` | Do not add unsupported rules or multi-rule composition until priority, conflict, final-decision, and historical-explanation semantics are signed off. |
| Trusted decision producer | `OUTSIDE_REPOSITORY_EVIDENCE` | Do not claim end-to-end admission until a trusted host records decisions and passes contract tests. |
| A2 decision context binding | `ENGINEERING_VERIFIED_2026-07-30` | Do not admit a decision whose persisted binding version, control window, or optional target account conflicts with the current wallet snapshot. |
| Production schema and migration | `NOT_IMPLEMENTED` | Do not enable production persistence until DDL, indexes, migration, validation, rollback, and historical-data handling have an approved change boundary. |
| IAM and change audit | `NOT_IMPLEMENTED_IN_THIS_REPOSITORY` | Do not expose rule lifecycle operations until caller authority, operator, reason, approval or audit reference, and before/after digest are traceable. |
| Observability and Runbook | `NOT_IMPLEMENTED` | Do not claim operational readiness until result signals, runtime samples, alerts, thresholds, owners, recovery drills, and acceptance checks are implemented and verified. |
| External practice verification | `LOCAL_REFERENCE_ONLY_NOT_REFRESHED` | Do not claim current Highnote, issuer, or network compatibility without separately authorized source verification. |
