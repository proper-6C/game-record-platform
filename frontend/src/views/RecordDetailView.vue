<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VideoPlay, Star, StarFilled, Picture, Close } from '@element-plus/icons-vue'
import { getRecordDetail, deleteRecord, updateRecord, toggleLike, getCommentList, addComment, deleteComment, uploadFile, generateSharePoster, addReport } from '../api'

const route = useRoute()
const router = useRouter()
const recordId = Number(route.params.id)

const record = ref(null)
const loading = ref(false)

const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentLoading = ref(false)
const newComment = ref('')
const commentImg = ref('')

const replyBox = ref({ commentId: null, toUserId: null, content: '' })
const replyImg = ref('')

// 分享海报
const posterDialog = ref(false)
const posterLoading = ref(false)
const posterUrl = ref('')

// 举报
const reportDialog = ref(false)
const reportReason = ref('')
const reportDetail = ref('')
const reportSubmitting = ref(false)

// 编辑对局
const editDialog = ref(false)
const editSaving = ref(false)
const editForm = ref({ gameName: '', gameMode: '', matchDate: '', result: '', rank: '', description: '', tags: [] })

const me = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    return null
  }
})
const isLoggedIn = computed(() => !!localStorage.getItem('token'))
const isOwner = computed(() => record.value && me.value && record.value.uploaderId === me.value.id)

const resultText = (r) => ({ win: '胜利', lose: '失败', draw: '平局' }[r] || '')
const resultType = (r) => ({ win: 'success', lose: 'danger', draw: 'info' }[r] || 'info')

async function loadDetail() {
  loading.value = true
  try {
    record.value = await getRecordDetail(recordId)
  } catch {
    router.replace('/')
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  commentLoading.value = true
  try {
    const data = await getCommentList({ recordId, page: commentPage.value, size: 10 })
    comments.value = data.list
    commentTotal.value = data.total
  } catch {
  } finally {
    commentLoading.value = false
  }
}

async function likeRecord() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  const data = await toggleLike({ targetId: recordId, targetType: 'record' })
  record.value.liked = data.liked
  record.value.likeCount = data.likeCount
}

async function likeComment(c) {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  const data = await toggleLike({ targetId: c.id, targetType: 'comment' })
  c.liked = data.liked
  c.likeCount = data.likeCount
}

async function submitComment() {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  await addComment({ recordId, content: newComment.value.trim(), imageUrl: commentImg.value || null })
  newComment.value = ''
  commentImg.value = ''
  ElMessage.success('评论成功')
  commentPage.value = 1
  loadComments()
  record.value.commentCount += 1
}

function openReply(c) {
  replyBox.value = { commentId: c.id, toUserId: c.userId, content: '' }
}

async function submitReply() {
  if (!replyBox.value.content.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  await addComment({
    recordId,
    content: replyBox.value.content.trim(),
    parentId: replyBox.value.commentId,
    toUserId: replyBox.value.toUserId,
    imageUrl: replyImg.value || null
  })
  replyBox.value = { commentId: null, toUserId: null, content: '' }
  replyImg.value = ''
  ElMessage.success('回复成功')
  loadComments()
  record.value.commentCount += 1
}

/** 评论/回复选图后上传，拿到 URL 存到预览 */
async function doUploadImage(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  const data = await uploadFile(fd)
  if (option.isReply) {
    replyImg.value = data.url
  } else {
    commentImg.value = data.url
  }
  ElMessage.success('图片已上传')
}

async function removeComment(c) {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteComment(c.id)
  ElMessage.success('已删除')
  loadComments()
  record.value.commentCount = Math.max(0, record.value.commentCount - 1)
}

function canDeleteComment(c) {
  return me.value && c.userId === me.value.id
}

async function removeRecord() {
  try {
    await ElMessageBox.confirm('删除后将同时删除所有评论和点赞，确定吗？', '警告', { type: 'warning' })
  } catch {
    return
  }
  await deleteRecord(recordId)
  ElMessage.success('已删除对局')
  router.replace('/')
}

async function showPoster() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  posterLoading.value = true
  posterDialog.value = true
  try {
    const data = await generateSharePoster(recordId)
    posterUrl.value = data.url
  } catch {
    posterUrl.value = ''
    ElMessage.error('海报生成失败')
  } finally {
    posterLoading.value = false
  }
}

