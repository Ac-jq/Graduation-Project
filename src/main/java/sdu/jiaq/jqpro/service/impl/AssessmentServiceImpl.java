package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.ReportLevelConstants;
import sdu.jiaq.jqpro.common.constant.ScaleSessionStatusConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.assessment.AnswerItemRequest;
import sdu.jiaq.jqpro.dto.assessment.AnswerSaveRequest;
import sdu.jiaq.jqpro.dto.assessment.QuestionOptionResponse;
import sdu.jiaq.jqpro.dto.assessment.QuestionResponse;
import sdu.jiaq.jqpro.dto.assessment.ScaleDetailResponse;
import sdu.jiaq.jqpro.dto.assessment.ScaleQuestionPageResponse;
import sdu.jiaq.jqpro.dto.assessment.ScaleSessionResponse;
import sdu.jiaq.jqpro.dto.assessment.ScaleSummaryResponse;
import sdu.jiaq.jqpro.dto.assessment.SubmitScaleResponse;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalScaleAnswer;
import sdu.jiaq.jqpro.entity.MentalScaleOption;
import sdu.jiaq.jqpro.entity.MentalScaleQuestion;
import sdu.jiaq.jqpro.entity.MentalScaleReport;
import sdu.jiaq.jqpro.entity.MentalScaleSession;
import sdu.jiaq.jqpro.mapper.MentalScaleAnswerMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleOptionMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleQuestionMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleReportMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleSessionMapper;
import sdu.jiaq.jqpro.service.AiInterpretationService;
import sdu.jiaq.jqpro.service.AssessmentService;
import sdu.jiaq.jqpro.service.AuditLogService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 量表测评服务实现。
 */
@Service
public class AssessmentServiceImpl implements AssessmentService {

    private final MentalScaleMapper mentalScaleMapper;
    private final MentalScaleQuestionMapper mentalScaleQuestionMapper;
    private final MentalScaleOptionMapper mentalScaleOptionMapper;
    private final MentalScaleSessionMapper mentalScaleSessionMapper;
    private final MentalScaleAnswerMapper mentalScaleAnswerMapper;
    private final MentalScaleReportMapper mentalScaleReportMapper;
    private final AiInterpretationService aiInterpretationService;
    private final AuditLogService auditLogService;

