package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.utils.ResourceUtils;
import com.example.pentaho.utils.StringUtils;
import com.example.pentaho.utils.custom.Sftp;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.nio.file.Path;
import java.util.UUID;


@Service
@Transactional
public class FileOutputService {


    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private Directory directories;

    @Autowired
    private ApServerComponent apServerComponent;

    @Autowired
    private ResourceUtils resourceUtils;

    @Autowired
    private BigDataService bigDataService;

    @Autowired
    private JobService jobService;

    @Autowired
    private Sftp sftp;

    private final static Logger log = LoggerFactory.getLogger(JobService.class);
    private final String sperator = "&";





    public void etlFinishedAndSendFile(JobParams jobParams) throws IOException {
//        String sourceFilePath =directories.getTarget()+directories.getReceivePath()+

        String sourceFilePath = directories.getTarget() + directories.getEtlOutputFileDirPrefix() + jobParams.getFORM_NAME() + ".zip";
        //先落地
        downloadFileFromPentahoServer(jobParams, sourceFilePath);
//        postFileToServer(
//                directories.getMockEtlSaveFileDirPrefix() +
//                        jobParams.getFILE() + ".zip",
//                apServerComponent.getTargetUrl(),
//                null
//        );
    }


    public void downloadFileFromPentahoServer(JobParams jobParams, String sourceFilePath) {
        URL url;
        try {
            url = new URL(sourceFilePath);
            URLConnection connection = url.openConnection();
            String fileName = jobParams.getFORM_NAME() + ".zip";
            String saveFilePath = directories.getMockEtlSaveFileDirPrefix() + fileName;
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


    /**
     * post file
     * 這個方法parts不能沒有file參數，且value也不能為空
     * @param sourceFilePath
     * @param action
     * @param jobParams
     * @return
     * @throws IOException
     */
//    public int postBatchFormRequest(String sourceFilePath, String action, JobParams jobParams) throws IOException {
//        String targetUrl = apServerComponent.getTargetUrl()+ action;
//        log.info("job參數:{}",jobParams);
//        log.info("聖森uri: {}",targetUrl);
//        log.info("本機暫存:{}",sourceFilePath);
//        HttpHeaders headers = new HttpHeaders();
//        /****/
//        Path path = Path.of(apServerComponent.getToken());
//        String token = Files.readString(path, StandardCharsets.UTF_8);
//        headers.set("Authorization",token);
//
//        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
//        if("DONE".equals(jobParams.getStatus())){
//            File file = new File(sourceFilePath);
//        if (file.exists()) {
//        parts.set("file", new org.springframework.core.io.ByteArrayResource(Files.readAllBytes(file.toPath())) {
//            @Override
//            public String getFilename() {
//                return file.getName();
//            }
//        });
//        }
//        }else{
//            parts.add("file",null);
//        }
//        parts.add("id",jobParams.getBATCH_ID());
//        parts.add("originalFileId",jobParams.getBATCHFORM_ORIGINAL_FILE_ID());
//        parts.add("processedCounts","0");
//        log.info("status:{}",jobParams.getStatus());
//        parts.add("status",jobParams.getStatus());
//        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parts, headers);
//        RestTemplate restTemplate = new RestTemplate();
//        ResponseEntity<Void> responseEntity = restTemplate.exchange(targetUrl, HttpMethod.PUT, requestEntity, Void.class,parts);
//
//        HttpStatusCode statusCode = responseEntity.getStatusCode();
//        if (statusCode == HttpStatus.OK) {
//            log.info("File uploaded successfully.");
//        } else {
//            log.error("File upload failed. Response Code: {} ", statusCode.value());
//        }
//        return statusCode.value();
//    }


    /***
     * petahoServer抓檔
     * AP落地
     * 檔案 post聖森
     * @param jobParams
     * @throws SftpException
     * @throws IOException
     */
    public void sftpDownloadBatchFormFileAndSend(JobParams jobParams) throws SftpException, IOException {
        log.info("jobParams:{}",jobParams);
        String sourceFilePath = "";
        String status = "SYS_FAILED";
        /**有成功才去抓檔**/
        if("DONE".equals(jobParams.getStatus())) {
            String targetDir = directories.getSendFileDir() + jobParams.getDATA_SRC() + "/" + jobParams.getDATA_DATE() + "/";
            String fileName = jobParams.getFORM_NAME() + ".zip";
            log.info("目標目錄:{}", targetDir);
            log.info("目標檔名:{}", fileName);
            /**SFTP抓檔落地**/

            sftp.connect();
            boolean hasFile = sftp.listFiles(targetDir, fileName);
            log.info("已完成zip檔:{}", hasFile);
            if (hasFile) {
                boolean hasDownload = sftp.downloadFile(directories.getLocalTempDir(), targetDir, fileName);
                log.info("已下載zip檔:{}", hasDownload);
                if (hasDownload) {
                    status = "DONE";
                    sourceFilePath = directories.getLocalTempDir() + fileName;
                }
            }
            sftp.disconnect();
        }
        jobParams.setStatus(status);

        BatchFormParams batchFormParams = new BatchFormParams(jobParams.getBATCH_ID(), jobParams.getBATCHFORM_ORIGINAL_FILE_ID(), String.valueOf(jobParams.getPROCESSED_COUNTS()), jobParams.getStatus(), null);
        log.info("給聖森更新狀態的參數:{}",batchFormParams );
        postBatchFormRequest("/batchForm/systemUpdate",batchFormParams,sourceFilePath);
//      postBatchFormRequest(sourceFilePath,"/batchForm/systemUpdate",jobParams);
    }

    /**
     *
     * @param formName
     * @return List<IbdTbAddrStatisticsOverallDev>
     */
    public Integer findLog(String formName){
        return bigDataService.findLog(formName);
    }

    /***
     * server 抓檔
     * @param batchId
     * @return
     */
    public boolean sftpDownloadBigQueryFile(String batchId){
        log.info("batchId:{}",batchId);
        String fileName = batchId+".zip";
        boolean result =  false;
        try {
            sftp.connect();
            result = sftp.downloadFile(directories.getLocalTempDir(),directories.getBigDataSendFileDir(),fileName);
        }catch (Exception e){
            log.info("e:{}",e.toString());
        }
        sftp.disconnect();
        return result;
    }



    /**
     *  post給聖森(未使用)
     * @param sourceFilePath
     * @param action
     * @param bigDataParams
     * @return
     * @throws IOException
     */
    public int postBigQueryResquest(String sourceFilePath, String action, BigDataParams bigDataParams) throws IOException {
        String targetUrl = apServerComponent.getTargetUrl()+ action;
        log.info("targetUrl: {}",targetUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization","");
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        if(!"".equals(sourceFilePath)){
            File file = new File(sourceFilePath);
            if (file.exists()) {
                parts.add("file", new org.springframework.core.io.ByteArrayResource(Files.readAllBytes(file.toPath())) {
                    @Override
                    public String getFilename() {
                        return file.getName();
                    }
                });
            }
        }
        parts.add("id",bigDataParams.getId());
        parts.add("recordCounts",bigDataParams.getRecordCounts());
        parts.add("fileUri",bigDataParams.getFileUri());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parts, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Void> responseEntity = restTemplate.exchange(targetUrl, HttpMethod.PUT, requestEntity, Void.class,parts);

        HttpStatusCode statusCode = responseEntity.getStatusCode();
        if (statusCode == HttpStatus.OK) {
            log.info("File uploaded successfully.");
        } else {
            log.error("File upload failed. Response Code: {} ", statusCode.value());
        }
        return statusCode.value();
    }

    /**
     * 大量查詢(未使用)
     * sftp抓檔
     * 撈log
     * post給聖森
     * **/
    public int SftpBigQueryFileAndPost(BigDataParams bigDataParams) throws IOException{
        String sourceFilePath ="";
        String fileUri = "";
        boolean hasFile = sftpDownloadBigQueryFile(bigDataParams.getId());
        if(hasFile) {
            /***/
            sourceFilePath = directories.getLocalTempDir()+bigDataParams.getId()+".zip";
            fileUri = directories.getLocalTempDir()+bigDataParams.getId()+".zip";
        }
        Integer cnt = findLog(bigDataParams.getId());
        bigDataParams.setRecordCounts(cnt == null? "0":String.valueOf(cnt));
        File file = new File(fileUri);
        if (!file.exists()) {
            bigDataParams.setFileUri("no exist");
        }
        bigDataParams.setFileUri(fileUri);
        return postBigQueryResquest(sourceFilePath,"/bigQueryForm/systemUpdate",bigDataParams);
    }



    public int postBatchFormRequest(String action, Object params, String filePath) throws IOException {
        String targerUrl = apServerComponent.getTargetUrl() + action;
        log.info("聖森URL:{}",targerUrl);

        File file = null;
        String fileName ="";
        if(StringUtils.isNotNullOrEmpty(filePath)){
           file = new File(filePath);
            if (file.exists()) {
                fileName = String.valueOf(Path.of(filePath).getFileName());
                log.info("完成檔名:{}",fileName);
//            throw new IOException("File not found: " + filePath);
            }
        }

        URL url = new URL(targerUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");

        /**necessay**/
        con.setDoOutput(true);
        /**necessay**/
        String boundary = UUID.randomUUID().toString();
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);


        Path path = Path.of(apServerComponent.getToken());
        String token = Files.readString(path, StandardCharsets.UTF_8);

        con.setRequestProperty("Authorization",token);

        try (OutputStream out = con.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true)) {
//            writer.append("--").append(boundary).append("\r\n");
//            writer.append("Content-Disposition: form-data; name=\"id\"\r\n\r\n");
//            writer.append(params.getBATCH_ID()).append("\r\n");

//            writer.append("--").append(boundary).append("\r\n");
//            writer.append("Content-Disposition: form-data; name=\"originalFileId\"\r\n\r\n");
//            writer.append(params.getBATCHFORM_ORIGINAL_FILE_ID()).append("\r\n");

//            writer.append("--").append(boundary).append("\r\n");
//            writer.append("Content-Disposition: form-data; name=\"processedCounts\"\r\n\r\n");
//            writer.append("0").append("\r\n");

//            writer.append("--").append(boundary).append("\r\n");
//            writer.append("Content-Disposition: form-data; name=\"status\"\r\n\r\n");
//            writer.append(params.getStatus()).append("\r\n");
            /**取得請求體*/
            String content = getContent(boundary, params);
            writer.append(content);

            if (StringUtils.isNotNullOrEmpty(fileName)) {
                // 如果檔案名稱非空，則將檔案內容寫入請求主體
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
                writer.append("Content-Type: application/zip\r\n\r\n");
                writer.flush();
                log.info("filePath:{}",filePath);
                Files.copy(file.toPath(), out);
                out.flush();
                writer.append("\r\n");
            }
            writer.append("--").append(boundary).append("--").append("\r\n");
            writer.flush();
        }


        int responseCode = con.getResponseCode();
        String responseMessage = con.getResponseMessage();
        log.info("Response Code: " + responseCode);
        log.info("Response Message: " + responseMessage);

        con.disconnect();
        return responseCode;
    }


    private String getFullUrl(StringBuilder url, JobParams jobParams) {
        url.append("?");
        if (jobParams.getBATCH_ID() != null) {
            url.append("id").append("=").append(jobParams.getBATCH_ID()).append(sperator);
        }
        if (jobParams.getBATCHFORM_ORIGINAL_FILE_ID() != null) {
            url.append("originalFileId").append("=").append(jobParams.getBATCHFORM_ORIGINAL_FILE_ID()).append(sperator);
        }
        if (jobParams.getPROCESSED_COUNTS() != null) {
            url.append("processedCounts").append("=").append(jobParams.getPROCESSED_COUNTS()).append(sperator);
        }
        if (jobParams.getStatus() != null) {
            url.append("status").append("=").append(jobParams.getStatus()).append(sperator);
        }
        // 移除最後一個 sperator
        String newUrl = url.toString();
        if (newUrl.endsWith(sperator)) {
            newUrl = newUrl.substring(0, newUrl.length() - sperator.length());
        }
        return newUrl;
    }







    public String getContent(String boundary,Object params) {
        StringBuilder content = new StringBuilder();
        if (params != null) {
            Method[] methods = params.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().startsWith("get") && !method.getName().equals("getClass")) {
                    try {
                        /**調用getter**/
                        Object value = method.invoke(params);
                        if (value != null && !"".equals(value)) {
                            if (method.getName().indexOf("File") == 3) {
                                continue;
                            }else{
                                content.append("--").append(boundary).append("\r\n");
                                String name = method.getName().substring(3);
                                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                                String format = String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n",name);
                                content.append(format);
                                content.append(value).append("\r\n");
                            }
                        }
                    } catch (Exception e) {
                        throw new MoiException("url解析錯誤 " + method.getName() + ": " + e.getMessage(), e);
                    }
                }
            }
        }
        log.info("content:{}",content);
        return content.toString();
    }


}
