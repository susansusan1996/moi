package com.example.pentaho.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Component
public class User  {

    @JsonProperty("token")
    private String token;

    @JsonProperty("refreshToken")
    private String refreshToken;

    @JsonProperty("remoteAddr")
    private  String remoteAddr;

    @JsonProperty("xrealIp")
    private String xrealIp;

    @JsonProperty("haveToChangePwd")
    private boolean haveToChangePwd;

    @JsonProperty("localizeName")
    private String localizeName;


    @JsonProperty("email")
    private String email;

    @JsonProperty("roles")
    private List<String> roles;

    @JsonProperty("tokenType")
    private String tokenType;

    /**使用者ID*/
    @JsonProperty("id")
    private String id;

    /**使用者名稱*/
    @JsonProperty("username")
    private String username;

    /**單位代號**/
    @JsonProperty("orgId")
    private String orgId;

    /**使用者單位名稱*/
    @JsonProperty("departName")
    private String departName;

    /**使用者密碼*/
    @JsonProperty("password")
    private String password;



    public User() {
    }

    public User(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(jsonString, User.class);
            this.remoteAddr = user.remoteAddr;
            this.xrealIp = user.xrealIp;
            this.haveToChangePwd = user.haveToChangePwd;
            this.localizeName = user.localizeName;
            this.password = user.password;
            this.roles = user.roles;
            this.email = user.email;
            this.id = user.id;
            this.username = user.username;
            this.orgId = user.orgId;
            this.departName = user.departName;
            this.tokenType = user.tokenType;
        } catch (JsonProcessingException e) {
        }
    }


    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public boolean isHaveToChangePwd() {
        return haveToChangePwd;
    }

    public void setHaveToChangePwd(boolean haveToChangePwd) {
        this.haveToChangePwd = haveToChangePwd;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getXrealIp() {
        return xrealIp;
    }

    public void setXrealIp(String xrealIp) {
        this.xrealIp = xrealIp;
    }

    public String getLocalizeName() {
        return localizeName;
    }

    public void setLocalizeName(String localizeName) {
        this.localizeName = localizeName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public String toString() {
        return "User{" +
                "token='" + token + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", remoteAddr='" + remoteAddr + '\'' +
                ", xrealIp='" + xrealIp + '\'' +
                ", haveToChangePwd=" + haveToChangePwd +
                ", localizeName='" + localizeName + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                ", tokenType='" + tokenType + '\'' +
                ", id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", orgId='" + orgId + '\'' +
                ", departName='" + departName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
