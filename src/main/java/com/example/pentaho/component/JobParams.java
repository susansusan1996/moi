package com.example.pentaho.component;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class JobParams implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("JobParamsJson")
    private String JobParamsJson;

    @JsonProperty("jobName")
    private String jobName;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("adrVersion")
    private String adrVersion;

    @JsonProperty("batchFormId")
    private String batchFormId;

    @JsonProperty("batchFormOriginalFileId")
    private String batchFormOriginalFileId;

    @JsonProperty("userId")
    private String userId;
    @JsonProperty("unitName")
    private String unitName;


    @JsonProperty("dataSrc")
    private String dataSrc;


    @JsonProperty("dataYr")
    private String dataYr;

    public JobParams(String jobParamsJson, String jobName, String filename, String adrVersion, String batchFormId, String batchFormOriginalFileId, String userId, String unitName, String dataSrc, String dataYr) {
        JobParamsJson = jobParamsJson;
        this.jobName = jobName;
        this.filename = filename;
        this.adrVersion = adrVersion;
        this.batchFormId = batchFormId;
        this.batchFormOriginalFileId = batchFormOriginalFileId;
        this.userId = userId;
        this.unitName = unitName;
        this.dataSrc = dataSrc;
        this.dataYr = dataYr;
    }

    public String getDataSrc() {
        return dataSrc;
    }

    public void setDataSrc(String dataSrc) {
        this.dataSrc = dataSrc;
    }

    public String getDataYr() {
        return dataYr;
    }

    public void setDataYr(String dataYr) {
        this.dataYr = dataYr;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public JobParams() {
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public JobParams(String jobName) {
        this.jobName = jobName;
    }


    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getAdrVersion() {
        return adrVersion;
    }

    public void setAdrVersion(String adrVersion) {
        this.adrVersion = adrVersion;
    }

    public String getBatchFormId() {
        return batchFormId;
    }

    public void setBatchFormId(String batchFormId) {
        this.batchFormId = batchFormId;
    }

    public String getBatchFormOriginalFileId() {
        return batchFormOriginalFileId;
    }

    public void setBatchFormOriginalFileId(String batchFormOriginalFileId) {
        this.batchFormOriginalFileId = batchFormOriginalFileId;
    }

    public String getJobParamsJson() {
        return JobParamsJson;
    }

    public void setJobParamsJson(String jobParamsJson) {
        JobParamsJson = jobParamsJson;
    }

    @Override
    public String toString() {
        return "JobParams{" +
                "JobParamsJson='" + JobParamsJson + '\'' +
                ", jobName='" + jobName + '\'' +
                ", filename='" + filename + '\'' +
                ", adrVersion='" + adrVersion + '\'' +
                ", batchFormId='" + batchFormId + '\'' +
                ", batchFormOriginalFileId='" + batchFormOriginalFileId + '\'' +
                '}';
    }

}
