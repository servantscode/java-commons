package org.servantscode.commons.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PermissionManager {
    private static final Logger LOG = LogManager.getLogger(PermissionManager.class);

    private static ThreadLocal<PermissionManager> LOCAL_INSTANCE = new ThreadLocal<>();

    public static void enablePermissions(String[] permissions) {
        LOCAL_INSTANCE.set(new PermissionManager(permissions));
    }

    public static boolean hasEnabledPermissions() {
        if(LOCAL_INSTANCE.get() == null) {
            LOG.debug("Called for permissions before they are enabled.", new Exception("Stacktrace:"));
            return false;
        }

        return true;
    }

    public static boolean canUser(String permission) {
        return LOCAL_INSTANCE.get().verifyAccess(permission);
    }

    public static void clearEnabledPermissions() {
        LOCAL_INSTANCE.remove();
    }

    // ----- Instance -----
    private String[] userPerms;

    private PermissionManager(String[] perms) {
        userPerms = perms;
    }

    private boolean verifyAccess(String permission) {
        for(String userPerm: userPerms) {
            if(matches(userPerm, permission))
                return true;
        }

        return false;
    }

    public static boolean matches(String userPerm, String permRequest) {
        return matches(userPerm.split("\\."), permRequest.split("\\."), 0);
    }

    // ----- Private -----
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
}
