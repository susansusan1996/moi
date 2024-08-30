package com.example.pentaho.cofig;


import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * 停用SSL驗證
 */
@Configuration
public class RestTemplateConfig {

    @Bean(name = "testtemplate")
    public RestTemplate restTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        // configure the SSLContext with a TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");

        TrustManager[] trustManagers = {new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};
        //
        sslContext.init(null,trustManagers,null);

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname,session)->true);
        RestTemplate restTemplate = new RestTemplateBuilder().requestFactory(SimpleClientHttpRequestFactory.class).build();
        return restTemplate;
    }
}

