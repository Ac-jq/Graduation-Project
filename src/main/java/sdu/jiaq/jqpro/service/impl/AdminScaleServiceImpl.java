package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.adminscale.AdminScaleOptionRequest;
import sdu.jiaq.jqpro.dto.adminscale.AdminScaleOptionResponse;
import sdu.jiaq.jqpro.dto.adminscale.AdminScaleQuestionRequest;
import sdu.jiaq.jqpro.dto.adminscale.AdminScaleQuestionResponse;
import sdu.jiaq.jqpro.dto.adminscale.AdminScaleResponse;
import sdu.jiaq.jqpro.dto.adminscale.UpsertAdminScaleRequest;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalScaleOption;
import sdu.jiaq.jqpro.entity.MentalScaleQuestion;
import sdu.jiaq.jqpro.entity.MentalScaleReport;
import sdu.jiaq.jqpro.entity.MentalScaleSession;
import sdu.jiaq.jqpro.mapper.MentalScaleMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleOptionMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleQuestionMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleReportMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleSessionMapper;
import sdu.jiaq.jqpro.service.AdminScaleService;
import sdu.jiaq.jqpro.service.AuditLogService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin scale service implementation.
 */
@Service
public class AdminScaleServiceImpl implements AdminScaleService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final MentalScaleMapper mentalScaleMapper;
    private final MentalScaleQuestionMapper mentalScaleQuestionMapper;
    private final MentalScaleOptionMapper mentalScaleOptionMapper;
    private final MentalScaleSessionMapper mentalScaleSessionMapper;
    private final MentalScaleReportMapper mentalScaleReportMapper;
    private final AuditLogService auditLogService;

    public AdminScaleServiceImpl(MentalScaleMapper mentalScaleMapper,
                                 MentalScaleQuestionMapper mentalScaleQuestionMapper,
                                 MentalScaleOptionMapper mentalScaleOptionMapper,
                                 MentalScaleSessionMapper mentalScaleSessionMapper,
                                 MentalScaleReportMapper mentalScaleReportMapper,
                                 AuditLogService auditLogService) {
        this.mentalScaleMapper = mentalScaleMapper;
        this.mentalScaleQuestionMapper = mentalScaleQuestionMapper;
        this.mentalScaleOptionMapper = mentalScaleOptionMapper;
        this.mentalScaleSessionMapper = mentalScaleSessionMapper;
        this.mentalScaleReportMapper = mentalScaleReportMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<AdminScaleResponse> listScales() {
        return mentalScaleMapper.selectList(new LambdaQueryWrapper<MentalScale>()
                        .orderByDesc(MentalScale::getCreatedAt, MentalScale::getId))
                .stream()
                .map(scale -> buildScaleResponse(scale, false))
                .toList();
    }

    @Override
    public AdminScaleResponse getScale(Long scaleId) {
        return buildScaleResponse(getRequiredScale(scaleId), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScaleResponse createScale(UpsertAdminScaleRequest request) {
        if (mentalScaleMapper.selectCount(new LambdaQueryWrapper<MentalScale>().eq(MentalScale::getCode, request.getCode().trim())) > 0) {
            throw new BusinessException("Scale code already exists");
        }
        validateScaleRequest(request);
        MentalScale scale = new MentalScale();
        applyScaleRequest(scale, request);
        scale.setStatus(STATUS_INACTIVE);
        mentalScaleMapper.insert(scale);
        replaceQuestions(scale.getId(), request.getQuestions());
        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_SCALE_CREATE", "Create scale",
                "Created scale " + scale.getCode(), null);
        return buildScaleResponse(scale, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScaleResponse updateScale(Long scaleId, UpsertAdminScaleRequest request) {
        MentalScale scale = getRequiredScale(scaleId);
        validateScaleRequest(request);
        MentalScale sameCode = mentalScaleMapper.selectOne(new LambdaQueryWrapper<MentalScale>()
                .eq(MentalScale::getCode, request.getCode().trim())
                .last("limit 1"));
        if (sameCode != null && !scaleId.equals(sameCode.getId())) {
            throw new BusinessException("Scale code already exists");
        }
        boolean inUse = isScaleInUse(scaleId);
        if (inUse && !hasSameStructure(scaleId, request.getQuestions())) {
            throw new BusinessException("Scale is already in use and its question structure cannot be changed");
        }
        applyScaleRequest(scale, request);
        mentalScaleMapper.updateById(scale);
        if (!inUse) {
            replaceQuestions(scaleId, request.getQuestions());
        }
        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_SCALE_UPDATE", "Update scale",
                "Updated scale " + scale.getCode(), null);
        return buildScaleResponse(scale, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScaleResponse activateScale(Long scaleId) {
        MentalScale scale = getRequiredScale(scaleId);
        scale.setStatus(STATUS_ACTIVE);
        mentalScaleMapper.updateById(scale);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_SCALE_ACTIVATE", "Activate scale",
                "Activated scale " + scale.getCode(), null);
        return buildScaleResponse(scale, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminScaleResponse deactivateScale(Long scaleId) {
        MentalScale scale = getRequiredScale(scaleId);
        scale.setStatus(STATUS_INACTIVE);
        mentalScaleMapper.updateById(scale);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_SCALE_DEACTIVATE", "Deactivate scale",
                "Deactivated scale " + scale.getCode(), null);
        return buildScaleResponse(scale, true);
    }

    private void validateScaleRequest(UpsertAdminScaleRequest request) {
        if (request.getHighThreshold() < request.getMediumThreshold() || request.getMediumThreshold() < request.getLowThreshold()) {
            throw new BusinessException("Threshold settings are invalid");
        }
    }

    private void applyScaleRequest(MentalScale scale, UpsertAdminScaleRequest request) {
        scale.setCode(request.getCode().trim());
        scale.setName(request.getName().trim());
        scale.setDescription(request.getDescription());
        scale.setIntroduction(request.getIntroduction());
        scale.setPageSize(request.getPageSize());
        scale.setLowThreshold(request.getLowThreshold());
        scale.setMediumThreshold(request.getMediumThreshold());
        scale.setHighThreshold(request.getHighThreshold());
        scale.setTotalQuestions(request.getQuestions().size());
    }

    private void replaceQuestions(Long scaleId, List<AdminScaleQuestionRequest> questionRequests) {
        List<MentalScaleQuestion> existingQuestions = mentalScaleQuestionMapper.selectList(new LambdaQueryWrapper<MentalScaleQuestion>()
                .eq(MentalScaleQuestion::getScaleId, scaleId));
        if (!existingQuestions.isEmpty()) {
            List<Long> questionIds = existingQuestions.stream().map(MentalScaleQuestion::getId).toList();
            mentalScaleOptionMapper.delete(new LambdaQueryWrapper<MentalScaleOption>().in(MentalScaleOption::getQuestionId, questionIds));
            mentalScaleQuestionMapper.delete(new LambdaQueryWrapper<MentalScaleQuestion>().eq(MentalScaleQuestion::getScaleId, scaleId));
        }
        for (AdminScaleQuestionRequest questionRequest : questionRequests.stream()
                .sorted(Comparator.comparing(AdminScaleQuestionRequest::getQuestionNo))
                .toList()) {
            MentalScaleQuestion question = new MentalScaleQuestion();
            question.setScaleId(scaleId);
            question.setQuestionNo(questionRequest.getQuestionNo());
            question.setContent(questionRequest.getContent().trim());
            question.setRequiredFlag(questionRequest.getRequiredFlag());
            mentalScaleQuestionMapper.insert(question);
            for (AdminScaleOptionRequest optionRequest : questionRequest.getOptions().stream()
                    .sorted(Comparator.comparing(AdminScaleOptionRequest::getSortNo))
                    .toList()) {
                MentalScaleOption option = new MentalScaleOption();
                option.setQuestionId(question.getId());
                option.setOptionCode(optionRequest.getOptionCode().trim());
                option.setContent(optionRequest.getContent().trim());
                option.setScore(optionRequest.getScore());
                option.setSortNo(optionRequest.getSortNo());
                mentalScaleOptionMapper.insert(option);
            }
        }
    }

    private boolean hasSameStructure(Long scaleId, List<AdminScaleQuestionRequest> questionRequests) {
        List<AdminScaleQuestionResponse> existingQuestions = buildQuestionResponses(scaleId);
        if (existingQuestions.size() != questionRequests.size()) {
            return false;
        }
        for (int i = 0; i < existingQuestions.size(); i++) {
            AdminScaleQuestionResponse existing = existingQuestions.get(i);
            AdminScaleQuestionRequest incoming = questionRequests.get(i);
            if (!existing.getQuestionNo().equals(incoming.getQuestionNo())
                    || !existing.getContent().equals(incoming.getContent())
                    || !existing.getRequiredFlag().equals(incoming.getRequiredFlag())
                    || existing.getOptions().size() != incoming.getOptions().size()) {
                return false;
            }
            for (int j = 0; j < existing.getOptions().size(); j++) {
                AdminScaleOptionResponse option = existing.getOptions().get(j);
                AdminScaleOptionRequest optionRequest = incoming.getOptions().get(j);
                if (!option.getOptionCode().equals(optionRequest.getOptionCode())
                        || !option.getContent().equals(optionRequest.getContent())
                        || !option.getScore().equals(optionRequest.getScore())
                        || !option.getSortNo().equals(optionRequest.getSortNo())) {
                    return false;
                }
            }
        }
        return true;
    }

    private AdminScaleResponse buildScaleResponse(MentalScale scale, boolean includeQuestions) {
        return AdminScaleResponse.builder()
                .scaleId(scale.getId())
                .code(scale.getCode())
                .name(scale.getName())
                .description(scale.getDescription())
                .introduction(scale.getIntroduction())
                .totalQuestions(scale.getTotalQuestions())
                .pageSize(scale.getPageSize())
                .lowThreshold(scale.getLowThreshold())
                .mediumThreshold(scale.getMediumThreshold())
                .highThreshold(scale.getHighThreshold())
                .status(scale.getStatus())
                .inUse(isScaleInUse(scale.getId()))
                .createdAt(scale.getCreatedAt())
                .updatedAt(scale.getUpdatedAt())
                .questions(includeQuestions ? buildQuestionResponses(scale.getId()) : List.of())
                .build();
    }

    private List<AdminScaleQuestionResponse> buildQuestionResponses(Long scaleId) {
        List<MentalScaleQuestion> questions = mentalScaleQuestionMapper.selectList(new LambdaQueryWrapper<MentalScaleQuestion>()
                .eq(MentalScaleQuestion::getScaleId, scaleId)
                .orderByAsc(MentalScaleQuestion::getQuestionNo, MentalScaleQuestion::getId));
        if (questions.isEmpty()) {
            return List.of();
        }
        List<Long> questionIds = questions.stream().map(MentalScaleQuestion::getId).toList();
        Map<Long, List<MentalScaleOption>> optionMap = mentalScaleOptionMapper.selectList(new LambdaQueryWrapper<MentalScaleOption>()
                        .in(MentalScaleOption::getQuestionId, questionIds)
                        .orderByAsc(MentalScaleOption::getSortNo, MentalScaleOption::getId))
                .stream()
                .collect(Collectors.groupingBy(MentalScaleOption::getQuestionId));
        return questions.stream()
                .map(question -> AdminScaleQuestionResponse.builder()
                        .questionId(question.getId())
                        .questionNo(question.getQuestionNo())
                        .content(question.getContent())
                        .requiredFlag(question.getRequiredFlag())
                        .options(optionMap.getOrDefault(question.getId(), List.of()).stream()
                                .map(option -> AdminScaleOptionResponse.builder()
                                        .optionId(option.getId())
                                        .optionCode(option.getOptionCode())
                                        .content(option.getContent())
                                        .score(option.getScore())
                                        .sortNo(option.getSortNo())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    private boolean isScaleInUse(Long scaleId) {
        return mentalScaleSessionMapper.selectCount(new LambdaQueryWrapper<MentalScaleSession>()
                .eq(MentalScaleSession::getScaleId, scaleId)) > 0
                || mentalScaleReportMapper.selectCount(new LambdaQueryWrapper<MentalScaleReport>()
                .eq(MentalScaleReport::getScaleId, scaleId)) > 0;
    }

    private MentalScale getRequiredScale(Long scaleId) {
        MentalScale scale = mentalScaleMapper.selectById(scaleId);
        if (scale == null) {
            throw new BusinessException("Scale does not exist");
        }
        return scale;
    }
}
