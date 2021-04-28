package com.idaptive.users.service;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.idaptive.users.entity.User;
import com.idaptive.users.exception.RoleNotFoundException;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;

@Service
@RefreshScope
public class UserService {

	Logger logger = LoggerFactory.getLogger(UserService.class);

	@Value("${customerId}")
	private String tenantID;

	@Value("${tenant}")
	private String tenant;

	@Value("${oauthAppId}")
	private String applicationID;

	@Value("${oauthUser}")
	private String clientID;

	@Value("${oauthPassword}")
	private String clientSecret;

	@Value("${scope}")
	private String scope;

	@Value("${grantType}")
	private String grantType;

	@Value("${mfaRole}")
	private String roleName;

	@Value("${spring.cloud.config.username}")
	private String configUsername;

	@Value("${spring.cloud.config.password}")
	private String configPassword;

	@Autowired
	private EurekaClient eurekaClient;

	@LoadBalanced
	private final RestTemplate restTemplate;

	public UserService(RestTemplateBuilder builder) {
		this.restTemplate = builder.build();
	}

	private String getJson(User user) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String name = user.getName();
		user.setName(name + "@" + tenantID);
		return mapper.writeValueAsString(user);
	}

	private String receiveOAuthTokenCC() {
		ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
		details.setAccessTokenUri(tenant + "/oauth2/token/" + applicationID);
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

	public ResponseEntity<JsonNode> createUser(User user) {
		String userJson = "";
		try {
			user.setSendEmailInvite(true);
			userJson = getJson(user);
			HttpHeaders headers = prepareForRequestOauth();
			HttpEntity<String> createuserrequest = new HttpEntity<>(userJson, headers);
			String createUserUrl = tenant + "/CDirectoryService/Signup";
			String updateRoleUrl = tenant + "/Roles/UpdateRole";
			ResponseEntity<JsonNode> createUserResponse = null;
			createUserResponse = restTemplate.exchange(createUserUrl, HttpMethod.POST, createuserrequest,
					JsonNode.class);
			StringBuffer message = new StringBuffer("User name " + user.getName() + " is already in use.");
			if (createUserResponse.getBody().get("Result").isNull()) {
				if (createUserResponse.getBody().get("Message").asText().contentEquals(message)) {
					JsonNode createUserResponseBody = createUserResponse.getBody();
					ObjectNode objNode = (ObjectNode) createUserResponseBody;
					objNode.remove("Message");
					objNode.put("Message", "User name " + user.getName().split("@")[0] + " is already in use.");
					return createUserResponse;
				}
			} else {
				if (user.isMfa()) {
					String roleUuid = getRoleUuid(roleName);

					HttpEntity<String> updateRoleRequest = new HttpEntity<>(
							"{\"Users\":{\"Add\":[\"" + createUserResponse.getBody().get("Result").get("UserId").asText()
									+ "\"]},\"Name\":\"" + roleUuid + "\",\"Description\":\"\"}",
							headers);
					restTemplate.exchange(updateRoleUrl, HttpMethod.POST, updateRoleRequest, JsonNode.class);
				}
				return createUserResponse;
			}
			return createUserResponse;
		} catch (JsonProcessingException e) {
			return new ResponseEntity<JsonNode>(HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (RoleNotFoundException e) {
			return new ResponseEntity<JsonNode>(e.exceptionBody(), HttpStatus.EXPECTATION_FAILED);
		}
	}

	public String getRoleUuid(String roleName) throws RoleNotFoundException {
		String getRoles = tenant + "/Redrock/query";
		HttpHeaders headers = prepareForRequestOauth();
		HttpEntity<String> getRolesRequest = new HttpEntity<>(
				"{ Script: \"Select * from Role WHERE Name = \'" + roleName + "\' ORDER BY Name COLLATE NOCASE \"}",
				headers);
		ResponseEntity<JsonNode> getRoleInfo = restTemplate.exchange(getRoles, HttpMethod.POST, getRolesRequest,
				JsonNode.class);
		JsonNode node = getRoleInfo.getBody().get("Result").get("Results");
		String roleUuid = null;
		if (node.size() == 1) {
			for (JsonNode objNode : node) {
				if (objNode.has("Row")) {
					return objNode.get("Row").get("ID").asText();
				}
			}
		} else {
			throw new RoleNotFoundException(roleName);
		}
		return roleUuid;
	}

	public ResponseEntity<JsonNode> getConfig() {
		String plainCreds = configUsername + ":" + configPassword;
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + base64Creds);
		HttpEntity request = new HttpEntity(headers);
		Application application = eurekaClient.getApplication("config-server");
		String url = "http://" + application.getInstances().get(0).getIPAddr() + ":"
				+ application.getInstances().get(0).getPort() + "/getclientconfig";
		return restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);

	}
	
	public ResponseEntity<JsonNode> refreshConfig() {
		HttpHeaders headers = new HttpHeaders();
		HttpEntity request = new HttpEntity(headers);
		Application userOpsApplication = eurekaClient.getApplication("user-ops-service");
		Application authApplication = eurekaClient.getApplication("auth-service");

		String userOpsUrl = "http://" + userOpsApplication.getInstances().get(0).getIPAddr() + ":"
				+ userOpsApplication.getInstances().get(0).getPort() + "/actuator/refresh";
		String authUrl = "http://" + authApplication.getInstances().get(0).getIPAddr() + ":"
				+ authApplication.getInstances().get(0).getPort() + "/actuator/refresh";
		restTemplate.exchange(userOpsUrl, HttpMethod.POST, request, JsonNode.class);
		return restTemplate.exchange(authUrl, HttpMethod.POST, request, JsonNode.class);

	}


}
