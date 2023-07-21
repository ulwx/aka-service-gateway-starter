package com.github.ulwx.aka.gateway.filters.auth;

import com.github.ulwx.aka.gateway.filters.TokenType;
import com.github.ulwx.aka.gateway.filters.TokenInfo;
import com.ulwx.tool.CTime;

import java.util.Date;

public class UserTokenInfo {
    private String userid;
    private String accessToken;
    private int accessTokenTtl;
    private Date accessTokenExpireTime;
    private String refreshToken;
    private int refreshTokenTtl;
    private Date refreshTokenExpireTime;
    private String source="";
    private TokenType tokenType=TokenType.jwt;
    private TokenInfo tokenInfo;


    public UserTokenInfo(){

    }


    /**
     *
     * @param userid
     * @param accessToken
     * @param accessTokenTtl
     * @param refreshToken
     * @param refreshTokenTtl
     * @param source 来源，例如 app，web
     */
    public UserTokenInfo(String userid,
                         String accessToken,
                         int accessTokenTtl,
                         String refreshToken,
                         int  refreshTokenTtl,
                         String source) {
        this.userid = userid;
        this.accessToken = accessToken;
        this.accessTokenTtl=accessTokenTtl;
        this.accessTokenExpireTime = CTime.addSenconds(accessTokenTtl);
        this.refreshToken = refreshToken;
        this.refreshTokenTtl=refreshTokenTtl;
        this.refreshTokenExpireTime = CTime.addSenconds(refreshTokenTtl);
        if(source==null){
            source="";
        }
        this.source = source;
    }


    public int getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(int accessTokenTtl) {
        this.accessTokenExpireTime = CTime.addSenconds(accessTokenTtl);
        this.accessTokenTtl = accessTokenTtl;
    }

    public int getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(int refreshTokenTtl) {
        this.refreshTokenExpireTime = CTime.addSenconds(refreshTokenTtl);
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public Date getAccessTokenExpireTime() {
        return accessTokenExpireTime;
    }

    public void setAccessTokenExpireTime(Date accessTokenExpireTime) {
        this.accessTokenExpireTime = accessTokenExpireTime;
    }

    public Date getRefreshTokenExpireTime() {
        return refreshTokenExpireTime;
    }

    public void setRefreshTokenExpireTime(Date refreshTokenExpireTime) {
        this.refreshTokenExpireTime = refreshTokenExpireTime;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }


    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public TokenInfo getTokenInfo() {
        return tokenInfo;
    }

    public void setTokenInfo(TokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
    }
}
