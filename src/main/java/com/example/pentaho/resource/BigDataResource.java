package com.example.pentaho.resource;

import com.example.pentaho.component.BigDataParams;
import com.example.pentaho.component.Directory;
import com.example.pentaho.component.IbdTbAddrStatisticsOverallDev;
import com.example.pentaho.repository.IbdTbAddrStatisticsOverallDevRepository;
import com.example.pentaho.service.FileOutputService;
import com.example.pentaho.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/bigdata")
public class BigDataResource {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private FileOutputService fileOutputService;

    @Autowired
    private IbdTbAddrStatisticsOverallDevRepository ibdTbAddrStatisticsOverallDevRepository;

    @Autowired
    private Directory directories;



    @PostMapping("/upload")
    public ResponseEntity<Integer> sftpUploadBigDataFile(@RequestPart("uploadFile") MultipartFile file, @RequestPart("batchFormId") String batchFormId) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "檔案為空");
        }
        String fileName = batchFormId + ".csv";
        /**
         * /home/addr/big_data/receive/
         * /home/addr/big_data/send/
         *
         * **/
        String[] targetDirs = {directories.getBigDataReceiveFileDir(), directories.getBigDataSendFileDir()};
        boolean sftpUpload = fileUploadService.sftpUpload(file, targetDirs, fileName);
        if (sftpUpload) {
            return new ResponseEntity<Integer>(200, HttpStatus.OK);
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "檔案為空");
    }

    @GetMapping("/testDB")
    public ResponseEntity<List<IbdTbAddrStatisticsOverallDev>> testDB() {
        return ResponseEntity.ok(ibdTbAddrStatisticsOverallDevRepository.findAll());
    }


    /***
     * 改回傳log就好
     * @param batchId
     * @return
     * @throws IOException
     */
    @PostMapping("/finished")
    public ResponseEntity<List<IbdTbAddrStatisticsOverallDev>> sftpDownloadBigDataFile(@RequestBody String batchId) throws IOException {
     return new ResponseEntity<>(fileOutputService.findLog(batchId),HttpStatus.OK);
    }

}
