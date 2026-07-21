<template>
  <div class="login-page">
    <!-- 全屏背景 -->
    <div class="bg-image" />

    <!-- 顶部栏 -->
    <div class="login-header">
      <div class="header-left">
        <img src="/images/logo.png" alt="Logo" class="header-logo" />
        <span class="header-company">昊昱精密</span>
      </div>
      <div class="header-right">
        <span class="header-support">
          <el-icon :size="16"><Phone /></el-icon>
          技术支持：0755-84586807
        </span>
      </div>
    </div>

    <!-- 主内容：左侧留空，右侧登录区域 -->
    <div class="login-body">
      <div class="login-left" />

      <div class="login-right">
        <div class="login-area">
          <h1 class="system-title">驻场维保数据采集平台</h1>
          <p class="system-subtitle">On-Site Maintenance Data Analysis Platform</p>

          <div class="login-card">
            <div class="card-header">
              <h2>欢迎回来</h2>
              <p>请登录您的账号</p>
            </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            size="large"
            class="login-form"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                class="login-input"
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                show-password
                :prefix-icon="Lock"
                class="login-input"
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="login-btn"
                @click="handleLogin"
              >
                <span v-if="!loading">登 录</span>
              </el-button>
            </el-form-item>
          </el-form>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="login-footer">
      <span>Copyright &copy; {{ new Date().getFullYear() }}</span>
      <a href="https://www.top-tpm.com/homepage" target="_blank" class="footer-link">深圳市昊昱精密机电有限公司</a>
      <span>All Rights Reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Phone } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

</script>

<style scoped>
.login-page {
  height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ===== 全屏背景 ===== */
.bg-image {
  position: absolute;
  inset: 0;
  background: url('/images/login-bg.png') center/cover no-repeat;
  z-index: 0;
}

/* 背景暗色叠加 */
.bg-image::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.25);
}

/* ===== 顶部栏 ===== */
.login-header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 40px;
  background: #fff;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-logo {
  width: 36px;
  height: 36px;
  object-fit: contain;
  border-radius: 4px;
}

.header-company {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 2px;
}

.header-right {
  display: flex;
  gap: 12px;
}

.header-btn {
  color: #333 !important;
  font-size: 14px;
  border-radius: 8px;
  padding: 8px 20px;
  transition: all 0.3s ease;
  background: rgba(0, 0, 0, 0.05) !important;
  border: 1px solid rgba(0, 0, 0, 0.1) !important;
}

.header-btn:hover {
  color: #1a1a1a !important;
  background: rgba(0, 0, 0, 0.1) !important;
  border-color: rgba(0, 0, 0, 0.18) !important;
}

.header-support {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #7A2B2D;
  padding: 8px 18px;
  border-radius: 8px;
  background: rgba(122, 43, 45, 0.08);
  border: 1px solid rgba(122, 43, 45, 0.25);
  letter-spacing: 0.5px;
}

/* ===== 主体区域 ===== */
.login-body {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 100px;
}

/* 左侧留空 */
.login-left {
  flex: 1;
}

/* 右侧登录区域 */
.login-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-area {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.system-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 4px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  text-align: center;
}

.system-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 28px;
  letter-spacing: 2px;
  text-align: center;
}

.login-card {
  width: 420px;
  padding: 48px 44px 40px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  box-shadow:
    0 8px 40px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 6px;
  letter-spacing: 1px;
}

.card-header p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* ===== 表单 ===== */
.login-form :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.75);
  font-size: 13px;
  font-weight: 500;
  padding-bottom: 4px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  box-shadow: none;
  transition: all 0.3s ease;
}

.login-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.12);
}

.login-input :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(255, 255, 255, 0.5);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.12);
}

.login-input :deep(.el-input__inner) {
  color: #fff;
}

.login-input :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.35);
}

.login-input :deep(.el-input__prefix),
.login-input :deep(.el-input__suffix) {
  color: rgba(255, 255, 255, 0.45);
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  margin-top: 8px;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  color: #1a1a1a;
  letter-spacing: 4px;
  transition: all 0.3s ease;
}

.login-btn:hover {
  background: #fff;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
}

.login-btn:active {
  transform: translateY(0);
}

/* ===== 底部版权 ===== */
.login-footer {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 48px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  background: rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.footer-link {
  color: rgba(255, 255, 255, 0.75);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.footer-link:hover {
  color: #fff;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .login-body {
    justify-content: center;
    padding: 40px 24px;
  }

  .login-left {
    display: none;
  }

  .system-title {
    font-size: 24px;
  }

  .login-card {
    width: 100%;
    max-width: 400px;
    padding: 32px 28px 28px;
  }

  .login-header {
    padding: 0 20px;
  }

  .header-company {
    font-size: 15px;
  }
}
</style>
