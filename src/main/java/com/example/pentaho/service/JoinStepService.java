package com.example.pentaho.service;

import com.example.pentaho.component.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private static final int VILLAGE_START_INDEX = 8; //里~鄰的INDEX
    private static final int VILLAGE_END_INDEX = 14;  //里~鄰的INDEX

    public Set<String> findJoinStep(Address address, Set<String> newMappingIdSet, Set<String> seqSet) {
        String mappingId = address.getMappingId();
        String seq = "";
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
        //樓之之樓(鄰、里、室挖掉，含有鄉鎮市區)
        String JB211 = exchangePosition(removeLastFiveChars(removeMiddleChars(mappingId, VILLAGE_START_INDEX, VILLAGE_END_INDEX))); //含鄉鎮市區
        log.info("JB211:{}", JB211);
        String JB212 = exchangePosition(removeLastFiveChars(removeMiddleChars(removeMiddleChars(mappingId, VILLAGE_START_INDEX, VILLAGE_END_INDEX), TOWN_START_INDEX, TOWN_END_INDEX))); //未含鄉鎮市區

        for (String id : newMappingIdSet) {
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

            //樓之之樓(鄰、里、室挖掉，含有鄉鎮市區)
            String newJB211 = removeLastFiveChars(removeMiddleChars(id, VILLAGE_START_INDEX, VILLAGE_END_INDEX));
            log.info("newJB211:{}", newJB211);
            String newJB212 = removeLastFiveChars(removeMiddleChars(removeMiddleChars(id, VILLAGE_START_INDEX, VILLAGE_END_INDEX), TOWN_START_INDEX, TOWN_END_INDEX));
            if (JA112_NO_COUNTY.equals(newJA112NoCounty)) {
                address.setJoinStep("JA112");
                seqSet.add(redisService.findByKey("最嚴謹比對(未含\"縣市\"，先歸在\"JA112\")", id, null));
                break;
            } else if (JA112.equals(newJA112)) {
                address.setJoinStep("JA112");
                seq = redisService.findByKey("最嚴謹，未含鄉鎮市區", id, null);
                seqSet.add(seq);
                break;
            } else if (JA211.equals(newJA211)) {
                address.setJoinStep("JA211");
                seq = redisService.findByKey("退鄰，含鄉鎮市區", id, null);
                seqSet.add(seq);
                break;
            } else if (JA212.equals(newJA212)) {
                address.setJoinStep("JA212");
                seq = redisService.findByKey("退鄰，不含鄉鎮市區", id, null);
                seqSet.add(seq);
                break;
            } else if (JA311.equals(newJA311)) {
                address.setJoinStep("JA311");
                seq = redisService.findByKey("退里，含鄉鎮市區", id, null);
                seqSet.add(seq);
                break;
            } else if (JA312.equals(newJA312)) {
                address.setJoinStep("JA312");
                seq = redisService.findByKey("退里，不含鄉鎮市區", id, null);
                seqSet.add(seq);
                break;
            } else if (JB111.equals(newJB111)) {
                address.setJoinStep("JB111");
                seq = redisService.findByKey("退室，含鄉鎮市區", id, null);
                seqSet.add(seq);
            } else if (JB112.equals(newJB112)) {
                address.setJoinStep("JB112");
                seq = redisService.findByKey("退室，不含鄉鎮市區", id, null);
                seqSet.add(seq);
            } else if (JB211.equals(newJB211)) {
                address.setJoinStep("JB211");
                seq = redisService.findByKey("樓之之樓，含鄉鎮市區", id, null);
                seqSet.add(seq);
                break;
            } else if (JB212.equals(newJB212)) {
                address.setJoinStep("JB212");
                seq = redisService.findByKey("樓之之樓，不含鄉鎮市區", id, null);
                seqSet.add(seq);
                break;
            } else {
                log.info("甚麼都沒有比到!!");
                seqSet.add(redisService.findByKey("甚麼都沒有比到，不退，找找看", id, null));
            }
        }
        if (address.getJoinStep() != null) {
            // 退室多址(退室，且不只一筆)
            if (seqSet.size() > 1) {
                if ("JB111".equals(address.getJoinStep()) || "JB112".equals(address.getJoinStep())) {
                    address.setJoinStep("JD311");
                } else {
                    seqSet.clear();
                    seqSet.add(seq);//留有比對到JOIN_STEP的那筆即可
                }
            }
        }
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

    //如果position欄位有 32xxx、x32xx、xx32x、xxx32 (32連在一起的)，就把position改成23xxx、x23xx、xx23x、xxx23
    public static String exchangePosition(String str) {
        log.info("原始 position mapping:{}", str);
        StringBuilder result = new StringBuilder();
        if (str.length() < 5) {
            return str;
        }
        // 將前面的字符直接添加進結果中
        result.append(str, 0, str.length() - 5);
        // 檢查最後五個字是否有32
        String lastFive = str.substring(str.length() - 5);
        if (lastFive.contains("32")) {
            // 如果存在，則進行交換
            int index = lastFive.indexOf("32");
            result.append(lastFive.substring(0, index)); // 將 32 前面的字符添加到结果中
            result.append("24"); // replace成" 七樓 之１"
            result.append(lastFive.substring(index + 2)); // 將 32 后面的字符添加到结果中
        } else {
            // 如果不存在，直接把最後五個字元添加回字串
            result.append(lastFive);
        }
        log.info("交換後的 position mapping:{}", result.toString());
        return result.toString();
    }

}
