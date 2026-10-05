<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import Starfield from './components/Starfield.vue'
import HeaderActions from './components/HeaderActions.vue'

const route = useRoute()
const router = useRouter()

// 登录态用响应式 ref 维护（localStorage 非响应式，直接读 computed 会导致退出后界面不刷新）
const loggedIn = ref(false)
const userInfo = ref(null)
const isLoggedIn = computed(() => loggedIn.value)
const user = computed(() => userInfo.value)
const isAdmin = computed(() => userInfo.value && userInfo.value.role === 'admin')

function refreshAuth() {
  loggedIn.value = !!localStorage.getItem('token')
  try {
    userInfo.value = JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    userInfo.value = null
  }
}

// 登录 / 退出后路由跳转都会触发，保证顶栏即时刷新
watch(() => route.fullPath, refreshAuth)

// 登录页登录成功时也会派发 auth-changed，保证顶栏即时刷新
onMounted(() => {
  refreshAuth()
  window.addEventListener('auth-changed', refreshAuth)
})

onBeforeUnmount(() => {
  window.removeEventListener('auth-changed', refreshAuth)
})

const logout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    .then(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      // 立即刷新登录态，避免停留在同一路由时顶栏仍显示用户名
      refreshAuth()
      window.dispatchEvent(new Event('auth-changed'))
      router.push('/')
    })
    .catch(() => {})
}
</script>

<template>
  <el-config-provider :locale="zhCn">
    <el-container class="app-shell">
      <!-- 全站子页面星空背景（主页自带星空，排除） -->
      <Starfield v-if="!route.meta.hideNav" />
      <el-header v-if="!route.meta.hideNav" class="app-header">
        <div class="header-left">
          <span class="logo" @click="router.push('/')">
            <span class="logo-mark">GR</span>
            <span class="logo-text">
              <span>游戏对局记录</span>
              <small>GAME RECORD CENTER</small>
            </span>
          </span>
        </div>
        <div class="header-right">
          <button class="nav-link" :class="{ active: route.path === '/records' }" @click="router.push('/records')">全部对局</button>
          <button class="nav-link" :class="{ active: route.path === '/upload' }" @click="router.push('/upload')">上传对局</button>
          <HeaderActions />
          <template v-if="isLoggedIn">
            <el-dropdown>
              <span class="user-info">
                <span class="head-avatar">{{ (user?.nickname || user?.username || '?').charAt(0).toUpperCase() }}</span>
                <span class="head-name">{{ user?.nickname || user?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="isAdmin" @click="router.push('/admin')">管理后台</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <button v-else class="nav-link nav-login" @click="router.push('/login')">登录 / 注册</button>
        </div>
      </el-header>
      <el-main class="app-main" :class="{ 'app-main-full': route.meta.hideNav }">
        <router-view />
      </el-main>
    </el-container>
  </el-config-provider>
</template>

<style>
body {
  margin: 0;
  background: var(--app-bg);
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}
.app-shell { min-height: 100vh; }
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
  padding: 0 28px;
  background: var(--nav-bg);
  border-bottom: 1px solid rgba(192, 132, 252, 0.25);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
  color: #fff;
  /* 确保顶栏位于全屏星空（fixed z-index:0）之上，避免合成层覆盖导致元素不可见 */
  position: relative;
  z-index: 10;
}
.header-left,
.header-right {
  position: relative;
  z-index: 11;
}
.logo {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  transition: color 0.2s ease, opacity 0.2s ease;
}
.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 900;
  color: #0a0612;
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  box-shadow: 0 0 14px rgba(192, 132, 252, 0.55);
}
.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
  background: linear-gradient(90deg, #f3eefc, #c084fc 60%, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.logo-text small {
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 0.14em;
  color: #8f82b5;
  margin-top: 3px;
  background: none;
  -webkit-text-fill-color: #8f82b5;
}
.logo:hover {
  opacity: 0.85;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
/* 主页导航同款文字链接（深色顶栏固定浅色，不依赖主题变量） */
.nav-link {
  border: 0;
  background: transparent;
  color: #c9b8f0;
  font-size: 14px;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s, text-shadow 0.2s;
}
.nav-link:hover {
  color: #c084fc;
  text-shadow: 0 0 12px rgba(192, 132, 252, 0.7);
  background: rgba(192, 132, 252, 0.08);
}
.nav-link.active {
  color: #c084fc;
  background: rgba(192, 132, 252, 0.1);
}
.nav-login {
  color: #22d3ee;
  border: 1px solid rgba(34, 211, 238, 0.4);
  margin-left: 8px;
  text-shadow: 0 0 8px rgba(34, 211, 238, 0.4);
}
.nav-login:hover {
  color: #67e8f9;
  border-color: #22d3ee;
  box-shadow: 0 0 18px rgba(34, 211, 238, 0.25);
}
.user-info {
  margin-left: 4px;
  color: #f3eefc;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  transition: background 0.2s ease, color 0.2s ease;
}
.user-info:hover {
  background: rgba(192, 132, 252, 0.1);
  color: #f3eefc;
}
.head-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  color: #0a0612;
  font-size: 15px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  user-select: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.head-avatar:hover {
  transform: scale(1.1);
  box-shadow: 0 0 0 3px rgba(192, 132, 252, 0.35), 0 0 16px rgba(192, 132, 252, 0.5);
}
.head-name {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #f3eefc;
  font-size: 13.5px;
}
.app-main { padding: 20px; position: relative; z-index: 1; }
.app-main-full { padding: 0 !important; overflow: hidden; }
</style>
