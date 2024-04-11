package com.example.pentaho.service;

import com.example.pentaho.component.Address;
import com.example.pentaho.repository.IbdTbAddrDataNewRepository;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JoinStepService {

    private static Logger log = LoggerFactory.getLogger(JoinStepService.class);

    @Autowired
    private RedisService redisService;

    @Autowired
    private IbdTbAddrDataNewRepository ibdTbAddrDataNewRepository;

    private static final int COUNTY_START_INDEX = 0;
    private static final int COUNTY_END_INDEX = 5;
    private static final int TOWN_START_INDEX = 5;
    private static final int TOWN_END_INDEX = 8;
    private static final int NEIGHBOR_START_INDEX = 10;
    private static final int NEIGHBOR_END_INDEX = 14;
    private static final int VILLAGE_START_INDEX = 8;
    private static final int VILLAGE_END_INDEX = 14;
    private static final int LAST_FIVE_INDEX = 14;

    private static final String OLD_POSITION_1 = "32";
    private static final String NEW_POSITION_1 = "24";
    private static final String OLD_POSITION_2 = "31";
    private static final String NEW_POSITION_2 = "14";
    static String[] steps = {
            "JA112_NO_COUNTY", //未含"縣市"，先歸在"JA112"
            "JA112", //最嚴謹，未含鄉鎮市區
            "JA211", "JA212",
            "JA311", "JA312",
            "JB111", "JB112",
            "JB211", "JB212",
            "JB311", "JB312", //退樓後之
            "JB411", "JB412",  //退樓
            "JB511", "JB512",  //號之之號
            "JC111",  //臨建特附
            "JC211",  //路地名進階比對(有寫路地名，只是比對不到)
            "JC311",  //缺路地名進階比對(沒寫路地名)
            "JC411"," JC412",  //號樓之要件 (漏寫之)，邏輯:原本有"之"的該欄位position會是0，要改成3(.+之$)
    };
    private static final String NUMFLRPOS = "NUMFLRPOS";
    static List<String> MULTI_ADDRESS = List.of("JB111", "JB112", "JB311", "JB312", "JB411", "JB412");

    public Set<String> findJoinStep(Address address, Set<String> newMappingIdSet, Set<String> seqSet) throws NoSuchFieldException, IllegalAccessException {
        String seq = "";
        if (!newMappingIdSet.isEmpty()) {
            for (String step : steps) {
//            if (step.startsWith("JB5")) {
//                //交換地址
//                exchangeFlr(address);
//                //交換地址後，重新查MAPPINGID
//                Address newAddress = setAddressService.setAddressAndFindCdByRedis(address);
//                seq = redisService.findByKey("JB5系列 mappingId 64碼", newAddress.getMappingId(), null);
//                if (StringUtils.isNotNullOrEmpty(seq)) {
//                    seqSet.add(seq);
//                }
//            } else {
                List<String> columns = new ArrayList<>(getColumnName(step));
                //把想要挖掉的地址片段代碼用0取代
                LinkedHashMap<String, String> oldMappingIdMap = new LinkedHashMap<>(address.getMappingIdMap());
                log.info("oldMappingIdMap:{}",oldMappingIdMap);
                String oldId = replaceCharsWithZero("oldId", columns, step, address, oldMappingIdMap);
                String newId;
                if (address.getJoinStep() != null) {
                    break;
                }
                for (String newMappingId : newMappingIdSet) {
                    if (address.getJoinStep() != null) {
                        break;
                    }
                    //先把newMappingId，切割好裝進map裡
                    LinkedHashMap<String, String> newMappingIdMap = mapNewMappingId(newMappingId);
                    log.info("newMappingIdMap:{}",newMappingIdMap);
                    newId = replaceCharsWithZero("newId", columns, step, address, newMappingIdMap);
                    if (step.startsWith("JB5")) {
                        log.info("newId:{}", newId);
                    }
                    if (oldId.equals(newId)) {
                        log.info("找到一樣的!:{}", newId);
                        log.info("newMappingIdMap:{}",newMappingIdMap);
                        if ("JA112_NO_COUNTY".equals(step)) {
                            step = "JA112";
                        }
                        seq = redisService.findByKey("退" + step, newMappingId, "");
                        if (StringUtils.isNotNullOrEmpty(seq)) {
                            seqSet.add(seq);
                            //路地名比對
                            log.info("address.getHasRoadArea():{}",address.getHasRoadArea().toString());
                            if("JC211".equals(step) || "JC311".equals(step)){
                                if (!address.getHasRoadArea()) {
                                    step = "JC311"; //路地名連寫都沒寫==>JC311
                                }else{
                                    step = "JC211"; //有寫路地名只是找不到代碼
                                }
                            }
                            step = checkIfHistory(step, seq); //確認歷史門牌
                            address.setJoinStep(step);
                            //除了"退室"、"退樓後之"、"退樓"，有可能造成多址，其他有找到seq就可以停止loop
                            if (!MULTI_ADDRESS.contains(step)) {
                                break;
                            }
                        }
                    }
                }
            }
        }else{
            log.info("沒有可以比對的mappingIdSet!!");
        }
        //如果JOIN_STEP都比對不到的話，就甚麼都不退，比對看看
        if (seqSet.isEmpty() && address.getJoinStep() == null) {
            log.info("找不到JOIN_STEP，甚麼都不退，比對看看");
            seqSet.addAll(redisService.findByKeys(newMappingIdSet));
        }

        //多址
        if (address.getJoinStep() != null && seqSet.size() > 1) {
            switch (address.getJoinStep()) {
                case "JB111", "JB112" -> address.setJoinStep("JD311");
                case "JB311" -> address.setJoinStep("JD411");
                case "JB312" -> address.setJoinStep("JD412");
                case "JB411" -> address.setJoinStep("JD511");
                case "JB412" -> address.setJoinStep("JD512");
                default -> {
                    seqSet.clear();
                    seqSet.add(seq);
                }
            }
        }
        return seqSet;
    }

    private List<String> getColumnName(String step) {
        return switch (step) {
            case "JA112_NO_COUNTY" -> List.of("COUNTY"); //未含"縣市"，先歸在"JA112"
            case "JA112" ->
                    List.of("TOWN");
            case "JA211" ->  //退鄰(鄰挖掉)，含鄉鎮市區
                    List.of("NEIGHBOR");
            case "JA212" ->  //退鄰(鄰挖掉)，不含鄉鎮市區
                    List.of("NEIGHBOR", "TOWN");
            case "JA311" ->  //退里(鄰、里挖掉)，含鄉鎮市區
                    List.of("NEIGHBOR", "VILLAGE");
            case "JA312" ->  //退里(鄰、里挖掉)，不含鄉鎮市區
                    List.of("NEIGHBOR", "VILLAGE", "TOWN");
            case "JB111", "JB211", "JB311", "JB411","JB511" ->  //含鄉鎮市區
                //JB111: 退室(鄰、里、室挖掉)
                //JB211: 樓之之樓
                //JB311: 退樓後之(鄰、里、室挖掉，position之的部分改0，mappingId之的部分也改0)
                //JB411: 退樓(鄰、里、室挖掉，position先全部歸零)
                    List.of("NEIGHBOR", "VILLAGE", "ROOM");
            case "JB112", "JB212", "JB312", "JB412", "JB512", "JC111" ->   //不含鄉鎮市區
                //JB112: 退室(鄰、里、室挖掉)
                //JB212: 樓之之樓
                //JB312: 退樓後之(鄰、里、室挖掉，position之的部分改0，mappingId之的部分也改0)
                //JB412: 退樓(鄰、里、室挖掉，position先全部歸零)
                    List.of("NEIGHBOR", "VILLAGE", "ROOM", "TOWN");
            case "JC211", "JC311", "JC412" ->   //不含鄉鎮市區
                    //鄰、里、室、路地名
                    List.of("NEIGHBOR", "VILLAGE", "ROADAREA", "ROOM", "TOWN");
            case "JC411" ->   //含鄉鎮市區
                //鄰、里、室、路地名
                    List.of("NEIGHBOR", "VILLAGE", "ROADAREA", "ROOM");
            default -> List.of();
        };
    }


    // 找出所有需要改成0的column
    // 再把column 組裝 成mappingId
    private String replaceCharsWithZero(String idType, List<String> columns, String step, Address address, LinkedHashMap<String, String> mappingIdMap) throws NoSuchFieldException, IllegalAccessException {
        // 如果是"退樓後之"，要把之相對應的mapping欄位改成0
        if (step.startsWith("JB3") || step.startsWith("JB4")) {
            int flrNum = findFlr(address);
            String flrColumnNamePrefix = "NUM_FLR_";
            String columnNameForChi = flrColumnNamePrefix + (flrNum + 1);
            //檢查是否有對應的column，不存在則相加
            boolean containsColumnNameForChi = columns.contains(columnNameForChi);
            if (!containsColumnNameForChi) {
                if (address.getMappingIdMap().get(columnNameForChi) != null) {
                    columns.add(columnNameForChi);
                }
            }
            // 如果是JB4，需要找到退樓的"樓"在NUM_FLR_1~NUM_FLR_5的哪個column
            if (step.startsWith("JB4")) {
                String columnNameForLo = flrColumnNamePrefix + (flrNum + 2);
                boolean containsColumnNameForLo = columns.contains(columnNameForLo);
                if (!containsColumnNameForLo) {
                    if (address.getMappingIdMap().get(columnNameForLo) != null) {
                        columns.add(columnNameForLo);
                    }
                }
            }
        }else if("JC111".equals(step) ){//臨建特附，NUMTYPE要歸零
            columns.add("NUMTYPE");
        }
        return assembleMap(idType, address, columns, mappingIdMap, step);
    }



    //如果position欄位有 32xxx、x32xx、xx32x、xxx32 (32連在一起的)，就把position改成23xxx、x23xx、xx23x、xxx23
    private void exchangePosition(String idType, LinkedHashMap<String, String> newMappingIdMap, String step) {
        //舊的地址代碼
        String oldNumSegment = newMappingIdMap.get(NUMFLRPOS);
        //新的地址代碼
        String newNumSegment = "";
        //補0的地址代碼
        String oldPosition = "";
        String newPosition = "";
        switch (step) {
            //樓之之樓
            case "JB211", "JB212" -> {
                oldPosition = OLD_POSITION_1;
                newPosition = NEW_POSITION_1;
                newNumSegment = oldNumSegment.replace(oldPosition, newPosition);
            }
            //退樓後之，把0前面的position號碼替換成0
            //oldId就不用再替換了，因為本來就沒有寫該欄位，所以本來就是0了
            case "JB311", "JB312" ->
                    newNumSegment = idType.equals("newId") ? replaceLeadingZeros(oldNumSegment) : oldNumSegment;
            //退樓，樓(包含樓的position要歸零)
            //找到樓的位置
            case "JB411", "JB412" -> {
                newNumSegment = oldNumSegment.replace("2","0"); //樓的position是4，替換成0
                log.info("newNumSegment:{}",newNumSegment);
            }
            //號之之號
            case "JB511", "JB512" -> {
                log.info("oldPosition:{}",oldNumSegment);
                oldPosition = OLD_POSITION_2;
                newPosition = NEW_POSITION_2;
                newNumSegment = oldNumSegment.replace(oldPosition, newPosition);
                log.info("newPosition:{}",newNumSegment);
            }
            //JC411 號樓之要件 (漏寫之)，邏輯:原本有"之"的該欄位position會是0或7，要改成3(.+之$)
            case "JC411", "JC412" -> {
                log.info("號樓之(漏寫之) oldPosition:{}",oldNumSegment);
                //只有oldId需要replace(因為只有oldId有漏寫之)
                newNumSegment = idType.equals("newId") ? oldNumSegment : replaceFirstZerosOrSevenWithThree(oldNumSegment);
                log.info("號樓之(漏寫之) newPosition:{}",newNumSegment);
            }
            default -> newNumSegment = oldNumSegment;
        }
        newMappingIdMap.put(NUMFLRPOS, newNumSegment);
    }

    //找FLR欄位，看到哪個FLR欄位都還有值
    private int findFlr(Address address) {
        String[] flrArray = {address.getNumFlr1(), address.getNumFlr2(), address.getNumFlr3(), address.getNumFlr4(), address.getNumFlr5()};
//                                        0              1                   2                            3                4
        for (int i = 0; i < flrArray.length; i++) {
            if (StringUtils.isNullOrEmpty(flrArray[i])) {
                log.info("目前最大到:{}", "NUM_FLR_" + (i));
                return (i);
            }
        }
        //如果都沒有比對到，表示NUM_FLR_1 ~ NUM_FLR_5 都有值
        return 5;
    }


    private LinkedHashMap<String, String> mapNewMappingId(String inputString) {
        LinkedHashMap<String, String> mappingIdMap = new LinkedHashMap<>();
        int[] mappingCount = {5, 3, 3, 3, 7, 4, 7, 2, 6, 5, 4, 3, 1, 1, 5, 5};
        String[] segmentName = {"COUNTY", "TOWN", "VILLAGE", "NEIGHBOR", "ROADAREA", "LANE", "ALLEY", "NUMTYPE", "NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5", "BASEMENT", "NUMFLRPOS", "ROOM"};
        String[] segmentedStrings = new String[mappingCount.length];
        int startIndex = 0;
        for (int i = 0; i < mappingCount.length; i++) {
            int count = mappingCount[i];
            segmentedStrings[i] = inputString.substring(startIndex, startIndex + count);
            startIndex += count;
            mappingIdMap.put(segmentName[i], segmentedStrings[i]);
        }
        return mappingIdMap;
    }

    //組裝裝mappingId的map
    private String assembleMap(String idType ,Address address, List<String> columns, LinkedHashMap<String, String> mappingIdMap, String step) {
        log.info("拼接mappingId，columnName:{},step:{}", columns, step);
        //columns: 要替換成0的所有欄位
        for (String columnName : columns) {
            if (address.getMappingIdMap().get(columnName) != null) {
                //舊的地址代碼
                String oldNumSegment = address.getMappingIdMap().get(columnName);
                //補0的地址代碼
                String newNumSegment = "0".repeat(oldNumSegment.length());
                //替換成0之後，裝進map裡
                mappingIdMap.put(columnName, newNumSegment);
            }
        }
        //交換POSITION
        if (step.startsWith("JB")) {
            exchangePosition(idType, mappingIdMap, step);
        }
        //再把newMappingIdMap的value都拼接起來
        StringBuilder stringBuilder = new StringBuilder();
        for (String value : mappingIdMap.values()) {
            stringBuilder.append(value);
        }
        return stringBuilder.toString();
    }

    private String replaceLeadingZeros(String number) {
        log.info("改之前==>:{}",number);
        int index = 0;
        // 找第一個不為0的數字的index
        while (index < number.length() && !(number.charAt(index) == '0')) {
            index++;
        }
        log.info("index==>:{}",index);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            if (i == index - 1) {
                sb.append('0'); // 第一個不為0的數字的前一個數字替換成0
            } else {
                sb.append(number.charAt(i));
            }
        }
        log.info("改之後==>:{}", sb);
        return sb.toString();
    }


    //找第一個0或7，把0或7改成3 (JC4 ，號樓之要件 (漏寫之))
    private static String replaceFirstZerosOrSevenWithThree(String number) {
        log.info("replaceFirstZerosOrSevenWithThree 改之前==>:{}",number);
        int index = 0;
        // 找第一個為0的數字的index
        while (index < number.length() && !(number.charAt(index) == '0') && !(number.charAt(index) == '7')) {
            index++;
        }
        log.info("index==>:{}",index);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            if (i == index) {
                sb.append('3'); // 第一個為0的數字的替換成3
            } else {
                sb.append(number.charAt(i));
            }
        }
        log.info("replaceFirstZerosOrSevenWithThree 改之後==>:{}", sb);
        return sb.toString();
    }


    private void exchangeFlr(Address address) throws NoSuchFieldException, IllegalAccessException {
        String[] flrArray = {address.getNumFlr1(), address.getNumFlr2(), address.getNumFlr3(), address.getNumFlr4(), address.getNumFlr5()};
        int index = address.getNumFlrPos().indexOf(NEW_POSITION_2); //找到"號之"在 postition number 的index
        //交換
        if (index != -1) {
            String num1 = address.getProperty("numFlr" + (index + 1));
            String num2 = address.getProperty("numFlr" + (index + 2));
            log.info("交換前 num1 :{}", num1);
            log.info("交換前 num2 :{}", num2);
            address.setProperty("numFlr" + (index + 1), flrArray[index + 1]);
            address.setProperty("numFlr" + (index + 2), flrArray[index]);
            num1 = address.getProperty("numFlr" + (index + 1));
            num2 = address.getProperty("numFlr" + (index + 2));
            log.info("交換後 num1 :{}", num1);
            log.info("交換後 num2 :{}", num2);
        }
    }

    private String checkIfHistory(String step, String seq){
        //確認是否為舊門牌
        String validity = ibdTbAddrDataNewRepository.queryDataSourceAndValidityBySeq(seq);
        if("HISTORY".equals(validity)){
            //把step裡的1換成2!!
            int index = step.length() - 2; //倒數第二個字的index
            return step.substring(0, index) + "2" + step.substring(index + 1);
        }
        return step;
    }

}
