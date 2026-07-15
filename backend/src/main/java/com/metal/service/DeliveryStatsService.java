package com.metal.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.metal.common.BizException;
import com.metal.common.PageResult;
import com.metal.common.ServiceHelper;
import com.metal.entity.DeliveryStats;
import com.metal.entity.DeliveryStatsDaily;
import com.metal.mapper.DeliveryStatsDailyMapper;
import com.metal.mapper.DeliveryStatsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryStatsService {

    @Autowired
    private DeliveryStatsMapper mapper;

    @Autowired
    private DeliveryStatsDailyMapper dailyMapper;

    public PageResult<DeliveryStats> query(int page, int pageSize, Long companyId, String keyword,
                                            String category, String yearMonth,
                                            String sortField, String sortOrder) {
        sortField = ServiceHelper.sanitizeSortField(sortField, "id");
        sortOrder = ServiceHelper.sanitizeSortOrder(sortOrder);
        PageHelper.startPage(page, pageSize);
        List<DeliveryStats> list = mapper.search(companyId, keyword, category, yearMonth, sortField, sortOrder);
        PageInfo<DeliveryStats> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, pageSize, list);
    }

    public DeliveryStats getById(Long id) {
        DeliveryStats r = mapper.findById(id);
        if (r == null) throw new BizException("记录不存在");
        return r;
    }

    public List<DeliveryStatsDaily> getDailies(Long statId) {
        return dailyMapper.findByStatId(statId);
    }

    @Transactional
    public DeliveryStats create(DeliveryStats record, List<DeliveryStatsDaily> dailies) {
        applyCalculations(record);
        mapper.insert(record);
        if (dailies != null && !dailies.isEmpty()) {
            for (DeliveryStatsDaily d : dailies) {
                d.setStatId(record.getId());
            }
            dailyMapper.batchInsert(dailies);
        }
        return record;
    }

    @Transactional
    public DeliveryStats update(DeliveryStats record, List<DeliveryStatsDaily> dailies) {
        getById(record.getId());
        applyCalculations(record);
        mapper.update(record);
        // 先删后插每日明细
        dailyMapper.deleteByStatId(record.getId());
        if (dailies != null && !dailies.isEmpty()) {
            for (DeliveryStatsDaily d : dailies) {
                d.setStatId(record.getId());
            }
            dailyMapper.batchInsert(dailies);
        }
        return record;
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        dailyMapper.deleteByStatId(id);
        mapper.deleteById(id);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BizException("请选择要删除的记录");
        for (Long id : ids) {
            dailyMapper.deleteByStatId(id);
        }
        mapper.batchDelete(ids);
    }

    private void applyCalculations(DeliveryStats record) {
        // 约定比例数量 = 机台数 × 单台机用量 × 比例
        if (record.getMachineCount() != null && record.getUnitUsage() != null && record.getRatio() != null) {
            record.setAgreedRatioQuantity(
                    record.getUnitUsage()
                            .multiply(record.getRatio())
                            .multiply(BigDecimal.valueOf(record.getMachineCount()))
                            .setScale(4, RoundingMode.HALF_UP)
            );
        }
        // 超比数量合计 = 送货数量 - 当月返修
        int delivery = record.getDeliveryQuantity() != null ? record.getDeliveryQuantity() : 0;
        int repair = record.getMonthRepair() != null ? record.getMonthRepair() : 0;
        record.setExcessQuantity(BigDecimal.valueOf(delivery - repair));
        // 超比含税金额合计 = 超比数量 × 含税单价
        if (record.getExcessQuantity() != null && record.getUnitPriceWithTax() != null) {
            record.setExcessAmountWithTax(
                    record.getExcessQuantity()
                            .multiply(record.getUnitPriceWithTax())
                            .setScale(4, RoundingMode.HALF_UP)
            );
        }
    }
}
