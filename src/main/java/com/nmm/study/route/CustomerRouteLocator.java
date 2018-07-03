package com.nmm.study.route;

import com.nmm.study.storage.RouteCache;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 实现RefreshableRouteLocator接口，即可实现路由的动态刷新，
 * 考虑到基本配置信息的加载和处理细节，我们继承默认的SimpleRouteLocator帮我们处理细节，我们只需要覆盖其中的加载路由方法
 * 实际代码中我们可以在ZUulConfiguration中看到，配置了Listener监听路由规则改变的事件，一旦接受到事件信息，就设置为脏，提醒
 * zuul刷新路由，也就是调用RouteLocator。调用的RouteLocator是事件携带的，我们这里当然就是我们提供的CustomerRouteLocator
 */
public class CustomerRouteLocator extends SimpleRouteLocator implements RefreshableRouteLocator {

    private ZuulProperties zuulProperties;

    public CustomerRouteLocator(String servletPath, ZuulProperties properties) {
        super(servletPath, properties);
        this.zuulProperties = properties;
    }

    /**
     * 调用doRefresh方法，刷新路由。
     */
    @Override
    public void refresh() {
        doRefresh();
    }

    /**
     * 复写LocateRoutes方法，重新加载路由规则
     * @return
     */
    @Override
    protected Map<String, ZuulProperties.ZuulRoute> locateRoutes() {
        LinkedHashMap<String,ZuulProperties.ZuulRoute> routes = new LinkedHashMap<String, ZuulProperties.ZuulRoute>();
        //首先调用父类方法，完成默认配置信息加载
        Map<String,ZuulProperties.ZuulRoute> tmp = super.locateRoutes();
        //加载缓存中数据，可以替换成从数据库中加载
        tmp.putAll(RouteCache.routeMap);
        //统一处理规范性问题
        for (Map.Entry<String,ZuulProperties.ZuulRoute> route:tmp.entrySet()){
            String path =route.getKey();
            if (!path.startsWith("/")){
                path = "/" + path;
            }
            if (StringUtils.hasText(zuulProperties.getPrefix())){
                path = zuulProperties.getPrefix() + path;
                if (!path.startsWith("/")){
                    path = "/" + path;
                }
            }
            routes.put(path,route.getValue());
        }

        return routes;
    }
}
