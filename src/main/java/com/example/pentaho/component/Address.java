package com.example.pentaho.component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Address {

    private String zipcode;
    private String county;
    private String town;
    private String village;
    private String neighbor;
    private String road;
    private String section;
    private String lane;
    private String alley;
    private String num;
    private String seq;
    private String floor;
    private String others;
    private boolean isParseSuccessed;
    private String originalAddress;

    public Address() {
    }
    public Address(String address) {
        this.originalAddress = address;
        this.parseByRegex(address);
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public String getNeighbor() {
        return neighbor;
    }

    public void setNeighbor(String neighbor) {
        this.neighbor = neighbor;
    }

    public String getRoad() {
        return road;
    }

    public void setRoad(String road) {
        this.road = road;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getLane() {
        return lane;
    }

    public void setLane(String lane) {
        this.lane = lane;
    }

    public String getAlley() {
        return alley;
    }

    public void setAlley(String alley) {
        this.alley = alley;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getOthers() {
        return others;
    }

    public void setOthers(String others) {
        this.others = others;
    }

    public boolean isParseSuccessed() {
        return isParseSuccessed;
    }

    public void setParseSuccessed(boolean parseSuccessed) {
        isParseSuccessed = parseSuccessed;
    }

    public String getOriginalAddress() {
        return originalAddress;
    }

    public void setOriginalAddress(String originalAddress) {
        this.originalAddress = originalAddress;
    }

    private void parseByRegex(String address) {
//        String pattern = "(?<zipcode>(^\\d{5}|^\\d{3})?)(?<county>[臺台][北中南東]|新竹|南投|嘉義|彰化|[縣市])?(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]))?(?<village>\\D+?[村里])?(?<neighbor>\\d+[鄰])?(?<road>\\D+?(村路|[路街道段]))?(?<section>\\D?段)?(?<lane>\\d+巷)?(?<alley>\\d+弄)?(?<num>\\d+號?)?(?<seq>-\\d+?(號))?(?<floor>\\d+樓)?(?<others>.+)?";
        String dynamicPart = "|新北(市)?|桃園(縣)?|新竹(縣)?|苗栗(縣)?|彰化(縣)?|雲林(縣)?|嘉義(縣)?|花蓮(縣)?|南投(縣)?|";
        String pattern = "(?<zipcode>(^\\d{5}|^\\d{3})?)(?<county>[臺台][北中南東]([縣市])?"+dynamicPart+")?(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]))?(?<village>\\D+?[村里])?(?<neighbor>\\d+[鄰])?(?<road>\\D+?(村路|[路街道段]))?(?<section>\\D?段)?(?<lane>\\d+巷)?(?<alley>\\d+弄)?(?<num>\\d+號?)?(?<seq>-\\d+?(號))?(?<floor>\\d+樓)?(?<others>.+)?";

        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(address);
        if (matcher.matches()) {
            this.isParseSuccessed = true;
            this.zipcode = matcher.group("zipcode");
            this.county = matcher.group("county");
            this.town = matcher.group("town");
            this.village = matcher.group("village");
            this.neighbor = matcher.group("neighbor");
            this.road = matcher.group("road");
            this.section = matcher.group("section");
            this.lane = matcher.group("lane");
            this.alley = matcher.group("alley");
            this.num = matcher.group("num");
            this.seq = matcher.group("seq");
            this.floor = matcher.group("floor");
            this.others = matcher.group("others");
        }
    }
}

