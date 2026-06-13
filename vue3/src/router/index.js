import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import BackendLayout from '@/layouts/BackendLayout.vue'

// 后台路由
export const backendRoutes = [
  {
    path: '/back',
    component: BackendLayout,
    redirect: '/back/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/backend/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'user',
        name: 'UserManagement',
        component: () => import('@/views/backend/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'article',
        name: 'ArticleManagement',
        component: () => import('@/views/backend/article/index.vue'),
        meta: { title: '文章管理', icon: 'Document' }
      },
      {
        path: 'article/create',
        name: 'ArticleCreate',
        component: () => import('@/views/backend/article/edit.vue'),
        meta: { title: '写文章', activeMenu: '/back/article' },
        hidden: true
      },
      {
        path: 'article/edit/:id',
        name: 'ArticleEdit',
        component: () => import('@/views/backend/article/edit.vue'),
        meta: { title: '编辑文章', activeMenu: '/back/article' },
        hidden: true
      },
      {
        path: 'category',
        name: 'CategoryManagement',
        component: () => import('@/views/backend/category/index.vue'),
        meta: { title: '分类管理', icon: 'Folder' }
      },
      {
        path: 'tag',
        name: 'TagManagement',
        component: () => import('@/views/backend/tag/index.vue'),
        meta: { title: '标签管理', icon: 'Collection' }
      },
      {
        path: 'comment',
        name: 'CommentManagement',
        component: () => import('@/views/backend/comment/index.vue'),
        meta: { title: '评论管理', icon: 'ChatDotRound' }
      },
      {
        path: 'sentence',
        name: 'SentenceManagement',
        component: () => import('@/views/backend/sentence/index.vue'),
        meta: { title: '句子管理', icon: 'ChatLineSquare' }
      },
      {
        path: 'product-category',
        name: 'ProductCategoryManagement',
        component: () => import('@/views/backend/product/ProductCategoryManageTree.vue'),
        meta: { title: '商品分类', icon: 'Grid' }
      },
      {
        path: 'product',
        name: 'ProductManagement',
        component: () => import('@/views/backend/product/ProductManage.vue'),
        meta: { title: '商品管理', icon: 'ShoppingBag' }
      },
      {
        path: 'product/add',
        name: 'ProductAdd',
        component: () => import('@/views/backend/product/ProductEdit.vue'),
        meta: { title: '新增商品', activeMenu: '/back/product' }
      },
      {
        path: 'product/edit/:id',
        name: 'ProductEdit',
        component: () => import('@/views/backend/product/ProductEdit.vue'),
        meta: { title: '编辑商品', activeMenu: '/back/product' }
      },
      {
        path: 'order',
        name: 'OrderManagement',
        component: () => import('@/views/backend/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'ShoppingCart' }
      },
      {
        path: 'order/:id',
        name: 'BackendOrderDetail',
        component: () => import('@/views/backend/order/OrderDetail.vue'),
        meta: { title: '订单详情', activeMenu: '/back/order' },
        hidden: true
      },
      {
        path: 'profile',
        name: 'BackendProfile',
        component: () => import('@/views/backend/user/profile.vue'),
        meta: { title: '个人信息', icon: 'UserFilled' }
      },
      // 系统设置路由
      {
        path: 'system',
        name: 'SystemSettings',
        component: () => import('@/views/backend/system/index.vue'),
        meta: { title: '系统设置', icon: 'Setting' },
        children: [
          {
            path: 'config',
            name: 'BlogConfig',
            component: () => import('@/views/backend/system/BlogConfig.vue'),
            meta: { title: '博客设置', icon: 'DocumentChecked' }
          },
          {
            path: 'friendLink',
            name: 'FriendLinkManagement',
            component: () => import('@/views/backend/system/FriendLink.vue'),
            meta: { title: '友情链接', icon: 'Link' }
          },
          {
            path: 'about',
            name: 'AboutMe',
            component: () => import('@/views/backend/system/AboutMe.vue'),
            meta: { title: '关于我', icon: 'User' }
          },
          {
            path: 'timeline',
            name: 'TimelineManagement',
            component: () => import('@/views/backend/system/Timeline.vue'),
            meta: { title: '发展历程', icon: 'Timer' }
          }
        ]
      }
    ]
  }
]

