-- AI内容生成任务表
CREATE TABLE `ai_generate_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文字描述',
  `reference_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参考图片URL',
  `image_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生图参数（JSON格式）',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-处理中，2-成功，3-失败',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '错误信息',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI内容生成任务表' ROW_FORMAT = DYNAMIC;

-- AI内容生成结果表
CREATE TABLE `ai_generate_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `result_type` int NOT NULL COMMENT '结果类型：1-图片，2-文案',
  `result_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '结果内容（图片URL或文案文本）',
  `generate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id` (`task_id` ASC) USING BTREE,
  INDEX `idx_result_type` (`result_type` ASC) USING BTREE,
  CONSTRAINT `fk_task_id` FOREIGN KEY (`task_id`) REFERENCES `ai_generate_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI内容生成结果表' ROW_FORMAT = DYNAMIC;