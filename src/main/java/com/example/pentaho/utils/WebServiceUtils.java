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
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Element;

import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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


    /**
     * call pentaho job
     * @param webService
     * @param jobParams
     * @param result
     * @return
     */

    public Map<String,String> getConnection(String webService, JobParams jobParams, Map<String,String> result){
        HttpURLConnection con = null;
        //todo:default 設失敗?
        String status ="CALL_JOB_ERROR";
        result.put("status",status);
        try {
            log.info("再確認一次要轉成url的參數:{}",jobParams);
            StringBuilder temp = new StringBuilder(pentahoComponent.getWebTarget() + webService);
            /*uri + params 組成完整url**/
            String fullUrl = getFullUrl(temp,jobParams);
            log.info("fullUrl:{}",fullUrl);
            URL url = new URL(fullUrl);
            con = (HttpURLConnection) url.openConnection();
            basicAuthentication(con);
            con.setRequestMethod("POST");
            //todo:可能需要設定一個timeOut時間
            //con.setConnectTimeout(30);
            int responseCode = con.getResponseCode();
            if(responseCode == 200){
                status="CALL_JOB_SUCESS";
                result.put("status",status);
                /*todo:目前程式會解析放入result*/
                //todo:成功呼叫解析後會是:result=OK, id=f44c9a72-4813-4b3a-a920-efc095838bcc, message=Job started, status=CALL_JOB_SUCESS
                XmlParseUtils.parser(con.getInputStream(),result);
            }
        }catch (Exception e){
            log.info("e:{}",e.toString());
        }finally {
            closeConnection(con);
        }
        /***/
        jobParams.setStatus(status);
        log.info("result:{}",result);
        return result;
    }


    /**
     * 關閉連線
     * @param con
     */
    private void closeConnection(HttpURLConnection con){
        if(con != null){
            con.disconnect();
        }
    }


    /***
     *  添加 Basic Authentication header
     * @param connection
     */
    private void basicAuthentication(HttpURLConnection connection) throws IOException {
        Path path = Path.of(pentahoComponent.getEncodeAuth());
        String encoded = Files.readString(path, StandardCharsets.UTF_8);
        connection.setRequestProperty("Authorization",encoded);
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


    public void getJobStatus(Map<String,String> result){
        StringBuilder temp = new StringBuilder(pentahoComponent.getWebTarget());
        String fullURL =
                String.format(temp.append(PentahoWebService.jobStatusById).toString(), result.get("id"));
//                        .append("id" + equal + result.get("id"))
//                        .toString();
        log.info("fullURL:{}",fullURL);
        HttpURLConnection con =null;
        try {
            URL url = new URL(fullURL);
            con = (HttpURLConnection) url.openConnection();
            basicAuthentication(con);
            XmlParseUtils.parser(con.getInputStream(),result);
//            log.info("result:{}",result);
        }catch (Exception e){
            log.info("e:{}",e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }finally {
            closeConnection(con);
        }
    }

}