package sdu.jiaq.jqpro.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import sdu.jiaq.jqpro.common.util.PasswordCryptoUtil;
import sdu.jiaq.jqpro.entity.ConsultAppointmentSlot;
import sdu.jiaq.jqpro.entity.CounselorStudent;
import sdu.jiaq.jqpro.entity.MentalResource;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalScaleOption;
import sdu.jiaq.jqpro.entity.MentalScaleQuestion;
import sdu.jiaq.jqpro.entity.MentalScaleRule;
import sdu.jiaq.jqpro.entity.ResourceCategory;
import sdu.jiaq.jqpro.entity.ResourceTag;
import sdu.jiaq.jqpro.entity.ResourceTagRelation;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysRole;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentSlotMapper;
import sdu.jiaq.jqpro.mapper.CounselorStudentMapper;
import sdu.jiaq.jqpro.mapper.MentalResourceMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleOptionMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleQuestionMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleRuleMapper;
import sdu.jiaq.jqpro.mapper.ResourceCategoryMapper;
import sdu.jiaq.jqpro.mapper.ResourceTagMapper;
import sdu.jiaq.jqpro.mapper.ResourceTagRelationMapper;
import sdu.jiaq.jqpro.mapper.StudentProfileMapper;
import sdu.jiaq.jqpro.mapper.SysRoleMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;

/**
 * 启动阶段初始化演示账号、标准量表、预约时段和资源数据。
 * 量表种子数据采用幂等同步，重复启动只会更新既有记录，不会产生重复种子。
 */
@Component
public class DataInitializationRunner implements CommandLineRunner {
    private static final String DEFAULT_PASSWORD = "Jqpro@123";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String SLOT_STATUS_OPEN = "OPEN";
    private static final String RESOURCE_STATUS_PUBLISHED = "PUBLISHED";

    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final CounselorStudentMapper counselorStudentMapper;
    private final MentalScaleMapper mentalScaleMapper;
    private final MentalScaleQuestionMapper mentalScaleQuestionMapper;
    private final MentalScaleOptionMapper mentalScaleOptionMapper;
    private final MentalScaleRuleMapper mentalScaleRuleMapper;
    private final ConsultAppointmentSlotMapper consultAppointmentSlotMapper;
    private final ResourceCategoryMapper resourceCategoryMapper;
    private final ResourceTagMapper resourceTagMapper;
    private final MentalResourceMapper mentalResourceMapper;
    private final ResourceTagRelationMapper resourceTagRelationMapper;

