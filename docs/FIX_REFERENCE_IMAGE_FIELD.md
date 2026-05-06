# 修复 AI 生成任务表 reference_image 字段

## 问题描述

`ai_generate_task` 表的 `reference_image` 字段定义为 `VARCHAR(500)`，无法存储 Base64 编码的图片数据（通常会超过几千甚至几万字符）。

## 解决方案

将字段类型从 `VARCHAR(500)` 改为 `LONGTEXT`，以支持存储完整的 Base64 图片数据。

## 执行步骤

### 方法 1：使用 MySQL 客户端工具（推荐）

1. 打开你的 MySQL 管理工具（如 Navicat、MySQL Workbench、phpMyAdmin 等）
2. 连接到数据库 `qihang-oms`
3. 执行以下 SQL 语句：

```sql
ALTER TABLE `ai_generate_task` 
MODIFY COLUMN `reference_image` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参考图片URL或Base64数据';
```

### 方法 2：使用命令行

如果你的 MySQL 在 PATH 中，可以执行：

```bash
mysql -u root -p qihang-oms < docs/update_ai_table_reference_image.sql
```

或者：

```bash
mysql -u root -p123456 qihang-oms -e "ALTER TABLE ai_generate_task MODIFY COLUMN reference_image LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参考图片URL或Base64数据';"
```

### 方法 3：直接在数据库中执行

复制上面的 SQL 语句，在你的数据库管理工具中直接执行。

## 验证修改

执行以下 SQL 检查字段是否修改成功：

```sql
DESCRIBE ai_generate_task;
```

应该看到 `reference_image` 字段的类型为 `longtext`。

## 注意事项

- `LONGTEXT` 最大可存储 4GB 的数据，足够存储任何合理的 Base64 图片
- 如果担心存储空间，可以考虑将图片上传到 OSS/云存储，只存储 URL
- 修改后无需重启应用，立即生效
