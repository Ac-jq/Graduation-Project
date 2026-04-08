package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin AI task entity.
 */
@Data
@TableName("admin_ai_task")
public class AdminAiTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminUserId;

    private String instructionText;

    private String taskType;

    private String parseStatus;

    private String confirmStatus;

    private String executeStatus;

    private String summaryText;

    private String failureReason;

    private LocalDateTime confirmedAt;

    private LocalDateTime executedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
