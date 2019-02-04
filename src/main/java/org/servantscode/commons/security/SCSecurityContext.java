package org.servantscode.commons.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;

public class SCSecurityContext implements SecurityContext {

    private final UriInfo uriInfo;
    private final DecodedJWT jwt;

    public SCSecurityContext(UriInfo uriInfo, DecodedJWT jwt)  {
        this.uriInfo = uriInfo;
        this.jwt = jwt;
    }

    @Override
    public Principal getUserPrincipal() {
        return jwt::getSubject;
    }

    @Override
    public boolean isUserInRole(String role) {
        Claim claim = jwt.getClaim("role");
        if(claim == null)
            return false;

        return claim.asString().equalsIgnoreCase(role);
    }

    public boolean canUser(String permission) {
        Claim claim = jwt.getClaim("permissions");
        if(claim == null)
            return false;

        String[] userPerms = claim.asArray(String.class);
        for(String userPerm: userPerms) {
            if(matches(userPerm, permission))
                return true;
        }

        return false;
    }

    public static boolean matches(String userPerm, String permRequest) {
       return matches(userPerm.split("\\."), permRequest.split("\\."), 0);
    }

    private static boolean matches(String[] userPerm, String[] permRequest, int index) {
        if(userPerm[index].equals("*"))
            return true;
        else if(!userPerm[index].equals(permRequest[index]))
            return false;

        if(index + 1  == userPerm.length)
            return true;
        else if(index + 1  == permRequest.length)
            return false;

        return matches(userPerm, permRequest, index + 1);
    }

    @Override
    public boolean isSecure() {
        return uriInfo.getAbsolutePath().toString().startsWith("https");
    }

    @Override
    public String getAuthenticationScheme() {
        return "JWT";
    }
}
