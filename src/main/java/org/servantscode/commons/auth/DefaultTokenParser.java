package org.servantscode.commons.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnvProperty;

import static org.servantscode.commons.StringUtils.isEmpty;

public class DefaultTokenParser implements TokenParser {
    private static final Logger LOG = LogManager.getLogger(DefaultTokenParser.class);
    private static final String SIGNING_KEY = EnvProperty.get("JWT_KEY");
    private static final String JWT_ISSUER = EnvProperty.get("JWT_ISSUER", "Servant's Code");
    private static final Algorithm algorithm = Algorithm.HMAC256(SIGNING_KEY);
    private static final JWTVerifier VERIFIER = JWT.require(algorithm)
            .acceptLeeway(1)   //1 sec leeway for date checks to account for clock slop
            .withIssuer(JWT_ISSUER)
            .build();

    @Override
    public DecodedJWT parseToken(String token) {
        if(isEmpty(token))
            return null;

        try {
            return VERIFIER.verify(token);
        } catch (JWTVerificationException e) {
            LOG.warn("Invalid jwt token presented.", e);
            return null;
        }
    }
}
