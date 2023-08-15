package com.example.polls.controller;

import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.User;
import com.example.polls.payload.*;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.security.UserPrincipal;
import com.example.polls.service.PollService;
import com.example.polls.security.CurrentUser;
import com.example.polls.service.UserService;
import com.example.polls.util.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private PollService pollService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/user/me")
    @PreAuthorize("hasRole('USER')")
    public UserSummary getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        return new UserSummary(currentUser.getId(), currentUser.getUsername(), currentUser.getName(), currentUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray(String[]::new));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserProfile> getAllUserProfile() {
        List<User> users = userRepository.findAll();
        List<UserProfile> userProfileList = users.stream().map(user -> {
            long pollCount = pollRepository.countByCreatedBy(user.getId());
            long voteCount = voteRepository.countByUserId(user.getId());
            return new UserProfile(user.getId(), user.getUsername(), user.getName(), user.getCreatedAt(), pollCount, voteCount);
        }).collect(Collectors.toList());
        return userProfileList;
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editUser(@PathVariable Long userId, @Valid @RequestBody UserSummary userSummary){
          User user = userRepository.findById(userId)
                  .orElseThrow(() -> new ResourceNotFoundException("User","id",userId));

        if(userRepository.existsByUsername(userSummary.getUsername())) {
            return new ResponseEntity(new ApiResponse(false, "Username is already taken!"),
                    HttpStatus.BAD_REQUEST);
        }
        if(userSummary == null || userSummary.getUsername() == null || userSummary.getName() == null){
            return new ResponseEntity(new ApiResponse(false, "Required fields missing"),
                    HttpStatus.BAD_REQUEST);
        }

          user.setUsername(userSummary.getUsername());
          user.setName(userSummary.getName());
          userRepository.save(user);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/users/{userId}")
                        .buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(location)
                .body(new ApiResponse(true, "User Edited Successfully"));
    }

    @GetMapping("/user/checkUsernameAvailability")
    public UserIdentityAvailability checkUsernameAvailability(@RequestParam(value = "username") String username) {
        logger.info("QUERY: checking for username availability {}",username);
        Boolean isAvailable = !userRepository.existsByUsername(username);
        return new UserIdentityAvailability(isAvailable);
    }

    @GetMapping("/user/checkEmailAvailability")
    public UserIdentityAvailability checkEmailAvailability(@RequestParam(value = "email") String email) {
        logger.info("QUERY: checking for email availability {}",email);
        Boolean isAvailable = !userRepository.existsByEmail(email);
        return new UserIdentityAvailability(isAvailable);
    }

    @GetMapping("/users/{username}")
    public UserProfile getUserProfile(@PathVariable(value = "username") String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        long pollCount = pollRepository.countByCreatedBy(user.getId());
        long voteCount = voteRepository.countByUserId(user.getId());

        return new UserProfile(user.getId(), user.getUsername(), user.getName(), user.getCreatedAt(), pollCount, voteCount);
    }

    @GetMapping("/users/{username}/polls")
    public PagedResponse<PollResponse> getPollsCreatedBy(@PathVariable(value = "username") String username,
                                                         @CurrentUser UserPrincipal currentUser,
                                                         @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
                                                         @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        logger.info("REQUEST: fetching polls created by {}",username);
        return pollService.getPollsCreatedBy(username, currentUser, page, size);
    }

    @GetMapping("/users/{username}/votes")
    public PagedResponse<PollResponse> getPollsVotedBy(@PathVariable(value = "username") String username,
                                                       @CurrentUser UserPrincipal currentUser,
                                                       @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
                                                       @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        logger.info("REQUEST: fetching polls voted by {}",username);
        return pollService.getPollsVotedBy(username, currentUser, page, size);
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable(value = "userId") Long userId){
        try{
            userService.deleteUser(userId);
            return ResponseEntity.ok("Deleted the user");
        }
        catch (Exception e){
            if(e instanceof ResourceNotFoundException){
                return new ResponseEntity<>("Error deleting user as user not found",HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.internalServerError().body("Error deleting the the user");
        }
    }

}
