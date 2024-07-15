package com.example.pentaho.utils;

import com.example.pentaho.component.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;


@Component
public class AuthorizationHandlerInterceptor implements HandlerInterceptor {

    private final static Logger log = LoggerFactory.getLogger(AuthorizationHandlerInterceptor.class);

    private final KeyComponent keyComponent;


    public AuthorizationHandlerInterceptor(KeyComponent keyComponent) {
        this.keyComponent = keyComponent;
    }


      @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
          log.info("Token Check");

          HandlerMethod handlerMethod = (HandlerMethod) handler;
          Class<?> clazz = handlerMethod.getBeanType();
          Method method = handlerMethod.getMethod();

          log.info("uri:{}",request.getRequestURI());

          /**依註解取keyName**/
          /**聖森公鑰**/
          if(clazz.isAnnotationPresent(Authorized.class) || method.isAnnotationPresent(Authorized.class)){
              String keyName = keyComponent.keyMapping(method.getAnnotation(Authorized.class).keyName());
            return vertifyToken(request,keyName);
          }

          if(clazz.isAnnotationPresent(UnAuthorized.class) || method.isAnnotationPresent(UnAuthorized.class)){
              log.info("Token Check OK");
              return true;
          }
          //todo:
          log.info("Token Check OK");
          return true;
      }

    /***
     * 驗證
     * @param request
     * @param keyName
     * @return
     * @throws Exception
     */
    public boolean vertifyToken(HttpServletRequest request, String keyName) throws Exception {
        log.info("keyName:{}",keyName);

        /**確認有沒有token**/
        String authHeader = request.getHeader("Authorization");

        /**確認有無Authorization:Bearer 的 header**/
        if (authHeader == null || !authHeader.startsWith("Bearer")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
        }

        /**
         * 確認有無token
         * 驗證使用者身分
         * 先直接給予JwtToken
         */
        if (authHeader.substring(7, authHeader.length()) == null && "".equals(authHeader.substring(7, authHeader.length()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
        }

        String RSATokenJwt = authHeader.substring(7, authHeader.length());
        if(Token.fromRSAJWTToken(RSATokenJwt, keyName)){

            if("/iisi/api/batchForm/finished".equals(request.getRequestURI())){
                return true;
            }
                User user = Token.extractUserFromRSAJWTToken(RSATokenJwt,keyName);
                log.info("user:{}",user);
                //判斷使用者是不是拿refresh_token
                if("refresh_token".equals(user.getTokenType())){
                    log.info("使用者拿refresh_token打api,駁回");
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
                }
                UserContextUtils.setUserHolder(user);
                log.info("Token Check OK");
                return true;
            }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
    }

}


