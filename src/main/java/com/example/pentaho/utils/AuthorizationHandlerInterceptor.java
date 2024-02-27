package com.example.pentaho.utils;

import com.example.pentaho.component.KeyComponent;
import com.example.pentaho.component.Token;
import com.example.pentaho.component.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class AuthorizationHandlerInterceptor implements HandlerInterceptor {

    private final static Logger log = LoggerFactory.getLogger(AuthorizationHandlerInterceptor.class);
    private final KeyComponent keyComponent;


    public AuthorizationHandlerInterceptor(KeyComponent keyComponent) {
        this.keyComponent = keyComponent;
    }


    /***
     *攔截
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String authHeader = request.getHeader("Authorization");
        log.info("request header = { Authorization:Bearer accessToken }:{}", authHeader);
        /**確認有無Authorization:Bearer 的 header**/
        if (authHeader == null || !authHeader.startsWith("Bearer")) {
            /**前端補403導回登入頁?**/
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
        }

            /**
             * 確認有無Authorization:Bearer RASJWTToken
             * 驗證使用者身分
             * 先直接給予JwtToken
             */
            if (authHeader.substring(7, authHeader.length()) == null && "".equals(authHeader.substring(7, authHeader.length()))) {
                /**前端補403導回登入頁?**/
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
            }

        String RSATokenJwt = authHeader.substring(7, authHeader.length());
        log.info("RASJWTToken:{}", RSATokenJwt);

        /**
         * 驗證RASJWTToken
         * 完成後return true 會再導向Spring-SecurityFilterChain
         */
        String keyName = keyComponent.getPubkeyName();
        log.info("requestURI:{}",request.getRequestURI());
        if("/api/batchForm/finished".equals(request.getRequestURI()) ||"/api/singlequery/query-single".equals(request.getRequestURI())){
          keyName =keyComponent.getApPubkeyName();
        }
            if(Token.fromRSAJWTToken(RSATokenJwt,keyName)){
                User user = Token.extractUserFromRSAJWTToken(RSATokenJwt,keyName);
                log.info("user:{}",user);
                UserContextUtils.setUserHolder(user);
                return true;
            }
        /**其他錯誤；前端補403導回登入頁?**/
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
    }


}


