package com.example.pentaho.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

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

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", userName='" + username + '\'' +
                ", departName='" + departName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
