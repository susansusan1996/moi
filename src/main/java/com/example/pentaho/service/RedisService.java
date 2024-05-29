package com.example.pentaho.service;

import com.example.pentaho.component.RefreshToken;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.utils.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
public class RedisService {


    private static Logger log = LoggerFactory.getLogger(SingleQueryService.class);

    Integer SCAN_SIZE = 10000;

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
        List<Object> results = stringRedisTemplate1.executePipelined((RedisCallback<List<String>>) connection -> {
            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
            for (String key : keys) {
                stringRedisConn.lRange(key, 0, -1);
            }
            return null;
        });
        for (Object result : results) {
            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> elements = (List<String>) result;
                resultList.addAll(elements);
            }
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

    private static final List<String> KEYWORDS = Arrays.asList(
            "COUNTY", "TOWN", "VILLAGE", "ROAD", "AREA", "LANE", "ALLEY",
            "NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5"
    );

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
                    log.info("redis<有>找到cd代碼，key: {}, value: {}", key, redisSet);
                    String redisValue = String.join(",", redisSet);
                    resultMap.put(key, redisValue);
                    //如果是 "COUNTY", "TOWN", "VILLAGE","ROAD", "AREA", "LANE", "ALLEY",
                    // "NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5"
                    //才需要判斷0或1
                    if(containsKeyword(key)){
                        segmentExistNumberBuilder.append("1");
                    }
                } else {
                    log.info("redis<沒有>找到cd代碼，key: {}", key);
                    //如果找不到，就要用模糊搜尋
                    String[] parts = key.split(":");
                    Set<String> scanSet = new HashSet<>();
                    if (parts.length == 2 && !"null".equals(parts[1])) {
                        scanSet = scanKeysAndReturnSet(key);
                        log.info("模糊搜尋後的value: {}", scanSet);
                    }
                    if (!scanSet.isEmpty()) {
                        String value = String.join(",", scanSet);
                        resultMap.put(key, value); //模糊搜尋有找到
                    } else {
                        resultMap.put(key, keyMap.get(key)); // 如果找不到對應的value，就要放default value
                    }
                    if (containsKeyword(key)) {
                        segmentExistNumberBuilder.append("0");
                    }
                }
            }
        } finally {
            connection.close();
        }
        resultMap.put("segmentExistNumber", segmentExistNumberBuilder.toString());
        return resultMap;
    }


    private Boolean containsKeyword (String key) {
        for (String keyword : KEYWORDS) {
            if (key.split(":")[0].equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    public List<String> findByKeys(Set<String> keys) {
        return stringRedisTemplate1.opsForValue().multiGet(keys.stream().toList());
    }


    /**
     * 模糊比對，找出相符的 KEY (redis: scan)
     */
    public Set<String> findListByScan(List<String> keys) {
        Set<String> keySet = new ConcurrentSkipListSet<>();
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        try {
            List<Future<Set<String>>> futures = executorService.invokeAll(createScanTasks(keys));
            for (Future<Set<String>> future : futures) {
                keySet.addAll(future.get());
            }
        } catch (Exception e) {
            log.error("Error during Redis scan: {}", e.getMessage());
        } finally {
            executorService.shutdown();
        }
        log.info("keySet: {}", keySet);
        return keySet;
    }

    private List<Callable<Set<String>>> createScanTasks(List<String> keys) {
        return keys.stream().map(this::createScanTask).toList();
    }

    private Callable<Set<String>> createScanTask(String key) {
        return () -> stringRedisTemplate1.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> localKeySet = new ConcurrentSkipListSet<>();
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions()
                    .match(key)
                    .count(SCAN_SIZE)
                    .build())) {
                while (cursor.hasNext()) {
                    localKeySet.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                log.error("redis，模糊比對錯誤:{}", e.getMessage());
            }
            return localKeySet;
        });
    }






    /**
     * 模糊比對，找出相符的 KEY (redis: scan)，
     */
    // TODO: 2024/5/14 速度慢 ??，需要優化
    public Set<String> scanKeysAndReturnSet(String key) {
        Set<String> resultSet = new HashSet<>();
        if(key.split(":")[1]!=null){
            // TODO: 2024/5/29 除了 ? 還有方框，帶補上
            // TODO: 2024/5/29 如果沒有?沒有方框，就不知道可以把甚麼字元挖掉用*取代。可能會造成 ex."民哈路"，無法找到 "民生路"
            String scanKey = key.split(":")[0]+ ":*" + key.split(":")[1].replace("?", "*") + "*";
            log.info("replace 問號、方框 後，scanKey: {}", scanKey);
            stringRedisTemplate2.execute((RedisCallback<Void>) connection -> {
                try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(scanKey).count(SCAN_SIZE).build())) {
                    List<String> bestMatches = new ArrayList<>();
                    //JaroWinklerDistance比較scan的key，取最高分的key們的value
                    JaroWinklerDistance distance = new JaroWinklerDistance();
                    double highestScore = 0.0;
                    while (cursor.hasNext()) {
                        byte[] next = cursor.next();
                        String currentKey = new String(next);
                        double score = distance.apply(key, currentKey);
                        // TODO: LOG如果會影響速度，不需要可以拿掉 ><
                        log.info("key:{}，currentKey:{}，score:{}",key,currentKey,score);
                        if (score > highestScore) {
                            highestScore = score;
                            bestMatches.clear();
                            bestMatches.add(currentKey);
                        } else if (score == highestScore) {
                            bestMatches.add(currentKey); //同分數
                        }
                    }
                    for (String match : bestMatches) {
                        resultSet.addAll(getSet(match));
                    }
                }
                return null;
            });
        }
        return resultSet;
    }

    public Set<String> getSet(String key) {
        SetOperations<String, String> setOperations = stringRedisTemplate2.opsForSet();
        return setOperations.members(key);
    }



    public List<String> scanKeysAndReturnList(String pattern) {
        List<String> resultList = new ArrayList<>();
        stringRedisTemplate2.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(SCAN_SIZE).build())) {
                while (cursor.hasNext()) {
                    byte[] next = cursor.next();
                    resultList.addAll(getList(new String(next)));
                }
            }
            return null;
        });
        return resultList;
    }

    public List<String> getList(String key) {
        ListOperations<String, String> listOps = stringRedisTemplate2.opsForList();
        List<String> elements = Optional.ofNullable(listOps.range(key, 0, -1)).orElse(Collections.emptyList());
        log.info("elements: {}", elements);
        return elements;
    }





}
