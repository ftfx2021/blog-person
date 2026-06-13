<template>
  <div class="order-detail-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button @click="goBack" size="small">
            <i class="fa fa-arrow-left"></i> 返回
          </el-button>
          <span class="title">订单详情（管理员）</span>
        </div>
      </template>

      <div v-if="orderDetail" class="order-content">
        <!-- 订单状态 -->
        <el-alert
          :title="getStatusTitle(orderDetail.orderStatus)"
          :type="getStatusAlertType(orderDetail.orderStatus)"
          :closable="false"
          show-icon
          class="status-alert"
        />

        <!-- 订单信息 -->
        <div class="info-section">
          <h3 class="section-title">订单信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">
              {{ orderDetail.orderNo }}
            </el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="getStatusType(orderDetail.orderStatus)">
                {{ orderDetail.orderStatusName }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="订单金额">
              <span class="amount">¥{{ orderDetail.orderAmount }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">
              {{ orderDetail.payTypeName || '未选择' }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ orderDetail.createTime }}
            </el-descriptions-item>
            <el-descriptions-item label="支付时间">
              {{ orderDetail.payTime || '未支付' }}
            </el-descriptions-item>
            <el-descriptions-item label="第三方流水号" :span="2">
              {{ orderDetail.thirdPayNo || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 用户信息 -->
        <div class="info-section">
          <h3 class="section-title">用户信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户ID">
              {{ orderDetail.userId }}
            </el-descriptions-item>
            <el-descriptions-item label="用户名">
              {{ orderDetail.username }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 商品信息 -->
        <div class="info-section">
          <h3 class="section-title">商品信息</h3>
          <div class="product-card">
            <el-image
              v-if="orderDetail.productCoverUrl"
              :src="orderDetail.productCoverUrl"
              fit="cover"
              class="product-cover"
            />
            <div class="product-info">
              <h4 class="product-name">{{ orderDetail.productName }}</h4>
              <div class="product-id">商品ID: {{ orderDetail.productId }}</div>
              <div class="product-price">¥{{ orderDetail.orderAmount }}</div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-section">
          <el-button
            v-if="orderDetail.orderStatus === 1"
            type="success"
            size="large"
            @click="handleUpdateStatus(2)"
          >
            <i class="fa fa-check"></i> 标记为已支付
          </el-button>
          <el-button
            v-if="orderDetail.orderStatus === 2"
            type="info"
            size="large"
            @click="handleUpdateStatus(3)"
          >
            <i class="fa fa-check-circle"></i> 标记为已完成
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getOrderById, updateOrderStatus } from '@/api/OrderApi';

const router = useRouter();
const route = useRoute();

const orderDetail = ref(null);
const loading = ref(false);

// 获取订单详情
const fetchOrderDetail = () => {
  const orderId = route.params.id;
  if (!orderId) {
    ElMessage.error('订单ID不存在');
    goBack();
    return;
  }

  loading.value = true;
  getOrderById(
    orderId,
    {
      onSuccess: (res) => {
        orderDetail.value = res;
        loading.value = false;
      },
      onError: () => {
        loading.value = false;
        goBack();
      }
    }
  );
};

// 返回
const goBack = () => {
  router.back();
};

// 更新订单状态
const handleUpdateStatus = (status) => {
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
      orderDetail.value.id,
      status,
      {
        successMsg: '订单状态更新成功',
        onSuccess: () => {
          fetchOrderDetail();
        }
      }
    );
  }).catch(() => {
    // 取消操作
  });
};

// 获取状态标题
const getStatusTitle = (status) => {
  const titleMap = {
    1: '等待支付',
    2: '支付成功',
    3: '订单已完成'
  };
  return titleMap[status] || '未知状态';
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

// 获取状态提示类型
const getStatusAlertType = (status) => {
  const typeMap = {
    1: 'warning',
    2: 'success',
    3: 'info'
  };
  return typeMap[status] || 'info';
};

// 页面加载时获取数据
onMounted(() => {
  fetchOrderDetail();
});
</script>

<style scoped>
.order-detail-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 15px;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.order-content {
  padding: 20px 0;
}

.status-alert {
  margin-bottom: 30px;
}

.info-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e4e7ed;
}

.amount {
  color: #f56c6c;
  font-weight: bold;
  font-size: 18px;
}

.product-card {
  display: flex;
  gap: 20px;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background-color: #fafafa;
}

.product-cover {
  width: 150px;
  height: 150px;
  border-radius: 8px;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-name {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
}

.product-id {
  color: #909399;
  font-size: 14px;
}

.product-price {
  color: #f56c6c;
  font-size: 24px;
  font-weight: bold;
  margin-top: auto;
}

.action-section {
  display: flex;
  justify-content: center;
  gap: 15px;
  padding: 30px 0;
  border-top: 1px solid #e4e7ed;
}
</style>
