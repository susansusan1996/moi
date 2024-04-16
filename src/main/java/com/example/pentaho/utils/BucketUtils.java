package com.example.pentaho.utils;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import javax.validation.constraints.NotNull;
import java.time.Duration;


public final class BucketUtils {

    /**
     * 定義令牌新增至儲存桶的固定速率
     * @param tokens 令牌數
     * @param period
     * @return Refill
     */
    private static Refill getRefill(long tokens, @NotNull Duration period){
     return Refill.greedy(tokens, period);
    }

    /**
     *
     * @param capacity 容量
     * @param period
     * @param tokens
     * @return
     */
    private static Bandwidth getLimit(@NotNull long capacity, @NotNull long tokens,@NotNull Duration period){
        return Bandwidth.classic(capacity,getRefill(tokens,period));
    }


    /**
     * 儲存桶
     * @param capacity
     * @param period
     * @return
     */
    public static Bucket getBucket(@NotNull long capacity,@NotNull long tokens,@NotNull Duration period){
       return Bucket.builder().addLimit(getLimit(capacity,tokens,period)).build();
    }
}
