package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Resource view log entity.
 */
@Data
@TableName("resource_view_log")
public class ResourceViewLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resourceId;

    private Long studentUserId;

    private LocalDateTime createdAt;
}
