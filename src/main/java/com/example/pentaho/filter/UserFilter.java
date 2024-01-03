package com.example.pentaho.filter;

import com.example.pentaho.utils.AuthorizationHandlerInterceptor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/***
 * 請求的過濾器
 * (這隻還在測試中，目前沒有用到)
 */
public class UserFilter extends OncePerRequestFilter {

    private final AuthorizationHandlerInterceptor authorizationHandlerInterceptor;

    public UserFilter(AuthorizationHandlerInterceptor authorizationHandlerInterceptor) {
        this.authorizationHandlerInterceptor = authorizationHandlerInterceptor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            /**請求進入spring sercurity filterchain模組前，可添加自自訂義的攔截**/
            authorizationHandlerInterceptor.preHandle(request, response, this);

            /**SecurityFilterChain 安全過濾鏈：
             SecurityFilterChain 是 一層層安全過濾鏈 模組，定義請求該如何被一層層安全過濾器處理。
             在 SecurityConfig 中，調用 http.addFilterBefore 可接自定義的過濾器(需繼承OncePerRequestFilter並覆寫)添加到安全過濾器模組前。
             **/
            filterChain.doFilter(request, response);

            /**請求過濾完成後的邏輯**/
//        authorizationHandlerInterceptor.postHandle(request, response, this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
