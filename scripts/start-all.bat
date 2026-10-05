@echo off
rem 一键启动：先打包后端，再启动后端与前端静态服务
setlocal
set JAVA_HOME=D:\soft\tools\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "%~dp0.."

echo [1/3] 打包后端 jar ...
call mvn -s D:\soft\tools\maven\conf\settings.xml clean package -DskipTests >nul
if errorlevel 1 (
  echo 打包失败，请检查 Maven 配置。
  exit /b 1
)

echo [2/3] 启动后端（8080）...
start "game-record-backend" /min java -jar target\game-record-1.0.0.jar

echo [3/3] 启动前端静态预览（8081，需 npx 支持）...
start "game-record-frontend" /min npx --yes serve frontend\dist -l 8081

echo 后端 http://localhost:8080   前端 http://localhost:8081
endlocal
