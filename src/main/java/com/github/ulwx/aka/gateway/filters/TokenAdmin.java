package com.github.ulwx.aka.gateway.filters;

import com.github.ulwx.aka.gateway.AkaGatewayProperties;
import com.github.ulwx.aka.gateway.filters.auth.TokenServiceFactory;
import com.github.ulwx.aka.gateway.filters.auth.UserTokenInfo;
import com.github.ulwx.aka.gateway.filters.utils.JwtHelper;
import com.github.ulwx.aka.gateway.filters.utils.TokenInfo;
import com.ulwx.tool.CTime;
import com.ulwx.tool.RandomUtils;
import com.ulwx.tool.StringUtils;
import com.ulwx.type.TResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component("com.github.ulwx.aka.gateway.filters.TokenCreator")
public class TokenAdmin {
    @Autowired
    private TokenServiceFactory tokenServiceFactory;

    public Tokens newTokens(TokenInfo tokenInfo,
                            AkaGatewayProperties.FilterConfig filterConfig){
        Tokens tokens=new Tokens();
        String tokenType = StringUtils.trim(filterConfig.getVerifyConfig().getTokenType());
        if (tokenType.equals(TokenType.jwt.toString())) {
            String key=filterConfig.getVerifyConfig().getSecret();
            Integer expiretime=filterConfig.getVerifyConfig().getAccessToken().getExpire();
            Integer refreshExpiredTime=filterConfig.getVerifyConfig().getRefreshToken().getExpire();
            if(tokenInfo.getExpiredAt()==null){
                tokenInfo.setExpiredAt(CTime.addSenconds(expiretime));
            }
            String jwtToken= JwtHelper.createJWT(tokenInfo,key);
            String refreshToken= RandomUtils.genUID();
            tokens.setAccessToken(jwtToken);
            tokens.setRefreshToken(refreshToken);
            //保存token
            UserTokenInfo userTokenInfo=new UserTokenInfo();
            userTokenInfo.setTokenInfo(tokenInfo);
            userTokenInfo.setUserid(tokenInfo.getUser());
            userTokenInfo.setAccessToken(jwtToken);
            userTokenInfo.setRefreshToken(refreshToken);
            userTokenInfo.setAccessTokenTtl(expiretime);
            userTokenInfo.setRefreshTokenTtl(refreshExpiredTime);
            userTokenInfo.setTokenType(TokenType.jwt);
            userTokenInfo.setSource(tokenInfo.getSource());
            tokenServiceFactory.storeTokenInfo(filterConfig.getStoreConfig(),userTokenInfo);
        }else{ //其他token类型

        }

        return tokens;
    }

    public boolean verifyToken(AkaGatewayProperties.FilterConfig filterConfig, String accessToken,
                               TResult<TokenInfo> result ){
        String tokenType = StringUtils.trim(filterConfig.getVerifyConfig().getTokenType());
        if (tokenType.equals(TokenType.jwt.toString())) {
            String secret = filterConfig.getVerifyConfig().getSecret();
            TokenInfo tokenInfo = JwtHelper.decode(accessToken);
            result.setValue(tokenInfo);
            Date date = tokenInfo.getExpiredAt();
            if (CTime.getDate().after(date)) {//access_token过期
                return false;
            }
        }
        return true;
    }
    public Tokens newAccessToken(
                                 TokenInfo tokenInfo,
                                 String refreshToken,
                                 AkaGatewayProperties.FilterConfig filterConfig){
        Tokens tokens=new Tokens();
        String tokenType = StringUtils.trim(filterConfig.getVerifyConfig().getTokenType());
        if (tokenType.equals(TokenType.jwt.toString())) {
            //检测refreshToken是否超时
            UserTokenInfo userTokenInfo=null;

            userTokenInfo =tokenServiceFactory.queryTokenInfoBy(filterConfig.getStoreConfig(),refreshToken) ;

            if(userTokenInfo==null || CTime.getDate().after(userTokenInfo.getRefreshTokenExpireTime())){ //已经过期
                throw new GateWayException(ResponseCode.REFRESH_TOKEN_INVALID);
            }else{
                String key=filterConfig.getVerifyConfig().getSecret();
                Integer expiretime=filterConfig.getVerifyConfig().getAccessToken().getExpire();
                Integer refreshExpiredTime=filterConfig.getVerifyConfig().getRefreshToken().getExpire();
                if(tokenInfo.getExpiredAt()==null){
                    tokenInfo.setExpiredAt(CTime.addSenconds(expiretime));
                }
                //生成access_token
                String jwtToken= JwtHelper.createJWT(tokenInfo,key);

                tokens.setAccessToken(jwtToken);
                tokens.setRefreshToken(refreshToken);
                //保存token
                userTokenInfo.setTokenInfo(tokenInfo);
                userTokenInfo.setAccessToken(jwtToken);
                userTokenInfo.setRefreshToken(refreshToken);
                userTokenInfo.setAccessTokenTtl(expiretime);
                //延长refreshToken的时间
                userTokenInfo.setRefreshTokenTtl(refreshExpiredTime);
                userTokenInfo.setTokenType(TokenType.jwt);
                userTokenInfo.setSource(tokenInfo.getSource());
                tokenServiceFactory.storeTokenInfo(filterConfig.getStoreConfig(),userTokenInfo);
            }

        }
        return tokens;

    }

    public void removeToken(AkaGatewayProperties.FilterConfig filterConfig,
            Tokens tokens,LogoutCondition logoutInfo ){
        String tokenType = StringUtils.trim(filterConfig.getVerifyConfig().getTokenType());
        if (tokenType.equals(TokenType.jwt.toString())) {
            tokenServiceFactory.removeToken(filterConfig.getStoreConfig(), tokens, logoutInfo);
        }
    }

}
