# 测试文件上传返回值

## 测试命令

```bash
# 测试上传文件到临时目录
curl -X POST http://localhost:8080/api/file/upload/temp \
  -F "file=@test.jpg" \
  -H "token: YOUR_TOKEN"
```

## 预期返回

```json
{
  "code": "200",
  "msg": "操作成功",
  "data": "/files/temp/xxx_xxx.jpg"
}
```

## 检查点

1. **后端日志** - 查看 `FileUtil.saveFile()` 的日志输出：
   ```
   返回文件访问路径：/files/temp/xxx.jpg
   ```

2. **Controller日志** - 查看 `FileManagementController.uploadToTemp()` 的日志：
   ```
   上传文件到临时目录: test.jpg
   ```

3. **前端接收** - 查看前端 console.log 输出：
   ```javascript
   console.log('上传返回:', tempUrl)
   // 应该是: /files/temp/xxx.jpg
   ```

## 如果返回值不含 /files

### 可能原因1：旧数据
数据库中存储的是旧路径（没有 `/files` 前缀）

**解决方案：** 重新上传文件

### 可能原因2：前端处理错误
前端某个地方把 `/files` 去掉了

**检查：** 
- `fileManagement.js` 的 `uploadToTemp()` 方法
- `request.js` 的响应拦截器

### 可能原因3：后端配置问题
`ROOT_PATH` 常量被修改了

**检查：**
```java
// FileUtil.java
private static final String ROOT_PATH = "/files/";  // 应该是这个值
```

## 调试步骤

### 1. 检查后端日志
启动后端，上传文件，查看日志：
```
[INFO] 返回文件访问路径：/files/temp/xxx.jpg
```

### 2. 检查前端接收
在 `fileManagement.js` 的 `uploadToTemp()` 方法中添加日志：
```javascript
export function uploadToTemp(file, callbacks = {}) {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/file/upload/temp', formData, {
    ...callbacks,
    onSuccess: (data) => {
      console.log('后端返回的路径:', data);  // 添加这行
      if (callbacks.onSuccess) {
        callbacks.onSuccess(data);
      }
    }
  });
}
```

### 3. 检查数据库
如果是编辑文章，检查数据库中的 `cover_image` 字段：
```sql
SELECT id, title, cover_image FROM article WHERE id = 'xxx';
```

应该看到：`/files/bussiness/article_cover/xxx/xxx.jpg`

## 修复建议

如果确认后端返回的路径不含 `/files`，需要修改 `FileUtil.java`：

```java
// 确保 ROOT_PATH 是正确的
private static final String ROOT_PATH = "/files/";

// 确保返回语句是正确的
String relativePath = ROOT_PATH + relativeDir + "/" +
    (StrUtil.isNotBlank(folderName) ? folderName + "/" : "") + uniqueFilename;
return relativePath;
```

## 当前代码状态

✅ `FileUtil.java` - ROOT_PATH = "/files/"
✅ `FileManagementService.java` - 直接返回 FileUtil.saveFile() 的结果
✅ `FileManagementController.java` - 直接返回 Service 的结果

**理论上后端返回的路径应该包含 `/files` 前缀！**

如果实际返回不含 `/files`，请提供：
1. 后端日志输出
2. 前端 console.log 输出
3. 网络请求的 Response 数据
