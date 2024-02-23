package com.example.pentaho.service;

import com.example.pentaho.component.Directory;
import com.example.pentaho.component.JobParams;
import com.example.pentaho.component.PentahoComponent;
import com.example.pentaho.component.PentahoWebService;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.utils.UserContextUtils;
import com.example.pentaho.utils.WebServiceUtils;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;


@Service
public class JobService {

    @Autowired
    private JobParams jobParams;

    @Autowired
    private PentahoComponent pentahoComponent;

    @Autowired
    private Directory directories;

    @Autowired
    private WebServiceUtils webServiceUtils;

    @Autowired
    private FileUploadService fileUploadService;

    private final String sperator = "&";

    private Gson gson = new Gson();

    private final static Logger log = LoggerFactory.getLogger(JobService.class);


    public Integer startJob(JobParams jobParams, String path) throws IOException {
        log.info("jobParams:{}", jobParams);

        try {
            /**targerUrl**/
//         Ex: http://52.33.116.195:8081/pentaho/kettle/startTrans/;
            URL url = new URL(pentahoComponent.getWebTarget() + "/kettle/startTrans/");

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
            postData.append(jobParams.getJobs());
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
            URL url = new URL(pentahoComponent.getWebTarget() + "/kettle/sniffStep/");


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
            postData.append(jobParams.getJobs());
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
//      ex:http://52.33.116.195:8081/pentaho/kettle/executeTrans/?rep=PetahoRepository&trans=/home/addr/dev/PI_TEST.ktr&level=Debug

        /**
         * 使用檔案在Pentaho BI server 上的 '絕對路徑'
         * 如果有指定repositpry(rep=kettle-itbigbird),才可以使用'相對路徑'
         **/
        String fileAbsolutePath = "/home/addr/dev/MAIN.ktr";
        /**這裡寫一個可以把jobParams寫成url的方法**/
        String url = pentahoComponent.getWebTarget() +
                "/kettle/executeTrans/?" +
                "rep=PentahoRepository"+sperator +
                "trans=" + fileAbsolutePath + sperator +
                "level=Debug";

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
        String fileAbsolutePath = directories.getKtrFilePath() + jobParams.getJobs() + ".ktr";
        StringBuilder url = new StringBuilder(
                pentahoComponent.getWebTarget() + "/kettle/executeTrans/?rep=&" +
                        "trans=" + fileAbsolutePath + sperator
        );
        String newUrl = getFullUrl(url);
        log.info("request url:{}", newUrl);
        return connectPentaho(newUrl);
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
        String url = pentahoComponent.getWebTarget() + "/api/repo/files/" +
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
            File directory = new File(directories.getReceiveFileDir());
            if (!directory.exists()) {
                directory.mkdirs(); // 創建路徑
            }
            Path path = Paths.get(directories.getReceiveFileDir());
            Path file = path.resolve("API_TEST.zip");
            Files.write(file, con.getInputStream().readAllBytes());
        }
    }


    private String getFullUrl(StringBuilder url){
        Method[] methods = JobParams.class.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("get") && !method.getName().equals("getClass")) {
                try {
                    Object value = method.invoke(jobParams);
                    if (value != null && !value.toString().isEmpty()) {
                        url.append(method.getName().substring(3)).append("=").append(value).append(sperator);
                    }
                } catch (Exception e) {
                    throw new MoiException("url解析錯誤 " + method.getName() + ": " + e.getMessage(), e);
                }
            }
        }
        // 移除最後一個 sperator
        String newUrl = url.toString();
        if (newUrl.endsWith(sperator)) {
            newUrl = newUrl.substring(0, newUrl.length() - sperator.length());
        }
        return newUrl;
    }

    private int  connectPentaho(String url) throws IOException {
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
        log.info("Response Content:{}", response);
        return responseCode;
    }
    /**
     * 檔名規格:origrinalFileName_batchId_yyyyMMdd_HHmmss
     * @param origrinalFileName
     * @return
     */
    private String getFileName(String origrinalFileName,String batchId){
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
//        String timestamp = dateFormat.format(new Date());
//        int last = origrinalFileName.lastIndexOf(".");
//        String fileName = origrinalFileName.substring(0, last);
//        return fileName+"_"+batchId+".csv";
        return batchId+".csv";
    }

    public String[] targetDirs(String dateStamp){
        /**
         * /home/addr/batch_data/receive/orgId/yyyyMMdd
         * /home/addr/batch_data/send/orgId/yyyyMMdd
         */
        String receiveDir =directories.getReceiveFileDir()+ UserContextUtils.getUserHolder().getOrgId() + "/" + dateStamp + "/";
        String sendDir=directories.getSendFileDir()+ UserContextUtils.getUserHolder().getOrgId() + "/" + dateStamp + "/";

        log.info("receiveDir:{}",receiveDir);
        log.info("sendDir:{}",sendDir);
        String[] targetDirs = new String[]{receiveDir,sendDir};
        return targetDirs;
    }

    public String sftpUploadAndExecuteTrans(MultipartFile file,JobParams jobParams){
        /**以'批次ID'為檔名**/
        String fileName = getFileName(file.getOriginalFilename(), jobParams.getBATCH_ID());
        /**目錄**/
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateStamp = dateFormat.format(new Date());
        String[] targetDirs = targetDirs(dateStamp);
        /**SFTP**/
        boolean sftpUpload = fileUploadService.sftpUpload(file,targetDirs,fileName);
        /**上傳失敗**/
        if(!sftpUpload){
            String status="UPLOAD_ERROR";
            jobParams.setStatus(status);
            return status;
        }
        /**準備要給 pentaho params*/
        String Id = String.valueOf(UserContextUtils.getUserHolder().getId());
        String orgId = UserContextUtils.getUserHolder().getOrgId();
        jobParams.setDATA_DATE(dateStamp);
        jobParams.setDATA_SRC(orgId);
        jobParams.setFILE(fileName);
        jobParams.setUSER_ID(Id);
        return webServiceUtils.getConnection(PentahoWebService.executeTrans,jobParams);
    }

}

