package sdu.jiaq.jqpro.service;

import java.util.List;
import sdu.jiaq.jqpro.dto.resource.ResourceSummaryResponse;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalScaleReport;

/**
 * 测评报告推荐资源快照服务。
 */
public interface ReportRecommendationService {

    /**
     * 在报告生成阶段生成推荐资源 ID 快照，后续详情查询只读取该快照。
     */
    String buildRecommendedResourceIdSnapshot(MentalScale scale, MentalScaleReport report, String detailedAnswerContext);

    /**
     * 根据报告中已持久化的资源 ID 快照恢复推荐资源列表。
     */
    List<ResourceSummaryResponse> listSnapshotResources(String recommendedResourceIds);
}
