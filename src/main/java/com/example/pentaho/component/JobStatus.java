package com.example.pentaho.component;

import org.springframework.stereotype.Component;

import java.sql.Date;

@Component
public class JobStatus {

    /*表單編號*/
    private  String id ;

    /*pentaho隨機給的jobId*/
    private String jobId ;

    /*批次參數*/
    private String jobParams;

    /*pentaho回覆內容*/
    private String jobResult;

    /*pentaho回覆內容*/
    private String jobMessage;

    /*狀態»» 完成_DONE, 人工處理_MANUAL_PROCESS, 處理錯誤_SYS_FAILED*/
    private String status;

    /*執行時間*/
    private String execute_date;


    /*更新狀態時間*/
    private String update_date;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobParams() {
        return jobParams;
    }

    public void setJobParams(String jobParams) {
        this.jobParams = jobParams;
    }

    public String getJobResult() {
        return jobResult;
    }

    public void setJobResult(String jobResult) {
        this.jobResult = jobResult;
    }

    public String getJobMessage() {
        return jobMessage;
    }

    public void setJobMessage(String jobMessage) {
        this.jobMessage = jobMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExecute_date() {
        return execute_date;
    }

    public void setExecute_date(String execute_date) {
        this.execute_date = execute_date;
    }

    public String getUpdate_date() {
        return update_date;
    }

    public void setUpdate_date(String update_date) {
        this.update_date = update_date;
    }

    @Override
    public String toString() {
        return "JobStatus{" +
                "id='" + id + '\'' +
                ", jobId='" + jobId + '\'' +
                ", jobParams='" + jobParams + '\'' +
                ", jobResult='" + jobResult + '\'' +
                ", jobMessage='" + jobMessage + '\'' +
                ", status='" + status + '\'' +
                ", execute_date=" + execute_date +
                ", update_date=" + update_date +
                '}';
    }
}
