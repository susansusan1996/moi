package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.repository.IbdTbAddrStatisticsOverallDevRepository;
import com.example.pentaho.utils.ResourceUtils;
import com.example.pentaho.utils.SFTPUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


import java.io.*;
import java.net.*;
import java.nio.file.Files;

import java.util.List;


@Service
public class FileOutputService {


    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private Directory directories;

    @Autowired
    private ApServerComponent apServerComponent;

    @Autowired
    private SFTPUtils sftpUtils;

    @Autowired
    private ResourceUtils resourceUtils;

    @Autowired
    private IbdTbAddrStatisticsOverallDevRepository ibdTbAddrStatisticsOverallDevRepository;

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
     * @param sourceFilePath
     * @param action
     * @param jobParams
     * @return
     * @throws IOException
     */
    public int postBatchFormRequest(String sourceFilePath, String action, JobParams jobParams) throws IOException {
        String targetUrl = apServerComponent.getTargetUrl()+ action;
        log.info("targetUrl: {}",targetUrl);
        HttpHeaders headers = new HttpHeaders();
        /****/
        headers.set("Authorization","Bearer eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoie30iLCJqdGkiOiJNVE0yTmpRMk9HWXRPRGcyWWkwME9UTXhMV0UyWlRRdE9URmlNelE1WlRjek5ETTAiLCJleHAiOjE3Mzc2MDE4MzJ9.3ghp8wCHziA6Az9UpS8ssL1d_JB5apN-3pbIV28BWx3bOK-FjRGA9676-EDpqhXrth_Sqln_TFd4wT0RGJ4V1M0RtKXj3EMpFBBV0otdAsgZLm0JcK7LjUrXmWvyfsBcasnHQ83rMo4hE4GeBgXlrhPUlRxnPcVbk4UrVkaMtxyngDfkGpInPJokUWzrScgo7TDA-aKmodw2eZbxYPjGTw1fzXTYHpJC4VNyAYbeGOTd9uMh-cCAyyYMsw__JmkQOAYPpKLnHdyHSb6C8ezxAZJNrI5Rpg4cG0ousXh694IXmixI_R7Q1nVBMFl7GG946fgTO9twiqhuaB64beUILg");
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        if(!"".equals(sourceFilePath)){
            File file = new File(sourceFilePath);
        if (file.exists()) {
        parts.set("file", new org.springframework.core.io.ByteArrayResource(Files.readAllBytes(file.toPath())) {
            @Override
            public String getFilename() {
                return file.getName();
            }
        });
        }
        }
        parts.add("file",null);
        parts.add("id",jobParams.getBATCH_ID());
        parts.add("originalFileId",jobParams.getBATCHFORM_ORIGINAL_FILE_ID());
        parts.add("processedCounts","0");
        parts.add("status",jobParams.getStatus());
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
        String targetDir = directories.getSendFileDir() + jobParams.getDATA_SRC() + "/" + jobParams.getDATA_DATE()+"/";
        String fileName = jobParams.getFORM_NAME() + ".zip";
        log.info("targetDir:{}",targetDir);
        log.info("fileName:{}",fileName);
        /**SFTP抓檔落地**/
        String sourceFilePath = "";
        String status="SYS_FAILED";
        sftpUtils.connect();
        boolean hasFile = sftpUtils.listFiles(targetDir,fileName);
        if(hasFile){
            boolean hasDownload = sftpUtils.downloadFile(directories.getLocalTempDir(), targetDir,fileName);
            if(hasDownload){
                status ="DONE";
                sourceFilePath= directories.getLocalTempDir()+fileName;
            }
        }
        jobParams.setStatus(status);
        sftpUtils.disconnect();
        postBatchFormRequest(sourceFilePath,"/batchForm/systemUpdate",jobParams);
    }

    /**
     * 等邏輯
     * @param batchId
     * @return
     */
    public List<IbdTbAddrStatisticsOverallDev> findLog(String batchId){
        return ibdTbAddrStatisticsOverallDevRepository.findAll();
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
            sftpUtils.connect();
            result = sftpUtils.downloadFile(directories.getLocalTempDir(),directories.getBigDataSendFileDir(),fileName);
        }catch (Exception e){
            log.info("e:{}",e.toString());
        }
        sftpUtils.disconnect();
        return result;
    }



    /**
     *  post給聖森
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
        headers.set("Authorization","Bearer eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoie30iLCJqdGkiOiJNVE0yTmpRMk9HWXRPRGcyWWkwME9UTXhMV0UyWlRRdE9URmlNelE1WlRjek5ETTAiLCJleHAiOjE3Mzc2MDE4MzJ9.3ghp8wCHziA6Az9UpS8ssL1d_JB5apN-3pbIV28BWx3bOK-FjRGA9676-EDpqhXrth_Sqln_TFd4wT0RGJ4V1M0RtKXj3EMpFBBV0otdAsgZLm0JcK7LjUrXmWvyfsBcasnHQ83rMo4hE4GeBgXlrhPUlRxnPcVbk4UrVkaMtxyngDfkGpInPJokUWzrScgo7TDA-aKmodw2eZbxYPjGTw1fzXTYHpJC4VNyAYbeGOTd9uMh-cCAyyYMsw__JmkQOAYPpKLnHdyHSb6C8ezxAZJNrI5Rpg4cG0ousXh694IXmixI_R7Q1nVBMFl7GG946fgTO9twiqhuaB64beUILg");
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
     * 大量查詢
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
        List<IbdTbAddrStatisticsOverallDev> logs = findLog(bigDataParams.getId());
        bigDataParams.setRecordCounts(logs.isEmpty()?"0":String.valueOf(logs.size()));
        File file = new File(fileUri);
        if (!file.exists()) {
            bigDataParams.setFileUri("no exist");
        }
        bigDataParams.setFileUri(fileUri);
        return postBigQueryResquest(sourceFilePath,"/bigQueryForm/systemUpdate",bigDataParams);
    }

    private String getFullUrl(StringBuilder url, JobParams jobParams) {
        url.append("?");
        if (jobParams.getBATCH_ID() != null) {
            url.append("id").append("=").append(jobParams.getBATCH_ID()).append(sperator);
        }
        if (jobParams.getBATCHFORM_ORIGINAL_FILE_ID() != null) {
            url.append("originalFileId").append("=").append(jobParams.getBATCHFORM_ORIGINAL_FILE_ID()).append(sperator);
        }
        if (jobParams.getProcessedCounts() != null) {
            url.append("processedCounts").append("=").append(jobParams.getProcessedCounts()).append(sperator);
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

}
