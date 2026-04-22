package sdu.jiaq.jqpro.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.adminuser.AdminUserSummaryResponse;
import sdu.jiaq.jqpro.dto.adminuser.CreateAdminUserRequest;
import sdu.jiaq.jqpro.dto.adminuser.CreateCounselorRequest;
import sdu.jiaq.jqpro.dto.adminuser.UpdateAdminUserRequest;
import sdu.jiaq.jqpro.service.AdminUserService;

import java.util.List;

/**
 * Admin user management controller.
 */
@RestController
@RequestMapping("/api/admin/users")
@SaCheckRole(RoleConstants.ADMIN)
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Result<List<AdminUserSummaryResponse>> listUsers(@RequestParam(required = false) String roleCode,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String grade,
                                                            @RequestParam(required = false) String college) {
        return Result.success(adminUserService.listUsers(roleCode, status, keyword, grade, college));
    }

    @PostMapping
    public Result<AdminUserSummaryResponse> createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        return Result.success("用户创建成功", adminUserService.createUser(request));
    }

    @PostMapping("/counselors")
    public Result<AdminUserSummaryResponse> createCounselor(@Valid @RequestBody CreateCounselorRequest request) {
        return Result.success("咨询师创建成功", adminUserService.createCounselor(request));
    }

    @PutMapping("/{userId}")
    public Result<AdminUserSummaryResponse> updateUser(@PathVariable Long userId,
                                                       @Valid @RequestBody UpdateAdminUserRequest request) {
        return Result.success("用户更新成功", adminUserService.updateUser(userId, request));
    }

    @PostMapping("/{userId}/enable")
    public Result<AdminUserSummaryResponse> enableUser(@PathVariable Long userId) {
        return Result.success("用户已启用", adminUserService.enableUser(userId));
    }

    @PostMapping("/{userId}/disable")
    public Result<AdminUserSummaryResponse> disableUser(@PathVariable Long userId) {
        return Result.success("用户已禁用", adminUserService.disableUser(userId));
    }

    @PostMapping("/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long userId) {
        adminUserService.resetPassword(userId);
        return Result.success("密码已重置为默认密码", null);
    }

    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return Result.success("用户已删除", null);
    }
}
