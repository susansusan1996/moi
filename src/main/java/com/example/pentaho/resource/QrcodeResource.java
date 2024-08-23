package com.example.pentaho.resource;

import com.example.pentaho.component.Authorized;
import com.example.pentaho.component.Directory;
import com.example.pentaho.component.SingleQueryDTO;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.print.DocFlavor;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/api/qrcode/")
@SecurityRequirement(name = "Authorization")
public class QrcodeResource {

    private static Logger log = LoggerFactory.getLogger(APIKeyResource.class);

    @Autowired
    private Directory directory;




    @PostMapping("/get-qrcode-img")
    @Authorized(keyName = "SHENG")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "取得Qrcode圖片",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = Map.class),
                    examples = @ExampleObject(value = "{\"seq\":\"3498211\"}")
            )
    )
    public ResponseEntity<byte[]> getQrcodeImg(Map<String,String> params) throws Exception {
        if (Strings.isNullOrEmpty(params.get("seq"))){
            return ResponseEntity.internalServerError()
                    .body(null);
        }
        String seq = params.get("seq");
        log.info("查詢:{}.jpg",seq);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.IMAGE_JPEG);
        String filePath = directory.getQrcodePath() + seq + ".jpg";
        File file = new File(filePath);
        if(file.exists()){
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(readFile(filePath));
        }

        return ResponseEntity.internalServerError()
                .headers(headers)
                .body(null);
    }

    private static byte[] readFile(String fileName) throws Exception {
        return Files.readAllBytes(new File(fileName).toPath());
    }

}

