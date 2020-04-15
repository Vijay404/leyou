package com.leyou.seckill.interceptor;

import com.leyou.common.pojo.UserInfo;
import com.leyou.seckill.annotation.AccessForSeckill;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class SeckillRequestInterceptor extends HandlerInterceptorAdapter {

    private static final String KEY_FOR_REQUEST_COUNTS = "seckill_";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod){
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            AccessForSeckill annotation = handlerMethod.getMethodAnnotation(AccessForSeckill.class);
            if(annotation == null){ // 没有获取到注解，不需要做任何操作
                return true;
            }

            int seconds = annotation.seconds();
            int requestCounts = annotation.requestCounts();
            boolean login = annotation.login();
            UserInfo user = UserInterceptor.getUser();
            if(login){
                // 判断用户是否已登录
                if(user == null){ // 未登录，无权限访问
                    String msg = "请登录！";
                    respMsg(response, msg);
                    return false;
                }
            }

            // 从redis中获取请求次数
            String key = KEY_FOR_REQUEST_COUNTS + user.getId();
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            String count = ops.get(key);
            if(StringUtils.isEmpty(count)){
                // 首次请求，新增请求次数为1，设置超时时间
                ops.set(key, "1", seconds, TimeUnit.SECONDS);
            }else if(Integer.valueOf(count) < requestCounts){
                // 请求次数自动增1
                redisTemplate.opsForValue().increment(key, 1);
            }else{
                String msg = "操作过于频繁，请稍后再试！";
                respMsg(response, msg);
            }
        }
        return super.preHandle(request, response, handler);
    }

    private void respMsg(HttpServletResponse response, String msg) throws IOException {
        // 请求次数超过限制
        ServletOutputStream os = response.getOutputStream();
        os.write(msg.getBytes("UTF-8"));
        os.flush();
        os.close();
    }
}
