package com.example.pentaho.service;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.JwtReponse;
import com.example.pentaho.component.RefreshToken;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.utils.AddressParser;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.pentaho.utils.NumberParser.*;

@Service
public class RedisService {


    private static Logger log = LoggerFactory.getLogger(SingleQueryService.class);

    Integer SCAN_SIZE = 1000;

    String segmentExistNumber = ""; //紀錄user是否有輸入每個地址片段，有:1，沒有:0

    @Autowired
    @Qualifier("stringRedisTemplate2")
    private StringRedisTemplate stringRedisTemplate2;

    @Autowired
    @Qualifier("stringRedisTemplate1")
    private StringRedisTemplate stringRedisTemplate1;

    @Autowired
    @Qualifier("stringRedisTemplate0")
    private StringRedisTemplate stringRedisTemplate0;


    /**
     * 找為LIST的值 (redis: LRANGE)
     */
    public List<String> findListByKey(String key) {
        log.info("key:{}",key);
        ListOperations<String, String> listOps = stringRedisTemplate1.opsForList();
        List<String> elements = listOps.range(key, 0, -1);
        log.info("elements:{}", elements);
        return elements;
    }


    public List<String> findListsByKeys(List<String> keys) {
        List<String> resultList = new ArrayList<>();
        for (String key : keys) {
            log.info("key: {}", key);
            ListOperations<String, String> listOps = stringRedisTemplate1.opsForList();
            List<String> elements = listOps.range(key, 0, -1);
            log.info("elements: {}", elements);
            resultList.addAll(elements);
//            resultMap.put(key, elements);
        }
        return resultList;
    }


    /**
     * set單一個值 (redis: set)
     */
    public void setData(SingleQueryDTO singleQueryDTO) {
        stringRedisTemplate2.opsForValue().set(singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValue());
        log.info("set單一個值，key: {}, value: {}", singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValue());
    }


    /**
     * 一個key塞多筆值(redis: RPUSH)
     */
    public void pushData(SingleQueryDTO singleQueryDTO) {
        stringRedisTemplate2.opsForList().rightPushAll(singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValueList());
        log.info("push value to a key，key: {}, value: {}", singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValueList());
    }


    /**
     * 找為SET的值 (redis: SMEMBERS)
     */
    public Set<String> findSetByKey(SingleQueryDTO singleQueryDTO) {
        SetOperations<String, String> setOps = stringRedisTemplate2.opsForSet();
        Set<String> elements = setOps.members(singleQueryDTO.getRedisKey());
        log.info("elements:{}", elements);
        return elements;
    }


    /**
     * 找單一個值 (redis: get)
     * 找 mappingId
     */
    public String findByKey(String columnName, String key, String defaultValue) {
        if (key != null && !key.isEmpty()) {
            String redisValue = stringRedisTemplate1.opsForValue().get(key);
            if (redisValue != null && !redisValue.isEmpty()) {
                log.info("columnName:{} , redisKey: {} , redisValue: {}", columnName, key, redisValue);
                return redisValue;
            }
        }
        return defaultValue;
    }


