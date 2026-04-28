<template>
  <div class="auth-container">
    <!-- 二次元风格背景 -->
    <div class="anime-bg">
      <div class="sakura-petals">
        <div v-for="n in 20" :key="n" :class="`petal petal-${n}`"></div>
      </div>
      
      <!-- 移除星星装饰元素 -->
      <!-- <div class="stars">
        <div v-for="n in 30" :key="`star-${n}`" :class="`star star-${n}`"></div>
      </div> -->

      <!-- 移除浮动动漫元素 -->
      <!-- <div class="floating-elements">
        <div class="floating-circle"></div>
        <div class="floating-triangle"></div>
        <div class="floating-square"></div>
      </div> -->
    </div>
    
    <div class="auth-layout">
      <!-- 左侧装饰区域 -->
      <div class="auth-left">
        <div class="brand-content">
          <div class="logo">
            <Notebook />
          </div>
          <h1 class="title">个人博客系统</h1>
          <div class="subtitle">PERSONAL BLOG SYSTEM</div>
          <p class="tagline">记录思想，分享生活</p>
        </div>
        
        <!-- 移除动漫装饰元素 -->
        <!-- <div class="anime-decoration">
          <div class="chibi-character"></div>
          <div class="decorative-bubbles">
            <div class="bubble bubble-1"></div>
            <div class="bubble bubble-2"></div>
            <div class="bubble bubble-3"></div>
          </div>
        </div> -->
      </div>
      
      <!-- 右侧表单区域 -->
      <div class="auth-right">
        <div class="auth-form-container">
          <h2 class="form-title" v-if="showHeader">{{ formTitle }}</h2>
          
          <el-form :model="formData" :rules="rules" ref="formRef" class="auth-form">
            <slot name="form-items"></slot>
            
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSubmit" class="auth-button">
                {{ submitText }}
              </el-button>
            </el-form-item>
            
            <div class="auth-links">
              <slot name="auth-links"></slot>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Notebook } from '@element-plus/icons-vue'

const props = defineProps({
  formData: {
    type: Object,
    required: true
  },
  rules: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  submitText: {
    type: String,
    default: '提交'
  },
  showHeader: {
    type: Boolean,
    default: true
  },
  formTitle: {
    type: String,
    default: '欢迎'
  }
})

const formRef = ref(null)

const emit = defineEmits(['submit'])

const handleSubmit = () => {
  formRef.value.validate(valid => {
    if (valid) {
      emit('submit', formRef)
    }
  })
}

defineExpose({
  formRef
})
</script>

<style lang="scss" scoped>
@use "sass:math";

// 导入项目字体
@font-face {
  font-family: 'PingFangSC-Medium';
  src: url('@/assets/font/PingFangSC-Medium.woff2') format('woff2');
  font-weight: normal;
  font-style: normal;
}

@font-face {
  font-family: 'JetBrainsMono-Medium';
  src: url('@/assets/font/JetBrainsMono-Medium.woff2') format('woff2');
  font-weight: normal;
  font-style: normal;
}

.auth-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  font-family: 'PingFangSC-Medium', 'JetBrainsMono-Medium', sans-serif;
  
  // 背景图片选项 - 可以更换为任意喜欢的二次元背景
  // 选项1: 动漫咖啡厅
  // background-image: url('https://1.bp.blogspot.com/-4KhgdMWOIU0/X-Q1Jeb-NII/AAAAAAAAQ60/_IpLaNNuL1gd15HRZBOsEjlLJ3XrpztBQCLcBGAsYHQ/s1000/Cool%2BCoffee%2BShop%2B%2528Anime%2BBackground%2529.jpg');
  
  // 选项2: SAO风格UI（取消注释启用）
  background-image: url('@/assets/bg.png');
  
  // 选项3: 科技感登录界面（取消注释启用）
  // background-image: url('https://img.freepik.com/premium-photo/modern-realistic-web-login-page-templates-3d-render-blue-background_67155-14333.jpg');
  
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.1); // 半透明遮罩，使背景不会太抢眼
    z-index: 0;
  }
}

/* 二次元风格背景 */
.anime-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

/* 樱花飘落效果 */
.sakura-petals {
  width: 100%;
  height: 100%;
  position: absolute;
}

.petal {
  position: absolute;
  background-size: contain;
  background-repeat: no-repeat;
  width: 15px;
  height: 15px;
  background-color: rgba(255, 183, 197, 0.7);
  border-radius: 150% 0 150% 0;
  animation: petal-fall linear infinite;
  transform-origin: center;
  opacity: 0.6;
  box-shadow: 0 0 5px rgba(255, 183, 197, 0.3);
}

