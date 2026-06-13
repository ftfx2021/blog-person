<template>
  <div class="download-records-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">下载记录</span>
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

      <!-- 记录列表 -->
      <el-table
        :data="tableData"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column label="商品信息" width="350">
          <template #default="{ row }">
            <div class="product-info">
              <el-image
                v-if="row.productCoverUrl"
                :src="row.productCoverUrl"
                fit="cover"
                class="product-cover"
              />
              <div class="product-name">{{ row.productName }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="downloadTime" label="下载时间" width="180" />
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleDownloadAgain(row)"
            >
              再次下载
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
import { getDownloadRecords, getDownloadLink } from '@/api/DownloadApi';

const router = useRouter();

// 搜索表单
const searchForm = reactive({
  productName: ''
});

// 表格数据
const tableData = ref([]);
const loading = ref(false);

// 分页
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 获取下载记录
const fetchRecords = () => {
  loading.value = true;
  getDownloadRecords(
    {
      current: currentPage.value,
      size: pageSize.value,
      productName: searchForm.productName
    },
    {
      onSuccess: (res) => {
        tableData.value = res.records || [];
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
  fetchRecords();
};

// 分页
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchRecords();
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchRecords();
};

// 再次下载
const handleDownloadAgain = (row) => {
  getDownloadLink(
    row.productId,
    {
      onSuccess: (link) => {
        window.open(link, '_blank');
        ElMessage.success('正在打开下载链接');
      }
    }
  );
};

// 页面加载时获取数据
onMounted(() => {
  fetchRecords();
});
</script>

<style scoped>
.download-records-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.search-form {
  margin-bottom: 20px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-cover {
  width: 60px;
  height: 60px;
  border-radius: 4px;
}

.product-name {
  flex: 1;
  font-size: 14px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
