<template>
  <div class="default-cover" :style="coverStyle">
    <!-- 左边渐变模糊层 -->
    <div class="gradient-blur-left" :style="{ background: leftGradient }"></div>
    
    <!-- 右边渐变模糊层 -->
    <div class="gradient-blur-right" :style="{ background: rightGradient }"></div>
    
    <!-- 文字容器 -->
    <div class="text-container">
      <h2 class="cover-text">{{ text }}</h2>
    </div>
  </div>
</template>

<script setup>
import { defineProps, computed } from 'vue'
import { generateCoverStyles } from '@/utils/colorUtils'

const props = defineProps({
  // 主色彩 (十六进制或rgb格式)
  color: {
    type: String,
    default: '#3498db'
  },
  // 显示的文字
  text: {
    type: String,
    required: true
  }
})

// 使用颜色工具生成所有样式
const colorStyles = computed(() => {
  return generateCoverStyles(props.color)
})

// 提取各个样式属性
const coverStyle = computed(() => colorStyles.value.heroStyle)
const leftGradient = computed(() => colorStyles.value.leftGradient)
const rightGradient = computed(() => colorStyles.value.rightGradient)
</script>

<style scoped>


.default-cover {
  position: relative;
  width: 100%;
  padding-bottom: 50%; /* 2:1 比例 */
  overflow: hidden;
  background-size: 200% 200%;
  animation: gradientShift 15s ease infinite;
}

/* 左边渐变模糊层 */
.gradient-blur-left {
  position: absolute;
  left: 0;
  top: 0;
  width: 50%;
  height: 100%;
  filter: blur(40px);
  opacity: 0.8;
}

/* 右边渐变模糊层 */
.gradient-blur-right {
  position: absolute;
  right: 0;
  top: 0;
  width: 50%;
  height: 100%;
  filter: blur(40px);
  opacity: 0.8;
}

/* 文字容器 */
.text-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  padding: 20px;
  box-sizing: border-box;
}

/* 封面文字 - 根据背景亮度自动切换黑白文字 */
.cover-text {
  margin: 0;
  font-size: 46px;
  font-weight: 1200;
  /* 使用JavaScript计算的动态颜色 */
  color: var(--text-color);
  text-shadow: var(--text-shadow-color);
  text-align: center;
  line-height: 1.3;
  font-family: 'dingding', 'JetBrains Mono', 'Microsoft YaHei', '微软雅黑', 'SimHei', '黑体', sans-serif;
  letter-spacing: 1px;
  max-width: 90%;
  word-break: break-word;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.3s ease;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .cover-text {
    font-size: 32px;
    letter-spacing: 1px;
  }
}

@media (max-width: 480px) {
  .cover-text {
    font-size: 24px;
    letter-spacing: 0.5px;
  }
}
</style>
