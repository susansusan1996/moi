package com.example.pentaho.service;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.IbdTbAddrCodeOfDataStandardDTO;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.repository.IbdTbAddrCodeOfDataStandardRepository;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import com.example.pentaho.utils.AddressParser;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.pentaho.utils.NumberParser.*;

@Service
public class SingleQueryService {

    private static Logger log = LoggerFactory.getLogger(SingleQueryService.class);

    @Autowired
    private IbdTbIhChangeDoorplateHisRepository ibdTbIhChangeDoorplateHisRepository;

    @Autowired
    private AddressParser addressParser;

    @Autowired
    private IbdTbAddrCodeOfDataStandardRepository ibdTbAddrCodeOfDataStandardRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private JoinStepService joinStepService;

    String segmentExistNumber = ""; //紀錄user是否有輸入每個地址片段，有:1，沒有:0


    public String findJsonTest(SingleQueryDTO singleQueryDTO) {
        return redisService.findByKey(null, "1066693", null);
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
        Map<String, String> keyMap = new LinkedHashMap<>();
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
            Map<String, String> resultMap = redisService.findByKeys(keyMap, segmentExistNumber);
            //===========把找到的各地址片段cd碼組裝好===========================
            address.setCountyCd(resultMap.get("COUNTY:" + county));
            address.setTownCd(resultMap.get("TOWN:" + town));
            address.setVillageCd(resultMap.get("VILLAGE:" + village));
            address.setNeighborCd(findNeighborCd(address.getNeighbor()));//鄰
            if (StringUtils.isNullOrEmpty(roadAreaKey)) {
                address.setRoadAreaSn("0000000");
            } else {
                address.setRoadAreaSn(resultMap.get("ROADAREA:" + roadAreaKey));
            }
            address.setLaneCd(resultMap.get("LANE:" + replaceWithHalfWidthNumber(lane)));
            address.setAlleyIdSn(resultMap.get("ALLEY:" + alleyIdSnKey));
            String numTypeCd = "95";
            address.setNumFlr1Id(setNumFlrId(resultMap, address, "NUM_FLR_1"));
            address.setNumFlr2Id(setNumFlrId(resultMap, address, "NUM_FLR_2"));
            address.setNumFlr3Id(setNumFlrId(resultMap, address, "NUM_FLR_3"));
            address.setNumFlr4Id(setNumFlrId(resultMap, address, "NUM_FLR_4"));
            address.setNumFlr5Id(setNumFlrId(resultMap, address, "NUM_FLR_5"));
            String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();
            //===========處理numFlrPos===========================
            String numFlrPos = getNumFlrPos(address);
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
            log.info("=== numFlrPos:{}", numFlrPos);
            log.info("=== getRoomIdSn:{}", address.getRoomIdSn());
            List<String> mappingIdList = Stream.of(
                            address.getCountyCd(), address.getTownCd(), address.getVillageCd(), address.getNeighborCd(),
                            address.getRoadAreaSn(), address.getLaneCd(), address.getAlleyIdSn(), numTypeCd,
                            address.getNumFlr1Id(), address.getNumFlr2Id(), address.getNumFlr3Id(), address.getNumFlr4Id(),
                            address.getNumFlr5Id(), basementStr, numFlrPos, address.getRoomIdSn())
                    .map(Object::toString)
                    .collect(Collectors.toList());
            LinkedHashMap<String, String> mappingIdMap = new LinkedHashMap<>();
            mappingIdMap.put("COUNTY",address.getCountyCd());
            mappingIdMap.put("TOWN",address.getTownCd());
            mappingIdMap.put("VILLAGE", address.getVillageCd());//里
            mappingIdMap.put("NEIGHBOR", address.getNeighborCd());
            mappingIdMap.put("ROADAREA",address.getRoadAreaSn());
            mappingIdMap.put("LANE" , address.getLaneCd());//巷
            mappingIdMap.put("ALLEY" , address.getAlleyIdSn());//弄
            mappingIdMap.put("NUMTYPE",numTypeCd );
            mappingIdMap.put("NUM_FLR_1",address.getNumFlr1Id());
            mappingIdMap.put("NUM_FLR_2",address.getNumFlr2Id());
            mappingIdMap.put("NUM_FLR_3",address.getNumFlr3Id());
            mappingIdMap.put("NUM_FLR_4",address.getNumFlr4Id());
            mappingIdMap.put("NUM_FLR_5",address.getNumFlr5Id());
            mappingIdMap.put("BASEMENT",basementStr);
            mappingIdMap.put("NUMFLRPOS",numFlrPos);
            mappingIdMap.put("ROOM",address.getRoomIdSn());
            address.setMappingIdMap(mappingIdMap);
            address.setMappingIdList(mappingIdList);
            address.setMappingId(String.join("", mappingIdList));
            address.setSegmentExistNumber(insertCharAtIndex(resultMap.getOrDefault("segmentExistNumber", ""), address));
        }
        return address;
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
        stringBuilder.insert(7, '1');  //numTypeCd一定找的到，所以直接寫1
        stringBuilder.insert(13, '0'); //basementStr一律當作找不到，去模糊比對
        stringBuilder.insert(14, '0'); //numFlrPos一律當作找不到，去模糊比對
        String result = stringBuilder.toString();
        log.info("segmentExistNumber: {}", result);
        return result;
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
    public Address removeBasementAndChangeFtoFloor(String rawString, Address address, String flrType) {
        if (rawString != null) {
            //convertFToFloorAndHyphenToZhi: 將F轉換成樓，-轉成之
            //replaceWithHalfWidthNumber: 把basement拿掉
            String result = convertFToFloorAndHyphenToZhi(replaceWithHalfWidthNumber(rawString).replace("basement:", ""));
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
        String seq = redisService.findByKey("mappingId 64碼", address.getMappingId(), null);
        if (!StringUtils.isNullOrEmpty(seq)) {
            seqSet.add(seq);
            //如果一次就找到seq，表示地址很完整，比對代碼為JA111
            address.setJoinStep("JA111");
        } else {
            //如果找不到完整代碼，要用正則模糊搜尋
            Map<String, String> map = buildRegexMappingId(address);
            String newMappingId = map.get("newMappingId");
            String regex = map.get("regex");
            log.info("因為地址不完整，組成新的 mappingId {}，以利模糊搜尋", newMappingId);
            log.info("模糊搜尋正則表達式為:{}", regex);
            Set<String> mappingIdSet = redisService.findListByScan(newMappingId);
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
            seqSet.addAll(joinStepService.findJoinStep(address, newMappingIdSet, seqSet));
        }
        address.setSeqSet(seqSet);
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
        map.put("newMappingId", newMappingId.toString());
        map.put("regex", regex.toString());
        return map;
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

    private String getResult(String result, String comparisonValue, String numericPart) {
        if (comparisonValue.equals(result)) {
            return padNumber(comparisonValue, numericPart);
        } else {
            return result;
        }
    }


    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(String addressId) {
        log.info("addressId:{}", addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
    }

}
