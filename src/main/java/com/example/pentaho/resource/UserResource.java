package com.example.pentaho.resource;


import com.example.pentaho.component.Login;
import com.example.pentaho.component.User;
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


    /**
     * login
     * @param user
     * @param response
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user, HttpServletResponse response) {
        log.info("user:{}", user);
        Login login = userService.findUserByUserName(user);
        log.info("login:{}", login);
        /**沒有用到cookie 先註解**/
        Cookie cookie = new Cookie("refresh_token", login.getRefreshToken().getToken());
        cookie.setMaxAge(100800000);
        /**dev用**/
        //cookie.setDomain("localhost");
        cookie.setDomain("34.211.215.66");
        cookie.setPath("/api");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return new ResponseEntity<>(login.getAcessToken().getToken(), HttpStatus.OK);
    }

    /***
     *refreshToken
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


        /**解密refreshToken確認user訊息**/
        User user = userService.vertifyUserInfo(refreshToken);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        /**開始refresh acessToken**/
        /**給新的一組refreshToken**/
        Login login = userService.refreshAllRSA(user.getUserId());
        oldOne.setValue(login.getRefreshToken().getToken());
        response.addCookie(oldOne);

        /**解析refreshToken 並重新給予AcessToken**/
        return new ResponseEntity<String>(login.getAcessToken().getToken(), HttpStatus.OK);
    }
}
