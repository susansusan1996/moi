package com.example.pentaho.resource;


import com.example.pentaho.model.Login;
import com.example.pentaho.model.User;
import com.example.pentaho.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserResource {

    private final static Logger log = LoggerFactory.getLogger(UserResource.class);

    @Autowired
    private UserService userService;


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user, HttpServletResponse response) {
        log.info("user:{}", user);
        Login login = userService.findUserByUserName(user);
        Cookie cookie = new Cookie("refresh_token", login.getRefreshToken().getToken());
        cookie.setMaxAge(100800000);
        /**dev用**/
//        cookie.setDomain("localhost");
        /**聖森域名**/
        cookie.setDomain("34.211.215.66");
        cookie.setPath("/api");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return new ResponseEntity<>(login.getAcessToken().getToken(), HttpStatus.OK);
    }

    /***
     *
     */
    @GetMapping("/refresh")
    public ResponseEntity<String> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        log.info("cookies:{}", cookies);
        String refreshToken = "";
        Cookie oldOne = null;
        for (Cookie cookie : cookies) {
            log.info("cookie name:{}", cookie.getName());
            if ("refresh_token".equals(cookie.getName())) {
                oldOne = cookie;
                refreshToken = cookie.getValue();
            }
        }
        log.info("refreshToken:{}", refreshToken);

        /**應該不會進到這裡，因為沒有refreshToken就會被AuthorizationHandlerInterceptor擋住
         * 這個方法應該只做renew
         * **/
        if (refreshToken.equals("")) {
            //error
        }


        /**refresh 確認user訊息**/
        Long userId = (Long) userService.vaildUser(refreshToken);
        User userByUserId = userService.findUserByUserId(userId);
        if (userByUserId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);//403
        }
        /**開始refresh acessToken**/
        /**給新的一組refreshToken**/
        Login login = userService.refreshAll(3);
        oldOne.setValue(login.getRefreshToken().getToken());
        response.addCookie(oldOne);

        /**解析refreshToken 並重新給予AcessToken**/
        return new ResponseEntity<String>(login.getAcessToken().getToken(), HttpStatus.OK);
    }
}
