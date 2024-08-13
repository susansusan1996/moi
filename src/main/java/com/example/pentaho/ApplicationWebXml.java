package com.example.pentaho;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * tomcat 不會自動去run  PentahoApplication.class
 * 需要給他一個接口 (繼承 SpringBootServletInitializer 並覆寫 configure()
 */
public class ApplicationWebXml extends SpringBootServletInitializer {


    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(PentahoApplication.class);
    }

}
