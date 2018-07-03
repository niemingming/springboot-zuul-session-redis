package com.nmm.study.storage;

import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 这里，我们将数据缓存到内存中，后续根据实际情况切换成数据库或者其他形式
 */
public class RouteCache {
    public static Map<String,ZuulProperties.ZuulRoute> routeMap = new LinkedHashMap<String, ZuulProperties.ZuulRoute>();
}
