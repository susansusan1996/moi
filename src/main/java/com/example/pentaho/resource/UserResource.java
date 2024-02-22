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
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        log.info("user:{}", user);
        Login login = userService.findUserByUserName(user);
//        log.info("login:{}", login);
        return new ResponseEntity<>(login.getAcessToken().getToken(), HttpStatus.OK);
    }


}
