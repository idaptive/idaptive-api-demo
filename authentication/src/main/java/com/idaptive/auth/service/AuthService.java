package com.idaptive.auth.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.idaptive.auth.entity.AuthRequest;
import java.security.Key;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.crypto.spec.SecretKeySpec;

@RefreshScope
@Service
public class AuthService {

	@Value("${tenant}")
	private String tenantPrefix;

	@Value("${callbackurl}")
	private String callBackUrl;

	@Value("${podurl}")
	private String podUrl;

	@Value("${customerId}")
	private String tenantID;

	@Value("${jwtKey}")
	private String jwtKey;

	@LoadBalanced
	private final RestTemplate restTemplate;

	public AuthService(RestTemplateBuilder builder) {
		this.restTemplate = builder.build();
	}

	private HttpHeaders setHeaders() {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("x-centrify-native-client", "true");
		httpHeaders.set("content-type", "application/json");
		httpHeaders.set("cache-control", "no-cache");
		return httpHeaders;
	}

	public ResponseEntity<JsonNode> startAuthenticationWithObject(AuthRequest authRequest, HttpServletResponse response)
			throws JsonProcessingException {
		String tenant = tenantPrefix + "/Security/StartAuthentication";
		HttpHeaders httpHeaders = setHeaders();
		ObjectMapper mapper = new ObjectMapper();
		String name = authRequest.getUsername();
		authRequest.setUsername(name + "@" + tenantID);
		String beginAuth = mapper.writeValueAsString(authRequest);
		HttpEntity<String> request = new HttpEntity<>(beginAuth, httpHeaders);
		boolean success = false;
		ResponseEntity<JsonNode> responseObj = null;
		do {
			ResponseEntity<JsonNode> idaptiveResponse = restTemplate.exchange(tenant, HttpMethod.POST, request,
					JsonNode.class);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set("content-type", "application/json");
			JsonNode result = idaptiveResponse.getBody();
			if (result.get("success").asBoolean()) {
				String auth = result.get("Result").has("Auth") ? result.get("Result").get("Auth").asText() : "";
				if (auth.length() == 0) {
					success = true;
					responseObj = new ResponseEntity<>(idaptiveResponse.getBody(), responseHeaders, HttpStatus.OK);
				} else {
					logout(auth, response);
				}
			} else {
				success = true;
				responseObj = new ResponseEntity<>(idaptiveResponse.getBody(), responseHeaders, HttpStatus.BAD_REQUEST);
			}
		} while (!success);
		return responseObj;
	}

	public ResponseEntity<JsonNode> advanceAuthenticationByObject(JsonNode authRequest, HttpServletResponse response) {
		String url = tenantPrefix + "/Security/AdvanceAuthentication";
		HttpHeaders httpHeaders = setHeaders();
		HttpEntity<JsonNode> request = new HttpEntity<>(authRequest, httpHeaders);
		ResponseEntity<JsonNode> advAuthResp = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
		JsonNode advAuthBody = advAuthResp.getBody();

		HttpHeaders advAuthHeader = advAuthResp.getHeaders();
		if (advAuthResp.getBody().get("Result").has("UserId")) {
			String uuid = advAuthResp.getBody().get("Result").get("UserId").asText();
			String token = null;
			for (String value : advAuthHeader.get("Set-Cookie")) {
				if (value.split(";")[0].split("=")[0].equals(".ASPXAUTH")) {
					token = value.split(";")[0].split("=")[1];
					Cookie aspxauthCookie = new Cookie(".ASPXAUTH", token);
					aspxauthCookie.setHttpOnly(true);
					aspxauthCookie.setSecure(true);
					aspxauthCookie.setPath("/");
			//		aspxauthCookie.setDomain("idaptive.app");
					response.addCookie(aspxauthCookie);
				}
			}
			boolean access = hasUpdateAccess(uuid, token);
			ObjectNode objNode = (ObjectNode) advAuthBody.get("Result");
			objNode.put("Custom", access);
			String jwt = doGenerateToken(advAuthResp.getBody().get("Result").get("UserId").asText());
			Cookie cookie = new Cookie("JwtToken", jwt);
			cookie.setHttpOnly(true);
			cookie.setSecure(true);
			cookie.setPath("/");
		//	cookie.setDomain("idaptive.app");
			response.addCookie(cookie);
			return new ResponseEntity<JsonNode>(advAuthBody, advAuthHeader, HttpStatus.OK);
		} else {
			return advAuthResp;
		}

	}

