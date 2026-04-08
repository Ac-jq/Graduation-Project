package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私密聊天室会话实体。
 */
@Data
@TableName("consult_chat_session")
public class ConsultChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appointmentId;

    private Long studentUserId;

    private Long counselorUserId;

    private LocalDateTime openTime;

    private LocalDateTime closeTime;

    private String status;

    private Integer sealedFlag;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
