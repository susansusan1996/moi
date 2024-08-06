package com.example.pentaho.cofig;


import com.example.pentaho.utils.AuthorizationHandlerInterceptor;
import com.example.pentaho.utils.MainHandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
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
    @Autowired
    private AuthorizationHandlerInterceptor authorizationHandlerInterceptor;

//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
//    }

//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // Ensure that the URL pattern correctly maps to the directory rather than a specific file
//        registry.addResourceHandler("/iisi/single-query/**")
//                .addResourceLocations("classpath:/static/")
//                // Enable or disable the resource chain (for performance reasons, consider setting to 'true' in production)
//                .resourceChain(true)
//                .addResolver(new PathResourceResolver() {
//                    @Override
//                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
//                        if (resourcePath.isEmpty() || "/".equals(resourcePath)) {
//                            // Redirect to index.html if the resourcePath is empty or '/'
//                            return location.createRelative("index.html");
//                        }
//                        return location.createRelative(resourcePath);
//                    }
//                });
//    }


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
    }

}
