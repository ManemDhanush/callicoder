package com.example.polls.controller;

import com.example.polls.model.*;
import com.example.polls.payload.*;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.security.CurrentUser;
import com.example.polls.security.UserPrincipal;
import com.example.polls.service.PollService;
import com.example.polls.util.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import javax.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/polls")
public class PollController {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PollService pollService;

    private static final Logger logger = LoggerFactory.getLogger(PollController.class);

    @GetMapping
    public PagedResponse<PollResponse> getAllOrFilteredPolls(
             @RequestParam(value = "date",required = false)@DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate date,
             @RequestParam(value = "startDate",required = false)@DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate startDate,
             @RequestParam(value = "endDate",required = false)@DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate endDate,
             @RequestParam(value = "keywords",required = false) List<String> keywords,
             @CurrentUser UserPrincipal currentUser,
             @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
             @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        if(date == null && keywords == null && startDate == null && endDate == null){
            logger.info("REQUEST: Fetch all polls requested by " + (currentUser!=null ? currentUser.getName() : "not logged in User"));
            return pollService.getAllPolls(currentUser, page, size);
        } else {
            logger.info("REQUEST: Fetch filtered polls requested by " + (currentUser!=null ? currentUser.getName() : "not logged in User"));
            return pollService.filterPolls(date, startDate, endDate, keywords, currentUser,page, size);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('POLLSTER')")
    public ResponseEntity<?> createPoll(@Valid @RequestBody PollRequest pollRequest) {
        logger.info("REQUEST: Create Poll {} with {} choices",pollRequest.getQuestion(),pollRequest.getChoices().size());
        Poll poll = pollService.createPoll(pollRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{pollId}")
                .buildAndExpand(poll.getId()).toUri();

        return ResponseEntity.created(location)
                .body(new ApiResponse(true, "Poll Created Successfully"));
    }

    @PutMapping("/{pollId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> editPoll(@CurrentUser UserPrincipal currentUser, @PathVariable Long pollId, @Valid @RequestBody PollRequest pollRequest) {
        Poll poll = pollService.editPoll(currentUser, pollId, pollRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{pollId}")
                .buildAndExpand(poll.getId()).toUri();

        return ResponseEntity.created(location)
                .body(new ApiResponse(true, "Poll Edited Successfully"));
    }

    @DeleteMapping("/{pollId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void deletePoll(@CurrentUser UserPrincipal currentUser,
                           @PathVariable Long pollId) {
        pollService.deletePoll(pollId, currentUser);
    }

    @GetMapping("/{pollId}")
    public PollResponse getPollById(@CurrentUser UserPrincipal currentUser,
                                    @PathVariable Long pollId) {
        logger.info("REQUEST: fetch Poll by {} requested by {}",pollId,currentUser.getUsername());
        return pollService.getPollById(pollId, currentUser);
    }

    @PostMapping("/{pollId}/votes")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('POLLSTER')")
    public PollResponse castVote(@CurrentUser UserPrincipal currentUser,
                         @PathVariable Long pollId,
                         @Valid @RequestBody VoteRequest voteRequest) {
        logger.info("REQUEST: cast vote for Poll {} requested by {}",pollId,currentUser.getUsername());
        return pollService.castVoteAndGetUpdatedPoll(pollId, voteRequest, currentUser);
    }
}
