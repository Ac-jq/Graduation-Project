package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.aichat.AiPersonaSettingResponse;
import sdu.jiaq.jqpro.dto.aichat.UpdateAiPersonaSettingRequest;

/**
 * Service for student-scoped AI mentor persona settings.
 */
public interface AiPersonaSettingService {

    AiPersonaSettingResponse getCurrentStudentPersona();

    AiPersonaSettingResponse updateCurrentStudentPersona(UpdateAiPersonaSettingRequest request);
}
