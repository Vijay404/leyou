package com.leyou.order.service;

import com.leyou.order.pojo.UserAddress;

import java.util.List;

public interface AddressService {
    List<UserAddress> queryAddress();

    void createAddress(UserAddress address);
}
