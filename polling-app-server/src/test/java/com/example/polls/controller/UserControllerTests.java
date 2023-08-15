package com.example.polls.controller;

import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.User;
import com.example.polls.payload.UserProfile;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UserControllerTests {

    @InjectMocks
    private UserController userController;

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private PollRepository pollRepository;

    @Mock
    private VoteRepository voteRepository;
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Test
    public void test_getAllUsers() {
        User user = new User("Test User", "testUser", "test@gmail.com", "password");
        User user1 = new User("New User", "newUser", "newUser@gmail.com", "password");
        List<User> userList = new ArrayList<>();
        userList.add(user);
        userList.add(user1);
        when(userRepository.findAll()).thenReturn(userList);
        when(pollRepository.countByCreatedBy(any())).thenReturn(4L);
        when(voteRepository.countByUserId(any())).thenReturn(2L);
        List<UserProfile> userProfileList = userController.getAllUserProfile();

        assertEquals(userProfileList.size(), 2);
        assertEquals(userProfileList.get(0).getName(), "Test User");
        assertEquals(userProfileList.get(1).getName(), "New User");
        assertEquals(userProfileList.get(0).getPollCount(), 4);
        assertEquals(userProfileList.get(0).getVoteCount(), 2);
    }

    @Test
    public void test_editUser() throws Exception {
        User user = new User("Test User", "testUser", "test@gmail.com", "password");
        user.setId(1L);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

        doReturn(Optional.of(user)).when(userRepository).findById(1L);
        doReturn(false).when(userRepository).existsByUsername(any());
        String requestBody = "{\"username\": \"newUser123\",\"name\": \"New User\"}";
        mockMvc
                .perform(
                        put(
                                "/api/users/{userId}", 1L)
                                .content(requestBody)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .characterEncoding("UTF-8"))
                .andExpect(status().isCreated());
    }

    @Test
    public void test_editUserError() throws Exception {
        User user = new User("Test User", "testUser", "test@gmail.com", "password");
        user.setId(1L);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

        doReturn(Optional.of(user)).when(userRepository).findById(1L);
        doReturn(false).when(userRepository).existsByUsername(any());
        String requestBody = "{\"name\": \"New User\"}";
        String expectedResponse = "{\"success\":false,\"message\":\"Required fields missing\"}";
        MvcResult result = mockMvc
                .perform(
                        put(
                                "/api/users/{userId}", 1L)
                                .content(requestBody)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .characterEncoding("UTF-8"))
                .andExpect(status().isBadRequest()).andReturn();

        String actualResponse = result.getResponse().getContentAsString();
        assertEquals(expectedResponse, actualResponse);
    }

    public void test_deleteUserPositive() {
        doNothing().when(userService).deleteUser(any());
        ResponseEntity<?> returnedValue = userController.deleteUser(1L);
        assertEquals(returnedValue.getBody(), "Deleted the user");
    }

    @Test
    public void test_deleteUserNegative() {
        doThrow(new ResourceNotFoundException("", "", "")).when(userService).deleteUser(any());
        ResponseEntity<?> returnedValue = userController.deleteUser(1L);
        assertEquals(returnedValue.getBody(), "Error deleting user as user not found");
    }
}
