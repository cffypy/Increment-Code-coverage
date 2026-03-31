# Super-Jacoco

基于 [didi/super-jacoco](https://github.com/didi/super-jacoco) 二次开发，升级至 **Java 21 + Spring Boot 3.2** 技术栈的一站式 Java 代码全量/增量覆盖率收集平台。

能够低成本、无侵入地收集代码覆盖率数据；既支持 JVM 运行时段的覆盖率收集，也能与部署环境无缝对接，收集服务端自定义时间段的代码全量/增量覆盖率，并提供可视化的 HTML 覆盖率报表，支撑精准测试落地。

---

## 产品特性

- **通用**：既支持单元测试覆盖率收集，也支持手工测试覆盖率收集；既支持全量覆盖率，也支持 diff 增量覆盖率
- **无侵入**：采用 on-the-fly 模式，无需对业务代码做任何改造即可收集覆盖率数据
- **高可用**：分布式架构，任务机可水平扩展，避免单点性能瓶颈
- **可视化**：提供 HTML 格式的覆盖率报告，可读性高

---

## Java 技术栈升级说明

本项目在原版 didi/super-jacoco（Java 8 + Spring Boot 2.x）基础上，完成了全面的现代化升级：

### 版本升级总览

| 组件 | 升级前 | 升级后 |
|------|--------|--------|
| **Java** | 8 | **21** |
| **Spring Boot** | 2.0.2.RELEASE | **3.2.5** |
| **Spring Cloud** | - | **2023.0.1** |
| **MyBatis Starter** | 1.3.2 | **3.0.3** |
| **MySQL Connector** | 旧版 | **8.3.0** (mysql-connector-j) |
| **JaCoCo Core** | 0.8.5 | **0.8.12** |
| **ASM** | 旧版 | **9.7** |
| **API 文档** | Springfox Swagger 2 | **SpringDoc OpenAPI 2.5** |
| **Maven Compiler** | - | **3.13.0** |
| **Lombok** | 旧版 | **1.18.34** |

### 核心升级内容

#### 1. Jakarta EE 命名空间迁移
Spring Boot 3.x 要求从 `javax.*` 迁移到 `jakarta.*`，项目中所有相关包引用已完成迁移：
```java
// 升级前
import javax.validation.Valid;
import javax.sql.DataSource;

// 升级后
import jakarta.validation.Valid;
import javax.sql.DataSource;  // javax.sql 属于 Java SE，无需迁移
```

#### 2. API 文档框架替换
Springfox Swagger 不支持 Spring Boot 3.x，已替换为 SpringDoc OpenAPI：
```java
// 升级前：Springfox Swagger 2
@EnableSwagger2

// 升级后：SpringDoc OpenAPI
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Super-Jacoco API").version("1.0"));
    }
}
```
- Swagger UI 地址：`http://localhost:8899/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8899/v3/api-docs`

#### 3. 数据源配置重构
数据源从 `application.properties` 直连方式重构为 **Apollo 配置中心 + HikariCP** 方式，支持动态配置管理：
```
# Apollo 配置示例
hikaricp.data = {"db0":["hikaricp.db0-0"]}
hikaricp.db0-0.jdbcUrl = jdbc:mysql://host:3306/super-jacoco?...
hikaricp.db0-0.username = xxx
hikaricp.db0-0.password = xxx
hikaricp.db0-0.maximumPoolSize = 20
```

#### 4. 其他适配项
- Spring Boot 3.x 内嵌 Tomcat 路径变更适配
- `spring.web.resources.static-locations` 替代旧版 `spring.resources.static-locations`
- `server.servlet.encoding.force` 替代旧版配置
- `spring.autoconfigure.exclude` 配置适配
- 编译目标从 Java 8 提升到 Java 21，Lombok 注解处理器路径显式声明

---

## 使用方法

### 1. 环境要求

- **JDK 21** 或以上
- **Maven 3.8+**
- **MySQL 8.0+**
- **Apollo 配置中心**（用于数据源等配置管理）

### 2. 数据库初始化

安装 MySQL 数据库，创建数据库后执行建表 SQL：

```sql
-- 执行 sql/coverage_report.sql
CREATE DATABASE `super-jacoco` DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;
-- 然后执行文件中的建表语句
```

### 3. 配置

#### Apollo 配置中心

在 `bootstrap.properties` 中配置 Apollo 连接信息：
```properties
app.id=qa-icc
apollo.bootstrap.enabled=true
```

在 Apollo 中配置数据源信息（参见上方"数据源配置重构"章节）。

#### 应用配置

`application.properties` 中的 Git 账号信息：
```properties
gitlab.username=your_username
gitlab.password=your_password
```

### 4. 编译打包

```bash
mvn clean package -DskipTests
```

### 5. 部署启动

```bash
# 启动
nohup java -jar target/super-jacoco.jar > nohup.out 2>&1 &

# 停止
./stop.sh

# 查看日志
tail -f nohup.out
```

默认端口为 **8899**。

---

## 覆盖率收集接口

### 单测覆盖率

#### 触发覆盖率收集
```
POST /cov/triggerUnitCover
Content-Type: application/json

{
  "uuid": "uuid",
  "type": 1,
  "gitUrl": "git@git",
  "subModule": "",
  "baseVersion": "master",
  "nowVersion": "feature",
  "envType": "-Ptest"
}

Response: {"code": 200, "data": true, "msg": "msg"}
```

#### 获取覆盖率结果
```
GET /cov/getUnitCoverResult?uuid={uuid}

Response:
{
  "code": 200,
  "data": {
    "coverStatus": 1,       // -1=失败, 0=进行中, 1=成功
    "errMsg": "msg",
    "lineCoverage": 100.0,
    "branchCoverage": 100.0,
    "logFile": "file content",
    "reportUrl": "http://..."
  },
  "msg": "msg"
}
```

### 环境覆盖率

#### 触发覆盖率收集
```
POST /cov/triggerEnvCov
Content-Type: application/json

{
  "uuid": "uuid",
  "type": 1,
  "gitUrl": "git@git",
  "subModule": "",
  "baseVersion": "master",
  "nowVersion": "feature",
  "address": "127.0.0.1",
  "port": "8088"
}

Response: {"code": 200, "data": true, "msg": "msg"}
```

> **注意**：`address` 和 `port` 为目标服务的部署地址。需要在目标服务启动时添加 JaCoCo Agent 参数：
> ```
> -javaagent:/path/to/org.jacoco.agent-0.8.12-runtime.jar=includes=*,output=tcpserver,address=*,port=18513
> ```

#### 获取覆盖率结果
```
GET /cov/getEnvCoverResult?uuid={uuid}

Response: (同单测覆盖率结果格式)
```

### 本地覆盖率

#### 获取本地覆盖率结果
```
POST /cov/getLocalCoverResult
Content-Type: application/json

适用于代码部署和覆盖率服务在同一机器上的场景，可直接读取本机源码和 class 文件。
```

---

## 项目结构

```
super-jacoco/
├── src/main/java/com/xiaoju/basetech/
│   ├── CodeCovApplication.java          # 主启动类
│   ├── config/
│   │   ├── DefaultDBConfig.java         # Apollo + HikariCP 数据源配置
│   │   ├── SwaggerConfig.java           # SpringDoc OpenAPI 配置
│   │   ├── GlobalExceptionHandler.java  # 全局异常处理
│   │   ├── ScheduleConfig.java          # 定时任务配置
│   │   └── InitConfig.java             # 初始化配置
│   ├── controller/
│   │   └── CodeCovController.java       # 覆盖率 REST 接口
│   ├── service/
│   │   ├── CodeCovService.java          # 覆盖率服务接口
│   │   └── impl/
│   │       ├── CodeCovServiceImpl.java  # 覆盖率服务实现
│   │       └── DiffMethodsCalculator.java # 增量方法计算
│   ├── entity/                          # 数据实体
│   ├── dao/                             # MyBatis DAO 层
│   ├── job/                             # 定时任务
│   └── util/                            # 工具类
├── src/main/resources/
│   ├── application.properties           # 应用配置
│   ├── bootstrap.properties             # Apollo 引导配置
│   └── mapper/                          # MyBatis XML 映射文件
├── sql/                                 # 数据库初始化脚本
├── jacoco/                              # JaCoCo 相关资源
├── start.sh                             # 启动脚本
├── stop.sh                              # 停止脚本
└── pom.xml                              # Maven 配置
```

---

## 致谢

本项目 fork 自 [didi/super-jacoco](https://github.com/didi/super-jacoco)，感谢滴滴出行开源团队的贡献。
