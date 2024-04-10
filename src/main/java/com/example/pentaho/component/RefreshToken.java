package com.example.pentaho.component;

import java.time.Instant;

public class RefreshToken {

    private String id;
    private String refreshToken;
    private String token;
    private Instant expiryDate;
    private Instant refreshTokenExpiryDate;
    private String reviewResult; //審核結果(AGREE、REJECT)
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


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getRefreshTokenExpiryDate() {
        return refreshTokenExpiryDate;
    }

    public void setRefreshTokenExpiryDate(Instant refreshTokenExpiryDate) {
        this.refreshTokenExpiryDate = refreshTokenExpiryDate;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(String reviewResult) {
        this.reviewResult = reviewResult;
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id='" + id + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", token='" + token + '\'' +
                ", expiryDate=" + expiryDate +
                ", refreshTokenExpiryDate=" + refreshTokenExpiryDate +
                ", reviewResult='" + reviewResult + '\'' +
                ", user=" + user +
                '}';
    }
}
