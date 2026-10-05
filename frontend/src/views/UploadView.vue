<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadRecord } from '../api'

const router = useRouter()
const uploading = ref(false)

const form = reactive({
  gameName: '',
  gameMode: '',
  matchDate: '',
  result: '',
  rank: '',
  description: '',
  tags: []
})

const selectedFile = ref(null)

const ALLOWED = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'mp4', 'mov']
const MAX_SIZE = 500 * 1024 * 1024

function checkFile(file) {
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  if (!ALLOWED.includes(ext)) {
    ElMessage.error('仅支持 jpg/png/gif/webp 图片或 mp4/mov 视频')
    return false
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error('文件不能超过 500MB')
    return false
  }
  return true
}

function onFileChange(file) {
  if (!checkFile(file.raw)) {
    selectedFile.value = null
    return
  }
  selectedFile.value = file.raw
}

function onFileRemove() {
  selectedFile.value = null
}

async function submit() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择对局文件')
    return
  }
  if (!form.gameName.trim()) {
    ElMessage.warning('请填写游戏名称')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('gameName', form.gameName.trim())
    if (form.gameMode) fd.append('gameMode', form.gameMode)
    if (form.matchDate) fd.append('matchDate', form.matchDate)
    if (form.result) fd.append('result', form.result)
    if (form.rank) fd.append('rank', form.rank)
    if (form.description) fd.append('description', form.description)
    if (form.tags?.length) fd.append('tags', form.tags.slice(0, 5).join(','))

    const record = await uploadRecord(fd)
    ElMessage.success('上传成功')
    router.push(`/record/${record.id}`)
  } catch {
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <el-card shadow="never" class="upload-card">
    <h2>上传对局</h2>

    <el-upload
      drag
      :auto-upload="false"
      :limit="1"
      :on-change="onFileChange"
      :on-remove="onFileRemove"
      class="uploader"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或 <em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip">支持 jpg/png/gif/webp 图片或 mp4/mov 视频，不超过 500MB</div>
      </template>
    </el-upload>

    <el-form label-width="100px" class="record-form">
      <el-form-item label="游戏名称" required>
        <el-input v-model="form.gameName" placeholder="如：王者荣耀" maxlength="100" />
      </el-form-item>
      <el-form-item label="游戏模式">
        <el-input v-model="form.gameMode" placeholder="如：排位 / 匹配" maxlength="50" />
      </el-form-item>
      <el-form-item label="对局日期">
        <el-date-picker v-model="form.matchDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
      </el-form-item>
      <el-form-item label="对局结果">
        <el-select v-model="form.result" placeholder="选择结果" style="width: 200px">
          <el-option label="胜利" value="win" />
          <el-option label="失败" value="lose" />
          <el-option label="平局" value="draw" />
        </el-select>
      </el-form-item>
      <el-form-item label="段位">
        <el-input v-model="form.rank" placeholder="如：王者 / 钻石" maxlength="50" />
      </el-form-item>
      <el-form-item label="对局描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          maxlength="2000"
          show-word-limit
          placeholder="简单描述这局的对战情况…"
        />
      </el-form-item>
      <el-form-item label="标签">
        <el-select
          v-model="form.tags"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="输入标签后回车，如：翻盘 / 五杀 / 逆风局"
          style="width: 100%"
        >
        </el-select>
        <div class="form-tip">回车创建标签，最多 5 个，逗号分隔保存</div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" :loading="uploading" @click="submit">提交上传</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.upload-card {
  max-width: 760px;
  margin: 0 auto;
}
.uploader {
  margin-bottom: 20px;
}
.record-form {
  margin-top: 16px;
}
</style>
