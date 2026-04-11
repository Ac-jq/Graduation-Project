package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 咨询预约实体。
 */
@Data
@TableName("consult_appointment")
public class ConsultAppointment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long slotId;

    private Long studentUserId;

    private Long counselorUserId;

    private String anonymousName;

    private String issueSummary;

    private String status;

    private String resultMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
