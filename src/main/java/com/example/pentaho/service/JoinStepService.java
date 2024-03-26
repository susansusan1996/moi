package com.example.pentaho.service;

import com.example.pentaho.component.Address;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Set;

@Service
public class JoinStepService {

    private static Logger log = LoggerFactory.getLogger(JoinStepService.class);

    @Autowired
    private RedisService redisService;

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
    private static final String OLD_POSITION_2 = "24";
    private static final String NEW_POSITION_2 = "20";


    public Set<String> findJoinStep(Address address, Set<String> newMappingIdSet, Set<String> seqSet) {
        String mappingId = address.getMappingId();
        String seq = "";
        String[] steps = {
                "JA112_NO_COUNTY", //未含"縣市"，先歸在"JA112"
                "JA112", //最嚴謹，未含鄉鎮市區
                "JA211", "JA212",
                "JA311", "JA312",
                "JB111", "JB112",
                "JB211", "JB212",
                "JB311", "JB312" //退樓後之
        };
        for (String step : steps) {
            String[][] index = getIndex(step);
            //取代為0
            String id = removeChars(mappingId, index, step, address, null);
            if (step.startsWith("JB3")) {
                log.info("oldId:{}", id);
            }
            String newId;
            if (address.getJoinStep() != null) {
                break;
            }
            for (String newMappingId : newMappingIdSet) {
                //先把newMappingId，切割好裝進map裡
                LinkedHashMap<String, String> newMappingIdMap = mapNewMappingId(newMappingId);
                newId = removeChars(newMappingId, index, step, address, newMappingIdMap);
                if (step.startsWith("JB3")) {
                    log.info("newId:{}", newId);
                }
                if (id.equals(newId)) {
                    if ("JA112_NO_COUNTY".equals(step)) {
                        step = "JA112";
                    }
                    seq = redisService.findByKey("退" + step, newMappingId, "");
                    if (StringUtils.isNotNullOrEmpty(seq)) {
                        seqSet.add(seq);
                        address.setJoinStep(step);
                        //除了"退室"、"退樓後之"，有可能造成多址，其他有找到seq就可以停止loop
                        if (!step.startsWith("JB1") && !step.startsWith("JB3")) {
                            break;
                        }
                    }
                } else {
                    seq = redisService.findByKey("不退，找找看", newId, "");
                    if (StringUtils.isNotNullOrEmpty(seq)) {
                        seqSet.add(seq);
                    }
                }
            }
        }

        //多址
        if (address.getJoinStep() != null && seqSet.size() > 1) {
            switch (address.getJoinStep()) {
                case "JB111":
                case "JB112":
                    address.setJoinStep("JD311");
                    break;
                case "JB311":
                    address.setJoinStep("JD411");
                    break;
                case "JB312":
                    address.setJoinStep("JD412");
                    break;
                default:
                    seqSet.clear();
                    seqSet.add(seq);
                    break;
            }
        }
        return seqSet;
    }

    private String[][] getIndex(String step) {
        return switch (step) {
            case "JA112_NO_COUNTY" ->
                    new String[][]{{Integer.toString(COUNTY_START_INDEX), Integer.toString(COUNTY_END_INDEX)}};
            case "JA112" -> //未含"縣市"，先歸在"JA112"
                    new String[][]{{Integer.toString(TOWN_START_INDEX), Integer.toString(TOWN_END_INDEX)}};
            case "JA211" ->  //退鄰(鄰挖掉)，含鄉鎮市區
                    new String[][]{{Integer.toString(NEIGHBOR_START_INDEX), Integer.toString(NEIGHBOR_END_INDEX)}};
            case "JA212" ->  //退鄰(鄰挖掉)，不含鄉鎮市區
                    new String[][]{
                            {Integer.toString(NEIGHBOR_START_INDEX), Integer.toString(NEIGHBOR_END_INDEX)},
                            {Integer.toString(TOWN_START_INDEX), Integer.toString(TOWN_END_INDEX)}};
            case "JA311" ->  //退里(鄰、里挖掉)，含鄉鎮市區
                    new String[][]{{Integer.toString(VILLAGE_START_INDEX), Integer.toString(VILLAGE_END_INDEX)}};
            case "JA312" ->  //退里(鄰、里挖掉)，不含鄉鎮市區
                    new String[][]{
                            {Integer.toString(VILLAGE_START_INDEX), Integer.toString(VILLAGE_END_INDEX)},
                            {Integer.toString(TOWN_START_INDEX), Integer.toString(TOWN_END_INDEX)}
                    };
            case "JB111", "JB211", "JB311" ->  //含鄉鎮市區
                //JB111: 退室(鄰、里、室挖掉)
                //JB211: 樓之之樓
                //JB311: 退樓後之(鄰、里、室挖掉，position之的部分改0，mappingId之的部分也改0)
                    new String[][]{
                            {Integer.toString(VILLAGE_START_INDEX), Integer.toString(VILLAGE_END_INDEX)},
                            {Integer.toString(LAST_FIVE_INDEX)}};
            case "JB112", "JB212", "JB312" ->   //不含鄉鎮市區
                //JB112: 退室(鄰、里、室挖掉)，不含鄉鎮市區
                //JB212: 樓之之樓，不含鄉鎮市區
                //JB312: 退樓後之(鄰、里、室挖掉，不含鄉鎮市區，position之的部分改0，mappingId之的部分也改0)
                    new String[][]{
                            {Integer.toString(VILLAGE_START_INDEX), Integer.toString(VILLAGE_END_INDEX)},
                            {Integer.toString(TOWN_START_INDEX), Integer.toString(TOWN_END_INDEX)},
                            {Integer.toString(LAST_FIVE_INDEX)}};
            default -> new String[][]{};
        };
    }


