package org.servantscode.commons;

import java.time.ZonedDateTime;

public class Session {
    private int personId;
    private int orgId;
    private String token;
    private ZonedDateTime expiration;
    private String ip;

    // ----- Accessors -----
    public int getPersonId() { return personId; }
    public void setPersonId(int personId) { this.personId = personId; }

    public int getOrgId() { return orgId; }
    public void setOrgId(int orgId) { this.orgId = orgId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public ZonedDateTime getExpiration() { return expiration; }
    public void setExpiration(ZonedDateTime expiration) { this.expiration = expiration; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