    /**
     * 存refresh_token
     */
    public void saveRefreshToken(RefreshToken refreshToken) {
        if (refreshToken.getToken() != null) {
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":token", refreshToken.getToken());
        }
        if (refreshToken.getRefreshToken() != null) {
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":refresh_token", refreshToken.getRefreshToken());
        }
        if (refreshToken.getExpiryDate() != null) {
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":expiry_date", refreshToken.getExpiryDate());
        }
        if (refreshToken.getRefreshTokenExpiryDate() != null) {
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":refresh_token_expiry_date", refreshToken.getRefreshTokenExpiryDate());
        }
        if (refreshToken.getReviewResult() != null) {
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":review_result", refreshToken.getReviewResult());
        }
        stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":create_timestamp", Instant.now().toString());
    }

    public void updateRefreshTokenByUserId(RefreshToken refreshToken) {
        if (StringUtils.isNotNullOrEmpty(refreshToken.getId())) {
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":token", refreshToken.getToken());
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":refresh_token", refreshToken.getRefreshToken());
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":expiry_date", refreshToken.getExpiryDate().toString());
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":refresh_token_expiry_date", refreshToken.getRefreshTokenExpiryDate().toString());
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":review_result", refreshToken.getReviewResult());
            stringRedisTemplate0.opsForValue().set(refreshToken.getId() + ":create_timestamp", Instant.now().toString());
        }
    }

    public void updateTokenByUserId(String id,JwtReponse response) {
        if (StringUtils.isNotNullOrEmpty(id)) {
            stringRedisTemplate0.opsForValue().set(id + ":token", response.getToken());
            stringRedisTemplate0.opsForValue().set(id + ":expiry_date", response.getExpiryDate());
            stringRedisTemplate0.opsForValue().set(id + ":create_timestamp", Instant.now().toString());
        }
    }



    public RefreshToken findRefreshTokenByUserId(String id) {
        RefreshToken refreshToken = new RefreshToken();
        if (StringUtils.isNotNullOrEmpty(id)) {
            String reviewResult = stringRedisTemplate0.opsForValue().get(id + ":review_result");
            if ("AGREE".equals(reviewResult)) {
                refreshToken.setId(id);
                refreshToken.setToken(stringRedisTemplate0.opsForValue().get(id + ":token"));
                refreshToken.setRefreshToken(stringRedisTemplate0.opsForValue().get(id + ":refresh_token"));
                refreshToken.setExpiryDate(stringRedisTemplate0.opsForValue().get(id + ":expiry_date"));
                refreshToken.setRefreshTokenExpiryDate(stringRedisTemplate0.opsForValue().get(id + ":refresh_token_expiry_date"));
                refreshToken.setReviewResult(stringRedisTemplate0.opsForValue().get(id + ":review_result"));
                return refreshToken;
            }
        }
        return null;
    }

    public void deleteToken(String id, String type) {
        if (StringUtils.isNotNullOrEmpty(id)) {
            //判斷刪哪種token
            if ("token".equals(type)) {
                stringRedisTemplate0.delete(id + ":token");
                stringRedisTemplate0.delete(id + ":expiry_date");
//                stringRedisTemplate0.opsForValue().set(id + ":token", "");
//                stringRedisTemplate0.opsForValue().set(id + ":expiry_date", "");
            } else {
                stringRedisTemplate0.delete(id + ":refresh_token");
                stringRedisTemplate0.delete(id + ":refresh_token_expiry_date");
//                stringRedisTemplate0.opsForValue().set(id + ":refresh_token", "");
//                stringRedisTemplate0.opsForValue().set(id + ":refresh_token_expiry_date", "");
            }
        }
    }





    public Map<String, String> findByKeys(Map<String, String> keyMap, String segmentExistNumber) {
        Map<String, String> resultMap = new HashMap<>();
        List<String> redisKeys = new ArrayList<>(keyMap.keySet());
        List<String> redisValues = stringRedisTemplate2.opsForValue().multiGet(redisKeys);
        for (int i = 0; i < redisKeys.size(); i++) {
            String key = redisKeys.get(i);
            String redisValue = redisValues.get(i);
            if (redisValue != null && !redisValue.isEmpty()) {
                log.info("redisKey: {} , redisValue: {}", key, redisValue);
                resultMap.put(key, redisValue);
                segmentExistNumber += "1";
            } else {
                log.info("redisKey: {} , 找不到redisValue: {}", key, redisValue);
                resultMap.put(key, keyMap.get(key)); //如果找不到對應的value的話，就要放default value
                segmentExistNumber += "0";
            }
        }
        resultMap.put("segmentExistNumber", segmentExistNumber);
        return resultMap;
    }

