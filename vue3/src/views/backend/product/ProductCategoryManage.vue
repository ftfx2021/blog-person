<template>
  <div class="product-category-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>商品分类管理</span>
          <el-button type="primary" @click="handleAdd">
            <i class="fa fa-plus"></i> 新增分类
          </el-button>
        </div>
      </template>

      <!-- 分类列表 -->
      <el-table :data="categoryList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="categoryName" label="分类名称" min-width="150" />
        <el-table-column prop="sortOrder" label="排序号" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productCount" label="商品数量" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form
        :model="categoryForm"
        :rules="formRules"
        ref="categoryFormRef"
        label-width="100px"
      >
        <el-form-item label="分类名称" prop="categoryName">
          <el-input
            v-model="categoryForm.categoryName"
            placeholder="请输入分类名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number
            v-model="categoryForm.sortOrder"
            :min="0"
            :max="999"
            placeholder="数字越小越靠前"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="categoryForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getAllProductCategories,
  createProductCategory,
  updateProductCategory,
  deleteProductCategory
} from '@/api/ProductCategoryApi';

// 数据
const loading = ref(false);
const categoryList = ref([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增分类');
const isEdit = ref(false);
const categoryFormRef = ref(null);

const categoryForm = reactive({
  id: null,
  categoryName: '',
  sortOrder: 0,
  status: 1
});

const formRules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 50, message: '分类名称长度必须在1到50个字符之间', trigger: 'blur' }
  ],
  sortOrder: [
    { required: true, message: '请输入排序号', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
};

// 方法
const fetchCategories = () => {
  loading.value = true;
  getAllProductCategories({
    onSuccess: (res) => {
      categoryList.value = res || [];
      loading.value = false;
    },
    onError: () => {
      loading.value = false;
    }
  });
};

const handleAdd = () => {
  dialogTitle.value = '新增分类';
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  dialogTitle.value = '编辑分类';
  isEdit.value = true;
  categoryForm.id = row.id;
  categoryForm.categoryName = row.categoryName;
  categoryForm.sortOrder = row.sortOrder;
  categoryForm.status = row.status;
  dialogVisible.value = true;
};

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除分类"${row.categoryName}"吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    deleteProductCategory(row.id, {
      successMsg: '删除成功',
      onSuccess: () => {
        fetchCategories();
      }
    });
  }).catch(() => {
    // 用户取消删除
  });
};

const handleSubmit = () => {
  categoryFormRef.value.validate((valid) => {
    if (valid) {
      const submitData = {
        categoryName: categoryForm.categoryName,
        sortOrder: categoryForm.sortOrder,
        status: categoryForm.status
      };

      if (isEdit.value) {
        updateProductCategory(categoryForm.id, submitData, {
          successMsg: '更新成功',
          onSuccess: () => {
            dialogVisible.value = false;
            fetchCategories();
          }
        });
      } else {
        createProductCategory(submitData, {
          successMsg: '创建成功',
          onSuccess: () => {
            dialogVisible.value = false;
            fetchCategories();
          }
        });
      }
    }
  });
};

const handleDialogClose = () => {
  resetForm();
};

const resetForm = () => {
  categoryForm.id = null;
  categoryForm.categoryName = '';
  categoryForm.sortOrder = 0;
  categoryForm.status = 1;
  if (categoryFormRef.value) {
    categoryFormRef.value.clearValidate();
  }
};

// 生命周期
onMounted(() => {
  fetchCategories();
});
</script>

<style scoped>
.product-category-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
