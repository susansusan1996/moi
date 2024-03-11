package com.example.pentaho.component;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pentaho")
public class PentahoComponent {

    private String webTarget;

    private String userName;

    private String password;

    public String getWebTarget() {
        return webTarget;
    }

    public void setWebTarget(String webTarget) {
        this.webTarget = webTarget;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
