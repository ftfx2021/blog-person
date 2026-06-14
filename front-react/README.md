# React Hello World 教程

## 1. 我做了什么

在 `src/helloword.jsx` 创建了最简单的 React 组件：

```jsx
function HelloWorld() {
  return (
    <div>
      <h1>Hello, World!</h1>
      <p>这是我的第一个React组件</p>
    </div>
  )
}

export default HelloWorld
```

然后在 `App.jsx` 中引入并使用了这个组件。

## 2. 如何运行

在终端执行：
```bash
npm run dev
```

## 3. 查看页面

打开浏览器访问：http://localhost:5174

你会看到：
- 一个大标题 "Hello, World!"
- 一段文字 "这是我的第一个React组件"

## 4. 代码解释

- `function HelloWorld()` - 定义一个 React 组件
- `return (...)` - 返回 JSX（类似 HTML 的语法）
- `export default` - 让其他文件可以导入这个组件
- `import HelloWorld from './helloword.jsx'` - 在 App.jsx 中导入组件
- `<HelloWorld />` - 使用组件

## 5. 修改试试

编辑 `src/helloword.jsx`，修改文字内容，保存后浏览器会自动刷新。