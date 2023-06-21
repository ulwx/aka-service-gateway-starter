package com.github.ulwx.aka.gateway.filters;

import org.springframework.web.server.ServerWebExchange;

public interface FetchLogoutCondition {

    public LogoutCondition fetch(String body, ServerWebExchange exchange);

}
