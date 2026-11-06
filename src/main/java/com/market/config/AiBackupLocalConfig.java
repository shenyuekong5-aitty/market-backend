package com.market.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * 可选的本机备用 AI 配置。文件放在后端工作目录，不纳入 Git，也不会打进 jar。
 * 运行时工作目录应为后端目录；其他部署方式可改用环境变量。
 */
@Configuration
@PropertySource(value = "file:./ai-backup.local.properties", ignoreResourceNotFound = true)
public class AiBackupLocalConfig {
}