@for $i from 1 through 20 {
  .petal-#{$i} {
    left: math.random(100) * 1%;
    top: -20px;
    width: 10px + math.random(10) * 1px;
    height: 10px + math.random(10) * 1px;
    animation-duration: 6s + math.random(8) * 1s;
    animation-delay: math.random(5) * 0.5s;
    transform: rotate(math.random(360) + deg);
  }
}

@keyframes petal-fall {
  0% {
    transform: translateY(-20px) rotate(0deg) translateX(0);
    opacity: 0;
  }
  10% {
    opacity: 0.8;
  }
  80% {
    opacity: 0.6;
  }
  100% {
    transform: translateY(calc(100vh + 20px)) rotate(360deg) translateX(100px);
    opacity: 0;
  }
}

/* 星星效果 - 已移除 */
// .stars {
//   width: 100%;
//   height: 100%;
//   position: absolute;
// }

.star {
  display: none; /* 隐藏星星 */
}

/* 浮动元素 - 已移除 */
.floating-elements {
  display: none; /* 隐藏浮动元素 */
}

.floating-circle,
.floating-triangle,
.floating-square {
  display: none; /* 隐藏具体浮动形状 */
}

.auth-layout {
  display: flex;
  width: 900px;
  height: 600px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
  position: relative;
  z-index: 1;
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.auth-left {
  flex: 1;
  background-color: rgba(255, 255, 255, 0.6);
  color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  border-right: 1px solid rgba(234, 234, 234, 0.5);
}

.brand-content {
  text-align: center;
  z-index: 2;
  padding: 0 40px;
  position: relative;
}

.logo {
  margin-bottom: 20px;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd4e5, #d4e7ff);
  margin: 0 auto 20px;
  box-shadow: 0 4px 15px rgba(183, 127, 209, 0.3);
  
  svg {
    width: 36px;
    height: 36px;
    color: #9370DB;
  }
}

.title {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px;
  letter-spacing: 1px;
  color: #6c5ce7;
  font-family: 'PingFangSC-Medium', 'JetBrainsMono-Medium', sans-serif;
  text-shadow: 1px 1px 2px rgba(108, 92, 231, 0.2);
}

.subtitle {
  font-size: 14px;
  font-weight: 300;
  letter-spacing: 4px;
  margin-bottom: 24px;
  color: #a29bfe;
  font-family: 'PingFangSC-Medium', 'JetBrainsMono-Medium', sans-serif;
}

.tagline {
  font-size: 16px;
  font-weight: 300;
  line-height: 1.6;
  color: #6c5ce7;
  font-family: 'PingFangSC-Medium', 'JetBrainsMono-Medium', sans-serif;
}

/* 动漫装饰元素 - 已移除 */
.anime-decoration {
  display: none; /* 隐藏动漫装饰 */
}

.chibi-character {
  display: none; /* 隐藏角色 */
}

.decorative-bubbles {
  display: none; /* 隐藏气泡 */
}

.bubble {
  display: none; /* 隐藏单个气泡 */
}

.auth-right {
  flex: 1;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-form-container {
  width: 80%;
  max-width: 360px;
}

.form-title {
  font-size: 24px;
  font-weight: 500;
  color: #6c5ce7;
  margin-bottom: 30px;
  text-align: center;
  font-family: 'PingFangSC-Medium', 'JetBrainsMono-Medium', sans-serif;
}

.auth-form {
  :deep(.el-input__wrapper) {
    border-radius: 12px;
    box-shadow: 0 0 0 1px rgba(108, 92, 231, 0.2);
    padding: 0 15px;
    height: 48px;
    transition: all 0.3s ease;
    
    &:hover, &.is-focus {
      box-shadow: 0 0 0 1px #6c5ce7;
      transform: translateY(-2px);
    }
  }
  
  :deep(.el-input__inner) {
    height: 48px;
    font-size: 15px;
    color: #6c5ce7;
  }
  
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
  
  :deep(.el-form-item__error) {
    padding-top: 4px;
    font-size: 12px;
    color: #ff7979;
  }
}

.auth-button {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  border: none;
  transition: all 0.3s ease;
  
  &:hover, &:focus {
    transform: translateY(-3px);
    box-shadow: 0 5px 15px rgba(108, 92, 231, 0.3);
    opacity: 0.9;
    background: linear-gradient(135deg, #5549c0, #8a7efe);
  }
}

.auth-links {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  font-size: 14px;
  
  a {
    color: #6c5ce7;
    text-decoration: none;
    transition: all 0.3s ease;
    
    &:hover {
      color: #5549c0;
      text-decoration: underline;
    }
  }
}
</style> 