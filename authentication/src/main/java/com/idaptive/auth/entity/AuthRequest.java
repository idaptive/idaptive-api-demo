package com.idaptive.auth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthRequest {
	@JsonProperty("TenantId")
	private String tenantId;

	@JsonProperty("User")
	private String username;

	@JsonProperty("Version")
	private String version;

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "AuthRequest [tenantId=" + tenantId + ", username=" + username + ", version=" + version + "]";
	}

	
}
