package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.entity.MentalScale;

/**
 * AI 解读服务。
 */
public interface AiInterpretationService {

    String generateInterpretation(MentalScale scale, Integer totalScore, String levelCode, String detailedAnswerContext);
}
