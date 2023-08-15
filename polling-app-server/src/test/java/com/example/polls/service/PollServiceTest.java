package com.example.polls.service;

import com.example.polls.exception.UnAuthorizedException;
import com.example.polls.model.*;
import com.example.polls.payload.*;
import com.example.polls.repository.ChoiceRepository;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.security.UserPrincipal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class PollServiceTest {
    @InjectMocks
    private PollService pollService;

    @Mock
    private PollRepository pollRepository;

    @Mock
    private ChoiceRepository choiceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Long> typedQuery;

    private User user;
    private UserPrincipal userPrincipal;
    private Poll poll;
    private Poll poll1;
    private PollRequest pollRequest;

    Vote vote = new Vote();

    @Before
    public void setUp() {
        user = new User("abc", "abc", "abc", "abc");
        userPrincipal = UserPrincipal.create(user);

        poll = new Poll();
        poll1 = new Poll();
        poll.setId(1234L);
        poll1.setId(1L);
        poll.setQuestion("Old question");
        poll1.setQuestion("New test poll");
        Choice choice = new Choice("Yes");
        Choice choice1 = new Choice("Choice 1");
        Choice choice2 = new Choice("Choice 2");
        choice.setPoll(poll1);
        choice.setId(1L);
        choice1.setPoll(poll);
        choice2.setPoll(poll);

        vote.setId(1L);
        vote.setPoll(poll1);
        vote.setChoice(choice);

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
    }

    @Test
    public void test_editPoll_authorized() {
        Poll newPoll = new Poll();
        newPoll.setQuestion(pollRequest.getQuestion());

        doReturn(Optional.of(poll)).when(pollRepository).findById(poll.getId());
        doReturn(newPoll).when(pollRepository).save(any());
        doNothing().when(choiceRepository).deleteById(any());
        Poll response = pollService.editPoll(userPrincipal, poll.getId(), pollRequest);
        Assert.assertEquals(response.getQuestion(), pollRequest.getQuestion());
        Mockito.verify(choiceRepository, times(2)).deleteById(any());
        Mockito.verify(pollRepository, times(1)).save(any());
    }

    @Test
    public void test_deletePoll_authorized() {
        Poll newPoll = new Poll();
        newPoll.setQuestion(pollRequest.getQuestion());

        doReturn(Optional.of(poll)).when(pollRepository).findById(poll.getId());
        doNothing().when(pollRepository).deleteById(any());
        pollService.deletePoll(poll.getId(), userPrincipal);
        Mockito.verify(pollRepository, times(1)).deleteById(any());
    }

    @Test
    public void test_editPoll_unauthorized() throws Exception {
        poll.setCreatedBy(123L);
        doReturn(Optional.of(poll)).when(pollRepository).findById(poll.getId());
        Assert.assertThrows("Not authorized", UnAuthorizedException.class,
                () -> pollService.editPoll(userPrincipal, poll.getId(), pollRequest));
    }

    @Test
    public void test_deletePoll_unauthorized() throws Exception {
        poll.setCreatedBy(123L);
        doReturn(Optional.of(poll)).when(pollRepository).findById(poll.getId());
        Assert.assertThrows("Not authorized", UnAuthorizedException.class,
                () -> pollService.deletePoll(poll.getId(), userPrincipal));
    }

    @Test
    public void testFilterPolls() {
        LocalDate date = LocalDate.of(2023, 6, 1);
        Instant dateInstant = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        List<String> keywords = new ArrayList<>();
        keywords.add("poll");
        user.setId(1L);
        UserPrincipal currentUser = UserPrincipal.create(user);
        int page = 1;
        int size = 10;

        poll1.setCreatedAt(dateInstant);
        poll1.setCreatedBy(1L);
        poll1.setExpirationDateTime(Instant.now());

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        List<Poll> filteredPolls = new ArrayList<>();
        filteredPolls.add(poll1);
        Page<Poll> filteredPollPage = new PageImpl<>(filteredPolls.subList(0,1),pageable,filteredPolls.size());

        List<Long> pollIds = Arrays.asList(1L);
        List<ChoiceVoteCount> choiceVotes = Arrays.asList(
                new ChoiceVoteCount(1L, 10L));

        List<Vote> votes = Arrays.asList(vote);

        when(voteRepository.countByPollIdInGroupByChoiceId(pollIds)).thenReturn(choiceVotes);
        when(voteRepository.findByUserIdAndPollIdIn(currentUser.getId(), pollIds)).thenReturn(votes);
        when(userRepository.findByIdIn(Arrays.asList(1L))).thenReturn(Arrays.asList(user));

        CriteriaBuilder criteriaBuilderMock = mock(CriteriaBuilder.class);
        CriteriaQuery<Long> criteriaQueryMock = mock(CriteriaQuery.class);
        Root<Poll> rootMock = mock(Root.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilderMock);
        when(criteriaBuilderMock.createQuery(Long.class)).thenReturn(criteriaQueryMock);
        when(criteriaQueryMock.from(Poll.class)).thenReturn(rootMock);
        when(criteriaQueryMock.select(rootMock.get("id"))).thenReturn(criteriaQueryMock);
        when(entityManager.createQuery(criteriaQueryMock)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of(1L));

        when(pollRepository.findByIds(anyList(),any())).thenReturn(filteredPollPage);
        PagedResponse<PollResponse> result = pollService.filterPolls(date, null, null, keywords, currentUser, page,
                size);
        List<PollResponse> content = result.getContent();
        content.forEach(pollResponse -> {
            assertEquals(pollResponse.getId(), 1);
            assertEquals(pollResponse.getQuestion(), "New test poll");
            assertEquals(pollResponse.getCreationDateTime(), dateInstant);
            assertEquals(pollResponse.getCreatedBy().getName(), "abc");
            assertEquals(pollResponse.getExpired(), true);
        });
    }

}
