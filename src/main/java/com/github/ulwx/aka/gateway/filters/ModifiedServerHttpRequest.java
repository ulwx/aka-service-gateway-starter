package com.github.ulwx.aka.gateway.filters;

import com.ulwx.tool.CTime;
import org.apache.log4j.Logger;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModifiedServerHttpRequest extends ServerHttpRequestDecorator {
    private static Logger log = Logger.getLogger(ModifiedServerHttpRequest.class);
    private BiFunction<String, String,String> function;
    private final HttpHeaders headers = new HttpHeaders();
    private ServerWebExchange exchange;
    private final List<HttpMessageReader<?>> messageReaders;
    private MyCachedBodyOutputMessage outputMessage;
    private Consumer<GateWayLog> logFunc ;
    private ModifiedServerHttpResponse serverHttpResponse;
    private  GatewayFilterChain chain;
    private GateWayLog gateWayLog;
    private URI URI;
    @Nullable
    private MultiValueMap<String, String> queryParams;

    public Mono<Void> forward() {
        return consumeBody();
    }
    public ModifiedServerHttpRequest(ServerWebExchange exchange,
                                     GatewayFilterChain chain,
                                     ModifiedServerHttpResponse serverHttpResponse,
                                     BiFunction<String, String,String>  alterReqBodyFunc,
                                     Consumer<GateWayLog> logFunc) {


        super(exchange.getRequest());
        this.exchange=exchange;
        this.serverHttpResponse=serverHttpResponse;
        if(logFunc!=null){
            this.logFunc=logFunc;
            this.gateWayLog=new GateWayLog();
            BiFunction<String, String,String> oldFun=this.serverHttpResponse.getModifyBodyFunction();
            if(oldFun!=null) {
                this.serverHttpResponse.modifyBody((body,contentType) -> {
                    String ret=oldFun.apply(body,contentType);
                    gateWayLog.setResponseBody(ret);
                    return ret;
                });
            }else{
                this.serverHttpResponse.modifyBody((body,contentType) -> {
                    gateWayLog.setResponseBody(body);
                    return body;
                });
            }
        }

        this.function = alterReqBodyFunc;
        this.chain=chain;
        headers.putAll(this.getDelegate().getHeaders());
        messageReaders = HandlerStrategies.withDefaults().messageReaders();
        URI = this.getDelegate().getURI();


        Route route=exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if(this.gateWayLog!=null) {
            gateWayLog.setSchema(URI.getScheme());
            gateWayLog.setRequestMethod(this.getDelegate().getMethodValue());
            gateWayLog.setRequestPath(this.getDelegate().getPath().pathWithinApplication().value());
            gateWayLog.setRouteid(route.getId());
            gateWayLog.setRouteURL(route.getUri()+"");
            gateWayLog.setRequestTime(CTime.getCurrentDateTime());
            //InetSocketAddress remoteAddress = this.getDelegate().getRemoteAddress();
            String ip=getIP(this);
            gateWayLog.setIp(ip);
            MultiValueMap<String, String> parms = exchange.getRequest().getQueryParams();
            gateWayLog.setQueryParms(parms);
            gateWayLog.setRequestHeaders(this.getHeaders());
            gateWayLog.setResponseHeaders(exchange.getResponse().getHeaders());

        }
    }
    public void setHeader(String name, String value) {
        headers.set(name, value);
    }

    /**
     * 获取客户端真实ip
     * @param request request
     * @return 返回ip
     */
    private static final String IP_UNKNOWN = "unknown";
    private static final String IP_LOCAL = "127.0.0.1";
    private static final int IP_LEN = 15;
    public static String getIP(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String ipAddress = headers.getFirst("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0 || IP_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = headers.getFirst("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || IP_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = headers.getFirst("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || IP_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = Optional.ofNullable(request.getRemoteAddress())
                    .map(address -> address.getAddress().getHostAddress())
                    .orElse("");
            if (IP_LOCAL.equals(ipAddress)) {
                // 根据网卡取本机配置的IP
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ipAddress = inet.getHostAddress();
                } catch (UnknownHostException e) {
                    // ignore
                }
            }
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ipAddress != null && ipAddress.length() > IP_LEN) {
            int index = ipAddress.indexOf(",");
            if (index > 0) {
                ipAddress = ipAddress.substring(0, index);
            }
        }
        return ipAddress;
    }

    @Override
    public URI getURI() {
        return URI;
    }
    public void repaceQuery(String query) {
        URI newUri = UriComponentsBuilder.fromUri(this.getDelegate().getURI())
                .replaceQuery(query).build(true).toUri();

        this.URI = newUri;
        this.queryParams = CollectionUtils.unmodifiableMultiValueMap(initQueryParams());
    }
    @Override
    public HttpHeaders getHeaders() {
        if (function == null) {
            return headers;
        }
        long contentLength = headers.getContentLength();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.putAll(headers);
        if (contentLength > 0L) {
            httpHeaders.setContentLength(contentLength);
        } else {
            httpHeaders.set("Transfer-Encoding", "chunked");
        }
        return httpHeaders;

    }

    @Override
    public Flux<DataBuffer> getBody() {
        if (function == null) {
            return super.getBody();
        }
        return outputMessage.getBody();

    }

    public static void main(String[] args) {

    }
    public Mono<Void> consumeBody() {
        Mono<Void> result=null;
        try {
            if (function == null) {
                result = null;
                return result;
            }
            ServerHttpRequest request = this.getDelegate();
            String contentType = this.getDelegate().getHeaders().getContentType().toString();
            String method = this.getDelegate().getMethodValue();
            if (HttpMethod.GET.name().equalsIgnoreCase(method)) {
                result = null;
                return result;
            }
             MediaType mediaType = request.getHeaders().getContentType();

            if (contentType.contains("text/") || contentType.contains("json") ||
                    MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {
                ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
                Mono<String> modifiedBody = serverRequest.bodyToMono(String.class).flatMap(
                        originalBody -> {
                            return modify().apply(exchange, originalBody);
                        }
                );
                BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
                this.headers.remove("Content-Length");
                MyCachedBodyOutputMessage outputMessage = new MyCachedBodyOutputMessage(exchange, headers);
                this.outputMessage = outputMessage;
                result = bodyInserter.insert(outputMessage, new BodyInserterContext()).then(Mono.defer(() -> {
                    ServerHttpRequest decorator = this;
                    return chain.filter(exchange.mutate().request(decorator).response(serverHttpResponse).build())
                            .doOnTerminate(() -> {
                                log();
                            });
                })).onErrorResume((throwable) -> {
                    return release(exchange, outputMessage, (Throwable) throwable);
                });
                return result;
            }

            return result;
        }finally {
            if(result==null){
                result=chain.filter(exchange.mutate().request(this).response(serverHttpResponse).build())
                        .doOnTerminate(() -> {
                            log();
                        });
                return  result;
            }
        }

    }

    private void log(){
        if(logFunc!=null && this.gateWayLog!=null){
            URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            this.gateWayLog.setTargetURL(targetUri+"");
            this.gateWayLog.setResponseTime(CTime.getCurrentDateTime());
            long executeTime = (gateWayLog.getResponseTime().getTime() -
                    gateWayLog.getRequestTime().getTime());
            this.gateWayLog.setExecuteTime(executeTime);
            logFunc.accept(this.gateWayLog);
        }
    }
    private BiFunction<ServerWebExchange, String, Mono<String>> modify() {
       // this.function
        return (ServerWebExchange serverWebExchange, String raw)->{
            try {
                String contentType = this.getDelegate().getHeaders().getContentType().toString();
                String ret=this.function.apply(raw,contentType);
                if(this.gateWayLog!=null) {
                    this.gateWayLog.setRequestBody(ret);
                }
                return Mono.just(ret);
            } catch (Exception e) {
                log.error("服务器异常",e);
                return Mono.empty();
            }
        };

    }


    protected Mono<Void> release(ServerWebExchange exchange, MyCachedBodyOutputMessage outputMessage, Throwable throwable) {
        return outputMessage.isCached() ? outputMessage.getBody().map(DataBufferUtils::release).then(Mono.error(throwable)) : Mono.error(throwable);
    }

    @Override
    public MultiValueMap<String, String> getQueryParams() {
        if (this.queryParams == null) {
            this.queryParams = CollectionUtils.unmodifiableMultiValueMap(initQueryParams());
        }
        return this.queryParams;
    }

    private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

    /**
     * A method for parsing of the query into name-value pairs. The return
     * value is turned into an immutable map and cached.
     * <p>Note that this method is invoked lazily on first access to
     * {@link #getQueryParams()}. The invocation is not synchronized but the
     * parsing is thread-safe nevertheless.
     */
    protected MultiValueMap<String, String> initQueryParams() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        String query = getURI().getRawQuery();
        if (query != null) {
            Matcher matcher = QUERY_PATTERN.matcher(query);
            while (matcher.find()) {
                String name = decodeQueryParam(matcher.group(1));
                String eq = matcher.group(2);
                String value = matcher.group(3);
                value = (value != null ? decodeQueryParam(value) : (StringUtils.hasLength(eq) ? "" : null));
                queryParams.add(name, value);
            }
        }
        return queryParams;
    }

    @SuppressWarnings("deprecation")
    private String decodeQueryParam(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Should never happen but we got a platform default fallback anyway.
            return URLDecoder.decode(value);
        }
    }


    /**
     * 跟CachedBodyOutputMessage几乎一样，只是将boolean isCached()改为public boolean isCached()
     * @author mask
     * @since 2022/7/15 10:16
     */
    public static class MyCachedBodyOutputMessage implements ReactiveHttpOutputMessage {
        private final DataBufferFactory bufferFactory;
        private final HttpHeaders httpHeaders;
        private boolean cached = false;
        private Flux<DataBuffer> body = Flux.error(new IllegalStateException("The body is not set. Did handling complete with success?"));

        public MyCachedBodyOutputMessage(ServerWebExchange exchange, HttpHeaders httpHeaders) {
            this.bufferFactory = exchange.getResponse().bufferFactory();
            this.httpHeaders = httpHeaders;
        }

        public void beforeCommit(Supplier<? extends Mono<Void>> action) {
        }

        public boolean isCommitted() {
            return false;
        }

        public boolean isCached() {
            return this.cached;
        }

        public HttpHeaders getHeaders() {
            return this.httpHeaders;
        }

        public DataBufferFactory bufferFactory() {
            return this.bufferFactory;
        }

        public Flux<DataBuffer> getBody() {
            return this.body;
        }

        /** @deprecated */
        @Deprecated
        public void setWriteHandler(Function<Flux<DataBuffer>, Mono<Void>> writeHandler) {
        }

        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            this.body = Flux.from(body);
            this.cached = true;
            return Mono.empty();
        }

        public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
            return this.writeWith(Flux.from(body).flatMap((p) -> {
                return p;
            }));
        }

        public Mono<Void> setComplete() {
            return this.writeWith(Flux.empty());
        }
    }

}
