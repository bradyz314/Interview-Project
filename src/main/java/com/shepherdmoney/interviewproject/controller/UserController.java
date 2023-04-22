package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    private UserRepository users;

    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // Create a new user based on the payload information
        User newUser = new User(payload.getName(), payload.getEmail());
        // Save the user to the users repository
        users.save(newUser);
        // Return an OK response with the user's id
        return ResponseEntity.ok(newUser.getId());
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        if (users.existsById(userId)) {
            // User exists. Delete them from the repository
            users.deleteById(userId);
            // Return an OK response
            return ResponseEntity
                .status(HttpStatus.OK)
                .body("Successfully deleted user");
        } else {
            // User does not exist. Return a BAD_REQUEST response
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("User doesn't exist");
        }
    }
}
