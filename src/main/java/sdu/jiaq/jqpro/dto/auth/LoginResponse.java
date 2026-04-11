package sdu.jiaq.jqpro.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录响应。
 */
@Data
@Builder
public class LoginResponse {

    private String token;

    private Long userId;

    private String account;

    private String roleCode;

    private String displayName;

    private List<String> roles;
}
