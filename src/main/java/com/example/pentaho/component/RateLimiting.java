package com.example.pentaho.component;

import jakarta.validation.constraints.NotNull;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiting {

    @NotNull
    String name();

    @NotNull
    double tokens();


}
