<template>
  <div class="page-content">
    <PageHeader title="送货超比统计" />

    <!-- 搜索筛选 -->
    <SearchForm :form="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="搜索" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="类别">
        <el-input v-model="searchForm.category" placeholder="类别" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="年月">
        <el-input v-model="searchForm.yearMonth" placeholder="年月" clearable style="width: 140px" />
      </el-form-item>
    </SearchForm>

    <!-- 操作栏 -->
    <ToolBar
      :selected-count="selectedRows.length"
      @add="handleAdd"
      @batch-delete="batchDelete"
      @import="handleImport"
      @export="handleExport"
      @template="handleTemplateDownload"
    />

    <!-- 全表合计 -->
    <div class="summary-row">
      <span class="summary-label">汇总（全部数据）</span>
      <span class="summary-item">送货数量：<b>{{ totals.deliveryQuantity }}</b></span>
      <span class="summary-item">上机数量：<b>{{ totals.machineOnQuantity }}</b></span>
      <span class="summary-item">当月返修：<b>{{ totals.monthRepair }}</b></span>
      <span class="summary-item">约定配比数量：<b>{{ totals.agreedRatioQuantity }}</b></span>
      <span class="summary-item">超比数量：<b>{{ totals.excessQuantity }}</b></span>
      <span class="summary-item">超比含税金额：<b>{{ totals.excessAmountWithTax.toFixed(2) }}</b></span>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="list"
      v-loading="loading"
      border
      stripe
      @selection-change="handleSelectionChange"
      style="width: 100%"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column prop="category" label="类别" width="100" />
      <el-table-column prop="materialCode" label="物料编码" width="130" show-overflow-tooltip />
      <el-table-column prop="systemName" label="系统名称" width="120" show-overflow-tooltip />
      <el-table-column prop="partName" label="零件名称" width="120" show-overflow-tooltip />
      <el-table-column prop="unitUsage" label="单车用量" width="90" />
      <el-table-column prop="ratio" label="配比" width="80" />
      <el-table-column prop="unitPriceWithTax" label="含税单价" width="100" />
      <el-table-column prop="machineCount" label="机台数" width="80" />
      <el-table-column prop="deliveryQuantity" label="送货数量" width="90" />
      <el-table-column prop="machineOnQuantity" label="上机数量" width="90" />
      <el-table-column prop="monthRepair" label="当月维修" width="90" />
      <el-table-column prop="agreedRatioQuantity" label="约定配比数量" width="120" />
      <el-table-column prop="excessQuantity" label="超比数量" width="90" />
      <el-table-column prop="excessAmountWithTax" label="超比含税金额" width="120" />
      <el-table-column prop="statDate" label="统计日期" width="110" />
      <el-table-column prop="yearMonth" label="年月" width="90" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="handleCopy(row)">复制</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑送货超比统计' : (isCopy ? '复制送货超比统计' : '新增送货超比统计')"
      width="1000px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" size="default">
        <!-- 主表单 - 3列布局 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="类别" prop="category">
              <el-input v-model="form.category" placeholder="类别" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="物料编码" prop="materialCode">
              <el-input v-model="form.materialCode" placeholder="物料编码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="系统名称" prop="systemName">
              <el-input v-model="form.systemName" placeholder="系统名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="零件名称" prop="partName">
              <el-input v-model="form.partName" placeholder="零件名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统计日期" prop="statDate">
              <el-date-picker v-model="form.statDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="单车用量" prop="unitUsage">
              <el-input-number v-model="form.unitUsage" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="配比" prop="ratio">
              <el-input-number v-model="form.ratio" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="含税单价" prop="unitPriceWithTax">
              <el-input-number v-model="form.unitPriceWithTax" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="机台数" prop="machineCount">
              <el-input-number v-model="form.machineCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="送货数量" prop="deliveryQuantity">
              <el-input-number v-model="form.deliveryQuantity" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="上机数量" prop="machineOnQuantity">
              <el-input-number v-model="form.machineOnQuantity" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="当月维修" prop="monthRepair">
              <el-input-number v-model="form.monthRepair" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 每日明细 -->
        <el-divider content-position="left">每日明细</el-divider>
        <el-table :data="dailies" border size="small" max-height="400">
          <el-table-column prop="dayNumber" label="日期" width="100" align="center">
            <template #default="{ row }">
              第{{ row.dayNumber }}天
            </template>
          </el-table-column>
          <el-table-column label="数值">
            <template #default="{ row }">
              <el-input-number v-model="row.value" :precision="2" :min="0" style="width: 100%" size="small" controls-position="right" />
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '../../api/delivery-stats'
import { useCompanyStore } from '../../stores/company'
import { usePagination } from '../../composables/usePagination'
import { useTableSelection } from '../../composables/useTableSelection'
import { useCrud } from '../../composables/useCrud'
import { getDaysInMonth, getYearMonth, downloadBlob } from '../../utils'
import PageHeader from '../../components/PageHeader.vue'
import SearchForm from '../../components/SearchForm.vue'
import ToolBar from '../../components/ToolBar.vue'

const companyStore = useCompanyStore()
const { list, total, loading, queryParams, fetchData, handlePageChange, handleSizeChange } = usePagination(
  (params) => api.getList({ ...params, companyId: companyStore.currentCompanyId })
)
const { selectedRows, handleSelectionChange } = useTableSelection()

