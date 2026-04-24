package cn.qihangerp.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * AI内容生成任务表
 * @TableName ai_generate_task
 */
@TableName(value = "ai_generate_task")
@Data
public class AiGenerateTask implements Serializable {
    /**
     * 任务ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 文字描述
     */
    private String description;

    /**
     * 参考图片URL
     */
    private String referenceImage;

    /**
     * 生图参数（JSON格式）
     */
    private String imageParams;

    /**
     * 任务状态：0-待处理，1-处理中，2-成功，3-失败
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}