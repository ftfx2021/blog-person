# 🚀 文件管理模块 - 快速开始

## 当前状态
✅ **代码完成** - 编译通过，等待数据库迁移和测试

---

## 立即执行（3步）

### 1️⃣ 数据库迁移（必须）

```bash
# 备份数据库
mysqldump -u root -p personal_blog > backup_$(date +%Y%m%d).sql

# 执行迁移
mysql -u root -p personal_blog < docs/database-migration.sql

# 验证
mysql -u root -p personal_blog -e "DESC product;"
```

**预期结果：** 看到 `cover_image_url` 和 `demo_image_urls` 字段

---

### 2️⃣ 启动服务

```bash
cd springboot
mvn spring-boot:run
```

**预期结果：** 服务正常启动，无错误日志

---

### 3️⃣ 测试上传

```bash
# 测试文件上传
curl -X POST http://localhost:8080/api/file/upload/temp \
  -F "file=@test.jpg"
```

**预期响应：**
```json
{
  "code": 200,
  "data": "/files/temp/1234567890.jpg"
}
```

---

## 核心变更

### 旧系统 ❌
```java
// 通过文件ID关联
product.setCoverImageId(123L);

// 需要查询数据库获取路径
SysFileInfo fileInfo = fileMapper.selectById(123L);
String url = fileInfo.getUrl();
```

### 新系统 ✅
```java
// 直接存储URL
product.setCoverImageUrl("/files/bussiness/product_cover/456/xxx.jpg");

// 直接使用URL，无需查询
String url = product.getCoverImageUrl();
```

---

## 上传流程

### OneToOne（单文件）- 用户头像、文章封面

```javascript
// 前端
// Step 1: 上传到临时目录
const tempUrl = await uploadToTemp(file);

// Step 2: 保存业务
await saveUser({ avatar: tempUrl });
```

```java
// 后端自动处理
if (user.getAvatar().contains("/temp/")) {
    String formalPath = fileManagementService.moveTempToFormal(
        user.getAvatar(), "user_avatar", userId
    );
    user.setAvatar(formalPath);
}
```

### OneToMany（多文件）- 商品图片列表

```javascript
// 前端
// Step 1: 批量上传
const tempUrls = await uploadMultipleToTemp(files);

// Step 2: 保存业务
await saveProduct({ 
    demoImageUrls: JSON.stringify(tempUrls) 
});
```

```java
// 后端自动处理
List<String> urls = JSON.parseArray(product.getDemoImageUrls(), String.class);
List<String> tempUrls = urls.stream()
    .filter(url -> url.contains("/temp/"))
    .collect(Collectors.toList());

List<String> formalPaths = fileManagementService.moveTempToFormalBatch(
    tempUrls, "product_image", productId
);
```

### 富文本（非结构化）- 文章内容

```javascript
// 前端 - 编辑器自动处理
const editorConfig = {
    uploadImage: async (file) => {
        return await uploadToTemp(file); // 返回临时URL
    }
};

// 保存文章
await saveArticle({ 
    content: editor.getHTML() // HTML中包含临时URL
});
```

```java
// 后端自动处理
String processedContent = fileManagementService.processRichTextFiles(
    article.getContent(), "article_content", articleId
);
article.setContent(processedContent);
```

---

## API接口

### 上传文件
```http
POST /api/file/upload/temp
Content-Type: multipart/form-data

file: [文件]
```

### 批量上传
```http
POST /api/file/upload/temp/batch
Content-Type: multipart/form-data

files: [文件数组]
```

### 手动清理临时文件
```http
POST /api/file/cleanup/temp
```

---

## 目录结构

```
/files/
  ├── temp/                          # 临时文件（24小时自动清理）
  └── bussiness/                     # 业务文件
      ├── user_avatar/{userId}/      # 用户头像
      ├── article_cover/{articleId}/ # 文章封面
      ├── article_content/{articleId}/ # 文章内容图片
      ├── product_cover/{productId}/ # 商品封面
      └── product_image/{productId}/ # 商品图片列表
```

---

## 常见问题

### Q1: 临时文件什么时候清理？
**A:** 24小时后自动清理，每天凌晨2点执行定时任务。

### Q2: 如何手动清理临时文件？
**A:** 
```bash
curl -X POST http://localhost:8080/api/file/cleanup/temp
```

### Q3: 删除业务时文件会自动删除吗？
**A:** 是的，ArticleService 和 ProductService 已经集成了文件删除逻辑。

### Q4: 支持哪些文件类型？
**A:** 图片：jpg, jpeg, png, gif, webp, svg  
文档：pdf, doc, docx, xls, xlsx, ppt, pptx  
压缩包：zip, rar, 7z

### Q5: 文件大小限制？
**A:** 默认2MB，可在配置文件中修改。

---

## 完整文档

- **后端快速开始：** `springboot/FILE_MANAGEMENT_README.md`
- **前端使用指南：** `vue3/FILE_UPLOAD_GUIDE.md`
- **测试指南：** `docs/FILE_MANAGEMENT_TEST_GUIDE.md`
- **完整报告：** `FILE_MANAGEMENT_REFACTOR_COMPLETE.md`

---

## 检查清单

- [x] 代码编译通过
- [x] 无旧代码引用
- [x] 文档齐全
- [ ] 数据库迁移
- [ ] 服务启动测试
- [ ] 功能测试
- [ ] 前端页面更新

---

**下一步：执行数据库迁移 → 启动服务 → 测试功能**
