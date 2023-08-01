//package com.github.ulwx.aka.gateway.filters;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Component
//public class TestGobalFilter implements GlobalFilter, Ordered {
//    private static final Logger log = LoggerFactory.getLogger(TestGobalFilter.class);
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        if(1==1) {
//            throw new RuntimeException();
//        }
//        return chain.filter(exchange);
//
//
//    }
//
//    @Override
//    public int getOrder() {
//        return -100;
//    }
//}
//
