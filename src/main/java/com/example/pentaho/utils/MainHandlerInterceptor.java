package com.example.pentaho.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Configuration
public class MainHandlerInterceptor implements HandlerInterceptor  {

    private Logger log = LoggerFactory.getLogger(MainHandlerInterceptor.class);

    private final List<HandlerInterceptor> interceptors;


    public MainHandlerInterceptor(List<HandlerInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
       log.info("uri:{}",request.getRequestURI());

        if(!(handler instanceof HandlerMethod)){
            /**請求靜態資源*/
            return true;
        }

        /**
         * false 直接 return
         * true 往下個 interceptor**/
       for(HandlerInterceptor interceptor:interceptors){
           if(!interceptor.preHandle(request,response,handler)){
             return false;
           }
       }
            return true;
    }
}
