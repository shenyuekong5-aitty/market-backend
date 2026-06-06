package com.market.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private FileUploadConfig fileUploadConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + fileUploadConfig.getUploadPath() + "/");
    }
}

//作用：把浏览器访问的 /uploads/** 路径，映射到硬盘上的 ./uploads/ 目录。

//例如，浏览器请求 http://localhost:8088/uploads/avatar/2024-06-06/abc.png 时，
// Spring Boot 会自动去 ./uploads/avatar/2024-06-06/abc.png 读取文件并返回。