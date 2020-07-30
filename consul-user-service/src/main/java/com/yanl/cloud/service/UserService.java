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
