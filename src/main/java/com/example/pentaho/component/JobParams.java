package com.example.pentaho.component;


import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class JobParams implements Serializable {

    private String jobName;

    private String taskId;


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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String toString() {
        return "JobParams{" +
                "jobName='" + jobName + '\'' +
                ", taskId='" + taskId + '\'' +
                '}';
    }
}
