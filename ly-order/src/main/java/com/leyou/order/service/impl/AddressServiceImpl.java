package com.leyou.order.service.impl;

import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.common.pojo.UserInfo;
import com.leyou.common.utils.CookieUtils;
import com.leyou.order.interceptor.UserInterceptor;
import com.leyou.order.mapper.UserAddressMapper;
import com.leyou.order.pojo.UserAddress;
import com.leyou.order.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

    @Autowired
    private UserAddressMapper addressMapper;

    /**
     * 查询用户的收货地址信息
     * @return
     */
    @Override
    public List<UserAddress> queryAddress() {
        // 获取当前已登录的用户
        UserInfo user = UserInterceptor.getUser();
        // 根据用户id查询地址信息
        UserAddress address = new UserAddress();
        address.setUserId(user.getId());
        List<UserAddress> addressList = addressMapper.select(address);
        return addressList;
    }

    /**
     * 新增收货地址
     * @param address
     */
    @Override
    public void createAddress(UserAddress address) {
        // 获取当前已登录的用户
        UserInfo user = UserInterceptor.getUser();
        // 封装地址信息
        address.setUserId(user.getId());
        address.setId(null);
        // 判断是否为默认地址
        if (address.getIsDefault()) {
            // 设置为默认地址，将数据库内原有的默认改为false即可
            UserAddress userAddress = new UserAddress();
            userAddress.setIsDefault(false);
            Example example = new Example(UserAddress.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("userId", user.getId());
            int i = addressMapper.updateByExampleSelective(userAddress, example);
            if(i == 0){
                throw new LyException(LyRespStatus.ADDRESS_SAVE_ERROR);
            }
        }
        // 新增地址
        int i = addressMapper.insertSelective(address);
        if(i == 0){
            throw new LyException(LyRespStatus.ADDRESS_SAVE_ERROR);
        }
    }
}
