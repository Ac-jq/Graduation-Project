package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 量表选项实体。
 */
@Data
@TableName("mental_scale_option")
public class MentalScaleOption {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private String optionCode;

    private String content;

    private Integer score;

    private Integer sortNo;

    private LocalDateTime createdAt;
}
