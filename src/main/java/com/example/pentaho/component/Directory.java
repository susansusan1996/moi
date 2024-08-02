package com.example.pentaho.component;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "directory")
public class Directory {

    private String target;

    private String receiveFileDir;

    private String sendFileDir;

    private String localTempDir;

    private String bigDataReceiveFileDir;

    private String bigDataSendFileDir;


    private String etlOutputFileDirPrefix; //etl作業產出的檔案位置前綴

    private String mockEtlSaveFileDirPrefix; //模擬聖森存檔案

    private String ktrFilePath; // etl .ktr檔的存放位置

    private String qrcodeUrl;

    private String qrcodePath;  //

    private String logoPath;

    public String getQrcodeUrl() {
        return qrcodeUrl;
    }

    public void setQrcodeUrl(String qrcodeUrl) {
        this.qrcodeUrl = qrcodeUrl;
    }

    public String getQrcodePath() {
        return qrcodePath;
    }

    public void setQrcodePath(String qrcodePath) {
        this.qrcodePath = qrcodePath;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

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

    public String getBigDataReceiveFileDir() {
        return bigDataReceiveFileDir;
    }

    public void setBigDataReceiveFileDir(String bigDataReceiveFileDir) {
        this.bigDataReceiveFileDir = bigDataReceiveFileDir;
    }

    public String getBigDataSendFileDir() {
        return bigDataSendFileDir;
    }

    public void setBigDataSendFileDir(String bigDataSendFileDir) {
        this.bigDataSendFileDir = bigDataSendFileDir;
    }

    public String getLocalTempDir() {
        return localTempDir;
    }

    public void setLocalTempDir(String localTempDir) {
        this.localTempDir = localTempDir;
    }

    @Override
    public String toString() {
        return "Directory{" +
                "target='" + target + '\'' +
                ", receiveFileDir='" + receiveFileDir + '\'' +
                ", sendFileDir='" + sendFileDir + '\'' +
                ", localTempDir='" + localTempDir + '\'' +
                ", bigDataReceiveFileDir='" + bigDataReceiveFileDir + '\'' +
                ", bigDataSendFileDir='" + bigDataSendFileDir + '\'' +
                ", etlOutputFileDirPrefix='" + etlOutputFileDirPrefix + '\'' +
                ", mockEtlSaveFileDirPrefix='" + mockEtlSaveFileDirPrefix + '\'' +
                ", ktrFilePath='" + ktrFilePath + '\'' +
                '}';
    }
}
