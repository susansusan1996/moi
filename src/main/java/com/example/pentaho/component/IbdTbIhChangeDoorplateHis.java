package com.example.pentaho.component;

public class IbdTbIhChangeDoorplateHis {


    //ADDRESS_ID BSZ7538-0

    /**
     * 地址編碼
     */
    private String addressId;

    /**
     * 地址
     */
    private String hisAdr;


    /**緯度*/
    private String wgsX;

    /**經度*/
    private String wgsY;

    /**更新種類*/
    private String updateType;

    /**更新時間*/
    private String updateDt;

    public String getAddressId() {
        return addressId;
    }

    public void setAddressId(String addressId) {
        this.addressId = addressId;
    }

    public String getHisAdr() {
        return hisAdr;
    }

    public void setHisAdr(String hisAdr) {
        this.hisAdr = hisAdr;
    }

    public String getWgsX() {
        return wgsX;
    }

    public void setWgsX(String wgsX) {
        this.wgsX = wgsX;
    }

    public String getWgsY() {
        return wgsY;
    }

    public void setWgsY(String wgsY) {
        this.wgsY = wgsY;
    }

    public String getUpdateType() {
        return updateType;
    }

    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }

    public String getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(String updateDt) {
        this.updateDt = updateDt;
    }

    @Override
    public String toString() {
        return "IbdTbIhChangeDoorplateHis{" +
                "addressId='" + addressId + '\'' +
                ", hisAdr='" + hisAdr + '\'' +
                ", wgsX='" + wgsX + '\'' +
                ", wgsY='" + wgsY + '\'' +
                ", updateType='" + updateType + '\'' +
                ", updateDt='" + updateDt + '\'' +
                '}';
    }
}
