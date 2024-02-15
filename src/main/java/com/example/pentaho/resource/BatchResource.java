package com.example.pentaho.resource;


import com.example.pentaho.component.Directory;
import com.example.pentaho.component.JobParams;
import com.example.pentaho.service.FileOutputService;
import com.example.pentaho.service.JobService;
import com.example.pentaho.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RequestMapping("/api/batchForm")
@RestController
public class BatchResource {

    private static Logger log = LoggerFactory.getLogger(BatchResource.class);

    @Autowired
    private JobService jobService;

    @Autowired
    private FileOutputService fileOutputService;

    @Autowired
    private Directory directories;


    /**
     * 測試啟動帶有parameter的transformation
     */
    @PostMapping("/excuteETLJob")
    public ResponseEntity<Integer> excuteTransWithParams(@RequestBody JobParams jobParams) throws IOException {
        log.info("ETL作業開始，參數為{}: ", jobParams.toString());
        Integer responseCode = jobService.excuteTransWithParams(jobParams);
        if (responseCode == 200) {
            return new ResponseEntity<>(responseCode, HttpStatus.OK);
        }
        return new ResponseEntity<>(responseCode, HttpStatus.FORBIDDEN);
    }


    /**
     * etl結束，並傳送檔案給聖森
     */
    @PostMapping(path = "/finished")
    public void etlFinishedAndSendFile(@RequestBody JobParams jobParams) throws IOException {
        log.info("ETL回CALL API，參數為{}: ", jobParams.toString());
        fileOutputService.etlFinishedAndSendFile(jobParams);
    }


    /**
     * 模擬聖森接收iisi送過去的檔案
     */
    @PostMapping("/getFile")
    public void getFile(
            @RequestPart(name = "etlOutPutFile") MultipartFile multiFile, @RequestBody JobParams jobParams) throws IOException {
        String filename = multiFile.getResource().getFilename();
        FileUtils.saveFile(directories.getMockEtlSaveFileDirPrefix(), multiFile, filename);
    }


}
