<template>
  <div class="product-card" @click="handleCardClick">
    <!-- 图片容器 - 2:1 比例 -->
    <div class="card-image-wrapper">
      <!-- 如果有封面图，显示图片；否则显示默认封面 -->
      <el-image 
        v-if="product.coverImageUrl" 
        :src="product.coverImageUrl" 
        fit="cover" 
        class="card-image"
        lazy
      />
      <DefaultCover 
        v-else
        :color="coverColor"
        :text="product.productName"
        class="card-image"
      />
      
      <!-- 分类标签 -->
      <div class="category-badge" :style="categoryBadgeStyle">{{ product.categoryName }}</div>
     
      <!-- 浏览数 -->
      <div class="view-count-badge">
        <el-icon><View /></el-icon>
        {{ product.viewCount }}
      </div>

      <!-- 商品状态 -->
      <div class="product-status" v-if="product.status === 0">
        <el-tag type="danger" size="small">已下架</el-tag>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="card-content">
      <!-- 标题 -->
      <h3 class="card-title">{{ product.productName }}</h3>

      <!-- 描述 -->
      <p class="card-description">{{ product.productDesc || '暂无简介' }}</p>

      <!-- 底部元数据 -->
      <div class="card-footer">
        <div class="price-info">
          <span class="price">¥{{ product.price }}</span>
        </div>
        <div class="action-button">
          <el-button type="primary" size="small" @click.stop="handleDetailClick">
            查看详情
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, computed } from 'vue'
import { View } from '@element-plus/icons-vue'
import DefaultCover from '@/components/frontend/DefaultCover.vue'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['product-click', 'detail-click'])

// 封面颜色 - 固定为黑色
const coverColor = computed(() => {
  return '#2D3748' // 固定黑色
})

// 分类徽章样式 - 使用黑色主题
const categoryBadgeStyle = computed(() => {
  return {
    background: 'rgba(45, 55, 72, 0.9)', // 黑色半透明
    color: '#fff'
  }
})

// 标题悬停颜色 - 使用黑色
const titleHoverColor = computed(() => {
  return '#2D3748'
})

const handleCardClick = () => {
  emit('product-click', props.product.id)
}

const handleDetailClick = () => {
  emit('detail-click', props.product.id)
}
</script>

<style scoped>
.product-card {
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
  border: 1.5px solid transparent;
  background: #fff;

}

.product-card:hover {
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



/* 分类徽章 */
.category-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(45, 55, 72, 0.9);
  color: #fff;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  backdrop-filter: blur(4px);
  z-index: 10;
  transition: all 0.3s ease;
}

.product-card:hover .category-badge {
  opacity: 1;
  transform: translateY(-2px);
}

/* 浏览数徽章 */
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

.product-card:hover .view-count-badge {
  background: rgba(0, 0, 0, 0.8);
  transform: translateY(-2px);
}

.view-count-badge .el-icon {
  font-size: 12px;
}

/* 商品状态 */
.product-status {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 15;
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

.product-card:hover .card-title {
  color: v-bind('titleHoverColor');
}

/* 描述 */
.card-description {
  margin: 0 0 16px;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
  min-height: 40px;
}

/* 底部元数据 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.price-info {
  display: flex;
  align-items: center;
}

.price {
  font-size: 20px;
  font-weight: bold;
  color: #ff4d4f;
}

.action-button {
  display: flex;
  align-items: center;
}

.action-button .el-button {
  background: v-bind('coverColor');
  border-color: v-bind('coverColor');
  transition: all 0.3s ease;
}

.action-button .el-button:hover {
  background: #1A202C;
  border-color: #1A202C;
  transform: translateY(-1px);
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

  .card-description {
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

  .price {
    font-size: 18px;
  }

  .action-button .el-button {
    font-size: 12px;
    padding: 6px 12px;
  }
}

@media (max-width: 480px) {
  .card-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .action-button {
    justify-content: center;
  }

  .action-button .el-button {
    width: 100%;
  }
}
</style>
