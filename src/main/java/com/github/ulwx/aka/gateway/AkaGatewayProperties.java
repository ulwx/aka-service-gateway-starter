package com.github.ulwx.aka.gateway;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.LinkedHashMap;


@ConfigurationProperties(AkaGatewayProperties.PROPERTIES_PREFX)
public class AkaGatewayProperties implements InitializingBean {
    public final static String  PROPERTIES_PREFX="aka.gateway";
    @NestedConfigurationProperty
    private LinkedHashMap<String,FilterConfig> filters=new LinkedHashMap<>();
    private StoreConfig storeConfig=new StoreConfig();

    public LinkedHashMap<String, FilterConfig> getFilters() {
        return filters;
    }

    public void setFilters(LinkedHashMap<String, FilterConfig> filters) {
        this.filters = filters;
    }
    public StoreConfig getStoreConfig() {
        return storeConfig;
    }

    public void setStoreConfig(StoreConfig storeConfig) {
        this.storeConfig = storeConfig;
    }

    public static class Matcher{
        private String[] paths=new String[0];
        private String[] routes=new String[0];

        public String[] getRoutes() {
            return routes;
        }

        public void setRoutes(String[] routes) {
            this.routes = routes;
        }

        public String[] getPaths() {
            return paths;
        }

        public void setPaths(String[] paths) {
            this.paths = paths;
        }
    }

    public static class TokenInRequest{
        private String in="";

        public String getIn() {
            return in;
        }

        public void setIn(String in) {
            this.in = in;
        }
    }
    public static class TokenInResponse{
        private String in="";
        private Boolean auto=true;
        private String accessTokenFetchUrl="";

        public Boolean getAuto() {
            return auto;
        }

        public void setAuto(Boolean auto) {
            this.auto = auto;
        }

        public String getAccessTokenFetchUrl() {
            return accessTokenFetchUrl;
        }

        public void setAccessTokenFetchUrl(String accessTokenFetchUrl) {
            this.accessTokenFetchUrl = accessTokenFetchUrl;
        }

        public String getIn() {
            return in;
        }

        public void setIn(String in) {
            this.in = in;
        }
    }
    public static class  VerifyConfig{
        private String tokenType="jwt";
        private String secret="";
        private TokenInRequest tokenInRequest=new TokenInRequest();
        private TokenInResponse tokenInResponse=new TokenInResponse();
        private String[] excludePaths=new String[0];
        private AccessToken accessToken=new AccessToken();
        private RefreshToken refreshToken=new RefreshToken();

        public void setTokenInRequest(TokenInRequest tokenInRequest) {
            this.tokenInRequest = tokenInRequest;
        }

        public TokenInResponse getTokenInResponse() {
            return tokenInResponse;
        }

        public TokenInRequest getTokenInRequest() {
            return tokenInRequest;
        }

        public void setTokenInResponse(TokenInResponse tokenInResponse) {
            this.tokenInResponse = tokenInResponse;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }


        public String[] getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(String[] excludePaths) {
            this.excludePaths = excludePaths;
        }

        public AccessToken getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(AccessToken accessToken) {
            this.accessToken = accessToken;
        }

        public RefreshToken getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(RefreshToken refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
    public static class AccessToken{
        private String paramName;
        private Integer expire;

        public String getParamName() {
            return paramName;
        }

        public void setParamName(String paramName) {
            this.paramName = paramName;
        }

        public Integer getExpire() {
            return expire;
        }

        public void setExpire(Integer expire) {
            this.expire = expire;
        }
    }
    public static class RefreshToken{
        private String paramName;
        private Integer expire;

        public String getParamName() {
            return paramName;
        }

        public void setParamName(String paramName) {
            this.paramName = paramName;
        }

        public Integer getExpire() {
            return expire;
        }

        public void setExpire(Integer expire) {
            this.expire = expire;
        }
    }
    public  static class FilterConfig{
        private Matcher matcher=new Matcher();
        private VerifyConfig verifyConfig=new VerifyConfig();
        private LoginConfig[] login=new LoginConfig[0];
        private LogoutConfig[] logout=new LogoutConfig[0];
        public Matcher getMatcher() {
            return matcher;
        }

        public void setMatcher(Matcher matcher) {
            this.matcher = matcher;
        }

        public VerifyConfig getVerifyConfig() {
            return verifyConfig;
        }

        public void setVerifyConfig(VerifyConfig verifyConfig) {
            this.verifyConfig = verifyConfig;
        }

        public LoginConfig[] getLogin() {
            return login;
        }

        public void setLogin(LoginConfig[] login) {
            this.login = login;
        }

        public LogoutConfig[] getLogout() {
            return logout;
        }

        public void setLogout(LogoutConfig[] logout) {
            this.logout = logout;
        }

    }

    public static class StoreConfig{
       private MemoryConfig memory=new MemoryConfig();
       private RedisConfig redis =new RedisConfig();

        public MemoryConfig getMemory() {
            return memory;
        }

        public void setMemory(MemoryConfig memory) {
            this.memory = memory;
        }

        public RedisConfig getRedis() {
            return redis;
        }

        public void setRedis(RedisConfig redis) {
            this.redis = redis;
        }
    }
    public static class RedisConfig{
        private Boolean enable=false;
        private String ds;

        public Boolean getEnable() {
            return enable;
        }

        public void setEnable(Boolean enable) {
            this.enable = enable;
        }

        public String getDs() {
            return ds;
        }

        public void setDs(String ds) {
            this.ds = ds;
        }
    }
    public static class MemoryConfig{
        private Boolean enable=false;

        public Boolean getEnable() {
            return enable;
        }

        public void setEnable(Boolean enable) {
            this.enable = enable;
        }
    }
    public static class LoginConfig{
        private String url;
        private String tokenBuilderClass;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTokenBuilderClass() {
            return tokenBuilderClass;
        }

        public void setTokenBuilderClass(String tokenBuilderClass) {
            this.tokenBuilderClass = tokenBuilderClass;
        }
    }

    public static class LogoutConfig{
        private String url;
        private String fetchLogoutCondition;

        public String getUrl() {
            return url;
        }


        public void setUrl(String url) {
            this.url = url;
        }

        public String getFetchLogoutCondition() {
            return fetchLogoutCondition;
        }

        public void setFetchLogoutCondition(String fetchLogoutCondition) {
            this.fetchLogoutCondition = fetchLogoutCondition;
        }
    }
    @Override
    public void afterPropertiesSet()  {
    }


}
