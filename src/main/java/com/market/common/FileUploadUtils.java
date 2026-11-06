package com.market.common;

import com.market.config.FileUploadConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传工具类
 * 负责处理用户头像、商品图片等文件的上传，并返回可访问的相对路径。
 * 文件存储到本地磁盘，按日期分子目录，避免单个文件夹文件过多，同时便于管理。
 */
@Component  // 注册为Spring Bean，方便全局使用
public class FileUploadUtils {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    @Autowired
    private FileUploadConfig fileUploadConfig; // 注入文件上传的配置参数

    /**
     * 上传头像文件
     *
     * @param file 前端传来的MultipartFile对象（用户选择的头像图片）
     * @return 可访问的相对路径，例如：/uploads/avatar/2024-06-06/uuid.png
     *         这个路径会被前端直接拼上域名或通过Spring静态资源映射访问
     * @throws IOException 文件保存过程中可能出现的IO异常
     * @throws RuntimeException 若上传的文件为空，直接抛出运行时异常
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        return saveImage(file, fileUploadConfig.getAvatarPath());
    }

    /**
     * 上传商品图片，返回可访问的相对路径，例如 /uploads/product/2024-06-06/uuid.png
     */
    public String uploadProductImage(MultipartFile file) throws IOException {
        return saveImage(file, "product");
    }

    private String saveImage(MultipartFile file, String category) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("图片大小不能超过5MB");
        }
        String suffix = IMAGE_EXTENSIONS.get(file.getContentType());
        if (suffix == null) {
            throw new RuntimeException("仅支持 JPG、PNG 或 WebP 图片");
        }

        String dateDir = LocalDate.now().toString();
        File dir = new File(fileUploadConfig.getUploadPath() + File.separator + category + File.separator + dateDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建上传目录");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + suffix;
        file.transferTo(new File(dir, filename));
        return fileUploadConfig.getStaticUrl() + "/" + category + "/" + dateDir + "/" + filename;
    }
}
