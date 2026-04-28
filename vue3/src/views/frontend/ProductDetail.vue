<template>
  <div class="product-detail-page" v-loading="loading">
    <el-card v-if="product">
      <div class="product-detail">
        <!-- 左侧：商品图片 -->
        <div class="product-images">
          <div class="main-image">
            <img
              :src="currentImage || product.coverImageUrl || '/default-product.png'"
              :alt="product.productName"
            />
          </div>
          <div class="image-thumbnails" v-if="productImages.length > 0">
            <!-- 封面图缩略图 -->
            <div
              class="thumbnail"
              :class="{ active: currentImage === product.coverImageUrl }"
              @click="currentImage = product.coverImageUrl"
            >
              <img :src="product.coverImageUrl" alt="封面图" />
            </div>
            <!-- 其他演示图缩略图 -->
            <div
              v-for="(image, index) in productImages"
              :key="image.id"
              class="thumbnail"
              :class="{ active: currentImage === image.filePath }"
              @click="currentImage = image.filePath"
            >
              <img :src="image.filePath" :alt="`演示图${index + 1}`" />
            </div>
          </div>
        </div>

        <!-- 右侧：商品信息 -->
        <div class="product-info">
          <h1 class="product-title">{{ product.productName }}</h1>
          
          <div class="product-meta">
            <el-tag type="success">{{ product.categoryName }}</el-tag>
            <span class="views">
              <i class="fa fa-eye"></i> {{ product.viewCount }} 次浏览
            </span>
            <span class="sales">
              <i class="fa fa-shopping-cart"></i> {{ product.saleCount }} 已售
            </span>
          </div>

          <div class="product-price">
            <span class="price-label">价格：</span>
            <span class="price-value">¥{{ product.price }}</span>
          </div>

          <div class="product-desc" v-if="product.productDesc">
            <h3>商品简介</h3>
            <p>{{ product.productDesc }}</p>
          </div>

          <div class="product-actions">
            <el-button 
              v-if="!hasPurchased"
              type="primary" 
              size="large" 
              @click="handlePurchase"
            >
              <i class="fa fa-shopping-cart"></i> 立即购买
            </el-button>
            <el-button 
              v-else
              type="success" 
              size="large" 
              @click="handlePurchase"
            >
              <i class="fa fa-download"></i> 查看下载
            </el-button>
            <el-button size="large" @click="goBack">
              <i class="fa fa-arrow-left"></i> 返回列表
            </el-button>
          </div>
        </div>
      </div>

      <!-- 商品详情 -->
      <div class="product-detail-content" v-if="product.productDetail">
        <el-divider />
        <h2>商品详情</h2>
        <div class="detail-html" v-html="product.productDetail"></div>
      </div>
    </el-card>

    <el-empty v-else description="商品不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getProductById, incrementViewCount } from '@/api/ProductApi';
import { checkPurchased } from '@/api/OrderApi';
import { useUserStore } from '@/store/user';
import request from '@/utils/request';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// 数据
const loading = ref(false);
const product = ref(null);
const productImages = ref([]);
const currentImage = ref('');
const hasPurchased = ref(false);

// 方法
const fetchProductDetail = () => {
  const productId = route.params.id;
  loading.value = true;
  
  getProductById(productId, {
    onSuccess: (res) => {
      product.value = res;
      currentImage.value = res.coverImageUrl;
      loading.value = false;
      
      // 增加浏览次数
      incrementViewCount(productId);
      
      // 获取演示图片
      fetchProductImages(productId);
    },
    onError: () => {
      loading.value = false;
    }
  });
};

const fetchProductImages = (productId) => {
  request.get(`/product/${productId}/images`, null, {
    onSuccess: (res) => {
      productImages.value = res || [];
    }
  });
};

