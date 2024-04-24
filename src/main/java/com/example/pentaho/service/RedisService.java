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
            } else {
                stringRedisTemplate0.delete(id + ":refresh_token");
                stringRedisTemplate0.delete(id + ":refresh_token_expiry_date");
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
}
