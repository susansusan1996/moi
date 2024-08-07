package com.example.pentaho.utils;

import com.example.pentaho.component.Address;
import com.example.pentaho.repository.AliasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.pentaho.utils.NumberParser.extractNumericPart;
import static com.example.pentaho.utils.NumberParser.replaceWithChineseNumber;

@Component
public class AddressParser {
    private static final Logger log = LoggerFactory.getLogger(AddressParser.class);


    @Autowired
    @Qualifier("stringRedisTemplate2")
    private StringRedisTemplate stringRedisTemplate2;

    @Autowired
    private AliasRepository aliasRepository;

    private final String BASEMENT_PATTERN = "basement:[一二三四五六七八九十百千]+樓"; //經過一次PARSE之後，如果有地下或屋頂，都會被改為basement:開頭
    private final String ALL_CHAR_FOR_ALLEY = "[0-9０-９A-ZＡ-Ｚa-zａ-ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚壹貳參肆伍陸柒捌玖拾佰卅廿整棟之-]";
    private final String ALL_CHAR = "[0-9０-９A-ZＡ-Ｚa-zａ-ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚壹貳參肆伍陸柒捌玖拾佰卅廿整棟]";
    private final String DYNAMIC_ALLEY_PART = "|卓厝|安農新邨|吉祥園|蕭厝|泰安新村|美喬|１弄圳東|堤外|中興二村|溝邊|長埤|清水|南苑|二橫路|朝安|黃泥塘|建行新村|牛頭|永和山莊";
    private final String COUNTY = "(?<zipcode>(^\\d{5}|^\\d{3}|^\\d)?)(?<county>(?:.*?[縣市](?!場)|%s))?"; //(?!場)==>為了避免"內埔市場"這種area被切到這裡
    private final String TOWN = "(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]|%s)(?![村里鄰路巷段街道弄]))?";
    //    private final String VILLAGE = "(?<village>(.*新里里|.*村里|.*?里|.*?村|%s))?";
    private final String VILLAGE = "(?<village>(?<!路)%s(新里里|村里|[^路]*?里|[^路]*?村|%s)(?![村里鄰路巷段街道弄]))?";
    private final String NEIGHBOR = "(?<neighbor>" + ALL_CHAR + "+鄰)?";
    private final String SPECIALLANE = "(?<speciallane>鐵路.*巷|丹路.*巷)?"; //避免被切到路，直接先寫死在這裡
    private final String ROAD = "(?<road>(.*?段|.*?街|.*?大道|.*?路(?!巷)|%s)?)";
    private final String LANE = "(?<lane>.*?巷)?";
    private final String ALLEY = "(?<alley>" + ALL_CHAR_FOR_ALLEY + "+弄" + DYNAMIC_ALLEY_PART + ")?";
    private final String SUBALLEY = "(?<subAlley>" + ALL_CHAR + "+[衖衕橫]{1})?";
    private final String NUMFLR1 = "(?<numFlr1>" + ALL_CHAR + "+[\\-號Ff樓之區棟]{1}|" + BASEMENT_PATTERN + ")?";
    private final String NUMFLR2 = "(?<numFlr2>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR3 = "(?<numFlr3>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR4 = "(?<numFlr4>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR5 = "(?<numFlr5>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String CONTINUOUS_NUM = "(?<continuousNum>[之\\-－]{1}" + ALL_CHAR + "+[樓FｆＦf]{1})?";
    private final String ROOM = "(?<room>.*?室)?";
    private final String BASEMENTSTR = "(?<basementStr>屋頂突出.*層|地下.*層|地下.*樓|地下|地下室|底層|屋頂|頂樓|屋頂突出物|屋頂樓|頂層|頂加|頂)?";
    private final String ADDRREMAINS = "(?<addrRemains>.+)?";
    private final String REMARK = "(?<remark>[\\(\\{\\〈\\【\\[\\〔\\『\\「\\「\\《\\（](.*?)[\\)\\〉\\】\\]\\〕\\』\\」\\}\\」\\》\\）])?";
    //〈〉【】[]〔〕()『』「」{}「」《》（）

    public Address parseAddress(String origninalAddress, Address address) {
        if (address == null) { //表示初次切割，如果有切完第一次有REMAIN或是地下室的狀況，才會再把地址丟進來再切一次
            address = new Address();
        }
        address.setCleanAddress(cleanAddress(origninalAddress));
        //先把有鄉鎮市區村里字眼的area拿出來
        Map<String, Set<String>> allKeys = new LinkedHashMap<>();
        allKeys = findAllKeys(); //redis查詢所有alias，要拼在正則後面
        origninalAddress = findSpecialArea(allKeys, address, origninalAddress); //先把帶有"村"、"鄉"、"鎮"、"市"、"區"的area抓出來
        String pattern = getPattern(allKeys); //組正則表達式
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(origninalAddress);
        if (matcher.matches()) {
            return setAddress(matcher, address);
        }
        return address;
    }

    public String findSpecialArea(Map<String, Set<String>> allKeys, Address address, String origninalAddress) {
        String newArea = "(" + String.join("|", allKeys.get("SPECIAL_AREA:")) + ")";
        Pattern patternForSpecialArea = Pattern.compile(newArea);
        Matcher matcherForSpecialArea = patternForSpecialArea.matcher(origninalAddress);
        if (matcherForSpecialArea.find()) {
            log.info(" special_area 匹配到的部分：{}", matcherForSpecialArea.group());
            address.setArea(matcherForSpecialArea.group());
            origninalAddress = origninalAddress.replace(matcherForSpecialArea.group(), "");
        }
        return origninalAddress;
    }


    public static String cleanAddress(String originalAddress) {
        if (originalAddress == null || originalAddress.isEmpty()) {
            return originalAddress;
        }
        // 去掉 "台灣省" 並去除特殊字元
        return originalAddress.replace("台灣省", "")
                .replaceAll("[`!@#$%^&*+=|';',./！@#￥%……&*+|‘”“’。，\\\\\\s]+", "");
    }


    //切出找不到的area
    public Address parseArea(Address address) {
        log.info("從remains切地名:{}", address.getAddrRemains());
        String[] regexArray = {
                "^[^0-9０-９]+;", // 第一個規則
                "^[^0-9０-９一二三四五六七八九十]+[一二三四五六七八九十]+[^0-9０-９一二三四五六七八九十]*;", // 第二個規則
                "^[^0-9０-９一二三四五六七八九十]{1,7}" // 第三個規則
        };
        String match = "";
        // 逐一檢查每一個正則表達式
        for (String regex : regexArray) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(address.getAddrRemains());
            if (matcher.find()) {
                match = matcher.group();
                log.info("匹配到的部分：{}" , match);
                break;
            }
        }
        //如果有再找到area，就把area砍掉，切出其他address片段
        if(StringUtils.isNotNullOrEmpty(match)){
            address.setArea(match);
            String cleanAddress = address.getCleanAddress();
            log.info("match:{}",match);
            int lastIndex = cleanAddress.lastIndexOf(match);
            //把找到的AREA從INPUT的ADDRESS刪掉，再切一次
            String newAddressString = cleanAddress.substring(0, lastIndex) + cleanAddress.substring(lastIndex + match.length());
            address = parseAddress(newAddressString, address);
        }
        log.info("切不出地名，再切一次的新address:{}",address);
        return address;
    }



    private String getPattern(Map<String, Set<String>> allKeys) {
        try {
            allKeys = findAllKeys(); //redis查詢所有alias，要拼在正則後面
        } catch (Exception e) {
            log.error("findAllKeys error: {}", e.getMessage());
        }
        String newCounty = String.format(COUNTY , String.join("|",allKeys.get("COUNTY_ALIAS:")));
        log.info("newCounty:{}",newCounty);
        String newTown = String.format(TOWN , String.join("|",allKeys.get("TOWN_ALIAS:")));
        String newVillage = String.format(VILLAGE , "(?!"+String.join("|",allKeys.get("SPECIAL_AREA:"))+")",String.join("|",allKeys.get("VILLAGE_ALIAS:")));
        String newRoad = String.format(ROAD , String.join("|",allKeys.get("ROAD_ALIAS:")));
        return newCounty + newTown + newVillage + NEIGHBOR + SPECIALLANE + newRoad + LANE + ALLEY + SUBALLEY + NUMFLR1 + NUMFLR2 + NUMFLR3 + NUMFLR4 + NUMFLR5 + CONTINUOUS_NUM + ROOM + BASEMENTSTR + REMARK + ADDRREMAINS;
    }



    private Map<String, Set<String>> findAllKeys( ) {
        String[] keys = {"COUNTY_ALIAS:", "TOWN_ALIAS:", "VILLAGE_ALIAS:", "ROAD_ALIAS:", "SPECIAL_AREA:"};
        Map<String, Set<String>> resultMap = new HashMap<>();
        RedisConnection connection = null;
        try {
            connection = getConnection();
            RedisSerializer<String> serializer = stringRedisTemplate2.getStringSerializer();
            for (String key : keys) {
                Set<byte[]> redisSetBytes = connection.sMembers(serializer.serialize(key));
                Set<String> deserializedSet = new HashSet<>();
                for (byte[] bytes : redisSetBytes) {
                    deserializedSet.add(serializer.deserialize(bytes));
                }
                resultMap.put(key, deserializedSet);
            }
        } catch (Exception e) {
            log.error("findAllKeys error: {}", e.getMessage());
        } finally {
            releaseConnection(connection);
        }
        return resultMap;
    }

    private RedisConnection getConnection() {
        return stringRedisTemplate2.getConnectionFactory().getConnection();
    }

    private void releaseConnection(RedisConnection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Error closing Redis connection: {}", e.getMessage());
            }
        }
    }

    public Address setAddress(Matcher matcher, Address address) {
        address.setParseSuccessed(true);
        String basementString = matcher.group("basementStr");
        // 特殊處理地下一層和地下的情況
        if (StringUtils.isNotNullOrEmpty(basementString)) {
            return parseAddress(parseBasement(basementString, address.getCleanAddress(), address), address);
        }
        address.setZipcode(matcher.group("zipcode"));
        address.setCounty(matcher.group("county"));
        address.setTown(matcher.group("town"));
        address.setVillage(matcher.group("village"));
        address.setNeighbor(matcher.group("neighbor"));
        address.setRoad(matcher.group("road"));
        address.setLane(matcher.group("speciallane") != null ? matcher.group("speciallane") : matcher.group("lane"));
        address.setAlley(matcher.group("alley"));
        address.setSubAlley(matcher.group("subAlley"));
        address.setNumFlr1(parseBasementForBF(matcher.group("numFlr1"), address));
        address.setNumFlr2(parseBasementForBF(matcher.group("numFlr2"), address));
        address.setNumFlr3(parseBasementForBF(matcher.group("numFlr3"), address));
        address.setNumFlr4(parseBasementForBF(matcher.group("numFlr4"), address));
        address.setNumFlr5(parseBasementForBF(matcher.group("numFlr5"), address));
        address.setContinuousNum(matcher.group("continuousNum"));
        address.setRoom(matcher.group("room"));
        address.setAddrRemains(matcher.group("addrRemains"));
        address.setRemark(matcher.group("remark"));
        return address;
    }

    private String parseBasement(String basementString, String origninalAddress, Address address) {
        String[] basemantPattern1 = {"地下層", "地下", "地下室", "底層"};
        String[] basemantPattern2 = {".*地下.*層.*", ".*地下室.*層.*",".*地下.*樓.*","屋頂突出.*層"};
        String[] roof1 = {"屋頂", "頂樓", "屋頂突出物", "屋頂樓", "頂層","頂加","頂"};
        if (Arrays.asList(basemantPattern1).contains(basementString)) {
            origninalAddress = origninalAddress.replaceAll(basementString, "一樓");
            address.setBasementStr("1");
        } else if (Arrays.asList(roof1).contains(basementString)) {
            origninalAddress = origninalAddress.replaceAll(basementString, "");
            address.setBasementStr("2");
        } else {
            for (String basemantPattern : basemantPattern2) {
                Pattern regex = Pattern.compile(basemantPattern);
                Matcher basemantMatcher = regex.matcher(basementString);
                if (basemantMatcher.matches()) {
                    // 提取數字
                    String numericPart = extractNumericPart(basementString);
                    // 加上"basement:"讓轉換為之４６一樓，的一樓可以被解析出來
                    // 會組成basement:一樓/basement:二樓...
                    origninalAddress = origninalAddress.replaceAll(basementString, "basement:" + replaceWithChineseNumber(numericPart) + "樓");
                    log.info("basementString 提取數字部分:{} ", numericPart);
                    if(basementString.contains("頂")){ //屋頂突出.*層
                        address.setBasementStr("2");
                    }else{
                        address.setBasementStr("1");
                    }
                    break;
                }
            }
        }
        return origninalAddress;
    }


    //再PARSE一次已經在FLR_NUM_1~5 的BF、B1F
    private String parseBasementForBF(String input, Address address) {
        if (StringUtils.isNotNullOrEmpty(input)) {
            String[] basemantPattern1 = {"BF", "bf", "B1", "b1", "Ｂ１", "ｂ１", "ＢＦ", "ｂｆ"};
            String[] basemantPattern2 = {".*B.*樓",".*b.*樓",".*Ｂ.*樓",".*ｂ.*樓",".*B.*F", ".*b.*f", ".*Ｂ.*Ｆ", ".*ｂ.*ｆ"};
            if (Arrays.asList(basemantPattern1).contains(input)) {
                log.info("basemantPattern1:{}", input);
                address.setBasementStr("1");
                return "一樓";
            } else {
                for (String basemantPattern : basemantPattern2) {
                    Pattern regex = Pattern.compile(basemantPattern);
                    Matcher basemantMatcher = regex.matcher(input);
                    if (basemantMatcher.matches()) {
                        // 提取數字
                        String numericPart = extractNumericPart(input);
                        log.info("basementString 提取數字部分:{} ", numericPart);
                        address.setBasementStr("1");
                        return replaceWithChineseNumber(numericPart) + "樓";

                    }
                }
            }
        }
        return input; //如果都沒有符合b1的格式，表示沒有地下室的字眼，就返回原字串即可
    }



    //如果還是有連在一起的地址，要切開EX.1之10樓，要切成"1之"，"10樓"
    public Map<String, Object> parseNumFlrAgain(String rawNumFLR,String flrType) {
        Map<String, Object> map = new HashMap();
        final String numFlrFirst = "(?<numFlrFirst>[之-－]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR + "+[FｆＦf]|" + ALL_CHAR + "+[號樓FｆＦf之-－區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
        final String numFlrSecond = "(?<numFlrSecond>[之-－]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR + "+[FｆＦf]|" + ALL_CHAR + "+[號樓FｆＦf之-－區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + ")?";
        Pattern regex = Pattern.compile(numFlrFirst + numFlrSecond);
        Matcher matcher = regex.matcher(rawNumFLR);
        map.put("isParsed", false);
        if (matcher.matches() && matcher.group("numFlrSecond") != null) {
            String first = matcher.group("numFlrFirst");
            String second = matcher.group("numFlrSecond");
            log.info("再切割一次，numFlrFirst==>{}",first);
            log.info("再切割一次，numFlrSecond==>{}",second);
            map.put("isParsed", true);
            map.put("numFlrFirst", first);
            map.put("numFlrSecond", second);
            map.put("flrType", flrType);
        }
        return map;
    }


}

