<template>
  <div class="order-confirm-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span class="title">确认订单</span>
        </div>
      </template>

      <div v-if="product" class="order-content">
        <!-- 商品信息 -->
        <div class="section">
          <h3 class="section-title">商品信息</h3>
          <div class="product-card">
            <el-image
              v-if="product.coverImageUrl"
              :src="product.coverImageUrl"
              fit="cover"
              class="product-cover"
            />
            <div class="product-info">
              <h4 class="product-name">{{ product.productName }}</h4>
              <p class="product-desc">{{ product.productDesc || '暂无简介' }}</p>
              <div class="product-price">¥{{ product.price }}</div>
            </div>
          </div>
        </div>

        <!-- 支付方式 -->
        <div class="section">
          <h3 class="section-title">支付方式</h3>
          <el-radio-group v-model="payType" class="pay-type-group">
            <el-radio :label="1" border>
              <i class="fa fa-alipay" style="color: #1677ff;"></i>
              支付宝支付
            </el-radio>
            <el-radio :label="2" border>
              <i class="fa fa-wechat" style="color: #07c160;"></i>
              微信支付
            </el-radio>
          </el-radio-group>
        </div>

        <!-- 订单金额 -->
        <div class="section">
          <h3 class="section-title">订单金额</h3>
          <div class="amount-detail">
            <div class="amount-item">
              <span>商品价格：</span>
              <span class="amount">¥{{ product.price }}</span>
            </div>
            <el-divider />
            <div class="amount-item total">
              <span>应付金额：</span>
              <span class="amount">¥{{ product.price }}</span>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-section">
          <el-button @click="goBack" size="large">
            <i class="fa fa-arrow-left"></i> 返回
          </el-button>
          <el-button 
            type="primary" 
            size="large" 
            @click="handleSubmitOrder"
            :loading="submitting"
          >
            <i class="fa fa-check"></i> 提交订单
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getProductById } from '@/api/ProductApi';
import { createOrder } from '@/api/OrderApi';

const route = useRoute();
const router = useRouter();

// 数据
const loading = ref(false);
const submitting = ref(false);
const product = ref(null);
const payType = ref(1); // 默认支付宝

// 获取商品信息
const fetchProduct = () => {
  const productId = route.query.productId;
  if (!productId) {
    ElMessage.error('商品ID不存在');
    router.back();
    return;
  }

  loading.value = true;
  getProductById(productId, {
    onSuccess: (res) => {
      product.value = res;
      loading.value = false;
    },
    onError: () => {
      loading.value = false;
      router.back();
    }
  });
};

// 提交订单
const handleSubmitOrder = () => {
  submitting.value = true;
  
  createOrder(
    {
      productId: product.value.id,
      payType: payType.value
    },
    {
      successMsg: '订单创建成功',
      onSuccess: (order) => {
        submitting.value = false;
        
        // 跳转到订单详情页
        ElMessage.success({
          message: '订单创建成功！请尽快完成支付',
          duration: 2000,
          onClose: () => {
            router.push(`/profile/orders/${order.id}`);
          }
        });
      },
      onError: () => {
        submitting.value = false;
      }
    }
  );
};

// 返回
const goBack = () => {
  router.back();
};

// 页面加载
onMounted(() => {
  fetchProduct();
});
</script>

<style scoped>
.order-confirm-page {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 20px;
  font-weight: bold;
}

.order-content {
  padding: 20px 0;
}

.section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e4e7ed;
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
  width: 120px;
  height: 120px;
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

.product-desc {
  font-size: 14px;
  color: #606266;
  margin: 0;
  flex: 1;
}

.product-price {
  color: #f56c6c;
  font-size: 24px;
  font-weight: bold;
  margin-top: 10px;
}

.pay-type-group {
  display: flex;
  gap: 15px;
}

.pay-type-group :deep(.el-radio) {
  margin-right: 0;
  padding: 15px 30px;
}

.pay-type-group :deep(.el-radio__label) {
  font-size: 16px;
}

.pay-type-group i {
  font-size: 20px;
  margin-right: 8px;
}

.amount-detail {
  background-color: #fafafa;
  padding: 20px;
  border-radius: 8px;
}

.amount-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  padding: 10px 0;
}

.amount-item.total {
  font-size: 18px;
  font-weight: bold;
}

.amount-item .amount {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
}

.amount-item.total .amount {
  font-size: 28px;
}

.action-section {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 30px 0 10px;
  border-top: 1px solid #e4e7ed;
}
</style>
