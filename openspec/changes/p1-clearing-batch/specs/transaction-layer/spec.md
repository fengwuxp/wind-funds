# Transaction Layer Spec Delta

## ADDED Requirements

### Requirement: Clearing confirmation must use clearing batch as the source fact

Future P1 clearing confirmation integration MUST use the clearing batch as the independent funds source fact.

#### Scenario: Clearing confirmation funds instruction

- WHEN a clearing batch confirmation initiates a funds instruction
- THEN the instruction MUST reference source fact type `CLEARING_BATCH`
- AND source fact number MUST be the clearing batch number
- AND source fact version MUST be the clearing batch version
- AND `businessScene/businessSn` MUST identify the current upstream confirmation action only
- AND `ClearingCandidate` MUST NOT be used as a posting source fact
- AND the design MUST NOT reintroduce unbounded `sourceObjectType/sourceObjectSn` fields
