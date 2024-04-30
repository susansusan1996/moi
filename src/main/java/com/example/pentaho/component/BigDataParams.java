package com.example.pentaho.component;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BigDataParams {


    /**
     * 流水號
     * */
    @JsonProperty("id")
    private String id;


    /**
     * 流水號
     * */
    @JsonProperty("formId")
    private String formId;


    @JsonProperty("userId")
    private String userId;

    @JsonProperty("recordCounts")
    private String recordCounts;


    @JsonProperty("fileUri")
    private String fileUri;

    //COUNTY
    @JsonProperty("county")
    private String county;

    //TOWN
    @JsonProperty("town")
    private String town;

    //VILLAGE
    @JsonProperty("village")
    private String village;


    //NEIGHBOR_CD
    @JsonProperty("neighborCd")
    private String neighborCd;

    //ROAD
    @JsonProperty("road")
    private String road;
    //AREA
    @JsonProperty("area")
    private String area;
    //LANE
    @JsonProperty("lane")
    private String lane;
    //ALLEY
    @JsonProperty("alley")
    private String alley;
    //SUBALLEY
    @JsonProperty("suballey")
    private String suballey;
    //NUM_TYPE
    @JsonProperty("numType")
    private String numType;
    //NUM FLR
    @JsonProperty("numFlr")
    private String numFlr;
    //ROOM
    @JsonProperty("room")
    private String room;
    //X
    @JsonProperty("x")
    private String x;
    //Y
    @JsonProperty("y")
    private String y;
    //VERSION
    @JsonProperty("version")
    private String version;
    //TOWN_SN
    @JsonProperty("townSn")
    private String townSn;
    //GEO_HASH
    @JsonProperty("geoHash")
    private String geoHash;
    //ROAD_ID
    @JsonProperty("roadId")
    private String roadId;
    //ROAD_ID_DT
    @JsonProperty("roadIdDt")
    private String roadIdDt;
    //POST_CODE
    @JsonProperty("postCode")
    private String postCode;
    //POST_CODE_DT
    @JsonProperty("postCodeDt")
    private String postCodeDt;
    //SOURCE
    @JsonProperty("source")
    private String source;
    //VALIDITY
    @JsonProperty("validity")
    private String validity;
    //INTEGRITY
    @JsonProperty("integrity")
    private String integrity;


    public BigDataParams() {
    }

    public BigDataParams(String id) {
        this.id = id;
    }

    public BigDataParams(String id, String recordCounts, String fileUri) {
        this.id = id;
        this.recordCounts = recordCounts;
        this.fileUri = fileUri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecordCounts() {
        return recordCounts;
    }

    public void setRecordCounts(String recordCounts) {
        this.recordCounts = recordCounts;
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
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

    public String getNeighborCd() {
        return neighborCd;
    }

    public void setNeighborCd(String neighborCd) {
        this.neighborCd = neighborCd;
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

    public String getSuballey() {
        return suballey;
    }

    public void setSuballey(String suballey) {
        this.suballey = suballey;
    }

    public String getNumType() {
        return numType;
    }

    public void setNumType(String numType) {
        this.numType = numType;
    }

    public String getNumFlr() {
        return numFlr;
    }

    public void setNumFlr(String numFlr) {
        this.numFlr = numFlr;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTownSn() {
        return townSn;
    }

    public void setTownSn(String townSn) {
        this.townSn = townSn;
    }

    public String getGeoHash() {
        return geoHash;
    }

    public void setGeoHash(String geoHash) {
        this.geoHash = geoHash;
    }

    public String getRoadId() {
        return roadId;
    }

    public void setRoadId(String roadId) {
        this.roadId = roadId;
    }

    public String getRoadIdDt() {
        return roadIdDt;
    }

    public void setRoadIdDt(String roadIdDt) {
        this.roadIdDt = roadIdDt;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getPostCodeDt() {
        return postCodeDt;
    }

    public void setPostCodeDt(String postCodeDt) {
        this.postCodeDt = postCodeDt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getValidity() {
        return validity;
    }

    public void setValidity(String validity) {
        this.validity = validity;
    }

    public String getIntegrity() {
        return integrity;
    }

    public void setIntegrity(String integrity) {
        this.integrity = integrity;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    @Override
    public String toString() {
        return "BigDataParams{" +
                "id='" + id + '\'' +
                ", formId='" + formId + '\'' +
                ", userId='" + userId + '\'' +
                ", recordCounts='" + recordCounts + '\'' +
                ", fileUri='" + fileUri + '\'' +
                ", county='" + county + '\'' +
                ", town='" + town + '\'' +
                ", village='" + village + '\'' +
                ", neighborCd='" + neighborCd + '\'' +
                ", road='" + road + '\'' +
                ", area='" + area + '\'' +
                ", lane='" + lane + '\'' +
                ", alley='" + alley + '\'' +
                ", suballey='" + suballey + '\'' +
                ", numType='" + numType + '\'' +
                ", numFlr='" + numFlr + '\'' +
                ", room='" + room + '\'' +
                ", x='" + x + '\'' +
                ", y='" + y + '\'' +
                ", version='" + version + '\'' +
                ", townSn='" + townSn + '\'' +
                ", geoHash='" + geoHash + '\'' +
                ", roadId='" + roadId + '\'' +
                ", roadIdDt='" + roadIdDt + '\'' +
                ", postCode='" + postCode + '\'' +
                ", postCodeDt='" + postCodeDt + '\'' +
                ", source='" + source + '\'' +
                ", validity='" + validity + '\'' +
                ", integrity='" + integrity + '\'' +
                '}';
    }
}
