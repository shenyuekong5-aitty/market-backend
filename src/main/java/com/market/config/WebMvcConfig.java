package com.market.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private FileUploadConfig fileUploadConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = Path.of(fileUploadConfig.getUploadPath())
                .toAbsolutePath().normalize().toUri().toString();
        if (!uploadLocation.endsWith("/")) uploadLocation += "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}

//作用：把浏览器访问的 /uploads/** 路径，映射到配置的磁盘目录。

//例如，浏览器请求 http://localhost:8088/uploads/avatar/2024-06-06/abc.png 时，
// Spring Boot 会从 uploadPath/avatar/2024-06-06/abc.png 读取文件并返回。
