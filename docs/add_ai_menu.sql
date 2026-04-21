-- 添加AI内容生成菜单
INSERT INTO `sys_menu` VALUES (3000, 'AI内容生成', 0, 80, 'ai', 'Layout', '', 1, 0, 'M', '0', '0', '', 'skill', 'admin', NOW(), 'admin', NOW(), 'AI内容生成目录');
INSERT INTO `sys_menu` VALUES (3001, '内容生成', 3000, 1, 'dashboard', 'ai/dashboard', '', 1, 0, 'C', '0', '0', '', 'star', 'admin', NOW(), 'admin', NOW(), 'AI内容生成');
INSERT INTO `sys_menu` VALUES (3002, '生成历史', 3000, 2, 'history', 'ai/history', '', 1, 0, 'C', '0', '0', '', 'history', 'admin', NOW(), 'admin', NOW(), 'AI生成历史记录');
