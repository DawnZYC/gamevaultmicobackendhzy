package com.sg.nusiss.gamevaultmicobackendhzy.config.forum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.sg.nusiss.gamevaultmicobackendhzy.interceptor.forum.ForumAuthInterceptor;

@Configuration
public class ForumWebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ForumAuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/forum/**")  // 拦截所有论坛 API
                .excludePathPatterns(
                        "/api/forum/auth/**",        // 认证相关不拦截（login, register, me等）
                        "/api/test/**"               // 测试接口不拦截
                );
    }
}