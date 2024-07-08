package com.example.pentaho.resource;

import com.example.pentaho.component.PentahoComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class TestBasicAuth {

    private final static Logger log = LoggerFactory.getLogger(TestBasicAuth.class);

    private PentahoComponent pentahoComponent;


    @Test
    public void testpass() {
        String username = "admin";
        String password = "password";
        String auth = username + ":" + password;
        byte[] authBytes = auth.getBytes(StandardCharsets.UTF_8);
        String encodedAuth = Base64.getEncoder().encodeToString(authBytes);
        log.info("encodedAuth:{}",encodedAuth);
    }

    @Test
    public void connection() throws IOException {
        Path path = Path.of("D:\\pentaho\\token.txt");
        String encode = Files.readString(path, StandardCharsets.UTF_8);

        String URL = "http://52.33.116.195:8080/pentaho/kettle/status";
        URL obj = new URL(URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestProperty("Authorization",encode);
        con.setRequestMethod("GET");
        con.connect();;
        int responseCode = con.getResponseCode();
        log.info("res:{}",responseCode);
    }
}
