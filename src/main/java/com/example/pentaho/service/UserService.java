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


    /**
     * 驗證使用者身分
     * @param user
     * @return
     */
    public Login findUserByUserName(User user) {
        /**待確認使否需要驗證**/
//        Optional<User> userByUserName = userRepository.findUserByUserName(user.getUserName());
//        if(!userByUserName.isPresent()){
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"not allowed");
//        }
//
//        if(!Objects.equals(userByUserName.get().getPassword(),user.getPassword())){
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"not allowed");
//        }
        if("user".equals(user.getUserName())){
            /**模擬DB找到userId**/
            user.setUserId(Long.valueOf("1"));
        }
        return Login.ofRSAJWTToken(user);

    }


}

