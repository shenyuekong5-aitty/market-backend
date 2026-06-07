package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.OperationLog;
import java.time.LocalDateTime;
import java.util.List;

public interface OperationLogService extends IService<OperationLog> {
    List<OperationLog> listByAdminAndTime(Long adminId, LocalDateTime start, LocalDateTime end);
    void saveLog(Long adminId, String type, String description);
}