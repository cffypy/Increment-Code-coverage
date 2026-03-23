package com.xiaoju.basetech.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("第三方服务api")
                        .description("Spring-Boot-RESTFUL风格的接口文档在线自动生成")
                        .version("1.0"));
    }

}
