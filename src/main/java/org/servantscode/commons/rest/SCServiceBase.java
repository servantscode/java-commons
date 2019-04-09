package org.servantscode.commons.rest;

import org.servantscode.commons.security.PermissionManager;
import org.servantscode.commons.security.SCPrincipal;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.SecurityContext;

public class SCServiceBase {
    protected void verifyUserAccess(String permission) {
        if(!PermissionManager.hasEnabledPermissions())
            throw new NotAuthorizedException("Requested action required login");

        if(!PermissionManager.canUser(permission))
            throw new ForbiddenException("Requested action is not available");
    }

    protected int getUserId(SecurityContext context) {
        return ((SCPrincipal)context.getUserPrincipal()).getUserId();
    }

    protected boolean userHasAccess(String permission) {
        if(!PermissionManager.hasEnabledPermissions())
            return false;

        return PermissionManager.canUser(permission);
    }
}
