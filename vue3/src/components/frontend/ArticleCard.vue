<template>
  <div class="article-card" @click="handleCardClick">
    <!-- 图片容器 - 2:1 比例 -->
    <div class="card-image-wrapper">
    
      <!-- 如果有封面图，显示图片；否则显示默认封面 -->
      <el-image 
        v-if="article.coverImage" 
        :src="article.coverImage" 
        fit="cover" 
        class="card-image"
        lazy
      />
      <DefaultCover 
        v-else
        :color="coverColor"
        :text="article.title"
        class="card-image"
      />
      
      <!-- 分类标签 -->
      <div class="category-badge" :style="categoryBadgeStyle">{{ article.categoryName }}</div>
     
      <!-- 阅读数 -->
      <div class="view-count-badge">
        <el-icon><View /></el-icon>
        {{ article.viewCount }}
      </div>
      
      <!-- 加密标志 -->
      <div v-if="article.isPasswordProtected === 1" class="lock-badge">
        <el-icon><Lock /></el-icon>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="card-content">
      <!-- 标题 -->
      <h3 class="card-title">{{ article.title }}</h3>

      <!-- 摘要 -->
      <p class="card-summary">{{ article.summary }}</p>

      <!-- 标签 -->
      <div class="card-tags">
        <DynamicTag 
          v-for="tag in article.tags" 
          :key="tag.id" 
          :label="tag.name"
          :bgColor="tag.color"
          :textColor="tag.textColor"
          size="small"
          @click.stop="handleTagClick(tag.id)"
        />
      </div>

      <!-- 底部元数据 -->
      <div class="card-footer">
        <div class="author-info">
          <el-icon><User /></el-icon>
          <span>{{ article.authorName }}</span>
        </div>
        <div class="publish-date">
          <el-icon><Calendar /></el-icon>
          <span>{{ formatDate(article.createTime) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, computed } from 'vue'
import { User, Calendar, View, Lock } from '@element-plus/icons-vue'
import DynamicTag from '@/components/common/DynamicTag.vue'
import DefaultCover from '@/components/frontend/DefaultCover.vue'
import DateUtils from '@/utils/dateUtils'

const props = defineProps({
  article: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['article-click', 'tag-click', 'category-click'])

// 封面颜色 - 优先使用文章设置的主色调，否则使用默认蓝色
const coverColor = computed(() => {
  return props.article.mainColor || '#409EFF'
})

// 分类徽章样式
const categoryBadgeStyle = computed(() => {
  const mainColor = props.article.mainColor || '#409EFF'
  return {
    background: mainColor.replace(')', ', 0.9)').replace('rgb', 'rgba'),
  }
})

// 标题悬停颜色
const titleHoverColor = computed(() => {
  return props.article.mainColor || '#409EFF'
})

const handleCardClick = () => {
  emit('article-click', props.article.id)
}

const handleTagClick = (tagId) => {
  emit('tag-click', tagId)
}

const formatDate = (dateString) => {
  return DateUtils.formatDate(dateString)
}
</script>

<style scoped>
.article-card {
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
    border: 1.5px solid transparent;
}

.article-card:hover {
   border: 1.5px solid v-bind('coverColor');
}

/* 图片容器 - 2:1 比例 */
.card-image-wrapper {
  position: relative;
  width: 100%;
  padding-bottom: 50%; /* 2:1 比例 */
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  
}

.card-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}


.card-image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  font-size: 48px;
  color: rgba(0, 0, 0, 0.1);
}

/* 分类徽章 */
.category-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(64, 158, 255, 0.9);
  color: #fff;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  backdrop-filter: blur(4px);
  z-index: 10;
  transition: all 0.3s ease;
}

.article-card:hover .category-badge {
  opacity: 1;
  transform: translateY(-2px);
}

/* 阅读数徽章 */
.view-count-badge {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  backdrop-filter: blur(4px);
  z-index: 10;
  transition: all 0.3s ease;
}

.article-card:hover .view-count-badge {
  background: rgba(0, 0, 0, 0.8);
  transform: translateY(-2px);
}

.view-count-badge .el-icon {
  font-size: 12px;
}

/* 加密标志 */
.lock-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(230, 162, 60, 0.9);
  color: #fff;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
  z-index: 10;
  transition: all 0.3s ease;
}

.lock-badge .el-icon {
  font-size: 16px;
}

.article-card:hover .lock-badge {
  transform: translateY(-2px);
  background: rgba(230, 162, 60, 1);
}

/* 内容区域 */
.card-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 20px;
  background: #fff;
}

/* 标题 */
.card-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.3s ease;
}

.article-card:hover .card-title {
  color: v-bind('titleHoverColor');
}

/* 摘要 */
.card-summary {
  margin: 0 0 12px;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

/* 标签 */
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  min-height: 24px;
}

/* 底部元数据 */
.card-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #888;
}

.author-info,
.publish-date {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: color 0.3s ease;
}

.author-info:hover,
.publish-date:hover {
  color: v-bind('titleHoverColor');
}

.card-footer .el-icon {
  font-size: 13px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .card-content {
    padding: 16px;
  }

  .card-title {
    font-size: 16px;
    -webkit-line-clamp: 2;
  }

  .card-summary {
    font-size: 13px;
    -webkit-line-clamp: 2;
  }

  .category-badge {
    font-size: 11px;
    padding: 4px 10px;
  }

  .view-count-badge {
    font-size: 11px;
    padding: 4px 8px;
  }

  .card-footer {
    font-size: 11px;
    gap: 12px;
  }
}
</style>
