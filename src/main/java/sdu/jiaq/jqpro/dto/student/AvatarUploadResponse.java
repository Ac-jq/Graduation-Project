package sdu.jiaq.jqpro.dto.student;

import lombok.Builder;
import lombok.Data;

/**
 * 头像上传结果。
 */
@Data
@Builder
public class AvatarUploadResponse {

    private String avatarUrl;
}
