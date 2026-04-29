package sdu.jiaq.jqpro.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sa-Token 全局配置。
 * 当前对 /api/** 开启登录校验，系统探活与文档接口默认放行。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> SaRouter.match("/api/**")
                        .notMatch(
                                "/api/system/**",
                                "/api/auth/login",
                                "/api/auth/register",
                                "/error",
                                "/doc.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**"
                        )
                        .check(r -> StpUtil.checkLogin())))
                .addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path legacyUserAssetDir = Paths.get(System.getProperty("user.dir"), ".local", "user-assets");
        Path uploadRootDir = Paths.get(System.getProperty("user.dir"), "uploads");
        registry.addResourceHandler("/user-assets/**")
                .addResourceLocations(legacyUserAssetDir.toUri().toString());
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadRootDir.toUri().toString());
    }
}
