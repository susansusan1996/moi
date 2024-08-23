package com.example.pentaho.resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticResource {


    /***
     *重定向
     */
    @GetMapping(value = "/single-query")
    public String forward() {
        return "forward:/index.html";
    }


    /***
     *重定向
     */
    @GetMapping(value = "/single-query-token")
    public String forwardToken() {
        return "forward:/index.html";
    }


    /***
     *重定向
     */
    @GetMapping(value = "/single-query-taskId")
    public String forwardTaskId() {
        return "forward:/index.html";
    }

}
