

# Spring Cloud Eureka：服务注册与发现

## 摘要

> Spring Cloud Eureka是Spring Cloud Netflix 子项目的核心组件之一，主要用于微服务架构中的服务治理。 

## Eureka简介

在微服务架构中往往会有一个注册中心，每个微服务都会向注册中心去注册自己的地址及端口信息，注册中心维护着服务名称与服务实例的对应关系。每个微服务都会定时从注册中心获取服务列表，同时汇报自己的运行情况，这样当有的服务需要调用其他服务时，就可以从自己获取到的服务列表中获取实例地址进行调用，Eureka实现了这套服务注册与发现机制。

## 搭建Eureka注册中心

- 创建一个eureka-server模块，并使用Spring Initializer初始化一个SpringBoot项目

- eureka-server的依赖

  ```xml
  	<properties>
          <java.version>1.8</java.version>
          <spring-cloud.version>Hoxton.SR6</spring-cloud.version>
      </properties>
  	<dependencyManagement>
          <dependencies>
              <dependency>
                  <groupId>org.springframework.cloud</groupId>
                  <artifactId>spring-cloud-dependencies</artifactId>
                  <version>${spring-cloud.version}</version>
                  <type>pom</type>
                  <scope>import</scope>
              </dependency>
          </dependencies>
      </dependencyManagement>
  
  	<dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
  	</dependency>
  ```

- 在启动类上添加@EnableEurekaServer注解来启用Euerka注册中心功能

  ```java
  @EnableEurekaServer
  @SpringBootApplication
  public class EurekaServerApplication {
  
      public static void main(String[] args) {
          SpringApplication.run(EurekaServerApplication.class, args);
      }
  
  }
  ```

- 在配置文件application.yml中添加Eureka注册中心的配置

  ```yaml
  server:
    port: 8001 #指定运行端口
  spring:
    application:
      name: eureka-server #指定服务名称
  eureka:
    instance:
      hostname: localhost #指定主机地址
    client:
      fetch-registry: false #指定是否要从注册中心获取服务（注册中心不需要开启）
      register-with-eureka: false #指定是否要注册到注册中心（注册中心不需要开启）
    server:
      enable-self-preservation: false #关闭保护模式
  ```

- 运行完成后访问地址http://localhost:8001/可以看到Eureka注册中心的界面

  ![](Spring Cloud.assets/eureka-server.png)

## 搭建Eureka客户端

- 新建一个eureka-client模块，并在pom.xml中添加如下依赖

  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
  
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  ```

- 在启动类上添加@EnableDiscoveryClient注解表明是一个Eureka客户端

  ```java
  @EnableDiscoveryClient
  @SpringBootApplication
  public class EurekaClientApplication {
  
      public static void main(String[] args) {
          SpringApplication.run(EurekaClientApplication.class, args);
      }
  }
  ```

- 在配置文件application.yml中添加Eureka客户端的配置

  ```yaml
  server:
    port: 9001 #运行端口号
  spring:
    application:
      name: eureka-client #服务名称
  eureka:
    client:
      register-with-eureka: true #注册到Eureka的注册中心
      fetch-registry: true #获取注册实例列表
      service-url:
        defaultZone: http://localhost:8001/eureka/ #配置注册中心地址
  ```

- 运行eureka-client

  ![](Spring Cloud.assets/eureka-client.png)

## 搭建Eureka注册中心集群

### 搭建两个注册中心

>由于所有服务都会注册到注册中心去，服务之间的调用都是通过从注册中心获取的服务列表来调用，注册中心一旦宕机，所有服务调用都会出现问题。所以我们需要多个注册中心组成集群来提供服务，下面搭建一个双节点的注册中心集群。

​	

- 给eureka-sever添加配置文件application-replica1.yml配置第一个注册中心

  ```yaml
  server:
    port: 8002
  spring:
    application:
      name: eureka-server
  eureka:
    instance:
      hostname: replica1
    client:
      serviceUrl:
        defaultZone: http://replica2:8003/eureka/ #注册到另一个Eureka注册中心
      fetch-registry: true
      register-with-eureka: true
  ```

- 给eureka-sever添加配置文件application-replica2.yml配置第二个注册中心

  ```yaml
  ![image-20200727140627149](H:/OldHardDrive/ly/java/springcloud/Spring Cloud.assets/image-20200727140627149.png)server:
    port: 8003
  spring:
    application:
      name: enreka-server
  eureka:
    instance:
      hostname: replica2
    client:
      service-url:
        defaultZone: http://replica1:8002/eureka/
      fetch-registry: true
      register-with-eureka: true
  ```

  **这里我们通过两个注册中心互相注册，搭建了注册中心的双节点集群，由于defaultZone使用了域名，所以还需在本机的host文件中配置一下。**

- 修改本地host文件

  ```txt
  127.0.0.1 replica1
  127.0.0.1 replica2
  ```

- ![](Spring Cloud.assets/replica1.png)

  ![](Spring Cloud.assets/replica2.png)

- 修改Eureka-client，让其连接到集群

  添加eureka-client的配置文件application-replica.yml，让其同时注册到两个注册中心。

  ```yaml
  server:
    port: 9002
  spring:
    application:
      name: eureka-client
  eureka:
    client:
      register-with-eureka: true
      fetch-registry: true
      service-url:
        defaultZone: http://replica1:8002/eureka/, http://replica2:8003/eureka/
  ```

  以该配置文件启动后访问任意一个注册中心节点都可以看到eureka-client

## 给Eureka注册中心添加认证

- 创建一个eureka-security-server模块，在pom.xml中添加以下依赖

  ```xml
              <dependency>
                  <groupId>org.springframework.cloud</groupId>
                  <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
              </dependency>
              <!-- 添加security模块 -->
              <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-security</artifactId>
              </dependency>
  ```

- 添加application.yml配置文件

  ```yaml
  #需要设置登录名和密码
  server:
    port: 8004
  spring:
    application:
      name: eureka-security-server
    security:
      user:
        name: ly
        password: 5211
  eureka:
    instance:
      hostname: localhost
    client:
      fetch-registry: false
      register-with-eureka: false
  ```

- 添加Java配置WebSecurityConfig

  >默认情况下添加SpringSecurity依赖的应用每个请求都需要添加CSRF token才能访问，Eureka客户端注册时并不会添加，所以需要配置/eureka/**路径不需要CSRF token。

  ```java
  @EnableWebSecurity
  public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
  
      @Override
      protected void configure(HttpSecurity http) throws Exception {
          http.csrf().ignoringAntMatchers("/eureka/**");
          super.configure(http);
      }
  }
  ```

-  运行eureka-security-server，访问[http://localhost:8004](http://localhost:8004/)发现需要登录认证

-  eureka-client注册到有登录认证的注册中心

  - 配置文件中需要修改注册中心地址格式

    ```
    http://${username}:${password}@${hostname}:${port}/eureka/
    ```

  - 添加application-security.yml配置文件，按格式修改用户名和密码

    ```yaml
    #配置文件中需要修改注册中心地址格式
    # http://${username}:${password}@${hostname}:${port}/eureka/
    server:
      port: 9003
    spring:
      application:
        name: eureka-client
    eureka:
      client:
        register-with-eureka: true
        fetch-registry: true
        service-url:
          defaultZone: http://ly:5211@localhost:8004/eureka/
    ```

- 以application-security.yml配置运行eureka-client，可以在注册中心界面看到eureka-client已经成功注册

![image-20200727141434842](Spring Cloud.assets/security-client.png)



## Eureka的常用配置

```yaml
eureka:
  client: #eureka客户端配置
    register-with-eureka: true #是否将自己注册到eureka服务端上去
    fetch-registry: true #是否获取eureka服务端上注册的服务列表
    service-url:
      defaultZone: http://localhost:8001/eureka/ # 指定注册中心地址
    enabled: true # 启用eureka客户端
    registry-fetch-interval-seconds: 30 #定义去eureka服务端获取服务列表的时间间隔
  instance: #eureka客户端实例配置
    lease-renewal-interval-in-seconds: 30 #定义服务多久去注册中心续约
    lease-expiration-duration-in-seconds: 90 #定义服务多久不去续约认为服务失效
    metadata-map:
      zone: chongqing #所在区域
    hostname: localhost #服务主机名称
    prefer-ip-address: false #是否优先使用ip来作为主机名
  server: #eureka服务端配置
    enable-self-preservation: false #关闭eureka服务端的保护机制
```

---



# Spring Cloud Ribbon：负载均衡的服务调用

## 摘要

> Spring Cloud Ribbon 是Spring Cloud Netflix 子项目的核心组件之一，主要给服务间调用及API网关转发提供负载均衡的功能

## Ribbon简介

> 在微服务架构中，很多服务都会部署多个，其他服务去调用该服务的时候，如何保证负载均衡是个不得不去考虑的问题。负载均衡可以增加系统的可用性和扩展性，当我们使用RestTemplate来调用其他服务时，Ribbon可以很方便的实现负载均衡功能。

## RestTemplate的使用

>RestTemplate是一个HTTP客户端，使用它我们可以方便的调用HTTP接口，支持GET、POST、PUT、DELETE等方法。

### GET请求方法

```java
<T> T getForObject(String url, Class<T> responseType, Object... uriVariables);

<T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables);

<T> T getForObject(URI url, Class<T> responseType);

<T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables);

<T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables);

<T> ResponseEntity<T> getForEntity(URI var1, Class<T> responseType);
```

#### getForObject方法

返回对象为响应体中数据转化成的对象

```java
@GetMapping("/{id}")
public CommonResult getUser(@PathVariable Long id) {
    return restTemplate.getForObject(userServiceUrl + "/user/{1}", CommonResult.class, id);
}
```

#### getForEntity方法

返回对象为ResponseEntity对象，包含了响应中的一些重要信息，比如响应头、响应状态码、响应体等

```java
@GetMapping("/getEntityByUsername")
public CommonResult getEntityByUsername(@RequestParam String username) {
    ResponseEntity<CommonResult> entity = restTemplate.getForEntity(userServiceUrl + "/user/getByUsername?username={1}", CommonResult.class, username);
    if (entity.getStatusCode().is2xxSuccessful()) {
        return entity.getBody();
    } else {
        return new CommonResult("操作失败", 500);
    }
}
```

### POST请求方法

```java
<T> T postForObject(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables);

<T> T postForObject(String url, @Nullable Object request, Class<T> responseType, Map<String, ?> uriVariables);

<T> T postForObject(URI url, @Nullable Object request, Class<T> responseType);

<T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables);

<T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType, Map<String, ?> uriVariables);

