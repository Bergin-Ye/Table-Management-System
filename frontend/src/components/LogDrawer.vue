<template>
  <el-drawer v-model="visible" title="最近操作日志" size="400px" @close="$emit('close')">
    <el-table :data="logs" size="small" v-loading="loading" max-height="calc(100vh - 120px)">
      <el-table-column prop="username" label="操作人" width="80" />
      <el-table-column prop="action" label="操作" width="70">
        <template #default="{ row }">
          <el-tag :type="actionType(row.action)" size="small">{{ row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="tableName" label="表名" width="120" />
      <el-table-column prop="createdAt" label="时间" width="150">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>
    <div class="drawer-footer">
      <el-button link type="primary" @click="$router.push('/operation-log')">
        查看全部日志
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getList } from '../api/operation-log'
import { formatDateTime } from '../utils'

const router = useRouter()
const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'close'])

const visible = ref(props.modelValue)
const logs = ref([])
const loading = ref(false)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) fetchLogs()
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

async function fetchLogs() {
  loading.value = true
  try {
    const res = await getList({ page: 1, pageSize: 20, sortField: 'createdAt', sortOrder: 'desc' })
    logs.value = res.data.list || []
  } finally {
    loading.value = false
  }
}

function actionType(action) {
  const map = { INSERT: 'success', UPDATE: 'warning', DELETE: 'danger' }
  return map[action] || 'info'
}
</script>

<style scoped>
.drawer-footer {
  margin-top: 16px;
  text-align: center;
}
</style>
