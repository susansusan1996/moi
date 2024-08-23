package com.example.pentaho.component;

import com.fasterxml.jackson.annotation.JsonProperty;


public class OpenPageDTO {


    private String origrinalAddress;

    private String fullAddress;

    private String joinStep;

    private String addressId;

    private String location;

    public static class QrcodeDTO{
//        public QrcodeDTO() {
//        }

        @JsonProperty("seq")
        private String seq;

        @JsonProperty("originalAddress")
        private String originalAddress;

        @JsonProperty("joinStep")
        private String joinStep;

        public String getSeq() {
            return seq;
        }

        public void setSeq(String seq) {
            this.seq = seq;
        }

        public String getOriginalAddress() {
            return originalAddress;
        }

        public void setOriginalAddress(String originalAddress) {
            this.originalAddress = originalAddress;
        }

        public String getJoinStep() {
            return joinStep;
        }

        public void setJoinStep(String joinStep) {
            this.joinStep = joinStep;
        }
    }


    public String getOrigrinalAddress() {
        return origrinalAddress;
    }

    public void setOrigrinalAddress(String origrinalAddress) {
        this.origrinalAddress = origrinalAddress;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public String getJoinStep() {
        return joinStep;
    }

    public void setJoinStep(String joinStep) {
        this.joinStep = joinStep;
    }

    public String getAddressId() {
        return addressId;
    }

    public void setAddressId(String addressId) {
        this.addressId = addressId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
