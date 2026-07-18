package com.metal.scheduler;

import com.metal.entity.DeliveryStats;
import com.metal.entity.DeliveryStatsDaily;
import com.metal.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeliveryStatsScheduler {

    @Autowired
    private DeliveryStatsMapper deliveryStatsMapper;
    @Autowired
    private DeliveryStatsDailyMapper dailyMapper;
    @Autowired
    private DeliveryRecordMapper deliveryRecordMapper;
    @Autowired
    private OriginalRecordMapper originalRecordMapper;

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("'FY'yyMM");

    /**
     * 每小时整点刷新当前月份的超比统计数据
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshCurrentMonthStats() {
        LocalDate now = LocalDate.now();
        String currentMonth = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String yearMonth = now.format(YM_FMT);

        // 查询当前月份所有超比统计记录
        List<DeliveryStats> statsList = deliveryStatsMapper.findByYearMonth(yearMonth);

        for (DeliveryStats stats : statsList) {
            String materialCode = stats.getMaterialCode();
            if (materialCode == null || materialCode.isBlank()) continue;

            // 重新查询统计数据
            int deliveryQty = deliveryRecordMapper.countByMaterialCodeAndMonth(materialCode, currentMonth);
            int machineOnQty = originalRecordMapper.countByMaterialCodeAndMonth(materialCode, currentMonth);
            int repairQty = originalRecordMapper.countRepairByMaterialCodeAndMonth(materialCode, currentMonth);

            stats.setDeliveryQuantity(deliveryQty);
            stats.setMachineOnQuantity(machineOnQty);
            stats.setMonthRepair(repairQty);

            // 重新计算
            applyCalculations(stats);

            // 更新主表
            deliveryStatsMapper.update(stats);

            // 刷新每日明细
            dailyMapper.deleteByStatId(stats.getId());
            List<Map<String, Object>> dailyCounts =
                    deliveryRecordMapper.countDailyByMaterialCodeAndMonth(materialCode, currentMonth);
            Map<Integer, Integer> dayMap = new HashMap<>();
            for (Map<String, Object> row : dailyCounts) {
                Number day = (Number) row.get("day");
                Number cnt = (Number) row.get("cnt");
                if (day != null && cnt != null) dayMap.put(day.intValue(), cnt.intValue());
            }
            int daysInMonth = YearMonth.parse(currentMonth).lengthOfMonth();
            List<DeliveryStatsDaily> dailies = new ArrayList<>();
            for (int d = 1; d <= daysInMonth; d++) {
                DeliveryStatsDaily daily = new DeliveryStatsDaily();
                daily.setStatId(stats.getId());
                daily.setDayNumber(d);
                daily.setValue(BigDecimal.valueOf(dayMap.getOrDefault(d, 0)));
                dailies.add(daily);
            }
            if (!dailies.isEmpty()) dailyMapper.batchInsert(dailies);
        }
    }

    private void applyCalculations(DeliveryStats record) {
        if (record.getMachineCount() != null && record.getUnitUsage() != null && record.getRatio() != null) {
            record.setAgreedRatioQuantity(
                    record.getUnitUsage()
                            .multiply(record.getRatio())
                            .multiply(BigDecimal.valueOf(record.getMachineCount()))
                            .setScale(4, RoundingMode.HALF_UP));
        }
        int delivery = record.getDeliveryQuantity() != null ? record.getDeliveryQuantity() : 0;
        int repair = record.getMonthRepair() != null ? record.getMonthRepair() : 0;
        record.setExcessQuantity(BigDecimal.valueOf(delivery - repair));
        if (record.getExcessQuantity() != null && record.getUnitPriceWithTax() != null) {
            record.setExcessAmountWithTax(
                    record.getUnitPriceWithTax()
                            .multiply(record.getExcessQuantity())
                            .divide(BigDecimal.valueOf(1.13), 4, RoundingMode.HALF_UP));
        }
    }
}
