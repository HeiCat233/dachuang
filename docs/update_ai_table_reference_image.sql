-- 修改 ai_generate_task 表的 reference_image 字段类型
-- 从 VARCHAR(500) 改为 LONGTEXT，以支持存储 Base64 编码的图片数据

ALTER TABLE `ai_generate_task` 
MODIFY COLUMN `reference_image` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参考图片URL或Base64数据';
