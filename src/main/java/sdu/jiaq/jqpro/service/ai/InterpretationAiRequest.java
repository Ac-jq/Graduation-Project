package sdu.jiaq.jqpro.service.ai;

/**
 * 测评报告 AI 解读请求。
 *
 * @param scaleName         量表名称
 * @param scaleDescription  量表简介
 * @param scaleIntroduction 量表说明
 * @param totalScore        总分
 * @param riskLevel         风险等级
 * @param thresholdSummary  阈值信息
 */
public record InterpretationAiRequest(String scaleName,
                                      String scaleDescription,
                                      String scaleIntroduction,
                                      Integer totalScore,
                                      String riskLevel,
                                      String thresholdSummary) {
}
