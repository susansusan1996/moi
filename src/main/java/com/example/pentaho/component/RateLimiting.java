package com.example.pentaho.component;

import jakarta.validation.constraints.NotNull;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiting {


    @NotNull
    long capacity();

    @NotNull
    long tokens();

    @NotNull
    long mintues();

}
