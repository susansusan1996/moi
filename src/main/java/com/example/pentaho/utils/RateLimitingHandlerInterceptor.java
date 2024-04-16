package com.example.pentaho.utils;

import com.example.pentaho.component.RateLimiting;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.time.Duration;

@Component
public class RateLimitingHandlerInterceptor implements HandlerInterceptor  {

    private Logger log = LoggerFactory.getLogger(RateLimitingHandlerInterceptor.class);


    private int times;


        @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("Rate Limiting Check");

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Class<?> clazz = handlerMethod.getBeanType();
        Method method = handlerMethod.getMethod();

        if(clazz.isAnnotationPresent(RateLimiting.class) || method.isAnnotationPresent(RateLimiting.class)){
            RateLimiting annotation = method.getAnnotation(RateLimiting.class);
            log.info("容量限制:{}",annotation.capacity());
            log.info("每"+ annotation.mintues() +"分鐘，回填" + annotation.tokens() +"個令牌");
            Bucket bucket = BucketUtils.getBucket(annotation.capacity(),annotation.tokens(), Duration.ofMinutes(annotation.mintues()));
            /**用 IP 來限制 **/
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            assertTrue(bucket.tryConsume(1));
            log.info("第"+ times +"次請求");
            if(!bucket.tryConsume(1)){
                throw new ResponseStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
            }
        }

        log.info("Rate Limiting Check OK");
        return true;
    }

    void assertTrue(boolean tryConsume){
        if(tryConsume){
            times+=1;
        }
    }

}
