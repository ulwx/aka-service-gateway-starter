package com.github.ulwx.aka.gateway.filters.utils;

import com.github.ulwx.aka.dbutils.springboot.redis.AkaRedisSelector;
import com.github.ulwx.aka.dbutils.springboot.redis.AkaRedisUtils;
import com.github.ulwx.aka.gateway.AkaGatewayProperties;
import com.ulwx.tool.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * redis工具类
 */

public class RedisUtils  {
    private static final Logger log = LoggerFactory.getLogger(RedisUtils.class);

    private AkaRedisUtils akaRedisUtils;
    private AkaGatewayProperties akaGatewayProperties;


    public RedisUtils(ObjectProvider<AkaRedisUtils> akaRedisUtilsObj
            , AkaGatewayProperties akaGatewayProperties) {
        AkaRedisUtils akaRedisUtils=akaRedisUtilsObj.getIfAvailable();
        this.akaRedisUtils=akaRedisUtils;
        this.akaGatewayProperties=akaGatewayProperties;
    }


    private String getDsName(){
        if(akaGatewayProperties.getStoreConfig().getRedis().getEnable()) {
            return akaGatewayProperties.getStoreConfig().getRedis().getDs();
        }
        return null;
    }

    public AkaRedisUtils getAkaRedisUtils(){
        String ds=this.getDsName();
        if(ds==null) throw new RuntimeException("ds为空！");
        AkaRedisUtils.setDS(ds);
        return this.akaRedisUtils;
    }

}