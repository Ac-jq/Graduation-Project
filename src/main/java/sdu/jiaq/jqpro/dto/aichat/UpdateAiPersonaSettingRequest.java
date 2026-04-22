package sdu.jiaq.jqpro.dto.aichat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request for updating a student's AI mentor persona.
 */
@Data
public class UpdateAiPersonaSettingRequest {

    @NotBlank(message = "AI导师昵称不能为空")
    @Size(max = 64, message = "AI导师昵称长度不能超过64")
    private String mentorName;

    @NotBlank(message = "AI导师头像不能为空")
    @Size(max = 32, message = "AI导师头像长度不能超过32")
    private String avatarText;
}
