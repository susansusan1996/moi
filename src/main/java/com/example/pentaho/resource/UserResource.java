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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Hidden
public class UserResource {

    private final static Logger log = LoggerFactory.getLogger(UserResource.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private ApiKeyService apiKeyService;


    /**
     * login
     * @param user
     * @return
     * {"id":"673f7eec-8ae5-4e79-ad3a-42029eedf742","departName":"A05"}
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "登入",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = User.class),
                            examples = @ExampleObject(value = "{\"id\":\"673f7eec-8ae5-4e79-ad3a-42029eedf742\",\"departName\":\"A05\"}")
                    )
            )
            @RequestBody User user) {
        log.info("user:{}", user);
        Login login = userService.findUserByUserName(user);
        return new ResponseEntity<>(login.getAcessToken().getToken(), HttpStatus.OK);
    }


    @PostMapping("/refreshToken")
    public ResponseEntity<JwtReponse> refreshToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "refreshToken",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = User.class),
                            examples = @ExampleObject(value = "{\"id\":\"673f7eec-8ae5-4e79-ad3a-42029eedf742\",\"refreshToken\":\"673f7eec-8ae5-4e79-ad3a-42029eedf742\"}")
                    )
            )
            @RequestBody RefreshToken refreshToken) throws Exception {
        log.info("refreshToken:{}", refreshToken);
        List<RefreshToken> refreshTokens = refreshTokenService.findByRefreshToken(refreshToken);
        if (!refreshTokens.isEmpty()) {
            try {
                if (refreshTokenService.verifyExpiration(refreshTokens.get(0))) {
                    if(refreshTokens.get(0).getId().equals(refreshToken.getId())){
                        return new ResponseEntity<>(apiKeyService.getApiKey(refreshTokens.get(0)), HttpStatus.OK);
                    }
                    return new ResponseEntity<>(new JwtReponse("使用者資訊錯誤"), HttpStatus.OK);
                }
            }catch (MoiException e){
                return new ResponseEntity<>(new JwtReponse("api_key過期，請重新申請"), HttpStatus.OK);
            }
        }
        return ResponseEntity.badRequest().build();
    }


}
