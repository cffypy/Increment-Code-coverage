package com.xiaoju.basetech;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import lombok.extern.java.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableApolloConfig
@Log
public class CodeCovApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeCovApplication.class, args);
    }

}
