package sdu.jiaq.jqpro.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 学生注册请求。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 64, message = "账号长度不能超过64")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需为6到64位")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 64, message = "真实姓名长度不能超过64")
    private String realName;

    @NotBlank(message = "展示昵称不能为空")
    @Size(max = 64, message = "展示昵称长度不能超过64")
    private String displayName;

    @NotBlank(message = "学号不能为空")
    @Size(max = 32, message = "学号长度不能超过32")
    private String studentNo;

    @NotBlank(message = "性别不能为空")
    @Size(max = 16, message = "性别长度不能超过16")
    private String gender;

    @NotBlank(message = "年级不能为空")
    @Size(max = 32, message = "年级长度不能超过32")
    private String grade;

    @NotBlank(message = "学院不能为空")
    @Size(max = 128, message = "学院长度不能超过128")
    private String college;
}
