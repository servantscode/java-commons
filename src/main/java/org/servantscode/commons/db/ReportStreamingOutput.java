package org.servantscode.commons.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.DateUtils;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.text.DateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.servantscode.commons.StringUtils.isSet;
import static org.servantscode.commons.db.DBAccess.convert;

public abstract class ReportStreamingOutput implements StreamingOutput {
    private static Logger LOG = LogManager.getLogger(ReportStreamingOutput.class);

    public static final String CRLF = "\r\n";
    private List<String> fields;
    ResultSetMetaData metaData;

    private ZoneId timezone;

    // StreamingOutput performs its function outside of the context of the web service call.
    // The constructor MUST ensure that any context related data is cached for operation.
    public ReportStreamingOutput(List<String> fields) {
        this.fields = fields;

        //Get timezone requires OrganizationContext to be populated.
        this.timezone = DateUtils.getTimeZone();
    }

    public ReportStreamingOutput(List<String> fields, ZoneId timezone) {
        this.fields = fields;
        this.timezone = timezone;
    }

    // ----- Protected -----
    protected void writeCsv(OutputStream output, ResultSet rs) throws IOException {
        try {
            metaData = rs.getMetaData();
            String data = null;

            writeLine(output, prepareHeaders());
            while((data = prepareData(rs)) != null)
                writeLine(output, data);
        } catch (SQLException e) {
            throw new IOException("Could not read report from results", e);
        }
    }

    // ----- Private -----
    private void writeLine(OutputStream stream, String line) throws IOException {
        stream.write(line.getBytes());
        stream.write(CRLF.getBytes());
    }

    private String prepareHeaders() {
        return String.join(",", fields);
    }

    private String prepareData(ResultSet rs) throws SQLException {
        if(!rs.next())
            return null;

        LinkedList<String> data = new LinkedList<>();
        for(String field: fields)
            data.add(quote(extractColumn(rs, field)));

        return data.stream().map(d -> d == null? "": d).collect(Collectors.joining(","));
    }

    private String quote(String value) {
        return isSet(value)? "\"" + value + "\"": "";
    }

    private String extractColumn(ResultSet rs, String field) throws SQLException {
        int columnType = metaData.getColumnType(rs.findColumn(field));
        if(columnType != Types.VARCHAR &&
           columnType != Types.BIT &&
           columnType != Types.INTEGER &&
           columnType != Types.DATE &&
           columnType != Types.TIMESTAMP &&
           columnType != Types.TIMESTAMP_WITH_TIMEZONE &&
           columnType != Types.ARRAY)
            LOG.debug(String.format("Processing unexpected field %s of type: %d. Will try to report it as string data.", field, columnType));

        switch (columnType) {
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case Types.TIMESTAMP:
                return extractZonedDateTime(rs, field);
            case Types.ARRAY:
                return extractArrayData(rs, field);
            default:
                return rs.getString(field);
        }
    }

    private String extractArrayData(ResultSet rs, String field) throws SQLException {
        Array arr = rs.getArray(field);
        if(arr == null)
            return null;

        if(arr.getBaseType() != Types.VARCHAR && arr.getBaseType() != Types.INTEGER)
            LOG.debug(String.format("Extracting array data from %s with type: %s. Data: %s", field, arr.getBaseTypeName(), arr.toString()));

        List<String> arrData = new LinkedList<>();
        ResultSet arrs = arr.getResultSet();
        while(arrs.next())
            arrData.add(arrs.getString(2)); //data is in second column

        if(arrData.isEmpty())
            return null;

        return String.join("|", arrData);
    }

    private String extractZonedDateTime(ResultSet rs, String field) throws SQLException {
        ZonedDateTime zdt = convert(rs.getTimestamp(field));
        if(zdt == null)
            return null;

        return zdt.withZoneSameInstant(timezone).format(DateTimeFormatter.ofPattern("YYYY-mm-dd hh:mm a"));
    }
}
