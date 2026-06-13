<template>
  <div class="purchased-products-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">我的购买</span>
          <span class="subtitle">已购买的商品可以随时查看下载链接</span>
        </div>
      </template>

      <!-- 搜索框 -->
      <el-form :inline="true" class="search-form">
        <el-form-item>
          <el-input
            v-model="searchForm.productName"
            placeholder="搜索商品名称"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <i class="fa fa-search"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <i class="fa fa-search"></i> 搜索
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 商品列表 -->
      <div v-loading="loading" class="products-grid">
        <div
          v-for="item in productList"
          :key="item.productId"
          class="product-card"
        >
          <div class="product-cover">
            <el-image
              v-if="item.coverImageUrl"
              :src="item.coverImageUrl"
              fit="cover"
              class="cover-image"
            />
            <div v-else class="no-image">
              <i class="fa fa-image"></i>
            </div>
          </div>
          
          <div class="product-info">
            <h3 class="product-name">{{ item.productName }}</h3>
            <p class="product-desc">{{ item.productDesc || '暂无简介' }}</p>
            
            <div class="product-meta">
              <div class="meta-item">
                <i class="fa fa-calendar"></i>
                <span>购买时间：{{ formatDate(item.purchaseTime) }}</span>
              </div>
              <div class="meta-item">
                <i class="fa fa-download"></i>
                <span>已下载：{{ item.downloadCount }}次</span>
              </div>
            </div>
            
            <div class="product-actions">
              <el-button
                type="primary"
                size="small"
                @click="handleViewOrder(item)"
              >
                <i class="fa fa-file-text"></i> 查看订单
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-if="!loading && productList.length === 0"
        description="暂无购买记录"
      >
        <el-button type="primary" @click="goToProducts">
          <i class="fa fa-shopping-cart"></i> 去购买商品
        </el-button>
      </el-empty>

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination"
      />
    </el-card>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getPurchasedProducts } from '@/api/DownloadApi';

const router = useRouter();

// 搜索表单
const searchForm = reactive({
  productName: ''
});

// 数据
const productList = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);


// 获取已购商品列表
const fetchProducts = () => {
  loading.value = true;
  getPurchasedProducts(
    {
      current: currentPage.value,
      size: pageSize.value,
      productName: searchForm.productName
    },
    {
      onSuccess: (res) => {
        productList.value = res.records || [];
        total.value = res.total || 0;
        loading.value = false;
      },
      onError: () => {
        loading.value = false;
      }
    }
  );
};

// 搜索
const handleSearch = () => {
  currentPage.value = 1;
  fetchProducts();
};

// 分页
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchProducts();
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchProducts();
};


// 查看订单
const handleViewOrder = (item) => {
  router.push(`/profile/orders/${item.orderId}`);
};

// 去购买商品
const goToProducts = () => {
  router.push('/products');
};

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 页面加载时获取数据
onMounted(() => {
  fetchProducts();
});
</script>

<style scoped>
.purchased-products-container {
  padding: 20px;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.subtitle {
  font-size: 14px;
  color: #909399;
}

.search-form {
  margin-bottom: 20px;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.product-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  background: #fff;
}

.product-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.product-cover {
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: #f5f7fa;
}

.cover-image {
  width: 100%;
  height: 100%;
}

.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #dcdfe6;
}

.product-info {
  padding: 15px;
}

.product-name {
  font-size: 16px;
  font-weight: bold;
  margin: 0 0 10px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: 14px;
  color: #606266;
  margin: 0 0 15px 0;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #909399;
}

.meta-item i {
  width: 16px;
}

.product-actions {
  display: flex;
  justify-content: center;
}

.product-actions .el-button {
  min-width: 120px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

</style>
