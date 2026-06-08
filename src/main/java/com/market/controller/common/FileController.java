package com.market.controller.common;

import com.market.common.FileUploadUtils;
import com.market.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
public class FileController {

    @Autowired
    private FileUploadUtils fileUploadUtils;

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = fileUploadUtils.uploadAvatar(file);
            return Result.success(filePath);
        } catch (IOException e) {
            return Result.error("头像上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/product-image")
    public Result<String> uploadProductImage(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = fileUploadUtils.uploadProductImage(file);
            return Result.success(filePath);
        } catch (IOException e) {
            return Result.error("商品图片上传失败：" + e.getMessage());
        }
    }
}