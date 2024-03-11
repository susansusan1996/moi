package com.example.pentaho.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class HeaderUtils {

    private static final Logger log = LoggerFactory.getLogger(HeaderUtils.class);

    private static final String HEADER_KEY = "x-sts-alert";

    private static final String CREATE_MESSAGE = "新增成功";

    private static final String SAVE_MESSAGE = "儲存成功";

    private static final String REMOVE_MESSAGE = "刪除成功";

    private static final String EX_MESSAGE = "執行成功";

    private static final String NO_FILES = "無檔案，請確認條件是否正確";

    public static HttpHeaders createAlert(String message) {
        HttpHeaders headers = new HttpHeaders();

        try {
            headers.add(HEADER_KEY, URLEncoder.encode(message, StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException exception) {
            log.error("Error occurs when encode alert message.", exception);
        }


        return headers;
    }

    public static HttpHeaders createEntityCreationAlert() {
        return createAlert(CREATE_MESSAGE);
    }

    public static HttpHeaders createEntityUpdateAlert() {
        return createAlert(SAVE_MESSAGE);
    }

    public static HttpHeaders createEntityDeletionAlert() {
        return createAlert(REMOVE_MESSAGE);
    }

    public static HttpHeaders createExecutionAlert() {
        return createAlert(EX_MESSAGE);
    }

    public static HttpHeaders queryFilesAlert() {
        return createAlert(NO_FILES);
    }
}
