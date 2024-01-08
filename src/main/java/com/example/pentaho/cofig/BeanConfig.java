package com.example.pentaho.cofig;

import com.example.pentaho.utils.ResourceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class BeanConfig {

    @Bean
    public ResourceLoader resourceLoader() {
        return new DefaultResourceLoader();
    }

//    @Bean
//    public Resource resource(ResourceLoader resourceLoader) {
//        // 通过ResourceLoader加载资源，这里可以替换成你需要的具体资源路径
//        return resourceLoader.getResource("classpath:");
//    }

    @Bean
    public ResourceUtils resource() {
        return new ResourceUtils();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
