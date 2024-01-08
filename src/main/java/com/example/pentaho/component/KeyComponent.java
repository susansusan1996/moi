package com.example.pentaho.component;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 取 public key的名稱
 */
@Component
@ConfigurationProperties(prefix = "key")
public class KeyComponent {
    private String keyname;

    public String getKeyname() {
        return keyname;
    }

    public void setKeyname(String keyname) {
        this.keyname = keyname;
    }
}
