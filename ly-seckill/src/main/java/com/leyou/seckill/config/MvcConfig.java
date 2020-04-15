package com.leyou.seckill.config;

import com.leyou.seckill.interceptor.SeckillRequestInterceptor;
import com.leyou.seckill.interceptor.UserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtProperties prop;

    // 注入ioc中，实现单例
    @Bean
    public UserInterceptor userInterceptor(){
        return new UserInterceptor(prop);
    }

    @Autowired
    private SeckillRequestInterceptor requestInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加用户登录校验拦截器
        registry.addInterceptor(userInterceptor()).addPathPatterns("/seckill/**");
        // 添加限制请求次数拦截器
        registry.addInterceptor(requestInterceptor).addPathPatterns("/seckill/seck");
    }
}
