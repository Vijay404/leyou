package com.leyou.item.service;

import item.pojo.SpecGroup;
import item.pojo.SpecParam;

import java.util.List;

public interface SpecificationService {
    List<SpecGroup> queryGroupsByCid(Long cid);

    List<SpecParam> queryParamsByGid(Long gid, Long cid, Boolean searching);

    void updateGroup(SpecGroup specGroup);

    void deleteGroup(Long gid);

    void saveGroup(SpecGroup specGroup);

    void saveParam(SpecParam specParam);

    void deleteParam(Long pid);

    void updateParam(SpecParam specParam);

    List<SpecGroup> queryGroupListByCid(Long cid);

    List<SpecParam> queryParamByIds(List<Long> ids);
}
