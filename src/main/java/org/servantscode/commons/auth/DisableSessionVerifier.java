package org.servantscode.commons.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.SessionVerifier;

public class DisableSessionVerifier implements SessionVerifier {
    private static Logger LOG = LogManager.getLogger(DisableSessionVerifier.class);

    public boolean verifySession(String callingIp, String token, DecodedJWT jwt) {

        LOG.trace("Session Verification DISABLED.");

        return true;
    }
}
