package com.github.ulwx.aka.gateway.filters.auth;

import com.github.ulwx.aka.gateway.AkaGatewayProperties;
import com.github.ulwx.aka.gateway.filters.LogoutCondition;
import com.github.ulwx.aka.gateway.filters.Tokens;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("com.github.ulwx.aka.gateway.filters.auth.TokenServiceFactory")
public class TokenServiceFactory {
    public static final String memory
            ="com.github.ulwx.aka.gateway.filters.auth.localstore.memory.MemoryTokenService";
    public static final String redis
            ="com.github.ulwx.aka.gateway.filters.auth.localstore.redis.RedisTokenService";
    private Map<String,TokenService> map;

    public Map<String, TokenService> getMap() {
        return map;
    }
    @Autowired
    public void setMap(Map<String, TokenService> map) {
        this.map = map;
    }
    public  void storeTokenInfo(AkaGatewayProperties.StoreConfig storeConfig,UserTokenInfo userTokenInfo){
        if(storeConfig.getMemory().getEnable()) {
            getTokenService(TokenServiceFactory.memory).storeTokenInfo(userTokenInfo);
        }

        if(storeConfig.getRedis().getEnable()){
            getTokenService(TokenServiceFactory.redis).storeTokenInfo(userTokenInfo);
        }

    }

    public void removeToken(AkaGatewayProperties.StoreConfig storeConfig, Tokens tokens,
                            LogoutCondition logoutCondition) {
        if (storeConfig.getMemory().getEnable()) {
            getTokenService(TokenServiceFactory.memory)
                    .removeTokenInfoBy(tokens.getRefreshToken(),
                            logoutCondition.getUserId(), logoutCondition.getResourceId());

        }
        if (storeConfig.getRedis().getEnable()) {
            getTokenService(TokenServiceFactory.redis)
                    .removeTokenInfoBy(tokens.getRefreshToken(),
                            logoutCondition.getUserId(), logoutCondition.getResourceId());

        }
    }

    public UserTokenInfo queryTokenInfoBy(AkaGatewayProperties.StoreConfig storeConfig,String refreshToken){
        if(storeConfig.getMemory().getEnable()) {
            UserTokenInfo userTokenInfo =  getTokenService(TokenServiceFactory.memory).queryTokenInfoBy(refreshToken);
            return userTokenInfo;
        }
        if(storeConfig.getRedis().getEnable()) {
            UserTokenInfo userTokenInfo =  getTokenService(TokenServiceFactory.redis).queryTokenInfoBy(refreshToken);
            return userTokenInfo;
        }
        return null;
    }
    public TokenService getTokenService(String className){
        return map.get(className);
    }

}
