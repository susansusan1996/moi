package com.example.pentaho.service;

import com.example.pentaho.component.ApServerComponent;
import com.example.pentaho.component.Directory;
import com.example.pentaho.component.IbdTbAddrStatisticsOverallDev;
import com.example.pentaho.component.JobParams;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.repository.IbdTbAddrStatisticsOverallDevRepository;
import com.example.pentaho.utils.SFTPUtils;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.List;

@Service
public class FileOutputService {

    @Autowired
    private Directory directories;

    @Autowired
    private ApServerComponent apServerComponent;

    @Autowired
    private SFTPUtils sftpUtils;

    @Autowired
    private IbdTbAddrStatisticsOverallDevRepository ibdTbAddrStatisticsOverallDevRepository;

    private final static Logger log = LoggerFactory.getLogger(JobService.class);
    private final String sperator = "&";


    public void etlFinishedAndSendFile(JobParams jobParams) throws IOException {
//        String sourceFilePath =directories.getTarget()+directories.getReceivePath()+

        String sourceFilePath = directories.getTarget() + directories.getEtlOutputFileDirPrefix() + jobParams.getFILE() + ".zip";
        //先落地
        downloadFileFromPentahoServer(jobParams, sourceFilePath);
        postFileToServer(
                directories.getMockEtlSaveFileDirPrefix() +
                        jobParams.getFILE() + ".zip",
                apServerComponent.getTargetUrl(),
                null
        );
    }


    public void downloadFileFromPentahoServer(JobParams jobParams, String sourceFilePath) {
        URL url;
        try {
            url = new URL(sourceFilePath);
            URLConnection connection = url.openConnection();
            String fileName = jobParams.getFILE() + ".zip";
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


    public void postFileToServer(String sourceFilePath, String targetUrl, JobParams jobParams) throws IOException {
        targetUrl = getFullUrl(new StringBuilder(targetUrl), jobParams);
        log.info("targetUrl: {}",targetUrl);
        File file = new File(sourceFilePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + sourceFilePath);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization",apServerComponent.getToken());
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new org.springframework.core.io.ByteArrayResource(Files.readAllBytes(file.toPath())) {
            @Override
            public String getFilename() {
                return file.getName();
            }
        });
//        parts.add("id",jobParams.getBATCH_ID());
//        parts.add("originalFileId",jobParams.getBATCHFORM_ORIGINAL_FILE_ID());
//        parts.add("processedCounts",1);
//        parts.add("status",jobParams.getStatus());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parts, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Void> responseEntity = restTemplate.exchange(targetUrl, HttpMethod.PUT, requestEntity, Void.class, parts);

        HttpStatusCode statusCode = responseEntity.getStatusCode();
        if (statusCode == HttpStatus.OK) {
            log.info("File uploaded successfully.");
        } else {
            log.error("File upload failed. Response Code: {} ", statusCode.value());
        }
    }


    public void sftpDownloadFileAndSend(JobParams jobParams) throws SftpException, IOException {
        log.info("jobParams:{}",jobParams);
        String targetDir = directories.getSendFileDir() + jobParams.getDATA_SRC() + "/" + jobParams.getDATA_DATE()+"/";
        String fileName = jobParams.getFILE() + ".zip";
        sftpUtils.connect();
        boolean hasFile = sftpUtils.listFiles(targetDir,fileName);
        if(!hasFile){
            jobParams.setStatus("找不到檔案_處理錯誤_SYS_FAILED");
        }
        boolean hasDownload = sftpUtils.downloadFile(directories.getLocalTempDir(), targetDir,fileName);
        if(!hasDownload){
            jobParams.setStatus("無法下載_處理錯誤_SYS_FAILED");
        }
        sftpUtils.disconnect();
        jobParams.setStatus("完成_DONE");
        String sourceFilePath = directories.getLocalTempDir()+fileName;
        postFileToServer(sourceFilePath, apServerComponent.getTargetUrl(), jobParams);
    }
    

    public boolean sftpDownloadFile(String batchId){
        log.info("batchId:{}",batchId);
        String fileName = batchId+".csv";
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

    public List<IbdTbAddrStatisticsOverallDev> findLog(String batchId){
        return ibdTbAddrStatisticsOverallDevRepository.findAll();
    }


    /**
     *大量查詢
     * sftp抓檔
     * 撈log
     * post給聖森
     * **/
    public void postFileAndLog(String batchId) throws IOException{
        boolean hasFile = sftpDownloadFile(batchId);
        if(!hasFile) {
            /***/
        }
        List<IbdTbAddrStatisticsOverallDev> logs = findLog(batchId);
        File file = new File(directories.getLocalTempDir()+batchId+".csv");
        if (!file.exists()) {
            throw new IOException("File not found: " + directories.getLocalTempDir()+batchId+".csv");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization",apServerComponent.getToken());
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("etlOutPutFile", new org.springframework.core.io.ByteArrayResource(Files.readAllBytes(file.toPath())) {
            @Override
            public String getFilename() {
                return file.getName();
            }
        });
        parts.add("recordCounts",logs);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parts, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Void> responseEntity = restTemplate.exchange(apServerComponent.getTargetUrl(), HttpMethod.POST, requestEntity, Void.class, parts);

        HttpStatusCode statusCode = responseEntity.getStatusCode();
        if (statusCode == HttpStatus.OK) {
            log.info("File uploaded successfully.");
        } else {
            log.error("File upload failed. Response Code: {} ", statusCode.value());
        }
        
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
