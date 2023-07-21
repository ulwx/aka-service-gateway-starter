package com.github.ulwx.aka.gateway.filters;

import com.alibaba.fastjson.JSON;
import org.apache.log4j.Logger;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.filter.factory.rewrite.GzipMessageBodyResolver;
import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyDecoder;
import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyEncoder;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;

 class ModifiedServerHttpResponse extends ServerHttpResponseDecorator {
     private static Logger log = Logger.getLogger(ModifiedServerHttpResponse.class);
     private final Map<String, MessageBodyDecoder> messageBodyDecoders;
     private final Map<String, MessageBodyEncoder> messageBodyEncoders;
     private final List<HttpMessageReader<?>> messageReaders= HandlerStrategies.withDefaults().messageReaders();;
     private final ServerWebExchange exchange;
     private final Class inClass;
     private final Class outClass;
     private final BiFunction<ServerWebExchange,Object,Mono>  modifyFunction;
     private Function<String,String> function;

     /**
      * 只能消费一次
      * @param function
      */
     public void setModifyBodyFunction(Function<String, String> function) {
         this.function = function;
     }

     public ModifiedServerHttpResponse(ServerWebExchange exchange,
                                       Class<?> inClass,
                                       Class<?> outClass){
        super(exchange.getResponse());
        this.exchange = exchange;
        this.inClass=inClass;
        this.outClass=outClass;
        this.modifyFunction= (webExchange,body)->{
            String ret=null;
            try {
                log.debug("before=============");
                log.debug(body);
                log.debug("=============");
                ret = function.apply((String) body);
                log.debug("after+++++++++++++");
                log.debug(ret);
                log.debug("+++++++++++++");
                if (ret == null) {
                    return Mono.just(body);
                } else {
                    return Mono.just(ret);
                }
            }catch (Exception e){
                log.error(e+"",e);
                GateWayException gateWayException;
                if(e instanceof GateWayException){
                    gateWayException=(GateWayException)e;
                }else{
                    gateWayException=new GateWayException(e);
                }
                ResponseResult responseResult= ResponseResult.error(gateWayException.getResponseCode().getCode(),
                        gateWayException.getMessage());
                return  Mono.just(JSON.toJSONString(responseResult));

            }

        };

        Set<MessageBodyDecoder> messageBodyDecodersSet = new HashSet<>();
        Set<MessageBodyEncoder> messageBodyEncodersSet = new HashSet<>();
        MessageBodyDecoder messageBodyDecoder = new GzipMessageBodyResolver();
        MessageBodyEncoder messageBodyEncoder = new GzipMessageBodyResolver();
        messageBodyDecodersSet.add(messageBodyDecoder);
        messageBodyEncodersSet.add(messageBodyEncoder);
        this.messageBodyDecoders = messageBodyDecodersSet.stream()
                .collect(Collectors.toMap(MessageBodyDecoder::encodingType, identity()));
        this.messageBodyEncoders = messageBodyEncodersSet.stream()
                .collect(Collectors.toMap(MessageBodyEncoder::encodingType, identity()));

    }


    @SuppressWarnings("unchecked")
    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {

         if(this.function ==null){
             return super.writeWith(body);
         }
        String  contentType=this.getDelegate().getHeaders().getContentType().toString();
        if(contentType.contains("/json") || contentType.contains("text/")) {
            String originalResponseContentType = exchange.getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, originalResponseContentType);
            ClientResponse clientResponse = prepareClientResponse(body, httpHeaders);
            // TODO: flux or mono
            Mono modifiedBody = extractBody(exchange, clientResponse, inClass)
                    .flatMap((originalBody) -> modifyFunction.apply(exchange, originalBody))
                    .switchIfEmpty(Mono.defer(() -> (Mono) modifyFunction.apply(exchange, null)));

            BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, outClass);
            CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange,
                    exchange.getResponse().getHeaders());
            return bodyInserter.insert(outputMessage, new BodyInserterContext()).then(Mono.defer(() -> {
                Mono<DataBuffer> messageBody = writeBody(getDelegate(), outputMessage, outClass);
                HttpHeaders headers = getDelegate().getHeaders();
                if (!headers.containsKey(HttpHeaders.TRANSFER_ENCODING)
                        || headers.containsKey(HttpHeaders.CONTENT_LENGTH)) {
                    messageBody = messageBody.doOnNext(data ->
                            headers.setContentLength(data.readableByteCount()));
                }
                // TODO: fail if isStreamingMediaType?
                return getDelegate().writeWith(messageBody);
            }));
        }

        return super.writeWith(body);
    }

    @Override
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        return writeWith(Flux.from(body).flatMapSequential(p -> p));
    }

    private ClientResponse prepareClientResponse(Publisher<? extends DataBuffer> body, HttpHeaders httpHeaders) {
        ClientResponse.Builder builder;
        builder = ClientResponse.create(exchange.getResponse().getStatusCode(), messageReaders);
        return builder.headers(headers -> headers.putAll(httpHeaders)).body(Flux.from(body)).build();
    }

    private <T> Mono<T> extractBody(ServerWebExchange exchange, ClientResponse clientResponse, Class<T> inClass) {
        if (byte[].class.isAssignableFrom(inClass)) {
            return clientResponse.bodyToMono(inClass);
        }

        List<String> encodingHeaders = exchange.getResponse().getHeaders().getOrEmpty(HttpHeaders.CONTENT_ENCODING);
        for (String encoding : encodingHeaders) {
            MessageBodyDecoder decoder = messageBodyDecoders.get(encoding);
            if (decoder != null) {
                return clientResponse.bodyToMono(byte[].class).publishOn(Schedulers.parallel()).map(decoder::decode)
                        .map(bytes -> exchange.getResponse().bufferFactory().wrap(bytes))
                        .map(buffer -> prepareClientResponse(Mono.just(buffer),
                                exchange.getResponse().getHeaders()))
                        .flatMap(response -> response.bodyToMono(inClass));
            }
        }

        return clientResponse.bodyToMono(inClass);
    }

    private Mono<DataBuffer> writeBody(ServerHttpResponse httpResponse, CachedBodyOutputMessage message,
                                       Class<?> outClass) {
        Mono<DataBuffer> response = DataBufferUtils.join(message.getBody());
        if (byte[].class.isAssignableFrom(outClass)) {
            return response;
        }

        List<String> encodingHeaders = httpResponse.getHeaders().getOrEmpty(HttpHeaders.CONTENT_ENCODING);
        for (String encoding : encodingHeaders) {
            MessageBodyEncoder encoder = messageBodyEncoders.get(encoding);
            if (encoder != null) {
                DataBufferFactory dataBufferFactory = httpResponse.bufferFactory();
                response = response.publishOn(Schedulers.parallel()).map(buffer -> {
                    byte[] encodedResponse = encoder.encode(buffer);
                    DataBufferUtils.release(buffer);
                    return encodedResponse;
                }).map(dataBufferFactory::wrap);
                break;
            }
        }

        return response;
    }

}