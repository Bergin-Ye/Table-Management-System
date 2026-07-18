<template>
  <div class="page-content">
    <PageHeader title="开机数量" />

    <SearchForm :form="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="机型">
        <el-input v-model="searchForm.keyword" placeholder="机型" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="统计月份">
        <el-date-picker v-model="searchForm.statMonth" type="month" placeholder="选择月份" value-format="YYYY-MM" clearable style="width: 160px" />
      </el-form-item>
    </SearchForm>

    <ToolBar :selected-count="selectedRows.length" @add="handleAdd" @batch-delete="batchDelete" @import="handleImport" @export="handleExport" @template="handleTemplateDownload" />

    <div style="margin-bottom:12px;display:flex;align-items:center;gap:12px">
      <el-date-picker v-model="clearMonth" type="month" placeholder="选择要清除的月份" value-format="YYYY-MM" style="width:180px" />
      <el-button type="danger" @click="handleClearByMonth">清除当月数据</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe @selection-change="handleSelectionChange" @sort-change="handleSortChange">
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column prop="id" label="ID" width="60" sortable="custom" />
      <el-table-column prop="machineModel" label="机型" width="120" />
      <el-table-column prop="count" label="数量" width="80" />
      <el-table-column prop="ratioPct" label="占比(%)" width="90" />
      <el-table-column prop="statMonth" label="统计月份" width="110" />
      <el-table-column prop="remark" label="备注" width="150" show-overflow-tooltip />
      <el-table-column prop="createdBy" label="创建人" width="100" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination v-model:current-page="queryParams.page" v-model:page-size="queryParams.pageSize" :page-sizes="[20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handlePageChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑开机数量' : '新增开机数量'" width="600px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="机型" prop="machineModel">
              <el-input v-model="form.machineModel" placeholder="机型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数量" prop="count">
              <el-input-number v-model="form.count" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="占比(%)" prop="ratioPct">
              <el-input-number v-model="form.ratioPct" :precision="2" :min="0" style="width: 100%" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统计月份" prop="statMonth">
              <el-date-picker v-model="form.statMonth" type="month" placeholder="选择月份" value-format="YYYY-MM" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label=" ">
              <el-checkbox v-model="form.isBaseline">设为基准线（当月开机总数）</el-checkbox>
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
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '../../api/machine-count'
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

const searchForm = reactive({ keyword: '', statMonth: '' })
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const sortField = ref('id')
const sortOrder = ref('asc')

const defaultForm = {
  id: null,
  machineModel: '',
  count: null,
  ratioPct: null,
  statMonth: '',
  remark: '',
  isBaseline: false
}
const form = reactive({ ...defaultForm })
const rules = {
  machineModel: [{ required: true, message: '请输入机型', trigger: 'blur' }],
  count: [{ required: true, message: '请输入数量', trigger: 'blur' }]
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
  Object.assign(searchForm, { keyword: '', statMonth: '' })
  queryParams.page = 1; doFetch()
}

function handleSortChange({ prop, order }) {
  sortField.value = order ? toSnakeCase(prop) : 'id'
  sortOrder.value = order === 'ascending' ? 'asc' : 'desc'
  queryParams.page = 1; doFetch()
}

function resetForm() { Object.assign(form, { ...defaultForm }) }

// 勾选基准线时自动设置占比为100
watch(() => form.isBaseline, (val) => {
  if (val) {
    form.ratioPct = 100
  } else {
    form.ratioPct = null
  }
})
function handleAdd() { isEdit.value = false; resetForm(); dialogVisible.value = true }
async function handleEdit(row) {
  isEdit.value = true
  const res = await api.getDetail(row.id); Object.assign(form, res.data)
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
    downloadBlob(response.data, '开机数量.xlsx')
    ElMessage.success('导出成功')
  } catch { /* error handled */ }
}

async function handleTemplateDownload() {
  try {
    const response = await api.downloadTemplate()
    downloadBlob(response.data, '开机数量模板.xlsx')
    ElMessage.success('模板下载成功')
  } catch { /* error handled */ }
}

const clearMonth = ref('')

async function handleClearByMonth() {
  if (!clearMonth.value) { ElMessage.warning('请先选择月份'); return }
  try {
    await ElMessageBox.confirm(`确认清除 ${clearMonth.value} 的所有开机数量数据？（基准线不会被删除）`, '二次确认', { type: 'warning' })
    const res = await api.clearByMonth(clearMonth.value)
    ElMessage.success(res.data?.msg || '清除成功')
    clearMonth.value = ''
    doFetch()
  } catch { /* cancelled or error */ }
}

onMounted(() => doFetch())
</script>

<style scoped>
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
