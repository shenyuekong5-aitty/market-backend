package com.market.config;

import com.market.common.AdminOperation;
import com.market.entity.User;
import com.market.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 只在管理员写操作成功返回后记录日志；查询失败或业务异常不会产生“成功”日志。
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminOperationAspect {

    private final OperationLogService operationLogService;

    @AfterReturning(value = "@annotation(operation)", argNames = "operation")
    public void record(AdminOperation operation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) return;
        try {
            operationLogService.saveLog(user.getId(), operation.value(), operation.value());
        } catch (Exception exception) {
            // 审计属于辅助能力，不能把已成功的管理员业务改成失败响应。
            log.warn("记录管理员操作失败：type={}, adminId={}", operation.value(), user.getId());
        }
    }
}
