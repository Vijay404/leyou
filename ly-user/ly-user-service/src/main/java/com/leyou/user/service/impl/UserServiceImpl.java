package com.leyou.user.service.impl;

import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.user.utils.CodecUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.config.VerifyCodeProperties;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Transactional
@Service
@EnableConfigurationProperties(VerifyCodeProperties.class)
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private VerifyCodeProperties codeProperties;

    private static final String KEY_PREFIX = "user:verify:phone:";

    @Override
    public Boolean checkData(String data, Integer type) {
        User user = new User();
        // type:1，用户名；2，手机
        switch (type){
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
            default:
                throw new LyException(LyRespStatus.INVALID_USER_DATA_TYPE);
        }
        // 校验数据是否存在
        return userMapper.selectCount(user) == 0; // true表示用户名或者手机号可用
    }

    @Override
    public void verifyCode(String phone) {
        // 生成redis存储验证码的key
        String key = KEY_PREFIX + phone;
        // 发送消息给sms短信服务
        // 生成随机验证码
        String code = NumberUtils.generateCode(codeProperties.getCodeLen());
        Map<String, String> msg = new HashMap<>();
        msg.put("phoneNumber", phone);
        msg.put("code", code);
        amqpTemplate.convertAndSend(codeProperties.getExchange(), codeProperties.getRoutingKey(), msg);

        // 发送成功后，保存验证码到redis，并且设置5分钟自动删除失效
        redisTemplate.opsForValue().set(key, code, codeProperties.getTimeOut(), TimeUnit.MINUTES);
    }

    /**
     * 注册用户
     * @param user
     * @param code
     */
    @Override
    public void registerUser(User user, String code) {
        // 校验验证码
        String lastCode = redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if("".equals(lastCode) || !code.equals(lastCode)){
            throw new LyException(LyRespStatus.INVALID_CODE);
        }
        // 生成盐salt
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        // 加密密码
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));
        user.setId(null);
        user.setCreated(new Date());
        // 新增用户
        int count = userMapper.insertSelective(user);
        if(count == 0){
            throw new LyException(LyRespStatus.USER_REGISTER_ERROR);
        }
        // 注册成功，删除redis中的验证码
        redisTemplate.delete(KEY_PREFIX + user.getPhone());
    }

    /**
     * 根据用户名和密码查询用户
     * @param username
     * @param password
     * @return
     */
    @Override
    public User queryUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        User dbUser = userMapper.selectOne(user);
        if(dbUser == null){
            throw new LyException(LyRespStatus.USER_NOT_EXIST);
        }
        // 验证加密后的密码
        String passwordAfter = CodecUtils.md5Hex(password,  dbUser.getSalt());
        if(!StringUtils.equals(dbUser.getPassword(), passwordAfter)){
            throw new LyException(LyRespStatus.PASSWORD_MISMATCH);
        }
        return dbUser;
    }


}
