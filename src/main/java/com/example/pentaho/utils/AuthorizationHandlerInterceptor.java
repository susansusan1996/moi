package com.example.pentaho.utils;

import com.example.pentaho.component.*;
import com.example.pentaho.service.RedisService;
import com.example.pentaho.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Component
public class AuthorizationHandlerInterceptor implements HandlerInterceptor {

    private final static Logger log = LoggerFactory.getLogger(AuthorizationHandlerInterceptor.class);

    private final KeyComponent keyComponent;



    private final RefreshTokenService refreshTokenService;


    private final static List<String> openAPI = Arrays.asList("/iisi/api/api-key/revise-address","/iisi/api/api-key/query-track","/iisi/api/api-key/query-standard-address","/iisi/api/api-key/query-single");


    public AuthorizationHandlerInterceptor(KeyComponent keyComponent, RefreshTokenService refreshTokenService) {
        this.keyComponent = keyComponent;
        this.refreshTokenService = refreshTokenService;
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
        log.info("使用的解密公鑰:{}",keyName);

        /**確認有沒有token**/
        String authHeader = request.getHeader("Authorization");
        log.info("request header = { Authorization:Bearer accessToken }:{}", authHeader);

        /**確認有無Authorization:Bearer 的 header**/
        if (authHeader == null || !authHeader.startsWith("Bearer")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
        }

        /**
         * 確認有無Authorization:Bearer RASJWTToken
         * 驗證使用者身分
         * 先直接給予JwtToken
         */
        if (authHeader.substring(7, authHeader.length()) == null && "".equals(authHeader.substring(7, authHeader.length()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
        }

        String RSATokenJwt = authHeader.substring(7, authHeader.length());
        if(Token.fromRSAJWTToken(RSATokenJwt, keyName)){

            //todo:這支給pentaho用的
            if("/iisi/api/batchForm/finished".equals(request.getRequestURI())){
                return true;
            }


            //todo:應該不會這樣用，可以刪掉
            if("/iisi/api/singlequery/qrcode-data-token".equals(request.getRequestURI())){
                OpenPageDTO.QrcodeDTO qrcodeDTO = Token.extractQrcodeDTOFromRSAJWTToken(RSATokenJwt, keyName);
                QrcodeContextUtils.setQrcodeData(qrcodeDTO);
                log.info("Token Check OK");
                return true;
            }

            /*可以解密才繼續往下做*/
            User user = Token.extractUserFromRSAJWTToken(RSATokenJwt,keyName);
            log.info("user:{}",user);
            /*判斷使用者是不是拿refresh_token*/
            if("refresh_token".equals(user.getTokenType())){
                log.info("使用者拿refresh_token打api,駁回");
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
            }

            //todo:如果是 OpenAPI 的uri 要先去 redis拿整個set出來判斷IP位置
           if(openAPI.contains(request.getRequestURI())){
               /**/
               if(!refreshTokenService.checkRemoteIp(user.getId(),request.getRemoteHost())){
                   throw new ResponseStatusException(HttpStatus.FORBIDDEN, "IP位置錯誤");
               }
           }

            UserContextUtils.setUserHolder(user);
                log.info("Token Check OK");
                return true;
            } //token 可以解密


        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
    }


    /**
     * 響應前
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理 ThreadLocal 避免内存洩漏
        QrcodeContextUtils.clear();
    }
}


