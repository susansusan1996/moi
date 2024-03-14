package com.example.pentaho.component;

import java.math.BigDecimal;
import java.sql.Date;

public class IbdTbAddrCodeOfDataStandardDTO {

    private Integer seq;
    private String addressId;
    private String fullAddress;
    private String validity;
    private String county;
    private String countyCd;
    private String town;
    private String townCd;
    private String postCode;
    private String postCodeDt;
    private String tcRoad;
    private String roadId;
    private String roadIdDt;
    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal wgsX;
    private BigDecimal wgsY;
    private String geohash;
    private String xyYear;
    private String adrVersion;
    private Date etldt;
    private String joinStep; //地址比對代碼

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public String getAddressId() {
        return addressId;
    }

    public void setAddressId(String addressId) {
        this.addressId = addressId;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public String getValidity() {
        return validity;
    }

    public void setValidity(String validity) {
        this.validity = validity;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCountyCd() {
        return countyCd;
    }

    public void setCountyCd(String countyCd) {
        this.countyCd = countyCd;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getTownCd() {
        return townCd;
    }

    public void setTownCd(String townCd) {
        this.townCd = townCd;
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

    public String getTcRoad() {
        return tcRoad;
    }

    public void setTcRoad(String tcRoad) {
        this.tcRoad = tcRoad;
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

    public BigDecimal getX() {
        return x;
    }

    public void setX(BigDecimal x) {
        this.x = x;
    }

    public BigDecimal getY() {
        return y;
    }

    public void setY(BigDecimal y) {
        this.y = y;
    }

    public BigDecimal getWgsX() {
        return wgsX;
    }

    public void setWgsX(BigDecimal wgsX) {
        this.wgsX = wgsX;
    }

    public BigDecimal getWgsY() {
        return wgsY;
    }

    public void setWgsY(BigDecimal wgsY) {
        this.wgsY = wgsY;
    }

    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    public String getXyYear() {
        return xyYear;
    }

    public void setXyYear(String xyYear) {
        this.xyYear = xyYear;
    }

    public String getAdrVersion() {
        return adrVersion;
    }

    public void setAdrVersion(String adrVersion) {
        this.adrVersion = adrVersion;
    }

    public Date getEtldt() {
        return etldt;
    }

    public void setEtldt(Date etldt) {
        this.etldt = etldt;
    }

    public String getJoinStep() {
        return joinStep;
    }

    public void setJoinStep(String joinStep) {
        this.joinStep = joinStep;
    }

    @Override
    public String toString() {
        return "IbdTbAddrCodeOfDataStandardDTO{" +
                "seq=" + seq +
                ", addressId='" + addressId + '\'' +
                ", fullAddress='" + fullAddress + '\'' +
                ", validity='" + validity + '\'' +
                ", county='" + county + '\'' +
                ", countyCd='" + countyCd + '\'' +
                ", town='" + town + '\'' +
                ", townCd='" + townCd + '\'' +
                ", postCode='" + postCode + '\'' +
                ", postCodeDt='" + postCodeDt + '\'' +
                ", tcRoad='" + tcRoad + '\'' +
                ", roadId='" + roadId + '\'' +
                ", roadIdDt='" + roadIdDt + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", wgsX=" + wgsX +
                ", wgsY=" + wgsY +
                ", geohash='" + geohash + '\'' +
                ", xyYear='" + xyYear + '\'' +
                ", adrVersion='" + adrVersion + '\'' +
                ", etldt=" + etldt +
                ", joinStep='" + joinStep + '\'' +
                '}';
    }
}
