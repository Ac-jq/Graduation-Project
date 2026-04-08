package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Resource and tag relation entity.
 */
@Data
@TableName("resource_tag_relation")
public class ResourceTagRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resourceId;

    private Long tagId;
}
