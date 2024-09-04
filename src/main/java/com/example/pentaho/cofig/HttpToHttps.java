package com.example.pentaho.cofig;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/***
 * 新增 tomcat 容器監聽端口
 */
@Configuration
@Profile("uat")
public class HttpToHttps {

    @Bean
    public Connector connector() {
       Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        //connector 通信協議
        connector.setScheme("https");
        //connector 監聽開放http的端口
        connector.setPort(8081);
        //從這個 connector 可以不使用SSL/TLS 加密通信
        connector.setSecure(true);
        //把請求進 connector 重定向到 SSL端口
        connector.setRedirectPort(8082);

//        這個 connector 使用SSL/TLS 加密通信。
//        connector.setSecure(true);
//        這個 connector 讀取的密鑰庫位置
//        connector.setProperty("keystoreFile", "/path/to/keystore.jks");
//        密鑰庫的密码，訪問密鑰中的密鑰對。
//        connector.setProperty("keystorePass", "keystorePassword");
//        密鑰庫中密鑰的别名。可以指定要使用的證書
//        connector.setProperty("keyAlias", "keyAlias");
//        指定密鑰的密碼。這個密鑰會可以用來解密儲存在密鑰庫中的私鑰
//        connector.setProperty("keyPass", "keyPassword");
//        配置信任儲存(驗證客戶端)
//        信任庫文件位置。信任庫儲存受信任的證書鏈，可以驗證客户端證書。
//        connector.setProperty("truststoreFile", "/path/to/truststore.jks");
//        信任庫的密碼
//        connector.setProperty("truststorePass", "truststorePassword");
//        是否需要客戶端認證。true ==> 會要求客户端提供證書，即 雙向 SSL/TLS 認證。
//        connector.setProperty("clientAuth", "true");
        return connector;
    }


    /**
     * 定義新增的tomcat容器
     * @param connector
     * @return
     */
    @Bean
    public TomcatServletWebServerFactory tomcatServletWebServerFactory(Connector connector) {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                //通過 HTTP 訪問時會拒绝，返回 HTTP 403 Forbidden，也可以設置重定向到 HTTPS (443) 的 URL
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                //匹配請求路徑
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(connector);
        return tomcat;
    }
}
