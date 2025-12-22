package com.winter.labforgeai.controller;

import cn.hutool.core.io.FileUtil;
import com.winter.labforgeai.common.BaseResponse;
import com.winter.labforgeai.common.ResultUtils;
import com.winter.labforgeai.exception.BusinessException;
import com.winter.labforgeai.exception.ErrorCode;
import com.winter.labforgeai.exception.ThrowUtils;
import com.winter.labforgeai.manager.CosManager;
import com.winter.labforgeai.model.entity.User;
import com.winter.labforgeai.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传 控制层
 *
 * @author <a href="https://github.com/LuWenTao529">Winter</a>
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    /**
     * 允许上传的图片类型
     */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /**
     * 最大文件大小（5MB）
     */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传用户头像
     *
     * @param file    头像文件
     * @param request HTTP请求
     * @return 头像访问URL
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 校验文件
        validateFile(file);
        
        // 获取文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        
        // 生成 COS 对象键：/avatar/{userId}/{date}/{uuid}.{suffix}
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "." + suffix;
        String cosKey = String.format("/avatar/%d/%s/%s", loginUser.getId(), datePath, fileName);
        
        // 创建临时文件
        File tempFile = null;
        try {
            tempFile = File.createTempFile("avatar_", "." + suffix);
            file.transferTo(tempFile);
            
            // 上传到 COS
            String avatarUrl = cosManager.uploadFile(cosKey, tempFile);
            ThrowUtils.throwIf(avatarUrl == null, ErrorCode.SYSTEM_ERROR, "头像上传失败");
            
            log.info("用户 {} 上传头像成功: {}", loginUser.getId(), avatarUrl);
            return ResultUtils.success(avatarUrl);
        } catch (IOException e) {
            log.error("头像上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                FileUtil.del(tempFile);
            }
        }
    }

    /**
     * 校验上传的文件
     *
     * @param file 上传的文件
     */
    private void validateFile(MultipartFile file) {
        // 校验文件是否为空
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        
        // 校验文件大小
        ThrowUtils.throwIf(file.getSize() > MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5MB");
        
        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(!ALLOWED_IMAGE_TYPES.contains(suffix.toLowerCase()), 
                ErrorCode.PARAMS_ERROR, "不支持的图片格式，仅支持：" + String.join(", ", ALLOWED_IMAGE_TYPES));
    }
}
