package com.example.pentaho.cofig;


import com.example.pentaho.utils.AuthorizationHandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/***
 * 攔截請求及處理方式
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {


    /**
     * for all roles
     */
    @Autowired
    private AuthorizationHandlerInterceptor authorizationHandlerInterceptor;


    /**
     * 過濾
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(authorizationHandlerInterceptor)
                .addPathPatterns(
                        "/api/kettle/**",
                        "/api/batchForm/**",
                        "/api/redis/**",
                        "/api/singlequery/**",
                        "/api/single-track-query/**",
                        "/api/bigdata/**",
                        "/api/api-key/get-api-key",
                        "/api/api-key/create-api-key",
                        "/api/api-key/forapikey",
                        "/api/api-key/forguest"
                        );
        /**
         * 可以新增別的路徑的攔截
         * AdminAuthorizationHandlerInterceptor
         */
    }

}
