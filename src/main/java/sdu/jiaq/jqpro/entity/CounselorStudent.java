package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 咨询师与学生绑定关系实体。
 */
@Data
@TableName("counselor_student")
public class CounselorStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long counselorUserId;

    private Long studentUserId;

    private LocalDateTime createdAt;
}
