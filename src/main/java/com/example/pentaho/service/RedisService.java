package com.example.pentaho.service;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.SingleQueryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RedisService {


    private static Logger log = LoggerFactory.getLogger(SingleQueryService.class);

    Integer SCAN_SIZE = 1000;

    @Autowired
    @Qualifier("stringRedisTemplate2")
    private StringRedisTemplate stringRedisTemplate2;

    @Autowired
    @Qualifier("stringRedisTemplate1")
    private StringRedisTemplate stringRedisTemplate1;


    /**
     * 找為LIST的值 (redis: LRANGE)
     */
    public List<String> findListByKey(String key) {
        ListOperations<String, String> listOps = stringRedisTemplate2.opsForList();
        List<String> elements = listOps.range(key, 0, -1);
        log.info("elements:{}", elements);
        return elements;
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



    public List<String> findByKeys(Set<String> keys) {
        return stringRedisTemplate2.opsForValue().multiGet(keys);
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


    public Set<String> fuzzySearchMappingId(Address address){
        Map<String, String> map = buildRegexMappingId(address);
        String newMappingId = map.get("newMappingId");
        String regex = map.get("regex");
        log.info("因為地址不完整，組成新的 mappingId {}，以利模糊搜尋", newMappingId);
        log.info("模糊搜尋正則表達式為:{}", regex);
        Set<String> mappingIdSet = findListByScan(newMappingId);
        log.info("mappingIdSet:{}",mappingIdSet);
        Pattern regexPattern = Pattern.compile(String.valueOf(regex));
        Set<String> newMappingIdSet = new HashSet<>();
        for (String newMapping : mappingIdSet) {
            //因為redis的scan命令，無法搭配正則，限制*的位置只能有多少字元，所以要再用java把不符合的mappingId刪掉
            Matcher matcher = regexPattern.matcher(newMapping);
            //有符合的mappingId，才是真正要拿來處理比對代碼的mappingId
            if (matcher.matches()) {
                newMappingIdSet.add(newMapping);
            }
        }
        return mappingIdSet;
    }

    private Map<String, String> buildRegexMappingId(Address address) {
        String segNum = address.getSegmentExistNumber();
        StringBuilder newMappingId = new StringBuilder(); //組給redis的模糊搜尋mappingId ex.10010***9213552**95********
        StringBuilder regex = new StringBuilder();   //組給java的模糊搜尋mappingId ex.10010020017029\d{18}95\d{19}000000\d{5}
        //mappingCount陣列代表，COUNTY_CD要5位元，TOWN_CD要3位元，VILLAGE_CD要3位元，以此類推
        //6,5,4,3,1 分別是NUM_FLR_1~NUM_FLR_5
        int[] mappingCount = {5, 3, 3, 3, 7, 4, 7, 2, 6, 5, 4, 3, 1, 1, 5, 5};
        int sum = 0;
        for (int i = 0; i < segNum.length(); i++) {
            //該欄位是1的情況(找的到的情況)
            if ("1".equals(String.valueOf(segNum.charAt(i)))) {
                newMappingId.append(address.getMappingIdList().get(i));
                regex.append(address.getMappingIdList().get(i));
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
        Map<String, String> map = new HashMap<>();
        map.put("newMappingId", newMappingId.toString()); //帶有**的mappingId(redis模糊搜尋)
        map.put("regex", regex.toString()); //帶有正則的mappingId(java比對redis模糊搜尋出來的結果)
        return map;
    }

}
