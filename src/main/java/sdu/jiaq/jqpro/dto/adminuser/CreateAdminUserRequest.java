package sdu.jiaq.jqpro.dto.adminuser;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员新增多角色用户请求。
 */
@Data
public class CreateAdminUserRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 64, message = "账号过长")
    private String account;

    @NotBlank(message = "用户角色不能为空")
    @Size(max = 32, message = "用户角色过长")
    private String roleCode;

    @NotBlank(message = "显示名不能为空")
    @Size(max = 64, message = "显示名过长")
    private String displayName;

    @Size(max = 64, message = "真实姓名过长")
    private String realName;

    @Size(max = 32, message = "学号过长")
    private String studentNo;

    @Size(max = 32, message = "工号过长")
    private String counselorNo;

    @Size(max = 128, message = "学院名称过长")
    private String college;

    @Size(max = 32, message = "年级过长")
    private String grade;

    @Size(max = 32, message = "手机号过长")
    private String phone;

    @Size(max = 128, message = "密码过长")
    private String password;
}
