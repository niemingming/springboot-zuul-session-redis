# springboot-zuul-session-redis

* [1、springboot+zuul实现session共享](#session)
* [2、zuul实现动态配置](#dyncconfig)

<h2 id="session">springboot+zuul实现session共享</h2>

# 1、说明
springsession+redis+springboot使用上一个repository就可以了。我们这里关注zuul的配置。
# 2、zuul的默认配置，引入zuul的依赖,同时引入springsession和redis的依赖
```
<dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-zuul</artifactId>
        </dependency>
        <!--增加session共享-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.session</groupId>
            <artifactId>spring-session-data-redis</artifactId>
        </dependency>
```
防止springcloud版本冲突，我们可以指定版本，通过指定管理依赖
```
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>Camden.SR7</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```
# 3、在application中加入相关注解，同时修改application.yml配置文件
```
@SpringBootApplication
@EnableZuulProxy//允许zuul代理，配置session存储后立刻刷新设置刷新模式为立刻刷新，否则可能获取不到session。
@EnableRedisHttpSession(redisFlushMode = RedisFlushMode.IMMEDIATE)
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class,args);
    }
}
zuul:
  routes:
    service0:
    #设置敏感头部信息。 Cookie/Set-Cookie
      sensitiveHeaders: Authorization
      path: /api/**
      url: http://localhost:8080/
    service1:
      path: /api2/**
      url: http://localhost:8090/
      sensitiveHeaders: Authorization
  #增加代理的header，防止session丢失
```
# 4、说明
@EnableZuulProxy注解，用于引入zuul代理，实现zuul的路由转发功能。
@EnableRedisHttpSession，用于指定redis管理session。这里我们指定了redisFlushMode为立即刷新，防止因为redis刷新缓慢，导致session信息无法获取
配置：sensitiveHeaders：（英文不好害死人。）网上查了很多zuul的session共享问题，都说设置sensitiveHeaders属性，还有列出源码的，经过分析和度娘翻译，
sensitive（敏感的），加上源码注释，可以知道，zuul默认情况下的sensitiveHeaders（敏感头），包含"Cookie"/"Set-Cookie"/"Authorization"三个头，分别
是cookie、权限头部分，zuul认为这些头是敏感的，默认不会再路由中携带，如果我们是内部服务路由，就可以覆盖sensitiveHeaders，指定其他值覆盖掉，让其
可以将cookie传到下一路由处，即可。

<h2 id="dyncconfig">zuul实现动态配置路由规则</h2>

# 1、首先实现RefreshableRouteLocator接口
这里我们继承Zuul默认的SimpleRouteLocator，配置的路由规则也放到内存中。代码如下：
```aidl
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
```
# 2、配置zuulConfiguration，将我们实现的RouteLocator配置进去
```aidl
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

```

# 3、实现刷新接口，同时通过事件发送刷新方法
具体的接口监听可以参看ZuulConfiguration的源码，这里我们直接通过ApplicationEventPublisher

```aidl
/**
 * 控制器，用于处理路由规则
 */
@Controller
public class RouteController {
    //spring默认的上下文事件发送器
    @Autowired
    public ApplicationEventPublisher publisher;
    @Autowired //注入我们自定义的路由规则加载器
    public RouteLocator routeLocator;

    /**
     * 加载所有可配置的路由规则
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/routes",method = RequestMethod.GET)
    public String listRoutes(){
        return RouteCache.routeMap.toString();
    }

    /**
     * 添加一条路由规则
     * @param serviceId
     * @param path
     * @param url
     * @param headers
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/routes/{serviceId}",method = RequestMethod.POST)
    public String addRoutes(@PathVariable String serviceId,
                            @RequestParam String path,
                            @RequestParam String url,
                            @RequestParam(required = false) String headers){
        ZuulProperties.ZuulRoute route = new ZuulProperties.ZuulRoute();
        route.setId(serviceId);
        route.setServiceId(serviceId);
        route.setUrl(url);
        route.setPath(path);
        if (headers != null && !"".equals(headers)) {
            Set<String> set = new HashSet<String>();
            set.addAll(Arrays.asList(headers.split(",")));
            route.setSensitiveHeaders(set);
        }
        RouteCache.routeMap.put(path,route);

        refreshRoute();
        return route.toString();
    }

    /**
     * 删除指定的路由
     * @param serviceId
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/routes/{serviceId}",method = RequestMethod.DELETE)
    public String delteRoutes(@PathVariable String serviceId){
        ZuulProperties.ZuulRoute route = null;
        for (ZuulProperties.ZuulRoute zuulRoute:RouteCache.routeMap.values()){
            if (serviceId.equals(zuulRoute.getServiceId())){
                route = zuulRoute;
                RouteCache.routeMap.remove(route.getPath());
                break;
            }
        }
        if (route == null) {
            return "未找到路由规则";
        }
        refreshRoute();
        return route.toString();
    }
    /**
     * 删除指定的路由
     * @param serviceId
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/routes/{serviceId}",method = RequestMethod.PUT)
    public String updateRoutes(@PathVariable String serviceId,
                               @RequestParam String path,
                               @RequestParam String url,
                               @RequestParam String headers){
        ZuulProperties.ZuulRoute route = null;
        for (ZuulProperties.ZuulRoute zuulRoute:RouteCache.routeMap.values()){
            if (serviceId.equals(zuulRoute.getServiceId())){
                route = zuulRoute;
                RouteCache.routeMap.remove(route.getPath());
                break;
            }
        }
        if (route == null) {
            return "未找到路由规则";
        }else {
            route.setUrl(url);
            route.setPath(path);
            if (headers != null && !"".equals(headers)) {
                Set<String> set = new HashSet<String>();
                set.addAll(Arrays.asList(headers.split(",")));
                route.setSensitiveHeaders(set);
            }
        }
        refreshRoute();
        return route.toString();
    }

    /**
     * 发送事件，刷新路由规则
     */
    private void refreshRoute() {
        //创建路由事件
        RoutesRefreshedEvent event = new RoutesRefreshedEvent(routeLocator);
        //发送路由事件
        publisher.publishEvent(event);
    }
}
```
# 4、通过调用http://localhost:8000/routes/接口，添加配置信息我们就可以看到相关配置

# 5、说明
这里我们与redis做了集成，实现session共享，如果不想使用，可以跳过1中的设置。