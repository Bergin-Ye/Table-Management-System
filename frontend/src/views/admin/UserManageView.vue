<template>
  <div class="user-manage-page">
    <PageHeader title="用户管理" description="管理系统用户和角色权限" />

    <div class="content-card">
      <el-table :data="users" border stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column label="操作" min-width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.role === 'admin' ? 'warning' : 'success'"
              @click="handleToggleRole(row)"
              :disabled="row.id === currentUserId"
            >
              {{ row.role === 'admin' ? '降级为普通用户' : '提升为管理员' }}
            </el-button>
            <el-button size="small" type="primary" @click="handleResetPassword(row)">
              重置密码
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(row)"
              :disabled="row.id === currentUserId"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="400px">
      <el-form :model="resetPwdForm" :rules="resetPwdRules" ref="resetPwdFormRef" label-position="top">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetPwdForm.password" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="confirmResetPassword">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- Delete Confirm Dialog -->
    <el-dialog v-model="deleteVisible" title="删除用户" width="400px">
      <p>确定要删除用户 <strong>{{ deleteTarget?.username }}</strong>（{{ deleteTarget?.realName }}）吗？此操作不可恢复。</p>
      <template #footer>
        <el-button @click="deleteVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleteLoading" @click="confirmDelete">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, updateUserRole, deleteUser, resetUserPassword } from '../../api/admin'
import { useAuthStore } from '../../stores/auth'
import PageHeader from '../../components/PageHeader.vue'

const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.id)

const users = ref([])
const loading = ref(false)

// Reset password
const resetPwdVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdFormRef = ref(null)
const resetPwdTarget = ref(null)
const resetPwdForm = ref({
  password: ''
})
const resetPwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

// Delete
const deleteVisible = ref(false)
const deleteLoading = ref(false)
const deleteTarget = ref(null)

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getUsers()
    users.value = res.data || []
  } catch {
    // error handled in interceptor
  } finally {
    loading.value = false
  }
}

async function handleToggleRole(row) {
  const newRole = row.role === 'admin' ? 'user' : 'admin'
  const roleLabel = newRole === 'admin' ? '管理员' : '普通用户'
  try {
    await ElMessageBox.confirm(
      `确定要将用户 "${row.username}" 的角色${newRole === 'admin' ? '提升' : '降级'}为"${roleLabel}"吗？`,
      '变更角色',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await updateUserRole(row.id, newRole)
    ElMessage.success('角色变更成功')
    await fetchUsers()
  } catch (err) {
    if (err !== 'cancel') {
      // error handled in interceptor
    }
  }
}

function handleResetPassword(row) {
  resetPwdTarget.value = row
  resetPwdForm.value.password = ''
  resetPwdVisible.value = true
}

async function confirmResetPassword() {
  const valid = await resetPwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  resetPwdLoading.value = true
  try {
    await resetUserPassword(resetPwdTarget.value.id, resetPwdForm.value.password)
    ElMessage.success('密码重置成功')
    resetPwdVisible.value = false
  } finally {
    resetPwdLoading.value = false
  }
}

function handleDelete(row) {
  deleteTarget.value = row
  deleteVisible.value = true
}

async function confirmDelete() {
  deleteLoading.value = true
  try {
    await deleteUser(deleteTarget.value.id)
    ElMessage.success('用户已删除')
    deleteVisible.value = false
    await fetchUsers()
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-manage-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.content-card {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 20px;
  flex: 1;
  overflow: auto;
}
</style>
