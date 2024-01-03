package com.example.pentaho.model;


import org.springframework.stereotype.Component;

@Component
public class JobParams {

    private String jobName;


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
}
