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
