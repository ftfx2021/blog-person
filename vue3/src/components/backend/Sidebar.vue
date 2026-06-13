<template>
  <div class="sidebar-container" :class="{ 'is-collapsed': isCollapsed }">
    <div class="logo">
      <span class="logo-icon">📝</span>
      <span class="logo-text" v-show="!isCollapsed">个人博客系统</span>
    </div>
    <div class="menu-wrapper">
      <el-menu :default-active="activeMenu" :collapse="isCollapsed" :collapse-transition="false" mode="vertical" class="sidebar-menu"
        text-color="#666" active-text-color="#333" router>
        
        <!-- 固定菜单项 -->
        <el-menu-item index="/back/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        
        <el-menu-item index="/back/article">
          <el-icon><Document /></el-icon>
          <template #title>文章管理</template>
        </el-menu-item>
        
        <el-menu-item index="/back/knowledge-base/stats">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>知识库统计</template>
        </el-menu-item>
        
        <el-menu-item index="/back/category">
          <el-icon><Folder /></el-icon>
          <template #title>分类管理</template>
        </el-menu-item>
        
        <el-menu-item index="/back/tag">
          <el-icon><PriceTag /></el-icon>
          <template #title>标签管理</template>
        </el-menu-item>
        
        <el-menu-item index="/back/comment">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>评论管理</template>
        </el-menu-item>
        
        <el-menu-item index="/back/sentence">
          <el-icon><ChatLineSquare /></el-icon>
          <template #title>句子管理</template>
        </el-menu-item>
        
        <el-menu-item index="/back/user">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        
        <el-menu-item index="/back/profile">
          <el-icon><UserFilled /></el-icon>
          <template #title>个人信息</template>
        </el-menu-item>
        
        <!-- 商品相关菜单已隐藏
        <el-sub-menu index="/back/product-menu">
          <template #title>
            <el-icon><ShoppingBag /></el-icon>
            <span>商品相关</span>
          </template>
          <el-menu-item index="/back/product-category">
            <el-icon><Grid /></el-icon>
            <template #title>商品分类</template>
          </el-menu-item>
          <el-menu-item index="/back/product">
            <el-icon><ShoppingCart /></el-icon>
            <template #title>商品管理</template>
          </el-menu-item>
          <el-menu-item index="/back/order">
            <el-icon><Tickets /></el-icon>
            <template #title>订单管理</template>
          </el-menu-item>
        </el-sub-menu>
        -->
        
        <!-- 系统设置菜单 -->
        <el-sub-menu index="/back/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/back/system/config">
            <el-icon><DocumentChecked /></el-icon>
            <template #title>博客设置</template>
          </el-menu-item>
          <!-- 友链模块已隐藏
          <el-menu-item index="/back/system/friendlink">
            <el-icon><Link /></el-icon>
            <template #title>友情链接</template>
          </el-menu-item>
          -->
          <el-menu-item index="/back/system/about">
            <el-icon><User /></el-icon>
            <template #title>关于我</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'
import { 
  HomeFilled, 
  User, 
  UserFilled,
  Document,
  Folder,
  PriceTag,
  Setting,
  DocumentChecked,
  Link,
  ChatDotRound,
  ChatLineSquare,
  ShoppingBag,
  ShoppingCart,
  Grid,
  Tickets,
  DataAnalysis
} from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()

const isCollapsed = computed(() => appStore.sidebarCollapsed)

// 当前激活的菜单
const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})
</script>

<style lang="scss" scoped>
.sidebar-container {
  height: 100%; 
  min-height: 100vh;
  background-color: #ffffff;
  border-right: 1px solid #eaeaea;
  display: flex;
  flex-direction: column;
  width: 220px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  &.is-collapsed {
    width: 64px;
    
    .logo {
      padding: 0;
      justify-content: center;
      
      .logo-icon {
        margin: 0;
      }
    }

    :deep(.el-menu) {
      .el-sub-menu__title span,
      .el-menu-item span {
        opacity: 0;
        transition: opacity 0.2s;
      }
    }
  }
  
  .logo {
    height: 60px;
    flex-shrink: 0;
    line-height: 60px;
    text-align: center;
    background: #f8f8f8;
    border-bottom: 1px solid #eaeaea;
    display: flex;
    align-items: center;
    padding: 0 16px;
    overflow: hidden;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    
    .logo-icon {
      font-size: 24px;
      margin-right: 8px;
      transition: margin 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    
    .logo-text {
      color: #333;
      font-size: 18px;
      font-weight: 600;
      white-space: nowrap;
      opacity: 1;
      transition: opacity 0.2s;
      font-family: 'SimSun', serif;
    }
  }

  .menu-wrapper {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(0, 0, 0, 0.1);
      border-radius: 2px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }
  }

  :deep(.sidebar-menu) {
    border: none;
    background: transparent;
    transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    .el-menu-item, .el-sub-menu__title {
      height: 50px;
      line-height: 50px;
      color: #666;
      background: transparent;
      transition: all 0.3s;
      font-weight: 400;
      
      span {
        opacity: 1;
        transition: opacity 0.3s;
      }
      
      &:hover {
        background: #f8f8f8 !important;
        color: #333;
      }
    }

    .el-menu-item.is-active {
      background: #f0f0f0 !important;
      color: #333 !important;
      font-weight: 500;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        width: 3px;
        height: 100%;
        background: #333;
      }
    }

    .el-sub-menu {
      &.is-opened {
        > .el-sub-menu__title {
          color: #333;
          background: #f8f8f8 !important;
        }
      }

      .el-menu {
        background: #f8f8f8;
        
        .el-menu-item {
          background: transparent;
          
          &:hover {
            background: #f0f0f0 !important;
          }
          
          &.is-active {
            background: #f0f0f0 !important;
          }
        }
      }
    }

    // 折叠状态下的弹出菜单样式
    &.el-menu--collapse {
      .el-sub-menu {
        &.is-opened {
          > .el-sub-menu__title {
            background: transparent !important;
          }
        }
      }
    }
  }

  .el-icon {
    vertical-align: middle;
    margin-right: 5px;
    width: 24px;
    text-align: center;
    color: inherit;
  }

  span {
    vertical-align: middle;
  }
}
</style> 