<template>
  <div class="product-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button @click="goBack" style="margin-right: 16px">
            <i class="fa fa-arrow-left"></i> 返回
          </el-button>
          <span>{{ pageTitle }}</span>
        </div>
      </template>

      <el-form
        :model="productForm"
        :rules="formRules"
        ref="productFormRef"
        label-width="120px"
        style="max-width: 900px"
        v-loading="loading"
      >
        <el-divider content-position="left">基本信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="productName">
              <el-input
                v-model="productForm.productName"
                placeholder="请输入商品名称"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryId">
              <el-select v-model="productForm.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option
                  v-for="category in flatCategoryList"
                  :key="category.id"
                  :label="category.label"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品价格" prop="price">
              <el-input-number
                v-model="productForm.price"
                :min="0"
                :precision="2"
                :step="0.01"
                placeholder="请输入价格"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="productForm.status">
                <el-radio :value="1">上架</el-radio>
                <el-radio :value="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品简介" prop="productDesc">
          <el-input
            v-model="productForm.productDesc"
            type="textarea"
            :rows="3"
            placeholder="请输入商品简介"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="下载链接" prop="downloadLink">
          <el-input
            v-model="productForm.downloadLink"
            placeholder="请输入下载链接"
            maxlength="500"
          />
        </el-form-item>

        <el-divider content-position="left">商品图片</el-divider>

        <!-- 商品图片上传（批量上传，第一张自动设为封面） -->
        <el-form-item label="商品图片">
          <div class="demo-images-container">
            <draggable
              v-model="demoImages"
              item-key="id"
              class="demo-images-list"
              @end="handleDragEnd"
            >
              <template #item="{ element, index }">
                <div class="demo-image-item">
                  <el-image
                    :src="getFileUrl(element.url)"
                    fit="cover"
                    class="demo-image"
                    :preview-src-list="demoImages.map(img => img.url)"
                    :initial-index="index"
                  />
                  <div class="demo-image-actions">
                    <el-button
                      type="danger"
                      size="small"
                      circle
                      @click="removeDemoImage(index)"
                    >
                      <i class="fa fa-trash"></i>
                    </el-button>
                  </div>
                  <div class="demo-image-order">{{ index + 1 }}</div>
                  <!-- 封面标识 -->
                  <div v-if="index === 0" class="cover-badge">
                    <i class="fa fa-star"></i> 封面
                  </div>
                </div>
              </template>
            </draggable>

            <!-- 批量上传按钮 -->
            <el-upload
              v-if="demoImages.length < 9"
              action="#"
              :http-request="handleBatchImageUpload"
              :show-file-list="false"
              :before-upload="beforeUpload"
              accept="image/*"
              multiple
              class="demo-image-upload"
            >
              <div class="upload-placeholder-small">
                <i class="fa fa-plus" style="font-size: 24px; color: #999"></i>
                <div style="margin-top: 8px; color: #999; font-size: 12px">批量上传</div>
              </div>
            </el-upload>
          </div>
          <div style="color: #999; font-size: 12px; margin-top: 8px">
            <i class="fa fa-info-circle" style="color: #409eff"></i>
            支持批量上传，最多9张。第一张图片将自动设为封面，支持拖拽排序调整封面。建议尺寸：800x600，大小不超过5MB
          </div>
        </el-form-item>

        <el-divider content-position="left">详细信息</el-divider>

        <el-form-item label="商品详情" prop="productDetail">
          <el-input
            v-model="productForm.productDetail"
            type="textarea"
            :rows="8"
            placeholder="请输入商品详情"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading" size="large">
            <i class="fa fa-save"></i> 保存
          </el-button>
          <el-button @click="goBack" size="large">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import draggable from 'vuedraggable';
import {
  getProductById,
  createProduct,
  updateProduct
} from '@/api/ProductApi';
import { getAllProductCategories } from '@/api/ProductCategoryApi';
import { uploadToTemp } from '@/api/fileManagement';
import { generateUUID } from '@/utils/uuidUtils';
import { getFileUrl } from '@/utils/fileUtils';

const route = useRoute();
const router = useRouter();

