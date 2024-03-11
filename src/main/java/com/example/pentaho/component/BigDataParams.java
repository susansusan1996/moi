package com.example.pentaho.component;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BigDataParams {


    @JsonProperty("id")
    private String id;

    @JsonProperty("recordCounts")
    private String recordCounts;


    @JsonProperty("fileUri")
    private String fileUri;



    public BigDataParams() {
    }

    public BigDataParams(String id) {
        this.id = id;
    }

    public BigDataParams(String id, String recordCounts, String fileUri) {
        this.id = id;
        this.recordCounts = recordCounts;
        this.fileUri = fileUri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecordCounts() {
        return recordCounts;
    }

    public void setRecordCounts(String recordCounts) {
        this.recordCounts = recordCounts;
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }
}
