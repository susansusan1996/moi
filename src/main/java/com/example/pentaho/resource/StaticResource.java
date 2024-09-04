package com.example.pentaho.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticResource {


    private Logger log = LoggerFactory.getLogger(StaticResource.class);


    /***
     * /iisi/ 重定向
     */
    @GetMapping(value = "/")
    public String forbiden() {
        return "404";
    }

    /***
     * /iisi/single-query 的重定向
     */
    @GetMapping(value = "/single-query")
    public String forward(HttpServletRequest httpServletRequest) {
        log.info("uri:{}",httpServletRequest.getRequestURI());
//        switch (){
//
//
//        }

        return "forward:/index.html";
    }


    /***
     * /iisi/single-query-token 的重定向
     */
    @GetMapping(value = "/single-query-token")
    public String forwardToken() {
        return "forward:/index.html";
    }


    /***
     * /single-query-taskId 的 重定向
     */
    @GetMapping(value = "/single-query-taskId")
    public String forwardTaskId() {
        return "forward:/index.html";
    }

}
