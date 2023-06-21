package com.github.ulwx.aka.gateway.filters.auth;

public interface TokenService {
    public void storeTokenInfo(UserTokenInfo userTokenInfo);
    public void removeTokenInfoBy(String refreshToken, String userId, String sourceId);
    public UserTokenInfo queryTokenInfoBy(String refreshToken);
    default public void refreshTokenExpired(String refreshToken){};

}