    private String removeChars(String str, String[][] indices, String type, Address address, LinkedHashMap<String, String> newMappingIdMap) {
        String result = "";
        StringBuilder builder = new StringBuilder(str);
        //如果是"退樓後之"，要把之相對應的mapping欄位改成0
        //先找最後一個"之"落在NUM_FLR_1~NUM_FLR_5的哪個欄位
        if (type.startsWith("JB3") && newMappingIdMap != null) {
            String flrColumnNamePrefix = "NUM_FLR_";
            int flrNum = findBiggestFlr(address);
            String columnName = flrColumnNamePrefix + flrNum;
            builder = new StringBuilder(assembleMap(address, columnName));
        }
        for (String[] index : indices) {
            if (index.length == 2) {
                builder.delete(Integer.parseInt(index[0]), Integer.parseInt(index[1]));
            } else if (index.length == 1) {
                builder = new StringBuilder(builder.substring(0, builder.length() - 5));
            }
        }
        //要交換POSITION
        if (type.startsWith("JB")) {
            builder = new StringBuilder(exchangePosition(builder.toString(), type));
        }
        result = builder.toString();
        return result;
    }


    //如果position欄位有 32xxx、x32xx、xx32x、xxx32 (32連在一起的)，就把position改成23xxx、x23xx、xx23x、xxx23
    private static String exchangePosition(String positionMapping, String type) {
        if (positionMapping == null || positionMapping.length() < 5) {
            return positionMapping;
        }
        String oldPosition;
        String newPosition;
        switch (type) {
            //樓之之樓
            case "JB211":
            case "JB212":
                oldPosition = OLD_POSITION_1;
                newPosition = NEW_POSITION_1;
                break;
            //退樓後之
            case "JB311":
            case "JB312":
                oldPosition = OLD_POSITION_2;
                newPosition = NEW_POSITION_2;
                break;
            default:
                return positionMapping;
        }
        String lastFive = positionMapping.substring(positionMapping.length() - 5);
        String updatedLastFive = lastFive.replace(oldPosition, newPosition);
        return positionMapping.replaceAll("\\d{5}$", updatedLastFive);
    }

    //找FLR欄位，到到哪個欄位都還有值
    private int findBiggestFlr(Address address) {
        String[] flrArray = {address.getNumFlr1(), address.getNumFlr2(), address.getNumFlr3(), address.getNumFlr4(), address.getNumFlr5()};
        for (int i = 0; i < flrArray.length; i++) {
            if (StringUtils.isNullOrEmpty(flrArray[i])) {
                log.info("目前最大到:{}", "表示NUM_FLR_" + (i + 1));
                return (i + 1);
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
    private String assembleMap(Address address, String columnName) {
        log.info("拼接mappingId，columnName:{}", columnName);
        LinkedHashMap<String, String> newMappingIdMap = address.getMappingIdMap();
        //舊的地址代碼
        String oldNumSegment = address.getMappingIdMap().get(columnName);
        //補0的地址代碼
        String newNumSegment = "0".repeat(oldNumSegment.length());
        newMappingIdMap.put(columnName, newNumSegment);
        //再把newMappingIdMap的value都拼接起來
        StringBuilder stringBuilder = new StringBuilder();
        for (String value : newMappingIdMap.values()) {
            stringBuilder.append(value);
        }
        return stringBuilder.toString();
    }

}
