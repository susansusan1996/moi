package com.example.pentaho.resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class StaticResource {


    /***
     *重定向
     */
    @GetMapping(value = "/single-query")
    public String forward() {
        return "forward:/index.html";
    }

}
