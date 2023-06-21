package com.github.ulwx.aka.gateway.filters;

public class LogoutCondition {
    private String UserId;
    private String resourceId;

    public LogoutCondition() {
    }

    public LogoutCondition(String userId, String resourceId, String refreshToken) {
        UserId = userId;
        this.resourceId = resourceId;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }


}
