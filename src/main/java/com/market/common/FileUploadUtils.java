package com.market.common;

import com.market.config.FileUploadConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 文件上传工具类
 * 负责处理用户头像、商品图片等文件的上传，并返回可访问的相对路径。
 * 文件存储到本地磁盘，按日期分子目录，避免单个文件夹文件过多，同时便于管理。
 */
@Component  // 注册为Spring Bean，方便全局使用
public class FileUploadUtils {

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
        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件为空");
        }

        // 2. 以当前日期生成子目录名，例如 "2024-06-06"
        String dateDir = LocalDate.now().toString();

        // 3. 构建文件保存的完整目录路径
        // 配置中的 uploadPath 通常是根路径，如 "./uploads"
        // avatarPath 是头像专用子目录，如 "avatar"
        // 最终路径示例：./uploads/avatar/2024-06-06/
        String dirPath = fileUploadConfig.getUploadPath() + File.separator
                + fileUploadConfig.getAvatarPath() + File.separator + dateDir;
        File dir = new File(dirPath);
        // 若目录不存在则创建（mkdirs会创建所有不存在的父目录）
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 4. 生成唯一文件名，避免重名覆盖
        String originalFilename = file.getOriginalFilename();
        // 获取原始文件后缀，若原始文件名为空则默认 .png
        String suffix = originalFilename != null
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
        // 用UUID生成随机名（去掉横线），加上原后缀，保证唯一性
        String filename = UUID.randomUUID().toString().replace("-", "") + suffix;

        // 5. 创建目标文件对象并保存
        File dest = new File(dir, filename);
        file.transferTo(dest); // Spring MultipartFile的方法，将文件写入磁盘

        // 6. 拼接返回给前端的相对路径
        // staticUrl 通常配置为 "/uploads"（与Spring资源映射一致）
        // 路径组成：/uploads/avatar/2024-06-06/uuid.png
        // 注意：实际物理路径的 "./uploads" 前缀被去掉，替换为可URL访问的静态路径
        return fileUploadConfig.getStaticUrl() + "/" + fileUploadConfig.getAvatarPath()
                + "/" + dateDir + "/" + filename;
    }
}