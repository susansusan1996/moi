package com.example.pentaho.utils;

import com.example.pentaho.component.User;
import io.netty.util.concurrent.FastThreadLocal;

public class UserContextUtils {

    private static FastThreadLocal<User> userHolder = new FastThreadLocal<User>();

    public static void setUserHolder(User user){
        userHolder.set(user);
    }

    public static User getUserHolder(){
       return userHolder.get();
    }

    public static void removeUserHolder(){
        userHolder.remove();
    }

}
