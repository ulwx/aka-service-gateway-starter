package com.github.ulwx.aka.gateway.filters;

import com.alibaba.fastjson.JSON;
import com.github.ulwx.aka.gateway.AkaGatewayProperties;
import com.ulwx.tool.*;
import com.ulwx.type.TResult;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("com.github.ulwx.aka.gateway.filters.AkaGlobalFilter")
public class AkaGlobalFilter implements GlobalFilter, Ordered {
    private static Logger log = Logger.getLogger(AkaGlobalFilter.class);
    @Autowired
    private AkaGatewayProperties properties;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    @Autowired
    private TokenAdmin tokenAdmin;

    private Tokens fetchTokens(ServerHttpRequest serverHttpRequest,
                               AkaGatewayProperties.FilterConfig filterConfig) {

        log.trace("AkaGlobalFilter start");
        String paramIn = filterConfig.getVerifyConfig().getTokenInRequest().getIn();
        String refreshTokenParamName = filterConfig.getVerifyConfig().getRefreshToken().getParamName();
        String accessTokenParamName = filterConfig.getVerifyConfig().getAccessToken().getParamName();

        Tokens tokens = new Tokens();
        boolean inHeader = false;
        boolean inQuery = false;
        boolean inCookie = false;
        if (!paramIn.isEmpty()) {
            String[] strs = ArrayUtils.trim(paramIn.split(","));
            for (int i = 0; i < strs.length; i++) {
                //header、query、cookie
                if (strs[i].equals("header")) inHeader = true;
                if (strs[i].equals("query")) inQuery = true;
                if (strs[i].equals("cookie")) inCookie = true;
            }
        } else {
            inHeader = true;
            inQuery = true;
            inCookie = true;
        }
        String token = "";
        String refreshToken = "";
        if (inHeader) {
            token = serverHttpRequest.getHeaders().getFirst(accessTokenParamName);
            refreshToken = serverHttpRequest.getHeaders().getFirst(refreshTokenParamName);

        }
        if (inQuery) {
            if (StringUtils.isEmpty(token)) {//请求参数是否有Authorization
                token = serverHttpRequest.getQueryParams().getFirst(accessTokenParamName);
            }
            if (StringUtils.isEmpty(refreshToken)) {//请求参数是否有Authorization
                refreshToken = serverHttpRequest.getQueryParams().getFirst(refreshTokenParamName);
            }
        }
        if (inCookie) {
            if (StringUtils.isEmpty(token)) { //cookie里是否有Authorization
                HttpCookie cookie = serverHttpRequest.getCookies().getFirst(accessTokenParamName);
                if (cookie != null) {
                    token = cookie.getValue();
                }
            }
            if (StringUtils.isEmpty(refreshToken)) { //cookie里是否有Authorization
                HttpCookie cookie = serverHttpRequest.getCookies().getFirst(refreshTokenParamName);
                if (cookie != null) {
                    refreshToken = cookie.getValue();
                }
            }
        }
        tokens.setAccessToken(token);
        tokens.setRefreshToken(refreshToken);
        return tokens;
    }

