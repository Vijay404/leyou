package com.leyou.auth.web;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.service.AuthService;
import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.common.pojo.UserInfo;
import com.leyou.common.utils.CookieUtils;
import com.leyou.common.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录校验
     * @param username
     * @param password
     * @param req
     * @param resp
     * @return
     */
    @PostMapping("/accredit")
    public ResponseEntity<Void> accredit(@RequestParam("username") String username,
                                         @RequestParam("password") String password,
                                         HttpServletRequest req,
                                         HttpServletResponse resp) {

        String token = authService.accredit(username, password);
        if(StringUtils.isBlank(token)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CookieUtils.setCookie(req, resp, jwtProperties.getCookieName(),
                token, jwtProperties.getExpire() * 60);
        return ResponseEntity.ok().build();
    }

    /**
     * 根据页面返回的token校验用户是否已登录
     * @param token
     * @return
     */
    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(@CookieValue("LY_TOKEN") String token,
                                           HttpServletRequest req,
                                           HttpServletResponse resp){
        // 解析token
        try {
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
            // 刷新token存在时间，重新生成token并写入cookie
            String newToken = JwtUtils.generateToken(userInfo,
                    jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            // 重新写入cookie，防止用户操作时，token超时
            CookieUtils.setCookie(req, resp, jwtProperties.getCookieName(),
                    newToken, jwtProperties.getExpire() * 60);
            // 已登录，则返回用户信息
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            // token已过期，或者token被篡改
            throw new LyException(LyRespStatus.UNAUTHORIZED);
        }
    }
}
