package com.example.polls.controller;

import com.example.polls.model.*;
import com.example.polls.payload.ChoiceRequest;
import com.example.polls.payload.PollLength;
import com.example.polls.payload.PollRequest;
import com.example.polls.security.UserPrincipal;
import com.example.polls.service.PollService;
import org.junit.Before;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.Test;

import javax.servlet.Filter;


@ExtendWith(MockitoExtension.class)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
@SpringBootTest
public class PollControllerTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Filter springSecurityFilterChain;

    @InjectMocks
    private PollController pollController;

    @MockBean
    private PollService pollService;

    private User user;
    private UserPrincipal userPrincipal;
    private Poll poll;
    private PollRequest pollRequest;
    @Before
    public void setUp() {
        user = new User("abc","abc","abc","abc");

        poll = new Poll();
        poll.setId(1234L);
        poll.setQuestion("Old question");
        Choice choice1 = new Choice("Choice 1");
        Choice choice2 = new Choice("Choice 2");
        choice1.setPoll(poll);
        choice2.setPoll(poll);

        poll.setChoices(List.of(choice1, choice2));
        poll.setCreatedBy(user.getId());

        pollRequest = new PollRequest();
        ChoiceRequest choiceRequest1 = new ChoiceRequest();
        choiceRequest1.setText(choice1.getText());
        ChoiceRequest choiceRequest2 = new ChoiceRequest();
        choiceRequest1.setText(choice2.getText());
        pollRequest.setChoices(List.of(choiceRequest1, choiceRequest2));

        PollLength pollLength = new PollLength();
        pollLength.setDays(1);
        pollLength.setHours(0);
        pollRequest.setPollLength(pollLength);
        pollRequest.setQuestion("New question");

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }
    @Test
    @WithMockUser
    public void test_editPoll_user() throws Exception {
        user.setRoles(Set.of(new Role(RoleName.ROLE_USER)));
        userPrincipal = UserPrincipal.create(user);
        doReturn(poll).when(pollService).editPoll(any(), any(), any());
        when(pollService.editPoll(any(), any(), any())).thenReturn(poll);
        String eg = "{\"question\": \"Is this a poll or not?\",\"choices\": [{\"text\": \"No\"},{\"text\": \"Yes\"}],\"pollLength\": {\"days\": 2,\"hours\": 0}}";
        RequestBuilder request = put(
                "/api/polls/{pollId}", 1234L
        ).with(user(userPrincipal))
                .content(eg)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8");
        mockMvc
                .perform(request)
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    public void test_deletePoll_user() throws Exception {
        doNothing().when(pollService).deletePoll(any(), any());
        user.setRoles(Set.of(new Role(RoleName.ROLE_USER)));
        userPrincipal = UserPrincipal.create(user);

        RequestBuilder request = delete("/api/polls/1234")
                .with(user(userPrincipal));
        mockMvc
                .perform(request).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void test_editPoll_admin() throws Exception {
        user.setRoles(Set.of(new Role(RoleName.ROLE_ADMIN)));
        user.setId(2605L);
        userPrincipal = UserPrincipal.create(user);
        doReturn(poll).when(pollService).editPoll(any(), any(), any());
        when(pollService.editPoll(any(), any(), any())).thenReturn(poll);
        String eg = "{\"question\": \"Is this a poll or not?\",\"choices\": [{\"text\": \"No\"},{\"text\": \"Yes\"}],\"pollLength\": {\"days\": 2,\"hours\": 0}}";
        RequestBuilder request = put(
                "/api/polls/{pollId}", 1234L
        ).with(user(userPrincipal))
                .content(eg)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8");
        mockMvc
                .perform(request)
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void test_deletePoll_admin() throws Exception {
        user.setRoles(Set.of(new Role(RoleName.ROLE_ADMIN)));
        user.setId(2605L);
        doNothing().when(pollService).deletePoll(any(), any());
        user.setRoles(Set.of(new Role(RoleName.ROLE_USER)));
        userPrincipal = UserPrincipal.create(user);

        RequestBuilder request = delete("/api/polls/1234")
                .with(user(userPrincipal));
        mockMvc
                .perform(request).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void test_createPoll_unauthorized() throws Exception {
        String pollRequestPayload = "{\"question\": \"Is this a poll or not?\",\"choices\": [{\"text\": \"No\"},{\"text\": \"Yes\"}],\"pollLength\": {\"days\": 2,\"hours\": 0}}";
        doReturn(poll).when(pollService).createPoll(any());
        user.setRoles(Set.of(new Role(RoleName.ROLE_USER)));
        userPrincipal = UserPrincipal.create(user);
        RequestBuilder request = post("/api/polls")
                .with(user(userPrincipal)).content(pollRequestPayload)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8");
        mockMvc
                .perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"POLLSTER"})
    public void test_createPoll_authorized() throws Exception {
        String pollRequestPayload = "{\"question\": \"Is this a poll or not?\",\"choices\": [{\"text\": \"No\"},{\"text\": \"Yes\"}],\"pollLength\": {\"days\": 2,\"hours\": 0}}";
        user.setRoles(Set.of(new Role(RoleName.ROLE_POLLSTER)));
        userPrincipal = UserPrincipal.create(user);
        doReturn(poll).when(pollService).createPoll(any());

        RequestBuilder request = post("/api/polls")
                .with(user(userPrincipal)).content(pollRequestPayload)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8");
        mockMvc
                .perform(request)
                .andExpect(status().isCreated());
    }
}
