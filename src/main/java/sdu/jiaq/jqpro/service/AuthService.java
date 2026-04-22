package sdu.jiaq.jqpro.service;

import jakarta.servlet.http.HttpServletRequest;
import sdu.jiaq.jqpro.dto.auth.ChangePasswordRequest;
import sdu.jiaq.jqpro.dto.auth.CurrentUserResponse;
import sdu.jiaq.jqpro.dto.auth.LoginRequest;
import sdu.jiaq.jqpro.dto.auth.LoginResponse;
import sdu.jiaq.jqpro.dto.auth.RegisterRequest;

/**
 * 认证服务。
 */
public interface AuthService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest);

    CurrentUserResponse register(RegisterRequest request, HttpServletRequest httpServletRequest);

    void logout(HttpServletRequest httpServletRequest);

    void changePassword(ChangePasswordRequest request, HttpServletRequest httpServletRequest);

    CurrentUserResponse getCurrentUser();
}
