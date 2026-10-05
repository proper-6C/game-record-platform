<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReportList, handleReport, restoreRecord, getSensitiveWords, addSensitiveWord, deleteSensitiveWord } from '../api'

const router = useRouter()

const me = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    return null
  }
})
const isAdmin = computed(() => me.value && me.value.role === 'admin')

const activeStatus = ref('0') // '0' 待处理 / '1' 已处理
const reports = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const handleLoading = ref(false)

// 敏感词管理
const mainTab = ref('report')
const sensitiveWords = ref([])
const newWord = ref('')
const wordAdding = ref(false)

const statusType = (s) => (s === 0 ? 'danger' : 'success')
const statusText = (s) => (s === 0 ? '待处理' : '已处理')
const recordStatusType = (s) => (s === 1 ? 'danger' : 'success')
const recordStatusText = (s) => (s === 1 ? '已下架' : '正常')

async function load() {
  loading.value = true
  try {
    const data = await getReportList({ page: page.value, size: 10, status: Number(activeStatus.value) })
    reports.value = data.list
    total.value = data.total
  } catch (e) {
    if (e?.response?.status === 403 || (e?.message && String(e.message).includes('管理员'))) {
      ElMessage.error('仅管理员可访问')
      router.replace('/')
    }
  } finally {
    loading.value = false
  }
}

function switchTab(s) {
  activeStatus.value = s
  page.value = 1
  load()
}

/** 驳回举报 */
async function dismiss(r) {
  try {
    await ElMessageBox.confirm('确定驳回该举报吗？', '驳回举报', { type: 'warning' })
  } catch {
    return
  }
  handleLoading.value = true
  try {
    await handleReport(r.id, { result: '举报不成立，已驳回', takeDown: false })
    ElMessage.success('已驳回')
    load()
  } finally {
    handleLoading.value = false
  }
}

/** 确认违规并下架对局 */
async function takeDown(r) {
  try {
    await ElMessageBox.confirm(`确定对《${r.recordName}》下架处理吗？下架后普通用户将看不到该对局。`, '下架对局', { type: 'warning' })
  } catch {
    return
  }
  handleLoading.value = true
  try {
    await handleReport(r.id, { result: '确认违规，对局已下架', takeDown: true })
    ElMessage.success('已下架该对局')
    load()
  } finally {
    handleLoading.value = false
  }
}

/** 恢复上架 */
async function restore(r) {
  try {
    await ElMessageBox.confirm(`确定恢复《${r.recordName}》上架吗？`, '恢复上架', { type: 'info' })
  } catch {
    return
  }
  await restoreRecord(r.recordId)
  ElMessage.success('已恢复上架')
  load()
}

/** 敏感词列表 */
async function loadWords() {
  try {
    sensitiveWords.value = await getSensitiveWords()
  } catch {
    sensitiveWords.value = []
  }
}

async function addWord() {
  const word = newWord.value.trim()
  if (!word) {
    ElMessage.warning('请输入敏感词')
    return
  }
  wordAdding.value = true
  try {
    await addSensitiveWord({ word })
    ElMessage.success(`已添加：${word}`)
    newWord.value = ''
    await loadWords()
  } catch (e) {
    ElMessage.error(e?.message || '添加失败（可能已存在）')
  } finally {
    wordAdding.value = false
  }
}

async function removeWord(item) {
  try {
    await ElMessageBox.confirm(`确定删除敏感词「${item.word}」吗？`, '删除敏感词', { type: 'warning' })
  } catch {
    return
  }
  await deleteSensitiveWord(item.id)
  ElMessage.success('已删除')
  await loadWords()
}

onMounted(() => {
  if (!isAdmin.value) {
    ElMessage.warning('仅管理员可访问管理后台')
    router.replace('/')
    return
  }
  load()
  loadWords()
})
</script>

<template>
  <div class="admin-page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span class="title">管理后台</span>
        </div>
      </template>

      <el-tabs v-model="mainTab">
        <el-tab-pane label="举报管理" name="report">
          <div class="sub-bar">
            <el-tag v-if="total > 0" type="danger" effect="plain">
              待处理 {{ activeStatus === '0' ? total : reports.length }}
            </el-tag>
          </div>
          <el-tabs v-model="activeStatus" @tab-change="switchTab">
            <el-tab-pane label="待处理" name="0" />
            <el-tab-pane label="已处理" name="1" />
          </el-tabs>

      <div v-loading="loading">
        <el-table :data="reports" stripe empty-text="暂无举报">
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="被举报对局" min-width="180">
            <template #default="{ row }">
              <div class="record-cell">
                <span class="game">{{ row.recordName || '—' }}</span>
                <el-tag :type="recordStatusType(row.recordStatus)" size="small" effect="plain">
                  {{ recordStatusText(row.recordStatus) }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="上传者" prop="uploaderName" width="100" />
          <el-table-column label="举报人" prop="reporterName" width="100" />
          <el-table-column label="原因" prop="reason" width="110" />
          <el-table-column label="补充说明" prop="detail" min-width="140" show-overflow-tooltip />
          <el-table-column label="举报时间" width="160">
            <template #default="{ row }">{{ row.createTime?.replace('T', ' ').slice(0, 19) }}</template>
          </el-table-column>
          <el-table-column label="处理结果" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.status === 1">{{ row.handleResult }}</span>
              <span v-else class="pending">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 0">
                <el-button link type="warning" size="small" @click="dismiss(row)">驳回</el-button>
                <el-button link type="danger" size="small" @click="takeDown(row)">下架</el-button>
              </template>
              <template v-else>
                <el-button v-if="row.recordStatus === 1" link type="primary" size="small" @click="restore(row)">
                  恢复上架
                </el-button>
                <span v-else class="done">已处理</span>
              </template>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="total > 10" class="pager">
          <el-pagination
            v-model:current-page="page"
            :page-size="10"
            :total="total"
            layout="prev, pager, next"
            @current-change="load"
          />
        </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="敏感词管理" name="sensitive">
          <div class="word-bar">
            <el-input v-model="newWord" placeholder="输入要拦截的敏感词，回车添加" maxlength="50" style="width: 300px" @keyup.enter="addWord" />
            <el-button type="primary" :loading="wordAdding" @click="addWord">添加</el-button>
          </div>
          <div class="word-list">
            <el-tag v-for="item in sensitiveWords" :key="item.id" closable class="word-tag" @close="removeWord(item)">
              {{ item.word }}
            </el-tag>
            <div v-if="sensitiveWords.length === 0" class="word-empty">暂无敏感词</div>
          </div>
          <p class="word-tip">评论与回复发表时将自动把命中词替换为「*」，添加后立即生效。</p>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.admin-page {
  max-width: 1200px;
  margin: 0 auto;
  /* 页面纵向布局：分页始终贴底 */
  min-height: calc(100vh - 110px);
  display: flex;
  flex-direction: column;
}
.head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.title {
  font-weight: 600;
}
.record-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.game {
  font-weight: 500;
}
.sub-bar {
  margin-bottom: 8px;
}
.word-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.word-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 40px;
}
.word-tag {
  font-size: 14px;
  padding: 6px 12px;
}
.word-empty {
  color: var(--text-sub);
}
.word-tip {
  color: var(--text-sub);
  font-size: 13px;
  margin-top: 16px;
}
.pending {
  color: var(--text-sub);
}
.done {
  color: var(--text-sub);
  font-size: 13px;
}
.pager {
  display: flex;
  justify-content: center;
  /* 分页推到页面底部，不与内容粘连 */
  margin-top: auto;
  padding: 14px 0 16px;
  border-top: 1px solid var(--border-color);
}
</style>