function openReport() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  reportReason.value = ''
  reportDetail.value = ''
  reportDialog.value = true
}

async function submitReport() {
  if (!reportReason.value) {
    ElMessage.warning('请选择举报原因')
    return
  }
  reportSubmitting.value = true
  try {
    await addReport({ recordId, reason: reportReason.value, detail: reportDetail.value.trim() || null })
    ElMessage.success('举报成功，等待管理员处理')
    reportDialog.value = false
  } finally {
    reportSubmitting.value = false
  }
}

function openEdit() {
  editForm.value = {
    gameName: record.value.gameName,
    gameMode: record.value.gameMode || '',
    matchDate: record.value.matchDate || '',
    result: record.value.result || '',
    rank: record.value.rank || '',
    description: record.value.description || '',
    tags: record.value.tags ? [...record.value.tags] : []
  }
  editDialog.value = true
}

async function saveEdit() {
  if (!editForm.value.gameName.trim()) {
    ElMessage.warning('请输入游戏名称')
    return
  }
  editSaving.value = true
  try {
    await updateRecord(recordId, {
      gameName: editForm.value.gameName.trim(),
      gameMode: editForm.value.gameMode || null,
      matchDate: editForm.value.matchDate || null,
      result: editForm.value.result || null,
      rank: editForm.value.rank || null,
      description: editForm.value.description || null,
      tags: editForm.value.tags.slice(0, 5).join(',')
    })
    ElMessage.success('已保存')
    editDialog.value = false
    await loadDetail()
  } finally {
    editSaving.value = false
  }
}

onMounted(() => {
  loadDetail()
  loadComments()
})
</script>