// 数据
const loading = ref(false);
const submitLoading = ref(false);
const categoryList = ref([]);
const productFormRef = ref(null);
const coverImageUrl = ref('');
const demoImages = ref([]);
const isEdit = computed(() => !!route.params.id);
const pageTitle = computed(() => isEdit.value ? '编辑商品' : '新增商品');

// 商品ID（新增时预生成UUID）
const productId = ref(isEdit.value ? route.params.id : generateUUID());

const productForm = reactive({
  productName: '',
  productDesc: '',
  productDetail: '',
  categoryId: null,
  coverImageId: null,
  price: 0,
  downloadLink: '',
  status: 1
});

const formRules = {
  productName: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 1, max: 100, message: '商品名称长度必须在1到100个字符之间', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' }
  ]
};

// 计算属性：扁平化分类列表
const flatCategoryList = computed(() => {
  const result = [];
  const flatten = (categories, prefix = '') => {
    categories.forEach(cat => {
      result.push({
        id: cat.id,
        label: prefix + cat.categoryName
      });
      if (cat.children && cat.children.length > 0) {
        flatten(cat.children, prefix + cat.categoryName + ' / ');
      }
    });
  };
  flatten(categoryList.value);
  return result;
});

// 方法
const fetchCategories = () => {
  getAllProductCategories({
    onSuccess: (res) => {
      categoryList.value = res || [];
    }
  });
};

const fetchProductDetail = () => {
  if (!isEdit.value) return;
  
  loading.value = true;
  getProductById(productId.value, {
    onSuccess: (res) => {
      productForm.productName = res.productName;
      productForm.productDesc = res.productDesc;
      productForm.productDetail = res.productDetail;
      productForm.categoryId = res.categoryId;
      productForm.coverImageId = res.coverImageId;
      productForm.price = res.price;
      productForm.downloadLink = res.downloadLink;
      productForm.status = res.status;
      
      // 设置封面图
      if (res.coverImageUrl) {
        coverImageUrl.value = res.coverImageUrl;
      }
      
      // 设置演示图片
      if (res.images && res.images.length > 0) {
        demoImages.value = res.images.map(img => ({
          id: img.id,
          url: img.filePath,
          sortOrder: img.sortOrder
        }));
      }
      
      loading.value = false;
    },
    onError: () => {
      loading.value = false;
    }
  });
};

// 文件上传前验证
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/');
  const isLt5M = file.size / 1024 / 1024 < 5;

  if (!isImage) {
    ElMessage.error('只能上传图片文件！');
    return false;
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB！');
    return false;
  }
  return true;
};

// 批量图片上传
const handleBatchImageUpload = async ({ file }) => {
  // 检查是否超过最大数量
  if (demoImages.value.length >= 9) {
    ElMessage.warning('最多只能上传9张图片');
    return;
  }

  const currentIndex = demoImages.value.length;
  const isFirstImage = currentIndex === 0;
  
  // 新系统：上传到临时目录
  uploadToTemp(file, {
    showDefaultMsg: false,
    onSuccess: (tempUrl) => {
      // 添加到图片列表
      demoImages.value.push({
        url: tempUrl,
        sortOrder: currentIndex
      });
      
      // 如果是第一张图片，自动设为封面
      if (isFirstImage) {
        productForm.coverImageUrl = tempUrl;
        coverImageUrl.value = tempUrl;
        ElMessage.success('图片上传成功，已自动设为封面');
      } else {
        ElMessage.success('图片上传成功');
      }
    },
    onError: (error) => {
      console.error('图片上传失败:', error);
      ElMessage.error('图片上传失败');
    }
  });
};