    private void modifyTokensInRequest(ModifiedServerHttpRequest request,
                                       Tokens tokens,
                                       AkaGatewayProperties.FilterConfig filterConfig) {

        String paramIn = filterConfig.getVerifyConfig().getTokenInRequest().getIn();
        String refreshTokenParamName = filterConfig.getVerifyConfig().getRefreshToken().getParamName();
        String accessTokenParamName = filterConfig.getVerifyConfig().getAccessToken().getParamName();

        boolean inHeader = false;
        boolean inQuery = false;
        boolean inCookie = false;

        if (!paramIn.isEmpty()) {
            String[] strs = ArrayUtils.trim(paramIn.split(","));
            for (int i = 0; i < strs.length; i++) {
                //header、query、cookie
                if (strs[i].equals("header")) inHeader = true;
                if (strs[i].equals("query")) inQuery = true;
                if (strs[i].equals("cookie")) inCookie = true;
            }
        } else {
            inHeader = true;
            inQuery = true;
            inCookie = true;
        }
        String token = tokens.getAccessToken();
        String refreshToken = tokens.getRefreshToken();
        if (inHeader) {
            request.setHeader(accessTokenParamName, token);
            request.setHeader(refreshTokenParamName, refreshToken);
        }
        if (inQuery) {
            String originalQuery = request.getURI().getRawQuery();
            if (StringUtils.hasText(originalQuery)) {
                String newStr = StringUtils.replaceAll(originalQuery,
                        "(" +
                                accessTokenParamName +
                                "=)(.*?)(?=(\\&|$))", 2, (str) -> {
                            return EscapeUtil.escapeUrl(token, "utf-8");
                        }, new int[]{1});

                newStr = StringUtils.replaceAll(newStr,
                        "(" +
                                refreshTokenParamName +
                                "=)(.*?)(?=(\\&|$))", 2, (str) -> {
                            return EscapeUtil.escapeUrl(refreshToken, "utf-8");
                        }, new int[]{1});
                  request.repaceQuery(newStr);
            }else{
                String newStr=accessTokenParamName+"="+ EscapeUtil.escapeUrl(token, "utf-8")
                        +"&"+refreshTokenParamName+EscapeUtil.escapeUrl(refreshToken, "utf-8");
                request.repaceQuery(newStr);
            }

        }

        if (inCookie) {

            // 获取原始的Cookie列表
            HttpHeaders headers = request.getHeaders();
            String originalCookie = headers.getFirst(HttpHeaders.COOKIE);
            // 创建新的Cookie对象
            ResponseCookie accesssTokenCookie = ResponseCookie.from(accessTokenParamName, token).path("/").build();
            ResponseCookie refreshTokenCookie = ResponseCookie.from(refreshTokenParamName, refreshToken).path("/").build();
            // 将新的Cookie添加到请求头中
            String modifiedCookie = "";
            String appendCookie = accesssTokenCookie.toString() + ";" + refreshTokenCookie.toString();
            if (StringUtils.hasText(originalCookie)) {
                // modifiedCookie = originalCookie + "; " + appendCookie;
                modifiedCookie = StringUtils.replaceAll(originalCookie,
                        "(" +
                                accessTokenParamName +
                                "=)(.*?)(?=(\\;|$))", 2, (str) -> {
                            return token;
                        }, new int[]{1});

                modifiedCookie = StringUtils.replaceAll(modifiedCookie,
                        "(" +
                                refreshTokenParamName +
                                "=)(.*?)(?=(\\;|$))", 2, (str) -> {
                            return refreshToken;
                        }, new int[]{1});
            } else {
                modifiedCookie = appendCookie.toString();
            }
            request.setHeader(HttpHeaders.COOKIE, modifiedCookie);
        }



    }

    private String setTokenInResponse(String responseBody,AkaGatewayProperties.FilterConfig filterConfig,
                                    Tokens tokens, ModifiedServerHttpResponse serverHttpResponse) {
        String tokenIn = filterConfig.getVerifyConfig().getTokenInResponse().getIn();
        String accessTokenName = filterConfig.getVerifyConfig().getAccessToken().getParamName();
        String refreshTokenName = filterConfig.getVerifyConfig().getRefreshToken().getParamName();
        boolean inHeader = false;
        boolean inCookie = false;
        boolean body=false;
        if (!tokenIn.isEmpty()) {
            String[] strs = ArrayUtils.trim(tokenIn.split(","));
            for (int i = 0; i < strs.length; i++) {
                //header、query、cookie
                if (strs[i].equals("header")) inHeader = true;
                if (strs[i].equals("cookie")) inCookie = true;
                if (strs[i].equals("body")) body = true;
            }
        } else {
            inHeader = true;
            inCookie = true;
        }
        if (inHeader) {
            serverHttpResponse.getHeaders().add(accessTokenName, tokens.getAccessToken());
            serverHttpResponse.getHeaders().add(refreshTokenName, tokens.getRefreshToken());
        }
        if (inCookie) {
            ResponseCookie acessTokenCookie = ResponseCookie.from(accessTokenName, tokens.getAccessToken()).path("/").build();
            ResponseCookie refreshTokenCookie = ResponseCookie.from(refreshTokenName, tokens.getRefreshToken()).path("/").build();
            serverHttpResponse.addCookie(acessTokenCookie);
            serverHttpResponse.addCookie(refreshTokenCookie);
        }
        if(body) {
            Map map = ObjectUtils.fromJsonToMap(responseBody);
            map.put(filterConfig.getVerifyConfig().getAccessToken().getParamName(), tokens.getAccessToken());
            map.put(filterConfig.getVerifyConfig().getRefreshToken().getParamName(), tokens.getRefreshToken());
            String newBody = ObjectUtils.toJsonString(map);
            return newBody;
        }
        return null;

    }

