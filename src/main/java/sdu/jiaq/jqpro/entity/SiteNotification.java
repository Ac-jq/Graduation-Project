package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体。
 */
@Data
@TableName("site_notification")
public class SiteNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receiverUserId;

    private String title;

    private String contentText;

    private Integer readFlag;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
