package sdu.jiaq.jqpro.service.ai;

import java.util.List;

/**
 * 测评报告 AI 客户端。
 */
public interface InterpretationAiClient {

    /**
     * 生成测评解读。
     */
    String generateInterpretation(InterpretationAiRequest request);

    /**
     * 从资源列表中挑选最适合的资源 ID。
     */
    List<Long> selectRecommendedResourceIds(ResourceRecommendationAiRequest request);
}
