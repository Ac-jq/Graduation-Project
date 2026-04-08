package sdu.jiaq.jqpro.dto.student;

import lombok.Builder;
import lombok.Data;

/**
 * 学生档案响应。
 */
@Data
@Builder
public class StudentProfileResponse {

    private Long userId;

    private String account;

    private String realName;

    private String displayName;

    private String studentNo;

    private String avatarUrl;

    private String college;

    private String grade;

    private String gender;

    private String phone;

    private String emergencyContact;

    private String emergencyPhone;

    private Long counselorUserId;
}
