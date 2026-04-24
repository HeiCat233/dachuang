package cn.qihangerp.api.service;

import cn.qihangerp.api.config.VolcEngineConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 火山引擎API服务
 */
@Service
public class VolcEngineApiService {
    private static final Logger logger = LoggerFactory.getLogger(VolcEngineApiService.class);

    @Autowired
    private VolcEngineConfig volcEngineConfig;

    /**
     * 生成图片
     * @param prompt 文字描述
     * @param referenceImage 参考图片URL
     * @param params 生图参数
     * @return 生成结果
     */
    public JSONObject generateImage(String prompt, String referenceImage, Map<String, Object> params) {
        int retryCount = volcEngineConfig.getRetryCount();
        int currentRetry = 0;
        Exception lastException = null;

        while (currentRetry < retryCount) {
            try {
                // 构建请求参数
                Map<String, Object> requestParams = new HashMap<>();
                requestParams.put("model", volcEngineConfig.getModelId()); // 使用配置的模型ID
                requestParams.put("prompt", prompt);

                // 针对不同模型的参数适配
                if (referenceImage != null && !referenceImage.isEmpty()) {
                    // 常见的火山引擎 Ark 生图模型参数
                    requestParams.put("reference_image", referenceImage);
                    // 兼容某些模型可能需要的参数名
                    // requestParams.put("image_url", referenceImage);
                }

                // 默认参数
                if (!requestParams.containsKey("n")) {
                    requestParams.put("n", 1);
                }
                if (!requestParams.containsKey("size")) {
                    // 根据报错信息：image size must be at least 3686400 pixels
                    // 1440 * 2560 = 3,686,400
                    // 2048 * 2048 = 4,194,304
                    requestParams.put("size", "2048x2048");
                }

                // 添加生图参数（从任务中传递过来的额外参数）
                if (params != null) {
                    requestParams.putAll(params);
                }

                // 强制校验 size，确保满足模型最小像素要求 (3686400 pixels)
                Object sizeObj = requestParams.get("size");
                if (sizeObj == null || "1024x1024".equals(sizeObj.toString()) || "768x1024".equals(sizeObj.toString()) || "1024x768".equals(sizeObj.toString())) {
                    logger.info("[VolcEngine] 检测到尺寸过小，强制调整为 2048x2048 以满足模型要求");
                    requestParams.put("size", "2048x2048");
                }

                // 发送API请求
                return sendApiRequest("images/generations", requestParams);
            } catch (Exception e) {
                currentRetry++;
                lastException = e;
                logger.warn("生成图片失败，第{}次重试: {}", currentRetry, e.getMessage());
                try {
                    // 指数退避策略
                    Thread.sleep(1000 * (1 << (currentRetry - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.error("生成图片失败，已达到最大重试次数", lastException);
        throw new RuntimeException("生成图片失败: " + (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    /**
     * 发送API请求
     * @param path API路径
     * @param params 请求参数
     * @return 响应结果
     * @throws IOException IO异常
     */
    private JSONObject sendApiRequest(String path, Map<String, Object> params) throws IOException {
        // 构建请求URL
        String url = volcEngineConfig.getServiceUrl() + "/" + path;

        // 构建请求头
        Map<String, String> headers = buildHeaders();

        // 创建HTTP客户端，设置超时时间
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(volcEngineConfig.getTimeout())
                .setConnectionRequestTimeout(volcEngineConfig.getTimeout())
                .setSocketTimeout(volcEngineConfig.getTimeout())
                .build();

        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build()) {
            // 创建HTTP POST请求
            HttpPost httpPost = new HttpPost(url);

            // 设置请求头
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue());
            }

            // 设置请求体
            String requestBody = JSON.toJSONString(params);
            httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

            // 发送请求
            logger.info("[VolcEngine] 发送API请求: URL={}, Method=POST, Body={}", url, requestBody);
            logger.debug("[VolcEngine] 请求头: {}", headers);
            
            long startTime = System.currentTimeMillis();
            HttpResponse response = httpClient.execute(httpPost);
            long endTime = System.currentTimeMillis();

            // 处理响应
            HttpEntity entity = response.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";
            int statusCode = response.getStatusLine().getStatusCode();
            
            logger.info("[VolcEngine] 收到响应: Status={}, Time={}ms", statusCode, endTime - startTime);
            
            if (statusCode != 200) {
                logger.error("[VolcEngine] API调用失败: Status={}, Response={}", statusCode, responseBody);
                throw new RuntimeException("火山引擎API调用失败，状态码: " + statusCode + ", 响应: " + responseBody);
            }

            // 解析响应
            JSONObject result;
            try {
                result = JSON.parseObject(responseBody);
            } catch (Exception e) {
                logger.error("[VolcEngine] 解析响应内容失败: Body={}", responseBody);
                throw new RuntimeException("解析火山引擎响应失败: " + e.getMessage());
            }

            if (result.containsKey("error")) {
                logger.error("[VolcEngine] API返回业务错误: {}", result.get("error"));
                throw new RuntimeException("火山引擎业务错误: " + result.get("error").toString());
            }

            return result;
        }
    }

    /**
     * 构建请求头
     * @return 请求头
     */
    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();

        // 添加Content-Type
        headers.put("Content-Type", "application/json");

        // 检查ApiKey
        String apiKey = volcEngineConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty() || "your-api-key".equals(apiKey)) {
            logger.error("[VolcEngine] API Key 未配置或为默认值，请检查 application.yaml 中的 volcengine.api-key 配置");
            throw new RuntimeException("火山引擎 API Key 未正确配置");
        }

        // 添加Bearer Token认证头
        headers.put("Authorization", "Bearer " + apiKey);

        return headers;
    }
}