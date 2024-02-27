package com.example.pentaho.component;

import org.springframework.stereotype.Component;

import java.sql.Timestamp;

public class IbdTbIhChangeDoorplateHis {


    //ADDRESS_ID BSZ7538-0


    private String addressId;

    private String newAdr;

    private String hisAdr;
    private String hisCity;

    private String hisVillage;

    private String hisNeighborCd;

    private String hisNeighbor;

    private String hisAddress;

    private String updateDt;

    private String UpdateCode;

    private String businessCode;

    private Integer lv;

    private Integer doorplateSeq;

    private Integer historySeq;

    private String status;

    private String dataYr;

    private String adrVersion;

    private Timestamp etlDt;


    public IbdTbIhChangeDoorplateHis() {
    }

    public IbdTbIhChangeDoorplateHis(String addressId) {
        this.addressId = addressId;
    }

    public String getNewAdr() {
        return newAdr;
    }

    public void setNewAdr(String newAdr) {
        this.newAdr = newAdr;
    }

    public String getHisAdr() {
        return hisAdr;
    }

    public void setHisAdr(String hisAdr) {
        this.hisAdr = hisAdr;
    }

    public String getHisCity() {
        return hisCity;
    }

    public void setHisCity(String hisCity) {
        this.hisCity = hisCity;
    }

    public String getHisVillage() {
        return hisVillage;
    }

    public void setHisVillage(String hisVillage) {
        this.hisVillage = hisVillage;
    }

    public String getHisNeighborCd() {
        return hisNeighborCd;
    }

    public void setHisNeighborCd(String hisNeighborCd) {
        this.hisNeighborCd = hisNeighborCd;
    }

    public String getHisNeighbor() {
        return hisNeighbor;
    }

    public void setHisNeighbor(String hisNeighbor) {
        this.hisNeighbor = hisNeighbor;
    }

    public String getHisAddress() {
        return hisAddress;
    }

    public void setHisAddress(String hisAddress) {
        this.hisAddress = hisAddress;
    }

    public String getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(String updateDt) {
        this.updateDt = updateDt;
    }

    public String getUpdateCode() {
        return UpdateCode;
    }

    public void setUpdateCode(String updateCode) {
        UpdateCode = updateCode;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public Integer getLv() {
        return lv;
    }

    public void setLv(Integer lv) {
        this.lv = lv;
    }

    public Integer getDoorplateSeq() {
        return doorplateSeq;
    }

    public void setDoorplateSeq(Integer doorplateSeq) {
        this.doorplateSeq = doorplateSeq;
    }

    public Integer getHistorySeq() {
        return historySeq;
    }

    public void setHistorySeq(Integer historySeq) {
        this.historySeq = historySeq;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDataYr() {
        return dataYr;
    }

    public void setDataYr(String dataYr) {
        this.dataYr = dataYr;
    }

    public String getAdrVersion() {
        return adrVersion;
    }

    public void setAdrVersion(String adrVersion) {
        this.adrVersion = adrVersion;
    }

    public Timestamp getEtlDt() {
        return etlDt;
    }

    public void setEtlDt(Timestamp etlDt) {
        this.etlDt = etlDt;
    }

    public String getAddressId() {
        return addressId;
    }

    public void setAddressId(String addressId) {
        this.addressId = addressId;
    }

    @Override
    public String toString() {
        return "IbdTbIhChangeDoorplateHis{" +
                "addressId='" + addressId + '\'' +
                ", newAdr='" + newAdr + '\'' +
                ", hisAdr='" + hisAdr + '\'' +
                ", hisCity='" + hisCity + '\'' +
                ", hisVillage='" + hisVillage + '\'' +
                ", hisNeighborCd='" + hisNeighborCd + '\'' +
                ", hisNeighbor='" + hisNeighbor + '\'' +
                ", hisAddress='" + hisAddress + '\'' +
                ", updateDt='" + updateDt + '\'' +
                ", UpdateCode='" + UpdateCode + '\'' +
                ", businessCode='" + businessCode + '\'' +
                ", lv=" + lv +
                ", doorplateSeq=" + doorplateSeq +
                ", historySeq=" + historySeq +
                ", status='" + status + '\'' +
                ", dataYr='" + dataYr + '\'' +
                ", adrVersion='" + adrVersion + '\'' +
                ", etlDt=" + etlDt +
                '}';
    }
}
