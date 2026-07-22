import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { smsLogin as smsLoginApi, register as registerApi, getMe } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.role || 'user')
  const isAdmin = computed(() => role.value === 'admin')

  async function smsLogin(phoneNumber, code) {
    const res = await smsLoginApi(phoneNumber, code)
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
    return res
  }

  async function register(data) {
    const res = await registerApi(data)
    return res
  }

  async function fetchMe() {
    try {
      const res = await getMe()
      user.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
    } catch {
      logout()
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    token,
    user,
    isLoggedIn,
    role,
    isAdmin,
    smsLogin,
    register,
    fetchMe,
    logout
  }
})
