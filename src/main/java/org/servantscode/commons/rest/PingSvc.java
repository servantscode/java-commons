package org.servantscode.commons.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/ping")
public class PingSvc {

    @GET
    public String ping() {
        return "pong";
    }
}
