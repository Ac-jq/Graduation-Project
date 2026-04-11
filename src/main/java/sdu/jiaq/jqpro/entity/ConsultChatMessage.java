package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私密聊天室消息实体。
 */
@Data
@TableName("consult_chat_message")
public class ConsultChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chatSessionId;

    private Long senderUserId;

    private String senderType;

    private String contentCipherText;

    private LocalDateTime createdAt;
}
