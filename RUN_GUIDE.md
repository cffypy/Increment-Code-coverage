# Super Jacoco 运行指南

## 项目信息

- **项目名称**: super-jacoco
- **Spring Boot 版本**: 2.0.2.RELEASE
- **Java 版本要求**: Java 8
- **运行端口**: 8899
- **Swagger UI**: http://localhost:8899/swagger-ui.html

## 重要说明

⚠️ **本项目必须使用 Java 8 运行！**

- 项目基于 Spring Boot 2.0.2，不兼容 Java 9 及以上版本
- 如果使用 Java 11/17/21 等高版本会出现模块系统反射访问错误

## 快速开始

### 1. 编译打包

```bash
# 使用 Java 8 编译
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home mvn clean package -DskipTests
```

### 2. 启动应用

```bash
# 方式 1: 使用启动脚本（推荐）
./start.sh

# 方式 2: 手动启动
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
nohup java -jar target/super-jacoco.jar > nohup.out 2>&1 &
```

### 3. 停止应用

```bash
# 方式 1: 使用停止脚本（推荐）
./stop.sh

# 方式 2: 手动停止
ps aux | grep super-jacoco.jar | grep -v grep | awk '{print $2}' | xargs kill -15
```

### 4. 查看日志

```bash
# 实时查看日志
tail -f nohup.out

# 查看最近的日志
tail -100 nohup.out

# 查看应用日志文件
tail -f log/super-jacoco.log
```

### 5. 检查应用状态

```bash
# 检查进程
ps aux | grep super-jacoco.jar

# 检查端口
lsof -i :8899

# 测试接口
curl http://localhost:8899/swagger-ui.html
```

## 可用的 API 端点

根据启动日志，应用提供以下 REST API：

- `POST /cov/triggerUnitCover` - 触发单元测试覆盖率
- `GET /cov/getEnvCoverResult` - 获取环境覆盖率结果
- `POST /cov/getLocalCoverResult` - 获取本地覆盖率结果
- `GET /cov/getUnitCoverResult` - 获取单元测试覆盖率结果
- `POST /cov/triggerEnvCov` - 触发环境覆盖率

访问 http://localhost:8899/swagger-ui.html 查看完整的 API 文档。

## 常见问题

### Q1: 启动时报错 `InaccessibleObjectException`？

**原因**: 使用了 Java 9 或更高版本运行应用。

**解决方案**: 确保使用 Java 8：
```bash
java -version  # 应该显示 1.8.x
./start.sh     # 使用提供的启动脚本
```

### Q2: 编译时找不到依赖？

**解决方案**: 
```bash
# 确保使用公司的 Nexus 仓库
mvn dependency:resolve

# 如果还有问题，清理并重新下载
mvn clean
rm -rf ~/.m2/repository/tk/mybatis/
mvn package -DskipTests
```

### Q3: 编译时找不到符号（ASM 相关）？

**原因**: 代码中使用了 JDK 内部的 ASM 包。

**解决方案**: 已修复，使用项目依赖中的 `org.objectweb.asm` 包。

### Q4: 如何更改运行端口？

编辑 `src/main/resources/application.properties`：
```properties
server.port=8899  # 改为你想要的端口
```

然后重新编译和启动。

## 数据库配置

检查 `src/main/resources/application.properties` 确认数据库配置：
- 数据库 URL
- 用户名和密码
- 连接池设置

## 项目结构

```
super-jacoco/
├── src/main/java/
│   └── com/xiaoju/basetech/
│       ├── CodeCovApplication.java    # 主启动类
│       ├── controller/                 # REST 控制器
│       ├── service/                    # 业务服务
│       └── util/                       # 工具类
├── target/
│   └── super-jacoco.jar               # 打包后的可执行 jar
├── log/                                # 应用日志目录
├── nohup.out                          # 启动日志
├── start.sh                           # 启动脚本
├── stop.sh                            # 停止脚本
└── pom.xml                            # Maven 配置

```

## 维护建议

1. **定期备份日志**: `log/` 目录会持续增长
2. **监控内存使用**: 可以在 `start.sh` 中添加 JVM 参数，如 `-Xmx512m`
3. **更新依赖**: 考虑升级到更新的 Spring Boot 版本（需要测试兼容性）

## 开发团队

- 原作者: gaoweiwei_v
- 维护者: [Your Name]

---

**最后更新**: 2026-03-18
