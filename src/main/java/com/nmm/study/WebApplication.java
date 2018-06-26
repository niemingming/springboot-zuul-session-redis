package com.nmm.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.session.data.redis.RedisFlushMode;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * @author nmm 2018/6/26
 * @description
 */
@SpringBootApplication
@EnableZuulProxy//允许zuul代理，配置session存储后立刻刷新设置刷新模式为立刻刷新，否则可能获取不到session。
@EnableRedisHttpSession(redisFlushMode = RedisFlushMode.IMMEDIATE)
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class,args);
    }
}
