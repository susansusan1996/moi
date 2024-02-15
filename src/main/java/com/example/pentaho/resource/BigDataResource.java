package com.example.pentaho.resource;

import com.example.pentaho.component.Directory;
import com.example.pentaho.entity.IbdTbAddrStatisticsOverallDev;
import com.example.pentaho.repository.IbdTbAddrStatisticsOverallDevRepository;
import com.example.pentaho.service.FileUploadService;
import com.example.pentaho.utils.SFTPUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
//@RequestMapping("/api/bigdata")
public class BigDataResource {

    @Autowired
    private FileUploadService fileUploadService;


    @Autowired
    private IbdTbAddrStatisticsOverallDevRepository ibdTbAddrStatisticsOverallDevRepository;

    @Autowired
    private Directory directories;



    public ResponseEntity<Integer> sftpUploadBigDataFile(@RequestPart("uploadFile")MultipartFile file, @RequestPart("batchFormId") String batchFormId){
        if(file == null){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"檔案為空");
        }
        String fileName = batchFormId+".csv";
        boolean sftpUpload = fileUploadService.sftpUpload(file, directories.getSendFileDir(), fileName);
        if(sftpUpload){
        return new ResponseEntity<Integer>(200,HttpStatus.OK);
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"檔案為空");
    }

    @GetMapping("/testDB")
    public  void  testDB(){
//        IbdTbAddrStatisticsOverallDev ibdTbAddrStatisticsOverallDev = ibdTbAddrStatisticsOverallDevRepository.findIbdTbAddrStatisticsOverallDevById(1).get();
//        System.out.println(ibdTbAddrStatisticsOverallDev.toString());
    }

//    public ResponseEntity<Integer> sftpDownloadBigDataFile(String batchFormId){
//
//        String fileName = batchFormId+".csv";
//        boolean sftpUpload = fileUploadService.sftpUpload(file, directories.getSendFileDir(), fileName);
//        if(sftpUpload){
//            return new ResponseEntity<Integer>(200,HttpStatus.OK);
//        }
//        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"檔案為空");
//    }
}
