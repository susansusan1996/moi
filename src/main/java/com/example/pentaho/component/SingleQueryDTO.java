package com.example.pentaho.component;

import org.springframework.stereotype.Component;

/**
 * 單筆查詢用
 **/

@Component
public class SingleQueryDTO {

    private String redisKey;

    public String getRedisKey() {
        return redisKey;
    }

    public void setRedisKey(String redisKey) {
        this.redisKey = redisKey;
    }

    @Override
    public String toString() {
        return "SingleQueryDTO{" +
                "redisKey='" + redisKey + '\'' +
                '}';
    }
}
