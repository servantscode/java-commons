package org.servantscode.commons.auth;

import org.servantscode.commons.rest.AuthFilter;

public class RequestType {
    private final String method;
    private final String path;
    private final boolean partial;

    public RequestType(String method, String path, boolean partial) {
        this.method = method;
        this.path = path;
        this.partial = partial;
    }

    public RequestType(String method, String path) {
        this(method, path, false);
    }

    public RequestType(String path) {
        this("POST", path, false);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RequestType))
            return false;
        RequestType other = (RequestType) obj;
        return (this.method.equals(ANY) || other.method.equals(ANY) || this.method.equals(other.method)) &&
                ((this.partial && other.path.startsWith(this.path)) ||
                        (other.partial && this.path.startsWith(other.path)) ||
                        this.path.equals(other.path));
    }

    @Override
    public String toString() {
        return String.format("Request {%s %s}", method, path);
    }
}
