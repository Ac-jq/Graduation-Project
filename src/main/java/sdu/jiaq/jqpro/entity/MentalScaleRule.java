package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 量表评分规则。
 * 第一版只支持基于总分区间的规则，不引入维度拆分。
 */
@Data
@TableName("mental_scale_rule")
public class MentalScaleRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scaleId;

    private String levelCode;

    private Integer minScore;

    private Integer maxScore;

    private String summaryText;

    private Integer sortNo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
