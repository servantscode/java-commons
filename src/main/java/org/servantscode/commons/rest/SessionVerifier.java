package org.servantscode.commons.rest;

public interface SessionVerifier {
    boolean verifySession(String callingIp, String token, String user);
}
