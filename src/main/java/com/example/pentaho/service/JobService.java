package com.example.pentaho.service;

import com.example.pentaho.component.ApServerComponent;
import com.example.pentaho.component.Directory;
import com.example.pentaho.component.JobParams;
import com.example.pentaho.component.PentahoComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;


@Service
public class JobService {

    @Autowired
    private JobParams jobParams;

    @Autowired
    private PentahoComponent pentahoComponent;

    @Autowired
    private Directory directories;

    @Autowired
    private ApServerComponent apServerComponent;

    private final String sperator = "&";

    private final static Logger log = LoggerFactory.getLogger(JobService.class);


    public Integer startJob(JobParams jobParams, String path) throws IOException {
        log.info("jobParams:{}", jobParams);

        try {
            /**targerUrl**/
//         Ex: http://52.33.116.195:8081/pentaho/kettle/startTrans/;
            URL url = new URL(pentahoComponent.getTarget() + "/kettle/startTrans/");

            /**openConnection**/
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            /**setRequestMethod = post **/
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            /**設定basic Authentiction Header**/
            basicAuthentication(connection);

            /**post body**/
//          ex:"name=Job 2&xml=Y";
            StringBuilder postData = new StringBuilder();
            postData.append("name=");
            postData.append(jobParams.getJobName());
            postData.append(sperator);
            postData.append("xml=Y");
            byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);

            /**requset header**/
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));

            /**output request**/
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(postDataBytes);
            }

            /**get Response**/
            int responseCode = connection.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            log.info("trans Response Code: " + responseCode);
            log.info("trans Response Content: " + content.toString());


            /**!!!!!!!**/
            connection.disconnect();
            return responseCode;
        } catch (Exception e) {
            log.info("e:{}", e);
            return 403;
        }
    }

    public Integer sniffStep(JobParams jobParams) {
        log.info("jobParams:{}", jobParams);
        try {
            /**targerUrl**/
            URL url = new URL(pentahoComponent.getTarget() + "/kettle/sniffStep/");


            /**openConnection**/
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            /**setRequestMethod = post **/
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            /**設定basic Authentiction Header**/
            basicAuthentication(connection);

            /**post body**/
//          String postData = "name=Job 2&xml=Y";
            StringBuilder postData = new StringBuilder();
            postData.append("trans=");
            postData.append(jobParams.getJobName());
            postData.append(sperator);
            postData.append("step=");
            postData.append("JSON output");
            postData.append(sperator);
            postData.append("xml=Y");
            byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);

            /**requset header**/
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));

            /**output request**/
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(postDataBytes);
            }

            /**get Response**/
            int responseCode = connection.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            log.info("trans Response Code: " + responseCode);
            log.info("trans Response Content: " + content.toString());


            /**!!!!!!!**/
            connection.disconnect();
            return responseCode;
        } catch (Exception e) {
            log.info("e:{}", e);
            return 403;
        }

    }


    public Integer excuteJob(JobParams jobParams) throws IOException {
        /**實測:user&pass不需要，需要的是basicAuthentication**/
//      ex: http://52.33.116.195:8081/pentaho/kettle/executeJob/?job=/home/ec2-user/API_TEST.ktr/API_TEST.ktr&level=Debug;

        /**需要使用檔案在作業系統上的絕對路徑rep=kettle-itbigbird **/
        String url = "http://52.33.116.195:8081/pentaho/kettle/executeJob/?job=/home/addr/prod/API_TEST.ktr&level=Debug";

        URL obj = new URL(url);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        /**很重要**/
        basicAuthentication(con);

        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        log.info("Response Code:{}", responseCode);
        log.info("Response Content:{}", response.toString());
        return responseCode;
    }

    public Integer excuteTrans(JobParams jobParams) throws IOException {
        /**實測:user&pass不需要，需要的是basicAuthentication**/
//      ex:http://52.33.116.195:8081/pentaho/kettle/executeTrans/?trans=/home/ec2-user/API_TEST.ktr/API_TEST.ktr&level=Debug

        /**
         * 使用檔案在作業系統上的 '絕對路徑'
         *如果有指定repositpry(rep=kettle-itbigbird),才可以使用'相對路徑'
         **/
        String fileAbsolutePath = "/home/addr/prod/API_TEST.ktr";
        String testFileAbsolutePath = ":home:addr:API_TEST.ktr";
        String url = pentahoComponent.getTarget() + "/kettle/executeTrans/?" +
                "trans=" + fileAbsolutePath + sperator + "level=Debug";

        log.info("request url:{}", url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        /**很重要**/
        basicAuthentication(con);

        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();


        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        log.info("Response Code:{}", responseCode);
        log.info("Response Content:{}", response.toString());
        return responseCode;
    }


    /**
     * 測試啟動帶有parameter的transformation
     */
    public Integer excuteTransWithParams(JobParams jobParams) throws IOException {
        String fileAbsolutePath = directories.getKtrFilePath() + jobParams.getJobName() + ".ktr";
        //rep= 是必要參數，即使後面值為空，也要寫在url上!
        //ADDR=AAA、JOIN_STEP=BBB，都是自定義要傳入transformation的參數
        String url = pentahoComponent.getTarget() + "/kettle/executeTrans/?rep=&" +
                "trans=" + fileAbsolutePath + sperator +
                "filename=filename_test" + sperator +
                "adrVersion=111" + sperator +
                "batchFormId=222" + sperator +
                "batchFormOriginalFileId=333";

        log.info("request url:{}", url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        /**很重要**/
        basicAuthentication(con);
        con.setRequestMethod("POST");
        int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        log.info("Response Code:{}", responseCode);
        log.info("Response Content:{}", response.toString());
        return responseCode;
    }


    /***
     *  添加 Basic Authentication header
     * @param connection
     */
    private void basicAuthentication(HttpURLConnection connection) {
        String username = pentahoComponent.getUserName(); //admin
        String password = pentahoComponent.getPassword(); //password
        String auth = username + ":" + password;
        byte[] authBytes = auth.getBytes(StandardCharsets.UTF_8);
        String encodedAuth = Base64.getEncoder().encodeToString(authBytes);
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
    }

    public void downloadFile() throws IOException {

        /**
         * pentaho bi server 下的檔案
         * /path/to/file, the encoded pathId would be :path:to:file.
         **/
        String pentahoBiPath = ":home:admin:API_TEST.ktr";
        String url = pentahoComponent.getTarget() + "/api/repo/files/" +
                pentahoBiPath + "/download";
//      ex: http://52.33.116.195:8081/pentaho/api/repo/files/:home:admin:API_TEST.ktr/download";
        log.info("request url:{}", url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        /**很重要**/
        basicAuthentication(con);

        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        log.info("Response Code:{}", responseCode);

        if (responseCode == 200) {
            File directory = new File(directories.getPath());
            if (!directory.exists()) {
                directory.mkdirs(); // 創建路徑
            }
            Path path = Paths.get(directories.getPath());
            Path file = path.resolve("API_TEST.zip");
            Files.write(file, con.getInputStream().readAllBytes());
        }
    }

    public void etlFinishedAndSendFile(String jobParams) throws IOException {
        String sourceFilePath = directories.getTarget() + directories.getEtlOutputFileDirPrefix() + "test" + ".zip";
        //先落地
        downloadFileFromPentahoServer(sourceFilePath, directories.getMockEtlSaveFileDirPrefix());
        postFileToServer(directories.getMockEtlSaveFileDirPrefix()+"test" + ".zip", apServerComponent.getTargetUrl());
    }


    public void downloadFileFromPentahoServer(String sourceFilePath, String savePath) {
        URL url;
        try {
            url = new URL(sourceFilePath);
            URLConnection connection = url.openConnection();
            String fileName = "test.zip";
            String saveFilePath = savePath + fileName;
            log.info("儲存zip檔案的路徑: {}", saveFilePath);
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(saveFilePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                log.info("成功儲存zip檔案到: {}", saveFilePath);
            } catch (IOException e) {
                log.error("在儲存zip檔案時發生IOException: {}", e.getMessage());
            }
        } catch (IOException e) {
            log.error("URL格式錯誤: {}", e.getMessage());
        }
    }


    public void postFileToServer(String sourceFilePath, String targetUrl) throws IOException {
        File file = new File(sourceFilePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + sourceFilePath);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + "eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoie30iLCJqdGkiOiJNVE0yTmpRMk9HWXRPRGcyWWkwME9UTXhMV0UyWlRRdE9URmlNelE1WlRjek5ETTAiLCJleHAiOjE3Mzc2MDE4MzJ9.3ghp8wCHziA6Az9UpS8ssL1d_JB5apN-3pbIV28BWx3bOK-FjRGA9676-EDpqhXrth_Sqln_TFd4wT0RGJ4V1M0RtKXj3EMpFBBV0otdAsgZLm0JcK7LjUrXmWvyfsBcasnHQ83rMo4hE4GeBgXlrhPUlRxnPcVbk4UrVkaMtxyngDfkGpInPJokUWzrScgo7TDA-aKmodw2eZbxYPjGTw1fzXTYHpJC4VNyAYbeGOTd9uMh-cCAyyYMsw__JmkQOAYPpKLnHdyHSb6C8ezxAZJNrI5Rpg4cG0ousXh694IXmixI_R7Q1nVBMFl7GG946fgTO9twiqhuaB64beUILg");
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("etlOutPutFile", new org.springframework.core.io.ByteArrayResource(Files.readAllBytes(file.toPath())) {
            @Override
            public String getFilename() {
                return file.getName();
            }
        });
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parts, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Void> responseEntity = restTemplate.exchange(targetUrl, HttpMethod.POST, requestEntity, Void.class, parts);

        HttpStatusCode statusCode = responseEntity.getStatusCode();
        if (statusCode == HttpStatus.OK) {
            log.info("File uploaded successfully.");
        } else {
            log.error("File upload failed. Response Code: {} ", statusCode.value());
        }
    }
}

