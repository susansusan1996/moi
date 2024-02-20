package com.example.pentaho.component;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 取 public key的名稱
 */
@Component
@ConfigurationProperties(prefix = "key")
public class KeyComponent {
    private String pubkeyName;

    private String apPubkeyName;




    public String getPubkeyName() {
        return pubkeyName;
    }

    public void setPubkeyName(String pubkeyName) {
        this.pubkeyName = pubkeyName;
    }

    public String getApPubkeyName() {
        return apPubkeyName;
    }

    public void setApPubkeyName(String apPubkeyName) {
        this.apPubkeyName = apPubkeyName;
    }
}
