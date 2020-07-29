

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

