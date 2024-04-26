package com.example.pentaho.component;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class UsageLogDTO {


    private List<String> ips;
    private List<String> uris;
    private List<String> userIds;
    private String dataDateStart;
    private String dataDateEnd;


    public List<String> getIps() {
        return ips;
    }

    public void setIps(List<String> ips) {
        this.ips = ips;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public String getDataDateStart() {
        return dataDateStart;
    }

    public void setDataDateStart(String dataDateStart) {
        this.dataDateStart = dataDateStart;
    }

    public String getDataDateEnd() {
        return dataDateEnd;
    }

    public void setDataDateEnd(String dataDateEnd) {
        this.dataDateEnd = dataDateEnd;
    }


    @Override
    public String toString() {
        return "UsageLogDTO{" +
                "ips=" + ips +
                ", uris=" + uris +
                ", userIds=" + userIds +
                ", dataDateStart='" + dataDateStart + '\'' +
                ", dataDateEnd='" + dataDateEnd + '\'' +
                '}';
    }
}
