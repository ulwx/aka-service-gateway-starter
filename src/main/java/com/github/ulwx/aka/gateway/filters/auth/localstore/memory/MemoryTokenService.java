package com.github.ulwx.aka.gateway.filters.auth.localstore.memory;

import com.github.ulwx.aka.gateway.filters.auth.TokenService;
import com.github.ulwx.aka.gateway.filters.auth.UserTokenInfo;
import com.ulwx.tool.ObjectUtils;
import com.ulwx.tool.StringUtils;
import com.ulwx.tool.cache.CacheUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component("com.github.ulwx.aka.gateway.filters.auth.localstore.memory.MemoryTokenService")
public class MemoryTokenService implements TokenService {
    //public final static MemoryTokenService x=new MemoryTokenService();
    private final static String prefx_rft ="token_rft";
    private final static String prefx_usr ="token_usr";
    private static String prefx_rft(String refreshToken){
        return prefx_rft +":"+refreshToken ;
    }
    private static String prefx_usr(String userID){
        return prefx_usr +":"+userID ;
    }
    static {
        CacheUtils.registRemoveListener(r->{
            String key=r.getKey().toString();
            CacheUtils.CacheObj value = r.getValue();
            if(key.startsWith(prefx_rft +":")){
                String[] strs=key.split("\\:",3);
                if(strs.length==2){
                    String refreshToken=strs[1];
                    UserTokenInfo userTokenInfo=(UserTokenInfo)(value.getValue());
                    if(userTokenInfo==null)  return;
                    String tokenkey= prefx_usr(userTokenInfo.getUserid());
                    Set<String> sourseSet=(Set<String>)CacheUtils.get(tokenkey);
                    if(sourseSet==null){
                       return;
                    }
                    if(sourseSet.isEmpty()){
                        CacheUtils.remove(tokenkey);
                    }
                    if(sourseSet.contains(refreshToken)){
                        sourseSet.remove(refreshToken);
                        if(sourseSet.isEmpty()){
                            CacheUtils.remove(tokenkey);
                        }else {
                            CacheUtils.set(tokenkey, sourseSet, -1);
                        }
                    }else{//删掉
                    }
                }
            }

        });
    }


    private void removeTokenInfo(String refreshToken) {
        CacheUtils.remove(prefx_rft(refreshToken));
    }
    @Override
    public void removeTokenInfoBy(String refreshToken, String userId, String sourceId){
        if(StringUtils.hasText(refreshToken)){
            removeTokenInfo(refreshToken);
            return;
        }
        if(StringUtils.hasText(userId)) {
            Set<String> refreshTokens = getRefreshTokensBy(userId);
            for (String refreshTk : refreshTokens) {
                UserTokenInfo userTokenInfo = queryTokenInfoBy(refreshTk);
                if (userTokenInfo != null) {
                    if (StringUtils.hasText(sourceId)) {
                        if (userTokenInfo.getSource().equals(sourceId)) {
                            removeTokenInfo(refreshToken);
                            break;
                        }
                    } else {
                        removeTokenInfo(refreshToken);
                    }

                }
            }
        }
    }
    private Set<String> getRefreshTokensBy(String userId) {
        String key= prefx_usr(userId);
        return (Set<String>)CacheUtils.get(key);
    }

    @Override
    public void storeTokenInfo(UserTokenInfo userTokenInfo) {
        userTokenInfo=ObjectUtils.CloneWithDeep(userTokenInfo);
        CacheUtils.set(prefx_rft(userTokenInfo.getRefreshToken()),
                     userTokenInfo,
                     userTokenInfo.getRefreshTokenTtl());
        String tokenkey= prefx_usr(userTokenInfo.getUserid());
        Set<String> refreshTokenSet=(Set<String>)CacheUtils.get(tokenkey);
        if(refreshTokenSet==null){
            refreshTokenSet=new LinkedHashSet<>();
        }

        if(!refreshTokenSet.contains(userTokenInfo.getRefreshToken())){
            refreshTokenSet.add(userTokenInfo.getRefreshToken());
            CacheUtils.set(tokenkey, refreshTokenSet, -1);
        }

    }
    @Override
   public UserTokenInfo queryTokenInfoBy(String refreshToken) {
        UserTokenInfo obj=(UserTokenInfo)CacheUtils.get(prefx_rft(refreshToken));
        if(obj==null)  return null;
        return obj;
    }

    public static void main(String[] args) throws Exception {
        Object ret;
        MemoryTokenService memoryTokenService=new MemoryTokenService();
        String userId="userid-a";
        UserTokenInfo userTokenInfo=new UserTokenInfo(userId, "a",
                3, "b", 500, "pc");
        memoryTokenService.storeTokenInfo(userTokenInfo);
        userTokenInfo.setSource("web");
        userTokenInfo.setRefreshToken("c");
        userTokenInfo.setRefreshTokenTtl(300);
        memoryTokenService.storeTokenInfo(userTokenInfo);

        ret=memoryTokenService.queryTokenInfoBy("b");

        Thread.sleep(10*1000);
        ret=memoryTokenService.getRefreshTokensBy(userId);
        memoryTokenService.removeTokenInfo("c");
        ret=memoryTokenService.getRefreshTokensBy(userId);
        System.out.println(ObjectUtils.toString(ret));
    }
}
