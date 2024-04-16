package com.example.pentaho.resource;


import com.example.pentaho.component.SingleBatchQueryParams;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.service.SingleTrackQueryService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/single-track-query")
@Hidden
public class SingleTrackQueryResource {

    private Logger logger = LoggerFactory.getLogger(SingleTrackQueryResource.class);


    @Autowired
    private SingleTrackQueryService singleQueryTrackService;



    @Operation(description = "單筆軌跡批次",
            parameters = {@Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "jwt token,body附帶 userInfo={\"Id\":1,\"orgId\":\"Admin\"}",
                    required = true,
                    schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "作業開始執行"),
                    @ApiResponse(responseCode = "500", description = "檔案為空 或 非CSV檔"
                    )})
    @PostMapping("/query-batch-track")
    public void queryBatchTrack(@Parameter(
            description = "批次ID",
            required = true,
            schema = @Schema(type = "string"))
                                    @RequestParam("Id") String Id,
                                @Parameter(
                                        description = "原始CSVID",
                                        required = true,
                                        schema = @Schema(type = "string"))
                                    @RequestParam("originalFileId") String originalFileId,
                                @Parameter(
                                        description = "申請單號",
                                        required = true,
                                        schema = @Schema(type = "string"))
                                    @RequestParam("formName") String formName,
                                @Parameter(
                                        description = "使用者上傳的CSV檔",
                                        required = true
                                )
                                    @RequestParam("file") MultipartFile file) throws IOException {
        logger.info("Received request");
        /**check**/
        if (file == null) {
            throw new MoiException("檔案為空");
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            throw new MoiException("檔案須為CSV");
        }
        logger.info("start processing");
        SingleBatchQueryParams singleBatchQueryParams = new SingleBatchQueryParams(Id, originalFileId, "0", "SYS_FAILED", formName + ".csv");
        String fileContent = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        singleQueryTrackService.queryBatchTrack(fileContent, singleBatchQueryParams);
    }




}
