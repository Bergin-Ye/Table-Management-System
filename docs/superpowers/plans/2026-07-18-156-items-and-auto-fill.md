# 156项表 + 自动回填 + 定时任务 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增"156项"基础物料表，改造原始记录/超比统计/结算机台数三个模块实现智能自动回填，新增超比统计定时刷新任务。

**Architecture:** 完全沿用现有 Spring Boot 3.3 + MyBatis 注解 + Vue 3 + Element Plus 分层架构模式。后端新增实体/映射/服务/控制器四层，前端新增视图/API模块，通过 REST API 通信。所有跨表查询逻辑在后端 Service 层实现。

**Tech Stack:** Java 21, Spring Boot 3.3, MyBatis (annotation-only), MySQL 8.4, EasyExcel 3.3.4, JWT, Vue 3, Element Plus, Vite

## Global Constraints

- 所有业务表需支持 `companyId` 多租户过滤
- 排序字段需经过 `ServiceHelper.sanitizeSortField` 安全校验
- 操作人自动记录 `createdBy` / `updatedBy`（从 JWT Token 提取）
- 前端 autocomplete 使用 `el-autocomplete` 组件，遵循原始记录页面的现有模式
- 自动查询无数据时保持字段空值，不做回填
- 超比含税金额 = `(含税单价 × 超比数量) / 1.13`
- 约定比例数量 = `机台数(用户手动输入) × 单台机用量 × 比例`

---

## Phase 1: 数据库变更

### Task 1: 执行数据库迁移 SQL

**Files:**
- Create: `scripts/migration-156.sql`

**Interfaces:**
- Produces: `base_material_156` 表（含唯一索引）、`settlement_machine` 表新增 `stat_month` 列

- [ ] **Step 1: 编写迁移 SQL**

```sql
-- 新增156项表
CREATE TABLE IF NOT EXISTS `base_material_156` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `company_id` BIGINT COMMENT '公司ID',
    `category` VARCHAR(100) COMMENT '类别',
    `material_code` VARCHAR(100) NOT NULL COMMENT '料号',
    `system_name` VARCHAR(200) COMMENT '系统名称',
    `part_name` VARCHAR(200) COMMENT '配件名称',
    `unit_usage` DECIMAL(10,4) COMMENT '单台机用量',
    `ratio` DECIMAL(10,4) COMMENT '比例',
    `unit_price_with_tax` DECIMAL(12,4) COMMENT '含税单价',
    `created_by` VARCHAR(50),
    `updated_by` VARCHAR(50),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX `idx_b156_mcode` (`material_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- settlement_machine 新增统计月份字段
ALTER TABLE `settlement_machine` ADD COLUMN `stat_month` VARCHAR(7) COMMENT '统计月份 格式yyyy-MM';
```

- [ ] **Step 2: 执行 SQL**

Run: `mysql -u root -p metal_system < scripts/migration-156.sql`

- [ ] **Step 3: 验证**

Run: `mysql -u root -p metal_system -e "SHOW CREATE TABLE base_material_156; SHOW COLUMNS FROM settlement_machine;"`

Expected: 两张表结构正确，base_material_156 有 UNIQUE INDEX，settlement_machine 有 stat_month 列。

- [ ] **Step 4: Commit**

```bash
git add scripts/migration-156.sql
git commit -m "feat: add base_material_156 table + settlement_machine.stat_month"
```

---

## Phase 2: 后端 — base_material_156 模块

### Task 2: 创建 Entity

**Files:**
- Create: `backend/src/main/java/com/metal/entity/BaseMaterial156.java`

**Interfaces:**
- Produces: `BaseMaterial156` 实体类，含 8 个业务字段 + EasyExcel 注解

- [ ] **Step 1: 创建实体类**

```java
package com.metal.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BaseMaterial156 {
    @ExcelIgnore
    private Long id;
    @ExcelIgnore
    private Long companyId;
    @ExcelProperty(value = "类别", index = 0)
    private String category;
    @ExcelProperty(value = "料号", index = 1)
    private String materialCode;
    @ExcelProperty(value = "系统名称", index = 2)
    private String systemName;
    @ExcelProperty(value = "配件名称", index = 3)
    private String partName;
    @ExcelProperty(value = "单台机用量", index = 4)
    private BigDecimal unitUsage;
    @ExcelProperty(value = "比例", index = 5)
    private BigDecimal ratio;
    @ExcelProperty(value = "含税单价", index = 6)
    private BigDecimal unitPriceWithTax;
    @ExcelIgnore
    private LocalDateTime createdAt;
    @ExcelIgnore
    private LocalDateTime updatedAt;
    @ExcelIgnore
    private String createdBy;
    @ExcelIgnore
    private String updatedBy;
}
```

### Task 3: 创建 Mapper

**Files:**
- Create: `backend/src/main/java/com/metal/mapper/BaseMaterial156Mapper.java`

**Interfaces:**
- Produces: `findById`, `insert`, `update`, `deleteById`, `batchDelete`, `batchInsert`, `search`（分页列表+搜索）, `searchByKeyword`（autocomplete搜索）, `countByMaterialCode`（唯一性校验）

- [ ] **Step 1: 创建 Mapper**

```java
package com.metal.mapper;

