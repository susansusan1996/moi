package com.example.pentaho.resource;


import com.example.pentaho.model.JobParams;
import com.example.pentaho.service.JobService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


@RequestMapping("/api/kettle")
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
    @PostMapping("/test")
    public ResponseEntity<String> test() throws IOException {
        log.info("sucess");
        return new ResponseEntity<>("成功!", HttpStatus.OK);
    }

    /**
     * @param jobParams
     * @return
     */
    @PostMapping("/excute")
    public ResponseEntity<Integer> excuteJob(@RequestBody JobParams jobParams) throws IOException {
        log.info("jobName:{}", jobParams.getJobName());
        Integer responseCode = jobService.excuteJob(jobParams);
        if (responseCode == 200) {
            return new ResponseEntity<>(responseCode, HttpStatus.OK);
        }
        return new ResponseEntity<>(responseCode, HttpStatus.FORBIDDEN);
    }

}
