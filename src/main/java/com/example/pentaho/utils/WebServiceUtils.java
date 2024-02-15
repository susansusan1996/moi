package com.example.pentaho.utils;

import com.example.pentaho.component.PentahoComponent;
import com.example.pentaho.component.User;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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





    public int getConnection(String webService, String json){
        try {
            String fullUrl = getUrl(webService, json);
            if("".equals(fullUrl)){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_SERVER_ERROR");
            }

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


    private void appendUserParams(StringBuilder url){
        User user = UserContextUtils.getUserHolder();
        url.append("USER_ID"+equal+user.getUserId()+ampersand+
                "UNITNAME"+equal+user.getUnitName());
    }

    public String getUrl(String webService, String json) {
        try {
            log.info("執行作業:{}", webService);
            log.info("json:{}",json);
            StringBuilder url = new StringBuilder(pentahoComponent.getWebTarget() + webService);
            TreeNode treeNode = objectMapper.readTree(json);
            Iterator<String> keys = treeNode.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                log.info("key:{}",key);
                url.append(String.valueOf(key).replaceAll("\"",""));
                url.append(equal);
                url.append(String.valueOf(treeNode.get(key)).replaceAll("\"",""));
                url.append(ampersand);
            }
            appendUserParams(url);
            url.append(ampersand);
            url.append("level=Debug");
            log.info("url:{}", url);
            return url.toString();
        }catch (Exception e){
            log.info("e:{}",e);
            return "";
        }
    }

}