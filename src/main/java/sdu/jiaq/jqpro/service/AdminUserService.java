package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.adminuser.AdminUserSummaryResponse;
import sdu.jiaq.jqpro.dto.adminuser.CreateCounselorRequest;

import java.util.List;

/**
 * Admin user service.
 */
public interface AdminUserService {

    List<AdminUserSummaryResponse> listUsers(String roleCode, String status, String keyword);

    AdminUserSummaryResponse createCounselor(CreateCounselorRequest request);

    AdminUserSummaryResponse enableUser(Long userId);

    AdminUserSummaryResponse disableUser(Long userId);

    void resetPassword(Long userId);
}
