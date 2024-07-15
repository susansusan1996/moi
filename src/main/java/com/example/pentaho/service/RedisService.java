package com.example.pentaho.service;

import com.example.pentaho.component.Address;
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
    @Qualifier("stringRedisTemplate4")
    private StringRedisTemplate stringRedisTemplate4;

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

    /**
     * 將所有 key帶入DB1查找對應的Set<String>
     * @param keys ->排列組合的mappingId
     * @return resultList -> 每個value
     */
    public List<String> findSetsByKeys(List<String> keys) {
        List<String> resultList = new ArrayList<>();
        List<Object> results = stringRedisTemplate1.executePipelined((RedisCallback<List<String>>) connection -> {
            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
            for (String key : keys) {
                // lRange for List ,smember for Set
                stringRedisConn.sMembers(key);
            }
            return null;
        });

        //results=[[JB411:5141047,...](key1的value),[JB311:5141047,...](key2的value),[JB411:5141047,...](key1的value),...]
        for (Object result : results) {
            //result=[JB411:5141047,...]
            if (result instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> elements = (Set<String>) result;
                //elements = [JB411:5141047,...]
                log.info("elements:{}",elements);
                resultList.addAll(elements);
            }
        }
        //resultList:[00000000:JE431:12345,63000123:JA111:12345,.....]
        log.info("resultList:{}",resultList);
        return resultList;
    }




    /**
     * 將所有 key帶入DB1查找對應的Set<String>
     * @param address ->排列組合的mappingId
     * @return resultList -> key = 56碼mappingId, value = county+town:join_step:seq
     */
    public Map<String,Set<String>> findMapsByKeys(Address address) {
        List<String> keys = address.getMappingId();
        Map<String,Set<String>> resultList = new HashMap<>();
        List<Object> results = stringRedisTemplate1.executePipelined((RedisCallback<List<String>>) connection -> {
            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
            for (String key : keys) {
                // lRange for List ,smember for Set
                stringRedisConn.sMembers(key);

            }
            return null;
        });

        //results=[[JB411:5141047,...](key1的value),[JB311:5141047,...](key2的value),[JB411:5141047,...](key1的value),...]
        int index = 0;
        for (Object result : results) {
            //result=[JB411:5141047,...]
            if (result instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> elements = (Set<String>) result;
                //elements = [JB411:5141047,...]
                resultList.put(keys.get(index),elements);
            }
            index++;
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
            "NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5","ROOM"
    );


    public Map<String, String> findSetByKeys(Map<String, String> keyMap, String segmentExistNumber) {
        Map<String, String> resultMap = new HashMap<>();
        //redisKeys =["COUNTY:新北市","TOWN:新莊渠",...]
        List<String> redisKeys = new ArrayList<>(keyMap.keySet());
        //要件清單
        StringBuilder segmentExistNumberBuilder = new StringBuilder(segmentExistNumber);


        RedisConnection connection = stringRedisTemplate2.getConnectionFactory().getConnection();
        RedisSerializer<String> serializer = stringRedisTemplate2.getStringSerializer();
        try {
            connection.openPipeline();
            for (String key : redisKeys) {
                /*getSet<String>byKey*/
                connection.sMembers(serializer.serialize(key));
            }
            List<Object> results = connection.closePipeline();

            for (int i = 0; i < results.size(); i++) {
                /*redisSet是key的value*/
                Set<byte[]> redisSetBytes = (Set<byte[]>) results.get(i);
                Set<String> redisSet = new HashSet<>();
                for (byte[] bytes : redisSetBytes) {
                    redisSet.add(serializer.deserialize(bytes));
                }
                /*有序*/
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
                    log.info("redis <沒有> 找到 <"+key+"> 的cd代碼，要用模糊搜尋");
                    /* ex: key="COUNTY:新市" -> parts = ["COUNTY","新市"]**/
                    String[] parts = key.split(":");
                    Set<String> scanSet = new HashSet<>();
                    if (parts.length == 2 && !"null".equals(parts[1])) {
                        scanSet = scanKeysAndReturnSet(key);
                        log.info("模糊搜尋後的value: {}", scanSet);
                    }
                      /* scanSet
                      1) 模糊查詢有找到
                      2) 模糊查詢沒有找到
                      3) Exception
                     */
                    if (!scanSet.isEmpty()) {
                        String value = String.join(",", scanSet);
                        resultMap.put(key, value); //模糊搜尋有找到
                    } else {
                        resultMap.put(key, keyMap.get(key)); // 如果找不到對應的value，就要放default value(對應字數的0)
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


    /**
     * 排列組合56碼mappingId作為key(setName),到 Redis DB4 找出 value
     * @param mappingIds 所有可能的64碼(程式內擷取56碼)
     * @return Map<String, String> key:比對到的56碼；value:所有value以",'區隔組成的字串
     */
    public Map<String, String> findOneSetByKeysWithoutCountyAndTown(List<String> mappingIds) {
        //key = MappingId(56碼)
        Map<String, String> resultMap = new HashMap<>();

        //取得連線
        RedisConnection connection = stringRedisTemplate1.getConnectionFactory().getConnection();
        RedisSerializer<String> serializer = stringRedisTemplate1.getStringSerializer();
        try {
            //todo:批量送出模式
            connection.openPipeline();
            //todo:批次送出會循環所有mappingId，要改嗎
            for (String mappingId : mappingIds) {
                /**擷取56碼**/
                String shortMappingId = mappingId.substring(8, 64);
                log.info("尋找56碼:{}", shortMappingId);
                //找出setName為56碼的values
                Set<byte[]> bytes = connection.sMembers(serializer.serialize(mappingId));
                if(bytes == null){
                    log.info("bytes:{}",bytes);
                }
            }

            //resultsList裝這次conection所有query的結果
            //resultsList=[query:[],query2:[],...]
            List<Object> resultsList = connection.closePipeline();
            if(resultsList.isEmpty() || resultsList == null){
                return resultMap;
            }

            for (int i = 0; i <resultsList.size(); i++) {
                //把對應mappingId的value取出後轉為字串
                Set<byte[]> byteSet = (Set<byte[]>) resultsList.get(i);
                if(!byteSet.isEmpty() && byteSet!=null){
                //表示這各mappingId有查找到喔~
                //用來裝反序列後的values
                Set<String> redisSet = new HashSet<>();
                for (byte[] bytes : byteSet) {
                    //redisSet=["63000320:JA111:seq","00000320:JA112:seq",...]
                    redisSet.add(serializer.deserialize(bytes));
                }

                //有找到對應的value
                if (!redisSet.isEmpty()) {
                    //把values集結成一個以","相隔的字串
                    String redisValue = String.join(",", redisSet);
                    log.info("redis找到符合的56碼，56碼: {}, value: {}", mappingIds.get(i).substring(8, 64),redisValue);
                    resultMap.put(mappingIds.get(i).substring(8, 64), redisValue);
                    //有一組set找到就停
                    return resultMap;
                }
               }
             }
        } finally {
            connection.close();
        }
        return resultMap;
    }


    /**
     * 單個query
     * @param mappingIds
     * @return
     */
    public Map<String, String> findOneSetByKeysWithoutCountyAndTown2(List<String> mappingIds) {
        //resultMap = { "MappingId(56碼)":"value1,value2,..."
        Map<String, String> resultMap = new HashMap<>();

        //取得連線
        RedisConnection connection = stringRedisTemplate1.getConnectionFactory().getConnection();
        RedisSerializer<String> serializer = stringRedisTemplate1.getStringSerializer();
        Set<byte[]> byteSet  = null;
        String targetCd = "";
        try {
            //todo:批次送出會循環所有mappingId，要改嗎
            for (String mappingId : mappingIds) {
                /**擷取56碼**/
                String shortMappingId = mappingId.substring(8, 64);
                log.info("尋找56碼:{}", shortMappingId);
                //找出setName為56碼的values
                byteSet = connection.sMembers(serializer.serialize(shortMappingId));
                if(byteSet != null){
                    targetCd = mappingId;
                    log.info("對應56碼:{},結果集:{}",targetCd,byteSet);
                    break;
                }
            }

            //resultsList裝這次conection所有query的結果
            //resultsList=[query:[],query2:[],...]

            if(byteSet.isEmpty() || byteSet == null){
                return resultMap;
            }

            //表示這各mappingId有查找到喔~
            //用來裝反序列後的values
            Set<String> redisSet = new HashSet<>();
            for (byte[] bytes : byteSet) {
                //redisSet=["63000320:JA111:seq","00000320:JA112:seq",...]
                redisSet.add(serializer.deserialize(bytes));
            }

            //有找到對應的value
            if (!redisSet.isEmpty()) {
                //把values集結成一個以","相隔的字串
                String redisValue = String.join(",", redisSet);
                log.info("redis找到符合的56碼，56碼: {}, 結果集轉成一個字串: {}", targetCd,redisValue);
                resultMap.put(targetCd, redisValue);
                return resultMap;
            }


        } finally {
            connection.close();
        }
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
     *  key="COUNTY:新市"
     * 模糊比對，找出相符的 KEY (redis: scan)，
     */
    // TODO: 2024/5/14 速度慢 ??，需要優化
    public Set<String> scanKeysAndReturnSet(String key) {
        Set<String> resultSet = new HashSet<>();
        if(key.split(":")[1]!=null){
            // TODO: 2024/5/29 除了 ? 方框待補上(難判斷應該在哪個關鍵字的哪個index開始切去做模糊查)
            // TODO: 2024/5/29 如果沒有 ?、方框(難字)，就不知道可以把甚麼字元挖掉用*取代。可能會造成 ex."民哈路"，無法找到 "民生路"
            // TODO: 2024/6/3 目前只有對?、方框(難字)做模糊查詢*字轉換，其餘錯別字、錯字、同音字等很難去判斷該將從字元中的哪個字做*字轉換
            String scanKey = key.split(":")[0]+ ":*" + key.split(":")[1].replace("?", "*") + "*";
            log.info("*字取代 <?、方框> 後，scanKey: {}", scanKey);
            /*有?、方框: key="COUNTY:新?市" -> "COUNTY:*新*市*" **/
            /*無?、方框: key="COUNTY:新市 ->  "COUNTY:*新市*"  */
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
                //TODO:exception 返回null
                return null;
            });
        }
        //TODO:模糊查詢也找不到是空集合
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
