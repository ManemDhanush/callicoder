package com.example.polls.service;

import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.User;
import com.example.polls.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {
    @InjectMocks private UserService userService;

    @Mock private UserRepository userRepository;

    @Test
    public void test_deleteUserPositive() {
        doReturn(Optional.of((Object) new User())).when(userRepository).findById(any());
        doNothing().when(userRepository).deleteById(any());
        userService.deleteUser(1L);
        Mockito.verify(userRepository, times(1)).deleteById(any());;
    }

    @Test
    public void test_deleteUserNegative() {
        try{
            userService.deleteUser(1L);
        }
        catch (Exception e){
            Assert.assertTrue(e instanceof ResourceNotFoundException);
        }
    }

}
