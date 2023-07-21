package com.github.ulwx.aka.gateway.filters;

import org.springframework.http.server.reactive.ServerHttpRequest;

public interface LoginInfoBuilder {
    public LoginInfo build(ServerHttpRequest request, String proxiedResponseBody);
    default public TokenType getType(){
        return TokenType.jwt;
    }
}
