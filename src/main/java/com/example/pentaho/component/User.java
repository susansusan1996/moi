package com.example.pentaho.component;

import org.springframework.stereotype.Component;

@Component
public class User  {


//      "haveToChangePwd": false,
//      "localizeName": "管理員姓名",
//      "departName": "管理機關_管理部門",
//      "id": "673f7eec-8ae5-4e79-ad3a-42029eedf742",
//      "username": "admin",
//      "email": "admin@gmail.com",
//      "roles": [
//      "ROLE_ADMIN",
//      "ROLE_IUSER",
//      "ROLE_MODERATOR"

    /**使用者ID*/
    private String id;

    /**使用者名稱*/
    private String userName;

    /**使用者單位名稱*/
    private String departName;

    /**使用者密碼*/
    private String password;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public User() {
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", userName='" + userName + '\'' +
                ", departName='" + departName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
