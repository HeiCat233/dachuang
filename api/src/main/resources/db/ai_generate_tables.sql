-- AI内容生成相关数据库表

-- AI内容生成任务表
CREATE TABLE IF NOT EXISTS `ai_generate_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_name` VARCHAR(100) DEFAULT NULL COMMENT '任务名称',
  `description` TEXT COMMENT '文字描述',
  `reference_image` TEXT COMMENT '参考图片URL或Base64',
  `image_params` TEXT COMMENT '生图参数（JSON格式）',
  `status` INT DEFAULT 0 COMMENT '任务状态：0-待处理，1-处理中，2-成功，3-失败',
  `error_message` TEXT COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_by` (`create_by`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI内容生成任务表';

-- AI内容生成结果表
CREATE TABLE IF NOT EXISTS `ai_generate_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `result_type` INT DEFAULT 1 COMMENT '结果类型：1-图片，2-文案',
  `result_content` TEXT COMMENT '结果内容（图片URL或文案内容）',
  `generate_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_result_type` (`result_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI内容生成结果表';
