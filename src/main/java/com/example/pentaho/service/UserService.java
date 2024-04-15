package com.example.pentaho.service;

import com.example.pentaho.component.KeyComponent;
import com.example.pentaho.component.Login;
import com.example.pentaho.component.Token;
import com.example.pentaho.component.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService  {

    private final static Logger log = LoggerFactory.getLogger(UserService.class);

    private KeyComponent keyComponent;


    public UserService(KeyComponent keyComponent) {
        this.keyComponent = keyComponent;
    }

    /**
     * 驗證使用者身分
     * @param user
     * @return
     */
    public Login findUserByUserName() {
        User user = new User("{\n" +
                "  \"haveToChangePwd\": false,\n" +
                "  \"localizeName\": \"管理員姓名\",\n" +
                "  \"orgId\": \"ADMIN\",\n" +
                "  \"departName\": \"管理機關_單位UPDATE\",\n" +
                "  \"id\": \"673f7eec-8ae5-4e79-ad3a-42029eedf742\",\n" +
                "  \"username\": \"admin\",\n" +
                "  \"email\": \"admin@gmail.com\",\n" +
                "  \"roles\": [\n" +
                "    \"ROLE_IUSER\",\n" +
                "    \"ROLE_ADMIN\",\n" +
                "    \"ROLE_MODERATOR\"\n" +
                "  ],\n" +
                "  \"token\": \"eyJhbGciOiJSUzUxMiJ9.eyJ1c2VySW5mbyI6eyJoYXZlVG9DaGFuZ2VQd2QiOmZhbHNlLCJsb2NhbGl6ZU5hbWUiOiLnrqHnkIblk6Hlp5PlkI0iLCJvcmdJZCI6IkFETUlOIiwiZGVwYXJ0TmFtZSI6IueuoeeQhuapn-mXnF_llq7kvY1VUERBVEUiLCJpZCI6IjY3M2Y3ZWVjLThhZTUtNGU3OS1hZDNhLTQyMDI5ZWVkZjc0MiIsInVzZXJuYW1lIjoiYWRtaW4iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInJvbGVzIjpbIlJPTEVfSVVTRVIiLCJST0xFX0FETUlOIiwiUk9MRV9NT0RFUkFUT1IiXSwidG9rZW4iOiJCZWFyZXIiLCJyZWZyZXNoVG9rZW4iOiJCZWFyZXIiLCJyZW1vdGVBZGRyIjoiMTkyLjE2OC4zMS4xNjciLCJ4cmVhbElwIjoiMTEwLjI4LjIuMTIzIn0sImlhdCI6MTcxMjc0MDEwNSwiZXhwIjoxNzEyNzQzNzA1fQ.fDW7qtvi3jty1asX0uPrWz1AVccxj4L-eU86KoUolKNNrxYqXDewHGB5t5CssYzYiBLBqhpL1BQ63tbwDOuOL46VkdXGeuJcbMcAs8bzBQE6sYkeMTSwibSipZUPLF4ytbIq0zlWsWLakMAPtC31ye-zRYBO9_aJvHoK3etVBaGi1bXslN-as3mIvr5B0o0rn1ET3seMyd5azwcUqC1o2ZfpsS0i7oINTtPX86mpeUnNmSYj6kVYQ9U3cyu3CdCpq8Fy5JhJs0NhjtDnTJbNHxfqj3epB5OyZb6JV-S2nU9eye-Ab-xZQO_GhaDGE1jsvxZN6149D7FixLgJN8yHzchuTvQiR20o9HWw-w7i2MmdcPoakpG_uTuC7Kd94T9HRTk7kqukwmMBdIv8nA3oB9Id0O3FeqFj06dI3lZ-3huFfLQjmb1tadlc_cHVGsHklxPVZ8js2rwHlRraykCu6tKK4pB2oNCIwA0XouVh4f0Uk4X_695irqrVT2qQq4r7FmJBajkJmSe0AbRAxEF1QrfjXitTAQrCgaYfkWBnFQAFO1DX5VuJaGnOgpu7CiW7Q7qSTcLH3GdDauoRJzBNquW1zGJG7yzxulsH13wTgtYWmsqJ6p33VhIZFx5N0RkMmWnmSXi34NnF91yjSgEOn2v0liYRVw71TWkRuY2Y3jQ\",\n" +
                "  \"refreshToken\": \"eyJhbGciOiJSUzUxMiJ9.eyJ1c2VySW5mbyI6eyJoYXZlVG9DaGFuZ2VQd2QiOmZhbHNlLCJsb2NhbGl6ZU5hbWUiOiLnrqHnkIblk6Hlp5PlkI0iLCJvcmdJZCI6IkFETUlOIiwiZGVwYXJ0TmFtZSI6IueuoeeQhuapn-mXnF_llq7kvY1VUERBVEUiLCJpZCI6IjY3M2Y3ZWVjLThhZTUtNGU3OS1hZDNhLTQyMDI5ZWVkZjc0MiIsInVzZXJuYW1lIjoiYWRtaW4iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInJvbGVzIjpbIlJPTEVfSVVTRVIiLCJST0xFX0FETUlOIiwiUk9MRV9NT0RFUkFUT1IiXSwidG9rZW4iOiJCZWFyZXIiLCJyZWZyZXNoVG9rZW4iOiJCZWFyZXIiLCJyZW1vdGVBZGRyIjoiMTkyLjE2OC4zMS4xNjciLCJ4cmVhbElwIjoiMTEwLjI4LjIuMTIzIn0sImlhdCI6MTcxMjc0MDEwNSwiZXhwIjoxNzEyODI2NTA1fQ.Q_WDdlopVwbHhxuDE5693ZP7guOGywqSUcVbGLiqZVKPPUohUETIxLE5CaQYGHwlPsVAiVEtZJPaBouu1uXy1amgHXxDM2v9eW8udTH5dI5C_VLVXYmXyAXQ7DQqPl0R4uenRxvob8HLU6Bjd-oWxAuVxM9jJIYvc6YBmBxIEotQLSMogodMhbr1S6pO6M2P4UYtfPLr9aMOtniVRh7rlilT2bDnjKvFeLYgIrM84fVOESwCQzWMrkqfTYfKbQqKKgALTMLSbjYDD_SZ2_UuiD1-QYFUSF0-TwBlkv5pTbq-vDqklJ-i51CE5BNymJJvKq3dU183qKz1L4ExToLVW_OPge0UH4KxJzp7SUBANyfG2BQybvzZ5YnOAt111aIe6hriKR_2CKkiQRbjgjex7Mw7r8-5jAQTpaKHR-nfZ2_TQh9Q-gBjj2xcBE4OmI7UqAffjC_uWl_RZEhgGX2WDlt3OIvAfRu-pSbr6-IW2NW65lV01V-nMbNCri8K5SxFScu0bcJYFUpYLIW2cN-PF6yCYSaV4YRSr6F-GGoJzLHDRziDVy6tsUjHjPCSJlUwnQekBgydC2wx7huIiWOVNuDepXvA48FE1gi37SAQF0o1_4lMBLj6q2eBM8nmiwguQ4DUJbuGHe2rP8IMC75oKm3OLb5PH_H8DAVlDOW8xGY\",\n" +
                "  \"remoteAddr\": \"192.168.31.167\",\n" +
                "  \"xrealIp\": \"110.28.2.123\"\n" +
                "}");
        return Login.ofRSAJWTToken(user,keyComponent.getApPrikeyName());

    }


}

