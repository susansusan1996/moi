package com.example.pentaho.utils;

import com.example.pentaho.component.JobParams;
import com.example.pentaho.component.PentahoComponent;
import com.example.pentaho.component.PentahoWebService;
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
import org.w3c.dom.Element;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


@Component
public class WebServiceUtils {


    private final static String ampersand = "&";

    private final static String separator = "?";

    private final static String equal ="=";

    private ObjectMapper objectMapper = new ObjectMapper();


    @Autowired
    private PentahoComponent pentahoComponent;

    private final static Logger log = LoggerFactory.getLogger(WebServiceUtils.class);



    public Map<String,String> getConnection(String webService, JobParams jobParams, Map<String,String> result){
        HttpURLConnection con = null;
        String status ="CALL_JOB_ERROR";
        result.put("status",status);
        try {
            log.info("再確認一次要轉成url的job參數:{}",jobParams);
            StringBuilder temp = new StringBuilder(pentahoComponent.getWebTarget() + webService);
            String fullUrl = getFullUrl(temp,jobParams);
            log.info("fullUrl:{}",fullUrl);
            URL url = new URL(fullUrl);
            con = (HttpURLConnection) url.openConnection();
            basicAuthentication(con);
            con.setRequestMethod("POST");
            //todo:可能需要設定一個timeOut時間
            //con.setConnectTimeout(30);
            int responseCode = con.getResponseCode();
            log.info("responseCode:{}",responseCode);
            if(responseCode == 200){
                status="CALL_JOB_SUCESS";
                result.put("status",status);
                /*responseContent*/
//                XmlParseUtils.parser(con.getInputStream(),result);
            }
        }catch (Exception e){
            log.info("e:{}",e.toString());
        }finally {
            if(con!=null){
                con.disconnect();
            }
            log.info("result:{}",result);
        }
        jobParams.setStatus(status);
        return result;
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


    public Map<String,String> getJobStatus(Map<String,String> result){
        StringBuilder temp = new StringBuilder(pentahoComponent.getWebTarget());
        String fullURL =
                temp.append(PentahoWebService.jobStatusById)
                        .append("id" + equal + result.get("id"))
                        .toString();
        log.info("fullURL:{}",fullURL);
        try {
            URL url = new URL(fullURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            basicAuthentication(con);
//            XmlParseUtils.parser(con.getInputStream(),result);
            log.info("result:{}",result);
            return result;
        }catch (Exception e){
            log.info("e:{}",e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}