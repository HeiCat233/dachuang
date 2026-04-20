package cn.qihangerp.module.service;

import cn.qihangerp.model.entity.AiGenerateResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * AI内容生成结果服务接口
 */
public interface AiGenerateResultService extends IService<AiGenerateResult> {
    /**
     * 保存生成结果
     * @param result 结果信息
     * @return 结果ID
     */
    Long saveResult(AiGenerateResult result);

    /**
     * 根据任务ID查询结果列表
     * @param taskId 任务ID
     * @return 结果列表
     */
    List<AiGenerateResult> queryResultListByTaskId(Long taskId);

    /**
     * 根据任务ID和结果类型查询结果列表
     * @param taskId 任务ID
     * @param resultType 结果类型
     * @return 结果列表
     */
    List<AiGenerateResult> queryResultListByTaskIdAndType(Long taskId, Integer resultType);
}