<template>
  <header class="header" :class="{ 'header-floating': isFloating, 'header-scrolled': isScrolled, 'header-dynamic-color': dynamicTextColor }" :style="dynamicStyles">
    <div class="header-container">
      <div class="logo">
        <router-link to="/">
          <span class="logo-icon">✒️</span>
          <span class="logo-text">{{ blogName }}</span>
        </router-link>
      </div>
      
      <!-- 原生导航菜单 -->
      <nav class="nav-menu">
        <ul class="nav-list">
          <li class="nav-item">
            <router-link to="/" class="nav-link" :class="{ 'active': activeMenu === '/' }">
              首页
            </router-link>
          </li>
          <li class="nav-item">
            <router-link to="/articles" class="nav-link" :class="{ 'active': activeMenu === '/articles' }">
              文章
            </router-link>
          </li>
          <!-- 商品模块已隐藏
          <li class="nav-item">
            <router-link to="/products" class="nav-link" :class="{ 'active': activeMenu === '/products' || activeMenu.startsWith('/product') }">
              商品
            </router-link>
          </li>
          -->
          <!-- 友链模块已隐藏
          <li class="nav-item">
            <router-link to="/friend-link" class="nav-link" :class="{ 'active': activeMenu === '/friend-link' }">
              友情链接
            </router-link>
          </li>
          -->
          <li class="nav-item">
            <router-link to="/about" class="nav-link" :class="{ 'active': activeMenu === '/about' }">
              关于我
            </router-link>
          </li>
          <li class="nav-item">
            <router-link to="/knowledge" class="nav-link" :class="{ 'active': activeMenu === '/knowledge' }">
              AI问答
            </router-link>
          </li>
        </ul>
      </nav>
      
      <!-- 快捷操作图标 -->
      <div class="quick-action-icons">
        <!-- 文章操作图标 -->
        <div 
          v-if="isArticleDetailPage && isBackendUser"
          class="action-icon"
          @click="handleArticleAction"
          title="文章操作"
        >
          <el-icon :size="20"><Tools /></el-icon>
        </div>
        
        <!-- 搜索图标 -->
        <div 
          class="action-icon"
          @click="handleSearchClick"
          title="搜索文章"
        >
          <el-icon :size="20"><Search /></el-icon>
        </div>
      </div>
      
      <!-- 用户菜单下拉框 -->
      <div class="user-menu">
        <el-dropdown v-if="isLoggedIn" trigger="hover" @command="handleCommand">
          <span class="user-dropdown-trigger">
            <el-icon><User /></el-icon>
            <span class="user-name">{{ userStore.userInfo?.name || '用户' }}</span>
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                <span>个人中心</span>
              </el-dropdown-item>
              <!-- 商品模块已隐藏
              <el-dropdown-item command="orders">
                <el-icon><ShoppingBag /></el-icon>
                <span>我的订单</span>
              </el-dropdown-item>
              <el-dropdown-item command="purchased">
                <el-icon><Download /></el-icon>
                <span>我的购买</span>
              </el-dropdown-item>
              -->
              <el-dropdown-item command="workspace" v-if="isBackendUser" divided>
                <el-icon><Edit /></el-icon>
                <span>文章工作台</span>
              </el-dropdown-item>
              <el-dropdown-item command="backend" v-if="isBackendUser">
                <el-icon><Setting /></el-icon>
                <span>管理后台</span>
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        
        <router-link to="/login" class="user-item login" v-else>
          登录
        </router-link>
      </div>
      <router-link to="/register" class="user-item register" v-if="!isLoggedIn">注册</router-link>
    </div>
  </header>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter, useRoute } from 'vue-router'
import { User, SwitchButton, Setting, ShoppingBag, Download, Search, ArrowDown, Tools, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getContrastTextColor } from '@/utils/colorUtils'

/**
 * @property {string} blogName - 博客名称
 * @property {boolean} isFloating - 是否开启导航栏悬浮效果（不占据页面高度）
 *   - true: 导航栏悬浮在页面上方，不占据页面高度，适用于首页和文章详情页
 *   - false: 导航栏占据页面高度，正常文档流布局（默认）
 * @property {string} backgroundColor - 页面背景主色调，用于自动切换导航栏文字颜色
 */
const props = defineProps({
  blogName: {
    type: String,
    default: '响の博客'
  },
  isFloating: {
    type: Boolean,
    default: false
  },
  backgroundColor: {
    type: String,
    default: null
  }
})

const route = useRoute()
const userStore = useUserStore()
const router = useRouter()

// 是否已滚动（用于悬浮模式）
const isScrolled = ref(false)

const isLoggedIn = computed(() => !!userStore.token)

// 判断是否为后台用户
const isBackendUser = computed(() => userStore.isLoggedIn && !userStore.isUser)

// 判断是否在文章详情页
const isArticleDetailPage = computed(() => {
  return route.path.startsWith('/article/') && route.params.id
})

