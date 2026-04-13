package sdu.jiaq.jqpro.common.constant;

/**
 * 心理测评模块统一对外说明。
 */
public final class AssessmentNoticeConstants {
    public static final String PRODUCT_POSITIONING = "基于专业量表的心理状态辅助评估";
    public static final String NON_DIAGNOSTIC_NOTICE =
            "本结果仅用于心理状态辅助评估，不作为医学诊断依据。如有持续困扰，请联系专业老师或医疗机构。";
    public static final String AI_FALLBACK_NOTICE =
            "当前报告中的 AI 解读为辅助性文字说明，旨在帮助你理解量表分数，不替代专业人员评估。";

    private AssessmentNoticeConstants() {
    }
}
