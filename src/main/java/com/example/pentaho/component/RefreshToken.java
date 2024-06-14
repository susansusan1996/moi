package com.example.pentaho.component;

public class RefreshToken {

    private String id;
    private String username;
    private String refreshToken;
    private String token;
    private String expiryDate;
    private String refreshTokenExpiryDate;
    private String reviewResult; //審核結果(AGREE、REJECT)
    private User user;

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

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getRefreshTokenExpiryDate() {
        return refreshTokenExpiryDate;
    }

    public void setRefreshTokenExpiryDate(String refreshTokenExpiryDate) {
        this.refreshTokenExpiryDate = refreshTokenExpiryDate;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(String reviewResult) {
        this.reviewResult = reviewResult;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
