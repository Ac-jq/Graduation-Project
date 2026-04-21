package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    private String passwordSalt;

    private String passwordHash;

    private String roleCode;

    private String realName;

    private String displayName;

    private String studentNo;

    private String counselorNo;

    private String avatarUrl;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
