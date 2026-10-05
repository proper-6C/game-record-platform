<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRecordList } from '../api'
import { VideoPlay, User, Calendar, Star, ChatDotRound, Trophy } from '@element-plus/icons-vue'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 9, gameName: '', rank: '', tag: '', sortBy: 'latest' })

async function load() {
  loading.value = true
  try {
    const data = await getRecordList({
      page: query.page,
      size: query.size,
      gameName: query.gameName || undefined,
      rank: query.rank || undefined,
      tag: query.tag || undefined,
      sortBy: query.sortBy
    })
    list.value = data.list
    total.value = data.total
  } catch {
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function resultType(r) {
  return { win: 'success', lose: 'danger', draw: 'info' }[r] || 'info'
}
function resultText(r) {
  return { win: '胜利', lose: '失败', draw: '平局' }[r] || ''
}
function cover(record) {
  return record.coverUrl || ''
}

onMounted(load)
</script>

<template>
  <div class="page-wrap">
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-inner">
        <el-input
          v-model="query.gameName"
          placeholder="搜索游戏名称"
          clearable
          style="width: 200px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-input
          v-model="query.rank"
          placeholder="筛选段位"
          clearable
          style="width: 150px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-input
          v-model="query.tag"
          placeholder="筛选标签"
          clearable
          style="width: 150px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-radio-group v-model="query.sortBy" @change="search">
          <el-radio-button value="latest">最新发布</el-radio-button>
          <el-radio-button value="hot">热门点赞</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="search">搜索</el-button>
      </div>
    </el-card>

    <div v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无对局记录，快去上传第一局吧" />
      <el-row :gutter="16">
        <el-col v-for="r in list" :key="r.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card
            shadow="hover"
            class="record-card"
            :body-style="{ padding: '0' }"
            @click="router.push(`/record/${r.id}`)"
          >
            <div class="cover-box">
              <img v-if="cover(r)" :src="cover(r)" alt="封面" class="cover-img" />
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
              <div v-if="r.tags?.length" class="tag-row">
                <el-tag v-for="t in r.tags" :key="t" size="small" type="info" effect="plain" class="mini-tag">
                  {{ t }}
                </el-tag>
              </div>
              <div class="meta">
                <span class="author-link" @click.stop="router.push(`/user/${r.uploaderId}`)">
                  <el-icon><User /></el-icon>{{ r.uploaderName }}
                </span>
                <span><el-icon><Calendar /></el-icon>{{ r.matchDate }}</span>
              </div>
              <div class="meta">
                <span><el-icon><Star /></el-icon>{{ r.likeCount }}</span>
                <span><el-icon><ChatDotRound /></el-icon>{{ r.commentCount }}</span>
                <span v-if="r.rank" class="rank"><el-icon><Trophy /></el-icon>{{ r.rank }}</span>
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
  </div>
</template>

<style scoped>
/* 页面纵向布局：分页始终贴底 */
.page-wrap {
  min-height: calc(100vh - 110px);
  display: flex;
  flex-direction: column;
}
.toolbar {
  margin-bottom: 16px;
}
.toolbar-inner {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}
.record-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.record-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(139, 92, 246, 0.22);
}
.cover-box {
  position: relative;
  height: 160px;
  background: var(--cover-ph);
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
  color: var(--text-sub);
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
  font-weight: 600;
  margin-bottom: 6px;
}
.mode {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-sub);
  background: var(--soft-bg);
  border-radius: 4px;
  padding: 1px 6px;
}
.tag-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}
.mini-tag {
  font-size: 11px;
  padding: 0 6px;
  height: 20px;
  line-height: 20px;
}
.meta {
  display: flex;
  gap: 14px;
  color: var(--text-sub);
  font-size: 13px;
  margin-top: 4px;
  align-items: center;
}
.author-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: var(--text-sub);
  transition: color 0.2s, text-shadow 0.2s;
}
.author-link:hover {
  color: var(--accent);
  text-shadow: 0 0 10px rgba(192, 132, 252, 0.5);
}
.pager {
  display: flex;
  justify-content: center;
  /* 分页推到页面底部，不与卡片粘连 */
  margin-top: auto;
  padding: 14px 0 16px;
  border-top: 1px solid var(--border-color);
}
</style>
