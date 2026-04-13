package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sdu.jiaq.jqpro.common.constant.AssessmentNoticeConstants;
import sdu.jiaq.jqpro.common.constant.ReportLevelConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalScaleRule;
import sdu.jiaq.jqpro.mapper.MentalScaleRuleMapper;
import sdu.jiaq.jqpro.service.AiInterpretationService;
import sdu.jiaq.jqpro.service.ai.InterpretationAiRequest;
import sdu.jiaq.jqpro.service.ai.InterpretationAiClient;

/**
 * AI 解读服务。
 * 当 DeepSeek 未配置或调用失败时，会自动回退到安全的本地解释模板，保证报告生成闭环不中断。
 */
@Service
public class AiInterpretationServiceImpl implements AiInterpretationService {
    private static final Logger log = LoggerFactory.getLogger(AiInterpretationServiceImpl.class);
    private static final List<String> FORBIDDEN_TERMS = List.of(
            "确诊", "患病", "精神疾病", "抑郁症", "焦虑症", "双相", "自杀", "自残", "病人", "治疗方案"
    );

    private final InterpretationAiClient interpretationAiClient;
    private final MentalScaleRuleMapper mentalScaleRuleMapper;

    public AiInterpretationServiceImpl(
            InterpretationAiClient interpretationAiClient,
            MentalScaleRuleMapper mentalScaleRuleMapper
    ) {
        this.interpretationAiClient = interpretationAiClient;
        this.mentalScaleRuleMapper = mentalScaleRuleMapper;
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

        String interpretation;
        try {
            interpretation = interpretationAiClient.generateInterpretation(request);
        } catch (BusinessException exception) {
            log.warn("AI interpretation fallback for scale {}: {}", scale.getCode(), exception.getMessage());
            interpretation = buildFallbackInterpretation(scale.getName(), levelCode);
        } catch (Exception exception) {
            log.warn("AI interpretation unexpected fallback for scale {}", scale.getCode(), exception);
            interpretation = buildFallbackInterpretation(scale.getName(), levelCode);
        }
        return sanitizeInterpretation(scale.getName(), levelCode, interpretation);
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

    private String buildFallbackInterpretation(String scaleName, String levelCode) {
        if (ReportLevelConstants.HIGH.equals(levelCode)) {
            return "%s结果提示你近期正在承受较明显的情绪压力或身心负担，这更像是一段需要被认真看见和支持的状态信号，而不是医学诊断。建议尽快联系学校心理老师、辅导员或可信赖的支持资源，尽量不要独自承受，同时适当减少持续高压安排。"
                    .formatted(scaleName);
        }
        if (ReportLevelConstants.MEDIUM.equals(levelCode)) {
            return "%s结果提示你近期存在一定程度的情绪波动或压力累积，这并不等同于医学诊断，但值得持续关注。建议先规律作息、适度运动、减少持续高压任务；如果困扰持续存在，可以主动联系学校心理支持资源进一步沟通。"
                    .formatted(scaleName);
        }
        return "%s结果整体相对平稳，说明你当前没有出现明显的高强度困扰信号。建议继续关注睡眠、压力和日常情绪变化，保持稳定的作息与自我照顾节奏，如后续状态波动增大，也可以及时寻求校园支持。"
                .formatted(scaleName);
    }

    private String sanitizeInterpretation(String scaleName, String levelCode, String interpretation) {
        String candidate = StringUtils.hasText(interpretation) ? interpretation.trim() : "";
        if (!StringUtils.hasText(candidate)) {
            return buildFallbackInterpretation(scaleName, levelCode);
        }

        String normalized = candidate
                .replace("###", "")
                .replace("**", "")
                .replace("\r", "")
                .replace("1.", "")
                .replace("2.", "")
                .replace("3.", "")
                .trim();

        for (String forbiddenTerm : FORBIDDEN_TERMS) {
          if (normalized.contains(forbiddenTerm)) {
                log.warn("AI interpretation hit forbidden term '{}', fallback applied", forbiddenTerm);
                return buildFallbackInterpretation(scaleName, levelCode);
            }
        }

        if (!normalized.contains("建议")) {
            normalized = normalized + " 建议结合近期学习、睡眠和压力变化继续观察，并在需要时主动联系学校心理支持资源。";
        }

        if (normalized.length() > 240) {
            normalized = normalized.substring(0, 240).trim();
        }

        return normalized + " " + AssessmentNoticeConstants.AI_FALLBACK_NOTICE;
    }

    private String pickText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未提供";
    }
}
