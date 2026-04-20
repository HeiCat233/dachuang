package cn.qihangerp.module.service.impl;

import cn.qihangerp.mapper.AiGenerateTaskMapper;
import cn.qihangerp.model.entity.AiGenerateTask;
import cn.qihangerp.module.service.AiGenerateTaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * AI内容生成任务服务实现类
 */
@Service
public class AiGenerateTaskServiceImpl extends ServiceImpl<AiGenerateTaskMapper, AiGenerateTask>
        implements AiGenerateTaskService {

    @Resource
    private AiGenerateTaskMapper taskMapper;

    @Override
    public Long createTask(AiGenerateTask task) {
        save(task);
        return task.getId();
    }

    @Override
    public boolean updateTaskStatus(Long taskId, Integer status, String errorMessage) {
        AiGenerateTask task = new AiGenerateTask();
        task.setId(taskId);
        task.setStatus(status);
        task.setErrorMessage(errorMessage);
        return updateById(task);
    }

    @Override
    public List<AiGenerateTask> queryTaskListByUserId(Long userId) {
        LambdaQueryWrapper<AiGenerateTask> queryWrapper = new LambdaQueryWrapper<AiGenerateTask>()
                .eq(AiGenerateTask::getCreateBy, userId.toString())
                .orderByDesc(AiGenerateTask::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public AiGenerateTask queryTaskById(Long taskId) {
        return getById(taskId);
    }
}