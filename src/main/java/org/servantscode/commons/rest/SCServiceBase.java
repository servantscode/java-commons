package org.servantscode.commons.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.ConfigDB;
import org.servantscode.commons.security.PermissionManager;
import org.servantscode.commons.security.SCPrincipal;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.util.concurrent.Callable;

public class SCServiceBase {
    private static Logger LOG = LogManager.getLogger(SCServiceBase.class);

    private ConfigDB configDB;

    @Context protected SecurityContext securityContext;

    public SCServiceBase() {
        this.configDB = new ConfigDB();
    }

    protected void verifyUserAccess(String permission) {
        if(!PermissionManager.hasEnabledPermissions())
            throw new NotAuthorizedException("Requested action required login");

        if(!PermissionManager.canUser(permission))
            throw new ForbiddenException("Requested action is not available");
    }

    protected int getUserId() {
        if(securityContext == null || securityContext.getUserPrincipal() == null)
            return -1;

        return ((SCPrincipal)securityContext.getUserPrincipal()).getUserId();
    }

    protected boolean userHasAccess(String permission) {
        if(!PermissionManager.hasEnabledPermissions())
            return false;

        return PermissionManager.canUser(permission);
    }

    protected String getConfiguration(String config) {
        return configDB.getConfiguration(config);
    }

    public <T> T processRequest(Callable<T> r) {
        try {
            return r.call();
        } catch(WebApplicationException t) {
            throw t;
        } catch(Throwable t) {
            LOG.error("Call failed.", t);
            throw new WebApplicationException("Call failed.", t);
        }
    }

    public void processRequest(Runnable r) {
        try {
            r.run();
        } catch(WebApplicationException t) {
            throw t;
        } catch(Throwable t) {
            LOG.error("Call failed.", t);
            throw new WebApplicationException("Call failed.", t);
        }
    }
}
