# 前端文件上传使用指南

## 概述

全新的文件上传系统，采用"先上传到临时目录，保存业务时再移动"的策略。

## 快速开始

### 1. 引入API

```javascript
import { 
  uploadToTemp,           // 上传到临时目录
  uploadMultipleToTemp,   // 批量上传到临时目录
  uploadOneToOne,         // OneToOne模式辅助函数
  uploadOneToMany,        // OneToMany模式辅助函数
  uploadForRichText,      // 富文本编辑器辅助函数
  BUSINESS_TYPES          // 业务类型常量
} from '@/api/fileManagement'
```

## 三种上传模式

### 模式1：OneToOne（用户头像、文章封面）

**场景**：一个业务对象对应一个文件

#### 完整示例：用户头像上传

```vue
<template>
  <el-upload
    :auto-upload="true"
    :show-file-list="false"
    :http-request="handleAvatarUpload"
  >
    <el-image v-if="form.avatar" :src="form.avatar" />
    <el-icon v-else><Plus /></el-icon>
  </el-upload>
</template>

<script setup>
import { ref } from 'vue'
import { uploadOneToOne } from '@/api/fileManagement'
import { saveUser } from '@/api/user'

const form = ref({
  name: '',
  avatar: '' // 存储临时URL
})

// 上传头像
const handleAvatarUpload = async ({ file }) => {
  try {
    // 上传到临时目录
    const tempUrl = await uploadOneToOne(file, {
      successMsg: '头像上传成功'
    })
    
    // 保存临时URL
    form.value.avatar = tempUrl
  } catch (error) {
    console.error('上传失败:', error)
  }
}

// 保存用户
const handleSave = async () => {
  // 直接发送临时URL，后端会自动处理
  await saveUser({
    name: form.value.name,
    avatar: form.value.avatar // 临时URL
  })
}
</script>
```

#### 后端处理（参考）

```java
@Service
public class UserService {
    @Resource
    private FileManagementService fileManagementService;
    
    public void saveUser(UserDTO dto) {
        // 如果是临时URL，移动到正式目录
        if (dto.getAvatar() != null && dto.getAvatar().contains("/temp/")) {
            String formalPath = fileManagementService.moveTempToFormal(
                dto.getAvatar(), 
                "user_avatar", 
                String.valueOf(userId)
            );
            dto.setAvatar(formalPath);
        }
        
        userMapper.insert(user);
    }
}
```

### 模式2：OneToMany（商品图片列表）

**场景**：一个业务对象对应多个文件

#### 完整示例：商品图片上传

```vue
<template>
  <div>
    <!-- 图片列表显示 -->
    <div class="image-list">
      <div v-for="(url, index) in form.images" :key="index" class="image-item">
        <el-image :src="url" fit="cover" />
        <el-button @click="removeImage(index)">删除</el-button>
      </div>
    </div>
    
    <!-- 上传按钮 -->
    <el-upload
      :auto-upload="true"
      :show-file-list="false"
      :multiple="true"
      :http-request="handleImagesUpload"
    >
      <el-button type="primary">上传图片</el-button>
    </el-upload>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { uploadOneToMany } from '@/api/fileManagement'
import { saveProduct } from '@/api/product'

const form = ref({
  name: '',
  images: [] // 存储临时URL数组
})

// 批量上传图片
const handleImagesUpload = async ({ file }) => {
  try {
    // 单个文件也可以用批量接口
    const tempUrls = await uploadOneToMany([file], {
      successMsg: '图片上传成功'
    })
    
    // 添加到列表
    form.value.images.push(...tempUrls)
  } catch (error) {
    console.error('上传失败:', error)
  }
}

// 删除图片
const removeImage = (index) => {
  form.value.images.splice(index, 1)
}

// 保存商品
const handleSave = async () => {
  // 发送JSON数组，后端会自动处理临时文件
  await saveProduct({
    name: form.value.name,
    images: JSON.stringify(form.value.images) // JSON数组
  })
}
</script>
```

#### 后端处理（参考）

```java
@Service
public class ProductService {
    @Resource
    private FileManagementService fileManagementService;
    
    public void saveProduct(ProductDTO dto) {
        List<String> imageUrls = JSON.parseArray(dto.getImages(), String.class);
        
        // 过滤临时URL
        List<String> tempUrls = imageUrls.stream()
            .filter(url -> url.contains("/temp/"))
            .collect(Collectors.toList());
        
        // 批量移动
        if (!tempUrls.isEmpty()) {
            List<String> formalPaths = fileManagementService.moveTempToFormalBatch(
                tempUrls, "product_image", String.valueOf(productId)
            );
            
            // 替换URL
            for (int i = 0; i < tempUrls.size(); i++) {
                imageUrls.set(imageUrls.indexOf(tempUrls.get(i)), formalPaths.get(i));
            }
        }
        
        product.setImages(JSON.toJSONString(imageUrls));
        productMapper.insert(product);
    }
}
```

### 模式3：富文本（文章内容）

**场景**：富文本编辑器中的图片

#### 完整示例：文章编辑器

