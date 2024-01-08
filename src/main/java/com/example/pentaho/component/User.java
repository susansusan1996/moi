package com.example.pentaho.component;

import org.springframework.stereotype.Component;

//@Entity
//@Table(name = "user")
@Component
public class User  {

    //    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "user_id")
    private Long userId;


    //    @Column(name = "user_name")
    private String userName;

    //    @Column(name = "password")
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

    public User() {
    }

    public User(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
