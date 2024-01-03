package com.example.pentaho.model;

import org.springframework.stereotype.Component;

@Component
public class Login {

    /**
     * in reponse
     * for request header
     * to remain login status
     */
    private Token acessToken;

    /**
     * in reponse cookie
     * for cookie
     * to get acessToken if it's missing
     */
    private Token refreshToken;

    public Login() {
    }


    public Login(Token acessToken, Token refreshToken) {
        this.acessToken = acessToken;
        this.refreshToken = refreshToken;
    }

    public Token getAcessToken() {
        return acessToken;
    }

    public Token getRefreshToken() {
        return refreshToken;
    }

    /***
     * 成功登入後會有acessToken refreshToken
     * @param userId
     * @param ascessPrivateKey
     * @param refreshPrivateKey
     * @return
     */
    public static Login of(Long userId, String ascessPrivateKey, String refreshPrivateKey) {
        return new Login(Token.of(userId, 1L, ascessPrivateKey), Token.of(userId, 10L, refreshPrivateKey));
    }


}
