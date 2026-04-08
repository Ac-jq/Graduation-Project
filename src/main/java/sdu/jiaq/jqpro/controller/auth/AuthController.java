package sdu.jiaq.jqpro.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.auth.ChangePasswordRequest;
import sdu.jiaq.jqpro.dto.auth.CurrentUserResponse;
import sdu.jiaq.jqpro.dto.auth.LoginRequest;
import sdu.jiaq.jqpro.dto.auth.LoginResponse;
import sdu.jiaq.jqpro.service.AuthService;

/**
 * 认证接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return Result.success("登录成功", authService.login(request, httpServletRequest));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpServletRequest) {
        authService.logout(httpServletRequest);
        return Result.success("登出成功", null);
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                       HttpServletRequest httpServletRequest) {
        authService.changePassword(request, httpServletRequest);
        return Result.success("密码修改成功", null);
    }

    @GetMapping("/current-user")
    public Result<CurrentUserResponse> currentUser() {
        return Result.success(authService.getCurrentUser());
    }
}
