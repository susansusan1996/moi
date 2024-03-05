package com.example.pentaho.utils;

import com.example.pentaho.component.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.pentaho.utils.NumberParser.replaceWithChineseNumber;

@Component
public class AddressParser {
    private static final Logger log = LoggerFactory.getLogger(AddressParser.class);


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final String BASEMENT_PATTERN = "basement:[一二三四五六七八九十百千]+樓"; //經過一次PARSE之後，如果有地下或屋頂，都會被改為basement:開頭
    private final String ALL_CHAR = "[0-9A-ZＡ-Ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚]";
    private final String DYNAMIC_COUNTY_PART = "新北(市)?|宜蘭(縣)?|桃園([縣市])?|苗栗(縣)?|彰化(縣)?|雲林(縣)?|花蓮(縣)?|南投縣?|南投?|高雄(市)?|澎湖(縣)?|金門(縣)?|連江(縣)|基隆(市)?|新竹([縣市])?|嘉義([縣市])?|屏東(縣)?|";
    private final String DYNAMIC_ALLEY_PART = "|卓厝|安農新邨|吉祥園|蕭厝|泰安新村|美喬|１弄圳東|堤外|中興二村|溝邊|長埤|清水|南苑|二橫路|朝安|黃泥塘|建行新村|牛頭|永和山莊";
    private final String COUNTY = "(?<zipcode>(^\\d{5}|^\\d{3})?)(?<county>"+DYNAMIC_COUNTY_PART+"[臺台]{0,1}[北中南東]{1}([縣市]{0,1})?" + ")?";
    private final String TOWN = "(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]))?";
    private final String VILLAGE = "(?<village>\\D+?[村里])?";
    private final String NEIGHBOR = "(?<neighbor>" + ALL_CHAR + "+鄰)?";
    private final String ROAD = "(?<road>.+段|.+街|.+大道|.+路)?";
    private final String LANE = "(?<lane>.+巷)?";
    private final String ALLEY = "(?<alley>" + ALL_CHAR + "+弄" + DYNAMIC_ALLEY_PART + ")?";
    private final String SUBALLEY = "(?<subAlley>" + ALL_CHAR + "+[衖衕橫])?";
    private final String NUMFLR1 = "(?<numFlr1>" + ALL_CHAR + "+[號樓之區棟]|"+BASEMENT_PATTERN+")?";
    private final String NUMFLR2 = "(?<numFlr2>之" + ALL_CHAR + "+|" + ALL_CHAR + "+[號樓之區棟]|"+BASEMENT_PATTERN+"|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR3 = "(?<numFlr3>之" + ALL_CHAR + "+|" + ALL_CHAR + "+[號樓之區棟]|"+BASEMENT_PATTERN+"|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR4 = "(?<numFlr4>之" + ALL_CHAR + "+|" + ALL_CHAR + "+[號樓之區棟]|"+BASEMENT_PATTERN+"|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR5 = "(?<numFlr5>之" + ALL_CHAR + "+|"+BASEMENT_PATTERN+")?";
    private final String ROOM = "(?<room>" + ALL_CHAR + "+室)?";
    private final String BASEMENTSTR = "(?<basementStr>地下.*層|地下|地下室|底層|屋頂|頂樓|屋頂突出物|屋頂樓|頂層)?";
    private final String ADDRREMAINS = "(?<addrRemains>.+)?";


    public Address parseAddress(String origninalAddress, Address address) {
        if (address == null) {
            address = new Address();
        }
        //先把county、town撈出來，以便核對有沒有area，
        //如果有area，就把area先去掉，
        //把area去掉之後，其餘部分再跑一次正則，把其他部分切割出來
        origninalAddress = findAreaByCountyAndTown(origninalAddress, address);
        String pattern = getPattern(); //組正則表達式
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(origninalAddress);
        if (matcher.matches()) {
            return setAddress(matcher, address, origninalAddress);
        }
        return null;
    }

    private static String extractNumericPart(String input) {
        Pattern numericPattern = Pattern.compile("[一二三四五六七八九十百千0-9]+");
        Matcher numericMatcher = numericPattern.matcher(input);
        if (numericMatcher.find()) {
            return numericMatcher.group();
        } else {
            return "";
        }
    }

    private String findAreaByCountyAndTown(String input, Address address) {
        if (!input.isEmpty()) {
            String patternForCountyAndTown = COUNTY + TOWN + ADDRREMAINS;
            Pattern pattern = Pattern.compile(patternForCountyAndTown);
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                address.setCounty(matcher.group("county"));
                address.setTown(matcher.group("town"));
                List<String> areaSet = getArea(address);
                for (String area : areaSet) {
                    if (input.contains(area)) {
                        address.setArea(area);
                        log.info("找到area==>:{}", area);
                        return removeLastMatch(input, area); //從後面數來，第一個匹配的字串刪除(防止從前面刪，會有跟area重名的村里名被刪掉)
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
        return COUNTY + TOWN + VILLAGE + NEIGHBOR + ROAD + LANE + ALLEY + SUBALLEY + NUMFLR1 + NUMFLR2 + NUMFLR3 + NUMFLR4 + NUMFLR5 + ROOM + BASEMENTSTR + ADDRREMAINS;
    }

    public Address setAddress(Matcher matcher, Address address, String origninalAddress) {
        address.setParseSuccessed(true);
        String basementString = matcher.group("basementStr");
        // 特殊處理地下一層和地下的情況
        if (basementString != null && !basementString.equals("")) {
            return parseAddress(parseBasement(basementString, origninalAddress, address), address);
        }
        address.setZipcode(matcher.group("zipcode"));
        address.setCounty(matcher.group("county"));
        address.setTown(matcher.group("town"));
        address.setVillage(matcher.group("village"));
        address.setNeighbor(matcher.group("neighbor"));
        address.setRoad(matcher.group("road"));
        address.setLane(matcher.group("lane"));
        address.setAlley(matcher.group("alley"));
        address.setSubAlley(matcher.group("subAlley"));
        address.setNumFlr1(matcher.group("numFlr1"));
        address.setNumFlr2(matcher.group("numFlr2"));
        address.setNumFlr3(matcher.group("numFlr3"));
        address.setNumFlr4(matcher.group("numFlr4"));
        address.setNumFlr5(matcher.group("numFlr5"));
        address.setRoom(matcher.group("room"));
        address.setAddrRemains(matcher.group("addrRemains"));
        return address;
    }

    private List<String> getArea(Address address) {
        return findListByKey(address.getCounty() + ":" + address.getTown() + ":地址");
    }

    private String parseBasement(String basementString, String origninalAddress, Address address) {
        String[] basemantPattern1 = {"地下層", "地下", "地下室", "底層"};
        String[] basemantPattern2 = {".*地下.*層.*", ".*地下室.*層.*"};
        String[] roof = {"屋頂", "頂樓", "屋頂突出物", "屋頂樓", "底層", "頂層"};
        if (Arrays.asList(basemantPattern1).contains(basementString)) {
            origninalAddress = origninalAddress.replaceAll(basementString, "一樓");
            address.setBasementStr("1");
        } else if (Arrays.asList(roof).contains(basementString)) {
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
                    address.setBasementStr("1");
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
        ListOperations<String, String> listOps = stringRedisTemplate.opsForList();
        List<String> elements = listOps.range(key, 0, -1);
        log.info("elements:{}", elements);
        return elements;
    }


}

