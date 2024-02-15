package com.example.pentaho.repository;

import com.example.pentaho.component.User;
//import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//@Repository
public interface UserRepository
//        extends JpaRepository<User, Long>
{
    Optional<User> findUserByUserName(String username);
}
