package com.github.ulwx.aka.gateway.filters;

import com.github.ulwx.aka.gateway.filters.utils.TokenInfo;
import org.springframework.http.server.reactive.ServerHttpRequest;

public interface TokenInfoBuilder {
    public TokenInfo build(ServerHttpRequest request, String responseBody);
    default public TokenType getType(){
        return TokenType.jwt;
    }
}
