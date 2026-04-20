package cn.qihangerp.module.service;

import cn.qihangerp.model.entity.AiGenerateTask;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * AI内容生成任务服务接口
 */
public interface AiGenerateTaskService extends IService<AiGenerateTask> {
    /**
     * 创建生成任务
     * @param task 任务信息
     * @return 任务ID
     */
    Long createTask(AiGenerateTask task);

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 状态
     * @param errorMessage 错误信息
     * @return 更新结果
     */
    boolean updateTaskStatus(Long taskId, Integer status, String errorMessage);

    /**
     * 根据用户ID查询任务列表
     * @param userId 用户ID
     * @return 任务列表
     */
    List<AiGenerateTask> queryTaskListByUserId(Long userId);

    /**
     * 根据任务ID查询任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    AiGenerateTask queryTaskById(Long taskId);
}