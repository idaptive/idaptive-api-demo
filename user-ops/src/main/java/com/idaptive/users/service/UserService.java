package com.idaptive.users.service;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idaptive.users.entity.User;

@Service
public class UserService {

	Logger logger = LoggerFactory.getLogger(UserService.class);
	 @Value("${tenant}")
	 private String tenant;
	 
	 @Value("${accesstokenuri}")
	 private String  accessTokenUri;
	 
	 @Value("${clientid}")
	 private String  clientID;
	 
	 @Value("${clientSecret}")
	 private String  clientSecret;
	 
	 @Value("${scope}")
	 private String  scope;
	 
	 @Value("${grantType}")
	 private String  grantType;
	 
	
	@LoadBalanced
	private final RestTemplate restTemplate;

	public UserService(RestTemplateBuilder builder) {
		this.restTemplate = builder.build();
	}

	private String getJson(User user) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(user);
	}

	private String receiveOAuthTokenCC() {
		ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
		details.setAccessTokenUri(accessTokenUri);
		details.setClientId(clientID);
		details.setClientSecret(clientSecret);
		details.setScope(Arrays.asList(scope));
		details.setGrantType(grantType);
		OAuth2RestTemplate template = new OAuth2RestTemplate(details);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		OAuth2AccessToken token = template.getAccessToken();
		return token.getValue();
	}

	private HttpHeaders setHeaders(String token) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("x-centrify-native-client", "true");
		httpHeaders.set("content-type", "application/json");
		httpHeaders.set("cache-control", "no-cache");
		httpHeaders.set("Authorization", "Bearer " + token);
		return httpHeaders;
	}

	private HttpHeaders prepareForRequestOauth() {
		String token = receiveOAuthTokenCC();		 
		return setHeaders(token);
	}
	
	private HttpHeaders prepareForRequest(String token ) {	
		return setHeaders(token);
	}
	

	public ResponseEntity<JsonNode> createUser(User user) {
		String userJson = "";
		try {
			userJson = getJson(user);
			logger.debug("Inside CreateUser method");
			HttpHeaders headers = prepareForRequestOauth();
			HttpEntity<String> request = new HttpEntity<>(userJson, headers);
			String url = tenant + "/CDirectoryService/CreateUser";
			return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);

		} catch (JsonProcessingException e) {
			logger.error("Json Formatting Exception");
			return new ResponseEntity<JsonNode>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}



	public ResponseEntity<JsonNode> updateUser(String token,String uuid, User user) throws JsonProcessingException {
		HttpHeaders headers = prepareForRequest(token);
		ObjectMapper mapper = new ObjectMapper();
		user.setUuid(uuid);
		String userJson = mapper.writeValueAsString(user);
		HttpEntity<String> request = new HttpEntity<>(userJson, headers);
		String url = tenant + "/CDirectoryService/ChangeUser";
		return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
	}

	public ResponseEntity<JsonNode> getUser(String uuid,String token) {
		logger.debug("Inside getUser method");
		logger.debug(uuid);
		HttpHeaders headers = prepareForRequest(token);
		HttpEntity<String> request = new HttpEntity<>("{\"ID\":\"" + uuid + "\"}", headers);
		String url = tenant + "/CDirectoryService/GetUser";
		return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
	}

	
	public ResponseEntity<JsonNode> getUserInfo(String userId,String token) {
		HttpHeaders headers = prepareForRequest(token);
		HttpEntity<String> request = new HttpEntity<>(headers);
		String url = tenant + "/UserMgmt/GetUserInfo?ID="+userId;
		return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);		
	}
	

	static ArrayList<String> iconList = new ArrayList<>();
	public JsonNode userDashboard(String username,String force) {

		HashMap<String, ArrayList<HashMap<String, String>>> appInfo = new HashMap<String, ArrayList<HashMap<String, String>>>();
		String url = tenant + "/UPRest/GetUPData" + "?" + "force=" +force + "&username="
				+ username;
		HttpHeaders headers=prepareForRequestOauth();
		HttpEntity<String> request = new HttpEntity<>(headers);
		JsonNode result = restTemplate.postForObject(url, request, JsonNode.class);
		JsonNode arrNode = result.get("Result").get("Apps");
		if (arrNode.isArray()) {
			for (final JsonNode objNode : arrNode) {
				ArrayList<HashMap<String, String>> appList = new ArrayList<HashMap<String, String>>();
				HashMap<String, String> map = new HashMap<>();
				map.put("Icon", objNode.get("Icon").asText());
				iconList.add(objNode.get("Icon").asText());
				map.put("AppKey", objNode.get("AppKey").asText());
				appList.add(map);
				appInfo.put(objNode.get("Name").asText(), appList);
			}

		}
		
		ObjectMapper mapper = new ObjectMapper();
		return mapper.convertValue(appInfo, JsonNode.class);
		
	}	
	
}
