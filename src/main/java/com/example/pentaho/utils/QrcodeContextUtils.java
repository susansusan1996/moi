package com.example.pentaho.utils;

import com.example.pentaho.component.OpenPageDTO;

public class QrcodeContextUtils {
    private static final ThreadLocal< OpenPageDTO.QrcodeDTO > QrCodeData = new ThreadLocal<>();

    public static void setQrcodeData( OpenPageDTO.QrcodeDTO  data) {
        QrCodeData.set(data);
    }

    public static  OpenPageDTO.QrcodeDTO  getQrcodeData() {
        return QrCodeData.get();
    }

    public static void clear() {
        QrCodeData.remove();
    }
}
