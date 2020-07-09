package org.servantscode.commons.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.Session;
import org.servantscode.commons.db.SessionDB;

import javax.ws.rs.NotAuthorizedException;
import java.time.ZonedDateTime;

import static org.servantscode.commons.security.SCSecurityContext.SYSTEM;

public class DefaultSessionVerifier implements SessionVerifier {
    private static Logger LOG = LogManager.getLogger(DefaultSessionVerifier.class);
    private SessionDB db = new SessionDB();

    public boolean verifySession(String callingIp, String token, String user) {
        if(!user.equals(SYSTEM)) {
            Session activeSession = db.getSessionByToken(token);
            if (activeSession == null || activeSession.getExpiration().isBefore(ZonedDateTime.now())) {
                throw new NotAuthorizedException("Not Authorized");
            } else if (!activeSession.getIp().equals(callingIp)) {
                LOG.info("Encountered change in calling ip. %s => %s", activeSession.getIp(), callingIp);
                activeSession.setIp(callingIp);
                db.updateCallingIp(activeSession);
            }
        }
        return true;
    }
}
