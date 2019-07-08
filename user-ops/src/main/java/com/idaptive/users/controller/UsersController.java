package com.idaptive.users.controller;

import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.idaptive.users.entity.User;
import com.idaptive.users.service.UserService;


@RestController
public class UsersController {

	
	Logger logger = LoggerFactory.getLogger(UsersController.class);

	@Autowired
	private UserService userService;

	@PostMapping("/")
	public ResponseEntity<JsonNode> createUser(HttpServletRequest request, @RequestBody User user) {
		return userService.createUser(user);
	}

	@PutMapping("/{uuid}")
	public ResponseEntity<JsonNode> updateUser(HttpServletRequest request, @RequestBody User user,
			@PathVariable String uuid) throws JsonProcessingException {
		Cookie[] cookieArray = request.getCookies();
		for (Cookie cookie : cookieArray) {
			if (cookie.getName().equals(".ASPXAUTH")) {
				return userService.updateUser(cookie.getValue(), uuid, user);
			}
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@GetMapping("/{uuid}")
	public ResponseEntity<JsonNode> getUser(HttpServletRequest request, @PathVariable String uuid) {
		Cookie[] cookieArray = request.getCookies();
		for (Cookie cookie : cookieArray) {
			if (cookie.getName().equals(".ASPXAUTH")) {
				return userService.getUser(uuid, cookie.getValue());
			}
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	
	@GetMapping("/dashboard")
	public JsonNode dashboard(@RequestParam String username, @RequestParam String force, HttpServletRequest request) {
		return userService.userDashboard(username, force);
	}

	@GetMapping("/info/{uuid}")
	public ResponseEntity<JsonNode> userInfo(@PathVariable String uuid, HttpServletRequest request) {
		Cookie[] cookieArray = request.getCookies();
		for (Cookie cookie : cookieArray) {
			if (cookie.getName().equals(".ASPXAUTH")) {
				return userService.getUserInfo(uuid, cookie.getValue());
			}
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);

	}
	
	
	@GetMapping("/suffix")
	public List<String> getSuffix() {
		return userService.getSuffix();
	}
	
	
	@PutMapping("/userconfig")
	public JsonNode updateConfig(@RequestBody JsonNode body) {	
		return userService.updateConfig(body);
	}
	
	
}
