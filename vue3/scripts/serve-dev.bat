@echo off
echo ========================================
echo 启动开发环境前端 (端口: 4000)
echo ========================================

cd /d %~dp0..

echo 当前目录: %cd%
echo 安装依赖...
npm install

echo 启动开发服务器 (http://localhost:4000)...
echo 使用 Vite 构建工具
npm run serve:dev
