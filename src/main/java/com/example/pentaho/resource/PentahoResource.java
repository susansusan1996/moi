package com.example.pentaho.resource;


import com.example.pentaho.component.JobParams;
import com.example.pentaho.component.User;
import com.example.pentaho.service.JobService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@RequestMapping("/api/batchForm")
@RestController
public class PentahoResource {

    private static Logger log = LoggerFactory.getLogger(PentahoResource.class);

    @Autowired
    private JobService jobService;


    @PostMapping("/start-job")
    public ResponseEntity<String> startJob(@RequestBody JobParams jobParams, HttpServletRequest request) throws IOException {
        log.info("jobName:{}", jobParams.getJobName());
        Integer responseCode = jobService.startJob(jobParams, "start-job");
        if (responseCode == 200) {
            return new ResponseEntity<>("sucess", HttpStatus.OK);
        }
        return new ResponseEntity<>("fail", HttpStatus.FORBIDDEN);
    }

    /**
     * @param jobParams
     * @return
     * @throws IOException
     */
    @PostMapping("/sniff-step")
    public ResponseEntity<Integer> sniffStep(@RequestBody JobParams jobParams) throws IOException {
        log.info("jobName:{}", jobParams.getJobName());
        jobService.sniffStep(jobParams);
        return new ResponseEntity<>(200, HttpStatus.OK);
    }

    /**
     * 測試用
     *
     * @return
     * @throws IOException
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() throws IOException {
        UserContextUtils.getUserHolder();
        return new ResponseEntity<>("成功", HttpStatus.OK);
    }

    /**
     * @param jobParams
     * @return
     */
    @PostMapping("/excuteETLJob")
    public ResponseEntity<Integer> excuteJob(@RequestBody JobParams jobParams) throws IOException {
        log.info("jobName:{}", jobParams.getJobName());
        Integer responseCode = jobService.excuteJob(jobParams);
        if (responseCode == 200) {
            return new ResponseEntity<>(responseCode, HttpStatus.OK);
        }
        return new ResponseEntity<>(responseCode, HttpStatus.FORBIDDEN);
    }

    /**
     * 使用者傳送想比對的批次檔案到指定位置
     * @param jobParams
     * @return
     */
    @PostMapping("/excuteETLTrans")
    public ResponseEntity<Integer> excuteTrans(@RequestBody JobParams jobParams) throws IOException {
        log.info("jobName:{}",jobParams.getJobName());
        Integer responseCode = jobService.excuteTrans(jobParams);
        if(responseCode== 200){
            return new ResponseEntity<>(responseCode,HttpStatus.OK);
        }
        return new ResponseEntity<>(responseCode,HttpStatus.FORBIDDEN);
    }

    @PostMapping("/downloadFile")
    public  ResponseEntity<String> downloadFile(){
        try {
            // TODO: 2024/1/8  fileUrl、savePath要再修正，如果不是會變動的，可以寫在.yml裡(??
            String fileUrl = "http://52.33.116.195/data.js";
            String savePath = "/home/ec2-user/downloadFile/data.js";
            jobService.downloadFileFromPentahoServer(fileUrl,savePath);
            return new ResponseEntity<>("sucess",HttpStatus.OK);
        }catch (Exception e){
            log.info("e:{}",e.toString());
            return new ResponseEntity<>("fail",HttpStatus.NOT_FOUND);
        }
    }
}
