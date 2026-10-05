import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 统一 axios 实例：开发环境经 vite 代理转发到后端 8080
const http = axios.create({ baseURL: '', timeout: 60000 })

// 请求拦截：自动附带登录 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一处理 { code, message, data }
http.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      ElMessage.error(res.message || '请先登录')
      router.push('/login')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  (error) => {
    ElMessage.error(error.message || '网络错误，请检查后端是否启动')
    return Promise.reject(error)
  }
)

// ==================== 接口定义 ====================

// 用户
export const register = (data) => http.post('/api/user/register', data)
export const login = (data) => http.post('/api/user/login', data)
export const getMyInfo = () => http.get('/api/user/info')
export const getUserStats = () => http.get('/api/user/stats')
export const getUserProfile = (id) => http.get(`/api/user/${id}/profile`)

// 对局
export const getRecordList = (params) => http.get('/api/record/list', { params })
export const getRecordStats = () => http.get('/api/record/stats')
export const getRecordDetail = (id) => http.get(`/api/record/${id}`)
export const updateRecord = (id, data) => http.put(`/api/record/${id}`, data)
export const generateSharePoster = (id) => http.post(`/api/record/${id}/share-poster`)
export const getLikedRecords = (params) => http.get('/api/record/liked', { params })
export const uploadRecord = (formData) =>
  http.post('/api/record/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const deleteRecord = (id) => http.delete(`/api/record/${id}`)

// 评论
export const addComment = (data) => http.post('/api/comment/add', data)
export const getCommentList = (params) => http.get('/api/comment/list', { params })
export const deleteComment = (id) => http.delete(`/api/comment/${id}`)

// 通用文件上传（评论配图等）
export const uploadFile = (formData) =>
  http.post('/api/file/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

// 点赞
export const toggleLike = (data) => http.post('/api/like/toggle', data)
export const getLikeStatus = (params) => http.get('/api/like/status', { params })

// 举报（用户提交 + 管理员处理）
export const addReport = (data) => http.post('/api/report/add', data)
export const getReportList = (params) => http.get('/api/report/list', { params })
export const handleReport = (id, data) => http.post(`/api/report/${id}/handle`, data)
export const restoreRecord = (recordId) => http.post('/api/report/restore-record', null, { params: { recordId } })

// 敏感词管理（管理员）
export const getSensitiveWords = () => http.get('/api/sensitive-word/list')
export const addSensitiveWord = (data) => http.post('/api/sensitive-word/add', data)
export const deleteSensitiveWord = (id) => http.delete(`/api/sensitive-word/${id}`)

// 通知
export const getNotifications = (params) => http.get('/api/notification/list', { params })
export const getUnreadCount = () => http.get('/api/notification/unread-count')
export const markNotificationRead = (id) => http.post(`/api/notification/${id}/read`)
export const markAllNotificationsRead = () => http.post('/api/notification/read-all')

// 邮箱绑定 / 邮箱验证码登录
export const sendEmailCode = (data) => http.post('/api/email/send-code', data)
export const bindEmail = (data) => http.post('/api/email/bind', data)
export const unbindEmail = (data) => http.post('/api/email/unbind', data)
export const emailLogin = (data) => http.post('/api/email/login', data)

export default http
