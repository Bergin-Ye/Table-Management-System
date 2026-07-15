package com.metal.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.metal.common.BizException;
import com.metal.common.PageResult;
import com.metal.common.ServiceHelper;
import com.metal.entity.OriginalRecord;
import com.metal.mapper.OriginalRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OriginalRecordService {

    @Autowired
    private OriginalRecordMapper mapper;

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("'FY'yyMM");

    public PageResult<OriginalRecord> query(int page, int pageSize, Long companyId, String keyword,
                                             String shift, String factory,
                                             String isOutOfWarranty, String startDate, String endDate,
                                             String sortField, String sortOrder) {
        sortField = ServiceHelper.sanitizeSortField(sortField, "id");
        sortOrder = ServiceHelper.sanitizeSortOrder(sortOrder);
        PageHelper.startPage(page, pageSize);
        List<OriginalRecord> list = mapper.search(companyId, keyword, shift, factory, isOutOfWarranty,
                startDate, endDate, sortField, sortOrder);
        PageInfo<OriginalRecord> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, pageSize, list);
    }

    public OriginalRecord getById(Long id) {
        OriginalRecord r = mapper.findById(id);
        if (r == null) throw new BizException("记录不存在");
        return r;
    }

    @Transactional
    public OriginalRecord create(OriginalRecord record) {
        applyCalculations(record);
        String user = ServiceHelper.getCurrentUserName();
        record.setCreatedBy(user);
        record.setUpdatedBy(user);
        mapper.insert(record);
        return record;
    }

    @Transactional
    public OriginalRecord update(OriginalRecord record) {
        getById(record.getId());
        applyCalculations(record);
        record.setUpdatedBy(ServiceHelper.getCurrentUserName());
        mapper.update(record);
        return record;
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BizException("请选择要删除的记录");
        mapper.batchDelete(ids);
    }

    public OriginalRecord copy(Long id) {
        return getById(id);
    }

    // =============== 自动计算 ===============
    private void applyCalculations(OriginalRecord record) {
        // 年+月
        if (record.getRecordDate() != null) {
            record.setYearMonth(record.getRecordDate().format(YM_FMT));
        }
        // 维修工时 = 结束时间 - 开始时间 (小时)
        if (record.getStartTime() != null && record.getEndTime() != null) {
            long minutes = Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
            record.setRepairHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        }
        // 停机工时 = 结束时间 - 报修时间 (小时)
        if (record.getRepairRequestTime() != null && record.getEndTime() != null) {
            long minutes = Duration.between(record.getRepairRequestTime(), record.getEndTime()).toMinutes();
            record.setDowntimeHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        }
        // 上次上机时间: 查询下机物料号上一次在上机物料列出现的日期
        if (record.getMachineOffMaterial() != null && !record.getMachineOffMaterial().isBlank()) {
            LocalDate lastTime = mapper.findLastMachineOnTime(record.getMachineOffMaterial());
            record.setLastMachineOnTime(lastTime);
            // 是否过保
            if (lastTime != null) {
                long months = Duration.between(lastTime.atStartOfDay(), LocalDate.now().atStartOfDay()).toDays() / 30;
                record.setIsOutOfWarranty(months >= 6 ? "已过保" : "未过保");
            } else {
                record.setIsOutOfWarranty("无");
            }
        } else {
            record.setIsOutOfWarranty("无");
        }
    }
}
