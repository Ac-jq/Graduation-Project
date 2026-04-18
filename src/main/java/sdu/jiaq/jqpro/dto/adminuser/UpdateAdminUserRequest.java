package sdu.jiaq.jqpro.dto.adminuser;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Admin-side update user request.
 */
@Data
public class UpdateAdminUserRequest {

    @NotBlank(message = "account is required")
    @Size(max = 64, message = "account is too long")
    private String account;

    @NotBlank(message = "displayName is required")
    @Size(max = 64, message = "displayName is too long")
    private String displayName;

    @Size(max = 64, message = "realName is too long")
    private String realName;

    @Size(max = 32, message = "studentNo is too long")
    private String studentNo;

    @Size(max = 32, message = "counselorNo is too long")
    private String counselorNo;

    @Size(max = 128, message = "college is too long")
    private String college;

    @Size(max = 32, message = "grade is too long")
    private String grade;

    @Size(max = 32, message = "phone is too long")
    private String phone;

    @Size(max = 64, message = "password is too long")
    private String password;
}
