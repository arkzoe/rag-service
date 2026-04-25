import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/user'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const tokenName = ref(localStorage.getItem('tokenName') || 'satoken')
  const isLoggedIn = computed(() => !!token.value)

  const initAuth = () => {
    const storedToken = localStorage.getItem('token')
    const storedTokenName = localStorage.getItem('tokenName')
    if (storedToken) {
      token.value = storedToken
      tokenName.value = storedTokenName || 'satoken'
    }
  }

  const login = async (username, password) => {
    try {
      const data = await loginApi(username, password)
      token.value = data.tokenValue
      tokenName.value = data.tokenName
      localStorage.setItem('token', data.tokenValue)
      localStorage.setItem('tokenName', data.tokenName)
      ElMessage.success('登录成功')
      return true
    } catch (error) {
      return false
    }
  }

  const logout = async () => {
    try {
      await logoutApi()
    } catch (error) {
    } finally {
      token.value = ''
      tokenName.value = 'satoken'
      localStorage.removeItem('token')
      localStorage.removeItem('tokenName')
      ElMessage.success('已退出登录')
    }
  }

  return {
    token,
    tokenName,
    isLoggedIn,
    initAuth,
    login,
    logout,
  }
})
