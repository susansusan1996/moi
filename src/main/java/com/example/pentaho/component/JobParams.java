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
     * 申請單號(formName)
     * pentaho的param,用於job 找出csv檔 & 產出zip檔名
     * */
    @JsonProperty("formName")
    private String FORM_NAME;

    /**
     * 執行批次的ID
     * pentaho的param,用於job完成後傳回AP，AP再傳回聖森
     */
    @JsonProperty("Id")
    private String BATCH_ID;


    /**
     * csv檔案的ID
     * pentaho的param,用於job完成後傳回AP，AP再傳回聖森
     */
    @JsonProperty("originalFileId")
    private String BATCHFORM_ORIGINAL_FILE_ID;

    /**
     * 使用者ID
     */
    @JsonProperty("userId")
    private String USER_ID;


    /**
     *  使用者單位
     *  用於建立指定目錄
     *  pentaho的param，使job到指定目錄下
     */
    @JsonProperty("dataSrc")
    private String DATA_SRC;

    /**
     * 日期
     * yyyymmdd
     *  用於建立指定目錄
     *  pentaho的param，使job到指定目錄下
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
    @JsonProperty("status")
    private String STATUS;

    public JobParams(String FORM_NAME, String BATCH_ID, String BATCHFORM_ORIGINAL_FILE_ID) {
        this.FORM_NAME = FORM_NAME;
        this.BATCH_ID = BATCH_ID;
        this.BATCHFORM_ORIGINAL_FILE_ID = BATCHFORM_ORIGINAL_FILE_ID;
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


    public String getFORM_NAME() {
        return FORM_NAME;
    }

    public void setFORM_NAME(String FORM_NAME) {
        this.FORM_NAME = FORM_NAME;
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
        return STATUS;
    }

    public void setStatus(String status) {
        this.STATUS = status;
    }

    @Override
    public String toString() {
        return "JobParams{" +
                "JobParamsJson='" + JobParamsJson + '\'' +
                ", jobs='" + jobs + '\'' +
                ", FORM_NAME='" + FORM_NAME + '\'' +
                ", BATCH_ID='" + BATCH_ID + '\'' +
                ", BATCHFORM_ORIGINAL_FILE_ID='" + BATCHFORM_ORIGINAL_FILE_ID + '\'' +
                ", USER_ID='" + USER_ID + '\'' +
                ", DATA_SRC='" + DATA_SRC + '\'' +
                ", DATA_DATE='" + DATA_DATE + '\'' +
                ", processedCounts=" + processedCounts +
                ", status='" + STATUS + '\'' +
                '}';
    }
}
