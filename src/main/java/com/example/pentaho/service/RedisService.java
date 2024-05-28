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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.pentaho.utils.NumberParser.*;

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

    /***
     * segmentExistNumber 以0,1組成一串長度為8的數字，用來判斷以下欄位是否填寫
     *  "NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5" -> 為一組，其中一個有寫就好
     */
    private static final List<String> KEYWORDS = Arrays.asList(
            "COUNTY", "TOWN", "VILLAGE", "ROAD", "AREA", "LANE", "ALLEY",
            "NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5"
    );

    /***
     * keyMap
     * @param keyMap {
     *            "COUNTY:新北市":"00000",
     *            "TOWN:新莊區":"000",..
     *         }
     * @param segmentExistNumber
     * @return
     */
    public Map<String, String> findSetByKeys(Map<String, String> keyMap, String segmentExistNumber) {
        Map<String, String> resultMap = new HashMap<>();
        //redisKeys = ["COUNTY:新北市","TOWN:新莊區",...]
        List<String> redisKeys = new ArrayList<>(keyMap.keySet());
        //
        StringBuilder segmentExistNumberBuilder = new StringBuilder(segmentExistNumber);
        //
        RedisConnection connection = stringRedisTemplate2.getConnectionFactory().getConnection();
        RedisSerializer<String> serializer = stringRedisTemplate2.getStringSerializer();
        try {
            connection.openPipeline();
            //
            for (String key : redisKeys) {
                //Set
                connection.sMembers(serializer.serialize(key));
            }
            List<Object> results = connection.closePipeline();


            for (int i = 0; i < results.size(); i++) {
                //到這裡轉型Set
                //byte[] = 地址片段代碼
                Set<byte[]> redisSetBytes = (Set<byte[]>) results.get(i);
                //一個key就一個Set
                Set<String> redisSet = new HashSet<>();
                for (byte[] bytes : redisSetBytes) {
                    redisSet.add(serializer.deserialize(bytes));
                }

                String key = redisKeys.get(i);
                if (!redisSet.isEmpty()) {
                    log.info("redis<有>找到cd代碼，key: {}, value: {}", key, redisSet);
                    //多個
                    String redisValue = String.join(",", redisSet);
                    resultMap.put(key, redisValue);
                    //如果是 "COUNTY", "TOWN", "VILLAGE","ROAD", "AREA", "LANE", "ALLEY",
                    //"NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5"
                    //才需要判斷0或1
                    if(containsKeyword(key)){
                        //地址片段有找到cd表示有填寫，segmentExistNumber 給 1
                        segmentExistNumberBuilder.append("1");
                    }
                    //
                } else {
                    log.info("redis<沒有>找到cd代碼，key: {}", key);
                    //如果地址片段找不到，就要用模糊搜尋
                    String redisValue = null;
                    // TODO: 2024/5/14 除了?還有方框，帶補上
                    // 難字判斷
                    // key="COUNTY:新?市"
                    if(key.contains("?")){
                        String scanKey = key.replace("?","*");
                        log.info("replace奇怪字元後，scanKey: {}", scanKey);
                        //可能的組合用 "," 區隔組成字串
                        redisValue = String.join(",",scanKeysAndReturnSet(scanKey));
                        log.info("scanKey: {}, redisValue: {}", scanKey, redisValue);
                    }

                    if(StringUtils.isNotNullOrEmpty(redisValue)){
                        resultMap.put(key, redisValue); //模糊搜尋有找到
                    }else{
                        resultMap.put(key, keyMap.get(key)); // 如果找不到對應的value，就要放default value(00000,0000)
                    }
                    //因為原始地址片段找不到cd,先判斷他沒有寫
                    if(containsKeyword(key)){
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
     * 會把難字換成*放進來模糊查詢
     */
    // TODO: 2024/5/14 速度慢，需要優化 
    public Set<String> scanKeysAndReturnSet(String pattern) {
        Set<String> resultSet = new HashSet<>();
        stringRedisTemplate2.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(SCAN_SIZE).build())) {
                while (cursor.hasNext()) {
                    byte[] next = cursor.next();
                    resultSet.addAll(getSet(new String(next)));
                }
            }
            return null;
        });

        return resultSet;
    }

    public Set<String> getSet(String key) {
        SetOperations<String, String> setOperations = stringRedisTemplate2.opsForSet();
        return setOperations.members(key);
    }



}
