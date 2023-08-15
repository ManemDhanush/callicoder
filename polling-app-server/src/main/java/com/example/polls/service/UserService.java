package com.example.polls.service;

import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.User;
import com.example.polls.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService{
    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public void deleteUser(Long userId){
            userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            userRepository.deleteById(userId);
    }
}
