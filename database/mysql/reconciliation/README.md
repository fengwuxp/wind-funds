# 对账 MySQL DDL

本目录发布 wind-funds 对账能力的 MySQL 表结构基线，不提供或绑定 Flyway、Liquibase 等运行时。wind-funds 是能力库，宿主应用必须把 SQL 注册到自身版本化迁移体系并保存执行记录与 checksum。

主资金链二十张表的前向迁移位于 `../core/001_create_core_tables.sql`。仓库级目标 MySQL 门禁会先执行主资金链迁移，再执行本目录二十一张对账、清分、清算、结算、出款和追偿表迁移，共回读四十一张表。

## 执行边界

- 目标数据库：MySQL 8.0+、InnoDB。宿主必须在与生产相同的小版本、字符集、排序规则和事务隔离级别完成预发演练。
- `001_create_reconciliation_tables.sql` 是首次建表的前向迁移，不包含 `DROP TABLE` 或 `IF NOT EXISTS`；同名表存在时应失败，禁止掩盖结构漂移。二十一张表统一使用 `utf8mb4_bin`，使流水号、业务引用、摘要和枚举值与 Java 精确字符串语义一致。
- MySQL DDL 会隐式提交，不能把整份脚本当成一个可回滚事务。执行中断后先按迁移记录和 `information_schema` 确认已完成语句，再制定前向修复。
- `001_verify_reconciliation_tables.sql` 必须在部署后执行；它会精确回读二十一张清结算表的表引擎、逐列结构与字符语义、索引唯一性和字段顺序。仓库级 MySQL integration test 同时通过 `information_schema` 精确回读二十张核心表；全部结构检查通过才满足仓库基线。
- 本目录不发布删除表的生产 rollback。初始化失败或应用回退时保留已创建表，停用入口并按部署回读制定前向修复；任何阶段都不得用删表代替回退。

## 宿主上线门禁

1. 备份与恢复演练完成，迁移账号权限、超时、元数据锁等待和失败告警已配置。
2. 对账表为空或确认为首次创建；若已有历史表或数据，必须另写带历史数据校验的增量迁移，不能执行本基线覆盖。
3. 生产同版本 MySQL 下并发验证同一 Gate 根批次竞争、重跑推进当前头、差错动作与后继重跑竞争，以及 Gate 与新差错插入竞争。
4. 最终清分、清算、结算或出款命令必须在自己的本地事务中调用 `ReconciliationGateApplicationService.checkGate`，并在同一事务内完成业务写入；只读 `inspectGate` 和出款预检不能作为提交授权。
5. 上线前记录二十一张表的行数、索引、慢查询基线与锁等待；使用峰值等量数据执行 `EXPLAIN`，确认批次状态与账龄扫描命中 `idx_clearing_split_batch_status_age` / `idx_clearing_batch_status_age`，候选到期、状态账龄和所属批次回查分别命中 `idx_clearing_candidate_status_available` / `idx_clearing_candidate_status_changed` / `idx_clearing_candidate_locked_batch`。任何发现查询出现全表扫描或估算扫描行数超出本轮分页容量，都必须停止准出并调整索引或查询。上线后监控死锁、Gate 阻断率、批次滞留、差错积压和迁移校验结果。

## 仓内验证

`just test-mysql-reconciliation` 只允许连接名为 `wind_funds_reconciliation_test` 的一次性数据库。命令会删除该库现有二十张主资金链表和二十一张对账表，依次执行两份生产前向 DDL 和部署回读，再在同一 MySQL 实例上运行支付工具授权、支出控制和对账回归。该库不得保存任何需要保留的数据。

```shell
export WIND_FUNDS_TEST_MYSQL_DESTRUCTIVE=true
export WIND_FUNDS_TEST_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/wind_funds_reconciliation_test'
export WIND_FUNDS_TEST_DATASOURCE_USERNAME='test_user'
export WIND_FUNDS_TEST_DATASOURCE_PASSWORD='test_password'
export WIND_FUNDS_TEST_MYSQL_EXPECTED_VERSION_PREFIX='8.0.36'
just test-mysql-reconciliation
```

`WIND_FUNDS_TEST_MYSQL_EXPECTED_VERSION_PREFIX` 必须与生产目标小版本一致；迁移测试同时要求 MySQL 产品身份、库名和 `REPEATABLE-READ` 隔离级别匹配。该命令产生仓内可执行证据，不替代宿主的迁移记录、checksum、备份恢复和预发验收。

仓内 H2 测试和 `ReconciliationMysqlDdlContractTests` 只证明 SQL 资产未与测试 schema 漂移，不替代真实 MySQL 并发、性能、备份恢复和宿主事务接入验收。