//    public Map<String, String> findSetByKeys(Map<String, String> keyMap, String segmentExistNumber) {
//        Map<String, String> resultMap = new HashMap<>();
//        List<String> redisKeys = new ArrayList<>(keyMap.keySet());
//        StringBuilder segmentExistNumberBuilder = new StringBuilder(segmentExistNumber);
//        for (String key : redisKeys) {
//            Set<String> redisSet = stringRedisTemplate2.opsForSet().members(key);
//            if (redisSet != null && !redisSet.isEmpty()) {
//                log.info("Found values for redisKey: {}", key);
//                String redisValue = String.join(",", redisSet);
//                resultMap.put(key, redisValue);
//                segmentExistNumberBuilder.append("1");
//            } else {
//                log.info("No values found for redisKey: {}", key);
//                resultMap.put(key, keyMap.get(key)); // 如果找不到对应的value的话，就要放default value
//                segmentExistNumberBuilder.append("0");
//            }
//        }
//        resultMap.put("segmentExistNumber", segmentExistNumberBuilder.toString());
//        return resultMap;
//    }


    public Map<String, String> findSetByKeys(Map<String, String> keyMap, String segmentExistNumber) {
        Map<String, String> resultMap = new HashMap<>();
        List<String> redisKeys = new ArrayList<>(keyMap.keySet());
        StringBuilder segmentExistNumberBuilder = new StringBuilder(segmentExistNumber);
        RedisConnection connection = stringRedisTemplate2.getConnectionFactory().getConnection();
        RedisSerializer<String> serializer = stringRedisTemplate2.getStringSerializer();
        try {
            connection.openPipeline();
            for (String key : redisKeys) {
                connection.sMembers(serializer.serialize(key));
            }
            List<Object> results = connection.closePipeline();

            for (int i = 0; i < results.size(); i++) {
                Set<byte[]> redisSetBytes = (Set<byte[]>) results.get(i);
                Set<String> redisSet = new HashSet<>();
                for (byte[] bytes : redisSetBytes) {
                    redisSet.add(serializer.deserialize(bytes));
                }
                String key = redisKeys.get(i);
                if (!redisSet.isEmpty()) {
                    log.info("Found values for redisKey: {}, value: {}", key, redisSet);
                    String redisValue = String.join(",", redisSet);
                    resultMap.put(key, redisValue);
                    segmentExistNumberBuilder.append("1");
                } else {
                    log.info("No values found for redisKey: {}", key);
                    resultMap.put(key, keyMap.get(key)); // 如果找不到对应的value的话，就要放default value
                    segmentExistNumberBuilder.append("0");
                }
            }
        } finally {
            connection.close();
        }

        resultMap.put("segmentExistNumber", segmentExistNumberBuilder.toString());
        return resultMap;
    }






    public List<String> findByKeys(Set<String> keys) {
        return stringRedisTemplate1.opsForValue().multiGet(keys.stream().toList());
    }


    /**
     * 模糊比對，找出相符的 KEY (redis: scan)
     */
    public Set<String> findListByScan(String key) {
        Set<String> keySet = stringRedisTemplate1.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keySetTemp = new ConcurrentSkipListSet<>();
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions()
                    .match(key) //模糊比對
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


    public Set<String> fuzzySearchMappingId(Address address) {
        Map<String, List<String>> map = buildRegexMappingId(address);
        List<String> newMappingId = map.get("newMappingId");
        List<String> regex = map.get("regex");
        log.info("因為地址不完整，組成新的 mappingId {}，以利模糊搜尋", newMappingId);
        log.info("模糊搜尋正則表達式為:{}", regex);
        Set<String> newMappingIdSet = new HashSet<>();
        for (int i = 0; i < newMappingId.size(); i++) {
            Set<String> mappingIdSet = findListByScan(newMappingId.get(i));
            log.info("mappingIdSet:{}", mappingIdSet);
            Pattern regexPattern = Pattern.compile(String.valueOf(regex.get(i)));
            for (String newMapping : mappingIdSet) {
                //因為redis的scan命令，無法搭配正則，限制*的位置只能有多少字元，所以要再用java把不符合的mappingId刪掉
                Matcher matcher = regexPattern.matcher(newMapping);
                //有符合的mappingId，才是真正要拿來處理比對代碼的mappingId
                if (matcher.matches()) {
                    newMappingIdSet.add(newMapping);
                }
            }
        }
        return newMappingIdSet;
    }

    private Map<String, List<String>> buildRegexMappingId(Address address) {
        List<String> newMappingIdList = new ArrayList<>();
        List<String> regexList = new ArrayList<>();
        String segNum = address.getSegmentExistNumber();
        StringBuilder newMappingId = new StringBuilder(); //組給redis的模糊搜尋mappingId ex.10010***9213552**95********
        StringBuilder regex = new StringBuilder();   //組給java的模糊搜尋mappingId ex.10010020017029\d{18}95\d{19}000000\d{5}
        //mappingCount陣列代表，COUNTY_CD要5位元，TOWN_CD要3位元，VILLAGE_CD要3位元，以此類推
        //6,5,4,3,1 分別是NUM_FLR_1~NUM_FLR_5
        int[] mappingCount = {5, 3, 3, 3, 7, 4, 7, 2, 6, 5, 4, 3, 1, 1, 5, 5};
        int sum = 0;
        for (int j = 0; j < address.getMappingIdList().size(); j++) {
            for (int i = 0; i < segNum.length(); i++) {
                //該欄位是1的情況(找的到的情況)
                if ("1".equals(String.valueOf(segNum.charAt(i)))) {
                    newMappingId.append(address.getMappingIdList().get(j).get(i));
                    regex.append(address.getMappingIdList().get(j).get(i));
                    sum = 0; //歸零
                    //該欄位是0的情況(找不到的情況)
                } else {
                    String segAfter = "";
                    String segBefore = "";
                    //第一碼
                    if (i == 0) {
                        segAfter = String.valueOf(segNum.charAt(i + 1));
                        //如果前後碼也是0的話，表示要相加
                        if ("0".equals(segAfter)) {
                            sum += mappingCount[i];
                        }
                        //後面那碼是1，表示不用相加了
                        else if ("1".equals(segAfter)) {
                            sum += mappingCount[i];
                            regex.append("\\d{").append(sum).append("}");
                            sum = 0; //歸零
                        }
                    }
                    //不是最後一個
                    else if (i != segNum.length() - 1) {
                        segAfter = String.valueOf(segNum.charAt(i + 1));
                        segBefore = String.valueOf(segNum.charAt(i - 1));
                        //如果前後都是是1的話，不用相加
                        if ("1".equals(segBefore) && "1".equals(segAfter)) {
                            sum = 0; //歸零
                            regex.append("\\d{").append(mappingCount[i]).append("}");
                        }
                        //如果前後碼也是0的話，表示要相加
                        else if ("0".equals(segAfter)) {
                            sum += mappingCount[i];
                        }
                        //後面那碼是1，表示不用相加了
                        else if ("1".equals(segAfter)) {
                            sum += mappingCount[i];
                            regex.append("\\d{").append(sum).append("}");
                            sum = 0; //歸零
                        }
                    }
                    //是最後一個
                    else {
                        segBefore = String.valueOf(segNum.charAt(i - 1));
                        //如果前面是0表示最後一碼也要加上去
                        if ("0".equals(segBefore)) {
                            sum += mappingCount[i];
                            regex.append("\\d{").append(sum).append("}");
                        } else {
                            regex.append("\\d{").append(mappingCount[i]).append("}");
                        }
                        sum = 0; //歸零
                    }
                    newMappingId.append("*");
                }
            }
            newMappingIdList.add(newMappingId.toString());
            regexList.add(regex.toString());
        }
        Map<String, List<String>> map = new HashMap<>();
        map.put("newMappingId", newMappingIdList); //帶有**的mappingId(redis模糊搜尋)
        map.put("regex", regexList); //帶有正則的mappingId(java比對redis模糊搜尋出來的結果)
        return map;
    }


    public Address setAddressAndFindCdByRedis(Address address) {
        log.info("address:{}", address);
        segmentExistNumber = ""; //先清空
        //===========取各地址片段===========================
        String county = address.getCounty();
        String town = address.getTown();
        String village = address.getVillage(); //里
        String road = address.getRoad();
        String area = address.getArea();
        String roadAreaKey = replaceWithHalfWidthNumber(road) + (area == null ? "" : area);
        String lane = address.getLane(); //巷
        String alley = address.getAlley(); //弄
        String subAlley = address.getSubAlley(); //弄
        String alleyIdSnKey = replaceWithHalfWidthNumber(alley) + replaceWithHalfWidthNumber(subAlley);
        String numTypeCd = address.getNumTypeCd(); //臨建特附
        //如果有"之45一樓"，要額外處理
        if (StringUtils.isNotNullOrEmpty(address.getContinuousNum())) {
            formatCoutinuousFlrNum(address.getContinuousNum(), address);
        }
        //處理完"之45一樓"，才能拿到正確的各部分地址
        String numFlr1 = address.getNumFlr1();
        String numFlr2 = address.getNumFlr2();
        String numFlr3 = address.getNumFlr3();
        String numFlr4 = address.getNumFlr4();
        String numFlr5 = address.getNumFlr5();
        String room = address.getRoom();//室
        //===========將各地址片段放進map===========================
        Map<String, String> keyMap = new LinkedHashMap<>();
        keyMap.put("COUNTY:" + county, "00000");//5
        keyMap.put("TOWN:" + town, "000");//鄉鎮市區 //3
        keyMap.put("VILLAGE:" + village, "000");//里 //3
        keyMap.put("ROADAREA:" + roadAreaKey, "0000000"); //7
        keyMap.put("LANE:" + replaceWithHalfWidthNumber(lane), "0000");//巷 //4
        keyMap.put("ALLEY:" + alleyIdSnKey, "0000000");//弄 //7
        keyMap.put("NUM_FLR_1:" + removeBasementAndChangeFtoFloor(numFlr1, address, "NUM_FLR_1").getNumFlr1(), "000000"); //6
        keyMap.put("NUM_FLR_2:" + removeBasementAndChangeFtoFloor(numFlr2, address, "NUM_FLR_2").getNumFlr2(), "00000"); //5
        keyMap.put("NUM_FLR_3:" + removeBasementAndChangeFtoFloor(numFlr3, address, "NUM_FLR_3").getNumFlr3(), "0000"); //4
        keyMap.put("NUM_FLR_4:" + removeBasementAndChangeFtoFloor(numFlr4, address, "NUM_FLR_4").getNumFlr4(), "000"); //3
        keyMap.put("NUM_FLR_5:" + removeBasementAndChangeFtoFloor(numFlr5, address, "NUM_FLR_5").getNumFlr5(), "0"); //1
        keyMap.put("ROOM:" + replaceWithHalfWidthNumber(address.getRoom()), "00000"); //5
        //===========把存有各地址片段的map丟到redis找cd碼===========================
        Map<String, String> resultMap = findSetByKeys(keyMap, segmentExistNumber);
        //===========把找到的各地址片段cd碼組裝好===========================
        address.setCountyCd(resultMap.get("COUNTY:" + county));
        address.setTownCd(resultMap.get("TOWN:" + town));
        address.setVillageCd(resultMap.get("VILLAGE:" + village));
        address.setNeighborCd(findNeighborCd(address.getNeighbor()));//鄰
        if (StringUtils.isNullOrEmpty(roadAreaKey)) {
            address.setRoadAreaSn("0000000");
            //"沒有"填寫"路地名"先註記起來
            address.setHasRoadArea(false);
        } else {
            address.setRoadAreaSn(resultMap.get("ROADAREA:" + roadAreaKey));
            //"有"填寫"路地名"先註記起來
            address.setHasRoadArea(true);
        }
        address.setLaneCd(resultMap.get("LANE:" + replaceWithHalfWidthNumber(lane)));
        address.setAlleyIdSn(resultMap.get("ALLEY:" + alleyIdSnKey));
        address.setNumFlr1Id(setNumFlrId(resultMap, address, "NUM_FLR_1"));
        address.setNumFlr2Id(setNumFlrId(resultMap, address, "NUM_FLR_2"));
        address.setNumFlr3Id(setNumFlrId(resultMap, address, "NUM_FLR_3"));
        address.setNumFlr4Id(setNumFlrId(resultMap, address, "NUM_FLR_4"));
        address.setNumFlr5Id(setNumFlrId(resultMap, address, "NUM_FLR_5"));
        String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();
        //===========處理numFlrPos===========================
        String numFlrPos = getNumFlrPos(address);
        address.setNumFlrPos(numFlrPos);
        address.setRoomIdSn(resultMap.get("ROOM:" + replaceWithHalfWidthNumber(room)));
        log.info("=== getCountyCd:{}", address.getCountyCd());
        log.info("=== getTownCd:{}", address.getTownCd());
        log.info("=== getVillageCd:{}", address.getVillageCd());
        log.info("=== getNeighborCd:{}", address.getNeighborCd());
        log.info("=== getRoadAreaSn:{}", address.getRoadAreaSn());
        log.info("=== getLaneCd:{}", address.getLaneCd());
        log.info("=== getAlleyIdSn:{}", address.getAlleyIdSn());
        log.info("=== numTypeCd:{}", numTypeCd);
        log.info("=== getNumFlr1Id:{}", address.getNumFlr1Id());
        log.info("=== getNumFlr2Id:{}", address.getNumFlr2Id());
        log.info("=== getNumFlr3Id:{}", address.getNumFlr3Id());
        log.info("=== getNumFlr4Id:{}", address.getNumFlr4Id());
        log.info("=== getNumFlr5Id:{}", address.getNumFlr5Id());
        log.info("=== basementStr:{}", basementStr);
        log.info("=== numFlrPos:{}", address.getNumFlrPos());
        log.info("=== getRoomIdSn:{}", address.getRoomIdSn());
        assembleMultiMappingId(address);
        address.setSegmentExistNumber(insertCharAtIndex(resultMap.getOrDefault("segmentExistNumber", ""), address));
        return address;
    }


    //處理: 之45一樓 (像這種連續的號碼，就會被歸在這裡)
    public void formatCoutinuousFlrNum(String input, Address address) {
        if (StringUtils.isNotNullOrEmpty(input)) {
            String firstPattern = "(?<coutinuousNum1>[之-]+[\\d\\uFF10-\\uFF19]+)(?<coutinuousNum2>\\D+[之樓FｆＦf])?"; //之45一樓
            String secondPattern = "(?<coutinuousNum1>[之-]\\D+)(?<coutinuousNum2>[\\d\\uFF10-\\uFF19]+[之樓FｆＦf])?"; //之四五1樓
            Matcher matcherFirst = Pattern.compile(firstPattern).matcher(input);
            Matcher matcherSecond = Pattern.compile(secondPattern).matcher(input);
            String[] flrArray = {address.getNumFlr1(), address.getNumFlr2(), address.getNumFlr3(), address.getNumFlr4(), address.getNumFlr5()};
            int count = 0;
            for (int i = 0; i < flrArray.length; i++) {
                if (StringUtils.isNullOrEmpty(flrArray[i])) {
                    log.info("目前最大到:{}，新分解好的flr，就要再往後塞到:{}", "numFlr" + (i), "numFlr" + (i + 1));
                    count = i + 1;
                    break;
                }
            }
            if (matcherFirst.matches()) {
                setFlrNum(count, matcherFirst.group("coutinuousNum1"), matcherFirst.group("coutinuousNum2"), address);
            } else if (matcherSecond.matches()) {
                setFlrNum(count, matcherFirst.group("coutinuousNum1"), matcherFirst.group("coutinuousNum2"), address);
            }
        }
    }

    private void setFlrNum(int count, String first, String second, Address address) {
        first = replaceWithHalfWidthNumber(first);
        second = replaceWithHalfWidthNumber(second);
        log.info("first:{},second:{}", first, second);
        switch (count) {
            case 1:
                address.setNumFlr1(first);
                address.setNumFlr2(second);
                address.setNumFlr3(address.getAddrRemains()); //剩下跑到remain的就塞到最後
                break;
            case 2:
                address.setNumFlr2(first);
                address.setNumFlr3(second);
                address.setNumFlr4(address.getAddrRemains());//剩下跑到remain的就塞到最後
                break;
            case 3:
                address.setNumFlr3(first);
                address.setNumFlr4(second);
                address.setNumFlr5(address.getAddrRemains());//剩下跑到remain的就塞到最後
                break;
            case 4:
                address.setNumFlr4(first);
                address.setNumFlr5(second);
                break;
        }
    }

    //補"segmentExistNumber"
    private String insertCharAtIndex(String segmentExistNumber, Address address) {
        StringBuilder stringBuilder = new StringBuilder(segmentExistNumber);
        //鄰
        if ("000".equals(address.getNeighborCd())) {
            stringBuilder.insert(3, '0');  //鄰找不到
        } else {
            stringBuilder.insert(3, '1');  //鄰找的到
        }
        stringBuilder.insert(7, '0');  //numTypeCd一律當作找不到，去模糊比對
        stringBuilder.insert(13, '0'); //basementStr一律當作找不到，去模糊比對
        stringBuilder.insert(14, '0'); //numFlrPos一律當作找不到，去模糊比對
        String result = stringBuilder.toString();
        log.info("segmentExistNumber: {}", result);
        return result;
    }


    //把為了識別是basement的字眼拿掉、將F轉換成樓、-轉成之
    public Address removeBasementAndChangeFtoFloor(String rawString, Address address, String flrType) {
        if (rawString != null) {
            //convertFToFloorAndHyphenToZhi: 將F轉換成樓，-轉成之
            //replaceWithHalfWidthNumber: 把basement拿掉
            String result = convertFToFloorAndHyphenToZhi(replaceWithHalfWidthNumber(rawString).replace("basement:", ""));
            AddressParser addressParser = new AddressParser();
            Map<String, Object> resultMap = addressParser.parseNumFlrAgain(result, flrType);
            Boolean isParsed = (Boolean) resultMap.get("isParsed"); //是否有再裁切過一次FLRNUM (有些地址還是會有1之10樓連再一起的狀況)
            String numFlrFirst = (String) resultMap.get("numFlrFirst");
            String numFlrSecond = (String) resultMap.get("numFlrSecond");
            String type = (String) resultMap.get("flrType");
            switch (flrType) {
                case "NUM_FLR_1":
                    if (isParsed && type != null && type.equals(flrType)) {
                        log.info("NUM_FLR_1，xxxxxxxx");
                        address.setNumFlr1(numFlrFirst);
                        address.setNumFlr2(numFlrSecond);
                    } else if (StringUtils.isNotNullOrEmpty(result)) {
                        address.setNumFlr1(result);
                    }
                    break;
                case "NUM_FLR_2":
                    if (isParsed && type != null && type.equals(flrType)) {
                        log.info("NUM_FLR_2，xxxxxxxx");
                        address.setNumFlr2(numFlrFirst);
                        address.setNumFlr3(numFlrSecond);
                    } else if (StringUtils.isNotNullOrEmpty(result)) {
                        address.setNumFlr2(result);
                    }
                    break;
                case "NUM_FLR_3":
                    if (isParsed && type != null && type.equals(flrType)) {
                        log.info("NUM_FLR_3，xxxxxxxx");
                        address.setNumFlr3(numFlrFirst);
                        address.setNumFlr4(numFlrSecond);
                    } else if (StringUtils.isNotNullOrEmpty(result)) {
                        address.setNumFlr3(result);
                    }
                    break;
                case "NUM_FLR_4":
                    if (isParsed && type != null && type.equals(flrType)) {
                        log.info("NUM_FLR_4，xxxxxxxx");
                        address.setNumFlr4(numFlrFirst);
                        address.setNumFlr5(numFlrSecond);
                    } else if (StringUtils.isNotNullOrEmpty(result)) {
                        address.setNumFlr4(result);
                    }
                    break;
                case "NUM_FLR_5":
                    if (isParsed && type != null && type.equals(flrType)) {
                        log.info("NUM_FLR_5，xxxxxxxx");
                        address.setNumFlr5(numFlrFirst);
                        address.setAddrRemains(numFlrSecond);
                    } else if (StringUtils.isNotNullOrEmpty(result)) {
                        address.setNumFlr5(result);
                    }
                    break;
            }
            return address;
        }
        return address;
    }

    //找numFlrId，如果redis裡找不到的，就直接看能不能抽取數字部分，前面補0
    public String setNumFlrId(Map<String, String> resultMap, Address address, String flrType) {
        String result = "";
        address = removeBasementAndChangeFtoFloor(getNumFlrByType(address, flrType), address, flrType);
        String numericPart = replaceWithHalfWidthNumber(extractNumericPart(getNumFlrByType(address, flrType)));
        switch (flrType) {
            case "NUM_FLR_1":
                result = resultMap.get(flrType + ":" + address.getNumFlr1());
                return getResult(result, "000000", numericPart);
            case "NUM_FLR_2":
                result = resultMap.get(flrType + ":" + address.getNumFlr2());
                return getResult(result, "00000", numericPart);
            case "NUM_FLR_3":
                result = resultMap.get(flrType + ":" + address.getNumFlr3());
                return getResult(result, "0000", numericPart);
            case "NUM_FLR_4":
                result = resultMap.get(flrType + ":" + address.getNumFlr4());
                return getResult(result, "000", numericPart);
            case "NUM_FLR_5":
                result = resultMap.get(flrType + ":" + address.getNumFlr5());
                return getResult(result, "0", numericPart);
            default:
                return result;
        }
    }

    private String getResult(String result, String comparisonValue, String numericPart) {
        if (comparisonValue.equals(result)) {
            return padNumber(comparisonValue, numericPart);
        } else {
            return result;
        }
    }

    public String findNeighborCd(String rawNeighbor) {
        if (StringUtils.isNotNullOrEmpty(rawNeighbor)) {
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

    public String getNumFlrPos(Address address) {
        String[] patternFlr1 = {".+號$", ".+樓$", ".+之$"};
        String[] patternFlr2 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^之.+號", "^[A-ZＡ-Ｚ]+$"};
        String[] patternFlr3 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^[0-9０-９a-zA-Zａ-ｚＡ-Ｚ一二三四五六七八九東南西北甲乙丙]+$"};
        String[] patternFlr4 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^[0-9０-９a-zA-Zａ-ｚＡ-Ｚ一二三四五六七八九東南西北甲乙丙]+$"};
        String[] patternFlr5 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^[0-9０-９a-zA-Zａ-ｚＡ-Ｚ一二三四五六七八九東南西北甲乙丙]+$"};
        return getNum(address.getNumFlr1(), patternFlr1) + getNum(address.getNumFlr2(), patternFlr2) +
                getNum(address.getNumFlr3(), patternFlr3) + getNum(address.getNumFlr4(), patternFlr4) +
                getNum(address.getNumFlr5(), patternFlr5);
    }

    private String getNum(String inputString, String[] patternArray) {
        if (inputString != null && !inputString.isEmpty()) {
            for (int i = 0; i < patternArray.length; i++) {
                Pattern pattern = Pattern.compile(patternArray[i]);
                Matcher matcher = pattern.matcher(inputString);
                if (matcher.matches()) {
                    return String.valueOf(i + 1);
                }
            }
        } else {
            return "0"; //如果沒有該片段地址，就補0
        }
        return "0";
    }

    private String getNumFlrByType(Address address, String flrType) {
        switch (flrType) {
            case "NUM_FLR_1":
                return address.getNumFlr1();
            case "NUM_FLR_2":
                return address.getNumFlr2();
            case "NUM_FLR_3":
                return address.getNumFlr3();
            case "NUM_FLR_4":
                return address.getNumFlr4();
            case "NUM_FLR_5":
                return address.getNumFlr5();
            default:
                return "";
        }
    }

    //因為town、village、road、area可能會有同名，但不同代碼的狀況，要組出不同的mappingId
    private void assembleMultiMappingId(Address address) {
        String numTypeCd = address.getNumTypeCd(); //臨建特附
        String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();
        List<String> townCds = new ArrayList<>();
        List<String> villageCds = new ArrayList<>();
        List<String> roadAreaCds = new ArrayList<>();
        townCds.addAll(splitAndAddToList(address.getTownCd()));
        villageCds.addAll(splitAndAddToList(address.getVillageCd()));
        roadAreaCds.addAll(splitAndAddToList(address.getRoadAreaSn()));
        List<LinkedHashMap<String, String>> mappingIdMapList = new ArrayList<>();
        List<List<String>> mappingIdListCollection = new ArrayList<>();
        List<String> mappingIdStringList = new ArrayList<>();
        for (String townCd : townCds) {
            for (String villageCd : villageCds) {
                for (String roadAreaCd : roadAreaCds) {
                    LinkedHashMap<String, String> mappingIdMap = new LinkedHashMap<>();
                    mappingIdMap.put("COUNTY", address.getCountyCd());
                    mappingIdMap.put("TOWN", townCd);
                    mappingIdMap.put("VILLAGE", villageCd);//里
                    mappingIdMap.put("NEIGHBOR", address.getNeighborCd());
                    mappingIdMap.put("ROADAREA", roadAreaCd);
                    mappingIdMap.put("LANE", address.getLaneCd());//巷
                    mappingIdMap.put("ALLEY", address.getAlleyIdSn());//弄
                    mappingIdMap.put("NUMTYPE", numTypeCd);
                    mappingIdMap.put("NUM_FLR_1", address.getNumFlr1Id());
                    mappingIdMap.put("NUM_FLR_2", address.getNumFlr2Id());
                    mappingIdMap.put("NUM_FLR_3", address.getNumFlr3Id());
                    mappingIdMap.put("NUM_FLR_4", address.getNumFlr4Id());
                    mappingIdMap.put("NUM_FLR_5", address.getNumFlr5Id());
                    mappingIdMap.put("BASEMENT", basementStr);
                    mappingIdMap.put("NUMFLRPOS", address.getNumFlrPos());
                    mappingIdMap.put("ROOM", address.getRoomIdSn());
                    List<String> mappingIdList = Stream.of(
                                    address.getCountyCd(), townCd, villageCd, address.getNeighborCd(),
                                    roadAreaCd, address.getLaneCd(), address.getAlleyIdSn(), numTypeCd,
                                    address.getNumFlr1Id(), address.getNumFlr2Id(), address.getNumFlr3Id(), address.getNumFlr4Id(),
                                    address.getNumFlr5Id(), basementStr, address.getNumFlrPos(), address.getRoomIdSn())
                            .map(Object::toString)
                            .collect(Collectors.toList());
                    // 將NUMFLRPOS為00000的組合也塞進去
                    String oldPos = mappingIdMap.get("NUMFLRPOS");
                    mappingIdStringList.add(replaceNumFlrPosWithZero(mappingIdMap));
                    mappingIdMap.put("NUMFLRPOS", oldPos); //還原
                    mappingIdMapList.add(mappingIdMap);
                    mappingIdListCollection.add(mappingIdList);
                    mappingIdStringList.add(String.join("", mappingIdList));
                }
            }
        }
        address.setMappingIdMap(mappingIdMapList);
        address.setMappingIdList(mappingIdListCollection);
        address.setMappingId(mappingIdStringList);
    }


    private static List<String> splitAndAddToList(String input) {
        List<String> result = new ArrayList<>();
        if (input.contains(",")) {
            result.addAll(Arrays.asList(input.split(",")));
        } else {
            result.add(input);
        }
        return result;
    }


    private String replaceNumFlrPosWithZero(Map<String,String> mappingIdMap){
        StringBuilder sb = new StringBuilder();
        // 將NUMFLRPOS為00000的組合也塞進去
        mappingIdMap.put("NUMFLRPOS", "00000");
        for (Map.Entry<String, String> entry : mappingIdMap.entrySet()) {
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

}
