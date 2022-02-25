package org.servantscode.commons.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

@Provider
public class GZipWriterInterceptor implements WriterInterceptor {
    private HttpHeaders context;

    public GZipWriterInterceptor(@Context HttpHeaders context) {
        this.context = context;
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext writerInterceptorContext) throws IOException, WebApplicationException {
        String acceptEncoding = context.getHeaderString("Accept-Encoding");
        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
            final OutputStream outputStream = writerInterceptorContext.getOutputStream();
            writerInterceptorContext.setOutputStream(new GZIPOutputStream(outputStream));
            writerInterceptorContext.getHeaders().putSingle("Content-Encoding", "gzip");
        }
        writerInterceptorContext.proceed();

    }
}
