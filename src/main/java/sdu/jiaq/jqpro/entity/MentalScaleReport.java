package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 量表报告实体。
 */
@Data
@TableName("mental_scale_report")
public class MentalScaleReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long scaleId;

    private Long userId;

    private String levelCode;

    private Integer totalScore;

    private String summaryText;

    private String aiInterpretation;

    private String recommendedResourceIds;

    private LocalDateTime createdAt;
}
