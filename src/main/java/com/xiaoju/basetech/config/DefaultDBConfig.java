package com.xiaoju.basetech.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 数据源配置类 - 参照 cloud-convert 项目的 DefaultDBConfig 方式
 * <p>
 * 从 Apollo 配置中心读取 hikaricp 前缀的数据库连接信息，
 * 通过 HikariCP 连接池构建 DataSource。
 * <p>
 * Apollo 中需要配置以下属性（以 hikaricp.data JSON 指定前缀）：
 * <pre>
 *   hikaricp.data = {"db0":["hikaricp.db0-0"]}
 *
 *   hikaricp.db0-0.poolName = super-jacoco-pool
 *   hikaricp.db0-0.driverClassName = com.mysql.cj.jdbc.Driver
 *   hikaricp.db0-0.jdbcUrl = jdbc:mysql://xxx:3306/super-jacoco?...
 *   hikaricp.db0-0.username = xxx
 *   hikaricp.db0-0.password = xxx
 *   hikaricp.db0-0.maximumPoolSize = 20
 *   hikaricp.db0-0.minimumIdle = 5
 *   hikaricp.db0-0.connectionTimeout = 30000
 *   hikaricp.db0-0.idleTimeout = 600000
 *   hikaricp.db0-0.maxLifetime = 1800000
 * </pre>
 */
@Configuration
@MapperScan(basePackages = "com.xiaoju.basetech.dao", sqlSessionFactoryRef = "sqlSessionFactory")
public class DefaultDBConfig {

    private static final Logger log = LoggerFactory.getLogger(DefaultDBConfig.class);

    @Autowired
    private Environment env;

    /**
     * 从 Apollo Environment 中按前缀构建 HikariCP 数据源
     * 与 cloud-convert 的 DBConfig.dataSource(String prefix) 逻辑一致
     */
    private DataSource createDataSource(String prefix) {
        HikariConfig config = new HikariConfig();

        config.setPoolName(env.getProperty(prefix + ".poolName", "super-jacoco-pool"));
        config.setDriverClassName(env.getProperty(prefix + ".driverClassName", "com.mysql.cj.jdbc.Driver"));
        config.setJdbcUrl(env.getProperty(prefix + ".jdbcUrl"));
        config.setUsername(env.getProperty(prefix + ".username"));
        config.setPassword(env.getProperty(prefix + ".password"));

        // 连接池参数（提供合理默认值）
        String readOnly = env.getProperty(prefix + ".readOnly");
        if (readOnly != null) {
            config.setReadOnly(Boolean.parseBoolean(readOnly));
        }

        String connectionTimeout = env.getProperty(prefix + ".connectionTimeout");
        if (connectionTimeout != null) {
            config.setConnectionTimeout(Long.parseLong(connectionTimeout));
        }

        String idleTimeout = env.getProperty(prefix + ".idleTimeout");
        if (idleTimeout != null) {
            config.setIdleTimeout(Long.parseLong(idleTimeout));
        }

        String maxLifetime = env.getProperty(prefix + ".maxLifetime");
        if (maxLifetime != null) {
            config.setMaxLifetime(Long.parseLong(maxLifetime));
        }

        String maximumPoolSize = env.getProperty(prefix + ".maximumPoolSize");
        if (maximumPoolSize != null) {
            config.setMaximumPoolSize(Integer.parseInt(maximumPoolSize));
        }

        String minimumIdle = env.getProperty(prefix + ".minimumIdle");
        if (minimumIdle != null) {
            config.setMinimumIdle(Integer.parseInt(minimumIdle));
        }

        log.info("Creating HikariCP DataSource with prefix [{}], jdbcUrl={}", prefix, config.getJdbcUrl());
        return new HikariDataSource(config);
    }

    /**
     * 参照 cloud-convert 的 DefaultDBConfig.dataSource() 方式
     * 从 hikaricp.data JSON 中解析配置前缀，构建主数据源
     */
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        String hikaricpData = env.getProperty("hikaricp.data", String.class);

        if (hikaricpData != null && !hikaricpData.isBlank()) {
            // cloud-convert 模式: 从 hikaricp.data JSON 解析前缀
            JSONObject datas = JSON.parseObject(hikaricpData);
            // 取第一个逻辑库的第一个数据源前缀（单库场景）
            for (String dbKey : datas.keySet()) {
                JSONArray prefixes = datas.getJSONArray(dbKey);
                if (prefixes != null && !prefixes.isEmpty()) {
                    String prefix = prefixes.getString(0);
                    log.info("Using hikaricp.data mode, dbKey={}, prefix={}", dbKey, prefix);
                    return createDataSource(prefix);
                }
            }
        }

        // 兜底: 直接使用 hikaricp.db0-0 作为前缀
        log.info("hikaricp.data not found or empty, falling back to prefix 'hikaricp.db0-0'");
        return createDataSource("hikaricp.db0-0");
    }

    @Primary
    @Bean(name = "transactionManager")
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Primary
    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource);
        sessionFactoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        sessionFactoryBean.setTypeAliasesPackage("com.xiaoju.basetech.entity");

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setCacheEnabled(true);
        configuration.setLazyLoadingEnabled(false);
        sessionFactoryBean.setConfiguration(configuration);

        return sessionFactoryBean.getObject();
    }
}