    private void processLogin(AkaGatewayProperties.FilterConfig filterConfig,
                              AkaGatewayProperties.LoginConfig loginConfig,
                              AkaGatewayProperties.StoreConfig storeConfig,
                              ModifiedServerHttpRequest serverHttpRequest,
                              ModifiedServerHttpResponse serverHttpResponse) {

                serverHttpResponse.setModifyBodyFunction((body) -> {
                    try {
                        String tokenBuilderClass = loginConfig.getTokenBuilderClass();
                        LoginInfoBuilder tokenBuilder = (LoginInfoBuilder) Class.forName(tokenBuilderClass).getConstructor().newInstance();
                        LoginInfo loginInfo = tokenBuilder.build(serverHttpRequest, body);
                        if(loginInfo!=null){
                            TokenInfo tokenInfo=TokenInfo.build(loginInfo);
                            Tokens tokens = tokenAdmin.newTokens(
                                    tokenInfo,
                                    filterConfig,
                                    storeConfig
                            );
                            String  newBody=this.setTokenInResponse(body,filterConfig, tokens, serverHttpResponse);
                            return newBody;
                        }
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                });
    }

    private void processLogout(AkaGatewayProperties.FilterConfig filterConfig,
                               AkaGatewayProperties.StoreConfig storeConfig,
                                                      AkaGatewayProperties.LogoutConfig loginOutConfig,
                                                      ServerWebExchange exchange,
                                                      ModifiedServerHttpRequest serverHttpRequest,
                                                      ModifiedServerHttpResponse serverHttpResponse) {

        serverHttpResponse.setModifyBodyFunction(
                body -> {
                    String classNmae = loginOutConfig.getFetchLogoutCondition();
                    LogoutCondition logoutInfo = new LogoutCondition();
                    try {
                        if (StringUtils.hasText(classNmae)) {
                            FetchLogoutCondition fetchLogoutCondition =
                                    (FetchLogoutCondition) (Class.forName(classNmae).getConstructor().newInstance());
                            logoutInfo = fetchLogoutCondition.fetch(body, exchange);
                        }
                    } catch (Exception e) {
                        throw new GateWayException(ResponseCode.LOG_OUT_ERROR, e + "");
                    }
                    Tokens tokens = this.fetchTokens(serverHttpRequest, filterConfig);

                    tokenAdmin.removeToken(filterConfig,storeConfig, tokens,logoutInfo);
                    //cookie置空
                    Tokens finalNewTokens=new Tokens();
                    finalNewTokens.setAccessToken("");
                    finalNewTokens.setRefreshToken("");
                    String newBody=this.setTokenInResponse(body,filterConfig, finalNewTokens, serverHttpResponse);
                    return newBody;
                }
        );

    }

    private Mono<Void> handLogin(AkaGatewayProperties.FilterConfig filterConfig,
                                 AkaGatewayProperties.StoreConfig storeConfig,
                                 ServerWebExchange exchange,
                                 GatewayFilterChain chain,
                                 ModifiedServerHttpRequest serverHttpRequest,
                                 ModifiedServerHttpResponse serverHttpResponse) {
        String uri = serverHttpRequest.getURI().getPath();
        String context = this.contextPath;
        uri = StringUtils.trimLeadingString(uri, context);

        AkaGatewayProperties.LoginConfig[] loginConfigs = filterConfig.getLogin();
        AkaGatewayProperties.LoginConfig loginConfig = null;
        for (int i = 0; i < loginConfigs.length; i++) {
            loginConfig = loginConfigs[i];
            String loginUrl = loginConfig.getUrl();
            if (pathMatcher.match(loginUrl, uri)) {
                this.processLogin(filterConfig, loginConfig,storeConfig,
                        serverHttpRequest,serverHttpResponse);
                return this.result(chain, exchange, serverHttpRequest, serverHttpResponse);
            }

        }
        return null;
    }

    private Mono<Void> handLogout(AkaGatewayProperties.FilterConfig filterConfig,
                                  AkaGatewayProperties.StoreConfig storeConfig,
                                  ServerWebExchange exchange, GatewayFilterChain chain,
                                  ModifiedServerHttpRequest serverHttpRequest,
                                  ModifiedServerHttpResponse serverHttpResponse) {

        String uri = serverHttpRequest.getURI().getPath();
        String context = this.contextPath;
        uri = StringUtils.trimLeadingString(uri, context);
        AkaGatewayProperties.LogoutConfig logoutConfig = null;
        for (int i = 0; i < filterConfig.getLogout().length; i++) {
            logoutConfig = filterConfig.getLogout()[i];
            String logoutUrl = logoutConfig.getUrl();
            if (pathMatcher.match(logoutUrl, uri)) {
                //清除redis或数据库里的refresh token
                 processLogout(filterConfig,storeConfig,
                         logoutConfig, exchange,serverHttpRequest,serverHttpResponse);
                return this.result(chain, exchange, serverHttpRequest, serverHttpResponse);

            }
        }
        return null;
    }
    private Mono<Void> handNewAccessToken(AkaGatewayProperties.FilterConfig filterConfig,
                                  AkaGatewayProperties.StoreConfig storeConfig,
                                  ServerWebExchange exchange, GatewayFilterChain chain,
                                  ModifiedServerHttpRequest serverHttpRequest,
                                  ModifiedServerHttpResponse serverHttpResponse) {

        String uri = serverHttpRequest.getURI().getPath();
        String tokenFetchURL=filterConfig.getVerifyConfig().getTokenInResponse().getAccessTokenFetchUrl();
        String context = this.contextPath;
        uri = StringUtils.trimLeadingString(uri, context);
        if (pathMatcher.match(tokenFetchURL, uri)) {
            try {
                Tokens tokens = this.fetchTokens(serverHttpRequest, filterConfig);
                Tokens newTokens = tokenAdmin.newAccessToken(
                        tokens.getRefreshToken(),
                        filterConfig,
                        storeConfig
                );
                Tokens finalNewTokens = newTokens;
                serverHttpResponse.setModifyBodyFunction(body -> {
                    String newBody = this.setTokenInResponse(body, filterConfig, finalNewTokens, serverHttpResponse);
                    return newBody;
                });
            }catch (Exception ex) {
                log.error(ex + "", ex);
                if (ex instanceof GateWayException) {
                    GateWayException gateWayException = (GateWayException) ex;
                    return getErrorVoidMono(serverHttpResponse, gateWayException.getResponseCode(), ex + "");
                } else {
                    return getErrorVoidMono(serverHttpResponse, ResponseCode.REFRESH_TOKEN_INVALID, ex + "");
                }
            }
            return this.result(chain, exchange, serverHttpRequest, serverHttpResponse);
        }
        return null;
    }
    public Mono<Void> result(GatewayFilterChain chain,
                             ServerWebExchange exchange, ModifiedServerHttpRequest serverHttpRequest,
                             ModifiedServerHttpResponse serverHttpResponse) {
        return chain.filter(exchange.mutate().request(serverHttpRequest).response(serverHttpResponse).build());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ModifiedServerHttpRequest serverHttpRequest = new ModifiedServerHttpRequest(exchange);
        ModifiedServerHttpResponse serverHttpResponse =
                new ModifiedServerHttpResponse(exchange, String.class, String.class);
        String uri = serverHttpRequest.getURI().getPath();
        String context = this.contextPath;
        uri = StringUtils.trimLeadingString(uri, context);
        LinkedHashMap<String, AkaGatewayProperties.FilterConfig> map = properties.getFilters();
        boolean find = false;
        AkaGatewayProperties.FilterConfig tmpfilterConfig = null;
        for (String key : map.keySet()) {
            tmpfilterConfig = map.get(key);
            AkaGatewayProperties.Matcher matcher = tmpfilterConfig.getMatcher();
            String[] urls = matcher.getPaths();
            for (String url : urls) {
                if (pathMatcher.match(url, uri)) {
                    find = true;
                    break;
                }
            }
            if(find){
                break;
            }
        }
        if (!find) {
            return this.result(chain, exchange, serverHttpRequest, serverHttpResponse);
        }
        AkaGatewayProperties.FilterConfig filterConfig = tmpfilterConfig;
        //处理登录接口
        Mono<Void> ret = null;
        ret = this.handLogin(filterConfig, properties.getStoreConfig(),
                exchange,
                chain, serverHttpRequest, serverHttpResponse);
        if (ret != null) {
            return ret;
        }
        //处理登出接口
        ret = this.handLogout(filterConfig, properties.getStoreConfig(),
                exchange,
                chain,serverHttpRequest, serverHttpResponse);
        if (ret != null) {
            return ret;
        }
        ret=this.handNewAccessToken(filterConfig, properties.getStoreConfig(),
                exchange,
                chain, serverHttpRequest, serverHttpResponse);
        if (ret != null) {
            return ret;
        }
        //排除哪些不需要验证的
        String[] exludes = filterConfig.getVerifyConfig().getExcludePaths();
        for (int i = 0; i < exludes.length; i++) {
            if (pathMatcher.match(exludes[i], uri)) {
                return this.result(chain, exchange, serverHttpRequest, serverHttpResponse);
            }
        }
        //根据配置获取token
        Tokens tokens = this.fetchTokens(serverHttpRequest, filterConfig);
        String accessToken = tokens.getAccessToken();
        String refreshToken = tokens.getRefreshToken();
        log.debug("accessToken=" + accessToken);
        if (StringUtils.isEmpty(accessToken)) {
            serverHttpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
            return getErrorVoidMono(serverHttpResponse, ResponseCode.TOKEN_MISSION, "");
        }
        //校验token
        TokenInfo jwtInf = null;
        Tokens newTokens = null;
        try {
            TResult<TokenInfo> result=new TResult<TokenInfo>();
            //accessToken是否过期
            boolean isValid=tokenAdmin.verifyAccessToken(filterConfig, accessToken,result );
            jwtInf=result.getValue();
            if(!isValid){
                if (StringUtils.hasText(refreshToken)) {
                    //jwtInf.setExpiredAt(null);
                    Boolean auto=filterConfig.getVerifyConfig().getTokenInResponse().getAuto();
                    if(auto!=null && auto) { //如果自动生成access_token
                        ////会判断refresh_token是否过期
                        newTokens = tokenAdmin.newAccessToken(
                                refreshToken,
                                filterConfig,
                                properties.getStoreConfig()
                        );
                        Tokens finalNewTokens = newTokens;
                        serverHttpResponse.setModifyBodyFunction(body -> {
                            String newBody = this.setTokenInResponse(body, filterConfig, finalNewTokens, serverHttpResponse);
                            return newBody;
                        });
                    }else{
                        throw new GateWayException(ResponseCode.TOKEN_EXPIRED);
                    }
                }else {
                    throw new GateWayException(ResponseCode.TOKEN_EXPIRED);
                }
            }

        } catch (Exception ex) {
            log.error(ex + "", ex);
            if (ex instanceof GateWayException) {
                GateWayException gateWayException = (GateWayException) ex;
                return getErrorVoidMono(serverHttpResponse, gateWayException.getResponseCode(), ex + "");
            } else {
                return getErrorVoidMono(serverHttpResponse, ResponseCode.TOKEN_INVALID, ex + "");
            }
        }
        if (newTokens != null) {
            modifyTokensInRequest(serverHttpRequest,  newTokens, filterConfig);
        }
        return this.result(chain, exchange, serverHttpRequest, serverHttpResponse);
    }

    public static void main(String[] args) {
        String originalQuery = "abc=124&xyz=677";
        String newStr = StringUtils.replaceAll(originalQuery,
                "(xyz=)(.*?)(?=(\\&|$))", 2, (str) -> {
                    return "uuuu";
                }, new int[]{1});
        System.out.println(newStr);
    }

    public final static String JWT_INFO_HEADER = "JWT_INFO";

    private Mono<Void> getErrorVoidMono(ServerHttpResponse serverHttpResponse, ResponseCode responseCode, String errMsg) {
        serverHttpResponse.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        ResponseResult responseResult = null;
        if (StringUtils.hasText(errMsg)) {
            responseResult = ResponseResult.error(responseCode.getCode(), responseCode.getMessage()
                    + "[" + errMsg + "]");
        } else {
            responseResult = ResponseResult.error(responseCode.getCode(), responseCode.getMessage());
        }
        DataBuffer dataBuffer = serverHttpResponse.bufferFactory().wrap(JSON.toJSONString(responseResult).getBytes());
        return serverHttpResponse.writeWith(Flux.just(dataBuffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
