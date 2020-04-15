package com.leyou.common.mapper;

import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.additional.insert.InsertListMapper;
import tk.mybatis.mapper.annotation.RegisterMapper;
import tk.mybatis.mapper.common.Mapper;

@RegisterMapper // 生效Mapper注解
public interface BaseMapper<T,Object> extends Mapper<T>, IdListMapper<T,Object>, InsertListMapper<T> {
}
