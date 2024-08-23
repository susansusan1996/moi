package com.example.pentaho.utils;

import com.example.pentaho.component.RateLimiting;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Scope
@Aspect
public class RateLimitAspect {


    private final static Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /*
    * 儲存 RateLimter 的 Map
    * 不能支援多執行續
    * 必須是靜態
    * */
    public static Map<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();




    /***
     * .RateLimiting Annotation切點
     */
    @Pointcut("@annotation(com.example.pentaho.component.RateLimiting)")
    public void ServiceAspect(){


    }

    /**
     * 以IP為單位
     * @param joinPoint
     * @return
     */
    @Around("ServiceAspect()")
    public Object aroundServiceAspect(ProceedingJoinPoint joinPoint) {
        Object obj = null;
        try {
            /**獲取客戶端IP*/
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String clientIp = request.getRemoteAddr();

            /**獲取目標類(接口類別)和方法(接口方法)**/
            Class<?> targetClass = joinPoint.getTarget().getClass();
            /**獲取方法簽名轉換為 MethodSignature (才可以拿到準確的Method實例)**/
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();


            log.info("街口方法名稱:{}",method.getName());
            log.info("接口類別:{},接口方法:{},客戶端IP:{}",targetClass.getName(),method,clientIp);

            String name = method.getAnnotation(RateLimiting.class).name();
            double tokens = method.getAnnotation(RateLimiting.class).tokens();

            /**RateLimiting = com.example.pentaho.resource.SingleQueryResouce:forGuestUser:0:0:0:0:0:0:0:1 **/
            String rateLimiterName = name + ":" + clientIp;

            /***檢查 RateLimiterMap 是否已經有這個IP的 RateLimiter 實例，沒有的話創建，有的話就tryAcquire**/
            RateLimiter rateLimiter = rateLimiterMap.computeIfAbsent(rateLimiterName, key -> RateLimiter.create(tokens)); // 每秒5个许可
            if (!rateLimiter.tryAcquire()) {
                return new ResponseEntity<>("操作頻繁，請稍後再試", HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
            }

            /**繼續執行接口的方法***/
            obj = joinPoint.proceed();
        } catch (Throwable e) {
            log.error("Exception occurred: ", e);
            throw new RuntimeException(e);
        }
        return obj;
    }

    private String getRateLimiterName(Class<?> targetClass, String signatureName, String clientIp) {
        return targetClass.getName() + ":" + signatureName + ":" + clientIp;
    }

    /***
     * 不以IP計算，以所有請求為基準的方式。
     * 在 app 啟動時就生成RateLmiter
     * 需要搭配 InitRateLimiter.class (目前以IP計算，所以這個類別先關了~)
     * @param targetClass
     * @param methodName
     * @return
     */
//    @Around("ServiceAspect()")
//    public Object ArroundSeviceAspect(ProceedingJoinPoint joinPoint){
//        Object obj = null;
//        try{
//            log.info("joinPoint:{}",joinPoint);
//            Class<?> targetClass = joinPoint.getTarget().getClass();
//            log.info("目標類別:{}",targetClass);
//            Signature signature = joinPoint.getSignature();
//            String signatureName = signature.getName();
//            log.info("目標signatureName:{}",signatureName);
//            String rateLimiterName = getRateLimiterName(targetClass, signatureName);
//            RateLimiter rateLimiter = rateLimiterMap.get(rateLimiterName);
//            if(!rateLimiter.tryAcquire()){
//                return new ResponseEntity<String>("頻繁執行，請稍後再試",HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
//            }
//                obj = joinPoint.proceed();
//
//        } catch (Exception e){
//            //todo:
//            throw new RuntimeException(e);
//
//        } catch (Throwable e) {
//            //todo:
//            log.info("e:{}",e);
//        }
//            return obj;
//
//    }

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
