package com.protean.security.aerith.controller;

import com.protean.security.auron.exception.AppException;
import com.protean.security.auron.exception.BadRequestException;
import com.protean.security.auron.model.AppRole;
import com.protean.security.auron.model.RoleName;
import com.protean.security.auron.model.User;
import com.protean.security.auron.payload.LoginRequest;
import com.protean.security.auron.payload.SignUpRequest;
import com.protean.security.auron.repository.RoleRepository;
import com.protean.security.auron.repository.UserRepository;
import com.protean.security.auron.response.BaseResponse;
import com.protean.security.auron.response.JwtAuthenticationResponse;
import com.protean.security.auron.response.StandardResponse;
import com.protean.security.auron.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private AuthenticationManager authenticationManager;

    private UserRepository userRepository;

    private RoleRepository roleRepository;

    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider tokenProvider;

    public UserController(AuthenticationManager authenticationManager, UserRepository userRepository,
                          RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                          JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @GetMapping("/user")
    public ResponseEntity<?> getAllUsers() {
        log.info("Searching for users");
        List<User> users = userRepository.findAll();
        log.info("User records found <" + users.size() + ">");
        StandardResponse<List<User>> userResponse = new StandardResponse<>(HttpStatus.OK, users);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        // Authenticate username and password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        log.info("Responding with successful login response for user: <" + loginRequest.getUsernameOrEmail() + ">");
        return new ResponseEntity<>(new JwtAuthenticationResponse(jwt), HttpStatus.OK);
//        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) throws Exception {
        if(userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new BadRequestException("Username is already taken, " + signUpRequest.getUsername());
        }

        if(userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Email Address already in use: " + signUpRequest.getEmail());
        }

        User user = new User(signUpRequest.getFirstName(), signUpRequest.getMiddleName(),
                signUpRequest.getLastName(), signUpRequest.getUsername(),
                signUpRequest.getEmail(), signUpRequest.getPassword());

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        AppRole userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new AppException("User Role not set."));

        user.setAppRoles(Collections.singleton(userRole));

        User result = userRepository.save(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/users/{username}")
                .buildAndExpand(result.getUsername()).toUri();

        return ResponseEntity.created(location).body(new BaseResponse(HttpStatus.OK.value(), "User registered successfully"));
    }
}