```vue
<template>
  <div>
    <el-input v-model="form.title" placeholder="标题" />
    
    <!-- Markdown编辑器 -->
    <MarkdownEditor
      v-model="form.content"
      :onUploadImg="handleEditorImageUpload"
    />
    
    <el-button @click="handleSave">保存文章</el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { uploadForRichText } from '@/api/fileManagement'
import { saveArticle } from '@/api/article'
import MarkdownEditor from '@/components/MarkdownEditor.vue'

const form = ref({
  title: '',
  content: '' // Markdown内容，包含临时图片URL
})

// 编辑器图片上传
const handleEditorImageUpload = async (file) => {
  try {
    // 上传到临时目录，返回 { url: tempUrl }
    return await uploadForRichText(file)
  } catch (error) {
    console.error('图片上传失败:', error)
    throw error
  }
}

// 保存文章
const handleSave = async () => {
  // 直接发送内容，后端会自动处理临时图片
  await saveArticle({
    title: form.value.title,
    content: form.value.content // 包含临时URL的内容
  })
}
</script>
```

#### 后端处理（参考）

```java
@Service
public class ArticleService {
    @Resource
    private FileManagementService fileManagementService;
    
    public void saveArticle(ArticleDTO dto) {
        // 自动处理富文本中的临时文件
        String processedContent = fileManagementService.processRichTextFiles(
            dto.getContent(),
            "article_content",
            articleId
        );
        
        article.setContent(processedContent);
        articleMapper.insert(article);
    }
}
```

## 业务类型常量

```javascript
import { BUSINESS_TYPES } from '@/api/fileManagement'

// 使用示例
BUSINESS_TYPES.USER_AVATAR        // 'user_avatar'
BUSINESS_TYPES.ARTICLE_COVER      // 'article_cover'
BUSINESS_TYPES.ARTICLE_CONTENT    // 'article_content'
BUSINESS_TYPES.PRODUCT_COVER      // 'product_cover'
BUSINESS_TYPES.PRODUCT_IMAGE      // 'product_image'
BUSINESS_TYPES.CATEGORY_ICON      // 'category_icon'
BUSINESS_TYPES.FRIEND_LINK_LOGO   // 'friend_link_logo'
```

## 高级用法

### 手动控制文件移动

如果需要在前端手动控制文件移动（不推荐）：

```javascript
import { moveTempToFormal, moveTempToFormalBatch } from '@/api/fileManagement'

// 单个文件
const formalPath = await moveTempToFormal(tempUrl, 'user_avatar', '123')

// 多个文件
const formalPaths = await moveTempToFormalBatch(tempUrls, 'product_image', '456')
```

### 删除业务文件

```javascript
import { deleteBusinessFiles } from '@/api/fileManagement'

// 删除用户的所有头像
await deleteBusinessFiles('user_avatar', '123')

// 删除商品的所有图片
await deleteBusinessFiles('product_image', '456')
```

## 注意事项

1. **临时文件有效期**：24小时，请及时保存业务
2. **URL格式**：临时URL包含 `/temp/`，正式URL包含 `/bussiness/`
3. **后端处理**：后端会自动识别临时URL并移动文件
4. **错误处理**：上传失败时会抛出异常，需要捕获处理

## 迁移指南

### 从旧API迁移

#### 旧代码（使用数据库）
```javascript
import { uploadBusinessFile } from '@/api/file'

// 旧方式：直接上传并保存到数据库
await uploadBusinessFile(file, {
  businessType: 'ARTICLE_COVER',
  businessId: articleId,
  businessField: 'coverImage'
})
```

#### 新代码（无数据库）
```javascript
import { uploadOneToOne } from '@/api/fileManagement'

// 新方式：先上传到临时目录
const tempUrl = await uploadOneToOne(file)

// 保存业务时传递临时URL
await saveArticle({
  coverImage: tempUrl
})

// 后端会自动处理临时文件
```

## 完整工作流程

```
前端                          后端
 │                             │
 ├─ 1. 上传到临时目录           │
 │   uploadToTemp(file)        │
 │                             │
 ├─ 2. 获取临时URL             │
 │   tempUrl = "/files/temp/xxx.jpg"
 │                             │
 ├─ 3. 保存业务（传递临时URL）  │
 │   saveArticle({ cover: tempUrl })
 │                             │
 │                             ├─ 4. 识别临时URL
 │                             │   contains("/temp/")
 │                             │
 │                             ├─ 5. 移动文件
 │                             │   moveTempToFormal()
 │                             │
 │                             ├─ 6. 更新URL
 │                             │   "/files/bussiness/article_cover/123/xxx.jpg"
 │                             │
 │                             ├─ 7. 保存到数据库
 │                             │   articleMapper.insert()
 │                             │
 ├─ 8. 保存成功                 │
 └─                            └─
```

## 常见问题

### Q: 为什么要先上传到临时目录？
A: 避免用户上传后不保存，导致文件孤立。临时文件24小时自动清理。

### Q: 如果用户上传后关闭页面怎么办？
A: 临时文件会在24小时后自动清理，不会占用磁盘空间。

### Q: 可以直接上传到正式目录吗？
A: 不推荐。新系统统一使用临时目录策略，保证文件管理的一致性。

### Q: 编辑模式下也要用临时目录吗？
A: 是的。即使是编辑模式，新上传的文件也先到临时目录，保存时再移动。

## 参考资料

- 后端文档：`springboot/FILE_MANAGEMENT_README.md`
- 完整文档：`docs/file-management-refactor.md`
- 迁移指南：`docs/migration-notes.md`
