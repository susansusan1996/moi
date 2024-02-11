package com.example.pentaho.component;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "directory")
public class Directory {

    private String target;

    private String receiveFileDir;

    private String sendFileDir;

    private String etlOutputFileDirPrefix; //etl作業產出的檔案位置前綴

    private String mockEtlSaveFileDirPrefix; //模擬聖森存檔案

    private String ktrFilePath; // etl .ktr檔的存放位置

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getReceiveFileDir() {
        return receiveFileDir;
    }

    public void setReceiveFileDir(String receiveFileDir) {
        this.receiveFileDir = receiveFileDir;
    }

    public String getSendFileDir() {
        return sendFileDir;
    }

    public void setSendFileDir(String sendFileDir) {
        this.sendFileDir = sendFileDir;
    }

    public String getEtlOutputFileDirPrefix() {
        return etlOutputFileDirPrefix;
    }

    public void setEtlOutputFileDirPrefix(String etlOutputFileDirPrefix) {
        this.etlOutputFileDirPrefix = etlOutputFileDirPrefix;
    }

    public String getMockEtlSaveFileDirPrefix() {
        return mockEtlSaveFileDirPrefix;
    }

    public void setMockEtlSaveFileDirPrefix(String mockEtlSaveFileDirPrefix) {
        this.mockEtlSaveFileDirPrefix = mockEtlSaveFileDirPrefix;
    }

    public String getKtrFilePath() {
        return ktrFilePath;
    }

    public void setKtrFilePath(String ktrFilePath) {
        this.ktrFilePath = ktrFilePath;
    }

    @Override
    public String toString() {
        return "Directory{" +
                "target='" + target + '\'' +
                ", receiveFileDir='" + receiveFileDir + '\'' +
                ", sendFileDir='" + sendFileDir + '\'' +
                ", etlOutputFileDirPrefix='" + etlOutputFileDirPrefix + '\'' +
                ", mockEtlSaveFileDirPrefix='" + mockEtlSaveFileDirPrefix + '\'' +
                ", ktrFilePath='" + ktrFilePath + '\'' +
                '}';
    }
}
