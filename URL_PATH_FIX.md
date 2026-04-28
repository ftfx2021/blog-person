# 文件URL路径修复

## 问题描述

封面图片URL显示错误：
```
错误：http://localhost:4000/back/article/edit/bussiness/article_cover/xxx/xxx.png
正确：http://localhost:4000/files/bussiness/article_cover/xxx/xxx.png
```

## 问题原因

`FileManagementService.moveTempToFormal()` 方法返回的是相对路径，没有 `/files/` 前缀。

### 旧代码
```java
public String moveTempToFormal(...) {
    // ...
    return targetPath;  // 返回：bussiness/article_cover/xxx/xxx.png
}
```

### 问题
- 数据库存储：`bussiness/article_cover/xxx/xxx.png`
- 前端显示：直接使用这个路径
- 浏览器解析：相对于当前页面路径，变成 `/back/article/edit/bussiness/...`

## 修复方案

### ✅ 修改1：moveTempToFormal() 返回完整URL
```java
public String moveTempToFormal(...) {
    // ...
    String fullUrl = "/files/" + targetPath;
    return fullUrl;  // 返回：/files/bussiness/article_cover/xxx/xxx.png
}
```

### ✅ 修改2：processRichTextFiles() 不再添加前缀
```java
// 旧代码
String formalPath = moveTempToFormal(...);
String formalUrl = "/" + formalPath;  // 会变成 //files/xxx

// 新代码
String formalUrl = moveTempToFormal(...);  // 已经是 /files/xxx
```

## 修复后的路径格式

### 临时文件
- 上传后：`/files/temp/xxx.jpg`
- 数据库：`/files/temp/xxx.jpg`
- 前端显示：`http://localhost:4000/files/temp/xxx.jpg` ✅

### 正式文件
- 移动后：`/files/bussiness/article_cover/123/xxx.jpg`
- 数据库：`/files/bussiness/article_cover/123/xxx.jpg`
- 前端显示：`http://localhost:4000/files/bussiness/article_cover/123/xxx.jpg` ✅

## 验证

### 1. 编译通过
```bash
cd springboot
mvn clean compile -DskipTests
# BUILD SUCCESS ✅
```

### 2. 测试上传
```bash
# 上传文件
curl -X POST http://localhost:8080/api/file/upload/temp -F "file=@test.jpg"
# 返回：/files/temp/xxx.jpg

# 保存文章（后端会自动移动文件）
# 数据库存储：/files/bussiness/article_cover/123/xxx.jpg
# 前端显示：http://localhost:4000/files/bussiness/article_cover/123/xxx.jpg
```

## 注意事项

1. **数据库中的路径格式**：统一为 `/files/xxx` 格式
2. **前端无需处理**：直接使用后端返回的URL
3. **静态资源配置**：确保后端配置了 `/files/**` 的静态资源映射

## 相关文件

- `springboot/src/main/java/org/example/springboot/service/FileManagementService.java`
  - `moveTempToFormal()` - 返回完整URL
  - `processRichTextFiles()` - 不再添加前缀

## 状态

✅ 已修复
✅ 编译通过
⏳ 等待测试
