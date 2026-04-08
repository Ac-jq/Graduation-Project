package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心理量表实体。
 */
@Data
@TableName("mental_scale")
public class MentalScale {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String description;

    private String introduction;

    private Integer totalQuestions;

    private Integer pageSize;

    private Integer lowThreshold;

    private Integer mediumThreshold;

    private Integer highThreshold;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
