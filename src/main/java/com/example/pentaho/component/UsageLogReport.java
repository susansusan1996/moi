package com.example.pentaho.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

@Component
public class UsageLogReport {


//    @JsonProperty("dateTime")
//    private Timestamp dateTime;

    @JsonProperty("dateTime")
    private String dateTime;

    @JsonProperty("query-single")
    private BigDecimal querySingle;

    @JsonProperty("query-standard-address")

    private BigDecimal queryStandardAddress;

    @JsonProperty("query-track")
    private BigDecimal queryTrack;



    public BigDecimal getQuerySingle() {
        return querySingle;
    }

    public void setQuerySingle(BigDecimal querySingle) {
        this.querySingle = querySingle;
    }

    public BigDecimal getQueryStandardAddress() {
        return queryStandardAddress;
    }

    public void setQueryStandardAddress(BigDecimal queryStandardAddress) {
        this.queryStandardAddress = queryStandardAddress;
    }

    public BigDecimal getQueryTrack() {
        return queryTrack;
    }

    public void setQueryTrack(BigDecimal queryTrack) {
        this.queryTrack = queryTrack;
    }


    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public String toString() {
        return "UsageLogReport{" +
                "dateTime='" + dateTime + '\'' +
                ", querySingle=" + querySingle +
                ", queryStandardAddress=" + queryStandardAddress +
                ", queryTrack=" + queryTrack +
                '}';
    }
}
