<template>
  <div class="category-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>商品分类管理（树形）</span>
          <el-button type="primary" @click="handleAdd(null)">
            <i class="fa fa-plus"></i> 新增一级分类
          </el-button>
        </div>
      </template>

      <!-- 树形表格 -->
      <el-table
        :data="categoryTree"
        style="width: 100%"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        v-loading="loading"
      >
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column prop="level" label="层级" width="100">
          <template #default="{ row }">
            <el-tag :type="row.level === 1 ? 'primary' : 'success'">
              {{ row.level === 1 ? '一级' : '二级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.level === 1"
              type="primary"
              size="small"
              @click="handleAdd(row.id)"
            >
              添加子分类
            </el-button>
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
        <el-form-item label="父分类" v-if="categoryForm.parentId > 0">
          <el-input v-model="parentCategoryName" disabled />
        </el-form-item>
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
            placeholder="数字越小越靠前"
            style="width: 100%"
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
const categoryTree = ref([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增分类');
const isEdit = ref(false);
const categoryFormRef = ref(null);
const parentCategoryName = ref('');

const categoryForm = reactive({
  id: null,
  parentId: 0,
  categoryName: '',
  sortOrder: 0,
  status: 1
});

const formRules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 50, message: '分类名称长度必须在1到50个字符之间', trigger: 'blur' }
  ]
};

// 方法
const fetchCategories = () => {
  loading.value = true;
  getAllProductCategories({
    onSuccess: (res) => {
      categoryTree.value = res || [];
      loading.value = false;
    },
    onError: () => {
      loading.value = false;
    }
  });
};

const handleAdd = (parentId) => {
  dialogTitle.value = parentId ? '新增二级分类' : '新增一级分类';
  isEdit.value = false;
  resetForm();
  
  if (parentId) {
    categoryForm.parentId = parentId;
    // 查找父分类名称
    const findParent = (categories) => {
      for (const cat of categories) {
        if (cat.id === parentId) {
          return cat.categoryName;
        }
        if (cat.children) {
          const found = findParent(cat.children);
          if (found) return found;
        }
      }
      return null;
    };
    parentCategoryName.value = findParent(categoryTree.value);
  } else {
    categoryForm.parentId = 0;
    parentCategoryName.value = '';
  }
  
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  dialogTitle.value = '编辑分类';
  isEdit.value = true;
  categoryForm.id = row.id;
  categoryForm.parentId = row.parentId || 0;
  categoryForm.categoryName = row.categoryName;
  categoryForm.sortOrder = row.sortOrder;
  categoryForm.status = row.status;
  
  if (row.parentId && row.parentId > 0) {
    // 查找父分类名称
    const findParent = (categories) => {
      for (const cat of categories) {
        if (cat.id === row.parentId) {
          return cat.categoryName;
        }
      }
      return null;
    };
    parentCategoryName.value = findParent(categoryTree.value);
  }
  
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
        parentId: categoryForm.parentId,
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
  categoryForm.parentId = 0;
  categoryForm.categoryName = '';
  categoryForm.sortOrder = 0;
  categoryForm.status = 1;
  parentCategoryName.value = '';
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
.category-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