<T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType);
```

#### postForObject示例

```java
@PostMapping("/create")
public CommonResult create(@RequestBody User user) {
    return restTemplate.postForObject(userServiceUrl + "/user/create", user, CommonResult.class);
}
```

#### postForEntity示例

```java
@PostMapping("/create")
public CommonResult create(@RequestBody User user) {
    return restTemplate.postForEntity(userServiceUrl + "/user/create", user, CommonResult.class).getBody();
}
```

### PUT请求方法

```java
void put(String url, @Nullable Object request, Object... uriVariables);

void put(String url, @Nullable Object request, Map<String, ?> uriVariables);

void put(URI url, @Nullable Object request);
```

#### PUT请求示例

```java
@PutMapping("/update")
public CommonResult update(@RequestBody User user) {
    restTemplate.put(userServiceUrl + "/user/update", user);
    return new CommonResult("操作成功",200);
}
```

### DELETE请求方法

```java
void delete(String url, Object... uriVariables);

void delete(String url, Map<String, ?> uriVariables);

void delete(URI url);
```

#### DELETE请求示例

```java
@DeleteMapping("/delete/{id}")
public CommonResult delete(@PathVariable Long id) {
   restTemplate.delete(userServiceUrl + "/user/delete/{1}", null, id);
   return new CommonResult("操作成功",200);
}
```

## 创建user-service模块

> 创建user-sercvice模块用于给Ribbon提供服务调用

- 在pom.xml中添加相关依赖

  ```xml
          <dependency>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <optional>true</optional>
          </dependency>
          <!--集成swagger以及UI-->
          <dependency>
              <groupId>io.springfox</groupId>
              <artifactId>springfox-swagger2</artifactId>
              <version>2.6.1</version>
          </dependency>
  
          <dependency>
              <groupId>io.springfox</groupId>
              <artifactId>springfox-swagger-ui</artifactId>
              <version>2.6.1</version>
          </dependency>
  
  
  
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
          </dependency>
  ```

- 在application.yml中配置端口和注册中心地址

  ```yaml
  server:
    port: 9004
  spring:
    application:
      name: user-service
  eureka:
    client:
      register-with-eureka: true
      fetch-registry: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  ```

- 定义User对象

  ```java
  package com.yanl.cloud.domain;
  
  import lombok.AllArgsConstructor;
  import lombok.Data;
  
  @Data
  @AllArgsConstructor
  public class User {
      private Long id;
      private String username;
      private String password;
  }
  ```

- 定义CommonResult

  对返回结果进行的一个封装

  ```java
  package com.yanl.cloud.domain;
  
  import lombok.Getter;
  import lombok.Setter;
  
  @Getter
  @Setter
  public class CommonResult<T> {
      private T data;
      private String msg;
      private Integer code;
  
      public CommonResult(){}
  
      public CommonResult(T data, String msg, Integer code){
          this.code = code;
          this.data = data;
          this.msg = msg;
      }
  
      public CommonResult(String msg, Integer code){
          this(null, msg, code);
      }
  
      public CommonResult(T data){
          this(data, "操作成功", 200);
      }
  }
  ```

- 定义userService接口

  ```java
  package com.yanl.cloud.service;
  
  import com.yanl.cloud.domain.User;
  
  import java.util.List;
  
  public interface UserService {
      void create(User user);
  
      User getUser(Long id);
  
      void update(User user);
  
      void delete(Long id);
  
      User getByUserName(String username);
  
      List<User> getUserByIds(List<Long> ids);
  }
  ```

- 实现userService接口

  ```java
  package com.yanl.cloud.service.impl;
  
  import com.yanl.cloud.domain.User;
  import com.yanl.cloud.service.UserService;
  import org.springframework.stereotype.Service;
  import org.springframework.util.CollectionUtils;
  
  import javax.annotation.PostConstruct;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.stream.Collectors;
  
  @Service
  public class UserServiceImpl implements UserService {
  
      private List<User> userList;
  
      @Override
      public void create(User user) {
          userList.add(user);
      }
  
      @Override
      public User getUser(Long id) {
          List<User> findUserList = userList.stream().filter(user -> user.getId().equals(id)).collect(Collectors.toList());
          if(!CollectionUtils.isEmpty(findUserList)){
              return findUserList.get(0);
          }
          return null;
      }
  
      @Override
      public void update(User user) {
          userList.stream().filter(userItem -> userItem.getId().equals(user.getId())).forEach(userItem -> {
              userItem.setUsername(user.getUsername());
              userItem.setPassword(user.getPassword());
          });
      }
  
      @Override
      public void delete(Long id) {
          User user = getUser(id);
          if(user != null){
              userList.remove(user);
          }
      }
  
      @Override
      public User getByUserName(String username) {
          List<User> findUserList = userList.stream().filter(userItem -> userItem.getUsername().equals(username)).collect(Collectors.toList());
          if(!CollectionUtils.isEmpty(findUserList)){
              return findUserList.get(0);
          }
          return null;
      }
  
      @Override
      public List<User> getUserByIds(List<Long> ids) {
          return userList.stream().filter(userItem -> ids.contains(userItem.getId())).collect(Collectors.toList());
      }
  
      @PostConstruct
      public void initData(){
          userList = new ArrayList<>();
          userList.add(new User(1L, "ly", "5211"));
          userList.add(new User(2L, "zy", "5211"));
      }
  }
  ```

- SwaggerConfig

  整合swagger接口文档

  ```java
  package com.yanl.cloud.config;
  
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import springfox.documentation.builders.ApiInfoBuilder;
  import springfox.documentation.builders.PathSelectors;
  import springfox.documentation.builders.RequestHandlerSelectors;
  import springfox.documentation.service.ApiInfo;
  import springfox.documentation.spi.DocumentationType;
  import springfox.documentation.spring.web.plugins.Docket;
  
  @Configuration
  public class SwaggerConfig {
  
      @Bean
      public Docket createRestApi(){
          return new Docket(DocumentationType.SWAGGER_2)
                  .apiInfo(apiInfo())
                  .select()
                  .apis(RequestHandlerSelectors.basePackage("com.yanl.cloud.controller"))
                  .paths(PathSelectors.any())
                  .build();
      }
  
      public ApiInfo apiInfo(){
          return new ApiInfoBuilder()
                  .title("userService")
                  .description("用户CRUD")
                  .version("1.0")
                  .build();
      }
  }
  ```

## 创建ribbon-service模块

> 创建ribbon-service来调用user-service演示负载均衡的服务调用

- 在pom.xml中添加相关依赖

  ```xml
  		<!-- 记得添加cloud版本控制	-->	
  		<dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
          </dependency>
  
          <dependency>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <optional>true</optional>
          </dependency>
          <!--集成swagger以及UI-->
          <dependency>
              <groupId>io.springfox</groupId>
              <artifactId>springfox-swagger2</artifactId>
              <version>2.6.1</version>
          </dependency>
  
          <dependency>
              <groupId>io.springfox</groupId>
              <artifactId>springfox-swagger-ui</artifactId>
              <version>2.6.1</version>
          </dependency>
  ```

- 在application.yml进行配置

  ```yaml
  server:
    port: 8005
  spring:
    application:
      name: ribbon-service
  eureka:
    client:
      fetch-registry: true
      register-with-eureka: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  # 指定要调用的服务名
  service-url:
    user-service: http://user-service
  ```

- 定义User和CommonResult类，同userService模块

  

- 创建RestTemplate配置类

  使用@LoadBalanced注解赋予RestTemplate负载均衡的能力

  > 可以看出使用Ribbon的负载均衡功能非常简单，和直接使用RestTemplate没什么两样，只需给RestTemplate添加一个@LoadBalanced即可。

  ```java
  package com.yanl.cloud.config;
  
  import org.springframework.cloud.client.loadbalancer.LoadBalanced;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.web.client.RestTemplate;
  
  @Configuration
  public class RibbonConfig {
  
      @Bean
      @LoadBalanced
      public RestTemplate restTemplate(){
          return new RestTemplate();
      }
  }
  ```

- 创建UserRibbonController类

  > 注入RestTemplate，使用其调用user-service中提供的相关接口，这里对GET和POST调用进行了演示，其他方法调用均可参考。

  ```java
  package com.yanl.cloud.controller;
  
  import com.yanl.cloud.domain.CommonResult;
  import com.yanl.cloud.domain.User;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.http.ResponseEntity;
  import org.springframework.web.bind.annotation.*;
  import org.springframework.web.client.RestTemplate;
  @Slf4j
  @RestController
  @RequestMapping("/user")
  public class UserRibbonController {
      @Autowired
      private RestTemplate restTemplate;
  
      @Value("${service-url.user-service}")
      private String userServiceUrl;
  
      @GetMapping("/{id}")
      public CommonResult getUser(@PathVariable Long id){
          return restTemplate.getForObject(userServiceUrl + "/user/{1}", CommonResult.class, id);
      }
  
      @GetMapping("/getByUserName")
      public CommonResult getByUserName(@RequestParam String username){
          return restTemplate.getForObject(userServiceUrl +"/user/getUserByName?username={1}", CommonResult.class, username);
      }
  
      @GetMapping("/getEntityByUserName")
      public CommonResult getEntityByUserName(@RequestParam String username){
          ResponseEntity<CommonResult> entity = restTemplate.getForEntity(userServiceUrl + "/user/getUserByName?username={1}", CommonResult.class, username);
          if(entity.getStatusCode().is2xxSuccessful()){
              return entity.getBody();
          }else {
              return new CommonResult("操作失败", 500);
          }
      }
  
      @PostMapping("/create")
      public CommonResult create(@RequestBody User user){
          return restTemplate.postForObject(userServiceUrl + "/user/create", user, CommonResult.class);
      }
  
      @PostMapping("/update")
      public CommonResult update(@RequestBody User user){
          return restTemplate.postForObject(userServiceUrl + "/user/update", user, CommonResult.class);
      }
  
      @PostMapping("/delete/{id}")
      public CommonResult delete(@PathVariable Long id){
          return restTemplate.postForObject(userServiceUrl + "/user/delete/{1}", null, CommonResult.class, id);
      }
  }
  ```

## 负载功能演示

- 启动eureka-server模块，ribbon-service模块

- 启动两个user-service模块

  ![image-20200727173232413](Spring Cloud.assets/multi-service.png)

- 此时运行的服务

  ![image-20200727173322084](Spring Cloud.assets/servicerunning.png)

- swagger接口测试

  ![image-20200727173357800](Spring Cloud.assets/swagger测试.png)

  ![image-20200727173540455](Spring Cloud.assets/service1.png)

  ![image-20200727173606447](Spring Cloud.assets/service2.png)

- 可以看到两个服务均被调用，交替打印日志

## Ribbon的常用配置

### 全局配置

```yaml
ribbon:
  ConnectTimeout: 1000 #服务请求连接超时时间（毫秒）
  ReadTimeout: 3000 #服务请求处理超时时间（毫秒）
  OkToRetryOnAllOperations: true #对超时请求启用重试机制
  MaxAutoRetriesNextServer: 1 #切换重试实例的最大个数
  MaxAutoRetries: 1 # 切换实例后重试最大次数
  NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule #修改负载均衡算法
