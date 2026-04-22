package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.aichat.AiPersonaSettingResponse;
import sdu.jiaq.jqpro.dto.aichat.UpdateAiPersonaSettingRequest;
import sdu.jiaq.jqpro.entity.AiPersonaSetting;
import sdu.jiaq.jqpro.mapper.AiPersonaSettingMapper;
import sdu.jiaq.jqpro.service.AiPersonaSettingService;

/**
 * Stores AI mentor persona settings by current student user id.
 */
@Service
public class AiPersonaSettingServiceImpl implements AiPersonaSettingService {

    private static final String DEFAULT_MENTOR_NAME = "青禾导师";
    private static final String DEFAULT_AVATAR_TEXT = "青";

    private final AiPersonaSettingMapper aiPersonaSettingMapper;

    public AiPersonaSettingServiceImpl(AiPersonaSettingMapper aiPersonaSettingMapper) {
        this.aiPersonaSettingMapper = aiPersonaSettingMapper;
    }

    @Override
    public AiPersonaSettingResponse getCurrentStudentPersona() {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        AiPersonaSetting setting = selectByStudentUserId(studentUserId);
        if (setting == null) {
            return defaultResponse(studentUserId);
        }
        return buildResponse(setting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiPersonaSettingResponse updateCurrentStudentPersona(UpdateAiPersonaSettingRequest request) {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        String mentorName = normalizeRequiredText(request.getMentorName(), "AI导师昵称不能为空");
        String avatarText = normalizeRequiredText(request.getAvatarText(), "AI导师头像不能为空");

        AiPersonaSetting setting = selectByStudentUserId(studentUserId);
        if (setting == null) {
            setting = new AiPersonaSetting();
            setting.setStudentUserId(studentUserId);
            setting.setMentorName(mentorName);
            setting.setAvatarText(avatarText);
            aiPersonaSettingMapper.insert(setting);
            return buildResponse(setting);
        }

        setting.setMentorName(mentorName);
        setting.setAvatarText(avatarText);
        aiPersonaSettingMapper.updateById(setting);
        return buildResponse(setting);
    }

    private AiPersonaSetting selectByStudentUserId(Long studentUserId) {
        return aiPersonaSettingMapper.selectOne(new LambdaQueryWrapper<AiPersonaSetting>()
                .eq(AiPersonaSetting::getStudentUserId, studentUserId)
                .last("limit 1"));
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private AiPersonaSettingResponse defaultResponse(Long studentUserId) {
        return AiPersonaSettingResponse.builder()
                .studentUserId(studentUserId)
                .mentorName(DEFAULT_MENTOR_NAME)
                .avatarText(DEFAULT_AVATAR_TEXT)
                .build();
    }

    private AiPersonaSettingResponse buildResponse(AiPersonaSetting setting) {
        return AiPersonaSettingResponse.builder()
                .studentUserId(setting.getStudentUserId())
                .mentorName(setting.getMentorName())
                .avatarText(setting.getAvatarText())
                .build();
    }
}
