package com.example.pentaho.resource;

import com.example.pentaho.component.Authorized;
import com.example.pentaho.component.UsageLog;
import com.example.pentaho.component.UsageLogDTO;
import com.example.pentaho.utils.UsageLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usage-log")
@SecurityRequirement(name = "Authorization")
public class UsageLogResource {

    final static Logger log = LoggerFactory.getLogger(UsageLogResource.class);


    @Autowired
    private UsageLogService usageLogService;


    @Operation(description = "取得使用紀錄",
            parameters = {
            @Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "聖森私鑰加密 jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                    schema = @Schema(type = "string"))
            })
    @PostMapping("/get-usagelog")
    @Authorized(keyName = "SHENG")
    public ResponseEntity<List<UsageLog>> getUasgeLog(
            @Parameter(required = true,
            content = @Content(
                    schema = @Schema(implementation=UsageLogDTO.class)
                   )
            )
            @RequestBody UsageLogDTO usageLogDTO){
        log.info("使用率查詢條件:{}",usageLogDTO);
        return new ResponseEntity<>(usageLogService.getUsageLogs(usageLogDTO), HttpStatus.OK);
    }
}
