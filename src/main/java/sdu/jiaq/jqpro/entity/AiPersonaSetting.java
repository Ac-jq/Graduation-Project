package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Student-scoped AI mentor persona setting.
 */
@Data
@TableName("ai_persona_setting")
public class AiPersonaSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentUserId;

    private String mentorName;

    private String avatarText;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
