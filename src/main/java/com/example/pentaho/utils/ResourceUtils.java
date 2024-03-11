package com.example.pentaho.utils;

import com.cht.commons.persistence.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Component
public class ResourceUtils {

    @Autowired
    private ResourceLoader resourceLoader;

    private final Logger log = LoggerFactory.getLogger(ResourceUtils.class);

    public ResourceUtils() {
    }

    public String readAsString(String filePath) throws IOException {
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
}

