package com.example.gamerecord.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web 配置：CORS 跨域、登录拦截器、静态资源（上传文件）映射
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /** 跨域配置：允许本地前端开发服务器访问 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    /** 登录拦截（登录、注册、静态资源除外） */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/login", "/api/user/register");
    }

    /** 上传文件访问映射：/uploads/xxx.jpg -&gt; 项目根目录/uploads/xxx.jpg */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保上传目录存在
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IllegalStateException("无法创建上传目录: " + dir.getAbsolutePath());
            }
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + dir.getAbsolutePath() + File.separator);
    }
}
