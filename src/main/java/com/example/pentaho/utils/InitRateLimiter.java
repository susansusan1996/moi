package com.example.pentaho.utils;

import com.example.pentaho.component.RateLimiting;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Map;

/***
 * 初始化RateLimiter
 */
@Component
public class InitRateLimiter implements ApplicationContextAware {

    private final static Logger log = LoggerFactory.getLogger(InitRateLimiter.class);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(RestController.class);
        beansWithAnnotation.forEach((ket,value)->
        {
            Class<?> resourceClass = value.getClass();
            Method[] methods = resourceClass.getSuperclass().getDeclaredMethods();
            for(Method method:methods){
                if(method.isAnnotationPresent(RateLimiting.class)){
                    String ratelimiterName = method.getAnnotation(RateLimiting.class).name();
                    double ratelimiterTokens = method.getAnnotation(RateLimiting.class).tokens();
                    /*每秒生成的token*/
                    RateLimiter rateLimiter = RateLimiter.create(ratelimiterTokens);
                    log.info("RateLimiter:"+ratelimiterName+" : 每秒生成/許可"+ratelimiterTokens+"次令牌");
                    /***
                     * 初始化RateLimiter放入切面
                     */
                    RateLimitAspect.rateLimiterMap.put(ratelimiterName,rateLimiter);
                }
            }
        });
    }
}
