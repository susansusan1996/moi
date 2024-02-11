package com.example.pentaho.service;

import com.example.pentaho.component.Directory;
import com.example.pentaho.component.JobParams;
import com.example.pentaho.utils.SFTPUtils;
import com.example.pentaho.utils.UserContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Service
public class FileUploadService {



    private final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    @Autowired
    private SFTPUtils sftpUtils;


    public boolean sftpUpload(MultipartFile file,String targetDir,String fileName){
        log.info("targetDir:{}",targetDir);
        log.info("fileName:{}",fileName);
        Boolean finishUpload = false;
        try {
            sftpUtils.uploadFile(targetDir, file, fileName);
            return sftpUtils.listFiles(targetDir, fileName);
        }catch (Exception e){
            log.info("e:{}",e);
            return finishUpload;
        }
    }

}

