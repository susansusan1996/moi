package com.example.pentaho.utils;

import com.example.pentaho.component.JobParams;
import com.example.pentaho.component.PentahoComponent;
import com.example.pentaho.component.User;
import com.example.pentaho.exception.MoiException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;


@Component
public class WebServiceUtils {


    private final static String ampersand = "&";

    private final static String separator = "?";

    private final static String equal ="=";

    private ObjectMapper objectMapper = new ObjectMapper();


    @Autowired
    private PentahoComponent pentahoComponent;

    private final static Logger log = LoggerFactory.getLogger(WebServiceUtils.class);





    public int getConnection(String webService,JobParams jobParams){
        try {
//            String fullUrl = getUrl(webService, json);
//            if("".equals(fullUrl)){
//                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_SERVER_ERROR");
//            }
            log.info("再確認一次要轉成url的job參數:{}",jobParams);
            StringBuilder temp = new StringBuilder(pentahoComponent.getWebTarget() + webService);
            String fullUrl = getFullUrl(temp,jobParams);
            log.info("fullUrl:{}",fullUrl);
            URL url = new URL(fullUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            basicAuthentication(con);
            con.setRequestMethod("POST");
            return con.getResponseCode();
        }catch (Exception e){
            log.info("e:{}",e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_SERVER_ERROR");
        }
    }



    /***
     *  添加 Basic Authentication header
     * @param connection
     */
    private void basicAuthentication(HttpURLConnection connection) {
        String username = pentahoComponent.getUserName(); //admin
        String password = pentahoComponent.getPassword(); //password
        String auth = username + ":" + password;
        byte[] authBytes = auth.getBytes(StandardCharsets.UTF_8);
        String encodedAuth = Base64.getEncoder().encodeToString(authBytes);
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
    }


    private String getFullUrl(StringBuilder url,JobParams jobParams){
        Method[] methods = JobParams.class.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("get") && !method.getName().equals("getClass")) {
                try {
                    Object value = method.invoke(jobParams);
                    if (value != null && !value.toString().isEmpty()) {
                        url.append(method.getName().substring(3)).append(equal).append(value).append(ampersand);
                    }
                } catch (Exception e) {
                    throw new MoiException("url解析錯誤 " + method.getName() + ": " + e.getMessage(), e);
                }
            }
        }
        // 移除最後一個 sperator
        String newUrl = url.toString();
        if (newUrl.endsWith(ampersand)) {
            newUrl = newUrl.substring(0, newUrl.length() - ampersand.length());
        }
        return newUrl;
    }

}