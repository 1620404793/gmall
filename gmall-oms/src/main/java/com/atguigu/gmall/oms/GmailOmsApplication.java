package com.atguigu.gmall.oms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
@EnableSwagger2
@MapperScan("com.atguigu.gmall.oms.dao")
public class GmailOmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmailOmsApplication.class, args);
    }

}
