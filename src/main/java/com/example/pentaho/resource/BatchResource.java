package com.example.pentaho.resource;


import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.service.BigDataService;
import com.example.pentaho.service.FileOutputService;
import com.example.pentaho.service.JobService;
import com.example.pentaho.service.SingleTrackQueryService;
import com.example.pentaho.utils.FileUtils;
import com.example.pentaho.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api/batchForm")
@SecurityRequirement(name = "Authorization")
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

    @Autowired
    private BigDataService bigDataService;


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


    /***
     * 批次查詢(call pentaho job & sftp file)、批次軌跡查詢
     * @param Id
     * @param originalFileId
     * @param formName
     * @param formBuilderId
     * @param formBuilderOrgId
     * @param file
     * @return
     * @throws IOException
     */
    @Operation(description = "批次查詢(call pentaho & sftp file)、批次軌跡查詢",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "jwt token,body附帶userInfo={\"Id\":\"673f7eec-8ae5-4e79-ad3a-42029eedf742\",\"orgId\":\"ADMIN\"}",
//                            required = true,
                            schema = @Schema(type = "string"))}
    ,
        responses = {
                @ApiResponse(responseCode = "200", description = "呼叫JOB成功、異動批次開始查詢",
                        content = @Content(schema = @Schema(implementation = String.class), examples= @ExampleObject(value = "CALL_JOB_SUCESS"))),
                @ApiResponse(responseCode = "500", description = "呼叫JOB失敗、異動批次查詢失敗",
                        content = @Content(schema = @Schema(implementation = String.class), examples= @ExampleObject(value = "CALL_JOB_ERROR"))),
        }
)
    @PostMapping(value="/excuteETLJob",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Authorized(keyName = "SHENG")
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
        @RequestPart("file")
        MultipartFile file) throws IOException {

        if (file == null) {
            return new ResponseEntity<>("檔案為空",HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            return new ResponseEntity<>("檔案須為CSV",HttpStatus.INTERNAL_SERVER_ERROR);

        }

        /**依據申請單號進行分流**/
        /****************************************異動軌跡批次****************************************/
        if(formName.startsWith("BC")){
            String fileContent = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.info("fileContent:{}",fileContent);

            CompletableFuture.runAsync(() -> {
                try {
//                  Thread.sleep(10000); //可以測試是否同步
                    queryBatchTrackAsync(Id, originalFileId, formName,fileContent);
                } catch (IOException e) {
                    log.info("e:{}",e.toString());
                    throw new MoiException("異動批次查詢失敗");
                }
            });
            return new ResponseEntity<>("異動批次開始查詢", HttpStatus.OK);
        }

        /****************************************批次查詢****************************************/
        /***建立批次查詢物件**/
        /*filePath:../yyyyMMdd/formBuilderOrgId*/
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateStamp = dateFormat.format(new Date());
        JobParams jobParams = new JobParams(formName, Id, originalFileId, formBuilderId, formBuilderOrgId,dateStamp);
        log.info("jobParams:{}",jobParams);
        /*result:response的內容*/
        Map<String, String> result = new HashMap<>();
        jobService.sftpUploadAndExecuteTrans(file,jobParams,result);
            if("CALL_JOB_SUCESS".equals(result.get("status"))){
                return new ResponseEntity<>(result.get("status"),HttpStatus.OK);
            }
            return new ResponseEntity<>(result.get("status"),HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Async
    public CompletableFuture<Void> queryBatchTrackAsync(String Id, String originalFileId, String formName, String fileContent) throws IOException {
        queryBatchTrack(Id, originalFileId, formName, fileContent);
        return CompletableFuture.completedFuture(null);
    }

    public void queryBatchTrack(String Id,String originalFileId,String formName,String fileContent) throws IOException {
        /*default:processedCount=0,status=SYS_FAILED*/
        SingleBatchQueryParams singleBatchQueryParams = new SingleBatchQueryParams(Id, originalFileId, "0", "SYS_FAILED", formName);
        singleQueryTrackService.queryBatchTrack(fileContent, singleBatchQueryParams);
    }

    @PostMapping(path = "/finished")
    @Authorized(keyName = "AP")
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
     * 大量查詢回傳log就好
     * @param formName
     * @return
     * @throws IOException
     */
    @Operation(description = "大量查詢",
            parameters = {  @Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "聖森私鑰加密 jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                    schema = @Schema(type = "string"))
            })
    @PostMapping("/bigdata-finished")
    @Authorized(keyName = "SHENG")
    public ResponseEntity<Integer> BigDataFinished(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "表單編號",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "BQ202401010001")
                    )
            )
            @RequestBody String formName) throws IOException {
        log.info("formName:{}",formName);
        return new ResponseEntity<>(bigDataService.findLog(formName),HttpStatus.OK);
    }



    @Operation(description = "大量查詢指定欄位",
            parameters = {  @Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "聖森私鑰加密 jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                    schema = @Schema(type = "string"))
            })
    @PostMapping("/bigdata-send-conditions")
    @Authorized(keyName = "SHENG")
    public ResponseEntity sendConditionsToBigData(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "(1) formId + formBuilder + 指定欄位 json ,\n"+
                                  "(2) formId,formBuilder 不得為空，且至少指定一個欄位 ,\n"+
                                  "(3) 指定欄位格式:欄位名為key,Y為value \n",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = BigDataParams.class)
                         ,examples = @ExampleObject(value = "{\n" +
                            " \"formId\": \"BQ202401010001\",\n" +
                            " \"userId\": \"formBuilder\",\n" +
                            "  \"alley\": \"Y\", \n" +
                            "  \"village\": \"Y\" \n" +
                            "}")
                    )
            )
            @RequestBody BigDataParams bigDataParams
            ){
        log.info("大量查詢條件:{}",bigDataParams);
        if(StringUtils.isNullOrEmpty(bigDataParams.getUserId()) || StringUtils.isNullOrEmpty(bigDataParams.getFormId())){
            return new ResponseEntity("表單編號、申請者Id皆不得為空",HttpStatus.FORBIDDEN);
        }

        boolean result = isValid(bigDataParams);
        if(!result){
            return new ResponseEntity("至少指定一欄位",HttpStatus.FORBIDDEN);
        }

        result = bigDataService.saveConditions(bigDataParams);

        return result ? new ResponseEntity("成功",HttpStatus.OK):new ResponseEntity("發生錯誤",HttpStatus.INTERNAL_SERVER_ERROR);
    }




    /**
     * 確認至少有指定一個欄位
     * @param bigDataParams
     * @return
     */
    private boolean isValid(BigDataParams bigDataParams){
        boolean result = false;
        try {
            Method[] methods = BigDataParams.class.getMethods();
            List<Method> getterMethods = Arrays.stream(methods).filter(m -> m.getName().startsWith("get")).toList();
            for (Method m : getterMethods) {
                log.info("method:{}",m.getName());
                Object value = m.invoke(bigDataParams);
                if("Y".equals(value)){
                  result = true;
                }
            }
        }catch (Exception e){
            log.info("e:{}",e.toString());
        }
        return result;
    }

    @GetMapping("/simple-job")
    @Profile("dev")
    public void simpleJob(){
        jobService.simpleJob();
    }

}
