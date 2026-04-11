package sdu.jiaq.jqpro.controller.counselor;

import cn.dev33.satoken.annotation.SaCheckRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.counselor.CounselorStudentSummaryResponse;
import sdu.jiaq.jqpro.service.CounselorStudentService;

import java.util.List;

/**
 * Counselor student controller.
 */
@RestController
@RequestMapping("/api/counselor/students")
@SaCheckRole(RoleConstants.COUNSELOR)
public class CounselorStudentController {

    private final CounselorStudentService counselorStudentService;

    public CounselorStudentController(CounselorStudentService counselorStudentService) {
        this.counselorStudentService = counselorStudentService;
    }

    @GetMapping
    public Result<List<CounselorStudentSummaryResponse>> listStudents() {
        return Result.success(counselorStudentService.listCurrentCounselorStudents());
    }
}