// 当前激活的菜单项
const activeMenu = computed(() => route.path)

// 动态文字颜色（悬浮模式且未滚动时，根据背景颜色自动切换黑白）
const dynamicTextColor = computed(() => {
  // 悬浮模式、未滚动且有背景色时使用动态颜色
  if (props.isFloating && !isScrolled.value && props.backgroundColor) {
    const color = getContrastTextColor(props.backgroundColor)
    console.log('[Navbar] 动态颜色计算:', {
      isFloating: props.isFloating,
      isScrolled: isScrolled.value,
      backgroundColor: props.backgroundColor,
      resultColor: color
    })
    return color
  }
  return null // 使用默认样式
})

// 动态样式对象
const dynamicStyles = computed(() => {
  if (dynamicTextColor.value) {
    return {
      '--dynamic-text-color': dynamicTextColor.value
    }
  }
  return {}
})

const handleLogout = () => {
  userStore.clearUserInfo()
  router.push('/login')
}

// 处理搜索点击（由布局组件实现）
const handleSearchClick = () => {
  // 触发自定义事件，由父组件（布局）处理
  window.dispatchEvent(new CustomEvent('search-click'))
}

// 下拉菜单命令处理
const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'orders':
      router.push('/profile/orders')
      break
    case 'purchased':
      router.push('/profile/purchased')
      break
    case 'workspace':
      router.push('/workspace')
      break
    case 'backend':
      router.push('/back/dashboard')
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 处理文章操作（由详情页实现）
const handleArticleAction = () => {
  // 触发自定义事件，由父组件（详情页）处理
  window.dispatchEvent(new CustomEvent('article-action-click'))
}

// 滚动监听处理函数
const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
}

// 生命周期钩子
onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  // 初始检查
  handleScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style lang="scss" scoped>
/* 顶部导航栏 */
.header {
  background: transparent;
  backdrop-filter: none;
  box-shadow: none;
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: none;
  width: 100%;
  /* 排除 border 从过渡效果中，避免初始加载时边框闪烁 */
  transition: background 0.3s ease, backdrop-filter 0.3s ease, box-shadow 0.3s ease;
}

/* 悬浮模式 - 首页和文章详情页专用 */
.header.header-floating {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: transparent;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  box-shadow: none;
  border-bottom: none;
  z-index: 1000;
}

/* 滚动后的导航栏样式（适用于所有模式） */
.header.header-scrolled {
  background: #ffffff;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  backdrop-filter: blur(10px);
}

/* 悬浮模式下的滚动样式优化 */
.header.header-floating.header-scrolled {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 动态文字颜色模式（悬浮模式且未滚动时，根据背景颜色自动切换） */
.header.header-dynamic-color {
  /* Logo 文字颜色 */
  :deep(.logo a),
  .logo a {
    color: var(--dynamic-text-color) !important;
    
    &:hover {
      opacity: 0.8;
    }
  }
  
  /* 导航菜单链接颜色 */
  :deep(.nav-link),
  .nav-link {
    color: var(--dynamic-text-color) !important;
    
    &:hover {
      color: var(--dynamic-text-color) !important;
      background: rgba(255, 255, 255, 0.15);
    }
    
    &.active {
      color: var(--dynamic-text-color) !important;
      background: rgba(255, 255, 255, 0.2);
      border: 1px solid rgba(255, 255, 255, 0.3);
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      
      &::before {
        color: var(--dynamic-text-color);
        opacity: 0.8;
      }
    }
  }
  
  /* 快捷操作图标颜色 */
  :deep(.quick-action-icons .action-icon),
  .quick-action-icons .action-icon {
    color: var(--dynamic-text-color) !important;
    
    &:hover {
      color: var(--dynamic-text-color) !important;
      background: rgba(255, 255, 255, 0.1);
    }
  }
  
  /* 用户下拉框触发器颜色 */
  :deep(.user-dropdown-trigger),
  .user-dropdown-trigger {
    color: var(--dynamic-text-color) !important;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    
    &:hover {
      background: rgba(255, 255, 255, 0.15);
      border-color: rgba(255, 255, 255, 0.3);
      color: var(--dynamic-text-color) !important;
    }
  }
  
  /* 登录按钮颜色 */
  :deep(.user-item.login),
  .user-item.login {
    color: var(--dynamic-text-color) !important;
    border: 1px solid rgba(255, 255, 255, 0.3);
    background: rgba(255, 255, 255, 0.1);
    
    &:hover {
      color: #fff !important;
      background: #3B82F6;
      border-color: #3B82F6;
    }
    
    &::before {
      color: var(--dynamic-text-color);
      opacity: 0.8;
    }
  }
  
  /* 注册按钮颜色 */
  :deep(.user-item.register),
  .user-item.register {
    color: var(--dynamic-text-color) !important;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.3);
    
    &:hover {
      color: #fff !important;
      background: #3B82F6;
      border-color: #3B82F6;
    }
    
    &::before {
      color: var(--dynamic-text-color);
      opacity: 0.8;
    }
  }
}

