<template>
  <div class="profile-page">
    <div v-if="loading" class="profile-loading" v-loading="true" element-loading-text="加载中" style="min-height: 300px" />
    <template v-else-if="profile">
      <!-- 玩家信息头 -->
      <section class="profile-head">
        <div class="head-avatar">{{ (profile.nickname || profile.username || '?').charAt(0).toUpperCase() }}</div>
        <div class="head-info">
          <h1 class="head-name">
            {{ profile.nickname || profile.username }}
            <el-tag v-if="profile.role === 'admin'" size="small" type="warning" effect="dark" class="admin-tag">管理员</el-tag>
          </h1>
          <p class="head-id">@{{ profile.username }}</p>
          <p class="head-time">加入于 {{ formatDate(profile.createTime) }}</p>
        </div>
        <div class="head-stats">
          <div class="stat-cell">
            <b>{{ profile.recordCount }}</b>
            <span>作品</span>
          </div>
          <div class="stat-cell">
            <b>{{ profile.totalLikes }}</b>
            <span>获赞</span>
          </div>
        </div>
      </section>

      <!-- 作品墙 -->
      <section class="works">
        <div class="works-title">
          <span class="eyebrow">WORKS</span>
          <h2>作品墙</h2>
        </div>
        <div v-if="!listLoading && list.length === 0" class="works-empty">
          <el-empty description="TA 还没有上传对局" />
        </div>
        <div v-loading="listLoading" class="works-grid">
          <div v-for="r in list" :key="r.id" class="work-card" @click="router.push(`/record/${r.id}`)">
            <div class="work-cover">
              <img v-if="cover(r)" :src="cover(r)" alt="封面" class="work-img" />
              <div v-else class="work-ph">
                <el-icon :size="36"><VideoPlay /></el-icon>
                <span>视频对局</span>
              </div>
              <el-tag v-if="r.result" :type="resultType(r.result)" class="work-result" size="small">
                {{ resultText(r.result) }}
              </el-tag>
            </div>
            <div class="work-body">
              <div class="work-name">{{ r.gameName }}<span v-if="r.gameMode" class="work-mode">{{ r.gameMode }}</span></div>
              <div v-if="r.tags?.length" class="work-tags">
                <el-tag v-for="t in r.tags" :key="t" size="small" type="info" effect="plain" class="mini-tag">{{ t }}</el-tag>
              </div>
              <div class="work-meta">
                <span><el-icon><Star /></el-icon>{{ r.likeCount }}</span>
                <span><el-icon><ChatDotRound /></el-icon>{{ r.commentCount }}</span>
                <span class="work-date">{{ String(r.matchDate || '').slice(0, 10) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="total > 0" class="pager">
          <el-pagination
            background
            layout="prev, pager, next, total"
            :total="total"
            :page-size="size"
            :current-page="page"
            @current-change="onPage"
          />
        </div>
      </section>
    </template>
    <template v-else>
      <el-empty description="用户不存在或已被删除" />
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { VideoPlay, Star, ChatDotRound } from '@element-plus/icons-vue'
import { getUserProfile, getRecordList } from '../api'

const route = useRoute()
const router = useRouter()

const profile = ref(null)
const loading = ref(true)

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 8
const listLoading = ref(false)

function cover(r) {
  return r.coverUrl || ''
}
function resultText(r) {
  return { win: '胜利', lose: '败北', draw: '平局' }[r] || ''
}
function resultType(r) {
  return { win: 'success', lose: 'danger', draw: 'warning' }[r] || 'info'
}
function formatDate(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 10)
}

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getUserProfile(route.params.id)
  } catch {
    profile.value = null
  } finally {
    loading.value = false
  }
}

async function loadWorks() {
  listLoading.value = true
  try {
    const data = await getRecordList({ page: page.value, size, sortBy: 'latest', userId: route.params.id })
    list.value = data.list || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    listLoading.value = false
  }
}

function onPage(p) {
  page.value = p
  loadWorks()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  loadProfile()
  loadWorks()
})
</script>

<style scoped>
.profile-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 8px 4px 24px;
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(192, 132, 252, 0.12), rgba(34, 211, 238, 0.1)), var(--card-bg);
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 30px rgba(139, 92, 246, 0.12);
  flex-wrap: wrap;
}
.head-avatar {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  font-size: 34px;
  font-weight: 900;
  color: #0a0612;
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  box-shadow: 0 0 24px rgba(192, 132, 252, 0.55);
}
.head-info { flex: 1; min-width: 200px; }
.head-name {
  margin: 0;
  font-size: 26px;
  font-weight: 900;
  color: var(--text-main);
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(90deg, var(--text-main), #c084fc 70%, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.admin-tag { -webkit-text-fill-color: currentColor; }
.head-id { margin: 6px 0 0; color: var(--text-sub); font-size: 14px; }
.head-time { margin: 4px 0 0; color: var(--text-sub); font-size: 12.5px; }
.head-stats { display: flex; gap: 30px; }
.stat-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 12px 22px;
  border-radius: 14px;
  background: var(--soft-bg);
  border: 1px solid var(--border-color);
  min-width: 86px;
}
.stat-cell b {
  font-size: 22px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.stat-cell span { font-size: 12px; color: var(--text-sub); }

.works { margin-top: 26px; }
.works-title { display: flex; align-items: baseline; gap: 12px; margin-bottom: 14px; }
.works-title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
  background: linear-gradient(90deg, #c084fc, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.works-title .eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--text-sub);
}
.works-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.work-card {
  border-radius: 14px;
  overflow: hidden;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s;
}
.work-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 0 0 1px rgba(192, 132, 252, 0.35), 0 12px 28px rgba(139, 92, 246, 0.22);
  border-color: rgba(192, 132, 252, 0.4);
}
.work-cover { position: relative; aspect-ratio: 16 / 9; background: var(--cover-ph); overflow: hidden; }
.work-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.work-ph {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-sub);
  font-size: 13px;
}
.work-result { position: absolute; top: 8px; left: 8px; }
.work-body { padding: 12px 14px 14px; }
.work-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.work-mode {
  margin-left: 6px;
  font-size: 11.5px;
  font-weight: 400;
  color: var(--text-sub);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 0 5px;
}
.work-tags { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
.mini-tag { margin-right: 0; }
.work-meta {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12.5px;
  color: var(--text-sub);
  font-variant-numeric: tabular-nums;
}
.work-meta span { display: inline-flex; align-items: center; gap: 4px; }
.work-date { margin-left: auto; }
.pager { margin-top: 22px; display: flex; justify-content: center; }
.works-empty { padding: 40px 0; }

@media (max-width: 1024px) {
  .works-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 720px) {
  .works-grid { grid-template-columns: repeat(2, 1fr); }
  .profile-head { gap: 16px; padding: 20px; }
  .head-avatar { width: 64px; height: 64px; font-size: 26px; }
  .head-stats { gap: 12px; }
  .stat-cell { padding: 10px 14px; min-width: 70px; }
}
@media (max-width: 420px) {
  .works-grid { grid-template-columns: 1fr; }
}
</style>
