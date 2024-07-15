package com.example.pentaho.resource;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * 用於產生Pentaho restful api 的Basic basicAuthentication
 *
 */
public class BasicAuthTest {

    private final static Logger log = LoggerFactory.getLogger(BasicAuthTest.class);

    private final static  String USERNAME = "cluster";
    private final static  String PASSWORD = "cluster";

    private final static String FILE_DESTINATION = "D:\\app\\pentaho\\basic-auth.txt";

    private final static String KETTLE_URL ="http://localhost:8081/kettle/status/";

    @Test
    public void generateEncodeAuth() throws IOException {

        String auth = USERNAME + ":" + PASSWORD;
        byte[] authBytes = auth.getBytes(StandardCharsets.UTF_8);
        String encodedAuth = "Basic "+Base64.getEncoder().encodeToString(authBytes);
        log.info("encodedAuth:{}",encodedAuth);
        writeFile(FILE_DESTINATION,encodedAuth.getBytes());
    }

    private static void writeFile(String destPath, byte[] bytes) throws IOException {
        File dest = new File(destPath);
        if (!dest.exists()) {
            dest.createNewFile();
        }else{
            dest.delete();
        }
        Files.write(dest.toPath(), bytes);
    }
    @Test
    public void connection() throws IOException {
        Path path = Path.of(FILE_DESTINATION);
        String encode = Files.readString(path, StandardCharsets.UTF_8);

        URL obj = new URL(KETTLE_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestProperty("Authorization",encode);
        con.setRequestMethod("GET");
        con.connect();;
        int responseCode = con.getResponseCode();
        log.info("res:{}",responseCode);
    }
}
