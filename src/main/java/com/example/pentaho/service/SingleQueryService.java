package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.repository.IbdTbAddrCodeOfDataStandardRepository;
import com.example.pentaho.repository.IbdTbAddrDataNewRepository;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import com.example.pentaho.utils.AddressParser;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.pentaho.utils.NumberParser.*;

@Service
public class SingleQueryService {

    private static Logger log = LoggerFactory.getLogger(SingleQueryService.class);

    Integer SCAN_SIZE = 1000;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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

    String segmentExistNumber = ""; //紀錄user是否有輸入每個地址片段，有:1，沒有:0

    /**
     * 找單一個值 (redis: get)
     */
    public String findByKey(String columnName, String key, String defaultValue) {
        if (key != null && !key.isEmpty()) {
            String redisValue = stringRedisTemplate.opsForValue().get(key);
            if (redisValue != null && !redisValue.isEmpty()) {
                log.info("columnName:{} , redisKey: {} , redisValue: {}", columnName, key, redisValue);
                segmentExistNumber += "1";
                return redisValue;
            }
        }
        segmentExistNumber += "0";
        return defaultValue;
    }

    public Map<String, String> findByKeys(Map<String, String> keyMap) {
        Map<String, String> resultMap = new HashMap<>();
        List<String> redisKeys = new ArrayList<>(keyMap.keySet());
        List<String> redisValues = stringRedisTemplate.opsForValue().multiGet(redisKeys);
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
        return resultMap;
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
        return findByKey(null, "1066693", null);
    }

    public List<IbdTbAddrCodeOfDataStandardDTO> findJson(String originalString) {
        Address address = findMappingId(originalString);
        log.info("mappingId:{}", address.getMappingId());
        address = findSeqByMappingIdAndJoinStep(address);
        Set<String> seqSet = address.getSeqSet();
        log.info("seq:{}", seqSet);
        List<IbdTbAddrCodeOfDataStandardDTO> list = ibdTbAddrCodeOfDataStandardRepository.findBySeq(seqSet.stream().map(Integer::parseInt).collect(Collectors.toList()));
        //放地址比對代碼
        Address finalAddress = address;
        list.forEach(IbdTbAddrDataRepositoryNewdto -> IbdTbAddrDataRepositoryNewdto.setJoinStep(finalAddress.getJoinStep()));
        return list;
    }

