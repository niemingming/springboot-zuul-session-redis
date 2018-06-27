# springboot-zuul-session-redis
springboot+zuul实现session共享
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
