package com.market.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Getter
@Configuration
public class FileUploadConfig {

    // 磁盘存储目录：可配置绝对路径；默认相对于后端启动目录。
    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    // 存储目录下的子目录名，不以斜杠开头。
    @Value("${file.upload.avatar-path:avatar}")
    private String avatarPath;

    // 浏览器访问的 URL 前缀，以 / 开头；不是磁盘路径。
    @Value("${file.upload.static-url:/uploads}")
    private String staticUrl;
}
