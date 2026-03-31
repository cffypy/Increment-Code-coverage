#!/bin/bash

# 设置 Java 8 环境
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# 停止旧进程
PID=$(ps aux | grep "super-jacoco.jar" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "停止旧进程: $PID"
    kill -9 $PID
    sleep 2
fi

# 启动应用
echo "使用 Java 8 启动应用..."
java -version
nohup java -jar target/super-jacoco.jar > nohup.out 2>&1 &

echo "应用已启动，PID: $!"
echo "查看日志: tail -f nohup.out"
