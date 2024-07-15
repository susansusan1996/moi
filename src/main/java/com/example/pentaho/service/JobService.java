package com.example.pentaho.service;

import com.example.pentaho.component.Directory;
import com.example.pentaho.component.JobParams;
import com.example.pentaho.component.PentahoComponent;
import com.example.pentaho.component.PentahoWebService;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.repository.JobStatusRepository;
import com.example.pentaho.utils.StringUtils;
import com.example.pentaho.utils.WebServiceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.time.DateUtils;
import org.checkerframework.checker.units.qual.A;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


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

    @Autowired
    private JobStatusRepository jobStatusRepository;

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

//        log.info("Response Code:{}", responseCode);
//        log.info("Response Content:{}", response.toString());
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

//        log.info("Response Code:{}", responseCode);
//        log.info("Response Content:{}", response.toString());
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
    private void basicAuthentication(HttpURLConnection connection) throws IOException {
        Path path = Path.of(pentahoComponent.getEncodeAuth());
        String encode = Files.readString(path, StandardCharsets.UTF_8);
        connection.setRequestProperty("Authorization", encode);
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
//        log.info("Response Code:{}", responseCode);

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
//        log.info("Response Code:{}", responseCode);
//        log.info("Response Content:{}", response);
        return responseCode;
    }
    /**
     * 檔名規格:batchId.csv
     * @return
     */
    private String getFileName(String batchId){
        return batchId+".csv";
    }

    /***
     * 建立上傳 & 抓檔目錄
     * /home/addr/batch_data/receive/formBuilderOrgId(申請者單位ID)/yyyyMMdd/
     * /home/addr/batch_data/send/formBuilderOrgId(申請者單位ID)/yyyyMMdd/
     * @param jobParams
     * @return
     */
    public String[] targetDirs(JobParams jobParams) {
        String receiveDir = directories.getReceiveFileDir() + jobParams.getDATA_SRC()+ "/" + jobParams.getDATA_DATE() + "/";
        String sendDir = directories.getSendFileDir() + jobParams.getDATA_SRC() + "/" + jobParams.getDATA_DATE() + "/";

        log.info("receiveDir:{}", receiveDir);
        log.info("sendDir:{}", sendDir);
        String[] targetDirs = new String[]{receiveDir, sendDir};
        return targetDirs;
    }


    public Integer CSVReader(MultipartFile file) throws IOException {
        try{
            Integer dataCnt = 0;
            CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()));
            String[] line;
            /**跳過表頭**/
            csvReader.readNext();
            while ((line = csvReader.readNext()) != null) {
                /**空行也算一筆**/
                    dataCnt+=1;
            }
            log.info("dataCnt:{}",dataCnt);
            return dataCnt;
        }catch (Exception e){
            log.info("e:{}",e.toString());
           throw new MoiException("READ_FILE_ERROR");
        }
    }

    /**
     * 上傳檔案至Pentaho Server & call Job
     * @param file
     * @param jobParams
     * @param result response content -> 目前只有用到status
     * @return
     * @throws IOException
     */
    public Map<String, String> sftpUploadAndExecuteTrans(MultipartFile file,JobParams jobParams,Map<String,String> result) throws IOException {
        /*PROCESSED_COUNTS(檔案筆數): Pentaho Server & Shengsen 的參數*/
        jobParams.setPROCESSED_COUNTS(CSVReader(file));

        /*以 formName(申請單號) 為檔名**/
        String fileName = getFileName(jobParams.getFORM_NAME());

        /*以 ../yyyyMMdd/formBuilderOrgId/ 作為目錄 */
        String[] targetDirs = targetDirs(jobParams);
        /*SFTP**/
        boolean sftpUpload = fileUploadService.sftpUpload(file,targetDirs,fileName);
        /*上傳失敗**/
        if(!sftpUpload){
            String status = "UPLOAD_ERROR";
            jobParams.setStatus(status);
            result.put("status",status);
            return result;
        }
        //todo:SFTP成功，準備呼叫job， result 中放 PentahoWebService 回傳內容的key(可參考官方文件)*/
        webServiceUtils.getConnection(PentahoWebService.executeJobs, jobParams, result);
        //todo: 要存到DB嗎
//        String jsonStr = gson.toJson(jobParams);
//        Date date = DateUtils.parseDate(new Date().toString(), "yyyy-mm-dd");
//        result.put("jobParamsJsonStr",jsonStr);
//        result.put("formName",jobParams.getFORM_NAME());
//        result.put("executeDate",date.toString());
//        result.put("updateDate",date.toString());
//        jobStatusRepository.insertJobStatus(result);
        return result;
    }


    /**
     * 模擬calljob
     */
    public void simpleJob() throws ParseException {
        /*RequestParams*/
        JobParams jobParams = new JobParams();
        /*Response*/
        HashMap<String, String> result = new HashMap<>();
        webServiceUtils.getConnection(PentahoWebService.simpleExecuteJob,jobParams,result);
        String jsonStr = gson.toJson(jobParams);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formatDate = simpleDateFormat.format(new Date());
        result.put("jobParamsJsonStr",jsonStr);
        result.put("formName", StringUtils.isNullOrEmpty(jobParams.getFORM_NAME())?"formName":jobParams.getFORM_NAME());
        result.put("executeDate",formatDate);
        result.put("updateDate",formatDate);
        jobStatusRepository.insertJobStatus(result);
    }


    /**
     * @return
     */
    public Map<String,String> getJobStatusById(String id){
        Map<String,String> result = new HashMap<>();
        result.put("id",id);
        webServiceUtils.getJobStatus(result);
        return result;
    }




}

