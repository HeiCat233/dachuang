package cn.qihangerp.api.service;

import cn.qihangerp.api.config.VolcEngineConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
        try {
            // 构建请求参数
            Map<String, Object> requestParams = new HashMap<>();
            requestParams.put("model", "ep-20240420172145-k4vfj"); // 生图模型，实际使用时需要替换为真实的模型ID
            requestParams.put("prompt", prompt);
            
            if (referenceImage != null && !referenceImage.isEmpty()) {
                requestParams.put("reference_image", referenceImage);
            }
            
            // 添加生图参数
            if (params != null) {
                requestParams.putAll(params);
            }

            // 发送API请求
            return sendApiRequest("images/generations", requestParams);
        } catch (Exception e) {
            logger.error("生成图片失败", e);
            throw new RuntimeException("生成图片失败: " + e.getMessage());
        }
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
        Map<String, String> headers = buildHeaders(params);
        
        // 创建HTTP客户端
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
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
            HttpResponse response = httpClient.execute(httpPost);
            
            // 处理响应
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            
            // 解析响应
            JSONObject result = JSON.parseObject(responseBody);
            
            // 检查响应状态
            if (result.containsKey("error")) {
                throw new RuntimeException("API请求失败: " + result.getString("error"));
            }
            
            return result;
        }
    }

    /**
     * 构建请求头
     * @param params 请求参数
     * @return 请求头
     */
    private Map<String, String> buildHeaders(Map<String, Object> params) {
        Map<String, String> headers = new HashMap<>();
        
        // 添加Content-Type
        headers.put("Content-Type", "application/json");
        
        // 添加API密钥
        headers.put("X-TOP-Request-Id", java.util.UUID.randomUUID().toString());
        headers.put("X-TOP-Service", "ark");
        headers.put("X-TOP-Region", "cn-beijing");
        headers.put("X-TOP-Account-Id", volcEngineConfig.getApiKeyId());
        
        // 构建签名（实际使用时需要根据火山引擎的签名算法实现）
        // 这里只是一个示例，实际实现需要参考火山引擎的API文档
        String signature = generateSignature(params);
        headers.put("X-TOP-Signature", signature);
        
        return headers;
    }

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
}
