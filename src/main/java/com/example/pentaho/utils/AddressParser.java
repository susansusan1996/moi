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
import redis.clients.jedis.*;

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
    private final String COUNTY = "(?<zipcode>(^\\d{5}|^\\d{3}|^\\d)?)(?<county>.*?縣|.*?市|%s)?";
    private final String TOWN = "(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區])|%s)?";

    /*不含路字串+新里里、村里、里、村hardcore*/
    private final String VILLAGE = "(?<village>(?<!路)(新里里|村里|[^路]*?里|[^路]*?村|%s))?";
    private final String NEIGHBOR = "(?<neighbor>" + ALL_CHAR + "+鄰)?";
    private final String SPECIALLANE = "(?<speciallane>鐵路.*巷|丹路.*巷)?"; //避免被切到路，直接先寫死在這裡
    private final String ROAD = "(?<road>(.*?段|.*?街|.*?大道|.*?路(?!巷)|%s)?)";
    private final String SPECIAL_AREA = "(?<area>%s)?"; //"村"結尾的AREA先抓出來
    private final String LANE = "(?<lane>.*?巷)?";
    private final String ALLEY = "(?<alley>" + ALL_CHAR_FOR_ALLEY + "+弄" + DYNAMIC_ALLEY_PART + ")?";
    //todo:ALL_CHAR前會有[//-－之]{1}嗎?*/
    private final String SUBALLEY = "(?<subAlley>" + ALL_CHAR + "+[衖衕橫]{1})?";
    private final String NUMFLR1 = "(?<numFlr1>" + ALL_CHAR + "+[\\-號Ff樓之區棟]{1}|" + BASEMENT_PATTERN + ")?";
    private final String NUMFLR2 = "(?<numFlr2>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"\\{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR3 = "(?<numFlr3>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR4 = "(?<numFlr4>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR5 = "(?<numFlr5>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String CONTINUOUS_NUM = "(?<continuousNum>[之\\-－]{1}" + ALL_CHAR + "+[樓FｆＦf]{1})?"; //之四十五F、－45樓、-四5f

    private final String ROOM = "(?<room>.*?室)?";
    private final String BASEMENTSTR = "(?<basementStr>屋頂突出.*層|地下.*層|地下.*樓|地下|地下室|底層|屋頂|頂樓|屋頂突出物|屋頂樓|頂層|頂加|頂)?";
    private final String ADDRREMAINS = "(?<addrRemains>.+)?";
    private final String REMARK = "(?<remark>[\\(\\{\\〈\\【\\[\\〔\\『\\「\\「\\《\\（](.*?)[\\)\\〉\\】\\]\\〕\\』\\」\\}\\」\\》\\）])?";
    //〈〉【】[]〔〕()『』「」{}「」《》（）

    public Address parseAddress(String origninalAddress, Address address) {
        if (address == null) {
            //表示初次切割，如果有切完第一次有REMAIN(見特付)或是地下室的狀況，才會再把地址丟進來再切一次
            address = new Address();
        }
        //
        address.setCleanAddress(cleanAddress(origninalAddress));
        String pattern = getPattern(); //組正則表達式
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(origninalAddress);
        if (matcher.matches()) {
            return setAddress(matcher, address);
        }
        return address;
    }


    public static String cleanAddress(String originalAddress) {
        if (originalAddress == null || originalAddress.isEmpty()) {
            return originalAddress;
        }
        // 去掉 "台灣省" 並去除特殊字元
        return originalAddress.replace("台灣省", "")
                .replaceAll("[`!@#$%^&*+=|';',./！@#￥%……&*+|‘”“’。，\\\\\\s]+", "");
    }


    //切出找不到的area(村) 南洋新村|大學新村...
    public Address parseArea(Address address) {
        log.info("從remains切地名:{}", address.getAddrRemains());
        String[] regexArray = {
                // 第一個規則:取出中文 南洋新村
                "^[^0-9０-９]+;",
                // 第二個規則:中文+[一二三四五六七八九十]+中文 匹配0以上個字服 陸光二村
                "^[^0-9０-９一二三四五六七八九十]+[一二三四五六七八九十]+[^0-9０-９一二三四五六七八九十]*;",
                // 第三個規則:取1~7個中文字 家裡蹲大學店
                "^[^0-9０-９一二三四五六七八九十]{1,7}"
        };
        String match = "";
        // 逐一檢查每一個正則表達式
        for (String regex : regexArray) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(address.getAddrRemains());
            if (matcher.find()) {
                match = matcher.group();
                //todo:一個就停切割，還有addressRemain會再進來
                log.info("匹配到的部分：{}" , match);
                break;
            }
        }
        //如果有再找到area，就把area砍掉，切出其他address片段
        if(StringUtils.isNotNullOrEmpty(match)){
            address.setArea(match);
            //cleanAddress 去除特殊字元+台灣省
            String cleanAddress = address.getCleanAddress();
            log.info("比對到的Area:{}",match);
            /*area高機率再字串尾，所以從後面刪掉*/
            int lastIndex = cleanAddress.lastIndexOf(match);
            //把找到的AREA從INPUT的ADDRESS刪掉，再切一次
            String newAddressString = cleanAddress.substring(0, lastIndex) + cleanAddress.substring(lastIndex + match.length());
            address = parseAddress(newAddressString, address);
        }
        log.info("切不出地名，再切一次的新address:{}",address);
        return address;
    }



    private String getPattern() {
        Map<String, Set<String>> allKeys = new LinkedHashMap<>();
        try {
            String[] keys = {"COUNTY_ALIAS:", "TOWN_ALIAS:", "VILLAGE_ALIAS:", "ROAD_ALIAS:", "SPECIAL_AREA:"};
            allKeys = findAllKeys(keys); //redis查詢所有alias，要拼在正則後面
        } catch (Exception e) {
            log.error("findAllKeys error: {}", e.getMessage());
        }
        String newCounty = String.format(COUNTY , String.join("|",allKeys.get("COUNTY_ALIAS:")));
        log.info("newCounty:{}",newCounty);
        String newTown = String.format(TOWN , String.join("|",allKeys.get("TOWN_ALIAS:")));
        String newVillage = String.format(VILLAGE , String.join("|",allKeys.get("VILLAGE_ALIAS:")));
        String newRoad = String.format(ROAD , String.join("|",allKeys.get("ROAD_ALIAS:")));
        //南陽新村|功學社新村|大學新村
        String newArea = String.format(SPECIAL_AREA , String.join("|",allKeys.get("SPECIAL_AREA:")));
        return newCounty + newTown + newArea + newVillage + NEIGHBOR + SPECIALLANE + newRoad + LANE + ALLEY + SUBALLEY + NUMFLR1 + NUMFLR2 + NUMFLR3 + NUMFLR4 + NUMFLR5 + CONTINUOUS_NUM + ROOM + BASEMENTSTR + REMARK + ADDRREMAINS;
    }



    private Map<String, Set<String>> findAllKeys(String[] keys) {
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

    /***
     * matcher 有可能室比對第二次的物件，ex:有可能是轉換basement:X樓在一次進行切割後的物件
     * address 第一次或上一次比對的物件丟進來
     * @param matcher
     * @param address
     * @return
     */
    public Address setAddress(Matcher matcher, Address address) {
        address.setParseSuccessed(true);
        //basement 屋頂突出一百零一層、地下18層、地下壹樓、地下、地下室
        String basementString = matcher.group("basementStr");
        // 特殊處理地下一層和地下的情況
        // 先parseBasement 再 切割一次地址(會被切到NumFlr1~5當中)

        if (StringUtils.isNotNullOrEmpty(basementString)) {
            //todo:如果原本NumFlr1非空，會不會被basement的匹配結果覆蓋掉?
            return parseAddress(parseBasement(basementString, address.getCleanAddress(), address), address);
        }
        address.setZipcode(matcher.group("zipcode")); //郵遞區號5|3
        address.setCounty(matcher.group("county")); //縣市
        address.setTown(matcher.group("town")); //區
        address.setVillage(matcher.group("village")); //村、里、村里、新里里(字串內不能含有路)
        address.setNeighbor(matcher.group("neighbor")); //鄰
        address.setRoad(matcher.group("road")); //路、段、大道、街 (不含路巷)
        address.setSpecialArea(matcher.group("area"));//帶有"村"的area會先歸在這裡，ex:南洋新村
        if(address.getSpecialArea()!=null){
            address.setArea(address.getSpecialArea());
        }
        //如果specialArea為空 那 area 也會為空
        //巷使用 鐵路.*巷|丹路.*巷 -> .*?巷
        address.setLane(matcher.group("speciallane") != null ? matcher.group("speciallane") : matcher.group("lane"));
        //弄使用 數字+弄
        address.setAlley(matcher.group("alley"));
        //subAlley使用 數字+衖、衕、橫
        address.setSubAlley(matcher.group("subAlley"));
        //basement:一樓 basementStr:1(地下)
        //basement:"" 2(屋頂)
        address.setNumFlr1(parseBasementForBF(matcher.group("numFlr1"), address));
        address.setNumFlr2(parseBasementForBF(matcher.group("numFlr2"), address));
        address.setNumFlr3(parseBasementForBF(matcher.group("numFlr3"), address));
        address.setNumFlr4(parseBasementForBF(matcher.group("numFlr4"), address));
        address.setNumFlr5(parseBasementForBF(matcher.group("numFlr5"), address));
        //之四十五F、－45樓、-四5f
        address.setContinuousNum(matcher.group("continuousNum"));
        //數字+室
        address.setRoom(matcher.group("room"));
        //切不出來的
        address.setAddrRemains(matcher.group("addrRemains"));
        //特殊符號
        address.setRemark(matcher.group("remark"));
        return address;
    }

    /***
     * basemantStr 的正則再分成三類組
     * 1.地下hardcore 去hardcore 改 一樓
     * 2.地下、屋頂的組合  去keyWord basement: 中文數字樓
     * 3.屋頂hardcore 去hardcore 改 ""
     * 代碼1(地下) 代碼2(屋頂)
     * @param basementString 有可能為 屋頂突出一百零一層、地下18層、地下壹樓、地下、地下室
     * @param origninalAddress 原始地址
     * @param address 不可能為空
     * @return
     */
    private String parseBasement(String basementString, String origninalAddress, Address address) {
        String[] basemantPattern1 = {"地下層", "地下", "地下室", "底層"};
        String[] basemantPattern2 = {".*地下.*層.*", ".*地下室.*層.*",".*地下.*樓.*","屋頂突出.*層"};
        String[] roof1 = {"屋頂", "頂樓", "屋頂突出物", "屋頂樓", "頂層","頂加","頂"};
        //basementString = "地下層", "地下", "地下室", "底層" 其一
        if (Arrays.asList(basemantPattern1).contains(basementString)) {
            origninalAddress = origninalAddress.replaceAll(basementString, "一樓");
            address.setBasementStr("1");
        //basementString = "屋頂", "頂樓", "屋頂突出物", "屋頂樓", "頂層","頂加","頂"
        } else if (Arrays.asList(roof1).contains(basementString)) {
            origninalAddress = origninalAddress.replaceAll(basementString, "");
            address.setBasementStr("2");
        } else {
            //basementString = 屋頂突出101層、地下18層
//          //pattern : .*地下.*層.*、屋頂突出.*層、.*地下室.*層.*
            for (String basemantPattern : basemantPattern2) {
                Pattern regex = Pattern.compile(basemantPattern);
                Matcher basemantMatcher = regex.matcher(basementString);
                if (basemantMatcher.matches()) {
                    // 提取數字 一百零一
                    String numericPart = extractNumericPart(basementString);
                    // 加上"basement:"讓轉換為之４６一樓，的一樓可以被解析出來
                    // 會組成basement:一樓/basement:二樓...
                    // 屋頂突出101層 -> basement:一百零一樓
                    origninalAddress = origninalAddress.replaceAll(basementString, "basement:" + replaceWithChineseNumber(numericPart) + "樓");
                    log.info("basementString 提取數字部分:{} ", numericPart);
//                    if(basemantMatcher.group().contains(keyWord)){
//                      address.setBasementStr("2");
//                    }else{
//                      address.setBasementStr("1");
//                    }
                    if(basementString.contains("頂")){
                        address.setBasementStr("2");
                    }else{
                        //todo:屋頂突出.*層 ->可能會被誤判到這裡
                        address.setBasementStr("1");
                    }
                    //todo:匹配到一個就停掉
                    break;
                }
            }
        }
        return origninalAddress;
    }


    /**
     *
     * @param input 有可能會室 basement:一樓
     * @param address
     * @return
     */
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

