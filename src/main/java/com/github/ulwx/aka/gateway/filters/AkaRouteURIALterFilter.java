package com.github.ulwx.aka.gateway.filters;

import com.ulwx.tool.EscapeUtil;
import com.ulwx.tool.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * 参考RouteToRequestUrlFilter实现
 */
@Component("com.github.ulwx.aka.gateway.filters.AkaRouteURIALterFilter")
public class AkaRouteURIALterFilter implements GlobalFilter, Ordered {

	public static final int ORDER = Integer.MIN_VALUE;
	private static final Log log = LogFactory.getLog(AkaRouteURIALterFilter.class);
	@Override
	public int getOrder() {
		return ORDER;
	}
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
		if (route == null) {
			return chain.filter(exchange);
		}
		log.trace("AkaRouteURIALterFilter start");

		URI routeUri = route.getUri();

		//ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE
		Map<String,String> map=exchange.getAttribute(ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if ("lb".equalsIgnoreCase(routeUri.getScheme()) ) {
			String authority=routeUri.getAuthority();
			String uri= EscapeUtil.unescapeUrl(routeUri.toString(),"utf-8");
			if(uri.contains("{") && uri.contains("}")){
				String regx="\\{(.*?)\\}";
				uri=StringUtils.replaceAll(uri,
						regx, 1, (key)->{
							return map.get(key);
						});
				Route newRoute = Route.async()
						.asyncPredicate(route.getPredicate())
						.filters(route.getFilters())
						.id(route.getId())
						.order(route.getOrder())
						.uri(uri)
						.build();
				exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, newRoute);
			}

		}

		return chain.filter(exchange);
	}

}
