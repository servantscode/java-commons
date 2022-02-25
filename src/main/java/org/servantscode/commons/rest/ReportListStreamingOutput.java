package org.servantscode.commons.rest;

import org.servantscode.commons.ReflectionUtils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.servantscode.commons.ReflectionUtils.getGetters;

public class ReportListStreamingOutput<T> implements StreamingOutput {

    final List<T> data;

    public ReportListStreamingOutput(List<T> data) {
        this.data = data;
    }

    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
        output.write(generateHeaders(data.get(0).getClass()).getBytes());
        data.forEach(d -> {
            String row = generateRow(d);
            try {
                output.write(row.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Could not write report row: " + row, e);
            }
        });
    }

    // ----- Private -----
    private String generateHeaders(Class<?> clazz) {
        return getGetters(clazz).stream().map(ReflectionUtils::getFieldName)
                .collect(Collectors.joining(",")) + "\r\n";
    }

    private String generateRow(T d) {
        return getGetters(d.getClass()).stream().map((m) -> ReflectionUtils.getFieldValue(m, d))
                .collect(Collectors.joining(",")) + "\r\n";
    }
}
