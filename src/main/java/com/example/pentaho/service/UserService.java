package com.example.pentaho.service;

import com.example.pentaho.component.KeyComponent;
import com.example.pentaho.component.Login;
import com.example.pentaho.component.Token;
import com.example.pentaho.component.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService  {

    private final static Logger log = LoggerFactory.getLogger(UserService.class);

    private KeyComponent keyComponent;


    public UserService(KeyComponent keyComponent) {
        this.keyComponent = keyComponent;
    }

    /**
     * 驗證使用者身分
     * @param user
     * @return
     */
    public Login findUserByUserName() {
        User user = new User("{\n" +
                "  \"haveToChangePwd\": false,\n" +
                "  \"localizeName\": \"管理員姓名\",\n" +
                "  \"orgId\": \"ADMIN\",\n" +
                "  \"departName\": \"管理機關_單位UPDATE\",\n" +
                "  \"id\": \"673f7eec-8ae5-4e79-ad3a-42029eedf742\",\n" +
                "  \"username\": \"admin\",\n" +
                "  \"email\": \"admin@gmail.com\",\n" +
                "  \"roles\": [\n" +
                "    \"ROLE_IUSER\",\n" +
                "    \"ROLE_ADMIN\",\n" +
                "    \"ROLE_MODERATOR\"\n" +
                "  ],\n" +
                "  \"remoteAddr\": \"192.168.31.167\",\n" +
                "  \"xrealIp\": \"110.28.2.123\"\n" +
                "}");
        return Login.ofRSAJWTToken(user,keyComponent.getApPrikeyName());

    }


}