    public AssessmentServiceImpl(MentalScaleMapper mentalScaleMapper,
                                 MentalScaleQuestionMapper mentalScaleQuestionMapper,
                                 MentalScaleOptionMapper mentalScaleOptionMapper,
                                 MentalScaleSessionMapper mentalScaleSessionMapper,
                                 MentalScaleAnswerMapper mentalScaleAnswerMapper,
                                 MentalScaleReportMapper mentalScaleReportMapper,
                                 AiInterpretationService aiInterpretationService,
                                 AuditLogService auditLogService) {
        this.mentalScaleMapper = mentalScaleMapper;
        this.mentalScaleQuestionMapper = mentalScaleQuestionMapper;
        this.mentalScaleOptionMapper = mentalScaleOptionMapper;
        this.mentalScaleSessionMapper = mentalScaleSessionMapper;
        this.mentalScaleAnswerMapper = mentalScaleAnswerMapper;
        this.mentalScaleReportMapper = mentalScaleReportMapper;
        this.aiInterpretationService = aiInterpretationService;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<ScaleSummaryResponse> listActiveScales() {
        List<MentalScale> scales = mentalScaleMapper.selectList(new LambdaQueryWrapper<MentalScale>()
                .eq(MentalScale::getStatus, "ACTIVE")
                .orderByAsc(MentalScale::getId));
        return scales.stream()
                .map(scale -> ScaleSummaryResponse.builder()
                        .id(scale.getId())
                        .code(scale.getCode())
                        .name(scale.getName())
                        .description(scale.getDescription())
                        .totalQuestions(scale.getTotalQuestions())
                        .pageSize(scale.getPageSize())
                        .build())
                .toList();
    }

    @Override
    public ScaleDetailResponse getScaleDetail(Long scaleId) {
        MentalScale scale = getRequiredScale(scaleId);
        return ScaleDetailResponse.builder()
                .id(scale.getId())
                .code(scale.getCode())
                .name(scale.getName())
                .description(scale.getDescription())
                .introduction(scale.getIntroduction())
                .totalQuestions(scale.getTotalQuestions())
                .pageSize(scale.getPageSize())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScaleSessionResponse createOrGetDraftSession(Long scaleId) {
        MentalScale scale = getRequiredScale(scaleId);
        Long userId = SecurityUtil.getCurrentUserId();
        MentalScaleSession session = mentalScaleSessionMapper.selectOne(new LambdaQueryWrapper<MentalScaleSession>()
                .eq(MentalScaleSession::getScaleId, scaleId)
                .eq(MentalScaleSession::getUserId, userId)
                .eq(MentalScaleSession::getStatus, ScaleSessionStatusConstants.DRAFT)
                .last("limit 1"));
        if (session == null) {
            session = new MentalScaleSession();
            session.setScaleId(scaleId);
            session.setUserId(userId);
            session.setStatus(ScaleSessionStatusConstants.DRAFT);
            session.setAnsweredCount(0);
            mentalScaleSessionMapper.insert(session);
        }
        return buildSessionResponse(session, scale.getTotalQuestions());
    }

    @Override
    public ScaleQuestionPageResponse getQuestionPage(Long sessionId, Integer pageNum, Integer pageSize) {
        MentalScaleSession session = getOwnedDraftOrSubmittedSession(sessionId);
        MentalScale scale = getRequiredScale(session.getScaleId());
        int currentPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentPageSize = pageSize == null || pageSize < 1 ? scale.getPageSize() : pageSize;

        Page<MentalScaleQuestion> page = mentalScaleQuestionMapper.selectPage(new Page<>(currentPageNum, currentPageSize),
                new LambdaQueryWrapper<MentalScaleQuestion>()
                        .eq(MentalScaleQuestion::getScaleId, scale.getId())
                        .orderByAsc(MentalScaleQuestion::getQuestionNo));

        List<MentalScaleQuestion> questionRecords = page.getRecords();
        Set<Long> questionIds = questionRecords.stream().map(MentalScaleQuestion::getId).collect(Collectors.toSet());
        Map<Long, Long> selectedOptionMap = buildSelectedOptionMap(sessionId, questionIds);
        Map<Long, List<MentalScaleOption>> optionMap = buildOptionMap(questionIds);

        List<QuestionResponse> records = questionRecords.stream()
                .sorted(Comparator.comparing(MentalScaleQuestion::getQuestionNo))
                .map(question -> QuestionResponse.builder()
                        .questionId(question.getId())
                        .questionNo(question.getQuestionNo())
                        .content(question.getContent())
                        .selectedOptionId(selectedOptionMap.get(question.getId()))
                        .options(optionMap.getOrDefault(question.getId(), List.of()).stream()
                                .map(option -> QuestionOptionResponse.builder()
                                        .id(option.getId())
                                        .optionCode(option.getOptionCode())
                                        .content(option.getContent())
                                        .score(option.getScore())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return ScaleQuestionPageResponse.builder()
                .sessionId(session.getId())
                .pageNum(currentPageNum)
                .pageSize(currentPageSize)
                .total(page.getTotal())
                .answeredCount(session.getAnsweredCount())
                .totalQuestions(scale.getTotalQuestions())
                .records(records)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScaleSessionResponse saveAnswers(Long sessionId, AnswerSaveRequest request) {
        MentalScaleSession session = getOwnedDraftSession(sessionId);
        List<AnswerItemRequest> answerRequests = request.getAnswers();
        Set<Long> questionIds = answerRequests.stream().map(AnswerItemRequest::getQuestionId).collect(Collectors.toSet());
        Set<Long> optionIds = answerRequests.stream().map(AnswerItemRequest::getOptionId).collect(Collectors.toSet());

        List<MentalScaleQuestion> questions = mentalScaleQuestionMapper.selectList(new LambdaQueryWrapper<MentalScaleQuestion>()
                .eq(MentalScaleQuestion::getScaleId, session.getScaleId())
                .in(MentalScaleQuestion::getId, questionIds));
        if (questions.size() != questionIds.size()) {
            throw new BusinessException("存在不属于当前量表的题目");
        }

        Map<Long, MentalScaleOption> optionMap = mentalScaleOptionMapper.selectList(new LambdaQueryWrapper<MentalScaleOption>()
                        .in(MentalScaleOption::getId, optionIds))
                .stream()
                .collect(Collectors.toMap(MentalScaleOption::getId, Function.identity()));
        if (optionMap.size() != optionIds.size()) {
            throw new BusinessException("存在非法选项");
        }

        Map<Long, MentalScaleQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(MentalScaleQuestion::getId, Function.identity()));

        for (AnswerItemRequest answerRequest : answerRequests) {
            MentalScaleQuestion question = questionMap.get(answerRequest.getQuestionId());
            MentalScaleOption option = optionMap.get(answerRequest.getOptionId());
            if (question == null || option == null || !question.getId().equals(option.getQuestionId())) {
                throw new BusinessException("题目与选项不匹配");
            }

            MentalScaleAnswer existingAnswer = mentalScaleAnswerMapper.selectOne(new LambdaQueryWrapper<MentalScaleAnswer>()
                    .eq(MentalScaleAnswer::getSessionId, sessionId)
                    .eq(MentalScaleAnswer::getQuestionId, question.getId())
                    .last("limit 1"));
            if (existingAnswer == null) {
                existingAnswer = new MentalScaleAnswer();
                existingAnswer.setSessionId(sessionId);
                existingAnswer.setQuestionId(question.getId());
                existingAnswer.setOptionId(option.getId());
                existingAnswer.setScore(option.getScore());
                mentalScaleAnswerMapper.insert(existingAnswer);
            } else {
                existingAnswer.setOptionId(option.getId());
                existingAnswer.setScore(option.getScore());
                mentalScaleAnswerMapper.updateById(existingAnswer);
            }
        }

        int answeredCount = Math.toIntExact(mentalScaleAnswerMapper.selectCount(new LambdaQueryWrapper<MentalScaleAnswer>()
                .eq(MentalScaleAnswer::getSessionId, sessionId)));
        session.setAnsweredCount(answeredCount);
        mentalScaleSessionMapper.updateById(session);

        MentalScale scale = getRequiredScale(session.getScaleId());
        return buildSessionResponse(session, scale.getTotalQuestions());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitScaleResponse submit(Long sessionId) {
        MentalScaleSession session = getOwnedDraftSession(sessionId);
        MentalScale scale = getRequiredScale(session.getScaleId());
        List<MentalScaleAnswer> answers = mentalScaleAnswerMapper.selectList(new LambdaQueryWrapper<MentalScaleAnswer>()
                .eq(MentalScaleAnswer::getSessionId, sessionId));
        if (answers.size() != scale.getTotalQuestions()) {
            throw new BusinessException("量表尚未全部作答，暂不能提交");
        }

        int totalScore = answers.stream().mapToInt(MentalScaleAnswer::getScore).sum();
        String levelCode = resolveLevel(scale, totalScore);
        String summaryText = buildSummary(scale.getName(), totalScore, levelCode);
        String interpretation = aiInterpretationService.generateInterpretation(scale, totalScore, levelCode);

        session.setStatus(ScaleSessionStatusConstants.SUBMITTED);
        session.setAnsweredCount(scale.getTotalQuestions());
        session.setTotalScore(totalScore);
        session.setSubmittedAt(LocalDateTime.now());
        mentalScaleSessionMapper.updateById(session);

        MentalScaleReport report = new MentalScaleReport();
        report.setSessionId(sessionId);
        report.setScaleId(scale.getId());
        report.setUserId(session.getUserId());
        report.setLevelCode(levelCode);
        report.setTotalScore(totalScore);
        report.setSummaryText(summaryText);
        report.setAiInterpretation(interpretation);
        mentalScaleReportMapper.insert(report);

        auditLogService.record(session.getUserId(), "ASSESSMENT_SUBMIT", "提交测评", scale.getName() + " 提交并生成报告", "system");

        return SubmitScaleResponse.builder()
                .sessionId(sessionId)
                .reportId(report.getId())
                .totalScore(totalScore)
                .levelCode(levelCode)
                .summaryText(summaryText)
                .build();
    }

    private MentalScale getRequiredScale(Long scaleId) {
        MentalScale scale = mentalScaleMapper.selectById(scaleId);
        if (scale == null || !"ACTIVE".equals(scale.getStatus())) {
            throw new BusinessException("量表不存在或已停用");
        }
        return scale;
    }

    private MentalScaleSession getOwnedDraftSession(Long sessionId) {
        MentalScaleSession session = getOwnedDraftOrSubmittedSession(sessionId);
        if (!ScaleSessionStatusConstants.DRAFT.equals(session.getStatus())) {
            throw new BusinessException("当前作答会话已提交，不能继续修改");
        }
        return session;
    }

    private MentalScaleSession getOwnedDraftOrSubmittedSession(Long sessionId) {
        Long userId = SecurityUtil.getCurrentUserId();
        MentalScaleSession session = mentalScaleSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException("作答会话不存在");
        }
        return session;
    }

    private ScaleSessionResponse buildSessionResponse(MentalScaleSession session, Integer totalQuestions) {
        return ScaleSessionResponse.builder()
                .sessionId(session.getId())
                .scaleId(session.getScaleId())
                .answeredCount(session.getAnsweredCount())
                .totalQuestions(totalQuestions)
                .status(session.getStatus())
                .build();
    }

    private Map<Long, Long> buildSelectedOptionMap(Long sessionId, Set<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        List<MentalScaleAnswer> answers = mentalScaleAnswerMapper.selectList(new LambdaQueryWrapper<MentalScaleAnswer>()
                .eq(MentalScaleAnswer::getSessionId, sessionId)
                .in(MentalScaleAnswer::getQuestionId, questionIds));
        Map<Long, Long> selectedOptionMap = new HashMap<>(answers.size());
        for (MentalScaleAnswer answer : answers) {
            selectedOptionMap.put(answer.getQuestionId(), answer.getOptionId());
        }
        return selectedOptionMap;
    }

    private Map<Long, List<MentalScaleOption>> buildOptionMap(Set<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        List<MentalScaleOption> options = mentalScaleOptionMapper.selectList(new LambdaQueryWrapper<MentalScaleOption>()
                .in(MentalScaleOption::getQuestionId, questionIds)
                .orderByAsc(MentalScaleOption::getQuestionId, MentalScaleOption::getSortNo));
        Map<Long, List<MentalScaleOption>> optionMap = new HashMap<>();
        for (MentalScaleOption option : options) {
            optionMap.computeIfAbsent(option.getQuestionId(), key -> new ArrayList<>()).add(option);
        }
        return optionMap;
    }

    private String resolveLevel(MentalScale scale, int totalScore) {
        if (totalScore >= scale.getHighThreshold()) {
            return ReportLevelConstants.HIGH;
        }
        if (totalScore >= scale.getMediumThreshold()) {
            return ReportLevelConstants.MEDIUM;
        }
        return ReportLevelConstants.LOW;
    }

    private String buildSummary(String scaleName, int totalScore, String levelCode) {
        return "%s已完成，总分%s，当前评估等级为%s。".formatted(scaleName, totalScore, levelCode);
    }
}