.header-container {
  width: 100%;

  margin: 0 auto;
  padding: 0 40px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
}

.logo a {
  display: flex;
  align-items: center;
  color: #2D3748;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
  
  &:hover {
    color: #F6AD55;
  }
}

.logo-icon {
  font-size: 28px;
  margin-right: 12px;
  filter: drop-shadow(0 2px 4px rgba(246, 173, 85, 0.3));
}

.logo-text {
  font-size: 22px;
 
  font-weight: 600;
  letter-spacing: -0.025em;
}

/* 原生导航菜单样式 */
.nav-menu {
  flex: 1;
  display: flex;
  justify-content: center;
  margin: 0 32px;
}

.nav-list {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 8px;
}

.nav-item {
  padding: 0;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  height: 40px;
  line-height: 1;
  color: #2D3748;
  text-decoration: none;
 
  font-weight: 800;
  font-size: 16px;
  position: relative;
  transition: all 0.3s ease;
  padding: 0 20px;
  border-radius: 8px;
  background: transparent;
  letter-spacing: 0.1em;

  
  &:hover {
    color: #F6AD55;
    background: rgba(246, 173, 85, 0.08);
  }
  
  &.active {
    color: #2D3748;
    background: rgba(255, 255, 255, 0.95);
    border: 1px solid rgba(246, 173, 85, 0.3);
    box-shadow: 0 1px 3px rgba(246, 173, 85, 0.1);
    font-weight: bolder;
    padding: 0 16px 0 22px; /* 左边距更大,让文字往右移 */
    
    &::before {
      content: '>';
      position: absolute;
      left: 6px;
      color: #F6AD55;
      font-size: 12px;
      font-weight: bold;
      animation: blink 1.5s infinite;
    }
  }
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

// 快捷操作图标样式
.quick-action-icons {
  margin-right: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  
  .action-icon {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #2D3748;
    border-radius: 8px;
    transition: all 0.3s ease;
    
    &:hover {
      color: #F6AD55;
      background: rgba(246, 173, 85, 0.1);
    }
  }
}

.user-menu {
  display: flex;
  gap: 12px;
  align-items: center;
}

// 用户下拉框触发器
.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  color: #2D3748;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(45, 55, 72, 0.15);
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(255, 255, 255, 0.8);
    border-color: rgba(246, 173, 85, 0.3);
    color: #F6AD55;
  }
  
  .user-name {
    max-width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .arrow-icon {
    font-size: 12px;
    transition: transform 0.3s ease;
  }
}

// 下拉菜单样式
:deep(.el-dropdown-menu) {
  padding: 8px 0;
   border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(45, 55, 72, 0.1);
  
  .el-dropdown-menu__item {
    padding: 10px 20px;
    font-size: 14px;
    color: #2D3748;
    display: flex;
    align-items: center;
    gap: 10px;
    transition: all 0.2s ease;
    
    &:hover {
      background: rgba(246, 173, 85, 0.1);
      color: #F6AD55;
    }
    
    .el-icon {
      font-size: 16px;
    }
  }
}

.user-item {
  color: #2D3748;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s ease;
   border-radius: 8px;
  font-weight: 500;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.user-item:hover {
  color: #F6AD55;
}

.user-item.login {
  color: #2D3748;
  padding: 6px 14px;
  border: 1px solid rgba(45, 55, 72, 0.3);

  background: transparent;
margin-right:5px;


  letter-spacing: 0.025em;
  position: relative;
  transition: all 0.3s ease;
  
  &:hover {
    color: #fff !important;
    background:  rgb(7, 26, 242);

  }
  

}

.user-item.register {
  background: rgba(59, 130, 246, 0.1);
  color: #2D3748;
  padding: 6px 14px;

  border: 1px solid rgba(59, 130, 246, 0.3);
  letter-spacing: 0.025em;
  position: relative;
  transition: all 0.3s ease;
  
  &:hover {
    color: #fff !important;
    background:  rgb(7, 26, 242);

  }
  

}

@media (max-width: 768px) {
  .header-container {
    flex-direction: column;
    height: auto;
    padding: 20px;
    gap: 16px;
  }
  
  .logo {
    margin-bottom: 0;
  }
  
  .nav-menu {
    width: 100%;
    margin: 0;
  }
  
  .nav-list {
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .nav-link {
    height: 36px;
    padding: 0 14px;
    font-size: 14px;
  }
  
  .user-menu {
    width: 100%;
    justify-content: center;
    gap: 10px;
  }
  
  .user-item.login, .user-item.register {
    padding: 6px 16px;
    font-size: 13px;
  }
}
</style>
