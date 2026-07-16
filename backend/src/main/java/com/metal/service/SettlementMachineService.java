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
import com.metal.entity.SettlementMachine;
import com.metal.mapper.SettlementMachineMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class SettlementMachineService {

    @Autowired
    private SettlementMachineMapper mapper;

    public PageResult<SettlementMachine> query(int page, int pageSize, Long companyId, String keyword,
                                                String machineModel, String sortField, String sortOrder) {
        sortField = ServiceHelper.sanitizeSortField(sortField, "id");
        sortOrder = ServiceHelper.sanitizeSortOrder(sortOrder);
        PageHelper.startPage(page, pageSize);
        List<SettlementMachine> list = mapper.search(companyId, keyword, machineModel, sortField, sortOrder);
        PageInfo<SettlementMachine> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, pageSize, list);
    }

    public SettlementMachine getById(Long id) {
        SettlementMachine r = mapper.findById(id);
        if (r == null) throw new BizException("记录不存在");
        return r;
    }

    @Transactional
    public SettlementMachine create(SettlementMachine record) {
        String user = ServiceHelper.getCurrentUserName();
        record.setCreatedBy(user);
        record.setUpdatedBy(user);
        mapper.insert(record);
        return record;
    }

    @Transactional
    public SettlementMachine update(SettlementMachine record) {
        SettlementMachine exist = getById(record.getId());
        ServiceHelper.checkOwnershipOrAdmin(exist.getCreatedBy(), "编辑");
        record.setUpdatedBy(ServiceHelper.getCurrentUserName());
        mapper.update(record);
        return record;
    }

    @Transactional
    public void delete(Long id) {
        SettlementMachine exist = getById(id);
        ServiceHelper.checkOwnershipOrAdmin(exist.getCreatedBy(), "删除");
        mapper.deleteById(id);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BizException("请选择要删除的记录");
        if (!ServiceHelper.isAdmin()) {
            for (Long id : ids) {
                SettlementMachine exist = getById(id);
                ServiceHelper.checkOwnershipOrAdmin(exist.getCreatedBy(), "删除");
            }
        }
        mapper.batchDelete(ids);
    }

    // =============== Excel 导入 ===============
    private static final int IMPORT_BATCH_SIZE = 500;

    @Transactional
    public ImportResultDTO importExcel(MultipartFile file, Long companyId) {
        List<ImportResultDTO.FailDetail> failDetails = new ArrayList<>();
        List<SettlementMachine> batch = new ArrayList<>(IMPORT_BATCH_SIZE);
        int[] counts = {0, 0, 0}; // total, success, fail

        try (InputStream is = file.getInputStream()) {
            EasyExcel.read(is, SettlementMachine.class, new AnalysisEventListener<SettlementMachine>() {
                @Override
                public void invoke(SettlementMachine data, AnalysisContext ctx) {
                    counts[0]++;
                    try {
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

    private void flushBatch(List<SettlementMachine> batch, int[] counts) {
        mapper.batchInsert(batch);
        counts[1] += batch.size();
        batch.clear();
    }

    // =============== Excel 导出 ===============
    public void exportExcel(HttpServletResponse response, Long companyId, String keyword, String machineModel) {
        try {
            PageHelper.startPage(1, 0);
            List<SettlementMachine> list = mapper.search(companyId, keyword, machineModel, "id", "desc");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = URLEncoder.encode("结算机台导出.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            OutputStream os = response.getOutputStream();
            EasyExcel.write(os, SettlementMachine.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("结算机台")
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
            String fileName = URLEncoder.encode("结算机台导入模板.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            SettlementMachine template = new SettlementMachine();
            template.setMaterialCode("示例编码");
            template.setCategory("示例类别");
            template.setPartName("示例零件");
            template.setUnitUsage(java.math.BigDecimal.ONE);
            template.setRatio(java.math.BigDecimal.ONE);
            template.setUnitPriceWithTax(java.math.BigDecimal.ZERO);
            template.setWarrantyPeriod("示例质保期");
            template.setPriceType("示例价格类型");
            template.setRemark("示例备注");
            template.setMachineModel("示例机型");
            template.setSettlementMachineCount(1);

            List<SettlementMachine> list = List.of(template);
            OutputStream os = response.getOutputStream();
            EasyExcel.write(os, SettlementMachine.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("结算机台")
                    .doWrite(list);
            os.flush();
        } catch (IOException e) {
            throw new BizException("模板下载失败: " + e.getMessage());
        }
    }
}
