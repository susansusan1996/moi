package com.example.pentaho.service;


import com.example.pentaho.component.ApServerComponent;
import com.example.pentaho.component.Directory;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.SingleBatchQueryParams;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.ResultSetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SingleTrackQueryService {


    private static Logger log = LoggerFactory.getLogger(SingleTrackQueryService.class);


    @Autowired
    private ApServerComponent apServerComponent;

    @Autowired
    private Directory directories;

    @Autowired
    private IbdTbIhChangeDoorplateHisRepository ibdTbIhChangeDoorplateHisRepository;


    public List<IbdTbIhChangeDoorplateHis> querySingleTrack(String addressId) {
        log.info("addressId:{}",addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
    }


    @Async
    public void queryBatchTrack(MultipartFile file, SingleBatchQueryParams singleBatchQueryParams) throws IOException {
        log.info("singleBatchQueryParams:{}",singleBatchQueryParams.toString());
        try {
            String filePath="";
            String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            int lastIndexOf = fileContent.lastIndexOf("\n");
            fileContent = fileContent.substring(0,lastIndexOf-1);
            log.info("fileContent:{}",fileContent);
            String[] lines = fileContent.split("\n");
            if(lines.length <= 0){
                /**檔案沒有內容*/
                singleBatchQueryParams.setStatus("REJECT");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }
            String[] newLines = Arrays.copyOfRange(lines, 1, lines.length);
            log.info("newLines:{}",newLines.length);
            if(newLines.length <= 0){
                /**去掉表頭檔案沒有內容*/
                singleBatchQueryParams.setStatus("REJECT");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }

            /**表頭除外之筆數，中有空白也算**/
            singleBatchQueryParams.setProcessedCounts(String.valueOf(newLines.length));

            /**取得乾淨的地址編碼，記得略過表頭**/
            List<String> addressIdList = CSVReader(file);
            if(addressIdList == null){
                singleBatchQueryParams.setStatus("SYS_FAILED");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }

            List<IbdTbIhChangeDoorplateHis> IbdTbIhChangeDoorplateHisList = ibdTbIhChangeDoorplateHisRepository.findByAddressIdList(addressIdList);
            boolean geneZip = getCSVFileZip(IbdTbIhChangeDoorplateHisList, singleBatchQueryParams.getFile());
            if(geneZip){
                /**成功建檔，沒有筆數給空檔嗎?*/
                singleBatchQueryParams.setStatus("DONE");
                filePath = directories.getLocalTempDir()+singleBatchQueryParams.getFile()+".zip";
            }
            postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
        }catch (Exception e){
            log.info("e:{}",e.toString());
            singleBatchQueryParams.setStatus("SYS_FAILED");
            postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,"");
        }
    }


    public List<String> CSVReader(MultipartFile file) throws IOException {
        try{
            HashSet<String> addressIdSet = new HashSet<String>();
            CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()));
            String[] line;
            /**跳過表頭**/
            csvReader.readNext();
            while ((line = csvReader.readNext()) != null) {
                /**跳過空行**/
                if (!isNullOrEmpty(line)) {
                    log.info("line:{}",line[0]);
                    addressIdSet.add(line[0]);
                }
            }
            return addressIdSet.stream().toList();
        }catch (Exception e){
            log.info("e:{}",e.toString());
            return null;
        }
    }


    /**判断陣列是否為空或元素都為空字串**/
    private boolean isNullOrEmpty(String[] array) {
        return array == null || Arrays.stream(array).allMatch(String::isEmpty);
    }

    private static void saveByteArrayToFile(byte[] byteArray, String filePath) throws IOException {
        File file = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            FileCopyUtils.copy(byteArray, fos);
        }
    }


    private boolean getCSVFileZip(List<IbdTbIhChangeDoorplateHis> IbdTbIhChangeDoorplateHisList,String fileName) {
        log.info("fileName:{}",fileName);
        StringBuilder newContent = new StringBuilder();
        try{
            for (IbdTbIhChangeDoorplateHis line : IbdTbIhChangeDoorplateHisList) {
                newContent.append(line.getAddressId()).append(",");
                newContent.append(line.getHisAdr()).append(",");
                newContent.append(line.getWgsX()).append(",");
                newContent.append(line.getWgsY()).append(",");
                newContent.append(line.getUpdateDt()).append(",");
                newContent.append(line.getUpdateType()).append("\n");
            }
            log.info("newContent:{}",newContent);
            String filePath =  directories.getLocalTempDir()+ fileName+".csv";
            saveByteArrayToFile(newContent.toString().getBytes(),filePath);
            return FileZipper(filePath,fileName);
        } catch (Exception e) {
            log.info("e:{}",e.toString());
            return false;
        }
    }

        public boolean FileZipper(String sourceFilePath,String fileName) {
            String zipFilePath = directories.getLocalTempDir()+ fileName +".zip";
            log.info("zipFilePath:{}",zipFilePath);
            try {
                FileOutputStream fos = new FileOutputStream(zipFilePath);
                ZipOutputStream zos = new ZipOutputStream(fos);
                FileInputStream fis = new FileInputStream(sourceFilePath);
                zos.putNextEntry(new ZipEntry(fileName+".csv"));

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }

                fis.close();
                zos.closeEntry();
                zos.close();
                return true;
            } catch (IOException e) {
              log.info("e:{}",e.toString());
              return false;
            }
        }

    public int postSingleBatchQueryRequest(String action, Object params, String filePath) throws IOException {
        String targerUrl = apServerComponent.getTargetUrl() + action;
        log.info("targetUrl:{}",targerUrl);

        File file = null;
        String fileName ="";
        if(!"".equals(filePath)){
            file = new File(filePath);
            if (file.exists()) {
                fileName = String.valueOf(Path.of(filePath).getFileName());
                log.info("fileName:{}",fileName);
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
            String content = getContent(boundary, params);
            writer.append(content);

            if (!"".equals(fileName)) {
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
