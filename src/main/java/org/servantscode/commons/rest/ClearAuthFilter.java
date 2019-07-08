package org.servantscode.commons.rest;

import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.commons.security.PermissionManager;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(2000)
public class ClearAuthFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        PermissionManager.clearEnabledPermissions();
        OrganizationContext.clearEnabledOrganization();
    }
}