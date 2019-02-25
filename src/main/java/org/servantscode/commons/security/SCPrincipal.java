package org.servantscode.commons.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import javax.ws.rs.NotAuthorizedException;
import java.security.Principal;

public class SCPrincipal implements Principal {

    private final DecodedJWT jwt;

    public SCPrincipal(DecodedJWT jwt) {
        this.jwt = jwt;
    }

    @Override
    public boolean equals(Object another) {
        if(!(another instanceof SCPrincipal))
            return false;

        return getUserId() == ((SCPrincipal)another).getUserId();
    }

    public int getUserId() {
        Claim claim = jwt.getClaim("userId");
        if(claim == null)
            throw new NotAuthorizedException("Token does not have a userId");

        return claim.asInt();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        return jwt.hashCode();
    }

    @Override
    public String getName() {
        return jwt.getSubject();
    }
}
