package sdu.jiaq.jqpro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 量表作答会话实体。
 */
@Data
@TableName("mental_scale_session")
public class MentalScaleSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scaleId;

    private Long userId;

    private String status;

    private Integer answeredCount;

    private Integer totalScore;

    private LocalDateTime submittedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
