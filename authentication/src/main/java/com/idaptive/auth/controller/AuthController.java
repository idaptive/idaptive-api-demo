package com.idaptive.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.idaptive.auth.service.AuthService;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

	@Autowired
	private AuthService authService;

	private Logger logger = LoggerFactory.getLogger(AuthController.class);
	
	@PostMapping("/beginAuth")
	public ResponseEntity<JsonNode> loginUsername(@RequestBody JsonNode authRequest) {
		logger.info("AuthRquest is as follow: -");
		return this.authService.startAuthenticationWithObject(authRequest);
	}

	@PostMapping("/advanceAuth")
	public ResponseEntity<JsonNode> advAuth(@RequestBody JsonNode advAuthRequest,
			HttpServletResponse httpServletResponse) {
		return this.authService.advanceAuthenticationByObject(advAuthRequest);
	}


	@PostMapping("/out")
	public ResponseEntity<JsonNode> logout(HttpServletRequest request) {
		Cookie[] cookieArray = request.getCookies();
		for (Cookie cookie : cookieArray) {
			if (cookie.getName().equals(".ASPXAUTH")) {
				return this.authService.logout(cookie.getValue());
			}

		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);

	}

	@GetMapping({ "/socialLogin/{idpName}" })
	public ResponseEntity<JsonNode> socialLogin(@PathVariable String idpName) {
		return this.authService.socialLogin(idpName);
	}

	@GetMapping({ "/socialLogin" })
	public ResponseEntity<JsonNode> socialLoginResult(@RequestParam String ExtIdpAuthChallengeState,
			@RequestParam String username, @RequestParam String customerId, HttpServletResponse httpServletResponse) {
		return authService.socialLoginResult(ExtIdpAuthChallengeState, username, customerId);

	}
}