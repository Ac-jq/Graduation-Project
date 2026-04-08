package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约时段实体。
 */
@Data
@TableName("consult_appointment_slot")
public class ConsultAppointmentSlot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long counselorUserId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
