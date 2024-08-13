package com.example.pentaho.resource;

import com.example.pentaho.component.Authorized;
import com.example.pentaho.component.Directory;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/qrcode/")
@SecurityRequirement(name = "Authorization")
public class QrcodeResource {

    private static Logger log = LoggerFactory.getLogger(APIKeyResource.class);

    @Autowired
    private Directory directory;




    @PostMapping("/get-qrcode-img")
    @Authorized(keyName = "SHENG")
    public ResponseEntity<byte[]> getQrcodeImg(String userId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.IMAGE_JPEG);
        String filePath = directory.getQrcodePath() + userId + ".jpg";
        File file = new File(filePath);
        if(file.exists()){

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(readFile(filePath));
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(null);
    }

    private static byte[] readFile(String fileName) throws Exception {
        return Files.readAllBytes(new File(fileName).toPath());
    }

}

