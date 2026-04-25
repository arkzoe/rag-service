import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'Layout',
      component: () => import('@/views/layout/LayoutView.vue'),
      redirect: '/chat',
      children: [
        {
          path: 'chat',
          name: 'Chat',
          component: () => import('@/views/chat/ChatView.vue'),
          meta: { title: '智能对话' },
        },
        {
          path: 'upload',
          name: 'Upload',
          component: () => import('@/views/upload/UploadView.vue'),
          meta: { title: '文件上传' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to, from) => {
  const userStore = useUserStore()

  if (!to.meta.public && !userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return '/login'
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    return '/'
  }
})

export default router