```

### 指定服务进行配置

> 对单独的服务进行配置

```yaml
user-service:
  ribbon:
    ConnectTimeout: 1000 #服务请求连接超时时间（毫秒）
    ReadTimeout: 3000 #服务请求处理超时时间（毫秒）
    OkToRetryOnAllOperations: true #对超时请求启用重试机制
    MaxAutoRetriesNextServer: 1 #切换重试实例的最大个数
    MaxAutoRetries: 1 # 切换实例后重试最大次数
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule #修改负载均衡算法
```

## Ribbon的负载均衡策略

> 所谓的负载均衡策略，就是当A服务调用B服务时，此时B服务有多个实例，这时A服务以何种方式来选择调用的B实例，ribbon可以选择以下几种负载均衡策略。

1. com.netflix.loadbalancer.RandomRule：从提供服务的实例中以随机的方式；

2. com.netflix.loadbalancer.RoundRobinRule：以线性轮询的方式，就是维护一个计数器，从提供服务的实例中按顺序选取，第一次选第一个，第二次选第二个，以此类推，到最后一个以后再从头来过；

3. com.netflix.loadbalancer.RetryRule：在RoundRobinRule的基础上添加重试机制，即在指定的重试时间内，反复使用线性轮询策略来选择可用实例；

4. com.netflix.loadbalancer.WeightedResponseTimeRule：对RoundRobinRule的扩展，响应速度越快的实例选择权重越大，越容易被选择；

5. com.netflix.loadbalancer.BestAvailableRule：选择并发较小的实例；

6. com.netflix.loadbalancer.AvailabilityFilteringRule：先过滤掉故障实例，再选择并发较小的实例；

7. com.netflix.loadbalancer.ZoneAwareLoadBalancer：采用双重过滤，同时过滤不是同一区域的实例和故障实例，选择并发较小的实例。

# Spring Cloud Hystrix：服务容错保护

## Hystrix 简介

> Spring Cloud Hystrix 是Spring Cloud Netflix 子项目的核心组件之一，具有服务容错及线程隔离等一系列服务保护功能

在微服务架构中，服务与服务之间通过远程调用的方式进行通信，一旦某个被调用的服务发生了故障，其依赖服务也会发生故障，此时就会发生故障的蔓延，最终导致系统瘫痪。Hystrix实现了断路器模式，当某个服务发生故障时，通过断路器的监控，给调用方返回一个错误响应，而不是长时间的等待，这样就不会使得调用方由于长时间得不到响应而占用线程，从而防止故障的蔓延。Hystrix具备服务降级、服务熔断、线程隔离、请求缓存、请求合并及服务监控等强大功能。

## 创建hystrix-service模块

> 创建一个hystrix-service模块来演示常用功能

-  在pom.xml中添加相关依赖

  ```xml
  <properties>
          <java.version>1.8</java.version>
          <spring-cloud.version>Hoxton.SR6</spring-cloud.version>
      </properties>
  
      <dependencyManagement>
          <dependencies>
              <dependency>
                  <groupId>org.springframework.cloud</groupId>
                  <artifactId>spring-cloud-dependencies</artifactId>
                  <version>${spring-cloud.version}</version>
                  <type>pom</type>
                  <scope>import</scope>
              </dependency>
          </dependencies>
      </dependencyManagement>
  
      <dependencies>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter</artifactId>
          </dependency>
  
          <dependency>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <optional>true</optional>
          </dependency>
          <!--集成swagger以及UI-->
          <dependency>
              <groupId>io.springfox</groupId>
              <artifactId>springfox-swagger2</artifactId>
              <version>2.6.1</version>
          </dependency>
  
          <dependency>
              <groupId>io.springfox</groupId>
              <artifactId>springfox-swagger-ui</artifactId>
              <version>2.6.1</version>
          </dependency>
  
          <dependency>
              <groupId>cn.hutool</groupId>
              <artifactId>hutool-all</artifactId>
              <version>4.6.3</version>
          </dependency>
  ```

- 在application.yml进行配置

  ```yaml
  server:
    port: 8006
  spring:
    application:
      name: hystrix-service
  eureka:
    client:
      register-with-eureka: true
      fetch-registry: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  
  # user-service的调用路径
  service-url:
    user-service: http://user-service
  ```

  

- 定义User和CommonResult类，同userService模块

- 创建RestTemplate配置类，同上

- 在启动类上添加@EnableCircuitBreaker来开启Hystrix的断路器功能

  ```java
  @EnableDiscoveryClient
  @EnableCircuitBreaker
  @EnableSwagger2
  @SpringBootApplication
  public class HystrixServiceApplication {
  
      public static void main(String[] args) {
          SpringApplication.run(HystrixServiceApplication.class, args);
      }
  
  }
  ```

- 创建UserHystrixController接口用于调用user-service服务

## 服务降级演示

- 在UserService中添加调用方法与服务降级方法，方法上需要添加@HystrixCommand注解：

  ```java
  package com.yanl.cloud.service;
  
  import cn.hutool.core.bean.BeanUtil;
  import cn.hutool.core.collection.CollUtil;
  import com.netflix.hystrix.contrib.javanica.annotation.HystrixCollapser;
  import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
  import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
  import com.netflix.hystrix.contrib.javanica.cache.annotation.CacheRemove;
  import com.netflix.hystrix.contrib.javanica.cache.annotation.CacheResult;
  import com.netflix.hystrix.contrib.javanica.command.AsyncResult;
  import com.yanl.cloud.domain.CommonResult;
  import com.yanl.cloud.domain.User;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.stereotype.Service;
  import org.springframework.web.bind.annotation.PathVariable;
  import org.springframework.web.client.RestTemplate;
  
  import java.util.List;
  import java.util.Map;
  import java.util.concurrent.Future;
  
  @Service
  @Slf4j
  public class UserService {
  
      @Autowired
      private RestTemplate restTemplate;
  
      @Value("${service-url.user-service}")
      private String userServiceUrl;
  
  
      /**
       * 服务降级注解，服务关闭调用getDefaultUser方法
       * @param id
       * @return
       */
      @HystrixCommand(fallbackMethod = "getDefaultUser")
      public CommonResult getUser(Long id){
          return restTemplate.getForObject(userServiceUrl + "/user/{1}", CommonResult.class, id);
      }
  
      /**
       * 服务不可用时调用
       * @param id
       * @return
       */
      public CommonResult<User> getDefaultUser(@PathVariable Long id){
          User defaultUser = new User(-1L, "default", "123456");
          return new CommonResult<>(defaultUser);
      }
  }
  ```

  

- 在UserHystrixController中添加用于测试服务降级的接口：

  ```java
  package com.yanl.cloud.controller;
  
  import cn.hutool.core.thread.ThreadUtil;
  import com.yanl.cloud.domain.CommonResult;
  import com.yanl.cloud.domain.User;
  import com.yanl.cloud.service.UserService;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.web.bind.annotation.GetMapping;
  import org.springframework.web.bind.annotation.PathVariable;
  import org.springframework.web.bind.annotation.RequestMapping;
  import org.springframework.web.bind.annotation.RestController;
  
  import java.util.concurrent.ExecutionException;
  import java.util.concurrent.Future;
  
  @RestController
  @RequestMapping("/user")
  public class UserHystrixController {
  
      @Autowired
      private UserService userService;
  
      @GetMapping("/testFallBack/{id}")
      public CommonResult testFallBack(@PathVariable Long id){
          return userService.getUser(id);
      }
  }
  ```

- 关闭user-service服务重新测试该接口，发现已经发生了服务降级：

  ![image-20200728151101999](Spring Cloud.assets/降级.png)

## @HystrixCommand详解

### @HystrixCommand中的常用参数

- fallbackMethod：指定服务降级处理方法；
- ignoreExceptions：忽略某些异常，不发生服务降级；
- commandKey：命令名称，用于区分不同的命令；
- groupKey：分组名称，Hystrix会根据不同的分组来统计命令的告警及仪表盘信息；
- threadPoolKey：线程池名称，用于划分线程池。

### 设置命令、分组及线程池名称

- 在UserService中添加方式实现功能：

  ```java
  /**
       * fallbackMethod：指定服务降级处理方法
       * ignoreExceptions：忽略某些异常，不发生服务降级
       * commandKey：命令名称，用于区分不同的命令
       * groupKey：分组名称，Hystrix会根据不同的分组来统计命令的告警及仪表盘信息
       * threadPoolKey：线程池名称，用于划分线程池
       * @param id
       * @return
       */
      @HystrixCommand(fallbackMethod = "getDefaultUser",
          commandKey = "getUserCommand",
          groupKey = "getUserGroup",
          threadPoolKey = "getUserThreadPool")
      public CommonResult getUserCommand(@PathVariable Long id){
          return restTemplate.getForObject(userServiceUrl + "/user/{1}", CommonResult.class, id);
      }
  ```

- 在UserHystrixController中添加测试接口：

  ```java
  @GetMapping("/testCommand/{id}")
      public CommonResult testCommand(@PathVariable Long id){
          return userService.getUserCommand(id);
      }
  ```

### 使用ignoreExceptions忽略某些异常降级

- 在UserService中添加实现方法，这里忽略了NullPointerException，当id为1时抛出IndexOutOfBoundsException，id为2时抛出NullPointerException：

  ```java
  /**
       * 当空指针异常时不发生服务降级
       * @param id
       * @return
       */
      @HystrixCommand(fallbackMethod = "getDefaultUser2", ignoreExceptions = {NullPointerException.class})
      public CommonResult getUserException(Long id){
          if(id == 1){
              throw new IndexOutOfBoundsException();
          }else if(id == 2){
              throw new NullPointerException();
          }
          return restTemplate.getForObject(userServiceUrl + "/user/{1}", CommonResult.class, id);
      }
  	public CommonResult<User> getDefaultUser2(@PathVariable Long id, Throwable e){
          log.error("getDefaultUser2 id:{}, throwable class: {}", id, e.getClass());
          User user = new User(-2L, "default2", "123456");
          return new CommonResult<>(user);
      }
  
  ```

  

- 在UserHystrixController中添加测试接口：

  ```java
  @GetMapping("/testException/{id}")
      public CommonResult testException(@PathVariable Long id){
          return userService.getUserException(id);
      }
  ```

- 调用接口进行测试

  ![image-20200728151835180](Spring Cloud.assets/id=1.png)

  ![image-20200728151912305](Spring Cloud.assets/id=2.png)

## Hystrix的请求缓存

> 当系统并发量越来越大时，我们需要使用缓存来优化系统，达到减轻并发请求线程数，提供响应速度的效果。

### 相关注解

- @CacheResult：开启缓存，默认所有参数作为缓存的key，cacheKeyMethod可以通过返回String类型的方法指定key；
- @CacheKey：指定缓存的key，可以指定参数或指定参数中的属性值为缓存key，cacheKeyMethod还可以通过返回String类型的方法指定；
- @CacheRemove：移除缓存，需要指定commandKey。

### 缓存使用过程中的问题

- 在缓存使用过程中，我们需要在每次使用缓存的请求前后对HystrixRequestContext进行初始化和关闭，否则会出现如下异常：

  ```java
  java.lang.IllegalStateException: Request caching is not available. Maybe you need to initialize the HystrixRequestContext?
  	at com.netflix.hystrix.HystrixRequestCache.get(HystrixRequestCache.java:104) ~[hystrix-core-1.5.18.jar:1.5.18]
  	at com.netflix.hystrix.AbstractCommand$7.call(AbstractCommand.java:478) ~[hystrix-core-1.5.18.jar:1.5.18]
  	at com.netflix.hystrix.AbstractCommand$7.call(AbstractCommand.java:454) ~[hystrix-core-1.5.18.jar:1.5.18]
  ```

- 这里我们通过使用过滤器，在每个请求前后初始化和关闭HystrixRequestContext来解决该问题：

  ```java
  package com.yanl.cloud.filter;
  
  import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
  import org.springframework.stereotype.Component;
  
  import javax.servlet.*;
  import javax.servlet.annotation.WebFilter;
  import java.io.IOException;
  
  /**
   * 	这里我们通过使用过滤器，在每个请求前后初始化和关闭HystrixRequestContext来解决该问题：
   *
   */
  @Component
  @WebFilter
  public class HystrixRequestContextFilter implements Filter {
      @Override
      public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
          HystrixRequestContext context = HystrixRequestContext.initializeContext();
          try{
              filterChain.doFilter(servletRequest, servletResponse);
          }finally {
              context.close();
          }
      }
  }
  ```

  

### 测试使用缓存

- 在UserService中添加具有缓存功能的getUserCache方法：

  ```java
  @CacheResult(cacheKeyMethod = "getCacheKey")
      @HystrixCommand(fallbackMethod = "getDefaultUser", commandKey = "getUserCache")
      public CommonResult getUserCache(Long id){
          log.info("getUserCache id:{}", id);
          return restTemplate.getForObject(userServiceUrl + "/user/{1}", CommonResult.class, id);
      }
  
      /**
       * 为缓存生成key
       * @param id
       * @return
       */
      public String getCacheKey(Long id){
          return String.valueOf(id);
      }
  ```

- 在UserHystrixController中添加使用缓存的测试接口，直接调用三次getUserCache方法：

  ```java
  @GetMapping("/testCache/{id}")
      public CommonResult testCache(@PathVariable Long id){
          userService.getUserCache(id);
          userService.getUserCache(id);
          userService.getUserCache(id);
          return new CommonResult("操作成功", 200);
      }
  ```

- 调用接口测试

  调用了三次接口，只打印了一次日志，说明有两次走的缓存。

  ![image-20200728152309737](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\cache)

### 测试移除缓存

- 在UserService中添加具有移除缓存功能的removeCache方法：

  ```java
  @CacheRemove(commandKey = "getUserCache", cacheKeyMethod = "getCacheKey")
      @HystrixCommand
      public CommonResult removeCache(Long id){
          log.info("removeCache id:{}", id);
          return restTemplate.postForObject(userServiceUrl + "/user/delete/{1}",null, CommonResult.class, id);
      }
  ```

- 在UserHystrixController中添加移除缓存的测试接口，调用一次removeCache方法：

  ```java
  @GetMapping("/testRemoveCache/{id}")
      public CommonResult testRemoveCache(@PathVariable Long id){
          userService.getUserCache(id);
          userService.removeCache(id);
          userService.getUserCache(id);
          userService.getUserCache(id);
          return new CommonResult("操作成功", 200);
      }
  ```

- 调用接口测试，发现两次查询都走的接口

  ![image-20200728152623281](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\removecahce)

## 请求合并

> 微服务系统中的服务间通信，需要通过远程调用来实现，随着调用次数越来越多，占用线程资源也会越来越多。Hystrix中提供了@HystrixCollapser用于合并请求，从而达到减少通信消耗及线程数量的效果。

### @HystrixCollapser的常用属性

- batchMethod：用于设置请求合并的方法；
- collapserProperties：请求合并属性，用于控制实例属性，有很多；
- timerDelayInMilliseconds：collapserProperties中的属性，用于控制每隔多少时间合并一次请求；

### 用法

- 在userService中添加单任务接口以及批处理接口，同时加上注解。

  ```java
  @HystrixCollapser(batchMethod = "getUserByIds", collapserProperties = {
          @HystrixProperty(name = "timerDelayInMilliseconds", value = "100")
      })
      public Future<User> getFutureUser(Long id){
          return new AsyncResult<User>(){
              public User invoke(){
                  CommonResult commonResult = restTemplate.getForObject(userServiceUrl + "/user/{1}", CommonResult.class, id);
                  Map data = (Map) commonResult.getData();
                  User user = BeanUtil.mapToBean(data, User.class, true);
                  log.info("getUserById username:{}", user.getUsername());
                  return user;
              }
          };
      }
  
      @HystrixCommand
      public List<User> getUserByIds(List<Long> ids){
          log.info("getUserByIds:{}", ids);
          List user = restTemplate.getForObject(userServiceUrl + "/user/getUserByIds?ids={1}", List.class, CollUtil.join(ids, ","));
          log.info(user.toString());
          return user;
      }
  ```

- 在UserHystrixController中添加testCollapser方法，先进行两次服务调用，再间隔200ms以后进行第三次服务调用：

  ```java
  @GetMapping("/testCollapse")
      public CommonResult testCollapse() throws ExecutionException, InterruptedException {
          Future<User> future1 = userService.getFutureUser(1L);
          Future<User> future2 = userService.getFutureUser(2L);
          future1.get();
          future2.get();
          ThreadUtil.safeSleep(200);
          Future<User> future3 = userService.getFutureUser(3L);
          future3.get();
          return new CommonResult("操作成功", 200);
      }
  ```

- 接口测试

  由于我们设置了100毫秒进行一次请求合并，所以前两次被合并，最后一次执行

  ![image-20200728153354946](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\collapser)

## Hystrix的常用配置

### 全局配置

```yaml
hystrix:
  command: #用于控制HystrixCommand的行为
    default:
      execution:
        isolation:
          strategy: THREAD #控制HystrixCommand的隔离策略，THREAD->线程池隔离策略(默认)，SEMAPHORE->信号量隔离策略
          thread:
            timeoutInMilliseconds: 1000 #配置HystrixCommand执行的超时时间，执行超过该时间会进行服务降级处理
            interruptOnTimeout: true #配置HystrixCommand执行超时的时候是否要中断
            interruptOnCancel: true #配置HystrixCommand执行被取消的时候是否要中断
          timeout:
            enabled: true #配置HystrixCommand的执行是否启用超时时间
          semaphore:
            maxConcurrentRequests: 10 #当使用信号量隔离策略时，用来控制并发量的大小，超过该并发量的请求会被拒绝
      fallback:
        enabled: true #用于控制是否启用服务降级
      circuitBreaker: #用于控制HystrixCircuitBreaker的行为
        enabled: true #用于控制断路器是否跟踪健康状况以及熔断请求
        requestVolumeThreshold: 20 #超过该请求数的请求会被拒绝
        forceOpen: false #强制打开断路器，拒绝所有请求
        forceClosed: false #强制关闭断路器，接收所有请求
      requestCache:
        enabled: true #用于控制是否开启请求缓存
  collapser: #用于控制HystrixCollapser的执行行为
    default:
      maxRequestsInBatch: 100 #控制一次合并请求合并的最大请求数
      timerDelayinMilliseconds: 10 #控制多少毫秒内的请求会被合并成一个
      requestCache:
        enabled: true #控制合并请求是否开启缓存
  threadpool: #用于控制HystrixCommand执行所在线程池的行为
    default:
      coreSize: 10 #线程池的核心线程数
      maximumSize: 10 #线程池的最大线程数，超过该线程数的请求会被拒绝
      maxQueueSize: -1 #用于设置线程池的最大队列大小，-1采用SynchronousQueue，其他正数采用LinkedBlockingQueue
      queueSizeRejectionThreshold: 5 #用于设置线程池队列的拒绝阀值，由于LinkedBlockingQueue不能动态改版大小，使用时需要用该参数来控制线程数
