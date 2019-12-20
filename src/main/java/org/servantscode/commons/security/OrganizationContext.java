package org.servantscode.commons.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.Organization;
import org.servantscode.commons.db.OrganizationDB;

public class OrganizationContext {
    private static final Logger LOG = LogManager.getLogger(OrganizationContext.class);

    private static ThreadLocal<OrganizationContext> LOCAL_INSTANCE = new ThreadLocal<>();
    private static OrganizationDB db = new OrganizationDB();

    public static void enableOrganization(String hostName) { LOCAL_INSTANCE.set(new OrganizationContext(hostName)); }

    public static Organization getOrganization() {
        OrganizationContext context = LOCAL_INSTANCE.get();
        return context == null? null: context.getEnabledOrganization();
    }

    public static int orgId() {
        OrganizationContext context = LOCAL_INSTANCE.get();
        return context == null? 0: context.getEnabledOrganization().getId();
    }

    public static void clearEnabledOrganization() { LOCAL_INSTANCE.remove(); }

    // ----- Instance -----
    private Organization enabledOrganization;

    private OrganizationContext(String hostName) {
        LOG.debug("Organization set to: " + hostName);
        enabledOrganization = db.getOrganization(hostName);
    }

    private Organization getEnabledOrganization() {
        return enabledOrganization;
    }
}
