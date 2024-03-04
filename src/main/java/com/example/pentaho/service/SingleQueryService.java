package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.repository.IbdTbAddrCodeOfDataStandardRepository;
import com.example.pentaho.repository.IbdTbAddrDataNewRepository;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import com.example.pentaho.utils.AddressParser;
import com.opencsv.CSVReader;
import io.jsonwebtoken.lang.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
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
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.pentaho.utils.NumberParser.replaceWithHalfWidthNumber;

@Service
public class SingleQueryService {

    private static Logger log = LoggerFactory.getLogger(SingleQueryService.class);

    Integer SCAN_SIZE = 1000;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Autowired
    private ApServerComponent apServerComponent;

    @Autowired
    private Directory directories;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IbdTbIhChangeDoorplateHisRepository ibdTbIhChangeDoorplateHisRepository;

    @Autowired
    private IbdTbAddrDataNewRepository ibdTbAddrDataNewRepository;

    @Autowired
    private AddressParser addressParser;

    @Autowired
    private IbdTbAddrCodeOfDataStandardRepository ibdTbAddrCodeOfDataStandardRepository;


    /**
     * 找單一個值 (redis: get)
     */
    public String findByKey(String key, String defaultValue) {
        if (key != null && !key.isEmpty()) {
            String redisValue = stringRedisTemplate.opsForValue().get(key);
            if (redisValue != null && !redisValue.isEmpty()) {
                log.info("redisKey: {} , redisValue: {}", key, redisValue);
                return redisValue;
            }
        }
        return defaultValue;
    }


    /**
     * 找為LIST的值 (redis: LRANGE)
     */
    public List<String> findListByKey(String key) {
        ListOperations<String, String> listOps = stringRedisTemplate.opsForList();
        List<String> elements = listOps.range(key, 0, -1);
        log.info("elements:{}", elements);
        return elements;
    }

