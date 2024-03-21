package com.example.pentaho.utils;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.AliasDTO;
import com.example.pentaho.repository.AliasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AliasRepository aliasRepository;

    private final String BASEMENT_PATTERN = "basement:[一二三四五六七八九十百千]+樓"; //經過一次PARSE之後，如果有地下或屋頂，都會被改為basement:開頭
    private final String ALL_CHAR = "[0-9A-ZＡ-Ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚]";
    private final String DYNAMIC_ALLEY_PART = "|卓厝|安農新邨|吉祥園|蕭厝|泰安新村|美喬|１弄圳東|堤外|中興二村|溝邊|長埤|清水|南苑|二橫路|朝安|黃泥塘|建行新村|牛頭|永和山莊";
    private final String COUNTY = "(?<zipcode>(^\\d{5}|^\\d{3})?)(?<county>.*縣|.*市|%s)?";
    private final String TOWN = "(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區])|%s)?";
    private final String VILLAGE = "(?<village>\\D+?[村里]|%s)?";
    private final String NEIGHBOR = "(?<neighbor>" + ALL_CHAR + "+鄰)?";
    private final String ROAD = "(?<road>.+段|.+街|.+大道|.+路|%s)?";
    private final String LANE = "(?<lane>.+巷)?";
    private final String ALLEY = "(?<alley>" + ALL_CHAR + "+弄" + DYNAMIC_ALLEY_PART + ")?";
    private final String SUBALLEY = "(?<subAlley>" + ALL_CHAR + "+[衖衕橫])?";
    private final String NUMFLR1 = "(?<numFlr1>" + ALL_CHAR + "+[號Ff樓之-區棟]|" + BASEMENT_PATTERN + ")?";
    private final String NUMFLR2 = "(?<numFlr2>[之-]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]|"+ ALL_CHAR + "+[號樓FｆＦf之-區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR3 = "(?<numFlr3>[之-]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]|"+ ALL_CHAR + "+[號樓FｆＦf之-區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR4 = "(?<numFlr4>[之-]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]|"+ ALL_CHAR + "+[號樓FｆＦf之-區棟]|" + BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR5 = "(?<numFlr5>[之-]+" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + BASEMENT_PATTERN + ")?";
    private final String CONTINUOUS_NUM = "(?<continuousNum>[之-]+.*[樓FｆＦf])?"; //之45一樓
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
        origninalAddress = findArea(origninalAddress, address);
        String pattern = getPattern(); //組正則表達式
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(origninalAddress);
        if (matcher.matches()) {
            return setAddress(matcher, address, origninalAddress);
        }
        return null;
    }



    private String findArea(String input, Address address) {
        if (!input.isEmpty()) {
            List<String> areaSet = getArea();
            String areaPatternString = "["+String.join("|", areaSet) + "]+[里區市鄉衖衕橫路道街]"; //如果後面帶有[里區市鄉衖衕橫路道街]這些後綴字，就代表是不area
            Pattern pattern = Pattern.compile(areaPatternString);
            Matcher matcher = pattern.matcher(input);
            if (!matcher.find()) {
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
        String finalPattern = newCounty + newTown + newVillage + NEIGHBOR + newRoad + LANE + ALLEY + SUBALLEY + NUMFLR1 + NUMFLR2 + NUMFLR3 + NUMFLR4 + NUMFLR5 + CONTINUOUS_NUM + ROOM + BASEMENTSTR + ADDRREMAINS;
        log.info("finalPattern==>{}",finalPattern);
        return finalPattern;
    }

    public Address setAddress(Matcher matcher, Address address, String origninalAddress) {
        address.setParseSuccessed(true);
        String basementString = matcher.group("basementStr");
        // 特殊處理地下一層和地下的情況
        if (StringUtils.isNotNullOrEmpty(basementString)) {
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

}

