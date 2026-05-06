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
     * 模型ID（图像生成）
     */
    private String modelId;

    /**
     * 文本模型ID（文案生成）
     */
    private String textModelId;

    /**
     * 服务地址
     */
    private String serviceUrl;

    /**
     * 视觉API地址（用于图生图）
     */
    private String visualServiceUrl;

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

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getTextModelId() {
        return textModelId;
    }

    public void setTextModelId(String textModelId) {
        this.textModelId = textModelId;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getVisualServiceUrl() {
        return visualServiceUrl;
    }

    public void setVisualServiceUrl(String visualServiceUrl) {
        this.visualServiceUrl = visualServiceUrl;
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