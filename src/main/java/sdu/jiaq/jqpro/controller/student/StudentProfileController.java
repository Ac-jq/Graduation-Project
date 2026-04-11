package sdu.jiaq.jqpro.controller.student;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.student.StudentProfileResponse;
import sdu.jiaq.jqpro.dto.student.UpdateStudentProfileRequest;
import sdu.jiaq.jqpro.service.StudentProfileService;

/**
 * 学生档案接口。
 */
@RestController
@RequestMapping("/api/student/profile")
@SaCheckRole(RoleConstants.STUDENT)
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping("/me")
    public Result<StudentProfileResponse> getCurrentProfile() {
        return Result.success(studentProfileService.getCurrentStudentProfile());
    }

    @PutMapping("/me")
    public Result<StudentProfileResponse> updateCurrentProfile(@Valid @RequestBody UpdateStudentProfileRequest request) {
        return Result.success("学生档案更新成功", studentProfileService.updateCurrentStudentProfile(request));
    }
}
