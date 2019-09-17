package com.idaptive.configserver.controller;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import com.idaptive.configserver.service.ConfigService;

@RestController
public class ConfigController {

	@Autowired
	private ConfigService configService;

	@PutMapping("/updateconfig")
	public ResponseEntity<JsonNode> updateUserConfig(@RequestBody JsonNode body, HttpServletRequest request) {
		return configService.updateConfig(body);
	}

	@GetMapping("/getconfig")
	public ResponseEntity<JsonNode> getUserConfig() {
		return configService.getConfig();
	}
	
	@GetMapping("/getclientconfig")
	public ResponseEntity<JsonNode> getClientrConfig() {
		return configService.getClientConfig();
	}
}
