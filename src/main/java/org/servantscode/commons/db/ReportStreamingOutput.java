package org.servantscode.commons.db;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public abstract class ReportStreamingOutput implements StreamingOutput {
    private List<String> fields;

    public ReportStreamingOutput(List<String> fields) {
        this.fields = fields;
    }

    // ----- Private -----
    protected void writeCsv(OutputStream output, ResultSet rs) throws IOException {
        output.write(prepareHeaders().getBytes());
        String data = null;
        while((data = prepareData(rs)) != null)
            output.write(data.getBytes());
    }

    protected String prepareHeaders() {
        String preparedData = "";

        boolean first = true;
        for(String field: fields) {
            if(first) {
                first = false;
            } else {
                preparedData += ",";
            }

            preparedData += field;
        }

        return preparedData + "\r\n";
    }

    protected String prepareData(ResultSet rs) throws IOException {
        String preparedData = "";

        try {
            if(!rs.next())
                return null;

            boolean first = true;
            for(String field: fields) {
                if(first) {
                    first = false;
                } else {
                    preparedData += ",";
                }

                preparedData += rs.getString(field);
            }

        } catch (SQLException e) {
            throw new IOException("Could not read field from results", e);
        }
        return preparedData + "\r\n";
    }
}
