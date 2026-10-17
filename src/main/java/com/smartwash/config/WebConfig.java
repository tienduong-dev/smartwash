package com.smartwash.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccessInterceptor accessInterceptor;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public WebConfig(AccessInterceptor accessInterceptor) {
        this.accessInterceptor = accessInterceptor;
    }

    /** /storage/** -> thư mục uploads (giống php artisan storage:link) */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/storage/**")
                .addResourceLocations(path.toUri().toString());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessInterceptor)
                .excludePathPatterns("/storage/**", "/css/**", "/js/**",
                                     "/img/**", "/fonts/**", "/favicon.ico",
                                     "/error", "/error/**");
    }
}
