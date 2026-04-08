package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Student favorite entity.
 */
@Data
@TableName("resource_favorite")
public class ResourceFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resourceId;

    private Long studentUserId;

    private LocalDateTime createdAt;
}
