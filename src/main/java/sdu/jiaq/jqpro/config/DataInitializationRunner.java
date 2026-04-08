package sdu.jiaq.jqpro.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.constant.AppointmentConstants;
import sdu.jiaq.jqpro.common.constant.ResourceConstants;
import sdu.jiaq.jqpro.common.constant.UserStatusConstants;
import sdu.jiaq.jqpro.common.util.PasswordCryptoUtil;
import sdu.jiaq.jqpro.entity.CounselorStudent;
import sdu.jiaq.jqpro.entity.ConsultAppointmentSlot;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalResource;
import sdu.jiaq.jqpro.entity.MentalScaleOption;
import sdu.jiaq.jqpro.entity.MentalScaleQuestion;
import sdu.jiaq.jqpro.entity.ResourceCategory;
import sdu.jiaq.jqpro.entity.ResourceTag;
import sdu.jiaq.jqpro.entity.ResourceTagRelation;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysRole;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.CounselorStudentMapper;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentSlotMapper;
import sdu.jiaq.jqpro.mapper.MentalResourceMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleOptionMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleQuestionMapper;
import sdu.jiaq.jqpro.mapper.ResourceCategoryMapper;
import sdu.jiaq.jqpro.mapper.ResourceTagMapper;
import sdu.jiaq.jqpro.mapper.ResourceTagRelationMapper;
import sdu.jiaq.jqpro.mapper.StudentProfileMapper;
import sdu.jiaq.jqpro.mapper.SysRoleMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 初始化演示数据。
 * 仅在数据库为空时补齐最小演示数据，便于本地直接启动验证三个闭环。
 */
@Component
public class DataInitializationRunner implements CommandLineRunner {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final CounselorStudentMapper counselorStudentMapper;
    private final MentalScaleMapper mentalScaleMapper;
    private final MentalScaleQuestionMapper mentalScaleQuestionMapper;
    private final MentalScaleOptionMapper mentalScaleOptionMapper;
    private final ConsultAppointmentSlotMapper consultAppointmentSlotMapper;
    private final ResourceCategoryMapper resourceCategoryMapper;
    private final ResourceTagMapper resourceTagMapper;
    private final MentalResourceMapper mentalResourceMapper;
    private final ResourceTagRelationMapper resourceTagRelationMapper;

    public DataInitializationRunner(SysRoleMapper sysRoleMapper,
                                    SysUserMapper sysUserMapper,
                                    StudentProfileMapper studentProfileMapper,
                                    CounselorStudentMapper counselorStudentMapper,
                                    MentalScaleMapper mentalScaleMapper,
                                    MentalScaleQuestionMapper mentalScaleQuestionMapper,
                                    MentalScaleOptionMapper mentalScaleOptionMapper,
                                    ConsultAppointmentSlotMapper consultAppointmentSlotMapper,
                                    ResourceCategoryMapper resourceCategoryMapper,
                                    ResourceTagMapper resourceTagMapper,
                                    MentalResourceMapper mentalResourceMapper,
                                    ResourceTagRelationMapper resourceTagRelationMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserMapper = sysUserMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.counselorStudentMapper = counselorStudentMapper;
        this.mentalScaleMapper = mentalScaleMapper;
        this.mentalScaleQuestionMapper = mentalScaleQuestionMapper;
        this.mentalScaleOptionMapper = mentalScaleOptionMapper;
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
        initScale();
        initAppointmentSlots();
        initResources();
    }

    private void initRoles() {
        saveRoleIfAbsent(RoleConstants.STUDENT, "学生", "学生角色");
        saveRoleIfAbsent(RoleConstants.COUNSELOR, "咨询师", "咨询师角色");
        saveRoleIfAbsent(RoleConstants.ADMIN, "管理员", "管理员角色");
    }

