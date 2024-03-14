package com.example.pentaho.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import javax.validation.constraints.NotNull;
import java.time.Duration;


public final class BucketUtils {

    /**
     * 令牌數
     * @param tokens
     * @param period
     * @return
     */
    private static Refill getRefill(long tokens, @NotNull Duration period){
     return Refill.greedy(tokens, period);
    }

    /**
     *
     * @param capacity
     * @param period
     * @return
     */
    private static Bandwidth getLimit(long capacity,@NotNull Duration period){
        return Bandwidth.classic(capacity,getRefill(capacity,period));
    }


    /**
     * 儲存桶
     * @param capacity
     * @param period
     * @return
     */
    public static Bucket getBucket(long capacity,@NotNull Duration period){
       return Bucket.builder().addLimit(getLimit(capacity,period)).build();
    }
}
