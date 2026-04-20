package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sdu.jiaq.jqpro.common.constant.ReportLevelConstants;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalScaleRule;
import sdu.jiaq.jqpro.mapper.MentalScaleRuleMapper;
import sdu.jiaq.jqpro.service.AiInterpretationService;
import sdu.jiaq.jqpro.service.ai.InterpretationAiClient;
import sdu.jiaq.jqpro.service.ai.InterpretationAiRequest;

import java.util.List;

/**
 * AI 解读服务。
 */
@Service
public class AiInterpretationServiceImpl implements AiInterpretationService {

    private final InterpretationAiClient interpretationAiClient;
    private final MentalScaleRuleMapper mentalScaleRuleMapper;

    public AiInterpretationServiceImpl(InterpretationAiClient interpretationAiClient,
                                       MentalScaleRuleMapper mentalScaleRuleMapper) {
        this.interpretationAiClient = interpretationAiClient;
        this.mentalScaleRuleMapper = mentalScaleRuleMapper;
    }

    @Override
    public String generateInterpretation(MentalScale scale, Integer totalScore, String levelCode, String detailedAnswerContext) {
        InterpretationAiRequest request = new InterpretationAiRequest(
                scale.getName(),
                pickText(scale.getDescription()),
                pickText(scale.getIntroduction()),
                totalScore,
                resolveRiskLevel(levelCode),
                buildThresholdSummary(scale),
                pickText(detailedAnswerContext)
        );
        String interpretation = interpretationAiClient.generateInterpretation(request);
        return StringUtils.hasText(interpretation) ? interpretation.trim() : "AI 未返回有效解读内容";
    }

    private String resolveRiskLevel(String levelCode) {
        return switch (levelCode) {
            case ReportLevelConstants.HIGH -> "高关注";
            case ReportLevelConstants.MEDIUM -> "中等关注";
            case ReportLevelConstants.LOW -> "低关注";
            default -> "未分级";
        };
    }

    private String buildThresholdSummary(MentalScale scale) {
        List<MentalScaleRule> rules = mentalScaleRuleMapper.selectList(
                new LambdaQueryWrapper<MentalScaleRule>()
                        .eq(MentalScaleRule::getScaleId, scale.getId())
                        .orderByAsc(MentalScaleRule::getSortNo)
                        .orderByAsc(MentalScaleRule::getMinScore)
        );
        if (!rules.isEmpty()) {
            return rules.stream()
                    .map(rule -> "%s=%d-%d".formatted(rule.getLevelCode(), rule.getMinScore(), rule.getMaxScore()))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("未配置");
        }
        String lowThreshold = scale.getLowThreshold() == null ? "未配置" : scale.getLowThreshold().toString();
        String mediumThreshold = scale.getMediumThreshold() == null ? "未配置" : scale.getMediumThreshold().toString();
        String highThreshold = scale.getHighThreshold() == null ? "未配置" : scale.getHighThreshold().toString();
        return "lowThreshold=%s, mediumThreshold=%s, highThreshold=%s".formatted(lowThreshold, mediumThreshold, highThreshold);
    }

    private String pickText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未提供";
    }
}
