# 使用官方的OpenJDK 17镜像作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制Maven构建的JAR文件
COPY target/springboot-0.0.1-SNAPSHOT.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 创建文件上传目录
RUN mkdir -p /app/files

# 暴露端口
EXPOSE 8080

# 设置环境变量
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