import com.metal.entity.BaseMaterial156;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface BaseMaterial156Mapper {

    @Select("SELECT * FROM base_material_156 WHERE id = #{id}")
    BaseMaterial156 findById(Long id);

    @Select("SELECT COUNT(*) FROM base_material_156 WHERE material_code = #{materialCode}")
    int countByMaterialCode(@Param("materialCode") String materialCode);

    @Insert("INSERT INTO base_material_156 (company_id, category, material_code, system_name, part_name, " +
            "unit_usage, ratio, unit_price_with_tax, created_by, updated_by) " +
            "VALUES (#{companyId}, #{category}, #{materialCode}, #{systemName}, #{partName}, " +
            "#{unitUsage}, #{ratio}, #{unitPriceWithTax}, #{createdBy}, #{updatedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BaseMaterial156 record);

    @Update("UPDATE base_material_156 SET category=#{category}, material_code=#{materialCode}, " +
            "system_name=#{systemName}, part_name=#{partName}, unit_usage=#{unitUsage}, " +
            "ratio=#{ratio}, unit_price_with_tax=#{unitPriceWithTax}, updated_by=#{updatedBy} WHERE id=#{id}")
    int update(BaseMaterial156 record);

    @Delete("DELETE FROM base_material_156 WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("<script>DELETE FROM base_material_156 WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach></script>")
    int batchDelete(@Param("ids") List<Long> ids);

    @Insert("<script>" +
            "INSERT INTO base_material_156 (company_id, category, material_code, system_name, part_name, " +
            "unit_usage, ratio, unit_price_with_tax, created_by, updated_by) VALUES " +
            "<foreach collection='list' item='r' separator=','>" +
            "(#{r.companyId}, #{r.category}, #{r.materialCode}, #{r.systemName}, #{r.partName}, " +
            "#{r.unitUsage}, #{r.ratio}, #{r.unitPriceWithTax}, #{r.createdBy}, #{r.updatedBy})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<BaseMaterial156> records);

    @Select("<script>" +
            "SELECT * FROM base_material_156 WHERE 1=1 " +
            "<if test='companyId != null'>AND company_id = #{companyId}</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (material_code LIKE CONCAT('%',#{keyword},'%') OR system_name LIKE CONCAT('%',#{keyword},'%') " +
            "OR part_name LIKE CONCAT('%',#{keyword},'%') OR category LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "ORDER BY ${sortField} ${sortOrder} " +
            "</script>")
    List<BaseMaterial156> search(@Param("companyId") Long companyId, @Param("keyword") String keyword,
                                  @Param("sortField") String sortField, @Param("sortOrder") String sortOrder);

    @Select("SELECT * FROM base_material_156 WHERE " +
            "material_code LIKE CONCAT('%',#{keyword},'%') " +
            "OR system_name LIKE CONCAT('%',#{keyword},'%') " +
            "OR part_name LIKE CONCAT('%',#{keyword},'%') LIMIT 15")
    List<BaseMaterial156> searchByKeyword(@Param("keyword") String keyword);

    @Select("SELECT * FROM base_material_156 WHERE material_code = #{materialCode} LIMIT 1")
    BaseMaterial156 findByMaterialCode(@Param("materialCode") String materialCode);
}
```

### Task 4: 创建 Service

**Files:**
- Create: `backend/src/main/java/com/metal/service/BaseMaterial156Service.java`

**Interfaces:**
- Produces: CRUD + 导入导出 + 模板下载，料号唯一性校验

- [ ] **Step 1: 创建 Service**

```java
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
import com.metal.entity.BaseMaterial156;
import com.metal.mapper.BaseMaterial156Mapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class BaseMaterial156Service {

    @Autowired
    private BaseMaterial156Mapper mapper;

    public PageResult<BaseMaterial156> query(int page, int pageSize, Long companyId, String keyword,
                                              String sortField, String sortOrder) {
        sortField = ServiceHelper.sanitizeSortField(sortField, "id");
        sortOrder = ServiceHelper.sanitizeSortOrder(sortOrder);
        PageHelper.startPage(page, pageSize);
        List<BaseMaterial156> list = mapper.search(companyId, keyword, sortField, sortOrder);
        PageInfo<BaseMaterial156> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, pageSize, list);
    }

    public BaseMaterial156 getById(Long id) {
        BaseMaterial156 r = mapper.findById(id);
        if (r == null) throw new BizException("记录不存在");
        return r;
    }

    public List<BaseMaterial156> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        return mapper.searchByKeyword(keyword);
    }

    public BaseMaterial156 findByMaterialCode(String materialCode) {
        return mapper.findByMaterialCode(materialCode);
    }

    @Transactional
    public BaseMaterial156 create(BaseMaterial156 record) {
        // 料号唯一性校验
        if (record.getMaterialCode() != null && !record.getMaterialCode().isBlank()) {
            if (mapper.countByMaterialCode(record.getMaterialCode()) > 0) {
                throw new BizException("料号 '" + record.getMaterialCode() + "' 已存在");
            }
        }
        String user = ServiceHelper.getCurrentUserName();
        record.setCreatedBy(user);
        record.setUpdatedBy(user);
        mapper.insert(record);
        return record;
    }

    @Transactional
    public BaseMaterial156 update(BaseMaterial156 record) {
        BaseMaterial156 exist = getById(record.getId());
        ServiceHelper.checkOwnershipOrAdmin(exist.getCreatedBy(), "编辑");
        // 料号唯一性校验（排除自身）
        if (record.getMaterialCode() != null && !record.getMaterialCode().isBlank()
                && !record.getMaterialCode().equals(exist.getMaterialCode())) {
            if (mapper.countByMaterialCode(record.getMaterialCode()) > 0) {
                throw new BizException("料号 '" + record.getMaterialCode() + "' 已存在");
            }
        }
        record.setUpdatedBy(ServiceHelper.getCurrentUserName());
        mapper.update(record);
        return record;
    }

    @Transactional
    public void delete(Long id) {
        BaseMaterial156 exist = getById(id);
        ServiceHelper.checkOwnershipOrAdmin(exist.getCreatedBy(), "删除");
        mapper.deleteById(id);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BizException("请选择要删除的记录");
        if (!ServiceHelper.isAdmin()) {
            for (Long id : ids) {
                BaseMaterial156 exist = getById(id);
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
        List<BaseMaterial156> batch = new ArrayList<>(IMPORT_BATCH_SIZE);
        int[] counts = {0, 0, 0};

        try (InputStream is = file.getInputStream()) {
            EasyExcel.read(is, BaseMaterial156.class, new AnalysisEventListener<BaseMaterial156>() {
                @Override
                public void invoke(BaseMaterial156 data, AnalysisContext ctx) {
                    counts[0]++;
                    try {
                        if (data.getMaterialCode() == null || data.getMaterialCode().isBlank()) {
                            failDetails.add(new ImportResultDTO.FailDetail(counts[0], "料号不能为空"));
                            counts[2]++;
                            return;
                        }
                        // 检查唯一性
                        if (mapper.countByMaterialCode(data.getMaterialCode()) > 0) {
                            failDetails.add(new ImportResultDTO.FailDetail(counts[0],
                                    "料号 '" + data.getMaterialCode() + "' 已存在"));
                            counts[2]++;
                            return;
                        }
                        String user = ServiceHelper.getCurrentUserName();
                        data.setCompanyId(companyId != null ? companyId : 1L);
                        data.setCreatedBy(user);
                        data.setUpdatedBy(user);
                        batch.add(data);
                        if (batch.size() >= IMPORT_BATCH_SIZE) flushBatch(batch, counts);
                    } catch (Exception e) {
                        failDetails.add(new ImportResultDTO.FailDetail(counts[0], e.getMessage()));
                        counts[2]++;
                    }
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext ctx) {
                    if (!batch.isEmpty()) flushBatch(batch, counts);
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

    private void flushBatch(List<BaseMaterial156> batch, int[] counts) {
        mapper.batchInsert(batch);
        counts[1] += batch.size();
        batch.clear();
    }

    // =============== Excel 导出 ===============
    public void exportExcel(HttpServletResponse response, Long companyId, String keyword) {
        try {
            PageHelper.startPage(1, 0);
            List<BaseMaterial156> list = mapper.search(companyId, keyword, "id", "desc");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = URLEncoder.encode("156项导出.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            OutputStream os = response.getOutputStream();
            EasyExcel.write(os, BaseMaterial156.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("156项")
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
            String fileName = URLEncoder.encode("156项导入模板.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            BaseMaterial156 template = new BaseMaterial156();
            template.setCategory("示例类别");
            template.setMaterialCode("示例料号");
            template.setSystemName("示例系统");
            template.setPartName("示例配件");
            template.setUnitUsage(BigDecimal.ONE);
            template.setRatio(BigDecimal.ONE);
            template.setUnitPriceWithTax(BigDecimal.ZERO);

            OutputStream os = response.getOutputStream();
            EasyExcel.write(os, BaseMaterial156.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("156项")
                    .doWrite(List.of(template));
            os.flush();
        } catch (IOException e) {
            throw new BizException("模板下载失败: " + e.getMessage());
        }
    }
}
```

### Task 5: 创建 Controller

**Files:**
- Create: `backend/src/main/java/com/metal/controller/BaseMaterial156Controller.java`

**Interfaces:**
- Produces: `GET /api/base-material-156`（分页列表）, `GET /api/base-material-156/search?keyword=`（autocomplete）, `GET/POST/PUT/DELETE` 标准CRUD, `POST /import`, `GET /export`, `GET /template`

- [ ] **Step 1: 创建 Controller**

```java
package com.metal.controller;

import com.metal.common.PageResult;
import com.metal.common.Result;
import com.metal.dto.BatchDeleteDTO;
import com.metal.dto.ImportResultDTO;
import com.metal.entity.BaseMaterial156;
import com.metal.service.BaseMaterial156Service;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/base-material-156")
public class BaseMaterial156Controller {

    @Autowired
    private BaseMaterial156Service service;

    @GetMapping
    public Result<PageResult<BaseMaterial156>> query(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return Result.ok(service.query(page, pageSize, companyId, keyword, sortField, sortOrder));
    }

    @GetMapping("/search")
    public Result<List<BaseMaterial156>> search(@RequestParam String keyword) {
        return Result.ok(service.searchByKeyword(keyword));
    }

    @GetMapping("/{id}")
    public Result<BaseMaterial156> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @PostMapping
    public Result<BaseMaterial156> create(@RequestBody BaseMaterial156 record) {
        return Result.ok(service.create(record));
    }

    @PutMapping("/{id}")
    public Result<BaseMaterial156> update(@PathVariable Long id, @RequestBody BaseMaterial156 record) {
        record.setId(id);
        return Result.ok(service.update(record));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody BatchDeleteDTO dto) {
        service.batchDelete(dto.getIds());
        return Result.ok();
    }

    @PostMapping("/import")
    public Result<ImportResultDTO> importExcel(@RequestParam("file") MultipartFile file,
                                               @RequestParam(required = false) Long companyId) {
        return Result.ok(service.importExcel(file, companyId));
    }

    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response,
                            @RequestParam(required = false) Long companyId,
                            @RequestParam(required = false) String keyword) {
        service.exportExcel(response, companyId, keyword);
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        service.downloadTemplate(response);
    }
}
```

- [ ] **Step 2: Commit backend base_material_156 module**

---

## Phase 3: 前端 — base_material_156 页面

### Task 6: 创建 API 模块

**Files:**
- Create: `frontend/src/api/base-material-156.js`

**Interfaces:**
- Produces: API 函数集（getList, search, getDetail, create, update, remove, batchDelete, importExcel, exportExcel, downloadTemplate）

- [ ] **Step 1: 创建 API 文件**

```javascript
import request from './request'

export function getList(params) {
  return request.get('/base-material-156', { params })
}

export function search(keyword) {
  return request.get('/base-material-156/search', { params: { keyword } })
}

export function getDetail(id) {
  return request.get(`/base-material-156/${id}`)
}

export function create(data) {
  return request.post('/base-material-156', data)
}

export function update(id, data) {
  return request.put(`/base-material-156/${id}`, data)
}

export function remove(id) {
  return request.delete(`/base-material-156/${id}`)
}

export function batchDelete(ids) {
  return request.post('/base-material-156/batch-delete', { ids })
}

export function importExcel(file, companyId) {
  const formData = new FormData()
  formData.append('file', file)
  if (companyId) formData.append('companyId', companyId)
  return request.post('/base-material-156/import', formData)
}

export function exportExcel(params) {
  return request.get('/base-material-156/export', { params, responseType: 'blob' })
}

export function downloadTemplate() {
  return request.get('/base-material-156/template', { responseType: 'blob' })
}
```

### Task 7: 创建 View 页面

**Files:**
- Create: `frontend/src/views/base-material-156/BaseMaterial156View.vue`

**Interfaces:**
- Consumes: API from Task 6, shared components (PageHeader, SearchForm, ToolBar), composables (usePagination, useTableSelection, useCrud)
- Produces: 完整的 CRUD 管理页面，表格展示 8 个业务字段，支持搜索/新增/编辑/复制/删除/批量删除/导入导出/模板下载

- [ ] **Step 1: 创建视图文件**

```vue
<template>
  <div class="page-content">
    <PageHeader title="156项" />

    <SearchForm :form="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="搜索料号/系统名称/配件名称/类别" clearable style="width: 260px" />
      </el-form-item>
    </SearchForm>

    <ToolBar
      :selected-count="selectedRows.length"
      @add="handleAdd"
      @batch-delete="batchDelete"
      @import="handleImport"
      @export="handleExport"
      @template="handleTemplateDownload"
    />

    <el-table
      :data="list"
      v-loading="loading"
      border
      stripe
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column prop="id" label="ID" width="60" sortable="custom" />
      <el-table-column prop="category" label="类别" width="100" />
      <el-table-column prop="materialCode" label="料号" width="130" sortable="custom" show-overflow-tooltip />
      <el-table-column prop="systemName" label="系统名称" width="120" show-overflow-tooltip />
      <el-table-column prop="partName" label="配件名称" width="140" show-overflow-tooltip />
      <el-table-column prop="unitUsage" label="单台机用量" width="100" />
      <el-table-column prop="ratio" label="比例" width="80" />
      <el-table-column prop="unitPriceWithTax" label="含税单价" width="110" />
      <el-table-column prop="createdBy" label="创建人" width="100" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="handleCopy(row)">复制</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑156项' : (isCopy ? '复制156项' : '新增156项')"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类别" prop="category">
              <el-input v-model="form.category" placeholder="类别" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="料号" prop="materialCode">
              <el-input v-model="form.materialCode" placeholder="料号（唯一）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="系统名称" prop="systemName">
              <el-input v-model="form.systemName" placeholder="系统名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="配件名称" prop="partName">
              <el-input v-model="form.partName" placeholder="配件名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="单台机用量" prop="unitUsage">
              <el-input-number v-model="form.unitUsage" :precision="4" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="比例" prop="ratio">
              <el-input-number v-model="form.ratio" :precision="4" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="含税单价" prop="unitPriceWithTax">
              <el-input-number v-model="form.unitPriceWithTax" :precision="4" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '../../api/base-material-156'
import { useCompanyStore } from '../../stores/company'
import { usePagination } from '../../composables/usePagination'
import { useTableSelection } from '../../composables/useTableSelection'
import { useCrud } from '../../composables/useCrud'
import { toSnakeCase, downloadBlob } from '../../utils'
import PageHeader from '../../components/PageHeader.vue'
import SearchForm from '../../components/SearchForm.vue'
import ToolBar from '../../components/ToolBar.vue'

const companyStore = useCompanyStore()
const { list, total, loading, queryParams, fetchData, handlePageChange, handleSizeChange } = usePagination(
  (params) => api.getList({ ...params, companyId: companyStore.currentCompanyId })
)
const { selectedRows, handleSelectionChange } = useTableSelection()
const { handleDelete, handleBatchDelete } = useCrud(api, doFetch)

const searchForm = reactive({ keyword: '' })
const dialogVisible = ref(false)
const isEdit = ref(false)
const isCopy = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const sortField = ref('id')
const sortOrder = ref('asc')

const defaultForm = {
  id: null,
  category: '',
  materialCode: '',
  systemName: '',
  partName: '',
  unitUsage: null,
  ratio: null,
  unitPriceWithTax: null
}
const form = reactive({ ...defaultForm })
const rules = {
  materialCode: [{ required: true, message: '请输入料号', trigger: 'blur' }]
}

function doFetch() {
  return fetchData({
    ...searchForm,
    companyId: companyStore.currentCompanyId,
    sortField: sortField.value,
    sortOrder: sortOrder.value
  })
}

function handleSearch() { queryParams.page = 1; doFetch() }
function handleReset() {
  searchForm.keyword = ''
  queryParams.page = 1; doFetch()
}

function handleSortChange({ prop, order }) {
  sortField.value = order ? toSnakeCase(prop) : 'id'
  sortOrder.value = order === 'ascending' ? 'asc' : 'desc'
  queryParams.page = 1; doFetch()
}

function resetForm() { Object.assign(form, { ...defaultForm }) }
function handleAdd() { isEdit.value = false; isCopy.value = false; resetForm(); dialogVisible.value = true }
async function handleEdit(row) {
  isEdit.value = true; isCopy.value = false
  const res = await api.getDetail(row.id); Object.assign(form, res.data)
  dialogVisible.value = true
}
async function handleCopy(row) {
  isEdit.value = false; isCopy.value = true
  const res = await api.getDetail(row.id)
  Object.assign(form, { ...res.data, id: null })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const data = { ...form, companyId: companyStore.currentCompanyId }
    delete data.id
    if (isEdit.value) await api.update(form.id, data)
    else await api.create(data)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false; doFetch()
  } finally { submitLoading.value = false }
}

function batchDelete() {
  handleBatchDelete(selectedRows.value.map(r => r.id))
}

function handleImport() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.xlsx,.xls'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    try {
      const res = await api.importExcel(file, companyStore.currentCompanyId)
      const d = res.data
      ElMessage.success(`导入完成：成功 ${d.success} 条，失败 ${d.fail} 条`)
      doFetch()
    } catch { /* error handled in interceptor */ }
  }
  input.click()
}

async function handleExport() {
  try {
    const response = await api.exportExcel({
      ...searchForm,
      companyId: companyStore.currentCompanyId
    })
    downloadBlob(response.data, '156项.xlsx')
    ElMessage.success('导出成功')
  } catch { /* error handled */ }
}

async function handleTemplateDownload() {
  try {
    const response = await api.downloadTemplate()
    downloadBlob(response.data, '156项模板.xlsx')
    ElMessage.success('模板下载成功')
  } catch { /* error handled */ }
}

onMounted(() => doFetch())
</script>

<style scoped>
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
```

### Task 8: 注册路由

**Files:**
- Modify: `frontend/src/router/index.js`

- [ ] **Step 1: 在 routes 中添加156项路由**

在 `children` 数组中添加（与其他业务模块同级）：

```javascript
{
  path: 'base-material-156',
  name: 'BaseMaterial156',
  component: () => import('../views/base-material-156/BaseMaterial156View.vue'),
  meta: { title: '156项' }
},
```

- [ ] **Step 2: Commit frontend base_material_156**

---

## Phase 4: 后端 — 原始记录 lookup-156 接口

### Task 9: 修改 OriginalRecordService + Controller 新增 lookup 接口

**Files:**
- Modify: `backend/src/main/java/com/metal/service/OriginalRecordService.java`
- Modify: `backend/src/main/java/com/metal/controller/OriginalRecordController.java`

**Interfaces:**
- Produces: `GET /api/original-record/lookup-156?materialCode=` → `{ partName: "..." }`

- [ ] **Step 1: 在 OriginalRecordService 中添加方法**

```java
@Autowired
private BaseMaterial156Mapper baseMaterial156Mapper;

/**
 * 根据料号查询156项表，返回配件名称（用于原始记录自动回填）
 */
public java.util.Map<String, String> lookupFrom156(String materialCode) {
    if (materialCode == null || materialCode.isBlank()) {
        return java.util.Map.of("partName", "");
    }
    com.metal.entity.BaseMaterial156 item = baseMaterial156Mapper.findByMaterialCode(materialCode);
    if (item != null) {
        return java.util.Map.of("partName", item.getPartName() != null ? item.getPartName() : "");
    }
    return java.util.Map.of("partName", "");
}
```

- [ ] **Step 2: 在 OriginalRecordController 中添加端点**

```java
@Autowired
private com.metal.mapper.BaseMaterial156Mapper baseMaterial156Mapper;

@GetMapping("/lookup-156")
public Result<java.util.Map<String, String>> lookupFrom156(@RequestParam String materialCode) {
    return Result.ok(service.lookupFrom156(materialCode));
}
```

---

## Phase 5: 前端 — 原始记录料号改为查询156项

### Task 10: 修改原始记录料号 autocomplete 数据源

**Files:**
- Modify: `frontend/src/views/original-record/OriginalRecordView.vue`

- [ ] **Step 1: 修改 import 和 autocomplete 方法**

将 import 从 `import { search as searchMaterialsApi } from '../../api/material'` 改为：
```javascript
import { search as search156Api } from '../../api/base-material-156'
```

- [ ] **Step 2: 修改 searchMaterials 方法**

```javascript
async function searchMaterials(query, cb) {
  if (!query || query.length < 1) { cb([]); return }
  try {
    const res = await search156Api(query)
    const data = res.data || []
    cb(data.map(m => ({ value: m.materialCode, label: `${m.materialCode} - ${m.partName || m.systemName || ''}` })))
  } catch { cb([]) }
}
```

- [ ] **Step 3: 修改 handleMaterialSelect 方法**

```javascript
async function handleMaterialSelect(item) {
  form.materialCode = item.value
  // 查询156项表，回填配件名称
  try {
    const res = await api.lookup156(item.value)
    if (res.data && res.data.partName) {
      form.partName = res.data.partName
    }
  } catch { /* 查不到就不回填 */ }
}
```

- [ ] **Step 4: 在 original-record API 文件中添加 lookup156 方法**

Modify: `frontend/src/api/original-record.js`，添加：
```javascript
export function lookup156(materialCode) {
  return request.get('/original-record/lookup-156', { params: { materialCode } })
}
```

- [ ] **Step 5: Commit**

---

## Phase 6: 后端 — delivery_stats auto-fill 接口 + 公式修正

### Task 11: 新增 Mapper 查询方法

**Files:**
- Modify: `backend/src/main/java/com/metal/mapper/DeliveryRecordMapper.java`
- Modify: `backend/src/main/java/com/metal/mapper/OriginalRecordMapper.java`
- Modify: `backend/src/main/java/com/metal/mapper/SettlementMachineMapper.java`

- [ ] **Step 1: DeliveryRecordMapper — 按料号+月份统计记录数**

```java
@Select("SELECT COUNT(*) FROM delivery_record WHERE material_code = #{materialCode} " +
        "AND DATE_FORMAT(record_date, '%Y-%m') = #{month}")
int countByMaterialCodeAndMonth(@Param("materialCode") String materialCode, @Param("month") String month);

@Select("SELECT DAY(record_date) as day, COUNT(*) as cnt FROM delivery_record " +
        "WHERE material_code = #{materialCode} AND DATE_FORMAT(record_date, '%Y-%m') = #{month} " +
        "GROUP BY DAY(record_date) ORDER BY day")
List<java.util.Map<String, Object>> countDailyByMaterialCodeAndMonth(
        @Param("materialCode") String materialCode, @Param("month") String month);
```

- [ ] **Step 2: OriginalRecordMapper — 按料号+月份统计记录数和返修数**

```java
@Select("SELECT COUNT(*) FROM original_record WHERE material_code = #{materialCode} " +
        "AND DATE_FORMAT(record_date, '%Y-%m') = #{month}")
int countByMaterialCodeAndMonth(@Param("materialCode") String materialCode, @Param("month") String month);

@Select("SELECT COUNT(*) FROM original_record WHERE material_code = #{materialCode} " +
        "AND DATE_FORMAT(record_date, '%Y-%m') = #{month} AND is_out_of_warranty = '未过保'")
int countRepairByMaterialCodeAndMonth(@Param("materialCode") String materialCode, @Param("month") String month);
```

- [ ] **Step 3: SettlementMachineMapper — 按料号+月份查询机台数**

```java
@Select("SELECT SUM(settlement_machine_count) FROM settlement_machine " +
        "WHERE material_code = #{materialCode} AND stat_month = #{statMonth}")
Integer sumMachineCountByMaterialCodeAndMonth(@Param("materialCode") String materialCode,
                                               @Param("statMonth") String statMonth);
```

### Task 12: 修改 DeliveryStatsService — auto-fill + 公式修正

**Files:**
- Modify: `backend/src/main/java/com/metal/service/DeliveryStatsService.java`

- [ ] **Step 1: 注入新依赖并添加 autoFill 方法**

```java
@Autowired
private com.metal.mapper.BaseMaterial156Mapper baseMaterial156Mapper;
@Autowired
private com.metal.mapper.DeliveryRecordMapper deliveryRecordMapper;
@Autowired
private com.metal.mapper.OriginalRecordMapper originalRecordMapper;
@Autowired
private com.metal.mapper.SettlementMachineMapper settlementMachineMapper;

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
        // 填充当月所有天
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
```

- [ ] **Step 2: 修改 applyCalculations 方法**

将 `excessAmountWithTax` 计算公式改为除以 1.13：

```java
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
```

### Task 13: 修改 DeliveryStatsController — 添加 auto-fill 端点

**Files:**
- Modify: `backend/src/main/java/com/metal/controller/DeliveryStatsController.java`

- [ ] **Step 1: 添加 auto-fill 端点**

```java
@GetMapping("/auto-fill")
public Result<java.util.Map<String, Object>> autoFill(
        @RequestParam String materialCode,
        @RequestParam(required = false) String statDate) {
    return Result.ok(service.autoFill(materialCode, statDate));
}
```

- [ ] **Step 2: Commit backend delivery_stats changes**

---

## Phase 7: 前端 — 超比统计表单改造

### Task 14: 修改 delivery-stats API + View

**Files:**
- Modify: `frontend/src/api/delivery-stats.js`
- Modify: `frontend/src/views/delivery-stats/DeliveryStatsView.vue`

- [ ] **Step 1: 在 API 文件中添加 autoFill 和 search156 方法**

```javascript
export function autoFill(materialCode, statDate) {
  return request.get('/delivery-stats/auto-fill', { params: { materialCode, statDate } })
}
```

- [ ] **Step 2: 修改 DeliveryStatsView.vue**

这是最大的改动，需要在表单中：

**a) 修改 import：** 添加 `import { search as search156Api } from '../../api/base-material-156'`

**b) 修改标签：** 
- `物料编码` → `料号`
- 料号输入框改为 `el-autocomplete`，`fetch-suggestions` 指向 `searchMaterial156`
- 系统名称输入框改为 `el-autocomplete`，`fetch-suggestions` 指向 `searchBySystemName`
- 送货数量/上机数量/当月返修 改为只读（`:disabled="true"`）

**c) 添加 autocomplete 回调：**

```javascript
// 料号 autocomplete
async function searchMaterial156(query, cb) {
  if (!query || query.length < 1) { cb([]); return }
  try {
    const res = await search156Api(query)
    const data = res.data || []
    cb(data.map(m => ({ value: m.materialCode, label: `${m.materialCode} - ${m.partName || m.systemName || ''}`, item: m })))
  } catch { cb([]) }
}

// 系统名称 autocomplete
async function searchBySystemName(query, cb) {
  if (!query || query.length < 1) { cb([]); return }
  try {
    const res = await search156Api(query)
    const data = res.data || []
    cb(data.map(m => ({ value: m.systemName, label: `${m.systemName} - ${m.materialCode}`, item: m })))
  } catch { cb([]) }
}
```

**d) 添加料号选中回调：**

```javascript
async function handleMaterialSelect(item) {
  form.materialCode = item.value
  await triggerAutoFill()
}

// 系统名称选中回调
async function handleSystemNameSelect(item) {
  form.systemName = item.value
  await triggerAutoFill()
}

async function triggerAutoFill() {
  if (!form.materialCode || !form.statDate) return
  try {
    const res = await api.autoFill(form.materialCode, form.statDate)
    const d = res.data
    // 回填156项数据
    if (d.from156) {
      form.category = d.from156.category || ''
      form.systemName = form.systemName || d.from156.systemName || ''
      form.partName = d.from156.partName || ''
      form.unitUsage = d.from156.unitUsage ?? null
      form.ratio = d.from156.ratio ?? null
      form.unitPriceWithTax = d.from156.unitPriceWithTax ?? null
    }
    // 回填统计数据
    if (d.machineCount != null) form.machineCount = d.machineCount
    if (d.deliveryQuantity != null) form.deliveryQuantity = d.deliveryQuantity
    if (d.machineOnQuantity != null) form.machineOnQuantity = d.machineOnQuantity
    if (d.monthRepair != null) form.monthRepair = d.monthRepair
    // 每日明细
    if (d.dailyQuantities && d.dailyQuantities.length > 0) {
      dailies.value = d.dailyQuantities.map(dq => ({
        dayNumber: dq.day,
        value: dq.count
      }))
    }
  } catch { /* 查不到就保持空值 */ }
}
```

**e) 添加机台数 watch（用户手动输入后重新计算约定比例）：**

```javascript
watch(() => form.machineCount, () => {
  if (form.machineCount != null && form.unitUsage != null && form.ratio != null) {
    // 约定比例由后端计算，前端仅做预览
  }
})
```

**f) 每日明细表格改为只读：**

将 dailies 表格中的 `el-input-number` 替换为只读文本显示：
```html
<el-table-column label="数值">
  <template #default="{ row }">
    {{ row.value }}
  </template>
</el-table-column>
```

- [ ] **Step 3: Commit frontend delivery_stats changes**

---

## Phase 8: 后端 — settlement_machine 改造

### Task 15: 修改 SettlementMachine Entity/Mapper/Service/Controller

**Files:**
- Modify: `backend/src/main/java/com/metal/entity/SettlementMachine.java`
- Modify: `backend/src/main/java/com/metal/mapper/SettlementMachineMapper.java`
- Modify: `backend/src/main/java/com/metal/service/SettlementMachineService.java`
- Modify: `backend/src/main/java/com/metal/controller/SettlementMachineController.java`

- [ ] **Step 1: Entity 添加 statMonth 字段**

```java
@ExcelProperty(value = "统计月份", index = 11)
private String statMonth;
```

- [ ] **Step 2: Mapper 更新 INSERT/UPDATE/search SQL 包含 stat_month**

```java
// INSERT 添加 stat_month 字段
// UPDATE 添加 stat_month=#{statMonth}
// search SQL 添加 <if test='statMonth != null'>AND stat_month = #{statMonth}</if>
```

- [ ] **Step 3: Service 添加方法**

```java
@Autowired
private BaseMaterial156Mapper baseMaterial156Mapper;

/**
 * 料号查156项表返回自动回填数据
 */
public java.util.Map<String, Object> lookupFrom156(String materialCode) {
    if (materialCode == null || materialCode.isBlank()) return java.util.Map.of();
    BaseMaterial156 item = baseMaterial156Mapper.findByMaterialCode(materialCode);
    if (item != null) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("category", item.getCategory());
        map.put("partName", item.getPartName());
        map.put("unitUsage", item.getUnitUsage());
        map.put("ratio", item.getRatio());
        map.put("unitPriceWithTax", item.getUnitPriceWithTax());
        return map;
    }
    return java.util.Map.of();
}

// create/update 中 warrantyPeriod 默认值处理
// 在 create 方法开头：
if (record.getWarrantyPeriod() == null || record.getWarrantyPeriod().isBlank()) {
    record.setWarrantyPeriod("6个月");
}
```

- [ ] **Step 4: Controller 添加端点**

```java
@GetMapping("/lookup-156")
public Result<java.util.Map<String, Object>> lookupFrom156(@RequestParam String materialCode) {
    return Result.ok(service.lookupFrom156(materialCode));
}
```

---

## Phase 9: 前端 — settlement_machine 改造

### Task 16: 修改结算机台数 View

**Files:**
- Modify: `frontend/src/api/settlement-machine.js`
- Modify: `frontend/src/views/settlement-machine/SettlementMachineView.vue`

- [ ] **Step 1: API 添加方法**

```javascript
export function lookup156(materialCode) {
  return request.get('/settlement-machine/lookup-156', { params: { materialCode } })
}
export function getMachineCountByMonth(statMonth) {
  return request.get('/machine-count/by-month', { params: { statMonth } })
}
```

- [ ] **Step 2: 添加 MachineCountController 端点**

在 `MachineCountController` 中添加：
```java
@GetMapping("/by-month")
public Result<List<MachineCount>> byMonth(@RequestParam String statMonth) {
    return Result.ok(service.findByMonth(statMonth));
}
```

在 `MachineCountService` 中添加：
```java
public List<MachineCount> findByMonth(String statMonth) {
    return mapper.findByMonth(statMonth);
}
```

注意：现有的 `findByMonth` 需要加 `companyId` 过滤。在 `MachineCountMapper` 中修改：
```java
@Select("SELECT * FROM machine_count WHERE stat_month = #{statMonth} ORDER BY count DESC")
List<MachineCount> findByMonth(@Param("statMonth") String statMonth);
```

- [ ] **Step 3: View 改造**

**a) 表格列标签修改：**
- `物料编码` → `料号`
- `系数` → `比例`
- 新增 `statMonth` 列（统计月份）

**b) 表单改造：**
- 料号输入框改为 `el-autocomplete`，匹配156项表
- 选中料号后调用 `lookup156` 自动回填：类别、配件名称、单台机用量、比例、含税单价
- 质保期默认值 `"6个月"`
- 价格类型改为下拉框：`<el-select>`，选项：空/新品价/维修价
- 新增统计月份字段：`<el-date-picker type="month">`
- 机型+结算机台数旁边新增按钮 `选择开机数量`，点击弹出弹窗

**c) 弹窗实现：**

```html
<!-- 开机数量选择弹窗 -->
<el-dialog v-model="machineCountDialogVisible" title="选择开机数量" width="600px">
  <el-table :data="machineCountList" highlight-current-row @row-click="handleMachineCountSelect">
    <el-table-column prop="machineModel" label="机型" />
    <el-table-column prop="count" label="开机台数" />
  </el-table>
  <template #footer>
    <el-button @click="machineCountDialogVisible = false">取消</el-button>
  </template>
</el-dialog>
```

```javascript
const machineCountDialogVisible = ref(false)
const machineCountList = ref([])

async function openMachineCountDialog() {
  if (!form.statMonth) {
    ElMessage.warning('请先选择统计月份')
    return
  }
  try {
    const res = await api.getMachineCountByMonth(form.statMonth)
    machineCountList.value = res.data || []
    machineCountDialogVisible.value = true
  } catch { ElMessage.error('查询失败') }
}

function handleMachineCountSelect(row) {
  form.machineModel = row.machineModel
  form.settlementMachineCount = row.count
  machineCountDialogVisible.value = false
}
```

- [ ] **Step 4: Commit**

---

## Phase 10: 定时任务

### Task 17: 创建 DeliveryStatsScheduler

**Files:**
- Create: `backend/src/main/java/com/metal/config/SchedulingConfig.java`
- Create: `backend/src/main/java/com/metal/scheduler/DeliveryStatsScheduler.java`

- [ ] **Step 1: 启用定时任务配置**

```java
package com.metal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

- [ ] **Step 2: 创建定时任务**

```java
package com.metal.scheduler;

import com.metal.entity.DeliveryStats;
import com.metal.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private SettlementMachineMapper settlementMachineMapper;

    /**
     * 每小时整点刷新当前月份的超比统计数据
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshCurrentMonthStats() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 查询当前月份所有超比统计记录
        List<DeliveryStats> statsList = deliveryStatsMapper.findByYearMonth(currentMonth);

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
            java.util.List<java.util.Map<String, Object>> dailyCounts =
                    deliveryRecordMapper.countDailyByMaterialCodeAndMonth(materialCode, currentMonth);
            java.util.Map<Integer, Integer> dayMap = new java.util.HashMap<>();
            for (java.util.Map<String, Object> row : dailyCounts) {
                Number day = (Number) row.get("day");
                Number cnt = (Number) row.get("cnt");
                if (day != null && cnt != null) dayMap.put(day.intValue(), cnt.intValue());
            }
            int daysInMonth = java.time.YearMonth.parse(currentMonth).lengthOfMonth();
            java.util.List<com.metal.entity.DeliveryStatsDaily> dailies = new java.util.ArrayList<>();
            for (int d = 1; d <= daysInMonth; d++) {
                com.metal.entity.DeliveryStatsDaily daily = new com.metal.entity.DeliveryStatsDaily();
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
```

- [ ] **Step 3: 在 DeliveryStatsMapper 中添加 findByYearMonth 方法**

```java
@Select("SELECT * FROM delivery_stats WHERE `year_month` = #{yearMonth}")
List<DeliveryStats> findByYearMonth(@Param("yearMonth") String yearMonth);
```

- [ ] **Step 4: Commit**

---

## Phase 11: 全链路验证

### Task 18: 编译和测试

- [ ] **Step 1: 编译后端**

```bash
cd backend && mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 编译前端**

```bash
cd frontend && npm run build
```
Expected: 无编译错误

- [ ] **Step 3: 启动后端验证 API**

```bash
cd backend && mvn spring-boot:run
# 测试156项 API
curl http://localhost:8080/api/base-material-156?page=1
curl http://localhost:8080/api/base-material-156/search?keyword=test
# 测试 auto-fill API
curl "http://localhost:8080/api/delivery-stats/auto-fill?materialCode=15250-00&statDate=2026-07-17"
```

- [ ] **Step 4: Commit final verification**

```bash
git add -A
git commit -m "feat: complete 156 table + auto-fill + scheduler implementation"
```