	private HttpHeaders setHeaders(String token) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("x-centrify-native-client", "true");
		httpHeaders.set("content-type", "application/json");
		httpHeaders.set("cache-control", "no-cache");
		httpHeaders.set("Authorization", "Bearer " + token);
		return httpHeaders;
	}

	public boolean hasUpdateAccess(String uuid, String token) {
		String url = tenantPrefix + "/UserMgmt/GetUsersRolesAndAdministrativeRights?id=" + uuid;
		HttpHeaders headers = setHeaders(token);
		HttpEntity<String> request = new HttpEntity<>(headers);
		JsonNode result = restTemplate.postForObject(url, request, JsonNode.class);
		JsonNode arr = result.get("Result").get("Results");
		for (JsonNode jsonNode : arr) {
			JsonNode administrativeRightsArr = jsonNode.get("Row").get("AdministrativeRights");
			for (JsonNode jsonNode2 : administrativeRightsArr) {
				if (jsonNode2.get("Description").asText().equals("Admin Portal Login")) {
					return true;
				}
			}
		}

		return false;
	}

	public ResponseEntity<JsonNode> logout(String authToken, HttpServletResponse respose) {
		String tenant = tenantPrefix + "/Security/Logout";
		HttpHeaders headers = setHeaders();
		headers.set("Authorization", "Bearer " + authToken);
		HttpEntity<String> request = new HttpEntity<>(headers);
		Cookie cookieJwt = new Cookie("JwtToken", null);
		cookieJwt.setPath("/");
		cookieJwt.setHttpOnly(true);
		cookieJwt.setMaxAge(0);
	//	cookieJwt.setDomain("idaptive.app");
		respose.addCookie(cookieJwt);
		Cookie cookie = new Cookie(".ASPXAUTH", null);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setMaxAge(0);
	//	cookie.setDomain("idaptive.app");
		respose.addCookie(cookie);
		return this.restTemplate.exchange(tenant, HttpMethod.POST, request, JsonNode.class);
	}

	public ResponseEntity<JsonNode> resetUserPasswordUser(JsonNode requestBody, String authToken) {
		String url = tenantPrefix + "/UserMgmt/ResetUserPassword";
		HttpHeaders headers = setHeaders();
		headers.set("Authorization", "Bearer " + authToken);
		HttpEntity<JsonNode> request = new HttpEntity<>(requestBody, headers);
		return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
	}

	public ResponseEntity<JsonNode> socialLogin(String idpName) {
		String url = tenantPrefix + "/Security/StartSocialAuthentication";
		HashMap<String, String> body = new HashMap<>();
		body.put("IdpName", idpName);
		body.put("PostExtIdpAuthCallbackUrl", callBackUrl);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("x-centrify-native-client", "Web");
		httpHeaders.set("content-type", "application/json");
		HttpEntity<HashMap<String, String>> request = new HttpEntity<>(body, httpHeaders);
		return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
	}

	public ResponseEntity<JsonNode> socialLoginResult(String extIdpAuthChallengeState, String username,
			String customerId, HttpServletResponse response) {
		String url = podUrl + extIdpAuthChallengeState + "&username=" + username + "&customerId=" + customerId;
		HttpHeaders httpHeaders = new HttpHeaders();
		HttpEntity<String> request = new HttpEntity<>(httpHeaders);
		ResponseEntity<JsonNode> result = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
		if (result.getBody().get("success").asBoolean()) {
			String userId = result.getBody().get("Result").get("UserId").asText();
			String jwt = doGenerateToken(userId);
			Cookie cookie = new Cookie("JwtToken", jwt);
			cookie.setHttpOnly(true);
			cookie.setSecure(true);
			cookie.setPath("/");
		//	cookie.setDomain("idaptive.app");
			response.addCookie(cookie);

			for (String value : result.getHeaders().get("Set-Cookie")) {
				if (value.split(";")[0].split("=")[0].equals(".ASPXAUTH")) {
					Cookie aspxauthCookie = new Cookie(".ASPXAUTH", value.split(";")[0].split("=")[1]);
					aspxauthCookie.setHttpOnly(true);
					aspxauthCookie.setSecure(true);
					aspxauthCookie.setPath("/");
			//		aspxauthCookie.setDomain("idaptive.app");
					response.addCookie(aspxauthCookie);
				}
			}

		}
		return result;
	}

	private String doGenerateToken(String usr) {
		List<GrantedAuthority> grantedAuthorities = AuthorityUtils
				.commaSeparatedStringToAuthorityList("ROLE_" + "Administrator");
		Map<String, Object> claims = new HashMap<String, Object>();
		claims.put("sub", usr);
		claims.put("iss", "Idaptive");
		claims.put("authorities",
				grantedAuthorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(jwtKey);
		Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
		String jwttoken = Jwts.builder().setClaims(claims).signWith(signatureAlgorithm, signingKey).compact();
		return jwttoken;
	}

}
