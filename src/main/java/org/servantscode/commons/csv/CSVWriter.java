package org.servantscode.commons.csv;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.servantscode.commons.StringUtils.isEmpty;

public class CSVWriter extends Writer {

    OutputStream output;

    public CSVWriter(OutputStream output) {
        this.output = output;
    }

    public CSVWriter(File f) {
        try {
            this.output = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not open file for writing: " + f.getAbsolutePath(), e);
        }
    }

    public CSVWriter(String fileName) {
        try {
            this.output = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not open file for writing: " + fileName, e);
        }
    }

    public void write(List<String> row) throws IOException {
        String rowData = row.stream()
                .map(d -> isEmpty(d)? "": "\"" + d.replaceAll("\"", "\"\"") + "\"")
                .collect(Collectors.joining(",")) + "\r\n";
        output.write(rowData.getBytes(UTF_8));
    }

    public void write(String str) throws IOException {
        output.write(str.getBytes(UTF_8));
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        output.write(new String(cbuf).getBytes(UTF_8), off, len);
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
