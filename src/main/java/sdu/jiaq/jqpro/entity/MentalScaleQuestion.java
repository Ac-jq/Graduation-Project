package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 量表题目实体。
 */
@Data
@TableName("mental_scale_question")
public class MentalScaleQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scaleId;

    private Integer questionNo;

    private String content;

    private Integer requiredFlag;

    private LocalDateTime createdAt;
}
