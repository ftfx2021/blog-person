<template>
  <div class="order-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">订单管理</span>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="商品名称">
          <el-input
            v-model="searchForm.productName"
            placeholder="请输入商品名称"
            clearable
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select
            v-model="searchForm.orderStatus"
            placeholder="请选择订单状态"
            clearable
            @clear="handleSearch"
          >
            <el-option label="待支付" :value="1" />
            <el-option label="已支付" :value="2" />
            <el-option label="已完成" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <i class="fa fa-search"></i> 搜索
          </el-button>
          <el-button @click="handleReset">
            <i class="fa fa-refresh"></i> 重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 订单列表 -->
      <el-table
        :data="tableData"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column label="商品信息" width="280">
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
        <el-table-column prop="orderAmount" label="订单金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ row.orderAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderStatusName" label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="getStatusType(row.orderStatus)"
              size="small"
            >
              {{ row.orderStatusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payTypeName" label="支付方式" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleViewDetail(row)"
            >
              查看详情
            </el-button>
            <el-button
              v-if="row.orderStatus === 1"
              type="success"
              size="small"
              @click="handleUpdateStatus(row, 2)"
            >
              标记已支付
            </el-button>
            <el-button
              v-if="row.orderStatus === 2"
              type="info"
              size="small"
              @click="handleUpdateStatus(row, 3)"
            >
              标记已完成
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { getOrderPage, updateOrderStatus } from '@/api/OrderApi';

const router = useRouter();

// 搜索表单
const searchForm = reactive({
  productName: '',
  orderStatus: null
});

// 表格数据
const tableData = ref([]);
const loading = ref(false);

// 分页
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 获取订单列表
const fetchOrders = () => {
  loading.value = true;
  getOrderPage(
    {
      current: currentPage.value,
      size: pageSize.value,
      productName: searchForm.productName,
      orderStatus: searchForm.orderStatus
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
  fetchOrders();
};

// 重置
const handleReset = () => {
  searchForm.productName = '';
  searchForm.orderStatus = null;
  currentPage.value = 1;
  fetchOrders();
};

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchOrders();
};

// 当前页改变
const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchOrders();
};

// 查看详情
const handleViewDetail = (row) => {
  router.push(`/backend/order/${row.id}`);
};

// 更新订单状态
const handleUpdateStatus = (row, status) => {
  const statusText = status === 2 ? '已支付' : '已完成';
  ElMessageBox.confirm(
    `确定要将订单标记为"${statusText}"吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    updateOrderStatus(
      row.id,
      status,
      {
        successMsg: '订单状态更新成功',
        onSuccess: () => {
          fetchOrders();
        }
      }
    );
  }).catch(() => {
    // 取消操作
  });
};

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    1: 'warning',
    2: 'success',
    3: 'info'
  };
  return typeMap[status] || '';
};

// 页面加载时获取数据
onMounted(() => {
  fetchOrders();
});
</script>

<style scoped>
.order-list-container {
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

.filter-form {
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

.amount {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
