package org.servantscode.commons.security;

import org.servantscode.commons.Organization;
import org.servantscode.commons.db.OrganizationDB;

public class OrganizationContext {
    private static ThreadLocal<OrganizationContext> LOCAL_INSTANCE = new ThreadLocal<>();
    private static OrganizationDB db = new OrganizationDB();

    public static void enableOrganization(String hostName) { LOCAL_INSTANCE.set(new OrganizationContext(hostName)); }

    public static Organization getOrganization() { return LOCAL_INSTANCE.get().getEnabledOrganization(); }

    public static int orgId() { return LOCAL_INSTANCE.get().getEnabledOrganization().getId(); }

    public static void clearEnabledOrganization() { LOCAL_INSTANCE.remove(); }

    // ----- Instance -----
    private Organization enabledOrganization;

    private OrganizationContext(String hostName) {
        System.out.println("Organization set to: " + hostName);
        enabledOrganization = db.getOrganization(hostName);
    }

    private Organization getEnabledOrganization() {
        return enabledOrganization;
    }
}
