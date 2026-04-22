package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.adminuser.AdminUserSummaryResponse;
import sdu.jiaq.jqpro.dto.adminuser.CreateAdminUserRequest;
import sdu.jiaq.jqpro.dto.adminuser.CreateCounselorRequest;
import sdu.jiaq.jqpro.dto.adminuser.UpdateAdminUserRequest;

import java.util.List;

/**
 * Admin user service.
 */
public interface AdminUserService {

    List<AdminUserSummaryResponse> listUsers(String roleCode, String status, String keyword, String grade, String college);

    AdminUserSummaryResponse createUser(CreateAdminUserRequest request);

    AdminUserSummaryResponse createCounselor(CreateCounselorRequest request);

    AdminUserSummaryResponse updateUser(Long userId, UpdateAdminUserRequest request);

    AdminUserSummaryResponse enableUser(Long userId);

    AdminUserSummaryResponse disableUser(Long userId);

    void resetPassword(Long userId);

    void deleteUser(Long userId);
}
