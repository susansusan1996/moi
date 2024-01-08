package com.example.pentaho.service;

import com.example.pentaho.component.Directory;
import com.example.pentaho.component.JobParams;
import com.example.pentaho.component.PentahoComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
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
        String testFileAbsolutePath =":home:addr:API_TEST.ktr";
        String url = pentahoComponent.getTarget()+"/kettle/executeTrans/?" +
                "trans="+ fileAbsolutePath + sperator + "level=Debug";

        log.info("request url:{}",url);
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

        log.info("Response Code:{}",responseCode);
        log.info("Response Content:{}",response.toString());
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
        String url = pentahoComponent.getTarget()+"/api/repo/files/" +
                pentahoBiPath + "/download";
//      ex: http://52.33.116.195:8081/pentaho/api/repo/files/:home:admin:API_TEST.ktr/download";
        log.info("request url:{}",url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        /**很重要**/
        basicAuthentication(con);

        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        log.info("Response Code:{}",responseCode);

        if(responseCode == 200){
            File directory = new File(directories.getPath());
            if (!directory.exists()) {
                directory.mkdirs(); // 創建路徑
            }
            Path path = Paths.get(directories.getPath());
            Path file = path.resolve("API_TEST.zip");
            Files.write(file, con.getInputStream().readAllBytes());
        }
    }


    public void downloadFileFromPentahoServer(String fileUrl, String savePath) throws IOException {
        URL url = new URL(fileUrl);
        URLConnection connection = url.openConnection();
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(savePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
}

