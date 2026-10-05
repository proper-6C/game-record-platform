<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api'

const route = useRoute()
const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', nickname: '' })

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await login({ ...loginForm })
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    // 通知全局（App.vue）刷新登录态，顶栏立即显示用户名
    window.dispatchEvent(new Event('auth-changed'))
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch {
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!regForm.username || !regForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (regForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    await register({ ...regForm })
    ElMessage.success('注册成功，请登录')
    loginForm.username = regForm.username
    activeTab.value = 'login'
  } catch {
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">
        <span class="gamepad" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 12h4M8 10v4M15 11h.01M18 13h.01" />
            <path d="M17.32 5H6.68a4 4 0 0 0-3.98 3.6L2 17a2.5 2.5 0 0 0 4.3 1.8L8.6 16.4a1 1 0 0 1 .8-.4h5.2a1 1 0 0 1 .8.4l2.3 2.4A2.5 2.5 0 0 0 22 17l-.7-8.4A4 4 0 0 0 17.32 5z" />
          </svg>
        </span>
        <span>游戏对局记录</span>
      </h2>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <el-form label-width="0" @submit.prevent="handleLogin">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" size="large" clearable />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="loginForm.password"
                placeholder="密码"
                type="password"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-button type="primary" size="large" class="submit" :loading="loading" @click="handleLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form label-width="0">
            <el-form-item>
              <el-input v-model="regForm.username" placeholder="用户名（登录用）" size="large" clearable />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="regForm.password"
                placeholder="密码（至少6位）"
                type="password"
                size="large"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-input v-model="regForm.nickname" placeholder="昵称（可选）" size="large" clearable />
            </el-form-item>
            <el-button type="primary" size="large" class="submit" :loading="loading" @click="handleRegister">
              注册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  padding: 30px 0;
  z-index: 1;
}
.login-card {
  width: 420px;
  padding: 22px 12px;
  position: relative;
  z-index: 2;
  border-radius: 18px;
  background: rgba(21, 16, 34, 0.78);
  border: 1px solid rgba(192, 132, 252, 0.22);
  box-shadow: 0 0 0 1px rgba(192, 132, 252, 0.08), 0 18px 50px rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(8px);
}
.title {
  text-align: center;
  margin: 0 0 18px;
  font-size: 21px;
  font-weight: 900;
  letter-spacing: 0.02em;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: linear-gradient(90deg, #f3eefc 20%, #c084fc 60%, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.gamepad {
  color: #c084fc;
  -webkit-background-clip: initial;
  background-clip: initial;
  background: none;
  filter: drop-shadow(0 0 8px rgba(192, 132, 252, 0.6));
  display: grid;
  place-items: center;
}
.submit {
  width: 100%;
  font-weight: 700;
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  border: 0;
  color: #0a0612;
  box-shadow: 0 0 24px rgba(192, 132, 252, 0.35);
}
.submit:hover {
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  box-shadow: 0 0 36px rgba(192, 132, 252, 0.55);
}

/* ===== Element Plus 深色适配 ===== */
.login-card :deep(.el-tabs__item) {
  color: #a99bc9;
  font-weight: 600;
}
.login-card :deep(.el-tabs__item.is-active) {
  color: #c084fc;
}
.login-card :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #c084fc, #22d3ee);
  box-shadow: 0 0 10px rgba(192, 132, 252, 0.6);
}
.login-card :deep(.el-tabs__nav-wrap::after) {
  background: rgba(192, 132, 252, 0.16);
}
.login-card :deep(.el-input__wrapper) {
  background: rgba(10, 6, 18, 0.55);
  box-shadow: 0 0 0 1px rgba(192, 132, 252, 0.25) inset;
  border-radius: 10px;
  transition: box-shadow 0.2s;
}
.login-card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(192, 132, 252, 0.45) inset;
}
.login-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #22d3ee inset, 0 0 16px rgba(34, 211, 238, 0.2);
}
.login-card :deep(.el-input__inner) {
  color: #f3eefc;
  caret-color: #22d3ee;
}
.login-card :deep(.el-input__inner::placeholder) {
  color: #6f6391;
}
.login-card :deep(.el-input__icon),
.login-card :deep(.el-icon) {
  color: #a99bc9;
}
</style>
