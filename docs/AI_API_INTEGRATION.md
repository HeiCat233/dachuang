# AI API集成和配置说明

## 火山引擎API集成

### 1. 注册火山引擎账号

1. 访问火山引擎官网（https://www.volcengine.com/）
2. 注册并登录账号
3. 进入控制台，创建一个项目
4. 在「API密钥管理」中获取API Key和API Key ID

### 2. 配置API密钥

在`api/src/main/resources/application.yaml`文件中，添加以下配置：

```yaml
volcengine:
  api-key: your-api-key
  api-key-id: your-api-key-id
  service-url: https://ark.cn-beijing.volces.com/api/v3
  timeout: 30000
  retry-count: 3
```

其中：
- `api-key`：火山引擎API密钥
- `api-key-id`：火山引擎API密钥ID
- `service-url`：火山引擎API服务地址
- `timeout`：API请求超时时间（毫秒）
- `retry-count`：API请求失败重试次数

### 3. 配置生图模型

在`VolcEngineApiService.java`文件中，修改生图模型ID：

```java
// 构建请求参数
Map<String, Object> requestParams = new HashMap<>();
requestParams.put("model", "ep-20240420172145-k4vfj"); // 生图模型，实际使用时需要替换为真实的模型ID
requestParams.put("prompt", prompt);
```

### 4. API签名实现

火山引擎API需要使用签名进行身份验证。在`VolcEngineApiService.java`文件中，实现了签名生成逻辑：

```java
/**
 * 生成签名
 * @param params 请求参数
 * @return 签名
 */
private String generateSignature(Map<String, Object> params) {
    // 这里只是一个示例，实际实现需要参考火山引擎的API文档
    // 火山引擎的签名算法通常涉及对请求参数进行排序、拼接，然后使用API密钥进行HMAC-SHA256加密
    try {
        // 对参数进行排序
        TreeMap<String, Object> sortedParams = new TreeMap<>(params);
        
        // 构建签名字符串
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
            sb.append(entry.getKey()).append("=")
              .append(JSON.toJSONString(entry.getValue())).append("&");
        }
        
        // 移除最后一个&符号
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        
        // 添加API密钥
        sb.append(volcEngineConfig.getApiKey());
        
        // 计算MD5签名（实际使用时需要根据火山引擎的签名算法实现）
        byte[] md5Bytes = DigestUtils.md5(sb.toString());
        return Hex.encodeHexString(md5Bytes);
    } catch (Exception e) {
        logger.error("生成签名失败", e);
        throw new RuntimeException("生成签名失败: " + e.getMessage());
    }
}
```

**注意**：上述签名实现只是一个示例，实际使用时需要根据火山引擎的API文档进行调整。

## 数据库配置

### 1. 创建数据库表

执行以下SQL语句创建AI内容生成相关的数据库表：

```sql
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
```

### 2. 添加菜单权限

执行以下SQL语句添加AI内容生成的菜单权限：

```sql
-- 添加AI内容生成菜单
INSERT INTO `sys_menu` VALUES (3000, 'AI内容生成', 0, 80, 'ai', 'Layout', '', 1, 0, 'M', '0', '0', '', 'skill', 'admin', NOW(), 'admin', NOW(), 'AI内容生成目录');
INSERT INTO `sys_menu` VALUES (3001, 'AI生图', 3000, 1, 'generate/image', 'ai/generate/index', '', 1, 0, 'C', '0', '0', '', 'image', 'admin', NOW(), 'admin', NOW(), 'AI图片生成');
INSERT INTO `sys_menu` VALUES (3002, '历史记录', 3000, 2, 'generate/history', 'ai/history/index', '', 1, 0, 'C', '0', '0', '', 'documentation', 'admin', NOW(), 'admin', NOW(), 'AI生成历史记录');
```

## API接口说明

### 1. 创建任务

**接口地址**：`POST /ai/generate/createTask`

**请求参数**：

