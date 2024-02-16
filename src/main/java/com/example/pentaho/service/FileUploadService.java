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



    public boolean sftpUpload(MultipartFile file,String[] targetDirs,String fileName){
        log.info("receiveDir:{}",targetDirs[0]);
        log.info("sendDir:{}",targetDirs[1]);
        log.info("fileName:{}",fileName);
        Boolean finishUpload = false;
        try {
            sftpUtils.connect();
            sftpUtils.uploadFile(targetDirs[0], file, fileName);
            finishUpload =sftpUtils.listFiles(targetDirs[0], fileName);
            if(finishUpload){
                sftpUtils.createDir(targetDirs[1]);
            }
        }catch (Exception e){
            log.info("e:{}",e);
        }
        sftpUtils.disconnect();
        return finishUpload;
    }

}

