package sdu.jiaq.jqpro.service.ai;

/**
 * AI client used to convert administrator instructions into structured plans.
 */
public interface AdminOpsAiClient {

    boolean isEnabled();

    AdminOpsAiPlan parseInstruction(String instruction);
}
