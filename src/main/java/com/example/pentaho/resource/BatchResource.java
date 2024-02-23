package com.example.pentaho.resource;


import com.example.pentaho.component.Directory;
import com.example.pentaho.component.JobParams;
import com.example.pentaho.component.User;
import com.example.pentaho.service.BatchService;
import com.example.pentaho.service.FileOutputService;
import com.example.pentaho.service.JobService;
import com.example.pentaho.utils.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcraft.jsch.SftpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/batchForm")
public class BatchResource {

    private static Logger log = LoggerFactory.getLogger(BatchResource.class);

    @Autowired
    private JobService jobService;

    @Autowired
    private FileOutputService fileOutputService;


    @Autowired
    private Directory directories;

    private ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 測試啟動帶有parameter的transformation
     */
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

    @Operation(description = "檔案上傳 & 呼叫JOB",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "驗證jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                            required = true,
                            schema = @Schema(type = "string"))}
    ,
        responses = {
                @ApiResponse(responseCode = "200", description = "呼叫JOB成功",
                        content = @Content(schema = @Schema(implementation = String.class), examples= @ExampleObject(value = "CALL_JOB_SUCESS"))),
                @ApiResponse(responseCode = "500", description = "呼叫JOB失敗",
                        content = @Content(schema = @Schema(implementation = String.class), examples= @ExampleObject(value = "CALL_JOB_ERROR"))),
        }
)
    @PostMapping("/excuteETLJob")
    public ResponseEntity<String> sftpUploadAndExecuteTrans(
        @Parameter(
                description ="批次ID" ,
                required = true,
                schema = @Schema(type = "string"))
        @RequestParam("Id") String Id,
        @Parameter(
                description ="原始CSVID" ,
                required = true,
                schema = @Schema(type = "string"))
        @RequestParam("originalFileId") String originalFileId,
        @Parameter(
                description = "使用者上傳的CSV檔",
                required = true
        )
        @RequestParam("file") MultipartFile file) throws IOException {
        if(file == null){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"UPLOAD_ERROR");
        }
        JobParams jobParams = new JobParams(Id,originalFileId);
        log.info("jobParams:{}",jobParams);
        String status = jobService.sftpUploadAndExecuteTrans(file, jobParams);
        if(!"CALL_JOB_SUCESS".equals(status)){
            return new ResponseEntity<>(status,HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(status,HttpStatus.OK);
    }

    @PostMapping(path = "/finished")
    public void sftpDownloadAndSend(@RequestBody String requestBody) throws IOException, SftpException {
        log.info("requestBody:{}",requestBody);
        //解析requestBody中的參數
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonObject = objectMapper.createObjectNode();
        log.info("requestBody:{}",requestBody);
        String[] params = requestBody.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            String key = keyValue[0];
            String value = keyValue.length > 1 ? keyValue[1] : "";
            jsonObject.put(key, value);
        }

        log.info("jsonObject:{}",jsonObject.toString());

        JobParams jobParams1 = objectMapper.readValue(jsonObject.toString(), JobParams.class);
        log.info("jobParams1:{}",jobParams1);
        fileOutputService.sftpDownloadFileAndSend(jobParams1);
    }

}
