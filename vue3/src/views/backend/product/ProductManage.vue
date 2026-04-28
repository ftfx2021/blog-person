<template>
  <div class="product-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>商品管理</span>
          <el-button type="primary" @click="handleAdd">
            <i class="fa fa-plus"></i> 新增商品
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.productName" placeholder="请输入商品名称" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="请选择分类" clearable>
            <el-option
              v-for="category in categoryList"
              :key="category.id"
              :label="category.categoryName"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchProducts">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 商品列表 -->
      <el-table :data="productList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="商品ID" width="280" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品名称" min-width="150" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column prop="saleCount" label="销量" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button 
              :type="row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchProducts"
        @current-change="fetchProducts"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getProductPage,
  deleteProduct,
  updateProductStatus
} from '@/api/ProductApi';
import { getAllProductCategories } from '@/api/ProductCategoryApi';

const router = useRouter();

// 数据
const loading = ref(false);
const productList = ref([]);
const categoryList = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const searchForm = reactive({
  productName: '',
  categoryId: null,
  status: null
});


// 方法
const fetchProducts = () => {
  loading.value = true;
  getProductPage(
    currentPage.value,
    pageSize.value,
    searchForm.productName,
    searchForm.categoryId,
    searchForm.status,
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
  getAllProductCategories({
    onSuccess: (res) => {
      categoryList.value = res || [];
    }
  });
};

const resetSearch = () => {
  searchForm.productName = '';
  searchForm.categoryId = null;
  searchForm.status = null;
  currentPage.value = 1;
  fetchProducts();
};

const handleAdd = () => {
  router.push('/back/product/add');
};

const handleEdit = (row) => {
  router.push(`/back/product/edit/${row.id}`);
};

const handleToggleStatus = (row) => {
  const newStatus = row.status === 1 ? 0 : 1;
  const action = newStatus === 1 ? '上架' : '下架';
  
  ElMessageBox.confirm(
    `确定要${action}商品"${row.productName}"吗？`,
    '状态确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    updateProductStatus(row.id, newStatus, {
      successMsg: `${action}成功`,
      onSuccess: () => {
        fetchProducts();
      }
    });
  }).catch(() => {
    // 用户取消
  });
};

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除商品"${row.productName}"吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    deleteProduct(row.id, {
      successMsg: '删除成功',
      onSuccess: () => {
        fetchProducts();
      }
    });
  }).catch(() => {
    // 用户取消删除
  });
};

// 生命周期
onMounted(() => {
  fetchCategories();
  fetchProducts();
});
</script>

<style scoped>
.product-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}
</style>
