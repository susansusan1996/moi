package com.example.pentaho.resource;


import com.example.pentaho.component.Directory;
import com.example.pentaho.component.JobParams;
import com.example.pentaho.component.SingleBatchQueryParams;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.service.FileOutputService;
import com.example.pentaho.service.JobService;
import com.example.pentaho.service.SingleTrackQueryService;
import com.example.pentaho.utils.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcraft.jsch.SftpException;
import io.swagger.v3.oas.annotations.Hidden;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api/batchForm")
public class BatchResource {

    private static Logger log = LoggerFactory.getLogger(BatchResource.class);

    @Autowired
    private JobService jobService;


    @Autowired
    private SingleTrackQueryService singleQueryTrackService;

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
    @Hidden
    public void getFile(
            @RequestPart(name = "etlOutPutFile") MultipartFile multiFile, @RequestBody JobParams jobParams) throws IOException {
        String filename = multiFile.getResource().getFilename();
        FileUtils.saveFile(directories.getMockEtlSaveFileDirPrefix(), multiFile, filename);
    }


    @Operation(description = "批次查詢(call pentaho & sftp file)、異動批次查詢",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "jwt token,body附帶userInfo={\"Id\":\"673f7eec-8ae5-4e79-ad3a-42029eedf742\",\"orgId\":\"ADMIN\"}",
                            required = true,
                            schema = @Schema(type = "string"))}
    ,
        responses = {
                @ApiResponse(responseCode = "200", description = "呼叫JOB成功、異動批次開始查詢",
                        content = @Content(schema = @Schema(implementation = String.class), examples= @ExampleObject(value = "CALL_JOB_SUCESS"))),
                @ApiResponse(responseCode = "500", description = "呼叫JOB失敗、異動批次查詢失敗",
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
                description ="申請單號 ; 批次地址編碼(BS)、批次異動軌跡(BC) " ,
                required = true,
                schema = @Schema(type = "string"))
        @RequestParam("formName") String formName,
        @Parameter(
                description ="表單申請者Id" ,
                required = true,
                schema = @Schema(type = "string"))
        @RequestParam("formBuilderId") String formBuilderId,
        @Parameter(
                description ="表單申請者部門Id" ,
                required = true,
                schema = @Schema(type = "string"))
        @RequestParam("formBuilderOrgId") String formBuilderOrgId,
        @Parameter(
                description = "使用者上傳的CSV檔",
                required = true
        )
        @RequestParam("file") MultipartFile file) throws IOException {

        if (file == null) {
            return new ResponseEntity<>("檔案為空",HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            return new ResponseEntity<>("檔案須為CSV",HttpStatus.INTERNAL_SERVER_ERROR);

        }

        /**依據申請單號進行分流**/
        /****************************************異動軌跡批次****************************************/
        if(formName.startsWith("BC")){
            CompletableFuture.runAsync(() -> {
                try {
//                  Thread.sleep(10000); //可以測試是否同步
                    queryBatchTrackAsync(Id, originalFileId, formName, file.getInputStream());
                } catch (IOException e) {
                    log.info("e:{}",e.toString());
                    throw new MoiException("異動批次查詢失敗");
                }
            });
            return new ResponseEntity<>("異動批次開始查詢", HttpStatus.OK);
        }

        /****************************************批次查詢****************************************/
        /***建立批次查詢物件**/
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateStamp = dateFormat.format(new Date());
        JobParams jobParams = new JobParams(formName, Id, originalFileId, formBuilderId, formBuilderOrgId,dateStamp);
        log.info("jobParams:{}",jobParams);
        String status = jobService.sftpUploadAndExecuteTrans(file, jobParams);
        if(!"CALL_JOB_SUCESS".equals(status)){
            return new ResponseEntity<>(status,HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(status,HttpStatus.OK);
    }


    @Async
    public CompletableFuture<Void> queryBatchTrackAsync(String Id, String originalFileId, String formName, InputStream inputStream) throws IOException {
        queryBatchTrack(Id, originalFileId, formName, inputStream);
        return CompletableFuture.completedFuture(null);
    }

    public void queryBatchTrack(String Id,String originalFileId,String formName, InputStream inputStream) throws IOException {
        SingleBatchQueryParams singleBatchQueryParams = new SingleBatchQueryParams(Id, originalFileId, "0", "SYS_FAILED", formName);
        singleQueryTrackService.queryBatchTrack(inputStream, singleBatchQueryParams);
    }

    @PostMapping(path = "/finished")
    public void sftpDownloadAndSend(@RequestBody String requestBody) throws IOException, SftpException, URISyntaxException {
        log.info("requestBody:{}",requestBody);
        /**解析requestBody中的參數**/
        ObjectNode jsonObject = objectMapper.createObjectNode();
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
        fileOutputService.sftpDownloadBatchFormFileAndSend(jobParams1);
    }


    /***
     * 改回傳log就好
     * @param formName
     * @return
     * @throws IOException
     */
    @Operation(description = "大量查詢",
            parameters = {  @Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "聖森私鑰加密 jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                    required = true,
                    schema = @Schema(type = "string"))
            })
    @PostMapping("/bigdata-finished")
    public ResponseEntity<Integer> BigDataFinished(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "表單編號",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "BT202401010001")
                    )
            )
            @RequestBody String formName) throws IOException {
        log.info("formName:{}",formName);
        return new ResponseEntity<>(fileOutputService.findLog(formName),HttpStatus.OK);
    }

}
