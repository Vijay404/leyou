package com.leyou.user.service;


import com.leyou.user.pojo.User;

public interface UserService {

    Boolean checkData(String data, Integer type);

    void verifyCode(String phone);

    void registerUser(User user, String code);

    User queryUser(String username, String password);
}
