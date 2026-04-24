package sdu.jiaq.jqpro.service.ai;

/**
 * 大模型返回的单个工具调用描述。
 *
 * @param id            工具调用唯一标识
 * @param name          工具名称
 * @param argumentsJson 工具参数 JSON
 */
public record AdminOpsAiToolCall(String id, String name, String argumentsJson) {
}
