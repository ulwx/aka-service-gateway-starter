package com.github.ulwx.aka.gateway.filters.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ulwx.tool.CTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;


public class JwtHelper {
    public static final String USER_ID = "userId";
    public static final String PHONE = "phone";
    public static final String USER_TYPE = "userType";
    public static final String JWT_EXT = "jwtExt";
    public static final String SOURCE = "source";

    private static Logger log = LoggerFactory.getLogger(JwtHelper.class);


    public static String createJWT(TokenInfo jwtInfo, String jwtKey) {

        /***
         iss: jwt签发者
         sub: jwt所面向的用户
         aud: 接收jwt的一方
         exp: jwt的过期时间，这个过期时间必须要大于签发时间
         nbf: 定义在什么时间之前，该jwt都是不可用的.
         iat: jwt的签发时间
         jti: jwt的唯一身份标识，主要用来作为一次性token,从而回避重放攻击。
         **/
        Date now = new Date();
        Date notBefore = CTime.addMinutes(-2);
        Date expirationDate =jwtInfo.getExpiredAt();
        Algorithm algorithm = Algorithm.HMAC256(jwtKey);

        String token = JWT.create()
                .withIssuer(Constant.JWT_ISSUER)
                .withSubject(jwtInfo.getDeviceID())
                .withIssuedAt(now)
                .withNotBefore(notBefore)
                .withJWTId(jwtInfo.getJwtID())
                .withExpiresAt(expirationDate)
				.withClaim(SOURCE, jwtInfo.getSource())
                .withClaim(PHONE, jwtInfo.getPhone())
                .withClaim(USER_ID, jwtInfo.getUser())
                .withClaim(USER_TYPE, jwtInfo.getUserType())
                .withClaim(JWT_EXT, jwtInfo.getExt())
                .sign(algorithm);

        return token;
    }


    public static  TokenInfo decode(String jwtToken){
        DecodedJWT jwt = JWT.decode(jwtToken);
        TokenInfo jwtInfo = new TokenInfo();
        jwtInfo.setDeviceID(jwt.getSubject());
        jwtInfo.setJwtID(jwt.getId());
        jwtInfo.setPhone(jwt.getClaim(PHONE).asString());
        jwtInfo.setUser(jwt.getClaim(USER_ID).asString());
        jwtInfo.setUserType(jwt.getClaim(USER_TYPE).asString());
        jwtInfo.setSource(jwt.getClaim(SOURCE).asString());
        jwtInfo.setExt(jwt.getClaim(JWT_EXT).asString());
        jwtInfo.setExpiredAt(jwt.getExpiresAt());
        return jwtInfo;
    }
    public static TokenInfo parseJWT(String jwtToken, String jwtKey) {

        Algorithm algorithm = Algorithm.HMAC256(jwtKey);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(Constant.JWT_ISSUER)
                .build();
        DecodedJWT jwt = verifier.verify(jwtToken);
        TokenInfo jwtInfo = new TokenInfo();
        jwtInfo.setDeviceID(jwt.getSubject());
        jwtInfo.setJwtID(jwt.getId());
        jwtInfo.setPhone(jwt.getClaim(PHONE).asString());
        jwtInfo.setUser(jwt.getClaim(USER_ID).asString());
        jwtInfo.setUserType(jwt.getClaim(USER_TYPE).asString());
		jwtInfo.setSource(jwt.getClaim(SOURCE).asString());
        jwtInfo.setExt(jwt.getClaim(JWT_EXT).asString());
        jwtInfo.setExpiredAt(jwt.getExpiresAt());
        //验证是否过期
        return jwtInfo;


    }


}


class Constant {
    public static final String JWT_ISSUER = "org.swt";

}