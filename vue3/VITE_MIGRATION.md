# Vue3 项目 Vite 迁移说明

## 迁移概述

本项目已成功从 Vue CLI (webpack) 迁移到 Vite 构建工具。

## 主要变更

### 1. 构建工具
- **之前**: Vue CLI + webpack
- **现在**: Vite

### 2. 配置文件变更
- **删除**: `vue.config.js`, `babel.config.js`
- **新增**: `vite.config.js`
- **更新**: `package.json`, `jsconfig.json`

### 3. HTML 入口文件
- **移动**: `public/index.html` → `index.html` (根目录)
- **更新**: 移除 webpack 模板语法，添加 ES 模块引入

### 4. 依赖包变更

#### 移除的依赖
```json
{
  "@babel/core": "^7.26.8",
  "@babel/eslint-parser": "^7.26.8",
  "@vue/cli-plugin-babel": "~5.0.0",
  "@vue/cli-plugin-eslint": "~5.0.0",
  "@vue/cli-plugin-router": "~5.0.0",
  "@vue/cli-service": "~5.0.0",
  "sass-loader": "^12.0.0"
}
```

#### 新增的依赖
```json
{
  "@vitejs/plugin-vue": "^5.0.0",
  "@vue/eslint-config-prettier": "^9.0.0",
  "prettier": "^3.2.0",
  "vite": "^5.0.0"
}
```

### 5. 脚本命令更新
```json
{
  "dev": "vite --mode development",
  "serve": "vite --mode development", 
  "build": "vite build --mode production",
  "preview": "vite preview"
}
```

### 6. 环境变量
- **访问方式**: `process.env.VUE_APP_*` → `import.meta.env.VITE_*`
- **前缀**: 环境变量必须以 `VITE_` 开头

## 配置特性

### 开发服务器
- **端口**: 4000 (避免权限问题)
- **主机**: true (允许外部访问)
- **代理**: 
  - `/api` → `http://localhost:1239`
  - `/files` → `http://localhost:1239`

### 构建优化
- **开发环境**: 生成 sourcemap
- **生产环境**: 启用 terser 压缩
- **静态资源**: 分类打包到 `static/` 目录

### 路径别名
- `@` → `src/`

### CSS 预处理
- **Sass**: 全局导入 `element-variables.scss`

## 启动方式

### 开发环境
```bash
npm run dev
# 或
npm run serve:dev
```

### 构建
```bash
# 开发环境构建
npm run build:dev

# 生产环境构建  
npm run build:prod

# 预览构建结果
npm run preview
```

### 使用脚本
```bash
# Windows
scripts/serve-dev.bat  # 启动开发服务器 (http://localhost:4000)
scripts/build-dev.bat
scripts/build-prod.bat
```

## 注意事项

1. **首次运行**: 需要重新安装依赖
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```

2. **环境变量**: 如需自定义环境变量，创建 `.env.*` 文件，变量必须以 `VITE_` 开头

3. **兼容性**: 
   - 现有的 `@` 路径别名继续有效
   - Element Plus 配置已优化，样式导入顺序调整
   - 开发服务器端口改为4000，避免权限问题
   - 所有现有功能保持兼容

4. **性能提升**: 
   - 开发服务器启动更快
   - 热更新速度提升
   - 构建速度显著改善

## 迁移完成

✅ 项目已成功迁移到 Vite，所有功能保持兼容性，性能得到显著提升。
