package com.example.pentaho.service;

import com.example.pentaho.model.Login;
import com.example.pentaho.model.Token;
import com.example.pentaho.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
//@Transactional
public class UserService {

    private final static Logger log = LoggerFactory.getLogger(UserService.class);

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


    public UserService(Token token, @Value("${application.security.access-private-key}") String accessPrivateKey, @Value("${application.security.refresh-private-key}") String refreshPrivateKey) {
        this.token = token;
        this.accessPrivateKey = accessPrivateKey;
        this.refreshPrivateKey = refreshPrivateKey;
    }

    public Login findUserByUserName(User user) {
//        Optional<User> userByUserName = userRepository.findUserByUserName(user.getUserName());
//        if(!userByUserName.isPresent()){
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"not allowed");
//        }
//
//        if(!Objects.equals(userByUserName.get().getPassword(),user.getPassword())){
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"not allowed");
//        }
        user.setUserId(Long.valueOf("1"));

        return Login.of(user.getUserId(), accessPrivateKey, refreshPrivateKey);

    }

    public User findUserByUserId(long userid) {
        return new User(userid);
//        return userRepository.findById(userid).get();
    }

    public long vaildUser(String refreshTokenStr) {
        long userId = Token.from(refreshTokenStr);
        return userId;
    }

    public Login refreshAll(long userId) {
        return Login.of(userId, accessPrivateKey, refreshPrivateKey);
    }
}

