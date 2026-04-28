<template>
  <div class="product-edit-example">
    <el-form :model="form" label-width="120px">
      <!-- 商品名称 -->
      <el-form-item label="商品名称">
        <el-input v-model="form.productName" placeholder="请输入商品名称" />
      </el-form-item>
      
      <!-- 封面图上传（OneToOne模式） -->
      <el-form-item label="封面图">
        <el-upload
          class="cover-uploader"
          :auto-upload="true"
          :show-file-list="false"
          :http-request="handleCoverUpload"
        >
          <el-image
            v-if="form.coverImageUrl"
            :src="form.coverImageUrl"
            fit="cover"
            style="width: 200px; height: 200px;"
          />
          <div v-else class="upload-placeholder">
            <el-icon><Plus /></el-icon>
            <div>点击上传封面</div>
          </div>
        </el-upload>
      </el-form-item>
      
      <!-- 商品图片列表（OneToMany模式） -->
      <el-form-item label="商品图片">
        <div class="image-list">
          <!-- 已上传的图片 -->
          <div
            v-for="(url, index) in form.imageUrls"
            :key="index"
            class="image-item"
          >
            <el-image :src="url" fit="cover" />
            <el-button
              type="danger"
              size="small"
              @click="removeImage(index)"
            >
              删除
            </el-button>
          </div>
          
          <!-- 上传按钮 -->
          <el-upload
            class="image-uploader"
            :auto-upload="true"
            :show-file-list="false"
            :multiple="true"
            :http-request="handleImagesUpload"
          >
            <div class="upload-placeholder">
              <el-icon><Plus /></el-icon>
              <div>添加图片</div>
            </div>
          </el-upload>
        </div>
      </el-form-item>
      
      <!-- 商品详情（富文本模式） -->
      <el-form-item label="商品详情">
        <MarkdownEditor
          v-model="form.productDetail"
          :onUploadImg="handleDetailImageUpload"
        />
      </el-form-item>
      
      <!-- 保存按钮 -->
      <el-form-item>
        <el-button type="primary" @click="handleSave">保存商品</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  uploadOneToOne,
  uploadOneToMany,
  uploadForRichText
} from '@/api/fileManagement'
import { saveProduct } from '@/api/product'
import MarkdownEditor from '@/components/MarkdownEditor.vue'

// 表单数据
const form = ref({
  productName: '',
  coverImageUrl: '', // 封面图URL（临时或正式）
  imageUrls: [], // 商品图片URL列表（临时或正式）
  productDetail: '' // 商品详情（包含临时图片URL）
})

/**
 * 上传封面图（OneToOne模式）
 */
const handleCoverUpload = async ({ file }) => {
  try {
    // 上传到临时目录
    const tempUrl = await uploadOneToOne(file, {
      successMsg: '封面上传成功'
    })
    
    // 保存临时URL
    form.value.coverImageUrl = tempUrl
    
    console.log('封面临时URL:', tempUrl)
  } catch (error) {
    console.error('封面上传失败:', error)
    ElMessage.error('封面上传失败')
  }
}

/**
 * 批量上传商品图片（OneToMany模式）
 */
const handleImagesUpload = async ({ file }) => {
  try {
    // 上传到临时目录（单个文件也用批量接口）
    const tempUrls = await uploadOneToMany([file], {
      successMsg: '图片上传成功'
    })
    
    // 添加到列表
    form.value.imageUrls.push(...tempUrls)
    
    console.log('图片临时URL:', tempUrls)
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('图片上传失败')
  }
}

/**
 * 删除商品图片
 */
const removeImage = (index) => {
  form.value.imageUrls.splice(index, 1)
}

/**
 * 富文本编辑器图片上传（富文本模式）
 */
const handleDetailImageUpload = async (file) => {
  try {
    // 上传到临时目录，返回 { url: tempUrl }
    const result = await uploadForRichText(file)
    
    console.log('详情图片临时URL:', result.url)
    
    return result
  } catch (error) {
    console.error('详情图片上传失败:', error)
    throw error
  }
}

/**
 * 保存商品
 * 
 * 注意：
 * 1. 前端只需要传递临时URL
 * 2. 后端会自动识别临时URL并移动文件
 * 3. 后端会将临时URL替换为正式URL后保存到数据库
 */
const handleSave = async () => {
  try {
    // 构建保存数据
    const saveData = {
      productName: form.value.productName,
      coverImageUrl: form.value.coverImageUrl, // 临时URL
      demoImageUrls: JSON.stringify(form.value.imageUrls), // JSON数组
      productDetail: form.value.productDetail // 包含临时URL的HTML
    }
    
    console.log('保存数据:', saveData)
    
    // 调用保存接口
    await saveProduct(saveData)
    
    ElMessage.success('商品保存成功')
    
    // 后端处理流程：
    // 1. 识别 coverImageUrl 中的 /temp/ 路径
    // 2. 调用 fileManagementService.moveTempToFormal()
    // 3. 将临时URL替换为正式URL
    // 4. 识别 demoImageUrls 中的临时URL
    // 5. 调用 fileManagementService.moveTempToFormalBatch()
    // 6. 替换所有临时URL为正式URL
    // 7. 识别 productDetail 中的临时图片
    // 8. 调用 fileManagementService.processRichTextFiles()
    // 9. 替换富文本中的临时URL
    // 10. 保存到数据库
    
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}
</script>

<style scoped>
.product-edit-example {
  padding: 20px;
}

.cover-uploader,
.image-uploader {
  display: inline-block;
}

.upload-placeholder {
  width: 200px;
  height: 200px;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
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

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-item {
  position: relative;
  width: 200px;
  height: 200px;
}

.image-item .el-image {
  width: 100%;
  height: 100%;
  border-radius: 6px;
}

.image-item .el-button {
  position: absolute;
  top: 5px;
  right: 5px;
}
</style>
