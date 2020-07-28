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

  ![image-20200727135843326](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\eureka-server)

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

  ![image-20200727140125526](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\eureka-client)

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
  server:
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

- ### 运行Eureka注册中心集群

  ![image-20200727140553830](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\replica1)

  ![image-20200727140627149](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\replica2)

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

![image-20200727141434842](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\security-client)



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

  ![image-20200727173232413](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\multi-service.png)

- 此时运行的服务

  ![image-20200727173322084](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\servicerunning)

- swagger接口测试

  ![image-20200727173357800](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\swagger测试)

  ![image-20200727173540455](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\service1)

  ![image-20200727173606447](H:\OldHardDrive\ly\java\springcloud\Spring Cloud.assets\service2)

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

