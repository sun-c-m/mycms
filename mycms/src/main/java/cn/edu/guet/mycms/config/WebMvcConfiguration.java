package cn.edu.guet.mycms.config;

import cn.edu.guet.mycms.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有以 /api 开头的请求
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/news/public/**",
                        "/api/news/attachments/**"
                ); // 排除登录接口和附件直连预览/下载地址
    }
}

