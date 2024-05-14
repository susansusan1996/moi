package com.example.pentaho.utils;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.AliasDTO;
import com.example.pentaho.repository.AliasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final String ALL_CHAR = "[0-9A-ZＡ-Ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚壹貳參肆伍陸柒捌玖拾佰卅廿整棟]";
    private final String DYNAMIC_ALLEY_PART = "|卓厝|安農新邨|吉祥園|蕭厝|泰安新村|美喬|１弄圳東|堤外|中興二村|溝邊|長埤|清水|南苑|二橫路|朝安|黃泥塘|建行新村|牛頭|永和山莊";
    private final String COUNTY = "(?<zipcode>(^\\d{5}|^\\d{3})?)(?<county>.*縣|.*市|%s)?";
    private final String TOWN = "(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區])|%s)?";
    private final String VILLAGE = "(?<village>.*村里|.*?村|.*?里|%s)?";
    private final String NEIGHBOR = "(?<neighbor>" + ALL_CHAR + "+鄰)?";
    private final String SPECIALLANE = "(?<speciallane>鐵路.*巷|丹路.*巷)?"; //避免被切到路，直接先寫死在這裡
    private final String ROAD = "(?<road>(.*段|.*街|.*大道|.*?路(?!巷)|%s)?)";
    private final String LANE = "(?<lane>.*巷)?";
    private final String ALLEY = "(?<alley>" + ALL_CHAR + "+弄" + DYNAMIC_ALLEY_PART + ")?";
    private final String SUBALLEY = "(?<subAlley>" + ALL_CHAR + "+[衖衕橫])?";
    private final String NUMFLR1 = "(?<numFlr1>" + ALL_CHAR + "+[號Ff樓之-區棟]|" + BASEMENT_PATTERN + ")?";
    private final String NUMFLR2 = "(?<numFlr2>[之-]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]|"+ ALL_CHAR + "+[號樓FｆＦf之-區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR3 = "(?<numFlr3>[之-]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]|"+ ALL_CHAR + "+[號樓FｆＦf之-區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR4 = "(?<numFlr4>[之-]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]|"+ ALL_CHAR + "+[號樓FｆＦf之-區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR5 = "(?<numFlr5>[之-]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + BASEMENT_PATTERN + ")?";
    private final String CONTINUOUS_NUM = "(?<continuousNum>[之-]+.*[樓FｆＦf])?"; //之45一樓
    private final String ROOM = "(?<room>.*室)?";
    private final String BASEMENTSTR = "(?<basementStr>屋頂突出.*層|地下.*層|地下.*樓|地下|地下室|底層|屋頂|頂樓|屋頂突出物|屋頂樓|頂層)?";
    private final String ADDRREMAINS = "(?<addrRemains>.+)?";


    public Address parseAddress(String origninalAddress, String newAddress, Address address) {
        String input = newAddress == null ? origninalAddress : newAddress;
        //去除特殊字元
        input = input.replaceAll("[`~!@#$%^&*()+=|{}';',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘”“’。，、？\\\\\\s]+", "");
        log.info("去除特殊字元後的input:{}",input);
        if (address == null) {
            address = new Address();
            if (newAddress == null) {
                address.setOriginalAddress(origninalAddress);
            }
        }
        String pattern = getPattern(); //組正則表達式
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(input);
        if (matcher.matches()) {
            return setAddress(matcher, address, input);
        }
        return null;
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
            String originalAddress = address.getOriginalAddress();
            log.info("originalAddress:{}",originalAddress);
            log.info("match:{}",match);
            int lastIndex = originalAddress.lastIndexOf(match);
            String newAddressString = originalAddress.substring(0, lastIndex) + originalAddress.substring(lastIndex + match.length());
            log.info("newAddressString:{}",newAddressString);
            address = parseAddress(null,newAddressString, address);

        }
        log.info("切不出地名，再切一次的新address:{}",address);
        return address;
    }



    private String findArea(String input, Address address) {
        if (!input.isEmpty()) {
            List<String> areaSet = getArea().stream()
//                    .map(area -> area + "(?!.*[里區市鄉衖衕橫路道街])") //如果後面帶有[里區市鄉衖衕橫路道街]這些後綴字，就代表是不area
                    .toList();
            String areaPatternString = String.join("|", areaSet);
            log.info("areaPatternString:{}",areaPatternString);
            Pattern pattern = Pattern.compile(areaPatternString);
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                for (String area : areaSet) {
                    if (input.contains(area)) {
                        address.setArea(area);
                        log.info("找到area==>:{}", area);
                        return removeLastMatch(input, area);
                    }
                }
            }
        }
        return input;
    }


    private static String removeLastMatch(String input, String area) {
        int lastIndex = input.lastIndexOf(area);
        if (lastIndex != -1) {
            return input.substring(0, lastIndex) + input.substring(lastIndex + area.length());
        } else {
            return input;
        }
    }

    private String getPattern() {
        List<AliasDTO> aliasList = aliasRepository.queryAllAlias();
        List<String> countyList = aliasList.stream().filter(aliasDTO -> aliasDTO.getTypeName().equals("COUNTY")).map(AliasDTO::getAlias).toList();
        List<String> roadList = aliasList.stream().filter(aliasDTO -> aliasDTO.getTypeName().equals("ROAD")).map(AliasDTO::getAlias).toList();
//        List<String> areaList = aliasList.stream().filter(aliasDTO -> aliasDTO.getTypeName().equals("AREA")).map(AliasDTO::getAlias).toList();
        List<String> townList = aliasList.stream()
                .filter(aliasDTO -> aliasDTO.getTypeName().equals("TOWN"))
                .map(aliasDTO -> aliasDTO.getAlias() + "(?!.*[里段街道路巷弄號樓之區棟])")
                .toList();
        List<String> villageList = aliasList.stream().filter(aliasDTO -> aliasDTO.getTypeName().equals("VILLAGE"))
                .map(aliasDTO -> aliasDTO.getTypeName() + "(?!.*[里段街道路巷弄號樓之區棟])")
                .toList();
        String newCounty = String.format(COUNTY , String.join("|",countyList));
        String newTown = String.format(TOWN , String.join("|",townList));
        String newVillage = String.format(VILLAGE , String.join("|",villageList));
        String newRoad = String.format(ROAD , String.join("|",roadList));
        String finalPattern = newCounty + newTown + newVillage + NEIGHBOR + SPECIALLANE + newRoad + LANE + ALLEY + SUBALLEY + NUMFLR1 + NUMFLR2 + NUMFLR3 + NUMFLR4 + NUMFLR5 + CONTINUOUS_NUM + ROOM + BASEMENTSTR + ADDRREMAINS;
//        log.info("finalPattern==>{}",finalPattern);
        return finalPattern;
    }


    private String getPatternForArea() {
        String area  = "(?<area>^[^0-9０-９]+;^[^0-9０-９一二三四五六七八九十]+[一二三四五六七八九十]+[^0-9０-９一二三四五六七八九十]*;^[^0-9０-９一二三四五六七八九十]{1,7})?";
//        String finalPattern =  area + LANE + ALLEY + SUBALLEY + NUMFLR1 + NUMFLR2 + NUMFLR3 + NUMFLR4 + NUMFLR5 + CONTINUOUS_NUM + ROOM + BASEMENTSTR + ADDRREMAINS;
        log.info("finalPattern==>{}",area);
        return area;
    }

    public Address setAddress(Matcher matcher, Address address, String origninalAddress) {
        address.setParseSuccessed(true);
        String basementString = matcher.group("basementStr");
        // 特殊處理地下一層和地下的情況
        if (StringUtils.isNotNullOrEmpty(basementString)) {
            return parseAddress(null, parseBasement(basementString, origninalAddress, address), address);
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
        address.setNumFlr1(matcher.group("numFlr1"));
        address.setNumFlr2(matcher.group("numFlr2"));
        address.setNumFlr3(matcher.group("numFlr3"));
        address.setNumFlr4(matcher.group("numFlr4"));
        address.setNumFlr5(matcher.group("numFlr5"));
        address.setContinuousNum(matcher.group("continuousNum"));
        address.setRoom(matcher.group("room"));
        address.setAddrRemains(matcher.group("addrRemains"));
        return address;
    }

    private List<String> getArea() {
        return findListByKey("地名");
    }

    private String parseBasement(String basementString, String origninalAddress, Address address) {
        String[] basemantPattern1 = {"地下層", "地下", "地下室", "底層"};
        String[] basemantPattern2 = {".*地下.*層.*", ".*地下室.*層.*",".*地下.*樓.*","屋頂突出.*層"};
        String[] roof1 = {"屋頂", "頂樓", "屋頂突出物", "屋頂樓", "頂層"};
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


    /**
     * 找為LIST的值 (redis: LRANGE)
     */
    public List<String> findListByKey(String key) {
        ListOperations<String, String> listOps = stringRedisTemplate2.opsForList();
        List<String> elements = listOps.range(key, 0, -1);
        log.info("elements:{}", elements);
        return elements;
    }

    //如果還是有連在一起的地址，要切開EX.1之10樓，要切成"1之"，"10樓"
    public Map<String, Object> parseNumFlrAgain(String rawNumFLR,String flrType) {
        Map<String, Object> map = new HashMap();
        final String numFlrFirst = "(?<numFlrFirst>[之-]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR + "+[FｆＦf]|" + ALL_CHAR + "+[號樓FｆＦf之-區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
        final String numFlrSecond = "(?<numFlrSecond>[之-]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR + "+[FｆＦf]|" + ALL_CHAR + "+[號樓FｆＦf之-區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + ")?";
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


    public static void main(String[] args) {
        String input = "石岡112";

        // 定義多個正則表達式
        String[] regexArray = {
                "^[^0-9０-９]+;", // 第一個規則
                "^[^0-9０-９一二三四五六七八九十]+[一二三四五六七八九十]+[^0-9０-９一二三四五六七八九十]*;", // 第二個規則
                "^[^0-9０-９一二三四五六七八九十]{1,7}" // 第三個規則
        };

        // 逐一檢查每一個正則表達式
        for (String regex : regexArray) {
            // 建立Pattern對象
            Pattern pattern = Pattern.compile(regex);

            // 建立Matcher對象
            Matcher matcher = pattern.matcher(input);

            // 尋找匹配
            if (matcher.find()) {
                // 匹配到的部分
                String match = matcher.group();
                System.out.println("匹配到的部分：" + match);
                // 找到匹配後可以在這裡執行其他邏輯，或者直接break跳出迴圈
                break;
            }
        }
    }

}