    public Address findMappingId(String originalString) {
        //切地址
        Address address = addressParser.parseAddress(originalString, null);
        Map<String,String> keyMap = new LinkedHashMap<>();
        if (address != null) {
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
            keyMap.put("COUNTY:" + county, "00000");//5
            keyMap.put("TOWN:" + town, "000");//鄉鎮市區 //3
            keyMap.put("VILLAGE:" + village, "000");//里 //3
            keyMap.put("ROADAREA:"+ roadAreaKey, "0000000"); //7
            keyMap.put("LANE:" + replaceWithHalfWidthNumber(lane), "0000");//巷 //4
            keyMap.put("ALLEY:" + alleyIdSnKey, "0000000");//弄 //7
            keyMap.put("NUM_FLR_1:" + removeBasementAndChangeFtoFloor(numFlr1,address,"NUM_FLR_1"), "000000"); //6
            keyMap.put("NUM_FLR_2:" + removeBasementAndChangeFtoFloor(numFlr2,address,"NUM_FLR_2"), "00000"); //5
            keyMap.put("NUM_FLR_3:" + removeBasementAndChangeFtoFloor(numFlr3,address,"NUM_FLR_3"), "0000"); //4
            keyMap.put("NUM_FLR_4:" + removeBasementAndChangeFtoFloor(numFlr4,address,"NUM_FLR_4"), "000"); //3
            keyMap.put("NUM_FLR_5:" + removeBasementAndChangeFtoFloor(numFlr5,address,"NUM_FLR_5"), "0"); //1
            keyMap.put("ROOM:" + replaceWithHalfWidthNumber(address.getRoom()), "00000"); //5
            //===========把存有各地址片段的map丟到redis找cd碼===========================
            Map<String, String> resultMap = findByKeys(keyMap);
            //===========把找到的各地址片段cd碼組裝好===========================
            address.setCountyCd(resultMap.get("COUNTY:" + county));
            address.setTownCd(resultMap.get("TOWN:" + town));
            address.setVillageCd(resultMap.get("VILLAGE:" + village));
            address.setNeighborCd(findNeighborCd(address.getNeighbor()));//鄰
            if(StringUtils.isNullOrEmpty(roadAreaKey)){
                address.setRoadAreaSn("0000000");
            }else {
                address.setRoadAreaSn(resultMap.get("ROADAREA:"+ roadAreaKey));
            }
            address.setLaneCd(resultMap.get("LANE:" + replaceWithHalfWidthNumber(lane)));
            address.setAlleyIdSn(resultMap.get("ALLEY:" + alleyIdSnKey));
            String numTypeCd = "95";
            address.setNumFlr1Id(resultMap.get("NUM_FLR_1:" + removeBasementAndChangeFtoFloor(numFlr1,address,"NUM_FLR_1")));
            address.setNumFlr2Id(resultMap.get("NUM_FLR_2:" + removeBasementAndChangeFtoFloor(numFlr2,address,"NUM_FLR_2")));
            address.setNumFlr3Id(resultMap.get("NUM_FLR_3:" + removeBasementAndChangeFtoFloor(numFlr3,address,"NUM_FLR_3")));
            address.setNumFlr4Id(resultMap.get("NUM_FLR_4:" + removeBasementAndChangeFtoFloor(numFlr4,address,"NUM_FLR_4")));
            address.setNumFlr5Id(resultMap.get("NUM_FLR_5:" + removeBasementAndChangeFtoFloor(numFlr5,address,"NUM_FLR_5")));
            String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();
            //===========處理numFlrPos===========================
            String numFlrPos = getNumFlrPos(address);
            address.setRoomIdSn(resultMap.get("ROOM:" + replaceWithHalfWidthNumber(room)));
            log.info("=== getCountyCd:{}",address.getCountyCd());
            log.info("=== getTownCd:{}",address.getTownCd());
            log.info("=== getVillageCd:{}", address.getVillageCd());
            log.info("=== getNeighborCd:{}" ,address.getNeighborCd());
            log.info("=== getRoadAreaSn:{}",address.getRoadAreaSn());
            log.info("=== getLaneCd:{}",address.getLaneCd());
            log.info("=== getAlleyIdSn:{}",address.getAlleyIdSn());
            log.info("=== numTypeCd:{}",numTypeCd);
            log.info("=== getNumFlr1Id:{}",address.getNumFlr1Id());
            log.info("=== getNumFlr2Id:{}",address.getNumFlr2Id());
            log.info("=== getNumFlr3Id:{}",address.getNumFlr3Id());
            log.info("=== getNumFlr4Id:{}",address.getNumFlr4Id());
            log.info("=== getNumFlr5Id:{}",address.getNumFlr5Id());
            log.info("=== basementStr:{}",basementStr);
            log.info("=== numFlrPos:{}",numFlrPos);
            log.info("=== getRoomIdSn:{}",address.getRoomIdSn());
            List<String> mappingIdList = Stream.of(
                            address.getCountyCd(), address.getTownCd(), address.getVillageCd(), address.getNeighborCd(),
                            address.getRoadAreaSn(), address.getLaneCd(), address.getAlleyIdSn(), numTypeCd,
                            address.getNumFlr1Id(), address.getNumFlr2Id(), address.getNumFlr3Id(), address.getNumFlr4Id(),
                            address.getNumFlr5Id(), basementStr, numFlrPos, address.getRoomIdSn())
                    .map(Object::toString)
                    .collect(Collectors.toList());
            address.setMappingIdList(mappingIdList);
            address.setMappingId(String.join("", mappingIdList));
            address.setSegmentExistNumber(insertCharAtIndex(segmentExistNumber, address));
        }
        return address;
    }

