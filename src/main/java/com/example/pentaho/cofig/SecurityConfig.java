package com.example.pentaho.cofig;

import com.example.pentaho.utils.AuthorizationHandlerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/***
 * 規範可以請求路徑
 * 解決CORS問題
 */
@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return
                http.authorizeRequests()
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/static/**").permitAll()
                        .and().cors(Customizer.withDefaults())
                        .csrf().disable().build();
    }


    /***
     * Customizer.withDefaults()的 CORS設定
     * @return
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        List<String> origins = new ArrayList<String>();
        List<String> methods = new ArrayList<String>();
        List<String> headers = new ArrayList<String>();
        /**AWS上有設定可以接收的port號；之後可以在這邊加上聖森的網域**/
//        origins.add("http://localhost:8080"); //打aws的瀏覽器
//        origins.add("http://localhost:8081"); //ETL
//        origins.add("http://localhost:8090"); //打本機的瀏覽器
//      corsConfiguration.setAllowedOrigins(origins);
        methods.add("GET");
        methods.add("POST");
        headers.add("Authorization");
        headers.add("Content-Type");
        corsConfiguration.setAllowedOrigins(Collections.singletonList("*"));
        corsConfiguration.setAllowedMethods(methods);
        corsConfiguration.setAllowedHeaders(headers);
//        corsConfiguration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        /**哪些請求路徑適用此CORS設定**/
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
