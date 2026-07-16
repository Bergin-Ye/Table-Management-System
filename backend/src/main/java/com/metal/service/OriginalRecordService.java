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
import com.metal.entity.OriginalRecord;
import com.metal.mapper.OriginalRecordMapper;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    // =============== Excel 导入 ===============
    private static final int IMPORT_BATCH_SIZE = 500;

    /**
     * 批量导入 Excel 数据
     * 采用分批 INSERT + 事务保护：每 500 条一批，中途失败自动回滚整批
     */
    @Transactional
    public ImportResultDTO importExcel(MultipartFile file, Long companyId) {
        List<ImportResultDTO.FailDetail> failDetails = new ArrayList<>();
        List<OriginalRecord> batch = new ArrayList<>(IMPORT_BATCH_SIZE);
        int[] counts = {0, 0, 0}; // total, success, fail

        try (InputStream is = file.getInputStream()) {
            EasyExcel.read(is, OriginalRecord.class, new AnalysisEventListener<OriginalRecord>() {
                @Override
                public void invoke(OriginalRecord data, AnalysisContext ctx) {
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

    private void flushBatch(List<OriginalRecord> batch, int[] counts) {
        mapper.batchInsert(batch);
        counts[1] += batch.size();
        batch.clear();
    }

    // =============== Excel 导出 ===============
    public void exportExcel(HttpServletResponse response, Long companyId, String keyword,
                            String shift, String factory,
                            String isOutOfWarranty, String startDate, String endDate) {
        try {
            PageHelper.startPage(1, 0);
            List<OriginalRecord> list = mapper.search(companyId, keyword, shift, factory, isOutOfWarranty,
                    startDate, endDate, "id", "desc");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = URLEncoder.encode("原始记录导出.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            OutputStream os = response.getOutputStream();
            EasyExcel.write(os, OriginalRecord.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("原始记录")
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
            String fileName = URLEncoder.encode("原始记录导入模板.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            OriginalRecord template = new OriginalRecord();
            template.setYearMonth("FY2607");
            template.setRecordDate(LocalDate.now());
            template.setShift("白班");
            template.setFactory("示例厂房");
            template.setSerialNumber("示例序号");
            template.setMachineNo("示例机台号");
            template.setDiagnostician("示例诊断人");
            template.setRepairPerson("示例维修人");
            template.setMachineModel("示例机型");
            template.setFaultPhenomenon("示例故障现象");
            template.setFaultDescription("示例故障描述");
            template.setMaterialCode("示例物料编码");
            template.setPartName("示例零件名称");
            template.setQuantity(1);
            template.setMachineOnMaterial("示例上机物料");
            template.setMachineOffMaterial("示例下机物料");
            template.setRemark("示例备注");
            template.setConfirmer("示例确认人");
            template.setDeliveryRecordRef("示例送货记录引用");

            List<OriginalRecord> list = List.of(template);
            OutputStream os = response.getOutputStream();
            EasyExcel.write(os, OriginalRecord.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("原始记录")
                    .doWrite(list);
            os.flush();
        } catch (IOException e) {
            throw new BizException("模板下载失败: " + e.getMessage());
        }
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
