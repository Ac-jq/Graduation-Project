package sdu.jiaq.jqpro.dto.student;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 学生档案更新请求。
 */
@Data
public class UpdateStudentProfileRequest {

    @Size(max = 500, message = "头像地址长度不能超过500")
    private String avatarUrl;

    @Size(max = 128, message = "学院长度不能超过128")
    private String college;

    @Size(max = 32, message = "年级长度不能超过32")
    private String grade;

    @Size(max = 16, message = "性别长度不能超过16")
    private String gender;

    @Size(max = 32, message = "联系电话长度不能超过32")
    private String phone;

    @Size(max = 64, message = "紧急联系人长度不能超过64")
    private String emergencyContact;

    @Size(max = 32, message = "紧急联系人电话长度不能超过32")
    private String emergencyPhone;
}
