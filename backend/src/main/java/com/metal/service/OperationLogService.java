package com.metal.service;

import com.metal.common.PageResult;
import com.metal.entity.OperationLog;
import com.metal.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    public void save(OperationLog log) {
        operationLogMapper.insert(log);
    }

    public PageResult<OperationLog> query(int page, int pageSize, Long userId,
                                           String tableName, String action,
                                           String startDate, String endDate) {
        int offset = (page - 1) * pageSize;
        var list = operationLogMapper.search(userId, tableName, action, startDate, endDate, offset, pageSize);
        long total = operationLogMapper.searchCount(userId, tableName, action, startDate, endDate);
        return new PageResult<>(total, page, pageSize, list);
    }
}
