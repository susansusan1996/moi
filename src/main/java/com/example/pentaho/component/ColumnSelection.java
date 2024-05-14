package com.example.pentaho.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.*;

public class ColumnSelection implements Serializable {

    /*表單Id*/

    @Schema(description = "表單Id", example = "2e91359e-c9d1-4abf-afd1-586f64c86528")
    @JsonProperty("formId")
    private String formId;

    /*表單名*/
    @Schema(description = "表單名", example = "BQ202405080001")
    @JsonProperty("formName")
    private String formName;

    /*建表的人*/
    @Schema(description = "申請者Id", example = "11930427-b12d-4c34-a49e-d85dab1f28f0")
    @JsonProperty("userId")
    private String userId;

    /*指定欄位*/
    @Schema(description = "指定欄位", example ="[\"addrId\",\"address\",\"county\",\"town\", \"village\", \"neighbor\", \"road\", \"area\", \"lane\", \"alley\", \"subAlley\", \"numType\", \"numFlr\", \"room\", \"x\", \"y\", \"z\", \"version\", \"townSn\", \"geohashCode\", \"roadId\", \"roadIdDt\", \"postCode\", \"postCodeDt\", \"source\", \"validity\", \"integrity\"]")
    @JsonProperty("showFields")
    private Set<String> showFields;


    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<String> getShowFields() {
        return showFields;
    }

    public void setShowFields(Set<String> showFields) {
        this.showFields = showFields;
    }
    
    @Override
    public String toString() {
        return "ColumnSelection{" +
                "formId='" + formId + '\'' +
                ", formName='" + formName + '\'' +
                ", userId='" + userId + '\'' +
                ", showFields=" + showFields +
                '}';
    }
}