<template>
  <div v-loading="loading" class="page-wrap">
    <el-card v-if="record" shadow="never">
      <el-page-header content="对局详情" @back="router.push('/')">
        <template #extra>
          <el-button v-if="!isOwner" type="warning" plain @click="openReport">举报</el-button>
          <el-button type="success" plain @click="showPoster">分享海报</el-button>
          <el-button v-if="isOwner" type="primary" plain @click="openEdit">编辑</el-button>
          <el-button v-if="isOwner" type="danger" plain @click="removeRecord">删除对局</el-button>
        </template>
      </el-page-header>

      <div class="player">
        <video v-if="record.videoUrl" :src="record.videoUrl" controls class="media" />
        <img v-else-if="record.coverUrl" :src="record.coverUrl" alt="封面" class="media" />
        <div v-else class="media media-placeholder">
          <el-icon :size="48"><VideoPlay /></el-icon>
          <span>暂无媒体</span>
        </div>
      </div>

      <div class="info-head">
        <h2>
          {{ record.gameName }}
          <el-tag v-if="record.result" :type="resultType(record.result)" size="small">
            {{ resultText(record.result) }}
          </el-tag>
        </h2>
        <el-button
          :type="record.liked ? 'danger' : 'default'"
          :class="{ 'liked-btn': record.liked }"
          @click="likeRecord"
        >
          <el-icon><component :is="record.liked ? StarFilled : Star" /></el-icon>&nbsp;{{ record.liked ? '已点赞' : '点赞' }} {{ record.likeCount }}
        </el-button>
      </div>

      <el-descriptions :column="2" border class="desc">
        <el-descriptions-item label="游戏模式">{{ record.gameMode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="对局日期">{{ record.matchDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="段位">{{ record.rank || '—' }}</el-descriptions-item>
        <el-descriptions-item label="上传者">{{ record.uploaderName }}</el-descriptions-item>
        <el-descriptions-item label="对局描述" :span="2">{{ record.description || '—' }}</el-descriptions-item>
        <el-descriptions-item label="标签" :span="2">
          <template v-if="record.tags?.length">
            <el-tag v-for="t in record.tags" :key="t" size="small" type="info" effect="plain" class="detail-tag">
              {{ t }}
            </el-tag>
          </template>
          <span v-else>—</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 分享海报弹窗 -->
    <el-dialog v-model="posterDialog" title="分享海报" width="520px" align-center>
      <div v-loading="posterLoading" class="poster-wrap">
        <img v-if="posterUrl" :src="posterUrl" alt="分享海报" class="poster-img" />
        <div v-else class="poster-empty">生成中…</div>
      </div>
      <template #footer>
        <a v-if="posterUrl" :href="posterUrl" download="game-record-poster.png" class="el-button el-button--primary">
          下载海报
        </a>
        <el-button @click="posterDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对局弹窗 -->
    <el-dialog v-model="editDialog" title="编辑对局" width="520px" align-center>
      <el-form label-width="80px">
        <el-form-item label="游戏名称" required>
          <el-input v-model="editForm.gameName" maxlength="100" />
        </el-form-item>
        <el-form-item label="游戏模式">
          <el-input v-model="editForm.gameMode" maxlength="50" placeholder="如：排位 / 匹配" />
        </el-form-item>
        <el-form-item label="对局日期">
          <el-date-picker v-model="editForm.matchDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="对局结果">
          <el-select v-model="editForm.result" style="width: 100%">
            <el-option label="胜利" value="win" />
            <el-option label="失败" value="lose" />
            <el-option label="平局" value="draw" />
          </el-select>
        </el-form-item>
        <el-form-item label="段位">
          <el-input v-model="editForm.rank" maxlength="50" placeholder="如：王者 / 钻石" />
        </el-form-item>
        <el-form-item label="对局描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="editForm.tags" multiple filterable allow-create default-first-option style="width: 100%">
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 举报弹窗 -->
    <el-dialog v-model="reportDialog" title="举报该对局" width="480px" align-center>
      <el-form label-width="80px">
        <el-form-item label="举报原因" required>
          <el-radio-group v-model="reportReason">
            <el-radio value="违规内容">违规内容</el-radio>
            <el-radio value="色情低俗">色情低俗</el-radio>
            <el-radio value="垃圾广告">垃圾广告</el-radio>
            <el-radio value="不友善言论">不友善言论</el-radio>
            <el-radio value="其他">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input
            v-model="reportDetail"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="可选，最多500字"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button type="danger" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>

    <el-card v-if="record" shadow="never" class="comment-card">
      <template #header>
        <span>评论 ({{ record.commentCount }})</span>
      </template>

      <div class="comment-input">
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="发表你的看法…"
        />
        <div class="input-bar">
          <el-upload :show-file-list="false" accept="image/*" :http-request="(o) => doUploadImage(o)" class="img-upload">
            <el-button size="small" :icon="Picture">配图</el-button>
          </el-upload>
          <div v-if="commentImg" class="img-preview">
            <el-image :src="commentImg" fit="cover" class="preview-img" />
            <el-icon class="img-close" @click="commentImg = ''"><Close /></el-icon>
          </div>
          <el-button type="primary" style="margin-left: auto" @click="submitComment">发表评论</el-button>
        </div>
      </div>

      <div v-loading="commentLoading">
        <div v-if="comments.length === 0" class="empty-comment">还没有评论，快来抢沙发</div>
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <el-avatar :size="36" class="avatar">{{ (c.nickname || '?').charAt(0) }}</el-avatar>
          <div class="comment-body">
            <div class="comment-head">
              <span class="nickname">{{ c.nickname }}</span>
              <span class="time">{{ c.createTime?.replace('T', ' ').slice(0, 19) }}</span>
            </div>
            <div class="comment-content">{{ c.content }}</div>
            <el-image
              v-if="c.imageUrl"
              :src="c.imageUrl"
              :preview-src-list="[c.imageUrl]"
              fit="cover"
              class="comment-img"
              preview-teleported
            />
            <div class="comment-actions">
              <el-button link :type="c.liked ? 'danger' : 'primary'" @click="likeComment(c)">
                <el-icon><component :is="c.liked ? StarFilled : Star" /></el-icon>&nbsp;{{ c.liked ? '已赞' : '赞' }} {{ c.likeCount || 0 }}
              </el-button>
              <el-button link type="primary" @click="openReply(c)">回复</el-button>
              <el-button v-if="canDeleteComment(c)" link type="danger" @click="removeComment(c)">删除</el-button>
            </div>

            <div v-for="r in c.replyList" :key="r.id" class="reply-item">
              <span class="nickname">{{ r.nickname }}</span>
              <span v-if="r.toUserName" class="to">回复 {{ r.toUserName }}：</span>
              <span class="reply-content">{{ r.content }}</span>
              <el-image
                v-if="r.imageUrl"
                :src="r.imageUrl"
                :preview-src-list="[r.imageUrl]"
                fit="cover"
                class="comment-img reply-img"
                preview-teleported
              />
              <div class="comment-actions">
                <el-button link :type="r.liked ? 'danger' : 'primary'" @click="likeComment(r)">
                  <el-icon><component :is="r.liked ? StarFilled : Star" /></el-icon>&nbsp;{{ r.liked ? '已赞' : '赞' }} {{ r.likeCount || 0 }}
                </el-button>
                <el-button link type="primary" @click="openReply(c)">回复</el-button>
                <el-button v-if="canDeleteComment(r)" link type="danger" @click="removeComment(r)">删除</el-button>
              </div>
            </div>

            <div v-if="replyBox.commentId === c.id" class="reply-input">
              <el-input v-model="replyBox.content" placeholder="回复…" maxlength="500" @keyup.enter="submitReply" />
              <div class="input-bar">
                <el-upload :show-file-list="false" accept="image/*" :http-request="(o) => doUploadImage({ ...o, isReply: true })" class="img-upload">
                  <el-button size="small" :icon="Picture">配图</el-button>
                </el-upload>
                <div v-if="replyImg" class="img-preview">
                  <el-image :src="replyImg" fit="cover" class="preview-img" />
                  <el-icon class="img-close" @click="replyImg = ''"><Close /></el-icon>
                </div>
                <el-button type="primary" size="small" style="margin-left: auto" @click="submitReply">提交回复</el-button>
              </div>
            </div>
          </div>
        </div>
        <div class="pager" v-if="commentTotal > 10">
          <el-pagination
            v-model:current-page="commentPage"
            :page-size="10"
            :total="commentTotal"
            layout="prev, pager, next"
            @current-change="loadComments"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.player {
  margin: 16px 0;
  background: var(--cover-ph);
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  justify-content: center;
  max-height: 480px;
}
.media {
  max-width: 100%;
  max-height: 480px;
  object-fit: contain;
  display: block;
}
.media-placeholder {
  min-height: 240px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-sub);
  gap: 8px;
}
.info-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.info-head h2 {
  margin: 0;
}
.liked-btn {
  /* 已点赞：使用 Element Plus danger 按钮默认样式（红底白字），不额外覆盖文字颜色 */
}
.detail-tag {
  margin-right: 8px;
}
.desc {
  margin-bottom: 8px;
}
.comment-card {
  margin-top: 16px;
}
.comment-input {
  margin-bottom: 16px;
}
.input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.img-preview {
  position: relative;
  display: inline-flex;
}
.preview-img {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  display: block;
}
.img-close {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #f56c6c;
  color: #fff;
  border-radius: 50%;
  padding: 2px;
  cursor: pointer;
}
.comment-img {
  width: 140px;
  height: 140px;
  border-radius: 8px;
  margin-top: 6px;
  display: block;
  cursor: zoom-in;
}
.reply-img {
  width: 100px;
  height: 100px;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}
.comment-body {
  flex: 1;
}
.comment-head {
  display: flex;
  gap: 10px;
  align-items: center;
}
.nickname {
  font-weight: 600;
}
.time {
  color: var(--text-sub);
  font-size: 12px;
}
.comment-content {
  margin: 4px 0;
  line-height: 1.6;
}
.comment-actions {
  display: flex;
  gap: 4px;
  font-size: 12px;
}
.reply-item {
  background: var(--soft-bg);
  border-radius: 6px;
  padding: 8px 12px;
  margin-top: 8px;
  font-size: 14px;
}
.to {
  color: var(--accent);
}
.reply-input {
  margin-top: 8px;
}
.empty-comment {
  color: var(--text-sub);
  text-align: center;
  padding: 20px 0;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}
.poster-wrap {
  display: flex;
  justify-content: center;
  min-height: 300px;
}
.poster-img {
  max-width: 100%;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
}
.poster-empty {
  color: var(--text-sub);
  align-self: center;
}

</style>
