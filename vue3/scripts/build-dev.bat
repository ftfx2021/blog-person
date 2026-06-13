@echo off
echo ========================================
echo 构建开发环境前端 (使用 Vite)
echo ========================================

cd /d %~dp0..

echo 当前目录: %cd%
echo 安装依赖...
npm install

echo 构建开发环境...
echo 使用 Vite 构建工具
npm run build:dev

echo 构建完成！
echo 输出目录: dist\

pause