    private String insertCharAtIndex(String segmentExistNumber, Address address) {
        StringBuilder stringBuilder = new StringBuilder(segmentExistNumber);
        //鄰
        if ("000".equals(address.getNeighborCd())) {
            stringBuilder.insert(3, '0');  //鄰找不到
        } else {
            stringBuilder.insert(3, '1');  //鄰找的到
        }
        stringBuilder.insert(7, '1');  //numTypeCd一定找的到，所以直接寫1
        stringBuilder.insert(13, '0'); //basementStr一律當作找不到，去模糊比對
        stringBuilder.insert(14, '0'); //numFlrPos一律當作找不到，去模糊比對
        String result = stringBuilder.toString();
        log.info("segmentExistNumber: {}", result);
        return result;
    }


    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(String addressId) {
        log.info("addressId:{}", addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
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
    //把為了識別是basement的字眼拿掉、將F轉換成樓、-轉成之
    public String removeBasementAndChangeFtoFloor(String rawString, Address address, String flrType) {
        if (rawString != null) {
            //convertFToFloorAndHyphenToZhi: 將F轉換成樓，-轉成之
            //replaceWithHalfWidthNumber: 把basement拿掉
            String result = convertFToFloorAndHyphenToZhi(replaceWithHalfWidthNumber(rawString).replace("basement:", ""));
            switch (flrType) {
                case "NUM_FLR_1" -> address.setNumFlr1(result);
                case "NUM_FLR_2" -> address.setNumFlr2(result);
                case "NUM_FLR_3" -> address.setNumFlr3(result);
                case "NUM_FLR_4" -> address.setNumFlr4(result);
                case "NUM_FLR_5" -> address.setNumFlr5(result);
            }
            return result;
        }
        return "";
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


    Address findSeqByMappingIdAndJoinStep(Address address) {
        Set<String> seqSet = new HashSet<>();
        String seq = findByKey("mappingId 64碼", address.getMappingId(), null);
        if (!StringUtils.isNullOrEmpty(seq)) {
            seqSet.add(seq);
            //如果一次就找到seq，表示地址很完整，比對代碼為JA111
            address.setJoinStep("JA111");
        } else {
            //如果找不到完整代碼，要用正則模糊搜尋
            Map<String,String> map = buildRegexMappingId(address);
            String newMappingId =  map.get("newMappingId");
            String regex =  map.get("regex");
            log.info("因為地址不完整，組成新的 mappingId {}，以利模糊搜尋",newMappingId);
            log.info("模糊搜尋正則表達式為:{}",regex);
            Set<String> mappingIdSet =  findListByScan(newMappingId);
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
            log.info("最終可比對的mappingId:{}", newMappingIdSet);
            //=========比對代碼====================
            seqSet.addAll(findJoinStep(address, newMappingIdSet, seqSet));
        }
        address.setSeqSet(seqSet);
        return address;
    }

    //處理: 之45一樓 (像這種連續的號碼，就會被歸在這裡)
    public void formatCoutinuousFlrNum(String input, Address address) {
        if (StringUtils.isNotNullOrEmpty(input)) {
            String firstPattern = "(?<coutinuousNum1>之[\\d\\uFF10-\\uFF19]+)(?<coutinuousNum2>\\D+樓)?"; //之45一樓
            String secondPattern = "(?<coutinuousNum1>之\\D+)(?<coutinuousNum2>[\\d\\uFF10-\\uFF19]+樓)?"; //之四五1樓
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
                if(i == 0){
                    segAfter = String.valueOf(segNum.charAt(i+1));
                    //如果前後碼也是0的話，表示要相加
                    if("0".equals(segAfter)){
                        sum += mappingCount[i];
                    }
                    //後面那碼是1，表示不用相加了
                    else if("1".equals(segAfter)) {
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
                    else if("0".equals(segAfter)){
                        sum += mappingCount[i];
                    }
                    //後面那碼是1，表示不用相加了
                    else if("1".equals(segAfter)) {
                        sum += mappingCount[i];
                        regex.append("\\d{").append(sum).append("}");
                        sum = 0; //歸零
                    }
                }
                //是最後一個
                else {
                    segBefore = String.valueOf(segNum.charAt(i-1));
                    //如果前面是0表示最後一碼也要加上去
                    if("0".equals(segBefore)){
                        sum += mappingCount[i];
                        regex.append("\\d{").append(sum).append("}");
                    }else{
                        regex.append("\\d{").append(mappingCount[i]).append("}");
                    }
                    sum = 0; //歸零
                }
                newMappingId.append("*");
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("newMappingId", newMappingId.toString());
        map.put("regex", regex.toString());
        return map;
    }

    private static final int COUNTY_START_INDEX = 0;
    private static final int COUNTY_END_INDEX = 5;
    private static final int TOWN_START_INDEX = 5;
    private static final int TOWN_END_INDEX = 8;
    private static final int NEIGHBOR_START_INDEX = 10;
    private static final int NEIGHBOR_END_INDEX = 14;
    private static final int VILLAGE_START_INDEX = 8; //里~鄰的INDEX
    private static final int VILLAGE_END_INDEX = 14;  //里~鄰的INDEX

    private Set<String> findJoinStep(Address address, Set<String> newMappingIdSet, Set<String> seqSet) {
        String mappingId = address.getMappingId();
        //最嚴謹
        String JA112_NO_COUNTY = removeMiddleChars(mappingId, COUNTY_START_INDEX, COUNTY_END_INDEX); //未含"縣市"，先歸在"JA112"
        String JA112 = removeMiddleChars(mappingId, TOWN_START_INDEX, TOWN_END_INDEX); //未含鄉鎮市區
        //退鄰(鄰 挖掉，看比不比得到)
        String JA211 = removeMiddleChars(mappingId, NEIGHBOR_START_INDEX, NEIGHBOR_END_INDEX); //含鄉鎮市區
        String JA212 = removeMiddleChars(removeMiddleChars(mappingId, NEIGHBOR_START_INDEX, NEIGHBOR_END_INDEX), TOWN_START_INDEX, TOWN_END_INDEX); //未含鄉鎮市區
        //退里(鄰、里挖掉，看比不比得到)
        String JA311 = removeMiddleChars(mappingId, VILLAGE_START_INDEX, VILLAGE_END_INDEX); //含鄉鎮市區
        String JA312 = removeMiddleChars(removeMiddleChars(mappingId, VILLAGE_START_INDEX, VILLAGE_END_INDEX), TOWN_START_INDEX, TOWN_END_INDEX); //未含鄉鎮市區
        //退室(鄰、里、室挖掉，看比不比得到)
        String JB111 = removeLastFiveChars(removeMiddleChars(mappingId, VILLAGE_START_INDEX, VILLAGE_END_INDEX)); //含鄉鎮市區
        String JB112 = removeLastFiveChars(removeMiddleChars(removeMiddleChars(mappingId, VILLAGE_START_INDEX, VILLAGE_END_INDEX), TOWN_START_INDEX, TOWN_END_INDEX)); //未含鄉鎮市區
        newMappingIdSet.forEach(id -> {
            //最嚴謹
            String newJA112NoCounty = removeMiddleChars(id, COUNTY_START_INDEX, COUNTY_END_INDEX); //最嚴謹比對(未含"縣市"，先歸在"JA112")
            //最嚴謹，未含鄉鎮市區
            String newJA112 = removeMiddleChars(id, TOWN_START_INDEX, TOWN_END_INDEX); //最嚴謹比對(未含鄉鎮市區)
            //退鄰(鄰 挖掉，看比不比得到)
            String newJA211 = removeMiddleChars(id, NEIGHBOR_START_INDEX, NEIGHBOR_END_INDEX); //含鄉鎮市區
            String newJA212 = removeMiddleChars(removeMiddleChars(id, NEIGHBOR_START_INDEX, NEIGHBOR_END_INDEX), TOWN_START_INDEX, TOWN_END_INDEX);
            //退里(鄰、里挖掉，看比不比得到)
            String newJA311 = removeMiddleChars(id, VILLAGE_START_INDEX, VILLAGE_END_INDEX);
            String newJA312 = removeMiddleChars(removeMiddleChars(id, VILLAGE_START_INDEX, VILLAGE_END_INDEX), TOWN_START_INDEX, TOWN_END_INDEX);
            //退室(鄰、里、室挖掉，看比不比得到)
            String newJB111 = removeLastFiveChars(removeMiddleChars(id, VILLAGE_START_INDEX, VILLAGE_END_INDEX));
            String newJB112 = removeLastFiveChars(removeMiddleChars(removeMiddleChars(id, VILLAGE_START_INDEX, VILLAGE_END_INDEX), TOWN_START_INDEX, TOWN_END_INDEX));
            if (JA112_NO_COUNTY.equals(newJA112NoCounty)) {
                address.setJoinStep("JA112");
                seqSet.add(findByKey("最嚴謹比對(未含\"縣市\"，先歸在\"JA112\")", id, null));
            }
            else if (JA112.equals(newJA112)) {
                address.setJoinStep("JA112");
                seqSet.add(findByKey("最嚴謹，未含鄉鎮市區", id, null));
            } else if (JA211.equals(newJA211)) {
                address.setJoinStep("JA211");
                seqSet.add(findByKey("退鄰，含鄉鎮市區", id, null));
            } else if (JA212.equals(newJA212)) {
                address.setJoinStep("JA212");
                seqSet.add(findByKey("退鄰，不含鄉鎮市區", id, null));
            } else if (JA311.equals(newJA311)) {
                address.setJoinStep("JA311");
                seqSet.add(findByKey("退里，含鄉鎮市區", id, null));
            } else if (JA312.equals(newJA312)) {
                address.setJoinStep("JA312");
                seqSet.add(findByKey("退里，不含鄉鎮市區", id, null));
            } else if (JB111.equals(newJB111)) {
                address.setJoinStep("JB111");
                seqSet.add(findByKey("退室，含鄉鎮市區", id, null));
            } else if (JB112.equals(newJB112)) {
                address.setJoinStep("JB112");
                seqSet.add(findByKey("退室，不含鄉鎮市區", id, null));
            } else {
                log.info("甚麼都沒有比到!!");
                seqSet.add(findByKey("甚麼都沒有比到，不退，找找看", id, null));
            }
        });
        return seqSet;
    }

    public static String removeMiddleChars(String str, int start, int end) {
        if (start < 0 || end > str.length() || start >= end) {
            // 如果起始所有小於0，結束索引大於字符串長度，或起始索引大於等於結束索引，則返回原始字串
            return str;
        }
        String leftPart = str.substring(0, start);
        String rightPart = str.substring(end);
        return leftPart + rightPart;
    }

    public static String removeLastFiveChars(String str) {
        if (str.length() <= 5) {
            return "";
        }
        return str.substring(0, str.length() - 5);
    }

//    public static void main(String[] args) {
//        log.info("aaaaa");
//        Pattern regexPattern = Pattern.compile(String.valueOf("66000090\\d{13}023600660009500001200004\\d{14}00002"));
//        Matcher matcher = regexPattern.matcher("6600009002101991055400236006600095000012000040000000001200000002");
//        //有符合的mappingId，才是真正要拿來處理比對代碼的mappingId
//        if (matcher.matches()) {
//            log.info("ddddddd");
//        }
//    }


        public static void main(String[] args) {
            String originalString = "Hello World";
            int start = 5; // 從索引3開始
            int end = 6; // 到索引8結束

            String result = removeMiddleChars(originalString, start, end);
            System.out.println("原始字符串: " + originalString);
            System.out.println("移除中間字符後: " + result);
        }


}
