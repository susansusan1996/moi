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
        return redisService.findListsByKeys(newMappingIdSet.stream().toList());
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
//            log.info("mappingIdSet:{}", mappingIdSet);
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
     * 拔 village + nvillage ->只模糊查詢這個pattern
     * 撈出來的mappingId前六碼(negihbor->village->county+town)
     * 由小至大根地址片段的查察到cd做比較
     * @param address
     * @return
     */
    public List<String> fuzzySearchSeqWith50(Address address) {
        /** *50碼所有可能的mappingId*/
        Set<String> newMappingIdSet = fuzzySearch50MappingId(address);
        log.info("newMappingIdSet:{}", newMappingIdSet);
        Set<String> filternewMappingIdSet = fitlerNewMappingIdSet(address, newMappingIdSet);
        //送進來前要先fliter newMappingIdSet
        return redisService.findListsByKeys(filternewMappingIdSet.stream().toList());
    }


    /***
     * (1) 先找出有沒有可以跟village,neighbor地址片段匹配的加入possible
     * (2) 如果 possible is empty -> 使用 fuzzyMappingResults
     * (3) 開始找出符合county + town 的mappingId
     * @param address
     * @param newMappingIdSet
     */

    public Set<String> fitlerNewMappingIdSet (Address address,Set<String> newMappingIdSet){
        Set<String> fitlerNewMappingIdSet = new HashSet<>();

        newMappingIdSet.forEach(fuzzyMappingId ->{
            //先找出有沒有符合以下條件可以加入possible
            //要true
            if(fuzzyMappingId.startsWith(address.getVillageCd()+address.getNeighborCd())){
                fitlerNewMappingIdSet.add(fuzzyMappingId);
            }

            //要true
            if(fuzzyMappingId.startsWith(address.getVillageCd())){
                fitlerNewMappingIdSet.add(fuzzyMappingId);
            }

            //要等於4
            if(fuzzyMappingId.indexOf(address.getNeighborCd()) == 4 ){
                fitlerNewMappingIdSet.add(fuzzyMappingId);
            }
        });
      if(!fitlerNewMappingIdSet.isEmpty()){
          return fitlerNewMappingIdSet;
      }
          return newMappingIdSet;
    }

    /**
     *
     * @param address
     * @return
     */
    public Set<String> fuzzySearch50MappingId(Address address) {
        /**組redis & 程式 的正則*/
        Map<String, List<String>> map = build50MappingIdRegexString(address);
        /*取出給redis的正則*/
        List<String> newMappingId = map.get("newMappingId");
        /*去除重複*/
        Set<String> set = new HashSet<>(newMappingId);
        /*去除重複*/
        newMappingId = new ArrayList<>(set);
        List<String> regex = map.get("regex");
        log.info("模糊搜尋正則表達式為:{}", regex);

        log.info("組成新的 mappingId {}，以利模糊搜尋", newMappingId);
        log.info("redis開始模糊搜尋:{}", Instant.now());

        /*redis撈出來的所有的key(mappingId)*/
        Set<String> mappingIdSet = redisService.findListByScan(newMappingId);
        log.info("redis結束模糊搜尋:{}", Instant.now());
        Set<String> resultSet = new HashSet<>();
        for (String newMapping : mappingIdSet) {
            log.info("mappingIdSet:{}", mappingIdSet);
            /* redis的scan命令，無法搭配正則，限制*的位置只能有多少字元
             * 要再用java把不符合的mappingId刪掉，regex只有三種可能
             * (1) /d{6}+50碼 ()
             * (2) 000/d{3}+50碼
             * (3) /d{3}000+50碼
             *  */
            for (String reg : regex) {
                Pattern regexPattern = Pattern.compile(reg);
                //
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
     * todo:reids -> * +50碼 / java正則: /d{6}+50碼
     * address.mappingId = ["56碼1","56碼2",...]
     * @param address
     * @return
     */
    private Map<String, List<String>> build50MappingIdRegexString(Address address) {
        /*for redis fuzzy*/
        List<String> newMappingIdList = new ArrayList<>();
        /*for java check the redis funzzyMapping result*/
        List<String> regexList = new ArrayList<>();
        //要件清單
        String segNum = address.getSegmentExistNumber();
        /*mapList=[{
        "COUNTY":"COUNTY"
        }
        ]*/
        /*omap的value合起來是一組56碼mappingId**/
        List<LinkedHashMap<String, String>> mapList = address.getMappingIdMap();

        mapList.forEach(map -> {
            // java正則 -> for checking the results of redis
            LinkedHashMap<String, String> regexMap = new LinkedHashMap<>(map);
            // redis的模糊查詢
            LinkedHashMap<String, String> fuzzyMap = new LinkedHashMap<>(map);
            StringBuilder newMappingId = new StringBuilder();

            //todo:改成先模糊查詢，再用程式判斷
            regexMap.put("VILLAGE", "\\d{6}");
            regexMap.put("NEIGHBOR", "");

            fuzzyMap.put("VILLAGE", "*");
            fuzzyMap.put("NEIGHBOR", "");

            //neighbor、villiage兩個都沒寫 redis 模糊查詢前6碼+ 後面所有可能的mappingId
//            if ("0".equals(String.valueOf(segNum.charAt(2))) && "0".equals(String.valueOf(segNum.charAt(3)))) {
//                regexMap.put("VILLAGE", "\\d{6}");
//                regexMap.put("NEIGHBOR", "");
//
//                fuzzyMap.put("VILLAGE", "*");
//                fuzzyMap.put("NEIGHBOR", "");
//                //neighbor有寫；villiage沒寫
//            } else if ("1".equals(String.valueOf(segNum.charAt(2))) && "0".equals(String.valueOf(segNum.charAt(3)))) {
//                //有VILLAGE，沒NEIGHBR，
//                regexMap.put("NEIGHBOR", "\\d{3}");
//                //005*~
//                fuzzyMap.put("NEIGHBOR", "*");
//            } else if ("0".equals(String.valueOf(segNum.charAt(0))) && "1".equals(String.valueOf(segNum.charAt(1)))) {
//                //沒VILLAGE，有NEIGHBR，
//                regexMap.put("VILLAGE", "\\d{3}");
//                //*001~
//                fuzzyMap.put("VILLAGE", "*");
//            }
            //拼出給程式的正則
            StringBuilder regex = new StringBuilder();
            for (String value : regexMap.values()) {
                regex.append(value);
            }
            //拼出給redis的正則
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


    public Map<String,String> findSeqsWithoutCountyAndTown(Address address) {
        log.info("準備進行DB4 56碼搜尋");
        Map<String,String> newMappingIdMap = findOneMappingIdWithoutCountyAndTown(address);
        log.info("newMappingIdMap:{}", newMappingIdMap);
        return newMappingIdMap;
    }


    /**
     * Redis DB4
     * 只會有一個完全相等的mappingId
     * @param address
     * @return
     */
    public Map<String, String> findOneMappingIdWithoutCountyAndTown(Address address) {
//        long startTime = System.currentTimeMillis();
        log.info("DB4-56碼搜尋開始-排列組合56碼與DB4比對,找出唯一相符set");
        //單個query出去
//        Map<String, String> oneSetByKeys =redisService.findOneSetByKeysWithoutCountyAndTown2(address.getMappingId());
        //批次query
      Map<String, String> oneSetByKeys = redisService.findOneSetByKeysWithoutCountyAndTown(address.getMappingId());
//        long endTime = System.currentTimeMillis();
//        long elapsedTime = endTime - startTime;
//        log.info("處理時間:{}",elapsedTime);
        return oneSetByKeys;
    }



    /**
     * 有可能是county跟town都沒寫才會造成找不到進到這～
     * address.mappingId = ["56碼1","56碼2",...]
     * @param address
     * @return
     */
    private Map<String, List<String>> buildMappingIdWithoutRegexString(Address address) {
        //裝組好的redis正則
        List<String> newMappingIdList = new ArrayList<>();
        List<String> regexList = new ArrayList<>();
        //要件清單
        String segNum = address.getSegmentExistNumber();
        /*mapList=[{
        "COUNTY":"COUNTY"
        }
        ]*/
        //一個map是一組mappingId的key&value
        List<LinkedHashMap<String, String>> mapList = address.getMappingIdMap();
        mapList.forEach(map -> {
            // input的正則
            LinkedHashMap<String, String> regexMap = new LinkedHashMap<>(map);
            // redis的模糊查詢
            LinkedHashMap<String, String> fuzzyMap = new LinkedHashMap<>(map);
            StringBuilder newMappingId = new StringBuilder();

            //COUNTY、TOWN 兩個都沒寫 redis 模糊查詢前8碼+ 後面所有可能的mappingId
            if ("0".equals(String.valueOf(segNum.charAt(0))) && "0".equals(String.valueOf(segNum.charAt(1)))) {
                regexMap.put("COUNTY", "\\d{8}");
                regexMap.put("TOWN", "");

                fuzzyMap.put("COUNTY", "*");
                fuzzyMap.put("TOWN", "");
            } else if ("1".equals(String.valueOf(segNum.charAt(0))) && "0".equals(String.valueOf(segNum.charAt(1)))) {
                //COUNTY有寫；TOWN沒寫
                // redis模糊查詢6~8碼
                regexMap.put("TOWN", "\\d{3}");
                fuzzyMap.put("TOWN", "*");
            } else if ("0".equals(String.valueOf(segNum.charAt(0))) && "1".equals(String.valueOf(segNum.charAt(1)))) {
                // redis模糊查詢前5碼
                // 有TOWN，沒COUNTY
                regexMap.put("COUNTY", "\\d{5}");
                fuzzyMap.put("COUNTY", "*");
            }
            //都有寫的話就是空

            /**regexMap.values() -> 地址片段cd */
            StringBuilder regex = new StringBuilder();
            //原本的key=COUNTY、TOWN，如果進到上面判斷式裡就會被取代掉
            for (String value : regexMap.values()) {
                //正則有三組可能
                //(1) 都沒寫 -> \\{8}+56碼
                //(2) 只寫county -> 5碼\\{3}+56碼
                //(3) 只寫town -> \\{5}+3碼+56碼
                regex.append(value);
            }

            //原本的key=COUNTY、TOWN，如果進到上面判斷式裡就會被取代掉
            for (String value : fuzzyMap.values()) {
                //正則有三組可能
                //(1) 都沒寫 -> * + 56碼
                //(2) 只寫county -> 5碼 + *{3} + 56碼
                //(3) 只寫town ->   *{5} + 3碼 + 56碼
                newMappingId.append(value);
            }
            //把每一組mappingId模糊查詢的正則放入List
            regexList.add(String.valueOf(regex));
            newMappingIdList.add(String.valueOf(newMappingId));
        });
        Map<String, List<String>> map = new HashMap<>();
        map.put("regex", regexList); //帶有正則的mappingId(java比對redis模糊搜尋出來的結果)
        map.put("newMappingId", newMappingIdList); //帶有**的mappingId(redis模糊搜尋)
        return map;
    }


    /**
     * address.mappingId = ["64碼1","64碼2",...]
     * @param address
     * @return
     */
    private Map<String, List<String>> buildMappingIdRegexString(Address address) {
        List<String> newMappingIdList = new ArrayList<>();
        List<String> regexList = new ArrayList<>();
        //要件清單
        String segNum = address.getSegmentExistNumber();
        /*mapList=[{
        "COUNTY":"COUNTY"
        }
        ]*/
        List<LinkedHashMap<String, String>> mapList = address.getMappingIdMap();
        mapList.forEach(map -> {
            // input的正則
            LinkedHashMap<String, String> regexMap = new LinkedHashMap<>(map);
            // redis的模糊查詢
            LinkedHashMap<String, String> fuzzyMap = new LinkedHashMap<>(map);
            StringBuilder newMappingId = new StringBuilder();
            //COUNTY、TOWN 兩個都沒寫 redis 模糊查詢前8碼+ 後面所有可能的mappingId
                if ("0".equals(String.valueOf(segNum.charAt(0))) && "0".equals(String.valueOf(segNum.charAt(1)))) {
                    regexMap.put("COUNTY", "\\d{8}");
                    regexMap.put("TOWN", "");

                    fuzzyMap.put("COUNTY", "*");
                    fuzzyMap.put("TOWN", "");
                //COUNTY有寫；TOWN沒寫
                } else if ("1".equals(String.valueOf(segNum.charAt(0))) && "0".equals(String.valueOf(segNum.charAt(1)))) {
                    // 有COUNTY，沒TOWN
                    regexMap.put("TOWN", "\\d{3}");
                    fuzzyMap.put("TOWN", "*");
                //第1個字 -> COUNTY  第2個字 -> TOWN ; COUNTY沒寫；TOWN有寫
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
