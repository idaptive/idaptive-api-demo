package com.idaptive.users.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
@RefreshScope
public class UserService {

	Logger logger = LoggerFactory.getLogger(UserService.class);

	@Value("${tenantID}")
	private String tenantID;

	@Value("${tenant}")
	private String tenant;

	@Value("${accesstokenuri}")
	private String accessTokenUri;

	@Value("${clientid}")
	private String clientID;

	@Value("${clientSecret}")
	private String clientSecret;

	@Value("${scope}")
	private String scope;

	@Value("${grantType}")
	private String grantType;

	@Value("${roleName}")
	private String roleName;

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

	private HttpHeaders prepareForRequest(String token) {
		return setHeaders(token);
	}

	public ResponseEntity<JsonNode> createUser(User user) {
		String userJson = "";
		try {
			userJson = getJson(user);
			logger.debug("Inside CreateUser method");
			HttpHeaders headers = prepareForRequestOauth();
			HttpEntity<String> createuserrequest = new HttpEntity<>(userJson, headers);
			String createUserUrl = tenant + "/CDirectoryService/CreateUser";
			String updateRoleUrl = tenant + "/Roles/UpdateRole";
			ResponseEntity<JsonNode> createUserResponse = null;
			if (user.isMfa()) {
				String roleUuid = getRoleUuid(roleName);
				createUserResponse = restTemplate.exchange(createUserUrl, HttpMethod.POST, createuserrequest,
						JsonNode.class);
				if (createUserResponse.getBody().get("Result").isNull()) {
					return createUserResponse;
				} else {
					HttpEntity<String> updateRoleRequest = new HttpEntity<>(
							"{\"Users\":{\"Add\":[\"" + createUserResponse.getBody().get("Result").asText()
									+ "\"]},\"Name\":\"" + roleUuid + "\",\"Description\":\"\"}",
							headers);
					return restTemplate.exchange(updateRoleUrl, HttpMethod.POST, updateRoleRequest, JsonNode.class);
				}
			}
			return restTemplate.exchange(createUserUrl, HttpMethod.POST, createuserrequest, JsonNode.class);
		} catch (JsonProcessingException e) {
			logger.error("Json Formatting Exception");
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

	public ResponseEntity<JsonNode> updateUser(String token, String uuid, User user) throws JsonProcessingException {
		String userJson = getJson(user);
		HttpHeaders headers = prepareForRequest(token);
		ObjectMapper mapper = new ObjectMapper();
		user.setUuid(uuid);
		userJson = mapper.writeValueAsString(user);
		HttpEntity<String> request = new HttpEntity<>(userJson, headers);
		String updateUserUrl = tenant + "/CDirectoryService/ChangeUser";
		String updateRoleUrl = tenant + "/Roles/UpdateRole";
		String roleUuid = null;
		try {
			roleUuid = getRoleUuid(roleName);
			if (user.isMfa()) {
				HttpEntity<String> updateRoleRequest = new HttpEntity<>(
						"{\"Users\":{\"Add\":[\"" + uuid + "\"]},\"Name\":\"" + roleUuid + "\",\"Description\":\"\"}",
						headers);
				restTemplate.exchange(updateRoleUrl, HttpMethod.POST, updateRoleRequest, JsonNode.class);
				return restTemplate.exchange(updateUserUrl, HttpMethod.POST, request, JsonNode.class);
			}
			HttpEntity<String> removeRoleRequest = new HttpEntity<>(
					"{\"Users\":{\"Delete\":[\"" + uuid + "\"]},\"Name\":\"" + roleUuid + "\",\"Description\":\"\"}",
					headers);
			restTemplate.exchange(updateRoleUrl, HttpMethod.POST, removeRoleRequest, JsonNode.class);
			return restTemplate.exchange(updateUserUrl, HttpMethod.POST, request, JsonNode.class);
		} catch (RoleNotFoundException e) {
			return new ResponseEntity<JsonNode>(e.exceptionBody(), HttpStatus.EXPECTATION_FAILED);
		}
	}

	public ResponseEntity<JsonNode> getUser(String uuid, String token) {
		logger.debug("Inside getUser method");
		logger.debug(uuid);
		//HttpHeaders headers = prepareForRequest(token);
		HttpHeaders headers = prepareForRequestOauth();
		HttpEntity<String> request = new HttpEntity<>("{\"ID\":\"" + uuid + "\"}", headers);
		String url = tenant + "/CDirectoryService/GetUser";
		JsonNode response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class).getBody();
		JsonNode result = response.get("Result");
		String name = response.get("Result").get("Name").asText();
		String[] nameArr = name.split("@");
		
		ObjectNode objNode = (ObjectNode) result;
		objNode.remove("Name");
		objNode.put("Name", nameArr[0]);
		objNode.put("MFA", isRolePresent(uuid));
		return new ResponseEntity<JsonNode>(response, HttpStatus.OK);
	}

	public ResponseEntity<JsonNode> getUserInfo(String userId, String token) {
		HttpHeaders headers = prepareForRequest(token);
		HttpEntity<String> request = new HttpEntity<>(headers);
		String url = tenant + "/UserMgmt/GetUserInfo?ID=" + userId;
		return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
	}

	static ArrayList<String> iconList = new ArrayList<>();

	public JsonNode userDashboard(String username, String force) {

		HashMap<String, ArrayList<HashMap<String, String>>> appInfo = new HashMap<String, ArrayList<HashMap<String, String>>>();
		String url = tenant + "/UPRest/GetUPData" + "?" + "force=" + force + "&username=" + username;
		HttpHeaders headers = prepareForRequestOauth();
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

	public List<String> getSuffix() {
		HttpHeaders headers = prepareForRequestOauth();
		HttpEntity<String> request = new HttpEntity<>(headers);
		String url = tenant + "/core/GetCdsAliasesForTenant";
		JsonNode response = restTemplate.postForObject(url, request, JsonNode.class).get("Result").get("Results");
		ArrayList<String> idList = new ArrayList<>();
		for (JsonNode jsonNode : response) {
			idList.add(jsonNode.get("Row").get("ID").asText());
		}
		return idList;
	}

	public JsonNode updateConfig(JsonNode body) {
		try {
			PropertiesConfiguration config = new PropertiesConfiguration("../config-data/user-service.properties");
			Iterator<String> it = body.fieldNames();
			while (it.hasNext()) {
				String fieldName = it.next();
				config.setProperty(fieldName, body.get(fieldName).asText());
			}
			config.save();
			JsonNode response = null;
			String message = "{\"Success\":\"true\",\"Result\":{\"message\":\"Updated user-service configuration file Successfully.\"}}";
			ObjectMapper mapper = new ObjectMapper();
			response = mapper.readTree(message);
			return response;
		} catch (ConfigurationException | IOException e) {
			JsonNode response = null;
			String message = "{\"Success\":\"true\",\"Result\":{\"message\":\"user-service configuration file not updated.\"}}";
			ObjectMapper mapper = new ObjectMapper();
			try {
				response = mapper.readTree(message);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return response;
		}
	}

	public boolean isRolePresent(String uuid) {
		String url = tenant + "/UserMgmt/GetUsersRolesAndAdministrativeRights?id=" + uuid;
		HttpHeaders headers = prepareForRequestOauth();
		HttpEntity<String> request = new HttpEntity<>(headers);
		JsonNode result = restTemplate.postForObject(url, request, JsonNode.class);
		JsonNode arr = result.get("Result").get("Results");
		for (JsonNode jsonNode : arr) {
			String roleName = jsonNode.get("Row").get("RoleName").asText();
			if (roleName.equals("MFA")) {
				return true;
			}
		}
		return false;

	}

}