```

### 实例配置

> 实例配置只需要将全局配置中的default换成与之对应的key即可。

```yaml
hystrix:
  command:
    HystrixComandKey: #将default换成HystrixComrnandKey
      execution:
        isolation:
          strategy: THREAD
  collapser:
    HystrixCollapserKey: #将default换成HystrixCollapserKey
      maxRequestsInBatch: 100
  threadpool:
    HystrixThreadPoolKey: #将default换成HystrixThreadPoolKey
      coreSize: 10
```

#### 配置文件中相关key的说明

- HystrixComandKey对应@HystrixCommand中的commandKey属性；
- HystrixCollapserKey对应@HystrixCollapser注解中的collapserKey属性；
- HystrixThreadPoolKey对应@HystrixCommand中的threadPoolKey属性。

---



# Hystrix Dashboard：断路器执行监控

## 简介

> Hystrix Dashboard 是Spring Cloud中查看Hystrix实例执行情况的一种仪表盘组件，支持查看单个实例和查看集群实例。Hystrix提供了Hystrix Dashboard来实时监控HystrixCommand方法的执行情况。 Hystrix Dashboard可以有效地反映出每个Hystrix实例的运行情况，帮助我们快速发现系统中的问题，从而采取对应措施。

## Hystrix 单个实例监控

### 创建hystrix-dashboard模块

- 在pom.xml中添加相关依赖：

  ```xml
   <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-actuator</artifactId>
          </dependency>
  ```

- 在application.yml进行配置：

  ```yaml
  server:
    port: 8007
  spring:
    application:
      name: hystrix-dashboard
  eureka:
    client:
      fetch-registry: true
      register-with-eureka: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  ```

- 在启动类上添加@EnableHystrixDashboard来启用监控功能：

  ```java
  @EnableDiscoveryClient
  @EnableHystrixDashboard
  @SpringBootApplication
  public class HystrixDashboardApplication {
  
      public static void main(String[] args) {
          SpringApplication.run(HystrixDashboardApplication.class, args);
      }
      
  }
  ```

### 启动相关服务

> 启动如下服务：eureka-server、user-service、hystrix-service、hystrix-dashboard。

### Hystrix实例监控

- 访问Hystrix Dashboard：http://localhost:8007/hystrix

  ![img](Spring Cloud.assets/16d.png)

- 还有一点值得注意的是，被监控的hystrix-service服务需要开启Actuator的hystrix.stream端点，配置信息如下：

  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: 'hystrix.stream'
  ```

