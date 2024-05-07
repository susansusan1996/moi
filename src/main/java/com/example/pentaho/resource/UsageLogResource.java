package com.example.pentaho.resource;

import com.example.pentaho.component.Authorized;
import com.example.pentaho.component.UsageLog;
import com.example.pentaho.component.UsageLogDTO;
import com.example.pentaho.component.UsageLogReport;
import com.example.pentaho.utils.UsageLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.sf.jasperreports.engine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

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
    @PostMapping("/get-usagelog-by-params")
    @Authorized(keyName = "SHENG")
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
    @GetMapping("/get-usagelogs")
    @Authorized(keyName = "SHENG")
    public ResponseEntity<List<UsageLogReport>> getUasgeLogs() {
        return new ResponseEntity<>(usageLogService.getUsageLogs(), HttpStatus.OK);
    }


    @GetMapping("/get-usagelog-file")
//    @Authorized(keyName = "SHENG")
    public void downloadFile(@RequestParam String fileType){
        log.info("檔案類型:{}",fileType);
//        List ussages = new ArrayList() {{
//            add(new UsageLog());
//        }};
//
//        byte[] content = usageLogService.getJasperReportContent(ussages,month);
//        ByteArrayResource resource = new ByteArrayResource(content);
//        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .contentLength(resource.contentLength())
//                .header(HttpHeaders.CONTENT_DISPOSITION,
//                        ContentDisposition.attachment()
//                                .filename("item-report.pdf")
//                                .build().toString())
//                .body(resource);
//    }
        try{
            JasperReport jasperReport = JasperCompileManager.compileReport("D:\\project\\moi_bigData_only\\moi\\src\\main\\resources\\jasperreport\\moi.jrxml");

            /***/
            SimpleDateFormat yyyyy = new SimpleDateFormat("yyyyy");
            String adYr = yyyyy.format(new Date());
            String mingoYr = String.valueOf(Integer.valueOf(adYr) - 1912);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("mingoYr",mingoYr);

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters,new JREmptyDataSource());
            byte[] reportContent = JasperExportManager.exportReportToPdf(jasperPrint);
            ByteArrayResource resource = new ByteArrayResource(reportContent);

            String desFilePath = "C:\\Users\\2212009\\test.pdf";
            // 输出文档
            JasperExportManager.exportReportToPdfFile(jasperPrint, desFilePath);
//            return ResponseEntity.ok()
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                    .contentLength(resource.contentLength())
//                    .header(HttpHeaders.CONTENT_DISPOSITION,
//                            ContentDisposition.attachment()
//                                    .filename("item-report.pdf")
//                                    .build().toString())
//                    .body(resource);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_PDF);
//        headers.setContentDispositionFormData("filename", "employees-details.pdf");
//        return new ResponseEntity<byte[]>(JasperExportManager.exportReportToPdf(jasperPrint), headers, HttpStatus.OK);
        } catch (Exception e) {
          log.info("e:{}",e.toString());
//          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}


