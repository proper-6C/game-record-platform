<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyInfo, getUserStats, getRecordList, getLikedRecords, deleteRecord, sendEmailCode, bindEmail, unbindEmail } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VideoPlay, Calendar, Star, ChatDotRound, Trophy } from '@element-plus/icons-vue'

const router = useRouter()
const me = ref({})
const stats = ref(null)
const activeTab = ref('mine')
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 9 })

// ===== 邮箱绑定 =====
const emailForm = reactive({ email: '', code: '' })
const sending = ref(false)
const binding = ref(false)

async function handleSendBindCode() {
  if (!emailForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  sending.value = true
  try {
    await sendEmailCode({ email: emailForm.email, type: 'bind' })
    ElMessage.success('验证码已发送，请查收邮件')
  } catch {
  } finally {
    sending.value = false
  }
}

async function handleBind() {
  if (!emailForm.email || !emailForm.code) {
    ElMessage.warning('请输入邮箱和验证码')
    return
  }
  binding.value = true
  try {
    const data = await bindEmail({ email: emailForm.email, code: emailForm.code })
    me.value = data
    emailForm.email = ''
    emailForm.code = ''
    ElMessage.success('邮箱绑定成功，支持邮箱验证码登录')
  } catch {
  } finally {
    binding.value = false
  }
}

async function handleUnbind() {
  if (!emailForm.email || !emailForm.code) {
    ElMessage.warning('请输入要解绑的邮箱和验证码（验证码将发送到该邮箱）')
    return
  }
  binding.value = true
  try {
    const data = await unbindEmail({ email: emailForm.email, code: emailForm.code })
    me.value = data
    emailForm.email = ''
    emailForm.code = ''
    ElMessage.success('已解绑邮箱')
  } catch {
  } finally {
    binding.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    const data =
      activeTab.value === 'mine'
        ? await getRecordList({ ...params, userId: me.value.id })
        : await getLikedRecords(params)
    list.value = data.list
    total.value = data.total
  } catch {
  } finally {
    loading.value = false
  }
}

function switchTab() {
  query.page = 1
  load()
}

function openDetail(r) {
  router.push(`/record/${r.id}`)
}

async function remove(r, e) {
  e.stopPropagation()
  try {
    await ElMessageBox.confirm(`确定删除对局「${r.gameName}」吗？其下的评论与点赞记录会一并删除。`, '删除确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteRecord(r.id)
  ElMessage.success('已删除')
  if (list.value.length === 1 && query.page > 1) query.page--
  load()
}

function resultType(r) {
  return { win: 'success', lose: 'danger', draw: 'info' }[r] || 'info'
}
function resultText(r) {
  return { win: '胜利', lose: '失败', draw: '平局' }[r] || ''
}
function initial(nickname) {
  return (nickname || '?').charAt(0)
}

onMounted(async () => {
  me.value = await getMyInfo()
  stats.value = await getUserStats().catch(() => null)
  load()
})
</script>

<template>
  <div class="page-wrap">
    <el-card shadow="never" class="user-card neo-card">
      <div class="user-inner">
        <el-avatar :size="56" class="avatar">{{ initial(me.nickname) }}</el-avatar>
        <div>
          <div class="nickname">{{ me.nickname || '未设置昵称' }}</div>
          <div class="username">@{{ me.username }}</div>
        </div>
        <el-button class="go-upload" @click="router.push('/upload')">上传新对局</el-button>
      </div>
    </el-card>

    <!-- 账号安全：邮箱绑定 -->
    <el-card shadow="never" class="neo-card">
      <div class="email-card">
        <div class="email-head">
          <span class="email-title">账号安全</span>
          <el-tag v-if="me.emailVerified" size="small" type="success" effect="plain">邮箱已验证</el-tag>
        </div>
        <div class="email-current">
          <template v-if="me.email">
            已绑定邮箱：<span class="email-val">{{ me.email }}</span>
          </template>
          <template v-else>
            未绑定邮箱，绑定后可<strong>免密码邮箱验证码登录</strong>
          </template>
        </div>
        <div class="email-ops">
          <el-input
            v-model="emailForm.email"
            :placeholder="me.email ? '输入当前绑定邮箱（解绑）' : '输入要绑定的邮箱'"
            size="default"
            class="email-input"
            clearable
          />
          <el-input
            v-model="emailForm.code"
            placeholder="6 位验证码"
            size="default"
            class="email-input code"
            maxlength="6"
          />
          <el-button class="code-btn" :disabled="sending" @click="handleSendBindCode">
            {{ sending ? '发送中…' : '获取验证码' }}
          </el-button>
          <el-button v-if="me.email" type="danger" plain :loading="binding" @click="handleUnbind">解绑</el-button>
          <el-button v-else type="primary" class="bind-btn" :loading="binding" @click="handleBind">绑定</el-button>
        </div>
        <div class="email-tip">提示：验证码 5 分钟内有效；同一邮箱 60 秒内只能发送一次；一个邮箱最多绑定 2 个账号。</div>
      </div>
    </el-card>

    <!-- 数据看板统计卡片 -->
    <el-card v-if="stats" shadow="never" class="stats-card neo-card">
      <el-row :gutter="16">
        <el-col :xs="12" :sm="6">
          <div class="stat-item">
            <div class="stat-num">{{ stats.totalUploads }}</div>
            <div class="stat-label">上传对局</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-item">
            <div class="stat-num">{{ stats.totalLikesReceived }}</div>
            <div class="stat-label">收到点赞</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-item">
            <div class="stat-num">{{ stats.winRate }}<span class="percent">%</span></div>
            <div class="stat-label">胜率</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-item">
            <div class="stat-num">{{ stats.totalLikesGiven }}</div>
            <div class="stat-label">我赞过的对局</div>
          </div>
        </el-col>
      </el-row>
      <div v-if="stats.topGames.length" class="top-games">
        <span class="top-label">常用游戏</span>
        <el-tag v-for="g in stats.topGames" :key="g.gameName" class="game-tag" effect="plain">
          {{ g.gameName }} ×{{ g.count }}
        </el-tag>
      </div>
    </el-card>

    <el-card shadow="never" class="neo-card">
      <el-tabs v-model="activeTab" @tab-change="switchTab">
        <el-tab-pane label="我的对局" name="mine" />
        <el-tab-pane label="我的点赞" name="liked" />
      </el-tabs>

      <div v-loading="loading">
        <el-empty v-if="!loading && list.length === 0" :description="activeTab === 'mine' ? '还没有上传过对局' : '还没有点赞过的对局'" />
        <el-row :gutter="16">
          <el-col v-for="r in list" :key="r.id" :xs="24" :sm="12" :md="8" :lg="6">
            <el-card
              shadow="hover"
              class="record-card neo-card"
              :body-style="{ padding: '0' }"
              @click="openDetail(r)"
            >
              <div class="cover-box">
                <img v-if="r.coverUrl" :src="r.coverUrl" alt="封面" class="cover-img" />
                <div v-else class="cover-placeholder">
                  <el-icon :size="40"><VideoPlay /></el-icon>
                  <span>视频对局</span>
                </div>
                <el-tag v-if="r.result" :type="resultType(r.result)" class="result-tag" size="small">
                  {{ resultText(r.result) }}
                </el-tag>
              </div>
              <div class="card-body">
                <div class="game-name">{{ r.gameName }}<span v-if="r.gameMode" class="mode">{{ r.gameMode }}</span></div>
                <div class="meta">
                  <span><el-icon><Calendar /></el-icon>{{ r.matchDate }}</span>
                  <span><el-icon><Star /></el-icon>{{ r.likeCount }}</span>
                  <span><el-icon><ChatDotRound /></el-icon>{{ r.commentCount }}</span>
                  <span v-if="r.rank" class="rank"><el-icon><Trophy /></el-icon>{{ r.rank }}</span>
                </div>
                <div class="card-actions">
                  <el-button size="small" type="danger" plain @click="remove(r, $event)">删除</el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
/* 页面纵向布局：分页始终贴底；星空背景铺底，内容浮于其上 */
.page-wrap {
  position: relative;
  z-index: 1;
  min-height: calc(100vh - 110px);
  display: flex;
  flex-direction: column;
}
/* ===== 深色霓虹玻璃卡片（与主页/预览页视觉语言一致） ===== */
.neo-card {
  margin-bottom: 16px;
  background: rgba(21, 16, 34, 0.78) !important;
  border: 1px solid rgba(192, 132, 252, 0.22) !important;
  border-radius: 16px;
  color: #f3eefc;
  box-shadow: 0 0 0 1px rgba(192, 132, 252, 0.06), 0 16px 40px rgba(0, 0, 0, 0.35) !important;
}
.neo-card :deep(.el-card__body) {
  color: #f3eefc;
}
.user-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}
/* ===== 账号安全邮箱绑定 ===== */
.email-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.email-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.email-title {
  font-size: 16px;
  font-weight: 800;
  background: linear-gradient(90deg, #c084fc, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.email-current {
  color: #a99bc9;
  font-size: 14px;
}
.email-val {
  color: #f3eefc;
  font-weight: 600;
  word-break: break-all;
}
.email-ops {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.email-input {
  flex: 1;
  min-width: 180px;
}
.email-input.code {
  max-width: 140px;
}
.email-input :deep(.el-input__wrapper) {
  background: rgba(10, 6, 18, 0.55);
  box-shadow: 0 0 0 1px rgba(192, 132, 252, 0.25) inset;
  border-radius: 10px;
}
.email-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #22d3ee inset, 0 0 16px rgba(34, 211, 238, 0.2);
}
.email-input :deep(.el-input__inner) {
  color: #f3eefc;
  caret-color: #22d3ee;
}
.code-btn {
  background: rgba(192, 132, 252, 0.12);
  border: 1px solid rgba(192, 132, 252, 0.4);
  color: #c084fc;
  font-weight: 600;
  border-radius: 10px;
}
.code-btn:hover {
  background: rgba(192, 132, 252, 0.24);
  border-color: #c084fc;
  color: #f3eefc;
}
.bind-btn {
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  border: 0;
  color: #0a0612;
  font-weight: 700;
}
.bind-btn:hover {
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  color: #0a0612;
  box-shadow: 0 0 20px rgba(192, 132, 252, 0.5);
}
.email-tip {
  color: #6f6391;
  font-size: 12px;
}
.avatar {
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  color: #0a0612;
  box-shadow: 0 0 18px rgba(192, 132, 252, 0.55);
  font-size: 22px;
  font-weight: 800;
}
.nickname {
  font-size: 18px;
  font-weight: 700;
  color: #f3eefc;
}
.username {
  color: #a99bc9;
  font-size: 13px;
  margin-top: 2px;
}
.go-upload {
  margin-left: auto;
  border: 0;
  font-weight: 700;
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  color: #0a0612;
  box-shadow: 0 0 20px rgba(192, 132, 252, 0.35);
}
.go-upload:hover {
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  color: #0a0612;
  box-shadow: 0 0 30px rgba(192, 132, 252, 0.55);
}
.stat-item {
  text-align: center;
  padding: 8px 0;
}
.stat-num {
  font-size: 26px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  background: linear-gradient(90deg, #c084fc, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.stat-num .percent {
  font-size: 15px;
  font-weight: 400;
  color: #a99bc9;
  -webkit-background-clip: initial;
  background-clip: initial;
  background: none;
}
.stat-label {
  color: #a99bc9;
  font-size: 13px;
  margin-top: 2px;
}
.top-games {
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px dashed rgba(192, 132, 252, 0.2);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.top-label {
  color: #a99bc9;
  font-size: 13px;
}
.game-tag {
  font-size: 12px;
  background: rgba(34, 211, 238, 0.08);
  border-color: rgba(34, 211, 238, 0.3);
  color: #67e8f9;
}
.record-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}
.record-card:hover {
  transform: translateY(-4px);
  border-color: rgba(192, 132, 252, 0.45) !important;
  box-shadow: 0 12px 30px rgba(139, 92, 246, 0.28) !important;
}
.cover-box {
  position: relative;
  height: 160px;
  background: linear-gradient(135deg, rgba(192, 132, 252, 0.12), rgba(34, 211, 238, 0.08));
  overflow: hidden;
}
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.35s ease;
}
.record-card:hover .cover-img {
  transform: scale(1.06);
}
.cover-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #a99bc9;
  gap: 6px;
  font-size: 13px;
}
.result-tag {
  position: absolute;
  top: 8px;
  left: 8px;
}
.card-body {
  padding: 12px;
}
.game-name {
  font-weight: 700;
  margin-bottom: 6px;
  color: #f3eefc;
}
.mode {
  margin-left: 8px;
  font-size: 12px;
  color: #a99bc9;
  background: rgba(192, 132, 252, 0.1);
  border: 1px solid rgba(192, 132, 252, 0.2);
  border-radius: 6px;
  padding: 1px 6px;
}
.meta {
  display: flex;
  gap: 14px;
  color: #a99bc9;
  font-size: 13px;
  margin-top: 4px;
  align-items: center;
}
.rank {
  margin-left: auto;
}
.card-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
.card-actions :deep(.el-button--danger.is-plain) {
  background: rgba(251, 113, 133, 0.1);
  border-color: rgba(251, 113, 133, 0.4);
  color: #fb7185;
}
.card-actions :deep(.el-button--danger.is-plain:hover) {
  background: rgba(251, 113, 133, 0.2);
  border-color: #fb7185;
  color: #fb7185;
}
.pager {
  display: flex;
  justify-content: center;
  /* 分页推到页面底部，不与内容粘连 */
  margin-top: auto;
  padding: 14px 0 16px;
  border-top: 1px solid rgba(192, 132, 252, 0.16);
}
.pager :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #a99bc9;
  --el-pagination-button-disabled-bg-color: transparent;
  --el-pagination-hover-color: #c084fc;
}
.pager :deep(.el-pager li) {
  background: transparent;
  color: #a99bc9;
  font-variant-numeric: tabular-nums;
}
.pager :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  color: #0a0612;
  font-weight: 800;
  box-shadow: 0 0 14px rgba(192, 132, 252, 0.4);
}
.pager :deep(.el-pagination button) {
  background: transparent;
  color: #a99bc9;
}
.pager :deep(.el-pagination__total) {
  color: #a99bc9;
}

/* Tabs 深色适配 */
.neo-card :deep(.el-tabs__item) {
  color: #a99bc9;
  font-weight: 600;
}
.neo-card :deep(.el-tabs__item.is-active) {
  color: #c084fc;
}
.neo-card :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #c084fc, #22d3ee);
  box-shadow: 0 0 10px rgba(192, 132, 252, 0.6);
}
.neo-card :deep(.el-tabs__nav-wrap::after) {
  background: rgba(192, 132, 252, 0.16);
}
.neo-card :deep(.el-empty__description p) {
  color: #a99bc9;
}
.neo-card :deep(.el-loading-mask) {
  background: rgba(10, 6, 18, 0.55);
}
</style>
