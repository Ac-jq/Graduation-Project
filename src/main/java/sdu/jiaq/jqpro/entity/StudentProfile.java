package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生档案实体。
 */
@Data
@TableName("student_profile")
public class StudentProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String avatarUrl;

    private String college;

    private String grade;

    private String gender;

    private String phone;

    private String emergencyContact;

    private String emergencyPhone;

    private Long counselorUserId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