| 参数名 | 类型 | 必选 | 描述 |
| ------ | ---- | ---- | ---- |
| taskName | String | 是 | 任务名称 |
| description | String | 是 | 文字描述 |
| referenceImage | String | 否 | 参考图片URL |
| imageParams | String | 否 | 生图参数（JSON格式） |

**返回值**：

| 字段名 | 类型 | 描述 |
| ------ | ---- | ---- |
| code | Integer | 状态码，200表示成功 |
| msg | String | 提示信息 |
| data | Long | 任务ID |

### 2. 生成图片

**接口地址**：`POST /ai/generate/generateImage/{taskId}`

**请求参数**：

| 参数名 | 类型 | 必选 | 描述 |
| ------ | ---- | ---- | ---- |
| taskId | Long | 是 | 任务ID |

**返回值**：

| 字段名 | 类型 | 描述 |
| ------ | ---- | ---- |
| code | Integer | 状态码，200表示成功 |
| msg | String | 提示信息 |
| data | Object | 生成结果 |

### 3. 生成文案

**接口地址**：`POST /ai/generate/generateCopywriting/{taskId}`

**请求参数**：

| 参数名 | 类型 | 必选 | 描述 |
| ------ | ---- | ---- | ---- |
| taskId | Long | 是 | 任务ID |

**返回值**：

| 字段名 | 类型 | 描述 |
| ------ | ---- | ---- |
| code | Integer | 状态码，200表示成功 |
| msg | String | 提示信息 |
| data | Object | 生成结果 |

### 4. 一键生成

**接口地址**：`POST /ai/generate/generateAll/{taskId}`

**请求参数**：

| 参数名 | 类型 | 必选 | 描述 |
| ------ | ---- | ---- | ---- |
| taskId | Long | 是 | 任务ID |

**返回值**：

| 字段名 | 类型 | 描述 |
| ------ | ---- | ---- |
| code | Integer | 状态码，200表示成功 |
| msg | String | 提示信息 |
| data | Object | 生成结果 |

### 5. 获取任务列表

**接口地址**：`GET /ai/generate/taskList`

**请求参数**：

| 参数名 | 类型 | 必选 | 描述 |
| ------ | ---- | ---- | ---- |
| page | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页大小，默认10 |
| taskName | String | 否 | 任务名称 |
| status | Integer | 否 | 任务状态 |

**返回值**：

| 字段名 | 类型 | 描述 |
| ------ | ---- | ---- |
| code | Integer | 状态码，200表示成功 |
| msg | String | 提示信息 |
| data | Object | 任务列表 |

### 6. 获取任务详情

**接口地址**：`GET /ai/generate/taskDetail/{taskId}`

**请求参数**：

| 参数名 | 类型 | 必选 | 描述 |
| ------ | ---- | ---- | ---- |
| taskId | Long | 是 | 任务ID |

**返回值**：

| 字段名 | 类型 | 描述 |
| ------ | ---- | ---- |
| code | Integer | 状态码，200表示成功 |
| msg | String | 提示信息 |
| data | Object | 任务详情 |

### 7. 获取生成结果列表

**接口地址**：`GET /ai/generate/resultList/{taskId}`

**请求参数**：

| 参数名 | 类型 | 必选 | 描述 |
| ------ | ---- | ---- | ---- |
| taskId | Long | 是 | 任务ID |

**返回值**：

| 字段名 | 类型 | 描述 |
| ------ | ---- | ---- |
| code | Integer | 状态码，200表示成功 |
| msg | String | 提示信息 |
| data | Object | 结果列表 |

### 8. 获取图片生成结果

**接口地址**：`GET /ai/generate/imageResult/{taskId}`

**请求参数**：

| 参数名 | 类型 | 必选 | 描述 |
| ------ | ---- | ---- | ---- |
| taskId | Long | 是 | 任务ID |

**返回值**：

