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
