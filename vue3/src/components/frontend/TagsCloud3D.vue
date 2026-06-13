<template>
  <div 
    class="tags-cloud-container" 
    :style="{ width: boxWidth + 'px', height: boxWidth + 'px' }"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
  >
    <div class="tags-cloud-inner">
      <span
        v-for="(item, index) in tagsData"
        :key="index"
        class="tag-item"
        :style="item.style"
        @click="handleTagClick(item)"
      >
        {{ item.name }}
        <span v-if="item.count !== undefined" class="tag-count">{{ item.count }}</span>
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'

const props = defineProps({
  // 标签数据 [{ name: 'xxx', id: 1, count: 10 }]
  data: {
    type: Array,
    default: () => []
  },
  // 容器宽度
  boxWidth: {
    type: Number,
    default: 300
  },
  // 旋转速度 (数值越大越慢)
  speed: {
    type: Number,
    default: 500
  },
  // 是否随机颜色
  randomColor: {
    type: Boolean,
    default: false
  },
  // 主题色
  primaryColor: {
    type: String,
    default: 'rgb(7, 26, 242)'
  }
})

const emit = defineEmits(['tag-click'])

// 标签数据
const tagsData = ref([])
// 动画帧ID
let animationId = null
// 球体半径
const radius = computed(() => props.boxWidth / 2.5)
// 旋转角度
let angleX = 0
let angleY = 0
// 目标旋转速度（基础自动旋转）
let targetSpeedX = 0.002
let targetSpeedY = 0.003
// 当前旋转速度
let currentSpeedX = 0.002
let currentSpeedY = 0.003
// 基础旋转速度
const baseSpeedX = 0.002
const baseSpeedY = 0.003

// 预设颜色
const colors = [
  'rgb(7, 26, 242)',    // 主蓝色
  '#6366f1',            // 靛蓝
  '#8b5cf6',            // 紫色
  '#ec4899',            // 粉色
  '#14b8a6',            // 青色
  '#f59e0b',            // 橙色
  '#10b981',            // 绿色
]

// 获取随机颜色
const getRandomColor = () => {
  return colors[Math.floor(Math.random() * colors.length)]
}

// 初始化标签位置
const initTags = () => {
  const len = props.data.length
  if (len === 0) return

  tagsData.value = props.data.map((item, index) => {
    // 使用斐波那契球面分布算法
    const phi = Math.acos(-1 + (2 * index + 1) / len)
    const theta = Math.sqrt(len * Math.PI) * phi

    const x = radius.value * Math.cos(theta) * Math.sin(phi)
    const y = radius.value * Math.sin(theta) * Math.sin(phi)
    const z = radius.value * Math.cos(phi)

    return {
      ...item,
      x,
      y,
      z,
      color: props.randomColor ? getRandomColor() : props.primaryColor,
      style: {}
    }
  })

  updateTagsPosition()
}

// 更新标签位置
const updateTagsPosition = () => {
  const centerX = props.boxWidth / 2
  const centerY = props.boxWidth / 2

  tagsData.value.forEach(tag => {
    // 计算屏幕坐标
    const scale = (tag.z + radius.value * 1.5) / (radius.value * 2)
    const x = centerX + tag.x
    const y = centerY + tag.y

    // 根据 z 值计算透明度和大小
    const opacity = Math.max(0.3, Math.min(1, scale))
    const fontSize = Math.max(12, Math.min(18, 12 + scale * 6))

    tag.style = {
      left: `${x}px`,
      top: `${y}px`,
      fontSize: `${fontSize}px`,
      opacity: opacity,
      color: tag.color,
      zIndex: Math.floor(scale * 100),
      transform: `translate(-50%, -50%) scale(${0.6 + scale * 0.4})`,
      filter: `blur(${(1 - scale) * 1}px)`
    }
  })
}

// 旋转动画
const rotate = () => {
  // 平滑过渡速度（0.05 让过渡更平滑）
  currentSpeedX += (targetSpeedX - currentSpeedX) * 0.05
  currentSpeedY += (targetSpeedY - currentSpeedY) * 0.05

  const cosX = Math.cos(currentSpeedX)
  const sinX = Math.sin(currentSpeedX)
  const cosY = Math.cos(currentSpeedY)
  const sinY = Math.sin(currentSpeedY)

  tagsData.value.forEach(tag => {
    // 绕 X 轴旋转
    const y1 = tag.y * cosX - tag.z * sinX
    const z1 = tag.y * sinX + tag.z * cosX

    // 绕 Y 轴旋转
    const x2 = tag.x * cosY + z1 * sinY
    const z2 = -tag.x * sinY + z1 * cosY

    tag.x = x2
    tag.y = y1
    tag.z = z2
  })

  updateTagsPosition()
  animationId = requestAnimationFrame(rotate)
}

// 鼠标移动处理 - 鼠标悬浮时暂停旋转
const handleMouseMove = (e) => {
  // 悬浮时停止旋转，方便用户点击
  targetSpeedX = 0
  targetSpeedY = 0
}

// 鼠标离开处理 - 恢复正常旋转
const handleMouseLeave = () => {
  targetSpeedX = baseSpeedX
  targetSpeedY = baseSpeedY
}

// 标签点击
const handleTagClick = (tag) => {
  emit('tag-click', tag)
}

// 监听数据变化
watch(() => props.data, () => {
  initTags()
}, { deep: true })

onMounted(() => {
  initTags()
  rotate()
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>

<style scoped>
.tags-cloud-container {
  position: relative;
  margin: 0 auto;
  cursor: default;
  perspective: 1000px;
}

.tags-cloud-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
}

.tag-item {
  position: absolute;
  white-space: nowrap;
  cursor: pointer;
  font-weight: 600;
  transition: transform 0.1s ease, opacity 0.1s ease;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 0;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

.tag-item:hover {
  transform: translate(-50%, -50%) scale(1.3) !important;
  opacity: 1 !important;
  filter: blur(0) !important;
  z-index: 1000 !important;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.tag-count {
  font-size: 0.75em;
  opacity: 0.5;
}
</style>
