package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Resource category entity.
 */
@Data
@TableName("resource_category")
public class ResourceCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Integer sortNo;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