- 同时由于Spring2.X.X版本，需要在被监控服务中添加路径，而且由于原因是spring cloud版本升级后出现的BUG，需要修改dashboard源码见链接https://www.cnblogs.com/jinjiyese153/p/13214629.html

  ```java
  /**
       * springboot 版本如果是2.0则需要添加 ServletRegistrationBean
       * 因为springboot的默认路径不是 "/hystrix.stream"
       * @return
       */
      @Bean
      public ServletRegistrationBean getServlet() {
          HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet();
          ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);
          registrationBean.setLoadOnStartup(1);
          registrationBean.addUrlMappings("/hystrix.stream");
          registrationBean.setName("HystrixMetricsStreamServlet");
          return registrationBean;
      }
  ```

- 修改源码

  - 在maven本地仓库中找到`spring-cloud-netflix-hystrix-dashboard-2.2.3.RELEASE.jar`文件
  - 用解压缩工具打开jar包，找到`templates.hystrix/monitor.ftlh`并打开
  - 将文件里所有的`$(window).load(function() {`修改为`$(window).on("load", function() {`，保存即可

- ![image-20200728175343974](Spring Cloud.assets/hystrixdashboard.png)

- 访问几次接口

  ![image-20200728175439746](Spring Cloud.assets/dashboardshow.png)

- 可以发现曾经我们在@HystrixCommand中添加的commandKey和threadPoolKey属性显示在上面

### Hystrix Dashboard 图表解读

> 图表解读如下，需要注意的是，小球代表该实例健康状态及流量情况，颜色越显眼，表示实例越不健康，小球越大，表示实例流量越大。曲线表示Hystrix实例的实时流量变化。

![img](Spring Cloud.assets/hystrix图表截图.png)

## Hystrix 集群实例监控

> 这里我们使用Turbine来聚合hystrix-service服务的监控信息，然后我们的hystrix-dashboard服务就可以从Turbine获取聚合好的监控信息展示给我们了。

### 创建turbine-service模块

- 在pom.xml中添加相关依赖：

  ```xml
  <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-turbine</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-actuator</artifactId>
          </dependency>
  ```

- 在application.yml进行配置，主要是添加了Turbine相关配置：

  ```yaml
  server:
    port: 8009
  spring:
    application:
      name: turbine-service
  eureka:
    client:
      service-url:
        defaultZone: http://localhost:8001/eureka/
      register-with-eureka: true
      fetch-registry: true
  turbine:
    app-config: hystrix-service #指定需要收集信息的服务名称
    combine-host-port: true # 以主机和端口号来区分服务
    cluster-name-expression: new String('default') # 指定服务所属集群
  ```

- 在启动类上添加@EnableTurbine来启用Turbine相关功能：

  ```java
  package com.yanl.cloud;
  
  import org.springframework.boot.SpringApplication;
  import org.springframework.boot.autoconfigure.SpringBootApplication;
  import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
  import org.springframework.cloud.netflix.turbine.EnableTurbine;
  
  @EnableDiscoveryClient
  @EnableTurbine
  @SpringBootApplication
  public class TurbineServiceApplication {
  
      public static void main(String[] args) {
          SpringApplication.run(TurbineServiceApplication.class, args);
      }
  
  }
  ```

### 启动相关服务

> 使用application-replica1.yml配置再启动一个hystrix-service服务，启动turbine-service服务，此时注册中心显示如下。

- application-replica1.yml

  ```yaml
  server:
    port: 8008
  spring:
    application:
      name: hystrix-service
  eureka:
    client:
      register-with-eureka: true
      fetch-registry: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  
  service-url:
    user-service: http://user-service
  management:
    endpoints:
      web:
        exposure:
          include: 'hystrix.stream'
  ```

### Hystrix集群监控

- 访问Hystrix Dashboard: http://localhost:8007/hystrix

- 添加集群监控地址，需要注意的是我们需要添加的是turbine-service的监控端点地址：即http://localhost:8009/turbine.stream

- 调用几次hystrix-service的接口

  ![image-20200728181015794](Spring Cloud.assets/turbineshow.png)

- 可以看到Hystrix实例数量变成了两个。

---



# Spring Cloud OpenFeign：基于Ribbon和Hystrix的声明式服务调用

## Feign简介

> Spring Cloud OpenFeign 是声明式的服务调用工具，它整合了Ribbon和Hystrix，拥有负载均衡和服务容错功能。Feign是声明式的服务调用工具，我们只需创建一个接口并用注解的方式来配置它，就可以实现对某个服务接口的调用，简化了直接使用RestTemplate来调用服务接口的开发量。Feign具备可插拔的注解支持，同时支持Feign注解、JAX-RS注解及SpringMvc注解。当使用Feign时，Spring Cloud集成了Ribbon和Eureka以提供负载均衡的服务调用及基于Hystrix的服务容错保护功能。

## 创建feign-service模块

-  在pom.xml中添加相关依赖

  ```xml
  <properties>
          <java.version>1.8</java.version>
          <spring-cloud.version>Hoxton.SR6</spring-cloud.version>
      </properties>
  
      <dependencyManagement>
          <dependencies>
              <dependency>
                  <groupId>org.springframework.cloud</groupId>
                  <artifactId>spring-cloud-dependencies</artifactId>
                  <version>${spring-cloud.version}</version>
                  <type>pom</type>
                  <scope>import</scope>
              </dependency>
          </dependencies>
      </dependencyManagement>
  
  	<dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-openfeign</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
          </dependency>
          <dependency>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <optional>true</optional>
          </dependency>
          <!--集成swagger以及UI-->
          <dependency>
              <groupId>io.springfox</groupId>
              <artifactId>springfox-swagger2</artifactId>
              <version>2.6.1</version>
          </dependency>
  
          <dependency>
              <groupId>io.springfox</groupId>
              <artifactId>springfox-swagger-ui</artifactId>
              <version>2.6.1</version>
          </dependency>
  ```

- 在application.yml中进行配置

  ```yaml
  server:
    port: 8010
  spring:
    application:
      name: feign-service
  eureka:
    client:
      fetch-registry: true
      register-with-eureka: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  feign:
    hystrix:
      enabled: true # 在feign中开启hystrix
  logging:
    level:
      com.yanl.cloud.service.UserService: debug
  ```

- 在启动类上添加@EnableFeignClients注解来启用Feign的客户端功能

  ```java
  package com.yanl.cloud;
  
  import org.springframework.boot.SpringApplication;
  import org.springframework.boot.autoconfigure.SpringBootApplication;
  import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
  import org.springframework.cloud.openfeign.EnableFeignClients;
  import springfox.documentation.swagger2.annotations.EnableSwagger2;
  
  @EnableDiscoveryClient
  @EnableFeignClients
  @EnableSwagger2
  @SpringBootApplication
  public class FeignServiceApplication {
  
      public static void main(String[] args) {
          SpringApplication.run(FeignServiceApplication.class, args);
      }
  
  }
  ```

- 添加UserService接口完成对user-service服务的接口绑定

  我们通过@FeignClient注解实现了一个Feign客户端，其中的value为user-service表示这是对user-service服务的接口调用客户端。我们可以回想下user-service中的UserController，只需将其改为接口，保留原来的SpringMvc注释即可。

  ```java
  package com.yanl.cloud.service;
  
  import com.yanl.cloud.domain.CommonResult;
  import com.yanl.cloud.domain.User;
  import com.yanl.cloud.service.impl.UserFallbackService;
  import org.springframework.cloud.openfeign.FeignClient;
  import org.springframework.web.bind.annotation.*;
  
  @FeignClient(value = "user-service", fallback = UserFallbackService.class)
  public interface UserService {
      @PostMapping("/user/create")
      CommonResult create(@RequestBody User user);
  
      @GetMapping("/user/{id}")
      CommonResult<User> getUser(@PathVariable Long id);
  
      @GetMapping("/user/getUserByName")
      CommonResult<User> getUserByName(@RequestParam String name);
  
      @PostMapping("/user/update")
      CommonResult update(@RequestBody User user);
  
      @PostMapping("/user/delete/{id}")
      CommonResult delete(@PathVariable Long id);
  }
  ```

- 添加UserFeignController调用UserService实现服务调用

  ```java
  package com.yanl.cloud.controller;
  
  import com.yanl.cloud.domain.CommonResult;
  import com.yanl.cloud.domain.User;
  import com.yanl.cloud.service.UserService;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.web.bind.annotation.*;
  
  @RestController
  @RequestMapping("/user")
  public class UserFeignController {
  
      @Autowired
      private UserService userService;
  
      @GetMapping("/{id}")
      public CommonResult getUser(@PathVariable Long id){
          return userService.getUser(id);
      }
      @PostMapping("/create")
      public CommonResult create(@RequestBody User user){
          return userService.create(user);
      }
  
      @GetMapping("/getUserByName")
      public CommonResult<User> getUserByName(@RequestParam String name){
          return userService.getUserByName(name);
      }
  
      @PostMapping("/update")
      public CommonResult update(@RequestBody User user){
          return userService.update(user);
      }
  
      @PostMapping("/delete/{id}")
      public CommonResult delete(@PathVariable Long id){
          return userService.delete(id);
      }
  }
  ```

  ## 负载均衡功能

  ![image-20200729112036484](Spring Cloud.assets/feign-eureka.png)

  多次调用接口可以发现两个user-service终端交替打印日志

  ![image-20200729112153787](Spring Cloud.assets/feign-interface1.png)

## Feign中的服务降级

> Feign中的服务降级使用起来非常方便，只需要为Feign客户端定义的接口添加一个服务降级处理的实现类即可，下面我们为UserService接口添加一个服务降级实现类。

### 添加服务降级实现类UserFallbackService

> 需要注意的是它实现了UserService接口，并且对接口中的每个实现方法进行了服务降级逻辑的实现。

```java
package com.yanl.cloud.service.impl;

import com.yanl.cloud.domain.CommonResult;
import com.yanl.cloud.domain.User;
import com.yanl.cloud.service.UserService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserFallbackService implements UserService {
    @Override
    public CommonResult create(User user) {
        User defaultUser = new User(-1L, "defaultUser", "123456");
        return new CommonResult<>(defaultUser);
    }

    @Override
    public CommonResult<User> getUser(Long id) {
        User defaultUser = new User(-1L, "defaultUser", "123456");
        return new CommonResult<>(defaultUser);
    }


    @Override
    public CommonResult<User> getUserByName(String name) {
        User defaultUser = new User(-1L, "defaultUser", "123456");
        return new CommonResult<>(defaultUser);
    }

    @Override
    public CommonResult update(User user) {
        return new CommonResult("调用失败，服务被降级", 500);
    }

    @Override
    public CommonResult delete(Long id) {
        return new CommonResult("调用失败，服务被降级", 500);
    }
}
```

### 修改UserService接口，设置服务降级处理类为UserFallbackService

