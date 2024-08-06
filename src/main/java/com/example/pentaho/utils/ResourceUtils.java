package com.example.pentaho.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Iterator;

@Component
public class ResourceUtils {

    @Autowired
    private ResourceLoader resourceLoader;


    private final static  Logger log = LoggerFactory.getLogger(ResourceUtils.class);

    private final static  ObjectMapper objectMapper = new ObjectMapper();

    public ResourceUtils() {
    }

    public  String readAsString(String filePath) throws IOException {
        try {
            InputStream inputStream = resourceLoader.getResource(filePath).getInputStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream));
            String readLine;
            StringBuilder fileContent = new StringBuilder();
            while ((readLine = bf.readLine()) != null) {
                fileContent.append(readLine);
            }
            bf.close();
            log.info("fileContent:{}", fileContent);
            return fileContent.toString();
        } catch (Exception e) {
            log.info("fileContent:{}", "fail to read");
            return "";
        }
    }


    public String getJoinStepDes(String joinStep) throws IOException {
        log.info("joinStep:{}",joinStep);
        String content = readAsString("classpath:joinstep-mapping.json");
        JsonNode jsonNode = objectMapper.readTree(content);
        log.info("jsonNode.get(joinStep.substring(0,3)).asText():{}",jsonNode.get(joinStep.substring(0,3)).asText());
        return StringUtils.isNullOrEmpty(jsonNode.get(joinStep.substring(0,3)).asText()) ? joinStep : joinStep+":"+jsonNode.get(joinStep.substring(0,3)).asText();
    }
}

