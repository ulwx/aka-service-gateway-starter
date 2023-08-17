package com.github.ulwx.aka.gateway.filters;

import com.github.ulwx.aka.gateway.filters.model.CbRequest;
import com.ulwx.tool.CTime;
import com.ulwx.tool.ObjectUtils;
import com.ulwx.tool.SnowflakeIdWorker;
import com.ulwx.tool.StringUtils;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.apache.skywalking.apm.toolkit.webflux.WebFluxSkyWalkingOperators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AccessLogFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Mono<Void> ret=null;
        GateWayLog gwlog=new GateWayLog();
        String traceId = WebFluxSkyWalkingOperators.continueTracing(exchange, TraceContext::traceId);
        ModifiedServerHttpResponse serverHttpResponse =
                new ModifiedServerHttpResponse(exchange, String.class, String.class);
        serverHttpResponse.getHeaders().set("x-trace-id", traceId);
        ModifiedServerHttpRequest serverHttpRequest = new ModifiedServerHttpRequest(exchange,
                chain,serverHttpResponse,
                (body,contentType)->{
                    String str= StringUtils.trim(body);
                    if(str.startsWith("{") && str.endsWith("}")){
                        CbRequest result=ObjectUtils.fromJsonToObject(str, CbRequest.class);
                        if(result!=null ){
                            if(result.getRequestId()==null || result.getRequestId().trim().isEmpty()){
                                result.setRequestId(SnowflakeIdWorker.instance.nextId()+"");
                            }
                            if(result.getTimestamp()==null){
                                result.setTimestamp(CTime.formatWholeAllDate());
                            }
                            return ObjectUtils.toStringUseFastJson(result);
                        }

                    }
                    return body;
                },
                (gwl)->{
                   log.debug("log+++"+ TraceContext.traceId());
                   log.debug(ObjectUtils.toString(gwl));
                    log.debug("log---"+ MDC.get("tid"));
                });

        return serverHttpRequest.forward();

    }

    @Override
    public int getOrder() {
        return ORDER;
    }
    public static final int ORDER = Integer.MIN_VALUE+1;
}

