package sdu.jiaq.jqpro.service.ai;

/**
 * AI client used by student AI mentor sessions.
 */
public interface AiChatAiClient {

    String generateReply(AiChatAiRequest request);
}
