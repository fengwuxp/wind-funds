# 治理投影恢复 MySQL DDL

`001_create_governance_tables.sql` 是三张治理投影恢复表的首次前向迁移。宿主应把它纳入自己的版本化迁移体系，并在核心资金表和对账表之后执行。

- `t_projection_replay_task` 保存 tenant 有界范围、双高水位 checkpoint、原因、审计与审批引用。
- `t_projection_replay_difference` 只保存差异摘要，不保存敏感原文。
- `t_funds_transaction_projection` 通过 `projection_scope + scope_ref` 隔离 `OFFICIAL` 与 `SHADOW`。

本目录不提供删除回滚，也不替代宿主实际数据库、IAM/审批、调度、告警和生产验收。仓库级门禁执行 H2 与静态 DDL 契约验证；实际数据库兼容由接入宿主负责。
