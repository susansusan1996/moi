package com.example.pentaho.utils;

import com.example.pentaho.component.RateLimiter;
import com.example.pentaho.component.RateLimiterKey;
import io.github.bucket4j.Bucket;
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

import java.util.List;
import java.util.Optional;

@Component
public class RateLimitingHandlerInterceptor implements HandlerInterceptor  {

    private Logger log = LoggerFactory.getLogger(RateLimitingHandlerInterceptor.class);

    @Autowired
    private RateLimiterUtils rateLimiterUtils;



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        log.info("in");
        /**annotation ean**/
        Optional<List<RateLimiter>> rateLimiters = rateLimiterUtils.getRateLimiters(handlerMethod);
        if(rateLimiters.isPresent()){
//           RateLimiterKey rateLimiterKey = new RateLimiterKey(request.getRemoteAddr(), new String[]{request.getRequestURI()});
            List<Bucket> buckets = rateLimiterUtils.getRateLimitersAnnotationToBucket(rateLimiters.get());
            if(!buckets.isEmpty()){
            for(Bucket bucket:buckets){
           if (!bucket.tryConsume(1)){
               throw new ResponseStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
           }
                }
            }
        }
         return true;
    }


    //    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        log.info("Rate Limiting Check");
//
//        HandlerMethod handlerMethod = (HandlerMethod) handler;
//        Class<?> clazz = handlerMethod.getBeanType();
//        Method method = handlerMethod.getMethod();
//
//        if(clazz.isAnnotationPresent(RateLimiting.class) || method.isAnnotationPresent(RateLimiting.class)){
//            RateLimiting annotation = method.getAnnotation(RateLimiting.class);
//            log.info("容量限制:{}",annotation.capacity());
//            log.info("每"+ annotation.mintues() +"分鐘，回填" + annotation.tokens() +"個令牌");
//            Bucket bucket = BucketUtils.getBucket(annotation.capacity(),annotation.tokens(),Duration.ofMinutes(annotation.mintues()));
//            /**用 IP 來限制 **/
//            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
//            assertTrue(bucket.tryConsume(1));
//            log.info("第"+ times +"次請求");
//            if(!bucket.tryConsume(1)){
//                throw new ResponseStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
//            }
//        }
//
//        log.info("Rate Limiting Check OK");
//        return true;
//    }
//
//    void assertTrue(boolean tryConsume){
//        if(tryConsume){
//            times+=1;
//        }
//    }

}
