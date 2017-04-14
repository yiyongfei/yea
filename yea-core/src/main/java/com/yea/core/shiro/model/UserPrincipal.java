package com.yea.core.shiro.model;

import com.yea.core.base.model.BaseModel;

public class UserPrincipal extends BaseModel{
	private static final long serialVersionUID = 1L;

	private Long partyId;
	private String loginName;
	private String personName;
	private String isLock;
	
	public Long getPartyId() {
		return partyId;
	}
	public void setPartyId(Long partyId) {
		this.partyId = partyId;
	}
	public String getLoginName() {
		return loginName;
	}
	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}
	public String getPersonName() {
		return personName;
	}
	public void setPersonName(String personName) {
		this.personName = personName;
	}
	public String getIsLock() {
		return isLock;
	}
	public void setIsLock(String isLock) {
		this.isLock = isLock;
	}
	
}
