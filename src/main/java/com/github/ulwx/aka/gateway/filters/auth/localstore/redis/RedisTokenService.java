package com.github.ulwx.aka.gateway.filters.auth.localstore.redis;

import com.github.ulwx.aka.gateway.filters.auth.TokenService;
import com.github.ulwx.aka.gateway.filters.auth.UserTokenInfo;
import com.github.ulwx.aka.gateway.filters.utils.RedisUtils;
import com.ulwx.tool.ObjectUtils;
import com.ulwx.tool.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
@Component("com.github.ulwx.aka.gateway.filters.auth.localstore.redis.RedisTokenService")
public class RedisTokenService implements TokenService {
    //public final static RedisTokenService x=new RedisTokenService();
    public final static String prefx_rft ="token_rft";
    public final static String prefx_usr ="token_usr";
    @Autowired
    private RedisUtils redisUtils;

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

        redisUtils.sRemove(prefx_usr(userId),refreshToken);
        redisUtils.del(prefx_rft(refreshToken));
        redisUtils.del(prefx_rft(refreshToken)+"@");
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
                if (userTokenInfo != null) {
                    if (StringUtils.hasText(sourceId)) {
                        if (userTokenInfo.getSource().equals(sourceId)) {
                            removeTokenInfo(userId,refreshToken);
                            break;
                        }
                    }else{
                        removeTokenInfo(userId,refreshToken);
                    }
                }else {
                    removeTokenInfo(userId,refreshToken);
                }
            }
        }
    }
    private Set<String> getRefreshTokensBy(String userId) {
        String key= prefx_usr(userId);
        Set<String> set= redisUtils.sGet(key);
        return set;
    }

    @Override
    public void storeTokenInfo(UserTokenInfo userTokenInfo) {
        String str=ObjectUtils.toStringUseFastJson(userTokenInfo);
        redisUtils.set(prefx_rft(userTokenInfo.getRefreshToken()), str,userTokenInfo.getRefreshTokenTtl());
        redisUtils.set(prefx_rft(userTokenInfo.getRefreshToken())+"@",userTokenInfo.getUserid()
                ,-1);
        String tokenkey= prefx_usr(userTokenInfo.getUserid());
        redisUtils.sSet(tokenkey,userTokenInfo.getRefreshToken());
    }
    @Override
   public UserTokenInfo queryTokenInfoBy(String refreshToken) {
        String str=redisUtils.get(prefx_rft(refreshToken));
        if(StringUtils.isEmpty(str)){
            return null;
        }
        UserTokenInfo obj=ObjectUtils.fromJsonToObject(str, UserTokenInfo.class);
        return obj;
    }
    @Override
    public void refreshTokenExpired(String refreshToken){
        String urserId=redisUtils.get(prefx_rft(refreshToken)+"@");
        redisUtils.sRemove(prefx_usr(urserId),refreshToken);
        redisUtils.del(prefx_rft(refreshToken)+"@");

    }


}
