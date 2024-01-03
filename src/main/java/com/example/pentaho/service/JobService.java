package com.example.pentaho.service;

import com.example.pentaho.model.JobParams;
import com.example.pentaho.model.PentahoComponent;
import com.example.pentaho.utils.ResourceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


@Service
public class JobService {

    @Autowired
    private JobParams jobParams;

    @Autowired
    private PentahoComponent pentahoComponent;

    @Autowired
    private ResourceUtils resource;

    @Autowired
    private ObjectMapper objectMapper;

    private final String sperator = "&";

    private final static Logger log = LoggerFactory.getLogger(JobService.class);


    public Integer startJob(JobParams jobParams, String path) throws IOException {
        log.info("jobParams:{}", jobParams);
//        String fileContent = resource.readAsString("classpath:kettle-actions/actions.json");
//        log.info("fileContent:{}", fileContent);
//        if(path.indexOf("trans")>=0){
//            JsonNode jsonNode = objectMapper.readTree(fileContent).get("trans").get(path);
//
//        }
        try {
            /**targerUrl**/

            URL url = new URL(pentahoComponent.getTarget() + "/kettle/startTrans/");

//        URL url = new URL("http://52.33.116.195:8081/pentaho/kettle/startTrans/");

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
            return 400;
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
            return 400;
        }

    }


    public Integer excuteJob(JobParams jobParams) throws IOException {
        /**實測:user&pass不需要，需要的是basicAuthentication**/
//      String url = "http://52.33.116.195:8081/pentaho/kettle/executeJob/?user=amdin&pass=password&job=/home/ec2-user/API_TEST.ktr/API_TEST.ktr&level=Debug";

        /**需要使用檔案在作業系統上的絕對路徑rep=kettle-itbigbird&**/
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
}

