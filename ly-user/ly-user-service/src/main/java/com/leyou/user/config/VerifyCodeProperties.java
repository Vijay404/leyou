package com.leyou.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@Data
@PropertySource("classpath:application.yml")
@ConfigurationProperties(prefix = "ly.vc")
public class VerifyCodeProperties {

    private String exchange;

    private String routingKey;

    private Long timeOut;

    private Integer codeLen;
}
