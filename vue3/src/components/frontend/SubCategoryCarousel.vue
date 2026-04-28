<template>
  <div class="sub-category-carousel">
    <!-- 左箭头 -->
    <button 
      class="arrow-btn left-arrow" 
      @click="scrollLeft"
      :disabled="isAtStart"
      v-show="showArrows"
    >
      <el-icon><ArrowLeft /></el-icon>
    </button>

    <!-- 子分类容器 -->
    <div class="carousel-container" ref="carouselRef">
      <div class="carousel-track">
        <div
          v-for="category in visibleCategories"
          :key="category.id"
          class="sub-category-item"
          :class="{ active: selectedCategoryId === category.id }"
          @click="handleSelect(category.id)"
        >
          <span class="category-name">{{ category.name }}</span>
        </div>
      </div>
    </div>

    <!-- 右箭头 -->
    <button 
      class="arrow-btn right-arrow" 
      @click="scrollRight"
      :disabled="isAtEnd"
      v-show="showArrows"
    >
      <el-icon><ArrowRight /></el-icon>
    </button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  subCategories: {
    type: Array,
    required: true,
    default: () => []
  },
  selectedCategoryId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['select'])

const carouselRef = ref(null)
const isAtStart = ref(true)
const isAtEnd = ref(false)
const showArrows = ref(false)

// 滑动窗口相关
const windowSize = 5 // 每次显示5个
const currentIndex = ref(0) // 当前窗口起始索引

// 计算当前窗口显示的子分类
const visibleCategories = computed(() => {
  return props.subCategories.slice(currentIndex.value, currentIndex.value + windowSize)
})

// 检查是否需要显示箭头
const checkArrows = () => {
  // 当子分类数量超过5个时显示箭头
  showArrows.value = props.subCategories.length > windowSize
  updateArrowStates()
}

// 更新箭头状态
const updateArrowStates = () => {
  isAtStart.value = currentIndex.value === 0
  isAtEnd.value = currentIndex.value + windowSize >= props.subCategories.length
}

// 向左滚动(显示前一个)
const scrollLeft = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
    updateArrowStates()
  }
}

// 向右滚动(显示后一个)
const scrollRight = () => {
  if (currentIndex.value + windowSize < props.subCategories.length) {
    currentIndex.value++
    updateArrowStates()
  }
}

// 选择分类
const handleSelect = (categoryId) => {
  emit('select', categoryId)
}

// 监听子分类数据变化
watch(() => props.subCategories, () => {
  currentIndex.value = 0 // 重置窗口位置
  checkArrows()
}, { immediate: true })

// 组件挂载时检查箭头
onMounted(() => {
  checkArrows()
})
</script>

<style scoped>
.sub-category-carousel {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 6px;
  background:transparent;
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  border: 1.5px solid transparent;
 border-radius: 10px;
  transition: all 0.3s ease;

}
.sub-category-carousel:hover {
  border: 1.5px solid rgb(38, 51, 185);
  
}

.carousel-container {
  flex: 1;
  overflow: visible;
  /* 隐藏滚动条 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

.carousel-container::-webkit-scrollbar {
  display: none; /* Chrome/Safari/Opera */
}

.carousel-track {
  display: flex;
  gap: 12px;
  padding: 4px;
}

.sub-category-item {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
    font-weight: 700;
  padding: 2px 12px;
   background:rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(5px);
  border: none;
  cursor: pointer;
  border: 1px solid #e0e0e0;
  transition: all 0.3s ease;
  white-space: nowrap;
  font-size: 16px;
  border-radius: 8px;
}

.sub-category-item:hover {
   background: rgb(7, 26, 242);
  color: #fff;
 border-radius: 8px;
  font-weight: 600;

}

.sub-category-item.active {
  background: rgb(7, 26, 242);
  color: #fff;
 border-radius: 8px;
  font-weight: 600;
}





.arrow-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  color: #409EFF;
  font-size: 16px;
}

.arrow-btn:hover:not(:disabled) {
  background: rgba(64, 158, 255, 0.15);
  transform: scale(1.1);
}

.arrow-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.arrow-btn:active:not(:disabled) {
  transform: scale(0.95);
}
</style>
