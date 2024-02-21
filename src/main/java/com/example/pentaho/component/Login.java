package com.example.pentaho.component;

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
     * 成功登入後會有RASJWTToken(acessToken 標頭；refreshToken cookie)
     * @param user
     * @return
     */
    public static Login ofRSAJWTToken(User user,String keyName) {
        return new Login(Token.ofRSAJWT(user,keyName), Token.ofRSAJWT(user,keyName));
    }

    @Override
    public String toString() {
        return "Login{" +
                "acessToken=" + acessToken +
                ", refreshToken=" + refreshToken +
                '}';
    }
}
