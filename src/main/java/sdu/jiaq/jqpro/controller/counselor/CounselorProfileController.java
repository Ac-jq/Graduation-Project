package sdu.jiaq.jqpro.controller.counselor;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.counselor.CounselorProfileResponse;
import sdu.jiaq.jqpro.dto.counselor.UpdateCounselorProfileRequest;
import sdu.jiaq.jqpro.service.CounselorProfileService;

/**
 * 咨询师个人资料接口。
 */
@RestController
@RequestMapping("/api/counselor/profile")
@SaCheckRole(RoleConstants.COUNSELOR)
public class CounselorProfileController {

    private final CounselorProfileService counselorProfileService;

    public CounselorProfileController(CounselorProfileService counselorProfileService) {
        this.counselorProfileService = counselorProfileService;
    }

    @GetMapping({"", "/me"})
    public Result<CounselorProfileResponse> getCurrentProfile() {
        return Result.success(counselorProfileService.getCurrentCounselorProfile());
    }

    @PutMapping({"", "/me"})
    public Result<CounselorProfileResponse> updateCurrentProfile(@Valid @RequestBody UpdateCounselorProfileRequest request) {
        return Result.success("咨询师资料更新成功", counselorProfileService.updateCurrentCounselorProfile(request));
    }
}
