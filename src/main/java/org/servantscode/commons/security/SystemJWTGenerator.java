package org.servantscode.commons.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.apache.logging.log4j.ThreadContext;
import org.servantscode.commons.EnvProperty;
import org.servantscode.commons.Organization;
import org.servantscode.commons.Session;
import org.servantscode.commons.db.SessionDB;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.servantscode.commons.security.SCSecurityContext.SYSTEM;

public class SystemJWTGenerator {
    private static final String SIGNING_KEY = EnvProperty.get("JWT_KEY", "aJWTKey");

    public static String generateToken() {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SIGNING_KEY);
            Date now = new Date();
            long duration = 60*1000; // 1 minute

            return JWT.create()
                    .withSubject(SYSTEM)
                    .withIssuedAt(now)
                    .withExpiresAt(new Date(now.getTime() + duration))
                    .withIssuer("Servant's Code")
                    .withClaim("role", "system")
                    .withClaim("userId", "0")
                    .withClaim("org", OrganizationContext.getOrganization().getName())
                    .withArrayClaim("permissions", new String[] {"*"})
                    .sign(algorithm);
        } catch (JWTCreationException e){
            throw new RuntimeException("Could not create system JWT Token", e);
        }
    }
}
