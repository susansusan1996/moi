package com.example.pentaho.component;

public class BatchFormParams {
   private String id;
   private String originalFileId;
   private String processedCounts;
   private String status;
   private String file;


    public BatchFormParams(String id, String originalFileId, String processedCounts, String status, String file) {
        this.id = id;
        this.originalFileId = originalFileId;
        this.processedCounts = processedCounts;
        this.status = status;
        this.file = file;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalFileId() {
        return originalFileId;
    }

    public void setOriginalFileId(String originalFileId) {
        this.originalFileId = originalFileId;
    }

    public String getProcessedCounts() {
        return processedCounts;
    }

    public void setProcessedCounts(String processedCounts) {
        this.processedCounts = processedCounts;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
