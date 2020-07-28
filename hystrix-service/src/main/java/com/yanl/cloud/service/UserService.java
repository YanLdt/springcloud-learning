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

    @CacheRemove(commandKey = "getUserCache", cacheKeyMethod = "getCacheKey")
    @HystrixCommand
    public CommonResult removeCache(Long id){
        log.info("removeCache id:{}", id);
        return restTemplate.postForObject(userServiceUrl + "/user/delete/{1}",null, CommonResult.class, id);
    }

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


}
