package com.example.pentaho.utils;

import com.example.pentaho.component.Address;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressParser {

    public static Address parseAddress(String origninalAddress) {
        String dynamicPart = "|新北(市)?|宜蘭(縣)?|桃園(縣)?|苗栗(縣)?|彰化(縣)?|雲林(縣)?|花蓮(縣)?|南投(縣)?|高雄(市)?|澎湖(縣)?|金門(縣)?|連江(縣)|基隆(市)?|新竹([縣市])?|嘉義([縣市])?|";
        String pattern = "(?<zipcode>(^\\d{5}|^\\d{3})?)(?<county>[臺台][北中南東]([縣市])?" + dynamicPart + ")?(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]))?(?<village>\\D+?[村里])?(?<neighbor>\\d+[鄰])?(?<road>\\D+?(村路|[路街道段]))?(?<section>\\D?段)?(?<lane>\\d+巷)?(?<alley>\\d+弄)?(?<num>\\d+號?)?(?<seq>-\\d+?(號))?(?<floor>\\d+樓)?(?<others>.+)?";

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
            address.setSection(matcher.group("section"));
            address.setLane(matcher.group("lane"));
            address.setAlley(matcher.group("alley"));
            address.setNum(matcher.group("num"));
            address.setSeq(matcher.group("seq"));
            address.setFloor(matcher.group("floor"));
            address.setOthers(matcher.group("others"));
            return address;
        }
        return null;
    }
}

