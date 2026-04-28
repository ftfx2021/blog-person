<template>
  <div class="product-list-page">
    <!-- 分类筛选 -->
    <div class="category-filter">
      <el-button
        :type="selectedCategory === null ? 'primary' : ''"
        @click="selectCategory(null)"
      >
        全部商品
      </el-button>
      <el-button
        v-for="category in categoryList"
        :key="category.id"
        :type="selectedCategory === category.id ? 'primary' : ''"
        @click="selectCategory(category.id)"
      >
        {{ category.categoryName }}
      </el-button>
    </div>

    <!-- 搜索框 -->
    <div class="search-box">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索商品名称"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">
            <i class="fa fa-search"></i> 搜索
          </el-button>
        </template>
      </el-input>
    </div>

    <!-- 商品列表 -->
    <div v-loading="loading" class="product-grid">
      <el-empty v-if="productList.length === 0" description="暂无商品" />
      <ProductCard
        v-for="product in productList"
        :key="product.id"
        :product="product"
        @product-click="goToDetail"
        @detail-click="goToDetail"
      />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchProducts"
        @current-change="fetchProducts"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getProductPage } from '@/api/ProductApi';
import { getEnabledProductCategories } from '@/api/ProductCategoryApi';
import ProductCard from '@/components/frontend/ProductCard.vue';

const router = useRouter();

// 数据
const loading = ref(false);
const productList = ref([]);
const categoryList = ref([]);
const currentPage = ref(1);
const pageSize = ref(12);
const total = ref(0);
const selectedCategory = ref(null);
const searchKeyword = ref('');

// 方法
const fetchProducts = () => {
  loading.value = true;
  getProductPage(
    currentPage.value,
    pageSize.value,
    searchKeyword.value,
    selectedCategory.value,
    1, // 只显示上架商品
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

const fetchCategories = () => {
  getEnabledProductCategories({
    onSuccess: (res) => {
      categoryList.value = res || [];
    }
  });
};

const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId;
  currentPage.value = 1;
  fetchProducts();
};

const handleSearch = () => {
  currentPage.value = 1;
  fetchProducts();
};

const goToDetail = (productId) => {
  router.push(`/product/${productId}`);
};

// 生命周期
onMounted(() => {
  fetchCategories();
  fetchProducts();
});
</script>

<style scoped>
.product-list-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.category-filter {
  margin-bottom: 20px;
  text-align: center;
}

.category-filter .el-button {
  margin: 5px;
}

.search-box {
  margin-bottom: 30px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 480px) {
  .product-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
