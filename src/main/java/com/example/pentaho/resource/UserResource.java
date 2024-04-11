package com.example.pentaho.resource;


import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.service.ApiKeyService;
import com.example.pentaho.service.RefreshTokenService;
import com.example.pentaho.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Profile("uat")
public class UserResource {

    private final static Logger log = LoggerFactory.getLogger(UserResource.class);

    @Autowired
    private UserService userService;


    @GetMapping("/login")
    public ResponseEntity<String> login() {
        Login login = userService.findUserByUserName();
        return new ResponseEntity<>(login.getAcessToken().getToken(), HttpStatus.OK);
    }


}
