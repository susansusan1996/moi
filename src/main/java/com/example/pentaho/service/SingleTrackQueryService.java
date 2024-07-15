package com.example.pentaho.service;


import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import com.example.pentaho.utils.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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


    private final static Logger log = LoggerFactory.getLogger(SingleTrackQueryService.class);

    private final static char[]  inValidatedChars= new char[] {'A','E','I','O','U'};

    private final static ObjectMapper objectMapper = new ObjectMapper();


    private final static CharacterNumMapping characterNumMapping =new CharacterNumMapping();


    @Autowired
    private ApServerComponent apServerComponent;

    @Autowired
    private Directory directories;

    @Autowired
    private IbdTbIhChangeDoorplateHisRepository ibdTbIhChangeDoorplateHisRepository;





    public List<SingleQueryTrackDTO> querySingleTrack(String addressId) {
        log.info("addressId:{}",addressId);
        /*回傳內容*/
        SingleQueryTrackDTO dto = new SingleQueryTrackDTO();
        List result = new ArrayList<SingleQueryTrackDTO>();
        List<IbdTbIhChangeDoorplateHis> byAddressId = ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
        if (byAddressId.isEmpty() || byAddressId == null){
            ArrayList<IbdTbIhChangeDoorplateHis> empty = new ArrayList<>();
            boolean isValidate = checkSum(addressId);
            if(isValidate){
                dto.setText("該筆識別碼無異動軌跡");
            }else{
                dto.setText("檢核為非地址識別碼格式");
            }
            dto.setData(empty);
        }else{
          dto.setText("");
          dto.setData(byAddressId);
        }
        result.add(dto);
        return result;
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
//        log.info("Response Code: " + responseCode);
//        log.info("Response Message: " + responseMessage);

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


    /***
     * BSZ7538-0
     * @param addressId
     * @return
     */
    public boolean checkSum(String addressId){
        boolean result = false;
        try {
            log.info("addressId:{}", addressId);

            //長度低於9不合法
            if(addressId.length() > 9){
                return false;
            }

            //'-' 的位置不合法
            if(addressId.indexOf("-") != 7){
                return false;
            }

            char[] chars = addressId.toCharArray();
            log.info("chars.length:{}",chars.length);

            //取target
           if(!isNumber(addressId.toCharArray()[8])){
                return false;
           }

           int comparation = Integer.parseInt(String.valueOf(addressId.toCharArray()[8]));
           if(comparation < 0 || comparation > 9){
               return false;
           }

            //取mapping
//            String filepath = Thread.currentThread().getClass().getResource("/mapping.json").toString();
//            log.info("filePath:{}",filepath);
//            Path path = ResourceUtils.getFile(filepath).toPath();
//            String content = Files.readString(path);
//            log.info("content:{}",content);

            /*英文對應的數字**/
            List<Integer> checkSums = new ArrayList<>();
            char[] characters = addressId.substring(0, 3).toCharArray();
            for(char w :characters){
                for(Map<String,Integer> pair: characterNumMapping.getCharacterMapping()){
                    String strChar = String.valueOf(w);
                    if(pair.containsKey(strChar)){
                        Integer value = pair.get(strChar);
                        log.info("英文:"+strChar+",對應數字:"+value);
                        checkSums.add(value);
                    }
                   }
                }

            //前三碼一定要對到，只要!=3即不合法
            if(checkSums.size()!=3){
                return false;
            }


            //確認英文後四碼能不能轉為數
            char[] numChars = addressId.substring(3, 7).toCharArray();
            for(char numChar:numChars){
             if(!isNumber(numChar)){
                 //只要一個非數就不合法
                 return false;
             }else{
                 int num = Integer.parseInt(String.valueOf(numChar));
                 //todo:不能超過2位，範圍是0~9嗎?
                 if(num < 0 || num > 9){
                     return false;
                 }
                 log.info("英文後四碼:{}",num);
                 checkSums.add(num);
             }
            }
            //開始計算
            Integer sum = 0;
            sum += checkSums.get(0)*7;
            sum += checkSums.get(1)*6;
            sum += checkSums.get(2)*5;
            sum += checkSums.get(3)*4;
            sum += checkSums.get(4)*3;
            sum += checkSums.get(5)*2;
            sum += checkSums.get(6)*1;

            int sourceValue = sum % 10;
            //sourceValue 找出對應的targetValue後，再與comparation(-0) 相比
            log.info("被除數:{}",sum);
            log.info("餘數:{}",sourceValue);
            int targetVal = -1;
            for(Map<String,Integer> pair:characterNumMapping.getNumMapping()){
               if(pair.containsKey(String.valueOf(sourceValue))){
                   targetVal = pair.get(String.valueOf(sourceValue));
                   break;
               }
            }
            log.info("對應的targetVal:{}",targetVal);
            log.info("comparation:{}",comparation);

            if(targetVal == -1){
                return false;
            }

            if(targetVal == comparation){
                return true;
            }

            return false;

        }catch (Exception e){
            log.info("e:{}",e.toString());
        }
        return result;
    }




    private boolean isNumber(char numChar){
        try{
            Double.parseDouble(String.valueOf(numChar));
            return true;
        }catch (Exception e){
            log.info("e:{}",e.toString());
            return false;
        }
    }

}
