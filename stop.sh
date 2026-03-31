#!/bin/bash

PID=$(ps aux | grep "super-jacoco.jar" | grep -v grep | awk '{print $2}')

if [ -n "$PID" ]; then
    echo "正在停止应用，PID: $PID"
    kill -15 $PID
    sleep 3
    
    # 如果还在运行，强制杀掉
    if ps -p $PID > /dev/null 2>&1; then
        echo "进程未停止，强制终止..."
        kill -9 $PID
    fi
    
    echo "应用已停止"
else
    echo "应用未运行"
fi
