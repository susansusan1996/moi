package com.example.pentaho.component;


import io.swagger.v3.oas.annotations.media.Schema;

public class IbdTbIhChangeDoorplateHis {


    //ADDRESS_ID BSZ7538-0

    /**
     * 地址編碼
     */
    @Schema(description = "地址識別碼", example = "BSZ7538-0")
    private String addressId;

    /**
     * 地址
     */
    @Schema(description = "標準地址", example = "10010020臺灣省嘉義縣朴子市大葛里027鄰祥和三路西段２３號三樓３９")
    private String hisAdr;


    /**緯度*/
    @Schema(description = "座標-緯度", example = "120.289578060453740")
    private String wgsX;

    /**經度*/
    @Schema(description = "座標-經度", example = "23.455075446688465")
    private String wgsY;

    /**更新種類*/
    @Schema(description = "異動原因", example = "行政區域調整")
    private String updateType;

    /**更新時間*/
    @Schema(description = "異動日期", example = "20200701")
    private String updateDt;

    /**門牌是否廢止*/
    @Schema(description = "門牌是否廢止", example = "X")
    private String status;

    /**歷史SEQ*/
    @Schema(description = "歷史SEQ", example = "11099781")
    private Integer historySeq;

    /**版本號碼*/
    @Schema(description = "版本號碼", example = "2312.2")
    private String adrVersion;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getHistorySeq() {
        return historySeq;
    }

    public void setHistorySeq(Integer historySeq) {
        this.historySeq = historySeq;
    }

    public String getAdrVersion() {
        return adrVersion;
    }

    public void setAdrVersion(String adrVersion) {
        this.adrVersion = adrVersion;
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
                ", status='" + status + '\'' +
                ", historySeq=" + historySeq +
                ", adrVersion='" + adrVersion + '\'' +
                '}';
    }
}
