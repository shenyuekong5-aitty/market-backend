package com.market.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Getter
@Configuration
public class FileUploadConfig {

    @Value("${file.upload.path:./uploads}")      // 改为 path
    private String uploadPath;

    @Value("${file.upload.avatar-path:avatar}")
    private String avatarPath;

    @Value("${file.upload.static-url:/uploads}")
    private String staticUrl;
}