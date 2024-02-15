package com.example.pentaho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

@Entity
@Table(name = "IBD_TB_ADDR_STATISTICS_OVERALL_DEV")
public class IbdTbAddrStatisticsOverallDev {


    @Column(name = "ID")
    @Id
    private int Id;
    @Column(name = "DATASET")
    private String dataSet;
    @Column(name = "DATA_YR")
    private String dataYr;
    @Column(name = "CNT")
    private int cnt;
    @Column(name = "STEP")
    private String step;
    @Column(name = "DETAIL")
    private String detail;
    @Column(name = "ETLDT")
    private Timestamp etldt;


    public IbdTbAddrStatisticsOverallDev() {
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getDataYr() {
        return dataYr;
    }

    public void setDataYr(String dataYr) {
        this.dataYr = dataYr;
    }

    public int getCnt() {
        return cnt;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Timestamp getEtldt() {
        return etldt;
    }

    public void setEtldt(Timestamp etldt) {
        this.etldt = etldt;
    }

    @Override
    public String toString() {
        return "IbdTbAddrStatisticsOverallDev{" +
                "Id=" + Id +
                ", dataSet='" + dataSet + '\'' +
                ", dataYr='" + dataYr + '\'' +
                ", cnt=" + cnt +
                ", step='" + step + '\'' +
                ", detail='" + detail + '\'' +
                ", etldt=" + etldt +
                '}';
    }
}
