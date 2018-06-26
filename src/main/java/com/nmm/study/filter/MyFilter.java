package com.nmm.study.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * @author nmm 2018/6/26
 * @description
 */
@Component
public class MyFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return "pre";//route/post/error
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;//需要过滤，且添加的trace
    }

    @Override
    public Object run() {
        //获取当前请求
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()){
            String name = headers.nextElement();
            System.out.println(name + ":"+ request.getHeader(name));
        }
        System.out.println(request.getSession().getId());

        requestContext.setSendZuulResponse(true);
        requestContext.setResponseStatusCode(200);
        return null;
    }
}
