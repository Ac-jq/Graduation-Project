package sdu.jiaq.jqpro.dto.adminscale;

import lombok.Builder;
import lombok.Data;

/**
 * Admin scale option response.
 */
@Data
@Builder
public class AdminScaleOptionResponse {

    private Long optionId;

    private String optionCode;

    private String content;

    private Integer score;

    private Integer sortNo;
}
