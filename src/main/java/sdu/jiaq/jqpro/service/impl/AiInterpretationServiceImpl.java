package sdu.jiaq.jqpro.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sdu.jiaq.jqpro.common.constant.ReportLevelConstants;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.service.AiInterpretationService;
import sdu.jiaq.jqpro.service.ai.InterpretationAiClient;
import sdu.jiaq.jqpro.service.ai.InterpretationAiRequest;

/**
 * 测评报告 AI 解读服务。
 * 只负责组装业务字段并调用外部 AI 客户端，不再返回本地模板兜底文案。
 */
@Service
public class AiInterpretationServiceImpl implements AiInterpretationService {

    private final InterpretationAiClient interpretationAiClient;

    public AiInterpretationServiceImpl(InterpretationAiClient interpretationAiClient) {
        this.interpretationAiClient = interpretationAiClient;
    }

    @Override
    public String generateInterpretation(MentalScale scale, Integer totalScore, String levelCode) {
        InterpretationAiRequest request = new InterpretationAiRequest(
                scale.getName(),
                pickText(scale.getDescription()),
                pickText(scale.getIntroduction()),
                totalScore,
                resolveRiskLevel(levelCode),
                buildThresholdSummary(scale)
        );
        return interpretationAiClient.generateInterpretation(request);
    }

    private String resolveRiskLevel(String levelCode) {
        if (ReportLevelConstants.HIGH.equals(levelCode)) {
            return "高关注";
        }
        if (ReportLevelConstants.MEDIUM.equals(levelCode)) {
            return "中等关注";
        }
        if (ReportLevelConstants.LOW.equals(levelCode)) {
            return "低关注";
        }
        return "未分级";
    }

    private String buildThresholdSummary(MentalScale scale) {
        String lowThreshold = scale.getLowThreshold() == null ? "未配置" : scale.getLowThreshold().toString();
        String mediumThreshold = scale.getMediumThreshold() == null ? "未配置" : scale.getMediumThreshold().toString();
        String highThreshold = scale.getHighThreshold() == null ? "未配置" : scale.getHighThreshold().toString();
        return "lowThreshold=%s, mediumThreshold=%s, highThreshold=%s".formatted(lowThreshold, mediumThreshold, highThreshold);
    }

    private String pickText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未提供";
    }
}
