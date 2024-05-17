package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.repository.IbdTbAddrCodeOfDataStandardRepository;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import com.example.pentaho.utils.AddressParser;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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

    public SingleQueryResultDTO findJson(SingleQueryDTO singleQueryDTO) throws NoSuchFieldException, IllegalAccessException {
        SingleQueryResultDTO result = new SingleQueryResultDTO();
        //確認是否是"連號"的地址
        if(checkIfMultiAddress(singleQueryDTO)){
            result.setText("該地址屬於多重地址");
            return result;
        }
        List<IbdTbAddrCodeOfDataStandardDTO> list = new ArrayList<>();
        //刪除使用者重複input的縣市、鄉鎮
        String cleanAddress = removeRepeatCountyAndTown(singleQueryDTO);
        //切地址+找mappingId
        Address address = parseAddressAndfindMappingId(cleanAddress);
        log.info("mappingId:{}", address.getMappingId());
        //找seq
        address = findSeqByMappingIdAndJoinStep(address);
        Set<String> seqSet = address.getSeqSet();
        if (!seqSet.isEmpty()) {
            log.info("seq:{}", seqSet);
            //檢查是否history(歷史門牌)，2的話就是history
            if ('2' == address.getJoinStep().charAt(3)) {
                log.info("歷史門牌!");
                List<IbdTbIhChangeDoorplateHis> hisList = ibdTbIhChangeDoorplateHisRepository.findByHistorySeq(seqSet.stream().toList());
                list = ibdTbAddrCodeOfDataStandardRepository.findByAddressId(hisList, address);
            } else {
                list = ibdTbAddrCodeOfDataStandardRepository.findBySeq(seqSet.stream().map(Integer::parseInt).collect(Collectors.toList()));
            }
            //放地址比對代碼
            Address finalAddress = address;
            list.forEach(IbdTbAddrDataRepositoryNewdto -> {
                if (!"JE621".equals(IbdTbAddrDataRepositoryNewdto.getJoinStep())
                        && !"JE431".equals(finalAddress.getJoinStep()) //缺少行政區
                        && !"JE421".equals(finalAddress.getJoinStep()) //缺少路地名
                        && !"JE511".equals(finalAddress.getJoinStep()) //地址完整切割但比對不到母體檔
                )
                {
                    IbdTbAddrDataRepositoryNewdto.setJoinStep(finalAddress.getJoinStep());
                }
            });
        }
        if (list.isEmpty()) {
            setJoinStepWhenResultIsEmpty(list,result,address); //JE431、JE421、JE511會在這邊寫入
        }
        result.setData(list);
        return result;
    }

    public Address parseAddressAndfindMappingId(String originalString) {
        //切地址
        Address address = addressParser.parseAddress(originalString, null, null);
        if(address!=null){
            log.info("getOriginalAddress:{}", address.getOriginalAddress());
            String numTypeCd = "95";
            //如果有addrRemain的話，表示有可能是"臨建特附"，要把"臨建特附"先拿掉，再PARSE一次地址
            if (StringUtils.isNotNullOrEmpty(address.getAddrRemains()) && StringUtils.isNullOrEmpty(address.getBasementStr())) {
                numTypeCd = getNumTypeCd(address);
                if (!"95".equals(numTypeCd)) { //臨建特附，再parse一次地址
                    log.info("臨建特附:{}", address.getOriginalAddress());
                    address = addressParser.parseAddress(null, address.getOriginalAddress(), address);
                } else if (StringUtils.isNotNullOrEmpty(address.getContinuousNum())) { //連在一起的數字，再parse一次地址
                    address = addressParser.parseAddress(null, address.getOriginalAddress(), address);
                } else {
                    //有可能是地址沒有切出來導致有remain
                    address = addressParser.parseArea(address);
                }
                address.setNumTypeCd(numTypeCd);
            } else {
                address.setNumTypeCd(numTypeCd); //95
            }
            return setAddressAndFindCdByRedis(address);
        }
        return new Address();
    }

    private static String getNumTypeCd(Address address) {
        String oldAddrRemains = address.getAddrRemains();
        String newAddrRemains = "";
        String numTypeCd;
        if (oldAddrRemains.startsWith("臨") || oldAddrRemains.endsWith("臨")) {
            numTypeCd = "96";
            newAddrRemains = oldAddrRemains.replace("臨", "");
        } else if (oldAddrRemains.startsWith("建") || oldAddrRemains.endsWith("建")) {
            numTypeCd = "97";
            newAddrRemains = oldAddrRemains.replace("建", "");
        } else if (oldAddrRemains.startsWith("特") || oldAddrRemains.endsWith("特")) {
            numTypeCd = "98";
            newAddrRemains = oldAddrRemains.replace("特", "");
        } else if (oldAddrRemains.startsWith("附") || oldAddrRemains.endsWith("附")) {
            numTypeCd = "99";
            newAddrRemains = oldAddrRemains.replace("附", "");
        } else {
            numTypeCd = "95";
            newAddrRemains = oldAddrRemains;
        }
        //再把addrRemains拼回原本的address，再重新切一次地址
        address.setOriginalAddress(address.getOriginalAddress().replace(oldAddrRemains, newAddrRemains));
        return numTypeCd;
    }


    Address findSeqByMappingIdAndJoinStep(Address address) throws NoSuchFieldException, IllegalAccessException {
        Set<String> seqSet = new HashSet<>();
        log.info("mappingIdList:{}", address.getMappingId());
        //直接用input的地址組mappingId，進redis db1 找有沒有符合的mappingId
        AtomicReference<List<String>> seqList = findSeqByMappingId(address);
        //有找到
        if (!seqList.get().isEmpty()) {
            log.info("第一次就有比到!!");
            seqSet = splitSeqAndStep(address, seqList, seqSet);
        }
        //沒找到
        //找不到符合得64碼。那就要把這串64碼，用join_step的邏輯一步一步(StepByStep)比較看看，看到哪一個join_step，會找到匹配的64碼
//        else {
//            seqSet.addAll(joinStepService.findSeqStepByStep(address));
//        }
        //多址判斷
        replaceJoinStepWhenMultiAdress(address,seqSet);
        address.setSeqSet(seqSet);
        return address;
    }



    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(String addressId) {
        log.info("addressId:{}", addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
    }


    //多址join_step判斷
    private void replaceJoinStepWhenMultiAdress(Address address, Set<String> seqSet) {
        if (address.getJoinStep() != null && seqSet.size() > 1) {
            switch (address.getJoinStep()) {
                case "JA211", "JA311" -> address.setJoinStep("JD111");
                case "JA212", "JA312" -> address.setJoinStep("JD112");
                case "JB111", "JB112" -> address.setJoinStep("JD311");
                case "JB311" -> address.setJoinStep("JD411");
                case "JB312" -> address.setJoinStep("JD412");
                case "JB411" -> address.setJoinStep("JD511");
                case "JB412" -> address.setJoinStep("JD512");
            }
        }
    }


    AtomicReference<List<String>> findSeqByMappingId(Address address) {
        AtomicReference<List<String>> seqList = new AtomicReference<>();
        for (String mappingId : address.getMappingId()) {
            seqList.set(redisService.findListByKey(mappingId));
            if (!seqList.get().isEmpty()) {
                break; //有比到就可以跳出迴圈了
            }
        }
        return seqList;
    }

    Set<String> splitSeqAndStep(Address address, AtomicReference<List<String>> seqList, Set<String> seqSet) {
        List<String> sortedList = seqList.get().stream().sorted().toList(); //排序
        String[] seqAndStep = sortedList.get(0).split(":");
        address.setJoinStep(seqAndStep[0]);
        for (String seq : sortedList) {
            seqAndStep = seq.split(":");
            seqSet.add(seqAndStep[1]);
        }
        if ("JC211".equals(address.getJoinStep()) && StringUtils.isNullOrEmpty(address.getArea())) {
            address.setJoinStep("JC311"); //路地名，連寫都沒寫
        }
        return seqSet;
    }




    public Address setAddressAndFindCdByRedis(Address address) {
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
        String numTypeCd = address.getNumTypeCd(); //臨建特附
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
        Map<String, String> keyMap = new LinkedHashMap<>();
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
        Map<String, String> resultMap = redisService.findSetByKeys(keyMap, segmentExistNumber);
        //===========把找到的各地址片段cd碼組裝好===========================
        address.setCountyCd(resultMap.get("COUNTY:" + county));
        address.setTownCd(resultMap.get("TOWN:" + town));
        address.setVillageCd(resultMap.get("VILLAGE:" + village));
        address.setNeighborCd(findNeighborCd(address.getNeighbor()));//鄰
        if (StringUtils.isNullOrEmpty(roadAreaKey)) {
            address.setRoadAreaSn("0000000");
            //"沒有"填寫"路地名"先註記起來
            address.setHasRoadArea(false);
        } else {
            address.setRoadAreaSn(resultMap.get("ROADAREA:" + roadAreaKey));
            //"有"填寫"路地名"先註記起來
            address.setHasRoadArea(true);
        }
        address.setLaneCd(resultMap.get("LANE:" + replaceWithHalfWidthNumber(lane)));
        address.setAlleyIdSn(resultMap.get("ALLEY:" + alleyIdSnKey));
        address.setNumFlr1Id(setNumFlrId(resultMap, address, "NUM_FLR_1"));
        address.setNumFlr2Id(setNumFlrId(resultMap, address, "NUM_FLR_2"));
        address.setNumFlr3Id(setNumFlrId(resultMap, address, "NUM_FLR_3"));
        address.setNumFlr4Id(setNumFlrId(resultMap, address, "NUM_FLR_4"));
        address.setNumFlr5Id(setNumFlrId(resultMap, address, "NUM_FLR_5"));
        String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();
        //===========處理numFlrPos===========================
        String numFlrPos = getNumFlrPos(address);
        address.setNumFlrPos(numFlrPos);
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
        log.info("=== numFlrPos:{}", address.getNumFlrPos());
        log.info("=== getRoomIdSn:{}", address.getRoomIdSn());
        assembleMultiMappingId(address);
        address.setSegmentExistNumber(insertCharAtIndex(resultMap.getOrDefault("segmentExistNumber", ""), address));
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

    //補"segmentExistNumber"
    private String insertCharAtIndex(String segmentExistNumber, Address address) {
        StringBuilder stringBuilder = new StringBuilder(segmentExistNumber);
        //鄰
        if ("000".equals(address.getNeighborCd())) {
            stringBuilder.insert(3, '0');  //鄰找不到
        } else {
            stringBuilder.insert(3, '1');  //鄰找的到
        }
        stringBuilder.insert(7, '0');  //numTypeCd一律當作找不到，去模糊比對
        stringBuilder.insert(13, '0'); //basementStr一律當作找不到，去模糊比對
        stringBuilder.insert(14, '0'); //numFlrPos一律當作找不到，去模糊比對
        String result = stringBuilder.toString();
        log.info("segmentExistNumber: {}", result);
        return result;
    }


    //把為了識別是basement的字眼拿掉、將F轉換成樓、-轉成之
    public Address removeBasementAndChangeFtoFloor(String rawString, Address address, String flrType) {
        if (rawString != null) {
            //convertFToFloorAndHyphenToZhi: 將F轉換成樓，-轉成之
            //replaceWithHalfWidthNumber: 把basement拿掉
            String result = convertFToFloorAndHyphenToZhi(replaceWithHalfWidthNumber(rawString).replace("basement:", ""));
            AddressParser addressParser = new AddressParser();
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

    private String getResult(String result, String comparisonValue, String numericPart) {
        if (comparisonValue.equals(result)) {
            return padNumber(comparisonValue, numericPart);
        } else {
            return result;
        }
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

    //因為county、town、village、road、area、lane可能會有同名，但不同代碼的狀況，要組出不同的mappingId
    private void assembleMultiMappingId(Address address) {
        String numTypeCd = address.getNumTypeCd(); //臨建特附
        String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();
        List<String> countys = new ArrayList<>();
        List<String> townCds = new ArrayList<>();
        List<String> villageCds = new ArrayList<>();
        List<String> roadAreaCds = new ArrayList<>();
        List<String> lanes = new ArrayList<>();
        countys.addAll(splitAndAddToList(address.getCountyCd()));
        townCds.addAll(splitAndAddToList(address.getTownCd()));
        villageCds.addAll(splitAndAddToList(address.getVillageCd()));
        roadAreaCds.addAll(splitAndAddToList(address.getRoadAreaSn()));
        lanes.addAll(splitAndAddToList(address.getLaneCd())); // Add lanes here
        List<LinkedHashMap<String, String>> mappingIdMapList = new ArrayList<>();
        List<List<String>> mappingIdListCollection = new ArrayList<>();
        List<String> mappingIdStringList = new ArrayList<>();
        for (String countyCd : countys) {
            for (String townCd : townCds) {
                for (String villageCd : villageCds) {
                    for (String roadAreaCd : roadAreaCds) {
                        for (String laneCd : lanes) { // Add this loop for lanes
                            LinkedHashMap<String, String> mappingIdMap = new LinkedHashMap<>();
                            mappingIdMap.put("COUNTY", countyCd);
                            mappingIdMap.put("TOWN", townCd);
                            mappingIdMap.put("VILLAGE", villageCd);//里
                            mappingIdMap.put("NEIGHBOR", address.getNeighborCd());
                            mappingIdMap.put("ROADAREA", roadAreaCd);
                            mappingIdMap.put("LANE", laneCd);
                            mappingIdMap.put("ALLEY", address.getAlleyIdSn());//弄
                            mappingIdMap.put("NUMTYPE", numTypeCd);
                            mappingIdMap.put("NUM_FLR_1", address.getNumFlr1Id());
                            mappingIdMap.put("NUM_FLR_2", address.getNumFlr2Id());
                            mappingIdMap.put("NUM_FLR_3", address.getNumFlr3Id());
                            mappingIdMap.put("NUM_FLR_4", address.getNumFlr4Id());
                            mappingIdMap.put("NUM_FLR_5", address.getNumFlr5Id());
                            mappingIdMap.put("BASEMENT", basementStr);
                            mappingIdMap.put("NUMFLRPOS", address.getNumFlrPos());
                            mappingIdMap.put("ROOM", address.getRoomIdSn());
                            List<String> mappingIdList = Stream.of(
                                            countyCd, townCd, villageCd, address.getNeighborCd(),
                                            roadAreaCd, laneCd, address.getAlleyIdSn(), numTypeCd,
                                            address.getNumFlr1Id(), address.getNumFlr2Id(), address.getNumFlr3Id(), address.getNumFlr4Id(),
                                            address.getNumFlr5Id(), basementStr, address.getNumFlrPos(), address.getRoomIdSn())
                                    .map(Object::toString)
                                    .collect(Collectors.toList());
                            mappingIdMapList.add(mappingIdMap);
                            mappingIdListCollection.add(mappingIdList);
                            mappingIdStringList.add(String.join("", mappingIdList));
                            // 將NUMFLRPOS為00000的組合也塞進去mappingIdStringList
                            String oldPos = mappingIdMap.get("NUMFLRPOS");
                            mappingIdStringList.add(replaceNumFlrPosWithZero(mappingIdMap));
                            mappingIdMap.put("NUMFLRPOS", oldPos); //還原
                        }
                    }
                }
            }
        }
        address.setMappingIdMap(mappingIdMapList);
        address.setMappingIdList(mappingIdListCollection);
        address.setMappingId(mappingIdStringList);
    }


    private static List<String> splitAndAddToList(String input) {
        List<String> result = new ArrayList<>();
        if (input.contains(",")) {
            result.addAll(Arrays.asList(input.split(",")));
        } else {
            result.add(input);
        }
        return result;
    }

    private String replaceNumFlrPosWithZero(Map<String,String> mappingIdMap){
        StringBuilder sb = new StringBuilder();
        // 將NUMFLRPOS為00000的組合也塞進去
        mappingIdMap.put("NUMFLRPOS", "00000");
        for (Map.Entry<String, String> entry : mappingIdMap.entrySet()) {
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    //刪除使用者重複input的縣市、鄉鎮
    private String removeRepeatCountyAndTown(SingleQueryDTO singleQueryDTO) {
        String county = singleQueryDTO.getCounty() == null ? "" : singleQueryDTO.getCounty();
        String town = singleQueryDTO.getTown() == null ? "" : singleQueryDTO.getTown();
        Pattern pattern = Pattern.compile("(" + county + town + ")");
        Matcher matcher = pattern.matcher(singleQueryDTO.getOriginalAddress());
        int count = 0;
        String result = singleQueryDTO.getOriginalAddress();
        while (matcher.find()) {
            count++;
            //出現兩次以上，才需要刪除第一次出現的鄉鎮市區
            if (count >= 2) {
                result = singleQueryDTO.getOriginalAddress().replaceFirst(matcher.group(), "");
                log.info("重複輸入:{}", matcher.group());
                return result;
            }
        }
        return result;
    }

    //如果input的地址包含、~就歸類在多重地址，就要吐回"該地址屬於多重地址"
    Boolean checkIfMultiAddress(SingleQueryDTO singleQueryDTO) {
        return singleQueryDTO.getOriginalAddress().matches(".*[、~].*");
    }

    void setJoinStepWhenResultIsEmpty(List<IbdTbAddrCodeOfDataStandardDTO> list, SingleQueryResultDTO result, Address address) {
        log.info("查無資料");
        IbdTbAddrCodeOfDataStandardDTO dto = new IbdTbAddrCodeOfDataStandardDTO();
        String segNum = address.getSegmentExistNumber();
        if (!segNum.startsWith("11")) {
            dto.setJoinStep("JE431"); //缺少行政區 >>> 如果最後都沒有比到的話，同時沒有寫 縣市、鄉鎮市區
            result.setText("缺少行政區");
        } else if (segNum.startsWith("11") && '1' != segNum.charAt(4) && '1' != segNum.charAt(5)) {
            dto.setJoinStep("JE421"); //缺少路地名 >>> 如果最後都沒有比到的話，地址中同時沒有寫路名、地名、巷名
            result.setText("缺少路地名");
        } else if ('1' == segNum.charAt(0) && '1' == segNum.charAt(1) && '1' == segNum.charAt(4) && '1' == segNum.charAt(8)) { //地址完整切割但比對不到母體檔(NEW那張)
            dto.setJoinStep("JE511");
            result.setText("查無地址");
        } else {
            result.setText("查無地址");
        }
        list.add(dto);
    }


}