// 删除演示图片
const removeDemoImage = (index) => {
  const imageToDelete = demoImages.value[index];
  const isFirstImage = index === 0;
  
  // 如果图片已上传到服务器（有ID），则调用删除API
  if (imageToDelete.id) {
    deleteFile(imageToDelete.id, {
      successMsg: '图片删除成功',
      onSuccess: () => {
        // 从数组中移除
        demoImages.value.splice(index, 1);
        // 重新排序
        demoImages.value.forEach((img, idx) => {
          img.sortOrder = idx;
        });
        
        // 如果删除的是第一张图片（封面），更新封面为新的第一张
        if (isFirstImage && demoImages.value.length > 0) {
          productForm.coverImageId = demoImages.value[0].id;
          coverImageUrl.value = demoImages.value[0].url;
          ElMessage.success('图片删除成功，封面已更新为第一张图片');
        } else if (demoImages.value.length === 0) {
          // 如果删除后没有图片了，清空封面
          productForm.coverImageId = null;
          coverImageUrl.value = '';
        }
      }
    });
  } else {
    // 图片未上传（理论上不会出现这种情况），直接从数组中移除
    demoImages.value.splice(index, 1);
    demoImages.value.forEach((img, idx) => {
      img.sortOrder = idx;
    });
    
    // 更新封面
    if (isFirstImage && demoImages.value.length > 0) {
      productForm.coverImageId = demoImages.value[0].id;
      coverImageUrl.value = demoImages.value[0].url;
    } else if (demoImages.value.length === 0) {
      productForm.coverImageId = null;
      coverImageUrl.value = '';
    }
    
    ElMessage.success('图片已移除');
  }
};

// 拖拽结束事件
const handleDragEnd = () => {
  // 更新排序号
  demoImages.value.forEach((img, index) => {
    img.sortOrder = index;
  });
  
  // 更新封面为第一张图片
  if (demoImages.value.length > 0) {
    productForm.coverImageId = demoImages.value[0].id;
    coverImageUrl.value = demoImages.value[0].url;
    ElMessage.success('排序已更新，封面已更新为第一张图片');
  }
};

// 提交表单
const handleSubmit = () => {
  productFormRef.value.validate((valid) => {
    if (valid) {
      submitLoading.value = true;
      
      // 构建演示图片ID列表（逗号分隔）
      const demoImageIds = demoImages.value.map(img => img.id).join(',');
      
      const submitData = {
        id: productId.value,
        productName: productForm.productName,
        productDesc: productForm.productDesc,
        productDetail: productForm.productDetail,
        categoryId: productForm.categoryId,
        coverImageId: productForm.coverImageId,
        demoImageIds: demoImageIds || null,
        price: productForm.price,
        downloadLink: productForm.downloadLink,
        status: productForm.status
      };

      if (isEdit.value) {
        // 编辑模式
        updateProduct(productId.value, submitData, {
          successMsg: '更新成功',
          onSuccess: () => {
            submitLoading.value = false;
            router.push('/back/product');
          },
          onError: () => {
            submitLoading.value = false;
          }
        });
      } else {
        // 新增模式
        createProduct(submitData, {
          successMsg: '创建成功',
          onSuccess: () => {
            submitLoading.value = false;
            router.push('/back/product');
          },
          onError: () => {
            submitLoading.value = false;
          }
        });
      }
    }
  });
};

// 返回
const goBack = () => {
  router.push('/back/product');
};

// 生命周期
onMounted(() => {
  fetchCategories();
  if (isEdit.value) {
    fetchProductDetail();
  }
});
</script>

<style scoped>
.product-edit {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
}

.upload-placeholder {
  width: 200px;
  height: 150px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-placeholder:hover {
  border-color: #409eff;
}

.upload-placeholder-small {
  width: 120px;
  height: 120px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-placeholder-small:hover {
  border-color: #409eff;
}

.demo-images-container {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.demo-images-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.demo-image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  cursor: move;
  border: 2px solid #dcdfe6;
  transition: all 0.3s;
}

.demo-image-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.3);
}

.demo-image {
  width: 100%;
  height: 100%;
}

.demo-image-actions {
  position: absolute;
  top: 4px;
  right: 4px;
  opacity: 0;
  transition: opacity 0.3s;
}

.demo-image-item:hover .demo-image-actions {
  opacity: 1;
}

.demo-image-order {
  position: absolute;
  bottom: 4px;
  left: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.cover-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
}

.cover-badge i {
  font-size: 10px;
}

.demo-image-upload {
  flex-shrink: 0;
}

:deep(.el-divider__text) {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
</style>
