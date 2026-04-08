package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

/**
 * Simple named metric.
 */
@Data
@Builder
public class NamedMetricResponse {

    private String name;

    private long count;
}
