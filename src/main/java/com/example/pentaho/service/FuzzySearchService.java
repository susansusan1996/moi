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


    public List<String> fuzzySearchSeq(Address address) {
        Set<String> newMappingIdSet = fuzzySearchMappingId(address);
        log.info("newMappingIdSet:{}", newMappingIdSet);
        return  redisService.findListsByKeys(newMappingIdSet.stream().toList());
    }

    public Set<String> fuzzySearchMappingId(Address address) {
        Map<String, List<String>> map = buildMappingIdRegexString(address);
        List<String> newMappingId = map.get("newMappingId");
        Set<String> set = new HashSet<>(newMappingId); //去除重複
        newMappingId = new ArrayList<>(set); //轉回LIST
        List<String> regex = map.get("regex");
        log.info("因為地址不完整，組成新的 mappingId {}，以利模糊搜尋", newMappingId);
        log.info("模糊搜尋正則表達式為:{}", regex);
        log.info("redis開始模糊搜尋:{}", Instant.now());
        Set<String> mappingIdSet = redisService.findListByScan(newMappingId); //redis撈出來的所有可能mappinId
        log.info("redis結束模糊搜尋:{}", Instant.now());
        Set<String> resultSet = new HashSet<>();
        for (String newMapping : mappingIdSet) {
            log.info("mappingIdSet:{}", mappingIdSet);
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


    private Map<String, List<String>> buildMappingIdRegexString(Address address) {
        List<String> newMappingIdList = new ArrayList<>();
        List<String> regexList = new ArrayList<>();
        String segNum = address.getSegmentExistNumber();
        List<LinkedHashMap<String, String>> mapList = address.getMappingIdMap();
        mapList.forEach(map -> {
            LinkedHashMap<String, String> regexMap = new LinkedHashMap<>(map);
            LinkedHashMap<String, String> fuzzyMap = new LinkedHashMap<>(map);
            StringBuilder newMappingId = new StringBuilder();
                if ("0".equals(String.valueOf(segNum.charAt(0))) && "0".equals(String.valueOf(segNum.charAt(1)))) { //COUNTY
                    regexMap.put("COUNTY", "\\d{8}");
                    regexMap.put("TOWN", "");
                    fuzzyMap.put("COUNTY", "*");
                    fuzzyMap.put("TOWN", "");
                } else if ("1".equals(String.valueOf(segNum.charAt(0))) && "0".equals(String.valueOf(segNum.charAt(1)))) {
                    // 有COUNTY，沒TOWN
                    regexMap.put("TOWN", "\\d{3}");
                    fuzzyMap.put("TOWN", "*");
                } else if ("0".equals(String.valueOf(segNum.charAt(0))) && "1".equals(String.valueOf(segNum.charAt(1)))) {
                    // 有TOWN，沒COUNTY
                    regexMap.put("COUNTY", "\\d{5}");
                    fuzzyMap.put("COUNTY", "*");
                }
                StringBuilder regex = new StringBuilder();
                for (String value : regexMap.values()) {
                        regex.append(value);
                }
                for (String value : fuzzyMap.values()) {
                    newMappingId.append(value);
                }
            regexList.add(String.valueOf(regex));
            newMappingIdList.add(String.valueOf(newMappingId));
        });
        Map<String, List<String>> map = new HashMap<>();
        map.put("regex", regexList); //帶有正則的mappingId(java比對redis模糊搜尋出來的結果)
        map.put("newMappingId", newMappingIdList); //帶有**的mappingId(redis模糊搜尋)
        return map;
    }

}
