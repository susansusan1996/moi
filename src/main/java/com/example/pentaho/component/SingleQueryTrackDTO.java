package com.example.pentaho.component;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class SingleQueryTrackDTO {


    /***
     *
     * 該筆地址無異動軌跡
     * 地址識別碼不合法
     */
    @Schema(description = "messge",example = "")
    private String text ;


    @Schema(description = "data",example = "")
    private List<IbdTbIhChangeDoorplateHis> data;


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<IbdTbIhChangeDoorplateHis> getData() {
        return data;
    }

    public void setData(List<IbdTbIhChangeDoorplateHis> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "SingleQueryTrackDTO{" +
                "text='" + text + '\'' +
                ", data=" + data +
                '}';
    }
}
