package com.nmm.study.configuration;

import com.nmm.study.route.CustomerRouteLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 配置zuul路由规则，可以参考ZuulConfiguration
 */
@Configuration
@EnableConfigurationProperties(ZuulProperties.class)
@Import({ServerPropertiesAutoConfiguration.class})
public class CustomerZullConfiguration {
    @Autowired
    private ServerProperties serverProperties;
    @Autowired
    private ZuulProperties zuulProperties;
    //创建自定义的RouteLocator,再用到的地方发送事件
    @Bean
    public RouteLocator routeLocator (){
        return new CustomerRouteLocator(serverProperties.getServletPrefix(),zuulProperties);
    }
}
