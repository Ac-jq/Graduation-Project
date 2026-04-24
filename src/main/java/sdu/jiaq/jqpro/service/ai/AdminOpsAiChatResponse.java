package sdu.jiaq.jqpro.service.ai;

import java.util.List;

/**
 * DeepSeek 工具调用对话响应。
 *
 * @param content   助手直接回复的文本内容
 * @param toolCalls 模型返回的工具调用列表
 */
public record AdminOpsAiChatResponse(String content, List<AdminOpsAiToolCall> toolCalls) {
}
