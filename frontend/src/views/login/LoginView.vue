<template>
  <div class="login-page">
    <!-- 动画背景 -->
    <div class="bg-animation">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
      <div class="shape shape-4"></div>
      <div class="shape shape-5"></div>
      <div class="shape shape-6"></div>
      <div class="particles">
        <span v-for="i in 20" :key="i" :style="particleStyle(i)"></span>
      </div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="card-icon">
        <div class="icon-circle">
          <el-icon :size="28"><Lock /></el-icon>
        </div>
      </div>
      <h1 class="card-title">数据管理系统</h1>
      <p class="card-subtitle">Metal Data Management System</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            class="login-input"
            @focus="onFocus('username')"
            @blur="onBlur('username')"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            :prefix-icon="Lock"
            class="login-input"
            @keyup.enter="handleLogin"
            @focus="onFocus('password')"
            @blur="onBlur('password')"
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

      <p class="card-footer">仅限授权用户访问</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
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

function onFocus() {}
function onBlur() {}

function particleStyle(i) {
  const size = 2 + Math.random() * 4
  return {
    left: Math.random() * 100 + '%',
    width: size + 'px',
    height: size + 'px',
    animationDelay: Math.random() * 8 + 's',
    animationDuration: 8 + Math.random() * 12 + 's',
    opacity: 0.15 + Math.random() * 0.35
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0c29, #1a1a3e, #24243e);
  overflow: hidden;
  position: relative;
}

/* ===== 动画背景 ===== */
.bg-animation {
  position: absolute;
  inset: 0;
  z-index: 0;
}

/* 浮动几何形状 */
.shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.3;
  animation: floatShape 20s ease-in-out infinite;
}

.shape-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #6366f1, transparent 70%);
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.shape-2 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, #8b5cf6, transparent 70%);
  bottom: -80px;
  right: -80px;
  animation-delay: -5s;
}

.shape-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #06b6d4, transparent 70%);
  top: 50%;
  left: 60%;
  animation-delay: -10s;
}

.shape-4 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, #ec4899, transparent 70%);
  top: 20%;
  right: 20%;
  animation-delay: -15s;
}

.shape-5 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, #f59e0b, transparent 70%);
  bottom: 20%;
  left: 20%;
  animation-delay: -7s;
}

.shape-6 {
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, #10b981, transparent 70%);
  top: 40%;
  left: 10%;
  animation-delay: -12s;
}

@keyframes floatShape {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(30px, -30px) scale(1.1);
  }
  50% {
    transform: translate(-20px, 20px) scale(0.9);
  }
  75% {
    transform: translate(10px, 10px) scale(1.05);
  }
}

/* 粒子 */
.particles span {
  position: absolute;
  bottom: -10px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  animation: rise linear infinite;
}

@keyframes rise {
  0% {
    transform: translateY(0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100vh) scale(0.3);
    opacity: 0;
  }
}

/* ===== 登录卡片 ===== */
.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 48px 40px 36px;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  animation: cardIn 0.6s ease-out;
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.card-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.icon-circle {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
}

.card-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 4px;
  letter-spacing: 1px;
}

.card-subtitle {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  margin: 0 0 32px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

/* ===== 表单 ===== */
.login-form :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  padding-bottom: 4px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  box-shadow: none;
  transition: all 0.3s ease;
}

.login-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.1);
}

.login-input :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
  background: rgba(255, 255, 255, 0.1);
}

.login-input :deep(.el-input__inner) {
  color: #fff;
}

.login-input :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.35);
}

.login-input :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.4);
}

.login-input :deep(.el-input__suffix) {
  color: rgba(255, 255, 255, 0.4);
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  border-radius: 10px;
  margin-top: 4px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  letter-spacing: 4px;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.card-footer {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.25);
  margin: 24px 0 0;
}

/* ===== 响应式 ===== */
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 36px 24px 28px;
  }
}
</style>
