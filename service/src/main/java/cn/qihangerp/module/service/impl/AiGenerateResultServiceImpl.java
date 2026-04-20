package cn.qihangerp.module.service.impl;

import cn.qihangerp.mapper.AiGenerateResultMapper;
import cn.qihangerp.model.entity.AiGenerateResult;
import cn.qihangerp.module.service.AiGenerateResultService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * AI内容生成结果服务实现类
 */
@Service
public class AiGenerateResultServiceImpl extends ServiceImpl<AiGenerateResultMapper, AiGenerateResult>
        implements AiGenerateResultService {

    @Resource
    private AiGenerateResultMapper resultMapper;

    @Override
    public Long saveResult(AiGenerateResult result) {
        save(result);
        return result.getId();
    }

    @Override
    public List<AiGenerateResult> queryResultListByTaskId(Long taskId) {
        LambdaQueryWrapper<AiGenerateResult> queryWrapper = new LambdaQueryWrapper<AiGenerateResult>()
                .eq(AiGenerateResult::getTaskId, taskId);
        return list(queryWrapper);
    }

    @Override
    public List<AiGenerateResult> queryResultListByTaskIdAndType(Long taskId, Integer resultType) {
        LambdaQueryWrapper<AiGenerateResult> queryWrapper = new LambdaQueryWrapper<AiGenerateResult>()
                .eq(AiGenerateResult::getTaskId, taskId)
                .eq(AiGenerateResult::getResultType, resultType);
        return list(queryWrapper);
    }
}