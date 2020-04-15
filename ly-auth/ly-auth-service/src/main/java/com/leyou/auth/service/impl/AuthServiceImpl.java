package com.leyou.auth.service.impl;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.service.AuthService;
import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.common.pojo.UserInfo;
import com.leyou.common.utils.JwtUtils;
import com.leyou.user.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties jwtProperties;
    /**
     * 使用jwt校验是否已登录
     * @param username
     * @param password
     * @return
     */
    @Override
    public String accredit(String username, String password) {
        // 1.根据用户名和密码查询数据库
        User user = userClient.queryUser(username, password);
        // 2.判断user是否存在
        if(user == null){
            throw new LyException(LyRespStatus.USER_NOT_EXIST);
        }
        // 3.JwtUtils生成Jwt类型的token
        try {
            UserInfo userInfo = new UserInfo();
            userInfo.setUsername(username);
            userInfo.setId(user.getId());
            return JwtUtils.generateToken(userInfo, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
        } catch (Exception e) {
            log.error("[授权中心] 生成jwt失败！用户：{}"+ username + e);
        }
        return null;
    }

}