    /**
     * 模糊比對，找出相符的 KEY (redis: scan)
     */
    public Set<String> findListByScan(String key) {
        Set<String> keySet = stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keySetTemp = new ConcurrentSkipListSet<>();
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions()
                    .match("*" + key + "*") //模糊比對
                    .count(SCAN_SIZE)
                    .build())) {
                while (cursor.hasNext() && keySetTemp.size() < SCAN_SIZE) {
                    keySetTemp.add(new String(cursor.next(), "utf-8"));
                }
            } catch (Exception e) {
                log.error("redis，模糊比對錯誤:{}", e.getMessage());
            }
            return keySetTemp;
        });
        log.info("keySet:{}", keySet);
        return keySet;
    }


    /**
     * set單一個值 (redis: set)
     */
    public void setData(SingleQueryDTO singleQueryDTO) {
        stringRedisTemplate.opsForValue().set(singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValue());
        log.info("set單一個值，key: {}, value: {}", singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValue());
    }


    /**
     * 一個key塞多筆值(redis: RPUSH)
     */
    public void pushData(SingleQueryDTO singleQueryDTO) {
        stringRedisTemplate.opsForList().rightPushAll(singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValueList());
        log.info("push value to a key，key: {}, value: {}", singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValueList());
    }


    /**
     * 找為SET的值 (redis: SMEMBERS)
     */
    public Set<String> findSetByKey(SingleQueryDTO singleQueryDTO) {
        SetOperations<String, String> setOps = stringRedisTemplate.opsForSet();
        Set<String> elements = setOps.members(singleQueryDTO.getRedisKey());
        log.info("elements:{}", elements);
        return elements;
    }


    public String findJsonTest(SingleQueryDTO singleQueryDTO) {
        return findByKey("1066693", null);
    }

    public String findJson(String originalString) {
        Address address = findMappingId(originalString);
        log.info("mappingId:{}", address.getMappingId());
        String seq = finSeqByMappingIdInRedis(address.getMappingId());
        log.info("seq:{}", seq);
        return ibdTbAddrCodeOfDataStandardRepository.findBySeq(Integer.valueOf(seq));
    }

    public Address findMappingId(String originalString) {
        //切地址
        Address address = addressParser.parseAddress(originalString, null);
        if (address != null) {
            log.info("address:{}", address);
            String county = address.getCounty();
            //如果是別名，要找到正確的名稱

            String countyCd = findByKey(county, null);

            String town = address.getTown();
            String townCd = findByKey(county + ":" + town, "000");

            String village = address.getVillage(); //里
            String villageCd = findByKey(town + ":" + village, "000");

            String neighbor = findNeighborCd(address.getNeighbor()); //鄰

            String road = address.getRoad();
            String area = address.getArea();

            String roadAreaSn = findByKey(replaceWithHalfWidthNumber(road) + (area == null ? "" : area), "0000000");

            String lane = address.getLane(); //巷
            String laneCd = findByKey(replaceWithHalfWidthNumber(lane), "0000");

            String alley = address.getAlley(); //弄
            String subAlley = address.getSubAlley(); //弄
            String alleyIdSn = findByKey(replaceWithHalfWidthNumber(alley) + replaceWithHalfWidthNumber(subAlley), "0000000");

            String numFlr1 = address.getNumFlr1();
            String numFlr1Id = findByKey("NUM_FLR_1:" + deleteBasementString(numFlr1), "000000");

            String numFlr2 = address.getNumFlr2();
            String numFlr2Id = findByKey("NUM_FLR_2:" + deleteBasementString(numFlr2), "00000");

            String numFlr3 = address.getNumFlr3();
            String numFlr3d = findByKey("NUM_FLR_3:" + deleteBasementString(numFlr3), "0000");

            String numFlr4 = address.getNumFlr4();
            String numFlr4d = findByKey("NUM_FLR_4:" + deleteBasementString(numFlr4), "000");

            String numFlr5 = address.getNumFlr5();
            String numFlr5d = findByKey("NUM_FLR_5:" + deleteBasementString(numFlr5), "0");

            String numTypeCd = "95";
            String numFlrId = numFlr1Id + numFlr2Id + numFlr3d + numFlr4d + numFlr5d;
            String room = address.getRoom(); //里
            String roomIdSn = findByKey(replaceWithHalfWidthNumber(room), "00000");
            String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();

            //處理numFlrPos
            String numFlrPos = getNumFlrPos(address);
            log.info("numFlrPos為:{}", numFlrPos);
            address.setMappingId(countyCd + townCd + villageCd + neighbor + roadAreaSn + laneCd + alleyIdSn + numTypeCd +
                    numFlrId + basementStr
                    + numFlrPos
                    + roomIdSn
            );
        }
        return address;
    }

    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(String addressId) {
        log.info("addressId:{}", addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
    }


    public String findNeighborCd(String rawNeighbor) {
        if (rawNeighbor != null && !rawNeighbor.isEmpty()) {
            Pattern pattern = Pattern.compile("\\d+"); //指提取數字
            Matcher matcher = pattern.matcher(replaceWithHalfWidthNumber(rawNeighbor));
            if (matcher.find()) {
                String neighborResult = matcher.group();
                // 往前補零，補到三位數
                String paddedNumber = String.format("%03d", Integer.parseInt(neighborResult));
                log.info("提取的數字部分為：{}", paddedNumber);
                return paddedNumber;
            }
        } else {
            log.info("沒有數字部分");
            return "000";
        }
        return "000";
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
                singleBatchQueryParams.setStatus("REJECT");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }

            /**表頭除外之筆數，空白行也算**/
            singleBatchQueryParams.setProcessedCounts(String.valueOf(newLines.length));

            /**取得地址編碼，記得略過表頭**/
            List<String> addressIdList = CSVReader(file);
            if(addressIdList == null){
                singleBatchQueryParams.setStatus("SYS_FAILED");
                postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
                return;
            }

            List<IbdTbIhChangeDoorplateHis> IbdTbIhChangeDoorplateHisList = ibdTbIhChangeDoorplateHisRepository.findByAddressIdList(addressIdList);
            boolean geneFile = getCSVFile(IbdTbIhChangeDoorplateHisList, singleBatchQueryParams.getFile());
            if(geneFile){
                /**成功建檔*/
                singleBatchQueryParams.setStatus("DONE");
                filePath = directories.getLocalTempDir()+singleBatchQueryParams.getFile();
            }
            postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,filePath);
        }catch (Exception e){
            log.info("e:{}",e.toString());
            singleBatchQueryParams.setStatus("SYS_FAILED");
            postSingleBatchQueryRequest("/batchForm/systemUpdate",singleBatchQueryParams,"");
        }
    }

    private boolean getCSVFile(List<IbdTbIhChangeDoorplateHis> IbdTbIhChangeDoorplateHisList,String fileName) {
        StringBuilder newContent = new StringBuilder();
        try{
            for (IbdTbIhChangeDoorplateHis line : IbdTbIhChangeDoorplateHisList) {
                newContent.append(line.getAddressId()).append(",");
                newContent.append(line.getHisAdr()).append(",");
                newContent.append(line.getWgsX()).append(",");
                newContent.append(line.getWgsY()).append(",");
                newContent.append(line.getUpdateDt()).append(",");
                newContent.append(line.getUpdateType()).append(",").append("\n");
            }
            log.info("newContent:{}",newContent);
                String filePath =  directories.getLocalTempDir()+ fileName;
                saveByteArrayToFile(newContent.toString().getBytes(),filePath);
                return true;
        } catch (Exception e) {
            log.info("e:{}",e.toString());
            return false;
        }
    }

    private static void saveByteArrayToFile(byte[] byteArray, String filePath) throws IOException {
        File file = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            FileCopyUtils.copy(byteArray, fos);
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
//      con.setRequestProperty("Authorization","Bearer eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoie30iLCJqdGkiOiJNVE0yTmpRMk9HWXRPRGcyWWkwME9UTXhMV0UyWlRRdE9URmlNelE1WlRjek5ETTAiLCJleHAiOjE3Mzc2MDE4MzJ9.3ghp8wCHziA6Az9UpS8ssL1d_JB5apN-3pbIV28BWx3bOK-FjRGA9676-EDpqhXrth_Sqln_TFd4wT0RGJ4V1M0RtKXj3EMpFBBV0otdAsgZLm0JcK7LjUrXmWvyfsBcasnHQ83rMo4hE4GeBgXlrhPUlRxnPcVbk4UrVkaMtxyngDfkGpInPJokUWzrScgo7TDA-aKmodw2eZbxYPjGTw1fzXTYHpJC4VNyAYbeGOTd9uMh-cCAyyYMsw__JmkQOAYPpKLnHdyHSb6C8ezxAZJNrI5Rpg4cG0ousXh694IXmixI_R7Q1nVBMFl7GG946fgTO9twiqhuaB64beUILg");

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



    public List<String> CSVReader(MultipartFile file) throws IOException {
        try{
            HashSet<String> addressIdSet = new HashSet<String>();
            CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()));
            String[] line;
            /**跳過表投**/
            csvReader.readNext();
            while ((line = csvReader.readNext()) != null) {
                /**跳過空行**/
                if (!isNullOrEmpty(line)) {
                    log.info("line:{}",line[0]);
                    /**欄位數符合**/
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
}