const handlePurchase = () => {
  // 检查登录状态
  if (!userStore.token) {
    ElMessageBox.confirm(
      '购买商品需要先登录，是否前往登录？',
      '提示',
      {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      router.push('/login');
    }).catch(() => {
      // 取消操作
    });
    return;
  }
  
  // 检查是否已购买
  if (hasPurchased.value) {
    ElMessageBox.confirm(
      '您已购买过此商品，是否前往查看下载链接？',
      '提示',
      {
        confirmButtonText: '查看下载',
        cancelButtonText: '取消',
        type: 'info'
      }
    ).then(() => {
      router.push('/profile/purchased');
    }).catch(() => {
      // 取消操作
    });
    return;
  }
  
  // 跳转到订单确认页面
  router.push({
    name: 'OrderConfirm',
    query: {
      productId: product.value.id
    }
  });
};

// 检查是否已购买
const checkIfPurchased = () => {
  if (!userStore.token) {
    return;
  }
  
  const productId = route.params.id;
  checkPurchased(productId, {
    onSuccess: (purchased) => {
      hasPurchased.value = purchased;
    }
  });
};

const goBack = () => {
  router.back();
};

// 生命周期
onMounted(() => {
  fetchProductDetail();
  checkIfPurchased();
});
</script>

<style scoped>
.product-detail-page {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
}

.product-detail {
  display: grid;
  grid-template-columns: 500px 1fr;
  gap: 50px;
  margin-bottom: 40px;
  align-items: start;
}

.product-images {
  position: sticky;
  top: 20px;
  align-self: start;
}

.main-image {
  width: 100%;
  height: 500px;
  background: #f5f7fa;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 15px;
  border: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.image-thumbnails {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 5px 0;
}

.image-thumbnails::-webkit-scrollbar {
  height: 6px;
}

.image-thumbnails::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.image-thumbnails::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

.thumbnail {
  width: 90px;
  height: 90px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
  background: #fff;
}

.thumbnail:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.thumbnail.active {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  padding: 10px 0;
  min-height: 500px;
  display: flex;
  flex-direction: column;
}

.product-title {
  font-size: 32px;
  font-weight: 600;
  margin: 0 0 20px 0;
  color: #303133;
  line-height: 1.4;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
}

.product-meta i {
  margin-right: 5px;
}

.product-price {
  background: linear-gradient(135deg, #fff5f5 0%, #ffe8e8 100%);
  padding: 25px;
  border-radius: 12px;
  margin-bottom: 25px;
  border: 1px solid #ffd4d4;
}

.price-label {
  font-size: 16px;
  color: #666;
}

.price-value {
  font-size: 36px;
  font-weight: 700;
  color: #ff4d4f;
  margin-left: 10px;
  font-family: 'Arial', sans-serif;
}

.product-desc {
  margin-bottom: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  flex: 1;
}

.product-desc h3 {
  font-size: 16px;
  margin-bottom: 12px;
  color: #606266;
  font-weight: 600;
}

.product-desc p {
  font-size: 15px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.product-actions {
  display: flex;
  gap: 15px;
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.product-actions .el-button {
  flex: 1;
  height: 50px;
  font-size: 16px;
  font-weight: 500;
}

.product-detail-content {
  margin-top: 40px;
}

.product-detail-content h2 {
  font-size: 24px;
  margin-bottom: 20px;
}

.detail-html {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
}

.detail-html :deep(img) {
  max-width: 100%;
  height: auto;
}

@media (max-width: 992px) {
  .product-detail {
    grid-template-columns: 1fr;
    gap: 30px;
  }
  
  .product-images {
    position: static;
  }
  
  .main-image {
    height: 400px;
  }
  
  .product-title {
    font-size: 24px;
  }
  
  .price-value {
    font-size: 28px;
  }
}

@media (max-width: 576px) {
  .product-detail-page {
    padding: 0 10px;
  }
  
  .main-image {
    height: 300px;
  }
  
  .thumbnail {
    width: 70px;
    height: 70px;
  }
  
  .product-actions {
    flex-direction: column;
  }
  
  .product-actions .el-button {
    width: 100%;
  }
}
</style>
