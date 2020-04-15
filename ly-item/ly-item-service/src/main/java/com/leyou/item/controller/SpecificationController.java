package com.leyou.item.controller;

import com.leyou.item.service.SpecificationService;
import item.pojo.SpecGroup;
import item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/spec")
public class SpecificationController {

    @Autowired
    private SpecificationService specificationService;

    /**
     * 根据分类id查询规格组信息
     * @param cid
     * @return
     */
    @GetMapping("/groups/{cid}")
    public ResponseEntity<List<SpecGroup>> queryGroupsByCid(@PathVariable("cid")Long cid){
        return ResponseEntity.ok(specificationService.queryGroupsByCid(cid));
    }

    /**
     * 根据规格组id修改规格组信息
     * @param specGroup
     * @return
     */
    @PutMapping("/group")
    public ResponseEntity<Void> updateGroup(@RequestBody SpecGroup specGroup){
        specificationService.updateGroup(specGroup);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 删除规格组信息
     * @param gid
     * @return
     */
    @DeleteMapping("/group/{gid}")
    public ResponseEntity<Void> deleteGroup(@PathVariable("gid") Long gid){
        specificationService.deleteGroup(gid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/group")
    public ResponseEntity<Void> saveGroup(@RequestBody SpecGroup specGroup){
        specificationService.saveGroup(specGroup);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据组id查询规格参数
     * @param gid
     * @return
     */
    @GetMapping("/params")
    public ResponseEntity<List<SpecParam>> queryParamsByGid(
            @RequestParam(value = "gid",required = false) Long gid,
            @RequestParam(value = "cid",required = false) Long cid,
            @RequestParam(value = "searching",required = false) Boolean searching
            ) {
        return ResponseEntity.ok(specificationService.queryParamsByGid(gid,cid,searching));
    }

    /**
     * 保存规格参数
     * @param specParam
     * @return
     */
    @PostMapping("/param")
    public ResponseEntity<Void> saveParam(SpecParam specParam){
        specificationService.saveParam(specParam);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据参数id删除规格参数
     * @param pid
     * @return
     */
    @DeleteMapping("/param/{pid}")
    public ResponseEntity<Void> deleteParam(@PathVariable("pid") Long pid) {
        specificationService.deleteParam(pid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/param")
    public ResponseEntity<Void> updateParam(SpecParam specParam){
        specificationService.updateParam(specParam);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 根据分类id查询规格组信息
     * @param cid
     * @return
     */
    @GetMapping("/group")
    ResponseEntity<List<SpecGroup>> queryGroupListByCid(@RequestParam("cid")Long cid){
        return ResponseEntity.ok(specificationService.queryGroupListByCid(cid));
    }

    /**
     * 根据规格参数id集合查询规格参数
     * @param ids
     * @return
     */
    @GetMapping("/param/ids")
    public ResponseEntity<List<SpecParam>> queryParamByIds(@RequestParam("ids") List<Long> ids){
        return ResponseEntity.ok(specificationService.queryParamByIds(ids));
    }
}
