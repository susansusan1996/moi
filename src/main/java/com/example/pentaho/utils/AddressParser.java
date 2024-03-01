package com.example.pentaho.utils;

import com.example.pentaho.component.Address;
import com.example.pentaho.repository.IbdTbAddrDataNewRepository;
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

@Component
public class AddressParser {
    private static final Logger log = LoggerFactory.getLogger(AddressParser.class);


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private IbdTbAddrDataNewRepository ibdTbAddrDataNewRepository;

    public IbdTbAddrDataNewRepository getIbdTbAddrDataNewRepository() {
        return ibdTbAddrDataNewRepository;
    }

    public void setIbdTbAddrDataNewRepository(IbdTbAddrDataNewRepository ibdTbAddrDataNewRepository) {
        this.ibdTbAddrDataNewRepository = ibdTbAddrDataNewRepository;
    }


    public Address parseAddress(String origninalAddress, Address address) {
        log.info("Address==>{}", address);
        if (address == null) {
            address = new Address();
        }
        String characters = "[0-9A-Z\\\\uFF10-\\\\uFF19一二三四五六七八九十]";
        //以下dynamic部分，可以再改成從TABLE撈
        String dynamicCountyPart = "|新北(市)?|宜蘭(縣)?|桃園(縣)?|苗栗(縣)?|彰化(縣)?|雲林(縣)?|花蓮(縣)?|南投(縣)?|高雄(市)?|澎湖(縣)?|金門(縣)?|連江(縣)|基隆(市)?|新竹([縣市])?|嘉義([縣市])?|";
        String dynamicAlleyPart = "|卓厝|安農新邨|吉祥園|蕭厝|泰安新村|美喬|１弄圳東|堤外|中興二村|溝邊|長埤|清水|南苑|二橫路|朝安|黃泥塘|建行新村|牛頭|永和山莊";

        String town = "(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]))?";
        String village = "(?<village>\\D+?[村里])?";
        String neighbor = "(?<neighbor>\\d+鄰)?";
        String road = "(?<road>\\D+?(村路|[路街道段]))?";
        String lane = "(?<lane>[0-9\\uFF10-\\uFF19]+巷)?";
        String alley = "(?<alley>[0-9\\uFF10-\\uFF19]+弄" + dynamicAlleyPart + ")?";
        String subAlley = "(?<subAlley>[0-9\\uFF10-\\uFF19一二三四五六七八九十]+[衖衕橫])?";
        String numFlr1 = "(?<numFlr1>[0-9\\uFF10-\\uFF19]+[號之樓]|basement:[一二三四五六七八九十]+樓)?";
        String numFlr2 = "(?<numFlr2>之[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+[號樓之區棟]|basement:[一二三四五六七八九十]+樓|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+(?!室))?";
        String numFlr3 = "(?<numFlr3>之[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+[號樓之區棟]|basement:[一二三四五六七八九十]+樓|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+(?!室))?";
        String numFlr4 = "(?<numFlr4>之[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+[號樓之區棟]|basement:[一二三四五六七八九十]+樓|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+(?!室))?";
        String numFlr5 = "(?<numFlr5>之[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+||basement:[一二三四五六七八九十]+樓)?";
        String room = "(?<room>[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]+室)?";
        String basementStr = "(?<basementStr>地下.*層|地下|地下室|底層|屋頂|頂樓|屋頂突出物|屋頂樓|頂層)?";
        String addrRemains = "(?<addrRemains>.+)?";
        //先把county、town撈出來，以便核對有沒有area，如果有，就把area先去掉，跑正則，把其他部分切割出來
        origninalAddress = parseCountyAndTown(origninalAddress, address);
//        if(parseCountyAndTown(origninalAddress, address)){
//            origninalAddress = origninalAddress.replace(address.getArea(),"");
//        }
        String pattern = "(?<zipcode>(^\\d{5}|^\\d{3})?)(?<county>[臺台][北中南東]([縣市])?" + dynamicCountyPart + ")?" + town + village + neighbor + road + lane + alley + subAlley + numFlr1 + numFlr2 + numFlr3 + numFlr4 + numFlr5 + room + basementStr + addrRemains;

        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(origninalAddress);

        if (matcher.matches()) {
            address.setParseSuccessed(true);

            // 特殊處理地下一層和地下的情況
            String basementString = matcher.group("basementStr");
            String[] basemantPattern1 = {"地下層", "地下", "地下室", "底層"};
            String[] basemantPattern2 = {".*地下.*層.*", ".*地下室.*層.*"};
            String[] roof = {"屋頂", "頂樓", "屋頂突出物", "屋頂樓", "底層", "頂層"};
            if (basementString != null) {
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
                            origninalAddress = origninalAddress.replaceAll(basementString, "basement:" + numericPart + "樓");
                            log.info("basementString 提取數字部分:{} ", numericPart);
                            address.setBasementStr("1");
                            break;
                        }
                    }
                }
                return parseAddress(origninalAddress, address);
            }
            address.setZipcode(matcher.group("zipcode"));
            address.setCounty(matcher.group("county"));
            address.setTown(matcher.group("town"));
            address.setVillage(matcher.group("village"));
            address.setNeighbor(matcher.group("neighbor"));
            address.setRoad(matcher.group("road"));
//            address.setArea(matcher.group("area"));
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
        return null;
    }

    private static String extractNumericPart(String input) {
        Pattern numericPattern = Pattern.compile("[一二三四五六七八九十]+");
        Matcher numericMatcher = numericPattern.matcher(input);
        if (numericMatcher.find()) {
            return numericMatcher.group();
        } else {
            return "";
        }
    }

    private String parseCountyAndTown(String input, Address address) {
        if (!input.isEmpty()) {
            String dynamicCountyPart = "|新北(市)?|宜蘭(縣)?|桃園(縣)?|苗栗(縣)?|彰化(縣)?|雲林(縣)?|花蓮(縣)?|南投(縣)?|高雄(市)?|澎湖(縣)?|金門(縣)?|連江(縣)|基隆(市)?|新竹([縣市])?|嘉義([縣市])?|";
            String town = "(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]))?";
            String addrRemains = "(?<addrRemains>.+)?";
            String patternForCountyAndTown = "(?<county>[臺台][北中南東]([縣市])?" + dynamicCountyPart + ")?" + town + addrRemains;
            Pattern pattern = Pattern.compile(patternForCountyAndTown);
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                address.setCounty(matcher.group("county"));
                address.setTown(matcher.group("town"));
                List<String> areaSet = getArea(address);
                for (String area : areaSet) {
                    if (input.contains(area)) {
                        address.setArea(area);
                        log.info("找到area==>{}:", area);
                        input = input.replace(area, "");
                    }
                }
            }
        }
        return input;
    }


    private List<String> getArea(Address address) {
        return findListByKey(address.getCounty() + ":" + address.getTown() + ":地址");
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

