package com.example.pentaho.cofig;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@SecurityScheme(
        name = "Authorization",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SwaggerConfig {
    private final Logger log = LoggerFactory.getLogger(SwaggerConfig.class);

    @Bean
    public OpenAPI springShopOpenAPI(Environment env) throws UnknownHostException {
        String serverPort = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path");
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        String baseURL = "https://" + hostAddress + ":" + serverPort + contextPath;
        log.info("hostAddress:{} ,serverPort:{}, contextPath:{} ===> baseURL:{} ",hostAddress,serverPort,contextPath,baseURL);
        return new OpenAPI().addServersItem(new Server().url(baseURL));
    }




}

