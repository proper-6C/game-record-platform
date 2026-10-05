<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Sunny, Moon } from '@element-plus/icons-vue'
import { getNotifications, getUnreadCount, markNotificationRead, markAllNotificationsRead } from '../api'

const router = useRouter()
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

// 暗色模式（与全局 html.dark 联动）
const isDark = ref(localStorage.getItem('theme') === 'dark')
function applyDark(v) {
  document.documentElement.classList.toggle('dark', v)
}
function toggleDark() {
  isDark.value = !isDark.value
  applyDark(isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

// 站内通知
const notifList = ref([])
const unreadCount = ref(0)
const notifFlash = ref(false)

// 实时通知（SSE）
let sseSource = null
function connectSse() {
  closeSse()
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    sseSource = new EventSource(`/api/notification/stream?token=${encodeURIComponent(token)}`)
    sseSource.addEventListener('unread', (e) => {
      try {
        const data = JSON.parse(e.data)
        const n = Number(data?.unread ?? 0)
        if (n > unreadCount.value) {
          notifFlash.value = true
          setTimeout(() => (notifFlash.value = false), 1300)
        }
        unreadCount.value = n
      } catch {}
    })
    sseSource.onerror = () => { /* 断线由 EventSource 自动重连 */ }
  } catch {}
}
function closeSse() {
  if (sseSource) {
    sseSource.close()
    sseSource = null
  }
}
watch(isLoggedIn, (v) => (v ? connectSse() : closeSse()))

async function loadUnread() {
  if (!isLoggedIn.value) return
  try {
    const data = await getUnreadCount()
    unreadCount.value = typeof data === 'number' ? data : Number(data?.unread ?? 0)
  } catch {}
}

async function loadNotifs() {
  if (!isLoggedIn.value) return
  try {
    const data = await getNotifications({ page: 1, size: 8 })
    notifList.value = data.list || []
    await loadUnread()
  } catch {}
}

async function openNotif(n) {
  if (n.isRead === 0) {
    n.isRead = 1
    unreadCount.value = Math.max(unreadCount.value - 1, 0)
    markNotificationRead(n.id).catch(() => {})
  }
  if (n.targetId) {
    router.push(`/record/${n.targetId}`)
  }
}

async function markAll() {
  await markAllNotificationsRead()
  notifList.value.forEach((n) => (n.isRead = 1))
  unreadCount.value = 0
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(5, 16)
}

function onAuthChanged() {
  loadUnread()
  connectSse()
}

onMounted(() => {
  applyDark(isDark.value)
  loadUnread()
  connectSse()
  window.addEventListener('auth-changed', onAuthChanged)
})

onBeforeUnmount(() => {
  closeSse()
  window.removeEventListener('auth-changed', onAuthChanged)
})
</script>

<template>
  <el-tooltip :content="isDark ? '切换亮色模式' : '切换暗色模式'" placement="bottom">
    <el-icon class="theme-toggle" :size="17" @click="toggleDark">
      <Sunny v-if="!isDark" />
      <Moon v-else />
    </el-icon>
  </el-tooltip>
  <template v-if="isLoggedIn">
    <el-popover placement="bottom-end" width="330" trigger="click" @show="loadNotifs">
      <template #reference>
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notif-badge" :class="{ flash: notifFlash }">
          <el-icon class="notif-icon" :size="17"><Bell /></el-icon>
        </el-badge>
      </template>
      <div class="notif-panel">
        <div class="notif-head">
          <span class="notif-title">通知</span>
          <el-button v-if="unreadCount > 0" link type="primary" size="small" @click="markAll">全部已读</el-button>
        </div>
        <div v-if="notifList.length === 0" class="notif-empty">暂无通知</div>
        <div
          v-for="n in notifList"
          :key="n.id"
          class="notif-item"
          :class="{ unread: n.isRead === 0 }"
          @click="openNotif(n)"
        >
          <div class="notif-content">{{ n.content }}</div>
          <div class="notif-time">{{ formatTime(n.createTime) }}</div>
        </div>
      </div>
    </el-popover>
  </template>
</template>

<style>
/* 主题切换图标（与全局深色 UI 一致的浅色） */
.theme-toggle {
  color: #c9b8f0;
  cursor: pointer;
  margin-right: 4px;
  vertical-align: middle;
  transition: transform 0.2s ease, color 0.2s ease, text-shadow 0.2s ease;
}
.theme-toggle:hover {
  color: #c084fc;
  transform: scale(1.15) rotate(8deg);
  text-shadow: 0 0 10px rgba(192, 132, 252, 0.6);
}

/* 通知铃铛 */
.notif-badge {
  margin-left: 10px;
  margin-right: 12px;
  padding-right: 14px;
  border-right: 1px solid rgba(192, 132, 252, 0.3);
  cursor: pointer;
  transition: transform 0.2s ease;
}
.notif-badge:hover {
  transform: scale(1.12);
}
.notif-icon {
  color: #c9b8f0;
  vertical-align: middle;
  display: block;
  transition: color 0.2s ease, text-shadow 0.2s ease;
}
.notif-badge:hover .notif-icon {
  color: #c084fc;
  text-shadow: 0 0 10px rgba(192, 132, 252, 0.6);
}
.notif-badge.flash .notif-icon {
  animation: notif-ping 0.7s ease-in-out 2;
}
@keyframes notif-ping {
  0%, 100% { color: #c9b8f0; transform: scale(1); }
  50% { color: #c084fc; transform: scale(1.25) rotate(12deg); text-shadow: 0 0 14px rgba(192, 132, 252, 0.9); }
}

/* 通知面板 */
.notif-panel {
  max-height: 360px;
  display: flex;
  flex-direction: column;
}
.notif-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(192, 132, 252, 0.25);
}
.notif-title {
  font-weight: 600;
  color: #f3eefc;
}
.notif-empty {
  text-align: center;
  color: #a99bc9;
  padding: 24px 0;
  font-size: 13px;
}
.notif-item {
  position: relative;
  padding: 10px 4px;
  border-bottom: 1px dashed rgba(192, 132, 252, 0.25);
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s ease;
}
.notif-item:last-child {
  border-bottom: none;
}
.notif-item:hover {
  background: rgba(192, 132, 252, 0.08);
}
.notif-item.unread {
  background: linear-gradient(90deg, rgba(192, 132, 252, 0.32), rgba(34, 211, 238, 0.12));
  border-left: 4px solid #e879f9;
  box-shadow: inset 0 0 24px rgba(192, 132, 252, 0.14);
}
.notif-item.unread::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 16px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f0abfc;
  box-shadow: 0 0 8px #e879f9;
}
.notif-item.unread .notif-content {
  color: #fdf4ff;
  font-weight: 600;
}
.notif-item.unread .notif-time {
  color: #c9b8f0;
}
.notif-content {
  font-size: 13px;
  color: #d7ccef;
  line-height: 1.5;
}
.notif-time {
  font-size: 12px;
  color: #a99bc9;
  margin-top: 4px;
}
</style>
