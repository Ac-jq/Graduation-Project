package sdu.jiaq.jqpro.dto.adminuser;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Create counselor request.
 */
@Data
public class CreateCounselorRequest {

    @NotBlank(message = "account is required")
    @Size(max = 64, message = "account is too long")
    private String account;

    @NotBlank(message = "displayName is required")
    @Size(max = 64, message = "displayName is too long")
    private String displayName;

    @Size(max = 64, message = "realName is too long")
    private String realName;

    @NotBlank(message = "counselorNo is required")
    @Size(max = 32, message = "counselorNo is too long")
    private String counselorNo;
}
