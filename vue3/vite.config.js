import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  // 根据当前工作目录中的 `mode` 加载 .env 文件
  const env = loadEnv(mode, process.cwd(), '')
  
  return {
    plugins: [vue()],
    
    // 路径别名配置
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    
    // 开发服务器配置
    server: {
      port: 4000, // 改为3000端口，避免权限问题
      host: true, // 允许外部访问
      open: false, // 是否自动打开浏览器
      proxy: {
        // 代理 /api 到后端服务
        '/api': {
          target: 'http://localhost:1239', // 后端服务地址
          changeOrigin: true,
          secure: false,
        },
        // 代理 /files 到后端服务
        '/files': {
          target: 'http://localhost:1239',
          changeOrigin: true,
          secure: false,
        }
      }
    },
    
    // 构建配置
    build: {
      outDir: 'dist', // 输出目录
      assetsDir: 'static', // 静态资源目录
      sourcemap: mode === 'development', // 开发环境生成源码映射
      minify: mode === 'production' ? 'terser' : false, // 生产环境压缩
      chunkSizeWarningLimit: 1000, // chunk 大小警告的限制
      rollupOptions: {
        output: {
          // 静态资源分类打包
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    
    // CSS 配置
    css: {
      preprocessorOptions: {
        scss: {
          // 全局 scss 变量 - 移除自动导入，由各组件按需导入
          // additionalData: `@import "@/styles/element-variables.scss";`
        }
      }
    },
    
    // 环境变量配置
    define: {
      __VUE_OPTIONS_API__: true,
      __VUE_PROD_DEVTOOLS__: false,
      // 可以在这里定义全局常量
      'process.env': env
    },
    
    // 优化配置
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'pinia',
        'axios',
        'element-plus',
        '@element-plus/icons-vue'
      ]
    }
  }
})
