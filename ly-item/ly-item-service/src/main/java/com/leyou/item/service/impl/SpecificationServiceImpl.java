package com.leyou.item.service.impl;

import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.service.SpecificationService;
import item.pojo.SpecGroup;
import item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpecificationServiceImpl implements SpecificationService {

    @Autowired
    private SpecGroupMapper groupMapper;

    @Autowired
    private SpecParamMapper paramMapper;

    /**
     * 根据分类id查询所有的规格组信息
     * @param cid
     * @return
     */
    @Override
    public List<SpecGroup> queryGroupsByCid(Long cid) {
        // 查询条件
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        // 查询规格组信息
        List<SpecGroup> list = groupMapper.select(specGroup);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(LyRespStatus.SPEC_GROUP_NOT_FOUND);
        }
        return list;
    }

    /**
     * 根据组id查询规格参数
     * @param gid
     * @param cid
     * @param searching
     * @return
     */
    @Override
    public List<SpecParam> queryParamsByGid(Long gid, Long cid, Boolean searching) {
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setSearching(searching);

        List<SpecParam> list = paramMapper.select(specParam);// 根据非空字段查询
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(LyRespStatus.SPEC_GROUP_NOT_FOUND);
        }
        return list;
    }

    /**
     * 根据规格组id修改组信息
     * @param specGroup
     */
    @Override
    public void updateGroup(SpecGroup specGroup) {
        int count = groupMapper.updateByPrimaryKey(specGroup);
        if(count != 1){
            throw new LyException(LyRespStatus.SPEC_GROUP_UPDATE_ERROR);
        }
    }

    /**
     * 根据id删除规格组
     * @param gid
     */
    @Override
    public void deleteGroup(Long gid) {
        int count = groupMapper.deleteByPrimaryKey(gid);
        if(count != 1){
            throw new LyException(LyRespStatus.SPEC_GROUP_DELETE_ERROR);
        }
    }

    /**
     * 保存规格组信息
     * @param specGroup
     */
    @Override
    public void saveGroup(SpecGroup specGroup) {
        int count = groupMapper.insert(specGroup);
        if(count != 1){
            throw new LyException(LyRespStatus.SPEC_GROUP_SAVE_ERROR);
        }
    }

    /**
     * 保存规格参数
     * @param specParam
     */
    @Override
    public void saveParam(SpecParam specParam) {
        int count = paramMapper.insert(specParam);
        if(count != 1){
            throw new LyException(LyRespStatus.SPEC_PARAM_SAVE_ERROR);
        }
    }

    /**
     * 删除规格参数
     * @param pid
     */
    @Override
    public void deleteParam(Long pid) {
        int count = paramMapper.deleteByPrimaryKey(pid);
        if(count != 1){
            throw new LyException(LyRespStatus.SPEC_PARAM_DELETE_ERROR);
        }
    }

    /**
     * 修改规格参数
     * @param specParam
     */
    @Override
    public void updateParam(SpecParam specParam) {
        int count = paramMapper.updateByPrimaryKey(specParam);
        if(count != 1){
            throw new LyException(LyRespStatus.SPEC_PARAM_UPDATE_ERROR);
        }
    }

    /**
     * 根据分类id查询规格组并查询出规格参数
     * @param cid
     * @return
     */
    @Override
    public List<SpecGroup> queryGroupListByCid(Long cid) {
        // 查询出规格参数组
        List<SpecGroup> specGroups = queryGroupsByCid(cid);
        // 查询当前分类下的参数
        List<SpecParam> specParams = queryParamsByGid(null, cid, null);

        // 先将规格参数变成map，map的key是规格组id，value是组下的所有参数
        Map<Long, List<SpecParam>> map = new HashMap<>();
        // 遍历specParams，将其添加进map
        for (SpecParam param : specParams) {
            if(!map.containsKey(param.getGroupId())){
                // 这个组id在map中不存在，新增一个list
                map.put(param.getGroupId(), new ArrayList<>());
            }
            // 将规格参数添加进map(不同的参数，可能有相同的规格组id)
            map.get(param.getGroupId()).add(param);
        }

        // 填充param到group中去，将规格组表和规格参数表一一对应起来
        for (SpecGroup specGroup : specGroups) {
            specGroup.setParams(map.get(specGroup.getId()));
        }
        return specGroups;
    }

    /**
     * 根据参数id集合查询规格参数
     * @param ids
     * @return
     */
    @Override
    public List<SpecParam> queryParamByIds(List<Long> ids) {
        List<SpecParam> params = paramMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(params)){
            throw new LyException(LyRespStatus.SPEC_PARAM_NOT_FOUND);
        }
        return params;
    }
}