```java
@FeignClient(value = "user-service", fallback = UserFallbackService.class)
```

### 修改application.yml，开启Hystrix功能

```yaml
feign:
  hystrix:
    enabled: true # 在feign中开启hystrix
```

## 服务降级功能

- 关闭两个user-service，调用接口测试，返回服务降级信息。

  ![image-20200729112915636](Spring Cloud.assets/feign降级.png)

## 日志打印功能

> Feign提供了日志打印功能，我们可以通过配置来调整日志级别，从而了解Feign中Http请求的细节。

### 日志级别

- NONE：默认的，不显示任何日志；
- BASIC：仅记录请求方法、URL、响应状态码及执行时间；
- HEADERS：除了BASIC中定义的信息之外，还有请求和响应的头信息；
- FULL：除了HEADERS中定义的信息之外，还有请求和响应的正文及元数据。

### 通过配置开启更为详细的日志

```java
package com.yanl.cloud.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }
}
```

### 在application.yml中配置需要开启日志的Feign客户端

> 配置UserService的日志级别为debug。

```yaml
logging:
  level:
    com.yanl.cloud.service.UserService: debug
```

调用接口可以看到日志

```
2020-07-29 10:57:54.547 DEBUG 25384 --- [-user-service-1] com.yanl.cloud.service.UserService       : [UserService#getUser] <--- HTTP/1.1 200 (367ms)
2020-07-29 10:57:54.548 DEBUG 25384 --- [-user-service-1] com.yanl.cloud.service.UserService       : [UserService#getUser] connection: keep-alive
2020-07-29 10:57:54.548 DEBUG 25384 --- [-user-service-1] com.yanl.cloud.service.UserService       : [UserService#getUser] content-type: application/json
2020-07-29 10:57:54.548 DEBUG 25384 --- [-user-service-1] com.yanl.cloud.service.UserService       : [UserService#getUser] date: Wed, 29 Jul 2020 02:57:54 GMT
2020-07-29 10:57:54.548 DEBUG 25384 --- [-user-service-1] com.yanl.cloud.service.UserService       : [UserService#getUser] keep-alive: timeout=60
2020-07-29 10:57:54.548 DEBUG 25384 --- [-user-service-1] com.yanl.cloud.service.UserService       : [UserService#getUser] transfer-encoding: chunked
2020-07-29 10:57:54.548 DEBUG 25384 --- [-user-service-1] com.yanl.cloud.service.UserService       : [UserService#getUser] 
2020-07-29 10:57:54.552 DEBUG 25384 --- [-user-service-1] com.yanl.cloud.service.UserService       : [UserService#getUser] {"data":{"id":1,"username":"ly","password":"5211"},"msg":"操作成功","code":200}
2020-07-29 10:57:54.552 DEBUG 25384 --- [-user-service-1] com.yanl.cloud.service.UserService       : [UserService#getUser] <--- END HTTP (83-byte body)
```

## Feign的常用配置

### Feign自己的配置

```yaml
feign:
  hystrix:
    enabled: true #在Feign中开启Hystrix
  compression:
    request:
      enabled: false #是否对请求进行GZIP压缩
      mime-types: text/xml,application/xml,application/json #指定压缩的请求数据类型
      min-request-size: 2048 #超过该大小的请求会被压缩
    response:
      enabled: false #是否对响应进行GZIP压缩
logging:
  level: #修改日志级别
    com.macro.cloud.service.UserService: debug
```

### Feign中的Ribbon配置

