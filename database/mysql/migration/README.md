# State 命名迁移说明

本目录只为已存在的 MySQL 8.0.4+ 宿主提供一次性升级资产。新建数据库直接执行 `core`、`reconciliation`、`governance` 下的 `001_create_*`，不得再执行本目录 forward SQL。

## 执行前

1. 确认宿主已经备份数据库，并验证备份可恢复。
2. 记录数据库版本、目标 schema，以及 forward SQL 输出的 27 列矩阵、20 索引矩阵、24 表精确行数和枚举分布。
3. 停止并排空所有旧应用实例、任务、消息消费者和批处理，确认没有旧 SQL 继续访问 `status` 等源列。
4. 预检必须得到 27 行 `source_exists=1、target_exists=0`，20 个源索引名称和列顺序精确匹配且目标索引不存在；其他结果由 guard 硬失败并停止。
5. 保存迁移前枚举分布输出，作为迁移后逐行比对基线。
6. mysql client 禁止使用 `--force`；必须检查非零退出码，任何 guard 或 DDL 错误都不得继续启动应用。

## Forward

在维护窗口内使用同一目标 schema 执行：

```bash
mysql --batch --raw --database=<wind_funds_host_schema> < database/mysql/migration/20260826_state_naming_forward.sql
```

执行后必须确认：

- 27 行结构检查全部为 `source_exists=0、target_exists=1`。
- 20 个源索引不存在，目标索引名称和列顺序与 canonical DDL 完全一致。
- 24 张表的迁移前后 `row_count` 完全一致。
- 迁移前后枚举分布仅列名变化，`value_code/row_count` 完全一致。
- 新应用使用同一版本的 SQL、Entity 和 schema 启动，完成最小资金、账本、交易、对账和投影 smoke test。
- 监控应用启动失败、未知列 SQL、资金流程错误率、对账差错和投影 backlog。

## 中途失败

MySQL 多表 DDL 会逐句提交，forward/rollback 不具备跨表事务原子性。任一语句失败后：

1. 保持新旧应用全部停止，不得使用 `--force` 跳过错误，也不得直接重跑完整脚本。
2. 重新执行失败脚本的预检查询段，保存 27 列和 20 索引矩阵；每个映射只能处于“仅源存在”或“仅目标存在”，双存在、双缺失或索引列顺序异常必须交 DBA 处理。
3. 选择单一方向：继续 forward 时，只从原脚本复制仍为“仅源存在”的 `RENAME COLUMN/INDEX`；回退时，只从 rollback 脚本复制仍为“仅源存在”的反向语句。生成的恢复 SQL 必须由第二人按矩阵逐项复核，禁止修改状态值或执行数据 DML。
4. 恢复 SQL 执行后重新运行完整脚本的 postcheck 查询段，核对结构、索引、24 表行数和枚举分布；未全部通过不得启动任一版本应用。

脚本 guard 只允许“全量旧结构”进入完整 forward、只允许“全量新结构”进入完整 rollback；部分状态只能按上述矩阵生成受控恢复 SQL，避免完整脚本在已完成对象上二次 rename。

## Rollback

只有新应用无法继续且尚未接受前滚修复时回滚：

1. 停止并排空全部新应用实例和任务。
2. 保存当前 27 列、20 索引、24 表行数和枚举分布证据。
3. 不使用 `--force` 执行 `20260826_state_naming_rollback.sql`，并检查 client 退出码。
4. 确认 27 行结构全部恢复、20 个旧索引名称和列顺序正确、24 表行数及分布一致，再启动旧应用。
5. 重跑旧版本最小 smoke test，并持续观察未知列 SQL 和资金流程错误率。

## 证据边界

仓库不会自动执行这两个脚本。H2、静态 DDL、编译和单元/集成测试不能证明真实 MySQL 的 metadata lock 时长、在线 DDL 能力、宿主停机窗口、备份可恢复性或生产流量结果；这些证据由采用该迁移的宿主 Owner 负责。
