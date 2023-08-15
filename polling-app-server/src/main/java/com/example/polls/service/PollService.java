package com.example.polls.service;

import com.example.polls.exception.BadRequestException;
import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.exception.UnAuthorizedException;
import com.example.polls.model.*;
import com.example.polls.payload.PagedResponse;
import com.example.polls.payload.PollRequest;
import com.example.polls.payload.PollResponse;
import com.example.polls.payload.VoteRequest;
import com.example.polls.repository.ChoiceRepository;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.security.UserPrincipal;
import com.example.polls.util.AppConstants;
import com.example.polls.util.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.*;
import org.springframework.security.core.GrantedAuthority;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PollService {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ChoiceRepository choiceRepository;

    private static final Logger logger = LoggerFactory.getLogger(PollService.class);

    public PagedResponse<PollResponse> getAllPolls(UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        // Retrieve Polls
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Poll> polls = pollRepository.findAll(pageable);

        if (polls.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), polls.getNumber(),
                    polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
        }

        // Map Polls to PollResponses containing vote counts and poll creator details
        List<Long> pollIds = polls.map(Poll::getId).getContent();
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);
        Map<Long, User> creatorMap = getPollCreatorMap(polls.getContent());

        List<PollResponse> pollResponses = polls.map(poll -> {
            return ModelMapper.mapPollToPollResponse(poll,
                    choiceVoteCountMap,
                    creatorMap.get(poll.getCreatedBy()),
                    pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).getContent();

        logger.info("RESPONSE: Fetched all " + pollResponses.size() + " polls");
        return new PagedResponse<>(pollResponses, polls.getNumber(),
                polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
    }

    public PagedResponse<PollResponse> getPollsCreatedBy(String username, UserPrincipal currentUser, int page,
            int size) {
        validatePageNumberAndSize(page, size);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Retrieve all polls created by the given username
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Poll> polls = pollRepository.findByCreatedBy(user.getId(), pageable);

        if (polls.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), polls.getNumber(),
                    polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
        }

        // Map Polls to PollResponses containing vote counts and poll creator details
        List<Long> pollIds = polls.map(Poll::getId).getContent();
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);

        List<PollResponse> pollResponses = polls.map(poll -> {
            return ModelMapper.mapPollToPollResponse(poll,
                    choiceVoteCountMap,
                    user,
                    pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).getContent();

        logger.info("RESPONSE: Fetched all " + pollResponses.size() + " polls created by " + username);
        return new PagedResponse<>(pollResponses, polls.getNumber(),
                polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
    }

    public PagedResponse<PollResponse> getPollsVotedBy(String username, UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Retrieve all pollIds in which the given username has voted
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Long> userVotedPollIds = voteRepository.findVotedPollIdsByUserId(user.getId(), pageable);

        if (userVotedPollIds.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), userVotedPollIds.getNumber(),
                    userVotedPollIds.getSize(), userVotedPollIds.getTotalElements(),
                    userVotedPollIds.getTotalPages(), userVotedPollIds.isLast());
        }

        // Retrieve all poll details from the voted pollIds.
        List<Long> pollIds = userVotedPollIds.getContent();

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<Poll> polls = pollRepository.findByIdIn(pollIds, sort);

        // Map Polls to PollResponses containing vote counts and poll creator details
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);
        Map<Long, User> creatorMap = getPollCreatorMap(polls);

        List<PollResponse> pollResponses = polls.stream().map(poll -> {
            return ModelMapper.mapPollToPollResponse(poll,
                    choiceVoteCountMap,
                    creatorMap.get(poll.getCreatedBy()),
                    pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).collect(Collectors.toList());

        logger.info("RESPONSE: Fetched all " + pollResponses.size() + " polls voted by " + username);
        return new PagedResponse<>(pollResponses, userVotedPollIds.getNumber(), userVotedPollIds.getSize(),
                userVotedPollIds.getTotalElements(), userVotedPollIds.getTotalPages(), userVotedPollIds.isLast());
    }

    public PagedResponse<PollResponse> filterPolls(LocalDate date, LocalDate startDate, LocalDate endDate,
            List<String> keywords, UserPrincipal currentUser, int page, int size) {
        // Create a base query
        Specification<Poll> spec = Specification.where(null);

        // Add filters based on provided parameters
        if (date != null) {
            Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = spec.and((root, query, cb) -> cb.between(root.get("createdAt"), startOfDay, endOfDay));
        }

        if (startDate != null && endDate != null) {
            Instant startDateInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endDateInstant = endDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = spec.and((root, query, cb) -> cb.between(root.get("createdAt"), startDateInstant, endDateInstant));
        }

        if (keywords != null && !keywords.isEmpty()) {
            for (String keyword : keywords) {
                spec = spec.and((root, query, cb) -> cb.like(root.get("question"), "%" + keyword + "%"));
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");

        List<Long> pollIds = getPollIdsBySpecification(spec);
        Page<Poll> filteredPolls = pollRepository.findByIds(pollIds,pageable);
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);
        Map<Long, User> creatorMap = getPollCreatorMap(filteredPolls.getContent());

        List<PollResponse> pollResponses = filteredPolls.map(poll -> {
            return ModelMapper.mapPollToPollResponse(poll,
                    choiceVoteCountMap,
                    creatorMap.get(poll.getCreatedBy()),
                    pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).getContent();
        return new PagedResponse<>(pollResponses, filteredPolls.getNumber(),
                filteredPolls.getSize(), filteredPolls.getTotalElements(), filteredPolls.getTotalPages(), filteredPolls.isLast());
    }

    public Poll createPoll(PollRequest pollRequest) {
        Poll poll = new Poll();
        poll.setQuestion(pollRequest.getQuestion());

        pollRequest.getChoices().forEach(choiceRequest -> {
            poll.addChoice(new Choice(choiceRequest.getText()));
        });

        Instant now = Instant.now();
        Instant expirationDateTime = now.plus(Duration.ofDays(pollRequest.getPollLength().getDays()))
                .plus(Duration.ofHours(pollRequest.getPollLength().getHours()));

        poll.setExpirationDateTime(expirationDateTime);
        logger.info("RESPONSE: Creating poll {} with {} choices with expiration date {}", pollRequest.getQuestion(),
                pollRequest.getChoices().size(), expirationDateTime.toString());
        return pollRepository.save(poll);
    }

    public PollResponse getPollById(Long pollId, UserPrincipal currentUser) {
        Poll poll = pollRepository.findById(pollId).orElseThrow(
                () -> {
                    logger.error("BAD_REQUEST: Poll with id {} not found", pollId);
                    return new ResourceNotFoundException("Poll", "id", pollId);
                });

        // Retrieve Vote Counts of every choice belonging to the current poll
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);

        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        // Retrieve poll creator details
        User creator = userRepository.findById(poll.getCreatedBy())
                .orElseThrow(() -> {
                    logger.error("BAD_REQUEST: User with id {} not found", poll.getCreatedBy());
                    return new ResourceNotFoundException("User", "id", poll.getCreatedBy());
                });

        // Retrieve vote done by logged in user
        Vote userVote = null;
        if (currentUser != null) {
            userVote = voteRepository.findByUserIdAndPollId(currentUser.getId(), pollId);
        }

        logger.info("RESPONSE: Fetching poll with id: " + pollId);
        return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap,
                creator, userVote != null ? userVote.getChoice().getId() : null);
    }

    public PollResponse castVoteAndGetUpdatedPoll(Long pollId, VoteRequest voteRequest, UserPrincipal currentUser) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> {
                    logger.error("BAD_REQUEST: Poll with id {} not found", pollId);
                    return new ResourceNotFoundException("Poll", "id", pollId);
                });

        if (poll.getExpirationDateTime().isBefore(Instant.now())) {
            logger.error("BAD_REQUEST: Poll is already expired");
            throw new BadRequestException("Sorry! This Poll has already expired");
        }

        User user = userRepository.getOne(currentUser.getId());

        Choice selectedChoice = poll.getChoices().stream()
                .filter(choice -> choice.getId().equals(voteRequest.getChoiceId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Choice", "id", voteRequest.getChoiceId()));

        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setUser(user);
        vote.setChoice(selectedChoice);

        try {
            vote = voteRepository.save(vote);
        } catch (DataIntegrityViolationException ex) {
            logger.info("User {} has already voted in Poll {}", currentUser.getId(), pollId);
            throw new BadRequestException("Sorry! You have already cast your vote in this poll");
        }

        // -- Vote Saved, Return the updated Poll Response now --

        // Retrieve Vote Counts of every choice belonging to the current poll
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);

        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        // Retrieve poll creator details
        User creator = userRepository.findById(poll.getCreatedBy())
                .orElseThrow(() -> {
                    logger.info("User not found with id: " + poll.getCreatedBy());
                    return new ResourceNotFoundException("User", "id", poll.getCreatedBy());
                });

        logger.info("RESPONSE: Vote casted as {} for poll {} by {}", vote.getChoice().getText(), pollId,
                user.getUsername());
        return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap, creator, vote.getChoice().getId());
    }

    private void validatePageNumberAndSize(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be less than zero.");
        }

        if (size > AppConstants.MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must not be greater than " + AppConstants.MAX_PAGE_SIZE);
        }
    }

    private Map<Long, Long> getChoiceVoteCountMap(List<Long> pollIds) {
        // Retrieve Vote Counts of every Choice belonging to the given pollIds
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdInGroupByChoiceId(pollIds);

        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        return choiceVotesMap;
    }

    private Map<Long, Long> getPollUserVoteMap(UserPrincipal currentUser, List<Long> pollIds) {
        // Retrieve Votes done by the logged-in user to the given pollIds
        Map<Long, Long> pollUserVoteMap = null;
        if (currentUser != null) {
            List<Vote> userVotes = voteRepository.findByUserIdAndPollIdIn(currentUser.getId(), pollIds);

            pollUserVoteMap = userVotes.stream()
                    .collect(Collectors.toMap(vote -> vote.getPoll().getId(), vote -> vote.getChoice().getId()));
        }
        return pollUserVoteMap;
    }

    Map<Long, User> getPollCreatorMap(List<Poll> polls) {
        // Get Poll Creator details of the given list of polls
        List<Long> creatorIds = polls.stream()
                .map(Poll::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        List<User> creators = userRepository.findByIdIn(creatorIds);
        Map<Long, User> creatorMap = creators.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return creatorMap;
    }

    public List<Long> getPollIdsBySpecification(Specification<Poll> specification) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<Poll> root = criteriaQuery.from(Poll.class);
        criteriaQuery.select(root.get("id")).where(specification.toPredicate(root, criteriaQuery, criteriaBuilder));

        TypedQuery<Long> query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    public void deletePoll(Long pollId, UserPrincipal currentUser) {
        Poll poll = pollRepository.findById(pollId).orElseThrow(
                () -> new ResourceNotFoundException("Poll", "id", pollId));
        List<GrantedAuthority> adminAuthority = currentUser.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().equals("ROLE_ADMIN")).collect(Collectors.toList());
        if (adminAuthority.isEmpty() && currentUser.getId() != poll.getCreatedBy()) {
            throw new UnAuthorizedException("Not authorized");
        }
        pollRepository.deleteById(pollId);
    }

    public Poll editPoll(UserPrincipal currentUser, Long pollId, PollRequest pollRequest) {
        Poll poll = pollRepository.findById(pollId).orElseThrow(
                () -> new ResourceNotFoundException("Poll", "id", pollId));
        List<GrantedAuthority> adminAuthority = currentUser.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().equals("ROLE_ADMIN")).collect(Collectors.toList());
        if (adminAuthority.isEmpty() && currentUser.getId() != poll.getCreatedBy()) {
            throw new UnAuthorizedException("Not authorized");
        }
        poll.setQuestion(pollRequest.getQuestion());
        List<Choice> previousChoices = poll.getChoices();
        List<Choice> updatedChoices = new ArrayList<>();
        pollRequest.getChoices().forEach(choiceRequest -> {
            Choice currentChoice = new Choice(choiceRequest.getText());
            updatedChoices.add(currentChoice);
            currentChoice.setPoll(poll);
        });
        poll.setChoices(updatedChoices);
        previousChoices.stream().forEach(choice -> choiceRepository.deleteById(choice.getId()));
        Instant now = Instant.now();
        Instant expirationDateTime = now.plus(Duration.ofDays(pollRequest.getPollLength().getDays()))
                .plus(Duration.ofHours(pollRequest.getPollLength().getHours()));

        poll.setExpirationDateTime(expirationDateTime);

        return pollRepository.save(poll);
    }
}
