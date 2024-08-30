package com.example.pentaho.resource;


import com.example.pentaho.component.*;
import com.example.pentaho.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;




@RestController
@RequestMapping("/api")
//@Profile("uat")
@Profile("dev")
public class UserResource {

    private final static Logger log = LoggerFactory.getLogger(UserResource.class);

    @Autowired
    private UserService userService;


    @Autowired
    @Qualifier("testtemplate")
    private RestTemplate restTemplate;


    @GetMapping("/login")
    public ResponseEntity<String> login() {
        Login login = userService.findUserByUserName();
        return new ResponseEntity<>(login.getAcessToken().getToken(), HttpStatus.OK);
    }

}
