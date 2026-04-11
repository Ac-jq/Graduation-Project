package sdu.jiaq.jqpro.security;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.SysUserMapper;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色提供器。
 * 当前阶段只实现角色校验，权限列表预留为空集合。
 */
@Component
public class SaTokenPermissionProvider implements StpInterface {

    private final SysUserMapper sysUserMapper;

    public SaTokenPermissionProvider(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(String.valueOf(loginId));
        SysUser sysUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getRoleCode)
                .eq(SysUser::getId, userId));
        if (sysUser == null) {
            return Collections.emptyList();
        }
        return List.of(sysUser.getRoleCode());
    }
}
