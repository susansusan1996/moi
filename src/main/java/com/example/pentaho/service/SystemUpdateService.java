package com.example.pentaho.service;

import com.example.pentaho.component.ApServerComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

@Component
public class SystemUpdateService {

    @Autowired
    private ApServerComponent apServerComponent;

    private final static String SYSTEM_UPDATE_URI = "/functionRecord/singleQuery/systemUpdate";

    private final static ObjectMapper objectMapper = new ObjectMapper();

    private final static Logger log = LoggerFactory.getLogger(SystemUpdateService.class);

    /*
     * 單筆查詢 & 單筆軌跡 給聖森更新使用狀況
     * */
    public void singleQuerySystemUpdate(String userId,String type){
        log.info("userId:{}",userId);
        log.info("type:{}",type);
        try {
            String targerUrl = apServerComponent.getTargetUrl() + SYSTEM_UPDATE_URI;
            log.info("targetUrl: {}", targerUrl);

            Path path = Path.of(apServerComponent.getToken());
            String token = Files.readString(path, StandardCharsets.UTF_8);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HashMap<String,String> params = new HashMap<>();
            params.put("userId",userId);
            params.put("type", type);

            String jsonStr = objectMapper.writeValueAsString(params);
            HttpEntity<String> request = new HttpEntity<>(jsonStr, headers);
            log.info("request:{}",request);
            String response = restTemplate.postForObject(targerUrl, request, String.class);
            log.info("response:{}", response);
        }catch (Exception e){
            log.info("e:{}",e.toString());
        }
    }
}