// 前台路由配置
const frontendRoutes = [
  {
    path: '/',
    component: () => import('@/layouts/FrontendLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/frontend/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'article/:id',
        name: 'ArticleDetail',
        component: () => import('@/views/frontend/article/detail.vue'),
        meta: { title: '文章详情' }
      },
      {
        path: 'category/:id',
        name: 'CategoryArticles',
        component: () => import('@/views/frontend/category/index.vue'),
        meta: { title: '分类文章' }
      },
      {
        path: 'tag/:id',
        name: 'TagArticles',
        component: () => import('@/views/frontend/tag/index.vue'),
        meta: { title: '标签文章' }
      },
      {
        path: 'articles',
        name: 'ArticleList',
        component: () => import('@/views/frontend/article/list.vue'),
        meta: { title: '文章列表' }
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/frontend/ProductList.vue'),
        meta: { title: '商品列表' }
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/frontend/ProductDetail.vue'),
        meta: { title: '商品详情' }
      },
      {
        path: 'order/confirm',
        name: 'OrderConfirm',
        component: () => import('@/views/frontend/OrderConfirm.vue'),
        meta: { title: '确认订单', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'profile/orders',
        name: 'MyOrders',
        component: () => import('@/views/profile/orders/MyOrders.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'profile/orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/profile/orders/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'profile/purchased',
        name: 'PurchasedProducts',
        component: () => import('@/views/profile/purchased/PurchasedProducts.vue'),
        meta: { title: '我的购买', requiresAuth: true }
      },
      {
        path: 'profile/downloads',
        name: 'DownloadRecords',
        component: () => import('@/views/profile/purchased/DownloadRecords.vue'),
        meta: { title: '下载记录', requiresAuth: true }
      },
      {
        path: 'friend-link',
        name: 'FriendLink',
        component: () => import('@/views/frontend/friendLink/FriendLink.vue'),
        meta: { title: '友情链接' }
      },
      {
        path: 'search',
        name: 'SearchResult',
        component: () => import('@/views/frontend/SearchResult.vue'),
        meta: { title: '搜索结果' }
      },
      {
        path: 'about',
        name: 'About',
        component: () => import('@/views/frontend/About.vue'),
        meta: { title: '关于我' }
      },
      {
        path: 'knowledge',
        name: 'KnowledgeChat',
        component: () => import('@/views/frontend/KnowledgeChat.vue'),
        meta: { title: '知识库问答' }
      }
    ] 
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/forget',
    name: 'ForgetPassword',
    component: () => import('@/views/auth/ForgetPassword.vue'),
    meta: { title: '忘记密码' }
  }
]



// 文章工作台路由（独立顶级路由，与frontend、backend平级）
const workspaceRoutes = [
  {
    path: '/workspace',
    name: 'ArticleWorkspace',
    component: () => import('@/views/workspace/index.vue'),
    meta: { title: '文章工作台', requiresAuth: true }
  }
]

// 错误页面路由
const errorRoutes = [
  {
    path: '/404',
    name: '404',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

// 路由配置
const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...frontendRoutes,
    ...backendRoutes,
    ...workspaceRoutes,
    ...errorRoutes
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 响の博客`
  }

  const userStore = useUserStore()
  console.log("Current route:", to.path)
  console.log("User status:", {
    isLoggedIn: userStore.isLoggedIn,
    isUser: userStore.isUser
  })

  // 检查是否需要登录权限
  if (to.matched.some(record => record.meta?.requiresAuth) && !userStore.isLoggedIn) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  // 已登录用户的路由控制
  if (userStore.isLoggedIn) {
    // 处理登录页面访问
    if (to.path === '/login') {
      next(userStore.isUser ? '/' : '/back/dashboard')
      return
    }

    if (!userStore.isUser) {
      // 非普通用户（后台用户）可以访问后台路由和前台路由
      if (to.path.startsWith('/back') || !to.path.startsWith('/back')) {
        next()
      }
      return
    } else {
      // 普通用户只能访问前台路由
      if (to.path.startsWith('/back') || to.path.startsWith('/workspace')) {
        next('/')
      } else {
        next()
      }
      return
    }
  } else {
    // 未登录用户
    if (to.path.startsWith('/back') || to.path.startsWith('/workspace')) {
      next('/login')
      return
    }
  }

  next()
})

export default router
