package com.leyou.page.service;

import java.util.Map;

public interface PageService {

    Map<String, Object> loadAttrs(Long spuId);

    void createHtml(Long spuId);

    void deleteHtml(Long spuId);
}
