-- 更新系统名称配置为"元萃智臻后台系统"
UPDATE sys_config 
SET config_value = '元萃智臻后台系统', 
    update_time = NOW() 
WHERE config_key = 'sys.name';

-- 如果不存在则插入
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_time, update_time)
SELECT '系统名称', 'sys.name', '元萃智臻后台系统', 'Y', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.name');
