package com.example.pentaho.utils;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.IbdTbAddrCodeOfDataStandardDTO;
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
    //todo:修正可能會有5、3碼以外的數
    // new:(?<zipcode>(\d{5}|\d{3}|\d)\b|\d+)
    // old:(?<zipcode>(^\d{5}|^\d{3}|^\d)?)

    private final String COUNTY = "(?<zipcode>(^\\d{5}|^\\d{3}|^\\d)?)(?<county>(?:.*?[縣市](?!場)|%s))?"; //(?!場)==>為了避免"內埔市場"這種area被切到這裡

    private final String TOWN = "(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]|%s)(?![村里鄰路巷段街道弄]))?";

    private final String VILLAGE = "(?<village>(?<!路)%s(新里里|村里|[^路]*?里|[^路]*?村|%s)(?![村里鄰路巷段街道弄]))?";
    private final String NEIGHBOR = "(?<neighbor>" + ALL_CHAR + "+鄰)?";

    //避免被切到路，直接先寫死在這裡
    private final String ROAD = "(?<road>(.*?段|.*?街|.*?大道|.*?路(?!巷)|%s)?)";

    private final String SPECIALLANE = "(?<speciallane>鐵路.*巷|丹路.*巷)?";
    private final String LANE = "(?<lane>.*?巷)?";

    private final String ALLEY = "(?<alley>" + ALL_CHAR_FOR_ALLEY + "+弄" + DYNAMIC_ALLEY_PART + ")?";
    private final String SUBALLEY = "(?<subAlley>" + ALL_CHAR + "+[衖衕橫]{1})?";

    private final String NUMFLR1 =
            "(?<numFlr1>" + ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +
                    "[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + //能:"之28", "-B2", "－123", "之一百", "-A", "之甲乙", "－拾", "之A3" / 不能:"之28樓", "-B2F", "－123f", "之100樓", "123", "A-"
                    "[之\\-－]{1}" + ALL_CHAR + "+[號]|" +
                    ALL_CHAR + "+[FｆＦf]{1} |" +
                    BASEMENT_PATTERN + "|" +
                    ALL_CHAR + "+(?!室))?";
    private final String NUMFLR2 =
            "(?<numFlr2>" + ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +
                    "[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + //能:"之28", "-B2", "－123", "之一百", "-A", "之甲乙", "－拾", "之A3" / 不能:"之28樓", "-B2F", "－123f", "之100樓", "123", "A-"
                    "[之\\-－]{1}" + ALL_CHAR + "+[號]|" +
                    ALL_CHAR + "+[FｆＦf]{1} |" +
                    BASEMENT_PATTERN + "|" +
                    ALL_CHAR + "+(?!室))?";

    private final String NUMFLR3 =
            "(?<numFlr3>" + ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +
                    "[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + //能:"之28", "-B2", "－123", "之一百", "-A", "之甲乙", "－拾", "之A3" / 不能:"之28樓", "-B2F", "－123f", "之100樓", "123", "A-"
                    "[之\\-－]{1}" + ALL_CHAR + "+[號]|" +
                    ALL_CHAR + "+[FｆＦf]{1} |" +
                    BASEMENT_PATTERN + "|" +
                    ALL_CHAR + "+(?!室))?";

    private final String NUMFLR4 =
            "(?<numFlr4>" + ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +
                    "[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + //能:"之28", "-B2", "－123", "之一百", "-A", "之甲乙", "－拾", "之A3" / 不能:"之28樓", "-B2F", "－123f", "之100樓", "123", "A-"
                    "[之\\-－]{1}" + ALL_CHAR + "+[號]|" +
                    ALL_CHAR + "+[FｆＦf]{1} |" +
                    BASEMENT_PATTERN + "|" +
                    ALL_CHAR + "+(?!室))?";

    private final String NUMFLR5 =
            "(?<numFlr5>" + ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +
                    "[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + //能:"之28", "-B2", "－123", "之一百", "-A", "之甲乙", "－拾", "之A3" / 不能:"之28樓", "-B2F", "－123f", "之100樓", "123", "A-"
                    "[之\\-－]{1}" + ALL_CHAR + "+[號]|" +
                    ALL_CHAR + "+[FｆＦf]{1} |" +
                    BASEMENT_PATTERN + "|" +
                    ALL_CHAR + "+(?!室))?";

    private final String CONTINUOUS_NUM = "(?<continuousNum>[之\\-－]{1}" + ALL_CHAR + "+[樓FｆＦf]{1})?";
    private final String ROOM = "(?<room>.*?室)?";
    private final String BASEMENTSTR = "(?<basementStr>屋頂突出.*層|地下.*層|地下.*樓|地下|地下室|底層|屋頂|頂樓|屋頂突出物|屋頂樓|頂層|頂加|頂)?";
    private final String ADDRREMAINS = "(?<addrRemains>.+)?";
    private final String REMARK = "(?<remark>[\\(\\{\\〈\\【\\[\\〔\\『\\「\\「\\《\\（](.*?)[\\)\\〉\\】\\]\\〕\\』\\」\\}\\」\\》\\）])?";
    //〈〉【】[]〔〕()『』「」{}「」《》（）


    /**
     * 地址切割
     *
     * @param origninalAddress
     * @param address
     * @return
     */
    public Address parseAddress(String origninalAddress, Address address) {


        /** 第二次切割條件:
         * (1) 含有地下室屋頂字眼 setAddress() 會將address.originalAddress中的 basementStr為 basement:一樓放入address物件後丟進來再切一次
         * (2-1) 含有臨建特附 handleAddressRemains() 會將 address.originalAddress中 臨建特附 拔除，給予address.numType值後，再丟進來切割
         * (2-1) 地名沒有被切出來 handleAddressRemains() 會取出 address.addressRemains的 中文 放入address.area後，並從address.originalAddress中移除後，，再丟進來切割
         */

        /**表示初次切割**/
        if (address == null) {
            address = new Address();
        }

       /**去除特殊符號、字元*/
        address.setCleanAddress(cleanAddress(origninalAddress));


        /**redis查詢所有alias，要拼在正則後面**/
        Map<String, Set<String>> allKeys = new LinkedHashMap<>();
        allKeys = findAllKeys();
        /**先把有 鄉、鎮、市、區、村、里、樓 等字眼的area拿出來，從原始地址中拔除 -> Redis.key = SPECIAL_AREA:**/
        origninalAddress = findSpecialArea(allKeys, address, origninalAddress);
        /**組正則表達式**/
        String pattern = getPattern(allKeys);
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(origninalAddress);
        if (matcher.matches()) {
            /**這裡會做檢查，看要不要再切割一次*/
            return setAddress(matcher, address);
        }
        return address;
    }


    /***
     * 只有初次切割會來這
     * 帶有 "村"、"鄉"、"鎮"、"市"、"區"的特殊字 的 AREA 的地名
     * 如果有符合特殊條件，會放入setArea,然後從原始地址中拔出
     * @param allKeys
     * @param address
     * @param origninalAddress
     * @return
     */
    public String findSpecialArea(Map<String, Set<String>> allKeys, Address address, String origninalAddress) {
        String newArea = "(" + String.join("|", allKeys.get("SPECIAL_AREA:")) + ")";
        log.info("特殊地名SPECIAL_AREA:組成正則 :{}", newArea);
        Pattern patternForSpecialArea = Pattern.compile(newArea);
        Matcher matcherForSpecialArea = patternForSpecialArea.matcher(origninalAddress);
        /**如果有符合特殊情況，會setArea,然後從原始地址中拔出*/
        if (matcherForSpecialArea.find()) {
            log.info(" SPECIAL_AREA: 匹配到的部分:{},準備放入address.area", matcherForSpecialArea.group());
            address.setArea(matcherForSpecialArea.group());
            origninalAddress = origninalAddress.replace(matcherForSpecialArea.group(), "");
        }
        return origninalAddress;
    }


    /**
     * 去除特殊字元
     *
     * @param originalAddress
     * @return
     */
    public static String cleanAddress(String originalAddress) {
        if (originalAddress == null || originalAddress.isEmpty()) {
            return originalAddress;
        }
        // 去掉 "台灣省" 並去除特殊字元
        return originalAddress.replace("台灣省", "").replace("福建省","")
                .replaceAll("[`!@#$%^&*+=|';',./！@#￥%……&*+|‘”“’。，\\\\\\s]+", "");
    }


    /**
     * 有remain，但不是臨建特附
     * @param address
     * @return
     */
    public Address parseArea(Address address) {
        log.info("從remains切地名:{},準備取出中文部分當作Area", address.getAddrRemains());
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
                break;
            }
        }
        //如果有再找到area，就把area砍掉，切出其他address片段
        if (StringUtils.isNotNullOrEmpty(match)) {
            address.setArea(match);
            String cleanAddress = address.getCleanAddress();
            log.info("匹配到的中文部分：{}", match);
            int lastIndex = cleanAddress.lastIndexOf(match);
            log.info("把找到的AREA從INPUT的ADDRESS刪掉，再切一次");
            String newAddressString = cleanAddress.substring(0, lastIndex) + cleanAddress.substring(lastIndex + match.length());
            address = parseAddress(newAddressString, address);
        }
        log.info("parseArea後，再切割一次的Address:{}", address);
        return address;
    }


    /**
     * SPECIAL_AREA:XXX ->是給VILLIAGE用的特殊情況
     * SPECIALLANE -> 鐵路.*巷|丹路.*巷
     *
     * @param allKeys
     * @return
     */

    private String getPattern(Map<String, Set<String>> allKeys) {
        try {
            /**看log**/
            showLog(allKeys);
        } catch (Exception e) {
            log.error("findAllKeys error: {}", e.getMessage());
        }
        String newCounty = String.format(COUNTY, String.join("|", allKeys.get("COUNTY_ALIAS:")));
        String newTown = String.format(TOWN, String.join("|", allKeys.get("TOWN_ALIAS:")));
        /**SPECIAL_AREA(特殊地名)加進VILLAGE正則是為了在里階段排除它們**/
        String newVillage = String.format(VILLAGE, "(?!" + String.join("|", allKeys.get("SPECIAL_AREA:")) + ")",  String.join("|",allKeys.get("VILLAGE_ALIAS:")));
        String newRoad = String.format(ROAD, String.join("|", allKeys.get("ROAD_ALIAS:")));
        return newCounty + newTown + newVillage + NEIGHBOR + SPECIALLANE + newRoad + LANE + ALLEY + SUBALLEY + NUMFLR1 + NUMFLR2 + NUMFLR3 + NUMFLR4 + NUMFLR5 + CONTINUOUS_NUM + ROOM + BASEMENTSTR + REMARK + ADDRREMAINS;
    }

    private void showLog(Object obj){
        if(obj instanceof List){
            List list = (List) obj;
           list.forEach(ele->{
               log.info("list:{}",ele);
           });
            return;
        }

        if(obj instanceof Set){
            Set set = (Set) obj;
            set.forEach(ele->{
                log.info("obj:{}",ele);
            });
            return;
        }

        if(obj instanceof Map){
            Map map = (Map) obj;
            map.forEach((key,value)->{
                log.info("key:{},value:{}",key,value);
            });
           return;
        }
        log.info("obj:{}",obj.toString());
    }


    /**
     * 用於拼接成正則，非比對cd
     * 縣市  "COUNTY_ALIAS:" -> 地址片段+別名
     * 市區正則  "TOWN_ALIAS:" -> 地址片段+別名
     * 村里正則  "VILLAGE_ALIAS:" -> 地址片段+別名
     * 路名正則 "ROAD_ALIAS:" -> 地址片段+別名
     * 村里正則  "SPECIAL_AREA:" ->  地址片段+別名
     * @return
     */
    private Map<String, Set<String>> findAllKeys() {
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

        /**處理地下一層、地下、屋頂的情況**/
        String basementString = matcher.group("basementStr");
        if (StringUtils.isNotNullOrEmpty(basementString)) {
            /***
             * 地下室1f -> basemenstr:1,origirnalAddress:basement:1f
             * 屋頂2F -> basemenstr:2,origirnalAddress:basement:2F
             * 再切割一次
             * origirnalAddress:basement:2F -> basemenstr:2,num_flr1_5: basement:2F,
             */
            String parseBasement = parseBasement(basementString, address.getCleanAddress(), address);
            log.info("處理完basementStr的address:{},地址:{}",address,parseBasement);
            log.info("準備再次切割!!!");
            return parseAddress(parseBasement, address);
        }
        address.setZipcode(matcher.group("zipcode"));
        address.setCounty(matcher.group("county"));
        address.setTown(matcher.group("town"));
        address.setVillage(matcher.group("village"));
        address.setNeighbor(matcher.group("neighbor"));
        address.setRoad(matcher.group("road"));
        /**speciallane:鐵路*巷*/
        address.setLane(matcher.group("speciallane") != null ? matcher.group("speciallane") : matcher.group("lane"));
        /**最後 alley 會是 alley + subAlley */
        address.setAlley(matcher.group("alley"));
        address.setSubAlley(matcher.group("subAlley"));
        /***當層有值，代表前面所有層都有值**/
        /**parseBasementForBF 處理 basement:2F -> 2樓,basement:-2 -> 之2*/
        address.setNumFlr1(parseBasementForBF(matcher.group("numFlr1"), address));
        address.setNumFlr2(parseBasementForBF(matcher.group("numFlr2"), address));
        address.setNumFlr3(parseBasementForBF(matcher.group("numFlr3"), address));
        address.setNumFlr4(parseBasementForBF(matcher.group("numFlr4"), address));
        address.setNumFlr5(parseBasementForBF(matcher.group("numFlr5"), address));
        /**連號:45一號，表示NUN_FLR沒有切乾淨**/
        address.setContinuousNum(matcher.group("continuousNum"));
        address.setRoom(matcher.group("room"));
        address.setAddrRemains(matcher.group("addrRemains"));
        address.setRemark(matcher.group("remark"));
        return address;
    }

    /***
     * 52號地下室 ->address.basement = 1, origrinalAddress= 52號
     * 52號頂加 ->address.basement = 2, origrinalAddress= 52號
     * 52號地下1樓 -> address.basement = 1, origrinalAddress= 52號basement:一樓
     * 52號頂加1樓 -> address.basement = 2, origrinalAddress= 52號basement:一樓
     * @param basementString
     * @param origninalAddress
     * @param address
     * @return
     */
    private String parseBasement(String basementString, String origninalAddress, Address address) {
        String[] basemantPattern1 = {"地下層", "地下", "地下室", "底層"};
        String[] basemantPattern2 = {".*地下.*層.*", ".*地下室.*層.*", ".*地下.*樓.*", "屋頂突出.*層"};
        String[] roof1 = {"屋頂", "頂樓", "屋頂突出物", "屋頂樓", "頂層", "頂加", "頂"};
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
                    if (basementString.contains("頂")) { //屋頂突出.*層
                        address.setBasementStr("2");
                    } else {
                        address.setBasementStr("1");
                    }
                    break;
                }
            }
        }
        return origninalAddress;
    }


    //再PARSE一次已經在FLR_NUM_1~5 的BF、B1F

    /**
     * todo:要再加一段整段'整棟樓'的正則，有的話 '整棟樓' 從numFlr拿掉 ,basementStr == 0
     *
     * @param input
     * @param address
     * @return
     */
    private String parseBasementForBF(String input, Address address) {
        if (StringUtils.isNotNullOrEmpty(input)) {
            String[] basemantPattern1 = {"BF", "bf", "B1", "b1", "Ｂ１", "ｂ１", "ＢＦ", "ｂｆ"};
            String[] basemantPattern2 = {".*B.*樓", ".*b.*樓", ".*Ｂ.*樓", ".*ｂ.*樓", ".*B.*F", ".*b.*f", ".*Ｂ.*Ｆ", ".*ｂ.*ｆ"};
            String[] basemantPattern3 = {"整棟樓"};
            if (Arrays.asList(basemantPattern1).contains(input)) {
                log.info("basemantPattern1:{}", input);
                address.setBasementStr("1");
                return "一樓";
            }

            if (Arrays.asList(basemantPattern3).contains(input)) {
                log.info("basemantPattern3:{}", input);
                address.setBasementStr("0");
                return "";
            }

            for (String basemantPattern : basemantPattern2) {
                Pattern regex = Pattern.compile(basemantPattern);
                Matcher basemantMatcher = regex.matcher(input);
                if (basemantMatcher.matches()) {
                    // 提取數字
                    String numericPart = extractNumericPart(input);
                    log.info("basementString 提取數字部分:{} ", numericPart);
                    address.setBasementStr("1");
                    //換成中文數字+樓
                    return replaceWithChineseNumber(numericPart) + "樓";

                }
            }
        }
        //todo:整棟樓
        //如果都沒有符合b1的格式，表示沒有地下室的字眼，就返回原字串即可
        return input;
    }


    //如果還是有連在一起的地址，要切開EX.1之10樓，要切成"1之"，"10樓"
    public Map<String, Object> parseNumFlrAgain(String rawNumFLR, String flrType) {
        Map<String, Object> map = new HashMap();
        final String numFlrFirst = "(?<numFlrFirst>[之-－]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR + "+[FｆＦf]|" + ALL_CHAR + "+[號樓FｆＦf之-－區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
        final String numFlrSecond = "(?<numFlrSecond>[之-－]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR + "+[FｆＦf]|" + ALL_CHAR + "+[號樓FｆＦf之-－區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + ")?";
        Pattern regex = Pattern.compile(numFlrFirst + numFlrSecond);
        Matcher matcher = regex.matcher(rawNumFLR);
        map.put("isParsed", false);
        if (matcher.matches() && matcher.group("numFlrSecond") != null) {
            String first = matcher.group("numFlrFirst");
            String second = matcher.group("numFlrSecond");
            log.info("再切割一次，numFlrFirst==>{}", first);
            log.info("再切割一次，numFlrSecond==>{}", second);
            map.put("isParsed", true);
            map.put("numFlrFirst", first);
            map.put("numFlrSecond", second);
            map.put("flrType", flrType);
        }
        return map;
    }

    /***
     * segExistNum 長度10
     * "COUNTY", "TOWN", "VILLAGE", "ROAD", "AREA", "LANE", "ALLEY", (inedex:0~6)
     * "NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5" (index:7,有寫1個value就為1)
     * 新增: "NEIGHBOR,""ROOM" (index 8,9)
     * @param fullAddress
     * @param segExistNum
     * @return
     */
    public String checkJoinStepBySegNum(String fullAddress, String segExistNum, String joinStep) {
        //todo:要先判斷原本的joinStep是什麼
        log.info("目前判斷的joinStep:{},要件清單:{}",joinStep,segExistNum);
        log.info("撈出的正確地址:{}", fullAddress);
        char[] segArray = segExistNum.toCharArray();

        /** 1 = 有寫 && 有找到 ; 0 = 沒寫 | 沒找到*/
        boolean hasCounty = "1".equals(String.valueOf(segArray[0]));
        boolean hasTown = "1".equals(String.valueOf(segArray[1]));
        boolean hasVillage = "1".equals(String.valueOf(segArray[2]));
        boolean hasNeighbor = "1".equals(String.valueOf(segArray[segArray.length-1]));

        joinStep = checkJC3(joinStep, hasCounty, hasTown, hasVillage, hasNeighbor);
        joinStep = checkJB1(joinStep,fullAddress,hasCounty,hasTown,hasVillage,hasNeighbor);
        joinStep = checkJA2AndJA3(joinStep,fullAddress,hasCounty,hasTown,hasVillage,hasNeighbor);
        return joinStep;
      }


    private String checkJC3(String joinStep, boolean hasCounty, boolean hasTown, boolean hasVillage, boolean hasNeighbor){
        String result = joinStep;
        if(joinStep.startsWith("JC3")){
            if (!hasVillage) {
                result =   "JA3";
            }

            if (!hasNeighbor) {
                result =  "JA2";
            }

            if (!hasCounty || !hasTown) {
                result =  "JA1";
            }

            if(!hasCounty && !hasTown){
                result =  "JE431";
            }
        }
        return result;
    }

    private String checkJB1(String joinStep, String fullAddress, boolean hasCounty, boolean hasTown, boolean hasVillage, boolean hasNeighbor){
        Pattern pattern ;
        Matcher matcher;
        String result = joinStep;
        //先檢查JB1(退室)有沒有誤判，完整地址是否含室
        if(joinStep.startsWith("JB1")){
            pattern = Pattern.compile(ROOM);
            matcher = pattern.matcher(fullAddress);
            if (!matcher.find()) {
                //完整地址無室，joinStep要往前找JA1,JA2,JA3,JE431..)

                if (!hasVillage) {
                    result = "JA3";
                }

                if (!hasNeighbor) {
                    result = "JA2";
                }

                if (!hasCounty || !hasTown) {
                    result =  "JA1";
                }

                if(!hasCounty && !hasTown){
                    result = "JE431";
                }
            }
        }
        return result;
    }

    private String checkJA2AndJA3(String joinStep, String fullAddress, boolean hasCounty, boolean hasTown, boolean hasVillage, boolean hasNeighbor){
        //用判斷過室的 joinStep 繼續檢查是退鄰 還是 退里
        String result =joinStep;
        if (joinStep.startsWith("JA2")||joinStep.startsWith("JA3")) {
            Pattern pattern = Pattern.compile(NEIGHBOR);
            Matcher matcher = pattern.matcher(fullAddress);
            //完整地址無鄰，join_step不會是退鄰，所以要往前判斷為要件不齊、JA1
            if (!matcher.find()) {
                //地址無鄰,join_step 卻是退鄰，往前找
                if (!hasCounty || !hasTown) {
                    result = "JA1";
                }

                //join_step JE，要件不齊
                if(!hasCounty && !hasTown){
                    result = "JE431";
                }
            }else{
                //todo:要判斷里、鄰都先錯是要以誰為優先
                //地址有鄰，要件清單 鄰=0,要退回JA
                if(!hasNeighbor){
                    result = "JA2";
                }

                if (!hasCounty || !hasTown) {
                    result = "JA1";
                }

                //join_step JE，要件不齊
                if(!hasCounty && !hasTown){
                    result = "JE431";
                }
            }
        }
        return result;
    }

    public IbdTbAddrCodeOfDataStandardDTO filterNumFlrPos(IbdTbAddrCodeOfDataStandardDTO dto,String completeNumFlr){
        String fullAddress = dto.getFullAddress();
        log.info("fullAddress:{}",fullAddress);
        log.info("completeNumFlr:{}",completeNumFlr);
        //去除室
        Pattern pattern = Pattern.compile(ROOM);
        Matcher matcher = pattern.matcher(fullAddress);
        String withoutRoom =  fullAddress;
        if(matcher.find()){
            //122號6室
            withoutRoom = fullAddress.substring(0, fullAddress.lastIndexOf(matcher.group()));
        }

        completeNumFlr  = NumberParser.replaceWithFullWidthNumber(completeNumFlr);
        log.info("withoutRoom:{}",withoutRoom);
        log.info("completeNumFLr:{}",completeNumFlr);
        if(withoutRoom.endsWith(completeNumFlr)){
            log.info("withoutRoom :{} , is endwith:{}",withoutRoom,completeNumFlr);
            return dto;
        }
        return null;
    }
}

