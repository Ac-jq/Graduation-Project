package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 量表答案实体。
 */
@Data
@TableName("mental_scale_answer")
public class MentalScaleAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long questionId;

    private Long optionId;

    private Integer score;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
