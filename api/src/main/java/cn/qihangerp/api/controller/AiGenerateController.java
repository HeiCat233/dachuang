package cn.qihangerp.api.controller;

import cn.qihangerp.common.AjaxResult;
import cn.qihangerp.model.entity.AiGenerateTask;
import cn.qihangerp.model.entity.AiGenerateResult;
import cn.qihangerp.module.service.AiGenerateTaskService;
import cn.qihangerp.module.service.AiGenerateResultService;
import cn.qihangerp.security.common.SecurityUtils;
import cn.qihangerp.api.service.VolcEngineApiService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * AI内容生成控制器
 */
@RestController
@RequestMapping("/ai/generate")
public class AiGenerateController {

    private static final Logger logger = LoggerFactory.getLogger(AiGenerateController.class);

    @Autowired
    private AiGenerateTaskService taskService;

    @Autowired
    private AiGenerateResultService resultService;

    @Autowired
    private VolcEngineApiService volcEngineApiService;

    /**
     * 创建生成任务
     * @param task 任务信息
     * @return 任务ID
     */
    @PostMapping("/createTask")
    public AjaxResult createTask(@RequestBody AiGenerateTask task) {
        try {
            Long userId = SecurityUtils.getUserId();
            task.setCreateBy(userId.toString());
            Long taskId = taskService.createTask(task);
            return AjaxResult.success(taskId);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 生成图片
     * @param taskId 任务ID
     * @return 生成结果
     */
    @PostMapping("/generateImage/{taskId}")
    public AjaxResult generateImage(@PathVariable Long taskId) {
        logger.info("[AiGenerate] 收到生图请求: TaskId={}", taskId);
        try {
            // 查询任务信息
            AiGenerateTask task = taskService.queryTaskById(taskId);
            if (task == null) {
                logger.error("[AiGenerate] 生图失败: 任务不存在, TaskId={}", taskId);
                return AjaxResult.error("任务不存在");
            }

            // 更新任务状态为处理中
            taskService.updateTaskStatus(taskId, 1, null);

            // 解析生图参数
            Map<String, Object> imageParams = null;
            if (task.getImageParams() != null && !task.getImageParams().isEmpty()) {
                imageParams = JSON.parseObject(task.getImageParams(), Map.class);
            }

            // 调用火山引擎API生成图片
            JSONObject result = volcEngineApiService.generateImage(
                    task.getDescription(),
                    task.getReferenceImage(),
                    imageParams
            );

            logger.info("[AiGenerate] 火山引擎API调用完成: TaskId={}, Result={}", taskId, result.toJSONString());

            // 保存生成结果
            if (result.containsKey("data")) {
                // 火山引擎 Ark API 返回的 data 通常是一个数组
                Object dataObj = result.get("data");
                if (dataObj instanceof com.alibaba.fastjson.JSONArray) {
                    com.alibaba.fastjson.JSONArray dataArray = (com.alibaba.fastjson.JSONArray) dataObj;
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONObject image = dataArray.getJSONObject(i);
                        AiGenerateResult generateResult = new AiGenerateResult();
                        generateResult.setTaskId(taskId);
                        generateResult.setResultType(1); // 1-图片
                        generateResult.setResultContent(image.getString("url"));
                        generateResult.setGenerateTime(new java.util.Date());
                        generateResult.setCreateBy(task.getCreateBy());
                        generateResult.setCreateTime(new java.util.Date());
                        resultService.saveResult(generateResult);
                    }
                } else if (dataObj instanceof JSONObject) {
                    // 兼容某些模型返回对象的情况
                    JSONObject data = (JSONObject) dataObj;
                    if (data.containsKey("images")) {
                        List<JSONObject> images = data.getJSONArray("images").toJavaList(JSONObject.class);
                        for (JSONObject image : images) {
                            AiGenerateResult generateResult = new AiGenerateResult();
                            generateResult.setTaskId(taskId);
                            generateResult.setResultType(1); // 1-图片
                            generateResult.setResultContent(image.getString("url"));
                            generateResult.setGenerateTime(new java.util.Date());
                            generateResult.setCreateBy(task.getCreateBy());
                            generateResult.setCreateTime(new java.util.Date());
                            resultService.saveResult(generateResult);
                        }
                    }
                }
            }

            // 更新任务状态为成功
            taskService.updateTaskStatus(taskId, 2, null);

            return AjaxResult.success("图片生成成功");
        } catch (Exception e) {
            // 更新任务状态为失败
            taskService.updateTaskStatus(taskId, 3, e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 生成文案
     * @param taskId 任务ID
     * @return 生成结果
     */
    @PostMapping("/generateCopywriting/{taskId}")
    public AjaxResult generateCopywriting(@PathVariable Long taskId) {
        logger.info("[AiGenerate] 收到文案生成请求: TaskId={}", taskId);
        try {
            // 查询任务信息
            AiGenerateTask task = taskService.queryTaskById(taskId);
            if (task == null) {
                logger.error("[AiGenerate] 文案生成失败: 任务不存在, TaskId={}", taskId);
                return AjaxResult.error("任务不存在");
            }

            // 更新任务状态为处理中
            taskService.updateTaskStatus(taskId, 1, null);

            // 解析生图参数获取文案风格
            String style = "default";
            if (task.getImageParams() != null && !task.getImageParams().isEmpty()) {
                try {
                    Map<String, Object> params = JSON.parseObject(task.getImageParams(), Map.class);
                    if (params != null && params.containsKey("style")) {
                        style = params.get("style").toString();
                    }
                } catch (Exception e) {
                    logger.warn("[AiGenerate] 解析生图参数失败，使用默认风格: {}", e.getMessage());
                }
            }

            // 调用文案生成服务（本地模板生成有趣文案）
            String copywriting = volcEngineApiService.generateCopywritingLocal(
                    task.getDescription(),
                    style
            );

            logger.info("[AiGenerate] 文案生成完成: TaskId={}, Style={}", taskId, style);

            // 保存生成结果
            AiGenerateResult generateResult = new AiGenerateResult();
            generateResult.setTaskId(taskId);
            generateResult.setResultType(2); // 2-文案
            generateResult.setResultContent(copywriting);
            generateResult.setGenerateTime(new java.util.Date());
            generateResult.setCreateBy(task.getCreateBy());
            generateResult.setCreateTime(new java.util.Date());
            resultService.saveResult(generateResult);

            // 更新任务状态为成功
            taskService.updateTaskStatus(taskId, 2, null);
            logger.info("[AiGenerate] 文案生成成功: TaskId={}", taskId);

            return AjaxResult.success("文案生成成功");
        } catch (Exception e) {
            logger.error("[AiGenerate] 文案生成异常: TaskId={}, Error={}", taskId, e.getMessage(), e);
            // 更新任务状态为失败
            taskService.updateTaskStatus(taskId, 3, e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 一键生成（图片+文案）
     * @param taskId 任务ID
     * @return 生成结果
     */
    @PostMapping("/generateAll/{taskId}")
    public AjaxResult generateAll(@PathVariable Long taskId) {
        try {
            // 先调用生成图片接口
            AjaxResult imageResult = generateImage(taskId);
            if (imageResult.get("code") != null && !imageResult.get("code").equals(200)) {
                return imageResult;
            }

            // 再调用生成文案接口
            AjaxResult copywritingResult = generateCopywriting(taskId);
            if (copywritingResult.get("code") != null && !copywritingResult.get("code").equals(200)) {
                return copywritingResult;
            }

            return AjaxResult.success("一键生成成功");
        } catch (Exception e) {
            // 更新任务状态为失败
            taskService.updateTaskStatus(taskId, 3, e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 获取任务列表
     * @return 任务列表
     */
    @GetMapping("/taskList")
    public AjaxResult getTaskList(@RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer pageSize,
                                 @RequestParam(required = false) String taskName,
                                 @RequestParam(required = false) Integer status) {
        try {
            Long userId = SecurityUtils.getUserId();
            List<AiGenerateTask> taskList = taskService.queryTaskListByUserId(userId);
            
            // 构建分页响应对象
            Map<String, Object> result = new HashMap<>();
            result.put("list", taskList);
            result.put("total", taskList.size());
            
            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 获取任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    @GetMapping("/taskDetail/{taskId}")
    public AjaxResult getTaskDetail(@PathVariable Long taskId) {
        try {
            AiGenerateTask task = taskService.queryTaskById(taskId);
            return AjaxResult.success(task);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 获取生成结果列表
     * @param taskId 任务ID
     * @return 结果列表
     */
    @GetMapping("/resultList/{taskId}")
    public AjaxResult getResultList(@PathVariable Long taskId) {
        try {
            List<AiGenerateResult> resultList = resultService.queryResultListByTaskId(taskId);
            return AjaxResult.success(resultList);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 获取图片生成结果
     * @param taskId 任务ID
     * @return 图片结果列表
     */
    @GetMapping("/imageResult/{taskId}")
    public AjaxResult getImageResult(@PathVariable Long taskId) {
        try {
            List<AiGenerateResult> resultList = resultService.queryResultListByTaskIdAndType(taskId, 1);
            return AjaxResult.success(resultList);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 获取文案生成结果
     * @param taskId 任务ID
     * @return 文案结果列表
     */
    @GetMapping("/copywritingResult/{taskId}")
    public AjaxResult getCopywritingResult(@PathVariable Long taskId) {
        try {
            List<AiGenerateResult> resultList = resultService.queryResultListByTaskIdAndType(taskId, 2);
            return AjaxResult.success(resultList);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 删除任务
     * @param taskId 任务ID
     * @return 删除结果
     */
    @DeleteMapping("/deleteTask/{taskId}")
    public AjaxResult deleteTask(@PathVariable Long taskId) {
        try {
            taskService.removeById(taskId);
            return AjaxResult.success("删除成功");
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}