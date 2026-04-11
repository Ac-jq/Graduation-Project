package sdu.jiaq.jqpro.service.ai;

/**
 * 测评报告 AI 解读客户端。
 */
public interface InterpretationAiClient {

    /**
     * 调用外部 AI 服务生成测评解读。
     *
     * @param request 解读请求
     * @return AI 返回的最终解读正文
     */
    String generateInterpretation(InterpretationAiRequest request);
}
