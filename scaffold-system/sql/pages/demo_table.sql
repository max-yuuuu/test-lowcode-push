-- 测试模型（模型 Demo，表 demo）
-- 由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。要改结构请回到设计器改模型，
-- 在这里手改的内容下次发布会被整文件替换掉。
--
-- 怎么执行：平台部署会自动执行本目录；手工执行时先跑 sql/V1__init_schema.sql 与 sql/V2__seed_data.sql。
-- 本目录不在 docker-entrypoint-initdb.d 的递归范围内（mysql 不递归子目录），由部署动作或人显式执行。
--
-- 新库直接建表；老库按 information_schema 判重后补列：模型后加的字段会跟着部署/同步自动补上。
-- 列类型变更、重命名与删除字段仍不会自动处理（缩窄类型或 DROP 可能丢数据），需要人工确认。
-- 本文件可重复执行：建表是 IF NOT EXISTS，补列先查列是否存在，已存在就 DO 0。

CREATE TABLE IF NOT EXISTS demo (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(255) DEFAULT NULL COMMENT '名称',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：1 删除，0 正常',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试模型';

-- 老表补列：每个模型字段一条条件 ALTER，列已存在时跳过。
SET @add_col_1 = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'demo' AND column_name = 'name') = 0,
    'ALTER TABLE demo ADD COLUMN name VARCHAR(255) DEFAULT NULL COMMENT ''名称''',
    'DO 0');
PREPARE stmt FROM @add_col_1;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @add_deleted = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'demo' AND column_name = 'deleted') = 0,
    'ALTER TABLE demo ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @add_deleted;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @add_create_by = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'demo' AND column_name = 'create_by') = 0,
    'ALTER TABLE demo ADD COLUMN create_by VARCHAR(64) DEFAULT NULL',
    'DO 0');
PREPARE stmt FROM @add_create_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @add_update_by = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'demo' AND column_name = 'update_by') = 0,
    'ALTER TABLE demo ADD COLUMN update_by VARCHAR(64) DEFAULT NULL',
    'DO 0');
PREPARE stmt FROM @add_update_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @add_create_time = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'demo' AND column_name = 'create_time') = 0,
    'ALTER TABLE demo ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP',
    'DO 0');
PREPARE stmt FROM @add_create_time;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @add_update_time = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'demo' AND column_name = 'update_time') = 0,
    'ALTER TABLE demo ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'DO 0');
PREPARE stmt FROM @add_update_time;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
