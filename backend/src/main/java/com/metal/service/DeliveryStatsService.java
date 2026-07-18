package com.metal.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.metal.common.BizException;
import com.metal.common.PageResult;
import com.metal.common.ServiceHelper;
import com.metal.dto.ImportResultDTO;
import com.metal.entity.DeliveryStats;
import com.metal.entity.DeliveryStatsDaily;
import com.metal.mapper.DeliveryStatsDailyMapper;
import com.metal.mapper.DeliveryStatsMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryStatsService {

    @Autowired
    private DeliveryStatsMapper mapper;

    @Autowired
    private DeliveryStatsDailyMapper dailyMapper;

    @Autowired
    private com.metal.mapper.BaseMaterial156Mapper baseMaterial156Mapper;
    @Autowired
    private com.metal.mapper.DeliveryRecordMapper deliveryRecordMapper;
    @Autowired
    private com.metal.mapper.OriginalRecordMapper originalRecordMapper;
    @Autowired
    private com.metal.mapper.SettlementMachineMapper settlementMachineMapper;

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
        String user = ServiceHelper.getCurrentUserName();
        record.setCreatedBy(user);
        record.setUpdatedBy(user);
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
        DeliveryStats exist = getById(record.getId());
        ServiceHelper.checkOwnershipOrAdmin(exist.getCreatedBy(), "编辑");
        applyCalculations(record);
        record.setUpdatedBy(ServiceHelper.getCurrentUserName());
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
        DeliveryStats exist = getById(id);
        ServiceHelper.checkOwnershipOrAdmin(exist.getCreatedBy(), "删除");
        dailyMapper.deleteByStatId(id);
        mapper.deleteById(id);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BizException("请选择要删除的记录");
        if (!ServiceHelper.isAdmin()) {
            for (Long id : ids) {
                DeliveryStats exist = getById(id);
                ServiceHelper.checkOwnershipOrAdmin(exist.getCreatedBy(), "删除");
            }
        }
        for (Long id : ids) {
            dailyMapper.deleteByStatId(id);
        }
        mapper.batchDelete(ids);
    }

    // =============== Excel 导入 ===============
    private static final int IMPORT_BATCH_SIZE = 500;

    /**
     * 批量导入 Excel 数据
     */
    @Transactional
    public ImportResultDTO importExcel(MultipartFile file, Long companyId) {
        List<ImportResultDTO.FailDetail> failDetails = new ArrayList<>();
        List<DeliveryStats> batch = new ArrayList<>(IMPORT_BATCH_SIZE);
        int[] counts = {0, 0, 0}; // total, success, fail

        try (InputStream is = file.getInputStream()) {
            EasyExcel.read(is, DeliveryStats.class, new AnalysisEventListener<DeliveryStats>() {
                @Override
                public void invoke(DeliveryStats data, AnalysisContext ctx) {
                    counts[0]++;
                    try {
                        applyCalculations(data);
                        String user = ServiceHelper.getCurrentUserName();
                        data.setCompanyId(companyId != null ? companyId : 1L);
                        data.setCreatedBy(user);
                        data.setUpdatedBy(user);
                        batch.add(data);

                        if (batch.size() >= IMPORT_BATCH_SIZE) {
                            flushBatch(batch, counts);
                        }
                    } catch (Exception e) {
                        failDetails.add(new ImportResultDTO.FailDetail(counts[0], e.getMessage()));
                        counts[2]++;
                    }
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext ctx) {
                    if (!batch.isEmpty()) {
                        flushBatch(batch, counts);
                    }
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new BizException("文件读取失败: " + e.getMessage());
        }

        ImportResultDTO result = new ImportResultDTO();
        result.setTotal(counts[0]);
        result.setSuccess(counts[1]);
        result.setFail(counts[2]);
        result.setFailDetails(failDetails);
        return result;
    }

    /** 将缓冲区数据批量写入数据库 */
    private void flushBatch(List<DeliveryStats> batch, int[] counts) {
        mapper.batchInsert(batch);
        counts[1] += batch.size();
        batch.clear();
    }

    // =============== Excel 导出 ===============
    public void exportExcel(HttpServletResponse response, Long companyId, String keyword,
                            String category, String yearMonth) {
        try {
            PageHelper.startPage(1, 0); // 0 disables paging
            List<DeliveryStats> list = mapper.search(companyId, keyword, category, yearMonth, "id", "desc");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = URLEncoder.encode("送货统计导出.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            OutputStream os = response.getOutputStream();
            EasyExcel.write(os, DeliveryStats.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("送货统计")
                    .doWrite(list);
            os.flush();
        } catch (IOException e) {
            throw new BizException("导出失败: " + e.getMessage());
        }
    }

    // =============== 模板下载 ===============
    public void downloadTemplate(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = URLEncoder.encode("送货统计导入模板.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            DeliveryStats template = new DeliveryStats();
            template.setCategory("示例类别");
            template.setMaterialCode("示例编码");
            template.setSystemName("示例系统");
            template.setPartName("示例零件");
            template.setUnitUsage(BigDecimal.ONE);
            template.setRatio(BigDecimal.ONE);
            template.setUnitPriceWithTax(BigDecimal.ZERO);
            template.setMachineCount(1);
            template.setDeliveryQuantity(0);
            template.setMachineOnQuantity(0);
            template.setMonthRepair(0);
            template.setAgreedRatioQuantity(BigDecimal.ZERO);
            template.setExcessQuantity(BigDecimal.ZERO);
            template.setExcessAmountWithTax(BigDecimal.ZERO);
            template.setStatDate(LocalDate.now());
            template.setYearMonth("2026-07");

            List<DeliveryStats> list = List.of(template);
            OutputStream os = response.getOutputStream();
            EasyExcel.write(os, DeliveryStats.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("送货统计")
                    .doWrite(list);
            os.flush();
        } catch (IOException e) {
            throw new BizException("模板下载失败: " + e.getMessage());
        }
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
        // 超比含税金额合计 = (含税单价 × 超比数量) / 1.13
        if (record.getExcessQuantity() != null && record.getUnitPriceWithTax() != null) {
            record.setExcessAmountWithTax(
                    record.getUnitPriceWithTax()
                            .multiply(record.getExcessQuantity())
                            .divide(BigDecimal.valueOf(1.13), 4, RoundingMode.HALF_UP)
            );
        }
    }

    /**
     * 根据料号+日期自动查询各字段的填充值
     */
    public java.util.Map<String, Object> autoFill(String materialCode, String statDate) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (materialCode == null || materialCode.isBlank()) return result;

        // 1. 从156项表查询基础信息
        com.metal.entity.BaseMaterial156 item = baseMaterial156Mapper.findByMaterialCode(materialCode);
        if (item != null) {
            java.util.Map<String, Object> from156 = new java.util.LinkedHashMap<>();
            from156.put("category", item.getCategory());
            from156.put("systemName", item.getSystemName());
            from156.put("partName", item.getPartName());
            from156.put("unitUsage", item.getUnitUsage());
            from156.put("ratio", item.getRatio());
            from156.put("unitPriceWithTax", item.getUnitPriceWithTax());
            result.put("from156", from156);
        }

        // 2. 获取月份
        String month = "";
        if (statDate != null && !statDate.isBlank()) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(statDate);
                month = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (Exception ignored) {}
        }

        if (!month.isEmpty()) {
            // 3. 机台数（来自结算机台数）
            Integer mc = settlementMachineMapper.sumMachineCountByMaterialCodeAndMonth(materialCode, month);
            result.put("machineCount", mc != null ? mc : 0);

            // 4. 送货数量
            int dq = deliveryRecordMapper.countByMaterialCodeAndMonth(materialCode, month);
            result.put("deliveryQuantity", dq);

            // 5. 上机数量
            int moq = originalRecordMapper.countByMaterialCodeAndMonth(materialCode, month);
            result.put("machineOnQuantity", moq);

            // 6. 当月返修（未过保）
            int mr = originalRecordMapper.countRepairByMaterialCodeAndMonth(materialCode, month);
            result.put("monthRepair", mr);

            // 7. 每日送货明细
            java.util.List<java.util.Map<String, Object>> dailyCounts =
                    deliveryRecordMapper.countDailyByMaterialCodeAndMonth(materialCode, month);
            java.util.Map<Integer, Integer> dayMap = new java.util.HashMap<>();
            for (java.util.Map<String, Object> row : dailyCounts) {
                Number day = (Number) row.get("day");
                Number cnt = (Number) row.get("cnt");
                if (day != null && cnt != null) {
                    dayMap.put(day.intValue(), cnt.intValue());
                }
            }
            int daysInMonth = java.time.YearMonth.parse(month).lengthOfMonth();
            java.util.List<java.util.Map<String, Object>> dailies = new java.util.ArrayList<>();
            for (int d = 1; d <= daysInMonth; d++) {
                java.util.Map<String, Object> daily = new java.util.LinkedHashMap<>();
                daily.put("day", d);
                daily.put("count", dayMap.getOrDefault(d, 0));
                dailies.add(daily);
            }
            result.put("dailyQuantities", dailies);
        }

        return result;
    }
}
