package com.example.pentaho.component;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UsageLog {


    @Schema(description = "id",implementation = BigDecimal.class)
    private BigDecimal id;

    @Schema(description = "使用者ip",implementation = String.class)
    private String ip;
    @Schema(description = "使用者Id",implementation = String.class)
    private String userId;

    @Schema(description = "uri",implementation = String.class)
    private String uri;

    @Schema(description = "參數",implementation = String.class)
    private String params;

    @Schema(description = "使用時間",implementation = Timestamp.class)
    private Timestamp dateTimeTrace; //yyyy-MM-dd HH:mm:ss

    public BigDecimal getId() {
        return id;
    }

    public void setId(BigDecimal id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Timestamp getDateTimeTrace() {
        return dateTimeTrace;
    }

    public void setDateTimeTrace(Timestamp dateTimeTrace) {
        this.dateTimeTrace = dateTimeTrace;
    }

    @Override
    public String toString() {
        return "UsageLog{" +
                "id=" + id +
                ", ip='" + ip + '\'' +
                ", userId='" + userId + '\'' +
                ", uri='" + uri + '\'' +
                ", params='" + params + '\'' +
                ", dateTimeTrace=" + dateTimeTrace +
                '}';
    }
}
