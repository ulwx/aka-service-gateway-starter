package com.github.ulwx.aka.gateway.filters.utils;

import java.util.Date;

public class TokenInfo {

	private String jwtID="";
	private String user="";//存放为用户id，最终会放入请求Bean的userid字段
	private String phone="";
	private String userType="";
	private String ext="";
	private String deviceID="";
	private String source="";
	private Date expiredAt;

	public Date getExpiredAt() {
		return expiredAt;
	}

	public void setExpiredAt(Date expiredAt) {
		this.expiredAt = expiredAt;
	}

	public String getExt() {
		return ext;
	}

	/**
	 * 设置扩展信息，根据业务信息自定义
	 * @param ext
	 */
	public void setExt(String ext) {
		this.ext = ext;
	}
	public String getUserType() {
		return userType;
	}

	/**
	 * 可以设置角色等业务信息
	 * @param userType
	 */
	public void setUserType(String userType) {
		this.userType = userType;
	}
	public String getJwtID() {
		return jwtID;
	}


	/**
	 * 用于标识本次jwt的id
	 * @param jwtID
	 */
	public void setJwtID(String jwtID) {
		this.jwtID = jwtID;
	}
	public String getUser() {
		return user;
	}
	/**
	 *
	 * @param userId 存放为用户id，最终会放入请求Bean的userid字段
	 */
	public void setUser(String userId) {
		this.user = userId;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getDeviceID() {
		return deviceID;
	}
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}


	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
