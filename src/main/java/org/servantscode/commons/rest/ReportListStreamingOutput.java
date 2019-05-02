package org.servantscode.commons.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        return Arrays.stream(clazz.getMethods()).
                filter(this::isGetter).
                map(this::getFieldName).
                collect(Collectors.joining(",")) + "\r\n";
    }

    private String generateRow(T d) {
        return Arrays.stream(d.getClass().getMethods()).
                filter(this::isGetter).
                map(m -> getFieldValue(m, d)).
                collect(Collectors.joining(",")) + "\r\n";
    }

    private boolean isGetter(Method m) {
        return (m.getName().startsWith("get") || m.getName().startsWith("is")) && !m.getName().equals("getClass");
    }

    private String getFieldName(Method m) {
        String methodName = m.getName();
        String fieldName = methodName.startsWith("get")? methodName.substring(3): methodName.substring(2);
        return fieldName.substring(0,1).toLowerCase() + fieldName.substring(1);
    }

    private String getFieldValue(Method m, T d) {
        try {
            Object o = m.invoke(d);
            return (o != null)? o.toString(): "null";
        } catch (IllegalAccessException| InvocationTargetException e) {
            throw new RuntimeException("Could not get value from: " + m.getName(), e);
        }
    }
}
