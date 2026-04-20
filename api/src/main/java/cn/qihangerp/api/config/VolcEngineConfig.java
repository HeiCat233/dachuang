package cn.qihangerp.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 火山引擎API配置
 */
@Component
@ConfigurationProperties(prefix = "volcengine")
public class VolcEngineConfig {
    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API密钥ID
     */
    private String apiKeyId;

    /**
     * 服务地址
     */
    private String serviceUrl;

    /**
     * 超时时间（毫秒）
     */
    private int timeout = 30000;

    /**
     * 重试次数
     */
    private int retryCount = 3;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}