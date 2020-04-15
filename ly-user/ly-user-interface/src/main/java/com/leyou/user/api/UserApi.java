package com.leyou.user.api;

import com.leyou.user.pojo.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserApi {

    /**
     * 根据用户名及密码校验用户登录
     * @param username
     * @param password
     * @return
     */
    @GetMapping("/query")
    User queryUser(
            @RequestParam("username") String username,
            @RequestParam("password") String password);

}
