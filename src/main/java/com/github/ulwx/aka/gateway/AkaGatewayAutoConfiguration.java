package com.github.ulwx.aka.gateway;

import com.github.ulwx.aka.gateway.filters.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@PropertySource(value = {"classpath*:aka-application-gateway.yml"},
        factory = MyPropertySourceFactory.class)
@ComponentScan(
        basePackages = {"com.github.ulwx.aka.gateway"},
        nameGenerator = UniqueNameGenerator.class,
        excludeFilters = {
                @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
        })

public class AkaGatewayAutoConfiguration {

    @Autowired
    private RedisUtils redisUtils;


    public ThreadPoolTaskExecutor akaRedisListenerTaskExecutor() {
        ThreadPoolTaskExecutor springSessionRedisTaskExecutor = new ThreadPoolTaskExecutor();
        springSessionRedisTaskExecutor.setCorePoolSize(4);
        springSessionRedisTaskExecutor.setMaxPoolSize(10);
        springSessionRedisTaskExecutor.setKeepAliveSeconds(60);
        springSessionRedisTaskExecutor.setThreadNamePrefix("redis-listener-");
        springSessionRedisTaskExecutor.initialize();
        return springSessionRedisTaskExecutor;
    }

    @Bean("akaRedisMessageListenerContainer")
    @ConditionalOnMissingBean(name = "akaRedisMessageListenerContainer")
    public RedisMessageListenerContainer akaRedisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisUtils.getRedisTemplate().getConnectionFactory());
        container.setTaskExecutor(akaRedisListenerTaskExecutor());

        return container;
    }

}

