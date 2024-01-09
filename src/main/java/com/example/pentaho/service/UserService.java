package com.example.pentaho.service;

import com.example.pentaho.component.KeyComponent;
import com.example.pentaho.component.Login;
import com.example.pentaho.component.Token;
import com.example.pentaho.component.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
//@Transactional
public class UserService {

    private final static Logger log = LoggerFactory.getLogger(UserService.class);

    private final KeyComponent keyComponent;


    @Autowired
    private Token token;

//    @Autowired
//    private UserRepository userRepository;

    private final String accessPrivateKey;

    private final String refreshPrivateKey;


//    public UserService(Token token, UserRepository userRepository, String accessPrivateKey, String refreshPrivateKey) {
//        this.token = token;
//        this.userRepository = userRepository;
//        this.accessPrivateKey = accessPrivateKey;
//        this.refreshPrivateKey = refreshPrivateKey;
//    }


    public UserService(KeyComponent keyComponent, Token token, @Value("${application.security.access-private-key}") String accessPrivateKey, @Value("${application.security.refresh-private-key}") String refreshPrivateKey) {
        this.keyComponent = keyComponent;
        this.token = token;
        this.accessPrivateKey = accessPrivateKey;
        this.refreshPrivateKey = refreshPrivateKey;
    }

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

    public User findUserByUserId(User user) {
//      return userRepository.findById(userid).get();
        if (user.getUserId() == 1) {
            return user;
        }
        return null;
    }


    /***
     * 驗證user信息
     * @param refreshTokenStr
     * @return
     */
    public User vertifyUserInfo(String refreshTokenStr) {
        User user = Token.extractUserFromRSAJWTToken((refreshTokenStr),keyComponent.getKeyname());
        /**解密失敗 或 解密後為空**/
        if(user == null){
            return null;
        }
        /**第二層驗證，再確認是否去掉**/
         return findUserByUserId(user);
    }


    public Login refreshAll(long userId) {
        return Login.ofHS256(userId);
    }

    public Login refreshAllRSA(long userId) {
        User user = new User(userId);
        return Login.ofRSAJWTToken(user);
    }
}

