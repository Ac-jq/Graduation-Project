package sdu.jiaq.jqpro.service.ai;

/**
 * AI 资源推荐请求。
 *
 * @param assessmentContext 学生测评详细上下文
 * @param resourceCatalog   可推荐资源清单
 */
public record ResourceRecommendationAiRequest(String assessmentContext,
                                              String resourceCatalog) {
}
