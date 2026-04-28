#!/bin/bash

echo "=== Spring Boot 服务启动脚本 ==="

# 检查Java环境
JAVA_HOME="/www/server/java/jdk-17.0.8"
JAVA_BIN="$JAVA_HOME/bin/java"

if [ ! -f "$JAVA_BIN" ]; then
    echo "错误: 未找到Java执行文件: $JAVA_BIN"
    echo "请检查Java安装路径"
    exit 1
fi

echo "Java版本信息:"
$JAVA_BIN -version
echo ""

# 应用配置
APP_NAME="springboot-0.0.1-SNAPSHOT.jar"
APP_PATH="/www/wwwroot/$APP_NAME"
LOG_FILE="/www/wwwroot/springboot.log"
PID_FILE="/www/wwwroot/springboot.pid"

# 检查应用文件
if [ ! -f "$APP_PATH" ]; then
    echo "错误: 未找到应用文件: $APP_PATH"
    exit 1
fi

# 停止现有服务
echo "检查现有服务..."
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo "停止现有服务 (PID: $OLD_PID)..."
        kill $OLD_PID
        sleep 3
        if ps -p $OLD_PID > /dev/null 2>&1; then
            echo "强制停止服务..."
            kill -9 $OLD_PID
        fi
    fi
    rm -f "$PID_FILE"
fi

# 清理旧的Java进程
echo "清理旧的Java进程..."
pkill -f "springboot-0.0.1-SNAPSHOT.jar"
sleep 2

# 启动服务
echo "启动Spring Boot服务..."
echo "应用路径: $APP_PATH"
echo "日志文件: $LOG_FILE"
echo "端口: 8080"
echo ""

nohup $JAVA_BIN -jar \
    -Xmx1024M \
    -Xms256M \
    -Dserver.port=8080 \
    -Dspring.profiles.active=prod \
    "$APP_PATH" \
    > "$LOG_FILE" 2>&1 &

# 保存PID
NEW_PID=$!
echo $NEW_PID > "$PID_FILE"

echo "服务已启动，PID: $NEW_PID"
echo "日志文件: $LOG_FILE"
echo ""

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
if ps -p $NEW_PID > /dev/null 2>&1; then
    echo "✓ 服务启动成功"
    
    # 测试API接口
    echo "测试API接口..."
    sleep 5
    
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/user/login)
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "401" ]; then
        echo "✓ API接口响应正常 (HTTP $HTTP_CODE)"
    else
        echo "⚠ API接口响应异常 (HTTP $HTTP_CODE)"
        echo "查看日志:"
        tail -20 "$LOG_FILE"
    fi
else
    echo "✗ 服务启动失败"
    echo "查看日志:"
    tail -20 "$LOG_FILE"
    exit 1
fi

echo ""
echo "=== 服务信息 ==="
echo "PID: $NEW_PID"
echo "端口: 8080"
echo "日志: tail -f $LOG_FILE"
echo "停止: kill $NEW_PID"
echo ""
echo "Nginx代理配置应该指向: http://127.0.0.1:8080"
