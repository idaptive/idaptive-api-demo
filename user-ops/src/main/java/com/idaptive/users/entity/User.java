package com.idaptive.users.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

	@JsonProperty("ID")
	private String uuid;

	@JsonProperty("DisplayName")
	private String displayName;

	@JsonProperty("Mail")
	private String mail;

	@JsonProperty("Name")
	private String name;

	@JsonProperty("Password")
	private String password;

	@JsonProperty("HomeNumber")
	private String homeNumber;

	@JsonProperty("ReportsTo")
	private String reportsTo;

	@JsonProperty("PreferredCulture")
	private String preferredCulture;

	@JsonProperty("PasswordNeverExpire")
	private boolean passwordNeverExpire;

	@JsonProperty("SendSmsInvite")
	private boolean sendSmsInvite;

	@JsonProperty("OfficeNumber")
	private String officeNumber;

	@JsonProperty("ForcePasswordChangeNext")
	private boolean forcePasswordChangeNext;

	@JsonProperty("InSysAdminRole")
	private boolean inSysAdminRole;

	@JsonProperty("MobileNumber")
	private String mobileNumber;

	@JsonProperty("InEverybodyRole")
	private boolean inEverybodyRole;

	@JsonProperty("ServiceUser")
	private boolean serviceUser;

	@JsonProperty("SendEmailInvite")
	private boolean sendEmailInvite;

	@JsonProperty("Description")
	private String description;

	@JsonProperty("PictureUri")
	private String pictureUri;

	public User(String displayName, String email, String name, String password) {
		this.displayName = displayName;
		this.mail = email;
		this.name = name;
		this.password = password;
	}
	
	public User() {
		
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHomeNumber() {
		return homeNumber;
	}

	public void setHomeNumber(String homeNumber) {
		this.homeNumber = homeNumber;
	}

	public String getReportsTo() {
		return reportsTo;
	}

	public void setReportsTo(String reportsTo) {
		this.reportsTo = reportsTo;
	}

	public boolean isPasswordNeverExpire() {
		return passwordNeverExpire;
	}

	public void setPasswordNeverExpire(boolean passwordNeverExpire) {
		this.passwordNeverExpire = passwordNeverExpire;
	}

	public boolean isSendSmsInvite() {
		return sendSmsInvite;
	}

	public void setSendSmsInvite(boolean sendSmsInvite) {
		this.sendSmsInvite = sendSmsInvite;
	}

	public String getOfficeNumber() {
		return officeNumber;
	}

	public void setOfficeNumber(String officeNumber) {
		this.officeNumber = officeNumber;
	}

	public boolean isForcePasswordChangeNext() {
		return forcePasswordChangeNext;
	}

	public void setForcePasswordChangeNext(boolean forcePasswordChangeNext) {
		this.forcePasswordChangeNext = forcePasswordChangeNext;
	}

	public boolean isInSysAdminRole() {
		return inSysAdminRole;
	}

	public void setInSysAdminRole(boolean inSysAdminRole) {
		this.inSysAdminRole = inSysAdminRole;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public boolean isInEverybodyRole() {
		return inEverybodyRole;
	}

	public void setInEverybodyRole(boolean inEverybodyRole) {
		this.inEverybodyRole = inEverybodyRole;
	}

	public boolean isServiceUser() {
		return serviceUser;
	}

	public void setServiceUser(boolean serviceUser) {
		this.serviceUser = serviceUser;
	}

	public boolean isSendEmailInvite() {
		return sendEmailInvite;
	}

	public void setSendEmailInvite(boolean sendEmailInvite) {
		this.sendEmailInvite = sendEmailInvite;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getPreferredCulture() {
		return preferredCulture;
	}

	public void setPreferredCulture(String preferredCulture) {
		this.preferredCulture = preferredCulture;
	}

	public String getPictureUri() {
		return pictureUri;
	}

	public void setPictureUri(String pictureUri) {
		this.pictureUri = pictureUri;
	}
	@Override
	public String toString() {
		return "User [uuid=" + uuid + ", displayName=" + displayName + ", mail=" + mail + ", name=" + name
				+ ", password=" + password + ", homeNumber=" + homeNumber + ", reportsTo=" + reportsTo
				+ ", preferredCulture=" + preferredCulture + ", passwordNeverExpire=" + passwordNeverExpire
				+ ", sendSmsInvite=" + sendSmsInvite + ", officeNumber=" + officeNumber + ", forcePasswordChangeNext="
				+ forcePasswordChangeNext + ", inSysAdminRole=" + inSysAdminRole + ", mobileNumber=" + mobileNumber
				+ ", inEverybodyRole=" + inEverybodyRole + ", serviceUser=" + serviceUser + ", sendEmailInvite="
				+ sendEmailInvite + ", description=" + description + ", pictureUri=" + pictureUri + "]";
	}

	
}
