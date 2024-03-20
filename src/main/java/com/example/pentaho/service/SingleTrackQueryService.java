package com.example.pentaho.service;


import com.example.pentaho.component.ApServerComponent;
import com.example.pentaho.component.Directory;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.SingleBatchQueryParams;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import com.example.pentaho.utils.StringUtils;
import com.opencsv.CSVReader;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
    public void queryBatchTrack(String fileContent, SingleBatchQueryParams singleBatchQueryParams) throws IOException {
        log.info("singleBatchQueryParams:{}",singleBatchQueryParams.toString());
        try {
            log.info("start processing ,fileContent:{}",fileContent);

            String filePath="";

            String[] lines = fileContent.split("\n");
            if(lines.length <= 0){
                /**檔案沒有內容*/
                log.info("Empty File");
                singleBatchQueryParams.setStatus("REJECT");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }

            String[] newLines = Arrays.copyOfRange(lines, 1, lines.length);
            log.info("newLines:{}",newLines.length);
            if(newLines.length <= 0 ){
                /**去掉表頭檔案沒有內容*/
                log.info("Empty File Content");
                singleBatchQueryParams.setStatus("REJECT");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }


            /**取得乾淨的地址編碼，記得略過表頭**/
            List<String> addressIdList = getAddressList(newLines);
            /**Exception**/
            if(addressIdList == null){
                log.info("Reading Exception");
                singleBatchQueryParams.setStatus("SYS_FAILED");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }
            /**Empty List**/
            if(addressIdList.isEmpty()){
                log.info("Empty Address");
                singleBatchQueryParams.setStatus("REJECT");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }


            List<IbdTbIhChangeDoorplateHis> IbdTbIhChangeDoorplateHisList = ibdTbIhChangeDoorplateHisRepository.findByAddressIdList(addressIdList);
            if(IbdTbIhChangeDoorplateHisList==null){
                log.info("Reading Exception");
                singleBatchQueryParams.setStatus("SYS_FAILED");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }

            if(IbdTbIhChangeDoorplateHisList.isEmpty()){
                log.info("no data was found");
                singleBatchQueryParams.setStatus("DONE");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }

            boolean geneZip = getCSVFileZip(IbdTbIhChangeDoorplateHisList, singleBatchQueryParams.getFile());
            if(geneZip){
                /**成功建檔*/
                singleBatchQueryParams.setStatus("DONE");
                singleBatchQueryParams.setProcessedCounts(String.valueOf(newLines.length));
                filePath = directories.getLocalTempDir()+singleBatchQueryParams.getFile()+".zip";
            }
            postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
        }catch (Exception e){
            log.info("e:{}",e.toString());
            singleBatchQueryParams.setStatus("SYS_FAILED");
            postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,"");
        }
    }

    public List<String> getAddressList(String[] rowsWithoutColNm){
        try{
            HashSet<String> addressIdSet = new HashSet<String>();
            for(String row :rowsWithoutColNm){
                if(StringUtils.isNotNullOrEmpty(row.trim())){
                        String[] split = row.split(",");
                        if(!StringUtil.isNullOrEmpty(split[0])){
                            addressIdSet.add(split[0].trim());
                    }
                }
            }
            return addressIdSet.stream().toList();
        }catch (Exception e){
            log.info("e:{}",e.toString());
            return null;
        }

    }

    public List<String> CSVReader(InputStream inputStream) throws IOException {
        try{
            HashSet<String> addressIdSet = new HashSet<String>();
            CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream));
            String[] line;
            /**跳過表頭**/
            csvReader.readNext();
            while ((line = csvReader.readNext()) != null) {
                /**跳過空行**/
                if (!isNullOrEmpty(line)) {
                    /**格式相符*/
                    if(line.length == 1){
                        log.info("line:{}",line[0]);
                        addressIdSet.add(line[0]);
                    }
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
        log.info("array.length:{}",array.length);
        return array == null || Arrays.stream(array).allMatch(String::isEmpty);
    }

    private static void saveByteArrayToFile(byte[] byteArray, String filePath) throws IOException {
        File file = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            FileCopyUtils.copy(byteArray, fos);
        }
    }


    /**
     * generate CSV File
     * @param IbdTbIhChangeDoorplateHisList
     * @param fileName
     * @return
     */
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

    /**
     * 壓縮檔案
     * @param sourceFilePath
     * @param fileName
     * @return
     */
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

    /**
     * send Request
     * @param action
     * @param params
     * @param filePath
     * @return
     * @throws IOException
     */
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

    /**
     * getPostContent
     * @param boundary
     * @param params
     * @return
     */
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
