package com.example.pentaho.resource;

import com.example.pentaho.component.Authorized;
import com.example.pentaho.component.UsageLog;
import com.example.pentaho.component.UsageLogDTO;
import com.example.pentaho.component.UsageLogReport;
import com.example.pentaho.utils.JasperResportUtils;
import com.example.pentaho.service.UsageLogService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/usage-log")
@SecurityRequirement(name = "Authorization")
public class UsageLogResource {

    final static Logger log = LoggerFactory.getLogger(UsageLogResource.class);


    private final static String PDF_FILE_PATH ="/moi.jrxml";


    private final static String ODS_FILE_PATH = "/moi_ods.jrxml";


    private final static SimpleDateFormat yyyyy = new SimpleDateFormat("yyyyy");

    private final static SimpleDateFormat MM = new SimpleDateFormat("MM");

    @Autowired
    private UsageLogService usageLogService;


    @Operation(description = "取得使用紀錄",
            parameters = {
            @Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "聖森私鑰加密 jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                    schema = @Schema(type = "string"))
            })
    @PostMapping("/get-usagelog-by-params")
    @Authorized(keyName = "SHENG")
    @Hidden
    public ResponseEntity<List<UsageLog>> getUasgeLogByPrams(
            @Parameter(required = true,
            content = @Content(
                    schema = @Schema(implementation=UsageLogDTO.class)
                   )
            )
            @RequestBody UsageLogDTO usageLogDTO){
        log.info("使用率查詢條件:{}",usageLogDTO);
        return new ResponseEntity<>(usageLogService.getUsageLogsByParams(usageLogDTO), HttpStatus.OK);
    }


    @Operation(description = "取得使用紀錄",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "聖森私鑰加密 jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                            schema = @Schema(type = "string"))
            })
    @PostMapping("/get-usagelogs")
    @Authorized(keyName = "SHENG")
    public ResponseEntity<List<UsageLogReport>> getUasgeLogs(
            @Parameter(required = true,
                    content = @Content(
                            schema = @Schema(implementation=UsageLogDTO.class)
                    )
            )
            @RequestBody UsageLogDTO usageLogDTO
    ) {
        log.info("usageLogDTO:{}",usageLogDTO);
        return new ResponseEntity<>(usageLogService.getUsageLogs(usageLogDTO), HttpStatus.OK);
    }


    @PostMapping(value = "/get-usagelog-pdf")
    @Authorized(keyName = "SHENG")
    public void downloadPDFFile(@RequestBody UsageLogDTO usageLogDTO,HttpServletResponse response) throws JRException, IOException, NoSuchMethodException {
        log.info("usageLogDTO:{}",usageLogDTO);
            List<UsageLogReport> originalList = usageLogService.getUsageLogs(usageLogDTO);
            List<UsageLogReport> usageLogs = usageLogService.changeDateTime(originalList);
            String fileName = usageLogDTO.getDataDateStart() + "_" + usageLogDTO.getDataDateEnd() + "_APIUsage.pdf";
        /***/
        String adYr = yyyyy.format(new Date());
        String mingoYr = String.valueOf(Integer.valueOf(adYr) - 1911);
        String month = MM.format(new Date());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("mingoYr",mingoYr);
        parameters.put("month",month);
        parameters.put("formId","待確認");
            JasperPrint jasperPrint = JasperResportUtils.compileReport(PDF_FILE_PATH,parameters, usageLogs);
            JasperResportUtils.exporterPDFFile(fileName, jasperPrint, response);
    }


    @PostMapping(value = "/get-usagelog-ods")
    @Authorized(keyName = "SHENG")
    public void downloadODSFile(@RequestBody UsageLogDTO usageLogDTO,HttpServletResponse response) throws JRException, IOException, NoSuchMethodException {
        log.info("usageLogDTO:{}",usageLogDTO);
        List<UsageLogReport> originalList = usageLogService.getUsageLogs(usageLogDTO);
        List<UsageLogReport> usageLogs = usageLogService.changeDateTime(originalList);
        JasperPrint jasperPrint = JasperResportUtils.compileReport(ODS_FILE_PATH,null, usageLogs);
        String fileName = usageLogDTO.getDataDateStart()+"_"+usageLogDTO.getDataDateEnd()+"_APIUsage.ods";
        JasperResportUtils.exporterOdsFile(fileName,jasperPrint,response);
    }




}