在Feign中配置Ribbon可以直接使用Ribbon的配置，具体可以参考[Ribbon配置](#Ribbon的常用配置)

### Feign中的Hystrix配置

在Feign中配置Hystrix可以直接使用Hystrix的配置，具体可以参考[Hystrix配置](#Hystrix的常用配置)

---



# Spring Cloud Zuul：API网关服务

## Zuul简介

Spring Cloud Zuul 是Spring Cloud Netflix 子项目的核心组件之一，可以作为微服务架构中的API网关使用，支持动态路由与过滤功能。

API网关为微服务架构中的服务提供了统一的访问入口，客户端通过API网关访问相关服务。API网关的定义类似于设计模式中的门面模式，它相当于整个微服务架构中的门面，所有客户端的访问都通过它来进行路由及过滤。它实现了请求路由、负载均衡、校验过滤、服务容错、服务聚合等功能。

## 创建zuul-proxy模块

-  在pom.xml中添加相关依赖

  ```xml
  <properties>
          <java.version>1.8</java.version>
          <spring-cloud.version>Hoxton.SR6</spring-cloud.version>
      </properties>
  
      <dependencyManagement>
          <dependencies>
              <dependency>
                  <groupId>org.springframework.cloud</groupId>
                  <artifactId>spring-cloud-dependencies</artifactId>
                  <version>${spring-cloud.version}</version>
                  <type>pom</type>
                  <scope>import</scope>
              </dependency>
          </dependencies>
      </dependencyManagement>
  
  	<dependency>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <optional>true</optional>
          </dependency>
  
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
          </dependency>
  
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-actuator</artifactId>
          </dependency>
  ```

- 在application.yml中进行配置

  ```yaml
  server:
    port:
      8011
  spring:
    application:
      name: zuul-proxy
  eureka:
    client:
      fetch-registry: true
      register-with-eureka: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  ```

-  在启动类上添加@EnableZuulProxy注解来启用Zuul的API网关功能

  ```java
  package com.yanl.cloud;
  
  import org.springframework.boot.SpringApplication;
  import org.springframework.boot.autoconfigure.SpringBootApplication;
  import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
  import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
  
  @EnableDiscoveryClient
  @EnableZuulProxy
  @SpringBootApplication
  public class ZuulProxyApplication {
  
      public static void main(String[] args) {
          SpringApplication.run(ZuulProxyApplication.class, args);
      }
  
  }
  ```

## 常用功能

### 启动相关服务

>启动eureka-server，两个user-service，feign-service和zuul-proxy

### 配置路由规则

- 我们可以通过修改application.yml中的配置来配置路由规则，这里我们将匹配`/userService/**`的请求路由到user-service服务上去，匹配`/feignService/**`的请求路由到feign-service上去。

  ```yaml
  zuul:
    routes: #给网关配置路由
      user-service:
        path: /user-service/**
      feign-service:
        path: /feign-service/**
  ```

- 访问 http://localhost:8011/user-service/user/1 可以发现请求路由到了user-service上

### 默认路由规则

- Zuul和Eureka结合使用，可以实现路由的自动配置，自动配置的路由以服务名称为匹配路径，相当于上述配置。

- 如果不想使用默认的路由规则，可以添加以下配置来忽略默认路由配置：

  ```yaml
  zuul:
    ignored-services: user-service,feign-service #关闭默认路由配置
  ```

### 负载均衡功能

- 多次访问 http://localhost:8011/user-service/user/1 进行测试，可以发现两个user-srevice交替打印日志。

### 配置访问前缀

- 可以通过以下配置来给网关路径添加前缀，此处添加了/proxy前缀，这样我们需要访问http://localhost:8011/proxy/user-service/user/1才能访问到user-service中的接口。

  ```yaml
  zuul:
    add-host-header: true #设置为true重定向是会添加host请求头
  ```

### Header过滤及重定向添加Host

- Zuul在请求路由时，默认会过滤掉一些敏感的头信息，以下配置可以防止路由时的Cookie及Authorization的丢失：

  ```yaml
  zuul:
    sensitive-headers: Cookie,Set-Cookie,Authorization #配置过滤敏感的请求头信息，设置为空就不会过滤
  ```

- Zuul在请求路由时，不会设置最初的host头信息，以下配置可以解决：

  ```yaml
  zuul:
    add-host-header: true #设置为true重定向是会添加host请求头
  ```

### 查看路由信息

> 我们可以通过SpringBoot Actuator来查看Zuul中的路由信息。

- 在pom文件中添加相关依赖

  ```xml
  		<dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-actuator</artifactId>
          </dependency>
  ```

- 修改application.yaml配置文件，开启查看路由的端点：

  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: 'routes'
  ```

- 访问 http://localhost:8011/actuator/routes 查看简单路由信息。

  ![image-20200729145537813](Spring Cloud.assets/simpleroute)

- 访问 http://localhost:8011/actuator/routes/details 查看详细信息。

  ![image-20200729145622129](Spring Cloud.assets/routedetails)

## 过滤器

>路由与过滤是Zuul的两大核心功能，路由功能负责将外部请求转发到具体的服务实例上去，是实现统一访问入口的基础，过滤功能负责对请求过程进行额外的处理，是请求校验过滤及服务聚合的基础。

### 过滤器类型

- pre：在请求被路由到目标服务前执行，比如权限校验、打印日志等功能；
- routing：在请求被路由到目标服务时执行，这是使用Apache HttpClient或Netflix Ribbon构建和发送原始HTTP请求的地方；
- post：在请求被路由到目标服务后执行，比如给目标服务的响应添加头信息，收集统计数据等功能；
- error：请求在其他阶段发生错误时执行。

### 过滤器的生命周期

> 下图描述了一个HTTP请求到达API网关后，如何在各种不同类型的过滤器中流转的过程。

![img](Spring Cloud.assets/filterlife.png)

### 自定义过滤器

#### 添加PreLogFilter类继承ZuulFilter

> 这是一个前置过滤器，用于在请求路由到目标服务前打印请求日志。

```java
package com.yanl.cloud.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

@Component
@Slf4j
public class PreLogFilter extends ZuulFilter {
    /**
     * 过滤器类型 有pre, routing, post, error四种
     * @return
     */
    @Override
    public String filterType() {
        return "pre";
    }

    /**
     * 过滤器执行顺序，数值越小，优先级越高
     * @return
     */
    @Override
    public int filterOrder() {
        return 1;
    }

    /**
     * 是否进行过滤
     * @return
     */
    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     * 自定义的过滤去逻辑
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        String host = request.getRemoteHost();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.info("Remote Host: {}, method: {}, uri: {}", host, method, uri);
        return null;
    }
}
```

- 添加过滤器后访问 http://localhost:8011/proxy/user-service/user/1 可以看到控制台打印日志

  ```
  2020-07-29 14:37:08.639  INFO 13376 --- [nio-8011-exec-5] com.yanl.cloud.filter.PreLogFilter       : Remote Host: 0:0:0:0:0:0:0:1, method: GET, uri: /proxy/user-service/user/1
  ```

### 核心过滤器

| 过滤器名称              | 过滤类型 | 优先级 | 作用                                                         |
| :---------------------- | -------- | :----: | :----------------------------------------------------------- |
| ServletDetectionFilter  | pre      |   -3   | 检测当前请求是通过DispatchServlet处理运行还是ZuulServlet处理的 |
| Servlet30WrapperFilter  | pre      |   -2   | 对原始的HttpServletRequest进行包装                           |
| FormBodyWarpperFilter   | pre      |   -1   | 将Content-Type为application/x-www-form-urlencoded或multipart/form-data的请求包装成FormBodyRequestWrapper对象。 |
| DebugFilter             | route    |   1    | 根据zuul.debug.request的配置来决定是否打印debug日志。        |
| PreDecorationFilter     | route    |   5    | 对当前请求进行预处理以便执行后续操作。                       |
| RibbonRoutingFilter     | route    |   10   | 通过Ribbon和Hystrix来向服务实例发起请求，并将请求结果进行返回。 |
| SimpleHostRoutingFilter | route    |  100   | 只对请求上下文中有routeHost参数的进行处理，直接使用HttpClient向routeHost对应的物理地址进行转发。 |
| SendForwardFilter       | route    |  500   | 只对请求上下文中有forward.to参数的进行处理，进行本地跳转。   |
| SendErrorFilter         | post     |   0    | 当其他过滤器内部发生异常时的会由它来进行处理，产生错误响应。 |
| SendResponseFilter      | post     |  1000  | 利用请求上下文的响应信息来组织请求成功的响应内容。           |

### 禁用过滤器

- 可以对过滤器进行禁用的配置，配置格式如下：

  ```yaml
  zuul:
    PreLogFilter:
      pre:
        disable: true 
  ```

## Ribbon和Hystrix的支持

> 由于Zuul自动集成了Ribbon和Hystrix，所以Zuul天生就有负载均衡和服务容错能力，我们可以通过Ribbon和Hystrix的配置来配置Zuul中的相应功能。

- 可以使用Hystrix的配置来设置路由转发时HystrixCommand的执行超时时间：

  ```yaml
  hystrix:
    command: #用于控制HystrixCommand的行为
      default:
        execution:
          isolation:
            thread:
              timeoutInMilliseconds: 1000 #配置HystrixCommand执行的超时时间，执行超过该时间会进行服务降级处理
  ```

- 可以使用Ribbon的配置来设置路由转发时请求连接及处理的超时时间：

  ```yaml
  ribbon: #全局配置
    ConnectTimeout: 1000 #服务请求连接超时时间（毫秒）
    ReadTimeout: 3000 #服务请求处理超时时间（毫秒）
  ```

## 常用配置

```yaml
zuul:
  routes: #给服务配置路由
    user-service:
      path: /userService/**
    feign-service:
      path: /feignService/**
  ignored-services: user-service,feign-service #关闭默认路由配置
  prefix: /proxy #给网关路由添加前缀
  sensitive-headers: Cookie,Set-Cookie,Authorization #配置过滤敏感的请求头信息，设置为空就不会过滤
  add-host-header: true #设置为true重定向是会添加host请求头
  retryable: true # 关闭重试机制
  PreLogFilter:
    pre:
      disable: false #控制是否启用过滤器
```

---



# Spring Cloud Config：外部集中化配置管理

## Spring Cloud Config 简介

> Spring Cloud Config 可以为微服务架构中的应用提供集中化的外部配置支持，它分为服务端和客户端两个部分，服务端被称为分布式配置中心，它是个独立的应用，可以从配置仓库获取配置信息并提供给客户端使用。客户端可以通过配置中心来获取配置信息，在启动时加载配置。Spring Cloud Config 的配置中心默认采用Git来存储配置信息，所以天然就支持配置信息的版本管理，并且可以使用Git客户端来方便地管理和访问配置信息。

## 在Git仓库中准备配置信息

> Git仓库地址为 https://github.com/YanLdt/springcloud-config.git

![image-20200729175432882](Spring Cloud.assets/git仓库)

### master分支下的配置信息

- config-dev.yml

  ```yaml
  config:
    info: "config info for dev(master)"
  ```

- config-test.yml

  ```yaml
  config:
    info: "config info for test(master)"
  ```

- config-prod.yml

  ```yaml
  config:
    info: "config info for prod(master)"
  ```

### dev分支下的配置信息
- config-dev.yml

  ```yaml
  config:
    info: "config info for dev(dev)"
  ```

- config-test.yml

  ```yaml
  config:
    info: "config info for test(dev)"
  ```

- config-prod.yml

  ```yaml
  config:
    info: "config info for prod(dev)"
  ```

## 创建config-server模块

- 在pom.xml中添加相关依赖

  ```xml
  <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-config-server</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
          </dependency>
  ```

- 在application.yml中进行配置

  ```yaml
  server:
    port: 8012
  spring:
    application:
      name: config-server
    cloud:
      config:
        server:
          git: # 配置存储信息的git仓库 注意要使用https方式
            uri: https://github.com/YanLdt/springcloud-config.git
            username: YanLdt
            password: datouwudi233
            clone-on-start: true # 开启启动时自动从git获取配置
  #          search-paths: '{application}'
  eureka:
    client:
      fetch-registry: true
      register-with-eureka: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  ```

- 在启动类上添加@EnableConfigServer注解来启用配置中心功能

  ```java
  package com.yanl.cloud;
  
  import org.springframework.boot.SpringApplication;
  import org.springframework.boot.autoconfigure.SpringBootApplication;
  import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
  import org.springframework.cloud.config.server.EnableConfigServer;
  
  @EnableDiscoveryClient
  @EnableConfigServer
  @SpringBootApplication
  public class ConfigServerApplication {
  
      public static void main(String[] args) {
          SpringApplication.run(ConfigServerApplication.class, args);
      }
  
  }
  ```


  ### 通过config-server获取配置信息

#### 获取配置文件信息的访问格式

  ```
  # 获取配置信息
  /{label}/{application}-{profile}
  # 获取配置文件信息
  /{label}/{application}-{profile}.yml
  ```

  #### 占位符相关解释

- application：代表应用名称，默认为配置文件中的spring.application.name，如果配置了spring.cloud.config.name，则为该名称；
- label：代表分支名称，对应配置文件中的spring.cloud.config.label；
- profile：代表环境名称，对应配置文件中的spring.cloud.config.profile。

#### 获取配置文件信息

- 访问 http://localhost:8014/dev/config-dev.yml 

  ![image-20200729180421466](Spring Cloud.assets/configinfo.png)





## 创建config-client模块

-  在pom.xml中添加相关依赖

  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-config</artifactId>
  </dependency>
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  ```

- 在bootstrap.yml中进行配置

  ```yaml
  server:
    port: 8013
  spring:
    application:
      name: config-client
    cloud:
      config: # Config客户端配置
        profile: dev # 启用配置后缀名称
        label: dev # 分支名称
        uri: http://localhost:8014/ # 配置中心地址
        name: config #配置文件名称
        username: ly #配置安全中心账户密码
        password: 5211
  eureka:
    client:
      register-with-eureka: true
      fetch-registry: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  management:
    endpoints:
      web:
        exposure:
          include: 'refresh'
  ```


### 添加ConfigClientController类用于获取配置

  ```java
  package com.yanl.cloud.controller;
  
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.cloud.context.config.annotation.RefreshScope;
  import org.springframework.web.bind.annotation.GetMapping;
  import org.springframework.web.bind.annotation.RestController;
  
  @RestController
  @RefreshScope
  public class ConfigClientController {
  
      @Value("${config.info}")
      private String configInfo;
  
      @GetMapping("/getConfigInfo")
      public String getConfigInfo(){
          return configInfo;
      }
  }
  ```

  ### 从配置中心获取配置

- 访问接口即可获取配置信息。

### 获取子目录下的配置

> 不仅可以把每个项目的配置放在不同的Git仓库存储，也可以在一个Git仓库中存储多个项目的配置，此时就会用到在子目录中搜索配置信息的配置。

需要在config-server中添加相关配置，用于搜索子目录中的配置，这里我们用到了application占位符，表示对于不同的应用，我们从对应应用名称的子目录中搜索配置，比如config子目录中的配置对应config应用；配置在服务端

```yaml
spring:
  cloud:
    config:
      server:
        git: 
          search-paths: '{application}' #配置在服务端
```

### 刷新配置

> 当Git仓库中的配置信息更改后，我们可以通过SpringBoot Actuator的refresh端点来刷新客户端配置信息，以下更改都需要在config-client中进行。

- 在pom.xml中添加Actuator的依赖：

  ```xml
  <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
  ```

- 在bootstrap.yml中开启refresh端点：

  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: 'refresh'
  ```

- 在ConfigClientController类添加@RefreshScope注解用于刷新配置：

- 重新启动config-client后，调用refresh端点进行配置刷新：post 请求 http://localhost:8013/actuator/refresh

  ![image-20200729181500574](Spring Cloud.assets/post.png)

## 配置中心添加安全认证

> 通过整合SpringSecurity来为配置中心添加安全认证。

### 创建config-security-server模块

- 在pom.xml中添加相关依赖：

  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-config-server</artifactId>
  </dependency>
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  ```

- 在application.yml中进行配置：

  添加用户和密码

  ```yaml
  server:
    port: 8014
  spring:
    application:
      name: config-security-server
    cloud:
      config:
        server:
          git: # 配置存储信息的git仓库
            uri: https://github.com/YanLdt/springcloud-config.git
            username: YanLdt
            password: datouwudi233
            clone-on-start: true # 开启启动时自动从git获取配置
  #          search-paths: '{application}'
    security:
      user:
        name: ly
        password: 5211
  eureka:
    client:
      fetch-registry: true
      register-with-eureka: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  ```

### 修改config-client的配置

- 添加bootstrap-security.yml配置文件，主要是配置了配置中心的用户名和密码：

  ```yaml
  server:
    port: 8013
  spring:
    application:
      name: config-client
    cloud:
      config: # Config客户端配置
        profile: dev # 启用配置后缀名称
        label: dev # 分支名称
        uri: http://localhost:8014/ # 配置中心地址
        name: config #配置文件名称
        username: ly #配置安全中心账户密码
        password: 5211
  eureka:
    client:
      register-with-eureka: true
      fetch-registry: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  management:
    endpoints:
      web:
        exposure:
          include: 'refresh'
  ```

## config-sever集群搭建

- 启动两个config-server

- 添加config-client的配置文件bootstrap-cluster.yml，主要是添加了从注册中心获取配置中心地址的配置并去除了配置中心uri的配置：

  ```yaml
  server:
    port: 8016
  spring:
    application:
      name: config-client
    cloud:
      config: # Config客户端配置
        profile: dev # 启用配置后缀名称
        label: dev # 分支名称
  #      uri: http://localhost:8014/ # 配置中心地址
        name: config #配置文件名称
        discovery: # 表示从注册中心获取配置中心的地址
          enabled: true
          service-id: config-server
  #      username: ly #配置安全中心账户密码
  #      password: 5211
  eureka:
    client:
      register-with-eureka: true
      fetch-registry: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  management:
    endpoints:
      web:
        exposure:
          include: 'refresh'
  
  ```

- 以bootstrap-cluster.yml启动config-client服务

- client将会在注册中心搜索配置中心地址。

---



# Spring Cloud Bus：消息总线

## Spring Cloud Bus 简介

Spring Cloud Bus 使用轻量级的消息代理来连接微服务架构中的各个服务，可以将其用于广播状态更改（例如配置中心配置更改）或其他管理指令。

我们通常会使用消息代理来构建一个主题，然后把微服务架构中的所有服务都连接到这个主题上去，当我们向该主题发送消息时，所有订阅该主题的服务都会收到消息并进行消费。使用 Spring Cloud Bus 可以方便地构建起这套机制，所以 Spring Cloud Bus 又被称为消息总线。Spring Cloud Bus 配合 Spring Cloud Config 使用可以实现配置的动态刷新。目前  Spring Cloud Bus 支持两种消息代理：RabbitMQ 和 Kafka

## RabbitMQ的安装

- 安装Erlang，推荐使用rabbitMQ推荐的网址 https://www.erlang-solutions.com/resources/download.html

  安装完成后记得添加环境变量。

- 安装完成后，进入RabbitMQ安装目录下的sbin目录：

  cmd

  ```shell
  rabbitmq-plugins enable rabbitmq_management
  ```

- 访问地址 http://lcoalhost:15672 用户名和密码都是guest

## 动态刷新配置

> 这里使用上一节的config-server和config-client

### 给config-server添加消息总线支持

- 在pom.xml中添加相关依赖：

  ```xml
  <!--        添加消息总线支持-->
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-bus-amqp</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-actuator</artifactId>
          </dependency>
  ```

- 添加配置文件application-amqp.yml，主要是添加了RabbitMQ的配置及暴露了刷新配置的Actuator端点；

  ```yaml
  server:
    port: 8017
  spring:
    application:
      name: config-server
    cloud:
      config:
        server:
          git: # 配置存储信息的git仓库
            uri: https://github.com/YanLdt/springcloud-config.git
            username: YanLdt
            password: datouwudi233
            clone-on-start: true # 开启启动时自动从git获取配置
  #          search-paths: '{application}'
    rabbitmq: #配置rabbitmq
      host: localhost
      port: 5672
      username: guest
      password: guest
  eureka:
    client:
      fetch-registry: true
      register-with-eureka: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  management:
    endpoints: # 暴露bus刷新配置的端点
      web:
        exposure:
          include: 'bus-refresh'
  ```

  使用application-amqp-yml文件启动服务

  ![image-20200730112809929](Spring Cloud.assets/image-20200730112809929.png)

### 给config-client添加消息总线支持

- 在pom.xml中添加相关依赖：

  ```xml
          <!--        添加消息总线支持-->
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-bus-amqp</artifactId>
          </dependency>
  ```

- 添加配置文件bootstrap-amqp1.yml及bootstrap-amqp2.yml用于启动两个不同的config-client，两个配置文件只有端口号不同；

  ```yaml
  #amqp1
  server:
    port: 8018
  spring:
    application:
      name: config-client
    cloud:
      config: # Config客户端配置
        profile: dev # 启用配置后缀名称
        label: dev # 分支名称
        #      uri: http://localhost:8014/ # 配置中心地址
        name: config #配置文件名称
        discovery: # 表示从注册中心获取配置中心的地址
          enabled: true
          service-id: config-server
  #      username: ly #配置安全中心账户密码
  #      password: 5211
    rabbitmq:
      host: localhost
      port: 5672
      username: guest
      password: guest
  eureka:
    client:
      register-with-eureka: true
      fetch-registry: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  
  management:
    endpoints:
      web:
        exposure:
          include: 'refresh'
  
  #amqp2
  server:
    port: 8019
  spring:
    application:
      name: config-client
    cloud:
      config: # Config客户端配置
        profile: dev # 启用配置后缀名称
        label: dev # 分支名称
        #      uri: http://localhost:8014/ # 配置中心地址
        name: config #配置文件名称
        discovery: # 表示从注册中心获取配置中心的地址
          enabled: true
          service-id: config-server
  #      username: ly #配置安全中心账户密码
  #      password: 5211
    rabbitmq:
      host: localhost
      port: 5672
      username: guest
      password: guest
  eureka:
    client:
      register-with-eureka: true
      fetch-registry: true
      service-url:
        defaultZone: http://localhost:8001/eureka/
  management:
    endpoints:
      web:
        exposure:
          include: 'refresh'
  ```

  > 指定bootstrap文件启动应用，在Program arguments里添加配置 `--spring.cloud.bootstrap.name=bootstrap-amqp1`

  ![image-20200730113056677](Spring Cloud.assets/image-20200730113056677.png)

### 动态刷新配置

启动相关服务，启动eureka-server，以application-amqp.yml为配置启动config-server，以bootstrap-amqp1.yml为配置启动config-client，以bootstrap-amqp2.yml为配置再启动一个config-client

启动所有服务后，我们登录RabbitMQ的控制台可以发现Spring Cloud Bus 创建了一个叫springCloudBus的交换机及三个以 springCloudBus.anonymous开头的队列：

![image-20200730113305422](Spring Cloud.assets/image-20200730113305422.png)

![image-20200730113327357](Spring Cloud.assets/image-20200730113327357.png)

- 修改Git仓库中dev分支下的config-dev.yml配置文件：

- 调用注册中心的接口刷新所有配置 http://localhost:8017/actuator/bus-refresh

  ![image-20200730113551563](Spring Cloud.assets/image-20200730113551563.png)

  可以看到rabbitMQ队列里是有消息消费的

  ![image-20200730113743389](Spring Cloud.assets/image-20200730113743389.png)

- 即可在client端读取最新配置信息

- 如果只需要刷新指定实例的配置可以使用以下格式进行刷新：http://localhost:8017/actuator/bus-refresh/{destination} ，我们这里以刷新运行在8018端口上的config-client为例http://localhost:8017/actuator/bus-refresh/config-client:8018。

## 配合WebHooks使用

WebHooks相当于是一个钩子函数，我们可以配置当向Git仓库push代码时触发这个钩子函数

![img](Spring Cloud.assets/16dd48b117c053.png)

-----

# Spring Cloud Sleuth：分布式请求链路跟踪

## Spring Cloud Sleuth简介

Spring Cloud Sleuth 是分布式系统中跟踪服务间调用的工具，它可以直观地展示出一次请求的调用过程。

随着我们的系统越来越庞大，各个服务间的调用关系也变得越来越复杂。当客户端发起一个请求时，这个请求经过多个服务后，最终返回了结果，经过的每一个服务都有可能发生延迟或错误，从而导致请求失败。这时候我们就需要请求链路跟踪工具来帮助我们，理清请求调用的服务链路，解决问题。

## 给服务添加请求链路追踪

通过user-service和ribbon-service之间的服务调用来演示该功能，这里调用ribbon-service的接口时，ribbon-service会通过RestTemplate来调用user-service提供的接口。

- 首先给user-service和ribbon-service添加请求链路跟踪功能的支持；

- 在user-service和ribbon-service中添加相关依赖：

  ```xml
          <!--        添加请求链路追踪依赖-->
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-zipkin</artifactId>
          </dependency>
  ```

- 修改application.yml文件，配置收集日志的zipkin-server访问地址：

  ```yaml
  spring:
    zipkin: # 设置zipkin-server访问地址
      base-url: http://localhost:9411
    sleuth:
      sampler:
        probability: 0.1 #设置Sleuth的抽样收集概率
  ```

## 整合Zipkin获取及分析日志

> Zipkin是Twitter的一个开源项目，可以用来获取和分析Spring Cloud Sleuth 中产生的请求链路跟踪日志，它提供了Web界面来帮助我们直观地查看请求链路跟踪信息。

- SpringBoot 2.0以上版本已经不需要自行搭建zipkin-server，我们可以从该地址下载zipkin-server：https://repo1.maven.org/maven2/io/zipkin/java/zipkin-server/2.12.9/zipkin-server-2.12.9-exec.jar

- 下载完成后使用以下命令运行zipkin-server：

  ```txt
  java -jar zipkin-server-2.12.9-exec.jar
  ```

- Zipkin页面访问地址：http://localhost:9411

- 启动eureka-sever，ribbon-service，user-service：

- 访问几次ribbon-service接口

  ![image-20200730151944795](Spring Cloud.assets/image-20200730151944795.png)

- 点击查看详情可以直观地看到请求调用链路和通过每个服务的耗时：

  ![image-20200730152103815](Spring Cloud.assets/image-20200730152103815.png)

  ![image-20200730152114922](Spring Cloud.assets/image-20200730152114922.png)

## 使用Elasticsearch存储跟踪信息

如果我们把zipkin-server重启一下就会发现刚刚的存储的跟踪信息全部丢失了，可见其是存储在内存中的，有时候我们需要将所有信息存储下来，这里把信息存储到Elasticsearch。

### 安装Elasticsearch

- 下载Elasticsearch6.2.2的zip包，并解压到指定目录，下载地址：https://www.elastic.co/cn/downloads/past-releases/elasticsearch-6-2-2
- 运行bin目录下的elasticsearch.bat启动Elasticsearch

### 修改Zipkin启动参数将信息存储到Elasticsearch

- 使用以下命令运行，就可以把跟踪信息存储到Elasticsearch里面去了，重新启动也不会丢失；

  ```
  # STORAGE_TYPE：表示存储类型 ES_HOSTS：表示ES的访问地址
  java -jar zipkin-server-2.12.9-exec.jar --STORAGE_TYPE=elasticsearch --ES_HOSTS=localhost:9200 
  ```

- 之后需要重新启动user-service和ribbon-service才能生效

### 安装Kibana可视化工具查看跟踪信息

- 下载Kibana6.2.2的zip包，并解压到指定目录，下载地址：https://artifacts.elastic.co/downloads/kibana/kibana-6.2.2-windows-x86_64.zip

- 解压，会创建一个文件夹叫 kibana-6.6.0-windows-x86_64，也就是我们指的 `$KIBANA_HOME` 。删除多余的字符，保留kibana

- `.zip` 整个包是独立的。默认情况下，所有的文件和目录都在 `$KIBANA_HOME` — 解压包时创建的目录下。这是非常方便的，因为您不需要创建任何目录来使用 Kibana，卸载 Kibana 只需要简单的删除 `$KIBANA_HOME`目录。但还是建议修改一下配置文件和数据目录，这样就不会删除重要数据。

- 通过配置文件配置 Kibana。Kibana 默认情况下从 `$KIBANA_HOME/config/kibana.yml` 加载配置文件。***\*设置`elasticsearch.url`为指向您的Elasticsearch实例\****

- 双击 bin\kibana.bat，默认情况下，Kibana 在前台启动，输出 log 到 `STDOUT` ，可以通过 `Ctrl-C` 停止 Kibana。

- 访问 http://localhost:5601

  ![image-20200730152909131](Spring Cloud.assets/image-20200730152909131.png)

---

