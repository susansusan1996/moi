package com.example.pentaho.utils;

import com.example.pentaho.component.Address;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressParser {

    public static Address parseAddress(String origninalAddress) {
        //以下dynamic部分，可以再改成從TABLE撈
        String dynamicCountyPart = "|新北(市)?|宜蘭(縣)?|桃園(縣)?|苗栗(縣)?|彰化(縣)?|雲林(縣)?|花蓮(縣)?|南投(縣)?|高雄(市)?|澎湖(縣)?|金門(縣)?|連江(縣)|基隆(市)?|新竹([縣市])?|嘉義([縣市])?|";
        String dynamicAreaPart = "|";
        String dynamicAlleyPart = "|卓厝|安農新邨|吉祥園|蕭厝|泰安新村|美喬|１弄圳東|堤外|中興二村|溝邊|長埤|清水|南苑|二橫路|朝安|黃泥塘|建行新村|牛頭|永和山莊";

        String town = "(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]))?";
        String village = "(?<village>\\D+?[村里])?";
        String neighbor = "(?<neighbor>\\d+[鄰])?";
        String road = "(?<road>\\D+?(村路|[路街道段]))?";
        String area = "(?<area>[0-9\\uFF10-\\uFF19一二三四五六七八九十]段"+dynamicAreaPart+")?";
        String lane = "(?<lane>[0-9\\uFF10-\\uFF19]+巷)?";
        String alley = "(?<alley>[0-9\\uFF10-\\uFF19]弄"+dynamicAlleyPart+")?";
        String subAlley = "(?<subAlley>[0-9\\uFF10-\\uFF19一二三四五六七八九十][衖衕橫])?";
        String numFlr1 = "(?<numFlr1>[0-9\\uFF10-\\uFF19]+[號之樓])?";
        String numFlr2 = "(?<numFlr2>[之][0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十][號樓之區棟]|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十])?";
        String numFlr3 = "(?<numFlr3>[之][0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十][號樓之區棟]|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十])?";
        String numFlr4 = "(?<numFlr4>[之][0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十]|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十][號樓之區棟]|[0-9A-Z\\uFF10-\\uFF19一二三四五六七八九十])?";
        String numFlr5 = "(?<numFlr5>之[0-9])?";
        String addrRemains = "(?<addrRemains>.+)?";
        String pattern = "(?<zipcode>(^\\d{5}|^\\d{3})?)(?<county>[臺台][北中南東]([縣市])?" + dynamicCountyPart + ")?" + town + village + neighbor + road + area + lane + alley + subAlley + numFlr1 + numFlr2 + numFlr3 + numFlr4 + numFlr5 + addrRemains;

        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(origninalAddress);
        Address address = new Address();
        if (matcher.matches()) {
            address.setParseSuccessed(true);
            address.setZipcode(matcher.group("zipcode"));
            address.setCounty(matcher.group("county"));
            address.setTown(matcher.group("town"));
            address.setVillage(matcher.group("village"));
            address.setNeighbor(matcher.group("neighbor"));
            address.setRoad(matcher.group("road"));
            address.setArea(matcher.group("area"));
            address.setLane(matcher.group("lane"));
            address.setAlley(matcher.group("alley"));
            address.setSubAlley(matcher.group("subAlley"));
            address.setNumFlr1(matcher.group("numFlr1"));
            address.setNumFlr2(matcher.group("numFlr2"));
            address.setNumFlr3(matcher.group("numFlr3"));
            address.setNumFlr4(matcher.group("numFlr4"));
            address.setNumFlr5(matcher.group("numFlr5"));
            address.setAddrRemains(matcher.group("addrRemains"));
            return address;
        }
        return null;
    }
}

