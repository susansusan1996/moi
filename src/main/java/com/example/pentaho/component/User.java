package com.example.pentaho.component;

import org.springframework.stereotype.Component;

@Component
public class User  {

    /**使用者ID*/
    private Long userId;

    /**使用者名稱*/
    private String userName;

    /**使用者單位名稱*/
    private String unitName;

    /**使用者密碼*/
    private String password;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public User() {
    }

    public User(Long userId) {
        this.userId = userId;
    }

    public User(Long userId, String unitName) {
        this.userId = userId;
        this.unitName = unitName;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", unitName='" + unitName + '\'' +
                '}';
    }
}