const searchForm = reactive({
  keyword: '',
  category: '',
  yearMonth: ''
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const isCopy = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

const defaultForm = {
  id: null,
  category: '',
  materialCode: '',
  systemName: '',
  partName: '',
  unitUsage: null,
  ratio: null,
  unitPriceWithTax: null,
  machineCount: null,
  deliveryQuantity: null,
  machineOnQuantity: null,
  monthRepair: null,
  statDate: ''
}
const form = reactive({ ...defaultForm })
const dailies = ref([])

const rules = {
  statDate: [{ required: true, message: '请选择统计日期', trigger: 'change' }],
  category: [{ required: true, message: '请输入类别', trigger: 'blur' }],
  materialCode: [{ required: true, message: '请输入物料编码', trigger: 'blur' }]
}

const totals = reactive({
  deliveryQuantity: 0,
  machineOnQuantity: 0,
  monthRepair: 0,
  agreedRatioQuantity: 0,
  excessQuantity: 0,
  excessAmountWithTax: 0
})

async function fetchTotals() {
  try {
    const res = await api.getList({ page: 1, pageSize: 200, companyId: companyStore.currentCompanyId })
    const all = res.data.list || []
    const sum = (field) => all.reduce((acc, r) => acc + (Number(r[field]) || 0), 0)
    totals.deliveryQuantity = sum('deliveryQuantity')
    totals.machineOnQuantity = sum('machineOnQuantity')
    totals.monthRepair = sum('monthRepair')
    totals.agreedRatioQuantity = sum('agreedRatioQuantity')
    totals.excessQuantity = sum('excessQuantity')
    totals.excessAmountWithTax = sum('excessAmountWithTax')
  } catch { /* ignore */ }
}

function doFetch() {
  return fetchData({
    keyword: searchForm.keyword,
    category: searchForm.category,
    yearMonth: searchForm.yearMonth,
    companyId: companyStore.currentCompanyId
  })
}

const { handleDelete, handleBatchDelete } = useCrud(api, doFetch)

function batchDelete() {
  handleBatchDelete(selectedRows.value.map(r => r.id))
}

function handleSearch() {
  queryParams.page = 1
  doFetch()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.yearMonth = ''
  queryParams.page = 1
  doFetch()
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
      keyword: searchForm.keyword,
      category: searchForm.category,
      yearMonth: searchForm.yearMonth,
      companyId: companyStore.currentCompanyId
    })
    downloadBlob(response.data, '超比统计.xlsx')
    ElMessage.success('导出成功')
  } catch { /* error handled */ }
}

async function handleTemplateDownload() {
  try {
    const response = await api.downloadTemplate()
    downloadBlob(response.data, '超比统计模板.xlsx')
    ElMessage.success('模板下载成功')
  } catch { /* error handled */ }
}

// ---- 每日明细辅助 ----

/**
 * 根据指定日期的月份生成 dailies 数组
 * 保留已有值，不足补 0，超出截断
 */
function generateDailies(dateStr, existing) {
  if (!dateStr) return []
  const yearMonth = getYearMonth(dateStr)
  const days = getDaysInMonth(yearMonth)
  const result = []
  for (let i = 1; i <= days; i++) {
    const prev = (existing || []).find(d => d.dayNumber === i)
    result.push({ dayNumber: i, value: prev ? prev.value : 0 })
  }
  return result
}

// 监听统计日期变化，重新生成 dailies
watch(() => form.statDate, (newDate) => {
  if (newDate) {
    dailies.value = generateDailies(newDate, dailies.value)
  }
})

// ---- 对话框操作 ----

function resetForm() {
  Object.assign(form, { ...defaultForm })
  dailies.value = []
}

function handleAdd() {
  isEdit.value = false; isCopy.value = false
  resetForm()
  // 默认当月
  const today = new Date()
  form.statDate = today.toISOString().slice(0, 10)
  // 生成当月 dailies
  dailies.value = generateDailies(form.statDate, [])
  dialogVisible.value = true
}

async function handleEdit(row) {
  isEdit.value = true; isCopy.value = false
  const res = await api.getDetail(row.id)
  Object.assign(form, res.data)
  try {
    const dailiesRes = await api.getDailies(row.id)
    const existingDailies = dailiesRes.data || []
    dailies.value = generateDailies(form.statDate, existingDailies)
  } catch {
    dailies.value = generateDailies(form.statDate, [])
  }
  dialogVisible.value = true
}

async function handleCopy(row) {
  isEdit.value = false; isCopy.value = true
  const res = await api.getDetail(row.id)
  Object.assign(form, { ...res.data, id: null })
  try {
    const dailiesRes = await api.getDailies(row.id)
    const existingDailies = dailiesRes.data || []
    dailies.value = generateDailies(form.statDate, existingDailies)
  } catch {
    dailies.value = generateDailies(form.statDate, [])
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const data = { ...form, dailies: dailies.value, companyId: companyStore.currentCompanyId }
    delete data.id
    if (isEdit.value) {
      await api.update(form.id, data)
      ElMessage.success('修改成功')
    } else {
      await api.create(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    doFetch()
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  doFetch()
  fetchTotals()
})
</script>

<style scoped>
.summary-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 6px;
  margin-bottom: 12px;
  background: #F5F5F7;
  border-radius: 8px;
  font-size: 13px;
  color: #1D1D1F;
  flex-wrap: wrap;
}

.summary-label {
  font-weight: 600;
  color: #0071E3;
  margin-right: 4px;
}

.summary-item b {
  color: #0071E3;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
