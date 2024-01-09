package com.example.pentaho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PentahoApplication {


	public static void main(String[] args)  {

		SpringApplication.run(PentahoApplication.class, args);
	}
}




