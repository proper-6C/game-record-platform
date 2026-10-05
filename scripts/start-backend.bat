@echo off
rem 启动后端（生产方式：java -jar）
rem 前置条件：已执行 mvn clean package -DskipTests 生成 target\game-record-1.0.0.jar
setlocal
set JAVA_HOME=D:\soft\tools\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "%~dp0.."
start "game-record-backend" /min java -jar target\game-record-1.0.0.jar
echo 后端已在后台启动（端口 8080），日志输出到控制台窗口。
echo 关闭窗口即停止后端。
endlocal
