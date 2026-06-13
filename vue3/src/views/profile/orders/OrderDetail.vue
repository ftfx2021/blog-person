<template>
  <div class="order-detail-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button @click="goBack" size="small">
            <i class="fa fa-arrow-left"></i> 返回
          </el-button>
          <span class="title">订单详情</span>
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
              <div class="product-price">¥{{ orderDetail.orderAmount }}</div>
              <div class="product-actions">
                <el-button
                  type="primary"
                  size="small"
                  @click="viewProduct"
                >
                  <i class="fa fa-eye"></i> 查看详情
                </el-button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 下载链接（已支付显示） -->
        <div class="info-section" v-if="orderDetail.orderStatus >= 2">
          <h3 class="section-title">下载链接</h3>
          <el-alert
            title="温馨提示"
            type="success"
            :closable="false"
            show-icon
          >
            <p>您已成功购买此商品，点击下方按钮获取下载链接</p>
          </el-alert>
          
          <!-- 获取下载链接按钮 -->
          <div class="download-action" v-if="!downloadLink">
            <el-button
              type="primary"
              size="large"
              @click="handleGetDownloadLink"
              :loading="fetchingLink"
            >
              <i class="fa fa-link"></i> {{ fetchingLink ? '获取中...' : '获取下载链接' }}
            </el-button>
          </div>
          
          <!-- 下载链接显示 -->
          <div class="download-box" v-else>
            <el-input
              v-model="downloadLink"
              readonly
              class="download-input"
            >
              <template #append>
                <el-button @click="copyLink">
                  <i class="fa fa-copy"></i> 复制
                </el-button>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-section">
          <el-button
            v-if="orderDetail.orderStatus === 1"
            type="primary"
            size="large"
            @click="handlePay"
          >
            <i class="fa fa-credit-card"></i> 立即支付
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 模拟支付对话框 -->
    <el-dialog
      v-model="payDialogVisible"
      title="模拟支付"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="pay-dialog-content">
        <el-alert
          title="模拟支付说明"
          type="info"
          :closable="false"
          show-icon
        >
          <p>这是模拟支付环境，点击确认支付后将自动完成支付</p>
        </el-alert>
        
        <div class="pay-info">
          <div class="pay-item">
            <span class="label">订单号：</span>
            <span class="value">{{ orderDetail?.orderNo }}</span>
          </div>
          <div class="pay-item">
            <span class="label">商品名称：</span>
            <span class="value">{{ orderDetail?.productName }}</span>
          </div>
          <div class="pay-item">
            <span class="label">支付方式：</span>
            <span class="value">{{ orderDetail?.payTypeName }}</span>
          </div>
          <div class="pay-item total">
            <span class="label">支付金额：</span>
            <span class="value amount">¥{{ orderDetail?.orderAmount }}</span>
          </div>
        </div>
        
        <div class="pay-countdown" v-if="countdown > 0">
          <el-progress
            :percentage="(countdown / 3) * 100"
            :show-text="false"
            :stroke-width="6"
            status="success"
          />
          <p>支付处理中... {{ countdown }}秒</p>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="payDialogVisible = false" :disabled="paying">取消</el-button>
        <el-button
          type="primary"
          @click="confirmPay"
          :loading="paying"
        >
          {{ paying ? '支付中...' : '确认支付' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getOrderById, payOrder } from '@/api/OrderApi';
import { getDownloadLink, recordDownload } from '@/api/DownloadApi';

const router = useRouter();
const route = useRoute();

const orderDetail = ref(null);
const loading = ref(false);
const downloadLink = ref('');
const payDialogVisible = ref(false);
const paying = ref(false);
const countdown = ref(0);
const fetchingLink = ref(false);

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
        
        // 注意：不自动获取下载链接，由用户手动点击按钮获取
        // 这样可以避免页面加载时的额外请求
      },
      onError: () => {
        loading.value = false;
        goBack();
      }
    }
  );
};

// 获取下载链接
const handleGetDownloadLink = () => {
  fetchingLink.value = true;
  
  getDownloadLink(orderDetail.value.productId, {
    successMsg: '下载链接获取成功！',
    onSuccess: (link) => {
      downloadLink.value = link;
      fetchingLink.value = false;
    },
    onError: () => {
      fetchingLink.value = false;
    }
  });
};

// 返回
const goBack = () => {
  router.back();
};

// 查看商品
const viewProduct = () => {
  if (orderDetail.value && orderDetail.value.productId) {
    router.push(`/product/${orderDetail.value.productId}`);
  }
};


// 复制链接
const copyLink = () => {
  navigator.clipboard.writeText(downloadLink.value).then(() => {
    ElMessage.success('链接已复制到剪贴板');
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制');
  });
};

// 打开下载链接
const openDownloadLink = () => {
  if (!downloadLink.value) {
    ElMessage.warning('请先获取下载链接');
    return;
  }
  // 记录下载行为
  recordDownload(orderDetail.value.productId, {});
  window.open(downloadLink.value, '_blank');
  ElMessage.success('正在打开下载链接');
};

// 支付
const handlePay = () => {
  payDialogVisible.value = true;
};

// 确认支付
const confirmPay = () => {
  paying.value = true;
  countdown.value = 3;
  
  // 模拟支付倒计时
  const timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
      // 调用支付接口
      executePay();
    }
  }, 1000);
};

// 执行支付
const executePay = () => {
  // 生成模拟第三方流水号
  const thirdPayNo = 'MOCK_' + Date.now() + Math.random().toString(36).substr(2, 9);
  
  payOrder(
    orderDetail.value.id,
    {
      payType: orderDetail.value.payType,
      thirdPayNo: thirdPayNo
    },
    {
      successMsg: '支付成功！',
      onSuccess: () => {
        paying.value = false;
        payDialogVisible.value = false;
        countdown.value = 0;
        
        // 刷新订单详情
        fetchOrderDetail();
      },
      onError: () => {
        paying.value = false;
        countdown.value = 0;
      }
    }
  );
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

.download-action {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.download-action .el-button {
  min-width: 200px;
  height: 50px;
  font-size: 16px;
}

.download-box {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.download-input {
  font-family: monospace;
}

.download-btn {
  align-self: flex-start;
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
  justify-content: space-between;
}

.product-name {
  font-size: 18px;
  font-weight: bold;
  margin: 0 0 10px 0;
}

.product-price {
  color: #f56c6c;
  font-size: 24px;
  font-weight: bold;
  margin: 10px 0;
}

.product-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.action-section {
  display: flex;
  justify-content: center;
  padding: 30px 0;
  border-top: 1px solid #e4e7ed;
}

/* 支付对话框样式 */
.pay-dialog-content {
  padding: 10px 0;
}

.pay-info {
  margin: 25px 0;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.pay-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #e4e7ed;
}

.pay-item:last-child {
  border-bottom: none;
}

.pay-item .label {
  font-size: 14px;
  color: #606266;
}

.pay-item .value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.pay-item.total {
  margin-top: 10px;
  padding-top: 15px;
  border-top: 2px solid #409eff;
}

.pay-item.total .label {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.pay-item.total .amount {
  font-size: 28px;
  font-weight: 700;
  color: #f56c6c;
}

.pay-countdown {
  margin-top: 20px;
  text-align: center;
}

.pay-countdown p {
  margin-top: 10px;
  font-size: 14px;
  color: #67c23a;
  font-weight: 500;
}
</style>
