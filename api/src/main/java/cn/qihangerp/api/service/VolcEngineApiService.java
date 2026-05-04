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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class VolcEngineApiService {
    private static final Logger logger = LoggerFactory.getLogger(VolcEngineApiService.class);

    @Autowired
    private VolcEngineConfig volcEngineConfig;

    public JSONObject generateImage(String prompt, String referenceImage, Map<String, Object> params) {
        int retryCount = volcEngineConfig.getRetryCount();
        int currentRetry = 0;
        Exception lastException = null;

        while (currentRetry < retryCount) {
            try {
                Map<String, Object> requestParams = new HashMap<>();
                requestParams.put("model", volcEngineConfig.getModelId());
                requestParams.put("response_format", "url");

                boolean isRefineMode = (referenceImage != null && !referenceImage.isEmpty());

                if (isRefineMode) {
                    logger.info("[图生图模式] 开始验证和处理参考图片");
                    
                    // 验证并处理参考图片
                    String validatedImage = validateAndProcessReferenceImage(referenceImage);
                    
                    requestParams.put("prompt", prompt);
                    requestParams.put("image", validatedImage);  // 使用 image 参数
                    
                    logger.info("[图生图模式] 参考图片验证通过");
                } else {
                    logger.info("[文生图模式] 从文本提示生成新图片");
                    requestParams.put("prompt", prompt);
                    // 设置默认尺寸（如果params中没有指定）
                    if (params == null || !params.containsKey("size")) {
                        requestParams.put("size", "2048x2048");
                    }
                }

                if (params != null) {
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        if (!requestParams.containsKey(entry.getKey())) {
                            requestParams.put(entry.getKey(), entry.getValue());
                        }
                    }
                }

                logger.info("模式: {}, 参考图片: {}", isRefineMode ? "图生图" : "文生图", isRefineMode ? "是" : "否");
                
                // 统一使用Ark API路径
                return sendApiRequest("images/generations", requestParams);
            } catch (Exception e) {
                currentRetry++;
                lastException = e;
                logger.warn("重试第{}次: {}", currentRetry, e.getMessage());
                try {
                    Thread.sleep(1000 * (1 << (currentRetry - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.error("重试{}次后仍然失败", retryCount, lastException);
        throw new RuntimeException("生成失败: " + (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    /**
     * 验证并处理参考图片
     * @param referenceImage 参考图片（Base64或URL）
     * @return 处理后的Base64图片字符串
     */
    private String validateAndProcessReferenceImage(String referenceImage) {
        if (referenceImage == null || referenceImage.isEmpty()) {
            throw new IllegalArgumentException("参考图片不能为空");
        }

        try {
            byte[] imageBytes;
            
            // 处理Data URL格式 (data:image/jpeg;base64,xxx)
            if (referenceImage.startsWith("data:image")) {
                logger.info("检测到Data URL格式图片");
                String base64Data = referenceImage.substring(referenceImage.indexOf(",") + 1);
                imageBytes = Base64.getDecoder().decode(base64Data);
            } 
            // 处理HTTP/HTTPS URL
            else if (referenceImage.startsWith("http://") || referenceImage.startsWith("https://")) {
                logger.warn("URL格式的图片暂未实现下载验证，请确保图片符合火山引擎要求: {}", referenceImage);
                return referenceImage;
            } 
            // 处理纯Base64格式
            else {
                logger.info("检测到纯Base64格式图片");
                imageBytes = Base64.getDecoder().decode(referenceImage);
            }

            // 验证文件大小（最大10MB）
            long fileSizeMB = imageBytes.length / 1024 / 1024;
            if (imageBytes.length > 10 * 1024 * 1024) {
                throw new IllegalArgumentException(String.format(
                    "图片文件大小超过10MB限制，当前大小: %dMB", fileSizeMB));
            }
            logger.info("图片文件大小: {}KB", imageBytes.length / 1024);

            // 验证图片格式和分辨率
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) {
                throw new IllegalArgumentException("无法解析图片，请确保图片格式为JPEG或PNG");
            }

            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            logger.info("图片分辨率: {}x{}", width, height);

            // 验证分辨率（最大4096x4096）
            if (width > 4096 || height > 4096) {
                throw new IllegalArgumentException(String.format(
                    "图片分辨率超过4096x4096限制，当前: %dx%d", width, height));
            }

            // 验证宽高比（Seedream 5.0/4.5/4.0支持 [1/16, 16]）
            double aspectRatio = (double) width / height;
            if (aspectRatio < 1.0/16.0 || aspectRatio > 16.0) {
                throw new IllegalArgumentException(String.format(
                    "图片宽高比超出范围 [1/16, 16]，当前比例: %.2f:1", aspectRatio));
            }
            logger.info("图片宽高比: {:.2f}:1", aspectRatio);

            // 转换为Base64格式（统一使用JPEG格式）
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String result = "data:image/jpeg;base64," + base64Image;
            
            logger.info("图片验证通过，已转换为Base64格式");
            return result;

        } catch (IOException e) {
            logger.error("图片处理失败: {}", e.getMessage());
            throw new IllegalArgumentException("图片处理失败: " + e.getMessage());
        }
    }

    private JSONObject sendApiRequest(String path, Map<String, Object> params) throws IOException {
        // 统一使用Ark API地址
        String url = volcEngineConfig.getServiceUrl() + "/" + path;
        
        Map<String, String> headers = buildHeaders();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(volcEngineConfig.getTimeout())
                .setConnectionRequestTimeout(volcEngineConfig.getTimeout())
                .setSocketTimeout(volcEngineConfig.getTimeout())
                .build();

        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build()) {
            HttpPost httpPost = new HttpPost(url);

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue());
            }

            String requestBody = JSON.toJSONString(params);
            httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

            logger.info("请求URL: {}", url);
            logger.debug("请求体: {}", requestBody);
            
            HttpResponse response = httpClient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            
            logger.info("响应状态: {}, 响应体: {}", response.getStatusLine().getStatusCode(), responseBody);

            JSONObject result = JSON.parseObject(responseBody);
            
            // 检查错误
            if (result.containsKey("error")) {
                String errorMsg = result.getString("error");
                logger.error("API返回错误: {}", errorMsg);
                throw new RuntimeException("API错误: " + errorMsg);
            }

            return result;
        }
    }

    public String generateCopywritingLocal(String productDesc, String style) {
        if (style == null || style.isEmpty()) {
            style = "default";
        }

        String headline = "";
        String body = "";
        String cta = "";

        switch (style) {
            case "humor":
                headline = generateHumorHeadline(productDesc);
                body = generateHumorBody(productDesc);
                cta = "点击下方链接，让快乐继续~";
                break;
            case "emotional":
                headline = generateEmotionalHeadline(productDesc);
                body = generateEmotionalBody(productDesc);
                cta = "这份温暖，我想与你分享";
                break;
            case "inspirational":
                headline = generateInspirationalHeadline(productDesc);
                body = generateInspirationalBody(productDesc);
                cta = "改变从这一刻开始";
                break;
            case "trendy":
                headline = generateTrendyHeadline(productDesc);
                body = generateTrendyBody(productDesc);
                cta = "赶紧 get 同款吧~";
                break;
            default:
                headline = generateDefaultHeadline(productDesc);
                body = generateDefaultBody(productDesc);
                cta = "立即购买，开启品质生活";
        }

        return "✨ " + headline + "\n\n" + body + "\n\n" + cta;
    }

    private String generateHumorHeadline(String desc) {
        String shortDesc = desc != null && desc.length() > 5 ? desc.substring(0, 5) : desc;
        String[] headlines = {
            "笑死！这件" + shortDesc + "的东西让我彻底破防了",
            "救命！" + shortDesc + "的出现直接让我笑到邻居来敲门",
            "OMG！这" + shortDesc + "简直是来报恩的吧",
            "我不允许还有人不知道这个" + shortDesc + "！真的绝",
            "这件神器让我直接原地封神！"
        };
        return headlines[(int)(Math.random() * headlines.length)];
    }

    private String generateHumorBody(String desc) {
        String[] bodies = {
            "作为一个资深网购达人，我用过的" + desc + "没有一百也有八十。\n但是！这款真的让我眼前一亮！\n用完之后直接给我妈也安排上了，我妈说：这孩子终于买对东西了！",
            "本来只是想买来试试水，结果直接被圈粉了！\n" + desc + "，你是我的神！\n用了之后才发现，原来生活可以这么美好~",
            "讲真，一开始我是拒绝的。\n但是用了之后：真香！\n" + desc + "这东西，谁用谁知道，懂的都懂！"
        };
        return bodies[(int)(Math.random() * bodies.length)];
    }

    private String generateEmotionalHeadline(String desc) {
        String shortDesc = desc != null && desc.length() > 5 ? desc.substring(0, 5) : desc;
        String[] headlines = {
            "时光里的温柔，都藏在" + shortDesc + "里",
            "这份美好，我想和你分享",
            "遇见它之后，我开始相信缘分",
            "它不只是产品，更是生活的仪式感",
            "有些东西，值得我们用心对待"
        };
        return headlines[(int)(Math.random() * headlines.length)];
    }

    private String generateEmotionalBody(String desc) {
        String[] bodies = {
            "在这个快节奏的时代，我们总是在寻找那份难得的慢生活。\n\n" + desc + "，给我的不只是一个产品，更是一种生活态度。\n\n每当看到它，就像是给疲惫的心灵找到了一处栖息地。",
            "记得第一次遇见它的时候，心里某个角落被轻轻触动了。\n\n" + desc + "，用它已经有一段时间了，每一天都感受着细微的美好。",
            "有时候，一件好物带来的满足感，是多少钱都买不到的。\n\n" + desc + "，推荐给你，希望它也能为你的生活增添一抹亮色。"
        };
        return bodies[(int)(Math.random() * bodies.length)];
    }

    private String generateInspirationalHeadline(String desc) {
        String shortDesc = desc != null && desc.length() > 5 ? desc.substring(0, 5) : desc;
        String[] headlines = {
            "改变，从这一" + shortDesc + "开始",
            "你值得更好的生活",
            "每一次选择，都是对未来的投票",
            "成为更好的自己，从拥有它开始",
            "生活的方式，由你自己决定"
        };
        return headlines[(int)(Math.random() * headlines.length)];
    }

    private String generateInspirationalBody(String desc) {
        String[] bodies = {
            "人生没有白走的路，每一步都算数。\n\n当你决定提升生活品质的那一刻，你已经赢在了起跑线上。\n\n" + desc + "，就是你的第一步。",
            "我们总是在等待一个契机，让自己变得更好。\n\n现在，契机来了。\n\n" + desc + "，帮助2000+用户开启了品质生活新篇章。",
            "每一个优秀的人，都有一段沉默的时光。\n\n那段时光，是付出努力，静待花开。\n\n" + desc + "，见证你的每一次成长。"
        };
        return bodies[(int)(Math.random() * bodies.length)];
    }

    private String generateTrendyHeadline(String desc) {
        String shortDesc = desc != null && desc.length() > 5 ? desc.substring(0, 5) : desc;
        String[] headlines = {
            shortDesc + "界的天花板，我算是玩明白了",
            "救命！这" + shortDesc + "也太绝了吧",
            "被问了800遍的" + shortDesc + "，真的yyds",
            "这件" + shortDesc + "直接封神，不接受反驳",
            "小红书吹爆的" + shortDesc + "，真的绝"
        };
        return headlines[(int)(Math.random() * headlines.length)];
    }

    private String generateTrendyBody(String desc) {
        String[] bodies = {
            desc + "\n\n真的绝了！用了一次就爱上了！\n上手效果直接拉满，谁用谁知道！\n完全超出预期，性价比逆天！",
            "姐妹们！挖到宝了！\n\n" + desc + "\n\n真的太好用了！\n用了之后就再也离不开了！",
            "这" + desc + "真的绝了绝了的！\n\n我之前也用过不少，但是这款真的让我服气！\n效果肉眼可见，使用感满分！"
        };
        return bodies[(int)(Math.random() * bodies.length)];
    }

    private String generateDefaultHeadline(String desc) {
        String shortDesc = desc != null && desc.length() > 5 ? desc.substring(0, 5) : desc;
        String[] headlines = {
            "发现一款超赞的" + shortDesc + "，必须推荐给你",
            "这款" + shortDesc + "，用了就离不开",
            "品质之选：" + shortDesc,
            "你不能错过的" + shortDesc,
            "这件好物，让我重新认识了什么是品质生活"
        };
        return headlines[(int)(Math.random() * headlines.length)];
    }

    private String generateDefaultBody(String desc) {
        String[] bodies = {
            "今天要给大家安利一款我最近超爱的好物——" + desc + "\n\n用了有一段时间了，真的越用越喜欢！\n品质非常好，使用体验满分！",
            "最近入手了这款" + desc + "，使用感受真的太棒了！\n\n外观设计很精美，材质也很讲究。\n实用性很强，功能齐全。",
            "分享一款让我非常满意的好物——" + desc + "\n\n做工精细，质量上乘。\n设计感十足，美观又实用。"
        };
        return bodies[(int)(Math.random() * bodies.length)];
    }

    /**
     * 构建请求头
     * @return 请求头Map
     */
    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        // 火山引擎API认证
        headers.put("Authorization", "Bearer " + volcEngineConfig.getApiKey().trim());
        
        logger.debug("请求头已构建 - Content-Type: application/json, Authorization: Bearer ***");
        return headers;
    }
}