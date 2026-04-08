package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin AI task item entity.
 */
@Data
@TableName("admin_ai_task_item")
public class AdminAiTaskItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String targetType;

    private Long targetId;

    private String targetLabel;

    private String operationType;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private Integer sortNo;

    private String executeStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