    public DataInitializationRunner(
            SysRoleMapper sysRoleMapper,
            SysUserMapper sysUserMapper,
            StudentProfileMapper studentProfileMapper,
            CounselorStudentMapper counselorStudentMapper,
            MentalScaleMapper mentalScaleMapper,
            MentalScaleQuestionMapper mentalScaleQuestionMapper,
            MentalScaleOptionMapper mentalScaleOptionMapper,
            MentalScaleRuleMapper mentalScaleRuleMapper,
            ConsultAppointmentSlotMapper consultAppointmentSlotMapper,
            ResourceCategoryMapper resourceCategoryMapper,
            ResourceTagMapper resourceTagMapper,
            MentalResourceMapper mentalResourceMapper,
            ResourceTagRelationMapper resourceTagRelationMapper
    ) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserMapper = sysUserMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.counselorStudentMapper = counselorStudentMapper;
        this.mentalScaleMapper = mentalScaleMapper;
        this.mentalScaleQuestionMapper = mentalScaleQuestionMapper;
        this.mentalScaleOptionMapper = mentalScaleOptionMapper;
        this.mentalScaleRuleMapper = mentalScaleRuleMapper;
        this.consultAppointmentSlotMapper = consultAppointmentSlotMapper;
        this.resourceCategoryMapper = resourceCategoryMapper;
        this.resourceTagMapper = resourceTagMapper;
        this.mentalResourceMapper = mentalResourceMapper;
        this.resourceTagRelationMapper = resourceTagRelationMapper;
    }

    @Override
    public void run(String... args) {
        initRoles();
        initUsers();
        initScales();
        initAppointmentSlots();
        initResources();
    }

    private void initRoles() {
        upsertRole("STUDENT", "学生", "学生角色");
        upsertRole("COUNSELOR", "咨询师", "咨询师角色");
        upsertRole("ADMIN", "管理员", "管理员角色");
    }

    private void upsertRole(String code, String name, String description) {
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code).last("limit 1")
        );
        if (role == null) {
            role = new SysRole();
            role.setCode(code);
            role.setName(name);
            role.setDescription(description);
            sysRoleMapper.insert(role);
            return;
        }
        role.setName(name);
        role.setDescription(description);
        sysRoleMapper.updateById(role);
    }

    private void initUsers() {
        SysUser studentUser = upsertUser(
                "20230001",
                "张同学",
                "向日葵同学",
                "STUDENT",
                "20230001",
                null
        );
        SysUser counselorUser = upsertUser(
                "teacher01",
                "李老师",
                "李老师",
                "COUNSELOR",
                null,
                "T001"
        );
        upsertUser(
                "admin",
                "系统管理员",
                "系统管理员",
                "ADMIN",
                null,
                "A001"
        );
        upsertStudentProfile(studentUser, counselorUser.getId());
        upsertCounselorRelation(counselorUser.getId(), studentUser.getId());
    }

    private SysUser upsertUser(
            String account,
            String realName,
            String displayName,
            String roleCode,
            String studentNo,
            String counselorNo
    ) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getAccount, account).last("limit 1")
        );
        if (user == null) {
            user = new SysUser();
            user.setAccount(account);
            String salt = PasswordCryptoUtil.generateSalt();
            user.setPasswordSalt(salt);
            user.setPasswordHash(PasswordCryptoUtil.hashPassword(DEFAULT_PASSWORD, salt));
        }
        user.setRoleCode(roleCode);
        user.setRealName(realName);
        user.setDisplayName(displayName);
        user.setStudentNo(studentNo);
        user.setCounselorNo(counselorNo);
        user.setStatus(STATUS_ACTIVE);
        if (user.getId() == null) {
            sysUserMapper.insert(user);
        } else {
            sysUserMapper.updateById(user);
        }
        return user;
    }

    private void upsertStudentProfile(SysUser studentUser, Long counselorUserId) {
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getUserId, studentUser.getId())
                        .last("limit 1")
        );
        if (profile == null) {
            profile = new StudentProfile();
            profile.setUserId(studentUser.getId());
        }
        profile.setAvatarUrl("https://example.com/avatar/student-20230001.png");
        profile.setCollege("软件学院");
        profile.setGrade("2023级");
        profile.setGender("男");
        profile.setPhone("13800000000");
        profile.setEmergencyContact("张家长");
        profile.setEmergencyPhone("13900000000");
        profile.setCounselorUserId(counselorUserId);
        if (profile.getId() == null) {
            studentProfileMapper.insert(profile);
        } else {
            studentProfileMapper.updateById(profile);
        }
    }

    private void upsertCounselorRelation(Long counselorUserId, Long studentUserId) {
        CounselorStudent relation = counselorStudentMapper.selectOne(
                new LambdaQueryWrapper<CounselorStudent>()
                        .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                        .eq(CounselorStudent::getStudentUserId, studentUserId)
                        .last("limit 1")
        );
        if (relation != null) {
            return;
        }
        relation = new CounselorStudent();
        relation.setCounselorUserId(counselorUserId);
        relation.setStudentUserId(studentUserId);
        counselorStudentMapper.insert(relation);
    }

    private void initScales() {
        List<OptionSeed> standardOptions = List.of(
                new OptionSeed("A", "完全没有", 0, 1),
                new OptionSeed("B", "好几天", 1, 2),
                new OptionSeed("C", "一半以上时间", 2, 3),
                new OptionSeed("D", "几乎每天", 3, 4)
        );

        ensureScale(new ScaleSeed(
                "PHQ9",
                "PHQ-9 抑郁情绪自评量表",
                "用于快速了解过去两周抑郁情绪、兴趣和活力变化的辅助筛查量表。",
                "请根据过去两周的真实感受作答。结果仅用于心理状态辅助评估，不作为医学诊断依据。",
                3,
                0,
                5,
                10,
                List.of(
                        new QuestionSeed(1, "做事时提不起劲或没有兴趣"),
                        new QuestionSeed(2, "感到心情低落、沮丧或绝望"),
                        new QuestionSeed(3, "入睡困难、睡不安稳或睡得太多"),
                        new QuestionSeed(4, "感觉疲倦或没有活力"),
                        new QuestionSeed(5, "食欲不振或吃太多"),
                        new QuestionSeed(6, "觉得自己很糟，或觉得自己很失败，或让自己或家人失望"),
                        new QuestionSeed(7, "对事物专注有困难，例如看报纸或看电视时难以集中"),
                        new QuestionSeed(8, "动作或说话速度明显变慢，或烦躁到难以安坐"),
                        new QuestionSeed(9, "有不如离开这个世界更好的念头")
                ),
                standardOptions,
                List.of(
                        new RuleSeed("LOW", 0, 4, "结果整体相对平稳，可继续保持规律作息和日常自我照顾。", 1),
                        new RuleSeed("MEDIUM", 5, 9, "结果提示近期存在一定程度的情绪困扰，建议持续观察并主动使用校园支持资源。", 2),
                        new RuleSeed("HIGH", 10, 27, "结果提示近期需要较高关注，建议尽快联系学校心理老师、辅导员或专业支持资源。", 3)
                )
        ));

        ensureScale(new ScaleSeed(
                "GAD7",
                "GAD-7 焦虑情绪筛查量表",
                "用于快速了解过去两周焦虑、紧张和担忧状态变化的辅助筛查量表。",
                "请根据过去两周的真实感受作答。结果仅用于心理状态辅助评估，不作为医学诊断依据。",
                3,
                0,
                5,
                10,
                List.of(
                        new QuestionSeed(1, "感到紧张、焦虑或心里发慌"),
                        new QuestionSeed(2, "不能停止或无法控制担心"),
                        new QuestionSeed(3, "对各种事情担忧过多"),
                        new QuestionSeed(4, "很难放松下来"),
                        new QuestionSeed(5, "由于坐立不安而难以安静坐着"),
                        new QuestionSeed(6, "变得容易烦恼或急躁"),
                        new QuestionSeed(7, "感到似乎将有可怕的事情发生")
                ),
                standardOptions,
                List.of(
                        new RuleSeed("LOW", 0, 4, "结果整体相对平稳，可继续保持稳定的生活节奏和压力管理习惯。", 1),
                        new RuleSeed("MEDIUM", 5, 9, "结果提示近期存在一定程度的紧张和担忧，建议留意压力源并及时进行调整。", 2),
                        new RuleSeed("HIGH", 10, 21, "结果提示近期需要较高关注，建议尽快联系学校心理老师、辅导员或专业支持资源。", 3)
                )
        ));
    }

    private void ensureScale(ScaleSeed seed) {
        MentalScale scale = mentalScaleMapper.selectOne(
                new LambdaQueryWrapper<MentalScale>().eq(MentalScale::getCode, seed.code()).last("limit 1")
        );
        if (scale == null) {
            scale = new MentalScale();
            scale.setCode(seed.code());
        }
        scale.setName(seed.name());
        scale.setDescription(seed.description());
        scale.setIntroduction(seed.introduction());
        scale.setTotalQuestions(seed.questions().size());
        scale.setPageSize(seed.pageSize());
        scale.setLowThreshold(seed.lowThreshold());
        scale.setMediumThreshold(seed.mediumThreshold());
        scale.setHighThreshold(seed.highThreshold());
        scale.setStatus(STATUS_ACTIVE);
        if (scale.getId() == null) {
            mentalScaleMapper.insert(scale);
        } else {
            mentalScaleMapper.updateById(scale);
        }

        for (QuestionSeed questionSeed : seed.questions()) {
            MentalScaleQuestion question = mentalScaleQuestionMapper.selectOne(
                    new LambdaQueryWrapper<MentalScaleQuestion>()
                            .eq(MentalScaleQuestion::getScaleId, scale.getId())
                            .eq(MentalScaleQuestion::getQuestionNo, questionSeed.questionNo())
                            .last("limit 1")
            );
            if (question == null) {
                question = new MentalScaleQuestion();
                question.setScaleId(scale.getId());
                question.setQuestionNo(questionSeed.questionNo());
            }
            question.setContent(questionSeed.content());
            question.setRequiredFlag(1);
            if (question.getId() == null) {
                mentalScaleQuestionMapper.insert(question);
            } else {
                mentalScaleQuestionMapper.updateById(question);
            }
            ensureOptions(question.getId(), seed.options());
        }

        for (RuleSeed ruleSeed : seed.rules()) {
            MentalScaleRule rule = mentalScaleRuleMapper.selectOne(
                    new LambdaQueryWrapper<MentalScaleRule>()
                            .eq(MentalScaleRule::getScaleId, scale.getId())
                            .eq(MentalScaleRule::getLevelCode, ruleSeed.levelCode())
                            .last("limit 1")
            );
            if (rule == null) {
                rule = new MentalScaleRule();
                rule.setScaleId(scale.getId());
                rule.setLevelCode(ruleSeed.levelCode());
            }
            rule.setMinScore(ruleSeed.minScore());
            rule.setMaxScore(ruleSeed.maxScore());
            rule.setSummaryText(ruleSeed.summaryText());
            rule.setSortNo(ruleSeed.sortNo());
            if (rule.getId() == null) {
                mentalScaleRuleMapper.insert(rule);
            } else {
                mentalScaleRuleMapper.updateById(rule);
            }
        }

        validateScaleSeed(scale, seed);
    }

    private void ensureOptions(Long questionId, List<OptionSeed> optionSeeds) {
        for (OptionSeed optionSeed : optionSeeds) {
            MentalScaleOption option = mentalScaleOptionMapper.selectOne(
                    new LambdaQueryWrapper<MentalScaleOption>()
                            .eq(MentalScaleOption::getQuestionId, questionId)
                            .eq(MentalScaleOption::getOptionCode, optionSeed.optionCode())
                            .last("limit 1")
            );
            if (option == null) {
                option = new MentalScaleOption();
                option.setQuestionId(questionId);
                option.setOptionCode(optionSeed.optionCode());
            }
            option.setContent(optionSeed.content());
            option.setScore(optionSeed.score());
            option.setSortNo(optionSeed.sortNo());
            if (option.getId() == null) {
                mentalScaleOptionMapper.insert(option);
            } else {
                mentalScaleOptionMapper.updateById(option);
            }
        }
    }

    private void validateScaleSeed(MentalScale scale, ScaleSeed seed) {
        List<MentalScaleQuestion> questions = mentalScaleQuestionMapper.selectList(
                new LambdaQueryWrapper<MentalScaleQuestion>()
                        .eq(MentalScaleQuestion::getScaleId, scale.getId())
                        .orderByAsc(MentalScaleQuestion::getQuestionNo)
        );
        if (questions.size() != seed.questions().size()) {
            throw new IllegalStateException("量表种子题目数量异常: " + seed.code());
        }
        for (MentalScaleQuestion question : questions) {
            long optionCount = mentalScaleOptionMapper.selectCount(
                    new LambdaQueryWrapper<MentalScaleOption>()
                            .eq(MentalScaleOption::getQuestionId, question.getId())
            );
            if (optionCount != seed.options().size()) {
                throw new IllegalStateException("量表选项数量异常: " + seed.code() + ", questionNo=" + question.getQuestionNo());
            }
        }
        long ruleCount = mentalScaleRuleMapper.selectCount(
                new LambdaQueryWrapper<MentalScaleRule>().eq(MentalScaleRule::getScaleId, scale.getId())
        );
        if (ruleCount != seed.rules().size()) {
            throw new IllegalStateException("量表规则数量异常: " + seed.code());
        }
    }

    private void initAppointmentSlots() {
        SysUser counselorUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getAccount, "teacher01").last("limit 1")
        );
        if (counselorUser == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Long activeCount = consultAppointmentSlotMapper.selectCount(
                new LambdaQueryWrapper<ConsultAppointmentSlot>()
                        .eq(ConsultAppointmentSlot::getCounselorUserId, counselorUser.getId())
                        .eq(ConsultAppointmentSlot::getStatus, SLOT_STATUS_OPEN)
                        .le(ConsultAppointmentSlot::getStartTime, now.plusMinutes(5))
                        .ge(ConsultAppointmentSlot::getEndTime, now.plusMinutes(20))
        );
        if (activeCount == null || activeCount == 0) {
            saveAppointmentSlot(
                    counselorUser.getId(),
                    now.minusMinutes(10).withSecond(0).withNano(0),
                    now.plusMinutes(40).withSecond(0).withNano(0)
            );
        }

        Long totalCount = consultAppointmentSlotMapper.selectCount(
                new LambdaQueryWrapper<ConsultAppointmentSlot>()
                        .eq(ConsultAppointmentSlot::getCounselorUserId, counselorUser.getId())
        );
        if (totalCount != null && totalCount > 0) {
            return;
        }

        LocalDateTime baseTime = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        saveAppointmentSlot(counselorUser.getId(), baseTime, baseTime.plusMinutes(50));
        saveAppointmentSlot(counselorUser.getId(), baseTime.plusHours(2), baseTime.plusHours(2).plusMinutes(50));
    }

    private void saveAppointmentSlot(Long counselorUserId, LocalDateTime startTime, LocalDateTime endTime) {
        ConsultAppointmentSlot slot = new ConsultAppointmentSlot();
        slot.setCounselorUserId(counselorUserId);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setStatus(SLOT_STATUS_OPEN);
        consultAppointmentSlotMapper.insert(slot);
    }

    private void initResources() {
        String assetBaseUrl = "http://127.0.0.1:8080/assets/resources";
        offlineLegacyDemoResources();
        ResourceCategory relaxCategory = saveCategoryIfAbsent("情绪舒缓", "帮助学生进行放松、减压和情绪稳定。", 1);
        ResourceCategory growthCategory = saveCategoryIfAbsent("自助成长", "帮助学生建立习惯、提升自我认知与求助意识。", 2);
        ResourceTag breathTag = saveTagIfAbsent("呼吸训练", "短时放松训练");
        ResourceTag sleepTag = saveTagIfAbsent("睡眠", "睡眠卫生与改善方法");
        ResourceTag stressTag = saveTagIfAbsent("学业压力", "学习与考试压力调节");
        saveResourceIfAbsent(
                relaxCategory.getId(),
                "三分钟呼吸放松",
                "通过短时呼吸练习帮助学生快速降低紧张感。",
                "AUDIO",
                "https://example.com/resources/breathing",
                "https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=800&q=80",
                List.of(breathTag.getId(), stressTag.getId())
        );
        saveResourceIfAbsent(
                growthCategory.getId(),
                "睡眠习惯自查清单",
                "从作息、手机使用和环境布置三个方面帮助学生建立睡眠卫生习惯。",
                "ARTICLE",
                "https://example.com/resources/sleep-checklist",
                "https://images.unsplash.com/photo-1495195134817-aeb325a55b65?auto=format&fit=crop&w=800&q=80",
                List.of(sleepTag.getId())
        );
        saveResourceIfAbsent(
                growthCategory.getId(),
                "考试周减压工具包",
                "整合时间切分、情绪记录和求助提醒三个模块，帮助学生在考试周保持节奏。",
                "LINK",
                "https://example.com/resources/exam-pressure",
                "https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&w=800&q=80",
                List.of(stressTag.getId())
        );

        ResourceCategory relaxVideoCategory = saveCategoryIfAbsent("舒缓放松", "用于承载减压、呼吸放松与感官安抚类资源。", 1);
        ResourceCategory sleepCategory = saveCategoryIfAbsent("睡眠修复", "用于承载睡前整理、作息修复与环境调整类资源。", 2);
        ResourceCategory studyCategory = saveCategoryIfAbsent("学习节律", "用于承载考试周节奏管理、专注恢复与校园支持类资源。", 3);

        ResourceTag groundingTag = saveTagIfAbsent("呼吸练习", "适合短时间快速降噪与回到当下的练习。");
        ResourceTag windDownTag = saveTagIfAbsent("睡前放松", "适合夜间整理情绪、降低唤醒水平的内容。");
        ResourceTag finalsTag = saveTagIfAbsent("考试周", "适合考试周压力管理与节律修复。");
        ResourceTag supportTag = saveTagIfAbsent("校园支持", "强调校园场景中的稳定支持与求助提醒。");

        saveOrUpdateResourceSeed(
                relaxVideoCategory.getId(),
                "林间慢呼吸视频",
                "一段适合 1 到 2 分钟观看的舒缓视频，可在紧张、心跳偏快或注意力发散时先做减速过渡。",
                "VIDEO",
                assetBaseUrl + "/videos/breathing-garden.mp4",
                assetBaseUrl + "/images/breathing-cover.jpg",
                List.of(groundingTag.getId(), supportTag.getId())
        );
        saveOrUpdateResourceSeed(
                sleepCategory.getId(),
                "睡前降噪整理卡",
                "整理灯光、手机、呼吸节奏和床边环境的睡前卡片，适合在宿舍里快速完成。",
                "ARTICLE",
                assetBaseUrl + "/articles/sleep-reset-guide.html",
                assetBaseUrl + "/images/sleep-cover.jpg",
                List.of(windDownTag.getId(), supportTag.getId())
        );
        saveOrUpdateResourceSeed(
                studyCategory.getId(),
                "考试周节律调整手册",
                "把复习、休息、饮水和求助提醒放进同一页，帮助学生在高压周期内稳定节奏。",
                "ARTICLE",
                assetBaseUrl + "/articles/exam-rhythm-toolkit.html",
                assetBaseUrl + "/images/study-cover.jpg",
                List.of(finalsTag.getId(), supportTag.getId())
        );
        saveOrUpdateResourceSeed(
                studyCategory.getId(),
                "校园节律观察短片",
                "一段可用于放空视线和短暂停顿的校园氛围短片，适合作为长时间学习后的切换点。",
                "VIDEO",
                assetBaseUrl + "/videos/campus-rhythm.mp4",
                assetBaseUrl + "/images/study-cover.jpg",
                List.of(finalsTag.getId(), supportTag.getId())
        );
        offlineLegacyDemoResources();
    }

    private ResourceCategory saveCategoryIfAbsent(String name, String description, int sortNo) {
        ResourceCategory category = resourceCategoryMapper.selectOne(
                new LambdaQueryWrapper<ResourceCategory>().eq(ResourceCategory::getName, name).last("limit 1")
        );
        if (category == null) {
            category = new ResourceCategory();
            category.setName(name);
        }
        category.setDescription(description);
        category.setSortNo(sortNo);
        category.setStatus(STATUS_ACTIVE);
        if (category.getId() == null) {
            resourceCategoryMapper.insert(category);
        } else {
            resourceCategoryMapper.updateById(category);
        }
        return category;
    }

    private ResourceTag saveTagIfAbsent(String name, String description) {
        ResourceTag tag = resourceTagMapper.selectOne(
                new LambdaQueryWrapper<ResourceTag>().eq(ResourceTag::getName, name).last("limit 1")
        );
        if (tag == null) {
            tag = new ResourceTag();
            tag.setName(name);
        }
        tag.setDescription(description);
        if (tag.getId() == null) {
            resourceTagMapper.insert(tag);
        } else {
            resourceTagMapper.updateById(tag);
        }
        return tag;
    }

    private MentalResource saveResourceIfAbsent(
            Long categoryId,
            String title,
            String summaryText,
            String resourceType,
            String contentUrl,
            String coverUrl,
            List<Long> tagIds
    ) {
        MentalResource resource = mentalResourceMapper.selectOne(
                new LambdaQueryWrapper<MentalResource>().eq(MentalResource::getTitle, title).last("limit 1")
        );
        if (resource != null) {
            return resource;
        }
        resource = new MentalResource();
        resource.setCategoryId(categoryId);
        resource.setTitle(title);
        resource.setSummaryText(summaryText);
        resource.setResourceType(resourceType);
        resource.setContentUrl(contentUrl);
        resource.setCoverUrl(coverUrl);
        resource.setStatus(RESOURCE_STATUS_PUBLISHED);
        resource.setPublishedAt(LocalDateTime.now());
        mentalResourceMapper.insert(resource);
        for (Long tagId : tagIds) {
            ResourceTagRelation relation = new ResourceTagRelation();
            relation.setResourceId(resource.getId());
            relation.setTagId(tagId);
            resourceTagRelationMapper.insert(relation);
        }
        return resource;
    }

    private MentalResource saveOrUpdateResourceSeed(
            Long categoryId,
            String title,
            String summaryText,
            String resourceType,
            String contentUrl,
            String coverUrl,
            List<Long> tagIds
    ) {
        MentalResource resource = mentalResourceMapper.selectOne(
                new LambdaQueryWrapper<MentalResource>().eq(MentalResource::getTitle, title).last("limit 1")
        );
        boolean isNew = resource == null;
        if (isNew) {
            resource = new MentalResource();
            resource.setTitle(title);
            resource.setPublishedAt(LocalDateTime.now());
        }
        resource.setCategoryId(categoryId);
        resource.setSummaryText(summaryText);
        resource.setResourceType(resourceType);
        resource.setContentUrl(contentUrl);
        resource.setCoverUrl(coverUrl);
        resource.setStatus(RESOURCE_STATUS_PUBLISHED);
        if (isNew) {
            mentalResourceMapper.insert(resource);
        } else {
            mentalResourceMapper.updateById(resource);
        }

        List<ResourceTagRelation> existingRelations = resourceTagRelationMapper.selectList(
                new LambdaQueryWrapper<ResourceTagRelation>().eq(ResourceTagRelation::getResourceId, resource.getId())
        );
        for (ResourceTagRelation existingRelation : existingRelations) {
            resourceTagRelationMapper.deleteById(existingRelation.getId());
        }
        for (Long tagId : tagIds) {
            ResourceTagRelation relation = new ResourceTagRelation();
            relation.setResourceId(resource.getId());
            relation.setTagId(tagId);
            resourceTagRelationMapper.insert(relation);
        }
        return resource;
    }

    private void offlineLegacyDemoResources() {
        List<MentalResource> legacyResources = mentalResourceMapper.selectList(new LambdaQueryWrapper<MentalResource>()
                .and(wrapper -> wrapper
                        .like(MentalResource::getContentUrl, "https://example.com/resources/")
                        .or()
                        .like(MentalResource::getCoverUrl, "https://images.unsplash.com/")
                        .or()
                        .like(MentalResource::getTitle, "Phase3 Acceptance Resource")
                        .or()
                        .like(MentalResource::getTitle, "Acceptance ")
                        .or()
                        .like(MentalResource::getTitle, "Stage79 Acceptance")
                        .or()
                        .like(MentalResource::getTitle, "Phase3 Admin Resource Check"))
                .eq(MentalResource::getStatus, RESOURCE_STATUS_PUBLISHED));
        for (MentalResource legacyResource : legacyResources) {
            legacyResource.setStatus("OFFLINE");
            mentalResourceMapper.updateById(legacyResource);
        }
    }

    private record QuestionSeed(int questionNo, String content) {
    }

    private record OptionSeed(String optionCode, String content, int score, int sortNo) {
    }

    private record RuleSeed(String levelCode, int minScore, int maxScore, String summaryText, int sortNo) {
    }

    private record ScaleSeed(
            String code,
            String name,
            String description,
            String introduction,
            int pageSize,
            int lowThreshold,
            int mediumThreshold,
            int highThreshold,
            List<QuestionSeed> questions,
            List<OptionSeed> options,
            List<RuleSeed> rules
    ) {
    }
}
