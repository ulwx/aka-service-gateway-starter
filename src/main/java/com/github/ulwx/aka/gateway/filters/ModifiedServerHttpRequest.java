package com.github.ulwx.aka.gateway.filters;

import org.apache.log4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModifiedServerHttpRequest extends ServerHttpRequestDecorator {
    private static Logger log = Logger.getLogger(ModifiedServerHttpRequest.class);
    private Function<String,String> function;
    private DataBufferFactory bufferFactory;
    private DataBufferHolder dataBufferHolder;
    private  final HttpHeaders headers=new HttpHeaders();
    private final List<HttpMessageReader<?>> messageReaders;
    private URI URI;
    @Nullable
    private MultiValueMap<String, String> queryParams;

    public void setModifyBodyFunction(Function<String, String> function, DataBufferFactory bufferFactory) {
        this.function = function;
        this.bufferFactory=bufferFactory;
        consumeBody();
    }

    public ModifiedServerHttpRequest(ServerWebExchange exchange){
        super(exchange.getRequest());
        headers.putAll(this.getDelegate().getHeaders());
        messageReaders= HandlerStrategies.withDefaults().messageReaders();
        URI=this.getDelegate().getURI();
    }

    public void setHeader(String name,String value){
        headers.set(name,value);
    }

    @Override
    public URI getURI() {
        return URI;
    }


    public void repaceQuery(String query){
        URI newUri = UriComponentsBuilder.fromUri(this.getDelegate().getURI())
                .replaceQuery(query).build(true).toUri();

        this.URI=newUri;
        this.queryParams = CollectionUtils.unmodifiableMultiValueMap(initQueryParams());
    }
    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public Flux<DataBuffer> getBody() {
        if(function==null) {
            return super.getBody();
        }
        return Flux.just(dataBufferHolder.dataBuffer);
    }

    public void consumeBody() {
        if(function!=null) {
            this.dataBufferHolder=
                    new ModifiedServerHttpRequest.DataBufferHolder();
            String contentType=this.getDelegate().getHeaders().getContentType().toString();
            if(contentType.contains("text/") || contentType.contains("json")) {
                Flux<DataBuffer> body = this.getDelegate().getBody();
                body.subscribe(dataBuffer -> {
                    int len = dataBuffer.readableByteCount();
                    byte[] bytes = new byte[len];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    String text = new String(bytes, StandardCharsets.UTF_8);
                    log.debug("=============");
                    log.debug(text);
                    log.debug("=============");
                    String ret = function.apply(text);
                    log.debug("+++++++++++++");
                    log.debug(ret);
                    log.debug("+++++++++++++");
                    DataBuffer data = bufferFactory.allocateBuffer();
                    byte[] content = ret.getBytes(StandardCharsets.UTF_8);
                    data.write(content);
                    dataBufferHolder.dataBuffer = data;
                    dataBufferHolder.length = content.length;
                });
                HttpHeaders headers = this.headers;
                headers.remove(HttpHeaders.CONTENT_LENGTH);
                int contentLength = dataBufferHolder.length;
                if (contentLength > 0) {
                    headers.setContentLength(contentLength);
                } else {
                    headers.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                }
            }

        }

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
        }
        catch (UnsupportedEncodingException ex) {
            // Should never happen but we got a platform default fallback anyway.
            return URLDecoder.decode(value);
        }
    }
    private static class DataBufferHolder {

        private DataBuffer dataBuffer=null;
        private int length=-1;


        public DataBuffer getDataBuffer() {
            return dataBuffer;
        }

        public void setDataBuffer(DataBuffer dataBuffer) {
            this.dataBuffer = dataBuffer;
        }


        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }
}
