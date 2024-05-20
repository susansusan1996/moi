package com.example.pentaho.utils;

import com.example.pentaho.component.RateLimiting;
import com.google.common.util.concurrent.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Scope
@Aspect
public class RateLimitAspect {


    private final static Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /*
    * 儲存 RateLimter 實例 Map
    * 不能支援多執行續
    * 必須是靜態
    * */
    public static Map<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();




    /***
     * 切點
     */
    @Pointcut("@annotation(com.example.pentaho.component.RateLimiting)")
    public void ServiceAspect(){


    }

    @Around("ServiceAspect()")
    public Object ArroundSeviceAspect(ProceedingJoinPoint joinPoint){
        Object obj = null;
        try{
            log.info("joinPoint:{}",joinPoint);
            Class<?> targetClass = joinPoint.getTarget().getClass();
            log.info("目標類別:{}",targetClass);
            Signature signature = joinPoint.getSignature();
            String signatureName = signature.getName();
            log.info("目標signatureName:{}",signatureName);
            String rateLimiterName = getRateLimiterName(targetClass, signatureName);
            RateLimiter rateLimiter = rateLimiterMap.get(rateLimiterName);
            if(!rateLimiter.tryAcquire()){
                return new ResponseStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
            }
                obj = joinPoint.proceed();

        } catch (Exception e){
            //todo:
            throw new RuntimeException(e);

        } catch (Throwable e) {
            //todo:
            log.info("e:{}",e);
        }
            return obj;

    }

    private String getRateLimiterName(Class<?> targetClass,String methodName){
        try {
            Method[] methods = targetClass.getDeclaredMethods();
            for(Method method:methods){
                if(method.getName().equals(methodName)){
                    if(method.isAnnotationPresent(RateLimiting.class)){
                        String rateLimiterName = method.getAnnotation(RateLimiting.class).name();
                        if(StringUtils.isNullOrEmpty(rateLimiterName)){
                            return method.getName();
                        }
                            return rateLimiterName;
                    }
                }
            }
        }catch (Exception e){
            log.info("e:{}",e);
        }
        return null;
    }
}
