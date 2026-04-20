-- 添加AI内容生成菜单
INSERT INTO `sys_menu` VALUES (3000, 'AI内容生成', 0, 80, 'ai', 'Layout', '', 1, 0, 'M', '0', '0', '', 'skill', 'admin', NOW(), 'admin', NOW(), 'AI内容生成目录');
INSERT INTO `sys_menu` VALUES (3001, 'AI生图', 3000, 1, 'generate/image', 'ai/generate/index', '', 1, 0, 'C', '0', '0', '', 'image', 'admin', NOW(), 'admin', NOW(), 'AI图片生成');
INSERT INTO `sys_menu` VALUES (3002, '历史记录', 3000, 2, 'generate/history', 'ai/history/index', '', 1, 0, 'C', '0', '0', '', 'documentation', 'admin', NOW(), 'admin', NOW(), 'AI生成历史记录');
