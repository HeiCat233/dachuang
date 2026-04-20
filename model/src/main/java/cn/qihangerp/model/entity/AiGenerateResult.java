package cn.qihangerp.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * AI内容生成结果表
 * @TableName ai_generate_result
 */
@TableName(value = "ai_generate_result")
@Data
public class AiGenerateResult implements Serializable {
    /**
     * 结果ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 结果类型：1-图片，2-文案
     */
    private Integer resultType;

    /**
     * 结果内容（图片URL或文案文本）
     */
    private String resultContent;

    /**
     * 生成时间
     */
    private Date generateTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createBy;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}