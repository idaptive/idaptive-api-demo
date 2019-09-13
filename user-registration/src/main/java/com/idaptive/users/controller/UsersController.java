package com.idaptive.users.controller;


import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import com.idaptive.users.entity.User;
import com.idaptive.users.service.UserService;

@RestController
public class UsersController {
	
	Logger logger = LoggerFactory.getLogger(UsersController.class);

	@Autowired
	private UserService userService;

	@PostMapping("/register")
	public ResponseEntity<JsonNode> createUser(HttpServletRequest request, @RequestBody User user) {
		return userService.createUser(user);
	}
	
	@GetMapping("/getclientconfig")
	public ResponseEntity<JsonNode> getCustomProperties(){
		return userService.getConfig();
	}
	
	@PostMapping("/refresh")
	public ResponseEntity<JsonNode> refreshConfig() {
		return userService.refreshConfig();
	}
}
