package com.example.pentaho.model;

import java.util.Map;

public class Action {

    private String url;

    private Map<String, Map<String, String>> para;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, Map<String, String>> getPara() {
        return para;
    }

    public void setPara(Map<String, Map<String, String>> para) {
        this.para = para;
    }
}
