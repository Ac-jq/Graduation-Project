package sdu.jiaq.jqpro.service.impl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import sdu.jiaq.jqpro.common.constant.ReportLevelConstants;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.service.AiInterpretationService;

/**
 * AI 解读服务实现。
 * 当外部大模型未配置或调用失败时，自动回退到内置模板文案，保证测评闭环可完成。
 */
@Service
public class AiInterpretationServiceImpl implements AiInterpretationService {

    private final ObjectProvider<ChatModel> chatModelProvider;

    public AiInterpretationServiceImpl(ObjectProvider<ChatModel> chatModelProvider) {
        this.chatModelProvider = chatModelProvider;
    }

    @Override
    public String generateInterpretation(MentalScale scale, Integer totalScore, String levelCode) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return buildFallbackInterpretation(scale.getName(), totalScore, levelCode);
        }

        try {
            String prompt = """
                    你是一名高校心理健康平台的AI解读助手。
                    请根据量表结果输出一段温和、客观、非诊断性的中文解读。
                    要求：
                    1. 不使用“确诊”“病症”等诊断词汇。
                    2. 先说明当前状态，再给出2条可执行建议。
                    3. 语气温和克制，不制造恐慌。
                    量表名称：%s
                    总分：%s
                    风险等级：%s
                    """.formatted(scale.getName(), totalScore, levelCode);

            String content = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                return buildFallbackInterpretation(scale.getName(), totalScore, levelCode);
            }
            return content;
        } catch (Exception exception) {
            return buildFallbackInterpretation(scale.getName(), totalScore, levelCode);
        }
    }

    private String buildFallbackInterpretation(String scaleName, Integer totalScore, String levelCode) {
        if (ReportLevelConstants.HIGH.equals(levelCode)) {
            return "%s结果显示你最近承受的压力偏高，总分为%s。建议尽快联系学校心理老师或可信赖的支持者，先把睡眠、饮食和作息稳定下来，并尽量不要独自承受持续性的情绪负担。".formatted(scaleName, totalScore);
        }
        if (ReportLevelConstants.MEDIUM.equals(levelCode)) {
            return "%s结果显示你近期存在一定心理压力，总分为%s。建议先关注过去一周最困扰你的两个场景，尝试记录触发因素，并安排规律休息、运动或向辅导员和咨询师寻求进一步支持。".formatted(scaleName, totalScore);
        }
        return "%s结果显示你当前整体状态相对平稳，总分为%s。建议继续保持规律作息，留意情绪波动时的触发因素，并通过运动、社交或放松练习巩固当前状态。".formatted(scaleName, totalScore);
    }
}
