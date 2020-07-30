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

    /**
     *
     * @param username 参数必须与user-service保持一致
     * @return
     */
    @GetMapping("/getByUserName")
    public CommonResult getByUserName(String username){
        return restTemplate.getForObject(userServiceUrl +"/user/getUserByName?username={1}", CommonResult.class, username);
    }

    @GetMapping("/getEntityByUserName")
    public CommonResult getEntityByUserName(String username){
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
