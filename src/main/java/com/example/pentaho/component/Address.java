package com.example.pentaho.component;


public class Address {

    private String zipcode;
    private String county;
    private String town;
    private String village;
    private String neighbor;
    private String road;
    private String area;
    private String lane;
    private String alley;
    private String subAlley;
    private String numFlr1;
    private String numFlr2;
    private String numFlr3;
    private String numFlr4;
    private String numFlr5;
    private String seq;
    private String addrRemains;
    private boolean isParseSuccessed;
    private String originalAddress;

    public Address() {
    }

    public Address(String address) {
        this.originalAddress = address;
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

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
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

    public String getSubAlley() {
        return subAlley;
    }

    public void setSubAlley(String subAlley) {
        this.subAlley = subAlley;
    }

    public String getNumFlr1() {
        return numFlr1;
    }

    public void setNumFlr1(String numFlr1) {
        this.numFlr1 = numFlr1;
    }

    public String getNumFlr2() {
        return numFlr2;
    }

    public void setNumFlr2(String numFlr2) {
        this.numFlr2 = numFlr2;
    }

    public String getNumFlr3() {
        return numFlr3;
    }

    public void setNumFlr3(String numFlr3) {
        this.numFlr3 = numFlr3;
    }

    public String getNumFlr4() {
        return numFlr4;
    }

    public void setNumFlr4(String numFlr4) {
        this.numFlr4 = numFlr4;
    }

    public String getNumFlr5() {
        return numFlr5;
    }

    public void setNumFlr5(String numFlr5) {
        this.numFlr5 = numFlr5;
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }


    public String getAddrRemains() {
        return addrRemains;
    }

    public void setAddrRemains(String addrRemains) {
        this.addrRemains = addrRemains;
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

    @Override
    public String toString() {
        return "Address{" +
                "zipcode='" + zipcode + '\'' +
                ", county='" + county + '\'' +
                ", town='" + town + '\'' +
                ", village='" + village + '\'' +
                ", neighbor='" + neighbor + '\'' +
                ", road='" + road + '\'' +
                ", area='" + area + '\'' +
                ", lane='" + lane + '\'' +
                ", alley='" + alley + '\'' +
                ", subAlley='" + subAlley + '\'' +
                ", numFlr1='" + numFlr1 + '\'' +
                ", numFlr2='" + numFlr2 + '\'' +
                ", numFlr3='" + numFlr3 + '\'' +
                ", numFlr4='" + numFlr4 + '\'' +
                ", numFlr5='" + numFlr5 + '\'' +
                ", seq='" + seq + '\'' +
                ", addrRemains='" + addrRemains + '\'' +
                ", isParseSuccessed=" + isParseSuccessed +
                ", originalAddress='" + originalAddress + '\'' +
                '}';
    }
}

