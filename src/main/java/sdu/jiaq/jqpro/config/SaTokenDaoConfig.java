package sdu.jiaq.jqpro.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Sa-Token 存储配置。
 * 本地开发默认使用内存存储，避免 Redis 未就绪时阻断基础闭环验证。
 */
@Configuration
public class SaTokenDaoConfig {

    @Bean
    @Primary
    public SaTokenDao saTokenDao() {
        return new SaTokenDaoDefaultImpl();
    }
}
