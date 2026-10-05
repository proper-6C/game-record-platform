# 后端镜像：多阶段构建（maven 打包 -> jre17 运行）
# 构建：docker build -f deploy/backend.Dockerfile -t game-record-backend:1.0 .
# 运行（配合 docker-compose 使用，见 deploy/docker-compose.yml）

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 先复制 pom 以利用依赖缓存
COPY pom.xml .
RUN mvn -B dependency:go-offline || true
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# 上传目录挂载点（图片/视频/海报）
RUN mkdir -p /app/uploads
COPY --from=build /app/target/game-record-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
