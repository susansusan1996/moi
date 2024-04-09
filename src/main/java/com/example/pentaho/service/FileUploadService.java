package com.example.pentaho.service;

import com.example.pentaho.utils.custom.Sftp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;



@Service
public class FileUploadService {

    private final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    @Autowired
    private Sftp sftp;


    public boolean sftpUpload(MultipartFile file, String[] targetDirs, String fileName){
        log.info("receiveDir:{}",targetDirs[0]);
        log.info("sendDir:{}",targetDirs[1]);
        log.info("fileName:{}",fileName);
        Boolean finishUpload = false;
        try {
            sftp.connect();
            sftp.uploadFile(targetDirs[0], file, fileName);
            finishUpload =sftp.listFiles(targetDirs[0], fileName);
            if(finishUpload){
                sftp.createDir(targetDirs[1]);
            }
        }catch (Exception e){
            log.info("e:{}",e);
        }
        sftp.disconnect();
        return finishUpload;
    }

}

