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
import java.util.List;
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
                
                // 【增强】构建增强后的提示词，使其更符合“产品宣传图像”风格，并强调保持产品一致性
                String enhancedPrompt = "Professional product photography, studio lighting, high quality, commercial advertisement style, centered product, consistent product appearance, " + prompt;

                if (isRefineMode) {
                    logger.info("[图生图模式] 开始验证和处理参考图片");
                    
                    // 验证并处理参考图片
                    String validatedImage = validateAndProcessReferenceImage(referenceImage);
                    
                    requestParams.put("prompt", enhancedPrompt);
                    // 【适配示例】image 参数为数组格式
                    requestParams.put("image", new String[]{validatedImage});
                    
                } else {
                    logger.info("[文生图模式] 从文本提示生成新图片");
                    requestParams.remove("image");
                    requestParams.put("prompt", enhancedPrompt);
                }

                // 合并其他参数
                if (params != null) {
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        requestParams.put(entry.getKey(), entry.getValue());
                    }
                }

                // 【核心修复：强制覆盖，防止旧任务数据干扰】
                // 针对 3686400 像素报错：必须在合并 params 之后执行，确保 100% 覆盖掉数据库中的旧 size 数据
                if (isRefineMode) {
                    // 图生图模式下绝对不能传 size，否则会报错尺寸不匹配
                    requestParams.remove("size");
                    logger.info("[图生图模式] 已强制移除 size 参数");
                } else {
                    // 文生图模式下强制使用高分辨率，且必须大于 3686400 像素
                    // 2048x2048 = 4,194,304 像素，绝对安全
                    requestParams.put("size", "2048x2048"); 
                    logger.info("[文生图模式] 已强制设置 size 为 2048x2048 (约419万像素)");
                }
                
                // 统一设置官方推荐参数
                requestParams.put("response_format", "url");
                requestParams.put("stream", false);
                requestParams.put("watermark", true);

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
                logger.info("检测到URL格式图片，开始下载: {}", referenceImage);
                
                // 下载图片
                try (java.io.InputStream inputStream = new java.net.URL(referenceImage).openStream()) {
                    imageBytes = inputStream.readAllBytes();
                    logger.info("图片下载成功，大小: {}KB", imageBytes.length / 1024);
                } catch (Exception e) {
                    logger.error("图片下载失败: {}", e.getMessage());
                    throw new IllegalArgumentException("图片下载失败: " + e.getMessage());
                }
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
            logger.info("原始图片分辨率: {}x{}", width, height);

            // 【核心修复】针对模型 3686400 像素的硬性要求，如果原图不足，则自动等比放大
            long totalPixels = (long) width * height;
            if (totalPixels < 3686400) {
                double scale = Math.sqrt(3686400.0 / totalPixels) + 0.1; // 略微多放大一点点以保安全
                int newWidth = (int) (width * scale);
                int newHeight = (int) (height * scale);
                
                logger.info("[自动优化] 检测到原图像素（{}）不足 368.64万，正在自动放大至: {}x{}", totalPixels, newWidth, newHeight);
                
                java.awt.Image scaledImage = bufferedImage.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
                BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                outputImage.getGraphics().drawImage(scaledImage, 0, 0, null);
                
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                ImageIO.write(outputImage, "jpg", baos);
                imageBytes = baos.toByteArray();
                width = newWidth;
                height = newHeight;
            }

            // 再次验证分辨率（最大4096x4096）
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

            logger.info("========== 火山引擎API调用开始 ==========");
            logger.info("请求URL: {}", url);
            logger.info("请求方法: POST");
            logger.info("请求体: {}", requestBody);
            logger.info("==========================================");
            
            HttpResponse response = httpClient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            
            logger.info("========== 火山引擎API响应 ==========");
            logger.info("响应状态码: {}", response.getStatusLine().getStatusCode());
            logger.info("响应体: {}", responseBody);
            logger.info("=====================================");

            JSONObject result = JSON.parseObject(responseBody);
            
            // 检查错误
            if (result.containsKey("error")) {
                String errorMsg = result.getString("error");
                logger.error("API返回错误: {}", errorMsg);
                throw new RuntimeException("API错误: " + errorMsg);
            }
            
            // 记录用量信息（如果有）
            if (result.containsKey("usage")) {
                logger.info("API用量信息: {}", result.getJSONObject("usage").toJSONString());
            } else {
                logger.warn("API响应中未包含用量信息");
            }

            return result;
        }
    }

    /**
     * 调用火山引擎API生成文案
     * @param productDesc 产品描述
     * @param style 文案风格
     * @return 生成的文案
     */
    public String generateCopywritingByAPI(String productDesc, String style) {
        try {
            Map<String, Object> requestParams = new HashMap<>();
            
            // 【重要】文案生成需要使用对话模型，而不是图像模型
            String chatModelId = volcEngineConfig.getTextModelId();
            
            // 如果没有配置文本模型ID，尝试回退到通用模型ID
            if (chatModelId == null || chatModelId.isEmpty() || chatModelId.contains("xxxxx")) {
                chatModelId = volcEngineConfig.getModelId();
                logger.warn("[文案生成API] 未配置有效的文本模型ID，尝试使用通用模型ID: {}", chatModelId);
            } else {
                logger.info("[文案生成API] 使用配置的文本模型ID: {}", chatModelId);
            }
            
            requestParams.put("model", chatModelId);
            
            // 【重要】构建 messages 数组格式（聊天 API 标准格式）
            List<Map<String, String>> messages = new java.util.ArrayList<>();
            
            // 系统提示词
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位专业的电商文案策划师，擅长为各种产品创作吸引人的营销文案。");
            messages.add(systemMessage);
            
            // 用户消息
            String userPrompt = buildCopywritingPrompt(productDesc, style);
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);
            
            requestParams.put("messages", messages);
            requestParams.put("stream", false);
            
            logger.info("[文案生成API] 开始调用火山引擎API");
            logger.info("[文案生成API] 产品描述: {}", productDesc);
            logger.info("[文案生成API] 文案风格: {}", style);
            logger.info("[文案生成API] 请求参数: model={}, messages数量={}", chatModelId, messages.size());
            
            // 【重要】使用 chat/completions 端点，而不是 completions
            JSONObject result = sendApiRequest("chat/completions", requestParams);
            
            logger.info("[文案生成API] API响应接收成功");
            
            // 解析响应结果
            if (result.containsKey("choices")) {
                com.alibaba.fastjson.JSONArray choices = result.getJSONArray("choices");
                logger.info("[文案生成API] 解析choices数组，大小: {}", choices != null ? choices.size() : 0);
                
                if (choices != null && choices.size() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject message = firstChoice.getJSONObject("message");
                    if (message != null) {
                        String content = message.getString("content");
                        logger.info("[文案生成API] 文案生成成功，内容长度: {}", content != null ? content.length() : 0);
                        return content;
                    } else {
                        logger.warn("[文案生成API] message字段为空");
                    }
                } else {
                    logger.warn("[文案生成API] choices数组为空");
                }
            } else {
                logger.warn("[文案生成API] 响应中不包含choices字段，完整响应: {}", result.toJSONString());
            }
            
            logger.warn("[文案生成API] API响应格式异常，降级使用本地模板");
            return generateCopywritingLocal(productDesc, style);
            
        } catch (Exception e) {
            logger.error("[文案生成API] API调用失败: {}", e.getMessage());
            logger.warn("[文案生成API] 降级使用本地模板生成文案");
            return generateCopywritingLocal(productDesc, style);
        }
    }
    
    /**
     * 构建文案生成提示词
     * @param productDesc 产品描述
     * @param style 文案风格
     * @return 完整的提示词
     */
    private String buildCopywritingPrompt(String productDesc, String style) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一位专业的电商文案策划师，请为以下产品生成营销文案。\n\n");
        prompt.append("产品信息：").append(productDesc).append("\n\n");
        
        switch (style) {
            case "humor":
                prompt.append("风格要求：幽默风趣、轻松活泼，可以使用网络流行语和夸张的表达方式，让读者会心一笑。\n");
                prompt.append("文案结构：\n");
                prompt.append("1. 吸引眼球的标题（15字以内）\n");
                prompt.append("2. 有趣的产品介绍（50-80字）\n");
                prompt.append("3. 俏皮的行动号召\n");
                break;
            case "emotional":
                prompt.append("风格要求：温情走心、情感共鸣，营造温暖感人的氛围，触动读者内心。\n");
                prompt.append("文案结构：\n");
                prompt.append("1. 富有诗意的标题（15字以内）\n");
                prompt.append("2. 温情的故事化描述（50-80字）\n");
                prompt.append("3. 温暖的行动号召\n");
                break;
            case "inspirational":
                prompt.append("风格要求：励志向上、正能量满满，激励读者追求更好的生活。\n");
                prompt.append("文案结构：\n");
                prompt.append("1. 鼓舞人心的标题（15字以内）\n");
                prompt.append("2. 激励性的产品介绍（50-80字）\n");
                prompt.append("3. 有力的行动号召\n");
                break;
            case "trendy":
                prompt.append("风格要求：时尚潮流、年轻化表达，使用小红书风格的文案，充满种草力。\n");
                prompt.append("文案结构：\n");
                prompt.append("1. 吸睛的网红风标题（15字以内）\n");
                prompt.append("2. 种草式产品介绍（50-80字）\n");
                prompt.append("3. 紧迫感行动号召\n");
                break;
            default:
                prompt.append("风格要求：专业得体、突出产品优势和卖点，简洁明了。\n");
                prompt.append("文案结构：\n");
                prompt.append("1. 清晰的标题（15字以内）\n");
                prompt.append("2. 详细的产品介绍（50-80字）\n");
                prompt.append("3. 明确的行动号召\n");
        }
        
        prompt.append("\n请直接输出文案内容，不要包含任何解释说明。");
        
        return prompt.toString();
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