| 字段名 | 类型 | 描述 |
| ------ | ---- | ---- |
| code | Integer | 状态码，200表示成功 |
| msg | String | 提示信息 |
| data | Object | 图片结果列表 |

### 9. 获取文案生成结果

**接口地址**：`GET /ai/generate/copywritingResult/{taskId}`

**请求参数**：

| 参数名 | 类型 | 必选 | 描述 |
| ------ | ---- | ---- | ---- |
| taskId | Long | 是 | 任务ID |

**返回值**：

| 字段名 | 类型 | 描述 |
| ------ | ---- | ---- |
| code | Integer | 状态码，200表示成功 |
| msg | String | 提示信息 |
| data | Object | 文案结果列表 |

### 10. 删除任务

**接口地址**：`DELETE /ai/generate/deleteTask/{taskId}`

**请求参数**：

| 参数名 | 类型 | 必选 | 描述 |
| ------ | ---- | ---- | ---- |
| taskId | Long | 是 | 任务ID |

**返回值**：

| 字段名 | 类型 | 描述 |
| ------ | ---- | ---- |
| code | Integer | 状态码，200表示成功 |
| msg | String | 提示信息 |
| data | Object | 删除结果 |

## 性能优化

### 1. 请求限流

为了避免API调用过于频繁导致被火山引擎限流，实现了请求限流机制：

- 对每个用户的API调用频率进行限制
- 对同一IP的API调用频率进行限制
- 对系统整体的API调用频率进行限制

### 2. 错误处理

实现了完善的错误处理机制：

- 捕获并处理API调用过程中的异常
- 记录错误日志，便于问题排查
- 对错误进行分类，提供友好的错误提示
- 实现请求重试机制，提高API调用成功率

### 3. 缓存策略

为了提高系统性能，实现了缓存策略：

- 缓存常用的生图参数和文案模板
- 缓存生成结果，避免重复生成
- 缓存API响应，减少重复请求

## 安全考虑

### 1. API密钥管理

- API密钥存储在配置文件中，避免硬编码
- 配置文件不提交到版本控制系统
- 使用环境变量或密钥管理服务管理API密钥

### 2. 数据安全

- 对用户输入进行验证和过滤，防止注入攻击
- 对生成结果进行安全检查，防止恶意内容
- 对敏感数据进行加密存储

### 3. 权限控制

- 使用Spring Security进行权限控制
- 只有授权用户才能访问AI内容生成功能
- 对API接口进行权限验证

## 部署说明

### 1. 环境要求

- Java 17+
- Spring Boot 3.0.2+
- MySQL 8+
- Redis（可选，用于缓存）

### 2. 部署步骤

1. 配置火山引擎API密钥
2. 创建数据库表
3. 添加菜单权限
4. 构建项目：`mvn clean package -DskipTests`
5. 部署项目：`java -jar api/target/api-2.2.0.jar`

### 3. 监控与日志

- 使用ELK或其他日志系统收集和分析日志
- 使用Prometheus或其他监控系统监控系统性能
- 设置告警机制，及时发现和处理问题

## 常见问题

### 1. API调用失败

**原因**：可能是API密钥配置错误、网络问题或火山引擎服务异常
**解决方法**：检查API密钥配置，确保网络连接正常，查看火山引擎服务状态

### 2. 生成图片质量差

**原因**：可能是文字描述不够详细、参考图片不相关或生图参数设置不合理
**解决方法**：优化文字描述，提供更详细的产品特性信息，上传更相关的参考图片，调整生图参数

### 3. 系统性能问题

**原因**：可能是API调用频繁、缓存配置不合理或数据库性能问题
**解决方法**：优化API调用频率，调整缓存策略，优化数据库查询

## 版本历史

- **v1.0.0**：初始版本，支持基本的API集成和配置
- **v1.1.0**：添加了请求限流和错误处理机制
- **v1.2.0**：添加了缓存策略和安全考虑

## 联系方式

如果在集成和配置过程中遇到问题，请联系系统管理员或技术支持。