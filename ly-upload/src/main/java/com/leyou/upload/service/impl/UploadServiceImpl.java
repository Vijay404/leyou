package com.leyou.upload.service.impl;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.domain.ThumbImageConfig;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.upload.config.UploadProperties;
import com.leyou.upload.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@EnableConfigurationProperties(UploadProperties.class)
public class UploadServiceImpl implements UploadService {

    @Autowired
    private FastFileStorageClient storageClient;

    @Autowired
//    private ThumbImageConfig thumbImageConfig;
    private UploadProperties props;

//    private final List<String> ALLOW_TYPES = Arrays.asList("image/jpeg","image/jpg","image/png","image/bmp");

    @Override
    public String uploadImage(MultipartFile file) {
        try {
            // 校验文件
            String contentType = file.getContentType();
            if(!props.getAllowTypes().contains(contentType)){
                throw new LyException(LyRespStatus.INVALID_FILE_TYPE);
            }
            // 校验文件内容，读取文件
            BufferedImage image = ImageIO.read(file.getInputStream());// 若不是图片，则会返回空
            if(image == null){
                throw new LyException(LyRespStatus.INVALID_FILE_TYPE);
            }

            // 上传到fastdfs上
            // file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")+1);
            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(),".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), extension, null);
            // 返回路径

            return props.getBaseUrl()+storePath.getFullPath();
        } catch (IOException e) {
            // 上传失败
            log.error("[文件上传] 上传文件失败！",e);
            throw new LyException(LyRespStatus.UPLOAD_FILE_ERROR);
        }

    }
}
