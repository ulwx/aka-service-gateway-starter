package com.github.ulwx.aka.gateway;

import com.github.ulwx.aka.dbutils.springboot.redis.AkaRedisAutoConfiguration;
import com.github.ulwx.aka.dbutils.springboot.redis.AkaRedisUtils;
import com.github.ulwx.aka.gateway.filters.utils.RedisUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@PropertySource(name = "classpath*:aka-application-gateway.yml",
        value = "classpath*:aka-application-gateway.yml",
        factory = MyPropertySourceFactory.class)
@ComponentScan(
        basePackages = {"com.github.ulwx.aka.gateway"},
        nameGenerator = UniqueNameGenerator.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM,
                        classes = AutoConfigurationExcludeFilter.class)
        })
@EnableConfigurationProperties(AkaGatewayProperties.class)
@AutoConfigureAfter(AkaRedisAutoConfiguration.class)
@AutoConfigureBefore(ErrorWebFluxAutoConfiguration.class)
public class AkaGatewayAutoConfiguration {

    @Autowired
    private AkaGatewayProperties akaGatewayProperties;

    public ThreadPoolTaskExecutor akaRedisListenerTaskExecutor() {
        ThreadPoolTaskExecutor springSessionRedisTaskExecutor = new ThreadPoolTaskExecutor();
        springSessionRedisTaskExecutor.setCorePoolSize(4);
        springSessionRedisTaskExecutor.setMaxPoolSize(10);
        springSessionRedisTaskExecutor.setKeepAliveSeconds(60);
        springSessionRedisTaskExecutor.setThreadNamePrefix("redis-listener-");
        springSessionRedisTaskExecutor.initialize();
        return springSessionRedisTaskExecutor;
    }

    @Bean("com.github.ulwx.aka.gateway.filters.utils.RedisUtils")
    @ConditionalOnMissingBean(name="com.github.ulwx.aka.gateway.filters.utils.RedisUtils")
    @ConditionalOnBean(AkaRedisUtils.class)
    public RedisUtils redisUtils(ObjectProvider<AkaRedisUtils> akaRedisUtilsObj
            , AkaGatewayProperties akaGatewayProperties){
        return new RedisUtils(akaRedisUtilsObj,akaGatewayProperties);
    }
    @Bean("akaRedisMessageListenerContainer")
    @ConditionalOnMissingBean(name = "akaRedisMessageListenerContainer")
    @ConditionalOnBean(AkaRedisUtils.class)
    public RedisMessageListenerContainer akaRedisMessageListenerContainer(RedisUtils redisUtils
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisUtils.getAkaRedisUtils().getRedisTemplate().getConnectionFactory());
        container.setTaskExecutor(akaRedisListenerTaskExecutor());
        return container;
    }
    @Bean("akaRedisListener")
    @ConditionalOnMissingBean(name ="akaRedisListener")
    @ConditionalOnBean(AkaRedisUtils.class)
    public AkaRedisListener akaRedisListener(@Qualifier("akaRedisMessageListenerContainer")
                                             RedisMessageListenerContainer listenerContainer){

        AkaRedisListener akaRedisListener=new AkaRedisListener(listenerContainer);
        return akaRedisListener;
    }

    @Bean
    public MyDefaultErrorAttributes errorAttributes() {
        return new MyDefaultErrorAttributes();

    }

}

