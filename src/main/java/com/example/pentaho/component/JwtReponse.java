package com.example.pentaho.component;

public class JwtReponse {
    private String token;
    private String refreshToken;
    private String expiryDate; //token的過期日(不是refreshToken的過期日)
    private String errorResponse;

    public JwtReponse() {
    }

    public JwtReponse(String errorResponse) {
        this.errorResponse = errorResponse;
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

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getErrorResponse() {
        return errorResponse;
    }

    public void setErrorResponse(String errorResponse) {
        this.errorResponse = errorResponse;
    }

    @Override
    public String toString() {
        return "JwtReponse{" +
                "token='" + token + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", errorResponse='" + errorResponse + '\'' +
                '}';
    }
}
