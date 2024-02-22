package com.example.pentaho.component;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class JobParams implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("JobParamsJson")
    private String JobParamsJson;

    /**
    * 呼叫的jobName
    * */
    @JsonProperty("jobName")
    private String jobs;


    /**
     * 檔案傳進來後，重新編輯過的檔名
     * */
    @JsonProperty("filename")
    private String FILE;

    /**
     * 執行批次的ID
     */
    @JsonProperty("Id")
    private String BATCH_ID;


    /**
     * csv檔案的ID
     */
    @JsonProperty("originalFileId")
    private String BATCHFORM_ORIGINAL_FILE_ID;

    /**
     * 使用者ID
     */
    @JsonProperty("userId")
    private String USER_ID;


    /**
     * 使用者單位
     */
    @JsonProperty("dataSrc")
    private String DATA_SRC;

    /**
     * 日期
     * yyyymmdd
     */
    @JsonProperty("dataDate")
    private String DATA_DATE;

    /**
     *
     * processedCounts
     */
    @JsonProperty("processedCounts")
    private Integer processedCounts;


    /**
     *
     * status
     */
    private String status;

    public JobParams(String jobParamsJson, String jobs, String FILE, String BATCH_ID, String BATCHFORM_ORIGINAL_FILE_ID, String USER_ID, String DATA_SRC, String DATA_DATE) {
        JobParamsJson = jobParamsJson;
        this.jobs = jobs;
        this.FILE = FILE;
        this.BATCH_ID = BATCH_ID;
        this.BATCHFORM_ORIGINAL_FILE_ID = BATCHFORM_ORIGINAL_FILE_ID;
        this.USER_ID = USER_ID;
        this.DATA_SRC = DATA_SRC;
        this.DATA_DATE = DATA_DATE;
    }

    public String getDATA_SRC() {
        return DATA_SRC;
    }

    public void setDATA_SRC(String DATA_SRC) {
        this.DATA_SRC = DATA_SRC;
    }

    public String getDATA_DATE() {
        return DATA_DATE;
    }

    public void setDATA_DATE(String DATA_DATE) {
        this.DATA_DATE = DATA_DATE;
    }

    public String getUSER_ID() {
        return USER_ID;
    }

    public void setUSER_ID(String USER_ID) {
        this.USER_ID = USER_ID;
    }


    public JobParams() {
        this.jobs = jobs;
    }

    public String getJobs() {
        return jobs;
    }

    public void setJobs(String jobs) {
        this.jobs = jobs;
    }

    public JobParams(String jobs) {
        this.jobs = jobs;
    }


    public String getFILE() {
        return FILE;
    }

    public void setFILE(String FILE) {
        this.FILE = FILE;
    }

    public String getBATCH_ID() {
        return BATCH_ID;
    }

    public void setBATCH_ID(String BATCH_ID) {
        this.BATCH_ID = BATCH_ID;
    }



    public String getJobParamsJson() {
        return JobParamsJson;
    }

    public void setJobParamsJson(String jobParamsJson) {
        JobParamsJson = jobParamsJson;
    }

    public String getBATCHFORM_ORIGINAL_FILE_ID() {
        return BATCHFORM_ORIGINAL_FILE_ID;
    }

    public void setBATCHFORM_ORIGINAL_FILE_ID(String BATCHFORM_ORIGINAL_FILE_ID) {
        this.BATCHFORM_ORIGINAL_FILE_ID = BATCHFORM_ORIGINAL_FILE_ID;
    }

    public Integer getProcessedCounts() {
        return processedCounts;
    }

    public void setProcessedCounts(Integer processedCounts) {
        this.processedCounts = processedCounts;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "JobParams{" +
                "JobParamsJson='" + JobParamsJson + '\'' +
                ", jobs='" + jobs + '\'' +
                ", FILE='" + FILE + '\'' +
                ", BATCH_ID='" + BATCH_ID + '\'' +
                ", BATCHFORM_ORIGINAL_FILE_ID='" + BATCHFORM_ORIGINAL_FILE_ID + '\'' +
                ", USER_ID='" + USER_ID + '\'' +
                ", DATA_SRC='" + DATA_SRC + '\'' +
                ", DATA_DATE='" + DATA_DATE + '\'' +
                ", processedCounts=" + processedCounts +
                ", status='" + status + '\'' +
                '}';
    }
}
