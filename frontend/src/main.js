import { createApp } from 'vue'
import { ElLoading } from 'element-plus'
// 函数式组件（ElMessage / ElMessageBox / v-loading）样式需手动引入
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
// Element Plus 暗色模式变量（html.dark 时生效）
import 'element-plus/theme-chalk/dark/css-vars.css'
import './style.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

// 全局注册 v-loading 指令（组件按需引入后，指令需显式注册）
app.directive('loading', ElLoading.directive)

app.use(router)
app.mount('#app')
