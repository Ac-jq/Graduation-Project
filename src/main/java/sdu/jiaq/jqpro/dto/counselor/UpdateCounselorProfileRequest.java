package sdu.jiaq.jqpro.dto.counselor;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 咨询师个人资料更新请求。
 */
@Data
public class UpdateCounselorProfileRequest {

    @Size(max = 500, message = "头像地址长度不能超过500")
    @JsonAlias("avatar")
    private String avatarUrl;
}
