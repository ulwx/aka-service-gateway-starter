package com.github.ulwx.aka.gateway;

import com.github.ulwx.aka.gateway.filters.AccessLogFilter;
import com.github.ulwx.aka.gateway.filters.auth.localstore.redis.RedisTokenService;
import com.github.ulwx.aka.gateway.filters.utils.RedisUtils;
import com.ulwx.tool.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyspaceEventMessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

@Component
public class AkaRedisListener extends KeyspaceEventMessageListener {
    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final Topic KEYSPACE_TOPIC =
            new PatternTopic("__keyspace@*__:"+RedisTokenService.prefx_rft+"*");
    @Autowired
    private RedisTokenService redisTokenService;


    public AkaRedisListener(@Qualifier("akaRedisMessageListenerContainer")
                            RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
        super.setKeyspaceNotificationsConfigParameter("KA");
    }
    @Override
    protected void doRegister(RedisMessageListenerContainer container) {
        container.addMessageListener(this, KEYSPACE_TOPIC);
    }
    @Override
    protected void doHandleMessage(Message message) {
        String event = message.toString();
        if(event.equals("expired")) {
           RedisSerializer<?> serializer = redisTokenService.getRedisUtils()
                   .getRedisTemplate().getKeySerializer();
            String channel = String.valueOf(serializer.deserialize(message.getChannel()));
            //__keyspace@0__:token_rft:4b2c2890:188bd35de7e:-7fb7
            int start=channel.indexOf("token_rft:");
            if(start>=0){
                String  key=channel.substring(start);
                String refreshToken= StringUtils.trimLeadingString(key,"token_rft:");
                redisTokenService.refreshTokenExpired(refreshToken);

            }
        }
    }


}

