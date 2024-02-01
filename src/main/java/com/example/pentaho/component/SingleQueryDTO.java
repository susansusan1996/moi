package com.example.pentaho.component;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 單筆查詢用
 **/

@Component
public class SingleQueryDTO {

    private String redisKey;

    private String redisValue;

    private List<String> redisValueList;


    public String getRedisKey() {
        return redisKey;
    }

    public void setRedisKey(String redisKey) {
        this.redisKey = redisKey;
    }


    public String getRedisValue() {
        return redisValue;
    }

    public void setRedisValue(String redisValue) {
        this.redisValue = redisValue;
    }

    public List<String> getRedisValueList() {
        return redisValueList;
    }

    public void setRedisValueList(List<String> redisValueList) {
        this.redisValueList = redisValueList;
    }

    @Override
    public String toString() {
        return "SingleQueryDTO{" +
                "redisKey='" + redisKey + '\'' +
                ", redisValue='" + redisValue + '\'' +
                ", redisValueList=" + redisValueList +
                '}';
    }
}
