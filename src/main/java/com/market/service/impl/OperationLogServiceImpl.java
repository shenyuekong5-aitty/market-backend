package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.OperationLog;
import com.market.mapper.OperationLogMapper;
import com.market.service.OperationLogService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public List<OperationLog> listByAdminAndTime(Long adminId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getAdminId, adminId);
        if (start != null) {
            wrapper.ge(OperationLog::getCreateTime, start);
        }
        if (end != null) {
            wrapper.le(OperationLog::getCreateTime, end);
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void saveLog(Long adminId, String type, String description) {
        OperationLog log = new OperationLog();
        log.setAdminId(adminId);
        log.setType(type);
        log.setDescription(description);
        log.setCreateTime(LocalDateTime.now());
        baseMapper.insert(log);
    }
}