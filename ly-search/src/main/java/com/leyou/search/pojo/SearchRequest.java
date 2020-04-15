package com.leyou.search.pojo;

import java.util.Map;

public class SearchRequest {
    private String key;// 搜索字段
    private Integer page;// 当前页
    private Map<String, String> filter; // 过滤项
    private String sortBy;// 排序字段
    private Boolean descending;// 是否降序
    private static final int DEFAULT_SIZE = 20;// 每页默认大小，不从页面接收，而是固定大小
    private static final int DEFAULT_PAGE = 1;// 默认页码

    public Map<String, String> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, String> filter) {
        this.filter = filter;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Boolean getDescending() {
        return descending;
    }

    public void setDescending(Boolean descending) {
        this.descending = descending;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getPage() {
        if(this.page == null){
            return DEFAULT_PAGE;
        }
        return Math.max(DEFAULT_PAGE, page);// 获取页码校验，不可小于1
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize(){
        return DEFAULT_SIZE;
    }
}
