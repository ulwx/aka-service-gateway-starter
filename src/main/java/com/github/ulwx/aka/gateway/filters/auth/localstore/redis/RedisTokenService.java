package com.github.ulwx.aka.gateway.filters.auth.localstore.redis;

import com.github.ulwx.aka.dbutils.springboot.redis.AkaRedisUtils;
import com.github.ulwx.aka.gateway.filters.auth.TokenService;
import com.github.ulwx.aka.gateway.filters.auth.UserTokenInfo;
import com.github.ulwx.aka.gateway.filters.utils.RedisUtils;
import com.ulwx.tool.ObjectUtils;
import com.ulwx.tool.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Set;
@Component("com.github.ulwx.aka.gateway.filters.auth.localstore.redis.RedisTokenService")
public class RedisTokenService implements TokenService {
    public final static String prefx_rft ="token_rft";
    public final static String prefx_usr ="token_usr";
    private RedisUtils redisUtils;
    public RedisTokenService(ObjectProvider<RedisUtils> redisUtils){
        this.redisUtils=redisUtils.getIfAvailable();
    }

    public RedisUtils getRedisUtils() {
        return redisUtils;
    }

    public void setRedisUtils(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    private static String prefx_rft(String refreshToken){
        return prefx_rft +":"+refreshToken ;
    }
    private static String prefx_usr(String userID){
        return prefx_usr +":"+userID ;
    }

    private void removeTokenInfo(String userId,String refreshToken) {
        AkaRedisUtils akaRedisUtils=redisUtils.getAkaRedisUtils();
        akaRedisUtils.sRemove(prefx_usr(userId),refreshToken);
        akaRedisUtils.del(prefx_rft(refreshToken));
        akaRedisUtils.del(prefx_rft(refreshToken)+"@");
    }

    @Override
    public void removeTokenInfoBy(String refreshToken, String userId, String sourceId){

        if(StringUtils.hasText(refreshToken)){
            removeTokenInfo(userId,refreshToken);
            return;
        }
        if(StringUtils.hasText(userId)) {
            Set<String> refreshTokens = getRefreshTokensBy(userId);
            for (String refreshTk : refreshTokens) {
                UserTokenInfo userTokenInfo = queryTokenInfoBy(refreshTk);
                if (StringUtils.hasText(sourceId)) {
                    if (userTokenInfo!=null &&
                            userTokenInfo.getSource().equals(sourceId)) {
                        removeTokenInfo(userId,refreshTk);
                        break;
                    }
                }else{
                    removeTokenInfo(userId,refreshTk);
                }

            }
        }
    }
    private Set<String> getRefreshTokensBy(String userId) {
        AkaRedisUtils akaRedisUtils=redisUtils.getAkaRedisUtils();
        String key= prefx_usr(userId);
        Set<String> set= akaRedisUtils.sGet(key);
        return set;
    }

    @Override
    public void storeTokenInfo(UserTokenInfo userTokenInfo) {
        String str=ObjectUtils.toStringUseFastJson(userTokenInfo);
        AkaRedisUtils akaRedisUtils=redisUtils.getAkaRedisUtils();
        akaRedisUtils.set(prefx_rft(userTokenInfo.getRefreshToken()), str , userTokenInfo.getRefreshTokenTtl());
        akaRedisUtils.set(prefx_rft(userTokenInfo.getRefreshToken())+"@", userTokenInfo.getUserid(),-1);
        String tokenkey= prefx_usr(userTokenInfo.getUserid());
        akaRedisUtils.sSet(tokenkey,userTokenInfo.getRefreshToken());
    }
    @Override
   public UserTokenInfo queryTokenInfoBy(String refreshToken) {
        AkaRedisUtils akaRedisUtils=redisUtils.getAkaRedisUtils();
        String str=akaRedisUtils.get(prefx_rft(refreshToken));
        if(StringUtils.isEmpty(str)){
            return null;
        }
        UserTokenInfo obj=ObjectUtils.fromJsonToObject(str, UserTokenInfo.class);
        return obj;
    }
    @Override
    public void refreshTokenExpired(String refreshToken){
        AkaRedisUtils akaRedisUtils=redisUtils.getAkaRedisUtils();
        String urserId=akaRedisUtils.get(prefx_rft(refreshToken)+"@");
        akaRedisUtils.sRemove(prefx_usr(urserId),refreshToken);
        akaRedisUtils.del(prefx_rft(refreshToken)+"@");

    }


}
