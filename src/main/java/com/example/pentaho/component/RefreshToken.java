package com.example.pentaho.component;

import java.time.Instant;

public class RefreshToken {

    private String id;
    private String refreshToken;
    private Instant expiryDate;
    private User user;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public Instant getExpiryDate() {
        return expiryDate;
    }

    public User getUser() {
        return user;
    }


    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
