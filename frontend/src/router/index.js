import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { hideNav: true } },
  { path: '/records', name: 'records', component: () => import('../views/RecordListView.vue') },
  { path: '/record/:id', name: 'detail', component: () => import('../views/RecordDetailView.vue') },
  { path: '/user/:id', name: 'user-profile', component: () => import('../views/UserProfileView.vue') },
  { path: '/upload', name: 'upload', component: () => import('../views/UploadView.vue'), meta: { requiresAuth: true } },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { requiresAuth: true } },
  { path: '/admin', name: 'admin', component: () => import('../views/AdminView.vue'), meta: { requiresAuth: true } },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：需要登录的页面校验 token
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
