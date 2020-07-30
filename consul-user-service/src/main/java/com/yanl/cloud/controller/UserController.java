package com.yanl.cloud.controller;

import com.yanl.cloud.domain.CommonResult;
import com.yanl.cloud.domain.User;
import com.yanl.cloud.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public CommonResult create(@RequestBody User user){
        userService.create(user);
        return new CommonResult("操作成功", 200);
    }

    @GetMapping("/{id}")
    public CommonResult<User> getUser(@PathVariable Long id){
        User user = userService.getUser(id);
        log.info("根据id获取用户信息，用户名称为: {}", user.getUsername());
        return new CommonResult<>(user);
    }

    @GetMapping("/getUserByIds")
    public CommonResult<List<User>> getUserByIds(@RequestParam List<Long> ids){
        List<User> findUserList = userService.getUserByIds(ids);
        log.info("根据ids获取用户组，用户组为：{}", findUserList);
        return new CommonResult<>(findUserList);
    }

    @GetMapping("/getUserByName")
    public CommonResult<User> getUserByName(@RequestParam String username){
        User user = userService.getByUserName(username);
        log.info("根据用户名称获取用户，用户信息为：{}", user);
        return new CommonResult<>(user);
    }

    @PostMapping("/update")
    public CommonResult update(@RequestBody User user){
        userService.update(user);
        return new CommonResult("操作成功", 200);
    }

    @PostMapping("/delete/{id}")
    public CommonResult delete(@PathVariable Long id){
        userService.delete(id);
        return new CommonResult("操作成功", 200);
    }

}
