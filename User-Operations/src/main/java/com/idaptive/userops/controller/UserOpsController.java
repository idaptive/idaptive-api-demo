package com.idaptive.userops.controller;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.idaptive.userops.entity.User;
import com.idaptive.userops.service.UserOpsService;

@RestController
public class UserOpsController {

	Logger logger = LoggerFactory.getLogger(UserOpsController.class);

	@Autowired
	private UserOpsService userOpsService;

	@PutMapping("/{uuid}")
	public ResponseEntity<JsonNode> updateUser(HttpServletRequest request, @RequestBody User user,
			@PathVariable String uuid) throws JsonProcessingException {
		Cookie[] cookieArray = request.getCookies();
		for (Cookie cookie : cookieArray) {
			if (cookie.getName().equals(".ASPXAUTH")) {
				return userOpsService.updateUser(cookie.getValue(), uuid, user);
			}
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@GetMapping("/{uuid}")
	public ResponseEntity<JsonNode> getUser(HttpServletRequest request, @PathVariable String uuid) {
		Cookie[] cookieArray = request.getCookies();
		for (Cookie cookie : cookieArray) {
			if (cookie.getName().equals(".ASPXAUTH")) {
				return userOpsService.getUser(uuid, cookie.getValue());
			}
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@GetMapping("/dashboard")
	public ResponseEntity<JsonNode> dashboard(@RequestParam String username, @RequestParam String force,
			HttpServletRequest request) {
		Cookie[] cookieArray = request.getCookies();
		for (Cookie cookie : cookieArray) {
			if (cookie.getName().equals(".ASPXAUTH")) {
				return userOpsService.userDashboard(username, force, cookie.getValue());
			}
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);

	}

	@GetMapping("/info/{uuid}")
	public ResponseEntity<JsonNode> userInfo(@PathVariable String uuid, HttpServletRequest request) {
		Cookie[] cookieArray = request.getCookies();
		for (Cookie cookie : cookieArray) {
			if (cookie.getName().equals(".ASPXAUTH")) {
				return userOpsService.getUserInfo(uuid, cookie.getValue());
			}
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);

	}

	@PutMapping("/updateconfig")
	public ResponseEntity<JsonNode> updateConfig(@RequestBody JsonNode body) {
		return userOpsService.updateConfig(body);
	}
	
	@GetMapping("/getconfig")
	public ResponseEntity<JsonNode> getCustomProperties(){
		return userOpsService.getConfig();
	}
	
	
	
	
	
}