    private void saveRoleIfAbsent(String code, String name, String description) {
        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code));
        if (count != null && count > 0) {
            return;
        }
        SysRole sysRole = new SysRole();
        sysRole.setCode(code);
        sysRole.setName(name);
        sysRole.setDescription(description);
        sysRoleMapper.insert(sysRole);
    }

    private void initUsers() {
        SysUser studentUser = saveUserIfAbsent("20230001", "张同学", "向日葵同学", RoleConstants.STUDENT, "20230001", null);
        SysUser counselorUser = saveUserIfAbsent("teacher01", "李老师", "李老师", RoleConstants.COUNSELOR, null, "T001");
        saveUserIfAbsent("admin", "系统管理员", "系统管理员", RoleConstants.ADMIN, null, "A001");

        saveStudentProfileIfAbsent(studentUser, counselorUser.getId());
        saveCounselorRelationIfAbsent(counselorUser.getId(), studentUser.getId());
    }

    private SysUser saveUserIfAbsent(String account,
                                     String realName,
                                     String displayName,
                                     String roleCode,
                                     String studentNo,
                                     String counselorNo) {
        SysUser existingUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, account)
                .last("limit 1"));
        if (existingUser != null) {
            return existingUser;
        }

        String salt = PasswordCryptoUtil.generateSalt();
        SysUser sysUser = new SysUser();
        sysUser.setAccount(account);
        sysUser.setPasswordSalt(salt);
        sysUser.setPasswordHash(PasswordCryptoUtil.hashPassword("Jqpro@123", salt));
        sysUser.setRoleCode(roleCode);
        sysUser.setRealName(realName);
        sysUser.setDisplayName(displayName);
        sysUser.setStudentNo(studentNo);
        sysUser.setCounselorNo(counselorNo);
        sysUser.setStatus(UserStatusConstants.ACTIVE);
        sysUserMapper.insert(sysUser);
        return sysUser;
    }

    private void saveStudentProfileIfAbsent(SysUser studentUser, Long counselorUserId) {
        StudentProfile existingProfile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, studentUser.getId())
                .last("limit 1"));
        if (existingProfile != null) {
            return;
        }

        StudentProfile studentProfile = new StudentProfile();
        studentProfile.setUserId(studentUser.getId());
        studentProfile.setAvatarUrl("https://example.com/avatar/student-20230001.png");
        studentProfile.setCollege("软件学院");
        studentProfile.setGrade("2023级");
        studentProfile.setGender("男");
        studentProfile.setPhone("13800000000");
        studentProfile.setEmergencyContact("张家长");
        studentProfile.setEmergencyPhone("13900000000");
        studentProfile.setCounselorUserId(counselorUserId);
        studentProfileMapper.insert(studentProfile);
    }

    private void saveCounselorRelationIfAbsent(Long counselorUserId, Long studentUserId) {
        Long count = counselorStudentMapper.selectCount(new LambdaQueryWrapper<CounselorStudent>()
                .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                .eq(CounselorStudent::getStudentUserId, studentUserId));
        if (count != null && count > 0) {
            return;
        }

        CounselorStudent counselorStudent = new CounselorStudent();
        counselorStudent.setCounselorUserId(counselorUserId);
        counselorStudent.setStudentUserId(studentUserId);
        counselorStudentMapper.insert(counselorStudent);
    }

    private void initScale() {
        MentalScale mentalScale = mentalScaleMapper.selectOne(new LambdaQueryWrapper<MentalScale>()
                .eq(MentalScale::getCode, "PHQ9")
                .last("limit 1"));
        if (mentalScale != null) {
            return;
        }

        mentalScale = new MentalScale();
        mentalScale.setCode("PHQ9");
        mentalScale.setName("PHQ-9 抑郁情绪自评量表");
        mentalScale.setDescription("用于快速了解过去两周情绪与兴趣状态变化。");
        mentalScale.setIntroduction("请根据过去两周的真实感受作答，结果仅用于辅助评估，不作为医学诊断。");
        mentalScale.setTotalQuestions(9);
        mentalScale.setPageSize(3);
        mentalScale.setLowThreshold(0);
        mentalScale.setMediumThreshold(10);
        mentalScale.setHighThreshold(15);
        mentalScale.setStatus("ACTIVE");
        mentalScaleMapper.insert(mentalScale);

        List<String> questions = List.of(
                "做事时提不起劲或没有兴趣",
                "感到心情低落、沮丧或绝望",
                "入睡困难、睡不安稳或睡得太多",
                "感觉疲倦或没有活力",
                "食欲不振或吃太多",
                "觉得自己很糟，或觉得自己很失败",
                "对事物专注有困难",
                "动作或说话变得迟缓，或烦躁坐立不安",
                "有不如离开世界更好的念头"
        );

        for (int index = 0; index < questions.size(); index++) {
            MentalScaleQuestion question = new MentalScaleQuestion();
            question.setScaleId(mentalScale.getId());
            question.setQuestionNo(index + 1);
            question.setContent(questions.get(index));
            question.setRequiredFlag(1);
            mentalScaleQuestionMapper.insert(question);
            saveOptions(question.getId());
        }
    }

    private void initAppointmentSlots() {
        SysUser counselorUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, "teacher01")
                .last("limit 1"));
        if (counselorUser == null) {
            return;
        }

        Long count = consultAppointmentSlotMapper.selectCount(new LambdaQueryWrapper<ConsultAppointmentSlot>()
                .eq(ConsultAppointmentSlot::getCounselorUserId, counselorUser.getId()));
        LocalDateTime now = LocalDateTime.now();
        Long activeCount = consultAppointmentSlotMapper.selectCount(new LambdaQueryWrapper<ConsultAppointmentSlot>()
                .eq(ConsultAppointmentSlot::getCounselorUserId, counselorUser.getId())
                .eq(ConsultAppointmentSlot::getStatus, AppointmentConstants.SLOT_OPEN)
                .le(ConsultAppointmentSlot::getStartTime, now.plusMinutes(5))
                .ge(ConsultAppointmentSlot::getEndTime, now.plusMinutes(20)));
        if (activeCount == null || activeCount == 0) {
            saveAppointmentSlot(counselorUser.getId(), now.minusMinutes(10).withSecond(0).withNano(0), now.plusMinutes(40).withSecond(0).withNano(0));
        }
        if (count != null && count > 0) {
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
        slot.setStatus(AppointmentConstants.SLOT_OPEN);
        consultAppointmentSlotMapper.insert(slot);
    }

    private void saveOptions(Long questionId) {
        insertOption(questionId, "A", "完全没有", 0, 1);
        insertOption(questionId, "B", "好几天", 1, 2);
        insertOption(questionId, "C", "一半以上时间", 2, 3);
        insertOption(questionId, "D", "几乎每天", 3, 4);
    }

    private void insertOption(Long questionId, String optionCode, String content, Integer score, Integer sortNo) {
        MentalScaleOption option = new MentalScaleOption();
        option.setQuestionId(questionId);
        option.setOptionCode(optionCode);
        option.setContent(content);
        option.setScore(score);
        option.setSortNo(sortNo);
        mentalScaleOptionMapper.insert(option);
    }

    private void initResources() {
        if (mentalResourceMapper.selectCount(null) != null && mentalResourceMapper.selectCount(null) > 0) {
            return;
        }

        ResourceCategory relaxCategory = saveCategoryIfAbsent("情绪舒缓", "帮助学生进行放松、减压和情绪稳定", 1);
        ResourceCategory growthCategory = saveCategoryIfAbsent("自助成长", "帮助学生建立习惯、提升自我认知与求助意识", 2);

        ResourceTag breathTag = saveTagIfAbsent("呼吸训练", "短时放松训练");
        ResourceTag sleepTag = saveTagIfAbsent("睡眠", "睡眠卫生与改善方法");
        ResourceTag stressTag = saveTagIfAbsent("学业压力", "学习与考试压力调节");

        MentalResource breathingResource = saveResourceIfAbsent(
                relaxCategory.getId(),
                "三分钟呼吸放松",
                "通过短时呼吸练习帮助学生快速降低紧张感。",
                "AUDIO",
                "https://example.com/resources/breathing",
                "https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=800&q=80",
                List.of(breathTag.getId(), stressTag.getId()));
        saveResourceIfAbsent(
                growthCategory.getId(),
                "睡眠习惯自查清单",
                "从作息、手机使用和环境布置三个方面帮助学生建立睡眠卫生习惯。",
                "ARTICLE",
                "https://example.com/resources/sleep-checklist",
                "https://images.unsplash.com/photo-1495195134817-aeb325a55b65?auto=format&fit=crop&w=800&q=80",
                List.of(sleepTag.getId()));
        saveResourceIfAbsent(
                growthCategory.getId(),
                "考试周减压工具包",
                "整合时间切分、情绪记录和求助提醒三个模块，帮助学生在考试周保持节奏。",
                "LINK",
                "https://example.com/resources/exam-pressure",
                "https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&w=800&q=80",
                List.of(stressTag.getId()));

        breathingResource.getId();//?
    }

    private ResourceCategory saveCategoryIfAbsent(String name, String description, int sortNo) {
        ResourceCategory category = resourceCategoryMapper.selectOne(new LambdaQueryWrapper<ResourceCategory>()
                .eq(ResourceCategory::getName, name)
                .last("limit 1"));
        if (category != null) {
            return category;
        }
        category = new ResourceCategory();
        category.setName(name);
        category.setDescription(description);
        category.setSortNo(sortNo);
        category.setStatus(ResourceConstants.CATEGORY_ACTIVE);
        resourceCategoryMapper.insert(category);
        return category;
    }

    private ResourceTag saveTagIfAbsent(String name, String description) {
        ResourceTag tag = resourceTagMapper.selectOne(new LambdaQueryWrapper<ResourceTag>()
                .eq(ResourceTag::getName, name)
                .last("limit 1"));
        if (tag != null) {
            return tag;
        }
        tag = new ResourceTag();
        tag.setName(name);
        tag.setDescription(description);
        resourceTagMapper.insert(tag);
        return tag;
    }

    private MentalResource saveResourceIfAbsent(Long categoryId,
                                                String title,
                                                String summaryText,
                                                String resourceType,
                                                String contentUrl,
                                                String coverUrl,
                                                List<Long> tagIds) {
        MentalResource resource = mentalResourceMapper.selectOne(new LambdaQueryWrapper<MentalResource>()
                .eq(MentalResource::getTitle, title)
                .last("limit 1"));
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
        resource.setStatus(ResourceConstants.RESOURCE_PUBLISHED);
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
}
