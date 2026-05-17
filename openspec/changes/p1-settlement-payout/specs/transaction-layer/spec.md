# Transaction Layer Spec Delta

## ADDED Requirements

### Requirement: Settlement and payout posting must use explicit source facts

Future P1 settlement and payout integration MUST use settlement and payout facts as independent funds source facts.

#### Scenario: Settlement lock funds instruction

- WHEN a settlement order lock initiates a funds instruction
- THEN the instruction MUST reference source fact type `SETTLEMENT_ORDER`
- AND source fact number MUST be the settlement order number
- AND source fact version MUST be the settlement order version
- AND `businessScene/businessSn` MUST identify the current upstream lock action only

#### Scenario: Payout result funds instruction

- WHEN payout success or payout failure restore initiates a funds instruction
- THEN the instruction MUST reference source fact type `PAYOUT_ORDER`
- AND source fact number MUST be the payout order number
- AND source fact version MUST be the payout order version
- AND the instruction MUST preserve external receipt reference when available
- AND `SettlementLine` and `PayoutReceipt` MUST NOT be used as posting source facts
- AND the design MUST NOT reintroduce unbounded `sourceObjectType/sourceObjectSn` fields
