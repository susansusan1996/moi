package com.example.pentaho.component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Address {

    private String zipcode;
    private String city;
    private String region;
    private String village;
    private String neighbor;
    private String road;
    private String section;
    private String lane;
    private String alley;
    private String no;
    private String seq;
    private String floor;
    private String others;
    private boolean isParseSuccessed;
    private String originalAddress;

    public Address(String address) {
        this.originalAddress = address;
        this.parseByRegex(address);
    }

    public String getZipcode() {
        return zipcode;
    }

    public String getCity() {
        return city;
    }

    public String getRegion() {
        return region;
    }

    public String getVillage() {
        return village;
    }

    public String getNeighbor() {
        return neighbor;
    }

    public String getRoad() {
        return road;
    }

    public String getSection() {
        return section;
    }

    public String getLane() {
        return lane;
    }

    public String getAlley() {
        return alley;
    }

    public String getNo() {
        return no;
    }

    public String getSeq() {
        return seq;
    }

    public String getFloor() {
        return floor;
    }

    public String getOthers() {
        return others;
    }

    public void setOriginalAddress(String originalAddress) {
        this.originalAddress = originalAddress;
    }

    public boolean isParseSuccessed() {
        return isParseSuccessed;
    }

    public String getOriginalAddress() {
        return originalAddress;
    }

    private void parseByRegex(String address) {
        String pattern = "(?<zipcode>(^\\d{5}|^\\d{3})?)(?<city>\\D+?[縣市])(?<region>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]))?(?<village>\\D+?[村里])?(?<neighbor>\\d+[鄰])?(?<road>\\D+?(村路|[路街道段]))?(?<section>\\D?段)?(?<lane>\\d+巷)?(?<alley>\\d+弄)?(?<no>\\d+號?)?(?<seq>-\\d+?(號))?(?<floor>\\d+樓)?(?<others>.+)?";
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(address);
        if (matcher.matches()) {
            this.isParseSuccessed = true;
            this.zipcode = matcher.group("zipcode");
            this.city = matcher.group("city");
            this.region = matcher.group("region");
            this.village = matcher.group("village");
            this.neighbor = matcher.group("neighbor");
            this.road = matcher.group("road");
            this.section = matcher.group("section");
            this.lane = matcher.group("lane");
            this.alley = matcher.group("alley");
            this.no = matcher.group("no");
            this.seq = matcher.group("seq");
            this.floor = matcher.group("floor");
            this.others = matcher.group("others");
        }
    }
}

