<template>
  <div class="timeline-section" v-loading="loading">
    <div class="section-header">
      <h2 class="section-title">发展历程</h2>
    </div>
    
    <el-empty v-if="timelines.length === 0" description="暂无发展历程" />
    
    <div class="timeline-container" v-else>
      <!-- 左箭头 -->
      <div class="scroll-arrow arrow-left" @click="scrollLeft" :class="{ 'arrow-disabled': !canScrollLeft }">
        <i class="fas fa-chevron-left"></i>
      </div>
      
      <!-- 可滚动区域 -->
      <div class="timeline-scroll" ref="scrollContainer" @scroll="updateScrollState">
        <div class="timeline-wrapper">
          <!-- 水平时间线轴 -->
          <div class="timeline-axis"></div>
          
          <!-- 时间线事件列表 -->
          <div class="timeline-items">
            <div 
              v-for="(item, index) in timelines" 
              :key="item.id" 
              class="timeline-item"
            >
              <!-- 节点 -->
              <div class="timeline-node" :style="{ backgroundColor: item.color || '#3498db' }"></div>
              
              <!-- 标题标签 -->
              <el-tooltip 
                :content="item.content" 
                :disabled="!item.content"
                placement="top"
                :show-after="200"
                popper-class="timeline-popper"
              >
                <template #content>
                  <div class="tooltip-inner">
                    <div class="tooltip-date">{{ formatDate(item.eventDate) }}</div>
                    <div class="tooltip-text">{{ item.content }}</div>
                  </div>
                </template>
                <div 
                  class="timeline-label" 
                  :class="{ 'label-top': index % 2 === 0, 'label-bottom': index % 2 === 1 }"
                >
                  <i :class="['fas', item.icon || 'fa-star']"></i>
                  <span>{{ item.title }}</span>
                </div>
              </el-tooltip>
              
              <!-- 年份 -->
              <div 
                class="timeline-year" 
                :class="{ 'year-bottom': index % 2 === 0, 'year-top': index % 2 === 1 }"
              >
                {{ getYear(item.eventDate) }}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右箭头 -->
      <div class="scroll-arrow arrow-right" @click="scrollRight" :class="{ 'arrow-disabled': !canScrollRight }">
        <i class="fas fa-chevron-right"></i>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getVisibleTimelines } from '@/api/SiteTimelineApi'

const loading = ref(false)
const timelines = ref([])
const scrollContainer = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)

// 获取发展历程
const fetchTimelines = () => {
  loading.value = true
  getVisibleTimelines({
    showDefaultMsg: false,
    onSuccess: (data) => {
      timelines.value = data || []
      loading.value = false
      nextTick(() => {
        updateScrollState()
      })
    },
    onError: (error) => {
      console.error('获取发展历程失败:', error)
      loading.value = false
    }
  })
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}年${month}月${day}日`
}

// 获取年份
const getYear = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.getFullYear()
}

// 更新滚动状态
const updateScrollState = () => {
  if (!scrollContainer.value) return
  const container = scrollContainer.value
  canScrollLeft.value = container.scrollLeft > 0
  canScrollRight.value = container.scrollLeft < container.scrollWidth - container.clientWidth - 1
}

// 向左滚动
const scrollLeft = () => {
  if (!scrollContainer.value || !canScrollLeft.value) return
  scrollContainer.value.scrollBy({
    left: -200,
    behavior: 'smooth'
  })
}

// 向右滚动
const scrollRight = () => {
  if (!scrollContainer.value || !canScrollRight.value) return
  scrollContainer.value.scrollBy({
    left: 200,
    behavior: 'smooth'
  })
}

// 生命周期钩子
onMounted(() => {
  fetchTimelines()
})
</script>

<style scoped>
/* 区域容器 - 参考About页面detail-section */
.timeline-section {
  flex: 1;
  background-color: #fff;
  padding: 30px;
  border-radius: 8px;
  border: 1px solid #eaeaea;
  min-width: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  margin: 0;
  font-size: 24px;
  color: #333;
  font-weight: 600;
}

/* 时间线外层容器 */
.timeline-container {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 滚动箭头 */
.scroll-arrow {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
  color: #666;
}

.scroll-arrow:hover {
  background: rgb(7, 26, 242);
  border-color: rgb(7, 26, 242);
  color: #fff;
}

.scroll-arrow.arrow-disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.scroll-arrow.arrow-disabled:hover {
  background: #fff;
  border-color: #e0e0e0;
  color: #666;
}

/* 可滚动区域 */
.timeline-scroll {
  flex: 1;
  overflow-x: auto;
  overflow-y: visible;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.timeline-scroll::-webkit-scrollbar {
  display: none;
}

/* 时间线容器 */
.timeline-wrapper {
  position: relative;
  padding: 45px 20px;
  min-width: max-content;
}

/* 水平时间线轴 */
.timeline-axis {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 6px;
  background: linear-gradient(to right, #3498db, #2ecc71);
  border-radius: 3px;
  transform: translateY(-50%);
}

/* 时间线事件容器 */
.timeline-items {
  display: flex;
  gap: 60px;
  position: relative;
}

/* 单个事件项 */
.timeline-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 100px;
}

.timeline-item:hover {
  z-index: 100;
}

/* 节点 - 放在最上层 */
.timeline-node {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 20;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.timeline-item:hover .timeline-node {
  transform: translate(-50%, -50%) scale(1.4);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.3);
}

/* 标题标签 - 参考关键词样式 */
.timeline-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  position: absolute;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10;
}

.timeline-label i {
  font-size: 14px;
  color: #3498db;
}

.label-top {
  bottom: calc(50% + 18px);
}

.label-bottom {
  top: calc(50% + 18px);
}

.timeline-label:hover {
  background: rgb(7, 26, 242);
  color: #fff;
  border-color: rgb(7, 26, 242);
}

.timeline-label:hover i {
  color: #fff;
}

/* 年份 */
.timeline-year {
  font-size: 12px;
  color: #999;
  position: absolute;
}

.year-top {
  top: calc(50% + 18px);
}

.year-bottom {
  bottom: calc(50% + 18px);
}

/* 响应式设计 */
@media (max-width: 992px) {
  .timeline-wrapper {
    padding: 45px 15px;
  }
  
  .timeline-items {
    gap: 40px;
  }
  
  .timeline-label {
    font-size: 12px;
    padding: 3px 8px;
  }
  
  .timeline-label i {
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .timeline-section {
    padding: 20px;
  }
  
  .scroll-arrow {
    width: 28px;
    height: 28px;
  }
  
  .timeline-wrapper {
    padding: 40px 10px;
  }
  
  .timeline-items {
    gap: 30px;
  }
  
  .timeline-item {
    min-width: 80px;
  }
  
  .timeline-label {
    font-size: 11px;
    padding: 3px 6px;
  }
  
  .timeline-year {
    font-size: 10px;
  }
}
</style>

<style>
/* 全局tooltip样式 */
.timeline-popper {
  border: 1.5px solid #f3f3f3 !important;
  max-width: 280px !important;
  background: rgba(255, 255, 255, 0.6) !important;
    backdrop-filter: blur(5px);
    border-radius: 8px;
}
.el-popper__arrow{
  display: none !important;
}
.timeline-popper .tooltip-inner {
  text-align: left;

}

.timeline-popper .tooltip-date {
  font-size: 12px;
  color: #999;
 
  margin-bottom: 6px;
}

.timeline-popper .tooltip-text {
  font-size: 13px;
  color: #333;
  line-height: 1.6;
   
}
</style>
