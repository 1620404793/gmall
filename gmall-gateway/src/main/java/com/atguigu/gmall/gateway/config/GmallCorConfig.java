package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GmallCorConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        /*cors跨域配置对象*/
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:1000");//设置允许访问服务端的域名
        config.setAllowCredentials(true);  //设置是否允许携带cookie
        config.addAllowedMethod("*");  //代表允许所有的方法都可以进行访问
        config.addAllowedHeader("*");

        /*配置源对象（配置被拦截的请求）*/
        UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        corsConfigurationSource.registerCorsConfiguration("/**", config);

        //cors过滤器对象
        return new CorsWebFilter(corsConfigurationSource);
    }
}
