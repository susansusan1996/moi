package com.example.pentaho.service;

import com.example.pentaho.component.Address;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                "JB211", "JB212"};
        for (String step : steps) {
            String[][] index = getIndex(step);
            String id = removeChars(mappingId, index, step);
            String newId;
            if (address.getJoinStep() != null) {
                break;
            }
            for (String newMappingId : newMappingIdSet) {
                newId = removeChars(newMappingId, index, step);
                if (id.equals(newId)) {
                    if ("JA112_NO_COUNTY".equals(step)) {
                        step = "JA112";
                    }
                    seq = redisService.findByKey("退" + step, newMappingId, "");
                    if (StringUtils.isNotNullOrEmpty(seq)) {
                        seqSet.add(seq);
                        address.setJoinStep(step);
                        //除了退室，有可能造成多址，其他有找到seq就可以停止loop
                        if (!"JB111".equals(step) && !"JB112".equals(step)) {
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

        if (address.getJoinStep() != null && seqSet.size() > 1) {
            if ("JB111".equals(address.getJoinStep()) || "JB112".equals(address.getJoinStep())) {
                address.setJoinStep("JD311");
            } else {
                seqSet.clear();
                seqSet.add(seq);
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
            case "JB111", "JB211" ->  //JB111: 退室(鄰、里、室挖掉)，含鄉鎮市區 //JB211: 樓之之樓，含鄉鎮市區
                    new String[][]{
                            {Integer.toString(VILLAGE_START_INDEX), Integer.toString(VILLAGE_END_INDEX)},
                            {Integer.toString(LAST_FIVE_INDEX)}};
            case "JB112", "JB212" ->  //JB112: 退室(鄰、里、室挖掉)，不含鄉鎮市區 //JB212: 樓之之樓，不含鄉鎮市區
                    new String[][]{
                            {Integer.toString(VILLAGE_START_INDEX), Integer.toString(VILLAGE_END_INDEX)},
                            {Integer.toString(TOWN_START_INDEX), Integer.toString(TOWN_END_INDEX)},
                            {Integer.toString(LAST_FIVE_INDEX)}};
            default -> new String[][]{};
        };
    }


    private String removeChars(String str, String[][] indices, String type) {
        StringBuilder builder = new StringBuilder(str);
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
        return builder.toString();
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

}
