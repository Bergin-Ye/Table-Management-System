package com.metal.scheduler;

import com.metal.entity.DeliveryStats;
import com.metal.entity.DeliveryStatsDaily;
import com.metal.entity.SysConfig;
import com.metal.mapper.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
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
import java.util.concurrent.ScheduledFuture;

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
    @Autowired
    private SysConfigMapper sysConfigMapper;

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("'FY'yyMM");
    private static final String DEFAULT_CRON = "0 0 3 * * *";
    private static final String CONFIG_KEY = "scheduler.cron";

    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    private ScheduledFuture<?> scheduledTask;

    @PostConstruct
    public void init() {
        taskScheduler.initialize();
        taskScheduler.setPoolSize(1);
        scheduleTask();
    }

    /**
     * 读取数据库配置的 cron 表达式并调度任务
     */
    private void scheduleTask() {
        String cron = getCronFromDb();
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduledTask = taskScheduler.schedule(this::refreshCurrentMonthStats, new CronTrigger(cron));
    }

    /**
     * 重新调度（配置变更后调用）
     */
    public void reschedule() {
        scheduleTask();
    }

    /**
     * 获取当前 cron 表达式
     */
    public String getCurrentCron() {
        return getCronFromDb();
    }

    /**
     * 更新 cron 表达式并重新调度
     */
    public void updateCron(String cron) {
        sysConfigMapper.updateValue(CONFIG_KEY, cron);
        reschedule();
    }

    private String getCronFromDb() {
        try {
            SysConfig config = sysConfigMapper.findByKey(CONFIG_KEY);
            if (config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
                return config.getConfigValue();
            }
        } catch (Exception ignored) {}
        return DEFAULT_CRON;
    }

    /**
     * 刷新当前月份的超比统计数据
     */
    public void refreshCurrentMonthStats() {
        LocalDate now = LocalDate.now();
        String currentMonth = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String yearMonth = now.format(YM_FMT);

        List<DeliveryStats> statsList = deliveryStatsMapper.findByYearMonth(yearMonth, null);

        for (DeliveryStats stats : statsList) {
            String materialCode = stats.getMaterialCode();
            Long companyId = stats.getCompanyId();
            if (materialCode == null || materialCode.isBlank()) continue;

            int deliveryQty = deliveryRecordMapper.countByMaterialCodeAndMonth(materialCode, currentMonth, companyId);
            int machineOnQty = originalRecordMapper.countByMaterialCodeAndMonth(materialCode, currentMonth, companyId);
            int repairQty = originalRecordMapper.countRepairByMaterialCodeAndMonth(materialCode, currentMonth, companyId);

            stats.setDeliveryQuantity(deliveryQty);
            stats.setMachineOnQuantity(machineOnQty);
            stats.setMonthRepair(repairQty);

            applyCalculations(stats);
            deliveryStatsMapper.update(stats);

            dailyMapper.deleteByStatId(stats.getId());
            List<Map<String, Object>> dailyCounts =
                    deliveryRecordMapper.countDailyByMaterialCodeAndMonth(materialCode, currentMonth, companyId);
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
