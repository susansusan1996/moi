package com.example.pentaho.cofig;


import com.example.pentaho.utils.AuthorizationHandlerInterceptor;
import com.example.pentaho.utils.MainHandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/***
 * 攔截請求及處理方式
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {



    /**
     * token check
     */
    private AuthorizationHandlerInterceptor authorizationHandlerInterceptor;


    public WebMvcConfig(AuthorizationHandlerInterceptor authorizationHandlerInterceptor) {
        this.authorizationHandlerInterceptor = authorizationHandlerInterceptor;
    }

    /**
     * 過濾
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /**
         * 可再新增攔截器放進 interceptors
         * AuthorizationHandlerInterceptor 身分驗證
         */
        List<HandlerInterceptor> interceptors = Arrays.asList(authorizationHandlerInterceptor);
        MainHandlerInterceptor mainHandlerInterceptor = new MainHandlerInterceptor(interceptors);
        registry
                .addInterceptor(mainHandlerInterceptor);
//                .addPathPatterns("/api/**");
//        registry
//                .addInterceptor(authorizationHandlerInterceptor)
//                .addPathPatterns(
//                        "/api/kettle/**",
//                        "/api/batchForm/**",
//                        "/api/redis/**",
//                        "/api/singlequery/**",
//                        "/api/single-track-query/**",
//                        "/api/bigdata/**",
//                        "/api/api-key/get-api-key",
//                        "/api/api-key/create-api-key",
//                        "/api/api-key/forapikey",
//                        "/api/api-key/forguest"
//                        );

    }

}
