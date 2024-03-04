package com.example.pentaho.resource;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.SingleBatchQueryParams;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.service.SingleQueryService;
import com.example.pentaho.utils.AddressParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/singlequery/")
public class SingleQueryResouce {

    private static Logger log = LoggerFactory.getLogger(SingleQueryResouce.class);

    @Autowired
    private SingleQueryService singleQueryService;

    @Autowired
    private AddressParser addressParser;


    /**
     * 拆分地址
     */
    @GetMapping("/query-address")
    public ResponseEntity<Address> queryData(@RequestBody SingleQueryDTO singleQueryDTO) {
//        return ResponseEntity.ok(new Address(singleQueryDTO.getOriginalAddress()));
        return ResponseEntity.ok(addressParser.parseAddress(singleQueryDTO.getOriginalAddress(),null));

    }

    /**
     * 找出地址相對應的json資訊
     */
    @Operation(description = "單筆查詢",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "驗證jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                            required = true,
                            schema = @Schema(type = "string"))}
    )
    @PostMapping("/query-single")
    public ResponseEntity<String> queryAddressJson(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "單筆查詢，request body 要帶 json，需包含:originalAddress、county(可為空)、town(可為空)。具體資料格式如下:",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SingleQueryDTO.class),
                            examples = @ExampleObject(value = "{\"originalAddress\":\"台南市東區衛國里007鄰衛國街１１４巷９弄１０號B六樓之５\",\"county\":\"臺南市(可為空)\",\"town\":\"東區(可為空)\"}")
                    )
            )
            @RequestBody SingleQueryDTO singleQueryDTO) {
        log.info("單筆查詢，參數為:{}", singleQueryDTO);
        return ResponseEntity.ok(singleQueryService.findJsonTest(singleQueryDTO));
    }


    @PostMapping("/query-single-test")
    public ResponseEntity<String> queryAddress(@RequestBody String originalString) {
//        try {
            return ResponseEntity.ok(singleQueryService.findJson(originalString));
//        } catch (Exception e) {
//            log.info("無法解析地址:{}", e.getMessage());
//            return ResponseEntity.ok("無法解析地址");
//        }
    }


    /***
     * 單筆查詢軌跡
     */
    @Operation(description = "單筆查詢軌跡",
            parameters = {  @Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "驗證jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                    required = true,
                    schema = @Schema(type = "string"))
            })
    @PostMapping("/query-track")
    public ResponseEntity<List<IbdTbIhChangeDoorplateHis>> queryTrack(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "編碼",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "BSZ7538-0")
                    )
            )
            @RequestBody String addressId ){
        return new ResponseEntity<>(singleQueryService.singleQueryTrack(addressId), HttpStatus.OK);
    }


    @PostMapping("/query-batch-track")
    public void queryBatchTrack (@Parameter(
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
            description ="申請單號" ,
            required = true,
            schema = @Schema(type = "string"))
    @RequestParam("formName") String formName,
    @Parameter(
            description = "使用者上傳的CSV檔",
            required = true
    )
    @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Received request");
        /**check**/
        if(file == null){
            throw new MoiException("檔案為空");
        }

        if(!file.getOriginalFilename().endsWith(".csv")){
           throw new MoiException("檔案須為CSV");
       }
        log.info("start processing");
        SingleBatchQueryParams singleBatchQueryParams = new SingleBatchQueryParams(Id, originalFileId, "0", "SYS_FAILED", formName + ".csv");
        singleQueryService.queryBatchTrack(file,singleBatchQueryParams);
    }
}
