package com.idaptive.auth.service;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class AuthService {

	@Value("${tenant}")
	String tenantPrefix;

	@Value("${callbackurl}")
	String callBackUrl;

	@Value("${podurl}")
	String podUrl;


	@LoadBalanced
	private final RestTemplate restTemplate;

	public AuthService(RestTemplateBuilder builder) {
		this.restTemplate = builder.build();
	}

	public ResponseEntity<JsonNode> startAuthenticationWithObject(JsonNode authRequest) {
		String tenant = tenantPrefix + "/Security/StartAuthentication";
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.set("x-centrify-native-client", "true");
		httpHeaders.set("content-type", "application/json");
		HttpEntity<JsonNode> request = new HttpEntity<>(authRequest);
		boolean success = false;
		ResponseEntity<JsonNode> responseObj = null;
		do {
			ResponseEntity<JsonNode> idaptiveResponse = restTemplate.exchange(tenant, HttpMethod.POST, request,
					JsonNode.class);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set("content-type", "application/json");
			JsonNode result = (JsonNode) idaptiveResponse.getBody();
			if (result.get("success").asBoolean()) {
				String auth = result.get("Result").has("Auth") ? result.get("Result").get("Auth").asText() : "";

				if (auth.length() == 0) {
					success = true;
					responseObj = new ResponseEntity<JsonNode>(idaptiveResponse.getBody(), responseHeaders,
							HttpStatus.OK);
				} else {

					logout(auth);
				}
			} else {
				success = true;
				responseObj = new ResponseEntity<JsonNode>(idaptiveResponse.getBody(), responseHeaders,
						HttpStatus.BAD_REQUEST);
			}
		} while (!success);
		return responseObj;
	}

	public ResponseEntity<JsonNode> advanceAuthenticationByObject(JsonNode authRequest) {
		String url = tenantPrefix + "/Security/AdvanceAuthentication";
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("x-centrify-native-client", "true");
		httpHeaders.set("content-type", "application/json");
		HttpEntity<JsonNode> request = new HttpEntity<>(authRequest);
		return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);

	}

	private HttpHeaders setHeaders(String token) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("x-centrify-native-client", "true");
		httpHeaders.set("content-type", "application/json");
		httpHeaders.set("cache-control", "no-cache");
		httpHeaders.set("Authorization", "Bearer " + token);
		return httpHeaders;
	}

	public ResponseEntity<JsonNode> logout(String authToken) {
		String tenant = tenantPrefix + "/Security/Logout";
		HttpHeaders headers = setHeaders(authToken);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		HttpEntity<String> request = new HttpEntity(headers);
		this.restTemplate.acceptHeaderRequestCallback(String.class);
		return this.restTemplate.exchange(tenant, HttpMethod.POST, request, JsonNode.class);
		
	}

	public ResponseEntity<JsonNode> resetUserPasswordUser(JsonNode requestBody, String authToken) {
		String url = tenantPrefix + "/UserMgmt/ResetUserPassword";
		HttpHeaders headers = setHeaders(authToken);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		HttpEntity<String> request = new HttpEntity(requestBody, headers);
		this.restTemplate.acceptHeaderRequestCallback(String.class);
		ResponseEntity<JsonNode> result = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
		return result;
	}

	public ResponseEntity<JsonNode> socialLogin(String idpName) {
		String url = tenantPrefix + "/Security/StartSocialAuthentication";
		HashMap<String, String> hashmap = new HashMap<>();
		hashmap.put("IdpName", idpName);
		hashmap.put("PostExtIdpAuthCallbackUrl", callBackUrl);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("x-centrify-native-client", "Web");
		httpHeaders.set("content-type", "application/json");
		HttpEntity<String> request = new HttpEntity(hashmap, httpHeaders);
		return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);

	}

	public ResponseEntity<JsonNode> socialLoginResult(String ExtIdpAuthChallengeState, String username,
			String customerId) {
		String url = podUrl + ExtIdpAuthChallengeState + "&username=" + username + "&customerId=" + customerId;
		HttpHeaders httpHeaders = new HttpHeaders();
		HttpEntity<String> request = new HttpEntity<String>(httpHeaders);
		return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);

	}

}
