package com.example.pentaho.service;

import com.example.pentaho.component.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FuzzySearchService {

    private static Logger log = LoggerFactory.getLogger(FuzzySearchService.class);

    @Autowired
    private RedisService redisService;


    /**
     * 模糊搜尋
     * @param address
     * @return
     */
    public List<String> fuzzySearchSeq(Address address) {
        Set<String> newMappingIdSet = fuzzySearchMappingId(address);
        log.info("newMappingIdSet:{}", newMappingIdSet);
        return  redisService.findListsByKeys(newMappingIdSet.stream().toList());
    }

    /**
     *
     * @param address
     * @return
     */
    public Set<String> fuzzySearchMappingId(Address address) {
        Map<String, List<String>> map = buildMappingIdRegexString(address);
        List<String> newMappingId = map.get("newMappingId");
        Set<String> set = new HashSet<>(newMappingId); //去除重複
        newMappingId = new ArrayList<>(set); //轉回LIST
        log.info("因為地址不完整，加入 * 組成新的mappingId:{}，以利redis模糊搜尋", newMappingId);
        List<String> regex = map.get("regex");
        log.info("模糊搜尋後，java使用正則表達式比對結果，正則為:{}", regex);
        //============================//
        log.info("redis開始模糊搜尋:{}", Instant.now());
        //redis撈出來的所有可能mappinId
        Set<String> mappingIdSet = redisService.findListByScan(newMappingId);
        log.info("redis結束模糊搜尋:{}", Instant.now());
        log.info("mappingIdSet:{}", mappingIdSet);
        //============================//
        Set<String> resultSet = new HashSet<>();
        for (String newMapping : mappingIdSet) {
            for (String reg : regex) {
                Pattern regexPattern = Pattern.compile(reg);
                //因為redis的scan命令，無法搭配正則，限制*的位置只能有多少字元，所以要再用java把不符合的mappingId刪掉
                Matcher matcher = regexPattern.matcher(newMapping);
                //有符合的mappingId，才是真正要拿來處理比對代碼的mappingId
                if (matcher.matches()) {
                    resultSet.add(newMapping);
                }
            }
        }
        return resultSet;
    }

    /**
     *
     * @param address
     * @return
     */
    private Map<String, List<String>> buildMappingIdRegexString(Address address) {
        List<String> newMappingIdList = new ArrayList<>();
        List<String> regexList = new ArrayList<>();
        String segNum = address.getSegmentExistNumber();
        /*一個map是value組合在一起就是64mappingId*/
        List<LinkedHashMap<String, String>> mapList = address.getMappingIdMap();
        mapList.forEach(map -> {
            LinkedHashMap<String, String> regexMap = new LinkedHashMap<>(map);
            LinkedHashMap<String, String> fuzzyMap = new LinkedHashMap<>(map);
            StringBuilder newMappingId = new StringBuilder();
            /*county&town都沒寫**/
                if ("0".equals(String.valueOf(segNum.charAt(0))) && "0".equals(String.valueOf(segNum.charAt(1)))) {
                    /*map中的count用 '\d{8}'(), '*' (redis)取代 */
                    regexMap.put("COUNTY", "\\d{8}");
                    regexMap.put("TOWN", "");
                    fuzzyMap.put("COUNTY", "*");
                    fuzzyMap.put("TOWN", "");
                    /*有COUNTY，沒TOWN*/
                } else if ("1".equals(String.valueOf(segNum.charAt(0))) && "0".equals(String.valueOf(segNum.charAt(1)))) {
                    regexMap.put("TOWN", "\\d{3}");
                    fuzzyMap.put("TOWN", "*");
                } else if ("0".equals(String.valueOf(segNum.charAt(0))) && "1".equals(String.valueOf(segNum.charAt(1)))) {
                    // 有TOWN，沒COUNTY
                    regexMap.put("COUNTY", "\\d{5}");
                    fuzzyMap.put("COUNTY", "*");
                }
                StringBuilder regex = new StringBuilder();
                for (String value : regexMap.values()) {
                    /*正則 64碼*/
                        regex.append(value);
                }
                for (String value : fuzzyMap.values()) {
                    /*redis 64碼*/
                    newMappingId.append(value);
                }
            /*正則 64碼*/
            regexList.add(String.valueOf(regex));
            /*redis 64碼*/
            newMappingIdList.add(String.valueOf(newMappingId));
        });
        Map<String, List<String>> map = new HashMap<>();
        map.put("regex", regexList); //帶有正則的mappingId(java比對redis模糊搜尋出來的結果)
        map.put("newMappingId", newMappingIdList); //帶有**的mappingId(redis模糊搜尋)
        return map;
    }

}
