package org.servantscode.commons.rest;

import org.servantscode.commons.security.PermissionManager;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;

public class SCServiceBase {
    protected void verifyUserAccess(String permission) {
        if(!PermissionManager.hasEnabledPermissions())
            throw new NotAuthorizedException("Requested action required login");

        if(!PermissionManager.canUser(permission))
            throw new ForbiddenException("Requested action is not available");
    }

    protected boolean userHasAccess(String permission) {
        if(!PermissionManager.hasEnabledPermissions())
            return false;

        return PermissionManager.canUser(permission);
    }
}
