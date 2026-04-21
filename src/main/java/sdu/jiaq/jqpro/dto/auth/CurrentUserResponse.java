package sdu.jiaq.jqpro.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息。
 */
@Data
@Builder
public class CurrentUserResponse {

    private Long userId;

    private String account;

    private String roleCode;

    private String realName;

    private String displayName;

    private String avatarUrl;

    private String studentNo;

    private String counselorNo;

    private List<String> roles;
}
