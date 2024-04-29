package com.example.pentaho.component;


import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class Address {

    private String zipcode;
    private String county;
    private String countyCd;
    private String town;
    private String townCd;
    private String village;
    private String villageCd;
    private String neighbor;
    private String neighborCd;
    private String road;
    private String area;
    private String roadAreaSn;
    private String lane;
    private String laneCd;
    private String alley;
    private String subAlley;
    private String alleyIdSn;
    private String numFlr1;
    private String numFlr1Id;
    private String numFlr2;
    private String numFlr2Id;
    private String numFlr3;
    private String numFlr3Id;
    private String numFlr4;
    private String numFlr4Id;
    private String numFlr5;
    private String numFlr5Id;
    private String continuousNum;//之45一樓 (像這種連續的號碼，就會被歸在這裡)
    private String basementStr;
    private String numFlrPos;
    private String room; //室
    private String roomIdSn;
    private String seq;
    private String addrRemains;
    private boolean isParseSuccessed;
    private String originalAddress;
    private List<String> mappingId; //64碼
    private String segmentExistNumber; //紀錄user是否有輸入每個地址片段，有:1，沒有:0
    private List<LinkedHashMap<String, String>> mappingIdMap;
    private List<List<String>> MappingIdList;
    private String joinStep; //地址比對代碼
    private Set<String> seqSet;
    private String numTypeCd; //臨建特附
    private Boolean hasRoadArea; //有寫路地名為true，沒寫為false


    public Address() {
    }

    public void setProperty(String propertyName, String value) throws NoSuchFieldException, IllegalAccessException {
        Field field = getClass().getDeclaredField(propertyName);
        field.setAccessible(true);
        field.set(this, value);
    }

    public String getProperty(String propertyName) throws NoSuchFieldException, IllegalAccessException {
        Field field = getClass().getDeclaredField(propertyName);
        field.setAccessible(true);
        return (String) field.get(this);
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

    public String getContinuousNum() {
        return continuousNum;
    }

    public void setContinuousNum(String continuousNum) {
        this.continuousNum = continuousNum;
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

    public String getBasementStr() {
        return basementStr;
    }

    public void setBasementStr(String basementStr) {
        this.basementStr = basementStr;
    }

    public String getNumFlrPos() {
        return numFlrPos;
    }

    public void setNumFlrPos(String numFlrPos) {
        this.numFlrPos = numFlrPos;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }


    public String getSegmentExistNumber() {
        return segmentExistNumber;
    }

    public void setSegmentExistNumber(String segmentExistNumber) {
        this.segmentExistNumber = segmentExistNumber;
    }


    public String getJoinStep() {
        return joinStep;
    }

    public void setJoinStep(String joinStep) {
        this.joinStep = joinStep;
    }

    public String getCountyCd() {
        return countyCd;
    }

    public void setCountyCd(String countyCd) {
        this.countyCd = countyCd;
    }

    public String getTownCd() {
        return townCd;
    }

    public void setTownCd(String townCd) {
        this.townCd = townCd;
    }

    public String getVillageCd() {
        return villageCd;
    }

    public void setVillageCd(String villageCd) {
        this.villageCd = villageCd;
    }

    public String getRoadAreaSn() {
        return roadAreaSn;
    }

    public void setRoadAreaSn(String roadAreaSn) {
        this.roadAreaSn = roadAreaSn;
    }

    public String getLaneCd() {
        return laneCd;
    }

    public void setLaneCd(String laneCd) {
        this.laneCd = laneCd;
    }

    public String getAlleyIdSn() {
        return alleyIdSn;
    }

    public void setAlleyIdSn(String alleyIdSn) {
        this.alleyIdSn = alleyIdSn;
    }

    public String getNumFlr1Id() {
        return numFlr1Id;
    }

    public void setNumFlr1Id(String numFlr1Id) {
        this.numFlr1Id = numFlr1Id;
    }

    public String getNumFlr2Id() {
        return numFlr2Id;
    }

    public void setNumFlr2Id(String numFlr2Id) {
        this.numFlr2Id = numFlr2Id;
    }

    public String getNumFlr3Id() {
        return numFlr3Id;
    }

    public void setNumFlr3Id(String numFlr3Id) {
        this.numFlr3Id = numFlr3Id;
    }

    public String getNumFlr4Id() {
        return numFlr4Id;
    }

    public void setNumFlr4Id(String numFlr4Id) {
        this.numFlr4Id = numFlr4Id;
    }

    public String getNumFlr5Id() {
        return numFlr5Id;
    }

    public void setNumFlr5Id(String numFlr5Id) {
        this.numFlr5Id = numFlr5Id;
    }

    public String getRoomIdSn() {
        return roomIdSn;
    }

    public void setRoomIdSn(String roomIdSn) {
        this.roomIdSn = roomIdSn;
    }

    public String getNeighborCd() {
        return neighborCd;
    }

    public void setNeighborCd(String neighborCd) {
        this.neighborCd = neighborCd;
    }

    public Set<String> getSeqSet() {
        return seqSet;
    }

    public void setSeqSet(Set<String> seqSet) {
        this.seqSet = seqSet;
    }



    public String getNumTypeCd() {
        return numTypeCd;
    }

    public void setNumTypeCd(String numTypeCd) {
        this.numTypeCd = numTypeCd;
    }

    public Boolean getHasRoadArea() {
        return hasRoadArea;
    }

    public void setHasRoadArea(Boolean hasRoadArea) {
        this.hasRoadArea = hasRoadArea;
    }

    public List<String> getMappingId() {
        return mappingId;
    }

    public void setMappingId(List<String> mappingId) {
        this.mappingId = mappingId;
    }

    public List<LinkedHashMap<String, String>> getMappingIdMap() {
        return mappingIdMap;
    }

    public void setMappingIdMap(List<LinkedHashMap<String, String>> mappingIdMap) {
        this.mappingIdMap = mappingIdMap;
    }

    public List<List<String>> getMappingIdList() {
        return MappingIdList;
    }

    public void setMappingIdList(List<List<String>> mappingIdList) {
        MappingIdList = mappingIdList;
    }

    @Override
    public String toString() {
        return "Address{" +
                "zipcode='" + zipcode + '\'' +
                ", county='" + county + '\'' +
                ", countyCd='" + countyCd + '\'' +
                ", town='" + town + '\'' +
                ", townCd='" + townCd + '\'' +
                ", village='" + village + '\'' +
                ", villageCd='" + villageCd + '\'' +
                ", neighbor='" + neighbor + '\'' +
                ", neighborCd='" + neighborCd + '\'' +
                ", road='" + road + '\'' +
                ", area='" + area + '\'' +
                ", roadAreaSn='" + roadAreaSn + '\'' +
                ", lane='" + lane + '\'' +
                ", laneCd='" + laneCd + '\'' +
                ", alley='" + alley + '\'' +
                ", subAlley='" + subAlley + '\'' +
                ", alleyIdSn='" + alleyIdSn + '\'' +
                ", numFlr1='" + numFlr1 + '\'' +
                ", numFlr1Id='" + numFlr1Id + '\'' +
                ", numFlr2='" + numFlr2 + '\'' +
                ", numFlr2Id='" + numFlr2Id + '\'' +
                ", numFlr3='" + numFlr3 + '\'' +
                ", numFlr3Id='" + numFlr3Id + '\'' +
                ", numFlr4='" + numFlr4 + '\'' +
                ", numFlr4Id='" + numFlr4Id + '\'' +
                ", numFlr5='" + numFlr5 + '\'' +
                ", numFlr5Id='" + numFlr5Id + '\'' +
                ", continuousNum='" + continuousNum + '\'' +
                ", basementStr='" + basementStr + '\'' +
                ", numFlrPos='" + numFlrPos + '\'' +
                ", room='" + room + '\'' +
                ", roomIdSn='" + roomIdSn + '\'' +
                ", seq='" + seq + '\'' +
                ", addrRemains='" + addrRemains + '\'' +
                ", isParseSuccessed=" + isParseSuccessed +
                ", originalAddress='" + originalAddress + '\'' +
                ", mappingId='" + mappingId + '\'' +
                ", segmentExistNumber='" + segmentExistNumber + '\'' +
                ", mappingIdMap=" + mappingIdMap +
                ", MappingIdList=" + MappingIdList +
                ", joinStep='" + joinStep + '\'' +
                ", seqSet=" + seqSet +
                ", numTypeCd='" + numTypeCd + '\'' +
                ", hasRoadArea=" + hasRoadArea +
                '}';
    }
}

