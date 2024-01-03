package com.example.pentaho.utils;

import com.example.pentaho.filter.UserFilter;
import com.example.pentaho.model.Token;
import jakarta.servlet.http.Cookie;
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
        log.info("request:{}", request);
        String authHeader = request.getHeader("Authorization");
        log.info("request header = { Authorization:Bearer accessToken }:{}", authHeader);
        /**確認有無Authorization:Bearer 的 header**/
        if (authHeader == null || !authHeader.startsWith("Bearer")) {


            Cookie[] cookies = request.getCookies();
            /**完全沒有或沒有refresh_token (cookie)**/
            if (cookies == null) {
                /**前端補充導回login的程式**/
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "not allowed");
            }

            Cookie cookie = null;
            for (Cookie c : cookies) {
                if ("refresh_token".equals(c.getName())) {
                    cookie = c;
                }
            }

            if (cookie == null) {
                /**前端補充導回login的程式**/
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "not allowed");
            }
            /**有name為refresh_token的cookie,需要指定客戶端接受什麼訊息後，會讓他自動向 /refresh 發起請求，以更新acessToken**/
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "not allowed");
        }

        /**
         * 確認有無Authorization:Bearer accessToken
         * 驗證使用者身分
         * 先直接給予JwtToken
         */
        if (authHeader.substring(6, authHeader.length()) != null && "".equals(authHeader.substring(6, authHeader.length()))) {
            /**這裡怪怪的**/
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "not allowed");
        }
        log.info("accessToken:{}", authHeader.substring(6, authHeader.length()));

        /**
         * 驗證accessToken
         * 完成後return true 會再導向Spring-SecurityFilterChain
         */
        return Token.fromA(authHeader.substring(7, authHeader.length()));
//        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, UserFilter userFilter) {

    }
}
